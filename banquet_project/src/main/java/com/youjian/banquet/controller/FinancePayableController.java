package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.service.FinancePayableService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 供应商应付管理。对应 GET/POST /api/finance/payables。
 * <p>POST 兼具新增与结算：body 含 payableId + settleAmount 时为结算，否则为新增。
 * 与 FinanceController 的 /api/finance/payable(JdbcTemplate 版) 共存。
 */
@RestController
@RequestMapping("/api/finance/payables")
@CrossOrigin(origins = "*")
public class FinancePayableController {

    @Autowired
    private FinancePayableService financePayableService;

    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    @GetMapping
    public Result<List<FinancePayable>> list(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {
        Long sid = UserContext.isGeneralManager() ? storeId : storeId();
        return Result.success(financePayableService.list(sid, supplierId, status));
    }

    @PostMapping
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        Object payableIdObj = body.get("payableId");
        if (payableIdObj != null) {
            // 结算分支
            Long payableId = Long.parseLong(payableIdObj.toString());
            BigDecimal settleAmount = body.get("settleAmount") != null
                    ? new BigDecimal(body.get("settleAmount").toString())
                    : BigDecimal.ZERO;
            FinancePayable settled = financePayableService.settle(payableId, settleAmount);
            return Result.success(Map.of("payableId", settled.getPayableId(), "unpaidAmount", settled.getUnpaidAmount()));
        }
        // 新增分支
        FinancePayable payable = new FinancePayable();
        payable.setStoreId(storeId());
        if (body.get("supplierId") != null) {
            payable.setSupplierId(Long.parseLong(body.get("supplierId").toString()));
        }
        if (body.get("totalAmount") != null) {
            payable.setTotalAmount(new BigDecimal(body.get("totalAmount").toString()));
        }
        if (body.get("paidAmount") != null) {
            payable.setPaidAmount(new BigDecimal(body.get("paidAmount").toString()));
        }
        if (body.get("status") != null) {
            payable.setStatus(body.get("status").toString());
        }
        if (body.get("remark") != null) {
            payable.setRemark(body.get("remark").toString());
        }
        FinancePayable saved = financePayableService.create(payable);
        return Result.success(Map.of("payableId", saved.getPayableId()));
    }
}
