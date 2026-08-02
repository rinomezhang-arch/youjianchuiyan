-- =====================================================================
-- 各类审批流程初始化 SQL
-- 来源：rbac_init.sql 中审批相关表 + 业务规则
-- 包含：审批模板 / 审批节点 / 审批流关联 / 报销审批配置
-- 维护：地龙（DL-BOT）
-- 更新：2026-08-02
-- =====================================================================

USE banquet;

-- ---------------------------------------------------------------------
-- 1. 审批模板（5 类）
--    表：approval_template
-- ---------------------------------------------------------------------
INSERT INTO `approval_template` (`template_code`, `template_name`, `template_type`, `store_id`, `is_active`, `created_at`, `updated_at`) VALUES
  ('TPL_PROCUREMENT', '采购申请审批', 'procurement', 0, 1, NOW(), NOW()),
  ('TPL_REIMBURSE',   '报销审批',     'reimbursement', 0, 1, NOW(), NOW()),
  ('TPL_PRICE_ADJ',   '调价审批',     'price_adjust', 0, 1, NOW(), NOW()),
  ('TPL_SCHEDULE',    '排班审批',     'schedule',     0, 1, NOW(), NOW()),
  ('TPL_RESIGN',      '离职审批',     'resignation',  0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `template_name` = VALUES(`template_name`);

-- ---------------------------------------------------------------------
-- 2. 审批节点（每类审批的多级节点）
--    表：approval_node
--    node_type：start / approve / reject / end
--    approver_type：role / staff / dept_manager / store_manager / gm
-- ---------------------------------------------------------------------
INSERT INTO `approval_node` (`template_id`, `node_code`, `node_name`, `node_type`, `approver_type`, `approver_role_id`, `sort_order`, `is_active`, `created_at`, `updated_at`) VALUES
  -- 采购申请审批（3 级）
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PROCUREMENT'), 'PROC_START',     '发起',     'start',   'staff',         NULL, 1, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PROCUREMENT'), 'PROC_MGR',       '店长审批', 'approve', 'store_manager', 3,   2, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PROCUREMENT'), 'PROC_GM',        '总经理审批', 'approve', 'gm',           1,   3, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PROCUREMENT'), 'PROC_END',       '结束',     'end',     NULL,            NULL, 4, 1, NOW(), NOW()),

  -- 报销审批（4 级：发起→店长→财务→GM→结束）
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_REIMBURSE'), 'REIM_START',     '发起',     'start',   'staff',         NULL, 1, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_REIMBURSE'), 'REIM_MGR',       '店长审批', 'approve', 'store_manager', 3,   2, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_REIMBURSE'), 'REIM_FIN',      '财务审批', 'approve', 'role',          1,   3, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_REIMBURSE'), 'REIM_GM',        '总经理审批', 'approve', 'gm',           1,   4, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_REIMBURSE'), 'REIM_END',       '结束',     'end',     NULL,            NULL, 5, 1, NOW(), NOW()),

  -- 调价审批（3 级）
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PRICE_ADJ'), 'PRICE_START',    '发起',     'start',   'staff',         NULL, 1, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PRICE_ADJ'), 'PRICE_MGR',     '店长审批', 'approve', 'store_manager', 3,   2, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PRICE_ADJ'), 'PRICE_GM',       '总经理审批', 'approve', 'gm',           1,   3, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_PRICE_ADJ'), 'PRICE_END',     '结束',     'end',     NULL,            NULL, 4, 1, NOW(), NOW()),

  -- 排班审批（2 级）
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_SCHEDULE'), 'SCH_START',      '发起',     'start',   'staff',         NULL, 1, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_SCHEDULE'), 'SCH_MGR',        '店长审批', 'approve', 'store_manager', 3,   2, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_SCHEDULE'), 'SCH_END',        '结束',     'end',     NULL,            NULL, 3, 1, NOW(), NOW()),

  -- 离职审批（3 级）
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_RESIGN'), 'RESIGN_START',    '发起',     'start',   'staff',         NULL, 1, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_RESIGN'), 'RESIGN_MGR',      '店长审批', 'approve', 'store_manager', 3,   2, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_RESIGN'), 'RESIGN_GM',      '总经理审批', 'approve', 'gm',           1,   3, 1, NOW(), NOW()),
  ((SELECT template_id FROM approval_template WHERE template_code='TPL_RESIGN'), 'RESIGN_END',      '结束',     'end',     NULL,            NULL, 4, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `node_name` = VALUES(`node_name`), `approver_type` = VALUES(`approver_type`);

-- ---------------------------------------------------------------------
-- 3. 审批阈值配置（写入 config 表）
--    超过阈值才触发审批，否则直接执行
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('approval.threshold.procurement',   '5000.00',  0),  -- 采购 ≥5000 触发审批
  ('approval.threshold.reimbursement', '1000.00',  0),  -- 报销 ≥1000 触发审批
  ('approval.threshold.price_adjust',  '0.05',     0),  -- 售价变动 ≥5% 触发审批
  ('approval.timeout_hours',           '48',       0),  -- 审批超时 48 小时
  ('approval.timeout_escalation',      'true',     0)   -- 超时自动升级上级
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- ---------------------------------------------------------------------
-- 4. 审批状态机变更记录（写入 change_log 表）
--    所有审批状态变更必须留痕
-- ---------------------------------------------------------------------
-- 字段说明：
-- approval_log 表记录每次审批操作（同意/驳回/撤销）
-- 字段：approval_id / business_type / business_id / node_code / action / approver_id / comment / created_at

-- ---------------------------------------------------------------------
-- 5. 验证查询
-- ---------------------------------------------------------------------
-- 5.1 查询所有审批模板及节点数
SELECT
  t.template_code,
  t.template_name,
  COUNT(n.node_id) AS node_count
FROM `approval_template` t
LEFT JOIN `approval_node` n ON t.template_id = n.template_id AND n.is_active = 1
WHERE t.is_active = 1
GROUP BY t.template_id, t.template_code, t.template_name
ORDER BY t.template_code;

-- 5.2 查询某业务单据的审批历程
-- SELECT
--   al.business_type,
--   al.business_id,
--   n.node_name,
--   al.action,
--   al.approver_id,
--   al.comment,
--   al.created_at
-- FROM approval_log al
-- JOIN approval_node n ON al.node_code = n.node_code
-- WHERE al.business_id = ?
-- ORDER BY al.created_at;

-- ---------------------------------------------------------------------
-- 回滚脚本（如需）
-- ---------------------------------------------------------------------
-- DELETE FROM approval_node WHERE template_id IN (SELECT template_id FROM approval_template WHERE template_code LIKE 'TPL_%');
-- DELETE FROM approval_template WHERE template_code LIKE 'TPL_%';
-- DELETE FROM config WHERE config_key LIKE 'approval.threshold.%' OR config_key LIKE 'approval.timeout%';
