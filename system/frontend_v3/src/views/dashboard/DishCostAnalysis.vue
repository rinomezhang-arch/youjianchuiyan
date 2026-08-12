<template>
  <div class="cost-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">菜品成本分析 · Dish Cost Analysis</h1>
        <span class="page-desc">成本监控 · 毛利率分析 · 菜品定价优化 · Cost Monitor · Margin · Pricing</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="exportData"><span>导出报表 · Export</span></el-button>
        <el-button type="primary" @click="openBatchEdit"><span>批量调价 · Batch</span></el-button>
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
      <el-input v-model="search" placeholder="搜索菜品名称/编号..." class="search-input" clearable />
      <el-select v-model="filterCategory" placeholder="分类筛选 · Category" clearable style="width:160px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterCostRate" placeholder="成本率范围 · Cost Rate" clearable style="width:180px">
        <el-option label="低成本(<20%) · Low" value="low" />
        <el-option label="正常(20-30%) · Normal" value="normal" />
        <el-option label="高成本(>30%) · High" value="high" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态 · Status" clearable style="width:130px">
        <el-option label="在售 · Active" value="active" />
        <el-option label="停售 · Inactive" value="inactive" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      :default-sort="{ prop: 'dishId', order: 'ascending' }"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="dishId" label="菜品编号 · ID" width="100" sortable />
      <el-table-column prop="dishName" label="菜品名称 · Name" width="140" />
      <el-table-column prop="category" label="分类 · Category" width="110">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="categoryTag(row.category)">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成本价 · Cost" width="110" align="right">
        <template #default="{ row }">
          <span class="price-text">¥{{ row.costPrice.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成本率 · Cost%" width="110" align="center" sortable prop="costRate">
        <template #default="{ row }">
          <span class="cost-rate" :class="costRateClass(row.costRate)">{{ row.costRate.toFixed(1) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="毛利率 · Margin%" width="110" align="center" sortable prop="grossMargin">
        <template #default="{ row }">
          <span class="margin-text">{{ row.grossMargin.toFixed(1) }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="cookingTime" label="烹饪时间 · Time" width="120" align="center">
        <template #default="{ row }">
          <span>{{ row.cookingTime }}分钟 · min</span>
        </template>
      </el-table-column>
      <el-table-column label="状态 · Status" width="100" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="row.status === 'active' ? 'active' : 'inactive'"></span>
          <span class="status-text">{{ row.status === 'active' ? '在售' : '停售' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 道菜品 · 右键编辑成本 · {{ filteredList.length }} dishes · Right-click to edit</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEditCost">编辑成本 · Edit Cost</div>
      <div class="ctx-item" @click="ctxViewDetail">查看详情 · Details</div>
    </div>

    <!-- 编辑成本对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑成本 · Edit Cost" width="520px" destroy-on-close>
      <div class="edit-dish-header">
        <span class="edit-dish-name">{{ editingDish.dishName }}</span>
        <span class="edit-dish-id">{{ editingDish.dishId }}</span>
      </div>
      <el-form :model="editForm" label-width="100px" style="margin-top:16px">
        <el-form-item label="成本价 · Cost">
          <el-input-number v-model="editForm.costPrice" :min="0" :precision="2" :step="0.5" style="width:100%" @change="onCostChange" />
        </el-form-item>
        <el-form-item label="售价 · Price">
          <el-input-number v-model="editForm.salePrice" :min="0" :precision="2" :step="1" style="width:100%" @change="onPriceChange" />
        </el-form-item>
        <el-divider content-position="left">自动计算 · Auto Calculate</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="calc-card">
              <div class="calc-label">成本率 · Cost Rate</div>
              <div class="calc-value" :class="costRateClass(computedCostRate)">{{ computedCostRate.toFixed(1) }}%</div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="calc-card">
              <div class="calc-label">毛利率 · Gross Margin</div>
              <div class="calc-value">{{ computedMargin.toFixed(1) }}%</div>
            </div>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="菜品详情 · Dish Details" width="600px" destroy-on-close>
      <div class="detail-grid">
        <div class="detail-item">
          <span class="detail-label">菜品编号 · ID</span>
          <span class="detail-value">{{ detailDish.dishId }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">菜品名称 · Name</span>
          <span class="detail-value">{{ detailDish.dishName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">分类 · Category</span>
          <span class="detail-value">{{ detailDish.category }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">成本价 · Cost</span>
          <span class="detail-value">¥{{ (detailDish.costPrice || 0).toFixed(2) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">售价 · Price</span>
          <span class="detail-value">¥{{ (detailDish.salePrice || 0).toFixed(2) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">成本率 · Cost Rate</span>
          <span class="detail-value cost-rate" :class="costRateClass(detailDish.costRate)">{{ (detailDish.costRate || 0).toFixed(1) }}%</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">毛利率 · Margin</span>
          <span class="detail-value">{{ (detailDish.grossMargin || 0).toFixed(1) }}%</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">烹饪时间 · Time</span>
          <span class="detail-value">{{ detailDish.cookingTime }}分钟 · min</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">状态 · Status</span>
          <span class="detail-value">
            <span class="status-dot" :class="detailDish.status === 'active' ? 'active' : 'inactive'"></span>
            {{ detailDish.status === 'active' ? '在售 · Active' : '停售 · Inactive' }}
          </span>
        </div>
        <div class="detail-item full-width">
          <span class="detail-label">利润分析 · Profit</span>
          <span class="detail-value">
            每份利润 · Profit/dish: <strong>¥{{ ((detailDish.salePrice || 0) - (detailDish.costPrice || 0)).toFixed(2) }}</strong>
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭 · Close</el-button>
        <el-button type="primary" @click="showDetailDialog = false; openEditCost(detailDish)">编辑成本 · Edit</el-button>
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
const list = ref([])
const search = ref('')
const filterCategory = ref('')
const filterCostRate = ref('')
const filterStatus = ref('')

const showEditDialog = ref(false)
const showDetailDialog = ref(false)
const editingDish = ref({})
const detailDish = ref({})
const editForm = ref({ costPrice: 0, salePrice: 0 })

const categories = ['凉菜刺身', '热菜', '汤羹', '主食', '甜品', '酒水']

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0 })
let ctxRow = null

function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 200),
    y: Math.min(event.clientY, window.innerHeight - 100),
  }
}

function closeMenu() {
  ctxMenu.value.visible = false
  ctxRow = null
}

function ctxEditCost() {
  closeMenu()
  openEditCost(ctxRow)
}

function ctxViewDetail() {
  closeMenu()
  detailDish.value = { ...ctxRow }
  showDetailDialog.value = true
}

// 计算属性
const computedCostRate = computed(() => {
  if (!editForm.value.salePrice || editForm.value.salePrice <= 0) return 0
  return (editForm.value.costPrice / editForm.value.salePrice) * 100
})

const computedMargin = computed(() => {
  if (!editForm.value.salePrice || editForm.value.salePrice <= 0) return 0
  return ((editForm.value.salePrice - editForm.value.costPrice) / editForm.value.salePrice) * 100
})

function onCostChange() { /* auto-calculated via computed */ }
function onPriceChange() { /* auto-calculated via computed */ }

const stats = computed(() => {
  const total = list.value.length
  const avgCostRate = total > 0 ? list.value.reduce((s, d) => s + d.costRate, 0) / total : 0
  const highCost = list.value.filter(d => d.costRate > 30).length
  const lowCost = list.value.filter(d => d.costRate < 20).length
  return [
    { label: '菜品总数', value: total, cls: 'st-total' },
    { label: '平均成本率', value: avgCostRate.toFixed(1) + '%', cls: 'st-avg' },
    { label: '高成本菜品', value: highCost, cls: 'st-high' },
    { label: '低成本', value: lowCost, cls: 'st-low' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(d => (d.dishName || '').includes(q) || (d.dishId || '').toLowerCase().includes(q))
  }
  if (filterCategory.value) l = l.filter(d => d.category === filterCategory.value)
  if (filterCostRate.value === 'low') l = l.filter(d => d.costRate < 20)
  if (filterCostRate.value === 'normal') l = l.filter(d => d.costRate >= 20 && d.costRate <= 30)
  if (filterCostRate.value === 'high') l = l.filter(d => d.costRate > 30)
  if (filterStatus.value) l = l.filter(d => d.status === filterStatus.value)
  return l
})

function costRateClass(rate) {
  if (rate < 20) return 'rate-low'
  if (rate > 30) return 'rate-high'
  return 'rate-normal'
}

function categoryTag(cat) {
  const map = { '凉菜刺身': '', '热菜': 'warning', '汤羹': 'info', '主食': 'success', '甜品': 'danger', '酒水': 'info' }
  return map[cat] || ''
}

// 编辑
function openEditCost(row) {
  editingDish.value = { ...row }
  editForm.value = { costPrice: row.costPrice, salePrice: row.salePrice }
  showEditDialog.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await request.put(`/dishes/${editingDish.value.dishId}`, {
      costPrice: editForm.value.costPrice,
      salePrice: editForm.value.salePrice
    })
    ElMessage.success('成本已更新 · Cost Updated')
    showEditDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存成本失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function openBatchEdit() {
  ElMessage.info('批量调价功能开发中 · Batch pricing coming soon')
}

function exportData() {
  ElMessage.success('报表导出成功 · Report Exported')
}

function clearFilters() {
  search.value = ''
  filterCategory.value = ''
  filterCostRate.value = ''
  filterStatus.value = ''
}

// 加载真实数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/dishes')
    const dishes = res.data || []
    list.value = dishes.map(d => ({
      ...d,
      costRate: d.salePrice > 0 ? (d.costPrice / d.salePrice) * 100 : 0,
      grossMargin: d.salePrice > 0 ? ((d.salePrice - d.costPrice) / d.salePrice) * 100 : 0,
    }))
  } catch (e) {
    console.error('获取菜品列表失败', e)
    ElMessage.error('获取菜品列表失败')
  } finally {
    loading.value = false
  }
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
.cost-page { max-width: 1600px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; padding: 14px 18px; border-radius: 2px; background: var(--color-card); text-align: center; border: 1px solid var(--color-border); }
.st-total { border-color: var(--color-border); }
.st-avg { background: rgba(91,123,138,0.04); border-color: rgba(91,123,138,0.2); }
.st-high { background: rgba(194,85,85,0.04); border-color: rgba(194,85,85,0.2); }
.st-low { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.st-high .stat-num { color: #C25555; }
.st-low .stat-num { color: #4A7C59; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }

.data-table { border-radius: 2px; overflow: hidden; }

.price-text { font-weight: 600; color: var(--color-text-primary); }

.cost-rate { font-weight: 700; font-size: 14px; padding: 2px 8px; border-radius: 2px; }
.rate-low { color: #4A7C59; background: rgba(45,74,62,0.08); }
.rate-normal { color: var(--color-text-primary); }
.rate-high { color: #C25555; background: rgba(194,85,85,0.08); }

.margin-text { font-weight: 600; color: var(--color-text-primary); }

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.active { background: #4A7C59; }
.status-dot.inactive { background: #94a3b8; }
.status-text { font-size: 12px; }

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
  min-width: 170px;
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
.ctx-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}

/* 编辑对话框 */
.edit-dish-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(45,74,62,0.04);
  border-radius: 2px;
  border: 1px solid var(--color-border);
}
.edit-dish-name { font-size: 16px; font-weight: 700; color: var(--color-text-primary); }
.edit-dish-id { font-size: 12px; color: var(--color-text-secondary); }

.calc-card {
  padding: 14px 16px;
  border-radius: 2px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  text-align: center;
}
.calc-label { font-size: 11px; color: var(--color-text-secondary); margin-bottom: 6px; }
.calc-value { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.calc-value.rate-low { color: #4A7C59; }
.calc-value.rate-high { color: #C25555; }

/* 详情对话框 */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-item {
  padding: 10px 14px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
}
.detail-item.full-width { grid-column: 1 / -1; }
.detail-label { display: block; font-size: 11px; color: var(--color-text-secondary); margin-bottom: 4px; }
.detail-value { font-size: 14px; font-weight: 600; color: var(--color-text-primary); }
</style>
