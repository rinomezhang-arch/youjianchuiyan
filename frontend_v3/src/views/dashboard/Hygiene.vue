<template>
  <div class="hygiene-dashboard">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">卫生巡检看板</h1>
        <p class="page-subtitle">Hygiene Inspection · 每日前厅后厨打分与整改追踪</p>
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
        <button class="add-btn" @click="openAddModal">新增巡检记录</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card pass">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.pass }}</div>
          <div class="stat-label">合格</div>
        </div>
      </div>
      <div class="stat-card fail">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.fail }}</div>
          <div class="stat-label">不合格</div>
        </div>
      </div>
      <div class="stat-card pending">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.pending }}</div>
          <div class="stat-label">待整改</div>
        </div>
      </div>
      <div class="stat-card total">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">巡检总数</div>
        </div>
      </div>
    </div>

    <!-- 双栏：巡检概览 + 评分趋势 -->
    <div class="mid-section">
      <!-- 巡检概览 -->
      <div class="inspection-overview">
        <div class="section-header">
          <h2 class="section-title">巡检概览</h2>
        </div>
        <div class="overview-grid">
          <div class="overview-item" v-for="item in inspectionOverview" :key="item.id">
            <div class="overview-header">
              <div class="overview-title">{{ item.title }}</div>
              <div class="overview-score" :class="item.level">{{ item.score }}分</div>
            </div>
            <div class="overview-bar">
              <div class="bar-fill" :style="{ width: item.score + '%', background: item.color }"></div>
            </div>
            <div class="overview-detail">
              <span>{{ item.passCount }}项合格</span>
              <span>{{ item.failCount }}项不合格</span>
            </div>
            <div class="overview-footer">
              <span class="overview-store">{{ item.store }}</span>
              <span class="overview-time">{{ item.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 评分趋势 -->
      <div class="score-trend">
        <div class="section-header">
          <h2 class="section-title">评分趋势</h2>
          <div class="chart-tabs">
            <button :class="['chart-tab', { active: trendType === 'daily' }]" @click="trendType = 'daily'">日趋势</button>
            <button :class="['chart-tab', { active: trendType === 'store' }]" @click="trendType = 'store'">门店对比</button>
          </div>
        </div>
        <div class="chart-container">
          <div class="chart-y-axis">
            <span>100</span>
            <span>80</span>
            <span>60</span>
            <span>40</span>
            <span>0</span>
          </div>
          <div class="chart-area">
            <div class="chart-grid">
              <div class="grid-line" v-for="i in 4" :key="i"></div>
            </div>
            <div class="chart-bars" v-if="trendType === 'daily'">
              <div v-for="(day, i) in dailyScoreTrend" :key="i" class="bar-group">
                <div class="bar-item">
                  <div class="bar-fill" :style="{ height: day.score + '%', background: day.score >= 80 ? '#67C23A' : day.score >= 60 ? '#E6A23C' : '#F56C6C' }"></div>
                </div>
                <div class="bar-label">{{ day.label }}</div>
                <div class="bar-value">{{ day.score }}分</div>
              </div>
            </div>
            <div class="chart-bars" v-else>
              <div v-for="(item, i) in storeScoreTrend" :key="i" class="bar-group">
                <div class="bar-item">
                  <div class="bar-fill" :style="{ height: item.score + '%', background: item.color }"></div>
                </div>
                <div class="bar-label">{{ item.label }}</div>
                <div class="bar-value">{{ item.score }}分</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 消毒台账 + 整改追踪 -->
    <div class="bottom-section">
      <!-- 消毒台账 -->
      <div class="disinfection-log">
        <div class="section-header">
          <h2 class="section-title">消毒台账</h2>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="log-table">
          <table>
            <thead>
              <tr>
                <th>门店</th>
                <th>消毒区域</th>
                <th>消毒方式</th>
                <th>消毒时间</th>
                <th>责任人</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in disinfectionLogs" :key="item.id">
                <td>{{ item.store }}</td>
                <td>{{ item.area }}</td>
                <td>{{ item.method }}</td>
                <td>{{ item.time }}</td>
                <td>{{ item.staff }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 整改追踪 -->
      <div class="rectification-tracking">
        <div class="section-header">
          <h2 class="section-title">整改追踪</h2>
          <button class="add-btn" @click="openRectificationModal">新建整改</button>
        </div>
        <div class="tracking-list">
          <div class="tracking-item" v-for="item in rectificationItems" :key="item.id">
            <div class="tracking-header">
              <div class="tracking-priority" :class="item.priority">{{ item.priorityText }}</div>
              <div class="tracking-status" :class="item.status">{{ item.statusText }}</div>
            </div>
            <div class="tracking-title">{{ item.title }}</div>
            <div class="tracking-detail">
              <span>{{ item.store }}</span>
              <span>{{ item.location }}</span>
              <span>{{ item.inspector }}</span>
            </div>
            <div class="tracking-desc">{{ item.description }}</div>
            <div class="tracking-footer">
              <span class="tracking-date">发现时间: {{ item.date }}</span>
              <span class="tracking-deadline">整改截止: {{ item.deadline }}</span>
            </div>
            <div class="tracking-actions">
              <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleRectify(item)">确认整改</button>
              <button v-if="item.status === 'rectifying'" class="action-btn verify" @click="handleVerify(item)">验收通过</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 留样记录 -->
    <div class="sample-section">
      <div class="section-header">
        <h2 class="section-title">留样记录</h2>
        <button class="add-btn" @click="openSampleModal">新增留样</button>
      </div>
      <div class="sample-grid">
        <div class="sample-card" v-for="item in sampleRecords" :key="item.id">
          <div class="sample-header">
            <div class="sample-dish">{{ item.dish }}</div>
            <div class="sample-status" :class="item.status">{{ item.statusText }}</div>
          </div>
          <div class="sample-info">
            <div class="info-row">
              <span class="info-label">门店</span>
              <span class="info-value">{{ item.store }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">留样时间</span>
              <span class="info-value">{{ item.time }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">留样人员</span>
              <span class="info-value">{{ item.staff }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">留样量</span>
              <span class="info-value">{{ item.amount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">留样期限</span>
              <span class="info-value">{{ item.expiry }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const dateRange = ref('today')
const selectedStore = ref('all')
const trendType = ref('daily')

// 统计数据
const stats = ref({
  pass: 156,
  fail: 8,
  pending: 5,
  total: 169
})

// 巡检概览
const inspectionOverview = ref([
  { id: 1, title: '前厅区域', score: 92, passCount: 18, failCount: 2, store: '宁国店', time: '09:30', level: 'excellent', color: '#67C23A' },
  { id: 2, title: '后厨区域', score: 85, passCount: 17, failCount: 3, store: '宁国店', time: '09:35', level: 'good', color: '#E6A23C' },
  { id: 3, title: '前厅区域', score: 78, passCount: 15, failCount: 5, store: '宣城店', time: '09:20', level: 'warning', color: '#E6A23C' },
  { id: 4, title: '后厨区域', score: 95, passCount: 19, failCount: 1, store: '杭州店', time: '09:40', level: 'excellent', color: '#67C23A' },
])

// 日评分趋势
const dailyScoreTrend = ref([
  { label: '周一', score: 88 },
  { label: '周二', score: 92 },
  { label: '周三', score: 85 },
  { label: '周四', score: 89 },
  { label: '周五', score: 91 },
  { label: '周六', score: 87 },
  { label: '周日', score: 90 },
])

// 门店评分趋势
const storeScoreTrend = ref([
  { label: '宁国店', score: 92, color: '#2D4A3E' },
  { label: '宣城店', score: 85, color: '#4A7C59' },
  { label: '杭州店', score: 88, color: '#5B7B8A' },
])

// 消毒台账
const disinfectionLogs = ref([
  { id: 1, store: '宁国店', area: '前厅大厅', method: '紫外线消毒', time: '09:00', staff: '张保洁', status: 'done', statusText: '已完成' },
  { id: 2, store: '宁国店', area: '后厨操作区', method: '消毒液喷洒', time: '09:10', staff: '李厨', status: 'done', statusText: '已完成' },
  { id: 3, store: '宁国店', area: '餐具消毒间', method: '高温消毒', time: '09:20', staff: '王阿姨', status: 'done', statusText: '已完成' },
  { id: 4, store: '宣城店', area: '前厅大厅', method: '紫外线消毒', time: '08:50', staff: '陈保洁', status: 'done', statusText: '已完成' },
  { id: 5, store: '杭州店', area: '后厨操作区', method: '消毒液喷洒', time: '09:05', staff: '赵厨', status: 'pending', statusText: '进行中' },
])

// 整改追踪
const rectificationItems = ref([
  { 
    id: 1, 
    title: '后厨地面油污未清理', 
    store: '宁国店', 
    location: '后厨1号区域', 
    inspector: '王经理', 
    description: '后厨地面有明显油污，存在安全隐患，需立即清理', 
    date: '2026-07-27 09:30', 
    deadline: '2026-07-27 12:00', 
    priority: 'high', 
    priorityText: '紧急', 
    status: 'rectifying', 
    statusText: '整改中' 
  },
  { 
    id: 2, 
    title: '餐具摆放不整齐', 
    store: '宣城店', 
    location: '前厅餐具柜', 
    inspector: '李主管', 
    description: '餐具柜内餐具摆放杂乱，影响工作效率', 
    date: '2026-07-27 09:20', 
    deadline: '2026-07-27 17:00', 
    priority: 'medium', 
    priorityText: '一般', 
    status: 'pending', 
    statusText: '待整改' 
  },
  { 
    id: 3, 
    title: '垃圾桶未加盖', 
    store: '杭州店', 
    location: '后厨垃圾区', 
    inspector: '张经理', 
    description: '后厨垃圾桶未加盖，容易滋生蚊虫', 
    date: '2026-07-27 09:40', 
    deadline: '2026-07-27 11:00', 
    priority: 'high', 
    priorityText: '紧急', 
    status: 'pending', 
    statusText: '待整改' 
  },
])

// 留样记录
const sampleRecords = ref([
  { id: 1, dish: '红烧肉', store: '宁国店', time: '2026-07-27 13:00', staff: '王厨', amount: '100g', expiry: '2026-07-28 13:00', status: 'valid', statusText: '有效' },
  { id: 2, dish: '清蒸鲈鱼', store: '宁国店', time: '2026-07-27 13:00', staff: '王厨', amount: '100g', expiry: '2026-07-28 13:00', status: 'valid', statusText: '有效' },
  { id: 3, dish: '蒜蓉西兰花', store: '宁国店', time: '2026-07-27 13:00', staff: '王厨', amount: '100g', expiry: '2026-07-28 13:00', status: 'valid', statusText: '有效' },
  { id: 4, dish: '宫保鸡丁', store: '宣城店', time: '2026-07-27 13:00', staff: '李厨', amount: '100g', expiry: '2026-07-28 13:00', status: 'valid', statusText: '有效' },
  { id: 5, dish: '麻婆豆腐', store: '杭州店', time: '2026-07-27 13:00', staff: '赵厨', amount: '100g', expiry: '2026-07-28 13:00', status: 'valid', statusText: '有效' },
])

function openAddModal() {
  ElMessage.info('新增巡检记录功能')
}

function openRectificationModal() {
  ElMessage.info('新建整改功能')
}

function openSampleModal() {
  ElMessage.info('新增留样功能')
}

function handleRectify(item) {
  item.status = 'rectifying'
  item.statusText = '整改中'
  ElMessage.success('已开始整改')
}

function handleVerify(item) {
  item.status = 'completed'
  item.statusText = '已完成'
  ElMessage.success('整改验收通过')
}
</script>

<style scoped>
.hygiene-dashboard {
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

.stat-card.pass {
  border-left: 4px solid #67C23A;
}

.stat-card.fail {
  border-left: 4px solid #F56C6C;
}

.stat-card.pending {
  border-left: 4px solid #E6A23C;
}

.stat-card.total {
  border-left: 4px solid #5B7B8A;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.pass .stat-icon {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.stat-card.fail .stat-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.stat-card.pending .stat-icon {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.stat-card.total .stat-icon {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
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
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* 中间区域 */
.mid-section {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
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

/* 巡检概览 */
.inspection-overview {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.overview-item {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.overview-title {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.overview-score {
  font-size: 20px;
  font-weight: 700;
}

.overview-score.excellent {
  color: #67C23A;
}

.overview-score.good {
  color: #E6A23C;
}

.overview-score.warning {
  color: #F56C6C;
}

.overview-bar {
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

.overview-detail {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.overview-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #7a8c84;
}

/* 评分趋势 */
.score-trend {
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
  height: 240px;
  margin-top: 16px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-right: 12px;
  font-size: 12px;
  color: #999;
  width: 40px;
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
  padding-bottom: 36px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 12%;
}

.bar-item {
  width: 100%;
  height: 160px;
}

.bar-fill {
  width: 100%;
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

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 24px;
  margin-bottom: 24px;
}

/* 消毒台账 */
.disinfection-log {
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

.log-table {
  overflow-x: auto;
}

.log-table table {
  width: 100%;
  border-collapse: collapse;
}

.log-table th,
.log-table td {
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.log-table th {
  background: #fafbfb;
  color: #7a8c84;
  font-weight: 600;
}

.log-table tbody tr {
  border-bottom: 1px solid #f0f2f1;
}

.status-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.done {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.status-tag.pending {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

/* 整改追踪 */
.rectification-tracking {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.tracking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tracking-item {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
  border-left: 4px solid #E6A23C;
}

.tracking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tracking-priority {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.tracking-priority.high {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.tracking-priority.medium {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.tracking-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.tracking-status.pending {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.tracking-status.rectifying {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.tracking-status.completed {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.tracking-title {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 8px;
}

.tracking-detail {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.tracking-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}

.tracking-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #7a8c84;
  margin-bottom: 12px;
}

.tracking-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.action-btn {
  padding: 6px 14px;
  font-size: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.action-btn.approve {
  background: #2D4A3E;
  color: #fff;
}

.action-btn.verify {
  background: #67C23A;
  color: #fff;
}

/* 留样记录 */
.sample-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.sample-card {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
}

.sample-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sample-dish {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.sample-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.sample-status.valid {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.sample-status.expired {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.sample-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.info-label {
  color: #999;
}

.info-value {
  color: #666;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .sample-grid {
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
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .sample-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>