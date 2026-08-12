package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinancePayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancePayableRepository extends JpaRepository<FinancePayable, Long> {

    List<FinancePayable> findByStoreIdOrderByPayableIdDesc(Long storeId);

    List<FinancePayable> findByStoreIdAndSupplierIdOrderByPayableIdDesc(Long storeId, Long supplierId);

    List<FinancePayable> findByStoreIdAndStatusOrderByPayableIdDesc(Long storeId, String status);
}
