import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const PW = "C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core";
const { chromium } = require(PW);
const b = await chromium.launch({ channel: "msedge", headless: true });
const p = await b.newPage();
await p.goto("http://127.0.0.1:5183/login", { waitUntil: "domcontentloaded" });
await new Promise(r=>setTimeout(r,1200));
await p.locator("input[name=\"yj-account-input\"]").fill("mx_mgr1");
const PASS = process.env.MX_PASS;
if (!PASS) throw new Error('缺少 MX_PASS：密码必须运行时注入');
await p.getByRole("button",{name:/登录|登 录/}).first().click();
await new Promise(r=>setTimeout(r,3000));
// 找含"退出"文本的元素的完整祖先链与可点性
const info = await p.evaluate(() => {
  const out = [];
  const walk = (root) => {
    const all = root.querySelectorAll("*");
    for (const el of all) {
      if (el.children.length === 0 && /退出|Logout/.test(el.textContent||"")) {
        let a = el, chain = [];
        for (let i=0;i<6 && a;i++){ chain.push(a.tagName+"."+(a.className||"").toString().slice(0,40)); a=a.parentElement; }
        out.push({ text: (el.textContent||"").trim().slice(0,20), chain });
      }
    }
  };
  walk(document);
  return out;
});
console.log(JSON.stringify(info, null, 2));
await b.close();
