-- ======================================================================
-- tag_master 建表迁移 v1
-- 生成时间: 2026-08-27
-- 说明: Tags.vue(标签管理页) 调用的 GET/POST/PUT /api/tags 生产库里
--       从来没有对应的表，之前请求失败就静默 catch 到内存里的假数据/本地
--       编辑，用户以为保存成功了，实际刷新页面就全部丢失。这里补建真实表。
--       dish_count 暂时不存在"标签-菜品"关联关系，如实固定为 0，不编造。
-- ======================================================================

CREATE TABLE IF NOT EXISTS `tag_master` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID(多租户隔离)',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_name_en` varchar(50) DEFAULT NULL COMMENT '英文名',
  `tag_group` varchar(20) NOT NULL COMMENT '分组: taste/feature/allergy/diet/cook',
  `tag_color` varchar(20) DEFAULT NULL COMMENT '标签颜色(十六进制)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tag_id`),
  KEY `idx_tag_store_group` (`store_id`, `tag_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签库(口味/特征/过敏原/饮食类型/烹饪方式)';
