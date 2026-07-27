package com.youjian.banquet.ipad.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetTable;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.repository.BanquetTableRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadTableController {

    @Autowired
    private BanquetTableRepository tableRepo;

    @Autowired
    private BookingMasterRepository bookingRepo;

    @Autowired
    private BookingTableRepository bookingTableRepo;

    @GetMapping("/table/list")
    public Result<List<Map<String, Object>>> getTableList(
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<BanquetTable> tables = tableRepo.findByStoreIdOrderBySortOrder(storeId);

            List<Map<String, Object>> result = tables.stream().map(table -> {
                Map<String, Object> map = new HashMap<>();
                map.put("table_id", table.getTableId());
                map.put("table_number", table.getTableNumber());
                map.put("table_name", table.getTableName());
                map.put("table_area", table.getTableArea());
                map.put("table_capacity", table.getTableCapacity());
                map.put("table_status", table.getTableStatus());
                map.put("min_capacity", table.getMinCapacity());
                map.put("max_capacity", table.getMaxCapacity());
                map.put("sort_order", table.getSortOrder());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询桌台列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/table/all")
    public Result<List<Map<String, Object>>> getAllTables(
            @RequestParam(required = false) String area,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<BanquetTable> tables = tableRepo.findByStoreId(storeId);

            if (area != null && !area.isEmpty()) {
                tables = tables.stream()
                        .filter(t -> area.equals(t.getTableArea()))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> result = tables.stream().map(table -> {
                Map<String, Object> map = new HashMap<>();
                map.put("table_id", table.getTableId());
                map.put("table_number", table.getTableNumber());
                map.put("table_name", table.getTableName());
                map.put("table_area", table.getTableArea());
                map.put("table_capacity", table.getTableCapacity());
                map.put("table_status", table.getTableStatus());
                map.put("min_capacity", table.getMinCapacity());
                map.put("max_capacity", table.getMaxCapacity());
                map.put("sort_order", table.getSortOrder());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询桌台列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/table/filter")
    public Result<List<Map<String, Object>>> filterTablesByStatus(
            @RequestParam String status,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            List<BanquetTable> tables = tableRepo.findByStoreIdAndTableStatus(storeId, status);

            List<Map<String, Object>> result = tables.stream().map(table -> {
                Map<String, Object> map = new HashMap<>();
                map.put("table_id", table.getTableId());
                map.put("table_number", table.getTableNumber());
                map.put("table_name", table.getTableName());
                map.put("table_area", table.getTableArea());
                map.put("table_capacity", table.getTableCapacity());
                map.put("table_status", table.getTableStatus());
                map.put("min_capacity", table.getMinCapacity());
                map.put("max_capacity", table.getMaxCapacity());
                map.put("sort_order", table.getSortOrder());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "筛选桌台失败：" + e.getMessage());
        }
    }

    @PostMapping("/table/open")
    public Result<Map<String, Object>> openTable(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            Long staffId = (Long) request.getAttribute("ipad_staff_id");

            Integer tableId = Integer.parseInt(body.get("table_id").toString());
            Integer guestCount = Integer.parseInt(body.getOrDefault("guest_count", "1").toString());
            String remark = (String) body.getOrDefault("remark", "");

            BanquetTable table = tableRepo.findById(tableId)
                    .orElseThrow(() -> new RuntimeException("桌台不存在"));

            if (!"available".equals(table.getTableStatus())) {
                return Result.error(400, "桌台当前状态不可用");
            }

            String bookingId = "B" + System.currentTimeMillis();
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            BookingMaster booking = new BookingMaster();
            booking.setBookingId(bookingId);
            booking.setStoreId(storeId);
            booking.setBookingDate(today);
            booking.setBookingTime(now);
            booking.setStaffId(staffId.intValue());
            booking.setGuestCount(guestCount);
            booking.setTableCount(1);
            booking.setBookingStatus("pending");
            booking.setBookingType("normal");
            booking.setPaymentStatus("unpaid");
            booking.setRemark(remark);
            booking.setStatus("pending");
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            booking = bookingRepo.save(booking);

            BookingTable bookingTable = new BookingTable();
            bookingTable.setStoreId(storeId);
            bookingTable.setBookingId(bookingId);
            bookingTable.setBookingDate(today);
            bookingTable.setBookingTime(now);
            bookingTable.setTableId(tableId);
            bookingTable.setTableNumber(table.getTableNumber());
            bookingTable.setTableName(table.getTableName());
            bookingTable.setGuestCount(guestCount);
            bookingTable.setCreatedAt(LocalDateTime.now());

            bookingTableRepo.save(bookingTable);

            table.setTableStatus("occupied");
            tableRepo.save(table);

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("table_id", tableId);
            data.put("table_name", table.getTableName());
            data.put("guest_count", guestCount);
            data.put("booking_date", today.toString());
            data.put("booking_time", now.toString());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "开台失败：" + e.getMessage());
        }
    }

    @PostMapping("/table/transfer")
    public Result<Map<String, Object>> transferTable(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");

            String bookingId = (String) body.get("booking_id");
            Integer targetTableId = Integer.parseInt(body.get("target_table_id").toString());
            String type = (String) body.getOrDefault("type", "transfer");

            List<BookingTable> bookingTables = bookingTableRepo.findByBookingId(bookingId);
            if (bookingTables.isEmpty()) {
                return Result.error(404, "预定记录不存在");
            }

            BookingTable oldBookingTable = bookingTables.get(0);
            Integer oldTableId = oldBookingTable.getTableId();

            BanquetTable targetTable = tableRepo.findById(targetTableId)
                    .orElseThrow(() -> new RuntimeException("目标桌台不存在"));

            if (!"available".equals(targetTable.getTableStatus())) {
                return Result.error(400, "目标桌台不可用");
            }

            oldBookingTable.setTableId(targetTableId);
            oldBookingTable.setTableNumber(targetTable.getTableNumber());
            oldBookingTable.setTableName(targetTable.getTableName());
            bookingTableRepo.save(oldBookingTable);

            BanquetTable oldTable = tableRepo.findById(oldTableId).orElse(null);
            if (oldTable != null) {
                oldTable.setTableStatus("available");
                tableRepo.save(oldTable);
            }

            targetTable.setTableStatus("occupied");
            tableRepo.save(targetTable);

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("old_table_id", oldTableId);
            data.put("new_table_id", targetTableId);
            data.put("new_table_name", targetTable.getTableName());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "转台失败：" + e.getMessage());
        }
    }

    @GetMapping("/booking/today")
    public Result<List<Map<String, Object>>> getTodayBookings(
            @RequestParam(required = false) String date,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();

            List<BookingMaster> bookings = bookingRepo.findByStoreIdAndBookingDate(storeId, queryDate);

            List<Map<String, Object>> result = bookings.stream().map(booking -> {
                Map<String, Object> map = new HashMap<>();
                map.put("booking_id", booking.getBookingId());
                map.put("booking_date", booking.getBookingDate().toString());
                map.put("booking_time", booking.getBookingTime().toString());
                map.put("customer_name", booking.getCustomerName());
                map.put("customer_phone", booking.getCustomerPhone());
                map.put("guest_count", booking.getGuestCount());
                map.put("table_count", booking.getTableCount());
                map.put("banquet_name", booking.getBanquetName());
                map.put("booking_status", booking.getBookingStatus());
                map.put("total_amount", booking.getTotalAmount());
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询今日预定失败：" + e.getMessage());
        }
    }

    @GetMapping("/wait/list")
    public Result<List<Map<String, Object>>> getWaitList(HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            LocalDate today = LocalDate.now();

            List<BookingMaster> waitBookings = bookingRepo.findByStoreIdAndBookingDate(storeId, today)
                    .stream()
                    .filter(b -> "wait".equals(b.getBookingType()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = waitBookings.stream().map(booking -> {
                Map<String, Object> map = new HashMap<>();
                map.put("booking_id", booking.getBookingId());
                map.put("customer_name", booking.getCustomerName());
                map.put("guest_count", booking.getGuestCount());
                map.put("booking_time", booking.getBookingTime().toString());
                LocalTime now = LocalTime.now();
                long waitMinutes = java.time.Duration.between(booking.getBookingTime(), now).toMinutes();
                map.put("wait_time", Math.max(0, waitMinutes));
                return map;
            }).collect(Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询等位队列失败：" + e.getMessage());
        }
    }
}
