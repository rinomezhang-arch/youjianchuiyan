package com.youjian.banquet.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.controller.BookingInquiryController;
import com.youjian.banquet.entity.BookingInquiry;
import com.youjian.banquet.repository.BookingInquiryRepository;
import com.youjian.banquet.service.MarketingInquiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TR-MARKETING-INQUIRY-API-55：营销 H5 咨询幂等落盘与客人回查（真实 HTTP + 服务层并发）。
 * 真实监听端口贯通 BookingInquiryController/MarketingInquiryService 与隔离 MySQL。
 * 业务数据合成；数据库仅使用本任务独占并保留的 TL55 合成 schema；不访问生产/法务数据。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@SpringBootTest(
        classes = MarketingInquiryApiTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
class MarketingInquiryApiTest {

    private static final String SCHEMA = requiredEnv("TL55_SCHEMA");
    private static final int MYSQL_HOST_PORT = Integer.parseInt(requiredEnv("YOUJIAN_TEST_MYSQL_PORT"));
    private static final int EXPECTED_CONTAINER_PORT = 3306;
    private static final String EXPECTED_DATA_DIR = normalizeDataDir(requiredEnv("YOUJIAN_TEST_MYSQL_DATADIR"));
    private static final String JDBC_HOST = "jdbc:mysql://127.0.0.1:" + MYSQL_HOST_PORT + "/";
    private static final String JDBC_OPTIONS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
    private static final DataSource DATA_SOURCE;
    private static final JdbcTemplate JDBC;

    static {
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(JDBC_HOST + JDBC_OPTIONS, "root", ""));
        Map<String, Object> server = admin.queryForMap("SELECT @@port AS port, @@datadir AS datadir");
        int actualPort = ((Number) server.get("port")).intValue();
        String actualDataDir = normalizeDataDir(String.valueOf(server.get("datadir")));
        if (actualPort != EXPECTED_CONTAINER_PORT || !actualDataDir.equalsIgnoreCase(EXPECTED_DATA_DIR)) {
            throw new IllegalStateException("Refusing non-isolated MySQL: port=" + actualPort + ", datadir=" + actualDataDir);
        }
        DATA_SOURCE = new DriverManagerDataSource(JDBC_HOST + SCHEMA + JDBC_OPTIONS, "root", "");
        JDBC = new JdbcTemplate(DATA_SOURCE);
        seed();
    }

    @Autowired
    MarketingInquiryService service;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    private static String requiredEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException(name + " is required");
        return v.trim();
    }

    private static String normalizeDataDir(String v) {
        String n = v.replace('\\', '/');
        return n.endsWith("/") ? n : n + "/";
    }

    private static void seed() {
        JDBC.update("INSERT INTO store_info (store_id, store_code, store_name) VALUES (1,'NG','宁国店'),(2,'XC','宣城店')");
        JDBC.update("INSERT INTO marketing_activity (activity_id, store_id, activity_code, activity_name, activity_type, status, document_version, row_version) VALUES "
                + "(1,1,'ACT-001','迎春接福宴','promotion','published',1,1)");
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,1,'h5','visible-slug','src-visible','可见活动',NULL,NULL,NULL,'咨询档期','published',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-1')");
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,2,'h5','paused-slug','src-paused','暂停活动',NULL,NULL,NULL,'咨询档期','paused',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-2')");
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,3,'h5','expired-slug','src-expired','过期活动',NULL,NULL,NULL,'咨询档期','expired',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'req-pub-3')");
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,4,'h5','future-slug','src-future','未来活动',NULL,NULL,NULL,'咨询档期','published',DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),'req-pub-4')");
    }

    private static Map<String, Object> payload(String sourceCode, String requestId, String phone, String name, String date, Integer partySize) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sourceCode", sourceCode);
        m.put("requestId", requestId);
        m.put("phone", phone);
        m.put("customerName", name);
        m.put("expectedDate", date);
        m.put("partySize", partySize);
        m.put("remark", "测试咨询");
        return m;
    }

    private JsonNode postJson(String path, Map<String, Object> body) throws Exception {
        ResponseEntity<String> resp = rest.postForEntity(path, body, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    private int postStatus(String path, Map<String, Object> body) {
        ResponseEntity<String> resp = rest.postForEntity(path, body, String.class);
        return resp.getStatusCode().value();
    }

    private static int count(String table) {
        Integer c = JDBC.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return c == null ? 0 : c;
    }

    // ==================== 服务层：1. 成功落盘 + 回执字段 ====================

    @Test
    void submitSuccessReturnsStableReceipt() {
        String reqId = "req-ok-" + System.nanoTime();
        Map<String, Object> r = service.submit(payload("src-visible", reqId, "13800000001", "张三", "2026-12-31", 6));
        assertNotNull(r.get("id"));
        String inquiryNo = (String) r.get("inquiryNo");
        assertTrue(inquiryNo.startsWith("INQ"));
        assertEquals("/h5/inquiry/" + inquiryNo, r.get("lookupUrl"));
        Map<String, Object> inq = JDBC.queryForMap(
                "SELECT store_id, marketing_publication_id, source_code, source_channel FROM booking_inquiry WHERE id = ?", r.get("id"));
        assertEquals(1L, ((Number) inq.get("store_id")).longValue());
        assertEquals(1L, ((Number) inq.get("marketing_publication_id")).longValue());
        assertEquals("src-visible", inq.get("source_code"));
        assertEquals("h5", inq.get("source_channel"));
        Map<String, Object> evt = JDBC.queryForMap(
                "SELECT business_type, business_id, business_no FROM marketing_attribution_event WHERE request_id = ?", reqId);
        assertEquals("booking_inquiry", evt.get("business_type"));
        assertEquals(String.valueOf(r.get("id")), String.valueOf(evt.get("business_id")));
        assertEquals(inquiryNo, evt.get("business_no"));
    }

    // ==================== 服务层：2. 真并发同 requestId 同载荷 ====================

    @Test
    void concurrentSameRequestIdSamePayloadProducesOneRow() throws Exception {
        int threads = 8;
        String reqId = "req-conc-" + System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return service.submit(payload("src-visible", reqId, "13800000002", "李四", "2026-12-31", 4));
            }));
        }
        ready.await();
        go.countDown();
        Set<Long> ids = new HashSet<>();
        Set<String> inquiryNos = new HashSet<>();
        for (Future<Map<String, Object>> f : futures) {
            Map<String, Object> r = f.get();
            assertNotNull(r);
            ids.add(((Number) r.get("id")).longValue());
            inquiryNos.add((String) r.get("inquiryNo"));
        }
        pool.shutdown();
        assertEquals(1, ids.size());
        assertEquals(1, inquiryNos.size());
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt);
        Integer inqCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE id = ?", Integer.class, ids.iterator().next());
        assertEquals(1, inqCnt == null ? 0 : inqCnt);
    }

    // ==================== 服务层：3. 同 requestId 不同载荷 -> 409 ====================

    @Test
    void conflictingPayloadSameRequestIdRejected() {
        String reqId = "req-conf-" + System.nanoTime();
        service.submit(payload("src-visible", reqId, "13800000003", "王五", "2026-12-31", 2));
        assertThrows(IllegalStateException.class,
                () -> service.submit(payload("src-visible", reqId, "13800000004", "王五", "2026-12-31", 2)));
        Integer inqCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE customer_phone = '13800000003' AND source_code='src-visible'", Integer.class);
        assertEquals(1, inqCnt == null ? 0 : inqCnt);
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt);
    }

    // ==================== 服务层：4. lookup 四态 ====================

    @Test
    void lookupFourStates() {
        String reqId = "req-lk-" + System.nanoTime();
        Map<String, Object> r = service.submit(payload("src-visible", reqId, "13800000005", "赵六", "2026-12-31", 8));
        String inquiryNo = (String) r.get("inquiryNo");
        Map<String, Object> ok = service.lookup(Map.of("inquiryNo", inquiryNo, "phone", "13800000005"));
        assertNotNull(ok);
        assertEquals(inquiryNo, ok.get("inquiryNo"));
        assertEquals("pending", ok.get("status"));
        assertEquals(8, ((Number) ok.get("partySize")).intValue());
        assertNull(service.lookup(Map.of("inquiryNo", "INQ99999999", "phone", "13800000005")));
        assertNull(service.lookup(Map.of("inquiryNo", inquiryNo, "phone", "13899999999")));
        assertNull(service.lookup(Map.of("inquiryNo", "not-a-no", "phone", "13800000005")));
        assertNull(service.lookup(Map.of("inquiryNo", inquiryNo)));
    }

    // ==================== 服务层：5. 零半单 ====================

    @Test
    void zeroHalfCommitOnInvalidInput() {
        int inqBefore = count("booking_inquiry");
        int evtBefore = count("marketing_attribution_event");
        for (String src : new String[]{"src-paused", "src-expired", "src-future", "src-unknown"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.submit(payload(src, "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2026-12-31", 3)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(payload("src-visible", "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2020-01-01", 3)));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(payload("src-visible", "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2026-12-31", 0)));
        Map<String, Object> noReq = payload("src-visible", null, "13800000006", "孙七", "2026-12-31", 3);
        assertThrows(IllegalArgumentException.class, () -> service.submit(noReq));
        assertEquals(inqBefore, count("booking_inquiry"));
        assertEquals(evtBefore, count("marketing_attribution_event"));
    }

    // ==================== 真实 HTTP：6. sourceCode 成功 + 重放 ====================

    @Test
    void httpSubmitSourceCodeSuccessAndReplay() throws Exception {
        String reqId = "req-http-ok-" + System.nanoTime();
        Map<String, Object> body = payload("src-visible", reqId, "13800000007", "周八", "2026-12-31", 5);
        assertEquals(200, postStatus("/api/public/booking-inquiry", body));
        JsonNode r1 = postJson("/api/public/booking-inquiry", body);
        assertEquals(200, r1.path("code").asInt(), r1.toString());
        assertNotNull(r1.path("data").path("id"));
        String inquiryNo = r1.path("data").path("inquiryNo").asText();
        assertTrue(inquiryNo.startsWith("INQ"));
        assertEquals("/h5/inquiry/" + inquiryNo, r1.path("data").path("lookupUrl").asText());
        // 同键同载荷重放返回完全相同值
        JsonNode r2 = postJson("/api/public/booking-inquiry", body);
        assertEquals(200, r2.path("code").asInt());
        assertEquals(r1.path("data").path("id").asLong(), r2.path("data").path("id").asLong());
        assertEquals(inquiryNo, r2.path("data").path("inquiryNo").asText());
        assertEquals(r1.path("data").path("lookupUrl").asText(), r2.path("data").path("lookupUrl").asText());
    }

    // ==================== 真实 HTTP：7. 不可见 sourceCode 拒绝 + 零新增 ====================

    @Test
    void httpSourceCodeNotVisibleRejected() throws Exception {
        int inqBefore = count("booking_inquiry");
        int evtBefore = count("marketing_attribution_event");
        for (String src : new String[]{"src-paused", "src-expired", "src-future", "src-unknown"}) {
            Map<String, Object> body = payload(src, "req-http-inv-" + System.nanoTime(), "13800000008", "吴九", "2026-12-31", 3);
            assertEquals(200, postStatus("/api/public/booking-inquiry", body));
            JsonNode r = postJson("/api/public/booking-inquiry", body);
            assertEquals(400, r.path("code").asInt(), r.toString());
        }
        assertEquals(inqBefore, count("booking_inquiry"));
        assertEquals(evtBefore, count("marketing_attribution_event"));
    }

    // ==================== 真实 HTTP：8. legacy 无 sourceCode 两态 ====================

    @Test
    void httpLegacyStoreIdValidation() throws Exception {
        Map<String, Object> noStore = new LinkedHashMap<>();
        noStore.put("customerName", "郑十");
        noStore.put("customerPhone", "13800000010");
        noStore.put("preferredDate", "2026-12-31");
        JsonNode r1 = postJson("/api/public/booking-inquiry", noStore);
        assertEquals(400, r1.path("code").asInt(), r1.toString());

        Map<String, Object> badStore = new LinkedHashMap<>(noStore);
        badStore.put("storeId", -1);
        JsonNode r2 = postJson("/api/public/booking-inquiry", badStore);
        assertEquals(400, r2.path("code").asInt(), r2.toString());

        Map<String, Object> goodStore = new LinkedHashMap<>(noStore);
        goodStore.put("storeId", 1);
        JsonNode r3 = postJson("/api/public/booking-inquiry", goodStore);
        assertEquals(200, r3.path("code").asInt(), r3.toString());
        assertNotNull(r3.path("data").path("id"));
    }

    // ==================== 真实 HTTP：9. 同 requestId 不同载荷 -> HTTP 200 + code 409 ====================

    @Test
    void httpConflictRequestId409() throws Exception {
        String reqId = "req-http-conf-" + System.nanoTime();
        Map<String, Object> b1 = payload("src-visible", reqId, "13800000011", "冯一", "2026-12-31", 2);
        assertEquals(200, postStatus("/api/public/booking-inquiry", b1));
        JsonNode ok = postJson("/api/public/booking-inquiry", b1);
        assertEquals(200, ok.path("code").asInt());
        Map<String, Object> b2 = payload("src-visible", reqId, "13800000012", "冯一", "2026-12-31", 2);
        JsonNode conflict = postJson("/api/public/booking-inquiry", b2);
        assertEquals(409, conflict.path("code").asInt(), conflict.toString());
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt);
    }

    // ==================== 真实 HTTP：10. lookup 四态 ====================

    @Test
    void httpLookupFourStates() throws Exception {
        String reqId = "req-http-lk-" + System.nanoTime();
        JsonNode sub = postJson("/api/public/booking-inquiry", payload("src-visible", reqId, "13800000013", "陈二", "2026-12-31", 6));
        String inquiryNo = sub.path("data").path("inquiryNo").asText();

        // 正例
        Map<String, Object> good = new LinkedHashMap<>();
        good.put("inquiryNo", inquiryNo);
        good.put("phone", "13800000013");
        assertEquals(200, postStatus("/api/public/booking-inquiry/lookup", good));
        JsonNode ok = postJson("/api/public/booking-inquiry/lookup", good);
        assertEquals(200, ok.path("code").asInt());
        assertEquals(inquiryNo, ok.path("data").path("inquiryNo").asText());
        assertEquals("pending", ok.path("data").path("status").asText());

        // 查无 / 电话不符 / 非法 → success(null)
        Map<String, Object> notFound = new LinkedHashMap<>();
        notFound.put("inquiryNo", "INQ99999999");
        notFound.put("phone", "13800000013");
        JsonNode r1 = postJson("/api/public/booking-inquiry/lookup", notFound);
        assertEquals(200, r1.path("code").asInt());
        assertTrue(r1.path("data").isNull(), r1.toString());

        Map<String, Object> badPhone = new LinkedHashMap<>();
        badPhone.put("inquiryNo", inquiryNo);
        badPhone.put("phone", "13899999999");
        JsonNode r2 = postJson("/api/public/booking-inquiry/lookup", badPhone);
        assertEquals(200, r2.path("code").asInt());
        assertTrue(r2.path("data").isNull(), r2.toString());

        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("inquiryNo", "not-a-no");
        invalid.put("phone", "13800000013");
        JsonNode r3 = postJson("/api/public/booking-inquiry/lookup", invalid);
        assertEquals(200, r3.path("code").asInt());
        assertTrue(r3.path("data").isNull(), r3.toString());
    }

    // ==================== 真实 HTTP：11. 并发同 requestId 同载荷 ====================

    @Test
    void httpConcurrentSameRequestId() throws Exception {
        int threads = 8;
        String reqId = "req-http-conc-" + System.nanoTime();
        Map<String, Object> body = payload("src-visible", reqId, "13800000014", "褚三", "2026-12-31", 7);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<JsonNode>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                ResponseEntity<String> resp = rest.postForEntity("/api/public/booking-inquiry", body, String.class);
                return objectMapper.readTree(resp.getBody());
            }));
        }
        ready.await();
        go.countDown();
        Set<Long> ids = new HashSet<>();
        Set<String> nos = new HashSet<>();
        for (Future<JsonNode> f : futures) {
            JsonNode n = f.get();
            assertEquals(200, n.path("code").asInt(), n.toString());
            ids.add(n.path("data").path("id").asLong());
            nos.add(n.path("data").path("inquiryNo").asText());
        }
        pool.shutdown();
        assertEquals(1, ids.size(), "HTTP 并发应同一回执 id");
        assertEquals(1, nos.size(), "HTTP 并发应同一 inquiryNo");
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt);
        Integer inqCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE id = ?", Integer.class, ids.iterator().next());
        assertEquals(1, inqCnt == null ? 0 : inqCnt);
    }

    // ==================== 最小 Spring 上下文 ====================

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        DataSource dataSource() { return DATA_SOURCE; }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }

        @Bean
        PlatformTransactionManager txManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }

        @Bean
        ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean
        MarketingInquiryService marketingInquiryService(JdbcTemplate jdbcTemplate, PlatformTransactionManager txManager) {
            return new MarketingInquiryService(jdbcTemplate, txManager);
        }

        @Bean
        BookingInquiryRepository bookingInquiryRepository() {
            BookingInquiryRepository repository = mock(BookingInquiryRepository.class);
            when(repository.save(any(BookingInquiry.class))).thenAnswer(inv -> {
                BookingInquiry inq = inv.getArgument(0);
                inq.setId(999L);
                return inq;
            });
            return repository;
        }

        @Bean
        BookingInquiryController bookingInquiryController() {
            return new BookingInquiryController();
        }
    }
}
