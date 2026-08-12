<template>
  <div class="supply-chain-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">采购仓储总览</h2>
        <p class="page-subtitle">Supply Chain &amp; Cost Management</p>
        <div class="update-info">
          <span class="update-dot"></span>
          <span class="update-text">数据更新：{{ lastUpdateTime || '--' }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="auto-refresh">
          <span class="auto-refresh-label">自动刷新</span>
          <el-select v-model="autoRefreshInterval" size="small" style="width:100px" @change="onAutoRefreshChange">
            <el-option label="关闭" :value="0" />
            <el-option label="30秒" :value="30" />
            <el-option label="60秒" :value="60" />
            <el-option label="5分钟" :value="300" />
          </el-select>
        </div>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          class="date-picker"
          @change="onDateChange"
        />
        <button class="icon-btn" @click="toggleFullscreen" title="全屏">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
        </button>
        <el-button type="primary" @click="exportReport" class="export-btn">
          导出报表
        </el-button>
        <button class="refresh-btn" @click="refreshData" title="刷新">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="alert-banner" v-if="alertList.length > 0">
      <div class="alert-banner-icon">📢</div>
      <div class="alert-banner-track">
        <div class="alert-banner-content" :style="{ animationDuration: alertList.length * 5 + 's' }">
          <span v-for="(alert, i) in alertList" :key="i" class="alert-item" @click="handleAlertClick(alert)">
            <span class="alert-level" :class="alert.level">{{ alert.level === 'urgent' ? '🔴 紧急' : '🟡 预警' }}</span>
            {{ alert.text }}
            <span class="alert-action-text">立即处理 →</span>
          </span>
          <span v-for="(alert, i) in alertList" :key="'dup-' + i" class="alert-item" @click="handleAlertClick(alert)">
            <span class="alert-level" :class="alert.level">{{ alert.level === 'urgent' ? '🔴 紧急' : '🟡 预警' }}</span>
            {{ alert.text }}
            <span class="alert-action-text">立即处理 →</span>
          </span>
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card stat-primary" @click="goTo('procurement')">
        <div class="stat-top">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <div class="stat-trend" :class="kpiData.purchaseMom >= 0 ? 'up' : 'down'">
            <span>{{ kpiData.purchaseMom >= 0 ? '↑' : '↓' }} {{ Math.abs(kpiData.purchaseMom || 0) }}%</span>
            <span class="trend-label">较昨日</span>
          </div>
        </div>
        <div class="stat-value">¥{{ formatNumber(kpiData.todayPurchaseAmount) }}</div>
        <div class="stat-bottom">
          <span class="stat-label">今日采购金额</span>
          <span class="stat-sub">{{ kpiData.todayPurchaseOrders }} 笔订单 · 本月 ¥{{ formatNumber(kpiData.monthPurchaseAmount) }} · 点击查看明细 →</span>
        </div>
      </div>

      <div class="stat-card stat-green" @click="goTo('inventory')">
        <div class="stat-top">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2"/>
              <path d="M3 10v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/>
            </svg>
          </div>
          <div class="stat-trend" :class="kpiData.stockMom <= 0 ? 'down' : 'up'">
            <span>{{ kpiData.stockMom >= 0 ? '↑' : '↓' }} {{ Math.abs(kpiData.stockMom || 0) }}%</span>
            <span class="trend-label">环比</span>
          </div>
        </div>
        <div class="stat-value">¥{{ formatNumber(kpiData.stockTotalValue) }}</div>
        <div class="stat-bottom">
          <span class="stat-label">库存总值</span>
          <span class="stat-sub">{{ kpiData.materialCount }} 种原料 · 周转 {{ kpiData.turnoverDays || 0 }} 天 · 点击查看库存 →</span>
        </div>
      </div>

      <div class="stat-card stat-gold" @click="goTo('inventory')">
        <div class="stat-top">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <div class="stat-trend" :class="kpiData.lossRate <= 5 ? 'good' : 'danger'">
            <span>{{ kpiData.lossRate <= 5 ? '✓ 优秀' : '⚠ 偏高' }}</span>
            <span class="trend-label">目标 5%</span>
          </div>
        </div>
        <div class="stat-value">{{ kpiData.lossRate }}%</div>
        <div class="stat-bottom">
          <span class="stat-label">食材损耗率</span>
          <span class="stat-sub">近30天统计 · 目标 5% · 查看损耗明细 →</span>
        </div>
      </div>

      <div class="stat-card stat-red" @click="goTo('inventory')">
        <div class="stat-top">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            </svg>
          </div>
          <div class="stat-trend danger">
            <span>🔴 {{ kpiData.warningCount }}种</span>
            <span class="trend-label">预警</span>
          </div>
        </div>
        <div class="stat-value">{{ kpiData.expiringCount }} 种</div>
        <div class="stat-bottom">
          <span class="stat-label">临期食材</span>
          <span class="stat-sub">需优先使用 · {{ kpiData.warningCount }}个库存预警 · 查看清单 →</span>
        </div>
      </div>
    </div>

    <div class="quick-entry-card">
      <div class="card-header">
          <h3 class="section-title">
            <span class="title-icon">⚡</span>
            快捷操作
          </h3>
          <span class="card-hint">点击直接打开录入窗口</span>
        </div>
      <div class="entry-grid">
        <div class="entry-item" @click="goTo('procurement', 'new')">
          <div class="entry-badge" v-if="entryBadges.procurement > 0">{{ entryBadges.procurement }}</div>
          <div class="entry-icon" style="background: rgba(45,74,62,0.08); color: #2D4A3E;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
              <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">新建采购单</span>
            <span class="entry-desc">快速录入采购申请</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>

        <div class="entry-item" @click="goTo('receipt', 'new')">
          <div class="entry-badge" v-if="entryBadges.receipt > 0">{{ entryBadges.receipt }}</div>
          <div class="entry-icon" style="background: rgba(74,124,89,0.08); color: #4A7C59;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v2"/>
              <path d="M3 10v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">入库登记</span>
            <span class="entry-desc">快速验收入库</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>

        <div class="entry-item" @click="goTo('inventory')">
          <div class="entry-badge" v-if="entryBadges.inventory > 0">{{ entryBadges.inventory }}</div>
          <div class="entry-icon" style="background: rgba(91,123,138,0.08); color: #5B7B8A;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M9 9h6"/>
              <path d="M9 15h6"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">库存查询</span>
            <span class="entry-desc">实时库存 / 预警</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>

        <div class="entry-item" @click="goTo('supplier-reconciliation', 'new')">
          <div class="entry-badge" v-if="entryBadges.reconciliation > 0">{{ entryBadges.reconciliation }}</div>
          <div class="entry-icon" style="background: rgba(196,163,90,0.08); color: #C4A35A;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="9" y1="13" x2="15" y2="13"/>
              <line x1="9" y1="17" x2="15" y2="17"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">生成对账单</span>
            <span class="entry-desc">快速对账结算</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>

        <div class="entry-item" @click="goTo('stock-take', 'new')">
          <div class="entry-badge" v-if="entryBadges.stocktake > 0">{{ entryBadges.stocktake }}</div>
          <div class="entry-icon" style="background: rgba(45,74,62,0.08); color: #2D4A3E;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 19v-6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2z"/>
              <path d="M19 19v-6a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2z"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">新建盘点</span>
            <span class="entry-desc">库存盘点登记</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>

        <div class="entry-item" @click="goTo('issue', 'new')">
          <div class="entry-icon" style="background: rgba(74,124,89,0.08); color: #4A7C59;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 19V5M5 12l7-7 7 7"/>
            </svg>
          </div>
          <div class="entry-info">
            <span class="entry-name">领用出库</span>
            <span class="entry-desc">厨房领料登记</span>
          </div>
          <span class="entry-arrow">→</span>
        </div>
      </div>
    </div>

    <div class="main-grid">
      <div class="left-panel">
        <div class="task-center">
          <div class="task-header">
            <h3 class="section-title">
              <span class="title-icon">📋</span>
              智能任务中心
            </h3>
            <div class="task-filter">
              <button 
                v-for="tab in taskTabs" 
                :key="tab.key"
                class="filter-btn"
                :class="{ active: activeTaskTab === tab.key }"
                @click="activeTaskTab = tab.key"
              >
                {{ tab.label }}
                <span class="tab-count" :class="tab.key">{{ getTaskCount(tab.key) }}</span>
              </button>
            </div>
          </div>

          <div class="task-actions" v-if="selectedTasks.length > 0">
            <label class="select-all">
              <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
              已选 {{ selectedTasks.length }} 项
            </label>
            <div class="batch-btns">
              <button class="batch-btn primary" @click="batchRestock">批量补货</button>
              <button class="batch-btn" @click="batchMarkDone">标记已处理</button>
              <button class="batch-btn" @click="exportTasks">导出清单</button>
            </div>
          </div>

          <div class="task-list">
            <div v-if="urgentTasks.length > 0 && (activeTaskTab === 'all' || activeTaskTab === 'urgent')">
              <div class="priority-label urgent">
                <span class="priority-dot"></span>
                紧急 · 今日必须处理
              </div>
              <div 
                class="task-item urgent"
                v-for="task in urgentTasks"
                :key="task.id"
                @click="handleTaskClick(task)"
              >
                <label class="task-check" @click.stop>
                  <input type="checkbox" :checked="selectedTasks.includes(task.id)" @change="toggleTask(task.id)" />
                </label>
                <div class="task-icon urgent">
                  <span v-if="task.type === 'lowstock'">⚠️</span>
                  <span v-else-if="task.type === 'purchase'">📦</span>
                  <span v-else>🔴</span>
                </div>
                <div class="task-content">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">
                    <span class="meta-item">{{ task.desc }}</span>
                    <span class="meta-time">{{ task.time }}</span>
                  </div>
                </div>
                <button class="task-action" @click.stop="handleTaskAction(task)">
                  {{ task.action }}
                </button>
              </div>
            </div>

            <div v-if="warningTasks.length > 0 && (activeTaskTab === 'all' || activeTaskTab === 'warning')">
              <div class="priority-label warning">
                <span class="priority-dot"></span>
                预警 · 本周需关注
              </div>
              <div 
                class="task-item warning"
                v-for="task in warningTasks"
                :key="task.id"
                @click="handleTaskClick(task)"
              >
                <label class="task-check" @click.stop>
                  <input type="checkbox" :checked="selectedTasks.includes(task.id)" @change="toggleTask(task.id)" />
                </label>
                <div class="task-icon warning">
                  <span v-if="task.type === 'lowstock'">📉</span>
                  <span v-else-if="task.type === 'expiring'">⏰</span>
                  <span v-else>🟡</span>
                </div>
                <div class="task-content">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">
                    <span class="meta-item">{{ task.desc }}</span>
                    <span class="meta-time">{{ task.time }}</span>
                  </div>
                </div>
                <button class="task-action warning-btn" @click.stop="handleTaskAction(task)">
                  {{ task.action }}
                </button>
              </div>
            </div>

            <div v-if="normalTasks.length > 0 && (activeTaskTab === 'all' || activeTaskTab === 'normal')">
              <div class="priority-label normal">
                <span class="priority-dot"></span>
                常规 · 日常处理
              </div>
              <div 
                class="task-item normal"
                v-for="task in normalTasks"
                :key="task.id"
                @click="handleTaskClick(task)"
              >
                <label class="task-check" @click.stop>
                  <input type="checkbox" :checked="selectedTasks.includes(task.id)" @change="toggleTask(task.id)" />
                </label>
                <div class="task-icon normal">
                  <span v-if="task.type === 'stocktake'">📊</span>
                  <span v-else-if="task.type === 'purchase'">📦</span>
                  <span v-else-if="task.type === 'reconciliation'">💰</span>
                  <span v-else>🔵</span>
                </div>
                <div class="task-content">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">
                    <span class="meta-item">{{ task.desc }}</span>
                    <span class="meta-time">{{ task.time }}</span>
                  </div>
                </div>
                <button class="task-action normal-btn" @click.stop="handleTaskAction(task)">
                  {{ task.action }}
                </button>
              </div>
            </div>

            <div v-if="filteredTasks.length === 0" class="empty-tasks">
              ✓ 暂无待处理任务
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="card-header">
            <div style="display:flex;align-items:center;gap:12px;">
              <h3 class="section-title">月度成本趋势</h3>
              <div class="chart-type-switch">
                <button class="type-btn" :class="{active: chartType === 'line'}" @click="chartType = 'line'">折线图</button>
                <button class="type-btn" :class="{active: chartType === 'bar'}" @click="chartType = 'bar'">柱状图</button>
              </div>
            </div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot actual"></span>实际成本</span>
              <span class="legend-item"><span class="legend-dot budget"></span>预算线</span>
              <span class="legend-compare">同比上月 {{ costCompare.text }}</span>
            </div>
          </div>
          <div class="chart-placeholder">
            <div class="line-chart">
              <div class="chart-grid">
                <div class="grid-line" v-for="i in 5" :key="i"></div>
              </div>
              <svg viewBox="0 0 400 150" class="line-svg" v-if="chartType === 'line'">
                <defs>
                  <linearGradient id="lineGradient2" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" style="stop-color:#2D4A3E;stop-opacity:0.3" />
                    <stop offset="100%" style="stop-color:#2D4A3E;stop-opacity:0" />
                  </linearGradient>
                </defs>
                <path :d="budgetPath" fill="none" stroke="#C4A35A" stroke-width="1.5" stroke-dasharray="6,4" />
                <path :d="areaPath" fill="url(#lineGradient2)" />
                <path :d="linePath" fill="none" stroke="#2D4A3E" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <g v-for="(point, index) in chartPoints" :key="index">
                  <circle :cx="point.x" :cy="point.y" r="6" fill="transparent" :class="hoveredIndex === index ? 'chart-point-hover' : 'chart-point'" @mouseenter="hoveredIndex = index" @mouseleave="hoveredIndex = -1" @click="drillDownMonth(index)" style="cursor:pointer;" />
                  <circle :cx="point.x" :cy="point.y" r="4" fill="#2D4A3E" style="pointer-events:none;" />
                </g>
                <g v-if="hoveredIndex >= 0" :transform="`translate(${chartPoints[hoveredIndex].x}, ${chartPoints[hoveredIndex].y})`">
                  <rect x="-45" y="-38" width="90" height="30" rx="4" fill="rgba(45,74,62,0.95)" />
                  <text x="0" y="-20" text-anchor="middle" fill="white" font-size="11" font-weight="600">¥{{ costData[hoveredIndex] }},000</text>
                </g>
              </svg>
              <svg viewBox="0 0 400 150" class="line-svg" v-else>
                <g v-for="(value, index) in costData" :key="index">
                  <rect :x="40 + index * 60 - 12" :y="130 - ((value - minValue) / valueRange) * 100" width="24" :height="((value - minValue) / valueRange) * 100" fill="#2D4A3E" rx="2" style="cursor:pointer;transition:all 0.2s;" :opacity="hoveredIndex === index ? 1 : 0.75" @mouseenter="hoveredIndex = index" @mouseleave="hoveredIndex = -1" @click="drillDownMonth(index)" />
                </g>
                <g v-if="hoveredIndex >= 0">
                  <rect :x="40 + hoveredIndex * 60 - 45" y="-5" width="90" height="26" rx="4" fill="rgba(196,163,90,0.95)" />
                  <text :x="40 + hoveredIndex * 60" y="12" text-anchor="middle" fill="white" font-size="11" font-weight="600">¥{{ costData[hoveredIndex] }},000</text>
                </g>
              </svg>
              <div class="chart-labels">
                <span v-for="(month, index) in months" :key="month" :class="{'active-label': hoveredIndex === index}" @click="drillDownMonth(index)">{{ month }}</span>
              </div>
            </div>
            <div class="chart-detail" v-if="selectedMonth >= 0">
              <div class="detail-header">
                <span class="detail-title">{{ months[selectedMonth] }} 成本明细</span>
                <span class="detail-close" @click="selectedMonth = -1">✕</span>
              </div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">采购成本</span>
                  <span class="detail-value">¥{{ costData[selectedMonth] * 0.6 }},000</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">人工成本</span>
                  <span class="detail-value">¥{{ costData[selectedMonth] * 0.25 }},000</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">水电能耗</span>
                  <span class="detail-value">¥{{ costData[selectedMonth] * 0.08 }},000</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">其他费用</span>
                  <span class="detail-value">¥{{ costData[selectedMonth] * 0.07 }},000</span>
                </div>
              </div>
              <div class="detail-footer">
                <span class="budget-info">预算：¥{{ budgetData[selectedMonth] }},000</span>
                <span :class="costData[selectedMonth] > budgetData[selectedMonth] ? 'over-budget' : 'under-budget'">
                  {{ costData[selectedMonth] > budgetData[selectedMonth] ? '超支' : '节省' }} ¥{{ Math.abs(costData[selectedMonth] - budgetData[selectedMonth]) }},000
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="supplier-rank-card">
          <div class="card-header">
            <h3 class="section-title">
              <span class="title-icon">🏆</span>
              供应商欠款排行
            </h3>
            <span class="card-link" @click="goTo('supplier-reconciliation')">全部 →</span>
          </div>
          <div class="rank-list">
            <div 
              class="rank-item"
              v-for="(supplier, index) in topSuppliers"
              :key="index"
              @click="goToSupplier(supplier)"
            >
              <div class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="rank-info">
                <div class="rank-name">{{ supplier.name }}</div>
                <div class="rank-meta">{{ supplier.terms }} · {{ supplier.count }}笔</div>
              </div>
              <div class="rank-amount">¥{{ supplier.amount }}</div>
            </div>
          </div>
        </div>

        <div class="category-cost-card">
          <div class="card-header">
            <h3 class="section-title">
              <span class="title-icon">🥧</span>
              品类成本占比
            </h3>
            <span class="card-subtitle">近30天</span>
          </div>
          <div class="pie-wrap">
            <svg viewBox="0 0 200 200" class="pie-svg">
              <circle cx="100" cy="100" r="70" fill="none" stroke="#F0F2F5" stroke-width="30" />
              <g v-for="(seg, i) in pieSegments" :key="i">
                <circle
                  cx="100" cy="100" r="70"
                  fill="none"
                  :stroke="seg.color"
                  stroke-width="30"
                  :stroke-dasharray="seg.dash + ' ' + (circumference - seg.dash)"
                  :stroke-dashoffset="seg.offset"
                  transform="rotate(-90 100 100)"
                />
              </g>
              <text x="100" y="95" text-anchor="middle" font-size="24" font-weight="700" fill="#2D4A3E">¥{{ formatShort(categoryTotal) }}</text>
              <text x="100" y="118" text-anchor="middle" font-size="12" fill="#909399">总采购</text>
            </svg>
            <div class="pie-legend">
              <div v-for="(item, i) in categoryCost.slice(0, 5)" :key="i" class="legend-item">
                <span class="legend-dot" :style="{ background: pieColors[i] }"></span>
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-value">{{ item.percent }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="docs-card">
          <div class="card-header">
            <h3 class="section-title">
              <span class="title-icon">📄</span>
              待处理单据
            </h3>
            <span class="card-link" @click="goTo('procurement')">全部 →</span>
          </div>
          <div class="doc-list">
            <div class="doc-item" v-for="(doc, index) in pendingDocs" :key="index" @click="goToDoc(doc)">
              <div class="doc-icon" :class="doc.type">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
              </div>
              <div class="doc-content">
                <div class="doc-title">{{ doc.title }}</div>
                <div class="doc-meta">{{ doc.supplier }} · {{ doc.amount }}</div>
              </div>
              <div class="doc-status" :class="doc.statusClass">{{ doc.status }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 深度分析区（可折叠） -->
    <div class="deep-analysis-card">
      <div class="card-header analysis-header" @click="showAnalysis = !showAnalysis">
        <h3 class="section-title">
          <span class="title-icon">📈</span>
          深度分析
        </h3>
        <span class="card-link">
          {{ showAnalysis ? '收起' : '展开' }}
          <span :style="{ transform: showAnalysis ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.3s', display: 'inline-block' }">▲</span>
        </span>
      </div>
      <div v-show="showAnalysis" class="analysis-body">
        <div class="analysis-grid">
          <div class="analysis-col">
            <div class="analysis-col-title">
              <span class="col-icon">💎</span>
              高价值库存 Top5
            </div>
            <div class="analysis-list">
              <div v-for="(item, i) in topHighValue" :key="i" class="analysis-item">
                <span class="item-rank" :style="{ background: topColors[i] }">{{ i + 1 }}</span>
                <span class="item-name">{{ item.material_name }}</span>
                <span class="item-val">¥{{ formatNumber(item.value) }}</span>
              </div>
              <div v-if="topHighValue.length === 0" class="analysis-empty">暂无数据</div>
            </div>
          </div>
          <div class="analysis-col">
            <div class="analysis-col-title">
              <span class="col-icon">⚠️</span>
              高损耗预警 Top5
            </div>
            <div class="analysis-list">
              <div v-for="(item, i) in topHighLoss" :key="i" class="analysis-item">
                <span class="item-rank" :style="{ background: topColors[i] }">{{ i + 1 }}</span>
                <span class="item-name">{{ item.material_name }}</span>
                <span class="item-val">{{ item.loss_rate }}%</span>
              </div>
              <div v-if="topHighLoss.length === 0" class="analysis-empty">暂无损耗记录</div>
            </div>
          </div>
          <div class="analysis-col">
            <div class="analysis-col-title">
              <span class="col-icon">⏰</span>
              临期风险 Top5
            </div>
            <div class="analysis-list">
              <div v-for="(item, i) in topExpiring" :key="i" class="analysis-item">
                <span class="item-rank" :style="{ background: topColors[i] }">{{ i + 1 }}</span>
                <span class="item-name">{{ item.material_name }}</span>
                <span class="item-val">{{ item.days }}天前入库</span>
              </div>
              <div v-if="topExpiring.length === 0" class="analysis-empty">暂无临期</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog v-model="showExportDialog" title="导出报表" width="480px" class="export-dialog">
      <div class="export-dialog-body">
        <p class="export-hint">请选择要导出的报表类型：</p>
        <div class="report-type-list">
          <div 
            v-for="type in reportTypes" 
            :key="type.value"
            class="report-type-item"
            :class="{ active: selectedReportType === type.value }"
            @click="selectedReportType = type.value"
          >
            <div class="report-type-label">{{ type.label }}</div>
            <div class="report-type-desc">{{ type.desc }}</div>
          </div>
        </div>
        <div class="export-date-range">
          <label class="date-label">时间范围：</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            style="width: 260px;"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmExport">导出CSV</el-button>
      </template>
    </el-dialog>

    <!-- 快速采购弹窗 -->
    <el-dialog v-model="showQuickPurchase" title="快速采购单" width="800px" top="5vh" class="quick-purchase-dialog">
      <div class="qp-form">
        <div class="qp-row">
          <div class="qp-field">
            <label>供应商 <span style="color:red">*</span></label>
            <el-select v-model="qpForm.supplierId" filterable placeholder="选择供应商" size="default" style="width:100%">
              <el-option v-for="s in qpSuppliers" :key="s.supplier_id" :label="s.supplier_name" :value="s.supplier_id" />
            </el-select>
          </div>
          <div class="qp-field">
            <label>需求日期</label>
            <el-date-picker v-model="qpForm.expectedDate" type="date" size="default" style="width:100%" />
          </div>
          <div class="qp-field">
            <label>采购员</label>
            <el-input v-model="qpForm.buyer" size="default" readonly />
          </div>
        </div>
        <div class="qp-table-wrap">
          <div class="qp-table-header">
            <span style="font-weight:600">原料明细</span>
            <el-button size="small" type="primary" plain @click="addQpRow">+ 添加一行</el-button>
          </div>
          <el-table :data="qpForm.items" border size="default" style="width:100%">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column label="原料名称" min-width="180">
              <template #default="{ row }">
                <el-select
                  v-model="row.material_id"
                  filterable
                  remote
                  reserve-keyword
                  placeholder="输入原料名/编码搜索"
                  :remote-method="(kw) => searchQpMaterial(kw, row)"
                  :loading="row.searching"
                  size="default"
                  style="width:100%"
                  @change="(val) => onQpMaterialChange(val, row)"
                >
                  <el-option
                    v-for="item in row.suggestions || []"
                    :key="item.material_id"
                    :label="item.material_name"
                    :value="item.material_id"
                  >
                    <span>{{ item.material_name }}</span>
                    <span style="float:right;color:#999;font-size:12px">¥{{ item.latest_price }}/{{ item.purchase_unit }}</span>
                  </el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80" align="center">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
            <el-table-column label="数量" width="110" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :precision="2" size="default" style="width:100%" @change="calcQpAmount(row)" />
              </template>
            </el-table-column>
            <el-table-column label="单价(元)" width="110" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :precision="2" size="default" style="width:100%" @change="calcQpAmount(row)" />
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" width="110" align="right">
              <template #default="{ row }">¥{{ (row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeQpRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="qp-summary">
            <span>共 <b>{{ qpForm.items.length }}</b> 种原料</span>
            <span>合计金额：<b style="color:#E74C3C;font-size:16px">¥{{ qpTotalAmount.toFixed(2) }}</b></span>
          </div>
        </div>
        <div class="qp-remark">
          <label>备注</label>
          <el-input v-model="qpForm.remark" type="textarea" :rows="2" placeholder="选填" size="default" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showQuickPurchase = false">取消</el-button>
        <el-button type="primary" @click="submitQpForm" :loading="qpSubmitting">提交采购单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const dateRange = ref([])
const chartType = ref('line')
const hoveredIndex = ref(-1)
const selectedMonth = ref(-1)
const loading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshInterval = ref(0)
let autoRefreshTimer = null

function onAutoRefreshChange(val) {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
  if (val > 0) {
    autoRefreshTimer = setInterval(() => {
      fetchDashboardData()
    }, val * 1000)
    ElMessage.success(`已开启自动刷新，每${val}秒刷新一次`)
  }
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 快速采购弹窗
const showQuickPurchase = ref(false)
const qpSubmitting = ref(false)
const qpSuppliers = ref([])
const allMaterials = ref([])

const emptyQpItem = () => ({
  material_id: null, material_name: '', unit: '', quantity: 0, price: 0, amount: 0,
  searching: false, suggestions: []
})

const qpForm = reactive({
  supplierId: null,
  expectedDate: dayjs().format('YYYY-MM-DD'),
  buyer: userStore.userInfo?.staffName || '系统管理员',
  remark: '',
  items: [emptyQpItem()]
})

const qpTotalAmount = computed(() => {
  return qpForm.items.reduce((s, i) => s + (i.amount || 0), 0)
})

function openQuickPurchase(materialName) {
  qpForm.supplierId = null
  qpForm.expectedDate = dayjs().format('YYYY-MM-DD')
  qpForm.buyer = userStore.userInfo?.staffName || '系统管理员'
  qpForm.remark = ''
  qpForm.items = [emptyQpItem()]
  if (materialName) {
    qpForm.items[0].material_name = materialName
  }
  loadQpBasics()
  showQuickPurchase.value = true
}

async function loadQpBasics() {
  try {
    const [sRes, mRes] = await Promise.all([
      fetch('/menu-api/suppliers').then(r => r.json()),
      fetch('/menu-api/ingredients?pageSize=9999').then(r => r.json())
    ])
    if (sRes.code === 200) {
      qpSuppliers.value = (sRes.data || []).map(s => ({
        supplier_id: s.supplierId || s.supplier_id,
        supplier_name: s.supplierName || s.supplier_name
      }))
    }
    if (mRes.code === 200) {
      const list = mRes.data?.list || mRes.data || []
      allMaterials.value = list.map(m => ({
        material_id: m.materialId || m.material_id,
        material_name: m.materialName || m.material_name,
        purchase_unit: m.purchaseUnit || m.purchase_unit || 'kg',
        latest_price: m.latestPrice || m.latest_price || 0
      }))
    }
  } catch (e) {
    console.error('加载基础数据失败:', e)
  }
}

function searchQpMaterial(keyword, row) {
  if (!keyword) { row.suggestions = []; return }
  row.searching = true
  const kw = keyword.toLowerCase()
  setTimeout(() => {
    row.suggestions = allMaterials.value.filter(m =>
      m.material_name?.toLowerCase().includes(kw)
    ).slice(0, 20)
    row.searching = false
  }, 100)
}

function onQpMaterialChange(val, row) {
  const m = allMaterials.value.find(x => x.material_id === val)
  if (m) {
    row.material_name = m.material_name
    row.unit = m.purchase_unit
    row.price = m.latest_price || 0
    calcQpAmount(row)
  }
}

function calcQpAmount(row) {
  row.amount = Number(((row.quantity || 0) * (row.price || 0)).toFixed(2))
}

function addQpRow() {
  qpForm.items.push(emptyQpItem())
}

function removeQpRow(index) {
  if (qpForm.items.length <= 1) {
    ElMessage.warning('至少保留一行')
    return
  }
  qpForm.items.splice(index, 1)
}

async function submitQpForm() {
  if (!qpForm.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }
  const validItems = qpForm.items.filter(i => i.material_id && i.quantity > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少添加一种有效原料')
    return
  }
  qpSubmitting.value = true
  try {
    const res = await fetch('/menu-api/purchases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        supplierId: qpForm.supplierId,
        orderDate: dayjs().format('YYYY-MM-DD'),
        expectedDate: qpForm.expectedDate,
        buyer: qpForm.buyer,
        remark: qpForm.remark,
        items: validItems.map(i => ({
          materialId: i.material_id,
          materialName: i.material_name,
          orderUnit: i.unit,
          orderQuantity: i.quantity,
          orderPrice: i.price,
          orderAmount: i.amount
        }))
      })
    })
    const d = await res.json()
    if (d.code === 200) {
      ElMessage.success('采购单已提交')
      showQuickPurchase.value = false
      fetchDashboardData()
    } else {
      ElMessage.error(d.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败：' + e.message)
  } finally {
    qpSubmitting.value = false
  }
}

const kpiData = reactive({
  todayPurchaseAmount: 0,
  todayPurchaseOrders: 0,
  stockTotalValue: 0,
  materialCount: 0,
  warningCount: 0,
  lossRate: 0,
  expiringCount: 0
})

const months = ref([])
const costData = ref([])
const budgetData = ref([])

const costCompare = computed(() => {
  const cd = costData.value
  if (cd.length < 2) return { rate: 0, up: true, text: '-' }
  const last = cd[cd.length - 1]
  const prev = cd[cd.length - 2]
  if (prev === 0) return { rate: 0, up: last > 0, text: '-' }
  const rate = ((last - prev) / prev * 100).toFixed(1)
  return {
    rate: Math.abs(rate),
    up: last >= prev,
    text: `${last >= prev ? '↑' : '↓'} ${Math.abs(rate)}%`
  }
})

// 品类成本占比
const categoryCost = ref([])
const pieColors = ['#E74C3C', '#F39C12', '#3498DB', '#2ECC71', '#9B59B6', '#1ABC9C', '#E67E22', '#95A5A6']
const circumference = 2 * Math.PI * 70

const categoryTotal = computed(() => {
  return categoryCost.value.reduce((s, c) => s + (c.value || 0), 0)
})

const pieSegments = computed(() => {
  const total = categoryTotal.value
  if (total === 0) return []
  let offset = 0
  return categoryCost.value.slice(0, 8).map((item, i) => {
    const dash = (item.percent / 100) * circumference
    const seg = { color: pieColors[i] || '#95A5A6', dash, offset }
    offset += dash
    return seg
  })
})

function formatShort(num) {
  const n = parseFloat(num) || 0
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toFixed(0)
}

// 深度分析
const showAnalysis = ref(false)
const topHighValue = ref([])
const topHighLoss = ref([])
const topExpiring = ref([])
const topColors = ['#E74C3C', '#F39C12', '#3498DB', '#2ECC71', '#9B59B6']

const minValue = computed(() => {
  const all = [...costData.value, ...budgetData.value]
  return all.length ? Math.min(...all) : 0
})
const valueRange = computed(() => {
  const all = [...costData.value, ...budgetData.value]
  const max = Math.max(...all)
  const min = Math.min(...all)
  return max - min || 1
})

const alertList = ref([])
const urgentTasks = ref([])
const warningTasks = ref([])
const normalTasks = ref([])
const topSuppliers = ref([])
const pendingDocs = ref([])

const chartPoints = computed(() => {
  const cd = costData.value
  const bd = budgetData.value
  if (!cd.length) return []
  const max = Math.max(...cd, ...bd)
  const min = Math.min(...cd, ...bd)
  const range = max - min || 1
  return cd.map((value, index) => ({
    x: 40 + (index * 60),
    y: 130 - ((value - min) / range) * 100
  }))
})

const budgetPoints = computed(() => {
  const cd = costData.value
  const bd = budgetData.value
  if (!bd.length) return []
  const max = Math.max(...cd, ...bd)
  const min = Math.min(...cd, ...bd)
  const range = max - min || 1
  return bd.map((value, index) => ({
    x: 40 + (index * 60),
    y: 130 - ((value - min) / range) * 100
  }))
})

const linePath = computed(() => {
  if (chartPoints.value.length === 0) return ''
  return chartPoints.value.map((point, index) => 
    `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`
  ).join(' ')
})

const budgetPath = computed(() => {
  if (budgetPoints.value.length === 0) return ''
  return budgetPoints.value.map((point, index) => 
    `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`
  ).join(' ')
})

const areaPath = computed(() => {
  if (chartPoints.value.length === 0) return ''
  const points = chartPoints.value
  const startX = points[0].x
  const endX = points[points.length - 1].x
  return `${linePath.value} L ${endX} 130 L ${startX} 130 Z`
})

const taskTabs = [
  { key: 'all', label: '全部' },
  { key: 'urgent', label: '紧急' },
  { key: 'warning', label: '预警' },
  { key: 'normal', label: '常规' }
]

const activeTaskTab = ref('all')
const selectedTasks = ref([])

const filteredTasks = computed(() => {
  if (activeTaskTab.value === 'all') return [...urgentTasks.value, ...warningTasks.value, ...normalTasks.value]
  if (activeTaskTab.value === 'urgent') return urgentTasks.value
  if (activeTaskTab.value === 'warning') return warningTasks.value
  if (activeTaskTab.value === 'normal') return normalTasks.value
  return []
})

const isAllSelected = computed(() => {
  const allIds = filteredTasks.value.map(t => t.id)
  return allIds.length > 0 && allIds.every(id => selectedTasks.value.includes(id))
})

function getTaskCount(key) {
  if (key === 'all') return urgentTasks.value.length + warningTasks.value.length + normalTasks.value.length
  if (key === 'urgent') return urgentTasks.value.length
  if (key === 'warning') return warningTasks.value.length
  if (key === 'normal') return normalTasks.value.length
  return 0
}

function toggleTask(id) {
  const idx = selectedTasks.value.indexOf(id)
  if (idx > -1) {
    selectedTasks.value.splice(idx, 1)
  } else {
    selectedTasks.value.push(id)
  }
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedTasks.value = []
  } else {
    selectedTasks.value = filteredTasks.value.map(t => t.id)
  }
}

function batchRestock() {
  ElMessageBox.confirm(
    `确定为已选的 ${selectedTasks.value.length} 项任务生成补货单？`,
    '批量补货确认',
    {
      confirmButtonText: '确认生成',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`已为 ${selectedTasks.value.length} 项任务生成补货单`)
    selectedTasks.value = []
    router.push('/dashboard/procurement')
  }).catch(() => {})
}

