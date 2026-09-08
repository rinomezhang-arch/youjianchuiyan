#!/usr/bin/env node
/**
 * TR-RELEASE-RC-15 真实端口鉴权数据流复核
 *
 * 把 CL-AUTH-CONTEXT-15 的 AuthContextScopeHttpMysqlTest（standalone MockMvc）
 * 原样搬到「真实监听容器 + 真实 HTTP + 隔离 MySQL」上复演：
 *   - 目标：运行在 18080 的 RC15 候选 jar（全量 Spring 上下文，非手工接线）
 *   - 库：banquet_rc15（13317 测试实例，结构复制自 banquet_e2e，合成数据）
 *   - JWT：按 jwt.secret 真实 HS256 签发，storeId/role 刻意冻结签发时旧值
 *   - 调店/降权/停用直接改库，模拟"库里已变、token 未变"
 *
 * 8 组断言：
 *   1 调店后读范围随库走（同一 token 只见新店）
 *   2 请求参数 storeId 撑不开门店范围
 *   3 调店后新建账户落新店 + 审计 store_id/user_id/target 正确
 *   4 降权(lawyer) 读 403
 *   5 降权后写 403 且零写入
 *   6 停用(resigned) 读 401
 *   7 停用后写 401 且零写入、零审计
 *   8 在职未变员工读写照常、审计照记（收紧不误伤）
 *
 * 全程合成账号 401-404，不碰真实账号/法务接口。
 */
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

const BASE = process.env.RC15_BASE || 'http://127.0.0.1:18080';
const SECRET = process.env.JWT_SECRET || 'rc15-isolated-jwt-secret-0123456789abcdef0123456789';
const MOVER = 401, DEMOTED = 402, LEAVER = 403, STAYER = 404;

let pass = 0, fail = 0;
function ok(name, cond, extra = '') {
  if (cond) { pass++; console.log(`PASS  ${name}${extra ? '  ' + extra : ''}`); }
  else { fail++; console.log(`FAIL  ${name}  ${extra}`); }
}

