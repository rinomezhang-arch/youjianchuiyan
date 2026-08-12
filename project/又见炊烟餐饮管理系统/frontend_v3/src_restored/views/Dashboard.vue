<template>
  <div class="dashboard">
    <aside :class="['sidebar', { collapsed: sidebarCollapsed }]" @dblclick="toggleSidebar">
      <div class="sidebar-logo">
        <div class="sidebar-logo-icon">
          <img src="@/assets/images/logo.png" alt="又见炊烟" />
        </div>
      </div>
      <div class="sidebar-toggle-hint" v-if="!sidebarCollapsed">双击收起</div>
      <nav class="nav-menu">
        <!-- 模块标题 -->
        <div v-if="activeModule" class="nav-group-title module-header" @click="goTo('/dashboard/home')" title="返回工作台">
          <div class="module-header-text">
            <span class="module-header-cn">{{ moduleLabels[activeModule]?.cn || '' }}</span>
            <span class="module-header-en">{{ moduleLabels[activeModule]?.en || '' }}</span>
          </div>
        </div>
        <div class="nav-group">
          <a
            v-for="(item, idx) in sidebarMenu"
            :key="item.path"
            :class="['nav-item', { 'nav-item-home': item.path === '/dashboard/home' || item.path === '/dashboard/table-board', 'nav-item-module': item.module, active: isActive(item.path), 'drag-over': dragOverIdx === idx, dragging: dragIdx === idx }]"
            draggable="true"
            @dragstart="onDragStart(idx, $event)"
            @dragover.prevent="onDragOver(idx, $event)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop(idx)"
            @dragend="onDragEnd"
            @click="goTo(item.path)"
            :title="sidebarCollapsed ? item.name : ''"
          >
            <span class="drag-handle">
              <svg width="10" height="14" viewBox="0 0 10 14" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="3" cy="2" r="1"/><circle cx="7" cy="2" r="1"/>
                <circle cx="3" cy="7" r="1"/><circle cx="7" cy="7" r="1"/>
                <circle cx="3" cy="12" r="1"/><circle cx="7" cy="12" r="1"/>
              </svg>
            </span>
            <span class="nav-icon" v-if="item.icon" v-html="iconSvg(item.icon)"></span>
            <span class="nav-text">{{ item.name }}</span>
            <span class="nav-sub">{{ item.sub }}</span>
            <span v-if="isActive(item.path)" class="nav-indicator"></span>
          </a>
        </div>
      </nav>
    </aside>
    <div class="main-content-wrapper">
      <header class="header">
      <div class="header-gold-line">
        <div class="header-gold-shimmer"></div>
      </div>
      <div class="header-left">
        <div class="store-badge">{{ storeName }}</div>
      </div>
      <div class="header-center">
        <div class="logo">
          <div class="logo-icon">
            <img src="@/assets/images/logo.png" alt="又见炊烟" />
          </div>
          <div class="logo-text">
            <div class="logo-main-row">
              <span class="logo-main">又见炊烟私房菜</span>
              <span class="logo-divider">·</span>
              <span class="logo-sub-title">{{ t('header.system') }}</span>
            </div>
            <div class="logo-eng-row">
              <span class="logo-eng">Youjianchuiyan</span>
              <span class="logo-divider-light">·</span>
              <span class="logo-eng-sub">{{ t('header.systemEn') }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="header-right">
        <button class="refresh-btn" @click="refreshPage" title="刷新页面">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
        </button>
        <div class="user-info">
          <span class="user-name">{{ userInfo.staffName || 'User' }}</span>
          <span class="user-role">{{ userInfo.staffPosition || userInfo.role || '' }}</span>
        </div>
        <el-dropdown @command="handleCommand" trigger="click">
          <div class="avatar">
            <span>{{ userInfo.staffName?.charAt(0) || 'U' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home">{{ t('sidebar.dashboard') }} · {{ t('sidebar.dashboardEn') }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">{{ t('common.logout') }} · {{ t('common.logoutEn') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showLogoutModal" class="modal-overlay" @click.self="cancelLogout">
          <div class="modal-dialog">
            <div class="modal-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </div>
            <h3 class="modal-title">{{ t('common.logout') }} · {{ t('common.logoutEn') }}</h3>
            <p class="modal-desc">{{ t('common.confirm') }} · {{ t('common.confirmEn') }}?</p>
            <div class="modal-actions">
              <button class="modal-btn btn-cancel" @click="cancelLogout">{{ t('common.cancel') }} · {{ t('common.cancelEn') }}</button>
              <button class="modal-btn btn-confirm" @click="confirmLogout">{{ t('common.confirm') }} · {{ t('common.confirmEn') }}</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <AIChatFloat />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import AIChatFloat from '@/components/AIChatFloat.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { t } = useI18n()

onMounted(() => {
  userStore.init()
})

const storeName = computed(() => userStore.storeName || '宁国店')
const userInfo = computed(() => userStore.userInfo || {})

const coreMenu = [
  { name: t('sidebar.dashboard'), sub: t('sidebar.dashboardEn'), path: '/dashboard/home', icon: 'home' },
  { name: '桌台看板', sub: 'Table Board', path: '/dashboard/table-board', icon: 'table' },
  { name: '菜单菜牌', sub: 'Menu', path: '/dashboard/menu', icon: 'menu' }
]

const allModulePages = [
  // 前厅运营
  { name: '前厅运营', sub: 'Front Office', path: '/dashboard/front-office', module: 'front', icon: 'front' },
  { name: '前台预定', sub: 'Front Desk', path: '/dashboard/front-desk', module: 'front', icon: 'booking' },
  { name: '客人分析', sub: 'Guest Analysis', path: '/dashboard/guest-analysis', module: 'front', icon: 'guest' },
  { name: '员工绩效', sub: 'Staff Performance', path: '/dashboard/staff-performance', module: 'front', icon: 'staff' },
  { name: '桌台利用率', sub: 'Table Utilization', path: '/dashboard/table-utilization', module: 'front', icon: 'tableUtil' },
  { name: '报表打印', sub: 'Report & Print', path: '/dashboard/report-print', module: 'front', icon: 'print' },
  { name: '预订管理', sub: 'Bookings', path: '/dashboard/bookings', module: 'front', icon: 'bookings' },
  { name: '客户管理', sub: 'Customers', path: '/dashboard/customers', module: 'front', icon: 'customer' },
  { name: '台型设计', sub: 'Table Layout', path: '/dashboard/table-layout', module: 'front', icon: 'layout' },
  { name: '美工设计', sub: 'Art Design', path: '/dashboard/art-design', module: 'front', icon: 'art' },
  // 厨房管理
  { name: '厨房管理', sub: 'Kitchen', path: '/dashboard/kitchen', module: 'kitchen', icon: 'kitchen' },
  { name: '出品管理', sub: 'Production', path: '/dashboard/production', module: 'kitchen', icon: 'production' },
  { name: '套餐管理', sub: 'Packages', path: '/dashboard/packages', module: 'kitchen', icon: 'package' },
  // 采购仓储
  { name: '采购仓储', sub: 'Procurement & Storage', path: '/dashboard/supply-chain', module: 'supply', icon: 'supply' },
  { name: '库存管理', sub: 'Inventory', path: '/dashboard/inventory', module: 'supply', icon: 'inventory' },
  { name: '采购管理', sub: 'Procurement', path: '/dashboard/procurement', module: 'supply', icon: 'procurement' },
  { name: '入库验收', sub: 'Receipt', path: '/dashboard/receipt', module: 'supply', icon: 'receipt' },
  { name: '领用出库', sub: 'Issue', path: '/dashboard/issue', module: 'supply', icon: 'issue' },
  { name: '供应商对账', sub: 'Supplier Reconciliation', path: '/dashboard/supplier-reconciliation', module: 'supply', icon: 'reconciliation' },
  { name: '盘点', sub: 'Stock Take', path: '/dashboard/stock-take', module: 'supply', icon: 'stocktake' },
  { name: '供应商', sub: 'Suppliers', path: '/dashboard/suppliers', module: 'supply', icon: 'supplier' },
  // 营销会员
  { name: '营销会员', sub: 'Marketing', path: '/dashboard/marketing', module: 'marketing', icon: 'marketing' },
  // 人事行政
  { name: '人事行政', sub: 'HR Admin', path: '/dashboard/hr-admin', module: 'hr', icon: 'hr' },
  { name: '员工档案', sub: 'Staff', path: '/dashboard/staff', module: 'hr', icon: 'staffFile' },
  { name: '考勤日历', sub: 'Att Calendar', path: '/dashboard/attendance-calendar', module: 'hr', icon: 'attendance' },
  { name: '考勤报表', sub: 'Att Print', path: '/dashboard/attendance-print', module: 'hr', icon: 'print' },
  { name: '工资管理', sub: 'Payroll', path: '/dashboard/payroll', module: 'hr', icon: 'finance' },
  { name: 'HR数据', sub: 'HR Analytics', path: '/dashboard/hr-analytics', module: 'hr', icon: 'analytics' },
  { name: '自助登记', sub: 'Self Service', path: '/dashboard/self-service', module: 'hr', icon: 'staff' },
  { name: '审核队列', sub: 'Review Queue', path: '/dashboard/review-queue', module: 'hr', icon: 'license' },
  { name: '培训管理', sub: 'Training', path: '/dashboard/training', module: 'hr', icon: 'training' },
  { name: '考勤管理', sub: 'Attendance', path: '/dashboard/attendance', module: 'hr', icon: 'attendance' },
  { name: '排班管理', sub: 'Schedule', path: '/dashboard/schedule', module: 'hr', icon: 'schedule' },
  { name: '请假管理', sub: 'Leave', path: '/dashboard/leave', module: 'hr', icon: 'leave' },
  { name: '证照管理', sub: 'License', path: '/dashboard/license', module: 'hr', icon: 'license' },
  { name: '安保保洁', sub: 'Security', path: '/dashboard/security', module: 'hr', icon: 'security' },
  { name: '行政资产', sub: 'Assets', path: '/dashboard/assets', module: 'hr', icon: 'assets' },
  // 财务数据
  { name: '财务数据', sub: 'Finance', path: '/dashboard/finance', module: 'finance', icon: 'finance' },
  { name: '菜品成本', sub: 'Dish Cost', path: '/dashboard/finance/dish-cost', module: 'finance', icon: 'cost' },
  { name: '成本分析', sub: 'Cost Analysis', path: '/dashboard/finance/cost-analysis', module: 'finance', icon: 'analysis' },
  { name: '数据报表', sub: 'Reports', path: '/dashboard/reports', module: 'finance', icon: 'report' },
  { name: '菜品成本分析', sub: 'Dish Analysis', path: '/dashboard/dish-cost-analysis', module: 'finance', icon: 'dishAnalysis' },
  // 系统设置
  { name: '系统设置', sub: 'Settings', path: '/dashboard/settings', module: 'settings', icon: 'settings' },
  { name: '权限管理', sub: 'Permissions', path: '/dashboard/perm-manager', module: 'settings', icon: 'permission' },
  { name: '改动日志', sub: 'Change Log', path: '/dashboard/change-logs', module: 'settings', icon: 'changelog' },
  // 数据大屏
  { name: '数据大屏', sub: 'Analytics', path: '/dashboard/data-screen', module: 'analytics', icon: 'analytics' },
  // 工程管理
  { name: '工程管理', sub: 'Engineering', path: '/dashboard/engineering', module: 'engineering', icon: 'engineering' },
  { name: '装修管理', sub: 'Decoration', path: '/dashboard/decoration', module: 'engineering', icon: 'decoration' },
  { name: '设备维护', sub: 'Maintenance', path: '/dashboard/maintenance', module: 'engineering', icon: 'maintenance' },
  { name: '能耗管理', sub: 'Energy', path: '/dashboard/energy', module: 'engineering', icon: 'energy' },
  { name: '安全管理', sub: 'Safety', path: '/dashboard/safety', module: 'engineering', icon: 'safety' },
  { name: '工程维护', sub: 'Floor Maintenance', path: '/dashboard/floor-project', module: 'engineering', icon: 'floorMaint' },
]

const moduleEntries = [
  { name: '前厅运营', sub: 'Front Office', path: '/dashboard/front-office', module: 'front', icon: 'front' },
  { name: '厨房管理', sub: 'Kitchen', path: '/dashboard/kitchen', module: 'kitchen', icon: 'kitchen' },
  { name: '采购仓储', sub: 'Procurement & Storage', path: '/dashboard/supply-chain', module: 'supply', icon: 'supply' },
  { name: '营销会员', sub: 'Marketing', path: '/dashboard/marketing', module: 'marketing', icon: 'marketing' },
  { name: '人事行政', sub: 'HR Admin', path: '/dashboard/hr-admin', module: 'hr', icon: 'hr' },
  { name: '财务数据', sub: 'Finance', path: '/dashboard/finance', module: 'finance', icon: 'finance' },
  { name: '系统设置', sub: 'Settings', path: '/dashboard/settings', module: 'settings', icon: 'settings' },
  { name: '数据大屏', sub: 'Analytics', path: '/dashboard/data-screen', module: 'analytics', icon: 'analytics' },
  { name: '工程管理', sub: 'Engineering', path: '/dashboard/engineering', module: 'engineering', icon: 'engineering' },
]

const moduleLabels = {
  front: { cn: '前厅运营', en: 'Front Office' },
  kitchen: { cn: '厨房管理', en: 'Kitchen' },
  supply: { cn: '采购仓储', en: 'Procurement & Storage' },
  marketing: { cn: '营销会员', en: 'Marketing' },
  hr: { cn: '人事行政', en: 'HR Admin' },
  finance: { cn: '财务数据', en: 'Finance' },
  settings: { cn: '系统设置', en: 'Settings' },
  analytics: { cn: '数据大屏', en: 'Analytics' },
  engineering: { cn: '工程管理', en: 'Engineering' },
}

// 根据当前路由判断所属模块
const activeModule = computed(() => {
  const p = route.path
  if (p.startsWith('/dashboard/finance/')) return 'finance'
  const map = {
    '/dashboard/front-office': 'front', '/dashboard/front-desk': 'front',
    '/dashboard/guest-analysis': 'front', '/dashboard/staff-performance': 'front',
    '/dashboard/table-utilization': 'front', '/dashboard/report-print': 'front',
    '/dashboard/bookings': 'front', '/dashboard/menu': 'front',
    '/dashboard/menu-banquet': 'front', '/dashboard/menu-alacarte': 'front',
    '/dashboard/menu-soldout': 'front', '/dashboard/menu-festive': 'front',
    '/dashboard/menu-full': 'front',
    '/dashboard/customers': 'front', '/dashboard/table-layout': 'front',
    '/dashboard/floor-project': 'front', '/dashboard/art-design': 'front',
    '/dashboard/kitchen': 'kitchen', '/dashboard/production': 'kitchen', '/dashboard/packages': 'kitchen',
    '/dashboard/supply-chain': 'supply', '/dashboard/inventory': 'supply',
    '/dashboard/procurement': 'supply', '/dashboard/suppliers': 'supply',
    '/dashboard/marketing': 'marketing',
    '/dashboard/hr-admin': 'hr', '/dashboard/staff': 'hr', '/dashboard/staff-profile': 'hr',
    '/dashboard/attendance-calendar': 'hr', '/dashboard/attendance-print': 'hr',
    '/dashboard/payroll': 'hr', '/dashboard/hr-analytics': 'hr',
    '/dashboard/self-service': 'hr', '/dashboard/review-queue': 'hr',
    '/dashboard/training': 'hr', '/dashboard/attendance': 'hr', '/dashboard/schedule': 'hr',
    '/dashboard/leave': 'hr', '/dashboard/license': 'hr', '/dashboard/security': 'hr', '/dashboard/assets': 'hr',
    '/dashboard/finance': 'finance', '/dashboard/reports': 'finance', '/dashboard/dish-cost-analysis': 'finance',
    '/dashboard/settings': 'settings', '/dashboard/perm-manager': 'settings', '/dashboard/change-logs': 'settings',
    '/dashboard/data-screen': 'analytics',
    '/dashboard/engineering': 'engineering', '/dashboard/decoration': 'engineering',
    '/dashboard/maintenance': 'engineering', '/dashboard/energy': 'engineering',
    '/dashboard/safety': 'engineering',
  }
  return map[p] || null
})

// 主模块入口路径列表（用于过滤）
const mainModulePaths = moduleEntries.map(e => e.path)

// 拖拽排序状态
const dragIdx = ref(-1)
const dragOverIdx = ref(-1)
const STORAGE_KEY = 'sidebar_menu_order'

// 从 localStorage 读取自定义排序
function loadMenuOrder() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : null
  } catch { return null }
}

// 保存排序到 localStorage
function saveMenuOrder(orderedMenu) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(orderedMenu.map(i => i.path)))
  } catch {}
}

