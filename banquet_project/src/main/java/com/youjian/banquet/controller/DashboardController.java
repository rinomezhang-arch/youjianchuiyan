package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.DashboardDTO;
import com.youjian.banquet.dto.ReportDTO;
import com.youjian.banquet.service.DashboardService;
import com.youjian.banquet.util.UserContext;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏控制器。
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

    /**
     * 报表查询。
     * 店长仅返回本店报表；总经理可传 storeId=all 查看双店合并汇总或具体 storeId 查看单店明细。
     */
    @GetMapping(value = {"/report"})
    public ApiResponse<ReportDTO> getReport(
            @RequestParam(required = false, defaultValue = "all") String storeId,
            @RequestParam String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
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
