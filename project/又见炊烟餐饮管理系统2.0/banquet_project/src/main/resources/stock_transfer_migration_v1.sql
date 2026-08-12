-- ======================================================================
-- 库存调拨主表（修复 P1-17/P2-31）
-- store_id 改 NOT NULL、status 默认值改英文 'draft'、maker_name 增加 maker_id、补 update_time
-- ======================================================================
CREATE TABLE IF NOT EXISTS stock_transfer (
    transfer_id    BIGINT       NOT NULL AUTO_INCREMENT,
    store_id       BIGINT       NOT NULL DEFAULT 1,
    transfer_no    VARCHAR(64)  NULL,
    from_store_id  BIGINT       NULL,
    to_store_id    BIGINT       NULL,
    ingredient_id  VARCHAR(64)  NULL,
    quantity       DECIMAL(12,2) NULL,
    unit           VARCHAR(32)  NULL,
    status         VARCHAR(32)  NULL DEFAULT 'draft' COMMENT 'draft/approved/in_transit/received/cancelled',
    maker_id       BIGINT       NULL COMMENT '制单人ID(关联 staff_master.staff_id)',
    maker_name     VARCHAR(64)  NULL COMMENT '制单人姓名(冗余字段,便于显示)',
    make_date      DATE         NULL,
    remark         TEXT         NULL,
    create_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (transfer_id),
    KEY idx_stock_transfer_no (transfer_no),
    KEY idx_stock_transfer_status (status),
    KEY idx_stock_transfer_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调拨主表';

-- ======================================================================
-- 库存调拨明细表（修复 P1-17）
-- 补 store_id/create_time，transfer_id 加外键 ON DELETE CASCADE
-- ======================================================================
CREATE TABLE IF NOT EXISTS stock_transfer_detail (
    detail_id      BIGINT        NOT NULL AUTO_INCREMENT,
    transfer_id    BIGINT        NOT NULL,
    store_id       BIGINT        NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)',
    ingredient_id  VARCHAR(64)   NULL,
    quantity       DECIMAL(12,2) NULL,
    unit           VARCHAR(32)   NULL,
    remark         VARCHAR(500)  NULL,
    create_time    DATETIME      NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (detail_id),
    KEY idx_stock_transfer_detail_transfer (transfer_id),
    KEY idx_stock_transfer_detail_store (store_id),
    CONSTRAINT fk_std_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfer(transfer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调拨明细表';
