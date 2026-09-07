// TR-RECEIPT-QUICK-01 隔离合成 API 浏览器验收
// 仅使用当前源 Vite + Playwright 路由拦截合成响应，不连真实后端、无生产网络。
// 凭据无关（合成 token）。
import { spawn } from 'node:child_process'
import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FRONTEND = 'F:/solo/artifacts/team-worktrees/trae-mobile-cost/frontend_v3'
const require = createRequire('F:/solo/artifacts/team-worktrees/trae/scripts/trae-cost-e2e/')
const { chromium } = require('playwright')

const PORT = 5191
const BASE = `http://127.0.0.1:${PORT}`

let pass = 0, fail = 0, info = 0
const I = (n, d) => { info++; console.log(`[INFO] ${n} | ${d}`) }
const A = (n, cond, d) => { if (cond) { pass++; console.log(`[PASS] ${n} | ${d}`) } else { fail++; console.log(`[FAIL] ${n} | ${d}`) } }

// ---- 启动 Vite（当前源）----
const vite = spawn('node', [path.join(FRONTEND, 'node_modules/vite/bin/vite.js'), '--port', PORT, '--strictPort'], {
  cwd: FRONTEND, stdio: ['ignore', 'pipe', 'pipe'], shell: false
})
let viteReady = false
vite.stdout.on('data', d => { const s = d.toString(); if (s.includes('ready') || s.includes('Local:')) viteReady = true })
vite.stderr.on('data', () => {})
await new Promise(r => { const t = setInterval(() => { if (viteReady) { clearInterval(t); r() } }, 200); setTimeout(() => { clearInterval(t); r() }, 25000) })
I('vite started', `port=${PORT} ready=${viteReady}`)
if (!viteReady) { console.log('vite failed to start'); process.exit(2) }

const browser = await chromium.launch({ headless: true, channel: 'msedge' })
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
// 预置登录态与门店
await context.addInitScript(() => {
  localStorage.setItem('token', 'synthetic-test-token')
  localStorage.setItem('storeId', '1')
  localStorage.setItem('currentStoreId', '1')
})

const ING = [{ ingredientId: 'ING-1', ingredientName: '五花肉', purchaseUnit: '千克', unitPrice: 25.5 }]
const SUP = [{ supplierId: 1, supplierName: '鲜肉供应商' }]

// 每个场景独立 page + 独立请求计数
async function newPage(counters) {
  const page = await context.newPage()
  const apiCalls = []
  let ingredientsDelay = 0
  let postStatus = 200
  page.route('**/*', async route => {
    const url = route.request().url()
    const u = new URL(url)
    if (!u.pathname.startsWith('/api/')) return route.continue()
    apiCalls.push({ method: route.request().method(), path: u.pathname, body: route.request().postData() })
    if (u.pathname === '/api/auth/me') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: { name: '测试验收员', username: 'tester', storeId: 1, storeName: '宁国店' } }) })
    if (u.pathname === '/api/kitchen-supply/goods-receipts' && route.request().method() === 'GET') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: [] }) })
    if (u.pathname === '/api/ingredients') { if (ingredientsDelay) await new Promise(r => setTimeout(r, ingredientsDelay)); return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: ING }) }) }
    if (u.pathname === '/api/suppliers') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: SUP }) })
    if (u.pathname === '/api/kitchen-supply/goods-receipts' && route.request().method() === 'POST') {
      if (postStatus === 200) return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: { receiptId: 'R-001' } }) })
      return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ code: 500, message: '服务器内部错误' }) })
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: [] }) })
  })
  counters.page = page
  counters.apiCalls = apiCalls
  counters.setIngredientsDelay = ms => { ingredientsDelay = ms }
  counters.setPostStatus = s => { postStatus = s }
  await page.goto(`${BASE}/dashboard/receipt`, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-testid="new-receipt"]', { timeout: 10000 })
  return page
}

const openDialog = async (c) => { await c.page.locator('[data-testid="new-receipt"]').click(); await c.page.waitForSelector('.el-dialog:visible .receipt-form', { timeout: 8000 }); await c.page.waitForTimeout(400) }
const selectSupplier = async (c) => {
  const s = c.page.locator('.el-dialog:visible .header-fields .el-select').first()
  await s.click()
  const opt = c.page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '鲜肉供应商' }).first()
  await opt.waitFor({ state: 'visible', timeout: 5000 })
  await opt.click()
  await c.page.waitForTimeout(200)
}
const selectIngredient = async (c, row = 0) => {
  const sel = c.page.locator('.el-dialog:visible .el-table .el-select').nth(row)
  await sel.click()
  const opt = c.page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '五花肉' }).first()
  await opt.waitFor({ state: 'visible', timeout: 5000 })
  await opt.click()
  await c.page.waitForTimeout(250)
}
const setQty = async (c, v, row = 0) => { const inp = c.page.locator('.el-dialog:visible .el-table .el-input-number').nth(row * 2).locator('input'); await inp.fill(String(v)) }
const setPrice = async (c, v, row = 0) => { const inp = c.page.locator('.el-dialog:visible .el-table .el-input-number').nth(row * 2 + 1).locator('input'); await inp.fill(String(v)) }
const clickSave = async (c) => { await c.page.locator('.el-dialog:visible .el-dialog__footer .el-button--primary').click() }
const qtyValue = async (c, row = 0) => c.page.locator('.el-dialog:visible .el-table .el-input-number').nth(row * 2).locator('input').inputValue()
const priceValue = async (c, row = 0) => c.page.locator('.el-dialog:visible .el-table .el-input-number').nth(row * 2 + 1).locator('input').inputValue()
const toastText = async (c) => { try { return (await c.page.locator('.el-message:visible').first().textContent({ timeout: 3000 })) || '' } catch { return '' } }

