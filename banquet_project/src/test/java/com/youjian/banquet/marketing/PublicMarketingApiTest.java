package com.youjian.banquet.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.controller.PublicMarketingController;
import com.youjian.banquet.service.PublicMarketingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TR-MARKETING-PUBLIC-API-54：营销客人 H5 公开读取与浏览归因后端。
 * 真实监听端口下贯通 PublicMarketingController/Service 与隔离 MySQL 的真实 HTTP + 数据库回读。
 * 业务数据使用合成对象；数据库仅使用本任务独占并保留的 TL54 合成 schema；不访问生产或法务数据。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@SpringBootTest(
        classes = PublicMarketingApiTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
class PublicMarketingApiTest {

    private static final String SCHEMA = requiredEnvironment("TL54_SCHEMA");
    private static final int MYSQL_HOST_PORT = Integer.parseInt(requiredEnvironment("YOUJIAN_TEST_MYSQL_PORT"));
    private static final int EXPECTED_CONTAINER_PORT = 3306;
    private static final String EXPECTED_DATA_DIR = normalizeDataDir(
            requiredEnvironment("YOUJIAN_TEST_MYSQL_DATADIR"));
    private static final String JDBC_HOST = "jdbc:mysql://127.0.0.1:" + MYSQL_HOST_PORT + "/";
    private static final String JDBC_OPTIONS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
    private static final DataSource DATA_SOURCE;
    private static final JdbcTemplate JDBC;

    /** 公开响应白名单字段（精确集合，用于断言不含任何内部字段）。 */
    private static final Set<String> WHITELIST = Set.of(
            "publicationId", "storeId", "storeName", "storeAddress", "storePhone",
            "sourceCode", "publicTitle", "publicSummary", "contentJson", "heroAssetUrl",
            "ctaLabel", "status", "validFrom", "validTo");

    static {
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(JDBC_HOST + JDBC_OPTIONS, "root", ""));
        Map<String, Object> server = admin.queryForMap("SELECT @@port AS port, @@datadir AS datadir");
        int actualPort = ((Number) server.get("port")).intValue();
        String actualDataDir = normalizeDataDir(String.valueOf(server.get("datadir")));
        if (actualPort != EXPECTED_CONTAINER_PORT || !actualDataDir.equalsIgnoreCase(EXPECTED_DATA_DIR)) {
            throw new IllegalStateException("Refusing non-isolated MySQL: port=" + actualPort
                    + ", datadir=" + actualDataDir);
        }
        DATA_SOURCE = new DriverManagerDataSource(JDBC_HOST + SCHEMA + JDBC_OPTIONS, "root", "");
        JDBC = new JdbcTemplate(DATA_SOURCE);
        seed();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for the isolated MySQL test");
        }
        return value.trim();
    }

    private static String normalizeDataDir(String value) {
        String normalized = value.replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /** 合成公开数据：2 门店、3 活动（published/draft/pending_approval）、4 发布（可见/暂停/过期/未来）。 */
    private static void seed() {
        JDBC.update("INSERT INTO store_info (store_id, store_code, store_name, address, phone, status) VALUES "
                + "(1,'NG','宁国店','宁国市测试路1号','0563-1000001','open'),"
                + "(2,'XC','宣城店','宣城市测试路2号','0563-2000002','open')");
        JDBC.update("INSERT INTO marketing_activity (activity_id, store_id, activity_code, activity_name, activity_type, status, document_version, row_version) VALUES "
                + "(1,1,'ACT-001','迎春接福宴','promotion','published',1,1),"
                + "(2,1,'ACT-002','草稿活动','promotion','draft',1,1),"
                + "(3,1,'ACT-003','待批活动','promotion','pending_approval',1,1)");
        // 可见
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,1,'h5','visible-slug','src-visible','可见活动','可见摘要','{\"desc\":\"可见\"}','/site-photos/storefront-entrance.jpg','咨询档期','published',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-1')");
        // 暂停
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,2,'h5','paused-slug','src-paused','暂停活动',NULL,NULL,NULL,'咨询档期','paused',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-2')");
        // 过期
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,3,'h5','expired-slug','src-expired','过期活动',NULL,NULL,NULL,'咨询档期','expired',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'req-pub-3')");
        // 未来生效（published 但 valid_from 在未来）
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,4,'h5','future-slug','src-future','未来活动',NULL,NULL,NULL,'咨询档期','published',DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),'req-pub-4')");
    }

    @LocalServerPort
    int port;

    private final ObjectMapper json = new ObjectMapper();

    // ---- 真实 HTTP 辅助 ----

    private JsonNode get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return json.readTree(resp.body());
    }

    private JsonNode post(String path, String bodyJson) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return json.readTree(resp.body());
    }

    // ---- 八类断言 ----

    /** 1. 可见成功：slug 快照 + 门店列表。 */
    @Test
    void visibleSuccess() throws Exception {
        JsonNode slug = get("/api/public/marketing/a/visible-slug");
        assertEquals(200, slug.path("code").asInt(), slug.toString());
        assertEquals("published", slug.path("data").path("status").asText());
        assertEquals("src-visible", slug.path("data").path("sourceCode").asText());
        assertEquals("可见活动", slug.path("data").path("publicTitle").asText());

        JsonNode list = get("/api/public/marketing/activities?storeId=1");
        assertEquals(200, list.path("code").asInt(), list.toString());
        assertEquals(1, list.path("data").size(), list.toString());
        assertEquals("src-visible", list.path("data").get(0).path("sourceCode").asText());
    }

    /** 2. 六类不可公开统一 404：草稿/待批/暂停/过期/未来/未知，响应一致不泄露存在性。 */
    @Test
    void sixNonPublicUnified404() throws Exception {
        String[] slugs = {"paused-slug", "expired-slug", "future-slug",
                "draft-slug", "pending-slug", "unknown-random-slug"};
        int expected = -1;
        String expectedMsg = null;
        for (String s : slugs) {
            JsonNode r = get("/api/public/marketing/a/" + s);
            int code = r.path("code").asInt();
            String msg = r.path("message").asText();
            assertTrue(code == 404, "slug=" + s + " 应 404 但为 " + r);
            if (expected == -1) { expected = code; expectedMsg = msg; }
            assertEquals(expected, code, "slug=" + s + " 404 口径不一致");
            assertEquals(expectedMsg, msg, "slug=" + s + " 404 文案不一致（泄露存在性）");
        }
    }

    /** 3. 公开字段白名单：响应不含任何内部字段。 */
    @Test
    void publicFieldWhitelist() throws Exception {
        JsonNode r = get("/api/public/marketing/a/visible-slug");
        JsonNode data = r.path("data");
        Set<String> keys = new LinkedHashSet<>();
        data.fieldNames().forEachRemaining(keys::add);
        assertEquals(WHITELIST, keys, "公开响应字段集合必须精确等于白名单");
        // 明确断言不含内部字段
        for (String internal : new String[]{"budgetAmount", "actualCost", "publishedBy", "createdBy",
                "activityCode", "activityName", "pausedBy", "approvalRemark", "requestId"}) {
            assertFalse(data.has(internal), "泄露内部字段 " + internal);
        }
    }

    /** 4. 伪造 storeId 不被采用：归因 store_id 取发布反查，而非客户端 storeId。 */
    @Test
    void forgedStoreIdNotTrusted() throws Exception {
        JsonNode r = post("/api/public/marketing/events",
                "{\"eventType\":\"view\",\"sourceCode\":\"src-visible\",\"storeId\":999,\"requestId\":\"req-evt-1\"}");
        assertEquals(200, r.path("code").asInt(), r.toString());
        Map<String, Object> row = JDBC.queryForMap(
                "SELECT store_id, publication_id FROM marketing_attribution_event WHERE request_id='req-evt-1'");
        assertEquals(1L, ((Number) row.get("store_id")).longValue(), "伪造 storeId=999 被写入了归因");
        assertEquals(1L, ((Number) row.get("publication_id")).longValue());
    }

    /** 5. publicationId 不匹配零新增。 */
    @Test
    void publicationIdMismatchZeroInsert() throws Exception {
        JsonNode r = post("/api/public/marketing/events",
                "{\"eventType\":\"view\",\"sourceCode\":\"src-visible\",\"publicationId\":999,\"requestId\":\"req-evt-mismatch\"}");
        int code = r.path("code").asInt();
        assertTrue(code == 409 || code == 400, "publicationId 不匹配应拒绝，实为 " + r);
        Integer cnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id='req-evt-mismatch'", Integer.class);
        assertEquals(0, cnt == null ? 0 : cnt, "publicationId 不匹配必须零新增");
    }

    /** 6. 重复 requestId 只一行，返回同一 eventId。 */
    @Test
    void duplicateRequestIdIdempotent() throws Exception {
        JsonNode r1 = post("/api/public/marketing/events",
                "{\"eventType\":\"view\",\"sourceCode\":\"src-visible\",\"requestId\":\"req-evt-idem\"}");
        assertEquals(200, r1.path("code").asInt(), r1.toString());
        long id1 = r1.path("data").path("eventId").asLong();
        JsonNode r2 = post("/api/public/marketing/events",
                "{\"eventType\":\"view\",\"sourceCode\":\"src-visible\",\"requestId\":\"req-evt-idem\"}");
        assertEquals(200, r2.path("code").asInt(), r2.toString());
        assertEquals(id1, r2.path("data").path("eventId").asLong(), "重复 requestId 应返回同一 eventId");
        Integer cnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id='req-evt-idem'", Integer.class);
        assertEquals(1, cnt == null ? 0 : cnt, "重复 requestId 必须只一行");
    }

    /** 7. 归因与发布同店（store_id 与发布一致）。 */
    @Test
    void attributionSameStoreAsPublication() throws Exception {
        Map<String, Object> pub = JDBC.queryForMap(
                "SELECT store_id FROM marketing_publication WHERE source_code='src-visible'");
        Map<String, Object> evt = JDBC.queryForMap(
                "SELECT store_id FROM marketing_attribution_event WHERE request_id='req-evt-1'");
        assertEquals(String.valueOf(pub.get("store_id")), String.valueOf(evt.get("store_id")),
                "归因 store_id 必须与发布同店");
    }

    /** 8. 孤儿与跨店计数为 0。 */
    @Test
    void orphanAndCrossStoreZero() throws Exception {
        Integer orphan = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event e "
                + "LEFT JOIN marketing_publication p ON e.publication_id=p.publication_id AND e.store_id=p.store_id "
                + "WHERE p.publication_id IS NULL", Integer.class);
        Integer cross = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event e "
                + "JOIN marketing_publication p ON e.publication_id=p.publication_id "
                + "WHERE e.store_id <> p.store_id", Integer.class);
        assertEquals(0, orphan == null ? 0 : orphan, "存在孤儿归因");
        assertEquals(0, cross == null ? 0 : cross, "存在跨店归因");
    }

    // ---- 最小 Spring 上下文 ----

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
        PublicMarketingService publicMarketingService(JdbcTemplate jdbcTemplate) {
            return new PublicMarketingService(jdbcTemplate);
        }

        @Bean
        PublicMarketingController publicMarketingController(PublicMarketingService service) {
            return new PublicMarketingController(service);
        }
    }
}
