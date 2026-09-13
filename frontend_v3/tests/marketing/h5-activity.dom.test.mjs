// TR-MARKETING-H5-UI-39 客人 H5（/h5/activity/:publicSlug）DOM 测试。
//
// ⚠️ MOCKED_CONTRACT：公开快照、view 事件、咨询提交全部由 axios adapter 合同假后端
// 提供（见 helpers.mjs），后端未接通。390px 布局/无横向滚动/真实图片加载等视觉项
// 由 Playwright 真实浏览器证据覆盖（happy-dom 无排版引擎，不在此断言像素）。
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import MarketingActivity from '@/views/site/MarketingActivity.vue'
import { useAdapter, resetAdapter, clearLocal, flushAllDeep, callsTo, setNativeInput, OK, NETWORK_FAIL, FAIL_HTTP } from './helpers.mjs'

const DAY = 86400000
function iso(offsetDays) {
  return new Date(Date.now() + offsetDays * DAY).toISOString()
}

/** 有效期内的发布快照（合同形状） */
function liveSnapshot(over = {}) {
  return {
    publicationId: 55,
    publication_id: 55,
    storeId: 1,
    store_id: 1,
    storeName: '又见炊烟·宁国店',
    publicTitle: '中秋家宴·包厢开放预订',
    status: 'published',
    validFrom: iso(-1),
    validTo: iso(14),
    sourceCode: 'SRC-MID-AUTUMN-55',
    publicSummary: '团圆家宴 6-10 人套餐，限量包厢。',
    contentJson: {
      content: ['第一道：冷碟四味', '主菜：花胶鸡锅'],
      packages: ['团圆宴 6 人餐 ¥1288', '赏月宴 10 人餐 ¥1988'],
      rules: ['需提前 1 天预约', '限堂食，不可与其他优惠同享']
    },
    storeAddress: '宁国路 88 号',
    storePhone: '0551-88888888',
    heroAssetUrl: '/site-photos/storefront-entrance.jpg',
    ...over
  }
}

let api
let mountedWrapper
let host
function handler(url, config) {
  if (url.includes('/marketing/a/')) return api.activity
  if (url.includes('/marketing/events')) return api.event ?? OK({ accepted: true })
  if (url.includes('/booking-inquiry')) {
    api.inquiryAttempts += 1
    return typeof api.inquiry === 'function' ? api.inquiry(api.inquiryAttempts, config) : api.inquiry
  }
  return FAIL_HTTP(404, 'no-mock')
}

async function mountH5(slug = 'slug-ok') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/h5/activity/:publicSlug', component: MarketingActivity }]
  })
  // 先进入公开路由再挂载：组件 onMounted 读一次 route.params，之后不再重取。
  await router.push(`/h5/activity/${slug}`)
  await router.isReady()
  // 挂到 document：happy-dom 对脱离文档的多根片段切换（咨询成功模板替换）
  // 会在 removeFragment 处抛 nextSibling null，接入文档后稳定（真实浏览器无此问题）。
  host = document.createElement('div')
  document.body.appendChild(host)
  mountedWrapper = mount(MarketingActivity, { attachTo: host, global: { plugins: [router] } })
  await flushPromises()
  await flushAllDeep(4)
  return mountedWrapper
}

async function fillInquiry(wrapper) {
  setNativeInput(wrapper.find('input[placeholder="您怎么称呼"]').element, '王女士')
  setNativeInput(wrapper.find('input[placeholder="用于门店与您确认"]').element, '13800000001')
  setNativeInput(wrapper.find('input[type="date"]').element, '2026-10-01')
  setNativeInput(wrapper.find('input[type="number"]').element, '4')
  setNativeInput(wrapper.find('textarea').element, '需要靠窗包厢')
  await flushPromises()
}

beforeEach(() => {
  clearLocal()
  api = { activity: OK(liveSnapshot()), event: OK({}), inquiry: OK({ inquiryNo: 'YJ20260913001', lookupUrl: '/h5/inquiry-lookup?no=YJ20260913001' }), inquiryAttempts: 0 }
  useAdapter(handler)
})
afterEach(async () => {
  mountedWrapper?.unmount()
  host?.remove()
  resetAdapter()
  await flushPromises()
  document.body.querySelectorAll('.el-message').forEach((n) => n.remove())
})

it('公开快照 GET 只按 slug：显式 storeId=null 挡住全局拦截器注入默认门店（R1-1）', async () => {
  api.activity = OK(liveSnapshot())
  await mountH5('slug-ok')
  const call = callsTo('/marketing/a/slug-ok', 'get')[0]
  expect(call).toBeTruthy()
  // 拦截器只在 storeId===undefined 时兜底注入；显式 null 必须原样保留。
  // axios 默认序列化不输出 null —— 真实线上 URL 无 ?storeId（线上证据由浏览器脚本核对 URL）。
  expect(call.params).toBeTruthy()
  expect(call.params.storeId ?? null).toBeNull()
  expect(call.params.storeId).not.toBe('1')
  expect(call.params.storeId).not.toBe(1)
})

