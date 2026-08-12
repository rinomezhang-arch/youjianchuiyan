-- =====================================================
-- 模块C1：采购入库 + 材料管理
-- 表：caigouruku, cailiaoxinxi, cailiaozhonglei
-- =====================================================

-- ----------------------------
-- Table structure for caigouruku
-- ----------------------------
CREATE TABLE IF NOT EXISTS `caigouruku` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaomingcheng` varchar(200) DEFAULT NULL COMMENT '材料名称',
  `cailiaozhonglei` varchar(200) DEFAULT NULL COMMENT '材料种类',
  `cailiaoguige` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `alllimittimes` int(11) NOT NULL COMMENT '库存',
  `rukushijian` datetime DEFAULT NULL COMMENT '入库时间',
  `beizhu` varchar(200) DEFAULT NULL COMMENT '备注',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `gongyingshangmingcheng` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='采购入库';

-- ----------------------------
-- Table structure for cailiaoxinxi
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cailiaoxinxi` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaomingcheng` varchar(200) NOT NULL COMMENT '材料名称',
  `tupian` longtext COMMENT '图片',
  `cailiaozhonglei` varchar(200) NOT NULL COMMENT '材料种类',
  `cailiaoguige` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `cailiaoxiangqing` longtext COMMENT '材料详情',
  `gongyingshangzhanghao` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `gongyingshangmingcheng` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  `onelimittimes` int(11) DEFAULT NULL COMMENT '单限',
  `alllimittimes` int(11) DEFAULT NULL COMMENT '库存',
  `clicktime` datetime DEFAULT NULL COMMENT '最近点击时间',
  `price` float NOT NULL COMMENT '价格',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='材料信息';

-- ----------------------------
-- Table structure for cailiaozhonglei
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cailiaozhonglei` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cailiaozhonglei` varchar(200) DEFAULT NULL COMMENT '材料种类',
  PRIMARY KEY (`id`),
  UNIQUE KEY `cailiaozhonglei` (`cailiaozhonglei`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='材料种类';