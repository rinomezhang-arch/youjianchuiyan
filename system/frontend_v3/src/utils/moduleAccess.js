/**
 * 板块访问控制（前端侧）
 *
 * 权威数据来自后端 /api/auth/login 与 /api/auth/me 返回的 modules 字段：
 *   - modules 非空 → 受限用户，只能进入这些板块
 *   - modules 为空 → 不受限，行为与改动前一致
 *
 * 前端这层只做"看不见 + 进不去"的体验拦截；真正的强制拦截在后端
 * ModuleAccessInterceptor，即使绕过前端也无法调用其他板块的接口。
 */

export const MODULE_KEYS = [
  'front', 'menu', 'kitchen', 'supply', 'marketing', 'hr',
  'finance', 'engineering', 'gm', 'system', 'settings', 'analytics', 'legal'
]

export const MODULE_LABELS = {
  front: '前厅运营',
  menu: '菜单管理',
  kitchen: '厨房管理',
  supply: '采购仓储',
  marketing: '营销会员',
  hr: '人事行政',
  finance: '财务数据',
  engineering: '工程管理',
  gm: '总经办',
  system: '系统工具',
  settings: '系统设置',
  analytics: '数据大屏',
  legal: '法务'
}

/** 每个板块的首页：受限用户登录后直接落到这里 */
export const MODULE_HOME = {
  front: '/dashboard/front-office',
  menu: '/dashboard/menu',
  kitchen: '/dashboard/kitchen',
  supply: '/dashboard/supply-chain',
  marketing: '/dashboard/marketing',
  hr: '/dashboard/hr-admin',
  finance: '/dashboard/finance',
  engineering: '/dashboard/engineering',
  gm: '/dashboard/gm-office',
  system: '/dashboard/bill-manage',
  settings: '/dashboard/settings',
  analytics: '/dashboard/data-screen',
  legal: '/dashboard/legal'
}

/**
 * 路径前缀 → 板块。按声明顺序匹配，先写更长的前缀。
 * 未列出的 /dashboard/* 路径视为"公共页"（如工作台、桌台看板、帮助），
 * 但受限用户同样只能看到自身板块，见 canAccessPath。
 */
