<template>
  <div class="guest-analysis-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">客人分析 · Guest Analysis</h2>
        <p class="page-subtitle">Guest profiling, behavior analysis and segmentation</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showQueryPanel = !showQueryPanel">筛选</el-button>
      </div>
    </div>

    <!-- 筛选面板 -->
    <div v-if="showQueryPanel" class="query-panel">
      <div class="query-row">
        <div class="query-group">
          <label>日期范围</label>
          <div class="date-range">
            <el-date-picker v-model="query.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:140px" />
            <span>至</span>
            <el-date-picker v-model="query.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:140px" />
          </div>
        </div>
        <div class="query-group">
          <label>客人类型</label>
          <el-select v-model="query.guestType" placeholder="全部" clearable style="width:100%">
            <el-option label="新客" value="new" />
            <el-option label="回头客" value="returning" />
            <el-option label="VIP" value="vip" />
            <el-option label="企业客户" value="corporate" />
          </el-select>
        </div>
        <div class="query-group">
          <label>消费等级</label>
          <el-select v-model="query.spendLevel" placeholder="全部" clearable style="width:100%">
            <el-option label="低 (¥0-500)" value="low" />
            <el-option label="中 (¥500-2000)" value="mid" />
            <el-option label="高 (¥2000-5000)" value="high" />
            <el-option label="超高 (¥5000+)" value="luxury" />
          </el-select>
        </div>
        <div class="query-group">
          <label>来源渠道</label>
          <el-select v-model="query.source" placeholder="全部" clearable style="width:100%">
            <el-option label="自来" value="walk-in" />
            <el-option label="电话" value="phone" />
            <el-option label="线上" value="online" />
            <el-option label="会员推荐" value="member" />
          </el-select>
        </div>
      </div>
      <div class="query-row">
        <div class="query-group">
          <label>人数范围</label>
          <div class="range-input">
            <el-input-number v-model="query.paxMin" :min="0" controls-position="right" placeholder="最少" style="width:110px" />
            <span>至</span>
            <el-input-number v-model="query.paxMax" :min="0" controls-position="right" placeholder="最多" style="width:110px" />
          </div>
        </div>
        <div class="query-group">
          <label>用餐频次</label>
          <el-select v-model="query.frequency" placeholder="全部" clearable style="width:100%">
            <el-option label="仅1次" value="1" />
            <el-option label="2-3次" value="2-3" />
            <el-option label="4-6次" value="4-6" />
            <el-option label="7次以上" value="7+" />
          </el-select>
        </div>
        <div class="query-group">
          <label>偏好桌台</label>
          <el-select v-model="query.prefTable" placeholder="全部" clearable style="width:100%">
            <el-option label="大厅" value="hall" />
            <el-option label="包厢" value="private" />
            <el-option label="VIP" value="vip" />
          </el-select>
        </div>
        <div class="query-group">
          <label>满意度</label>
          <el-select v-model="query.satisfaction" placeholder="全部" clearable style="width:100%">
            <el-option label="5星" value="5" />
            <el-option label="4星" value="4" />
            <el-option label="3星" value="3" />
            <el-option label="2星" value="2" />
            <el-option label="1星" value="1" />
          </el-select>
        </div>
      </div>
      <div class="query-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="applyQuery">查询</el-button>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">总客人数 · Total Guests</div>
          <div class="stat-value" style="color:#2D4A3E">{{ stats.totalGuests }}</div>
          <div class="stat-sub"><span :class="stats.guestTrend >= 0 ? 'trend-up' : 'trend-down'">{{ stats.guestTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.guestTrend) }}%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">回头客比例 · Return Rate</div>
          <div class="stat-value" style="color:#C4A35A">{{ stats.returnRate }}%</div>
          <div class="stat-sub"><span :class="stats.returnTrend >= 0 ? 'trend-up' : 'trend-down'">{{ stats.returnTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.returnTrend) }}%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">平均消费 · Avg Spend</div>
          <div class="stat-value" style="color:#4A7C59">¥{{ stats.avgSpend }}</div>
          <div class="stat-sub"><span :class="stats.spendTrend >= 0 ? 'trend-up' : 'trend-down'">{{ stats.spendTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.spendTrend) }}%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-content">
          <div class="stat-label">平均评分 · Avg Rating</div>
          <div class="stat-value" style="color:#C4A35A">{{ stats.avgRating }}</div>
          <div class="stat-sub">共 {{ stats.ratingCount }} 条评价</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <!-- 客人类型分布 - 环形图 -->
      <div class="chart-card">
        <h3 class="section-title">客人类型分布 · Guest Type Distribution</h3>
        <div v-if="!guestTypeSlices.length" class="chart-empty">暂无数据</div>
        <div v-else class="donut-chart-container">
          <div class="donut-svg-wrap">
            <svg viewBox="0 0 200 200" class="donut-svg">
              <defs>
                <linearGradient id="dg1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#2D4A3E"/><stop offset="100%" stop-color="#4A7C59"/></linearGradient>
                <linearGradient id="dg2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#4A7C59"/><stop offset="100%" stop-color="#6A9C79"/></linearGradient>
                <linearGradient id="dg3" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#C4A35A"/><stop offset="100%" stop-color="#D4B36A"/></linearGradient>
                <linearGradient id="dg4" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#5B7B8A"/><stop offset="100%" stop-color="#7B9BAA"/></linearGradient>
              </defs>
              <circle cx="100" cy="100" r="72" fill="none" stroke="#f0f2f0" stroke-width="28"/>
              <circle v-for="(slice, i) in guestTypeSlices" :key="i"
                cx="100" cy="100" r="72" fill="none"
                :stroke="slice.gradient" stroke-width="28"
                :stroke-dasharray="`${slice.length} ${452.4 - slice.length}`"
                :stroke-dashoffset="-slice.offset"
                transform="rotate(-90 100 100)"
                class="donut-segment"/>
              <circle cx="100" cy="100" r="56" fill="#fff"/>
              <text x="100" y="92" text-anchor="middle" font-size="22" font-weight="700" fill="#1a2f23">{{ stats.totalGuests }}</text>
              <text x="100" y="112" text-anchor="middle" font-size="11" fill="#8a9a8e">总客人数</text>
            </svg>
          </div>
          <div class="donut-legend">
            <div v-for="slice in guestTypeSlices" :key="slice.label" class="legend-item">
              <span class="legend-dot" :style="{ background: slice.color }"></span>
              <span class="legend-label">{{ slice.label }}</span>
              <span class="legend-value">{{ slice.value }} <span class="legend-pct">({{ slice.percent }}%)</span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 消费等级分布 -->
      <div class="chart-card">
        <h3 class="section-title">消费等级 · Spending Level</h3>
        <div v-if="!spendLevelData.length" class="chart-empty">暂无数据</div>
        <div v-else class="bar-chart-horizontal">
          <div v-for="item in spendLevelData" :key="item.level" class="h-bar-row">
            <span class="h-bar-label">{{ item.level }}</span>
            <div class="h-bar-track">
              <div class="h-bar-fill anim-bar" :style="{ width: item.percent + '%', '--bar-color': item.color }">
                <div class="h-bar-shine"></div>
              </div>
            </div>
            <span class="h-bar-value">{{ item.count }}人 · {{ item.percent }}%</span>
          </div>
        </div>
      </div>

      <!-- 客人来源趋势 -->
      <div class="chart-card wide">
        <div class="chart-header">
          <h3 class="section-title">客人来源趋势 · Guest Source Trend</h3>
          <div class="chart-tabs">
            <el-button :class="{ active: sourcePeriod === 'week' }" @click="sourcePeriod = 'week'">本周</el-button>
            <el-button :class="{ active: sourcePeriod === 'month' }" @click="sourcePeriod = 'month'">本月</el-button>
          </div>
        </div>
        <div v-if="!sourceTrendData.length" class="chart-empty">暂无数据</div>
        <div v-else class="stacked-bar-chart">
          <div v-for="day in sourceTrendData" :key="day.label" class="stacked-bar-group">
            <div class="stacked-bars">
              <div v-for="seg in day.segments" :key="seg.source" class="stacked-segment anim-segment"
                :style="{ height: seg.height + '%', '--seg-color': seg.color }"
                :title="`${seg.source}: ${seg.value}`"></div>
            </div>
            <span class="stacked-label">{{ day.label }}</span>
          </div>
          <div class="stacked-legend">
            <span v-for="s in sourceLegend" :key="s.source" class="legend-item">
              <span class="legend-dot" :style="{ background: s.color }"></span>
              {{ s.source }}
            </span>
          </div>
        </div>
      </div>

      <!-- 客人画像 -->
      <div class="chart-card">
        <h3 class="section-title">客人画像 · Guest Profile</h3>
        <div v-if="!guestProfile.length" class="chart-empty">暂无数据</div>
        <div v-else class="profile-grid">
          <div class="profile-item" v-for="group in guestProfile" :key="group.category">
            <div class="profile-label">{{ group.category }}</div>
            <div class="profile-bar-row" v-for="bar in group.bars" :key="bar.label">
              <span class="profile-bar-label">{{ bar.label }}</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" :style="{ width: bar.percent + '%', '--bar-color': bar.color }"></div>
              </div>
              <span class="profile-bar-value">{{ bar.percent }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- VIP客人排行 -->
      <div class="chart-card">
        <h3 class="section-title">VIP客人排行 · Top VIP Guests</h3>
        <div v-if="!vipGuests.length" class="chart-empty">暂无数据</div>
        <div v-else class="vip-list">
          <div v-for="(g, i) in vipGuests" :key="g.id" class="vip-item">
            <div class="vip-rank" :class="{ 'rank-top': i < 3 }">{{ i + 1 }}</div>
            <div class="vip-avatar" :style="{ background: `linear-gradient(135deg, ${g.color}, ${g.color}dd)` }">{{ g.name ? g.name[0] : '' }}</div>
            <div class="vip-info">
              <div class="vip-name">{{ g.name }}</div>
              <div class="vip-meta">{{ g.visits }}次消费 · {{ g.lastVisit }}</div>
            </div>
            <div class="vip-spend">¥{{ (g.totalSpend || 0).toLocaleString() }}</div>
          </div>
        </div>
      </div>

      <!-- 满意度趋势 -->
      <div class="chart-card wide">
        <h3 class="section-title">满意度趋势 · Satisfaction Trend</h3>
        <div class="satisfaction-chart">
          <div v-if="!satisfactionData.length" class="chart-empty">暂无数据</div>
          <svg v-else viewBox="0 0 700 180" class="sat-svg">
            <defs>
              <linearGradient id="satGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#C4A35A" stop-opacity="0.3"/>
                <stop offset="100%" stop-color="#C4A35A" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'g'+i" x1="50" :y1="20+(i-1)*30" x2="680" :y2="20+(i-1)*30" stroke="#e8ece9" stroke-width="0.5"/>
            <text v-for="i in 5" :key="'y'+i" x="45" :y="24+(i-1)*30" text-anchor="end" font-size="9" fill="#8a9a8e">{{ (5-i+1) }}</text>
            <!-- 面积 -->
            <path :d="satAreaPath" fill="url(#satGrad)"/>
            <!-- 折线 -->
            <path :d="satLinePath" fill="none" stroke="#C4A35A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <!-- 数据点 -->
            <circle v-for="(p,i) in satPoints" :key="'p'+i" :cx="p.x" :cy="p.y" r="5" fill="#C4A35A" stroke="#fff" stroke-width="2.5"/>
            <text v-for="(p,i) in satPoints" :key="'v'+i" :x="p.x" :y="p.y - 12" text-anchor="middle" font-size="10" font-weight="600" fill="#1a2f23">{{ satisfactionData[i].avgRating.toFixed(1) }}</text>
            <!-- X轴标签 -->
            <text v-for="(p,i) in satPoints" :key="'x'+i" :x="p.x" y="175" text-anchor="middle" font-size="10" fill="#8a9a8e">{{ satisfactionData[i].month }}</text>
          </svg>
        </div>
      </div>
    </div>

    <!-- 客人列表 -->
    <div class="guest-table-card">
      <div class="card-header">
        <h3 class="section-title">客人明细 · Guest Details</h3>
        <div class="card-actions">
          <el-input v-model="guestSearch" placeholder="搜索客人姓名/电话..." style="width:220px" clearable />
        </div>
      </div>
      <el-table :data="filteredGuests" border style="width: 100%">
        <el-table-column label="客人" min-width="160">
          <template #default="scope">
            <div class="guest-cell">
              <div class="guest-avatar" :style="{ background: `linear-gradient(135deg, ${scope.row.color}, ${scope.row.color}dd)` }">{{ scope.row.name ? scope.row.name[0] : '' }}</div>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <span :class="['type-badge', scope.row.type]">{{ typeText(scope.row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="visits" label="消费次数" width="100" />
        <el-table-column label="累计消费" width="130">
          <template #default="scope">
            ¥{{ (scope.row.totalSpend || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="平均消费" width="120">
          <template #default="scope">
            ¥{{ scope.row.visits ? Math.round(scope.row.totalSpend / scope.row.visits) : 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="prefTable" label="偏好桌台" width="110" />
        <el-table-column prop="lastVisit" label="最近到访" width="120" />
        <el-table-column label="评分" width="90">
          <template #default="scope">
            <span class="rating-text">{{ scope.row.rating || 0 }}分</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const showQueryPanel = ref(false)
const sourcePeriod = ref('month')
const guestSearch = ref('')

const query = ref({
  dateFrom: '', dateTo: '', guestType: '', spendLevel: '',
  source: '', paxMin: null, paxMax: null, frequency: '', prefTable: '', satisfaction: ''
})

// 统计数据（空值，待后端接入）
const stats = ref({
  totalGuests: 0,
  guestTrend: 0,
  returnRate: 0,
  returnTrend: 0,
  avgSpend: 0,
  spendTrend: 0,
  avgRating: 0,
  ratingCount: 0
})

// 客人类型分布原始数据（空数组，待后端接入）
const guestTypeData = ref([])
// 消费等级分布数据（空数组，待后端接入）
const spendLevelData = ref([])
// 客人来源趋势数据（空数组，待后端接入）
const sourceTrendData = ref([])
// VIP 客人排行（空数组，待后端接入）
const vipGuests = ref([])
// 满意度趋势数据（空数组，待后端接入）
const satisfactionData = ref([])
// 客人明细列表（空数组，待后端接入）
const guests = ref([])
// 客人画像数据（空数组，待后端接入）
const guestProfile = ref([])

// 客人类型环形图分段计算
const guestTypeSlices = computed(() => {
  const data = guestTypeData.value
  if (!data.length) return []
  const total = data.reduce((s, d) => s + (d.value || 0), 0) || 1
  const circumference = 2 * Math.PI * 72 // ~452.4
  let offset = 0
  return data.map(d => {
    const length = (d.value / total) * circumference
    const slice = { ...d, length, offset, percent: Math.round(d.value / total * 100) }
    offset += length
    return slice
  })
})

// 来源图例（静态配置，颜色映射）
const sourceLegend = [
  { source: '自来', color: '#2D4A3E' },
  { source: '电话', color: '#4A7C59' },
  { source: '线上', color: '#C4A35A' },
  { source: '会员', color: '#5B7B8A' },
]

// 满意度趋势折线图坐标点
const satPoints = computed(() => {
  const data = satisfactionData.value
  if (!data.length) return []
  const w = 700, padX = 60, padY = 20, chartH = 130
  const maxVal = 5
  return data.map((d, i) => ({
    x: padX + (i / Math.max(data.length - 1, 1)) * (w - padX * 2),
    y: padY + chartH - (d.avgRating / maxVal) * chartH
  }))
})

const satLinePath = computed(() => {
  if (!satPoints.value.length) return ''
  return satPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})
const satAreaPath = computed(() => {
  const pts = satPoints.value
  if (pts.length < 2) return ''
  const first = pts[0], last = pts[pts.length - 1]
  return `${satLinePath.value} L ${last.x} 160 L ${first.x} 160 Z`
})

const filteredGuests = computed(() => {
  if (!guestSearch.value) return guests.value
  const q = guestSearch.value.toLowerCase()
  return guests.value.filter(g => (g.name && g.name.includes(q)) || (g.phone && g.phone.includes(q)))
})

const typeText = (t) => ({ new: '新客', returning: '回头客', vip: 'VIP', corporate: '企业' }[t] || t)

const resetQuery = () => {
  query.value = { dateFrom: '', dateTo: '', guestType: '', spendLevel: '', source: '', paxMin: null, paxMax: null, frequency: '', prefTable: '', satisfaction: '' }
}
const applyQuery = () => { console.log('Query:', query.value) }
</script>

<style scoped>
.guest-analysis-page { padding: 24px 32px; background: #FAF8F5; min-height: calc(100vh - 108px); }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; font-family: var(--font-family); }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }
.header-actions { display: flex; gap: 8px; }
.btn-primary {
  background: linear-gradient(135deg, #C4A35A, #D4B36A); color: #fff; border: none; padding: 8px 16px;
  border-radius: 2px; font-size: 13px; cursor: pointer; font-weight: 500;
  display: flex; align-items: center; gap: 6px;
  box-shadow: 0 2px 6px rgba(196,163,90,0.3);
}
.btn-primary:hover { background: linear-gradient(135deg, #D4B36A, #E4C37A); }

.query-panel { background: #fff; border-radius: 2px; padding: 20px; border: 1px solid #e8ece9; margin-bottom: 20px; }
.query-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.query-group { display: flex; flex-direction: column; gap: 6px; }
.query-group label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.query-group input, .query-group select {
  padding: 7px 10px; border: 1px solid #d0d8d2; border-radius: 2px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.query-group input:focus, .query-group select:focus { border-color: #C4A35A; }
.date-range, .range-input { display: flex; align-items: center; gap: 6px; }
.date-range span, .range-input span { font-size: 12px; color: #8a9a8e; }
.query-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-secondary { padding: 8px 16px; border-radius: 2px; font-size: 13px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 2px; padding: 18px 20px; border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.stat-icon-wrap svg { width: 48px; height: 48px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }
.trend-up { color: #4A7C59; font-weight: 500; }
.trend-down { color: #C0392B; font-weight: 500; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 2px; padding: 20px; border: 1px solid #e8ece9; }
.chart-card.wide { grid-column: 1 / -1; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0 0 16px 0; font-family: var(--font-family); }
.chart-tabs { display: flex; gap: 4px; }
.chart-tabs button { padding: 4px 12px; border-radius: 2px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
.chart-tabs button.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }

.donut-chart-container { display: flex; align-items: center; gap: 24px; }
.donut-svg-wrap { width: 180px; height: 180px; flex-shrink: 0; }
.donut-svg { width: 100%; height: 100%; }
.donut-segment { transition: stroke-dasharray 0.8s ease; }
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }
.legend-label { flex: 1; color: #3a4a3e; }
.legend-value { color: #6a7a6e; font-weight: 600; }
.legend-pct { font-weight: 400; color: #a0b0a5; font-size: 11px; }

.bar-chart-horizontal { display: flex; flex-direction: column; gap: 14px; }
.h-bar-row { display: flex; align-items: center; gap: 10px; }
.h-bar-label { width: 120px; font-size: 12px; color: #3a4a3e; }
.h-bar-track { flex: 1; height: 10px; background: #f0f2f0; border-radius: 2px; overflow: hidden; }
.h-bar-fill { height: 100%; border-radius: 2px; background: var(--bar-color); position: relative; overflow: hidden; }
.h-bar-shine { position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); animation: barShine 2s ease-in-out infinite; }
@keyframes barShine { 0% { left: -100%; } 100% { left: 100%; } }
.h-bar-value { width: 110px; font-size: 11px; color: #6a7a6e; text-align: right; }

.stacked-bar-chart { display: flex; gap: 8px; align-items: flex-end; height: 220px; padding-bottom: 30px; position: relative; }
.stacked-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.stacked-bars { display: flex; flex-direction: column; width: 100%; height: 180px; border-radius: 2px 2px 0 0; overflow: hidden; }
.stacked-segment { width: 100%; min-height: 2px; background: var(--seg-color); }
.stacked-label { font-size: 11px; color: #8a9a8e; margin-top: 6px; }
.stacked-legend { position: absolute; bottom: 0; left: 0; right: 0; display: flex; gap: 16px; justify-content: center; }

.profile-grid { display: flex; flex-direction: column; gap: 16px; }
.profile-label { font-size: 12px; font-weight: 600; color: #1a2f23; margin-bottom: 8px; }
.profile-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.profile-bar-label { width: 50px; font-size: 11px; color: #6a7a6e; }
.profile-bar-track { flex: 1; height: 8px; background: #f0f2f0; border-radius: 2px; overflow: hidden; }
.profile-bar-fill { height: 100%; border-radius: 2px; background: var(--bar-color); }
.profile-bar-value { width: 36px; font-size: 11px; color: #6a7a6e; text-align: right; }

.vip-list { display: flex; flex-direction: column; gap: 10px; }
.vip-item { display: flex; align-items: center; gap: 10px; padding: 10px; background: #f8f9f8; border-radius: 2px; border: 1px solid #f0f2f0; }
.vip-rank { width: 24px; height: 24px; border-radius: 2px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; background: #e8ece9; color: #6a7a6e; }
.vip-rank.rank-top { background: linear-gradient(135deg, #C4A35A, #D4B36A); color: #fff; }
.vip-avatar { width: 32px; height: 32px; border-radius: 2px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 13px; }
.vip-info { flex: 1; }
.vip-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.vip-meta { font-size: 11px; color: #8a9a8e; }
.vip-spend { font-size: 14px; font-weight: 600; color: #C4A35A; }

.satisfaction-chart { width: 100%; }
.sat-svg { width: 100%; height: 180px; }

.guest-table-card { background: #fff; border-radius: 2px; border: 1px solid #e8ece9; overflow-x: auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e8ece9; }
.card-actions { display: flex; gap: 8px; }
.search-input { padding: 6px 12px; border: 1px solid #d0d8d2; border-radius: 2px; font-size: 13px; outline: none; width: 220px; }
.search-input:focus { border-color: #C4A35A; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e; border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }
.guest-cell { display: flex; align-items: center; gap: 8px; }
.guest-avatar { width: 28px; height: 28px; border-radius: 2px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 600; }
.type-badge { padding: 2px 8px; border-radius: 2px; font-size: 11px; font-weight: 500; }
.type-badge.new { background: rgba(91,123,138,0.1); color: #5B7B8A; }
.type-badge.returning { background: rgba(74,124,89,0.1); color: #4A7C59; }
.type-badge.vip { background: rgba(196,163,90,0.12); color: #b8922e; }
.type-badge.corporate { background: rgba(192,57,43,0.08); color: #C0392B; }
.rating-stars { display: flex; gap: 1px; }
.rating-stars .filled { color: #C4A35A; }
.rating-stars span:not(.filled) { color: #e0e0e0; }
.rating-text { font-size: 13px; font-weight: 600; color: #C4A35A; }
.chart-empty { display: flex; align-items: center; justify-content: center; height: 180px; color: #a0b0a5; font-size: 13px; }
.vip-empty, .spend-empty, .source-empty { color: #a0b0a5; font-size: 13px; padding: 24px; text-align: center; }
</style>
