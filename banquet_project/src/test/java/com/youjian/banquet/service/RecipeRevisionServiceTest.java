package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.DishRecipe;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.entity.RecipeRevision;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.DishRecipeRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.RecipeRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CL-R3-RECIPE 验收测试。
 * <p>
 * 这些用例全部不依赖数据库，验证的是保存流程本身的行为约束：写与不写、顺序、以及失败时的传播。
 * 库级行为（真实事务回滚、FOR UPDATE 实际串行化两个连接）需要真实 MySQL，
 * 按任务说明留到基线集成后的运行时验证，本轮不冒充已通过。
 */
class RecipeRevisionServiceTest {

    private RecipeRevisionService service;
    private DishRecipeRepository recipeRepo;
    private RecipeRevisionRepository revisionRepo;
    private IngredientMasterRepository ingredientRepo;
    private DishMasterRepository dishRepo;
    private JdbcTemplate jdbc;

    private static final String DISH = "YC00001";
    private static final Long STORE = 1L;

    @BeforeEach
    void setUp() {
        service = new RecipeRevisionService();
        recipeRepo = mock(DishRecipeRepository.class);
        revisionRepo = mock(RecipeRevisionRepository.class);
        ingredientRepo = mock(IngredientMasterRepository.class);
        dishRepo = mock(DishMasterRepository.class);
        jdbc = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(service, "recipeRepo", recipeRepo);
        ReflectionTestUtils.setField(service, "revisionRepo", revisionRepo);
        ReflectionTestUtils.setField(service, "ingredientRepo", ingredientRepo);
        ReflectionTestUtils.setField(service, "dishRepo", dishRepo);
        ReflectionTestUtils.setField(service, "jdbc", jdbc);

        // 菜品存在（加锁查询返回一行）
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dish_id", DISH);
        when(jdbc.queryForList(contains("FOR UPDATE"), anyString(), anyLong())).thenReturn(List.of(row));

        when(revisionRepo.findMaxVersionNo(STORE, DISH)).thenReturn(0);
        // 保存版本时补一个自增主键，模拟数据库回填
        AtomicInteger seq = new AtomicInteger(100);
        when(revisionRepo.save(any(RecipeRevision.class))).thenAnswer(inv -> {
            RecipeRevision r = inv.getArgument(0);
            if (r.getRevisionId() == null) r.setRevisionId((long) seq.incrementAndGet());
            return r;
        });
        when(recipeRepo.save(any(DishRecipe.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** 龙虾：采购单位「只」17.12 元，1 只 = 500 克，出成率 60% → 每克净成本 0.05706667。 */
    private IngredientMaster lobster() {
        IngredientMaster ing = new IngredientMaster();
        ing.setIngredientId("ING001");
        ing.setStoreId(STORE);
        ing.setIngredientName("波士顿龙虾");
        ing.setUsageUnit("克");
        ing.setPurchaseUnit("只");
        ing.setUnitPrice(new BigDecimal("17.12"));
        ing.setConversionRate(new BigDecimal("500"));
        ing.setYieldRate(new BigDecimal("60"));
        return ing;
    }

    private DishRecipe item(String ingredientId, String quantity) {
        DishRecipe r = new DishRecipe();
        r.setIngredientId(ingredientId);
        r.setQuantity(new BigDecimal(quantity));
        return r;
    }

    private DishMaster dish(String salePrice) {
        DishMaster d = new DishMaster();
        d.setDishId(DISH);
        d.setStoreId(STORE);
        d.setSalePrice(new BigDecimal(salePrice));
        return d;
    }

    private void ingredientExists() {
        when(ingredientRepo.findByIngredientIdAndStoreId("ING001", STORE)).thenReturn(Optional.of(lobster()));
    }

    // ==================== 验收 1：不得先删后插丢历史 ====================

    @Test
    @DisplayName("保存配方全程不删行：旧版本只置为不生效，deleteByDishIdAndStoreId 一次都不调用")
    void savingNeverDeletes() {
        ingredientExists();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(dish("100")));

        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);

        verify(recipeRepo, never()).deleteByDishIdAndStoreId(anyString(), anyLong());
        verify(recipeRepo, never()).deleteAll();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, atLeastOnce()).update(sql.capture(), anyString(), anyLong());
        for (String executed : sql.getAllValues()) {
            assertFalse(executed.toUpperCase().contains("DELETE"),
                    "保存流程里不应出现 DELETE，实际执行了：" + executed);
        }
        assertTrue(sql.getAllValues().stream().anyMatch(q -> q.contains("is_active = 0")),
                "旧版本应被置为不生效");
    }

    // ==================== 验收 2：两个版本可追溯 ====================

    @Test
    @DisplayName("连续两次保存生成第 1、2 版，版本号递增且各自记录条目数与合计成本")
    void twoSavedVersionsAreTraceable() {
        ingredientExists();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(dish("100")));

        RecipeRevision first = service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", "初版");
        when(revisionRepo.findMaxVersionNo(STORE, DISH)).thenReturn(first.getVersionNo());
        RecipeRevision second = service.saveNewVersion(DISH, STORE, List.of(item("ING001", "200")), "zhangjing", "加量");

        assertEquals(1, first.getVersionNo());
        assertEquals(2, second.getVersionNo());
        assertNotEquals(first.getRevisionId(), second.getRevisionId());
        assertEquals("rino", first.getCreatedBy());
        assertEquals("zhangjing", second.getCreatedBy());
        // 用量翻倍，合计成本随之翻倍：100*0.05706667=5.7067，200*…=11.4133
        assertEquals(new BigDecimal("5.7067"), first.getTotalCost());
        assertEquals(new BigDecimal("11.4133"), second.getTotalCost());
    }

