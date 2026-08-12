<template>
  <div class="table-layout-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">台型设计 · Table Layout</h2>
        <p class="page-desc">桌台布局 · 区域规划 · 排位设计 · 容量管理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">桌台总数</div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-sub">全部桌台</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">大厅桌数</div>
        <div class="stat-value">{{ stats.hall }}</div>
        <div class="stat-sub">大厅区域</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">包厢数</div>
        <div class="stat-value">{{ stats.privateRoom }}</div>
        <div class="stat-sub">独立包厢</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">最大容量</div>
        <div class="stat-value">{{ stats.maxCapacity }}</div>
        <div class="stat-sub">同时可容纳人数</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="search"
            placeholder="搜索桌号 / 备注"
            clearable
            style="width: 220px"
          />
          <el-select v-model="filterArea" placeholder="区域" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="大厅" value="大厅" />
            <el-option label="包厢" value="包厢" />
            <el-option label="卡座区" value="卡座区" />
          </el-select>
          <el-select v-model="filterType" placeholder="类型" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="大圆桌" value="大圆桌" />
            <el-option label="方桌" value="方桌" />
            <el-option label="包厢" value="包厢" />
            <el-option label="卡座" value="卡座" />
          </el-select>
          <el-button @click="onReset">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button-group>
            <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
            <el-button :type="viewMode === 'grid' ? 'primary' : ''" @click="viewMode = 'grid'">网格视图</el-button>
          </el-button-group>
          <el-button type="primary" @click="openAdd">+ 新增桌台</el-button>
        </div>
      </div>

      <!-- 列表视图 -->
      <el-table v-if="viewMode === 'list'" :data="filteredList" stripe v-loading="loading">
        <el-table-column prop="tableNo" label="桌号" width="110" />
        <el-table-column prop="area" label="区域" width="120">
          <template #default="{ row }">
            <el-tag :type="areaTagType(row.area)" size="small" effect="plain">{{ row.area || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="seats" label="座位数" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ row.status || '空闲' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="removeTable(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格视图 -->
      <div v-else class="grid-view">
        <div
          v-for="row in filteredList"
          :key="row.tableNo"
          class="grid-card"
          :class="{ 'grid-card--busy': row.status === '占用', 'grid-card--reserved': row.status === '预留' }"
        >
          <div class="grid-card-top">
            <span class="grid-table-no">{{ row.tableNo || '-' }}</span>
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ row.status || '空闲' }}
            </el-tag>
          </div>
          <div class="grid-card-info">
            <div class="grid-info-row">
              <span class="grid-info-label">区域</span>
              <span class="grid-info-value">{{ row.area || '-' }}</span>
            </div>
            <div class="grid-info-row">
              <span class="grid-info-label">类型</span>
              <span class="grid-info-value">{{ row.type || '-' }}</span>
            </div>
            <div class="grid-info-row">
              <span class="grid-info-label">座位</span>
              <span class="grid-info-value">{{ row.seats || 0 }} 位</span>
            </div>
          </div>
          <div class="grid-card-actions">
            <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="removeTable(row)">删除</el-button>
          </div>
        </div>
        <div v-if="!filteredList.length && !loading" class="grid-empty">暂无桌台数据</div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showFormDialog" :title="editing ? '编辑桌台' : '新增桌台'" width="520px">
      <el-form :model="tableForm" label-width="80px">
        <el-form-item label="桌号" required><el-input v-model="tableForm.tableNo" /></el-form-item>
        <el-form-item label="区域" required>
          <el-select v-model="tableForm.area" style="width: 100%">
            <el-option label="大厅" value="大厅" />
            <el-option label="包厢" value="包厢" />
            <el-option label="卡座区" value="卡座区" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="tableForm.type" style="width: 100%">
            <el-option label="大圆桌" value="大圆桌" />
            <el-option label="方桌" value="方桌" />
            <el-option label="包厢" value="包厢" />
            <el-option label="卡座" value="卡座" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位数" required>
          <el-input v-model.number="tableForm.seats" type="number" min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="tableForm.status" style="width: 100%">
            <el-option label="空闲" value="空闲" />
            <el-option label="占用" value="占用" />
            <el-option label="预留" value="预留" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="tableForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTable">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const search = ref('')
const filterArea = ref('')
const filterType = ref('')
const viewMode = ref('list')

const tables = ref([])

const stats = ref({ total: 0, hall: 0, privateRoom: 0, maxCapacity: 0 })

const showFormDialog = ref(false)
const editing = ref(false)

const tableForm = ref({
  tableNo: '',
  area: '大厅',
  type: '方桌',
  seats: 4,
  status: '空闲',
  remark: ''
})

const filteredList = computed(() => {
  let list = tables.value
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(t =>
      (t.tableNo || '').toLowerCase().includes(q) ||
      (t.remark || '').toLowerCase().includes(q)
    )
  }
  if (filterArea.value) list = list.filter(t => t.area === filterArea.value)
  if (filterType.value) list = list.filter(t => t.type === filterType.value)
  return list
})

function areaTagType(area) {
  return { '大厅': 'info', '包厢': 'warning', '卡座区': 'success' }[area] || 'info'
}

function typeTagType(type) {
  return { '大圆桌': 'warning', '方桌': 'info', '包厢': 'danger', '卡座': 'success' }[type] || 'info'
}

function statusTagType(status) {
  return { '空闲': 'success', '占用': 'danger', '预留': 'warning', '停用': 'info' }[status] || 'success'
}

function onReset() {
  search.value = ''
  filterArea.value = ''
  filterType.value = ''
}

function openAdd() {
  editing.value = false
  tableForm.value = { tableNo: '', area: '大厅', type: '方桌', seats: 4, status: '空闲', remark: '' }
  showFormDialog.value = true
}

function openEdit(row) {
  editing.value = true
  tableForm.value = { ...row }
  showFormDialog.value = true
}

function saveTable() {
  if (!tableForm.value.tableNo) {
    ElMessage.warning('请填写桌号')
    return
  }
  if (!tableForm.value.seats || tableForm.value.seats < 1) {
    ElMessage.warning('请填写有效座位数')
    return
  }
  ElMessage.success('保存成功')
  showFormDialog.value = false
}

function removeTable(row) {
  ElMessage.success(`已删除桌台 ${row.tableNo || ''}`)
}
</script>

<style scoped>
.table-layout-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2D4A3E 0%, #C4A35A 100%);
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #95A5A6;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 6px;
}

.content-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.grid-card {
  background: #FAF8F5;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 16px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.grid-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
  border-color: #C4A35A;
}

.grid-card--busy {
  background: #FFF5F5;
  border-color: #F5C6CB;
}

.grid-card--reserved {
  background: #FFFBF0;
  border-color: #FFE5A8;
}

.grid-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.grid-table-no {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
  letter-spacing: 0.5px;
}

.grid-card-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px dashed #E8E4DE;
}

.grid-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.grid-info-label {
  font-size: 12px;
  color: #95A5A6;
}

.grid-info-value {
  font-size: 13px;
  font-weight: 600;
  color: #1a2f23;
}

.grid-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.grid-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 48px 0;
  color: #95A5A6;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
