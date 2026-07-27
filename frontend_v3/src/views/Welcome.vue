<template>
  <div class="welcome-page">
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
    </div>

    <div class="main-container">
      <div class="brand-section">
        <div class="logo-container">
          <img src="@/assets/images/logo.png" alt="又见炊烟" class="logo-image" />
        </div>
        <h1 class="brand-title">又见炊烟</h1>
        <p class="brand-subtitle">私厨 · 宴会 · 臻选</p>
        <div class="brand-divider"></div>
        <p class="brand-slogan">人间烟火味 · 最抚凡人心</p>
      </div>

      <div class="entry-section">
        <div class="entry-grid">
          <div class="entry-card customer" @click="showStoreModal = true">
            <div class="entry-icon-wrapper">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <div class="entry-title">客户点菜</div>
            <div class="entry-desc">自助扫码 · 浏览菜单 · 一键下单</div>
            <div class="entry-arrow">→</div>
          </div>

          <div class="entry-card admin" @click="goAdmin">
            <div class="entry-icon-wrapper">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <path d="M9 12l2 2 4-4"/>
              </svg>
            </div>
            <div class="entry-title">后台管理</div>
            <div class="entry-desc">门店管理 · 数据报表 · 系统配置</div>
            <div class="entry-arrow">→</div>
          </div>
        </div>
      </div>

      <div class="beian-section">
        <div class="beian-content">
          <a href="https://beian.mps.gov.cn/#/query/webSearch?code=32132302010492" rel="noreferrer" target="_blank" class="beian-link">
            <span class="beian-icon">
              <svg viewBox="0 0 36 36" fill="currentColor">
                <circle cx="18" cy="18" r="16" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
                <path d="M18 6v8l6 3" stroke="rgba(255,255,255,0.9)" stroke-width="2" stroke-linecap="round" fill="none"/>
                <circle cx="18" cy="18" r="5" fill="rgba(255,255,255,0.2)"/>
                <path d="M15 18h6M18 15v6" stroke="rgba(255,255,255,0.6)" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </span>
            <span class="beian-text">苏公网安备32132302010492号</span>
          </a>
        </div>
        <div class="copyright">© 2026 又见炊烟餐饮管理系统</div>
      </div>
    </div>

    <div v-if="showStoreModal" class="modal-overlay" @click="closeStoreModal">
      <div class="modal-content" @click.stop>
        <button class="modal-close" @click="closeStoreModal">×</button>
        <div class="modal-header">
          <h3>选择门店</h3>
          <span class="modal-subtitle">Select a Store</span>
        </div>
        <div class="store-list">
          <div
            v-for="store in stores"
            :key="store.id"
            :class="['store-item', { active: store.status === 'open', disabled: store.status !== 'open' }]"
            @click="selectStore(store)"
          >
            <div class="store-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>
            <div class="store-info">
              <div class="store-name">{{ store.name }}</div>
              <div class="store-addr">{{ store.address }}</div>
            </div>
            <div :class="['store-status', store.status]">
              {{ store.status === 'open' ? '营业中' : '未开业' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showTableModal" class="modal-overlay" @click="closeTableModal">
      <div class="modal-content table-modal" @click.stop>
        <button class="modal-close" @click="closeTableModal">×</button>
        <div class="modal-header">
          <div class="header-logo">
            <img src="@/assets/images/logo.png" alt="又见炊烟" />
          </div>
          <h3>{{ selectedStore?.name || '选择桌台' }}</h3>
          <span class="modal-subtitle">Select Table</span>
          <div class="time-slot-badge" @dblclick="handleTimeSlotDoubleClick">
            {{ currentTimeSlot }}时段
          </div>
          <div class="header-gold-line">
            <div class="header-gold-shimmer"></div>
          </div>
        </div>
        
        <div class="table-filters">
          <div class="filter-group">
            <button
              v-for="floor in floorOptions"
              :key="floor.value"
              :class="['filter-btn', { active: selectedFloor === floor.value }]"
              @click="selectedFloor = floor.value"
            >
              {{ floor.label }}
            </button>
          </div>
          <div class="filter-divider gold"></div>
        </div>
        
        <div class="table-grid">
          <div
            v-for="table in filteredTables"
            :key="table.id"
            :class="['table-item', table.table_type, { reserved: table.status === 'reserved' }]"
            @click="selectTable(table)"
          >
            <div class="table-number">{{ table.table_number }}</div>
            <div class="table-type">{{ table.table_type || '散台' }}</div>
            <div v-if="table.status === 'reserved'" class="table-reserved-info">
              <span class="reserved-name">{{ table.reserved_name }}</span>
              <span class="reserved-count">{{ table.reserved_count }}人</span>
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
import { useUserStore } from '@/store/user'
import { getStoreList } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const showStoreModal = ref(false)
const showTableModal = ref(false)
const selectedStore = ref({})
const stores = ref([
  { id: 1, name: '宁国店', nameEn: 'Ningguo', address: '宁国市青龙西路1号', status: 'open', tables: 13, capacity: 150 },
  { id: 2, name: '宣城店', nameEn: 'Xuancheng', address: '宣城市状元南路88号', status: 'open', tables: 15, capacity: 180 },
])
const tables = ref([])

const selectedFloor = ref('一楼')
const selectedStatus = ref('available')

const floorOptions = ref([
  { value: '一楼', label: '一楼' },
  { value: '二楼', label: '二楼' },
  { value: '三楼', label: '三楼' },
])

const statusOptions = ref([
  { value: 'available', label: '可使用' },
])

const currentTimeSlot = ref('')

const updateTimeSlot = () => {
  const hour = new Date().getHours()
  if (hour >= 6 && hour < 13) {
    currentTimeSlot.value = '上午'
  } else {
    currentTimeSlot.value = '下午'
  }
}

onMounted(() => {
  updateTimeSlot()
})

const filteredTables = computed(() => {
  return tables.value.filter(table => {
    const floorMatch = String(table.floor) === String(selectedFloor.value)
    const statusMatch = table.status === 'available' || table.status === 'reserved'
    return floorMatch && statusMatch
  })
})

const goAdmin = () => {
  router.push('/login')
}

const handleTimeSlotDoubleClick = () => {
  if (confirm('预定其他时间段吗？')) {
    router.push('/dashboard/reservation')
  }
}

const closeStoreModal = () => {
  showStoreModal.value = false
}

const closeTableModal = () => {
  showTableModal.value = false
}

const selectStore = (store) => {
  if (store.status !== 'open') return
  selectedStore.value = store
  userStore.selectStore(store)
  showStoreModal.value = false
  console.log('selectStore called, store.id:', store.id)
  fetchTables(store.id)
  showTableModal.value = true
}

const getTableStatusText = (status) => {
  const map = {
    'available': '可使用',
    'occupied': '使用中',
    'reserved': '已预订',
    'maintenance': '维护中'
  }
  return map[status] || status
}

const selectTable = (table) => {
  userStore.selectTable(table)
  showTableModal.value = false
  router.push({ path: '/ipad-menu', query: { v: Date.now() } })
}

const fetchTables = async (storeId) => {
  const defaultTables = [
    { id: 1, table_number: 'A01', table_type: '包厢', capacity: 10, floor: '一楼', status: 'available' },
    { id: 2, table_number: 'A02', table_type: '包厢', capacity: 10, floor: '一楼', status: 'available' },
    { id: 3, table_number: 'A03', table_type: '包厢', capacity: 8, floor: '一楼', status: 'reserved', reserved_name: '张先生', reserved_count: 6 },
    { id: 4, table_number: 'A04', table_type: '包厢', capacity: 8, floor: '一楼', status: 'available' },
    { id: 5, table_number: 'B01', table_type: '散台', capacity: 4, floor: '一楼', status: 'occupied' },
    { id: 6, table_number: 'B02', table_type: '散台', capacity: 4, floor: '一楼', status: 'available' },
    { id: 7, table_number: 'B03', table_type: '散台', capacity: 6, floor: '一楼', status: 'available' },
    { id: 8, table_number: 'B04', table_type: '散台', capacity: 6, floor: '一楼', status: 'maintenance' },
    { id: 9, table_number: 'C01', table_type: '大厅', capacity: 12, floor: '二楼', status: 'available' },
    { id: 10, table_number: 'C02', table_type: '大厅', capacity: 12, floor: '二楼', status: 'available' },
    { id: 11, table_number: 'D01', table_type: '宴席', capacity: 20, floor: '二楼', status: 'reserved', reserved_name: '李女士', reserved_count: 18 },
    { id: 12, table_number: 'D02', table_type: '宴席', capacity: 20, floor: '二楼', status: 'available' },
    { id: 13, table_number: 'VIP1', table_type: 'VIP', capacity: 15, floor: '三楼', status: 'reserved', reserved_name: '王总', reserved_count: 10 },
    { id: 14, table_number: 'VIP2', table_type: 'VIP', capacity: 15, floor: '三楼', status: 'available' }
  ]
  
  try {
    const res = await fetch(`/api/ipad/table/list?store_id=${storeId}`, {
      headers: {
        'X-Store-Id': storeId,
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200 && json.data && json.data.length > 0) {
      const mappedTables = json.data.map(t => ({
        id: t.table_id || t.id,
        table_number: t.table_name || t.table_number,
        table_type: t.table_type || '散台',
        capacity: t.seats || t.capacity || 4,
        floor: t.table_area || t.floor || '一楼',
        status: t.table_status || t.status || 'available',
        reserved_name: t.reserved_name || '',
        reserved_count: t.reserved_count || 0
      }))
      tables.value = mappedTables
      // 动态提取区域作为楼层选项
      const areas = [...new Set(mappedTables.map(t => t.floor))]
      floorOptions.value = areas.map((area, idx) => ({
        value: area,
        label: area || `区域${idx + 1}`
      }))
      if (floorOptions.value.length > 0) {
        selectedFloor.value = floorOptions.value[0].value
      }
    } else {
      tables.value = defaultTables
    }
  } catch (e) {
    tables.value = defaultTables
  }
}

onMounted(async () => {
  try {
    const res = await getStoreList()
    if (res.code === 200 && res.data?.length) {
      const apiStores = res.data
      const defaultStores = [
        { id: 1, name: '宁国店', nameEn: 'Ningguo', address: '宁国市青龙西路1号', status: 'open', tables: 13, capacity: 150 },
        { id: 2, name: '宣城店', nameEn: 'Xuancheng', address: '宣城市状元南路88号', status: 'open', tables: 15, capacity: 180 },
      ]
      const storeMap = new Map()
      defaultStores.forEach(s => storeMap.set(s.id, s))
      apiStores.forEach(s => storeMap.set(s.id, { ...storeMap.get(s.id), ...s }))
      stores.value = Array.from(storeMap.values())
    }
  } catch (e) {
    console.log('Using default store data')
  }
})
</script>

<style scoped>
.welcome-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #1A2F1F 0%, #2D4A3E 40%, #1F3A2E 100%);
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 30px 30px;
}

.main-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 800px;
  padding: 40px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-section {
  text-align: center;
  margin-bottom: 60px;
}

.logo-container {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 30px;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 12px;
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 12px;
  margin-bottom: 10px;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(201, 169, 98, 0.8);
  letter-spacing: 6px;
  margin-bottom: 20px;
}

.brand-divider {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #C9A962, transparent);
  margin: 0 auto 20px;
  border-radius: 1px;
}

.brand-slogan {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 4px;
}

.entry-section {
  width: 100%;
  margin-bottom: 60px;
}

.entry-grid {
  display: flex;
  justify-content: center;
  gap: 30px;
  flex-wrap: wrap;
}

.entry-card {
  width: 280px;
  padding: 36px 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.entry-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s;
}

.entry-card:hover {
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(201, 169, 98, 0.4);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.entry-card:hover::before {
  opacity: 1;
}

.entry-icon-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  transition: transform 0.4s;
}

.entry-card:hover .entry-icon-wrapper {
  transform: scale(1.1);
}

.entry-card.customer .entry-icon-wrapper {
  background: rgba(201, 169, 98, 0.15);
  color: #C9A962;
}

.entry-card.admin .entry-icon-wrapper {
  background: rgba(100, 180, 150, 0.15);
  color: #64B496;
}

.entry-icon-wrapper svg {
  width: 28px;
  height: 28px;
}

.entry-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.entry-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.entry-arrow {
  font-size: 18px;
  color: rgba(201, 169, 98, 0.8);
  font-weight: 300;
  transition: transform 0.4s;
}

.entry-card:hover .entry-arrow {
  transform: translateX(8px);
}

.beian-section {
  text-align: center;
}

.beian-content {
  margin-bottom: 12px;
}

.beian-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.5);
  transition: color 0.3s;
}

.beian-link:hover {
  color: rgba(201, 169, 98, 0.8);
}

.beian-icon {
  width: 18px;
  height: 18px;
}

.beian-icon svg {
  width: 100%;
  height: 100%;
}

.beian-text {
  font-size: 12px;
  letter-spacing: 1px;
}

.copyright {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 24px;
  width: 90%;
  max-width: 520px;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
  font-size: 20px;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  z-index: 10;
}

.modal-close:hover {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.5);
}

