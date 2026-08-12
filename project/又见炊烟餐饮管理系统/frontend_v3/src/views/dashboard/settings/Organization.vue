<template>
  <div class="organization-page">
    <div class="page-header">
      <h2 class="page-title">门店与组织</h2>
      <p class="page-subtitle">Store & Organization Management</p>
    </div>

    <!-- 门店列表 -->
    <div class="section-block">
      <h3 class="section-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="7" width="20" height="15" rx="2"/>
          <polyline points="17 2 12 7 7 2"/>
        </svg>
        门店列表
      </h3>
      <div class="store-cards">
        <div v-for="store in stores" :key="store.id" class="store-card" :class="{ active: store.id === currentStore }">
          <div class="store-header">
            <div class="store-logo">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>
            <div class="store-info">
              <h4 class="store-name">{{ store.name }}</h4>
              <p class="store-address">{{ store.address }}</p>
            </div>
            <div class="store-badge" :class="store.status">{{ store.statusText }}</div>
          </div>
          <div class="store-stats">
            <div class="stat-item">
              <div class="stat-value">{{ store.employees }}</div>
              <div class="stat-label">员工数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ store.tables }}</div>
              <div class="stat-label">桌台数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ store.departments }}</div>
              <div class="stat-label">部门数</div>
            </div>
          </div>
          <div class="store-footer">
            <span class="store-phone">{{ store.phone }}</span>
            <el-button size="small" @click="editStore(store)">编辑</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 部门管理 -->
    <div class="section-block">
      <h3 class="section-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        部门管理
        <el-button size="small" type="primary" style="margin-left:auto" @click="addDepartment">添加部门</el-button>
      </h3>
      <el-table :data="departments" border stripe style="width: 100%">
        <el-table-column prop="name" label="部门名称" width="180" />
        <el-table-column prop="manager" label="负责人" width="120" />
        <el-table-column prop="count" label="人数" width="80" />
        <el-table-column prop="store" label="所属门店" width="120" />
        <el-table-column prop="description" label="职能描述">
          <template #default="{ row }">
            <span style="color: var(--color-text-secondary); font-size: 13px">{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editDepartment(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteDepartment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 岗位管理 -->
    <div class="section-block">
      <h3 class="section-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 7h-9"/>
          <path d="M14 17H5"/>
          <circle cx="17" cy="17" r="3"/>
          <circle cx="7" cy="7" r="3"/>
        </svg>
        岗位管理
        <el-button size="small" type="primary" style="margin-left:auto" @click="addPosition">添加岗位</el-button>
      </h3>
      <div class="position-grid">
        <div v-for="pos in positions" :key="pos.name" class="position-card">
          <div class="position-header">
            <div class="position-icon" :style="{ background: pos.bgColor }">
              <span>{{ pos.emoji }}</span>
            </div>
            <div class="position-info">
              <h4>{{ pos.name }}</h4>
              <p>{{ pos.department }}</p>
            </div>
          </div>
          <div class="position-meta">
            <span>{{ pos.count }} 人在岗</span>
            <span class="position-level">{{ pos.level }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentStore = ref(1)

const stores = ref([
  {
    id: 1,
    name: '宁国店',
    address: '安徽省宣城市宁国市宁国大道88号',
    phone: '0563-4001234',
    status: 'active',
    statusText: '营业中',
    employees: 45,
    tables: 28,
    departments: 6
  },
  {
    id: 2,
    name: '宣城店',
    address: '安徽省宣城市宣州区鳌峰路128号',
    phone: '0563-3005678',
    status: 'active',
    statusText: '营业中',
    employees: 38,
    tables: 22,
    departments: 5
  }
])

const departments = ref([
  { id: 1, name: '前厅部', manager: '王五', count: 15, store: '宁国店', description: '负责前厅接待、预订管理、客户服务' },
  { id: 2, name: '后厨部', manager: '赵六', count: 12, store: '宁国店', description: '负责菜品制作、厨房管理、食品安全' },
  { id: 3, name: '财务部', manager: '钱七', count: 3, store: '宁国店', description: '负责财务核算、报表编制、资金管理' },
  { id: 4, name: '人事部', manager: '孙八', count: 2, store: '宁国店', description: '负责招聘培训、考勤管理、薪资核算' },
  { id: 5, name: '采购部', manager: '周九', count: 3, store: '宁国店', description: '负责食材采购、供应商管理、库存控制' },
  { id: 6, name: '营销部', manager: '吴十', count: 2, store: '宁国店', description: '负责营销策划、会员管理、品牌推广' },
  { id: 7, name: '前厅部', manager: '陈一', count: 12, store: '宣城店', description: '负责前厅接待、预订管理、客户服务' },
  { id: 8, name: '后厨部', manager: '刘二', count: 10, store: '宣城店', description: '负责菜品制作、厨房管理、食品安全' },
])

const positions = ref([
  { name: '店长', department: '管理层', count: 2, level: '高级', emoji: '👔', bgColor: 'rgba(45,74,62,0.08)' },
  { name: '前厅主管', department: '前厅部', count: 3, level: '中级', emoji: '🎩', bgColor: 'rgba(74,124,89,0.08)' },
  { name: '后厨主管', department: '后厨部', count: 2, level: '中级', emoji: '👨‍🍳', bgColor: 'rgba(196,163,90,0.08)' },
  { name: '收银员', department: '前厅部', count: 4, level: '基础', emoji: '💰', bgColor: 'rgba(91,123,138,0.08)' },
  { name: '服务员', department: '前厅部', count: 12, level: '基础', emoji: '🍽️', bgColor: 'rgba(139,154,140,0.08)' },
  { name: '厨师', department: '后厨部', count: 8, level: '中级', emoji: '🔥', bgColor: 'rgba(220,120,60,0.08)' },
  { name: '传菜员', department: '后厨部', count: 6, level: '基础', emoji: '🏃', bgColor: 'rgba(100,140,180,0.08)' },
  { name: '采购员', department: '采购部', count: 2, level: '基础', emoji: '🛒', bgColor: 'rgba(160,120,180,0.08)' },
  { name: '会计', department: '财务部', count: 2, level: '中级', emoji: '📊', bgColor: 'rgba(80,160,120,0.08)' },
])

function editStore(store) {
  ElMessage.info(`编辑门店: ${store.name}`)
}

function addDepartment() {
  ElMessage.info('添加部门功能待实现')
}

function editDepartment(row) {
  ElMessage.info(`编辑部门: ${row.name}`)
}

function deleteDepartment(row) {
  ElMessageBox.confirm(`确定删除部门 ${row.name} 吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const idx = departments.value.findIndex(d => d.id === row.id)
    if (idx > -1) departments.value.splice(idx, 1)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function addPosition() {
  ElMessage.info('添加岗位功能待实现')
}
</script>

<style scoped>
.organization-page {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.section-block {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.store-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.store-card {
  background: var(--color-bg-alt);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all 0.2s;
}

.store-card.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(45, 74, 62, 0.1);
}

.store-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.store-logo {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(45, 74, 62, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}

.store-logo svg {
  width: 22px;
  height: 22px;
}

.store-info {
  flex: 1;
}

.store-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 2px 0;
}

.store-address {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.store-badge {
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.store-badge.active {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.store-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 2px;
}

.stat-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.store-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.store-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-family: monospace;
}

.position-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.position-card {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  transition: all 0.2s;
}

.position-card:hover {
  box-shadow: var(--shadow-sm);
}

.position-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.position-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.position-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 2px 0;
}

.position-info p {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
}

.position-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.position-level {
  padding: 2px 8px;
  background: rgba(45, 74, 62, 0.06);
  border-radius: 4px;
  font-size: 11px;
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .store-cards {
    grid-template-columns: 1fr;
  }
  
  .position-grid {
    grid-template-columns: 1fr;
  }
}
</style>
