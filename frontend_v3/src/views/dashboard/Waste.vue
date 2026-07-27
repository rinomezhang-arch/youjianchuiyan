<template>
  <div class="waste-dashboard">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">损耗报废看板</h1>
        <p class="page-subtitle">Waste Management · 后厨每日食材报废登记与损耗率统计</p>
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
        <button class="add-btn" @click="openAddModal">新增报废记录</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card total">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18"/>
            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">报废次数</div>
        </div>
        <div class="stat-trend up">+{{ stats.totalTrend }}%</div>
      </div>
      <div class="stat-card amount">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">¥{{ formatNumber(stats.totalAmount) }}</div>
          <div class="stat-label">报废金额</div>
        </div>
        <div class="stat-trend down">+{{ stats.amountTrend }}%</div>
      </div>
      <div class="stat-card rate">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.wasteRate }}%</div>
          <div class="stat-label">损耗率</div>
        </div>
        <div class="stat-trend" :class="stats.rateTrend > 0 ? 'down' : 'up'">
          {{ stats.rateTrend > 0 ? '+' : '' }}{{ stats.rateTrend }}%
        </div>
      </div>
      <div class="stat-card categories">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.categories }}</div>
          <div class="stat-label">报废品类</div>
        </div>
      </div>
    </div>

    <!-- 双栏：门店损耗对比 + 损耗趋势 -->
    <div class="mid-section">
      <!-- 门店损耗对比 -->
      <div class="store-compare">
        <div class="section-header">
          <h2 class="section-title">门店损耗对比</h2>
        </div>
        <div class="compare-list">
          <div class="compare-item" v-for="store in storeCompare" :key="store.name">
            <div class="compare-rank">{{ store.rank }}</div>
            <div class="compare-info">
              <div class="compare-name">{{ store.name }}</div>
              <div class="compare-desc">{{ store.count }}笔报废 · 损耗率{{ store.rate }}%</div>
            </div>
            <div class="compare-bar-wrap">
              <div class="compare-bar" :style="{ width: (store.amount / maxStoreAmount * 100) + '%', background: store.color }"></div>
            </div>
            <div class="compare-value">¥{{ formatNumber(store.amount) }}</div>
            <div class="compare-trend" :class="store.trend > 0 ? 'down' : 'up'">
              {{ store.trend > 0 ? '+' : '' }}{{ store.trend }}%
            </div>
          </div>
        </div>
      </div>

      <!-- 损耗趋势图 -->
      <div class="trend-chart">
        <div class="section-header">
          <h2 class="section-title">损耗趋势</h2>
          <div class="chart-tabs">
            <button :class="['chart-tab', { active: trendType === 'daily' }]" @click="trendType = 'daily'">日趋势</button>
            <button :class="['chart-tab', { active: trendType === 'category' }]" @click="trendType = 'category'">品类对比</button>
          </div>
        </div>
        <div class="chart-container">
          <div class="chart-y-axis">
            <span>¥10万</span>
            <span>¥8万</span>
            <span>¥6万</span>
            <span>¥4万</span>
            <span>¥2万</span>
            <span>0</span>
          </div>
          <div class="chart-area">
            <div class="chart-grid">
              <div class="grid-line" v-for="i in 5" :key="i"></div>
            </div>
            <div class="chart-bars" v-if="trendType === 'daily'">
              <div v-for="(day, i) in dailyTrend" :key="i" class="bar-group">
                <div class="bar-item">
                  <div class="bar-fill" :style="{ height: (day.amount / 100000 * 100) + '%' }"></div>
                </div>
                <div class="bar-label">{{ day.label }}</div>
                <div class="bar-value">¥{{ formatNumber(day.amount) }}</div>
              </div>
            </div>
            <div class="chart-bars" v-else>
              <div v-for="(item, i) in categoryTrend" :key="i" class="bar-group">
                <div class="bar-item">
                  <div class="bar-fill" :style="{ height: (item.amount / 100000 * 100) + '%', background: item.color }"></div>
                </div>
                <div class="bar-label">{{ item.label }}</div>
                <div class="bar-value">¥{{ formatNumber(item.amount) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="chart-legend" v-if="trendType === 'category'">
          <div class="legend-item" v-for="(item, i) in categoryLegend" :key="i">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <span class="legend-text">{{ item.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：报废记录列表 + 高损耗菜品 -->
    <div class="bottom-section">
      <!-- 报废记录列表 -->
      <div class="waste-list">
        <div class="section-header">
          <h2 class="section-title">报废记录</h2>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="list-table">
          <table>
            <thead>
              <tr>
                <th>门店</th>
                <th>菜品名称</th>
                <th>品类</th>
                <th>数量</th>
                <th>单价</th>
                <th>金额</th>
                <th>报废原因</th>
                <th>登记人</th>
                <th>登记时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in wasteRecords" :key="item.id">
                <td>{{ item.store }}</td>
                <td>{{ item.dish }}</td>
                <td><span class="category-tag" :class="item.category">{{ item.category }}</span></td>
                <td>{{ item.quantity }}{{ item.unit }}</td>
                <td>¥{{ item.unitPrice }}</td>
                <td class="amount">¥{{ item.amount }}</td>
                <td><span class="reason-tag" :class="item.reason">{{ item.reasonText }}</span></td>
                <td>{{ item.staff }}</td>
                <td>{{ item.time }}</td>
                <td>
                  <button class="action-btn view" @click="viewDetail(item)">详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 高损耗菜品 TOP10 -->
      <div class="high-waste-dishes">
        <div class="section-header">
          <h2 class="section-title">高损耗菜品 TOP10</h2>
        </div>
        <div class="dish-list">
          <div class="dish-item" v-for="(dish, i) in highWasteDishes" :key="i">
            <div class="dish-rank" :class="{ top3: i < 3 }">{{ i + 1 }}</div>
            <div class="dish-info">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-category">{{ dish.category }}</div>
            </div>
            <div class="dish-waste">
              <div class="dish-label">报废金额</div>
              <div class="dish-value">¥{{ formatNumber(dish.amount) }}</div>
            </div>
            <div class="dish-count">
              <div class="dish-label">报废次数</div>
              <div class="dish-value">{{ dish.count }}次</div>
            </div>
            <div class="dish-rate">
              <div class="dish-label">损耗率</div>
              <div class="dish-value" :class="dish.rate > 10 ? 'danger' : 'warning'">{{ dish.rate }}%</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 损耗原因分析 -->
    <div class="reason-section">
      <div class="section-header">
        <h2 class="section-title">损耗原因分析</h2>
      </div>
      <div class="reason-grid">
        <div class="reason-card" v-for="item in reasonAnalysis" :key="item.reason">
          <div class="reason-header">
            <div class="reason-icon" :class="item.reason">{{ item.icon }}</div>
            <div class="reason-name">{{ item.reasonText }}</div>
          </div>
          <div class="reason-value">¥{{ formatNumber(item.amount) }}</div>
          <div class="reason-bar">
            <div class="bar-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
          </div>
          <div class="reason-detail">
            <span>{{ item.count }}笔记录</span>
            <span>{{ item.percent }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const dateRange = ref('today')
const selectedStore = ref('all')
const trendType = ref('daily')

function formatNumber(num) {
  return num.toLocaleString('zh-CN')
}

// 统计数据
const stats = ref({
  total: 48,
  totalTrend: 12.5,
  totalAmount: 28600,
  amountTrend: 8.3,
  wasteRate: 3.2,
  rateTrend: -0.5,
  categories: 12
})

// 门店对比
const storeCompare = ref([
  { name: '宁国店', amount: 12400, count: 18, rate: 3.5, trend: 5.2, rank: 1, color: '#F56C6C' },
  { name: '宣城店', amount: 8600, count: 14, rate: 2.8, trend: -3.1, rank: 2, color: '#E6A23C' },
  { name: '杭州店', amount: 7600, count: 16, rate: 3.1, trend: 2.8, rank: 3, color: '#5B7B8A' },
])

const maxStoreAmount = computed(() => Math.max(...storeCompare.value.map(s => s.amount)))

// 日趋势
const dailyTrend = ref([
  { label: '周一', amount: 3200 },
  { label: '周二', amount: 2800 },
  { label: '周三', amount: 3500 },
  { label: '周四', amount: 4100 },
  { label: '周五', amount: 4800 },
  { label: '周六', amount: 5600 },
  { label: '周日', amount: 4600 },
])

// 品类趋势
const categoryLegend = ref([
  { name: '蔬菜类', color: '#67C23A' },
  { name: '海鲜类', color: '#4A7C59' },
  { name: '肉类', color: '#F56C6C' },
  { name: '干货类', color: '#C4A35A' },
  { name: '其他', color: '#5B7B8A' },
])

const categoryTrend = ref([
  { label: '蔬菜', amount: 8500, color: '#67C23A' },
  { label: '海鲜', amount: 7200, color: '#4A7C59' },
  { label: '肉类', amount: 6800, color: '#F56C6C' },
  { label: '干货', amount: 3500, color: '#C4A35A' },
  { label: '其他', amount: 2600, color: '#5B7B8A' },
])

// 报废记录
const wasteRecords = ref([
  { id: 1, store: '宁国店', dish: '西兰花', category: 'vegetable', quantity: 5, unit: '斤', unitPrice: 8, amount: 40, reason: 'expired', reasonText: '过期', staff: '王厨', time: '2026-07-27 10:30' },
  { id: 2, store: '宁国店', dish: '鲈鱼', category: 'seafood', quantity: 2, unit: '条', unitPrice: 85, amount: 170, reason: 'spoiled', reasonText: '变质', staff: '王厨', time: '2026-07-27 10:35' },
  { id: 3, store: '宣城店', dish: '五花肉', category: 'meat', quantity: 3, unit: '斤', unitPrice: 28, amount: 84, reason: 'expired', reasonText: '过期', staff: '李厨', time: '2026-07-27 09:45' },
  { id: 4, store: '杭州店', dish: '大虾', category: 'seafood', quantity: 2, unit: '斤', unitPrice: 68, amount: 136, reason: 'spoiled', reasonText: '变质', staff: '赵厨', time: '2026-07-27 11:00' },
  { id: 5, store: '宁国店', dish: '豆腐', category: 'vegetable', quantity: 10, unit: '块', unitPrice: 2, amount: 20, reason: 'spoiled', reasonText: '变质', staff: '王厨', time: '2026-07-27 11:15' },
  { id: 6, store: '宣城店', dish: '生姜', category: 'vegetable', quantity: 2, unit: '斤', unitPrice: 12, amount: 24, reason: 'mold', reasonText: '发霉', staff: '李厨', time: '2026-07-27 14:30' },
])

// 高损耗菜品
const highWasteDishes = ref([
  { name: '西兰花', category: '蔬菜', amount: 3200, count: 12, rate: 12.5 },
  { name: '鲈鱼', category: '海鲜', amount: 2800, count: 8, rate: 8.2 },
  { name: '大虾', category: '海鲜', amount: 2400, count: 10, rate: 9.8 },
  { name: '五花肉', category: '肉类', amount: 1800, count: 9, rate: 5.5 },
  { name: '豆腐', category: '蔬菜', amount: 1500, count: 15, rate: 15.2 },
  { name: '生姜', category: '蔬菜', amount: 1200, count: 8, rate: 6.8 },
  { name: '芹菜', category: '蔬菜', amount: 980, count: 6, rate: 4.5 },
  { name: '排骨', category: '肉类', amount: 860, count: 5, rate: 3.2 },
  { name: '香菇', category: '干货', amount: 720, count: 4, rate: 2.8 },
  { name: '鸡蛋', category: '其他', amount: 650, count: 7, rate: 4.1 },
])

// 损耗原因分析
const reasonAnalysis = ref([
  { reason: 'expired', reasonText: '过期', amount: 8600, count: 15, percent: 30.1, color: '#F56C6C', icon: '过' },
  { reason: 'spoiled', reasonText: '变质', amount: 7200, count: 12, percent: 25.2, color: '#E6A23C', icon: '变' },
  { reason: 'mold', reasonText: '发霉', amount: 5800, count: 10, percent: 20.3, color: '#C4A35A', icon: '霉' },
  { reason: 'damage', reasonText: '破损', amount: 3500, count: 6, percent: 12.2, color: '#5B7B8A', icon: '破' },
  { reason: 'other', reasonText: '其他', amount: 3500, count: 5, percent: 12.2, color: '#8B9A8C', icon: '其' },
])

function openAddModal() {
  ElMessage.info('新增报废记录功能')
}

function viewDetail(item) {
  ElMessage.info(`查看 ${item.dish} 报废详情`)
}
</script>

<style scoped>
.waste-dashboard {
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
  gap: 16px;
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

.add-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: #fff;
  background: #2D4A3E;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* 统计卡片 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card.total {
  border-left: 4px solid #F56C6C;
}

.stat-card.amount {
  border-left: 4px solid #E6A23C;
}

.stat-card.rate {
  border-left: 4px solid #5B7B8A;
}

.stat-card.categories {
  border-left: 4px solid #4A7C59;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.total .stat-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.stat-card.amount .stat-icon {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.stat-card.rate .stat-icon {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.stat-card.categories .stat-icon {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #999;
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

/* 中间区域 */
.mid-section {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 24px;
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
  width: 14%;
}

.bar-item {
  width: 100%;
  height: 200px;
}

.bar-fill {
  width: 100%;
  background: #F56C6C;
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
  grid-template-columns: 1.3fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

/* 报废记录 */
.waste-list {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.export-btn {
  padding: 6px 14px;
  font-size: 13px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.list-table {
  overflow-x: auto;
}

.list-table table {
  width: 100%;
  border-collapse: collapse;
}

.list-table th,
.list-table td {
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.list-table th {
  background: #fafbfb;
  color: #7a8c84;
  font-weight: 600;
}

.list-table tbody tr {
  border-bottom: 1px solid #f0f2f1;
}

.list-table tbody tr:hover {
  background: #fafbfb;
}

.amount {
  font-weight: 700;
  color: #F56C6C;
}

.category-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.category-tag.vegetable {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.category-tag.seafood {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.category-tag.meat {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.category-tag.dry {
  background: rgba(196,163,90,0.1);
  color: #C4A35A;
}

.reason-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.reason-tag.expired {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.reason-tag.spoiled {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.reason-tag.mold {
  background: rgba(196,163,90,0.1);
  color: #C4A35A;
}

.reason-tag.damage {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.reason-tag.other {
  background: rgba(139,154,140,0.1);
  color: #8B9A8C;
}

.action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.action-btn.view {
  background: #f5f7f6;
  color: #7a8c84;
}

/* 高损耗菜品 */
.high-waste-dishes {
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

.dish-waste, .dish-count, .dish-rate {
  text-align: center;
  min-width: 70px;
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

.dish-value.danger {
  color: #F56C6C;
}

.dish-value.warning {
  color: #E6A23C;
}

/* 损耗原因分析 */
.reason-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.reason-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.reason-card {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
}

.reason-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.reason-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.reason-icon.expired {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.reason-icon.spoiled {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.reason-icon.mold {
  background: rgba(196,163,90,0.1);
  color: #C4A35A;
}

.reason-icon.damage {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.reason-icon.other {
  background: rgba(139,154,140,0.1);
  color: #8B9A8C;
}

.reason-name {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.reason-value {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 12px;
}

.reason-bar {
  height: 6px;
  background: #e8edea;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s;
}

.reason-detail {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .reason-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .mid-section, .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }
  .reason-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>