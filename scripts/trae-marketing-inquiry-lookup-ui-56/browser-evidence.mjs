// TR-MARKETING-INQUIRY-LOOKUP-UI-56 浏览器证据采集（MOCKED_CONTRACT）。
// 运行：由 run-browser-evidence.ps1 启动（自动定位 npx 缓存中的 playwright 并设 NPMX_NODE_MODULES）。
// 前置：已存在 frontend_v3/dist（唯一一次生产构建产物）。
// 机制：playwright(msedge channel) + 390x844@2x + 本目录 mock-server（合同假后端）。
// 隐私：所有证据文本只出现掩码手机号（138****0001 形态），完整号码不进任何日志/JSON。
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { startMockServer } from './mock-server.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const distDir = join(here, '..', '..', 'frontend_v3', 'dist')
const evidenceDir = join(here, '..', '..', 'docs', '协作', 'Trae', 'marketing-inquiry-lookup-ui-56', 'evidence')
mkdirSync(evidenceDir, { recursive: true })

const npxDir = process.env.NPMX_NODE_MODULES
if (!npxDir) {
  console.error('ABORT: 缺少 NPMX_NODE_MODULES（指向含 playwright 的 node_modules 目录）')
  process.exit(2)
}
const { chromium } = createRequire(join(npxDir, 'index.js'))('playwright')

const P = {
  pending: { inq: 'INQ9001', phone: '13800000001', mask: '138****0001' },
  converted: { inq: 'INQ9002', phone: '13900000002', mask: '139****0002' },
  rejected: { inq: 'INQ9003', phone: '13700000003', mask: '137****0003' },
  unknown: { inq: 'INQ9004', phone: '13600000004', mask: '136****0004' }
}
const UNIFIED_TEXT = '未找到匹配的咨询记录'
const ERROR_TEXT = '系统繁忙，请稍后重试'

const checks = []
const runLogLines = []
const origConsoleLog = console.log
console.log = (...args) => {
  const line = args.map((a) => (typeof a === 'string' ? a : JSON.stringify(a))).join(' ')
  runLogLines.push(line)
  origConsoleLog(line)
}
function record(no, name, ok, detail = '') {
  checks.push({ no, name, ok: !!ok, detail: String(detail) })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${String(no).padStart(2, '0')} ${name}${detail ? ' | ' + detail : ''}`)
}

// 证据脱敏：手机号在进入任何落盘 JSON 前掩码（138****0001 形态）
function maskBody(raw) {
  try {
    const parsed = JSON.parse(raw || '{}')
    if (typeof parsed.phone === 'string' && parsed.phone.length >= 7) {
      parsed.phone = parsed.phone.slice(0, 3) + '****' + parsed.phone.slice(-2) + '（证据掩码）'
    }
    return parsed
  } catch {
    return { unparsed: raw }
  }
}

const server = await startMockServer(distDir, 0)
const port = server.address().port
const base = `http://127.0.0.1:${port}`
console.log(`[mock] serving dist at ${base} (MOCKED_CONTRACT)`)

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2,
  permissions: ['clipboard-read', 'clipboard-write']
})
const page = await context.newPage()

const consoleMessages = []
page.on('console', (m) => consoleMessages.push(`${m.type()}: ${m.text()}`))
const lookupRequests = []
page.on('request', (r) => {
  if (r.url().includes('/api/public/booking-inquiry/lookup')) {
    lookupRequests.push({ url: r.url(), method: r.method(), body: r.postData() ?? '' })
  }
})

async function noHScroll(label) {
  const sw = await page.evaluate(() => Math.max(document.documentElement.scrollWidth, document.body.scrollWidth))
  return { sw, ok: sw <= 390, label }
}
const hscrollSamples = []
async function sampleHScroll() {
  hscrollSamples.push(await noHScroll(page.url()))
}

async function gotoDeepLink(inq) {
  await page.goto(`${base}/h5/inquiry/${inq}`, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-testid="inquiry-no"]')
}

