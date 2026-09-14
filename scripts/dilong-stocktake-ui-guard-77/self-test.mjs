#!/usr/bin/env node
/**
 * DL-STOCKTAKE-UI-GUARD-77 (R1 返工) 离线自测
 *
 * 要求：调用导出的 assertStocktakeEditingReady 本体，使用真实 Playwright 离线 DOM
 * （page.setContent 构造），不连业务后端、不登录、不写库。
 *
 * 覆盖：
 *   - 混合陷阱：可见但 disabled 的 A + 隐藏但可编辑的 B → 必须 FAIL
 *   - readonly 拒绝
 *   - 登录页陷阱（53 回归）
 *   - redirect 查询串
 *   - 正常通过对照（同一元素 visible+enabled+editable）
 *   - expectedPath 缺省/空 → 抛错拒绝
 */

import { createRequire } from 'node:module';
import { assertStocktakeEditingReady } from './assert-stocktake-ready.mjs';

const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);

const SUFFIX = '实盘数量';

/**
 * 用真实 DOM 构造一个页面；qtyHtml 为数量框区域的 HTML。
 * 通过 route 拦截本地假 origin 的文档请求并回填 HTML，
 * 使真实 URL/pathname 成立（不连业务后端、不发真实网络请求）。
 */
async function makePage(browser, { path, qtyHtml = '' }) {
  const page = await browser.newPage();
  const origin = 'http://127.0.0.1:5199';
  await page.route('**/*', async (route) => {
    const req = route.request();
    if (req.resourceType() === 'document' || req.url().startsWith(origin)) {
      await route.fulfill({
        status: 200,
        contentType: 'text/html; charset=utf-8',
        body: `<!doctype html><html><body>
    <div id="app">
      <h1>盘点</h1>
      <table><tbody>
        ${qtyHtml}
      </tbody></table>
    </div>
  </body></html>`,
      });
    } else {
      await route.abort();
    }
  });
  await page.goto(origin + path, { waitUntil: 'domcontentloaded' });
  return page;
}

let pass = 0; let fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log(`[PASS] ${name} — ${detail}`); }
  else { fail++; console.log(`[FAIL] ${name} — ${detail}`); }
}

/** 可见+可编辑的正常框 */
const READY_INPUT = `<input aria-label="白菜 实盘数量" value="0">`;
/** 可见但 disabled */
const DISABLED_INPUT = `<input aria-label="白菜 实盘数量" value="0" disabled>`;
/** 隐藏（display:none）但可编辑 */
const HIDDEN_EDITABLE_INPUT = `<input aria-label="萝卜 实盘数量" value="0" style="display:none">`;
/** 可见但 readonly */
const READONLY_INPUT = `<input aria-label="白菜 实盘数量" value="0" readonly>`;

const browser = await chromium.launch({ channel: 'msedge', headless: true });

try {
  // ---- 1. 混合陷阱：可见但disabled的A + 隐藏但可编辑的B → FAIL ----
  {
    const page = await makePage(browser, {
      path: '/dashboard/stock-take',
      qtyHtml: `<tr><td>${DISABLED_INPUT}</td><td>${HIDDEN_EDITABLE_INPUT}</td></tr>`,
    });
    const r = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
    check('mixed-trap: visible-disabled + hidden-editable', r.ok === false,
      `ok=${r.ok} ready断言=${r.results.find(x => x.name === 'guard.qty-input-ready-same-element')?.status}`);
    await page.close();
  }

  // ---- 2. readonly 拒绝 ----
  {
    const page = await makePage(browser, {
      path: '/dashboard/stock-take',
      qtyHtml: `<tr><td>${READONLY_INPUT}</td></tr>`,
    });
    const r = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
    check('readonly-rejected', r.ok === false, `ok=${r.ok}`);
    await page.close();
  }

  // ---- 3. 登录页陷阱（53 回归）----
  {
    const page = await makePage(browser, {
      path: '/login',
      qtyHtml: '',
    });
    const r = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
    check('login-page-trap', r.ok === false, `ok=${r.ok} path=${r.pathname}`);
    await page.close();
  }

  // ---- 4. redirect 查询串 ----
  {
    const page = await makePage(browser, {
      path: '/login?redirect=/dashboard/stock-take',
      qtyHtml: '',
    });
    const r = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
    check('redirect-query-rejected', r.ok === false, `ok=${r.ok}`);
    await page.close();
  }

  // ---- 5. 正常通过对照（同一元素 visible+enabled+editable）----
  {
    const page = await makePage(browser, {
      path: '/dashboard/stock-take',
      qtyHtml: `<tr><td>${READY_INPUT}</td></tr>`,
    });
    const r = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
    check('ready-control-passes', r.ok === true, `ok=${r.ok} pass=${r.pass} fail=${r.fail}`);
    await page.close();
  }

  // ---- 6. expectedPath 缺省 → 抛错 ----
  {
    const page = await makePage(browser, { path: '/dashboard/stock-take', qtyHtml: READY_INPUT });
    let threw = false, msg = '';
    try { await assertStocktakeEditingReady(page, {}); }
    catch (e) { threw = true; msg = e.message; }
    check('expectedPath-missing-throws', threw === true, `threw=${threw} msg=${msg.slice(0, 60)}`);
    // 空值同样拒绝
    let threw2 = false;
    try { await assertStocktakeEditingReady(page, { expectedPath: '  ' }); }
    catch { threw2 = true; }
    check('expectedPath-empty-throws', threw2 === true, `threw=${threw2}`);
    await page.close();
  }
} finally {
  await browser.close();
}

console.log(`\nSELF-TEST(real-DOM) pass=*** fail=${fail} total=${pass + fail}`);
process.exit(fail === 0 ? 0 : 1);
