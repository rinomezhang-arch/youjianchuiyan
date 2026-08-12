/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.StaffMaster
 *  com.youjian.banquet.repository.StaffMasterRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.StaffMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffMasterRepository
extends JpaRepository<StaffMaster, Integer>,
JpaSpecificationExecutor<StaffMaster> {
    public List<StaffMaster> findByStoreId(Long var1);

    public List<StaffMaster> findByStoreIdAndEmploymentStatus(Long var1, String var2);

    public List<StaffMaster> findByEmploymentStatus(String var1);

    public Optional<StaffMaster> findByStaffIdAndStoreId(Integer var1, Long var2);

    public List<StaffMaster> findByStoreIdAndRole(Long var1, String var2);

    @Query(value="SELECT s FROM StaffMaster s WHERE s.storeId = :storeId AND (s.staffName LIKE %:keyword% OR s.staffPhone LIKE %:keyword% OR s.staffAccount LIKE %:keyword%)")
    public List<StaffMaster> searchByKeyword(@Param(value="storeId") Long var1, @Param(value="keyword") String var2);

    public void deleteByStaffIdAndStoreId(Integer var1, Long var2);

    public boolean existsByStaffPhoneAndStoreId(String var1, Long var2);

    public Optional<StaffMaster> findByStaffAccountAndStoreId(String var1, Long var2);
}

