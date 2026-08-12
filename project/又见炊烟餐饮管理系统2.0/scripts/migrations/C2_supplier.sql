-- =====================================================
-- 模块C2：供应商管理
-- 表：gongyingshang
-- =====================================================

CREATE TABLE IF NOT EXISTS `gongyingshang` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gongyingshangzhanghao` varchar(200) NOT NULL COMMENT '供应商账号',
  `mima` varchar(200) NOT NULL COMMENT '密码',
  `gongyingshangmingcheng` varchar(200) NOT NULL COMMENT '供应商名称',
  `tupian` longtext COMMENT '图片',
  `lianxiren` varchar(200) DEFAULT NULL COMMENT '联系人',
  `lianxidianhua` varchar(200) DEFAULT NULL COMMENT '联系电话',
  `gongyingshangdizhi` varchar(200) DEFAULT NULL COMMENT '供应商地址',
  `money` float DEFAULT '0' COMMENT '余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `gongyingshangzhanghao` (`gongyingshangzhanghao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='供应商';