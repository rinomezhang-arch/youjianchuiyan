#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-PERSISTENCE-33 盘点单主从金额与落盘关系只读审计（R1 返工版）
 *
 * R1 修复点：
 * 1. 首次查询前锁死 host=127.0.0.1 / port=13318 / schema=tr37_stocktake_20260909；
 *    不匹配立即零查询退出（exit 2），不再从环境变量接受任意值。
 * 2. 每次查询在同一 MySQL 会话内使用 START TRANSACTION READ ONLY ... COMMIT。
 * 3. ingredient 非空判断改为 > 0；为 0 时标 NOT_COVERED，不算 PASS。
 *
 * 硬约束：只读；无任何写 SQL / DDL / 建 schema / 清理 / 停启库；未连 13317/3306。
 * 只输出聚合计数与合成标识；不输出员工顾客资料。
 */
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';

// ---- 锁死连接目标（常量，刻意不提供任何环境变量注入点）----
const HOST = '127.0.0.1';
const PORT = '13318';
const SCHEMA = 'tr37_stocktake_20260909';
const EXPECT_DATADIR_HINT = 'mysql-test-13317';
const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
// 输出路径允许指定；其余连接参数一律不可被环境变量覆盖。
const OUT = process.env.MX_OUT || '';

// 显式记录并忽略任何试图影响连接目标的注入（R1 第1项：连接前锁死）。
// R2 修正：只记录被忽略的变量名，绝不记录其值（值可能含路径或敏感信息）。
const INJECTION_ENV_KEYS = ['MX_DB_HOST', 'MX_DB_PORT', 'MX_DB_SCHEMA', 'MX_MYSQL_CLI', 'MYSQL_HOST', 'MYSQL_TCP_PORT'];
const ignoredInjectionKeys = INJECTION_ENV_KEYS.filter(k => process.env[k] !== undefined);

// 真实查询计数（R2 修正：不再固定报 0）
let queryCount = 0;

// 只在同一会话内执行：SET read-only + START TRANSACTION READ ONLY -> 查询 -> COMMIT
// 通过把多条语句写进一个 mysql -e 调用，保证同一连接/会话。
function sessionQuery(sql) {
  const wrapped = [
    'SET SESSION TRANSACTION READ ONLY;',
    'START TRANSACTION READ ONLY;',
    sql.endsWith(';') ? sql : sql + ';',
    'COMMIT;',
  ].join('\n');
  queryCount++;  // R2: 真实计数，每个查询（含守卫）都计
  const out = execFileSync(
    MYSQL,
    ['-h', HOST, '-P', PORT, '-u', 'root', SCHEMA, '-N', '-B', '--batch', '--raw', '-e', wrapped],
    { encoding: 'utf8' }
  );
  const trimmed = out.trim();
  if (!trimmed) return [];
  // COMMIT 无输出；按行切分，跳过空行
  return trimmed.split(/\r?\n/).filter(l => l.length > 0).map(l => l.split('\t'));
}
function scalar(q) { const r = sessionQuery(q); return r.length ? r[0][0] : null; }
function num(q) { const v = scalar(q); return v === null ? null : Number(v); }

const results = [];
let pass = 0, fail = 0, notCovered = 0;
const anomalies = [];
function assert(name, cond, detail) {
  if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
  else { fail++; anomalies.push(name); results.push({ name, status: 'FAIL', detail }); }
}
function notCoveredAssert(name, detail) { notCovered++; results.push({ name, status: 'NOT_COVERED', detail }); }
function infoAssert(name, detail) { results.push({ name, status: 'INFO', detail }); }
function hardExit(reason, detail) {
  // R2: 使用真实查询计数，不再固定报 0
  const payload = { phase: 'preflight', reason, detail, queries_executed: queryCount };
  console.error('PREFLIGHT_FAIL=' + JSON.stringify(payload));
  process.exit(2);
}

// ---------- 0. 首次查询前锁死目标 ----------
// 唯一允许的第一条查询：确认当前连接落在期望实例与 schema 上。
{
  // 先确认连接目标（这三条是本会话的第一批语句，不做任何业务查询前先校验）
  const port = scalar('SELECT @@port');
  if (String(port) !== PORT) hardExit('port-mismatch', `expected=${PORT} actual=${port}`);
  const datadir = scalar('SELECT @@datadir');
  if (!String(datadir).replace(/\\/g, '/').toLowerCase().includes(EXPECT_DATADIR_HINT)) {
    hardExit('datadir-mismatch', `expected hint=${EXPECT_DATADIR_HINT} actual=${datadir}`);
  }
  const schemaN = scalar(`SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${SCHEMA}'`);
  if (Number(schemaN) !== 1) hardExit('schema-missing', `schema=${SCHEMA} found=${schemaN}`);
  // 确认当前默认库就是目标 schema
  const curDb = scalar('SELECT DATABASE()');
  if (String(curDb) !== SCHEMA) hardExit('default-schema-mismatch', `expected=${SCHEMA} actual=${curDb}`);

  assert('guard.port-is-13318', String(port) === '13318', `@@port=${port}`);
  assert('guard.datadir-isolated', true, `@@datadir=${datadir}`);
  assert('guard.schema-exists', Number(schemaN) === 1, `schema=${SCHEMA}`);
  assert('guard.default-schema', String(curDb) === SCHEMA, `DATABASE()=${curDb}`);
  // R1 第1项：连接参数常量硬编码，任何注入尝试均被忽略（连接仍落在锁死目标）
  // R2 修正：只记变量名，不记值
  assert('guard.no-env-injection', true,
    ignoredInjectionKeys.length
      ? `ignored_keys=${JSON.stringify(ignoredInjectionKeys)}; effective=${HOST}:${PORT}/${SCHEMA}`
      : `no-injection-attempted; effective=${HOST}:${PORT}/${SCHEMA}`);
}

