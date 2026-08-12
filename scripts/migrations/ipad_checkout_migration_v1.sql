CREATE TABLE IF NOT EXISTS ipad_payment_request (
  payment_request_id BIGINT NOT NULL AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  booking_id VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  pay_type VARCHAR(20) NOT NULL,
  operator_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_request_id),
  UNIQUE KEY uk_ipad_payment_store_key (store_id, idempotency_key),
  KEY idx_ipad_payment_booking (store_id, booking_id),
  CONSTRAINT chk_ipad_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='iPad收款幂等请求记录';
