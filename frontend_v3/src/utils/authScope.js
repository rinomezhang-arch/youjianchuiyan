// 身份与门店作用域规则（TR-AUTH-SHELL-14）。
// 这里只做**外壳层**的判定：登录态清理、门店切换许可、角色可见菜单与路由守卫。
// 页面内部的业务权限仍以服务端为准——本文件的裁决只决定"壳给不给你进"。

// 角色规范化。**服务端 userInfo.role 是唯一权威**：
//   - 服务端给了 role：按 role 归类，本地 roles 一律不参与（防止篡改 localStorage 提权）；
//   - 服务端 role 缺失：**取最低权限 staff，拒绝采信旧 roles**——
//     缺权威角色宁可错杀，也绝不允许 ['admin']/['lawyer'] 这类本地残留把人提成 gm/lawyer。
//   - lawyer（代理律师，后端 JwtAuthInterceptor 只放行 /api/legal/** + me/logout）
//   - gm（总经理；后端 legal.allowed-roles 里的 gm/super_admin/admin 都归到这里）
//   - manager（店长，绑定单一门店）
//   - staff（其余员工，绑定单一门店；也是无权威角色时的兜底）
export function canonicalRole(userInfo, roles) {
  const info = userInfo && typeof userInfo === 'object' ? userInfo : {}
  const raw = String(info.role ?? '').trim().toLowerCase()

  if (!raw) return 'staff'
  if (raw === 'lawyer') return 'lawyer'
  if (['gm', 'super_admin', 'admin'].includes(raw)) return 'gm'
  if (raw === 'manager' || raw === 'store_manager') return 'manager'
  return 'staff'
}

// storeId 规范化：0 是总经理的合法身份（全店视角），绝不能被 || 1 吞掉。
// 只有正整数才是具体门店；其余一律落回 fallback。
export function normalizeStoreId(value, fallback = 0) {
  if (value === null || value === undefined || value === '') return fallback
  const n = Number(value)
  if (!Number.isSafeInteger(n) || n < 0) return fallback
  return n
}

// 当前角色可切换的门店清单。gm 全店可见；其余角色只看得到自己绑定的门店。
export function switchableStores(role, storeId, stores) {
  const list = Array.isArray(stores) ? stores : []
  if (role === 'gm') return list.slice()
  return list.filter(s => Number(s.id) === Number(storeId))
}

// 能否切换到目标门店。越权切换一律拒绝，且不产生任何状态变更。
export function canSwitchTo(role, storeId, targetId) {
  const target = normalizeStoreId(targetId, -1)
  if (target <= 0) return false
  if (role === 'gm') return true
  return Number(storeId) === target
}

// 法务入口。**真实入口是 /case/**（public/case/index.html 静态页，服务器直接托管，
// 不经过 SPA 路由）；/dashboard/legal 只是占位壳，不能作为律师入口。
// lawyer 在 SPA 内没有合法停留页——登录后和访问任何 dashboard 路由都被守卫送 /case/。
export const LEGAL_ENTRY_HREF = '/case/'

// lawyer 菜单条目：href 指向真实入口，由外壳用整页跳转离开 SPA。
// path 仅作菜单 key/占位比较，不是可导航路由。
export const LEGAL_SHELL_MENU = [
  { name: '法务案卷', sub: 'Legal', path: '/case/', href: LEGAL_ENTRY_HREF, module: 'legal', icon: 'license' }
]

// /dashboard/legal 显式壳路由（requiresAuth）仍保留，供 gm 查看占位；
// staff/manager 不放，防止壳路径对他们出现"可进"的假象。
export const LEGAL_SHELL_PATH = '/dashboard/legal'

// 角色在 dashboard 外壳里可见的模块入口。lawyer 只见法务入口。
export function dashboardEntriesForRole(role, entries) {
  const list = Array.isArray(entries) ? entries : []
  if (role === 'lawyer') return LEGAL_SHELL_MENU.slice()
  return list
}

// 路由守卫裁决（外壳层）：
//   lawyer：SPA 内任何 /dashboard 路由都不放行，由守卫整页送 /case/（见 redirectFor 返回 LEGAL_ENTRY_HREF）；
//   /dashboard/legal 壳路径仅 gm 可进（与后端 legal.allowed-roles=lawyer,gm,super_admin,admin 对齐）；
//   其余角色访问普通路由不在壳层加码——页面级权限由服务端与各页面自行裁决。
export function canAccessDashboardRoute(role, path) {
  if (role === 'lawyer') return false
  if (path === LEGAL_SHELL_PATH) return role === 'gm'
  return true
}

// 守卫拦截后的去向：
//   返回 LEGAL_ENTRY_HREF 表示必须整页跳转真实法务入口（不走 SPA next）；
//   返回 SPA 路径串表示 router 内部重定向；null 表示放行。
export function redirectFor(role, path) {
  if (!canAccessDashboardRoute(role, path)) {
    if (role === 'lawyer') return LEGAL_ENTRY_HREF
    return '/dashboard/home'
  }
  return null
}

// 401/登出时必须清掉的本地身份键。任何一处遗漏都会留下"僵尸登录态"。
export const IDENTITY_STORAGE_KEYS = ['token', 'storeId', 'storeName', 'roles', 'currentStoreId']

// 清理本地陈旧身份。返回实际删除的键，供测试与日志取证。
export function clearIdentity(storage) {
  const removed = []
  for (const key of IDENTITY_STORAGE_KEYS) {
    try {
      if (storage.getItem(key) !== null) {
        storage.removeItem(key)
        removed.push(key)
      }
    } catch {
      // 个别 storage 实现可能抛错，清理不因单键失败而中断
    }
  }
  return removed
}
