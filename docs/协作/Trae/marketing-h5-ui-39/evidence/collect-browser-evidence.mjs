// TR-MARKETING-H5-UI-39 浏览器证据采集（390px 真实 Chromium 内核）。
//
// ⚠️ MOCKED_CONTRACT：所有 /api/public/marketing/** 与 /api/public/booking-inquiry
// 响应均由 Playwright page.route 按设计文档第四节合同形状 fulfill，后端未接通。
// 本脚本只证明前端在 390px 真实排版引擎下的渲染/无横向滚动/状态分支/请求体纪律，
// 不构成真实 HTTP/数据库闭环证据。
//
// 运行：先 `npx vite preview --port 4391 --strictPort`（dist 产物），再：
//   node docs/协作/Trae/marketing-h5-ui-39/evidence/collect-browser-evidence.mjs
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const require = createRequire(import.meta.url)
const PW_PATH = 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core'
const { chromium } = require(PW_PATH)

const HERE = dirname(fileURLToPath(import.meta.url))
const BASE = 'http://127.0.0.1:4391'
const DAY = 86400000
const iso = (d) => new Date(Date.now() + d * DAY).toISOString()
const results = []
const log = (name, ok, detail = '') => { results.push({ name, ok, detail }); console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  — ' + detail : ''}`) }

function snapshot(over = {}) {
  return {
    code: 200,
    data: {
      publicationId: 3901,
      storeId: 1,
      storeName: '又见炊烟·宁国店',
      publicTitle: '中秋团圆家宴 · 限量包厢开放预订',
      status: 'published',
      validFrom: iso(-1),
      validTo: iso(14),
      sourceCode: 'SRC-EVIDENCE-3901',
      publicSummary: '6-10 人团圆套餐，含花胶鸡锅与节气冷碟，限堂食预约。',
      contentJson: {
        content: ['第一道：节气冷碟四味', '主菜：花胶鸡锅与时令蔬食', '甜品：桂花酒酿圆子'],
        packages: ['团圆宴 6 人餐 ¥1288', '赏月宴 10 人餐 ¥1988'],
        rules: ['需提前 1 天预约', '限堂食，不可与其他优惠同享', '包厢数量有限，先约先得']
      },
      storeAddress: '宁国路 88 号 2 楼',
      storePhone: '0551-88888888',
      heroAssetUrl: '/site-photos/storefront-entrance.jpg',
      ...over
    }
  }
}

async function noHorizontalOverflow(page, tag) {
  const w = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    bodyScrollWidth: document.body.scrollWidth
  }))
  log(`无横向滚动(${tag}) scrollWidth=${w.scrollWidth} viewport=390`, w.scrollWidth <= 390 && w.bodyScrollWidth <= 390, JSON.stringify(w))
}

async function ctaDiscipline(page, tag) {
  const r = await page.evaluate(() => {
    const btns = [...document.querySelectorAll('.mk-cta')]
    return btns.map((b) => {
      const cs = getComputedStyle(b)
      return {
        text: b.textContent.trim(),
        rects: b.getClientRects().length,
        scrollW: b.scrollWidth, clientW: b.clientWidth,
        nowrap: cs.whiteSpace === 'nowrap',
        singleLine: b.scrollWidth <= b.clientWidth + 1
      }
    })
  })
  const ok = r.length > 0 && r.every((x) => x.nowrap && x.singleLine)
  log(`主按钮单行不溢出(${tag}) n=${r.length}`, ok, JSON.stringify(r))
  return r
}

async function shot(page, name) {
  await page.screenshot({ path: join(HERE, `${name}.png`) })
}

const inquiryBodies = []
const publicGetUrls = [] // R1-1：公开快照 GET 的真实线上 URL（证明不含 storeId）

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const errors = []
const expected404 = [] // 负向用例刻意制造的 404 URL
const expectedAborted = [] // 负向用例刻意 abort 的请求
const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
context.on('request', (r) => { if (r.url().includes('/api/public/marketing/a/')) publicGetUrls.push(r.url()) })
context.on('response', (r) => { if (r.status() === 404) expected404.push(r.url()) })
context.on('requestfailed', (r) => expectedAborted.push(r.url()))

