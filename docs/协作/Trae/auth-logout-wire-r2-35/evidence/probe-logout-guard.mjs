// TR35 post-logout guard probe (evidence-only, real clicks + real navigation).
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT;
const USER = process.env.MX_USER;
const PASS = process.env.MX_PASS;
const b = await chromium.launch({ channel: 'msedge', headless: true });
const p = await b.newPage();
p.setDefaultTimeout(15000);
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise((r) => setTimeout(r, 1200));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await p.waitForURL(/\/dashboard/, { timeout: 10000 });
await new Promise((r) => setTimeout(r, 1200));
// real logout through confirm
await p.locator('.el-dropdown .avatar').first().click();
await new Promise((r) => setTimeout(r, 700));
await p.locator('.el-dropdown-menu__item').filter({ hasText: /退出|注销|登出|Logout/ }).first().click();
await p.locator('.modal-dialog .btn-confirm').first().waitFor({ state: 'visible' });
await p.locator('.modal-dialog .btn-confirm').first().click();
await p.waitForURL(/\/login/, { timeout: 10000 });
await new Promise((r) => setTimeout(r, 800));

// (1) browser back
await p.goBack();
await new Promise((r) => setTimeout(r, 1200));
const afterBack = await p.evaluate(() => ({ url: location.pathname, avatar: document.querySelectorAll('.el-dropdown .avatar').length, token: localStorage.getItem('token') }));
console.log('AFTER BACK:', JSON.stringify(afterBack));

// (2) direct protected route
await p.goto(FRONT + '/dashboard/home', { waitUntil: 'domcontentloaded' });
await new Promise((r) => setTimeout(r, 1500));
const afterDirect = await p.evaluate(() => ({ url: location.pathname, avatar: document.querySelectorAll('.el-dropdown .avatar').length, token: localStorage.getItem('token'), hasLoginInput: !!document.querySelector('input[name="yj-account-input"]') }));
console.log('AFTER DIRECT GUARDED URL:', JSON.stringify(afterDirect));
await p.screenshot({ path: process.env.TR35_SHOT });
await b.close();
