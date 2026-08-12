/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.EmployeeLifecycle
 *  com.youjian.banquet.repository.EmployeeLifecycleRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.EmployeeLifecycle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLifecycleRepository extends JpaRepository<EmployeeLifecycle, Long> {
    @Query("SELECT e FROM EmployeeLifecycle e WHERE e.storeId = :storeId ORDER BY e.eventDate DESC")
    List<EmployeeLifecycle> findByStoreIdOrderByEventDateDesc(@Param("storeId") Long storeId);

    List<EmployeeLifecycle> findByStaffId(Integer staffId);
}

