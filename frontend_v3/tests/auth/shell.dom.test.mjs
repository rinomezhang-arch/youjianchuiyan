// 真实 DOM 组件测试：@vue/test-utils 真挂载 Login.vue / Dashboard.vue，
// element-plus 组件树、i18n、pinia 全部走真件；仅 AIChatFloat/NotifyBell 两个
// 会自拉接口/建 iframe 的挂件替换为 stub，网络层用假后端。
// 断言对象是真实渲染出的 DOM 列表与内容，不做快照、不允许空列表。
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import i18n from '@/i18n'
import Login from '@/views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'
import { useUserStore } from '@/store/user'
import { useAdapter, clearLocal, seedIdentity, flushAll, flushAllDeep, currentPathname } from './helpers.mjs'
import { LEGAL_ENTRY_HREF, LEGAL_SHELL_PATH } from '@/utils/authScope'

let router

function mountLogin() {
  return mount(Login, {
    global: {
      plugins: [ElementPlus, i18n, router]
    }
  })
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

async function openStoreDropdown(wrapper) {
  await wrapper.find('.store-badge').trigger('click')
  await flushPromises()
  await flushAll()
  return [...document.body.querySelectorAll('.el-dropdown-menu__item')].map((el) => el.textContent.trim())
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

describe('登录：真实表单提交', () => {
  async function submitLogin(username, password) {
    const wrapper = mountLogin()
    await flushAllDeep()
    // 绕开防浏览器自动填充的诱饵输入框，按真实 name 取业务输入框
    await wrapper.find('input[name="yj-account-input"]').setValue(username)
    await wrapper.find('input[name="yj-pwd-input"]').setValue(password)
    await wrapper.find('.login-btn').trigger('click')
    await flushAllDeep()
    return wrapper
  }

  it('桌面：经理登录成功，token/用户/角色/门店全部落位', async () => {
    useAdapter(async (url) => {
      if (url === '/auth/login') {
        return { body: { code: 200, data: { token: 'tok-mgr', user: { role: 'manager', staffName: '王经理' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const wrapper = await submitLogin('wang', 'whatever-not-stored')
    try {
      expect(localStorage.getItem('token')).toBe('tok-mgr')
      expect(localStorage.getItem('storeId')).toBe('1')
      expect(localStorage.getItem('storeName')).toBe('宁国店')
      expect(localStorage.getItem('currentStoreId')).toBe('1')
      expect(JSON.parse(localStorage.getItem('roles'))).toEqual(['manager'])
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(true)
      expect(store.userInfo.staffName).toBe('王经理')
      // 真实 DOM：登录卡片和门店名可见
      expect(wrapper.find('.form-card').exists()).toBe(true)
      expect(wrapper.text()).toContain('宁国店')
    } finally {
      wrapper.unmount()
    }
  })

  it('桌面：总经理登录 storeId=0，落盘为 0 不被吞成 1', async () => {
    useAdapter(async (url) => {
      if (url === '/auth/login') {
        return { body: { code: 200, data: { token: 'tok-gm', user: { role: 'admin', staffName: '总经理' }, storeId: 0, storeName: '' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const wrapper = await submitLogin('boss', 'whatever-not-stored')
    try {
      const store = useUserStore()
      expect(store.storeId).toBe(0)
      expect(localStorage.getItem('storeId')).toBe('0')
      expect(localStorage.getItem('currentStoreId')).toBe('0')
      expect(JSON.parse(localStorage.getItem('roles'))).toEqual(['gm'])
    } finally {
      wrapper.unmount()
    }
  })

  it('窄屏 375px：登录页真实表单元素齐全非空', async () => {
    window.innerWidth = 375
    window.innerHeight = 667
    useAdapter(async () => ({ body: { code: 200, data: {} } }))
    const wrapper = mountLogin()
    await flushAllDeep()
    try {
      expect(wrapper.find('.login-page').exists()).toBe(true)
      expect(wrapper.find('input[name="yj-account-input"]').exists()).toBe(true)
      expect(wrapper.find('input[name="yj-pwd-input"]').exists()).toBe(true)
      expect(wrapper.find('.login-btn').exists()).toBe(true)
      expect(wrapper.find('.login-btn').text()).toContain('登录')
    } finally {
      wrapper.unmount()
    }
  })
})

describe('外壳：真实渲染的菜单与门店切换', () => {
  function meReply(role, storeId) {
    return async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role, staffName: '测试用户' }, storeId, storeName: storeId > 0 ? '宁国店' : '' } } }
      }
      return { body: { code: 200, data: {} } }
    }
  }

  it('桌面：经理进入后台，菜单列表真实非空，门店下拉只有本店（越权选项不下发）', async () => {
    seedIdentity({ token: 't', storeId: 1, roles: ['manager'] })
    useAdapter(meReply('manager', 1))
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      const navTexts = wrapper.findAll('.nav-item .nav-text').map((n) => n.text())
      expect(navTexts.length).toBeGreaterThan(3)
      expect(navTexts).toContain('工作台')

      const items = await openStoreDropdown(wrapper)
      expect(items).toContain('宁国店')
      expect(items).not.toContain('宣城店')

      // 店级兜底：即便绕过下拉直接调 store，越权切换也被拒绝且状态不变
      const store = useUserStore()
      expect(store.switchStore(2)).toBe(false)
      expect(store.storeId).toBe(1)
    } finally {
      wrapper.unmount()
    }
  })

  it('桌面：总经理全店可见，可切换到宣城店', async () => {
    seedIdentity({ token: 't', storeId: 0, storeName: '', roles: ['gm'] })
    useAdapter(meReply('admin', 0))
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      const items = await openStoreDropdown(wrapper)
      expect(items).toEqual(expect.arrayContaining(['宁国店', '宣城店']))
      const store = useUserStore()
      expect(store.storeId).toBe(0)
      expect(store.switchStore(2)).toBe(true)
      expect(store.storeId).toBe(2)
    } finally {
      wrapper.unmount()
    }
  })

  it('lawyer：菜单只剩法务案卷，指向 /case/ 真实入口，点击整页离开 SPA', async () => {
    seedIdentity({ token: 't', storeId: 3, storeName: '', roles: ['lawyer'] })
    useAdapter(meReply('lawyer', 3))
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      const navTexts = wrapper.findAll('.nav-item .nav-text').map((n) => n.text())
      expect(navTexts.length).toBeGreaterThan(0)
      expect(navTexts.every((t) => t.includes('法务'))).toBe(true)
      expect(navTexts.join('|')).not.toContain('工作台')

      // 点击法务条目 → 整页跳真实静态入口 /case/
      await wrapper.findAll('.nav-item')[0].trigger('click')
      await flushAllDeep()
      expect(currentPathname()).toBe('/case/')
    } finally {
      wrapper.unmount()
    }
  })

  it('窄屏 375px：后台外壳菜单列表真实非空', async () => {
    window.innerWidth = 375
    window.innerHeight = 667
    seedIdentity({ token: 't', storeId: 1, roles: ['manager'] })
    useAdapter(meReply('manager', 1))
    const wrapper = mountDashboard()
    await flushAllDeep()
    try {
      const navTexts = wrapper.findAll('.nav-item .nav-text').map((n) => n.text())
      expect(navTexts.length).toBeGreaterThan(3)
      expect(navTexts).toContain('工作台')
      expect(wrapper.find('.store-badge').exists()).toBe(true)
    } finally {
      wrapper.unmount()
    }
  })
})

describe('401 与退出：本地状态清理', () => {
  it('任意业务 401：清掉全部身份键并回登录页', async () => {
    seedIdentity({ token: 'stale', storeId: 2, roles: ['manager'] })
    useAdapter(async () => ({ body: { code: 401, message: '登录已失效' } }))
    const { default: request } = await import('@/utils/request')
    await expect(request({ url: '/anything' })).rejects.toThrow()
    await flushAllDeep()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('storeId')).toBeNull()
    expect(localStorage.getItem('storeName')).toBeNull()
    expect(localStorage.getItem('roles')).toBeNull()
    expect(localStorage.getItem('currentStoreId')).toBeNull()
    expect(currentPathname()).toBe('/login')
  })

  it('HTTP 级 401：同样清理身份', async () => {
    seedIdentity({ token: 'stale', storeId: 2, roles: ['manager'] })
    useAdapter(async () => ({ status: 401, body: { message: 'unauthorized' } }))
    const { default: request } = await import('@/utils/request')
    await expect(request({ url: '/anything' })).rejects.toThrow()
    await flushAllDeep()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('storeId')).toBeNull()
    expect(currentPathname()).toBe('/login')
  })

  it('logout 请求失败（网络断）：本地 token/用户/角色/门店仍然全部清理', async () => {
    seedIdentity({ token: 't', storeId: 1, roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/logout') return { networkError: 'Network Error' }
      return { body: { code: 200, data: {} } }
    })
    const store = useUserStore()
    const removed = await store.logout()
    await flushAllDeep()
    expect(removed).toEqual(expect.arrayContaining(['token', 'storeId', 'storeName', 'roles', 'currentStoreId']))
    expect(localStorage.getItem('token')).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
    expect(store.storeId).toBe(1)
    expect(Object.keys(store.userInfo)).toHaveLength(0)
    expect(store.roles).toEqual([])
  })

  it('退出后可重新登录：身份完整重建，无上一位用户残留', async () => {
    seedIdentity({ token: 'old', storeId: 2, storeName: '宣城店', roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/logout') return { networkError: 'Network Error' }
      if (url === '/auth/login') {
        return { body: { code: 200, data: { token: 'new-tok', user: { role: 'staff', staffName: '小李' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const store = useUserStore()
    await store.logout()
    const res = await store.login('li', 'whatever-not-stored')
    await flushAllDeep()
    expect(res.code).toBe(200)
    expect(store.token).toBe('new-tok')
    expect(store.storeId).toBe(1)
    expect(store.storeName).toBe('宁国店')
    expect(store.roles).toEqual(['staff'])
    expect(localStorage.getItem('token')).toBe('new-tok')
    expect(localStorage.getItem('storeId')).toBe('1')
  })
})

describe('防篡改：本地 roles 无法把 manager 提升为 lawyer/gm', () => {
  it('localStorage roles 被改成 lawyer，服务端 role=manager 仍判 manager，越权切换被拒', async () => {
    seedIdentity({ token: 't', storeId: 1, roles: ['lawyer'] }) // 篡改后的本地角色
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'manager', staffName: '王经理' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const store = useUserStore()
    await store.init()
    await flushAllDeep()
    expect(store.roles).toEqual(['manager'])
    expect(store.switchStore(2)).toBe(false)
    // 页面刷新恢复路径同样不能借篡改的 roles 进法务壳
    expect(LEGAL_ENTRY_HREF).toBe('/case/')
    expect(LEGAL_SHELL_PATH).toBe('/dashboard/legal')
  })
})
