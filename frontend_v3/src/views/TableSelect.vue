<template>
  <div class="table-select">
    <div class="bg-layer">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
    </div>

    <div class="main-container">
      <div class="header-section">
        <button class="btn-back" @click="goBack">← 返回</button>
        <div class="header-info">
          <h2 class="store-name">{{ currentStore?.name || '选择门店' }}</h2>
          <p class="store-addr">{{ currentStore?.address || '' }}</p>
        </div>
      </div>

      <div class="section-title">
        <span>选择桌台</span>
        <span class="subtitle">Select Table</span>
      </div>

      <div class="table-grid">
        <div
          v-for="table in tables"
          :key="table.id"
          :class="['table-card', table.status]"
          @click="selectTable(table)"
        >
          <div class="table-header">
            <span class="table-number">{{ table.table_number }}</span>
            <span :class="['status-badge', table.status]">{{ getStatusText(table.status) }}</span>
          </div>
          <div class="table-info">
            <span class="table-type">{{ table.table_type || '散台' }}</span>
            <span class="table-capacity">{{ table.capacity }}人</span>
          </div>
          <div class="table-floor" v-if="table.floor">
            {{ table.floor }}楼
          </div>
        </div>
      </div>

      <div class="status-legend">
        <div class="legend-item">
          <span class="legend-dot available"></span>
          <span>可使用</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot occupied"></span>
          <span>使用中</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot reserved"></span>
          <span>已预订</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot maintenance"></span>
          <span>维护中</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentStore = ref({})
const tables = ref([])

const getStatusText = (status) => {
  const map = {
    'available': '可使用',
    'occupied': '使用中',
    'reserved': '已预订',
    'maintenance': '维护中'
  }
  return map[status] || status
}

const goBack = () => {
  router.back()
}

const selectTable = (table) => {
  if (table.status !== 'available' && table.status !== 'reserved') {
    return
  }
  userStore.selectTable(table)
  router.push('/ipad-menu')
}

const fetchTables = async () => {
  try {
    const storeId = currentStore.value.id || route.query.storeId || 1
    const res = await fetch(`/api/ipad/table/list?store_id=${storeId}`, {
      headers: {
        'X-Store-Id': storeId,
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200) {
      tables.value = json.data
    }
  } catch (e) {
    console.error('获取桌台失败:', e)
    tables.value = [
      { id: 1, table_number: 'A01', table_type: '包厢', capacity: 10, floor: '1', status: 'available' },
      { id: 2, table_number: 'A02', table_type: '包厢', capacity: 10, floor: '1', status: 'available' },
      { id: 3, table_number: 'A03', table_type: '包厢', capacity: 8, floor: '1', status: 'reserved' },
      { id: 4, table_number: 'A04', table_type: '包厢', capacity: 8, floor: '1', status: 'available' },
      { id: 5, table_number: 'B01', table_type: '散台', capacity: 4, floor: '1', status: 'occupied' },
      { id: 6, table_number: 'B02', table_type: '散台', capacity: 4, floor: '1', status: 'available' },
      { id: 7, table_number: 'B03', table_type: '散台', capacity: 6, floor: '1', status: 'available' },
      { id: 8, table_number: 'B04', table_type: '散台', capacity: 6, floor: '1', status: 'maintenance' },
      { id: 9, table_number: 'C01', table_type: '大厅', capacity: 12, floor: '2', status: 'available' },
      { id: 10, table_number: 'C02', table_type: '大厅', capacity: 12, floor: '2', status: 'available' },
      { id: 11, table_number: 'D01', table_type: '宴席', capacity: 20, floor: '2', status: 'reserved' },
      { id: 12, table_number: 'D02', table_type: '宴席', capacity: 20, floor: '2', status: 'available' }
    ]
  }
}

onMounted(() => {
  currentStore.value = userStore.currentStore || {}
  fetchTables()
})
</script>

<style scoped>
.table-select {
  min-height: 100vh;
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
  padding: 30px 24px;
  max-width: 900px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.btn-back {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateX(-4px);
}

.header-info {
  flex: 1;
}

.store-name {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 4px;
}

.store-addr {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.section-title > span:first-child {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 3px;
}

.subtitle {
  font-size: 12px;
  color: rgba(201, 169, 98, 0.7);
  letter-spacing: 2px;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}

.table-card {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
}

.table-card:hover {
  transform: translateY(-4px);
  border-color: rgba(201, 169, 98, 0.3);
}

.table-card.available {
  border-color: rgba(100, 180, 150, 0.3);
}

.table-card.available:hover {
  background: rgba(100, 180, 150, 0.15);
}

.table-card.occupied {
  opacity: 0.4;
  cursor: not-allowed;
}

.table-card.reserved {
  border-color: rgba(201, 169, 98, 0.3);
}

.table-card.maintenance {
  opacity: 0.3;
  cursor: not-allowed;
  filter: grayscale(0.5);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-number {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.status-badge {
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.status-badge.available {
  background: rgba(100, 180, 150, 0.2);
  color: #64B496;
}

.status-badge.occupied {
  background: rgba(245, 108, 108, 0.2);
  color: #F56C6C;
}

.status-badge.reserved {
  background: rgba(201, 169, 98, 0.2);
  color: #C9A962;
}

.status-badge.maintenance {
  background: rgba(144, 147, 153, 0.2);
  color: #909399;
}

.table-info {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.table-type,
.table-capacity {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.table-floor {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.status-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.available {
  background: #64B496;
}

.legend-dot.occupied {
  background: #F56C6C;
}

.legend-dot.reserved {
  background: #C9A962;
}

.legend-dot.maintenance {
  background: #909399;
}

@media (max-width: 768px) {
  .main-container {
    padding: 20px 16px;
  }

  .table-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }

  .table-card {
    padding: 16px 12px;
  }

  .table-number {
    font-size: 20px;
  }

  .status-legend {
    gap: 16px;
  }
}
</style>