package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping({"/api/bookings", "/menu-api/bookings"})
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingMasterRepository bookingMasterRepo;

    @Autowired
    private BookingTableRepository bookingTableRepo;

    @Autowired
    private BookingDishDetailRepository bookingDishDetailRepo;

    @Autowired
    private JdbcTemplate jdbc;

    private Object getValue(Map<String, Object> body, String camelCase) {
        if (body.containsKey(camelCase)) return body.get(camelCase);
        String snakeCase = camelCase.replaceAll("([A-Z])", "_$1").toLowerCase();
        return body.get(snakeCase);
    }

    @GetMapping
    public List<BookingMaster> list(@RequestParam(defaultValue = "1") Long storeId) {
        logger.info("GET /api/bookings storeId={}", storeId);
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
        logger.info("=== CREATE BOOKING START ===");
        logger.info("Received body keys: {}", body.keySet());
        
        try {
            BookingMaster booking = new BookingMaster();
            String bookingId = "BK" + System.currentTimeMillis();
            booking.setBookingId(bookingId);
            
            // store_id
            Object storeIdObj = getValue(body, "storeId");
            logger.info("storeId value: {} (type: {})", storeIdObj, storeIdObj != null ? storeIdObj.getClass().getName() : "null");
            if (storeIdObj != null) {
                booking.setStoreId(Long.valueOf(storeIdObj.toString()));
            } else {
                booking.setStoreId(1L);
            }
            
            // booking_date
            Object bd = getValue(body, "bookingDate");
            logger.info("bookingDate value: {} (type: {})", bd, bd != null ? bd.getClass().getName() : "null");
            if (bd != null) {
                booking.setBookingDate(LocalDate.parse(bd.toString()));
            } else {
                booking.setBookingDate(LocalDate.now());
            }
            
            // booking_time
            Object bt = getValue(body, "bookingTime");
            logger.info("bookingTime value: {} (type: {})", bt, bt != null ? bt.getClass().getName() : "null");
            if (bt != null) {
                String timeStr = bt.toString();
                if (timeStr.length() == 5) timeStr += ":00";
                booking.setBookingTime(java.time.LocalTime.parse(timeStr));
            }
            
            // customer_name
            Object cn = getValue(body, "customerName");
            logger.info("customerName value: {} (type: {})", cn, cn != null ? cn.getClass().getName() : "null");
            booking.setCustomerName(cn != null ? cn.toString() : null);
            
            // customer_phone
            Object cp = getValue(body, "customerPhone");
            logger.info("customerPhone value: {} (type: {})", cp, cp != null ? cp.getClass().getName() : "null");
            booking.setCustomerPhone(cp != null ? cp.toString() : null);
            
            // guest_count
            Object gc = getValue(body, "guestCount");
            logger.info("guestCount value: {} (type: {})", gc, gc != null ? gc.getClass().getName() : "null");
            if (gc != null) {
                booking.setGuestCount(Integer.valueOf(gc.toString()));
            }
            
            // table_count
            Object tc = getValue(body, "tableCount");
            logger.info("tableCount value: {} (type: {})", tc, tc != null ? tc.getClass().getName() : "null");
            if (tc != null) {
                booking.setTableCount(Integer.valueOf(tc.toString()));
            }
            
            // spare_tables
            Object st = getValue(body, "spareTables");
            logger.info("spareTables value: {} (type: {})", st, st != null ? st.getClass().getName() : "null");
            if (st != null) {
                booking.setSpareTables(Integer.valueOf(st.toString()));
            }
            
            // occasion_type
            Object ot = getValue(body, "occasionType");
            logger.info("occasionType value: {} (type: {})", ot, ot != null ? ot.getClass().getName() : "null");
            booking.setOccasionType(ot != null ? ot.toString() : null);
            
            // deposit
            Object dep = getValue(body, "deposit");
            logger.info("deposit value: {} (type: {})", dep, dep != null ? dep.getClass().getName() : "null");
            if (dep != null) {
                booking.setDeposit(new BigDecimal(dep.toString()));
            }
            
            // remark
            Object r = getValue(body, "remark");
            logger.info("remark value: {} (type: {})", r, r != null ? r.getClass().getName() : "null");
            booking.setRemark(r != null ? r.toString() : null);
            
            // booking_status
            Object bs = getValue(body, "bookingStatus");
            logger.info("bookingStatus value: {} (type: {})", bs, bs != null ? bs.getClass().getName() : "null");
            booking.setBookingStatus(bs != null ? bs.toString() : "confirmed");
            
            // payment_status
            Object ps = getValue(body, "paymentStatus");
            logger.info("paymentStatus value: {} (type: {})", ps, ps != null ? ps.getClass().getName() : "null");
            booking.setPaymentStatus(ps != null ? ps.toString() : "unpaid");
            
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            BookingMaster saved = bookingMasterRepo.save(booking);
            logger.info("BookingMaster saved successfully, bookingId={}", bookingId);

            // 处理桌台
            Object tablesObj = body.get("tables");
            Object tableIdsObj = getValue(body, "tableIds");
            Object tableNamesObj = getValue(body, "tableNames");
            
            logger.info("tables value: {} (type: {})", tablesObj, tablesObj != null ? tablesObj.getClass().getName() : "null");
            logger.info("tableIds value: {} (type: {})", tableIdsObj, tableIdsObj != null ? tableIdsObj.getClass().getName() : "null");
            logger.info("tableNames value: {} (type: {})", tableNamesObj, tableNamesObj != null ? tableNamesObj.getClass().getName() : "null");
            
            @SuppressWarnings("unchecked")
            List<Integer> tableIds = (List<Integer>) tableIdsObj;
            @SuppressWarnings("unchecked")
            List<String> tableNames = (List<String>) tableNamesObj;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tables = (List<Map<String, Object>>) tablesObj;
            
            if (tableIds != null && tableIds.size() > 0) {
                logger.info("Processing tableIds, count={}", tableIds.size());
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

                    jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                            tableIds.get(i), booking.getStoreId());
                }
            } else if (tables != null) {
                logger.info("Processing tables array, count={}", tables.size());
                for (Map<String, Object> t : tables) {
                    BookingTable btObj = new BookingTable();
                    btObj.setStoreId(booking.getStoreId());
                    btObj.setBookingId(bookingId);
                    btObj.setBookingDate(booking.getBookingDate());
                    btObj.setBookingTime(booking.getBookingTime());
                    Object tid = t.get("tableId");
                    if (tid != null)
                        btObj.setTableId(Integer.valueOf(tid.toString()));
                    btObj.setTableNumber((String) t.get("tableNumber"));
                    btObj.setTableName((String) t.get("tableName"));
                    Object gct = t.get("guestCount");
                    if (gct != null)
                        btObj.setGuestCount(Integer.valueOf(gct.toString()));
                    btObj.setPackageId((String) t.get("packageId"));
                    btObj.setPackageName((String) t.get("packageName"));
                    btObj.setOpenTableType((String) t.get("openTableType"));
                    btObj.setTableNote((String) t.get("tableNote"));
                    btObj.setCreatedAt(LocalDateTime.now());
                    bookingTableRepo.save(btObj);

                    if (tid != null) {
                        jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                                tid, booking.getStoreId());
                    }
                }
            }
            
            logger.info("=== CREATE BOOKING END ===");
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            logger.error("CREATE BOOKING ERROR:", e);
            throw e;
        }
    }

    @PutMapping("/{bookingId}")
    @Transactional
    public ResponseEntity<BookingMaster> update(@PathVariable String bookingId,
                                                 @RequestBody Map<String, Object> body) {
        Object storeIdObj = getValue(body, "storeId");
        Long storeId = storeIdObj != null ? Long.valueOf(storeIdObj.toString()) : 1L;
        BookingMasterId id = new BookingMasterId(bookingId, storeId);
        Optional<BookingMaster> opt = bookingMasterRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BookingMaster b = opt.get();
        Object bd = getValue(body, "bookingDate");
        if (bd != null) b.setBookingDate(LocalDate.parse(bd.toString()));
        Object bt = getValue(body, "bookingTime");
        if (bt != null) {
            String t = bt.toString();
            if (t.length() == 5) t += ":00";
            b.setBookingTime(java.time.LocalTime.parse(t));
        }
        Object cn = getValue(body, "customerName");
        if (cn != null) b.setCustomerName(cn.toString());
        Object cp = getValue(body, "customerPhone");
        if (cp != null) b.setCustomerPhone(cp.toString());
        Object gc = getValue(body, "guestCount");
        if (gc != null) b.setGuestCount(Integer.valueOf(gc.toString()));
        Object tc = getValue(body, "tableCount");
        if (tc != null) b.setTableCount(Integer.valueOf(tc.toString()));
        Object bs = getValue(body, "bookingStatus");
        if (bs != null) b.setBookingStatus(bs.toString());
        Object bn = getValue(body, "banquetName");
        if (bn != null) b.setBanquetName(bn.toString());
        Object ot = getValue(body, "occasionType");
        if (ot != null) b.setOccasionType(ot.toString());
        Object sr = getValue(body, "specialRequest");
        if (sr != null) b.setSpecialRequest(sr.toString());
        Object r = getValue(body, "remark");
        if (r != null) b.setRemark(r.toString());
        Object ps = getValue(body, "paymentStatus");
        if (ps != null) b.setPaymentStatus(ps.toString());
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
            Object tid = t.get("table_id");
            if (tid != null) {
                Long tableId = Long.valueOf(tid.toString());
                Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
                if (count == null || count == 0) {
                    jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
                }
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
        Object tid = tableInfo.get("table_id");
        Object sid = tableInfo.get("store_id");
        
        if (tid != null && sid != null) {
            Long tableId = Long.valueOf(tid.toString());
            Long storeId = Long.valueOf(sid.toString());

            bookingTableRepo.deleteById(tableBookingId);

            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=?", Integer.class, tableId, storeId);
            if (count == null || count == 0) {
                jdbc.update("UPDATE table_master SET table_status='available' WHERE table_id=? AND store_id=?", tableId, storeId);
            }
        }

        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
