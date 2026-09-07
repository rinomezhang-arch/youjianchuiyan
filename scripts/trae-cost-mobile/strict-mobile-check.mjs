// TR-OPS-COST-MOBILE-02 R3 严格移动验收（Trae, 2026-09-07）
// 被测：trae-mobile-cost 工作树 CostRecipe.vue（整合版 a94cc043 + 手机触控修复）
//
// 安全要求：
// - 凭据与地址全部由环境变量注入，缺项列出变量名后安全退出（exit 2），脚本内无任何默认凭据
// - 不打印凭据、token、Authorization 头、认证响应体；日志只含断言名与几何/业务数值
//
// 必需环境变量：
//   COST_E2E_BASE_URL    前端地址，如 http://localhost:5173
//   COST_E2E_API_TARGET  真实后端地址，如 http://127.0.0.1:8081（/api/ 经 route.fetch 服务端转发）
//   COST_E2E_USERNAME    测试账号
//   COST_E2E_PASSWORD    测试密码
//
// 断言口径：双轴几何(x/y/right/bottom 在视口内) + elementFromPoint 中心遮挡命中；
// 真实 scrollLeft/scrollTop 变化 + 滚动后点击；长配方纵向滚动后底部按钮命中；
// 失败分支精确（toast 必须含后端 Out of range，非任意错误/校验 warning）；
// 取消重开精确旧值 500.000、成功重开精确新值 300.000；采集项记 INFO 不计入通过数。
import { createRequire } from 'module'
const require = createRequire('F:/solo/artifacts/team-worktrees/trae/scripts/trae-cost-e2e/')
const { chromium } = require('playwright')
import { mkdirSync } from 'fs'

const required = ['COST_E2E_BASE_URL', 'COST_E2E_API_TARGET', 'COST_E2E_USERNAME', 'COST_E2E_PASSWORD']
const missing = required.filter(k => !process.env[k])
if (missing.length) {
  console.error('SAFETY-ABORT: 缺少环境变量 ' + missing.join(', ') + '；不使用默认凭据，退出。')
  process.exit(2)
}
const BASE = process.env.COST_E2E_BASE_URL.replace(/\/$/, '')
const API = process.env.COST_E2E_API_TARGET.replace(/\/$/, '')
const USER = process.env.COST_E2E_USERNAME
const PASS = process.env.COST_E2E_PASSWORD
const SHOT_DIR = 'f:/solo/screenshots'
mkdirSync(SHOT_DIR, { recursive: true })

