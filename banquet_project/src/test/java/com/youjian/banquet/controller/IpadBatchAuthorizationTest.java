package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.config.IpadInterceptor;
import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.NotifyPublisher;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Scoped authorization regression. Real password/JDBC/interceptor/controller and one-use grant.
 * Real interceptor, controller and JDBC binding lookup. Downstream JPA/notification are doubles.
 * No full app. Synthetic booking/menu/receipt SQL; downstream dish JPA remains a double. Dedicated idempotency suite verifies real JPA.
 */
class IpadBatchAuthorizationTest {
    String schema;
    static class MutableClock extends java.time.Clock {
        long now = 1000000;
        public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        public java.time.Instant instant() { return java.time.Instant.ofEpochMilli(now); }
    }
    MutableClock clock = new MutableClock();
    com.youjian.banquet.service.IpadBatchAuthorizationService grants = new com.youjian.banquet.service.IpadBatchAuthorizationService(clock);
    JdbcTemplate jdbc;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    BookingMasterRepository bookings = mock(BookingMasterRepository.class);
    DishMasterRepository dishes = mock(DishMasterRepository.class);
    BookingDishDetailRepository details = mock(BookingDishDetailRepository.class);
    NotifyPublisher notifications = mock(NotifyPublisher.class);

    @BeforeEach void setup() throws Exception {
        schema = "ipad_auth2_" + UUID.randomUUID().toString().replace("-", "");
        String host = "jdbc:mysql://127.0.0.1:13317/", options = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        new JdbcTemplate(new DriverManagerDataSource(host + options, "root", "")).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(host + schema + options, "root", ""));
        String captured;
        try (var in = new ClassPathResource("restaurant-production-schema-20260906.sql").getInputStream()) {
            captured = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String marker : List.of("CREATE TABLE staff_master (", "CREATE TABLE `store_info` (")) {
            int start = captured.indexOf(marker); assertTrue(start >= 0);
            jdbc.execute(captured.substring(start, captured.indexOf(';', start)));
        }
        // Explicit fixture adaptation only: original migration refers to nonexistent id columns.
        String ddl = Files.readString(Path.of("../scripts/migrations/ipad_device_binding_migration_v1.sql"))
            .replace("staff_id BIGINT NULL", "staff_id INT NULL")
            .replace("REFERENCES store_info(id)", "REFERENCES store_info(store_id)")
            .replace("REFERENCES staff_master(id)", "REFERENCES staff_master(staff_id)");
        jdbc.execute(ddl);
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-IPAD','Synthetic')");
        // Minimal synthetic auth columns, not a claim of full employee schema migration coverage.
        jdbc.execute("ALTER TABLE staff_master ADD store_id BIGINT, ADD staff_phone VARCHAR(60), ADD staff_account VARCHAR(60), ADD staff_password VARCHAR(100), ADD staff_name VARCHAR(60), ADD role VARCHAR(30), ADD employment_status VARCHAR(30)");
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("synthetic-test-password");
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_phone,staff_account,staff_password,staff_name,role,employment_status) VALUES(11,1,'SYN11','SYN11',?,'Synthetic11','staff','active'),(12,1,'SYN12','SYN12',?,'Synthetic12','staff','active'),(21,2,'SYN21','SYN21',?,'Synthetic21','staff','active')",hash,hash,hash);
        jdbc.update("INSERT INTO ipad_device_binding(device_sn,store_id,staff_id) VALUES('SYN-EMPTY',1,NULL),('SYN-BOUND',1,11)");
        BookingMaster booking = new BookingMaster(); booking.setBookingId("SYN-BOOK"); booking.setStoreId(1L); booking.setBookingStatus("confirmed");
        when(bookings.findForOrderUpdate("SYN-BOOK", 1L)).thenReturn(Optional.of(booking));
        when(bookings.findByBookingIdAndStoreId("SYN-BOOK", 1L)).thenReturn(Optional.of(booking));
        DishMaster dish = new DishMaster(); dish.setDishId("SYN-DISH"); dish.setStoreId(1L); dish.setDishName("Synthetic"); dish.setSalePrice(new BigDecimal("12.50"));
        when(dishes.findByDishIdAndStoreId("SYN-DISH", 1L)).thenReturn(Optional.of(dish));
        when(details.saveAndFlush(any(BookingDishDetail.class))).thenAnswer(inv -> { BookingDishDetail d = inv.getArgument(0); d.setDishBookingId(81L); return d; });
        jdbc.execute("CREATE TABLE booking_master(id BIGINT PRIMARY KEY,booking_id VARCHAR(255),store_id BIGINT,booking_status VARCHAR(30),payment_status VARCHAR(30)) ENGINE=InnoDB");
        jdbc.update("INSERT INTO booking_master VALUES(1,'SYN-BOOK',1,'confirmed','unpaid')");
        jdbc.execute("CREATE TABLE dish_master(dish_id VARCHAR(255),store_id BIGINT,dish_name VARCHAR(255),sale_price DECIMAL(12,2),is_active BIT,PRIMARY KEY(dish_id,store_id)) ENGINE=InnoDB");
        jdbc.update("INSERT INTO dish_master VALUES('SYN-DISH',1,'Synthetic',12.50,1)");
        try(var in=new ClassPathResource("ipad_batch_request_migration_v1.sql").getInputStream()) {jdbc.execute(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
        var controller = new IpadOrderController();
        ReflectionTestUtils.setField(controller,"jdbcTemplate",jdbc);
        ReflectionTestUtils.setField(controller,"bookingRepo",bookings);
        ReflectionTestUtils.setField(controller,"dishRepo",dishes);
        ReflectionTestUtils.setField(controller,"dishDetailRepo",details);
        ReflectionTestUtils.setField(controller,"notifyPublisher",notifications);
        ReflectionTestUtils.setField(controller,"batchAuthorization",grants);
        var login = new IpadAuthController(); ReflectionTestUtils.setField(login,"jdbc",jdbc);
        var menu = new IpadDishController(); ReflectionTestUtils.setField(menu,"dishRepo",dishes);
        when(dishes.findByStoreId(1L)).thenReturn(List.of(dish));
        var proxy=new org.springframework.aop.framework.ProxyFactory(controller);
        proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy(),login,menu).addInterceptors(new IpadInterceptor(jdbc)).build();
    }
    @AfterEach void retained() { System.out.println("SYNTHETIC_SCHEMA_RETAINED=" + schema); }
    MockHttpServletRequestBuilder req(String route, String device, long staff, long store, Map<String,Object> body) throws Exception {
        return post("/api/ipad/"+route).header("X-Client-Type","ipad").header("X-Device-Sn",device)
            .header("X-Store-Id",store).header("X-Staff-Id",staff).contentType("application/json").content(json.writeValueAsBytes(body));
    }
    Map<String,Object> batch() { return new HashMap<>(Map.of("client_request_id","SYNTHETIC-AUTH-REQUEST-0001","booking_id","SYN-BOOK","dishes",List.of(Map.of("dish_id","SYN-DISH","dish_quantity",2)))); }
    void successful(MvcResult result) throws Exception {
        assertEquals(200,result.getResponse().getStatus());
        assertEquals(200,json.readTree(result.getResponse().getContentAsByteArray()).path("code").asInt());
        var write = ArgumentCaptor.forClass(BookingDishDetail.class); verify(details).saveAndFlush(write.capture());
        assertEquals(1L,write.getValue().getStoreId()); assertEquals("SYN-BOOK",write.getValue().getBookingId());
        assertEquals("SYN-DISH",write.getValue().getDishId());
    }
    int code(MvcResult r) throws Exception { return json.readTree(r.getResponse().getContentAsByteArray()).path("code").asInt(); }
    String authorize(String device, String account) throws Exception {
        var r=mvc.perform(req("auth/verify",device,0,1,Map.of("username",account,"password","synthetic-test-password","booking_id","SYN-BOOK"))).andReturn();
        assertEquals(200,code(r));
        var data=json.readTree(r.getResponse().getContentAsByteArray()).path("data");
        assertEquals("ipad:batch-add",data.path("purpose").asText()); assertEquals(120,data.path("expires_in").asInt());
        return data.path("authorization_token").asText();
    }
    MvcResult submit(String token,String device,String booking) throws Exception {
        var body=batch(); body.put("booking_id",booking); if(token!=null)body.put("authorization_token",token);
        return mvc.perform(req("order/add-dishes",device,0,1,body)).andReturn();
    }
    @Test void missingRequestIdRejectsWithoutConsumingValidAuthorization() throws Exception {
        String token=authorize("SYN-EMPTY","SYN12");clearInvocations(bookings);
        var body=batch();body.remove("client_request_id");body.put("authorization_token",token);
        assertEquals(400,code(mvc.perform(req("order/add-dishes","SYN-EMPTY",0,1,body)).andReturn()));
        verifyNoInteractions(bookings,details,notifications);
        successful(submit(token,"SYN-EMPTY","SYN-BOOK"));
    }
    @Test void emptyBindingHeaderCannotBecomeEmployeeOrWriteSingleDish() throws Exception {
        var r=mvc.perform(req("order/dish/add","SYN-EMPTY",900001,1,Map.of("booking_id","SYN-BOOK","dish_id","SYN-DISH"))).andReturn();
        assertEquals(403,r.getResponse().getStatus());assertNull(r.getRequest().getAttribute("ipad_staff_id"));verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void forgedBodyAndMissingTokenRejectBeforeBusinessAccess() throws Exception {
        var body=batch();body.put("staff_id",900002);
        assertEquals(400,code(mvc.perform(req("order/add-dishes","SYN-BOUND",11,1,body)).andReturn()));
        assertEquals(403,code(submit(null,"SYN-BOUND","SYN-BOOK")));verifyNoInteractions(bookings,dishes,details,notifications);
    }
    @Test void realPasswordVerifyThenSharedDeviceBatchUsesVerifiedEmployee() throws Exception {
        String token=authorize("SYN-EMPTY","SYN12");
        var r=submit(token,"SYN-EMPTY","SYN-BOOK");successful(r);assertNull(r.getRequest().getAttribute("ipad_staff_id"));
        var event=ArgumentCaptor.forClass(NotifyEvent.class);verify(notifications).publish(event.capture());assertEquals(12,event.getValue().getSenderId());
        assertEquals(403,code(submit(token,"SYN-EMPTY","SYN-BOOK")));verify(details,times(1)).saveAndFlush(any());
    }
    @Test void boundDeviceCanAuthorizeAnotherOnDutyEmployeeWithoutTrustingBody() throws Exception {
        successful(submit(authorize("SYN-BOUND","SYN12"),"SYN-BOUND","SYN-BOOK"));
        var event=ArgumentCaptor.forClass(NotifyEvent.class);verify(notifications).publish(event.capture());assertEquals(12,event.getValue().getSenderId());
    }
    @Test void wrongPasswordForeignStaffAndForeignOrderCannotIssueGrant() throws Exception {
        for(String account:List.of("SYN11","SYN21")) {
            var r=mvc.perform(req("auth/verify","SYN-EMPTY",0,1,Map.of("username",account,"password",account.equals("SYN11")?"wrong":"synthetic-test-password","booking_id","SYN-BOOK"))).andReturn();
            assertEquals(401,code(r)); assertFalse(json.readTree(r.getResponse().getContentAsByteArray()).path("data").has("authorization_token"));
        }
        var r=mvc.perform(req("auth/verify","SYN-EMPTY",0,1,Map.of("username","SYN11","password","synthetic-test-password","booking_id","FOREIGN"))).andReturn();
        assertEquals(403,code(r));verifyNoInteractions(details,notifications);
    }
    @Test void deviceOrderStoreAndTamperedTokenAreBoundWithoutWrites() throws Exception {
        String token=authorize("SYN-EMPTY","SYN11");clearInvocations(bookings);
        assertEquals(403,code(submit(token,"SYN-BOUND","SYN-BOOK")));
        assertEquals(403,code(submit(token,"SYN-EMPTY","OTHER")));
        assertEquals(403,code(submit("X"+token.substring(1,42)+"!","SYN-EMPTY","SYN-BOOK")));
        assertThrows(SecurityException.class,()->grants.consume(token,2,"SYN-EMPTY","SYN-BOOK",jdbc));
        verifyNoInteractions(bookings,details,notifications);
    }
    @Test void expiryAndDepartedEmployeeRejectWithoutWrites() throws Exception {
        String expired=authorize("SYN-EMPTY","SYN11");clock.now+=120000;
        assertEquals(403,code(submit(expired,"SYN-EMPTY","SYN-BOOK")));
        String left=authorize("SYN-EMPTY","SYN12");jdbc.update("UPDATE staff_master SET employment_status='left' WHERE staff_id=12");
        assertEquals(403,code(submit(left,"SYN-EMPTY","SYN-BOOK")));verifyNoInteractions(details,notifications);
    }
    @Test void sharedDeviceLoginActuallySwitchesBindingAfterPasswordValidation() throws Exception {
        var r=mvc.perform(req("login","SYN-BOUND",0,1,Map.of("phone","SYN12","password","synthetic-test-password"))).andReturn();
        assertEquals(200,code(r));assertEquals(12,jdbc.queryForObject("SELECT staff_id FROM ipad_device_binding WHERE device_sn='SYN-BOUND'",Integer.class));
        var bad=mvc.perform(req("login","SYN-BOUND",11,1,Map.of("phone","SYN11","password","wrong"))).andReturn();
        assertEquals(401,code(bad));assertEquals(12,jdbc.queryForObject("SELECT staff_id FROM ipad_device_binding WHERE device_sn='SYN-BOUND'",Integer.class));
    }
    @Test void guestMenuStillWorksWithoutEmployeeAndFinancialWriteDoesNot() throws Exception {
        var r=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ipad/dish/list").header("X-Client-Type","ipad").header("X-Device-Sn","SYN-EMPTY").header("X-Store-Id",1).header("X-Staff-Id",0)).andReturn();
        assertEquals(200,code(r));assertEquals(1,json.readTree(r.getResponse().getContentAsByteArray()).path("data").size());assertNull(r.getRequest().getAttribute("ipad_staff_id"));
        var denied=mvc.perform(req("order/dish/refund","SYN-EMPTY",900001,1,Map.of("dish_booking_id",81,"refund_reason","synthetic"))).andReturn();
        assertEquals(403,denied.getResponse().getStatus());verifyNoInteractions(details);
    }
    @Test void oneUseCapabilityCannotBeAcceptedAsPcJwt() throws Exception {
        String token=authorize("SYN-EMPTY","SYN11");
        var gate=new com.youjian.banquet.config.JwtAuthInterceptor();ReflectionTestUtils.setField(gate,"jwtSecret",UUID.randomUUID().toString()+UUID.randomUUID());
        var req=new org.springframework.mock.web.MockHttpServletRequest("GET","/api/finance/payables");req.addHeader("Authorization","Bearer "+token);
        var res=new org.springframework.mock.web.MockHttpServletResponse();assertFalse(gate.preHandle(req,res,new Object()));assertEquals(401,res.getStatus());
    }
    @Test void concurrentConsumersHaveOnlyOneWinner() throws Exception {
        String token=authorize("SYN-EMPTY","SYN11");var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Integer> call=()->{start.await();return code(submit(token,"SYN-EMPTY","SYN-BOOK"));};
        try { var a=pool.submit(call);var b=pool.submit(call);start.countDown();var codes=new ArrayList<>(List.of(a.get(),b.get()));Collections.sort(codes);assertEquals(List.of(200,403),codes);verify(details,times(1)).saveAndFlush(any()); }
        finally {pool.shutdownNow();}
    }
}
