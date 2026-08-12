-- ============================================================
-- 模块B4：评论收藏资讯（discusscaipinxinxi + storeup + news）
-- 来源：餐厅点餐系统 springboot3258n
-- ============================================================

-- 菜品信息评论表
DROP TABLE IF EXISTS `discusscaipinxinxi`;
CREATE TABLE `discusscaipinxinxi` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `refid` bigint(20) NOT NULL COMMENT '关联表id',
  `userid` bigint(20) NOT NULL COMMENT '用户id',
  `avatarurl` longtext COMMENT '头像',
  `nickname` varchar(200) DEFAULT NULL COMMENT '用户名',
  `content` longtext NOT NULL COMMENT '评论内容',
  `reply` longtext COMMENT '回复内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='菜品信息评论表';

-- 收藏表
DROP TABLE IF EXISTS `storeup`;
CREATE TABLE `storeup` (
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='收藏表';

-- 餐厅资讯表
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `introduction` longtext COMMENT '简介',
  `picture` longtext NOT NULL COMMENT '图片',
  `content` longtext NOT NULL COMMENT '内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8 COMMENT='餐厅资讯';

-- 资讯初始数据
INSERT INTO `news` VALUES (101,'2024-04-08 06:12:11','有梦想，就要努力去实现','不管你想要怎样的生活，你都要去努力争取，不多尝试一些事情怎么知道自己适合什么、不适合什么呢?','upload/news_picture1.jpg','<p>不管你想要怎样的生活，你都要去努力争取，不多尝试一些事情怎么知道自己适合什么、不适合什么呢?</p>'),(102,'2024-04-08 06:12:11','又是一年毕业季','又是一年毕业季，感慨万千，还记的自己刚进学校那时候的情景','upload/news_picture2.jpg','<p>又是一年毕业季，感慨万千</p>'),(103,'2024-04-08 06:12:11','挫折路上，坚持常在心间','回头看看，你会不会发现，曾经的你在这里摔倒过','upload/news_picture3.jpg','<p>回头看看，你会不会发现，曾经的你在这里摔倒过</p>'),(104,'2024-04-08 06:12:11','挫折是另一个生命的开端','当遇到挫折或失败，你是看见失败还是看见机会?','upload/news_picture4.jpg','<p>当遇到挫折或失败，你是看见失败还是看见机会?</p>'),(105,'2024-04-08 06:12:11','你要去相信，没有到不了的明天','有梦想就去努力，因为在这一辈子里面，现在不去勇敢的努力，也许就再也没有机会了。','upload/news_picture5.jpg','<p>有梦想就去努力</p>'),(106,'2024-04-08 06:12:11','离开是一种痛苦，是一种勇气，但同样也是一个考验，是一个新的开端','无穷无尽是离愁，天涯海角遍寻思','upload/news_picture6.jpg','<p>无穷无尽是离愁</p>'),(107,'2024-04-08 06:12:11','Leave未必是一种痛苦','无穷无尽是离愁','upload/news_picture7.jpg','<p>无穷无尽是离愁</p>'),(108,'2024-04-08 06:12:11','坚持才会成功','回头看看，你会不会发现，曾经的你在这里摔倒过','upload/news_picture8.jpg','<p>坚持才会成功</p>');