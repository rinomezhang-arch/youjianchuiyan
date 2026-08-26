package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockLossDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLossDetailRepository extends JpaRepository<StockLossDetail, Long> {
    List<StockLossDetail> findByLossId(String lossId);
    List<StockLossDetail> findByStoreId(Long storeId);
    List<StockLossDetail> findByLossIdAndStoreId(String lossId, Long storeId);
    void deleteByLossId(String lossId);
}
