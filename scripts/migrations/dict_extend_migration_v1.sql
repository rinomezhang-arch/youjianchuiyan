-- =====================================================================
-- 数据字典补全 v1
-- 来源：项目资料档案.md 第 8.3 节"代码内枚举未入字典"补全
-- 新增 7 类字典 + 27 项明细，统一管理散落在代码中的硬编码状态
-- 字典 ID 从 11 起（sys_dict 已有 1~10）
-- 字典 item_id 从 32 起（sys_dict_item 已有 1~31）
-- =====================================================================

USE banquet;

-- ---------------------------------------------------------------------
-- 1. 新增字典分类（7 类）
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict` (`dict_id`, `dict_code`, `dict_name`, `dict_type`, `store_id`, `description`, `sort_order`, `is_active`, `created_at`, `updated_at`) VALUES
  (11, 'kitchen_status',         '厨房状态',     'list', 0, '菜品后厨出单状态枚举',         11, 1, NOW(), NOW()),
  (12, 'payment_status',          '支付状态',     'list', 0, '订单/预订支付状态枚举',       12, 1, NOW(), NOW()),
  (13, 'booking_type',            '预订类型',     'list', 0, '宴会预订业务类型枚举',       13, 1, NOW(), NOW()),
  (14, 'occasion_type',           '宴会场合',     'list', 0, '宴会场景类型枚举',           14, 1, NOW(), NOW()),
  (15, 'reimbursement_status',    '报销状态',     'list', 0, '报销单审批/支付状态枚举',     15, 1, NOW(), NOW()),
  (16, 'stock_transfer_status',   '调拨状态',     'list', 0, '库存调拨单状态枚举',         16, 1, NOW(), NOW()),
  (17, 'tool_issue_status',       '工具领用状态', 'list', 0, '工具领用/归还状态枚举',       17, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `dict_name` = VALUES(`dict_name`), `description` = VALUES(`description`);

-- ---------------------------------------------------------------------
-- 2. 新增字典明细（27 项）
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict_item` (`item_id`, `dict_id`, `dict_code`, `item_value`, `item_label`, `parent_id`, `store_id`, `sort_order`, `is_active`, `remark`, `created_at`, `updated_at`) VALUES
  -- kitchen_status 厨房状态
  (32,  11, 'kitchen_status',       'pending',      '待出单',   NULL, 0, 1, 1, '已下单待后厨接单',   NOW(), NOW()),
  (33,  11, 'kitchen_status',       'cooking',      '烹饪中',   NULL, 0, 2, 1, '后厨正在制作',       NOW(), NOW()),
  (34,  11, 'kitchen_status',       'served',       '已出菜',   NULL, 0, 3, 1, '菜品已上桌',         NOW(), NOW()),
  (35,  11, 'kitchen_status',       'cancelled',    '已取消',   NULL, 0, 4, 1, '客户取消或退菜',     NOW(), NOW()),
  -- payment_status 支付状态
  (36,  12, 'payment_status',        'unpaid',       '未支付',   NULL, 0, 1, 1, NULL, NOW(), NOW()),
  (37,  12, 'payment_status',        'partial',      '部分支付', NULL, 0, 2, 1, '部分订金已付',       NOW(), NOW()),
  (38,  12, 'payment_status',        'paid',         '已支付',   NULL, 0, 3, 1, '全额已付',           NOW(), NOW()),
  (39,  12, 'payment_status',        'refunding',    '退款中',   NULL, 0, 4, 1, NULL, NOW(), NOW()),
  (40,  12, 'payment_status',        'refunded',     '已退款',   NULL, 0, 5, 1, NULL, NOW(), NOW()),
  -- booking_type 预订类型
  (41,  13, 'booking_type',          'normal',       '常规预订', NULL, 0, 1, 1, '默认类型',           NOW(), NOW()),
  (42,  13, 'booking_type',          'banquet',      '宴会预订', NULL, 0, 2, 1, '大型宴会',           NOW(), NOW()),
  (43,  13, 'booking_type',          'a_la_carte',   '零点预订', NULL, 0, 3, 1, '散客零点',           NOW(), NOW()),
  (44,  13, 'booking_type',          'package',      '套餐预订', NULL, 0, 4, 1, '已选套餐',           NOW(), NOW()),
  -- occasion_type 宴会场合
  (45,  14, 'occasion_type',         'birthday',     '生日宴',   NULL, 0, 1, 1, NULL, NOW(), NOW()),
  (46,  14, 'occasion_type',         'wedding',      '婚宴',     NULL, 0, 2, 1, NULL, NOW(), NOW()),
  (47,  14, 'occasion_type',         'business',     '商务宴',   NULL, 0, 3, 1, NULL, NOW(), NOW()),
  (48,  14, 'occasion_type',         'family',       '家宴',     NULL, 0, 4, 1, NULL, NOW(), NOW()),
  (49,  14, 'occasion_type',         'friends',     '朋友聚餐', NULL, 0, 5, 1, NULL, NOW(), NOW()),
  (50,  14, 'occasion_type',         'a_la_carte',   '零点',     NULL, 0, 6, 1, '非宴会场景',         NOW(), NOW()),
  -- reimbursement_status 报销状态
  (51,  15, 'reimbursement_status',  'draft',        '草稿',     NULL, 0, 1, 1, '保存未提交',         NOW(), NOW()),
  (52,  15, 'reimbursement_status',  'pending',      '待审批',   NULL, 0, 2, 1, NULL, NOW(), NOW()),
  (53,  15, 'reimbursement_status',  'approved',     '已通过',   NULL, 0, 3, 1, NULL, NOW(), NOW()),
  (54,  15, 'reimbursement_status',  'rejected',     '已驳回',   NULL, 0, 4, 1, NULL, NOW(), NOW()),
  (55,  15, 'reimbursement_status',  'paid',         '已支付',   NULL, 0, 5, 1, '财务已出款',         NOW(), NOW()),
  -- stock_transfer_status 调拨状态
  (56,  16, 'stock_transfer_status', 'pending',      '待调出',   NULL, 0, 1, 1, NULL, NOW(), NOW()),
  (57,  16, 'stock_transfer_status', 'in_transit',   '运输中',   NULL, 0, 2, 1, '已调出未入库',       NOW(), NOW()),
  (58,  16, 'stock_transfer_status', 'completed',    '已入库',   NULL, 0, 3, 1, '调拨完成',           NOW(), NOW()),
  -- tool_issue_status 工具领用状态
  (59,  17, 'tool_issue_status',     'issued',       '已领用',   NULL, 0, 1, 1, NULL, NOW(), NOW()),
  (60,  17, 'tool_issue_status',     'returned',     '已归还',   NULL, 0, 2, 1, NULL, NOW(), NOW()),
  (61,  17, 'tool_issue_status',     'damaged',      '已损坏',   NULL, 0, 3, 1, '归还时报损',         NOW(), NOW()),
  (62,  17, 'tool_issue_status',     'lost',         '已丢失',   NULL, 0, 4, 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE `item_label` = VALUES(`item_label`), `remark` = VALUES(`remark`);

-- ---------------------------------------------------------------------
-- 3. 修复现有 table_status 字典不一致问题
--    档案第 8.2.9 节：代码用 'available'，字典定义为 'idle'
--    方案：字典新增 'available' 别名（兼容历史数据），保持代码不动
--    【注意】此为兼容方案，待 BookingController 代码统一后再考虑删除别名
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict_item` (`item_id`, `dict_id`, `dict_code`, `item_value`, `item_label`, `parent_id`, `store_id`, `sort_order`, `is_active`, `remark`, `created_at`, `updated_at`) VALUES
  (63, 9, 'table_status', 'available', '空闲(旧)', NULL, 0, 0, 1, '历史兼容别名，与 idle 等价，待代码统一后下线', NOW(), NOW())
ON DUPLICATE KEY UPDATE `item_label` = VALUES(`item_label`), `remark` = VALUES(`remark`);

-- ---------------------------------------------------------------------
-- 验证查询：按字典分类列出所有明细
-- ---------------------------------------------------------------------
SELECT
  d.dict_code,
  d.dict_name,
  i.item_value,
  i.item_label,
  i.sort_order,
  i.remark
FROM `sys_dict` d
LEFT JOIN `sys_dict_item` i ON d.dict_id = i.dict_id
ORDER BY d.sort_order, i.sort_order;
