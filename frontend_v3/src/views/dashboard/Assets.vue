<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">行政资产 · Assets Management</h1>
        <span class="page-desc">资产登记 · 状态跟踪 · 报废管理 · Assets · Status · Disposal</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openAdd"><span>新增资产 · Add Asset</span></el-button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :class="s.cls">
        <div class="stat-num">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="filter-bar">
      <el-input v-model="search" placeholder="搜索资产编号/名称..." class="search-input" clearable />
      <el-select v-model="filterCategory" placeholder="分类筛选" clearable style="width:160px">
        <el-option label="家具 · Furniture" value="家具" />
        <el-option label="电器 · Appliance" value="电器" />
        <el-option label="厨具 · Kitchenware" value="厨具" />
        <el-option label="餐具 · Tableware" value="餐具" />
        <el-option label="办公设备 · Office" value="办公设备" />
        <el-option label="其他 · Other" value="其他" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px">
        <el-option label="在用 · In Use" value="在用" />
        <el-option label="维修中 · Repairing" value="维修中" />
        <el-option label="报废 · Scrapped" value="报废" />
        <el-option label="库存 · In Stock" value="库存" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      :default-sort="{ prop: 'assetId', order: 'ascending' }"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="assetId" label="资产编号 · ID" width="100" sortable />
      <el-table-column prop="assetName" label="资产名称 · Name" width="140" />
      <el-table-column prop="category" label="分类 · Category" width="110" />
      <el-table-column prop="quantity" label="数量 · Qty" width="80" align="center" />
      <el-table-column prop="unitPrice" label="单价 · Unit Price" width="110" align="right">
        <template #default="{ row }">
          ¥{{ row.unitPrice.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="总值 · Total" width="120" align="right">
        <template #default="{ row }">
          ¥{{ (row.quantity * row.unitPrice).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="department" label="使用部门 · Dept" width="120" />
      <el-table-column prop="status" label="状态 · Status" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="购入日期 · Date" width="120" />
      <el-table-column prop="remark" label="备注 · Remark" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 项 · 右键编辑</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-item" @click="ctxScrap">报废 · Scrap</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑资产 · Edit Asset' : '新增资产 · Add Asset'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="必填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width:100%">
                <el-option label="家具 · Furniture" value="家具" />
                <el-option label="电器 · Appliance" value="电器" />
                <el-option label="厨具 · Kitchenware" value="厨具" />
                <el-option label="餐具 · Tableware" value="餐具" />
                <el-option label="办公设备 · Office" value="办公设备" />
                <el-option label="其他 · Other" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数量">
              <el-input-number v-model="form.quantity" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单价">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用部门">
              <el-select v-model="form.department" style="width:100%">
                <el-option label="前厅 · Front" value="前厅" />
                <el-option label="厨房 · Kitchen" value="厨房" />
                <el-option label="办公室 · Office" value="办公室" />
                <el-option label="仓库 · Warehouse" value="仓库" />
                <el-option label="包厢 · Private" value="包厢" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="在用 · In Use" value="在用" />
                <el-option label="维修中 · Repairing" value="维修中" />
                <el-option label="报废 · Scrapped" value="报废" />
                <el-option label="库存 · In Stock" value="库存" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购入日期">
              <el-date-picker
                v-model="form.purchaseDate"
                type="date"
                placeholder="选择日期"
                style="width:100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选备注信息..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveAsset" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editing = ref(false)
const search = ref('')
const filterCategory = ref('')
const filterStatus = ref('')
const formRef = ref(null)

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0, row: null })
let ctxRow = null

// 资产数据
const list = ref([])

// 加载真实数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/assets')
    list.value = res.data || []
  } catch (e) {
    console.error('获取资产列表失败', e)
    ElMessage.error('获取资产列表失败')
  } finally {
    loading.value = false
  }
}

function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 180),
    y: Math.min(event.clientY, window.innerHeight - 120),
  }
}

function closeMenu() {
  ctxMenu.value.visible = false
  ctxRow = null
}

