<template>
  <div class="guest-analysis-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">客人分析 · Guest Analysis</h2>
        <p class="page-subtitle">Guest profiling, behavior analysis and segmentation</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="showQueryPanel = !showQueryPanel">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          筛选
        </button>
      </div>
    </div>

    <!-- 筛选面板 -->
    <div v-if="showQueryPanel" class="query-panel">
      <div class="query-row">
        <div class="query-group">
          <label>日期范围</label>
          <div class="date-range">
            <input type="date" v-model="query.dateFrom" />
            <span>至</span>
            <input type="date" v-model="query.dateTo" />
          </div>
        </div>
        <div class="query-group">
          <label>客人类型</label>
          <select v-model="query.guestType">
            <option value="">全部</option>
            <option value="new">新客</option>
            <option value="returning">回头客</option>
            <option value="vip">VIP</option>
            <option value="corporate">企业客户</option>
          </select>
        </div>
        <div class="query-group">
          <label>消费等级</label>
          <select v-model="query.spendLevel">
            <option value="">全部</option>
            <option value="low">低 (¥0-500)</option>
            <option value="mid">中 (¥500-2000)</option>
            <option value="high">高 (¥2000-5000)</option>
            <option value="luxury">超高 (¥5000+)</option>
          </select>
        </div>
        <div class="query-group">
          <label>来源渠道</label>
          <select v-model="query.source">
            <option value="">全部</option>
            <option value="walk-in">自来</option>
            <option value="phone">电话</option>
            <option value="online">线上</option>
            <option value="member">会员推荐</option>
          </select>
        </div>
      </div>
      <div class="query-row">
        <div class="query-group">
          <label>人数范围</label>
          <div class="range-input">
            <input type="number" v-model.number="query.paxMin" placeholder="最少" />
            <span>至</span>
            <input type="number" v-model.number="query.paxMax" placeholder="最多" />
          </div>
        </div>
        <div class="query-group">
          <label>用餐频次</label>
          <select v-model="query.frequency">
            <option value="">全部</option>
            <option value="1">仅1次</option>
            <option value="2-3">2-3次</option>
            <option value="4-6">4-6次</option>
            <option value="7+">7次以上</option>
          </select>
        </div>
        <div class="query-group">
          <label>偏好桌台</label>
          <select v-model="query.prefTable">
            <option value="">全部</option>
            <option value="hall">大厅</option>
            <option value="private">包厢</option>
            <option value="vip">VIP</option>
          </select>
        </div>
        <div class="query-group">
          <label>满意度</label>
          <select v-model="query.satisfaction">
            <option value="">全部</option>
            <option value="5">★★★★★</option>
            <option value="4">★★★★</option>
            <option value="3">★★★</option>
            <option value="2">★★</option>
            <option value="1">★</option>
          </select>
        </div>
      </div>
      <div class="query-actions">
        <button class="btn-secondary" @click="resetQuery">重置</button>
        <button class="btn-primary" @click="applyQuery">查询</button>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent:#2D4A3E">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="20" stroke="#2D4A3E" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
            <circle cx="24" cy="24" r="14" fill="rgba(45,74,62,0.08)"/>
            <path d="M18 28v-4a6 6 0 0 1 12 0v4" stroke="#2D4A3E" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="24" cy="18" r="4" stroke="#2D4A3E" stroke-width="1.8"/>
            <circle cx="34" cy="22" r="3" stroke="#2D4A3E" stroke-width="1.5" opacity="0.6"/>
            <circle cx="14" cy="22" r="3" stroke="#2D4A3E" stroke-width="1.5" opacity="0.6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总客人数 · Total Guests</div>
          <div class="stat-value" style="color:#2D4A3E">1,286</div>
          <div class="stat-sub"><span class="trend-up">↑ 8.2%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent:#C4A35A">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="20" stroke="#C4A35A" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
            <circle cx="24" cy="24" r="14" fill="rgba(196,163,90,0.08)"/>
            <path d="M24 14v10l6 4" stroke="#C4A35A" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="24" cy="24" r="14" stroke="#C4A35A" stroke-width="1.5" opacity="0.4"/>
            <path d="M24 10v2M24 36v2M10 24h2M36 24h2" stroke="#C4A35A" stroke-width="1.2" opacity="0.4"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">回头客比例 · Return Rate</div>
          <div class="stat-value" style="color:#C4A35A">42%</div>
          <div class="stat-sub"><span class="trend-up">↑ 3.1%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent:#4A7C59">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="20" stroke="#4A7C59" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
            <circle cx="24" cy="24" r="14" fill="rgba(74,124,89,0.08)"/>
            <rect x="16" y="20" width="4" height="12" rx="1" fill="#4A7C59" opacity="0.6"/>
            <rect x="22" y="14" width="4" height="18" rx="1" fill="#4A7C59" opacity="0.8"/>
            <rect x="28" y="17" width="4" height="15" rx="1" fill="#4A7C59"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">平均消费 · Avg Spend</div>
          <div class="stat-value" style="color:#4A7C59">¥680</div>
          <div class="stat-sub"><span class="trend-down">↓ 2.1%</span> 较上月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent:#C4A35A">
          <svg viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="20" stroke="#C4A35A" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
            <circle cx="24" cy="24" r="14" fill="rgba(196,163,90,0.08)"/>
            <polygon points="24,12 26.5,19 34,19 28,23.5 30,30.5 24,26.5 18,30.5 20,23.5 14,19 21.5,19" fill="#C4A35A" opacity="0.8"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">平均评分 · Avg Rating</div>
          <div class="stat-value" style="color:#C4A35A">4.6</div>
          <div class="stat-sub">共 892 条评价</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <!-- 客人类型分布 - 环形图 -->
      <div class="chart-card">
        <h3 class="section-title">客人类型分布 · Guest Type Distribution</h3>
        <div class="donut-chart-container">
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
              <text x="100" y="92" text-anchor="middle" font-size="22" font-weight="700" fill="#1a2f23">1,286</text>
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
        <div class="bar-chart-horizontal">
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
            <button :class="{ active: sourcePeriod === 'week' }" @click="sourcePeriod = 'week'">本周</button>
            <button :class="{ active: sourcePeriod === 'month' }" @click="sourcePeriod = 'month'">本月</button>
          </div>
        </div>
        <div class="stacked-bar-chart">
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
        <div class="profile-grid">
          <div class="profile-item">
            <div class="profile-label">性别分布</div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">男</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:58%;--bar-color:#5B7B8A"></div>
              </div>
              <span class="profile-bar-value">58%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">女</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:42%;--bar-color:#C4A35A"></div>
              </div>
              <span class="profile-bar-value">42%</span>
            </div>
          </div>
          <div class="profile-item">
            <div class="profile-label">年龄段</div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">18-25</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:15%;--bar-color:#2D4A3E"></div>
              </div>
              <span class="profile-bar-value">15%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">26-35</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:35%;--bar-color:#4A7C59"></div>
              </div>
              <span class="profile-bar-value">35%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">36-50</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:32%;--bar-color:#C4A35A"></div>
              </div>
              <span class="profile-bar-value">32%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">50+</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:18%;--bar-color:#5B7B8A"></div>
              </div>
              <span class="profile-bar-value">18%</span>
            </div>
          </div>
          <div class="profile-item">
            <div class="profile-label">用餐人数</div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">1-2人</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:25%;--bar-color:#2D4A3E"></div>
              </div>
              <span class="profile-bar-value">25%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">3-4人</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:30%;--bar-color:#4A7C59"></div>
              </div>
              <span class="profile-bar-value">30%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">5-8人</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:28%;--bar-color:#C4A35A"></div>
              </div>
              <span class="profile-bar-value">28%</span>
            </div>
            <div class="profile-bar-row">
              <span class="profile-bar-label">8人以上</span>
              <div class="profile-bar-track">
                <div class="profile-bar-fill anim-bar" style="width:17%;--bar-color:#5B7B8A"></div>
              </div>
              <span class="profile-bar-value">17%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- VIP客人排行 -->
      <div class="chart-card">
        <h3 class="section-title">VIP客人排行 · Top VIP Guests</h3>
        <div class="vip-list">
          <div v-for="(g, i) in vipGuests" :key="g.id" class="vip-item">
            <div class="vip-rank" :class="{ 'rank-top': i < 3 }">{{ i + 1 }}</div>
            <div class="vip-avatar" :style="{ background: `linear-gradient(135deg, ${g.color}, ${g.color}dd)` }">{{ g.name[0] }}</div>
            <div class="vip-info">
              <div class="vip-name">{{ g.name }}</div>
              <div class="vip-meta">{{ g.visits }}次消费 · {{ g.lastVisit }}</div>
            </div>
            <div class="vip-spend">¥{{ g.totalSpend.toLocaleString() }}</div>
          </div>
        </div>
      </div>

      <!-- 满意度趋势 -->
      <div class="chart-card wide">
        <h3 class="section-title">满意度趋势 · Satisfaction Trend</h3>
        <div class="satisfaction-chart">
          <svg viewBox="0 0 700 180" class="sat-svg">
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
          <input type="text" v-model="guestSearch" placeholder="搜索客人姓名/电话..." class="search-input" />
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>客人</th>
            <th>电话</th>
            <th>类型</th>
            <th>消费次数</th>
            <th>累计消费</th>
            <th>平均消费</th>
            <th>偏好桌台</th>
            <th>最近到访</th>
            <th>评分</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in filteredGuests" :key="g.id">
            <td>
              <div class="guest-cell">
                <div class="guest-avatar" :style="{ background: `linear-gradient(135deg, ${g.color}, ${g.color}dd)` }">{{ g.name[0] }}</div>
                <span>{{ g.name }}</span>
              </div>
            </td>
            <td>{{ g.phone }}</td>
            <td><span :class="['type-badge', g.type]">{{ typeText(g.type) }}</span></td>
            <td>{{ g.visits }}</td>
            <td>¥{{ g.totalSpend.toLocaleString() }}</td>
            <td>¥{{ Math.round(g.totalSpend / g.visits) }}</td>
            <td>{{ g.prefTable }}</td>
            <td>{{ g.lastVisit }}</td>
            <td>
              <span class="rating-stars">
                <span v-for="s in 5" :key="s" :class="{ filled: s <= g.rating }">★</span>
              </span>
            </td>
          </tr>
        </tbody>
      </table>
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

