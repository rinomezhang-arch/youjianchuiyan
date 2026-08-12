/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BanquetTemplate
 *  com.youjian.banquet.repository.BanquetTemplateRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BanquetTemplateRepository
extends JpaRepository<BanquetTemplate, Integer> {
    public List<BanquetTemplate> findByTemplateType(String var1);

    public List<BanquetTemplate> findByIsActive(Integer var1);
}

