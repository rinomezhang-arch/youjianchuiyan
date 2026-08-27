package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/api/bookings", "/menu-api/bookings"})
public class BookingController {

    @Autowired
    private BookingMasterRepository bookingMasterRepo;
    @Autowired
    private BookingTableRepository bookingTableRepo;
    @Autowired
    private BookingDishDetailRepository bookingDishDetailRepo;
    @Autowired
    private CustomerMasterRepository customerRepo;
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询接口门店过滤：店长强制查询本店，总经理可查询任意门店。
     * <p>GET 请求由 {@code StoreDataScopeAspect} 已填充 UserContext 并设置 dataScopeAll 标记，
     * 本方法据此覆盖客户端传入的 storeId，防止店长越权查询其他门店预订。
     */
    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ===== Booking Master =====

    @GetMapping
    public Result<List<BookingMaster>> list(@RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingMasterRepo.findByStoreIdOrderByBookingDateDesc(storeId));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listWithFilters(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            // 店长强制查询本店
            storeId = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                "SELECT bm.*, " +
                "(SELECT COUNT(*) FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) as bt_count, " +
                "(SELECT GROUP_CONCAT(bt.table_name SEPARATOR ', ') FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) as bt_names, " +
                "(SELECT COUNT(*) FROM booking_dish_detail bd WHERE bd.booking_id=bm.booking_id AND bd.store_id=bm.store_id) as dish_count, " +
                "(SELECT GROUP_CONCAT(CONCAT(bd.dish_name, '×', bd.dish_quantity) SEPARATOR ', ') FROM booking_dish_detail bd WHERE bd.booking_id=bm.booking_id AND bd.store_id=bm.store_id) as dish_names " +
                "FROM booking_master bm WHERE bm.store_id=?");
            List<Object> params = new ArrayList<>();
            params.add(storeId);

            if (date != null && !date.isEmpty()) {
                sql.append(" AND bm.booking_date=?");
                params.add(java.sql.Date.valueOf(date));
            }
            if (time != null && !time.isEmpty()) {
                if (time.contains("午餐")) {
                    sql.append(" AND bm.booking_time < '15:00:00'");
                } else if (time.contains("晚餐")) {
                    sql.append(" AND bm.booking_time >= '15:00:00'");
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (bm.customer_name LIKE ? OR bm.customer_phone LIKE ?)");
                String kw = "%" + keyword + "%";
                params.add(kw);
                params.add(kw);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND bm.booking_status=?");
                params.add(status);
            }

            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") as sub";
            Integer total = jdbc.queryForObject(countSql, Integer.class, params.toArray());
            if (total == null) total = 0;

            // Add pagination
            sql.append(" ORDER BY bm.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> rawRows = jdbc.queryForList(sql.toString(), params.toArray());
            // 将 snake_case 字段名转换为 camelCase，匹配前端 Bookings.vue 模板
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> raw : rawRows) {
                Map<String, Object> mapped = new HashMap<>();
                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    Object val = e.getValue();
                    String key = e.getKey();
                    String camel = toCamelCase(key);
                    mapped.put(key, val);
                    mapped.put(camel, val);
                }
                // 派生字段：桌台名称列表、区域
                Object tNames = mapped.get("bt_names");
                if (tNames != null) {
                    mapped.put("tableNames", tNames);
                }
                Object bCount = mapped.get("bt_count");
                if (bCount != null) {
                    mapped.put("tableCount", bCount);
                }
                Object dCount = mapped.get("dish_count");
                if (dCount != null) {
                    mapped.put("dishCount", dCount);
                }
                Object dNames = mapped.get("dish_names");
                if (dNames != null) {
                    mapped.put("dishNames", dNames);
                }
                // 时段标签：午餐/晚餐
                Object bTime = mapped.get("booking_time");
                if (bTime != null) {
                    String ts = bTime.toString();
                    if (ts.length() >= 5) {
                        int hour = Integer.parseInt(ts.substring(0, 2));
                        mapped.put("timeLabel", hour < 15 ? "午餐" : "晚餐");
                    }
                }
                // created_at 格式化
                Object createdAt = mapped.get("created_at");
                if (createdAt != null) {
                    mapped.put("createdAt", createdAt.toString());
                }
                rows.add(mapped);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rows", rows);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String date) {
        try {
            storeId = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE store_id=?");
            List<Object> params = new ArrayList<>();
            params.add(storeId);

            if (date != null && !date.isEmpty()) {
                where.append(" AND booking_date=?");
                params.add(java.sql.Date.valueOf(date));
            }

            // 基础统计
            Integer total = countOrZero("SELECT COUNT(*) FROM booking_master" + where, params);
            Integer confirmed = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_status='confirmed'", params);
            Integer pending = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_status='pending'", params);
            Integer cancelled = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_status='cancelled'", params);
            Integer completed = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_status='completed'", params);

            // 总人数
            Integer totalPeople = sumIntOrZero("SELECT COALESCE(SUM(guest_count),0) FROM booking_master" + where, params);

            // 午餐/晚餐
            Integer lunchCount = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_time < '15:00:00'", params);
            Integer dinnerCount = countOrZero("SELECT COUNT(*) FROM booking_master" + where + " AND booking_time >= '15:00:00'", params);

            Map<String, Object> data = new HashMap<>();
            // 后端字段名（老接口兼容）
            data.put("total", total);
            data.put("confirmed", confirmed);
            data.put("pending", pending);
            data.put("cancelled", cancelled);
            data.put("completed", completed);
            // 前端 Bookings.vue 统计卡片期望字段
            data.put("confirmedCount", confirmed);
            data.put("totalPeople", totalPeople);
            data.put("lunchCount", lunchCount);
            data.put("dinnerCount", dinnerCount);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询统计失败: " + e.getMessage());
        }
    }

