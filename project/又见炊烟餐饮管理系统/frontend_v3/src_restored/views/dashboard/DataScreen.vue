<template>
  <div class="data-screen-page">
    <div class="page-header">
      <h2 class="page-title">经营数据大屏分析</h2>
      <p class="page-subtitle">Data Analytics Dashboard</p>
    </div>

    <div class="overview-row">
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">综合营收</span>
          <span class="card-trend up">+12.5%</span>
        </div>
        <div class="card-value">¥168,000</div>
        <div class="card-sub">今日实时数据</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">客流量</span>
          <span class="card-trend up">+8.3%</span>
        </div>
        <div class="card-value">286</div>
        <div class="card-sub">今日进店人数</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">客单价</span>
          <span class="card-trend up">+4.2%</span>
        </div>
        <div class="card-value">¥587</div>
        <div class="card-sub">平均消费金额</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">毛利率</span>
          <span class="card-trend up">+2.1%</span>
        </div>
        <div class="card-value">68.5%</div>
        <div class="card-sub">成本控制优良</div>
      </div>
    </div>

    <div class="main-section">
      <div class="chart-card">
        <h3 class="card-title">营收趋势分析</h3>
        <div class="chart-placeholder">
          <div class="bar-chart">
            <div class="bar-group" v-for="(item, index) in revenueData" :key="index">
              <div class="bar" :style="{ height: item.percent + '%' }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">菜品热销排行</h3>
        <div class="ranking-list">
          <div class="ranking-item" v-for="(item, index) in hotDishes" :key="index">
            <div class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <div class="dish-info">
              <div class="dish-name">{{ item.name }}</div>
              <div class="dish-meta">{{ item.sales }} 份 · ¥{{ item.revenue }}</div>
            </div>
            <div class="dish-percent">
              <div class="percent-bar">
                <div class="percent-fill" :style="{ width: item.percent + '%' }"></div>
              </div>
              <span class="percent-text">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="chart-card">
        <h3 class="card-title">客户分析</h3>
        <div class="customer-grid">
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">68%</div>
              <div class="customer-label">复购率</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">42%</div>
              <div class="customer-label">会员消费占比</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">156</div>
              <div class="customer-label">今日新增会员</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">18:00</div>
              <div class="customer-label">客流高峰时段</div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">成本分析</h3>
        <div class="cost-section">
          <div class="cost-item">
            <div class="cost-header">
              <span class="cost-label">食材成本</span>
              <span class="cost-value">¥48,500</span>
            </div>
            <div class="cost-bar">
              <div class="cost-fill" style="width: 58%; background: #4A7C59;"></div>
            </div>
            <div class="cost-meta">占营收 28.9%</div>
          </div>
          <div class="cost-item">
            <div class="cost-header">
              <span class="cost-label">人力成本</span>
              <span class="cost-value">¥28,000</span>
            </div>
            <div class="cost-bar">
              <div class="cost-fill" style="width: 42%; background: #C4A35A;"></div>
            </div>
            <div class="cost-meta">占营收 16.7%</div>
          </div>
          <div class="cost-item">
            <div class="cost-header">
              <span class="cost-label">运营成本</span>
              <span class="cost-value">¥15,200</span>
            </div>
            <div class="cost-bar">
              <div class="cost-fill" style="width: 28%; background: #5B7B8A;"></div>
            </div>
            <div class="cost-meta">占营收 9.1%</div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">异常预警</h3>
        <div class="alert-list">
          <div class="alert-item danger" v-for="(alert, index) in alerts" :key="index">
            <div class="alert-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
                <path d="M12 9v4"/>
                <path d="M12 17h.01"/>
                <circle cx="12" cy="12" r="10"/>
              </svg>
            </div>
            <div class="alert-content">
              <div class="alert-title">{{ alert.title }}</div>
              <div class="alert-meta">{{ alert.meta }}</div>
            </div>
            <button class="alert-action">处理</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const revenueData = [
  { label: '周一', value: '¥12,500', percent: 45 },
  { label: '周二', value: '¥15,800', percent: 58 },
  { label: '周三', value: '¥13,200', percent: 48 },
  { label: '周四', value: '¥18,600', percent: 68 },
  { label: '周五', value: '¥25,800', percent: 95 },
  { label: '周六', value: '¥32,500', percent: 100 },
  { label: '周日', value: '¥28,600', percent: 88 }
]

const hotDishes = [
  { name: '红烧肉', sales: 156, revenue: '12,480', percent: 100 },
  { name: '清蒸鲈鱼', sales: 128, revenue: '10,240', percent: 82 },
  { name: '水煮鱼', sales: 98, revenue: '7,840', percent: 63 },
  { name: '宫保鸡丁', sales: 86, revenue: '4,300', percent: 55 },
  { name: '蒜蓉西兰花', sales: 72, revenue: '2,880', percent: 46 }
]

const alerts = [
  { title: '五花肉库存不足预警', meta: '剩余 5kg · 低于安全库存 10kg' },
  { title: '出餐超时预警', meta: '牡丹厅订单已超过 20 分钟' },
  { title: '会员储值到期提醒', meta: '3 位金卡会员储值即将到期' }
]
</script>

<style scoped>
.data-screen-page {
  max-width: 1600px;
  margin: 0 auto;
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.card-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.card-trend.up {
  background: rgba(74, 124, 89, 0.08);
  color: #4A7C59;
}

.card-trend.down {
  background: rgba(194, 85, 85, 0.08);
  color: #C25555;
}

.card-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
  margin-bottom: 4px;
}

.card-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}

.main-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
}

.chart-placeholder {
  height: 200px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  height: 100%;
  width: 100%;
  padding-bottom: 8px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar {
  width: 40px;
  background: linear-gradient(180deg, var(--color-primary) 0%, rgba(45, 74, 62, 0.3) 100%);
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  transition: height 0.5s ease;
}

.bar-value {
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.bar-group:hover .bar-value {
  opacity: 1;
}

.bar-label {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.rank-1 {
  background: #C4A35A;
  color: #fff;
}

.rank-2 {
  background: #95A5A6;
  color: #fff;
}

.rank-3 {
  background: #B8835B;
  color: #fff;
}

.rank-4, .rank-5 {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-text-secondary);
}

.dish-info {
  flex: 1;
}

.dish-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.dish-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.dish-percent {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100px;
}

.percent-bar {
  flex: 1;
  height: 6px;
  background: var(--color-border);
  border-radius: 3px;
  overflow: hidden;
}

.percent-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.percent-text {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.bottom-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.customer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.customer-item {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.customer-icon {
  width: 48px;
  height: 48px;
  opacity: 0.6;
  margin-bottom: 12px;
}

.customer-content {
  flex: 1;
}

.customer-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.customer-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.cost-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cost-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cost-header {
  display: flex;
  justify-content: space-between;
}

.cost-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.cost-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.cost-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.cost-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.cost-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(194, 85, 85, 0.06);
  border-radius: var(--radius-sm);
  border-left: 3px solid #C25555;
}

.alert-icon {
  width: 36px;
  height: 36px;
  opacity: 0.7;
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.alert-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.alert-action {
  padding: 6px 12px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.alert-action:hover {
  background: rgba(45, 74, 62, 0.1);
}

@media (max-width: 1200px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-section {
    grid-template-columns: 1fr;
  }
  .bottom-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: 1fr;
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
  .customer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
