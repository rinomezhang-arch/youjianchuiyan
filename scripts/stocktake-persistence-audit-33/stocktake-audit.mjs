#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-PERSISTENCE-33 盘点单主从金额与落盘关系只读审计
 *
 * 硬约束（任务卡）：
 * - 只连 127.0.0.1:13318 的 tr37_stocktake_20260909；查前断言 @@port=13318 且 @@datadir 隔离目录
 * - 全程只读：无任何写 SQL / DDL / 建 schema / 清理 / 停启库
 * - 主表或明细空则 NOT_COVERED，不得算 PASS
 * - 异常计数必须严格为 0 才 PASS；外键索引只记 INFO
 * - 只输出聚合计数与合成标识；不输出员工/顾客资料
 */
import { execFileSync } from 'node:child_process';

const MYSQL = process.env.MX_MYSQL_CLI || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = process.env.MX_DB_HOST || '127.0.0.1';
const PORT = String(process.env.MX_DB_PORT || '');
const DB = process.env.MX_DB_SCHEMA || 'tr37_stocktake_20260909';
const OUT = process.env.MX_OUT || '';

if (!PORT) throw new Error('缺少 MX_DB_PORT：端口必须显式指定');
if (['3306', '13317'].includes(PORT)) throw new Error(`拒绝连接端口 ${PORT}`);

const results = [];
let pass = 0, fail = 0, notCovered = 0;
const anomalies = [];
function assert(name, cond, detail) {
  if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
  else { fail++; anomalies.push(name); results.push({ name, status: 'FAIL', detail }); }
}
function notCoveredAssert(name, detail) { notCovered++; results.push({ name, status: 'NOT_COVERED', detail }); }
function infoAssert(name, detail) { results.push({ name, status: 'INFO', detail }); }

function rows(q) {
  const out = execFileSync(MYSQL, ['-h', HOST, '-P', PORT, '-u', 'root', DB, '-N', '-B', '-e', q], { encoding: 'utf8' }).trim();
  if (!out) return [];
  return out.split(/\r?\n/).map(l => l.split('\t'));
}
function scalar(q) { const r = rows(q); return r.length ? r[0][0] : null; }
function num(q) { const v = scalar(q); return v === null ? null : Number(v); }

// ---------- 0. 环境守卫 + 非空前置 ----------
{
  const port = scalar('SELECT @@port');
  const datadir = scalar('SELECT @@datadir');
  assert('guard.port-is-13318', String(port) === '13318', `@@port=${port}`);
  assert('guard.datadir-isolated', String(datadir).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317'), `@@datadir=${datadir}`);
  const schemaOk = Number(scalar(`SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${DB}'`));
  assert('guard.schema-exists', schemaOk === 1, `schema=${DB} exists=${schemaOk}`);

  const masterN = num(`SELECT COUNT(*) FROM stock_take`);
  const detailN = num(`SELECT COUNT(*) FROM stock_take_detail`);
  assert('nonempty.stock_take', masterN > 0, `rows=${masterN}`);
  assert('nonempty.stock_take_detail', detailN > 0, `rows=${detailN}`);
  results.push({ name: 'counts', status: 'INFO', detail: `stock_take=${masterN} stock_take_detail=${detailN}` });
}

// ---------- 1. 明细 -> 主单 take_id + store_id 一致 ----------
{
  const orphan = num(`SELECT COUNT(*) FROM stock_take_detail d
    LEFT JOIN stock_take t ON d.take_id=t.take_id WHERE t.take_id IS NULL`);
  const crossStore = num(`SELECT COUNT(*) FROM stock_take_detail d
    JOIN stock_take t ON d.take_id=t.take_id WHERE t.store_id<>d.store_id`);
  assert('detail.orphan-master', orphan === 0, `orphans=${orphan} (必须为0)`);
  assert('detail.cross-store', crossStore === 0, `crossStore=${crossStore} (必须为0)`);
}

// ---------- 2. ingredient_id -> ingredient_master (ingredient_id+store_id) ----------
{
  const nonEmpty = num(`SELECT COUNT(*) FROM stock_take_detail WHERE ingredient_id IS NOT NULL AND ingredient_id<>''`);
  const orphan = num(`SELECT COUNT(*) FROM stock_take_detail d
    LEFT JOIN ingredient_master m ON d.ingredient_id=m.ingredient_id AND d.store_id=m.store_id
    WHERE d.ingredient_id IS NOT NULL AND d.ingredient_id<>'' AND m.ingredient_id IS NULL`);
  assert('ingredient.nonempty-id', nonEmpty !== null, `nonEmpty=${nonEmpty}`);
  assert('ingredient.orphan', orphan === 0, `orphans=${orphan} (必须为0)`);
}

// ---------- 3. 主单汇总 = 明细聚合 ----------
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
  assert('rollup.total-diff-items', badDiffItems === 0, `mismatch=${badDiffItems} (diff_quantity 非零数，必须为0)`);
  assert('rollup.total-diff-amount', badDiffAmount === 0, `mismatch=${badDiffAmount} (diff_amount 之和，必须为0)`);
}

