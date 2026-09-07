package com.youjian.banquet.controller;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the existing physical supply DDL/transactions and relationship assertions; no production writes. */
class ReceiptSupplierIntegrityTest extends SupplyCostMysqlIntegrationTest {
    @Test void invalidSupplierCannotCreateUnacceptablePendingReceiptAndValidSourceFlowsOnce() {
        String ingredientId = ingredient();
        jdbc.update("INSERT INTO supplier_master(supplier_id,store_id,supplier_name,is_active) VALUES(901,2,'Synthetic foreign supplier',1),(902,1,'Synthetic disabled supplier',0)");
        var receiptsBefore = jdbc.queryForList("SELECT * FROM purchase_receipt ORDER BY receipt_id");
        var detailsBefore = jdbc.queryForList("SELECT * FROM purchase_receipt_detail ORDER BY detail_id");
        var stockBefore = stock(ingredientId);
        var logsBefore = jdbc.queryForList("SELECT * FROM ingredient_inventory_log ORDER BY log_id");
        var payableBefore = jdbc.queryForList("SELECT * FROM finance_payable ORDER BY payable_id");
        for (Integer supplierId : Arrays.asList(null, 0, -1, 999999, 901, 902)) {
            var receipt = receipt("PENDING");
            receipt.setSupplierId(supplierId);
            assertThrows(IllegalArgumentException.class, () -> supply.createGoodsReceipt(receipt, List.of(item(ingredientId,"1.25","2.34567891"))));
            assertEquals(receiptsBefore, jdbc.queryForList("SELECT * FROM purchase_receipt ORDER BY receipt_id"));
            assertEquals(detailsBefore, jdbc.queryForList("SELECT * FROM purchase_receipt_detail ORDER BY detail_id"));
            assertEquals(stockBefore, stock(ingredientId));
            assertEquals(logsBefore, jdbc.queryForList("SELECT * FROM ingredient_inventory_log ORDER BY log_id"));
            assertEquals(payableBefore, jdbc.queryForList("SELECT * FROM finance_payable ORDER BY payable_id"));
        }
        var input = receipt("PENDING");
        input.setSupplierName("Untrusted supplied name");
        var pending = supply.createGoodsReceipt(input, List.of(item(ingredientId,"1.25","2.34567891")));
        assertEquals(stockBefore, stock(ingredientId));
        assertEquals(jdbc.queryForObject("SELECT supplier_name FROM supplier_master WHERE supplier_id=1 AND store_id=1", String.class), pending.getSupplierName());
        supply.acceptGoodsReceipt(pending.getReceiptId(), "Synthetic operator");
        assertEquals(0, new BigDecimal("1.25").compareTo(stock(ingredientId)));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=? AND supplier_id=1 AND store_id=1", Integer.class, pending.getReceiptId()));
        assertEquals(0, new BigDecimal("2.93").compareTo(jdbc.queryForObject("SELECT total_amount FROM finance_payable WHERE source_receipt_id=?", BigDecimal.class, pending.getReceiptId())));
        var acceptedLogs = jdbc.queryForList("SELECT * FROM ingredient_inventory_log ORDER BY log_id");
        supply.acceptGoodsReceipt(pending.getReceiptId(), "Synthetic retry");
        assertEquals(acceptedLogs, jdbc.queryForList("SELECT * FROM ingredient_inventory_log ORDER BY log_id"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=?", Integer.class, pending.getReceiptId()));
        assertEquals(0, new BigDecimal("1.25").compareTo(stock(ingredientId)));
    }
}
