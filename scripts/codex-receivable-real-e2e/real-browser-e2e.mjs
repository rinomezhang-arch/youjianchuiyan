import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_MODULE)
const frontend = process.env.CX13_FRONTEND
const backend = process.env.CX13_BACKEND
const evidence = process.env.CX13_EVIDENCE
const username = process.env.CX13_USERNAME
const password = process.env.CX13_PASSWORD
if (![frontend, backend, evidence, username, password].every(Boolean)) throw new Error('CX13 runtime environment incomplete')

const steps = []
const network = []
const browserEvents = []
const ids = { receivableId: null, receivableNo: null, paymentIds: [], paymentNos: [] }
let dropFinalPaymentOnce = true

function step(name, ok, detail = '') {
  steps.push({ name, status: ok ? 'PASS' : 'FAIL', detail })
  if (!ok) throw new Error(`${name}: ${detail}`)
}

function safeRequest(pathname, body) {
  if (pathname === '/api/auth/login') return { credentials: 'redacted' }
  return body
}

function safeResponse(pathname, json) {
  if (pathname === '/api/auth/login') return { code: json?.code, tokenPresent: Boolean(json?.data?.token) }
  if (pathname.startsWith('/api/finance/')) return json
  return { code: json?.code }
}

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } })
page.setDefaultTimeout(20000)
page.on('console', message => browserEvents.push({ type: `console:${message.type()}`, text: message.text() }))
page.on('pageerror', error => browserEvents.push({ type: 'pageerror', text: error.message }))
page.on('requestfailed', request => browserEvents.push({
  type: 'requestfailed', url: request.url(), text: request.failure()?.errorText || 'unknown'
}))

await page.route(`${frontend}/api/**`, async route => {
  const req = route.request()
  const source = new URL(req.url())
  const pathname = source.pathname
  const target = `${backend}${pathname}${source.search}`
  let body = null
  try { body = req.postDataJSON() } catch { body = req.postData() }
  const headers = { ...req.headers() }
  delete headers.host
  delete headers.authorization
  const authorization = req.headers().authorization
  if (authorization) headers.authorization = authorization
  const response = await route.fetch({ url: target, headers })
  let json = null
  try { json = await response.json() } catch {}
  const entry = {
    method: req.method(), path: `${pathname}${source.search}`, backendStatus: response.status(),
    authenticated: Boolean(authorization), request: safeRequest(pathname, body), response: safeResponse(pathname, json)
  }
  if (pathname === '/api/finance/receivable' && req.method() === 'POST' && json?.data) {
    ids.receivableId = json.data.receivableId
    ids.receivableNo = json.data.no
  }
  if (pathname === '/api/finance/payment' && req.method() === 'POST' && json?.data) {
    if (!ids.paymentIds.includes(json.data.paymentId)) ids.paymentIds.push(json.data.paymentId)
    if (!ids.paymentNos.includes(json.data.no)) ids.paymentNos.push(json.data.no)
  }
  if (pathname === '/api/finance/payment' && req.method() === 'POST' && body?.amount === '70.00' &&
      dropFinalPaymentOnce && json?.code === 200 && json?.data?.replayed === false) {
    dropFinalPaymentOnce = false
    entry.delivery = 'aborted_after_backend_commit'
    network.push(entry)
    await route.abort('failed')
    return
  }
  entry.delivery = 'fulfilled'
  network.push(entry)
  await route.fulfill({ response })
})

