package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetTable;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.repository.BanquetTableRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BanquetTableRepository tableRepo;

    @Autowired
    private BookingMasterRepository bookingRepo;

    /**
     * GET /api/dashboard/kpi — 获取KPI数据
     */
    @GetMapping("/dashboard/kpi")
    public Result<Map<String, Object>> getKpi() {
        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.toString();
            String yesterdayStr = today.minusDays(1).toString();

            // 查询今日营收
            Double todayRevenue = getRevenueByDate(todayStr);
            Double yesterdayRevenue = getRevenueByDate(yesterdayStr);

            // 查询订单数
            Integer orderCount = getOrderCountByDate(todayStr);

            // 查询客流
            Integer traffic = getTrafficByDate(todayStr);

            // 查询桌台统计
            List<BanquetTable> tables = tableRepo.findByStoreIdOrderBySortOrder(1L);
            int totalTables = tables.size();
            long occupiedTables = tables.stream().filter(t -> "occupied".equals(t.getTableStatus())).count();
            double turnoverRate = totalTables > 0 ? Math.round((occupiedTables * 100.0 / totalTables) * 10.0) / 10.0 : 0;

            // 查询各门店营收
            Map<String, Object> storeRevenue = getStoreRevenue();

            Map<String, Object> kpi = new HashMap<>();
            kpi.put("totalRevenue", todayRevenue);
            kpi.put("ningguoRevenue", storeRevenue.get("ningguo"));
            kpi.put("xuanchengRevenue", storeRevenue.get("xuancheng"));
            kpi.put("hangzhouRevenue", storeRevenue.get("hangzhou"));
            kpi.put("revenueTrend", yesterdayRevenue > 0 ? Math.round((todayRevenue - yesterdayRevenue) / yesterdayRevenue * 1000.0) / 10.0 : 0);
            kpi.put("traffic", traffic);
            kpi.put("trafficTrend", 5.0);
            kpi.put("hourlyTraffic", getHourlyTraffic(todayStr));
            kpi.put("turnoverRate", turnoverRate);
            kpi.put("turnoverTrend", 3.0);
            kpi.put("grossMargin", 68.5);
            kpi.put("grossMarginTrend", 1.8);
            kpi.put("netProfit", Math.round(todayRevenue * 0.15));
            kpi.put("netProfitTrend", 15.2);
            kpi.put("costBreakdown", Map.of("food", 28, "labor", 18, "energy", 5));
            kpi.put("orderCount", orderCount);
            kpi.put("orderTrend", 6.5);
            kpi.put("channelDineIn", orderCount > 0 ? Math.round(orderCount * 0.67) : 0);
            kpi.put("channelTakeout", orderCount > 0 ? Math.round(orderCount * 0.25) : 0);
            kpi.put("channelBanquet", orderCount > 0 ? Math.round(orderCount * 0.08) : 0);

            return Result.success(kpi);
        } catch (Exception e) {
            return Result.error(500, "获取KPI数据失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/dashboard/booking-overview — 获取预订概览
     */
    @GetMapping("/dashboard/booking-overview")
    public Result<Map<String, Object>> getBookingOverview(@RequestParam(required = false) String date) {
        try {
            String queryDate = date != null ? date : LocalDate.now().toString();
            LocalDate today = LocalDate.parse(queryDate);
            LocalDate tomorrow = today.plusDays(1);

            // 查询今日预订
            List<BookingMaster> todayBookings = bookingRepo.findByStoreIdAndBookingDate(1L, today);
            List<BookingMaster> tomorrowBookings = bookingRepo.findByStoreIdAndBookingDate(1L, tomorrow);

            Map<String, Object> result = new HashMap<>();
            result.put("todayBoxes", todayBookings.size());
            result.put("todayList", todayBookings.stream()
                .limit(4)
                .map(b -> Map.of(
                    "id", b.getBookingId(),
                    "box", b.getBookingTable() != null ? b.getBookingTable() : "包厢",
                    "time", b.getBookingTime() != null ? b.getBookingTime().toString() : "12:00",
                    "name", b.getCustomerName() != null ? b.getCustomerName() : "客户"
                ))
                .collect(Collectors.toList()));

            // 宴会厅统计
            long banquetCount = todayBookings.stream()
                .filter(b -> "banquet".equals(b.getBanquetType()))
                .count();
            result.put("banquetCount", banquetCount);
            result.put("banquetList", todayBookings.stream()
                .filter(b -> "banquet".equals(b.getBanquetType()))
                .limit(3)
                .map(b -> Map.of(
                    "id", b.getBookingId(),
                    "box", b.getBookingTable() != null ? b.getBookingTable() : "宴会厅",
                    "date", b.getBookingDate() != null ? b.getBookingDate().toString() : "",
                    "guests", b.getGuestCount() != null ? b.getGuestCount() : 0
                ))
                .collect(Collectors.toList()));

            result.put("emptyWarning", 0);
            result.put("emptyList", Collections.emptyList());
            result.put("tomorrowTotal", tomorrowBookings.size());
            result.put("tomorrowLunch", tomorrowBookings.stream()
                .filter(b -> b.getBookingTime() != null && b.getBookingTime().getHour() < 15)
                .count());
            result.put("tomorrowDinner", tomorrowBookings.stream()
                .filter(b -> b.getBookingTime() != null && b.getBookingTime().getHour() >= 15)
                .count());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取预订概览失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/approvals — 获取审批列表
     */
    @GetMapping("/approvals")
    public Result<Map<String, Object>> getApprovals(@RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<Map<String, Object>> approvals = new ArrayList<>();

            // 查询待审批的采购申请
            try {
                List<Map<String, Object>> procurementList = jdbcTemplate.queryForList(
                    "SELECT id, title, department, created_at, amount, status FROM purchase_requests WHERE status = 'pending' ORDER BY created_at DESC LIMIT ?",
                    pageSize
                );
                for (Map<String, Object> item : procurementList) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", item.get("id"));
                    approval.put("type", "procurement");
                    approval.put("title", item.get("title"));
                    approval.put("department", item.get("department"));
                    approval.put("time", formatTime(item.get("created_at")));
                    approval.put("amount", item.get("amount") != null ? "¥" + item.get("amount") : "");
                    approval.put("status", "pending");
                    approval.put("statusText", "待审批");
                    approval.put("link", "approval-center");
                    approvals.add(approval);
                }
            } catch (Exception e) {
                // 表不存在则跳过
            }

            // 查询待审批的请假申请
            try {
                List<Map<String, Object>> leaveList = jdbcTemplate.queryForList(
                    "SELECT id, reason, department, created_at FROM leave_requests WHERE status = 'pending' ORDER BY created_at DESC LIMIT ?",
                    pageSize
                );
                for (Map<String, Object> item : leaveList) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", item.get("id"));
                    approval.put("type", "leave");
                    approval.put("title", item.get("reason"));
                    approval.put("department", item.get("department"));
                    approval.put("time", formatTime(item.get("created_at")));
                    approval.put("amount", "");
                    approval.put("status", "pending");
                    approval.put("statusText", "待审批");
                    approval.put("link", "approval-center");
                    approvals.add(approval);
                }
            } catch (Exception e) {
                // 表不存在则跳过
            }

            // 查询待审批的费用报销
            try {
                List<Map<String, Object>> expenseList = jdbcTemplate.queryForList(
                    "SELECT id, title, department, amount, created_at FROM expense_reimbursements WHERE status = 'pending' ORDER BY created_at DESC LIMIT ?",
                    pageSize
                );
                for (Map<String, Object> item : expenseList) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", item.get("id"));
                    approval.put("type", "expense");
                    approval.put("title", item.get("title"));
                    approval.put("department", item.get("department"));
                    approval.put("time", formatTime(item.get("created_at")));
                    approval.put("amount", item.get("amount") != null ? "¥" + item.get("amount") : "");
                    approval.put("status", "pending");
                    approval.put("statusText", "待审批");
                    approval.put("link", "approval-center");
                    approvals.add(approval);
                }
            } catch (Exception e) {
                // 表不存在则跳过
            }

            // 审批标签页统计
            List<Map<String, Object>> tabs = Arrays.asList(
                createApprovalTab("all", "全部", "", approvals.size()),
                createApprovalTab("procurement", "采购申请", "", (int) approvals.stream().filter(a -> "procurement".equals(a.get("type"))).count()),
                createApprovalTab("leave", "员工请假", "", (int) approvals.stream().filter(a -> "leave".equals(a.get("type"))).count()),
                createApprovalTab("repair", "维修报修", "", 0),
                createApprovalTab("expense", "费用报销", "", (int) approvals.stream().filter(a -> "expense".equals(a.get("type"))).count()),
                createApprovalTab("reconciliation", "供应商对账", "", 0)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("list", approvals);
            result.put("tabs", tabs);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取审批列表失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/dashboard/warnings — 获取风险预警
     */
    @GetMapping("/dashboard/warnings")
    public Result<List<Map<String, Object>>> getWarnings() {
        try {
            List<Map<String, Object>> warnings = new ArrayList<>();

            // 查询临期食材
            try {
                List<Map<String, Object>> expiringItems = jdbcTemplate.queryForList(
                    "SELECT id, name, expire_date FROM ingredients WHERE expire_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND status = 'active' LIMIT 5"
                );
                if (!expiringItems.isEmpty()) {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("type", "expiring");
                    warning.put("title", "食材临期预警");
                    warning.put("desc", expiringItems.size() + "种食材即将过期，请及时处理");
                    warning.put("count", expiringItems.size());
                    warning.put("level", "warning");
                    warning.put("icon", "<path d=\"M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z\"/>");
                    warning.put("link", "inventory");
                    warnings.add(warning);
                }
            } catch (Exception e) {
                // 表不存在则跳过
            }

            // 查询卫生检查问题
            try {
                List<Map<String, Object>> hygieneIssues = jdbcTemplate.queryForList(
                    "SELECT id, title FROM hygiene_checks WHERE status = 'failed' ORDER BY created_at DESC LIMIT 5"
                );
                if (!hygieneIssues.isEmpty()) {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("type", "hygiene");
                    warning.put("title", "卫生巡检不合格");
                    warning.put("desc", hygieneIssues.size() + "项卫生检查未达标");
                    warning.put("count", hygieneIssues.size());
                    warning.put("level", "danger");
                    warning.put("icon", "<circle cx=\"12\" cy=\"12\" r=\"10\"/><line x1=\"12\" y1=\"8\" x2=\"12\" y2=\"12\"/><line x1=\"12\" y1=\"16\" x2=\"12.01\" y2=\"16\"/>");
                    warning.put("link", "hygiene");
                    warnings.add(warning);
                }
            } catch (Exception e) {
                // 表不存在则跳过
            }

            return Result.success(warnings);
        } catch (Exception e) {
            return Result.error(500, "获取风险预警失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/front-office/stats — 获取前台统计
     */
    @GetMapping("/front-office/stats")
    public Result<Map<String, Object>> getFrontOfficeStats() {
        try {
            LocalDate today = LocalDate.now();

            // 查询桌台统计
            List<BanquetTable> tables = tableRepo.findByStoreIdOrderBySortOrder(1L);
            long occupiedCount = tables.stream().filter(t -> "occupied".equals(t.getTableStatus())).count();
            long pendingCount = tables.stream().filter(t -> "pending".equals(t.getTableStatus())).count();

            // 查询今日预订
            List<BookingMaster> todayBookings = bookingRepo.findByStoreIdAndBookingDate(1L, today);
            int guestCount = todayBookings.stream()
                .mapToInt(b -> b.getGuestCount() != null ? b.getGuestCount() : 0)
                .sum();

            // 查询今日营收
            String todayStr = today.toString();
            Double todayRevenue = getRevenueByDate(todayStr);

            Map<String, Object> stats = new HashMap<>();
            stats.put("guestCount", guestCount);
            stats.put("tableCount", (int) occupiedCount);
            stats.put("avgGuests", occupiedCount > 0 ? Math.round(guestCount / (double) occupiedCount) : 0);
            stats.put("pendingTables", (int) pendingCount);
            stats.put("todayRevenue", todayRevenue);
            stats.put("revenueGrowth", 0);
            stats.put("pendingComplaints", 0);

            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(500, "获取前台统计失败: " + e.getMessage());
        }
    }

    private Double getRevenueByDate(String date) {
        try {
            Double revenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE DATE(created_at) = ? AND status != 'cancelled'",
                Double.class,
                date
            );
            return revenue != null ? revenue : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer getOrderCountByDate(String date) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = ? AND status != 'cancelled'",
                Integer.class,
                date
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Integer getTrafficByDate(String date) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(guest_count), 0) FROM orders WHERE DATE(created_at) = ? AND status != 'cancelled'",
                Integer.class,
                date
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Integer> getHourlyTraffic(String date) {
        List<Integer> hourlyData = new ArrayList<>(Collections.nCopies(12, 0));
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT HOUR(created_at) as hour, COUNT(*) as count FROM orders WHERE DATE(created_at) = ? AND status != 'cancelled' GROUP BY HOUR(created_at)",
                date
            );
            for (Map<String, Object> row : results) {
                int hour = ((Number) row.get("hour")).intValue();
                int count = ((Number) row.get("count")).intValue();
                // 将0-23小时映射到12个时间段
                int index = Math.min(hour / 2, 11);
                hourlyData.set(index, hourlyData.get(index) + count);
            }
        } catch (Exception e) {
            // 使用默认分布
            hourlyData = Arrays.asList(20, 35, 55, 70, 85, 95, 80, 65, 50, 35, 25, 15);
        }
        return hourlyData;
    }

    private Map<String, Object> getStoreRevenue() {
        Map<String, Object> result = new HashMap<>();
        result.put("ningguo", 0.0);
        result.put("xuancheng", 0.0);
        result.put("hangzhou", 0.0);

        try {
            LocalDate today = LocalDate.now();
            String todayStr = today.toString();

            List<Map<String, Object>> revenues = jdbcTemplate.queryForList(
                "SELECT s.store_code, COALESCE(SUM(o.total_amount), 0) as revenue " +
                "FROM stores s LEFT JOIN orders o ON s.id = o.store_id AND DATE(o.created_at) = ? " +
                "WHERE s.store_code IN ('ningguo', 'xuancheng', 'hangzhou') " +
                "GROUP BY s.store_code",
                todayStr
            );

            for (Map<String, Object> row : revenues) {
                String storeCode = (String) row.get("store_code");
                Double revenue = (Double) row.get("revenue");
                result.put(storeCode, revenue != null ? revenue : 0.0);
            }
        } catch (Exception e) {
            // 使用默认值
        }

        return result;
    }

    private Map<String, Object> createApprovalTab(String key, String name, String icon, int count) {
        Map<String, Object> tab = new HashMap<>();
        tab.put("key", key);
        tab.put("name", name);
        tab.put("icon", icon);
        tab.put("count", count);
        return tab;
    }

    private String formatTime(Object timeObj) {
        if (timeObj == null) return "";
        try {
            if (timeObj instanceof LocalDateTime) {
                LocalDateTime time = (LocalDateTime) timeObj;
                long minutesAgo = (System.currentTimeMillis() - time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()) / 60000;
                if (minutesAgo < 60) return minutesAgo + "分钟前";
                if (minutesAgo < 1440) return (minutesAgo / 60) + "小时前";
                return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            return timeObj.toString();
        } catch (Exception e) {
            return timeObj.toString();
        }
    }
}
