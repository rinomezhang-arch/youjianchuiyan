/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.KitchenLog
 *  com.youjian.banquet.repository.KitchenLogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.KitchenLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KitchenLogRepository
extends JpaRepository<KitchenLog, Long> {
    public List<KitchenLog> findByBookingId(String var1);

    public List<KitchenLog> findByStoreId(Long var1);

    public List<KitchenLog> findByBookingIdAndStoreId(String var1, Long var2);
}

