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
class BookingMysqlIntegrationTest {
    JdbcTemplate jdbc;
    BookingController controller;
    TableBoardController board;
    IpadOrderController orders;
    IpadCheckoutController checkout;
    com.youjian.banquet.service.InventoryService inventory;
    LocalContainerEntityManagerFactoryBean factory;
    JpaTransactionManager transactionManager;
    int nextTable = 100;
    LocalDate date = LocalDate.now().plusDays(2);

    @BeforeAll void start() {
        String schema = "acceptance_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:mysql://127.0.0.1:13317/";
        DriverManagerDataSource admin = new DriverManagerDataSource(url + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        new JdbcTemplate(admin).execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
        System.out.println("BOOKING_EVIDENCE schema=" + schema + " retained=true source=JPA-explicit-restaurant-entities");
        DriverManagerDataSource ds = new DriverManagerDataSource(url + schema + "?useSSL=false&allowPublicKeyRetrieval=true", "root", "");
        jdbc = new JdbcTemplate(ds);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(BookingMaster.class.getName(), BookingTable.class.getName(),
            BookingDishDetail.class.getName(), CustomerMaster.class.getName(), TableMaster.class.getName(),
            DishMaster.class.getName(), KitchenLog.class.getName(), FinanceTransaction.class.getName(),
            IngredientMaster.class.getName(), IngredientInventoryLog.class.getName(),
            StoreInfo.class.getName(), StaffMaster.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name,status) VALUES (1,'TEST-1','合成验收门店一','active'),(2,'TEST-2','合成验收门店二','active')");
        jdbc.update("INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,employment_status) VALUES (1,1,'合成验收员工','test-operator','在职')");
        EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(em);
        BookingController target = new BookingController();
        ReflectionTestUtils.setField(target, "jdbc", jdbc);
        ReflectionTestUtils.setField(target, "bookingMasterRepo", repositories.getRepository(BookingMasterRepository.class));
        ReflectionTestUtils.setField(target, "bookingTableRepo", repositories.getRepository(BookingTableRepository.class));
        ReflectionTestUtils.setField(target, "bookingDishDetailRepo", repositories.getRepository(BookingDishDetailRepository.class));
        JpaTransactionManager manager = new JpaTransactionManager(factory.getObject());
        transactionManager = manager;
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        controller = (BookingController) proxy.getProxy();
        board = new TableBoardController();
        ReflectionTestUtils.setField(board, "jdbc", jdbc);
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
        ProxyFactory checkoutProxy = new ProxyFactory(new IpadCheckoutController(jdbc,
            repositories.getRepository(BookingMasterRepository.class), repositories.getRepository(BookingTableRepository.class),
            repositories.getRepository(BookingDishDetailRepository.class), repositories.getRepository(DishMasterRepository.class)));
        checkoutProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        checkout = (IpadCheckoutController) checkoutProxy.getProxy();
        com.youjian.banquet.service.InventoryService inventoryTarget = new com.youjian.banquet.service.InventoryService();
        ReflectionTestUtils.setField(inventoryTarget, "ingredientMasterRepository", repositories.getRepository(IngredientMasterRepository.class));
        ReflectionTestUtils.setField(inventoryTarget, "inventoryLogRepository", repositories.getRepository(IngredientInventoryLogRepository.class));
        ProxyFactory inventoryProxy = new ProxyFactory(inventoryTarget);
        inventoryProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        inventory = (com.youjian.banquet.service.InventoryService) inventoryProxy.getProxy();
        jdbc.execute("CREATE TABLE ipad_payment_request (payment_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, store_id BIGINT NOT NULL, "
            + "idempotency_key VARCHAR(100) NOT NULL, booking_id VARCHAR(64) NOT NULL, amount DECIMAL(12,2) NOT NULL, "
            + "pay_type VARCHAR(20) NOT NULL, operator_id BIGINT NOT NULL, UNIQUE KEY uk_store_key(store_id,idempotency_key))");
    }
    @AfterAll void stop() { if (factory != null) factory.destroy(); }
    @BeforeEach void identity() { UserContext.set(new UserContext.CurrentUser(1L, 1L, "store_manager", "test-operator")); }
    @AfterEach void clear() { UserContext.clear(); }
    @AfterEach void verifyPersistedRelationshipsAndAmounts(TestInfo test) {
        System.out.println("BOOKING_EVIDENCE method=" + test.getTestMethod().orElseThrow().getName()
            + " bookings=" + jdbc.queryForList("SELECT id,booking_id,store_id,customer_id,booking_status FROM booking_master ORDER BY id")
            + " tables=" + jdbc.queryForList("SELECT table_id,table_status FROM table_master ORDER BY table_id")
            + " links=" + count("booking_table") + " dishes=" + count("booking_dish_detail"));
        Map<String, Integer> rowCounts = new LinkedHashMap<>();
        for (String table : List.of("store_info", "staff_master", "customer_master", "booking_master", "booking_table",
                "booking_dish_detail", "table_master", "dish_master", "kitchen_log", "finance_transaction",
                "ipad_payment_request", "ingredient_master", "ingredient_inventory_log")) {
            rowCounts.put(table, count(table));
        }
        System.out.println("BOOKING_EVIDENCE rows method=" + test.getTestMethod().orElseThrow().getName() + " " + rowCounts);
        System.out.println("BOOKING_EVIDENCE ledger method=" + test.getTestMethod().orElseThrow().getName()
            + " receipts=" + jdbc.queryForList("SELECT trans_id,store_id,related_type,related_id,amount FROM finance_transaction ORDER BY trans_id")
            + " payments=" + jdbc.queryForList("SELECT payment_request_id,store_id,booking_id,amount FROM ipad_payment_request ORDER BY payment_request_id")
            + " stock=" + jdbc.queryForList("SELECT store_id,ingredient_id,current_stock FROM ingredient_master ORDER BY store_id,ingredient_id")
            + " movements=" + jdbc.queryForList("SELECT log_id,store_id,food_material_id,change_type,change_quantity,before_quantity,after_quantity FROM ingredient_inventory_log ORDER BY log_id"));
        BusinessDataAssertions.verify(jdbc);
        System.out.println("BOOKING_EVIDENCE integrity=passed method=" + test.getTestMethod().orElseThrow().getName());
    }

    int table() {
        int id = nextTable++;
        jdbc.update("INSERT INTO table_master (table_id, store_id, table_name, table_number, table_status, is_active, sort_order) VALUES (?,1,?,?,'idle',1,1)", id, "验收桌" + id, "T" + id);
        return id;
    }
    Map<String, Object> request(int table, String time) {
        return new HashMap<>(Map.of("customerName", "合成验收客户", "customerPhone", "1380000" + String.format("%04d", table),
            "bookingDate", date.toString(), "bookingTime", time, "tableIds", List.of(table)));
    }
    String status(int table) { return jdbc.queryForObject("SELECT table_status FROM table_master WHERE table_id=?", String.class, table); }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    MockHttpServletRequest device() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("ipad_store_id", 1L);
        req.setAttribute("ipad_staff_id", 1L);
        return req;
    }
    String dish(int table) {
        String id = "TEST" + table;
        jdbc.update("INSERT INTO dish_master (dish_id,store_id,dish_name,sale_price,is_active) VALUES (?,1,'合成验收菜',80.50,1)", id);
        return id;
    }
    String walkIn(int table, String dish) {
        Result<Map<String,Object>> result = checkout.submit(Map.of("table_id", table, "guest_count", 2,
            "dishes", List.of(Map.of("dish_id",dish,"dish_quantity",2))), device());
        assertEquals(200, result.getCode(), result.getMessage());
        return result.getData().get("booking_id").toString();
    }