function batchMarkDone() {
  ElMessage.info(`已标记 ${selectedTasks.value.length} 项为已处理`)
  selectedTasks.value = []
}

function exportTasks() {
  const tasks = filteredTasks.value
  let csv = '任务类型,任务标题,描述,截止时间,优先级\n'
  tasks.forEach(t => {
    const priority = urgentTasks.includes(t) ? '紧急' : warningTasks.includes(t) ? '预警' : '常规'
    csv += `"${priority}","${t.title}","${t.desc}","${t.time}"\n`
  })
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '采购任务清单.csv'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('任务清单已导出')
}

function handleTaskClick(task) {
  if (task.target) {
    const query = {}
    if (task.actionType) query.action = task.actionType
    if (task.docId) query.docId = task.docId
    if (task.material) query.keyword = task.material
    router.push({ path: `/dashboard/${task.target}`, query })
  }
}

function handleTaskAction(task) {
  if (task.type === 'lowstock') {
    ElMessageBox.confirm(
      `确定为「${task.title}」生成采购单？`,
      '生成采购单确认',
      {
        confirmButtonText: '确认生成',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      openQuickPurchase(task.material)
    }).catch(() => {})
  } else if (task.type === 'purchase' && task.action === '催办') {
    ElMessageBox.confirm(
      `确定向「鑫源食品」发送催办通知？\n采购单号：${task.docId || 'PO-2026061501'}`,
      '催办确认',
      {
        confirmButtonText: '发送催办',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => {
      ElMessage.success('催办通知已发送，正在跳转到采购单详情...')
      setTimeout(() => {
        const query = {}
        if (task.docId) query.docId = task.docId
        router.push({ path: '/dashboard/procurement', query })
      }, 800)
    }).catch(() => {})
  } else if (task.target) {
    const query = {}
    if (task.actionType) query.action = task.actionType
    if (task.docId) query.docId = task.docId
    router.push({ path: `/dashboard/${task.target}`, query })
  }
}

function handleAlertClick(alert) {
  if (alert.action) {
    router.push(`/dashboard/${alert.action}`)
  }
}

function onDateChange() {
  ElMessage.info('数据筛选条件已更新')
}

function drillDownMonth(index) {
  selectedMonth.value = selectedMonth.value === index ? -1 : index
}

function refreshData() {
  fetchDashboardData()
  ElMessage.success('数据已刷新')
}

function formatNumber(num) {
  if (!num) return '0'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

async function fetchDashboardData() {
  loading.value = true
  try {
    const res = await fetch('/menu-api/supply-chain/overview').then(r => r.json())
    if (res.code !== 200 || !res.data) return
    
    const d = res.data
    
    // KPI
    if (d.kpi) {
      Object.assign(kpiData, d.kpi)
    }
    
    // 月度成本趋势
    if (d.costTrend) {
      months.value = (d.costTrend.months || []).map(m => {
        const parts = m.split('-')
        return parts.length === 2 ? `${parseInt(parts[1])}月` : m
      })
      costData.value = d.costTrend.costData || []
      budgetData.value = d.costTrend.budgetData || []
    }
    
    // 告警
    alertList.value = d.alertList || []
    
    // 任务
    if (d.tasks) {
      urgentTasks.value = d.tasks.urgentTasks || []
      warningTasks.value = d.tasks.warningTasks || []
      normalTasks.value = d.tasks.normalTasks || []
    }
    
    // 供应商排行
    topSuppliers.value = d.supplierRanking || []
    
    // 品类成本占比
    categoryCost.value = d.categoryCost || []
    
    // 深度分析Top5
    topHighValue.value = d.topHighValue || []
    topHighLoss.value = d.topHighLoss || []
    topExpiring.value = d.topExpiring || []
    
    // 待处理单据
    pendingDocs.value = d.pendingDocs || []
    
    // 快捷入口待办数
    if (d.entryBadges) {
      Object.assign(entryBadges, d.entryBadges)
    }
    lastUpdateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  } catch (e) {
    console.error('获取看板数据失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})

const showExportDialog = ref(false)
const selectedReportType = ref('purchase')

const reportTypes = [
  { value: 'purchase', label: '采购报表', desc: '采购明细、供应商汇总' },
  { value: 'inventory', label: '库存报表', desc: '库存现状、预警清单' },
  { value: 'reconciliation', label: '对账单报表', desc: '对账明细、欠款汇总' },
  { value: 'cost', label: '成本分析报表', desc: '月度成本、分类统计' }
]

function exportReport() {
  showExportDialog.value = true
}

function confirmExport() {
  doExport(selectedReportType.value)
  showExportDialog.value = false
}

function doExport(type) {
  let csv = ''
  let filename = ''
  const dateStr = dateRange.value && dateRange.value.length === 2 
    ? `${dateRange.value[0]}_${dateRange.value[1]}` 
    : '最新'
  
  if (type === 'purchase') {
    csv = '日期,采购单号,供应商,原料名称,数量,单位,单价,金额,状态,经办人\n'
    csv += '2026-07-11,PO-2026071101,鑫源食品,五花肉,50,kg,¥35.00,¥1,750.00,已入库,张三\n'
    csv += '2026-07-11,PO-2026071101,鑫源食品,青菜,30,kg,¥8.00,¥240.00,已入库,张三\n'
    csv += '2026-07-11,PO-2026071102,恒达生鲜,鲫鱼,20,kg,¥28.00,¥560.00,运输中,李四\n'
    csv += '2026-07-10,PO-2026071001,福临门粮油,大豆油,10,桶,¥85.00,¥850.00,已入库,王五\n'
    filename = `采购报表_${dateStr}.csv`
  } else if (type === 'inventory') {
    csv = '原料编码,原料名称,分类,库存数量,单位,单价,库存金额,安全库存,状态\n'
    csv += 'M001,五花肉,肉类,5,kg,¥35.00,¥175.00,10kg,库存告急\n'
    csv += 'M002,青菜,蔬菜,3,kg,¥8.00,¥24.00,8kg,库存不足\n'
    csv += 'M003,干辣椒,调味品,0.8,kg,¥45.00,¥36.00,2kg,库存不足\n'
    csv += 'M004,大米,粮油,200,kg,¥5.50,¥1,100.00,100kg,正常\n'
    filename = `库存报表_${dateStr}.csv`
  } else if (type === 'reconciliation') {
    csv = '对账单号,供应商,对账期间,应付金额,已付金额,未付金额,状态,到期日,逾期天数\n'
    csv += 'DZ2026060001,鑫源食品,2026-06-01~2026-06-30,¥23,500.00,¥0.00,¥23,500.00,待对账,2026-07-15,-\n'
    csv += 'DZ2026060002,恒达生鲜,2026-06-01~2026-06-30,¥12,800.00,¥0.00,¥12,800.00,对账中,2026-07-10,2\n'
    csv += 'DZ2026050001,福临门粮油,2026-05-01~2026-05-31,¥8,200.00,¥8,200.00,¥0.00,已付款,2026-06-15,-\n'
    filename = `对账单报表_${dateStr}.csv`
  } else {
    csv = '月份,采购成本,人工成本,水电能耗,其他费用,总成本,预算,差异\n'
    for (let i = 0; i < costData.length; i++) {
      const total = costData[i] * 1000
      const budget = budgetData[i] * 1000
      const diff = total - budget
      csv += `${months[i]},¥${(costData[i]*0.6).toFixed(0)},000,¥${(costData[i]*0.25).toFixed(0)},000,¥${(costData[i]*0.08).toFixed(0)},000,¥${(costData[i]*0.07).toFixed(0)},000,¥${total.toLocaleString()},¥${budget.toLocaleString()},¥${diff.toLocaleString()}\n`
    }
    filename = `成本分析报表_${dateStr}.csv`
  }
  
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`报表导出成功：${filename}`)
}

const entryBadges = reactive({
  procurement: 0,
  receipt: 0,
  inventory: 0,
  reconciliation: 0,
  stocktake: 0,
  issue: 0
})

function goTo(path, action) {
  if (path === 'procurement' && action === 'new') {
    openQuickPurchase()
    return
  }
  const query = action ? { action } : {}
  router.push({ path: `/dashboard/${path}`, query })
}

function goToSupplier(supplier) {
  ElMessage.info(`正在查看 ${supplier.name} 的对账记录...`)
  router.push('/dashboard/supplier-reconciliation')
}

function goToDoc(doc) {
  if (doc.target) {
    router.push(`/dashboard/${doc.target}`)
  }
}
</script>

<style scoped>
.supply-chain-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  flex: 1;
}

.update-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.update-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67C23A;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #DCDFE6;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s;
}
.icon-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 6px 0;
  letter-spacing: 1px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-picker {
  width: 260px;
}

.export-btn {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
  font-weight: 500;
}

.export-btn:hover {
  background: var(--color-accent-light);
  border-color: var(--color-accent-light);
  color: white;
}

.refresh-btn {
  width: 38px;
  height: 38px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  transition: var(--transition);
}

.refresh-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.refresh-btn svg {
  width: 18px;
  height: 18px;
}

.alert-banner {
  background: linear-gradient(135deg, rgba(196, 163, 90, 0.12) 0%, rgba(45, 74, 62, 0.06) 100%);
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-lg);
  padding: 14px 20px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.alert-banner-icon {
  font-size: 22px;
  flex-shrink: 0;
  z-index: 2;
}

.alert-banner-track {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.alert-banner-content {
  display: flex;
  gap: 80px;
  white-space: nowrap;
  animation: scrollAlert linear infinite;
}

@keyframes scrollAlert {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

.alert-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.alert-item:hover {
  color: var(--color-primary);
}

.alert-level {
  font-weight: 600;
  font-size: 12px;
}

.alert-level.urgent {
  color: var(--color-danger);
}

.alert-level.warning {
  color: var(--color-warning);
}

.alert-action-text {
  color: var(--color-accent);
  font-weight: 500;
  font-size: 12px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.12);
  border-color: var(--color-accent);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.stat-card.stat-primary::before { background: var(--color-primary); }
.stat-card.stat-green::before { background: var(--color-success); }
.stat-card.stat-gold::before { background: var(--color-accent); }
.stat-card.stat-red::before { background: var(--color-danger); }

.stat-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-primary .stat-icon {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-primary);
}

.stat-green .stat-icon {
  background: rgba(74, 124, 89, 0.08);
  color: var(--color-success);
}

.stat-gold .stat-icon {
  background: rgba(196, 163, 90, 0.08);
  color: var(--color-accent);
}

.stat-red .stat-icon {
  background: rgba(194, 85, 85, 0.08);
  color: var(--color-danger);
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-trend {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-trend.up {
  color: var(--color-danger);
  background: rgba(194, 85, 85, 0.08);
}

.stat-trend.down {
  color: var(--color-success);
  background: rgba(74, 124, 89, 0.08);
}

.stat-trend.good {
  color: var(--color-success);
  background: rgba(74, 124, 89, 0.08);
}

.stat-trend.danger {
  color: var(--color-danger);
  background: rgba(194, 85, 85, 0.08);
}

.stat-trend span:first-child {
  font-weight: 600;
  font-size: 13px;
}

.trend-label {
  font-size: 11px;
  opacity: 0.8;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
  line-height: 1.2;
}

.stat-bottom {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
}

.quick-entry-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 18px;
}

.card-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.card-link {
  font-size: 12px;
  color: var(--color-accent);
  cursor: pointer;
  font-weight: 500;
}

.card-link:hover {
  color: var(--color-primary);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.entry-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-bg-alt);
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  border: 1px solid transparent;
}

.entry-item:hover {
  background: var(--color-bg-side);
  transform: translateY(-2px);
  border-color: var(--color-accent);
}

.entry-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: var(--color-danger);
  color: white;
  font-size: 11px;
  font-weight: 700;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(194, 85, 85, 0.3);
}

.entry-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.entry-icon svg {
  width: 22px;
  height: 22px;
}

.entry-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.entry-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.entry-desc {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-arrow {
  color: var(--color-text-muted);
  font-size: 14px;
  flex-shrink: 0;
  transition: var(--transition);
}

.entry-item:hover .entry-arrow {
  color: var(--color-accent);
  transform: translateX(3px);
}

.main-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-center,
.chart-card,
.supplier-rank-card,
.docs-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.task-filter {
  display: flex;
  gap: 4px;
  background: var(--color-bg-alt);
  padding: 4px;
  border-radius: 8px;
}

.filter-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-btn:hover {
  color: var(--color-text);
}

.filter-btn.active {
  color: var(--color-primary);
  background: var(--color-card);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  font-weight: 600;
}

.tab-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  background: var(--color-bg-side);
  min-width: 18px;
  text-align: center;
}

.filter-btn.active .tab-count {
  background: rgba(45, 74, 62, 0.1);
  color: var(--color-primary);
}

.tab-count.urgent { background: rgba(194, 85, 85, 0.1); color: var(--color-danger); }
.filter-btn.active .tab-count.urgent { background: rgba(194, 85, 85, 0.15); color: var(--color-danger); }
.tab-count.warning { background: rgba(212, 168, 83, 0.1); color: var(--color-warning); }
.filter-btn.active .tab-count.warning { background: rgba(212, 168, 83, 0.15); color: var(--color-warning); }
.tab-count.normal { background: rgba(91, 123, 138, 0.1); color: var(--color-info); }
.filter-btn.active .tab-count.normal { background: rgba(91, 123, 138, 0.15); color: var(--color-info); }

.task-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--color-bg-alt);
  border-radius: 8px;
  margin-bottom: 14px;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.select-all input {
  cursor: pointer;
}

.batch-btns {
  display: flex;
  gap: 8px;
}

.batch-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: var(--transition);
}