it('草稿统一"不存在或已下架"：无咨询入口、无 view 埋点', async () => {
  api.activity = OK({ status: 'draft' })
  const wrapper = await mountH5('draft-slug')
  expect(wrapper.find('.mk-state-title').text()).toBe('活动不存在或已下架')
  expect(wrapper.findAll('.mk-cta')).toHaveLength(0)
  expect(wrapper.find('.mk-form-card').exists()).toBe(false)
  expect(callsTo('/marketing/events', 'post')).toHaveLength(0)
})

it('HTTP 404 与草稿同口径（不区分试探）', async () => {
  api.activity = FAIL_HTTP(404)
  const wrapper = await mountH5('missing')
  expect(wrapper.find('.mk-state-title').text()).toBe('活动不存在或已下架')
  expect(wrapper.findAll('.mk-cta')).toHaveLength(0)
})

it('暂停：明示原因且不渲染内容、不给咨询', async () => {
  api.activity = OK({ status: 'paused', storeName: '宁国店' })
  const wrapper = await mountH5('paused')
  expect(wrapper.find('.mk-state-title').text()).toBe('活动已暂停')
  expect(wrapper.findAll('.mk-cta')).toHaveLength(0)
  expect(wrapper.find('.mk-title').exists()).toBe(false)
  expect(callsTo('/marketing/events', 'post')).toHaveLength(0)
})

it('过期：不显示咨询按钮与表单', async () => {
  api.activity = OK({ status: 'expired' })
  const wrapper = await mountH5('expired')
  expect(wrapper.find('.mk-state-title').text()).toBe('活动已结束')
  expect(wrapper.findAll('.mk-cta')).toHaveLength(0)
})

it('断网：网络错误页可重试，恢复后正常加载', async () => {
  api.activity = NETWORK_FAIL()
  const wrapper = await mountH5('slug-ok')
  expect(wrapper.find('.mk-state-title').text()).toBe('网络不太顺畅')
  expect(wrapper.find('.mk-retry').exists()).toBe(true)

  api.activity = OK(liveSnapshot())
  await wrapper.find('.mk-retry').trigger('click')
  await flushPromises()
  await flushAllDeep(4)
  expect(wrapper.find('.mk-store').text()).toContain('宁国店')
  expect(wrapper.findAll('.mk-cta').length).toBeGreaterThan(0)
})

it('成功首屏：门店/标题/有效期/真实门店图 + 首屏唯一主按钮；成功页双字段只认 API', async () => {
  const wrapper = await mountH5()

  // 首屏信息
  expect(wrapper.find('.mk-store').text()).toContain('宁国店')
  expect(wrapper.find('.mk-title').text()).toBe('中秋家宴·包厢开放预订')
  expect(wrapper.find('.mk-validity').text()).toContain('有效期')
  const img = wrapper.find('.mk-hero-img')
  expect(img.attributes('src')).toBe('/site-photos/storefront-entrance.jpg')
  expect(img.attributes('width')).toBe('358')

  // R1-2：初始视口只允许一个主 CTA；底部第二次入口未滚出首屏前不得渲染
  const openBtns = wrapper.findAll('.mk-cta').filter((b) => b.text().trim() === '咨询档期')
  expect(openBtns).toHaveLength(1)
  expect(wrapper.find('.mk-cta--second').exists()).toBe(false)
  expect(wrapper.find('.mk-form-card').classes()).not.toContain('is-open')

  // view 埋点（合成 visitor_key，无个人信息）
  await flushAllDeep(2)
  const events = callsTo('/marketing/events', 'post')
  expect(events).toHaveLength(1)
  expect(events[0].data.eventType).toBe('view')
  expect(events[0].data.sourceCode).toBe('SRC-MID-AUTUMN-55')
  expect(events[0].data.requestId).toBeTruthy()

  // 打开表单 → 真实填写 → 提交
  await openBtns[0].trigger('click')
  expect(wrapper.find('.mk-form-card').classes()).toContain('is-open')
  await fillInquiry(wrapper)
  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  await flushAllDeep(4)

  // R1-3：编号与查询入口都来自后端响应，前端不自行拼门店地址
  expect(wrapper.find('.mk-inquiry-no').text()).toBe('YJ20260913001')
  const lookup = wrapper.find('.mk-form-card a.mk-state-link')
  expect(lookup.attributes('href')).toBe('/h5/inquiry-lookup?no=YJ20260913001')
  expect(wrapper.text()).not.toContain('/stores/1')

  // 请求体：sourceCode + 表单字段 + requestId；显式不含门店标识
  const posts = callsTo('/booking-inquiry', 'post')
  expect(posts).toHaveLength(1)
  const body = posts[0].data
  expect(body.sourceCode).toBe('SRC-MID-AUTUMN-55')
  expect(body.requestId).toBeTruthy()
  expect(body.customerName).toBe('王女士')
  expect(body.phone).toBe('13800000001')
  expect(body.expectedDate).toBe('2026-10-01')
  expect(body.partySize).toBe(4)
  expect(body.remark).toBe('需要靠窗包厢')
  expect('storeId' in body).toBe(false)
  expect('store_id' in body).toBe(false)
})

