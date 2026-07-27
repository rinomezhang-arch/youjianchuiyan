<template>
  <div class="front-office">
    <div class="page-header">
      <h2 class="page-title">前厅运营 · Front Office</h2>
      <p class="page-subtitle">客户接待 · 桌台服务 · 收银结算 · Reception · Table Service · Cashier</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">实时在店人数 · Guests</div>
          <div class="stat-value">{{ stats.guestCount }}</div>
          <div class="stat-sub">{{ stats.tableCount }}桌 · 平均{{ stats.avgGuests }}人/桌</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#D4A853' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="16" rx="2"/>
            <path d="M2 9h20"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">待开台 · Pending</div>
          <div class="stat-value">{{ stats.pendingTables }}</div>
          <div class="stat-sub">需立即处理</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日营收 · Revenue</div>
          <div class="stat-value">¥{{ stats.todayRevenue.toLocaleString() }}</div>
          <div class="stat-sub">较昨日 +{{ stats.revenueGrowth }}%</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#C25555' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <path d="M12 17h.01"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">未处理客诉 · Complaints</div>
          <div class="stat-value">{{ stats.pendingComplaints }}</div>
          <div class="stat-sub">需及时跟进</div>
        </div>
      </div>
    </div>

    <!-- 快捷功能入口 -->
    <div class="quick-actions-card">
      <h3 class="section-title">快捷功能入口 · Quick Access</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('front-desk')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <span class="action-text">客户接待 · Reception</span>
        </div>
        <div class="action-card" @click="goTo('table-board')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <rect x="2" y="3" width="20" height="16" rx="2"/>
              <path d="M2 9h20"/>
              <path d="M8 21h8"/>
            </svg>
          </div>
          <span class="action-text">桌台管理 · Tables</span>
        </div>
        <div class="action-card" @click="goTo('cashier')">
          <div class="action-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <rect x="2" y="3" width="20" height="16" rx="2"/>
              <path d="M2 9h20"/>
              <path d="M12 17v4"/>
            </svg>
          </div>
          <span class="action-text">收银台 · Cashier</span>
        </div>
        <div class="action-card" @click="goTo('kitchen')">
          <div class="action-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
            </svg>
          </div>
          <span class="action-text">传菜管控 · Kitchen</span>
        </div>
        <div class="action-card" @click="goTo('bookings')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <path d="M16 2v4"/>
              <path d="M8 2v4"/>
              <path d="M3 10h18"/>
            </svg>
          </div>
          <span class="action-text">宴会预定 · Banquet</span>
        </div>
        <div class="action-card" @click="goTo('customers')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
            </svg>
          </div>
          <span class="action-text">VIP管理 · VIP</span>
        </div>
        <div class="action-card" @click="goTo('complaints')">
          <div class="action-icon" style="background: rgba(194,85,85,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            </svg>
          </div>
          <span class="action-text">客诉处理 · Complaints</span>
        </div>
        <div class="action-card" @click="goTo('service-log')">
          <div class="action-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
          </div>
          <span class="action-text">服务日志 · Service Log</span>
        </div>
      </div>
    </div>

    <!-- 底部区域 -->
    <div class="bottom-section">
      <!-- 实时桌台视图 -->
      <div class="table-view-card">
        <h3 class="section-title">实时桌台视图 · Live Table View</h3>
        <div v-loading="tablesLoading" class="table-layouts">
          <div v-for="area in tableAreas" :key="area.name" class="table-layout">
            <div class="area-header">
              <span class="area-name">{{ area.name }}</span>
              <span class="area-status">{{ area.occupied }}/{{ area.total }} 桌在用</span>
            </div>
            <div class="table-grid">
              <div
                v-for="table in area.tables"
                :key="table.id"
                :class="['table-item', { occupied: table.status === 'occupied', free: table.status === 'free' }]"
                @click="goToTable(table)"
              >
                <span class="table-number">{{ table.name }}</span>
                <span v-if="table.status === 'occupied'" class="table-people">{{ table.guestCount }}人</span>
                <span v-else class="table-status">空闲</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 今日工单 -->
      <div class="tasks-card">
        <h3 class="section-title">今日前厅工单 · Today's Tasks</h3>
        <div v-loading="tasksLoading" class="task-list">
          <div v-if="tasks.length === 0" class="empty-tasks">暂无待处理工单</div>
          <div v-for="(task, index) in tasks" :key="index" class="task-item">
            <div class="task-priority" :class="task.priority"></div>
            <div class="task-content">
              <div class="task-title">{{ task.title }}</div>
              <div class="task-meta">{{ task.time }} · {{ task.location }}</div>
            </div>
            <button class="task-action" @click="handleTask(task)">处理</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部快捷操作栏 -->
    <div class="quick-bar">
      <button class="quick-btn" @click="goTo('table-board')">开台 · Open Table</button>
      <button class="quick-btn" @click="goTo('menu')">加菜 · Add Dish</button>
      <button class="quick-btn" @click="goTo('cashier')">结账 · Checkout</button>
      <button class="quick-btn" @click="goTo('kitchen')">呼叫后厨 · Call Kitchen</button>
      <button class="quick-btn" @click="goTo('cleaning')">呼叫保洁 · Call Cleaning</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

