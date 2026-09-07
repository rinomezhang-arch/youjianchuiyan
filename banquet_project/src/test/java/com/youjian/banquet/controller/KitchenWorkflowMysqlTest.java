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
class KitchenWorkflowMysqlTest {
    JdbcTemplate jdbc;
    BookingController controller;
    KitchenController kitchen;
    IpadOrderController orders;
    LocalContainerEntityManagerFactoryBean factory;
    JpaTransactionManager transactionManager;
    int nextTable = 100;
    LocalDate date = LocalDate.now().plusDays(2);

    @BeforeAll void start() {
        String schema = "kitchen_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        DriverManagerDataSource admin = new DriverManagerDataSource(url + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        new JdbcTemplate(admin).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("KITCHEN_EVIDENCE schema=" + schema + " retained=true source=JPA-explicit-restaurant-entities");
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
        IpadOrderController orderTarget = new IpadOrderController();
        ReflectionTestUtils.setField(orderTarget, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(orderTarget, "entityManager", em);
        ReflectionTestUtils.setField(orderTarget, "bookingRepo", repositories.getRepository(BookingMasterRepository.class));
        ReflectionTestUtils.setField(orderTarget, "bookingTableRepo", repositories.getRepository(BookingTableRepository.class));
        ReflectionTestUtils.setField(orderTarget, "dishDetailRepo", repositories.getRepository(BookingDishDetailRepository.class));
        ReflectionTestUtils.setField(orderTarget, "dishRepo", repositories.getRepository(DishMasterRepository.class));
        ReflectionTestUtils.setField(orderTarget, "kitchenLogRepo", repositories.getRepository(KitchenLogRepository.class));
        ReflectionTestUtils.setField(orderTarget, "notifyPublisher", mock(com.youjian.banquet.service.NotifyPublisher.class));
        ProxyFactory orderProxy = new ProxyFactory(orderTarget);
        orderProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        orders = (IpadOrderController) orderProxy.getProxy();
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,employment_status) VALUES (2,2,'合成员工二','kitchen-test-2','在职'),(3,1,'离职测试员工','kitchen-test-3','离职')");
        KitchenController kt = new KitchenController();
        ReflectionTestUtils.setField(kt,"jdbc",jdbc);
        ProxyFactory kp = new ProxyFactory(kt);
        kp.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        kitchen = (KitchenController)kp.getProxy();
    }

