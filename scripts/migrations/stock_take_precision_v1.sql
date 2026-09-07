-- Restaurant-only candidate: rehearse on an isolated copy before deployment.
-- Existing rounded values cannot be recovered. Do not narrow columns on rollback.
-- No ingredient source data or legal tables are modified.
ALTER TABLE stock_take_detail
  MODIFY COLUMN system_quantity DECIMAL(12,3) NOT NULL,
  MODIFY COLUMN actual_quantity DECIMAL(12,3) NOT NULL,
  MODIFY COLUMN diff_quantity DECIMAL(12,3) NULL,
  MODIFY COLUMN unit_price DECIMAL(15,8) NULL;
