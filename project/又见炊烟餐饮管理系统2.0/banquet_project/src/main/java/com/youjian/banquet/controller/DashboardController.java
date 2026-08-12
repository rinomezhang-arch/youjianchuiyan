package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.DashboardDTO;
import com.youjian.banquet.dto.ReportDTO;
import com.youjian.banquet.service.DashboardService;
import com.youjian.banquet.util.UserContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏 + 工作台首页控制器。
 * <p>
 * 双门店数据隔离规则：
 * <ul>
 *   <li>店长（store_id != 0）：仅返回本店数据，storeId 参数被强制覆盖为 UserContext.currentStoreId()</li>
 *   <li>总经理（store_id == 0, isDataScopeAll()=true）：可选全门店汇总（storeId=all）或单店明细（storeId=具体值）</li>
 * </ul>
 */
@RestController
@RequestMapping(value = {"/api/dashboard"})
@CrossOrigin
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 今日数据大屏。
     * 店长仅返回本店今日数据；总经理可传 storeId=all 查看双店汇总或具体 storeId 查看单店。
     */
    @GetMapping(value = {"/today"})
    public ApiResponse<DashboardDTO> getTodayDashboard(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        return ApiResponse.success(this.dashboardService.getTodayDashboard(effectiveStoreId));
    }

    /* ====================== 工作台首页 5 个接口 ====================== */

    /**
     * 经营概览（KPI 指标）。
     * 返回：今日营收/营收同比/各门店营收/客流/客流同比/翻台率/毛利率/净利/订单数等。
     */
    @GetMapping(value = {"/overview"})
    public ApiResponse<Map<String, Object>> getOverview(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String effectiveStoreId = resolveEffectiveStoreId(store);
        DashboardDTO d = this.dashboardService.getTodayDashboard(effectiveStoreId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayRevenue", d.getTodayRevenue());
        data.put("yesterdayRevenue", d.getYesterdayRevenue());
        data.put("revenueByStore", d.getRevenueByStore());
        data.put("revenueTrendPct", d.getRevenueTrendPct());
        data.put("todayTraffic", d.getTodayTraffic());
        data.put("trafficTrendPct", d.getTrafficTrendPct());
        data.put("turnoverRate", d.getTurnoverRate());
        data.put("turnoverTrendPct", d.getTurnoverTrendPct());
        data.put("grossMarginRate", d.getGrossMarginRate());
        data.put("grossMarginTrendPct", d.getGrossMarginTrendPct());
        data.put("netProfitEstimate", d.getNetProfitEstimate());
        data.put("netProfitTrendPct", d.getNetProfitTrendPct());
        data.put("orderCount", d.getOrderCount());
        data.put("orderTrendPct", d.getOrderTrendPct());
        data.put("orderByChannel", d.getOrderByChannel());
        data.put("costBreakdown", d.getCostBreakdown());
        data.put("statusBreakdown", d.getStatusBreakdown());
        return ApiResponse.success(data);
    }

    /**
     * 今日预定：包厢 / 宴席 / 空包厢预警 / 明日预定。
     */
    @GetMapping(value = {"/today-bookings"})
    public ApiResponse<Map<String, Object>> getTodayBookings(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String effectiveStoreId = resolveEffectiveStoreId(store);
        DashboardDTO d = this.dashboardService.getTodayDashboard(effectiveStoreId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayBoxBookings", d.getTodayBoxBookings());
        data.put("todayBanquetBookings", d.getTodayBanquetBookings());
        data.put("emptyBoxWarningCount", d.getEmptyBoxWarningCount());
        data.put("tomorrowTotal", d.getTomorrowTotal());
        data.put("tomorrowLunch", d.getTomorrowLunch());
        data.put("tomorrowDinner", d.getTomorrowDinner());
        data.put("todayBoxList", d.getTodayBoxList());
        data.put("todayBanquetList", d.getTodayBanquetList());
        data.put("emptyBoxList", d.getEmptyBoxList());
        data.put("recentBookings", d.getRecentBookings());
        return ApiResponse.success(data);
    }

    /**
     * 7 天营收趋势（折线图）。
     */
    @GetMapping(value = {"/revenue-chart"})
    public ApiResponse<Object> getRevenueChart(
            @RequestParam(required = false, defaultValue = "all") String store,
            @RequestParam(required = false, defaultValue = "7") int days) {
        String effectiveStoreId = resolveEffectiveStoreId(store);
        return ApiResponse.success(this.dashboardService.getRevenueChart(effectiveStoreId, days));
    }

    /**
     * 热门菜品 Top N。
     */
    @GetMapping(value = {"/hot-dishes"})
    public ApiResponse<Object> getHotDishes(
            @RequestParam(required = false, defaultValue = "all") String store,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        String effectiveStoreId = resolveEffectiveStoreId(store);
        return ApiResponse.success(this.dashboardService.getHotDishes(effectiveStoreId, limit));
    }

    /**
     * 风险预警：低库存 / 待采购 / 待审批 / 空包厢 + 待审批列表。
     */
    @GetMapping(value = {"/alerts"})
    public ApiResponse<Map<String, Object>> getAlerts(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String effectiveStoreId = resolveEffectiveStoreId(store);
        DashboardDTO d = this.dashboardService.getTodayDashboard(effectiveStoreId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("riskWarnings", d.getRiskWarnings());
        data.put("riskCounts", d.getRiskCounts());
        data.put("pendingApprovalCount", d.getPendingApprovalCount());
        data.put("approvalByType", d.getApprovalByType());
        data.put("pendingApprovalList", d.getPendingApprovalList());
        data.put("navBadges", d.getNavBadges());
        data.put("lowStockCount", d.getLowStockCount());
        data.put("pendingPurchases", d.getPendingPurchases());
        return ApiResponse.success(data);
    }

    @GetMapping(value = {"/today-reservations"})
    public ApiResponse<List<Map<String, Object>>> todayReservations(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String sid = resolveEffectiveStoreId(store);
        try {
            String sql = "SELECT b.booking_id, b.booking_no, b.customer_name, b.customer_phone, b.booking_time AS arrival_time, " +
                    "b.guest_count, b.table_count, b.booking_status AS status, b.banquet_name, b.total_amount " +
                    "FROM booking_master b WHERE DATE(b.booking_date) = CURDATE()";
            if (!"all".equalsIgnoreCase(sid) && sid != null && !sid.isEmpty()) {
                sql += " AND b.store_id = " + Long.parseLong(sid);
            }
            sql += " ORDER BY b.booking_time ASC LIMIT 20";
            return ApiResponse.success(this.jdbc.queryForList(sql));
        } catch (Exception e) {
            return ApiResponse.success(new ArrayList<>());
        }
    }

    @GetMapping(value = {"/upcoming-banquets"})
    public ApiResponse<List<Map<String, Object>>> upcomingBanquets(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String sid = resolveEffectiveStoreId(store);
        try {
            String sql = "SELECT b.booking_id, b.booking_no, b.customer_name, b.booking_date, b.booking_time, " +
                    "b.banquet_name, b.occasion_type, b.guest_count, b.table_count, b.booking_status AS status, b.deposit_amount " +
                    "FROM booking_master b WHERE b.booking_date >= CURDATE() AND b.occasion_type IS NOT NULL";
            if (!"all".equalsIgnoreCase(sid) && sid != null && !sid.isEmpty()) {
                sql += " AND b.store_id = " + Long.parseLong(sid);
            }
            sql += " ORDER BY b.booking_date ASC, b.booking_time ASC LIMIT 10";
            return ApiResponse.success(this.jdbc.queryForList(sql));
        } catch (Exception e) {
            return ApiResponse.success(new ArrayList<>());
        }
    }

    @GetMapping(value = {"/recent-activity"})
    public ApiResponse<List<Map<String, Object>>> recentActivity(
            @RequestParam(required = false, defaultValue = "all") String store) {
        String sid = resolveEffectiveStoreId(store);
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            String where = "1=1";
            if (!"all".equalsIgnoreCase(sid) && sid != null && !sid.isEmpty()) {
                where = "store_id = " + Long.parseLong(sid);
            }
            // 最近订单/预定
            String sqlB = "SELECT CONCAT('预定:',booking_no) as title, CONCAT(customer_name,' 预订了 ',guest_count,' 人') as content, " +
                    "created_at as happened_at, 'booking' as type FROM booking_master WHERE " + where +
                    " ORDER BY created_at DESC LIMIT 8";
            out.addAll(jdbc.queryForList(sqlB));
            try {
                String sqlO = "SELECT CONCAT('订单:',order_no) as title, CONCAT('新增订单 金额 ',total_amount) as content, " +
                        "order_time as happened_at, 'order' as type FROM order_master WHERE " + where +
                        " ORDER BY order_time DESC LIMIT 8";
                out.addAll(jdbc.queryForList(sqlO));
            } catch (Exception ignored) {}
            out.sort(Comparator.comparing(m -> {
                Object v = m.get("happened_at");
                if (v == null) return "";
                return v.toString();
            }, Comparator.reverseOrder()));
            if (out.size() > 15) out = new ArrayList<>(out.subList(0, 15));
            return ApiResponse.success(out);
        } catch (Exception e) {
            return ApiResponse.success(new ArrayList<>());
        }
    }

    /**
     * 报表查询。
     * 店长仅返回本店报表；总经理可传 storeId=all 查看双店合并汇总或具体 storeId 查看单店明细。
     */
    @GetMapping(value = {"/report"})
    public ApiResponse<ReportDTO> getReport(
            @RequestParam(required = false, defaultValue = "all") String storeId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate == null) {
            switch (period.toLowerCase()) {
                case "month": startDate = today.withDayOfMonth(1); break;
                case "quarter": startDate = today.withMonth(((today.getMonthValue()-1)/3)*3+1).withDayOfMonth(1); break;
                case "year": startDate = today.withDayOfYear(1); break;
                default: startDate = today.minusDays(6); break;
            }
        }
        if (endDate == null) { endDate = today; }
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        return ApiResponse.success(this.dashboardService.getReport(effectiveStoreId, period, startDate, endDate));
    }

    /**
     * 依据当前登录用户的数据范围解析最终 storeId。
     * <ul>
     *   <li>店长（非全门店权限）：强制使用 UserContext.currentStoreId()，忽略前端传入值</li>
     *   <li>总经理（全门店权限）：原样使用前端传入值（"all" 或具体门店ID）</li>
     *   <li>未登录或上下文缺失：原样返回前端传入值（由全局鉴权拦截器拒绝）</li>
     * </ul>
     */
    private String resolveEffectiveStoreId(String requestedStoreId) {
        if (!UserContext.isDataScopeAll()) {
            Long currentStoreId = UserContext.currentStoreId();
            if (currentStoreId != null && currentStoreId > 0) {
                return String.valueOf(currentStoreId);
            }
        }
        return requestedStoreId;
    }
}
