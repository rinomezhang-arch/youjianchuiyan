-- 系统初始化基础数据SQL.sql | 又见炊烟餐饮管理系统V2.0 | 生成日期：2026-08-02
--
-- 说明：系统首次部署初始化数据（门店 / 字典 / 权限 / 角色 / 菜单 / 示例员工与用户）
-- 数据库：MySQL 8.0 / banquet / utf8mb4
-- 多门店隔离：所有业务数据带 store_id（0=全局，1=宁国店，2=宣城店）
-- 菜单结构依据前端 Dashboard.vue 实际模块组织
-- 依赖：先执行 banquet_init.sql 与 rbac_init.sql 建表后再执行本脚本
-- 幂等：使用 INSERT IGNORE，重复执行不会产生重复数据

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============ 1. 门店初始化 ============

INSERT IGNORE INTO `store_info`
  (`store_id`,`store_code`,`store_name`,`store_short_name`,`store_type`,`store_level`,`address`,`province`,`city`,`district`,`phone`,`contact_person`,`business_hours`,`table_count`,`max_capacity`,`business_area`,`manager_id`,`manager_name`,`opening_date`,`status`,`sort_order`,`remark`)
VALUES
  (1,'HQ_NINGGUO','又见炊烟（宁国店）','宁国店','normal','flagship','安徽省宁国市宁城南路88号','安徽省','宣城市','宁国市','0563-4018888','秋总','10:00-21:00',20,200,800.00,2,'宁国店长','2026-01-08','open',1,'总店/旗舰店'),
  (2,'BR_XUANCHENG','又见炊烟（宣城店）','宣城店','normal','branch','安徽省宣城市宣州区鳌峰东路66号','安徽省','宣城市','宣州区','0563-3026666','宣城店长','10:00-21:00',15,150,500.00,5,'宣城店长','2026-03-18','open',2,'分店');

-- ============ 2. 字典初始化 ============

INSERT IGNORE INTO `sys_dict`
  (`dict_id`,`dict_code`,`dict_name`,`dict_type`,`store_id`,`description`,`sort_order`,`is_active`)
VALUES
  (1,'employment_status','员工状态','list',0,'员工在职状态枚举',1,1),
  (2,'booking_status','预订状态','list',0,'宴会预订状态枚举',2,1),
  (3,'attendance_status','考勤状态','list',0,'考勤打卡状态枚举',3,1),
  (4,'store_status','门店状态','list',0,'门店营业状态枚举',4,1),
  (5,'approval_status','审批状态','list',0,'审批流程状态枚举',5,1),
  (6,'procurement_status','采购申请状态','list',0,'采购申请状态枚举',6,1),
  (7,'purchase_status','采购订单状态','list',0,'采购订单状态枚举',7,1),
  (8,'dish_status','菜品状态','list',0,'菜品上下架状态枚举',8,1),
  (9,'table_status','桌位状态','list',0,'桌位占用状态枚举',9,1),
  (10,'gender','性别','list',0,'性别枚举',10,1),
  (11,'kitchen_status','后厨状态','list',0,'后厨出菜状态枚举',11,1),
  (12,'payment_status','付款状态','list',0,'财务付款状态枚举',12,1),
  (13,'booking_type','预订类型','list',0,'宴会预订类型枚举',13,1),
  (14,'occasion_type','场合类型','list',0,'宴会场合类型枚举',14,1),
  (15,'reimbursement_status','报销状态','list',0,'费用报销状态枚举',15,1),
  (16,'stock_transfer_status','调拨状态','list',0,'库存调拨状态枚举',16,1),
  (17,'tool_issue_status','工具领用状态','list',0,'工具领用状态枚举',17,1);

INSERT IGNORE INTO `sys_dict_item`
  (`item_id`,`dict_id`,`dict_code`,`item_value`,`item_label`,`store_id`,`sort_order`,`is_active`,`remark`)
