/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Overtime
 *  com.youjian.banquet.repository.OvertimeRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Overtime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OvertimeRepository
extends JpaRepository<Overtime, Integer> {
    public List<Overtime> findByStoreId(Long var1);

    public List<Overtime> findByStoreIdOrderByCreatedAtDesc(Long var1);

    public List<Overtime> findByStaffId(Integer var1);
}

