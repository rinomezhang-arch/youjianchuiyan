<template>
  <div class="dashboard-home">
    <!-- 顶部标题 -->
    <div class="top-bar">
      <div class="header-left">
        <h1 class="page-title">总经理总驾驶舱</h1>
        <p class="page-subtitle">General Manager Dashboard · {{ currentDate }}</p>
      </div>
      <div class="header-right">
        <div class="store-selector">
          <span class="selector-label">门店</span>
          <select v-model="selectedStore" class="selector-dropdown" @change="loadDashboard">
            <option value="all">全部门店</option>
            <option v-for="s in storeOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </div>
          <rect x="12" y="28" width="16" height="16" rx="2" stroke="#2D4A3E" stroke-width="2.5"/>
          <rect x="36" y="28" width="16" height="16" rx="2" stroke="#2D4A3E" stroke-width="2.5"/>
    <!-- 经营指标卡片 -->
    <div class="stats-section">
      <h2 class="section-title">经营指标</h2>
      <div v-if="loadError" class="load-error">
        数据加载失败：{{ loadError }}，请稍后重试或联系管理员。
      </div>
      <div class="stats-grid">
        <div class="stat-card revenue" @click="goTo('finance')">
          <div class="stat-icon-wrap">
      </div>
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
    </div>

          <div class="stat-info">
            <div class="stat-label">今日总营收</div>
            <div class="stat-value">¥{{ formatNumber(kpi.todayRevenue) }}</div>
            <div class="stat-trend" :class="kpi.revenueTrendPct > 0 ? 'up' : (kpi.revenueTrendPct < 0 ? 'down' : 'flat')">
              {{ kpi.revenueTrendPct > 0 ? '+' : '' }}{{ kpi.revenueTrendPct.toFixed(1) }}%
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-detail">
            <div class="detail-item" v-for="(amt, sid) in kpi.revenueByStore" :key="sid">
              <span>门店 #{{ sid }}</span>
              <span>¥{{ formatNumber(amt) }}</span>
            </div>
            <div v-if="Object.keys(kpi.revenueByStore || {}).length === 0" class="detail-item">
              <span>暂无数据</span>
              <span>¥0</span>
            </div>
          </div>
      <div class="stat-card">

        <div class="stat-card traffic" @click="goTo('guest-analysis')">
          <div class="stat-icon-wrap">
        <div class="stat-label">餐厅总数</div>
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6m0 6v.01"/>
      <div class="stat-card">
        <div class="stat-header">
          <div class="stat-info">
            <div class="stat-label">今日客流</div>
            <div class="stat-value">{{ kpi.todayTraffic || 0 }}</div>
            <div class="stat-trend" :class="kpi.trafficTrendPct > 0 ? 'up' : (kpi.trafficTrendPct < 0 ? 'down' : 'flat')">
              {{ kpi.trafficTrendPct > 0 ? '+' : '' }}{{ kpi.trafficTrendPct.toFixed(1) }}%
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-detail">
            <div class="detail-item">
              <span>较昨日</span>
              <span>{{ kpi.trafficTrendPct > 0 ? '+' : '' }}{{ kpi.trafficTrendPct.toFixed(1) }}%</span>
            </div>
            <div class="detail-item">
              <span>数据来源</span>
              <span>booking_master</span>
            </div>
          </div>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9 12l2 2 4-4"/></svg>

        <div class="stat-card turnover" @click="goTo('table-utilization')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">翻台率</div>
            <div class="stat-value">{{ (kpi.turnoverRate || 0).toFixed(1) }}%</div>
            <div class="stat-trend flat">真实数据</div>
          </div>
          <div class="stat-gauge">
            <svg viewBox="0 0 100 60" class="gauge-svg">
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" stroke="#e8edea" stroke-width="8"/>
              <path d="M10 50 A40 40 0 0 1 90 50" fill="none" :stroke="(kpi.turnoverRate || 0) > 80 ? '#67C23A' : (kpi.turnoverRate || 0) > 60 ? '#E6A23C' : '#F56C6C'" stroke-width="8" :stroke-dasharray="`${Math.min((kpi.turnoverRate || 0) * 2.51, 251)} 251`"/>
            </svg>
          </div>
        </div>

        <div class="stat-card margin" @click="goTo('finance/cost-analysis')">
          <div class="stat-icon-wrap">
      <div class="stat-card">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <div class="stat-info">
            <div class="stat-label">综合毛利率</div>
            <div class="stat-value">{{ (kpi.grossMarginRate || 0).toFixed(1) }}%</div>
            <div class="stat-trend flat">真实聚合</div>
          </div>
          <div class="stat-pie">
            <svg viewBox="0 0 100 100" class="pie-svg">
              <circle cx="50" cy="50" r="40" fill="none" stroke="#E8EDEB" stroke-width="20"/>
              <circle cx="50" cy="50" r="40" fill="none" stroke="#2D4A3E" stroke-width="20" :stroke-dasharray="`${Math.min((kpi.grossMarginRate || 0) * 2.51, 251)} 251`" transform="rotate(-90 50 50)"/>
            </svg>
            <div class="pie-center">{{ (kpi.grossMarginRate || 0).toFixed(1) }}%</div>
          </div>
          <span class="stat-badge">{{ stats.staffCount }}</span>

        <div class="stat-card profit" @click="goTo('finance')">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="stat-info">
            <div class="stat-label">预估当日净利</div>
            <div class="stat-value">¥{{ formatNumber(kpi.netProfitEstimate) }}</div>
            <div class="stat-trend flat">仅扣食材成本</div>
          </div>
          <div class="stat-bar-chart">
            <div class="bar-item">
              <div class="bar-label">食材成本</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: (kpi.costBreakdown?.food || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.food || 0).toFixed(1) }}%</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">人工成本</div>
              <div class="bar-track">
                <div class="bar-fill labor" :style="{ width: (kpi.costBreakdown?.labor || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.labor || 0) > 0 ? (kpi.costBreakdown.labor).toFixed(1) + '%' : '无数据' }}</div>
            </div>
            <div class="bar-item">
              <div class="bar-label">能耗费用</div>
              <div class="bar-track">
                <div class="bar-fill energy" :style="{ width: (kpi.costBreakdown?.energy || 0) + '%' }"></div>
              </div>
              <div class="bar-value">{{ (kpi.costBreakdown?.energy || 0) > 0 ? (kpi.costBreakdown.energy).toFixed(1) + '%' : '无数据' }}</div>
            </div>
          </div>
        </div>

        <div class="stat-card orders" @click="goTo('bookings')">
          <div class="stat-icon-wrap">
          <div class="stat-icon">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
          <span class="stat-badge">¥{{ stats.todayRevenue }}</span>
        </div>
          <div class="stat-info">
            <div class="stat-label">今日订单数</div>
            <div class="stat-value">{{ kpi.orderCount || 0 }}</div>
            <div class="stat-trend flat">已确认 confirmed</div>
          </div>
          <div class="stat-channel">
            <div class="channel-item" v-for="(cnt, ch) in kpi.orderByChannel" :key="ch">
              <span class="channel-icon" :class="channelClass(ch)">{{ channelLabel(ch) }}</span>
              <span class="channel-count">{{ cnt }}</span>
            </div>
            <div v-if="Object.keys(kpi.orderByChannel || {}).length === 0" class="channel-item">
              <span class="channel-icon">暂无</span>
              <span class="channel-count">0</span>
            </div>
          </div>
        <div class="stat-label">今日营收</div>

    <div class="section-header">
      <h3 class="section-title">功能导航</h3>
    <!-- 第二行：预定看板 + 待办审批 -->
    <div class="mid-section">
      <!-- 预定看板 -->
      <div class="booking-section">
        <div class="section-header">
          <h2 class="section-title">预定看板</h2>
          <button class="section-action" @click="goTo('bookings')">查看全部</button>
        </div>
        <div class="booking-grid">
          <div class="booking-card today">
            <div class="booking-header">
              <span class="booking-label">今日包厢预定</span>
              <span class="booking-count">{{ kpi.todayBoxBookings || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.todayBoxList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-time">{{ item.time }}</span>
                </div>
                <span class="booking-name">{{ item.name }}</span>
              </div>
              <div v-if="!kpi.todayBoxList || kpi.todayBoxList.length === 0" class="empty-row">今日暂无包厢预定</div>
            </div>
          </div>
      <div v-for="item in navItems" :key="item.path" class="nav-card" @click="goTo(item.path)">
          <div class="booking-card banquet">
            <div class="booking-header">
              <span class="booking-label">宴席预定</span>
              <span class="booking-count">{{ kpi.todayBanquetBookings || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.todayBanquetList" :key="item.id">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-date">{{ item.date }}</span>
                </div>
                <span class="booking-guests">{{ item.guests }}人</span>
              </div>
              <div v-if="!kpi.todayBanquetList || kpi.todayBanquetList.length === 0" class="empty-row">今日暂无宴席</div>
            </div>
    <div class="quick-actions">

          <div class="booking-card alert">
            <div class="booking-header">
              <span class="booking-label">空包厢预警</span>
              <span class="booking-count warning">{{ kpi.emptyBoxWarningCount || 0 }}</span>
            </div>
            <div class="booking-list">
              <div class="booking-item" v-for="item in kpi.emptyBoxList" :key="item.box">
                <div class="booking-info">
                  <span class="booking-box">{{ item.box }}</span>
                  <span class="booking-status">{{ item.status }}</span>
                </div>
              </div>
              <div v-if="!kpi.emptyBoxList || kpi.emptyBoxList.length === 0" class="empty-row">暂无空置包厢</div>
            </div>
          </div>

          <div class="booking-card tomorrow">
            <div class="booking-header">
              <span class="booking-label">明日预定</span>
              <span class="booking-count">{{ kpi.tomorrowTotal || 0 }}</span>
            </div>
            <div class="booking-chart">
              <div class="chart-item">
                <span class="chart-label">午市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar" :style="{ width: tomorrowPct(kpi.tomorrowLunch, kpi.tomorrowTotal) + '%' }"></div>
                </div>
                <span class="chart-value">{{ kpi.tomorrowLunch || 0 }}</span>
              </div>
              <div class="chart-item">
                <span class="chart-label">晚市</span>
                <div class="chart-bar-wrap">
                  <div class="chart-bar evening" :style="{ width: tomorrowPct(kpi.tomorrowDinner, kpi.tomorrowTotal) + '%' }"></div>
                </div>
                <span class="chart-value">{{ kpi.tomorrowDinner || 0 }}</span>
              </div>
            </div>
          </div>
      <div class="action-buttons">
      </div>

      <!-- 待办审批 -->
      <div class="approval-section">
        <div class="section-header">
          <h2 class="section-title">待办审批</h2>
          <button class="section-action" @click="goTo('approval-center')">审批中心</button>
        </div>
        <div class="approval-tabs">
          <button
            v-for="tab in approvalTabs"
            :key="tab.key"
            :class="['tab-btn', { active: activeApprovalTab === tab.key }]"
            @click="activeApprovalTab = tab.key"
          >
            <span class="tab-text">{{ tab.name }}</span>
            <span class="tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
          </button>
        </div>
        <div class="approval-list">
          <div v-for="item in filteredApprovals" :key="item.id" class="approval-item" @click="goTo('approval-center')">
            <div class="approval-icon" :class="approvalTypeClass(item.flowType)">{{ approvalTypeLabel(item.flowType) }}</div>
            <div class="approval-content">
              <div class="approval-title">{{ item.title || item.flowNo }}</div>
              <div class="approval-meta">
                <span>{{ item.applicant || '申请人' }}</span>
                <span>{{ item.time }}</span>
              </div>
            </div>
            <div class="approval-status pending">待审批</div>
          </div>
          <div v-if="filteredApprovals.length === 0" class="empty-state">
            <span>暂无待审批事项</span>
          </div>
        <button class="action-btn" @click="goTo('kitchen')">呼叫后厨</button>
        <button class="action-btn" @click="goTo('front-office')">保洁呼叫</button>
      </div>
    </div>
    <!-- 第三行：风险预警 + 快捷跳转 -->
  </div>
      <div class="warning-section">
        <div class="section-header">
          <h2 class="section-title">风险预警</h2>
        </div>
        <div class="warning-grid">
          <div class="warning-card" v-for="item in riskWarnings" :key="item.type" :class="item.level">
            <div class="warning-content">
              <div class="warning-title">{{ item.title }}</div>
              <div class="warning-desc">{{ item.desc }}</div>
              <div class="warning-count">{{ item.count }}项待处理</div>
            </div>
            <button class="warning-action" @click="goTo(item.link)">处理</button>
          </div>
          <div v-if="riskWarnings.length === 0" class="empty-row full">暂无风险预警</div>
        </div>
      </div>

      <div class="quick-nav-section">
        <div class="section-header">
          <h2 class="section-title">业务看板</h2>
        </div>
        <div class="quick-nav-grid">
          <div
            v-for="nav in quickNav"
            :key="nav.path"
            class="nav-card"
            :style="{ '--card-color': nav.color }"
            @click="goTo(nav.path)"
          >
            <div class="nav-text">{{ nav.name }}</div>
            <div class="nav-badge" v-if="(kpi.navBadges?.[nav.key] || 0) > 0">{{ kpi.navBadges[nav.key] }}</div>
          </div>
      headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
    })
    if (resp.ok) { const d = await resp.json(); if (d?.code === 200 && d.data) stats.value = { ...stats.value, ...d.data } }
  } catch {}
}

