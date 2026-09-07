package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.repository.FinancePayableRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 供应商应付服务。对应 GET/POST /api/finance/payables。
 * POST 用于结算记账，更新 paid_amount / pending_amount / status。
 */
@Service
public class FinancePayableService {

    public static class PayableAccessDeniedException extends IllegalArgumentException {
        public PayableAccessDeniedException() { super("无权限操作该门店应付单"); }
    }

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
     * 结算记账：锁定单据后同步已付、待付及状态，不执行银行支付。
     *
     * @param payableId   应付单ID
     * @param settleAmount 本次结算金额
     */
    @Transactional
    public FinancePayable settle(Long payableId, BigDecimal settleAmount) {
        if (settleAmount == null || settleAmount.signum() <= 0 || settleAmount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("结算金额必须为正数，最多保留两位小数");
        }
        FinancePayable existing = financePayableRepository.findForSettlement(payableId)
                .orElseThrow(() -> new IllegalArgumentException("应付单不存在: " + payableId));
        try { UserContext.assertStoreAccess(existing.getStoreId()); }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }
        BigDecimal total = existing.getTotalAmount();
        BigDecimal paid = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
        if (total == null || total.signum() < 0 || paid.signum() < 0 || paid.compareTo(total) > 0) {
            throw new IllegalArgumentException("应付单金额不一致，请先核对原单");
        }
        BigDecimal newPaid = paid.add(settleAmount);
        if (newPaid.compareTo(total) > 0) {
            throw new IllegalArgumentException("结算金额不能超过待付金额");
        }
        BigDecimal unpaid = total.subtract(newPaid);
        existing.setPaidAmount(newPaid);
        existing.setPendingAmount(unpaid);
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
            // 已移除: 字段对齐数据库
        }
        return financePayableRepository.save(payable);
    }
}