VALUES
  (1 ,1,'employment_status','active','在职',0,1,1,NULL),
  (2 ,1,'employment_status','probation','试用期',0,2,1,NULL),
  (3 ,1,'employment_status','resigned','离职',0,3,1,NULL),
  (4 ,1,'employment_status','suspended','停职',0,4,1,NULL),
  (5 ,2,'booking_status','pending','待确认',0,1,1,NULL),
  (6 ,2,'booking_status','confirmed','已确认',0,2,1,NULL),
  (7 ,2,'booking_status','deposited','已收订金',0,3,1,NULL),
  (8 ,2,'booking_status','completed','已完成',0,4,1,NULL),
  (9 ,2,'booking_status','cancelled','已取消',0,5,1,NULL),
  (10,3,'attendance_status','normal','正常',0,1,1,NULL),
  (11,3,'attendance_status','late','迟到',0,2,1,NULL),
  (12,3,'attendance_status','early_leave','早退',0,3,1,NULL),
  (13,3,'attendance_status','absent','缺勤',0,4,1,NULL),
  (14,3,'attendance_status','leave','请假',0,5,1,NULL),
  (15,4,'store_status','open','营业',0,1,1,NULL),
  (16,4,'store_status','closed','歇业',0,2,1,NULL),
  (17,4,'store_status','decorating','装修',0,3,1,NULL),
  (18,5,'approval_status','pending','待审批',0,1,1,NULL),
  (19,5,'approval_status','approved','已通过',0,2,1,NULL),
  (20,5,'approval_status','rejected','已驳回',0,3,1,NULL),
  (21,5,'approval_status','cancelled','已撤销',0,4,1,NULL),
  (22,6,'procurement_status','draft','草稿',0,1,1,NULL),
  (23,6,'procurement_status','submitted','已提交',0,2,1,NULL),
  (24,6,'procurement_status','approved','已审批',0,3,1,NULL),
  (25,6,'procurement_status','rejected','已驳回',0,4,1,NULL),
  (26,6,'procurement_status','done','已完成',0,5,1,NULL),
  (27,7,'purchase_status','draft','草稿',0,1,1,NULL),
  (28,7,'purchase_status','ordered','已下单',0,2,1,NULL),
  (29,7,'purchase_status','received','已入库',0,3,1,NULL),
  (30,7,'purchase_status','partial','部分入库',0,4,1,NULL),
  (31,7,'purchase_status','cancelled','已取消',0,5,1,NULL),
  (32,8,'dish_status','active','在售',0,1,1,NULL),
  (33,8,'dish_status','off','停售',0,2,1,NULL),
  (34,8,'dish_status','soldout','沽清',0,3,1,NULL),
  (35,8,'dish_status','seasonal','季节性',0,4,1,NULL),
  (36,9,'table_status','idle','空闲',0,1,1,NULL),
  (37,9,'table_status','seated','就座',0,2,1,NULL),
  (38,9,'table_status','reserved','已预订',0,3,1,NULL),
  (39,9,'table_status','cleaning','清洁中',0,4,1,NULL),
  (40,10,'gender','male','男',0,1,1,NULL),
  (41,10,'gender','female','女',0,2,1,NULL),
  (42,10,'gender','unknown','未知',0,3,1,NULL),
  (43,11,'kitchen_status','pending','待出菜',0,1,1,NULL),
  (44,11,'kitchen_status','cooking','制作中',0,2,1,NULL),
  (45,11,'kitchen_status','served','已出菜',0,3,1,NULL),
  (46,11,'kitchen_status','timeout','超时',0,4,1,NULL),
  (47,12,'payment_status','unpaid','未付款',0,1,1,NULL),
  (48,12,'payment_status','partial','部分付款',0,2,1,NULL),
  (49,12,'payment_status','paid','已付款',0,3,1,NULL),
  (50,13,'booking_type','wedding','婚宴',0,1,1,NULL),
  (51,13,'booking_type','birthday','寿宴',0,2,1,NULL),
  (52,13,'booking_type','business','商务宴请',0,3,1,NULL),
  (53,13,'booking_type','family','家庭聚会',0,4,1,NULL),
  (54,14,'occasion_type','lunch','午市',0,1,1,NULL),
  (55,14,'occasion_type','dinner','晚市',0,2,1,NULL),
  (56,14,'occasion_type','private','包场',0,3,1,NULL),
  (57,15,'reimbursement_status','draft','草稿',0,1,1,NULL),
  (58,15,'reimbursement_status','submitted','已提交',0,2,1,NULL),
  (59,15,'reimbursement_status','approved','已审批',0,3,1,NULL),
  (60,15,'reimbursement_status','paid','已付款',0,4,1,NULL),
  (61,16,'stock_transfer_status','draft','草稿',0,1,1,NULL),
  (62,16,'stock_transfer_status','transferring','调拨中',0,2,1,NULL),
  (63,16,'stock_transfer_status','received','已入库',0,3,1,NULL),
  (64,16,'stock_transfer_status','cancelled','已取消',0,4,1,NULL),
  (65,17,'tool_issue_status','issued','已领用',0,1,1,NULL),
  (66,17,'tool_issue_status','returned','已归还',0,2,1,NULL),
  (67,17,'tool_issue_status','damaged','已损坏',0,3,1,NULL),
  (68,17,'tool_issue_status','lost','已丢失',0,4,1,NULL);

