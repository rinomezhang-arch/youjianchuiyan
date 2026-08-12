package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinanceReconciliationRepository extends JpaRepository<FinanceReconciliation, Long> {
    List<FinanceReconciliation> findByStoreIdOrderByReconIdDesc(Long storeId);
    List<FinanceReconciliation> findByAccountIdOrderByReconIdDesc(Long accountId);
}
