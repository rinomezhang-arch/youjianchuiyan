<template>
  <div class="dashboard-home">
    <!-- 顶部标题 -->
    <div class="top-bar">
      <div class="header-left">
        <h1 class="page-title">总经理总驾驶舱</h1>
        <p class="page-subtitle">General Manager Dashboard · {{ currentDate }}</p>
      </div>
      <div class="header-right">
        <div class="store-selector">
          <span class="selector-label">门店</span>
          <select v-model="selectedStore" class="selector-dropdown" @change="loadDashboard">
            <option value="all">全部门店</option>
            <option v-for="s in storeOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 经营指标卡片 -->
    <div class="stats-section">
      <h2 class="section-title">经营指标</h2>
      <div v-if="loadError" class="load-error">
        数据加载失败：{{ loadError }}，请稍后重试或联系管理员。
      </div>
      <div class="stats-grid">
        <div class="stat-card revenue" @click="goTo('finance')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日总营收</div>
            <div class="stat-value">¥{{ formatNumber(kpi.todayRevenue) }}</div>
            <div class="stat-trend" :class="kpi.revenueTrendPct > 0 ? 'up' : (kpi.revenueTrendPct < 0 ? 'down' : 'flat')">
              {{ kpi.revenueTrendPct > 0 ? '+' : '' }}{{ kpi.revenueTrendPct.toFixed(1) }}%
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-detail">
            <div class="detail-item" v-for="(amt, sid) in kpi.revenueByStore" :key="sid">
              <span>门店 #{{ sid }}</span>
              <span>¥{{ formatNumber(amt) }}</span>
            </div>
            <div v-if="Object.keys(kpi.revenueByStore || {}).length === 0" class="detail-item">
              <span>暂无数据</span>
              <span>¥0</span>
            </div>
          </div>
        </div>

        <div class="stat-card traffic" @click="goTo('guest-analysis')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日客流</div>
            <div class="stat-value">{{ kpi.todayTraffic || 0 }}</div>
            <div class="stat-trend" :class="kpi.trafficTrendPct > 0 ? 'up' : (kpi.trafficTrendPct < 0 ? 'down' : 'flat')">
              {{ kpi.trafficTrendPct > 0 ? '+' : '' }}{{ kpi.trafficTrendPct.toFixed(1) }}%
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-detail">
            <div class="detail-item">
              <span>较昨日</span>
              <span>{{ kpi.trafficTrendPct > 0 ? '+' : '' }}{{ kpi.trafficTrendPct.toFixed(1) }}%</span>
            </div>
            <div class="detail-item">
              <span>数据来源</span>
              <span>booking_master</span>
            </div>
          </div>
        </div>

        <div class="stat-card turnover" @click="goTo('table-utilization')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">翻台率</div>
            <div class="stat-value">{{ (kpi.turnoverRate || 0).toFixed(1) }}%</div>
            <div class="stat-trend flat">实时数据</div>
          </div>
          <div class="stat-gauge">
            <svg viewBox="0 0 100 60" class="gauge-svg">
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" stroke="#e8edea" stroke-width="8"/>
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" :stroke="(kpi.turnoverRate || 0) > 80 ? '#67C23A' : (kpi.turnoverRate || 0) > 60 ? '#E6A23C' : '#F56C6C'" stroke-width="8" :stroke-dasharray="`${Math.min((kpi.turnoverRate || 0) * 2.51, 251)} 251`"/>
            </svg>
          </div>
        </div>

        <div class="stat-card margin" @click="goTo('finance/cost-analysis')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">综合毛利率</div>
            <div class="stat-value">{{ (kpi.grossMarginRate || 0).toFixed(1) }}%</div>
            <div class="stat-trend flat">真实聚合</div>
          </div>
          <div class="stat-pie">
            <svg viewBox="0 0 100 100" class="pie-svg">
              <circle cx="50" cy="50" r="40" fill="none" stroke="#E8EDEB" stroke-width="20"/>
              <circle cx="50" cy="50" r="40" fill="none" stroke="#2D4A3E" stroke-width="20" :stroke-dasharray="`${Math.min((kpi.grossMarginRate || 0) * 2.51, 251)} 251`" transform="rotate(-90 50 50)"/>
            </svg>
            <div class="pie-center">{{ (kpi.grossMarginRate || 0).toFixed(1) }}%</div>
          </div>
        </div>

        <div class="stat-card profit" @click="goTo('finance')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">预估当日净利</div>
            <div class="stat-value">¥{{ formatNumber(kpi.netProfitEstimate) }}</div>
            <div class="stat-trend flat">仅扣食材成本</div>
          </div>
          <div class="stat-bar-chart">
            <div class="bar-item">
              <div class="bar-label">食材成本</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: (kpi.costBreakdown?.food || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.food || 0).toFixed(1) }}%</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">人工成本</div>
              <div class="bar-track">
                <div class="bar-fill labor" :style="{ width: (kpi.costBreakdown?.labor || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.labor || 0) > 0 ? (kpi.costBreakdown.labor).toFixed(1) + '%' : '无数据' }}</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">能耗费用</div>
              <div class="bar-track">
                <div class="bar-fill energy" :style="{ width: (kpi.costBreakdown?.energy || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.energy || 0) > 0 ? (kpi.costBreakdown.energy).toFixed(1) + '%' : '无数据' }}</div>
            </div>
          </div>
        </div>

        <div class="stat-card orders" @click="goTo('bookings')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日订单数</div>
            <div class="stat-value">{{ kpi.orderCount || 0 }}</div>
            <div class="stat-trend flat">已确认 confirmed</div>
          </div>
          <div class="stat-channel">
            <div class="channel-item" v-for="(cnt, ch) in kpi.orderByChannel" :key="ch">
              <span class="channel-icon" :class="channelClass(ch)">{{ channelLabel(ch) }}</span>
              <span class="channel-count">{{ cnt }}</span>
            </div>
            <div v-if="Object.keys(kpi.orderByChannel || {}).length === 0" class="channel-item">
              <span class="channel-icon">暂无</span>
              <span class="channel-count">0</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能导航 -->
    <div class="section-header">
      <h3 class="section-title">功能导航</h3>
    </div>
    <div class="nav-grid">
      <div v-for="item in navItems" :key="item.path" class="nav-card" @click="goTo(item.path)">
        <div class="nav-card-icon" :style="{ background: item.color }" v-html="item.icon"></div>
        <div class="nav-card-body">
          <div class="nav-card-name">{{ item.name }}</div>
          <div class="nav-card-desc">{{ item.desc }}</div>
        </div>
        <span class="nav-card-arrow">›</span>
      </div>
    </div>

    <!-- 第二行：预定看板 + 待办审批 -->
    <div class="mid-section">
      <!-- 预定看板 -->
      <div class="booking-section">
        <div class="section-header">
          <h2 class="section-title">预定看板</h2>
          <button class="section-action" @click="goTo('bookings')">查看全部</button>
        </div>
        <div class="booking-grid">
          <div class="booking-card today">
            <div class="booking-header">
              <span class="booking-label">今日包厢预定</span>
              <span class="booking-count">{{ kpi.todayBoxBookings || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.todayBoxList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-time">{{ item.time }}</span>
                </div>
                <span class="booking-name">{{ item.name }}</span>
              </div>
              <div v-if="!kpi.todayBoxList || kpi.todayBoxList.length === 0" class="empty-row">今日暂无包厢预定</div>
            </div>
          </div>

          <div class="booking-card banquet">
            <div class="booking-header">
              <span class="booking-label">宴席预定</span>
              <span class="booking-count">{{ kpi.todayBanquetBookings || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.todayBanquetList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-date">{{ item.date }}</span>
                </div>
                <span class="booking-guests">{{ item.guests }}人</span>
              </div>
              <div v-if="!kpi.todayBanquetList || kpi.todayBanquetList.length === 0" class="empty-row">今日暂无宴席</div>
            </div>
          </div>

          <div class="booking-card alert">
            <div class="booking-header">
              <span class="booking-label">空包厢预警</span>
              <span class="booking-count warning">{{ kpi.emptyBoxWarningCount || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.emptyBoxList" :key="item.box">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-status">{{ item.status }}</span>
                </div>
              </div>
              <div v-if="!kpi.emptyBoxList || kpi.emptyBoxList.length === 0" class="empty-row">暂无空置包厢</div>
            </div>
          </div>

          <div class="booking-card tomorrow">
            <div class="booking-header">
              <span class="booking-label">明日预定</span>
              <span class="booking-count">{{ kpi.tomorrowTotal || 0 }}</span>
            </div>
            <div class="booking-chart">
              <div class="chart-item">
                <span class="chart-label">午市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar" :style="{ width: tomorrowPct(kpi.tomorrowLunch, kpi.tomorrowTotal) + '%' }"></div>
                </div>
                <span class="chart-value">{{ kpi.tomorrowLunch || 0 }}</span>
              </div>
              <div class="chart-item">
                <span class="chart-label">晚市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar evening" :style="{ width: tomorrowPct(kpi.tomorrowDinner, kpi.tomorrowTotal) + '%' }"></div>
                </div>
                <span class="chart-value">{{ kpi.tomorrowDinner || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 待办审批 -->
      <div class="approval-section">
        <div class="section-header">
          <h2 class="section-title">待办审批</h2>
          <button class="section-action" @click="goTo('approval-center')">审批中心</button>
        </div>
        <div class="approval-tabs">
          <button
            v-for="tab in approvalTabs"
            :key="tab.key"
            :class="['tab-btn', { active: activeApprovalTab === tab.key }]"
            @click="activeApprovalTab = tab.key"
          >
            <span class="tab-text">{{ tab.name }}</span>
            <span class="tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
          </button>
        </div>
        <div class="approval-list">
          <div v-for="item in filteredApprovals" :key="item.id" class="approval-item" @click="goTo('approval-center')">
            <div class="approval-icon" :class="approvalTypeClass(item.flowType)">{{ approvalTypeLabel(item.flowType) }}</div>
            <div class="approval-content">
              <div class="approval-title">{{ item.title || item.flowNo }}</div>
              <div class="approval-meta">
                <span>{{ item.applicant || '申请人' }}</span>
                <span>{{ item.time }}</span>
              </div>
            </div>
            <div class="approval-status pending">待审批</div>
          </div>
          <div v-if="filteredApprovals.length === 0" class="empty-state">
            <span>暂无待审批事项</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三行：风险预警 + 快捷跳转 -->
    <div class="bottom-section">
      <div class="warning-section">
        <div class="section-header">
          <h2 class="section-title">风险预警</h2>
        </div>
        <div class="warning-grid">
          <div class="warning-card" v-for="item in riskWarnings" :key="item.type" :class="item.level">
            <div class="warning-content">
              <div class="warning-title">{{ item.title }}</div>
              <div class="warning-desc">{{ item.desc }}</div>
              <div class="warning-count">{{ item.count }}项待处理</div>
            </div>
            <button class="warning-action" @click="goTo(item.link)">处理</button>
          </div>
          <div v-if="riskWarnings.length === 0" class="empty-row full">暂无风险预警</div>
        </div>
      </div>

      <div class="quick-nav-section">
        <div class="section-header">
          <h2 class="section-title">业务看板</h2>
        </div>
        <div class="quick-nav-grid">
          <div
            v-for="nav in quickNav"
            :key="nav.path"
            class="nav-card"
            :style="{ '--card-color': nav.color }"
            @click="goTo(nav.path)"
          >
            <div class="nav-text">{{ nav.name }}</div>
            <div class="nav-badge" v-if="(kpi.navBadges?.[nav.key] || 0) > 0">{{ kpi.navBadges[nav.key] }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <div class="action-buttons">
        <button class="action-btn" @click="goTo('kitchen')">呼叫后厨</button>
        <button class="action-btn" @click="goTo('front-office')">保洁呼叫</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

// 当前日期
const currentDate = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

// 门店选择
const selectedStore = ref('all')
const storeOptions = ref([
  { value: '1', label: '旗舰店' },
  { value: '2', label: '分店A' },
  { value: '3', label: '分店B' }
])

// 加载状态
const loading = ref(false)
const loadError = ref('')

// KPI 数据（从 API 获取）
const kpi = ref({
  todayRevenue: 0,
  revenueTrendPct: 0,
  revenueByStore: {},
  todayTraffic: 0,
  trafficTrendPct: 0,
  turnoverRate: 0,
  grossMarginRate: 0,
  netProfitEstimate: 0,
  costBreakdown: { food: 0, labor: 0, energy: 0 },
  orderCount: 0,
  orderByChannel: {},
  todayBoxBookings: 0,
  todayBoxList: [],
  todayBanquetBookings: 0,
  todayBanquetList: [],
  emptyBoxWarningCount: 0,
  emptyBoxList: [],
  tomorrowTotal: 0,
  tomorrowLunch: 0,
  tomorrowDinner: 0,
  navBadges: {}
})

// 风险预警（从 API 获取）
const riskWarnings = ref([])

// 审批数据
const approvalList = ref([])
const activeApprovalTab = ref('all')
const approvalTabs = ref([
  { key: 'all', name: '全部', count: 0 },
  { key: 'purchase', name: '采购', count: 0 },
  { key: 'expense', name: '报销', count: 0 },
  { key: 'leave', name: '请假', count: 0 }
])

const filteredApprovals = computed(() => {
  if (activeApprovalTab.value === 'all') return approvalList.value
  return approvalList.value.filter(item => item.flowType === activeApprovalTab.value)
})

// 导航项
const navItems = [
  { name: '前厅运营', path: 'front-office', color: '#2D4A3E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="6" width="16" height="12" rx="2"/><polyline points="14 2 10 6 6 2"/></svg>', desc: '客户接待 · 桌台服务 · 收银结算' },
  { name: '菜单管理', path: 'menu-manager', color: '#4A7C59', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><line x1="6" y1="5" x2="18" y2="5"/><line x1="6" y1="10" x2="18" y2="10"/><line x1="6" y1="15" x2="18" y2="15"/><line x1="2" y1="5" x2="2.01" y2="5"/><line x1="2" y1="10" x2="2.01" y2="10"/><line x1="2" y1="15" x2="2.01" y2="15"/></svg>', desc: '菜品库 · 成本卡 · 套餐组合' },
  { name: '财务中心', path: 'finance', color: '#8B5E3C', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="10" cy="10" r="7"/><path d="M10 6v8m-2-5h4a2 2 0 0 1 0 4H8"/></svg>', desc: '营收报表 · 成本分析 · 利润核算' },
  { name: '营销会员', path: 'marketing', color: '#C4A35A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="8" cy="6" r="3"/><path d="M14 12v-2a3 3 0 0 0-3-3H8a3 3 0 0 0-3 3v2"/><path d="M18 17v-2a3 3 0 0 0-3-3h-2"/></svg>', desc: '会员管理 · 营销活动 · 积分商城' },
  { name: '人事行政', path: 'hr-admin', color: '#6B8C9E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="2" width="16" height="6" rx="1"/><rect x="2" y="12" width="16" height="6" rx="1"/></svg>', desc: '员工管理 · 考勤排班 · 薪资福利' },
  { name: '库存采购', path: 'inventory', color: '#9B6B4A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="3" y="7" width="14" height="10" rx="1"/><path d="M6 7V5a4 4 0 0 1 8 0v2"/></svg>', desc: '库存监控 · 采购申请 · 供应商管理' }
]

// 快捷导航
const quickNav = [
  { name: '营收报表', path: 'finance/revenue', key: 'revenue', color: '#2D4A3E' },
  { name: '客户分析', path: 'guest-analysis', key: 'guest', color: '#4A7C59' },
  { name: '桌台利用率', path: 'table-utilization', key: 'table', color: '#8B5E3C' },
  { name: '库存预警', path: 'inventory/alerts', key: 'inventory', color: '#C4A35A' },
  { name: '员工考勤', path: 'hr-admin/attendance', key: 'attendance', color: '#6B8C9E' },
  { name: '审批中心', path: 'approval-center', key: 'approval', color: '#9B6B4A' }
]

// ===== API 函数 =====

// 工作台全部指标由后端单一聚合接口返回，避免页面调用不存在的拆分接口。
async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request({
      url: '/dashboard/today',
      method: 'get',
      params: { storeId: selectedStore.value }
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '工作台数据加载失败')
    }

    const data = res.data
    kpi.value = { ...kpi.value, ...data }
    riskWarnings.value = data.riskWarnings || data.warnings || []
    approvalList.value = data.pendingApprovalList || data.approvalList || []
    const approvalCounts = data.approvalByType || data.approvalCounts || {}
    approvalTabs.value = approvalTabs.value.map(tab => ({
      ...tab,
      count: tab.key === 'all'
        ? approvalList.value.length
        : (approvalCounts[tab.key] || approvalList.value.filter(item => item.flowType === tab.key).length)
    }))
  } catch (err) {
    loadError.value = err.message || '网络请求失败'
  } finally {
    loading.value = false
  }
}

// ===== 工具函数 =====

function formatNumber(val) {
  if (!val && val !== 0) return '0'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

function goTo(path) {
  router.push({ name: path })
}

function channelClass(ch) {
  const map = { wechat: 'wechat', phone: 'phone', walkin: 'walkin', meituan: 'meituan' }
  return map[ch] || 'default'
}

function channelLabel(ch) {
  const map = { wechat: '微信', phone: '电话', walkin: '到店', meituan: '美团', douyin: '抖音' }
  return map[ch] || ch
}

function tomorrowPct(val, total) {
  if (!total || total === 0) return 0
  return Math.round((val / total) * 100)
}

function approvalTypeClass(type) {
  const map = { purchase: 'purchase', expense: 'expense', leave: 'leave', contract: 'contract' }
  return map[type] || 'default'
}

function approvalTypeLabel(type) {
  const map = { purchase: '采购', expense: '报销', leave: '请假', contract: '合同' }
  return map[type] || '其他'
}

// ===== 生命周期 =====
onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard-home {
  padding: 24px 28px;
  background: #f5f6f4;
  min-height: 100vh;
}

/* Top bar */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}
.header-left { flex: 1; }
.page-title { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0 0 2px 0; }
.page-subtitle { font-size: 12px; color: #8a8a8a; margin: 0; }
.store-selector { display: flex; align-items: center; gap: 8px; }
.selector-label { font-size: 13px; color: #666; }
.selector-dropdown {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  background: #fff;
  cursor: pointer;
}

/* Stats section */
.stats-section { margin-bottom: 32px; }
.section-title { font-size: 15px; font-weight: 700; color: #1a1a1a; margin-bottom: 16px; }
.load-error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 14px;
  padding: 20px 22px;
  cursor: pointer;
  transition: all 0.25s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #ccc;
}
.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(45, 74, 62, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.stat-icon-wrap svg {
  width: 22px;
  height: 22px;
  stroke: #2D4A3E;
}
.stat-info { margin-bottom: 12px; }
.stat-label { font-size: 13px; color: #888; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #2D4A3E; line-height: 1.2; }
.stat-trend {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  margin-top: 4px;
}
.stat-trend.up { color: #67C23A; background: rgba(103, 194, 58, 0.1); }
.stat-trend.down { color: #F56C6C; background: rgba(245, 108, 108, 0.1); }
.stat-trend.flat { color: #909399; background: rgba(144, 147, 153, 0.1); }
.stat-divider { height: 1px; background: #f0f0f0; margin: 12px 0; }
.stat-detail { font-size: 12px; color: #666; }
.detail-item { display: flex; justify-content: space-between; padding: 4px 0; }

/* Gauge */
.stat-gauge { margin-top: 8px; }
.gauge-svg { width: 100%; max-width: 120px; }

/* Pie */
.stat-pie { position: relative; width: 60px; height: 60px; margin-top: 8px; }
.pie-svg { width: 100%; height: 100%; }
.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 700;
  color: #2D4A3E;
}

/* Bar chart */
.stat-bar-chart { margin-top: 12px; }
.bar-item { margin-bottom: 8px; }
.bar-label { font-size: 11px; color: #888; margin-bottom: 2px; }
.bar-track { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; background: #2D4A3E; border-radius: 3px; transition: width 0.3s; }
.bar-fill.labor { background: #E6A23C; }
.bar-fill.energy { background: #409EFF; }
.bar-value { font-size: 11px; color: #666; margin-top: 2px; }

/* Channel */
.stat-channel { margin-top: 12px; }
.channel-item { display: flex; align-items: center; justify-content: space-between; padding: 3px 0; font-size: 12px; }
.channel-icon { color: #888; }
.channel-count { font-weight: 600; color: #333; }

/* Nav grid */
.nav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}
.nav-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.nav-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #ccc;
}
.nav-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.nav-card-icon :deep(svg) { width: 22px; height: 22px; }
.nav-card-body { flex: 1; min-width: 0; }
.nav-card-name { font-size: 14px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
.nav-card-desc { font-size: 12px; color: #999; line-height: 1.5; }
.nav-card-arrow { color: #ccc; font-size: 16px; margin-top: 4px; transition: all 0.2s; }
.nav-card:hover .nav-card-arrow { color: #2D4A3E; transform: translateX(3px); }

/* Mid section */
.mid-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 28px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-action {
  font-size: 12px;
  color: #2D4A3E;
  background: none;
  border: none;
  cursor: pointer;
}
.section-action:hover { text-decoration: underline; }

/* Booking */
.booking-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.booking-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px;
}
.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.booking-label { font-size: 13px; font-weight: 600; color: #333; }
.booking-count {
  background: #2D4A3E;
  color: #fff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.booking-count.warning { background: #E6A23C; }
.booking-list { font-size: 12px; }
.booking-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}
.booking-item:last-child { border-bottom: none; }
.booking-info { display: flex; gap: 8px; }
.booking-box { font-weight: 600; color: #2D4A3E; }
.booking-time, .booking-date { color: #888; }
.booking-name, .booking-guests { color: #666; }
.empty-row {
  text-align: center;
  padding: 16px 0;
  color: #bbb;
  font-size: 12px;
}
.empty-row.full { grid-column: 1 / -1; }

/* Booking chart */
.booking-chart { margin-top: 8px; }
.chart-item { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.chart-label { font-size: 12px; color: #888; width: 30px; }
.chart-bar-wrap { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.chart-bar { height: 100%; background: #4A7C59; border-radius: 4px; transition: width 0.3s; }
.chart-bar.evening { background: #2D4A3E; }
.chart-value { font-size: 12px; font-weight: 600; color: #333; width: 24px; text-align: right; }

/* Approval */
.approval-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px;
}
.approval-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 10px;
}
.tab-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  background: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}
.tab-btn.active { background: #2D4A3E; color: #fff; }
.tab-count {
  background: rgba(0, 0, 0, 0.1);
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
}
.tab-btn.active .tab-count { background: rgba(255, 255, 255, 0.2); }
.approval-list { max-height: 260px; overflow-y: auto; }
.approval-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.approval-item:hover { background: #fafafa; }
.approval-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}
.approval-icon.purchase { background: #4A7C59; }
.approval-icon.expense { background: #E6A23C; }
.approval-icon.leave { background: #409EFF; }
.approval-icon.contract { background: #9B6B4A; }
.approval-icon.default { background: #909399; }
.approval-content { flex: 1; min-width: 0; }
.approval-title { font-size: 13px; font-weight: 500; color: #333; margin-bottom: 2px; }
.approval-meta { font-size: 11px; color: #999; display: flex; gap: 12px; }
.approval-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.approval-status.pending { color: #E6A23C; background: rgba(230, 162, 60, 0.1); }
.empty-state { text-align: center; padding: 24px 0; color: #bbb; font-size: 13px; }

/* Bottom section */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 28px;
}

/* Warning */
.warning-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px;
}
.warning-grid { display: flex; flex-direction: column; gap: 10px; }
.warning-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.warning-card.high { border-left: 4px solid #F56C6C; background: #fff5f5; }
.warning-card.medium { border-left: 4px solid #E6A23C; background: #fffbe6; }
.warning-card.low { border-left: 4px solid #409EFF; background: #f0f7ff; }
.warning-content { flex: 1; }
.warning-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 2px; }
.warning-desc { font-size: 12px; color: #888; margin-bottom: 4px; }
.warning-count { font-size: 11px; color: #999; }
.warning-action {
  padding: 6px 16px;
  border: 1px solid #2D4A3E;
  border-radius: 6px;
  background: none;
  color: #2D4A3E;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.warning-action:hover { background: #2D4A3E; color: #fff; }

/* Quick nav */
.quick-nav-section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 16px;
}
.quick-nav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.quick-nav-grid .nav-card {
  padding: 14px;
  position: relative;
}
.nav-text { font-size: 13px; font-weight: 600; color: #333; }
.nav-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #F56C6C;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 600;
}

/* Quick actions */
.quick-actions {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 20px;
}
.action-buttons { display: flex; gap: 10px; flex-wrap: wrap; }
.action-btn {
  padding: 9px 18px;
  border: 1px solid #2D4A3E;
  border-radius: 6px;
  background: #2D4A3E;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover { background: #3D5A4E; }

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .mid-section { grid-template-columns: 1fr; }
  .bottom-section { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .nav-grid { grid-template-columns: repeat(2, 1fr); }
  .booking-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .nav-grid { grid-template-columns: 1fr; }
  .quick-nav-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
