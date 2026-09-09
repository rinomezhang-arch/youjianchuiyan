-- IPAD-BATCH-SCOPE-FK-02. Explicit MySQL 8 migration after v1; never run on startup.
-- Before execution: inspect SHOW CREATE TABLE and information_schema COLUMNS/STATISTICS.
-- Required matching signed BIGINT ids/stores and matching booking_id charset/collation.
-- Verified test baseline: VARCHAR(255), utf8mb4 / utf8mb4_0900_ai_ci on both sides.
-- Keep foreign_key_checks=1. Inspect receipt scope mismatches first; any mismatch means STOP.
-- No data correction, deletion, or conversion is part of this migration.
-- Existing exact full-column UNIQUE(id,store_id,booking_id): verify and skip STEP 1.
-- Same index name with another definition: STOP. Do not drop or replace an existing index.
-- PRIMARY(id) and UNIQUE(booking_id) alone do not supply the three-column referenced key.
-- MySQL DDL implicitly commits: these steps are NOT one atomic transaction.
-- On STEP 2 failure, the STEP 1 index may remain. Stop and inspect state, never clean rows.
-- Retry only missing steps after verifying exact index/FK column order and referenced table.
-- Both exact constraints already present: verify historical receipts, execute no DDL.
-- STEP 1
ALTER TABLE booking_master
    ADD UNIQUE INDEX uk_booking_master_id_store_booking (id, store_id, booking_id);
-- STEP 2: retain fk_ipad_batch_booking and the existing submission unique key.
ALTER TABLE ipad_batch_request
    ADD CONSTRAINT fk_ipad_batch_booking_scope
    FOREIGN KEY (booking_master_id, store_id, booking_id)
    REFERENCES booking_master (id, store_id, booking_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
