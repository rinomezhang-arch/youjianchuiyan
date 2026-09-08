// 浏览器端到端验收台：挂载**真实的 Vue 组件**，只把网络换成合成 API。
//
// 关键在顺序：XHR 必须在导入组件之前替换掉。
// utils/request 里的 axios 实例是模块加载时创建的，晚一步就来不及了。
// 这样跑的是真组件、真路由渲染、真 DOM，唯独后端是合成的——
// 不是"组件挂载成功"那种糊弄，页面上点出来的每个字都由真实代码产生。

const calls = []
window.__harness = { calls }

// ---------- 合成后端：口径照第二轮真实契约 ----------
const BOOKING = {
  booking_id: 'BK1788800000abcde',
  booking_date: '2026-09-12',
  booking_time: '18:30',
  guest_count: 8,
  table_count: 2,
  booking_status: 'confirmed'
}
const PHONE = '13700001111'

let inquiries = [
  { id: 1, customerName: '合成客人甲', customerPhone: '13800001111', guestCount: 6,
    preferredDate: '2026-09-12', preferredTime: '18:00', status: 'pending' },
  { id: 2, customerName: '合成客人乙', customerPhone: '13800002222', guestCount: 4,
    preferredDate: '', preferredTime: '', status: 'pending' }
]
const converted = new Map()          // inquiryId -> bookingId，模拟后端幂等

function safeParse(body) {
  if (!body || typeof body !== 'string') return {}
  try { return JSON.parse(body) } catch { return {} }
}

function respond(url, method, body) {
  const payload = safeParse(body)

  if (url.includes('/public/booking-lookup')) {
    const { phone, bookingId } = payload
    // 与后端同口径：查无、手机号不符、别人的单、缺参数、格式非法 —— 统统同一种空结果
    const hit = phone === PHONE && bookingId === BOOKING.booking_id
    return { code: 200, message: 'success', data: hit ? BOOKING : null }
  }

  if (url.includes('/booking-inquiries/') && url.includes('/convert')) {
    const id = Number(url.match(/booking-inquiries\/(\d+)\/convert/)[1])
    if (converted.has(id)) {
      // 幂等：还原单，标 replayed
      return { code: 200, message: 'success',
        data: { bookingId: converted.get(id), inquiryId: id, replayed: true } }
    }
    if ((payload.tableIds || []).includes(99)) {
      return { code: 400, message: '桌台不存在或不属于该咨询所在门店' }
    }
    if ((payload.tableIds || []).includes(88)) {
      return { code: 400, message: '桌台 88 在该日期已被占用，请另选' }
    }
    const bookingId = 'BK' + Date.now() + id
    converted.set(id, bookingId)
    inquiries = inquiries.map(x => x.id === id ? { ...x, status: 'converted' } : x)
    return { code: 200, message: 'success', data: { bookingId, inquiryId: id, replayed: false } }
  }

  if (url.includes('/public/stores')) {
    return { code: 200, message: 'success', data: [
      { store_id: 1, store_name: '又见炊烟宁国店', address: '宁国路1号',
        phone: '0551-0000001', business_hours: '10:00-22:00' }
    ] }
  }

  if (url.includes('/booking-inquiries')) {
    return { code: 200, message: 'success', data: inquiries }
  }

  // 其余接口给个空成功，免得页面初始化炸掉遮住真正要验的东西
  return { code: 200, message: 'success', data: [] }
}

class StubXHR {
  constructor() { this.readyState = 0; this.status = 0; this.responseText = ''; this.headers = {} }
  open(method, url) { this.method = method; this.url = url; this.readyState = 1 }
  setRequestHeader(k, v) { this.headers[k] = v }
  getAllResponseHeaders() { return 'content-type: application/json' }
  send(body) {
    calls.push({ url: this.url, method: this.method, body: safeParse(body) })
    setTimeout(() => {
      const data = respond(this.url, this.method, body)
      this.status = 200
      this.readyState = 4
      this.responseText = JSON.stringify(data)
      this.response = this.responseText
      if (this.onreadystatechange) this.onreadystatechange()
      if (this.onload) this.onload()
    }, 5)
  }
  abort() {}
  addEventListener(type, fn) { if (type === 'load') this.onload = fn }
}
window.XMLHttpRequest = StubXHR

// axios 1.x 在支持的环境里可能走 fetch 适配器而不是 XHR，两条都得接管，
// 否则请求会穿透到 vite 代理去打真后端——第一次跑就是这么漏的。
window.fetch = async (input, init = {}) => {
  const url = typeof input === 'string' ? input : input.url
  const body = init.body ?? (typeof input === 'object' ? input.body : null)
  calls.push({ url, method: (init.method || 'GET').toUpperCase(), body: safeParse(body) })
  const data = respond(url, init.method || 'GET', body)
  return new Response(JSON.stringify(data), {
    status: 200, headers: { 'content-type': 'application/json' }
  })
}

// ---------- XHR 换完了，现在才导入真实组件 ----------
const { createApp, h } = await import('vue')
const ElementPlus = await import('element-plus')
await import('element-plus/dist/index.css')
const StoreDetail = (await import('../../src/views/site/StoreDetail.vue')).default
const Bookings = (await import('../../src/views/dashboard/Bookings.vue')).default

// 极简路由替身：StoreDetail 用 useRoute 取 id
const { createRouter, createWebHashHistory, RouterView } = await import('vue-router')
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/store/1' },
    { path: '/store/:id', component: StoreDetail },
    { path: '/bookings', component: Bookings }
  ]
})

const app = createApp({
  render: () => h('div', [
    h('nav', { style: 'padding:8px;display:flex;gap:12px;background:#eee' }, [
      h('a', { href: '#/store/1', id: 'nav-store' }, '门店页（查预订）'),
      h('a', { href: '#/bookings', id: 'nav-bookings' }, '员工端（咨询转单）')
    ]),
    h(RouterView)
  ])
})
// 被测页面里的 BookingDialog 等子组件依赖本验收台没提供的上下文（会话/权限等），
// 它们的 setup 抛错不该把整页拖白——本轮要验的是咨询转单面板。
// 吞掉的错误照常打到 console，不静默。
app.config.errorHandler = (err, instance, info) => {
  console.warn('[harness] 子组件异常已隔离：', info, err && err.message)
}
app.config.warnHandler = () => {}
app.use(router)
try {
  const plugin = ElementPlus.default ?? ElementPlus
  if (plugin && typeof plugin.install === 'function') app.use(plugin)
} catch (e) {
  // 项目本身用 unplugin-vue-components 按需自动引入，这里装不上不影响被测组件渲染
  console.warn('[harness] ElementPlus 未整体安装，按需自动引入照常生效')
}
app.mount('#app')
window.__harness.ready = true
