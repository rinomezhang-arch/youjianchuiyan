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
}
