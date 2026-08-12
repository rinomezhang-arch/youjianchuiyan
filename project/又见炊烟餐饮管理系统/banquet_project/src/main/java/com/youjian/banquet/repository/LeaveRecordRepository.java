/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.LeaveRecord
 *  com.youjian.banquet.repository.LeaveRecordRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.LeaveRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRecordRepository
extends JpaRepository<LeaveRecord, Integer> {
    public List<LeaveRecord> findByStoreId(Long var1);

    public List<LeaveRecord> findByStoreIdOrderByCreatedAtDesc(Long var1);

    public List<LeaveRecord> findByStaffId(Integer var1);
}

