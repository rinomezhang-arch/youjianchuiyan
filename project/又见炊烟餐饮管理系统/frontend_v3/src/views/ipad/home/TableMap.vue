<template>
  <div class="ipad-home">
    <!-- 顶部状态栏 -->
    <div class="top-bar">
      <div class="top-left">
        <span class="store-name">{{ ipad.storeName }}</span>
        <span class="staff-name">{{ ipad.staffName }}</span>
      </div>
      <div class="top-center">
        <h1 class="page-title">桌台 · Tables</h1>
      </div>
      <div class="top-right">
        <button class="icon-btn" @click="showSearch = true" title="搜索">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <span>搜索</span>
        </button>
        <button class="icon-btn" @click="showNotice = true" title="通知">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        </button>
        <button class="icon-btn" @click="router.push('/ipad/bookings')" title="预定">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <span>预定</span>
        </button>
        <button class="icon-btn" @click="router.push('/ipad/wait')" title="等位">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>等位</span>
        </button>
        <button class="icon-btn" @click="router.push('/ipad/member')" title="会员">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <span>会员</span>
        </button>
        <button class="icon-btn logout-btn" @click="handleLogout" title="退出">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </div>

    <!-- 区域筛选 -->
    <div class="area-bar">
      <button v-for="a in areas" :key="a" :class="['area-chip', { active: activeArea === a }]" @click="activeArea = a">
        {{ a }}
      </button>
      <div class="area-stats">
        <span class="stat-free">空闲 {{ freeCount }}</span>
        <span class="stat-occupied">占用 {{ occupiedCount }}</span>
        <span class="stat-reserved">预定 {{ reservedCount }}</span>
      </div>
    </div>

    <!-- 桌台网格 -->
    <div class="table-grid">
      <div
        v-for="t in filteredTables"
        :key="t.table_id || t.id"
        :class="['table-card', statusClass(t)]"
        @click="handleTableClick(t)"
      >
        <div class="table-number">{{ t.table_number || t.table_name }}</div>
        <div class="table-area-label">{{ t.table_area }}</div>
        <div v-if="t.booking" class="table-info">
          <div class="guest-name">{{ t.booking.customer_name }}</div>
          <div class="guest-count">{{ t.booking.guest_count }}人</div>
        </div>
        <div v-else class="table-capacity">
          {{ t.table_capacity || t.table_seat_num || '—' }}人
        </div>
        <span class="status-indicator"></span>
      </div>
    </div>

    <!-- 开台弹窗 -->
    <Transition name="modal">
      <div v-if="showOpenModal" class="modal-overlay" @click.self="showOpenModal = false">
        <div class="modal-box">
          <div class="modal-header">
            <h3>开台 · Open Table</h3>
            <span class="modal-table-name">{{ selectedTable?.table_number || selectedTable?.table_name }}</span>
          </div>
          <div class="modal-body">
            <div class="form-row">
              <label>用餐人数 · Guests</label>
              <div class="qty-control">
                <button @click="guestCount = Math.max(1, guestCount - 1)">−</button>
                <span>{{ guestCount }}</span>
                <button @click="guestCount++">+</button>
              </div>
            </div>
            <div class="form-row">
              <label>备注 · Remark</label>
              <input v-model="openRemark" placeholder="可选" class="remark-input" />
            </div>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showOpenModal = false">取消 · Cancel</button>
            <button class="btn-confirm" @click="confirmOpenTable">确认开台 · Confirm</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 桌台右键操作菜单 -->
    <Transition name="modal">
      <div v-if="showContextMenu" class="modal-overlay" @click.self="showContextMenu = false">
        <div class="context-menu-box">
          <div class="context-table-name">{{ contextTable?.table_number || contextTable?.table_name }}</div>
          <button class="context-item" @click="handleContextAction('enter')">进入点餐 · Enter Order</button>
          <button class="context-item" @click="handleContextAction('transfer')">转台 · Transfer</button>
          <button class="context-item" @click="handleContextAction('merge')">合台 · Merge</button>
          <button class="context-item danger" @click="handleContextAction('clear')">清台 · Clear</button>
          <button class="context-item cancel" @click="showContextMenu = false">取消</button>
        </div>
      </div>
    </Transition>

    <!-- 转台弹窗 -->
    <TransferTablePopup
      :visible="showTransferPopup"
      :source-table="contextTable"
      :all-tables="tables"
      :booking-id="contextTable?.booking?.booking_id"
      @close="showTransferPopup = false"
      @done="onTransferDone"
    />

    <!-- 合台弹窗 -->
    <MergeTablePopup
      :visible="showMergePopup"
      :target-table="contextTable"
      :all-tables="tables"
      @close="showMergePopup = false"
      @done="onMergeDone"
    />

    <!-- 清台弹窗 -->
    <ClearTablePopup
      :visible="showClearPopup"
      :table="contextTable"
      :booking-id="contextTable?.booking?.booking_id"
      @close="showClearPopup = false"
      @done="onClearDone"
    />

    <!-- 全局搜索弹窗 -->
    <GlobalSearch :visible="showSearch" @close="showSearch = false" @select="handleSearchSelect" />
    
    <!-- 通知弹窗 -->
    <NoticePopup :visible="showNotice" @close="showNotice = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadTableAll, ipadTableOpen } from '@/api/ipad'