function ctxEdit() {
  closeMenu()
  editRow(ctxRow)
}

function ctxScrap() {
  closeMenu()
  ElMessageBox.confirm(`确定将「${ctxRow.assetName}」标记为报废？`, '报废确认 · Scrap Confirm', {
    confirmButtonText: '确定报废 · Scrap',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.put(`/hr/assets/${ctxRow.assetId}`, { ...ctxRow, status: '报废' })
        ElMessage.success('已标记为报废 · Scrapped')
        await fetchData()
      } catch (e) {
        console.error('报废操作失败', e)
        ElMessage.error('操作失败')
      }
    })
    .catch(() => {})
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.assetName}」？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.delete(`/hr/assets/${ctxRow.assetId}`)
        ElMessage.success('已删除 · Deleted')
        await fetchData()
      } catch (e) {
        console.error('删除失败', e)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// ---------- form ----------
const form = ref({
  assetName: '',
  category: '家具',
  quantity: 1,
  unitPrice: 0,
  department: '前厅',
  status: '在用',
  purchaseDate: '',
  remark: '',
})

const rules = {
  assetName: [{ required: true, message: '资产名称必填' }],
}

const stats = computed(() => {
  const total = list.value.length
  const inUse = list.value.filter(a => a.status === '在用').length
  const repairing = list.value.filter(a => a.status === '维修中').length
  const scrapped = list.value.filter(a => a.status === '报废').length
  return [
    { label: '资产总数 · Total', value: total, cls: 'st-total' },
    { label: '在用 · In Use', value: inUse, cls: 'st-active' },
    { label: '维修中 · Repairing', value: repairing, cls: 'st-mgr' },
    { label: '报废 · Scrapped', value: scrapped, cls: 'st-admin' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(a => (a.assetId || '').includes(q) || (a.assetName || '').includes(q))
  }
  if (filterCategory.value) l = l.filter(a => a.category === filterCategory.value)
  if (filterStatus.value) l = l.filter(a => a.status === filterStatus.value)
  return l
})

const statusTag = s => ({ '在用': 'success', '维修中': 'warning', '报废': 'danger', '库存': 'info' }[s] || 'info')
const statusLabel = s => ({ '在用': '在用', '维修中': '维修中', '报废': '报废', '库存': '库存' }[s] || s)

function openAdd() {
  editing.value = false
  form.value = { assetName: '', category: '家具', quantity: 1, unitPrice: 0, department: '前厅', status: '在用', purchaseDate: '', remark: '' }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveAsset() {
  await formRef.value?.validate().catch(() => false)

  saving.value = true
  try {
    if (editing.value) {
      await request.put(`/hr/assets/${form.value.assetId}`, form.value)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/assets', form.value)
      ElMessage.success('已创建 · Created')
    }
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function clearFilters() {
  search.value = ''
  filterCategory.value = ''
  filterStatus.value = ''
}

onMounted(() => {
  fetchData()
  document.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
  closeMenu()
})
</script>

<style scoped>
.hr-page { max-width: 1600px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; padding: 14px 18px; border-radius: 2px; background: var(--color-card); text-align: center; border: 1px solid var(--color-border); }
.st-total { border-color: var(--color-border); }
.st-active { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.st-mgr { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-admin { background: rgba(194,85,85,0.04); border-color: rgba(194,85,85,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.st-admin .stat-num { color: #C25555; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }

.data-table { border-radius: 2px; overflow: hidden; }

.table-footer { margin-top: 10px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }

/* 右键菜单 */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  padding: 6px;
  min-width: 150px;
}
.ctx-item {
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 2px;
  color: var(--color-text-primary);
  transition: background 0.1s;
}
.ctx-item:hover {
  background: rgba(45,74,62,0.04);
}
.ctx-item.danger {
  color: #C25555;
}
.ctx-item.danger:hover {
  background: rgba(194,85,85,0.04);
}
.ctx-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}
</style>
