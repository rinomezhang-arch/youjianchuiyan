// authScope 规则单元测试：角色权威、store 规范化、切换许可、守卫裁决、身份清理。
import { describe, it, expect } from 'vitest'
import {
  canonicalRole, normalizeStoreId, switchableStores, canSwitchTo,
  canAccessDashboardRoute, redirectFor, clearIdentity, IDENTITY_STORAGE_KEYS,
  LEGAL_ENTRY_HREF, LEGAL_SHELL_PATH
} from '@/utils/authScope'

describe('canonicalRole：服务端 role 唯一权威', () => {
  it('服务端 role 存在时，本地 roles 被篡改也不参与判定（防提权）', () => {
    expect(canonicalRole({ role: 'manager' }, ['lawyer'])).toBe('manager')
    expect(canonicalRole({ role: 'manager' }, ['super_admin', 'gm'])).toBe('manager')
    expect(canonicalRole({ role: 'staff' }, ['lawyer', 'admin'])).toBe('staff')
  })

  it('服务端 role 缺失时才用本地 roles 兜底', () => {
    expect(canonicalRole({}, ['lawyer'])).toBe('lawyer')
    expect(canonicalRole(null, ['store_manager'])).toBe('manager')
    expect(canonicalRole(undefined, ['admin'])).toBe('gm')
    expect(canonicalRole({}, ['super_admin'])).toBe('gm')
    expect(canonicalRole({}, ['gm'])).toBe('gm')
    expect(canonicalRole({}, [])).toBe('staff')
  })

  it('服务端 role 全集映射（admin 归 gm）', () => {
    expect(canonicalRole({ role: 'lawyer' }, [])).toBe('lawyer')
    expect(canonicalRole({ role: 'gm' }, [])).toBe('gm')
    expect(canonicalRole({ role: 'super_admin' }, [])).toBe('gm')
    expect(canonicalRole({ role: 'admin' }, [])).toBe('gm')
    expect(canonicalRole({ role: 'manager' }, [])).toBe('manager')
    expect(canonicalRole({ role: 'store_manager' }, [])).toBe('manager')
    expect(canonicalRole({ role: 'staff' }, [])).toBe('staff')
  })

  it('大小写与空白容忍，未知 role 一律按最紧的 staff 处理', () => {
    expect(canonicalRole({ role: '  Lawyer ' }, [])).toBe('lawyer')
    expect(canonicalRole({ role: 'visitor' }, [])).toBe('staff')
  })
})

describe('normalizeStoreId：0 是总经理的合法身份，不能被吞', () => {
  it('0 保持为 0', () => {
    expect(normalizeStoreId(0, 1)).toBe(0)
    expect(normalizeStoreId('0', 1)).toBe(0)
  })
  it('正整数透传（含字符串）', () => {
    expect(normalizeStoreId(2, 1)).toBe(2)
    expect(normalizeStoreId('2', 1)).toBe(2)
  })
  it('空值/负数/非整数落回 fallback', () => {
    expect(normalizeStoreId(null, 1)).toBe(1)
    expect(normalizeStoreId(undefined, 1)).toBe(1)
    expect(normalizeStoreId('', 1)).toBe(1)
    expect(normalizeStoreId(-1, 1)).toBe(1)
    expect(normalizeStoreId('abc', 1)).toBe(1)
    expect(normalizeStoreId(1.5, 1)).toBe(1)
  })
})

describe('门店切换许可', () => {
  const STORES = [{ id: 1, name: '宁国店' }, { id: 2, name: '宣城店' }]

  it('gm 可切换任意真实门店，清单全量下发', () => {
    expect(switchableStores('gm', 0, STORES)).toHaveLength(2)
    expect(canSwitchTo('gm', 0, 1)).toBe(true)
    expect(canSwitchTo('gm', 0, 2)).toBe(true)
  })

  it('manager 只能留在自己门店，越权目标一律拒绝', () => {
    expect(switchableStores('manager', 1, STORES)).toEqual([{ id: 1, name: '宁国店' }])
    expect(canSwitchTo('manager', 1, 2)).toBe(false)
    expect(canSwitchTo('manager', 1, 1)).toBe(true)
  })

  it('staff/lawyer 同样锁定本店', () => {
    expect(canSwitchTo('staff', 2, 1)).toBe(false)
    expect(canSwitchTo('lawyer', 3, 1)).toBe(false)
  })

  it('非法目标（0/负数）对任何角色都拒绝', () => {
    expect(canSwitchTo('gm', 0, 0)).toBe(false)
    expect(canSwitchTo('gm', 0, -2)).toBe(false)
  })
})

describe('路由守卫裁决', () => {
  it('lawyer 在 SPA 内没有任何合法停留页', () => {
    expect(canAccessDashboardRoute('lawyer', '/dashboard/home')).toBe(false)
    expect(canAccessDashboardRoute('lawyer', '/dashboard/legal')).toBe(false)
    expect(canAccessDashboardRoute('lawyer', '/dashboard/finance')).toBe(false)
  })

  it('lawyer 被拦时重定向到真实法务入口 /case/（整页跳转标记）', () => {
    expect(redirectFor('lawyer', '/dashboard/home')).toBe(LEGAL_ENTRY_HREF)
    expect(LEGAL_ENTRY_HREF).toBe('/case/')
  })

  it('法务占位壳只放 gm；staff/manager 拦回工作台', () => {
    expect(canAccessDashboardRoute('gm', LEGAL_SHELL_PATH)).toBe(true)
    expect(canAccessDashboardRoute('manager', LEGAL_SHELL_PATH)).toBe(false)
    expect(canAccessDashboardRoute('staff', LEGAL_SHELL_PATH)).toBe(false)
    expect(redirectFor('manager', LEGAL_SHELL_PATH)).toBe('/dashboard/home')
    expect(redirectFor('staff', LEGAL_SHELL_PATH)).toBe('/dashboard/home')
  })

  it('非 lawyer 访问普通路由不在壳层加码', () => {
    expect(canAccessDashboardRoute('manager', '/dashboard/finance')).toBe(true)
    expect(canAccessDashboardRoute('staff', '/dashboard/home')).toBe(true)
    expect(redirectFor('gm', '/dashboard/home')).toBeNull()
  })
})

describe('clearIdentity：僵尸登录态清理', () => {
  it('只清身份键，别的键一律不碰', () => {
    localStorage.setItem('token', 't')
    localStorage.setItem('storeId', '1')
    localStorage.setItem('storeName', '宁国店')
    localStorage.setItem('roles', '["staff"]')
    localStorage.setItem('currentStoreId', '1')
    localStorage.setItem('sidebar_menu_order', '[]')
    localStorage.setItem('ipad_device_sn', 'SN123')

    const removed = clearIdentity(localStorage)

    expect(new Set(removed)).toEqual(new Set(IDENTITY_STORAGE_KEYS))
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('storeId')).toBeNull()
    expect(localStorage.getItem('storeName')).toBeNull()
    expect(localStorage.getItem('roles')).toBeNull()
    expect(localStorage.getItem('currentStoreId')).toBeNull()
    // 业务键与 iPad 绑定键不属于身份，不能被顺手清掉
    expect(localStorage.getItem('sidebar_menu_order')).toBe('[]')
    expect(localStorage.getItem('ipad_device_sn')).toBe('SN123')
  })

  it('重复清理安全（幂等）', () => {
    localStorage.setItem('token', 't')
    expect(clearIdentity(localStorage)).toEqual(['token'])
    expect(clearIdentity(localStorage)).toEqual([])
  })
})
