<template>
  <div class="dashboard-home">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">总经理总驾驶舱</h1>
        <p class="page-subtitle">General Manager Dashboard · {{ currentDate }}</p>
      </div>
      <div class="header-right">
        <div class="store-selector">
          <span class="selector-label">门店</span>
          <select v-model="selectedStore" class="selector-dropdown">
            <option value="all">全部门店</option>
            <option value="ningguo">宁国店</option>
            <option value="xuancheng">宣城店</option>
            <option value="hangzhou">杭州店</option>
          </select>
        </div>
        <div class="date-range">
          <span class="range-btn" :class="{ active: dateRange === 'today' }" @click="dateRange = 'today'">今日</span>
          <span class="range-btn" :class="{ active: dateRange === 'week' }" @click="dateRange = 'week'">本周</span>
          <span class="range-btn" :class="{ active: dateRange === 'month' }" @click="dateRange = 'month'">本月</span>
        </div>
      </div>
    </div>

    <!-- 经营指标卡片 -->
    <div class="stats-section">
      <h2 class="section-title">经营指标</h2>
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
            <div class="stat-value">¥{{ formatNumber(kpi.totalRevenue) }}</div>
            <div class="stat-trend up">+{{ kpi.revenueTrend }}%</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-detail">
            <div class="detail-item">
              <span>宁国店</span>
              <span>¥{{ formatNumber(kpi.ningguoRevenue) }}</span>
            </div>
            <div class="detail-item">
              <span>宣城店</span>
              <span>¥{{ formatNumber(kpi.xuanchengRevenue) }}</span>
            </div>
            <div class="detail-item">
              <span>杭州店</span>
              <span>¥{{ formatNumber(kpi.hangzhouRevenue) }}</span>
            </div>
          </div>
        </div>

        <div class="stat-card traffic" @click="goTo('guest-analysis')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6m0 6v.01"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日客流</div>
            <div class="stat-value">{{ kpi.traffic }}</div>
            <div class="stat-trend up">+{{ kpi.trafficTrend }}%</div>
          </div>
          <div class="stat-chart">
            <div class="chart-bar" v-for="(h, i) in hourlyTraffic" :key="i" :style="{ height: h + '%' }"></div>
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
            <div class="stat-value">{{ kpi.turnoverRate }}%</div>
            <div class="stat-trend" :class="kpi.turnoverTrend > 0 ? 'up' : 'down'">
              {{ kpi.turnoverTrend > 0 ? '+' : '' }}{{ kpi.turnoverTrend }}%
            </div>
          </div>
          <div class="stat-gauge">
            <svg viewBox="0 0 100 60" class="gauge-svg">
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" stroke="#e8edea" stroke-width="8"/>
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" :stroke="kpi.turnoverRate > 80 ? '#67C23A' : kpi.turnoverRate > 60 ? '#E6A23C' : '#F56C6C'" stroke-width="8" :stroke-dasharray="`${kpi.turnoverRate * 2.51} 251`"/>
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
            <div class="stat-value">{{ kpi.grossMargin }}%</div>
            <div class="stat-trend" :class="kpi.grossMarginTrend > 0 ? 'up' : 'down'">
              {{ kpi.grossMarginTrend > 0 ? '+' : '' }}{{ kpi.grossMarginTrend }}%
            </div>
          </div>
          <div class="stat-pie">
            <svg viewBox="0 0 100 100" class="pie-svg">
              <circle cx="50" cy="50" r="40" fill="none" stroke="#E8EDEB" stroke-width="20"/>
              <circle cx="50" cy="50" r="40" fill="none" stroke="#2D4A3E" stroke-width="20" :stroke-dasharray="`${kpi.grossMargin * 2.51} 251`" transform="rotate(-90 50 50)"/>
            </svg>
            <div class="pie-center">{{ kpi.grossMargin }}%</div>
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
            <div class="stat-value">¥{{ formatNumber(kpi.netProfit) }}</div>
            <div class="stat-trend up">+{{ kpi.netProfitTrend }}%</div>
          </div>
          <div class="stat-bar-chart">
            <div class="bar-item">
              <div class="bar-label">食材成本</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: kpi.costBreakdown.food + '%' }"></div>
              </div>
              <div class="bar-value">{{ kpi.costBreakdown.food }}%</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">人工成本</div>
              <div class="bar-track">
                <div class="bar-fill labor" :style="{ width: kpi.costBreakdown.labor + '%' }"></div>
              </div>
              <div class="bar-value">{{ kpi.costBreakdown.labor }}%</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">能耗费用</div>
              <div class="bar-track">
                <div class="bar-fill energy" :style="{ width: kpi.costBreakdown.energy + '%' }"></div>
              </div>
              <div class="bar-value">{{ kpi.costBreakdown.energy }}%</div>
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
            <div class="stat-value">{{ kpi.orderCount }}</div>
            <div class="stat-trend up">+{{ kpi.orderTrend }}%</div>
          </div>
          <div class="stat-channel">
            <div class="channel-item">
              <span class="channel-icon">堂食</span>
              <span class="channel-count">{{ kpi.channelDineIn }}</span>
            </div>
            <div class="channel-item">
              <span class="channel-icon takeout">外卖</span>
              <span class="channel-count">{{ kpi.channelTakeout }}</span>
            </div>
            <div class="channel-item">
              <span class="channel-icon banquet">宴席</span>
              <span class="channel-count">{{ kpi.channelBanquet }}</span>
            </div>
          </div>
        </div>
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
              <span class="booking-count">{{ booking.todayBoxes }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in booking.todayList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-time">{{ item.time }}</span>
                </div>
                <span class="booking-name">{{ item.name }}</span>
              </div>
            </div>
          </div>

          <div class="booking-card banquet">
            <div class="booking-header">
              <span class="booking-label">宴席预定</span>
              <span class="booking-count">{{ booking.banquetCount }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in booking.banquetList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-date">{{ item.date }}</span>
                </div>
                <span class="booking-guests">{{ item.guests }}人</span>
              </div>
            </div>
          </div>

          <div class="booking-card alert">
            <div class="booking-header">
              <span class="booking-label">空包厢预警</span>
              <span class="booking-count warning">{{ booking.emptyWarning }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in booking.emptyList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-status">{{ item.status }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="booking-card tomorrow">
            <div class="booking-header">
              <span class="booking-label">明日预定</span>
              <span class="booking-count">{{ booking.tomorrowTotal }}</span>
            </div>
            <div class="booking-chart">
              <div class="chart-item">
                <span class="chart-label">午市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar" :style="{ width: (booking.tomorrowLunch / booking.tomorrowTotal * 100) + '%' }"></div>
                </div>
                <span class="chart-value">{{ booking.tomorrowLunch }}</span>
              </div>
              <div class="chart-item">
                <span class="chart-label">晚市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar evening" :style="{ width: (booking.tomorrowDinner / booking.tomorrowTotal * 100) + '%' }"></div>
                </div>
                <span class="chart-value">{{ booking.tomorrowDinner }}</span>
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
            <span class="tab-icon">{{ tab.icon }}</span>
            <span class="tab-text">{{ tab.name }}</span>
            <span class="tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
          </button>
        </div>
        <div class="approval-list">
          <div v-for="item in filteredApprovals" :key="item.id" class="approval-item" @click="goTo(item.link)">
            <div class="approval-icon" :class="item.type"></div>
            <div class="approval-content">
              <div class="approval-title">{{ item.title }}</div>
              <div class="approval-meta">
                <span>{{ item.department }}</span>
                <span>{{ item.time }}</span>
              </div>
            </div>
            <div class="approval-amount" v-if="item.amount">{{ item.amount }}</div>
            <div class="approval-status" :class="item.status">{{ item.statusText }}</div>
          </div>
          <div v-if="filteredApprovals.length === 0" class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
            <span>暂无待审批事项</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三行：风险预警 + 快捷跳转 -->
    <div class="bottom-section">
      <!-- 风险预警 -->
      <div class="warning-section">
        <div class="section-header">
          <h2 class="section-title">风险预警</h2>
        </div>
        <div class="warning-grid">
          <div class="warning-card" v-for="item in warnings" :key="item.type" :class="item.level">
            <div class="warning-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="item.icon"></svg>
            </div>
            <div class="warning-content">
              <div class="warning-title">{{ item.title }}</div>
              <div class="warning-desc">{{ item.desc }}</div>
              <div class="warning-count">{{ item.count }}项待处理</div>
            </div>
            <button class="warning-action" @click="goTo(item.link)">处理</button>
          </div>
        </div>
      </div>

      <!-- 快捷跳转 -->
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
            <div class="nav-icon-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="nav.icon"></svg>
            </div>
            <div class="nav-text">{{ nav.name }}</div>
            <div class="nav-badge" v-if="nav.badge">{{ nav.badge }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBookings } from '@/api/booking'

const router = useRouter()

const selectedStore = ref('all')
const dateRange = ref('today')
const activeApprovalTab = ref('all')

const currentDate = computed(() => {
  const d = new Date()
  const opts = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
  return d.toLocaleDateString('zh-CN', opts)
})

function formatNumber(num) {
  return num.toLocaleString('zh-CN')
}

// KPI数据
const kpi = ref({
  totalRevenue: 48680,
  ningguoRevenue: 22400,
  xuanchengRevenue: 14280,
  hangzhouRevenue: 12000,
  revenueTrend: 12.5,
  traffic: 356,
  trafficTrend: 8.3,
  hourlyTraffic: [20, 35, 55, 70, 85, 95, 80, 65, 50, 35, 25, 15],
  turnoverRate: 72,
  turnoverTrend: 3.2,
  grossMargin: 68.5,
  grossMarginTrend: 1.8,
  netProfit: 18500,
  netProfitTrend: 15.2,
  costBreakdown: { food: 28, labor: 18, energy: 5 },
  orderCount: 128,
  orderTrend: 6.5,
  channelDineIn: 86,
  channelTakeout: 32,
  channelBanquet: 10
})

// 预定数据
const booking = ref({
  todayBoxes: 12,
  todayList: [
    { id: 1, box: '牡丹厅', time: '12:00', name: '张总' },
    { id: 2, box: '梅花厅', time: '12:30', name: '李经理' },
    { id: 3, box: '兰香厅', time: '18:00', name: '王总' },
    { id: 4, box: '竹韵厅', time: '18:30', name: '陈主任' },
  ],
  banquetCount: 3,
  banquetList: [
    { id: 1, box: '宴会厅A', date: '07-28', guests: 120 },
    { id: 2, box: '宴会厅B', date: '07-30', guests: 80 },
    { id: 3, box: '牡丹厅', date: '08-01', guests: 30 },
  ],
  emptyWarning: 3,
  emptyList: [
    { id: 1, box: '荷花厅', status: '全天空闲' },
    { id: 2, box: '桂花厅', status: '午市空闲' },
    { id: 3, box: '菊花厅', status: '晚市空闲' },
  ],
  tomorrowTotal: 15,
  tomorrowLunch: 6,
  tomorrowDinner: 9
})

// 审批标签页
const approvalTabs = ref([
  { key: 'all', name: '全部', icon: '', count: 12 },
  { key: 'procurement', name: '采购申请', icon: '', count: 4 },
  { key: 'leave', name: '员工请假', icon: '', count: 3 },
  { key: 'repair', name: '维修报修', icon: '', count: 2 },
  { key: 'expense', name: '费用报销', icon: '', count: 2 },
  { key: 'reconciliation', name: '供应商对账', icon: '', count: 1 },
])

// 审批列表数据
const approvals = ref([
  { id: 1, type: 'procurement', title: '食材采购申请 - 蔬菜类', department: '宁国店后厨', time: '10分钟前', amount: '¥3,500', status: 'pending', statusText: '待审批', link: 'approval-center' },
  { id: 2, type: 'procurement', title: '酒水采购申请 - 白酒', department: '宣城店前厅', time: '30分钟前', amount: '¥8,200', status: 'pending', statusText: '待审批', link: 'approval-center' },
  { id: 3, type: 'leave', title: '事假申请', department: '宁国店', time: '1小时前', amount: '', status: 'pending', statusText: '待审批', link: 'approval-center' },
  { id: 4, type: 'repair', title: '空调维修申请', department: '杭州店', time: '2小时前', amount: '¥1,500', status: 'pending', statusText: '待审批', link: 'approval-center' },
  { id: 5, type: 'expense', title: '办公用品报销', department: '宁国店行政', time: '3小时前', amount: '¥680', status: 'pending', statusText: '待审批', link: 'approval-center' },
  { id: 6, type: 'reconciliation', title: '供应商对账 - 鑫源食品', department: '财务部', time: '昨天', amount: '¥28,500', status: 'pending', statusText: '待确认', link: 'approval-center' },
])

const filteredApprovals = computed(() => {
  if (activeApprovalTab.value === 'all') return approvals.value
  return approvals.value.filter(a => a.type === activeApprovalTab.value)
})

// 风险预警数据
const warnings = ref([
  { 
    type: 'expiring', 
    title: '食材临期预警', 
    desc: '3种食材即将过期，请及时处理', 
    count: 3, 
    level: 'warning',
    icon: '<path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>',
    link: 'inventory'
  },
  { 
    type: 'hygiene', 
    title: '卫生巡检不合格', 
    desc: '2项卫生检查未达标', 
    count: 2, 
    level: 'danger',
    icon: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>',
    link: 'hygiene'
  },
  { 
    type: 'fire', 
    title: '消防隐患', 
    desc: '灭火器过期需更换', 
    count: 1, 
    level: 'danger',
    icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/>',
    link: 'safety'
  },
  { 
    type: 'energy', 
    title: '能耗异常', 
    desc: '杭州店用电量超出预警值', 
    count: 1, 
    level: 'warning',
    icon: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
    link: 'energy'
  },
  { 
    type: 'payment', 
    title: '应付账款到期', 
    desc: '3笔款项即将到期', 
    count: 3, 
    level: 'warning',
    icon: '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>',
    link: 'supplier-reconciliation'
  },
])

// 快捷导航
const quickNav = ref([
  { name: '审批中心', path: 'approval-center', icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/>', color: '#2D4A3E', badge: '12' },
  { name: '门店经营', path: 'revenue', icon: '<path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/>', color: '#4A7C59', badge: '' },
  { name: '供应链', path: 'supply-chain', icon: '<path d="M21 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2"/><path d="M3 10v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/>', color: '#5B7B8A', badge: '' },
  { name: '损耗报废', path: 'waste', icon: '<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>', color: '#F56C6C', badge: '' },
  { name: '安全卫生', path: 'hygiene', icon: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>', color: '#C25555', badge: '5' },
  { name: '人事考勤', path: 'attendance', icon: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>', color: '#8B9A8C', badge: '' },
  { name: '财务总账', path: 'finance', icon: '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>', color: '#6B7B8A', badge: '' },
  { name: '票据税务', path: 'tax', icon: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>', color: '#5B7B8A', badge: '' },
  { name: '工程管理', path: 'engineering', icon: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>', color: '#7B8D6E', badge: '' },
  { name: '能耗管理', path: 'energy', icon: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>', color: '#E6A23C', badge: '' },
  { name: '库存管理', path: 'inventory', icon: '<path d="M21 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2"/><path d="M3 10v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/><path d="M13 10h4"/>', color: '#4A7C59', badge: '' },
  { name: '数据大屏', path: 'data-screen', icon: '<rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>', color: '#2D4A3E', badge: '' },
])

function goTo(path) {
  router.push(`/dashboard/${path}`)
}

async function loadStats() {
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await getBookings({ booking_date: today, page_size: 999 })
    const list = res?.data?.list || res?.data || []
    booking.value.todayBoxes = list.length
    booking.value.todayList = list.slice(0, 4).map((b, i) => ({
      id: i + 1,
      box: b.room_name || '包厢' + (i + 1),
      time: b.time_slot || b.booking_time || '12:00',
      name: b.customer_name || '客户'
    }))
  } catch (e) {
    console.error('加载预订统计失败', e)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard-home {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #7a8c84;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.store-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selector-label {
  font-size: 13px;
  color: #7a8c84;
}

.selector-dropdown {
  padding: 8px 16px;
  border: 1px solid #e8edea;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.date-range {
  display: flex;
  background: #f5f7f6;
  border-radius: 8px;
  padding: 4px;
}

.range-btn {
  padding: 8px 20px;
  font-size: 13px;
  color: #7a8c84;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.range-btn.active {
  background: #fff;
  color: #2D4A3E;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

/* 统计区域 */
.stats-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.stat-card.revenue .stat-icon-wrap {
  background: rgba(45,74,62,0.08);
  color: #2D4A3E;
}

.stat-card.traffic .stat-icon-wrap {
  background: rgba(74,124,89,0.08);
  color: #4A7C59;
}

.stat-card.turnover .stat-icon-wrap {
  background: rgba(196,163,90,0.08);
  color: #C4A35A;
}

.stat-card.margin .stat-icon-wrap {
  background: rgba(91,123,138,0.08);
  color: #5B7B8A;
}

.stat-card.profit .stat-icon-wrap {
  background: rgba(45,74,62,0.08);
  color: #2D4A3E;
}

.stat-card.orders .stat-icon-wrap {
  background: rgba(139,154,140,0.08);
  color: #8B9A8C;
}

.stat-icon-wrap svg {
  width: 22px;
  height: 22px;
}

.stat-info {
  margin-bottom: 12px;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.stat-trend.up {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.stat-trend.down {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.stat-divider {
  height: 1px;
  background: #f0f2f1;
  margin-bottom: 12px;
}

.stat-detail {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.detail-item span:first-child {
  color: #999;
}

.detail-item span:last-child {
  color: #666;
  font-weight: 500;
}

.stat-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 60px;
}

.chart-bar {
  flex: 1;
  background: linear-gradient(180deg, #4A7C59 0%, #A8D5B8 100%);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
}

.stat-gauge {
  display: flex;
  justify-content: center;
}

.gauge-svg {
  width: 80px;
  height: 40px;
}

.stat-pie {
  position: relative;
  display: flex;
  justify-content: center;
}

.pie-svg {
  width: 70px;
  height: 70px;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 14px;
  font-weight: 700;
  color: #5B7B8A;
}

.stat-bar-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-label {
  width: 50px;
  font-size: 11px;
  color: #999;
}

.bar-track {
  flex: 1;
  height: 6px;
  background: #f0f2f1;
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #2D4A3E;
  border-radius: 3px;
}

.bar-fill.labor {
  background: #C4A35A;
}

.bar-fill.energy {
  background: #5B7B8A;
}

.bar-value {
  width: 35px;
  font-size: 11px;
  color: #666;
  text-align: right;
}

.stat-channel {
  display: flex;
  justify-content: space-around;
}

.channel-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.channel-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(45,74,62,0.08);
  color: #2D4A3E;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.channel-icon.takeout {
  background: rgba(74,124,89,0.08);
  color: #4A7C59;
}

.channel-icon.banquet {
  background: rgba(196,163,90,0.08);
  color: #C4A35A;
}

.channel-count {
  font-size: 16px;
  font-weight: 700;
  color: #2D4A3E;
}

/* 中间区域 */
.mid-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-action {
  padding: 8px 16px;
  font-size: 13px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.section-action:hover {
  background: rgba(45,74,62,0.12);
}

/* 预定看板 */
.booking-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.booking-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8edea;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.booking-label {
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.booking-count {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
}

.booking-count.warning {
  color: #E6A23C;
}

.booking-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.booking-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: #fafbfb;
  border-radius: 8px;
}

.booking-info {
  display: flex;
  gap: 8px;
}

.booking-box {
  font-size: 13px;
  font-weight: 600;
  color: #2D4A3E;
}

.booking-time, .booking-date, .booking-status {
  font-size: 12px;
  color: #999;
}

.booking-name, .booking-guests {
  font-size: 12px;
  color: #666;
}

.booking-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-label {
  width: 40px;
  font-size: 12px;
  color: #999;
}

.chart-bar-wrap {
  flex: 1;
  height: 8px;
  background: #f0f2f1;
  border-radius: 4px;
  overflow: hidden;
}

.chart-bar {
  height: 100%;
  background: #2D4A3E;
  border-radius: 4px;
}

.chart-bar.evening {
  background: #4A7C59;
}

.chart-value {
  width: 30px;
  font-size: 12px;
  font-weight: 600;
  color: #2D4A3E;
  text-align: right;
}

/* 审批区域 */
.approval-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  color: #7a8c84;
  background: #f5f7f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #2D4A3E;
  color: #fff;
}

.tab-count {
  padding: 2px 8px;
  background: #E6A23C;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
}

.tab-btn.active .tab-count {
  background: #fff;
  color: #2D4A3E;
}

.approval-list {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8edea;
  overflow: hidden;
}

.approval-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f2f1;
  cursor: pointer;
  transition: background 0.2s;
}

.approval-item:last-child {
  border-bottom: none;
}

.approval-item:hover {
  background: #fafbfb;
}

.approval-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.approval-icon.procurement {
  background: rgba(45,74,62,0.08);
  color: #2D4A3E;
}

.approval-icon.procurement::before {
  content: '采';
  font-size: 14px;
  font-weight: 700;
}

.approval-icon.leave {
  background: rgba(74,124,89,0.08);
  color: #4A7C59;
}

.approval-icon.leave::before {
  content: '请';
  font-size: 14px;
  font-weight: 700;
}

.approval-icon.repair {
  background: rgba(196,163,90,0.08);
  color: #C4A35A;
}

.approval-icon.repair::before {
  content: '修';
  font-size: 14px;
  font-weight: 700;
}

.approval-icon.expense {
  background: rgba(91,123,138,0.08);
  color: #5B7B8A;
}

.approval-icon.expense::before {
  content: '报';
  font-size: 14px;
  font-weight: 700;
}

.approval-icon.reconciliation {
  background: rgba(139,154,140,0.08);
  color: #8B9A8C;
}

.approval-icon.reconciliation::before {
  content: '对';
  font-size: 14px;
  font-weight: 700;
}

.approval-content {
  flex: 1;
  min-width: 0;
}

.approval-title {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.approval-amount {
  font-size: 14px;
  font-weight: 700;
  color: #2D4A3E;
  flex-shrink: 0;
}

.approval-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.approval-status.pending {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
}

.empty-state svg {
  width: 40px;
  height: 40px;
}

.empty-state span {
  font-size: 14px;
  color: #999;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* 风险预警 */
.warning-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.warning-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border-left: 4px solid #ccc;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.warning-card.warning {
  border-left-color: #E6A23C;
}

.warning-card.danger {
  border-left-color: #F56C6C;
}

.warning-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning-card.warning .warning-icon {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.warning-card.danger .warning-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.warning-icon svg {
  width: 18px;
  height: 18px;
}

.warning-title {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.warning-desc {
  font-size: 12px;
  color: #999;
}

.warning-count {
  font-size: 11px;
  color: #E6A23C;
  font-weight: 600;
}

.warning-action {
  align-self: flex-end;
  padding: 6px 12px;
  font-size: 12px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

/* 快捷导航 */
.quick-nav-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.nav-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8edea;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

.nav-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  border-color: var(--card-color);
}

.nav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-color);
  border-radius: 12px 12px 0 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.nav-card:hover::before {
  opacity: 1;
}

.nav-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--card-color) 8%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-wrap svg {
  width: 22px;
  height: 22px;
  color: var(--card-color);
}

.nav-text {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.nav-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  background: #F56C6C;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 10px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .warning-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .mid-section, .bottom-section {
    grid-template-columns: 1fr;
  }
  .quick-nav-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .booking-grid {
    grid-template-columns: 1fr;
  }
  .warning-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-nav-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>