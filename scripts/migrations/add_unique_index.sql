-- ================================================================
-- 添加UNIQUE索引到复合主键的部分列
-- 解决外键引用失败问题
-- ================================================================
SET NAMES utf8mb4;

-- dish_master: 复合主键(dish_id, store_id), 需要dish_id单独UNIQUE
ALTER TABLE `dish_master` ADD UNIQUE INDEX `uk_dish_id` (`dish_id`);

-- ingredient_master: 复合主键(ingredient_id, store_id), 需要ingredient_id单独UNIQUE
ALTER TABLE `ingredient_master` ADD UNIQUE INDEX `uk_ingredient_id` (`ingredient_id`);

-- 验证
SELECT 'dish_master索引:' AS info;
SHOW INDEX FROM dish_master WHERE Key_name = 'uk_dish_id';
SELECT 'ingredient_master索引:' AS info;
SHOW INDEX FROM ingredient_master WHERE Key_name = 'uk_ingredient_id';
