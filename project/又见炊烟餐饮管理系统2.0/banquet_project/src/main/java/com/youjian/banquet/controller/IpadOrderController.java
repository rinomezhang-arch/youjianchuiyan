package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.CustomerMaster;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.KitchenLog;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.CustomerMasterRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.KitchenLogRepository;
import com.youjian.banquet.repository.TableMasterRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private TableMasterRepository tableRepo;

    @Autowired
    private CustomerMasterRepository customerRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @GetMapping("/order/current")
    public Result<List<Map<String, Object>>> getCurrentOrder(
            @RequestParam String table_id,
            HttpServletRequest request) {
        try {
            String sql = "SELECT d.dish_booking_id, d.dish_id, d.dish_name, d.dish_quantity, " +
                         "d.unit_price, d.subtotal, d.dish_note, d.kitchen_status " +
                         "FROM booking_dish_detail d " +
                         "JOIN booking_table bt ON d.booking_id = bt.booking_id " +
                         "WHERE bt.table_id = ? " +
                         "ORDER BY d.create_time DESC";
            List<Map<String, Object>> list = entityManager.createNativeQuery(sql)
                    .setParameter(1, Integer.parseInt(table_id))
                    .unwrap(org.hibernate.query.NativeQuery.class)
                    .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE)
                    .list();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取订单失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/dish/add")
    public Result<Map<String, Object>> addDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) storeId = 1L;
            Long staffId = (Long) request.getAttribute("ipad_staff_id");
            if (staffId == null) staffId = 1L;

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

            if (tableId != null && bookingId == null) {
                String findBookingSql = "SELECT booking_id FROM booking_table WHERE table_id = ?";
                List<?> bookingResults = entityManager.createNativeQuery(findBookingSql)
                        .setParameter(1, Integer.parseInt(tableId))
                        .getResultList();
                if (!bookingResults.isEmpty()) {
                    bookingId = (String) bookingResults.get(0);
                }

                if (bookingId == null) {
                    String newBookingId = "BK" + System.currentTimeMillis();
                    BookingMaster booking = new BookingMaster();
                    booking.setBookingId(newBookingId);
                    booking.setStoreId(storeId);
                    booking.setBookingDate(LocalDate.now());
                    booking.setBookingTime(LocalTime.now());
                    booking.setBookingStatus("confirmed");
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

            if (dishId == null || dishId.isEmpty()) {
                return Result.error(400, "缺少 dish_id");
            }

            DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId)
                    .orElseThrow(() -> new RuntimeException("菜品不存在"));

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
            return Result.error(500, "加菜失败：" + e.getMessage());
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

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

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
    public Result<Map<String, Object>> removeDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

            dishDetailRepo.delete(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "删菜失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/dish/refund")
    public Result<Map<String, Object>> refundDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());
            String refundReason = (String) body.get("refund_reason");

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

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

    @PostMapping("/order/send-kitchen")
    public Result<Map<String, Object>> sendToKitchen(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) storeId = 1L;
            Long staffId = (Long) request.getAttribute("ipad_staff_id");

            String bookingId = body.get("booking_id") != null ? body.get("booking_id").toString() : null;
            String tableId = body.get("table_id") != null ? body.get("table_id").toString() : null;

            if (tableId != null && bookingId == null) {
                String findBookingSql = "SELECT booking_id FROM booking_table WHERE table_id = ?";
                List<?> bookingResults = entityManager.createNativeQuery(findBookingSql)
                        .setParameter(1, Integer.parseInt(tableId))
                        .getResultList();
                if (!bookingResults.isEmpty()) {
                    bookingId = (String) bookingResults.get(0);
                }
            }

            if (bookingId == null) {
                return Result.error(400, "缺少 booking_id 或 table_id");
            }

            List<BookingDishDetail> pendingDishes = dishDetailRepo.findByBookingId(bookingId).stream()
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
            return Result.error(500, "提交后厨失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/urgent")
    public Result<Map<String, Object>> urgentDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long dishBookingId = Long.parseLong(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

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

    /**
     * 批量提交订单（点菜→确认→选桌台→填写信息→落盘）
     * 事务性操作：创建预订 + 关联桌台 + 批量保存菜品 + 更新桌台状态 + 记录后厨日志
     */
    @PostMapping("/order/submit")
    public Result<Map<String, Object>> submitOrder(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) storeId = 1L;
            Long staffId = (Long) request.getAttribute("ipad_staff_id");
            if (staffId == null) staffId = 0L;

            // 参数校验
            Object tableIdObj = body.get("table_id");
            if (tableIdObj == null) return Result.error(400, "缺少参数 table_id");
            Integer tableId = Integer.valueOf(tableIdObj.toString());

            Object guestCountObj = body.get("guest_count");
            Integer guestCount = guestCountObj != null ? Integer.valueOf(guestCountObj.toString()) : 1;

            String customerName = body.get("customer_name") != null ? body.get("customer_name").toString() : "散客";
            String customerPhone = body.get("customer_phone") != null ? body.get("customer_phone").toString() : "13800000000";
            String bookingType = body.get("booking_type") != null ? body.get("booking_type").toString() : "normal";
            String remark = body.get("remark") != null ? body.get("remark").toString() : "";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dishes = (List<Map<String, Object>>) body.get("dishes");
            if (dishes == null || dishes.isEmpty()) {
                return Result.error(400, "购物车为空，请先点菜");
            }

            // 1. 检查桌台状态
            var tableOpt = tableRepo.findById(tableId);
            if (tableOpt.isEmpty()) {
                return Result.error(404, "桌台不存在");
            }
            var table = tableOpt.get();
            if (!"idle".equals(table.getTableStatus()) && !"available".equals(table.getTableStatus())) {
                return Result.error(400, "桌台当前状态不可用：" + table.getTableStatus());
            }

            // 2. 创建/更新客户信息 (customer_master)
            CustomerMaster customer = null;
            if (customerPhone != null && !customerPhone.isEmpty() && !"13800000000".equals(customerPhone)) {
                customer = customerRepo.findByCustomerPhoneAndStoreId(customerPhone, storeId).orElse(null);
            }
            if (customer == null && customerName != null && !customerName.isEmpty() && !"散客".equals(customerName)) {
                customer = customerRepo.findByCustomerNameAndStoreId(customerName, storeId).orElse(null);
            }
            if (customer == null) {
                // 新客户
                customer = new CustomerMaster();
                customer.setStoreId(storeId);
                customer.setCustomerName(customerName);
                customer.setCustomerPhone(customerPhone);
                customer.setTotalAmount(BigDecimal.ZERO);
                customer.setMemberLevel("v1");
                customer.setBookingCount(0);
                customer.setIsActive(1);
                customer = customerRepo.save(customer);
            }

            // 3. 创建 booking_master（关联 customer_id）
            String bookingId = "BK" + System.currentTimeMillis();
            BookingMaster booking = new BookingMaster();
            booking.setBookingId(bookingId);
            booking.setStoreId(storeId);
            booking.setBookingDate(LocalDate.now());
            booking.setBookingTime(LocalTime.now());
            booking.setCustomerName(customerName);
            booking.setCustomerPhone(customerPhone);
            if (customer != null) {
                booking.setCustomerId(customer.getCustomerId());
            }
            booking.setStaffId(staffId.intValue());
            booking.setStaffName("iPad点菜员");
            booking.setGuestCount(guestCount);
            booking.setTableCount(1);
            booking.setBookingStatus("confirmed");
            booking.setBookingType(bookingType);
            booking.setPaymentStatus("unpaid");
            booking.setRemark(remark);
            booking.setStatus("confirmed");
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            booking = bookingRepo.save(booking);

            // 3. 创建 booking_table
            BookingTable bookingTable = new BookingTable();
            bookingTable.setStoreId(storeId);
            bookingTable.setBookingId(bookingId);
            bookingTable.setBookingDate(LocalDate.now());
            bookingTable.setBookingTime(LocalTime.now());
            bookingTable.setTableId(tableId);
            bookingTable.setTableNumber(table.getTableNumber());
            bookingTable.setTableName(table.getTableName());
            bookingTable.setGuestCount(guestCount);
            bookingTable.setCreatedAt(LocalDateTime.now());
            bookingTableRepo.save(bookingTable);

            // 4. 批量保存 booking_dish_detail
            BigDecimal totalAmount = BigDecimal.ZERO;
            int dishCount = 0;
            for (Map<String, Object> d : dishes) {
                String dishId = d.get("dish_id") != null ? d.get("dish_id").toString() : null;
                if (dishId == null) continue;

                Integer qty = 1;
                Object qtyObj = d.get("dish_quantity");
                if (qtyObj instanceof Number) {
                    qty = ((Number) qtyObj).intValue();
                } else if (qtyObj != null) {
                    qty = Integer.parseInt(qtyObj.toString());
                }

                String dishNote = d.get("dish_note") != null ? d.get("dish_note").toString() : null;

                DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId)
                        .orElseThrow(() -> new RuntimeException("菜品不存在: " + dishId));

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
                detail.setDishNote(dishNote);
                detail.setKitchenStatus("submitted");
                detail.setCreatedAt(LocalDateTime.now());
                dishDetailRepo.save(detail);

                totalAmount = totalAmount.add(subtotal);
                dishCount += qty;

                // 5. 记录后厨日志
                KitchenLog log = new KitchenLog();
                log.setBookingId(bookingId);
                log.setDishId(dishId);
                log.setDishName(dish.getDishName());
                log.setStoreId(storeId);
                log.setAction("submit");
                log.setTargetType("dish");
                log.setNote("iPad 批量下单");
                if (staffId != null) log.setOperatorId(staffId.intValue());
                log.setCreatedAt(LocalDateTime.now());
                kitchenLogRepo.save(log);
            }

            // 6. 更新订单总金额
            booking.setTotalAmount(totalAmount);
            bookingRepo.save(booking);

            // 7. 更新桌台状态为占用
            table.setTableStatus("occupied");
            tableRepo.save(table);

            // 8. 更新客户累计消费、次数、最后到店日期
            if (customer != null) {
                BigDecimal prevAmount = customer.getTotalAmount() != null ? customer.getTotalAmount() : BigDecimal.ZERO;
                customer.setTotalAmount(prevAmount.add(totalAmount));
                int prevCount = customer.getBookingCount() != null ? customer.getBookingCount() : 0;
                customer.setBookingCount(prevCount + 1);
                customer.setLastBookingDate(LocalDate.now());
                customerRepo.save(customer);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("table_id", tableId);
            data.put("table_name", table.getTableName());
            data.put("guest_count", guestCount);
            data.put("customer_name", customerName);
            data.put("customer_id", customer != null ? customer.getCustomerId() : null);
            data.put("dish_count", dishCount);
            data.put("total_amount", totalAmount);
            data.put("kitchen_status", "submitted");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "提交订单失败：" + e.getMessage());
        }
    }

    // ==================== 客人自助点菜：服务员授权 + 加菜 ====================

    /** 服务员授权验证（客人在自助点菜提交时，需服务员输入账号密码） */
    @PostMapping("/auth/verify")
    public Result<Map<String, Object>> verifyStaff(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String username = body.get("username") != null ? body.get("username").toString() : null;
            String password = body.get("password") != null ? body.get("password").toString() : null;
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null && body.get("storeId") != null) {
                storeId = Long.valueOf(body.get("storeId").toString());
            }

            if (username == null || password == null) {
                return Result.error(400, "账号和密码不能为空");
            }

            String sql = "SELECT * FROM staff_master WHERE staff_account = ? AND employment_status IN ('active', '在职') LIMIT 1";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, username);

            if (list.isEmpty()) {
                return Result.error(401, "账号不存在或已停用");
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

            // 验证门店
            if (storeId != null && staff.get("store_id") != null) {
                Long staffStoreId = Long.valueOf(staff.get("store_id").toString());
                if (!storeId.equals(staffStoreId)) {
                    return Result.error(403, "该员工不属于当前门店");
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("staff_id", staff.get("staff_id"));
            data.put("staff_name", staff.get("staff_name"));
            data.put("role", staff.get("role"));
            data.put("staff_position", staff.get("staff_position"));

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "授权验证失败：" + e.getMessage());
        }
    }

    /** 加菜：在已有订单上追加菜品（客人自助点菜后由服务员授权提交） */
    @PostMapping("/order/add-dishes")
    public Result<Map<String, Object>> addDishes(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            String bookingId = body.get("booking_id") != null ? body.get("booking_id").toString() : null;
            if (bookingId == null) {
                return Result.error(400, "缺少参数 booking_id");
            }

            Object staffIdObj = body.get("staff_id");
            Integer staffId = staffIdObj != null ? Integer.valueOf(staffIdObj.toString()) : null;

            // 查订单
            BookingMaster booking = bookingRepo.findByBookingIdAndStoreId(bookingId, storeId)
                    .orElseThrow(() -> new RuntimeException("订单不存在: " + bookingId));

            // 菜品列表
            List<Map<String, Object>> dishes = (List<Map<String, Object>>) body.get("dishes");
            if (dishes == null || dishes.isEmpty()) {
                return Result.error(400, "菜品列表为空");
            }

            BigDecimal addAmount = BigDecimal.ZERO;
            int addCount = 0;

            for (Map<String, Object> d : dishes) {
                String dishId = d.get("dish_id") != null ? d.get("dish_id").toString() : null;
                int qty = d.get("dish_quantity") != null ? Integer.parseInt(d.get("dish_quantity").toString()) : 1;
                if (dishId == null || qty <= 0) continue;

                DishMaster dish = dishRepo.findById(new com.youjian.banquet.entity.DishMaster.DishMasterId(dishId, storeId)).orElse(null);
                if (dish == null) continue;

                BigDecimal price = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

                BookingDishDetail detail = new BookingDishDetail();
                detail.setBookingId(bookingId);
                detail.setDishId(dishId);
                detail.setDishName(dish.getDishName());
                detail.setDishQuantity(qty);
                detail.setUnitPrice(price);
                detail.setSubtotal(subtotal);
                detail.setKitchenStatus("submitted");
                detail.setStoreId(storeId);
                dishDetailRepo.save(detail);

                KitchenLog log = new KitchenLog();
                log.setBookingId(bookingId);
                log.setDishId(dishId);
                log.setDishName(dish.getDishName());
                log.setStoreId(storeId);
                log.setAction("submit");
                log.setTargetType("dish");
                log.setNote("iPad 客人加菜");
                if (staffId != null) log.setOperatorId(staffId);
                log.setCreatedAt(LocalDateTime.now());
                kitchenLogRepo.save(log);

                addAmount = addAmount.add(subtotal);
                addCount++;
            }

            // 更新订单总金额
            BigDecimal prevTotal = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
            booking.setTotalAmount(prevTotal.add(addAmount));
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepo.save(booking);

            // 更新客户累计消费
            final BigDecimal finalAddAmount = addAmount;
            if (booking.getCustomerId() != null) {
                customerRepo.findById(booking.getCustomerId()).ifPresent(c -> {
                    BigDecimal prev = c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO;
                    c.setTotalAmount(prev.add(finalAddAmount));
                    customerRepo.save(c);
                });
            }

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("added_dishes", addCount);
            data.put("added_amount", addAmount);
            data.put("new_total", booking.getTotalAmount());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加菜失败：" + e.getMessage());
        }
    }

    /** 获取订单详情（已点菜品列表，供客人模式页面展示） */
    @GetMapping("/order/detail")
    public Result<Map<String, Object>> getOrderDetail(
            @RequestParam String booking_id,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");

            BookingMaster booking = bookingRepo.findByBookingIdAndStoreId(booking_id, storeId)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));

            // 查桌台
            List<BookingTable> bts = bookingTableRepo.findByBookingId(booking_id);
            String tableName = bts.isEmpty() ? "" : bts.get(0).getTableName();

            // 查菜品明细
            List<BookingDishDetail> details = dishDetailRepo.findByBookingId(booking_id);
            List<Map<String, Object>> dishList = details.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("dish_booking_id", d.getDishBookingId());
                m.put("dish_id", d.getDishId());
                m.put("dish_name", d.getDishName());
                m.put("dish_quantity", d.getDishQuantity());
                m.put("unit_price", d.getUnitPrice());
                m.put("subtotal", d.getSubtotal());
                m.put("kitchen_status", d.getKitchenStatus());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", booking.getBookingId());
            data.put("customer_name", booking.getCustomerName());
            data.put("guest_count", booking.getGuestCount());
            data.put("booking_type", booking.getBookingType());
            data.put("total_amount", booking.getTotalAmount());
            data.put("table_name", tableName);
            data.put("dishes", dishList);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询订单详情失败：" + e.getMessage());
        }
    }
}