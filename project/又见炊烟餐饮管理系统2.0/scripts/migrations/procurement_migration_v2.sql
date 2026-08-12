-- ======================================================================
-- 采购管理系统完整迁移脚本 (procurement_migration_v2.sql)
-- 
-- 说明：
--   本脚本为采购管理系统创建 14 张核心业务表（pr_ 前缀），
--   所有表均包含 store_id 字段用于多租户隔离。
--   
--   对照关系（原始表 → 新表）：
--   1.  address            → pr_address           (地址表)
--   2.  caigouruku         → pr_purchase_in       (采购入库表)
--   3.  cailiaoxinxi       → pr_material_info     (材料信息表)
--   4.  cailiaozhonglei    → pr_material_type     (材料种类表)
--   5.  cart               → pr_cart              (购物车表)
--   6.  config             → pr_config            (配置表)
--   7.  discusscailiaoxinxi→ pr_material_review   (材料评论表)
--   8.  gongyingshang      → pr_supplier          (供应商表)
--   9.  news               → pr_news              (公告信息表)
--   10. orders             → pr_order             (订单表)
--   11. storeup            → pr_storeup           (收藏表)
--   12. token              → pr_token             (令牌表)
--   13. users              → pr_user              (管理员表)
--   14. yonghu             → pr_yonghu            (用户表)
--   
-- 使用方式：
--   仅在以下场景执行：
--     1. 首次部署采购管理模块
--     2. 重置采购模块数据（会清空全部 pr_ 表）
--   重复执行安全（DROP TABLE IF EXISTS + CREATE TABLE）
-- ======================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ======================================================================
-- 1. pr_address — 地址表
-- ======================================================================
DROP TABLE IF EXISTS `pr_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `address` varchar(200) NOT NULL COMMENT '地址',
  `name` varchar(200) NOT NULL COMMENT '收货人',
  `phone` varchar(200) NOT NULL COMMENT '电话',
  `isdefault` varchar(200) NOT NULL DEFAULT '否' COMMENT '是否默认地址[是/否]',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地址表';

-- ======================================================================
-- 2. pr_purchase_in — 采购入库表
-- ======================================================================
DROP TABLE IF EXISTS `pr_purchase_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_purchase_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaomingcheng` varchar(200) DEFAULT NULL COMMENT '材料名称',
  `cailiaozhonglei` varchar(200) DEFAULT NULL COMMENT '材料种类',
  `cailiaoguige` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `alllimittimes` int NOT NULL DEFAULT 0 COMMENT '库存',
  `rukushijian` datetime DEFAULT NULL COMMENT '入库时间',
  `beizhu` varchar(500) DEFAULT NULL COMMENT '备注',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `gongyingshangmingcheng` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_gongyingshangzhanghao` (`gongyingshangzhanghao`),
  KEY `idx_cailiaomingcheng` (`cailiaomingcheng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库表';

-- ======================================================================
-- 3. pr_material_info — 材料信息表
-- ======================================================================
DROP TABLE IF EXISTS `pr_material_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_material_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaomingcheng` varchar(200) NOT NULL COMMENT '材料名称',
  `tupian` longtext COMMENT '图片',
  `cailiaozhonglei` varchar(200) NOT NULL COMMENT '材料种类',
  `cailiaoguige` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `cailiaoxiangqing` longtext COMMENT '材料详情',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `gongyingshangmingcheng` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  `onelimittimes` int DEFAULT NULL COMMENT '单限',
  `alllimittimes` int DEFAULT NULL COMMENT '库存',
  `clicktime` datetime DEFAULT NULL COMMENT '最近点击时间',
  `price` float NOT NULL DEFAULT 0 COMMENT '价格',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_cailiaomingcheng` (`cailiaomingcheng`),
  KEY `idx_cailiaozhonglei` (`cailiaozhonglei`),
  KEY `idx_gongyingshangzhanghao` (`gongyingshangzhanghao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料信息表';

-- ======================================================================
-- 4. pr_material_type — 材料种类表
-- ======================================================================
DROP TABLE IF EXISTS `pr_material_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_material_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaozhonglei` varchar(200) NOT NULL COMMENT '材料种类',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_cailiaozhonglei` (`store_id`, `cailiaozhonglei`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料种类表';

-- ======================================================================
-- 5. pr_cart — 购物车表
-- ======================================================================
DROP TABLE IF EXISTS `pr_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tablename` varchar(200) DEFAULT 'cailiaoxinxi' COMMENT '商品表名',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `goodid` bigint NOT NULL COMMENT '商品ID',
  `goodname` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '图片',
  `buynumber` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `price` float DEFAULT NULL COMMENT '单价',
  `discountprice` float DEFAULT NULL COMMENT '会员价',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '商户名称',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_goodid` (`goodid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ======================================================================
-- 6. pr_config — 配置表
-- ======================================================================
DROP TABLE IF EXISTS `pr_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `name` varchar(100) NOT NULL COMMENT '配置参数名称',
  `value` varchar(500) DEFAULT NULL COMMENT '配置参数值',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置表';

-- ======================================================================
-- 7. pr_material_review — 材料评论表
-- ======================================================================
DROP TABLE IF EXISTS `pr_material_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_material_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `refid` bigint NOT NULL COMMENT '关联表ID',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `avatarurl` longtext COMMENT '头像',
  `nickname` varchar(200) DEFAULT NULL COMMENT '用户名',
  `content` longtext NOT NULL COMMENT '评论内容',
  `reply` longtext COMMENT '回复内容',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_refid` (`refid`),
  KEY `idx_userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料评论表';

-- ======================================================================
-- 8. pr_supplier — 供应商表
-- ======================================================================
DROP TABLE IF EXISTS `pr_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gongyingshangzhanghao` varchar(200) NOT NULL COMMENT '供应商账号',
  `mima` varchar(200) NOT NULL COMMENT '密码',
  `gongyingshangmingcheng` varchar(200) NOT NULL COMMENT '供应商名称',
  `tupian` longtext COMMENT '图片',
  `lianxiren` varchar(200) DEFAULT NULL COMMENT '联系人',
  `lianxidianhua` varchar(200) DEFAULT NULL COMMENT '联系电话',
  `gongyingshangdizhi` varchar(200) DEFAULT NULL COMMENT '供应商地址',
  `money` float DEFAULT 0 COMMENT '余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_gongyingshangzhanghao` (`store_id`, `gongyingshangzhanghao`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_gongyingshangmingcheng` (`gongyingshangmingcheng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- ======================================================================
-- 9. pr_news — 公告信息表
-- ======================================================================
DROP TABLE IF EXISTS `pr_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_news` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `introduction` longtext COMMENT '简介',
  `picture` longtext COMMENT '图片',
  `content` longtext COMMENT '内容',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告信息表';

-- ======================================================================
-- 10. pr_order — 订单表
-- ======================================================================
DROP TABLE IF EXISTS `pr_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `orderid` varchar(200) NOT NULL COMMENT '订单编号',
  `tablename` varchar(200) DEFAULT 'cailiaoxinxi' COMMENT '商品表名',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `goodid` bigint NOT NULL COMMENT '商品ID',
  `goodname` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '商品图片',
  `buynumber` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `price` float NOT NULL DEFAULT 0 COMMENT '价格',
  `discountprice` float DEFAULT 0 COMMENT '折扣价格',
  `total` float NOT NULL DEFAULT 0 COMMENT '总价格',
  `discounttotal` float DEFAULT 0 COMMENT '折扣总价格',
  `type` int DEFAULT 1 COMMENT '支付类型[1:在线支付]',
  `status` varchar(200) DEFAULT NULL COMMENT '状态',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `tel` varchar(200) DEFAULT NULL COMMENT '电话',
  `consignee` varchar(200) DEFAULT NULL COMMENT '收货人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `logistics` longtext COMMENT '物流',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '商户名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orderid` (`orderid`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_status` (`status`),
  KEY `idx_goodid` (`goodid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ======================================================================
-- 11. pr_storeup — 收藏表
-- ======================================================================
DROP TABLE IF EXISTS `pr_storeup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_storeup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `refid` bigint DEFAULT NULL COMMENT '商品ID',
  `tablename` varchar(200) DEFAULT NULL COMMENT '表名',
  `name` varchar(200) NOT NULL COMMENT '名称',
  `picture` longtext COMMENT '图片',
  `type` varchar(200) DEFAULT '1' COMMENT '类型(1:收藏,21:赞,22:踩,31:竞拍参与,41:关注)',
  `inteltype` varchar(200) DEFAULT NULL COMMENT '推荐类型',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_refid` (`refid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ======================================================================
-- 12. pr_token — 令牌表
-- ======================================================================
DROP TABLE IF EXISTS `pr_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `userid` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `tablename` varchar(100) DEFAULT NULL COMMENT '表名',
  `role` varchar(100) DEFAULT NULL COMMENT '角色',
  `token` varchar(200) NOT NULL COMMENT '令牌',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `expiratedtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_userid` (`userid`),
  KEY `idx_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='令牌表';

-- ======================================================================
-- 13. pr_user — 管理员表
-- ======================================================================
DROP TABLE IF EXISTS `pr_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `role` varchar(100) DEFAULT '管理员' COMMENT '角色',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ======================================================================
-- 14. pr_yonghu — 用户表
-- ======================================================================
DROP TABLE IF EXISTS `pr_yonghu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pr_yonghu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户)',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yonghuzhanghao` varchar(200) NOT NULL COMMENT '用户账号',
  `mima` varchar(200) NOT NULL COMMENT '密码',
  `yonghuxingming` varchar(200) NOT NULL COMMENT '用户姓名',
  `touxiang` longtext COMMENT '头像',
  `xingbie` varchar(200) DEFAULT NULL COMMENT '性别',
  `shoujihaoma` varchar(200) DEFAULT NULL COMMENT '手机号码',
  `money` float DEFAULT 0 COMMENT '余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_yonghuzhanghao` (`store_id`, `yonghuzhanghao`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ======================================================================
-- 默认数据插入
-- ======================================================================

-- ----------------------------
-- 默认管理员账号 (admin / admin)
-- ----------------------------
INSERT INTO `pr_user` (`store_id`, `username`, `password`, `role`, `addtime`) VALUES
(1, 'admin', 'admin', '管理员', NOW());

-- ----------------------------
-- 默认采购系统配置
-- ----------------------------
INSERT INTO `pr_config` (`store_id`, `name`, `value`) VALUES
(1, 'system_name', '采购管理系统'),
(1, 'system_version', 'v2.0'),
(1, 'contact_email', 'admin@example.com'),
(1, 'page_size', '10'),
(1, 'auto_confirm_days', '7');

-- ----------------------------
-- 默认材料种类
-- ----------------------------
INSERT INTO `pr_material_type` (`store_id`, `cailiaozhonglei`) VALUES
(1, '蔬菜'),
(1, '肉类'),
(1, '海鲜水产'),
(1, '调味品'),
(1, '粮油米面'),
(1, '酒水饮料'),
(1, '厨房用品'),
(1, '一次性用品');

-- ----------------------------
-- 默认供应商
-- ----------------------------
INSERT INTO `pr_supplier` (`store_id`, `gongyingshangzhanghao`, `mima`, `gongyingshangmingcheng`, `tupian`, `lianxiren`, `lianxidianhua`, `gongyingshangdizhi`, `money`) VALUES
(1, 'supplier01', '123456', '宁国市蔬菜批发市场', NULL, '王老板', '13900139001', '安徽省宁国市农贸市场A区01号', 0),
(1, 'supplier02', '123456', '东海海鲜直供', NULL, '李老板', '13900139002', '安徽省宁国市水产市场B区12号', 0),
(1, 'supplier03', '123456', '皖南土猪直供', NULL, '张老板', '13900139003', '安徽省宁国市畜牧市场C区08号', 0);

-- ----------------------------
-- 默认公告信息
-- ----------------------------
INSERT INTO `pr_news` (`store_id`, `title`, `introduction`, `picture`, `content`) VALUES
(1, '采购管理系统正式上线', '为提升采购效率，采购管理系统已正式上线运行。', NULL, '<p>为提升采购效率，规范采购流程，采购管理系统已正式上线运行。请各供应商及时登录系统完善信息。</p>'),
(1, '供应商注册须知', '欢迎各位供应商注册使用本采购系统。', NULL, '<p>欢迎各位供应商注册使用本采购系统。注册时请如实填写企业信息，上传相关资质证明文件。</p>');

-- ======================================================================
-- 完成
-- ======================================================================
SET FOREIGN_KEY_CHECKS = 1;