const PATH_MODULE_RULES = [
  ['/dashboard/legal', 'legal'],

  ['/dashboard/front-office', 'front'],
  ['/dashboard/front-desk', 'front'],
  ['/dashboard/guest-analysis', 'front'],
  ['/dashboard/staff-performance', 'front'],
  ['/dashboard/table-utilization', 'front'],
  ['/dashboard/report-print', 'front'],
  ['/dashboard/bookings', 'front'],
  ['/dashboard/banquet-notices', 'front'],
  ['/dashboard/customers', 'front'],
  ['/dashboard/table-layout', 'front'],
  ['/dashboard/art-design', 'front'],

  ['/dashboard/menu', 'menu'],
  ['/dashboard/menu-alacarte', 'menu'],
  ['/dashboard/menu-banquet', 'menu'],
  ['/dashboard/menu-detail', 'menu'],
  ['/dashboard/menu-festive', 'menu'],
  ['/dashboard/menu-full', 'menu'],
  ['/dashboard/menu-manager', 'menu'],
  ['/dashboard/menu-soldout', 'menu'],
  ['/dashboard/menu-sort', 'menu'],
  ['/dashboard/set-menu-edit', 'menu'],
  ['/dashboard/pricing-manage', 'menu'],
  ['/dashboard/categories', 'menu'],
  ['/dashboard/dictionaries', 'menu'],
  ['/dashboard/ordering', 'menu'],
  ['/dashboard/dish-library', 'menu'],
  ['/dashboard/cost-recipe', 'menu'],
  ['/dashboard/set-menu', 'menu'],
  ['/dashboard/pricing', 'menu'],
  ['/dashboard/sold-out', 'menu'],
  ['/dashboard/soldout-control', 'menu'],
  ['/dashboard/tags', 'menu'],
  ['/dashboard/print-config', 'menu'],
  ['/dashboard/store-permission', 'menu'],
  ['/dashboard/audit-log', 'menu'],
  ['/dashboard/price-tiers', 'menu'],
  ['/dashboard/category-sort', 'menu'],
  ['/dashboard/dict-manager', 'menu'],

  ['/dashboard/kitchen', 'kitchen'],
  ['/dashboard/kitchen-log', 'kitchen'],
  ['/dashboard/production', 'kitchen'],
  ['/dashboard/packages', 'kitchen'],

  ['/dashboard/supply-chain', 'supply'],
  ['/dashboard/inventory', 'supply'],
  ['/dashboard/procurement', 'supply'],
  ['/dashboard/receipt', 'supply'],
  ['/dashboard/issue', 'supply'],
  ['/dashboard/supplier-reconciliation', 'supply'],
  ['/dashboard/stock-take', 'supply'],
  ['/dashboard/suppliers', 'supply'],

  ['/dashboard/marketing', 'marketing'],
  ['/dashboard/member-list', 'marketing'],

  ['/dashboard/hr-admin', 'hr'],
  ['/dashboard/hr-analytics', 'hr'],
  ['/dashboard/staff', 'hr'],
  ['/dashboard/attendance', 'hr'],
  ['/dashboard/attendance-calendar', 'hr'],
  ['/dashboard/attendance-print', 'hr'],
  ['/dashboard/payroll', 'hr'],
  ['/dashboard/self-service', 'hr'],
  ['/dashboard/review-queue', 'hr'],
  ['/dashboard/training', 'hr'],
  ['/dashboard/schedule', 'hr'],
  ['/dashboard/leave', 'hr'],
  ['/dashboard/overtime', 'hr'],
  ['/dashboard/license', 'hr'],
  ['/dashboard/security', 'hr'],
  ['/dashboard/assets', 'hr'],

  ['/dashboard/finance', 'finance'],
  ['/dashboard/reports', 'finance'],
  ['/dashboard/dish-cost-analysis', 'finance'],

  ['/dashboard/engineering', 'engineering'],
  ['/dashboard/decoration', 'engineering'],
  ['/dashboard/maintenance', 'engineering'],
  ['/dashboard/energy', 'engineering'],
  ['/dashboard/safety', 'engineering'],
  ['/dashboard/floor-project', 'engineering'],

  ['/dashboard/gm-office', 'gm'],
  ['/dashboard/approval', 'gm'],

  ['/dashboard/settings', 'settings'],
  ['/dashboard/perm-manager', 'settings'],
  ['/dashboard/users', 'settings'],
  ['/dashboard/data-screen', 'analytics'],

  ['/dashboard/bill-manage', 'system'],
  ['/dashboard/system-checkup', 'system'],
  ['/dashboard/ipad-menu', 'system'],
  ['/dashboard/store-org', 'system'],
  ['/dashboard/change-logs', 'system'],
  ['/dashboard/admin', 'system'],
  ['/dashboard/ai-assistant', 'system'],
  ['/dashboard/export-panel', 'system']
]

/** 解析某个路径属于哪个板块；返回 null 表示公共页 */
export function resolveModule(path) {
  if (!path) return null
  for (const [prefix, moduleKey] of PATH_MODULE_RULES) {
    if (path === prefix || path.startsWith(`${prefix}/`)) {
      return moduleKey
    }
  }
  return null
}

/** 受限用户登录后的落地页 */
export function landingPath(allowedModules) {
  if (!allowedModules || allowedModules.length === 0) return '/dashboard/home'
  return MODULE_HOME[allowedModules[0]] || '/dashboard/home'
}

/**
 * 受限用户只能进入自身板块的页面：
 * 板块页按 modules 判定，公共页（工作台/桌台看板等）对受限用户同样关闭，
 * 唯一例外是登录页与登出流程（不经过本函数）。
 */
export function canAccessPath(allowedModules, path) {
  if (!allowedModules || allowedModules.length === 0) return true
  const moduleKey = resolveModule(path)
  if (!moduleKey) return false
  return allowedModules.includes(moduleKey)
}
