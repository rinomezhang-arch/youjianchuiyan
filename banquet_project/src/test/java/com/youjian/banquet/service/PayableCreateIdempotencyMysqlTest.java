package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.entity.PayableCreateRequest;
import com.youjian.banquet.entity.PayableSettlementRecord;
import com.youjian.banquet.repository.FinancePayableRepository;
import com.youjian.banquet.repository.PayableCreateRequestRepository;
import com.youjian.banquet.repository.PayableSettlementRecordRepository;
import com.youjian.banquet.util.UserContext;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-PAYABLE-CREATE-01 隔离库集成测试。
 * <p>
 * 每个用例一套独立随机 schema，<b>全部保留、不清表不 DROP</b>，便于事后复核当时的真实数据。
 * 真实 JPA、真实事务、真实两连接并发。不读生产凭证、不碰生产库、不写 integration 目录。
 * <p>
 * <b>不涉及任何真实银行付款。</b>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayableCreateIdempotencyMysqlTest {

    private static final String HOST = "jdbc:mysql://127.0.0.1:13317/";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final Long STORE = 1L;
    private static final Long OTHER_STORE = 2L;

    private String schema;
    private AnnotationConfigApplicationContext ctx;
    private JdbcTemplate jdbc;
    private FinancePayableService service;

    @EnableTransactionManagement
    static class TxConfig { }

    @BeforeEach
    void setUp(TestInfo info) {
        schema = "payable_create_" + System.currentTimeMillis() + "_"
                + info.getTestMethod().map(java.lang.reflect.Method::getName).orElse("case");
        if (schema.length() > 60) schema = schema.substring(0, 60);
        new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        DataSource ds = new DriverManagerDataSource(HOST + schema + OPTS, "root", "");
        jdbc = new JdbcTemplate(ds);

        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        emfBean.setJpaVendorAdapter(adapter);
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emfBean.setJpaProperties(props);
        emfBean.setPackagesToScan("com.youjian.banquet.entity");
        emfBean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            pui.addManagedClassName(FinancePayable.class.getName());
            pui.addManagedClassName(PayableCreateRequest.class.getName());
            pui.addManagedClassName(PayableSettlementRecord.class.getName());
        });
        emfBean.afterPropertiesSet();
        EntityManagerFactory emf = Objects.requireNonNull(emfBean.getObject());
        EntityManager sharedEm = SharedEntityManagerCreator.createSharedEntityManager(emf);
        JpaRepositoryFactory factory = new JpaRepositoryFactory(sharedEm);

        FinancePayableRepository payableRepo = factory.getRepository(FinancePayableRepository.class);
        PayableCreateRequestRepository createRepo = factory.getRepository(PayableCreateRequestRepository.class);
        PayableSettlementRecordRepository settlementRepo =
                factory.getRepository(PayableSettlementRecordRepository.class);

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(PlatformTransactionManager.class, () -> new JpaTransactionManager(emf));
        ctx.registerBean(FinancePayableRepository.class, () -> payableRepo);
        ctx.registerBean(PayableCreateRequestRepository.class, () -> createRepo);
        ctx.registerBean(PayableSettlementRecordRepository.class, () -> settlementRepo);
        ctx.register(TxConfig.class);
        ctx.registerBean(FinancePayableService.class);
        ctx.refresh();
        service = ctx.getBean(FinancePayableService.class);
        loginAs(STORE, false);
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
        UserContext.clear();
        System.out.println("测试库保留以便复核：" + schema);
    }

    private void loginAs(Long storeId, boolean allStores) {
        UserContext.set(new UserContext.CurrentUser(9L, storeId, allStores ? "gm" : "store_manager", "zhangxiaoqiu"));
        UserContext.setDataScopeAll(allStores);
    }

    /** 一张待创建的手工应付单；每次都是新对象，避免被上一次调用改过状态。 */
    private FinancePayable draft(Long storeId, String total, String supplier) {
        FinancePayable p = new FinancePayable();
        p.setStoreId(storeId);
        p.setSupplierName(supplier);
        p.setTotalAmount(new BigDecimal(total));
        p.setPayableDate(LocalDate.of(2026, 9, 1));
        p.setRemark("合成测试单据");
        return p;
    }

    /** 把服务的日历基准固定到某天；不改系统时钟。 */
    private void setToday(java.time.LocalDate day) {
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "setClock",
                java.time.Clock.fixed(day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                        java.time.ZoneId.systemDefault()));
    }

    private long payableCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable", Integer.class);
        return n == null ? 0 : n;
    }

    private long requestCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM payable_create_request", Integer.class);
        return n == null ? 0 : n;
    }

    // ==================== 缺 requestId ====================

    @Test
    @Order(1)
    @DisplayName("缺 requestId：可解释错误，说明服务端不代生成，且一张单都不建")
    void missingRequestIdRejected() {
        for (String bad : new String[]{null, "", "   "}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.create(draft(STORE, "1000.00", "供应商甲"), bad));
            assertTrue(e.getMessage().contains("requestId"), "错误没点明 requestId：" + e.getMessage());
            assertTrue(e.getMessage().contains("不会代为生成"), "没说明服务端不代生成：" + e.getMessage());
        }
        assertEquals(0, payableCount());
        assertEquals(0, requestCount());
    }

    // ==================== 同键顺序重复只建一张 ====================

    @Test
    @Order(2)
    @DisplayName("同键同参数重复提交：只建一张单，第二次返回原回执且 replayed=true")
    void sameKeySameParamsCreatesOnce() {
        String req = "REQ-" + UUID.randomUUID();
        FinancePayableService.CreateResult first = service.create(draft(STORE, "1000.00", "供应商甲"), req);
        assertFalse(first.replayed);
        assertNotNull(first.payable.getPayableId());
        assertNotNull(first.payable.getPayableNo());

        FinancePayableService.CreateResult second = service.create(draft(STORE, "1000.00", "供应商甲"), req);
        assertTrue(second.replayed, "第二次没被识别成重试");

        // 回执逐项一致，前端可核对
        assertEquals(req, second.requestId);
        assertEquals(first.payable.getPayableId(), second.payable.getPayableId());
        assertEquals(first.payable.getPayableNo(), second.payable.getPayableNo());

        assertEquals(1, payableCount(), "重复提交建出了第二张单");
        assertEquals(1, requestCount());
        // 原单内容没有被第二次提交改写
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                jdbc.queryForObject("SELECT total_amount FROM finance_payable WHERE payable_id=?",
                        BigDecimal.class, first.payable.getPayableId())));
    }

    // ==================== 同键改参数：明确拒绝且不泄漏 ====================

    @Test
    @Order(3)
    @DisplayName("同键改金额/供应商/日期/备注：一律 409 冲突，不新建也不改原单")
    void sameKeyChangedParamsRejected() {
        String req = "REQ-" + UUID.randomUUID();
        FinancePayableService.CreateResult first = service.create(draft(STORE, "1000.00", "供应商甲"), req);

        List<FinancePayable> mutations = new ArrayList<>();
        mutations.add(draft(STORE, "1200.00", "供应商甲"));               // 改金额
        mutations.add(draft(STORE, "1000.00", "供应商乙"));               // 改供应商
        FinancePayable changedDate = draft(STORE, "1000.00", "供应商甲");
        changedDate.setPayableDate(LocalDate.of(2026, 9, 2));
        mutations.add(changedDate);                                        // 改日期
        FinancePayable changedRemark = draft(STORE, "1000.00", "供应商甲");
        changedRemark.setRemark("改过的备注");
        mutations.add(changedRemark);                                      // 改备注

        for (FinancePayable mutated : mutations) {
            FinancePayableService.SettlementConflictException e = assertThrows(
                    FinancePayableService.SettlementConflictException.class,
                    () -> service.create(mutated, req));
            assertTrue(e.getMessage().contains("已创建过"), "冲突提示不够解释：" + e.getMessage());
        }

        assertEquals(1, payableCount(), "同键改参数建出了新单");
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                jdbc.queryForObject("SELECT total_amount FROM finance_payable WHERE payable_id=?",
                        BigDecimal.class, first.payable.getPayableId())), "原单被改写了");
    }

    @Test
    @Order(4)
    @DisplayName("跨店拿同一个 key 探测：冲突提示不含对方门店、金额、供应商和单号")
    void conflictLeaksNothingAboutTheOtherStore() {
        String sharedKey = "REQ-PROBE-" + UUID.randomUUID();
        loginAs(OTHER_STORE, false);
        FinancePayableService.CreateResult theirs =
                service.create(draft(OTHER_STORE, "8888.88", "别店供应商"), sharedKey);

        loginAs(STORE, false);
        FinancePayableService.SettlementConflictException e = assertThrows(
                FinancePayableService.SettlementConflictException.class,
                () -> service.create(draft(STORE, "100.00", "本店供应商"), sharedKey));

        String msg = e.getMessage();
        assertFalse(msg.contains("8888"), "泄漏了对方金额：" + msg);
        assertFalse(msg.contains("别店供应商"), "泄漏了对方供应商：" + msg);
        assertFalse(msg.contains(String.valueOf(theirs.payable.getPayableId())), "泄漏了对方单号：" + msg);
        assertFalse(msg.contains(theirs.payable.getPayableNo()), "泄漏了对方单据编号：" + msg);
        assertFalse(msg.contains(sharedKey), "回显了 requestId：" + msg);

        assertEquals(1, payableCount(), "本店建出了单");
    }

    // ==================== 身份与门店 ====================

    @Test
    @Order(5)
    @DisplayName("无身份、越店创建一律 403，且不留任何痕迹")
    void identityAndStoreGuards() {
        UserContext.clear();
        assertThrows(FinancePayableService.PayableAccessDeniedException.class,
                () -> service.create(draft(STORE, "1000.00", "供应商甲"), "REQ-" + UUID.randomUUID()));

        loginAs(STORE, false);
        assertThrows(FinancePayableService.PayableAccessDeniedException.class,
                () -> service.create(draft(OTHER_STORE, "1000.00", "供应商甲"), "REQ-" + UUID.randomUUID()));

        assertEquals(0, payableCount());
        assertEquals(0, requestCount());
    }

    // ==================== 并发同键只建一张 ====================

    @Test
    @Order(6)
    @DisplayName("两个连接同时用同一 key 创建：只建一张单，另一个是明确的在途冲突而不是底层异常")
    void concurrentSameKeyCreatesOnce() throws Exception {
        String req = "REQ-RACE-" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                loginAs(STORE, false);
                start.await();
                try {
                    return service.create(draft(STORE, "1000.00", "供应商甲"), req);
                } catch (RuntimeException e) {
                    return e;
                } finally {
                    UserContext.clear();
                }
            }));
        }
        start.countDown();
        int ok = 0, handled = 0;
        List<String> unexpected = new ArrayList<>();
        for (Future<Object> f : futures) {
            Object r = f.get(30, TimeUnit.SECONDS);
            if (r instanceof FinancePayableService.CreateResult) ok++;
            else if (r instanceof FinancePayableService.CreateInFlightException
                    || r instanceof FinancePayableService.SettlementConflictException) handled++;
            else unexpected.add(r == null ? "null" : r.getClass().getName() + ": " + ((Throwable) r).getMessage());
        }
        pool.shutdown();

        assertTrue(unexpected.isEmpty(), "出现了既非成功也非明确冲突的结果，等于把底层异常吞成 500：" + unexpected);
        assertEquals(1, ok, "两个连接都建单成功了");
        assertEquals(1, handled);
        assertEquals(1, payableCount(), "并发建出了两张单");
        assertEquals(1, requestCount());

        // 在途冲突之后用同一 key 重试，应当取回原回执
        loginAs(STORE, false);
        FinancePayableService.CreateResult recovered =
                service.create(draft(STORE, "1000.00", "供应商甲"), req);
        assertTrue(recovered.replayed, "重试没有取回原回执");
        assertEquals(1, payableCount());
    }

    // ==================== 创建失败整批回滚 ====================

    @Test
    @Order(7)
    @DisplayName("登记写入失败：刚建的应付单一起回滚，不留无主单据")
    void registrationFailureRollsBackThePayable() {
        // 把登记表的 payable_no 列压到 1 个字符，登记必然写失败 —— 用数据库层的真实失败，
        // 而不是 mock 造异常。失败点在应付单已经 save 之后，才能验证整笔回滚。
        jdbc.execute("ALTER TABLE payable_create_request MODIFY payable_no VARCHAR(1)");
        try {
            assertThrows(Exception.class,
                    () -> service.create(draft(STORE, "1000.00", "供应商甲"), "REQ-" + UUID.randomUUID()));
            assertEquals(0, payableCount(), "登记失败但应付单留了下来 —— 成了无主单据");
            assertEquals(0, requestCount());
        } finally {
            jdbc.execute("ALTER TABLE payable_create_request MODIFY payable_no VARCHAR(50)");
        }
    }

    @Test
    @Order(8)
    @DisplayName("已有历史单据不受新建影响，且新单不会顶掉旧单")
    void existingPayablesUntouched() {
        FinancePayableService.CreateResult older =
                service.create(draft(STORE, "500.00", "老供应商"), "REQ-OLD-" + UUID.randomUUID());
        String olderNo = older.payable.getPayableNo();

        service.create(draft(STORE, "700.00", "新供应商"), "REQ-NEW-" + UUID.randomUUID());

        assertEquals(2, payableCount());
        assertEquals(0, new BigDecimal("500.00").compareTo(
                jdbc.queryForObject("SELECT total_amount FROM finance_payable WHERE payable_no=?",
                        BigDecimal.class, olderNo)), "旧单金额被改了");
        assertEquals("unpaid", jdbc.queryForObject(
                "SELECT status FROM finance_payable WHERE payable_no=?", String.class, olderNo));
    }

    // ==================== 跨日期恢复 ====================

    @Test
    @Order(10)
    @DisplayName("不传日期的请求跨天恢复：仍按重放返回原单，不因服务端补的默认日期变成冲突")
    void undatedRequestRecoversAcrossDays() {
        // 不改系统时钟：改成把已建单据的应付日期改成"前一天"，
        // 等价于"这张单是昨天建的、今天带同一个 key 来恢复"。
        // 指纹若用了服务端补的默认日期，这里必然 409；用调用方原始值（null）才会正确重放。
        String req = "REQ-CROSSDAY-" + UUID.randomUUID();
        FinancePayable undated = draft(STORE, "1000.00", "供应商甲");
        undated.setPayableDate(null);   // 调用方没传日期
        FinancePayableService.CreateResult first = service.create(undated, req);
        assertNotNull(first.payable.getPayableDate(), "服务端应补上默认日期");

        jdbc.update("UPDATE finance_payable SET payable_date=? WHERE payable_id=?",
                LocalDate.now().minusDays(1), first.payable.getPayableId());

        FinancePayable retry = draft(STORE, "1000.00", "供应商甲");
        retry.setPayableDate(null);
        FinancePayableService.CreateResult second = service.create(retry, req);
        assertTrue(second.replayed, "跨天恢复被误判成参数变更");
        assertEquals(first.payable.getPayableId(), second.payable.getPayableId());
        assertEquals(1, payableCount(), "跨天恢复建出了第二张单");
    }

    @Test
    @Order(11)
    @DisplayName("显式改日期仍然是冲突：跨日期修复不能顺带把真正的改日期也放过去")
    void explicitDateChangeStillConflicts() {
        String req = "REQ-DATE-" + UUID.randomUUID();
        FinancePayable dated = draft(STORE, "1000.00", "供应商甲");
        dated.setPayableDate(LocalDate.of(2026, 9, 1));
        service.create(dated, req);

        FinancePayable changed = draft(STORE, "1000.00", "供应商甲");
        changed.setPayableDate(LocalDate.of(2026, 9, 5));
        assertThrows(FinancePayableService.SettlementConflictException.class,
                () -> service.create(changed, req));

        // 到期日同理
        FinancePayable changedDue = draft(STORE, "1000.00", "供应商甲");
        changedDue.setPayableDate(LocalDate.of(2026, 9, 1));
        changedDue.setDueDate(LocalDate.of(2026, 10, 1));
        assertThrows(FinancePayableService.SettlementConflictException.class,
                () -> service.create(changedDue, req));

        assertEquals(1, payableCount());
    }

    // ==================== 边界一：调用方显式单号属于业务参数 ====================

    @Test
    @Order(12)
    @DisplayName("同键改显式单号：409 拒绝，不能拿旧单号的回执糊弄过去")
    void explicitPayableNoChangeConflicts() {
        String req = "REQ-NO-" + UUID.randomUUID();
        FinancePayable first = draft(STORE, "1000.00", "供应商甲");
        first.setPayableNo("PY-EXPLICIT-A");
        FinancePayableService.CreateResult created = service.create(first, req);
        assertEquals("PY-EXPLICIT-A", created.payable.getPayableNo());

        // 换成另一个显式单号：前端若严格核对单号，返回旧号会让它永远对不上而挂住，
        // 所以这里必须是明确冲突，而不是重放。
        FinancePayable renamed = draft(STORE, "1000.00", "供应商甲");
        renamed.setPayableNo("PY-EXPLICIT-B");
        assertThrows(FinancePayableService.SettlementConflictException.class,
                () -> service.create(renamed, req));

        assertEquals(1, payableCount());
        assertEquals("PY-EXPLICIT-A", jdbc.queryForObject(
                "SELECT payable_no FROM finance_payable", String.class), "原单单号被改了");
    }

    @Test
    @Order(13)
    @DisplayName("同键同显式单号重发：正常重放；不传单号时服务端生成的号不进指纹")
    void payableNoFingerprintIsStable() {
        // 显式同号重发 -> 重放
        String reqExplicit = "REQ-NOSAME-" + UUID.randomUUID();
        FinancePayable a = draft(STORE, "1000.00", "供应商甲");
        a.setPayableNo("PY-SAME");
        long id = service.create(a, reqExplicit).payable.getPayableId();
        FinancePayable b = draft(STORE, "1000.00", "供应商甲");
        b.setPayableNo("PY-SAME");
        FinancePayableService.CreateResult replay = service.create(b, reqExplicit);
        assertTrue(replay.replayed);
        assertEquals(id, replay.payable.getPayableId());

        // 不传单号重发 -> 也必须重放：服务端每次生成的新号若进了指纹，这里会次次 409
        String reqAuto = "REQ-NOAUTO-" + UUID.randomUUID();
        FinancePayable c = draft(STORE, "2000.00", "供应商乙");
        long autoId = service.create(c, reqAuto).payable.getPayableId();
        FinancePayable d = draft(STORE, "2000.00", "供应商乙");
        FinancePayableService.CreateResult autoReplay = service.create(d, reqAuto);
        assertTrue(autoReplay.replayed, "服务端生成的单号污染了指纹");
        assertEquals(autoId, autoReplay.payable.getPayableId());
        assertEquals(2, payableCount());
    }

    // ==================== 边界二：跨到期日恢复不被日历默认校验挡住 ====================

    @Test
    @Order(14)
    @DisplayName("首次未传应付日+显式到期日，过了到期日再恢复：仍然重放，不被 400 挡住")
    void recoveryAfterDueDatePassedStillReplays() {
        // 可控时钟推进"今天"，不改系统时钟。
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        java.time.LocalDate day1 = java.time.LocalDate.of(2026, 3, 1);
        setToday(day1);

        String req = "REQ-DUE-" + UUID.randomUUID();
        FinancePayable original = draft(STORE, "1000.00", "供应商甲");
        original.setPayableDate(null);                                  // 没传应付日
        original.setDueDate(java.time.LocalDate.of(2026, 3, 5));        // 显式到期日，当时合法
        FinancePayableService.CreateResult created = service.create(original, req);
        assertEquals(day1, created.payable.getPayableDate(), "服务端应把应付日补成当天");

        // 时间来到到期日之后
        setToday(java.time.LocalDate.of(2026, 3, 20));

        FinancePayable retry = draft(STORE, "1000.00", "供应商甲");
        retry.setPayableDate(null);
        retry.setDueDate(java.time.LocalDate.of(2026, 3, 5));
        FinancePayableService.CreateResult recovered = service.create(retry, req);
        assertTrue(recovered.replayed, "恢复请求被新日历的默认应付日校验挡住了");
        assertEquals(created.payable.getPayableId(), recovered.payable.getPayableId());
        assertEquals(1, payableCount());
        // 原单的应付日仍是当初那天，没被恢复请求改写
        assertEquals(day1, jdbc.queryForObject(
                "SELECT payable_date FROM finance_payable WHERE payable_id=?",
                java.time.LocalDate.class, created.payable.getPayableId()));
    }

    @Test
    @Order(15)
    @DisplayName("首次创建的非法日期照旧拒绝：放宽的只是重放，不是首次校验")
    void firstTimeInvalidDatesStillRejected() {
        setToday(java.time.LocalDate.of(2026, 3, 10));

        // 到期日早于应付日：首次创建必须拒绝
        FinancePayable bad = draft(STORE, "1000.00", "供应商甲");
        bad.setPayableDate(java.time.LocalDate.of(2026, 3, 10));
        bad.setDueDate(java.time.LocalDate.of(2026, 3, 1));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.create(bad, "REQ-BAD-" + UUID.randomUUID()));
        assertTrue(e.getMessage().contains("到期日"), "拒绝原因不对：" + e.getMessage());

        // 未传应付日、到期日早于今天：同样在首次创建时被拒
        FinancePayable badAuto = draft(STORE, "1000.00", "供应商甲");
        badAuto.setPayableDate(null);
        badAuto.setDueDate(java.time.LocalDate.of(2026, 3, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(badAuto, "REQ-BAD2-" + UUID.randomUUID()));

        assertEquals(0, payableCount(), "非法日期的单据被建出来了");
        assertEquals(0, requestCount());
    }

    // ==================== 真实迁移与外键 ====================

    @Test
    @Order(9)
    @DisplayName("真实执行迁移：登记表外键拒绝不存在的单和跨店错挂，存量数据不受影响")
    void migrationAddsForeignKeyAndKeepsData() throws Exception {
        String legacy = "payable_create_mig_" + System.currentTimeMillis();
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""));
        admin.execute("CREATE DATABASE " + legacy + " CHARACTER SET utf8mb4");
        JdbcTemplate old = new JdbcTemplate(new DriverManagerDataSource(HOST + legacy + OPTS, "root", ""));

        old.execute("CREATE TABLE finance_payable (payable_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "store_id BIGINT NOT NULL, payable_no VARCHAR(50), supplier_name VARCHAR(100), "
                + "total_amount DECIMAL(12,2), paid_amount DECIMAL(12,2), pending_amount DECIMAL(12,2), "
                + "status VARCHAR(20), payable_date DATE, "
                // 被引用的唯一键由上一支迁移建立，这里按迁移后的状态准备母表
                + "UNIQUE KEY uk_finance_payable_id_store (payable_id, store_id)) ENGINE=InnoDB");
        old.update("INSERT INTO finance_payable(store_id,payable_no,total_amount,paid_amount,pending_amount,status) "
                + "VALUES (1,'PY-KEEP-1',1000.00,0.00,1000.00,'unpaid')");
        old.update("INSERT INTO finance_payable(store_id,payable_no,total_amount,paid_amount,pending_amount,status) "
                + "VALUES (2,'PY-KEEP-2',500.00,0.00,500.00,'unpaid')");
        String before = old.queryForList("SELECT payable_id,store_id,payable_no,total_amount,status "
                + "FROM finance_payable ORDER BY payable_id").toString();

        String sql = java.nio.file.Files.readString(
                java.nio.file.Path.of("..", "scripts", "migrations", "payable_create_request_v1.sql"));
        for (String stmt : sql.split(";")) {
            String one = stmt.replaceAll("(?m)^--.*$", "").trim();
            if (!one.isEmpty()) old.execute(one);
        }

        assertEquals(before, old.queryForList("SELECT payable_id,store_id,payable_no,total_amount,status "
                + "FROM finance_payable ORDER BY payable_id").toString(), "迁移改动了存量应付单");

        Integer fk = old.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='payable_create_request' AND CONSTRAINT_TYPE='FOREIGN KEY'",
                Integer.class);
        assertEquals(1, fk, "外键没有建出来");

        Long store1Bill = old.queryForObject(
                "SELECT payable_id FROM finance_payable WHERE payable_no='PY-KEEP-1'", Long.class);

        old.update("INSERT INTO payable_create_request(request_id,store_id,payable_id,payable_no,params_hash) "
                        + "VALUES (?,?,?,?,?)",
                "REQ-OK", 1L, store1Bill, "PY-KEEP-1", "0".repeat(64));

        // 不存在的单
        assertThrows(Exception.class, () ->
                old.update("INSERT INTO payable_create_request(request_id,store_id,payable_id,payable_no,params_hash) "
                                + "VALUES (?,?,?,?,?)",
                        "REQ-GHOST", 1L, 999999L, "PY-GHOST", "1".repeat(64)));

        // 跨店错挂：单存在但门店对不上
        assertThrows(Exception.class, () ->
                old.update("INSERT INTO payable_create_request(request_id,store_id,payable_id,payable_no,params_hash) "
                                + "VALUES (?,?,?,?,?)",
                        "REQ-CROSS", 2L, store1Bill, "PY-KEEP-1", "2".repeat(64)));

        assertEquals(1, (long) old.queryForObject(
                "SELECT COUNT(*) FROM payable_create_request", Integer.class), "被拒的两条不该留下痕迹");
        System.out.println("迁移比对库保留以便复核：" + legacy);
    }
}
