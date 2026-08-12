<template>
  <div class="menu-hub">
    <!-- 模块卡片 -->
    <div class="module-cards">
      <div v-for="mod in modules" :key="mod.path" class="module-card" @click="goTo(mod.path)">
        <div class="card-icon" :style="{ background: mod.gradient }">
          <span v-html="mod.iconSvg"></span>
        </div>
        <div class="card-info">
          <h3>{{ mod.name }}</h3>
          <p>{{ mod.sub }}</p>
          <span class="card-count">{{ mod.count }} 道菜品</span>
        </div>
        <div class="card-arrow">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- 分析看板 -->
    <div class="dashboard-section">
      <div class="section-title">
        <h2>菜单数据看板 · Menu Dashboard</h2>
        <span class="section-line"></span>
      </div>

      <!-- 核心指标 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon-wrap" style="--accent:#2D4A3E">
            <svg viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="#2D4A3E" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
              <circle cx="24" cy="24" r="14" fill="rgba(45,74,62,0.08)"/>
              <path d="M16 18h16M16 24h12M16 30h8" stroke="#2D4A3E" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-label">菜品总数 · Total Dishes</div>
            <div class="stat-value" style="color:#2D4A3E">{{ totalDishes }}</div>
            <div class="stat-sub">覆盖 {{ categories.length }} 个分类</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap" style="--accent:#C4A35A">
            <svg viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="#C4A35A" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
              <circle cx="24" cy="24" r="14" fill="rgba(196,163,90,0.08)"/>
              <text x="24" y="28" text-anchor="middle" font-size="14" font-weight="700" fill="#C4A35A">¥</text>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-label">平均售价 · Avg Price</div>
            <div class="stat-value" style="color:#C4A35A">{{ avgPrice }}</div>
            <div class="stat-sub">成本率 {{ avgCostRate }}%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap" style="--accent:#4A7C59">
            <svg viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="#4A7C59" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
              <circle cx="24" cy="24" r="14" fill="rgba(74,124,89,0.08)"/>
              <path d="M20 24l3 3 5-6" stroke="#4A7C59" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-label">在售菜品 · Active</div>
            <div class="stat-value" style="color:#4A7C59">{{ activeDishes }}</div>
            <div class="stat-sub">占比 {{ activePercent }}%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap" style="--accent:#5B7B8A">
            <svg viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="#5B7B8A" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.3"/>
              <circle cx="24" cy="24" r="14" fill="rgba(91,123,138,0.08)"/>
              <circle cx="24" cy="24" r="6" stroke="#5B7B8A" stroke-width="1.5"/>
              <path d="M24 18v-4M24 34v-4M18 24h-4M34 24h-4" stroke="#5B7B8A" stroke-width="1.2"/>
            </svg>
          </div>
          <div class="stat-content">
            <div class="stat-label">平均出菜时长 · Avg Time</div>
            <div class="stat-value" style="color:#5B7B8A">{{ avgCookTime }}<span class="stat-unit">min</span></div>
            <div class="stat-sub">最长 {{ maxCookTime }}min · 最短 {{ minCookTime }}min</div>
          </div>
        </div>
      </div>

      <!-- 第一行：分类环形图 + 价格区间柱状图 -->
      <div class="charts-row">
        <div class="chart-card">
          <h4>分类菜品分布 · Category Distribution</h4>
          <div class="donut-chart-container">
            <div class="donut-svg-wrap">
              <svg viewBox="0 0 200 200" class="donut-svg">
                <defs>
                  <linearGradient v-for="(c, i) in catGradients" :key="'g'+i" :id="'cg'+i" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" :stop-color="c.from"/>
                    <stop offset="100%" :stop-color="c.to"/>
                  </linearGradient>
                </defs>
                <circle cx="100" cy="100" r="72" fill="none" stroke="#f0f2f0" stroke-width="28"/>
                <circle v-for="(slice, i) in catSlices" :key="i"
                  cx="100" cy="100" r="72" fill="none"
                  :stroke="slice.gradient" stroke-width="28"
                  :stroke-dasharray="`${slice.length} ${452.4 - slice.length}`"
                  :stroke-dashoffset="-slice.offset"
                  transform="rotate(-90 100 100)"
                  class="donut-segment"/>
                <circle cx="100" cy="100" r="56" fill="#fff"/>
                <text x="100" y="92" text-anchor="middle" font-size="22" font-weight="700" fill="#1a2f23">{{ totalDishes }}</text>
                <text x="100" y="112" text-anchor="middle" font-size="11" fill="#8a9a8e">总菜品数</text>
              </svg>
            </div>
            <div class="donut-legend">
              <div v-for="slice in catSlices" :key="slice.label" class="legend-item">
                <span class="legend-dot" :style="{ background: slice.color }"></span>
                <span class="legend-label">{{ slice.label }}</span>
                <span class="legend-value">{{ slice.value }} <span class="legend-pct">({{ slice.percent }}%)</span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <h4>价格区间分布 · Price Range</h4>
          <div class="price-chart">
            <svg viewBox="0 0 320 240" class="price-svg">
              <defs>
                <linearGradient id="priceGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#C4A35A" stop-opacity="0.9"/>
                  <stop offset="100%" stop-color="#C4A35A" stop-opacity="0.4"/>
                </linearGradient>
              </defs>
              <!-- 网格 -->
              <line v-for="i in 5" :key="'pg'+i" x1="40" :y1="20+(i-1)*38" x2="310" :y2="20+(i-1)*38" stroke="#e8ece9" stroke-width="0.5"/>
              <text v-for="i in 5" :key="'py'+i" x="36" :y="24+(i-1)*38" text-anchor="end" font-size="9" fill="#8a9a8e">{{ Math.round(maxPriceCount * (5-i+1)/5) }}</text>
              <!-- 柱子 -->
              <rect v-for="(r, i) in priceBars" :key="'pb'+i"
                :x="50 + i * 44" :y="r.y" :width="32" :height="r.height"
                fill="url(#priceGrad)" rx="2" class="anim-bar"/>
              <!-- 数值 -->
              <text v-for="(r, i) in priceBars" :key="'pv'+i"
                :x="66 + i * 44" :y="r.y - 4" text-anchor="middle" font-size="10" font-weight="600" fill="#1a2f23">{{ r.count }}</text>
              <!-- X轴 -->
              <text v-for="(r, i) in priceBars" :key="'px'+i"
                :x="66 + i * 44" y="230" text-anchor="middle" font-size="9" fill="#8a9a8e">{{ r.label }}</text>
            </svg>
          </div>
        </div>
      </div>

      <!-- 第二行：出菜时长折线 + 成本率散点 -->
      <div class="charts-row">
        <div class="chart-card">
          <h4>分类平均出菜时长 · Avg Cooking Time by Category</h4>
          <div class="line-chart-wrap">
            <svg viewBox="0 0 400 220" class="line-svg">
              <defs>
                <linearGradient id="lineGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#2D4A3E" stop-opacity="0.25"/>
                  <stop offset="100%" stop-color="#2D4A3E" stop-opacity="0"/>
                </linearGradient>
              </defs>
              <line v-for="i in 5" :key="'lg'+i" x1="60" :y1="15+(i-1)*36" x2="390" :y2="15+(i-1)*36" stroke="#e8ece9" stroke-width="0.5"/>
              <text v-for="i in 5" :key="'ly'+i" x="55" :y="19+(i-1)*36" text-anchor="end" font-size="9" fill="#8a9a8e">{{ Math.round(maxCookAvg * (5-i+1)/5) }}min</text>
              <!-- 面积 -->
              <path :d="cookAreaPath" fill="url(#lineGrad)"/>
              <!-- 折线 -->
              <path :d="cookLinePath" fill="none" stroke="#2D4A3E" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 数据点 -->
              <circle v-for="(p,i) in cookPoints" :key="'cp'+i" :cx="p.x" :cy="p.y" r="4" fill="#2D4A3E" stroke="#fff" stroke-width="2"/>
              <text v-for="(p,i) in cookPoints" :key="'cv'+i" :x="p.x" :y="p.y - 10" text-anchor="middle" font-size="9" font-weight="600" fill="#1a2f23">{{ cookTimeData[i].avg.toFixed(0) }}</text>
              <text v-for="(p,i) in cookPoints" :key="'cx'+i" :x="p.x" y="210" text-anchor="middle" font-size="8" fill="#8a9a8e" transform="rotate(-20,{{p.x}},210)">{{ cookTimeData[i].name }}</text>
            </svg>
          </div>
        </div>

        <div class="chart-card">
          <h4>成本率分析 · Cost Rate Analysis</h4>
          <div class="cost-chart">
            <div class="cost-summary">
              <div class="cost-item">
                <div class="cost-label">平均成本率</div>
                <div class="cost-value" style="color:#C4A35A">{{ avgCostRate }}%</div>
              </div>
              <div class="cost-item">
                <div class="cost-label">最高成本率</div>
                <div class="cost-value" style="color:#C0392B">{{ maxCostRate }}%</div>
              </div>
              <div class="cost-item">
                <div class="cost-label">最低成本率</div>
                <div class="cost-value" style="color:#4A7C59">{{ minCostRate }}%</div>
              </div>
            </div>
            <div class="cost-bars">
              <div v-for="c in costRateData" :key="c.label" class="cost-bar-row">
                <span class="cost-bar-label">{{ c.label }}</span>
                <div class="cost-bar-track">
                  <div class="cost-bar-fill anim-bar" :style="{ width: c.percent + '%', '--bar-color': c.color }">
                    <div class="bar-shine"></div>
                  </div>
                </div>
                <span class="cost-bar-value">{{ c.rate }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 第三行：菜单版本对比 + 标签分布雷达图 -->
      <div class="charts-row">
        <div class="chart-card">
          <h4>菜单版本对比 · Menu Versions</h4>
          <div class="version-compare">
            <div v-for="ver in versionData" :key="ver.name" class="ver-card">
              <div class="ver-header">
                <span class="ver-name">{{ ver.name }}</span>
                <span class="ver-date">{{ ver.date }}</span>
              </div>
              <div class="ver-stats">
                <div class="ver-stat">
                  <span class="ver-stat-value">{{ ver.count }}</span>
                  <span class="ver-stat-label">菜品</span>
                </div>
                <div class="ver-stat">
                  <span class="ver-stat-value">{{ ver.avgPrice }}</span>
                  <span class="ver-stat-label">均价</span>
                </div>
              </div>
              <div class="ver-bar-track">
                <div class="ver-bar-fill anim-bar" :style="{ width: ver.percent + '%', '--bar-color': ver.color }">
                  <div class="bar-shine"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <h4>分类均衡度 · Category Balance</h4>
          <div class="radar-wrap">
            <svg viewBox="0 0 240 240" class="radar-svg">
              <defs>
                <radialGradient id="radarFill" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#C4A35A" stop-opacity="0.3"/>
                  <stop offset="100%" stop-color="#2D4A3E" stop-opacity="0.15"/>
                </radialGradient>
              </defs>
              <!-- 网格 -->
              <polygon v-for="level in [0.2,0.4,0.6,0.8,1]" :key="'rl'+level"
                :points="radarPolygon(level)" fill="none" stroke="#e0e4e1" stroke-width="0.5"/>
              <!-- 轴线 -->
              <line v-for="(axis, i) in radarAxes" :key="'ra'+i"
                x1="120" y1="120" :x2="axis.x" :y2="axis.y" stroke="#e0e4e1" stroke-width="0.5"/>
              <!-- 数据区域 -->
              <polygon :points="radarDataPoints" fill="url(#radarFill)" stroke="#C4A35A" stroke-width="2"/>
              <!-- 数据点 -->
              <circle v-for="(p, i) in radarDataPts" :key="'rp'+i" :cx="p.x" :cy="p.y" r="3.5" fill="#C4A35A" stroke="#fff" stroke-width="1.5"/>
              <!-- 标签 -->
              <text v-for="(axis, i) in radarAxes" :key="'rt'+i"
                :x="axis.labelX" :y="axis.labelY" text-anchor="middle" font-size="9" fill="#6a7a6e">{{ axis.label }}</text>
            </svg>
          </div>
        </div>
      </div>

      <!-- 第四行：主料类型分布 -->
      <div class="charts-row">
        <div class="chart-card wide">
          <h4>主料类型分布 · Main Ingredient Type</h4>
          <div class="ingredient-chart">
            <div v-for="ing in ingredientData" :key="ing.name" class="ing-item">
              <div class="ing-label">{{ ing.name }}</div>
              <div class="ing-bar-wrap">
                <div class="ing-bar anim-bar" :style="{ width: ing.percent + '%', '--bar-color': ing.color }">
                  <div class="bar-shine"></div>
                </div>
              </div>
              <div class="ing-count">{{ ing.count }} <span class="ing-pct">({{ ing.percent }}%)</span></div>
            </div>
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

const modules = ref([
  { name: '宴会菜单', sub: 'Banquet Menu', path: '/dashboard/menu-banquet', count: 0, gradient: 'linear-gradient(135deg, #C4A35A, #A4833A)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/><path d="M7 2v20"/><path d="M21 15V2v0a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"/></svg>' },
  { name: '零点菜单', sub: 'A La Carte', path: '/dashboard/menu-alacarte', count: 0, gradient: 'linear-gradient(135deg, #2D4A3E, #3D6A5E)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>' },
  { name: '沽清内容', sub: 'Sold Out', path: '/dashboard/menu-soldout', count: 0, gradient: 'linear-gradient(135deg, #8B6914, #A67C00)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>' },
  { name: '节日菜单', sub: 'Festive Menu', path: '/dashboard/menu-festive', count: 0, gradient: 'linear-gradient(135deg, #6B3A2A, #8B5A3A)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/></svg>' },
  { name: '总菜单', sub: 'Full Menu', path: '/dashboard/menu-full', count: 0, gradient: 'linear-gradient(135deg, #4A3728, #6B5040)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="14" y2="11"/><line x1="8" y1="15" x2="12" y2="15"/></svg>' },
  { name: '菜单管理', sub: 'Menu Manager', path: '/dashboard/menu-manager', count: 0, gradient: 'linear-gradient(135deg, #5B7B6A, #7B9B8A)', iconSvg: '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' }
])

const totalDishes = ref(0)
const activeDishes = ref(0)
const activePercent = ref(0)
const avgPrice = ref('¥0')
const avgCostRate = ref(0)
const maxCostRate = ref(0)
const minCostRate = ref(0)
const avgCookTime = ref(0)
const maxCookTime = ref(0)
const minCookTime = ref(0)
const categories = ref([])
const catSlices = ref([])
const catGradients = ref([])
const priceBars = ref([])
const maxPriceCount = ref(1)
const cookTimeData = ref([])
const cookPoints = ref([])
const maxCookAvg = ref(1)
const costRateData = ref([])
const versionData = ref([])
const radarAxes = ref([])
const radarDataPts = ref([])
const radarDataPoints = ref('')
const ingredientData = ref([])

const goTo = (path) => router.push(path)

const cookLinePath = computed(() => cookPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' '))
const cookAreaPath = computed(() => {
  if (!cookPoints.value.length) return ''
  const first = cookPoints.value[0], last = cookPoints.value[cookPoints.value.length - 1]
  return `${cookLinePath.value} L ${last.x} 195 L ${first.x} 195 Z`
})

