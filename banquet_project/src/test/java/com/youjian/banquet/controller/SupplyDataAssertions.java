package com.youjian.banquet.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class SupplyDataAssertions {
    private SupplyDataAssertions() {}
    static void verify(JdbcTemplate jdbc) {
        var rules=new LinkedHashMap<String,String>();
        rules.put("收货明细不能脱离同店主单", "SELECT COUNT(*) FROM purchase_receipt_detail d LEFT JOIN purchase_receipt r ON r.receipt_id=d.receipt_id AND r.store_id=d.store_id WHERE r.receipt_id IS NULL");
        rules.put("收货明细原料必须存在", "SELECT COUNT(*) FROM purchase_receipt_detail d LEFT JOIN ingredient_master i ON i.ingredient_id=d.ingredient_id AND i.store_id=d.store_id WHERE i.ingredient_id IS NULL");
        rules.put("收货单必须有明细并且金额一致", "SELECT COUNT(*) FROM purchase_receipt r WHERE NOT EXISTS(SELECT 1 FROM purchase_receipt_detail d WHERE d.receipt_id=r.receipt_id AND d.store_id=r.store_id) OR r.total_amount<>(SELECT SUM(d.amount) FROM purchase_receipt_detail d WHERE d.receipt_id=r.receipt_id AND d.store_id=r.store_id)");
        rules.put("收货明细金额必须由实收数量计算", "SELECT COUNT(*) FROM purchase_receipt_detail WHERE actual_quantity<=0 OR unit_price<0 OR amount<>ROUND(actual_quantity*unit_price,2)");
        rules.put("入库流水必须关联已验收收货单", "SELECT COUNT(*) FROM ingredient_inventory_log l LEFT JOIN purchase_receipt r ON CAST(r.receipt_id AS CHAR)=l.source_id AND r.store_id=l.store_id WHERE l.source_type='GOODS_RECEIPT' AND (r.receipt_id IS NULL OR r.status<>'ACCEPTED')");
        rules.put("已验收入库数量与明细必须一致", "SELECT COUNT(*) FROM (SELECT d.receipt_id,d.store_id,d.ingredient_id,SUM(d.actual_quantity) qty FROM purchase_receipt_detail d JOIN purchase_receipt r ON r.receipt_id=d.receipt_id AND r.store_id=d.store_id WHERE r.status='ACCEPTED' GROUP BY d.receipt_id,d.store_id,d.ingredient_id) q WHERE q.qty<>(SELECT COALESCE(SUM(l.change_quantity),0) FROM ingredient_inventory_log l WHERE l.store_id=q.store_id AND l.food_material_id=q.ingredient_id AND l.source_type='GOODS_RECEIPT' AND l.source_id=CAST(q.receipt_id AS CHAR))");
        rules.put("领料明细必须有同店主单与原料", "SELECT COUNT(*) FROM material_requisition_item d LEFT JOIN material_requisition r ON r.requisition_id=d.requisition_id AND r.store_id=d.store_id LEFT JOIN ingredient_master i ON i.ingredient_id=d.ingredient_id AND i.store_id=d.store_id WHERE r.requisition_id IS NULL OR i.ingredient_id IS NULL");
        rules.put("领料主单和明细金额必须一致", "SELECT COUNT(*) FROM material_requisition r WHERE NOT EXISTS(SELECT 1 FROM material_requisition_item d WHERE d.requisition_id=r.requisition_id AND d.store_id=r.store_id) OR r.total_amount<>(SELECT SUM(d.amount) FROM material_requisition_item d WHERE d.requisition_id=r.requisition_id AND d.store_id=r.store_id)");
        rules.put("领料出库必须有已审批来源", "SELECT COUNT(*) FROM ingredient_inventory_log l LEFT JOIN material_requisition_item d ON CAST(d.item_id AS CHAR)=l.source_id AND d.store_id=l.store_id AND d.ingredient_id=l.food_material_id LEFT JOIN material_requisition r ON r.requisition_id=d.requisition_id AND r.store_id=d.store_id WHERE l.source_type='REQUISITION_ITEM' AND (d.item_id IS NULL OR r.status<>'APPROVED' OR l.change_quantity<>d.quantity OR l.total_amount<>d.amount)");
        rules.put("每条已审批领料只能出库一次", "SELECT COUNT(*) FROM material_requisition_item d JOIN material_requisition r ON r.requisition_id=d.requisition_id AND r.store_id=d.store_id WHERE r.status='APPROVED' AND (SELECT COUNT(*) FROM ingredient_inventory_log l WHERE l.source_type='REQUISITION_ITEM' AND l.source_id=CAST(d.item_id AS CHAR) AND l.store_id=d.store_id)<>1");
        rules.put("加工记录必须对应同店同原料已领明细", "SELECT COUNT(*) FROM preprocessing_record p LEFT JOIN material_requisition_item d ON d.item_id=p.requisition_item_id AND d.store_id=p.store_id AND d.ingredient_id=p.ingredient_id LEFT JOIN material_requisition r ON r.requisition_id=d.requisition_id AND r.store_id=d.store_id WHERE d.item_id IS NULL OR r.status<>'APPROVED'");
        rules.put("加工出成率必须由毛料净料计算", "SELECT COUNT(*) FROM preprocessing_record WHERE raw_qty<=0 OR processed_qty<0 OR yield_rate<>ROUND(processed_qty/raw_qty*100,2)");
        rules.put("累计加工不能超过领料", "SELECT COUNT(*) FROM material_requisition_item d WHERE (SELECT COALESCE(SUM(p.raw_qty),0) FROM preprocessing_record p WHERE p.requisition_item_id=d.item_id AND p.store_id=d.store_id)>d.quantity");
        rules.put("成本卡必须对应同门店菜品及有效配方成本", "SELECT COUNT(*) FROM dish_cost_card c LEFT JOIN dish_master d ON d.dish_id=c.dish_id AND d.store_id=c.store_id WHERE d.dish_id IS NULL OR c.standard_cost<>d.cost_price OR c.standard_cost<>(SELECT ROUND(SUM(r.total_cost),2) FROM dish_recipe r WHERE r.dish_id=c.dish_id AND r.store_id=c.store_id)");
        for(var rule:rules.entrySet())assertEquals(0,jdbc.queryForObject(rule.getValue(),Integer.class),rule.getKey());
    }
}
