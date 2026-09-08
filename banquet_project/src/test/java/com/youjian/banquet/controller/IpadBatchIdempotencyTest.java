package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.config.IpadInterceptor;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.service.IpadBatchAuthorizationService;
import com.youjian.banquet.service.IpadBatchSubmissionService;
import com.youjian.banquet.service.NotifyPublisher;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DL-IPAD-BATCH-AUTH-16 真实两表回读 + client_request_id 幂等（地龙）
 * 真实 MySQL（staff/store/dish/booking/ipad_batch_request 均真实建表落库），仅 NotifyPublisher 为 mock。
 * 最小反例：同 client_request_id 同 payload 重放 → 菜品 1 份、回执 1 份、返回一致；改 payload → 409 拒绝零新增。
 */
class IpadBatchIdempotencyTest {
    String schema;
    JdbcTemplate jdbc;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    NotifyPublisher notifications = mock(NotifyPublisher.class);

    static class MutableClock extends java.time.Clock {
        long now = 1000000;
        public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        public java.time.Instant instant() { return java.time.Instant.ofEpochMilli(now); }
    }
    MutableClock clock = new MutableClock();
    IpadBatchAuthorizationService grants = new IpadBatchAuthorizationService(clock);

    @BeforeEach void setup() throws Exception {
        schema = "ipad_idem_" + UUID.randomUUID().toString().replace("-", "");
        String base = "jdbc:mysql://127.0.0.1:13317/";
        String options = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        new JdbcTemplate(new DriverManagerDataSource(base + options, "root", "")).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(base + schema + options, "root", ""));
        // 真实建表：staff_master / store_info / ipad_device_binding / booking_master / dish_master / booking_dish_detail / ipad_batch_request
        String captured;
        try (var in = new ClassPathResource("restaurant-production-schema-20260906.sql").getInputStream()) {
            captured = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String marker : List.of("CREATE TABLE staff_master (", "CREATE TABLE `store_info` (")) {
            int s = captured.indexOf(marker); assertTrue(s >= 0, "schema marker missing: " + marker);
            jdbc.execute(captured.substring(s, captured.indexOf(';', s)));
        }
        String ddl = Files.readString(Path.of("../scripts/migrations/ipad_device_binding_migration_v1.sql"))
            .replace("staff_id BIGINT NULL", "staff_id INT NULL")
            .replace("REFERENCES store_info(id)", "REFERENCES store_info(store_id)")
            .replace("REFERENCES staff_master(id)", "REFERENCES staff_master(staff_id)");
        jdbc.execute(ddl);
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'IDEM','Idem Store')");
        jdbc.execute("ALTER TABLE staff_master ADD store_id BIGINT, ADD staff_phone VARCHAR(60), ADD staff_account VARCHAR(60), ADD staff_password VARCHAR(100), ADD staff_name VARCHAR(60), ADD role VARCHAR(30), ADD employment_status VARCHAR(30)");
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("idem-test-password");
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_account,staff_password,staff_name,role,employment_status) VALUES(11,1,'IDEM11',?,'Idem Staff','staff','active')", hash);
        jdbc.update("INSERT INTO ipad_device_binding(device_sn,store_id,staff_id) VALUES('IDEM-DEV',1,11)");
        // 真实 booking_master
        jdbc.execute("CREATE TABLE booking_master(id BIGINT PRIMARY KEY AUTO_INCREMENT,booking_id VARCHAR(255),store_id BIGINT,booking_status VARCHAR(30),payment_status VARCHAR(30)) ENGINE=InnoDB");
        jdbc.update("INSERT INTO booking_master(booking_id,store_id,booking_status,payment_status) VALUES('IDEM-BOOK',1,'confirmed','unpaid')");
        // 真实 dish_master
        jdbc.execute("CREATE TABLE dish_master(dish_id VARCHAR(255),store_id BIGINT,dish_name VARCHAR(255),sale_price DECIMAL(12,2),is_active BIT,PRIMARY KEY(dish_id,store_id)) ENGINE=InnoDB");
        jdbc.update("INSERT INTO dish_master VALUES('IDEM-DISH',1,'Idem Dish',10.00,1)");
        // 真实 booking_dish_detail（与生产同结构的最小字段）
        jdbc.execute("CREATE TABLE booking_dish_detail(dish_booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,store_id BIGINT,table_booking_id BIGINT,booking_id VARCHAR(255),dish_id VARCHAR(255),dish_name VARCHAR(255),dish_quantity INT,unit_price DECIMAL(10,2),subtotal DECIMAL(10,2),custom_name VARCHAR(255),dish_note VARCHAR(255),dish_order INT,kitchen_status VARCHAR(30),kitchen_station VARCHAR(50),kitchen_note VARCHAR(255),kitchen_started_at DATETIME,kitchen_done_at DATETIME,created_at DATETIME) ENGINE=InnoDB");
        // 真实 ipad_batch_request
        try (var in = new ClassPathResource("ipad_batch_request_migration_v1.sql").getInputStream()) {
            jdbc.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        // 真实 repository（真实 MySQL，非 mock）
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(jdbc.getDataSource());
        factory.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(BookingDishDetail.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();
        jakarta.persistence.EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        org.springframework.data.jpa.repository.support.JpaRepositoryFactory rf = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(em);
        BookingDishDetailRepository realDetails = rf.getRepository(BookingDishDetailRepository.class);
        BookingMasterRepository bookings = mock(BookingMasterRepository.class);
        DishMasterRepository dishes = mock(DishMasterRepository.class);

        var controller = new IpadOrderController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(controller, "bookingRepo", bookings);
        ReflectionTestUtils.setField(controller, "dishRepo", dishes);
        ReflectionTestUtils.setField(controller, "dishDetailRepo", realDetails);
        ReflectionTestUtils.setField(controller, "notifyPublisher", notifications);
        ReflectionTestUtils.setField(controller, "batchAuthorization", grants);
        ReflectionTestUtils.setField(controller, "batchSubmission", new IpadBatchSubmissionService(jdbc, realDetails, notifications));

        var proxy = new ProxyFactory(controller);
        proxy.addAdvice(new TransactionInterceptor(new org.springframework.orm.jpa.JpaTransactionManager(factory.getObject()), new AnnotationTransactionAttributeSource()));
        mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy()).addInterceptors(new IpadInterceptor(jdbc)).build();
    }

    @AfterEach void retained() { System.out.println("IDEM_SCHEMA_RETAINED=" + schema); }

    MockHttpServletRequestBuilder req(String route, String device, long store, Map<String,Object> body) throws Exception {
        return post("/api/ipad/" + route).header("X-Client-Type","ipad").header("X-Device-Sn",device)
            .header("X-Store-Id",store).header("X-Staff-Id",0).contentType("application/json").content(json.writeValueAsBytes(body));
    }
    String authorize() throws Exception {
        var r = mvc.perform(req("auth/verify","IDEM-DEV",1,Map.of("username","IDEM11","password","idem-test-password","booking_id","IDEM-BOOK"))).andReturn();
        assertEquals(200, json.readTree(r.getResponse().getContentAsByteArray()).path("code").asInt());
        return json.readTree(r.getResponse().getContentAsByteArray()).path("data").path("authorization_token").asText();
    }
    int submit(String token, String crId, String dishId, int qty) throws Exception {
        var body = new HashMap<String,Object>();
        body.put("client_request_id", crId);
        body.put("booking_id", "IDEM-BOOK");
        body.put("dishes", List.of(Map.of("dish_id", dishId, "dish_quantity", qty)));
        body.put("authorization_token", token);
        var r = mvc.perform(req("order/add-dishes","IDEM-DEV",1,body)).andReturn();
        return json.readTree(r.getResponse().getContentAsByteArray()).path("code").asInt();
    }

    @Test void firstSubmitPersistsOneDishAndOneReceipt() throws Exception {
        String token = authorize();
        assertEquals(200, submit(token, "IDEM-REQ-00000000000001", "IDEM-DISH", 1));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='IDEM-BOOK'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='IDEM-REQ-00000000000001'", Integer.class));
        assertEquals(11, jdbc.queryForObject("SELECT operator_id FROM ipad_batch_request WHERE client_request_id='IDEM-REQ-00000000000001'", Integer.class));
    }

    @Test void sameRequestIdSamePayloadReplayIsIdempotent() throws Exception {
        String token1 = authorize();
        assertEquals(200, submit(token1, "IDEM-REQ-00000000000002", "IDEM-DISH", 1));
        String token2 = authorize(); // 新授权
        assertEquals(200, submit(token2, "IDEM-REQ-00000000000002", "IDEM-DISH", 1));
        // 菜品仍 1 份、回执仍 1 份
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='IDEM-BOOK'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='IDEM-REQ-00000000000002'", Integer.class));
    }

    @Test void sameRequestIdChangedPayloadRejected() throws Exception {
        String token1 = authorize();
        assertEquals(200, submit(token1, "IDEM-REQ-00000000000003", "IDEM-DISH", 1));
        String token2 = authorize();
        assertEquals(409, submit(token2, "IDEM-REQ-00000000000003", "IDEM-DISH", 3));
        // 零新增
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='IDEM-BOOK'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='IDEM-REQ-00000000000003'", Integer.class));
    }

    @Test void replayedOneUseTokenRejected() throws Exception {
        String token = authorize();
        assertEquals(200, submit(token, "IDEM-REQ-00000000000004", "IDEM-DISH", 1));
        assertEquals(403, submit(token, "IDEM-REQ-00000000000005", "IDEM-DISH", 1));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='IDEM-BOOK'", Integer.class));
    }
}
