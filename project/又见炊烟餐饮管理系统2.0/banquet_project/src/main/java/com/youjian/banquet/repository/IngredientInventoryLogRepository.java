/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.IngredientInventoryLog
 *  com.youjian.banquet.repository.IngredientInventoryLogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.IngredientInventoryLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientInventoryLogRepository
extends JpaRepository<IngredientInventoryLog, Long>,
JpaSpecificationExecutor<IngredientInventoryLog> {
    public List<IngredientInventoryLog> findByStoreId(Long var1);

    public List<IngredientInventoryLog> findByStoreIdAndIngredientId(Long var1, String var2);

    public List<IngredientInventoryLog> findByStoreIdAndCreatedAtBetween(Long var1, LocalDateTime var2, LocalDateTime var3);

    public List<IngredientInventoryLog> findByStoreIdAndChangeType(Long var1, String var2);
}

