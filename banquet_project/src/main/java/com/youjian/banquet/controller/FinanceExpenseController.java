package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.FinanceExpense;
import com.youjian.banquet.service.FinanceExpenseService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 费用报销管理。对应 GET/POST /api/finance/expenses。
 * <p>POST 兼具新增与审批：body 含 expenseId + status 时为审批，否则为新增。
 * 与 FinanceController 的 /api/finance/expense(JdbcTemplate 版) 共存。
 */
@RestController
@RequestMapping("/api/finance/expenses")
@CrossOrigin(origins = "*")
public class FinanceExpenseController {

    @Autowired
    private FinanceExpenseService financeExpenseService;

    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    @GetMapping
    public Result<List<FinanceExpense>> list(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String expenseNo,
            @RequestParam(required = false) String applicantName,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status) {
        Long sid = UserContext.isGeneralManager() ? storeId : storeId();
        return Result.success(financeExpenseService.list(sid, expenseNo, applicantName, expenseType, status));
    }

    @PostMapping
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        Object expenseIdObj = body.get("expenseId");
        Object statusObj = body.get("status");
        // 审批分支：已有 expenseId 且仅传 status
        if (expenseIdObj != null && statusObj != null) {
            Long expenseId = Long.parseLong(expenseIdObj.toString());
            FinanceExpense approved = financeExpenseService.approve(expenseId, statusObj.toString());
            return Result.success(Map.of("expenseId", approved.getExpenseId(), "status", approved.getStatus()));
        }
        // 新增分支
        FinanceExpense expense = new FinanceExpense();
        expense.setStoreId(storeId());
        if (body.get("expenseNo") != null) {
            expense.setExpenseNo(body.get("expenseNo").toString());
        }
        if (body.get("applicantName") != null) {
            expense.setApplicantName(body.get("applicantName").toString());
        }
        if (body.get("deptName") != null) {
            expense.setDeptName(body.get("deptName").toString());
        }
        if (body.get("expenseType") != null) {
            expense.setExpenseType(body.get("expenseType").toString());
        }
        if (body.get("amount") != null) {
            expense.setAmount(new BigDecimal(body.get("amount").toString()));
        }
        if (body.get("occurDate") != null) {
            expense.setOccurDate(java.time.LocalDate.parse(body.get("occurDate").toString()));
        }
        if (statusObj != null) {
            expense.setStatus(statusObj.toString());
        }
        if (body.get("remark") != null) {
            expense.setRemark(body.get("remark").toString());
        }
        FinanceExpense saved = financeExpenseService.create(expense);
        return Result.success(Map.of("expenseId", saved.getExpenseId()));
    }
}
