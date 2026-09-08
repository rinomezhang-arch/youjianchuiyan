package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-INQUIRY-CONVERT-07 第二轮：客人自助查预订的 HTTP 级验收。
 * <p>
 * 走 MockMvc 真实 HTTP 分发 + 真实隔离 MySQL。
 * <b>本套件的重点不是"能查到"，而是"查不到时什么都问不出来"。</b>
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingInquiryPublicLookupHttpMysqlTest {

    static final String MINE = "13700001111", OTHERS = "13700002222", STRANGER = "13700009999";

    JdbcTemplate jdbc;
    MockMvc mvc;
    LocalContainerEntityManagerFactoryBean factory;
    final ObjectMapper json = new ObjectMapper();
    String myBookingId, othersBookingId, foreignStoreBookingId;
    String schema;

    @BeforeAll
    void start() {
        schema = "pub_lookup_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(url + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("PUB_LOOKUP_EVIDENCE schema=" + schema + " retained=true");

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

        myBookingId = seed(1L, MINE, "18:00");
        othersBookingId = seed(1L, OTHERS, "19:00");
        foreignStoreBookingId = seed(2L, STRANGER, "20:00");

        BookingInquiryController controller = new BookingInquiryController();
        ReflectionTestUtils.setField(controller, "jdbc", jdbc);
        ReflectionTestUtils.setField(controller, "objectMapper", json);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    String seed(long store, String phone, String time) {
        String bookingId = "BK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5);
        jdbc.update("INSERT INTO booking_master(booking_id,store_id,booking_date,booking_time,"
                        + "customer_name,customer_phone,guest_count,table_count,booking_status,payment_status,"
                        + "remark,total_amount,deposit_amount,created_at) "
                        + "VALUES(?,?,?,?,?,?,?,?,'confirmed','unpaid',?,?,?,NOW())",
                bookingId, store, LocalDate.now().plusDays(3), time, "合成客人", phone, 8, 2,
                "内部备注不得外泄", "8888.88", "1000.00");
        return bookingId;
    }

    @AfterAll
    void stop() {
        if (factory != null) factory.destroy();
        System.out.println("PUB_LOOKUP_EVIDENCE schema=" + schema + " retained=true");
    }

    /** 返回 (HTTP 状态, 原始响应体)。响应体按 UTF-8 读原始字节——MockMvc 默认用 ISO-8859-1 解码，中文会全乱。 */
    record Reply(int status, String raw) { }

    Reply post(Map<String, Object> body) throws Exception {
        var response = mvc.perform(MockMvcRequestBuilders.post("/api/public/booking-lookup")
                .contentType("application/json")
                .content(json.writeValueAsString(body))).andReturn().getResponse();
        return new Reply(response.getStatus(), new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    Map<String, Object> body(String phone, String bookingId) {
        Map<String, Object> b = new LinkedHashMap<>();
        if (phone != null) b.put("phone", phone);
        if (bookingId != null) b.put("bookingId", bookingId);
        return b;
    }

    // ==================== 用例 ====================

    @Test @Order(1)
    @DisplayName("手机号与单号都对：查得到，且只返回白名单字段")
    void correctCombinationReturnsWhitelistedFields() throws Exception {
        Reply reply = post(body(MINE, myBookingId));
        assertEquals(200, reply.status());
        JsonNode data = json.readTree(reply.raw()).path("data");
        assertFalse(data.isNull(), "正确组合应查得到：" + reply.raw());

        Set<String> keys = new HashSet<>();
        data.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("booking_id", "booking_date", "booking_time",
                        "guest_count", "table_count", "booking_status"), keys,
                "返回字段超出白名单：" + keys);
        assertEquals(myBookingId, data.path("booking_id").asText());

        // 敏感内容一个都不能带出来，包括客人自己的电话（他知道，回显只会在日志里多留一份）
        for (String forbidden : List.of(MINE, "内部备注不得外泄", "8888.88", "1000.00", "合成客人")) {
            assertFalse(reply.raw().contains(forbidden), "响应里出现了不该出现的内容：" + forbidden);
        }
    }

    @Test @Order(2)
    @DisplayName("手机号错：空结果")
    void wrongPhoneReturnsEmpty() throws Exception {
        assertTrue(json.readTree(post(body(STRANGER, myBookingId)).raw()).path("data").isNull());
    }

    @Test @Order(3)
    @DisplayName("单号错或不存在：空结果")
    void wrongBookingIdReturnsEmpty() throws Exception {
        assertTrue(json.readTree(post(body(MINE, "BK-DOES-NOT-EXIST")).raw()).path("data").isNull());
    }

    @Test @Order(4)
    @DisplayName("拿别人的单号配自己的手机号：空结果，问不出这单是谁的")
    void othersBookingReturnsEmpty() throws Exception {
        assertTrue(json.readTree(post(body(MINE, othersBookingId)).raw()).path("data").isNull());
        // 反过来也一样
        assertTrue(json.readTree(post(body(OTHERS, myBookingId)).raw()).path("data").isNull());
    }

    @Test @Order(5)
    @DisplayName("别店的单：同样空结果，不因跨店给出不同反应")
    void crossStoreReturnsEmpty() throws Exception {
        assertTrue(json.readTree(post(body(MINE, foreignStoreBookingId)).raw()).path("data").isNull());
    }

    @Test @Order(6)
    @DisplayName("只给手机号不给单号：绝不列出该手机号名下任何预订")
    void phoneOnlyCannotEnumerate() throws Exception {
        Reply reply = post(body(MINE, null));
        assertEquals(200, reply.status());
        assertTrue(json.readTree(reply.raw()).path("data").isNull(), "仅凭手机号列出了预订：" + reply.raw());
        assertFalse(reply.raw().contains(myBookingId), "响应里泄漏了该手机号名下的单号");
    }

    @Test @Order(7)
    @DisplayName("只给单号、都不给、手机号格式非法：全部空结果")
    void otherIncompleteInputsReturnEmpty() throws Exception {
        for (Map<String, Object> b : List.of(
                body(null, myBookingId), body(null, null),
                body("12345", myBookingId), body("23700001111", myBookingId))) {
            assertTrue(json.readTree(post(b).raw()).path("data").isNull(), "本应空结果：" + b);
        }
    }

    @Test @Order(8)
    @DisplayName("所有查不到的情况，响应体逐字节相同——外部无从区分是单号错、手机号错还是别人的单")
    void allMissResponsesAreByteIdentical() throws Exception {
        List<Map<String, Object>> misses = List.of(
                body(STRANGER, myBookingId),          // 手机号不符
                body(MINE, "BK-DOES-NOT-EXIST"),      // 单号不存在
                body(MINE, othersBookingId),          // 别人的单
                body(MINE, foreignStoreBookingId),    // 别店的单
                body(MINE, null),                     // 缺单号
                body(null, myBookingId),              // 缺手机号
                body("12345", myBookingId));          // 手机号格式非法

        Set<String> distinct = new LinkedHashSet<>();
        Set<Integer> statuses = new HashSet<>();
        for (Map<String, Object> b : misses) {
            Reply reply = post(b);
            statuses.add(reply.status());
            distinct.add(reply.raw());
        }
        assertEquals(1, statuses.size(), "不同的失败原因给出了不同的 HTTP 状态：" + statuses);
        assertEquals(1, distinct.size(),
                "不同的失败原因给出了可区分的响应，接口会被当成校验器用来反推机主：" + distinct);
    }
}
