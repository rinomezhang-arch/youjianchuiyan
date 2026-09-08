// 真实后端验收台：**不打任何桩**。
// 请求经 vite 代理打到真实 Spring Boot（8080）+ 隔离 MySQL。
// 与 harness.js 的唯一区别就是这里没有 XHR/fetch 替身——那份验的是前端逻辑，这份验的是真链路。
const { createApp, h } = await import('vue')
const ElementPlus = await import('element-plus')
await import('element-plus/dist/index.css')
const StoreDetail = (await import('../../src/views/site/StoreDetail.vue')).default
const Bookings = (await import('../../src/views/dashboard/Bookings.vue')).default
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
      h('a', { href: '#/store/1' }, '门店页（查预订）'),
      h('a', { href: '#/bookings' }, '员工端（咨询转单）')
    ]),
    h(RouterView)
  ])
})
app.config.errorHandler = (err, i, info) => console.warn('[real-harness] 子组件异常已隔离：', info, err?.message)
app.config.warnHandler = () => {}
app.use(router)
try { const p = ElementPlus.default ?? ElementPlus; if (p?.install) app.use(p) } catch {}
app.mount('#app')
window.__realHarness = { ready: true }
