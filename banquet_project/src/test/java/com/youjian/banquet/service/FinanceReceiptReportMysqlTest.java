package com.youjian.banquet.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Query/physical schema contract only; receipts are synthetic fixtures, not a full procurement workflow. */
class FinanceReceiptReportMysqlTest {
    @Test void receiptReportUsesCapturedPhysicalTableAndExactPeriodStoreBounds() {
        String base="jdbc:mysql://127.0.0.1:13317/", opts="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String schema="report_receipt_"+UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(new DriverManagerDataSource(base+opts,"root", "")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var ds=new DriverManagerDataSource(base+schema+opts,"root", "");
        try {
            new ResourceDatabasePopulator(new ClassPathResource("restaurant-production-schema-20260906.sql")).execute(ds);
            var jdbc=new JdbcTemplate(ds);
            jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-R1','合成一'),(2,'SYN-R2','合成二')");
            Object[][] receipts={{1,"SYN-A","2026-09-01","100.01"},{1,"SYN-B","2026-09-30","20.02"},{2,"SYN-C","2026-09-15","40.04"},{1,"SYN-BEFORE","2026-08-31","900.00"},{1,"SYN-AFTER","2026-10-01","800.00"}};
            for(Object[] row:receipts) jdbc.update("INSERT INTO purchase_receipt(store_id,receipt_no,receipt_date,total_amount,status) VALUES(?,?,?,?, 'ACCEPTED')",row);
            var service=new FinanceReportService();ReflectionTestUtils.setField(service,"jdbc",jdbc);
            LocalDate start=LocalDate.of(2026,9,1), end=LocalDate.of(2026,10,1);
            assertEquals(0,new BigDecimal("120.03").compareTo(service.receiptAmount(1L,start,end)));
            assertEquals(0,new BigDecimal("40.04").compareTo(service.receiptAmount(2L,start,end)));
            assertEquals(0,new BigDecimal("160.07").compareTo(service.receiptAmount(null,start,end)));
            assertEquals(0,service.receiptAmount(1L,LocalDate.of(2026,11,1),LocalDate.of(2026,12,1)).signum());
            assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM purchase_receipt",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM purchase_receipt r LEFT JOIN store_info s ON s.store_id=r.store_id WHERE s.store_id IS NULL",Integer.class));
        } finally { System.out.println("SYNTHETIC_SCHEMA_RETAINED="+schema); }
    }
}