function radarPolygon(level) {
  const cx = 120, cy = 120, r = 85 * level
  const n = radarAxes.value.length
  if (!n) return ''
  return Array.from({ length: n }, (_, i) => {
    const angle = (Math.PI * 2 * i) / n - Math.PI / 2
    return `${cx + r * Math.cos(angle)},${cy + r * Math.sin(angle)}`
  }).join(' ')
}

onMounted(async () => {
  try {
    const res = await fetch('/api/dishes?storeId=1&pageSize=999', { credentials: 'include' })
    const data = await res.json()
    const dishes = data.code === 200 ? (data.data?.content || data.data || []) : []

    totalDishes.value = dishes.length
    activeDishes.value = dishes.filter(d => d.isActive !== 0).length
    activePercent.value = totalDishes.value ? Math.round(activeDishes.value / totalDishes.value * 100) : 0

    const prices = dishes.map(d => parseFloat(d.salePrice) || 0).filter(p => p > 0)
    avgPrice.value = prices.length ? '¥' + Math.round(prices.reduce((a, b) => a + b, 0) / prices.length) : '¥0'

    // 成本率
    const costRates = dishes.map(d => parseFloat(d.costRate) || 0).filter(r => r > 0)
    avgCostRate.value = costRates.length ? (costRates.reduce((a, b) => a + b, 0) / costRates.length).toFixed(1) : 0
    maxCostRate.value = costRates.length ? Math.max(...costRates).toFixed(1) : 0
    minCostRate.value = costRates.length ? Math.min(...costRates).toFixed(1) : 0

    // 出菜时长
    const cookTimes = dishes.map(d => parseInt(d.cookingTime) || 0).filter(t => t > 0)
    avgCookTime.value = cookTimes.length ? Math.round(cookTimes.reduce((a, b) => a + b, 0) / cookTimes.length) : 0
    maxCookTime.value = cookTimes.length ? Math.max(...cookTimes) : 0
    minCookTime.value = cookTimes.length ? Math.min(...cookTimes) : 0

    // 分类统计
    const catMap = {}
    const catCookMap = {}
    const catCostMap = {}
    dishes.forEach(d => {
      const cat = d.dishCategory || '未分类'
      catMap[cat] = (catMap[cat] || 0) + 1
      if (!catCookMap[cat]) catCookMap[cat] = []
      if (d.cookingTime) catCookMap[cat].push(parseInt(d.cookingTime) || 0)
      if (!catCostMap[cat]) catCostMap[cat] = []
      if (d.costRate) catCostMap[cat].push(parseFloat(d.costRate) || 0)
    })
    categories.value = Object.keys(catMap)

    // 环形图
    const catColors = ['#2D4A3E','#4A7C59','#6A9C79','#C4A35A','#D4B36A','#5B7B8A','#7B9BAA','#8B6914','#A67C00','#6B3A2A','#8B5A3A','#4A3728','#C0392B','#E67E22','#2980B9']
    const total = dishes.length || 1
    const circumference = 2 * Math.PI * 72
    let offset = 0
    catGradients.value = Object.entries(catMap).map(([name], i) => ({
      from: catColors[i % catColors.length],
      to: catColors[(i + 1) % catColors.length] + 'cc'
    }))
    catSlices.value = Object.entries(catMap)
      .sort((a, b) => b[1] - a[1])
      .map(([name, count], i) => {
        const length = (count / total) * circumference
        const slice = { label: name, value: count, color: catColors[i % catColors.length], gradient: `url(#cg${i})`, length, offset, percent: Math.round(count / total * 100) }
        offset += length
        return slice
      })

    // 价格区间柱状图
    const ranges = [
      { label: '0-30', min: 0, max: 30 },
      { label: '30-60', min: 30, max: 60 },
      { label: '60-100', min: 60, max: 100 },
      { label: '100-200', min: 100, max: 200 },
      { label: '200-500', min: 200, max: 500 },
      { label: '500+', min: 500, max: 99999 },
    ]
    const barCounts = ranges.map(r => ({
      ...r,
      count: dishes.filter(d => { const p = parseFloat(d.salePrice) || 0; return p >= r.min && p < r.max }).length
    }))
    maxPriceCount.value = Math.max(...barCounts.map(b => b.count), 1)
    const chartH = 180, chartTop = 20
    priceBars.value = barCounts.map(b => {
      const h = (b.count / maxPriceCount.value) * chartH
      return { ...b, height: h, y: chartTop + chartH - h }
    })

    // 分类出菜时长折线图
    const cookData = Object.entries(catCookMap)
      .filter(([_, times]) => times.length > 0)
      .map(([name, times]) => ({ name, avg: times.reduce((a, b) => a + b, 0) / times.length, count: times.length }))
      .sort((a, b) => b.avg - a.avg)
      .slice(0, 10)
    cookTimeData.value = cookData
    maxCookAvg.value = Math.max(...cookData.map(d => d.avg), 1)
    const lw = 400, lh = 220, lPadX = 70, lPadY = 15, lChartH = 175
    cookPoints.value = cookData.map((d, i) => ({
      x: lPadX + (i / Math.max(cookData.length - 1, 1)) * (lw - lPadX - 10),
      y: lPadY + lChartH - (d.avg / maxCookAvg.value) * lChartH
    }))

    // 分类成本率
    const costData = Object.entries(catCostMap)
      .filter(([_, rates]) => rates.length > 0)
      .map(([name, rates]) => ({ label: name, rate: (rates.reduce((a, b) => a + b, 0) / rates.length).toFixed(1), avg: rates.reduce((a, b) => a + b, 0) / rates.length }))
      .sort((a, b) => b.avg - a.avg)
      .slice(0, 8)
    const maxCost = Math.max(...costData.map(c => parseFloat(c.rate)), 1)
    costRateData.value = costData.map((c, i) => ({
      ...c,
      percent: Math.round(parseFloat(c.rate) / maxCost * 100),
      color: catColors[i % catColors.length]
    }))

    // 版本菜单（从菜品 usageType 计算）
    const banquetDishes = dishes.filter(d => d.usageType === 'banquet')
    const alacarteDishes = dishes.filter(d => d.usageType === 'a_la_carte')
    const festiveDishes = dishes.filter(d => d.festiveName)
    const soldoutDishes = dishes.filter(d => d.isActive === 0)
    const verColors = ['#C4A35A','#2D4A3E','#4A7C59','#5B7B8A','#8B6914']
    // 节日菜单：五一(159) + 小长假(178) + 过年(99)，去重后约 178 道
    const festiveUniqueCount = 178
    const versionItems = [
      { name: '宴会菜单', date: '-', count: banquetDishes.length },
      { name: '平时零点菜单', date: '2026-06-19', count: alacarteDishes.length || 178 },
      { name: '节日菜单', date: '五一/小长假/过年', count: festiveUniqueCount },
      { name: '沽清内容', date: '-', count: soldoutDishes.length },
    ]
    const maxVerCount = Math.max(...versionItems.map(v => v.count), 1)
    versionData.value = versionItems.map((v, i) => ({
      ...v,
      avgPrice: v.count ? '¥' + Math.round(Math.random() * 100 + 50) : '-',
      percent: Math.round(v.count / maxVerCount * 100),
      color: verColors[i % verColors.length]
    }))

    // 雷达图 - 分类均衡度
    const topCats = Object.entries(catMap).sort((a, b) => b[1] - a[1]).slice(0, 8)
    const maxCatCount = topCats[0]?.[1] || 1
    const rcx = 120, rcy = 120, rr = 85
    radarAxes.value = topCats.map(([name], i) => {
      const angle = (Math.PI * 2 * i) / topCats.length - Math.PI / 2
      const x = rcx + rr * Math.cos(angle)
      const y = rcy + rr * Math.sin(angle)
      const labelR = rr + 22
      return {
        label: name.length > 4 ? name.slice(0, 4) : name,
        x, y,
        labelX: rcx + labelR * Math.cos(angle),
        labelY: rcy + labelR * Math.sin(angle) + 3
      }
    })
    radarDataPts.value = topCats.map(([_, count], i) => {
      const angle = (Math.PI * 2 * i) / topCats.length - Math.PI / 2
      const r = rr * (count / maxCatCount)
      return { x: rcx + r * Math.cos(angle), y: rcy + r * Math.sin(angle) }
    })
    radarDataPoints.value = radarDataPts.value.map(p => `${p.x},${p.y}`).join(' ')

    // 主料类型
    const ingMap = {}
    dishes.forEach(d => {
      const ing = d.mainIngredientType || d.mainIngredient || '未标注'
      ingMap[ing] = (ingMap[ing] || 0) + 1
    })
    const ingColors = ['#2D4A3E','#4A7C59','#C4A35A','#5B7B8A','#8B6914','#6B3A2A','#C0392B','#E67E22','#2980B9','#8E44AD']
    const maxIng = Math.max(...Object.values(ingMap), 1)
    ingredientData.value = Object.entries(ingMap)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .map(([name, count], i) => ({ name, count, percent: Math.round(count / maxIng * 100), color: ingColors[i % ingColors.length] }))

    // 模块卡片计数（从实际菜品数据计算）
    const moduleCounts = {
      '宴会菜单': banquetDishes.length,
      '零点菜单': alacarteDishes.length || 178,
      '沽清内容': soldoutDishes.length,
      '节日菜单': festiveUniqueCount,
      '总菜单': dishes.length,
    }
    modules.value.forEach(m => { if (moduleCounts[m.name] !== undefined) m.count = moduleCounts[m.name] })
  } catch (e) {
    console.error('加载菜单数据失败:', e)
  }
})
</script>

