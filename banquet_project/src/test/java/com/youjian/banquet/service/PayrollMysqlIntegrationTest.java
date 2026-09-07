package com.youjian.banquet.service;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-PAYROLL-01 隔离库集成测试。
 * <p>
 * 用本机隔离 MySQL（127.0.0.1:13317），每次运行新建独立 schema，只写合成数据，
 * 不读生产凭证、不碰生产库、不覆盖其他协作方的测试。
 * <p>
 * 这里跑的是真实 JDBC 写入、真实 Spring 事务代理，所以"整批回滚""状态不被静默改回"
 * 这类断言是库级的，不是 mock 出来的。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollMysqlIntegrationTest {

    private static final String HOST = "jdbc:mysql://127.0.0.1:13317/";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String MONTH = "2026-08";

    private static String schema;
    private static AnnotationConfigApplicationContext ctx;
    private static JdbcTemplate jdbc;
    private static PayrollService service;

    @EnableTransactionManagement
    static class TxConfig { }

    @BeforeEach
    void setUpSchema() {
        schema = "payroll_it_" + System.currentTimeMillis();
        new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");

        DataSource ds = new DriverManagerDataSource(HOST + schema + OPTS, "root", "");
        jdbc = new JdbcTemplate(ds);

        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.ClassPathResource("payroll-metadata-fixture-20260907.sql"),
            new org.springframework.core.io.FileSystemResource("../scripts/migrations/payroll_approval_payout_v1.sql")
        ).execute(ds);

        // 1 号店两人，2 号店一人（用于跨店拒绝）
        jdbc.update("INSERT INTO staff_master VALUES (1,1,'张晓秋','zhangxiaoqiu','rino')");
        jdbc.update("INSERT INTO staff_master VALUES (2,1,'李四','lisi',NULL)");
        jdbc.update("INSERT INTO staff_master VALUES (3,2,'王五','wangwu',NULL)");
        jdbc.update("INSERT INTO staff_master VALUES (4,1,'普通员工','putong',NULL)");

        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(DataSource.class, () -> ds);
        ctx.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(ds));
        ctx.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(ds));
        ctx.register(TxConfig.class);
        ctx.registerBean(PayrollService.class);
        ctx.refresh();
        service = ctx.getBean(PayrollService.class);
        ReflectionTestUtils.setField(service, "approversRaw", "张婧,zhangjing,张晓秋,zhangxiaoqiu,rino");
    }

    @AfterEach
    void tearDown() {
        // 保留 schema 便于复核，只关上下文；隔离库里全是合成数据。
        if (ctx != null) ctx.close();
        System.out.println("测试库保留以便复核：" + schema);
    }

    @AfterEach void relationsRemainHealthy() {
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM month_salary m LEFT JOIN payroll_payout_record p ON m.payout_id=p.payout_id AND m.salary_month=p.salary_month WHERE m.payout_id IS NOT NULL AND (p.payout_id IS NULL OR (p.store_id IS NOT NULL AND p.store_id<>m.store_id))",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record p WHERE p.headcount<>(SELECT COUNT(*) FROM month_salary m WHERE m.payout_id=p.payout_id) OR p.total_net<>(SELECT COALESCE(SUM(m.net_salary),0) FROM month_salary m WHERE m.payout_id=p.payout_id)",Integer.class));
    }

    @Test void payoutLinkRejectsMissingBatchAndWrongMonth() {
        service.save(MONTH,List.of(row(1,"8000")),gm());
        service.approve(MONTH,gm());
        service.payout(MONTH,gm());
        var before=jdbc.queryForList("SELECT * FROM month_salary");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            ()->jdbc.update("UPDATE month_salary SET payout_id=999999 WHERE staff_id=1"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            ()->jdbc.update("UPDATE month_salary SET salary_month='2026-09' WHERE staff_id=1"));
        assertEquals(before,jdbc.queryForList("SELECT * FROM month_salary"));
    }

    @Test void reopeningSavedPayrollDoesNotAddAttendanceOrRewardsAgain() {
        jdbc.execute("ALTER TABLE staff_master ADD can_view_all_stores INT DEFAULT 0, ADD can_manage_hr INT DEFAULT 0, ADD department VARCHAR(30), ADD basic_salary DECIMAL(10,2), ADD monthly_salary DECIMAL(10,2), ADD performance_salary DECIMAL(10,2), ADD subsidy DECIMAL(10,2), ADD bonus DECIMAL(10,2), ADD social_insurance DECIMAL(10,2), ADD housing_fund DECIMAL(10,2), ADD employment_status VARCHAR(20)");
        jdbc.update("UPDATE staff_master SET can_view_all_stores=1,can_manage_hr=1 WHERE staff_id=1");
        jdbc.execute("CREATE TABLE attendance_records(staff_id VARCHAR(30), month VARCHAR(7),total_present DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE overtime(staff_id INT,overtime_date DATE,hours DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE report_staff_kpi(staff_id INT,stat_month VARCHAR(7),reward_count INT)");
        jdbc.update("INSERT INTO attendance_records VALUES('1',?,22)",MONTH);
        jdbc.update("INSERT INTO report_staff_kpi VALUES(1,?,9)",MONTH);
        var submitted=row(1,"1000");submitted.put("post_salary","200");submitted.put("attendance_pay","300");
        submitted.put("bonus","40");submitted.put("overtime_pay","50");submitted.put("allowance","5");
        service.save(MONTH,List.of(submitted),gm());
        var before=jdbc.queryForMap("SELECT gross_salary,net_salary,performance_salary,reward_amount,overtime_pay FROM month_salary WHERE staff_id=1");
        var controller=new com.youjian.banquet.controller.PayrollController();
        ReflectionTestUtils.setField(controller,"jdbc",jdbc);
        com.youjian.banquet.util.UserContext.set(new com.youjian.banquet.util.UserContext.CurrentUser(1L,1L,"gm","zhangxiaoqiu"));
        try {
            var response=controller.getPayroll(MONTH);
            assertEquals(200,response.getCode());
            var read=response.getData().stream().filter(r->((Number)r.get("emp_id")).intValue()==1).findFirst().orElseThrow();
            assertEquals(new BigDecimal("200.00"),read.get("post_salary"));
            assertEquals(new BigDecimal("300.00"),read.get("attendance_pay"));
            assertEquals(new BigDecimal("40.00"),read.get("bonus"));
            assertEquals(new BigDecimal("50.00"),read.get("overtime_pay"));
            assertEquals(before.get("gross_salary"),read.get("gross_pay"));
            assertEquals(before.get("net_salary"),read.get("net_pay"));
            service.save(MONTH,List.of(read),gm());
            assertEquals(before,jdbc.queryForMap("SELECT gross_salary,net_salary,performance_salary,reward_amount,overtime_pay FROM month_salary WHERE staff_id=1"));
        } finally { com.youjian.banquet.util.UserContext.clear(); }
    }

    private PayrollService.PayrollContext gm() {
        return new PayrollService.PayrollContext(true, null, 1L, "zhangxiaoqiu");
    }

    private PayrollService.PayrollContext storeManager() {
        return new PayrollService.PayrollContext(false, 1L, 2L, "lisi");
    }

    private Map<String, Object> row(int empId, String base) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("emp_id", empId);
        r.put("base_salary", base);
        r.put("post_salary", "0");
        r.put("attendance_pay", "0");
        r.put("overtime_pay", "0");
        r.put("bonus", "0");
        r.put("allowance", "0");
        r.put("deduction_social", "0");
        r.put("deduction_other", "0");
        return r;
    }

    private long salaryRowCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM month_salary", Integer.class);
        return n == null ? 0 : n;
    }

    // ==================== 保存 → 读回 ====================

    @Test
    @Order(1)
    @DisplayName("保存后读回：金额真实落库，状态为已保存")
    void saveThenReadBack() {
        Map<String, Object> result = service.save(MONTH, List.of(row(1, "8000"), row(2, "6000")), gm());
        assertEquals(2, result.get("saved"));

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT staff_id, base_salary, gross_salary, net_salary, status FROM month_salary ORDER BY staff_id");
        assertEquals(2, rows.size());
        assertEquals(0, new BigDecimal("8000.00").compareTo((BigDecimal) rows.get(0).get("base_salary")));
        assertEquals(PayrollService.STATUS_SAVED, ((Number) rows.get(0).get("status")).intValue());
        // 8000 应发，扣除 0，应税 3000 → 个税 90，实发 7910
        assertEquals(0, new BigDecimal("7910.00").compareTo((BigDecimal) rows.get(0).get("net_salary")));
    }

    // ==================== 服务端重算，不信任客户端合计 ====================

    @Test
    @Order(2)
    @DisplayName("客户端传假的应发/实发：落库的是服务端重算值，差异被如实记录")
    void clientTotalsAreIgnored() {
        Map<String, Object> tampered = row(1, "8000");
        tampered.put("gross_pay", "999999");
        tampered.put("net_pay", "999999");
        tampered.put("deduction_tax", "0");

        Map<String, Object> result = service.save(MONTH, List.of(tampered), gm());

        BigDecimal gross = jdbc.queryForObject(
                "SELECT gross_salary FROM month_salary WHERE staff_id=1", BigDecimal.class);
        BigDecimal net = jdbc.queryForObject(
                "SELECT net_salary FROM month_salary WHERE staff_id=1", BigDecimal.class);
        assertEquals(0, new BigDecimal("8000.00").compareTo(gross), "应发被客户端数字污染了");
        assertEquals(0, new BigDecimal("7910.00").compareTo(net), "实发被客户端数字污染了");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ignored = (List<Map<String, Object>>) result.get("ignoredClientTotals");
        assertEquals(3, ignored.size(), "三个被忽略的合计字段都应记录下来");
    }

    // ==================== 坏数据整批拒绝并回滚 ====================

    @Test
    @Order(3)
    @DisplayName("批次里有不存在的员工：整批拒绝，一行都不写")
    void unknownStaffRejectsWholeBatch() {
        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "8000"), row(999, "5000")), gm()));
        assertTrue(e.getMessage().contains("999"));
        assertEquals(0, salaryRowCount(), "合法的那一行也不该被写进去");
    }

    @Test
    @Order(4)
    @DisplayName("同一员工重复出现：整批拒绝，一行都不写")
    void duplicateStaffRejectsWholeBatch() {
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "8000"), row(1, "9000")), gm()));
        assertEquals(0, salaryRowCount());
    }

    @Test
    @Order(5)
    @DisplayName("店长保存到别的门店员工：整批拒绝，一行都不写")
    void crossStoreRejectsWholeBatch() {
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "8000"), row(3, "5000")), storeManager()));
        assertEquals(0, salaryRowCount(), "跨店数据必须整批拒绝，不能只跳过越权那一条");
    }

    @Test
    @Order(6)
    @DisplayName("金额为负或多于两位小数：整批拒绝")
    void badAmountsRejected() {
        Map<String, Object> negative = row(1, "-100");
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(negative), gm()));
        Map<String, Object> tooPrecise = row(1, "8000.123");
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(tooPrecise), gm()));
        assertEquals(0, salaryRowCount());
    }

    // ==================== 审批只认真人白名单 ====================

    @Test
    @Order(7)
    @DisplayName("不在批复白名单的人审批：拒绝，状态仍是已保存")
    void nonApproverCannotApprove() {
        service.save(MONTH, List.of(row(4, "5000")), gm());
        PayrollService.PayrollContext outsider =
                new PayrollService.PayrollContext(true, null, 4L, "putong");

        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class, () -> service.approve(MONTH, outsider));
        assertTrue(e.getMessage().contains("张婧"));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM month_salary WHERE staff_id=4", Integer.class);
        assertEquals(PayrollService.STATUS_SAVED, status);
    }

    @Test
    @Order(8)
    @DisplayName("未登录（无 staffId）不能审批")
    void anonymousCannotApprove() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        PayrollService.PayrollContext noLogin =
                new PayrollService.PayrollContext(true, null, null, "some-agent");
        assertThrows(PayrollService.PayrollRejectedException.class, () -> service.approve(MONTH, noLogin));
        assertEquals(PayrollService.STATUS_SAVED,
                jdbc.queryForObject("SELECT status FROM month_salary WHERE staff_id=1", Integer.class));
    }

    @Test
    @Order(9)
    @DisplayName("白名单真人审批：状态推进到已审批并留下审批人")
    void approverAdvancesStatus() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        Map<String, Object> result = service.approve(MONTH, gm());
        assertEquals(1, result.get("approved"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, approved_by, approved_at FROM month_salary WHERE staff_id=1");
        assertEquals(PayrollService.STATUS_APPROVED, ((Number) row.get("status")).intValue());
        assertEquals("zhangxiaoqiu", row.get("approved_by"));
        assertNotNull(row.get("approved_at"));
    }

    // ==================== 发放记账 ====================

    @Test
    @Order(10)
    @DisplayName("未审批不能发放记账")
    void payoutRequiresApproval() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class, () -> service.payout(MONTH, gm()));
        assertTrue(e.getMessage().contains("未审批"));
        assertEquals(0, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
    }

    @Test
    @Order(11)
    @DisplayName("完整闭环：保存 → 审批 → 发放记账，台账合计与库内实发一致")
    void fullLoop() {
        service.save(MONTH, List.of(row(1, "8000"), row(2, "6000")), gm());
        service.approve(MONTH, gm());
        Map<String, Object> paid = service.payout(MONTH, gm());

        assertEquals(2, paid.get("paid"));
        assertTrue(paid.get("message").toString().contains("不代表银行已到账"),
                "发放记账必须明确它不是银行到账");

        BigDecimal ledger = jdbc.queryForObject(
                "SELECT total_net FROM payroll_payout_record", BigDecimal.class);
        BigDecimal actual = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);
        assertEquals(0, ledger.compareTo(actual), "台账合计与库内实发不一致");
        assertEquals(2, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM month_salary WHERE status=3 AND paid_by='zhangxiaoqiu'", Integer.class));
    }

    @Test
    @Order(12)
    @DisplayName("重复发放记账：不重复记账，如实说明本月已记过")
    void payoutIsIdempotent() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());
        service.payout(MONTH, gm());

        Map<String, Object> again = service.payout(MONTH, gm());
        assertEquals(0, again.get("paid"));
        assertEquals(Boolean.TRUE, again.get("alreadyRecorded"));
        assertEquals(1, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record", Integer.class), "台账被重复写入了");
    }

    // ==================== 已发放不可被保存静默退回 ====================

    @Test
    @Order(13)
    @DisplayName("已发放记账(3)的月份再保存：拒绝，状态不被退回成已保存(1)")
    void paidPayrollCannotBeSilentlyReverted() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());
        service.payout(MONTH, gm());

        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "12000")), gm()));
        assertTrue(e.getMessage().contains("已发放记账"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, base_salary FROM month_salary WHERE staff_id=1");
        assertEquals(PayrollService.STATUS_PAID, ((Number) row.get("status")).intValue(),
                "已发放被静默退回成已保存——这正是基线的缺陷");
        assertEquals(0, new BigDecimal("8000.00").compareTo((BigDecimal) row.get("base_salary")),
                "已发放月份的金额被改掉了");
    }

    // ==================== 分批记账不重复计金额 ====================

    @Test
    @Order(16)
    @DisplayName("同店分两批记账：第二批台账只算本批，不把上一批的金额加进去")
    void secondBatchLedgerCountsOnlyItsOwnRows() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());
        Map<String, Object> first = service.payout(MONTH, gm());
        assertEquals(1, first.get("paid"));
        BigDecimal firstTotal = (BigDecimal) first.get("totalNet");

        service.save(MONTH, List.of(row(2, "6000")), gm());
        service.approve(MONTH, gm());
        Map<String, Object> second = service.payout(MONTH, gm());
        assertEquals(1, second.get("paid"));
        BigDecimal secondTotal = (BigDecimal) second.get("totalNet");

        BigDecimal staff2Net = jdbc.queryForObject(
                "SELECT net_salary FROM month_salary WHERE staff_id=2", BigDecimal.class);
        assertEquals(0, secondTotal.compareTo(staff2Net),
                "第二批台账把第一批的金额也算进去了 —— 这正是分批重复计账");

        List<Map<String, Object>> ledgers = jdbc.queryForList(
                "SELECT headcount, total_net FROM payroll_payout_record ORDER BY payout_id");
        assertEquals(2, ledgers.size());
        assertEquals(1, ((Number) ledgers.get(0).get("headcount")).intValue());
        assertEquals(1, ((Number) ledgers.get(1).get("headcount")).intValue());
        BigDecimal ledgerSum = ((BigDecimal) ledgers.get(0).get("total_net"))
                .add((BigDecimal) ledgers.get(1).get("total_net"));
        BigDecimal actualSum = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);
        assertEquals(0, ledgerSum.compareTo(actualSum), "两批台账合计与库内实发不一致");
        assertEquals(0, firstTotal.add(secondTotal).compareTo(actualSum));
    }

    @Test
    @Order(17)
    @DisplayName("台账与本批工资行互相反查：每行的 payout_id 指向自己那一批")
    void salaryRowsLinkBackToTheirBatch() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());
        long batch1 = ((Number) service.payout(MONTH, gm()).get("payoutId")).longValue();

        service.save(MONTH, List.of(row(2, "6000")), gm());
        service.approve(MONTH, gm());
        long batch2 = ((Number) service.payout(MONTH, gm()).get("payoutId")).longValue();

        assertNotEquals(batch1, batch2);
        assertEquals(batch1, (long) jdbc.queryForObject(
                "SELECT payout_id FROM month_salary WHERE staff_id=1", Long.class));
        assertEquals(batch2, (long) jdbc.queryForObject(
                "SELECT payout_id FROM month_salary WHERE staff_id=2", Long.class));
        BigDecimal byBatch = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE payout_id=?", BigDecimal.class, batch2);
        BigDecimal ledger = jdbc.queryForObject(
                "SELECT total_net FROM payroll_payout_record WHERE payout_id=?", BigDecimal.class, batch2);
        assertEquals(0, byBatch.compareTo(ledger));
    }

    @Test
    @Order(18)
    @DisplayName("先按门店记账再全店记账：全店那批只记剩下的门店，金额不重复")
    void storeScopedThenAllStores() {
        service.save(MONTH, List.of(row(1, "8000"), row(2, "6000")), gm());
        service.save(MONTH, List.of(row(3, "5000")), gm());
        service.approve(MONTH, gm());

        PayrollService.PayrollContext store1Manager =
                new PayrollService.PayrollContext(false, 1L, 2L, "lisi");
        Map<String, Object> storeBatch = service.payout(MONTH, store1Manager);
        assertEquals(2, storeBatch.get("paid"));

        Map<String, Object> allBatch = service.payout(MONTH, gm());
        assertEquals(1, allBatch.get("paid"), "全店记账把已记过账的门店又算了一遍");

        BigDecimal staff3Net = jdbc.queryForObject(
                "SELECT net_salary FROM month_salary WHERE staff_id=3", BigDecimal.class);
        assertEquals(0, ((BigDecimal) allBatch.get("totalNet")).compareTo(staff3Net));

        BigDecimal ledgerSum = jdbc.queryForObject(
                "SELECT SUM(total_net) FROM payroll_payout_record", BigDecimal.class);
        BigDecimal actualSum = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);
        assertEquals(0, ledgerSum.compareTo(actualSum));
    }

    // ==================== 空月份不能假成功 ====================

    @Test
    @Order(19)
    @DisplayName("从未保存过工资的月份记账：明确拒绝，不能回已完成记账")
    void emptyMonthIsRejectedNotFakedAsDone() {
        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class, () -> service.payout("2026-01", gm()));
        assertTrue(e.getMessage().contains("没有任何工资记录"), "实际提示：" + e.getMessage());
        assertEquals(0, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
    }

    @Test
    @Order(20)
    @DisplayName("本门店本月没有记录：按门店记账同样明确拒绝")
    void emptyStoreScopeIsRejected() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());
        PayrollService.PayrollContext store2Manager =
                new PayrollService.PayrollContext(false, 2L, 1L, "zhangxiaoqiu");
        PayrollService.PayrollRejectedException e = assertThrows(
                PayrollService.PayrollRejectedException.class, () -> service.payout(MONTH, store2Manager));
        assertTrue(e.getMessage().contains("本门店本月没有任何工资记录"), "实际提示：" + e.getMessage());
    }

    @Test
    @Order(21)
    @DisplayName("真已记完的月份再记账：说清是多少条已记过")
    void alreadyRecordedReportsHowMany() {
        service.save(MONTH, List.of(row(1, "8000"), row(2, "6000")), gm());
        service.approve(MONTH, gm());
        service.payout(MONTH, gm());

        Map<String, Object> again = service.payout(MONTH, gm());
        assertEquals(Boolean.TRUE, again.get("alreadyRecorded"));
        assertEquals(2, ((Number) again.get("previouslyRecorded")).intValue());
        assertEquals(1, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
    }

    // ==================== 范围 fail-closed ====================

    @Test
    @Order(22)
    @DisplayName("非全店身份却解析不出门店：保存/审批/记账一律拒绝，不退化成全店")
    void incompleteScopeIsRejectedEverywhere() {
        PayrollService.PayrollContext broken =
                new PayrollService.PayrollContext(false, null, 1L, "zhangxiaoqiu");

        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "8000")), broken));
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.approve(MONTH, broken));
        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.payout(MONTH, broken));
        assertEquals(0, salaryRowCount());
    }

    // ==================== 两连接竞争 ====================

    @Test
    @Order(23)
    @DisplayName("两个连接同时记账：只成功一批，台账不出现两条、金额不翻倍")
    void concurrentPayoutRecordsOnlyOnce() throws Exception {
        service.save(MONTH, List.of(row(1, "8000"), row(2, "6000")), gm());
        service.approve(MONTH, gm());

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    return service.payout(MONTH, gm());
                } catch (RuntimeException e) {
                    return e;
                }
            }));
        }
        start.countDown();
        int successes = 0;
        for (java.util.concurrent.Future<Object> f : futures) {
            Object r = f.get(30, java.util.concurrent.TimeUnit.SECONDS);
            if (r instanceof Map && ((Number) ((Map<?, ?>) r).get("paid")).intValue() > 0) successes++;
        }
        pool.shutdown();

        assertEquals(1, successes, "两个连接都记账成功了，说明锁没起作用");
        assertEquals(1, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record", Integer.class), "台账被记了两次");
        BigDecimal ledger = jdbc.queryForObject(
                "SELECT SUM(total_net) FROM payroll_payout_record", BigDecimal.class);
        BigDecimal actual = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);
        assertEquals(0, ledger.compareTo(actual), "台账金额与实发对不上");
    }

    @Test
    @Order(15)
    @DisplayName("写到一半在数据库层失败：真实事务回滚，先写进去的那行也没了")
    void midWriteFailureRollsBackEverything() {
        // 前面那些"整批拒绝"的用例都是在写库之前就拦下了，证明不了事务本身有没有生效。
        // 这里故意让第二行通过全部应用层校验、却在 INSERT 时撑爆 DECIMAL(12,2)，
        // 失败点落在第一行已经写入之后，才能真正验证 @Transactional 的回滚。
        Map<String, Object> overflow = row(2, "99999999999999");
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> service.save(MONTH, List.of(row(1, "8000"), overflow), gm()));

        assertEquals(0, salaryRowCount(),
                "第二行失败后，第一行必须一起回滚；留下任何一行都说明事务没生效");
    }

    @Test
    @Order(14)
    @DisplayName("已审批(2)的月份再保存同样拒绝，且批次里的其他人也不被写入")
    void approvedPayrollCannotBeOverwritten() {
        service.save(MONTH, List.of(row(1, "8000")), gm());
        service.approve(MONTH, gm());

        assertThrows(PayrollService.PayrollRejectedException.class,
                () -> service.save(MONTH, List.of(row(1, "9000"), row(2, "6000")), gm()));

        assertEquals(PayrollService.STATUS_APPROVED,
                jdbc.queryForObject("SELECT status FROM month_salary WHERE staff_id=1", Integer.class));
        assertEquals(0, (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM month_salary WHERE staff_id=2", Integer.class),
                "同批次的员工 2 不该被写入");
    }
}
