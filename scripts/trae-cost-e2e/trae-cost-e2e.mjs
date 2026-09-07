// TR-OPS-COST-UI-01 Playwright E2E（Trae, 2026-09-07）
// 可复跑浏览器验证：真实后端 8080 + 隔离 MySQL 13317，非 mock。
// 用法：node trae-cost-e2e.mjs
// 依赖：playwright（本目录独立安装，不污染前端依赖）；浏览器用系统 Edge（channel=msedge）。
// 覆盖：390px 视口 / 零 /dish-cost/* 请求 / 失败保存可见提示+弹窗保留+取消 / 成功保存回读 / 增删行。
import { chromium } from 'playwright'
import { mkdirSync } from 'fs'

const BASE = 'http://localhost:5174'
const SHOT_DIR = 'f:/solo/screenshots'
mkdirSync(SHOT_DIR, { recursive: true })

const results = []
const R = (name, ok, evidence) => { results.push({ name, ok: !!ok, evidence: String(evidence) }) ; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name} | ${evidence}`) }

const apiRequests = []

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 })
const page = await context.newPage()
page.on('request', req => { const u = req.url(); if (u.includes('/api/')) apiRequests.push({ method: req.method(), url: u }) })
page.on('console', msg => { if (msg.type() === 'error') console.log('[console.error]', msg.text().slice(0, 200)) })

try {
  // ---- 登录（干净会话：全新 context 无 localStorage）----
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
  await page.locator('.login-form .el-input').first().locator('input').fill('trae_test')
  await page.locator('.login-form .el-input input[type=password]').fill('002323')
  await page.locator('.login-form button.el-button--primary').click()
  await page.waitForURL(u => !u.pathname.startsWith('/login'), { timeout: 10000 })
  R('login redirect', !page.url().includes('/login'), page.url())

  // ---- innerWidth 390 ----
  const iw = await page.evaluate(() => window.innerWidth)
  R('viewport innerWidth=390', iw === 390, `innerWidth=${iw}`)
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-01-login390.png`, fullPage: false })

  // ---- 旧入口加载 CostRecipe ----
  await page.goto(`${BASE}/dashboard/finance/dish-cost`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.cost-recipe-page', { timeout: 10000 })
  const title = (await page.locator('.page-title').first().textContent()).trim()
  R('old entry renders CostRecipe', title.includes('成本配方'), `title=${title} pathname=${new URL(page.url()).pathname}`)

  // ---- 列表非空（TR验收_ 三菜）----
  await page.waitForSelector('.cost-table-wrapper .el-table__row', { timeout: 10000 })
  const listText = await page.locator('.cost-table-wrapper .el-table').innerText()
  R('list non-empty TR dishes', ['红烧肉', '酸辣土豆丝', '清蒸鲈鱼'].every(d => listText.includes(d)), `has 3 TR dishes; sample=${listText.replace(/\s+/g, ' ').slice(0, 150)}`)
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-02-list390.png`, fullPage: false })

  const dialog = page.locator('.el-dialog:visible').last()
  const openRecipe = async (dishKey) => {
    await page.locator('.cost-table-wrapper .el-table__row', { hasText: dishKey }).getByRole('button', { name: '编辑配方' }).click()
    await page.waitForSelector('.el-dialog:visible', { timeout: 8000 })
    await page.waitForTimeout(500) // 等 el-table 行渲染稳定（count() 不自动等待）
  }
  const closeDialog = async () => {
    await page.locator('.el-dialog:visible').last().getByRole('button', { name: '取消' }).click()
    // el-dialog 关闭后 DOM 不销毁（仅 display:none），等待 overlay 隐藏即可
    await page.waitForTimeout(700)
  }
  const recipeRows = () => page.locator('.el-dialog:visible .recipe-editor .el-table__row:visible')

  // ---- 失败保存：清蒸鲈鱼 + 生抽 99999999 ----
  await openRecipe('清蒸鲈鱼')
  const dlgTitle = await page.locator('.el-dialog:visible .el-dialog__title').last().textContent()
  const rowsBeforeFail = await recipeRows().count()
  R('fail-test dialog open & initial rows', dlgTitle.includes('清蒸鲈鱼') && rowsBeforeFail === 1, `${dlgTitle.trim()} rows=${rowsBeforeFail}`)

  await page.locator('.el-dialog:visible').last().getByRole('button', { name: '添加原料' }).click()
  await page.waitForTimeout(300)
  const newRow = recipeRows().last()
  await newRow.locator('.el-select').first().click()
  await page.waitForSelector('.el-select-dropdown:visible .el-select-dropdown__item', { timeout: 5000 })
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: 'TR验收_生抽' }).click()
  await newRow.locator('.el-input-number input').fill('99999999')
  await page.waitForTimeout(200)
  const qtyVal = await newRow.locator('.el-input-number input').inputValue()
  R('fail-test input prepared', qtyVal.includes('99999999'), `qty input=${qtyVal}`)

  await page.locator('.el-dialog:visible').last().getByRole('button', { name: '保存配方' }).click()

  // ---- 失败 toast 必须实际可见 ----
  let failToast = ''
  try {
    const toast = page.locator('.el-message--error:visible').first()
    await toast.waitFor({ state: 'visible', timeout: 8000 })
    failToast = (await toast.textContent()).trim()
  } catch (e) { failToast = '' }
  R('fail toast VISIBLE with backend message', !!failToast && failToast.includes('保存配方失败'), `toast=${failToast.slice(0, 120)}`)

  await page.waitForTimeout(500)
  const dialogStillOpen = await page.locator('.el-dialog:visible').count()
  const rowsAfterFail = await recipeRows().count()
  const qtyAfterFail = await recipeRows().last().locator('.el-input-number input').inputValue().catch(() => '')
  R('dialog stays open after failure', dialogStillOpen >= 1 && rowsAfterFail === 2, `dialogs=${dialogStillOpen} rows=${rowsAfterFail}`)
  R('failed input retained', qtyAfterFail.includes('99999999'), `retained qty=${qtyAfterFail}`)
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-03-fail-toast390.png`, fullPage: false })

  // ---- 取消可关闭 ----
  await closeDialog()
  await page.waitForTimeout(300)
  const dialogAfterCancel = await page.locator('.el-overlay:visible').count()
  R('cancel closes dialog', dialogAfterCancel === 0, `visible overlays after cancel=${dialogAfterCancel}`)

  // ---- 成功保存 + 回读：清蒸鲈鱼 五花肉改 300 ----
  await openRecipe('清蒸鲈鱼')
  const rowsAfterRollback = await recipeRows().count()
  const qtyAfterRollback = await recipeRows().first().locator('.el-input-number input').inputValue()
  R('readback after rollback: old version kept', rowsAfterRollback === 1 && qtyAfterRollback.startsWith('500'), `rows=${rowsAfterRollback} qty=${qtyAfterRollback}`)

  await recipeRows().first().locator('.el-input-number input').fill('300')
  await page.locator('.el-dialog:visible').last().getByRole('button', { name: '保存配方' }).click()
  let okToast = ''
  try {
    const t = page.locator('.el-message--success:visible').first()
    await t.waitFor({ state: 'visible', timeout: 8000 })
    okToast = (await t.textContent()).trim()
  } catch (e) { okToast = '' }
  R('success toast VISIBLE', !!okToast, `toast=${okToast.slice(0, 100)}`)
  await page.waitForTimeout(2500) // 等弹窗关闭 + recalc-all + 列表刷新

  // 重开回读
  await openRecipe('清蒸鲈鱼')
  const readbackRows = await recipeRows().count()
  const readbackQty = await recipeRows().first().locator('.el-input-number input').inputValue()
  const readbackIng = (await recipeRows().first().innerText()).replace(/\s+/g, ' ').slice(0, 60)
  R('save success readback 300', readbackRows === 1 && readbackQty.startsWith('300'), `rows=${readbackRows} qty=${readbackQty} rowText=${readbackIng}`)
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-04-success-readback390.png`, fullPage: false })
  await closeDialog()

  // ---- 390px 增删行：红烧肉 ----
  await openRecipe('红烧肉')
  const hongRows = await recipeRows().count()
  await page.locator('.el-dialog:visible').last().getByRole('button', { name: '添加原料' }).click()
  await page.waitForTimeout(300)
  const afterAdd = await recipeRows().count()
  await recipeRows().last().getByRole('button', { name: '删除' }).click()
  await page.waitForTimeout(300)
  const afterDel = await recipeRows().count()
  R('390px add/remove row', hongRows === 2 && afterAdd === 3 && afterDel === 2, `initial=${hongRows} afterAdd=${afterAdd} afterDel=${afterDel}`)
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-05-mobile390-dialog.png`, fullPage: false })
  await closeDialog()

  // ---- 网络断言：零 /dish-cost/* ----
  const dishCostHits = apiRequests.filter(r => r.url.includes('/dish-cost/'))
  R('ZERO /dish-cost/* requests', dishCostHits.length === 0, `apiCalls=${apiRequests.length} dishCostHits=${dishCostHits.length}; sample=${apiRequests.slice(0, 8).map(r => r.url.replace(BASE, '')).join(', ')}`)
} catch (e) {
  R('SCRIPT ERROR', false, e.message.slice(0, 300))
  await page.screenshot({ path: `${SHOT_DIR}/trae-cost-pw-error.png`, fullPage: false }).catch(() => {})
} finally {
  const pass = results.filter(r => r.ok).length
  console.log(`\n=== RESULT: PASS=${pass} FAIL=${results.length - pass} ===`)
  console.log(JSON.stringify(results, null, 1))
  await browser.close()
  process.exit(results.every(r => r.ok) ? 0 : 1)
}
