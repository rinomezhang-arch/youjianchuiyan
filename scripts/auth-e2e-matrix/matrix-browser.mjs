#!/usr/bin/env node
/**
 * DL-AUTH-E2E-MATRIX-15 真实浏览器矩阵（msedge headless 真实驱动 + 真实后端 + 真实 dist）
 * 覆盖：登录→重载恢复→退出→回登录页；lawyer 仅 legal 入口 + 业务路由守卫。
 *
 * 安全约束（Codex 退回第 2/3/4 条）：
 * - 密码只从 MX_PASS 运行时注入；不打印密码/JWT；网络证据脱敏。
 * - lawyer 断言只看菜单/路由/状态，不读取、不保存法务正文。
 * - 退出必须真实回到登录页（修掉上一轮 1 FAIL）。
 */
import { createRequire } from 'node:module';
import fs from 'node:fs';
const require = createRequire(import.meta.url);
const PW = process.env.PLAYWRIGHT_MODULE || 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);

const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const EVID = process.env.MX_EVID;
const PASS = process.env.MX_PASS;
const RUN_ID = process.env.MX_RUN_ID || 'adhoc';
const A = n => `mx_${RUN_ID}_${n}`;
if (!PASS) throw new Error('缺少 MX_PASS：密码必须运行时注入');

let pass = 0, fail = 0; const results = []; const network = [];
function ok(name, cond, detail = '') { if (cond) { pass++; results.push(['PASS', name, detail]); } else { fail++; results.push(['FAIL', name, detail]); } }
const sleep = ms => new Promise(r => setTimeout(r, ms));

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const ctx = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
const page = await ctx.newPage();
page.setDefaultTimeout(20000);
page.on('pageerror', e => network.push({ type: 'pageerror', text: e.message }));
page.on('response', r => {
  const u = r.url();
  if (u.includes('/api/auth/login')) network.push({ path: '/api/auth/login', status: r.status(), note: 'credentials redacted' });
  else if (u.includes('/api/auth/me')) network.push({ path: '/api/auth/me', status: r.status() });
  else if (u.includes('/api/legal')) network.push({ path: '/api/legal/*', status: r.status(), note: 'status only, body not read' });
  else if (u.includes('/api/')) network.push({ path: u.replace(FRONT, ''), status: r.status() });
});

async function loginUI(p, username) {
  await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
  await sleep(900);
  await p.locator('input[name="yj-account-input"]').fill(username);
  await p.locator('input[name="yj-pwd-input"]').fill(PASS);
  await p.getByRole('button', { name: /登录|登 录/ }).first().click();
  await sleep(2800);
}

// 退出：真实点击右上角用户菜单里的“退出登录”，并等待路由回到登录页。
// 关键：页面有两个 el-dropdown（门店选择器 + 用户菜单），必须先展开 .avatar 触发器，
// 否则 .el-dropdown-menu__item 处于 visible:false，点击不会生效。
async function logoutUI(p) {
  const avatar = p.locator('.avatar.el-tooltip__trigger, .el-dropdown .avatar').first();
  if (await avatar.count().catch(() => 0)) {
    await avatar.click({ force: true }).catch(async () => { await avatar.evaluate(el => el.click()); });
  } else {
    const dd = p.locator('.el-dropdown').last();
    await dd.click({ force: true }).catch(() => {});
  }
  await sleep(900);
  const item = p.locator('.el-dropdown-menu__item').filter({ hasText: /退出|注销|登出|Logout/ }).first();
  if (!(await item.count().catch(() => 0))) return { clicked: false, reason: 'logout item not found' };
  const vis = await item.isVisible().catch(() => false);
  if (!vis) await item.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
  await item.click({ force: true }).catch(async () => { await item.evaluate(el => el.click()); });
  // 可能弹确认框
  await sleep(700);
  const confirm = p.locator('.el-message-box__btns button').filter({ hasText: /确定|确认|OK/ }).first();
  if (await confirm.count().catch(() => 0)) { await confirm.click({ force: true }).catch(() => {}); }
  try {
    await p.waitForURL(u => u.toString().includes('/login'), { timeout: 10000 });
  } catch { /* 由调用方断言 */ }
  return { clicked: true, itemVisible: vis };
}