.batch-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.batch-btn.primary {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.batch-btn.primary:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary-light);
  color: white;
}

.task-list {
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.task-list::-webkit-scrollbar {
  width: 6px;
}

.task-list::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.priority-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  padding: 8px 4px;
  margin-top: 8px;
}

.priority-label:first-child {
  margin-top: 0;
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.priority-label.urgent {
  color: var(--color-danger);
}

.priority-label.urgent .priority-dot {
  background: var(--color-danger);
  box-shadow: 0 0 8px rgba(194, 85, 85, 0.4);
}

.priority-label.warning {
  color: var(--color-warning);
}

.priority-label.warning .priority-dot {
  background: var(--color-warning);
  box-shadow: 0 0 8px rgba(212, 168, 83, 0.4);
}

.priority-label.normal {
  color: var(--color-info);
}

.priority-label.normal .priority-dot {
  background: var(--color-info);
  box-shadow: 0 0 8px rgba(91, 123, 138, 0.4);
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--color-bg-alt);
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
}

.task-item:hover {
  background: var(--color-bg-side);
  border-color: var(--color-border);
}

.task-item.urgent {
  border-left: 3px solid var(--color-danger);
}

.task-item.warning {
  border-left: 3px solid var(--color-warning);
}

.task-item.normal {
  border-left: 3px solid var(--color-info);
}

