-- ============================================================
-- 模块B1：菜品管理（caipinleixing + caipinxinxi）
-- 来源：餐厅点餐系统 springboot3258n
-- ============================================================

-- 菜品类型表
DROP TABLE IF EXISTS `caipinleixing`;
CREATE TABLE `caipinleixing` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `caipinleixing` varchar(200) NOT NULL COMMENT '菜品类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8 COMMENT='菜品类型';

-- 菜品类型初始数据
INSERT INTO `caipinleixing` VALUES (41,'2024-04-08 06:12:11','菜品类型1'),(42,'2024-04-08 06:12:11','菜品类型2'),(43,'2024-04-08 06:12:11','菜品类型3'),(44,'2024-04-08 06:12:11','菜品类型4'),(45,'2024-04-08 06:12:11','菜品类型5'),(46,'2024-04-08 06:12:11','菜品类型6'),(47,'2024-04-08 06:12:11','菜品类型7'),(48,'2024-04-08 06:12:11','菜品类型8');

-- 菜品信息表
DROP TABLE IF EXISTS `caipinxinxi`;
CREATE TABLE `caipinxinxi` (
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8 COMMENT='菜品信息';

-- 菜品信息初始数据
INSERT INTO `caipinxinxi` VALUES (51,'2024-04-08 06:12:11','菜品名称1','菜品类型1','upload/caipinxinxi_tupian1.jpg,upload/caipinxinxi_tupian2.jpg,upload/caipinxinxi_tupian3.jpg','口味1','预计时间1','菜品介绍1','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(52,'2024-04-08 06:12:11','菜品名称2','菜品类型2','upload/caipinxinxi_tupian2.jpg,upload/caipinxinxi_tupian3.jpg,upload/caipinxinxi_tupian4.jpg','口味2','预计时间2','菜品介绍2','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(53,'2024-04-08 06:12:11','菜品名称3','菜品类型3','upload/caipinxinxi_tupian3.jpg,upload/caipinxinxi_tupian4.jpg,upload/caipinxinxi_tupian5.jpg','口味3','预计时间3','菜品介绍3','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(54,'2024-04-08 06:12:11','菜品名称4','菜品类型4','upload/caipinxinxi_tupian4.jpg,upload/caipinxinxi_tupian5.jpg,upload/caipinxinxi_tupian6.jpg','口味4','预计时间4','菜品介绍4','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(55,'2024-04-08 06:12:11','菜品名称5','菜品类型5','upload/caipinxinxi_tupian5.jpg,upload/caipinxinxi_tupian6.jpg,upload/caipinxinxi_tupian7.jpg','口味5','预计时间5','菜品介绍5','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(56,'2024-04-08 06:12:11','菜品名称6','菜品类型6','upload/caipinxinxi_tupian6.jpg,upload/caipinxinxi_tupian7.jpg,upload/caipinxinxi_tupian8.jpg','口味6','预计时间6','菜品介绍6','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(57,'2024-04-08 06:12:11','菜品名称7','菜品类型7','upload/caipinxinxi_tupian7.jpg,upload/caipinxinxi_tupian8.jpg,upload/caipinxinxi_tupian9.jpg','口味7','预计时间7','菜品介绍7','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9),(58,'2024-04-08 06:12:11','菜品名称8','菜品类型8','upload/caipinxinxi_tupian8.jpg,upload/caipinxinxi_tupian9.jpg,upload/caipinxinxi_tupian10.jpg','口味8','预计时间8','菜品介绍8','2024-04-08 14:12:11','2024-04-08 14:12:11',99.9);