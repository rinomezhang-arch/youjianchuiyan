package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceCostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinanceCostRecordRepository extends JpaRepository<FinanceCostRecord, Long> {
    List<FinanceCostRecord> findByStoreIdOrderByCostIdDesc(Long storeId);
}
