#!/usr/bin/env node
/**
 * TR-RELEASE-RC-15 真实登录链 + 真实端口鉴权数据流复核
 *
 * 与上一版 rc15-real-http.mjs 的区别：token 一律来自真实登录接口
 *   POST /api/auth/login {username: staff_account, password}
 * 不再在本地签发 JWT。登录成功后再做调店/降权/停用，复核实时复核链：
 *   真实登录签发 token → 库里改身份 → 同一 token 打真实 /api/finance/account → DB 回读。
 *
 * 前置：setup-isolated-db.ps1 已重建 banquet_rc15 并灌入 seed-synthetic.sql
 *       真实后端 jar 已在 18080 运行且连 banquet_rc15
 * 合成账号：syn_mover/syn_demoted/syn_leaver/syn_stayer，密码 synpass123
 */
import { execFileSync } from 'node:child_process';

const BASE = process.env.RC15_BASE || 'http://127.0.0.1:18080';
const PASS = 'synpass123';
const ACTORS = {
  mover:   { id: 401, username: 'syn_mover'   },
  demoted: { id: 402, username: 'syn_demoted' },
  leaver:  { id: 403, username: 'syn_leaver'  },
  stayer:  { id: 404, username: 'syn_stayer'  },
};

let pass = 0, fail = 0;
function ok(name, cond, extra = '') {
  if (cond) { pass++; console.log(`PASS  ${name}${extra ? '  ' + extra : ''}`); }
  else { fail++; console.log(`FAIL  ${name}  ${extra}`); }
}