.modal-header {
  padding: 24px;
  text-align: center;
  border-bottom: none;
  background: linear-gradient(160deg, #1A2F1F 0%, #2D4A3E 100%);
  position: relative;
  border-radius: 24px 24px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 4px;
}

.modal-subtitle {
  display: block;
  font-size: 12px;
  color: rgba(201, 169, 98, 0.8);
  letter-spacing: 2px;
  margin-top: 4px;
}

.time-slot-badge {
  display: inline-block;
  padding: 6px 16px;
  margin-top: 10px;
  background: rgba(201, 169, 98, 0.2);
  border: 1px solid rgba(201, 169, 98, 0.5);
  border-radius: 20px;
  font-size: 13px;
  color: #C9A962;
  cursor: pointer;
  transition: all 0.3s;
}

.time-slot-badge:hover {
  background: rgba(201, 169, 98, 0.3);
  transform: scale(1.05);
}

.store-list {
  padding: 16px 24px 24px;
}

.store-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 12px;
  border: 2px solid transparent;
}

.store-item:last-child {
  margin-bottom: 0;
}

.store-item.active:hover {
  background: rgba(74, 124, 89, 0.06);
  border-color: rgba(74, 124, 89, 0.3);
}

.store-item.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.store-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(74, 124, 89, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4A7C59;
  flex-shrink: 0;
}

