package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
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
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated MySQL persistence; ORM schema only, not production-schema or browser acceptance. */
@EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_MYSQL", matches="1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StockTakePersistenceTest {
    JdbcTemplate jdbc;
    StockTakeController controller;
    LocalContainerEntityManagerFactoryBean factory;
    @BeforeAll void setup() {
        String root="jdbc:mysql://127.0.0.1:13317/", opts="?useSSL=false&allowPublicKeyRetrieval=true";
        String schema="stock_take_"+UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(new DriverManagerDataSource(root+opts,"root","")).execute("CREATE DATABASE "+schema);
        var ds=new DriverManagerDataSource(root+schema+opts,"root","");
        jdbc=new JdbcTemplate(ds);
        factory=new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(IngredientMaster.class.getName(),StockTake.class.getName(),StockTakeDetail.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create","hibernate.hbm2ddl.halt_on_error","true"));
        factory.afterPropertiesSet();
        var repos=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        var target=new StockTakeController();
        ReflectionTestUtils.setField(target,"stockTakeRepo",repos.getRepository(StockTakeRepository.class));
        ReflectionTestUtils.setField(target,"stockTakeDetailRepo",repos.getRepository(StockTakeDetailRepository.class));
        ReflectionTestUtils.setField(target,"ingredientRepo",repos.getRepository(IngredientMasterRepository.class));
        var proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory.getObject()),new AnnotationTransactionAttributeSource()));
        controller=(StockTakeController)proxy.getProxy();
        jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,current_stock,unit_price) VALUES('SYN-ST',1,'Synthetic count',10,2),('SYN-OTHER',2,'Other store',20,3)");
    }
    @BeforeEach void identity(){UserContext.set(new UserContext.CurrentUser(1L,1L,"store_manager","Synthetic operator"));}
    @AfterEach void clear(){UserContext.clear();}
    @AfterAll void close(){if(factory!=null)factory.destroy();}
    Map<String,Object> body(List<Map<String,Object>> items){return Map.of("storeId",1,"takeDate","2026-09-07","items",items);}
    Map<String,Object> item(String id,Object qty){return Map.of("ingredientId",id,"actualQuantity",qty);}
    int count(){return jdbc.queryForObject("SELECT COUNT(*) FROM stock_take",Integer.class);}
    @Test void quantityBeyondStoragePrecisionCannotSilentlyRoundOrPartiallyPersist() {
        int before=count();
        int details=jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class);
        for(String invalid:List.of("8.0004","0.0001","1000000000","999999999.9991")) {
            var result=controller.createStockTake(body(List.of(item("SYN-ST",invalid))));
            assertEquals(400,result.getCode(),"Rejected before persistence: "+invalid);
        }
        assertEquals(before,count());
        assertEquals(details,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class));
    }
    @Test void acceptedQuantityKeepsExactStoredDifferenceAndOriginalStock() {
        for(String amount:List.of("8.001000","999999999.999")) {
            var result=controller.createStockTake(body(List.of(item("SYN-ST",amount))));
            assertEquals(200,result.getCode(),result.getMessage());
            Long id=result.getData().getTakeId();
            BigDecimal actual=jdbc.queryForObject("SELECT actual_quantity FROM stock_take_detail WHERE take_id=?",BigDecimal.class,id);
            BigDecimal difference=jdbc.queryForObject("SELECT diff_quantity FROM stock_take_detail WHERE take_id=?",BigDecimal.class,id);
            assertEquals(0,new BigDecimal(amount).compareTo(actual));
            assertEquals(0,actual.subtract(new BigDecimal("10")).compareTo(difference));
            assertEquals(0,new BigDecimal("10").compareTo(jdbc.queryForObject("SELECT current_stock FROM ingredient_master WHERE ingredient_id='SYN-ST' AND store_id=1",BigDecimal.class)));
            assertEquals(200,controller.getStockTake(id).getCode());
        }
    }
    @Test void rejectsMissingIdentityAndExplicitForeignStoreWithoutWriting() {
        int before=count();
        var input=new HashMap<String,Object>(body(List.of(item("SYN-ST",8))));
        input.put("storeId",2);
        assertEquals(403,controller.createStockTake(input).getCode());
        UserContext.clear();
        assertEquals(403,controller.createStockTake(body(List.of(item("SYN-ST",8)))).getCode());
        assertEquals(before,count());
    }
    @Test void gmMustChooseStoreAndExplicitStoreLinksEveryDetail() {
        int before=count();
        UserContext.set(new UserContext.CurrentUser(99L,0L,"gm","Synthetic GM"));
        var input=new HashMap<String,Object>(body(List.of(item("SYN-OTHER",19))));
        input.remove("storeId");
        assertEquals(400,controller.createStockTake(input).getCode());
        input.put("storeId",0);
        assertEquals(400,controller.createStockTake(input).getCode());
        assertEquals(before,count());
        input.put("storeId",2);
        var created=controller.createStockTake(input);
        assertEquals(200,created.getCode(),created.getMessage());
        assertEquals(2L,created.getData().getStoreId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail d JOIN stock_take s ON s.take_id=d.take_id AND s.store_id=d.store_id JOIN ingredient_master i ON i.ingredient_id=d.ingredient_id AND i.store_id=d.store_id WHERE s.take_id=? AND s.store_id=2",Integer.class,created.getData().getTakeId()));
    }
    @Test void malformedDateStoreAndDetailShapesAreInputErrorsAndLeaveNoPartialRows() {
        int before=count();
        int details=jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class);
        for(Object invalid:List.of("2026-02-30","not-a-date",123)) {
            var input=new HashMap<String,Object>(body(List.of(item("SYN-ST",8))));
            input.put("takeDate",invalid);
            assertEquals(400,controller.createStockTake(input).getCode());
        }
        for(Object invalid:List.of("bad",-1,1.5)) {
            var input=new HashMap<String,Object>(body(List.of(item("SYN-ST",8))));
            input.put("storeId",invalid);
            assertEquals(400,controller.createStockTake(input).getCode());
        }
        for(Object invalid:List.of("bad",Map.of("ingredientId","SYN-ST"),List.of("bad"))) {
            var input=new HashMap<String,Object>(body(List.of(item("SYN-ST",8))));
            input.put("items",invalid);
            assertEquals(400,controller.createStockTake(input).getCode());
        }
        assertEquals(before,count());
        assertEquals(details,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class));
    }
    @Test void persistsMasterDetailsAndReadsBackWithoutChangingInventory(){
        var result=controller.createStockTake(body(List.of(item("SYN-ST",8))));
        assertEquals(200,result.getCode(),result.getMessage());
        Long id=result.getData().getTakeId();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail WHERE take_id=? AND store_id=1",Integer.class,id));
        assertEquals(0,new BigDecimal("-4").compareTo(jdbc.queryForObject("SELECT diff_amount FROM stock_take_detail WHERE take_id=?",BigDecimal.class,id)));
        assertEquals(0,new BigDecimal("10").compareTo(jdbc.queryForObject("SELECT current_stock FROM ingredient_master WHERE ingredient_id='SYN-ST'",BigDecimal.class)));
        var read=controller.getStockTake(id);
        assertEquals(200,read.getCode());
        assertEquals(1,((List<?>)read.getData().get("details")).size());
        assertEquals(1,controller.listStockTakeDetails(id).getData().size());
        assertEquals(404,controller.listStockTakeDetails(Long.MAX_VALUE).getCode());
        assertTrue(controller.listStockTakes(1L,null).getData().stream().anyMatch(t->t.getTakeId().equals(id)));
        UserContext.set(new UserContext.CurrentUser(2L,2L,"store_manager","Other operator"));
        assertEquals(403,controller.getStockTake(id).getCode());
        assertEquals(403,controller.listStockTakeDetails(id).getCode());
    }
    @Test void completedHistoryRejectsMutationAndRetainsMasterDetailsAndInventory(){
        var created=controller.createStockTake(body(List.of(item("SYN-ST",8))));
        assertEquals(200,created.getCode(),created.getMessage());
        Long id=created.getData().getTakeId();
        var before=jdbc.queryForMap("SELECT * FROM stock_take WHERE take_id=?",id);
        var details=jdbc.queryForList("SELECT * FROM stock_take_detail WHERE take_id=? ORDER BY detail_id",id);
        var inventory=jdbc.queryForList("SELECT * FROM ingredient_master ORDER BY ingredient_id,store_id");
        StockTake patch=new StockTake();
        patch.setTotalDiffAmount(new BigDecimal("999"));
        patch.setStatus("draft");
        assertEquals(409,controller.updateStockTake(id,patch).getCode());
        assertEquals(409,controller.deleteStockTake(id).getCode());
        assertEquals(409,controller.addStockTakeDetail(id,new StockTakeDetail()).getCode());
        UserContext.set(new UserContext.CurrentUser(2L,2L,"store_manager","Other operator"));
        assertEquals(403,controller.updateStockTake(id,patch).getCode());
        assertEquals(403,controller.deleteStockTake(id).getCode());
        assertEquals(403,controller.addStockTakeDetail(id,new StockTakeDetail()).getCode());
        assertEquals(before,jdbc.queryForMap("SELECT * FROM stock_take WHERE take_id=?",id));
        assertEquals(details,jdbc.queryForList("SELECT * FROM stock_take_detail WHERE take_id=? ORDER BY detail_id",id));
        assertEquals(inventory,jdbc.queryForList("SELECT * FROM ingredient_master ORDER BY ingredient_id,store_id"));
    }
    @Test void invalidLinesCannotSilentlyCreatePartialDocuments(){
        int before=count();
        for(var bad:List.of(item("missing",1),item("SYN-OTHER",1),item("SYN-ST",-1),item("SYN-ST","bad"),Map.<String,Object>of("ingredientId","SYN-ST"))){
            assertEquals(400,controller.createStockTake(body(List.of(bad))).getCode());
            assertEquals(before,count());
        }
        assertEquals(400,controller.createStockTake(body(List.of(item("SYN-ST",8),item("missing",1)))).getCode());
        assertEquals(400,controller.createStockTake(body(List.of(item("SYN-ST",8),item("SYN-ST",9)))).getCode());
        assertEquals(before,count());
    }
    @Test void detailStorageFailureRollsBackMasterAndEarlierDetail(){
        jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,current_stock,unit_price) VALUES('SYN-FAIL',1,'Synthetic rollback',10,2)");
        jdbc.execute("CREATE TRIGGER synthetic_detail_failure BEFORE INSERT ON stock_take_detail FOR EACH ROW BEGIN IF NEW.ingredient_id = 'SYN-FAIL' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic detail storage failure'; END IF; END");
        int before=count();
        int details=jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class);
        var result=controller.createStockTake(body(List.of(item("SYN-ST",8),item("SYN-FAIL",9))));
        assertEquals(500,result.getCode());
        assertEquals(before,count(),"Failed detail must roll back its master");
        assertEquals(details,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail",Integer.class),"Earlier detail must also roll back");
    }
    @Test void countPreservesThreeDecimalQuantityAndEightDecimalPrice(){
        jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,current_stock,unit_price) VALUES('SYN-PREC',1,'Synthetic precision',10.001,0.05706667)");
        var result=controller.createStockTake(body(List.of(item("SYN-PREC","8.123"))));
        assertEquals(200,result.getCode(),result.getMessage());
        Long id=result.getData().getTakeId();
        var detail=controller.listStockTakeDetails(id).getData().get(0);
        assertEquals(new BigDecimal("10.001"),detail.getSystemQuantity());
        assertEquals(new BigDecimal("8.123"),detail.getActualQuantity());
        assertEquals(new BigDecimal("-1.878"),detail.getDiffQuantity());
        assertEquals(new BigDecimal("0.05706667"),detail.getUnitPrice());
        assertEquals(new BigDecimal("-0.11"),detail.getDiffAmount());
    }

    @Test void threeSurplusLinesSumPersistedCentsInsteadOfRoundingUnroundedTotal(){
        assertThreeLineRounding("SYN-ROUND-P333", "0.333", "11", "0.33", "0.99", "surplus");
    }

    @Test void threeShortageLinesSumPersistedCentsInsteadOfRoundingUnroundedTotal(){
        assertThreeLineRounding("SYN-ROUND-N333", "0.333", "9", "-0.33", "-0.99", "shortage");
    }

    @Test void surplusHalfCentRoundsUpBeforeSummingThreeLines(){
        assertThreeLineRounding("SYN-ROUND-P335", "0.335", "11", "0.34", "1.02", "surplus");
    }

    @Test void shortageHalfCentRoundsAwayFromZeroBeforeSummingThreeLines(){
        assertThreeLineRounding("SYN-ROUND-N335", "0.335", "9", "-0.34", "-1.02", "shortage");
    }

    private void assertThreeLineRounding(String prefix, String price, String actual,
                                        String lineAmount, String totalAmount, String diffType){
        List<Map<String,Object>> items=new ArrayList<>();
        for(int line=1;line<=3;line++){
            String ingredientId=prefix+"-"+line;
            jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,current_stock,unit_price) VALUES(?,1,?,10,?)",
                    ingredientId,"Synthetic rounding "+ingredientId,new BigDecimal(price));
            // Only ingredientId/actualQuantity are authoritative client input.
            items.add(Map.of("ingredientId",ingredientId,"actualQuantity",actual,
                    "systemQuantity","999","unitPrice","999","diffAmount","999"));
        }
        var ingredientsBefore=jdbc.queryForList("SELECT * FROM ingredient_master ORDER BY ingredient_id,store_id");
        Map<String,Object> request=new HashMap<>(body(items));
        request.put("remark",prefix);
        request.put("total", "99999.99");
        request.put("totalDiffAmount", "99999.99");
        request.put("totalItems",999);
        request.put("totalDiffItems",999);
        var created=controller.createStockTake(request);
        assertEquals(200,created.getCode(),created.getMessage());
        Long takeId=created.getData().getTakeId();
        assertNotNull(takeId);
        BigDecimal expectedTotal=new BigDecimal(totalAmount);
        assertEquals(expectedTotal,created.getData().getTotalDiffAmount(),"POST response must already contain rounded line sum");
        assertEquals(3,created.getData().getTotalItems());
        assertEquals(3,created.getData().getTotalDiffItems());

        // JDBC reads persisted MySQL values after the proxied transaction completes,
        // independently of the POST response or a JPA first-level cached entity.
        BigDecimal storedTotal=jdbc.queryForObject("SELECT total_diff_amount FROM stock_take WHERE take_id=?",BigDecimal.class,takeId);
        BigDecimal detailSum=jdbc.queryForObject("SELECT SUM(diff_amount) FROM stock_take_detail WHERE take_id=?",BigDecimal.class,takeId);
        assertEquals(expectedTotal,storedTotal,"Persisted master total: "+prefix);
        assertEquals(0,storedTotal.compareTo(detailSum),"Persisted master must equal SQL SUM(details): "+prefix);
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail WHERE take_id=? AND store_id=1",Integer.class,takeId));
        var storedAmounts=jdbc.queryForList("SELECT diff_amount FROM stock_take_detail WHERE take_id=? ORDER BY line_no",BigDecimal.class,takeId);
        assertEquals(List.of(new BigDecimal(lineAmount),new BigDecimal(lineAmount),new BigDecimal(lineAmount)),storedAmounts);

        var read=controller.getStockTake(takeId);
        assertEquals(200,read.getCode(),read.getMessage());
        assertEquals(expectedTotal,((StockTake)read.getData().get("stockTake")).getTotalDiffAmount());
        var detailsResponse=controller.listStockTakeDetails(takeId);
        assertEquals(200,detailsResponse.getCode(),detailsResponse.getMessage());
        var details=detailsResponse.getData();
        assertEquals(3,details.size());
        Set<String> expectedIds=Set.of(prefix+"-1",prefix+"-2",prefix+"-3");
        Set<String> actualIds=new HashSet<>();
        for(var detail:details){
            assertEquals(takeId,detail.getTakeId());
            assertEquals(1L,detail.getStoreId());
            actualIds.add(detail.getIngredientId());
            assertEquals(new BigDecimal(lineAmount),detail.getDiffAmount());
            assertEquals(diffType,detail.getDiffType());
            assertEquals(0,new BigDecimal(price).compareTo(detail.getUnitPrice()));
            assertEquals(0,new BigDecimal("10").compareTo(detail.getSystemQuantity()));
            assertEquals(0,new BigDecimal(actual).compareTo(detail.getActualQuantity()));
        }
        assertEquals(expectedIds,actualIds,"All synthetic ingredients must be linked exactly once");
        assertEquals(ingredientsBefore,jdbc.queryForList("SELECT * FROM ingredient_master ORDER BY ingredient_id,store_id"),
                "Recording rounded amounts must not change inventory or source prices");
    }
}
