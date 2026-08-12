package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PurchaseCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseCartRepository extends JpaRepository<PurchaseCart, Long>, JpaSpecificationExecutor<PurchaseCart> {

    List<PurchaseCart> findByUserId(Long userId);

    List<PurchaseCart> findByGoodId(Long goodId);
}