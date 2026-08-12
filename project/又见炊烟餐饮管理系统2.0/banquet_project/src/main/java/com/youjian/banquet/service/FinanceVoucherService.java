package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinanceVoucher;
import com.youjian.banquet.repository.FinanceVoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 凭证总账服务。对应 GET/POST /api/finance/vouchers。
 */
@Service
public class FinanceVoucherService {

    @Autowired
    private FinanceVoucherRepository financeVoucherRepository;

    public List<FinanceVoucher> list(Long storeId, String voucherNo, String voucherType,
                                     LocalDate startDate, LocalDate endDate) {
        boolean hasNo = voucherNo != null && !voucherNo.isEmpty();
        boolean hasType = voucherType != null && !voucherType.isEmpty();
        boolean hasRange = startDate != null && endDate != null;

        if (hasRange) {
            List<FinanceVoucher> list = financeVoucherRepository
                    .findByStoreIdAndVoucherDateBetweenOrderByVoucherDateDescVoucherIdDesc(storeId, startDate, endDate);
            if (hasNo) list.removeIf(v -> v.getVoucherNo() == null || !v.getVoucherNo().contains(voucherNo));
            if (hasType) list.removeIf(v -> !voucherType.equals(v.getVoucherType()));
            return list;
        }
        if (hasNo) {
            return financeVoucherRepository
                    .findByStoreIdAndVoucherNoContainingOrderByVoucherDateDescVoucherIdDesc(storeId, voucherNo);
        }
        if (hasType) {
            return financeVoucherRepository
                    .findByStoreIdAndVoucherTypeOrderByVoucherDateDescVoucherIdDesc(storeId, voucherType);
        }
        return financeVoucherRepository.findByStoreIdOrderByVoucherDateDescVoucherIdDesc(storeId);
    }

    @Transactional
    public FinanceVoucher create(FinanceVoucher voucher) {
        if (voucher.getVoucherId() == null) {
            voucher.setVoucherId(System.currentTimeMillis());
        }
        if (voucher.getVoucherNo() == null || voucher.getVoucherNo().isEmpty()) {
            voucher.setVoucherNo("VCH" + voucher.getVoucherId());
        }
        if (voucher.getVoucherDate() == null) {
            voucher.setVoucherDate(LocalDate.now());
        }
        if (voucher.getDebitAmount() == null) {
            voucher.setDebitAmount(BigDecimal.ZERO);
        }
        if (voucher.getCreditAmount() == null) {
            voucher.setCreditAmount(BigDecimal.ZERO);
        }
        return financeVoucherRepository.save(voucher);
    }
}