try {
  await page.goto(`${frontend}/login`, { waitUntil: 'networkidle' })
  await page.locator('input[name="yj-account-input"]').fill(username)
  await page.locator('input[name="yj-pwd-input"]').fill(password)
  await page.locator('.login-btn').click()
  await page.waitForURL(/\/dashboard/)
  step('真实登录JWT', Boolean(await page.evaluate(() => localStorage.getItem('token'))), '登录后浏览器未保存JWT')

  await page.goto(`${frontend}/dashboard/finance`)
  await page.getByRole('heading', { name: '客户应收与收款' }).waitFor()
  await page.getByRole('button', { name: '新建应收' }).waitFor({ state: 'visible' })
  step('财务应收组件加载', true)

  await page.getByRole('button', { name: '新建应收' }).click()
  const createDialog = page.getByRole('dialog', { name: '新建应收' })
  await createDialog.getByLabel('应收金额').fill('100.00')
  await createDialog.getByLabel('客户名称').fill('CX13合成客户')
  await createDialog.getByLabel('客户编号').fill('11')
  await createDialog.getByLabel('预订单号').fill('BN-CX13-1')
  await createDialog.getByLabel('备注').fill('CX13真实浏览器应收')
  await createDialog.getByRole('button', { name: '保存应收' }).click()
  await page.getByText('CX13合成客户', { exact: true }).waitFor()
  step('创建应收并列表回读', Number.isSafeInteger(ids.receivableId) && Boolean(ids.receivableNo), `id=${ids.receivableId}`)

  const row = page.locator('.el-table__body tr').filter({ hasText: 'CX13合成客户' }).first()
  await row.getByRole('button', { name: '登记收款' }).click()
  let payDialog = page.getByRole('dialog', { name: '登记收款' })
  await payDialog.getByLabel('收款金额').fill('30.00')
  await payDialog.getByLabel('收款方式').fill('cash')
  await payDialog.getByLabel('收款账户编号').fill('101')
  await payDialog.getByRole('button', { name: '确认登记' }).click()
  await row.getByText('部分收款').waitFor()
  step('部分收款30元', true)

  await row.getByRole('button', { name: '登记收款' }).click()
  payDialog = page.getByRole('dialog', { name: '登记收款' })
  await payDialog.getByLabel('收款金额').fill('70.00')
  await payDialog.getByLabel('收款方式').fill('cash')
  await payDialog.getByLabel('收款账户编号').fill('101')
  await payDialog.getByRole('button', { name: '确认登记' }).click()
  await page.getByText(/结果待核对，请勿重复收款/).waitFor()
  await payDialog.getByRole('button', { name: '取消' }).click()
  step('提交成功后响应丢失并保留恢复记录', !dropFinalPaymentOnce)

  await page.getByRole('button', { name: /恢复原请求/ }).last().click()
  await row.getByText('已收清').waitFor()
  step('同requestId恢复且收清', network.some(x => x.path === '/api/finance/payment' && x.response?.data?.replayed === true))

  const manualPaymentButton = page.getByRole('button', { name: '手工收款' })
  await manualPaymentButton.waitFor({ state: 'visible' })
  await page.waitForFunction(() => {
    const button = [...document.querySelectorAll('button')].find(item => item.textContent?.trim() === '手工收款')
    return button && !button.disabled
  })
  step('收款流水自动核对后解除写入锁', network.some(x => x.method === 'GET' && x.path.startsWith('/api/finance/payment?')))

  await page.reload()
  await page.getByRole('heading', { name: '客户应收与收款' }).waitFor()
  const refreshedRow = page.locator('.el-table__body tr').filter({ hasText: 'CX13合成客户' }).first()
  await refreshedRow.getByText('已收清').waitFor()
  step('刷新后数据库状态回读', true)

  await page.getByRole('button', { name: '手工收款' }).click()
  const manual = page.getByRole('dialog', { name: /手工收款/ })
  await manual.getByLabel('收款金额').fill('12.34')
  await manual.getByLabel('收款账户编号').fill('103')
  await manual.getByLabel(/收款类别/).fill('隔离测试')
  await manual.getByLabel(/业务说明/).fill('停用账户拒绝且保留输入')
  await manual.getByRole('button', { name: '确认登记' }).click()
  await page.getByText(/收款账户不存在、不属于当前门店或已停用/).waitFor()
  const retained = (await manual.getByLabel('收款金额').inputValue()) === '12.34' &&
    (await manual.getByLabel('收款账户编号').inputValue()) === '103' &&
    (await manual.getByLabel(/业务说明/).inputValue()) === '停用账户拒绝且保留输入'
  step('停用账户拒绝且输入保留', retained)

  await page.screenshot({ path: path.join(evidence, 'receivable-paid-and-disabled-rejected.png'), fullPage: true })
} catch (error) {
  steps.push({ name: 'browser-run', status: 'FAIL', detail: error.message })
  await page.screenshot({ path: path.join(evidence, 'browser-failure.png'), fullPage: true }).catch(() => {})
  throw error
} finally {
  const pass = steps.filter(x => x.status === 'PASS').length
  const fail = steps.filter(x => x.status === 'FAIL').length
  fs.writeFileSync(path.join(evidence, 'browser-result.json'), JSON.stringify({ pass, fail, skipped: 0, steps }, null, 2))
  fs.writeFileSync(path.join(evidence, 'browser-events.json'), JSON.stringify(browserEvents, null, 2))
  fs.writeFileSync(path.join(evidence, 'network-redacted.json'), JSON.stringify(network, null, 2))
  fs.writeFileSync(path.join(evidence, 'business-ids.json'), JSON.stringify(ids, null, 2))
  await browser.close()
}

if (steps.some(x => x.status !== 'PASS')) process.exitCode = 1
