package com.youjian.banquet.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Read-only invariants for the restaurant flows exercised in the isolated integration database. */
final class BusinessDataAssertions {
    private BusinessDataAssertions() {}

    static void verify(JdbcTemplate jdbc) {
        Map<String,String> rules = new LinkedHashMap<>();
        rules.put("桌台关联必须有同门店预订单", "SELECT COUNT(*) FROM booking_table d LEFT JOIN booking_master p ON p.booking_id=d.booking_id AND p.store_id=d.store_id WHERE p.booking_id IS NULL");
        rules.put("桌台关联必须有同门店桌台", "SELECT COUNT(*) FROM booking_table d LEFT JOIN table_master p ON p.table_id=d.table_id AND p.store_id=d.store_id WHERE p.table_id IS NULL");
        rules.put("菜品明细必须有同门店预订单", "SELECT COUNT(*) FROM booking_dish_detail d LEFT JOIN booking_master p ON p.booking_id=d.booking_id AND p.store_id=d.store_id WHERE p.booking_id IS NULL");
        rules.put("菜品明细必须有同门店菜品", "SELECT COUNT(*) FROM booking_dish_detail d LEFT JOIN dish_master p ON p.dish_id=d.dish_id AND p.store_id=d.store_id WHERE p.dish_id IS NULL");
        rules.put("预订客户引用必须有效", "SELECT COUNT(*) FROM booking_master d LEFT JOIN customer_master p ON p.customer_id=d.customer_id AND p.store_id=d.store_id WHERE d.customer_id IS NOT NULL AND p.customer_id IS NULL");
        rules.put("厨房记录必须有同门店预订单", "SELECT COUNT(*) FROM kitchen_log d LEFT JOIN booking_master p ON p.booking_id=d.booking_id AND p.store_id=d.store_id WHERE p.booking_id IS NULL");
        rules.put("库存流水必须有同门店原料", "SELECT COUNT(*) FROM ingredient_inventory_log d LEFT JOIN ingredient_master p ON p.ingredient_id=d.food_material_id AND p.store_id=d.store_id WHERE p.ingredient_id IS NULL");
        rules.put("支付请求必须有同门店订单", "SELECT COUNT(*) FROM ipad_payment_request d LEFT JOIN booking_master p ON p.booking_id=d.booking_id AND p.store_id=d.store_id WHERE p.booking_id IS NULL");
        rules.put("订单收款必须有同门店订单", "SELECT COUNT(*) FROM finance_transaction d LEFT JOIN booking_master p ON p.id=d.related_id AND p.store_id=d.store_id WHERE d.related_type='booking' AND p.booking_id IS NULL");
        rules.put("收款操作员必须存在", "SELECT COUNT(*) FROM finance_transaction d LEFT JOIN staff_master p ON p.staff_id=d.operator_id WHERE p.staff_id IS NULL");
        rules.put("菜品数量价格小计必须一致", "SELECT COUNT(*) FROM booking_dish_detail WHERE dish_quantity IS NULL OR dish_quantity<=0 OR unit_price IS NULL OR unit_price<0 OR subtotal IS NULL OR subtotal<>ROUND(dish_quantity*unit_price,2)");
        rules.put("已付订单金额必须等于有效明细", "SELECT COUNT(*) FROM booking_master b WHERE b.payment_status='paid' AND (b.booking_status<>'completed' OR b.total_amount<>(SELECT COALESCE(SUM(d.subtotal),0) FROM booking_dish_detail d WHERE d.booking_id=b.booking_id AND d.store_id=b.store_id AND COALESCE(d.kitchen_status,'') NOT IN ('refunded','cancelled')))");
        rules.put("支付与财务流水和应收必须相符", "SELECT COUNT(*) FROM ipad_payment_request r JOIN booking_master b ON b.booking_id=r.booking_id AND b.store_id=r.store_id WHERE b.payment_status<>'paid' OR r.amount<>b.final_amount OR r.amount<>(SELECT COALESCE(SUM(f.amount),0) FROM finance_transaction f WHERE f.related_type='booking' AND f.related_id=b.id AND f.store_id=r.store_id)");
        rules.put("已付订单必须有且仅有一次收款请求", "SELECT COUNT(*) FROM booking_master b WHERE b.payment_status='paid' AND (SELECT COUNT(*) FROM ipad_payment_request r WHERE r.booking_id=b.booking_id AND r.store_id=b.store_id)<>1");
        rules.put("库存不能为负数", "SELECT COUNT(*) FROM ingredient_master WHERE current_stock IS NULL OR current_stock<0");
        rules.put("库存流水数量运算必须正确", "SELECT COUNT(*) FROM ingredient_inventory_log WHERE change_quantity IS NULL OR change_quantity<=0 OR before_quantity IS NULL OR after_quantity IS NULL OR after_quantity<0 OR change_type NOT IN ('IN','OUT') OR (change_type='IN' AND after_quantity<>before_quantity+change_quantity) OR (change_type='OUT' AND after_quantity<>before_quantity-change_quantity)");
        rules.put("连续库存流水不能断链", "SELECT COUNT(*) FROM (SELECT before_quantity,LAG(after_quantity) OVER(PARTITION BY store_id,food_material_id ORDER BY log_id) previous_stock FROM ingredient_inventory_log) q WHERE previous_stock IS NOT NULL AND before_quantity<>previous_stock");
        rules.put("最新库存流水必须等于库存余额", "SELECT COUNT(*) FROM ingredient_inventory_log l JOIN ingredient_master i ON i.store_id=l.store_id AND i.ingredient_id=l.food_material_id WHERE l.log_id=(SELECT MAX(x.log_id) FROM ingredient_inventory_log x WHERE x.store_id=l.store_id AND x.food_material_id=l.food_material_id) AND l.after_quantity<>i.current_stock");
        for (String table : new String[]{"booking_master","booking_table","booking_dish_detail","table_master","customer_master","dish_master","ingredient_master","ingredient_inventory_log","kitchen_log","finance_transaction","ipad_payment_request","staff_master"}) {
            rules.put(table+" 必须归属有效门店", "SELECT COUNT(*) FROM "+table+" d LEFT JOIN store_info s ON s.store_id=d.store_id WHERE s.store_id IS NULL");
        }
        for (var rule : rules.entrySet()) {
            assertEquals(0, jdbc.queryForObject(rule.getValue(), Integer.class), rule.getKey());
        }
    }
}
