#!/usr/bin/env node
/**
 * DL-AUTH-E2E-MATRIX-15 真实 HTTP 契约探测（只读探测，非最终断言）
 * 目的：摸清后端在真实隔离库下的实际行为，再据此写正式矩阵。
 * 不打印密码/JWT。所有请求走真实后端 18080 + 真实 MySQL 13317。
 */
const BASE = process.env.MX_BASE || 'http://127.0.0.1:18080';
const PASS = process.env.MX_PASS;
if (!PASS) throw new Error('缺少 MX_PASS：密码必须运行时注入');

function redact(obj) {
  if (!obj || typeof obj !== 'object') return obj;
  const c = Array.isArray(obj) ? [...obj] : { ...obj };
  for (const k of Object.keys(c)) {
    if (/token|password|secret/i.test(k)) c[k] = c[k] ? '***' : c[k];
    else if (typeof c[k] === 'object') c[k] = redact(c[k]);
  }
  return c;
}

async function login(username) {
  const res = await fetch(BASE + '/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: PASS }),
  });
  const j = await res.json().catch(() => null);
  return { status: res.status, code: j?.code, role: j?.data?.user?.role, storeId: j?.data?.storeId, token: j?.data?.token };
}

async function api(method, path, token) {
  const res = await fetch(BASE + path, {
    method, headers: token ? { Authorization: 'Bearer ' + token } : {},
  });
  let j = null; try { j = await res.json(); } catch {}
  return { status: res.status, code: j?.code, message: j?.message, dataKind: Array.isArray(j?.data) ? `array[${j.data.length}]` : typeof j?.data };
}

const out = {};
for (const [label, user] of Object.entries({ gm: 'mx_gm', mgr1: 'mx_mgr1', mgr2: 'mx_mgr2', lawyer: 'mx_lawyer', dead: 'mx_dead' })) {
  const l = await login(user);
  out[label] = { loginStatus: l.status, loginCode: l.code, role: l.role, storeId: l.storeId, hasToken: Boolean(l.token) };
  if (l.token) {
    out[label].me = await api('GET', '/api/auth/me', l.token);
    out[label].stores = await api('GET', '/api/stores', l.token);
    out[label].financeAccount = await api('GET', '/api/finance/account?storeId=1', l.token);
    out[label].financeAccountS2 = await api('GET', '/api/finance/account?storeId=2', l.token);
    out[label].staffList = await api('GET', '/api/staff?storeId=1', l.token);
    out[label].legalCase = await api('GET', '/api/legal/case', l.token);
  }
}
console.log(JSON.stringify(redact(out), null, 2));