-- ============ 3. 权限初始化 ============

INSERT IGNORE INTO `sys_permission`
  (`permission_id`,`perm_code`,`name`,`parent_id`,`url`,`method`,`perm_type`,`description`,`status`)
VALUES
  (1 ,'dashboard:view','仪表盘查看',0,'/api/dashboard/**','GET','api','查看经营仪表盘数据',1),
  (2 ,'booking:list','预订列表查询',0,'/api/bookings','GET','api','查询宴会预订列表',1),
  (3 ,'booking:create','创建预订',0,'/api/bookings','POST','api','新建宴会预订',1),
  (4 ,'booking:update','修改预订',0,'/api/bookings/**','PUT','api','修改预订信息',1),
  (5 ,'booking:delete','删除预订',0,'/api/bookings/**','DELETE','api','删除预订记录',1),
  (6 ,'staff:list','员工列表查询',0,'/api/hr/staff','GET','api','查询员工列表',1),
  (7 ,'staff:create','新增员工',0,'/api/hr/staff','POST','api','新增员工档案',1),
  (8 ,'staff:update','修改员工',0,'/api/hr/staff/**','PUT','api','修改员工信息',1),
  (9 ,'dish:list','菜品列表查询',0,'/api/dishes','GET','api','查询菜品列表',1),
  (10,'dish:manage','菜品管理',0,'/api/dishes','POST','api','新增/修改菜品',1),
  (11,'customer:list','客户列表查询',0,'/api/customers','GET','api','查询客户列表',1),
  (12,'customer:manage','客户管理',0,'/api/customers','POST','api','新增/修改客户',1),
  (13,'table:list','桌位查询',0,'/api/tables','GET','api','查询桌位状态',1),
  (14,'inventory:view','库存查询',0,'/api/menu-api/inventory/**','GET','api','查询库存信息',1),
  (15,'inventory:manage','库存管理',0,'/api/menu-api/inventory/**','POST','api','库存出入库操作',1),
  (16,'purchase:manage','采购管理',0,'/api/menu-api/purchases/**','POST','api','采购单管理',1),
  (17,'finance:view','财务查看',0,'/api/finance/**','GET','api','查看财务账户/应付应收/报销/凭证/报表',1),
  (18,'hr:payroll','薪资管理',0,'/api/hr/payroll/**','POST','api','薪资发放管理',1),
  (19,'system:perm','权限管理',0,'/api/perm/**','*','api','系统权限配置',1),
  (20,'audit:view','审计日志查看',0,'/api/audit/**','GET','api','查看操作审计日志',1);

-- ============ 4. 角色初始化 ============

INSERT IGNORE INTO `sys_role`
  (`role_id`,`role_code`,`role_name`,`store_id`,`data_scope`,`description`,`status`,`sort_order`)
