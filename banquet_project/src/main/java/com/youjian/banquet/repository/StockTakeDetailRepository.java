package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockTakeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTakeDetailRepository extends JpaRepository<StockTakeDetail, Long> {
    List<StockTakeDetail> findByTakeId(Long takeId);
    List<StockTakeDetail> findByStoreId(Long storeId);
    List<StockTakeDetail> findByTakeIdAndStoreId(Long takeId, Long storeId);
    void deleteByTakeId(Long takeId);
}
