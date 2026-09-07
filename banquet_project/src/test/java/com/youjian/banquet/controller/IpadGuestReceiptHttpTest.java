package com.youjian.banquet.controller;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Composes the existing real servlet/DB fixture. Seeded receipts are NOT a submit-flow test. */
class IpadGuestReceiptHttpTest {
    IpadGuestOrderReadHttpTest f;
    static final String KEY="SYN-RECEIPT-000001";
    @BeforeEach void setup()throws Exception {
        f=new IpadGuestOrderReadHttpTest();f.setup();
        try(var ddl=new org.springframework.core.io.ClassPathResource("ipad_batch_request_migration_v1.sql").getInputStream()) {
            f.jdbc.execute(new String(ddl.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    @AfterEach void close(){if(f!=null)f.close();}
    String receipt(){return "{\"client_request_id\":\""+KEY+"\",\"booking_id\":\"SYN-BOOK\",\"status\":\"committed\",\"dish_booking_ids\":[1,2],\"added_dishes\":2,\"added_quantity\":4,\"added_amount\":50.98}";}
    void seed(String content){f.jdbc.update("INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) SELECT 1,id,booking_id,?, ?,12,? FROM booking_master WHERE booking_id='SYN-BOOK' AND store_id=1",KEY,"0".repeat(64),content);}
    IpadGuestOrderReadHttpTest.Reply read(String token,String suffix)throws Exception{return f.call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK"+suffix,"SYN-A",1,null,token);}
    String query(){return "&client_request_id="+KEY;}
    @Test void closedOrderOriginalReceiptAndNoWrites()throws Exception {
        seed(receipt());f.jdbc.update("UPDATE booking_master SET payment_status='paid',booking_status='completed' WHERE booking_id='SYN-BOOK'");
        var before=f.state();var receipts=f.jdbc.queryForList("SELECT * FROM ipad_batch_request");
        var r=read(f.grant(),query());assertEquals(200,r.status());assertEquals(f.json.readTree(receipt()),r.body().path("data").path("submission"));
        assertEquals("paid",r.body().path("data").path("payment_status").asText());assertEquals(before,f.state());assertEquals(receipts,f.jdbc.queryForList("SELECT * FROM ipad_batch_request"));
        assertFalse(r.body().toString().contains("operator_id"));assertFalse(r.body().toString().contains("payload_sha256"));
    }
    @Test void absentKeyIsNullAndOmittedKeyNeverNeedsReceiptTable()throws Exception {
        String token=f.grant();var r=read(token,query());assertEquals(200,r.status());assertTrue(r.body().path("data").get("submission").isNull());
        f.jdbc.execute("ALTER TABLE ipad_batch_request RENAME TO preserved_receipts");assertEquals(200,read(token,"").status());assertEquals(503,read(token,query()).status());
    }
    @Test void duplicateJsonFieldsAndUnknownPiiRejected()throws Exception {
        seed(receipt().replace("{","{\"status\":\"committed\","));String token=f.grant();assertEquals(503,read(token,query()).status());
        f.jdbc.update("UPDATE ipad_batch_request SET result_json=?",receipt().replace("{","{\"customer_name\":\"SYN-PRIVATE\","));var r=read(token,query());assertEquals(503,r.status());assertFalse(r.body().toString().contains("SYN-PRIVATE"));
    }
    @Test void mismatchedJsonAndInvalidShapesRejected()throws Exception {
        seed(receipt());String token=f.grant();
        for(String bad:List.of(receipt().replace(KEY,"OTHER-REQUEST-0001"),receipt().replace("SYN-BOOK","OTHER-BOOK"),receipt().replace("committed","pending"),receipt().replace("[1,2]","[1,1]"),receipt().replace("50.98","-1"),receipt().replace("\"added_dishes\":2","\"added_dishes\":3"),"[]","{}")){
            f.jdbc.update("UPDATE ipad_batch_request SET result_json=?",bad);assertEquals(503,read(token,query()).status());
        }
    }
    @Test void corruptJsonRejectedEvenIfLegacyConstraintNotEnforced()throws Exception {
        seed(receipt());f.jdbc.execute("ALTER TABLE ipad_batch_request ALTER CHECK chk_ipad_batch_result_json NOT ENFORCED");
        f.jdbc.update("UPDATE ipad_batch_request SET result_json='not-json'");assertEquals(503,read(f.grant(),query()).status());
    }
    @Test void allRelationalScopeColumnsRequired()throws Exception {
        seed(receipt());String token=f.grant();
        f.jdbc.update("UPDATE ipad_batch_request SET store_id=2");assertTrue(read(token,query()).body().path("data").path("submission").isNull());
        f.jdbc.update("UPDATE ipad_batch_request SET store_id=1,booking_id='OTHER'");assertTrue(read(token,query()).body().path("data").path("submission").isNull());
        f.jdbc.update("UPDATE ipad_batch_request SET booking_id='SYN-BOOK',booking_master_id=(SELECT id FROM booking_master WHERE booking_id='SYN-OTHER')");assertTrue(read(token,query()).body().path("data").path("submission").isNull());
    }
    @Test void authScopeAndSuspensionStillReject()throws Exception {
        seed(receipt());String token=f.grant();assertEquals(403,read(null,query()).status());
        assertEquals(403,f.call("GET","/api/ipad/order/detail?booking_id=SYN-BOOK"+query(),"SYN-B",1,null,token).status());
        assertEquals(403,f.call("GET","/api/ipad/order/detail?booking_id=SYN-OTHER"+query(),"SYN-C",2,null,token).status());
        f.jdbc.update("UPDATE staff_master SET employment_status='left' WHERE staff_id=11");assertEquals(403,read(token,query()).status());
    }
    @Test void strictQueryAndCaseSensitiveKey()throws Exception {
        seed(receipt());String token=f.grant();
        for(String q:List.of("&client_request_id=","&client_request_id=short",query()+query(),"&booking_id=SYN-BOOK"+query(),"&client_request_id="+"x".repeat(101)))assertEquals(400,read(token,q).status());
        assertTrue(read(token,"&client_request_id="+KEY.toLowerCase(Locale.ROOT)).body().path("data").path("submission").isNull());
    }
    @Test void brokenDetailNotBypassedByValidReceipt()throws Exception {
        seed(receipt());f.jdbc.update("UPDATE booking_dish_detail SET subtotal=1 WHERE dish_booking_id=1");assertEquals(503,read(f.grant(),query()).status());
    }
}
