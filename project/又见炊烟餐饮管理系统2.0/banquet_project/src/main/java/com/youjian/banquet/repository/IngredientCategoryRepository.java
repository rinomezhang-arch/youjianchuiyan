package com.youjian.banquet.repository;

import com.youjian.banquet.entity.IngredientCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, Integer> {

    List<IngredientCategory> findByStoreId(Long storeId);

    List<IngredientCategory> findByStoreIdAndParentId(Long storeId, Integer parentId);

    List<IngredientCategory> findByStoreIdOrderBySortOrder(Long storeId);
}