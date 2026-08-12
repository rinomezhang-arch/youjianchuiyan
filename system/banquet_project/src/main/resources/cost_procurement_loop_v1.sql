-- 菜单—成本—采购—库存—领料闭环迁移 v1
-- MySQL 8 / MariaDB 10.5 兼容；执行前请备份。所有新增结构均为增量，不删除历史数据。

CREATE TABLE IF NOT EXISTS data_import_batch (
  import_batch_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  source_file VARCHAR(255) NOT NULL,
  source_hash VARCHAR(64) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PREVIEW',
  dish_count INT NOT NULL DEFAULT 0,
  ingredient_count INT NOT NULL DEFAULT 0,
  cost_card_count INT NOT NULL DEFAULT 0,
  draft_recipe_count INT NOT NULL DEFAULT 0,
  warning_count INT NOT NULL DEFAULT 0,
  error_count INT NOT NULL DEFAULT 0,
  summary_json LONGTEXT,
  imported_by VARCHAR(100),
  imported_at DATETIME,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (import_batch_id),
  UNIQUE KEY uk_import_source (store_id, source_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_import_issue (
  issue_id BIGINT NOT NULL AUTO_INCREMENT,
  import_batch_id BIGINT NOT NULL,
  sheet_name VARCHAR(100) NOT NULL,
  source_row INT,
  entity_type VARCHAR(30) NOT NULL,
  source_value VARCHAR(500),
  issue_code VARCHAR(50) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  message VARCHAR(500) NOT NULL,
  resolved TINYINT(1) NOT NULL DEFAULT 0,
  resolution VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (issue_id),
  KEY idx_import_issue_batch (import_batch_id, severity, resolved),
  CONSTRAINT fk_import_issue_batch FOREIGN KEY (import_batch_id) REFERENCES data_import_batch(import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_draft (
  recipe_draft_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  dish_id VARCHAR(50) NOT NULL,
  source_text VARCHAR(500) NOT NULL,
  source_sheet VARCHAR(100),
  source_row INT,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  review_note VARCHAR(500),
  reviewed_by VARCHAR(100),
  reviewed_at DATETIME,
  import_batch_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (recipe_draft_id),
  KEY idx_recipe_draft_dish (store_id, dish_id, status),
  CONSTRAINT fk_recipe_draft_batch FOREIGN KEY (import_batch_id) REFERENCES data_import_batch(import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_draft_detail (
  draft_detail_id BIGINT NOT NULL AUTO_INCREMENT,
  recipe_draft_id BIGINT NOT NULL,
  token VARCHAR(100) NOT NULL,
  ingredient_id VARCHAR(50),
  ingredient_name VARCHAR(100),
  match_status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED',
  match_confidence DECIMAL(5,2),
  gross_quantity DECIMAL(14,4),
  net_quantity DECIMAL(14,4),
  unit VARCHAR(20),
  yield_rate DECIMAL(7,4),
  line_no INT NOT NULL,
  PRIMARY KEY (draft_detail_id),
  KEY idx_draft_detail_parent (recipe_draft_id),
  CONSTRAINT fk_draft_detail_parent FOREIGN KEY (recipe_draft_id) REFERENCES recipe_draft(recipe_draft_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ingredient_price_history (
  price_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  ingredient_id VARCHAR(50) NOT NULL,
  ingredient_name VARCHAR(100) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  unit_price DECIMAL(16,6) NOT NULL,
  effective_from DATETIME NOT NULL,
  effective_to DATETIME,
  source_type VARCHAR(30) NOT NULL,
  source_id VARCHAR(100),
  import_batch_id BIGINT,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (price_id),
  KEY idx_price_effective (store_id, ingredient_id, is_active, effective_from),
  CONSTRAINT chk_ingredient_price_positive CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS material_requirement_snapshot (
  requirement_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  requirement_no VARCHAR(50) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  source_id VARCHAR(100) NOT NULL,
  menu_id VARCHAR(50),
  banquet_order_id VARCHAR(50),
  cost_card_version VARCHAR(50),
  serving_count DECIMAL(14,4) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'CALCULATED',
  calculated_by VARCHAR(100),
  calculated_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (requirement_id),
  UNIQUE KEY uk_requirement_no (requirement_no),
  KEY idx_requirement_source (store_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS material_requirement_detail (
  requirement_detail_id BIGINT NOT NULL AUTO_INCREMENT,
  requirement_id BIGINT NOT NULL,
  dish_id VARCHAR(50) NOT NULL,
  cost_card_id BIGINT NOT NULL,
  ingredient_id VARCHAR(50) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  net_quantity DECIMAL(14,4) NOT NULL,
  yield_rate DECIMAL(7,4) NOT NULL,
  gross_quantity DECIMAL(14,4) NOT NULL,
  available_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  in_transit_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  suggested_purchase_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  PRIMARY KEY (requirement_detail_id),
  KEY idx_requirement_detail_parent (requirement_id),
  CONSTRAINT fk_requirement_detail_parent FOREIGN KEY (requirement_id) REFERENCES material_requirement_snapshot(requirement_id),
  CONSTRAINT chk_requirement_yield CHECK (yield_rate > 0 AND yield_rate <= 1),
  CONSTRAINT chk_requirement_quantity CHECK (net_quantity > 0 AND gross_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS procurement_request_detail (
  request_detail_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  requirement_detail_id BIGINT,
  ingredient_id VARCHAR(50) NOT NULL,
  ingredient_name VARCHAR(100) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  requested_quantity DECIMAL(14,4) NOT NULL,
  approved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  ordered_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  estimated_unit_price DECIMAL(16,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (request_detail_id),
  KEY idx_procurement_request_detail (request_id),
  CONSTRAINT chk_procurement_request_quantity CHECK (requested_quantity > 0 AND approved_quantity >= 0 AND ordered_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS procurement_approval_record (
  approval_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  business_type VARCHAR(30) NOT NULL,
  business_id BIGINT NOT NULL,
  approval_level INT NOT NULL,
  from_status VARCHAR(40) NOT NULL,
  to_status VARCHAR(40) NOT NULL,
  action VARCHAR(30) NOT NULL,
  approver_id BIGINT,
  approver_name VARCHAR(100) NOT NULL,
  comment VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (approval_id),
  KEY idx_procurement_approval (business_type, business_id, approval_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receipt_quality_detail (
  quality_detail_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  receipt_id BIGINT NOT NULL,
  order_detail_id BIGINT NOT NULL,
  ingredient_id VARCHAR(50) NOT NULL,
  delivered_quantity DECIMAL(14,4) NOT NULL,
  accepted_quantity DECIMAL(14,4) NOT NULL,
  rejected_quantity DECIMAL(14,4) NOT NULL,
  rejection_reason VARCHAR(500),
  batch_no VARCHAR(100),
  production_date DATE,
  expiry_date DATE,
  actual_unit_price DECIMAL(16,6) NOT NULL,
  quality_result VARCHAR(30) NOT NULL,
  inspector_id BIGINT,
  inspector_name VARCHAR(100),
  idempotency_key VARCHAR(100) NOT NULL,
  confirmed_at DATETIME,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (quality_detail_id),
  UNIQUE KEY uk_receipt_idempotency (idempotency_key),
  KEY idx_receipt_quality (receipt_id, order_detail_id),
  CONSTRAINT chk_receipt_quantities CHECK (delivered_quantity > 0 AND accepted_quantity >= 0 AND rejected_quantity >= 0 AND accepted_quantity + rejected_quantity = delivered_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_batch_ledger (
  inventory_batch_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  ingredient_id VARCHAR(50) NOT NULL,
  batch_no VARCHAR(100) NOT NULL,
  warehouse_id BIGINT,
  received_quantity DECIMAL(14,4) NOT NULL,
  available_quantity DECIMAL(14,4) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  unit_cost DECIMAL(16,6) NOT NULL,
  production_date DATE,
  expiry_date DATE,
  receipt_quality_detail_id BIGINT,
  status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (inventory_batch_id),
  UNIQUE KEY uk_inventory_batch (store_id, ingredient_id, batch_no),
  KEY idx_inventory_fifo (store_id, ingredient_id, status, expiry_date, created_at),
  CONSTRAINT chk_batch_quantity CHECK (received_quantity >= 0 AND available_quantity >= 0 AND available_quantity <= received_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS material_issue_detail (
  issue_detail_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  requisition_id BIGINT NOT NULL,
  ingredient_id VARCHAR(50) NOT NULL,
  requested_quantity DECIMAL(14,4) NOT NULL,
  approved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  issued_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  returned_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  unit VARCHAR(20) NOT NULL,
  source_requirement_detail_id BIGINT,
  over_issue_reason VARCHAR(500),
  substitute_ingredient_id VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (issue_detail_id),
  KEY idx_material_issue_requisition (requisition_id),
  CONSTRAINT chk_material_issue_quantity CHECK (requested_quantity > 0 AND approved_quantity >= 0 AND issued_quantity >= 0 AND returned_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_movement_ledger (
  movement_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL DEFAULT 1,
  ingredient_id VARCHAR(50) NOT NULL,
  inventory_batch_id BIGINT,
  movement_type VARCHAR(30) NOT NULL,
  business_type VARCHAR(30) NOT NULL,
  business_id BIGINT NOT NULL,
  quantity DECIMAL(14,4) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  unit_cost DECIMAL(16,6) NOT NULL,
  amount DECIMAL(18,6) NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(100),
  occurred_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (movement_id),
  UNIQUE KEY uk_movement_idempotency (idempotency_key),
  KEY idx_movement_trace (business_type, business_id),
  KEY idx_movement_ingredient (store_id, ingredient_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 现有成本卡扩展：版本化、审批与价格快照。
ALTER TABLE dish_cost_card
  ADD COLUMN IF NOT EXISTS version_no INT NOT NULL DEFAULT 1 AFTER dish_id,
  ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' AFTER status,
  ADD COLUMN IF NOT EXISTS effective_from DATETIME NULL AFTER effective_date,
  ADD COLUMN IF NOT EXISTS effective_to DATETIME NULL AFTER effective_from,
  ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100) NULL AFTER created_by,
  ADD COLUMN IF NOT EXISTS approved_at DATETIME NULL AFTER approved_by,
  ADD COLUMN IF NOT EXISTS source_import_batch_id BIGINT NULL AFTER approved_at;

ALTER TABLE dish_cost_card_detail
  ADD COLUMN IF NOT EXISTS gross_quantity DECIMAL(14,4) NULL AFTER standard_quantity,
  ADD COLUMN IF NOT EXISTS net_quantity DECIMAL(14,4) NULL AFTER gross_quantity,
  ADD COLUMN IF NOT EXISTS loss_rate DECIMAL(7,4) NULL AFTER yield_rate,
  ADD COLUMN IF NOT EXISTS price_id BIGINT NULL AFTER unit_price,
  ADD COLUMN IF NOT EXISTS price_snapshot DECIMAL(16,6) NULL AFTER price_id;

ALTER TABLE purchase_order
  ADD COLUMN IF NOT EXISTS request_id BIGINT NULL AFTER store_id,
  ADD COLUMN IF NOT EXISTS manager_approved_by VARCHAR(100) NULL AFTER approver_name,
  ADD COLUMN IF NOT EXISTS manager_approved_at DATETIME NULL AFTER manager_approved_by,
  ADD COLUMN IF NOT EXISTS gm_approved_by VARCHAR(100) NULL AFTER manager_approved_at,
  ADD COLUMN IF NOT EXISTS gm_approved_at DATETIME NULL AFTER gm_approved_by;

ALTER TABLE requisition_order
  ADD COLUMN IF NOT EXISTS source_requirement_id BIGINT NULL AFTER store_id,
  ADD COLUMN IF NOT EXISTS approval_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT' AFTER status,
  ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS approved_at DATETIME NULL;
