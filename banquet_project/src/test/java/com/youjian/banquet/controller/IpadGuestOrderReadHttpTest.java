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
class IpadGuestOrderReadHttpTest {
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
        emf.setManagedTypes(PersistenceManagedTypes.of(BookingMaster.class.getName(),BookingTable.class.getName(),BookingDishDetail.class.getName(),TableMaster.class.getName(),StoreInfo.class.getName(),StaffMaster.class.getName()));
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
        start();
    }
    void start(){
        context=new AnnotationConfigServletWebServerApplicationContext();context.registerBean(JdbcTemplate.class,()->jdbc);
        context.registerBean(PlatformTransactionManager.class,()->new DataSourceTransactionManager(ds));
        context.registerBean(IpadGuestOrderViewService.class,()->new IpadGuestOrderViewService(jdbc,clock));
        context.register(Wiring.class,IpadGuestOrderController.class,IpadInterceptor.class,WriteProbe.class);context.refresh();base="http://127.0.0.1:"+context.getWebServer().getPort();
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
    @Test void multiTableMultiDishStableIdsExactMoneyNoPiiAndNoWrites()throws Exception{
        var before=state();String token=grant();var r=detail(token);assertEquals(200,r.status);var d=r.body.path("data");
        assertEquals("50.98",d.path("total_amount").asText());assertEquals("active_dish_subtotal",d.path("amount_basis").asText());assertTrue(d.path("read_only").asBoolean());assertEquals("unpaid",d.path("payment_status").asText());
        assertEquals(2,d.path("tables").size());assertEquals(3,d.path("dishes").size());assertEquals(1,d.path("dishes").get(0).path("dish_booking_id").asLong());assertEquals(2,d.path("dishes").get(0).path("dish_quantity").asInt());assertEquals("40.98",d.path("dishes").get(0).path("subtotal").asText());
        assertFalse(r.body.toString().contains("PII-"));assertFalse(r.body.toString().contains("customer_name"));assertFalse(r.body.toString().contains("staff_id"));assertEquals(d,detail(token).body.path("data"));assertEquals(before,state());
        Files.writeString(Path.of("../docs/协作/Codex/guest-order-read/detail-example.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(r.body));
    }
    @Test void missingTokenAndGuessedBookingAreRejected()throws Exception{
        assertEquals(403,detail(null).status);assertEquals(403,detail("x".repeat(43)).status);
        var denied=call("POST","/api/ipad/order/view-authorize","SYN-A",1,Map.of("username","SYN11","password",password,"booking_id","GUESSED"),null);assertEquals(403,denied.status);
    }
    @Test void wrongDeviceStoreBookingAndDeviceHeaderScopeReject()throws Exception{
        String token=grant();var before=state();
        assertEquals(403,call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK","SYN-B",1,null,token).status);
        assertEquals(403,call("GET","/api/ipad/order/detail?booking_id=SYN-OTHER","SYN-C",2,null,token).status);
        assertEquals(403,call("GET","/api/ipad/order/detail?booking_id=SYN-OTHER","SYN-A",1,null,token).status);
        assertEquals(403,call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK","SYN-A",2,null,token).status);assertEquals(before,state());
    }
    @Test void badPasswordInactiveForeignAndPlaintextStaffCannotAuthorize()throws Exception{
        for(String user:List.of("SYN11","SYN12","SYN21","SYNPLAIN")){
            var r=call("POST","/api/ipad/order/view-authorize","SYN-A",1,Map.of("username",user,"password",user.equals("SYN11")?"wrong":password,"booking_id","SYN-BOOK"),null);assertEquals(403,r.status);assertFalse(r.body.path("data").has("order_view_token"));
        }
    }
    @Test void suspensionRevokesExistingTokenEvenAfterReactivation()throws Exception{
        String token=grant();jdbc.update("UPDATE staff_master SET employment_status='left' WHERE staff_id=11");assertEquals(403,detail(token).status);
        jdbc.update("UPDATE staff_master SET employment_status='active' WHERE staff_id=11");assertEquals(403,detail(token).status);assertEquals(200,detail(grant()).status);
    }
    @Test void exactThirtyMinuteExpiryRequiresNewAuthorization()throws Exception{
        String token=grant();clock.millis+=1799999;assertEquals(200,detail(token).status);clock.millis++;assertEquals(403,detail(token).status);assertEquals(200,detail(grant()).status);
    }
    @Test void restartLosesProcessLocalTokens()throws Exception{
        String token=grant();context.close();start();assertEquals(403,detail(token).status);assertEquals(200,detail(grant()).status);
    }
    @Test void closedOrdersRemainReadable()throws Exception{
        for(String status:List.of("completed","cancelled")){
            jdbc.update("UPDATE booking_master SET booking_status=? WHERE booking_id='SYN-BOOK'",status);var r=detail(grant());assertEquals(200,r.status);assertEquals(status,r.body.path("data").path("booking_status").asText());assertEquals("50.98",r.body.path("data").path("total_amount").asText());
        }
    }
    @Test void unavailableDishTableIsErrorNotZeroAndContainsNoSql()throws Exception{
        String token=grant();jdbc.execute("ALTER TABLE booking_dish_detail RENAME TO preserved_unavailable_dishes");var r=detail(token);assertEquals(503,r.status);assertFalse(r.body.has("data")&&!r.body.path("data").isNull());assertFalse(r.body.toString().contains("SELECT"));assertFalse(r.body.toString().contains("booking_dish_detail"));
    }
    @Test void invalidMoneyFailsRatherThanReturningInventedZero()throws Exception{
        String token=grant();jdbc.update("UPDATE booking_dish_detail SET subtotal=NULL WHERE dish_booking_id=1");var before=state();assertEquals(503,detail(token).status);assertEquals(before,state());
    }
    @Test void tokenCannotBePutInUrlOrAuthorizeWrites()throws Exception{
        String token=grant();int reached=WriteProbe.reached.get();assertEquals(400,call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK&order_view_token=forbidden","SYN-A",1,null,token).status);
        assertEquals(403,call("POST","/api/ipad/order/dish/add","SYN-A",1,Map.of("booking_id","SYN-BOOK"),token).status);assertEquals(reached,WriteProbe.reached.get());
    }
    @Test void missingAndUnknownParametersReturn400()throws Exception{
        assertEquals(400,call("GET","/api/ipad/order/detail","SYN-A",1,null,null).status);
        assertEquals(400,call("POST","/api/ipad/order/view-authorize","SYN-A",1,Map.of("username","SYN11","password",password,"booking_id","SYN-BOOK","extra",true),null).status);
    }

    @Test void paidStatusIsReturnedRawWhileViewRemainsReadOnly()throws Exception{
        jdbc.update("UPDATE booking_master SET payment_status='paid' WHERE booking_id='SYN-BOOK'");
        var d=detail(grant()).body.path("data");assertEquals("paid",d.path("payment_status").asText());assertEquals("confirmed",d.path("booking_status").asText());assertTrue(d.path("read_only").asBoolean());
    }
    @Test void mismatchedStoredSubtotalIsExplicitIncompleteDataNotHealthyAmount()throws Exception{
        String token=grant();jdbc.update("UPDATE booking_dish_detail SET subtotal=39.98 WHERE dish_booking_id=1");var before=state();
        var r=detail(token);assertEquals(503,r.status);assertTrue(r.body.path("message").asText().contains("数据不完整"));assertFalse(r.body.path("data").has("total_amount"));assertEquals(before,state());
    }

}
