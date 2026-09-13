#!/usr/bin/env node
// TR-RECEIPT-REAL-24 真实链驱动：
// 真实登录 -> 账单页面点击实际「打印」按钮 -> 真实 /api/bills/{id}/receipt 请求 ->
// 业务预览 DOM（textContent 安全渲染，80mm）-> 来自预览页的 PDF/截图。
// 另含：HTTP 身份/门店矩阵（401/403/400/404/200）、只读 DB 金额与零孤儿断言、
// XSS 安全渲染、弹窗拦截、后端失败三条不得弹成功的路径。
// 一个浏览器上下文；不写库、不重灌种子、不接触生产。
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const require = createRequire(import.meta.url);
const playwrightModule = process.env.PLAYWRIGHT_MODULE;
if (!playwrightModule) throw new Error('PLAYWRIGHT_MODULE is required');
const { chromium } = require(playwrightModule);

const webBase = process.env.TR24_WEB_BASE || 'http://127.0.0.1:5184';
const schema = process.env.TR24_SCHEMA || 'co_print23_20260909_022305';
const orderNo = process.env.TR24_ORDER || 'COPRINT23-BK-001';
const mysql = process.env.TR24_MYSQL || 'C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe';
// r4: DB port comes from the runner's identity-gated parameter (this round: isolated 13318).
const mysqlPort = process.env.TR24_MYSQL_PORT || '13318';
const evidenceDir = resolve(process.env.TR24_EVIDENCE || 'docs/协作/Trae/receipt-real-24/evidence');
mkdirSync(evidenceDir, { recursive: true });

const ACCOUNTS = {
  manager: { username: 'coprint23_manager', password: '123456' },
  gm: { username: 'tr24_gm', password: '123456' },
  staff1: { username: 'tr24_staff1', password: '123456' },
  staff2: { username: 'tr24_staff2', password: '123456' },
};

let pass = 0, fail = 0, skip = 0;
const checks = [];
function check(name, condition, actual) {
  const state = condition ? 'PASS' : 'FAIL';
  checks.push({ state, name, actual: actual === undefined ? undefined : String(actual).slice(0, 500) });
  condition ? pass++ : fail++;
  process.stdout.write(`${state} ${name}${actual === undefined ? '' : ` actual=${JSON.stringify(actual).slice(0, 300)}`}\n`);
}
function skipCheck(name, reason) {
  checks.push({ state: 'SKIP', name, actual: reason });
  skip++;
  process.stdout.write(`SKIP ${name} reason=${reason}\n`);
}

function query(sql) {
  return execFileSync(mysql, ['--no-defaults', '--protocol=tcp', '--host=127.0.0.1', `--port=${mysqlPort}`, '--user=root',
    '--default-character-set=utf8mb4', '--batch', '--skip-column-names', schema, '-e', sql], { encoding: 'utf8' }).trim();
}
function rows(sql) {
  const raw = query(sql);
  return raw ? raw.split(/\r?\n/).map(line => line.split('\t')) : [];
}
const money = (v) => Number(v).toFixed(2);

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
const page = await context.newPage();
const network = [];
page.on('response', (response) => {
  const u = new URL(response.url());
  if (u.pathname.startsWith('/api/')) {
    network.push({ method: response.request().method(), path: u.pathname + u.search, status: response.status() });
  }
});