const navItems = [
  { name: '前厅运营', path: 'front-office', color: '#2D4A3E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="6" width="16" height="12" rx="2"/><polyline points="14 2 10 6 6 2"/></svg>', desc: '客户接待 · 桌台服务 · 收银结算' },
  { name: '菜单管理', path: 'menu-manager', color: '#4A7C59', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><line x1="6" y1="5" x2="18" y2="5"/><line x1="6" y1="10" x2="18" y2="10"/><line x1="6" y1="15" x2="18" y2="15"/><line x1="2" y1="5" x2="2.01" y2="5"/><line x1="2" y1="10" x2="2.01" y2="10"/><line x1="2" y1="15" x2="2.01" y2="15"/></svg>', desc: '菜品库 · 成本卡 · 套餐组合' },
import request from '@/utils/request'
  { name: '营销会员', path: 'marketing', color: '#C4A35A', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><circle cx="8" cy="6" r="3"/><path d="M14 12v-2a3 3 0 0 0-3-3H8a3 3 0 0 0-3 3v2"/><path d="M18 17v-2a3 3 0 0 0-3-3h-2"/></svg>', desc: '会员管理 · 营销活动 · 积分商城' },
  { name: '人事行政', path: 'hr-admin', color: '#6B8C9E', icon: '<svg viewBox="0 0 20 20" fill="none" stroke="white" stroke-width="1.8"><rect x="2" y="2" width="16" height="6" rx="1"/><rect x="2" y="12" width="16" height="6" rx="1"/></svg>', desc: '员工管理 · 考勤排班 · 薪资福利' },

.top-bar {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 28px;
}
.top-logo {
  width: 48px; height: 48px;
  background: rgba(45,74,62,0.06);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.page-title { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0 0 2px 0; }
.page-subtitle { font-size: 12px; color: #8a8a8a; margin: 0; }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.stat-card {
  background: #1E2A24;
  border-radius: 14px;
  padding: 20px 22px;
}
.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.stat-icon { width: 26px; height: 26px; color: #4CAF50; }
.stat-badge {
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.7);
  padding: 2px 10px; border-radius: 10px;
  font-size: 12px; font-weight: 600;
}
.stat-value { font-size: 30px; font-weight: 700; color: #4CAF50; line-height: 1.1; margin-bottom: 6px; }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.55); margin-bottom: 2px; }
.stat-sub { font-size: 11px; color: rgba(255,255,255,0.25); }

/* Section */
.section-header { display: flex; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 700; color: #1a1a1a; }

/* Nav cards */
.nav-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 28px; }
.nav-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 20px;
  display: flex; align-items: flex-start; gap: 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.nav-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); border-color: #ccc; }
.nav-card-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.nav-card-icon :deep(svg) { width: 22px; height: 22px; }
.nav-card-body { flex: 1; min-width: 0; }
.nav-card-name { font-size: 14px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
.nav-card-desc { font-size: 12px; color: #999; line-height: 1.5; }
.nav-card-arrow { color: #ccc; font-size: 16px; margin-top: 4px; transition: all 0.2s; }
.nav-card:hover .nav-card-arrow { color: #2D4A3E; transform: translateX(3px); }

/* Quick actions */
.quick-actions { background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 20px; }
.action-buttons { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.action-btn {
  padding: 9px 18px;
  border: 1px solid #2D4A3E; border-radius: 6px;
  background: #2D4A3E; color: #fff;
  font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover { background: #3D5A4E; }

@media (max-width: 900px) { .stats-row { grid-template-columns: repeat(2, 1fr); } .nav-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .stats-row { grid-template-columns: 1fr; } .nav-grid { grid-template-columns: 1fr; } }
</style>
