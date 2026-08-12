CREATE TABLE IF NOT EXISTS ipad_device_binding (
  id BIGINT NOT NULL AUTO_INCREMENT,
  device_sn VARCHAR(128) NOT NULL,
  store_id BIGINT NOT NULL,
  staff_id BIGINT NULL,
  device_name VARCHAR(100) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  last_seen_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ipad_device_sn (device_sn),
  KEY idx_ipad_device_store_status (store_id, status),
  KEY idx_ipad_device_staff (staff_id),
  CONSTRAINT fk_ipad_device_store FOREIGN KEY (store_id) REFERENCES store_info(id),
  CONSTRAINT fk_ipad_device_staff FOREIGN KEY (staff_id) REFERENCES staff_master(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