function getLocalDateStr() {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 统计数据
const stats = ref({
  guestCount: 0,
  tableCount: 0,
  avgGuests: 0,
  pendingTables: 0,
  todayRevenue: 0,
  revenueGrowth: 0,
  pendingComplaints: 0
})

// 桌台数据
const tablesLoading = ref(false)
const tableAreas = ref([])

// 工单数据
const tasksLoading = ref(false)
const tasks = ref([])

// 加载统计数据
async function fetchStats() {
  try {
    const today = getLocalDateStr()
    const res = await request.get('/tables/board', { params: { storeId: 1, date: today, period: 'all' } })
    if (res?.data) {
      const tables = res.data
      const occupiedTables = tables.filter(t => t.booking_id)
      const guestCount = occupiedTables.reduce((sum, t) => sum + (t.bm_guest_count || 0), 0)
      const occupiedCount = occupiedTables.length
      const avgGuests = occupiedCount > 0 ? Math.round(guestCount / occupiedCount) : 0
      const pendingTables = tables.filter(t => !t.booking_id && t.table_area && t.table_area.includes('包厢') && t.booking_status === 'pending').length
      const todayRevenue = occupiedTables.reduce((sum, t) => sum + (t.dishes_count || 0) * 68, 0)
      
      stats.value = {
        guestCount: guestCount,
        tableCount: occupiedCount,
        avgGuests: avgGuests,
        pendingTables: pendingTables || 0,
        todayRevenue: todayRevenue,
        revenueGrowth: 0,
        pendingComplaints: 0
      }
    }
  } catch (e) {
    console.warn('获取统计数据失败', e)
  }
}

// 加载桌台数据
async function fetchTables() {
  tablesLoading.value = true
  try {
    const today = getLocalDateStr()
    const res = await request.get('/tables/board', { params: { storeId: 1, date: today, period: 'all' } })
    if (res?.data) {
      const tables = res.data
      
      // 按区域分组
      const areaMap = {}
      tables.forEach(t => {
        const area = t.table_area || '其他'
        if (!areaMap[area]) {
          areaMap[area] = { name: area, tables: [], occupied: 0, total: 0 }
        }
        const isOccupied = !!t.booking_id
        areaMap[area].tables.push({
          id: t.table_id,
          name: t.table_name || t.table_number || '桌台',
          status: isOccupied ? 'occupied' : 'free',
          guestCount: t.bm_guest_count || 0,
          capacity: t.table_capacity || t.capacity || 0
        })
        areaMap[area].total++
        if (isOccupied) areaMap[area].occupied++
      })
      
      tableAreas.value = Object.values(areaMap)
    }
  } catch (e) {
    console.warn('获取桌台数据失败', e)
  } finally {
    tablesLoading.value = false
  }
}

// 加载工单数据
async function fetchTasks() {
  tasksLoading.value = true
  try {
    // TODO: 从后端获取工单数据
    tasks.value = [
      { title: '待安排服务 - 3号桌', time: '14:30', location: '大厅 3号', priority: 'high' },
      { title: '待买单 - 牡丹厅', time: '14:20', location: '包厢 牡丹厅', priority: 'medium' },
      { title: '清洁待收台 - 5号桌', time: '14:15', location: '大厅 5号', priority: 'low' },
      { title: '催菜提醒 - 荷花厅', time: '14:00', location: '包厢 荷花厅', priority: 'high' },
      { title: '待安排服务 - 8号桌', time: '13:55', location: '大厅 8号', priority: 'medium' }
    ]
  } catch (e) {
    console.error('获取工单数据失败', e)
  } finally {
    tasksLoading.value = false
  }
}

function goTo(path) {
  router.push(`/dashboard/${path}`)
}

function goToTable(table) {
  router.push(`/dashboard/table-board?table=${table.id}`)
}

function handleTask(task) {
  ElMessage.success(`已处理：${task.title}`)
  // TODO: 调用后端API更新工单状态
}

onMounted(() => {
  fetchStats()
  fetchTables()
  fetchTasks()
})
</script>

<style scoped>
.front-office {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
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
  flex-shrink: 0;
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
  grid-template-columns: repeat(4, 1fr);
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
  text-align: center;
}

.bottom-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.table-view-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.table-layouts {
  min-height: 200px;
}

.table-layout {
  margin-bottom: 20px;
}

.table-layout:last-child {
  margin-bottom: 0;
}

.area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
}

.area-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.area-status {
  font-size: 12px;
  color: var(--color-text-muted);
}

.table-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-item {
  width: 90px;
  height: 70px;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
}

.table-item.occupied {
  background: rgba(45, 74, 62, 0.08);
  border: 1px solid rgba(45, 74, 62, 0.2);
}

.table-item.free {
  background: var(--color-bg-alt);
  border: 1px dashed var(--color-border);
}

.table-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.table-number {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.table-people {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.table-status {
  font-size: 10px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.tasks-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.empty-tasks {
  text-align: center;
  color: var(--color-text-muted);
  padding: 40px 0;
  font-size: 13px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.task-priority {
  width: 4px;
  height: 36px;
  border-radius: 2px;
  flex-shrink: 0;
}

.task-priority.high {
  background: #C25555;
}

.task-priority.medium {
  background: #D4A853;
}

.task-priority.low {
  background: #4A7C59;
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.task-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.task-action {
  padding: 6px 12px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.task-action:hover {
  background: rgba(45, 74, 62, 0.1);
}

.quick-bar {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.quick-btn {
  flex: 1;
  padding: 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-alt);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.quick-btn:hover {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-primary);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(4, 1fr);
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
  .quick-bar {
    flex-direction: column;
  }
}
</style>
