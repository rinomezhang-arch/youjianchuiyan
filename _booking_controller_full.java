package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private JdbcTemplate jdbc;

    // ===== Booking Master =====

    @GetMapping
    public Result<List<BookingMaster>> list(@RequestParam(defaultValue = "1") Long storeId) {
        return Result.success(bookingMasterRepo.findByStoreIdOrderByBookingDateDesc(storeId));
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
        LocalDate d = LocalDate.parse(date);
        return Result.success(bookingMasterRepo.findByStoreIdAndBookingDate(storeId, d));
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

            // storeId - 支持 snake_case 和 camelCase
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            if (storeIdObj == null) storeIdObj = 1;
            booking.setStoreId(Long.valueOf(storeIdObj.toString()));

            // bookingDate - 支持 snake_case 和 camelCase
            Object bookingDateObj = body.get("bookingDate");
            if (bookingDateObj == null) bookingDateObj = body.get("booking_date");
            if (bookingDateObj != null) {
                booking.setBookingDate(LocalDate.parse(bookingDateObj.toString()));
            } else {
                booking.setBookingDate(LocalDate.now());
            }

            // bookingTime - 支持 snake_case 和 camelCase
            Object bookingTimeObj = body.get("bookingTime");
            if (bookingTimeObj == null) bookingTimeObj = body.get("booking_time");
            if (bookingTimeObj != null) {
                String timeStr = bookingTimeObj.toString();
                if (timeStr.length() == 5) timeStr += ":00";
                booking.setBookingTime(java.time.LocalTime.parse(timeStr));
            } else {
                // 默认18:00
                booking.setBookingTime(java.time.LocalTime.of(18, 0));
            }

            // 其他字段 - 全部支持 snake_case 和 camelCase
            Object customerIdObj = body.get("customerId");
            if (customerIdObj == null) customerIdObj = body.get("customer_id");
            if (customerIdObj != null)
                booking.setCustomerId(Integer.valueOf(customerIdObj.toString()));

            booking.setCustomerName(body.get("customerName") != null ? (String) body.get("customerName") : (String) body.get("customer_name"));
            booking.setCustomerPhone(body.get("customerPhone") != null ? (String) body.get("customerPhone") : (String) body.get("customer_phone"));

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

            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            System.out.println("=== Creating booking: " + booking.getBookingId() + " time=" + booking.getBookingTime() + " date=" + booking.getBookingDate());
            BookingMaster saved = bookingMasterRepo.save(booking);
            System.out.println("=== BookingMaster saved: " + saved.getBookingId());

            // 预定桌台 - 支持 tables 数组和 table_ids 数组两种格式
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tables = (List<Map<String, Object>>) body.get("tables");

            // 如果没有tables，但有table_ids数组，则转换格式
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
                System.out.println("=== Processing " + tables.size() + " tables");
                for (Map<String, Object> t : tables) {
                    BookingTable bt = new BookingTable();
                    bt.setStoreId(booking.getStoreId());
                    bt.setBookingId(bookingId);
                    bt.setBookingDate(booking.getBookingDate());
                    bt.setBookingTime(booking.getBookingTime());

                    Object tableIdObj = t.get("tableId");
                    if (tableIdObj == null) tableIdObj = t.get("table_id");
                    if (tableIdObj != null)
                        bt.setTableId(Integer.valueOf(tableIdObj.toString()));

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

                    // 更新桌台状态为已预订
                    Object updateTableIdObj = t.get("tableId");
                    if (updateTableIdObj == null) updateTableIdObj = t.get("table_id");
                    if (updateTableIdObj != null) {
                        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                                updateTableIdObj, booking.getStoreId());
                    }
                }
            }

            System.out.println("=== Booking created successfully: " + bookingId);
            return ResponseEntity.ok(Result.success(saved));
        } catch (Exception e) {
            System.out.println("=== Booking creation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Result.error(500, "创建预订失败: " + e.getMessage()));
        }
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

        b.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(Result.success(bookingMasterRepo.save(b)));
    }

    @DeleteMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable String bookingId,
                                     @RequestParam(defaultValue = "1") Long storeId) {
        List<Map<String, Object>> tables = jdbc.queryForList(
            "SELECT table_id FROM booking_table WHERE booking_id=? AND store_id=?", bookingId, storeId);

        bookingDishDetailRepo.deleteByBookingId(bookingId);
        bookingTableRepo.deleteByBookingId(bookingId);
        bookingMasterRepo.deleteById(new BookingMasterId(bookingId, storeId));

        for (Map<String, Object> t : tables) {
            Long tableId = Long.valueOf(t.get("table_id").toString());
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
            if (count == null || count == 0) {
                jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
            }
        }

        return ResponseEntity.ok(Result.success(Map.of("deleted", true, "bookingId", bookingId)));
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
}