import { ElMessage } from 'element-plus'
import GlobalSearch from './components/GlobalSearch.vue'
import NoticePopup from './components/NoticePopup.vue'
import TransferTablePopup from './components/TransferTablePopup.vue'
import MergeTablePopup from './components/MergeTablePopup.vue'
import ClearTablePopup from './components/ClearTablePopup.vue'

const router = useRouter()
const ipad = useIpadStore()

const tables = ref([])
const activeArea = ref('全部')
const showOpenModal = ref(false)
const showSearch = ref(false)
const showNotice = ref(false)
const showContextMenu = ref(false)
const showTransferPopup = ref(false)
const showMergePopup = ref(false)
const showClearPopup = ref(false)
const selectedTable = ref(null)
const contextTable = ref(null)
const guestCount = ref(4)
const openRemark = ref('')

// 区域列表
const areas = computed(() => {
  const set = new Set(['全部'])
  tables.value.forEach(t => { if (t.table_area) set.add(t.table_area) })
  return [...set]
})

// 过滤桌台
const filteredTables = computed(() => {
  if (activeArea.value === '全部') return tables.value
  return tables.value.filter(t => t.table_area === activeArea.value)
})

// 统计
const freeCount = computed(() => tables.value.filter(t => !t.booking && (t.table_status === 0 || t.table_status === 'available')).length)
const occupiedCount = computed(() => tables.value.filter(t => t.booking || t.table_status === 1 || t.table_status === 'occupied').length)
const reservedCount = computed(() => tables.value.filter(t => t.table_status === 2 || t.table_status === 'reserved').length)

function statusClass(t) {
  if (t.booking) return 'occupied'
  const s = t.table_status
  if (s === 0 || s === 'available' || s === 'free') return 'free'
  if (s === 1 || s === 'occupied') return 'occupied'
  if (s === 2 || s === 'reserved') return 'reserved'
  if (s === 3 || s === 'maintenance') return 'maintenance'
  return 'free'
}

let longPressTimer = null

function handleTableClick(t) {
  if (t.booking) {
    // 已占用 → 右键菜单
    contextTable.value = t
    showContextMenu.value = true
    return
  }
  if (t.table_status === 3 || t.table_status === 'maintenance') {
    ElMessage.warning('该桌台维护中')
    return
  }
  // 空闲 → 弹窗开台
  selectedTable.value = t
  guestCount.value = 4
  openRemark.value = ''
  showOpenModal.value = true
}

function handleContextAction(action) {
  showContextMenu.value = false
  switch (action) {
    case 'enter':
      ipad.openTable(contextTable.value.booking)
      router.push(`/ipad/order/${contextTable.value.booking.booking_id}`)
      break
    case 'transfer':
      showTransferPopup.value = true
      break
    case 'merge':
      showMergePopup.value = true
      break
    case 'clear':
      showClearPopup.value = true
      break
  }
}

