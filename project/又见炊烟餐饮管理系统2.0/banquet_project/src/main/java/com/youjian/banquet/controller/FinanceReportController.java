package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.FinanceReportService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 财务报表控制器。
 * <p>利润表：GET /api/finance/profit-report?month=YYYY-MM
 * <p>资产负债表：GET /api/finance/balance-report?month=YYYY-MM
 * <p>总经理（store_id=0）可查全门店汇总，店长仅查本店。
 */
@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceReportController {

    @Autowired
    private FinanceReportService financeReportService;

    /** 总经理传 storeId=all/空 → 查全门店；店长强制本店。null 表示不按门店过滤。 */
    private Long resolveQueryStoreId(String storeId) {
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
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    private String resolveMonth(String month) {
        if (month == null || month.isEmpty()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return month;
    }

    @GetMapping("/profit-report")
    public Result<Map<String, Object>> profitReport(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String month) {
        Long sid = resolveQueryStoreId(storeId);
        String m = resolveMonth(month);
        return Result.success(financeReportService.profitReport(sid, m));
    }

    @GetMapping("/balance-report")
    public Result<Map<String, Object>> balanceReport(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String month) {
        Long sid = resolveQueryStoreId(storeId);
        String m = resolveMonth(month);
        return Result.success(financeReportService.balanceReport(sid, m));
    }
}
