-- ============================================================
-- 餐厅点餐系统完整迁移脚本 v2
-- 来源：springboot239餐厅点餐系统 (MyBatis → JPA + 多租户)
-- 目标：将14张表迁移为 bt_ 前缀 + store_id 多租户隔离
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- 兼容：MySQL 8.0+
-- 说明：原表名（如 caipinleixing）→ 新表名（如 bt_dish_type）
--       若表不存在，用 CREATE TABLE 创建（含 store_id）
--       若表已存在，用 ALTER TABLE 补全缺失字段
-- 依赖：依赖 banquet 数据库已有 store_info 表
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 通用辅助：安全添加 store_id 列（MySQL 8.0 兼容）
-- 使用 INFORMATION_SCHEMA 检查列是否存在，避免 MariaDB 语法
-- ============================================================

-- ============================================================
-- 1. bt_dish_type (菜品类型表) ← caipinleixing
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_dish_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `caipinleixing` varchar(200) NOT NULL COMMENT '菜品类型',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品类型';

-- 安全补全：若表已存在但缺少 store_id（MySQL 8.0 兼容写法）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_type' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_dish_type` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_dish_type'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_type' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_dish_type` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_dish_type'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_dish_type` (`id`, `addtime`, `caipinleixing`, `store_id`) VALUES
(41, '2024-04-08 06:12:11', '菜品类型1', 1),
(42, '2024-04-08 06:12:11', '菜品类型2', 1),
(43, '2024-04-08 06:12:11', '菜品类型3', 1),
(44, '2024-04-08 06:12:11', '菜品类型4', 1),
(45, '2024-04-08 06:12:11', '菜品类型5', 1),
(46, '2024-04-08 06:12:11', '菜品类型6', 1),
(47, '2024-04-08 06:12:11', '菜品类型7', 1),
(48, '2024-04-08 06:12:11', '菜品类型8', 1);

-- ============================================================
-- 2. bt_dish_info (菜品信息表) ← caipinxinxi
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_dish_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `caipinmingcheng` varchar(200) NOT NULL COMMENT '菜品名称',
  `caipinleixing` varchar(200) NOT NULL COMMENT '菜品类型',
  `tupian` longtext COMMENT '图片',
  `kouwei` varchar(200) DEFAULT NULL COMMENT '口味',
  `yujishijian` varchar(200) DEFAULT NULL COMMENT '预计时间',
  `caipinjieshao` longtext COMMENT '菜品介绍',
  `fabushijian` datetime DEFAULT NULL COMMENT '发布时间',
  `clicktime` datetime DEFAULT NULL COMMENT '最近点击时间',
  `price` float NOT NULL COMMENT '价格',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_caipinleixing` (`caipinleixing`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品信息';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_info' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_dish_info` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_dish_info'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_info' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_dish_info` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_dish_info'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_dish_info` (`id`, `addtime`, `caipinmingcheng`, `caipinleixing`, `tupian`, `kouwei`, `yujishijian`, `caipinjieshao`, `fabushijian`, `clicktime`, `price`, `store_id`) VALUES
(51, '2024-04-08 06:12:11', '菜品名称1', '菜品类型1', 'upload/caipinxinxi_tupian1.jpg,upload/caipinxinxi_tupian2.jpg,upload/caipinxinxi_tupian3.jpg', '口味1', '预计时间1', '菜品介绍1', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(52, '2024-04-08 06:12:11', '菜品名称2', '菜品类型2', 'upload/caipinxinxi_tupian2.jpg,upload/caipinxinxi_tupian3.jpg,upload/caipinxinxi_tupian4.jpg', '口味2', '预计时间2', '菜品介绍2', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(53, '2024-04-08 06:12:11', '菜品名称3', '菜品类型3', 'upload/caipinxinxi_tupian3.jpg,upload/caipinxinxi_tupian4.jpg,upload/caipinxinxi_tupian5.jpg', '口味3', '预计时间3', '菜品介绍3', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(54, '2024-04-08 06:12:11', '菜品名称4', '菜品类型4', 'upload/caipinxinxi_tupian4.jpg,upload/caipinxinxi_tupian5.jpg,upload/caipinxinxi_tupian6.jpg', '口味4', '预计时间4', '菜品介绍4', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(55, '2024-04-08 06:12:11', '菜品名称5', '菜品类型5', 'upload/caipinxinxi_tupian5.jpg,upload/caipinxinxi_tupian6.jpg,upload/caipinxinxi_tupian7.jpg', '口味5', '预计时间5', '菜品介绍5', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(56, '2024-04-08 06:12:11', '菜品名称6', '菜品类型6', 'upload/caipinxinxi_tupian6.jpg,upload/caipinxinxi_tupian7.jpg,upload/caipinxinxi_tupian8.jpg', '口味6', '预计时间6', '菜品介绍6', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(57, '2024-04-08 06:12:11', '菜品名称7', '菜品类型7', 'upload/caipinxinxi_tupian7.jpg,upload/caipinxinxi_tupian8.jpg,upload/caipinxinxi_tupian9.jpg', '口味7', '预计时间7', '菜品介绍7', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1),
(58, '2024-04-08 06:12:11', '菜品名称8', '菜品类型8', 'upload/caipinxinxi_tupian8.jpg,upload/caipinxinxi_tupian9.jpg,upload/caipinxinxi_tupian10.jpg', '口味8', '预计时间8', '菜品介绍8', '2024-04-08 14:12:11', '2024-04-08 14:12:11', 99.9, 1);

-- ============================================================
-- 3. bt_table_info (餐桌信息表) ← canzhuoxinxi
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_table_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `canzhuohaoma` varchar(200) NOT NULL COMMENT '餐桌号码',
  `tupian` longtext COMMENT '图片',
  `kezuorenshu` int(11) DEFAULT NULL COMMENT '可坐人数',
  `canzhuoweizhi` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `canzhuozhuangtai` varchar(200) DEFAULT NULL COMMENT '餐桌状态',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_haoma` (`store_id`, `canzhuohaoma`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐桌信息';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_table_info' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_table_info` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_table_info'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_table_info' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_table_info` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_table_info'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_table_info` (`id`, `addtime`, `canzhuohaoma`, `tupian`, `kezuorenshu`, `canzhuoweizhi`, `canzhuozhuangtai`, `store_id`) VALUES
(21, '2024-04-08 06:12:11', '餐桌号码1', 'upload/canzhuoxinxi_tupian1.jpg,upload/canzhuoxinxi_tupian2.jpg,upload/canzhuoxinxi_tupian3.jpg', 1, '餐桌位置1', '使用中', 1),
(22, '2024-04-08 06:12:11', '餐桌号码2', 'upload/canzhuoxinxi_tupian2.jpg,upload/canzhuoxinxi_tupian3.jpg,upload/canzhuoxinxi_tupian4.jpg', 2, '餐桌位置2', '使用中', 1),
(23, '2024-04-08 06:12:11', '餐桌号码3', 'upload/canzhuoxinxi_tupian3.jpg,upload/canzhuoxinxi_tupian4.jpg,upload/canzhuoxinxi_tupian5.jpg', 3, '餐桌位置3', '使用中', 1),
(24, '2024-04-08 06:12:11', '餐桌号码4', 'upload/canzhuoxinxi_tupian4.jpg,upload/canzhuoxinxi_tupian5.jpg,upload/canzhuoxinxi_tupian6.jpg', 4, '餐桌位置4', '使用中', 1),
(25, '2024-04-08 06:12:11', '餐桌号码5', 'upload/canzhuoxinxi_tupian5.jpg,upload/canzhuoxinxi_tupian6.jpg,upload/canzhuoxinxi_tupian7.jpg', 5, '餐桌位置5', '使用中', 1),
(26, '2024-04-08 06:12:11', '餐桌号码6', 'upload/canzhuoxinxi_tupian6.jpg,upload/canzhuoxinxi_tupian7.jpg,upload/canzhuoxinxi_tupian8.jpg', 6, '餐桌位置6', '使用中', 1),
(27, '2024-04-08 06:12:11', '餐桌号码7', 'upload/canzhuoxinxi_tupian7.jpg,upload/canzhuoxinxi_tupian8.jpg,upload/canzhuoxinxi_tupian9.jpg', 7, '餐桌位置7', '使用中', 1),
(28, '2024-04-08 06:12:11', '餐桌号码8', 'upload/canzhuoxinxi_tupian8.jpg,upload/canzhuoxinxi_tupian9.jpg,upload/canzhuoxinxi_tupian10.jpg', 8, '餐桌位置8', '使用中', 1);

-- ============================================================
-- 4. bt_table_usage (餐桌使用表) ← canzhuoshiyong
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_table_usage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `canzhuohaoma` varchar(200) DEFAULT NULL COMMENT '餐桌号码',
  `canzhuoweizhi` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `kezuorenshu` int(11) DEFAULT NULL COMMENT '可坐人数',
  `shiyongshijian` datetime DEFAULT NULL COMMENT '使用时间',
  `yonghuming` varchar(200) DEFAULT NULL COMMENT '用户名',
  `xingming` varchar(200) DEFAULT NULL COMMENT '姓名',
  `shouji` varchar(200) DEFAULT NULL COMMENT '手机',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_canzhuohaoma` (`canzhuohaoma`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐桌使用';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_table_usage' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_table_usage` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_table_usage'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_table_usage' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_table_usage` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_table_usage'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_table_usage` (`id`, `addtime`, `canzhuohaoma`, `canzhuoweizhi`, `kezuorenshu`, `shiyongshijian`, `yonghuming`, `xingming`, `shouji`, `store_id`) VALUES
(31, '2024-04-08 06:12:11', '餐桌号码1', '餐桌位置1', 1, '2024-04-08 14:12:11', '用户名1', '姓名1', '13823888881', 1),
(32, '2024-04-08 06:12:11', '餐桌号码2', '餐桌位置2', 2, '2024-04-08 14:12:11', '用户名2', '姓名2', '13823888882', 1),
(33, '2024-04-08 06:12:11', '餐桌号码3', '餐桌位置3', 3, '2024-04-08 14:12:11', '用户名3', '姓名3', '13823888883', 1),
(34, '2024-04-08 06:12:11', '餐桌号码4', '餐桌位置4', 4, '2024-04-08 14:12:11', '用户名4', '姓名4', '13823888884', 1),
(35, '2024-04-08 06:12:11', '餐桌号码5', '餐桌位置5', 5, '2024-04-08 14:12:11', '用户名5', '姓名5', '13823888885', 1),
(36, '2024-04-08 06:12:11', '餐桌号码6', '餐桌位置6', 6, '2024-04-08 14:12:11', '用户名6', '姓名6', '13823888886', 1),
(37, '2024-04-08 06:12:11', '餐桌号码7', '餐桌位置7', 7, '2024-04-08 14:12:11', '用户名7', '姓名7', '13823888887', 1),
(38, '2024-04-08 06:12:11', '餐桌号码8', '餐桌位置8', 8, '2024-04-08 14:12:11', '用户名8', '姓名8', '13823888888', 1);

-- ============================================================
-- 5. bt_address (地址表) ← address
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `address` varchar(200) NOT NULL COMMENT '地址',
  `name` varchar(200) NOT NULL COMMENT '收货人',
  `phone` varchar(200) NOT NULL COMMENT '电话',
  `isdefault` varchar(200) NOT NULL COMMENT '是否默认地址[是/否]',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地址';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_address' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_address` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_address'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_address' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_address` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_address'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`, `store_id`) VALUES
(1, '2024-04-08 06:12:11', 11, '宇宙银河系金星1号', '金某', '13823888881', '是', 1),
(2, '2024-04-08 06:12:11', 12, '宇宙银河系木星1号', '木某', '13823888882', '是', 1),
(3, '2024-04-08 06:12:11', 13, '宇宙银河系水星1号', '水某', '13823888883', '是', 1),
(4, '2024-04-08 06:12:11', 14, '宇宙银河系火星1号', '火某', '13823888884', '是', 1),
(5, '2024-04-08 06:12:11', 15, '宇宙银河系土星1号', '土某', '13823888885', '是', 1),
(6, '2024-04-08 06:12:11', 16, '宇宙银河系月球1号', '月某', '13823888886', '是', 1),
(7, '2024-04-08 06:12:11', 17, '宇宙银河系黑洞1号', '黑某', '13823888887', '是', 1),
(8, '2024-04-08 06:12:11', 18, '宇宙银河系地球1号', '地某', '13823888888', '是', 1);

-- ============================================================
-- 6. bt_cart (购物车表) ← cart
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_cart` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tablename` varchar(200) DEFAULT 'caipinxinxi' COMMENT '商品表名',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `goodid` bigint(20) NOT NULL COMMENT '商品id',
  `goodname` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '图片',
  `buynumber` int(11) NOT NULL COMMENT '购买数量',
  `price` float DEFAULT NULL COMMENT '单价',
  `discountprice` float DEFAULT NULL COMMENT '会员价',
  `goodtype` varchar(200) DEFAULT NULL COMMENT '商品类型',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_cart' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_cart` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_cart'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_cart' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_cart` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_cart'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 购物车无默认数据

-- ============================================================
-- 7. bt_order (订单表) ← orders
-- 注意：banquet 库已有 orders 表（宴会订单），此为点餐订单，独立表
-- 多租户：UNIQUE KEY 改为 (store_id, orderid)
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `orderid` varchar(200) NOT NULL COMMENT '订单编号',
  `tablename` varchar(200) DEFAULT 'caipinxinxi' COMMENT '商品表名',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `goodid` bigint(20) NOT NULL COMMENT '商品id',
  `goodname` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '商品图片',
  `buynumber` int(11) NOT NULL COMMENT '购买数量',
  `price` float NOT NULL DEFAULT '0' COMMENT '价格',
  `discountprice` float DEFAULT '0' COMMENT '折扣价格',
  `total` float NOT NULL DEFAULT '0' COMMENT '总价格',
  `discounttotal` float DEFAULT '0' COMMENT '折扣总价格',
  `type` int(11) DEFAULT '1' COMMENT '支付类型',
  `status` varchar(200) DEFAULT NULL COMMENT '状态',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `tel` varchar(200) DEFAULT NULL COMMENT '电话',
  `consignee` varchar(200) DEFAULT NULL COMMENT '收货人',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `logistics` longtext COMMENT '物流',
  `goodtype` varchar(200) DEFAULT NULL COMMENT '商品类型',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_orderid` (`store_id`, `orderid`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点餐订单';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_order' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_order` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_order'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_order' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_order` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_order'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 订单无默认数据

-- ============================================================
-- 8. bt_dish_review (菜品评论表) ← discusscaipinxinxi
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_dish_review` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `refid` bigint(20) NOT NULL COMMENT '关联表id',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `avatarurl` longtext COMMENT '头像',
  `nickname` varchar(200) DEFAULT NULL COMMENT '用户名',
  `content` longtext NOT NULL COMMENT '评论内容',
  `reply` longtext COMMENT '回复内容',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_refid` (`refid`),
  KEY `idx_userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品信息评论表';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_review' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_dish_review` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_dish_review'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_dish_review' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_dish_review` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_dish_review'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 评论无默认数据

-- ============================================================
-- 9. bt_storeup (收藏表) ← storeup
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_storeup` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `refid` bigint(20) DEFAULT NULL COMMENT '商品id',
  `tablename` varchar(200) DEFAULT NULL COMMENT '表名',
  `name` varchar(200) NOT NULL COMMENT '名称',
  `picture` longtext NOT NULL COMMENT '图片',
  `type` varchar(200) DEFAULT '1' COMMENT '类型(1:收藏,21:赞,22:踩,31:竞拍参与,41:关注)',
  `inteltype` varchar(200) DEFAULT NULL COMMENT '推荐类型',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_refid` (`refid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_storeup' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_storeup` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_storeup'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_storeup' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_storeup` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_storeup'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 收藏无默认数据

-- ============================================================
-- 10. bt_news (餐厅资讯表) ← news
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_news` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `introduction` longtext COMMENT '简介',
  `picture` longtext NOT NULL COMMENT '图片',
  `content` longtext NOT NULL COMMENT '内容',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐厅资讯';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_news' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_news` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_news'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_news' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_news` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_news'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`, `store_id`) VALUES
(101, '2024-04-08 06:12:11', '有梦想，就要努力去实现', '不管你想要怎样的生活，你都要去努力争取，不多尝试一些事情怎么知道自己适合什么、不适合什么呢?', 'upload/news_picture1.jpg', '<p>不管你想要怎样的生活，你都要去努力争取，不多尝试一些事情怎么知道自己适合什么、不适合什么呢?</p>', 1),
(102, '2024-04-08 06:12:11', '又是一年毕业季', '又是一年毕业季，感慨万千，还记的自己刚进学校那时候的情景...', 'upload/news_picture2.jpg', '<p>又是一年毕业季，感慨万千...</p>', 1),
(103, '2024-04-08 06:12:11', '挫折路上，坚持常在心间', '回头看看，你会不会发现，曾经的你在这里摔倒过...', 'upload/news_picture3.jpg', '<p>回头看看，你会不会发现...</p>', 1),
(104, '2024-04-08 06:12:11', '挫折是另一个生命的开端', '当遇到挫折或失败，你是看见失败还是看见机会?', 'upload/news_picture4.jpg', '<p>当遇到挫折或失败...</p>', 1),
(105, '2024-04-08 06:12:11', '你要去相信，没有到不了的明天', '有梦想就去努力，因为在这一辈子里面，现在不去勇敢的努力，也许就再也没有机会了。', 'upload/news_picture5.jpg', '<p>有梦想就去努力...</p>', 1),
(106, '2024-04-08 06:12:11', '离开是一种痛苦，是一种勇气，但同样也是一个考验，是一个新的开端', '无穷无尽是离愁，天涯海角遍寻思。', 'upload/news_picture6.jpg', '<p>无穷无尽是离愁...</p>', 1),
(107, '2024-04-08 06:12:11', 'Leave未必是一种痛苦', '无穷无尽是离愁，天涯海角遍寻思。当离别在即之时...', 'upload/news_picture7.jpg', '<p>无穷无尽是离愁...</p>', 1),
(108, '2024-04-08 06:12:11', '坚持才会成功', '回头看看，你会不会发现，曾经的你在这里摔倒过...', 'upload/news_picture8.jpg', '<p>回头看看...</p>', 1);

-- ============================================================
-- 11. bt_config (配置表) ← config
-- 注意：banquet 库已有 config 表（键值对配置），此为点餐系统配置，独立表
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '配置参数名称',
  `value` varchar(100) DEFAULT NULL COMMENT '配置参数值',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置文件';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_config' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_config` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_config'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_config' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_config` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_config'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_config` (`id`, `name`, `value`, `store_id`) VALUES
(1, 'picture1', 'upload/picture1.jpg', 1),
(2, 'picture2', 'upload/picture2.jpg', 1),
(3, 'picture3', 'upload/picture3.jpg', 1);

-- ============================================================
-- 12. bt_user (管理员表) ← users
-- 注意：banquet 库已有 admin_users 和 users 表，此为点餐系统管理员，独立表
-- 多租户：UNIQUE KEY 改为 (store_id, username)
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `role` varchar(100) DEFAULT '管理员' COMMENT '角色',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_username` (`store_id`, `username`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_user' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_user` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_user'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_user' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_user` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_user'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_user` (`id`, `username`, `password`, `role`, `addtime`, `store_id`) VALUES
(1, 'admin', 'admin', '管理员', '2024-04-08 06:12:11', 1);

-- ============================================================
-- 13. bt_yonghu (用户表) ← yonghu
-- 多租户：UNIQUE KEY 改为 (store_id, yonghuming)
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_yonghu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yonghuming` varchar(200) NOT NULL COMMENT '用户名',
  `mima` varchar(200) NOT NULL COMMENT '密码',
  `xingming` varchar(200) NOT NULL COMMENT '姓名',
  `touxiang` longtext COMMENT '头像',
  `xingbie` varchar(200) DEFAULT NULL COMMENT '性别',
  `youxiang` varchar(200) DEFAULT NULL COMMENT '邮箱',
  `shouji` varchar(200) DEFAULT NULL COMMENT '手机',
  `money` float DEFAULT '0' COMMENT '余额',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_yonghuming` (`store_id`, `yonghuming`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_yonghu' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_yonghu` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_yonghu'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_yonghu' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_yonghu` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_yonghu'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `bt_yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `youxiang`, `shouji`, `money`, `store_id`) VALUES
(11, '2024-04-08 06:12:11', '用户名1', '123456', '姓名1', 'upload/yonghu_touxiang1.jpg', '男', '773890001@qq.com', '13823888881', 200, 1),
(12, '2024-04-08 06:12:11', '用户名2', '123456', '姓名2', 'upload/yonghu_touxiang2.jpg', '男', '773890002@qq.com', '13823888882', 200, 1),
(13, '2024-04-08 06:12:11', '用户名3', '123456', '姓名3', 'upload/yonghu_touxiang3.jpg', '男', '773890003@qq.com', '13823888883', 200, 1),
(14, '2024-04-08 06:12:11', '用户名4', '123456', '姓名4', 'upload/yonghu_touxiang4.jpg', '男', '773890004@qq.com', '13823888884', 200, 1),
(15, '2024-04-08 06:12:11', '用户名5', '123456', '姓名5', 'upload/yonghu_touxiang5.jpg', '男', '773890005@qq.com', '13823888885', 200, 1),
(16, '2024-04-08 06:12:11', '用户名6', '123456', '姓名6', 'upload/yonghu_touxiang6.jpg', '男', '773890006@qq.com', '13823888886', 200, 1),
(17, '2024-04-08 06:12:11', '用户名7', '123456', '姓名7', 'upload/yonghu_touxiang7.jpg', '男', '773890007@qq.com', '13823888887', 200, 1),
(18, '2024-04-08 06:12:11', '用户名8', '123456', '姓名8', 'upload/yonghu_touxiang8.jpg', '男', '773890008@qq.com', '13823888888', 200, 1);

-- ============================================================
-- 14. bt_token (令牌表) ← token
-- ============================================================
CREATE TABLE IF NOT EXISTS `bt_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `tablename` varchar(100) DEFAULT NULL COMMENT '表名',
  `role` varchar(100) DEFAULT NULL COMMENT '角色',
  `token` varchar(200) NOT NULL COMMENT '令牌',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `expiratedtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '多租户门店ID',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='token表';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_token' AND COLUMN_NAME = 'store_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `bt_token` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1 COMMENT ''多租户门店ID''', 'SELECT ''[SKIP] store_id exists in bt_token'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bt_token' AND INDEX_NAME = 'idx_store_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `bt_token` ADD INDEX `idx_store_id` (`store_id`)', 'SELECT ''[SKIP] idx_store_id exists in bt_token'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 令牌无默认数据

-- ============================================================
-- 迁移完成
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;