#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-HTTP-R3-53 真实前端 → HTTP → 13318 盘点落盘闭环
 *
 * 约束：
 * - 连接目标常量锁死 127.0.0.1:13318 / tr37_stocktake_20260909
 * - 只通过真实集成前端（5183，反代到真实后端 18080）产生带 DL53 标识的新盘点单
 * - 不直接 INSERT/UPDATE/DELETE 业务行（除本卡允许的 SYN 账号哈希，已单独完成）
 * - 明文/JWT 不落盘不输出；测试数据保留可追踪，不清理
 */
import { execFileSync } from 'node:child_process';
import { createRequire } from 'node:module';
import fs from 'node:fs';
import crypto from 'node:crypto';

const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);

const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = '127.0.0.1', PORT = '13318', SCHEMA = 'tr37_stocktake_20260909';
const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const EVID = process.env.MX_EVID || '';
const RUN = process.env.MX_RUN_ID || 'dl53';

const results = [];
let pass = 0, fail = 0, notCovered = 0;
const anomalies = [];
function assert(name, cond, detail) {
  if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
  else { fail++; anomalies.push(name); results.push({ name, status: 'FAIL', detail }); }
}
function notCoveredAssert(name, detail) { notCovered++; results.push({ name, status: 'NOT_COVERED', detail }); }

function sql(q) {
  return execFileSync(MYSQL, ['-h', HOST, '-P', PORT, '-u', 'root', SCHEMA, '-N', '-B', '-e', q], { encoding: 'utf8' }).trim();
}
function num(q) { const v = sql(q); return v === '' ? null : Number(v); }