VALUES
  (1,'BOSS','老板',0,'all','集团老板，跨门店全局管理',1,1),
  (2,'ADMIN','管理员',0,'all','系统管理员，全部权限',1,2),
  (3,'STORE_MANAGER','店长',0,'store','门店店长，本店经营与人员管理',1,3),
  (4,'WAITER','服务员',0,'store','前厅服务员，本店基础业务',1,4),
  (5,'CHEF','厨师长',0,'store','后厨厨师长，本店出品与厨房管理',1,5),
  (6,'CASHIER','收银员',0,'store','前厅收银，本店结算与账单',1,6),
  (7,'FINANCE','财务',0,'store','财务人员，本店财务核算与报表',1,7),
  (8,'PURCHASER','采购',0,'store','采购员，本店采购与供应商对接',1,8),
  (9,'WAREHOUSE','库管',0,'store','库管员，本店库存与出入库管理',1,9);

-- ============ 5. 菜单初始化（依据 Dashboard.vue 模块结构） ============

INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`parent_id`,`menu_name`,`menu_path`,`menu_icon`,`perm_code`,`menu_type`,`store_scope`,`sort_order`,`visible`,`status`)
VALUES
  (1 ,0,'工作台','/dashboard/home','home','dashboard:view','menu','all',1,1,1),
  (2 ,0,'桌台看板','/dashboard/table-board','table','table:list','menu','all',2,1,1),
  (3 ,0,'前厅运营','/dashboard/front-office','front',NULL,'directory','all',3,1,1),
  (4 ,0,'菜单管理','/dashboard/menu','menu',NULL,'directory','all',4,1,1),
  (5 ,0,'厨房管理','/dashboard/kitchen','kitchen',NULL,'directory','all',5,1,1),
  (6 ,0,'采购仓储','/dashboard/supply-chain','supply',NULL,'directory','all',6,1,1),
  (7 ,0,'财务数据','/dashboard/finance','finance',NULL,'directory','all',7,1,1),
  (8 ,0,'营销会员','/dashboard/marketing','marketing',NULL,'directory','all',8,1,1),
  (9 ,0,'人事行政','/dashboard/hr-admin','hr',NULL,'directory','all',9,1,1),
  (10,0,'工程管理','/dashboard/engineering','engineering',NULL,'directory','all',10,1,1),
  (11,0,'总经办','/dashboard/gm-office','gm',NULL,'directory','all',11,1,1),
  (12,0,'系统工具','/dashboard/bill-manage','system',NULL,'directory','all',12,1,1),
  (13,0,'系统设置','/dashboard/settings','settings',NULL,'directory','all',13,1,1),
  (14,0,'数据大屏','/dashboard/data-screen','analytics',NULL,'directory','all',14,1,1),
  -- 前厅运营子菜单
  (15,3,'前台预定','/dashboard/front-desk','booking','booking:create','menu','all',1,1,1),
  (16,3,'客人分析','/dashboard/guest-analysis','guest',NULL,'menu','all',2,1,1),
  (17,3,'员工绩效','/dashboard/staff-performance','staff',NULL,'menu','all',3,1,1),
  (18,3,'桌台利用率','/dashboard/table-utilization','tableUtil',NULL,'menu','all',4,1,1),
  (19,3,'报表打印','/dashboard/report-print','print',NULL,'menu','all',5,1,1),
  (20,3,'预订管理','/dashboard/bookings','bookings','booking:list','menu','all',6,1,1),
  (21,3,'客户管理','/dashboard/customers','customer','customer:list','menu','all',7,1,1),
  (22,3,'台型设计','/dashboard/table-layout','layout',NULL,'menu','all',8,1,1),
  (23,3,'美工设计','/dashboard/art-design','art',NULL,'menu','all',9,1,1),
  -- 菜单管理子菜单
  (24,4,'点菜','/dashboard/ordering','ordering',NULL,'menu','all',1,1,1),
  (25,4,'菜库编辑','/dashboard/dish-library','dishLib','dish:list','menu','all',2,1,1),
  (26,4,'成本配方','/dashboard/cost-recipe','recipe','dish:manage','menu','all',3,1,1),
  (27,4,'套餐管理','/dashboard/set-menu','setMenu',NULL,'menu','all',4,1,1),
  (28,4,'调价管理','/dashboard/pricing','pricing',NULL,'menu','all',5,1,1),
  (29,4,'沽清管控','/dashboard/sold-out','soldout',NULL,'menu','all',6,1,1),
  (30,4,'标签管理','/dashboard/tags','tags',NULL,'menu','all',7,1,1),
  (31,4,'打印配置','/dashboard/print-config','printCfg',NULL,'menu','all',8,1,1),
  (32,4,'门店权限','/dashboard/store-permission','storePerm','system:perm','menu','all',9,1,1),
  (33,4,'操作日志','/dashboard/audit-log','auditLog','audit:view','menu','all',10,1,1),
  (34,4,'多价格体系','/dashboard/price-tiers','tiers',NULL,'menu','all',11,1,1),
  -- 厨房管理子菜单
  (35,5,'后厨日志','/dashboard/kitchen-log','kitchenLog',NULL,'menu','all',1,1,1),
  (36,5,'出品管理','/dashboard/production','production',NULL,'menu','all',2,1,1),
  (37,5,'套餐管理','/dashboard/packages','package',NULL,'menu','all',3,1,1),
  -- 采购仓储子菜单
  (38,6,'库存管理','/dashboard/inventory','inventory','inventory:view','menu','all',1,1,1),
  (39,6,'采购管理','/dashboard/procurement','procurement','purchase:manage','menu','all',2,1,1),
  (40,6,'入库验收','/dashboard/receipt','receipt',NULL,'menu','all',3,1,1),
  (41,6,'领用出库','/dashboard/issue','issue',NULL,'menu','all',4,1,1),
  (42,6,'供应商对账','/dashboard/supplier-reconciliation','reconciliation','finance:view','menu','all',5,1,1),
  (43,6,'盘点','/dashboard/stock-take','stocktake','inventory:manage','menu','all',6,1,1),
  (44,6,'供应商','/dashboard/suppliers','supplier',NULL,'menu','all',7,1,1),
  -- 财务数据子菜单
  (45,7,'菜品成本','/dashboard/finance/dish-cost','cost',NULL,'menu','all',1,1,1),
  (46,7,'成本分析','/dashboard/finance/cost-analysis','analysis',NULL,'menu','all',2,1,1),
  (47,7,'数据报表','/dashboard/reports','report','finance:view','menu','all',3,1,1),
  (48,7,'菜品成本分析','/dashboard/dish-cost-analysis','dishAnalysis',NULL,'menu','all',4,1,1),
  -- 营销会员子菜单
  (49,8,'营销活动','/dashboard/marketing','marketing',NULL,'menu','all',1,1,1),
  (50,8,'会员列表','/dashboard/member-list','customer','customer:list','menu','all',2,1,1),
  -- 人事行政子菜单
  (51,9,'员工档案','/dashboard/staff','staffFile','staff:list','menu','all',1,1,1),
  (52,9,'考勤日历','/dashboard/attendance-calendar','attendance',NULL,'menu','all',2,1,1),
  (53,9,'考勤报表','/dashboard/attendance-print','print',NULL,'menu','all',3,1,1),
  (54,9,'工资管理','/dashboard/payroll','finance','hr:payroll','menu','hq',4,1,1),
  (55,9,'HR数据','/dashboard/hr-analytics','analytics',NULL,'menu','all',5,1,1),
  (56,9,'自助登记','/dashboard/self-service','staff',NULL,'menu','all',6,1,1),
  (57,9,'审核队列','/dashboard/review-queue','license',NULL,'menu','all',7,1,1),
  (58,9,'培训管理','/dashboard/training','training',NULL,'menu','all',8,1,1),
  (59,9,'考勤管理','/dashboard/attendance','attendance',NULL,'menu','all',9,1,1),
  (60,9,'排班管理','/dashboard/schedule','schedule',NULL,'menu','all',10,1,1),
  (61,9,'请假管理','/dashboard/leave','leave',NULL,'menu','all',11,1,1),
  (62,9,'证照管理','/dashboard/license','license',NULL,'menu','all',12,1,1),
  (63,9,'安保保洁','/dashboard/security','security',NULL,'menu','all',13,1,1),
  (64,9,'行政资产','/dashboard/assets','assets',NULL,'menu','all',14,1,1),
  -- 工程管理子菜单
  (65,10,'装修管理','/dashboard/decoration','decoration',NULL,'menu','all',1,1,1),
  (66,10,'设备维护','/dashboard/maintenance','maintenance',NULL,'menu','all',2,1,1),
  (67,10,'能耗管理','/dashboard/energy','energy',NULL,'menu','all',3,1,1),
  (68,10,'安全管理','/dashboard/safety','safety',NULL,'menu','all',4,1,1),
  (69,10,'工程维护','/dashboard/floor-project','floorMaint',NULL,'menu','all',5,1,1),
  -- 系统工具子菜单
  (70,12,'系统体检','/dashboard/system-checkup','checkup',NULL,'menu','all',1,1,1),
  (71,12,'账单管理','/dashboard/bill-manage','bill',NULL,'menu','all',2,1,1),
  (72,12,'iPad点菜','/dashboard/ipad-menu','ipad',NULL,'menu','all',3,1,1),
  (73,12,'帮助与日志','/dashboard/help','help',NULL,'menu','all',4,1,1);

