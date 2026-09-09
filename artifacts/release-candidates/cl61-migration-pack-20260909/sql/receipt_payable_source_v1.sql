-- Restaurant-only additive candidate; apply once after backup and schema review.
-- No backfill, no deletion, no modification of legacy purchase_id references.
ALTER TABLE purchase_receipt
  ADD UNIQUE KEY uq_receipt_store_source (store_id, receipt_id);
ALTER TABLE finance_payable
  ADD COLUMN source_receipt_id BIGINT NULL,
  ADD COLUMN source_receipt_no VARCHAR(50) NULL,
  ADD UNIQUE KEY uq_payable_receipt_source (source_receipt_id),
  ADD KEY idx_payable_store_receipt (store_id, source_receipt_id),
  ADD CONSTRAINT fk_payable_store_receipt FOREIGN KEY (store_id, source_receipt_id)
    REFERENCES purchase_receipt (store_id, receipt_id) ON DELETE RESTRICT ON UPDATE RESTRICT;
