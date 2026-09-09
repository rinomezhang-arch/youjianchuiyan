-- IPAD-BATCH-IDEMPOTENCY-01: apply explicitly before enabling the paired client/server release.
-- No startup migration, backfill, reset or deletion. Existing receipts must never be cleared for retry.
CREATE TABLE IF NOT EXISTS ipad_batch_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    booking_master_id BIGINT NOT NULL,
    booking_id VARCHAR(255) NOT NULL,
    client_request_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    operator_id BIGINT NOT NULL,
    result_json LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_ipad_batch_result_json CHECK (JSON_VALID(result_json)),
    PRIMARY KEY (request_id),
    UNIQUE KEY uk_ipad_batch_scope (store_id, booking_master_id, client_request_id),
    CONSTRAINT fk_ipad_batch_booking FOREIGN KEY (booking_master_id) REFERENCES booking_master(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
