-- 岗位表
CREATE TABLE IF NOT EXISTS post (
    post_id        INT          NOT NULL AUTO_INCREMENT,
    dept_id        INT          NULL,
    post_name      VARCHAR(64)  NULL,
    post_code      VARCHAR(64)  NULL,
    headcount      INT          NULL DEFAULT 0,
    on_duty_count  INT          NULL DEFAULT 0,
    sort_order     INT          NULL DEFAULT 0,
    remark         VARCHAR(255) NULL,
    PRIMARY KEY (post_id),
    KEY idx_post_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
