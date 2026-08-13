package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/ipad")
public class IpadGuestOrderController {
    private final JdbcTemplate jdbc;
    private final StaffMasterRepository staffRepository;
    private final DishMasterRepository dishRepository;
    private final BookingDishDetailRepository detailRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IpadGuestOrderController(JdbcTemplate jdbc,
                                    StaffMasterRepository staffRepository,
                                    DishMasterRepository dishRepository,
                                    BookingDishDetailRepository detailRepository) {
        this.jdbc = jdbc;
        this.staffRepository = staffRepository;
        this.dishRepository = dishRepository;
        this.detailRepository = detailRepository;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        Long storeId = requiredStore(request);
        String account = clean(body.get("username"));
        String password = clean(body.get("password"));
        if (account == null || password == null) {
            return Result.error(400, "请输入用户名或手机号和密码");
        }

        StaffMaster staff = staffRepository.findByAccountOrPhoneAndStoreId(account, storeId)
                .filter(item -> "active".equals(item.getEmploymentStatus()) || "在职".equals(item.getEmploymentStatus()))
                .filter(item -> passwordMatches(password, item.getStaffPassword()))
                .orElse(null);
        if (staff == null) return Result.error(401, "用户名、手机号或密码错误");

        List<Map<String, Object>> stores = jdbc.queryForList(
                "SELECT store_name FROM store_info WHERE store_id=? LIMIT 1", storeId);
        String storeName = stores.isEmpty() ? "" : Objects.toString(stores.get(0).get("store_name"), "");

        Map<String, Object> data = new HashMap<>();
        data.put("staff_id", staff.getStaffId());
        data.put("staff_name", staff.getStaffName());
        data.put("staff_account", staff.getStaffAccount());
        data.put("staff_phone", staff.getStaffPhone());
        data.put("role_type", staff.getRole());
        data.put("store_id", staff.getStoreId());
        data.put("store_name", storeName);
        data.put("device_sn", request.getHeader("X-Device-Sn"));
        return Result.success(data);
    }

    @PostMapping("/auth/verify")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long storeId = requiredStore(request);
        String username = clean(body.get("username"));
        String password = clean(body.get("password"));
        if (username == null || password == null) return Result.error(400, "请输入员工账号和权限密码");

        StaffMaster staff = staffRepository.findByStaffAccountAndStoreId(username, storeId)
                .filter(item -> "active".equals(item.getEmploymentStatus()) || "在职".equals(item.getEmploymentStatus()))
                .filter(item -> passwordMatches(password, item.getStaffPassword()))
                .orElse(null);
        if (staff == null) return Result.error(401, "员工账号或权限密码错误");

        Map<String, Object> data = new HashMap<>();
        data.put("staff_id", staff.getStaffId());
        data.put("staff_name", staff.getStaffName());
        return Result.success(data);
    }

    @GetMapping("/order/detail")
    public Result<Map<String, Object>> detail(@RequestParam("booking_id") String bookingId,
                                               HttpServletRequest request) {
        Long storeId = requiredStore(request);
        List<Map<String, Object>> bookings = jdbc.queryForList(
                "SELECT b.booking_id,b.customer_name,b.guest_count,bt.table_id,bt.table_name " +
                        "FROM booking_master b LEFT JOIN booking_table bt ON bt.booking_id=b.booking_id AND bt.store_id=b.store_id " +
                        "WHERE b.booking_id=? AND b.store_id=? LIMIT 1", bookingId, storeId);
        if (bookings.isEmpty()) return Result.error(404, "订单不存在或不属于当前门店");

        Map<String, Object> data = new HashMap<>(bookings.get(0));
        data.put("dishes", jdbc.queryForList(
                "SELECT dish_booking_id,dish_id,dish_name,dish_quantity,unit_price,subtotal,dish_note,kitchen_status " +
                        "FROM booking_dish_detail WHERE booking_id=? AND store_id=? ORDER BY created_at", bookingId, storeId));
        return Result.success(data);
    }

    @Transactional
    @PostMapping("/order/add-dishes")
    public Result<Map<String, Object>> addDishes(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        Long storeId = requiredStore(request);
        String bookingId = clean(body.get("booking_id"));
        Integer staffId = positiveInt(body.get("staff_id"));
        if (bookingId == null || staffId == null) return Result.error(400, "缺少订单或授权员工信息");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM booking_master WHERE booking_id=? AND store_id=?", Integer.class, bookingId, storeId) == 0) {
            return Result.error(404, "订单不存在或无权操作");
        }
        if (staffRepository.findByStaffIdAndStoreId(staffId, storeId).isEmpty()) return Result.error(403, "授权员工不属于当前门店");

        Object rawDishes = body.get("dishes");
        if (!(rawDishes instanceof List<?> rows) || rows.isEmpty() || rows.size() > 100) {
            return Result.error(400, "请选择菜品，单次最多100项");
        }

        BigDecimal total = BigDecimal.ZERO;
        int added = 0;
        List<BookingDishDetail> details = new ArrayList<>();
        for (Object rowObject : rows) {
            if (!(rowObject instanceof Map<?, ?> row)) return Result.error(400, "菜品数据格式错误");
            String dishId = clean(row.get("dish_id"));
            Integer quantity = positiveInt(row.get("dish_quantity"));
            if (dishId == null || quantity == null || quantity > 99) return Result.error(400, "菜品数量必须在1到99之间");
            DishMaster dish = dishRepository.findByDishIdAndStoreId(dishId, storeId).orElse(null);
            if (dish == null) return Result.error(404, "菜品不存在或已下架");

            BigDecimal unitPrice = dish.getSalePrice() == null ? BigDecimal.ZERO : dish.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BookingDishDetail detail = new BookingDishDetail();
            detail.setStoreId(storeId);
            detail.setBookingId(bookingId);
            detail.setDishId(dishId);
            detail.setDishName(dish.getDishName());
            detail.setDishQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);
            detail.setKitchenStatus("submitted");
            detail.setCreatedAt(LocalDateTime.now());
            details.add(detail);
            total = total.add(subtotal);
            added += quantity;
        }
        detailRepository.saveAll(details);

        Map<String, Object> data = new HashMap<>();
        data.put("booking_id", bookingId);
        data.put("added_dishes", added);
        data.put("added_amount", total);
        return Result.success(data);
    }

    private Long requiredStore(HttpServletRequest request) {
        Object value = request.getAttribute("ipad_store_id");
        if (!(value instanceof Number number)) throw new SecurityException("未识别当前 iPad 门店");
        return number.longValue();
    }

    private boolean passwordMatches(String raw, String encoded) {
        if (encoded == null) return false;
        return (encoded.startsWith("$2a$") || encoded.startsWith("$2b$"))
                ? passwordEncoder.matches(raw, encoded) : Objects.equals(raw, encoded);
    }

    private String clean(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer positiveInt(Object value) {
        try {
            int number = Integer.parseInt(String.valueOf(value));
            return number > 0 ? number : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
