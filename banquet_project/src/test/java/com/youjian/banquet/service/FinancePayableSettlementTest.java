package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.repository.FinancePayableRepository;
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
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated MySQL; ORM schema, service boundary only. No HTTP or bank transfer. */
@EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_MYSQL", matches="1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinancePayableSettlementTest {
    JdbcTemplate jdbc;
    FinancePayableService service;
    LocalContainerEntityManagerFactoryBean factory;
    long nextId=100;
    @BeforeAll void setup(){
        String root="jdbc:mysql://127.0.0.1:13317/", opts="?useSSL=false&allowPublicKeyRetrieval=true";
        String schema="payable_settle_"+UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(new DriverManagerDataSource(root+opts,"root","")).execute("CREATE DATABASE "+schema);
        var ds=new DriverManagerDataSource(root+schema+opts,"root",""); jdbc=new JdbcTemplate(ds);
        factory=new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(FinancePayable.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create","hibernate.hbm2ddl.halt_on_error","true"));
        factory.afterPropertiesSet();
        var repos=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        var target=new FinancePayableService();
        ReflectionTestUtils.setField(target,"financePayableRepository",repos.getRepository(FinancePayableRepository.class));
        var proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory.getObject()),new AnnotationTransactionAttributeSource()));
        service=(FinancePayableService)proxy.getProxy();
    }
    void identity(long store){UserContext.set(new UserContext.CurrentUser(store,store,"store_manager","Synthetic operator"));}
    @BeforeEach void identity(){identity(1);}
    @AfterEach void clear(){UserContext.clear();}
    @AfterAll void close(){if(factory!=null)factory.destroy();}
    long seed(){
        long id=nextId++;
        jdbc.update("INSERT INTO finance_payable(payable_id,store_id,payable_no,total_amount,paid_amount,pending_amount,status) VALUES(?,1,?,100,0,100,'unpaid')",id,"SYN-PAY-"+id);
        return id;
    }
    Map<String,Object> row(long id){return jdbc.queryForMap("SELECT * FROM finance_payable WHERE payable_id=?",id);}
    @Test void partialThenFullKeepsAmountsConserved(){
        long id=seed();
        service.settle(id,new BigDecimal("30.25"));
        var partial=row(id);
        assertEquals(new BigDecimal("30.25"),partial.get("paid_amount"));
        assertEquals(new BigDecimal("69.75"),partial.get("pending_amount"));
        assertEquals("partial",partial.get("status"));
        service.settle(id,new BigDecimal("69.75"));
        var full=row(id);
        assertEquals(new BigDecimal("100.00"),full.get("paid_amount"));
        assertEquals(new BigDecimal("0.00"),full.get("pending_amount"));
        assertEquals("paid",full.get("status"));
        assertThrows(IllegalArgumentException.class,()->service.settle(id,BigDecimal.ONE));
        assertEquals(full,row(id));
    }
    @Test void invalidAmountsAndOtherStoreCannotChangeTheRecord(){
        long id=seed();var before=row(id);
        for(var amount:Arrays.asList(null,BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("0.001"),new BigDecimal("101"))) {
            assertThrows(IllegalArgumentException.class,()->service.settle(id,amount));
            assertEquals(before,row(id));
        }
        identity(2);
        assertThrows(IllegalArgumentException.class,()->service.settle(id,BigDecimal.ONE));
        assertEquals(before,row(id));
        UserContext.clear();
        assertThrows(IllegalArgumentException.class,()->service.settle(id,BigDecimal.ONE));
        assertEquals(before,row(id));
    }
    @Test void concurrentSettlementsDoNotLoseEitherAmount() throws Exception {
        long id=seed();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            List<Future<?>> jobs=new ArrayList<>();
            for(String amount:List.of("30","40")) jobs.add(pool.submit(()->{
                identity(1);
                try {start.await();service.settle(id,new BigDecimal(amount));}
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
                finally {UserContext.clear();}
            }));
            start.countDown();for(var job:jobs)job.get(20,TimeUnit.SECONDS);
            var after=row(id);
            assertEquals(new BigDecimal("70.00"),after.get("paid_amount"));
            assertEquals(new BigDecimal("30.00"),after.get("pending_amount"));
            assertEquals("partial",after.get("status"));
        } finally {pool.shutdownNow();}
    }
}
