-- Additive restaurant-only migration. Apply once after checking the active database and backup.
-- Existing processing history stays intact; the new source reference is required by the application for new records.
-- Do not apply to production as part of local tests.
ALTER TABLE preprocessing_record
  ADD COLUMN requisition_item_id BIGINT NULL COMMENT 'Source approved material requisition detail',
  ADD INDEX idx_preprocessing_requisition_item (store_id, requisition_item_id);
