package com.youjian.banquet.service;

import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.DishRecipe;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.entity.RecipeRevision;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.DishRecipeRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.RecipeRevisionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-R3-RECIPE 隔离库集成测试。
 * <p>
 * 上一轮只有 Mockito 单元测试，Codex 退回时明确指出：那些证明不了数据库并发和事务回滚。
 * 这个类用本机隔离 MySQL（127.0.0.1:13317）跑真实 JPA、真实事务、真实两个连接竞争，
 * 每次运行新建独立 schema，只写合成数据，不读生产凭证、不碰生产库。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeRevisionMysqlIntegrationTest {

    private static final String HOST = "jdbc:mysql://127.0.0.1:13317/";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String DISH = "YC00001";
    private static final Long STORE = 1L;

    private static String schema;
    private static AnnotationConfigApplicationContext ctx;
    private static JdbcTemplate jdbc;
    private static RecipeRevisionService service;
    private static DishRecipeRepository recipeRepo;
    private static RecipeRevisionRepository revisionRepo;

    @EnableTransactionManagement
    static class TxConfig { }

    @BeforeEach
    void setUp() {
        schema = "recipe_it_" + System.currentTimeMillis();
        new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        DataSource ds = new DriverManagerDataSource(HOST + schema + OPTS, "root", "");
        jdbc = new JdbcTemplate(ds);

        // 只挂被测流程真正用到的四个实体，不把 85 个实体整包拖进来。
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        emfBean.setJpaVendorAdapter(adapter);
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emfBean.setJpaProperties(props);
        // 必须扫描包才能建出默认持久化单元；扫完再清空，只留被测流程用到的四个实体，
        // 避免把 85 个实体整包拖进来（本分支还没有 ToolReturn 的保留字修复）。
        emfBean.setPackagesToScan("com.youjian.banquet.entity");
        emfBean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            for (Class<?> c : List.of(DishRecipe.class, RecipeRevision.class,
                    DishMaster.class, IngredientMaster.class)) {
                pui.addManagedClassName(c.getName());
            }
        });
        emfBean.afterPropertiesSet();
        EntityManagerFactory emf = Objects.requireNonNull(emfBean.getObject());
        EntityManager sharedEm = SharedEntityManagerCreator.createSharedEntityManager(emf);
        JpaRepositoryFactory factory = new JpaRepositoryFactory(sharedEm);

        recipeRepo = factory.getRepository(DishRecipeRepository.class);
        revisionRepo = factory.getRepository(RecipeRevisionRepository.class);
        IngredientMasterRepository ingredientRepo = factory.getRepository(IngredientMasterRepository.class);
        DishMasterRepository dishRepo = factory.getRepository(DishMasterRepository.class);

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(PlatformTransactionManager.class, () -> new JpaTransactionManager(emf));
        ctx.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(ds));
        ctx.registerBean(DishRecipeRepository.class, () -> recipeRepo);
        ctx.registerBean(RecipeRevisionRepository.class, () -> revisionRepo);
        ctx.registerBean(IngredientMasterRepository.class, () -> ingredientRepo);
        ctx.registerBean(DishMasterRepository.class, () -> dishRepo);
        ctx.register(TxConfig.class);
        ctx.registerBean(RecipeRevisionService.class);
        ctx.refresh();
        service = ctx.getBean(RecipeRevisionService.class);
        seed();
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
        com.youjian.banquet.util.UserContext.clear();
        System.out.println("测试库保留以便复核：" + schema);
    }

    void seed() {
        com.youjian.banquet.util.UserContext.set(new com.youjian.banquet.util.UserContext.CurrentUser(1L, STORE, "store_manager", "合成验收员"));
        // 龙虾：每只 17.12 元，1 只 = 500 克，出成率 60% → 每克净成本 0.05706667
        insertIngredient("ING001", "波士顿龙虾", "17.12", "500", "60");
        insertIngredient("ING002", "小葱", "2.00", "1000", "90");
        jdbc.update("INSERT INTO dish_master (dish_id, store_id, dish_name, sale_price) VALUES (?,?,?,?)",
                DISH, STORE, "清蒸龙虾", new BigDecimal("288.00"));
    }

    private void insertIngredient(String id, String name, String price, String conv, String yield) {
        jdbc.update("INSERT INTO ingredient_master (ingredient_id, store_id, ingredient_name, purchase_unit, usage_unit, "
                        + "unit_price, conversion_rate, yield_rate) VALUES (?,?,?,?,?,?,?,?)",
                id, STORE, name, "只", "克", new BigDecimal(price), new BigDecimal(conv), new BigDecimal(yield));
    }

    private DishRecipe item(String ingredientId, String quantity) {
        DishRecipe r = new DishRecipe();
        r.setIngredientId(ingredientId);
        r.setQuantity(new BigDecimal(quantity));
        return r;
    }

    // ==================== 两版并存，旧版仍可读 ====================

    @Test
    @Order(1)
    @DisplayName("连续保存两版：旧版明细仍可按版本号完整读回，含当时的单价")
    void twoVersionsBothReadable() {
        RecipeRevision v1 = service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", "初版");
        RecipeRevision v2 = service.saveNewVersion(DISH, STORE,
                List.of(item("ING001", "200"), item("ING002", "10")), "zhangjing", "加量加葱");

        assertEquals(1, v1.getVersionNo());
        assertEquals(2, v2.getVersionNo());

        List<DishRecipe> old = service.revisionItems(v1.getRevisionId());
        assertEquals(1, old.size(), "旧版明细丢了");
        assertEquals(0, new BigDecimal("100.000").compareTo(old.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("0.05706667").compareTo(old.get(0).getNetUnitPrice()),
                "旧版单价快照没保住");
        assertEquals(0, old.get(0).getIsActive(), "旧版应标记为不生效");

        List<DishRecipe> current = service.revisionItems(v2.getRevisionId());
        assertEquals(2, current.size());
        // 库里三行都在，一行都没被删
        assertEquals(3, (long) jdbc.queryForObject("SELECT COUNT(*) FROM dish_recipe", Integer.class));
    }

    // ==================== 成本卡只用当前版 ====================

    @Test
    @Order(2)
    @DisplayName("既有业务查询只返回当前版：成本卡不会把历史用量叠加进去")
    void legacyQueriesReturnOnlyActiveVersion() {
        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);
        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "200")), "rino", null);

        // KitchenSupplyService.calculateAndSaveCostCard 走的就是这个方法
        List<DishRecipe> forCostCard = recipeRepo.findByDishIdAndStoreId(DISH, STORE);
        assertEquals(1, forCostCard.size(), "读到了历史版本，成本卡会把两版用量加在一起");
        assertEquals(0, new BigDecimal("200.000").compareTo(forCostCard.get(0).getQuantity()));

        // 按成本卡的算法累加，结果必须只等于当前版
        BigDecimal cardCost = BigDecimal.ZERO;
        for (DishRecipe r : forCostCard) cardCost = cardCost.add(r.getTotalCost());
        // 库里 total_cost 是 DECIMAL(15,4)，期望值要按同样精度比，否则比的是精度不是逻辑。
        // 若历史被叠加，这里会变成 100 克那版加 200 克那版，数值明显偏大，仍然抓得住。
        BigDecimal expected = new BigDecimal("200").multiply(new BigDecimal("0.05706667"))
                .setScale(4, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(cardCost), "成本卡金额被历史版本叠加了");

        // 其余几个既有查询同样只认当前版
        assertEquals(1, recipeRepo.findByStoreId(STORE).size());
        assertEquals(1, recipeRepo.findByIngredientIdAndStoreId("ING001", STORE).size());
        assertEquals(1, recipeRepo.findDistinctDishIdsByStoreId(STORE).size());
        // 单行查询在多版本下原本会抛 IncorrectResultSizeDataAccessException
        assertTrue(recipeRepo.findByDishIdAndStoreIdAndIngredientId(DISH, STORE, "ING001").isPresent());
    }

    @Test
    @Order(3)
    @DisplayName("入库价更新后重新保存：成本按新价重算，且不叠加历史版本")
    void priceUpdateDoesNotAccumulateHistory() {
        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);
        BigDecimal costV1 = jdbc.queryForObject(
                "SELECT cost_price FROM dish_master WHERE dish_id=?", BigDecimal.class, DISH);

        // 最近一次入库价翻倍
        jdbc.update("UPDATE ingredient_master SET unit_price=? WHERE ingredient_id=? AND store_id=?",
                new BigDecimal("34.24"), "ING001", STORE);
        service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null);

        BigDecimal costV2 = jdbc.queryForObject(
                "SELECT cost_price FROM dish_master WHERE dish_id=?", BigDecimal.class, DISH);
        // 直接用精确期望值，不用"costV1 乘 2"——那是拿两次各自四舍五入的结果做比较，
        // 差一分钱就误报。100 克 × 17.12/(500×60%) = 5.71；单价翻倍后 = 11.41。
        // 若历史被叠加，这里会是 5.71 + 11.41 ≈ 17.12，差得很远，照样抓得住。
        assertEquals(0, new BigDecimal("5.71").compareTo(costV1));
        assertEquals(0, new BigDecimal("11.41").compareTo(costV2),
                "成本不等于当前版单独计算的结果，说明叠加了历史版本");

        BigDecimal activeSum = jdbc.queryForObject(
                "SELECT SUM(total_cost) FROM dish_recipe WHERE is_active=1", BigDecimal.class);
        assertEquals(0, costV2.compareTo(activeSum.setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    // ==================== 真实事务回滚 ====================

    @Test
    @Order(4)
    @DisplayName("成本回写溢出导致失败：版本、明细、成本一起回滚，库里什么都没留下")
    void costWriteFailureRollsBackEverything() {
        // Database failure occurs during final dish write, after version and detail writes.
        jdbc.execute("CREATE TRIGGER fail_recipe_cost BEFORE UPDATE ON dish_master FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SYN_RECIPE_COST_FAILURE'");
        Exception failure=assertThrows(Exception.class,
                () -> service.saveNewVersion(DISH, STORE, List.of(item("ING001", "100")), "rino", null));
        Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();
        assertTrue(cause.getMessage().contains("SYN_RECIPE_COST_FAILURE"));

        assertEquals(0, (long) jdbc.queryForObject("SELECT COUNT(*) FROM recipe_revision", Integer.class),
                "版本记录残留了");
        assertEquals(0, (long) jdbc.queryForObject("SELECT COUNT(*) FROM dish_recipe", Integer.class),
                "配方明细残留了");
    }

    // ==================== 两连接并发 ====================

    @Test
    @Order(5)
    @DisplayName("两个连接同时保存同一菜品：版本号不重复，最终只有一个生效版本")
    void concurrentSavesAreSerialized() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final String qty = i == 0 ? "100" : "300";
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    return service.saveNewVersion(DISH, STORE, List.of(item("ING001", qty)), "t" + qty, null);
                } catch (RuntimeException e) {
                    return e;
                }
            }));
        }
        start.countDown();
        int ok = 0;
        for (java.util.concurrent.Future<Object> f : futures) {
            if (f.get(30, java.util.concurrent.TimeUnit.SECONDS) instanceof RecipeRevision) ok++;
        }
        pool.shutdown();

        assertEquals(2, ok, "两次保存都应成功，只是被串行化");
        List<Integer> versions = jdbc.queryForList(
                "SELECT version_no FROM recipe_revision ORDER BY version_no", Integer.class);
        assertEquals(List.of(1, 2), versions, "版本号重复或跳号，说明锁没起作用");
        assertEquals(1, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM dish_recipe WHERE is_active=1", Integer.class),
                "同时存在多个生效版本");
    }

    // ==================== 迁移前后既有查询结果比对 ====================

    @Test
    @Order(6)
    @DisplayName("迁移在真实库执行：存量行迁移前后查询结果完全一致，且默认仍为生效")
    void migrationKeepsExistingQueriesIdentical() throws Exception {
        String legacy = "recipe_mig_" + System.currentTimeMillis();
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""));
        admin.execute("CREATE DATABASE " + legacy + " CHARACTER SET utf8mb4");
        JdbcTemplate old = new JdbcTemplate(new DriverManagerDataSource(HOST + legacy + OPTS, "root", ""));

        // 版本化之前的表结构：没有 revision_id / is_active
        old.execute("CREATE TABLE dish_recipe (recipe_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "store_id BIGINT, dish_id VARCHAR(40), ingredient_id VARCHAR(50), ingredient_name VARCHAR(100), "
                + "unit VARCHAR(20), unit_price DECIMAL(15,8), quantity DECIMAL(10,3), total_cost DECIMAL(15,4), "
                + "sort_order INT, wastage_rate DECIMAL(5,2), yield_rate DECIMAL(5,2), last_entry_date DATE, "
                + "net_unit_price DECIMAL(15,8), notes VARCHAR(255), created_at DATETIME, updated_at DATETIME) ENGINE=InnoDB");
        old.update("INSERT INTO dish_recipe (store_id, dish_id, ingredient_id, quantity, total_cost) "
                + "VALUES (1,'YC00001','ING001',100.000,5.7067)");
        old.update("INSERT INTO dish_recipe (store_id, dish_id, ingredient_id, quantity, total_cost) "
                + "VALUES (1,'YC00001','ING002',10.000,0.0222)");

        String before = old.queryForList(
                "SELECT recipe_id, dish_id, ingredient_id, quantity, total_cost FROM dish_recipe ORDER BY recipe_id")
                .toString();

        // 真实执行迁移脚本
        String sql = Files.readString(Path.of("..", "scripts", "migrations", "recipe_revision_v1.sql"));
        for (String stmt : sql.split(";")) {
            String s = stmt.replaceAll("(?m)^--.*$", "").trim();
            if (!s.isEmpty()) old.execute(s);
        }

        String after = old.queryForList(
                "SELECT recipe_id, dish_id, ingredient_id, quantity, total_cost FROM dish_recipe ORDER BY recipe_id")
                .toString();
        assertEquals(before, after, "迁移改动了存量数据");

        // 存量行默认仍为生效，revision_id 留空表示导入基线
        assertEquals(2, (long) old.queryForObject(
                "SELECT COUNT(*) FROM dish_recipe WHERE is_active=1", Integer.class));
        assertEquals(2, (long) old.queryForObject(
                "SELECT COUNT(*) FROM dish_recipe WHERE revision_id IS NULL", Integer.class));
        assertEquals(0, (long) old.queryForObject(
                "SELECT COUNT(*) FROM recipe_revision", Integer.class));
        System.out.println("迁移比对库保留以便复核：" + legacy);
    }
}
