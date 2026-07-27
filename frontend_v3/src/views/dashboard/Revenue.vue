<template>
  <div class="revenue-dashboard">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">营收数据看板</h1>
        <p class="page-subtitle">Revenue Dashboard · 分渠道收入分析</p>
      </div>
      <div class="header-right">
        <div class="date-range">
          <span class="range-btn" :class="{ active: dateRange === 'today' }" @click="dateRange = 'today'">今日</span>
          <span class="range-btn" :class="{ active: dateRange === 'week' }" @click="dateRange = 'week'">本周</span>
          <span class="range-btn" :class="{ active: dateRange === 'month' }" @click="dateRange = 'month'">本月</span>
        </div>
        <div class="store-selector">
          <select v-model="selectedStore" class="selector-dropdown">
            <option value="all">全部门店</option>
            <option value="ningguo">宁国店</option>
            <option value="xuancheng">宣城店</option>
            <option value="hangzhou">杭州店</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 总览卡片 -->
    <div class="overview-section">
      <div class="overview-card">
        <div class="overview-icon revenue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="overview-info">
          <div class="overview-label">营收总额</div>
          <div class="overview-value">¥{{ formatNumber(totalRevenue) }}</div>
          <div class="overview-trend up">+{{ revenueTrend }}%</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon orders">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
        </div>
        <div class="overview-info">
          <div class="overview-label">订单总数</div>
          <div class="overview-value">{{ totalOrders }}</div>
          <div class="overview-trend up">+{{ orderTrend }}%</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon avg">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="overview-info">
          <div class="overview-label">客单价</div>
          <div class="overview-value">¥{{ avgOrderValue }}</div>
          <div class="overview-trend" :class="avgTrend > 0 ? 'up' : 'down'">
            {{ avgTrend > 0 ? '+' : '' }}{{ avgTrend }}%
          </div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon turnover">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <line x1="3" y1="9" x2="21" y2="9"/>
            <line x1="9" y1="21" x2="9" y2="9"/>
          </svg>
        </div>
        <div class="overview-info">
          <div class="overview-label">翻台率</div>
          <div class="overview-value">{{ turnoverRate }}%</div>
          <div class="overview-trend" :class="turnoverTrend > 0 ? 'up' : 'down'">
            {{ turnoverTrend > 0 ? '+' : '' }}{{ turnoverTrend }}%
          </div>
        </div>
      </div>
    </div>

    <!-- 渠道收入拆分 -->
    <div class="split-section">
      <div class="section-header">
        <h2 class="section-title">渠道收入拆分</h2>
      </div>
      <div class="split-grid">
        <div class="split-card dine-in">
          <div class="split-header">
            <div class="split-icon">堂食</div>
            <div class="split-percent">{{ channelSplit.dineInPercent }}%</div>
          </div>
          <div class="split-value">¥{{ formatNumber(channelSplit.dineIn) }}</div>
          <div class="split-trend up">+{{ channelSplit.dineInTrend }}%</div>
          <div class="split-bar">
            <div class="bar-fill" :style="{ width: channelSplit.dineInPercent + '%' }"></div>
          </div>
        </div>
        <div class="split-card takeout">
          <div class="split-header">
            <div class="split-icon">外卖</div>
            <div class="split-percent">{{ channelSplit.takeoutPercent }}%</div>
          </div>
          <div class="split-value">¥{{ formatNumber(channelSplit.takeout) }}</div>
          <div class="split-trend up">+{{ channelSplit.takeoutTrend }}%</div>
          <div class="split-bar">
            <div class="bar-fill" :style="{ width: channelSplit.takeoutPercent + '%' }"></div>
          </div>
        </div>
        <div class="split-card group-buy">
          <div class="split-header">
            <div class="split-icon">团购</div>
            <div class="split-percent">{{ channelSplit.groupBuyPercent }}%</div>
          </div>
          <div class="split-value">¥{{ formatNumber(channelSplit.groupBuy) }}</div>
          <div class="split-trend down">-{{ channelSplit.groupBuyTrend }}%</div>
          <div class="split-bar">
            <div class="bar-fill" :style="{ width: channelSplit.groupBuyPercent + '%' }"></div>
          </div>
        </div>
        <div class="split-card banquet">
          <div class="split-header">
            <div class="split-icon">宴席</div>
            <div class="split-percent">{{ channelSplit.banquetPercent }}%</div>
          </div>
          <div class="split-value">¥{{ formatNumber(channelSplit.banquet) }}</div>
          <div class="split-trend up">+{{ channelSplit.banquetTrend }}%</div>
          <div class="split-bar">
            <div class="bar-fill" :style="{ width: channelSplit.banquetPercent + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 双栏：门店对比 + 趋势图 -->
    <div class="mid-section">
      <!-- 门店收入对比 -->
      <div class="store-compare">
        <div class="section-header">
          <h2 class="section-title">门店收入对比</h2>
        </div>
        <div class="compare-list">
          <div class="compare-item" v-for="store in storeCompare" :key="store.name">
            <div class="compare-rank">{{ store.rank }}</div>
            <div class="compare-info">
              <div class="compare-name">{{ store.name }}</div>
              <div class="compare-desc">{{ store.orders }}笔订单 · 客单价¥{{ store.avgValue }}</div>
            </div>
            <div class="compare-bar-wrap">
              <div class="compare-bar" :style="{ width: (store.revenue / maxStoreRevenue * 100) + '%', background: store.color }"></div>
            </div>
            <div class="compare-value">¥{{ formatNumber(store.revenue) }}</div>
            <div class="compare-trend" :class="store.trend > 0 ? 'up' : 'down'">
              {{ store.trend > 0 ? '+' : '' }}{{ store.trend }}%
            </div>
          </div>
        </div>
      </div>

      <!-- 收入趋势图 -->
      <div class="trend-chart">
        <div class="section-header">
          <h2 class="section-title">收入趋势</h2>
          <div class="chart-tabs">
            <button :class="['chart-tab', { active: trendType === 'daily' }]" @click="trendType = 'daily'">日趋势</button>
            <button :class="['chart-tab', { active: trendType === 'channel' }]" @click="trendType = 'channel'">渠道对比</button>
          </div>
        </div>
        <div class="chart-container">
          <div class="chart-y-axis">
            <span>¥40万</span>
            <span>¥30万</span>
            <span>¥20万</span>
            <span>¥10万</span>
            <span>0</span>
          </div>
          <div class="chart-area">
            <div class="chart-grid">
              <div class="grid-line" v-for="i in 4" :key="i"></div>
            </div>
            <div class="chart-bars" v-if="trendType === 'daily'">
              <div v-for="(day, i) in dailyTrend" :key="i" class="bar-group">
                <div class="bar-item">
                  <div class="bar-fill" :style="{ height: (day.revenue / 400000 * 100) + '%' }"></div>
                </div>
                <div class="bar-label">{{ day.label }}</div>
                <div class="bar-value">¥{{ formatNumber(day.revenue) }}</div>
              </div>
            </div>
            <div class="chart-bars" v-else>
              <div v-for="(item, i) in channelTrend" :key="i" class="bar-group stacked">
                <div class="bar-item" v-for="(channel, j) in item.channels" :key="j">
                  <div class="bar-fill" :style="{ height: (channel.value / 400000 * 100) + '%', background: channel.color }"></div>
                </div>
                <div class="bar-label">{{ item.label }}</div>
                <div class="bar-value">¥{{ formatNumber(item.total) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="chart-legend" v-if="trendType === 'channel'">
          <div class="legend-item" v-for="(item, i) in channelLegend" :key="i">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <span class="legend-text">{{ item.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 佣金统计 + 热销菜品 -->
    <div class="bottom-section">
      <!-- 渠道佣金统计 -->
      <div class="commission-section">
        <div class="section-header">
          <h2 class="section-title">渠道佣金统计</h2>
        </div>
        <div class="commission-table">
          <table>
            <thead>
              <tr>
                <th>渠道名称</th>
                <th>订单数</th>
                <th>佣金比例</th>
                <th>佣金金额</th>
                <th>佣金占比</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in commissionData" :key="item.channel">
                <td>{{ item.channel }}</td>
                <td>{{ item.orders }}</td>
                <td>{{ item.rate }}%</td>
                <td class="amount">¥{{ formatNumber(item.amount) }}</td>
                <td>
                  <div class="commission-bar">
                    <div class="bar-fill" :style="{ width: item.percent + '%' }"></div>
                  </div>
                  <span class="percent-text">{{ item.percent }}%</span>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td><strong>合计</strong></td>
                <td><strong>{{ totalCommissionOrders }}</strong></td>
                <td><strong>-</strong></td>
                <td class="amount"><strong>¥{{ formatNumber(totalCommissionAmount) }}</strong></td>
                <td><strong>100%</strong></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <!-- 热销菜品 TOP10 -->
      <div class="top-dishes">
        <div class="section-header">
          <h2 class="section-title">热销菜品 TOP10</h2>
        </div>
        <div class="dish-list">
          <div class="dish-item" v-for="(dish, i) in topDishes" :key="i">
            <div class="dish-rank" :class="{ top3: i < 3 }">{{ i + 1 }}</div>
            <div class="dish-info">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-category">{{ dish.category }}</div>
            </div>
            <div class="dish-sales">
              <div class="dish-label">销量</div>
              <div class="dish-value">{{ dish.sales }}份</div>
            </div>
            <div class="dish-revenue">
              <div class="dish-label">营收</div>
              <div class="dish-value">¥{{ formatNumber(dish.revenue) }}</div>
            </div>
            <div class="dish-margin">
              <div class="dish-label">毛利</div>
              <div class="dish-value">{{ dish.margin }}%</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const dateRange = ref('today')
const selectedStore = ref('all')
const trendType = ref('daily')

function formatNumber(num) {
  return num.toLocaleString('zh-CN')
}

// 总览数据
const totalRevenue = ref(486800)
const totalOrders = ref(1256)
const avgOrderValue = ref(387)
const turnoverRate = ref(72)
const revenueTrend = ref(12.5)
const orderTrend = ref(8.3)
const avgTrend = ref(4.2)
const turnoverTrend = ref(3.2)

// 渠道拆分
const channelSplit = ref({
  dineIn: 285000,
  dineInPercent: 58.5,
  dineInTrend: 10.2,
  takeout: 98000,
  takeoutPercent: 20.1,
  takeoutTrend: 25.8,
  groupBuy: 45800,
  groupBuyPercent: 9.4,
  groupBuyTrend: 3.5,
  banquet: 58000,
  banquetPercent: 12.0,
  banquetTrend: 18.6
})

// 门店对比
const storeCompare = ref([
  { name: '宁国店', revenue: 224000, orders: 582, avgValue: 385, trend: 15.2, rank: 1, color: '#2D4A3E' },
  { name: '宣城店', revenue: 142800, orders: 372, avgValue: 384, trend: 8.3, rank: 2, color: '#4A7C59' },
  { name: '杭州店', revenue: 120000, orders: 302, avgValue: 397, trend: 12.1, rank: 3, color: '#5B7B8A' },
])

const maxStoreRevenue = computed(() => Math.max(...storeCompare.value.map(s => s.revenue)))

// 日趋势
const dailyTrend = ref([
  { label: '周一', revenue: 32000 },
  { label: '周二', revenue: 28000 },
  { label: '周三', revenue: 35000 },
  { label: '周四', revenue: 31000 },
  { label: '周五', revenue: 48000 },
  { label: '周六', revenue: 68000 },
  { label: '周日', revenue: 58000 },
])

// 渠道对比趋势
const channelLegend = ref([
  { name: '堂食', color: '#2D4A3E' },
  { name: '外卖', color: '#4A7C59' },
  { name: '团购', color: '#C4A35A' },
  { name: '宴席', color: '#5B7B8A' },
])

const channelTrend = ref([
  { label: '周一', total: 32000, channels: [{ value: 18000, color: '#2D4A3E' }, { value: 8000, color: '#4A7C59' }, { value: 3500, color: '#C4A35A' }, { value: 2500, color: '#5B7B8A' }] },
  { label: '周二', total: 28000, channels: [{ value: 16000, color: '#2D4A3E' }, { value: 7000, color: '#4A7C59' }, { value: 3000, color: '#C4A35A' }, { value: 2000, color: '#5B7B8A' }] },
  { label: '周三', total: 35000, channels: [{ value: 20000, color: '#2D4A3E' }, { value: 8500, color: '#4A7C59' }, { value: 4000, color: '#C4A35A' }, { value: 2500, color: '#5B7B8A' }] },
  { label: '周四', total: 31000, channels: [{ value: 17500, color: '#2D4A3E' }, { value: 8000, color: '#4A7C59' }, { value: 3500, color: '#C4A35A' }, { value: 2000, color: '#5B7B8A' }] },
  { label: '周五', total: 48000, channels: [{ value: 26000, color: '#2D4A3E' }, { value: 12000, color: '#4A7C59' }, { value: 6000, color: '#C4A35A' }, { value: 4000, color: '#5B7B8A' }] },
  { label: '周六', total: 68000, channels: [{ value: 35000, color: '#2D4A3E' }, { value: 18000, color: '#4A7C59' }, { value: 8000, color: '#C4A35A' }, { value: 7000, color: '#5B7B8A' }] },
  { label: '周日', total: 58000, channels: [{ value: 32000, color: '#2D4A3E' }, { value: 15000, color: '#4A7C59' }, { value: 6000, color: '#C4A35A' }, { value: 5000, color: '#5B7B8A' }] },
])

// 佣金统计
const commissionData = ref([
  { channel: '美团外卖', orders: 286, rate: 18, amount: 17640, percent: 42.1 },
  { channel: '饿了么', orders: 215, rate: 17, amount: 11285, percent: 27.0 },
  { channel: '抖音团购', orders: 156, rate: 20, amount: 9160, percent: 21.9 },
  { channel: '大众点评', orders: 89, rate: 15, amount: 3738, percent: 9.0 },
])

const totalCommissionOrders = computed(() => commissionData.value.reduce((sum, c) => sum + c.orders, 0))
const totalCommissionAmount = computed(() => commissionData.value.reduce((sum, c) => sum + c.amount, 0))

// 热销菜品
const topDishes = ref([
  { name: '红烧肉', category: '热菜', sales: 156, revenue: 4680, margin: 68 },
  { name: '清蒸鲈鱼', category: '海鲜', sales: 132, revenue: 5280, margin: 62 },
  { name: '蒜蓉西兰花', category: '素菜', sales: 128, revenue: 1920, margin: 75 },
  { name: '宫保鸡丁', category: '热菜', sales: 118, revenue: 3540, margin: 70 },
  { name: '麻婆豆腐', category: '热菜', sales: 108, revenue: 1620, margin: 78 },
  { name: '糖醋排骨', category: '热菜', sales: 96, revenue: 4320, margin: 65 },
  { name: '白灼虾', category: '海鲜', sales: 88, revenue: 4400, margin: 60 },
  { name: '北京烤鸭', category: '招牌', sales: 76, revenue: 6840, margin: 58 },
  { name: '干锅牛蛙', category: '热菜', sales: 72, revenue: 3240, margin: 66 },
  { name: '蚝油生菜', category: '素菜', sales: 68, revenue: 1360, margin: 72 },
])
</script>

<style scoped>
.revenue-dashboard {
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
  gap: 20px;
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

.selector-dropdown {
  padding: 8px 16px;
  border: 1px solid #e8edea;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}

/* 总览卡片 */
.overview-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  display: flex;
  align-items: center;
  gap: 16px;
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-icon.revenue {
  background: rgba(45,74,62,0.08);
  color: #2D4A3E;
}

.overview-icon.orders {
  background: rgba(74,124,89,0.08);
  color: #4A7C59;
}

.overview-icon.avg {
  background: rgba(196,163,90,0.08);
  color: #C4A35A;
}

.overview-icon.turnover {
  background: rgba(91,123,138,0.08);
  color: #5B7B8A;
}

.overview-icon svg {
  width: 24px;
  height: 24px;
}

.overview-info {
  flex: 1;
}

.overview-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.overview-value {
  font-size: 24px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.overview-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.overview-trend.up {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.overview-trend.down {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

/* 渠道拆分 */
.split-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.split-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.split-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  border-top: 4px solid #2D4A3E;
}

.split-card.dine-in {
  border-top-color: #2D4A3E;
}

.split-card.takeout {
  border-top-color: #4A7C59;
}

.split-card.group-buy {
  border-top-color: #C4A35A;
}

.split-card.banquet {
  border-top-color: #5B7B8A;
}

.split-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.split-icon {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.split-percent {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
}

.split-value {
  font-size: 24px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 8px;
}

.split-trend {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.split-trend.up {
  color: #67C23A;
}

.split-trend.down {
  color: #F56C6C;
}

.split-bar {
  height: 6px;
  background: #f0f2f1;
  border-radius: 3px;
  overflow: hidden;
}

.split-bar .bar-fill {
  height: 100%;
  background: #2D4A3E;
  border-radius: 3px;
  transition: width 0.5s;
}

.split-card.dine-in .bar-fill { background: #2D4A3E; }
.split-card.takeout .bar-fill { background: #4A7C59; }
.split-card.group-buy .bar-fill { background: #C4A35A; }
.split-card.banquet .bar-fill { background: #5B7B8A; }

/* 中间区域 */
.mid-section {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 24px;
  margin-bottom: 24px;
}

/* 门店对比 */
.store-compare {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.compare-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compare-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafbfb;
  border-radius: 12px;
}

.compare-rank {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #e8edea;
  color: #7a8c84;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.compare-rank:nth-child(1) {
  background: linear-gradient(135deg, #F5D98C, #C4A35A);
  color: #8B7030;
}

.compare-rank:nth-child(2) {
  background: linear-gradient(135deg, #E8EDEB, #C8C8C8);
  color: #666;
}

.compare-rank:nth-child(3) {
  background: linear-gradient(135deg, #E8C4A8, #C49A6C);
  color: #8B6030;
}

.compare-info {
  flex: 1;
  min-width: 0;
}

.compare-name {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 2px;
}

.compare-desc {
  font-size: 12px;
  color: #999;
}

.compare-bar-wrap {
  flex: 1;
  height: 8px;
  background: #f0f2f1;
  border-radius: 4px;
  overflow: hidden;
}

.compare-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s;
}

.compare-value {
  width: 100px;
  text-align: right;
  font-size: 14px;
  font-weight: 700;
  color: #2D4A3E;
}

.compare-trend {
  font-size: 12px;
  font-weight: 600;
}

.compare-trend.up { color: #67C23A; }
.compare-trend.down { color: #F56C6C; }

/* 趋势图 */
.trend-chart {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.chart-tabs {
  display: flex;
  gap: 8px;
}

.chart-tab {
  padding: 6px 14px;
  font-size: 13px;
  color: #7a8c84;
  background: #f5f7f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.chart-tab.active {
  background: #2D4A3E;
  color: #fff;
}

.chart-container {
  display: flex;
  height: 280px;
  margin-top: 16px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-right: 12px;
  font-size: 12px;
  color: #999;
  width: 60px;
  text-align: right;
}

.chart-area {
  flex: 1;
  position: relative;
}

.chart-grid {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-line {
  height: 1px;
  background: #f0f2f1;
}

.chart-bars {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  padding-bottom: 40px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 12%;
}

.bar-group.stacked {
  width: 12%;
}

.bar-item {
  width: 100%;
  display: flex;
  flex-direction: column-reverse;
  height: 200px;
}

.bar-fill {
  width: 100%;
  background: #2D4A3E;
  border-radius: 4px 4px 0 0;
  transition: height 0.5s;
}

.bar-label {
  font-size: 12px;
  color: #7a8c84;
}

.bar-value {
  font-size: 11px;
  color: #2D4A3E;
  font-weight: 600;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-text {
  font-size: 12px;
  color: #7a8c84;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 24px;
}

/* 佣金统计 */
.commission-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.commission-table {
  overflow-x: auto;
}

.commission-table table {
  width: 100%;
  border-collapse: collapse;
}

.commission-table th,
.commission-table td {
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.commission-table th {
  background: #fafbfb;
  color: #7a8c84;
  font-weight: 600;
}

.commission-table tbody tr {
  border-bottom: 1px solid #f0f2f1;
}

.commission-table tbody tr:hover {
  background: #fafbfb;
}

.commission-table .amount {
  font-weight: 700;
  color: #2D4A3E;
}

.commission-bar {
  height: 6px;
  background: #f0f2f1;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.commission-bar .bar-fill {
  height: 100%;
  background: #E6A23C;
  border-radius: 3px;
}

.percent-text {
  font-size: 12px;
  color: #7a8c84;
}

.commission-table tfoot {
  background: #f5f7f6;
}

/* 热销菜品 */
.top-dishes {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.dish-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dish-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #fafbfb;
  border-radius: 10px;
}

.dish-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #e8edea;
  color: #7a8c84;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-rank.top3:nth-child(1) {
  background: linear-gradient(135deg, #F5D98C, #C4A35A);
  color: #8B7030;
}

.dish-rank.top3:nth-child(2) {
  background: linear-gradient(135deg, #E8EDEB, #C8C8C8);
  color: #666;
}

.dish-rank.top3:nth-child(3) {
  background: linear-gradient(135deg, #E8C4A8, #C49A6C);
  color: #8B6030;
}

.dish-info {
  flex: 1;
}

.dish-name {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.dish-category {
  font-size: 12px;
  color: #999;
}

.dish-sales, .dish-revenue, .dish-margin {
  text-align: center;
  min-width: 80px;
}

.dish-label {
  font-size: 11px;
  color: #999;
  margin-bottom: 2px;
}

.dish-value {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

/* 响应式 */
@media (max-width: 1400px) {
  .overview-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .split-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .mid-section, .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-section {
    grid-template-columns: 1fr;
  }
  .split-grid {
    grid-template-columns: 1fr;
  }
}
</style>