it('R1-3 响应缺 lookupUrl：绝不显示成功，不拼门店地址；补全后重试同 requestId 才成功', async () => {
  api.inquiry = (attempt) => (attempt === 1
    ? OK({ inquiryNo: 'YJ20260913003' }) // 缺 lookupUrl —— 双门槛不满足
    : OK({ inquiryNo: 'YJ20260913004', lookupUrl: '/h5/inquiry-lookup?no=YJ20260913004' }))
  const wrapper = await mountH5()

  await wrapper.findAll('.mk-cta')[0].trigger('click')
  await fillInquiry(wrapper)
  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  await flushAllDeep(4)

  // 缺 lookupUrl：不当成功、无编号、无任何查询入口链接（尤其不得出现 /stores/1 拼接）
  expect(wrapper.find('.mk-inquiry-no').exists()).toBe(false)
  expect(wrapper.find('.mk-form-card a.mk-state-link').exists()).toBe(false)
  expect(wrapper.find('.mk-submit-err').text()).toContain('咨询回执')
  expect(wrapper.find('input[placeholder="您怎么称呼"]').element.value).toBe('王女士')

  // 重试补全双字段才成功；requestId 沿用同一意图
  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  await flushAllDeep(4)
  expect(wrapper.find('.mk-inquiry-no').text()).toBe('YJ20260913004')
  expect(wrapper.find('.mk-form-card a.mk-state-link').attributes('href')).toBe('/h5/inquiry-lookup?no=YJ20260913004')

  const posts = callsTo('/booking-inquiry', 'post')
  expect(posts).toHaveLength(2)
  expect(posts[0].data.requestId).toBe(posts[1].data.requestId)
})

it('咨询失败：留在原地保留输入；重试沿用同一 requestId，成功才换页', async () => {
  api.inquiry = (attempt) => (attempt === 1
    ? NETWORK_FAIL()
    : OK({ inquiry_no: 'YJ20260913002', lookup_url: '/h5/inquiry-lookup?no=YJ20260913002' }))
  const wrapper = await mountH5()

  await wrapper.findAll('.mk-cta')[0].trigger('click')
  await fillInquiry(wrapper)

  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  await flushAllDeep(4)
  expect(wrapper.find('.mk-submit-err').text()).toContain('网络异常')
  expect(wrapper.find('.mk-inquiry-no').exists()).toBe(false)
  // 输入保留
  expect(wrapper.find('input[placeholder="您怎么称呼"]').element.value).toBe('王女士')

  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  await flushAllDeep(4)
  expect(wrapper.find('.mk-inquiry-no').text()).toBe('YJ20260913002')

  const posts = callsTo('/booking-inquiry', 'post')
  expect(posts).toHaveLength(2)
  expect(posts[0].data.requestId).toBeTruthy()
  expect(posts[0].data.requestId).toBe(posts[1].data.requestId)
  expect(posts[0].data.phone).toBe('13800000001')
})

it('表单校验：空提交不发请求；手机号非法就地提示', async () => {
  const wrapper = await mountH5()
  await wrapper.findAll('.mk-cta')[0].trigger('click')
  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  expect(callsTo('/booking-inquiry', 'post')).toHaveLength(0)
  expect(wrapper.findAll('.mk-err').length).toBeGreaterThan(0)

  setNativeInput(wrapper.find('input[placeholder="您怎么称呼"]').element, '王女士')
  setNativeInput(wrapper.find('input[placeholder="用于门店与您确认"]').element, '123')
  await wrapper.find('form.mk-form').trigger('submit.prevent')
  await flushPromises()
  const phoneErr = wrapper.find('input[placeholder="用于门店与您确认"]').element.closest('label').querySelector('.mk-err')
  expect(phoneErr.textContent).toContain('手机号')
  expect(callsTo('/booking-inquiry', 'post')).toHaveLength(0)
})
