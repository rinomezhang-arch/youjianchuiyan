package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByStoreIdAndStatus(Long storeId, String status);
    List<PurchaseRequest> findByStoreId(Long storeId);
}
