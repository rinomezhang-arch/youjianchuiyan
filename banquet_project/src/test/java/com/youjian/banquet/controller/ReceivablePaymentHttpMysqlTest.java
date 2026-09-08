package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.aop.*;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.ReceivablePaymentService;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * CL-OPS-RECEIVABLE-PAYMENT-01：应收创建 → 部分收款 → 收清 的真实闭环。
 * <p>
 * 真实 JWT → 拦截器与门店切面 → MockMvc HTTP → 服务事务 → MySQL → GET 回读。
 * 每个用例一套独立随机 schema，<b>全部保留、不清表不 DROP</b>，schema 名带用例名便于复核。
 * 真实执行本任务的迁移脚本，历史合成记录不被改动。
 * <p>
 * <b>不写生产库、不执行真实收付款。</b>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReceivablePaymentHttpMysqlTest {

    private static final String HOST = "jdbc:mysql://127.0.0.1:13317/";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    String schema, secret;
    AnnotationConfigApplicationContext context;
    EntityManagerFactory emf;
    JdbcTemplate jdbc;
    MockMvc mvc;
    ReceivablePaymentService service;
    ObjectMapper json = new ObjectMapper();

    @Configuration @EnableTransactionManagement @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class Wiring { }

    @BeforeEach
    void setup(TestInfo info) throws Exception {
        assertNull(UserContext.get());
        schema = "recv_pay_" + System.currentTimeMillis() + "_"
                + info.getTestMethod().map(java.lang.reflect.Method::getName).orElse("case");
        if (schema.length() > 60) schema = schema.substring(0, 60);
        new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        DataSource ds = new DriverManagerDataSource(HOST + schema + OPTS, "root", "");
        jdbc = new JdbcTemplate(ds);

        // 分两步，顺序是刻意的：
        // 先只用两张财务实体建表，随后**由真实迁移**创建 receivable_payment_request 并追加列，
        // 最后才挂上包含登记实体的正式 EMF。
        // 若一开始就让 Hibernate 建登记表，迁移的 CREATE TABLE IF NOT EXISTS 形同虚设，
        // 后面的结构校验验的是"实体对实体"，根本发现不了实体与迁移的列类型分叉。
        var boot = new LocalContainerEntityManagerFactoryBean();
        boot.setDataSource(ds);
        var bootAdapter = new HibernateJpaVendorAdapter();
        bootAdapter.setGenerateDdl(true);
        boot.setJpaVendorAdapter(bootAdapter);
        Properties bootProps = new Properties();
        bootProps.setProperty("hibernate.hbm2ddl.auto", "update");
        boot.setJpaProperties(bootProps);
        boot.setPackagesToScan("com.youjian.banquet.entity");
        boot.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            pui.addManagedClassName(FinanceReceivable.class.getName());
            pui.addManagedClassName(FinancePaymentRecord.class.getName());
        });
        boot.afterPropertiesSet();
        Objects.requireNonNull(boot.getObject()).close();

        prepareFixturesAndMigration();

        var bean = new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(ds);
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        bean.setJpaVendorAdapter(adapter);
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        bean.setJpaProperties(props);
        bean.setPackagesToScan("com.youjian.banquet.entity");
        bean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            pui.addManagedClassName(FinanceReceivable.class.getName());
            pui.addManagedClassName(FinancePaymentRecord.class.getName());
            pui.addManagedClassName(ReceivablePaymentRequest.class.getName());
        });
        bean.afterPropertiesSet();
        emf = Objects.requireNonNull(bean.getObject());
        var factory = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
        var requests = factory.getRepository(ReceivablePaymentRequestRepository.class);


        secret = UUID.randomUUID() + "" + UUID.randomUUID();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("synthetic-only", Map.of("jwt.secret", secret)));
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(PlatformTransactionManager.class, () -> new JpaTransactionManager(emf));
        context.registerBean(ReceivablePaymentRequestRepository.class, () -> requests);
        // FinanceController 还依赖应付服务（应付那批已整合进基线），但它与本任务这六个方法无关。
        // 这里用替身顶上，避免把应付的实体与表拖进应收的测试库；应付本身另有 57 项回归覆盖。
        // 直接注册成单例：用 registerBean 的话 Spring 仍会对替身做字段注入，
        // 反而把应付那一串仓储依赖拖进来。
        context.getBeanFactory().registerSingleton("financePayableService",
                org.mockito.Mockito.mock(com.youjian.banquet.service.FinancePayableService.class));
        context.register(Wiring.class, ReceivablePaymentService.class, FinanceController.class,
                JwtAuthInterceptor.class, StoreDataScopeAspect.class, AuditLogAspect.class);
        context.refresh();
        service = context.getBean(ReceivablePaymentService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(FinanceController.class))
                .addInterceptors(context.getBean(JwtAuthInterceptor.class)).build();
    }

    /** 父表夹具 + 历史合成记录 + 真实执行迁移。顺序：夹具先于迁移，迁移建登记表。 */
    private void prepareFixturesAndMigration() throws Exception {
        // 关联校验用到的父表：只建被测流程真正查询的列
        jdbc.execute("CREATE TABLE customer_master(customer_id INT PRIMARY KEY, store_id BIGINT, "
                + "customer_name VARCHAR(100))");
        jdbc.execute("CREATE TABLE booking_master(booking_id VARCHAR(40) PRIMARY KEY, store_id BIGINT, "
                + "customer_name VARCHAR(100))");
        // is_active 与生产迁移同口径：int DEFAULT 1，1=启用 0=停用
        jdbc.execute("CREATE TABLE finance_account(account_id BIGINT PRIMARY KEY, store_id BIGINT, "
                + "account_name VARCHAR(100), is_active INT DEFAULT 1)");
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(60),"
                + "action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        jdbc.update("INSERT INTO customer_master VALUES (11,1,'合成客户一')");
        jdbc.update("INSERT INTO customer_master VALUES (22,2,'别店客户')");
        jdbc.update("INSERT INTO booking_master VALUES ('BK-SYN-1',1,'合成客户一')");
        jdbc.update("INSERT INTO booking_master VALUES ('BK-SYN-2',2,'别店客户')");
        jdbc.update("INSERT INTO finance_account VALUES (101,1,'合成收款账户',1)");
        jdbc.update("INSERT INTO finance_account VALUES (202,2,'别店账户',1)");
        jdbc.update("INSERT INTO finance_account VALUES (103,1,'本店已停用账户',0)");
        jdbc.update("INSERT INTO finance_account VALUES (104,1,'启用状态为空的历史账户',NULL)");

        // 迁移前先塞一条历史合成收款，用来证明迁移不改存量
        jdbc.update("INSERT INTO finance_payment_record(store_id,payment_no,payment_date,amount,"
                + "payment_method,operator_name,created_at) VALUES (1,'PAY-HIST-1',?,88.88,'cash','历史操作人',NOW())",
                LocalDate.of(2026, 1, 1));

        runMigration();
    }

    /** 真实执行本任务的迁移脚本，不用手写 DDL 冒充。 */
    private void runMigration() throws Exception {
        String sql = Files.readString(Path.of("..", "scripts", "migrations", "receivable_payment_request_v1.sql"));
        for (String stmt : sql.split(";")) {
            String one = stmt.replaceAll("(?m)^--.*$", "").trim();
            if (!one.isEmpty()) jdbc.execute(one);
        }
    }

    @AfterEach
    void close() {
        try { assertNull(UserContext.get()); }
        finally {
            if (context != null) context.close();
            if (emf != null) emf.close();
            System.out.println("测试库保留以便复核：" + schema);
        }
    }

    String token(long store, String role) {
        return Jwts.builder().subject("synthetic_manager").claim("staffId", 9L).claim("storeId", store)
                .claim("role", role).expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    record Reply(int status, JsonNode body) { }

    Reply post(String path, Map<String, Object> body, String bearer) throws Exception {
        var req = MockMvcRequestBuilders.post(path).contentType("application/json")
                .content(json.writeValueAsString(body));
        if (bearer != null) req.header("Authorization", "Bearer " + bearer);
        var response = mvc.perform(req).andReturn().getResponse();
        assertNull(UserContext.get(), "请求结束后身份上下文未清理");
        return new Reply(response.getStatus(), readBody(response));
    }

    /** MockMvc 默认用 ISO-8859-1 解码响应体，中文断言会全部落空；这里按 UTF-8 读原始字节。 */
    JsonNode readBody(org.springframework.mock.web.MockHttpServletResponse response) throws Exception {
        return json.readTree(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    Reply get(String path, String bearer) throws Exception {
        var req = MockMvcRequestBuilders.get(path);
        if (bearer != null) req.header("Authorization", "Bearer " + bearer);
        var response = mvc.perform(req).andReturn().getResponse();
        assertNull(UserContext.get());
        return new Reply(response.getStatus(), readBody(response));
    }

    Map<String, Object> receivableBody(String total, String key) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("storeId", 1);
        b.put("customerId", 11);
        b.put("customerName", "合成客户一");
        b.put("bookingId", "BK-SYN-1");
        b.put("totalAmount", total);
        b.put("receivableDate", "2026-03-01");
        b.put("dueDate", "2026-03-31");
        b.put("requestId", key);
        return b;
    }

    Map<String, Object> paymentBody(Long receivableId, String amount, String key) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("storeId", 1);
        b.put("receivableId", receivableId);
        b.put("amount", amount);
        b.put("paymentDate", "2026-03-05");
        b.put("paymentMethod", "cash");
        b.put("accountId", 101);
        b.put("requestId", key);
        return b;
    }

    /** 金额守恒：total = received + pending，且已收等于其名下收款流水合计。 */
    void conserved(long receivableId, String total, String received, String status, int payments) {
        var r = jdbc.queryForMap("SELECT * FROM finance_receivable WHERE receivable_id=?", receivableId);
        BigDecimal t = (BigDecimal) r.get("total_amount");
        BigDecimal rec = (BigDecimal) r.get("received_amount");
        BigDecimal pend = (BigDecimal) r.get("pending_amount");
        assertEquals(0, new BigDecimal(total).compareTo(t), "总额不符");
        assertEquals(0, new BigDecimal(received).compareTo(rec), "已收不符");
        assertEquals(0, t.compareTo(rec.add(pend)), "total = received + pending 不守恒");
        assertEquals(status, r.get("status"), "状态不符");
        assertEquals(payments, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE receivable_id=?", Integer.class, receivableId));
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM finance_payment_record WHERE receivable_id=?",
                BigDecimal.class, receivableId);
        assertEquals(0, rec.compareTo(sum), "已收与收款流水合计不一致");
    }

    // ==================== 主闭环 ====================

    @Test @Order(1)
    @DisplayName("应收创建→部分收款→收清→列表与详情回读，全程金额守恒、状态正确")
    void fullLoopFromCreationToFullyPaid() throws Exception {
        String actor = token(1, "store_manager");

        Reply created = post("/api/finance/receivable", receivableBody("1000.00", "REQ-R1"), actor);
        assertEquals(200, created.status(), created.body().toString());
        long id = created.body().path("data").path("receivableId").asLong();
        assertFalse(created.body().path("data").path("replayed").asBoolean());
        conserved(id, "1000.00", "0.00", "unpaid", 0);

        // 部分收款
        Reply part = post("/api/finance/payment", paymentBody(id, "300.00", "REQ-P1"), actor);
        assertEquals(200, part.status(), part.body().toString());
        conserved(id, "1000.00", "300.00", "partial", 1);

        // 再收一部分
        assertEquals(200, post("/api/finance/payment", paymentBody(id, "200.50", "REQ-P2"), actor).status());
        conserved(id, "1000.00", "500.50", "partial", 2);

        // 收清
        assertEquals(200, post("/api/finance/payment", paymentBody(id, "499.50", "REQ-P3"), actor).status());
        conserved(id, "1000.00", "1000.00", "paid", 3);

        // 列表回读
        Reply list = get("/api/finance/receivable", actor);
        assertEquals(200, list.status());
        assertEquals(1, list.body().path("data").size());
        assertEquals("paid", list.body().path("data").get(0).path("status").asText());

        // 详情回读：应收本体 + 名下三笔流水
        Reply detail = get("/api/finance/receivable?id=" + id, actor);
        assertEquals(200, detail.status());
        assertEquals(3, detail.body().path("data").path("payments").size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                new BigDecimal(detail.body().path("data").path("received_amount").asText())));

        // 收款列表回读（含迁移前那条历史记录）
        Reply payments = get("/api/finance/payment", actor);
        assertEquals(200, payments.status());
        assertEquals(4, payments.body().path("data").size(), "应含 3 笔新收款 + 1 条历史记录");
    }

    // ==================== 幂等与并发 ====================

    @Test @Order(2)
    @DisplayName("同键同参数重发：应收与收款都只落一条，回执可核对")
    void sameKeyReplaysForBothOperations() throws Exception {
        String actor = token(1, "store_manager");
        Reply first = post("/api/finance/receivable", receivableBody("500.00", "REQ-R"), actor);
        long id = first.body().path("data").path("receivableId").asLong();
        Reply again = post("/api/finance/receivable", receivableBody("500.00", "REQ-R"), actor);
        assertEquals(200, again.status());
        assertTrue(again.body().path("data").path("replayed").asBoolean());
        assertEquals(id, again.body().path("data").path("receivableId").asLong());
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(*) FROM finance_receivable", Integer.class));

        post("/api/finance/payment", paymentBody(id, "100.00", "REQ-P"), actor);
        Reply payAgain = post("/api/finance/payment", paymentBody(id, "100.00", "REQ-P"), actor);
        assertTrue(payAgain.body().path("data").path("replayed").asBoolean());
        conserved(id, "500.00", "100.00", "partial", 1);
    }

    @Test @Order(3)
    @DisplayName("同键改金额：409，且原记录与余额分文未动")
    void sameKeyChangedAmountConflicts() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("500.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();
        post("/api/finance/payment", paymentBody(id, "100.00", "REQ-P"), actor);

        assertEquals(409, post("/api/finance/receivable", receivableBody("900.00", "REQ-R"), actor).status());
        assertEquals(409, post("/api/finance/payment", paymentBody(id, "250.00", "REQ-P"), actor).status());
        conserved(id, "500.00", "100.00", "partial", 1);
    }

    @Test @Order(4)
    @DisplayName("两连接同键并发收款：只生效一次，余额不翻倍")
    void concurrentSameKeyPaymentTakesEffectOnce() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("1000.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();

        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        List<Future<Reply>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return post("/api/finance/payment", paymentBody(id, "400.00", "REQ-SAME"), actor);
            }));
        }
        start.countDown();
        int fresh = 0, handled = 0;
        for (var f : futures) {
            Reply r = f.get(30, TimeUnit.SECONDS);
            if (r.status() == 200 && !r.body().path("data").path("replayed").asBoolean()) fresh++;
            else if (r.status() == 200 || r.status() == 409) handled++;
            else fail("并发下出现意外状态码 " + r.status() + "：" + r.body());
        }
        pool.shutdown();
        assertEquals(1, fresh, "同一个 key 生效了两次");
        assertEquals(1, handled);
        conserved(id, "1000.00", "400.00", "partial", 1);
    }

    @Test @Order(5)
    @DisplayName("两连接不同键并发收同一应收：两笔都生效，总额守恒且不超收")
    void concurrentDifferentKeysBothSettleAndConserve() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("1000.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();

        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        List<Future<Reply>> futures = new ArrayList<>();
        for (String key : List.of("REQ-A", "REQ-B")) {
            futures.add(pool.submit(() -> {
                start.await();
                return post("/api/finance/payment", paymentBody(id, "300.00", key), actor);
            }));
        }
        start.countDown();
        for (var f : futures) assertEquals(200, f.get(30, TimeUnit.SECONDS).status());
        pool.shutdown();
        conserved(id, "1000.00", "600.00", "partial", 2);
    }

    // ==================== 负向 ====================

    @Test @Order(6)
    @DisplayName("超额收款拒绝：不生成负余额，原余额不变")
    void overpaymentRejected() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("100.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();
        post("/api/finance/payment", paymentBody(id, "60.00", "REQ-P1"), actor);

        Reply over = post("/api/finance/payment", paymentBody(id, "50.00", "REQ-P2"), actor);
        assertEquals(400, over.status());
        assertTrue(over.body().path("message").asText().contains("超过"), over.body().toString());
        conserved(id, "100.00", "60.00", "partial", 1);
        assertTrue(jdbc.queryForObject("SELECT pending_amount FROM finance_receivable WHERE receivable_id=?",
                BigDecimal.class, id).signum() >= 0, "出现了负余额");
    }

    @Test @Order(7)
    @DisplayName("非法金额一律拒绝且零写入")
    void invalidAmountsRejected() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("100.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();
        for (String bad : new String[]{"0", "-1", "1.234"}) {
            Reply r = post("/api/finance/payment", paymentBody(id, bad, "REQ-" + bad), actor);
            assertEquals(400, r.status(), "金额 " + bad + " 未被拒绝");
        }
        conserved(id, "100.00", "0.00", "unpaid", 0);
    }

    @Test @Order(8)
    @DisplayName("引用必须真实且同店：不存在与别店的应收/客户/预订/账户全部拒绝")
    void referencesMustBeRealAndSameStore() throws Exception {
        String actor = token(1, "store_manager");

        // 不存在的应收
        assertEquals(400, post("/api/finance/payment", paymentBody(999999L, "10.00", "REQ-X1"), actor).status());

        // 别店客户 / 别店预订 / 别店账户
        Map<String, Object> b = receivableBody("100.00", "REQ-X2");
        b.put("customerId", 22);
        assertEquals(400, post("/api/finance/receivable", b, actor).status());

        b = receivableBody("100.00", "REQ-X3");
        b.put("bookingId", "BK-SYN-2");
        assertEquals(400, post("/api/finance/receivable", b, actor).status());

        long id = post("/api/finance/receivable", receivableBody("100.00", "REQ-OK"), actor)
                .body().path("data").path("receivableId").asLong();
        Map<String, Object> pay = paymentBody(id, "10.00", "REQ-X4");
        pay.put("accountId", 202);
        assertEquals(400, post("/api/finance/payment", pay, actor).status());

        // 只应留下那张合法应收，且没有任何收款
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(*) FROM finance_receivable", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE receivable_id IS NOT NULL", Integer.class));
    }

    @Test @Order(9)
    @DisplayName("无来源手工收款：必须写明类别与业务说明，齐全才放行")
    void manualPaymentNeedsCategoryAndNote() throws Exception {
        String actor = token(1, "store_manager");
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("storeId", 1);
        base.put("amount", "50.00");
        base.put("paymentMethod", "cash");
        base.put("requestId", "REQ-M1");

        // 只有金额：拒绝
        Reply noCategory = post("/api/finance/payment", base, actor);
        assertEquals(400, noCategory.status());
        assertTrue(noCategory.body().path("message").asText().contains("类别"), noCategory.body().toString());

        // 有类别没说明：仍拒绝
        Map<String, Object> onlyCategory = new LinkedHashMap<>(base);
        onlyCategory.put("category", "散客现金");
        onlyCategory.put("requestId", "REQ-M2");
        Reply noRemark = post("/api/finance/payment", onlyCategory, actor);
        assertEquals(400, noRemark.status());
        assertTrue(noRemark.body().path("message").asText().contains("说明"), noRemark.body().toString());

        // 齐全：放行，并落库类别与操作人
        Map<String, Object> full = new LinkedHashMap<>(onlyCategory);
        full.put("remark", "3 号桌散客现金结账");
        full.put("requestId", "REQ-M3");
        Reply ok = post("/api/finance/payment", full, actor);
        assertEquals(200, ok.status(), ok.body().toString());
        long pid = ok.body().path("data").path("paymentId").asLong();
        var row = jdbc.queryForMap("SELECT * FROM finance_payment_record WHERE payment_id=?", pid);
        assertEquals("散客现金", row.get("payment_category"));
        assertEquals("synthetic_manager", row.get("operator_name"), "操作人未取自登录身份");
        assertNull(row.get("receivable_id"), "无来源收款不应凭空挂到某条应收上");
    }

    // ==================== 身份与门店 ====================

    @Test @Order(10)
    @DisplayName("无 JWT 401；店长跨店 403；总经理必须显式指定门店")
    void identityAndStoreGuards() throws Exception {
        assertEquals(401, post("/api/finance/receivable", receivableBody("100.00", "REQ-N"), null).status());
        assertEquals(401, get("/api/finance/receivable", null).status());

        String storeOne = token(1, "store_manager");
        Map<String, Object> otherStore = receivableBody("100.00", "REQ-C");
        otherStore.put("storeId", 2);
        assertEquals(403, post("/api/finance/receivable", otherStore, storeOne).status());
        assertEquals(403, get("/api/finance/receivable?storeId=2", storeOne).status());

        String gm = token(0, "gm");
        Map<String, Object> gmNoStore = receivableBody("100.00", "REQ-G");
        gmNoStore.remove("storeId");
        Reply gmReply = post("/api/finance/receivable", gmNoStore, gm);
        assertEquals(400, gmReply.status(), gmReply.body().toString());

        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM finance_receivable", Integer.class));
    }

    // ==================== 删除拒绝与历史留存 ====================

    @Test @Order(11)
    @DisplayName("删除应收与收款一律 409，记录仍在")
    void deletionRejectedAndHistoryKept() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("100.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();
        long pid = post("/api/finance/payment", paymentBody(id, "10.00", "REQ-P"), actor)
                .body().path("data").path("paymentId").asLong();

        var delR = mvc.perform(delete("/api/finance/receivable/" + id)
                .header("Authorization", "Bearer " + actor)).andReturn().getResponse();
        assertEquals(409, delR.getStatus(), "删除应收应返回 409");
        assertEquals(409, readBody(delR).path("code").asInt());

        var delP = mvc.perform(delete("/api/finance/payment/" + pid)
                .header("Authorization", "Bearer " + actor)).andReturn().getResponse();
        assertEquals(409, delP.getStatus(), "删除收款应返回 409");
        assertEquals(409, readBody(delP).path("code").asInt());

        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_receivable WHERE receivable_id=?", Integer.class, id));
        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=?", Integer.class, pid));
        conserved(id, "100.00", "10.00", "partial", 1);
    }

    // ==================== 迁移与历史 ====================

    @Test @Order(12)
    @DisplayName("迁移真实执行后：历史合成收款一字未改，新增列为 NULL")
    void migrationKeepsHistoryUntouched() {
        var row = jdbc.queryForMap("SELECT * FROM finance_payment_record WHERE payment_no='PAY-HIST-1'");
        assertEquals(0, new BigDecimal("88.88").compareTo((BigDecimal) row.get("amount")));
        assertEquals("历史操作人", row.get("operator_name"));
        assertNull(row.get("payment_category"), "迁移不应回填历史类别");
        assertNull(row.get("receivable_id"), "迁移不应给历史记录编造来源");

        // 新增的唯一键确实建出来了
        Integer uk = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='finance_receivable' AND INDEX_NAME='uk_finance_receivable_id_store'",
                Integer.class);
        assertTrue(uk != null && uk > 0, "同门店唯一键未建立");

        // 本迁移刻意没有加收款→应收的外键（历史存在孤儿行），确认确实没加
        Integer fk = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='finance_payment_record' AND CONSTRAINT_TYPE='FOREIGN KEY'",
                Integer.class);
        assertEquals(0, fk, "外键被提前加上了，历史孤儿行会让生产部署失败");
    }

    // ==================== 失败回滚 ====================

    @Test @Order(13)
    @DisplayName("收款写入失败：应收余额一起回滚，不出现只改余额没有流水的账")
    void paymentFailureRollsBackReceivable() throws Exception {
        String actor = token(1, "store_manager");
        long id = post("/api/finance/receivable", receivableBody("100.00", "REQ-R"), actor)
                .body().path("data").path("receivableId").asLong();

        // 把收款表的备注列压到 1 个字符，用数据库层的真实失败逼出回滚
        jdbc.execute("ALTER TABLE finance_payment_record MODIFY remark VARCHAR(1)");
        try {
            Map<String, Object> pay = paymentBody(id, "10.00", "REQ-FAIL");
            pay.put("remark", "这条备注一定超长");
            Reply r = post("/api/finance/payment", pay, actor);
            assertNotEquals(200, r.status(), "写入失败却返回了成功");
            conserved(id, "100.00", "0.00", "unpaid", 0);
        } finally {
            jdbc.execute("ALTER TABLE finance_payment_record MODIFY remark VARCHAR(255)");
        }
    }
    // ==================== 指纹缺口的负例 ====================

    @Test @Order(14)
    @DisplayName("同键只改 bookingNo：必须 409，不能当成原样重试返回旧回执")
    void changingBookingNoIsNotAReplay() throws Exception {
        String actor = token(1, "store_manager");
        Map<String, Object> first = receivableBody("100.00", "REQ-BNO");
        first.put("bookingNo", "BN-001");
        Reply created = post("/api/finance/receivable", first, actor);
        assertEquals(200, created.status(), created.body().toString());
        long id = created.body().path("data").path("receivableId").asLong();

        Map<String, Object> changed = receivableBody("100.00", "REQ-BNO");
        changed.put("bookingNo", "BN-002");
        Reply conflict = post("/api/finance/receivable", changed, actor);
        assertEquals(409, conflict.status(), "改了 bookingNo 仍被当成重试：" + conflict.body());

        // 原单的 bookingNo 未被改写，也没有多出第二张
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(*) FROM finance_receivable", Integer.class));
        assertEquals("BN-001", jdbc.queryForObject(
                "SELECT booking_no FROM finance_receivable WHERE receivable_id=?", String.class, id));

        // 收款侧同样
        Map<String, Object> pay = paymentBody(id, "10.00", "REQ-PBNO");
        pay.put("bookingNo", "BN-001");
        assertEquals(200, post("/api/finance/payment", pay, actor).status());
        Map<String, Object> payChanged = paymentBody(id, "10.00", "REQ-PBNO");
        payChanged.put("bookingNo", "BN-999");
        assertEquals(409, post("/api/finance/payment", payChanged, actor).status());
        conserved(id, "100.00", "10.00", "partial", 1);
    }

    @Test @Order(15)
    @DisplayName("字段值含分隔符时不得指纹碰撞：category=A|B,remark=C 与 category=A,remark=B|C 必须区分")
    void separatorInValuesMustNotCollide() throws Exception {
        String actor = token(1, "store_manager");

        Map<String, Object> one = new LinkedHashMap<>();
        one.put("storeId", 1);
        one.put("amount", "50.00");
        one.put("category", "A|B");
        one.put("remark", "C");
        one.put("requestId", "REQ-SEP");
        assertEquals(200, post("/api/finance/payment", one, actor).status());

        // 只是把分隔符两侧的内容挪了个位置，业务含义完全不同，必须判为参数变更
        Map<String, Object> two = new LinkedHashMap<>(one);
        two.put("category", "A");
        two.put("remark", "B|C");
        Reply conflict = post("/api/finance/payment", two, actor);
        assertEquals(409, conflict.status(), "分隔符歧义导致两个不同请求算出同一指纹：" + conflict.body());

        // 只落了第一笔，且类别是第一笔的原值
        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE payment_category IS NOT NULL", Integer.class));
        assertEquals("A|B", jdbc.queryForObject(
                "SELECT payment_category FROM finance_payment_record WHERE payment_category IS NOT NULL",
                String.class));

        // 反向确认：同键同参数仍能正常重放，没有被过度收紧
        Reply replay = post("/api/finance/payment", one, actor);
        assertEquals(200, replay.status());
        assertTrue(replay.body().path("data").path("replayed").asBoolean());
    }

    // ==================== 实体与真实迁移的结构一致性 ====================

    @Test @Order(16)
    @DisplayName("按真实迁移建表后，实体映射通过 Hibernate strict validate")
    void entitiesValidateAgainstRealMigration() {
        // setup 里：两张财务表由实体建出，随后真实执行迁移建 receivable_payment_request 并追加列。
        // 这里再用 validate 模式起一个 EMF——映射与真实结构对不上就会直接失败。
        // params_hash 若仍是 VARCHAR(64) 而迁移是 CHAR(64)，这条用例会红。
        var bean = new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(new DriverManagerDataSource(HOST + schema + OPTS, "root", ""));
        var adapter = new HibernateJpaVendorAdapter();
        bean.setJpaVendorAdapter(adapter);
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        bean.setJpaProperties(props);
        bean.setPackagesToScan("com.youjian.banquet.entity");
        bean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            pui.addManagedClassName(FinanceReceivable.class.getName());
            pui.addManagedClassName(FinancePaymentRecord.class.getName());
            pui.addManagedClassName(ReceivablePaymentRequest.class.getName());
        });
        bean.afterPropertiesSet();
        var validated = bean.getObject();
        assertNotNull(validated, "结构校验未通过");
        validated.close();

        // 直接核对列类型，避免只依赖 validate 的宽松判定
        String type = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='receivable_payment_request' AND COLUMN_NAME='params_hash'",
                String.class);
        assertEquals("char", type, "params_hash 实际类型与迁移声明不一致");
    }

    // ==================== CL-OPS-RECEIVABLE-RULES-03 ====================
    // Codex 裁定：历史成功请求的同指纹重放用于恢复未知结果，不因后来引用停用而失效；
    // 新收款只能用本店且 is_active=1 的账户。

    @Test @Order(17)
    @DisplayName("收款成功后账户被停用：用原 requestId 重放仍返回原回执，且不新增流水")
    void replaySurvivesAccountDeactivation() throws Exception {
        String actor = token(1, "store_manager");
        Reply created = post("/api/finance/receivable", receivableBody("100.00", "REQ-RV-DEACT"), actor);
        assertEquals(200, created.status(), created.body().toString());
        long id = created.body().path("data").path("receivableId").asLong();

        Map<String, Object> pay = paymentBody(id, "40.00", "REQ-PAY-DEACT");
        Reply first = post("/api/finance/payment", pay, actor);
        assertEquals(200, first.status(), first.body().toString());
        long paymentId = first.body().path("data").path("paymentId").asLong();
        assertFalse(first.body().path("data").path("replayed").asBoolean());

        // 事后把这笔用过的账户停用——模拟"当初合法、后来被停"的真实情形
        jdbc.update("UPDATE finance_account SET is_active=0 WHERE account_id=101");

        Reply replayed = post("/api/finance/payment", pay, actor);
        assertEquals(200, replayed.status(), "账户被停用后原键重放被挡住了：" + replayed.body());
        assertTrue(replayed.body().path("data").path("replayed").asBoolean(), "应判为重放");
        assertEquals(paymentId, replayed.body().path("data").path("paymentId").asLong(), "重放必须拿回同一个主键");

        // 零副作用：只有一笔流水，余额不动
        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE receivable_id=?", Integer.class, id));
        conserved(id, "100.00", "40.00", "partial", 1);
    }

    @Test @Order(18)
    @DisplayName("新 requestId 引用已停用账户：拒绝且零写入")
    void newPaymentRejectsDeactivatedAccount() throws Exception {
        String actor = token(1, "store_manager");
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM finance_payment_record", Long.class);

        Map<String, Object> pay = paymentBody(null, "20.00", "REQ-PAY-DEACT-NEW");
        pay.put("accountId", 103);              // 本店，但 is_active=0
        pay.put("category", "零星收款");
        pay.put("remark", "停用账户测试");
        Reply reply = post("/api/finance/payment", pay, actor);
        assertEquals(400, reply.status(), "停用账户应被拒绝：" + reply.body());

        assertEquals(before, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record", Long.class), "被拒的请求不得留下任何流水");
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM receivable_payment_request WHERE request_id=?",
                Integer.class, "REQ-PAY-DEACT-NEW"), "被拒的请求不得留下幂等登记");
    }

    @Test @Order(19)
    @DisplayName("is_active 为 NULL 的历史账户同样不能用于新收款")
    void newPaymentRejectsNullActiveAccount() throws Exception {
        String actor = token(1, "store_manager");
        Map<String, Object> pay = paymentBody(null, "20.00", "REQ-PAY-NULLACT");
        pay.put("accountId", 104);              // 本店，is_active IS NULL
        pay.put("category", "零星收款");
        pay.put("remark", "空启用状态测试");
        Reply reply = post("/api/finance/payment", pay, actor);
        assertEquals(400, reply.status(), "is_active 为 NULL 应判为不启用：" + reply.body());
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM receivable_payment_request WHERE request_id=?",
                Integer.class, "REQ-PAY-NULLACT"));
    }

    @Test @Order(20)
    @DisplayName("别店账户与本店停用账户给同一句提示，不泄漏他店信息")
    void accountRejectionDoesNotLeakOtherStore() throws Exception {
        String actor = token(1, "store_manager");

        Map<String, Object> foreign = paymentBody(null, "20.00", "REQ-PAY-FOREIGN-ACC");
        foreign.put("accountId", 202);          // 别店账户
        foreign.put("category", "零星收款");
        foreign.put("remark", "别店账户测试");
        Reply a = post("/api/finance/payment", foreign, actor);

        Map<String, Object> missing = paymentBody(null, "20.00", "REQ-PAY-MISSING-ACC");
        missing.put("accountId", 999999);       // 根本不存在
        missing.put("category", "零星收款");
        missing.put("remark", "不存在账户测试");
        Reply b = post("/api/finance/payment", missing, actor);

        Map<String, Object> disabled = paymentBody(null, "20.00", "REQ-PAY-DISABLED-ACC");
        disabled.put("accountId", 103);         // 本店已停用
        disabled.put("category", "零星收款");
        disabled.put("remark", "停用账户提示测试");
        Reply c = post("/api/finance/payment", disabled, actor);

        assertEquals(400, a.status());
        assertEquals(400, b.status());
        assertEquals(400, c.status());
        String ma = a.body().path("message").asText();
        assertEquals(ma, b.body().path("message").asText(), "别店与不存在必须同一句提示");
        assertEquals(ma, c.body().path("message").asText(), "停用与不存在必须同一句提示");
        assertFalse(ma.contains("别店账户"), "提示不得回显他店账户名");
        assertFalse(ma.contains("202"), "提示不得回显他店账户号");
    }

    @Test @Order(21)
    @DisplayName("同 requestId 改账户仍判冲突：重放前置不等于放宽同键改参")
    void changingAccountIsStillConflict() throws Exception {
        String actor = token(1, "store_manager");
        Map<String, Object> pay = paymentBody(null, "30.00", "REQ-PAY-ACC-SWAP");
        pay.put("category", "零星收款");
        pay.put("remark", "换账户冲突测试");
        assertEquals(200, post("/api/finance/payment", pay, actor).status());

        Map<String, Object> swapped = new LinkedHashMap<>(pay);
        swapped.put("accountId", 103);          // 换成另一个账户
        Reply conflict = post("/api/finance/payment", swapped, actor);
        assertEquals(409, conflict.status(), "同键改账户必须 409：" + conflict.body());

        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_payment_record WHERE payment_category=?",
                Integer.class, "零星收款"));
        assertEquals(101L, (long) jdbc.queryForObject(
                "SELECT account_id FROM finance_payment_record WHERE payment_category=?",
                Long.class, "零星收款"), "原单账户不得被改写");
    }

    @Test @Order(22)
    @DisplayName("应收侧同理：引用客户事后改店，用原 requestId 恢复仍返回原回执")
    void receivableReplaySurvivesReferenceChange() throws Exception {
        String actor = token(1, "store_manager");
        Map<String, Object> body = receivableBody("60.00", "REQ-RV-REFCHANGE");
        Reply first = post("/api/finance/receivable", body, actor);
        assertEquals(200, first.status(), first.body().toString());
        long id = first.body().path("data").path("receivableId").asLong();

        // 事后把引用的客户改到别店——当初合法，现在校验不过
        jdbc.update("UPDATE customer_master SET store_id=2 WHERE customer_id=11");
        try {
            Reply replayed = post("/api/finance/receivable", body, actor);
            assertEquals(200, replayed.status(), "引用事后失效挡住了历史恢复：" + replayed.body());
            assertTrue(replayed.body().path("data").path("replayed").asBoolean());
            assertEquals(id, replayed.body().path("data").path("receivableId").asLong());
            assertEquals(1, (int) jdbc.queryForObject(
                    "SELECT COUNT(*) FROM finance_receivable WHERE receivable_no=?",
                    Integer.class, first.body().path("data").path("no").asText()));

            // 但新 key 引用同一个已跨店的客户，必须照样拒绝
            Reply fresh = post("/api/finance/receivable", receivableBody("60.00", "REQ-RV-REFCHANGE-NEW"), actor);
            assertEquals(400, fresh.status(), "新登记引用别店客户应拒绝：" + fresh.body());
        } finally {
            jdbc.update("UPDATE customer_master SET store_id=1 WHERE customer_id=11");
        }
    }
}
