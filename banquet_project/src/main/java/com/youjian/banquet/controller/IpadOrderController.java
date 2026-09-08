package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.KitchenLog;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.KitchenLogRepository;
import com.youjian.banquet.service.NotifyPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadOrderController {

    @Autowired
    private BookingDishDetailRepository dishDetailRepo;

    @Autowired
    private BookingMasterRepository bookingRepo;

    @Autowired
    private BookingTableRepository bookingTableRepo;

    @Autowired
    private DishMasterRepository dishRepo;

    @Autowired
    private KitchenLogRepository kitchenLogRepo;

    @Autowired
    private NotifyPublisher notifyPublisher;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/order/current")
    public Result<List<Map<String, Object>>> getCurrentOrder(
            @RequestParam String table_id,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) return Result.error(401, "设备门店未验证，请重新登录");
            String mealTimeFilter = LocalTime.now().getHour() < 15
                    ? " AND bt.booking_time < '15:00:00' "
                    : " AND bt.booking_time >= '15:00:00' ";
            String sql = "SELECT d.dish_booking_id, d.dish_id, d.dish_name, d.dish_quantity, " +
                         "d.unit_price, d.subtotal, d.dish_note, d.kitchen_status " +
                         "FROM booking_dish_detail d " +
                         "JOIN booking_table bt ON d.booking_id = bt.booking_id AND d.store_id = bt.store_id " +
                         "JOIN booking_master bm ON bm.booking_id=bt.booking_id AND bm.store_id=bt.store_id " +
                         "WHERE bt.table_id = ? AND bt.store_id = ? " +
                         "AND bt.booking_date=CURRENT_DATE AND bm.booking_status NOT IN ('cancelled','completed') " +
                         mealTimeFilter +
                         "AND (d.kitchen_status IS NULL OR d.kitchen_status NOT IN ('refunded','cancelled')) " +
                         "ORDER BY d.created_at DESC";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, Integer.parseInt(table_id), storeId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取订单失败：" + e.getMessage());
        }
    }

    @Transactional
    @PostMapping("/order/dish/add")
    public Result<Map<String, Object>> addDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) return Result.error(401, "设备门店未验证，请重新登录");
            Long staffId = (Long) request.getAttribute("ipad_staff_id");
            if (staffId == null || staffId == 0L) return Result.error(401, "员工未登录，请先验证身份");

            String tableId = body.get("table_id") != null ? body.get("table_id").toString() : null;
            String bookingId = body.get("booking_id") != null ? body.get("booking_id").toString() : null;
            String dishId = body.get("dish_id") != null ? body.get("dish_id").toString() : null;
            
            Integer dishQuantity = 1;
            Object qtyObj = body.get("dish_quantity");
            if (qtyObj instanceof Number) {
                dishQuantity = ((Number) qtyObj).intValue();
            } else if (qtyObj != null) {
                dishQuantity = Integer.parseInt(qtyObj.toString());
            }
            
            String dishNote = (String) body.get("dish_note");
            if (dishQuantity < 1 || dishQuantity > 99) {
                return Result.error(400, "菜品数量必须在 1 到 99 之间");
            }

            if (dishId == null || dishId.isEmpty()) return Result.error(400, "请选择菜品后提交");
            DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId).orElse(null);
            if (dish == null) return Result.error(404, "菜品不存在或不属于当前门店，请刷新菜单");

            if (tableId != null && bookingId == null) {
                List<Map<String,Object>> selected = jdbcTemplate.queryForList(
                    "SELECT table_id FROM table_master WHERE table_id=? AND store_id=? AND is_active=1 FOR UPDATE",
                    Integer.parseInt(tableId), storeId);
                if (selected.isEmpty()) return Result.error(404, "桌台不存在或不属于当前门店");
                bookingId = findActiveBooking(tableId, storeId);

                if (bookingId == null) {
                    String newBookingId = "BK" + System.currentTimeMillis();
                    BookingMaster booking = new BookingMaster();
                    booking.setBookingId(newBookingId);
                    booking.setStoreId(storeId);
                    booking.setBookingDate(LocalDate.now());
                    booking.setBookingTime(LocalTime.now());
                    booking.setBookingStatus("confirmed");
                    booking.setGuestConfirmed(0);
                    // iPad自动建单必须有客户名,避免桌台看板出现无主单
                    // 优先从请求体取;否则用 iPad 设备员工名 + "散客" 兜底
                    String autoName = body.get("customerName") != null ? (String) body.get("customerName")
                            : body.get("customer_name") != null ? (String) body.get("customer_name")
                            : null;
                    String autoPhone = body.get("customerPhone") != null ? (String) body.get("customerPhone")
                            : body.get("customer_phone") != null ? (String) body.get("customer_phone")
                            : null;
                    if (autoName == null || autoName.trim().isEmpty()) autoName = "散客 · Walk-in";
                    if (autoPhone == null || autoPhone.trim().isEmpty()) autoPhone = "13800000000";
                    booking.setCustomerName(autoName);
                    booking.setCustomerPhone(autoPhone);
                    booking.setStaffId(staffId.intValue());
                    booking.setStaffName("iPad点菜员");
                    bookingRepo.save(booking);

                    BookingTable bookingTable = new BookingTable();
                    bookingTable.setBookingId(newBookingId);
                    bookingTable.setStoreId(storeId);
                    bookingTable.setBookingDate(LocalDate.now());
                    bookingTable.setBookingTime(LocalTime.now());
                    bookingTable.setTableId(Integer.parseInt(tableId));
                    bookingTable.setGuestCount(0);
                    bookingTableRepo.save(bookingTable);

                    bookingId = newBookingId;
                }
            }

            if (bookingId == null) {
                return Result.error(400, "缺少 booking_id 或 table_id");
            }

            BookingMaster activeBooking = bookingRepo.findForOrderUpdate(bookingId, storeId).orElse(null);
            if (activeBooking == null) return Result.error(404, "订单不存在或不属于当前门店");
            if (List.of("cancelled", "completed").contains(activeBooking.getBookingStatus())) {
                return Result.error(409, "订单已取消或已结账，请刷新桌台后重新操作");
            }

            if (dishId == null || dishId.isEmpty()) {
                return Result.error(400, "缺少 dish_id");
            }

            BigDecimal unitPrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(dishQuantity));

            BookingDishDetail detail = new BookingDishDetail();
            detail.setStoreId(storeId);
            detail.setBookingId(bookingId);
            detail.setDishId(dishId);
            detail.setDishName(dish.getDishName());
            detail.setDishQuantity(dishQuantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);
            detail.setDishNote(dishNote);
            detail.setKitchenStatus("pending");
            detail.setCreatedAt(LocalDateTime.now());

            detail = dishDetailRepo.save(detail);

            // 发布通知事件：新菜品下单 → 广播给厨房屏/管理员后台
            try {
                // 查找预订信息获取客户名
                BookingMaster bookingMaster = bookingRepo.findByBookingIdAndStoreId(bookingId, storeId).orElse(null);
                String customerName = bookingMaster != null ? bookingMaster.getCustomerName() : null;
                notifyPublisher.publish(NotifyEvent.builder()
                        .eventType(NotifyEvent.NotifyType.ORDER_CREATED)
                        .storeId(storeId)
                        .title("新菜品：" + dish.getDishName())
                        .content("桌台" + tableId + " · " + bookingId + "单 · "
                                + dish.getDishName() + " x" + dishQuantity
                                + (customerName != null ? " · 客人" + customerName : ""))
                        .priority(NotifyEvent.Priority.HIGH)
                        .senderId(staffId.intValue())
                        .senderName("iPad点菜员")
                        .receiverType(NotifyEvent.ReceiverType.ALL)
                        .relatedType("order")
                        .relatedId(detail.getDishBookingId())
                        .triggerTime(LocalDateTime.now())
                        .build());
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(IpadOrderController.class)
                        .warn("通知发布失败（不影响点菜）: {}", ex.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", detail.getDishBookingId());
            data.put("booking_id", bookingId);
            data.put("dish_id", dishId);
            data.put("dish_name", dish.getDishName());
            data.put("dish_quantity", dishQuantity);
            data.put("unit_price", unitPrice);
            data.put("subtotal", subtotal);
            data.put("kitchen_status", "pending");

            return Result.success(data);
        } catch (Exception e) {
            markRollbackOnly();
            return Result.error(500, "加菜未完成，订单没有保存，请稍后重试");
        }
    }

    /**
     * 客人自助点餐授权：验证店员账号密码，用于自助点餐终端确认加菜操作。
     * 只做身份核验，不签发 JWT，只在本店 staff_master 范围内查找（避免跨店越权）。
     */
    @PostMapping("/auth/verify")
    public Result<Map<String, Object>> authVerify(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long storeId = (Long) request.getAttribute("ipad_store_id");
        if (storeId == null) return Result.error(401, "设备门店未验证，请重新登录");
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Result.error(400, "账号和密码不能为空");
        }
        try {
            String sql = "SELECT * FROM staff_master WHERE (staff_phone = ? OR staff_account = ?) " +
                    "AND store_id = ? AND employment_status IN ('active','在职') LIMIT 1";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, username, username, storeId);
            if (list.isEmpty()) {
                return Result.error(401, "账号不存在或不属于本店");
            }
            Map<String, Object> staff = list.get(0);
            String staffPassword = (String) staff.get("staff_password");
            boolean passwordMatch = false;
            if (staffPassword != null) {
                if (staffPassword.startsWith("$2a$") || staffPassword.startsWith("$2b$")) {
                    passwordMatch = passwordEncoder.matches(password, staffPassword);
                } else {
                    passwordMatch = staffPassword.equals(password);
                }
            }
            if (!passwordMatch) {
                return Result.error(401, "密码错误");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("staff_id", staff.get("staff_id"));
            data.put("staff_name", staff.get("staff_name"));
            data.put("role", staff.get("role"));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "授权失败：" + e.getMessage());
        }
    }

    /**
     * 批量加菜：客人自助点餐授权通过后一次性提交购物车。内部复用单品加菜逻辑逐条落库。
     */
    @Transactional
    @PostMapping("/order/add-dishes")
    public Result<Map<String, Object>> addDishesBatch(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long storeId = (Long) request.getAttribute("ipad_store_id");
        if (storeId == null) return Result.error(401, "设备门店未验证，请重新登录");

        String bookingId = body.get("booking_id") != null ? body.get("booking_id").toString() : null;
        if (bookingId == null || bookingId.isEmpty()) {
            return Result.error(400, "缺少 booking_id");
        }
        Object dishesObj = body.get("dishes");
        if (!(dishesObj instanceof List) || ((List<?>) dishesObj).isEmpty()) {
            return Result.error(400, "菜品列表不能为空");
        }

        try {
            BookingMaster bookingMaster = bookingRepo.findForOrderUpdate(bookingId, storeId).orElse(null);
            if (bookingMaster == null) {
                return Result.error(404, "预订不存在");
            }
            if ("cancelled".equals(bookingMaster.getBookingStatus()) || "completed".equals(bookingMaster.getBookingStatus())) {
                return Result.error(409, "订单已取消或已结账，不能加菜");
            }
            Integer authorizedStaffId = null;
            Object staffIdObj = body.get("staff_id");
            if (staffIdObj instanceof Number) authorizedStaffId = ((Number) staffIdObj).intValue();
            else if (staffIdObj != null) {
                try { authorizedStaffId = Integer.parseInt(staffIdObj.toString()); } catch (NumberFormatException ignore) {}
            }

            int addedDishes = 0;
            BigDecimal addedAmount = BigDecimal.ZERO;
            List<String> dishNames = new ArrayList<>();

            for (Object item : (List<?>) dishesObj) {
                if (!(item instanceof Map)) throw new IllegalArgumentException("购物车中有无效菜品，请刷新后重试");
                Map<?, ?> row = (Map<?, ?>) item;
                Object dishIdObj = row.get("dish_id");
                if (dishIdObj == null) throw new IllegalArgumentException("菜品编号不能为空");
                String dishId = dishIdObj.toString();

                int qty = 1;
                Object qtyObj = row.get("dish_quantity");
                if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();
                else if (qtyObj != null) qty = Integer.parseInt(qtyObj.toString());
                if (qty < 1 || qty > 99) throw new IllegalArgumentException("菜品数量必须在 1 到 99 之间");

                DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId).orElse(null);
                if (dish == null) throw new IllegalArgumentException("购物车中有已不存在的菜品，请刷新菜单");

                BigDecimal unitPrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                BookingDishDetail detail = new BookingDishDetail();
                detail.setStoreId(storeId);
                detail.setBookingId(bookingId);
                detail.setDishId(dishId);
                detail.setDishName(dish.getDishName());
                detail.setDishQuantity(qty);
                detail.setUnitPrice(unitPrice);
                detail.setSubtotal(subtotal);
                detail.setKitchenStatus("pending");
                detail.setCreatedAt(LocalDateTime.now());
                dishDetailRepo.save(detail);

                addedDishes++;
                addedAmount = addedAmount.add(subtotal);
                dishNames.add(dish.getDishName());
            }

            if (addedDishes == 0) {
                return Result.error(400, "没有可加入的菜品（可能菜品不存在）");
            }

            try {
                notifyPublisher.publish(NotifyEvent.builder()
                        .eventType(NotifyEvent.NotifyType.ORDER_CREATED)
                        .storeId(storeId)
                        .title("客人自助加菜：" + String.join("、", dishNames))
                        .content(bookingId + "单 · 客人自助加菜 " + addedDishes + " 道 · 合计¥" + addedAmount)
                        .priority(NotifyEvent.Priority.HIGH)
                        .senderId(authorizedStaffId)
                        .senderName("客人自助点餐")
                        .receiverType(NotifyEvent.ReceiverType.ALL)
                        .relatedType("order")
                        .triggerTime(LocalDateTime.now())
                        .build());
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(IpadOrderController.class)
                        .warn("通知发布失败（不影响加菜）: {}", ex.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("added_dishes", addedDishes);
            data.put("added_amount", addedAmount);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            markRollbackOnly();
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            markRollbackOnly();
            return Result.error(500, "本次加菜未完成，全部菜品均未保存，请稍后重试");
        }
    }

    @PutMapping("/order/dish/edit")
    public Result<Map<String, Object>> editDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");

            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());
            Integer dishQuantity = Integer.parseInt(body.get("dish_quantity").toString());
            String dishNote = (String) body.get("dish_note");
            if (dishQuantity < 1 || dishQuantity > 99) {
                return Result.error(400, "菜品数量必须在 1 到 99 之间");
            }

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .filter(item -> storeId != null && storeId.equals(item.getStoreId()))
                    .orElseThrow(() -> new SecurityException("菜品明细不存在或无权操作"));
            if (!"pending".equals(detail.getKitchenStatus())) {
                return Result.error(409, "菜品已提交后厨，不能直接修改数量");
            }

            BigDecimal subtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(dishQuantity));

            detail.setDishQuantity(dishQuantity);
            detail.setDishNote(dishNote);
            detail.setSubtotal(subtotal);

            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", detail.getDishBookingId());
            data.put("dish_quantity", dishQuantity);
            data.put("subtotal", subtotal);
            data.put("dish_note", dishNote);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "改菜失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/order/dish/remove")
    @Transactional
    public Result<Map<String, Object>> removeDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .filter(item -> storeId != null && storeId.equals(item.getStoreId()))
                    .orElseThrow(() -> new SecurityException("菜品明细不存在或无权操作"));
            if (!"pending".equals(detail.getKitchenStatus())) {
                return Result.error(409, "菜品已提交后厨，请走退菜审批流程");
            }

            detail.setKitchenStatus("refunded");
            detail.setDishNote("下单前取消");
            dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);

            return Result.success(data);
        } catch (Exception e) {
            markRollbackOnly();
            return Result.error(500, "取消菜品未完成，请稍后重试");
        }
    }

    @PostMapping("/order/dish/refund")
    public Result<Map<String, Object>> refundDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());
            String refundReason = (String) body.get("refund_reason");
            if (refundReason == null || refundReason.isBlank()) {
                return Result.error(400, "退菜原因不能为空");
            }

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .filter(item -> storeId != null && storeId.equals(item.getStoreId()))
                    .orElseThrow(() -> new SecurityException("菜品明细不存在或无权操作"));
            if ("refunded".equals(detail.getKitchenStatus())) {
                return Result.error(409, "该菜品已退菜，请勿重复操作");
            }

            detail.setKitchenStatus("refunded");
            detail.setDishNote(refundReason);

            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);
            data.put("kitchen_status", "refunded");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "退菜失败：" + e.getMessage());
        }
    }

    @Transactional
    @PostMapping("/order/send-kitchen")
    public Result<Map<String, Object>> sendToKitchen(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) return Result.error(401, "设备门店未验证，请重新登录");
            Long staffId = (Long) request.getAttribute("ipad_staff_id");

            String bookingId = body.get("booking_id") != null ? body.get("booking_id").toString() : null;
            String tableId = body.get("table_id") != null ? body.get("table_id").toString() : null;

            if (tableId != null && bookingId == null) {
                bookingId = findActiveBooking(tableId, storeId);
            }

            if (bookingId == null) {
                return Result.error(400, "缺少 booking_id 或 table_id");
            }

            BookingMaster activeBooking = bookingRepo.findForOrderUpdate(bookingId, storeId).orElse(null);
            if (activeBooking == null) return Result.error(404, "订单不存在或不属于当前门店");
            if (List.of("cancelled", "completed").contains(activeBooking.getBookingStatus())) {
                return Result.error(409, "订单已取消或已结账，不能提交后厨");
            }
            List<BookingDishDetail> pendingDishes = dishDetailRepo.findByBookingIdAndStoreId(bookingId, storeId).stream()
                    .filter(d -> "pending".equals(d.getKitchenStatus()))
                    .collect(Collectors.toList());

            if (pendingDishes.isEmpty()) {
                return Result.error(400, "没有待处理的菜品");
            }

            for (BookingDishDetail detail : pendingDishes) {
                detail.setKitchenStatus("submitted");
                dishDetailRepo.save(detail);

                KitchenLog log = new KitchenLog();
                log.setBookingId(bookingId);
                log.setDishId(detail.getDishId());
                log.setDishName(detail.getDishName());
                log.setStoreId(storeId);
                log.setAction("submit");
                log.setTargetType("dish");
                log.setNote("iPad 下单到厨房");
                if (staffId != null) log.setOperatorId(staffId.intValue());
                log.setCreatedAt(LocalDateTime.now());
                kitchenLogRepo.save(log);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("dish_count", pendingDishes.size());
            data.put("kitchen_status", "submitted");

            return Result.success(data);
        } catch (Exception e) {
            markRollbackOnly();
            return Result.error(500, "提交后厨未完成，菜品状态未变更，请稍后重试");
        }
    }

    private String findActiveBooking(String tableId, Long storeId) {
        String timeFilter = LocalTime.now().getHour() < 15
                ? " AND bt.booking_time < '15:00:00'" : " AND bt.booking_time >= '15:00:00'";
        List<String> matches = jdbcTemplate.queryForList(
                "SELECT bt.booking_id FROM booking_table bt JOIN booking_master bm "
                    + "ON bm.booking_id=bt.booking_id AND bm.store_id=bt.store_id "
                    + "WHERE bt.table_id=? AND bt.store_id=? AND bt.booking_date=? "
                    + "AND bm.booking_status NOT IN ('cancelled','completed')" + timeFilter,
                String.class, Integer.parseInt(tableId), storeId, LocalDate.now());
        if (matches.size() > 1) throw new IllegalStateException("该桌台有多张活动订单，请先核对预订");
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static void markRollbackOnly() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @PostMapping("/order/urgent")
    public Result<Map<String, Object>> urgentDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .filter(item -> storeId != null && storeId.equals(item.getStoreId()))
                    .orElseThrow(() -> new SecurityException("菜品明细不存在或无权操作"));
            if (!("submitted".equals(detail.getKitchenStatus()) || "preparing".equals(detail.getKitchenStatus()))) {
                return Result.error(409, "仅已下单或制作中的菜品可以催菜");
            }

            detail.setKitchenStatus("urgent");
            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);
            data.put("kitchen_status", "urgent");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加急失败：" + e.getMessage());
        }
    }
}
