package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceVoucherDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinanceVoucherDetailRepository extends JpaRepository<FinanceVoucherDetail, Long> {
    List<FinanceVoucherDetail> findByVoucherIdOrderByDetailIdAsc(Long voucherId);
    List<FinanceVoucherDetail> findByStoreIdOrderByDetailIdDesc(Long storeId);
}