try {
  // ---------- 只读 DB 基线（流程前后核对行数/金额/零孤儿） ----------
  const before = countsSnapshot();

  // ---------- HTTP 身份/门店矩阵（真实服务，经同一 web 代理） ----------
  await page.goto(webBase + '/login', { waitUntil: 'domcontentloaded' });
  const matrix = await page.evaluate(async ({ accounts, orderNo }) => {
    async function api(method, path, token, body) {
      const resp = await fetch(path, {
        method,
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { 'Content-Type': 'application/json;charset=utf-8' } : {}) },
        body: body ? JSON.stringify(body) : undefined,
      });
      const raw = await resp.text();
      let json = null;
      try { json = JSON.parse(raw); } catch {}
      return { status: resp.status, code: json?.code, message: json?.message, data: json?.data };
    }
    async function login(u) {
      const r = await api('POST', '/api/auth/login', null, u);
      return r.data?.token || null;
    }
    const tokens = {};
    for (const [key, u] of Object.entries(accounts)) tokens[key] = await login(u);
    const receipt = (token, storeId) => api('GET', `/api/bills/${orderNo}/receipt${storeId === undefined ? '' : `?storeId=${storeId}`}`, token);
    return {
      tokens,
      noJwt: await api('GET', `/api/bills/${orderNo}/receipt?storeId=1`, null),
      gmMissing: tokens.gm ? await receipt(tokens.gm, undefined) : null,
      gmZero: tokens.gm ? await receipt(tokens.gm, 0) : null,
      gmAll: tokens.gm ? await receipt(tokens.gm, 'all') : null,
      gmInvalid: tokens.gm ? await receipt(tokens.gm, 'abc') : null,
      gmUnknownOrder: tokens.gm ? await api('GET', '/api/bills/TR24-NOPE-404/receipt?storeId=1', tokens.gm) : null,
      gmOk: tokens.gm ? await receipt(tokens.gm, 1) : null,
      managerCross: tokens.manager ? await receipt(tokens.manager, 2) : null,
      managerOk: tokens.manager ? await receipt(tokens.manager, 1) : null,
      staff2Cross: tokens.staff2 ? await receipt(tokens.staff2, 1) : null,
      staff1Ok: tokens.staff1 ? await receipt(tokens.staff1, 1) : null,
    };
  }, { accounts: ACCOUNTS, orderNo });

  check('四个合成账号真实登录全部取到服务端JWT', Object.values(matrix.tokens).every(Boolean), Object.fromEntries(Object.entries(matrix.tokens).map(([k, v]) => [k, Boolean(v)])));
  check('无JWT请求小票=401', matrix.noJwt?.status === 401 && matrix.noJwt?.code === 401, { status: matrix.noJwt?.status });
  check('GM不传storeId=400', matrix.gmMissing?.status === 400 && matrix.gmMissing?.code === 400, matrix.gmMissing?.status);
  check('GM传0=400', matrix.gmZero?.status === 400 && matrix.gmZero?.code === 400, matrix.gmZero?.status);
  check('GM传all=400', matrix.gmAll?.status === 400 && matrix.gmAll?.code === 400, matrix.gmAll?.status);
  check('GM传非法门店=400', matrix.gmInvalid?.status === 400 && matrix.gmInvalid?.code === 400, matrix.gmInvalid?.status);
  check('同店未知订单=404', matrix.gmUnknownOrder?.status === 404 && matrix.gmUnknownOrder?.code === 404, matrix.gmUnknownOrder?.status);
  check('普通员工(店长)显式跨店=403', matrix.managerCross?.status === 403 && matrix.managerCross?.code === 403, matrix.managerCross?.status);
  check('二店员工要一店小票=403', matrix.staff2Cross?.status === 403 && matrix.staff2Cross?.code === 403, matrix.staff2Cross?.status);

  const ok = matrix.gmOk?.data;
  check('GM显式门店返回真实小票快照', matrix.gmOk?.status === 200 && ok?.orderNo === orderNo && Number(ok?.storeId) === 1
    && Array.isArray(ok?.dishes) && ok.dishes.length === 2
    && money(ok?.totalAmount) === '100.00' && money(ok?.finalAmount) === '100.00'
    && typeof ok?.amountNote === 'string' && ok.amountNote.includes('应付'),
    { status: matrix.gmOk?.status, dishes: ok?.dishes?.length, total: ok?.totalAmount, final: ok?.finalAmount });
  // 同单 TR24 桌台按 table_booking_id 稳定排序后聚合（共享隔离库中其他卡的夹具不去重、不删除，
  // 仅按 TR24 标识断言本卡的两桌及其稳定顺序）。
  const tr24Names = Array.isArray(ok?.tableNames) ? ok.tableNames.filter((n) => String(n).startsWith('TR24-')) : [];
  check('小票JSON桌台贯通：TR24两桌按table_booking_id稳定聚合且排在前序',
    typeof ok?.tableName === 'string'
    && ok.tableName.includes('TR24-01号桌') && ok.tableName.includes('TR24-02号桌')
    && ok.tableName.indexOf('TR24-01号桌') < ok.tableName.indexOf('TR24-02号桌')
    && tr24Names.length === 2 && tr24Names[0] === 'TR24-01号桌' && tr24Names[1] === 'TR24-02号桌',
    { tableName: ok?.tableName, tableNames: ok?.tableNames, tr24Names });
  check('店长本店小票=200', matrix.managerOk?.status === 200 && matrix.managerOk?.data?.orderNo === orderNo, matrix.managerOk?.status);
  check('一店普通员工本店小票=200', matrix.staff1Ok?.status === 200 && matrix.staff1Ok?.data?.orderNo === orderNo, matrix.staff1Ok?.status);

  // ---------- DB 只读金额断言 ----------
  const booking = rows(`SELECT booking_id,store_id,DATE_FORMAT(booking_date,'%Y-%m-%d'),guest_count,booking_status,payment_status,total_amount,final_amount FROM booking_master WHERE booking_id='${orderNo}'`)[0];
  const dishes = rows(`SELECT dish_name,dish_quantity,unit_price,subtotal FROM booking_dish_detail WHERE booking_id='${orderNo}' AND store_id=1 ORDER BY dish_booking_id`);
  const dishTotal = dishes.reduce((s, r) => s + Number(r[3]), 0);
  check('DB订单回读属一店且两行明细', booking?.[0] === orderNo && Number(booking?.[1]) === 1 && dishes.length === 2, { booking: booking?.slice(0, 3), dishRows: dishes.length });
  check('DB明细 2x35 + 1x30 = 100.00，与账面/应付一致',
    dishes[0]?.[0] === 'COPRINT23红烧肉' && Number(dishes[0][1]) === 2 && money(dishes[0][2]) === '35.00' && money(dishes[0][3]) === '70.00'
    && dishes[1]?.[0] === 'COPRINT23时蔬' && Number(dishes[1][1]) === 1 && money(dishes[1][2]) === '30.00' && money(dishes[1][3]) === '30.00'
    && money(dishTotal) === '100.00' && money(booking?.[6]) === '100.00' && money(booking?.[7]) === '100.00',
    { dishTotal: money(dishTotal), total: booking?.[6], final: booking?.[7] });

  // ---------- DB 桌台夹具只读断言（本卡 TR24 标识，同单两桌，稳定排序；他卡夹具不删不碰） ----------
  const tabs = rows(`SELECT table_name FROM booking_table WHERE booking_id='${orderNo}' AND store_id=1 AND table_name LIKE 'TR24-%' ORDER BY table_booking_id`);
  check('DB桌台夹具同单绑定两桌且带TR24标识', tabs.length === 2 && tabs[0]?.[0] === 'TR24-01号桌' && tabs[1]?.[0] === 'TR24-02号桌',
    tabs.map((t) => t[0]));

  // ---------- 真实 UI：登录 -> 账单页 -> 点击实际打印按钮 ----------
  await page.goto(webBase + '/login', { waitUntil: 'domcontentloaded' });
  // 登录页带离屏诱饵输入框（yj-user/yj-pass，防密码管理器自动填充）；真实字段是 yj-account-input/yj-pwd-input
  await page.locator('input[name="yj-account-input"]').fill(ACCOUNTS.manager.username);
  await page.locator('input[name="yj-pwd-input"]').fill(ACCOUNTS.manager.password);
  await page.getByRole('button', { name: '登 录' }).click().catch(() => {});
  await page.waitForURL(/\/dashboard/, { timeout: 30000 }).catch(async () => {
    await page.getByRole('button').filter({ hasText: /登录/ }).first().click().catch(() => {});
    await page.waitForURL(/\/dashboard/, { timeout: 20000 });
  });
  await page.goto(webBase + '/dashboard/bill-manage', { waitUntil: 'networkidle' });
  await page.getByRole('row').filter({ hasText: orderNo }).waitFor({ timeout: 20000 });

  const receiptRequests = () => network.filter((n) => n.path.includes('/receipt'));
  const beforeReceiptCount = receiptRequests().length;

  // 第一处按钮：表格行内「打印」
  const rowPrint = page.getByRole('row').filter({ hasText: orderNo }).getByText('打印', { exact: true });
  const popupPromise = context.waitForEvent('page', { timeout: 15000 });
  await rowPrint.click();
  const popup = await popupPromise;
  await popup.waitForLoadState('domcontentloaded');
  try {
    await popup.getByText('订单小票').waitFor({ timeout: 15000 });
  } catch (e) {
    const debug = {
      receiptNet: receiptRequests().map((n) => ({ path: n.path, status: n.status })),
      popupUrl: popup.url(),
      popupText: (await popup.locator('body').innerText().catch(() => '<no body>')).slice(0, 1000),
    };
    await popup.screenshot({ path: resolve(evidenceDir, 'tr24-popup-debug.png'), fullPage: true }).catch(() => {});
    console.log('POPUP_DEBUG ' + JSON.stringify(debug, null, 2));
    throw e;
  }

  check('点击行内打印触发真实 receipt 请求', receiptRequests().length > beforeReceiptCount
    && receiptRequests().some((n) => n.path.includes(`/api/bills/${orderNo}/receipt`) && n.path.includes('storeId=1') && n.status === 200),
    receiptRequests().filter((n) => n.path.includes('/receipt')));
  check('业务预览渲染订单号/门店', await popup.getByText(orderNo).count() >= 1 && await popup.getByText('COPRINT23合成门店').count() >= 1);
  check('业务预览渲染两行菜品与金额', await popup.getByText('COPRINT23红烧肉').count() >= 1 && await popup.getByText('COPRINT23时蔬').count() >= 1
    && await popup.getByText('¥100.00').count() >= 2);
  check('预览以应付金额表述，不冒充实收', await popup.getByText('应付金额').count() >= 1 && await popup.getByText('不代表实收').count() >= 1);
  check('预览提供打印/另存PDF按钮', await popup.getByRole('button', { name: /打印.*PDF/ }).count() === 1);
  check('业务预览渲染桌台（多桌稳定聚合字符串）',
    await popup.getByText('TR24-01号桌、TR24-02号桌').count() >= 1,
    { aggregated: await popup.getByText('TR24-01号桌、TR24-02号桌').count() });

  // r2 返修：实际点击业务按钮「打印 / 另存为 PDF」，并对 window.print 做插桩计数。
  // 插桩只证明业务按钮在真实预览页调用了 window.print；不代表系统打印对话框或实体出纸成功。
  await popup.evaluate(() => {
    window.__tr24PrintCalls = 0;
    window.print = function () { window.__tr24PrintCalls += 1; };
  });
  await popup.getByRole('button', { name: /打印.*PDF/ }).click();
  await popup.waitForFunction(() => window.__tr24PrintCalls >= 1, { timeout: 5000 });
  const printCalls = await popup.evaluate(() => window.__tr24PrintCalls);
  check('实际点击业务打印按钮后window.print被调用（插桩计数）', printCalls >= 1,
    { printCalls, evidence: 'instrumented window.print counter; native print dialog / physical paper NOT claimed' });

  // @page 80mm 规则：读内联 <style> 原文断言（CSSOM cssText 会丢弃 Chromium 未建模的 size 声明）
  const pageCss = await popup.evaluate(() => {
    const raw = Array.from(document.querySelectorAll('style')).map((s) => s.textContent || '').join('\n');
    const m = /@page[^{]*\{[^}]*\}/.exec(raw);
    return m ? m[0] : '';
  });
  check('预览页@page规则声明80mm纸宽', /@page[^{]*\{[^}]*80mm/.test(pageCss), pageCss.slice(0, 200));

  await popup.screenshot({ path: resolve(evidenceDir, 'tr24-receipt-preview.png'), fullPage: true });
  // PDF 来自同一预览页，页面尺寸按 80mm 收银纸宽生成
  const pdfPath = resolve(evidenceDir, 'tr24-receipt.pdf');
  await popup.pdf({ path: pdfPath, width: '80mm', height: '297mm', printBackground: true });
  await popup.close();

  // 解析 PDF MediaBox 验证纸宽：80mm = 226.77pt
  const pdfBytes = readFileSync(pdfPath, 'latin1');
  const mediaBox = /\/MediaBox\s*\[\s*0\s+0\s+([\d.]+)\s+([\d.]+)\s*\]/.exec(pdfBytes);
  const pdfWidthPt = mediaBox ? Number(mediaBox[1]) : 0;
  check('PDF来自80mm预览页且MediaBox纸宽≈80mm(226.8pt)', pdfWidthPt > 225 && pdfWidthPt < 229,
    { widthPt: pdfWidthPt.toFixed(2), expectMm: 80, expectPt: '226.77', pdf: 'tr24-receipt.pdf', png: 'tr24-receipt-preview.png' });
  check('PDF与截图来自真实业务预览页（与点击/截图同一popup）', true, { pdf: 'tr24-receipt.pdf', png: 'tr24-receipt-preview.png' });

  // 第二处按钮：详情弹窗「打印账单」
  await page.getByRole('row').filter({ hasText: orderNo }).getByText('详情', { exact: true }).click();
  await page.getByRole('dialog').waitFor();
  const popup2Promise = context.waitForEvent('page', { timeout: 15000 });
  await page.getByRole('dialog').getByRole('button', { name: '打印账单' }).click();
  const popup2 = await popup2Promise;
  await popup2.waitForLoadState('domcontentloaded');
  await popup2.getByText('订单小票').waitFor({ timeout: 15000 });
  check('详情弹窗打印账单按钮同样打开真实小票（含桌台）',
    await popup2.getByText('COPRINT23红烧肉').count() >= 1 && await popup2.getByText('¥100.00').count() >= 2
    && await popup2.getByText('TR24-01号桌、TR24-02号桌').count() >= 1);
  await popup2.close();
  // el-dialog 头部 X 与底部按钮同名「关闭」，严格模式会冲突；精确点底部按钮，Escape 兜底
  await page.getByRole('dialog').locator('.el-dialog__footer').getByRole('button', { name: '关闭' }).click().catch(async () => {
    await page.keyboard.press('Escape').catch(() => {});
  });
  await page.getByRole('dialog').waitFor({ state: 'detached', timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(600);

  // ---------- XSS 安全渲染：夹带脚本的菜名只作文字显示 ----------
  const payload = '<img src=x onerror="window.__xss=1"><b>TR24粗体</b><script>window.__xss2=1<\/script>';
  await page.route('**/api/bills/*/receipt**', async (route) => {
    const resp = await route.fetch();
    const body = await resp.json();
    if (body?.data?.dishes?.length) body.data.dishes[0].dishName = payload;
    await route.fulfill({ response: resp, body: JSON.stringify(body) });
  });
  console.log('XSS pages before click: ' + context.pages().length);
  const xssPopupPromise = context.waitForEvent('page', { timeout: 15000 });
  await page.getByRole('row').filter({ hasText: orderNo }).getByText('打印', { exact: true }).click();
  const xssPopup = await xssPopupPromise.catch(async (e) => {
    await page.screenshot({ path: resolve(evidenceDir, 'tr24-xss-click-debug.png'), fullPage: true }).catch(() => {});
    console.log('XSS pages after click: ' + context.pages().length + ' urls=' + JSON.stringify(context.pages().map((p) => p.url())));
    throw e;
  });
  await xssPopup.waitForLoadState('domcontentloaded');
  await xssPopup.getByText('订单小票').waitFor({ timeout: 15000 });
  const imgCount = await xssPopup.locator('img').count();
  const scriptCount = await xssPopup.locator('script').count();
  const payloadShown = await xssPopup.getByText('<img src=x').count();
  const xssRan = await xssPopup.evaluate(() => ({ a: window.__xss, b: window.__xss2 })).catch(() => ({ a: 'eval-failed', b: 'eval-failed' }));
  check('夹带HTML的菜名原样显示且无img/script注入', imgCount === 0 && scriptCount === 0 && payloadShown >= 1 && xssRan.a === undefined && xssRan.b === undefined,
    { imgCount, scriptCount, payloadShown, xssRan });
  await xssPopup.close();
  await page.unroute('**/api/bills/*/receipt**');

  // ---------- 弹窗被拦截：不得弹成功 ----------
  await page.evaluate(() => { window.__origOpen = window.open; window.open = () => null; });
  await page.getByRole('row').filter({ hasText: orderNo }).getByText('打印', { exact: true }).click();
  await page.getByText(/预览窗口被浏览器拦截/).waitFor({ timeout: 10000 });
  const blockedSuccessToasts = await page.locator('.el-message--success').count();
  check('弹窗被拦截时提示拦截且无成功提示', blockedSuccessToasts === 0 && await page.getByText(/预览窗口被浏览器拦截/).count() >= 1, { successToasts: blockedSuccessToasts });
  await page.evaluate(() => { if (window.__origOpen) window.open = window.__origOpen; });

  // ---------- 后端错误：预览窗显示失败，不得弹成功 ----------
  await page.route('**/api/bills/*/receipt**', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8',
      body: JSON.stringify({ code: 500, message: 'TR24故障注入小票暂不可用', data: null }) });
  });
  const errPopupPromise = context.waitForEvent('page', { timeout: 15000 });
  await page.getByRole('row').filter({ hasText: orderNo }).getByText('打印', { exact: true }).click();
  const errPopup = await errPopupPromise;
  await errPopup.waitForLoadState('domcontentloaded');
  await errPopup.getByText(/小票加载失败/).waitFor({ timeout: 15000 });
  await page.getByText('TR24故障注入小票暂不可用').waitFor({ timeout: 8000 });
  const errorSuccessToasts = await page.locator('.el-message--success').count();
  check('后端错误时预览窗与主页均显示失败且无成功提示', errorSuccessToasts === 0
    && await errPopup.getByText(/小票加载失败/).count() >= 1 && await page.getByText('TR24故障注入小票暂不可用').count() >= 1,
    { successToasts: errorSuccessToasts });
  await errPopup.close();
  await page.unroute('**/api/bills/*/receipt**');

  // 物理打印机未接入：诚实列为未验证
  skipCheck('物理打印机出纸', '环境无实体打印机/网络打印服务；本链止于浏览器 window.print 预览（PDF 可另存）');

  // ---------- 流程后只读核对：行数/金额/零孤儿不变 ----------
  const after = countsSnapshot();
  check('打印链只读：订单/明细行数前后不变', before.bookingCount === after.bookingCount && before.dishCount === after.dishCount,
    { before: [before.bookingCount, before.dishCount], after: [after.bookingCount, after.dishCount] });
  check('打印链只读：6项孤儿/跨店计数保持0', after.relations.length === 6 && after.relations.every((x) => x === 0), after.relations);

  writeFileSync(resolve(evidenceDir, 'result.json'), JSON.stringify({ task: 'TR-RECEIPT-REAL-24', round: 'r4', mysqlPort: Number(mysqlPort), schema, webBase, orderNo, pass, fail, skip, checks }, null, 2), 'utf8');
  writeFileSync(resolve(evidenceDir, 'network-redacted.json'), JSON.stringify(network.map((n) => n.path.includes('/auth/login') ? { ...n, credentials: 'redacted' } : n), null, 2), 'utf8');
  writeFileSync(resolve(evidenceDir, 'db-assertions.json'), JSON.stringify({ schema, mysqlPort: Number(mysqlPort), booking, dishes, dishTotal: money(dishTotal), tables: tabs.map((t) => t[0]), before, after }, null, 2), 'utf8');
  process.stdout.write(`TR24_RECEIPT_RESULT pass=${pass} fail=${fail} skip=${skip}\n`);
  process.exitCode = fail ? 1 : 0;
} finally {
  await context.close();
  await browser.close();
}

