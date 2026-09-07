package com.youjian.banquet.service;

import com.youjian.banquet.entity.InventorySummary;
import com.youjian.banquet.repository.InventorySummaryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Physical report DDL, no Hibernate schema generation and no destructive rebuild execution. */
@EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_MYSQL",matches="1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventorySummaryMysqlMappingTest {
    JdbcTemplate jdbc;
    LocalContainerEntityManagerFactoryBean factory;
    InventorySummaryRepository repository;
    InventorySummaryService service;
    TransactionTemplate tx;
    List<Map<String,Object>> initialColumns;
    static final String REPORT_DDL = """
        CREATE TABLE `inventory_summary` (
          `summary_id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇总ID',
          `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
          `ingredient_id` varchar(50) NOT NULL COMMENT '食材ID',
          `total_quantity` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总库存数量',
          `total_cost` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总成本金额',
          `avg_unit_price` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '平均单价',
          `last_in_time` timestamp NULL DEFAULT NULL COMMENT '最后入库时间',
          `last_out_time` timestamp NULL DEFAULT NULL COMMENT '最后出库时间',
          `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`summary_id`),
          UNIQUE KEY `uk_store_ingredient` (`store_id`,`ingredient_id`),
          KEY `idx_store_inv_summary` (`store_id`),
          KEY `idx_ingredient_inv_summary` (`ingredient_id`),
          CONSTRAINT `fk_inv_summary_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存汇总表'
        """;

    @BeforeAll void start() {
        String schema="summary_mapping_"+UUID.randomUUID().toString().replace("-","");
        String host="jdbc:mysql://127.0.0.1:13317/";
        var admin=new JdbcTemplate(new DriverManagerDataSource(host+"?useSSL=false&allowPublicKeyRetrieval=true","root",""));
        admin.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        var ds=new DriverManagerDataSource(host+schema+"?useSSL=false&allowPublicKeyRetrieval=true","root","");jdbc=new JdbcTemplate(ds);
        // Minimal synthetic parent for the report's exact store FK, not a claimed full production store_info DDL.
        jdbc.execute("CREATE TABLE store_info(store_id BIGINT NOT NULL PRIMARY KEY,store_name VARCHAR(100) NOT NULL) ENGINE=InnoDB");
        jdbc.execute(REPORT_DDL);
        jdbc.update("INSERT INTO store_info(store_id,store_name) VALUES(91001,'SYNTHETIC-MAPPING-STORE-A'),(91002,'SYNTHETIC-MAPPING-STORE-B')");
        // Fixed synthetic round-trip markers; not derived costs, initialization or a pricing/rebuild policy.
        jdbc.update("""
            INSERT INTO inventory_summary(store_id,ingredient_id,total_quantity,total_cost,avg_unit_price,last_in_time,last_out_time,updated_at)
            VALUES(91001,'SYNTHETIC-MAPPING-SAME-ITEM',7.25,29.00,4.00,'2001-01-02 03:04:05',NULL,'2001-01-02 03:04:05'),
                  (91002,'SYNTHETIC-MAPPING-SAME-ITEM',3.00,15.00,5.00,'2001-01-02 03:04:05',NULL,'2001-01-02 03:04:05')
            """);
        initialColumns=jdbc.queryForList("SHOW COLUMNS FROM inventory_summary");
        System.out.println("SUMMARY_MAPPING_EVIDENCE schema="+schema+" retained=true hibernateDdl=none physicalDDL=TL-OPS-report stores="+jdbc.queryForList("SELECT * FROM store_info ORDER BY store_id")+" initialRows="+rows()+" columns="+initialColumns);
        factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(InventorySummary.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.show_sql","false"));factory.afterPropertiesSet();
        var manager=new JpaTransactionManager(factory.getObject());tx=new TransactionTemplate(manager);
        var repositories=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        repositories.addRepositoryProxyPostProcessor((proxy,info)->proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource())));
        repository=repositories.getRepository(InventorySummaryRepository.class);
        var proxy=new ProxyFactory(new InventorySummaryService(repository));proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        service=(InventorySummaryService)proxy.getProxy();
    }
    @AfterAll void close() { if(factory!=null)factory.destroy(); }
    List<Map<String,Object>> rows() { return jdbc.queryForList("SELECT * FROM inventory_summary ORDER BY summary_id"); }
    @AfterEach void evidence(TestInfo method) {
        assertEquals(initialColumns,jdbc.queryForList("SHOW COLUMNS FROM inventory_summary"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_summary s LEFT JOIN store_info st ON st.store_id=s.store_id WHERE st.store_id IS NULL",Integer.class));
        System.out.println("SUMMARY_MAPPING_EVIDENCE method="+method.getTestMethod().orElseThrow().getName()+" rows="+rows());
    }
    @Test @Order(1) void jpaReadsWritesPhysicalUpdatedAtWithoutChangingDecimalSchema() {
        assertTrue(initialColumns.stream().anyMatch(x->x.get("Field").equals("updated_at")));
        assertFalse(initialColumns.stream().anyMatch(x->x.get("Field").equals("update_time")));
        var first=tx.execute(t->repository.findByStoreIdAndIngredientId(91001L,"SYNTHETIC-MAPPING-SAME-ITEM").orElseThrow());
        var second=tx.execute(t->repository.findByStoreIdAndIngredientId(91002L,"SYNTHETIC-MAPPING-SAME-ITEM").orElseThrow());
        assertNotEquals(first.getSummaryId(),second.getSummaryId());assertEquals(91001L,first.getStoreId());assertEquals(91002L,second.getStoreId());
        assertEquals(new BigDecimal("7.25"),first.getTotalQuantity());assertEquals(new BigDecimal("29.00"),first.getTotalCost());assertEquals(new BigDecimal("4.00"),first.getAvgUnitPrice());
        assertEquals(LocalDateTime.of(2001,1,2,3,4,5),first.getUpdateTime());
        assertEquals(new BigDecimal("3.00"),second.getTotalQuantity());
        tx.executeWithoutResult(t->{var current=repository.findById(first.getSummaryId()).orElseThrow();current.setLastOutTime(LocalDateTime.of(2002,2,3,4,5,6));repository.saveAndFlush(current);});
        var reloaded=tx.execute(t->repository.findById(first.getSummaryId()).orElseThrow());
        var physical=jdbc.queryForMap("SELECT * FROM inventory_summary WHERE summary_id=?",first.getSummaryId());
        assertEquals(LocalDateTime.of(2002,2,3,4,5,6),reloaded.getLastOutTime());
        assertNotEquals(LocalDateTime.of(2001,1,2,3,4,5),reloaded.getUpdateTime());
        assertEquals(reloaded.getUpdateTime(),jdbc.queryForObject("SELECT updated_at FROM inventory_summary WHERE summary_id=?",java.sql.Timestamp.class,first.getSummaryId()).toLocalDateTime());
        assertEquals(first.getTotalQuantity(),physical.get("total_quantity"));assertEquals(first.getTotalCost(),physical.get("total_cost"));assertEquals(first.getAvgUnitPrice(),physical.get("avg_unit_price"));
        assertEquals(second,tx.execute(t->repository.findById(second.getSummaryId()).orElseThrow()));
        Long id=tx.execute(t->{var added=new InventorySummary();added.setStoreId(91001L);added.setIngredientId("SYNTHETIC-JPA-INSERT");added.setTotalQuantity(new BigDecimal("8.50"));added.setTotalCost(new BigDecimal("42.50"));added.setAvgUnitPrice(new BigDecimal("5.00"));return repository.saveAndFlush(added).getSummaryId();});
        var inserted=tx.execute(t->repository.findById(id).orElseThrow());assertNotNull(inserted.getUpdateTime());assertEquals(new BigDecimal("8.50"),inserted.getTotalQuantity());
        assertEquals(new BigDecimal("42.50"),inserted.getTotalCost());assertEquals(new BigDecimal("5.00"),inserted.getAvgUnitPrice());
        assertEquals(3,rows().size());
        for(String column:List.of("total_quantity","total_cost","avg_unit_price")) assertEquals("decimal(12,2)",initialColumns.stream().filter(x->x.get("Field").equals(column)).findFirst().orElseThrow().get("Type"));
    }
    @Test @Order(2) void rebuildExplicitlyRefusesAndEveryStoredColumnRemainsUnchanged() {
        var before=rows();var stores=jdbc.queryForList("SELECT * FROM store_info ORDER BY store_id");assertTrue(before.size()>=2);
        for(int i=0;i<2;i++) {
            var error=assertThrows(IllegalStateException.class,()->service.rebuildAllSummary());
            assertEquals("未配置有来源重建流程，不修改库存汇总",error.getMessage());
            assertEquals(before,rows());assertEquals(stores,jdbc.queryForList("SELECT * FROM store_info ORDER BY store_id"));
        }
        var jpaRows=tx.execute(t->repository.findAll());assertEquals(before.size(),jpaRows.size());
        System.out.println("SUMMARY_REBUILD_REFUSAL_EVIDENCE exactRowsBefore="+before+" exactRowsAfter="+rows()+" repeatedRefusal=2");
    }
}