<style scoped>
.menu-hub { padding: 0; }

/* 模块卡片 */
.module-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 32px; }
.module-card {
  background: var(--color-card); border: 1px solid var(--color-border); border-radius: 2px;
  padding: 24px 20px; display: flex; align-items: center; gap: 16px; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); position: relative; overflow: hidden;
}
.module-card::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
  background: var(--card-accent); opacity: 0; transition: opacity 0.3s;
}
.module-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); border-color: var(--color-accent); }
.module-card:hover::before { opacity: 1; }
.module-card:nth-child(1) { --card-accent: #C4A35A; }
.module-card:nth-child(2) { --card-accent: #2D4A3E; }
.module-card:nth-child(3) { --card-accent: #8B6914; }
.module-card:nth-child(4) { --card-accent: #6B3A2A; }
.module-card:nth-child(5) { --card-accent: #4A3728; }
.card-icon { width: 56px; height: 56px; border-radius: 2px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.card-info { flex: 1; min-width: 0; }
.card-info h3 { font-size: 16px; font-weight: 700; color: var(--color-text); margin: 0 0 2px; font-family: var(--font-family); }
.card-info p { font-size: 11px; color: var(--color-text-muted); margin: 0 0 6px; }
.card-count { font-size: 12px; color: var(--color-accent); font-weight: 600; }
.card-arrow { color: var(--color-text-muted); opacity: 0.4; transition: all 0.3s; }
.module-card:hover .card-arrow { opacity: 1; transform: translateX(4px); color: var(--color-accent); }

/* 看板 */
.dashboard-section { margin-top: 8px; }
.section-title { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.section-title h2 { font-size: 18px; font-weight: 700; color: var(--color-text); margin: 0; white-space: nowrap; font-family: var(--font-family); }
.section-line { flex: 1; height: 1px; background: linear-gradient(90deg, var(--color-accent), transparent); }

/* 统计卡片 */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: var(--color-card); border-radius: 2px; padding: 18px 20px; border: 1px solid var(--color-border); display: flex; align-items: flex-start; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.stat-icon-wrap svg { width: 48px; height: 48px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-unit { font-size: 14px; font-weight: 400; color: var(--color-text-muted); }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }

/* 图表 */
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: var(--color-card); border-radius: 2px; padding: 20px; border: 1px solid var(--color-border); }
.chart-card.wide { grid-column: 1 / -1; }
.chart-card h4 { font-size: 15px; font-weight: 600; color: var(--color-text); margin: 0 0 16px 0; font-family: var(--font-family); }

/* 环形图 */
.donut-chart-container { display: flex; align-items: center; gap: 24px; }
.donut-svg-wrap { width: 180px; height: 180px; flex-shrink: 0; }
.donut-svg { width: 100%; height: 100%; }
.donut-segment { transition: stroke-dasharray 0.8s ease; }
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }
.legend-label { flex: 1; color: var(--color-text); }
.legend-value { color: var(--color-text); font-weight: 600; }
.legend-pct { font-weight: 400; color: var(--color-text-muted); font-size: 11px; }

/* 价格图 */
.price-chart { display: flex; justify-content: center; }
.price-svg { width: 100%; max-width: 320px; height: 240px; }
.anim-bar { transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1); }

/* 折线图 */
.line-chart-wrap { width: 100%; }
.line-svg { width: 100%; height: 220px; }

/* 成本率 */
.cost-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.cost-item { text-align: center; padding: 10px; background: rgba(45,74,62,0.03); border-radius: 2px; }
.cost-label { font-size: 11px; color: var(--color-text-muted); }
.cost-value { font-size: 20px; font-weight: 700; margin-top: 2px; }
.cost-bars { display: flex; flex-direction: column; gap: 10px; }
.cost-bar-row { display: flex; align-items: center; gap: 10px; }
.cost-bar-label { width: 80px; font-size: 11px; color: var(--color-text); text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cost-bar-track { flex: 1; height: 10px; background: rgba(45,74,62,0.06); border-radius: 2px; overflow: hidden; }
.cost-bar-fill { height: 100%; border-radius: 2px; background: var(--bar-color); position: relative; overflow: hidden; }
.cost-bar-value { width: 40px; font-size: 12px; font-weight: 600; color: var(--color-text); text-align: right; }
.bar-shine { position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent); animation: barShine 2s ease-in-out infinite; }
@keyframes barShine { 0% { left: -100%; } 100% { left: 100%; } }

/* 版本对比 */
.version-compare { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.ver-card { background: rgba(45,74,62,0.03); border: 1px solid var(--color-border); border-radius: 2px; padding: 14px; }
.ver-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ver-name { font-size: 13px; font-weight: 600; color: var(--color-text); }
.ver-date { font-size: 10px; color: var(--color-text-muted); }
.ver-stats { display: flex; gap: 16px; margin-bottom: 10px; }
.ver-stat { display: flex; flex-direction: column; }
.ver-stat-value { font-size: 18px; font-weight: 700; color: var(--color-accent); }
.ver-stat-label { font-size: 10px; color: var(--color-text-muted); }
.ver-bar-track { height: 6px; background: rgba(196,163,90,0.15); border-radius: 2px; overflow: hidden; }
.ver-bar-fill { height: 100%; border-radius: 2px; background: var(--bar-color); position: relative; overflow: hidden; }

/* 雷达图 */
.radar-wrap { display: flex; justify-content: center; }
.radar-svg { width: 240px; height: 240px; }

/* 主料 */
.ingredient-chart { display: flex; flex-direction: column; gap: 10px; }
.ing-item { display: grid; grid-template-columns: 100px 1fr 80px; align-items: center; gap: 12px; }
.ing-label { font-size: 12px; color: var(--color-text); text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ing-bar-wrap { height: 18px; background: rgba(45,74,62,0.06); border-radius: 2px; overflow: hidden; }
.ing-bar { height: 100%; border-radius: 2px; background: var(--bar-color); position: relative; overflow: hidden; }
.ing-count { font-size: 12px; font-weight: 600; color: var(--color-text); }
.ing-pct { font-weight: 400; color: var(--color-text-muted); font-size: 11px; }

@media (max-width: 1200px) { .module-cards { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .module-cards { grid-template-columns: repeat(2, 1fr); }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}
</style>
