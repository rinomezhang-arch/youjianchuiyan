#!/usr/bin/env node
/**
 * DL-STOCKTAKE-UI-GUARD-77
 * 盘点操作前真实页面状态只读断言助手（给 Trae66 用）
 *
 * 目的：修复 53 中过弱的断言 `!url.includes('/login')`——
 *   该断言把"被路由守卫踢回登录页"的瞬间误判为成功。
 *
 * 本模块提供：
 *   1) assertStocktakeEditingReady(page, {expectedPath})  —— 硬断言，任一不满足即 FAIL
 *   2) 离线 DOM 夹具测试（--self-test），不需要真实后端/浏览器
 *
 * 只读：不登录、不提交、不写库、不改源码。
 */

import fs from 'node:fs';

/** 断言结果收集器 */
export function createGuard() {
  const results = [];
  let pass = 0, fail = 0;
  const anomalies = [];
  const assert = (name, cond, detail) => {
    if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
    else { fail++; anomalies.push(name); results.push({ name, status: 'FAIL', detail }); }
  };
  return { assert, results, anomalies, summary: () => ({ pass, fail, results, anomalies }) };
}

/** 是否停在登录页（路径或页面指纹任一命中即判定为登录页） */
function isLoginLike(page) {
  let pathname = '';
  try { pathname = new URL(page.url()).pathname; } catch { pathname = String(page.url() || ''); }
  if (/\/(login|signin)\b/i.test(pathname)) return { hit: true, why: `pathname=${pathname}` };
  if (/[?&]redirect=/i.test(page.url())) return { hit: true, why: `redirect-query url=${page.url()}` };
  return { hit: false, why: `pathname=${pathname}` };
}

/**
 * 硬断言：盘点编辑态是否真正就绪。
 * 全部条件为真才算 PASS；任一为假即 FAIL（不做"宽容放行"）。
 *
 * @param {import('playwright-core').Page} page
 * @param {object} opts
 * @param {string} opts.expectedPath  期望的 pathname（如 /dashboard/stock-take）
 * @param {string} [opts.qtyAriaSuffix] 实盘数量输入框 aria-label 后缀，默认「实盘数量」
 */
export async function assertStocktakeEditingReady(page, opts = {}) {
  const g = createGuard();
  const expectedPath = opts.expectedPath || '/dashboard/stock-take';
  const suffix = opts.qtyAriaSuffix || '实盘数量';

  let pathname = '', fullUrl = '';
  try { const u = new URL(page.url()); pathname = u.pathname; fullUrl = u.toString(); }
  catch { pathname = String(page.url() || ''); fullUrl = pathname; }

  // 1. 必须停在期望路径上（而非被踢回 /login）
  g.assert('guard.path-is-expected', pathname === expectedPath,
    `expect=${expectedPath} actual=${pathname}`);

  // 2. 明确拒绝登录页（路径或 redirect 指纹）
  const loginLike = isLoginLike(page);
  g.assert('guard.not-login-page', !loginLike.hit, loginLike.why);

  // 3. 实盘数量输入框必须真实存在
  const qtyInputs = page.locator(`input[aria-label$="${suffix}"]`);
  const qtyCount = await qtyInputs.count().catch(() => 0);
  g.assert('guard.qty-input-present', qtyCount > 0, `count=${qtyCount}`);

  // 4. 至少一个实盘框可见（拒绝隐藏框）
  let visibleCount = 0;
  for (let i = 0; i < qtyCount; i++) {
    if (await qtyInputs.nth(i).isVisible().catch(() => false)) visibleCount++;
  }
  g.assert('guard.qty-input-visible', visibleCount > 0, `visible=${visibleCount}/${qtyCount}`);

  // 5. 至少一个实盘框可编辑（拒绝 disabled/readonly）
  let editableCount = 0;
  for (let i = 0; i < qtyCount; i++) {
    const dis = await qtyInputs.nth(i).isDisabled().catch(() => true);
    const ro = await qtyInputs.nth(i).getAttribute('readonly').catch(() => null);
    if (!dis && ro === null) editableCount++;
  }
  g.assert('guard.qty-input-editable', editableCount > 0, `editable=${editableCount}/${qtyCount}`);

  const out = g.summary();
  return { ...out, url: fullUrl, pathname, ok: out.fail === 0 };
}

/* ------------------------------------------------------------------ *
 * 离线 DOM 夹具自测：不启后端、不连库、不登录
 * ------------------------------------------------------------------ */
if (process.argv.includes('--self-test')) {
  const fixtures = JSON.parse(fs.readFileSync(new URL('./fixtures.json', import.meta.url), 'utf8'));
  let pass = 0, fail = 0;
  for (const fx of fixtures) {
    // 用纯函数复刻断言逻辑，离线可跑
    const got = evalFixture(fx);
    const ok = got.status === fx.expect;
    ok ? pass++ : fail++;
    console.log(`[${ok ? 'PASS' : 'FAIL'}] ${fx.name} expect=${fx.expect} got=${got.status} (${got.detail})`);
  }
  console.log(`\nSELF-TEST pass=${pass} fail=${fail} total=${fixtures.length}`);
  process.exit(fail === 0 ? 0 : 1);
}

/** 离线夹具判定（复刻 assertStocktakeEditingReady 的核心规则） */
function evalFixture(fx) {
  const url = fx.url || '';
  let pathname = '';
  try { pathname = new URL(url).pathname; } catch { pathname = url; }
  const expectedPath = fx.expectedPath || '/dashboard/stock-take';

  if (pathname !== expectedPath) return { status: 'FAIL', detail: `path mismatch ${pathname}` };
  if (/\/(login|signin)\b/i.test(pathname)) return { status: 'FAIL', detail: 'login page' };
  if (/[?&]redirect=/i.test(url)) return { status: 'FAIL', detail: 'redirect query' };
  if (!fx.qtyCount) return { status: 'FAIL', detail: 'no qty input' };
  if (!fx.qtyVisible) return { status: 'FAIL', detail: 'qty input hidden' };
  if (!fx.qtyEditable) return { status: 'FAIL', detail: 'qty input disabled/readonly' };
  return { status: 'PASS', detail: 'all guards satisfied' };
}
