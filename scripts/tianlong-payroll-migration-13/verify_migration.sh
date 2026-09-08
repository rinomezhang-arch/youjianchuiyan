#!/bin/bash
# TL-OPS-PAYROLL-MIGRATION-CANONICAL-13 隔离库迁移演练脚本（只读记录口径 + 可复现命令）
# 用途：固化唯一工资迁移脚本口径，在全新隔离 MySQL 演练并验证幂等与结构守恒。
# 边界：只连隔离 MySQL 13317，不碰生产 3306、不读真实工资明细。

set -euo pipefail

ISOLATED_HOST="127.0.0.1"
ISOLATED_PORT="13317"
MIGRATION="scripts/migrations/payroll_approval_payout_v1.sql"
FIXTURE="banquet_project/src/test/resources/payroll-metadata-fixture-20260907.sql"

echo "=== 唯一迁移口径固化结论 ==="
echo "正式口径脚本（唯一执行入口）: $MIGRATION"
echo "废弃口径（冲突候选，不保留第二执行入口）: db/migration/V20260908_01__payroll_payout_record.sql（Flyway V__ 命名，项目无 Flyway，不自动执行）"

echo ""
echo "=== 1. 全新隔离 schema 建库 + 执行迁移 ==="
SCHEMA="payroll_canonical_$(date +%s)"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" \
  -e "CREATE DATABASE $SCHEMA CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
echo "schema=$SCHEMA"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" "$SCHEMA" < "$FIXTURE"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" "$SCHEMA" < "$MIGRATION"
echo "迁移执行成功"

echo ""
echo "=== 2. 结构守恒断言 ==="
echo "--- month_salary 审批发放列 ---"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" -N -e \
  "USE $SCHEMA; SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.columns WHERE table_schema='$SCHEMA' AND table_name='month_salary' AND COLUMN_NAME IN ('post_salary_snapshot','attendance_pay_snapshot','approved_by','approved_at','paid_by','paid_at','payout_id') ORDER BY ORDINAL_POSITION;"
echo "--- payroll_payout_record 完整 DDL ---"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" -e "USE $SCHEMA; SHOW CREATE TABLE payroll_payout_record\G"
echo "--- month_salary 复合外键 ---"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" -N -e \
  "USE $SCHEMA; SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.key_column_usage WHERE table_schema='$SCHEMA' AND table_name='month_salary' AND referenced_table_name='payroll_payout_record';"

echo ""
echo "=== 3. 幂等性验证 ==="
echo "--- 重复执行迁移脚本(观察是否报错) ---"
mysql -uroot --protocol=tcp -h"$ISOLATED_HOST" -P"$ISOLATED_PORT" "$SCHEMA" < "$MIGRATION" \
  && echo "幂等OK：重复执行无报错" \
  || echo "非幂等：重复执行报错（month_salary ALTER 部分重复列，CREATE 部分 IF NOT EXISTS 幂等）"

echo ""
echo "=== 4. 集成测试 ==="
echo "mvn -f banquet_project/pom.xml -Dtest=PayrollMysqlIntegrationTest test"
echo "（本脚本不自动跑 mvn，集成测试由调用方执行并记录 surefire 数字）"
