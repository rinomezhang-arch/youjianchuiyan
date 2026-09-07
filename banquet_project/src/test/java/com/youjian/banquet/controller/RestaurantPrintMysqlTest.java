package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.aop.*;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.service.RestaurantPrintService;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Isolated persistence, actual JWT/aspects/MVC/transactions. No device adapter or production access. */
class RestaurantPrintMysqlTest {
    String schema,secret;
    DataSource ds;
    JdbcTemplate jdbc;
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    ObjectMapper json=new ObjectMapper();
    @Configuration @EnableTransactionManagement @EnableAspectJAutoProxy(proxyTargetClass=true)
    static class Wiring {}
    @BeforeEach void setup() throws Exception {
        assertNull(UserContext.get());
        String host="jdbc:mysql://127.0.0.1:13317/",opts="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        schema="print_http_"+UUID.randomUUID().toString().replace("-","");
        new JdbcTemplate(new DriverManagerDataSource(host+opts,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        ds=new DriverManagerDataSource(host+schema+opts,"root","");jdbc=new JdbcTemplate(ds);
        String captured;
        try(var input=new ClassPathResource("restaurant-production-schema-20260906.sql").getInputStream()) {
            captured=new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
        int start=captured.indexOf("CREATE TABLE `store_info` (");
        assertTrue(start>=0,"captured real store DDL required");
        // Original captured restaurant fixture's external staff key target;
        // retain the real store.manager_id FK, with no staff records copied.
        int staffStart=captured.indexOf("CREATE TABLE staff_master (");
        assertTrue(staffStart>=0,"original fixture external staff key target required");
        jdbc.execute(captured.substring(staffStart,captured.indexOf(';',staffStart)));
        int end=captured.indexOf(';',start);assertTrue(end>start);
        jdbc.execute(captured.substring(start,end));
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-PRINT-1','合成门店一'),(2,'SYN-PRINT-2','合成门店二')");
        var migration=new ResourceDatabasePopulator(new FileSystemResource("../scripts/migrations/restaurant_print_config_v1.sql"));
        migration.execute(ds);migration.execute(ds);
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(60),action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        secret=UUID.randomUUID().toString()+UUID.randomUUID();startContext();
    }
    void startContext() {
        context=new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-only",Map.of("jwt.secret",secret)));
        context.registerBean(JdbcTemplate.class,()->jdbc);context.registerBean(PlatformTransactionManager.class,()->new DataSourceTransactionManager(ds));
        context.register(Wiring.class,RestaurantPrintService.class,RestaurantPrintController.class,JwtAuthInterceptor.class,StoreDataScopeAspect.class,AuditLogAspect.class);
        context.refresh();mvc=MockMvcBuilders.standaloneSetup(context.getBean(RestaurantPrintController.class)).addInterceptors(context.getBean(JwtAuthInterceptor.class)).build();
    }
    @AfterEach void close(){try{assertNull(UserContext.get());}finally{if(context!=null)context.close();System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema);}}
    String token(Long store,String role) {
        var b=Jwts.builder().subject("synthetic_print_user").claim("staffId",9L).claim("role",role).expiration(new Date(System.currentTimeMillis()+300000));
        if(store!=null)b.claim("storeId",store);
        return b.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    JsonNode post(String route,Map<String,Object> body,String token,int status)throws Exception {
        var req=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/restaurant-print/"+route).contentType("application/json").content(json.writeValueAsString(body));
        if(token!=null)req.header("Authorization","Bearer "+token);
        var response=mvc.perform(req).andReturn().getResponse();assertNull(UserContext.get());assertEquals(status,response.getStatus());
        var result=json.readTree(response.getContentAsByteArray());assertEquals(status,result.path("code").asInt());return result.path("data");
    }
    JsonNode get(String route,String store,String token,int status)throws Exception {
        var req=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/restaurant-print/"+route);
        if(store!=null)req.param("storeId",store);if(token!=null)req.header("Authorization","Bearer "+token);
        var response=mvc.perform(req).andReturn().getResponse();assertNull(UserContext.get());assertEquals(status,response.getStatus());return json.readTree(response.getContentAsByteArray()).path("data");
    }
    Map<String,Object> printer(){return new LinkedHashMap<>(Map.of("name","合成前台","type","network","paperWidth","80","copies",1));}
    long create(String actor)throws Exception{return post("printers",printer(),actor,200).path("id").asLong();}
    Map<String,Object> rule(long printer){return new LinkedHashMap<>(Map.of("name","合成小票","printerId",printer,"documentType","receipt","configuredEnabled",true));}
    @Test void savedPrinterAndRuleSurviveServiceRestartAndRepeatedUpdates()throws Exception {
        String actor=token(1L,"manager");long id=create(actor);
        var r=post("rules",rule(id),actor,200);assertFalse(r.path("effectiveEnabled").asBoolean());assertTrue(r.path("configuredEnabled").asBoolean());assertEquals("device adapter not connected",r.path("inactiveReason").asText());
        for(int i=0;i<2;i++)post("printers",Map.of("id",id,"copies",2),actor,200);
        context.close();startContext();
        var read=get("printers",null,actor,200);assertEquals(1,read.size());assertEquals(2,read.get(0).path("copies").asInt());assertEquals("unverified",read.get(0).path("connectionStatus").asText());
        assertEquals(id,get("rules",null,actor,200).get(0).path("printerId").asLong());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer",Integer.class));
    }
    @Test void archiveKeepsRowsAndRequiresRuleArchiveFirst()throws Exception {
        String actor=token(1L,"manager");long id=create(actor);long rid=post("rules",rule(id),actor,200).path("id").asLong();
        post("printers",Map.of("id",id,"archive",true),actor,409);
        post("rules",Map.of("id",rid,"archive",true),actor,200);post("printers",Map.of("id",id,"archive",true),actor,200);
        assertTrue(get("printers",null,actor,200).get(0).path("archive").asBoolean());assertTrue(get("rules",null,actor,200).get(0).path("archive").asBoolean());
        post("rules",rule(id),actor,409);post("printers",Map.of("id",id,"archive",false),actor,409);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule",Integer.class));
    }
    @Test void realCompositeForeignKeyAndHttpScopeRejectForeignStore()throws Exception {
        long id=create(token(1L,"manager"));String other=token(2L,"manager");
        post("rules",rule(id),other,403);post("printers",Map.of("id",id,"name","越店"),other,403);get("printers","1",other,403);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO restaurant_print_rule(store_id,name,printer_id,document_type) VALUES(2,'fixture',?,'receipt')",id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule r LEFT JOIN restaurant_print_printer p ON p.id=r.printer_id AND p.store_id=r.store_id WHERE p.id IS NULL",Integer.class));
    }
    @Test void absentIdentityInvalidFieldsAndGmScopeFailClosed()throws Exception {
        post("printers",printer(),null,401);get("printers",null,token(null,"manager"),403);get("printers",null,token(1L,"gm"),400);
        get("printers","0",token(1L,"gm"),400);get("printers","all",token(1L,"gm"),400);get("printers","2",token(1L,"gm"),200);
        for(var entry:Map.of("type","cloud","paperWidth","79","copies",6,"name","","online",true).entrySet()){var b=printer();b.put(entry.getKey(),entry.getValue());post("printers",b,token(1L,"manager"),400);}
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer",Integer.class));
    }
    @Test void databaseFailureRollsBackUpdateAndPreservesConfiguration()throws Exception {
        String actor=token(1L,"manager");long id=create(actor);
        jdbc.execute("CREATE TRIGGER synthetic_print_fail AFTER UPDATE ON restaurant_print_printer FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic rollback probe'");
        post("printers",Map.of("id",id,"name","失败修改"),actor,500);
        assertEquals("合成前台",get("printers",null,actor,200).get(0).path("name").asText());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer",Integer.class));
    }
    @Test void invalidRuleFieldsAreRejectedAndUpdatesDoNotDuplicate()throws Exception {
        String actor=token(1L,"manager");long id=create(actor);
        for(var e:Map.of("documentType","arbitrary","configuredEnabled","true","effectiveEnabled",true,"printerId",0).entrySet()){
            var b=rule(id);b.put(e.getKey(),e.getValue());post("rules",b,actor,400);
        }
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule",Integer.class));
        long rid=post("rules",rule(id),actor,200).path("id").asLong();
        for(int i=0;i<2;i++)post("rules",Map.of("id",rid,"configuredEnabled",false),actor,200);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule",Integer.class));
        assertFalse(get("rules",null,actor,200).get(0).path("configuredEnabled").asBoolean());
    }
    @Test void gmNonexistentStoreAndDirectDatabaseOrphansAreRejected()throws Exception {
        String gm=token(1L,"gm");var b=printer();b.put("storeId",999999);
        post("printers",b,gm,400);get("printers","999999",gm,400);get("rules","999999",gm,400);
        get("printers",null,token(999999L,"manager"),400);
        var invalidRule=rule(1);invalidRule.put("storeId",999999);post("rules",invalidRule,gm,400);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO restaurant_print_printer(store_id,name,type,paper_width,copies) VALUES(999999,'synthetic','browser','A4',1)"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_printer p LEFT JOIN store_info s ON s.store_id=p.store_id WHERE s.store_id IS NULL",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME='store_info' AND TABLE_NAME IN ('restaurant_print_printer','restaurant_print_rule')",Integer.class));
    }
}
