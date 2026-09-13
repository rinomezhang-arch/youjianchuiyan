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
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise(r => setTimeout(r, 1500));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await new Promise(r => setTimeout(r, 3500));
console.log('URL:', p.url());

// 找所有含“退出”文本的元素及其祖先链/可见性
const info = await p.evaluate(() => {
  const out = [];
  for (const el of document.querySelectorAll('*')) {
    const own = [...el.childNodes].filter(n => n.nodeType === 3).map(n => n.textContent).join('').trim();
    if (/退出|注销|登出|Logout/.test(own)) {
      let chain = [], a = el;
      for (let i = 0; i < 6 && a; i++) { chain.push(a.tagName + '.' + String(a.className || '').slice(0, 50)); a = a.parentElement; }
      const r = el.getBoundingClientRect();
      out.push({ text: own.slice(0, 20), visible: r.width > 0 && r.height > 0, chain });
    }
  }
  return out;
});
console.log('LOGOUT ELEMENTS:', JSON.stringify(info, null, 2));

// 看 dropdown 相关
const dd = await p.evaluate(() => {
  const els = [...document.querySelectorAll('.el-dropdown, .el-dropdown-link, .el-dropdown-menu, .el-avatar, [class*=user], [class*=avatar]')];
  return els.slice(0, 20).map(e => ({ cls: String(e.className).slice(0, 60), text: (e.textContent || '').trim().slice(0, 25), vis: e.getBoundingClientRect().width > 0 }));
});
console.log('DROPDOWN-ish:', JSON.stringify(dd, null, 2));
await b.close();
