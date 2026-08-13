package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/ipad")
public class IpadCheckoutController {
    private static final Set<String> PAY_TYPES = Set.of("wechat", "alipay", "cash", "card", "credit", "split");

    private final JdbcTemplate jdbc;
    private final BookingMasterRepository bookingRepository;
    private final BookingTableRepository tableRepository;
    private final BookingDishDetailRepository detailRepository;
    private final DishMasterRepository dishRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IpadCheckoutController(JdbcTemplate jdbc, BookingMasterRepository bookingRepository,
                                  BookingTableRepository tableRepository,
                                  BookingDishDetailRepository detailRepository,
                                  DishMasterRepository dishRepository) {
        this.jdbc = jdbc;
        this.bookingRepository = bookingRepository;
        this.tableRepository = tableRepository;
        this.detailRepository = detailRepository;
        this.dishRepository = dishRepository;
    }

    @Transactional
    @PostMapping("/order/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long storeId = requiredStore(request);
        Map<String, Object> authorizer = verifyOrderPassword(storeId, clean(body.get("staff_password"), null));
        Long staffId = ((Number) authorizer.get("staff_id")).longValue();
        Integer tableId = positiveInt(body.get("table_id"), "请选择桌台");
        Integer guestCount = positiveInt(body.get("guest_count"), "用餐人数必须大于0");
        if (guestCount > 2000) throw new IllegalArgumentException("用餐人数超出合理范围");

        List<Map<String, Object>> rows = castRows(body.get("dishes"));
        if (rows.isEmpty() || rows.size() > 200) throw new IllegalArgumentException("菜品不能为空且最多200项");

        List<Map<String, Object>> lockedTable = jdbc.queryForList(
                "SELECT table_status FROM table_master WHERE table_id=? AND store_id=? FOR UPDATE", tableId, storeId);
        if (lockedTable.isEmpty()) throw new IllegalArgumentException("桌台不存在或无权访问");
        String tableStatus = Objects.toString(lockedTable.get(0).get("table_status"), "");
        if (!("idle".equals(tableStatus) || "available".equals(tableStatus))) {
            throw new IllegalStateException("桌台已被占用，请刷新后选择其他桌台");
        }

        String bookingId = "IP" + storeId + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
        BookingMaster booking = new BookingMaster();
        booking.setBookingId(bookingId);
        booking.setStoreId(storeId);
        booking.setBookingDate(LocalDate.now());
        booking.setBookingTime(LocalTime.now());
        booking.setBookingType(Objects.toString(body.get("booking_type"), "normal"));
        booking.setCustomerName(clean(body.get("customer_name"), "散客"));
        booking.setCustomerPhone(clean(body.get("customer_phone"), null));
        booking.setGuestCount(guestCount);
        booking.setStaffId(staffId.intValue());
        booking.setStaffName(Objects.toString(authorizer.get("staff_name"), "授权员工"));
        booking.setBookingStatus("dining");
        booking.setPaymentStatus("unpaid");
        booking.setRemark(clean(body.get("remark"), null));

        BigDecimal total = BigDecimal.ZERO;
        List<BookingDishDetail> details = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String dishId = clean(row.get("dish_id"), null);
            Integer quantity = positiveInt(row.get("dish_quantity"), "菜品数量必须大于0");
            if (quantity > 99) throw new IllegalArgumentException("单项菜品最多99份");
            DishMaster dish = dishRepository.findByDishIdAndStoreId(dishId, storeId)
                    .orElseThrow(() -> new IllegalArgumentException("菜品不存在或已下架: " + dishId));
            BigDecimal price = Optional.ofNullable(dish.getSalePrice()).orElse(BigDecimal.ZERO);
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            BookingDishDetail detail = new BookingDishDetail();
            detail.setStoreId(storeId);
            detail.setBookingId(bookingId);
            detail.setDishId(dishId);
            detail.setDishName(dish.getDishName());
            detail.setDishQuantity(quantity);
            detail.setUnitPrice(price);
            detail.setSubtotal(subtotal);
            detail.setDishNote(clean(row.get("dish_note"), null));
            detail.setKitchenStatus("submitted");
            details.add(detail);
        }

        booking.setTotalAmount(total);
        booking.setFinalAmount(total);
        bookingRepository.save(booking);

        BookingTable bookingTable = new BookingTable();
        bookingTable.setBookingId(bookingId);
        bookingTable.setStoreId(storeId);
        bookingTable.setBookingDate(LocalDate.now());
        bookingTable.setBookingTime(LocalTime.now());
        bookingTable.setTableId(tableId);
        bookingTable.setGuestCount(guestCount);
        tableRepository.save(bookingTable);
        detailRepository.saveAll(details);
        jdbc.update("UPDATE table_master SET table_status='occupied', update_time=NOW() WHERE table_id=? AND store_id=?", tableId, storeId);