// 侧边栏菜单：核心 + 当前模块的子页面（排除主模块入口）
// 使用 ref 而非 computed，避免拖拽时索引变化
const sidebarMenu = ref([])

function updateSidebarMenu() {
  const mod = activeModule.value
  const base = mod
    ? [...coreMenu, ...allModulePages.filter(p => p.module === mod && !mainModulePaths.includes(p.path))]
    : [...coreMenu, ...moduleEntries]

  // 尝试应用自定义排序
  const order = loadMenuOrder()
  if (!order || order.length === 0) {
    sidebarMenu.value = base
    return
  }

  // 按保存的顺序排列，未保存的追加到末尾
  const orderMap = new Map(order.map((p, i) => [p, i]))
  const sorted = [...base].sort((a, b) => {
    const ai = orderMap.has(a.path) ? orderMap.get(a.path) : 9999
    const bi = orderMap.has(b.path) ? orderMap.get(b.path) : 9999
    return ai - bi
  })
  sidebarMenu.value = sorted
}

// 监听路由变化更新菜单
watch(() => activeModule.value, updateSidebarMenu, { immediate: true })

// 拖拽处理
const onDragStart = (idx, e) => {
  dragIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(idx))
}

const onDragOver = (idx, e) => {
  if (dragIdx.value === idx) return
  dragOverIdx.value = idx
  // 必须设置 dropEffect 才能触发 drop 事件
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
}

