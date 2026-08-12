/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BanquetTable
 *  com.youjian.banquet.repository.BanquetTableRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetTable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BanquetTableRepository
extends JpaRepository<BanquetTable, Integer> {
    // 安全修复 N3：storeId 类型从 String 改为 Long，与数据库 bigint 对齐
    public List<BanquetTable> findByStoreId(Long var1);

    public List<BanquetTable> findByStoreIdOrderBySortOrder(Long var1);

    public List<BanquetTable> findByStoreIdAndTableStatus(Long var1, String var2);

    public List<BanquetTable> findByStoreIdAndTableArea(Long var1, String var2);

    public List<BanquetTable> findByTableAreaAndStoreIdOrderBySortOrder(String var1, Long var2);
}

