package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinancePayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancePayableRepository extends JpaRepository<FinancePayable, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from FinancePayable p where p.payableId = :id")
    java.util.Optional<FinancePayable> findForSettlement(@org.springframework.data.repository.query.Param("id") Long id);

    List<FinancePayable> findByStoreIdOrderByPayableIdDesc(Long storeId);

    List<FinancePayable> findByStoreIdAndSupplierIdOrderByPayableIdDesc(Long storeId, Long supplierId);

    List<FinancePayable> findByStoreIdAndStatusOrderByPayableIdDesc(Long storeId, String status);
}
