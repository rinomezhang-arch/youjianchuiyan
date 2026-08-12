/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.TemplateDishRel
 *  com.youjian.banquet.repository.TemplateDishRelRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.TemplateDishRel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateDishRelRepository
extends JpaRepository<TemplateDishRel, Integer> {
    public List<TemplateDishRel> findByTemplateId(Integer var1);

    public List<TemplateDishRel> findByTemplateIdAndStoreId(Integer var1, Long var2);

    public List<TemplateDishRel> findByTemplateIdAndStoreIdOrderBySortOrderAsc(Integer var1, Long var2);
}