function onTransferDone(data) {
  showTransferPopup.value = false
  loadTables()
  ElMessage.success(`已转台到 ${data.to_table_id}`)
}

function onMergeDone(data) {
  showMergePopup.value = false
  loadTables()
  ElMessage.success(`已合并桌台`)
}

function onClearDone(data) {
  showClearPopup.value = false
  loadTables()
  ElMessage.success('已清台')
}

async function confirmOpenTable() {
  try {
    const res = await ipadTableOpen({
      table_id: selectedTable.value.table_id || selectedTable.value.id,
      guest_count: guestCount.value,
      remark: openRemark.value || undefined
    })
    if (res.code === 200) {
      ElMessage.success('开台成功')
      ipad.openTable(res.data)
      showOpenModal.value = false
      router.push(`/ipad/order/${res.data.booking_id}`)
    } else {
      ElMessage.error(res.msg || '开台失败')
    }
  } catch (e) {
    // 降级模拟
    console.warn('Open table API failed:', e.message)
    const mockBooking = {
      booking_id: 'MOCK_' + Date.now(),
      table_id: selectedTable.value.table_id || selectedTable.value.id,
      table_name: selectedTable.value.table_number || selectedTable.value.table_name,
      guest_count: guestCount.value
    }
    ipad.openTable(mockBooking)
    showOpenModal.value = false
    ElMessage.warning('演示模式（后端未连接）')
    router.push(`/ipad/order/${mockBooking.booking_id}`)
  }
}

function handleLogout() {
  ipad.logout()
  router.push('/ipad/login')
}

function handleSearchSelect(item) {
  if (item.dish_name) {
    router.push(`/ipad/dish/${item.dish_id || item.id}`)
  } else if (item.table_name) {
    // 找到对应桌台并滚动定位
    const t = tables.value.find(x => (x.table_id || x.id) === (item.table_id || item.id))
    if (t) handleTableClick(t)
  }
}

async function loadTables() {
  try {
    const res = await ipadTableAll()
    if (res.code === 200) {
      tables.value = res.data || []
    }
  } catch (e) {
    console.warn('Table API failed, using mock:', e.message)
    const zones = ['一楼包厢', '一楼散客大厅', '二楼1号服务厅', '二楼2号服务厅', '三楼宴会厅']
    tables.value = Array.from({ length: 20 }, (_, i) => ({
      table_id: i + 1,
      table_number: `${i + 1}号桌`,
      table_area: zones[i % zones.length],
      table_capacity: [4, 6, 8, 10, 12][i % 5],
      table_status: i < 14 ? 0 : (i < 18 ? 1 : 2),
      booking: i >= 14 && i < 18 ? { customer_name: '张先生', guest_count: 6, booking_id: `B${i}` } : null
    }))
  }
}

onMounted(() => loadTables())
</script>

<style scoped>
.ipad-home {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  background: var(--color-bg);
}

/* 顶部栏 */
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}
.top-left { display: flex; align-items: center; gap: 12px; }
.store-name { font-size: 14px; font-weight: 600; color: var(--color-primary); padding: 4px 12px; background: rgba(45,74,62,0.06); border-radius: 4px; }
.staff-name { font-size: 13px; color: var(--color-text-secondary); }
.top-center { position: absolute; left: 50%; transform: translateX(-50%); }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.top-right { display: flex; align-items: center; gap: 8px; }
.icon-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); color: var(--color-text-secondary);
  font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.icon-btn:hover { border-color: var(--color-primary); color: var(--color-primary); background: rgba(45,74,62,0.04); }
.icon-btn svg { flex-shrink: 0; }
.logout-btn { color: var(--color-text-muted); }
.logout-btn:hover { color: var(--color-danger); border-color: var(--color-danger); }

/* 区域筛选 */
.area-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 24px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}
.area-chip {
  padding: 6px 16px; border-radius: 20px;
  border: 1px solid var(--color-border);
  background: transparent; color: var(--color-text-secondary);
  font-size: 13px; cursor: pointer; transition: all 0.2s;
  font-family: var(--font-family);
}
.area-chip.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.area-chip:not(.active):hover { border-color: var(--color-primary); color: var(--color-primary); }
.area-stats { margin-left: auto; display: flex; gap: 16px; font-size: 12px; }
.stat-free { color: var(--color-success); }
.stat-occupied { color: var(--color-warning); }
.stat-reserved { color: var(--color-info); }

