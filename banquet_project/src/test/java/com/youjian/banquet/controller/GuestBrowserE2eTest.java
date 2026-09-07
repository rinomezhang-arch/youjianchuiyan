package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.config.IpadInterceptor;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.service.IpadGuestOrderViewService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual servlet HTTP + real binding lookup/BCrypt/transactions/DB; no mocked authorization or business. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="GUEST_BROWSER_E2E", matches="1")
class GuestBrowserE2eTest {
    // Routing sentinel only. Never substitutes successful business or authorization.
    @org.springframework.web.bind.annotation.RestController
    static class WriteProbe {
        static final java.util.concurrent.atomic.AtomicInteger reached=new java.util.concurrent.atomic.AtomicInteger();
        @org.springframework.web.bind.annotation.PostMapping("/api/ipad/order/dish/add")
        public String forbidden() {reached.incrementAndGet();throw new AssertionError("read capability reached write handler");}
    }
    @Configuration @EnableWebMvc @EnableTransactionManagement
    static class Wiring implements WebMvcConfigurer {
        private final org.springframework.context.ApplicationContext beans;
        Wiring(org.springframework.context.ApplicationContext beans){this.beans=beans;}
        public void addInterceptors(InterceptorRegistry r){r.addInterceptor(beans.getBean(IpadInterceptor.class)).addPathPatterns("/api/ipad/**");}
        @Bean TomcatServletWebServerFactory webServer()throws Exception{var f=new TomcatServletWebServerFactory(0);f.setAddress(InetAddress.getByName("127.0.0.1"));return f;}
        @Bean DispatcherServlet dispatcher(){return new DispatcherServlet();}
        @Bean ServletRegistrationBean<DispatcherServlet> servlet(DispatcherServlet d){return new ServletRegistrationBean<>(d,"/");}
    }
    static class MutableClock extends Clock {
        long millis=1000000;
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.ofEpochMilli(millis);}
    }
    JdbcTemplate jdbc;DriverManagerDataSource ds;LocalContainerEntityManagerFactoryBean emf;
    AnnotationConfigServletWebServerApplicationContext context;String schema,password,base;
    MutableClock clock=new MutableClock();ObjectMapper json=new ObjectMapper();HttpClient client=HttpClient.newHttpClient();
    @BeforeEach void setup() throws Exception {
        schema="guest_read_"+UUID.randomUUID().toString().replace("-","");
        String host="jdbc:mysql://127.0.0.1:13317/",opts="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        new JdbcTemplate(new DriverManagerDataSource(host+opts,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema);
        ds=new DriverManagerDataSource(host+schema+opts,"root","");jdbc=new JdbcTemplate(ds);
        emf=new LocalContainerEntityManagerFactoryBean();emf.setDataSource(ds);emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setManagedTypes(PersistenceManagedTypes.of(BookingMaster.class.getName(),BookingTable.class.getName(),BookingDishDetail.class.getName(),TableMaster.class.getName(),StoreInfo.class.getName(),StaffMaster.class.getName(),DishMaster.class.getName(),KitchenLog.class.getName(),MenuCategory.class.getName(),PackageMaster.class.getName(),PackageDishDetail.class.getName(),BanquetTemplate.class.getName(),TemplateDishRel.class.getName()));
        emf.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","update"));emf.afterPropertiesSet();
        String binding=Files.readString(Path.of("../scripts/migrations/ipad_device_binding_migration_v1.sql")).replace("staff_id BIGINT NULL","staff_id INT NULL").replace("REFERENCES store_info(id)","REFERENCES store_info(store_id)").replace("REFERENCES staff_master(id)","REFERENCES staff_master(staff_id)");
        jdbc.execute(binding);
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-G1','Synthetic1'),(2,'SYN-G2','Synthetic2')");
        password=UUID.randomUUID().toString();String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_account,staff_name,employment_status,staff_password) VALUES(11,1,'SYN11','Synthetic Staff','active',?),(12,1,'SYN12','Synthetic Staff2','left',?),(21,2,'SYN21','Synthetic Other','active',?),(13,1,'SYNPLAIN','Synthetic Plain','active',?)",hash,hash,hash,password);
        jdbc.update("INSERT INTO ipad_device_binding(device_sn,store_id,staff_id) VALUES('SYN-A',1,NULL),('SYN-B',1,NULL),('SYN-C',2,NULL)");
        jdbc.update("INSERT INTO booking_master(booking_id,store_id,booking_status,customer_name,customer_phone,total_amount) VALUES('SYN-BOOK',1,'confirmed','PII-SYN-NAME','PII-SYN-PHONE',9999),('SYN-OTHER',2,'confirmed','PII-OTHER','PII-OTHER-PHONE',8888)");
        jdbc.update("UPDATE booking_master SET payment_status='unpaid'");
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_name,table_number) VALUES(101,1,'Synthetic A','A'),(102,1,'Synthetic B','B'),(201,2,'Other','C')");
        jdbc.update("INSERT INTO booking_table(table_booking_id,booking_id,store_id,table_id,table_name) VALUES(1,'SYN-BOOK',1,101,'Synthetic A'),(2,'SYN-BOOK',1,102,'Synthetic B'),(3,'SYN-OTHER',2,201,'Other')");
        jdbc.update("INSERT INTO booking_dish_detail(dish_booking_id,booking_id,store_id,dish_id,dish_name,dish_quantity,unit_price,subtotal,kitchen_status,dish_note) VALUES(1,'SYN-BOOK',1,'D1','Synthetic fish',2,20.49,40.98,'pending','PII-NOTE'),(2,'SYN-BOOK',1,'D2','Synthetic rice',2,5,10,'served',NULL),(3,'SYN-BOOK',1,'D3','Synthetic zero',1,0,0,NULL,NULL),(4,'SYN-BOOK',1,'D4','Refunded',1,999,999,'refunded',NULL),(5,'SYN-BOOK',1,'D5','Cancelled',1,100,100,'cancelled',NULL),(6,'SYN-OTHER',2,'D6','Other',1,500,500,'pending',NULL)");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.ClassPathResource("ipad_batch_request_migration_v1.sql"),
            new org.springframework.core.io.ClassPathResource("ipad_batch_request_migration_v2.sql")).execute(ds);
        assertEquals(List.of("booking_master_id:id","store_id:store_id","booking_id:booking_id"),jdbc.queryForList("SELECT CONCAT(COLUMN_NAME,':',REFERENCED_COLUMN_NAME) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='ipad_batch_request' AND CONSTRAINT_NAME='fk_ipad_batch_booking_scope' ORDER BY ORDINAL_POSITION",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT @@SESSION.foreign_key_checks",Integer.class));
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active,dish_category) VALUES('MENU-A',1,'Synthetic Decimal',12.34,1,'Synthetic'),('MENU-B',1,'Synthetic Rice',5.67,1,'Synthetic')");
        start();
    }
    void start(){
        context=new AnnotationConfigServletWebServerApplicationContext();context.registerBean(JdbcTemplate.class,()->jdbc);
        context.registerBean(PlatformTransactionManager.class,()->new JpaTransactionManager(emf.getObject()));
        var em=SharedEntityManagerCreator.createSharedEntityManager(emf.getObject());
        context.registerBean(jakarta.persistence.EntityManager.class,()->em);
        var repos=new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(em);
        for(Class<?> type:List.of(com.youjian.banquet.repository.BookingMasterRepository.class,com.youjian.banquet.repository.BookingTableRepository.class,com.youjian.banquet.repository.BookingDishDetailRepository.class,com.youjian.banquet.repository.DishMasterRepository.class,com.youjian.banquet.repository.KitchenLogRepository.class,com.youjian.banquet.repository.MenuCategoryRepository.class,com.youjian.banquet.repository.PackageMasterRepository.class,com.youjian.banquet.repository.PackageDishDetailRepository.class,com.youjian.banquet.repository.BanquetTemplateRepository.class,com.youjian.banquet.repository.TemplateDishRelRepository.class))registerRepo(type,repos);
        context.getBeanFactory().registerSingleton("notifyTransportDouble",org.mockito.Mockito.mock(com.youjian.banquet.service.NotifyPublisher.class));
        context.registerBean(FixtureControl.class,()->new FixtureControl(jdbc,password));
        context.registerBean(IpadGuestOrderViewService.class,()->new IpadGuestOrderViewService(jdbc,clock));
        context.register(Wiring.class,IpadGuestOrderController.class,IpadInterceptor.class,IpadOrderController.class,IpadDishController.class,com.youjian.banquet.service.IpadBatchAuthorizationService.class);context.refresh();base="http://127.0.0.1:"+context.getWebServer().getPort();
    }
    @AfterEach void close(){if(context!=null)context.close();if(emf!=null)emf.destroy();}
    record Reply(int status,JsonNode body) {}
    Reply call(String method,String path,String device,int store,Map<String,Object> body,String token)throws Exception{
        var b=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(15)).header("X-Client-Type","ipad").header("X-Store-Id",String.valueOf(store)).header("X-Staff-Id","0").header("X-Device-Sn",device);
        if(token!=null)b.header("X-Order-View-Token",token);
        if(method.equals("POST"))b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));else b.GET();
        var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());return new Reply(response.statusCode(),json.readTree(response.body()));
    }
    String grant()throws Exception{
        var r=call("POST","/api/ipad/order/view-authorize","SYN-A",1,Map.of("username","SYN11","password",password,"booking_id","SYN-BOOK"),null);
        assertEquals(200,r.status);var d=r.body.path("data");assertEquals(1800,d.path("expires_in").asInt());assertEquals("ipad:order-view",d.path("purpose").asText());assertEquals("SYN-BOOK",d.path("booking_id").asText());return d.path("order_view_token").asText();
    }
    Reply detail(String token)throws Exception{return call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK","SYN-A",1,null,token);}
    List<Map<String,Object>> state(){return jdbc.queryForList("SELECT dish_booking_id,store_id,booking_id,dish_quantity,unit_price,subtotal,kitchen_status FROM booking_dish_detail ORDER BY dish_booking_id");}
    @SuppressWarnings({"unchecked","rawtypes"}) void registerRepo(Class type,org.springframework.data.jpa.repository.support.JpaRepositoryFactory repos){context.registerBean(type,()->repos.getRepository(type));}
    @org.springframework.web.bind.annotation.RestController static class FixtureControl {
        final JdbcTemplate jdbc;final String secret;FixtureControl(JdbcTemplate j,String s){jdbc=j;secret=s;}
        @org.springframework.web.bind.annotation.PostMapping("/__fixture/close") public Map<String,Object> close(@org.springframework.web.bind.annotation.RequestHeader("X-Fixture-Key") String key){
            if(!secret.equals(key))throw new SecurityException();
            jdbc.update("UPDATE booking_master SET booking_status='confirmed',payment_status='paid' WHERE store_id=1 AND booking_id='SYN-BOOK'");return Map.of("fixture_closed",true);
        }
    }
    @Test void browserFlow()throws Exception {
        Path evidence=Path.of("../docs/CO-guest-browser").toAbsolutePath().normalize();
        Process proc=null;
        try {
            var pb=new ProcessBuilder(System.getenv().getOrDefault("GUEST_NODE_BINARY","node"),evidence.resolve("harness/browser.mjs").toString());pb.directory(evidence.toFile());
            pb.environment().put("GUEST_TEST_PORT",String.valueOf(context.getWebServer().getPort()));pb.environment().put("GUEST_TEST_PASSWORD",password);
            pb.redirectErrorStream(true);pb.redirectOutput(evidence.resolve("browser.log").toFile());proc=pb.start();
            boolean done=proc.waitFor(180,java.util.concurrent.TimeUnit.SECONDS);assertTrue(done,"browser timeout");assertEquals(0,proc.exitValue(),"browser failed; see safe browser.log");
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request r LEFT JOIN booking_master b ON b.id=r.booking_master_id AND b.store_id=r.store_id AND b.booking_id=r.booking_id WHERE b.id IS NULL",Integer.class));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE dish_id='MENU-A'",Integer.class));
            assertEquals(0,new java.math.BigDecimal("24.68").compareTo(jdbc.queryForObject("SELECT SUM(subtotal) FROM booking_dish_detail WHERE dish_id='MENU-A'",java.math.BigDecimal.class)));
            var beforeFk=jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id");
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE ipad_batch_request SET store_id=2 WHERE request_id=(SELECT x.id FROM (SELECT MIN(request_id) id FROM ipad_batch_request) x)"));
            assertEquals(beforeFk,jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
            var rows=jdbc.queryForList("SELECT client_request_id,result_json FROM ipad_batch_request ORDER BY request_id");
            var mapper=new ObjectMapper();for(var row:rows){var receipt=mapper.readTree(row.get("result_json").toString());for(var id:receipt.path("dish_booking_ids"))assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE dish_booking_id=? AND booking_id='SYN-BOOK' AND store_id=1",Integer.class,id.longValue()));}
            mapper.writerWithDefaultPrettyPrinter().writeValue(evidence.resolve("db-result.json").toFile(),Map.of("schema",schema,"receipts",rows,"newAmount","24.68","orphans",0,"fixtureClosure",true,"scopeFkV2",true));
        } finally {if(proc!=null&&proc.isAlive())proc.destroy();}
    }
}
