package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.aop.*;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.FinancePayableService;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.*;
import java.net.InetAddress;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Browser -> real HTTP -> password AuthController -> JWT/aspects -> JPA/MySQL. No business mocks. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_BROWSER", matches="1")
class PayableBrowserIsolationTest {
    @Configuration @EnableWebMvc @EnableTransactionManagement @EnableAspectJAutoProxy(proxyTargetClass=true)
    static class Wiring implements WebMvcConfigurer {
        private final org.springframework.context.ApplicationContext beans;
        Wiring(org.springframework.context.ApplicationContext beans) {this.beans=beans;}
        @Override public void addInterceptors(InterceptorRegistry r) {r.addInterceptor(beans.getBean(JwtAuthInterceptor.class)).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login");}
        @Bean TomcatServletWebServerFactory webServer() throws Exception {
            var f=new TomcatServletWebServerFactory(0);f.setAddress(InetAddress.getByName("127.0.0.1"));return f;
        }
        @Bean DispatcherServlet dispatcherServlet() {return new DispatcherServlet();}
        @Bean ServletRegistrationBean<DispatcherServlet> registration(DispatcherServlet servlet) {return new ServletRegistrationBean<>(servlet,"/");}
    }
    @Test void browserFlow() throws Exception {
        Path evidence=Path.of("../docs/协作/Codex/payable-browser").toAbsolutePath().normalize();
        Files.createDirectories(evidence);
        String schema="payable_browser_"+UUID.randomUUID().toString().replace("-","");
        String root="jdbc:mysql://127.0.0.1:13317/",options="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        Files.writeString(evidence.resolve("schema-"+schema+".txt"),schema);
        new JdbcTemplate(new DriverManagerDataSource(root+options,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var ds=new DriverManagerDataSource(root+schema+options,"root","");var jdbc=new JdbcTemplate(ds);
        new ResourceDatabasePopulator(new ClassPathResource("restaurant-production-schema-20260906.sql"),new ClassPathResource("payable-metadata-fixture-20260907.sql"),
            new FileSystemResource("../scripts/migrations/receipt_payable_source_v1.sql"),new FileSystemResource("../scripts/migrations/payable_settlement_record_v1.sql"),new FileSystemResource("../scripts/migrations/payable_create_request_v1.sql")).execute(ds);
        jdbc.execute("ALTER TABLE staff_master ADD store_id BIGINT, ADD staff_account VARCHAR(60), ADD staff_phone VARCHAR(60), ADD staff_name VARCHAR(60), ADD staff_password VARCHAR(100), ADD role VARCHAR(30), ADD employment_status VARCHAR(20), ADD department VARCHAR(40), ADD staff_position VARCHAR(40), ADD permission_level INT");
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-B1','Synthetic Store1'),(2,'SYN-B2','Synthetic Store2')");
        String password=UUID.randomUUID().toString();String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_account,staff_name,staff_password,role,employment_status) VALUES(8001,1,'SYN-BROWSER-STAFF','Synthetic Staff',?,'store_manager','active'),(8002,0,'SYN-BROWSER-GM','Synthetic GM',?,'gm','active')",hash,hash);
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(60),action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        var emf=new LocalContainerEntityManagerFactoryBean();emf.setDataSource(ds);emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setManagedTypes(PersistenceManagedTypes.of(FinancePayable.class.getName(),PayableSettlementRecord.class.getName(),PayableCreateRequest.class.getName()));
        // Validate unchanged migration schema against the explicit CHAR entity mapping.
        assertEquals("char",jdbc.queryForObject("SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema=? AND table_name='payable_create_request' AND column_name='params_hash'",String.class,schema));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=? AND constraint_name='fk_payable_create_request_payable'",Integer.class,schema));
        assertEquals(64,jdbc.queryForObject("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema=? AND table_name='payable_create_request' AND column_name='params_hash'",Integer.class,schema));
        emf.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));emf.afterPropertiesSet();
        var factory=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf.getObject()));
        var context=new AnnotationConfigServletWebServerApplicationContext();
        String secret=UUID.randomUUID().toString()+UUID.randomUUID();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-only",Map.of("jwt.secret",secret,"jwt.expiration",3600000)));
        context.registerBean(JdbcTemplate.class,()->jdbc);context.registerBean(PlatformTransactionManager.class,()->new JpaTransactionManager(emf.getObject()));
        context.registerBean(FinancePayableRepository.class,()->factory.getRepository(FinancePayableRepository.class));
        context.registerBean(PayableSettlementRecordRepository.class,()->factory.getRepository(PayableSettlementRecordRepository.class));
        context.registerBean(PayableCreateRequestRepository.class,()->factory.getRepository(PayableCreateRequestRepository.class));
        context.register(Wiring.class,AuthController.class,FinancePayableController.class,FinancePayableService.class,JwtAuthInterceptor.class,StoreDataScopeAspect.class,AuditLogAspect.class);
        try {
            context.refresh();assertTrue(secret.equals(org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(JwtAuthInterceptor.class),"jwtSecret")),"isolated JWT injection must resolve before requests");int port=context.getWebServer().getPort();
            var pb=new ProcessBuilder("node",evidence.resolve("harness/browser.mjs").toString());pb.directory(evidence.toFile());
            pb.environment().put("PAYABLE_TEST_PORT",String.valueOf(port));pb.environment().put("PAYABLE_TEST_PASSWORD",password);
            pb.redirectErrorStream(true);pb.redirectOutput(evidence.resolve("browser.log").toFile());
            var proc=pb.start();boolean done=proc.waitFor(240,TimeUnit.SECONDS);if(!done)proc.destroy();assertTrue(done,"browser timeout; see redacted browser.log");assertEquals(0,proc.exitValue(),"browser flow failed; see browser.log");
            var json=new ObjectMapper();var browser=json.readTree(evidence.resolve("browser-result.json").toFile());
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable",Integer.class));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM payable_create_request",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payable_create_request WHERE CHAR_LENGTH(params_hash)<>64 OR params_hash NOT REGEXP '^[0-9a-f]{64}$'",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payable_settlement_record",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE total_amount<>paid_amount+pending_amount OR paid_amount<>0 OR store_id<>1",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payable_create_request r LEFT JOIN finance_payable p ON p.payable_id=r.payable_id AND p.store_id=r.store_id WHERE p.payable_id IS NULL",Integer.class));
            long restored=browser.path("restoredId").asLong();assertTrue(restored>0);
            assertEquals(restored,jdbc.queryForObject("SELECT payable_id FROM payable_create_request WHERE request_id=?",Long.class,browser.path("restoredRequestId").asText()));
            assertEquals(0,new BigDecimal("36.25").compareTo(jdbc.queryForObject("SELECT SUM(total_amount) FROM finance_payable",BigDecimal.class)));
            assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE target='AuthController.login'", Integer.class) >= 2);
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE LOCATE(?,detail)>0",Integer.class,password),
                    "authentication audit must not retain the runtime password");
            var rows=jdbc.queryForList("SELECT payable_id,store_id,total_amount,paid_amount,pending_amount,operator_name FROM finance_payable ORDER BY payable_id");
            assertTrue(rows.stream().allMatch(r->"SYN-BROWSER-STAFF".equals(r.get("operator_name"))));
            json.writerWithDefaultPrettyPrinter().writeValue(evidence.resolve("database-result.json").toFile(),Map.of("schema",schema,"rows",rows,"orphanCount",0,"requestCount",2,"sum","36.25","base",System.getenv().getOrDefault("PAYABLE_TEST_BASE","current-worktree")));
            assertNull(UserContext.get());
        } finally {context.close();emf.destroy();System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema);}
    }
}
