/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.MenuCategory
 *  com.youjian.banquet.repository.MenuCategoryRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MenuCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuCategoryRepository
extends JpaRepository<MenuCategory, Integer> {
    public List<MenuCategory> findAll();

    public List<MenuCategory> findByIsActive(Integer var1);
}

