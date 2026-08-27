-- ======================================================================
-- self_service_submission 建表迁移 v1
-- 生成时间: 2026-08-27
-- 说明: SelfService.vue(员工自助入职登记) + ReviewQueue.vue(入职审核队列)
--       两个前端页面早就是完整成品(表单校验、驳回原因、审核统计都做好了)，
--       但调用的 /api/hr/self-service/* 系列接口后端完全不存在，这两个
--       页面从建成起从来没能真正用过。这里补建表 + 真实接口。
-- ======================================================================

CREATE TABLE IF NOT EXISTS `self_service_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID',
  `submit_type` varchar(20) NOT NULL DEFAULT 'new' COMMENT '提交类型: new=新增入职/update=信息更新',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `id_card` varchar(255) DEFAULT NULL COMMENT '身份证号(AES加密存储)',
  `department` varchar(50) DEFAULT NULL COMMENT '意向部门',
  `position` varchar(50) DEFAULT NULL COMMENT '意向职位',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `address` varchar(200) DEFAULT NULL COMMENT '家庭住址',
  `emergency_contact` varchar(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像照片URL(腾讯云COS)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  `reject_note` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `reviewer_id` int DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(50) DEFAULT NULL COMMENT '审核人姓名',
  `review_time` timestamp NULL DEFAULT NULL COMMENT '审核时间',
  `converted_staff_id` int DEFAULT NULL COMMENT '通过后创建/关联的正式员工ID(staff_master.staff_id)',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_ssb_store_status` (`store_id`, `status`),
  KEY `idx_ssb_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工自助入职登记/信息更新提交';
