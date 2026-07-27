package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
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
    public Result<List<BookingMaster>> list(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            List<BookingMaster> bookings = bookingMasterRepo.findByStoreIdOrderByBookingDateDesc(storeId);
            return Result.success(bookings);
        } catch (Exception e) {
            return Result.error(500, "获取预订列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public Result<Map<String, Object>> detail(@PathVariable String bookingId,
                                               @RequestParam(defaultValue = "1") Long storeId) {
        try {
            BookingMasterId id = new BookingMasterId(bookingId, storeId);
            Optional<BookingMaster> master = bookingMasterRepo.findById(id);
            if (master.isEmpty()) return Result.error(404, "预订不存在");

            List<BookingTable> tables = bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId);
            List<BookingDishDetail> dishes = bookingDishDetailRepo.findByBookingId(bookingId);

            Map<String, Object> result = new HashMap<>();
            result.put("booking", master.get());
            result.put("tables", tables);
            result.put("dishes", dishes);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取预订详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result<List<BookingMaster>> search(@RequestParam(defaultValue = "1") Long storeId,
                                               @RequestParam(required = false) String keyword) {
        try {
            return Result.success(bookingMasterRepo.search(storeId, keyword));
        } catch (Exception e) {
            return Result.error(500, "搜索失败: " + e.getMessage());
        }
    }

    @GetMapping("/date/{date}")
    public Result<List<BookingMaster>> byDate(@PathVariable String date,
                                               @RequestParam(defaultValue = "1") Long storeId) {
        try {
            LocalDate d = LocalDate.parse(date);
            return Result.success(bookingMasterRepo.findByStoreIdAndBookingDate(storeId, d));
        } catch (Exception e) {
            return Result.error(500, "获取日期预订失败: " + e.getMessage());
        }
    }

    @GetMapping("/range")
    public Result<List<BookingMaster>> byDateRange(@RequestParam String start,
                                                    @RequestParam String end,
                                                    @RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(bookingMasterRepo.findByDateRange(storeId,
                    LocalDate.parse(start), LocalDate.parse(end)));
        } catch (Exception e) {
            return Result.error(500, "获取日期范围预订失败: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    public Result<List<BookingMaster>> byStatus(@PathVariable String status,
                                                 @RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(bookingMasterRepo.findByStoreIdAndBookingStatus(storeId, status));
        } catch (Exception e) {
            return Result.error(500, "获取状态预订失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Transactional
    public Result<BookingMaster> create(@RequestBody Map<String, Object> body) {
        try {
            BookingMaster booking = new BookingMaster();
            String bookingId = "BK" + System.currentTimeMillis();
            booking.setBookingId(bookingId);
            
            Object storeIdObj = getValue(body, "storeId");
            booking.setStoreId(storeIdObj != null ? Long.valueOf(storeIdObj.toString()) : 1L);
            
            Object bd = getValue(body, "bookingDate");
            booking.setBookingDate(bd != null ? LocalDate.parse(bd.toString()) : LocalDate.now());
            
            Object bt = getValue(body, "bookingTime");
            if (bt != null) {
                String timeStr = bt.toString();
                if (timeStr.length() == 5) timeStr += ":00";
                booking.setBookingTime(java.time.LocalTime.parse(timeStr));
            }
            
            Object cn = getValue(body, "customerName");
            booking.setCustomerName(cn != null ? cn.toString() : null);
            
            Object cp = getValue(body, "customerPhone");
            booking.setCustomerPhone(cp != null ? cp.toString() : null);
            
            Object gc = getValue(body, "guestCount");
            if (gc != null) booking.setGuestCount(Integer.valueOf(gc.toString()));
            
            Object tc = getValue(body, "tableCount");
            if (tc != null) booking.setTableCount(Integer.valueOf(tc.toString()));
            
            Object st = getValue(body, "spareTables");
            if (st != null) booking.setSpareTables(Integer.valueOf(st.toString()));
            
            Object ot = getValue(body, "occasionType");
            booking.setOccasionType(ot != null ? ot.toString() : null);
            
            Object dep = getValue(body, "deposit");
            if (dep != null) booking.setDeposit(new BigDecimal(dep.toString()));
            
            Object r = getValue(body, "remark");
            booking.setRemark(r != null ? r.toString() : null);
            
            Object bs = getValue(body, "bookingStatus");
            booking.setBookingStatus(bs != null ? bs.toString() : "confirmed");
            
            Object ps = getValue(body, "paymentStatus");
            booking.setPaymentStatus(ps != null ? ps.toString() : "unpaid");
            
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            BookingMaster saved = bookingMasterRepo.save(booking);

            List<Integer> tableIds = (List<Integer>) getValue(body, "tableIds");
            List<String> tableNames = (List<String>) getValue(body, "tableNames");
            
            if (tableIds != null && tableIds.size() > 0) {
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
            }
            
            return Result.success(saved);
            
        } catch (Exception e) {
            logger.error("CREATE BOOKING ERROR:", e);
            return Result.error(500, "创建预订失败: " + e.getMessage());
        }
    }

    @PutMapping("/{bookingId}")
    @Transactional
    public Result<BookingMaster> update(@PathVariable String bookingId,
                                         @RequestBody Map<String, Object> body) {
        try {
            Object storeIdObj = getValue(body, "storeId");
            Long storeId = storeIdObj != null ? Long.valueOf(storeIdObj.toString()) : 1L;
            BookingMasterId id = new BookingMasterId(bookingId, storeId);
            Optional<BookingMaster> opt = bookingMasterRepo.findById(id);
            if (opt.isEmpty()) return Result.error(404, "预订不存在");

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

            return Result.success(bookingMasterRepo.save(b));
        } catch (Exception e) {
            return Result.error(500, "更新预订失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}")
    @Transactional
    public Result<?> delete(@PathVariable String bookingId,
                             @RequestParam(defaultValue = "1") Long storeId) {
        try {
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

            return Result.success(Map.of("deleted", true, "bookingId", bookingId));
        } catch (Exception e) {
            return Result.error(500, "删除预订失败: " + e.getMessage());
        }
    }

    @GetMapping("/{bookingId}/tables")
    public Result<List<BookingTable>> getTables(@PathVariable String bookingId,
                                                 @RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(bookingTableRepo.findByBookingIdAndStoreId(bookingId, storeId));
        } catch (Exception e) {
            return Result.error(500, "获取预订桌台失败: " + e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/tables")
    @Transactional
    public Result<BookingTable> addTable(@PathVariable String bookingId,
                                          @RequestBody BookingTable table) {
        try {
            table.setBookingId(bookingId);
            table.setCreatedAt(LocalDateTime.now());
            BookingTable saved = bookingTableRepo.save(table);

            jdbc.update("UPDATE table_master SET table_status='occupied' WHERE table_id=? AND store_id=?",
                    table.getTableId(), table.getStoreId());

            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "添加桌台失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{bookingId}/tables/{tableBookingId}")
    @Transactional
    public Result<?> deleteTable(@PathVariable String bookingId, @PathVariable Long tableBookingId) {
        try {
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

            return Result.success(Map.of("deleted", true));
        } catch (Exception e) {
            return Result.error(500, "删除桌台失败: " + e.getMessage());
        }
    }
}
