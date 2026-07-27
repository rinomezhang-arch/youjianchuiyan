package com.youjian.banquet.controller;

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

    // 辅助方法：获取值，同时支持snake_case和camelCase
    private Object getValue(Map<String, Object> body, String camelCase) {
        if (body.containsKey(camelCase)) return body.get(camelCase);
        // 转换为snake_case
        String snakeCase = camelCase.replaceAll("([A-Z])", "_$1").toLowerCase();
        return body.get(snakeCase);
    }

    @GetMapping
    public List<BookingMaster> list(@RequestParam(defaultValue = "1") Long storeId) {
        return bookingMasterRepo.findByStoreIdOrderByBookingDateDesc(storeId);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String bookingId,
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
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public List<BookingMaster> search(@RequestParam(defaultValue = "1") Long storeId,
                                       @RequestParam(required = false) String keyword) {
        return bookingMasterRepo.search(storeId, keyword);
    }

    @GetMapping("/date/{date}")
    public List<BookingMaster> byDate(@PathVariable String date,
                                       @RequestParam(defaultValue = "1") Long storeId) {
        LocalDate d = LocalDate.parse(date);
        return bookingMasterRepo.findByStoreIdAndBookingDate(storeId, d);
    }

    @GetMapping("/range")
    public List<BookingMaster> byDateRange(@RequestParam String start,
                                            @RequestParam String end,
                                            @RequestParam(defaultValue = "1") Long storeId) {
        return bookingMasterRepo.findByDateRange(storeId,
                LocalDate.parse(start), LocalDate.parse(end));
    }

    @GetMapping("/status/{status}")
    public List<BookingMaster> byStatus(@PathVariable String status,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        return bookingMasterRepo.findByStoreIdAndBookingStatus(storeId, status);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<BookingMaster> create(@RequestBody Map<String, Object> body) {
        BookingMaster booking = new BookingMaster();
        String bookingId = "BK" + System.currentTimeMillis();
        booking.setBookingId(bookingId);
        booking.setStoreId(Long.valueOf(getOrDefault(body, "storeId", "1")));
        
        // booking_date
        Object bd = getValue(body, "bookingDate");
        if (bd != null) {
            booking.setBookingDate(LocalDate.parse(bd.toString()));
        } else {
            booking.setBookingDate(LocalDate.now());
        }
        
        // booking_time format: HH:mm
        Object bt = getValue(body, "bookingTime");
        if (bt != null) {
            String timeStr = bt.toString();
            if (timeStr.length() == 5) timeStr += ":00";
            booking.setBookingTime(java.time.LocalTime.parse(timeStr));
        }
        if (getValue(body, "customerId") != null)
            booking.setCustomerId(Integer.valueOf(getValue(body, "customerId").toString()));
        booking.setCustomerName((String) getValue(body, "customerName"));
        booking.setCustomerPhone((String) getValue(body, "customerPhone"));
        if (getValue(body, "staffId") != null)
            booking.setStaffId(Integer.valueOf(getValue(body, "staffId").toString()));
        booking.setStaffName((String) getValue(body, "staffName"));
        if (getValue(body, "guestCount") != null)
            booking.setGuestCount(Integer.valueOf(getValue(body, "guestCount").toString()));
        if (getValue(body, "tableCount") != null)
            booking.setTableCount(Integer.valueOf(getValue(body, "tableCount").toString()));
        if (getValue(body, "spareTables") != null)
            booking.setSpareTables(Integer.valueOf(getValue(body, "spareTables").toString()));
        if (getValue(body, "guestPerTable") != null)
            booking.setGuestPerTable(Integer.valueOf(getValue(body, "guestPerTable").toString()));
        booking.setBookingStatus((String) getOrDefault(body, "bookingStatus", "confirmed"));
        booking.setBanquetName((String) getValue(body, "banquetName"));
        booking.setOccasionType((String) getValue(body, "occasionType"));
        booking.setSpecialRequest((String) getValue(body, "specialRequest"));
        booking.setRemark((String) getValue(body, "remark"));
        booking.setPaymentStatus((String) getOrDefault(body, "paymentStatus", "unpaid"));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        BookingMaster saved = bookingMasterRepo.save(booking);

        // 预定桌台 - 支持两种格式：tables数组 或 table_ids/table_names数组
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) body.get("tables");
        List<Integer> tableIds = (List<Integer>) getValue(body, "tableIds");
        List<String> tableNames = (List<String>) getValue(body, "tableNames");
        
        if (tableIds != null && tableIds.size() > 0) {
            // 前端发送的格式：table_ids和table_names数组
            for (int i = 0; i < tableIds.size(); i++) {
                BookingTable btObj = new BookingTable();
                btObj.setStoreId(booking.getStoreId());
                btObj.setBookingId(bookingId);
                btObj.setBookingDate(booking.getBookingDate());
                btObj.setBookingTime(booking.getBookingTime());
                btObj.setTableId(tableIds.get(i));
                String tn = (tableNames != null && i < tableNames.size()) ? tableNames.get(i) : "";
                btObj.setTableNumber(tn);
                btObj.setTableName(tn);
                btObj.setCreatedAt(LocalDateTime.now());
                bookingTableRepo.save(btObj);

                // 创建预订时同步 table_status = occupied
                jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                        tableIds.get(i), booking.getStoreId());
            }
        } else if (tables != null) {
            // 旧格式：tables数组
            for (Map<String, Object> t : tables) {
                BookingTable btObj = new BookingTable();
                btObj.setStoreId(booking.getStoreId());
                btObj.setBookingId(bookingId);
                btObj.setBookingDate(booking.getBookingDate());
                btObj.setBookingTime(booking.getBookingTime());
                if (t.get("tableId") != null)
                    btObj.setTableId(Integer.valueOf(t.get("tableId").toString()));
                btObj.setTableNumber((String) t.get("tableNumber"));
                btObj.setTableName((String) t.get("tableName"));
                if (t.get("guestCount") != null)
                    btObj.setGuestCount(Integer.valueOf(t.get("guestCount").toString()));
                btObj.setPackageId((String) t.get("packageId"));
                btObj.setPackageName((String) t.get("packageName"));
                btObj.setOpenTableType((String) t.get("openTableType"));
                btObj.setTableNote((String) t.get("tableNote"));
                btObj.setCreatedAt(LocalDateTime.now());
                bookingTableRepo.save(btObj);

                // 创建预订时同步 table_status = occupied
                if (t.get("tableId") != null) {
                    jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                            t.get("tableId"), booking.getStoreId());
                }
            }
        }
        return ResponseEntity.ok(saved);
    }

    private String getOrDefault(Map<String, Object> body, String key, String defaultValue) {
        Object val = getValue(body, key);
        return val != null ? val.toString() : defaultValue;
    }

    @PutMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<BookingMaster> update(@PathVariable String bookingId,
                                                 @RequestBody Map<String, Object> body) {
        Long storeId = Long.valueOf(getOrDefault(body, "storeId", "1"));
        BookingMasterId id = new BookingMasterId(bookingId, storeId);
        Optional<BookingMaster> opt = bookingMasterRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BookingMaster b = opt.get();
        if (getValue(body, "bookingDate") != null) b.setBookingDate(LocalDate.parse(getValue(body, "bookingDate").toString()));
        if (getValue(body, "bookingTime") != null) {
            String t = getValue(body, "bookingTime").toString();
            if (t.length() == 5) t += ":00";
            b.setBookingTime(java.time.LocalTime.parse(t));
        }
        if (getValue(body, "customerName") != null) b.setCustomerName((String) getValue(body, "customerName"));
        if (getValue(body, "customerPhone") != null) b.setCustomerPhone((String) getValue(body, "customerPhone"));
        if (getValue(body, "guestCount") != null) b.setGuestCount(Integer.valueOf(getValue(body, "guestCount").toString()));
        if (getValue(body, "tableCount") != null) b.setTableCount(Integer.valueOf(getValue(body, "tableCount").toString()));
        if (getValue(body, "bookingStatus") != null) b.setBookingStatus((String) getValue(body, "bookingStatus"));
        if (getValue(body, "banquetName") != null) b.setBanquetName((String) getValue(body, "banquetName"));
        if (getValue(body, "occasionType") != null) b.setOccasionType((String) getValue(body, "occasionType"));
        if (getValue(body, "specialRequest") != null) b.setSpecialRequest((String) getValue(body, "specialRequest"));
        if (getValue(body, "remark") != null) b.setRemark((String) getValue(body, "remark"));
        if (getValue(body, "paymentStatus") != null) b.setPaymentStatus((String) getValue(body, "paymentStatus"));
        b.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(bookingMasterRepo.save(b));
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

        return ResponseEntity.ok(Map.of("deleted", true, "bookingId", bookingId));
    }

    @GetMapping("/{bookingId}/tables")
    public List<BookingTable> getTables(@PathVariable String bookingId,
                                         @RequestParam(defaultValue = "1") Long storeId) {
        return bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId);
    }

    @PostMapping("/{bookingId}/tables")
    @Transactional
    public BookingTable addTable(@PathVariable String bookingId,
                                  @RequestBody BookingTable table) {
        table.setBookingId(bookingId);
        table.setCreatedAt(LocalDateTime.now());
        BookingTable saved = bookingTableRepo.save(table);

        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                table.getTableId(), table.getStoreId());

        return saved;
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

        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
