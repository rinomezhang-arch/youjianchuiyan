package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.aop.AuditLogAspect;
import com.youjian.banquet.config.IpadInterceptor;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.KitchenLogRepository;
import com.youjian.banquet.repository.SysNotificationRepository;
import com.youjian.banquet.service.NotifyPublisher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CO-AUTH-CONTEXT-RECOVERY-26：真实监听端口下贯通 iPad 拦截器、写控制器、审计切面和 MySQL。
 * 业务主数据使用合成对象，数据库仅使用本测试独占并保留的合成 schema；不访问生产或法务数据。
 */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@SpringBootTest(
        classes = IpadAuditIdentityHttpMysqlTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.banner-mode=off")
class IpadAuditIdentityHttpMysqlTest {

    private static final String SCHEMA = "ipad_audit_" + UUID.randomUUID().toString().replace("-", "");
    private static final int MYSQL_PORT = Integer.parseInt(requiredEnvironment("YOUJIAN_TEST_MYSQL_PORT"));
    private static final String EXPECTED_DATA_DIR = normalizeDataDir(
            requiredEnvironment("YOUJIAN_TEST_MYSQL_DATADIR"));
    private static final String JDBC_HOST = "jdbc:mysql://127.0.0.1:" + MYSQL_PORT + "/";
    private static final String JDBC_OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final DataSource DATA_SOURCE;
    private static final JdbcTemplate JDBC;

    static {
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(JDBC_HOST + JDBC_OPTIONS, "root", ""));
        Map<String, Object> server = admin.queryForMap("SELECT @@port AS port, @@datadir AS datadir");
        int actualPort = ((Number) server.get("port")).intValue();
        String actualDataDir = normalizeDataDir(String.valueOf(server.get("datadir")));
        if (actualPort != MYSQL_PORT || !actualDataDir.equalsIgnoreCase(EXPECTED_DATA_DIR)) {
            throw new IllegalStateException("Refusing non-isolated MySQL: port=" + actualPort
                    + ", datadir=" + actualDataDir);
        }
        admin.execute("CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4");
        DATA_SOURCE = new DriverManagerDataSource(JDBC_HOST + SCHEMA + JDBC_OPTIONS, "root", "");
        JDBC = new JdbcTemplate(DATA_SOURCE);
        JDBC.execute("CREATE TABLE ipad_device_binding(device_sn VARCHAR(128) PRIMARY KEY,"
                + "store_id BIGINT NOT NULL,staff_id BIGINT NULL,status VARCHAR(20) NOT NULL DEFAULT 'active')");
        JDBC.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(64),"
                + "action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        JDBC.update("INSERT INTO ipad_device_binding(device_sn,store_id,staff_id,status) VALUES(?,?,NULL,'active')",
                "SYN-AUDIT-DEVICE", 1L);
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

    @LocalServerPort
    int port;

    @Autowired
    BookingDishDetailRepository dishDetails;

    private final ObjectMapper json = new ObjectMapper();

    @AfterAll
    static void retained() {
        System.out.println("IPAD_AUDIT_EVIDENCE port=random schema=" + SCHEMA + " retained=true");
    }

    @Test
    void forgedEmployeeHeaderCannotBecomeAuditUserOnRealIpadWrite() throws Exception {
        Map<String, Object> body = Map.of(
                "booking_id", "SYN-AUDIT-BOOKING",
                "dishes", java.util.List.of(Map.of("dish_id", "SYN-AUDIT-DISH", "dish_quantity", 1)));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/ipad/order/add-dishes"))
                .header("Content-Type", "application/json")
                .header("X-Client-Type", "ipad")
                .header("X-Device-Sn", "SYN-AUDIT-DEVICE")
                .header("X-Store-Id", "1")
                .header("X-Staff-Id", "900001")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), response.body());
        assertEquals(200, json.readTree(response.body()).path("code").asInt(), response.body());
        verify(dishDetails).save(any(BookingDishDetail.class));

        Map<String, Object> audit = JDBC.queryForMap(
                "SELECT user_id,action,target,store_id FROM audit_logs ORDER BY id DESC LIMIT 1");
        assertEquals("ipad-device:SYN-AUDIT-DEVICE", String.valueOf(audit.get("user_id")));
        assertNotEquals("900001", String.valueOf(audit.get("user_id")),
                "客户端伪造的 X-Staff-Id 进入了 audit_logs.user_id");
        assertEquals("POST /api/ipad/order/add-dishes", audit.get("action"));
        assertEquals("IpadOrderController.addDishesBatch", audit.get("target"));
        assertEquals(1L, ((Number) audit.get("store_id")).longValue());
        System.out.println("IPAD_AUDIT_EVIDENCE port=" + port + " schema=" + SCHEMA
                + " audit_user=ipad-device:SYN-AUDIT-DEVICE forged_user_rejected=true");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return DATA_SOURCE;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        AuditLogAspect auditLogAspect() {
            return new AuditLogAspect();
        }

        @Bean
        IpadInterceptor ipadInterceptor(JdbcTemplate jdbcTemplate) {
            return new IpadInterceptor(jdbcTemplate);
        }

        @Bean
        WebMvcConfigurer ipadWebMvcConfigurer(IpadInterceptor ipadInterceptor) {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(ipadInterceptor).addPathPatterns("/api/ipad/**");
                }
            };
        }

        @Bean
        IpadOrderController ipadOrderController() {
            return new IpadOrderController();
        }

        @Bean
        BookingMasterRepository bookingMasterRepository() {
            BookingMasterRepository repository = mock(BookingMasterRepository.class);
            BookingMaster booking = new BookingMaster();
            booking.setBookingId("SYN-AUDIT-BOOKING");
            booking.setStoreId(1L);
            booking.setBookingStatus("confirmed");
            when(repository.findForOrderUpdate("SYN-AUDIT-BOOKING", 1L)).thenReturn(Optional.of(booking));
            return repository;
        }

        @Bean
        DishMasterRepository dishMasterRepository() {
            DishMasterRepository repository = mock(DishMasterRepository.class);
            DishMaster dish = new DishMaster();
            dish.setDishId("SYN-AUDIT-DISH");
            dish.setStoreId(1L);
            dish.setDishName("Synthetic audit dish");
            dish.setSalePrice(new BigDecimal("12.50"));
            when(repository.findByDishIdAndStoreId(eq("SYN-AUDIT-DISH"), eq(1L)))
                    .thenReturn(Optional.of(dish));
            return repository;
        }

        @Bean
        BookingDishDetailRepository bookingDishDetailRepository() {
            BookingDishDetailRepository repository = mock(BookingDishDetailRepository.class);
            when(repository.save(any(BookingDishDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
            return repository;
        }

        @Bean BookingTableRepository bookingTableRepository() { return mock(BookingTableRepository.class); }
        @Bean KitchenLogRepository kitchenLogRepository() { return mock(KitchenLogRepository.class); }
        @Bean SysNotificationRepository sysNotificationRepository() { return mock(SysNotificationRepository.class); }
        @Bean NotifyPublisher notifyPublisher() { return mock(NotifyPublisher.class); }
        @Bean EntityManager entityManager() { return mock(EntityManager.class); }
    }
}