const guestTypeSlices = computed(() => {
  const data = [
    { label: '新客', value: 420, color: '#2D4A3E', gradient: 'url(#dg1)' },
    { label: '回头客', value: 540, color: '#4A7C59', gradient: 'url(#dg2)' },
    { label: 'VIP', value: 226, color: '#C4A35A', gradient: 'url(#dg3)' },
    { label: '企业客户', value: 100, color: '#5B7B8A', gradient: 'url(#dg4)' },
  ]
  const total = 1286
  const circumference = 2 * Math.PI * 72 // ~452.4
  let offset = 0
  return data.map(d => {
    const length = (d.value / total) * circumference
    const slice = { ...d, length, offset, percent: Math.round(d.value / total * 100) }
    offset += length
    return slice
  })
})

const spendLevelData = ref([
  { level: '低 (¥0-500)', count: 380, percent: 30, color: '#5B7B8A' },
  { level: '中 (¥500-2000)', count: 520, percent: 40, color: '#4A7C59' },
  { level: '高 (¥2000-5000)', count: 280, percent: 22, color: '#C4A35A' },
  { level: '超高 (¥5000+)', count: 106, percent: 8, color: '#C0392B' },
])

const sourceTrendData = ref([
  { label: '周一', segments: [
    { source: '自来', value: 8, height: 32, color: '#2D4A3E' },
    { source: '电话', value: 6, height: 24, color: '#4A7C59' },
    { source: '线上', value: 4, height: 16, color: '#C4A35A' },
    { source: '会员', value: 2, height: 8, color: '#5B7B8A' },
  ]},
  { label: '周二', segments: [
    { source: '自来', value: 6, height: 24, color: '#2D4A3E' },
    { source: '电话', value: 7, height: 28, color: '#4A7C59' },
    { source: '线上', value: 5, height: 20, color: '#C4A35A' },
    { source: '会员', value: 3, height: 12, color: '#5B7B8A' },
  ]},
  { label: '周三', segments: [
    { source: '自来', value: 10, height: 40, color: '#2D4A3E' },
    { source: '电话', value: 5, height: 20, color: '#4A7C59' },
    { source: '线上', value: 3, height: 12, color: '#C4A35A' },
    { source: '会员', value: 2, height: 8, color: '#5B7B8A' },
  ]},
  { label: '周四', segments: [
    { source: '自来', value: 7, height: 28, color: '#2D4A3E' },
    { source: '电话', value: 8, height: 32, color: '#4A7C59' },
    { source: '线上', value: 6, height: 24, color: '#C4A35A' },
    { source: '会员', value: 4, height: 16, color: '#5B7B8A' },
  ]},
  { label: '周五', segments: [
    { source: '自来', value: 12, height: 36, color: '#2D4A3E' },
    { source: '电话', value: 10, height: 30, color: '#4A7C59' },
    { source: '线上', value: 8, height: 24, color: '#C4A35A' },
    { source: '会员', value: 5, height: 15, color: '#5B7B8A' },
  ]},
  { label: '周六', segments: [
    { source: '自来', value: 15, height: 35, color: '#2D4A3E' },
    { source: '电话', value: 12, height: 28, color: '#4A7C59' },
    { source: '线上', value: 10, height: 24, color: '#C4A35A' },
    { source: '会员', value: 6, height: 14, color: '#5B7B8A' },
  ]},
  { label: '周日', segments: [
    { source: '自来', value: 14, height: 34, color: '#2D4A3E' },
    { source: '电话', value: 11, height: 27, color: '#4A7C59' },
    { source: '线上', value: 9, height: 22, color: '#C4A35A' },
    { source: '会员', value: 7, height: 17, color: '#5B7B8A' },
  ]},
])

