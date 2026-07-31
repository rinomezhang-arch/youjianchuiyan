package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer> findByStoreId(Long storeId);

    List<StockTransfer> findByTransferNo(String transferNo);

    List<StockTransfer> findByStatus(String status);

    List<StockTransfer> findByStoreIdAndStatus(Long storeId, String status);
}
