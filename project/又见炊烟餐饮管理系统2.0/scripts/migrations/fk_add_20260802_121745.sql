-- ================================================================
-- 外键补全SQL - 20260802_121745
-- ================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- =====================================================
-- 模块: 采购 (19个)
-- =====================================================

-- =====================================================
-- 模块: 库存 (19个)
-- =====================================================

-- =====================================================
-- 模块: 成本 (8个)
-- =====================================================

-- =====================================================
-- 模块: 财务 (23个)
-- =====================================================

-- =====================================================
-- 模块: 预订 (6个)
-- =====================================================

-- =====================================================
-- 模块: 会员 (14个)
-- =====================================================

-- =====================================================
-- 模块: 员工 (10个)
-- =====================================================

-- =====================================================
-- 模块: 审批 (5个)
-- =====================================================

-- =====================================================
-- 模块: 报表 (5个)
-- =====================================================

-- =====================================================
-- 模块: 系统 (6个)
-- =====================================================

-- =====================================================
-- 模块: 套餐 (3个)
-- =====================================================

-- =====================================================
-- 模块: 报销 (4个)
-- =====================================================

-- =====================================================
-- 模块: 工程 (13个)
-- =====================================================

-- =====================================================
-- 模块: 工具 (9个)
-- =====================================================
ALTER TABLE `tool_issue` ADD CONSTRAINT `fk_tool_issue_staff_id` FOREIGN KEY (`staff_id`) REFERENCES `staff_master`(`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_issue` ADD CONSTRAINT `fk_tool_issue_tool_id` FOREIGN KEY (`tool_id`) REFERENCES `tool_master`(`tool_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_return` ADD CONSTRAINT `fk_tool_return_issue_id` FOREIGN KEY (`issue_id`) REFERENCES `tool_issue`(`issue_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_return` ADD CONSTRAINT `fk_tool_return_tool_id` FOREIGN KEY (`tool_id`) REFERENCES `tool_master`(`tool_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_return` ADD CONSTRAINT `fk_tool_return_staff_id` FOREIGN KEY (`staff_id`) REFERENCES `staff_master`(`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_damage` ADD CONSTRAINT `fk_tool_damage_tool_id` FOREIGN KEY (`tool_id`) REFERENCES `tool_master`(`tool_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_damage` ADD CONSTRAINT `fk_tool_damage_staff_id` FOREIGN KEY (`staff_id`) REFERENCES `staff_master`(`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `tool_inventory` ADD CONSTRAINT `fk_tool_inventory_staff_id` FOREIGN KEY (`staff_id`) REFERENCES `staff_master`(`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS=1;
-- ================================================================
-- 共生成 8 条ADD CONSTRAINT语句