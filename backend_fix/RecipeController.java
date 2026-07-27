package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired private DishRecipeRepository recipeRepo;
    @Autowired private DishMasterRepository dishRepo;

    @GetMapping("/recipes/{dishId}")
    public Result<List<DishRecipe>> getRecipe(@PathVariable String dishId,
                                               @RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(recipeRepo.findByDishIdAndStoreId(dishId, storeId));
        } catch (Exception e) {
            return Result.error(500, "获取配方失败: " + e.getMessage());
        }
    }

    @PostMapping("/recipes/{dishId}")
    @Transactional
    public Result<?> saveRecipe(@PathVariable String dishId,
                                 @RequestParam(defaultValue = "1") Long storeId,
                                 @RequestBody List<DishRecipe> items) {
        try {
            recipeRepo.deleteByDishIdAndStoreId(dishId, storeId);
            for (DishRecipe item : items) {
                item.setDishId(dishId);
                item.setStoreId(storeId);
                recipeRepo.save(item);
            }
            return Result.success("配方保存成功");
        } catch (Exception e) {
            return Result.error(500, "保存配方失败: " + e.getMessage());
        }
    }

    @GetMapping("/recipes/dishes-with-recipe")
    public Result<List<DishMaster>> dishesWithRecipe(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            List<DishRecipe> recipes = recipeRepo.findAll();
            Set<String> dishIds = recipes.stream()
                .filter(r -> r.getStoreId() != null && r.getStoreId().equals(storeId))
                .map(DishRecipe::getDishId)
                .collect(Collectors.toSet());
            List<DishMaster> allDishes = dishRepo.findAll();
            List<DishMaster> result = allDishes.stream()
                .filter(d -> dishIds.contains(d.getDishId()))
                .collect(Collectors.toList());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/recipes/recalc-all")
    @Transactional
    public Result<?> recalcAll() {
        try {
            // 重新计算所有菜品的成本
            List<DishMaster> allDishes = dishRepo.findAll();
            int updated = 0;
            for (DishMaster dish : allDishes) {
                List<DishRecipe> recipes = recipeRepo.findByDishIdAndStoreId(dish.getDishId(), dish.getStoreId());
                if (recipes.isEmpty()) continue;
                java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
                for (DishRecipe r : recipes) {
                    if (r.getTotalCost() != null) {
                        totalCost = totalCost.add(r.getTotalCost());
                    }
                }
                dish.setCostPrice(totalCost);
                if (dish.getSalePrice() != null && dish.getSalePrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal costRate = totalCost.divide(dish.getSalePrice(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new java.math.BigDecimal(100));
                    dish.setCostRate(costRate);
                }
                dishRepo.save(dish);
                updated++;
            }
            return Result.success("成本重算完成，更新了 " + updated + " 个菜品");
        } catch (Exception e) {
            return Result.error(500, "重算失败: " + e.getMessage());
        }
    }
}
