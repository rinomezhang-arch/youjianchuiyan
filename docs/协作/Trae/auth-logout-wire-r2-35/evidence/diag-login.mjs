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
p.on('response', async (r) => {
  if (r.url().includes('/api/')) {
    let body = '';
    try { body = (await r.text()).slice(0, 220); } catch {}
    console.log('API', r.status(), r.request().method(), r.url().replace(FRONT, ''), '=>', body);
  }
});
p.on('console', (m) => { if (m.type() === 'error') console.log('CONSOLE.ERR', m.text().slice(0, 160)); });
p.on('pageerror', (e) => console.log('PAGE.ERR', String(e).slice(0, 160)));
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise((r) => setTimeout(r, 1200));
console.log('inputs:', await p.locator('input[name="yj-account-input"], input[name="yj-pwd-input"]').count());
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
for (let i = 0; i < 6; i++) {
  await new Promise((r) => setTimeout(r, 1000));
  console.log(`t+${i + 1}s url=`, p.url());
}
console.log('LS:', JSON.stringify(await p.evaluate(() => Object.keys(localStorage))));
await p.screenshot({ path: process.env.TR35_SHOT });
await b.close();
