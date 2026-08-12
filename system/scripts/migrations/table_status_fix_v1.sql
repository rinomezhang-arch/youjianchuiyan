-- =====================================================================
-- table_status 枚举统一数据修复 v1
-- 来源：项目资料档案.md 第 8.2.9 节"代码 vs 字典不一致"修复
-- 操作：将历史数据 table_master.table_status='available' 统一为 'idle'
--      与 sys_dict_item 中字典编码 'idle' 对齐
-- 安全：UPDATE 前先 SELECT 备份，可回滚
-- =====================================================================

USE banquet;

-- ---------------------------------------------------------------------
-- 0. 备份待修复数据（用于回滚）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `_backup_table_master_status_v1`;
CREATE TABLE `_backup_table_master_status_v1` AS
SELECT table_id, store_id, table_status, NOW() AS backup_time
FROM `table_master`
WHERE `table_status` = 'available';

SELECT CONCAT('待修复记录数: ', COUNT(*)) AS info FROM `_backup_table_master_status_v1`;

-- ---------------------------------------------------------------------
-- 1. 修复 table_master.table_status
-- ---------------------------------------------------------------------
UPDATE `table_master`
SET `table_status` = 'idle'
WHERE `table_status` = 'available';

SELECT
  table_status,
  COUNT(*) AS cnt
FROM `table_master`
GROUP BY table_status
ORDER BY table_status;

-- ---------------------------------------------------------------------
-- 2. 同步修复 booking_master 中冗余的状态字段（如有）
--    booking_master.status 默认 'pending'，与 table_status 无关，跳过
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 3. 验证：所有 table_status 取值应全部在字典范围内
--    字典合法值：idle / occupied / reserved（available 已下线）
-- ---------------------------------------------------------------------
SELECT
  tm.table_status,
  COUNT(*) AS cnt,
  CASE
    WHEN di.item_label IS NOT NULL THEN CONCAT('✅ 字典匹配: ', di.item_label)
    ELSE '❌ 字典缺失，需补全'
  END AS dict_check
FROM `table_master` tm
LEFT JOIN `sys_dict_item` di
  ON di.dict_code = 'table_status'
  AND di.item_value = tm.table_status
  AND di.is_active = 1
GROUP BY tm.table_status, di.item_label
ORDER BY tm.table_status;

-- ---------------------------------------------------------------------
-- 4. 删除兼容别名（确认代码已统一后执行，默认注释）
--    字典 item_id=63 的 'available' 别名，待代码全部统一后下线
-- ---------------------------------------------------------------------
-- DELETE FROM `sys_dict_item` WHERE item_id = 63 AND dict_code = 'table_status' AND item_value = 'available';

-- ---------------------------------------------------------------------
-- 回滚脚本（如需）
-- ---------------------------------------------------------------------
-- UPDATE `table_master` tm
-- JOIN `_backup_table_master_status_v1` b ON tm.table_id = b.table_id
-- SET tm.table_status = 'available';
-- DROP TABLE `_backup_table_master_status_v1`;
