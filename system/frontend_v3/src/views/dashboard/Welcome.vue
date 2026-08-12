<template>
  <div class="welcome-page">
    <div class="welcome-container">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">
          <img src="@/assets/images/logo.png" alt="又见炊烟" />
        </div>
        <h1 class="brand-name">又见炊烟私房菜</h1>
        <p class="brand-eng">Youjianchuiyan Private Kitchen</p>
        <div class="brand-divider"></div>
        <p class="brand-slogan">传承经典 · 匠心独运</p>
      </div>

      <!-- 欢迎语 -->
      <div class="welcome-section">
        <h2 class="welcome-title">欢迎使用餐饮管理系统</h2>
        <p class="welcome-subtitle">Welcome to Restaurant Management System</p>
        <p class="welcome-desc">
          智能化餐饮全流程管理平台，助您提升运营效率，优化顾客体验
        </p>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-access">
        <h3 class="section-title">快捷入口 · Quick Access</h3>
        <div class="access-grid">
          <div class="access-card" @click="goTo('table-board')">
            <div class="access-icon" style="background: rgba(45, 74, 62, 0.1); color: #2D4A3E">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M9 9h6"/>
                <path d="M12 6v6"/>
              </svg>
            </div>
            <div class="access-text">
              <div class="access-name">桌台管理</div>
              <div class="access-desc">开台·点菜·结账</div>
            </div>
          </div>

          <div class="access-card" @click="goTo('bookings')">
            <div class="access-icon" style="background: rgba(74, 124, 89, 0.1); color: #4A7C59">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2"/>
                <path d="M16 2v4"/>
                <path d="M8 2v4"/>
                <path d="M3 10h18"/>
              </svg>
            </div>
            <div class="access-text">
              <div class="access-name">预订管理</div>
              <div class="access-desc">宴会·包厢·预订</div>
            </div>
          </div>

          <div class="access-card" @click="goTo('kitchen')">
            <div class="access-icon" style="background: rgba(196, 163, 90, 0.1); color: #C4A35A">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
            </div>
            <div class="access-text">
              <div class="access-name">厨房出品</div>
              <div class="access-desc">订单·制作·出餐</div>
            </div>
          </div>

          <div class="access-card" @click="goTo('reports')">
            <div class="access-icon" style="background: rgba(91, 123, 138, 0.1); color: #5B7B8A">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 20V10"/>
                <path d="M12 20V4"/>
                <path d="M6 20v-6"/>
              </svg>
            </div>
            <div class="access-text">
              <div class="access-name">数据报表</div>
              <div class="access-desc">营收·客流·分析</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 今日概览 -->
      <div class="today-overview" v-loading="statsLoading">
        <h3 class="section-title">今日概览 · Today Overview</h3>
        <div class="overview-grid">
          <div class="overview-item">
            <div class="overview-value">{{ todayStats.bookings }}</div>
            <div class="overview-label">今日预订</div>
          </div>
          <div class="overview-item">
            <div class="overview-value">{{ todayStats.tables }}</div>
            <div class="overview-label">在用桌台</div>
          </div>
          <div class="overview-item">
            <div class="overview-value highlight">¥{{ todayStats.revenue.toLocaleString() }}</div>
            <div class="overview-label">今日营收</div>
          </div>
          <div class="overview-item">
            <div class="overview-value">{{ todayStats.guests }}</div>
            <div class="overview-label">接待客流</div>
          </div>
        </div>
      </div>

      <!-- 系统信息 -->
      <div class="system-info">
        <div class="info-row">
          <span class="info-label">当前门店：</span>
          <span class="info-value">{{ storeName }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前用户：</span>
          <span class="info-value">{{ userName }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">系统版本：</span>
          <span class="info-value">v3.2.1</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前时间：</span>
          <span class="info-value">{{ currentTime }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" size="large" @click="goTo('home')">
          进入工作台
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-left: 6px">
            <line x1="5" y1="12" x2="19" y2="12"/>
            <polyline points="12 5 19 12 12 19"/>
          </svg>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const currentTime = ref('')
let timer = null

const storeName = computed(() => userStore.storeName || '宁国店')
const userName = computed(() => userStore.userInfo?.staffName || '管理员')

const todayStats = ref({ bookings: 0, tables: 0, revenue: 0, guests: 0 })
const statsLoading = ref(false)

async function loadTodayStats() {
  statsLoading.value = true
  try {
    const res = await request.get('/report/overview')
    const data = res.data || {}
    todayStats.value = {
      bookings: Number(data.todayBookings || 0),
      tables: Number(data.occupiedTables || 0),
      revenue: Number(data.todayRevenue || 0),
      guests: Number(data.todayGuests || 0)
    }
  } finally {
    statsLoading.value = false
  }
}

function updateTime() {
  const now = new Date()
  const options = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }
  currentTime.value = now.toLocaleString('zh-CN', options)
}

function goTo(path) {
  router.push(`/dashboard/${path}`)
}

onMounted(() => {
  updateTime()
  loadTodayStats()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.welcome-page {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.welcome-container {
  max-width: 900px;
  width: 100%;
  background: var(--color-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  padding: 60px 40px;
  text-align: center;
}

/* Logo区域 */
.logo-section {
  margin-bottom: 40px;
}

.logo-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-name {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 8px;
  letter-spacing: 2px;
  font-family: var(--font-family);
}

.brand-eng {
  font-size: 14px;
  color: var(--color-text-muted);
  letter-spacing: 1px;
  margin-bottom: 16px;
  font-family: var(--font-family-sans);
}

.brand-divider {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--color-accent), transparent);
  margin: 0 auto 16px;
}

.brand-slogan {
  font-size: 16px;
  color: var(--color-accent-dark);
  letter-spacing: 4px;
  font-family: var(--font-family);
}

/* 欢迎语 */
.welcome-section {
  margin-bottom: 40px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
  font-family: var(--font-family-sans);
}

.welcome-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  max-width: 600px;
  margin: 0 auto;
}

/* 快捷入口 */
.quick-access {
  margin-bottom: 40px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
}

.access-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.access-card {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.access-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent);
}

.access-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.access-icon svg {
  width: 28px;
  height: 28px;
}

.access-text {
  text-align: center;
}

.access-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.access-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 今日概览 */
.today-overview {
  margin-bottom: 40px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.overview-item {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 20px;
  text-align: center;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.overview-value.highlight {
  color: var(--color-primary);
}

.overview-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

/* 系统信息 */
.system-info {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 32px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  text-align: left;
}

.info-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: var(--color-text-muted);
  min-width: 80px;
}

.info-value {
  color: var(--color-text);
  font-weight: 500;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
}

.action-buttons :deep(.el-button) {
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .welcome-container {
    padding: 40px 20px;
  }

  .access-grid,
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .system-info {
    grid-template-columns: 1fr;
  }

  .brand-name {
    font-size: 24px;
  }

  .welcome-title {
    font-size: 20px;
  }
}
</style>
