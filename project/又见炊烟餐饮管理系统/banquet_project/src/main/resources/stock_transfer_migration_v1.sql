-- 库存调拨主表
CREATE TABLE IF NOT EXISTS stock_transfer (
    transfer_id    BIGINT       NOT NULL AUTO_INCREMENT,
    store_id       BIGINT       NULL,
    transfer_no    VARCHAR(64)  NULL,
    from_store_id  BIGINT       NULL,
    to_store_id    BIGINT       NULL,
    ingredient_id  VARCHAR(64)  NULL,
    quantity       DECIMAL(12,2) NULL,
    unit           VARCHAR(32)  NULL,
    status         VARCHAR(32)  NULL DEFAULT '草稿',
    maker_name     VARCHAR(64)  NULL,
    make_date      DATE         NULL,
    remark         TEXT         NULL,
    create_time    DATETIME     NULL,
    PRIMARY KEY (transfer_id),
    KEY idx_stock_transfer_no (transfer_no),
    KEY idx_stock_transfer_status (status),
    KEY idx_stock_transfer_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存调拨明细表
CREATE TABLE IF NOT EXISTS stock_transfer_detail (
    detail_id      BIGINT        NOT NULL AUTO_INCREMENT,
    transfer_id    BIGINT        NOT NULL,
    ingredient_id  VARCHAR(64)   NULL,
    quantity       DECIMAL(12,2) NULL,
    unit           VARCHAR(32)   NULL,
    PRIMARY KEY (detail_id),
    KEY idx_stock_transfer_detail_transfer (transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
