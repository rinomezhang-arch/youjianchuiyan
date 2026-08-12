package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PurchaseStoreup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseStoreupRepository extends JpaRepository<PurchaseStoreup, Long>, JpaSpecificationExecutor<PurchaseStoreup> {

    List<PurchaseStoreup> findByUserId(Long userId);

    List<PurchaseStoreup> findByRefId(Long refId);
}