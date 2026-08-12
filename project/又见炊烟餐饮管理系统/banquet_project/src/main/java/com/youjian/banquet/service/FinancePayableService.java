package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.repository.FinancePayableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 供应商应付服务。对应 GET/POST /api/finance/payables。
 * POST 用于结算（更新 paid_amount / unpaid_amount / last_settle_date / status）。
 */
@Service
public class FinancePayableService {

    @Autowired
    private FinancePayableRepository financePayableRepository;

    public List<FinancePayable> list(Long storeId, Long supplierId, String status) {
        boolean hasSupplier = supplierId != null;
        boolean hasStatus = status != null && !status.isEmpty();
        if (hasSupplier) {
            List<FinancePayable> list = financePayableRepository
                    .findByStoreIdAndSupplierIdOrderByPayableIdDesc(storeId, supplierId);
            if (hasStatus) {
                list.removeIf(p -> !status.equals(p.getStatus()));
            }
            return list;
        }
        if (hasStatus) {
            return financePayableRepository.findByStoreIdAndStatusOrderByPayableIdDesc(storeId, status);
        }
        return financePayableRepository.findByStoreIdOrderByPayableIdDesc(storeId);
    }

    /**
     * 结算：累加 paid_amount，重算 unpaid_amount，更新最近结算日与状态。
     *
     * @param payableId   应付单ID
     * @param settleAmount 本次结算金额
     */
    @Transactional
    public FinancePayable settle(Long payableId, BigDecimal settleAmount) {
        FinancePayable existing = financePayableRepository.findById(payableId)
                .orElseThrow(() -> new IllegalArgumentException("应付单不存在: " + payableId));
        BigDecimal total = existing.getTotalAmount() == null ? BigDecimal.ZERO : existing.getTotalAmount();
        BigDecimal paid = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
        BigDecimal newPaid = paid.add(settleAmount == null ? BigDecimal.ZERO : settleAmount);
        if (newPaid.compareTo(total) > 0) {
            newPaid = total;
        }
        BigDecimal unpaid = total.subtract(newPaid);
        existing.setPaidAmount(newPaid);
        existing.setUnpaidAmount(unpaid);
        existing.setLastSettleDate(LocalDate.now());
        if (unpaid.signum() <= 0) {
            existing.setStatus("paid");
        } else if (newPaid.signum() > 0) {
            existing.setStatus("partial");
        }
        return financePayableRepository.save(existing);
    }

    @Transactional
    public FinancePayable create(FinancePayable payable) {
        if (payable.getPayableId() == null) {
            payable.setPayableId(System.currentTimeMillis());
        }
        if (payable.getTotalAmount() != null) {
            if (payable.getPaidAmount() == null) {
                payable.setPaidAmount(BigDecimal.ZERO);
            }
            payable.setUnpaidAmount(payable.getTotalAmount().subtract(payable.getPaidAmount()));
        }
        return financePayableRepository.save(payable);
    }
}
