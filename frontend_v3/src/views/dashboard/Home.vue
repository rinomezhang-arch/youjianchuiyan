<template>
  <div class="home">
    <div class="top-bar">
      <div class="top-logo">
        <svg width="32" height="32" viewBox="0 0 64 64" fill="none">
          <rect x="12" y="28" width="16" height="16" rx="2" stroke="#2D4A3E" stroke-width="2.5"/>
          <rect x="36" y="28" width="16" height="16" rx="2" stroke="#2D4A3E" stroke-width="2.5"/>
          <path d="M20 28 L20 18 L28 18" stroke="#2D4A3E" stroke-width="2"/>
          <path d="M44 28 L44 18 L36 18" stroke="#2D4A3E" stroke-width="2"/>
          <path d="M20 12 C24 8 32 6 32 6 C32 6 40 8 44 12" stroke="#2D4A3E" stroke-width="2" fill="none"/>
        </svg>
      </div>
      <div class="top-title-area">
        <h2 class="page-title">控制台</h2>
        <p class="page-subtitle">{{ storeName }} · {{ currentDate }}</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          </div>
          <span class="stat-badge">2</span>
        </div>
        <div class="stat-value">2</div>
        <div class="stat-label">餐厅总数</div>
        <div class="stat-sub">Total Stores</div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9 12l2 2 4-4"/></svg>
          </div>
          <span class="stat-badge">1</span>
        </div>
        <div class="stat-value">1</div>
        <div class="stat-label">营业餐厅</div>
        <div class="stat-sub">Operating Stores</div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <span class="stat-badge">{{ stats.staffCount }}</span>
        </div>
        <div class="stat-value">{{ stats.staffCount }}</div>
        <div class="stat-label">员工总数</div>
        <div class="stat-sub">Total Staff</div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          </div>
          <span class="stat-badge">¥{{ stats.todayRevenue }}</span>
        </div>
        <div class="stat-value">¥{{ stats.todayRevenue }}</div>
        <div class="stat-label">今日营收</div>
        <div class="stat-sub">Today Revenue</div>
      </div>
    </div>

    <div class="section-header">
      <h3 class="section-title">功能导航</h3>
    </div>

    <div class="nav-grid">
      <div v-for="item in navItems" :key="item.path" class="nav-card" @click="goTo(item.path)">
        <div class="nav-card-icon" :style="{ background: item.color }">
          <div v-html="item.icon"></div>
        </div>
        <div class="nav-card-body">
          <div class="nav-card-name">{{ item.name }}</div>
          <div class="nav-card-desc">{{ item.desc }}</div>
        </div>
        <span class="nav-card-arrow">→</span>
      </div>
    </div>

    <div class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="action-buttons">
        <button class="action-btn" @click="goTo('table-board')">开台</button>
        <button class="action-btn" @click="goTo('menu')">加菜</button>
        <button class="action-btn" @click="goTo('front-office')">结账</button>
        <button class="action-btn" @click="goTo('kitchen')">呼叫后厨</button>
        <button class="action-btn" @click="goTo('front-office')">保洁呼叫</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({ staffCount: 0, todayRevenue: 0 })

const storeName = computed(() => userStore.storeName || '宁国店')
const currentDate = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

onMounted(() => { loadStats() })
async function loadStats() {
  try {
    const resp = await fetch('/api/dashboard/stats', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
    })
    if (resp.ok) { const d = await resp.json(); if (d?.code === 200 && d.data) stats.value = { ...stats.value, ...d.data } }
  } catch {}
}

