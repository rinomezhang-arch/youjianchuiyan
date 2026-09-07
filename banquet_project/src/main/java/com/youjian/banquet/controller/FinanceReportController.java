package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.FinanceReportService;
import com.youjian.banquet.util.ReportQueryScope;
import org.springframework.http.ResponseEntity;
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
        Long sid = ReportQueryScope.resolve(storeId);
        String m = resolveMonth(month);
        return Result.success(financeReportService.profitReport(sid, m));
    }

    @GetMapping("/balance-report")
    public Result<Map<String, Object>> balanceReport(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String month) {
        Long sid = ReportQueryScope.resolve(storeId);
        String m = resolveMonth(month);
        return Result.success(financeReportService.balanceReport(sid, m));
    }
    @ExceptionHandler(ReportQueryScope.ScopeException.class)
    public ResponseEntity<Result<Void>> scopeError(ReportQueryScope.ScopeException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Result.error(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> queryError(Exception ex) {
        return ResponseEntity.status(500).body(Result.error(500, "报表查询失败"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> invalidMonth(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Result.error(400, "月份格式必须为有效的YYYY-MM"));
    }

}