-- ============ 6. 角色-权限关联 ============

-- 老板(1)、管理员(2) 拥有全部权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.role_id, p.permission_id FROM `sys_role` r CROSS JOIN `sys_permission` p
WHERE r.role_id IN (1,2);

-- 店长(3)：本店经营+人员管理（不含系统权限/审计/删除预订）
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (3,1),(3,2),(3,3),(3,4),(3,6),(3,7),(3,8),(3,9),(3,10),(3,11),(3,12),(3,13),(3,14),(3,15),(3,17),(3,20);

-- 服务员(4)：预订与桌位基础操作
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (4,1),(4,2),(4,3),(4,11),(4,13);

-- 厨师长(5)：菜品与库存查看管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (5,1),(5,9),(5,10),(5,14),(5,15);

-- 收银员(6)：预订与财务查看
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (6,1),(6,2),(6,17);

-- 财务(7)：财务查看与薪资管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (7,1),(7,17),(7,18),(7,20);

-- 采购(8)：采购与库存管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (8,1),(8,14),(8,15),(8,16);

-- 库管(9)：库存查看与管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
  (9,1),(9,14),(9,15);

-- ============ 7. 角色-菜单关联 ============

-- 老板(1)、管理员(2)：全部菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_id IN (1,2);

-- 店长(3)：经营类菜单（排除系统工具/系统设置/数据大屏/总经办/操作日志/门店权限）
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 3, m.menu_id FROM `sys_menu` m
WHERE m.menu_path NOT LIKE '/dashboard/gm-office%'
  AND m.menu_path NOT LIKE '/dashboard/bill-manage%'
  AND m.menu_path NOT LIKE '/dashboard/settings%'
  AND m.menu_path NOT LIKE '/dashboard/data-screen%'
  AND m.menu_path NOT LIKE '/dashboard/system-checkup%'
  AND m.menu_path NOT LIKE '/dashboard/ipad-menu%'
  AND m.menu_path NOT LIKE '/dashboard/help%'
  AND m.menu_path NOT LIKE '/dashboard/audit-log%'
  AND m.menu_path NOT LIKE '/dashboard/store-permission%';

