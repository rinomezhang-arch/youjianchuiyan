package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.aop.AuditLogAspect;
import com.youjian.banquet.aop.StoreDataScopeAspect;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.service.PayrollService;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * TL-RC-PAYROLL-HTTP-R2-59：云端隔离库工资审批付款真实 HTTP 闭环。
 * <p>
 * 与基线 {@link PayrollHttpMysqlFlowTest} 同一套真实基础设施（MockMvc standalone + JwtAuthInterceptor
 * 真实 JWT + 真实 StaffRealtimeGuard 复核 + 真实事务 JDBC），但按卡片五条验收逐条铺证：
 * <ol>
 *   <li>只连 127.0.0.1:13318，唯一 TLPAY59 schema + 合成账号（不删任何已有 schema）。</li>
 *   <li>核算保存 → 审批角色审批 → 付款角色付款 → 刷新回读，HTTP/DB/金额三方一致。</li>
 *   <li>停用账号 / 无权限角色 / 跨门店 / 重复付款 均被拒绝，成功金额不变，台账仅一条。</li>
 *   <li>工资(month_salary)/审批状态/付款台账(payroll_payout_record) 之间无孤儿，
 *       业务键 (staff_id, salary_month) 回读可恢复，重复付款不重复扣款。</li>
 * </ol>
 * 本类只读隔离库 13318、只写自己新建的 tlpay59_* schema、只造合成身份，不碰生产库。
 */
class PayrollRealE2e59Test {

    static final String HOST = "jdbc:mysql://127.0.0.1:13318/";
    static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    static final String MONTH = "2026-08";

    // 合成身份编号（与花名册插入一一对应）
    static final long APPROVER = 1;          // 审批角色：白名单 + 可管理薪酬 + 全门店
    static final long PAYER = 2;             // 付款角色：可管理薪酬 + 全门店（不在批复白名单）
    static final long HR = 3;                // 核算角色：可管理薪酬 + 全门店（创建/保存）
    static final long WORKER_A = 4;          // 1 号店员工甲（被发工资）
    static final long WORKER_B = 5;          // 1 号店员工乙（被发工资）
    static final long NO_PERM = 6;           // 无权限角色（waiter，不可管理薪酬）
    static final long DISABLED = 7;          // 停用账号（employment_status=resigned）
    static final long STORE_MGR = 8;         // 1 号店店长（仅本门店，不可跨店）
    static final long OTHER_STORE_WORKER = 9;// 2 号店员工（跨店反例目标）

    static final String APPROVER_ACCT = "tlpay59_approver";

    String schema;
    String secret;
    JdbcTemplate jdbc;
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class Wiring {
    }

