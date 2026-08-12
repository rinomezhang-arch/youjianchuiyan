CREATE TABLE IF NOT EXISTS energy_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  energy_type VARCHAR(20) NOT NULL,
  meter_reading DECIMAL(14,2) DEFAULT 0,
  daily_usage DECIMAL(14,2) DEFAULT 0,
  daily_cost DECIMAL(14,2) DEFAULT 0,
  recorder VARCHAR(50) DEFAULT '',
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_energy_store_date (store_id, record_date),
  KEY idx_energy_type (energy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS engineering_work_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  order_type VARCHAR(30) NOT NULL DEFAULT 'repair',
  priority VARCHAR(20) NOT NULL DEFAULT 'medium',
  title VARCHAR(120) NOT NULL,
  location VARCHAR(120) DEFAULT '',
  equipment VARCHAR(120) DEFAULT '',
  description VARCHAR(500) DEFAULT '',
  applicant_name VARCHAR(50) DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_engineering_order_no (order_no),
  KEY idx_engineering_store_status (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS engineering_work_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  operator_name VARCHAR(50) DEFAULT '',
  remark VARCHAR(500) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_work_log_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS engineering_inspection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  inspector_name VARCHAR(50) DEFAULT '',
  remark VARCHAR(500) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inspection_store_type (store_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS engineering_spare_part (
  id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  part_name VARCHAR(120) NOT NULL,
  specification VARCHAR(120) DEFAULT '',
  quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
  unit VARCHAR(20) DEFAULT '件',
  min_quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_spare_part_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