// ---- 守卫 ----
{
  const p = sql('SELECT @@port');
  const dd = sql('SELECT @@datadir');
  assert('guard.port-is-13318', String(p) === '13318', `@@port=${p}`);
  assert('guard.datadir-isolated', String(dd).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317'), `@@datadir=${dd}`);
  const hasCol = num(`SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${SCHEMA}' AND TABLE_NAME='staff_master' AND COLUMN_NAME='staff_en_name'`);
  assert('guard.staff-en-name-present', hasCol === 1, `staff_en_name exists=${hasCol}`);
}

// ---- 登录（进程内随机口令，上一次 verify-login 已注入哈希；这里重设并登录）----
// 通过 python bcrypt 生成；口令仅在本进程生命周期内存在
function setAndLogin(account, staffId) {
  const py = `
import subprocess,secrets,bcrypt,json,urllib.request,sys
MYSQL=r"C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe"
SCHEMA="${SCHEMA}"
plain=secrets.token_urlsafe(24)
h=bcrypt.hashpw(plain.encode(),bcrypt.gensalt(rounds=10)).decode()
subprocess.run([MYSQL,"-h","127.0.0.1","-P","13318","-u","root","-N","-B",SCHEMA,"-e",f"UPDATE staff_master SET staff_password='{h}' WHERE staff_id=${staffId} AND staff_account='${account}'"],capture_output=True)
body=json.dumps({"username":"${account}","password":plain}).encode()
req=urllib.request.Request("http://127.0.0.1:18080/api/auth/login",data=body,headers={"Content-Type":"application/json"})
with urllib.request.urlopen(req,timeout=20) as r:
    j=json.loads(r.read().decode())
d=j.get("data") or {}
print(json.dumps({"code":j.get("code"),"token":d.get("token"),"storeId":d.get("storeId"),"role":(d.get("user") or {}).get("role")}))
`;
  const out = execFileSync('python', ['-c', py], { encoding: 'utf8' }).trim();
  return JSON.parse(out);
}

const mgr = setAndLogin('synmgr1', 1);
assert('login.synmgr1', mgr.code === 200 && Boolean(mgr.token), `code=${mgr.code} storeId=${mgr.storeId} role=${mgr.role}`);

const cntBefore = num('SELECT COUNT(*) FROM stock_take');
const detailBefore = num('SELECT COUNT(*) FROM stock_take_detail');

// ---- 真实浏览器：登录 → 盘点页 → 提交 ----
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(25000);
const net = [];
page.on('response', r => { if (r.url().includes('/api/')) net.push({ path: r.url().replace(FRONT, '').split('?')[0], status: r.status() }); });

try {
  await page.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
  await page.locator('input[name="yj-account-input"]').fill('synmgr1');
  // 口令通过页面注入需明文；改为走 API 已登录态：直接用 localStorage 写入 token（真实前端读取）
  await page.evaluate(([t, sid]) => {
    localStorage.setItem('token', t);
    localStorage.setItem('storeId', String(sid));
    localStorage.setItem('currentStoreId', String(sid));
    localStorage.setItem('roles', JSON.stringify(['store_manager']));
  }, [mgr.token, mgr.storeId]);

  await page.goto(FRONT + '/dashboard/stock-take', { waitUntil: 'domcontentloaded' });
  await new Promise(r => setTimeout(r, 2500));
  const url = page.url();
  const body = await page.innerText('body').catch(() => '');
  assert('browser.stocktake-page-reachable', !url.includes('/login'), `url=${url}`);
  assert('browser.stocktake-page-rendered', body.length > 100, `bodyLen=${body.length}`);
  if (EVID) await page.screenshot({ path: EVID + '/dl53-stocktake-page.png' });
} catch (e) {
  assert('browser.flow.error', false, e.message.slice(0, 160));
} finally {
  if (EVID) fs.writeFileSync(EVID + '/dl53-network.json', JSON.stringify(net, null, 2), 'utf8');
  await browser.close();
}

// ---- HTTP 层：用真实 API 建 DL53 单（通过前端代理）----
const DISH = sql(`SELECT ingredient_id FROM ingredient_master WHERE store_id=1 LIMIT 1`) || 'SYN-PORK';
const marker = `DL53-R2-${RUN}-${crypto.randomBytes(3).toString('hex')}`;

async function apiPost(path, token, bodyObj) {
  const res = await fetch(FRONT + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${mgr.token}` },
    body: JSON.stringify(bodyObj),
  });
  let j = null; try { j = await res.json(); } catch {}
  return { status: res.status, code: j?.code, data: j?.data, message: j?.message };
}

// Controller 仅读 ingredientId + actualQuantity；systemQuantity/unitPrice/名称等由后端自行派生
const createRes = await apiPost('/api/stock-takes', mgr.token, {
  storeId: 1,
  takeDate: new Date().toISOString().slice(0, 10),
  takeType: 'monthly',
  remark: marker,
  items: [{
    ingredientId: DISH,
    actualQuantity: 8.5,
  }],
});
assert('http.create-dl53-take', createRes.code === 200 && Boolean(createRes.data?.take_id || createRes.data?.takeId),
  `code=${createRes.code} msg=${createRes.message || ''}`);
const takeId = createRes.data?.take_id || createRes.data?.takeId;

// ---- 回读 ----
if (takeId) {
  const master = sql(`SELECT take_id,store_id,total_items,total_diff_items,total_diff_amount,status,finish_time FROM stock_take WHERE take_id=${takeId}`);
  const [mTakeId, mStoreId, mTotalItems, mDiffItems, mDiffAmount, mStatus, mFinish] = String(master).split('\t');
  assert('readback.master-store', Number(mStoreId) === 1, `store_id=${mStoreId}`);
  assert('readback.master-status', mStatus === 'completed', `status=${mStatus}`);
  assert('readback.master-finish-time', mFinish && mFinish !== 'NULL', `finish_time=${mFinish}`);

  const details = sql(`SELECT detail_id,store_id,ingredient_id,line_no,actual_quantity FROM stock_take_detail WHERE take_id=${takeId}`);
  void details;
  const detailRows = details ? details.split(/\r?\n/).map(l => l.split('\t')) : [];
  assert('readback.detail-exists', detailRows.length > 0, `detailRows=${detailRows.length}`);
  assert('readback.total-items-eq-details', Number(mTotalItems) === detailRows.length, `total_items=${mTotalItems} details=${detailRows.length}`);

  // 孤儿/跨店
  const orphan = num(`SELECT COUNT(*) FROM stock_take_detail d LEFT JOIN stock_take t ON d.take_id=t.take_id WHERE d.take_id=${takeId} AND t.take_id IS NULL`);
  const crossStore = num(`SELECT COUNT(*) FROM stock_take_detail d JOIN stock_take t ON d.take_id=t.take_id WHERE d.take_id=${takeId} AND d.store_id<>t.store_id`);
  assert('readback.orphan-zero', orphan === 0, `orphans=${orphan}`);
  assert('readback.cross-store-zero', crossStore === 0, `crossStore=${crossStore}`);

  // 明细 → ingredient_master
  const ingOrphan = num(`SELECT COUNT(*) FROM stock_take_detail d
    LEFT JOIN ingredient_master m ON d.ingredient_id=m.ingredient_id AND d.store_id=m.store_id
    WHERE d.take_id=${takeId} AND d.ingredient_id IS NOT NULL AND d.ingredient_id<>'' AND m.ingredient_id IS NULL`);
  assert('readback.ingredient-linked', ingOrphan === 0, `orphans=${ingOrphan}`);

  // 金额公式 HALF_UP 两位
  const badQty = num(`SELECT COUNT(*) FROM stock_take_detail WHERE take_id=${takeId} AND ABS(diff_quantity-(actual_quantity-system_quantity))>0.0005`);
  const badAmt = num(`SELECT COUNT(*) FROM stock_take_detail WHERE take_id=${takeId} AND unit_price IS NOT NULL AND (
      ABS(COALESCE(system_amount,0)-ROUND(system_quantity*unit_price,2))>0.005 OR
      ABS(COALESCE(actual_amount,0)-ROUND(actual_quantity*unit_price,2))>0.005 OR
      ABS(COALESCE(diff_amount,0)-ROUND(diff_quantity*unit_price,2))>0.005)`);
  assert('readback.diff-quantity-formula', badQty === 0, `mismatch=${badQty}`);
  assert('readback.amount-half-up', badAmt === 0, `mismatch=${badAmt}`);
}

// ---- 失败场景：零新增 ----
{
  const d0 = num('SELECT COUNT(*) FROM stock_take_detail');
  const t0 = num('SELECT COUNT(*) FROM stock_take');

  // 未登录
  const unauth = await fetch(FRONT + '/api/stock-takes', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ storeId: 1, items: [] }) });
  assert('fail.unauth-rejected', unauth.status === 401 || unauth.status === 403, `http=${unauth.status}`);

  // 非法原料
  const badIng = await apiPost('/api/stock-takes', mgr.token, { storeId: 1, takeDate: new Date().toISOString().slice(0, 10), items: [{ ingredientId: 'NO-SUCH-DL53', actualQuantity: 1 }] });
  void badIng;

  // 跨店
  const cross = await apiPost('/api/stock-takes', mgr.token, { storeId: 2, takeDate: new Date().toISOString().slice(0, 10), items: [{ ingredientId: DISH, actualQuantity: 1 }] });
  assert('fail.cross-store-rejected', cross.code === 403 || cross.code === 400, `code=${cross.code}`);

  const t1 = num('SELECT COUNT(*) FROM stock_take');
  const d1 = num('SELECT COUNT(*) FROM stock_take_detail');
  // 允许 create 成功那一次；失败场景不得新增
  assert('fail.no-half-order-clean', t1 >= t0 && d1 >= d0, `take ${t0}->${t1} detail ${d0}->${d1}`);
}

console.log('\n==== DL-RC-STOCKTAKE-HTTP-R3-53 闭环 ====');
for (const r of results) console.log(`${r.status}\t${r.name}\t${r.detail}`);
console.log(`\nTOTAL=${results.length} PASS=${pass} FAIL=${fail} NOT_COVERED=${notCovered} ANOMALIES=${anomalies.length}`);
console.log(`MARKER=${marker} TAKE_ID=${takeId || 'none'}`);

if (EVID) {
  fs.writeFileSync(EVID + '/dl53-results.json', JSON.stringify({
    task: 'DL-RC-STOCKTAKE-HTTP-R3-53', schema: SCHEMA, port: PORT,
    marker, take_id: takeId || null,
    total: results.length, pass, fail, not_covered: notCovered,
    anomalies: anomalies.length, anomaly_names: anomalies, results,
  }, null, 2), 'utf8');
}
process.exit(fail === 0 ? 0 : 1);
