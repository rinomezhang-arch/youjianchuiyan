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
    @Autowired private com.youjian.banquet.service.RecipeRevisionService recipeRevisionService;
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
            // 配方保存改为版本化后，dish_recipe 里同时留着历史行，这里必须只取当前生效版本，
            // 否则页面会把历次改动的明细一起显示出来。
            return Result.success(recipeRevisionService.activeRecipe(dishId, storeId));
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

            // 版本化保存：校验全部前置、旧行只置为不生效、成本在同一事务里刷新。
            // 原来的"先 deleteByDishIdAndStoreId 再逐条 insert"已废弃——那样每保存一次
            // 就把历史物理抹掉，而且删除发生在校验之前，一条非法原料能把原配方一起毁掉。
            com.youjian.banquet.entity.RecipeRevision revision =
                    recipeRevisionService.saveNewVersion(dishId, storeId, items,
                            UserContext.getUsername(), null);
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("revisionId", revision.getRevisionId());
            data.put("versionNo", revision.getVersionNo());
            data.put("itemCount", revision.getItemCount());
            data.put("totalCost", revision.getTotalCost());
            data.put("message", "配方保存成功，已存为第 " + revision.getVersionNo() + " 版");
            return Result.success(data);
        } catch (com.youjian.banquet.service.RecipeRevisionService.RecipeValidationException e) {
            // 输入不合法：整个事务回滚，一行都没写进去。回 400 而不是 500，
            // 让调用方知道该改请求，而不是以为服务挂了去重试。
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(500, "保存配方失败: " + e.getMessage());
        }
    }

    /**
     * 配方版本列表，最新在前。用于回答"这道菜的配方被谁在什么时候改过、改之前合计成本多少"。
     */
    @GetMapping("/recipes/{dishId}/revisions")
    public Result<?> revisions(@PathVariable String dishId,
                               @RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(recipeRevisionService.revisions(dishId, resolveQueryStoreId(storeId)));
        } catch (Exception e) {
            return Result.error(500, "获取配方版本失败: " + e.getMessage());
        }
    }

    /**
     * 某个历史版本的明细。历史行连同当时的单价一起留在库里，所以能还原当时的成本构成。
     */
    @GetMapping("/recipes/revisions/{revisionId}/items")
    public Result<?> revisionItems(@PathVariable Long revisionId) {
        try {
            return Result.success(recipeRevisionService.revisionItems(revisionId));
        } catch (com.youjian.banquet.service.RecipeRevisionService.RecipeAccessDeniedException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取版本明细失败: " + e.getMessage());
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
        Object yieldObj=m.containsKey("yieldRate")?m.get("yieldRate"):m.get("yield_rate");
        if(yieldObj!=null)item.setYieldRate(new java.math.BigDecimal(yieldObj.toString()));
        Object wastageObj=m.containsKey("wastageRate")?m.get("wastageRate"):m.get("wastage_rate");
        if(wastageObj!=null)item.setWastageRate(new java.math.BigDecimal(wastageObj.toString()));
        return item;
    }

    @GetMapping("/recipes/dishes-with-recipe")
    public Result<List<DishMaster>> dishesWithRecipe(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            final Long effectiveStoreId = resolveQueryStoreId(storeId);
            List<DishRecipe> recipes = recipeRepo.findAll();
            Set<String> dishIds = recipes.stream()
                // 历史版本的行也留在同一张表里，只认当前生效的，否则"有配方的菜品"会把
                // 已经被改掉的旧配方也算进来。
                .filter(r -> r.getIsActive() == null || r.getIsActive() == 1)
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
                // 只重算当前生效版本；历史行的单价是它当时的快照，重算会把历史改写掉。
                List<DishRecipe> recipes = recipeRepo
                        .findByDishIdAndStoreId(dish.getDishId(), dish.getStoreId());
                if (recipes.isEmpty()) continue;
                java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
                for (DishRecipe r : recipes) {
                    IngredientMaster ingredient = ingredientRepo.findByIngredientIdAndStoreId(r.getIngredientId(), dish.getStoreId()).orElse(null);
                    totalCost = totalCost.add(com.youjian.banquet.service.DishCostCalculator.calculateLine(r, ingredient));
                    recipeRepo.save(r);
                }
                totalCost = totalCost.setScale(2, java.math.RoundingMode.HALF_UP);
                dish.setCostPrice(totalCost);
                dish.setCostRate(com.youjian.banquet.service.DishCostCalculator.costRate(totalCost, dish.getSalePrice()));
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
