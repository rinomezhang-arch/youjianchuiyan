// TR-AUTH-LOGOUT-WIRE-R2-35 最小 DOM 回归。
// 背景：a900174f 浏览器矩阵唯一失败"点退出无确认框"，经真实链复验其探针只查旧
// Element Plus 的 .el-message-box，而桌面外壳早已改用自定义确认弹层（.modal-overlay，
// Teleport 到 body）。本测试把"头像下拉 -> 真实点击退出项 -> 确认弹层 -> 取消/确认"
// 整条 DOM 契约锁死，防止再次只靠方法调用或过期选择器误判。
// 纪律：只对真实渲染元素派发 click，绝不直接调用 handleCommand/confirmLogout。
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import i18n from '@/i18n'
import Dashboard from '@/views/Dashboard.vue'
import { useUserStore } from '@/store/user'
import { useAdapter, clearLocal, seedIdentity, flushAllDeep } from './helpers.mjs'

let router

function meReply(role = 'manager', storeId = 1) {
  return async (url) => {
    if (url === '/auth/me') {
      return { body: { code: 200, data: { user: { role, staffName: '测试经理' }, storeId, storeName: '宁国店' } } }
    }
    return { body: { code: 200, data: {} } }
  }
}

function mountDashboard() {
  return mount(Dashboard, {
    global: {
      plugins: [ElementPlus, i18n, router],
      stubs: {
        AIChatFloat: { template: '<div class="ai-chat-stub" />' },
        NotifyBell: { template: '<div class="notify-stub" />' }
      }
    }
  })
}

/** 真实点击头像下拉触发器，返回 teleport 到 body 的菜单项元素列表 */
async function openAvatarMenu(wrapper) {
  await wrapper.find('.el-dropdown .avatar').trigger('click')
  await flushPromises()
  await flushAllDeep()
  return [...document.body.querySelectorAll('.el-dropdown-menu__item')]
}

/** 真实点击菜单项（DOM click，不走组件方法） */
async function clickMenuItemByText(items, re) {
  const el = items.find((node) => re.test(node.textContent || ''))
  expect(el, `菜单项 ${re} 必须存在`).toBeTruthy()
  el.click()
  await flushPromises()
  await flushAllDeep()
}

function modalNode() {
  return document.body.querySelector('.modal-overlay')
}

beforeEach(async () => {
  setActivePinia(createPinia())
  clearLocal()
  router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/dashboard/home' },
      { path: '/dashboard/:pathMatch(.*)*', component: { template: '<div class="route-stub" />' } },
      { path: '/login', component: { template: '<div />' } }
    ]
  })
  await router.push('/dashboard/home')
  window.innerWidth = 1280
  window.innerHeight = 800
})

afterEach(() => {
  clearLocal()
  window.innerWidth = 1280
})

describe('R2-35：头像下拉退出的真实 DOM 确认链', () => {
  it('点击可见退出项只弹确认框：弹层出现、尚未发 logout、会话保持', async () => {
    seedIdentity({ token: 'tok-r35', storeId: 1, roles: ['manager'] })
    const logoutCalls = []
    useAdapter(async (url, config) => {
      if (url === '/auth/logout') logoutCalls.push(config.method || 'post')
      return meReply()(url)
    })
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      const items = await openAvatarMenu(wrapper)
      await clickMenuItemByText(items, /退出|注销|登出|Logout/)

      const overlay = modalNode()
      expect(overlay, '退出项点击后必须出现确认弹层（自定义 .modal-overlay）').toBeTruthy()
      const dialog = document.body.querySelector('.modal-dialog')
      expect(dialog).toBeTruthy()
      // happy-dom 不做布局（rect 恒 0），用计算样式 + 文案证明弹层真实渲染而非隐藏残骸
      expect(getComputedStyle(dialog).display).not.toBe('none')
      expect(dialog.querySelector('.modal-title').textContent).toMatch(/退出|注销|登出|Logout/)
      // 确认前不得有任何 logout 出站，也不得提前清身份
      expect(logoutCalls).toEqual([])
      expect(localStorage.getItem('token')).toBe('tok-r35')
      expect(router.currentRoute.value.path).toBe('/dashboard/home')
    } finally {
      wrapper.unmount()
    }
  })

  it('取消：弹层关闭，token/路由/登录态保持，不发 logout', async () => {
    seedIdentity({ token: 'tok-r35', storeId: 1, roles: ['manager'] })
    const logoutCalls = []
    useAdapter(async (url, config) => {
      if (url === '/auth/logout') logoutCalls.push(config.method || 'post')
      return meReply()(url)
    })
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      await clickMenuItemByText(await openAvatarMenu(wrapper), /退出|注销|登出|Logout/)
      expect(modalNode()).toBeTruthy()

      document.body.querySelector('.modal-dialog .btn-cancel').click()
      await flushPromises()
      await flushAllDeep()

      expect(modalNode()).toBeFalsy()
      expect(logoutCalls).toEqual([])
      expect(localStorage.getItem('token')).toBe('tok-r35')
      expect(useUserStore().isLoggedIn).toBe(true)
      expect(router.currentRoute.value.path).toBe('/dashboard/home')
    } finally {
      wrapper.unmount()
    }
  })

  it('确认：调用 store.logout 清理全部本地身份并进入 /login（logout 200）', async () => {
    seedIdentity({ token: 'tok-r35', storeId: 1, roles: ['manager'] })
    const logoutCalls = []
    useAdapter(async (url, config) => {
      if (url === '/auth/logout') { logoutCalls.push(config.method || 'post'); return { body: { code: 200, data: {} } } }
      return meReply()(url)
    })
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      await clickMenuItemByText(await openAvatarMenu(wrapper), /退出|注销|登出|Logout/)
      document.body.querySelector('.modal-dialog .btn-confirm').click()
      await flushPromises()
      await flushAllDeep()

      expect(logoutCalls).toEqual(['post'])
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('roles')).toBeNull()
      expect(localStorage.getItem('currentStoreId')).toBeNull()
      expect(useUserStore().isLoggedIn).toBe(false)
      expect(modalNode()).toBeFalsy()
      expect(router.currentRoute.value.path).toBe('/login')
    } finally {
      wrapper.unmount()
    }
  })

  it('确认：服务端 logout 失败（网络错误）仍完成本地退出并进入 /login', async () => {
    seedIdentity({ token: 'tok-r35', storeId: 1, roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/logout') return { networkError: 'Network Error' }
      return meReply()(url)
    })
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      await clickMenuItemByText(await openAvatarMenu(wrapper), /退出|注销|登出|Logout/)
      document.body.querySelector('.modal-dialog .btn-confirm').click()
      await flushPromises()
      await flushAllDeep()

      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('storeId')).toBeNull()
      expect(localStorage.getItem('storeName')).toBeNull()
      expect(localStorage.getItem('roles')).toBeNull()
      expect(localStorage.getItem('currentStoreId')).toBeNull()
      expect(useUserStore().isLoggedIn).toBe(false)
      expect(router.currentRoute.value.path).toBe('/login')
    } finally {
      wrapper.unmount()
    }
  })
})
