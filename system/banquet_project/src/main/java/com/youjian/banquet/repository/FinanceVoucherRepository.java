package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinanceVoucherRepository extends JpaRepository<FinanceVoucher, Long> {

    List<FinanceVoucher> findByStoreIdOrderByVoucherDateDescVoucherIdDesc(Long storeId);

    List<FinanceVoucher> findByStoreIdAndVoucherNoContainingOrderByVoucherDateDescVoucherIdDesc(Long storeId, String voucherNo);

    List<FinanceVoucher> findByStoreIdAndVoucherTypeOrderByVoucherDateDescVoucherIdDesc(Long storeId, String voucherType);

    List<FinanceVoucher> findByStoreIdAndVoucherDateBetweenOrderByVoucherDateDescVoucherIdDesc(
            Long storeId, LocalDate start, LocalDate end);
}
