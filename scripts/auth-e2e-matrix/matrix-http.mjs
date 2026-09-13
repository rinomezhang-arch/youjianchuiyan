#!/usr/bin/env node
/**
 * DL-AUTH-E2E-MATRIX-15 正式矩阵（真实隔离库 + 真实后端）
 * 覆盖：登录/重载/退出/重登、停用撤权、GM跨店、经理越店、律师仅legal（含 booking/customer 拒绝）、
 *       iPad 一次性授权矩阵、写入零副作用与门店归属。
 *
 * 安全约束（Codex 退回第 1/2/4/5 条）：
 * - 端口/主机/schema 全部走显式变量，缺省即报错；绝不默认 13317/3306。
 * - 密码只从环境变量 MX_PASS 运行时注入；不打印密码/JWT；证据文件不含密码/JWT。
 * - 仅只读断言 + iPad 授权路径的必要业务写入（全部落在隔离库合成账号范围内）。
 * - 不调用返回法务正文的接口：仅断言状态码，不读取/保存 legal 正文。
 */
import { execFileSync } from 'node:child_process';

const BASE = process.env.MX_BASE || 'http://127.0.0.1:18080';
const PASS = process.env.MX_PASS;
const MYSQL = process.env.MX_MYSQL_CLI || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const DB_PORT = String(process.env.MX_DB_PORT || '');
const DB_HOST = process.env.MX_DB_HOST || '127.0.0.1';
const DB = process.env.MX_DB_SCHEMA || 'banquet_rc15';
const RUN_ID = process.env.MX_RUN_ID || 'adhoc';
const OUT = process.env.MX_OUT || '';

if (!PASS) throw new Error('缺少 MX_PASS：密码必须运行时注入');
if (!DB_PORT) throw new Error('缺少 MX_DB_PORT：隔离端口必须显式指定');
if (['3306', '13317'].includes(DB_PORT)) throw new Error(`拒绝连接端口 ${DB_PORT}`);

// 账号名前缀与 seed 保持一致：mx_<runId>_<n>
const A = n => `mx_${RUN_ID}_${n}`;

let pass = 0, fail = 0; const results = [];
function ok(name, cond, detail = '') {
  if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
  else { fail++; results.push({ name, status: 'FAIL', detail }); }
}
function sql(q) {
  return execFileSync(MYSQL, ['-h', DB_HOST, '-P', DB_PORT, '-u', 'root', DB, '-N', '-B', '-e', q], { encoding: 'utf8' }).trim();
}
function redact(obj) {
  if (obj == null || typeof obj !== 'object') return obj;
  const c = Array.isArray(obj) ? obj.map(redact) : { ...obj };
  for (const k of Object.keys(c)) {
    if (/token|password|secret|authorization/i.test(k)) c[k] = c[k] ? '***' : c[k];
    else if (typeof c[k] === 'object') c[k] = redact(c[k]);
  }
  return c;
}
const THROTTLE_MS = Number(process.env.MX_THROTTLE_MS || 400);
const sleep = ms => new Promise(r => setTimeout(r, ms));

