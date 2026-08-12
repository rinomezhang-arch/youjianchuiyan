-- ======================================================================
-- 又见炊烟餐饮管理系统 - P1-15 薪资字段独立迁移脚本
-- 对应审计报告 P1-15：staff_master 薪资字段独立到 month_salary 表
-- 执行方式: docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet < salary_migration_v1.sql
-- 特性: 幂等（可重复执行，已存在的 month_salary 记录自动跳过）
-- ======================================================================

-- ------------------------------------------------------------------
-- 阶段1：将 staff_master 薪资明细字段迁移到 month_salary 表
--   映射关系：
--     staff_master.basic_salary        → month_salary.base_salary
--     staff_master.performance_salary  → month_salary.performance_salary
--     staff_master.subsidy             → month_salary.other_allowance
--     staff_master.bonus               → month_salary.reward_amount
--     staff_master.social_insurance    → month_salary.social_security_deduction
--     staff_master.housing_fund        → month_salary.housing_fund_deduction
--     staff_master.monthly_salary      → month_salary.base_salary (兜底，当 basic_salary 为空时)
--   迁移目标月份：当前月（YYYY-MM）
-- ------------------------------------------------------------------

-- 取当前月份（仅取 YYYY-MM，避免存储过程复杂度）
SET @salary_month := DATE_FORMAT(NOW(), '%Y-%m');

-- 幂等插入：仅当 (staff_id, salary_month) 不存在时才插入
INSERT INTO month_salary (
    store_id, staff_id, salary_month,
    base_salary, performance_salary, reward_amount,
    social_security_deduction, housing_fund_deduction, other_allowance,
    gross_salary, net_salary, tax_amount, status, remark
)
SELECT
    s.store_id,
    s.staff_id,
    @salary_month,
    COALESCE(s.basic_salary, s.monthly_salary, 0)                    AS base_salary,
    COALESCE(s.performance_salary, 0)                               AS performance_salary,
    COALESCE(s.bonus, 0)                                            AS reward_amount,
    COALESCE(s.social_insurance, 0)                                 AS social_security_deduction,
    COALESCE(s.housing_fund, 0)                                     AS housing_fund_deduction,
    COALESCE(s.subsidy, 0)                                          AS other_allowance,
    -- gross_salary = base + performance + reward + allowance
    COALESCE(s.basic_salary, s.monthly_salary, 0)
        + COALESCE(s.performance_salary, 0)
        + COALESCE(s.bonus, 0)
        + COALESCE(s.subsidy, 0)                                    AS gross_salary,
    -- net_salary = gross - social_security - housing_fund
    COALESCE(s.basic_salary, s.monthly_salary, 0)
        + COALESCE(s.performance_salary, 0)
        + COALESCE(s.bonus, 0)
        + COALESCE(s.subsidy, 0)
        - COALESCE(s.social_insurance, 0)
        - COALESCE(s.housing_fund, 0)                               AS net_salary,
    0                                                                AS tax_amount,
    0                                                                AS status,
    'P1-15迁移：从staff_master薪资明细字段迁入'
FROM staff_master s
WHERE s.employment_status IS NULL
   OR s.employment_status NOT IN ('resigned', '离职', '已离职')
  AND NOT EXISTS (
      SELECT 1 FROM month_salary m
      WHERE m.staff_id = s.staff_id
        AND m.salary_month = @salary_month
  );

-- ------------------------------------------------------------------
-- 阶段2：标记 staff_master 薪资明细字段为弃用（修改字段注释）
--   字段保留以兼容旧版 PayrollController，但注释标注已迁出
--   staff_master.monthly_salary 保留作为汇总字段（由 month_salary 自动汇总）
-- ------------------------------------------------------------------

ALTER TABLE staff_master
    MODIFY COLUMN basic_salary DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.base_salary，保留兼容旧版读取';

ALTER TABLE staff_master
    MODIFY COLUMN performance_salary DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.performance_salary';

ALTER TABLE staff_master
    MODIFY COLUMN subsidy DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.other_allowance';

ALTER TABLE staff_master
    MODIFY COLUMN bonus DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.reward_amount';

ALTER TABLE staff_master
    MODIFY COLUMN social_insurance DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.social_security_deduction';

ALTER TABLE staff_master
    MODIFY COLUMN housing_fund DECIMAL(12,2) DEFAULT NULL
    COMMENT '[已弃用-P1-15] 已迁入 month_salary.housing_fund_deduction';

-- ------------------------------------------------------------------
-- 阶段3：验证迁移结果
-- ------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM staff_master WHERE employment_status NOT IN ('resigned','离职','已离职') OR employment_status IS NULL) AS active_staff_count,
    (SELECT COUNT(*) FROM month_salary WHERE salary_month = DATE_FORMAT(NOW(), '%Y-%m')) AS migrated_salary_count,
    (SELECT COUNT(*) FROM month_salary) AS total_salary_records;

-- ======================================================================
-- 迁移完成说明：
-- 1. staff_master 的 basic_salary/performance_salary/subsidy/bonus/
--    social_insurance/housing_fund 字段已标记弃用，数据已迁入 month_salary
-- 2. staff_master.monthly_salary 保留作为汇总字段
-- 3. PayrollController 应改为从 month_salary 读取薪资明细
-- 4. StaffService 新增 getMonthSalary/upsertMonthSalary 方法
-- ======================================================================
