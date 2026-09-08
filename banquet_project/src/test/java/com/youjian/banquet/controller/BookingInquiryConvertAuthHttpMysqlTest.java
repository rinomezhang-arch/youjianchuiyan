package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.aop.AuditLogAspect;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.BookingInquiryRepository;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-INQUIRY-CONVERT-AUTH-09 第二轮：转单鉴权的 <b>HTTP 级</b>验收。
 * <p>
 * 与 {@link BookingInquiryConvertMysqlTest} 的分工：那一套直接调方法、手工塞 UserContext，
 * 验的是控制器内部的判定与事务；<b>这一套不碰 UserContext 一个字</b>，
 * 身份完全由真实签发的 JWT 经真实拦截器与真实切面还原出来。
 * <p>
 * 为什么必须这么验：第一轮的鉴权只信 token 里的 storeId。
 * 而 token 是<b>签发那一刻的快照</b>——签完之后员工可能已离职、被停用、被撤权限，
 * token 却还在有效期内。所以第二轮改成回 staff_master 核对在册/在职/权限，
 * 那条「回库核对」的链路只有走完整 HTTP 才算被证明过：
 * 拦截器验签 → AuditLogAspect 还原 UserContext → 控制器回库核档。
 * 少走任何一环，验的都是测试自己塞进去的身份，不是系统真实认定的身份。
 * <p>
 * 六种身份各验一遍：不在册 / 已离职 / 无 HR 权限 / 跨店 / 本店 HR / 总经理。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingInquiryConvertAuthHttpMysqlTest {

    /** 只在本测试进程内使用的合成密钥，与任何环境的真实密钥无关。 */
    static final String SECRET = "youjian-synthetic-test-secret-key-0123456789abcdef";
    static final long STORE = 1L, OTHER_STORE = 2L;

    /** 越权被拒时对外的统一口径：状态码与文案都必须一致，否则接口会被当成存在性探测器。 */
    static final int NOT_VISIBLE_CODE = 404;
    static final String NOT_VISIBLE = "咨询不存在或不属于当前门店";
    static final String NOT_ALLOWED = "当前账号无权操作预约咨询";

    JdbcTemplate jdbc;
    MockMvc mvc;
    LocalContainerEntityManagerFactoryBean factory;
    final ObjectMapper json = new ObjectMapper();
    String schema;
    int nextTable = 100;

    @BeforeAll
    void start() {
        schema = "inq_auth_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(url + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("INQ_AUTH_HTTP_EVIDENCE schema=" + schema + " retained=true");

        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + opts, "root", "");
        jdbc = new JdbcTemplate(ds);

        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                StoreInfo.class.getName(), BookingInquiry.class.getName(),
                BookingMaster.class.getName(), BookingTable.class.getName(),
                TableMaster.class.getName(), StaffMaster.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();

        jdbc.execute("CREATE TABLE IF NOT EXISTS audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id VARCHAR(60),action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,status) "
                + "VALUES (1,'S-1','合成门店一','open'),(2,'S-2','合成门店二','open')");

        // 六种员工档案。差别只在 store_id / employment_status / can_manage_hr 三列上，
        // 其余一律相同——这样任何一处判定失效都会单独暴露，不会被别的差异掩盖。
        seedStaff(101, STORE, "active", 1, "本店有权主管");
        seedStaff(102, STORE, "active", 0, "本店无权员工");
        seedStaff(103, STORE, "resigned", 1, "本店已离职主管");
        seedStaff(104, OTHER_STORE, "active", 1, "二店有权主管");
        seedStaff(105, 0L, "active", 1, "总经理");
        // 106 故意不入库：用来验「token 有效但人不在册」

        JpaTransactionManager manager = new JpaTransactionManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        repositories.addRepositoryProxyPostProcessor((p, i) ->
                p.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource())));

        BookingInquiryController target = new BookingInquiryController();
        ReflectionTestUtils.setField(target, "jdbc", jdbc);
        ReflectionTestUtils.setField(target, "inquiryRepo",
                repositories.getRepository(BookingInquiryRepository.class));
        ReflectionTestUtils.setField(target, "objectMapper", json);

        // 生产里 POST 的 UserContext 是由 AuditLogAspect 还原的（StoreDataScopeAspect 只管 @GetMapping）。
        // 这里装的就是那个真切面，不是替身——否则"身份从 token 还原"这件事等于没验。
        AuditLogAspect audit = new AuditLogAspect();
        ReflectionTestUtils.setField(audit, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(audit, "jdbcTemplate", jdbc);

        // 切面与事务必须挂在**同一个**代理上，且强制 CGLIB。
        // 套两层代理不行：内层 CGLIB 代理类自带 SpringProxy/Advised 等接口，
        // 外层就会退化成 JDK 动态代理，@PostMapping 全丢，MockMvc 一律 404。
        // 顺序：先加切面后加事务 → 切面在外，UserContext 在事务开始前就位、提交后才清。
        AspectJProxyFactory factoryForProxy = new AspectJProxyFactory(target);
        factoryForProxy.setProxyTargetClass(true);
        factoryForProxy.addAspect(audit);
        factoryForProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));

        JwtAuthInterceptor interceptor = new JwtAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
        // 必须先落到 Object 变量再传：standaloneSetup 是 Object... 变长参数，
        // 直接传泛型方法 getProxy() 会把 T 推断成 Object[]，运行期变成 ClassCastException。
        Object controllerProxy = factoryForProxy.getProxy();
        mvc = MockMvcBuilders.standaloneSetup(controllerProxy)
                .addInterceptors(interceptor).build();
    }

    @AfterAll
    void stop() {
        if (factory != null) factory.destroy();
        System.out.println("INQ_AUTH_HTTP_EVIDENCE schema=" + schema + " retained=true");
    }

    @AfterEach
    void noContextLeak() {
        // 切面必须自己收干净，否则线程池复用会把上一个请求的身份带给下一个人。
        assertNull(UserContext.get(), "请求结束后 UserContext 没有被清理");
    }

    // ==================== 夹具 ====================

    void seedStaff(int staffId, long storeId, String employment, int canManageHr, String name) {
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,"
                + "employment_status,can_manage_hr) VALUES (?,?,?,?,?,?)",
                staffId, storeId, name, "syn_" + staffId, employment, canManageHr);
    }

    /** 真实签发的 JWT，与生产同一套签名算法与 claim 名。 */
    String token(long staffId, long storeId, String role) {
        return Jwts.builder().subject("syn_" + staffId)
                .claim("staffId", staffId).claim("storeId", storeId).claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    long inquiry(long store, String phone) {
        jdbc.update("INSERT INTO booking_inquiry(store_id,customer_name,customer_phone,guest_count,"
                + "status,created_at) VALUES (?,?,?,8,'pending',NOW())", store, "合成客人", phone);
        return jdbc.queryForObject(
                "SELECT id FROM booking_inquiry WHERE customer_phone=? ORDER BY id DESC LIMIT 1",
                Long.class, phone);
    }

    int table(long store) {
        int id = ++nextTable;
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_status) VALUES (?,?,'idle')", id, store);
        return id;
    }

    Map<String, Object> body(long store) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("bookingDate", LocalDate.now().plusDays(1).toString());
        b.put("bookingTime", "18:30");
        b.put("tableIds", List.of(table(store)));
        return b;
    }

    record Reply(int status, String raw, JsonNode body) { }

    /** 响应体按 UTF-8 读原始字节：MockMvc 默认用 ISO-8859-1 解码，中文提示会全部乱掉。 */
    Reply post(long inquiryId, Map<String, Object> payload, String bearer) throws Exception {
        var builder = MockMvcRequestBuilders.post("/api/booking-inquiries/" + inquiryId + "/convert")
                .contentType("application/json").content(json.writeValueAsString(payload));
        if (bearer != null) builder = builder.header("Authorization", "Bearer " + bearer);
        var response = mvc.perform(builder).andReturn().getResponse();
        String raw = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        return new Reply(response.getStatus(), raw, raw.isEmpty() ? null : json.readTree(raw));
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    /** 被拒的全部要求：给定的码与文案、且数据库一个字都没动。 */
    void deniedWithoutAnyWrite(String bearer, long inquiryId, int expectCode, String expectMessage)
            throws Exception {
        long masters = count("booking_master"), tables = count("booking_table");
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", inquiryId);

        Reply reply = post(inquiryId, body(STORE), bearer);
        assertEquals(expectCode, reply.body().path("code").asInt(), "状态码不符：" + reply.raw());
        assertEquals(expectMessage, reply.body().path("message").asText(), "文案不符：" + reply.raw());
        assertTrue(reply.body().path("data").isNull() || reply.body().path("data").isMissingNode(),
                "被拒却带回了数据：" + reply.raw());

        assertEquals(masters, count("booking_master"), "被拒仍建了预订单");
        assertEquals(tables, count("booking_table"), "被拒仍占了台位");
        assertEquals(before, jdbc.queryForMap("SELECT * FROM booking_inquiry WHERE id=?", inquiryId),
                "被拒仍改动了咨询行");
    }

    // ==================== 用例 ====================

    @Test @Order(1)
    @DisplayName("本店在职且有权：真 JWT 走完整 HTTP 能转单，身份全程由 token 还原")
    void sameStoreHrSucceeds() throws Exception {
        long id = inquiry(STORE, "13800009001");
        Reply reply = post(id, body(STORE), token(101, STORE, "store_manager"));
        assertEquals(200, reply.body().path("code").asInt(), reply.raw());
        String bookingId = reply.body().path("data").path("bookingId").asText();
        assertFalse(bookingId.isBlank(), "没有返回单号：" + reply.raw());
        assertFalse(reply.body().path("data").path("replayed").asBoolean(), "首次转单不该标记为重放");

        // 落库核对：单号真的存在，且落在咨询自己的门店
        assertEquals(STORE, (long) jdbc.queryForObject(
                "SELECT store_id FROM booking_master WHERE booking_id=?", Long.class, bookingId));
        assertEquals(bookingId, jdbc.queryForObject(
                "SELECT booking_id FROM booking_inquiry WHERE id=?", String.class, id));
    }

    @Test @Order(2)
    @DisplayName("总经理：跨店也能转，但转出来的仍是咨询自己门店的单")
    void generalManagerSucceedsAcrossStores() throws Exception {
        long id = inquiry(OTHER_STORE, "13800009002");
        Reply reply = post(id, body(OTHER_STORE), token(105, 0L, "gm"));
        assertEquals(200, reply.body().path("code").asInt(), reply.raw());
        String bookingId = reply.body().path("data").path("bookingId").asText();
        assertEquals(OTHER_STORE, (long) jdbc.queryForObject(
                "SELECT store_id FROM booking_master WHERE booking_id=?", Long.class, bookingId),
                "总经理转出来的单落到了别的门店");
    }

    @Test @Order(3)
    @DisplayName("token 里有 staffId，但人不在花名册：403，零写入")
    void unknownStaffDenied() throws Exception {
        long id = inquiry(STORE, "13800009003");
        deniedWithoutAnyWrite(token(106, STORE, "store_manager"), id, 403, NOT_ALLOWED);
    }

    @Test @Order(4)
    @DisplayName("已离职员工手里的 token 仍在有效期：照样被挡，403，零写入")
    void resignedStaffDenied() throws Exception {
        long id = inquiry(STORE, "13800009004");
        // 这条最能说明为什么必须回库：token 本身完全合法，签发时此人还在职。
        deniedWithoutAnyWrite(token(103, STORE, "store_manager"), id, 403, NOT_ALLOWED);
    }

    @Test @Order(5)
    @DisplayName("在职但没有人事权限（can_manage_hr=0）：403，零写入")
    void nonHrStaffDenied() throws Exception {
        long id = inquiry(STORE, "13800009005");
        // 与之并列的 GET /api/booking-inquiries 一直要求 can_manage_hr=1；
        // 转单这个写操作反而更松是说不通的，这里把两者拉齐。
        deniedWithoutAnyWrite(token(102, STORE, "store_manager"), id, 403, NOT_ALLOWED);
    }

    @Test @Order(6)
    @DisplayName("二店在职主管转一店咨询：404 统一口径，零写入")
    void crossStoreDenied() throws Exception {
        long id = inquiry(STORE, "13800009006");
        deniedWithoutAnyWrite(token(104, OTHER_STORE, "store_manager"), id, NOT_VISIBLE_CODE, NOT_VISIBLE);
    }

    @Test @Order(7)
    @DisplayName("跨店与不存在给出逐字节相同的响应：拿不到「这条 id 到底存不存在」的线索")
    void crossStoreAndMissingAreIndistinguishable() throws Exception {
        long existing = inquiry(STORE, "13800009007");
        long missing = jdbc.queryForObject("SELECT MAX(id)+9999 FROM booking_inquiry", Long.class);
        String bearer = token(104, OTHER_STORE, "store_manager");

        Reply onExisting = post(existing, body(STORE), bearer);
        Reply onMissing = post(missing, body(STORE), bearer);
        assertEquals(onExisting.status(), onMissing.status(), "HTTP 状态不同就能区分");
        assertEquals(onExisting.raw(), onMissing.raw(),
                "存在与不存在的响应不同，接口会被拿去枚举 id：" + onExisting.raw() + " / " + onMissing.raw());
    }

    @Test @Order(8)
    @DisplayName("越权者对已转单的咨询重放：拿不到真实单号")
    void unauthorizedReplayLeaksNothing() throws Exception {
        long id = inquiry(STORE, "13800009008");
        Reply ok = post(id, body(STORE), token(101, STORE, "store_manager"));
        String bookingId = ok.body().path("data").path("bookingId").asText();
        assertFalse(bookingId.isBlank());

        for (String bearer : List.of(
                token(104, OTHER_STORE, "store_manager"),   // 跨店
                token(102, STORE, "store_manager"),         // 本店但无权
                token(103, STORE, "store_manager"),         // 已离职
                token(106, STORE, "store_manager"))) {      // 不在册
            Reply reply = post(id, body(STORE), bearer);
            assertNotEquals(200, reply.body().path("code").asInt(), "越权重放竟然成功：" + reply.raw());
            assertFalse(reply.raw().contains(bookingId), "越权重放泄漏了真实单号：" + reply.raw());
        }
    }

    @Test @Order(9)
    @DisplayName("有权者重放：拿回同一张单，不建第二张")
    void authorizedReplayReturnsSameBooking() throws Exception {
        long id = inquiry(STORE, "13800009009");
        String bearer = token(101, STORE, "store_manager");
        Reply first = post(id, body(STORE), bearer);
        long masters = count("booking_master");
        Reply again = post(id, body(STORE), bearer);

        assertEquals(200, again.body().path("code").asInt(), again.raw());
        assertEquals(first.body().path("data").path("bookingId").asText(),
                again.body().path("data").path("bookingId").asText(), "重放拿到了不同的单号");
        assertTrue(again.body().path("data").path("replayed").asBoolean(), "重放没有如实标记");
        assertEquals(masters, count("booking_master"), "重放又建了一张单");
    }

    @Test @Order(10)
    @DisplayName("没有 token / token 无效：拦截器就挡下，401，根本进不到控制器")
    void noOrBadTokenRejectedAtInterceptor() throws Exception {
        long id = inquiry(STORE, "13800009010");
        long masters = count("booking_master");

        assertEquals(401, post(id, body(STORE), null).status(), "缺 Authorization 头竟然放行");
        assertEquals(401, post(id, body(STORE), "not-a-real-token").status(), "伪造 token 竟然放行");

        String otherSecret = "another-synthetic-secret-key-9876543210fedcba";
        String forged = Jwts.builder().subject("syn_101").claim("staffId", 101L).claim("storeId", STORE)
                .claim("role", "store_manager").expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8))).compact();
        assertEquals(401, post(id, body(STORE), forged).status(), "别的密钥签的 token 竟然放行");

        assertEquals(masters, count("booking_master"), "未通过鉴权却建了单");
        assertNull(jdbc.queryForObject("SELECT booking_id FROM booking_inquiry WHERE id=?", String.class, id));
    }
}
