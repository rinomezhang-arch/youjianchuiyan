package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 应收款 Repository
 * 对应 finance_receivable 表
 */
@Repository
public interface FinanceReceivableRepository extends JpaRepository<FinanceReceivable, Long> {

    List<FinanceReceivable> findByStoreIdOrderByReceivableIdDesc(Long storeId);

    List<FinanceReceivable> findByStoreIdAndStatusOrderByReceivableIdDesc(Long storeId, String status);

    List<FinanceReceivable> findByCustomerIdOrderByReceivableIdDesc(Integer customerId);

    List<FinanceReceivable> findByBookingId(String bookingId);
}
