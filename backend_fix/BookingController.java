package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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

    // ===== Booking Master =====

    @GetMapping
    public Result<List<BookingMaster>> list(@RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingMasterRepo.findByStoreIdOrderByBookingDateDesc(storeId));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listWithFilters(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String occasionType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
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
                if (endDate != null && !endDate.isEmpty()) {
                    sql.append(" AND bm.booking_date >= ? AND bm.booking_date <= ?");
                    params.add(java.sql.Date.valueOf(date));
                    params.add(java.sql.Date.valueOf(endDate));
                } else {
                    sql.append(" AND bm.booking_date=?");
                    params.add(java.sql.Date.valueOf(date));
                }
            }
            if (time != null && !time.isEmpty()) {
                if (time.contains("午餐")) {
                    sql.append(" AND bm.booking_time < '15:00:00'");
                } else if (time.contains("晚餐")) {
                    sql.append(" AND bm.booking_time >= '15:00:00'");
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (bm.customer_name LIKE ? OR bm.customer_phone LIKE ? OR bm.booking_id LIKE ?)");
                String kw = "%" + keyword + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND bm.booking_status=?");
                params.add(status);
            }
            if (occasionType != null && !occasionType.isEmpty()) {
                sql.append(" AND bm.occasion_type=?");
                params.add(occasionType);
            }

            // Count total
            String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") as sub";
            Integer total = jdbc.queryForObject(countSql, Integer.class, params.toArray());
            if (total == null) total = 0;

            // Add pagination
            sql.append(" ORDER BY bm.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

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

    @GetMapping("/{bookingId}")
    public ResponseEntity<Result<Map<String, Object>>> detail(@PathVariable String bookingId,
                                                       @RequestParam(defaultValue = "1") Long storeId) {
        BookingMasterId id = new BookingMasterId(bookingId, storeId);
        Optional<BookingMaster> master = bookingMasterRepo.findById(id);
        if (master.isEmpty()) return ResponseEntity.notFound().build();

        List<BookingTable> tables = bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId);
        List<BookingDishDetail> dishes = bookingDishDetailRepo.findByBookingId(bookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("booking", master.get());
        result.put("tables", tables);
        result.put("dishes", dishes);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/search")
    public Result<List<BookingMaster>> search(@RequestParam(defaultValue = "1") Long storeId,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(bookingMasterRepo.search(storeId, keyword));
    }

    @GetMapping("/date/{date}")
    public Result<List<BookingMaster>> byDate(@PathVariable String date,
                                       @RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingDate(storeId, LocalDate.parse(date)));
    }

    @GetMapping("/range")
    public Result<List<BookingMaster>> byDateRange(@RequestParam String start,
                                            @RequestParam String end,
                                            @RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingMasterRepo.findByDateRange(storeId,
                LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/status/{status}")
    public Result<List<BookingMaster>> byStatus(@PathVariable String status,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingStatus(storeId, status));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Result<BookingMaster>> create(@RequestBody Map<String, Object> body) {
        try {
            BookingMaster booking = new BookingMaster();
            String bookingId = "BK" + System.currentTimeMillis();
            booking.setBookingId(bookingId);

            // storeId
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            if (storeIdObj == null) storeIdObj = 1;
            booking.setStoreId(Long.valueOf(storeIdObj.toString()));

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
                    booking.setDeposit(new java.math.BigDecimal(depositObj.toString()));
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
                jdbc.update("UPDATE customer_master SET booking_count=booking_count+1, last_booking_date=?, customer_name=COALESCE(?, customer_name), updated_at=NOW() WHERE customer_id=?",
                    LocalDate.now(), name != null ? name : null, customerId);
                return customerId;
            } else {
                // 新客户：自动创建
                jdbc.update("INSERT INTO customer_master (store_id, customer_name, customer_phone, total_amount, member_level, booking_count, last_booking_date, is_active, created_at, updated_at) VALUES (?, ?, ?, 0, 'v1', 1, ?, 1, NOW(), NOW())",
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
        Long storeId = Long.valueOf(body.getOrDefault("storeId", body.getOrDefault("store_id", 1)).toString());
        BookingMasterId id = new BookingMasterId(bookingId, storeId);
        Optional<BookingMaster> opt = bookingMasterRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BookingMaster b = opt.get();
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
                b.setDeposit(new java.math.BigDecimal(depositObj.toString()));
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
        bookingMasterRepo.deleteById(new BookingMasterId(bookingId, storeId));

        // 恢复桌台状态：检查该桌台在该日期是否还有其他预订
        for (Map<String, Object> t : tables) {
            Long tableId = Long.valueOf(t.get("table_id").toString());
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?",
                Integer.class, tableId, storeId);
            if (count == null || count == 0) {
                // 没有任何预订关联了，恢复为可用
                jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
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

            // 获取源预订
            BookingMasterId sourceId = new BookingMasterId(sourceBookingId, storeId);
            Optional<BookingMaster> sourceOpt = bookingMasterRepo.findById(sourceId);
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

            // 查找两个桌台在该日期的预订
            List<Map<String, Object>> fromBookings = jdbc.queryForList(
                "SELECT booking_id FROM booking_table WHERE table_id=? AND store_id=? AND booking_date=?",
                fromTableId, storeId, date);
            List<Map<String, Object>> toBookings = jdbc.queryForList(
                "SELECT booking_id FROM booking_table WHERE table_id=? AND store_id=? AND booking_date=?",
                toTableId, storeId, date);

            // 交换桌台ID
            for (Map<String, Object> fb : fromBookings) {
                String bookingId = fb.get("booking_id").toString();
                jdbc.update("UPDATE booking_table SET table_id=? WHERE booking_id=? AND table_id=? AND store_id=?",
                    toTableId, bookingId, fromTableId, storeId);
                // 更新桌台名称
                Map<String, Object> tableInfo = jdbc.queryForMap("SELECT table_number, table_name FROM table_master WHERE table_id=? AND store_id=?", toTableId, storeId);
                jdbc.update("UPDATE booking_table SET table_number=?, table_name=? WHERE booking_id=? AND table_id=? AND store_id=?",
                    tableInfo.get("table_number"), tableInfo.get("table_name"), bookingId, toTableId, storeId);
            }
            for (Map<String, Object> tb : toBookings) {
                String bookingId = tb.get("booking_id").toString();
                jdbc.update("UPDATE booking_table SET table_id=? WHERE booking_id=? AND table_id=? AND store_id=?",
                    fromTableId, bookingId, toTableId, storeId);
                Map<String, Object> tableInfo = jdbc.queryForMap("SELECT table_number, table_name FROM table_master WHERE table_id=? AND store_id=?", fromTableId, storeId);
                jdbc.update("UPDATE booking_table SET table_number=?, table_name=? WHERE booking_id=? AND table_id=? AND store_id=?",
                    tableInfo.get("table_number"), tableInfo.get("table_name"), bookingId, fromTableId, storeId);
            }

            return ResponseEntity.ok(Result.success(Map.of("swapped", true)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "互换预订失败: " + e.getMessage()));
        }
    }

    // ===== Booking Tables =====

    @GetMapping("/{bookingId}/tables")
    public Result<List<BookingTable>> getTables(@PathVariable String bookingId,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId));
    }

    @PostMapping("/{bookingId}/tables")
    @Transactional
    public Result<BookingTable> addTable(@PathVariable String bookingId,
                                  @RequestBody BookingTable table) {
        table.setBookingId(bookingId);
        table.setCreatedAt(LocalDateTime.now());
        BookingTable saved = bookingTableRepo.save(table);
        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                table.getTableId(), table.getStoreId());
        return Result.success(saved);
    }

    @DeleteMapping("/{bookingId}/tables/{tableBookingId}")
    @Transactional
    public ResponseEntity<?> deleteTable(@PathVariable String bookingId, @PathVariable Long tableBookingId) {
        Map<String, Object> tableInfo = jdbc.queryForMap(
            "SELECT table_id, store_id FROM booking_table WHERE table_booking_id=?", tableBookingId);
        Long tableId = Long.valueOf(tableInfo.get("table_id").toString());
        Long storeId = Long.valueOf(tableInfo.get("store_id").toString());

        bookingTableRepo.deleteById(tableBookingId);

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
        if (count == null || count == 0) {
            jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
        }
        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }

    // ===== Booking Dishes =====

    @GetMapping("/{bookingId}/dishes")
    public Result<List<BookingDishDetail>> getDishes(@PathVariable String bookingId,
                                              @RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingDishDetailRepo.findByBookingIdAndStoreId(bookingId, storeId));
    }

    @PostMapping("/{bookingId}/dishes")
    @Transactional
    public Result<BookingDishDetail> addDish(@PathVariable String bookingId,
                                      @RequestBody Map<String, Object> body) {
        try {
            BookingDishDetail dish = new BookingDishDetail();
            dish.setBookingId(bookingId);
            dish.setDishId(body.get("dishId") != null ? (String) body.get("dishId") : (String) body.get("dish_id"));
            dish.setDishName(body.get("dishName") != null ? (String) body.get("dishName") : (String) body.get("dish_name"));
            dish.setStoreId(body.get("storeId") != null ? Long.valueOf(body.get("storeId").toString()) : 1L);

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
                                    @RequestBody List<Map<String, Object>> dishes) {
        try {
            // 先删除该预订已有的菜品
            bookingDishDetailRepo.deleteByBookingId(bookingId);

            // 获取预订的 storeId
            List<Map<String, Object>> bookingInfo = jdbc.queryForList(
                "SELECT store_id FROM booking_master WHERE booking_id=?", bookingId);
            Long storeId = 1L;
            if (!bookingInfo.isEmpty()) {
                storeId = Long.valueOf(bookingInfo.get(0).get("store_id").toString());
            }

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
                jdbc.update("UPDATE booking_master SET total_amount=?, updated_at=NOW() WHERE booking_id=?",
                    totalAmount, bookingId);
            } catch (Exception updateEx) {
                System.out.println("=== update total_amount error: " + updateEx.getMessage());
            }

            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "批量添加菜品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}/dishes/{dishBookingId}")
    @Transactional
    public ResponseEntity<?> deleteDish(@PathVariable String bookingId, @PathVariable Integer dishBookingId) {
        bookingDishDetailRepo.deleteById(dishBookingId);
        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }
}
