-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: banquet
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_users`
--

DROP TABLE IF EXISTS `admin_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `real_name` varchar(50) DEFAULT NULL,
  `role` enum('admin','manager','staff') DEFAULT 'staff',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_chat_history`
--

DROP TABLE IF EXISTS `ai_chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `staff_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL COMMENT 'user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `image_url` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_memory`
--

DROP TABLE IF EXISTS `ai_memory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_memory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text NOT NULL COMMENT '记忆内容',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI记忆表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_flow`
--

DROP TABLE IF EXISTS `approval_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `flow_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批流水号',
  `flow_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'leave/overtime/purchase/expense/stock_loss',
  `business_id` bigint NOT NULL COMMENT '业务单据ID',
  `business_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务单据号',
  `applicant_id` int DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请人姓名',
  `store_id` bigint NOT NULL COMMENT '门店ID：0=全局',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `current_node` int NOT NULL DEFAULT '1' COMMENT '当前节点序号',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_no` (`flow_no`),
  KEY `idx_flow_type` (`flow_type`),
  KEY `idx_flow_store` (`store_id`),
  KEY `idx_flow_status` (`status`),
  KEY `idx_flow_business` (`business_id`,`business_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_log`
--

DROP TABLE IF EXISTS `approval_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `business_id` varchar(50) NOT NULL,
  `business_type` varchar(50) NOT NULL,
  `current_status` varchar(20) NOT NULL,
  `previous_status` varchar(20) DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `action` varchar(20) NOT NULL,
  `comment` text,
  `approval_time` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_business` (`business_id`,`business_type`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_node`
--

DROP TABLE IF EXISTS `approval_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `flow_id` bigint NOT NULL COMMENT '审批流ID（approval_flow.id）',
  `node_order` int NOT NULL COMMENT '节点序号',
  `node_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称',
  `approver_id` int DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人姓名',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `approved_time` datetime DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_node_flow` (`flow_id`),
  KEY `idx_node_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_template`
--

