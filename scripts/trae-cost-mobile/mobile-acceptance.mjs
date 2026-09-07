// TR-OPS-COST-MOBILE-02 移动布局验收（Trae, 2026-09-07）
// 被测：整合版工作树 trae-mobile-cost @a94cc043 + CostRecipe.vue 移动适配
// 服务：整合版后端 8081（dev profile + 隔离库 13317），vite dev 5175
//   vite.config proxy 默认指 8080，禁止改 config；这里在浏览器层把 **/api/** 路由改写到 8081，确保打整合版后端
// Playwright 复用旧工作树已装依赖（任务禁止新装依赖）
// 用法：node mobile-acceptance.mjs
import { createRequire } from 'module'
const require = createRequire('F:/solo/artifacts/team-worktrees/trae/scripts/trae-cost-e2e/')
const { chromium } = require('playwright')
import { mkdirSync } from 'fs'

const SHOT_DIR = 'f:/solo/screenshots'
mkdirSync(SHOT_DIR, { recursive: true })
const results = []
const R = (name, ok, ev) => { results.push({ name, ok: !!ok, ev: String(ev) }); console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name} | ${ev}`) }

const browser = await chromium.launch({ channel: 'msedge', headless: true })

async function runFlow(vw, vh, tag) {
  const context = await browser.newContext({ viewport: { width: vw, height: vh }, deviceScaleFactor: 2 })
  await context.route('**/*', async route => {
    const u = new URL(route.request().url())
    // 只转发真实 API 调用（/api/ 开头）到整合版后端 8081；
    // 用 route.fetch 服务端转发：浏览器保持同源(5175)，避免跨端口 CORS 预检（白名单无 5175）
    if (u.pathname.startsWith('/api/') && u.hostname === 'localhost') {
      u.host = '127.0.0.1:8081'
      // 后端 CORS 白名单含 5174 不含 5175；服务端转发时挂白名单 Origin，
      // 浏览器侧经 route.fulfill 同源返回、不做 CORS 校验
      const headers = { ...route.request().headers(), origin: 'http://localhost:5174', referer: 'http://localhost:5174/' }
      const resp = await route.fetch({ url: u.toString(), headers })
      await route.fulfill({ response: resp })
    } else {
      await route.continue()
    }
  })
  const page = await context.newPage()
  const apiCalls = []
  page.on('request', req => { const p = new URL(req.url()).pathname; if (p.startsWith('/api/')) apiCalls.push(req.method() + ' ' + p) })
  page.on('console', msg => { if (msg.type() === 'error') console.log(`[${tag} console.error]`, msg.text().slice(0, 160)) })

  const inViewport = (r) => r && r.x >= -1 && r.x + r.width <= vw + 1
  const rectOf = async (sel) => { const els = page.locator(sel); const out = []; const n = Math.min(await els.count(), 3); for (let i = 0; i < n; i++) { const e = els.nth(i); if (await e.isVisible().catch(() => false)) out.push(await e.boundingBox()); } return out }
  const assertInView = (name, sel) => new Promise(async resolve => {
    const rects = await rectOf(sel)
    const bad = rects.filter(r => !inViewport(r))
    R(`${tag} ${name}`, rects.length > 0 && bad.length === 0,
      `count=${rects.length} rects=${rects.map(r => `[${Math.round(r.x)},${Math.round(r.x + r.width)}]`).join(' ')} vw=${vw}${bad.length ? ' OUT-OF-VIEW!' : ''}`)
    resolve()
  })

  try {
    await page.goto('http://localhost:5175/login', { waitUntil: 'networkidle' })
    await page.locator('.login-form .el-input').first().locator('input').fill('trae_test')
    await page.locator('.login-form .el-input input[type=password]').fill('002323')
    await page.locator('.login-form button.el-button--primary').click()
    await page.waitForURL(u => !u.pathname.startsWith('/login'), { timeout: 10000 })

    await page.goto('http://localhost:5175/dashboard/finance/dish-cost', { waitUntil: 'networkidle' })
    await page.waitForSelector('.cost-recipe-page', { timeout: 10000 })
    await page.waitForSelector('.cost-table-wrapper .el-table__row', { timeout: 10000 })
    const iw = await page.evaluate(() => window.innerWidth)
    R(`${tag} innerWidth`, iw === vw, `innerWidth=${iw}`)

    // ---- 壳层证据（超范围，只采集不改）----
    const shell = await page.evaluate(() => {
      const sb = document.querySelector('aside.sidebar')
      const main = document.querySelector('.main-content') || document.querySelector('.dashboard-main') || document.querySelector('main')
      const r = el => { if (!el) return null; const b = el.getBoundingClientRect(); return { w: Math.round(b.width), left: Math.round(b.left), cls: el.className } }
      return { sidebar: r(sb), main: r(main) }
    })
    console.log(`[${tag} SHELL]`, JSON.stringify(shell))
    results.push({ name: `${tag} shell-evidence`, ok: true, ev: JSON.stringify(shell), shell: true })

    // ---- 页面控件 boundingRect 断言 ----
    await assertInView('page title visible', '.cost-recipe-page .page-title')
    await assertInView('search input in view', '.cost-recipe-page .search-input input')
    await assertInView('recalc button in view', '.page-header-right .el-button')
    await assertInView('stat cards in view', '.stats-row .stat-card')
    await assertInView('list edit buttons in view', '.cost-table-wrapper .el-table__row .el-button')
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-${tag}-01-list.png` })

    // 列表表格横向滚动能力（明确局部滚动，不撑破页面）
    const listScroll = await page.evaluate(() => {
      const w = document.querySelector('.cost-table-wrapper .el-table__body-wrapper')
      return w ? { sw: w.scrollWidth, cw: w.clientWidth } : null
    })
    R(`${tag} list table scrollable locally`, listScroll && listScroll.sw > 0, `scrollWidth=${listScroll?.sw} clientWidth=${listScroll?.cw} (sw>cw 时局部横滚)`)

    // ---- 打开编辑弹窗 ----
    await page.locator('.cost-table-wrapper .el-table__row', { hasText: '清蒸鲈鱼' }).getByRole('button', { name: '编辑配方' }).click()
    await page.waitForSelector('.cost-recipe-dialog:visible', { timeout: 8000 })
    await page.waitForTimeout(800)

    await assertInView('dialog in view', '.cost-recipe-dialog:visible')
    await assertInView('dialog title visible', '.cost-recipe-dialog .el-dialog__title')
    await assertInView('dialog close X visible', '.cost-recipe-dialog .el-dialog__headerbtn')
    await assertInView('ingredient select in view', '.cost-recipe-dialog .recipe-table .el-table__row .el-select')
    await assertInView('qty input in view', '.cost-recipe-dialog .recipe-table .el-table__row .el-table__cell:nth-child(2) .el-input-number input')
    await assertInView('add-ingredient button in view', '.cost-recipe-dialog .recipe-editor > .el-button')
    await assertInView('footer cancel in view', '.cost-recipe-dialog .el-dialog__footer .el-button')
    await assertInView('footer save in view', '.cost-recipe-dialog .el-dialog__footer .el-button--primary')

    const dialogTitle = await page.locator('.cost-recipe-dialog .el-dialog__title').textContent()
    R(`${tag} dialog title text`, dialogTitle.includes('清蒸鲈鱼'), dialogTitle.trim())
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-${tag}-02-dialog.png` })

    // 弹窗内表格局部横滚
    const dlgScroll = await page.evaluate(() => {
      const w = document.querySelector('.cost-recipe-dialog .el-table__body-wrapper')
      return w ? { sw: w.scrollWidth, cw: w.clientWidth } : null
    })
    R(`${tag} dialog table local scroll`, dlgScroll && dlgScroll.sw >= dlgScroll.cw, `scrollWidth=${dlgScroll?.sw} clientWidth=${dlgScroll?.cw}`)

    // ---- 增删行 ----
    // 用量列是第 2 个 cell（原料=1、用量=2）；出成率列(第4列)也是 input-number，不能直接选
    const qtyInput = rowLoc => rowLoc.locator('.el-table__cell').nth(1).locator('.el-input-number input')
    const rows = () => page.locator('.cost-recipe-dialog .recipe-table .el-table__row:visible')
    const before = await rows().count()
    await page.locator('.cost-recipe-dialog .recipe-editor > .el-button').click()
    await page.waitForTimeout(400)
    const afterAdd = await rows().count()
    await rows().last().getByRole('button', { name: '删除' }).click()
    await page.waitForTimeout(400)
    const afterDel = await rows().count()
    R(`${tag} add/remove row`, before >= 1 && afterAdd === before + 1 && afterDel === before, `${before} -> ${afterAdd} -> ${afterDel}`)

    // ---- 失败保存：输入保留 + 可见提示 + 弹窗保留 + 取消 ----
    await page.locator('.cost-recipe-dialog .recipe-editor > .el-button').click()
    await page.waitForTimeout(400)
    const nr = rows().last()
    await nr.locator('.el-select').first().click()
    await page.waitForSelector('.el-select-dropdown:visible .el-select-dropdown__item', { timeout: 5000 })
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: 'TR验收_生抽' }).click()
    await qtyInput(nr).fill('99999999')
    await page.locator('.cost-recipe-dialog .el-dialog__footer .el-button--primary').click()
    let failToast = ''
    try { const t = page.locator('.el-message--error:visible').first(); await t.waitFor({ state: 'visible', timeout: 8000 }); failToast = (await t.textContent()).trim() } catch { failToast = '' }
    R(`${tag} fail toast visible`, !!failToast && failToast.length > 4, failToast.slice(0, 100))
    await page.waitForTimeout(400)
    const dlgOpen = await page.locator('.cost-recipe-dialog:visible').count()
    const kept = await qtyInput(nr).inputValue().catch(() => '')
    R(`${tag} dialog stays + input retained`, dlgOpen === 1 && kept.includes('99999999'), `dialogOpen=${dlgOpen} qty=${kept}`)
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-${tag}-03-fail.png` })
    await page.locator('.cost-recipe-dialog .el-dialog__footer .el-button').first().click()
    await page.waitForTimeout(700)
    const overlayGone = await page.locator('.el-overlay:visible').count()
    R(`${tag} cancel closes`, overlayGone === 0, `visible overlays=${overlayGone}`)

    // ---- 成功保存 + 回读 ----
    await page.locator('.cost-table-wrapper .el-table__row', { hasText: '清蒸鲈鱼' }).getByRole('button', { name: '编辑配方' }).click()
    await page.waitForSelector('.cost-recipe-dialog:visible', { timeout: 8000 })
    await page.waitForTimeout(800)
    const rollbackQty = await qtyInput(rows().first()).inputValue()
    R(`${tag} rollback readback old kept`, rollbackQty.startsWith('500') || rollbackQty.startsWith('300'), `qty=${rollbackQty}`)
    await qtyInput(rows().first()).fill('300')
    await page.locator('.cost-recipe-dialog .el-dialog__footer .el-button--primary').click()
    let okToast = ''
    try { const t = page.locator('.el-message--success:visible').first(); await t.waitFor({ state: 'visible', timeout: 8000 }); okToast = (await t.textContent()).trim() } catch { okToast = '' }
    R(`${tag} success toast`, !!okToast, okToast.slice(0, 60))
    await page.waitForTimeout(2500)
    await page.locator('.cost-table-wrapper .el-table__row', { hasText: '清蒸鲈鱼' }).getByRole('button', { name: '编辑配方' }).click()
    await page.waitForSelector('.cost-recipe-dialog:visible', { timeout: 8000 })
    await page.waitForTimeout(800)
    const rbQty = await qtyInput(rows().first()).inputValue()
    R(`${tag} save readback 300`, rbQty.startsWith('300'), `readback qty=${rbQty}`)
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-${tag}-04-readback.png` })
    await page.locator('.cost-recipe-dialog .el-dialog__footer .el-button').first().click()
    await page.waitForTimeout(500)

    R(`${tag} zero /dish-cost/ calls`, !apiCalls.some(a => a.includes('/dish-cost/')), `apiCalls=${apiCalls.length}, sample=${apiCalls.slice(0, 6).join(', ')}`)
  } catch (e) {
    R(`${tag} SCRIPT ERROR`, false, e.message.slice(0, 300))
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-${tag}-error.png` }).catch(() => {})
  } finally {
    await context.close()
  }
}

await runFlow(390, 844, 'm')
await runFlow(1280, 800, 'd')
const pass = results.filter(r => r.ok).length
console.log(`\n=== RESULT: PASS=${pass} FAIL=${results.length - pass} ===`)
await browser.close()
process.exit(results.every(r => r.ok) ? 0 : 1)
