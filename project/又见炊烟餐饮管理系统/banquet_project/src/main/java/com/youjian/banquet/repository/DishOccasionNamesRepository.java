/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishOccasionNames
 *  com.youjian.banquet.entity.DishOccasionNames$DishOccasionNamesId
 *  com.youjian.banquet.repository.DishOccasionNamesRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishOccasionNames;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DishOccasionNamesRepository
extends JpaRepository<DishOccasionNames, DishOccasionNames.DishOccasionNamesId>,
JpaSpecificationExecutor<DishOccasionNames> {
    public List<DishOccasionNames> findByStoreId(String var1);

    public List<DishOccasionNames> findByDishIdAndStoreId(String var1, String var2);

    public List<DishOccasionNames> findByOccasionNameAndStoreId(String var1, String var2);

    public void deleteByDishIdAndStoreId(String var1, String var2);
}

