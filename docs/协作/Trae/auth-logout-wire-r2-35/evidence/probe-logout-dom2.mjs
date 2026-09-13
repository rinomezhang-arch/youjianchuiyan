// TR35 DOM localization probe (evidence-only; does NOT modify app source).
// Real avatar dropdown open -> real logout item click -> poll custom modal + dropdown state.
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const PW = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(PW);
const FRONT = process.env.MX_FRONT;
const USER = process.env.MX_USER;
const PASS = process.env.MX_PASS;
const TAG = process.env.TR35_TAG || 'baseline';
const b = await chromium.launch({ channel: 'msedge', headless: true });
const p = await b.newPage();
p.setDefaultTimeout(15000);
await p.goto(FRONT + '/login', { waitUntil: 'domcontentloaded' });
await new Promise((r) => setTimeout(r, 1200));
await p.locator('input[name="yj-account-input"]').fill(USER);
await p.locator('input[name="yj-pwd-input"]').fill(PASS);
await p.getByRole('button', { name: /登录|登 录/ }).first().click();
await p.waitForURL(/\/dashboard/, { timeout: 10000 });
await new Promise((r) => setTimeout(r, 1500));

await p.locator('.el-dropdown .avatar').first().click();
await new Promise((r) => setTimeout(r, 800));
const item = p.locator('.el-dropdown-menu__item').filter({ hasText: /退出|注销|登出|Logout/ }).first();
const box0 = await item.boundingBox();
console.log('item box:', JSON.stringify(box0));

// Snapshot state every 120ms after the REAL click on the dropdown item.
await item.click();
const trace = [];
for (let i = 0; i < 12; i++) {
  const s = await p.evaluate(() => {
    const ov = document.querySelector('.modal-overlay');
    const dlg = document.querySelector('.modal-dialog');
    const menu = document.querySelector('.el-dropdown-menu');
    const mb = document.querySelector('.el-message-box');
    const vis = (el) => { if (!el) return null; const r = el.getBoundingClientRect(); const cs = getComputedStyle(el); return { w: Math.round(r.width), h: Math.round(r.height), display: cs.display, opacity: cs.opacity, z: cs.zIndex }; };
    return { overlay: vis(ov), dialog: vis(dlg), messageBox: vis(mb), menuInDom: !!menu, menuVis: menu ? vis(menu) : null };
  });
  trace.push(s);
  await new Promise((r) => setTimeout(r, 120));
}
console.log('trace[0]:', JSON.stringify(trace[0]));
console.log('trace[3]:', JSON.stringify(trace[3]));
console.log('trace[11]:', JSON.stringify(trace[11]));
const overlayEverShown = trace.some((t) => t.overlay && t.overlay.w > 0 && t.overlay.h > 0);
console.log('RESULT', TAG, 'customModalEverShown=', overlayEverShown);
await p.screenshot({ path: process.env.TR35_SHOT });

// If a confirm is present, exercise CANCEL and assert session survives (acceptance path A).
if (overlayEverShown) {
  await p.locator('.modal-dialog .btn-cancel').first().click();
  await new Promise((r) => setTimeout(r, 600));
  const after = await p.evaluate(() => ({ url: location.pathname, token: !!localStorage.getItem('token'), overlay: !!document.querySelector('.modal-overlay') }));
  console.log('CANCEL path:', JSON.stringify(after));
}
await b.close();
