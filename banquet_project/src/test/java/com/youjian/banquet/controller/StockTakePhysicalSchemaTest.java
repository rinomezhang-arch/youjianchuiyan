package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Inherits the 14 controller flow cases, using current captured physical DDL and an explicit migration.
 * Synthetic data, staff parent stub, no production writes and no destructive cleanup. */
class StockTakePhysicalSchemaTest extends StockTakePersistenceTest {
    @Override @BeforeAll void setup() {
        String schema="stock_take_physical_"+UUID.randomUUID().toString().replace("-","");
        String root="jdbc:mysql://127.0.0.1:13317/", options="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        new JdbcTemplate(new DriverManagerDataSource(root+options,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        System.out.println("STOCKTAKE_PHYSICAL_SCHEMA_RETAINED="+schema);
        var ds=new DriverManagerDataSource(root+schema+options,"root","");jdbc=new JdbcTemplate(ds);
        new ResourceDatabasePopulator(new ClassPathResource("restaurant-production-schema-20260906.sql"),
                new ClassPathResource("stocktake-current-schema-20260907.sql")).execute(ds);
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-1','Synthetic 1'),(2,'SYN-2','Synthetic 2')");
        jdbc.update("INSERT INTO staff_master(staff_id) VALUES(1),(2),(99)"); // Includes the inherited SYN-GM identity.
        jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,current_stock,unit_price) VALUES('SYN-ST',1,'Synthetic count',10,2),('SYN-OTHER',2,'Other store',20,3),('123',1,'Synthetic historical parent',0,1)");
        // Historical synthetic sentinel exercises the old full integer capacity, zero/NULL,
        // and identifier preservation without reading or transforming real business records.
        jdbc.update("INSERT INTO stock_take(take_id,store_id,take_no,take_date) VALUES(900001,1,'SYN-HISTORICAL','2001-01-01')");
        jdbc.update("INSERT INTO stock_take_detail(take_id,store_id,line_no,ingredient_id,ingredient_name,system_quantity,actual_quantity,diff_quantity,unit_price,system_amount,actual_amount,diff_amount) VALUES(900001,1,1,123,'SYN-HISTORY',99999999.99,0,-99999999.99,99999999.99,25.12,NULL,-25.12)");
        var before=jdbc.queryForMap("SELECT * FROM stock_take_detail WHERE take_id=900001");
        var relationships=jdbc.queryForList("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('stock_take','stock_take_detail') AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION");
        var migration=new ResourceDatabasePopulator(new FileSystemResource("../scripts/migrations/stocktake_precision_v1.sql"));
        migration.execute(ds);
        migration.execute(ds); // Retrying the same widening statement is non-destructive.
        var after=jdbc.queryForMap("SELECT * FROM stock_take_detail WHERE take_id=900001");
        for(String column:before.keySet()) {
            if(column.equals("ingredient_id")) assertEquals(before.get(column).toString(),after.get(column));
            else if(before.get(column) instanceof BigDecimal amount)
                assertEquals(0,amount.compareTo((BigDecimal)after.get(column)),column);
            else assertEquals(before.get(column),after.get(column),column);
        }
        assertEquals("decimal(16,8)",jdbc.queryForMap("SHOW COLUMNS FROM stock_take_detail LIKE 'unit_price'").get("Type"));
        assertEquals("decimal(12,3)",jdbc.queryForMap("SHOW COLUMNS FROM stock_take_detail LIKE 'actual_quantity'").get("Type"));
        assertEquals(relationships,jdbc.queryForList("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('stock_take','stock_take_detail') AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION"));
        assertEquals(5,relationships.size(),"Two staff FKs, two ingredient FK columns and one master FK remain");
        assertEquals("NULL",jdbc.queryForMap("SHOW COLUMNS FROM stock_take_detail LIKE 'ingredient_id'").get("Default"),"Do not silently change unrelated legacy default");
        factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(IngredientMaster.class.getName(),StockTake.class.getName(),StockTakeDetail.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));factory.afterPropertiesSet();
        var repos=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        var target=new StockTakeController();
        ReflectionTestUtils.setField(target,"stockTakeRepo",repos.getRepository(StockTakeRepository.class));
        ReflectionTestUtils.setField(target,"stockTakeDetailRepo",repos.getRepository(StockTakeDetailRepository.class));
        ReflectionTestUtils.setField(target,"ingredientRepo",repos.getRepository(IngredientMasterRepository.class));
        var proxy=new ProxyFactory(target);proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory.getObject()),new AnnotationTransactionAttributeSource()));
        controller=(StockTakeController)proxy.getProxy();
    }

    @AfterEach void verifyAllPersistedRelationships() {
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take_detail d LEFT JOIN stock_take s ON s.take_id=d.take_id AND s.store_id=d.store_id LEFT JOIN ingredient_master i ON i.ingredient_id=d.ingredient_id AND i.store_id=d.store_id WHERE s.take_id IS NULL OR i.ingredient_id IS NULL",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM stock_take s LEFT JOIN store_info st ON st.store_id=s.store_id WHERE st.store_id IS NULL",Integer.class));
    }
}
