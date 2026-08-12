/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.PackageDishDetail
 *  com.youjian.banquet.repository.PackageDishDetailRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PackageDishDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageDishDetailRepository
extends JpaRepository<PackageDishDetail, Long>,
JpaSpecificationExecutor<PackageDishDetail> {
    public List<PackageDishDetail> findByPackageIdAndStoreId(String var1, Long var2);

    public List<PackageDishDetail> findByPackageIdAndStoreIdOrderByDishOrderAsc(String var1, Long var2);

    public void deleteByPackageIdAndStoreId(String var1, Long var2);

    public void deleteByPackageIdAndStoreIdAndDishId(String var1, Long var2, String var3);
}

