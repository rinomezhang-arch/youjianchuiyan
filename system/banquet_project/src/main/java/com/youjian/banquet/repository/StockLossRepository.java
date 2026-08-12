package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLossRepository extends JpaRepository<StockLoss, Long> {
    List<StockLoss> findByStoreId(Long storeId);
    List<StockLoss> findByStoreIdAndStatus(Long storeId, String status);
    Optional<StockLoss> findByLossNo(String lossNo);
}
