<template>
  <div class="table-util-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">桌台利用率 · Table Utilization</h2>
        <p class="page-subtitle">Real-time occupancy, turnover rate and revenue per table analysis</p>
      </div>
      <div class="header-actions">
        <div class="period-selector">
          <button :class="{ active: period === 'today' }" @click="period = 'today'">今日</button>
          <button :class="{ active: period === 'week' }" @click="period = 'week'">本周</button>
          <button :class="{ active: period === 'month' }" @click="period = 'month'">本月</button>
        </div>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">综合利用率 · Overall</div>
          <div class="stat-value" style="color:#2D4A3E">{{ stats.overallRate }}%</div>
          <div class="stat-sub"><span :class="stats.rateTrend > 0 ? 'trend-up' : 'trend-down'">{{ stats.rateTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.rateTrend) }}%</span> 较昨日</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">翻台率 · Turnover</div>
          <div class="stat-value" style="color:#D4A853">{{ stats.turnoverRate }}</div>
          <div class="stat-sub">平均每桌 {{ stats.turnoverRate }} 轮</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">桌均营收 · Rev/Table</div>
          <div class="stat-value" style="color:#5B7B8A">¥{{ (stats.revPerTable || 0).toLocaleString() }}</div>
          <div class="stat-sub"><span :class="stats.revTrend > 0 ? 'trend-up' : 'trend-down'">{{ stats.revTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.revTrend) }}%</span> 较昨日</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">平均用餐时长 · Avg Duration</div>
          <div class="stat-value" style="color:#4A7C59">{{ stats.avgDuration }}<span class="unit">min</span></div>
          <div class="stat-sub">午餐 {{ stats.lunchDuration }}min · 晚餐 {{ stats.dinnerDuration }}min</div>
        </div>
      </div>
    </div>

    <!-- 实时桌台状态图 -->
    <div class="table-map-card">
      <div class="card-header">
        <h3 class="section-title">实时桌台状态 · Live Table Status</h3>
        <div class="legend">
          <span class="legend-item"><span class="legend-dot available"></span> 空闲</span>
          <span class="legend-item"><span class="legend-dot occupied"></span> 用餐中</span>
          <span class="legend-item"><span class="legend-dot reserved"></span> 已预订</span>
          <span class="legend-item"><span class="legend-dot cleaning"></span> 清洁中</span>
        </div>
      </div>
      <div class="table-map">
        <!-- 大厅 -->
        <div class="table-zone">
          <div class="zone-title">大厅散台 · Hall</div>
          <div v-if="!hallTables.length" class="chart-empty">暂无桌台</div>
          <div v-else class="table-grid hall-grid">
            <div v-for="t in hallTables" :key="t.id" class="table-cell" :class="t.status" @click="showTableDetail(t)">
              <div class="table-number">{{ t.number }}</div>
              <div class="table-pax">{{ t.pax }}人桌</div>
              <div class="table-status-text">{{ statusText(t.status) }}</div>
              <div v-if="t.status === 'occupied'" class="table-timer">{{ t.duration }}min</div>
            </div>
          </div>
        </div>
        <!-- 包厢 -->
        <div class="table-zone">
          <div class="zone-title">包厢 · Private Rooms</div>
          <div v-if="!privateTables.length" class="chart-empty">暂无桌台</div>
          <div v-else class="table-grid private-grid">
            <div v-for="t in privateTables" :key="t.id" class="table-cell large" :class="t.status" @click="showTableDetail(t)">
              <div class="table-number">{{ t.name }}</div>
              <div class="table-pax">{{ t.pax }}人桌</div>
              <div class="table-status-text">{{ statusText(t.status) }}</div>
              <div v-if="t.status === 'occupied'" class="table-timer">{{ t.duration }}min</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <!-- 时段利用率曲线 - 高质量SVG -->
      <div class="chart-card wide">
        <div class="chart-header">
          <h3 class="section-title">时段利用率曲线 · Hourly Utilization</h3>
          <div class="chart-tabs">
            <button :class="{ active: utilView === 'rate' }" @click="utilView = 'rate'">利用率</button>
            <button :class="{ active: utilView === 'revenue' }" @click="utilView = 'revenue'">营收</button>
            <button :class="{ active: utilView === 'turnover' }" @click="utilView = 'turnover'">翻台</button>
          </div>
        </div>
        <div class="utilization-chart">
          <div v-if="!utilData.length" class="chart-empty">暂无数据</div>
          <svg v-else viewBox="0 0 800 240" class="util-svg">
            <defs>
              <linearGradient id="utilGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#2D4A3E" stop-opacity="0.35"/>
                <stop offset="60%" stop-color="#4A7C59" stop-opacity="0.1"/>
                <stop offset="100%" stop-color="#4A7C59" stop-opacity="0"/>
              </linearGradient>
              <linearGradient id="utilLineGrad" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="#2D4A3E"/>
                <stop offset="50%" stop-color="#4A7C59"/>
                <stop offset="100%" stop-color="#2D4A3E"/>
              </linearGradient>
              <filter id="utilShadow">
                <feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#2D4A3E" flood-opacity="0.15"/>
              </filter>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'h'+i" x1="60" :y1="30+(i-1)*40" x2="780" :y2="30+(i-1)*40" stroke="#e8ece9" stroke-width="1" stroke-dasharray="4 4"/>
            <!-- Y轴标签 -->
            <text v-for="i in 5" :key="'y'+i" x="52" :y="35+(i-1)*40" text-anchor="end" font-size="11" fill="#8a9a8e">
              {{ utilView === 'revenue' ? Math.round(maxUtil*(5-i)/4/1000)+'k' : Math.round(maxUtil*(5-i)/4)+'%' }}
            </text>
            <!-- 午餐区域 -->
            <rect x="140" y="25" width="180" height="175" fill="rgba(212,168,83,0.06)" rx="6"/>
            <text x="230" y="18" text-anchor="middle" font-size="11" fill="#C4A35A" font-weight="500">午餐时段</text>
            <!-- 晚餐区域 -->
            <rect x="460" y="25" width="220" height="175" fill="rgba(45,74,62,0.06)" rx="6"/>
            <text x="570" y="18" text-anchor="middle" font-size="11" fill="#2D4A3E" font-weight="500">晚餐时段</text>
            <!-- 面积 -->
            <path :d="utilAreaPath" fill="url(#utilGradient)"/>
            <!-- 折线 -->
            <path :d="utilLinePath" fill="none" stroke="url(#utilLineGrad)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#utilShadow)"/>
            <!-- 数据点 -->
            <circle v-for="(p,i) in utilData" :key="'p'+i" :cx="p.x" :cy="p.y" r="4" fill="#2D4A3E" stroke="#fff" stroke-width="2.5" class="util-dot"/>
            <!-- X轴标签 -->
            <text v-for="(p,i) in utilData" :key="'x'+i" :x="p.x" y="228" text-anchor="middle" font-size="10" fill="#8a9a8e">{{ p.label }}</text>
          </svg>
        </div>
      </div>

      <!-- 桌台类型对比 - 高质量SVG分组柱状图 -->
      <div class="chart-card">
        <h3 class="section-title">桌台类型对比 · Type Comparison</h3>
        <div v-if="!tableTypeComparison.length" class="chart-empty">暂无数据</div>
        <div v-else class="type-chart-container">
          <svg viewBox="0 0 400 260" class="type-svg">
            <defs>
              <linearGradient v-for="t in tableTypeComparison" :key="'tg'+t.type" :id="'typeGrad'+t.type.replace(/[^a-zA-Z0-9]/g,'')" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" :stop-color="t.color"/>
                <stop offset="100%" :stop-color="t.color" stop-opacity="0.6"/>
              </linearGradient>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'tg'+i" x1="50" :y1="30+(i-1)*40" x2="380" :y2="30+(i-1)*40" stroke="#e8ece9" stroke-width="1"/>
            <text v-for="i in 5" :key="'ty'+i" x="45" :y="35+(i-1)*40" text-anchor="end" font-size="10" fill="#8a9a8e">{{ 100-(i-1)*25 }}%</text>
            <!-- 柱状图组 -->
            <g v-for="(t, tIdx) in tableTypeComparison" :key="'tbar'+tIdx">
              <!-- 利用率柱 -->
              <rect :x="70 + tIdx * 80" :y="190 - t.utilization * 1.6" width="20" :height="t.utilization * 1.6"
                :fill="`url(#typeGrad${t.type.replace(/[^a-zA-Z0-9]/g,'')})`" rx="3" class="type-bar"/>
              <!-- 翻台率柱 -->
              <rect :x="94 + tIdx * 80" :y="190 - (t.turnover / 5 * 100) * 1.6" width="20" :height="(t.turnover / 5 * 100) * 1.6"
                :fill="t.color" opacity="0.5" rx="3" class="type-bar"/>
              <!-- 数值 -->
              <text :x="80 + tIdx * 80" :y="185 - t.utilization * 1.6" text-anchor="middle" font-size="9" font-weight="600" fill="#3a4a3e">{{ t.utilization }}%</text>
              <text :x="104 + tIdx * 80" :y="185 - (t.turnover / 5 * 100) * 1.6" text-anchor="middle" font-size="9" font-weight="600" fill="#3a4a3e">{{ t.turnover }}</text>
              <!-- 标签 -->
              <text :x="92 + tIdx * 80" y="210" text-anchor="middle" font-size="11" font-weight="500" fill="#3a4a3e">{{ t.type }}</text>
              <text :x="92 + tIdx * 80" y="225" text-anchor="middle" font-size="10" fill="#8a9a8e">{{ t.count }}桌 · ¥{{ (t.revPerTable || 0).toLocaleString() }}</text>
            </g>
          </svg>
        </div>
        <div class="type-legend">
          <span class="legend-item"><span class="legend-dot" style="background:#2D4A3E"></span> 利用率</span>
          <span class="legend-item"><span class="legend-dot" style="background:#2D4A3E;opacity:0.5"></span> 翻台率</span>
        </div>
      </div>

      <!-- 桌台营收排行 - 高质量SVG横向柱状图 -->
      <div class="chart-card">
        <h3 class="section-title">桌台营收排行 · Revenue Ranking</h3>
        <div v-if="!tableRevenueRank.length" class="chart-empty">暂无数据</div>
        <div v-else class="rank-chart-container">
          <svg viewBox="0 0 400 280" class="rank-svg">
            <defs>
              <linearGradient v-for="(t, i) in tableRevenueRank" :key="'rg'+i" :id="'rankGrad'+i" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" :stop-color="t.color"/>
                <stop offset="100%" :stop-color="t.color" stop-opacity="0.5"/>
              </linearGradient>
            </defs>
            <g v-for="(t, i) in tableRevenueRank" :key="'rbar'+i">
              <!-- 排名 -->
              <text x="18" :y="30 + i * 45" text-anchor="middle" font-size="12" font-weight="700" :fill="i < 3 ? '#C4A35A' : '#8a9a8e'">{{ i + 1 }}</text>
              <!-- 桌台名 -->
              <text x="38" :y="25 + i * 45" font-size="12" font-weight="500" fill="#3a4a3e">{{ t.name }}</text>
              <text x="38" :y="38 + i * 45" font-size="9" fill="#8a9a8e">{{ t.type }} · {{ t.turns }}轮</text>
              <!-- 横向柱 -->
              <rect x="120" :y="18 + i * 45" :width="(t.revPercent || 0) * 2" height="22"
                :fill="`url(#rankGrad${i})`" rx="4" class="rank-bar"/>
              <!-- 金额 -->
              <text :x="130 + (t.revPercent || 0) * 2" :y="33 + i * 45" font-size="12" font-weight="700" :fill="t.color">¥{{ (t.revenue || 0).toLocaleString() }}</text>
            </g>
          </svg>
        </div>
      </div>

      <!-- 周利用率对比 - 高质量SVG双柱图 -->
      <div class="chart-card wide">
        <h3 class="section-title">本周利用率对比 · Weekly Comparison</h3>
        <div v-if="!weeklyData.length" class="chart-empty">暂无数据</div>
        <div v-else class="weekly-chart-svg">
          <svg viewBox="0 0 800 260" class="weekly-svg">
            <defs>
              <linearGradient id="weekLunchGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#C4A35A"/>
                <stop offset="100%" stop-color="#D4B36A" stop-opacity="0.7"/>
              </linearGradient>
              <linearGradient id="weekDinnerGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#2D4A3E"/>
                <stop offset="100%" stop-color="#4A7C59" stop-opacity="0.7"/>
              </linearGradient>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'wg'+i" x1="60" :y1="30+(i-1)*40" x2="760" :y2="30+(i-1)*40" stroke="#e8ece9" stroke-width="1" stroke-dasharray="4 4"/>
            <text v-for="i in 5" :key="'wy'+i" x="52" :y="35+(i-1)*40" text-anchor="end" font-size="11" fill="#8a9a8e">{{ 100-(i-1)*25 }}%</text>
            <!-- 柱组 -->
            <g v-for="(day, dIdx) in weeklyData" :key="'wd'+dIdx">
              <rect :x="100 + dIdx * 95" :y="190 - day.lunch * 1.6" width="30" :height="day.lunch * 1.6"
                fill="url(#weekLunchGrad)" rx="4" class="week-bar"/>
              <rect :x="136 + dIdx * 95" :y="190 - day.dinner * 1.6" width="30" :height="day.dinner * 1.6"
                fill="url(#weekDinnerGrad)" rx="4" class="week-bar"/>
              <!-- 数值 -->
              <text :x="115 + dIdx * 95" :y="185 - day.lunch * 1.6" text-anchor="middle" font-size="9" font-weight="600" fill="#C4A35A">{{ day.lunch }}%</text>
              <text :x="151 + dIdx * 95" :y="185 - day.dinner * 1.6" text-anchor="middle" font-size="9" font-weight="600" fill="#2D4A3E">{{ day.dinner }}%</text>
              <!-- 标签 -->
              <text :x="133 + dIdx * 95" y="210" text-anchor="middle" font-size="12" fill="#6a7a6e">{{ day.label }}</text>
            </g>
          </svg>
        </div>
        <div class="weekly-legend">
          <span class="legend-item"><span class="legend-dot lunch"></span> 午餐</span>
          <span class="legend-item"><span class="legend-dot dinner"></span> 晚餐</span>
        </div>
      </div>
    </div>

    <!-- 桌台明细表 -->
    <div class="detail-table-card">
      <div class="card-header">
        <h3 class="section-title">桌台明细 · Table Details</h3>
        <div class="card-actions">
          <el-select v-model="filterZone" placeholder="全部区域" clearable style="width:140px">
            <el-option label="大厅" value="hall" />
            <el-option label="包厢" value="private" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:140px">
            <el-option label="空闲" value="available" />
            <el-option label="用餐中" value="occupied" />
            <el-option label="已预订" value="reserved" />
            <el-option label="清洁中" value="cleaning" />
          </el-select>
        </div>
      </div>
      <el-table :data="filteredTables" border style="width: 100%">
        <el-table-column label="桌台" width="120">
          <template #default="scope">
            <span class="table-name-cell">{{ scope.row.name || scope.row.number + '号桌' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="zone" label="区域" width="90" />
        <el-table-column label="座位数" width="90">
          <template #default="scope">{{ scope.row.pax }}人</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <span :class="['status-badge', scope.row.status]">{{ statusText(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="今日轮次" width="100">
          <template #default="scope">{{ scope.row.turns }}轮</template>
        </el-table-column>
        <el-table-column label="当前客人" width="110">
          <template #default="scope">{{ scope.row.currentGuest || '-' }}</template>
        </el-table-column>
        <el-table-column label="用餐时长" width="110">
          <template #default="scope">{{ scope.row.duration ? scope.row.duration + 'min' : '-' }}</template>
        </el-table-column>
        <el-table-column label="今日营收" width="130">
          <template #default="scope">
            <span class="money-cell">¥{{ (scope.row.todayRevenue || 0).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="利用率" min-width="180">
          <template #default="scope">
            <div class="util-bar-cell">
              <div class="util-bar-track">
                <div class="util-bar-fill" :style="{ width: (scope.row.utilization || 0) + '%', background: scope.row.utilization >= 80 ? '#4A7C59' : scope.row.utilization >= 50 ? '#D4A853' : '#C0392B' }"></div>
              </div>
              <span class="util-bar-value">{{ scope.row.utilization || 0 }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 桌台详情弹窗 -->
    <div v-if="selectedTable" class="dialog-overlay" @click.self="selectedTable = null">
      <div class="dialog-box">
        <div class="dialog-header">
          <h3>{{ selectedTable.name || selectedTable.number + '号桌' }}</h3>
          <span :class="['status-badge', selectedTable.status]">{{ statusText(selectedTable.status) }}</span>
        </div>
        <div class="dialog-grid">
          <div class="dialog-item">
            <span class="dialog-label">区域</span>
            <span class="dialog-value">{{ selectedTable.zone }}</span>
          </div>
          <div class="dialog-item">
            <span class="dialog-label">座位数</span>
            <span class="dialog-value">{{ selectedTable.pax }}人</span>
          </div>
          <div class="dialog-item">
            <span class="dialog-label">今日轮次</span>
            <span class="dialog-value">{{ selectedTable.turns }}轮</span>
          </div>
          <div class="dialog-item">
            <span class="dialog-label">今日营收</span>
            <span class="dialog-value money">¥{{ (selectedTable.todayRevenue || 0).toLocaleString() }}</span>
          </div>
          <div class="dialog-item">
            <span class="dialog-label">利用率</span>
            <span class="dialog-value">{{ selectedTable.utilization || 0 }}%</span>
          </div>
          <div class="dialog-item">
            <span class="dialog-label">当前客人</span>
            <span class="dialog-value">{{ selectedTable.currentGuest || '-' }}</span>
          </div>
        </div>
        <div class="dialog-actions">
          <el-button @click="selectedTable = null">关闭</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const period = ref('today')
const utilView = ref('rate')
const filterZone = ref('')
const filterStatus = ref('')
const selectedTable = ref(null)

// 统计数据（空值，待后端接入）
const stats = ref({
  overallRate: 0, rateTrend: 0,
  turnoverRate: 0,
  revPerTable: 0, revTrend: 0,
  avgDuration: 0, lunchDuration: 0, dinnerDuration: 0,
})

// 桌台数据（空数组，待后端接入）
const hallTables = ref([])
const privateTables = ref([])

// 时段利用率数据（空数组，待后端接入）
const hourlyUtilData = ref([])
// 桌台类型对比数据（空数组，待后端接入）
const tableTypeComparison = ref([])
// 周利用率数据（空数组，待后端接入）
const weeklyData = ref([])

const allTables = computed(() => {
  const hall = hallTables.value.map(t => ({ ...t, name: t.name || (t.number !== undefined ? t.number + '号桌' : '') }))
  return [...hall, ...privateTables.value]
})

const filteredTables = computed(() => {
  return allTables.value.filter(t => {
    if (filterZone.value && t.zone !== (filterZone.value === 'hall' ? '大厅' : '包厢')) return false
    if (filterStatus.value && t.status !== filterStatus.value) return false
    return true
  })
})

const statusText = (s) => ({ available: '空闲', occupied: '用餐中', reserved: '已预订', cleaning: '清洁中' }[s] || s)

const showTableDetail = (t) => { selectedTable.value = { ...t, name: t.name || (t.number !== undefined ? t.number + '号桌' : '') } }

// 时段利用率图表数据（基于 hourlyUtilData 计算，支持 rate/revenue/turnover 切换）
const utilData = computed(() => {
  const data = hourlyUtilData.value
  if (!data.length) return []
  const maxVal = Math.max(...data.map(d => Number(d[utilView.value] || 0))) || 1
  return data.map((d, i) => ({
    label: (d.hour || '') + ':00',
    value: Number(d[utilView.value] || 0),
    x: 50 + (i / Math.max(data.length - 1, 1)) * 740,
    y: 180 - (Number(d[utilView.value] || 0) / maxVal) * 150
  }))
})

const maxUtil = computed(() => utilData.value.length ? Math.max(...utilData.value.map(d => d.value)) : 0)
const utilLinePath = computed(() => {
  if (!utilData.value.length) return ''
  return utilData.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})
const utilAreaPath = computed(() => {
  const pts = utilData.value
  if (pts.length < 2) return ''
  const first = pts[0], last = pts[pts.length - 1]
  return `${utilLinePath.value} L ${last.x} 180 L ${first.x} 180 Z`
})

// 桌台营收排行（基于 allTables 计算）
const tableRevenueRank = computed(() => {
  const sorted = [...allTables.value].sort((a, b) => (b.todayRevenue || 0) - (a.todayRevenue || 0))
  if (!sorted.length) return []
  const maxRev = sorted[0]?.todayRevenue || 1
  const colors = ['#D4A853', '#2D4A3E', '#4A7C59', '#5B7B8A', '#C0392B', '#8a9a8e']
  return sorted.map((t, i) => ({
    ...t,
    name: t.name || (t.number !== undefined ? t.number + '号桌' : ''),
    type: t.zone,
    turns: t.turns,
    avgPax: t.pax,
    revenue: t.todayRevenue,
    revPercent: (t.todayRevenue || 0) / maxRev * 100,
    color: colors[i % colors.length],
  }))
})
</script>

<style scoped>
.table-util-page { padding: 24px 32px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }
.header-actions { display: flex; gap: 8px; }
.period-selector { display: flex; gap: 4px; }
.period-selector button { padding: 5px 14px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
.period-selector button.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px 20px; border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px; }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-icon svg { width: 22px; height: 22px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-value .unit { font-size: 14px; font-weight: 400; }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }
.trend-up { color: #4A7C59; }
.trend-down { color: #C0392B; }

.table-map-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0; }
.legend { display: flex; gap: 16px; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #6a7a6e; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; }
.legend-dot.available { background: #e8f5e9; border: 1px solid #a5d6a7; }
.legend-dot.occupied { background: #2D4A3E; }
.legend-dot.reserved { background: #D4A853; }
.legend-dot.cleaning { background: #5B7B8A; }
.legend-dot.lunch { background: #D4A853; }
.legend-dot.dinner { background: #2D4A3E; }

.table-map { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
.table-zone { }
.zone-title { font-size: 13px; font-weight: 600; color: #1a2f23; margin-bottom: 10px; padding-bottom: 6px; border-bottom: 1px solid #e8ece9; }
.table-grid { display: grid; gap: 8px; }
.hall-grid { grid-template-columns: repeat(5, 1fr); }
.private-grid { grid-template-columns: 1fr; }
.table-cell {
  padding: 14px 10px; border-radius: 8px; text-align: center; cursor: pointer;
  border: 2px solid transparent; transition: all 0.2s;
}
.table-cell:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.table-cell.available { background: #e8f5e9; border-color: #a5d6a7; }
.table-cell.occupied { background: #2D4A3E; color: #fff; }
.table-cell.reserved { background: #fef9e7; border-color: #D4A853; }
.table-cell.cleaning { background: #eef2f5; border-color: #5B7B8A; }
.table-cell.large { padding: 18px 14px; }
.table-number { font-size: 16px; font-weight: 700; }
.table-pax { font-size: 11px; opacity: 0.8; margin-top: 2px; }
.table-status-text { font-size: 10px; margin-top: 4px; opacity: 0.7; }
.table-timer { font-size: 12px; font-weight: 600; margin-top: 4px; color: #D4A853; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; }
.chart-card.wide { grid-column: 1 / -1; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.chart-tabs { display: flex; gap: 4px; }
.chart-tabs button { padding: 4px 12px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
.chart-tabs button.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }

.utilization-chart { overflow-x: auto; padding: 10px 0; }
.util-svg { width: 100%; height: 240px; }
.util-dot { transition: r 0.2s; cursor: pointer; }
.util-dot:hover { r: 6; }

.type-chart-container { width: 100%; padding: 10px 0; }
.type-svg { width: 100%; height: 260px; }
.type-bar { transition: opacity 0.2s; cursor: pointer; }
.type-bar:hover { opacity: 0.85; }
.type-legend { display: flex; justify-content: center; gap: 20px; margin-top: 12px; }
.type-legend .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6a7a6e; }
.type-legend .legend-dot { width: 12px; height: 12px; border-radius: 2px; }

.rank-chart-container { width: 100%; padding: 10px 0; }
.rank-svg { width: 100%; height: 280px; }
.rank-bar { transition: opacity 0.2s; cursor: pointer; }
.rank-bar:hover { opacity: 0.85; }

.weekly-chart-svg { width: 100%; padding: 10px 0; }
.weekly-svg { width: 100%; height: 260px; }
.week-bar { transition: opacity 0.2s; cursor: pointer; }
.week-bar:hover { opacity: 0.85; }
.weekly-legend { display: flex; justify-content: center; gap: 20px; margin-top: 12px; }
.weekly-legend .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6a7a6e; }
.weekly-legend .legend-dot { width: 12px; height: 12px; border-radius: 2px; }
.weekly-legend .legend-dot.lunch { background: #C4A35A; }
.weekly-legend .legend-dot.dinner { background: #2D4A3E; }

.detail-table-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; overflow-x: auto; }
.card-actions { display: flex; gap: 8px; }
.filter-select { padding: 5px 10px; border: 1px solid #d0d8d2; border-radius: 4px; font-size: 12px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e; border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }
.table-name-cell { font-weight: 600; color: #1a2f23; }
.money-cell { font-weight: 600; color: #D4A853; }
.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.available { background: rgba(74,124,89,0.1); color: #4A7C59; }
.status-badge.occupied { background: rgba(45,74,62,0.1); color: #2D4A3E; }
.status-badge.reserved { background: rgba(212,168,83,0.12); color: #b8922e; }
.status-badge.cleaning { background: rgba(91,123,138,0.1); color: #5B7B8A; }
.util-bar-cell { display: flex; align-items: center; gap: 6px; }
.util-bar-track { flex: 1; height: 6px; background: #f0f2f0; border-radius: 3px; overflow: hidden; min-width: 60px; }
.util-bar-fill { height: 100%; border-radius: 3px; }
.util-bar-value { font-size: 11px; color: #6a7a6e; min-width: 36px; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-box { background: #fff; border-radius: 12px; padding: 28px; width: 420px; max-width: 90vw; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.dialog-header h3 { font-size: 18px; color: #1a2f23; margin: 0; }
.dialog-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.dialog-item { display: flex; flex-direction: column; gap: 4px; }
.dialog-label { font-size: 11px; color: #8a9a8e; }
.dialog-value { font-size: 14px; font-weight: 600; color: #1a2f23; }
.dialog-value.money { color: #D4A853; }
.dialog-actions { display: flex; justify-content: flex-end; margin-top: 20px; }
.btn-cancel { padding: 8px 20px; border-radius: 6px; font-size: 13px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
</style>
