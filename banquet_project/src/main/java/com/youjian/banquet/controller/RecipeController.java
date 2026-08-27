package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired private DishRecipeRepository recipeRepo;
    @Autowired private DishMasterRepository dishRepo;
    @Autowired private IngredientMasterRepository ingredientRepo;

    /**
     * 查询接口门店过滤：店长强制查询本店，总经理可查询任意门店。
     * <p>GET 请求由 {@code StoreDataScopeAspect} 已填充 UserContext 并设置 dataScopeAll 标记，
     * 本方法据此覆盖客户端传入的 storeId，防止店长越权查询其他门店配方。
     */
    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    @GetMapping("/recipes/{dishId}")
    public Result<List<DishRecipe>> getRecipe(@PathVariable String dishId,
                                               @RequestParam(defaultValue = "1") Long storeId) {
        try {
            storeId = resolveQueryStoreId(storeId);
            return Result.success(recipeRepo.findByDishIdAndStoreId(dishId, storeId));
        } catch (Exception e) {
            return Result.error(500, "获取配方失败: " + e.getMessage());
        }
    }

    @PostMapping("/recipes/{dishId}")
    @Transactional
    public Result<?> saveRecipe(@PathVariable String dishId,
                                 @RequestParam(defaultValue = "1") Long storeId,
                                 @RequestBody Object rawBody) {
        // 配方属于菜品核心数据，仅总经理可编辑
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return Result.error(403, "无权限：菜品配方仅总经理可编辑");
        }
        try {
            // 兼容 PowerShell 单元素数组被序列化为对象的情况
            // 注意：@RequestBody Object 时 Jackson 将数组元素反序列化为 LinkedHashMap，需逐个转换
            List<DishRecipe> items = new ArrayList<>();
            if (rawBody instanceof List) {
                for (Object elem : (List<?>) rawBody) {
                    if (elem instanceof DishRecipe) {
                        items.add((DishRecipe) elem);
                    } else if (elem instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> m = (java.util.Map<String, Object>) elem;
                        items.add(convertMapToRecipe(m));
                    }
                }
            } else if (rawBody instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> single = (java.util.Map<String, Object>) rawBody;
                items.add(convertMapToRecipe(single));
            } else if (rawBody instanceof DishRecipe) {
                items.add((DishRecipe) rawBody);
            } else {
                return Result.error(400, "请求体必须是数组或对象");
            }

            recipeRepo.deleteByDishIdAndStoreId(dishId, storeId);
            for (DishRecipe item : items) {
                item.setDishId(dishId);
                item.setStoreId(storeId);
                recipeRepo.save(item);
            }
            return Result.success("配方保存成功");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(500, "保存配方失败: " + e.getMessage());
        }
    }

    /** 将 Map 转换为 DishRecipe，用于兼容单对象请求体 */
    @SuppressWarnings("unchecked")
    private DishRecipe convertMapToRecipe(java.util.Map<String, Object> m) {
        DishRecipe item = new DishRecipe();
        Object ingredientIdObj = m.get("ingredientId");
        if (ingredientIdObj == null) ingredientIdObj = m.get("ingredient_id");
        if (ingredientIdObj != null) item.setIngredientId(ingredientIdObj.toString());

        Object quantityObj = m.get("quantity");
        if (quantityObj != null) {
            item.setQuantity(new java.math.BigDecimal(quantityObj.toString()));
        }
        Object unitObj = m.get("unit");
        if (unitObj == null) unitObj = m.get("usageUnit");
        if (unitObj == null) unitObj = m.get("usage_unit");
        if (unitObj != null) item.setUnit(unitObj.toString());
        return item;
    }

    @GetMapping("/recipes/dishes-with-recipe")
    public Result<List<DishMaster>> dishesWithRecipe(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            final Long effectiveStoreId = resolveQueryStoreId(storeId);
            List<DishRecipe> recipes = recipeRepo.findAll();
            Set<String> dishIds = recipes.stream()
                .filter(r -> r.getStoreId() != null && r.getStoreId().equals(effectiveStoreId))
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
        // 重算成本会写入菜品成本价（核心数据），仅总经理可触发
        UserContext.ensureDataScopeFromStoreId();
        if (!UserContext.isDataScopeAll()) {
            return Result.error(403, "无权限：成本重算仅总经理可触发");
        }
        try {
            // 重新计算所有菜品的成本
            //
            // ingredient_master.unit_price 是按采购单位(purchase_unit)计价的（比如"每只龙虾17.12元"），
            // 不能直接乘配方用量(克)——配方用量是按使用单位(usage_unit)算的。中间必须先用
            // conversion_rate(采购单位->使用单位换算，如1只=500克)和yield_rate(出成率，如初加工后
            // 只有60%可用)把它折算成每使用单位的净成本，即 unit_price / (conversion_rate * yield_rate/100)。
            // 这个公式是从 dish_recipe 表历史导入数据反推校验出来的（YC00001龙虾：17.12/(500*0.6)=0.05706667，
            // 与库里 dish_recipe.unit_price 实际值完全吻合），此前这里直接 quantity*unitPrice 完全没做这层换算，
            // 只要有人保存配方触发这个接口，就会把所有菜品成本价算错（放大几百倍）。
            List<DishMaster> allDishes = dishRepo.findAll();
            int updated = 0;
            for (DishMaster dish : allDishes) {
                List<DishRecipe> recipes = recipeRepo.findByDishIdAndStoreId(dish.getDishId(), dish.getStoreId());
                if (recipes.isEmpty()) continue;
                java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
                for (DishRecipe r : recipes) {
                    if (r.getQuantity() == null) continue;
                    IngredientMaster ing = ingredientRepo
                        .findByIngredientIdAndStoreId(r.getIngredientId(), r.getStoreId())
                        .orElse(null);
                    if (ing != null && ing.getUnitPrice() != null) {
                        java.math.BigDecimal conversionRate = ing.getConversionRate() != null
                            && ing.getConversionRate().compareTo(java.math.BigDecimal.ZERO) > 0
                            ? ing.getConversionRate() : java.math.BigDecimal.ONE;
                        java.math.BigDecimal yieldRate = r.getYieldRate() != null && r.getYieldRate().compareTo(java.math.BigDecimal.ZERO) > 0
                            ? r.getYieldRate()
                            : (ing.getYieldRate() != null && ing.getYieldRate().compareTo(java.math.BigDecimal.ZERO) > 0
                                ? ing.getYieldRate() : new java.math.BigDecimal(100));
                        java.math.BigDecimal divisor = conversionRate.multiply(yieldRate)
                            .divide(new java.math.BigDecimal(100), 8, java.math.RoundingMode.HALF_UP);
                        java.math.BigDecimal netUnitPrice = ing.getUnitPrice().divide(divisor, 8, java.math.RoundingMode.HALF_UP);
                        java.math.BigDecimal lineCost = r.getQuantity().multiply(netUnitPrice);
                        r.setUnitPrice(netUnitPrice);
                        r.setNetUnitPrice(netUnitPrice);
                        r.setTotalCost(lineCost);
                        recipeRepo.save(r);
                        totalCost = totalCost.add(lineCost);
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
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(500, "重算失败: " + e.getMessage());
        }
    }
}
