#!/bin/bash
# TL-OPS-PAYROLL-MIGRATION-CANONICAL-13 隔离库迁移演练脚本（自愈版，遇失败非零退出）
# 用途：固化唯一工资迁移脚本口径，三态演练 + 幂等 + 自愈 + 结构一致 + 集成测试核验。
# 边界：只连隔离 MySQL 13317，不碰生产 3306、不读真实工资明细。
set -euo pipefail

ISOLATED_HOST="127.0.0.1"
ISOLATED_PORT="13317"
MIGRATION="scripts/migrations/payroll_approval_payout_v1.sql"
FIXTURE="banquet_project/src/test/resources/payroll-metadata-fixture-20260907.sql"
RETIRED="banquet_project/src/main/resources/db/migration/V20260908_01__payroll_payout_record.sql.retired"

MYSQL="mysql -uroot --protocol=tcp -h$ISOLATED_HOST -P$ISOLATED_PORT"

echo "=== 唯一迁移口径固化结论 ==="
echo "正式口径脚本（唯一执行入口）: $MIGRATION"
echo "废弃口径（已迁出执行目录，.retired 不可执行）: $RETIRED"

dump_struct() {
  local s=$1
  $MYSQL -N -e "USE $s; SHOW CREATE TABLE payroll_payout_record\G" 2>&1
  $MYSQL -N -e "USE $s; SELECT GROUP_CONCAT(CONCAT(COLUMN_NAME,':',COLUMN_TYPE,':',IS_NULLABLE) ORDER BY ORDINAL_POSITION SEPARATOR '|') FROM information_schema.columns WHERE table_schema='$s' AND table_name='month_salary' AND COLUMN_NAME IN ('post_salary_snapshot','attendance_pay_snapshot','approved_by','approved_at','paid_by','paid_at','payout_id');" 2>&1
  $MYSQL -N -e "USE $s; SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.statistics WHERE table_schema='$s' AND table_name='payroll_payout_record' GROUP BY INDEX_NAME, NON_UNIQUE ORDER BY INDEX_NAME;" 2>&1
  $MYSQL -N -e "USE $s; SELECT CONSTRAINT_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ','), REFERENCED_TABLE_NAME, GROUP_CONCAT(REFERENCED_COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',') FROM information_schema.key_column_usage WHERE table_schema='$s' AND referenced_table_name IS NOT NULL AND table_name IN ('payroll_payout_record','month_salary') GROUP BY CONSTRAINT_NAME, REFERENCED_TABLE_NAME ORDER BY CONSTRAINT_NAME;" 2>&1
}

echo ""
echo "=== 态1：全新库首次迁移 ==="
S1="payroll_canonical_s1_$(date +%s)"
$MYSQL -e "CREATE DATABASE $S1 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
$MYSQL "$S1" < "$FIXTURE"
$MYSQL "$S1" < "$MIGRATION"
echo "态1首次迁移退出码: 0"
dump_struct "$S1" > /tmp/canonical_s1.txt

echo ""
echo "=== 态2：同一 schema 重复执行（必须退出0） ==="
$MYSQL "$S1" < "$MIGRATION" && echo "态2第二次退出码: 0" || { echo "态2第二次失败(退出非0)"; exit 1; }
$MYSQL "$S1" < "$MIGRATION" && echo "态2第三次退出码: 0" || { echo "态2第三次失败(退出非0)"; exit 1; }
dump_struct "$S1" > /tmp/canonical_s2.txt

echo ""
echo "=== 态3：旧冲突候选表已存在（自愈） ==="
S3="payroll_canonical_s3_$(date +%s)"
$MYSQL -e "CREATE DATABASE $S3 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
$MYSQL "$S3" < "$FIXTURE"
$MYSQL "$S3" < "$RETIRED"
echo "旧冲突表已建（用 retired 旧 DDL）"
dump_struct "$S3" > /tmp/canonical_s3_before.txt
$MYSQL "$S3" < "$MIGRATION" && echo "态3自愈迁移退出码: 0" || { echo "态3自愈失败(退出非0)"; exit 1; }
dump_struct "$S3" > /tmp/canonical_s3_after.txt

echo ""
echo "=== 三态结构一致性断言 ==="
diff /tmp/canonical_s1.txt /tmp/canonical_s2.txt >/dev/null 2>&1 && echo "态1=态2: 一致" || { echo "态1 vs 态2 不一致，失败"; diff /tmp/canonical_s1.txt /tmp/canonical_s2.txt; exit 1; }
diff /tmp/canonical_s1.txt /tmp/canonical_s3_after.txt >/dev/null 2>&1 && echo "态1=态3(自愈后): 一致" || { echo "态1 vs 态3 不一致，失败"; diff /tmp/canonical_s1.txt /tmp/canonical_s3_after.txt; exit 1; }

echo ""
echo "=== 集成测试（工资审批→发放闭环，隔离库真实 JDBC + 事务） ==="
cd banquet_project
mvn -B -q test -Dtest=PayrollMysqlIntegrationTest 2>&1 | tail -5
TEST_EXIT=$?
cd ..
if [ "$TEST_EXIT" -ne 0 ]; then
  echo "集成测试失败(退出码 $TEST_EXIT)，脚本非零退出"
  exit 1
fi

echo ""
echo "=== 核验 Surefire XML 测试数字 ==="
SUREFIRE="banquet_project/target/surefire-reports/com.youjian.banquet.service.PayrollMysqlIntegrationTest.txt"
if [ -f "$SUREFIRE" ]; then
  grep -H "Tests run" "$SUREFIRE"
  # 解析数字：Tests run / Failures / Errors / Skipped
  RUNS=$(grep -oE "Tests run: [0-9]+" "$SUREFIRE" | grep -oE "[0-9]+")
  FAILS=$(grep -oE "Failures: [0-9]+" "$SUREFIRE" | grep -oE "[0-9]+")
  ERRS=$(grep -oE "Errors: [0-9]+" "$SUREFIRE" | grep -oE "[0-9]+")
  SKIPS=$(grep -oE "Skipped: [0-9]+" "$SUREFIRE" | grep -oE "[0-9]+")
  echo "解析结果: run=$RUNS fail=$FAILS error=$ERRS skip=$SKIPS"
  # 断言：25 通过 0 失败 0 错误 0 跳过
  if [ "$RUNS" -ne 25 ] || [ "$FAILS" -ne 0 ] || [ "$ERRS" -ne 0 ] || [ "$SKIPS" -ne 0 ]; then
    echo "测试数字不符预期(25/0/0/0)，失败"
    exit 1
  fi
  echo "核验通过: Tests run 25, Failures 0, Errors 0, Skipped 0"
else
  echo "Surefire 报告不存在，失败"
  exit 1
fi

echo ""
echo "=== 全部通过 ==="
echo "schema_s1=$S1 schema_s3=$S3"
echo "$S1 $S3" > /tmp/canonical_schemas.txt
exit 0
