// 真实路由守卫测试：导入 src/router/index.js 的真 router 单例与真 beforeEach 守卫，
// 只把网络层换成假后端。判定对象是"导航最终落点"，不是对守卫代码的复述。
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router'
import { useUserStore } from '@/store/user'
import { useAdapter, clearLocal, seedIdentity, flushAll } from './helpers.mjs'
import { LEGAL_ENTRY_HREF, LEGAL_SHELL_PATH } from '@/utils/authScope'

async function goto(path) {
  await router.push(path).catch(() => {})
  await flushAll()
  return router.currentRoute.value
}

beforeEach(async () => {
  setActivePinia(createPinia())
  clearLocal()
  // 守卫对 /login 等无 requiresAuth 路由不拦截，作为每条用例的起始停靠点
  await goto('/login')
})

afterEach(() => {
  clearLocal()
})

describe('真实守卫：未登录', () => {
  it('无 token 访问 dashboard 一律回登录页并携带 redirect', async () => {
    const route = await goto('/dashboard/finance')
    expect(route.path).toBe('/login')
    expect(route.query.redirect).toBe('/dashboard/finance')
  })

  it('Codex 反例1：无 token 访问 404 兜底路由（/dashboard/not-exist）不得挂载后台外壳', async () => {
    const route = await goto('/dashboard/not-exist')
    expect(route.path).toBe('/login')
    expect(route.query.redirect).toBe('/dashboard/not-exist')
  })
})

describe('真实守卫：lawyer 送真实法务入口 /case/', () => {
  it('lawyer 访问任意 dashboard 路由被整页送往 /case/，SPA 导航取消', async () => {
    seedIdentity({ token: 'lawyer-token', storeId: 3, roles: ['lawyer'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'lawyer', staffName: '张律师' }, storeId: 3, storeName: '' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    await goto('/dashboard/finance')
    // 守卫对 lawyer 的动作：location 指向 /case/（整页离开 SPA），并取消本次 SPA 导航。
    // happy-dom 会真的尝试对 /case/ 发起导航请求（测试环境无服务器，连接被拒属预期噪音），
    // 因此这里断言 SPA 侧可观测的结果：导航被取消、停在原页。
    // location 落在 /case/ 的副作用已在 shell.dom.test.mjs 的 lawyer 用例中真实断言。
    expect(LEGAL_ENTRY_HREF).toBe('/case/')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})

describe('真实守卫：法务占位壳只放 gm', () => {
  it('manager 访问 /dashboard/legal 被拦回工作台', async () => {
    seedIdentity({ token: 'mgr-token', storeId: 1, roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'manager', staffName: '王经理' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const route = await goto(LEGAL_SHELL_PATH)
    expect(route.path).toBe('/dashboard/home')
  })

  it('gm 访问 /dashboard/legal 放行（显式壳路由，非 catch-all）', async () => {
    seedIdentity({ token: 'gm-token', storeId: 0, roles: ['gm'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'admin', staffName: '总经理' }, storeId: 0, storeName: '' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const route = await goto(LEGAL_SHELL_PATH)
    expect(route.path).toBe(LEGAL_SHELL_PATH)
    expect(route.name).toBe('LegalShell')
  })

  it('staff 访问 /dashboard/legal 同样拦回工作台', async () => {
    seedIdentity({ token: 'staff-token', storeId: 1, roles: ['staff'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'staff', staffName: '小李' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const route = await goto(LEGAL_SHELL_PATH)
    expect(route.path).toBe('/dashboard/home')
  })
})

describe('真实守卫：普通路由与身份恢复', () => {
  it('manager 携有效 token 直接进入目标页，init 恢复身份且不丢 storeId', async () => {
    seedIdentity({ token: 'mgr-token', storeId: 1, storeName: '宁国店', roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'manager', staffName: '王经理' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    const route = await goto('/dashboard/finance')
    expect(route.path).toBe('/dashboard/finance')
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(true)
    expect(store.storeId).toBe(1)
    expect(store.userInfo.role).toBe('manager')
  })

  it('GM 的 storeId=0 在 init 恢复后保持为 0', async () => {
    seedIdentity({ token: 'gm-token', storeId: 0, storeName: '', roles: ['gm'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 200, data: { user: { role: 'admin', staffName: '总经理' }, storeId: 0, storeName: '' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    await goto('/dashboard/home')
    const store = useUserStore()
    expect(store.storeId).toBe(0)
    expect(localStorage.getItem('storeId')).toBe('0')
    expect(localStorage.getItem('currentStoreId')).toBe('0')
  })

  it('/auth/me 失效时清空本地身份回登录页，不残留半套状态', async () => {
    seedIdentity({ token: 'dead-token', storeId: 1, roles: ['manager'] })
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        return { body: { code: 401, message: '登录已失效' } }
      }
      return { body: { code: 200, data: {} } }
    })
    const route = await goto('/dashboard/home')
    expect(route.path).toBe('/login')
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('storeId')).toBeNull()
    expect(localStorage.getItem('roles')).toBeNull()
  })

  it('Codex 反例2：/auth/me 返回的用户无 role 时，旧 roles=["admin"] 不得被采信为 gm 并持久化', async () => {
    seedIdentity({ token: 't', storeId: 1, roles: ['admin'] }) // 篡改/残留的旧角色
    useAdapter(async (url) => {
      if (url === '/auth/me') {
        // 服务端没给 role（role 字段缺失）
        return { body: { code: 200, data: { user: { staffName: '无名氏' }, storeId: 1, storeName: '宁国店' } } }
      }
      return { body: { code: 200, data: {} } }
    })
    await goto('/dashboard/home')
    const store = useUserStore()
    // 缺权威角色必须落最低权限，不得提权
    expect(store.roles).toEqual(['staff'])
    expect(localStorage.getItem('roles')).toBe('["staff"]')
    // staff 锁定本店：越权切换被拒
    expect(store.switchStore(2)).toBe(false)
    expect(store.storeId).toBe(1)
  })
})
