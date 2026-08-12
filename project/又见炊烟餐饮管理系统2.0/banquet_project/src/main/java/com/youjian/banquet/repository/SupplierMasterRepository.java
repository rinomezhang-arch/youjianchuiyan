/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.SupplierMaster
 *  com.youjian.banquet.repository.SupplierMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SupplierMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierMasterRepository
extends JpaRepository<SupplierMaster, Integer>,
JpaSpecificationExecutor<SupplierMaster> {
    public List<SupplierMaster> findByStoreId(Long var1);

    public List<SupplierMaster> findByStoreIdAndStatus(Long var1, String var2);

    public Optional<SupplierMaster> findBySupplierIdAndStoreId(Integer var1, Long var2);

    public List<SupplierMaster> findByStoreIdAndCategory(Long var1, String var2);

    @Query(value="SELECT s FROM SupplierMaster s WHERE s.storeId = :storeId AND (s.supplierName LIKE %:keyword% OR s.contactPerson LIKE %:keyword% OR s.phone LIKE %:keyword%)")
    public List<SupplierMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    public void deleteBySupplierIdAndStoreId(Integer var1, Long var2);
}