    @AfterAll void stop() { if (factory != null) factory.destroy(); }
    void as(long staff, long store) { UserContext.set(new UserContext.CurrentUser(staff,store,"store_manager","kitchen-synthetic")); }
    @BeforeEach void identity() { as(1,1); }
    @AfterEach void evidence(TestInfo test) {
        try {
            assertEquals(0,jdbc.queryForObject("""
                SELECT COUNT(*) FROM kitchen_log l
                LEFT JOIN booking_dish_detail d ON d.dish_booking_id=CAST(JSON_UNQUOTE(JSON_EXTRACT(l.note,'$.dishBookingId')) AS UNSIGNED)
                    AND d.store_id=l.store_id AND d.booking_id=l.booking_id AND d.dish_id=l.dish_id
                LEFT JOIN booking_master b ON b.booking_id=l.booking_id AND b.store_id=l.store_id
                LEFT JOIN staff_master s ON s.staff_id=l.operator_id AND s.store_id=l.store_id
                WHERE l.target_type='booking_dish_detail' AND (d.dish_booking_id IS NULL OR b.id IS NULL OR s.staff_id IS NULL
                    OR JSON_UNQUOTE(JSON_EXTRACT(l.note,'$.to'))<>l.action)
                """,Integer.class));
            System.out.println("KITCHEN_EVIDENCE method="+test.getTestMethod().orElseThrow().getName()
                +" details="+jdbc.queryForList("SELECT dish_booking_id,store_id,booking_id,dish_id,kitchen_status,kitchen_started_at,kitchen_done_at FROM booking_dish_detail ORDER BY dish_booking_id")
                +" events="+jdbc.queryForList("SELECT id,store_id,booking_id,dish_id,action,target_type,operator_id,note FROM kitchen_log ORDER BY id")
                +" bookings="+jdbc.queryForList("SELECT id,store_id,booking_id,booking_status,payment_status FROM booking_master ORDER BY id")
                +" tableLinks="+jdbc.queryForList("SELECT table_booking_id,booking_id,store_id,table_id FROM booking_table ORDER BY table_booking_id"));
        } finally { UserContext.clear(); }
    }
    record Dish(String booking,long id,long store,int table) {}
    MockHttpServletRequest device(long store) {
        var req=new MockHttpServletRequest(); req.setAttribute("ipad_store_id",store); req.setAttribute("ipad_staff_id",store); return req;
    }
    Dish fixture(long store,boolean send) {
        as(store,store); int t=nextTable++;
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_name,table_number,table_status,is_active,sort_order) VALUES (?,?,?,?,'idle',1,1)",t,store,"合成厨房桌"+t,"K"+t);
        var created=controller.create(new HashMap<>(Map.of("customerName","合成厨房客户","customerPhone","1380000"+String.format("%04d",t),"bookingDate",date.toString(),"bookingTime","18:00","tableIds",List.of(t)))).getBody();
        assertEquals(200,created.getCode(),created.getMessage());
        String booking=created.getData().getBookingId();
        return add(booking,t,store,send);
    }
    Dish add(String booking,int t,long store,boolean send) {
        String did="KITCHEN"+UUID.randomUUID().toString().replace("-","").substring(0,16);
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES (?,?,?,18.50,1)",did,store,"合成菜"+did);
        var added=orders.addDish(Map.of("booking_id",booking,"dish_id",did,"dish_quantity",2),device(store));
        assertEquals(200,added.getCode(),added.getMessage());
        long id=jdbc.queryForObject("SELECT dish_booking_id FROM booking_dish_detail WHERE booking_id=? AND dish_id=? AND store_id=?",Long.class,booking,did,store);
        if(send) { var sent=orders.sendToKitchen(Map.of("booking_id",booking),device(store)); assertEquals(200,sent.getCode(),sent.getMessage()); }
        return new Dish(booking,id,store,t);
    }
    int move(Dish d,String status) { return kitchen.updateOrderStatus(d.id(),Map.of("status",status)).getCode(); }
    String state(Dish d) { return jdbc.queryForObject("SELECT kitchen_status FROM booking_dish_detail WHERE dish_booking_id=?",String.class,d.id()); }
    int events(Dish d) { return jdbc.queryForObject("SELECT COUNT(*) FROM kitchen_log WHERE target_type='booking_dish_detail' AND JSON_EXTRACT(note,'$.dishBookingId')=?",Integer.class,d.id()); }
    Map<String,Object> detail(Dish d) { return jdbc.queryForMap("SELECT * FROM booking_dish_detail WHERE dish_booking_id=?",d.id()); }
    boolean queued(Dish d) { var r=kitchen.listOrders(null); assertEquals(200,r.getCode(),r.getMessage()); return r.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==d.id()); }

    @Test void realAddSendPrepareServeTargetsOneDetailAndPreservesHistory() throws Exception {
        Dish a=fixture(1,true), b=add(a.booking(),a.table(),1,true);
        assertTrue(queued(a)); assertTrue(queued(b));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(kitchen).build();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        for(String next:List.of("preparing","served")) {
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/kitchen/orders/"+a.id()+"/status")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",next)))).andReturn().getResponse();
            assertEquals(200,json.readTree(response.getContentAsString()).get("code").asInt());
            assertEquals(next,state(a)); assertEquals("submitted",state(b));
        }
        assertFalse(queued(a)); assertTrue(queued(b)); assertEquals(2,events(a)); assertEquals(0,events(b));
        assertNotNull(detail(a).get("kitchen_started_at")); assertNotNull(detail(a).get("kitchen_done_at"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id=?",Integer.class,a.booking()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM booking_table WHERE booking_id=?",Integer.class,a.booking()));
        assertEquals(200,move(a,"served")); assertEquals(2,events(a));
        var list=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/kitchen/orders")).andReturn().getResponse();
        var data=json.readTree(list.getContentAsString()).get("data");
        assertTrue(data.isArray()); assertTrue(data.size()>0);
        assertTrue(java.util.stream.StreamSupport.stream(data.spliterator(),false).anyMatch(x->x.get("id").asLong()==b.id() && x.has("dishName") && x.get("quantity").asInt()==2));
    }
    @Test void strictStoreIdentityAndCrossStoreIsolation() {
        Dish one=fixture(1,true), two=fixture(2,true);
        assertTrue(queued(two)); assertFalse(queued(one)); assertEquals(404,move(one,"preparing"));
        assertEquals(403,kitchen.listOrders("1").getCode()); assertEquals(200,move(two,"preparing"));
        as(1,1); assertTrue(queued(one)); assertFalse(queued(two)); assertEquals(404,move(two,"served"));
        for(long[] identity:List.of(new long[]{1,2},new long[]{3,1},new long[]{999,1})) {
            as(identity[0],identity[1]); assertEquals(403,move(one,"preparing")); assertEquals(403,kitchen.listOrders(null).getCode());
        }
        as(1,0); assertEquals(403,move(one,"preparing"));
        UserContext.clear(); assertEquals(403,move(one,"preparing"));
        assertEquals("submitted",state(one)); assertEquals(0,events(one)); assertEquals(1,events(two));
    }
    @Test void illegalTransitionsAndIdempotentRetries() {
        Dish d=fixture(1,false); assertEquals(409,move(d,"preparing"));
        assertEquals(200,orders.sendToKitchen(Map.of("booking_id",d.booking()),device(1)).getCode());
        assertEquals(409,move(d,"served"));
        for(String invalid:List.of("making","ready","cancelled","submitted","served'; UPDATE booking_master", "")) assertEquals(400,move(d,invalid));
        assertEquals(200,move(d,"preparing")); var before=detail(d); assertEquals(200,move(d,"preparing")); assertEquals(before,detail(d)); assertEquals(1,events(d));
        assertEquals(200,move(d,"served")); assertEquals(409,move(d,"preparing")); assertEquals(2,events(d));
    }
    @Test void urgentMustReconfirmPreparingBeforeServing() {
        Dish d=fixture(1,true);
        var urgent=orders.urgentDish(Map.of("dish_booking_id",d.id()),device(1));
        assertEquals(200,urgent.getCode(),urgent.getMessage());
        assertEquals("urgent",state(d)); assertTrue(queued(d)); assertEquals(409,move(d,"served"));
        assertEquals(200,move(d,"preparing")); assertEquals(200,move(d,"served")); assertEquals(2,events(d));
    }
    @Test void cancelledCompletedAndPaidOrdersRejectAndRetainRows() {
        for(String closed:List.of("cancelled","completed","paid")) {
            Dish d=fixture(1,true);
            if(closed.equals("cancelled")) assertEquals(200,((Result<?>)controller.delete(d.booking(),1L).getBody()).getCode());
            else if(closed.equals("completed")) jdbc.update("UPDATE booking_master SET booking_status='completed' WHERE booking_id=?",d.booking());
            else jdbc.update("UPDATE booking_master SET payment_status='paid' WHERE booking_id=?",d.booking());
            // Completed/paid are explicit closed-state fixtures, not claimed as checkout acceptance.
            assertEquals(409,move(d,"preparing")); assertFalse(queued(d)); assertEquals(0,events(d));
            assertEquals("submitted",state(d)); assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM booking_table WHERE booking_id=?",Integer.class,d.booking()));
        }
    }
    @Test void concurrentDuplicateTransitionsProduceOneEventPerStage() throws Exception {
        Dish d=fixture(1,true);
        var pool=Executors.newFixedThreadPool(2);
        try {
            for(String next:List.of("preparing","served")) {
                CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
                Callable<Integer> task=()->{ as(1,1); try { ready.countDown(); assertTrue(go.await(10,TimeUnit.SECONDS)); return move(d,next); } finally { UserContext.clear(); } };
                var first=pool.submit(task); var second=pool.submit(task); assertTrue(ready.await(10,TimeUnit.SECONDS)); go.countDown();
                assertEquals(200,first.get(15,TimeUnit.SECONDS)); assertEquals(200,second.get(15,TimeUnit.SECONDS)); assertEquals(next,state(d));
            }
            assertEquals(2,events(d));
        } finally { pool.shutdown(); assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS)); }
    }
    @Test void logInsertFailureRollsBackStateAndTimestamp() {
        Dish d=fixture(1,true); assertEquals(200,move(d,"preparing")); var before=detail(d);
        jdbc.execute("CREATE TRIGGER reject_served_"+d.id()+" BEFORE INSERT ON kitchen_log FOR EACH ROW BEGIN IF NEW.action='served' AND NEW.booking_id='"+d.booking()+"' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic kitchen log failure'; END IF; END");
        assertEquals(500,move(d,"served")); assertEquals(before,detail(d)); assertEquals(1,events(d)); assertTrue(queued(d));
    }
    @Test void cancellationLockPreventsConcurrentServing() throws Exception {
        Dish d=fixture(1,true); assertEquals(200,move(d,"preparing"));
        var pool=Executors.newSingleThreadExecutor(); var started=new CountDownLatch(1);
        final Future<Integer>[] future=new Future[1];
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                jdbc.queryForList("SELECT id FROM booking_master WHERE booking_id=? FOR UPDATE",d.booking());
                future[0]=pool.submit(()->{ as(1,1); try { started.countDown(); return move(d,"served"); } finally { UserContext.clear(); } });
                try { assertTrue(started.await(10,TimeUnit.SECONDS)); } catch(InterruptedException e) { throw new RuntimeException(e); }
                assertEquals(200,((Result<?>)controller.delete(d.booking(),1L).getBody()).getCode());
            });
            assertEquals(409,future[0].get(15,TimeUnit.SECONDS)); assertEquals("preparing",state(d)); assertEquals(1,events(d)); assertFalse(queued(d));
        } finally { pool.shutdown(); assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS)); }
    }
    @Test void gmReadScopeSelectsStoresWithoutGrantingCrossStoreOperations() {
        Dish one=fixture(1,true), two=fixture(2,true);
        var aspect=new com.youjian.banquet.aop.StoreDataScopeAspect();
        for(long identityStore:List.of(1L,0L)) {
            UserContext.set(new UserContext.CurrentUser(1L,identityStore,"gm","synthetic-gm"));
            // Execute the existing aspect's actual scope rule against a pre-populated context, not a fabricated permission flag.
            ReflectionTestUtils.invokeMethod(aspect,"applyDataScope");
            assertTrue(UserContext.isDataScopeAll()); assertTrue(UserContext.isGeneralManager());
            var selected=kitchen.listOrders("2"); assertEquals(200,selected.getCode(),selected.getMessage());
            assertFalse(selected.getData().isEmpty());
            assertTrue(selected.getData().stream().allMatch(x->((Number)x.get("storeId")).longValue()==2 && Boolean.FALSE.equals(x.get("canOperate"))));
            assertTrue(selected.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==two.id()));
            var all=kitchen.listOrders("0"); assertEquals(200,all.getCode());
            assertTrue(all.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==one.id()));
            assertTrue(all.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==two.id()));
            assertEquals(identityStore==0 ? 403 : 404,move(two,"preparing"));
            assertEquals("submitted",state(two)); assertEquals(0,events(two));
            assertEquals(400,kitchen.listOrders("bad-store").getCode());
        }
        UserContext.clear(); as(1,1);
        assertEquals(403,kitchen.listOrders("2").getCode()); assertEquals(200,move(one,"preparing"));
    }

    @Test void servedLogsReadBackThroughBusinessQueryWithStoreAndDetailIdentity() throws Exception {
        var columns=jdbc.queryForList("SHOW COLUMNS FROM kitchen_log").stream().map(x->x.get("Field").toString()).toList();
        assertTrue(columns.contains("created_at")); assertFalse(columns.contains("create_time"));
        Dish one=fixture(1,true); assertEquals(200,move(one,"preparing")); assertEquals(200,move(one,"served"));
        Dish two=fixture(2,true); assertEquals(200,move(two,"preparing")); assertEquals(200,move(two,"served"));
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        as(1,1);
        var logs=kitchen.listLogs(null,"served",one.booking()); assertEquals(200,logs.getCode(),logs.getMessage());
        assertEquals(1,logs.getData().size()); var row=logs.getData().get(0);
        assertEquals(1L,((Number)row.get("store_id")).longValue()); assertEquals(one.booking(),row.get("booking_id"));
        assertEquals("booking_dish_detail",row.get("target_type")); assertEquals("served",row.get("action"));
        var note=json.readTree(row.get("note").toString()); assertEquals(one.id(),note.get("dishBookingId").asLong());
        assertEquals("preparing",note.get("from").asText()); assertEquals("served",note.get("to").asText()); assertNotNull(row.get("created_at"));
        // Existing ordinary-user resolver ignores a foreign selection and still scopes results to the operator's store.
        var isolated=kitchen.listLogs("2","served",null); assertEquals(200,isolated.getCode()); assertFalse(isolated.getData().isEmpty());
        assertTrue(isolated.getData().stream().allMatch(x->((Number)x.get("store_id")).longValue()==1));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(kitchen).build();
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/kitchen/logs")
            .param("action","served").param("keyword",one.booking())).andReturn().getResponse();
        var data=json.readTree(response.getContentAsString()); assertEquals(200,data.get("code").asInt()); assertEquals(1,data.get("data").size());
        assertEquals(one.id(),json.readTree(data.get("data").get(0).get("note").asText()).get("dishBookingId").asLong());
        as(2,2); var own=kitchen.listLogs(null,"served",two.booking()); assertEquals(200,own.getCode()); assertEquals(1,own.getData().size());
        assertEquals(two.id(),json.readTree(own.getData().get(0).get("note").toString()).get("dishBookingId").asLong());
        System.out.println("KITCHEN_LOG_API_EVIDENCE columns="+columns+" store1="+logs.getData()+" store2="+own.getData());
    }

}
