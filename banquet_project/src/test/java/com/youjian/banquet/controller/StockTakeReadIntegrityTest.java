package com.youjian.banquet.controller;

import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Current captured physical DDL, no production access; inherited 14 valid flow cases remain intact. */
class StockTakeReadIntegrityTest extends StockTakePhysicalSchemaTest {
    private Long saved() {
        var result = controller.createStockTake(body(List.of(item("SYN-ST", "8.125"))));
        assertEquals(200, result.getCode(), result.getMessage());
        return result.getData().getTakeId();
    }

    @Test void completeMasterWithoutDetailsIsNotAHealthyEmptyRead() {
        var tx = new TransactionTemplate(new JpaTransactionManager(factory.getObject()));
        tx.execute(status -> {
            try {
                jdbc.update("INSERT INTO stock_take(take_no,store_id,take_type,take_date,status,total_items) VALUES('SYN-EMPTY-READ',1,'monthly','2026-09-07','completed',0)");
                Long id = jdbc.queryForObject("SELECT take_id FROM stock_take WHERE take_no='SYN-EMPTY-READ'", Long.class);
                assertEquals(503, controller.getStockTake(id).getCode());
                assertNull(controller.getStockTake(id).getData());
                assertEquals(503, controller.listStockTakeDetails(id).getCode());
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM stock_take WHERE take_id=? AND status='completed'", Integer.class, id));
            } finally { status.setRollbackOnly(); }
            return null;
        });
    }

    @Test void mismatchedStoreAndCountAreRejectedWithoutExposingOrRewritingRows() {
        Long id = saved();
        var tx = new TransactionTemplate(new JpaTransactionManager(factory.getObject()));
        tx.execute(status -> {
            try {
                jdbc.update("UPDATE stock_take_detail SET store_id=2,ingredient_id='SYN-OTHER' WHERE take_id=?", id);
                assertEquals(503, controller.getStockTake(id).getCode());
                assertNull(controller.getStockTake(id).getData());
                assertEquals(503, controller.listStockTakeDetails(id).getCode());
                assertEquals(2L, jdbc.queryForObject("SELECT store_id FROM stock_take_detail WHERE take_id=?", Long.class, id));
            } finally { status.setRollbackOnly(); }
            return null;
        });
        tx.execute(status -> {
            try {
                jdbc.update("UPDATE stock_take SET total_items=99 WHERE take_id=?", id);
                assertEquals(503, controller.getStockTake(id).getCode());
                assertEquals(503, controller.listStockTakeDetails(id).getCode());
                assertEquals(99, jdbc.queryForObject("SELECT total_items FROM stock_take WHERE take_id=?", Integer.class, id));
            } finally { status.setRollbackOnly(); }
            return null;
        });
        assertEquals(200, controller.getStockTake(id).getCode());
    }

    @Test void readRequiresRealIdentityAndStoreAccess() {
        Long id = saved();
        UserContext.clear();
        assertEquals(403, controller.getStockTake(id).getCode());
        assertEquals(403, controller.listStockTakeDetails(id).getCode());
        UserContext.set(new UserContext.CurrentUser(2L,2L,"store_manager","Synthetic other"));
        assertEquals(403, controller.getStockTake(id).getCode());
        assertEquals(403, controller.listStockTakeDetails(id).getCode());
    }
}
