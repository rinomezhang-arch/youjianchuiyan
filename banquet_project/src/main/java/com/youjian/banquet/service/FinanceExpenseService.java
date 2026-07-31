package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinanceExpense;
import com.youjian.banquet.repository.FinanceExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销服务。对应 GET/POST /api/finance/expenses。
 * POST 兼具新增与审批：body 含 expenseId 时为审批（更新 status），否则为新增。
 */
@Service
public class FinanceExpenseService {

    @Autowired
    private FinanceExpenseRepository financeExpenseRepository;

    public List<FinanceExpense> list(Long storeId, String expenseNo, String applicantName,
                                     String expenseType, String status) {
        boolean hasNo = expenseNo != null && !expenseNo.isEmpty();
        boolean hasApp = applicantName != null && !applicantName.isEmpty();
        boolean hasType = expenseType != null && !expenseType.isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();

        if (hasNo && hasType && hasStatus) {
            return financeExpenseRepository
                    .findByStoreIdAndExpenseNoContainingAndExpenseTypeAndStatusOrderByExpenseIdDesc(
                            storeId, expenseNo, expenseType, status);
        }
        if (hasNo) {
            return financeExpenseRepository.findByStoreIdAndExpenseNoContainingOrderByExpenseIdDesc(storeId, expenseNo);
        }
        if (hasApp) {
            return financeExpenseRepository
                    .findByStoreIdAndApplicantNameContainingOrderByExpenseIdDesc(storeId, applicantName);
        }
        if (hasType) {
            return financeExpenseRepository.findByStoreIdAndExpenseTypeOrderByExpenseIdDesc(storeId, expenseType);
        }
        if (hasStatus) {
            return financeExpenseRepository.findByStoreIdAndStatusOrderByExpenseIdDesc(storeId, status);
        }
        return financeExpenseRepository.findByStoreIdOrderByExpenseIdDesc(storeId);
    }

    @Transactional
    public FinanceExpense create(FinanceExpense expense) {
        if (expense.getExpenseId() == null) {
            expense.setExpenseId(System.currentTimeMillis());
        }
        if (expense.getExpenseNo() == null || expense.getExpenseNo().isEmpty()) {
            expense.setExpenseNo("EX" + expense.getExpenseId());
        }
        if (expense.getAmount() == null) {
            expense.setAmount(BigDecimal.ZERO);
        }
        if (expense.getOccurDate() == null) {
            expense.setOccurDate(LocalDate.now());
        }
        if (expense.getStatus() == null) {
            expense.setStatus("待审");
        }
        return financeExpenseRepository.save(expense);
    }

    /**
     * 审批：更新 approval_status（待审/已批/已驳/已付）。
     */
    @Transactional
    public FinanceExpense approve(Long expenseId, String status) {
        FinanceExpense existing = financeExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("报销单不存在: " + expenseId));
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("审批状态不能为空");
        }
        existing.setStatus(status);
        return financeExpenseRepository.save(existing);
    }
}
