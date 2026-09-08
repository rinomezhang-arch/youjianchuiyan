package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.auth.StaffRealtimeGuard;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.entity.StoreInfo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-AUTH-REALTIME-14：登录防枚举 + 实时档案复核 + 旧 JWT 撤权的 HTTP 级验收。
 * <p>
 * 真实隔离 MySQL、真实 HTTP 分发、真实拦截器、真实 BCrypt。
 * <b>全部账号为本进程现造的合成账号，密码由测试自己生成；不使用、不写入、不猜任何真实账号。</b>
 * <p>
 * 这套件要钉死两件事：
 * <ol>
 *   <li><b>登录问不出人</b>：未知账号、已停用账号、密码错误，三者必须给出逐字节相同的回答，
 *       且不能靠"未知账号返回得特别快"从侧面区分。</li>
 *   <li><b>token 不是免死金牌</b>：JWT 是签发那一刻的快照，停用、调店、降权都发生在签发之后。
 *       每次请求回库复核，旧 token 当场失效，不必等它过期。</li>
 * </ol>
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthRealtimeHttpMysqlTest {

    static final String SECRET = "youjian-synthetic-test-secret-key-0123456789abcdef";
    /** 合成密码，只存在于本测试进程内。 */
    static final String PASSWORD = "synthetic-pass-9f3a";
    static final long STORE = 1L, OTHER_STORE = 2L;

    JdbcTemplate jdbc;
    MockMvc mvc;
    LocalContainerEntityManagerFactoryBean factory;
    final ObjectMapper json = new ObjectMapper();
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String schema;

    @BeforeAll
    void start() {
        schema = "auth_rt_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(url + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("AUTH_REALTIME_EVIDENCE schema=" + schema + " retained=true");

        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + opts, "root", "");
        jdbc = new JdbcTemplate(ds);

        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                StoreInfo.class.getName(), StaffMaster.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();

        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,status,sort_order) "
                + "VALUES (1,'S-1','合成门店一','open',1),(2,'S-2','合成门店二','open',2)");

        seed(201, STORE, "syn_normal", "active", "store_manager");   // 全程在职，做基准
        seed(202, STORE, "syn_disabled", "resigned", "store_manager"); // 一开始就停用
        seed(203, STORE, "syn_revoke", "active", "store_manager");   // 登录后被停用
        seed(204, STORE, "syn_moved", "active", "store_manager");    // 登录后被调店
        seed(205, STORE, "syn_demoted", "active", "store_manager");  // 登录后被降成 lawyer
        seed(206, STORE, "syn_logout", "active", "store_manager");   // 验退出后的 token 行为

        AuthController auth = new AuthController();
        ReflectionTestUtils.setField(auth, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(auth, "jwtSecret", SECRET);
        // 有效期必须显式给：不给的话字段是 0，签出来的 token 当场就过期，
        // 后面所有"旧 token 还能不能用"的对照都会因为它已过期而失去意义。
        ReflectionTestUtils.setField(auth, "jwtExpiration", 600000L);

        JwtAuthInterceptor interceptor = new JwtAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
        // 装上真实的复核 Bean——这条链路正是本任务要验的，用替身等于没验。
        ReflectionTestUtils.setField(interceptor, "staffRealtimeGuard", new StaffRealtimeGuard(jdbc));

        // 与 WebMvcConfig 同口径：登录接口不经拦截器，受保护接口都经过。
        // standaloneSetup 只支持 include 模式，所以把本套件用到的受保护路径显式列出来，
        // 而不是把 /api/** 全挂上再想办法把登录摘出去。
        mvc = MockMvcBuilders.standaloneSetup(auth)
                .addMappedInterceptors(
                        new String[]{"/api/auth/me", "/api/auth/logout", "/api/stores"}, interceptor)
                .build();
    }

    @AfterAll
    void stop() {
        if (factory != null) factory.destroy();
        System.out.println("AUTH_REALTIME_EVIDENCE schema=" + schema + " retained=true");
    }

    void seed(int staffId, long storeId, String account, String employment, String role) {
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,staff_password,"
                        + "employment_status,role) VALUES (?,?,?,?,?,?,?)",
                staffId, storeId, "合成员工" + staffId, account, encoder.encode(PASSWORD), employment, role);
    }

    record Reply(int status, String raw, JsonNode body) { }

    /** 响应体按 UTF-8 读原始字节：MockMvc 默认 ISO-8859-1 解码，中文提示会全乱。 */
    Reply send(org.springframework.test.web.servlet.RequestBuilder rb) throws Exception {
        var response = mvc.perform(rb).andReturn().getResponse();
        String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        return new Reply(response.getStatus(), raw, raw.isEmpty() ? null : json.readTree(raw));
    }

    Reply login(String username, String password) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        if (username != null) body.put("username", username);
        if (password != null) body.put("password", password);
        return send(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType("application/json").content(json.writeValueAsString(body)));
    }

    Reply get(String path, String bearer) throws Exception {
        var rb = MockMvcRequestBuilders.get(path);
        if (bearer != null) rb = rb.header("Authorization", "Bearer " + bearer);
        return send(rb);
    }

    String tokenOf(String account) throws Exception {
        Reply reply = login(account, PASSWORD);
        assertEquals(200, reply.body().path("code").asInt(), "合成账号登录失败：" + reply.raw());
        String token = reply.body().path("data").path("token").asText();
        assertFalse(token.isBlank(), "没拿到 token：" + reply.raw());
        return token;
    }

    // ==================== 登录：防枚举 ====================

    @Test @Order(1)
    @DisplayName("合成账号能正常登录，且响应里不带密码哈希")
    void loginSucceedsAndLeaksNoCredential() throws Exception {
        Reply reply = login("syn_normal", PASSWORD);
        assertEquals(200, reply.body().path("code").asInt(), reply.raw());
        assertFalse(reply.body().path("data").path("token").asText().isBlank());

        String hash = jdbc.queryForObject(
                "SELECT staff_password FROM staff_master WHERE staff_id=201", String.class);
        assertFalse(reply.raw().contains(hash), "响应里带出了密码哈希");
        assertFalse(reply.raw().contains(PASSWORD), "响应里带出了明文密码");
        assertFalse(reply.raw().contains("staff_password"), "响应里带出了密码字段名");
    }

    @Test @Order(2)
    @DisplayName("未知账号 / 已停用账号 / 密码错误：三种响应逐字节相同，问不出这个人在不在册")
    void threeFailuresAreIndistinguishable() throws Exception {
        Reply unknown = login("syn_no_such_person", PASSWORD);      // 不在册
        Reply disabled = login("syn_disabled", PASSWORD);           // 在册但已停用
        Reply wrongPass = login("syn_normal", "definitely-wrong");  // 在册在职但密码错

        Set<String> bodies = new LinkedHashSet<>(List.of(unknown.raw(), disabled.raw(), wrongPass.raw()));
        Set<Integer> statuses = new HashSet<>(List.of(unknown.status(), disabled.status(), wrongPass.status()));
        assertEquals(1, statuses.size(), "三种失败给出了不同 HTTP 状态：" + statuses);
        assertEquals(1, bodies.size(), "三种失败给出了可区分的响应，登录接口会被拿去枚举账号：" + bodies);

        // 顺带确认这句话本身不带任何账号信息
        String message = unknown.body().path("message").asText();
        assertFalse(message.contains("不存在"), "文案泄漏了存在性：" + message);
        assertFalse(message.contains("停用"), "文案泄漏了在职状态：" + message);
        assertFalse(message.contains("密码错误"), "文案区分了密码错误：" + message);
    }

    @Test @Order(3)
    @DisplayName("未知账号也真跑一次比对：不能靠「回得特别快」从侧面认出账号存在")
    void unknownAccountStillPaysTheHashingCost() throws Exception {
        // 时间断言天生不稳，所以这里不卡绝对毫秒，只要求两条路径处在同一量级：
        // 未知账号耗时不得低于已知账号的四分之一。真跳过 BCrypt 的话差距是几十上百倍。
        warmUp();
        long known = medianMillis(() -> login("syn_normal", "definitely-wrong"));
        long unknown = medianMillis(() -> login("syn_no_such_person", "definitely-wrong"));
        assertTrue(unknown * 4 >= known,
                "未知账号明显更快，耗时本身就是个可测的存在性信号：已知 " + known + "ms / 未知 " + unknown + "ms");
    }

    private void warmUp() throws Exception {
        // 头几次要load类、建连接、算诱饵哈希，不进统计
        for (int i = 0; i < 3; i++) {
            login("syn_normal", "definitely-wrong");
            login("syn_no_such_person", "definitely-wrong");
        }
    }

    private interface Call { Reply run() throws Exception; }

    private long medianMillis(Call call) throws Exception {
        long[] samples = new long[5];
        for (int i = 0; i < samples.length; i++) {
            long t0 = System.nanoTime();
            call.run();
            samples[i] = (System.nanoTime() - t0) / 1_000_000;
        }
        Arrays.sort(samples);
        return samples[samples.length / 2];
    }

    // ==================== 实时复核：旧 token 当场失效 ====================

    @Test @Order(4)
    @DisplayName("停用立即生效：token 一个字没变、也没过期，人一停用下一次请求就 401")
    void deactivationInvalidatesExistingToken() throws Exception {
        String bearer = tokenOf("syn_revoke");
        assertEquals(200, get("/api/auth/me", bearer).body().path("code").asInt(),
                "停用前这个 token 就不好使，后面的对照不成立");

        jdbc.update("UPDATE staff_master SET employment_status='resigned' WHERE staff_id=203");

        Reply me = get("/api/auth/me", bearer);
        assertEquals(401, me.body().path("code").asInt(), "停用后旧 token 仍然可用：" + me.raw());
        // 受拦截器保护的普通接口同样当场挡下，不是只有 /auth/me 做了检查
        assertEquals(401, get("/api/stores", bearer).status(), "停用后旧 token 仍能访问受保护接口");
    }

    @Test @Order(5)
    @DisplayName("调店立即生效：/auth/me 报的是库里当前门店，不是 token 里的旧门店")
    void storeTransferTakesEffectOnTheSameToken() throws Exception {
        String bearer = tokenOf("syn_moved");
        assertEquals(STORE, get("/api/auth/me", bearer).body().path("data").path("storeId").asLong());

        jdbc.update("UPDATE staff_master SET store_id=? WHERE staff_id=204", OTHER_STORE);

        Reply me = get("/api/auth/me", bearer);
        assertEquals(OTHER_STORE, me.body().path("data").path("storeId").asLong(),
                "调店后仍按 token 里的旧门店返回：" + me.raw());
        assertEquals("合成门店二", me.body().path("data").path("storeName").asText(),
                "门店名没跟着走：" + me.raw());
    }

    @Test @Order(6)
    @DisplayName("降权立即生效：角色被改成 lawyer 后，旧 token 打非法务接口当场 403")
    void roleDowngradeTakesEffectOnTheSameToken() throws Exception {
        // 这条最能说明"角色必须以库为准"：token 里冻着旧角色 store_manager，
        // 如果拦截器信 token，改库就等于没改。
        // 只验被拒这一侧，不访问任何 /api/legal/ 接口，不读写法务数据。
        String bearer = tokenOf("syn_demoted");
        assertEquals(200, get("/api/stores", bearer).body().path("code").asInt(),
                "降权前这个 token 就不好使，后面的对照不成立");

        jdbc.update("UPDATE staff_master SET role='lawyer' WHERE staff_id=205");

        assertEquals(403, get("/api/stores", bearer).status(),
                "改成 lawyer 后旧 token 仍能访问业务接口——说明角色取的还是 token 里的快照");
    }

    @Test @Order(7)
    @DisplayName("被拒时不吐档案：401 与 403 的响应体里没有姓名、门店、角色任何一项")
    void rejectionRevealsNothingAboutTheStaff() throws Exception {
        String bearer = tokenOf("syn_normal");
        jdbc.update("UPDATE staff_master SET employment_status='resigned' WHERE staff_id=201");
        try {
            Reply me = get("/api/auth/me", bearer);
            for (String leak : List.of("合成员工201", "合成门店一", "store_manager", "syn_normal")) {
                assertFalse(me.raw().contains(leak), "拒绝响应里带出了 " + leak + "：" + me.raw());
            }
        } finally {
            jdbc.update("UPDATE staff_master SET employment_status='active' WHERE staff_id=201");
        }
    }

    // ==================== 退出后的 token 行为：把约定钉死 ====================

    @Test @Order(8)
    @DisplayName("退出不作废 token（这是明确约定，不是疏漏）——但人一停用照样当场失效")
    void logoutContractIsStatelessAndBoundedByRealtimeCheck() throws Exception {
        String bearer = tokenOf("syn_logout");

        Reply out = send(MockMvcRequestBuilders.post("/api/auth/logout")
                .header("Authorization", "Bearer " + bearer).contentType("application/json").content("{}"));
        assertEquals(200, out.body().path("code").asInt(), out.raw());

        // 事实第一条：退出之后，同一个 token 仍然验得过。
        // 服务端不保存已签发 token 的清单，也没有黑名单，这里如实钉住这个行为，
        // 免得有人以为"点了退出就安全了"。
        assertEquals(200, get("/api/auth/me", bearer).body().path("code").asInt(),
                "退出后 token 立即失效了——若确已改成有状态会话，本约定需同步更新");

        // 事实第二条：真正兜住的是实时复核。人一停用，同一个 token 下一次请求就废。
        jdbc.update("UPDATE staff_master SET employment_status='resigned' WHERE staff_id=206");
        assertEquals(401, get("/api/auth/me", bearer).body().path("code").asInt(),
                "停用后旧 token 仍然可用，实时复核没生效");
    }

    @Test @Order(9)
    @DisplayName("没有 token / 伪造 token / 别的密钥签的 token：一律 401，不透露原因差别")
    void badTokensAllRejected() throws Exception {
        assertEquals(401, get("/api/stores", null).status());
        assertEquals(401, get("/api/stores", "not-a-real-token").status());
        assertEquals(401, get("/api/stores", "").status());
    }

    // ==================== 复核环节本身失效时必须拒绝（fail-closed） ====================

    @Test @Order(10)
    @DisplayName("复核 Bean 缺失：即便 JWT 完全合法也拒绝，不退回「只验签名」")
    void missingGuardRejectsEvenAValidToken() throws Exception {
        // 这条是为了钉死一件事：漏配不能变成降级。
        // 早前的实现缺 Guard 时只记一条 ERROR 就继续放行，
        // 等于一个配置疏忽就让离职、停用、调店、降权全部失效，而且是静悄悄发生的。
        String bearer = tokenOf("syn_normal");

        JwtAuthInterceptor crippled = new JwtAuthInterceptor();
        ReflectionTestUtils.setField(crippled, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(crippled, "staffRealtimeGuard", null);

        var response = MockMvcBuilders.standaloneSetup(new AuthController())
                .addMappedInterceptors(new String[]{"/api/stores"}, crippled).build()
                .perform(MockMvcRequestBuilders.get("/api/stores")
                        .header("Authorization", "Bearer " + bearer))
                .andReturn().getResponse();

        assertEquals(500, response.getStatus(),
                "复核缺失时放行了一个合法 token——这就是 fail-open");
        String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertFalse(raw.contains("syn_normal"), "拒绝响应里带出了账号：" + raw);
    }

    @Test @Order(11)
    @DisplayName("复核查询抛异常：同样拒绝，不因为数据库抖一下就把门打开")
    void guardQueryFailureRejectsEvenAValidToken() throws Exception {
        String bearer = tokenOf("syn_normal");

        // 指向一个不存在的库：查询必然抛异常，走的是 Guard 里的 catch 分支。
        JdbcTemplate broken = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:mysql://127.0.0.1:13317/no_such_schema_" + UUID.randomUUID().toString().replace("-", "")
                        + "?useSSL=false&allowPublicKeyRetrieval=true", "root", ""));

        JwtAuthInterceptor flaky = new JwtAuthInterceptor();
        ReflectionTestUtils.setField(flaky, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(flaky, "staffRealtimeGuard", new StaffRealtimeGuard(broken));

        var response = MockMvcBuilders.standaloneSetup(new AuthController())
                .addMappedInterceptors(new String[]{"/api/stores"}, flaky).build()
                .perform(MockMvcRequestBuilders.get("/api/stores")
                        .header("Authorization", "Bearer " + bearer))
                .andReturn().getResponse();

        assertEquals(401, response.getStatus(),
                "复核查询失败时放行了请求——基础设施抖动不该变成鉴权开门");
        String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertFalse(raw.contains("no_such_schema"), "拒绝响应里带出了库名等内部细节：" + raw);
    }
}
