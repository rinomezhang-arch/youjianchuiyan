import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const PASS = process.env.MX_PASS;
const USER = process.env.MX_USER || 'mx_r161213_2';
const b = await chromium.launch({ channel: 'msedge', headless: true });
const p = await b.newPage();
p.setDefaultTimeout(15000);
const api = [];
p.on('request', r => { if (r.url().includes('/api/')) api.push(r.method() + ' ' + r.url().replace(FRONT, '')); });
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise(r => setTimeout(r, 1500));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await new Promise(r => setTimeout(r, 3500));
console.log('URL before logout:', p.url());

const avatar = p.locator('.avatar.el-tooltip__trigger, .el-dropdown .avatar').first();
console.log('avatar count:', await avatar.count().catch(() => 0));
await avatar.click({ force: true }).catch(async (e) => { console.log('avatar click err', e.message.slice(0, 60)); await avatar.evaluate(el => el.click()); });
await new Promise(r => setTimeout(r, 1200));

const item = p.locator('.el-dropdown-menu__item').filter({ hasText: /退出|注销|登出|Logout/ }).first();
console.log('item count:', await item.count().catch(() => 0), 'visible:', await item.isVisible().catch(() => false));

// 记录点击前 URL 与 token
const tokenBefore = await p.evaluate(() => localStorage.getItem('token'));
console.log('token before:', tokenBefore ? tokenBefore.slice(0, 20) + '...' : null);
api.length = 0;

await item.click({ force: true }).catch(async () => { await item.evaluate(el => el.click()); });
await new Promise(r => setTimeout(r, 2500));
console.log('URL after click:', p.url());
const tokenAfter = await p.evaluate(() => localStorage.getItem('token'));
console.log('token after:', tokenAfter ? tokenAfter.slice(0, 20) + '...' : null);
console.log('API during logout:', JSON.stringify(api));
const lsAll = await p.evaluate(() => Object.keys(localStorage));
console.log('LS keys after:', JSON.stringify(lsAll));
// 是否有确认弹窗
const box = await p.evaluate(() => {
  const mb = document.querySelector('.el-message-box');
  return mb ? { found: true, text: mb.textContent.trim().slice(0, 120), vis: mb.getBoundingClientRect().width > 0 } : { found: false };
});
console.log('message-box:', JSON.stringify(box));
await b.close();
