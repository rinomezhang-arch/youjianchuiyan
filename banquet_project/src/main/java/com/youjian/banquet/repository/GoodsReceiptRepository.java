package com.youjian.banquet.repository;

import com.youjian.banquet.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM GoodsReceipt r WHERE r.receiptId=:id")
    Optional<GoodsReceipt> findForAcceptance(@Param("id") Long id);
    List<GoodsReceipt> findByStoreIdAndStatus(Long storeId, String status);
    List<GoodsReceipt> findByStoreId(Long storeId);
}