    @Test void frontendOrderAndCashPaymentJsonContractPersistsAndReloads() throws Exception {
        // MVC serialization/binding plus real MySQL; identity is supplied by the test, not a login test.
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(checkout, orders)
            .setControllerAdvice(new com.youjian.banquet.exception.GlobalExceptionHandler()).build();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        String apiSource = java.nio.file.Files.readString(java.nio.file.Path.of("../frontend_v3/src/api/ipad.js"));
        assertTrue(apiSource.contains("ipadRequest.post('/order/submit', data)"));
        assertTrue(apiSource.contains("ipadRequest.get(`/settlement/bill/${bookingId}`)"));
        assertTrue(apiSource.contains("'Idempotency-Key': idempotencyKey"));
        int t = table(); String d = dish(t);
        var submitted = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ipad/order/submit")
            .requestAttr("ipad_store_id",1L).requestAttr("ipad_staff_id",1L)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("table_id",t,"guest_count",2,"customer_name","合成契约验收散客",
                "booking_type","normal","dishes",List.of(Map.of("dish_id",d,"dish_quantity",2,"dish_note","测试备注"))))))
            .andReturn().getResponse();
        assertEquals(200,submitted.getStatus());
        var body=json.readTree(submitted.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(200,body.path("code").asInt());
        String id=body.path("data").path("booking_id").asText();
        assertFalse(id.isBlank());
        assertEquals(161.0,body.path("data").path("total_amount").asDouble());
        assertEquals("测试备注",jdbc.queryForObject("SELECT dish_note FROM booking_dish_detail WHERE booking_id=?",String.class,id));
        String key=UUID.randomUUID().toString();
        var paid=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ipad/settlement/pay")
            .requestAttr("ipad_store_id",1L).requestAttr("ipad_staff_id",1L).header("Idempotency-Key",key)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("booking_id",id,"pay_type","cash","pay_amount",170))))
            .andReturn().getResponse();
        var paidBody=json.readTree(paid.getContentAsString());
        assertEquals(200,paidBody.path("code").asInt());
        assertEquals(9.0,paidBody.path("data").path("change_amount").asDouble());
        var reloaded=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ipad/settlement/bill/"+id)
            .requestAttr("ipad_store_id",1L).requestAttr("ipad_staff_id",1L)).andReturn().getResponse();
        var bill=json.readTree(reloaded.getContentAsString());
        assertEquals("paid",bill.path("data").path("payment_status").asText());
        assertEquals(161.0,bill.path("data").path("total_amount").asDouble());
        Long persistedBookingId = jdbc.queryForObject("SELECT id FROM booking_master WHERE booking_id=? AND store_id=?", Long.class, id, 1L);
        assertNotNull(persistedBookingId);
        assertEquals(new java.math.BigDecimal("161.00"),jdbc.queryForObject(
            "SELECT amount FROM finance_transaction WHERE related_type='booking' AND related_id=? AND store_id=?",
            java.math.BigDecimal.class, persistedBookingId, 1L));
    }

    @Test void integrityChecksDetectOrphansAndWrongAmountsWithoutLeavingCorruptData() {
        int t=table(); String d=dish(t),id=walkIn(t,d);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(state -> {
            jdbc.update("UPDATE booking_dish_detail SET booking_id='MISSING' WHERE booking_id=?",id);
            assertThrows(AssertionError.class,()->BusinessDataAssertions.verify(jdbc));
            state.setRollbackOnly();
        });
        transaction.executeWithoutResult(state -> {
            jdbc.update("UPDATE booking_dish_detail SET subtotal=subtotal+1 WHERE booking_id=?",id);
            assertThrows(AssertionError.class,()->BusinessDataAssertions.verify(jdbc));
            state.setRollbackOnly();
        });
        BusinessDataAssertions.verify(jdbc);
    }

    @Test void orderingKitchenBillingAndPaymentFormOneBusinessFlow() {
        int t = table();
        String dish = dish(t), id = walkIn(t,dish);
        assertEquals("occupied", status(t));
        Result<Map<String,Object>> added = orders.addDish(new HashMap<>(Map.of("booking_id",id,"dish_id",dish,"dish_quantity",1)), device());
        assertEquals(200, added.getCode(), added.getMessage());
        assertEquals(200, orders.sendToKitchen(Map.of("booking_id",id), device()).getCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id=? AND kitchen_status='pending'", Integer.class,id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM kitchen_log WHERE booking_id=?", Integer.class,id));
        assertEquals(new java.math.BigDecimal("241.50"), checkout.bill(id,device()).getData().get("total_amount"));
        Map<String,Object> payment = Map.of("booking_id",id,"pay_type","cash","pay_amount","250.00");
        String key = UUID.randomUUID().toString();
        Result<Map<String,Object>> paid = checkout.pay(payment,key,device());
        assertEquals(200,paid.getCode());
        assertEquals(new java.math.BigDecimal("8.50"),paid.getData().get("change_amount"));
        assertEquals("idle",status(t));
        assertEquals(200,checkout.pay(payment,key,device()).getCode());
        Long persistedBookingId = jdbc.queryForObject("SELECT id FROM booking_master WHERE booking_id=? AND store_id=?", Long.class, id, 1L);
        assertNotNull(persistedBookingId);
        assertEquals(1,jdbc.queryForObject(
            "SELECT COUNT(*) FROM finance_transaction WHERE related_type='booking' AND related_id=? AND store_id=?",
            Integer.class, persistedBookingId, 1L));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_payment_request WHERE booking_id=?",Integer.class,id));
    }

    @Test void cancelledBookingCannotReceiveDishesOrPayment() {
        int t=table(); String d=dish(t);
        String id=controller.create(request(t,"18:00")).getBody().getData().getBookingId();
        assertEquals(200,((Result<?>)controller.delete(id,1L).getBody()).getCode());
        assertEquals(409,orders.addDish(Map.of("booking_id",id,"dish_id",d),device()).getCode());
        assertEquals(409,orders.sendToKitchen(Map.of("booking_id",id),device()).getCode());
        assertThrows(IllegalStateException.class,()->checkout.pay(Map.of("booking_id",id,"pay_type","cash","pay_amount","1.00"),UUID.randomUUID().toString(),device()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id=?",Integer.class,id));
    }

    @Test void invalidBatchRollsBackAllDishesInsteadOfSilentlySavingSome() {
        int t=table(); String d=dish(t),id=walkIn(t,d);
        int before=count("booking_dish_detail");
        Result<?> result=orders.addDishesBatch(Map.of("booking_id",id,"dishes",List.of(
            Map.of("dish_id",d,"dish_quantity",1),Map.of("dish_id","MISSING","dish_quantity",1))),device());
        assertEquals(400,result.getCode());
        assertEquals(before,count("booking_dish_detail"));
    }

    @Test void removingUnsubmittedDishPreservesHistoryAndCorrectBill() {
        int t=table(); String d=dish(t),id=walkIn(t,d);
        Result<Map<String,Object>> added=orders.addDish(Map.of("booking_id",id,"dish_id",d),device());
        Object detail=added.getData().get("dish_booking_id");
        assertEquals(200,orders.removeDish(Map.of("dish_booking_id",detail),device()).getCode());
        assertEquals("refunded",jdbc.queryForObject("SELECT kitchen_status FROM booking_dish_detail WHERE dish_booking_id=?",String.class,detail));
        assertEquals(new java.math.BigDecimal("161.00"),checkout.bill(id,device()).getData().get("total_amount"));
    }

    @Test void paymentKeyCannotBeReusedForAnotherOrder() {
        int t=table(); String id=walkIn(t,dish(t));
        String key=UUID.randomUUID().toString();
        checkout.pay(Map.of("booking_id",id,"pay_type","cash","pay_amount","161.00"),key,device());
        assertThrows(IllegalArgumentException.class,()->checkout.pay(Map.of("booking_id","OTHER","pay_type","cash","pay_amount","161.00"),key,device()));
    }

    String ingredient() {
        String id="ING"+nextTable++;
        jdbc.update("INSERT INTO ingredient_master (ingredient_id,store_id,ingredient_name,current_stock) VALUES (?,1,'合成验收食材',10.000)",id);
        return id;
    }
    com.youjian.banquet.dto.InventoryDTO movement(String id,String quantity) {
        com.youjian.banquet.dto.InventoryDTO dto=new com.youjian.banquet.dto.InventoryDTO();
        dto.setIngredientId(id); dto.setStoreId("1"); dto.setQuantity(new java.math.BigDecimal(quantity));
        dto.setOperator("test-operator"); return dto;
    }
    java.math.BigDecimal stock(String id,long store) {
        return jdbc.queryForObject("SELECT current_stock FROM ingredient_master WHERE ingredient_id=? AND store_id=?",java.math.BigDecimal.class,id,store);
    }

    @Test void stockMovementMatchesPersistentAuditQuantities() {
        String id=ingredient();
        inventory.stockIn(movement(id,"2.500"));
        inventory.stockOut(movement(id,"1.250"));
        assertEquals(new java.math.BigDecimal("11.250"),stock(id,1L));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=? AND "
            + "((change_type='IN' AND after_quantity<>before_quantity+change_quantity) OR (change_type='OUT' AND after_quantity<>before_quantity-change_quantity))",Integer.class,id));
    }

    @Test void invalidInventoryQuantitiesCannotChangeStock() {
        String id=ingredient();
        for(String qty:List.of("0","-2","0.0001")) {
            assertThrows(IllegalArgumentException.class,()->inventory.stockIn(movement(id,qty)));
            assertThrows(IllegalArgumentException.class,()->inventory.stockOut(movement(id,qty)));
        }
        assertEquals(new java.math.BigDecimal("10.000"),stock(id,1L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
    }

    @Test void concurrentStockOutCannotOversell() throws Exception {
        String id=ingredient();
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        Callable<Boolean> withdraw=()->{
            ready.countDown();
            if(!go.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test start timeout");
            try { inventory.stockOut(movement(id,"7"));return true; }
            catch(IllegalArgumentException expected){return false;}
        };
        try {
            Future<Boolean> a=executor.submit(withdraw),b=executor.submit(withdraw);
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(new java.math.BigDecimal("3.000"),stock(id,1L));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        } finally {executor.shutdownNow();}
    }

    @Test void transferPreservesCombinedStockAndCreatesBothLogs() {
        String id=ingredient();
        jdbc.update("INSERT INTO ingredient_master (ingredient_id,store_id,ingredient_name,current_stock) VALUES (?,2,'合成验收食材',1.000)",id);
        inventory.transfer(id,1L,2L,new java.math.BigDecimal("3.500"),"test-operator",null);
        assertEquals(new java.math.BigDecimal("6.500"),stock(id,1L));
        assertEquals(new java.math.BigDecimal("4.500"),stock(id,2L));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
    }

    @Test void failedTransferDoesNotDeductSourceStock() {
        String id=ingredient();
        assertThrows(IllegalArgumentException.class,()->inventory.transfer(id,1L,2L,new java.math.BigDecimal("3"),"test-operator",null));
        assertEquals(new java.math.BigDecimal("10.000"),stock(id,1L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
    }

    @Test void createCancelAndRebookPreservesHistoryAndReleasesTable() {
        int t = table();
        Result<BookingMaster> created = controller.create(request(t, "18:00")).getBody();
        assertEquals(200, created.getCode(), created.getMessage());
        String id = created.getData().getBookingId();
        assertEquals("reserved", status(t));
        assertNotNull(created.getData().getCustomerId());
        String d = dish(t);
        assertEquals(200, orders.addDish(Map.of("booking_id",id,"dish_id",d,"dish_quantity",1),device()).getCode());
        Result<?> cancelled = (Result<?>) controller.delete(id, 1L).getBody();
        assertEquals(200, cancelled.getCode(), cancelled.getMessage());
        assertEquals("idle", status(t));
        assertEquals("cancelled", jdbc.queryForObject("SELECT booking_status FROM booking_master WHERE booking_id=?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_table WHERE booking_id=?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id=?", Integer.class, id));
        Map<String,Object> tile = board.board(1L, date.toString(), "dinner").getData().stream()
            .filter(row -> ((Number) row.get("table_id")).intValue() == t).findFirst().orElseThrow();
        assertNull(tile.get("booking_id"));
        assertEquals(200, controller.create(request(t, "18:00")).getBody().getCode());
    }

    @Test void currentOrderReadsOnlyCurrentMealFromRealAddedDishes() {
        LocalDate today = LocalDate.now();
        int t = table();
        String lunchDish = dish(t), dinnerDish = dish(nextTable++);
        Map<String, Object> lunchRequest = request(t, "12:00");
        lunchRequest.put("bookingDate", today.toString());
        Map<String, Object> dinnerRequest = request(t, "18:00");
        dinnerRequest.put("bookingDate", today.toString());
        Result<BookingMaster> lunch = controller.create(lunchRequest).getBody();
        Result<BookingMaster> dinner = controller.create(dinnerRequest).getBody();
        assertEquals(200, lunch.getCode(), lunch.getMessage());
        assertEquals(200, dinner.getCode(), dinner.getMessage());
        String lunchId = lunch.getData().getBookingId(), dinnerId = dinner.getData().getBookingId();
        assertNotEquals(lunchId, dinnerId);
        Result<?> lunchAdded = orders.addDish(Map.of("booking_id", lunchId, "dish_id", lunchDish,
            "dish_quantity", 1), device());
        Result<?> dinnerAdded = orders.addDish(Map.of("booking_id", dinnerId, "dish_id", dinnerDish,
            "dish_quantity", 2), device());
        assertEquals(200, lunchAdded.getCode(), lunchAdded.getMessage());
        assertEquals(200, dinnerAdded.getCode(), dinnerAdded.getMessage());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM booking_table WHERE table_id=? AND store_id=1", Integer.class, t));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id IN (?,?) AND store_id=1",
            Integer.class, lunchId, dinnerId));

        java.time.LocalDateTime beforeRead = java.time.LocalDateTime.now();
        boolean lunchNow = beforeRead.getHour() < 15;
        Result<List<Map<String, Object>>> current = orders.getCurrentOrder(Integer.toString(t), device());
        java.time.LocalDateTime afterRead = java.time.LocalDateTime.now();
        assertEquals(today, afterRead.toLocalDate(), "Clock crossed midnight; rerun with today's fixture");
        assertEquals(lunchNow, afterRead.getHour() < 15, "Clock crossed meal boundary during read; rerun");
        assertEquals(200, current.getCode(), current.getMessage());
        assertEquals(1, current.getData().size(), "Only the active meal's dish may be returned");
        Map<String, Object> actual = current.getData().get(0);
        assertEquals(lunchNow ? lunchDish : dinnerDish, actual.get("dish_id"));
        assertEquals(lunchNow ? 1 : 2, ((Number) actual.get("dish_quantity")).intValue());
        String expectedBooking = lunchNow ? lunchId : dinnerId;
        assertEquals(expectedBooking, jdbc.queryForObject(
            "SELECT booking_id FROM booking_dish_detail WHERE dish_booking_id=? AND store_id=1",
            String.class, actual.get("dish_booking_id")));
        System.out.println("BOOKING_EVIDENCE currentMeal=" + (lunchNow ? "lunch" : "dinner")
            + " readAt=" + beforeRead + " table=" + t + " lunchBooking=" + lunchId
            + " dinnerBooking=" + dinnerId + " returned=" + actual);
    }

    @Test void lunchAndDinnerAreIndependentButSamePeriodConflicts() {
        int t = table();
        assertEquals(200, controller.create(request(t, "12:00")).getBody().getCode());
        int before = count("booking_master");
        assertEquals(409, controller.create(request(t, "13:00")).getBody().getCode());
        assertEquals(before, count("booking_master"));
        assertEquals(200, controller.create(request(t, "18:00")).getBody().getCode());
    }

    @Test void databaseFailureRollsBackCustomerBookingAndTableWrites() {
        int t = table();
        jdbc.execute("CREATE TRIGGER fail_table_" + t + " BEFORE INSERT ON booking_table FOR EACH ROW BEGIN IF NEW.table_id=" + t
            + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='intentional acceptance failure'; END IF; END");
        int customers = count("customer_master"), bookings = count("booking_master"), links = count("booking_table");
        Result<?> result = controller.create(request(t, "18:00")).getBody();
        assertEquals(500, result.getCode());
        assertEquals(customers, count("customer_master"));
        assertEquals(bookings, count("booking_master"));
        assertEquals(links, count("booking_table"));
        assertEquals("idle", status(t));
    }

    @Test void concurrentReservationsAllowExactlyOneWinner() throws Exception {
        int t = table();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        Callable<Integer> reserve = () -> {
            identity();
            try {
                ready.countDown();
                if (!go.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test start timeout");
                return controller.create(request(t, "18:00")).getBody().getCode();
            } finally { clear(); }
        };
        try {
            Future<Integer> first = executor.submit(reserve), second = executor.submit(reserve);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            List<Integer> results = new ArrayList<>(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)));
            Collections.sort(results);
            assertEquals(List.of(200,409), results);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM booking_table WHERE table_id=?", Integer.class, t));
        } finally { executor.shutdownNow(); }
    }
}