const sourceLegend = [
  { source: '自来', color: '#2D4A3E' },
  { source: '电话', color: '#4A7C59' },
  { source: '线上', color: '#C4A35A' },
  { source: '会员', color: '#5B7B8A' },
]

const vipGuests = ref([
  { id: 1, name: '王建国', color: '#2D4A3E', visits: 48, totalSpend: 156000, lastVisit: '2026-07-08' },
  { id: 2, name: '李美华', color: '#C4A35A', visits: 36, totalSpend: 98000, lastVisit: '2026-07-07' },
  { id: 3, name: '张志强', color: '#4A7C59', visits: 28, totalSpend: 72000, lastVisit: '2026-07-06' },
  { id: 4, name: '陈秀英', color: '#5B7B8A', visits: 22, totalSpend: 58000, lastVisit: '2026-07-05' },
  { id: 5, name: '刘大明', color: '#C0392B', visits: 18, totalSpend: 45000, lastVisit: '2026-07-04' },
])

const satisfactionData = ref([
  { month: '1月', avgRating: 4.3, count: 68 },
  { month: '2月', avgRating: 4.4, count: 72 },
  { month: '3月', avgRating: 4.5, count: 75 },
  { month: '4月', avgRating: 4.4, count: 80 },
  { month: '5月', avgRating: 4.6, count: 85 },
  { month: '6月', avgRating: 4.6, count: 92 },
  { month: '7月', avgRating: 4.7, count: 45 },
])