DROP TABLE IF EXISTS `approval_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'leave/overtime/purchase/expense/stock_loss',
  `store_id` bigint NOT NULL COMMENT '门店ID：0=全局通用模板',
  `node_count` int NOT NULL COMMENT '节点数量',
  `node1_approver_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点1审批角色',
  `node2_approver_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点2审批角色',
  `node3_approver_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点3审批角色',
  `is_active` int NOT NULL DEFAULT '1' COMMENT '1=启用 0=禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_type_store` (`template_type`,`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `attendance_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint DEFAULT '1',
  `staff_id` int NOT NULL,
  `attendance_date` date NOT NULL,
  `clock_in` datetime DEFAULT NULL,
  `clock_out` datetime DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `late_minutes` int DEFAULT '0',
  `early_leave_minutes` int DEFAULT '0',
  `absent` tinyint(1) DEFAULT '0',
  `work_hours` double DEFAULT '0',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`attendance_id`),
  KEY `idx_staff_date` (`staff_id`,`attendance_date`)
) ENGINE=InnoDB AUTO_INCREMENT=227 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` int NOT NULL AUTO_INCREMENT,
  `record_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'YYYY-MM',
  `scope` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'full',
  `day_num` int NOT NULL,
  `am_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pm_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `am_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `pm_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `day_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `employment` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '全勤在职',
  `salary_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '未发放',
  `public_holiday` int DEFAULT '6',
  `carry_over` int DEFAULT '0',
  `summary_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `total_present` decimal(6,1) DEFAULT '0.0',
  `total_statutory` decimal(6,1) DEFAULT '0.0',
  `total_holiday` decimal(6,1) DEFAULT '0.0',
  `total_comp` decimal(6,1) DEFAULT '0.0',
  `total_travel` decimal(6,1) DEFAULT '0.0',
  `total_overtime` decimal(6,1) DEFAULT '0.0',
  `total_leave` decimal(6,1) DEFAULT '0.0',
  `total_late` decimal(6,1) DEFAULT '0.0',
  `total_early` decimal(6,1) DEFAULT '0.0',
  `total_absent` decimal(6,1) DEFAULT '0.0',
  `final_balance` decimal(6,1) DEFAULT '0.0',
  `recorded_days` int DEFAULT '0',
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Rino',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `staff_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `staff_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `record_id` (`record_id`),
  UNIQUE KEY `uk_emp_month` (`emp_id`,`month`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_month` (`month`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` bigint DEFAULT (unix_timestamp()),
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `banquet_template`
--

DROP TABLE IF EXISTS `banquet_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banquet_template` (
  `id` int NOT NULL AUTO_INCREMENT,
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_type` varchar(20) NOT NULL COMMENT '模板类型：banquet-宴会, alacarte-零点, festive-节日',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `base_price` decimal(10,2) DEFAULT NULL COMMENT '基础价格',
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `template_code` (`template_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `banquet_template_rel`
--

DROP TABLE IF EXISTS `banquet_template_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banquet_template_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `banquet_type_id` int NOT NULL COMMENT '宴会类型ID',
  `template_id` int NOT NULL COMMENT '模板ID',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模板',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_banquet_template` (`banquet_type_id`,`template_id`),
  KEY `template_id` (`template_id`),
  CONSTRAINT `banquet_template_rel_ibfk_1` FOREIGN KEY (`banquet_type_id`) REFERENCES `banquet_type` (`id`) ON DELETE CASCADE,
  CONSTRAINT `banquet_template_rel_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `banquet_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宴会-模板关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `banquet_type`
--

DROP TABLE IF EXISTS `banquet_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banquet_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) NOT NULL COMMENT '宴会类型名称',
  `type_code` varchar(50) NOT NULL COMMENT '类型编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宴会类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_material`
--

DROP TABLE IF EXISTS `base_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_material` (
  `material_id` varchar(50) NOT NULL,
  `material_name` varchar(100) NOT NULL,
  `category_id` varchar(20) DEFAULT 'C010',
  `purchase_unit` varchar(20) DEFAULT NULL,
  `minor_unit` varchar(20) DEFAULT NULL,
  `purchase_to_minor_ratio` decimal(10,3) DEFAULT '1.000',
  `default_yield_rate` decimal(5,2) DEFAULT '100.00',
  `latest_price` decimal(10,2) DEFAULT '0.00',
  `latest_price_per_minor` decimal(10,4) DEFAULT '0.0000',
  `latest_true_price_per_minor` decimal(10,4) DEFAULT '0.0000',
  `current_stock` decimal(12,3) DEFAULT '0.000',
  `default_supplier_id` int DEFAULT NULL,
  `last_purchase_date` date DEFAULT NULL,
  `last_purchase_detail` text,
  `status` int DEFAULT '1',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_supplier`
--

DROP TABLE IF EXISTS `base_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_supplier` (
  `supplier_id` int NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(100) NOT NULL,
  `short_name` varchar(50) DEFAULT NULL,
  `contact_person` varchar(50) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `contact_address` varchar(200) DEFAULT NULL,
  `tax_id` varchar(50) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `bank_account` varchar(50) DEFAULT NULL,
  `payment_terms` varchar(100) DEFAULT NULL,
  `status` int DEFAULT '1',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_dish_detail`
--

DROP TABLE IF EXISTS `booking_dish_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_dish_detail` (
  `dish_booking_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_booking_id` bigint DEFAULT NULL,
  `booking_id` varchar(20) DEFAULT NULL,
  `dish_id` varchar(20) NOT NULL,
  `dish_name` varchar(100) DEFAULT NULL,
  `dish_quantity` int DEFAULT '1',
  `unit_price` decimal(10,2) DEFAULT '0.00',
  `subtotal` decimal(10,2) DEFAULT '0.00',
  `custom_name` varchar(100) DEFAULT NULL,
  `dish_note` varchar(255) DEFAULT NULL,
  `dish_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `kitchen_status` varchar(20) DEFAULT 'pending' COMMENT '备菜状态',
  `kitchen_station` varchar(50) DEFAULT NULL COMMENT '负责灶台: 炒锅/蒸锅/冷菜/汤锅/面点',
  `kitchen_note` varchar(255) DEFAULT NULL COMMENT '厨房备注',
  `kitchen_started_at` bigint DEFAULT NULL COMMENT '开始备菜',
  `kitchen_done_at` bigint DEFAULT NULL COMMENT '备菜完成',
  PRIMARY KEY (`dish_booking_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_table_booking` (`table_booking_id`),
  KEY `idx_dish` (`dish_id`)
) ENGINE=InnoDB AUTO_INCREMENT=336 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_master`
--

DROP TABLE IF EXISTS `booking_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_master` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `booking_date` date NOT NULL,
  `booking_time` time DEFAULT NULL,
  `customer_id` int DEFAULT NULL,
  `customer_name` varchar(50) DEFAULT NULL,
  `customer_phone` varchar(20) DEFAULT NULL,
  `staff_id` int DEFAULT NULL,
  `staff_name` varchar(20) DEFAULT NULL,
  `deposit` decimal(10,2) DEFAULT '0.00',
  `guest_count` int DEFAULT '0',
  `table_count` int DEFAULT '0',
  `spare_tables` int DEFAULT '0',
  `guest_per_table` int DEFAULT '10',
  `booking_status` varchar(20) DEFAULT 'confirmed',
  `banquet_name` varchar(100) DEFAULT NULL,
  `occasion_type` varchar(20) DEFAULT NULL,
  `special_request` text,
  `total_amount` decimal(10,2) DEFAULT '0.00',
  `final_amount` decimal(10,2) DEFAULT '0.00',
  `payment_status` varchar(20) DEFAULT 'unpaid',
  `remark` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `booking_no` varchar(30) DEFAULT NULL,
  `package_id` varchar(20) DEFAULT NULL,
  `booking_type` varchar(20) DEFAULT 'normal',
  `deposit_amount` decimal(12,2) DEFAULT '0.00',
  `package_name` varchar(100) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'pending',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_id` (`booking_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_date` (`booking_date`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_status` (`booking_status`),
  KEY `idx_phone` (`customer_phone`),
  KEY `fk_bm_staff` (`staff_id`),
  KEY `idx_store_id` (`store_id`),
  CONSTRAINT `fk_bm_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_master` (`customer_id`),
  CONSTRAINT `fk_bm_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`)
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_table`
--

DROP TABLE IF EXISTS `booking_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_table` (
  `booking_master_id` bigint DEFAULT NULL,
  `booking_table_code` varchar(50) DEFAULT NULL,
  `table_booking_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `booking_id` varchar(20) NOT NULL,
  `booking_date` date NOT NULL,
  `booking_time` time DEFAULT NULL,
  `table_id` int NOT NULL,
  `table_number` varchar(10) DEFAULT NULL,
  `table_name` varchar(20) DEFAULT NULL,
  `guest_count` int DEFAULT '0',
  `package_id` varchar(20) DEFAULT NULL,
  `package_name` varchar(100) DEFAULT NULL,
  `open_table_type` varchar(50) DEFAULT NULL,
  `table_note` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`table_booking_id`),
  UNIQUE KEY `uk_table_date_time` (`table_id`,`booking_date`,`booking_time`),
  KEY `idx_store` (`store_id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_table` (`table_id`),
  KEY `fk_booking_master` (`booking_master_id`),
  CONSTRAINT `fk_booking_master` FOREIGN KEY (`booking_master_id`) REFERENCES `booking_master` (`id`),
  CONSTRAINT `fk_bt_table` FOREIGN KEY (`table_id`) REFERENCES `table_master` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_categories_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `change_log`
--

DROP TABLE IF EXISTS `change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `change_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` int NOT NULL DEFAULT '1',
  `operator_id` int DEFAULT NULL COMMENT '操作人staff_id',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operation_type` varchar(30) NOT NULL COMMENT '操作类型: create/update/delete/login/config',
  `target_type` varchar(30) NOT NULL COMMENT '目标类型: booking/customer/table/dish/menu/staff/supplier/system',
  `target_id` varchar(50) DEFAULT NULL COMMENT '目标ID',
  `summary` varchar(200) NOT NULL COMMENT '操作摘要',
  `detail` text COMMENT '详细内容(JSON)',
  `old_value` text COMMENT '修改前的值(JSON)',
  `new_value` text COMMENT '修改后的值(JSON)',
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_store_time` (`store_id`,`created_at` DESC),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_type` (`operation_type`,`target_type`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统改动日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config`
--

DROP TABLE IF EXISTS `config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config` (
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_master`
--

DROP TABLE IF EXISTS `customer_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_master` (
  `customer_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `customer_name` varchar(50) NOT NULL,
  `customer_phone` varchar(20) NOT NULL,
  `customer_preference` text,
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `member_level` varchar(10) DEFAULT 'v1',
  `booking_count` int DEFAULT '0',
  `last_booking_date` date DEFAULT NULL,
  `remark` text,
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uk_store_name_phone` (`store_id`,`customer_name`,`customer_phone`),
  KEY `idx_store` (`store_id`),
  KEY `idx_phone` (`customer_phone`),
  KEY `idx_name` (`customer_name`),
  KEY `idx_level` (`member_level`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `dept_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint DEFAULT '1',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dept_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parent_id` int DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'active',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `level` int DEFAULT '1',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_category`
--

DROP TABLE IF EXISTS `dish_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '厨房分类名称',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0',
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `menu_type` varchar(20) DEFAULT 'alacarte' COMMENT '菜单类型',
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='厨房分类表（食材/做法）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_cost_card`
--

DROP TABLE IF EXISTS `dish_cost_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_cost_card` (
  `cost_card_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dish_id` varchar(50) NOT NULL,
  `dish_name` varchar(100) NOT NULL,
  `dish_category` varchar(50) DEFAULT NULL,
  `standard_yield` decimal(10,3) DEFAULT NULL,
  `actual_yield` decimal(10,3) DEFAULT NULL,
  `yield_rate` decimal(5,2) DEFAULT NULL,
  `standard_cost` decimal(12,2) DEFAULT NULL,
  `actual_cost` decimal(12,2) DEFAULT NULL,
  `selling_price` decimal(12,2) DEFAULT NULL,
  `gross_margin` decimal(5,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'active',
  `effective_date` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cost_card_id`),
  KEY `idx_store_costcard` (`store_id`),
  KEY `idx_dish_costcard` (`dish_id`),
  KEY `idx_status_costcard` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_cost_card_detail`
--

DROP TABLE IF EXISTS `dish_cost_card_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_cost_card_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `cost_card_id` bigint NOT NULL,
  `line_no` int NOT NULL,
  `ingredient_id` varchar(50) NOT NULL,
  `ingredient_name` varchar(100) NOT NULL,
  `spec` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `standard_quantity` decimal(10,3) DEFAULT NULL,
  `actual_quantity` decimal(10,3) DEFAULT NULL,
  `converted_quantity` decimal(10,3) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `cost_amount` decimal(12,2) DEFAULT NULL,
  `yield_rate` decimal(5,2) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_costcard_detail` (`cost_card_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_master`
--

DROP TABLE IF EXISTS `dish_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_master` (
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dish_name` varchar(100) NOT NULL,
  `dish_category` varchar(50) DEFAULT NULL,
  `spicy_level` int DEFAULT '0',
  `main_ingredient_type` varchar(50) DEFAULT NULL,
  `main_ingredient` varchar(100) DEFAULT NULL,
  `english_name` varchar(200) DEFAULT NULL,
  `cost_price` decimal(10,2) DEFAULT '0.00',
  `sale_price` decimal(10,2) DEFAULT '0.00',
  `cost_rate` decimal(5,2) DEFAULT '0.00',
  `cooking_time` int DEFAULT '15',
  `servings` int DEFAULT '1',
  `birthday_name` varchar(100) DEFAULT NULL,
  `wedding_name` varchar(100) DEFAULT NULL,
  `house_move_name` varchar(100) DEFAULT NULL,
  `promotion_name` varchar(100) DEFAULT NULL,
  `reunion_name` varchar(100) DEFAULT NULL,
  `thanksgiving_name` varchar(100) DEFAULT NULL,
  `year_end_name` varchar(100) DEFAULT NULL,
  `baby_born_name` varchar(100) DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `usage_type` varchar(20) DEFAULT 'unused',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `image_url` varchar(500) DEFAULT NULL COMMENT '菜品图片URL',
  `dish_intro` text COMMENT '菜肴介绍',
  `tiktok_recommend` text COMMENT '抖音推荐',
  `festive_name` varchar(100) DEFAULT NULL COMMENT '吉庆名',
  `menu_type` varchar(20) DEFAULT 'alacarte' COMMENT '菜单类型: alacarte零点/banquet宴会/all全部',
  `category` varchar(50) DEFAULT NULL,
  `category_id` varchar(64) DEFAULT NULL,
  `cooking_method` varchar(50) DEFAULT NULL,
  `dish_code` varchar(64) DEFAULT NULL,
  `dish_name_en` varchar(100) DEFAULT NULL,
  `is_seasonal` int DEFAULT '0',
  `is_specialty` int DEFAULT '0',
  `main_ingredients` text,
  `taste` varchar(50) DEFAULT NULL,
  `unit` varchar(32) DEFAULT '?',
  `price` decimal(12,2) DEFAULT '0.00',
  `remark` text,
  PRIMARY KEY (`dish_id`,`store_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_category` (`dish_category`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_occasion_names`
--

DROP TABLE IF EXISTS `dish_occasion_names`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_occasion_names` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dish_id` varchar(20) NOT NULL,
  `occasion_type` varchar(20) NOT NULL,
  `custom_name` varchar(100) NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_occasion` (`occasion_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_recipe`
--

DROP TABLE IF EXISTS `dish_recipe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_recipe` (
  `recipe_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dish_id` varchar(20) NOT NULL,
  `ingredient_id` varchar(50) NOT NULL,
  `ingredient_name` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `unit_price` decimal(10,4) DEFAULT '0.0000',
  `quantity` decimal(10,3) DEFAULT '0.000',
  `total_cost` decimal(10,2) DEFAULT '0.00',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `wastage_rate` decimal(5,2) DEFAULT '0.00' COMMENT '损耗率%',
  `yield_rate` decimal(5,2) DEFAULT '0.00' COMMENT '出成率',
  `last_entry_date` date DEFAULT NULL,
  `net_unit_price` decimal(10,4) DEFAULT '0.0000' COMMENT '净料单价',
  `notes` text,
  PRIMARY KEY (`recipe_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_ingredient` (`ingredient_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_tag`
--

DROP TABLE IF EXISTS `dish_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) NOT NULL COMMENT '标记名称（本店招牌菜、本地炖锅特色菜等）',
  `tag_code` varchar(50) NOT NULL COMMENT '标记代码',
  `tag_type` varchar(20) NOT NULL COMMENT '用途类型：banquet=宴会, a_la_carte=零点',
  `dish_category` varchar(50) DEFAULT '' COMMENT '菜品分类（海鲜水产、家常炒菜等）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `import_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间标记',
  `menu_date` date DEFAULT NULL COMMENT '菜牌形成日期标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag_code` (`tag_code`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜牌标记类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_tag_relation`
--

DROP TABLE IF EXISTS `dish_tag_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_tag_relation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dish_id` varchar(20) NOT NULL COMMENT '菜品编号',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID',
  `tag_id` int NOT NULL COMMENT '标记ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_tag` (`dish_id`,`store_id`,`tag_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `dish_tag_relation_ibfk_1` FOREIGN KEY (`dish_id`, `store_id`) REFERENCES `dish_master` (`dish_id`, `store_id`) ON DELETE CASCADE,
  CONSTRAINT `dish_tag_relation_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `dish_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1375 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品标记关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_usage`
--

DROP TABLE IF EXISTS `dish_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_usage` (
  `id` int NOT NULL AUTO_INCREMENT,
  `usage_name` varchar(20) NOT NULL COMMENT '用途名称：宴会/零点',
  `usage_code` varchar(20) NOT NULL COMMENT '用途代码：banquet/a_la_carte',
  `description` varchar(100) DEFAULT '' COMMENT '说明',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usage_code` (`usage_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品用途表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dish_usage_relation`
--

DROP TABLE IF EXISTS `dish_usage_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_usage_relation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dish_id` varchar(20) NOT NULL COMMENT '菜品编号',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID',
  `usage_id` int NOT NULL COMMENT '用途ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_usage` (`dish_id`,`store_id`,`usage_id`),
  KEY `usage_id` (`usage_id`),
  CONSTRAINT `dish_usage_relation_ibfk_1` FOREIGN KEY (`dish_id`, `store_id`) REFERENCES `dish_master` (`dish_id`, `store_id`) ON DELETE CASCADE,
  CONSTRAINT `dish_usage_relation_ibfk_2` FOREIGN KEY (`usage_id`) REFERENCES `dish_usage` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=512 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品用途关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dishes`
--

DROP TABLE IF EXISTS `dishes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dishes` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `en` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `spicy_level` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `sort_order` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_dishes_category` (`category_id`),
  KEY `idx_dishes_name` (`name`),
  FULLTEXT KEY `idx_dishes_fulltext` (`name`,`en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee_lifecycle`
--

DROP TABLE IF EXISTS `employee_lifecycle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_lifecycle` (
  `id` int NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(50) NOT NULL,
  `emp_name` varchar(50) DEFAULT NULL,
  `event_type` varchar(50) NOT NULL,
  `event_date` date NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_account`
--

DROP TABLE IF EXISTS `finance_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_account` (
  `account_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `account_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_holder` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `initial_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `current_balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `is_active` tinyint NOT NULL DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_account_code` (`account_code`,`store_id`),
  KEY `idx_account_type` (`account_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_cost_record`
--

DROP TABLE IF EXISTS `finance_cost_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_cost_record` (
  `cost_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `cost_date` date NOT NULL,
  `cost_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cost_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cost_id`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_expense`
--

DROP TABLE IF EXISTS `finance_expense`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_expense` (
  `expense_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `expense_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `expense_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expense_date` date NOT NULL,
  `applicant_id` int DEFAULT NULL,
  `applicant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `invoice_amount` decimal(12,2) DEFAULT NULL,
  `approval_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'unpaid',
  `payment_time` datetime DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`expense_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_expense_no` (`expense_no`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用报销表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_payable`
--

DROP TABLE IF EXISTS `finance_payable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_payable` (
  `payable_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `payable_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchase_id` int DEFAULT NULL,
  `purchase_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `paid_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `pending_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `payable_date` date DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `credit_days` int DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payable_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_payable_no` (`payable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应付账款表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_payment_plan`
--

DROP TABLE IF EXISTS `finance_payment_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_payment_plan` (
  `id` int NOT NULL AUTO_INCREMENT,
  `plan_no` varchar(30) DEFAULT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) DEFAULT NULL,
  `plan_amount` decimal(12,2) DEFAULT '0.00',
  `paid_amount` decimal(12,2) DEFAULT '0.00',
  `plan_date` date DEFAULT NULL,
  `status` int DEFAULT '0',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_no` (`plan_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_payment_record`
--

DROP TABLE IF EXISTS `finance_payment_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_payment_record` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `payment_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_date` date NOT NULL,
  `receivable_id` bigint DEFAULT NULL,
  `customer_id` int DEFAULT NULL,
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_id` int DEFAULT NULL,
  `booking_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_receivable`
--

DROP TABLE IF EXISTS `finance_receivable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_receivable` (
  `receivable_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `receivable_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` int DEFAULT NULL,
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_id` int DEFAULT NULL,
  `booking_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `received_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `pending_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `receivable_date` date DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `credit_days` int DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`receivable_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_receivable_no` (`receivable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_reconciliation`
--

DROP TABLE IF EXISTS `finance_reconciliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_reconciliation` (
  `recon_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `recon_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `recon_date` date NOT NULL,
  `account_id` bigint DEFAULT NULL,
  `account_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `book_balance` decimal(12,2) DEFAULT NULL,
  `bank_balance` decimal(12,2) DEFAULT NULL,
  `diff_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`recon_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_recon_no` (`recon_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_settlement`
--

DROP TABLE IF EXISTS `finance_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_settlement` (
  `settlement_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `settlement_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `settlement_date` date NOT NULL,
  `settlement_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `total_income` decimal(12,2) DEFAULT '0.00',
  `total_expense` decimal(12,2) DEFAULT '0.00',
  `total_profit` decimal(12,2) DEFAULT '0.00',
  `food_cost` decimal(12,2) DEFAULT '0.00',
  `labor_cost` decimal(12,2) DEFAULT '0.00',
  `rent_cost` decimal(12,2) DEFAULT '0.00',
  `utility_cost` decimal(12,2) DEFAULT '0.00',
  `other_cost` decimal(12,2) DEFAULT '0.00',
  `cost_rate` decimal(5,2) DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`settlement_id`),
  KEY `idx_settlement_date` (`settlement_date`),
  KEY `idx_settlement_no` (`settlement_no`),
  KEY `idx_settlement_type` (`settlement_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_settlement_detail`
--

DROP TABLE IF EXISTS `finance_settlement_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_settlement_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `settlement_no` varchar(30) NOT NULL,
  `line_no` int DEFAULT '1',
  `bill_type` varchar(30) DEFAULT NULL,
  `bill_no` varchar(50) DEFAULT NULL,
  `bill_amount` decimal(12,2) DEFAULT '0.00',
  `settled_amount` decimal(12,2) DEFAULT '0.00',
  `remark` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_transaction`
--

DROP TABLE IF EXISTS `finance_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_transaction` (
  `trans_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `trans_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_date` date NOT NULL,
  `trans_time` datetime NOT NULL,
  `trans_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trans_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `related_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2) DEFAULT NULL,
  `payer_payee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`trans_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_trans_date` (`trans_date`),
  KEY `idx_trans_no` (`trans_no`),
  KEY `idx_trans_type` (`trans_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_voucher`
--

DROP TABLE IF EXISTS `finance_voucher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_voucher` (
  `voucher_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `voucher_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `voucher_date` date NOT NULL,
  `voucher_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'transfer',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_debit` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_credit` decimal(12,2) NOT NULL DEFAULT '0.00',
  `is_balanced` tinyint DEFAULT '1',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft',
  `prepared_by` int DEFAULT NULL,
  `prepared_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audited_by` int DEFAULT NULL,
  `audited_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `audited_at` datetime DEFAULT NULL,
  `posted_by` int DEFAULT NULL,
  `posted_at` datetime DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`voucher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_voucher_date` (`voucher_date`),
  KEY `idx_voucher_no` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `finance_voucher_detail`
--

DROP TABLE IF EXISTS `finance_voucher_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_voucher_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `voucher_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `subject_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `debit_amount` decimal(12,2) DEFAULT '0.00',
  `credit_amount` decimal(12,2) DEFAULT '0.00',
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_subject_code` (`subject_code`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ingredient_inventory_log`
--

DROP TABLE IF EXISTS `ingredient_inventory_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingredient_inventory_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `ingredient_id` varchar(50) NOT NULL,
  `change_type` varchar(50) DEFAULT NULL,
  `quantity` decimal(10,3) DEFAULT NULL,
  `before_stock` decimal(10,3) DEFAULT NULL,
  `after_stock` decimal(10,3) DEFAULT NULL,
  `reference_id` varchar(100) DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `operator` varchar(100) DEFAULT NULL,
  `notes` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `log_type` varchar(20) DEFAULT NULL,
  `log_quantity` decimal(12,3) DEFAULT NULL,
  `stock_after` decimal(12,3) DEFAULT '0.000',
  `log_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `related_order_id` varchar(50) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `note` text,
  PRIMARY KEY (`log_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_type` (`log_type`),
  KEY `idx_time` (`log_time`),
  KEY `fk_iil_ingredient` (`ingredient_id`,`store_id`),
  CONSTRAINT `fk_iil_ingredient` FOREIGN KEY (`ingredient_id`, `store_id`) REFERENCES `ingredient_master` (`ingredient_id`, `store_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ingredient_master`
--

DROP TABLE IF EXISTS `ingredient_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingredient_master` (
  `ingredient_id` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `ingredient_name` varchar(100) NOT NULL,
  `ingredient_category` varchar(50) DEFAULT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `purchase_unit` varchar(20) DEFAULT NULL,
  `usage_unit` varchar(20) DEFAULT NULL,
  `conversion_rate` decimal(10,3) DEFAULT '1.000',
  `primary_supplier_id` int DEFAULT NULL,
  `current_stock` decimal(12,3) DEFAULT '0.000',
  `warning_threshold` decimal(10,3) DEFAULT '0.000',
  `avg_price` decimal(10,4) DEFAULT '0.0000',
  `yield_rate` decimal(5,2) DEFAULT '0.00' COMMENT '出成率',
  `last_entry_date` date DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `category` varchar(255) DEFAULT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `min_stock` decimal(10,3) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `supplier_id` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ingredient_id`,`store_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_category` (`ingredient_category`),
  KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ingredient_purchase`
--

DROP TABLE IF EXISTS `ingredient_purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingredient_purchase` (
  `purchase_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `ingredient_id` varchar(50) NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `purchase_date` date NOT NULL,
  `purchase_quantity` decimal(12,3) DEFAULT '0.000',
  `purchase_price` decimal(10,2) DEFAULT '0.00',
  `purchase_total` decimal(12,2) DEFAULT '0.00',
  `usage_quantity` decimal(12,3) DEFAULT '0.000',
  `usage_price` decimal(10,4) DEFAULT '0.0000',
  `operator_id` int DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending',
  `processing_note` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`purchase_id`),
  KEY `fk_ip_ingredient` (`ingredient_id`,`store_id`),
  CONSTRAINT `fk_ip_ingredient` FOREIGN KEY (`ingredient_id`, `store_id`) REFERENCES `ingredient_master` (`ingredient_id`, `store_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kitchen_log`
--

DROP TABLE IF EXISTS `kitchen_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kitchen_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `action` varchar(30) NOT NULL COMMENT 'start/finish/serve',
  `target_type` varchar(20) NOT NULL COMMENT 'booking/dish',
  `booking_id` varchar(20) DEFAULT NULL,
  `dish_id` varchar(20) DEFAULT NULL,
  `dish_name` varchar(100) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `note` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ld_zero_point_menu`
--

DROP TABLE IF EXISTS `ld_zero_point_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ld_zero_point_menu` (
  `ld_id` varchar(20) NOT NULL,
  `update_time` date DEFAULT NULL,
  `menu_name` varchar(50) DEFAULT NULL,
  `menu_type` varchar(20) NOT NULL,
  `dish_code` varchar(20) DEFAULT NULL,
  `category_name` varchar(50) DEFAULT NULL,
  `dish_name` varchar(100) DEFAULT NULL,
  `price_str` varchar(50) DEFAULT NULL,
  `base_price` decimal(10,2) DEFAULT NULL,
  `cost_price` decimal(10,2) DEFAULT '0.00',
  `cost_rate` decimal(5,2) DEFAULT '0.00',
  `remark` text,
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ld_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `leave_record`
--

DROP TABLE IF EXISTS `leave_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_record` (
  `leave_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int NOT NULL,
  `leave_type` varchar(20) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `days` double DEFAULT '0',
  `status` varchar(20) DEFAULT 'pending',
  `reason` varchar(500) DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`leave_id`),
  KEY `idx_lv_staff` (`staff_id`),
  CONSTRAINT `fk_lr_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_activity`
--

DROP TABLE IF EXISTS `marketing_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `activity_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `activity_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `activity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `activity_rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `activity_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `target_customers` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `budget_amount` decimal(12,2) DEFAULT NULL,
  `actual_cost` decimal(12,2) DEFAULT '0.00',
  `expected_income` decimal(12,2) DEFAULT NULL,
  `actual_income` decimal(12,2) DEFAULT '0.00',
  `participant_count` int DEFAULT '0',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  UNIQUE KEY `uk_activity_code` (`activity_code`,`store_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_coupon`
--

DROP TABLE IF EXISTS `marketing_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_coupon` (
  `coupon_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `coupon_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `coupon_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `coupon_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_value` decimal(10,2) DEFAULT NULL,
  `min_consume` decimal(12,2) DEFAULT '0.00',
  `total_count` int DEFAULT '0',
  `received_count` int DEFAULT '0',
  `used_count` int DEFAULT '0',
  `valid_days` int DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `applicable_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'all',
  `applicable_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`coupon_id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`,`store_id`),
  KEY `idx_coupon_type` (`coupon_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_coupon_record`
--

DROP TABLE IF EXISTS `marketing_coupon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_coupon_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `coupon_id` bigint NOT NULL,
  `coupon_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coupon_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `member_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receive_time` datetime DEFAULT NULL,
  `use_time` datetime DEFAULT NULL,
  `expire_date` date DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'unused',
  `booking_id` int DEFAULT NULL,
  `booking_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券领取使用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_discount_rule`
--

DROP TABLE IF EXISTS `marketing_discount_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_discount_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `condition_amount` decimal(12,2) DEFAULT NULL,
  `condition_quantity` int DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `discount_rate` decimal(5,2) DEFAULT NULL,
  `gift_item_id` int DEFAULT NULL,
  `gift_item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `applicable_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'all',
  `applicable_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `priority` int DEFAULT '0',
  `stackable` tinyint DEFAULT '0',
  `is_active` tinyint NOT NULL DEFAULT '1',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_lottery`
--

DROP TABLE IF EXISTS `marketing_lottery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_lottery` (
  `lottery_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `lottery_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `lottery_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `daily_limit` int DEFAULT '1',
  `total_limit` int DEFAULT NULL,
  `cost_points` int DEFAULT '0',
  `cost_amount` decimal(12,2) DEFAULT '0.00',
  `is_active` tinyint NOT NULL DEFAULT '1',
  `prizes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `probability_rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`lottery_id`),
  UNIQUE KEY `uk_lottery_code` (`lottery_code`,`store_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖活动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_member_reward`
--

DROP TABLE IF EXISTS `marketing_member_reward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_member_reward` (
  `reward_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `reward_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_balance` decimal(12,2) DEFAULT '0.00',
  `reward_points` int DEFAULT '0',
  `reward_coupon_id` bigint DEFAULT NULL,
  `reward_coupon_count` int DEFAULT '0',
  `condition_value` decimal(12,2) DEFAULT NULL,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`reward_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员奖励规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marketing_promo_code`
--

DROP TABLE IF EXISTS `marketing_promo_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marketing_promo_code` (
  `code_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `promo_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_value` decimal(10,2) DEFAULT NULL,
  `min_consume` decimal(12,2) DEFAULT '0.00',
  `total_count` int DEFAULT '1',
  `used_count` int DEFAULT '0',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`code_id`),
  UNIQUE KEY `uk_promo_code` (`promo_code`,`store_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浼樻儬鐮佽〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meal_package`
--

DROP TABLE IF EXISTS `meal_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meal_package` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_name` varchar(100) NOT NULL COMMENT '套餐名称',
  `package_code` varchar(50) NOT NULL COMMENT '套餐编码',
  `package_type` varchar(20) DEFAULT NULL COMMENT '套餐类型：wedding-婚宴, birthday-寿宴, business-商务',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `package_price` decimal(10,2) DEFAULT NULL COMMENT '套餐价格',
  `servings` int DEFAULT '10' COMMENT '适用人数',
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `package_code` (`package_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_card`
--

DROP TABLE IF EXISTS `member_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_card` (
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `level_id` int DEFAULT NULL,
  `level_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_points` int NOT NULL DEFAULT '0',
  `total_recharge` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_consume` decimal(12,2) NOT NULL DEFAULT '0.00',
  `consume_count` int NOT NULL DEFAULT '0',
  `last_consume_date` date DEFAULT NULL,
  `register_date` date DEFAULT NULL,
  `register_store_id` bigint DEFAULT NULL,
  `referrer_id` bigint DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_card_no` (`card_no`,`store_id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡主档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_consume_record`
--

DROP TABLE IF EXISTS `member_consume_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_consume_record` (
  `consume_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `consume_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_id` bigint NOT NULL,
  `card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `member_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `consume_date` date NOT NULL,
  `booking_id` int DEFAULT NULL,
  `booking_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `consume_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) DEFAULT '0.00',
  `actual_amount` decimal(12,2) NOT NULL,
  `balance_pay` decimal(12,2) DEFAULT '0.00',
  `cash_pay` decimal(12,2) DEFAULT '0.00',
  `other_pay` decimal(12,2) DEFAULT '0.00',
  `balance_before` decimal(12,2) DEFAULT NULL,
  `balance_after` decimal(12,2) DEFAULT NULL,
  `points_earned` int DEFAULT '0',
  `points_used` int DEFAULT '0',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`consume_id`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_consume_date` (`consume_date`),
  KEY `idx_consume_no` (`consume_no`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员消费记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_level`
--

DROP TABLE IF EXISTS `member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_level` (
  `level_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `level_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `min_points` int DEFAULT '0',
  `min_recharge` decimal(12,2) DEFAULT '0.00',
  `discount_rate` decimal(5,2) DEFAULT '100.00',
  `point_rate` decimal(5,2) DEFAULT '1.00',
  `birthday_discount` decimal(5,2) DEFAULT '100.00',
  `benefits` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`level_id`),
  UNIQUE KEY `uk_level_code` (`level_code`,`store_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_point_log`
--

DROP TABLE IF EXISTS `member_point_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_point_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `member_id` bigint NOT NULL,
  `card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `member_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `change_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `change_points` int NOT NULL,
  `points_before` int DEFAULT NULL,
  `points_after` int DEFAULT NULL,
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `related_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_point_rule`
--

DROP TABLE IF EXISTS `member_point_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_point_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `point_value` int DEFAULT NULL,
  `amount_condition` decimal(12,2) DEFAULT NULL,
  `is_active` tinyint NOT NULL DEFAULT '1',
  `effective_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_recharge_record`
--

DROP TABLE IF EXISTS `member_recharge_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_recharge_record` (
  `recharge_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `recharge_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_id` bigint NOT NULL,
  `card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `member_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recharge_date` date NOT NULL,
  `recharge_amount` decimal(12,2) NOT NULL,
  `gift_amount` decimal(12,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL,
  `balance_before` decimal(12,2) DEFAULT NULL,
  `balance_after` decimal(12,2) DEFAULT NULL,
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recharge_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `activity_id` bigint DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`recharge_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_recharge_date` (`recharge_date`),
  KEY `idx_recharge_no` (`recharge_no`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储值充值记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menu_category`
--

DROP TABLE IF EXISTS `menu_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL,
  `menu_type` varchar(20) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menu_dish`
--

DROP TABLE IF EXISTS `menu_dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_dish` (
  `id` int NOT NULL AUTO_INCREMENT,
  `menu_type` varchar(20) NOT NULL,
  `menu_type_name` varchar(50) DEFAULT NULL,
  `dish_id` varchar(20) NOT NULL,
  `dish_name` varchar(100) DEFAULT NULL,
  `dish_category` varchar(50) DEFAULT NULL,
  `menu_category_id` varchar(20) DEFAULT NULL,
  `menu_category_name` varchar(50) DEFAULT NULL,
  `sale_price` decimal(10,2) DEFAULT '0.00',
  `special_price` decimal(10,2) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `table_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `dishes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `total_price` decimal(12,2) DEFAULT '0.00',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `created_at` bigint DEFAULT (unix_timestamp()),
  `kitchen_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '厨房状态: pending/备菜中/已完成/已出品',
  `kitchen_priority` int DEFAULT '0' COMMENT '优先级 0普通 1加急',
  `kitchen_started_at` bigint DEFAULT NULL COMMENT '开始备菜时间',
  `kitchen_finished_at` bigint DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_orders_table` (`table_id`),
  KEY `idx_orders_status` (`status`),
  KEY `idx_orders_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `overtime`
--

DROP TABLE IF EXISTS `overtime`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `overtime` (
  `overtime_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint DEFAULT '1',
  `staff_id` int NOT NULL,
  `overtime_date` date NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `hours` double DEFAULT '0',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`overtime_id`),
  KEY `idx_staff_date` (`staff_id`,`overtime_date`),
  CONSTRAINT `fk_ot_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `package_details`
--

DROP TABLE IF EXISTS `package_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `package_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dish_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seq` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pkg_details_pkg` (`package_code`),
  KEY `idx_pkg_details_dish` (`dish_code`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `package_dish_detail`
--

DROP TABLE IF EXISTS `package_dish_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `package_dish_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `package_id` varchar(20) NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  `dish_quantity` int DEFAULT '1',
  `dish_order` int DEFAULT '0',
  `custom_name` varchar(100) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_package` (`package_id`),
  KEY `idx_dish` (`dish_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `package_dish_rel`
--

DROP TABLE IF EXISTS `package_dish_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `package_dish_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `package_id` int NOT NULL COMMENT '套餐ID',
  `dish_id` varchar(20) NOT NULL COMMENT '菜品ID',
  `dish_name_snapshot` varchar(100) DEFAULT NULL COMMENT '菜品名称快照（防止改名后记录错乱）',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_dish` (`package_id`,`dish_id`),
  CONSTRAINT `package_dish_rel_ibfk_1` FOREIGN KEY (`package_id`) REFERENCES `meal_package` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐-菜品关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `package_master`
--

DROP TABLE IF EXISTS `package_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `package_master` (
  `package_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `package_name` varchar(100) NOT NULL,
  `package_total_price` decimal(10,2) DEFAULT '0.00',
  `package_cost_price` decimal(10,2) DEFAULT '0.00',
  `cost_rate` decimal(5,2) DEFAULT '0.00',
  `dish_count` int DEFAULT '0',
  `suggest_guests` int DEFAULT '10',
  `occasion_type` varchar(20) DEFAULT NULL,
  `package_series` varchar(20) DEFAULT NULL,
  `is_active` int DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`package_id`),
  KEY `idx_pm_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `packages`
--

DROP TABLE IF EXISTS `packages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `packages` (
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `en` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `price` decimal(10,2) NOT NULL,
  `people` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` bigint DEFAULT (unix_timestamp()),
  PRIMARY KEY (`code`),
  KEY `idx_packages_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pkg_used`
--

DROP TABLE IF EXISTS `pkg_used`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pkg_used` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `used_at` bigint DEFAULT (unix_timestamp()),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_key` (`order_key`),
  KEY `idx_pkg_used_key` (`order_key`),
  KEY `idx_pkg_used_at` (`used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `po_order`
--

DROP TABLE IF EXISTS `po_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(30) NOT NULL,
  `order_date` date NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) DEFAULT NULL,
  `buyer` varchar(50) DEFAULT NULL,
  `expected_date` date DEFAULT NULL,
  `status` int DEFAULT '0',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_po_order_no` (`order_no`),
  KEY `idx_po_date` (`order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `po_order_detail`
--

DROP TABLE IF EXISTS `po_order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_order_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(30) NOT NULL,
  `line_no` int DEFAULT '1',
  `material_id` varchar(50) NOT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `order_unit` varchar(20) DEFAULT NULL,
  `order_quantity` decimal(12,3) DEFAULT '0.000',
  `order_price` decimal(10,2) DEFAULT '0.00',
  `order_amount` decimal(12,2) DEFAULT '0.00',
  `received_quantity` decimal(12,3) DEFAULT '0.000',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pod_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `po_receipt`
--

DROP TABLE IF EXISTS `po_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_receipt` (
  `receipt_no` varchar(30) NOT NULL,
  `receipt_date` date NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) DEFAULT NULL,
  `order_no` varchar(30) DEFAULT NULL,
  `delivery_no` varchar(50) DEFAULT NULL,
  `buyer` varchar(50) DEFAULT NULL,
  `receiver` varchar(50) DEFAULT NULL,
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `status` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `remark` text,
  PRIMARY KEY (`receipt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `po_receipt_detail`
--

DROP TABLE IF EXISTS `po_receipt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_receipt_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `receipt_no` varchar(30) NOT NULL,
  `line_no` int DEFAULT '1',
  `material_id` varchar(50) NOT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `quantity` decimal(12,3) DEFAULT '0.000',
  `price` decimal(10,2) DEFAULT '0.00',
  `amount` decimal(12,2) DEFAULT '0.00',
  `order_no` varchar(30) DEFAULT NULL,
  `remark` text,
  PRIMARY KEY (`id`),
  KEY `idx_prd_receipt_no` (`receipt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `po_return`
--

DROP TABLE IF EXISTS `po_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_return` (
  `id` int NOT NULL AUTO_INCREMENT,
  `return_no` varchar(30) NOT NULL,
  `return_date` date NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) DEFAULT NULL,
  `material_id` varchar(50) DEFAULT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `quantity` decimal(12,3) DEFAULT '0.000',
  `price` decimal(10,2) DEFAULT '0.00',
  `amount` decimal(12,2) DEFAULT '0.00',
  `reason` varchar(500) DEFAULT NULL,
  `order_no` varchar(30) DEFAULT NULL,
  `receipt_no` varchar(30) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `status` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `return_no` (`return_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `procurement_request`
--

DROP TABLE IF EXISTS `procurement_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `procurement_request` (
  `request_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `request_no` varchar(50) NOT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `requester_id` int DEFAULT NULL,
  `requester_name` varchar(50) DEFAULT NULL,
  `request_date` date NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `total_amount` decimal(12,2) DEFAULT NULL,
  `reason` text,
  `urgency` varchar(20) DEFAULT NULL,
  `expected_date` date DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_comment` text,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `request_no` (`request_no`),
  KEY `idx_store_request` (`store_id`),
  KEY `idx_status_request` (`status`),
  KEY `idx_dept_request` (`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_date` date NOT NULL,
  `expected_date` date DEFAULT NULL,
  `total_quantity` decimal(10,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `received_quantity` decimal(10,2) DEFAULT '0.00',
  `received_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `order_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `purchaser_id` int DEFAULT NULL,
  `purchaser_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `warehouse_keeper_id` int DEFAULT NULL,
  `warehouse_keeper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `idx_order_no` (`order_no`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_order_detail`
--

DROP TABLE IF EXISTS `purchase_order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `ingredient_id` int DEFAULT NULL,
  `ingredient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `received_quantity` decimal(10,2) DEFAULT '0.00',
  `returned_quantity` decimal(10,2) DEFAULT '0.00',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_receipt`
--

DROP TABLE IF EXISTS `purchase_receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_receipt` (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `receipt_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_date` date NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_quantity` decimal(10,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'confirmed',
  `warehouse_keeper_id` int DEFAULT NULL,
  `warehouse_keeper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delivery_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`receipt_id`),
  UNIQUE KEY `idx_receipt_no` (`receipt_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_receipt_date` (`receipt_date`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库单主档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_receipt_detail`
--

DROP TABLE IF EXISTS `purchase_receipt_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_receipt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `order_detail_id` bigint DEFAULT NULL,
  `ingredient_id` int DEFAULT NULL,
  `ingredient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_quantity` decimal(10,2) DEFAULT NULL,
  `actual_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `quality_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'qualified',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_return`
--

DROP TABLE IF EXISTS `purchase_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_return` (
  `return_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `return_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `return_date` date NOT NULL,
  `receipt_id` bigint DEFAULT NULL,
  `receipt_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `supplier_id` int DEFAULT NULL,
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_quantity` decimal(10,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `return_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'confirmed',
  `warehouse_keeper_id` int DEFAULT NULL,
  `warehouse_keeper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`return_id`),
  UNIQUE KEY `idx_return_no` (`return_no`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_return_date` (`return_date`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单主档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_return_detail`
--

DROP TABLE IF EXISTS `purchase_return_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_return_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `return_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `receipt_detail_id` bigint DEFAULT NULL,
  `ingredient_id` int DEFAULT NULL,
  `ingredient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `return_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `return_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_return_id` (`return_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reimbursement`
--

DROP TABLE IF EXISTS `reimbursement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reimbursement` (
  `reimbursement_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `reimbursement_no` varchar(50) NOT NULL,
  `applicant_id` int DEFAULT NULL,
  `applicant_name` varchar(50) DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `reimburse_date` date NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `reimburse_type` varchar(50) DEFAULT NULL,
  `receipt_count` int DEFAULT NULL,
  `receipt_file_path` varchar(255) DEFAULT NULL,
  `purpose` text,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_comment` text,
  `finance_approver_id` int DEFAULT NULL,
  `finance_approver_name` varchar(50) DEFAULT NULL,
  `finance_approve_time` datetime DEFAULT NULL,
  `payment_date` date DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reimbursement_id`),
  UNIQUE KEY `reimbursement_no` (`reimbursement_no`),
  KEY `idx_store_reimburse` (`store_id`),
  KEY `idx_status_reimburse` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_daily`
--

DROP TABLE IF EXISTS `report_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_daily` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `report_date` date NOT NULL,
  `week_day` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_holiday` tinyint DEFAULT '0',
  `weather` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_booking_count` int DEFAULT '0',
  `total_guest_count` int DEFAULT '0',
  `total_table_count` int DEFAULT '0',
  `table_turnover_rate` decimal(5,2) DEFAULT NULL,
  `total_revenue` decimal(12,2) DEFAULT '0.00',
  `food_revenue` decimal(12,2) DEFAULT '0.00',
  `beverage_revenue` decimal(12,2) DEFAULT '0.00',
  `other_revenue` decimal(12,2) DEFAULT '0.00',
  `member_recharge` decimal(12,2) DEFAULT '0.00',
  `total_cost` decimal(12,2) DEFAULT '0.00',
  `food_cost` decimal(12,2) DEFAULT '0.00',
  `labor_cost` decimal(12,2) DEFAULT '0.00',
  `rent_cost` decimal(12,2) DEFAULT '0.00',
  `utility_cost` decimal(12,2) DEFAULT '0.00',
  `other_cost` decimal(12,2) DEFAULT '0.00',
  `gross_profit` decimal(12,2) DEFAULT '0.00',
  `gross_profit_rate` decimal(5,2) DEFAULT NULL,
  `net_profit` decimal(12,2) DEFAULT '0.00',
  `net_profit_rate` decimal(5,2) DEFAULT NULL,
  `food_cost_rate` decimal(5,2) DEFAULT NULL,
  `avg_consumption` decimal(10,2) DEFAULT NULL,
  `avg_table_spending` decimal(10,2) DEFAULT NULL,
  `new_member_count` int DEFAULT '0',
  `active_member_count` int DEFAULT '0',
  `member_consume_count` int DEFAULT '0',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_store_date` (`store_id`,`report_date`),
  KEY `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_department_cost`
--

DROP TABLE IF EXISTS `report_department_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_department_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `stat_date` date NOT NULL,
  `stat_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `department_id` int DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `labor_cost` decimal(12,2) DEFAULT '0.00',
  `material_cost` decimal(12,2) DEFAULT '0.00',
  `other_cost` decimal(12,2) DEFAULT '0.00',
  `total_cost` decimal(12,2) DEFAULT '0.00',
  `output_value` decimal(12,2) DEFAULT '0.00',
  `cost_rate` decimal(5,2) DEFAULT NULL,
  `staff_count` int DEFAULT NULL,
  `per_capita_cost` decimal(10,2) DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门成本统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_dish_sales`
--

DROP TABLE IF EXISTS `report_dish_sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_dish_sales` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `stat_date` date NOT NULL,
  `stat_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dish_id` int DEFAULT NULL,
  `dish_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `spicy_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `main_ingredient_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sale_quantity` decimal(10,2) DEFAULT '0.00',
  `sale_amount` decimal(12,2) DEFAULT '0.00',
  `cost_amount` decimal(12,2) DEFAULT '0.00',
  `gross_profit` decimal(12,2) DEFAULT '0.00',
  `gross_profit_rate` decimal(5,2) DEFAULT NULL,
  `refund_quantity` decimal(10,2) DEFAULT '0.00',
  `refund_amount` decimal(12,2) DEFAULT '0.00',
  `sale_rank` int DEFAULT NULL,
  `amount_rank` int DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品销售统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_monthly`
--

DROP TABLE IF EXISTS `report_monthly`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_monthly` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `report_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_year` int DEFAULT NULL,
  `report_month_of_year` int DEFAULT NULL,
  `total_booking_count` int DEFAULT '0',
  `total_guest_count` int DEFAULT '0',
  `total_table_count` int DEFAULT '0',
  `avg_daily_guest` decimal(10,2) DEFAULT NULL,
  `table_turnover_rate` decimal(5,2) DEFAULT NULL,
  `total_revenue` decimal(12,2) DEFAULT '0.00',
  `food_revenue` decimal(12,2) DEFAULT '0.00',
  `beverage_revenue` decimal(12,2) DEFAULT '0.00',
  `other_revenue` decimal(12,2) DEFAULT '0.00',
  `member_recharge` decimal(12,2) DEFAULT '0.00',
  `total_cost` decimal(12,2) DEFAULT '0.00',
  `food_cost` decimal(12,2) DEFAULT '0.00',
  `labor_cost` decimal(12,2) DEFAULT '0.00',
  `rent_cost` decimal(12,2) DEFAULT '0.00',
  `utility_cost` decimal(12,2) DEFAULT '0.00',
  `other_cost` decimal(12,2) DEFAULT '0.00',
  `gross_profit` decimal(12,2) DEFAULT '0.00',
  `gross_profit_rate` decimal(5,2) DEFAULT NULL,
  `net_profit` decimal(12,2) DEFAULT '0.00',
  `net_profit_rate` decimal(5,2) DEFAULT NULL,
  `food_cost_rate` decimal(5,2) DEFAULT NULL,
  `avg_consumption` decimal(10,2) DEFAULT NULL,
  `avg_table_spending` decimal(10,2) DEFAULT NULL,
  `new_member_count` int DEFAULT '0',
  `total_member_count` int DEFAULT '0',
  `active_member_count` int DEFAULT '0',
  `total_purchase_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_store_month` (`store_id`,`report_month`),
  KEY `idx_report_month` (`report_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_staff_kpi`
--

DROP TABLE IF EXISTS `report_staff_kpi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_staff_kpi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `stat_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `staff_id` int DEFAULT NULL,
  `staff_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attendance_days` decimal(5,1) DEFAULT '0.0',
  `late_count` int DEFAULT '0',
  `early_leave_count` int DEFAULT '0',
  `absent_days` decimal(5,1) DEFAULT '0.0',
  `overtime_hours` decimal(6,1) DEFAULT '0.0',
  `leave_days` decimal(5,1) DEFAULT '0.0',
  `performance_score` decimal(5,2) DEFAULT '0.00',
  `performance_rank` int DEFAULT NULL,
  `sale_amount` decimal(12,2) DEFAULT '0.00',
  `service_count` int DEFAULT '0',
  `customer_praise` int DEFAULT '0',
  `customer_complaint` int DEFAULT '0',
  `reward_count` int DEFAULT '0',
  `penalty_count` int DEFAULT '0',
  `kpi_score` decimal(5,2) DEFAULT '0.00',
  `kpi_grade` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_department` (`department`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_stat_month` (`stat_month`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工KPI统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requisition_detail`
--

DROP TABLE IF EXISTS `requisition_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requisition_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `requisition_id` bigint NOT NULL,
  `line_no` int NOT NULL,
  `ingredient_id` varchar(50) NOT NULL,
  `ingredient_name` varchar(100) NOT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `request_quantity` decimal(10,3) NOT NULL,
  `issue_quantity` decimal(10,3) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_requisition_detail` (`requisition_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requisition_order`
--

DROP TABLE IF EXISTS `requisition_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requisition_order` (
  `requisition_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `requisition_no` varchar(50) NOT NULL,
  `department_id` int DEFAULT NULL,
  `department_name` varchar(50) DEFAULT NULL,
  `requester_id` int DEFAULT NULL,
  `requester_name` varchar(50) DEFAULT NULL,
  `requisition_date` date NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `total_amount` decimal(12,2) DEFAULT NULL,
  `reason` text,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `warehouse_keeper_id` int DEFAULT NULL,
  `warehouse_keeper_name` varchar(50) DEFAULT NULL,
  `issue_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`requisition_id`),
  UNIQUE KEY `requisition_no` (`requisition_no`),
  KEY `idx_store_requisition` (`store_id`),
  KEY `idx_status_requisition` (`status`),
  KEY `idx_dept_requisition` (`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schedule`
--

DROP TABLE IF EXISTS `schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule` (
  `schedule_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int NOT NULL,
  `schedule_date` date NOT NULL,
  `shift_type` varchar(20) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT 'normal',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`schedule_id`),
  KEY `idx_sc_staff` (`staff_id`),
  KEY `idx_sc_date` (`schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_info`
--

DROP TABLE IF EXISTS `staff_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `department` varchar(50) DEFAULT NULL,
  `position` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` int DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_master`
--

DROP TABLE IF EXISTS `staff_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_master` (
  `staff_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_name` varchar(20) NOT NULL,
  `staff_account` varchar(20) DEFAULT NULL,
  `staff_password` varchar(100) DEFAULT NULL,
  `staff_gender` varchar(2) DEFAULT NULL,
  `staff_age` int DEFAULT NULL,
  `staff_phone` varchar(20) DEFAULT NULL,
  `staff_position` varchar(50) DEFAULT NULL,
  `department` varchar(50) DEFAULT NULL,
  `dept_id` int DEFAULT NULL,
  `role` varchar(30) DEFAULT 'staff',
  `permission_level` int DEFAULT '1',
  `can_manage_kitchen` tinyint(1) DEFAULT '0',
  `can_manage_sales` tinyint(1) DEFAULT '0',
  `can_manage_finance` tinyint(1) DEFAULT '0',
  `can_manage_hr` tinyint(1) DEFAULT '0',
  `can_view_all_stores` tinyint(1) DEFAULT '0',
  `can_edit_system` tinyint(1) DEFAULT '0',
  `employment_status` varchar(10) DEFAULT 'active',
  `monthly_salary` decimal(10,2) DEFAULT NULL,
  `hire_date` date DEFAULT NULL,
  `id_card` varchar(20) DEFAULT NULL,
  `home_address` varchar(100) DEFAULT NULL,
  `emergency_contact` varchar(20) DEFAULT NULL,
  `emergency_phone` varchar(20) DEFAULT NULL,
  `resign_reason` text,
  `resign_date` date DEFAULT NULL,
  `remark` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`staff_id`),
  UNIQUE KEY `uk_staff_account` (`staff_account`),
  KEY `idx_sm_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_bill_detail`
--

DROP TABLE IF EXISTS `stock_bill_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_bill_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `bill_id` int DEFAULT NULL,
  `bill_no` varchar(50) DEFAULT NULL,
  `line_no` int DEFAULT '1',
  `material_id` varchar(50) DEFAULT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `quantity` decimal(12,3) DEFAULT '0.000',
  `unit_price` decimal(10,4) DEFAULT '0.0000',
  `amount` decimal(12,2) DEFAULT '0.00',
  `warehouse_id` int DEFAULT '1',
  `remark` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_inventory`
--

DROP TABLE IF EXISTS `stock_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `material_id` varchar(50) NOT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `warehouse_id` int DEFAULT '1',
  `stock_type` varchar(20) DEFAULT 'material',
  `current_qty` decimal(12,3) DEFAULT '0.000',
  `unit` varchar(20) DEFAULT NULL,
  `avg_cost` decimal(10,4) DEFAULT '0.0000',
  `safety_qty` decimal(12,3) DEFAULT '0.000',
  `last_in_date` date DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock` (`material_id`,`warehouse_id`,`stock_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_loss`
--

DROP TABLE IF EXISTS `stock_loss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_loss` (
  `loss_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `loss_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `loss_date` date NOT NULL,
  `loss_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_quantity` decimal(10,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `applicant_id` int DEFAULT NULL,
  `applicant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approver_id` int DEFAULT NULL,
  `approver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `warehouse_keeper_id` int DEFAULT NULL,
  `warehouse_keeper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`loss_id`),
  UNIQUE KEY `idx_loss_no` (`loss_no`),
  KEY `idx_loss_date` (`loss_date`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鎶ユ崯鍗曚富妗ｈ〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_loss_detail`
--

DROP TABLE IF EXISTS `stock_loss_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_loss_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `loss_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `ingredient_id` int DEFAULT NULL,
  `ingredient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loss_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `loss_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_loss_id` (`loss_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报损明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_take`
--

DROP TABLE IF EXISTS `stock_take`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_take` (
  `take_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `take_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `take_date` date NOT NULL,
  `take_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'full',
  `category_id` int DEFAULT NULL,
  `warehouse_id` int DEFAULT NULL,
  `total_items` int DEFAULT '0',
  `total_diff_items` int DEFAULT '0',
  `total_diff_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `supervisor_id` int DEFAULT NULL,
  `supervisor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`take_id`),
  UNIQUE KEY `idx_take_no` (`take_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_date` (`take_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单主档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_take_detail`
--

DROP TABLE IF EXISTS `stock_take_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_take_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `take_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `line_no` int NOT NULL,
  `ingredient_id` int DEFAULT NULL,
  `ingredient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `system_quantity` decimal(10,2) NOT NULL,
  `system_amount` decimal(12,2) DEFAULT NULL,
  `actual_quantity` decimal(10,2) NOT NULL,
  `actual_amount` decimal(12,2) DEFAULT NULL,
  `diff_quantity` decimal(10,2) DEFAULT '0.00',
  `diff_amount` decimal(12,2) DEFAULT '0.00',
  `diff_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_id` (`take_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_transaction`
--

DROP TABLE IF EXISTS `stock_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transaction` (
  `id` int NOT NULL AUTO_INCREMENT,
  `trans_date` date NOT NULL,
  `bill_no` varchar(50) DEFAULT NULL,
  `bill_type` varchar(30) DEFAULT NULL,
  `warehouse_id` int DEFAULT '1',
  `item_type` varchar(20) DEFAULT 'material',
  `material_id` varchar(50) NOT NULL,
  `material_name` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `in_qty` decimal(12,3) DEFAULT '0.000',
  `out_qty` decimal(12,3) DEFAULT '0.000',
  `unit_cost` decimal(10,4) DEFAULT '0.0000',
  `operator` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_st_date` (`trans_date`),
  KEY `idx_st_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_transfer`
--

DROP TABLE IF EXISTS `stock_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transfer` (
  `transfer_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `transfer_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `transfer_date` date NOT NULL,
  `from_warehouse_id` int DEFAULT NULL,
  `from_warehouse_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_warehouse_id` int DEFAULT NULL,
  `to_warehouse_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_quantity` decimal(10,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `out_time` datetime DEFAULT NULL,
  `in_time` datetime DEFAULT NULL,
  `operator_out_id` int DEFAULT NULL,
  `operator_out_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_in_id` int DEFAULT NULL,
  `operator_in_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transfer_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transfer_id`),
  UNIQUE KEY `idx_transfer_no` (`transfer_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_transfer_date` (`transfer_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搴撳瓨璋冩嫧鍗曡〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_info`
--

DROP TABLE IF EXISTS `store_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_info` (
  `store_id` bigint NOT NULL AUTO_INCREMENT,
  `store_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `store_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `store_short_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `store_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `store_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_hours` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `table_count` int DEFAULT '0',
  `max_capacity` int DEFAULT '0',
  `business_area` decimal(8,2) DEFAULT NULL,
  `manager_id` int DEFAULT NULL,
  `manager_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `opening_date` date DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'open',
  `tax_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `store_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_id`),
  UNIQUE KEY `uk_store_code` (`store_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `supplier_master`
--

DROP TABLE IF EXISTS `supplier_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_master` (
  `supplier_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `supplier_code` varchar(20) DEFAULT NULL,
  `supplier_name` varchar(100) NOT NULL,
  `contact_person` varchar(50) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `bank_account` varchar(50) DEFAULT NULL,
  `platform_account` varchar(100) DEFAULT NULL,
  `main_products` text,
  `wechat_account` varchar(50) DEFAULT NULL,
  `alipay_account` varchar(50) DEFAULT NULL,
  `taobao_account` varchar(50) DEFAULT NULL,
  `supplier_rating` int DEFAULT '5',
  `is_active` int DEFAULT '1',
  `remark` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict`
--

DROP TABLE IF EXISTS `sys_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT,
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dict_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'list',
  `store_id` bigint NOT NULL DEFAULT '1',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `is_active` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`,`store_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_item`
--

DROP TABLE IF EXISTS `sys_dict_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `dict_id` bigint NOT NULL,
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `store_id` bigint NOT NULL DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `is_active` tinyint DEFAULT '1',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_dict_value` (`dict_id`,`item_value`,`store_id`),
  KEY `idx_dict_code` (`dict_code`,`store_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏁版嵁瀛楀吀椤硅〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID：0=顶级',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端路由路径',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单图标',
  `permission_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联权限编码（sys_permission.permission_code）',
  `menu_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'menu' COMMENT '类型：directory=目录 / menu=菜单 / button=按钮',
  `store_scope` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'all' COMMENT '门店范围：all=全门店 / hq=仅总店 / branch=仅分店',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否可见：1=可见 0=隐藏',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`),
  KEY `idx_menu_perm` (`permission_code`),
  KEY `idx_menu_sort` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前端动态菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notification`
--

DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `notify_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `notify_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notify_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `notify_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `sender_id` int DEFAULT NULL,
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `send_time` datetime DEFAULT NULL,
  `receiver_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'all',
  `receiver_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `is_read` tinyint DEFAULT '0',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'published',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notify_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_notify_type` (`notify_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `operation_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operation_action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `request_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `diff_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'success',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `cost_time` int DEFAULT NULL,
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（唯一，如 booking:list）',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `url` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口URL匹配模式，如 /api/bookings/**',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'HTTP方法：GET/POST/PUT/DELETE/*',
  `perm_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'api' COMMENT '权限类型：api=接口 / menu=菜单 / button=按钮',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_perm_url` (`url`),
  KEY `idx_perm_method` (`method`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统接口权限点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码（唯一）',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `store_id` bigint NOT NULL DEFAULT '0' COMMENT '所属门店ID：0=全局，1=总店，2=分店',
  `data_scope` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'store' COMMENT '数据范围：all=全门店 / store=本店',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_store` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID（sys_role.role_id）',
  `permission_id` bigint NOT NULL COMMENT '权限ID（sys_permission.permission_id）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_rp_role` (`role_id`),
  KEY `idx_rp_perm` (`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `staff_id` bigint NOT NULL COMMENT '员工ID（staff_master.staff_id）',
  `role_id` bigint NOT NULL COMMENT '角色ID（sys_role.role_id）',
  `store_id` bigint NOT NULL DEFAULT '0' COMMENT '冗余门店ID，便于按门店筛选',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_role` (`staff_id`,`role_id`),
  KEY `idx_ur_staff` (`staff_id`),
  KEY `idx_ur_role` (`role_id`),
  KEY `idx_ur_store` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `table_master`
--

DROP TABLE IF EXISTS `table_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_master` (
  `table_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_number` varchar(10) NOT NULL,
  `table_name` varchar(20) DEFAULT NULL,
  `table_location` varchar(50) DEFAULT NULL,
  `table_area` varchar(20) DEFAULT NULL,
  `table_capacity` int DEFAULT '10',
  `table_type` varchar(20) DEFAULT NULL,
  `table_status` varchar(20) DEFAULT 'available',
  `min_capacity` int DEFAULT '6',
  `max_capacity` int DEFAULT '12',
  `sort_order` int DEFAULT '0',
  `is_active` int DEFAULT '1',
  `remark` text,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`table_id`),
  KEY `idx_tm_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_category_rel`
--

DROP TABLE IF EXISTS `template_category_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template_category_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `template_id` int NOT NULL COMMENT '模板ID',
  `menu_category_id` int NOT NULL COMMENT '零点分类ID',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_category` (`template_id`,`menu_category_id`),
  KEY `menu_category_id` (`menu_category_id`),
  CONSTRAINT `template_category_rel_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `banquet_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `template_category_rel_ibfk_2` FOREIGN KEY (`menu_category_id`) REFERENCES `menu_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板-分类关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_dish_rel`
--

DROP TABLE IF EXISTS `template_dish_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template_dish_rel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `template_id` int NOT NULL COMMENT '模板ID',
  `dish_id` varchar(20) NOT NULL COMMENT '菜品ID',
  `store_id` bigint NOT NULL DEFAULT '1',
  `menu_category_id` int DEFAULT NULL COMMENT '在该模板下的零点分类ID',
  `special_price` decimal(10,2) DEFAULT NULL COMMENT '特殊价格（覆盖菜品标准价）',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_dish` (`template_id`,`dish_id`),
  KEY `menu_category_id` (`menu_category_id`),
  CONSTRAINT `template_dish_rel_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `banquet_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `template_dish_rel_ibfk_2` FOREIGN KEY (`menu_category_id`) REFERENCES `menu_category` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=1153 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板-菜品关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unit_conversion`
--

DROP TABLE IF EXISTS `unit_conversion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unit_conversion` (
  `conversion_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `from_unit` varchar(20) NOT NULL,
  `to_unit` varchar(20) NOT NULL,
  `conversion_rate` decimal(15,6) NOT NULL,
  `reverse_rate` decimal(15,6) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'active',
  `created_by` varchar(50) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`conversion_id`),
  KEY `idx_from_to_unit` (`from_unit`,`to_unit`),
  KEY `idx_store_conversion` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'user',
  `create_time` bigint DEFAULT (unix_timestamp()),
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse`
--

DROP TABLE IF EXISTS `warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse` (
  `id` int NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(20) DEFAULT NULL,
  `warehouse_name` varchar(50) NOT NULL,
  `store_id` int DEFAULT '1',
  `location` varchar(100) DEFAULT NULL,
  `manager` varchar(50) DEFAULT NULL,
  `status` int DEFAULT '1',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yield_rate_config`
--

DROP TABLE IF EXISTS `yield_rate_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `yield_rate_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `ingredient_id` varchar(50) NOT NULL,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `raw_unit` varchar(20) NOT NULL,
  `processed_unit` varchar(20) NOT NULL,
  `standard_yield_rate` decimal(5,2) DEFAULT NULL,
  `min_yield_rate` decimal(5,2) DEFAULT NULL,
  `max_yield_rate` decimal(5,2) DEFAULT NULL,
  `loss_reason` text,
  `status` varchar(20) DEFAULT 'active',
  `effective_date` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  KEY `idx_store_yield` (`store_id`),
  KEY `idx_ingredient_yield` (`ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-31 13:33:59
