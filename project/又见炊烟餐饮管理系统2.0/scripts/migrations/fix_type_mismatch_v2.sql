-- ============================================================
-- 修复外键类型不匹配 v2 (16处, 排除有物理外键约束的列)
-- 所有目标表为空表, 无数据丢失风险
-- ============================================================
USE banquet;

-- FATAL (1处)
ALTER TABLE `sys_user_role` MODIFY COLUMN `staff_id` int COMMENT '员工ID';

-- ERROR (15处)
ALTER TABLE `attendance_records` MODIFY COLUMN `staff_id` int COMMENT '员工ID';
ALTER TABLE `booking_dish_detail` MODIFY COLUMN `booking_id` bigint COMMENT '预订ID';
ALTER TABLE `dish_master` MODIFY COLUMN `category_id` int COMMENT '分类ID';
ALTER TABLE `dishes` MODIFY COLUMN `category_id` int COMMENT '分类ID';
ALTER TABLE `finance_payable` MODIFY COLUMN `purchase_id` bigint COMMENT '采购订单ID';
ALTER TABLE `finance_payment_record` MODIFY COLUMN `booking_id` bigint COMMENT '预订ID';
ALTER TABLE `kitchen_log` MODIFY COLUMN `booking_id` bigint COMMENT '预订ID';
ALTER TABLE `marketing_coupon_record` MODIFY COLUMN `booking_id` bigint COMMENT '预订ID';
ALTER TABLE `member_consume_record` MODIFY COLUMN `booking_id` bigint COMMENT '预订ID';
ALTER TABLE `purchase_order_detail` MODIFY COLUMN `ingredient_id` varchar(50) COMMENT '食材ID';
ALTER TABLE `purchase_receipt_detail` MODIFY COLUMN `ingredient_id` varchar(50) COMMENT '食材ID';
ALTER TABLE `purchase_return_detail` MODIFY COLUMN `ingredient_id` varchar(50) COMMENT '食材ID';
ALTER TABLE `report_dish_sales` MODIFY COLUMN `dish_id` varchar(20) COMMENT '菜品ID';
ALTER TABLE `stock_loss_detail` MODIFY COLUMN `ingredient_id` varchar(50) COMMENT '食材ID';
ALTER TABLE `stock_take_detail` MODIFY COLUMN `ingredient_id` varchar(50) COMMENT '食材ID';

SELECT '类型修复v2完成' AS result;
