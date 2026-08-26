package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLossRepository extends JpaRepository<StockLoss, String> {
    List<StockLoss> findByStoreId(Long storeId);
    List<StockLoss> findByStoreIdAndLossStatus(Long storeId, String lossStatus);
    long countByLossIdStartingWith(String prefix);
}