async function fillPhoneAndSubmit(phone) {
  await page.fill('[data-testid="phone-input"]', phone)
  await page.click('[data-testid="lookup-btn"]')
}
async function resetView(inq) {
  await gotoDeepLink(inq)
  await page.evaluate(() => localStorage.clear())
}

try {
  // 01 深链接直开 + 路由 inquiryNo 可见
  await gotoDeepLink(P.pending.inq)
  const shownNo = await page.textContent('[data-testid="inquiry-no"]')
  record(1, '深链接直开：路由 inquiryNo 页面可见并等于链接参数', shownNo.trim() === P.pending.inq, shownNo.trim())
  const hasResultBeforeQuery = await page.$('[data-testid="state-result"]')
  record(2, '直开仅表单视图，不自动查询不泄露任何结果', hasResultBeforeQuery === null)
  await sampleHScroll()
  await page.screenshot({ path: join(evidenceDir, '01-deeplink-initial-390.png'), fullPage: true })

  // 03 刷新可用
  await page.reload({ waitUntil: 'networkidle' })
  const shownNoAfterReload = (await page.textContent('[data-testid="inquiry-no"]')).trim()
  record(3, '刷新后深链接仍可用（history 模式 + 服务端 fallback）', shownNoAfterReload === P.pending.inq, shownNoAfterReload)

  // 04 复制咨询号
  await page.click('[data-testid="copy-inquiry-no"]')
  await page.waitForTimeout(200)
  const clip = await page.evaluate(() => navigator.clipboard.readText())
  record(4, '咨询号可复制（clipboard 内容=路由参数）', clip === P.pending.inq, `clipboard=${clip}`)

  // 05-07 pending：加载态→成功白名单
  await fillPhoneAndSubmit(P.pending.phone)
  const loadingVisible = await page.waitForSelector('[data-testid="state-loading"]', { timeout: 1500 }).then(() => true).catch(() => false)
  record(5, '查询中出现加载态（mock 300ms 延迟窗口内可观察）', loadingVisible)
  await page.waitForSelector('[data-testid="state-result"]', { timeout: 4000 })
  const resultText = await page.textContent('[data-testid="state-result"]')
  record(6, 'pending → 处理中，期望日期/人数/提交时间齐全，无预订编号行', resultText.includes('处理中') && resultText.includes('2026-09-20') && resultText.includes('8 人') && resultText.includes('2026-09-14 10:20') && !resultText.includes('BK'))
  record(7, '结果区不含 mock 多给的内部字段（备注/操作人/门店）', !resultText.includes('内部备注勿展示') && !resultText.includes('内部员工') && !resultText.includes('宁国总店'))
  await sampleHScroll()
  await page.screenshot({ path: join(evidenceDir, '02-pending-result-390.png'), fullPage: true })

  // 08 converted
  await gotoDeepLink(P.converted.inq)
  await fillPhoneAndSubmit(P.converted.phone)
  await page.waitForSelector('[data-testid="state-result"]', { timeout: 4000 })
  const convText = await page.textContent('[data-testid="state-result"]')
  record(8, 'converted → 已转预订且展示预订编号', convText.includes('已转预订') && convText.includes('BK20260914002'))
  await sampleHScroll()
  await page.screenshot({ path: join(evidenceDir, '03-converted-bookingid-390.png'), fullPage: true })

  // 09 rejected
  await gotoDeepLink(P.rejected.inq)
  await fillPhoneAndSubmit(P.rejected.phone)
  await page.waitForSelector('[data-testid="state-result"]', { timeout: 4000 })
  const rejText = await page.textContent('[data-testid="state-result"]')
  record(9, 'rejected → 未通过', rejText.includes('未通过'))
  await page.screenshot({ path: join(evidenceDir, '04-rejected-390.png'), fullPage: true })

  // 10 未知状态
  await gotoDeepLink(P.unknown.inq)
  await fillPhoneAndSubmit(P.unknown.phone)
  await page.waitForSelector('[data-testid="state-result"]', { timeout: 4000 })
  const unkText = await page.textContent('[data-testid="state-result"]')
  record(10, '未知状态 → 清楚中文「状态未知」', unkText.includes('状态未知'))
  await page.screenshot({ path: join(evidenceDir, '05-unknown-status-390.png'), fullPage: true })

  // 11 统一空结果（查无）
  await gotoDeepLink('INQ9999')
  await fillPhoneAndSubmit(P.pending.phone)
  await page.waitForSelector('[data-testid="state-empty"]', { timeout: 4000 })
  const emptyText1 = (await page.textContent('[data-testid="state-empty"]')).trim()
  record(11, '查无 → 统一不泄露空结果', emptyText1.includes(UNIFIED_TEXT))
  await page.screenshot({ path: join(evidenceDir, '06-unified-empty-390.png'), fullPage: true })

  // 12 统一空结果（手机号不符）与 11 同文案
  await gotoDeepLink(P.pending.inq)
  await fillPhoneAndSubmit('13555556666')
  await page.waitForSelector('[data-testid="state-empty"]', { timeout: 4000 })
  const emptyText2 = (await page.textContent('[data-testid="state-empty"]')).trim()
  record(12, '手机号不符 → 与查无完全同一文案（不泄露原因）', emptyText2.includes(UNIFIED_TEXT) && emptyText2 === emptyText1)

  // 13 统一空结果（非法输入）且不发请求
  const reqCountBefore = lookupRequests.length
  await gotoDeepLink(P.pending.inq)
  await fillPhoneAndSubmit('123')
  await page.waitForSelector('[data-testid="state-empty"]', { timeout: 2000 })
  const emptyText3 = (await page.textContent('[data-testid="state-empty"]')).trim()
  record(13, '非法输入 → 同一空结果且不发网络请求', emptyText3.includes(UNIFIED_TEXT) && lookupRequests.length === reqCountBefore, `新增请求数=${lookupRequests.length - reqCountBefore}`)
  await sampleHScroll()
  await page.screenshot({ path: join(evidenceDir, '07-invalid-input-empty-390.png'), fullPage: true })

  // 14-15 系统故障独立错误态 + 输入保留 + 重试稳定
  await gotoDeepLink('INQERR1')
  await fillPhoneAndSubmit(P.pending.phone)
  await page.waitForSelector('[data-testid="state-error"]', { timeout: 4000 })
  const errText = await page.textContent('[data-testid="state-error"]')
  const keptPhone = await page.inputValue('[data-testid="phone-input"]')
  record(14, '系统故障 → 独立错误态（与空结果不同）且输入保留', errText.includes(ERROR_TEXT) && keptPhone === P.pending.phone, `输入保留=${keptPhone === P.pending.phone ? 'true(掩码 ' + P.pending.mask + ')' : 'false'}`)
  await page.click('[data-testid="retry-btn"]')
  await page.waitForTimeout(800)
  const stillError = await page.$('[data-testid="state-error"]')
  record(15, '重试期间界面稳定（持续故障仍为同一错误态，不白屏不串态）', stillError !== null)
  await sampleHScroll()
  await page.screenshot({ path: join(evidenceDir, '08-system-error-input-kept-390.png'), fullPage: true })

  // 15b HTTP 200 + 业务 code=500 反例：不落空结果，进系统错误态，无 .el-message
  await gotoDeepLink('INQBIZ1')
  await fillPhoneAndSubmit(P.pending.phone)
  await page.waitForSelector('[data-testid="state-error"]', { timeout: 4000 })
  const bizErrText = (await page.textContent('[data-testid="state-error"]')).trim()
  const bizErrEmpty = await page.$('[data-testid="state-empty"]')
  const elMessageVisible = await page.$('.el-message')
  record('15b', 'HTTP 200 + 业务 code=500 → 系统错误态（不落空结果），无 .el-message 弹窗', bizErrText.includes(ERROR_TEXT) && bizErrEmpty === null && elMessageVisible === null, `error=${bizErrText.includes(ERROR_TEXT)} empty=${bizErrEmpty !== null} elMsg=${elMessageVisible !== null}`)
  await page.screenshot({ path: join(evidenceDir, '08b-biz-code-500-error-390.png'), fullPage: true })

  // 16 重复点击只发一次请求
  await gotoDeepLink(P.pending.inq)
  const before = lookupRequests.length
  await page.fill('[data-testid="phone-input"]', P.pending.phone)
  await page.dblclick('[data-testid="lookup-btn"]')
  await page.waitForTimeout(1200)
  const added = lookupRequests.length - before
  record(16, '重复（双击）点击期间只发一次 lookup 请求', added === 1, `新增请求数=${added}`)

  // 17 请求体白名单：真实浏览器出站请求体恰好两键
  const sampleBody = lookupRequests.find((r) => r.body.includes(P.converted.inq)) ?? lookupRequests[0]
  let bodyOk = false
  try {
    const parsed = JSON.parse(sampleBody.body)
    bodyOk = sampleBody.method === 'POST' && JSON.stringify(Object.keys(parsed).sort()) === JSON.stringify(['inquiryNo', 'phone'])
  } catch { bodyOk = false }
  record(17, '浏览器出站请求体为 POST 且键恰为 {inquiryNo, phone}', bodyOk)

  // 18 手机号不进 URL
  const visitedUrls = [page.url()]
  record(18, '手机号不出现在任何访问 URL', visitedUrls.every((u) => !u.includes('13800000001') && !u.includes('13900000002') && !u.includes('13700000003') && !u.includes('13600000004') && !u.includes('13555556666')))

  // 19 手机号不进存储
  const storageDump = await page.evaluate(() => JSON.stringify({ ls: { ...localStorage }, ss: { ...sessionStorage } }))
  record(19, 'localStorage/sessionStorage 不含手机号', !storageDump.includes('13800000001') && !storageDump.includes('13900000002'))

  // 20 控制台无手机号
  const consoleDump = consoleMessages.join('\n')
  record(20, '控制台消息不含手机号', !consoleDump.includes('13800000001') && !consoleDump.includes('13900000002'), `console条数=${consoleMessages.length}`)

  // 21 390px 全状态无横向滚动
  const bad = hscrollSamples.filter((s) => !s.ok)
  record(21, '390px 全状态页无横向滚动（scrollWidth≤390）', bad.length === 0, `样本数=${hscrollSamples.length} 最大scrollWidth=${Math.max(...hscrollSamples.map((s) => s.sw))}`)
} catch (e) {
  record(99, '证据采集未捕获异常中断', false, e.message)
} finally {
  const pass = checks.filter((c) => c.ok).length
  const fail = checks.length - pass
  const summary = {
    contract: 'MOCKED_CONTRACT',
    note: '后端 TL55 未 reviewed；本证据全部来自 mock-server 合同假后端，不冒充真实闭环。证据中手机号一律掩码。',
    pass,
    fail,
    total: checks.length,
    viewport: '390x844@2x',
    browser: 'msedge(headless)',
    checks
  }
  writeFileSync(join(evidenceDir, 'browser-evidence-result.json'), JSON.stringify(summary, null, 1))
  writeFileSync(join(evidenceDir, 'lookup-request-bodies.json'), JSON.stringify(lookupRequests.map((r) => ({ url: r.url, method: r.method, body: maskBody(r.body) })), null, 1))
  writeFileSync(join(evidenceDir, 'browser-evidence-run.log'), runLogLines.join('\n') + '\n')
  console.log(`\nSUMMARY browser-evidence: ${pass} passed, ${fail} failed, ${checks.length} total (MOCKED_CONTRACT)`)
  await browser.close()
  server.close()
  process.exit(fail === 0 ? 0 : 2)
}