/* 桌台网格 */
.table-grid {
  flex: 1; overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px; padding: 20px 24px;
  align-content: start;
}

.table-card {
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  border: 2px solid var(--color-border);
  background: var(--color-card);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.25s;
  position: relative; overflow: hidden;
  padding: 12px;
}
.table-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }

/* 状态样式 */
.table-card.free { border-color: rgba(74, 124, 89, 0.3); }
.table-card.free:hover { border-color: var(--color-success); background: rgba(74, 124, 89, 0.04); }
.table-card.free .status-indicator { background: var(--color-success); }

.table-card.occupied { border-color: rgba(212, 168, 83, 0.4); background: rgba(212, 168, 83, 0.04); }
.table-card.occupied:hover { border-color: var(--color-warning); }
.table-card.occupied .status-indicator { background: var(--color-warning); }

.table-card.reserved { border-color: rgba(91, 123, 138, 0.3); background: rgba(91, 123, 138, 0.04); }
.table-card.reserved:hover { border-color: var(--color-info); }
.table-card.reserved .status-indicator { background: var(--color-info); }

.table-card.maintenance { border-color: rgba(194, 85, 85, 0.3); opacity: 0.5; cursor: not-allowed; }
.table-card.maintenance .status-indicator { background: var(--color-danger); }

.table-number { font-size: 22px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; font-family: var(--font-family); }
.table-area-label { font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }
.table-info { margin-top: 8px; text-align: center; }
.guest-name { font-size: 13px; font-weight: 600; color: var(--color-text); }
.guest-count { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.table-capacity { font-size: 12px; color: var(--color-text-muted); margin-top: 6px; }
.status-indicator { position: absolute; top: 8px; right: 8px; width: 8px; height: 8px; border-radius: 50%; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal-box {
  background: var(--color-card); border-radius: var(--radius-xl);
  width: 420px; max-width: 90vw;
  box-shadow: var(--shadow-xl); overflow: hidden;
}
.modal-header {
  padding: 20px 24px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
}
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.modal-table-name { font-size: 14px; color: var(--color-primary); font-weight: 600; padding: 4px 12px; background: rgba(45,74,62,0.06); border-radius: 4px; }
.modal-body { padding: 24px; }
.form-row { margin-bottom: 20px; }
.form-row label { display: block; font-size: 14px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px; }
.qty-control { display: flex; align-items: center; gap: 16px; }
.qty-control button { width: 40px; height: 40px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 20px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.qty-control button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.qty-control span { font-size: 24px; font-weight: 700; min-width: 40px; text-align: center; }
.remark-input { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; }
.modal-actions { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--color-border); }
.btn-cancel { flex: 1; padding: 12px; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { flex: 1; padding: 12px; border-radius: var(--radius-md); border: none; background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 600; cursor: pointer; }
.btn-confirm:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }

/* 右键菜单 */
.context-menu-box {
  background: var(--color-card); border-radius: var(--radius-xl);
  width: 300px; overflow: hidden; box-shadow: var(--shadow-xl);
  display: flex; flex-direction: column;
}
.context-table-name {
  padding: 16px 20px; font-size: 16px; font-weight: 700;
  color: var(--color-primary); border-bottom: 1px solid var(--color-border);
  letter-spacing: 1px; text-align: center;
}
.context-item {
  width: 100%; padding: 14px 20px; border: none; border-bottom: 1px solid var(--color-border-light);
  background: transparent; font-size: 14px; cursor: pointer; transition: all 0.15s;
  text-align: left; color: var(--color-text); font-family: var(--font-family);
}
.context-item:hover { background: rgba(45,74,62,0.06); color: var(--color-primary); }
.context-item.danger { color: var(--color-danger); }
.context-item.danger:hover { background: rgba(194,85,85,0.06); }
.context-item.cancel { color: var(--color-text-muted); font-size: 13px; border-bottom: none; }

/* 动画 */
.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-box { transform: scale(0.95); }
</style>
