import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT || 'http://127.0.0.1:5183';
const PASS = process.env.MX_PASS;
const USER = process.env.MX_USER || 'mx_r161213_2';
const b = await chromium.launch({ channel: 'msedge', headless: true });
const p = await b.newPage();
const reqs = [];
p.on('request', r => { if (r.url().includes('/api/')) reqs.push({ method: r.method(), url: r.url(), hdrs: r.headers() }); });
p.on('requestfailed', r => { if (r.url().includes('/api/')) console.log('REQFAILED', r.url(), r.failure()?.errorText); });
p.on('response', async r => {
  if (!r.url().includes('/api/')) return;
  let tx = '';
  try { tx = (await r.text()).slice(0, 200); } catch (e) { tx = 'READ-ERR:' + e.message.slice(0, 60); }
  console.log('RESP', r.status(), r.url().replace(FRONT, ''), '| headers:', JSON.stringify(r.headers()).slice(0, 260), '| body:', tx);
});
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise(r => setTimeout(r, 1500));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
console.log('--- clicking login ---');
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await new Promise(r => setTimeout(r, 4000));
console.log('URL:', p.url());
console.log('REQUESTS:');
for (const r of reqs) console.log('  ', r.method, r.url.replace(FRONT, ''), 'origin=', r.hdrs.origin, 'auth=', r.hdrs.authorization ? 'yes' : 'no');
await b.close();
