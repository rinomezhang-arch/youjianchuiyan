package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.DishRecipe;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.entity.RecipeRevision;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.DishRecipeRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.RecipeRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配方保存的唯一入口，负责版本化、校验、加锁和成本刷新。
 * <p>
 * 基线的 {@code RecipeController.saveRecipe} 是「先 delete 整组再逐条 insert」：
 * 保存一次就把该菜品该门店的配方行物理删掉重建，recipe_id 换新、created_at 丢失，
 * 改过什么、谁改的、改之前是什么样，全查不回来；而且删除发生在校验之前，
 * 一条非法原料就能把原有配方连带毁掉。配方直接决定菜品成本，这两件事都不能留着。
 * <p>
 * 这里改成版本化保存，五件事在同一个事务里按固定顺序做：
 * <ol>
 *   <li><b>加锁</b>——对 dish_master 对应行 {@code FOR UPDATE}，同一菜品的并发保存串行化；</li>
 *   <li><b>全量校验</b>——原料必须存在于本门店、用量必须为正且不超过三位小数。
 *       <b>校验前置于任何一次写入</b>，所以非法输入的结果是零写入，而不是"删了一半才发现"；</li>
 *   <li><b>写版本</b>——生成新版本号；</li>
 *   <li><b>旧行置 0</b>——{@code UPDATE ... SET is_active = 0}，<b>全程没有 DELETE</b>；</li>
 *   <li><b>写新行 + 刷成本</b>——回写 dish_master 的成本价与成本率，<b>不碰售价</b>。</li>
 * </ol>
 * 任一步失败整个事务回滚，不会出现"配方换了但成本还是旧的"这种半成品状态。
 */