// ---------- 1. 非空前置 ----------
{
  const masterN = num('SELECT COUNT(*) FROM stock_take');
  const detailN = num('SELECT COUNT(*) FROM stock_take_detail');
  assert('nonempty.stock_take', masterN > 0, `rows=${masterN}`);
  assert('nonempty.stock_take_detail', detailN > 0, `rows=${detailN}`);
  infoAssert('counts', `stock_take=${masterN} stock_take_detail=${detailN}`);
}

// ---------- 2. 明细 -> 主单 take_id + store_id 一致 ----------
{
  const orphan = num(`SELECT COUNT(*) FROM stock_take_detail d
    LEFT JOIN stock_take t ON d.take_id=t.take_id WHERE t.take_id IS NULL`);
  const crossStore = num(`SELECT COUNT(*) FROM stock_take_detail d
    JOIN stock_take t ON d.take_id=t.take_id WHERE t.store_id<>d.store_id`);
  assert('detail.orphan-master', orphan === 0, `orphans=${orphan} (必须为0)`);
  assert('detail.cross-store', crossStore === 0, `crossStore=${crossStore} (必须为0)`);
}

// ---------- 3. ingredient_id -> ingredient_master（R1 修复：非空判断 > 0，为0则 NOT_COVERED）----------
{
  const total = num('SELECT COUNT(*) FROM stock_take_detail');
  const nonEmpty = num(`SELECT COUNT(*) FROM stock_take_detail WHERE ingredient_id IS NOT NULL AND ingredient_id<>''`);
  if (nonEmpty > 0) {
    const orphan = num(`SELECT COUNT(*) FROM stock_take_detail d
      LEFT JOIN ingredient_master m ON d.ingredient_id=m.ingredient_id AND d.store_id=m.store_id
      WHERE d.ingredient_id IS NOT NULL AND d.ingredient_id<>'' AND m.ingredient_id IS NULL`);
    assert('ingredient.nonempty-id', nonEmpty > 0, `nonEmpty=${nonEmpty} (必须>0)`);
    assert('ingredient.orphan', orphan === 0, `orphans=${orphan} (必须为0)`);
  } else {
    notCoveredAssert('ingredient.nonempty-id', `nonEmpty=0 / total=${total}：无 ingredient_id 可核，NOT_COVERED`);
    notCoveredAssert('ingredient.orphan', `nonEmpty=0：孤儿关系无样本，NOT_COVERED`);
  }
}

// ---------- 4. 主单汇总 = 明细聚合 ----------
{
  const badItems = num(`SELECT COUNT(*) FROM (
    SELECT t.take_id FROM stock_take t
    LEFT JOIN stock_take_detail d ON d.take_id=t.take_id
    GROUP BY t.take_id, t.total_items
    HAVING t.total_items IS NULL OR t.total_items <> COUNT(d.detail_id)) x`);
  const badDiffItems = num(`SELECT COUNT(*) FROM (
    SELECT t.take_id, t.total_diff_items, COUNT(CASE WHEN d.diff_quantity<>0 THEN 1 END) c
    FROM stock_take t LEFT JOIN stock_take_detail d ON d.take_id=t.take_id
    GROUP BY t.take_id, t.total_diff_items
    HAVING t.total_diff_items IS NULL OR t.total_diff_items <> c) x`);
  const badDiffAmount = num(`SELECT COUNT(*) FROM (
    SELECT t.take_id, t.total_diff_amount, COALESCE(SUM(d.diff_amount),0) s
    FROM stock_take t LEFT JOIN stock_take_detail d ON d.take_id=t.take_id
    GROUP BY t.take_id, t.total_diff_amount
    HAVING t.total_diff_amount IS NULL OR ABS(t.total_diff_amount - s) > 0.005) x`);
  assert('rollup.total-items', badItems === 0, `mismatch=${badItems} (必须为0)`);
  assert('rollup.total-diff-items', badDiffItems === 0, `mismatch=${badDiffItems} (必须为0)`);
  assert('rollup.total-diff-amount', badDiffAmount === 0, `mismatch=${badDiffAmount} (必须为0)`);
}

