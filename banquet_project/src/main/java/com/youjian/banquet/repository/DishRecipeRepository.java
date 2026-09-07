/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishRecipe
 *  com.youjian.banquet.entity.DishRecipe$DishRecipeId
 *  com.youjian.banquet.repository.DishRecipeRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishRecipe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 配方明细仓储。
 * <p>
 * <b>active 语义（版本化之后固化）</b>：本接口除 {@code findByRevisionId*} 外，
 * <b>所有查询一律只返回当前生效版本</b>（{@code is_active = 1}，NULL 视为生效，
 * 兼容版本化之前的存量行）。历史版本只能按 revisionId 取。
 * <p>
 * 新增查询请沿用这个约定：<b>不要再加不带 is_active 过滤的派生查询</b>，
 * 否则调用方会在不知情的情况下把历史版本一起读进去，成本类计算会被叠加。
 */
@Repository
public interface DishRecipeRepository
extends JpaRepository<DishRecipe, Long>,
JpaSpecificationExecutor<DishRecipe> {
    /**
     * 某菜品当前生效的配方明细。
     * <p>
     * 版本化之前这张表里只有当前行，这个方法的语义本来就是"当前配方"；加上 is_active
     * 过滤是恢复原语义，不是改语义。不加的话，KitchenSupplyService 算成本卡时会把
     * 历史版本和当前版本的用量**加在一起**，成本随保存次数越滚越高。
     * 历史只能通过 {@link #findByRevisionIdOrderBySortOrderAsc} 按版本号取。
     */
    @Query("SELECT r FROM DishRecipe r WHERE r.dishId = :dishId AND r.storeId = :storeId "
            + "AND (r.isActive IS NULL OR r.isActive = 1)")
    public List<DishRecipe> findByDishIdAndStoreId(@Param("dishId") String var1, @Param("storeId") Long var2);

    /** 全店当前生效的配方明细，同样只认当前版本。 */
    @Query("SELECT r FROM DishRecipe r WHERE r.storeId = :storeId "
            + "AND (r.isActive IS NULL OR r.isActive = 1)")
    public List<DishRecipe> findByStoreId(@Param("storeId") Long var1);

    /** 某原料被哪些当前生效配方用到；历史版本用过但已被替换掉的不算。 */
    @Query("SELECT r FROM DishRecipe r WHERE r.ingredientId = :ingredientId AND r.storeId = :storeId "
            + "AND (r.isActive IS NULL OR r.isActive = 1)")
    public List<DishRecipe> findByIngredientIdAndStoreId(
            @Param("ingredientId") String var1, @Param("storeId") Long var2);

    /** 有配方的菜品编号；只统计当前生效版本，否则配方已被清空的菜品也会被算成"有配方"。 */
    @Query(value="SELECT DISTINCT r.dishId FROM DishRecipe r WHERE r.storeId = :storeId "
            + "AND (r.isActive IS NULL OR r.isActive = 1)")
    public List<String> findDistinctDishIdsByStoreId(@Param(value="storeId") Long var1);

    /**
     * 单行查询：不加 is_active 过滤时，同一原料在多个历史版本里各有一行，
     * 这个 Optional 会直接抛 IncorrectResultSizeDataAccessException，属于潜在崩溃点。
     */
    @Query("SELECT r FROM DishRecipe r WHERE r.dishId = :dishId AND r.storeId = :storeId "
            + "AND r.ingredientId = :ingredientId AND (r.isActive IS NULL OR r.isActive = 1)")
    public Optional<DishRecipe> findByDishIdAndStoreIdAndIngredientId(
            @Param("dishId") String var1, @Param("storeId") Long var2, @Param("ingredientId") String var3);

    public void deleteByDishIdAndStoreId(String var1, Long var2);

    /**
     * 当前生效的配方明细。配方保存改为版本化之后，dish_recipe 里同时存着历史行，
     * 对外查询必须带 is_active，否则会把历次版本一起返回。
     */
    public List<DishRecipe> findByDishIdAndStoreIdAndIsActiveOrderBySortOrderAsc(String var1, Long var2, Integer var3);

    /** 某个历史版本的明细，用于逐版本追溯。 */
    public List<DishRecipe> findByRevisionIdOrderBySortOrderAsc(Long var1);
}

