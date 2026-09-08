package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CL-OPS-PUBLIC-H5-BLOCKERS-06：修 DL-OPS-PUBLIC-E2E-02 实证的两个 H5 后端阻断。
 * <p>
 * 真实隔离 MySQL，真实 schema——门店查询那条必须在真实库上跑，
 * 因为它的病就是"查了库里没有的列"，用替身根本发现不了。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PublicH5BlockersMysqlTest {

    JdbcTemplate jdbc;
    PublicStoreController stores;
    BookingInquiryController inquiries;
    LocalContainerEntityManagerFactoryBean factory;
    String schema;

    @BeforeAll
    void start() {
        schema = "public_h5_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        String opts = "?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(url + opts, "root", ""))
                .execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("PUBLIC_H5_EVIDENCE schema=" + schema + " retained=true");

        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + opts, "root", "");
        jdbc = new JdbcTemplate(ds);

        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                StoreInfo.class.getName(), BookingInquiry.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();

        JpaTransactionManager manager = new JpaTransactionManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        // 仓储代理必须挂事务拦截器，否则 save 会报 No EntityManager with actual transaction
        repositories.addRepositoryProxyPostProcessor((proxyFactory, info) ->
                proxyFactory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                        manager, new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource())));

        stores = new PublicStoreController();
        ReflectionTestUtils.setField(stores, "jdbc", jdbc);

        inquiries = new BookingInquiryController();
        ReflectionTestUtils.setField(inquiries, "inquiryRepo",
                repositories.getRepository(BookingInquiryRepository.class));
        ReflectionTestUtils.setField(inquiries, "objectMapper",
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @AfterAll
    void stop() {
        if (factory != null) factory.destroy();
        System.out.println("PUBLIC_H5_EVIDENCE schema=" + schema + " retained=true");
    }

    long inquiryCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM booking_inquiry", Long.class);
    }

    Map<String, Object> inquiryBody(String phone, String date) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "合成客人");
        body.put("customerPhone", phone);
        if (date != null) body.put("preferredDate", date);
        body.put("guestCount", 6);
        return body;
    }

    // ==================== 阻断一：门店列表 ====================

    @Test @Order(1)
    @DisplayName("空库时门店列表返回空列表，不报错")
    void emptyStoreListIsNotAnError() {
        var result = stores.list();
        assertEquals(200, result.getCode(), "空门店应返回空列表而不是错误：" + result.getMessage());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test @Order(2)
    @DisplayName("门店列表在真实 schema 上不再 Unknown column，且只返回存在的字段")
    void storeListRunsOnRealSchema() {
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,store_short_name,address,"
                + "phone,business_hours,status,sort_order) VALUES "
                + "(1,'S-1','又见炊烟宁国店','宁国店','宁国路1号','0551-1','10:00-22:00','open',2),"
                + "(2,'S-2','又见炊烟宣城店','宣城店','宣城路2号','0551-2','11:00-21:00','open',1),"
                + "(3,'S-3','尚未开业店','筹备店','某路3号','0551-3','待定','preparing',3)");

        var result = stores.list();
        assertEquals(200, result.getCode(), String.valueOf(result.getMessage()));
        List<Map<String, Object>> rows = result.getData();

        // 只有 status='open' 的两家可见，筹备中的不可见
        assertEquals(2, rows.size(), "只应返回 open 门店，实际：" + rows);
        // sort_order 2 号店在前；若按 store_id 排就会反过来
        assertEquals(2L, ((Number) rows.get(0).get("store_id")).longValue(), "排序未按 sort_order");
        assertEquals(1L, ((Number) rows.get(1).get("store_id")).longValue());

        // 字段集合固定，且**不包含**库里根本不存在的经纬度
        Set<String> expected = Set.of("store_id", "store_name", "store_short_name",
                "address", "phone", "business_hours");
        for (Map<String, Object> row : rows) {
            assertEquals(expected, row.keySet(), "返回字段与预期不符：" + row.keySet());
            assertFalse(row.containsKey("latitude"), "latitude 这列在库里根本不存在");
            assertFalse(row.containsKey("longitude"), "longitude 这列在库里根本不存在");
        }

        // 连查两次逐字相同，证明排序稳定
        assertEquals(rows, stores.list().getData());
    }

    @Test @Order(3)
    @DisplayName("库里确实没有 latitude/longitude 列——证明原实现必然 Unknown column")
    void latitudeColumnTrulyAbsent() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                        + "AND TABLE_NAME='store_info' AND COLUMN_NAME IN ('latitude','longitude')",
                Integer.class);
        assertEquals(0, n, "store_info 居然有经纬度列，本用例的前提要重新核");
        assertThrows(Exception.class,
                () -> jdbc.queryForList("SELECT latitude FROM store_info"),
                "查不存在的列本应直接报错——这就是原实现的病");
    }

    // ==================== 阻断二：咨询日期 ====================

    @Test @Order(4)
    @DisplayName("非法日期格式：拒绝且零落库")
    void malformedDateRejectedWithoutWrite() {
        for (String bad : List.of("2026-13-45", "not-a-date", "2026/03/01", "20260301", "2026-3-1 ")) {
            long before = inquiryCount();
            var result = inquiries.submit(inquiryBody("13800000001", bad));
            assertEquals(400, result.getCode(), "非法日期本应被拒绝：" + bad);
            assertTrue(result.getMessage().contains("yyyy-MM-dd"), result.getMessage());
            assertEquals(before, inquiryCount(), "被拒的请求不得落库：" + bad);
        }
    }

    @Test @Order(5)
    @DisplayName("过去日期：拒绝且零落库")
    void pastDateRejectedWithoutWrite() {
        long before = inquiryCount();
        var result = inquiries.submit(inquiryBody("13800000002", LocalDate.now().minusDays(1).toString()));
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("不能早于今天"), result.getMessage());
        assertEquals(before, inquiryCount(), "过去日期不得落库");
    }

    @Test @Order(6)
    @DisplayName("当天与未来日期正常提交并正确落库")
    void todayAndFutureAccepted() {
        for (LocalDate date : List.of(LocalDate.now(), LocalDate.now().plusDays(30))) {
            long before = inquiryCount();
            var result = inquiries.submit(inquiryBody("13800000003", date.toString()));
            assertEquals(200, result.getCode(), "当天/未来日期应可提交：" + date + " -> " + result.getMessage());
            assertEquals(before + 1, inquiryCount());
            Object id = result.getData().get("id");
            assertEquals(date, jdbc.queryForObject(
                    "SELECT preferred_date FROM booking_inquiry WHERE id=?", LocalDate.class, id),
                    "落库日期与提交不符");
        }
    }

    @Test @Order(7)
    @DisplayName("不填日期仍可提交：咨询本来就允许不指定日期")
    void missingDateStillAccepted() {
        long before = inquiryCount();
        var result = inquiries.submit(inquiryBody("13800000004", null));
        assertEquals(200, result.getCode(), String.valueOf(result.getMessage()));
        assertEquals(before + 1, inquiryCount());
        assertNull(jdbc.queryForObject("SELECT preferred_date FROM booking_inquiry WHERE id=?",
                LocalDate.class, result.getData().get("id")));
    }

    @Test @Order(8)
    @DisplayName("错误手机号：拒绝且零落库（既有行为，回归保护）")
    void badPhoneRejectedWithoutWrite() {
        for (String bad : List.of("12345", "23800000000", "1380000000a", "")) {
            long before = inquiryCount();
            var result = inquiries.submit(inquiryBody(bad, LocalDate.now().toString()));
            assertEquals(400, result.getCode(), "错误手机号本应被拒：" + bad);
            assertEquals(before, inquiryCount(), "被拒的请求不得落库：" + bad);
        }
    }

    @Test @Order(9)
    @DisplayName("日期在手机号之后校验，但都在写入之前——两者都非法时同样零落库")
    void bothInvalidStillNoWrite() {
        long before = inquiryCount();
        var result = inquiries.submit(inquiryBody("12345", "not-a-date"));
        assertEquals(400, result.getCode());
        assertEquals(before, inquiryCount(), "两者都非法时更不该落库");
    }
}
