package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StockTransferDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferDetailRepository extends JpaRepository<StockTransferDetail, Long> {

    List<StockTransferDetail> findByTransferId(Long transferId);

    void deleteByTransferId(Long transferId);
}