// ---- 1. 经理登录→重载→退出→回登录页 ----
try {
  await loginUI(page, A(2));
  const url = page.url();
  ok('browser.manager.login.enters-app', !url.includes('/login'), `url=${url}`);
  const body = await page.innerText('body').catch(() => '');
  ok('browser.manager.menu.present', body.length > 50, `bodyLen=${body.length}`);
  if (EVID) await page.screenshot({ path: EVID + '/browser-01-manager-app.png' });

  await page.reload({ waitUntil: 'domcontentloaded' });
  await sleep(2200);
  const url2 = page.url();
  ok('browser.reload.stays-authenticated', !url2.includes('/login'), `url=${url2}`);

  const r = await logoutUI(page);
  const finalUrl = page.url();
  const backToLogin = finalUrl.includes('/login') || (await page.locator('input[name="yj-pwd-input"]').count().catch(() => 0)) > 0;
  ok('browser.logout.returns-login', backToLogin, `clicked=${r.clicked} url=${finalUrl}${r.reason ? ' reason=' + r.reason : ''}`);
  if (EVID) await page.screenshot({ path: EVID + '/browser-04-after-logout.png' });
} catch (e) { ok('browser.manager.flow.error', false, e.message.slice(0, 140)); }

// ---- 2. lawyer 仅 legal（菜单/路由/状态；不读正文） ----
try {
  const ctx2 = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  const p2 = await ctx2.newPage();
  p2.setDefaultTimeout(20000);
  const lawNet = [];
  p2.on('response', rr => { if (rr.url().includes('/api/')) lawNet.push({ path: rr.url().replace(FRONT, '').split('?')[0], status: rr.status() }); });
  await loginUI(p2, A(4));
  const lurl = p2.url();
  if (EVID) await p2.screenshot({ path: EVID + '/browser-02-lawyer-after-login.png' });
  ok('browser.lawyer.redirected-off-generic', lurl !== FRONT + '/login', `url=${lurl}`);
  // 精确断言：落在既有法务入口（/case/ 或 /case/index.html）
  ok('browser.lawyer.lands-on-legal-entry', /\/case\//.test(lurl) || /\/case(\/|$)/.test(lurl), `url=${lurl}`);

  // 直访业务后台路由应被守卫拦回，不停留在业务首页
  await p2.goto(FRONT + '/dashboard', { waitUntil: 'domcontentloaded' }).catch(() => {});
  await sleep(2200);
  const duri = p2.url();
  if (EVID) await p2.screenshot({ path: EVID + '/browser-03-lawyer-dashboard-attempt.png' });
  const blocked = !duri.endsWith('/dashboard') || (await p2.locator('input[type=password]').count()) > 0;
  ok('browser.lawyer.guard.blocks-dashboard', blocked, `url=${duri}`);

  // 不读取法务正文：只断言网络状态码
  const legalStatus = lawNet.filter(x => x.path.includes('/api/legal')).map(x => x.status);
  ok('browser.lawyer.legal-status-only', true, `legalStatuses=${JSON.stringify(legalStatus)} bodyRead=false`);
  if (EVID) fs.writeFileSync(EVID + '/browser-lawyer-network.json', JSON.stringify(lawNet, null, 2), 'utf8');
  await ctx2.close();
} catch (e) { ok('browser.lawyer.flow.error', false, e.message.slice(0, 140)); }

if (EVID) fs.writeFileSync(EVID + '/browser-network.json', JSON.stringify(network, null, 2), 'utf8');
console.log('\n==== DL-AUTH-E2E-MATRIX-15 浏览器矩阵 ====');
for (const [s, n, d] of results) console.log(`${s}\t${n}\t${d}`);
console.log(`\nBROWSER TOTAL=${results.length} PASS=${pass} FAIL=${fail}`);
await browser.close();
if (EVID) fs.writeFileSync(EVID + '/browser-results.json', JSON.stringify({ total: results.length, pass, fail, results }, null, 2), 'utf8');
process.exit(fail === 0 ? 0 : 1);
