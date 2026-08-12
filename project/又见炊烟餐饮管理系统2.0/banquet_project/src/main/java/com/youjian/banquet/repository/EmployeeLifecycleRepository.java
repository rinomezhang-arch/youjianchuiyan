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
public interface EmployeeLifecycleRepository
extends JpaRepository<EmployeeLifecycle, Integer> {
    @Query(value="SELECT e FROM EmployeeLifecycle e ORDER BY e.eventDate DESC")
    public List<EmployeeLifecycle> findByStoreIdOrderByEventDateDesc(@Param(value="storeId") Long var1);

    public List<EmployeeLifecycle> findByStaffId(Integer var1);
}

