package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.*;
import com.youjian.banquet.aop.*;
import com.youjian.banquet.config.JwtAuthInterceptor;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.FinancePayableService;
import com.youjian.banquet.util.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** JWT + actual identity aspects + MVC + JPA/MySQL settlement chain; fixture creation is not HTTP. */
class PayableHttpMysqlFlowTest {
    String schema,secret;
    AnnotationConfigApplicationContext context;
    EntityManagerFactory emf;
    JdbcTemplate jdbc;
    FinancePayableRepository repo;
    TransactionTemplate tx;
    MockMvc mvc;
    ObjectMapper json=new ObjectMapper();
    @Configuration @EnableTransactionManagement @EnableAspectJAutoProxy(proxyTargetClass=true)
    static class Wiring {}
    @BeforeEach void setup() {
        assertNull(UserContext.get());
        String host="jdbc:mysql://127.0.0.1:13317/",opts="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        schema="payable_http_"+UUID.randomUUID().toString().replace("-","");
        new JdbcTemplate(new DriverManagerDataSource(host+opts,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        DataSource ds=new DriverManagerDataSource(host+schema+opts,"root","");jdbc=new JdbcTemplate(ds);
        var bean=new LocalContainerEntityManagerFactoryBean();bean.setDataSource(ds);
        var adapter=new HibernateJpaVendorAdapter();adapter.setGenerateDdl(true);bean.setJpaVendorAdapter(adapter);
        Properties props=new Properties();props.setProperty("hibernate.hbm2ddl.auto","update");bean.setJpaProperties(props);
        bean.setPackagesToScan("com.youjian.banquet.entity");
        bean.setPersistenceUnitPostProcessors(pui->{pui.getManagedClassNames().clear();pui.setExcludeUnlistedClasses(true);pui.addManagedClassName(FinancePayable.class.getName());pui.addManagedClassName(PayableSettlementRecord.class.getName());pui.addManagedClassName(PayableCreateRequest.class.getName());});
        bean.afterPropertiesSet();emf=Objects.requireNonNull(bean.getObject());
        var factory=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
        repo=factory.getRepository(FinancePayableRepository.class);
        var records=factory.getRepository(PayableSettlementRecordRepository.class);
        // 创建幂等登记：服务已依赖它，测试上下文不注册就装配不起来
        var createRequests=factory.getRepository(PayableCreateRequestRepository.class);
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(60),action VARCHAR(200),target VARCHAR(200),detail TEXT,store_id BIGINT)");
        secret=UUID.randomUUID().toString()+UUID.randomUUID();
        context=new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-only",Map.of("jwt.secret",secret)));
        context.registerBean(JdbcTemplate.class,()->jdbc);
        context.registerBean(PlatformTransactionManager.class,()->new JpaTransactionManager(emf));
        context.registerBean(FinancePayableRepository.class,()->repo);
        context.registerBean(PayableSettlementRecordRepository.class,()->records);
        context.registerBean(PayableCreateRequestRepository.class,()->createRequests);
        context.register(Wiring.class,FinancePayableService.class,FinancePayableController.class,JwtAuthInterceptor.class,StoreDataScopeAspect.class,AuditLogAspect.class);
        context.refresh();tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        mvc=MockMvcBuilders.standaloneSetup(context.getBean(FinancePayableController.class)).addInterceptors(context.getBean(JwtAuthInterceptor.class)).build();
    }
    @AfterEach void close() {
        try {assertNull(UserContext.get());} finally {if(context!=null)context.close();if(emf!=null)emf.close();System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema);}
    }
    String token(long store) {
        return Jwts.builder().subject("synthetic_manager").claim("staffId",9L).claim("storeId",store).claim("role","store_manager")
            .expiration(new Date(System.currentTimeMillis()+300000)).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    Long original(long store) {
        var p=new FinancePayable();p.setStoreId(store);p.setPayableNo("FIX-"+UUID.randomUUID());p.setSupplierName("合成供应商");
        p.setTotalAmount(new BigDecimal("1000"));p.setPaidAmount(BigDecimal.ZERO);p.setPendingAmount(new BigDecimal("1000"));p.setStatus("unpaid");p.setPayableDate(LocalDate.now());
        return tx.execute(s->repo.saveAndFlush(p).getPayableId());
    }
    record Reply(int status,JsonNode body) {}
    Reply settle(long id,String amount,String key,String bearer) throws Exception {
        var req=post("/api/finance/payables").contentType("application/json").content(json.writeValueAsString(Map.of("payableId",id,"settleAmount",amount,"requestId",key)));
        if(bearer!=null)req.header("Authorization","Bearer "+bearer);
        var response=mvc.perform(req).andReturn().getResponse();assertNull(UserContext.get());
        return new Reply(response.getStatus(),json.readTree(response.getContentAsString()));
    }
    Reply history(long id,String bearer) throws Exception {
        var req=get("/api/finance/payables/"+id+"/settlements");if(bearer!=null)req.header("Authorization","Bearer "+bearer);
        var response=mvc.perform(req).andReturn().getResponse();assertNull(UserContext.get());return new Reply(response.getStatus(),json.readTree(response.getContentAsString()));
    }
    void conserved(long id,String paid,int count) {
        var p=jdbc.queryForMap("SELECT * FROM finance_payable WHERE payable_id=?",id);
        BigDecimal actual=(BigDecimal)p.get("paid_amount");assertEquals(0,new BigDecimal(paid).compareTo(actual));
        assertEquals(0,((BigDecimal)p.get("total_amount")).compareTo(actual.add((BigDecimal)p.get("pending_amount"))));
        assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM payable_settlement_record WHERE payable_id=?",Integer.class,id));
        assertEquals(0,actual.compareTo(jdbc.queryForObject("SELECT COALESCE(SUM(settle_amount),0) FROM payable_settlement_record WHERE payable_id=?",BigDecimal.class,id)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM payable_settlement_record r LEFT JOIN finance_payable p ON p.payable_id=r.payable_id AND p.store_id=r.store_id WHERE p.payable_id IS NULL",Integer.class));
    }
    @Test void settlementHistoryReplayAndFullPaymentRemainConserved() throws Exception {
        long id=original(1);String actor=token(1);
        Reply first=settle(id,"300","synthetic-first",actor);assertEquals(200,first.status);assertFalse(first.body.path("data").path("replayed").asBoolean());
        var get=history(id,actor);assertEquals(200,get.status);assertEquals(1,get.body.path("data").size());
        assertEquals(first.body.path("data").path("settlementNo").asText(),get.body.path("data").get(0).path("settlementNo").asText());
        assertEquals(1,get.body.path("data").get(0).path("storeId").asInt());
        var retry=settle(id,"300","synthetic-first",actor);assertEquals(200,retry.status);assertTrue(retry.body.path("data").path("replayed").asBoolean());conserved(id,"300",1);
        assertEquals(409,settle(id,"301","synthetic-first",actor).status);conserved(id,"300",1);
        assertEquals(200,settle(id,"700","synthetic-final",actor).status);conserved(id,"1000",2);
        assertEquals("paid",jdbc.queryForObject("SELECT status FROM finance_payable WHERE payable_id=?",String.class,id));
        assertEquals("synthetic_manager",jdbc.queryForObject("SELECT operator_name FROM payable_settlement_record WHERE request_id='synthetic-first'",String.class));
    }
    @Test void foreignStoreAndMissingJwtCannotWriteOrReadLedger() throws Exception {
        long id=original(2);
        assertEquals(401,settle(id,"100","no-login",null).status);assertEquals(401,history(id,null).status);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class));
        assertEquals(403,settle(id,"100","wrong-store",token(1)).status);assertEquals(403,history(id,token(1)).status);conserved(id,"0",0);
    }
    @Test void concurrentSameKeyThroughMvcProducesSingleLedgerAndReplayOr409() throws Exception {
        long id=original(1);String actor=token(1);var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            List<Future<Reply>> futures=new ArrayList<>();
            for(int i=0;i<2;i++)futures.add(pool.submit(()->{assertNull(UserContext.get());ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return settle(id,"400","concurrent-key",actor);}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            List<Reply> responses=new ArrayList<>();for(var f:futures)responses.add(f.get(30,TimeUnit.SECONDS));
            assertEquals(1,responses.stream().filter(r->r.status==200&&!r.body.path("data").path("replayed").asBoolean()).count());
            for(var r:responses)assertTrue(r.status==200||r.status==409,"unexpected HTTP status");
            System.out.println("CONCURRENT_HTTP_STATUSES="+responses.stream().map(r->r.status+":"+r.body.path("data").path("replayed").asBoolean()).toList());
            var retry=settle(id,"400","concurrent-key",actor);assertEquals(200,retry.status);assertTrue(retry.body.path("data").path("replayed").asBoolean());conserved(id,"400",1);
        } finally {start.countDown();pool.shutdown();assertTrue(pool.awaitTermination(35,TimeUnit.SECONDS));}
    }

    /** 走真实 JWT 与拦截器发起手工创建；参数与结算分支的区别只在 body 是否含 payableId。 */
    Reply create(String amount,String key,String bearer,String supplier) throws Exception {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("supplierName",supplier);
        body.put("totalAmount",amount);
        body.put("storeId",1);
        if(key!=null)body.put("requestId",key);
        var req=post("/api/finance/payables").contentType("application/json").content(json.writeValueAsString(body));
        if(bearer!=null)req.header("Authorization","Bearer "+bearer);
        var response=mvc.perform(req).andReturn().getResponse();assertNull(UserContext.get());
        return new Reply(response.getStatus(),json.readTree(response.getContentAsString()));
    }

    @Test void manualCreateThroughHttpIsIdempotentAndScoped() throws Exception {
        String actor=token(1);

        // 未登录与缺幂等键：都进不到建单
        assertEquals(401,create("100","no-login",null,"合成供应商").status);
        assertEquals(400,create("100",null,actor,"合成供应商").status);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable",Integer.class));

        // 正常创建：回执字段齐全，前端可逐项核对
        Reply first=create("123.45","synthetic-create",actor,"合成供应商");
        assertEquals(200,first.status);
        assertFalse(first.body.path("data").path("replayed").asBoolean());
        long id=first.body.path("data").path("payableId").asLong();
        assertEquals("synthetic-create",first.body.path("data").path("requestId").asText());
        assertFalse(first.body.path("data").path("payableNo").asText().isBlank());

        // 同键同参数重发：拿回原单，不新建
        Reply replay=create("123.45","synthetic-create",actor,"合成供应商");
        assertEquals(200,replay.status);
        assertTrue(replay.body.path("data").path("replayed").asBoolean());
        assertEquals(id,replay.body.path("data").path("payableId").asLong());

        // 同键改金额：409，且原单分文未动
        assertEquals(409,create("999.99","synthetic-create",actor,"合成供应商").status);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable",Integer.class));
        assertEquals(0,new BigDecimal("123.45").compareTo(
                jdbc.queryForObject("SELECT total_amount FROM finance_payable WHERE payable_id=?",BigDecimal.class,id)));
        assertEquals(0,new BigDecimal("0.00").compareTo(
                jdbc.queryForObject("SELECT paid_amount FROM finance_payable WHERE payable_id=?",BigDecimal.class,id)));

        // 登记与单据一一对应，且同门店
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM payable_create_request",Integer.class));
        assertEquals(0,jdbc.queryForObject(
                "SELECT COUNT(*) FROM payable_create_request r LEFT JOIN finance_payable p "
                +"ON p.payable_id=r.payable_id AND p.store_id=r.store_id WHERE p.payable_id IS NULL",Integer.class));

        // 2 号店的身份显式提交 1 号店：明确 403，而不是静默改写成 2 号店建出来
        assertEquals(403,create("50","cross-store",token(2),"合成供应商").status);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable",Integer.class));
    }
}
