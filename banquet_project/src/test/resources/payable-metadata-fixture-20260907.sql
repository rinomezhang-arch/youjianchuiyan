-- finance_payable columns/indexes from Tianlong's corrected 20260907 metadata.
-- ingredient_purchase is ONLY a referenced-key stub, not its full production schema.
-- Do not claim this fixture reproduces the entire production database.
CREATE TABLE ingredient_purchase (purchase_id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE finance_payable (
 payable_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 store_id BIGINT NOT NULL DEFAULT 1,
 payable_no VARCHAR(50) NOT NULL,
 supplier_id INT NULL,
 supplier_name VARCHAR(100) NULL,
 purchase_id BIGINT NULL,
 purchase_no VARCHAR(50) NULL,
 total_amount DECIMAL(12,2) NOT NULL,
 paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
 pending_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
 payable_date DATE NULL,
 due_date DATE NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'unpaid',
 credit_days INT NULL,
 operator_id INT NULL,
 operator_name VARCHAR(50) NULL,
 remark VARCHAR(500) NULL,
 created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_payable_no(payable_no), KEY idx_store_id(store_id),
 KEY idx_supplier_id(supplier_id), KEY idx_status(status), KEY idx_due_date(due_date),
 CONSTRAINT fk_fp_purchase FOREIGN KEY(purchase_id) REFERENCES ingredient_purchase(purchase_id),
 CONSTRAINT fk_fp_supplier FOREIGN KEY(supplier_id) REFERENCES supplier_master(supplier_id)
);