function b64url(buf) {
  return Buffer.from(buf).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function token(staffId, storeIdAtIssue, roleAtIssue) {
  const header = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = b64url(JSON.stringify({
    sub: 'syn_' + staffId,
    staffId, storeId: storeIdAtIssue, role: roleAtIssue,
    exp: Math.floor(Date.now() / 1000) + 600,
  }));
  const sig = b64url(crypto.createHmac('sha256', Buffer.from(SECRET, 'utf8'))
    .update(header + '.' + payload).digest());
  return header + '.' + payload + '.' + sig;
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

// 隔离库变更与回读：经容器内 mysql 客户端（13317 为宿主机测试实例）
function sql(query) {
  const out = execFileSync('docker', [
    'exec', '-i', 'youjian-mysql-e2e',
    'mysql', '-h', 'host.docker.internal', '-P', '13317', '-uroot',
    '--default-character-set=utf8mb4', '-N', '-B', 'banquet_rc15', '-e', query,
  ], { encoding: 'utf8' });
  return out.trim();
}
function accountStores(reply) {
  return (reply.json?.data ?? []).map(r => Number(r.store_id ?? r.storeId));
}

const tMover = token(MOVER, 1, 'manager');
const tDemoted = token(DEMOTED, 1, 'manager');
const tLeaver = token(LEAVER, 1, 'manager');
const tStayer = token(STAYER, 1, 'manager');

async function main() {
  // ---------- 场景 1：调店后读范围随库走 ----------
  let r = await http('GET', '/api/finance/account', tMover);
  ok('1a 调店前 200', r.status === 200, `status=${r.status}`);
  ok('1b 调店前只见一店', JSON.stringify(accountStores(r).sort()) === '[1]', `stores=${accountStores(r)}`);

  sql(`UPDATE staff_master SET store_id=2 WHERE staff_id=${MOVER}`);

  r = await http('GET', '/api/finance/account', tMover);
  ok('1c 调店后同一token只见二店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[2]',
    `status=${r.status} stores=${accountStores(r)}`);

  // ---------- 场景 2：请求参数撑不开范围 ----------
  r = await http('GET', '/api/finance/account?storeId=1', tMover);
  ok('2 传 storeId=1 仍只见二店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[2]',
    `stores=${accountStores(r)}`);

  // ---------- 场景 3：写归属 + 审计随库走 ----------
  const auditBefore = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('POST', '/api/finance/account', tMover, { accountName: '真实端口合成新账户', accountType: 'cash' });
  const newId = r.json?.data?.accountId ?? r.json?.data?.account_id;
  ok('3a 调店后建账户 code=200', r.json?.code === 200 && newId, `status=${r.status} body=${r.raw.slice(0, 200)}`);
  if (newId) {
    const newStore = Number(sql(`SELECT store_id FROM finance_account WHERE account_id=${Number(newId)}`));
    ok('3b 新账户落二店（非token旧店）', newStore === 2, `account_id=${newId} store_id=${newStore}`);
    const auditRow = sql('SELECT user_id,target,store_id FROM audit_logs ORDER BY id DESC LIMIT 1');
    const [au, at, as] = auditRow.split('\t');
    ok('3c 审计+1', Number(sql('SELECT COUNT(*) FROM audit_logs')) === auditBefore + 1);
    ok('3d 审计门店=二店', Number(as) === 2, `audit=${auditRow}`);
    ok('3e 审计人=401', String(au) === String(MOVER), `user_id=${au}`);
    ok('3f 审计对象含createAccount', String(at).includes('createAccount'), `target=${at}`);
  } else { ok('3b 新账户落二店', false, '无 accountId'); ok('3c-3f 审计链', false, '建账户未成功'); }

  // ---------- 场景 4/5：降权立即生效 ----------
  r = await http('GET', '/api/finance/account', tDemoted);
  ok('4a 降权前对照 200', r.status === 200, `status=${r.status}`);
  sql(`UPDATE staff_master SET role='lawyer' WHERE staff_id=${DEMOTED}`);
  const acctsBeforeDemote = Number(sql('SELECT COUNT(*) FROM finance_account'));
  r = await http('GET', '/api/finance/account', tDemoted);
  ok('4b 降权后读 403', r.status === 403, `status=${r.status}`);
  r = await http('POST', '/api/finance/account', tDemoted, { accountName: '降权后不该建出来的账户', accountType: 'cash' });
  ok('5a 降权后写 403', r.status === 403, `status=${r.status}`);
  ok('5b 降权后零写入', Number(sql('SELECT COUNT(*) FROM finance_account')) === acctsBeforeDemote);

  // ---------- 场景 6/7：停用立即生效 ----------
  r = await http('GET', '/api/finance/account', tLeaver);
  ok('6a 停用前对照 200', r.status === 200, `status=${r.status}`);
  sql(`UPDATE staff_master SET employment_status='resigned' WHERE staff_id=${LEAVER}`);
  const acctsBeforeLeave = Number(sql('SELECT COUNT(*) FROM finance_account'));
  const auditsBeforeLeave = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('GET', '/api/finance/account', tLeaver);
  ok('6b 停用后读 401', r.status === 401, `status=${r.status}`);
  r = await http('POST', '/api/finance/account', tLeaver, { accountName: '停用后不该建出来的账户', accountType: 'cash' });
  ok('7a 停用后写 401', r.status === 401, `status=${r.status}`);
  ok('7b 停用后零写入', Number(sql('SELECT COUNT(*) FROM finance_account')) === acctsBeforeLeave);
  ok('7c 停用后零审计', Number(sql('SELECT COUNT(*) FROM audit_logs')) === auditsBeforeLeave);

  // ---------- 场景 8：在职未变不受影响 ----------
  r = await http('GET', '/api/finance/account', tStayer);
  ok('8a 在职员工读 200 见一店', r.status === 200 && JSON.stringify(accountStores(r).sort()) === '[1]',
    `status=${r.status} stores=${accountStores(r)}`);
  const auditsBeforeNormal = Number(sql('SELECT COUNT(*) FROM audit_logs'));
  r = await http('POST', '/api/finance/account', tStayer, { accountName: '在职员工真实端口建的账户', accountType: 'cash' });
  const normalId = r.json?.data?.accountId ?? r.json?.data?.account_id;
  ok('8b 在职员工建账户 code=200', r.json?.code === 200 && normalId, `status=${r.status} body=${r.raw.slice(0, 160)}`);
  if (normalId) {
    ok('8c 新账户落一店', Number(sql(`SELECT store_id FROM finance_account WHERE account_id=${Number(normalId)}`)) === 1);
    ok('8d 审计照记', Number(sql('SELECT COUNT(*) FROM audit_logs')) === auditsBeforeNormal + 1);
  }

  console.log(`\nRC15_REAL_HTTP_RESULT pass=${pass} fail=${fail}`);
  process.exit(fail ? 1 : 0);
}
main().catch(e => { console.error('DRIVER_ERROR', e); process.exit(2); });
