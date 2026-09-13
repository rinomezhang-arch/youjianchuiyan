#!/usr/bin/env node
/**
 * DL-RC-STOCKTAKE-HTTP-R3-53 失败场景复验（仅非法原料 + 跨店各一次）
 *
 * Codex 裁决：前后主明细计数必须完全不变；只跑这两项；任何不符立即 blocked。
 * 连接目标常量锁死；真实集成后端 18080；沿用原隔离库。
 */
import { execFileSync } from 'node:child_process';

const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = '127.0.0.1', PORT = '13318', SCHEMA = 'tr37_stocktake_20260909';
const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const EVID = process.env.MX_EVID || '';
const fs = await import('node:fs');

function sql(q) {
  return execFileSync(MYSQL, ['-h', HOST, '-P', PORT, '-u', 'root', SCHEMA, '-N', '-B', '-e', q], { encoding: 'utf8' }).trim();
}
function num(q) { const v = sql(q); return v === '' ? null : Number(v); }

// ---- 守卫 ----
const port = sql('SELECT @@port');
if (String(port) !== PORT) { console.error('GUARD_FAIL port=' + port); process.exit(2); }
const dd = sql('SELECT @@datadir');
if (!dd.replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317')) { console.error('GUARD_FAIL datadir=' + dd); process.exit(2); }

// ---- 登录（进程内随机口令 -> 通过 python bcrypt 重设 synmgr1）----
function setAndLogin(account, staffId) {
  const py = `
import subprocess,secrets,bcrypt,json,urllib.request
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
print(json.dumps({"code":j.get("code"),"token":d.get("token"),"storeId":d.get("storeId")}))
`;
  return JSON.parse(execFileSync('python', ['-c', py], { encoding: 'utf8' }).trim());
}
const mgr = setAndLogin('synmgr1', 1);
if (mgr.code !== 200) { console.error('LOGIN_FAIL=' + JSON.stringify(mgr)); process.exit(3); }

async function apiPost(path, token, bodyObj) {
  const res = await fetch(FRONT + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(bodyObj),
  });
  let j = null; try { j = await res.json(); } catch {}
  return { status: res.status, code: j?.code, message: j?.message, data: j?.data };
}

// ---- 基线 ----
const before = { master: num('SELECT COUNT(*) FROM stock_take'), detail: num('SELECT COUNT(*) FROM stock_take_detail') };

const out = { before, scenarios: {} };

// 场景A：非法原料
const bad = await apiPost('/api/stock-takes', mgr.token, {
  storeId: 1,
  takeDate: new Date().toISOString().slice(0, 10),
  items: [{ ingredientId: 'NO-SUCH-DL53-XYZ', actualQuantity: 1 }],
});
out.scenarios.illegal_ingredient = { http: bad.status, code: bad.code, message: bad.message };

// 场景B：跨店（店长请求 storeId=2）
const cross = await apiPost('/api/stock-takes', mgr.token, {
  storeId: 2,
  takeDate: new Date().toISOString().slice(0, 10),
  items: [{ ingredientId: sql('SELECT ingredient_id FROM ingredient_master WHERE store_id=1 LIMIT 1') || 'SYN-PORK', actualQuantity: 1 }],
});
out.scenarios.cross_store = { http: cross.status, code: cross.code, message: cross.message };

// ---- 复检 ----
const after = { master: num('SELECT COUNT(*) FROM stock_take'), detail: num('SELECT COUNT(*) FROM stock_take_detail') };
out.after = after;
out.counts_unchanged = (before.master === after.master) && (before.detail === after.detail);

const newRows = sql(`SELECT take_id,store_id,status,remark FROM stock_take WHERE take_id>8 ORDER BY take_id`);
out.new_rows_after_8 = newRows || '(none)';

console.log('\n==== DL-RC-STOCKTAKE-HTTP-R3-53 失败场景复验 ====');
console.log(JSON.stringify(out, null, 2));

if (EVID) {
  fs.writeFileSync(EVID + '/dl53-r3-failscenarios.json', JSON.stringify(out, null, 2), 'utf8');
}
process.exit(out.counts_unchanged ? 0 : 1);
