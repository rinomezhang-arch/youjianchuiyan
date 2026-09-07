package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.entity.PayableSettlementRecord;
import com.youjian.banquet.repository.FinancePayableRepository;
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
 * CL-OPS-PAYABLE-IDEMPOTENCY-01 隔离库集成测试。
 * <p>
 * 本机隔离 MySQL（127.0.0.1:13317），每次运行新建独立 schema，只写合成数据，
 * 不读生产凭证、不碰生产库、不写 integration 工作目录。真实 JPA、真实事务、真实并发。
 * <p>
 * <b>不涉及任何真实银行付款。</b>被测的"结算"只是账务记账。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayableIdempotencyMysqlTest {

    private static final String HOST = "jdbc:mysql://127.0.0.1:13317/";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final Long STORE = 1L;
    private static final Long OTHER_STORE = 2L;

    // 每个用例一套独立 schema 和上下文：测试库全部保留，便于事后复核当时的真实数据。
    private String schema;
    private AnnotationConfigApplicationContext ctx;
    private JdbcTemplate jdbc;
    private FinancePayableService service;
    private FinancePayableRepository payableRepo;
    private PayableSettlementRecordRepository settlementRepo;
    private org.springframework.transaction.support.TransactionTemplate tx;

    @EnableTransactionManagement
    static class TxConfig { }

    @BeforeEach
    void setUp(org.junit.jupiter.api.TestInfo info) {
        // schema 名带上用例名，出问题时一眼看出该去哪个库复核
        schema = "payable_it_" + System.currentTimeMillis() + "_"
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
        // 扫描后清空、只挂被测流程用到的两个实体，不把整包实体拖进来。
        emfBean.setPackagesToScan("com.youjian.banquet.entity");
        emfBean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.setExcludeUnlistedClasses(true);
            pui.addManagedClassName(FinancePayable.class.getName());
            pui.addManagedClassName(PayableSettlementRecord.class.getName());
            // 创建幂等登记：服务已依赖它的仓储，实体不挂上就建不出表
            pui.addManagedClassName(com.youjian.banquet.entity.PayableCreateRequest.class.getName());
        });
        emfBean.afterPropertiesSet();
        EntityManagerFactory emf = Objects.requireNonNull(emfBean.getObject());
        EntityManager sharedEm = SharedEntityManagerCreator.createSharedEntityManager(emf);
        JpaRepositoryFactory factory = new JpaRepositoryFactory(sharedEm);
        payableRepo = factory.getRepository(FinancePayableRepository.class);
        settlementRepo = factory.getRepository(PayableSettlementRecordRepository.class);
        com.youjian.banquet.repository.PayableCreateRequestRepository createRepo =
                factory.getRepository(com.youjian.banquet.repository.PayableCreateRequestRepository.class);

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(PlatformTransactionManager.class, () -> new JpaTransactionManager(emf));
        ctx.registerBean(FinancePayableRepository.class, () -> payableRepo);
        ctx.registerBean(PayableSettlementRecordRepository.class, () -> settlementRepo);
        ctx.registerBean(com.youjian.banquet.repository.PayableCreateRequestRepository.class,
                () -> createRepo);
        ctx.register(TxConfig.class);
        ctx.registerBean(FinancePayableService.class);
        ctx.refresh();
        service = ctx.getBean(FinancePayableService.class);
        // 夹具用仓储写入需要事务，包一层事务模板；被测方法自己带 @Transactional，不受影响。
        tx = new org.springframework.transaction.support.TransactionTemplate(
                ctx.getBean(PlatformTransactionManager.class));
        loginAs(STORE, false);
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
        UserContext.clear();
        // 不 DROP、不清表：测试库连同当时的数据一起留着，作为可复核的证据。
        System.out.println("测试库保留以便复核：" + schema);
    }

    /** 登录上下文：dataScopeAll=true 相当于总经理，可跨店。 */
    private void loginAs(Long storeId, boolean allStores) {
        UserContext.set(new UserContext.CurrentUser(9L, storeId, allStores ? "gm" : "store_manager", "zhangxiaoqiu"));
        UserContext.setDataScopeAll(allStores);
    }

    private Long newPayable(Long storeId, String total) {
        final org.springframework.transaction.support.TransactionTemplate template = tx;
        final FinancePayableRepository repo = payableRepo;
        FinancePayable p = new FinancePayable();
        p.setStoreId(storeId);
        p.setPayableNo("PY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        p.setSupplierName("测试供应商");
        p.setTotalAmount(new BigDecimal(total));
        p.setPaidAmount(BigDecimal.ZERO);
        p.setPendingAmount(new BigDecimal(total));
        p.setStatus("unpaid");
        p.setPayableDate(LocalDate.now());
        return template.execute(status -> repo.save(p).getPayableId());
    }

    private BigDecimal paidOf(Long payableId) {
        return jdbc.queryForObject("SELECT paid_amount FROM finance_payable WHERE payable_id=?",
                BigDecimal.class, payableId);
    }

    private long ledgerCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM payable_settlement_record", Integer.class);
        return n == null ? 0 : n;
    }

    // ==================== 缺 requestId ====================

    @Test
    @Order(1)
    @DisplayName("缺 requestId：返回可解释错误，且明确说明服务端不会代为生成")
    void missingRequestIdIsExplainedNotAutoGenerated() {
        Long id = newPayable(STORE, "1000.00");
        for (String bad : new String[]{null, "", "   "}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.settle(id, new BigDecimal("100.00"), bad));
            assertTrue(e.getMessage().contains("requestId"), "错误信息没点明是 requestId：" + e.getMessage());
            assertTrue(e.getMessage().contains("不会代为生成"), "没说明服务端不代生成：" + e.getMessage());
        }
        assertEquals(0, ledgerCount(), "缺 requestId 时不该产生任何流水");
        assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(id)));
    }

    // ==================== 重复同请求仅一笔 ====================

    @Test
    @Order(2)
    @DisplayName("同 requestId 同参数重试：只结算一笔，第二次返回原结算号且金额不变")
    void retryWithSameRequestSettlesOnce() {
        Long id = newPayable(STORE, "1000.00");
        String req = "REQ-" + UUID.randomUUID();

        FinancePayableService.SettlementResult first =
                service.settle(id, new BigDecimal("300.00"), req);
        assertFalse(first.replayed);
        FinancePayableService.SettlementResult second =
                service.settle(id, new BigDecimal("300.00"), req);
        assertTrue(second.replayed, "第二次没被识别成重试");

        assertEquals(first.record.getSettlementNo(), second.record.getSettlementNo(), "重试返回了新的结算号");
        assertEquals(1, ledgerCount(), "同一请求产生了两笔流水");
        assertEquals(0, new BigDecimal("300.00").compareTo(paidOf(id)), "重试把钱又记了一遍");
        assertEquals(0, new BigDecimal("700.00").compareTo(
                jdbc.queryForObject("SELECT pending_amount FROM finance_payable WHERE payable_id=?",
                        BigDecimal.class, id)));
    }

    // ==================== 并发同请求仅一笔 ====================

    @Test
    @Order(3)
    @DisplayName("两个连接并发提交同一 requestId：只产生一笔结算，金额不翻倍")
    void concurrentSameRequestSettlesOnce() throws Exception {
        Long id = newPayable(STORE, "1000.00");
        String req = "REQ-" + UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                loginAs(STORE, false);          // 每个线程各自的 ThreadLocal 上下文
                start.await();
                try {
                    return service.settle(id, new BigDecimal("400.00"), req);
                } catch (RuntimeException e) {
                    return e;
                } finally {
                    UserContext.clear();
                }
            }));
        }
        start.countDown();
        int fresh = 0, replayed = 0;
        for (Future<Object> f : futures) {
            Object r = f.get(30, TimeUnit.SECONDS);
            assertTrue(r instanceof FinancePayableService.SettlementResult,
                    "并发下出现异常而不是幂等返回：" + r);
            if (((FinancePayableService.SettlementResult) r).replayed) replayed++; else fresh++;
        }
        pool.shutdown();

        assertEquals(1, fresh, "两个连接都真结算了，幂等没起作用");
        assertEquals(1, replayed);
        assertEquals(1, ledgerCount());
        assertEquals(0, new BigDecimal("400.00").compareTo(paidOf(id)), "并发导致金额翻倍");
    }

    // ==================== 同 key 不同参数拒绝 ====================

    @Test
    @Order(4)
    @DisplayName("同 requestId 换金额：拒绝，且不产生第二笔流水")
    void sameKeyDifferentAmountRejected() {
        Long id = newPayable(STORE, "1000.00");
        String req = "REQ-" + UUID.randomUUID();
        service.settle(id, new BigDecimal("300.00"), req);

        FinancePayableService.SettlementConflictException e = assertThrows(
                FinancePayableService.SettlementConflictException.class,
                () -> service.settle(id, new BigDecimal("500.00"), req));
        assertTrue(e.getMessage().contains("已被另一笔结算占用"), "冲突信息不够解释：" + e.getMessage());
        assertFalse(e.getMessage().contains("500.00"), "冲突提示回显了本次金额，可能顺带泄漏他单数据");

        assertEquals(1, ledgerCount());
        assertEquals(0, new BigDecimal("300.00").compareTo(paidOf(id)));
    }

    @Test
    @Order(5)
    @DisplayName("同 requestId 换应付单：拒绝，另一张单不受影响")
    void sameKeyDifferentPayableRejected() {
        Long first = newPayable(STORE, "1000.00");
        Long second = newPayable(STORE, "2000.00");
        String req = "REQ-" + UUID.randomUUID();
        service.settle(first, new BigDecimal("300.00"), req);

        assertThrows(FinancePayableService.SettlementConflictException.class,
                () -> service.settle(second, new BigDecimal("300.00"), req));

        assertEquals(1, ledgerCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(second)), "另一张单被动了");
    }

    // ==================== 跨店拒绝 ====================

    @Test
    @Order(6)
    @DisplayName("店长结算别的门店应付单：拒绝，无流水无金额变化")
    void crossStoreRejected() {
        Long otherStoreBill = newPayable(OTHER_STORE, "1000.00");
        loginAs(STORE, false);   // 1 号店店长

        assertThrows(FinancePayableService.PayableAccessDeniedException.class,
                () -> service.settle(otherStoreBill, new BigDecimal("100.00"), "REQ-" + UUID.randomUUID()));

        assertEquals(0, ledgerCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(otherStoreBill)));
    }

    // ==================== 失败整批回滚 ====================

    @Test
    @Order(7)
    @DisplayName("流水写入失败：原单金额一起回滚，不出现只改余额没有流水的账")
    void ledgerFailureRollsBackPayable() {
        Long id = newPayable(STORE, "1000.00");
        // 把流水表的 note 列压到 1 个字符，服务写入的固定说明必然超长 —— 用数据库层的真实失败
        // 来验证"流水失败则原单一起回滚"，而不是靠 mock 制造异常。
        jdbc.execute("ALTER TABLE payable_settlement_record MODIFY note VARCHAR(1)");
        try {
            assertThrows(Exception.class,
                    () -> service.settle(id, new BigDecimal("300.00"), "REQ-" + UUID.randomUUID()));
            assertEquals(0, ledgerCount(), "失败后仍留下了流水");
            assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(id)),
                    "流水写失败但原单金额被改了 —— 账对不上");
        } finally {
            jdbc.execute("ALTER TABLE payable_settlement_record MODIFY note VARCHAR(200)");
        }
    }

    @Test
    @Order(8)
    @DisplayName("结算金额超过待付：拒绝且零写入")
    void overpayRejectedWithNoWrite() {
        Long id = newPayable(STORE, "1000.00");
        assertThrows(IllegalArgumentException.class,
                () -> service.settle(id, new BigDecimal("1200.00"), "REQ-" + UUID.randomUUID()));
        assertEquals(0, ledgerCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(id)));
    }

    // ==================== 金额守恒与可读回 ====================

    @Test
    @Order(9)
    @DisplayName("多次结算：流水合计恒等于原单已付，状态随之推进到已付清")
    void ledgerSumEqualsPaidAmount() {
        Long id = newPayable(STORE, "1000.00");
        service.settle(id, new BigDecimal("300.00"), "REQ-A-" + UUID.randomUUID());
        service.settle(id, new BigDecimal("200.50"), "REQ-B-" + UUID.randomUUID());

        assertEquals(0, new BigDecimal("500.50").compareTo(paidOf(id)));
        BigDecimal ledgerSum = jdbc.queryForObject(
                "SELECT SUM(settle_amount) FROM payable_settlement_record WHERE payable_id=?",
                BigDecimal.class, id);
        assertEquals(0, ledgerSum.compareTo(paidOf(id)), "流水合计与原单已付不守恒");
        assertEquals("partial", jdbc.queryForObject(
                "SELECT status FROM finance_payable WHERE payable_id=?", String.class, id));

        // 结清
        service.settle(id, new BigDecimal("499.50"), "REQ-C-" + UUID.randomUUID());
        assertEquals(0, new BigDecimal("1000.00").compareTo(paidOf(id)));
        assertEquals(0, new BigDecimal("0.00").compareTo(
                jdbc.queryForObject("SELECT pending_amount FROM finance_payable WHERE payable_id=?",
                        BigDecimal.class, id)));
        assertEquals("paid", jdbc.queryForObject(
                "SELECT status FROM finance_payable WHERE payable_id=?", String.class, id));

        List<PayableSettlementRecord> records = service.settlements(id);
        assertEquals(3, records.size(), "结算流水读不回来");
        assertEquals(0, new BigDecimal("1000.00").compareTo(records.get(2).getPaidAfter()));
        assertTrue(records.get(0).getNote().contains("不代表银行"),
                "流水必须写明它不是银行付款凭证");
        Set<String> nos = new HashSet<>();
        for (PayableSettlementRecord r : records) {
            assertTrue(nos.add(r.getSettlementNo()), "结算号重复：" + r.getSettlementNo());
        }
    }
    // ==================== 冲突提示不得泄漏他店数据 ====================

    @Test
    @Order(10)
    @DisplayName("拿同一个 key 去探测别的门店：冲突提示不含对方单号、金额和原 requestId")
    void conflictMessageLeaksNothingAboutTheOtherStore() {
        // 2 号店先用某个 key 结算了一笔
        Long otherStoreBill = newPayable(OTHER_STORE, "8888.00");
        loginAs(OTHER_STORE, false);
        String sharedKey = "REQ-PROBE-" + UUID.randomUUID();
        service.settle(otherStoreBill, new BigDecimal("777.00"), sharedKey);

        // 1 号店拿同一个 key 来探测
        Long myBill = newPayable(STORE, "1000.00");
        loginAs(STORE, false);
        FinancePayableService.SettlementConflictException e = assertThrows(
                FinancePayableService.SettlementConflictException.class,
                () -> service.settle(myBill, new BigDecimal("100.00"), sharedKey));

        String msg = e.getMessage();
        assertFalse(msg.contains(String.valueOf(otherStoreBill)), "冲突提示泄漏了对方单号：" + msg);
        assertFalse(msg.contains("777"), "冲突提示泄漏了对方金额：" + msg);
        assertFalse(msg.contains(sharedKey), "冲突提示回显了 requestId：" + msg);
        assertFalse(msg.contains("8888"), "冲突提示泄漏了对方单据总额：" + msg);
        assertTrue(msg.contains("换用新的 requestId") || msg.contains("重试请沿用"),
                "冲突提示没告诉调用方该怎么办：" + msg);

        // 对方那笔不受影响，本店也没被写入
        assertEquals(1, ledgerCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(paidOf(myBill)));
    }

    @Test
    @Order(11)
    @DisplayName("同单同参数重试仍然正常返回原流水，没有被冲突逻辑误伤")
    void normalRetryStillReplaysAfterConflictHardening() {
        Long id = newPayable(STORE, "1000.00");
        String req = "REQ-" + UUID.randomUUID();
        FinancePayableService.SettlementResult first = service.settle(id, new BigDecimal("250.00"), req);
        FinancePayableService.SettlementResult again = service.settle(id, new BigDecimal("250.00"), req);
        assertTrue(again.replayed);
        assertEquals(first.record.getSettlementNo(), again.record.getSettlementNo());
        assertEquals(1, ledgerCount());
    }

    // ==================== 不同应付共用同 key 的唯一键竞态 ====================

    @Test
    @Order(12)
    @DisplayName("两个连接用同一 key 结算不同应付单：只成功一笔，另一笔是明确冲突而不是底层 500")
    void concurrentDifferentPayablesSameKeyGivesClearConflict() throws Exception {
        Long billA = newPayable(STORE, "1000.00");
        Long billB = newPayable(STORE, "2000.00");
        String sharedKey = "REQ-RACE-" + UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Long target : List.of(billA, billB)) {
            futures.add(pool.submit(() -> {
                loginAs(STORE, false);
                start.await();
                try {
                    return service.settle(target, new BigDecimal("100.00"), sharedKey);
                } catch (RuntimeException e) {
                    return e;
                } finally {
                    UserContext.clear();
                }
            }));
        }
        start.countDown();
        int ok = 0, conflicts = 0;
        List<String> unexpected = new ArrayList<>();
        for (Future<Object> f : futures) {
            Object r = f.get(30, TimeUnit.SECONDS);
            if (r instanceof FinancePayableService.SettlementResult) ok++;
            // 冲突与"并发撞锁可重试"都算明确处理；不明确的类型才是把底层异常吞成 500。
            else if (r instanceof FinancePayableService.SettlementConflictException
                    || r instanceof FinancePayableService.SettlementRetryableException) conflicts++;
            else unexpected.add(r == null ? "null" : r.getClass().getName() + ": " + ((Throwable) r).getMessage());
        }
        pool.shutdown();

        assertTrue(unexpected.isEmpty(),
                "出现了既不是成功也不是明确冲突的结果，等于把底层异常吞成 500：" + unexpected);
        assertEquals(1, ok, "两笔都成功了，同一个 key 被用在了两张单上");
        assertEquals(1, conflicts);
        assertEquals(1, ledgerCount(), "流水多于一条");
        // 只有一张单被动过，另一张分文未动
        BigDecimal paidA = paidOf(billA), paidB = paidOf(billB);
        assertTrue((paidA.signum() > 0) ^ (paidB.signum() > 0), "两张单都被动了或都没动");
        assertEquals(0, new BigDecimal("0.00").compareTo(paidA.min(paidB)));
    }

    // ==================== 历史已付没有流水：不编造回填 ====================

    @Test
    @Order(13)
    @DisplayName("历史已付无流水：不回填历史，只校验历史基数 + 本轮增量守恒")
    void historicalPaidWithoutLedgerIsNotBackfilled() {
        // 模拟版本化之前就已经付过一部分、但没有任何流水的存量单据
        Long id = newPayable(STORE, "1000.00");
        jdbc.update("UPDATE finance_payable SET paid_amount=?, pending_amount=?, status='partial' "
                + "WHERE payable_id=?", new BigDecimal("400.00"), new BigDecimal("600.00"), id);
        BigDecimal historicalBase = paidOf(id);
        assertEquals(0, new BigDecimal("400.00").compareTo(historicalBase));
        assertEquals(0, ledgerCount(), "前提：历史已付没有任何流水");

        service.settle(id, new BigDecimal("150.00"), "REQ-H1-" + UUID.randomUUID());
        service.settle(id, new BigDecimal("50.00"), "REQ-H2-" + UUID.randomUUID());

        BigDecimal ledgerSum = jdbc.queryForObject(
                "SELECT SUM(settle_amount) FROM payable_settlement_record WHERE payable_id=?",
                BigDecimal.class, id);
        assertEquals(0, new BigDecimal("200.00").compareTo(ledgerSum), "本轮流水增量不对");
        assertEquals(0, new BigDecimal("600.00").compareTo(paidOf(id)));
        // 守恒式：历史基数 + 本轮流水 = 当前已付。历史那 400 没有流水，也不去伪造。
        assertEquals(0, historicalBase.add(ledgerSum).compareTo(paidOf(id)),
                "历史基数 + 本轮流水 与当前已付不守恒");
        assertEquals(2, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payable_settlement_record WHERE payable_id=?", Integer.class, id),
                "流水条数应只有本轮这两笔，没有凭空补出历史流水");
    }

    // ==================== 复合外键：真实迁移后生效 ====================

    @Test
    @Order(14)
    @DisplayName("真实执行迁移：复合外键拒绝不存在的应付单和跨店错挂，原单历史不受影响")
    void migrationAddsCompositeForeignKey() throws Exception {
        String legacy = "payable_mig_" + System.currentTimeMillis();
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""));
        admin.execute("CREATE DATABASE " + legacy + " CHARACTER SET utf8mb4");
        JdbcTemplate old = new JdbcTemplate(new DriverManagerDataSource(HOST + legacy + OPTS, "root", ""));

        // 迁移之前的母表：只有 payable_id 单列主键，没有 (payable_id, store_id) 索引
        old.execute("CREATE TABLE finance_payable (payable_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "store_id BIGINT NOT NULL, payable_no VARCHAR(50), supplier_name VARCHAR(100), "
                + "total_amount DECIMAL(12,2), paid_amount DECIMAL(12,2), pending_amount DECIMAL(12,2), "
                + "status VARCHAR(20), payable_date DATE) ENGINE=InnoDB");
        old.update("INSERT INTO finance_payable(store_id,payable_no,total_amount,paid_amount,pending_amount,status) "
                + "VALUES (1,'PY-LEGACY-1',1000.00,300.00,700.00,'partial')");
        old.update("INSERT INTO finance_payable(store_id,payable_no,total_amount,paid_amount,pending_amount,status) "
                + "VALUES (2,'PY-LEGACY-2',500.00,0.00,500.00,'unpaid')");
        String before = old.queryForList(
                "SELECT payable_id,store_id,payable_no,total_amount,paid_amount,pending_amount,status "
                        + "FROM finance_payable ORDER BY payable_id").toString();

        // 真实执行迁移脚本
        String sql = java.nio.file.Files.readString(
                java.nio.file.Path.of("..", "scripts", "migrations", "payable_settlement_record_v1.sql"));
        for (String stmt : sql.split(";")) {
            String one = stmt.replaceAll("(?m)^--.*$", "").trim();
            if (!one.isEmpty()) old.execute(one);
        }

        // 存量数据不受影响
        String after = old.queryForList(
                "SELECT payable_id,store_id,payable_no,total_amount,paid_amount,pending_amount,status "
                        + "FROM finance_payable ORDER BY payable_id").toString();
        assertEquals(before, after, "迁移改动了应付单存量数据");

        // 外键确实建出来了
        Integer fk = old.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='payable_settlement_record' AND CONSTRAINT_TYPE='FOREIGN KEY'",
                Integer.class);
        assertEquals(1, fk, "复合外键没有建出来");

        Long store1Bill = old.queryForObject(
                "SELECT payable_id FROM finance_payable WHERE payable_no='PY-LEGACY-1'", Long.class);

        // 正常挂接可以写入
        old.update("INSERT INTO payable_settlement_record(request_id,settlement_no,store_id,payable_id,"
                        + "settle_amount,paid_after,pending_after,status_after) VALUES (?,?,?,?,?,?,?,?)",
                "REQ-OK", "ST-OK", 1L, store1Bill,
                new BigDecimal("100.00"), new BigDecimal("400.00"), new BigDecimal("600.00"), "partial");

        // 不存在的应付单：被外键挡住
        assertThrows(Exception.class, () ->
                old.update("INSERT INTO payable_settlement_record(request_id,settlement_no,store_id,payable_id,"
                                + "settle_amount,paid_after,pending_after,status_after) VALUES (?,?,?,?,?,?,?,?)",
                        "REQ-GHOST", "ST-GHOST", 1L, 999999L,
                        new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("0.00"), "paid"));

        // 跨店错挂：单号存在但门店对不上，同样被挡住
        assertThrows(Exception.class, () ->
                old.update("INSERT INTO payable_settlement_record(request_id,settlement_no,store_id,payable_id,"
                                + "settle_amount,paid_after,pending_after,status_after) VALUES (?,?,?,?,?,?,?,?)",
                        "REQ-CROSS", "ST-CROSS", 2L, store1Bill,
                        new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("0.00"), "paid"));

        assertEquals(1, (long) old.queryForObject(
                "SELECT COUNT(*) FROM payable_settlement_record", Integer.class),
                "被拒绝的两条不该留下痕迹");
        System.out.println("迁移比对库保留以便复核：" + legacy);
    }
}
