-- =====================================================================
-- 又见炊烟双门店餐饮管理系统 - RBAC 权限核心表初始化脚本
-- 5 张核心表：sys_role / sys_permission / sys_user_role / sys_role_permission / sys_menu
-- 门店规划：store_id=0 全局(总经理) / store_id=1 总店 / store_id=2 分店
-- 字符集：utf8mb4，引擎：InnoDB
-- =====================================================================

/*!50503 SET NAMES utf8mb4 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

-- ---------------------------------------------------------------------
-- 1. 角色表 sys_role
--    预设角色：超级总经理(store_id=0) / 总店员工(store_id=1) /
--              分店店长(store_id=2) / 分店服务员(store_id=2)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id`     bigint        NOT NULL AUTO_INCREMENT                 COMMENT '角色ID',
  `role_code`   varchar(50)   NOT NULL                                COMMENT '角色编码（唯一）',
  `role_name`   varchar(50)   NOT NULL                                COMMENT '角色名称',
  `store_id`    bigint        NOT NULL DEFAULT 0                      COMMENT '所属门店ID：0=全局，1=总店，2=分店',
  `data_scope`  varchar(20)   NOT NULL DEFAULT 'store'                COMMENT '数据范围：all=全门店 / store=本店',
  `description` varchar(200)           DEFAULT NULL                   COMMENT '角色描述',
  `status`      tinyint       NOT NULL DEFAULT 1                      COMMENT '状态：1=启用 0=禁用',
  `sort_order`  int           NOT NULL DEFAULT 0                      COMMENT '排序',
  `created_at`  timestamp     NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
  `updated_at`  timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

INSERT INTO `sys_role` (`role_id`,`role_code`,`role_name`,`store_id`,`data_scope`,`description`,`status`,`sort_order`) VALUES
  (1,'GM',            '超级总经理', 0, 'all',   '跨门店全局管理者，可查询全部门店数据', 1, 1),
  (2,'HQ_STAFF',      '总店员工',   1, 'store', '总店(门店1)普通员工，仅可见本店数据',   1, 2),
  (3,'STORE_MANAGER', '分店店长',   2, 'store', '分店(门店2)店长，管理本店经营与人员',   1, 3),
  (4,'WAITER',        '分店服务员', 2, 'store', '分店(门店2)服务员，仅本店基础业务',     1, 4)
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`);

-- ---------------------------------------------------------------------
-- 2. 接口权限点表 sys_permission
--    字段：url / method / permission_code / description
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `permission_id`   bigint        NOT NULL AUTO_INCREMENT             COMMENT '权限ID',
  `permission_code` varchar(100)  NOT NULL                            COMMENT '权限编码（唯一，如 booking:list）',
  `name`            varchar(100)  NOT NULL                            COMMENT '权限名称',
  `url`             varchar(200)           DEFAULT NULL               COMMENT '接口URL匹配模式，如 /api/bookings/**',
  `method`          varchar(10)            DEFAULT NULL               COMMENT 'HTTP方法：GET/POST/PUT/DELETE/*',
  `perm_type`       varchar(20)   NOT NULL DEFAULT 'api'              COMMENT '权限类型：api=接口 / menu=菜单 / button=按钮',
  `description`     varchar(200)           DEFAULT NULL               COMMENT '权限描述',
  `status`          tinyint       NOT NULL DEFAULT 1                  COMMENT '状态：1=启用 0=禁用',
  `created_at`      timestamp     NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
  `updated_at`      timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_perm_url` (`url`),
  KEY `idx_perm_method` (`method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统接口权限点表';

INSERT INTO `sys_permission` (`permission_id`,`permission_code`,`name`,`url`,`method`,`perm_type`,`description`) VALUES
  (1, 'dashboard:view',      '仪表盘查看',     '/api/dashboard/**',          'GET',    'api', '查看经营仪表盘数据'),
  (2, 'booking:list',        '预订列表查询',   '/api/bookings',              'GET',    'api', '查询宴会预订列表'),
  (3, 'booking:create',      '创建预订',       '/api/bookings',              'POST',   'api', '新建宴会预订'),
  (4, 'booking:update',      '修改预订',       '/api/bookings/**',           'PUT',    'api', '修改预订信息'),
  (5, 'booking:delete',      '删除预订',       '/api/bookings/**',           'DELETE', 'api', '删除预订记录'),
  (6, 'staff:list',          '员工列表查询',   '/api/hr/staff',              'GET',    'api', '查询员工列表'),
  (7, 'staff:create',        '新增员工',       '/api/hr/staff',              'POST',   'api', '新增员工档案'),
  (8, 'staff:update',        '修改员工',       '/api/hr/staff/**',           'PUT',    'api', '修改员工信息'),
  (9, 'dish:list',           '菜品列表查询',   '/api/dishes',                'GET',    'api', '查询菜品列表'),
  (10,'dish:manage',         '菜品管理',       '/api/dishes',                'POST',   'api', '新增/修改菜品'),
  (11,'customer:list',       '客户列表查询',   '/api/customers',             'GET',    'api', '查询客户列表'),
  (12,'customer:manage',     '客户管理',       '/api/customers',             'POST',   'api', '新增/修改客户'),
  (13,'table:list',          '桌位查询',       '/api/tables',                'GET',    'api', '查询桌位状态'),
  (14,'inventory:view',      '库存查询',       '/api/menu-api/inventory/**','GET',    'api', '查询库存信息(对齐 InventoryController @RequestMapping=/api/menu-api/inventory)'),
  (15,'inventory:manage',    '库存管理',       '/api/menu-api/inventory/**','POST',   'api', '库存出入库操作(对齐 InventoryController @RequestMapping=/api/menu-api/inventory)'),
  (16,'purchase:manage',     '采购管理',       '/api/menu-api/purchases/**','POST',   'api', '采购单管理(对齐 PurchaseController @RequestMapping=/api/menu-api/purchases)'),
  (17,'finance:view',        '财务查看',       '/api/finance/**',           'GET',    'api', '查看财务账户/应付应收/报销/凭证/报表(对齐 FinanceAccountController=/api/finance/accounts, FinancePayableController=/api/finance/payables, FinanceExpenseController=/api/finance/expenses, FinanceVoucherController=/api/finance/vouchers, FinanceReportController=/api/finance)'),
  (18,'hr:payroll',          '薪资管理',       '/api/hr/payroll/**',         'POST',   'api', '薪资发放管理(对齐 PayrollController @RequestMapping=/api/hr/payroll)'),
  (19,'system:perm',         '权限管理',       '/api/perm/**',               '*',      'api', '系统权限配置'),
  (20,'audit:view',          '审计日志查看',   '/api/audit/**',              'GET',    'api', '查看操作审计日志')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- ---------------------------------------------------------------------
-- 3. 员工-角色关联表 sys_user_role
--    字段：staff_id / role_id
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id`         bigint    NOT NULL AUTO_INCREMENT                       COMMENT '主键',
  `staff_id`   bigint    NOT NULL                                      COMMENT '员工ID（staff_master.staff_id）',
  `role_id`    bigint    NOT NULL                                      COMMENT '角色ID（sys_role.role_id）',
  `store_id`   bigint    NOT NULL DEFAULT 0                            COMMENT '冗余门店ID，便于按门店筛选',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_role` (`staff_id`,`role_id`),
  KEY `idx_ur_staff` (`staff_id`),
  KEY `idx_ur_role`  (`role_id`),
  KEY `idx_ur_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工-角色关联表';

-- 默认员工-角色映射：
-- staff_id=1 (rino/总经理) → GM (全门店)
-- staff_id=100 (张婧/超级管理员) → GM (全门店)
-- staff_id=101 (宁国店长) → HQ_STAFF (总店)
-- staff_id=102 (宣城店长) → STORE_MANAGER (分店)
INSERT INTO `sys_user_role` (`staff_id`,`role_id`,`store_id`) VALUES
  (1,   1, 0),
  (100, 1, 0),
  (101, 2, 1),
  (102, 3, 2)
ON DUPLICATE KEY UPDATE `store_id`=VALUES(`store_id`);

-- ---------------------------------------------------------------------
-- 4. 角色-权限关联表 sys_role_permission
--    字段：role_id / permission_id
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id`            bigint    NOT NULL AUTO_INCREMENT                    COMMENT '主键',
  `role_id`       bigint    NOT NULL                                   COMMENT '角色ID（sys_role.role_id）',
  `permission_id` bigint    NOT NULL                                   COMMENT '权限ID（sys_permission.permission_id）',
  `created_at`    timestamp NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_rp_role` (`role_id`),
  KEY `idx_rp_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- 超级总经理(GM, role_id=1) 拥有全部权限
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT 1, `permission_id` FROM `sys_permission`
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 总店员工(HQ_STAFF, role_id=2) 仅业务操作权限，不含系统/审计权限
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (2,1),(2,2),(2,3),(2,4),(2,6),(2,9),(2,11),(2,12),(2,13),(2,14),(2,17)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 分店店长(STORE_MANAGER, role_id=3) 本店经营+人员管理
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (3,1),(3,2),(3,3),(3,4),(3,6),(3,7),(3,8),(3,9),(3,10),(3,11),(3,12),(3,13),(3,14),(3,15),(3,17)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 分店服务员(WAITER, role_id=4) 仅预订与桌位基础操作
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (4,1),(4,2),(4,3),(4,11),(4,13)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- ---------------------------------------------------------------------
-- 5. 前端动态菜单权限表 sys_menu
--    字段：menu_name / path / icon / parent_id / permission_code /
--          sort_order / visible
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id`         bigint        NOT NULL AUTO_INCREMENT              COMMENT '菜单ID',
  `parent_id`       bigint        NOT NULL DEFAULT 0                   COMMENT '父菜单ID：0=顶级',
  `menu_name`       varchar(50)   NOT NULL                             COMMENT '菜单名称',
  `path`            varchar(200)           DEFAULT NULL                COMMENT '前端路由路径',
  `icon`            varchar(100)           DEFAULT NULL                COMMENT '菜单图标',
  `permission_code` varchar(100)           DEFAULT NULL                COMMENT '关联权限编码（sys_permission.permission_code）',
  `menu_type`       varchar(20)   NOT NULL DEFAULT 'menu'              COMMENT '类型：directory=目录 / menu=菜单 / button=按钮',
  `store_scope`     varchar(20)   NOT NULL DEFAULT 'all'               COMMENT '门店范围：all=全门店 / hq=仅总店 / branch=仅分店',
  `sort_order`      int           NOT NULL DEFAULT 0                   COMMENT '排序值',
  `visible`         tinyint       NOT NULL DEFAULT 1                   COMMENT '是否可见：1=可见 0=隐藏',
  `status`          tinyint       NOT NULL DEFAULT 1                   COMMENT '状态：1=启用 0=禁用',
  `created_at`      timestamp     NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
  `updated_at`      timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`),
  KEY `idx_menu_perm`   (`permission_code`),
  KEY `idx_menu_sort`   (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前端动态菜单权限表';

INSERT INTO `sys_menu` (`menu_id`,`parent_id`,`menu_name`,`path`,`icon`,`permission_code`,`menu_type`,`store_scope`,`sort_order`,`visible`) VALUES
  (1,  0, '经营总览',  '/dashboard',      'Odometer',    'dashboard:view', 'menu', 'all',     1, 1),
  (2,  0, '宴会预订',  '/bookings',       'Calendar',    'booking:list',   'menu', 'all',     2, 1),
  (3,  2, '新建预订',  '/bookings/new',   'Plus',        'booking:create', 'menu', 'all',     1, 1),
  (4,  0, '菜品管理',  '/menu',           'Dish',        'dish:list',      'menu', 'all',     3, 1),
  (5,  0, '客户管理',  '/customers',      'User',        'customer:list',  'menu', 'all',     4, 1),
  (6,  0, '桌位看板',  '/tables',         'Grid',        'table:list',     'menu', 'all',     5, 1),
  (7,  0, '库存管理',  '/inventory',      'Box',         'inventory:view', 'menu', 'all',     6, 1),
  (8,  0, '采购管理',  '/purchase',       'ShoppingCart','purchase:manage','menu', 'hq',      7, 1),
  (9,  0, '财务报表',  '/finance',        'Money',       'finance:view',   'menu', 'all',     8, 1),
  (10, 0, '人力资源',  '/hr',             'UserFilled',  'staff:list',     'menu', 'all',     9, 1),
  (11,10, '员工档案',  '/hr/staff',       'Document',    'staff:list',     'menu', 'all',     1, 1),
  (12,10, '薪资管理',  '/hr/payroll',     'Wallet',      'hr:payroll',     'menu', 'hq',      2, 1),
  (13, 0, '系统管理',  '/system',         'Setting',     'system:perm',    'menu', 'all',    10, 1),
  (14,13, '权限管理',  '/system/perm',    'Lock',        'system:perm',    'menu', 'all',     1, 1),
  (15,13, '审计日志',  '/system/audit',   'List',        'audit:view',     'menu', 'all',     2, 1)
ON DUPLICATE KEY UPDATE `menu_name`=VALUES(`menu_name`);

/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
