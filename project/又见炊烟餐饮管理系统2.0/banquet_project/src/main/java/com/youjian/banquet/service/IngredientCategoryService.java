package com.youjian.banquet.service;

import com.youjian.banquet.entity.IngredientCategory;
import com.youjian.banquet.repository.IngredientCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 材料种类服务
 * 来源：采购系统 cailiaozhonglei
 */
@Service
public class IngredientCategoryService {

    @Autowired
    private IngredientCategoryRepository categoryRepo;

    public List<IngredientCategory> listByStore(Long storeId) {
        return categoryRepo.findByStoreIdOrderBySortOrder(storeId);
    }

    public List<IngredientCategory> listTopLevel(Long storeId) {
        return categoryRepo.findByStoreIdAndParentId(storeId, 0);
    }

    public List<IngredientCategory> listChildren(Long storeId, Integer parentId) {
        return categoryRepo.findByStoreIdAndParentId(storeId, parentId);
    }

    /**
     * 构建树形结构
     */
    public List<IngredientCategory> buildTree(Long storeId) {
        List<IngredientCategory> all = listByStore(storeId);
        return all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .peek(root -> setChildren(root, all))
                .collect(Collectors.toList());
    }

    private void setChildren(IngredientCategory parent, List<IngredientCategory> all) {
        // 此处简化，不使用children字段，前端自行处理
    }

    @Transactional
    public IngredientCategory create(IngredientCategory category) {
        return categoryRepo.save(category);
    }

    @Transactional
    public IngredientCategory update(Integer id, IngredientCategory category) {
        category.setCategoryId(id);
        return categoryRepo.save(category);
    }

    @Transactional
    public void delete(Integer id) {
        categoryRepo.deleteById(id);
    }
}