-- TR-RECEIPT-REAL-24 r2 返修：隔离 schema co_print23_20260909_022305 的只读链夹具。
-- 原则（返修单）：只允许在本隔离 schema 补齐原项目 banquet_full_schema.sql 已定义的
--   缺失标准表/列，并新增带 TR24 标识的桌台关联夹具；只 INSERT 不 DELETE、不 DROP、
--   不 UPDATE 旧记录、不建全新业务表。已存在但定义不符时由运行器预检中止，本脚本
--   自身全部 CREATE TABLE IF NOT EXISTS / 信息架构守卫的 ADD COLUMN / NOT EXISTS 插入，
--   重复执行只跳过、不覆盖。
SET NAMES utf8mb4;

-- ========== 1) booking_master 补齐原项目标准列（缺则加，已存在则跳过） ==========
SET @ddl := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE booking_master ADD COLUMN booking_time time DEFAULT NULL',
  'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_master' AND COLUMN_NAME='booking_time');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE booking_master ADD COLUMN staff_name varchar(20) DEFAULT NULL',
  'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_master' AND COLUMN_NAME='staff_name');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE booking_master ADD COLUMN updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_master' AND COLUMN_NAME='updated_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 2) 原项目标准表（定义摘自 banquet_project/banquet_full_schema.sql） ==========
CREATE TABLE IF NOT EXISTS `table_master` (
  `table_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_number` varchar(10) NOT NULL,
  `table_name` varchar(20) DEFAULT NULL,
  `table_location` varchar(50) DEFAULT NULL,
  `table_area` varchar(20) DEFAULT NULL,
  `table_capacity` int DEFAULT '10',
  `table_type` varchar(20) DEFAULT NULL,
  `table_status` varchar(20) DEFAULT 'available',
  `min_capacity` int DEFAULT '6',
  `max_capacity` int DEFAULT '12',
  `sort_order` int DEFAULT '0',
  `is_active` int DEFAULT '1',
  `remark` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`table_id`),
  KEY `idx_tm_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `booking_table` (
  `booking_master_id` bigint DEFAULT NULL,
  `booking_table_code` varchar(50) DEFAULT NULL,
  `table_booking_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `booking_id` varchar(20) NOT NULL,
  `booking_date` date NOT NULL,
  `booking_time` time DEFAULT NULL,
  `table_id` int NOT NULL,
  `table_number` varchar(10) DEFAULT NULL,
  `table_name` varchar(20) DEFAULT NULL,
  `guest_count` int DEFAULT '0',
  `package_id` varchar(20) DEFAULT NULL,
  `package_name` varchar(100) DEFAULT NULL,
  `open_table_type` varchar(50) DEFAULT NULL,
  `table_note` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`table_booking_id`),
  UNIQUE KEY `uk_table_date_time` (`table_id`,`booking_date`,`booking_time`),
  KEY `idx_store` (`store_id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_table` (`table_id`),
  KEY `fk_booking_master` (`booking_master_id`),
  CONSTRAINT `fk_bt_booking_master` FOREIGN KEY (`booking_master_id`) REFERENCES `booking_master` (`id`),
  CONSTRAINT `fk_bt_table` FOREIGN KEY (`table_id`) REFERENCES `table_master` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `finance_transaction` (
  `trans_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `trans_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_date` date NOT NULL,
  `trans_time` datetime NOT NULL,
  `trans_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `related_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2) DEFAULT NULL,
  `payer_payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`trans_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_trans_date` (`trans_date`),
  KEY `idx_trans_no` (`trans_no`),
  KEY `idx_trans_type` (`trans_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支流水表';

-- ========== 3) TR24 标识桌台夹具（只新增，NOT EXISTS 守卫；不碰任何旧行） ==========
-- 两张 TR24 桌台绑定既有订单 COPRINT23-BK-001（store 1），用于多桌稳定聚合贯通断言。
INSERT INTO table_master
  (table_id, store_id, table_number, table_name, table_location, table_area,
   table_capacity, table_type, table_status, min_capacity, max_capacity, sort_order, is_active, remark)
SELECT 924001, 1, 'TR24-T01', 'TR24-01号桌', 'TR24隔离区', 'TR24', 8, 'normal', 'available', 2, 10, 9241, 1, 'TR24-fixture'
WHERE NOT EXISTS (SELECT 1 FROM table_master WHERE table_id = 924001);

INSERT INTO table_master
  (table_id, store_id, table_number, table_name, table_location, table_area,
   table_capacity, table_type, table_status, min_capacity, max_capacity, sort_order, is_active, remark)
SELECT 924002, 1, 'TR24-T02', 'TR24-02号桌', 'TR24隔离区', 'TR24', 8, 'normal', 'available', 2, 10, 9242, 1, 'TR24-fixture'
WHERE NOT EXISTS (SELECT 1 FROM table_master WHERE table_id = 924002);

INSERT INTO booking_table
  (table_booking_id, booking_master_id, booking_table_code, store_id, booking_id,
   booking_date, booking_time, table_id, table_number, table_name, guest_count, open_table_type, table_note)
SELECT 924001, b.id, 'TR24-TC01', 1, b.booking_id,
   b.booking_date, '12:00:00', 924001, 'TR24-T01', 'TR24-01号桌', b.guest_count, 'TR24-fixture', 'TR24 fixture table 1'
FROM booking_master b
WHERE b.booking_id = 'COPRINT23-BK-001' AND b.store_id = 1
  AND NOT EXISTS (SELECT 1 FROM booking_table WHERE table_booking_id = 924001);

INSERT INTO booking_table
  (table_booking_id, booking_master_id, booking_table_code, store_id, booking_id,
   booking_date, booking_time, table_id, table_number, table_name, guest_count, open_table_type, table_note)
SELECT 924002, b.id, 'TR24-TC02', 1, b.booking_id,
   b.booking_date, '12:30:00', 924002, 'TR24-T02', 'TR24-02号桌', b.guest_count, 'TR24-fixture', 'TR24 fixture table 2'
FROM booking_master b
WHERE b.booking_id = 'COPRINT23-BK-001' AND b.store_id = 1
  AND NOT EXISTS (SELECT 1 FROM booking_table WHERE table_booking_id = 924002);

SELECT 'TR24_FIXTURES_OK' AS result,
  (SELECT COUNT(*) FROM table_master WHERE table_id IN (924001,924002)) AS tr24_tables,
  (SELECT COUNT(*) FROM booking_table WHERE booking_id='COPRINT23-BK-001' AND store_id=1 AND table_name LIKE 'TR24%') AS tr24_bindings;
