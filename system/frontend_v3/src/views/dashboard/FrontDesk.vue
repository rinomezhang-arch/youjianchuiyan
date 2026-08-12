<template>
  <div class="front-desk-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">前台预定 · Front Desk Booking</h2>
        <p class="page-subtitle">Multi-dimensional booking analysis and management</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="showQueryPanel = !showQueryPanel">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          高级查询
        </button>
        <button class="btn-primary" @click="exportReport">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          导出报表
        </button>
      </div>
    </div>

    <!-- 高级查询面板 -->
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
          <label>时段</label>
          <select v-model="query.timeSlot">
            <option value="">全部时段</option>
            <option value="lunch">午餐 (11:00-14:00)</option>
            <option value="dinner">晚餐 (17:00-21:00)</option>
            <option value="late">夜宵 (21:00-02:00)</option>
          </select>
        </div>
        <div class="query-group">
          <label>桌台类型</label>
          <select v-model="query.tableType">
            <option value="">全部类型</option>
            <option value="hall">大厅散台</option>
            <option value="private">包厢</option>
            <option value="vip">VIP包房</option>
            <option value="banquet">宴会厅</option>
          </select>
        </div>
        <div class="query-group">
          <label>预订状态</label>
          <select v-model="query.status">
            <option value="">全部状态</option>
            <option value="confirmed">已确认</option>
            <option value="pending">待确认</option>
            <option value="cancelled">已取消</option>
            <option value="completed">已完成</option>
          </select>
        </div>
      </div>
      <div class="query-row">
        <div class="query-group">
          <label>客人来源</label>
          <select v-model="query.source">
            <option value="">全部来源</option>
            <option value="walk-in">自来客</option>
            <option value="phone">电话预订</option>
            <option value="online">线上平台</option>
            <option value="member">会员推荐</option>
            <option value="corporate">企业客户</option>
          </select>
        </div>
        <div class="query-group">
          <label>人数范围</label>
          <div class="range-input">
            <input type="number" v-model.number="query.paxMin" placeholder="最少" min="1" />
            <span>至</span>
            <input type="number" v-model.number="query.paxMax" placeholder="最多" />
          </div>
        </div>
        <div class="query-group">
          <label>消费金额</label>
          <div class="range-input">
            <input type="number" v-model.number="query.amountMin" placeholder="最低" min="0" />
            <span>至</span>
            <input type="number" v-model.number="query.amountMax" placeholder="最高" />
          </div>
        </div>
        <div class="query-group">
          <label>接待员工</label>
          <select v-model="query.staff">
            <option value="">全部员工</option>
            <option v-for="s in staffList" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
      </div>
      <div class="query-actions">
        <button class="btn-secondary" @click="resetQuery">重置</button>
        <button class="btn-primary" @click="applyQuery">查询</button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日预订 · Today</div>
          <div class="stat-value" style="color:#2D4A3E">{{ stats.todayBookings }}</div>
          <div class="stat-sub">
            <span :class="{ 'trend-up': stats.bookingTrend > 0, 'trend-down': stats.bookingTrend < 0 }">
              {{ stats.bookingTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.bookingTrend) }}%
            </span>
            较昨日
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(212,168,83,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日营收 · Revenue</div>
          <div class="stat-value" style="color:#D4A853">¥{{ stats.todayRevenue.toLocaleString() }}</div>
          <div class="stat-sub">
            <span :class="{ 'trend-up': stats.revenueTrend > 0, 'trend-down': stats.revenueTrend < 0 }">
              {{ stats.revenueTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.revenueTrend) }}%
            </span>
            较昨日
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(91,123,138,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">桌台利用率 · Occupancy</div>
          <div class="stat-value" style="color:#5B7B8A">{{ stats.occupancyRate }}%</div>
          <div class="stat-sub">
            {{ stats.occupiedTables }} / {{ stats.totalTables }} 桌在用
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(192,57,43,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">人均消费 · Avg Spend</div>
          <div class="stat-value" style="color:#C0392B">¥{{ stats.avgSpend }}</div>
          <div class="stat-sub">
            <span :class="{ 'trend-up': stats.spendTrend > 0, 'trend-down': stats.spendTrend < 0 }">
              {{ stats.spendTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.spendTrend) }}%
            </span>
            较上周
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 营收趋势 -->
      <div class="chart-card wide">
        <div class="chart-header">
          <h3 class="section-title">营收趋势 · Revenue Trend</h3>
          <div class="chart-tabs">
            <button :class="{ active: revenuePeriod === 'week' }" @click="revenuePeriod = 'week'">本周</button>
            <button :class="{ active: revenuePeriod === 'month' }" @click="revenuePeriod = 'month'">本月</button>
            <button :class="{ active: revenuePeriod === 'year' }" @click="revenuePeriod = 'year'">本年</button>
          </div>
        </div>
        <div class="chart-area">
          <svg :viewBox="`0 0 ${revenueChartWidth} 200`" class="line-chart">
            <!-- 网格线 -->
            <line v-for="i in 5" :key="'h'+i" :x1="40" :y1="20 + (i-1)*35" :x2="revenueChartWidth-10" :y2="20 + (i-1)*35" stroke="#e8ece9" stroke-width="1"/>
            <!-- Y轴标签 -->
            <text v-for="i in 5" :key="'y'+i" x="35" :y="25 + (i-1)*35" text-anchor="end" font-size="10" fill="#8a9a8e">
              {{ Math.round(maxRevenue * (5-i) / 4 / 1000) }}k
            </text>
            <!-- 面积图 -->
            <path :d="revenueAreaPath" fill="url(#revenueGradient)" opacity="0.3"/>
            <!-- 折线 -->
            <path :d="revenueLinePath" fill="none" stroke="#D4A853" stroke-width="2.5"/>
            <!-- 数据点 -->
            <circle v-for="(p, i) in revenueData" :key="'p'+i" :cx="p.x" :cy="p.y" r="4" fill="#D4A853" stroke="#fff" stroke-width="2"/>
            <!-- X轴标签 -->
            <text v-for="(p, i) in revenueData" :key="'x'+i" :x="p.x" y="195" text-anchor="middle" font-size="10" fill="#8a9a8e">
              {{ p.label }}
            </text>
            <defs>
              <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#D4A853" stop-opacity="0.4"/>
                <stop offset="100%" stop-color="#D4A853" stop-opacity="0"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
      </div>

      <!-- 时段分布 -->
      <div class="chart-card">
        <h3 class="section-title">时段分布 · Time Slot Distribution</h3>
        <div class="donut-chart-container">
          <svg viewBox="0 0 120 120" class="donut-chart">
            <circle cx="60" cy="60" r="50" fill="none" stroke="#f0f2f0" stroke-width="20"/>
            <circle v-for="(slice, i) in timeSlotSlices" :key="i"
              :cx="60" :cy="60" r="50"
              fill="none"
              :stroke="slice.color"
              stroke-width="20"
              :stroke-dasharray="`${slice.length} ${314 - slice.length}`"
              :stroke-dashoffset="-slice.offset"
              transform="rotate(-90 60 60)"/>
            <text x="60" y="55" text-anchor="middle" font-size="14" font-weight="700" fill="#1a2f23">{{ stats.todayBookings }}</text>
            <text x="60" y="72" text-anchor="middle" font-size="9" fill="#8a9a8e">总预订</text>
          </svg>
          <div class="donut-legend">
            <div v-for="slice in timeSlotSlices" :key="slice.label" class="legend-item">
              <span class="legend-dot" :style="{ background: slice.color }"></span>
              <span class="legend-label">{{ slice.label }}</span>
              <span class="legend-value">{{ slice.value }} ({{ slice.percent }}%)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 桌台类型占比 -->
      <div class="chart-card">
        <h3 class="section-title">桌台类型 · Table Type Mix</h3>
        <div class="bar-chart-horizontal">
          <div v-for="item in tableTypeData" :key="item.type" class="h-bar-row">
            <span class="h-bar-label">{{ item.type }}</span>
            <div class="h-bar-track">
              <div class="h-bar-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
            </div>
            <span class="h-bar-value">{{ item.count }}桌 · {{ item.percent }}%</span>
          </div>
        </div>
      </div>

      <!-- 客人来源分析 -->
      <div class="chart-card">
        <h3 class="section-title">客人来源 · Guest Source</h3>
        <div class="source-chart">
          <div v-for="item in guestSourceData" :key="item.source" class="source-item">
            <div class="source-icon" :style="{ background: item.color + '15' }">
              <svg viewBox="0 0 24 24" fill="none" :stroke="item.color" stroke-width="2">
                <g v-if="item.source === '自来客'">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                </g>
                <g v-else-if="item.source === '电话预订'">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/>
                </g>
                <g v-else-if="item.source === '线上平台'">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>
                </g>
                <g v-else-if="item.source === '会员推荐'">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </g>
                <g v-else>
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
                </g>
              </svg>
            </div>
            <div class="source-info">
              <div class="source-name">{{ item.source }}</div>
              <div class="source-bar">
                <div class="source-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
            </div>
            <div class="source-stats">
              <div class="source-count">{{ item.count }}</div>
              <div class="source-percent">{{ item.percent }}%</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 员工绩效对比 -->
      <div class="chart-card wide">
        <div class="chart-header">
          <h3 class="section-title">员工绩效对比 · Staff Performance</h3>
          <div class="chart-tabs">
            <button :class="{ active: perfMetric === 'bookings' }" @click="perfMetric = 'bookings'">预订量</button>
            <button :class="{ active: perfMetric === 'revenue' }" @click="perfMetric = 'revenue'">营收额</button>
            <button :class="{ active: perfMetric === 'conversion' }" @click="perfMetric = 'conversion'">转化率</button>
          </div>
        </div>
        <div class="perf-chart">
          <div v-for="staff in staffPerformance" :key="staff.id" class="perf-row">
            <div class="perf-staff">
              <div class="staff-avatar" :style="{ background: staff.color }">{{ staff.name[0] }}</div>
              <div class="staff-info">
                <div class="staff-name">{{ staff.name }}</div>
                <div class="staff-role">{{ staff.role }}</div>
              </div>
            </div>
            <div class="perf-bar-container">
              <div class="perf-bar-track">
                <div class="perf-bar-fill" :style="{ width: staff[perfMetric + 'Percent'] + '%', background: staff.color }"></div>
              </div>
            </div>
            <div class="perf-value" :style="{ color: staff.color }">
              {{ perfMetric === 'revenue' ? '¥' + staff.revenue.toLocaleString() : perfMetric === 'conversion' ? staff.conversion + '%' : staff.bookings + '单' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 桌台利用率热力图 -->
      <div class="chart-card wide">
        <h3 class="section-title">桌台利用率热力图 · Table Utilization Heatmap</h3>
        <div class="heatmap-container">
          <div class="heatmap-labels-y">
            <span v-for="t in tableList" :key="t.id">{{ t.name }}</span>
          </div>
          <div class="heatmap-grid">
            <div class="heatmap-labels-x">
              <span v-for="h in hours" :key="h">{{ h }}:00</span>
            </div>
            <div class="heatmap-cells">
              <div v-for="(row, i) in heatmapData" :key="i" class="heatmap-row">
                <div v-for="(cell, j) in row" :key="j" class="heatmap-cell" :style="{ background: getHeatColor(cell) }" :title="`${tableList[i].name} ${hours[j]}:00 - ${cell}%`">
                  <span v-if="cell > 0" class="cell-text">{{ cell }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="heatmap-legend">
            <span>空闲</span>
            <div class="legend-gradient">
              <div class="legend-stop" style="background:#e8f5e9"></div>
              <div class="legend-stop" style="background:#a5d6a7"></div>
              <div class="legend-stop" style="background:#66bb6a"></div>
              <div class="legend-stop" style="background:#ffa726"></div>
              <div class="legend-stop" style="background:#ef5350"></div>
            </div>
            <span>满座</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 实时预订列表 -->
    <div class="booking-list-card">
      <div class="card-header">
        <h3 class="section-title">实时预订 · Live Bookings</h3>
        <div class="card-actions">
          <button class="btn-sm" @click="refreshBookings">刷新</button>
        </div>
      </div>
      <div class="booking-scroll">
        <div v-for="b in recentBookings" :key="b.id" class="booking-item" :class="b.status">
          <div class="booking-time">
            <div class="time-hour">{{ b.time }}</div>
            <div class="time-date">{{ b.date }}</div>
          </div>
          <div class="booking-info">
            <div class="booking-guest">{{ b.guestName }}</div>
            <div class="booking-detail">{{ b.tableName }} · {{ b.pax }}人 · {{ b.source }}</div>
          </div>
          <div class="booking-amount">¥{{ b.amount.toLocaleString() }}</div>
          <span class="booking-status">{{ statusText(b.status) }}</span>
        </div>
      </div>
    </div>

    <!-- 对比分析 -->
    <div class="chart-card wide">
      <div class="chart-header">
        <h3 class="section-title">对比分析 · Comparison Analysis</h3>
        <div class="chart-tabs">
          <button :class="{ active: compareType === 'week' }" @click="compareType = 'week'">本周vs上周</button>
          <button :class="{ active: compareType === 'month' }" @click="compareType = 'month'">本月vs上月</button>
          <button :class="{ active: compareType === 'year' }" @click="compareType = 'year'">今年vs去年</button>
        </div>
      </div>
      <div class="compare-chart">
        <svg :viewBox="`0 0 ${compareChartWidth} 220`" class="compare-svg">
          <!-- 网格线 -->
          <line v-for="i in 5" :key="'ch'+i" :x1="50" :y1="20 + (i-1)*40" :x2="compareChartWidth-10" :y2="20 + (i-1)*40" stroke="#e8ece9" stroke-width="1"/>
          <!-- Y轴标签 -->
          <text v-for="i in 5" :key="'cy'+i" x="45" :y="25 + (i-1)*40" text-anchor="end" font-size="10" fill="#8a9a8e">
            {{ Math.round(maxCompareValue * (5-i) / 4 / 1000) }}k
          </text>
          <!-- 本期柱状图 -->
          <rect v-for="(bar, i) in compareBars" :key="'curr'+i" 
            :x="bar.x1" :y="bar.y1" :width="bar.width" :height="bar.height" 
            fill="#2D4A3E" opacity="0.85" rx="2"/>
          <!-- 上期柱状图 -->
          <rect v-for="(bar, i) in compareBars" :key="'prev'+i" 
            :x="bar.x2" :y="bar.y2" :width="bar.width" :height="bar.height" 
            fill="#D4A853" opacity="0.85" rx="2"/>
          <!-- X轴标签 -->
          <text v-for="(label, i) in compareLabels" :key="'xl'+i" :x="50 + i * (compareChartWidth - 60) / compareLabels.length + (compareChartWidth - 60) / compareLabels.length / 2" y="210" text-anchor="middle" font-size="11" fill="#6a7a6e">
            {{ label }}
          </text>
        </svg>
        <div class="compare-legend">
          <div class="legend-item">
            <span class="legend-dot" style="background:#2D4A3E"></span>
            <span>本期 · Current</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#D4A853"></span>
            <span>上期 · Previous</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 客人深度分析 -->
    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="section-title">客人画像 · Guest Profile</h3>
        <div class="guest-profile">
          <div class="profile-item">
            <div class="profile-label">新客占比</div>
            <div class="profile-bar">
              <div class="profile-fill" style="width:35%;background:#4A7C59"></div>
            </div>
            <div class="profile-value">35%</div>
          </div>
          <div class="profile-item">
            <div class="profile-label">老客复购</div>
            <div class="profile-bar">
              <div class="profile-fill" style="width:65%;background:#2D4A3E"></div>
            </div>
            <div class="profile-value">65%</div>
          </div>
          <div class="profile-item">
            <div class="profile-label">会员消费</div>
            <div class="profile-bar">
              <div class="profile-fill" style="width:48%;background:#D4A853"></div>
            </div>
            <div class="profile-value">48%</div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="section-title">消费等级分布 · Spending Level</h3>
        <div class="spending-levels">
          <div class="level-item" v-for="level in spendingLevels" :key="level.label">
            <div class="level-icon" :style="{ background: level.color + '15' }">
              <svg viewBox="0 0 24 24" fill="none" :stroke="level.color" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>
              </svg>
            </div>
            <div class="level-info">
              <div class="level-label">{{ level.label }}</div>
              <div class="level-count">{{ level.count }}人</div>
            </div>
            <div class="level-percent">{{ level.percent }}%</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 员工深度分析 -->
    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="section-title">员工效率分析 · Staff Efficiency</h3>
        <div class="efficiency-chart">
          <div class="efficiency-item" v-for="staff in staffEfficiency" :key="staff.name">
            <div class="eff-header">
              <div class="eff-avatar" :style="{ background: staff.color }">{{ staff.name[0] }}</div>
              <div class="eff-info">
                <div class="eff-name">{{ staff.name }}</div>
                <div class="eff-role">{{ staff.role }}</div>
              </div>
            </div>
            <div class="eff-metrics">
              <div class="eff-metric">
                <span class="metric-label">接单量</span>
                <span class="metric-value">{{ staff.orders }}</span>
              </div>
              <div class="eff-metric">
                <span class="metric-label">转化率</span>
                <span class="metric-value">{{ staff.conversion }}%</span>
              </div>
              <div class="eff-metric">
                <span class="metric-label">满意度</span>
                <span class="metric-value">{{ staff.satisfaction }}分</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="section-title">工作量分布 · Workload Distribution</h3>
        <div class="workload-chart">
          <svg viewBox="0 0 300 200" class="workload-svg">
            <!-- 堆叠柱状图 -->
            <rect v-for="(bar, i) in workloadBars" :key="'w1'+i" 
              :x="bar.x" :y="bar.y1" :width="bar.width" :height="bar.h1" 
              fill="#2D4A3E" opacity="0.9"/>
            <rect v-for="(bar, i) in workloadBars" :key="'w2'+i" 
              :x="bar.x" :y="bar.y2" :width="bar.width" :height="bar.h2" 
              fill="#4A7C59" opacity="0.9"/>
            <rect v-for="(bar, i) in workloadBars" :key="'w3'+i" 
              :x="bar.x" :y="bar.y3" :width="bar.width" :height="bar.h3" 
              fill="#D4A853" opacity="0.9"/>
            <!-- X轴标签 -->
            <text v-for="(label, i) in workloadLabels" :key="'wl'+i" :x="30 + i * 50 + 20" y="195" text-anchor="middle" font-size="10" fill="#6a7a6e">
              {{ label }}
            </text>
          </svg>
          <div class="workload-legend">
            <div class="legend-item"><span class="legend-dot" style="background:#2D4A3E"></span>预订处理</div>
            <div class="legend-item"><span class="legend-dot" style="background:#4A7C59"></span>客户咨询</div>
            <div class="legend-item"><span class="legend-dot" style="background:#D4A853"></span>其他事务</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 房间/桌台使用深度分析 -->
    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="section-title">桌台周转率 · Table Turnover</h3>
        <div class="turnover-chart">
          <div class="turnover-item" v-for="table in tableTurnover" :key="table.name">
            <div class="turnover-header">
              <span class="turnover-name">{{ table.name }}</span>
              <span class="turnover-rate">{{ table.turnover }}次/天</span>
            </div>
            <div class="turnover-bar">
              <div class="turnover-fill" :style="{ width: table.percent + '%', background: table.color }"></div>
            </div>
            <div class="turnover-detail">
              <span>平均用餐: {{ table.avgDuration }}分钟</span>
              <span>空闲时段: {{ table.idleHours }}小时</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="section-title">时段使用分析 · Time Slot Usage</h3>
        <div class="time-usage-chart">
          <svg viewBox="0 0 300 180" class="time-usage-svg">
            <!-- 面积图 -->
            <path :d="timeUsageAreaPath" fill="url(#timeUsageGradient)" opacity="0.3"/>
            <path :d="timeUsageLinePath" fill="none" stroke="#2D4A3E" stroke-width="2.5"/>
            <!-- 数据点 -->
            <circle v-for="(p, i) in timeUsagePoints" :key="'tu'+i" :cx="p.x" :cy="p.y" r="4" fill="#2D4A3E" stroke="#fff" stroke-width="2"/>
            <!-- X轴标签 -->
            <text v-for="(p, i) in timeUsagePoints" :key="'tx'+i" :x="p.x" y="170" text-anchor="middle" font-size="9" fill="#8a9a8e">
              {{ p.label }}
            </text>
            <defs>
              <linearGradient id="timeUsageGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#2D4A3E" stop-opacity="0.4"/>
                <stop offset="100%" stop-color="#2D4A3E" stop-opacity="0"/>
              </linearGradient>
            </defs>
          </svg>
          <div class="time-usage-stats">
            <div class="usage-stat">
              <div class="stat-label">高峰时段</div>
              <div class="stat-value">18:00-20:00</div>
            </div>
            <div class="usage-stat">
              <div class="stat-label">低谷时段</div>
              <div class="stat-value">14:00-17:00</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 报表打印 -->
    <div class="report-print-card">
      <div class="card-header">
        <h3 class="section-title">报表生成与打印 · Report Generation & Print</h3>
      </div>
      <div class="report-content">
        <div class="report-options">
          <div class="option-group">
            <label>报表类型</label>
            <select v-model="reportConfig.type">
              <option value="daily">日报 · Daily Report</option>
              <option value="weekly">周报 · Weekly Report</option>
              <option value="monthly">月报 · Monthly Report</option>
              <option value="custom">自定义 · Custom Report</option>
            </select>
          </div>
          <div class="option-group">
            <label>日期范围</label>
            <div class="date-range">
              <input type="date" v-model="reportConfig.dateFrom" />
              <span>至</span>
              <input type="date" v-model="reportConfig.dateTo" />
            </div>
          </div>
          <div class="option-group">
            <label>报表内容</label>
            <div class="checkbox-group">
              <label><input type="checkbox" v-model="reportConfig.includeRevenue" /> 营收数据</label>
              <label><input type="checkbox" v-model="reportConfig.includeBookings" /> 预订统计</label>
              <label><input type="checkbox" v-model="reportConfig.includeStaff" /> 员工绩效</label>
              <label><input type="checkbox" v-model="reportConfig.includeGuest" /> 客人分析</label>
            </div>
          </div>
          <div class="option-group">
            <label>导出格式</label>
            <div class="format-buttons">
              <button class="format-btn" :class="{ active: reportConfig.format === 'pdf' }" @click="reportConfig.format = 'pdf'">PDF</button>
              <button class="format-btn" :class="{ active: reportConfig.format === 'excel' }" @click="reportConfig.format = 'excel'">Excel</button>
              <button class="format-btn" :class="{ active: reportConfig.format === 'print' }" @click="reportConfig.format = 'print'">打印</button>
            </div>
          </div>
        </div>
        <div class="report-actions">
          <button class="btn-primary" @click="generateReport">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
            生成报表
          </button>
          <button class="btn-secondary" @click="previewReport">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
            </svg>
            预览
          </button>
        </div>
      </div>
      <div v-if="reportPreview" class="report-preview">
        <div class="preview-header">
          <h4>报表预览 · Report Preview</h4>
          <button class="close-btn" @click="reportPreview = false">×</button>
        </div>
        <div class="preview-content">
          <div class="preview-title">又见炊烟私房菜 · 前台预定{{ reportConfig.type === 'daily' ? '日报' : reportConfig.type === 'weekly' ? '周报' : '月报' }}</div>
          <div class="preview-period">报表周期: {{ reportConfig.dateFrom }} 至 {{ reportConfig.dateTo }}</div>
          <div class="preview-section" v-if="reportConfig.includeRevenue">
            <h5>一、营收概况</h5>
            <p>总营收: ¥{{ stats.todayRevenue.toLocaleString() }} | 预订收入: ¥{{ (stats.todayRevenue * 0.7).toLocaleString() }} | 散客收入: ¥{{ (stats.todayRevenue * 0.3).toLocaleString() }}</p>
          </div>
          <div class="preview-section" v-if="reportConfig.includeBookings">
            <h5>二、预订统计</h5>
            <p>总预订数: {{ stats.todayBookings }} | 已确认: {{ Math.round(stats.todayBookings * 0.8) }} | 待确认: {{ Math.round(stats.todayBookings * 0.15) }} | 已取消: {{ Math.round(stats.todayBookings * 0.05) }}</p>
          </div>
          <div class="preview-section" v-if="reportConfig.includeStaff">
            <h5>三、员工绩效</h5>
            <p>最佳员工: {{ staffPerformance[0].name }} ({{ staffPerformance[0].bookings }}单) | 平均转化率: {{ Math.round(staffPerformance.reduce((a,b) => a + b.conversion, 0) / staffPerformance.length) }}%</p>
          </div>
          <div class="preview-section" v-if="reportConfig.includeGuest">
            <h5>四、客人分析</h5>
            <p>新客占比: 35% | 老客复购: 65% | 会员消费: 48% | 人均消费: ¥{{ stats.avgSpend }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 工程管理 -->
    <div class="engineering-section">
      <h3 class="section-title">工程管理 · Engineering Management</h3>
      
      <!-- 设备状态概览 -->
      <div class="engineering-grid">
        <div class="eng-card">
          <div class="eng-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
            </svg>
            <span>设备状态</span>
          </div>
          <div class="eng-stats">
            <div class="eng-stat-item">
              <span class="stat-label">正常运行</span>
              <span class="stat-value" style="color:#4A7C59">{{ engineeringStats.equipmentNormal }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">维修中</span>
              <span class="stat-value" style="color:#D4A853">{{ engineeringStats.equipmentRepair }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">故障</span>
              <span class="stat-value" style="color:#C0392B">{{ engineeringStats.equipmentFault }}</span>
            </div>
          </div>
        </div>

        <div class="eng-card">
          <div class="eng-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <path d="M16 2v4M8 2v4M3 10h18"/>
            </svg>
            <span>维护工单</span>
          </div>
          <div class="eng-stats">
            <div class="eng-stat-item">
              <span class="stat-label">待处理</span>
              <span class="stat-value" style="color:#C0392B">{{ engineeringStats.maintenancePending }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">处理中</span>
              <span class="stat-value" style="color:#D4A853">{{ engineeringStats.maintenanceProcessing }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">已完成</span>
              <span class="stat-value" style="color:#4A7C59">{{ engineeringStats.maintenanceDone }}</span>
            </div>
          </div>
        </div>

        <div class="eng-card">
          <div class="eng-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
            </svg>
            <span>今日能耗</span>
          </div>
          <div class="eng-stats">
            <div class="eng-stat-item">
              <span class="stat-label">用电</span>
              <span class="stat-value" style="color:#D4A853">{{ engineeringStats.energyElectric }} kWh</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">用水</span>
              <span class="stat-value" style="color:#5B7B8A">{{ engineeringStats.energyWater }} t</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">费用</span>
              <span class="stat-value" style="color:#2D4A3E">¥{{ engineeringStats.energyCost }}</span>
            </div>
          </div>
        </div>

        <div class="eng-card">
          <div class="eng-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <span>安全隐患</span>
          </div>
          <div class="eng-stats">
            <div class="eng-stat-item">
              <span class="stat-label">待整改</span>
              <span class="stat-value" style="color:#C0392B">{{ engineeringStats.safetyPending }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">已整改</span>
              <span class="stat-value" style="color:#4A7C59">{{ engineeringStats.safetyResolved }}</span>
            </div>
            <div class="eng-stat-item">
              <span class="stat-label">巡检次数</span>
              <span class="stat-value" style="color:#2D4A3E">{{ engineeringStats.safetyInspections }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 维护工单列表 -->
      <div class="maintenance-list-card">
        <div class="card-header">
          <h4>维护工单 · Maintenance Orders</h4>
          <button class="btn-sm" @click="showAllMaintenance = !showAllMaintenance">
            {{ showAllMaintenance ? '收起' : '查看全部' }}
          </button>
        </div>
        <div class="maintenance-list" :class="{ expanded: showAllMaintenance }">
          <div v-for="order in maintenanceOrders" :key="order.id" class="maintenance-item" :class="order.priority">
            <div class="maintenance-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
              </svg>
            </div>
            <div class="maintenance-info">
              <div class="maintenance-title">{{ order.title }}</div>
              <div class="maintenance-meta">
                <span>{{ order.location }}</span>
                <span>{{ order.time }}</span>
              </div>
            </div>
            <div class="maintenance-status" :class="order.status">
              {{ maintenanceStatusText(order.status) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

// --- API Functions ---
const getTableList = (params) => request.get('/api/tables', { params })
const getTodayOrders = (params) => request.get('/api/bookings', { params })
const createOrder = (data) => request.post('/api/bookings', data)
const updateOrderStatus = (id, status) => request.put(`/api/bookings/${id}`, { status })
const getRoomAvailability = (date) => request.get('/api/bookings', { params: { date, type: 'rooms' } })

// --- Loading & Error State ---
const loading = ref({
  stats: false, tables: false, orders: false,
  revenue: false, staff: false, engineering: false
})
const errors = ref({})

const showQueryPanel = ref(false)
const revenuePeriod = ref('week')
const perfMetric = ref('bookings')

const query = ref({
  dateFrom: '', dateTo: '', timeSlot: '', tableType: '',
  status: '', source: '', paxMin: null, paxMax: null,
  amountMin: null, amountMax: null, staff: ''
})

const staffList = ref([])

const stats = ref({
  todayBookings: 0,
  bookingTrend: 0,
  todayRevenue: 0,
  revenueTrend: 0,
  occupancyRate: 0,
  occupiedTables: 0,
  totalTables: 0,
  avgSpend: 0,
  spendTrend: 0,
})

const revenueRaw = ref([])

const revenueData = computed(() => {
  const data = revenueRaw.value
  if (!data.length) return []
  const maxVal = Math.max(...data.map(d => d.value)) || 1
  const chartWidth = revenuePeriod.value === 'week' ? 600 : revenuePeriod.value === 'month' ? 900 : 700
  const padding = 50
  const chartHeight = 160

  return data.map((d, i) => ({
    label: d.label,
    value: d.value,
    x: padding + (i / (data.length - 1 || 1)) * (chartWidth - padding * 2),
    y: chartHeight - (d.value / maxVal) * chartHeight + 20
  }))
})

const revenueChartWidth = computed(() => {
  return revenuePeriod.value === 'week' ? 600 : revenuePeriod.value === 'month' ? 900 : 700
})

const maxRevenue = computed(() => Math.max(...revenueData.value.map(d => d.value)))

const revenueLinePath = computed(() => {
  return revenueData.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})

const revenueAreaPath = computed(() => {
  const data = revenueData.value
  if (!data.length) return ''
  const first = data[0]
  const last = data[data.length - 1]
  return `${revenueLinePath.value} L ${last.x} 180 L ${first.x} 180 Z`
})

const timeSlotRaw = ref([])

const timeSlotSlices = computed(() => {
  const total = stats.value.todayBookings || 1
  const data = timeSlotRaw.value
  if (!data.length) return []
  let offset = 0
  return data.map(d => {
    const length = (d.value / total) * 314
    const slice = { ...d, length, offset, percent: Math.round(d.value / total * 100) }
    offset += length
    return slice
  })
})

const tableTypeData = ref([])
const guestSourceData = ref([])

const staffPerformance = ref([])
const tableList = ref([])

const hours = ref([11, 12, 13, 17, 18, 19, 20, 21])

const heatmapData = ref([])

const getHeatColor = (value) => {
  if (value === 0) return '#f5f5f5'
  if (value < 30) return '#e8f5e9'
  if (value < 60) return '#a5d6a7'
  if (value < 80) return '#66bb6a'
  if (value < 95) return '#ffa726'
  return '#ef5350'
}

const recentBookings = ref([])

const statusText = (s) => ({
  confirmed: '已确认',
  pending: '待确认',
  cancelled: '已取消',
  completed: '已完成'
}[s] || s)

const resetQuery = () => {
  query.value = {
    dateFrom: '', dateTo: '', timeSlot: '', tableType: '',
    status: '', source: '', paxMin: null, paxMax: null,
    amountMin: null, amountMax: null, staff: ''
  }
}

const applyQuery = () => {
  console.log('Applying query:', query.value)
  // TODO: Implement actual query logic
}

const exportReport = () => {
  console.log('Exporting report...')
  // TODO: Implement export logic
}

const refreshBookings = () => {
  console.log('Refreshing bookings...')
  // TODO: Implement refresh logic
}

// --- 报表配置 ---
const reportConfig = ref({
  type: 'daily',
  dateFrom: new Date().toISOString().split('T')[0],
  dateTo: new Date().toISOString().split('T')[0],
  includeRevenue: true,
  includeBookings: true,
  includeStaff: true,
  includeGuest: true,
  format: 'pdf'
})

const reportPreview = ref(false)

const generateReport = () => {
  console.log('Generating report:', reportConfig.value)
  // TODO: Implement actual report generation
  reportPreview.value = true
}

const previewReport = () => {
  reportPreview.value = true
}

// --- 工程管理数据 ---
const showAllMaintenance = ref(false)

const engineeringStats = ref({
  equipmentNormal: 0,
  equipmentRepair: 0,
  equipmentFault: 0,
  maintenancePending: 0,
  maintenanceProcessing: 0,
  maintenanceDone: 0,
  energyElectric: 0,
  energyWater: 0,
  energyCost: 0,
  safetyPending: 0,
  safetyResolved: 0,
  safetyInspections: 0
})

const maintenanceOrders = ref([])

const maintenanceStatusText = (status) => {
  const map = {
    pending: '待处理',
    processing: '处理中',
    done: '已完成'
  }
  return map[status] || status
}

// --- 客人分析 & 员工效率 & 桌台周转 ---
const spendingLevels = ref([])
const staffEfficiency = ref([])
const tableTurnover = ref([])


// --- 对比分析数据 (computed, API-driven via compareType) ---
const compareType = ref('week')
const compareChartWidth = ref(700)
const compareRawData = ref({ current: [], previous: [] })

const compareData = computed(() => {
  const labels = compareType.value === 'week'
    ? ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    : compareType.value === 'month'
    ? ['第1周', '第2周', '第3周', '第4周']
    : ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
  return {
    labels,
    current: compareRawData.value.current || [],
    previous: compareRawData.value.previous || []
  }
})

const compareLabels = computed(() => compareData.value.labels)
const maxCompareValue = computed(() => Math.max(...(compareData.value.current || [0]), ...(compareData.value.previous || [0])))

const compareBars = computed(() => {
  const data = compareData.value
  const maxVal = maxCompareValue.value || 1
  const chartHeight = 180
  const barWidth = 20
  const spacing = (compareChartWidth.value - 60) / (data.labels.length || 1)
  
  return data.labels.map((_, i) => {
    const x = 50 + i * spacing + spacing / 2
    const h1 = ((data.current[i] || 0) / maxVal) * chartHeight
    const h2 = ((data.previous[i] || 0) / maxVal) * chartHeight
    return {
      x1: x - barWidth - 2, y1: 20 + chartHeight - h1,
      x2: x + 2, y2: 20 + chartHeight - h2,
      width: barWidth, height: h1, h2: h2
    }
  })
})

// --- Workload distribution (API-driven) ---
const workloadLabels = ref([])
const workloadRaw = ref([])
const workloadBars = computed(() => {
  const maxVal = 120
  const chartHeight = 160
  const barWidth = 40
  return workloadRaw.value.map((d, i) => {
    const x = 30 + i * 50
    const h1 = (d.orders / maxVal) * chartHeight
    const h2 = (d.consult / maxVal) * chartHeight
    const h3 = (d.other / maxVal) * chartHeight
    return {
      x, y1: 20 + chartHeight - h1 - h2 - h3, h1,
      y2: 20 + chartHeight - h2 - h3, h2,
      y3: 20 + chartHeight - h3, h3, width: barWidth
    }
  })
})

// --- Time usage (API-driven) ---
const timeUsageRaw = ref([])
const timeUsagePoints = computed(() => {
  const data = timeUsageRaw.value
  if (!data.length) return []
  const maxVal = 100
  const chartWidth = 280, chartHeight = 140, padding = 20
  return data.map((d, i) => ({
    label: d.label, value: d.value,
    x: padding + (i / (data.length - 1)) * (chartWidth - padding * 2),
    y: chartHeight - (d.value / maxVal) * chartHeight + padding
  }))
})

const timeUsageLinePath = computed(() => {
  return timeUsagePoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
})
const timeUsageAreaPath = computed(() => {
  const pts = timeUsagePoints.value
  if (!pts.length) return ''
  const first = pts[0], last = pts[pts.length - 1]
  return `${timeUsageLinePath.value} L ${last.x} 160 L ${first.x} 160 Z`
})

// --- Data Fetching ---
const fetchStats = async () => {
  loading.value.stats = true
  try {
    const { data } = await getTodayOrders({ summary: true })
    if (data) {
      stats.value = { ...stats.value, ...data }
      if (data.timeSlots) timeSlotRaw.value = data.timeSlots
    }
  } catch (e) { errors.value.stats = e.message } finally { loading.value.stats = false }
}

const fetchTables = async () => {
  loading.value.tables = true
  try {
    const { data } = await getTableList()
    if (data) {
      tableList.value = data.tables || data || []
      tableTypeData.value = data.typeDistribution || []
      tableTurnover.value = data.turnover || []
      if (data.heatmap) heatmapData.value = data.heatmap
      if (data.hours) hours.value = data.hours
    }
  } catch (e) { errors.value.tables = e.message } finally { loading.value.tables = false }
}

const fetchOrders = async () => {
  loading.value.orders = true
  try {
    const { data } = await getTodayOrders()
    if (data) {
      recentBookings.value = data.orders || data || []
      guestSourceData.value = data.guestSources || []
      timeUsageRaw.value = data.timeUsage || []
    }
  } catch (e) { errors.value.orders = e.message } finally { loading.value.orders = false }
}

const fetchRevenue = async () => {
  loading.value.revenue = true
  try {
    const { data } = await request.get('/api/report/revenue', { params: { period: revenuePeriod.value } })
    if (data) {
      if (data.trend) revenueRaw.value = data.trend
      if (data.compare) compareRawData.value = data.compare
    }
  } catch (e) { errors.value.revenue = e.message } finally { loading.value.revenue = false }
}

const fetchStaff = async () => {
  loading.value.staff = true
  try {
    const { data } = await request.get('/api/report/staff-kpi')
    if (data) {
      staffPerformance.value = data.performance || []
      staffEfficiency.value = data.efficiency || []
      staffList.value = data.list || []
      if (data.spendingLevels) spendingLevels.value = data.spendingLevels
      if (data.workload) {
        workloadRaw.value = data.workload.bars || []
        workloadLabels.value = data.workload.labels || []
      }
    }
  } catch (e) { errors.value.staff = e.message } finally { loading.value.staff = false }
}

const fetchEngineering = async () => {
  loading.value.engineering = true
  try {
    const { data } = await request.get('/api/engineering/work-orders')
    if (data) {
      engineeringStats.value = { ...engineeringStats.value, ...data.stats }
      maintenanceOrders.value = data.orders || []
    }
  } catch (e) { errors.value.engineering = e.message } finally { loading.value.engineering = false }
}

onMounted(() => {
  fetchStats()
  fetchTables()
  fetchOrders()
  fetchRevenue()
  fetchStaff()
  fetchEngineering()
})
</script>

<style scoped>
.front-desk-page { padding: 24px 32px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }
.header-actions { display: flex; gap: 8px; }

.btn-primary {
  background: #2D4A3E; color: #fff; border: none; padding: 8px 16px;
  border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500;
  display: flex; align-items: center; gap: 6px;
}
.btn-primary:hover { background: #3a5f50; }

.query-panel {
  background: #fff; border-radius: 8px; padding: 20px;
  border: 1px solid #e8ece9; margin-bottom: 20px;
}
.query-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.query-group { display: flex; flex-direction: column; gap: 6px; }
.query-group label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.query-group input, .query-group select {
  padding: 7px 10px; border: 1px solid #d0d8d2; border-radius: 6px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.query-group input:focus, .query-group select:focus { border-color: #2D4A3E; }
.date-range, .range-input { display: flex; align-items: center; gap: 6px; }
.date-range span, .range-input span { font-size: 12px; color: #8a9a8e; }
.query-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-secondary {
  padding: 8px 16px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e;
}

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 18px 20px;
  border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px;
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-icon svg { width: 22px; height: 22px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }
.trend-up { color: #4A7C59; }
.trend-down { color: #C0392B; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; }
.chart-card.wide { grid-column: 1 / -1; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0; }
.chart-tabs { display: flex; gap: 4px; }
.chart-tabs button {
  padding: 4px 12px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e;
}
.chart-tabs button.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }

.chart-area { overflow-x: auto; }
.line-chart { width: 100%; height: 200px; }

.donut-chart-container { display: flex; align-items: center; gap: 20px; }
.donut-chart { width: 120px; height: 120px; flex-shrink: 0; }
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }
.legend-label { flex: 1; color: #3a4a3e; }
.legend-value { color: #6a7a6e; font-weight: 500; }

.bar-chart-horizontal { display: flex; flex-direction: column; gap: 12px; }
.h-bar-row { display: flex; align-items: center; gap: 10px; }
.h-bar-label { width: 70px; font-size: 12px; color: #3a4a3e; }
.h-bar-track { flex: 1; height: 8px; background: #f0f2f0; border-radius: 4px; overflow: hidden; }
.h-bar-fill { height: 100%; border-radius: 4px; }
.h-bar-value { width: 80px; font-size: 11px; color: #6a7a6e; text-align: right; }

.source-chart { display: flex; flex-direction: column; gap: 12px; }
.source-item { display: flex; align-items: center; gap: 12px; }
.source-icon {
  width: 36px; height: 36px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.source-icon svg { width: 18px; height: 18px; }
.source-info { flex: 1; }
.source-name { font-size: 12px; color: #3a4a3e; margin-bottom: 4px; }
.source-bar { height: 6px; background: #f0f2f0; border-radius: 3px; overflow: hidden; }
.source-fill { height: 100%; border-radius: 3px; }
.source-stats { text-align: right; }
.source-count { font-size: 14px; font-weight: 600; color: #1a2f23; }
.source-percent { font-size: 11px; color: #8a9a8e; }

.perf-chart { display: flex; flex-direction: column; gap: 14px; }
.perf-row { display: flex; align-items: center; gap: 12px; }
.perf-staff { display: flex; align-items: center; gap: 10px; width: 140px; }
.staff-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 600; font-size: 14px; flex-shrink: 0;
}
.staff-info { flex: 1; }
.staff-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.staff-role { font-size: 11px; color: #8a9a8e; }
.perf-bar-container { flex: 1; }
.perf-bar-track { height: 8px; background: #f0f2f0; border-radius: 4px; overflow: hidden; }
.perf-bar-fill { height: 100%; border-radius: 4px; }
.perf-value { width: 80px; text-align: right; font-size: 13px; font-weight: 600; }

.heatmap-container { overflow-x: auto; }
.heatmap-labels-y {
  display: flex; flex-direction: column; gap: 4px;
  font-size: 11px; color: #6a7a6e; padding-right: 8px;
}
.heatmap-grid { display: flex; flex-direction: column; }
.heatmap-labels-x {
  display: flex; gap: 4px; margin-left: 60px; margin-bottom: 4px;
  font-size: 11px; color: #6a7a6e;
}
.heatmap-labels-x span { width: 40px; text-align: center; }
.heatmap-cells { display: flex; flex-direction: column; gap: 4px; }
.heatmap-row { display: flex; gap: 4px; align-items: center; }
.heatmap-cell {
  width: 40px; height: 32px; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; color: #fff; font-weight: 500;
}
.heatmap-legend {
  display: flex; align-items: center; gap: 8px;
  margin-top: 12px; font-size: 11px; color: #6a7a6e;
}
.legend-gradient { display: flex; gap: 2px; }
.legend-stop { width: 20px; height: 8px; border-radius: 2px; }

.booking-list-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e8ece9; }
.card-actions { display: flex; gap: 8px; }
.btn-sm {
  padding: 5px 12px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #3a4a3e;
}
.btn-sm:hover { background: #f0f4f1; }
.booking-scroll { max-height: 400px; overflow-y: auto; }
.booking-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; border-bottom: 1px solid #f0f2f0;
}
.booking-item:last-child { border-bottom: none; }
.booking-time { text-align: center; min-width: 60px; }
.time-hour { font-size: 16px; font-weight: 600; color: #1a2f23; }
.time-date { font-size: 11px; color: #8a9a8e; }
.booking-info { flex: 1; }
.booking-guest { font-size: 14px; font-weight: 500; color: #1a2f23; }
.booking-detail { font-size: 12px; color: #6a7a6e; margin-top: 2px; }
.booking-amount { font-size: 14px; font-weight: 600; color: #D4A853; min-width: 80px; text-align: right; }
.booking-status {
  padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 500;
}
.booking-item.confirmed .booking-status { background: rgba(74,124,89,0.1); color: #4A7C59; }
.booking-item.pending .booking-status { background: rgba(212,168,83,0.12); color: #b8922e; }
.booking-item.completed .booking-status { background: rgba(91,123,138,0.1); color: #5B7B8A; }
.booking-item.cancelled .booking-status { background: rgba(192,57,43,0.08); color: #C0392B; }

/* 对比分析 */
.compare-chart { padding: 16px 0; }
.compare-svg { width: 100%; height: 220px; }
.compare-legend { display: flex; justify-content: center; gap: 24px; margin-top: 12px; }
.compare-legend .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6a7a6e; }

/* 客人画像 */
.guest-profile { display: flex; flex-direction: column; gap: 16px; }
.profile-item { display: flex; align-items: center; gap: 12px; }
.profile-label { width: 80px; font-size: 12px; color: #6a7a6e; }
.profile-bar { flex: 1; height: 8px; background: #f0f2f0; border-radius: 4px; overflow: hidden; }
.profile-fill { height: 100%; border-radius: 4px; }
.profile-value { width: 40px; text-align: right; font-size: 13px; font-weight: 600; color: #1a2f23; }

/* 消费等级 */
.spending-levels { display: flex; flex-direction: column; gap: 12px; }
.level-item { display: flex; align-items: center; gap: 12px; padding: 10px; background: #fafbfa; border-radius: 6px; }
.level-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.level-icon svg { width: 18px; height: 18px; }
.level-info { flex: 1; }
.level-label { font-size: 12px; color: #3a4a3e; }
.level-count { font-size: 11px; color: #8a9a8e; margin-top: 2px; }
.level-percent { font-size: 14px; font-weight: 600; color: #1a2f23; }

/* 员工效率分析 */
.efficiency-chart { display: flex; flex-direction: column; gap: 14px; }
.efficiency-item { padding: 12px; background: #fafbfa; border-radius: 6px; }
.eff-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.eff-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 14px; flex-shrink: 0; }
.eff-info { flex: 1; }
.eff-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.eff-role { font-size: 11px; color: #8a9a8e; }
.eff-metrics { display: flex; gap: 16px; }
.eff-metric { display: flex; flex-direction: column; gap: 2px; }
.metric-label { font-size: 10px; color: #8a9a8e; }
.metric-value { font-size: 13px; font-weight: 600; color: #1a2f23; }

/* 工作量分布 */
.workload-chart { display: flex; flex-direction: column; gap: 12px; }
.workload-svg { width: 100%; height: 200px; }
.workload-legend { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; }
.workload-legend .legend-item { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #6a7a6e; }

/* 桌台周转率 */
.turnover-chart { display: flex; flex-direction: column; gap: 14px; }
.turnover-item { padding: 12px; background: #fafbfa; border-radius: 6px; }
.turnover-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.turnover-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.turnover-rate { font-size: 12px; color: #6a7a6e; }
.turnover-bar { height: 8px; background: #f0f2f0; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.turnover-fill { height: 100%; border-radius: 4px; }
.turnover-detail { display: flex; justify-content: space-between; font-size: 11px; color: #8a9a8e; }

/* 时段使用分析 */
.time-usage-chart { display: flex; flex-direction: column; gap: 12px; }
.time-usage-svg { width: 100%; height: 180px; }
.time-usage-stats { display: flex; justify-content: space-around; gap: 16px; }
.usage-stat { text-align: center; }
.usage-stat .stat-label { font-size: 11px; color: #8a9a8e; }
.usage-stat .stat-value { font-size: 14px; font-weight: 600; color: #1a2f23; margin-top: 4px; }

/* 报表打印 */
.report-print-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; margin-top: 20px; }
.report-content { padding: 20px; }
.report-options { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 16px; }
.option-group { display: flex; flex-direction: column; gap: 6px; }
.option-group label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.option-group select, .option-group input { padding: 7px 10px; border: 1px solid #d0d8d2; border-radius: 6px; font-size: 13px; color: #3a4a3e; outline: none; }
.option-group input:focus, .option-group select:focus { border-color: #2D4A3E; }
.checkbox-group { display: flex; flex-wrap: wrap; gap: 12px; }
.checkbox-group label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #3a4a3e; cursor: pointer; }
.checkbox-group input[type="checkbox"] { width: 14px; height: 14px; cursor: pointer; }
.format-buttons { display: flex; gap: 8px; }
.format-btn { padding: 6px 14px; border-radius: 4px; font-size: 12px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }
.format-btn.active { background: #2D4A3E; color: #fff; border-color: #2D4A3E; }
.report-actions { display: flex; gap: 10px; justify-content: flex-end; }
.report-preview { border-top: 1px solid #e8ece9; padding: 20px; background: #fafbfa; }
.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.preview-header h4 { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0; }
.close-btn { width: 28px; height: 28px; border-radius: 50%; border: none; background: #e8ece9; color: #6a7a6e; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.close-btn:hover { background: #d0d8d2; }
.preview-content { background: #fff; padding: 20px; border-radius: 6px; border: 1px solid #e8ece9; }
.preview-title { font-size: 16px; font-weight: 600; color: #1a2f23; text-align: center; margin-bottom: 8px; }
.preview-period { font-size: 12px; color: #8a9a8e; text-align: center; margin-bottom: 16px; }
.preview-section { margin-bottom: 16px; }
.preview-section h5 { font-size: 13px; font-weight: 600; color: #2D4A3E; margin: 0 0 8px 0; }
.preview-section p { font-size: 12px; color: #3a4a3e; line-height: 1.6; margin: 0; }

/* 工程管理 */
.engineering-section { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; padding: 20px; margin-top: 20px; }
.engineering-section > .section-title { margin-bottom: 16px; }
.engineering-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.eng-card { background: #fafbfa; border-radius: 8px; padding: 16px; border: 1px solid #e8ece9; }
.eng-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.eng-header svg { width: 20px; height: 20px; }
.eng-header span { font-size: 13px; font-weight: 600; color: #1a2f23; }
.eng-stats { display: flex; flex-direction: column; gap: 8px; }
.eng-stat-item { display: flex; justify-content: space-between; align-items: center; }
.eng-stat-item .stat-label { font-size: 11px; color: #8a9a8e; }
.eng-stat-item .stat-value { font-size: 14px; font-weight: 600; }

.maintenance-list-card { background: #fafbfa; border-radius: 8px; border: 1px solid #e8ece9; }
.maintenance-list-card .card-header { padding: 12px 16px; }
.maintenance-list-card .card-header h4 { font-size: 14px; font-weight: 600; color: #1a2f23; margin: 0; }
.maintenance-list { display: flex; flex-direction: column; gap: 8px; padding: 12px 16px; max-height: 240px; overflow: hidden; transition: max-height 0.3s ease; }
.maintenance-list.expanded { max-height: 600px; overflow-y: auto; }
.maintenance-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: #fff; border-radius: 6px; border-left: 3px solid #e8ece9; }
.maintenance-item.high { border-left-color: #C0392B; }
.maintenance-item.medium { border-left-color: #D4A853; }
.maintenance-item.low { border-left-color: #4A7C59; }
.maintenance-icon { width: 32px; height: 32px; border-radius: 6px; background: rgba(45,74,62,0.08); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.maintenance-icon svg { width: 16px; height: 16px; color: #2D4A3E; }
.maintenance-info { flex: 1; min-width: 0; }
.maintenance-title { font-size: 13px; font-weight: 500; color: #1a2f23; margin-bottom: 2px; }
.maintenance-meta { display: flex; gap: 12px; font-size: 11px; color: #8a9a8e; }
.maintenance-status { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; white-space: nowrap; }
.maintenance-status.pending { background: rgba(192,57,43,0.1); color: #C0392B; }
.maintenance-status.processing { background: rgba(212,168,83,0.12); color: #b8922e; }
.maintenance-status.done { background: rgba(74,124,89,0.1); color: #4A7C59; }
</style>
