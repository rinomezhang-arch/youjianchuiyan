SET NAMES utf8mb4;
ALTER TABLE `tool_issue` MODIFY COLUMN staff_id INT NULL COMMENT '员工ID(关联staff_master.staff_id)';
ALTER TABLE `tool_return` MODIFY COLUMN staff_id INT NULL COMMENT '员工ID(关联staff_master.staff_id)';
ALTER TABLE `tool_damage` MODIFY COLUMN staff_id INT NULL COMMENT '员工ID(关联staff_master.staff_id)';
ALTER TABLE `tool_inventory` MODIFY COLUMN staff_id INT NULL COMMENT '员工ID(关联staff_master.staff_id)';

