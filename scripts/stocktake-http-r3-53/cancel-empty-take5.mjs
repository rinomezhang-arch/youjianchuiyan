#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-HTTP-R3-53 方案C：将空单 take_id=5 标记为 cancelled
 *
 * 严格约束（Codex 裁决）：
 * - 禁止 DELETE，保留审计痕迹
 * - 仅在 take_id=5 且 take_no/remark/operator/零明细/当前completed 全匹配时执行
 * - 单事务：status -> cancelled，remark 追加 INVALID_FIXTURE
 * - 受影响行数必须恰好 1；不符立即回滚并按失败处理
 */
import { execFileSync } from 'node:child_process';

const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = '127.0.0.1', PORT = '13318', SCHEMA = 'tr37_stocktake_20260909';

const EXPECT = {
  takeId: 5,
  takeNo: 'PD20260913001',
  remark: 'DL53-dl53-abca3d',
  operatorId: 1,
  status: 'completed',
  zeroDetails: true,
};

function run(sqlText, { db = SCHEMA } = {}) {
  const args = ['-h', HOST, '-P', PORT, '-u', 'root', '-N', '-B'];
  if (db) args.push(db);
  args.push('-e', sqlText);
  const out = execFileSync(MYSQL, args, { encoding: 'utf8' });
  return out.trim();
}
function rows(q) {
  const o = run(q);
  return o ? o.split(/\r?\n/).map(l => l.split('\t')) : [];
}

// ---- 守卫 ----
const port = rows('SELECT @@port')[0][0];
if (String(port) !== PORT) { console.error('GUARD_FAIL port=' + port); process.exit(2); }
const dd = rows('SELECT @@datadir')[0][0];
if (!String(dd).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317')) {
  console.error('GUARD_FAIL datadir=' + dd); process.exit(2);
}

// ---- 全匹配断言 ----
const m = rows(`SELECT take_id,take_no,remark,operator_id,status FROM stock_take WHERE take_id=${EXPECT.takeId}`);
if (m.length !== 1) { console.error('PRECOND_FAIL rows=' + m.length); process.exit(3); }
const [tid, tno, rmk, op, st] = m[0];
const dcnt = Number(rows(`SELECT COUNT(*) FROM stock_take_detail WHERE take_id=${EXPECT.takeId}`)[0][0]);

const checks = [
  ['take_id', String(tid), String(EXPECT.takeId)],
  ['take_no', tno, EXPECT.takeNo],
  ['remark', rmk, EXPECT.remark],
  ['operator_id', String(op), String(EXPECT.operatorId)],
  ['status', st, EXPECT.status],
  ['details', String(dcnt), '0'],
];
const mismatch = checks.filter(([k, a, e]) => a !== e);
if (mismatch.length) {
  console.error('PRECOND_FAIL=' + JSON.stringify(mismatch));
  process.exit(3);
}

// ---- 单事务改状态（受影响必须恰好 1）----
const newRemark = EXPECT.remark + ' INVALID_FIXTURE';
const script = [
  'START TRANSACTION;',
  `UPDATE stock_take SET status='cancelled', remark=CONCAT(remark,' INVALID_FIXTURE')`,
  ` WHERE take_id=${EXPECT.takeId} AND take_no='${EXPECT.takeNo}' AND remark='${EXPECT.remark}'`,
  `   AND operator_id=${EXPECT.operatorId} AND status='completed'`,
  `   AND (SELECT COUNT(*) FROM stock_take_detail d WHERE d.take_id=stock_take.take_id)=0;`,
  'SELECT ROW_COUNT();',
  'COMMIT;',
].join('\n');

const raw = run(script);
const affected = Number(String(raw).split(/\r?\n/).filter(Boolean).pop());
if (affected !== 1) {
  run(`ROLLBACK;`); // 兜底
  console.error('AFFECTED_FAIL expected=1 actual=' + affected);
  process.exit(4);
}

// ---- 回读确认 ----
const after = rows(`SELECT take_id,status,remark FROM stock_take WHERE take_id=${EXPECT.takeId}`)[0];
console.log(JSON.stringify({
  ok: affected === 1,
  guard: { port: String(port), datadir: dd },
  precondition_checks: checks.map(([k, a, e]) => ({ field: k, actual: a, expected: e, match: a === e })),
  affected_rows: affected,
  after: { take_id: Number(after[0]), status: after[1], remark: after[2] },
  deleted: false,
  note: 'cancelled with INVALID_FIXTURE appended; audit trail preserved',
}, null, 2));
