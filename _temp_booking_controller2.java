package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import com.youjian.banquet.common.Result;
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
        BookingMaster booking = new BookingMaster();
        String bookingId = "BK" + System.currentTimeMillis();
        booking.setBookingId(bookingId);
        booking.setStoreId(Long.valueOf(body.getOrDefault("storeId", 1).toString()));
        if (body.get("bookingDate") != null) {
            booking.setBookingDate(LocalDate.parse(body.get("bookingDate").toString()));
        } else {
            booking.setBookingDate(LocalDate.now());
        }
        // booking_time format: HH:mm
        if (body.get("bookingTime") != null) {
            String timeStr = body.get("bookingTime").toString();
            if (timeStr.length() == 5) timeStr += ":00";
            booking.setBookingTime(java.time.LocalTime.parse(timeStr));
        }
        if (body.get("customerId") != null)
            booking.setCustomerId(Integer.valueOf(body.get("customerId").toString()));
        booking.setCustomerName((String) body.get("customerName"));
        booking.setCustomerPhone((String) body.get("customerPhone"));
        if (body.get("staffId") != null)
            booking.setStaffId(Integer.valueOf(body.get("staffId").toString()));
        booking.setStaffName((String) body.get("staffName"));
        if (body.get("guestCount") != null)
            booking.setGuestCount(Integer.valueOf(body.get("guestCount").toString()));
        if (body.get("tableCount") != null)
            booking.setTableCount(Integer.valueOf(body.get("tableCount").toString()));
        if (body.get("spareTables") != null)
            booking.setSpareTables(Integer.valueOf(body.get("spareTables").toString()));
        if (body.get("guestPerTable") != null)
            booking.setGuestPerTable(Integer.valueOf(body.get("guestPerTable").toString()));
        booking.setBookingStatus((String) body.getOrDefault("bookingStatus", "confirmed"));
        booking.setBanquetName((String) body.get("banquetName"));
        booking.setOccasionType((String) body.get("occasionType"));
        booking.setSpecialRequest((String) body.get("specialRequest"));
        booking.setRemark((String) body.get("remark"));
        booking.setPaymentStatus((String) body.getOrDefault("paymentStatus", "unpaid"));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        BookingMaster saved = bookingMasterRepo.save(booking);

        // 预定桌台 - 支持前端发送的table_ids格式
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) body.get("tables");
        
        // 如果没有tables，但有table_ids数组，则转换格式
        if (tables == null || tables.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Integer> tableIds = (List<Integer>) body.get("tableIds");
            @SuppressWarnings("unchecked")
            List<String> tableNames = (List<String>) body.get("tableNames");
            if (tableIds != null && !tableIds.isEmpty()) {
                tables = new ArrayList<>();
                for (int i = 0; i < tableIds.size(); i++) {
                    Map<String, Object> t = new HashMap<>();
                    t.put("tableId", tableIds.get(i));
                    if (tableNames != null && i < tableNames.size()) {
                        t.put("tableName", tableNames.get(i));
                        t.put("tableNumber", tableNames.get(i));
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
                if (t.get("tableId") != null)
                    bt.setTableId(Integer.valueOf(t.get("tableId").toString()));
                bt.setTableNumber((String) t.get("tableNumber"));
                bt.setTableName((String) t.get("tableName"));
                if (t.get("guestCount") != null)
                    bt.setGuestCount(Integer.valueOf(t.get("guestCount").toString()));
                bt.setPackageId((String) t.get("packageId"));
                bt.setPackageName((String) t.get("packageName"));
                bt.setOpenTableType((String) t.get("openTableType"));
                bt.setTableNote((String) t.get("tableNote"));
                bt.setCreatedAt(LocalDateTime.now());
                bookingTableRepo.save(bt);

                // BUG 2: 创建预订时同步 table_status = occupied
                if (t.get("tableId") != null) {
                    jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                            t.get("tableId"), booking.getStoreId());
                }
            }
        }
        return ResponseEntity.ok(Result.success(saved));
    }

    @PutMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<Result<BookingMaster>> update(@PathVariable String bookingId,
                                                 @RequestBody Map<String, Object> body) {
        Long storeId = Long.valueOf(body.getOrDefault("storeId", 1).toString());
        BookingMasterId id = new BookingMasterId(bookingId, storeId);
        Optional<BookingMaster> opt = bookingMasterRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BookingMaster b = opt.get();
        if (body.get("bookingDate") != null) b.setBookingDate(LocalDate.parse(body.get("bookingDate").toString()));
        if (body.get("bookingTime") != null) {
            String t = body.get("bookingTime").toString();
            if (t.length() == 5) t += ":00";
            b.setBookingTime(java.time.LocalTime.parse(t));
        }
        if (body.get("customerName") != null) b.setCustomerName((String) body.get("customerName"));
        if (body.get("customerPhone") != null) b.setCustomerPhone((String) body.get("customerPhone"));
        if (body.get("guestCount") != null) b.setGuestCount(Integer.valueOf(body.get("guestCount").toString()));
        if (body.get("tableCount") != null) b.setTableCount(Integer.valueOf(body.get("tableCount").toString()));
        if (body.get("bookingStatus") != null) b.setBookingStatus((String) body.get("bookingStatus"));
        if (body.get("banquetName") != null) b.setBanquetName((String) body.get("banquetName"));
        if (body.get("occasionType") != null) b.setOccasionType((String) body.get("occasionType"));
        if (body.get("specialRequest") != null) b.setSpecialRequest((String) body.get("specialRequest"));
        if (body.get("remark") != null) b.setRemark((String) body.get("remark"));
        if (body.get("paymentStatus") != null) b.setPaymentStatus((String) body.get("paymentStatus"));
        b.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(Result.success(bookingMasterRepo.save(b)));
    }

    @DeleteMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable String bookingId,
                                     @RequestParam(defaultValue = "1") Long storeId) {
        // BUG 2: 删除预订前，获取关联的桌台
        List<Map<String, Object>> tables = jdbc.queryForList(
            "SELECT table_id FROM booking_table WHERE booking_id=? AND store_id=?", bookingId, storeId);

        bookingDishDetailRepo.deleteByBookingId(bookingId);
        bookingTableRepo.deleteByBookingId(bookingId);
        bookingMasterRepo.deleteById(new BookingMasterId(bookingId, storeId));

        // BUG 2: 删除预订后，检查桌台是否还有其他预订，没有则恢复 available
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

        // BUG 2: 添加桌台时同步 table_status = occupied
        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                table.getTableId(), table.getStoreId());

        return Result.success(saved);
    }

    @DeleteMapping("/{bookingId}/tables/{tableBookingId}")
    @Transactional
    public ResponseEntity<?> deleteTable(@PathVariable String bookingId, @PathVariable Long tableBookingId) {
        // BUG 2: 删除桌台前获取桌台信息
        Map<String, Object> tableInfo = jdbc.queryForMap(
            "SELECT table_id, store_id FROM booking_table WHERE table_booking_id=?", tableBookingId);
        Long tableId = Long.valueOf(tableInfo.get("table_id").toString());
        Long storeId = Long.valueOf(tableInfo.get("store_id").toString());

        bookingTableRepo.deleteById(tableBookingId);

        // BUG 2: 删除桌台后检查是否还有其他预订
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
        if (count == null || count == 0) {
            jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
        }

        return ResponseEntity.ok(Result.success(Map.of("deleted", true)));
    }
}