.store-icon svg {
  width: 24px;
  height: 24px;
}

.store-info {
  flex: 1;
}

.store-info .store-name {
  font-size: 17px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.store-info .store-addr {
  font-size: 13px;
  color: #999;
}

.store-status {
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 20px;
}

.store-status.open {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.store-status.closed {
  background: rgba(144, 147, 153, 0.1);
  color: #909399;
}

.table-modal {
  width: 680px;
  height: 520px;
}

.header-logo {
  text-align: center;
  margin-bottom: 12px;
}

.header-logo img {
  width: 50px;
  height: 50px;
  object-fit: contain;
}

.header-gold-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #C4A35A 30%, #F5D98C 50%, #C4A35A 70%, transparent 100%);
  overflow: hidden;
}

.header-gold-shimmer {
  position: absolute;
  top: 0;
  left: -120px;
  width: 120px;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(245,217,140,0.9), transparent);
  animation: shimmerMove 3s ease-in-out infinite;
}

@keyframes shimmerMove {
  0% { left: -120px; }
  100% { left: 100%; }
}

.table-filters {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  gap: 16px;
  border-bottom: 1px solid #f5f5f5;
  background: #fafafa;
}

.filter-group {
  display: flex;
  gap: 8px;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  color: #666;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}

.filter-btn.active {
  background: #2D4A3E;
  border-color: #2D4A3E;
  color: white;
}

.filter-divider {
  width: 1px;
  height: 24px;
  background: #e0e0e0;
}

.filter-divider.gold {
  background: linear-gradient(180deg, transparent, #C4A35A, transparent);
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 20px 24px;
  max-height: 320px;
  overflow-y: auto;
}

.table-item {
  border-radius: 12px;
  padding: 20px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  border: 2px solid transparent;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.table-item:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.table-item.包厢 {
  background: #FEF3C7;
  border-color: #FCD34D;
}

.table-item.包厢 .table-number {
  color: #B45309;
}

.table-item.包厢 .table-type {
  color: #D97706;
}

.table-item.散台 {
  background: #ECFDF5;
  border-color: #A7F3D0;
}

.table-item.散台 .table-number {
  color: #047857;
}

.table-item.散台 .table-type {
  color: #059669;
}

.table-item.大厅 {
  background: #EFF6FF;
  border-color: #BFDBFE;
}

.table-item.大厅 .table-number {
  color: #1D4ED8;
}

.table-item.大厅 .table-type {
  color: #2563EB;
}

.table-item.宴席 {
  background: #F3E8FF;
  border-color: #D8B4FE;
}

.table-item.宴席 .table-number {
  color: #7C3AED;
}

.table-item.宴席 .table-type {
  color: #8B5CF6;
}

.table-item.VIP {
  background: #F5F3FF;
  border-color: #C4B5FD;
}

.table-item.VIP .table-number {
  color: #6D28D9;
}

.table-item.VIP .table-type {
  color: #7C3AED;
}

.table-number {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.table-type {
  font-size: 13px;
  color: #999;
}

.table-item.reserved {
  background: #FEF2F2 !important;
  border-color: #DC2626 !important;
}

.table-item.reserved .table-number {
  color: #991B1B !important;
}

.table-item.reserved .table-type {
  color: #DC2626 !important;
}

.table-item.reserved:hover {
  cursor: not-allowed;
  transform: none !important;
}

.table-reserved-info {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba(220, 38, 38, 0.3);
}

.reserved-name {
  display: block;
  font-size: 11px;
  color: #991B1B;
  font-weight: 500;
}

.reserved-count {
  display: block;
  font-size: 10px;
  color: #DC2626;
  margin-top: 2px;
}



@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .main-container {
    padding: 30px 20px;
  }

  .brand-title {
    font-size: 36px;
    letter-spacing: 8px;
  }

  .entry-grid {
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  .entry-card {
    width: 100%;
    max-width: 320px;
  }

  .modal-content {
    width: 95%;
    border-radius: 16px;
  }
}
</style>