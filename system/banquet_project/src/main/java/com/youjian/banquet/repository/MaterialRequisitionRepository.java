package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MaterialRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRequisitionRepository extends JpaRepository<MaterialRequisition, Long> {
    List<MaterialRequisition> findByStoreIdAndStatus(Long storeId, String status);
    List<MaterialRequisition> findByStoreId(Long storeId);
}
