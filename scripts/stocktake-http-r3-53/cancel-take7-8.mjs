#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-HTTP-R3-53 方案C（扩展）：标记 take_id=7、8 为 cancelled
 *
 * Codex 裁决：禁止 DELETE；全字段守卫；各影响行数必须恰好 1，否则 blocked。
 * 仅操作本任务自产行，不改脚本/不断言/不删数据。
 */
import { execFileSync } from 'node:child_process';

const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = '127.0.0.1', PORT = '13318', SCHEMA = 'tr37_stocktake_20260909';

const TARGETS = [
  { takeId: 7, takeNo: 'PD20260913003', storeId: 1, operatorId: 1, status: 'completed', remark: null },
  { takeId: 8, takeNo: 'PD20260913004', storeId: 1, operatorId: 1, status: 'completed', remark: null },
];

function run(sqlText) {
  return execFileSync(MYSQL,
    ['-h', HOST, '-P', PORT, '-u', 'root', '-N', '-B', SCHEMA, '-e', sqlText],
    { encoding: 'utf8' }).trim();
}
function rows(q) { const o = run(q); return o ? o.split(/\r?\n/).map(l => l.split('\t')) : []; }

// ---- 守卫 ----
const port = rows('SELECT @@port')[0][0];
if (String(port) !== PORT) { console.error('GUARD_FAIL port=' + port); process.exit(2); }
const dd = rows('SELECT @@datadir')[0][0];
if (!String(dd).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317')) {
  console.error('GUARD_FAIL datadir=' + dd); process.exit(2);
}

const report = [];
let allOk = true;

for (const t of TARGETS) {
  // 全字段守卫
  const m = rows(`SELECT take_id,take_no,remark,operator_id,status,store_id FROM stock_take WHERE take_id=${t.takeId}`);
  if (m.length !== 1) { console.error(`PRECOND_FAIL take=${t.takeId} rows=${m.length}`); process.exit(3); }
  const [tid, tno, rmk, op, st, sid] = m[0];
  const checks = [
    ['take_id', String(tid), String(t.takeId)],
    ['take_no', tno, t.takeNo],
    ['remark', rmk === 'NULL' ? null : rmk, t.remark],
    ['operator_id', String(op), String(t.operatorId)],
    ['status', st, t.status],
    ['store_id', String(sid), String(t.storeId)],
  ];
  const bad = checks.filter(([k, a, e]) => String(a) !== String(e));
  if (bad.length) {
    console.error(`PRECOND_FAIL take=${t.takeId} ` + JSON.stringify(bad));
    process.exit(3);
  }

  // 单事务标记（受影响必须恰好 1）
  const script = [
    'START TRANSACTION;',
    `UPDATE stock_take SET status='cancelled', remark=CONCAT(COALESCE(remark,''),' INVALID_FIXTURE')`,
    ` WHERE take_id=${t.takeId} AND take_no='${t.takeNo}' AND operator_id=${t.operatorId}`,
    `   AND store_id=${t.storeId} AND status='completed' AND remark IS NULL;`,
    'SELECT ROW_COUNT();',
    'COMMIT;',
  ].join('\n');
  const affected = Number(String(run(script)).split(/\r?\n/).filter(Boolean).pop());
  if (affected !== 1) {
    run('ROLLBACK;');
    console.error(`AFFECTED_FAIL take=${t.takeId} expected=1 actual=${affected}`);
    process.exit(4);
  }

  const after = rows(`SELECT take_id,status,remark FROM stock_take WHERE take_id=${t.takeId}`)[0];
  report.push({
    take_id: t.takeId, affected_rows: affected,
    status_after: after[1], remark_after: after[2],
  });
}

console.log(JSON.stringify({
  ok: allOk && report.every(r => r.affected_rows === 1),
  guard: { port: String(port), datadir: dd },
  marked: report,
  deleted: false,
}, null, 2));
