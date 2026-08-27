-- 招聘岗位信息表：员工自助入职登记页"点击岗位信息"浏览用
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行安全

CREATE TABLE IF NOT EXISTS `job_posting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `department` varchar(50) NOT NULL,
  `position` varchar(50) NOT NULL,
  `headcount` int NOT NULL DEFAULT '1',
  `salary_range` varchar(50) DEFAULT NULL,
  `work_time` varchar(100) DEFAULT NULL,
  `requirements` varchar(500) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'open',
  `created_by` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_jp_store_status` (`store_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 自助登记提交记录关联应聘的岗位（可选，非强约束）
ALTER TABLE `self_service_submission`
  ADD COLUMN `job_posting_id` bigint DEFAULT NULL AFTER `submit_type`;
