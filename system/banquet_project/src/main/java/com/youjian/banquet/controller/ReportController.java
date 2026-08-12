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

    private java.math.BigDecimal sumOrZero(String sql, Object... args) {
        try {
            java.math.BigDecimal v = jdbc.queryForObject(sql, java.math.BigDecimal.class, args);
            return v == null ? java.math.BigDecimal.ZERO : v;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