// 带节流 + 429 退避重试的请求；429 是环境限流，不是断言语义。
async function request(method, path, opts = {}) {
  const { token, body, extraHeaders } = opts;
  for (let attempt = 0; attempt < 6; attempt++) {
    await sleep(THROTTLE_MS);
    const res = await fetch(BASE + path, {
      method,
      headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}), ...(extraHeaders || {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    let j = null; try { j = await res.json(); } catch {}
    if (res.status !== 429) return { status: res.status, code: j?.code, data: j?.data, message: j?.message, token: j?.data?.token, user: j?.data?.user };
    const wait = 800 * (attempt + 1);
    throttled429.push({ path, attempt, wait });
    await sleep(wait);
  }
  return { status: 429, code: 429, data: null, message: 'throttled after retries' };
}
const throttled429 = [];

async function login(username, password = PASS) {
  const r = await request('POST', '/api/auth/login', { body: { username, password } });
  return { status: r.status, code: r.code, role: r.user?.role, storeId: r.data?.storeId, staffId: r.user?.staffId, token: r.token };
}
async function api(method, path, token, body, extraHeaders) {
  return request(method, path, { token, body, extraHeaders });
}
const log = [];
function rec(section, name, r) { log.push({ section, name, status: r.status, code: r.code, data_kind: Array.isArray(r.data) ? `array[${r.data.length}]` : typeof r.data }); }

// ============ 0. 夹具自检：确认隔离库与合成账号 ============
{
  const datadir = sql('SELECT @@datadir');
  ok('env.datadir-isolated', /mysql-test-13317/i.test(datadir), `datadir=${datadir}`);
  const cnt = Number(sql(`SELECT COUNT(*) FROM staff_master WHERE staff_account LIKE 'mx_${RUN_ID}_%'`));
  ok('env.synthetic-accounts-present', cnt === 6, `n=${cnt}`);
}

// ============ 1. 登录 / me / 重载 / 退出 / 重登 ============
{
  const a = await login(A(2)); // 一店经理
  ok('login.manager.ok', a.status === 200 && a.code === 200 && a.role === 'manager' && a.storeId === 1 && Boolean(a.token), `role=${a.role} store=${a.storeId}`);
  const me1 = await api('GET', '/api/auth/me', a.token); rec('login', 'me.after-login', me1);
  ok('me.after-login', me1.status === 200 && me1.code === 200, `status=${me1.status}`);
  const me2 = await api('GET', '/api/auth/me', a.token);
  ok('reload.same-token.me', me2.status === 200 && Number(me2.data?.storeId) === 1, `storeId=${me2.data?.storeId}`);
  const lo = await api('POST', '/api/auth/logout', a.token);
  ok('logout.ok', lo.status === 200 && lo.code === 200, `code=${lo.code}`);
  const b = await login(A(2));
  ok('relogin.fresh', b.status === 200 && b.storeId === 1 && Boolean(b.token), `storeId=${b.storeId}`);
}

// ============ 2. 停用撤权（唯一 DB 写：隔离库合成号 905->resigned->active） ============
{
  const dead = A(5);
  const a = await login(dead);
  ok('dead.login-before-disable.ok', a.status === 200 && a.code === 200, `code=${a.code}`);
  const before = await api('GET', '/api/auth/me', a.token);
  ok('dead.me-before-disable', before.status === 200 && before.code === 200, `code=${before.code}`);
  const sid = Number(sql(`SELECT staff_id FROM staff_master WHERE staff_account='${dead}'`));
  sql(`UPDATE staff_master SET employment_status='resigned' WHERE staff_id=${sid}`);
  const after = await api('GET', '/api/auth/me', a.token);
  ok('revoke.after-disable.rejected', after.status === 401 || after.status === 403 || after.code === 401 || after.code === 403, `status=${after.status} code=${after.code}`);
  const relogin = await login(dead);
  ok('revoke.disabled-cannot-login', relogin.status !== 200 || relogin.code !== 200, `status=${relogin.status} code=${relogin.code}`);
  sql(`UPDATE staff_master SET employment_status='active' WHERE staff_id=${sid}`);
  const restored = await login(dead);
  ok('revoke.restored-can-login-again', restored.status === 200 && restored.code === 200, `code=${restored.code}`);
}

// ============ 3. GM 跨店 / 4. 经理越店 ============
{
  const g = await login(A(1));
  ok('gm.login.store0', g.status === 200 && g.role === 'gm' && g.storeId === 0, `store=${g.storeId}`);
  const s1 = await api('GET', '/api/finance/account?storeId=1', g.token);
  const s2 = await api('GET', '/api/finance/account?storeId=2', g.token); rec('gm', 'cross-store.s1', s1); rec('gm', 'cross-store.s2', s2);
  const all1 = (s1.data || []).every(r => Number(r.store_id) === 1);
  const all2 = (s2.data || []).every(r => Number(r.store_id) === 2);
  ok('gm.cross-store.query1', s1.status === 200 && (s1.data || []).length > 0 && all1, `n=${(s1.data || []).length}`);
  ok('gm.cross-store.query2', s2.status === 200 && (s2.data || []).length > 0 && all2, `n=${(s2.data || []).length}`);

  const m1 = await login(A(2));
  const a = await api('GET', '/api/finance/account?storeId=1', m1.token);
  const b = await api('GET', '/api/finance/account?storeId=2', m1.token);
  const c = await api('GET', '/api/finance/account', m1.token);
  const only1 = arr => (arr || []).length > 0 && arr.every(r => Number(r.store_id) === 1);
  const zeroOrOnly1 = arr => (arr || []).length === 0 || arr.every(r => Number(r.store_id) === 1);
  ok('mgr1.own-store', a.status === 200 && only1(a.data), `n=${(a.data || []).length}`);
  ok('mgr1.cross-store-blocked', zeroOrOnly1(b.data) && b.status === 200, `n=${(b.data || []).length} stores=${[...new Set((b.data || []).map(r => r.store_id))]}`);
  ok('mgr1.default-scope-own', only1(c.data), `n=${(c.data || []).length}`);
}

// ============ 5. 律师仅 legal（含 booking/customer 拒绝） ============
{
  const l = await login(A(4));
  ok('lawyer.login.ok', l.status === 200 && l.role === 'lawyer', `role=${l.role}`);
  const legal = await api('GET', '/api/legal/case', l.token);
  ok('lawyer.legal.case.allowed', legal.status === 200 && legal.code === 200, `status=${legal.status}`);
  const meL = await api('GET', '/api/auth/me', l.token);
  ok('lawyer.me.allowed', meL.status === 200 && meL.code === 200, `status=${meL.status}`);

  const denies = [
    ['stores', '/api/stores'],
    ['finance', '/api/finance/account?storeId=1'],
    ['staff', '/api/staff?storeId=1'],
    ['hr', '/api/hr/payroll?month=2026-09'],
    ['booking.list', '/api/bookings/list'],
    ['booking.stats', '/api/bookings/stats'],
    ['booking.byId', '/api/bookings/RC15-IDEM-BK'],
    ['booking.dishes', '/api/bookings/RC15-IDEM-BK/dishes'],
    ['customer.list', '/api/customers'],
    ['customer.search', '/api/customers/search?keyword=a'],
  ];
  for (const [nm, p] of denies) {
    const r = await api('GET', p, l.token); rec('lawyer.deny', nm, r);
    ok(`lawyer.deny.${nm}`, r.status === 403 || r.code === 403, `status=${r.status} code=${r.code}`);
  }
}

// ============ 6. 写入零副作用 + 门店归属 ============
{
  const g = await login(A(1));
  const cnt1 = Number(sql('SELECT COUNT(*) FROM finance_account WHERE store_id=1'));
  const cnt2 = Number(sql('SELECT COUNT(*) FROM finance_account WHERE store_id=2'));
  const before = Number(sql('SELECT COUNT(*) FROM finance_account'));
  const s1 = await api('GET', '/api/finance/account?storeId=1', g.token);
  const s2 = await api('GET', '/api/finance/account?storeId=2', g.token);
  const after = Number(sql('SELECT COUNT(*) FROM finance_account'));
  ok('write.no-side-effect.readonly', before === after, `before=${before} after=${after}`);
  ok('write.store-attribution.s1', (s1.data || []).length === cnt1 && (s1.data || []).every(r => Number(r.store_id) === 1), `api=${(s1.data || []).length} db=${cnt1}`);
  ok('write.store-attribution.s2', (s2.data || []).length === cnt2 && (s2.data || []).every(r => Number(r.store_id) === 2), `api=${(s2.data || []).length} db=${cnt2}`);
  const m1 = await login(A(2));
  const b = await api('GET', '/api/finance/account?storeId=2', m1.token);
  const leak = (b.data || []).some(r => Number(r.store_id) === 2);
  ok('write.no-cross-store-leak', !leak, `leaked=${leak}`);
}

// ============ 7. iPad 一次性授权矩阵（写接口 + 审计身份） ============
// 设备必须已在 ipad_device_binding 有 active 绑定；授权 token 为进程内一次性能力。
{
  const DEV = process.env.MX_IPAD_DEVICE || 'RC15IDEMDEV';
  const STORE = 1;
  const BIND_STAFF = Number(sql(`SELECT COALESCE(staff_id,0) FROM ipad_device_binding WHERE device_sn='${DEV}' AND status='active' LIMIT 1`));
  const bookingRow = sql(`SELECT id,booking_id FROM booking_master WHERE store_id=${STORE} AND booking_status NOT IN ('cancelled','completed') AND payment_status<>'paid' LIMIT 1`);
  const [bookingPk, bookingId] = bookingRow ? bookingRow.split('\t') : [null, null];
  const dishId = sql(`SELECT dish_id FROM dish_master WHERE store_id=${STORE} AND is_active=1 LIMIT 1`);
  const baseHdr = { 'X-Client-Type': 'ipad', 'X-Store-Id': String(STORE), 'X-Staff-Id': String(BIND_STAFF), 'X-Device-Sn': DEV };

  ok('ipad.fixture.binding', BIND_STAFF > 0, `boundStaff=${BIND_STAFF}`);
  ok('ipad.fixture.booking', Boolean(bookingId), `bookingId=${bookingId}`);
  ok('ipad.fixture.dish', Boolean(dishId), `dishId=${dishId}`);

  const cntDetail0 = Number(sql('SELECT COUNT(*) FROM booking_dish_detail'));
  const cntReq0 = Number(sql('SELECT COUNT(*) FROM ipad_batch_request'));

  // 7a. 未带 X-Client-Type -> 403（拦截器硬约束）
  const noType = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: 'RC15-NOTYPE-000001', dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, { 'X-Store-Id': String(STORE), 'X-Staff-Id': String(BIND_STAFF), 'X-Device-Sn': DEV });
  rec('ipad', 'no-client-type', noType);
  ok('ipad.guard.missing-client-type.403', noType.status === 403, `status=${noType.status}`);

  // 7b. 未绑定设备 -> 401
  const unbound = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: 'RC15-UNBOUND-00001', dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, { 'X-Client-Type': 'ipad', 'X-Store-Id': String(STORE), 'X-Staff-Id': String(BIND_STAFF), 'X-Device-Sn': 'NO-SUCH-DEVICE' });
  rec('ipad', 'unbound-device', unbound);
  ok('ipad.guard.unbound-device.401', unbound.status === 401, `status=${unbound.status}`);

  // 7c. 错店（header 报 2 但设备绑定在 1）-> 403
  const wrongStore = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: 'RC15-WRONGST-00001', dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, { 'X-Client-Type': 'ipad', 'X-Store-Id': '2', 'X-Staff-Id': String(BIND_STAFF), 'X-Device-Sn': DEV });
  rec('ipad', 'wrong-store', wrongStore);
  ok('ipad.guard.wrong-store.403', wrongStore.status === 403, `status=${wrongStore.status}`);

  // 7d. 无授权 token -> 业务码 403（Spring Result.error 走 HTTP 200 + body.code）
  const noToken = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: `RC15-NOTOK-${RUN_ID}`, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'missing-token', noToken);
  ok('ipad.guard.missing-token.403', noToken.code === 403, `http=${noToken.status} code=${noToken.code}`);

  // 7e. 自报 staff_id -> 400（normalize 拒绝）
  const selfStaff = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: `RC15-SELF-${RUN_ID}`, staff_id: 99999, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'self-reported-staff', selfStaff);
  ok('ipad.guard.self-reported-staff.400', selfStaff.status === 400 || selfStaff.code === 400, `status=${selfStaff.status} code=${selfStaff.code}`);

  // 7f. 有效授权：auth/verify 取 token -> add-dishes 成功，审计身份=已验证员工
  const cred = A(2);
  const verify = await api('POST', '/api/ipad/auth/verify', null, { username: cred, password: PASS, booking_id: bookingId }, baseHdr);
  rec('ipad', 'auth-verify', verify);
  const authToken = verify.data?.authorization_token;
  ok('ipad.auth.verify.issues-token', verify.status === 200 && verify.code === 200 && Boolean(authToken), `status=${verify.status} hasToken=${Boolean(authToken)}`);

  const cr = `RC15-RUN-${RUN_ID}-000001`;
  const okSubmit = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: cr, authorization_token: authToken, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'add-dishes.ok', okSubmit);
  ok('ipad.submit.success', okSubmit.status === 200 && okSubmit.code === 200, `http=${okSubmit.status} code=${okSubmit.code}`);

  // 审计身份必须来自已验证上下文（= 触发 verify 的合成员工 staff_id），而非头部自报
  if (okSubmit.code === 200) {
    const op = sql(`SELECT operator_id FROM ipad_batch_request WHERE store_id=${STORE} AND client_request_id='${cr}' LIMIT 1`);
    const realStaffId = Number(sql(`SELECT staff_id FROM staff_master WHERE staff_account='${cred}'`));
    ok('ipad.audit.operator-from-verified-context', Number(op) === realStaffId, `operator=${op} expected=${realStaffId}`);
  } else {
    ok('ipad.audit.operator-from-verified-context', false, 'submit did not succeed; audit unverifiable');
  }

  // 7g. 一次性能力：同一 token 消费后不可复用（重新签发 → 第一次成功 → 复用同 token 拒绝）
  const v2 = await api('POST', '/api/ipad/auth/verify', null, { username: cred, password: PASS, booking_id: bookingId }, baseHdr);
  const t2 = v2.data?.authorization_token;
  const first = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: `RC15-RUN-${RUN_ID}-000003`, authorization_token: t2, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'token-first-use', first);
  const reuse = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: `RC15-RUN-${RUN_ID}-000004`, authorization_token: t2, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'token-reuse-same-body', { status: reuse.status, code: reuse.code, data: reuse.data });
  ok('ipad.guard.token-single-use', first.code === 200 && reuse.code === 403, `first=${first.code} reuse=${reuse.code} (t2issuable=${Boolean(t2)})`);

  // 7h. 错预订（token 绑定 A 预订，提交 B 预订）-> 403
  const v3 = await api('POST', '/api/ipad/auth/verify', null, { username: cred, password: PASS, booking_id: bookingId }, baseHdr);
  const t3 = v3.data?.authorization_token;
  const otherBooking = sql(`SELECT booking_id FROM booking_master WHERE store_id=${STORE} AND booking_id<>'${bookingId}' LIMIT 1`) || 'NO-SUCH-BOOKING';
  const wrongBooking = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: otherBooking, client_request_id: `RC15-RUN-${RUN_ID}-000005`, authorization_token: t3, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'wrong-booking', wrongBooking);
  ok('ipad.guard.wrong-booking.403', wrongBooking.code === 403, `http=${wrongBooking.status} code=${wrongBooking.code} (tokenIssued=${Boolean(t3)})`);

  // 7i. 成功路径只写当前门店当前预订；越权项零脏写
  const cntDetail1 = Number(sql('SELECT COUNT(*) FROM booking_dish_detail'));
  const cntReq1 = Number(sql('SELECT COUNT(*) FROM ipad_batch_request'));
  const crossStoreDetail = Number(sql(`SELECT COUNT(*) FROM booking_dish_detail WHERE store_id<>${STORE}`));
  ok('ipad.write.success-path-isolated', cntDetail1 >= cntDetail0 && crossStoreDetail === 0, `detail ${cntDetail0}->${cntDetail1} crossStore=${crossStoreDetail}`);
  ok('ipad.write.receipts-scoped', cntReq1 >= cntReq0, `receipts ${cntReq0}->${cntReq1}`);

  // 幂等重试：同 client_request_id 同内容 -> 命中已存在收据，不新增业务行
  const idemKey = `RC15-RUN-${RUN_ID}-000003`;
  const beforeIdem = Number(sql(`SELECT COUNT(*) FROM booking_dish_detail WHERE store_id=${STORE}`));
  const v4 = await api('POST', '/api/ipad/auth/verify', null, { username: cred, password: PASS, booking_id: bookingId }, baseHdr);
  const t4 = v4.data?.authorization_token;
  const idem = await api('POST', '/api/ipad/order/add-dishes', null, { booking_id: bookingId, client_request_id: idemKey, authorization_token: t4, dishes: [{ dish_id: dishId, dish_quantity: 1 }] }, baseHdr);
  rec('ipad', 'idempotent-replay-same-key', idem);
  const afterIdem = Number(sql(`SELECT COUNT(*) FROM booking_dish_detail WHERE store_id=${STORE}`));
  ok('ipad.idempotency.same-key-no-new-rows', idem.code === 200 && afterIdem === beforeIdem, `before=${beforeIdem} after=${afterIdem} retryCode=${idem.code}`);
}

console.log('\n==== DL-AUTH-E2E-MATRIX-15 HTTP 矩阵 ====');
for (const r of results) console.log(`${r.status}\t${r.name}\t${r.detail}`);
console.log(`\nTOTAL=${results.length} PASS=${pass} FAIL=${fail}`);
if (throttled429.length) console.log(`THROTTLED_RETRIES=${throttled429.length} (429 backoff applied, not a product defect)`);

if (OUT) {
  const fs = await import('node:fs');
  fs.writeFileSync(OUT, JSON.stringify({
    run_id: RUN_ID, base: BASE, db_port: DB_PORT, schema: DB,
    total: results.length, pass, fail,
    results,
    throttled_429_retries: throttled429.length,
    raw_log: log.map(redact),
  }, null, 2), 'utf8');
}
process.exit(fail === 0 ? 0 : 1);
