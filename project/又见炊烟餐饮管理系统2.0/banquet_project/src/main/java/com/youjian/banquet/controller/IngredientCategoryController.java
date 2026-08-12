package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.IngredientCategory;
import com.youjian.banquet.service.IngredientCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 材料种类管理控制器
 * 来源：采购系统 cailiaozhonglei
 */
@RestController
@RequestMapping("/api/purchase/category")
@CrossOrigin(origins = "*")
public class IngredientCategoryController {

    @Autowired
    private IngredientCategoryService categoryService;

    @GetMapping
    public Result<List<IngredientCategory>> list(@RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        return Result.success(categoryService.listByStore(storeId));
    }

    @GetMapping("/top-level")
    public Result<List<IngredientCategory>> listTopLevel(@RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        return Result.success(categoryService.listTopLevel(storeId));
    }

    @GetMapping("/children/{parentId}")
    public Result<List<IngredientCategory>> listChildren(@RequestAttribute(value = "store_id", required = false) Long storeId,
                                                          @PathVariable Integer parentId) {
        if (storeId == null) storeId = 1L;
        return Result.success(categoryService.listChildren(storeId, parentId));
    }

    @GetMapping("/tree")
    public Result<List<IngredientCategory>> tree(@RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        return Result.success(categoryService.buildTree(storeId));
    }

    @PostMapping
    public Result<IngredientCategory> create(@RequestBody IngredientCategory category,
                                              @RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        category.setStoreId(storeId);
        return Result.success(categoryService.create(category));
    }

    @PutMapping("/{id}")
    public Result<IngredientCategory> update(@PathVariable Integer id, @RequestBody IngredientCategory category) {
        return Result.success(categoryService.update(id, category));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Result.success("删除成功");
    }
}