async function http(method, path, bearer, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      ...(bearer ? { Authorization: 'Bearer ' + bearer } : {}),
      ...(body ? { 'Content-Type': 'application/json;charset=utf-8' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const raw = Buffer.from(await res.arrayBuffer()).toString('utf8');
  let json = null;
  try { json = raw ? JSON.parse(raw) : null; } catch { /* 非 JSON */ }
  return { status: res.status, raw, json };
}

function sql(query) {
  // 本机隔离测试 MySQL 13317（MEM-DILONG-005 入口）：直接用本机 mysql 客户端 TCP 回环，root 空密码
  return execFileSync(
    'C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe',
    ['--no-defaults', '--protocol=tcp', '--host=127.0.0.1', '--port=13317', '--user=root',
     '--default-character-set=utf8mb4', '-N', '-B', 'banquet_rc15', '-e', query],
    { encoding: 'utf8' }
  ).trim();
}

async function login(username, password = PASS) {
  const r = await http('POST', '/api/auth/login', null, { username, password });
  return { status: r.status, json: r.json, token: r.json?.data?.token || null, raw: r.raw };
}

function accountStores(reply) {
  return (reply.json?.data ?? []).map(r => Number(r.store_id ?? r.storeId));
}

async function main() {
  // ---------- 起点复位（幂等重跑；完整重建走 setup-isolated-db.ps1） ----------
  sql(`UPDATE staff_master SET store_id=1, role='manager', employment_status='active' WHERE staff_id BETWEEN 401 AND 404;
       DELETE FROM finance_account WHERE account_id NOT IN (9001,9002);
       DELETE FROM audit_logs;`);

  // ---------- 0. 真实登录链 ----------
  const wrong = await login('syn_stayer', 'wrong-password-xx');
  ok('0a 错误密码 401 且口径统一', wrong.status === 200 && wrong.json?.code === 401,
    `http=${wrong.status} code=${wrong.json?.code} msg=${wrong.json?.message}`);
  const noUser = await login('syn_nobody_xyz', 'whatever');
  ok('0b 不存在账号同样 401 同文案', noUser.json?.code === 401 && noUser.json?.message === wrong.json?.message,
    `code=${noUser.json?.code} msg=${noUser.json?.message}`);

  const lMover = await login('syn_mover');
  const lDemoted = await login('syn_demoted');
  const lLeaver = await login('syn_leaver');
  const lStayer = await login('syn_stayer');
  ok('0c 调店员工真实登录拿到 token', lMover.status === 200 && !!lMover.token && lMover.json?.data?.user?.staffId === 401,
    `http=${lMover.status} staff=${lMover.json?.data?.user?.staffId} store=${lMover.json?.data?.storeId}`);
  ok('0d 降权/停用/在职员工真实登录均成功', lDemoted.token && lLeaver.token && lStayer.token,
    `demoted=${!!lDemoted.token} leaver=${!!lLeaver.token} stayer=${!!lStayer.token}`);

  const tMover = lMover.token, tDemoted = lDemoted.token, tLeaver = lLeaver.token, tStayer = lStayer.token;

  // ---------- 1. 调店后读范围随库走（登录在调店前） ----------
  let r = await http('GET', '/api/finance/account', tMover);
  ok('1a 调店前只见一店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[1]', `stores=${accountStores(r)}`);
  sql(`UPDATE staff_master SET store_id=2 WHERE staff_id=${ACTORS.mover.id}`);
  r = await http('GET', '/api/finance/account', tMover);
  ok('1b 调店后同一登录token只见二店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[2]',
    `status=${r.status} stores=${accountStores(r)}`);

  // ---------- 2. 请求参数撑不开范围 ----------
  r = await http('GET', '/api/finance/account?storeId=1', tMover);
  ok('2 传 storeId=1 仍只见二店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[2]', `stores=${accountStores(r)}`);

  // ---------- 3. 写归属 + 审计随库走 ----------
  const auditBefore = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('POST', '/api/finance/account', tMover, { accountName: '真实登录链合成新账户', accountType: 'cash' });
  const newId = r.json?.data?.accountId ?? r.json?.data?.account_id;
  ok('3a 调店后建账户 code=200', r.json?.code === 200 && newId, `status=${r.status} body=${r.raw.slice(0, 160)}`);
  if (newId) {
    ok('3b 新账户落二店', Number(sql(`SELECT store_id FROM finance_account WHERE account_id=${Number(newId)}`)) === 2);
    const [au, at, as] = sql('SELECT user_id,target,store_id FROM audit_logs ORDER BY id DESC LIMIT 1').split('\t');
    ok('3c 审计+1', Number(sql('SELECT COUNT(*) FROM audit_logs')) === auditBefore + 1);
    ok('3d 审计门店=二店', Number(as) === 2, `store_id=${as}`);
    ok('3e 审计人=401(真实登录账号)', String(au) === '401', `user_id=${au}`);
    ok('3f 审计对象=createAccount', String(at).includes('createAccount'), `target=${at}`);
  } else { ok('3b-3f 写归属审计链', false, '建账户未成功'); }

  // ---------- 4/5. 降权立即生效 ----------
  r = await http('GET', '/api/finance/account', tDemoted);
  ok('4a 降权前对照 200', r.status === 200, `status=${r.status}`);
  sql(`UPDATE staff_master SET role='lawyer' WHERE staff_id=${ACTORS.demoted.id}`);
  const acctsBefore = Number(sql('SELECT COUNT(*) FROM finance_account'));
  r = await http('GET', '/api/finance/account', tDemoted);
  ok('4b 降权(lawyer)后读 403', r.status === 403, `status=${r.status}`);
  r = await http('POST', '/api/finance/account', tDemoted, { accountName: '降权后不该建出来的账户', accountType: 'cash' });
  ok('5a 降权后写 403', r.status === 403, `status=${r.status}`);
  ok('5b 降权后零写入', Number(sql('SELECT COUNT(*) FROM finance_account')) === acctsBefore);

  // 律师白名单仍可用真实登录token访问法务自身入口（不读正文/证据内容，只看状态码口径）
  const legalMe = await http('GET', '/api/legal/me', tDemoted);
  ok('5c 降权后 /api/legal/me 不在403拒绝面（法务入口保留）', legalMe.status !== 403, `status=${legalMe.status}`);

  // ---------- 6/7. 停用立即生效 ----------
  r = await http('GET', '/api/finance/account', tLeaver);
  ok('6a 停用前对照 200', r.status === 200, `status=${r.status}`);
  sql(`UPDATE staff_master SET employment_status='resigned' WHERE staff_id=${ACTORS.leaver.id}`);
  const auditsBefore = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('GET', '/api/finance/account', tLeaver);
  ok('6b 停用后读 401', r.status === 401, `status=${r.status}`);
  r = await http('POST', '/api/finance/account', tLeaver, { accountName: '停用后不该建出来的账户', accountType: 'cash' });
  ok('7a 停用后写 401', r.status === 401, `status=${r.status}`);
  ok('7b 停用后零写入', Number(sql('SELECT COUNT(*) FROM finance_account')) === acctsBefore);
  ok('7c 停用后零审计', Number(sql('SELECT COUNT(*) FROM audit_logs')) === auditsBefore);

  // 停用后再登录被拒（登录链与停用状态一致）
  const reLogin = await login('syn_leaver');
  ok('7d 停用账号重新登录 401', reLogin.json?.code === 401, `code=${reLogin.json?.code}`);

  // ---------- 8. 在职未变不受影响 ----------
  r = await http('GET', '/api/finance/account', tStayer);
  ok('8a 在职员工读 200 见一店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[1]', `stores=${accountStores(r)}`);
  const audits8 = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('POST', '/api/finance/account', tStayer, { accountName: '在职员工真实登录建的账户', accountType: 'cash' });
  const normalId = r.json?.data?.accountId ?? r.json?.data?.account_id;
  ok('8b 在职员工建账户 code=200', r.json?.code === 200 && normalId, `status=${r.status}`);
  if (normalId) {
    ok('8c 新账户落一店', Number(sql(`SELECT store_id FROM finance_account WHERE account_id=${Number(normalId)}`)) === 1);
    ok('8d 审计照记', Number(sql('SELECT COUNT(*) FROM audit_logs')) === audits8 + 1);
  }
  // 退出登录接口可用（真实登录链闭环）
  const logout = await http('POST', '/api/auth/logout', tStayer, {});
  ok('8e /api/auth/logout 可调用', logout.status === 200, `status=${logout.status}`);

  console.log(`\nRC15_REAL_LOGIN_HTTP_RESULT pass=${pass} fail=${fail}`);
  process.exit(fail ? 1 : 0);
}
main().catch(e => { console.error('DRIVER_ERROR', e); process.exit(2); });