async function openPage(path) {
  const page = await context.newPage()
  page.on('console', (m) => { if (m.type() === 'error') errors.push(`console.error: ${m.text()}`) })
  page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`))
  await page.goto(BASE + path, { waitUntil: 'networkidle' })
  await page.waitForTimeout(300)
  return page
}

try {
  /* ---------- 1. 成功首屏（published + 有效期内） ---------- */
  await context.route('**/api/public/marketing/a/**', async (route) => {
    const url = route.request().url()
    if (url.includes('/live-3901')) return route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify(snapshot()) })
    if (url.includes('/paused-3901')) return route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify({ code: 200, data: { status: 'paused', storeName: '又见炊烟·宁国店' } }) })
    if (url.includes('/expired-3901')) return route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify(snapshot({ status: 'published', validFrom: iso(-30), validTo: iso(-2) })) })
    if (url.includes('/draft-3901')) return route.fulfill({ status: 404, contentType: 'application/json;charset=utf-8', body: JSON.stringify({ code: 404, message: 'not found' }) })
    if (url.includes('/net-3901')) {
      netAttempts += 1
      if (netAttempts === 1) return route.abort('failed')
      return route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify(snapshot()) })
    }
    return route.fulfill({ status: 404, contentType: 'application/json;charset=utf-8', body: JSON.stringify({ code: 404 }) })
  })
  let netAttempts = 0

  await context.route('**/api/public/marketing/events**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify({ code: 200, data: { accepted: true } }) }))

  let inquiryAttempt = 0
  await context.route('**/api/public/booking-inquiry**', async (route) => {
    inquiryAttempt += 1
    const body = route.request().postDataJSON()
    inquiryBodies.push({ attempt: inquiryAttempt, body })
    if (inquiryAttempt === 1) return route.abort('failed') // 首提网络失败，验幂等重试
    // R1-3：成功回执同时返回编号与查询入口；两者都必须来自 API
    return route.fulfill({ status: 200, contentType: 'application/json;charset=utf-8', body: JSON.stringify({ code: 200, data: { inquiryNo: 'YJ-20260913-3901', lookupUrl: '/h5/inquiry-lookup?no=YJ-20260913-3901' } }) })
  })

  const live = await openPage('/h5/activity/live-3901')
  await live.waitForSelector('.mk-title')
  const firstScreen = await live.evaluate(() => ({
    store: document.querySelector('.mk-store')?.textContent.trim(),
    title: document.querySelector('.mk-title')?.textContent.trim(),
    validity: document.querySelector('.mk-validity')?.textContent.replace(/\s+/g, '').trim(),
    imgSrc: document.querySelector('.mk-hero-img')?.getAttribute('src'),
    imgNatural: (() => { const i = document.querySelector('.mk-hero-img'); return i ? { w: i.naturalWidth, h: i.naturalHeight, complete: i.complete } : null })(),
    cta: [...document.querySelectorAll('.mk-cta')].map((b) => b.textContent.trim()),
    ctaVisible: [...document.querySelectorAll('.mk-cta')]
      .filter((b) => b.offsetParent !== null && b.getClientRects().length > 0)
      .map((b) => b.textContent.trim())
  }))
  log('首屏门店/标题/有效期', /宁国店/.test(firstScreen.store) && /中秋/.test(firstScreen.title) && /有效期/.test(firstScreen.validity), JSON.stringify(firstScreen))
  log('首屏真实门店图加载', firstScreen.imgSrc === '/site-photos/storefront-entrance.jpg' && firstScreen.imgNatural.complete && firstScreen.imgNatural.w > 0, JSON.stringify(firstScreen.imgNatural))
  // R1-2：初始视口可见主按钮恰好一个「咨询档期」；底部第二次入口首屏内不得出现
  const visibleCta = firstScreen.ctaVisible
  log('首屏可见主按钮恰好一个「咨询档期」(R1-2)', visibleCta.length === 1 && visibleCta[0] === '咨询档期', JSON.stringify(visibleCta))
  const secondCtaBeforeScroll = await live.$('.mk-cta--second')
  log('首屏不渲染底部第二入口', secondCtaBeforeScroll === null)
  await noHorizontalOverflow(live, 'success')
  await ctaDiscipline(live, 'success')
  await shot(live, '01-live-firstscreen-390')

  /* ---------- 2. 暂停 / 过期 / 草稿404 / 网络错误 ---------- */
  const paused = await openPage('/h5/activity/paused-3901')
  log('暂停态文案', (await paused.textContent('.mk-state-title')) === '活动已暂停')
  log('暂停态无咨询入口', (await paused.$$('.mk-cta')).length === 0)
  await shot(paused, '02-paused-390')

  const expired = await openPage('/h5/activity/expired-3901')
  log('过期态文案', (await expired.textContent('.mk-state-title')) === '活动已结束')
  log('过期态无咨询入口', (await expired.$$('.mk-cta')).length === 0)
  await shot(expired, '03-expired-390')

  const draft = await openPage('/h5/activity/draft-3901')
  log('草稿 slug 统一 404 口径（不可公开）', (await draft.textContent('.mk-state-title')) === '活动不存在或已下架')
  log('草稿态无咨询入口', (await draft.$$('.mk-cta')).length === 0)
  await shot(draft, '04-notfound-draft404-390')

  const net = await openPage('/h5/activity/net-3901')
  log('断网态文案+重试', (await net.textContent('.mk-state-title')) === '网络不太顺畅' && (await net.$('.mk-retry')) !== null)
  await shot(net, '05-network-error-390')
  await net.click('.mk-retry')
  await net.waitForSelector('.mk-title', { timeout: 8000 })
  log('重试后成功加载', await net.$('.mk-cta') !== null)
  await shot(net, '06-retry-success-390')

  /* ---------- 3. 咨询：失败留输入、requestId 复用、成功编号、请求体无 storeId ---------- */
  await live.click('.mk-hero-card .mk-cta')
  await live.waitForSelector('.mk-form-card.is-open')
  // R1-2 下半段：表单展开后页面显著长于首屏；滚动到底（主按钮滚出视口）后底部同名入口可用
  await live.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await live.waitForSelector('.mk-cta--second', { timeout: 4000 })
  const secondVisible = await live.evaluate(() => {
    const b = document.querySelector('.mk-cta--second')
    return b && b.offsetParent !== null && b.getClientRects().length > 0
  })
  log('滚出首屏后底部第二入口出现（不与首屏 CTA 同框）', secondVisible === true)
  await live.evaluate(() => window.scrollTo(0, 0))
  await live.waitForTimeout(150)
  await live.fill('input[placeholder="您怎么称呼"]', '王女士')
  await live.fill('input[placeholder="用于门店与您确认"]', '13800000001')
  await live.fill('input[type="date"]', '2026-10-01')
  await live.fill('input[type="number"]', '4')
  await live.fill('textarea', '需要靠窗包厢（合成测试数据）')
  await shot(live, '07-inquiry-form-filled-390')
  await noHorizontalOverflow(live, 'form')

  await live.click('button[type="submit"]')
  await live.waitForSelector('.mk-submit-err', { timeout: 8000 })
  const errText = await live.textContent('.mk-submit-err')
  log('首次提交失败明示且不冒充成功', /网络异常|提交未成功/.test(errText) && (await live.$('.mk-inquiry-no')) === null, errText)
  log('失败后表单输入保留', await live.inputValue('input[placeholder="您怎么称呼"]') === '王女士')

  // 等全局拦截器的 3s 错误 toast（既有项目约定）自然消失，避免与成功页同框
  await live.waitForTimeout(3400)

  await live.click('button[type="submit"]')
  await live.waitForSelector('.mk-inquiry-no', { timeout: 8000 })
  const no = await live.textContent('.mk-inquiry-no')
  const lookup = await live.getAttribute('.mk-form-card a.mk-state-link', 'href')
  log('成功只认后端编号', no === 'YJ-20260913-3901', `编号=${no}`)
  // R1-3：查询入口必须是 API 返回的 lookupUrl 原文，前端不得自行拼接门店地址
  log('查询入口来自 API lookupUrl (R1-3)', lookup === '/h5/inquiry-lookup?no=YJ-20260913-3901', `href=${lookup}`)
  await shot(live, '08-inquiry-success-390')
  await noHorizontalOverflow(live, 'inquiry-success')

  log('咨询恰好两次提交', inquiryBodies.length === 2, `n=${inquiryBodies.length}`)
  const [b1, b2] = inquiryBodies
  log('请求体不含 storeId/store_id', inquiryBodies.every((x) => !('storeId' in x.body) && !('store_id' in x.body)))
  log('请求体含 sourceCode/requestId/客人字段', inquiryBodies.every((x) =>
    x.body.sourceCode === 'SRC-EVIDENCE-3901' &&
    typeof x.body.requestId === 'string' && x.body.requestId.length > 8 &&
    x.body.customerName === '王女士' && x.body.phone === '13800000001'))
  log('失败重试复用同一 requestId（幂等）', b1.body.requestId === b2.body.requestId, `${b1.body.requestId}`)

  /* ---------- 3b. R1-1：公开快照 GET 的真实 URL 只按 slug，无任何 storeId ---------- */
  log('公开 GET 均带真实路径', publicGetUrls.length >= 5, `n=${publicGetUrls.length}`)
  log('公开 GET URL 不含 storeId (R1-1)', publicGetUrls.length > 0 && publicGetUrls.every((u) => !/[?&]storeId=/.test(u)), publicGetUrls.join(' | ').slice(0, 300))

  await writeFile(join(HERE, 'inquiry-request-bodies.json'), JSON.stringify(inquiryBodies, null, 2), 'utf8')

  /* ---------- 4. 控制台错误只允许负向用例刻意注入的 404/abort ---------- */
  const unexpected = errors.filter((m) => !/status of 404|net::ERR_FAILED|ERR_NETWORK|failed/i.test(m))
  const expectedNeg = expected404.filter((u) => u.includes('/draft-3901')).length
    + expectedAborted.filter((u) => u.includes('/net-3901') || u.includes('/booking-inquiry')).length
  log('负向注入计数：草稿404×1 + abort×2', expectedNeg === 3, `404=${expected404.length} aborted=${expectedAborted.length}`)
  log('无负向用例之外的页面错误', unexpected.length === 0, unexpected.join(' | ').slice(0, 300))

  const failed = results.filter((r) => !r.ok)
  await mkdir(HERE, { recursive: true })
  await writeFile(join(HERE, 'browser-evidence-result.json'), JSON.stringify({ base: BASE, viewport: 390, mockedContract: true, results, expected404, expectedAborted, unexpected, inquiryBodies, publicGetUrls }, null, 2), 'utf8')
  console.log(`\n${results.length - failed.length}/${results.length} PASS；截图 8 张与请求体见 evidence/`)
  process.exitCode = failed.length ? 1 : 0
} finally {
  await browser.close()
}
