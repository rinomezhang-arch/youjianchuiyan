package com.youjian.banquet.repository;

import com.youjian.banquet.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    List<GoodsReceipt> findByStoreIdAndStatus(Long storeId, String status);
    List<GoodsReceipt> findByStoreId(Long storeId);
}
