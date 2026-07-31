<template>
  <div class="menu-hub">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">菜单管理 · Menu Manager</h1>
        <span class="page-desc">菜品总生命周期 · 成本配方 · 沽清管控 · 调价控制 · 数据看板</span>
      </div>
    </div>

    <!-- 4张统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-label">菜品总数 · Total</div>
          <div class="stat-value">{{ totalDishes }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-label">在售 · Active</div>
          <div class="stat-value active">{{ activeDishes }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-label">已沽清 · Sold Out</div>
          <div class="stat-value soldout">{{ soldoutCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-label">平均毛利率 · Gross Margin</div>
          <div class="stat-value margin">{{ avgMargin }}%</div>
        </div>
      </div>
    </div>

    <!-- 快速功能入口 -->
    <div class="section-title">
      <h2>功能快速入口 · Quick Access</h2>
    </div>
    <div class="quick-access">
      <div v-for="mod in quickModules" :key="mod.path" class="qa-card" @click="goTo(mod.path)">
        <div class="qa-icon">
          <span v-html="mod.iconSvg"></span>
        </div>
        <div class="qa-info">
          <span class="qa-name">{{ mod.name }}</span>
          <span class="qa-sub">{{ mod.sub }}</span>
        </div>
      </div>
    </div>

    <!-- 第二行：菜品销售排行 + 沽清损失 -->
    <div class="charts-row">
      <div class="chart-card">
        <h4>菜品销售排行 · Sales Ranking</h4>
        <div class="sales-ranking">
          <div v-for="(item, i) in salesRanking" :key="i" class="rank-item">
            <span class="rank-num" :class="{ 'top3': i < 3 }">{{ i + 1 }}</span>
            <span class="rank-name">{{ item.name }}</span>
            <div class="rank-bar-wrap">
              <div class="rank-bar" :style="{ width: item.percent + '%' }"></div>
            </div>
            <span class="rank-count">{{ item.count }}</span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h4>沽清损失分析 · Sold Out & Loss</h4>
        <div class="soldout-summary">
          <div class="so-stat">
            <span class="so-num">{{ soldoutCount }}</span>
            <span class="so-label">已沽清菜品</span>
          </div>
          <div class="so-stat">
            <span class="so-num loss">¥{{ soldoutLoss }}</span>
            <span class="so-label">预估损失</span>
          </div>
        </div>
        <div class="soldout-list">
          <div v-for="item in soldoutItems" :key="item.name" class="so-item">
            <span class="so-item-name">{{ item.name }}</span>
            <span class="so-item-price">¥{{ item.price }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三行：营收毛利分析 + 调价影响 -->
    <div class="charts-row">
      <div class="chart-card">
        <h4>营收毛利分析 · Revenue & Margin</h4>
        <div class="revenue-list">
          <div v-for="item in revenueData" :key="item.name" class="rev-item">
            <span class="rev-name">{{ item.name }}</span>
            <div class="rev-bar-wrap">
              <div class="rev-bar" :style="{ width: item.percent + '%', background: item.color }"></div>
            </div>
            <span class="rev-value">¥{{ item.value.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h4>调价效果分析 · Price Change Impact</h4>
        <div class="price-impact">
          <div v-for="item in priceImpact" :key="item.name" class="pi-item">
            <span class="pi-name">{{ item.name }}</span>
            <span class="pi-change" :class="{ 'up': item.change > 0, 'down': item.change < 0 }">
              {{ item.change > 0 ? '+' : '' }}{{ item.change }}%
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const totalDishes = ref(0)
const activeDishes = ref(0)
const soldoutCount = ref(0)
const avgMargin = ref(0)

const quickModules = [
  { name: '点菜', sub: 'Ordering', path: '/dashboard/ordering', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="1.5"><path d="M3 3h18v18H3V3z"/><path d="M9 9h6v6H9V9z"/><path d="M9 3v6M15 3v6M9 15v6M15 15v6M3 9h6M3 15h6M15 9h6M15 15h6"/></svg>' },
  { name: '菜库编辑', sub: 'Dish Library', path: '/dashboard/dish-library', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="16" y2="11"/></svg>' },
  { name: '成本配方', sub: 'Cost Recipe', path: '/dashboard/cost-recipe', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><path d="M9 15l2 2 4-4"/></svg>' },
  { name: '套餐管理', sub: 'Set Menu', path: '/dashboard/set-menu', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>' },
  { name: '调价管理', sub: 'Pricing', path: '/dashboard/pricing-manage', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="1.5"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>' },
  { name: '沽清管控', sub: 'Sold Out', path: '/dashboard/soldout-control', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>' },
  { name: '标签管理', sub: 'Tags', path: '/dashboard/tags', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#8B6914" stroke-width="1.5"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>' },
  { name: '打印配置', sub: 'Print', path: '/dashboard/print-config', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#6B3A2A" stroke-width="1.5"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>' },
  { name: '门店权限', sub: 'Store', path: '/dashboard/store-permission', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#4A3728" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>' },
  { name: '操作日志', sub: 'Audit Log', path: '/dashboard/audit-log', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' },
  { name: '多价格体系', sub: 'Price Tiers', path: '/dashboard/price-tiers', iconSvg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="1.5"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="9" y2="6"/><line x1="3" y1="12" x2="9" y2="12"/><line x1="3" y1="18" x2="9" y2="18"/></svg>' },
]

const salesRanking = ref([])
const soldoutItems = ref([])
const soldoutLoss = ref(0)
const revenueData = ref([])
const priceImpact = ref([])

const goTo = (path) => router.push(path)

onMounted(async () => {
  try {
    const res = await fetch('/api/dishes?storeId=1&pageSize=999', { credentials: 'include' })
    const data = await res.json()
    const dishes = data.code === 200 ? (data.data?.content || data.data || []) : []

    totalDishes.value = dishes.length
    activeDishes.value = dishes.filter(d => d.isActive !== 0 && d.isActive !== false).length
    soldoutCount.value = dishes.filter(d => d.isActive === 0 || d.isActive === false).length

    const prices = dishes.map(d => parseFloat(d.salePrice) || 0).filter(p => p > 0)
    const costs = dishes.map(d => parseFloat(d.costPrice) || 0)
    const margins = dishes.filter((d, i) => prices[i] > 0 && costs[i] > 0).map((d, i) => ((prices[i] - costs[i]) / prices[i] * 100))
    avgMargin.value = margins.length ? (margins.reduce((a, b) => a + b, 0) / margins.length).toFixed(1) : 0

    // 销售排行（暂无数据，等待真实接口）
    salesRanking.value = []

    // 沽清菜品
    const soldoutDishes = dishes.filter(d => d.isActive === 0 || d.isActive === false)
    soldoutItems.value = soldoutDishes.slice(0, 5).map(d => ({ name: d.dishName || d.dish_name, price: parseFloat(d.salePrice || d.sale_price || 0).toFixed(0) }))
    soldoutLoss.value = soldoutDishes.reduce((sum, d) => sum + (parseFloat(d.salePrice || d.sale_price || 0) * 10), 0).toFixed(0)

    // 营收数据（暂无数据，等待真实接口）
    revenueData.value = []

    // 调价影响（暂无数据，等待真实接口）
    priceImpact.value = []
  } catch (e) {
    console.error('加载菜单数据失败:', e)
  }
})
</script>

<style scoped>
.menu-hub { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; display: block; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 2px; padding: 20px; display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { flex: 1; }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text-primary); }
.stat-value.active { color: #4A7C59; }
.stat-value.soldout { color: #C0392B; }
.stat-value.margin { color: #C4A35A; }

.section-title { margin-bottom: 16px; }
.section-title h2 { font-size: 16px; font-weight: 700; color: var(--color-text-primary); margin: 0; }

.quick-access { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 28px; }
.qa-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 2px; padding: 18px 16px; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: all 0.2s; }
.qa-card:hover { border-color: var(--color-accent); box-shadow: 0 2px 8px rgba(0,0,0,0.06); transform: translateY(-1px); }
.qa-icon { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.qa-info { display: flex; flex-direction: column; }
.qa-name { font-size: 14px; font-weight: 600; color: var(--color-text-primary); }
.qa-sub { font-size: 11px; color: var(--color-text-secondary); }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 2px; padding: 20px; }
.chart-card h4 { font-size: 15px; font-weight: 600; color: var(--color-text-primary); margin: 0 0 16px 0; }

.sales-ranking { display: flex; flex-direction: column; gap: 10px; }
.rank-item { display: grid; grid-template-columns: 24px 80px 1fr 40px; align-items: center; gap: 10px; }
.rank-num { font-size: 12px; font-weight: 700; color: var(--color-text-secondary); text-align: center; }
.rank-num.top3 { color: #C4A35A; }
.rank-name { font-size: 13px; color: var(--color-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-bar-wrap { height: 8px; background: rgba(45,74,62,0.08); border-radius: 4px; overflow: hidden; }
.rank-bar { height: 100%; background: linear-gradient(90deg, #2D4A3E, #4A7C59); border-radius: 4px; transition: width 0.6s ease; }
.rank-count { font-size: 12px; font-weight: 600; color: var(--color-text-primary); text-align: right; }

.soldout-summary { display: flex; gap: 24px; margin-bottom: 16px; }
.so-stat { display: flex; flex-direction: column; }
.so-num { font-size: 24px; font-weight: 700; color: #C0392B; }
.so-num.loss { font-size: 20px; }
.so-label { font-size: 12px; color: var(--color-text-secondary); }
.soldout-list { display: flex; flex-direction: column; gap: 8px; }
.so-item { display: flex; justify-content: space-between; font-size: 13px; padding: 6px 0; border-bottom: 1px solid var(--color-border); }
.so-item-name { color: var(--color-text-primary); }
.so-item-price { color: #C0392B; font-weight: 600; }

.revenue-list { display: flex; flex-direction: column; gap: 12px; }
.rev-item { display: grid; grid-template-columns: 80px 1fr 80px; align-items: center; gap: 10px; }
.rev-name { font-size: 13px; color: var(--color-text-primary); }
.rev-bar-wrap { height: 10px; background: rgba(45,74,62,0.06); border-radius: 5px; overflow: hidden; }
.rev-bar { height: 100%; border-radius: 5px; transition: width 0.6s ease; }
.rev-value { font-size: 13px; font-weight: 600; color: var(--color-text-primary); text-align: right; }

.price-impact { display: flex; flex-direction: column; gap: 10px; }
.pi-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--color-border); }
.pi-name { font-size: 13px; color: var(--color-text-primary); }
.pi-change { font-size: 13px; font-weight: 700; }
.pi-change.up { color: #C0392B; }
.pi-change.down { color: #4A7C59; }

@media (max-width: 1200px) { .quick-access { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .quick-access { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}
</style>
