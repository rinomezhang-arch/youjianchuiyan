package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockTake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTakeRepository extends JpaRepository<StockTake, Long> {
    List<StockTake> findByStoreId(Long storeId);
    List<StockTake> findByStoreIdAndStatus(Long storeId, String status);
    Optional<StockTake> findByTakeNo(String takeNo);
}
