-- Verified against the captured 2026-09-07 SHOW CREATE; back up before any release.
-- No historical values are recalculated. Old DECIMAL(10,2) has eight integer digits:
-- DECIMAL(16,8) retains those eight digits while preserving eight fractional digits.
-- Preserve existing ingredient type/default, comments and every FK; no source relinking.
-- Existing orphan/source relationships must be audited separately before production release.
ALTER TABLE stock_take_detail
    MODIFY COLUMN system_quantity DECIMAL(12,3) NOT NULL COMMENT '数量',
    MODIFY COLUMN actual_quantity DECIMAL(12,3) NOT NULL COMMENT '数量',
    MODIFY COLUMN diff_quantity DECIMAL(12,3) NULL DEFAULT 0.000 COMMENT '数量',
    MODIFY COLUMN unit_price DECIMAL(16,8) NULL DEFAULT NULL COMMENT '单价';