// ---------- 5. 明细公式 ----------
{
  const badDiffQty = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_quantity IS NULL OR ABS(diff_quantity - (actual_quantity - system_quantity)) > 0.0005`);
  const badSystemAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE system_amount IS NOT NULL AND unit_price IS NOT NULL AND system_quantity IS NOT NULL
      AND system_amount <> ROUND(system_quantity*unit_price, 2)`);
  const badActualAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE actual_amount IS NOT NULL AND unit_price IS NOT NULL AND actual_quantity IS NOT NULL
      AND actual_amount <> ROUND(actual_quantity*unit_price, 2)`);
  const badDiffAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_amount IS NOT NULL AND unit_price IS NOT NULL AND diff_quantity IS NOT NULL
      AND diff_amount <> ROUND(diff_quantity*unit_price, 2)`);
  const badType = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_quantity IS NOT NULL AND diff_type IS NOT NULL AND (
      (diff_quantity < 0 AND diff_type NOT IN ('shortage')) OR
      (diff_quantity > 0 AND diff_type NOT IN ('overage','surplus')) OR
      (diff_quantity = 0 AND diff_type NOT IN ('equal','none','')) )`);
  assert('formula.diff-quantity', badDiffQty === 0, `mismatch=${badDiffQty} (必须为0)`);
  assert('formula.system-amount', badSystemAmt === 0, `mismatch=${badSystemAmt} (必须为0)`);
  assert('formula.actual-amount', badActualAmt === 0, `mismatch=${badActualAmt} (必须为0)`);
  assert('formula.diff-amount', badDiffAmt === 0, `mismatch=${badDiffAmt} (必须为0)`);
  assert('formula.diff-type-sign', badType === 0, `mismatch=${badType} (必须为0)`);
}

// ---------- 6. completed 必须有 finish_time ----------
{
  const missingFinish = num(`SELECT COUNT(*) FROM stock_take WHERE status='completed' AND finish_time IS NULL`);
  assert('status.completed-has-finish-time', missingFinish === 0, `missing=${missingFinish} (必须为0)`);
}

// ---------- 7. 重复项 ----------
{
  const dupLine = num(`SELECT COUNT(*) FROM (
    SELECT take_id, line_no, COUNT(*) c FROM stock_take_detail
    GROUP BY take_id, line_no HAVING c>1) x`);
  const dupIng = num(`SELECT COUNT(*) FROM (
    SELECT take_id, ingredient_id, COUNT(*) c FROM stock_take_detail
    WHERE ingredient_id IS NOT NULL AND ingredient_id<>''
    GROUP BY take_id, ingredient_id HAVING c>1) x`);
  assert('dup.line-no', dupLine === 0, `dupPairs=${dupLine} (必须为0)`);
  assert('dup.ingredient-id', dupIng === 0, `dupPairs=${dupIng} (必须为0)`);
}

// ---------- 8. 约束元数据（INFO）----------
{
  const tables = ['stock_take', 'stock_take_detail', 'ingredient_master'];
  const inList = tables.map(t => `'${t}'`).join(',');
  const fks = sessionQuery(`SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
     FROM information_schema.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA='${SCHEMA}' AND REFERENCED_TABLE_NAME IS NOT NULL AND TABLE_NAME IN (${inList})
     ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION`);
  const uqs = sessionQuery(`SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA='${SCHEMA}' AND NON_UNIQUE=0 AND TABLE_NAME IN (${inList})
     GROUP BY TABLE_NAME, INDEX_NAME ORDER BY TABLE_NAME, INDEX_NAME`);
  infoAssert('meta.fk-count', `fkCount=${fks.length}`);
  infoAssert('meta.unique-index-count', `uniqueIndexCount=${uqs.length}`);
  infoAssert('meta.foreign-keys', fks.map(r => `${r[0]}.${r[1]}:${r[2]}->${r[3]}.${r[4]}`).join('; ') || 'none');
  infoAssert('meta.unique-indexes', uqs.map(r => `${r[0]}.${r[1]}(${r[2]})`).join('; ') || 'none');
}

const infoCount = results.filter(r => r.status === 'INFO').length;
console.log('\n==== DL-RC-STOCKTAKE-PERSISTENCE-33 只读盘点落盘审计 (R2) ====');
for (const r of results) console.log(`${r.status}\t${r.name}\t${r.detail}`);
console.log(`\nTOTAL=${results.length} PASS=${pass} FAIL=${fail} NOT_COVERED=${notCovered} INFO=${infoCount} ANOMALIES=${anomalies.length}`);
console.log(`QUERIES_EXECUTED=${queryCount}`);
if (anomalies.length) console.log('ANOMALY_NAMES=' + anomalies.join(','));

if (OUT) {
  fs.writeFileSync(OUT, JSON.stringify({
    task: 'DL-RC-STOCKTAKE-PERSISTENCE-33',
    revision: 'R2',
    connection: { host: HOST, port: PORT, schema: SCHEMA },
    readonly_transaction: true,
    queries_executed: queryCount,
    ignored_injection_keys: ignoredInjectionKeys,
    total: results.length, pass, fail, not_covered: notCovered, info: infoCount,
    anomalies: anomalies.length, anomaly_names: anomalies,
    generated_at: new Date().toISOString(),
    results,
  }, null, 2), 'utf8');
}
process.exit(fail === 0 ? 0 : 1);
