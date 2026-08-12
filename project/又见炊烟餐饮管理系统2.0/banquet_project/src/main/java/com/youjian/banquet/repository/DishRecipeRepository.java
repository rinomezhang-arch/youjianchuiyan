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

@Repository
public interface DishRecipeRepository
extends JpaRepository<DishRecipe, DishRecipe.DishRecipeId>,
JpaSpecificationExecutor<DishRecipe> {
    public List<DishRecipe> findByDishIdAndStoreId(String var1, Long var2);

    public List<DishRecipe> findByStoreId(Long var1);

    public List<DishRecipe> findByIngredientIdAndStoreId(String var1, Long var2);

    @Query(value="SELECT DISTINCT r.dishId FROM DishRecipe r WHERE r.storeId = :storeId")
    public List<String> findDistinctDishIdsByStoreId(@Param(value="storeId") Long var1);

    public Optional<DishRecipe> findByDishIdAndStoreIdAndIngredientId(String var1, Long var2, String var3);

    public void deleteByDishIdAndStoreId(String var1, Long var2);
}

