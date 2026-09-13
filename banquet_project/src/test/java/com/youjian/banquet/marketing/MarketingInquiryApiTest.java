package com.youjian.banquet.marketing;

import com.youjian.banquet.service.MarketingInquiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
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

/**
 * TR-MARKETING-INQUIRY-API-55：营销 H5 咨询幂等落盘与客人回查。
 * 直接贯通 MarketingInquiryService + 真实隔离 MySQL；业务数据合成；不访问生产/法务数据。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
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
    private static final MarketingInquiryService SERVICE;

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
        SERVICE = new MarketingInquiryService(JDBC, new DataSourceTransactionManager(DATA_SOURCE));
        seed();
    }

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
        // 可见
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,1,'h5','visible-slug','src-visible','可见活动',NULL,NULL,NULL,'咨询档期','published',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-1')");
        // 暂停
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,2,'h5','paused-slug','src-paused','暂停活动',NULL,NULL,NULL,'咨询档期','paused',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),'req-pub-2')");
        // 过期
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,3,'h5','expired-slug','src-expired','过期活动',NULL,NULL,NULL,'咨询档期','expired',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),'req-pub-3')");
        // 未来生效
        JDBC.update("INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,summary,content_json,hero_asset_url,cta_label,status,valid_from,valid_to,request_id) VALUES "
                + "(1,1,4,'h5','future-slug','src-future','未来活动',NULL,NULL,NULL,'咨询档期','published',DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 2 DAY),'req-pub-4')");
    }

    private static Map<String, Object> payload(String sourceCode, String requestId, String phone, String name, String date, Integer partySize) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("sourceCode", sourceCode);
        m.put("requestId", requestId);
        m.put("phone", phone);
        m.put("customerName", name);
        m.put("expectedDate", date);
        m.put("partySize", partySize);
        m.put("remark", "测试咨询");
        return m;
    }

    // ==================== 1. 成功落盘 + 回执字段 ====================

    @Test
    void submitSuccessReturnsStableReceipt() {
        String reqId = "req-ok-" + System.nanoTime();
        Map<String, Object> r = SERVICE.submit(payload("src-visible", reqId, "13800000001", "张三", "2026-12-31", 6));
        assertNotNull(r.get("id"));
        String inquiryNo = (String) r.get("inquiryNo");
        assertTrue(inquiryNo.startsWith("INQ"), "inquiryNo 应为 INQ + id");
        assertEquals("/h5/inquiry/" + inquiryNo, r.get("lookupUrl"));
        // 数据库回读：一条咨询 + 一条事件
        Map<String, Object> inq = JDBC.queryForMap(
                "SELECT store_id, marketing_publication_id, source_code, source_channel, customer_phone, guest_count "
                + "FROM booking_inquiry WHERE id = ?", r.get("id"));
        assertEquals(1L, ((Number) inq.get("store_id")).longValue());
        assertEquals(1L, ((Number) inq.get("marketing_publication_id")).longValue());
        assertEquals("src-visible", inq.get("source_code"));
        assertEquals("h5", inq.get("source_channel"));
        Map<String, Object> evt = JDBC.queryForMap(
                "SELECT business_type, business_id, business_no, store_id FROM marketing_attribution_event WHERE request_id = ?", reqId);
        assertEquals("booking_inquiry", evt.get("business_type"));
        assertEquals(String.valueOf(r.get("id")), String.valueOf(evt.get("business_id")));
        assertEquals(inquiryNo, evt.get("business_no"));
        assertEquals(1L, ((Number) evt.get("store_id")).longValue());
    }

    // ==================== 2. 真并发同 requestId 同载荷 ====================

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
                return SERVICE.submit(payload("src-visible", reqId, "13800000002", "李四", "2026-12-31", 4));
            }));
        }
        ready.await();
        go.countDown();
        Set<Long> ids = new HashSet<>();
        Set<String> inquiryNos = new HashSet<>();
        for (Future<Map<String, Object>> f : futures) {
            Map<String, Object> r = f.get();
            assertNotNull(r, "并发提交不应返回 null");
            ids.add(((Number) r.get("id")).longValue());
            inquiryNos.add((String) r.get("inquiryNo"));
        }
        pool.shutdown();
        assertEquals(1, ids.size(), "并发同 requestId 应产生同一回执 id");
        assertEquals(1, inquiryNos.size(), "并发同 requestId 应产生同一 inquiryNo");
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt, "并发同 requestId 应只一条事件");
        Integer inqCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE id = ?", Integer.class, ids.iterator().next());
        assertEquals(1, inqCnt == null ? 0 : inqCnt, "并发同 requestId 应只一条咨询");
    }

    // ==================== 3. 同 requestId 不同载荷 -> 409 且零新增 ====================

    @Test
    void conflictingPayloadSameRequestIdRejected() {
        String reqId = "req-conf-" + System.nanoTime();
        SERVICE.submit(payload("src-visible", reqId, "13800000003", "王五", "2026-12-31", 2));
        // 同 requestId、不同手机号
        assertThrows(IllegalStateException.class,
                () -> SERVICE.submit(payload("src-visible", reqId, "13800000004", "王五", "2026-12-31", 2)));
        Integer inqCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM booking_inquiry WHERE customer_phone = '13800000003' AND source_code='src-visible'", Integer.class);
        assertEquals(1, inqCnt == null ? 0 : inqCnt, "冲突载荷不得新增咨询");
        Integer evtCnt = JDBC.queryForObject(
                "SELECT COUNT(*) FROM marketing_attribution_event WHERE request_id = ?", Integer.class, reqId);
        assertEquals(1, evtCnt == null ? 0 : evtCnt, "冲突载荷不得新增事件");
    }

    // ==================== 4. lookup 四态 ====================

    @Test
    void lookupFourStates() {
        String reqId = "req-lk-" + System.nanoTime();
        Map<String, Object> r = SERVICE.submit(payload("src-visible", reqId, "13800000005", "赵六", "2026-12-31", 8));
        String inquiryNo = (String) r.get("inquiryNo");

        // 正例：正确组合
        Map<String, Object> ok = SERVICE.lookup(Map.of("inquiryNo", inquiryNo, "phone", "13800000005"));
        assertNotNull(ok);
        assertEquals(inquiryNo, ok.get("inquiryNo"));
        assertEquals("pending", ok.get("status"));
        assertEquals(8, ((Number) ok.get("partySize")).intValue());
        assertNotNull(ok.get("createdAt"));

        // 查无（inquiryNo 不存在）
        assertNull(SERVICE.lookup(Map.of("inquiryNo", "INQ99999999", "phone", "13800000005")));
        // 电话不符
        assertNull(SERVICE.lookup(Map.of("inquiryNo", inquiryNo, "phone", "13899999999")));
        // 非法输入
        assertNull(SERVICE.lookup(Map.of("inquiryNo", "not-a-no", "phone", "13800000005")));
        assertNull(SERVICE.lookup(Map.of("inquiryNo", inquiryNo)));  // 缺 phone
    }

    // ==================== 5. 零半单：不可见 sourceCode / 非法日期人数 ====================

    @Test
    void zeroHalfCommitOnInvalidInput() {
        int inqBefore = count("booking_inquiry");
        int evtBefore = count("marketing_attribution_event");

        // 暂停/过期/未来/未知 sourceCode 全部拒绝
        for (String src : new String[]{"src-paused", "src-expired", "src-future", "src-unknown"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.submit(payload(src, "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2026-12-31", 3)));
        }
        // 非法日期
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.submit(payload("src-visible", "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2020-01-01", 3)));
        // 非法人数
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.submit(payload("src-visible", "req-zz-" + System.nanoTime(), "13800000006", "孙七", "2026-12-31", 0)));
        // 缺 requestId
        Map<String, Object> noReq = payload("src-visible", null, "13800000006", "孙七", "2026-12-31", 3);
        assertThrows(IllegalArgumentException.class, () -> SERVICE.submit(noReq));

        assertEquals(inqBefore, count("booking_inquiry"), "非法输入不得留下半条咨询");
        assertEquals(evtBefore, count("marketing_attribution_event"), "非法输入不得留下半条事件");
    }

    private static int count(String table) {
        Integer c = JDBC.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return c == null ? 0 : c;
    }
}
