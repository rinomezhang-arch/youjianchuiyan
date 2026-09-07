package com.youjian.banquet.controller;

import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** v2 migration plus all inherited core scenarios. Original v1 suite/negative evidence stays frozen. */
class IpadBatchScopeFkMysqlTest extends IpadBatchIdempotencyMysqlTest {
    List<Map<String,Object>> historicalReceipts;
    List<Map<String,Object>> historicalDetails;
    List<Map<String,Object>> historicalParents;
    String parentDdlBefore;
    String sourceSchema;
    List<String> migration;

    @BeforeAll void migrateNonemptyTwoStoreHistory() throws Exception {
        // JUnit runs the inherited start() first: real explicit-entity JPA, physical v1 receipt table.
        identity();
        var one=fixture(1);var two=fixture(2);
        assertChain(one,success(submit(one,key())));assertChain(two,success(submit(two,key())));
        historicalReceipts=jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id");
        historicalDetails=jdbc.queryForList("SELECT * FROM booking_dish_detail ORDER BY dish_booking_id");
        historicalParents=jdbc.queryForList("SELECT * FROM booking_master ORDER BY id");
        assertEquals(2,historicalReceipts.size());assertEquals(4,historicalDetails.size());assertEquals(2,historicalParents.size());
        sourceSchema=jdbc.queryForObject("SELECT DATABASE()",String.class);assertTrue(sourceSchema.matches("ipad_batch_[a-f0-9]{32}"));
        parentDdlBefore=jdbc.queryForMap("SHOW CREATE TABLE booking_master").get("Create Table").toString();
        String script;
        try(var in=new ClassPathResource("ipad_batch_request_migration_v2.sql").getInputStream()){script=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        migration=Arrays.stream(script.lines().filter(line->!line.stripLeading().startsWith("--")).reduce("",(a,b)->a+"\n"+b).split(";")).map(String::trim).filter(s->!s.isEmpty()).toList();
        assertEquals(2,migration.size());
        assertCompatibleColumns(jdbc);assertEquals(0,scopeMismatches(jdbc));assertEquals(0,scopeIndexCount(jdbc));
        jdbc.execute(migration.get(0));jdbc.execute(migration.get(1));
        verifyInstalled(jdbc);
        assertEquals(historicalReceipts,jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
        assertEquals(historicalDetails,jdbc.queryForList("SELECT * FROM booking_dish_detail ORDER BY dish_booking_id"));
        assertEquals(historicalParents,jdbc.queryForList("SELECT * FROM booking_master ORDER BY id"));
        System.out.println("SCOPE_FK_MIGRATED schema="+sourceSchema+" history_receipts="+historicalReceipts+" history_detail_count=4 unchanged=true ddl="+jdbc.queryForMap("SHOW CREATE TABLE ipad_batch_request"));
    }
    int scopeMismatches(JdbcTemplate db) {
        return db.queryForObject("SELECT COUNT(*) FROM ipad_batch_request r LEFT JOIN booking_master b ON b.id=r.booking_master_id AND b.store_id=r.store_id AND b.booking_id=r.booking_id WHERE b.id IS NULL",Integer.class);
    }
    List<Map<String,Object>> columns(JdbcTemplate db,String table,String name) {
        return db.queryForList("SELECT COLUMN_TYPE,CHARACTER_SET_NAME,COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?",table,name);
    }
    void assertCompatibleColumns(JdbcTemplate db) {
        assertEquals(columns(db,"booking_master","id"),columns(db,"ipad_batch_request","booking_master_id"));
        assertEquals(columns(db,"booking_master","store_id"),columns(db,"ipad_batch_request","store_id"));
        assertEquals(columns(db,"booking_master","booking_id"),columns(db,"ipad_batch_request","booking_id"));
    }
    int scopeIndexCount(JdbcTemplate db) {
        return db.queryForObject("SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_master' AND NON_UNIQUE=0 GROUP BY INDEX_NAME HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)='id,store_id,booking_id' AND COUNT(*)=3 AND SUM(SUB_PART IS NOT NULL)=0) x",Integer.class);
    }
    int scopeFkColumns(JdbcTemplate db) {
        return db.queryForObject("SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='ipad_batch_request' AND CONSTRAINT_NAME='fk_ipad_batch_booking_scope'",Integer.class);
    }
    void verifyInstalled(JdbcTemplate db) {
        assertEquals(1,scopeIndexCount(db));
        assertEquals(List.of("booking_master_id:id","store_id:store_id","booking_id:booking_id"),db.queryForList("SELECT CONCAT(COLUMN_NAME,':',REFERENCED_COLUMN_NAME) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='ipad_batch_request' AND CONSTRAINT_NAME='fk_ipad_batch_booking_scope' AND REFERENCED_TABLE_NAME='booking_master' AND REFERENCED_TABLE_SCHEMA=DATABASE() ORDER BY ORDINAL_POSITION",String.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='ipad_batch_request' AND CONSTRAINT_NAME='fk_ipad_batch_booking' AND COLUMN_NAME='booking_master_id' AND REFERENCED_TABLE_NAME='booking_master' AND REFERENCED_COLUMN_NAME='id'",Integer.class));
        assertEquals(1,db.queryForObject("SELECT @@SESSION.foreign_key_checks",Integer.class));
        assertEquals(0,scopeMismatches(db));
    }
    int mysqlCode(Throwable ex) {for(Throwable x=ex;x!=null;x=x.getCause())if(x instanceof java.sql.SQLException s)return s.getErrorCode();return -1;}

    @Override @Test void singleColumnForeignKeyRejectsMissingMasterButDoesNotEnforceStoreOrBookingCopies() {
        // Same inherited scenario name for traceability; v2 now must REJECT each formerly accepted copy.
        var f=fixture(1);String originalKey=key();success(submit(f,originalKey));
        var before=jdbc.queryForList("SELECT * FROM ipad_batch_request WHERE client_request_id=?",originalKey);
        for(String values:List.of("2,booking_master_id,booking_id","store_id,booking_master_id,'SYNTHETIC-WRONG-BOOKING'","store_id,9223372036854775807,booking_id")) {
            var rejected=assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) SELECT "+values+",?,payload_sha256,operator_id,result_json FROM ipad_batch_request WHERE client_request_id=?",key(),originalKey));
            assertEquals(1452,mysqlCode(rejected));
        }
        assertEquals(before,jdbc.queryForList("SELECT * FROM ipad_batch_request WHERE client_request_id=?",originalKey));assertEquals(1,receipts(f));assertEquals(2,rows(f));verifyInstalled(jdbc);
        System.out.println("SCOPE_FK_NEGATIVE wrong_store=1452 wrong_booking=1452 missing_master=1452 original_receipt_unchanged=true");
    }
    @Test void installedMigrationStateCanBeVerifiedAndSkippedWithoutRewritingHistory() {
        // Documented retry: verify metadata, skip both installed statements (SQL file is not idempotent).
        var before=jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id");
        int plannedSteps=0;
        if(scopeIndexCount(jdbc)==0)plannedSteps++;
        if(scopeFkColumns(jdbc)==0)plannedSteps++;
        assertEquals(0,plannedSteps);verifyInstalled(jdbc);
        for(var receipt:historicalReceipts)assertEquals(receipt,jdbc.queryForMap("SELECT * FROM ipad_batch_request WHERE request_id=?",receipt.get("request_id")));
        assertEquals(before,jdbc.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
    }
    JdbcTemplate physicalCopy(String purpose,boolean incompatibleCollation) throws Exception {
        String schema="batch_fk_"+purpose+"_"+UUID.randomUUID().toString().replace("-","");
        String host="jdbc:mysql://127.0.0.1:13317/",options="?useSSL=false&allowPublicKeyRetrieval=true";
        var admin=new JdbcTemplate(new DriverManagerDataSource(host+options,"root",""));admin.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var db=new JdbcTemplate(new DriverManagerDataSource(host+schema+options,"root",""));db.execute(parentDdlBefore);
        String v1;try(var in=new ClassPathResource("ipad_batch_request_migration_v1.sql").getInputStream()){v1=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        if(incompatibleCollation)v1=v1.replace("booking_id VARCHAR(255) NOT NULL","booking_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
        db.execute(v1);
        // Copy only our two pre-migration synthetic parents and real receipts, not another workspace or production.
        for(var parent:historicalParents)db.update("INSERT INTO booking_master SELECT * FROM `"+sourceSchema+"`.booking_master WHERE id=?",parent.get("id"));
        for(var receipt:historicalReceipts)db.update("INSERT INTO ipad_batch_request SELECT * FROM `"+sourceSchema+"`.ipad_batch_request WHERE request_id=?",receipt.get("request_id"));
        System.out.println("SCOPE_FK_PHYSICAL_FIXTURE schema="+schema+" retained=true source="+sourceSchema+" rows=two-real-historical-receipts purpose="+purpose);
        return db;
    }
    @Test void dirtyScopeStopsConstraintAdditionRetainsRowsAndPreviouslyAddedIndex() throws Exception {
        var db=physicalCopy("dirty",false);assertCompatibleColumns(db);
        db.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) SELECT 2,booking_master_id,booking_id,?,payload_sha256,operator_id,result_json FROM ipad_batch_request WHERE request_id=?",key(),historicalReceipts.get(0).get("request_id"));
        var rows=db.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id");assertEquals(3,rows.size());assertEquals(1,scopeMismatches(db));
        // Exercise the server failure too, as if preflight was missed or a writer introduced a mismatch.
        db.execute(migration.get(0));
        var fail=assertThrows(DataAccessException.class,()->db.execute(migration.get(1)));assertEquals(1452,mysqlCode(fail));
        assertEquals(1,scopeIndexCount(db));assertEquals(0,scopeFkColumns(db));assertEquals(rows,db.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
        // Only retry the missing step; still fails, no automatic repairs and no repeat of STEP 1.
        var retry=assertThrows(DataAccessException.class,()->db.execute(migration.get(1)));assertEquals(1452,mysqlCode(retry));assertEquals(1,scopeIndexCount(db));assertEquals(0,scopeFkColumns(db));
        assertEquals(rows,db.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
        System.out.println("SCOPE_FK_DIRTY failed=1452 retry=1452 retained_rows=3 mismatches=1 parent_unique_remains=true scope_fk_absent=true no_cleanup=true");
    }
    @Test void incompatibleCollationFailsWithoutAutomaticConversion() throws Exception {
        var db=physicalCopy("collation",true);
        assertNotEquals(columns(db,"booking_master","booking_id"),columns(db,"ipad_batch_request","booking_id"));
        var before=db.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id");
        db.execute(migration.get(0));var fail=assertThrows(DataAccessException.class,()->db.execute(migration.get(1)));assertEquals(3780,mysqlCode(fail));
        assertEquals(1,scopeIndexCount(db));assertEquals(0,scopeFkColumns(db));assertEquals(before,db.queryForList("SELECT * FROM ipad_batch_request ORDER BY request_id"));
        assertEquals("utf8mb4_bin",columns(db,"ipad_batch_request","booking_id").get(0).get("COLLATION_NAME"));
        System.out.println("SCOPE_FK_COLLATION failed=3780 no_conversion=true parent_unique_remains=true receipts_unchanged=true");
    }
}
