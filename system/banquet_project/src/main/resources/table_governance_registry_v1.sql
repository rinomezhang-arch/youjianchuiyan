-- 表级数据治理登记：解释每张表的现实用途与空表策略。
CREATE TABLE IF NOT EXISTS system_table_registry (
  table_name varchar(128) NOT NULL,
  business_domain varchar(50) NOT NULL,
  data_kind varchar(30) NOT NULL COMMENT 'MASTER/TRANSACTION/DETAIL/LOG/SNAPSHOT/CONFIG/LEGACY',
  empty_policy varchar(30) NOT NULL COMMENT 'ALLOW_EMPTY/REQUIRE_SEED/REQUIRE_BUSINESS_DATA/DEPRECATE',
  backend_binding varchar(30) NOT NULL DEFAULT 'SQL_OR_ENTITY',
  frontend_binding varchar(30) NOT NULL DEFAULT 'VIA_API',
  purpose varchar(255) NOT NULL,
  reviewed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库表用途与贯通状态登记';

INSERT INTO system_table_registry(table_name,business_domain,data_kind,empty_policy,purpose)
SELECT t.TABLE_NAME,'general','MASTER','ALLOW_EMPTY',CONCAT('系统业务表：',t.TABLE_COMMENT)
FROM information_schema.TABLES t
WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME<>'system_table_registry'
ON DUPLICATE KEY UPDATE purpose=VALUES(purpose),reviewed_at=CURRENT_TIMESTAMP;

INSERT INTO system_table_registry(table_name,business_domain,data_kind,empty_policy,backend_binding,frontend_binding,purpose)
VALUES ('system_table_registry','system','CONFIG','REQUIRE_SEED','SQL','VIA_API','数据库表用途、数据性质、空表政策与全栈连接状态登记')
ON DUPLICATE KEY UPDATE purpose=VALUES(purpose),reviewed_at=CURRENT_TIMESTAMP;

UPDATE system_table_registry SET business_domain='finance',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='财务业务事实；无真实业务发生时允许为空，禁止伪造金额'
WHERE table_name LIKE 'finance_%' OR table_name IN ('reimbursement');
UPDATE system_table_registry SET business_domain='member',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='会员业务事实；须由真实注册、储值或消费产生'
WHERE table_name LIKE 'member_%' AND table_name NOT IN ('member_level','member_point_rule');
UPDATE system_table_registry SET business_domain='member',data_kind='CONFIG',empty_policy='REQUIRE_SEED',purpose='会员制度主数据；由门店政策初始化'
WHERE table_name IN ('member_level','member_point_rule');
UPDATE system_table_registry SET business_domain='procurement',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='采购业务事实；由审批、收货或退货流程产生'
WHERE table_name LIKE 'purchase_%' OR table_name LIKE 'procurement_%';
UPDATE system_table_registry SET business_domain='inventory',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='库存业务事实；由盘点、调拨、损耗流程产生'
WHERE table_name LIKE 'stock_%' OR table_name LIKE 'inventory_%';
UPDATE system_table_registry SET business_domain='hr',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='人力业务事实；由排班、薪资、合同或奖惩流程产生'
WHERE table_name LIKE 'schedule_%' OR table_name IN ('contract','month_salary','reward_punish','leave_record','overtime');
UPDATE system_table_registry SET business_domain='approval',data_kind='TRANSACTION',empty_policy='ALLOW_EMPTY',purpose='审批实例与轨迹；没有待办或历史审批时允许为空'
WHERE table_name LIKE 'approval_%';
UPDATE system_table_registry SET business_domain='reporting',data_kind='SNAPSHOT',empty_policy='ALLOW_EMPTY',purpose='报表快照或汇总；当前报表可实时计算，未结算时允许为空'
WHERE table_name LIKE 'report_%';
UPDATE system_table_registry SET business_domain='audit',data_kind='LOG',empty_policy='ALLOW_EMPTY',purpose='审计日志；仅由真实系统操作产生'
WHERE table_name LIKE '%log%' OR table_name LIKE 'audit_%';
UPDATE system_table_registry SET business_domain='legacy',data_kind='LEGACY',empty_policy='DEPRECATE',backend_binding='NONE',frontend_binding='NONE',purpose='历史兼容表；禁止新增数据，迁移确认后归档'
WHERE table_name IN ('admin_users','users','orders','package_details','package_dish_rel','pkg_used');
