/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.TableMaster
 *  com.youjian.banquet.repository.TableMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.TableMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TableMasterRepository
extends JpaRepository<TableMaster, Integer>,
JpaSpecificationExecutor<TableMaster> {
    public List<TableMaster> findByStoreId(Long var1);

    public List<TableMaster> findByStoreIdAndTableStatus(Long var1, String var2);

    public Optional<TableMaster> findByTableIdAndStoreId(Integer var1, Long var2);

    public List<TableMaster> findByStoreIdOrderBySortOrderAsc(Long var1);

    public List<TableMaster> findByStoreIdOrderBySortOrder(Long var1);

    public List<TableMaster> findByStoreIdAndTableArea(Long var1, String var2);

    public List<TableMaster> findByTableAreaAndStoreIdOrderBySortOrder(String var1, Long var2);

    public void deleteByTableIdAndStoreId(Integer var1, Long var2);

    /** 全店按桌台区域模糊匹配 */
    @Query("SELECT t FROM TableMaster t WHERE t.tableArea LIKE %:area%")
    public List<TableMaster> findAllByTableAreaLike(@Param("area") String area);

    /** 单店按桌台区域模糊匹配 */
    @Query("SELECT t FROM TableMaster t WHERE t.storeId = :storeId AND t.tableArea LIKE %:area%")
    public List<TableMaster> findByStoreIdAndTableAreaLike(@Param("storeId") Long storeId, @Param("area") String area);
}

