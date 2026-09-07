package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MaterialRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MaterialRequisitionRepository extends JpaRepository<MaterialRequisition, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM MaterialRequisition r WHERE r.requisitionId=:id")
    Optional<MaterialRequisition> findForIssue(@Param("id") Long id);
    List<MaterialRequisition> findByStoreIdAndStatus(Long storeId, String status);
    List<MaterialRequisition> findByStoreId(Long storeId);
}