const navItems = [
  { name: '前厅运营', path: 'front-office', color: '#2D4A3E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="6" width="16" height="12" rx="2"/><polyline points="14 2 10 6 6 2"/></svg>', desc: '客户接待 · 桌台服务 · 收银结算' },
  { name: '菜单管理', path: 'menu-manager', color: '#4A7C59', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><line x1="6" y1="5" x2="18" y2="5"/><line x1="6" y1="10" x2="18" y2="10"/><line x1="6" y1="15" x2="18" y2="15"/><line x1="2" y1="5" x2="2.01" y2="5"/><line x1="2" y1="10" x2="2.01" y2="10"/><line x1="2" y1="15" x2="2.01" y2="15"/></svg>', desc: '菜品库 · 成本卡 · 套餐组合' },
  { name: '厨房管理', path: 'kitchen', color: '#E2764A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><path d="M6 12a3.5 3.5 0 0 1 0-7h1a3.5 3.5 0 0 1 0 7H6z"/><path d="M6 5v12"/><path d="M14 12a3.5 3.5 0 0 1 0-7h1a3.5 3.5 0 0 1 0 7h-1z"/><path d="M14 5v12"/></svg>', desc: '出品管控 · 食材管理 · 卫生巡检' },
  { name: '采购仓储', path: 'supply-chain', color: '#5B8B7A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="6" width="16" height="12" rx="2"/><line x1="10" y1="6" x2="10" y2="2"/><line x1="2" y1="11" x2="18" y2="11"/></svg>', desc: '采购管理 · 库存预警 · 盘点管理' },
  { name: '营销会员', path: 'marketing', color: '#C4A35A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="8" cy="6" r="3"/><path d="M14 12v-2a3 3 0 0 0-3-3H8a3 3 0 0 0-3 3v2"/><path d="M18 17v-2a3 3 0 0 0-3-3h-2"/></svg>', desc: '会员管理 · 营销活动 · 积分商城' },
  { name: '人事行政', path: 'hr-admin', color: '#6B8C9E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="2" width="16" height="6" rx="1"/><rect x="2" y="12" width="16" height="6" rx="1"/></svg>', desc: '员工管理 · 考勤排班 · 薪资福利' },
  { name: '财务数据', path: 'finance', color: '#8B9A6E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="10" cy="10" r="8"/><path d="M10 5v10M7 8h6M7 12h6"/></svg>', desc: '营收核算 · 对账管理 · 财务报表' },
  { name: '报表中心', path: 'reports', color: '#7B8DAE', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><path d="M12 2H4a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7z"/><polyline points="12 2 12 7 17 7"/><line x1="6" y1="11" x2="14" y2="11"/><line x1="6" y1="14" x2="14" y2="14"/></svg>', desc: '经营报表 · 菜品排行 · 客流分析' },
  { name: '系统设置', path: 'settings', color: '#95A5A6', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="10" cy="10" r="2.5"/><path d="M10 2v2M10 16v2M4 4l1.5 1.5M14.5 14.5L16 16M2 10h2M16 10h2M4 16l1.5-1.5M14.5 5.5L16 4"/></svg>', desc: '门店配置 · 权限管理 · 系统维护' },
]

function goTo(path) { router.push(`/dashboard/${path}`) }
</script>

<style scoped>
.home { max-width: 1280px; margin: 0 auto; }

.top-bar {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 28px;
}
.top-logo {
  width: 48px; height: 48px;
  background: rgba(45,74,62,0.06);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.page-title { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0 0 2px 0; }
.page-subtitle { font-size: 12px; color: #8a8a8a; margin: 0; }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.stat-card {
  background: #1E2A24;
  border-radius: 14px;
  padding: 20px 22px;
}
.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.stat-icon { width: 26px; height: 26px; color: #4CAF50; }
.stat-badge {
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.7);
  padding: 2px 10px; border-radius: 10px;
  font-size: 12px; font-weight: 600;
}
.stat-value { font-size: 30px; font-weight: 700; color: #4CAF50; line-height: 1.1; margin-bottom: 6px; }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.55); margin-bottom: 2px; }
.stat-sub { font-size: 11px; color: rgba(255,255,255,0.25); }

/* Section */
.section-header { display: flex; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 700; color: #1a1a1a; }

/* Nav cards */
.nav-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 28px; }
.nav-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px;
  display: flex; align-items: flex-start; gap: 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.nav-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); border-color: #ccc; }
.nav-card-icon {
  width: 44px; height: 44px;
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

/* Quick actions */
.quick-actions { background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 20px; }
.action-buttons { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.action-btn {
  padding: 9px 18px;
  border: 1px solid #2D4A3E; border-radius: 6px;
  background: #2D4A3E; color: #fff;
  font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover { background: #3D5A4E; }

@media (max-width: 900px) { .stats-row { grid-template-columns: repeat(2, 1fr); } .nav-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .stats-row { grid-template-columns: 1fr; } .nav-grid { grid-template-columns: 1fr; } }
</style>
