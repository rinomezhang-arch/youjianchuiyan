package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.DashboardDTO;
import com.youjian.banquet.dto.ReportDTO;
import com.youjian.banquet.service.DashboardService;
import com.youjian.banquet.util.UserContext;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/dashboard"})
@CrossOrigin
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping(value = {"/today"})
    public ApiResponse<DashboardDTO> getTodayDashboard(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        return ApiResponse.success(this.dashboardService.getTodayDashboard(effectiveStoreId));
    }

    @GetMapping(value = {"/report"})
    public ApiResponse<ReportDTO> getReport(
            @RequestParam(required = false, defaultValue = "all") String storeId,
            @RequestParam String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        return ApiResponse.success(this.dashboardService.getReport(effectiveStoreId, period, startDate, endDate));
    }

    @GetMapping(value = {"/overview", "/screen/overview"})
    public ApiResponse<Map<String, Object>> getOverview(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        DashboardDTO dto = this.dashboardService.getTodayDashboard(effectiveStoreId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayRevenue", dto.getTodayRevenue());
        result.put("todayBookings", dto.getOrderCount());
        result.put("todayGuests", dto.getTodayTraffic());
        result.put("memberRate", 0);
        result.put("totalCustomers", dto.getTotalCustomers());
        result.put("totalDishes", dto.getTotalDishes());
        result.put("totalTables", dto.getTotalTables());
        return ApiResponse.success(result);
    }

    @GetMapping(value = {"/today-bookings"})
    public ApiResponse<List<Map<String, Object>>> getTodayBookings(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        DashboardDTO dto = this.dashboardService.getTodayDashboard(effectiveStoreId);
        return ApiResponse.success(dto.getRecentBookings() != null ? dto.getRecentBookings() : new ArrayList<>());
    }

    @GetMapping(value = {"/revenue-chart", "/screen/revenue-trend"})
    public ApiResponse<List<Map<String, Object>>> getRevenueChart(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        List<Map<String, Object>> trend = this.dashboardService.getRevenueTrend(effectiveStoreId);
        return ApiResponse.success(trend);
    }

    @GetMapping(value = {"/hot-dishes", "/screen/hot-dishes"})
    public ApiResponse<List<Map<String, Object>>> getHotDishes(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        List<Map<String, Object>> dishes = this.dashboardService.getHotDishes(effectiveStoreId);
        return ApiResponse.success(dishes);
    }

    @GetMapping(value = {"/alerts", "/screen/alerts"})
    public ApiResponse<List<Map<String, Object>>> getAlerts(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        DashboardDTO dto = this.dashboardService.getTodayDashboard(effectiveStoreId);
        return ApiResponse.success(dto.getRiskWarnings() != null ? dto.getRiskWarnings() : new ArrayList<>());
    }

    @GetMapping(value = {"/screen/customer-analysis"})
    public ApiResponse<List<Map<String, Object>>> getCustomerAnalysis(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        List<Map<String, Object>> result = this.dashboardService.getCustomerAnalysis(effectiveStoreId);
        return ApiResponse.success(result);
    }

    @GetMapping(value = {"/screen/cost-analysis"})
    public ApiResponse<List<Map<String, Object>>> getCostAnalysis(
            @RequestParam(required = false, defaultValue = "all") String storeId) {
        String effectiveStoreId = resolveEffectiveStoreId(storeId);
        DashboardDTO dto = this.dashboardService.getTodayDashboard(effectiveStoreId);
        Map<String, Double> breakdown = dto.getCostBreakdown();
        List<Map<String, Object>> result = new ArrayList<>();
        if (breakdown != null) {
            breakdown.forEach((k, v) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", translateCostName(k));
                item.put("value", v);
                result.add(item);
            });
        }
        return ApiResponse.success(result);
    }

    private String translateCostName(String key) {
        switch (key) {
            case "food": return "食材";
            case "labor": return "人工";
            case "energy": return "水电";
            default: return key;
        }
    }

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
