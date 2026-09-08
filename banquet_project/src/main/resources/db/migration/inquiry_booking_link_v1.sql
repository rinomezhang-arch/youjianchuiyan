-- CL-OPS-INQUIRY-CONVERT-07 候选迁移：公开咨询与正式预订的可追踪关联
-- **候选，只在隔离库验证，严禁生产执行。**
--
-- 只做两件事，都兼容历史数据：
--   1. 新增可空列 booking_id —— 历史咨询本来就没转过，留 NULL 即可，不回填。
--   2. 该列加唯一索引 —— 让"同一张正式预订被两条咨询认领"在数据库层不可能发生。
--      MySQL 唯一索引允许多个 NULL，所以全部历史行照旧。
--
-- 不加外键指向 booking_master：booking_master.booking_id 是业务字符串主键之外的列，
-- 且历史数据的完整性未经取证；贸然加外键会让迁移在生产直接失败。
-- 需要强约束时另立任务，先做只读盘点。

ALTER TABLE booking_inquiry
    ADD COLUMN booking_id VARCHAR(20) NULL COMMENT '转正式预订后回填的 booking_id，未转换为 NULL';

ALTER TABLE booking_inquiry
    ADD UNIQUE KEY uk_inquiry_booking_id (booking_id);
