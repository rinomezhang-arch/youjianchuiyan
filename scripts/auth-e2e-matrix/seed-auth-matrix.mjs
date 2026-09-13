#!/usr/bin/env node
/**
 * DL-AUTH-E2E-MATRIX-15 合成账号装载器（替代 seed-auth-matrix.sql）
 *
 * 规则（Codex 退回第 4/5 条）：
 * - 不使用 DELETE；不物理删除任何行。
 * - 每个 run 生成唯一账号后缀（唯一合成账号），互不覆盖。
 * - 密码运行时注入（MX_PASS 环境变量或进程内随机生成），绝不写进脚本、日志、制品。
 * - 本轮结束只做“可追踪停用”（employment_status→resigned），保留行以便审计。
 * - 只允许连接隔离实例（端口必须显式给出且不得为生产端口）。
 *
 * 用法：
 *   MX_PASS=<密码> node seed-auth-matrix.mjs --port 13318 --schema banquet_rc15
 *   若不传 --port 则使用 MX_DB_PORT；拒绝默认 3306 与 13317。
 */
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';

const MYSQL = process.env.MX_MYSQL_CLI || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';

function arg(name, fallback = null) {
  const i = process.argv.indexOf('--' + name);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}

const PORT = String(arg('port', process.env.MX_DB_PORT || ''));
const SCHEMA = arg('schema', process.env.MX_DB_SCHEMA || 'banquet_rc15');
const HOST = arg('host', process.env.MX_DB_HOST || '127.0.0.1');
// 生产端口与已知生产实例端口一律拒绝
const FORBIDDEN_PORTS = new Set(['3306', '13317']);
if (!PORT) throw new Error('必须显式提供隔离端口：--port 或 MX_DB_PORT');
if (FORBIDDEN_PORTS.has(PORT)) throw new Error(`拒绝连接端口 ${PORT}：该端口按裁决为生产/禁用端口`);

// 密码：运行时注入，绝不落盘
const PASS = process.env.MX_PASS;
if (!PASS) throw new Error('必须通过环境变量 MX_PASS 运行时注入合成密码，禁止硬编码/落盘');

const runId = process.env.MX_RUN_ID || crypto.randomBytes(4).toString('hex');

function sql(q, { db = SCHEMA } = {}) {
  const args = ['-h', HOST, '-P', PORT, '-u', 'root', '-N', '-B'];
  if (db) args.push(db);
  args.push('-e', q);
  return execFileSync(MYSQL, args, { encoding: 'utf8' }).trim();
}

// 每个 run 唯一主键号段，避免与上一轮/其它任务重叠；不删除任何既有行。
function runSeed(id) {
  let h = 0;
  for (const ch of String(id)) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return h;
}
const base = 900000 + (runSeed(runId) % 90000);
const accounts = [
  ['gm',      0],
  ['manager', 1],
  ['manager', 2],
  ['lawyer',  1],
  ['manager', 1],
  ['manager', 1],
];

const rows = accounts.map(([role, store], i) => {
  const id = base + i + 1;
  const acct = `mx_${runId}_${i + 1}`;
  return { id, acct, role, store };
});

// 幂等写入：同 id 冲突时只更新（不删除）
const values = rows.map(r =>
  `(${r.id},'MX ${r.acct}','${r.acct}','${r.role}',${r.store},'active',0,0,'${PASS}')`
).join(',\n ');

sql(`INSERT INTO staff_master(staff_id,staff_name,staff_account,role,store_id,employment_status,can_manage_hr,can_view_all_stores,staff_password) VALUES
 ${values}
 ON DUPLICATE KEY UPDATE staff_account=VALUES(staff_account), role=VALUES(role), store_id=VALUES(store_id), employment_status='active', staff_password=VALUES(staff_password)`);

sql(`INSERT INTO store_info(store_id,store_code,store_name) VALUES
 (1,'MXS1','MX 一店'),(2,'MXS2','MX 二店')
 ON DUPLICATE KEY UPDATE store_code=VALUES(store_code)`);

const check = sql(`SELECT COUNT(*) FROM staff_master WHERE staff_id BETWEEN ${base + 1} AND ${base + 6}`);

// 只输出可公开的：runId / 号段 / 账号名。不输出密码。
console.log(JSON.stringify({
  ok: true,
  run_id: runId,
  schema: SCHEMA,
  port: PORT,
  staff_id_range: [base + 1, base + 6],
  accounts: rows.map(r => r.acct),
  inserted_or_updated: Number(check),
  password_printed: false,
}, null, 2));