    // ==================== 验收 3：非法原料导致零写入 ====================

    @Test
    @DisplayName("原料不属于本门店：抛校验异常，且没有任何写入发生")
    void invalidIngredientCausesZeroWrites() {
        when(ingredientRepo.findByIngredientIdAndStoreId("ING999", STORE)).thenReturn(Optional.empty());

        List<DishRecipe> items = new ArrayList<>();
        items.add(item("ING001", "100"));   // 合法
        items.add(item("ING999", "50"));    // 非法，排在后面
        ingredientExists();

        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion(DISH, STORE, items, "rino", null));

        // 关键：非法条目在第二行，但第一行也不能被写进去——校验必须全部通过后才开始写
        verify(recipeRepo, never()).save(any(DishRecipe.class));
        verify(revisionRepo, never()).save(any(RecipeRevision.class));
        verify(dishRepo, never()).save(any(DishMaster.class));
        verify(jdbc, never()).update(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("用量为零或负数、超过三位小数、原料重复，一律拒绝且零写入")
    void otherInvalidInputsAlsoWriteNothing() {
        ingredientExists();

        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion(DISH, STORE, List.of(item("ING001", "0")), "rino", null));
        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion(DISH, STORE, List.of(item("ING001", "-5")), "rino", null));
        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion(DISH, STORE, List.of(item("ING001", "1.23456")), "rino", null));
        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion(DISH, STORE,
                        List.of(item("ING001", "10"), item("ING001", "20")), "rino", null));

        verify(recipeRepo, never()).save(any(DishRecipe.class));
        verify(revisionRepo, never()).save(any(RecipeRevision.class));
        verify(jdbc, never()).update(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("菜品不存在或不属于本门店：加锁查询落空即拒绝，零写入")
    void unknownDishIsRejectedBeforeAnyWrite() {
        when(jdbc.queryForList(contains("FOR UPDATE"), anyString(), anyLong())).thenReturn(List.of());
        ingredientExists();

        assertThrows(RecipeRevisionService.RecipeValidationException.class,
                () -> service.saveNewVersion("NOPE", STORE, List.of(item("ING001", "100")), "rino", null));

        verify(revisionRepo, never()).save(any(RecipeRevision.class));
        verify(recipeRepo, never()).save(any(DishRecipe.class));
    }

    // ==================== 验收 4：并发更新串行化 ====================

    @Test
    @DisplayName("排他锁在任何写入之前取得：先 SELECT ... FOR UPDATE，再取版本号，最后才写")
    void lockIsTakenBeforeAnyWrite() {
        ingredientExists();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(dish("100")));

        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);

        InOrder order = inOrder(jdbc, revisionRepo, recipeRepo);
        order.verify(jdbc).queryForList(contains("FOR UPDATE"), anyString(), anyLong());
        order.verify(revisionRepo).findMaxVersionNo(STORE, DISH);
        order.verify(revisionRepo).save(any(RecipeRevision.class));
        order.verify(jdbc).update(contains("is_active = 0"), anyString(), anyLong());
        order.verify(recipeRepo).save(any(DishRecipe.class));
    }

    // ==================== 验收 5：成本刷新失败全部回滚，且不改售价 ====================

    @Test
    @DisplayName("成本刷新只写成本价与成本率，售价原封不动")
    void costRefreshNeverTouchesSalePrice() {
        ingredientExists();
        DishMaster target = dish("100");
        BigDecimal originalSalePrice = target.getSalePrice();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(target));

        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);

        ArgumentCaptor<DishMaster> saved = ArgumentCaptor.forClass(DishMaster.class);
        verify(dishRepo).save(saved.capture());
        assertEquals(0, originalSalePrice.compareTo(saved.getValue().getSalePrice()), "售价被改动了");
        assertEquals(new BigDecimal("5.71"), saved.getValue().getCostPrice());
        // 成本率 = 5.7067/100*100 = 5.71%
        assertEquals(new BigDecimal("5.71"), saved.getValue().getCostRate());
    }

    @Test
    @DisplayName("成本刷新失败时异常向外传播，交给事务回滚，不吞成半成功")
    void costRefreshFailurePropagates() {
        ingredientExists();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(dish("100")));
        when(dishRepo.save(any(DishMaster.class))).thenThrow(new RuntimeException("成本回写失败"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null));
        assertEquals("成本回写失败", thrown.getMessage());
    }

    // ==================== 附加：净成本换算 ====================

    @Test
    @DisplayName("净成本按 单价/(换算率*出成率/100) 折算，不是直接乘采购单价")
    void netUnitPriceUsesConversionAndYield() {
        ingredientExists();
        when(dishRepo.findByDishIdAndStoreId(DISH, STORE)).thenReturn(Optional.of(dish("100")));

        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);

        ArgumentCaptor<DishRecipe> saved = ArgumentCaptor.forClass(DishRecipe.class);
        verify(recipeRepo).save(saved.capture());
        DishRecipe row = saved.getValue();
        // 17.12 / (500 * 60/100) = 17.12 / 300 = 0.05706667
        assertEquals(new BigDecimal("0.05706667"), row.getNetUnitPrice());
        assertEquals(1, row.getIsActive());
        assertNotNull(row.getRevisionId());
        assertEquals("克", row.getUnit(), "单位应回退到原料档案的使用单位");
    }
}
