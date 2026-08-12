SET NAMES utf8mb4;
ALTER TABLE `engineering_work_order` MODIFY COLUMN `assignee_id` INT NULL COMMENT 'assignee_id';
ALTER TABLE `attachment` MODIFY COLUMN `upload_by` INT NULL COMMENT 'upload_by';
ALTER TABLE `decoration_project` MODIFY COLUMN `manager_id` INT NULL COMMENT 'manager_id';
ALTER TABLE `floor_project` MODIFY COLUMN `manager_id` INT NULL COMMENT 'manager_id';
ALTER TABLE `duty_record` MODIFY COLUMN `staff_id` INT NULL COMMENT 'staff_id';
ALTER TABLE `safety_issue` MODIFY COLUMN `reporter_id` INT NULL COMMENT 'reporter_id';
ALTER TABLE `safety_issue` MODIFY COLUMN `handler_id` INT NULL COMMENT 'handler_id';
ALTER TABLE `engineering_inspection` MODIFY COLUMN `inspector_id` INT NULL COMMENT 'inspector_id';

