package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.FinanceVoucher;
import com.youjian.banquet.service.FinanceVoucherService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 凭证总账管理。对应 GET/POST /api/finance/vouchers。
 * <p>与 FinanceController 的 /api/finance/voucher(JdbcTemplate 版) 共存。
 */
@RestController
@RequestMapping("/api/finance/vouchers")
@CrossOrigin(origins = "*")
public class FinanceVoucherController {

    @Autowired
    private FinanceVoucherService financeVoucherService;

    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    @GetMapping
    public Result<List<FinanceVoucher>> list(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long sid = UserContext.isGeneralManager() ? storeId : storeId();
        return Result.success(financeVoucherService.list(sid, voucherNo, voucherType, startDate, endDate));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody FinanceVoucher body) {
        body.setStoreId(storeId());
        FinanceVoucher saved = financeVoucherService.create(body);
        return Result.success(Map.of("voucherId", saved.getVoucherId()));
    }
}
