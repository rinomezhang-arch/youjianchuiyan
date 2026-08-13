-- 又见炊烟：核心实体键、索引及业务单号序列审计修复
-- 执行前请先在预发布环境备份并检查重复数据。

CREATE TABLE IF NOT EXISTS business_number_sequence (
  sequence_key VARCHAR(64) NOT NULL COMMENT '业务类型+门店+日期',
  current_value BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sequence_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='并发安全业务单号序列';

-- 下列索引覆盖系统最常用的门店隔离、日期区间和状态查询。
-- MySQL 8.0 可在 information_schema.statistics 中检查后按需执行。
SET @db = DATABASE();

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_index_if_missing(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl TEXT)
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=@db AND table_name=p_table)
     AND NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@db AND table_name=p_table AND index_name=p_index) THEN
    SET @ddl = p_ddl;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL add_index_if_missing('booking_master','idx_booking_store_date_status','CREATE INDEX idx_booking_store_date_status ON booking_master(store_id, booking_date, booking_status)');
CALL add_index_if_missing('booking_master','idx_booking_store_payment_date','CREATE INDEX idx_booking_store_payment_date ON booking_master(store_id, payment_status, booking_date)');
CALL add_index_if_missing('booking_master','uk_booking_store_no','CREATE UNIQUE INDEX uk_booking_store_no ON booking_master(store_id, booking_no)');
CALL add_index_if_missing('booking_dish_detail','idx_booking_dish_store_booking','CREATE INDEX idx_booking_dish_store_booking ON booking_dish_detail(store_id, booking_id, kitchen_status)');
CALL add_index_if_missing('finance_transaction','idx_finance_store_date_type','CREATE INDEX idx_finance_store_date_type ON finance_transaction(store_id, trans_date, trans_type)');
CALL add_index_if_missing('finance_transaction','idx_finance_store_category_date','CREATE INDEX idx_finance_store_category_date ON finance_transaction(store_id, trans_category, trans_date)');
CALL add_index_if_missing('staff_master','uk_staff_store_account','CREATE UNIQUE INDEX uk_staff_store_account ON staff_master(store_id, staff_account)');
CALL add_index_if_missing('staff_master','idx_staff_store_phone','CREATE INDEX idx_staff_store_phone ON staff_master(store_id, staff_phone)');
CALL add_index_if_missing('customer_master','idx_customer_store_phone','CREATE INDEX idx_customer_store_phone ON customer_master(store_id, customer_phone)');
CALL add_index_if_missing('table_master','uk_table_store_code','CREATE UNIQUE INDEX uk_table_store_code ON table_master(store_id, table_code)');
CALL add_index_if_missing('inventory_stock','uk_inventory_store_item','CREATE UNIQUE INDEX uk_inventory_store_item ON inventory_stock(store_id, item_id)');
CALL add_index_if_missing('purchase_order','idx_purchase_store_date_status','CREATE INDEX idx_purchase_store_date_status ON purchase_order(store_id, order_date, order_status)');
CALL add_index_if_missing('attendance_record','idx_attendance_store_staff_date','CREATE INDEX idx_attendance_store_staff_date ON attendance_record(store_id, staff_id, attendance_date)');
CALL add_index_if_missing('energy_record','idx_energy_store_date_type','CREATE INDEX idx_energy_store_date_type ON energy_record(store_id, record_date, energy_type)');

DROP PROCEDURE IF EXISTS add_index_if_missing;
