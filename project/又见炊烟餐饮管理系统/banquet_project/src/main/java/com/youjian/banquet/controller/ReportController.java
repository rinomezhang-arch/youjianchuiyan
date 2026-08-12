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

            StringBuilder sql = new StringBuilder("SELECT * FROM report_daily_summary WHERE summary_date >= ? AND summary_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" ORDER BY summary_date ASC");
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

            StringBuilder sql = new StringBuilder("SELECT * FROM report_revenue WHERE revenue_date >= ? AND revenue_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" ORDER BY revenue_date ASC");
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

            StringBuilder sql = new StringBuilder("SELECT * FROM report_dish_sales WHERE stat_date >= ? AND stat_date <= ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" ORDER BY stat_date DESC, sale_amount DESC");
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

            StringBuilder sql = new StringBuilder("SELECT * FROM report_department_cost WHERE stat_date >= ? AND stat_date < ?");
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(monthStart));
            params.add(java.sql.Date.valueOf(nextMonthStart));
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" ORDER BY stat_date DESC, total_cost DESC");
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

            StringBuilder sql = new StringBuilder("SELECT * FROM report_staff_kpi WHERE stat_month = ?");
            List<Object> params = new ArrayList<>();
            params.add(m);
            if (sid != null) {
                sql.append(" AND store_id = ?");
                params.add(sid);
            }
            sql.append(" ORDER BY performance_score DESC, sale_amount DESC");
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

            StringBuilder baseWhere = new StringBuilder(" WHERE 1=1");
            List<Object> baseParams = new ArrayList<>();
            if (sid != null) {
                baseWhere.append(" AND store_id = ?");
                baseParams.add(sid);
            }

            List<Object> todayParams = new ArrayList<>(baseParams);
            todayParams.add(java.sql.Date.valueOf(today));
            java.math.BigDecimal todayRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(total_revenue),0) FROM report_daily_summary" + baseWhere + " AND summary_date = ?",
                    todayParams.toArray());

            List<Object> monthParams = new ArrayList<>(baseParams);
            monthParams.add(java.sql.Date.valueOf(monthStart));
            monthParams.add(java.sql.Date.valueOf(nextMonthStart));
            java.math.BigDecimal monthRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(total_revenue),0) FROM report_daily_summary" + baseWhere + " AND summary_date >= ? AND summary_date < ?",
                    monthParams.toArray());
            java.math.BigDecimal monthCost = sumOrZero(
                    "SELECT COALESCE(SUM(total_cost),0) FROM report_daily_summary" + baseWhere + " AND summary_date >= ? AND summary_date < ?",
                    monthParams.toArray());
            java.math.BigDecimal monthProfit = monthRevenue.subtract(monthCost);

            List<Object> lastParams = new ArrayList<>(baseParams);
            lastParams.add(java.sql.Date.valueOf(lastMonthStart));
            lastParams.add(java.sql.Date.valueOf(monthStart));
            java.math.BigDecimal lastMonthRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(total_revenue),0) FROM report_daily_summary" + baseWhere + " AND summary_date >= ? AND summary_date < ?",
                    lastParams.toArray());

            double momPct = lastMonthRevenue.signum() == 0 ? 0.0
                    : monthRevenue.subtract(lastMonthRevenue)
                            .divide(lastMonthRevenue.abs(), 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue() * 100.0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("todayRevenue", todayRevenue);
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
