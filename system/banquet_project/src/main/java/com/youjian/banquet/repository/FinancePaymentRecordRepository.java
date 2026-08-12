package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinancePaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinancePaymentRecordRepository extends JpaRepository<FinancePaymentRecord, Long> {
    List<FinancePaymentRecord> findByStoreIdOrderByPaymentIdDesc(Long storeId);
    List<FinancePaymentRecord> findByReceivableIdOrderByPaymentIdDesc(Long receivableId);
    List<FinancePaymentRecord> findByBookingId(String bookingId);
}