const onDragLeave = () => {
  dragOverIdx.value = -1
}

const onDrop = (toIdx) => {
  const fromIdx = dragIdx.value
  if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return

  // 工作台、桌台看板、菜单菜牌不可移动（前3项）
  if (fromIdx < 3 || toIdx < 3) return

  const menu = [...sidebarMenu.value]
  const [moved] = menu.splice(fromIdx, 1)
  menu.splice(toIdx, 0, moved)
  saveMenuOrder(menu)
  sidebarMenu.value = menu
  dragOverIdx.value = -1
}

const onDragEnd = () => {
  dragIdx.value = -1
  dragOverIdx.value = -1
}

const goTo = (path) => {
  router.push(path)
}

const isActive = (path) => {
  return route.path.startsWith(path)
}

const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const iconMap = {
  home: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  table: '<rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/>',
  // 前厅运营 - 门店/前台
  front: '<rect x="2" y="7" width="20" height="15" rx="2"/><polyline points="17 2 12 7 7 2"/>',
  booking: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/>',
  // 客人分析 - 单人带放大镜
  guest: '<circle cx="11" cy="8" r="4"/><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="17" cy="5" r="3"/><line x1="20" y1="8" x2="22" y2="10"/>',
  // 员工绩效 - 奖杯
  staff: '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>',
  // 桌台利用率 - 饼图
  tableUtil: '<path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>',
  print: '<polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>',
  // 预订管理 - 文件带笔（区别于booking的日历）
  bookings: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><path d="M12 18l4-4 4 4"/><path d="M16 14v4h-4"/>',
  menu: '<line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>',
  // 客户管理 - 通讯录
  customer: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22 6 12 13 2 6"/>',
  layout: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>',
  art: '<circle cx="13.5" cy="6.5" r="2.5"/><path d="M17 2l-5.5 5.5"/><path d="M12 7L2 17l3 3 10-10z"/><path d="M17 2l3 3-2 2-3-3z"/>',
  kitchen: '<path d="M6 13.87A4 4 0 0 1 7.41 6a5.11 5.11 0 0 1 1.05-1.54 5 5 0 0 1 7.08 0A5.11 5.11 0 0 1 16.59 6 4 4 0 0 1 18 13.87V21H6z"/><line x1="6" y1="17" x2="18" y2="17"/>',
  production: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>',
  package: '<line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
  // 采购仓储 - 仓库
  supply: '<path d="M22 8.35V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35A2 2 0 0 1 3.26 6.5l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35Z"/><path d="M6 18h12"/><path d="M6 14h12"/><rect x="6" y="10" width="12" height="12"/>',
  // 库存管理 - 箱子带层
  inventory: '<path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/><line x1="7" y1="14" x2="17" y2="14"/>',
  // 采购管理 - 购物车
  procurement: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>',
  // 供应商 - 握手/合作
  supplier: '<path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z"/>',
  // 营销会员 - 喇叭
  marketing: '<path d="m3 11 18-5v12L3 14v-3z"/><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6"/>',
  // 人事行政 - 文件夹带人
  hr: '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="2"/>',
  // 员工档案 - 身份证/卡片
  staffFile: '<rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/><circle cx="7" cy="14" r="2"/><line x1="12" y1="13" x2="18" y2="13"/><line x1="12" y1="16" x2="16" y2="16"/>',
  training: '<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>',
  attendance: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><polyline points="9 16 11 18 15 14"/>',
  schedule: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  leave: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="10" y1="14" x2="14" y2="18"/><line x1="14" y1="14" x2="10" y2="18"/>',
  license: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="M9 12l2 2 4-4"/>',
  security: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>',
  assets: '<rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>',
  // 财务数据 - 钱包
  finance: '<path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"/><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"/><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"/>',
  // 菜品成本 - 标签/价格
  cost: '<path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/>',
  // 成本分析 - 柱状图+放大镜（区别于production的折线）
  analysis: '<rect x="3" y="12" width="4" height="9"/><rect x="10" y="8" width="4" height="13"/><rect x="17" y="4" width="4" height="17"/><circle cx="19" cy="5" r="3"/><line x1="21" y1="7" x2="23" y2="9"/>',
  // 数据报表 - 带图表的文档
  report: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>',
  // 菜品成本分析 - 盘子带图表
  dishAnalysis: '<path d="M12 2a10 10 0 1 0 10 10H12V2z"/><path d="M12 12 2.1 12a10.1 10.1 0 0 0 9.9 9.9V12z"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  permission: '<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
  // 改动日志 - 时钟历史
  changelog: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/><path d="M16 16l2 2 4-4"/>',
  // 数据大屏 - 大屏/显示器
  analytics: '<rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/><polyline points="6 10 9 7 12 10 15 7 18 10"/>',
  // 工程管理 - 建筑/楼房
  engineering: '<rect x="4" y="2" width="16" height="20" rx="2"/><path d="M9 22v-4h6v4"/><path d="M8 6h.01M16 6h.01M12 6h.01M12 10h.01M12 14h.01M16 10h.01M16 14h.01M8 10h.01M8 14h.01"/>',
  decoration: '<path d="M2 22h20"/><path d="M12 2v4"/><path d="M12 18v4"/><path d="M4.93 4.93l2.83 2.83"/><path d="M16.24 16.24l2.83 2.83"/><path d="M2 12h4"/><path d="M18 12h4"/><path d="M4.93 19.07l2.83-2.83"/><path d="M16.24 7.76l2.83-2.83"/>',
  // 设备维护 - 齿轮
  maintenance: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9c.604.26.997.852 1 1.51V11h.09a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  energy: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
  // 安全管理 - 盾牌带叉（区别于license的盾牌带勾）
  safety: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><line x1="9" y1="9" x2="15" y2="15"/><line x1="15" y1="9" x2="9" y2="15"/>',
  // 工程维护(楼面) - 楼层平面
  floorMaint: '<rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/><path d="M14 14h4M14 17h4"/>',
}

