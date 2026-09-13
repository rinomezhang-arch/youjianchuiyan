import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const PASS = process.env.MX_PASS;
const USER = process.env.MX_USER || 'mx_r161213_2';
const b = await chromium.launch({ channel: 'msedge', headless: true });
const p = await b.newPage();
const api = [];
p.on('response', async r => {
  const u = r.url();
  if (u.includes('/api/')) {
    let body = '';
    if (u.includes('/api/auth/login')) { try { body = JSON.stringify(await r.json()).slice(0, 300); } catch {} }
    api.push({ u: u.replace(FRONT, ''), s: r.status(), body });
  }
});
p.on('console', m => { if (m.type() === 'error') console.log('CONSOLE-ERR', m.text().slice(0, 200)); });
p.on('pageerror', e => console.log('PAGEERR', e.message.slice(0, 200)));
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise(r => setTimeout(r, 1500));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await new Promise(r => setTimeout(r, 3500));
console.log('URL after login:', p.url());
console.log('API:', JSON.stringify(api, null, 2));
const txt = (await p.innerText('body')).replace(/\s+/g, ' ').slice(0, 300);
console.log('BODY:', txt);
const ls = await p.evaluate(() => Object.fromEntries(Object.entries(localStorage).map(([k, v]) => [k, String(v).slice(0, 40)])));
console.log('LOCALSTORAGE keys:', JSON.stringify(Object.keys(ls)));
// 尝试展开 dropdown，看退出项
const dd = await p.locator('.el-dropdown').count();
console.log('el-dropdown count:', dd);
const outCount = await p.getByText(/退出|注销|登出/).count().catch(() => 0);
console.log('logout-text count:', outCount);
await b.close();
