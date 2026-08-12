-- ============================================================
-- 模块B2：桌台管理（canzhuoxinxi + canzhuoshiyong）
-- 来源：餐厅点餐系统 springboot3258n
-- ============================================================

-- 餐桌信息表
DROP TABLE IF EXISTS `canzhuoxinxi`;
CREATE TABLE `canzhuoxinxi` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `canzhuohaoma` varchar(200) NOT NULL COMMENT '餐桌号码',
  `tupian` longtext COMMENT '图片',
  `kezuorenshu` int(11) DEFAULT NULL COMMENT '可坐人数',
  `canzhuoweizhi` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `canzhuozhuangtai` varchar(200) DEFAULT NULL COMMENT '餐桌状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `canzhuohaoma` (`canzhuohaoma`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8 COMMENT='餐桌信息';

-- 餐桌信息初始数据
INSERT INTO `canzhuoxinxi` VALUES (21,'2024-04-08 06:12:11','餐桌号码1','upload/canzhuoxinxi_tupian1.jpg,upload/canzhuoxinxi_tupian2.jpg,upload/canzhuoxinxi_tupian3.jpg',1,'餐桌位置1','使用中'),(22,'2024-04-08 06:12:11','餐桌号码2','upload/canzhuoxinxi_tupian2.jpg,upload/canzhuoxinxi_tupian3.jpg,upload/canzhuoxinxi_tupian4.jpg',2,'餐桌位置2','使用中'),(23,'2024-04-08 06:12:11','餐桌号码3','upload/canzhuoxinxi_tupian3.jpg,upload/canzhuoxinxi_tupian4.jpg,upload/canzhuoxinxi_tupian5.jpg',3,'餐桌位置3','使用中'),(24,'2024-04-08 06:12:11','餐桌号码4','upload/canzhuoxinxi_tupian4.jpg,upload/canzhuoxinxi_tupian5.jpg,upload/canzhuoxinxi_tupian6.jpg',4,'餐桌位置4','使用中'),(25,'2024-04-08 06:12:11','餐桌号码5','upload/canzhuoxinxi_tupian5.jpg,upload/canzhuoxinxi_tupian6.jpg,upload/canzhuoxinxi_tupian7.jpg',5,'餐桌位置5','使用中'),(26,'2024-04-08 06:12:11','餐桌号码6','upload/canzhuoxinxi_tupian6.jpg,upload/canzhuoxinxi_tupian7.jpg,upload/canzhuoxinxi_tupian8.jpg',6,'餐桌位置6','使用中'),(27,'2024-04-08 06:12:11','餐桌号码7','upload/canzhuoxinxi_tupian7.jpg,upload/canzhuoxinxi_tupian8.jpg,upload/canzhuoxinxi_tupian9.jpg',7,'餐桌位置7','使用中'),(28,'2024-04-08 06:12:11','餐桌号码8','upload/canzhuoxinxi_tupian8.jpg,upload/canzhuoxinxi_tupian9.jpg,upload/canzhuoxinxi_tupian10.jpg',8,'餐桌位置8','使用中');

-- 餐桌使用表
DROP TABLE IF EXISTS `canzhuoshiyong`;
CREATE TABLE `canzhuoshiyong` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `canzhuohaoma` varchar(200) DEFAULT NULL COMMENT '餐桌号码',
  `canzhuoweizhi` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `kezuorenshu` int(11) DEFAULT NULL COMMENT '可坐人数',
  `shiyongshijian` datetime DEFAULT NULL COMMENT '使用时间',
  `yonghuming` varchar(200) DEFAULT NULL COMMENT '用户名',
  `xingming` varchar(200) DEFAULT NULL COMMENT '姓名',
  `shouji` varchar(200) DEFAULT NULL COMMENT '手机',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8 COMMENT='餐桌使用';

-- 餐桌使用初始数据
INSERT INTO `canzhuoshiyong` VALUES (31,'2024-04-08 06:12:11','餐桌号码1','餐桌位置1',1,'2024-04-08 14:12:11','用户名1','姓名1','13823888881'),(32,'2024-04-08 06:12:11','餐桌号码2','餐桌位置2',2,'2024-04-08 14:12:11','用户名2','姓名2','13823888882'),(33,'2024-04-08 06:12:11','餐桌号码3','餐桌位置3',3,'2024-04-08 14:12:11','用户名3','姓名3','13823888883'),(34,'2024-04-08 06:12:11','餐桌号码4','餐桌位置4',4,'2024-04-08 14:12:11','用户名4','姓名4','13823888884'),(35,'2024-04-08 06:12:11','餐桌号码5','餐桌位置5',5,'2024-04-08 14:12:11','用户名5','姓名5','13823888885'),(36,'2024-04-08 06:12:11','餐桌号码6','餐桌位置6',6,'2024-04-08 14:12:11','用户名6','姓名6','13823888886'),(37,'2024-04-08 06:12:11','餐桌号码7','餐桌位置7',7,'2024-04-08 14:12:11','用户名7','姓名7','13823888887'),(38,'2024-04-08 06:12:11','餐桌号码8','餐桌位置8',8,'2024-04-08 14:12:11','用户名8','姓名8','13823888888');