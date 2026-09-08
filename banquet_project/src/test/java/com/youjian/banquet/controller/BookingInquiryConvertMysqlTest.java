package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-INQUIRY-CONVERT-07：公开咨询转正式预订。
 * <p>
 * 真实隔离 MySQL、真实事务代理。**候选迁移在这里真跑一遍**：
 * 先把 booking_inquiry 退回到没有 booking_id 的历史形态、塞两条历史咨询，
 * 再执行迁移文件原文，最后断言列与唯一索引都建出来了且历史行完好。
 * 不这么做的话，"迁移能用"就只是个说法。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingInquiryConvertMysqlTest {

    static final long STORE = 1L, OTHER_STORE = 2L;

    JdbcTemplate jdbc;
    BookingInquiryController controller;
    LocalContainerEntityManagerFactoryBean factory;
    String schema;
    int nextTable = 10;

    @BeforeAll
    void start() throws Exception {
        schema = "inq_conv_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(url + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("INQ_CONVERT_EVIDENCE schema=" + schema + " retained=true");

        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + opts, "root", "");
        jdbc = new JdbcTemplate(ds);

        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                StoreInfo.class.getName(), BookingInquiry.class.getName(),
                BookingMaster.class.getName(), BookingTable.class.getName(),
                TableMaster.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();

        // ---- 把 booking_inquiry 退回历史形态，再真跑候选迁移 ----
        // Hibernate 给唯一约束起的名字不固定，运行时查出来再删，不硬编码
        for (String index : jdbc.queryForList(
                "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' "
                        + "AND COLUMN_NAME='booking_id'", String.class)) {
            jdbc.execute("ALTER TABLE booking_inquiry DROP INDEX `" + index + "`");
        }
        jdbc.update("INSERT INTO booking_inquiry(store_id,customer_name,customer_phone,status,created_at) "
                + "VALUES (1,'历史咨询一','13900000001','pending',NOW()),"
                + "(1,'历史咨询二','13900000002','rejected',NOW())");
        jdbc.execute("ALTER TABLE booking_inquiry DROP COLUMN booking_id");
        String migration = new String(new org.springframework.core.io.ClassPathResource(
                "db/migration/inquiry_booking_link_v1.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        for (String one : migration.split(";")) {
            String sql = one.replaceAll("(?m)^\\s*--.*$", "").trim();
            if (!sql.isEmpty()) jdbc.execute(sql);
        }

        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,status) "
                + "VALUES (1,'S-1','合成门店一','open'),(2,'S-2','合成门店二','open')");

        JpaTransactionManager manager = new JpaTransactionManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        repositories.addRepositoryProxyPostProcessor((p, i) ->
                p.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource())));

        BookingInquiryController target = new BookingInquiryController();
        ReflectionTestUtils.setField(target, "jdbc", jdbc);
        ReflectionTestUtils.setField(target, "inquiryRepo",
                repositories.getRepository(BookingInquiryRepository.class));
        ReflectionTestUtils.setField(target, "objectMapper",
                new com.fasterxml.jackson.databind.ObjectMapper());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        controller = (BookingInquiryController) proxy.getProxy();
    }

    @AfterAll
    void stop() {
        if (factory != null) factory.destroy();
        System.out.println("INQ_CONVERT_EVIDENCE schema=" + schema + " retained=true");
    }

    @BeforeEach
    void identity() {
        // 加了转单鉴权之后，不带身份调用会直接 401——这正说明鉴权生效了。
        // 既有用例默认扮演一店员工；越权场景在各自用例里临时切换。
        UserContext.set(new UserContext.CurrentUser(1L, STORE, "store_manager", "synthetic-store1"));
    }

    @AfterEach
    void clearIdentity() { UserContext.clear(); }

    /** 临时换一个身份跑一段，跑完恢复——越权用例专用。 */
    <T> T as(Long staffId, Long storeId, String role, java.util.function.Supplier<T> body) {
        UserContext.CurrentUser before = UserContext.get();
        try {
            if (storeId == null) UserContext.clear();
            else UserContext.set(new UserContext.CurrentUser(staffId, storeId, role, "synthetic"));
            return body.get();
        } finally {
            if (before != null) UserContext.set(before); else UserContext.clear();
        }
    }

    // ==================== 夹具 ====================

    long inquiry(long store, String phone, String status) {
        jdbc.update("INSERT INTO booking_inquiry(store_id,customer_name,customer_phone,guest_count,"
                + "status,created_at) VALUES (?,?,?,8,?,NOW())", store, "合成客人", phone, status);
        // DriverManagerDataSource 每次都开新连接，LAST_INSERT_ID() 会拿到 0，必须按业务键回查
        return jdbc.queryForObject(
                "SELECT id FROM booking_inquiry WHERE customer_phone=? ORDER BY id DESC LIMIT 1",
                Long.class, phone);
    }

    int table(long store) {
        int id = ++nextTable;
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_status) VALUES (?,?,'idle')", id, store);
        return id;
    }

    Map<String, Object> convertBody(String date, String time, List<Integer> tables) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (date != null) body.put("bookingDate", date);
        if (time != null) body.put("bookingTime", time);
        if (tables != null) body.put("tableIds", tables);
        return body;
    }

    String tomorrow() { return LocalDate.now().plusDays(1).toString(); }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    /** 被拒时的零副作用：不建单、不占台、咨询不被改动。 */
    void rejectedWithoutSideEffect(long inquiryId, Map<String, Object> body, String contains) {
        long masters = count("booking_master"), tables = count("booking_table");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", inquiryId);
        var result = controller.convert(inquiryId, body);
        assertNotEquals(200, result.getCode(), "本应被拒绝：" + body);
        assertTrue(String.valueOf(result.getMessage()).contains(contains),
                "提示不符：" + result.getMessage());
        assertEquals(masters, count("booking_master"), "被拒不得建预订单");
        assertEquals(tables, count("booking_table"), "被拒不得占台");
        assertEquals(before, jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", inquiryId),
                "被拒不得改动咨询");
    }

    // ==================== 用例 ====================

    @Test @Order(1)
    @DisplayName("候选迁移在有历史数据的表上真跑通过：列与唯一索引都建出来，历史行完好")
    void candidateMigrationRunsOnLegacyData() {
        Integer col = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME='booking_id'", Integer.class);
        assertEquals(1, col, "迁移没有把 booking_id 列加上");

        Integer uniq = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='booking_inquiry' AND INDEX_NAME='uk_inquiry_booking_id' "
                        + "AND NON_UNIQUE=0", Integer.class);
        assertEquals(1, uniq, "迁移没有把唯一索引加上");

        // 两条历史咨询还在，booking_id 为 NULL，没有被回填也没有被删
        assertEquals(2, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE booking_id IS NULL", Long.class));
        assertEquals(2, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE customer_name LIKE '历史咨询%'", Long.class));
    }

    @Test @Order(2)
    @DisplayName("正常转换：一个事务内建预订单与台位，咨询标记 converted 并回填 booking_id")
    void successfulConversion() {
        long id = inquiry(STORE, "13800001001", "pending");
        int t1 = table(STORE), t2 = table(STORE);

        var result = controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t1, t2)));
        assertEquals(200, result.getCode(), String.valueOf(result.getMessage()));
        String bookingId = (String) result.getData().get("bookingId");
        assertNotNull(bookingId);
        assertFalse((Boolean) result.getData().get("replayed"));

        Map<String, Object> master = jdbc.queryForMap(
                "SELECT * FROM booking_master WHERE booking_id=?", bookingId);
        assertEquals(STORE, ((Number) master.get("store_id")).longValue());
        assertEquals("13800001001", master.get("customer_phone"), "客人手机号必须从咨询带过来");
        assertEquals(LocalDate.parse(tomorrow()),
                ((java.sql.Date) master.get("booking_date")).toLocalDate());
        assertEquals(2, ((Number) master.get("table_count")).intValue());

        assertEquals(2, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_table WHERE booking_id=?", Long.class, bookingId));

        Map<String, Object> after = jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", id);
        assertEquals("converted", after.get("status"));
        assertEquals(bookingId, after.get("booking_id"), "咨询必须回填 booking_id 才追踪得到");
        assertNotNull(after.get("handled_time"));
    }

    @Test @Order(3)
    @DisplayName("四类拒绝：缺桌台 / 跨店桌台 / 已占桌台 / 过去日期，全部零副作用")
    void fourRejections() {
        long id = inquiry(STORE, "13800001002", "pending");
        int mine = table(STORE), foreign = table(OTHER_STORE);

        // 缺桌台——系统绝不自动分配
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), "18:30", null), "指定桌台");
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), "18:30", List.of()), "指定桌台");
        // 跨店桌台
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), "18:30", List.of(foreign)), "不属于该咨询所在门店");
        // 过去日期
        rejectedWithoutSideEffect(id, convertBody(LocalDate.now().minusDays(1).toString(), "18:30",
                List.of(mine)), "不能早于今天");
        // 缺时间
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), null, List.of(mine)), "明确预订日期与到店时间");

        // 已占桌台：先用另一条咨询把桌占掉
        long other = inquiry(STORE, "13800001003", "pending");
        assertEquals(200, controller.convert(other,
                convertBody(tomorrow(), "19:00", List.of(mine))).getCode());
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), "18:30", List.of(mine)), "已被占用");
    }

    @Test @Order(4)
    @DisplayName("已拒绝的咨询不能转正式预订")
    void rejectedInquiryCannotConvert() {
        long id = inquiry(STORE, "13800001004", "rejected");
        rejectedWithoutSideEffect(id, convertBody(tomorrow(), "18:30", List.of(table(STORE))),
                "已被拒绝");
    }

    @Test @Order(5)
    @DisplayName("重复确认幂等：返回同一个 booking_id，不建第二张单")
    void repeatConfirmIsIdempotent() {
        long id = inquiry(STORE, "13800001005", "pending");
        int t = table(STORE);
        var first = controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t)));
        String bookingId = (String) first.getData().get("bookingId");
        long masters = count("booking_master"), tables = count("booking_table");

        var again = controller.convert(id, convertBody(tomorrow(), "20:00", List.of(t)));
        assertEquals(200, again.getCode());
        assertEquals(bookingId, again.getData().get("bookingId"), "重复确认必须返回同一张单");
        assertTrue((Boolean) again.getData().get("replayed"));
        assertEquals(masters, count("booking_master"), "重复确认不得建第二张单");
        assertEquals(tables, count("booking_table"));
    }

    @Test @Order(6)
    @DisplayName("并发确认同一条咨询：只生成一张正式预订")
    void concurrentConfirmCreatesOnlyOne() throws Exception {
        long id = inquiry(STORE, "13800001006", "pending");
        int t = table(STORE);
        long before = count("booking_master");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                // UserContext 是 ThreadLocal，线程池里的线程拿不到主线程的身份，
                // 加了鉴权之后不补这一句会直接 401，一单都建不出来。
                UserContext.set(new UserContext.CurrentUser(1L, STORE, "store_manager", "synthetic"));
                gate.await();
                try {
                    var r = controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t)));
                    return r.getCode() == 200 ? (String) r.getData().get("bookingId") : "REJECTED";
                } catch (Exception e) {
                    return "ERROR";
                } finally {
                    UserContext.clear();
                }
            }));
        }
        gate.countDown();
        Set<String> results = new HashSet<>();
        for (Future<String> f : futures) results.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(before + 1, count("booking_master"), "并发确认生成了不止一张单：" + results);
        assertEquals(1, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE id=? AND booking_id IS NOT NULL", Long.class, id));
    }

    @Test @Order(7)
    @DisplayName("中途失败全部回滚：不留半张预订单，咨询不被改动")
    void failureRollsBackEverything() {
        long id = inquiry(STORE, "13800001007", "pending");
        int t = table(STORE);
        long masters = count("booking_master"), tables = count("booking_table");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", id);

        // 让 booking_table 的插入必然失败：临时把表改名，插入会直接报表不存在。
        // 不用改列宽——已有行的 booking_id 就超长，ALTER 自己会先失败。
        jdbc.execute("ALTER TABLE booking_table RENAME TO booking_table_tmp_rollback");
        try {
            assertThrows(Exception.class,
                    () -> controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t))),
                    "台位插入本应失败");
        } finally {
            jdbc.execute("ALTER TABLE booking_table_tmp_rollback RENAME TO booking_table");
        }

        assertEquals(masters, count("booking_master"), "预订单没有随台位一起回滚");
        assertEquals(tables, count("booking_table"));
        assertEquals(before, jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", id),
                "回滚后咨询必须原样，且不得被删除");
    }

    @Test @Order(8)
    @DisplayName("客人手机号能查到这张正式预订，且不会串到别人的单")
    void customerPhoneFindsTheBooking() {
        long id = inquiry(STORE, "13800001008", "pending");
        var result = controller.convert(id, convertBody(tomorrow(), "17:30", List.of(table(STORE))));
        String bookingId = (String) result.getData().get("bookingId");

        List<Map<String, Object>> mine = jdbc.queryForList(
                "SELECT booking_id, booking_date, customer_phone FROM booking_master WHERE customer_phone=?",
                "13800001008");
        assertEquals(1, mine.size(), "按手机号应恰好查到自己那一张");
        assertEquals(bookingId, mine.get(0).get("booking_id"));

        assertTrue(jdbc.queryForList(
                "SELECT booking_id FROM booking_master WHERE customer_phone=?", "13900009999").isEmpty(),
                "无关手机号不该查到任何单");
    }

    @Test @Order(9)
    @DisplayName("门店取咨询自身的 store_id，请求体里塞 storeId 不起作用")
    void storeIdComesFromInquiryOnly() {
        long id = inquiry(OTHER_STORE, "13800001009", "pending");
        int foreignTable = table(OTHER_STORE);
        Map<String, Object> body = convertBody(tomorrow(), "18:00", List.of(foreignTable));
        body.put("storeId", STORE);      // 试图把二店的咨询转成一店的预订

        // 二店的咨询必须由二店员工来转——加了鉴权之后，一店员工做这件事本来就该被拒。
        // 本用例要验的是「门店取自咨询而不是请求体」，所以换成合法身份继续验那个断言。
        var result = as(9L, OTHER_STORE, "store_manager", () -> controller.convert(id, body));
        assertEquals(200, result.getCode(), String.valueOf(result.getMessage()));
        assertEquals(OTHER_STORE, (long) jdbc.queryForObject(
                "SELECT store_id FROM booking_master WHERE booking_id=?", Long.class,
                result.getData().get("bookingId")),
                "门店被请求体覆盖了——必须只认咨询自身的 store_id");
    }

    // ==================== CL-OPS-INQUIRY-CONVERT-AUTH-09 ====================

    @Test @Order(10)
    @DisplayName("别店员工不能转本店咨询：403 且零副作用")
    void foreignStoreStaffCannotConvert() {
        long id = inquiry(STORE, "13800002001", "pending");
        int t = table(STORE);
        long masters = count("booking_master"), tables = count("booking_table");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", id);

        var result = as(9L, OTHER_STORE, "store_manager",
                () -> controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t))));
        assertEquals(403, result.getCode(), "别店员工本应被拒：" + result.getMessage());

        assertEquals(masters, count("booking_master"), "越权请求不得建单");
        assertEquals(tables, count("booking_table"), "越权请求不得占台");
        assertEquals(before, jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", id),
                "越权请求不得改动咨询");
    }

    @Test @Order(11)
    @DisplayName("越权的幂等重放同样被挡住，且不泄露 bookingId")
    void foreignStaffReplayLeaksNothing() {
        long id = inquiry(STORE, "13800002002", "pending");
        int t = table(STORE);
        // 本店员工先正常转一单
        var ok = controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t)));
        assertEquals(200, ok.getCode());
        String bookingId = (String) ok.getData().get("bookingId");
        assertNotNull(bookingId);

        // 别店员工用同一条咨询重放：原来这里会直接把原单号还回去
        var denied = as(9L, OTHER_STORE, "store_manager",
                () -> controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t))));
        assertEquals(403, denied.getCode(), "越权重放本应被拒");
        assertNull(denied.getData(), "越权重放不得返回任何数据");
        assertFalse(String.valueOf(denied.getMessage()).contains(bookingId),
                "提示里泄露了真实单号：" + denied.getMessage());
    }

    @Test @Order(12)
    @DisplayName("提示不区分「咨询不存在」与「不是你门店的」，避免被当探测器")
    void notFoundAndForeignShareOneMessage() {
        long mine = inquiry(STORE, "13800002003", "pending");
        var foreign = as(9L, OTHER_STORE, "store_manager",
                () -> controller.convert(mine, convertBody(tomorrow(), "18:30", List.of(table(STORE)))));
        var missing = as(9L, OTHER_STORE, "store_manager",
                () -> controller.convert(999999L, convertBody(tomorrow(), "18:30", List.of(table(STORE)))));
        // 不存在的走 404，别店的走 403，但**文案一致**，不告诉对方到底是哪种
        assertEquals("咨询不存在或不属于当前门店", foreign.getMessage());
        assertTrue(String.valueOf(missing.getMessage()).contains("咨询不存在"), missing.getMessage());
    }

    @Test @Order(13)
    @DisplayName("未登录直接 401，且零副作用")
    void anonymousRejected() {
        long id = inquiry(STORE, "13800002004", "pending");
        long masters = count("booking_master");
        var result = as(null, null, null,
                () -> controller.convert(id, convertBody(tomorrow(), "18:30", List.of(table(STORE)))));
        assertEquals(401, result.getCode(), result.getMessage());
        assertEquals(masters, count("booking_master"));
    }

    @Test @Order(14)
    @DisplayName("加了鉴权之后，并发仍然只生成一单")
    void concurrencyStillOneAfterAuth() throws Exception {
        long id = inquiry(STORE, "13800002005", "pending");
        int t = table(STORE);
        long before = count("booking_master");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            fs.add(pool.submit(() -> {
                UserContext.set(new UserContext.CurrentUser(1L, STORE, "store_manager", "synthetic"));
                try {
                    gate.await();
                    return controller.convert(id, convertBody(tomorrow(), "18:30", List.of(t))).getCode();
                } catch (Exception e) {
                    return -1;
                } finally {
                    UserContext.clear();
                }
            }));
        }
        gate.countDown();
        for (Future<Integer> f : fs) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(before + 1, count("booking_master"), "加鉴权后并发生成了不止一单");
    }
}
