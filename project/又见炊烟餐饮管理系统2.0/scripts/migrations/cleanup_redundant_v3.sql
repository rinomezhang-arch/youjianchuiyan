-- ========================================================================
-- 又见炊烟餐饮管理系统 — 冗余对象清理 v3
-- 生成时间: 2026-08-02
-- 来源: 体检v4 super报告 FATAL/ERROR 真问题清理
-- 作者: 地龙 (DL-BOT)
-- ========================================================================
-- 基于体检报告 system_checkup_v4_latest.html 的真问题清单：
--   FATAL-3: package_dish_rel 与 package_dish_detail 重复表 → 删 package_dish_rel
--   FATAL-4: meal_package 与 package_master 重复表 → 删 meal_package
--   ERROR-7: booking_master.deposit 与 deposit_amount 字段重复 → 删 deposit
--   ERROR-7: ingredient_master.avg_price 与 unit_price 字段重复 → 删 avg_price
--
-- 体检脚本误报（本脚本不处理，需修改体检脚本识别逻辑）：
--   FATAL-1/2: dish_tag_relation/dish_usage_relation 的 store_id 是复合外键一部分
--            (dish_id, store_id) REFERENCES dish_master(dish_id, store_id)
--            体检脚本拆分识别为单独外键导致误报
--   ERROR-1~6: 6张表的 booking_id(varchar) 应指向 booking_master.booking_id(varchar 业务编号)
--            体检脚本误检查为指向 booking_master.id(bigint 主键)
--   ERROR-7 部分: dish_master 是 sale_price/cost_price (售价/成本价)，非重复
-- ========================================================================

-- 预检：所有待清理对象均为空表/空字段，已通过 check_redundant.py 验证
-- 删除顺序：先删依赖方 (package_dish_rel 引用 meal_package)，再删被依赖方

-- ============ FATAL-3: 删除 package_dish_rel ============
-- 该表通过外键 package_dish_rel_ibfk_1 引用 meal_package(id)
-- 必须先于 meal_package 删除
DROP TABLE IF EXISTS `package_dish_rel`;

-- ============ FATAL-4: 删除 meal_package ============
-- 重复表，业务已统一到 package_master
DROP TABLE IF EXISTS `meal_package`;

-- ============ ERROR-7a: 清理 booking_master.deposit 字段 ============
-- 数据已确认 0 行有值，可直接删除字段
-- 保留 deposit_amount (精度12,2) 作为唯一押金字段
ALTER TABLE `booking_master` DROP COLUMN `deposit`;

-- ============ ERROR-7b: 清理 ingredient_master.avg_price 字段 ============
-- 数据已确认 0 行有值，可直接删除字段
-- 保留 unit_price (当前单价) 作为唯一价格字段
ALTER TABLE `ingredient_master` DROP COLUMN `avg_price`;

-- ============ 验证：清理后状态 ============
-- 期望：meal_package/package_dish_rel 不存在；booking_master 无 deposit；ingredient_master 无 avg_price
SELECT
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='banquet' AND table_name='meal_package') AS meal_package_exists,
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='banquet' AND table_name='package_dish_rel') AS package_dish_rel_exists,
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='banquet' AND table_name='booking_master' AND column_name='deposit') AS deposit_exists,
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='banquet' AND table_name='ingredient_master' AND column_name='avg_price') AS avg_price_exists;