@Service
public class RecipeRevisionService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRevisionService.class);

    /** 用量最多三位小数，与 dish_recipe.quantity DECIMAL(10,3) 对齐——多出来的位数会被数据库悄悄截断。 */
    private static final int QUANTITY_SCALE = 3;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired private DishRecipeRepository recipeRepo;
    @Autowired private RecipeRevisionRepository revisionRepo;
    @Autowired private IngredientMasterRepository ingredientRepo;
    @Autowired private DishMasterRepository dishRepo;
    @Autowired private JdbcTemplate jdbc;

    /** 输入非法（客户端该改请求），与系统异常区分开，让接口能回 400 而不是一律 500。 */
    public static class RecipeValidationException extends RuntimeException {
        public RecipeValidationException(String message) { super(message); }
    }

    /**
     * 保存一个新配方版本。
     *
     * @param items 本次提交的全部条目；空列表表示清空配方，同样生成一个可追溯的版本
     * @return 新生成的版本
     */
    @Transactional
    public RecipeRevision saveNewVersion(String dishId, Long storeId, List<DishRecipe> items,
                                         String operator, String note) {
        if (dishId == null || dishId.isBlank()) throw new RecipeValidationException("菜品编号不能为空");
        if (storeId == null) throw new RecipeValidationException("门店不能为空");

        // 1. 先加锁再做任何事：同一菜品的两次并发保存在这里排队，避免版本号打架、
        //    也避免两次保存的 is_active 切换交错成"两个版本同时生效"。
        //    锁的是菜品档案行，不是配方行——配方行会被本次保存新增，锁不住还没存在的行。
        List<java.util.Map<String, Object>> locked = jdbc.queryForList(
                "SELECT dish_id FROM dish_master WHERE dish_id = ? AND store_id = ? FOR UPDATE",
                dishId, storeId);
        if (locked.isEmpty()) {
            throw new RecipeValidationException("菜品不存在或不属于当前门店：" + dishId);
        }

        // 2. 全量校验 + 计算，全部完成之后才开始写。校验期间一行都不落库。
        List<DishRecipe> prepared = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        Set<String> seen = new HashSet<>();
        int line = 0;
        for (DishRecipe raw : items) {
            line++;
            String ingredientId = raw.getIngredientId() == null ? null : raw.getIngredientId().trim();
            if (ingredientId == null || ingredientId.isEmpty()) {
                throw new RecipeValidationException("第 " + line + " 行：原料编号不能为空");
            }
            if (!seen.add(ingredientId)) {
                // 同一原料出现两次，合计成本会翻倍，且没法判断该以哪一行为准，直接拒绝。
                throw new RecipeValidationException("第 " + line + " 行：原料 " + ingredientId + " 重复出现");
            }
            IngredientMaster ing = ingredientRepo.findByIngredientIdAndStoreId(ingredientId, storeId)
                    .orElseThrow(() -> new RecipeValidationException(
                            "原料不存在或不属于当前门店：" + ingredientId));

            BigDecimal quantity = raw.getQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RecipeValidationException("第 " + line + " 行：用量必须大于 0");
            }
            if (quantity.scale() > QUANTITY_SCALE) {
                throw new RecipeValidationException("第 " + line + " 行：用量最多三位小数，收到 " + quantity.toPlainString());
            }

            DishRecipe item = new DishRecipe();
            item.setStoreId(storeId);
            item.setDishId(dishId);
            item.setIngredientId(ingredientId);
            item.setIngredientName(ing.getIngredientName());
            item.setQuantity(quantity);
            String unit = raw.getUnit() != null && !raw.getUnit().isBlank()
                    ? raw.getUnit().trim() : ing.getUsageUnit();
            if (unit == null || unit.isBlank()) {
                throw new RecipeValidationException("第 " + line + " 行：单位为空，且原料 "
                        + ingredientId + " 也没有配置使用单位");
            }
            item.setUnit(unit);
            item.setSortOrder(raw.getSortOrder() != null ? raw.getSortOrder() : line);
            item.setWastageRate(raw.getWastageRate());
            item.setNotes(raw.getNotes());

            item.setYieldRate(raw.getYieldRate());
            try {
                totalCost = totalCost.add(DishCostCalculator.calculateLine(item, ing));
            } catch (IllegalArgumentException e) {
                throw new RecipeValidationException("第 " + line + " 行：" + e.getMessage());
            }
            prepared.add(item);
        }

        // 3. 写版本。版本号在锁保护下取，唯一键 (store_id, dish_id, version_no) 再兜一道。
        int nextVersion = safeVersion(revisionRepo.findMaxVersionNo(storeId, dishId)) + 1;
        RecipeRevision revision = new RecipeRevision();
        revision.setStoreId(storeId);
        revision.setDishId(dishId);
        revision.setVersionNo(nextVersion);
        revision.setItemCount(prepared.size());
        revision.setTotalCost(totalCost.setScale(4, RoundingMode.HALF_UP));
        revision.setCreatedBy(operator);
        revision.setCreatedAt(LocalDateTime.now());
        revision.setNote(note);
        revision = revisionRepo.save(revision);

        // 4. 旧版本置为历史——只改标记，不删行。历史明细连同它当时的单价一起留在库里。
        int retired = jdbc.update(
                "UPDATE dish_recipe SET is_active = 0, updated_at = NOW() " +
                        "WHERE dish_id = ? AND store_id = ? AND (is_active = 1 OR is_active IS NULL)",
                dishId, storeId);

        // 5. 写新版本明细。
        LocalDateTime now = LocalDateTime.now();
        for (DishRecipe item : prepared) {
            item.setRevisionId(revision.getRevisionId());
            item.setIsActive(1);
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            recipeRepo.save(item);
        }

        // 6. 同一事务内刷新菜品成本。**只写 cost_price 与 cost_rate，绝不碰 sale_price**——
        //    售价是经营决策，不能被一次配方保存顺手改掉。
        DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId).orElse(null);
        if (dish != null) {
            dish.setCostPrice(totalCost.setScale(2, RoundingMode.HALF_UP));
            dish.setCostRate(DishCostCalculator.costRate(dish.getCostPrice(), dish.getSalePrice()));
            dishRepo.save(dish);
        }

        log.info("【配方版本】{} 门店 {} 菜品 {} 保存为第 {} 版，{} 条明细，合计成本 {}，历史 {} 行转为不生效",
                operator, storeId, dishId, nextVersion, prepared.size(), revision.getTotalCost(), retired);
        return revision;
    }

    public static class RecipeAccessDeniedException extends RuntimeException {
        public RecipeAccessDeniedException() { super("无权查看其他门店配方版本"); }
    }

    /** 当前生效的配方明细。 */
    public List<DishRecipe> activeRecipe(String dishId, Long storeId) {
        return recipeRepo.findByDishIdAndStoreId(dishId, storeId).stream()
                .sorted(java.util.Comparator.comparing(DishRecipe::getSortOrder, java.util.Comparator.nullsLast(Integer::compareTo))).toList();
    }

    /** 版本列表，最新在前。 */
    public List<RecipeRevision> revisions(String dishId, Long storeId) {
        return revisionRepo.findByStoreIdAndDishIdOrderByVersionNoDesc(storeId, dishId);
    }

    /** 某个历史版本的明细，用于逐版本追溯。 */
    public List<DishRecipe> revisionItems(Long revisionId) {
        RecipeRevision revision=revisionRepo.findById(revisionId)
                .orElseThrow(() -> new RecipeValidationException("配方版本不存在"));
        try { com.youjian.banquet.util.UserContext.assertStoreAccess(revision.getStoreId()); }
        catch (IllegalArgumentException e) { throw new RecipeAccessDeniedException(); }
        return recipeRepo.findByRevisionIdOrderBySortOrderAsc(revisionId);
    }

    private static int safeVersion(Integer max) {
        return max == null ? 0 : max;
    }
}