// ===== 场景1：重复打开互斥（慢响应下双击只触发一次加载，草稿不被迟到请求清空）=====
{
  const c = {}
  const page = await newPage(c)
  c.setIngredientsDelay(600)
  const before = c.apiCalls.length
  await page.locator('[data-testid="new-receipt"]').click()
  await page.waitForTimeout(80)
  await page.locator('[data-testid="new-receipt"]').click({ force: true }) // 第二次：被 opening 互斥拦截（按钮loading中，强制触发处理函数）
  await page.waitForSelector('.el-dialog:visible .receipt-form', { timeout: 8000 })
  await page.waitForTimeout(300)
  const ingCalls = c.apiCalls.filter(x => x.path === '/api/ingredients')
  const supCalls = c.apiCalls.filter(x => x.path === '/api/suppliers')
  A('double-click fires single ingredients load', ingCalls.length === 1, `ingredients calls=${ingCalls.length}`)
  A('double-click fires single suppliers load', supCalls.length === 1, `suppliers calls=${supCalls.length}`)
  await selectIngredient(c)
  await setQty(c, 1.25)
  await page.waitForTimeout(300)
  // 没有第二个加载请求，草稿不会被迟到响应覆盖
  const q = await qtyValue(c)
  A('draft quantity retained after slow open', q === '1.25', `qty=${q}`)
  // 防御性代次守卫存在性（源码证据）
  const src = await page.evaluate(() => document.querySelector('.receipt-page').__vueParentComponent?.type?.__file || '')
  I('component source file (collection)', src || 'unknown')
  await page.close()
}

// ===== 场景2：正常提交精确回读（1.25 / 2.34567891 / supplierId=1）=====
{
  const c = {}
  const page = await newPage(c)
  await openDialog(c)
  await selectSupplier(c)
  await selectIngredient(c)
  await setQty(c, 1.25)
  await setPrice(c, 2.34567891)
  const beforePost = c.apiCalls.filter(x => x.path === '/api/kitchen-supply/goods-receipts' && x.method === 'POST').length
  await clickSave(c)
  await page.waitForTimeout(800)
  const posts = c.apiCalls.filter(x => x.path === '/api/kitchen-supply/goods-receipts' && x.method === 'POST')
  A('normal save fires exactly one POST', posts.length === beforePost + 1, `posts=${posts.length}`)
  const body = posts.length ? JSON.parse(posts[posts.length - 1].body) : null
  A('POST supplierId is current store supplier', body && Number(body.receipt.supplierId) === 1, `supplierId=${body?.receipt?.supplierId}`)
  A('POST quantity exact 1.25', body && Number(body.items[0].actualQuantity) === 1.25, `qty=${body?.items?.[0]?.actualQuantity}`)
  A('POST unitPrice exact 2.34567891', body && Math.abs(Number(body.items[0].unitPrice) - 2.34567891) < 1e-9, `price=${body?.items?.[0]?.unitPrice}`)
  const tt = await toastText(c)
  A('success toast on normal save', tt.includes('已保存待验收单'), tt.slice(0, 40))
  A('dialog closed after success', await page.locator('.el-dialog:visible').count() === 0, `overlays=${await page.locator('.el-dialog:visible').count()}`)
  await page.close()
}

// ===== 场景3：缺供应商零 POST（定位字段 + 禁止提交）=====
{
  const c = {}
  const page = await newPage(c)
  await openDialog(c)
  // 不选供应商
  await selectIngredient(c)
  await setQty(c, 1.25)
  await setPrice(c, 2.34567891)
  const beforePost = c.apiCalls.filter(x => x.path === '/api/kitchen-supply/goods-receipts' && x.method === 'POST').length
  await clickSave(c)
  await page.waitForTimeout(600)
  const posts = c.apiCalls.filter(x => x.path === '/api/kitchen-supply/goods-receipts' && x.method === 'POST')
  A('missing supplier: zero POST', posts.length === beforePost, `posts=${posts.length}`)
  const tt = await toastText(c)
  A('missing supplier: warning toast', tt.includes('有效供应商'), tt.slice(0, 40))
  A('missing supplier: dialog stays open', await page.locator('.el-dialog:visible').count() === 1, `overlays=${await page.locator('.el-dialog:visible').count()}`)
  await page.close()
}

// ===== 场景4：失败输入保留（POST 500 后弹窗与输入值不丢）=====
{
  const c = {}
  const page = await newPage(c)
  c.setPostStatus(500)
  await openDialog(c)
  await selectSupplier(c)
  await selectIngredient(c)
  await setQty(c, 1.25)
  await setPrice(c, 2.34567891)
  await clickSave(c)
  await page.waitForTimeout(800)
  const tt = await toastText(c)
  A('failure: error toast shown', /失败|错误|500|status code/.test(tt), tt.slice(0, 50))
  A('failure: dialog stays open', await page.locator('.el-dialog:visible').count() === 1, `overlays=${await page.locator('.el-dialog:visible').count()}`)
  const q = await qtyValue(c)
  const p = await priceValue(c)
  A('failure: quantity retained 1.25', q === '1.25', `qty=${q}`)
  A('failure: unitPrice retained 2.34567891', p === '2.34567891', `price=${p}`)
  await page.close()
}

console.log(`\n=== ASSERTIONS: PASS=${pass} FAIL=${fail} | COLLECTIONS(info)=${info} ===`)
await browser.close()
vite.kill()
process.exit(fail === 0 ? 0 : 1)
