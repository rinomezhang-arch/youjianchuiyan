-- 官网门店详情页"点菜预定"：客人免登录在门店页浏览真实菜单、选菜、填联系方式提交，
-- 落地为一条真实的预约咨询记录，员工侧再跟进确认。不是纯展示页，是能真操作的入口。

CREATE TABLE IF NOT EXISTS `booking_inquiry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT 1,
  `customer_name` varchar(50) NOT NULL,
  `customer_phone` varchar(20) NOT NULL,
  `preferred_date` date DEFAULT NULL,
  `preferred_time` varchar(20) DEFAULT NULL,
  `guest_count` int DEFAULT NULL,
  `selected_dishes` text DEFAULT NULL COMMENT '客人挑选的菜品，JSON数组[{dishName,salePrice}]',
  `remark` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/contacted/confirmed/cancelled',
  `staff_note` varchar(500) DEFAULT NULL,
  `handled_by` varchar(50) DEFAULT NULL,
  `handled_time` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bi_store_status` (`store_id`,`status`),
  KEY `idx_bi_phone` (`customer_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