-- 服务员(4)：工作台/桌台看板/前厅运营
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (4,1),(4,2),(4,3),(4,15),(4,20),(4,21);

-- 厨师长(5)：工作台/桌台看板/菜单管理/厨房管理/采购仓储-库存
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (5,1),(5,2),(5,4),(5,25),(5,26),(5,29),(5,5),(5,35),(5,36),(5,37),(5,6),(5,38);

-- 收银员(6)：工作台/桌台看板/前厅运营/财务数据
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (6,1),(6,2),(6,3),(6,15),(6,19),(6,20),(6,7),(6,47);

-- 财务(7)：工作台/财务数据/人事-工资
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (7,1),(7,7),(7,45),(7,46),(7,47),(7,48),(7,9),(7,54);

-- 采购(8)：工作台/采购仓储
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (8,1),(8,6),(8,38),(8,39),(8,40),(8,42),(8,43),(8,44);

-- 库管(9)：工作台/采购仓储-库存/盘点/入库/领用
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  (9,1),(9,6),(9,38),(9,40),(9,41),(9,43);

-- ============ 8. 示例员工与用户 ============

-- 示例员工（staff_master）
INSERT IGNORE INTO `staff_master`
  (`staff_id`,`store_id`,`staff_name`,`staff_account`,`staff_password`,`staff_gender`,`staff_age`,`staff_phone`,`staff_position`,`department`,`hire_date`,`monthly_salary`,`employment_status`,`role`)
