<template>
  <div class="staff-perf-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">员工绩效 · Staff Performance</h2>
        <p class="page-subtitle">Booking volume, revenue contribution and service quality analysis</p>
      </div>
      <div class="header-actions">
        <div class="period-selector">
          <el-button :class="{ active: period === 'today' }" @click="period = 'today'">今日</el-button>
          <el-button :class="{ active: period === 'week' }" @click="period = 'week'">本周</el-button>
          <el-button :class="{ active: period === 'month' }" @click="period = 'month'">本月</el-button>
        </div>
      </div>
    </div>

    <!-- 团队总览 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">在岗人数 · On Duty</div>
          <div class="stat-value" style="color:#2D4A3E">{{ teamStats.onDuty }}</div>
          <div class="stat-sub">总计 {{ teamStats.total }} 人</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">团队总预订 · Team Bookings</div>
          <div class="stat-value" style="color:#D4A853">{{ teamStats.totalBookings }}</div>
          <div class="stat-sub">人均 {{ teamStats.avgBookings }} 单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">团队总营收 · Revenue</div>
          <div class="stat-value" style="color:#5B7B8A">¥{{ (teamStats.totalRevenue || 0).toLocaleString() }}</div>
          <div class="stat-sub">人均 ¥{{ (teamStats.avgRevenue || 0).toLocaleString() }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">平均转化率 · Conversion</div>
          <div class="stat-value" style="color:#4A7C59">{{ teamStats.avgConversion }}%</div>
          <div class="stat-sub">较上周 {{ teamStats.conversionTrend > 0 ? '↑' : '↓' }} {{ Math.abs(teamStats.conversionTrend) }}%</div>
        </div>
      </div>
    </div>

    <!-- 绩效排行 -->
    <div class="ranking-card">
      <div class="card-header">
        <h3 class="section-title">绩效排行榜 · Performance Ranking</h3>
        <div class="metric-tabs">
          <el-button :class="{ active: metric === 'bookings' }" @click="metric = 'bookings'">预订量</el-button>
          <el-button :class="{ active: metric === 'revenue' }" @click="metric = 'revenue'">营收额</el-button>
          <el-button :class="{ active: metric === 'conversion' }" @click="metric = 'conversion'">转化率</el-button>
          <el-button :class="{ active: metric === 'satisfaction' }" @click="metric = 'satisfaction'">满意度</el-button>
        </div>
      </div>

      <!-- 前三名 podium -->
      <div v-if="!topThree.length" class="chart-empty">暂无数据</div>
      <div v-else class="podium">
        <div v-for="(staff, i) in topThree" :key="staff.id" class="podium-item" :class="'rank-' + (i + 1)">
          <div class="podium-avatar" :style="{ background: staff.color }">
            {{ staff.name ? staff.name[0] : '' }}
            <span class="podium-medal">{{ ['第一名','第二名','第三名'][i] }}</span>
          </div>
          <div class="podium-name">{{ staff.name }}</div>
          <div class="podium-role">{{ staff.role }}</div>
          <div class="podium-value" :style="{ color: staff.color }">
            {{ getMetricValue(staff) }}
          </div>
          <div class="podium-bar">
            <div class="podium-fill" :style="{ height: (staff[metric + 'Percent'] || 0) + '%', background: staff.color }"></div>
          </div>
        </div>
      </div>

      <!-- 其余排行 -->
      <div v-if="restRanking.length" class="ranking-list">
        <div v-for="(staff, i) in restRanking" :key="staff.id" class="ranking-row">
          <div class="rank-num">{{ i + 4 }}</div>
          <div class="staff-cell">
            <div class="staff-avatar" :style="{ background: staff.color }">{{ staff.name ? staff.name[0] : '' }}</div>
            <div class="staff-info">
              <div class="staff-name">{{ staff.name }}</div>
              <div class="staff-role">{{ staff.role }}</div>
            </div>
          </div>
          <div class="metric-bars">
            <div class="metric-bar-item">
              <span class="metric-bar-label">预订</span>
              <div class="metric-bar-track">
                <div class="metric-bar-fill" :style="{ width: (staff.bookingsPercent || 0) + '%', background: '#2D4A3E' }"></div>
              </div>
              <span class="metric-bar-value">{{ staff.bookings }}单</span>
            </div>
            <div class="metric-bar-item">
              <span class="metric-bar-label">营收</span>
              <div class="metric-bar-track">
                <div class="metric-bar-fill" :style="{ width: (staff.revenuePercent || 0) + '%', background: '#D4A853' }"></div>
              </div>
              <span class="metric-bar-value">¥{{ (staff.revenue || 0).toLocaleString() }}</span>
            </div>
            <div class="metric-bar-item">
              <span class="metric-bar-label">转化</span>
              <div class="metric-bar-track">
                <div class="metric-bar-fill" :style="{ width: (staff.conversionPercent || 0) + '%', background: '#5B7B8A' }"></div>
              </div>
              <span class="metric-bar-value">{{ staff.conversion }}%</span>
            </div>
          </div>
          <div class="highlight-value" :style="{ color: staff.color }">
            {{ getMetricValue(staff) }}
          </div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <!-- 员工趋势对比 - 高质量SVG折线图 -->
      <div class="chart-card wide">
        <div class="chart-header">
          <h3 class="section-title">员工趋势对比 · Staff Trend Comparison</h3>
          <div class="chart-tabs">
            <el-button v-for="s in staffList" :key="s.id"
              :class="{ active: selectedStaff.includes(s.id) }"
              @click="toggleStaff(s.id)"
              :style="{ borderColor: s.color, color: selectedStaff.includes(s.id) ? s.color : '#6a7a6e' }">
              {{ s.name }}
            </el-button>
          </div>
        </div>
        <div v-if="!filteredStaff.length" class="chart-empty">暂无数据</div>
        <div v-else class="trend-chart-svg">
          <svg viewBox="0 0 800 280" class="trend-svg">
            <defs>
              <linearGradient v-for="s in filteredStaff" :key="'grad'+s.id" :id="'trendGrad'+s.id" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" :stop-color="s.color" stop-opacity="0.3"/>
                <stop offset="100%" :stop-color="s.color" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'h'+i" x1="60" :y1="40+(i-1)*45" x2="780" :y2="40+(i-1)*45" stroke="#e8ece9" stroke-width="1"/>
            <!-- Y轴标签 -->
            <text v-for="i in 5" :key="'y'+i" x="50" :y="45+(i-1)*45" text-anchor="end" font-size="11" fill="#8a9a8e">{{ 10 - (i-1)*2.5 }}</text>
            <!-- X轴标签 -->
            <text v-for="(day, i) in trendDays" :key="'x'+i" :x="100+i*100" y="270" text-anchor="middle" font-size="12" fill="#6a7a6e">{{ day }}</text>
            <!-- 折线和面积 -->
            <template v-for="(s, idx) in filteredStaff" :key="'line'+s.id">
              <path :d="getTrendAreaPath(s)" :fill="`url(#trendGrad${s.id})`" class="trend-area"/>
              <path :d="getTrendLinePath(s)" fill="none" :stroke="s.color" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="trend-line"/>
              <circle v-for="(day, i) in trendDays" :key="'p'+i"
                :cx="100+i*100" :cy="getTrendY(s, day)"
                r="4" :fill="s.color" stroke="#fff" stroke-width="2" class="trend-point"/>
            </template>
          </svg>
        </div>
      </div>

      <!-- 能力雷达 - 高质量SVG -->
      <div class="chart-card">
        <h3 class="section-title">能力雷达 · Skill Radar</h3>
        <div class="radar-selector">
          <el-select v-model="radarStaffId" placeholder="请选择员工" style="width:200px">
            <el-option v-for="s in staffList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div v-if="!radarValues.length" class="chart-empty">暂无数据</div>
        <div v-else class="radar-chart-container">
          <svg viewBox="0 0 240 240" class="radar-svg">
            <defs>
              <radialGradient id="radarGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#2D4A3E" stop-opacity="0.4"/>
                <stop offset="100%" stop-color="#4A7C59" stop-opacity="0.1"/>
              </radialGradient>
            </defs>
            <!-- 背景网格 -->
            <polygon v-for="level in 5" :key="'g'+level"
              :points="getRadarPoints(level * 18)"
              fill="none" stroke="#e8ece9" stroke-width="1"/>
            <!-- 轴线 -->
            <line v-for="(axis, i) in radarAxes" :key="'a'+i"
              x1="120" y1="120"
              :x2="120 + Math.cos(axis.angle - Math.PI/2) * 90"
              :y2="120 + Math.sin(axis.angle - Math.PI/2) * 90"
              stroke="#e8ece9" stroke-width="1"/>
            <!-- 数据多边形 -->
            <polygon :points="getRadarDataPoints()" fill="url(#radarGrad)" stroke="#2D4A3E" stroke-width="2.5" class="radar-polygon"/>
            <!-- 数据点 -->
            <circle v-for="(axis, i) in radarAxes" :key="'d'+i"
              :cx="120 + Math.cos(axis.angle - Math.PI/2) * (radarValues[i] / 100 * 90)"
              :cy="120 + Math.sin(axis.angle - Math.PI/2) * (radarValues[i] / 100 * 90)"
              r="5" fill="#2D4A3E" stroke="#fff" stroke-width="2.5" class="radar-dot"/>
            <!-- 标签 -->
            <text v-for="(axis, i) in radarAxes" :key="'l'+i"
              :x="120 + Math.cos(axis.angle - Math.PI/2) * 108"
              :y="120 + Math.sin(axis.angle - Math.PI/2) * 108 + 4"
              text-anchor="middle" font-size="12" font-weight="500" fill="#3a4a3e">
              {{ axis.label }}
            </text>
            <!-- 数值标签 -->
            <text v-for="(axis, i) in radarAxes" :key="'v'+i"
              :x="120 + Math.cos(axis.angle - Math.PI/2) * (radarValues[i] / 100 * 90 + 15)"
              :y="120 + Math.sin(axis.angle - Math.PI/2) * (radarValues[i] / 100 * 90 + 15) + 4"
              text-anchor="middle" font-size="10" font-weight="600" fill="#2D4A3E">
              {{ radarValues[i] }}
            </text>
          </svg>
        </div>
        <div v-if="radarValues.length" class="radar-values">
          <div v-for="(axis, i) in radarAxes" :key="'rv'+i" class="radar-value-item">
            <span class="radar-value-label">{{ axis.label }}</span>
            <span class="radar-value-num">{{ radarValues[i] }}</span>
          </div>
        </div>
      </div>

      <!-- 服务评价分布 - 高质量SVG柱状图 -->
      <div class="chart-card">
        <h3 class="section-title">服务评价 · Service Rating</h3>
        <div v-if="!staffList.length" class="chart-empty">暂无数据</div>
        <div v-else class="rating-chart-container">
          <svg viewBox="0 0 400 280" class="rating-svg">
            <defs>
              <linearGradient id="ratingGrad5" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#4A7C59"/>
                <stop offset="100%" stop-color="#6A9C79"/>
              </linearGradient>
              <linearGradient id="ratingGrad4" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#6A9C79"/>
                <stop offset="100%" stop-color="#8AB899"/>
              </linearGradient>
              <linearGradient id="ratingGrad3" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#D4A853"/>
                <stop offset="100%" stop-color="#E4B863"/>
              </linearGradient>
              <linearGradient id="ratingGrad2" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#C0392B"/>
                <stop offset="100%" stop-color="#D0493B"/>
              </linearGradient>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'rg'+i" x1="40" :y1="40+(i-1)*45" x2="380" :y2="40+(i-1)*45" stroke="#e8ece9" stroke-width="1"/>
            <!-- Y轴标签 -->
            <text v-for="i in 5" :key="'ry'+i" x="35" :y="45+(i-1)*45" text-anchor="end" font-size="11" fill="#8a9a8e">{{ 100 - (i-1)*25 }}%</text>
            <!-- 柱状图 -->
            <g v-for="(staff, sIdx) in staffList" :key="'sg'+staff.id">
              <rect v-for="star in 5" :key="'bar'+star"
                :x="60 + sIdx * 65 + (star-1) * 12"
                :y="220 - getRatingPercent(staff.id, star) * 1.8"
                width="10"
                :height="getRatingPercent(staff.id, star) * 1.8"
                :fill="`url(#ratingGrad${star})`"
                rx="2"
                class="rating-bar"/>
              <text :x="85 + sIdx * 65" y="245" text-anchor="middle" font-size="12" font-weight="500" fill="#3a4a3e">{{ staff.name }}</text>
              <text :x="85 + sIdx * 65" y="262" text-anchor="middle" font-size="11" font-weight="600" fill="#C4A35A">{{ getAvgRating(staff.id).toFixed(1) }}分</text>
            </g>
          </svg>
        </div>
        <div class="rating-legend">
          <div v-for="star in 5" :key="'leg'+star" class="rating-legend-item">
            <span class="rating-legend-color" :style="{ background: `url(#ratingGrad${star})` }"></span>
            <span class="rating-legend-label">{{ star }}星</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 详细数据表 -->
    <div class="detail-table-card">
      <div class="card-header">
        <h3 class="section-title">绩效明细 · Performance Details</h3>
        <el-button size="small" @click="exportData">导出</el-button>
      </div>
      <el-table :data="allStaffSorted" border style="width: 100%">
        <el-table-column label="排名" width="80">
          <template #default="scope">
            <span class="rank-badge" :class="{ 'top3': scope.$index < 3 }">{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="员工" min-width="160">
          <template #default="scope">
            <div class="staff-cell">
              <div class="staff-avatar-sm" :style="{ background: scope.row.color }">{{ scope.row.name ? scope.row.name[0] : '' }}</div>
              {{ scope.row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="岗位" width="120" />
        <el-table-column label="预订量" width="100">
          <template #default="scope">{{ scope.row.bookings }}单</template>
        </el-table-column>
        <el-table-column label="营收额" width="130">
          <template #default="scope">¥{{ (scope.row.revenue || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="转化率" width="100">
          <template #default="scope">{{ scope.row.conversion }}%</template>
        </el-table-column>
        <el-table-column label="满意度" width="120">
          <template #default="scope">
            <span class="rating-text">{{ getAvgRating(scope.row.id).toFixed(1) }}分</span>
          </template>
        </el-table-column>
        <el-table-column label="客单价" width="110">
          <template #default="scope">¥{{ scope.row.bookings ? Math.round(scope.row.revenue / scope.row.bookings) : 0 }}</template>
        </el-table-column>
        <el-table-column label="复购率" width="100">
          <template #default="scope">{{ scope.row.repeatRate }}%</template>
        </el-table-column>
        <el-table-column label="综合评分" width="120">
          <template #default="scope">
            <span class="score-badge" :style="{ background: getScoreColor(scope.row.compositeScore) }">{{ scope.row.compositeScore }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const period = ref('week')
const metric = ref('bookings')
const selectedStaff = ref([])
const radarStaffId = ref(null)

// 团队统计（空值，待后端接入）
const teamStats = ref({
  onDuty: 0, total: 0, totalBookings: 0, avgBookings: 0,
  totalRevenue: 0, avgRevenue: 0, avgConversion: 0, conversionTrend: 0,
})

// 员工列表（空数组，待后端接入）
const staffList = ref([])

// 趋势数据（空对象，待后端接入，key: staffId, value: number[]）
const trendData = ref({})
// 雷达图数据（空对象，待后端接入，key: staffId, value: number[]）
const radarValuesMap = ref({})
// 服务评价数据（空对象，待后端接入，key: staffId, value: {1..5: percent}）
const ratingData = ref({})

const allStaffSorted = computed(() => {
  if (!staffList.value.length) return []
  return [...staffList.value].sort((a, b) => {
    if (metric.value === 'bookings') return (b.bookings || 0) - (a.bookings || 0)
    if (metric.value === 'revenue') return (b.revenue || 0) - (a.revenue || 0)
    if (metric.value === 'conversion') return (b.conversion || 0) - (a.conversion || 0)
    return (b.compositeScore || 0) - (a.compositeScore || 0)
  })
})

const topThree = computed(() => {
  const sorted = allStaffSorted.value
  if (!sorted.length) return []
  const maxBookings = sorted[0].bookings || 1
  const maxRevenue = sorted[0].revenue || 1
  return sorted.slice(0, 3).map(s => ({
    ...s,
    bookingsPercent: (s.bookings || 0) / maxBookings * 100,
    revenuePercent: (s.revenue || 0) / maxRevenue * 100,
    conversionPercent: s.conversion || 0,
  }))
})

const restRanking = computed(() => {
  const sorted = allStaffSorted.value
  if (sorted.length <= 3) return []
  const maxBookings = sorted[0].bookings || 1
  const maxRevenue = sorted[0].revenue || 1
  return sorted.slice(3).map(s => ({
    ...s,
    bookingsPercent: (s.bookings || 0) / maxBookings * 100,
    revenuePercent: (s.revenue || 0) / maxRevenue * 100,
    conversionPercent: s.conversion || 0,
  }))
})

const getMetricValue = (s) => {
  if (metric.value === 'bookings') return (s.bookings || 0) + '单'
  if (metric.value === 'revenue') return '¥' + (s.revenue || 0).toLocaleString()
  if (metric.value === 'conversion') return (s.conversion || 0) + '%'
  return (s.satisfaction || 0).toFixed(1) + '分'
}

const trendDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const filteredStaff = computed(() => staffList.value.filter(s => selectedStaff.value.includes(s.id)))
const toggleStaff = (id) => {
  const idx = selectedStaff.value.indexOf(id)
  if (idx >= 0) selectedStaff.value.splice(idx, 1)
  else selectedStaff.value.push(id)
}

const getTrendY = (staff, day) => {
  const dayIdx = trendDays.indexOf(day)
  const val = trendData.value[staff.id]?.[dayIdx] || 0
  return 220 - (val / 10) * 180
}
const getTrendLinePath = (staff) => {
  return trendDays.map((day, i) => {
    const x = 100 + i * 100
    const y = getTrendY(staff, day)
    return (i === 0 ? 'M' : 'L') + x + ',' + y
  }).join(' ')
}
const getTrendAreaPath = (staff) => {
  const linePath = getTrendLinePath(staff)
  const lastX = 100 + (trendDays.length - 1) * 100
  return linePath + ` L${lastX},220 L100,220 Z`
}

const radarAxes = [
  { label: '预订量', angle: 0 },
  { label: '营收额', angle: 72 },
  { label: '转化率', angle: 144 },
  { label: '满意度', angle: 216 },
  { label: '复购率', angle: 288 },
]

const radarValues = computed(() => radarValuesMap.value[radarStaffId.value] || [])

const getRadarPoints = (r) => {
  return radarAxes.map(a => {
    const x = 100 + Math.cos(a.angle * Math.PI / 180 - Math.PI / 2) * r
    const y = 100 + Math.sin(a.angle * Math.PI / 180 - Math.PI / 2) * r
    return `${x},${y}`
  }).join(' ')
}

const getRadarDataPoints = () => {
  return radarAxes.map((a, i) => {
    const r = ((radarValues.value[i] || 0) / 100) * 100
    const x = 100 + Math.cos(a.angle * Math.PI / 180 - Math.PI / 2) * r
    const y = 100 + Math.sin(a.angle * Math.PI / 180 - Math.PI / 2) * r
    return `${x},${y}`
  }).join(' ')
}

const getRatingPercent = (staffId, star) => ratingData.value[staffId]?.[star] || 0
const getAvgRating = (staffId) => {
  const d = ratingData.value[staffId]
  if (!d) return 0
  return (5*(d[5]||0) + 4*(d[4]||0) + 3*(d[3]||0) + 2*(d[2]||0) + 1*(d[1]||0)) / 100
}

const getScoreColor = (score) => {
  if (!score) return 'rgba(192,57,43,0.1)'
  if (score >= 85) return 'rgba(74,124,89,0.15)'
  if (score >= 70) return 'rgba(212,168,83,0.15)'
  return 'rgba(192,57,43,0.1)'
}

const exportData = () => { console.log('Exporting...') }
</script>

<style scoped>
.staff-perf-page { padding: 24px 32px; }
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
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }

.ranking-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0; }
.metric-tabs { display: flex; gap: 4px; }
.metric-tabs button { padding: 4px 12px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
.metric-tabs button.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }

.podium { display: flex; justify-content: center; gap: 20px; margin-bottom: 24px; align-items: flex-end; }
.podium-item { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.podium-item.rank-1 { order: 2; }
.podium-item.rank-2 { order: 1; }
.podium-item.rank-3 { order: 3; }
.podium-avatar { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: 700; position: relative; }
.podium-medal { position: absolute; bottom: -8px; font-size: 16px; }
.podium-name { font-size: 14px; font-weight: 600; color: #1a2f23; }
.podium-role { font-size: 11px; color: #8a9a8e; }
.podium-value { font-size: 16px; font-weight: 700; }
.podium-bar { width: 60px; height: 80px; background: #f0f2f0; border-radius: 4px 4px 0 0; position: relative; overflow: hidden; display: flex; align-items: flex-end; }
.podium-fill { width: 100%; border-radius: 4px 4px 0 0; }

.ranking-list { display: flex; flex-direction: column; gap: 8px; }
.ranking-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: #f8f9f8; border-radius: 6px; }
.rank-num { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; background: #e8ece9; color: #6a7a6e; }
.staff-cell { display: flex; align-items: center; gap: 8px; width: 130px; }
.staff-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 13px; flex-shrink: 0; }
.staff-info { flex: 1; }
.staff-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.staff-role { font-size: 11px; color: #8a9a8e; }
.metric-bars { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.metric-bar-item { display: flex; align-items: center; gap: 6px; }
.metric-bar-label { width: 30px; font-size: 10px; color: #8a9a8e; }
.metric-bar-track { flex: 1; height: 5px; background: #e8ece9; border-radius: 3px; overflow: hidden; }
.metric-bar-fill { height: 100%; border-radius: 3px; }
.metric-bar-value { width: 70px; font-size: 10px; color: #6a7a6e; text-align: right; }
.highlight-value { width: 80px; text-align: right; font-size: 14px; font-weight: 700; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; }
.chart-card.wide { grid-column: 1 / -1; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.chart-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.chart-tabs button { padding: 3px 10px; border-radius: 4px; font-size: 11px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; }
.chart-tabs button.active { font-weight: 600; }

.trend-chart-svg { width: 100%; overflow-x: auto; }
.trend-svg { width: 100%; min-width: 600px; height: 280px; }
.trend-area { opacity: 0.6; transition: opacity 0.3s; }
.trend-area:hover { opacity: 0.8; }
.trend-line { transition: stroke-width 0.3s; }
.trend-line:hover { stroke-width: 3.5; }
.trend-point { transition: r 0.2s; cursor: pointer; }
.trend-point:hover { r: 6; }

.radar-selector { margin-bottom: 12px; }
.radar-selector select { padding: 6px 12px; border: 1px solid #d0d8d2; border-radius: 6px; font-size: 13px; background: #fff; cursor: pointer; }
.radar-chart-container { display: flex; justify-content: center; padding: 10px 0; }
.radar-svg { width: 240px; height: 240px; }
.radar-polygon { transition: all 0.3s; }
.radar-polygon:hover { stroke-width: 3; }
.radar-dot { transition: r 0.2s; cursor: pointer; }
.radar-dot:hover { r: 7; }
.radar-values { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 12px; }
.radar-value-item { display: flex; align-items: center; gap: 4px; font-size: 11px; }
.radar-value-label { color: #6a7a6e; }
.radar-value-num { font-weight: 600; color: #1a2f23; }

.rating-chart-container { width: 100%; padding: 10px 0; }
.rating-svg { width: 100%; height: 280px; }
.rating-bar { transition: opacity 0.2s; cursor: pointer; }
.rating-bar:hover { opacity: 0.8; }
.rating-legend { display: flex; justify-content: center; gap: 16px; margin-top: 12px; flex-wrap: wrap; }
.rating-legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.rating-legend-color { width: 16px; height: 12px; border-radius: 2px; }
.rating-legend-label { color: #6a7a6e; }

.detail-table-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; overflow-x: auto; }
.btn-sm { padding: 5px 12px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #3a4a3e; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e; border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }
.rank-badge { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; font-size: 11px; font-weight: 700; background: #e8ece9; color: #6a7a6e; }
.rank-badge.top3 { background: #D4A853; color: #fff; }
.staff-cell { display: flex; align-items: center; gap: 8px; }
.staff-avatar-sm { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 600; }
.rating-stars { display: flex; gap: 1px; margin-right: 4px; }
.rating-stars .filled { color: #D4A853; }
.rating-stars span:not(.filled) { color: #e0e0e0; }
.rating-text { font-size: 13px; font-weight: 600; color: #D4A853; }
.score-badge { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.chart-empty { display: flex; align-items: center; justify-content: center; height: 200px; color: #a0b0a5; font-size: 13px; }
</style>
