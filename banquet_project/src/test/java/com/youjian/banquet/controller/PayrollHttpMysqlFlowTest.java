package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.aop.*;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.service.PayrollService;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Actual JWT interceptor + actual identity aspects + HTTP controllers + transactional MySQL service.
 * Synthetic identities/fixtures; no login endpoint, browser, bank payment or production environment. */
class PayrollHttpMysqlFlowTest {
    static final String HOST="jdbc:mysql://127.0.0.1:13317/";
    static final String OPTS="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    static final String MONTH="2026-08";
    String schema, secret;
    JdbcTemplate jdbc;
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    ObjectMapper json=new ObjectMapper();
    @Configuration @EnableTransactionManagement @EnableAspectJAutoProxy(proxyTargetClass=true)
    static class Wiring {}

    @BeforeEach void setup() {
        assertNull(UserContext.get());
        schema="payroll_http_"+UUID.randomUUID().toString().replace("-","");
        new JdbcTemplate(new DriverManagerDataSource(HOST+OPTS,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        DataSource ds=new DriverManagerDataSource(HOST+schema+OPTS,"root","");
        jdbc=new JdbcTemplate(ds);
        new ResourceDatabasePopulator(new ClassPathResource("payroll-metadata-fixture-20260907.sql"),new FileSystemResource("../scripts/migrations/payroll_approval_payout_v1.sql")).execute(ds);
        jdbc.execute("ALTER TABLE staff_master ADD can_view_all_stores INT DEFAULT 0, ADD can_manage_hr INT DEFAULT 0, ADD department VARCHAR(30), ADD basic_salary DECIMAL(10,2), ADD monthly_salary DECIMAL(10,2), ADD performance_salary DECIMAL(10,2), ADD subsidy DECIMAL(10,2), ADD bonus DECIMAL(10,2), ADD social_insurance DECIMAL(10,2), ADD housing_fund DECIMAL(10,2), ADD employment_status VARCHAR(20)");
        jdbc.execute("CREATE TABLE attendance_records(staff_id VARCHAR(30), month VARCHAR(7),total_present DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE overtime(staff_id INT,overtime_date DATE,hours DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE report_staff_kpi(staff_id INT,stat_month VARCHAR(7),reward_count INT)");
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(60),action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,can_manage_hr) VALUES (1,1,'合成审批人','synthetic_approver',1),(2,1,'合成工资员工','synthetic_worker',1),(3,2,'合成他店员工','synthetic_other',1)");
        jdbc.update("INSERT INTO attendance_records VALUES('2',?,22)",MONTH);
        jdbc.update("INSERT INTO report_staff_kpi VALUES(2,?,9)",MONTH);
        secret=UUID.randomUUID().toString()+UUID.randomUUID();
        context=new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-only",Map.of("jwt.secret",secret,"approval.approvers","synthetic_approver")));
        context.registerBean(DataSource.class,()->ds);
        context.registerBean(JdbcTemplate.class,()->jdbc);
        context.registerBean(PlatformTransactionManager.class,()->new DataSourceTransactionManager(ds));
        context.register(Wiring.class,PayrollService.class,PayrollController.class,JwtAuthInterceptor.class,StoreDataScopeAspect.class,AuditLogAspect.class);
        context.refresh();
        mvc=MockMvcBuilders.standaloneSetup(context.getBean(PayrollController.class)).addInterceptors(context.getBean(JwtAuthInterceptor.class)).build();
    }
    @AfterEach void close() {
        try { assertNull(UserContext.get(),"real aspects must clean identity after HTTP"); }
        finally { if(context!=null) context.close(); System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema); }
    }
    String token(long id,long store,String account) {
        return Jwts.builder().subject(account).claim("staffId",id).claim("storeId",store).claim("role","manager")
            .expiration(new Date(System.currentTimeMillis()+300000)).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    JsonNode postAs(String endpoint,String actor,String body,int code) throws Exception {
        var request=post("/api/hr/payroll/"+endpoint).param("month",MONTH).header("Authorization","Bearer "+actor);
        if(body!=null) request.contentType("application/json").content(body);
        var response=mvc.perform(request).andReturn().getResponse();
        assertEquals(200,response.getStatus(),"existing payroll business status is carried in Result.code");
        JsonNode result=json.readTree(response.getContentAsString());
        assertEquals(code,result.path("code").asInt(),"payroll response code");
        assertNull(UserContext.get());
        return result.path("data");
    }
    String items(int... ids) throws Exception {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(int id:ids) rows.add(Map.of("emp_id",id,"base_salary","1000","post_salary","200","attendance_pay","300","bonus","40","overtime_pay","50","allowance","5"));
        return json.writeValueAsString(rows);
    }
    void conserved() {
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM month_salary m LEFT JOIN payroll_payout_record p ON m.payout_id=p.payout_id AND m.salary_month=p.salary_month WHERE m.payout_id IS NOT NULL AND (p.payout_id IS NULL OR (p.store_id IS NOT NULL AND p.store_id<>m.store_id))",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record p WHERE p.headcount<>(SELECT COUNT(*) FROM month_salary m WHERE m.payout_id=p.payout_id) OR p.total_net<>(SELECT COALESCE(SUM(m.net_salary),0) FROM month_salary m WHERE m.payout_id=p.payout_id)",Integer.class));
    }
    @Test void saveReadApproveRecordRetryThroughJwtHttpAndMysql() throws Exception {
        String approver=token(1,1,"synthetic_approver");
        postAs("save",approver,items(2),200);
        var before=jdbc.queryForMap("SELECT gross_salary,net_salary,status FROM month_salary WHERE staff_id=2");
        assertEquals(new BigDecimal("1595.00"),before.get("net_salary"));
        var response=mvc.perform(get("/api/hr/payroll").param("month",MONTH).header("Authorization","Bearer "+approver)).andReturn().getResponse();
        assertEquals(200,response.getStatus());
        var root=json.readTree(response.getContentAsString());assertEquals(200,root.path("code").asInt());
        JsonNode row=null;
        for(JsonNode r:root.path("data")) { assertNotEquals(3,r.path("emp_id").asInt()); if(r.path("emp_id").asInt()==2) row=r; }
        assertNotNull(row); assertEquals(0,new BigDecimal("1595").compareTo(row.path("net_pay").decimalValue()));
        assertEquals(0,new BigDecimal("300").compareTo(row.path("attendance_pay").decimalValue()));
        postAs("approve",approver,null,200);
        assertEquals("synthetic_approver",jdbc.queryForObject("SELECT approved_by FROM month_salary WHERE staff_id=2",String.class));
        postAs("payout",approver,null,200); conserved();
        var batch=jdbc.queryForList("SELECT * FROM payroll_payout_record");
        var details=jdbc.queryForList("SELECT * FROM month_salary");
        assertEquals(1,batch.size());assertEquals(3,((Number)details.get(0).get("status")).intValue());
        assertTrue(postAs("payout",approver,null,200).path("alreadyRecorded").asBoolean());
        assertEquals(batch,jdbc.queryForList("SELECT * FROM payroll_payout_record"));
        assertEquals(details,jdbc.queryForList("SELECT * FROM month_salary"));conserved();
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE user_id='1'",Integer.class)>=4);
    }
    @Test void missingJwtIsRejectedBeforeAnyPayrollOrAuditWrites() throws Exception {
        for(String endpoint:List.of("save","approve","payout")) assertEquals(401,mvc.perform(post("/api/hr/payroll/"+endpoint).param("month",MONTH)).andReturn().getResponse().getStatus());
        assertEquals(401,mvc.perform(get("/api/hr/payroll").param("month",MONTH)).andReturn().getResponse().getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM month_salary",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class));conserved();
    }
    @Test void mixedStoreSaveRollsBackWholeBatchThroughHttp() throws Exception {
        postAs("save",token(1,1,"synthetic_approver"),items(2,3),400);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM month_salary",Integer.class));conserved();
    }
    @Test void signedNonApproverCannotApprove() throws Exception {
        postAs("save",token(1,1,"synthetic_approver"),items(2),200);
        // Existing controller maps PayrollRejectedException to Result.code=400 (HTTP200).
        postAs("approve",token(2,1,"synthetic_worker"),null,400);
        assertEquals(1,jdbc.queryForObject("SELECT status FROM month_salary WHERE staff_id=2",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payroll_payout_record",Integer.class));conserved();
    }
}
