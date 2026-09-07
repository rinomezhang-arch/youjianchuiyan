package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
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
import org.springframework.mock.web.MockHttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real order repositories and transaction writes; synthetic device attributes, not device authentication. */
@EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_MYSQL", matches="1")
class IpadReceiptLinkMysqlTest {
    JdbcTemplate jdbc;
    LocalContainerEntityManagerFactoryBean factory;
    IpadCheckoutController checkout;
    @BeforeEach void setup() {
        String schema="checkout_link_"+UUID.randomUUID().toString().replace("-", "");
        String host="jdbc:mysql://127.0.0.1:13317/", opts="?useSSL=false&allowPublicKeyRetrieval=true";
        new JdbcTemplate(new DriverManagerDataSource(host+opts,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var ds=new DriverManagerDataSource(host+schema+opts,"root","");
        jdbc=new JdbcTemplate(ds);
        factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(BookingMaster.class.getName(),BookingTable.class.getName(),
            BookingDishDetail.class.getName(),TableMaster.class.getName(),DishMaster.class.getName(),
            FinanceTransaction.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","update","hibernate.show_sql","false"));
        factory.afterPropertiesSet();
        var repositories=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        var target=new IpadCheckoutController(jdbc,repositories.getRepository(BookingMasterRepository.class),
            repositories.getRepository(BookingTableRepository.class),repositories.getRepository(BookingDishDetailRepository.class),repositories.getRepository(DishMasterRepository.class));
        var proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory.getObject()),new AnnotationTransactionAttributeSource()));
        checkout=(IpadCheckoutController)proxy.getProxy();
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource("../scripts/migrations/ipad_checkout_migration_v1.sql")).execute(ds);
        jdbc.update("INSERT INTO table_master(table_id,store_id,table_name,table_number,table_status,is_active,sort_order) VALUES(1,1,'合成桌台','S1','idle',1,1)");
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES('SYNTHETIC-DISH',1,'合成菜品',80.50,1)");
        System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema);
    }
    @AfterEach void close(){if(factory!=null)factory.destroy();}
    MockHttpServletRequest device(long store){var r=new MockHttpServletRequest();r.setAttribute("ipad_store_id",store);r.setAttribute("ipad_staff_id",1L);return r;}
    String order(){
        var res=checkout.submit(Map.of("table_id",1,"guest_count",2,"dishes",List.of(Map.of("dish_id","SYNTHETIC-DISH","dish_quantity",2))),device(1));
        assertEquals(200,res.getCode());return (String)res.getData().get("booking_id");
    }
    Map<String,Object> payment(String id){return Map.of("booking_id",id,"pay_type","cash","pay_amount","200.00");}
    @Test void orderPaymentLedgerReferencesActualPrimaryKeyAndBusinessNumber() {
        String id=order(),key=UUID.randomUUID().toString();
        assertEquals(new BigDecimal("161.00"),checkout.bill(id,device(1)).getData().get("total_amount"));
        var res=checkout.pay(payment(id),key,device(1));
        assertEquals(new BigDecimal("39.00"),res.getData().get("change_amount"));
        var row=jdbc.queryForMap("SELECT t.amount,t.related_no,b.id FROM finance_transaction t JOIN booking_master b ON t.related_id=b.id AND t.store_id=b.store_id AND t.related_no=b.booking_id WHERE t.related_type='booking'");
        assertEquals(id,row.get("related_no"));assertEquals(new BigDecimal("161.00"),row.get("amount"));
        assertEquals("idle",jdbc.queryForObject("SELECT table_status FROM table_master WHERE table_id=1",String.class));
        var before=jdbc.queryForList("SELECT * FROM finance_transaction");
        checkout.pay(payment(id),key,device(1));
        assertEquals(before,jdbc.queryForList("SELECT * FROM finance_transaction"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_payment_request",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_transaction t LEFT JOIN booking_master b ON t.related_id=b.id AND t.store_id=b.store_id WHERE t.related_type='booking' AND b.id IS NULL",Integer.class));
        assertThrows(IllegalArgumentException.class,()->checkout.pay(payment("ANOTHER-ORDER"),key,device(1)));
    }
    @Test void cancelledOrderAndOtherStoreCannotProduceRevenue() {
        String id=order();
        assertThrows(IllegalArgumentException.class,()->checkout.pay(payment(id),UUID.randomUUID().toString(),device(2)));
        jdbc.update("UPDATE booking_master SET booking_status='cancelled' WHERE booking_id=?",id);
        assertThrows(IllegalStateException.class,()->checkout.pay(payment(id),UUID.randomUUID().toString(),device(1)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_transaction",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_payment_request",Integer.class));
    }
    @Test void revenueInsertFailureRollsBackPaymentAndTableRelease() {
        String id=order();
        jdbc.execute("ALTER TABLE finance_transaction ADD CONSTRAINT synthetic_fail_revenue CHECK(amount<0)");
        assertThrows(RuntimeException.class,()->checkout.pay(payment(id),UUID.randomUUID().toString(),device(1)));
        assertEquals("unpaid",jdbc.queryForObject("SELECT payment_status FROM booking_master WHERE booking_id=?",String.class,id));
        assertEquals("occupied",jdbc.queryForObject("SELECT table_status FROM table_master WHERE table_id=1",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_transaction",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ipad_payment_request",Integer.class));
    }
}