// ---------- 4. 明细公式：diff/amount/type ----------
{
  const badDiffQty = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_quantity IS NULL OR ABS(diff_quantity - (actual_quantity - system_quantity)) > 0.0005`);
  // system/actual/diff amount 按 unit_price HALF_UP 两位
  const badSystemAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE system_amount IS NOT NULL AND unit_price IS NOT NULL AND system_quantity IS NOT NULL
      AND system_amount <> ROUND(system_quantity*unit_price, 2)`);
  const badActualAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE actual_amount IS NOT NULL AND unit_price IS NOT NULL AND actual_quantity IS NOT NULL
      AND actual_amount <> ROUND(actual_quantity*unit_price, 2)`);
  const badDiffAmt = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_amount IS NOT NULL AND unit_price IS NOT NULL AND diff_quantity IS NOT NULL
      AND diff_amount <> ROUND(diff_quantity*unit_price, 2)`);
  // diff_type 与正负号一致
  const badType = num(`SELECT COUNT(*) FROM stock_take_detail
    WHERE diff_quantity IS NOT NULL AND diff_type IS NOT NULL AND (
      (diff_quantity < 0 AND diff_type NOT IN ('shortage')) OR
      (diff_quantity > 0 AND diff_type NOT IN ('overage','surplus')) OR
      (diff_quantity = 0 AND diff_type NOT IN ('equal','none','')) )`);
  assert('formula.diff-quantity', badDiffQty === 0, `mismatch=${badDiffQty} (必须为0)`);
  assert('formula.system-amount', badSystemAmt === 0, `mismatch=${badSystemAmt} (HALF_UP两位，必须为0)`);
  assert('formula.actual-amount', badActualAmt === 0, `mismatch=${badActualAmt} (必须为0)`);
  assert('formula.diff-amount', badDiffAmt === 0, `mismatch=${badDiffAmt} (必须为0)`);
  assert('formula.diff-type-sign', badType === 0, `mismatch=${badType} (与正负号一致，必须为0)`);
}

// ---------- 5. completed 主单必须有 finish_time ----------
{
  const missingFinish = num(`SELECT COUNT(*) FROM stock_take WHERE status='completed' AND finish_time IS NULL`);
  assert('status.completed-has-finish-time', missingFinish === 0, `missing=${missingFinish} (必须为0)`);
}

// ---------- 6. 同 take_id 下 line_no / ingredient_id 不得重复 ----------
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

// ---------- 7. 外键/唯一索引元数据（INFO） ----------
{
  const tables = ['stock_take', 'stock_take_detail', 'ingredient_master'];
  const inList = tables.map(t => `'${t}'`).join(',');
  const fks = rows(`SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
     FROM information_schema.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA='${DB}' AND REFERENCED_TABLE_NAME IS NOT NULL AND TABLE_NAME IN (${inList})
     ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION`);
  const uqs = rows(`SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA='${DB}' AND NON_UNIQUE=0 AND TABLE_NAME IN (${inList})
     GROUP BY TABLE_NAME, INDEX_NAME ORDER BY TABLE_NAME, INDEX_NAME`);
  infoAssert('meta.fk-count', `fkCount=${fks.length}`);
  infoAssert('meta.unique-index-count', `uniqueIndexCount=${uqs.length}`);
  infoAssert('meta.foreign-keys', fks.map(r => `${r[0]}.${r[1]}:${r[2]}->${r[3]}.${r[4]}`).join('; ') || 'none');
  infoAssert('meta.unique-indexes', uqs.map(r => `${r[0]}.${r[1]}(${r[2]})`).join('; ') || 'none');
}

console.log('\n==== DL-RC-STOCKTAKE-PERSISTENCE-33 只读盘点落盘审计 ====');
for (const r of results) console.log(`${r.status}\t${r.name}\t${r.detail}`);
const infoCount = results.filter(r => r.status === 'INFO').length;
console.log(`\nTOTAL=${results.length} PASS=${pass} FAIL=${fail} NOT_COVERED=${notCovered} INFO=${infoCount} ANOMALIES=${anomalies.length}`);
if (anomalies.length) console.log('ANOMALY_NAMES=' + anomalies.join(','));

if (OUT) {
  const fs = await import('node:fs');
  fs.writeFileSync(OUT, JSON.stringify({
    task: 'DL-RC-STOCKTAKE-PERSISTENCE-33', db_port: PORT, schema: DB,
    total: results.length, pass, fail, not_covered: notCovered, info: infoCount,
    anomalies: anomalies.length, anomaly_names: anomalies, results,
  }, null, 2), 'utf8');
}
process.exit(fail === 0 ? 0 : 1);
