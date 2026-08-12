<template>
  <div class="home">
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <p class="page-subtitle">{{ storeName }} · {{ currentDate }}</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <path d="M16 2v4"/>
              <path d="M8 2v4"/>
              <path d="M3 10h18"/>
            </svg>
          </div>
          <span class="stat-trend up">{{ stats.todayTotal }}桌</span>
        </div>
        <div class="stat-label">今日预订总数</div>
        <div class="stat-value">{{ stats.todayTotal }}</div>
        <div class="stat-sub">Total Bookings Today</div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5"/>
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>
          </div>
          <span class="stat-trend">午餐</span>
        </div>
        <div class="stat-label">午餐时段预订</div>
        <div class="stat-value">{{ stats.lunchCount }}</div>
        <div class="stat-sub">Lunch Bookings</div>
      </div>
      <div class="stat-card" :style="{ color: '#C4A35A' }">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </div>
          <span class="stat-trend up">晚餐</span>
        </div>
        <div class="stat-label">晚餐时段预订</div>
        <div class="stat-value">{{ stats.dinnerCount }}</div>
        <div class="stat-sub">Dinner Bookings</div>
      </div>
      <div class="stat-card" :style="{ color: '#C25555' }">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
          </div>
          <span class="stat-trend">需处理</span>
        </div>
        <div class="stat-label">待确认预订</div>
        <div class="stat-value">{{ stats.pendingCount }}</div>
        <div class="stat-sub">Pending Confirmation</div>
      </div>
    </div>

    <div class="section-header">
      <h3 class="section-title">功能导航</h3>
    </div>

    <div class="nav-grid">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-card"
        :style="{ '--card-color': item.color }"
        @click="goTo(item.path)"
      >
        <div class="card-top">
          <div class="card-icon-wrap">
            <div class="card-icon" v-html="item.icon"></div>
          </div>
          <div class="card-badge" v-if="item.badge">{{ item.badge }}</div>
        </div>
        <div class="card-body">
          <div class="card-title">{{ item.name }}</div>
          <div class="card-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="quick-actions">
        <h3 class="section-title">快捷操作</h3>
        <div class="action-buttons">
          <button class="action-btn" @click="goTo('table-board')">
            <span class="btn-icon">开台</span>
          </button>
          <button class="action-btn" @click="goTo('menu')">
            <span class="btn-icon">加菜</span>
          </button>
          <button class="action-btn" @click="goTo('front-office')">
            <span class="btn-icon">结账</span>
          </button>
          <button class="action-btn" @click="goTo('kitchen')">
            <span class="btn-icon">呼叫后厨</span>
          </button>
          <button class="action-btn" @click="goTo('front-office')">
            <span class="btn-icon">呼叫保洁</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getBookings } from '@/api/booking'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  todayTotal: 0,
  lunchCount: 0,
  dinnerCount: 0,
  pendingCount: 0
})

const storeName = computed(() => userStore.storeName || '宁国店')

const currentDate = computed(() => {
  const d = new Date()
  const opts = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
  return d.toLocaleDateString('zh-CN', opts)
})

function fmtDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadStats() {
  try {
    const today = fmtDate(new Date())
    const res = await getBookings({ booking_date: today, page_size: 999 })
    const list = res?.data?.list || res?.data || []
    let lunch = 0
    let dinner = 0
    let pending = 0
    list.forEach(b => {
      const t = b.time_slot || b.booking_time || ''
      const hour = parseInt(String(t).split(':')[0] || '0')
      if (hour < 15) lunch++
      else dinner++
      const s = b.status || ''
      if (s === 'pending' || s === '待确认' || s === 'unconfirmed') pending++
    })
    stats.value = {
      todayTotal: list.length,
      lunchCount: lunch,
      dinnerCount: dinner,
      pendingCount: pending
    }
  } catch (e) {
    console.error('加载预订统计失败', e)
  }
}

onMounted(() => {
  loadStats()
})

const navItems = [
  {
    name: '前厅运营',
    path: 'front-office',
    desc: '客户接待 · 桌台服务 · 收银结算',
    color: '#2D4A3E',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 9h6"/><path d="M12 6v6"/></svg>',
    badge: '7项'
  },
  {
    name: '厨房出品',
    path: 'kitchen',
    desc: '菜品制作 · 出餐管控 · 卫生巡检',
    color: '#4A7C59',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>',
    badge: '6项'
  },
  {
    name: '采购仓储',
    path: 'supply-chain',
    desc: '采购管理 · 库存预警 · 成本分析',
    color: '#5B7B8A',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2"/><path d="M3 10v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/></svg>',
    badge: '4项'
  },
  {
    name: '营销会员',
    path: 'marketing',
    desc: '会员管理 · 活动营销 · 线上平台',
    color: '#C4A35A',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>',
    badge: '5项'
  },
  {
    name: '人事行政',
    path: 'hr-admin',
    desc: '员工管理 · 考勤排班 · 培训考核',
    color: '#8B9A8C',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/></svg>',
    badge: '6项'
  },
  {
    name: '财务数据',
    path: 'finance',
    desc: '营收核算 · 对账管理 · 成本报表',
    color: '#6B7B8A',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>',
    badge: '4项'
  },
  {
    name: '系统设置',
    path: 'settings',
    desc: '门店配置 · 权限管理 · 硬件对接',
    color: '#95A5A6',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v2"/><path d="M12 21v2"/><path d="M4.22 4.22l1.42 1.42"/><path d="M18.36 18.36l1.42 1.42"/></svg>',
    badge: ''
  },
  {
    name: '数据大屏',
    path: 'data-screen',
    desc: '经营分析 · 菜品排行 · 客流统计',
    color: '#2D4A3E',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/></svg>',
    badge: '实时'
  },
  {
    name: '工程管理',
    path: 'engineering',
    desc: '设备维护 · 装修管理 · 能耗安全',
    color: '#7B8D6E',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>',
    badge: '6项'
  }
]

function goTo(path) {
  router.push(`/dashboard/${path}`)
}
</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  position: relative;
  overflow: hidden;
  transition: var(--transition-slow);
}

.stat-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: currentColor;
  opacity: 0.03;
  border-radius: 0 0 0 100px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.stat-icon {
  width: 36px;
  height: 36px;
  opacity: 0.7;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  background: var(--color-bg-alt);
  color: var(--color-text-secondary);
}

.stat-trend.up {
  background: rgba(45, 74, 62, 0.06);
  color: var(--color-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 6px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.nav-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  transition: var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.nav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-color);
  opacity: 0;
  transition: opacity 0.3s;
}

.nav-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--card-color);
}

.nav-card:hover::before {
  opacity: 1;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--card-color) 8%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon {
  width: 24px;
  height: 24px;
  color: var(--card-color);
}

.card-badge {
  padding: 2px 10px;
  border-radius: 12px;
  background: var(--card-color);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.card-body {
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.card-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.bottom-section {
  margin-top: 24px;
}

.quick-actions {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 12px 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.action-btn:hover {
  background: rgba(45, 74, 62, 0.06);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .nav-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .nav-grid {
    grid-template-columns: 1fr;
  }
  .action-buttons {
    flex-direction: column;
  }
}
</style>
