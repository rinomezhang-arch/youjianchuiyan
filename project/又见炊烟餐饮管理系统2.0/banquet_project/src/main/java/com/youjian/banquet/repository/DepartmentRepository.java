/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Department
 *  com.youjian.banquet.repository.DepartmentRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository
extends JpaRepository<Department, Integer> {
    public List<Department> findByStoreId(Long var1);

    public List<Department> findByStoreIdOrderBySortOrderAsc(Long var1);
}

