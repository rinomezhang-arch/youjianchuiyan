/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.PackageMaster
 *  com.youjian.banquet.entity.PackageMaster$PackageMasterId
 *  com.youjian.banquet.repository.PackageMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PackageMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageMasterRepository
extends JpaRepository<PackageMaster, PackageMaster.PackageMasterId>,
JpaSpecificationExecutor<PackageMaster> {
    List<PackageMaster> findByStoreId(Long storeId);

    List<PackageMaster> findByStoreIdAndStatus(Long storeId, String status);

    List<PackageMaster> findByStoreIdAndCategory(Long storeId, String category);

    Optional<PackageMaster> findByPackageIdAndStoreId(String packageId, Long storeId);

    List<PackageMaster> findByStoreIdOrderBySortOrderAsc(Long storeId);

    @Query("SELECT p FROM PackageMaster p WHERE p.storeId = :storeId AND (p.packageName LIKE %:keyword% OR p.category LIKE %:keyword%)")
    List<PackageMaster> searchByKeyword(@Param("storeId") Long storeId, @Param("keyword") String keyword);

    void deleteByPackageIdAndStoreId(String packageId, Long storeId);

    @Query(value="SELECT COUNT(p) FROM PackageMaster p WHERE p.packageId LIKE CONCAT(:prefix, '%')")
    public long countByPackageIdPrefix(@Param(value="prefix") String prefix);
}

