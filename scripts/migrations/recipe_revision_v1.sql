-- CL-R3-RECIPE　配方历史版本化（追加式迁移，不改列、不删列、不改写任何存量数据）
--
-- 背景：RecipeController.saveRecipe 基线实现是 deleteByDishIdAndStoreId 之后逐条重插，
-- 每保存一次就把该菜品该门店的配方行物理删掉再新建，recipe_id 换新、created_at 丢失，
-- 改过什么、谁改的、改之前是什么样，全都查不回来。配方直接决定菜品成本，这段历史不能没有。
--
-- 方案：dish_recipe 保留全部历史行，只切换 is_active 标记；每次保存生成一个 recipe_revision 版本号。
-- 存量行 is_active 默认 1（保持现有查询结果不变），revision_id 留空表示"版本化之前的导入基线"。
-- 是否把存量行统一回填成"版本 0"由集成阶段决定，本迁移不擅自回填历史。

CREATE TABLE IF NOT EXISTS recipe_revision (
  revision_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id    BIGINT       NOT NULL,
  dish_id     VARCHAR(40)  NOT NULL,
  version_no  INT          NOT NULL COMMENT '同一门店同一菜品内递增，从 1 开始',
  item_count  INT          NOT NULL DEFAULT 0,
  total_cost  DECIMAL(15,4) NULL COMMENT '该版本配方合计标准成本，与 dish_master.cost_price 同源同事务写入',
  created_by  VARCHAR(40)  NULL COMMENT '保存人登录名，取自 JWT',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note        VARCHAR(200) NULL,
  -- 唯一键既保证版本号不重复，也让并发保存在数据库层再兜一道：
  -- 应用层已对 dish_master 行加 FOR UPDATE 串行化，这里是第二道防线。
  UNIQUE KEY uk_recipe_revision_version (store_id, dish_id, version_no),
  KEY idx_recipe_revision_dish (store_id, dish_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品配方版本';

-- dish_recipe 追加两列。默认 is_active=1 保证迁移瞬间现有查询结果完全不变。
ALTER TABLE dish_recipe
  ADD COLUMN revision_id BIGINT   NULL COMMENT '所属配方版本；NULL=版本化之前的导入基线',
  ADD COLUMN is_active   TINYINT  NOT NULL DEFAULT 1 COMMENT '1=当前生效版本 0=历史版本，历史行只置 0 不删除',
  ADD INDEX idx_dish_recipe_active (store_id, dish_id, is_active),
  ADD INDEX idx_dish_recipe_revision (revision_id);
