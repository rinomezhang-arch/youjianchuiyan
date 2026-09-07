-- Configuration only. No device execution or changes to existing tables.
CREATE TABLE IF NOT EXISTS restaurant_print_printer (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 name VARCHAR(80) NOT NULL,
 type VARCHAR(20) NOT NULL,
 paper_width VARCHAR(2) NOT NULL,
 copies INT NOT NULL,
 address VARCHAR(255) NOT NULL DEFAULT '',
 archived BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_print_printer_store(id,store_id),
 CONSTRAINT fk_print_printer_store FOREIGN KEY(store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT,
 CHECK(store_id > 0),
 CHECK(type IN ('network','usb','bluetooth','browser')),
 CHECK(paper_width IN ('58','80','A4')),
 CHECK(copies BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS restaurant_print_rule (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL,
 name VARCHAR(80) NOT NULL,
 printer_id BIGINT NOT NULL,
 document_type VARCHAR(30) NOT NULL,
 configured_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 archived BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_print_rule_printer_store(printer_id,store_id),
 CONSTRAINT fk_print_rule_store FOREIGN KEY(store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT,
 CONSTRAINT fk_print_rule_printer_store FOREIGN KEY(printer_id,store_id)
 REFERENCES restaurant_print_printer(id,store_id) ON DELETE RESTRICT,
 CHECK(store_id > 0),
 CHECK(document_type IN ('receipt','kitchen','refund','daily_report'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Also upgrade a previous isolated candidate whose tables already exist.
-- Existing orphan rows cause ALTER to fail; never silently delete or remap them.
SET @print_fk_sql = IF(EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='restaurant_print_printer' AND CONSTRAINT_NAME='fk_print_printer_store'), 'SELECT 1', 'ALTER TABLE restaurant_print_printer ADD CONSTRAINT fk_print_printer_store FOREIGN KEY(store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT');
PREPARE print_fk_stmt FROM @print_fk_sql;
EXECUTE print_fk_stmt;
DEALLOCATE PREPARE print_fk_stmt;
SET @print_fk_sql = IF(EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='restaurant_print_rule' AND CONSTRAINT_NAME='fk_print_rule_store'), 'SELECT 1', 'ALTER TABLE restaurant_print_rule ADD CONSTRAINT fk_print_rule_store FOREIGN KEY(store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT');
PREPARE print_fk_stmt FROM @print_fk_sql;
EXECUTE print_fk_stmt;
DEALLOCATE PREPARE print_fk_stmt;
