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
        if (payable == null) throw new IllegalArgumentException("请填写应付单");
        if (payable.getStoreId() == null || payable.getStoreId() <= 0)
            throw new IllegalArgumentException("请选择有效门店");
        try { UserContext.assertStoreAccess(payable.getStoreId()); }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }
        if (payable.getPayableId() != null || payable.getSourceReceiptId() != null || payable.getSourceReceiptNo() != null)
            throw new IllegalArgumentException("手工新增不能覆盖原单或冒用收货来源");
        BigDecimal total=payable.getTotalAmount();
        if (total == null || total.signum() <= 0 || total.stripTrailingZeros().scale() > 2 || total.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("应付金额必须为正数且最多两位小数");
        if (payable.getPaidAmount() != null && payable.getPaidAmount().signum() != 0)
            throw new IllegalArgumentException("新增应付不能直接标记已付款，请使用结算记账");
        if (payable.getStatus() != null && !"unpaid".equals(payable.getStatus()))
            throw new IllegalArgumentException("新增应付必须为未付款状态");
        if (payable.getSupplierId() == null && (payable.getSupplierName() == null || payable.getSupplierName().isBlank()))
            throw new IllegalArgumentException("请填写供应商");
        if (payable.getPayableNo() == null || payable.getPayableNo().isBlank())
            payable.setPayableNo("PY"+java.util.UUID.randomUUID().toString().replace("-",""));
        if (payable.getPayableNo().length() > 50) throw new IllegalArgumentException("应付单号不能超过50字");
        if (payable.getPayableDate() == null) payable.setPayableDate(LocalDate.now());
        if (payable.getDueDate() != null && payable.getDueDate().isBefore(payable.getPayableDate()))
            throw new IllegalArgumentException("到期日不能早于应付日期");
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setPendingAmount(total);
        payable.setStatus("unpaid");
        payable.setOperatorName(UserContext.getUsername());
        return financePayableRepository.save(payable);
    }
}