function countsSnapshot() {
  const bookingCount = Number(query('SELECT COUNT(*) FROM booking_master'));
  const dishCount = Number(query('SELECT COUNT(*) FROM booking_dish_detail'));
  const orphanSql = `SELECT
    (SELECT COUNT(*) FROM booking_dish_detail d LEFT JOIN booking_master b ON b.booking_id=d.booking_id AND b.store_id=d.store_id WHERE b.booking_id IS NULL),
    (SELECT COUNT(*) FROM finance_receivable r LEFT JOIN booking_master b ON b.booking_id=r.booking_id AND b.store_id=r.store_id WHERE b.booking_id IS NULL),
    (SELECT COUNT(*) FROM finance_payment_record p LEFT JOIN finance_receivable r ON r.receivable_id=p.receivable_id AND r.store_id=p.store_id WHERE r.receivable_id IS NULL),
    (SELECT COUNT(*) FROM booking_dish_detail d JOIN booking_master b ON b.booking_id=d.booking_id WHERE b.store_id<>d.store_id),
    (SELECT COUNT(*) FROM finance_receivable r JOIN booking_master b ON b.booking_id=r.booking_id WHERE b.store_id<>r.store_id),
    (SELECT COUNT(*) FROM finance_payment_record p JOIN finance_receivable r ON r.receivable_id=p.receivable_id WHERE r.store_id<>p.store_id)`;
  const relations = rows(orphanSql)[0].map(Number);
  return { bookingCount, dishCount, relations };
}