const assertions = []
const collections = []
const A = (name, ok, ev) => { assertions.push({ name, ok: !!ok, ev: String(ev) }); console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name} | ${ev}`) }
const I = (name, ev) => { collections.push({ name, ev: String(ev) }); console.log(`[INFO] ${name} | ${ev}`) }

const browser = await chromium.launch({ channel: 'msedge', headless: true })

async function runFlow(vw, vh, tag) {
  const context = await browser.newContext({ viewport: { width: vw, height: vh }, deviceScaleFactor: 2 })
  // /api/ 服务端转发到真实后端；浏览器侧全程同源，不产生 CORS 预检；不记录任何请求头
  await context.route('**/*', async route => {
    const u = new URL(route.request().url())
    if (u.pathname.startsWith('/api/') && u.hostname === new URL(BASE).hostname) {
      u.href = API + u.pathname + u.search
      const resp = await route.fetch({ url: u.toString() })
      await route.fulfill({ response: resp })
    } else {
      await route.continue()
    }
  })
  const page = await context.newPage()
  const apiCalls = []
  page.on('request', req => { const p = new URL(req.url()).pathname; if (p.startsWith('/api/')) apiCalls.push(req.method() + ' ' + p) })
  page.on('console', msg => { if (msg.type() === 'error' && !msg.text().includes('401') && !msg.text().includes('Failed to load resource')) console.log(`[${tag} console.error]`, msg.text().slice(0, 160)) })

  const DLG = '.cost-recipe-dialog:visible'
  const rows = () => page.locator(`${DLG} .recipe-table .el-table__row:visible`)
  const cell = (rowLoc, i) => rowLoc.locator('.el-table__cell').nth(i)
  const qtyInput = rowLoc => cell(rowLoc, 1).locator('.el-input-number input')
  const unitInput = rowLoc => cell(rowLoc, 2).locator('input')
  const yieldInput = rowLoc => cell(rowLoc, 3).locator('.el-input-number input')
  const selectWrap = rowLoc => cell(rowLoc, 0).locator('.el-select__wrapper')
  const addBtn = () => page.locator(`${DLG} .recipe-editor > .el-button`)
  const footerBtn = (primary) => page.locator(`${DLG} .el-dialog__footer .el-button${primary ? '--primary' : ':not(.el-button--primary)'}`)

  // 双轴几何 + 中心遮挡命中
  const geoHit = async (name, loc) => {
    const box = await loc.boundingBox().catch(() => null)
    if (!box) return A(`${tag} ${name}`, false, '无 boundingBox（元素不存在或不可见）')
    const inAxes = box.x >= -1 && box.y >= -1 && box.x + box.width <= vw + 1 && box.y + box.height <= vh + 1
    const cx = box.x + box.width / 2, cy = box.y + box.height / 2
    const handle = await loc.elementHandle()
    const hit = await page.evaluate(({ el, x, y }) => {
      const top = document.elementFromPoint(x, y)
      return { inside: !!el && (el === top || el.contains(top)), topCls: top ? (top.className || '').toString().slice(0, 60) : 'null' }
    }, { el: handle, x: cx, y: cy })
    A(`${tag} ${name}`, inAxes && hit.inside,
      `x=${Math.round(box.x)} y=${Math.round(box.y)} right=${Math.round(box.x + box.width)} bottom=${Math.round(box.y + box.height)} h=${Math.round(box.height)} hit=${hit.inside}${!hit.inside ? ' topAt=' + hit.topCls : ''}`)
    return box
  }
  const heightAtLeast = async (name, loc, min) => {
    const box = await loc.boundingBox().catch(() => null)
    A(`${tag} ${name}`, !!box && box.height >= min, box ? `height=${Math.round(box.height)} need>=${min}` : '元素不可见')
  }
  const scrollMetrics = async (sel) => page.evaluate(s => {
    const el = document.querySelector(s)
    if (!el) return null
    return { l: el.scrollLeft, t: el.scrollTop, sw: el.scrollWidth, cw: el.clientWidth, sh: el.scrollHeight, ch: el.clientHeight }
  }, sel)
  const hScroller = '.cost-recipe-dialog .el-table__body-wrapper .el-scrollbar__wrap'
  const vScroller = '.cost-recipe-dialog .recipe-editor'
  const openDialog = async () => {
    await page.locator('.cost-table-wrapper .el-table__row', { hasText: '清蒸鲈鱼' }).getByRole('button', { name: '编辑配方' }).click()
    await page.waitForSelector(DLG, { timeout: 8000 })
    await page.waitForTimeout(900)
  }
  const closeDialog = async () => {
    await footerBtn(false).click()
    await page.waitForTimeout(700)
  }

  try {
    // ---- 登录（凭据仅用于填充，绝不打印）----
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
    await page.locator('.login-form .el-input').first().locator('input').fill(USER)
    await page.locator('.login-form .el-input input[type=password]').fill(PASS)
    await page.locator('.login-form button.el-button--primary').click()
    await page.waitForURL(u => !u.pathname.startsWith('/login'), { timeout: 10000 })
    A(`${tag} login ok`, !page.url().includes('/login'), 'redirected')

    await page.goto(`${BASE}/dashboard/finance/dish-cost`, { waitUntil: 'networkidle' })
    await page.waitForSelector('.cost-recipe-page', { timeout: 10000 })
    await page.waitForSelector('.cost-table-wrapper .el-table__row', { timeout: 10000 })
    const iw = await page.evaluate(() => window.innerWidth)
    A(`${tag} innerWidth exact`, iw === vw, `innerWidth=${iw}`)

    // 壳层几何：仅采集，不计断言
    const shell = await page.evaluate(() => {
      const r = el => { if (!el) return null; const b = el.getBoundingClientRect(); return { w: Math.round(b.width), left: Math.round(b.left) } }
      return { sidebar: r(document.querySelector('aside.sidebar')), main: r(document.querySelector('.main-content')) }
    })
    I(`${tag} shell geometry (collection)`, JSON.stringify(shell))

    // ---- 列表页控件几何+命中 ----
    await geoHit('page title', page.locator('.cost-recipe-page .page-title').first())
    await geoHit('search input', page.locator('.cost-recipe-page .search-input input'))
    await geoHit('recalc button', page.locator('.page-header-right .el-button'))
    const cards = page.locator('.stats-row .stat-card')
    A(`${tag} stat cards count=4`, await cards.count() === 4, `count=${await cards.count()}`)
    for (let i = 0; i < 4; i++) await geoHit(`stat card ${i + 1}`, cards.nth(i))
    await geoHit('list edit button row1', page.locator('.cost-table-wrapper .el-table__row').first().getByRole('button', { name: '编辑配方' }))
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-01-list.png` })

    // ---- 打开弹窗 ----
    await openDialog()
    A(`${tag} dialog title`, (await page.locator(`${DLG} .el-dialog__title`).textContent()).includes('清蒸鲈鱼'), 'title ok')
    await geoHit('dialog', page.locator(DLG))
    await geoHit('dialog title', page.locator(`${DLG} .el-dialog__title`))
    await geoHit('dialog close X', page.locator(`${DLG} .el-dialog__headerbtn`))
    const r0 = rows().first()
    await geoHit('ingredient select (col1, no scroll)', selectWrap(r0))
    await geoHit('qty input (col2, no scroll)', qtyInput(r0))
    await geoHit('add button', addBtn())
    await geoHit('cancel button', footerBtn(false))
    await geoHit('save button', footerBtn(true))
    if (vw <= 768) {
      await heightAtLeast('touch height: ingredient select', selectWrap(r0), 40)
      await heightAtLeast('touch height: qty number', qtyInput(r0), 40)
    } else {
      const b = await qtyInput(r0).boundingBox()
      I('desktop qty input height (collection)', `height=${Math.round(b.height)}`)
    }
    // 数据流驱动的精确旧值：打开时读当前持久化值，后续回滚断言必须与此精确相等
    const persistedV0 = await qtyInput(r0).inputValue()
    I(`${tag} persisted qty at open (collection)`, persistedV0)
    const targetQty = vw <= 768 ? '300' : '350'
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-02-dialog.png` })

    // ---- 真实横向滚动 + 滚动后点击 ----
    const h0 = await scrollMetrics(hScroller)
    I(`${tag} h-scroll before (collection)`, JSON.stringify(h0))
    await addBtn().click()
    await page.waitForTimeout(400)
    A(`${tag} row added for h-scroll`, await rows().count() === 2, `rows=${await rows().count()}`)
    await page.evaluate(s => { const el = document.querySelector(s); el.scrollTo({ left: el.scrollWidth, behavior: 'instant' }) }, hScroller)
    await page.waitForTimeout(400)
    const h1 = await scrollMetrics(hScroller)
    A(`${tag} h-scroll real movement`, h1 && h1.l > 100 && h1.l >= h1.sw - h1.cw - 2, `scrollLeft 0->${Math.round(h1?.l)} (sw=${h1?.sw} cw=${h1?.cw})`)
    // 最右端：删除按钮（操作列）进入视口并真实点击
    const delLast = rows().last().getByRole('button', { name: '删除' })
    await geoHit('delete button at h-scroll end', delLast)
    await delLast.click()
    await page.waitForTimeout(400)
    A(`${tag} click after h-scroll removed row`, await rows().count() === 1, `rows=${await rows().count()}`)
    // 中间位置：单位/出成率列进入视口（滚到最右时这两列在左侧屏外），几何/命中/触控高度
    await page.evaluate(s => { const el = document.querySelector(s); el.scrollTo({ left: Math.min(250, el.scrollWidth - el.clientWidth), behavior: 'instant' }) }, hScroller)
    await page.waitForTimeout(300)
    const hMid = await scrollMetrics(hScroller)
    I(`${tag} h-scroll mid (collection)`, `scrollLeft=${Math.round(hMid?.l)}`)
    await geoHit('unit input at h-scroll mid', unitInput(rows().first()))
    await geoHit('yield input at h-scroll mid', yieldInput(rows().first()))
    if (vw <= 768) {
      await heightAtLeast('touch height: unit input', unitInput(rows().first()), 40)
      await heightAtLeast('touch height: yield number', yieldInput(rows().first()), 40)
    }
    await page.evaluate(s => { const el = document.querySelector(s); el.scrollTo({ left: 0, behavior: 'instant' }) }, hScroller)
    await page.waitForTimeout(300)
    const h2 = await scrollMetrics(hScroller)
    A(`${tag} h-scroll back to 0`, h2 && h2.l === 0, `scrollLeft=${Math.round(h2?.l)}`)
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-03-hscroll.png` })

    // ---- 长配方纵向滚动 + 底部按钮命中（不保存，取消丢弃）----
    for (let i = 0; i < 12; i++) { await addBtn().click(); await page.waitForTimeout(120) }
    A(`${tag} long recipe rows=13`, await rows().count() === 13, `rows=${await rows().count()}`)
    await page.evaluate(s => { const el = document.querySelector(s); el.scrollTop = 0 }, vScroller)
    await page.waitForTimeout(200)
    const v0 = await scrollMetrics(vScroller)
    I(`${tag} v-scroll at top (collection)`, JSON.stringify(v0))
    await page.evaluate(s => { const el = document.querySelector(s); el.scrollTo({ top: el.scrollHeight, behavior: 'instant' }) }, vScroller)
    await page.waitForTimeout(500)
    const v1 = await scrollMetrics(vScroller)
    A(`${tag} v-scroll real movement`, v0 && v1 && v0.t === 0 && v1.t > 50, `scrollTop ${v0?.t}->${Math.round(v1?.t)} (sh=${v1?.sh} ch=${v1?.ch})`)
    await geoHit('add button at long-recipe bottom', addBtn())
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-04-longrecipe.png` })
    await closeDialog()
    A(`${tag} dialog closed after discard`, await page.locator('.el-overlay:visible').count() === 0, `overlays=${await page.locator('.el-overlay:visible').count()}`)

    // ---- 失败分支（精确：后端 Out of range；输入保留精确值）----
    await openDialog()
    A(`${tag} fail-branch: fresh rows=1`, await rows().count() === 1, `rows=${await rows().count()}`)
    await addBtn().click()
    await page.waitForTimeout(400)
    const nr = rows().last()
    await nr.locator('.el-select').first().click()
    await page.waitForSelector('.el-select-dropdown:visible .el-select-dropdown__item', { timeout: 5000 })
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: 'TR验收_生抽' }).click()
    await qtyInput(nr).fill('99999999')
    await footerBtn(true).click()
    const errToast = page.locator('.el-message--error:visible').first()
    await errToast.waitFor({ state: 'visible', timeout: 8000 })
    const errText = (await errToast.textContent())
    A(`${tag} fail toast is backend Out-of-range branch`, errText.includes('Out of range') && !errText.includes('warning'), errText.slice(0, 110))
    A(`${tag} warn toast absent on backend failure`, (await page.locator('.el-message--warning:visible').count()) === 0, `warnings=${await page.locator('.el-message--warning:visible').count()}`)
    A(`${tag} dialog stays open on failure`, await page.locator(DLG).count() === 1, 'dialog visible')
    const kept = await qtyInput(nr).inputValue()
    A(`${tag} failed input retained exact`, kept === '99999999.000', `qty=${kept}`)
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-05-fail.png` })
    await closeDialog()
    A(`${tag} fail dialog cancel closed`, await page.locator('.el-overlay:visible').count() === 0, `overlays=${await page.locator('.el-overlay:visible').count()}`)

    // ---- 取消重开：精确旧值（打开时采集的持久化值，不接受任意值）----
    await openDialog()
    A(`${tag} rows after rollback`, await rows().count() === 1, `rows=${await rows().count()}`)
    const oldVal = await qtyInput(rows().first()).inputValue()
    A(`${tag} reopen exact persisted value (${persistedV0})`, oldVal === persistedV0, `qty=${oldVal} expected=${persistedV0}`)

    // ---- 成功保存：精确新值（本视口目标值，与旧值不同）----
    A(`${tag} target differs from old`, targetQty + '.000' !== persistedV0, `old=${persistedV0} target=${targetQty}.000`)
    await qtyInput(rows().first()).fill(targetQty)
    await footerBtn(true).click()
    const okToast = page.locator('.el-message--success:visible').first()
    await okToast.waitFor({ state: 'visible', timeout: 8000 })
    const okText = (await okToast.textContent())
    A(`${tag} success toast branch`, okText.includes('配方已保存'), okText.slice(0, 60))
    await page.waitForTimeout(2500)
    A(`${tag} dialog closed after save`, await page.locator('.el-overlay:visible').count() === 0, `overlays=${await page.locator('.el-overlay:visible').count()}`)
    await openDialog()
    const newVal = await qtyInput(rows().first()).inputValue()
    A(`${tag} reopen exact new value ${targetQty}.000`, newVal === targetQty + '.000', `qty=${newVal} expected=${targetQty}.000`)
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-06-readback.png` })
    await closeDialog()

    A(`${tag} zero /dish-cost/ calls`, !apiCalls.some(a => a.includes('/dish-cost/')), `apiCalls=${apiCalls.length}`)
  } catch (e) {
    A(`${tag} SCRIPT ERROR`, false, e.message.slice(0, 260))
    await page.screenshot({ path: `${SHOT_DIR}/trae-mobile-r3-${tag}-error.png` }).catch(() => {})
  } finally {
    await context.close()
  }
}

await runFlow(390, 844, 'm')
await runFlow(1440, 1000, 'd')
const pass = assertions.filter(r => r.ok).length
const fail = assertions.length - pass
console.log(`\n=== ASSERTIONS: PASS=${pass} FAIL=${fail} | COLLECTIONS(info)=${collections.length} ===`)
await browser.close()
process.exit(fail === 0 ? 0 : 1)
