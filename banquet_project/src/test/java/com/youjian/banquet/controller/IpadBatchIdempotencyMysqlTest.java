package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Only runs against the dedicated loopback instance; every run creates and preserves a new schema. */
@EnabledIfEnvironmentVariable(named = "YOUJIAN_TEST_MYSQL", matches = "1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IpadBatchIdempotencyMysqlTest {
    JdbcTemplate jdbc;
    BookingController controller;
    IpadOrderController orderTarget;
    com.youjian.banquet.service.IpadBatchAuthorizationService grants;
    com.youjian.banquet.service.NotifyPublisher notifications = mock(com.youjian.banquet.service.NotifyPublisher.class);
    final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper();
    IpadOrderController orders;
    LocalContainerEntityManagerFactoryBean factory;
    JpaTransactionManager transactionManager;
    int nextTable = 100;
    LocalDate date = LocalDate.now().plusDays(2);

    @BeforeAll void start() {
        String schema = "ipad_batch_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        DriverManagerDataSource admin = new DriverManagerDataSource(url + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        new JdbcTemplate(admin).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("BATCH_EVIDENCE schema=" + schema + " retained=true source=JPA-explicit-restaurant-entities");
        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        jdbc = new JdbcTemplate(ds);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(BookingMaster.class.getName(), BookingTable.class.getName(),
            BookingDishDetail.class.getName(), CustomerMaster.class.getName(), TableMaster.class.getName(),
            DishMaster.class.getName(), KitchenLog.class.getName(),
            StoreInfo.class.getName(), StaffMaster.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,status) VALUES (1,'TEST-1','合成验收门店一','active'),(2,'TEST-2','合成验收门店二','active')");
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,employment_status) VALUES (1,1,'合成验收员工','test-operator','在职')");
        EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        JpaTransactionManager manager = new JpaTransactionManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(em);
        // Match Spring Data repository transaction boundaries, including unannotated controller callers.
        repositories.addRepositoryProxyPostProcessor((proxyFactory, repositoryInformation) ->
            proxyFactory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource())));
        BookingController target = new BookingController();
        ReflectionTestUtils.setField(target, "jdbc", jdbc);
        ReflectionTestUtils.setField(target, "bookingMasterRepo", repositories.getRepository(BookingMasterRepository.class));
        ReflectionTestUtils.setField(target, "bookingTableRepo", repositories.getRepository(BookingTableRepository.class));
        ReflectionTestUtils.setField(target, "bookingDishDetailRepo", repositories.getRepository(BookingDishDetailRepository.class));
        transactionManager = manager;
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        controller = (BookingController) proxy.getProxy();
        orderTarget = new IpadOrderController();
        ReflectionTestUtils.setField(orderTarget, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(orderTarget, "entityManager", em);
        ReflectionTestUtils.setField(orderTarget, "bookingRepo", repositories.getRepository(BookingMasterRepository.class));
        ReflectionTestUtils.setField(orderTarget, "bookingTableRepo", repositories.getRepository(BookingTableRepository.class));
        ReflectionTestUtils.setField(orderTarget, "dishDetailRepo", repositories.getRepository(BookingDishDetailRepository.class));
        ReflectionTestUtils.setField(orderTarget, "dishRepo", repositories.getRepository(DishMasterRepository.class));
        ReflectionTestUtils.setField(orderTarget, "kitchenLogRepo", repositories.getRepository(KitchenLogRepository.class));
        ReflectionTestUtils.setField(orderTarget, "notifyPublisher", notifications);
        ProxyFactory orderProxy = new ProxyFactory(orderTarget);
        orderProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        orders = (IpadOrderController) orderProxy.getProxy();
        try { jdbc.execute(new String(new org.springframework.core.io.ClassPathResource("ipad_batch_request_migration_v1.sql").getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)); }
        catch(Exception e) { throw new RuntimeException(e); }
        String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("synthetic-batch-password");
        jdbc.update("UPDATE staff_master SET staff_password=? WHERE staff_id=1",hash);
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,staff_password,employment_status) VALUES(2,2,'合成二','test-operator-2',?,'在职')",hash);
    }
    @AfterAll void stop() { if(factory!=null)factory.destroy(); }
    @BeforeEach void identity() {
        UserContext.set(new UserContext.CurrentUser(1L,1L,"store_manager","synthetic"));
        grants=new com.youjian.banquet.service.IpadBatchAuthorizationService();
        ReflectionTestUtils.setField(orderTarget,"batchAuthorization",grants);
        org.mockito.Mockito.clearInvocations(notifications);
    }
    @AfterEach void evidence(TestInfo info) {
        try {
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request r LEFT JOIN booking_master b ON b.id=r.booking_master_id AND b.store_id=r.store_id AND b.booking_id=r.booking_id WHERE b.id IS NULL",Integer.class));
            System.out.println("BATCH_EVIDENCE method="+info.getTestMethod().orElseThrow().getName()+" receipts="+jdbc.queryForList("SELECT request_id,store_id,booking_master_id,booking_id,client_request_id,operator_id,result_json FROM ipad_batch_request ORDER BY request_id")+" details="+jdbc.queryForList("SELECT dish_booking_id,store_id,booking_id,dish_id,dish_quantity,unit_price,subtotal,kitchen_status FROM booking_dish_detail ORDER BY dish_booking_id"));
        } finally {UserContext.clear();}
    }
    record Fixture(String booking,String first,String second,long store) {}
    Fixture fixture(long store) {
        UserContext.set(new UserContext.CurrentUser(store,store,"store_manager","synthetic"));
        int t=nextTable++;
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_name,table_number,table_status,is_active,sort_order) VALUES(?,?,?,?,'idle',1,1)",t,store,"合成批次桌"+t,"B"+t);
        var r=controller.create(new HashMap<>(Map.of("customerName","合成批次客","customerPhone","1380000"+String.format("%04d",t),"bookingDate",date.toString(),"bookingTime","18:00","tableIds",List.of(t)))).getBody();
        assertEquals(200,r.getCode(),r.getMessage());
        String a="BATCH-A-"+t,b="BATCH-B-"+t;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,?,?,12.34,1),(?,?,?,26.40,1)",a,store,"合成菜A",b,store,"合成菜B");
        return new Fixture(r.getData().getBookingId(),a,b,store);
    }
    MockHttpServletRequest device(long store) { var r=new MockHttpServletRequest();r.setAttribute("ipad_store_id",store);r.setAttribute("ipad_device_sn","SYN-DEVICE-"+store);return r; }
    String authorize(Fixture f) {
        var r=orders.authVerify(Map.of("username",f.store()==1?"test-operator":"test-operator-2","password","synthetic-batch-password","booking_id",f.booking()),device(f.store()));
        assertEquals(200,r.getCode(),r.getMessage());return r.getData().get("authorization_token").toString();
    }
    String key() { return UUID.randomUUID().toString(); }
    Map<String,Object> body(Fixture f,String key,String token) { return new HashMap<>(Map.of("booking_id",f.booking(),"client_request_id",key,"authorization_token",token,"dishes",List.of(Map.of("dish_id",f.first(),"dish_quantity",2,"sale_price",0),Map.of("dish_id",f.second(),"dish_quantity",1)))); }
    Result<Map<String,Object>> submit(Fixture f,String key) {return orders.addDishesBatch(body(f,key,authorize(f)),device(f.store()));}
    Map<String,Object> success(Result<Map<String,Object>> r) {assertEquals(200,r.getCode(),r.getMessage());assertEquals("committed",r.getData().get("status"));return r.getData();}
    int rows(Fixture f) {return jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE store_id=? AND booking_id=?",Integer.class,f.store(),f.booking());}
    int receipts(Fixture f) {return jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request WHERE store_id=? AND booking_id=?",Integer.class,f.store(),f.booking());}
    void assertChain(Fixture f,Map<String,Object> data) {
        assertEquals(f.booking(),data.get("booking_id"));assertEquals(2,((Number)data.get("added_dishes")).intValue());assertEquals(3,((Number)data.get("added_quantity")).intValue());
        assertEquals(0,new java.math.BigDecimal("51.08").compareTo(new java.math.BigDecimal(data.get("added_amount").toString())));
        List<?> ids=(List<?>)data.get("dish_booking_ids");assertEquals(2,ids.size());
        for(Object id:ids)assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE dish_booking_id=? AND store_id=? AND booking_id=? AND kitchen_status='pending'",Integer.class,id,f.store(),f.booking()));
        assertEquals(0,new java.math.BigDecimal("51.08").compareTo(jdbc.queryForObject("SELECT SUM(subtotal) FROM booking_dish_detail WHERE store_id=? AND booking_id=?",java.math.BigDecimal.class,f.store(),f.booking())));
        assertEquals(1,receipts(f));assertEquals(2,rows(f));
    }
    @Test void lostResponseFreshAuthorizationReplaysOriginalRowsAndAmountThroughPost() throws Exception {
        var f=fixture(1);String k=key();
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(orders).build();
        var original=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ipad/order/add-dishes").requestAttr("ipad_store_id",1L).requestAttr("ipad_device_sn","SYN-DEVICE-1").contentType("application/json").content(json.writeValueAsBytes(body(f,k,authorize(f))))).andReturn();
        var first=json.readTree(original.getResponse().getContentAsByteArray());assertEquals(200,first.path("code").asInt());
        jdbc.update("UPDATE dish_master SET sale_price=999,is_active=0 WHERE dish_id IN (?,?)",f.first(),f.second());
        var replay=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ipad/order/add-dishes").requestAttr("ipad_store_id",1L).requestAttr("ipad_device_sn","SYN-DEVICE-1").contentType("application/json").content(json.writeValueAsBytes(body(f,k,authorize(f))))).andReturn();
        var second=json.readTree(replay.getResponse().getContentAsByteArray());assertEquals(200,second.path("code").asInt());assertEquals(first.path("data"),second.path("data"));
        assertChain(f,json.convertValue(second.path("data"),Map.class));org.mockito.Mockito.verify(notifications,org.mockito.Mockito.times(1)).publish(org.mockito.ArgumentMatchers.any());
    }
    @Test void canonicalOrderWhitespaceDuplicateRowsAndUntrustedPriceReplay() {
        var f=fixture(1);String k=key();var first=success(submit(f,k));
        var b=body(f,k,authorize(f));b.put("dishes",List.of(Map.of("dish_id",f.second(),"dish_quantity",1),Map.of("dish_id"," "+f.first()+" ","dish_quantity","1.0","price",999),Map.of("dish_id",f.first(),"dish_quantity",1)));
        assertEquals(first,success(orders.addDishesBatch(b,device(1))));assertChain(f,first);
    }
    @Test void sameKeyChangedQuantityReturnsConflictWithoutWrites() {
        var f=fixture(1);String k=key();var first=success(submit(f,k));var b=body(f,k,authorize(f));b.put("dishes",List.of(Map.of("dish_id",f.first(),"dish_quantity",3)));
        var r=orders.addDishesBatch(b,device(1));assertEquals(409,r.getCode());assertEquals("request_conflict",r.getData().get("error_code"));assertEquals(k,r.getData().get("client_request_id"));assertChain(f,first);
    }
    @Test void concurrentFreshGrantsAfterStaffSnapshotCommitOneReceipt() throws Exception {
        var f=fixture(1);String k=key();var accepted=new CountDownLatch(2);
        grants=new com.youjian.banquet.service.IpadBatchAuthorizationService(){@Override public synchronized Integer consume(String t,long s,String d,String b,JdbcTemplate j){Integer staff=super.consume(t,s,d,b,j);accepted.countDown();return staff;}};
        ReflectionTestUtils.setField(orderTarget,"batchAuthorization",grants);
        String a=authorize(f),b=authorize(f);var pool=Executors.newFixedThreadPool(2);
        try {
            var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            List<Future<Result<Map<String,Object>>>> calls=tx.execute(status->{jdbc.queryForList("SELECT id FROM booking_master WHERE booking_id=? FOR UPDATE",f.booking());
                var x=pool.submit(()->orders.addDishesBatch(body(f,k,a),device(1)));var y=pool.submit(()->orders.addDishesBatch(body(f,k,b),device(1)));
                try{assertTrue(accepted.await(15,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return List.of(x,y);});
            var first=success(calls.get(0).get(20,TimeUnit.SECONDS));assertEquals(first,success(calls.get(1).get(20,TimeUnit.SECONDS)));assertChain(f,first);
            org.mockito.Mockito.verify(notifications,org.mockito.Mockito.times(1)).publish(org.mockito.ArgumentMatchers.any());
        } finally {pool.shutdown();assertTrue(pool.awaitTermination(30,TimeUnit.SECONDS));}
    }
    @Test void receiptFailureRollsBackAllJpaRowsAndSameKeyCanRetry() {
        var f=fixture(1);String k=key();
        jdbc.execute("CREATE TABLE failure_gate (enabled INT NOT NULL)");jdbc.update("INSERT INTO failure_gate VALUES(1)");
        jdbc.execute("CREATE TRIGGER reject_batch BEFORE INSERT ON ipad_batch_request FOR EACH ROW BEGIN IF NEW.client_request_id='"+k+"' AND (SELECT enabled FROM failure_gate)=1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic receipt failure'; END IF; END");
        var r=submit(f,k);assertEquals(500,r.getCode());assertEquals(0,rows(f));assertEquals(0,receipts(f));org.mockito.Mockito.verifyNoInteractions(notifications);
        jdbc.update("UPDATE failure_gate SET enabled=0");assertChain(f,success(submit(f,k)));
    }
    @Test void secondDishUnavailableRollsBackFirstFlush() {
        var f=fixture(1);jdbc.update("UPDATE dish_master SET is_active=0 WHERE dish_id=?",f.second());var r=submit(f,key());
        assertEquals(409,r.getCode());assertEquals("dish_unavailable",r.getData().get("error_code"));assertEquals(0,rows(f));assertEquals(0,receipts(f));org.mockito.Mockito.verifyNoInteractions(notifications);
    }
    @Test void missingIdInvalidQuantityAndForgedStaffDoNotConsumeGrant() {
        var f=fixture(1);String k=key(),token=authorize(f);var b=body(f,k,token);b.remove("client_request_id");assertEquals(400,orders.addDishesBatch(b,device(1)).getCode());
        b=body(f,k,token);b.put("staff_id",1);assertEquals(400,orders.addDishesBatch(b,device(1)).getCode());
        b=body(f,k,token);b.put("dishes",List.of(Map.of("dish_id",f.first(),"dish_quantity",1.5)));assertEquals(400,orders.addDishesBatch(b,device(1)).getCode());
        assertChain(f,success(orders.addDishesBatch(body(f,k,token),device(1))));assertEquals(403,orders.addDishesBatch(body(f,k,token),device(1)).getCode());
    }
    @Test void crossDeviceStoreAndBookingCannotReplayButIndependentStoresCanUseSameKey() {
        var a=fixture(1);var b=fixture(2);String k=key(),token=authorize(a);var req=device(1);req.setAttribute("ipad_device_sn","SYN-OTHER");
        assertEquals(403,orders.addDishesBatch(body(a,k,token),req).getCode());assertEquals(403,orders.addDishesBatch(body(a,k,token),device(2)).getCode());assertEquals(403,orders.addDishesBatch(body(b,k,token),device(1)).getCode());
        assertChain(a,success(orders.addDishesBatch(body(a,k,token),device(1))));assertChain(b,success(submit(b,k)));
    }
    @Test void closedStatesRejectNewKeysWhileValidPriorGrantReplaysCommittedReceipt() {
        for(String state:List.of("cancelled","completed","paid")) {
            var f=fixture(1);String k=key();var first=success(submit(f,k));String replay=authorize(f),fresh=authorize(f);
            if(state.equals("cancelled"))assertEquals(200,((Result<?>)controller.delete(f.booking(),1L).getBody()).getCode());
            else if(state.equals("paid"))jdbc.update("UPDATE booking_master SET payment_status='paid' WHERE booking_id=?",f.booking());
            else jdbc.update("UPDATE booking_master SET booking_status='completed' WHERE booking_id=?",f.booking());
            var r=orders.addDishesBatch(body(f,key(),fresh),device(1));assertEquals(409,r.getCode());assertEquals("booking_closed",r.getData().get("error_code"));assertEquals(first,success(orders.addDishesBatch(body(f,k,replay),device(1))));assertEquals(2,rows(f));assertEquals(1,receipts(f));
        }
    }
    @Test void authorizationServiceRestartDoesNotLosePersistedReceipt() {
        var f=fixture(1);String k=key();var first=success(submit(f,k));String old=authorize(f);
        grants=new com.youjian.banquet.service.IpadBatchAuthorizationService();ReflectionTestUtils.setField(orderTarget,"batchAuthorization",grants);
        assertEquals(403,orders.addDishesBatch(body(f,k,old),device(1)).getCode());assertEquals(first,success(submit(f,k)));assertChain(f,first);
    }
    @Test void singleColumnForeignKeyRejectsMissingMasterButDoesNotEnforceStoreOrBookingCopies() {
        var f=fixture(1);String originalKey=key();success(submit(f,originalKey));
        String wrongKey=key();
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status->{
            int inserted=jdbc.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) SELECT 2,booking_master_id,'SYNTHETIC-WRONG-BOOKING',?,payload_sha256,operator_id,result_json FROM ipad_batch_request WHERE client_request_id=?",wrongKey,originalKey);
            assertEquals(1,inserted,"Single-column FK does not protect the redundant scope copies");
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request r JOIN booking_master b ON b.id=r.booking_master_id WHERE r.client_request_id=? AND r.store_id<>b.store_id AND r.booking_id<>b.booking_id",Integer.class,wrongKey));
            System.out.println("BATCH_FK_NEGATIVE accepted_mismatched_store_and_booking=true action=transaction_rollback application_checks_are_not_composite_foreign_key=true");
            status.setRollbackOnly();
        });
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id=?",Integer.class,wrongKey));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) SELECT store_id,9223372036854775807,booking_id,?,payload_sha256,operator_id,result_json FROM ipad_batch_request WHERE client_request_id=?",key(),originalKey));
        assertEquals(1,receipts(f));assertEquals(2,rows(f));
    }
}
