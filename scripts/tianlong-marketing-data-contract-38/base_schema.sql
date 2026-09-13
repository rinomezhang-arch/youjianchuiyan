-- base_schema.sql  隔离库 TL38 验证基座：复刻迁移前的三张原始表结构
-- 仅用于验证脚本在隔离 MySQL 13317 内建基线；不含任何真实业务数据。
-- store_info：最小可引用门店（外键父表）
-- marketing_activity：对齐 banquet_full_schema.sql 第 1562 行原始定义（迁移前）
-- booking_inquiry：对齐 create_booking_inquiry_v1.sql 原始定义（迁移前）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS store_info (
  store_id   BIGINT NOT NULL AUTO_INCREMENT,
  store_code VARCHAR(50) NOT NULL,
  store_name VARCHAR(100) NOT NULL,
  PRIMARY KEY (store_id),
  UNIQUE KEY uk_store_code (store_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店信息表（验证用最小结构）';

CREATE TABLE IF NOT EXISTS marketing_activity (
  activity_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT '1',
  activity_code VARCHAR(50) NOT NULL,
  activity_name VARCHAR(100) NOT NULL,
  activity_type VARCHAR(50) NOT NULL,
  start_date DATE DEFAULT NULL,
  end_date DATE DEFAULT NULL,
  is_active TINYINT NOT NULL DEFAULT '1',
  activity_rules TEXT,
  activity_content TEXT,
  target_customers VARCHAR(50) DEFAULT NULL,
  budget_amount DECIMAL(12,2) DEFAULT NULL,
  actual_cost DECIMAL(12,2) DEFAULT '0.00',
  expected_income DECIMAL(12,2) DEFAULT NULL,
  actual_income DECIMAL(12,2) DEFAULT '0.00',
  participant_count INT DEFAULT '0',
  operator_id INT DEFAULT NULL,
  operator_name VARCHAR(50) DEFAULT NULL,
  description VARCHAR(500) DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (activity_id),
  UNIQUE KEY uk_activity_code (activity_code, store_id),
  KEY idx_activity_type (activity_type),
  KEY idx_is_active (is_active),
  KEY idx_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动表';

CREATE TABLE IF NOT EXISTS booking_inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  customer_name VARCHAR(50) NOT NULL,
  customer_phone VARCHAR(20) NOT NULL,
  preferred_date DATE DEFAULT NULL,
  preferred_time VARCHAR(20) DEFAULT NULL,
  guest_count INT DEFAULT NULL,
  selected_dishes TEXT DEFAULT NULL COMMENT '客人挑选的菜品，JSON数组[{dishName,salePrice}]',
  remark VARCHAR(500) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/contacted/confirmed/cancelled',
  staff_note VARCHAR(500) DEFAULT NULL,
  handled_by VARCHAR(50) DEFAULT NULL,
  handled_time TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bi_store_status (store_id, status),
  KEY idx_bi_phone (customer_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
