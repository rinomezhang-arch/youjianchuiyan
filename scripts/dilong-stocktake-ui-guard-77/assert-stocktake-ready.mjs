#!/usr/bin/env node
/**
 * DL-STOCKTAKE-UI-GUARD-77 (R1 返工)
 * 盘点操作前真实页面状态只读断言助手（给 Trae66 用）
 *
 * 修复 53 中过弱的断言 `!url.includes('/login')`——
 *   该断言把"被路由守卫踢回登录页"的瞬间误判为成功。
 *
 * 只读：不登录、不提交、不写库、不改源码。
 */

/**
 * 硬断言：盘点编辑态是否真正就绪。任一条件不满足即 FAIL（不做宽容放行）。
 *
 * @param {import('playwright-core').Page} page
 * @param {object} opts
 * @param {string} opts.expectedPath  期望的 pathname，必填；缺省/空值直接拒绝
 * @param {string} [opts.qtyAriaSuffix] 实盘数量输入框 aria-label 后缀，默认「实盘数量」
 */
export async function assertStocktakeEditingReady(page, opts = {}) {
  const expectedPath = (opts && typeof opts.expectedPath === 'string') ? opts.expectedPath.trim() : '';
  if (!expectedPath) {
    throw new Error('assertStocktakeEditingReady: opts.expectedPath is required (no default guessing)');
  }
  const suffix = (opts && opts.qtyAriaSuffix) || '实盘数量';

  const results = [];
  let pass = 0; let fail = 0;
  const anomalies = [];
  const assert = (name, cond, detail) => {
    if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
    else { fail++; anomalies.push(name); results.push({ name, status: 'FAIL', detail }); }
  };

  let pathname = '', fullUrl = '';
  try { const u = new URL(page.url()); pathname = u.pathname; fullUrl = u.toString(); }
  catch { pathname = String(page.url() || ''); fullUrl = pathname; }

  // 1. 必须停在期望路径上（而非被踢回 /login）
  assert('guard.path-is-expected', pathname === expectedPath,
    `expect=${expectedPath} actual=${pathname}`);

  // 2. 明确拒绝登录页（路径或 redirect 指纹）
  const loginByPath = /\/(login|signin)\b/i.test(pathname);
  const loginByRedirect = /[?&]redirect=/i.test(fullUrl);
  assert('guard.not-login-page', !loginByPath && !loginByRedirect,
    `pathHit=${loginByPath} redirectHit=${loginByRedirect}`);

  // 3. 实盘数量输入框必须真实存在
  const qtyInputs = page.locator(`input[aria-label$="${suffix}"]`);
  const qtyCount = await qtyInputs.count().catch(() => 0);
  assert('guard.qty-input-present', qtyCount > 0, `count=${qtyCount}`);

  // 4. 必须存在【同一个】input 同时满足 visible + enabled + editable（非 readonly）
  //    逐个元素合判，避免"可见但disabled的A + 隐藏但可编辑的B"被误放行。
  let readyCount = 0;
  const perEl = [];
  for (let i = 0; i < qtyCount; i++) {
    const el = qtyInputs.nth(i);
    const visible = await el.isVisible().catch(() => false);
    const enabled = await el.isEnabled().catch(() => false);
    const ro = await el.getAttribute('readonly').catch(() => null);
    const editable = enabled && ro === null;
    const ok = visible && enabled && editable;
    if (ok) readyCount++;
    perEl.push({ i, visible, enabled, readonly: ro, ok });
  }
  assert('guard.qty-input-ready-same-element', readyCount > 0,
    `ready=${readyCount}/${qtyCount} perEl=${JSON.stringify(perEl)}`);

  return { pass, fail, results, anomalies, url: fullUrl, pathname, ok: fail === 0 };
}
