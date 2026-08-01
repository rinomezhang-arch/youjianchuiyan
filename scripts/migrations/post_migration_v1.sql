-- 岗位表（修复 P1-26）
-- 补 store_id/create_time/update_time/status 实现多租户隔离与审计追踪
CREATE TABLE IF NOT EXISTS post (
    post_id        INT          NOT NULL AUTO_INCREMENT,
    store_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    dept_id        INT          NULL COMMENT '部门ID(关联 department.dept_id INT)',
    post_name      VARCHAR(64)  NULL,
    post_code      VARCHAR(64)  NULL,
    headcount      INT          NULL DEFAULT 0 COMMENT '编制人数',
    on_duty_count  INT          NULL DEFAULT 0 COMMENT '在岗人数',
    sort_order     INT          NULL DEFAULT 0,
    status         VARCHAR(20)  NULL DEFAULT 'active' COMMENT 'active/inactive',
    remark         VARCHAR(255) NULL,
    create_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    KEY idx_post_dept (dept_id),
    KEY idx_post_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';
