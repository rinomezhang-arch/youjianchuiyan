/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishMaster
 *  com.youjian.banquet.entity.DishMaster$DishMasterId
 *  com.youjian.banquet.repository.DishMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DishMasterRepository
extends JpaRepository<DishMaster, DishMaster.DishMasterId>,
JpaSpecificationExecutor<DishMaster> {
    public List<DishMaster> findByStoreId(Long var1);

    public List<DishMaster> findByStoreIdAndIsActive(Long var1, Integer var2);

    public List<DishMaster> findByStoreIdAndDishCategory(Long var1, String var2);

    public Optional<DishMaster> findByDishIdAndStoreId(String var1, Long var2);

    @Query(value="SELECT d FROM DishMaster d WHERE d.storeId = :storeId AND (d.dishName LIKE %:keyword% OR d.dishCategory LIKE %:keyword% OR d.englishName LIKE %:keyword%)")
    public List<DishMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    @Query(value="SELECT DISTINCT d.dishCategory FROM DishMaster d WHERE d.storeId = :storeId ORDER BY d.dishCategory")
    public List<String> findDistinctCategoriesByStoreId(@Param(value="storeId") Long var1);

    public List<DishMaster> findByStoreIdOrderBySortOrderAsc(Long var1);

    public void deleteByDishIdAndStoreId(String var1, Long var2);

    /** 全店按 (dishId, storeId) in 列表批量查菜品 */
    @Query("SELECT d FROM DishMaster d WHERE d.dishId IN :dishIds")
    public List<DishMaster> findByDishIdIn(@Param("dishIds") java.util.Collection<String> dishIds);
}

