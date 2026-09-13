#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-HTTP-R3-53 合成账号口令注入（严格受限于任务卡）
 *
 * 约束：
 * - 只更新 tr37_stocktake_20260909 中已存在的 3 个 SYN 合成账号的 BCrypt 哈希
 * - 更新前逐项断言：staff_account / staff_id / store_id / role 完全匹配预期，且账号含 SYN 标识
 * - 口令在进程内随机生成（crypto.randomBytes），不写文件、不打印、不进日志
 * - 不新增、不删除、不改真实账号
 * - 连接目标常量锁死，无环境变量注入点
 */
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';

const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = '127.0.0.1';
const PORT = '13318';
const SCHEMA = 'tr37_stocktake_20260909';

// 预期账号（逐项断言用）
const EXPECTED = [
  { staffId: 1,  account: 'synmgr1', storeId: 1, role: 'store_manager' },
  { staffId: 2,  account: 'synmgr2', storeId: 2, role: 'store_manager' },
  { staffId: 99, account: 'syngm99', storeId: 0, role: 'gm' },
];

function sql(q, { db = SCHEMA } = {}) {
  const args = ['-h', HOST, '-P', PORT, '-u', 'root', '-N', '-B'];
  if (db) args.push(db);
  args.push('-e', q);
  return execFileSync(MYSQL, args, { encoding: 'utf8' }).trim();
}
function rows(q) {
  const out = sql(q);
  if (!out) return [];
  return out.split(/\r?\n/).map(l => l.split('\t'));
}

// ---- 守卫 ----
const port = rows('SELECT @@port')[0][0];
if (String(port) !== PORT) { console.error('GUARD_FAIL port=' + port); process.exit(2); }
const datadir = rows('SELECT @@datadir')[0][0];
if (!String(datadir).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317')) {
  console.error('GUARD_FAIL datadir=' + datadir); process.exit(2);
}

// ---- 逐项断言现有账号 ----
const actual = rows(`SELECT staff_id, staff_account, store_id, role FROM staff_master ORDER BY staff_id`);
const actualMap = new Map(actual.map(r => [Number(r[0]), { account: r[1], storeId: Number(r[2]), role: r[3] }]));

let allOk = true;
const checks = [];
for (const e of EXPECTED) {
  const a = actualMap.get(e.staffId);
  const ok = !!a && a.account === e.account && a.storeId === e.storeId && a.role === e.role && /^syn/i.test(a.account);
  if (!ok) allOk = false;
  checks.push({ staff_id: e.staffId, expected: e.account, actual: a ? a.account : null, match: ok });
}
if (!allOk) {
  console.error('PRECONDITION_FAIL=' + JSON.stringify(checks));
  process.exit(3);
}

// ---- 进程内随机口令（不落盘、不输出）----
// 使用 cost=10 与现有哈希一致的 BCrypt；这里用 node 自带能力不可行，改用后端同类做法：
// 由于无法在纯 node 无依赖下生成 BCrypt，改用 mysql 侧不可行（无 bcrypt 函数）。
// 因此用已安装的 bcryptjs / bcrypt 若存在，否则用 openssl 兼容方案。
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
let bcrypt = null;
for (const mod of ['bcryptjs', 'bcrypt']) {
  try { bcrypt = require(mod); break; } catch { /* try next */ }
}
if (!bcrypt) {
  console.error('NO_BCRYPT_MODULE: 需要 bcryptjs 或 bcrypt 模块生成哈希');
  process.exit(4);
}

const results = [];
for (const e of EXPECTED) {
  const plain = crypto.randomBytes(24).toString('base64url'); // 仅存在于本进程
  const hash = bcrypt.hashSync(plain, 10);
  const escaped = hash.replace(/'/g, "''");
  sql(`UPDATE staff_master SET staff_password='${escaped}' WHERE staff_id=${e.staffId} AND staff_account='${e.account}'`);
  const back = rows(`SELECT staff_password FROM staff_master WHERE staff_id=${e.staffId}`);
  results.push({
    staff_id: e.staffId,
    account: e.account,
    updated: back.length === 1 && back[0][0] === hash,
    hash_prefix: hash.slice(0, 7),
  });
  // plain 随循环结束即可被回收；不输出、不写文件
}

console.log(JSON.stringify({
  ok: results.every(r => r.updated),
  guard: { port: String(port), datadir },
  precondition_checks: checks,
  updated_rows: results.length,
  accounts: results.map(r => ({ staff_id: r.staff_id, account: r.account, updated: r.updated })),
  plaintext_printed: false,
}, null, 2));