    // ============ 工具方法 ============
    private Integer countOrZero(String sql, List<Object> params) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, params.toArray());
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
    private Integer sumIntOrZero(String sql, List<Object> params) {
        try {
            Number v = jdbc.queryForObject(sql, Number.class, params.toArray());
            return v == null ? 0 : v.intValue();
        } catch (Exception e) {
            return 0;
        }
    }
    private static String toCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                upper = true;
                continue;
            }
            if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @GetMapping(value = {"/copy", "/swap"})
    public Result<List<?>> copySwapFallback() {
        return Result.success(new ArrayList<>());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Result<Map<String, Object>>> detail(@PathVariable String bookingId,
                                                       @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        Optional<BookingMaster> master = bookingMasterRepo.findByBookingIdAndStoreId(bookingId, storeId);
        if (master.isEmpty()) return ResponseEntity.notFound().build();

        List<BookingTable> tables = bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId);
        List<BookingDishDetail> dishes = bookingDishDetailRepo.findByBookingId(bookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("booking", master.get());
        result.put("tables", tables);
        result.put("dishes", dishes);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 生成/获取该预订的客人自助确认链接。员工登录后调用，token 首次生成后固定复用。
     * "发预定信息-客人确认回执"这一步原来完全没有落地机制，只能电话/微信口头确认没有留痕；
     * 这里生成一个免登录的公开短链接，员工发给客人，客人打开确认即写回 booking_master。
     */
    @PostMapping("/{bookingId}/confirm-link")
    public Result<Map<String, Object>> getConfirmLink(@PathVariable String bookingId,
                                                        @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        Optional<BookingMaster> masterOpt = bookingMasterRepo.findByBookingIdAndStoreId(bookingId, storeId);
        if (masterOpt.isEmpty()) return Result.error(404, "预订不存在");
        BookingMaster master = masterOpt.get();
        if (master.getConfirmToken() == null || master.getConfirmToken().isBlank()) {
            master.setConfirmToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            bookingMasterRepo.save(master);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", master.getConfirmToken());
        return Result.success(data);
    }

    /**
     * 客人打开确认链接看到的预订摘要。免登录公开接口，见 WebMvcConfig 放行配置——
     * 客人手机点开链接时不可能带 JWT。只读，不暴露电话以外的敏感信息。
     */
    @GetMapping("/confirm/{token}")
    public Result<Map<String, Object>> getConfirmSummary(@PathVariable String token) {
        Optional<BookingMaster> masterOpt = bookingMasterRepo.findByConfirmToken(token);
        if (masterOpt.isEmpty()) return Result.error(404, "确认链接无效或已过期");
        BookingMaster master = masterOpt.get();
        List<BookingDishDetail> dishes = bookingDishDetailRepo.findByBookingId(master.getBookingId());

        Map<String, Object> data = new HashMap<>();
        data.put("customerName", master.getCustomerName());
        data.put("bookingDate", master.getBookingDate());
        data.put("bookingTime", master.getBookingTime());
        data.put("guestCount", master.getGuestCount());
        data.put("tableCount", master.getTableCount());
        data.put("banquetName", master.getBanquetName());
        data.put("packageName", master.getPackageName());
        data.put("depositAmount", master.getDepositAmount());
        data.put("totalAmount", master.getTotalAmount());
        data.put("specialRequest", master.getSpecialRequest());
        data.put("bookingStatus", master.getBookingStatus());
        data.put("guestConfirmed", master.getGuestConfirmed() != null && master.getGuestConfirmed() == 1);
        data.put("guestConfirmTime", master.getGuestConfirmTime());
        data.put("dishes", dishes.stream().map(d -> Map.of(
                "dishName", d.getDishName() != null ? d.getDishName() : "",
                "quantity", d.getDishQuantity() != null ? d.getDishQuantity() : 0
        )).toList());
        return Result.success(data);
    }

    /**
     * 客人点击"确认预订"。免登录公开接口，幂等——重复点击不会覆盖第一次确认时间。
     */
    @PostMapping("/confirm/{token}")
    @Transactional
    public Result<Map<String, Object>> confirmByGuest(@PathVariable String token) {
        Optional<BookingMaster> masterOpt = bookingMasterRepo.findByConfirmToken(token);
        if (masterOpt.isEmpty()) return Result.error(404, "确认链接无效或已过期");
        BookingMaster master = masterOpt.get();
        if (master.getGuestConfirmed() == null || master.getGuestConfirmed() != 1) {
            master.setGuestConfirmed(1);
            master.setGuestConfirmTime(LocalDateTime.now());
            bookingMasterRepo.save(master);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("guestConfirmTime", master.getGuestConfirmTime());
        return Result.success(data);
    }

    /**
     * 获取宴会菜单打印数据
     * @param bookingId 预订ID
     * @param lang 语言版本：cn 或 en
     * @return 宴会菜单数据（头部信息 + 菜品列表）
     */
    @GetMapping("/{bookingId}/banquet-menu")
    public Result<Map<String, Object>> getBanquetMenu(
            @PathVariable String bookingId,
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(defaultValue = "cn") String lang) {
        try {
            storeId = resolveQueryStoreId(storeId);
            Optional<BookingMaster> masterOpt = bookingMasterRepo.findByBookingIdAndStoreId(bookingId, storeId);
            if (masterOpt.isEmpty()) {
                return Result.error(404, "预订不存在");
            }
            BookingMaster master = masterOpt.get();

            // 查询预订的菜品详情
            String sql = "SELECT bd.dish_name, bd.english_name, bd.dish_quantity " +
                    "FROM booking_dish_detail bd " +
                    "WHERE bd.booking_id = ? AND bd.store_id = ? " +
                    "ORDER BY bd.dish_id";
            List<Map<String, Object>> dishes = jdbc.queryForList(sql, bookingId, storeId);

            // 构建菜单数据
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("header", Map.of(
                    "brandName", "又见炊烟私房菜",
                    "banquetName", master.getOccasionType() != null ? getBanquetName(master.getOccasionType()) : "宴会菜单",
                    "price", "5888",
                    "packageCode", "A",
                    "guestCount", master.getGuestCount() != null ? master.getGuestCount() : 12,
                    "orderNo", bookingId,
                    "tableCount", master.getTableCount() != null ? master.getTableCount() : 0
            ));
            menuData.put("dishes", dishes);
            menuData.put("footer", Map.of(
                    "address", "宁国青龙西路1号",
                    "addressEn", "No. 1 Qinglong West Road, Ningguo City",
                    "tel", "15905638866"
            ));
            menuData.put("lang", lang);

            return Result.success(menuData);
        } catch (Exception e) {
            return Result.error(500, "获取宴会菜单失败：" + e.getMessage());
        }
    }

    /**
     * 根据宴会类型获取宴会名称
     */
    private String getBanquetName(String occasionType) {
        Map<String, String> banquetNames = Map.of(
                "wedding", "龙凤呈祥宴",
                "birthday", "寿比南山宴",
                "house_move", "乔迁之喜宴",
                "promotion", "平步青云宴",
                "reunion", "阖家团圆宴",
                "thanksgiving", "感恩有你宴",
                "year_end", "财源广进宴",
                "baby_born", "金枝玉叶宴",
                "graduation", "金榜题名宴",
                "engagement", "天作之合宴"
        );
        return banquetNames.getOrDefault(occasionType, "宴会菜单");
    }

    @GetMapping("/search")
    public Result<List<BookingMaster>> search(@RequestParam(defaultValue = "1") Long storeId,
                                       @RequestParam(required = false) String keyword) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingMasterRepo.searchByKeyword(storeId, keyword));
    }

    @GetMapping("/date/{date}")
    public Result<List<BookingMaster>> byDate(@PathVariable String date,
                                       @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingDate(storeId, LocalDate.parse(date)));
    }

    @GetMapping("/range")
    public Result<List<BookingMaster>> byDateRange(@RequestParam String start,
                                            @RequestParam String end,
                                            @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingDateBetween(storeId,
                LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/status/{status}")
    public Result<List<BookingMaster>> byStatus(@PathVariable String status,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingStatus(storeId, status));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Result<BookingMaster>> create(@RequestBody Map<String, Object> body) {
        try {
            // ===== 必填校验：客户姓名 + 联系电话(双层防御) =====
            String preName = body.get("customerName") != null ? (String) body.get("customerName") : (String) body.get("customer_name");
            String prePhone = body.get("customerPhone") != null ? (String) body.get("customerPhone") : (String) body.get("customer_phone");
            if (preName == null || preName.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "客户姓名不能为空 · customerName is required"));
            }
            if (prePhone == null || prePhone.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "联系电话不能为空 · customerPhone is required"));
            }
            if (!prePhone.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error(400, "联系电话格式错误 · customerPhone format invalid"));
            }

            // 自动绑定当前用户门店：店长强制绑定本店，总经理允许指定
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            Long effectiveStoreId;
            if (UserContext.isDataScopeAll()) {
                // 总经理可指定门店，未指定默认 1
                Object storeIdObj = body.get("storeId");
                if (storeIdObj == null) storeIdObj = body.get("store_id");
                if (storeIdObj == null) storeIdObj = 1;
                effectiveStoreId = Long.valueOf(storeIdObj.toString());
            } else {
                // 店长强制绑定本门店，忽略请求体中的 storeId
                if (currentStoreId == null) {
                    return ResponseEntity.ok(Result.error(403, "未识别到当前用户门店，禁止创建预订"));
                }
                effectiveStoreId = currentStoreId;
            }

            BookingMaster booking = new BookingMaster();
            String bookingId = "BK" + System.currentTimeMillis();
            booking.setBookingId(bookingId);

            // storeId（已根据当前用户身份兜底绑定）
            booking.setStoreId(effectiveStoreId);

            // bookingDate
            Object bookingDateObj = body.get("bookingDate");
            if (bookingDateObj == null) bookingDateObj = body.get("booking_date");
            if (bookingDateObj != null) {
                booking.setBookingDate(LocalDate.parse(bookingDateObj.toString()));
            } else {
                booking.setBookingDate(LocalDate.now());
            }

            // bookingTime
            Object bookingTimeObj = body.get("bookingTime");
            if (bookingTimeObj == null) bookingTimeObj = body.get("booking_time");
            if (bookingTimeObj != null) {
                String timeStr = bookingTimeObj.toString();
                if (timeStr.length() == 5) timeStr += ":00";
                booking.setBookingTime(java.time.LocalTime.parse(timeStr));
            } else {
                booking.setBookingTime(java.time.LocalTime.of(18, 0));
            }

            // 客户信息
            String customerName = body.get("customerName") != null ? (String) body.get("customerName") : (String) body.get("customer_name");
            String customerPhone = body.get("customerPhone") != null ? (String) body.get("customerPhone") : (String) body.get("customer_phone");
            booking.setCustomerName(customerName);
            booking.setCustomerPhone(customerPhone);

            // 自动录入客户资料并回写统计
            if (customerPhone != null && !customerPhone.isEmpty()) {
                Integer customerId = upsertCustomer(booking.getStoreId(), customerName, customerPhone);
                if (customerId != null) {
                    booking.setCustomerId(customerId);
                }
            }

            // 其他字段
            Object customerIdObj = body.get("customerId");
            if (customerIdObj == null) customerIdObj = body.get("customer_id");
            if (customerIdObj != null && booking.getCustomerId() == null) {
                booking.setCustomerId(Integer.valueOf(customerIdObj.toString()));
            }

            Object staffIdObj = body.get("staffId");
            if (staffIdObj == null) staffIdObj = body.get("staff_id");
            if (staffIdObj != null)
                booking.setStaffId(Integer.valueOf(staffIdObj.toString()));
            booking.setStaffName(body.get("staffName") != null ? (String) body.get("staffName") : (String) body.get("staff_name"));

            Object guestCountObj = body.get("guestCount");
            if (guestCountObj == null) guestCountObj = body.get("guest_count");
            if (guestCountObj != null)
                booking.setGuestCount(Integer.valueOf(guestCountObj.toString()));

            Object tableCountObj = body.get("tableCount");
            if (tableCountObj == null) tableCountObj = body.get("table_count");
            if (tableCountObj != null)
                booking.setTableCount(Integer.valueOf(tableCountObj.toString()));

            Object spareTablesObj = body.get("spareTables");
            if (spareTablesObj == null) spareTablesObj = body.get("spare_tables");
            if (spareTablesObj != null)
                booking.setSpareTables(Integer.valueOf(spareTablesObj.toString()));

            Object guestPerTableObj = body.get("guestPerTable");
            if (guestPerTableObj == null) guestPerTableObj = body.get("guest_per_table");
            if (guestPerTableObj != null)
                booking.setGuestPerTable(Integer.valueOf(guestPerTableObj.toString()));

            String bookingStatus = body.get("bookingStatus") != null ? (String) body.get("bookingStatus") : (String) body.getOrDefault("booking_status", "confirmed");
            booking.setBookingStatus(bookingStatus);

            booking.setBanquetName(body.get("banquetName") != null ? (String) body.get("banquetName") : (String) body.get("banquet_name"));
            booking.setOccasionType(body.get("occasionType") != null ? (String) body.get("occasionType") : (String) body.get("occasion_type"));
            booking.setSpecialRequest(body.get("specialRequest") != null ? (String) body.get("specialRequest") : (String) body.get("special_request"));
            booking.setRemark(body.get("remark") != null ? (String) body.get("remark") : "");

            String paymentStatus = body.get("paymentStatus") != null ? (String) body.get("paymentStatus") : (String) body.getOrDefault("payment_status", "unpaid");
            booking.setPaymentStatus(paymentStatus);

            // deposit 字段读取（防御 snake_case/camelCase）
            Object depositObj = body.get("deposit");
            if (depositObj == null) depositObj = body.get("Deposit");
            if (depositObj != null) {
                try {
                    booking.setDepositAmount(new java.math.BigDecimal(depositObj.toString()));
                } catch (Exception e) {
                    // ignore parse error
                }
            }

            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            BookingMaster saved = bookingMasterRepo.save(booking);

            // 处理桌台
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tables = (List<Map<String, Object>>) body.get("tables");
            if (tables == null || tables.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Object> tableIdsRaw = (List<Object>) body.get("table_ids");
                @SuppressWarnings("unchecked")
                List<Object> tableNamesRaw = (List<Object>) body.get("table_names");
                if (tableIdsRaw != null && !tableIdsRaw.isEmpty()) {
                    tables = new ArrayList<>();
                    for (int i = 0; i < tableIdsRaw.size(); i++) {
                        Map<String, Object> t = new HashMap<>();
                        t.put("table_id", tableIdsRaw.get(i));
                        t.put("tableId", tableIdsRaw.get(i));
                        if (tableNamesRaw != null && i < tableNamesRaw.size()) {
                            t.put("table_name", tableNamesRaw.get(i));
                            t.put("tableName", tableNamesRaw.get(i));
                            t.put("table_number", tableNamesRaw.get(i));
                            t.put("tableNumber", tableNamesRaw.get(i));
                        }
                        tables.add(t);
                    }
                }
            }

            if (tables != null) {
                for (Map<String, Object> t : tables) {
                    BookingTable bt = new BookingTable();
                    bt.setStoreId(booking.getStoreId());
                    bt.setBookingId(bookingId);
                    bt.setBookingDate(booking.getBookingDate());
                    bt.setBookingTime(booking.getBookingTime());

                    Object tableIdObj2 = t.get("tableId");
                    if (tableIdObj2 == null) tableIdObj2 = t.get("table_id");
                    if (tableIdObj2 != null)
                        bt.setTableId(Integer.valueOf(tableIdObj2.toString()));

                    bt.setTableNumber(t.get("tableNumber") != null ? (String) t.get("tableNumber") : (String) t.get("table_number"));
                    bt.setTableName(t.get("tableName") != null ? (String) t.get("tableName") : (String) t.get("table_name"));

                    Object tGuestCountObj = t.get("guestCount");
                    if (tGuestCountObj == null) tGuestCountObj = t.get("guest_count");
                    if (tGuestCountObj != null)
                        bt.setGuestCount(Integer.valueOf(tGuestCountObj.toString()));

                    bt.setPackageId(t.get("packageId") != null ? (String) t.get("packageId") : (String) t.get("package_id"));
                    bt.setPackageName(t.get("packageName") != null ? (String) t.get("packageName") : (String) t.get("package_name"));
                    bt.setOpenTableType(t.get("openTableType") != null ? (String) t.get("openTableType") : (String) t.get("open_table_type"));
                    bt.setTableNote(t.get("tableNote") != null ? (String) t.get("tableNote") : (String) t.get("table_note"));
                    bt.setCreatedAt(LocalDateTime.now());
                    bookingTableRepo.save(bt);

                    // 更新桌台状态
                    Object updateTableIdObj = t.get("tableId");
                    if (updateTableIdObj == null) updateTableIdObj = t.get("table_id");
                    if (updateTableIdObj != null) {
                        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                                updateTableIdObj, booking.getStoreId());
                    }
                }
            }

            return ResponseEntity.ok(Result.success(saved));
        } catch (Exception e) {
            e.printStackTrace();
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return ResponseEntity.ok(Result.error(500, "创建预订失败: " + e.getMessage()));
        }
    }

    // 自动录入客户资料并回写统计
    private Integer upsertCustomer(Long storeId, String name, String phone) {
        try {
            // 查找是否已有该客户
            List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT customer_id, booking_count FROM customer_master WHERE customer_phone=? AND store_id=?",
                phone, storeId);

            if (!existing.isEmpty()) {
                Integer customerId = Integer.valueOf(existing.get(0).get("customer_id").toString());
                // 更新统计：booking_count+1, last_booking_date=今天
                jdbc.update("UPDATE customer_master SET booking_count=booking_count+1, last_booking_date=?, customer_name=COALESCE(?, customer_name), update_time=NOW() WHERE customer_id=?",
                    LocalDate.now(), name != null ? name : null, customerId);
                return customerId;
            } else {
                // 新客户：自动创建
                jdbc.update("INSERT INTO customer_master (store_id, customer_name, customer_phone, total_amount, member_level, booking_count, last_booking_date, is_active, create_time, update_time) VALUES (?, ?, ?, 0, 'v1', 1, ?, 1, NOW(), NOW())",
                    storeId, name != null ? name : "客户", phone, LocalDate.now());
                // 获取新创建的customer_id
                List<Map<String, Object>> newCustomer = jdbc.queryForList(
                    "SELECT customer_id FROM customer_master WHERE customer_phone=? AND store_id=? ORDER BY customer_id DESC LIMIT 1",
                    phone, storeId);
                if (!newCustomer.isEmpty()) {
                    return Integer.valueOf(newCustomer.get(0).get("customer_id").toString());
                }
            }
        } catch (Exception e) {
            System.out.println("=== upsertCustomer error: " + e.getMessage());
        }
        return null;
    }

    @PutMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<Result<BookingMaster>> update(@PathVariable String bookingId,
                                                 @RequestBody Map<String, Object> body) {
        // 店长仅可操作本店预订，总经理可跨店
        Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
        Long storeId = Long.valueOf(body.getOrDefault("storeId", body.getOrDefault("store_id", 1)).toString());
        if (!UserContext.isDataScopeAll() && (currentStoreId == null || !currentStoreId.equals(storeId))) {
            return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
        }
        Optional<BookingMaster> opt = bookingMasterRepo.findByBookingIdAndStoreId(bookingId, storeId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BookingMaster b = opt.get();

        // ===== 必填校验：更新时禁止把客户名/电话改成空 =====
        if (body.containsKey("customerName") || body.containsKey("customer_name")) {
            String newName = body.get("customerName") != null ? (String) body.get("customerName") : (String) body.get("customer_name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "客户姓名不能为空 · customerName is required"));
            }
        }
        if (body.containsKey("customerPhone") || body.containsKey("customer_phone")) {
            String newPhone = body.get("customerPhone") != null ? (String) body.get("customerPhone") : (String) body.get("customer_phone");
            if (newPhone == null || newPhone.trim().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "联系电话不能为空 · customerPhone is required"));
            }
            if (!newPhone.matches("^1[3-9]\\d{9}$")) {
                return ResponseEntity.ok(Result.error(400, "联系电话格式错误 · customerPhone format invalid"));
            }
        }
        Object bookingDateObj = body.get("bookingDate");
        if (bookingDateObj == null) bookingDateObj = body.get("booking_date");
        if (bookingDateObj != null) b.setBookingDate(LocalDate.parse(bookingDateObj.toString()));

        Object bookingTimeObj = body.get("bookingTime");
        if (bookingTimeObj == null) bookingTimeObj = body.get("booking_time");
        if (bookingTimeObj != null) {
            String t = bookingTimeObj.toString();
            if (t.length() == 5) t += ":00";
            b.setBookingTime(java.time.LocalTime.parse(t));
        }

        if (body.get("customerName") != null) b.setCustomerName((String) body.get("customerName"));
        else if (body.get("customer_name") != null) b.setCustomerName((String) body.get("customer_name"));

        if (body.get("customerPhone") != null) b.setCustomerPhone((String) body.get("customerPhone"));
        else if (body.get("customer_phone") != null) b.setCustomerPhone((String) body.get("customer_phone"));

        Object guestCountObj = body.get("guestCount");
        if (guestCountObj == null) guestCountObj = body.get("guest_count");
        if (guestCountObj != null) b.setGuestCount(Integer.valueOf(guestCountObj.toString()));

        Object tableCountObj = body.get("tableCount");
        if (tableCountObj == null) tableCountObj = body.get("table_count");
        if (tableCountObj != null) b.setTableCount(Integer.valueOf(tableCountObj.toString()));

        if (body.get("bookingStatus") != null) b.setBookingStatus((String) body.get("bookingStatus"));
        else if (body.get("booking_status") != null) b.setBookingStatus((String) body.get("booking_status"));

        if (body.get("banquetName") != null) b.setBanquetName((String) body.get("banquetName"));
        else if (body.get("banquet_name") != null) b.setBanquetName((String) body.get("banquet_name"));

        if (body.get("occasionType") != null) b.setOccasionType((String) body.get("occasionType"));
        else if (body.get("occasion_type") != null) b.setOccasionType((String) body.get("occasion_type"));

        if (body.get("specialRequest") != null) b.setSpecialRequest((String) body.get("specialRequest"));
        else if (body.get("special_request") != null) b.setSpecialRequest((String) body.get("special_request"));

        if (body.get("remark") != null) b.setRemark((String) body.get("remark"));
        if (body.get("paymentStatus") != null) b.setPaymentStatus((String) body.get("paymentStatus"));
        else if (body.get("payment_status") != null) b.setPaymentStatus((String) body.get("payment_status"));

        // deposit 字段更新
        Object depositObj = body.get("deposit");
        if (depositObj == null) depositObj = body.get("Deposit");
        if (depositObj != null) {
            try {
                b.setDepositAmount(new java.math.BigDecimal(depositObj.toString()));
            } catch (Exception e) {
                // ignore parse error
            }
        }

        b.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(Result.success(bookingMasterRepo.save(b)));
    }

    @DeleteMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable String bookingId,
                                     @RequestParam(defaultValue = "1") Long storeId) {
        // 店长仅可删除本店预订，总经理可跨店
        Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll() && (currentStoreId == null || !currentStoreId.equals(storeId))) {
            return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
        }
        // 获取该预订关联的桌台
        List<Map<String, Object>> tables = jdbc.queryForList(
            "SELECT table_id FROM booking_table WHERE booking_id=? AND store_id=?", bookingId, storeId);

        // 获取预订日期
        List<Map<String, Object>> bookingInfo = jdbc.queryForList(
            "SELECT booking_date FROM booking_master WHERE booking_id=? AND store_id=?", bookingId, storeId);
        LocalDate bookingDate = null;
        if (!bookingInfo.isEmpty()) {
            bookingDate = LocalDate.parse(bookingInfo.get(0).get("booking_date").toString().substring(0, 10));
        }

        // 删除预订相关数据
        bookingDishDetailRepo.deleteByBookingId(bookingId);
        bookingTableRepo.deleteByBookingId(bookingId);
        bookingMasterRepo.deleteByBookingIdAndStoreId(bookingId, storeId);

        // 恢复桌台状态：检查该桌台在该日期是否还有其他预订
        for (Map<String, Object> t : tables) {
            Long tableId = Long.valueOf(t.get("table_id").toString());
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?",
                Integer.class, tableId, storeId);
            if (count == null || count == 0) {
                // 没有任何预订关联了，恢复为可用
                jdbc.update("UPDATE table_master SET table_status='idle' WHERE table_id=? AND store_id=?", tableId, storeId);
            }
        }
        return ResponseEntity.ok(Result.success(Map.of("deleted", true, "bookingId", bookingId)));
    }

    // ===== 复制预订 =====
    @PostMapping("/copy")
    @Transactional
    public ResponseEntity<Result<Map<String, Object>>> copyBooking(@RequestBody Map<String, Object> body) {
        try {
            // 防御 snake_case / camelCase
            Object sourceObj = body.get("sourceBookingId");
            if (sourceObj == null) sourceObj = body.get("source_booking_id");
            String sourceBookingId = sourceObj != null ? sourceObj.toString() : null;
            if (sourceBookingId == null) return ResponseEntity.ok(Result.error(400, "缺少 sourceBookingId"));

            @SuppressWarnings("unchecked")
            List<Object> targetTableIds = (List<Object>) body.get("targetTableIds");
            if (targetTableIds == null) targetTableIds = (List<Object>) body.get("target_table_ids");
            if (targetTableIds == null || targetTableIds.isEmpty()) return ResponseEntity.ok(Result.error(400, "缺少 targetTableIds"));

            String date = body.get("date") != null ? body.get("date").toString() : null;
            String time = body.get("time") != null ? body.get("time").toString() : null;
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            Long storeId = storeIdObj != null ? Long.valueOf(storeIdObj.toString()) : 1L;

            // 店长仅可复制本店预订，总经理可跨店
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll() && (currentStoreId == null || !currentStoreId.equals(storeId))) {
                return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
            }

            // 获取源预订
            Optional<BookingMaster> sourceOpt = bookingMasterRepo.findByBookingIdAndStoreId(sourceBookingId, storeId);
            if (sourceOpt.isEmpty()) return ResponseEntity.ok(Result.error(404, "源预订不存在"));

            BookingMaster source = sourceOpt.get();
            List<Map<String, Object>> createdBookings = new ArrayList<>();

            // 为每个目标桌台创建新预订
            for (Object tableIdObj : targetTableIds) {
                Integer tableId = Integer.valueOf(tableIdObj.toString());
                String newBookingId = "BK" + System.currentTimeMillis() + "_" + tableId;

                BookingMaster newBooking = new BookingMaster();
                newBooking.setBookingId(newBookingId);
                newBooking.setStoreId(storeId);
                newBooking.setBookingDate(date != null ? LocalDate.parse(date) : source.getBookingDate());
                if (time != null) {
                    String t = time.length() == 5 ? time + ":00" : time;
                    newBooking.setBookingTime(java.time.LocalTime.parse(t));
                } else {
                    newBooking.setBookingTime(source.getBookingTime());
                }
                newBooking.setCustomerId(source.getCustomerId());
                newBooking.setCustomerName(source.getCustomerName());
                newBooking.setCustomerPhone(source.getCustomerPhone());
                newBooking.setStaffId(source.getStaffId());
                newBooking.setStaffName(source.getStaffName());
                newBooking.setGuestCount(source.getGuestCount());
                newBooking.setTableCount(1);
                newBooking.setSpareTables(0);
                newBooking.setGuestPerTable(source.getGuestPerTable());
                newBooking.setBookingStatus("confirmed");
                newBooking.setBanquetName(source.getBanquetName());
                newBooking.setOccasionType(source.getOccasionType());
                newBooking.setSpecialRequest(source.getSpecialRequest());
                newBooking.setRemark(source.getRemark());
                newBooking.setPaymentStatus("unpaid");
                newBooking.setCreatedAt(LocalDateTime.now());
                newBooking.setUpdatedAt(LocalDateTime.now());
                bookingMasterRepo.save(newBooking);

                // 创建桌台关联
                BookingTable bt = new BookingTable();
                bt.setStoreId(storeId);
                bt.setBookingId(newBookingId);
                bt.setBookingDate(newBooking.getBookingDate());
                bt.setBookingTime(newBooking.getBookingTime());
                bt.setTableId(tableId);

                // 获取桌台名称
                try {
                    Map<String, Object> tableInfo = jdbc.queryForMap("SELECT table_number, table_name FROM table_master WHERE table_id=? AND store_id=?", tableId, storeId);
                    bt.setTableNumber((String) tableInfo.get("table_number"));
                    bt.setTableName((String) tableInfo.get("table_name"));
                } catch (Exception e) {
                    bt.setTableNumber("");
                    bt.setTableName("");
                }
                bt.setCreatedAt(LocalDateTime.now());
                bookingTableRepo.save(bt);

                // 更新桌台状态
                jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?", tableId, storeId);

                // 更新客户统计
                if (newBooking.getCustomerPhone() != null) {
                    upsertCustomer(storeId, newBooking.getCustomerName(), newBooking.getCustomerPhone());
                }

                createdBookings.add(Map.of("bookingId", newBookingId, "tableId", tableId));
            }

            return ResponseEntity.ok(Result.success(Map.of("copied", true, "bookings", createdBookings)));
        } catch (Exception e) {
            e.printStackTrace();
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return ResponseEntity.ok(Result.error(500, "复制预订失败: " + e.getMessage()));
        }
    }

    // ===== 互换预订 =====
    @PostMapping("/swap")
    @Transactional
    public ResponseEntity<Result<Map<String, Object>>> swapBooking(@RequestBody Map<String, Object> body) {
        try {
            // 防御 snake_case / camelCase
            Object fromObj = body.get("fromTableId");
            if (fromObj == null) fromObj = body.get("from_table_id");
            if (fromObj == null) return ResponseEntity.ok(Result.error(400, "缺少 fromTableId"));
            Integer fromTableId = Integer.valueOf(fromObj.toString());

            Object toObj = body.get("toTableId");
            if (toObj == null) toObj = body.get("to_table_id");
            if (toObj == null) return ResponseEntity.ok(Result.error(400, "缺少 toTableId"));
            Integer toTableId = Integer.valueOf(toObj.toString());

            String date = body.get("date") != null ? body.get("date").toString() : null;
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            Long storeId = storeIdObj != null ? Long.valueOf(storeIdObj.toString()) : 1L;

            // 店长仅可互换本店桌台，总经理可跨店
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll() && (currentStoreId == null || !currentStoreId.equals(storeId))) {
                return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
            }

            // 查找两个桌台在该日期的预订（包含 booking_time 以便后续恢复）
            List<Map<String, Object>> fromBookings = jdbc.queryForList(
                "SELECT booking_id, booking_time FROM booking_table WHERE table_id=? AND store_id=? AND booking_date=?",
                fromTableId, storeId, date);
            List<Map<String, Object>> toBookings = jdbc.queryForList(
                "SELECT booking_id FROM booking_table WHERE table_id=? AND store_id=? AND booking_date=?",
                toTableId, storeId, date);

            // 预取两个桌台的名称信息
            Map<String, Object> fromTableInfo;
            Map<String, Object> toTableInfo;
            try {
                fromTableInfo = jdbc.queryForMap("SELECT table_number, table_name FROM table_master WHERE table_id=? AND store_id=?", fromTableId, storeId);
                toTableInfo = jdbc.queryForMap("SELECT table_number, table_name FROM table_master WHERE table_id=? AND store_id=?", toTableId, storeId);
            } catch (Exception e) {
                return ResponseEntity.ok(Result.error(404, "桌台不存在: " + e.getMessage()));
            }

            // 三步法交换桌台，避免唯一键 uk_table_date_time 冲突：
            // 利用 MySQL 唯一键中 NULL 不参与比较的特性，临时把 fromBookings 的 booking_time 置 NULL。
            // Step 1: fromBookings 的 booking_time 置 NULL（移出唯一键检查）
            for (Map<String, Object> fb : fromBookings) {
                String bookingId = fb.get("booking_id").toString();
                jdbc.update("UPDATE booking_table SET booking_time=NULL WHERE booking_id=? AND store_id=?",
                    bookingId, storeId);
            }
            // Step 2: toBookings 改到 fromTableId（此时 fromTableId 在原时间段已空闲）
            if (!toBookings.isEmpty()) {
                jdbc.update("UPDATE booking_table SET table_id=?, table_number=?, table_name=? WHERE table_id=? AND store_id=? AND booking_date=?",
                    fromTableId, fromTableInfo.get("table_number"), fromTableInfo.get("table_name"),
                    toTableId, storeId, date);
            }
            // Step 3: fromBookings（booking_time 为 NULL）改到 toTableId 并恢复原 booking_time
            for (Map<String, Object> fb : fromBookings) {
                String bookingId = fb.get("booking_id").toString();
                Object originalTime = fb.get("booking_time");
                jdbc.update("UPDATE booking_table SET table_id=?, table_number=?, table_name=?, booking_time=? WHERE booking_id=? AND store_id=?",
                    toTableId, toTableInfo.get("table_number"), toTableInfo.get("table_name"),
                    originalTime, bookingId, storeId);
            }

            return ResponseEntity.ok(Result.success(Map.of("swapped", true)));
        } catch (Exception e) {
            e.printStackTrace();
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return ResponseEntity.ok(Result.error(500, "互换预订失败: " + e.getMessage()));
        }
    }

    // ===== Booking Tables =====

    @GetMapping("/{bookingId}/tables")
    public Result<List<BookingTable>> getTables(@PathVariable String bookingId,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId));
    }

    @PostMapping("/{bookingId}/tables")
    @Transactional
    public Result<BookingTable> addTable(@PathVariable String bookingId,
                                  @RequestBody BookingTable table) {
        // 店长仅可操作本店预订，总经理可跨店
        Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
        // 查询预订所属门店及日期/时间，用于兜底填充 booking_date/booking_time（数据库 NOT NULL）
        List<Map<String, Object>> bookingInfo = jdbc.queryForList(
            "SELECT store_id, booking_date, booking_time FROM booking_master WHERE booking_id=?", bookingId);
        Long bookingStoreId = bookingInfo.isEmpty() ? null
            : Long.valueOf(bookingInfo.get(0).get("store_id").toString());
        if (!UserContext.isDataScopeAll()) {
            if (currentStoreId == null || !currentStoreId.equals(bookingStoreId)) {
                return Result.error(403, "无权限：仅可操作本店预订");
            }
            // 强制绑定本店，忽略请求体中的 storeId
            table.setStoreId(currentStoreId);
        } else if (table.getStoreId() == null && bookingStoreId != null) {
            // 总经理未指定门店时，使用预订所属门店
            table.setStoreId(bookingStoreId);
        }
        table.setBookingId(bookingId);
        // 兜底填充 booking_date/booking_time，避免数据库 NOT NULL 约束报错
        if (table.getBookingDate() == null && !bookingInfo.isEmpty() && bookingInfo.get(0).get("booking_date") != null) {
            try {
                table.setBookingDate(LocalDate.parse(bookingInfo.get(0).get("booking_date").toString().substring(0, 10)));
            } catch (Exception ignore) {
                table.setBookingDate(LocalDate.now());
            }
        }
        if (table.getBookingTime() == null && !bookingInfo.isEmpty() && bookingInfo.get(0).get("booking_time") != null) {
            try {
                table.setBookingTime(java.time.LocalTime.parse(bookingInfo.get(0).get("booking_time").toString()));
            } catch (Exception ignore) {
                table.setBookingTime(java.time.LocalTime.of(18, 0));
            }
        }
        if (table.getBookingDate() == null) table.setBookingDate(LocalDate.now());
        if (table.getBookingTime() == null) table.setBookingTime(java.time.LocalTime.of(18, 0));
        table.setCreatedAt(LocalDateTime.now());
        BookingTable saved = bookingTableRepo.save(table);
        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                table.getTableId(), table.getStoreId());
        return Result.success(saved);
    }

    @DeleteMapping("/{bookingId}/tables/{tableBookingId}")
    @Transactional
    public ResponseEntity<?> deleteTable(@PathVariable String bookingId, @PathVariable Long tableBookingId) {
        // 存在性检查：删除不存在的记录返回 404，避免 queryForMap 抛 EmptyResultDataAccessException
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT table_id, store_id FROM booking_table WHERE table_booking_id=?", tableBookingId);
        if (rows.isEmpty()) {
            return ResponseEntity.ok(Result.error(404, "预订桌台记录不存在: " + tableBookingId));
        }
        Map<String, Object> tableInfo = rows.get(0);
        Long tableId = Long.valueOf(tableInfo.get("table_id").toString());
        Long storeId = Long.valueOf(tableInfo.get("store_id").toString());

        // 店长仅可删除本店预订关联的桌台，总经理可跨店
        Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll() && (currentStoreId == null || !currentStoreId.equals(storeId))) {
            return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
        }

        bookingTableRepo.deleteById(tableBookingId);

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
        if (count == null || count == 0) {
            jdbc.update("UPDATE table_master SET table_status='idle' WHERE table_id=? AND store_id=?", tableId, storeId);
        }
        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }

    // ===== Booking Dishes =====

    @GetMapping("/{bookingId}/dishes")
    public Result<List<BookingDishDetail>> getDishes(@PathVariable String bookingId,
                                              @RequestParam(defaultValue = "1") Long storeId) {
        storeId = resolveQueryStoreId(storeId);
        return Result.success(bookingDishDetailRepo.findByBookingIdAndStoreId(bookingId, storeId));
    }

    @PostMapping("/{bookingId}/dishes")
    @Transactional
    public Result<BookingDishDetail> addDish(@PathVariable String bookingId,
                                      @RequestBody Map<String, Object> body) {
        try {
            // 店长仅可操作本店预订，总经理可跨店
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            List<Map<String, Object>> bookingInfo = jdbc.queryForList(
                "SELECT store_id FROM booking_master WHERE booking_id=?", bookingId);
            Long bookingStoreId = bookingInfo.isEmpty() ? null
                : Long.valueOf(bookingInfo.get(0).get("store_id").toString());
            if (!UserContext.isDataScopeAll()) {
                if (currentStoreId == null || !currentStoreId.equals(bookingStoreId)) {
                    return Result.error(403, "无权限：仅可操作本店预订");
                }
            }

            BookingDishDetail dish = new BookingDishDetail();
            dish.setBookingId(bookingId);
            dish.setDishId(body.get("dishId") != null ? (String) body.get("dishId") : (String) body.get("dish_id"));
            dish.setDishName(body.get("dishName") != null ? (String) body.get("dishName") : (String) body.get("dish_name"));
            // storeId 强制使用预订所属门店，避免店长越权写入其他门店
            dish.setStoreId(bookingStoreId != null ? bookingStoreId
                : (body.get("storeId") != null ? Long.valueOf(body.get("storeId").toString()) : 1L));

            Object qtyObj = body.get("dishQuantity");
            if (qtyObj == null) qtyObj = body.get("dish_quantity");
            if (qtyObj == null) qtyObj = body.get("qty");
            if (qtyObj != null) dish.setDishQuantity(Integer.valueOf(qtyObj.toString()));

            Object priceObj = body.get("unitPrice");
            if (priceObj == null) priceObj = body.get("unit_price");
            if (priceObj != null) dish.setUnitPrice(new java.math.BigDecimal(priceObj.toString()));

            Object subObj = body.get("subtotal");
            if (subObj == null) subObj = body.get("subtotal");
            if (subObj != null) dish.setSubtotal(new java.math.BigDecimal(subObj.toString()));

            dish.setKitchenStatus("pending");
            dish.setCreatedAt(LocalDateTime.now());

            BookingDishDetail saved = bookingDishDetailRepo.save(dish);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "添加菜品失败: " + e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/dishes/batch")
    @Transactional
    public Result<?> batchAddDishes(@PathVariable String bookingId,
                                    @RequestBody Object rawBody) {
        try {
            // 兼容 PowerShell 单元素数组被序列化为对象的情况
            List<Map<String, Object>> dishes;
            if (rawBody instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) rawBody;
                dishes = list;
            } else if (rawBody instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> single = (Map<String, Object>) rawBody;
                dishes = new ArrayList<>();
                dishes.add(single);
            } else {
                return Result.error(400, "请求体必须是数组或对象");
            }

            // 店长仅可操作本店预订，总经理可跨店
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            List<Map<String, Object>> bookingInfo = jdbc.queryForList(
                "SELECT store_id FROM booking_master WHERE booking_id=?", bookingId);
            if (bookingInfo.isEmpty()) {
                return Result.error(404, "预订不存在: " + bookingId);
            }
            Long storeId = Long.valueOf(bookingInfo.get(0).get("store_id").toString());
            if (!UserContext.isDataScopeAll()) {
                if (currentStoreId == null || !currentStoreId.equals(storeId)) {
                    return Result.error(403, "无权限：仅可操作本店预订");
                }
            }

            // 先删除该预订已有的菜品
            bookingDishDetailRepo.deleteByBookingId(bookingId);

            List<BookingDishDetail> saved = new ArrayList<>();
            for (Map<String, Object> body : dishes) {
                BookingDishDetail dish = new BookingDishDetail();
                dish.setBookingId(bookingId);
                String dishId = body.get("dishId") != null ? (String) body.get("dishId") : (String) body.get("dish_id");
                dish.setDishId(dishId);
                dish.setStoreId(body.get("storeId") != null ? Long.valueOf(body.get("storeId").toString()) : storeId);

                // 菜名：优先用前端传的，没有就从 dish_master 查
                String dishName = body.get("dishName") != null ? (String) body.get("dishName") : (String) body.get("dish_name");
                if (dishName == null || dishName.isEmpty()) {
                    try {
                        List<Map<String, Object>> dishInfo = jdbc.queryForList(
                            "SELECT dish_name FROM dish_master WHERE dish_id=? AND store_id=? LIMIT 1",
                            dishId, storeId);
                        if (!dishInfo.isEmpty()) {
                            dishName = dishInfo.get(0).get("dish_name").toString();
                        }
                    } catch (Exception ex) {
                        System.out.println("=== lookup dish_name error: " + ex.getMessage());
                    }
                }
                dish.setDishName(dishName);

                Object qtyObj = body.get("dishQuantity");
                if (qtyObj == null) qtyObj = body.get("dish_quantity");
                if (qtyObj == null) qtyObj = body.get("qty");
                if (qtyObj == null) qtyObj = body.get("quantity");
                int qty = qtyObj != null ? Integer.valueOf(qtyObj.toString()) : 1;
                dish.setDishQuantity(qty);

                // 价格：优先用前端传的，没有就从 dish_master 查
                java.math.BigDecimal unitPrice = null;
                Object priceObj = body.get("unitPrice");
                if (priceObj == null) priceObj = body.get("unit_price");
                if (priceObj == null) priceObj = body.get("price");
                if (priceObj == null) priceObj = body.get("salePrice");
                if (priceObj == null) priceObj = body.get("sale_price");
                if (priceObj != null) {
                    unitPrice = new java.math.BigDecimal(priceObj.toString());
                } else {
                    try {
                        List<Map<String, Object>> priceInfo = jdbc.queryForList(
                            "SELECT sale_price FROM dish_master WHERE dish_id=? AND store_id=? LIMIT 1",
                            dishId, storeId);
                        if (!priceInfo.isEmpty() && priceInfo.get(0).get("sale_price") != null) {
                            unitPrice = new java.math.BigDecimal(priceInfo.get(0).get("sale_price").toString());
                        }
                    } catch (Exception ex) {
                        System.out.println("=== lookup price error: " + ex.getMessage());
                    }
                }
                dish.setUnitPrice(unitPrice);

                // 小计：优先用前端传的，否则自动算
                java.math.BigDecimal subtotal = null;
                Object subObj = body.get("subtotal");
                if (subObj != null) {
                    subtotal = new java.math.BigDecimal(subObj.toString());
                } else if (unitPrice != null) {
                    subtotal = unitPrice.multiply(java.math.BigDecimal.valueOf(qty));
                }
                dish.setSubtotal(subtotal);

                dish.setKitchenStatus("pending");
                dish.setCreatedAt(LocalDateTime.now());
                saved.add(bookingDishDetailRepo.save(dish));
            }

            // 自动回写 total_amount 到 booking_master
            java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
            for (BookingDishDetail d : saved) {
                if (d.getSubtotal() != null) {
                    totalAmount = totalAmount.add(d.getSubtotal());
                }
            }
            try {
                jdbc.update("UPDATE booking_master SET total_amount=?, update_time=NOW() WHERE booking_id=?",
                    totalAmount, bookingId);
            } catch (Exception updateEx) {
                System.out.println("=== update total_amount error: " + updateEx.getMessage());
            }

            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "批量添加菜品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}/dishes/{dishBookingId}")
    @Transactional
    public ResponseEntity<?> deleteDish(@PathVariable String bookingId, @PathVariable Long dishBookingId) {
        // 店长仅可删除本店预订关联的菜品，总经理可跨店
        Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            List<Map<String, Object>> dishInfo = jdbc.queryForList(
                "SELECT store_id FROM booking_dish_detail WHERE dish_booking_id=?", dishBookingId);
            Long dishStoreId = dishInfo.isEmpty() ? null
                : Long.valueOf(dishInfo.get(0).get("store_id").toString());
            if (currentStoreId == null || !currentStoreId.equals(dishStoreId)) {
                return ResponseEntity.ok(Result.error(403, "无权限：仅可操作本店预订"));
            }
        }
        bookingDishDetailRepo.deleteById(dishBookingId);
        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }
}