const satPoints = computed(() => {
  const w = 700, h = 180, padX = 60, padY = 20, chartH = 130
  const maxVal = 5
  return satisfactionData.value.map((d, i) => ({
    x: padX + (i / (satisfactionData.value.length - 1)) * (w - padX * 2),
    y: padY + chartH - (d.avgRating / maxVal) * chartH
  }))
})

const satLinePath = computed(() => satPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' '))
const satAreaPath = computed(() => {
  const first = satPoints.value[0], last = satPoints.value[satPoints.value.length - 1]
  return `${satLinePath.value} L ${last.x} 160 L ${first.x} 160 Z`
})

const guests = ref([
  { id: 1, name: '王建国', phone: '138****1234', type: 'vip', visits: 48, totalSpend: 156000, prefTable: 'VIP-1', lastVisit: '2026-07-08', rating: 5, color: '#2D4A3E' },
  { id: 2, name: '李美华', phone: '139****5678', type: 'vip', visits: 36, totalSpend: 98000, prefTable: '牡丹厅', lastVisit: '2026-07-07', rating: 5, color: '#C4A35A' },
  { id: 3, name: '张志强', phone: '137****9012', type: 'returning', visits: 28, totalSpend: 72000, prefTable: '3号桌', lastVisit: '2026-07-06', rating: 4, color: '#4A7C59' },
  { id: 4, name: '陈秀英', phone: '136****3456', type: 'returning', visits: 22, totalSpend: 58000, prefTable: '荷花厅', lastVisit: '2026-07-05', rating: 5, color: '#5B7B8A' },
  { id: 5, name: '刘大明', phone: '135****7890', type: 'corporate', visits: 18, totalSpend: 45000, prefTable: '宴会厅', lastVisit: '2026-07-04', rating: 4, color: '#C0392B' },
  { id: 6, name: '赵丽娜', phone: '133****2345', type: 'new', visits: 1, totalSpend: 680, prefTable: '5号桌', lastVisit: '2026-07-09', rating: 4, color: '#2D4A3E' },
  { id: 7, name: '孙伟', phone: '131****6789', type: 'returning', visits: 12, totalSpend: 28000, prefTable: '2号桌', lastVisit: '2026-07-03', rating: 4, color: '#4A7C59' },
  { id: 8, name: '周婷', phone: '132****0123', type: 'new', visits: 2, totalSpend: 1500, prefTable: '6号桌', lastVisit: '2026-07-02', rating: 3, color: '#C4A35A' },
])

const filteredGuests = computed(() => {
  if (!guestSearch.value) return guests.value
  const q = guestSearch.value.toLowerCase()
  return guests.value.filter(g => g.name.includes(q) || g.phone.includes(q))
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
</style>
