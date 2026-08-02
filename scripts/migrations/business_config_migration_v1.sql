-- =====================================================================
-- 业务规则配置初始化 v1
-- 模块：退款 / 违约金 / 预订时限 / 订金 / 桌台占用 / 套餐替换
-- 来源：项目资料档案.md 第 4 节缺失项补全
-- 约束：config 表 PRIMARY KEY=config_key（不含 store_id），
--       故采用 "<rule>.<scope>__s<store_id>" 命名约定支持分门店
-- store_id 约定：0=全局默认，1=宁国店，2=宣城店
-- 默认值参考餐饮行业通行规则，标注【待业务确认】需用户复核
-- =====================================================================

USE banquet;

-- 清理历史测试数据（保留 config 表结构）
DELETE FROM `config` WHERE `config_key` = 'test';

-- ---------------------------------------------------------------------
-- 1. 退款比例（按距开餐时间分级）
--    退款比例 = 应退金额 / 已付订金
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  -- 全局默认（store_id=0）
  ('refund_ratio.full',          '1.00',  0),  -- 开餐前 ≥24h：退 100%
  ('refund_ratio.half',          '0.50',  0),  -- 开餐前 24h~6h：退 50%
  ('refund_ratio.none',          '0.00',  0),  -- 开餐前 <6h：不退
  -- 宁国店（store_id=1）—— 采用全局默认
  ('refund_ratio.full__s1',      '1.00',  1),
  ('refund_ratio.half__s1',       '0.50',  1),
  ('refund_ratio.none__s1',       '0.00',  1),
  -- 宣城店（store_id=2）—— 【待业务确认】是否差异化
  ('refund_ratio.full__s2',      '1.00',  2),
  ('refund_ratio.half__s2',       '0.50',  2),
  ('refund_ratio.none__s2',       '0.00',  2)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 2. 违约金比例（取消预订时向客户收取）
--    违约金 = 订单总额 × 违约金比例
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('cancellation_penalty.default',   '0.20', 0),  -- 默认 20%
  ('cancellation_penalty.default__s1','0.20', 1),
  ('cancellation_penalty.default__s2','0.20', 2),
  -- 阶梯违约金（按取消时间）
  ('cancellation_penalty.tier1',     '0.00', 0),  -- 开餐前 ≥48h：0%
  ('cancellation_penalty.tier2',     '0.10', 0),  -- 开餐前 48~24h：10%
  ('cancellation_penalty.tier3',     '0.30', 0),  -- 开餐前 24~6h：30%
  ('cancellation_penalty.tier4',     '0.50', 0)  -- 开餐前 <6h：50%
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 3. 预订时限
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('booking.advance_hours_min',     '2',    0),   -- 最早提前 2 小时可预订
  ('booking.advance_days_max',      '90',   0),   -- 最晚提前 90 天可预订
  ('booking.cancel_hours_min',      '1',    0),   -- 开餐前 1 小时内不可取消
  ('booking.hold_minutes',          '30',   0),   -- 桌位保留 30 分钟（超时自动释放）
  ('booking.advance_hours_min__s1', '2',    1),
  ('booking.advance_hours_min__s2', '2',    2),
  ('booking.advance_days_max__s1',  '90',   1),
  ('booking.advance_days_max__s2',  '90',   2)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 4. 订金规则
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('booking.deposit_ratio',          '0.30', 0),  -- 订金比例 30%
  ('booking.deposit_min_amount',     '500.00', 0),  -- 最低订金 500 元
  ('booking.deposit_refundable',     'true',  0),   -- 订金可退（按退款比例规则）
  ('booking.deposit_ratio__s1',      '0.30',  1),
  ('booking.deposit_ratio__s2',      '0.30',  2)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 5. 桌台占用时长
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('table.occupancy_hours_max',      '4',    0),   -- 用餐时长上限 4 小时
  ('table.occupancy_warn_minutes',   '15',   0),   -- 到期前 15 分钟提醒
  ('table.occupancy_hours_max__s1',  '4',    1),
  ('table.occupancy_hours_max__s2',  '4',    2)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 6. 套餐替换策略（套餐内菜品沽清时）
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('package.substitute.allow',           'true',   0),  -- 允许替换
  ('package.substitute.same_category',    'true',   0),  -- 仅可替换同分类菜品
  ('package.substitute.price_diff_max',   '50.00',  0),  -- 允许差价 ±50 元（超出需补差）
  ('package.substitute.require_confirm',  'true',   0),  -- 替换需客户确认
  ('package.substitute.auto_suggest',     'true',   0)   -- 自动推荐替换项
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 7. 其他业务规则（订单/库存/审批）
-- ---------------------------------------------------------------------
INSERT INTO `config` (`config_key`, `config_value`, `store_id`) VALUES
  ('order.payment_timeout_minutes',    '120',   0),  -- 订单支付超时 120 分钟
  ('order.min_amount_for_deposit',     '1000.00', 0),  -- 订单 ≥1000 元需付订金
  ('inventory.low_stock_threshold',    '0.20',  0),   -- 库存低于安全库存 20% 触发预警
  ('inventory.recount_days_interval',  '30',    0),   -- 盘点周期 30 天
  ('approval.timeout_hours',           '48',    0),   -- 审批超时 48 小时自动升级
  ('kitchen.order_timeout_minutes',    '45',    0)    -- 厨房出单超时 45 分钟告警
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`), `store_id` = VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 验证查询
-- ---------------------------------------------------------------------
SELECT
  config_key,
  config_value,
  store_id,
  CASE
    WHEN config_key LIKE 'refund_ratio%'         THEN '退款比例'
    WHEN config_key LIKE 'cancellation_penalty%' THEN '违约金'
    WHEN config_key LIKE 'booking.advance%'      THEN '预订时限'
    WHEN config_key LIKE 'booking.cancel%'      THEN '取消时限'
    WHEN config_key LIKE 'booking.hold%'        THEN '桌位保留'
    WHEN config_key LIKE 'booking.deposit%'     THEN '订金规则'
    WHEN config_key LIKE 'table.occupancy%'     THEN '桌台占用'
    WHEN config_key LIKE 'package.substitute%'  THEN '套餐替换'
    WHEN config_key LIKE 'order.%'              THEN '订单规则'
    WHEN config_key LIKE 'inventory.%'           THEN '库存规则'
    WHEN config_key LIKE 'approval.%'           THEN '审批规则'
    WHEN config_key LIKE 'kitchen.%'           THEN '厨房规则'
    ELSE '其他'
  END AS rule_category
FROM `config`
ORDER BY rule_category, store_id, config_key;
