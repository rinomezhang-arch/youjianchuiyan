// TR35 confirm-path real-click probe (evidence-only).
// Real open -> real logout item -> real confirm: must clear local identity, hit /api/auth/logout, land /login.
// Second pass: force /api/auth/logout to 500 -> local logout MUST still complete.
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT;
const USER = process.env.MX_USER;
const PASS = process.env.MX_PASS;

async function loginAndOpenConfirm(page) {
  await page.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
  await new Promise((r) => setTimeout(r, 1200));
  await page.locator('input[name="yj-account-input"]').fill(USER);
  await page.locator('input[name="yj-pwd-input"]').fill(PASS);
  await page.getByRole('button', { name: /登录|登 录/ }).first().click();
  await page.waitForURL(/\/dashboard/, { timeout: 10000 });
  await new Promise((r) => setTimeout(r, 1200));
  await page.locator('.el-dropdown .avatar').first().click();
  await new Promise((r) => setTimeout(r, 700));
  await page.locator('.el-dropdown-menu__item').filter({ hasText: /退出|注销|登出|Logout/ }).first().click();
  await page.locator('.modal-dialog .btn-confirm').first().waitFor({ state: 'visible', timeout: 5000 });
}

const b = await chromium.launch({ channel: 'msedge', headless: true });

// Pass 1: normal confirm
{
  const p = await b.newPage();
  const calls = [];
  p.on('response', (r) => { if (r.url().includes('/api/auth/logout')) calls.push(r.status()); });
  await loginAndOpenConfirm(p);
  const before = await p.evaluate(() => Object.keys(localStorage).length);
  await p.locator('.modal-dialog .btn-confirm').first().click();
  await p.waitForURL(/\/login/, { timeout: 10000 }).catch(() => {});
  await new Promise((r) => setTimeout(r, 800));
  const after = await p.evaluate(() => ({ url: location.pathname, ls: Object.keys(localStorage), token: localStorage.getItem('token'), roles: localStorage.getItem('roles') }));
  console.log('CONFIRM normal: logoutStatus=', JSON.stringify(calls), 'lsBefore=', before, 'after=', JSON.stringify(after));
  await p.screenshot({ path: process.env.TR35_SHOT1 });
  await p.close();
}

// Pass 2: server logout fails (500) -> local logout still completes
{
  const p = await b.newPage();
  await p.route('**/api/auth/logout', (route) => route.fulfill({ status: 500, contentType: 'application/json', body: '{"code":500,"message":"forced failure"}' }));
  await loginAndOpenConfirm(p);
  await p.locator('.modal-dialog .btn-confirm').first().click();
  await p.waitForURL(/\/login/, { timeout: 10000 }).catch(() => {});
  await new Promise((r) => setTimeout(r, 800));
  const after = await p.evaluate(() => ({ url: location.pathname, token: localStorage.getItem('token'), roles: localStorage.getItem('roles'), currentStoreId: localStorage.getItem('currentStoreId') }));
  console.log('CONFIRM server-fail: after=', JSON.stringify(after));
  await p.screenshot({ path: process.env.TR35_SHOT2 });
  await p.close();
}

await b.close();
