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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KitchenStatsMysqlTest {
    JdbcTemplate jdbc;
    BookingController controller;
    KitchenController kitchen;
    IpadOrderController orders;
    LocalContainerEntityManagerFactoryBean factory;
    JpaTransactionManager transactionManager;
    int nextTable = 100;
    LocalDate date = LocalDate.now();

    @BeforeAll void start() {
        String schema = "kitchen_stats_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        DriverManagerDataSource admin = new DriverManagerDataSource(url + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        new JdbcTemplate(admin).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("KITCHEN_STATS_EVIDENCE schema=" + schema + " retained=true source=JPA-explicit-restaurant-entities");
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
            System.out.println("KITCHEN_STATS_EVIDENCE method="+test.getTestMethod().orElseThrow().getName()
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


    Map<String,Object> stats(String store) { var r=kitchen.stats(store); assertEquals(200,r.getCode(),r.getMessage()); return r.getData(); }
    void assertCounts(Map<String,Object> data,long total,long pending,long urgent,long refunded,String rate) {
        assertEquals(total,((Number)data.get("totalDetails")).longValue());
        assertEquals(pending,((Number)data.get("pendingDetails")).longValue());
        assertEquals(urgent,((Number)data.get("urgentDetails")).longValue());
        assertEquals(refunded,((Number)data.get("refundedDetails")).longValue());
        assertEquals(rate,data.get("returnRate")); assertEquals(LocalDate.now().toString(),data.get("periodDate"));
        assertEquals("booking_date",data.get("periodBasis")); assertEquals("dish_booking_id",data.get("grain"));
        assertEquals(data.get("totalDetails"),data.get("todayTotal")); assertEquals(data.get("pendingDetails"),data.get("pendingOrders"));
    }
    void refund(Dish d) { var r=orders.refundDish(Map.of("dish_booking_id",d.id(),"refund_reason","合成统计退菜"),device(d.store())); assertEquals(200,r.getCode(),r.getMessage()); }
    void shiftMealDate(Dish d,LocalDate day) {
        jdbc.update("UPDATE booking_master SET booking_date=? WHERE booking_id=? AND store_id=?",java.sql.Date.valueOf(day),d.booking(),d.store());
        jdbc.update("UPDATE booking_table SET booking_date=? WHERE booking_id=? AND store_id=?",java.sql.Date.valueOf(day),d.booking(),d.store());
    }
    @Test @Order(1) void sameMealDatePopulationQueueGrainRefundCounterexamplesAndGmScope() throws Exception {
        Dish submitted=fixture(1,true);
        Dish preparing=add(submitted.booking(),submitted.table(),1,true); assertEquals(200,move(preparing,"preparing"));
        Dish urgent=add(submitted.booking(),submitted.table(),1,true); assertEquals(200,orders.urgentDish(Map.of("dish_booking_id",urgent.id()),device(1)).getCode());
        Dish served=add(submitted.booking(),submitted.table(),1,true); assertEquals(200,move(served,"preparing")); assertEquals(200,move(served,"served"));
        Dish refunded=add(submitted.booking(),submitted.table(),1,true); refund(refunded);
        Dish pending=add(submitted.booking(),submitted.table(),1,false);
        Dish cancelledDetail=add(submitted.booking(),submitted.table(),1,false);
        jdbc.update("UPDATE booking_dish_detail SET kitchen_status='cancelled' WHERE dish_booking_id=?",cancelledDetail.id());
        Dish cancelledBooking=fixture(1,true); assertEquals(200,((Result<?>)controller.delete(cancelledBooking.booking(),1L).getBody()).getCode());
        Dish completed=fixture(1,true); jdbc.update("UPDATE booking_master SET booking_status='completed' WHERE booking_id=?",completed.booking());
        Dish paid=fixture(1,true); assertEquals(200,orders.urgentDish(Map.of("dish_booking_id",paid.id()),device(1)).getCode());
        jdbc.update("UPDATE booking_master SET payment_status='paid' WHERE booking_id=?",paid.booking());
        // These closed-state markers are fixtures for inclusion rules, not payment/checkout acceptance.
        Dish yesterday=fixture(1,true); refund(yesterday);
        for(int i=0;i<11;i++) refund(add(yesterday.booking(),yesterday.table(),1,true));
        Dish oldQueue=add(yesterday.booking(),yesterday.table(),1,true); shiftMealDate(yesterday,LocalDate.now().minusDays(1));
        Dish tomorrow=fixture(1,true); shiftMealDate(tomorrow,LocalDate.now().plusDays(1));
        var local=stats(null); assertCounts(local,8,3,1,1,"12.5%");
        // Six active-booking details, two closed/paid details; quantity=2 per detail is never doubled in counts.
        assertEquals(13,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE store_id=1 AND kitchen_status='refunded'",Integer.class));
        var queue=kitchen.listOrders(null); assertEquals(200,queue.getCode());
        Set<Long> todayQueue=new HashSet<>();
        for(var row:queue.getData()) {
            LocalDate meal=jdbc.queryForObject("SELECT booking_date FROM booking_master WHERE booking_id=? AND store_id=1",java.sql.Date.class,row.get("bookingId")).toLocalDate();
            if(meal.equals(LocalDate.now())) todayQueue.add(((Number)row.get("id")).longValue());
        }
        assertEquals(Set.of(submitted.id(),preparing.id(),urgent.id()),todayQueue);
        assertEquals(todayQueue.size(),((Number)local.get("pendingDetails")).intValue());
        assertTrue(queue.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==oldQueue.id()));
        assertTrue(queue.getData().stream().anyMatch(x->((Number)x.get("id")).longValue()==tomorrow.id()));
        Dish second=fixture(2,true); Dish secondRefund=add(second.booking(),second.table(),2,true);refund(secondRefund);
        var store2=stats(null);assertCounts(store2,2,1,0,1,"50.0%");
        as(1,1);assertCounts(stats("2"),8,3,1,1,"12.5%"); // Existing stats contract: ordinary user remains in own store.
        UserContext.set(new UserContext.CurrentUser(1L,1L,"gm","stats-gm"));
        var gm2=stats("2");assertCounts(gm2,2,1,0,1,"50.0%");
        var global=stats("0");assertCounts(global,10,4,1,2,"20.0%");
        UserContext.set(new UserContext.CurrentUser(1L,0L,"gm","stats-global"));assertCounts(stats("2"),2,1,0,1,"50.0%");
        assertEquals(400,kitchen.stats("invalid").getCode());
        UserContext.clear();assertEquals(403,kitchen.stats(null).getCode());as(1,1);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(kitchen).build();
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/kitchen/stats")).andReturn().getResponse();
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());assertEquals(200,json.get("code").asInt());assertEquals(8,json.get("data").get("totalDetails").asLong());
        System.out.println("KITCHEN_STATS_API_EVIDENCE own="+local+" second="+store2+" gm2="+gm2+" global="+global+" historicalRefunds=12 todayRefunds=1 todayQueue="+todayQueue);
    }
    @Test @Order(2) void realQueryFailureReturnsErrorNotZeroStatistics() {
        Dish d=fixture(1,true); var valid=stats(null);assertTrue(((Number)valid.get("totalDetails")).longValue()>0);
        String schema="kitchen_stats_fault_"+UUID.randomUUID().toString().replace("-","");
        var admin=new JdbcTemplate(new DriverManagerDataSource("jdbc:mysql://127.0.0.1:13317/?useSSL=false&allowPublicKeyRetrieval=true","root",""));
        admin.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var broken=new KitchenController();ReflectionTestUtils.setField(broken,"jdbc",new JdbcTemplate(new DriverManagerDataSource("jdbc:mysql://127.0.0.1:13317/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true","root","")));
        // GM query bypasses staff lookup so the real failure is the absent statistics tables in this new isolated schema.
        UserContext.set(new UserContext.CurrentUser(1L,1L,"gm","stats-fault"));
        var failure=broken.stats("1");assertEquals(500,failure.getCode());assertNull(failure.getData());
        System.out.println("KITCHEN_STATS_FAILURE_EVIDENCE retainedSchema="+schema+" nonemptyControl="+valid+" failureCode="+failure.getCode()+" data="+failure.getData());
    }
}
