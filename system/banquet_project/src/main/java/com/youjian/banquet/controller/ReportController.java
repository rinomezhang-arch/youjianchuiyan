package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private Long resolveStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) {
                return null;
            }
            try {
                return Long.parseLong(storeId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }

    private String currentMonth() {
        return LocalDate.now().format(MONTH_FMT);
    }

    @GetMapping("/daily-summary")
    public Result<List<Map<String, Object>>> dailySummary(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Long sid = resolveStoreId(storeId);
            LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : end.minusDays(29);

            StringBuilder sql = new StringBuilder("SELECT booking_date AS summary_date, store_id, COUNT(*) AS total_bookings, COALESCE(SUM(guest_count),0) AS total_guests, COALESCE(SUM(CASE WHEN booking_status='completed' THEN 1 ELSE 0 END),0) AS completed_bookings, COALESCE(SUM(CASE WHEN payment_status='paid' THEN final_amount ELSE 0 END),0) AS total_revenue FROM booking_master WHERE booking_date >= ? AND booking_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" GROUP BY booking_date, store_id ORDER BY summary_date ASC");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询每日汇总失败: " + e.getMessage());
        }
    }

    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Long sid = resolveStoreId(storeId);
            LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : end.minusDays(29);

            StringBuilder sql = new StringBuilder("SELECT trans_date AS revenue_date, store_id, payment_method, COUNT(*) AS transaction_count, COALESCE(SUM(amount),0) AS revenue_amount FROM finance_transaction WHERE trans_type='income' AND trans_date >= ? AND trans_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" GROUP BY trans_date, store_id, payment_method ORDER BY revenue_date ASC");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询营收报表失败: " + e.getMessage());
        }
    }

    @GetMapping("/dish-sales")
    public Result<List<Map<String, Object>>> dishSales(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Long sid = resolveStoreId(storeId);
            LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : end.minusDays(29);

            StringBuilder sql = new StringBuilder("SELECT b.booking_date AS stat_date, d.store_id, d.dish_id, d.dish_name, COALESCE(SUM(d.dish_quantity),0) AS sale_quantity, COALESCE(SUM(d.subtotal),0) AS sale_amount FROM booking_dish_detail d JOIN booking_master b ON b.booking_id=d.booking_id AND b.store_id=d.store_id WHERE d.kitchen_status <> 'refunded' AND b.booking_date >= ? AND b.booking_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND d.store_id = ?");
                params.add(sid);
            }
            sql.append(" GROUP BY b.booking_date, d.store_id, d.dish_id, d.dish_name ORDER BY stat_date DESC, sale_amount DESC");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询菜品销售失败: " + e.getMessage());
        }
    }

    @GetMapping("/department-cost")
    public Result<List<Map<String, Object>>> departmentCost(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String month) {
        try {
            Long sid = resolveStoreId(storeId);
            String m = (month != null && !month.isEmpty()) ? month : currentMonth();
            LocalDate monthStart = LocalDate.parse(m + "-01");
            LocalDate nextMonthStart = monthStart.plusMonths(1);

            StringBuilder sql = new StringBuilder("SELECT trans_date AS stat_date, store_id, COALESCE(trans_category,'其他') AS department_name, COUNT(*) AS item_count, COALESCE(SUM(amount),0) AS total_cost FROM finance_transaction WHERE trans_type='expense' AND trans_date >= ? AND trans_date < ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(monthStart));
            params.add(java.sql.Date.valueOf(nextMonthStart));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" GROUP BY trans_date, store_id, trans_category ORDER BY stat_date DESC, total_cost DESC");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询部门成本失败: " + e.getMessage());
        }
    }

    @GetMapping("/staff-kpi")
    public Result<List<Map<String, Object>>> staffKpi(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String month) {
        try {
            Long sid = resolveStoreId(storeId);
            String m = (month != null && !month.isEmpty()) ? month : currentMonth();

            LocalDate monthStart = LocalDate.parse(m + "-01");
            LocalDate nextMonthStart = monthStart.plusMonths(1);
            StringBuilder sql = new StringBuilder("SELECT DATE_FORMAT(booking_date,'%Y-%m') AS stat_month, store_id, staff_id, staff_name, COUNT(*) AS booking_count, COALESCE(SUM(guest_count),0) AS served_guests, COALESCE(SUM(CASE WHEN payment_status='paid' THEN final_amount ELSE 0 END),0) AS sale_amount, ROUND(100 * SUM(CASE WHEN booking_status='completed' THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0),1) AS performance_score FROM booking_master WHERE booking_date >= ? AND booking_date < ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(monthStart));
            params.add(java.sql.Date.valueOf(nextMonthStart));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" GROUP BY store_id, staff_id, staff_name ORDER BY performance_score DESC, sale_amount DESC");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询员工KPI失败: " + e.getMessage());
        }
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate nextMonthStart = monthStart.plusMonths(1);
            LocalDate lastMonthStart = monthStart.minusMonths(1);

            String tenantClause = sid == null ? "" : " AND store_id = ?";
            List<Object> tenantParams = sid == null ? new ArrayList<>() : new ArrayList<>(List.of(sid));

            List<Object> todayParams = new ArrayList<>(tenantParams);
            todayParams.add(java.sql.Date.valueOf(today));
            java.math.BigDecimal todayRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_transaction WHERE trans_type='income'" + tenantClause + " AND trans_date = ?",
                    todayParams.toArray());

            List<Object> monthParams = new ArrayList<>(tenantParams);
            monthParams.add(java.sql.Date.valueOf(monthStart));
            monthParams.add(java.sql.Date.valueOf(nextMonthStart));
            java.math.BigDecimal monthRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_transaction WHERE trans_type='income'" + tenantClause + " AND trans_date >= ? AND trans_date < ?",
                    monthParams.toArray());
            java.math.BigDecimal monthCost = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_transaction WHERE trans_type='expense'" + tenantClause + " AND trans_date >= ? AND trans_date < ?",
                    monthParams.toArray());
            java.math.BigDecimal monthProfit = monthRevenue.subtract(monthCost);

            List<Object> lastParams = new ArrayList<>(tenantParams);
            lastParams.add(java.sql.Date.valueOf(lastMonthStart));
            lastParams.add(java.sql.Date.valueOf(monthStart));
            java.math.BigDecimal lastMonthRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_transaction WHERE trans_type='income'" + tenantClause + " AND trans_date >= ? AND trans_date < ?",
                    lastParams.toArray());

            double momPct = lastMonthRevenue.signum() == 0 ? 0.0
                    : monthRevenue.subtract(lastMonthRevenue)
                            .divide(lastMonthRevenue.abs(), 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue() * 100.0;

            Map<String, Object> data = new LinkedHashMap<>();
            Object[] tenantArgs = sid == null ? new Object[]{} : new Object[]{sid};
            Integer todayBookings = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_master WHERE booking_date = CURDATE()" + tenantClause,
                    Integer.class, tenantArgs);
            Integer todayGuests = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(guest_count),0) FROM booking_master WHERE booking_date = CURDATE() AND booking_status <> 'cancelled'" + tenantClause,
                    Integer.class, tenantArgs);
            Integer occupiedTables = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM table_master WHERE table_status IN ('occupied','dining')" + tenantClause,
                    Integer.class, tenantArgs);

            data.put("todayRevenue", todayRevenue);
            data.put("todayBookings", Optional.ofNullable(todayBookings).orElse(0));
            data.put("todayGuests", Optional.ofNullable(todayGuests).orElse(0));
            data.put("occupiedTables", Optional.ofNullable(occupiedTables).orElse(0));
            data.put("monthRevenue", monthRevenue);
            data.put("monthCost", monthCost);
            data.put("monthProfit", monthProfit);
            data.put("lastMonthRevenue", lastMonthRevenue);
            data.put("momPct", Math.round(momPct * 10.0) / 10.0);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询报表概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/operations")
    public Result<Map<String, Object>> operations(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Long sid = resolveStoreId(storeId);
            LocalDate end = parseDate(endDate, LocalDate.now());
            LocalDate start = parseDate(startDate, end.minusDays(29));
            if (start.isAfter(end) || start.isBefore(end.minusYears(2))) {
                return Result.error(400, "日期范围无效，最长支持两年");
            }

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("period", Map.of("startDate", start.toString(), "endDate", end.toString()));
            report.put("overview", operationalOverview(sid, start, end));
            report.put("daily", operationalDaily(sid, start, end));
            report.put("paymentMix", paymentMix(sid, start, end));
            report.put("dishRanking", dishRanking(sid, start, end));
            report.put("staffKpi", operationalStaffKpi(sid, start, end));
            report.put("departmentCost", operationalDepartmentCost(sid, start, end));
            report.put("bookingDetails", bookingDetails(sid, start, end));
            report.put("generatedAt", java.time.LocalDateTime.now().toString());
            report.put("reportCount", 7);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error(500, "查询营运综合报表失败: " + e.getMessage());
        }
    }

    private Map<String, Object> operationalOverview(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        Map<String, Object> row = jdbc.queryForMap("SELECT COUNT(*) totalBookings, " +
                        "COALESCE(SUM(CASE WHEN booking_status='completed' THEN 1 ELSE 0 END),0) completedBookings, " +
                        "COALESCE(SUM(CASE WHEN booking_status='cancelled' THEN 1 ELSE 0 END),0) cancelledBookings, " +
                        "COALESCE(SUM(CASE WHEN booking_status<>'cancelled' THEN guest_count ELSE 0 END),0) totalGuests, " +
                        "COALESCE(SUM(CASE WHEN payment_status='paid' THEN final_amount ELSE 0 END),0) bookingRevenue " +
                        "FROM booking_master WHERE booking_date BETWEEN ? AND ?" + tenant, args.toArray());
        java.math.BigDecimal revenue = decimal(row.get("bookingRevenue"));
        long guests = number(row.get("totalGuests"));
        long total = number(row.get("totalBookings"));
        long completed = number(row.get("completedBookings"));
        row.put("averageCheck", guests == 0 ? java.math.BigDecimal.ZERO : revenue.divide(java.math.BigDecimal.valueOf(guests), 2, java.math.RoundingMode.HALF_UP));
        row.put("completionRate", total == 0 ? 0 : Math.round(completed * 1000.0 / total) / 10.0);
        row.put("revPASH", revenue);
        return row;
    }

    private List<Map<String, Object>> operationalDaily(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT booking_date reportDate, COUNT(*) bookingCount, " +
                "COALESCE(SUM(CASE WHEN booking_status<>'cancelled' THEN guest_count ELSE 0 END),0) guestCount, " +
                "COALESCE(SUM(CASE WHEN payment_status='paid' THEN final_amount ELSE 0 END),0) revenue, " +
                "COALESCE(SUM(deposit_amount),0) deposit, COALESCE(SUM(CASE WHEN booking_status='cancelled' THEN 1 ELSE 0 END),0) cancelled " +
                "FROM booking_master WHERE booking_date BETWEEN ? AND ?" + tenant + " GROUP BY booking_date ORDER BY booking_date", args.toArray());
    }

    private List<Map<String, Object>> paymentMix(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT COALESCE(payment_method,'其他') paymentMethod, COUNT(*) transactionCount, COALESCE(SUM(amount),0) amount " +
                "FROM finance_transaction WHERE trans_type='income' AND trans_date BETWEEN ? AND ?" + tenant +
                " GROUP BY payment_method ORDER BY amount DESC", args.toArray());
    }

    private List<Map<String, Object>> dishRanking(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND d.store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT d.dish_id dishId, d.dish_name dishName, COALESCE(SUM(d.dish_quantity),0) quantity, " +
                "COALESCE(SUM(d.subtotal),0) salesAmount FROM booking_dish_detail d JOIN booking_master b " +
                "ON b.booking_id=d.booking_id AND b.store_id=d.store_id WHERE b.booking_date BETWEEN ? AND ? " +
                "AND d.kitchen_status<>'refunded'" + tenant + " GROUP BY d.dish_id,d.dish_name ORDER BY salesAmount DESC LIMIT 20", args.toArray());
    }

    private List<Map<String, Object>> operationalStaffKpi(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT staff_id staffId, COALESCE(staff_name,'未分配') staffName, COUNT(*) bookingCount, " +
                "COALESCE(SUM(guest_count),0) servedGuests, COALESCE(SUM(CASE WHEN payment_status='paid' THEN final_amount ELSE 0 END),0) salesAmount " +
                "FROM booking_master WHERE booking_date BETWEEN ? AND ?" + tenant +
                " GROUP BY staff_id,staff_name ORDER BY salesAmount DESC LIMIT 30", args.toArray());
    }

    private List<Map<String, Object>> operationalDepartmentCost(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT COALESCE(trans_category,'其他') department, COUNT(*) itemCount, COALESCE(SUM(amount),0) totalCost " +
                "FROM finance_transaction WHERE trans_type='expense' AND trans_date BETWEEN ? AND ?" + tenant +
                " GROUP BY trans_category ORDER BY totalCost DESC", args.toArray());
    }

    private List<Map<String, Object>> bookingDetails(Long sid, LocalDate start, LocalDate end) {
        String tenant = sid == null ? "" : " AND store_id=?";
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)));
        if (sid != null) args.add(sid);
        return jdbc.queryForList("SELECT booking_no bookingNo, booking_date bookingDate, booking_time bookingTime, customer_name customerName, " +
                "guest_count guestCount, booking_type bookingType, staff_name staffName, final_amount finalAmount, booking_status bookingStatus, payment_status paymentStatus " +
                "FROM booking_master WHERE booking_date BETWEEN ? AND ?" + tenant + " ORDER BY booking_date DESC,booking_time DESC LIMIT 1000", args.toArray());
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private java.math.BigDecimal decimal(Object value) {
        if (value instanceof java.math.BigDecimal n) return n;
        if (value instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        return java.math.BigDecimal.ZERO;
    }

    private java.math.BigDecimal sumOrZero(String sql, Object... args) {
        try {
            java.math.BigDecimal v = jdbc.queryForObject(sql, java.math.BigDecimal.class, args);
            return v == null ? java.math.BigDecimal.ZERO : v;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
