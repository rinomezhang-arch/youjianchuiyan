package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {
    List<FinanceTransaction> findByStoreIdOrderByTransIdDesc(Long storeId);
    List<FinanceTransaction> findByAccountIdOrderByTransIdDesc(Long accountId);
}
