/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.IngredientMaster
 *  com.youjian.banquet.entity.IngredientMaster$IngredientMasterId
 *  com.youjian.banquet.repository.IngredientMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.IngredientMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientMasterRepository
extends JpaRepository<IngredientMaster, IngredientMaster.IngredientMasterId>,
JpaSpecificationExecutor<IngredientMaster> {
    public List<IngredientMaster> findByStoreId(Long var1);

    public List<IngredientMaster> findByStoreIdAndStatus(Long var1, String var2);

    public Optional<IngredientMaster> findByIngredientIdAndStoreId(String var1, Long var2);

    @Query(value="SELECT i FROM IngredientMaster i WHERE i.storeId = :storeId AND i.currentStock <= i.minStock")
    public List<IngredientMaster> findLowStockIngredients(@Param(value="storeId") Long var1);

    @Query(value="SELECT i FROM IngredientMaster i WHERE i.currentStock <= i.minStock")
    public List<IngredientMaster> findAllLowStockIngredients();

    public List<IngredientMaster> findByStoreIdAndSupplierId(Long var1, Long var2);

    public List<IngredientMaster> findByStoreIdAndCategory(Long var1, String var2);

    @Query(value="SELECT i FROM IngredientMaster i WHERE i.storeId = :storeId AND (i.ingredientName LIKE %:keyword% OR i.ingredientId LIKE %:keyword%)")
    public List<IngredientMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    public void deleteByIngredientIdAndStoreId(String var1, Long var2);
}