    @BeforeEach
    void setup() {
        assertNull(UserContext.get());
        // 唯一 TLPAY59 schema，带时间戳 + 随机后缀，不删除任何已有 schema。
        schema = "tlpay59_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
        new JdbcTemplate(new DriverManagerDataSource(HOST + OPTS, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        DataSource ds = new DriverManagerDataSource(HOST + schema + OPTS, "root", "");
        jdbc = new JdbcTemplate(ds);
        new ResourceDatabasePopulator(
                new ClassPathResource("payroll-metadata-fixture-20260907.sql"),
                new FileSystemResource("../scripts/migrations/payroll_approval_payout_v1.sql")
        ).execute(ds);

        // 补齐 staff_master 业务列
        jdbc.execute("ALTER TABLE staff_master ADD can_view_all_stores INT DEFAULT 0, "
                + "ADD can_manage_hr INT DEFAULT 0, ADD department VARCHAR(30), "
                + "ADD basic_salary DECIMAL(10,2), ADD monthly_salary DECIMAL(10,2), "
                + "ADD performance_salary DECIMAL(10,2), ADD subsidy DECIMAL(10,2), "
                + "ADD bonus DECIMAL(10,2), ADD social_insurance DECIMAL(10,2), "
                + "ADD housing_fund DECIMAL(10,2), ADD employment_status VARCHAR(20)");
        for (String ddl : new String[]{"ALTER TABLE staff_master ADD COLUMN role VARCHAR(30)"}) {
            try { jdbc.execute(ddl); } catch (Exception columnAlreadyThere) { /* 已有该列 */ }
        }
        // getPayroll() 只读依赖的三张明细表 + 审计表
        jdbc.execute("CREATE TABLE attendance_records(staff_id VARCHAR(30), month VARCHAR(7), total_present DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE overtime(staff_id INT, overtime_date DATE, hours DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE report_staff_kpi(staff_id INT, stat_month VARCHAR(7), reward_count INT)");
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(60), "
                + "action VARCHAR(200), target VARCHAR(200), detail TEXT, store_id BIGINT)");

        // 合成花名册（10 人）
        insertStaff(APPROVER, 1, "TLPAY59审批人", APPROVER_ACCT, "gm", 1, 1, "active");
        insertStaff(PAYER, 1, "TLPAY59付款人", "tlpay59_payer", "gm", 1, 1, "active");
        insertStaff(HR, 1, "TLPAY59核算员", "tlpay59_hr", "gm", 1, 1, "active");
        insertStaff(WORKER_A, 1, "TLPAY59员工甲", "tlpay59_worker_a", "staff", 0, 0, "active");
        insertStaff(WORKER_B, 1, "TLPAY59员工乙", "tlpay59_worker_b", "staff", 0, 0, "active");
        insertStaff(NO_PERM, 1, "TLPAY59无权限", "tlpay59_noperm", "waiter", 0, 0, "active");
        insertStaff(DISABLED, 1, "TLPAY59停用", "tlpay59_disabled", "gm", 1, 1, "resigned");
        insertStaff(STORE_MGR, 1, "TLPAY59店长", "tlpay59_mgr", "store_manager", 1, 0, "active");
        insertStaff(OTHER_STORE_WORKER, 2, "TLPAY59他店员工", "tlpay59_other", "staff", 0, 0, "active");

        secret = UUID.randomUUID().toString() + UUID.randomUUID();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-only",
                Map.of("jwt.secret", secret, "approval.approvers", APPROVER_ACCT)));
        context.registerBean(DataSource.class, () -> ds);
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(ds));
        context.register(Wiring.class, PayrollService.class, PayrollController.class, JwtAuthInterceptor.class,
                StoreDataScopeAspect.class, AuditLogAspect.class,
                com.youjian.banquet.auth.StaffRealtimeGuard.class);
        context.refresh();
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(PayrollController.class))
                .addInterceptors(context.getBean(JwtAuthInterceptor.class))
                .build();
    }

    private void insertStaff(long id, long store, String name, String account, String role,
                             int canManageHr, int canViewAll, String employment) {
        jdbc.update("INSERT INTO staff_master(staff_id, store_id, staff_name, staff_account, role, "
                        + "can_manage_hr, can_view_all_stores, employment_status) VALUES (?,?,?,?,?,?,?,?)",
                id, store, name, account, role, canManageHr, canViewAll, employment);
    }

    @AfterEach
    void close() {
        try {
            assertNull(UserContext.get(), "real aspects must clean identity after HTTP");
        } finally {
            if (context != null) context.close();
            System.out.println("TLPAY59_SCHEMA_RETAINED=" + schema);
        }
    }

    String token(long id, long store, String account) {
        return Jwts.builder().subject(account).claim("staffId", id).claim("storeId", store).claim("role", "manager")
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    MockHttpServletResponse rawPost(String endpoint, String actor, String body) throws Exception {
        var request = post("/api/hr/payroll/" + endpoint).param("month", MONTH);
        if (actor != null) request.header("Authorization", "Bearer " + actor);
        if (body != null) request.contentType("application/json").content(body);
        return mvc.perform(request).andReturn().getResponse();
    }

    /** 正向链：HTTP 恒为 200，业务码在 Result.code。 */
    JsonNode postAs(String endpoint, String actor, String body, int code) throws Exception {
        MockHttpServletResponse response = rawPost(endpoint, actor, body);
        assertEquals(200, response.getStatus(), endpoint + " HTTP 状态应为 200，业务码在 Result.code");
        JsonNode result = json.readTree(response.getContentAsString());
        assertEquals(code, result.path("code").asInt(), endpoint + " 业务响应码");
        assertNull(UserContext.get());
        return result.path("data");
    }

    String items(int... ids) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int id : ids) {
            rows.add(Map.of("emp_id", id, "base_salary", "1000", "post_salary", "200",
                    "attendance_pay", "300", "bonus", "40", "overtime_pay", "50", "allowance", "5"));
        }
        return json.writeValueAsString(rows);
    }

    /** 台账与本批工资行互反查、无孤儿、金额守恒（与基线 conserved 同口径）。 */
    void conserved() {
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM month_salary m LEFT JOIN payroll_payout_record p "
                        + "ON m.payout_id=p.payout_id AND m.salary_month=p.salary_month "
                        + "WHERE m.payout_id IS NOT NULL AND (p.payout_id IS NULL "
                        + "OR (p.store_id IS NOT NULL AND p.store_id<>m.store_id))", Integer.class),
                "工资行 payout_id 悬空或跨门店指向错误台账");
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM payroll_payout_record p WHERE p.headcount<>"
                        + "(SELECT COUNT(*) FROM month_salary m WHERE m.payout_id=p.payout_id) "
                        + "OR p.total_net<>(SELECT COALESCE(SUM(m.net_salary),0) FROM month_salary m "
                        + "WHERE m.payout_id=p.payout_id)", Integer.class),
                "台账 headcount/total_net 与本批工资行不一致");
    }

    // ============ 验收 3：核算保存 → 审批角色审批 → 付款角色付款 → 刷新回读，三方一致 ============

    @Test
    void saveApprovePayoutReadbackAcrossDistinctRoles() throws Exception {
        String hrTok = token(HR, 1, "tlpay59_hr");
        String approverTok = token(APPROVER, 1, APPROVER_ACCT);
        String payerTok = token(PAYER, 1, "tlpay59_payer");

        // 核算保存
        JsonNode save = postAs("save", hrTok, items((int) WORKER_A, (int) WORKER_B), 200);
        assertEquals(2, ((Number) save.path("saved").asInt()).intValue());
        Map<String, Object> a = jdbc.queryForMap(
                "SELECT base_salary, gross_salary, net_salary, status FROM month_salary WHERE staff_id=?", WORKER_A);
        assertEquals(1, ((Number) a.get("status")).intValue(), "保存后状态应为 1=已保存");
        assertEquals(0, new BigDecimal("1595.00").compareTo((BigDecimal) a.get("net_salary")), "保存后实发应为 1595.00");

        // 审批角色审批
        JsonNode approve = postAs("approve", approverTok, null, 200);
        assertEquals(2, ((Number) approve.path("approved").asInt()).intValue());
        assertEquals(2, jdbc.queryForObject("SELECT status FROM month_salary WHERE staff_id=?", Integer.class, WORKER_A));
        assertEquals(APPROVER_ACCT, jdbc.queryForObject(
                "SELECT approved_by FROM month_salary WHERE staff_id=?", String.class, WORKER_A));

        // 付款角色付款
        JsonNode payout = postAs("payout", payerTok, null, 200);
        assertEquals(2, ((Number) payout.path("paid").asInt()).intValue());
        assertEquals(3, jdbc.queryForObject("SELECT status FROM month_salary WHERE staff_id=?", Integer.class, WORKER_A));
        assertEquals("tlpay59_payer", jdbc.queryForObject(
                "SELECT paid_by FROM month_salary WHERE staff_id=?", String.class, WORKER_A));

        // 刷新回读（审批人全门店读回）
        MockHttpServletResponse resp = mvc.perform(get("/api/hr/payroll").param("month", MONTH)
                .header("Authorization", "Bearer " + approverTok)).andReturn().getResponse();
        assertEquals(200, resp.getStatus());
        JsonNode root = json.readTree(resp.getContentAsString());
        assertEquals(200, root.path("code").asInt());
        JsonNode rowA = null;
        for (JsonNode r : root.path("data")) {
            if (r.path("emp_id").asInt() == (int) WORKER_A) rowA = r;
        }
        assertNotNull(rowA, "回读结果应包含员工甲");
        assertEquals(3, rowA.path("salary_status").asInt(), "回读状态应为 3=已发放记账");
        assertEquals(0, new BigDecimal("1595").compareTo(rowA.path("net_pay").decimalValue()), "回读实发应等于落库实发");

        // 金额一致：台账 total_net == 库内实发合计
        BigDecimal ledger = jdbc.queryForObject("SELECT total_net FROM payroll_payout_record", BigDecimal.class);
        BigDecimal actual = jdbc.queryForObject("SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);
        assertEquals(0, ledger.compareTo(actual), "台账合计与库内实发合计不一致");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
        conserved();
    }

    // ============ 验收 4a：停用账号被拒绝（HTTP 401，零写入） ============

    @Test
    void disabledAccountRejected() throws Exception {
        String disabledTok = token(DISABLED, 1, "tlpay59_disabled");
        MockHttpServletResponse save = rawPost("save", disabledTok, items((int) WORKER_A));
        assertEquals(401, save.getStatus(), "停用账号应被拦截器 401 拒绝");
        MockHttpServletResponse approve = rawPost("approve", disabledTok, null);
        assertEquals(401, approve.getStatus());
        MockHttpServletResponse payout = rawPost("payout", disabledTok, null);
        assertEquals(401, payout.getStatus());
        MockHttpServletResponse read = mvc.perform(get("/api/hr/payroll").param("month", MONTH)
                .header("Authorization", "Bearer " + disabledTok)).andReturn().getResponse();
        assertEquals(401, read.getStatus());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM month_salary", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
    }

    // ============ 验收 4b：无权限角色被拒绝（Result.code 403，零工资写入） ============

    @Test
    void noPermissionRoleRejected() throws Exception {
        String noPermTok = token(NO_PERM, 1, "tlpay59_noperm");
        MockHttpServletResponse save = rawPost("save", noPermTok, items((int) WORKER_A));
        assertEquals(200, save.getStatus(), "业务拒绝走 Result.code，HTTP 仍为 200");
        assertEquals(403, json.readTree(save.getContentAsString()).path("code").asInt(),
                "无权限角色应得业务码 403");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM month_salary", Integer.class),
                "无权限角色不得写入任何工资行");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
    }

    // ============ 验收 4c：跨门店保存被拒绝（Result.code 400，整批回滚） ============

    @Test
    void crossStoreRejected() throws Exception {
        String mgrTok = token(STORE_MGR, 1, "tlpay59_mgr");
        MockHttpServletResponse save = rawPost("save", mgrTok, items((int) OTHER_STORE_WORKER));
        assertEquals(200, save.getStatus(), "业务拒绝走 Result.code，HTTP 仍为 200");
        assertEquals(400, json.readTree(save.getContentAsString()).path("code").asInt(),
                "跨门店保存应得业务码 400");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM month_salary", Integer.class),
                "跨门店保存必须整批回滚，一行都不写");
    }

    // ============ 验收 4d：重复付款被拒绝（alreadyRecorded，台账仅一条，金额不变） ============

    @Test
    void duplicatePayoutRejected() throws Exception {
        postAs("save", token(HR, 1, "tlpay59_hr"), items((int) WORKER_A), 200);
        postAs("approve", token(APPROVER, 1, APPROVER_ACCT), null, 200);
        JsonNode first = postAs("payout", token(PAYER, 1, "tlpay59_payer"), null, 200);
        assertEquals(1, ((Number) first.path("paid").asInt()).intValue());

        List<Map<String, Object>> beforeSalary = jdbc.queryForList("SELECT * FROM month_salary ORDER BY salary_id");
        List<Map<String, Object>> beforeLedger = jdbc.queryForList("SELECT * FROM payroll_payout_record");
        BigDecimal netBefore = jdbc.queryForObject("SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class);

        JsonNode dup = postAs("payout", token(PAYER, 1, "tlpay59_payer"), null, 200);
        assertTrue(dup.path("alreadyRecorded").asBoolean(), "重复付款应明确回 alreadyRecorded");
        assertEquals(0, ((Number) dup.path("paid").asInt()).intValue(), "重复付款不得再记一批");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record", Integer.class),
                "财务流水（台账）必须仍只有一条");
        assertEquals(beforeSalary, jdbc.queryForList("SELECT * FROM month_salary ORDER BY salary_id"), "工资行被改动");
        assertEquals(beforeLedger, jdbc.queryForList("SELECT * FROM payroll_payout_record"), "台账被改动");
        assertEquals(netBefore, jdbc.queryForObject("SELECT SUM(net_salary) FROM month_salary WHERE status=3", BigDecimal.class));
        conserved();
    }

    // ============ 验收 5：无孤儿 + 业务键回读恢复 + 不重复扣款 ============

    @Test
    void noOrphansAndBusinessKeyRecovery() throws Exception {
        postAs("save", token(HR, 1, "tlpay59_hr"), items((int) WORKER_A, (int) WORKER_B), 200);
        postAs("approve", token(APPROVER, 1, APPROVER_ACCT), null, 200);
        JsonNode payout = postAs("payout", token(PAYER, 1, "tlpay59_payer"), null, 200);
        long payoutId = payout.path("payoutId").asLong();
        assertTrue(payoutId > 0);

        conserved(); // 工资/审批/付款/台账 之间无孤儿、金额守恒

        // 未知结果用业务键 (staff_id, salary_month) 回读恢复，指向同一条台账且状态一致
        for (long wid : new long[]{WORKER_A, WORKER_B}) {
            Map<String, Object> m = jdbc.queryForMap(
                    "SELECT status, payout_id, net_salary FROM month_salary WHERE staff_id=? AND salary_month=?",
                    wid, MONTH);
            assertEquals(3, ((Number) m.get("status")).intValue(), "员工 " + wid + " 应为已发放记账");
            assertEquals(payoutId, ((Number) m.get("payout_id")).longValue(), "员工 " + wid + " 应指向唯一台账");
        }
        BigDecimal byBatch = jdbc.queryForObject(
                "SELECT SUM(net_salary) FROM month_salary WHERE payout_id=?", BigDecimal.class, payoutId);
        BigDecimal ledger = jdbc.queryForObject(
                "SELECT total_net FROM payroll_payout_record WHERE payout_id=?", BigDecimal.class, payoutId);
        assertEquals(0, byBatch.compareTo(ledger), "本批工资合计应与台账合计一致");

        // 不重复扣款：重复付款只回 alreadyRecorded，不新增台账
        JsonNode again = postAs("payout", token(PAYER, 1, "tlpay59_payer"), null, 200);
        assertTrue(again.path("alreadyRecorded").asBoolean());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record", Integer.class));
        conserved();
    }
}
