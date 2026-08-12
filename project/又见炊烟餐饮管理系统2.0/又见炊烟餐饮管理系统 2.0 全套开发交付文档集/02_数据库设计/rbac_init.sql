-- rbac_init.sql RBAC权限体系建表脚本 | 又见炊烟餐饮管理系统V2.0 | 生成日期：2026-08-02
--
-- 说明：RBAC 权限体系核心建表脚本（7 张表）
--   用户表 sys_user / 角色表 sys_role / 权限表 sys_permission / 菜单表 sys_menu
--   用户-角色关联 sys_user_role / 角色-权限关联 sys_role_permission / 角色-菜单关联 sys_role_menu
-- 数据库：MySQL 8.0 / 库名 banquet / 字符集 utf8mb4 / 引擎 InnoDB
-- 多租户：store_id（0=全局，1=宁国店，2=宣城店）；data_scope（all=跨店 / store=本店）
-- 初始化数据见同目录《系统初始化基础数据SQL.sql》

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============ 用户表 ============

CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id`       bigint        NOT NULL AUTO_INCREMENT             COMMENT '用户ID',
  `username`      varchar(50)   NOT NULL                            COMMENT '登录账号（唯一）',
  `password_hash` varchar(255)  NOT NULL                            COMMENT '密码哈希（BCrypt）',
  `real_name`     varchar(50)            DEFAULT NULL                COMMENT '真实姓名',
  `phone`         varchar(20)            DEFAULT NULL                COMMENT '手机号',
  `store_id`      bigint        NOT NULL DEFAULT 0                   COMMENT '所属门店ID：0=全局，1=宁国店，2=宣城店',
  `staff_id`      bigint                 DEFAULT NULL                COMMENT '关联员工ID（staff_master.staff_id）',
  `status`        tinyint       NOT NULL DEFAULT 1                   COMMENT '状态：1=启用 0=禁用',
  `last_login_at` datetime               DEFAULT NULL                COMMENT '最后登录时间',
  `last_login_ip` varchar(50)            DEFAULT NULL                COMMENT '最后登录IP',
  `create_time`   timestamp     NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
  `update_time`   timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_store` (`store_id`),
  KEY `idx_user_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============ 角色表 ============

CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id`     bigint        NOT NULL AUTO_INCREMENT               COMMENT '角色ID',
  `role_code`   varchar(50)   NOT NULL                              COMMENT '角色编码（唯一）',
  `role_name`   varchar(50)   NOT NULL                              COMMENT '角色名称',
  `store_id`    bigint        NOT NULL DEFAULT 0                    COMMENT '所属门店ID：0=全局，1=总店，2=分店',
  `data_scope`  varchar(20)   NOT NULL DEFAULT 'store'              COMMENT '数据范围：all=全门店 / store=本店',
  `description` varchar(200)           DEFAULT NULL                  COMMENT '角色描述',
  `status`      tinyint       NOT NULL DEFAULT 1                    COMMENT '状态：1=启用 0=禁用',
  `sort_order`  int           NOT NULL DEFAULT 0                    COMMENT '排序',
  `create_time` timestamp     NULL DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
  `update_time` timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ============ 权限表 ============

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `permission_id` bigint        NOT NULL AUTO_INCREMENT             COMMENT '权限ID',
  `perm_code`     varchar(100)  NOT NULL                            COMMENT '权限编码（唯一，如 booking:list）',
  `name`          varchar(100)  NOT NULL                            COMMENT '权限名称',
  `parent_id`     bigint        NOT NULL DEFAULT 0                  COMMENT '父权限ID：0=顶级',
  `url`           varchar(200)           DEFAULT NULL                COMMENT '接口URL匹配模式，如 /api/bookings/**',
  `method`        varchar(10)            DEFAULT NULL                COMMENT 'HTTP方法：GET/POST/PUT/DELETE/*',
  `perm_type`     varchar(20)   NOT NULL DEFAULT 'api'              COMMENT '权限类型：api=接口 / menu=菜单 / button=按钮',
  `description`   varchar(200)           DEFAULT NULL                COMMENT '权限描述',
  `status`        tinyint       NOT NULL DEFAULT 1                  COMMENT '状态：1=启用 0=禁用',
  `create_time`   timestamp     NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
  `update_time`   timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_perm_parent` (`parent_id`),
  KEY `idx_perm_url` (`url`),
  KEY `idx_perm_method` (`method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- ============ 菜单表 ============

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id`      bigint        NOT NULL AUTO_INCREMENT              COMMENT '菜单ID',
  `parent_id`    bigint        NOT NULL DEFAULT 0                   COMMENT '父菜单ID：0=顶级',
  `menu_name`    varchar(50)   NOT NULL                             COMMENT '菜单名称',
  `menu_path`    varchar(200)           DEFAULT NULL                 COMMENT '前端路由路径，如 /dashboard/bookings',
  `menu_icon`    varchar(100)           DEFAULT NULL                 COMMENT '菜单图标',
  `perm_code`    varchar(100)           DEFAULT NULL                 COMMENT '关联权限编码（sys_permission.perm_code）',
  `menu_type`    varchar(20)   NOT NULL DEFAULT 'menu'              COMMENT '类型：directory=目录 / menu=菜单 / button=按钮',
  `store_scope`  varchar(20)   NOT NULL DEFAULT 'all'               COMMENT '门店范围：all=全门店 / hq=仅总店 / branch=仅分店',
  `sort_order`   int           NOT NULL DEFAULT 0                   COMMENT '排序值',
  `visible`      tinyint       NOT NULL DEFAULT 1                   COMMENT '是否可见：1=可见 0=隐藏',
  `status`       tinyint       NOT NULL DEFAULT 1                   COMMENT '状态：1=启用 0=禁用',
  `create_time`  timestamp     NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
  `update_time`  timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`),
  KEY `idx_menu_perm`   (`perm_code`),
  KEY `idx_menu_sort`   (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ============ 用户-角色关联表 ============

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id`          bigint    NOT NULL AUTO_INCREMENT                    COMMENT '主键',
  `user_id`     bigint    NOT NULL                                   COMMENT '用户ID（sys_user.user_id）',
  `role_id`     bigint    NOT NULL                                   COMMENT '角色ID（sys_role.role_id）',
  `store_id`    bigint    NOT NULL DEFAULT 0                         COMMENT '冗余门店ID，便于按门店筛选',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_ur_user`  (`user_id`),
  KEY `idx_ur_role`  (`role_id`),
  KEY `idx_ur_store` (`store_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============ 角色-权限关联表 ============

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id`            bigint    NOT NULL AUTO_INCREMENT                  COMMENT '主键',
  `role_id`       bigint    NOT NULL                                 COMMENT '角色ID（sys_role.role_id）',
  `permission_id` bigint    NOT NULL                                 COMMENT '权限ID（sys_permission.permission_id）',
  `create_time`   timestamp NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_rp_role` (`role_id`),
  KEY `idx_rp_perm` (`permission_id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`permission_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- ============ 角色-菜单关联表 ============

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id`          bigint    NOT NULL AUTO_INCREMENT                    COMMENT '主键',
  `role_id`     bigint    NOT NULL                                   COMMENT '角色ID（sys_role.role_id）',
  `menu_id`     bigint    NOT NULL                                   COMMENT '菜单ID（sys_menu.menu_id）',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_rm_role` (`role_id`),
  KEY `idx_rm_menu` (`menu_id`),
  CONSTRAINT `fk_rm_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rm_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`menu_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

SET FOREIGN_KEY_CHECKS = 1;
