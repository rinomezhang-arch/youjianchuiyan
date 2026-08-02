-- ========================================================================
-- 又见炊烟餐饮管理系统 — 冗余字段清理 v4
-- 生成时间: 2026-08-02
-- 来源: 体检v4 super 报告剩余 1 个 ERROR
-- 作者: 地龙 (DL-BOT)
-- ========================================================================
-- 体检报告 ERROR: dish_master 存在重复业务字段 price/cost_price
-- 实际诊断: dish_master 有 3 个价格字段
--   - cost_price  decimal(10,2)  50行有值  成本价  ← 保留
--   - sale_price  decimal(10,2)  50行有值  售价    ← 保留
--   - price       decimal(12,2)  0行有值   废弃   ← 删除
-- 后端 DishMaster.java 仅映射 cost_price/sale_price，不引用 price
-- ========================================================================

-- 删除 dish_master.price 字段（0行数据，无需迁移）
ALTER TABLE `dish_master` DROP COLUMN `price`;

-- 验证
SELECT
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='banquet' AND table_name='dish_master' AND column_name='price') AS price_exists,
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='banquet' AND table_name='dish_master' AND column_name='sale_price') AS sale_price_exists,
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='banquet' AND table_name='dish_master' AND column_name='cost_price') AS cost_price_exists;