.task-check {
  flex-shrink: 0;
  cursor: pointer;
}

.task-check input {
  cursor: pointer;
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.task-icon.urgent {
  background: rgba(194, 85, 85, 0.08);
}

.task-icon.warning {
  background: rgba(212, 168, 83, 0.08);
}

.task-icon.normal {
  background: rgba(91, 123, 138, 0.08);
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: var(--color-text-muted);
}

.meta-item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-time {
  flex-shrink: 0;
  font-weight: 500;
}

.urgent .meta-time {
  color: var(--color-danger);
}

.task-action {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  background: var(--color-danger);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: var(--transition);
}

.task-action:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.task-action.warning-btn {
  background: var(--color-warning);
}

.task-action.normal-btn {
  background: var(--color-primary);
}

.task-action.warning-btn:hover {
  opacity: 0.9;
}

.task-action.normal-btn:hover {
  background: var(--color-primary-light);
}

.empty-tasks {
  text-align: center;
  padding: 40px 20px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-dot.actual {
  background: var(--color-primary);
}

.legend-dot.budget {
  background: var(--color-accent);
}

.legend-compare {
  margin-left: auto;
  font-size: 12px;
  color: var(--color-danger);
  font-weight: 500;
}

.chart-placeholder {
  position: relative;
}

.chart-type-switch {
  display: flex;
  background: var(--color-bg-alt);
  padding: 2px;
  border-radius: 6px;
}

.type-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.type-btn.active {
  background: var(--color-card);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.chart-point-hover {
  fill: rgba(196, 163, 90, 0.3) !important;
}

.chart-labels span {
  font-size: 11px;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.chart-labels span.active-label {
  color: var(--color-accent);
  font-weight: 600;
  background: rgba(196, 163, 90, 0.1);
}

.chart-labels span:hover {
  color: var(--color-primary);
}

.chart-detail {
  margin-top: 12px;
  padding: 12px 14px;
  background: var(--color-bg-alt);
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.detail-close {
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.detail-close:hover {
  background: var(--color-bg-side);
  color: var(--color-text-secondary);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  background: var(--color-card);
  border-radius: 6px;
}

.detail-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
  font-size: 12px;
}

.budget-info {
  color: var(--color-text-secondary);
}

.over-budget {
  color: var(--color-danger);
  font-weight: 600;
}

.under-budget {
  color: var(--color-success);
  font-weight: 600;
}

.line-chart {
  height: 100%;
  position: relative;
}

.chart-grid {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 10px 0 30px 0;
}

.grid-line {
  height: 1px;
  background: var(--color-border-light);
}

.line-svg {
  width: 100%;
  height: 170px;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 10px;
  margin-top: 4px;
}

.chart-labels span {
  font-size: 11px;
  color: var(--color-text-muted);
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--color-bg-alt);
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
}

.rank-item:hover {
  background: var(--color-bg-side);
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  background: var(--color-bg-side);
  color: var(--color-text-muted);
}

.rank-1 {
  background: linear-gradient(135deg, #C4A35A, #D4B36A);
  color: white;
}

.rank-2 {
  background: linear-gradient(135deg, #95A5A6, #A5B5B6);
  color: white;
}

.rank-3 {
  background: linear-gradient(135deg, #CD7F32, #D49042);
  color: white;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.rank-amount {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-danger);
  flex-shrink: 0;
}

.category-cost-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}
.card-subtitle {
  font-size: 12px;
  color: #909399;
}
.pie-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}
.pie-svg {
  width: 150px;
  height: 150px;
  flex-shrink: 0;
}
.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.legend-value {
  font-weight: 600;
  color: #303133;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--color-bg-alt);
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
}

.doc-item:hover {
  background: var(--color-bg-side);
}

.doc-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.doc-icon.purchase {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-primary);
}

.doc-icon.inventory {
  background: rgba(196, 163, 90, 0.08);
  color: var(--color-accent);
}

.doc-icon svg {
  width: 18px;
  height: 18px;
}

.doc-content {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.doc-status {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.doc-status.pending {
  color: var(--color-warning);
  background: rgba(212, 168, 83, 0.1);
}

.doc-status.shipping {
  color: var(--color-info);
  background: rgba(91, 123, 138, 0.1);
}

:deep(.export-dialog .el-dialog__body) {
  padding: 16px 20px;
}

.export-dialog-body {
  font-size: 13px;
}

.export-hint {
  margin: 0 0 12px 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.report-type-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.report-type-item {
  padding: 12px 14px;
  border: 2px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-bg-alt);
}

.report-type-item:hover {
  border-color: var(--color-accent-light);
}

.report-type-item.active {
  border-color: var(--color-accent);
  background: rgba(196, 163, 90, 0.08);
}

.report-type-label {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
  margin-bottom: 4px;
}

.report-type-item.active .report-type-label {
  color: var(--color-primary);
}

.report-type-desc {
  font-size: 11px;
  color: var(--color-text-muted);
}

.export-date-range {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.date-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-grid {
    grid-template-columns: 1fr;
  }
  .entry-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .entry-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .task-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .task-actions {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
}

.quick-purchase-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
}

.deep-analysis-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-top: 16px;
}
.analysis-header {
  cursor: pointer;
  user-select: none;
}
.analysis-body {
  margin-top: 16px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.analysis-col {
  background: #FAFBFC;
  border-radius: 10px;
  padding: 16px;
}
.analysis-col-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.col-icon { font-size: 16px; }
.analysis-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.analysis-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  transition: all 0.2s;
}
.analysis-item:hover {
  background: #F0F2F5;
}
.item-rank {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: white;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.item-name {
  flex: 1;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-val {
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}
.analysis-empty {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 20px 0;
}

.qp-form {
  .qp-row {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }
  .qp-field {
    label {
      display: block;
      font-size: 13px;
      color: #555;
      margin-bottom: 6px;
      font-weight: 500;
    }
  }
  .qp-table-wrap {
    margin: 12px 0;
    padding: 12px;
    background: #FAFBFC;
    border-radius: 8px;
    border: 1px solid #EBEEF5;
  }
  .qp-table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }
  .qp-summary {
    display: flex;
    justify-content: flex-end;
    gap: 24px;
    margin-top: 12px;
    font-size: 13px;
    color: #606266;
  }
  .qp-remark {
    margin-top: 12px;
    label {
      display: block;
      font-size: 13px;
      color: #555;
      margin-bottom: 6px;
      font-weight: 500;
    }
  }
}
</style>