function iconSvg(name) {
  const paths = iconMap[name] || iconMap['home']
  return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`
}

const showLogoutModal = ref(false)

const handleCommand = (command) => {
  if (command === 'home') {
    router.push('/dashboard/home')
  } else if (command === 'logout') {
    showLogoutModal.value = true
  }
}

const refreshPage = () => {
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      t: Date.now()
    }
  })
  router.go(0)
}

const cancelLogout = () => {
  showLogoutModal.value = false
}

const confirmLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('退出成功')
  showLogoutModal.value = false
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
}

.main-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: 108px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  padding: 0 32px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.header-gold-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #C4A35A 30%, #F5D98C 50%, #C4A35A 70%, transparent 100%);
  overflow: hidden;
}

.header-gold-shimmer {
  position: absolute;
  top: 0;
  left: -120px;
  width: 120px;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(245,217,140,0.9), transparent);
  animation: shimmerMove 3s ease-in-out infinite;
}

@keyframes shimmerMove {
  0% {
    left: -120px;
  }
  100% {
    left: 100%;
  }
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: flex-start;
}

.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.header-right {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: flex-end;
  gap: 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
}

.logo-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.logo-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.logo-main-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-main {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 2px;
  font-family: var(--font-family);
}

.logo-divider {
  color: var(--color-accent);
  font-size: 18px;
  font-weight: 700;
}

.logo-sub-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-accent);
  letter-spacing: 1px;
}

.logo-eng-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.logo-eng {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 1.5px;
  font-family: var(--font-family-sans);
}

.logo-divider-light {
  color: var(--color-border);
  font-size: 10px;
}

.logo-eng-sub {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 1.5px;
  font-family: var(--font-family-sans);
}

.store-badge {
  padding: 5px 14px;
  background: rgba(45, 74, 62, 0.06);
  color: var(--color-primary);
  border-radius: 2px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(45, 74, 62, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.refresh-btn {
  width: 38px;
  height: 38px;
  background: transparent;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.25s;
}

.refresh-btn:hover {
  background: rgba(45, 74, 62, 0.06);
  color: var(--color-primary);
  transform: rotate(180deg);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.user-role {
  font-size: 11px;
  color: var(--color-text-muted);
}

.avatar {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #2D4A3E, #3D5A4E);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.sidebar {
  width: 210px;
  background: linear-gradient(180deg, #2D4A3E 0%, #1D3A2E 100%);
  border-right: none;
  overflow-y: auto;
  position: relative;
  box-shadow: 2px 0 20px rgba(0, 0, 0, 0.1);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  padding: 24px 20px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(196, 163, 90, 0.15);
}

.sidebar-logo-icon {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-logo-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.nav-menu {
  padding: 28px 0 16px 0;
  flex: 1;
}

.sidebar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #C4A35A, #D4B36A, #C4A35A, transparent);
}

.nav-menu {
  padding: 16px 0;
}

.nav-group {
  margin-bottom: 6px;
}

.nav-item-home {
  color: rgba(196, 163, 90, 0.9);
  background: rgba(196, 163, 90, 0.06);
}

.nav-item-home:hover {
  color: #E8D5A0;
  background: rgba(196, 163, 90, 0.12);
}

.nav-item-home .nav-sub {
  color: rgba(196, 163, 90, 0.5);
}

.nav-item-home:hover .nav-sub {
  color: rgba(196, 163, 90, 0.7);
}

.nav-item-home.active {
  color: #E8D5A0;
  background: linear-gradient(135deg, rgba(196, 163, 90, 0.35) 0%, rgba(196, 163, 90, 0.15) 100%);
  border: 1px solid rgba(196, 163, 90, 0.6);
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.15),
    0 2px 8px rgba(196, 163, 90, 0.25);
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.nav-item-home.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 36px;
  background: linear-gradient(180deg, #F5D98C 0%, #C4A35A 40%, #A4833A 100%);
  border-radius: 0 3px 3px 0;
  box-shadow: 
    2px 0 12px rgba(196, 163, 90, 0.8),
    0 0 6px rgba(245, 217, 140, 0.6);
}

/* 模块入口样式 - 工作台状态下显示在侧边栏下方 */
.nav-item-module {
  color: rgba(250, 248, 245, 0.65);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  margin: 3px 12px;
  padding: 10px 20px;
}

.nav-item-module:hover {
  color: rgba(250, 248, 245, 0.9);
  background: rgba(196, 163, 90, 0.12);
  border-color: rgba(196, 163, 90, 0.25);
  transform: translateX(2px);
}

.nav-item-module.active {
  color: #FFE8A8;
  background: linear-gradient(135deg, rgba(196, 163, 90, 0.3) 0%, rgba(196, 163, 90, 0.12) 100%);
  border-color: rgba(196, 163, 90, 0.5);
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    0 2px 6px rgba(196, 163, 90, 0.25);
  font-weight: 600;
}

.nav-item-module.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 32px;
  background: linear-gradient(180deg, #F5D98C 0%, #C4A35A 40%, #A4833A 100%);
  border-radius: 0 3px 3px 0;
  box-shadow: 
    2px 0 10px rgba(196, 163, 90, 0.7),
    0 0 5px rgba(245, 217, 140, 0.5);
}

.nav-item-module .nav-text {
  font-size: 13px;
  font-weight: 500;
}

.nav-item-module .nav-sub {
  font-size: 11px;
  color: rgba(250, 248, 245, 0.45);
}

.nav-item-module:hover .nav-sub {
  color: rgba(196, 163, 90, 0.6);
}

.nav-item-module.active .nav-sub {
  color: rgba(196, 163, 90, 0.75);
}

.nav-group-title {
  padding: 14px 24px 8px;
  font-size: 10px;
  font-weight: 600;
  color: rgba(196, 163, 90, 0.6);
  letter-spacing: 2px;
  text-transform: uppercase;
  font-family: var(--font-family);
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 24px 12px;
  font-size: 12px;
  color: #C4A35A;
  cursor: pointer;
  border-bottom: 1px solid rgba(196, 163, 90, 0.12);
  margin-bottom: 4px;
  transition: all 0.2s;
  letter-spacing: 1px;
}

.module-header-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  line-height: 1.5;
  width: 100%;
}

.module-header-cn {
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-family);
  text-align: center;
}

.module-header-en {
  font-size: 11px;
  font-family: var(--font-family);
  opacity: 0.6;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  text-align: center;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.module-header:hover {
  color: #E8D5A0;
  background: rgba(196, 163, 90, 0.08);
}

.sidebar.collapsed .module-header {
  display: none;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 12px 24px;
  margin: 2px 12px;
  color: rgba(250, 248, 245, 0.7);
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border-radius: 8px;
  cursor: grab;
  user-select: none;
}

.nav-item:active {
  cursor: grabbing;
}

.nav-item.dragging {
  opacity: 0.4;
  transform: scale(0.95);
}

.nav-item.drag-over {
  border-top: 2px solid #C4A35A;
}

.drag-handle {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
  color: rgba(250, 248, 245, 0.4);
}

.nav-item:hover .drag-handle {
  opacity: 1;
}

.nav-item:hover {
  background: rgba(196, 163, 90, 0.1);
  color: #FAF8F5;
  transform: translateX(4px);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(196, 163, 90, 0.45) 0%, rgba(196, 163, 90, 0.2) 100%);
  color: #FFE8A8;
  border: 1px solid rgba(196, 163, 90, 0.6);
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.25),
    0 2px 8px rgba(196, 163, 90, 0.3);
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 36px;
  background: linear-gradient(180deg, #F5D98C 0%, #C4A35A 40%, #A4833A 100%);
  border-radius: 0 3px 3px 0;
  box-shadow: 
    2px 0 12px rgba(196, 163, 90, 0.8),
    0 0 6px rgba(245, 217, 140, 0.6);
}

.nav-icon {
  display: none;
}

.nav-text {
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-family);
  letter-spacing: 0.5px;
}

.nav-sub {
  font-size: 11px;
  color: rgba(250, 248, 245, 0.45);
  margin-top: 2px;
  letter-spacing: 0.5px;
  font-family: var(--font-family-sans);
  font-weight: 400;
}

.nav-item:hover .nav-sub {
  color: rgba(250, 248, 245, 0.6);
}

.nav-item.active .nav-sub {
  color: rgba(196, 163, 90, 0.8);
}

.nav-indicator {
  display: none;
}

.nav-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(196, 163, 90, 0.2), transparent);
  margin: 12px 20px;
}

.nav-divider.gold {
  height: 2px;
  background: linear-gradient(90deg, transparent, #C4A35A 20%, #D4B36A 50%, #C4A35A 80%, transparent);
  margin: 8px 16px;
  box-shadow: 0 0 8px rgba(196, 163, 90, 0.4);
  position: relative;
}

.nav-divider.gold::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  background: #D4B36A;
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(212, 179, 106, 0.8);
}

.sidebar-toggle-hint {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: rgba(196, 163, 90, 0.4);
  letter-spacing: 1px;
  pointer-events: none;
}

.sidebar.collapsed {
  width: 60px;
  cursor: pointer;
}

.sidebar.collapsed .nav-group-title {
  display: none;
}

.sidebar.collapsed .nav-item {
  padding: 12px 0;
  margin: 2px 8px;
  text-align: center;
  align-items: center;
  justify-content: center;
}

.sidebar.collapsed .nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: rgba(250, 248, 245, 0.7);
}

.sidebar.collapsed .nav-icon svg {
  width: 20px;
  height: 20px;
}

.sidebar.collapsed .nav-item.active .nav-icon {
  color: #FFE8A8;
}

.sidebar.collapsed .nav-text {
  display: none;
}

.sidebar.collapsed .nav-sub {
  display: none;
}

.sidebar.collapsed .drag-handle {
  display: none;
}

.sidebar.collapsed .nav-item.active::before {
  display: none;
}

.sidebar.collapsed .sidebar-toggle-hint {
  display: none;
}

.sidebar.collapsed::after {
  content: '»';
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(196, 163, 90, 0.5);
  font-size: 16px;
  pointer-events: none;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: var(--color-card);
  border-radius: var(--radius-xl);
  padding: 32px;
  width: 400px;
  max-width: 90vw;
  text-align: center;
  box-shadow: var(--shadow-xl);
}

.modal-icon {
  width: 64px;
  height: 64px;
  background: rgba(194, 85, 85, 0.08);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: var(--color-danger);
}

.modal-icon svg {
  width: 32px;
  height: 32px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.modal-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.modal-btn {
  padding: 10px 24px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
  border: none;
}

.btn-cancel {
  background: var(--color-bg-alt);
  color: var(--color-text-secondary);
}

.btn-cancel:hover {
  background: var(--color-border);
}

.btn-confirm {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
}

.btn-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(45, 74, 62, 0.3);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-dialog,
.modal-leave-to .modal-dialog {
  transform: scale(0.9);
}

@media (max-width: 768px) {
  .sidebar {
    width: 60px;
  }
  .nav-group-title {
    display: none;
  }
  .nav-sub {
    display: none;
  }
  .nav-item {
    padding: 12px;
    align-items: center;
  }
  .main-content {
    padding: 12px;
  }
  .header {
    padding: 0 16px;
  }
  .logo-sub {
    display: none;
  }
}
</style>
