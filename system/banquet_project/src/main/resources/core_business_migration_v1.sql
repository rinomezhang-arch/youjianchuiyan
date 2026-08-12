-- 核心业务域数据库兼容迁移（幂等）
-- 对齐当前 JPA 实体映射，保留历史字段以兼容旧页面与旧脚本。

-- 桌台实体使用 created_at / updated_at。
ALTER TABLE table_master
  ADD COLUMN IF NOT EXISTS created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE table_master
SET created_at = COALESCE(created_at, create_time),
    updated_at = COALESCE(updated_at, update_time);

-- 采购实体同时读取标准字段和兼容字段。
ALTER TABLE ingredient_purchase
  ADD COLUMN IF NOT EXISTS quantity DECIMAL(10,3) NULL DEFAULT 0.000,
  ADD COLUMN IF NOT EXISTS unit_price DECIMAL(10,2) NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS total_amount DECIMAL(12,2) NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS approved_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS notes TEXT NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE ingredient_purchase
SET quantity = COALESCE(quantity, purchase_quantity),
    unit_price = COALESCE(unit_price, purchase_price),
    total_amount = COALESCE(total_amount, purchase_total),
    created_at = COALESCE(created_at, create_time);

-- 供应商实体的扩展资料与标准时间字段。
ALTER TABLE supplier_master
  ADD COLUMN IF NOT EXISTS phone VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS address TEXT NULL,
  ADD COLUMN IF NOT EXISTS category VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS payment_terms VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS status VARCHAR(255) NULL DEFAULT 'active',
  ADD COLUMN IF NOT EXISTS notes TEXT NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE supplier_master
SET phone = COALESCE(phone, contact_phone),
    notes = COALESCE(notes, remark),
    status = COALESCE(status, IF(is_active = 1, 'active', 'inactive')),
    created_at = COALESCE(created_at, create_time),
    updated_at = COALESCE(updated_at, update_time);

-- 套餐实体的新字段与历史 create_time/update_time 字段兼容。
ALTER TABLE package_master
  ADD COLUMN IF NOT EXISTS created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS category VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) NULL,
  ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2) NULL,
  ADD COLUMN IF NOT EXISTS description TEXT NULL,
  ADD COLUMN IF NOT EXISTS image_url VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS min_guests INT NULL,
  ADD COLUMN IF NOT EXISTS max_guests INT NULL,
  ADD COLUMN IF NOT EXISTS status VARCHAR(255) NULL DEFAULT 'active',
  ADD COLUMN IF NOT EXISTS tags VARCHAR(255) NULL;
UPDATE package_master
SET created_at = COALESCE(created_at, create_time),
    updated_at = COALESCE(updated_at, update_time),
    price = COALESCE(price, package_total_price),
    status = COALESCE(status, IF(is_active = 1, 'active', 'inactive'));

-- 库存流水实体依赖标准创建时间字段。
ALTER TABLE ingredient_inventory_log
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 库存服务与资产负债表共同依赖的实时汇总表。
CREATE TABLE IF NOT EXISTS inventory_summary (
  summary_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  ingredient_id VARCHAR(50) NOT NULL,
  total_quantity DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  total_cost DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  avg_unit_price DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
  last_in_time DATETIME NULL,
  last_out_time DATETIME NULL,
  updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (summary_id),
  UNIQUE KEY uk_inventory_summary_store_ingredient (store_id, ingredient_id),
  KEY idx_inventory_summary_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO inventory_summary
  (store_id, ingredient_id, total_quantity, total_cost, avg_unit_price, updated_at)
SELECT 1, material_id, COALESCE(current_qty, 0),
       COALESCE(current_qty, 0) * COALESCE(avg_cost, 0),
       COALESCE(avg_cost, 0), NOW()
FROM stock_inventory
ON DUPLICATE KEY UPDATE
  total_quantity = VALUES(total_quantity),
  total_cost = VALUES(total_cost),
  avg_unit_price = VALUES(avg_unit_price),
  updated_at = NOW();
