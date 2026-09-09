-- CO-RECEIPT-R1-29 explicit one-time additive fixture; exact preserved schema only.
USE co_print23_20260909_022305;

ALTER TABLE booking_master ADD COLUMN booking_time time DEFAULT NULL, ADD COLUMN staff_name varchar(20) DEFAULT NULL, ADD COLUMN updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE TABLE `table_master` (
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

CREATE TABLE `booking_table` (
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
  CONSTRAINT `fk_booking_master` FOREIGN KEY (`booking_master_id`) REFERENCES `booking_master` (`id`),
  CONSTRAINT `fk_bt_table` FOREIGN KEY (`table_id`) REFERENCES `table_master` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `finance_transaction` (
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

START TRANSACTION;
INSERT INTO table_master (table_id,store_id,table_number,table_name,remark) VALUES
(929001,1,'TR29-A','TR29-A','CO-RECEIPT-R1-29 synthetic'),
(929002,1,'TR29-B','TR29-B','CO-RECEIPT-R1-29 synthetic');
INSERT INTO booking_table (table_booking_id,booking_master_id,booking_table_code,store_id,booking_id,booking_date,table_id,table_number,table_name,guest_count,table_note)
SELECT 929001,id,'TR29-A',store_id,booking_id,booking_date,929001,'TR29-A','TR29-A',guest_count,'CO-RECEIPT-R1-29 synthetic' FROM booking_master WHERE booking_id='COPRINT23-BK-001' AND store_id=1;
INSERT INTO booking_table (table_booking_id,booking_master_id,booking_table_code,store_id,booking_id,booking_date,table_id,table_number,table_name,guest_count,table_note)
SELECT 929002,id,'TR29-B',store_id,booking_id,booking_date,929002,'TR29-B','TR29-B',guest_count,'CO-RECEIPT-R1-29 synthetic' FROM booking_master WHERE booking_id='COPRINT23-BK-001' AND store_id=1;
COMMIT;
