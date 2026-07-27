package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetTable;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.repository.BanquetTableRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TableController {

    @Autowired private BanquetTableRepository tableRepo;
    @Autowired private BookingMasterRepository bookingRepo;
    @Autowired private BookingTableRepository bookingTableRepo;

    /** GET /api/tables — get all tables, with optional area filter */
    @GetMapping("/tables")
    public Result<List<BanquetTable>> getTables(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(required = false) String area) {
        try {
            List<BanquetTable> list;
            if (area != null && !area.isEmpty() && !"all".equals(area)) {
                list = tableRepo.findByTableAreaAndStoreIdOrderBySortOrder(area, storeId);
            } else {
                list = tableRepo.findByStoreIdOrderBySortOrder(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取桌台列表失败: " + e.getMessage());
        }
    }

    /** PUT /api/tables/{id} — update table (also called by frontend with status field) */
    @PutMapping("/tables/{id}")
    public Result<BanquetTable> updateTable(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            BanquetTable t = tableRepo.findById(id).orElse(null);
            if (t == null) return Result.error(404, "桌台不存在");
            if (body.containsKey("table_status") || body.containsKey("status"))
                t.setTableStatus((String) body.getOrDefault("table_status", body.get("status")));
            if (body.containsKey("table_name")) t.setTableName((String) body.get("table_name"));
            if (body.containsKey("table_number")) t.setTableNumber((String) body.get("table_number"));
            if (body.containsKey("table_area")) t.setTableArea((String) body.get("table_area"));
            if (body.containsKey("table_capacity")) t.setTableCapacity(Integer.valueOf(body.get("table_capacity").toString()));
            if (body.containsKey("table_type")) t.setTableType((String) body.get("table_type"));
            if (body.containsKey("table_location")) t.setTableLocation((String) body.get("table_location"));
            if (body.containsKey("sort_order")) t.setSortOrder(Integer.valueOf(body.get("sort_order").toString()));
            if (body.containsKey("is_active")) t.setIsActive(Integer.valueOf(body.get("is_active").toString()));
            tableRepo.save(t);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "更新桌台失败: " + e.getMessage());
        }
    }

    /** POST /api/tables/reorder — reorder tables (frontend sends POST) */
    @PostMapping("/tables/reorder")
    public Result<?> reorderTablesPost(@RequestBody List<Map<String, Object>> orderList) {
        return reorderTables(orderList);
    }

    /** PUT /api/tables/reorder — reorder tables (compat) */
    @PutMapping("/tables/reorder")
    public Result<?> reorderTables(@RequestBody List<Map<String, Object>> orderList) {
        try {
            for (Map<String, Object> item : orderList) {
                Integer id = (Integer) item.get("table_id");
                Integer sort = (Integer) item.get("sort_order");
                tableRepo.findById(id).ifPresent(t -> { t.setSortOrder(sort); tableRepo.save(t); });
            }
            return Result.success("排序已更新");
        } catch (Exception e) {
            return Result.error(500, "排序失败: " + e.getMessage());
        }
    }

    /** POST /api/tables — add new table */
    @PostMapping("/tables")
    public Result<BanquetTable> addTable(@RequestBody BanquetTable table) {
        try {
            table.setTableId(null);
            table.setIsActive(1);
            table.setTableStatus("available");
            BanquetTable saved = tableRepo.save(table);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "添加桌台失败: " + e.getMessage());
        }
    }

    /** DELETE /api/tables/{id} — delete table */
    @DeleteMapping("/tables/{id}")
    public Result<?> deleteTable(@PathVariable Integer id) {
        try {
            if (!tableRepo.existsById(id)) return Result.error(404, "桌台不存在");
            tableRepo.deleteById(id);
            return Result.success("桌台已删除");
        } catch (Exception e) {
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }

    public Result<List<Map<String, Object>>> getTableBoard(@RequestParam(defaultValue = "1") Long storeId,
                                                             @RequestParam(required = false) String date,
                                                             @RequestParam(defaultValue = "all") String period) {
        try {
            LocalDate d = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            List<BanquetTable> tables = tableRepo.findByStoreIdOrderBySortOrder(storeId);
            List<BookingMaster> todayBookings = bookingRepo.findByBookingDateAndStoreIdOrderByBookingTime(d, storeId);

            // 构建 桌台ID → 午/晚预订 的索引
            Map<Integer, BookingMaster> morningMap = new HashMap<>();   // booking_time < 14:00
            Map<Integer, BookingMaster> afternoonMap = new HashMap<>(); // booking_time >= 14:00
            for (BookingMaster bm : todayBookings) {
                List<BookingTable> bts = bookingTableRepo.findByBookingId(bm.getBookingNo());
                boolean isMorning = bm.getBookingTime() != null && bm.getBookingTime().getHour() < 14;
                for (BookingTable bt : bts) {
                    if (isMorning) {
                        morningMap.putIfAbsent(bt.getTableId(), bm);
                    } else {
                        afternoonMap.putIfAbsent(bt.getTableId(), bm);
                    }
                }
            }

            // 扁平化输出：每个桌台一行，预订信息在同一层级
            List<Map<String, Object>> result = new ArrayList<>();
            for (BanquetTable t : tables) {
                Map<String, Object> row = new LinkedHashMap<>();
                // 桌台基础字段（snake_case，匹配前端期望）
                row.put("table_id", t.getTableId());
                row.put("store_id", t.getStoreId());
                row.put("table_number", t.getTableNumber());  // table_number 同 table_id
                row.put("table_name", t.getTableName());
                row.put("table_area", t.getTableArea());
                row.put("table_capacity", t.getTableCapacity());
                row.put("table_type", t.getTableType());
                row.put("sort_order", t.getSortOrder());
                row.put("is_active", t.getIsActive());

                // 按 period 筛选预订
                BookingMaster primary = null;
                BookingMaster secondary = null;
                if ("morning".equals(period)) {
                    primary = morningMap.get(t.getTableId());
                } else if ("afternoon".equals(period)) {
                    primary = afternoonMap.get(t.getTableId());
                } else {
                    // all：午餐为主，晚餐为辅
                    primary = morningMap.get(t.getTableId());
                    secondary = afternoonMap.get(t.getTableId());
                    if (primary == null && secondary != null) {
                        primary = secondary;
                        secondary = null;
                    }
                }

                // 主预订信息（扁平到同一层）
                if (primary != null) {
                    row.put("booking_id", primary.getBookingId());
                    row.put("booking_date", primary.getBookingDate() != null ? primary.getBookingDate().toString() : null);
                    row.put("booking_time", primary.getBookingTime() != null ? primary.getBookingTime().toString() : null);
                    row.put("customer_name", primary.getCustomerName());
                    row.put("customer_phone", primary.getCustomerPhone());
                    row.put("booking_status", primary.getStatus() != null ? primary.getStatus() : primary.getBookingStatus());
                    row.put("banquet_name", primary.getBanquetName());
                    row.put("occasion_type", primary.getOccasionType());
                    row.put("bm_guest_count", primary.getGuestCount());
                    row.put("guest_count", primary.getGuestCount());
                    // 菜品数（从 booking_table 统计，暂返回0）
                    row.put("dishes_count", 0);
                    // 回头次数（查同手机号历史预订数）
                    int visits = 0;
                    if (primary.getCustomerPhone() != null && !primary.getCustomerPhone().isEmpty()) {
                        visits = bookingRepo.countByCustomerPhoneAndStoreId(primary.getCustomerPhone(), storeId);
                    }
                    row.put("visit_count", Math.max(visits - 1, 0));  // 减1因为含本次
                    row.put("table_status", "occupied");
                } else {
                    row.put("table_status", "available");
                }

                // 全天模式：辅预订（午餐+晚餐双标）
                if (secondary != null) {
                    row.put("booking_id2", secondary.getBookingId());
                    row.put("customer_name2", secondary.getCustomerName());
                    row.put("dishes_count2", 0);
                }

                result.add(row);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取看板失败: " + e.getMessage());
        }
    }

    /** POST /api/tables/swap-booking — swap table booking assignments */
    @PostMapping("/tables/swap-booking")
    public Result<?> swapTableBooking(@RequestBody Map<String, Object> body) {
        try {
            Integer tableId1 = Integer.valueOf(body.get("table_id_1").toString());
            Integer tableId2 = Integer.valueOf(body.get("table_id_2").toString());
            // Find current bookings for these tables
            List<BookingTable> bts = bookingTableRepo.findAll();
            BookingTable bt1 = null, bt2 = null;
            for (BookingTable bt : bts) {
                if (bt.getTableId().equals(tableId1)) bt1 = bt;
                if (bt.getTableId().equals(tableId2)) bt2 = bt;
            }
            if (bt1 != null) { bt1.setTableId(tableId2); bookingTableRepo.save(bt1); }
            if (bt2 != null) { bt2.setTableId(tableId1); bookingTableRepo.save(bt2); }
            return Result.success("桌台互换成功");
        } catch (Exception e) {
            return Result.error(500, "桌台互换失败: " + e.getMessage());
        }
    }

    // --- Staff endpoints (called by booking.js — paths /staff, /menu-api/staff) ---
    @Autowired private com.youjian.banquet.repository.StaffRepository staffRepo;

    @GetMapping("/staff")
    public Result<List<com.youjian.banquet.entity.Staff>> getStaff() {
        try {
            List<com.youjian.banquet.entity.Staff> list = staffRepo.findByEmploymentStatus("active");
            if (list.isEmpty()) list = staffRepo.findAll();
            return Result.success(list);
        } catch (Exception e) { return Result.error(500, "获取员工列表失败"); }
    }
    @PostMapping("/staff")
    public Result<com.youjian.banquet.entity.Staff> createStaff(@RequestBody com.youjian.banquet.entity.Staff staff) {
        try { staff.setStoreId(staff.getStoreId() != null ? staff.getStoreId() : 1L); staff.setEmploymentStatus("active"); return Result.success(staffRepo.save(staff)); }
        catch (Exception e) { return Result.error(500, "创建员工失败"); }
    }
    @PutMapping("/staff/{id}")
    public Result<com.youjian.banquet.entity.Staff> putStaff(@PathVariable Integer id, @RequestBody com.youjian.banquet.entity.Staff s) {
        try {
            var e = staffRepo.findById(id).orElse(null); if (e == null) return Result.error(404, "员工不存在");
            if (s.getStaffName() != null) e.setStaffName(s.getStaffName());
            if (s.getStaffPhone() != null) e.setStaffPhone(s.getStaffPhone());
            if (s.getStaffPosition() != null) e.setStaffPosition(s.getStaffPosition());
            if (s.getDepartment() != null) e.setDepartment(s.getDepartment());
            return Result.success(staffRepo.save(e));
        } catch (Exception ex) { return Result.error(500, "更新员工失败"); }
    }
    @DeleteMapping("/staff/{id}")
    public Result<?> delStaff(@PathVariable Integer id) {
        try { var e = staffRepo.findById(id).orElse(null); if (e == null) return Result.error(404, "员工不存在"); e.setEmploymentStatus("resigned"); staffRepo.save(e); return Result.success("已标记离职"); }
        catch (Exception ex) { return Result.error(500, "删除员工失败"); }
    }
    @GetMapping("/menu-api/staff")
    public Result<List<com.youjian.banquet.entity.Staff>> getMenuStaff() {
        try {
            List<com.youjian.banquet.entity.Staff> list = staffRepo.findByEmploymentStatus("active");
            if (list.isEmpty()) list = staffRepo.findAll();
            return Result.success(list);
        } catch (Exception e) { return Result.error(500, "获取员工列表失败"); }
    }

    // --- Helper methods ---
    private void updateTableStatus(Integer tableId, String status) {
        tableRepo.findById(tableId).ifPresent(t -> {
            t.setTableStatus(status);
            tableRepo.save(t);
        });
    }

    private LocalTime parseTime(Object val) {
        if (val == null) return null;
        String s = val.toString();
        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            // Try HH:mm format
            if (s.length() == 5 && s.charAt(2) == ':') {
                return LocalTime.of(Integer.parseInt(s.substring(0, 2)), Integer.parseInt(s.substring(3)));
            }
            return null;
        }
    }
}
