package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinanceSettlementRepository extends JpaRepository<FinanceSettlement, Long> {
    List<FinanceSettlement> findByStoreIdOrderBySettlementIdDesc(Long storeId);
}
