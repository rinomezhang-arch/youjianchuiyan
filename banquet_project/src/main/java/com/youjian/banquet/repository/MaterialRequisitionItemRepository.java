package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MaterialRequisitionItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MaterialRequisitionItemRepository extends JpaRepository<MaterialRequisitionItem,Long> {
    List<MaterialRequisitionItem> findByRequisitionIdAndStoreId(Long requisitionId,Long storeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM MaterialRequisitionItem i WHERE i.itemId=:id")
    Optional<MaterialRequisitionItem> findForProcessing(@Param("id") Long id);
}