VALUES
  (1  ,1,'秋总','rino','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','男',45,'13900139001','总经理','管理层','2026-01-08',30000.00,'active','老板'),
  (2  ,1,'李宁国','ng_manager','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','男',38,'13900139002','店长','管理层','2026-01-10',12000.00,'active','店长'),
  (3  ,1,'王厨师','ng_chef','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','男',40,'13900139003','厨师长','后厨','2026-01-12',10000.00,'active','厨师长'),
  (4  ,1,'李服务','ng_waiter','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','女',24,'13900139004','服务员','前厅','2026-02-01',4500.00,'active','服务员'),
  (5  ,2,'张宣城','xc_manager','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','男',36,'13900139005','店长','管理层','2026-03-18',12000.00,'active','店长'),
  (6  ,2,'陈财务','xc_finance','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','女',32,'13900139006','财务','财务部','2026-03-20',8000.00,'active','财务'),
  (7  ,2,'周采购','xc_purchase','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','男',35,'13900139007','采购','采购部','2026-03-22',7000.00,'active','采购'),
  (100,0,'张婧','admin','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','女',28,'13900139000','超级管理员','信息中心','2026-01-01',9000.00,'active','管理员');

-- 示例用户（sys_user）默认密码均为 admin，首次登录后请强制修改
INSERT IGNORE INTO `sys_user`
  (`user_id`,`username`,`password_hash`,`real_name`,`phone`,`store_id`,`staff_id`,`status`)
VALUES
  (1,'admin','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','张婧','13900139000',0,100,1),
  (2,'rino','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','秋总','13900139001',1,1,1),
  (3,'ng_manager','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','李宁国','13900139002',1,2,1),
  (4,'xc_manager','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','张宣城','13900139005',2,5,1),
  (5,'ng_chef','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','王厨师','13900139003',1,3,1),
  (6,'ng_waiter','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','李服务','13900139004',1,4,1),
  (7,'xc_finance','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','陈财务','13900139006',2,6,1),
  (8,'xc_purchase','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','周采购','13900139007',2,7,1);

-- 用户-角色关联
INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`,`store_id`) VALUES
  (1,2,0),  -- admin -> 管理员（全局）
  (2,1,1),  -- rino -> 老板（宁国店）
  (3,3,1),  -- ng_manager -> 店长（宁国店）
  (4,3,2),  -- xc_manager -> 店长（宣城店）
  (5,5,1),  -- ng_chef -> 厨师长（宁国店）
  (6,4,1),  -- ng_waiter -> 服务员（宁国店）
  (7,7,2),  -- xc_finance -> 财务（宣城店）
  (8,8,2);  -- xc_purchase -> 采购（宣城店）

SET FOREIGN_KEY_CHECKS = 1;
