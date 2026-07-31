<template>
  <div class="kitchen-page">
    <div class="page-header">
      <h2 class="page-title">厨房管理总览</h2>
      <p class="page-subtitle">Kitchen Management</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">待制作订单</div>
          <div class="stat-value">8</div>
          <div class="stat-sub">需立即处理</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#C25555' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">出餐超时预警</div>
          <div class="stat-value">2</div>
          <div class="stat-sub">需加急处理</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日出品总数</div>
          <div class="stat-value">156</div>
          <div class="stat-sub">较昨日 +8%</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#D4A853' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">退菜率</div>
          <div class="stat-value">2.3%</div>
          <div class="stat-sub">低于目标 5%</div>
        </div>
      </div>
    </div>

    <div class="quick-actions-card">
      <h3 class="section-title">快捷功能</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
            </svg>
          </div>
          <span class="action-text">菜品制作</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <span class="action-text">菜品研发</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(194,85,85,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
              <path d="M3 21l1.65-3.8a9 9 0 1 1 3.4 2.9L3 21"/>
            </svg>
          </div>
          <span class="action-text">后厨卫生</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M9 9h6"/>
              <path d="M9 15h6"/>
            </svg>
          </div>
          <span class="action-text">档口管控</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <span class="action-text">出餐时效</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(194,85,85,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="9" y1="15" x2="15" y2="15"/>
            </svg>
          </div>
          <span class="action-text">退菜管理</span>
        </div>
        <div class="action-card" @click="goTo('menu-soldout')">
          <div class="action-icon" style="background: rgba(139,105,20,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#8B6914" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
          </div>
          <span class="action-text">沽清内容</span>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="order-screen-card">
        <h3 class="section-title">分档口订单滚动屏</h3>
        <div class="tabs">
          <button class="tab-btn active">热菜</button>
          <button class="tab-btn">凉菜</button>
          <button class="tab-btn">面点</button>
          <button class="tab-btn">粗加工</button>
        </div>
        <div class="order-list">
          <div class="order-item" v-for="(order, index) in orders" :key="index">
            <div class="order-priority" :class="order.priority"></div>
            <div class="order-info">
              <div class="order-header">
                <span class="order-table">{{ order.table }}</span>
                <span class="order-time">{{ order.time }}</span>
              </div>
              <div class="order-dishes">
                <span v-for="(dish, i) in order.dishes" :key="i" class="dish-tag">{{ dish }}</span>
              </div>
            </div>
            <div class="order-status" :class="order.status">
              {{ order.statusText }}
            </div>
          </div>
        </div>
      </div>

      <div class="side-cards">
        <div class="alert-card">
          <h3 class="section-title">损耗预警</h3>
          <div class="alert-list">
            <div class="alert-item" v-for="(item, index) in alerts" :key="index">
              <div class="alert-icon" :class="item.type">
                <svg v-if="item.type === 'warning'" viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
                  <path d="M12 9v4"/>
                  <path d="M12 17h.01"/>
                  <circle cx="12" cy="12" r="10"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
                  <path d="M3 6h18"/>
                  <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                  <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                </svg>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ item.title }}</div>
                <div class="alert-meta">{{ item.meta }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="staff-card">
          <h3 class="section-title">后厨排班</h3>
          <div class="staff-list">
            <div class="staff-item" v-for="(staff, index) in staffs" :key="index">
              <div class="staff-status" :class="staff.status"></div>
              <div class="staff-info">
                <div class="staff-name">{{ staff.name }}</div>
                <div class="staff-role">{{ staff.role }}</div>
              </div>
              <div class="staff-time">{{ staff.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const orders = [
  { table: '牡丹厅', time: '14:30', dishes: ['红烧肉', '清蒸鲈鱼'], priority: 'high', status: 'pending', statusText: '待制作' },
  { table: '荷花厅', time: '14:25', dishes: ['蒜蓉西兰花', '宫保鸡丁'], priority: 'medium', status: 'making', statusText: '制作中' },
  { table: '大厅3号', time: '14:20', dishes: ['酸辣土豆丝'], priority: 'low', status: 'pending', statusText: '待制作' },
  { table: '大厅5号', time: '14:15', dishes: ['水煮鱼', '干锅牛蛙'], priority: 'high', status: 'making', statusText: '制作中' },
  { table: '大厅8号', time: '14:10', dishes: ['麻婆豆腐', '回锅肉'], priority: 'medium', status: 'pending', statusText: '待制作' },
  { table: '牡丹厅', time: '14:05', dishes: ['拔丝红薯'], priority: 'low', status: 'ready', statusText: '已完成' }
]

const alerts = [
  { title: '五花肉库存不足', meta: '剩余 5kg · 低于安全库存', type: 'warning' },
  { title: '青菜已过期', meta: '入库超过 7天 · 需报废', type: 'danger' },
  { title: '生姜临期预警', meta: '剩余 3天 · 建议优先使用', type: 'warning' }
]

const staffs = [
  { name: '张大厨', role: '热菜主厨', time: '09:00-17:00', status: 'online' },
  { name: '李师傅', role: '凉菜师傅', time: '09:00-17:00', status: 'online' },
  { name: '王师傅', role: '面点师傅', time: '09:00-17:00', status: 'online' },
  { name: '陈师傅', role: '配菜', time: '10:00-18:00', status: 'offline' }
]

function goTo(path) {
  router.push(`/dashboard/${path}`)
}
</script>

<style scoped>
.kitchen-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: currentColor;
  opacity: 0.03;
  border-radius: 0 0 0 80px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  opacity: 0.7;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.quick-actions-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
}

.action-card:hover {
  background: var(--color-bg-side);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.action-icon svg {
  width: 22px;
  height: 22px;
}

.action-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.bottom-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.order-screen-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border-light);
  padding-bottom: 8px;
}

.tab-btn {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.tab-btn.active {
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.order-priority {
  width: 4px;
  height: 40px;
  border-radius: 2px;
}

.order-priority.high {
  background: #C25555;
}

.order-priority.medium {
  background: #D4A853;
}

.order-priority.low {
  background: #4A7C59;
}

.order-info {
  flex: 1;
}

.order-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.order-table {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.order-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.order-dishes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dish-tag {
  padding: 2px 8px;
  font-size: 11px;
  color: var(--color-text-secondary);
  background: rgba(45, 74, 62, 0.06);
  border-radius: 4px;
}

.order-status {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}

.order-status.pending {
  background: rgba(194, 85, 85, 0.08);
  color: #C25555;
}

.order-status.making {
  background: rgba(196, 163, 90, 0.08);
  color: #C4A35A;
}

.order-status.ready {
  background: rgba(74, 124, 89, 0.08);
  color: #4A7C59;
}

.side-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.alert-card, .staff-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.alert-list, .staff-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.alert-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-icon.warning {
  background: rgba(212, 168, 83, 0.08);
}

.alert-icon.danger {
  background: rgba(194, 85, 85, 0.08);
}

.alert-icon svg {
  width: 18px;
  height: 18px;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.alert-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.staff-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.staff-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.staff-status.online {
  background: #4A7C59;
}

.staff-status.offline {
  background: #95A5A6;
}

.staff-info {
  flex: 1;
}

.staff-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.staff-role {
  font-size: 11px;
  color: var(--color-text-muted);
}

.staff-time {
  font-size: 11px;
  color: var(--color-text-muted);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
