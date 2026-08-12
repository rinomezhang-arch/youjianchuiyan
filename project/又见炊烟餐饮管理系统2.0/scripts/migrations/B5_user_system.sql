-- ============================================================
-- 模块B5：用户系统（users + yonghu + token + config）
-- 来源：餐厅点餐系统 springboot3258n
-- ============================================================

-- 用户表（管理员）
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `role` varchar(100) DEFAULT '管理员' COMMENT '角色',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='用户表';

-- 管理员初始数据
INSERT INTO `users` VALUES (1,'admin','admin','管理员','2024-04-08 06:12:11');

-- 用户表（普通用户/会员）
DROP TABLE IF EXISTS `yonghu`;
CREATE TABLE `yonghu` (
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `yonghuming` (`yonghuming`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8 COMMENT='用户';

-- 普通用户初始数据
INSERT INTO `yonghu` VALUES (11,'2024-04-08 06:12:11','用户名1','123456','姓名1','upload/yonghu_touxiang1.jpg','男','773890001@qq.com','13823888881',200),(12,'2024-04-08 06:12:11','用户名2','123456','姓名2','upload/yonghu_touxiang2.jpg','男','773890002@qq.com','13823888882',200),(13,'2024-04-08 06:12:11','用户名3','123456','姓名3','upload/yonghu_touxiang3.jpg','男','773890003@qq.com','13823888883',200),(14,'2024-04-08 06:12:11','用户名4','123456','姓名4','upload/yonghu_touxiang4.jpg','男','773890004@qq.com','13823888884',200),(15,'2024-04-08 06:12:11','用户名5','123456','姓名5','upload/yonghu_touxiang5.jpg','男','773890005@qq.com','13823888885',200),(16,'2024-04-08 06:12:11','用户名6','123456','姓名6','upload/yonghu_touxiang6.jpg','男','773890006@qq.com','13823888886',200),(17,'2024-04-08 06:12:11','用户名7','123456','姓名7','upload/yonghu_touxiang7.jpg','男','773890007@qq.com','13823888887',200),(18,'2024-04-08 06:12:11','用户名8','123456','姓名8','upload/yonghu_touxiang8.jpg','男','773890008@qq.com','13823888888',200);

-- token表
DROP TABLE IF EXISTS `token`;
CREATE TABLE `token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `tablename` varchar(100) DEFAULT NULL COMMENT '表名',
  `role` varchar(100) DEFAULT NULL COMMENT '角色',
  `token` varchar(200) NOT NULL COMMENT '密码',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `expiratedtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='token表';

-- 配置文件表
DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '配置参数名称',
  `value` varchar(100) DEFAULT NULL COMMENT '配置参数值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COMMENT='配置文件';

-- 配置初始数据
INSERT INTO `config` VALUES (1,'picture1','upload/picture1.jpg'),(2,'picture2','upload/picture2.jpg'),(3,'picture3','upload/picture3.jpg');