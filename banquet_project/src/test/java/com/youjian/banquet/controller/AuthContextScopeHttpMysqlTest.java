package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.aop.AuditLogAspect;
import com.youjian.banquet.aop.StoreDataScopeAspect;
import com.youjian.banquet.auth.StaffRealtimeGuard;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-AUTH-CONTEXT-15：实时复核的结论必须一路贯通到业务数据范围与审计身份。
 * <p>
 * 上一个任务把实时复核加在了 JwtAuthInterceptor 上，但 StoreDataScopeAspect 与
 * AuditLogAspect 各自拿 Authorization 头<b>重新解析一遍 token</b> 来填 UserContext——
 * 等于绕过刚做完的复核，把签发那一刻的快照又当成了当前权威。
 * 于是"人调了店、降了权"之后，凭 UserContext 判门店范围和写审计的代码还在用旧值。
 * <p>
 * 这套件验的就是这条链：<b>拦截器复核 → request 属性 → UserContext → 读范围 / 写归属 / 审计人</b>。
 * 用的是真实隔离 MySQL、真实 HTTP、真实签发的 JWT、真实切面，
 * 被测接口是真正依赖 UserContext 门店范围的 {@code GET/POST /api/finance/account}。
 * <p>
 * 全部账号为本进程现造的合成账号；不使用、不写入、不猜任何真实账号，
 * 不访问也不修改任何法务接口与数据。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthContextScopeHttpMysqlTest {

    static final String SECRET = "youjian-synthetic-test-secret-key-0123456789abcdef";
    static final long STORE_ONE = 1L, STORE_TWO = 2L;
    static final int MOVER = 401, DEMOTED = 402, LEAVER = 403, STAYER = 404;

    JdbcTemplate jdbc;
    MockMvc mvc;
    AnnotationConfigApplicationContext context;
    final ObjectMapper json = new ObjectMapper();
    String schema;

    @Configuration
    @EnableAspectJAutoProxy
    static class Wiring {
        static JdbcTemplate shared;

        @Bean JdbcTemplate jdbcTemplate() { return shared; }

        @Bean FinanceController financeController() { return new FinanceController(); }

        @Bean JwtAuthInterceptor jwtAuthInterceptor() { return new JwtAuthInterceptor(); }

        @Bean StaffRealtimeGuard staffRealtimeGuard() { return new StaffRealtimeGuard(shared); }

        @Bean StoreDataScopeAspect storeDataScopeAspect() { return new StoreDataScopeAspect(); }

        @Bean AuditLogAspect auditLogAspect() { return new AuditLogAspect(); }

    }

    @BeforeAll
    void start() {
        schema = "auth_ctx_" + UUID.randomUUID().toString().replace("-", "");
        String host = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(host + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("AUTH_CONTEXT_EVIDENCE schema=" + schema + " retained=true");

        jdbc = new JdbcTemplate(new DriverManagerDataSource(host + schema + opts, "root", ""));

        jdbc.execute("CREATE TABLE staff_master(staff_id INT PRIMARY KEY, store_id BIGINT, "
                + "staff_name VARCHAR(50), role VARCHAR(30), employment_status VARCHAR(20))");
        jdbc.execute("CREATE TABLE finance_account(account_id BIGINT PRIMARY KEY, store_id BIGINT, "
                + "account_code VARCHAR(40), account_name VARCHAR(100), account_type VARCHAR(30), "
                + "initial_balance DECIMAL(12,2), current_balance DECIMAL(12,2), is_active INT DEFAULT 1, "
                + "sort_order INT DEFAULT 0, created_at DATETIME)");
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(60), "
                + "action VARCHAR(200), target VARCHAR(200), detail TEXT, store_id BIGINT)");

        staff(MOVER, STORE_ONE, "manager", "active", "合成调店员工");
        staff(DEMOTED, STORE_ONE, "manager", "active", "合成降权员工");
        staff(LEAVER, STORE_ONE, "manager", "active", "合成离职员工");
        staff(STAYER, STORE_ONE, "manager", "active", "合成在职员工");

        account(9001, STORE_ONE, "一店现金");
        account(9002, STORE_TWO, "二店现金");

        Wiring.shared = jdbc;
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getSystemProperties().put("jwt.secret", SECRET);
        // 账户这几个接口用不到这两个服务。必须用 registerSingleton 而不是 @Bean：
        // 走 @Bean 的话 Spring 仍会对替身做字段注入，反而把应收应付整条仓储依赖链拖进来。
        context.getBeanFactory().registerSingleton("financePayableService",
                org.mockito.Mockito.mock(com.youjian.banquet.service.FinancePayableService.class));
        context.getBeanFactory().registerSingleton("receivablePaymentService",
                org.mockito.Mockito.mock(com.youjian.banquet.service.ReceivablePaymentService.class));
        context.register(Wiring.class);
        context.refresh();

        mvc = MockMvcBuilders.standaloneSetup(context.getBean(FinanceController.class))
                .addMappedInterceptors(new String[]{"/api/finance/**"},
                        context.getBean(JwtAuthInterceptor.class))
                .build();
    }

    @AfterAll
    void stop() {
        if (context != null) context.close();
        System.out.println("AUTH_CONTEXT_EVIDENCE schema=" + schema + " retained=true");
    }

    @AfterEach
    void noContextLeak() {
        assertNull(UserContext.get(), "请求结束后 UserContext 没有被清理");
    }

    // ==================== 夹具 ====================

    void staff(int staffId, long storeId, String role, String employment, String name) {
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,role,employment_status) "
                + "VALUES (?,?,?,?,?)", staffId, storeId, name, role, employment);
    }

    void account(long id, long storeId, String name) {
        jdbc.update("INSERT INTO finance_account(account_id,store_id,account_code,account_name,"
                + "account_type,initial_balance,current_balance,is_active,sort_order,created_at) "
                + "VALUES (?,?,?,?,'cash',0,0,1,0,NOW())", id, storeId, "ACC" + id, name);
    }

    /**
     * 真实签发的 JWT。<b>storeId 与 role 刻意写成签发那一刻的旧值</b>——
     * 本套件要证明的正是"库里改了之后，token 里的旧值不再作数"。
     */
    String token(long staffId, long storeIdAtIssue, String roleAtIssue) {
        return Jwts.builder().subject("syn_" + staffId)
                .claim("staffId", staffId).claim("storeId", storeIdAtIssue).claim("role", roleAtIssue)
                .expiration(new Date(System.currentTimeMillis() + 600000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    record Reply(int status, String raw, JsonNode body) { }

    Reply get(String path, String bearer) throws Exception {
        var rb = MockMvcRequestBuilders.get(path);
        if (bearer != null) rb = rb.header("Authorization", "Bearer " + bearer);
        return send(rb);
    }

    Reply post(String path, Map<String, Object> body, String bearer) throws Exception {
        var rb = MockMvcRequestBuilders.post(path)
                .contentType("application/json").content(json.writeValueAsString(body));
        if (bearer != null) rb = rb.header("Authorization", "Bearer " + bearer);
        return send(rb);
    }

    /** 响应体按 UTF-8 读原始字节：MockMvc 默认 ISO-8859-1 解码，中文会乱。 */
    Reply send(org.springframework.test.web.servlet.RequestBuilder rb) throws Exception {
        var response = mvc.perform(rb).andReturn().getResponse();
        String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        return new Reply(response.getStatus(), raw, raw.isEmpty() ? null : json.readTree(raw));
    }

    Set<Long> visibleAccountStores(String bearer) throws Exception {
        Reply reply = get("/api/finance/account", bearer);
        assertEquals(200, reply.status(), reply.raw());
        Set<Long> stores = new HashSet<>();
        for (JsonNode row : reply.body().path("data")) stores.add(row.path("store_id").asLong());
        return stores;
    }

    long countAccounts() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM finance_account", Long.class);
    }

    long countAudit() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
    }

    // ==================== 读范围 ====================

    @Test @Order(1)
    @DisplayName("读范围随库走：调店之后，同一个未过期 token 只看得到新门店的账户")
    void readScopeFollowsTheDatabaseNotTheToken() throws Exception {
        // token 里冻着 storeId=1。若下游还认 token，调店之后这个人仍然看一店的数据。
        String bearer = token(MOVER, STORE_ONE, "manager");
        assertEquals(Set.of(STORE_ONE), visibleAccountStores(bearer), "调店前应只看到一店");

        jdbc.update("UPDATE staff_master SET store_id=? WHERE staff_id=?", STORE_TWO, MOVER);

        assertEquals(Set.of(STORE_TWO), visibleAccountStores(bearer),
                "调店后仍按 token 里的旧门店取数——说明数据范围还在用签发时的快照");
    }

    @Test @Order(2)
    @DisplayName("请求参数改不动门店：普通角色传 storeId=1 也只拿得到自己门店的数据")
    void requestParameterCannotWidenTheScope() throws Exception {
        String bearer = token(MOVER, STORE_ONE, "manager");   // 此人已被调到二店
        Reply reply = get("/api/finance/account?storeId=" + STORE_ONE, bearer);
        assertEquals(200, reply.status(), reply.raw());
        Set<Long> stores = new HashSet<>();
        for (JsonNode row : reply.body().path("data")) stores.add(row.path("store_id").asLong());
        assertEquals(Set.of(STORE_TWO), stores, "请求参数把门店范围撑开了：" + reply.raw());
    }

    // ==================== 写归属与审计 ====================

    @Test @Order(3)
    @DisplayName("写归属随库走：调店后新建的账户落在新门店，审计记录记的也是新门店")
    void writeAttributionAndAuditFollowTheDatabase() throws Exception {
        String bearer = token(MOVER, STORE_ONE, "manager");   // token 里仍是一店
        long auditBefore = countAudit();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountName", "合成新账户");
        body.put("accountType", "cash");
        Reply reply = post("/api/finance/account", body, bearer);
        assertEquals(200, reply.body().path("code").asInt(), reply.raw());
        long accountId = reply.body().path("data").path("accountId").asLong();

        // 证据一：新建的账户落在库里当前门店（二店），不是 token 里的一店
        assertEquals(STORE_TWO, (long) jdbc.queryForObject(
                        "SELECT store_id FROM finance_account WHERE account_id=?", Long.class, accountId),
                "新建数据落到了 token 里的旧门店");

        // 证据二：审计记录确实写了，且门店取的是复核后的当前门店
        assertEquals(auditBefore + 1, countAudit(), "写操作没有留下审计记录");
        Map<String, Object> audit = jdbc.queryForMap(
                "SELECT user_id, action, target, store_id FROM audit_logs ORDER BY id DESC LIMIT 1");
        assertEquals(STORE_TWO, ((Number) audit.get("store_id")).longValue(),
                "审计记的是旧门店：" + audit);
        assertTrue(String.valueOf(audit.get("target")).contains("createAccount"),
                "审计记错了操作对象：" + audit);
        assertEquals(String.valueOf(MOVER), String.valueOf(audit.get("user_id")),
                "审计人不是当前操作人：" + audit);
    }

    // ==================== 降权 / 停用 ====================

    @Test @Order(4)
    @DisplayName("降权立即生效：角色被改成 lawyer 后，同一 token 打财务接口 403 且零写入")
    void roleDowngradeBlocksBusinessAccess() throws Exception {
        // 只验被拒这一侧，不访问任何 /api/legal/ 接口，不读写法务数据。
        String bearer = token(DEMOTED, STORE_ONE, "manager");
        assertEquals(200, get("/api/finance/account", bearer).status(), "降权前就进不去，对照不成立");

        jdbc.update("UPDATE staff_master SET role='lawyer' WHERE staff_id=?", DEMOTED);

        long accounts = countAccounts();
        assertEquals(403, get("/api/finance/account", bearer).status(),
                "改成 lawyer 后仍能读财务数据——角色取的还是 token 里的快照");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountName", "降权后不该建出来的账户");
        assertEquals(403, post("/api/finance/account", body, bearer).status());
        assertEquals(accounts, countAccounts(), "降权后仍然写进去了");
    }

    @Test @Order(5)
    @DisplayName("停用立即生效：同一 token 读写都 401，且零写入、不留审计")
    void deactivationBlocksEverything() throws Exception {
        String bearer = token(LEAVER, STORE_ONE, "manager");
        assertEquals(200, get("/api/finance/account", bearer).status(), "停用前就进不去，对照不成立");

        jdbc.update("UPDATE staff_master SET employment_status='resigned' WHERE staff_id=?", LEAVER);

        long accounts = countAccounts(), audits = countAudit();
        assertEquals(401, get("/api/finance/account", bearer).status());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountName", "停用后不该建出来的账户");
        assertEquals(401, post("/api/finance/account", body, bearer).status());
        assertEquals(accounts, countAccounts(), "停用后仍然写进去了");
        assertEquals(audits, countAudit(), "被拒的请求不该留下审计记录");
    }

    // ==================== 缺已验证属性：fail-closed ====================

    @Test @Order(6)
    @DisplayName("鉴权链没挂上时带 Bearer 头进来：切面拒绝执行，不以「没有身份」继续跑")
    void missingVerifiedAttributesIsRefused() throws Exception {
        // 继续跑意味着门店范围与审计人全部落空，而落空往往被下游当成"不限门店"。
        // 这里刻意不挂拦截器，模拟配置疏漏。
        // 挂上生产用的全局异常处理，好按生产的实际形状断言：拒绝对外是 500，
        // 且响应体只有一句通用文案，不把内部原因吐出去。
        MockMvc unguarded = MockMvcBuilders
                .standaloneSetup(context.getBean(FinanceController.class))
                .setControllerAdvice(new com.youjian.banquet.exception.GlobalExceptionHandler())
                .build();
        String bearer = token(STAYER, STORE_ONE, "manager");

        long accounts = countAccounts();
        var result = unguarded.perform(MockMvcRequestBuilders.post("/api/finance/account")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType("application/json")
                        .content("{\"accountName\":\"没有身份不该建出来的账户\"}"))
                .andReturn();

        assertEquals(500, result.getResponse().getStatus(), "缺已验证属性却照常执行了");
        String raw = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertFalse(raw.contains("SecurityException"), "把内部异常名吐给了调用方：" + raw);
        assertFalse(raw.contains("身份属性"), "把内部原因吐给了调用方：" + raw);
        assertEquals(accounts, countAccounts(), "缺身份的请求仍然写进去了");
        assertEquals(0L, (long) jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_logs WHERE detail LIKE '%没有身份不该建出来的账户%'", Long.class),
                "被拒的请求不该留下审计记录");
    }

    @Test @Order(7)
    @DisplayName("客户端自报的门店/员工头部一文不值：伪造 X-Store-Id 与 X-Staff-Id 照样 401")
    void clientSuppliedHeadersGrantNothing() throws Exception {
        // iPad 分支只信 IpadInterceptor 在校验设备绑定后写入的 request 属性，
        // 从不信任客户端头部。这条是核实既有实现，不是本轮新改的。
        var response = mvc.perform(MockMvcRequestBuilders.get("/api/finance/account")
                .header("X-Store-Id", "1").header("X-Staff-Id", "1")).andReturn().getResponse();
        assertEquals(401, response.getStatus(), "客户端自报头部竟然换来了访问权");

        var alsoWithBadToken = mvc.perform(MockMvcRequestBuilders.get("/api/finance/account")
                .header("Authorization", "Bearer not-a-real-token")
                .header("X-Store-Id", "1").header("X-Staff-Id", "1")).andReturn().getResponse();
        assertEquals(401, alsoWithBadToken.getStatus());
    }

    @Test @Order(8)
    @DisplayName("在职未变的人不受影响：读写照常，审计照记——收紧没有误伤正常路径")
    void normalStaffStillWorks() throws Exception {
        String bearer = token(STAYER, STORE_ONE, "manager");
        assertEquals(Set.of(STORE_ONE), visibleAccountStores(bearer));

        long audits = countAudit();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountName", "在职员工建的账户");
        Reply reply = post("/api/finance/account", body, bearer);
        assertEquals(200, reply.body().path("code").asInt(), reply.raw());
        assertEquals(STORE_ONE, (long) jdbc.queryForObject(
                "SELECT store_id FROM finance_account WHERE account_id=?", Long.class,
                reply.body().path("data").path("accountId").asLong()));
        assertEquals(audits + 1, countAudit());
    }
}