        return Result.success(Map.of("booking_id", bookingId, "total_amount", total, "dish_count", details.size()));
    }

    @GetMapping("/settlement/bill/{bookingId}")
    public Result<Map<String, Object>> bill(@PathVariable String bookingId, HttpServletRequest request) {
        Long storeId = requiredStore(request);
        BookingMaster booking = bookingRepository.findByBookingIdAndStoreId(bookingId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("账单不存在或无权访问"));
        BigDecimal total = detailRepository.findByBookingIdAndStoreId(bookingId, storeId).stream()
                .filter(item -> !"refunded".equals(item.getKitchenStatus()))
                .map(BookingDishDetail::getSubtotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deposit = Optional.ofNullable(booking.getDepositAmount()).orElse(BigDecimal.ZERO);
        BigDecimal payable = total.subtract(deposit).max(BigDecimal.ZERO);
        return Result.success(Map.of("booking_id", bookingId, "total_amount", total,
                "deposit_amount", deposit, "final_amount", payable,
                "payment_status", Objects.toString(booking.getPaymentStatus(), "unpaid")));
    }

    @Transactional
    @PostMapping("/settlement/pay")
    public Result<Map<String, Object>> pay(@RequestBody Map<String, Object> body,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                           HttpServletRequest request) {
        Long storeId = requiredStore(request);
        Long staffId = requiredStaff(request);
        String bookingId = clean(body.get("booking_id"), null);
        String payType = clean(body.get("pay_type"), null);
        if (bookingId == null || !PAY_TYPES.contains(payType)) throw new IllegalArgumentException("支付参数不完整");
        if (idempotencyKey == null || idempotencyKey.length() < 16 || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("支付幂等键无效");
        }

        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT booking_id, amount FROM ipad_payment_request WHERE store_id=? AND idempotency_key=?",
                storeId, idempotencyKey);
        if (!existing.isEmpty()) return Result.success(existing.get(0));

        List<Map<String, Object>> locked = jdbc.queryForList(
                "SELECT payment_status, deposit_amount FROM booking_master WHERE booking_id=? AND store_id=? FOR UPDATE",
                bookingId, storeId);
        if (locked.isEmpty()) throw new IllegalArgumentException("订单不存在或无权访问");
        if ("paid".equals(Objects.toString(locked.get(0).get("payment_status"), ""))) {
            throw new IllegalStateException("订单已支付，请勿重复收款");
        }

        BigDecimal total = detailRepository.findByBookingIdAndStoreId(bookingId, storeId).stream()
                .filter(item -> !"refunded".equals(item.getKitchenStatus()))
                .map(BookingDishDetail::getSubtotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deposit = locked.get(0).get("deposit_amount") instanceof BigDecimal value ? value : BigDecimal.ZERO;
        BigDecimal payable = total.subtract(deposit).max(BigDecimal.ZERO);
        BigDecimal paid = paymentTotal(body, payType);
        if (paid.compareTo(payable) < 0) throw new IllegalArgumentException("实收金额不足");

        String transNo = "POS" + storeId + System.currentTimeMillis();
        jdbc.update("INSERT INTO finance_transaction(store_id,trans_no,trans_date,trans_time,trans_type,trans_category,related_type,related_no,amount,payment_method,operator_id,remark) VALUES(?,?,CURDATE(),NOW(),'income','餐饮收款','booking',?,?,?,?,?)",
                storeId, transNo, bookingId, payable, payType, staffId.intValue(), clean(body.get("credit_account"), null));
        jdbc.update("INSERT INTO ipad_payment_request(store_id,idempotency_key,booking_id,amount,pay_type,operator_id) VALUES(?,?,?,?,?,?)",
                storeId, idempotencyKey, bookingId, payable, payType, staffId);
        jdbc.update("UPDATE booking_master SET payment_status='paid', booking_status='completed', total_amount=?, final_amount=?, updated_at=NOW() WHERE booking_id=? AND store_id=?",
                total, payable, bookingId, storeId);
        jdbc.update("UPDATE table_master t JOIN booking_table bt ON bt.table_id=t.table_id AND bt.store_id=t.store_id SET t.table_status='available', t.update_time=NOW() WHERE bt.booking_id=? AND bt.store_id=?",
                bookingId, storeId);

        return Result.success(Map.of("booking_id", bookingId, "transaction_no", transNo,
                "amount", payable, "change_amount", paid.subtract(payable)));
    }

    private BigDecimal paymentTotal(Map<String, Object> body, String payType) {
        if ("split".equals(payType)) {
            return castRows(body.get("pay_details")).stream()
                    .map(row -> decimal(row.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return decimal(body.get("pay_amount"));
    }

    private Map<String, Object> verifyOrderPassword(Long storeId, String password) {
        if (password == null || password.length() < 4 || password.length() > 100) {
            throw new SecurityException("请输入有效的员工权限密码");
        }
        List<Map<String, Object>> staff = jdbc.queryForList(
                "SELECT staff_id, staff_name, staff_password, permission_level FROM staff_master " +
                "WHERE store_id=? AND employment_status IN ('active','在职') AND COALESCE(permission_level,0) >= 1",
                storeId);
        return staff.stream().filter(item -> {
            String stored = Objects.toString(item.get("staff_password"), "");
            return (stored.startsWith("$2a$") || stored.startsWith("$2b$"))
                    ? passwordEncoder.matches(password, stored) : stored.equals(password);
        }).findFirst().orElseThrow(() -> new SecurityException("员工权限密码错误，不能下单"));
    }

    private Long requiredStore(HttpServletRequest request) {
        Object value = request.getAttribute("ipad_store_id");
        if (!(value instanceof Number number)) throw new SecurityException("未识别设备门店");
        return number.longValue();
    }

    private Long requiredStaff(HttpServletRequest request) {
        Object value = request.getAttribute("ipad_staff_id");
        if (!(value instanceof Number number)) throw new SecurityException("未识别操作员工");
        return number.longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRows(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?> map) rows.add((Map<String, Object>) map);
        return rows;
    }

    private Integer positiveInt(Object value, String message) {
        try {
            int parsed = Integer.parseInt(Objects.toString(value, "0"));
            if (parsed <= 0) throw new IllegalArgumentException(message);
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal decimal(Object value) {
        try {
            BigDecimal parsed = new BigDecimal(Objects.toString(value, "0"));
            if (parsed.signum() < 0) throw new IllegalArgumentException("支付金额不能为负数");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("支付金额格式错误");
        }
    }

    private String clean(Object value, String fallback) {
        String text = value == null ? null : value.toString().trim();
        return text == null || text.isBlank() ? fallback : text;
    }
}
