/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.IngredientPurchase
 *  com.youjian.banquet.repository.IngredientPurchaseRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.IngredientPurchase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientPurchaseRepository
extends JpaRepository<IngredientPurchase, Long>,
JpaSpecificationExecutor<IngredientPurchase> {
    public List<IngredientPurchase> findByStoreId(Long var1);

    public List<IngredientPurchase> findByStoreIdAndStatus(Long var1, String var2);

    public List<IngredientPurchase> findByStatus(String var1);

    public List<IngredientPurchase> findByStoreIdAndIngredientId(Long var1, String var2);

    public List<IngredientPurchase> findByStoreIdAndPurchaseDateBetween(Long var1, LocalDate var2, LocalDate var3);

    public List<IngredientPurchase> findByStoreIdAndSupplierId(Long var1, Integer var2);

    /** 全店按状态返回最近 N 条采购单 */
    @Query("SELECT p FROM IngredientPurchase p WHERE p.status = :status ORDER BY p.createdAt DESC")
    public List<IngredientPurchase> findRecentByStatus(@Param("status") String status, org.springframework.data.domain.Pageable pageable);

    /** 单店按状态返回最近 N 条采购单 */
    @Query("SELECT p FROM IngredientPurchase p WHERE p.storeId = :storeId AND p.status = :status ORDER BY p.createdAt DESC")
    public List<IngredientPurchase> findRecentByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") String status, org.springframework.data.domain.Pageable pageable);
}

