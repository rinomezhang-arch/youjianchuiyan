<template>
  <div class="pricing-manage-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">调价管理 · Pricing Manage</h2>
        <p class="page-subtitle">价格策略 · 批量调价 · 历史追踪</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品..." clearable class="search-input" />
        <el-select v-model="filterCategory" placeholder="全部分类" clearable class="filter-select">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-button type="primary" @click="batchAdjust">批量调价</el-button>
      </div>
    </div>

    <!-- 调价历史 -->
    <div class="history-card">
      <div class="history-title">最近调价记录 · Recent Adjustments</div>
      <div class="history-list">
        <div v-for="(item, idx) in recentChanges" :key="idx" class="history-item">
          <span class="history-time">{{ item.time }}</span>
          <span class="history-dish">{{ item.dishName }}</span>
          <span class="history-change">
            ¥{{ item.oldPrice.toFixed(0) }} → ¥{{ item.newPrice.toFixed(0) }}
            <span :class="item.newPrice > item.oldPrice ? 'price-up' : 'price-down'">
              {{ item.newPrice > item.oldPrice ? '↑' : '↓' }}
              {{ Math.abs(((item.newPrice - item.oldPrice) / item.oldPrice) * 100).toFixed(1) }}%
            </span>
          </span>
          <span class="history-operator">{{ item.operator }}</span>
        </div>
        <div v-if="recentChanges.length === 0" class="empty-history">暂无调价记录</div>
      </div>
    </div>

    <!-- 菜品价格列表 -->
    <div class="price-table-wrapper">
      <el-table :data="filteredDishes" stripe v-loading="loading">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="dishName" label="菜品名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="当前售价" width="120">
          <template #default="{ row }">
            <span class="current-price">¥{{ (row.salePrice || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="新价格" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.newPrice"
              :precision="2"
              :min="0"
              controls-position="right"
              size="small"
              class="full-width"
              @change="(val) => onPriceChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="变化" width="100">
          <template #default="{ row }">
            <span v-if="row.newPrice !== undefined && row.newPrice !== row.salePrice"
              :class="row.newPrice > row.salePrice ? 'price-up' : 'price-down'">
              {{ row.newPrice > row.salePrice ? '+' : '' }}{{ (row.newPrice - row.salePrice).toFixed(2) }}
            </span>
            <span v-else class="no-change">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastAdjustDate" label="最近调价" width="120">
          <template #default="{ row }">
            <span class="muted-text">{{ row.lastAdjustDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="applyPrice(row)" :disabled="row.newPrice === undefined || row.newPrice === row.salePrice">
              应用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 批量调价弹窗 -->
    <el-dialog v-model="showBatchDialog" title="批量调价" width="500px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="调价范围">
          <el-select v-model="batchForm.category" placeholder="选择分类（留空为全部）" clearable class="full-width">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="调价方式">
          <el-radio-group v-model="batchForm.mode">
            <el-radio label="percent">百分比调整</el-radio>
            <el-radio label="fixed">固定金额</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="batchForm.mode === 'percent' ? '调整比例' : '调整金额'">
          <el-input-number
            v-model="batchForm.value"
            :precision="batchForm.mode === 'percent' ? 1 : 2"
            controls-position="right"
            class="full-width"
          />
          <span class="form-suffix">{{ batchForm.mode === 'percent' ? '%' : '元' }}</span>
        </el-form-item>
      </el-form>
      <div class="batch-preview" v-if="batchPreview.length > 0">
        <div class="batch-preview-title">预览 ({{ batchPreview.length }}道菜品)</div>
        <div class="batch-preview-list">
          <div v-for="(item, idx) in batchPreview.slice(0, 10)" :key="idx" class="batch-preview-item">
            <span>{{ item.dishName }}</span>
            <span class="batch-change">¥{{ item.oldPrice.toFixed(0) }} → ¥{{ item.newPrice.toFixed(0) }}</span>
          </div>
          <div v-if="batchPreview.length > 10" class="batch-more">...还有{{ batchPreview.length - 10 }}道</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="applyBatch">确认调价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDishes, updateDish } from '@/api/dish'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const searchQuery = ref('')
const filterCategory = ref('')
const showBatchDialog = ref(false)

const categories = ['凉菜', '热菜', '汤羹', '主食', '点心', '水果', '饮品']

const batchForm = ref({
  category: '',
  mode: 'percent',
  value: 0
})

const recentChanges = ref([])

const filteredDishes = computed(() => {
  let result = list.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(d => (d.dishName || '').toLowerCase().includes(q))
  }
  if (filterCategory.value) {
    result = result.filter(d => (d.categoryName || d.category) === filterCategory.value)
  }
  return result
})

const batchPreview = computed(() => {
  if (batchForm.value.value === 0) return []
  let target = list.value
  if (batchForm.value.category) {
    target = target.filter(d => (d.categoryName || d.category) === batchForm.value.category)
  }
  return target.map(d => {
    const oldPrice = d.salePrice || 0
    let newPrice
    if (batchForm.value.mode === 'percent') {
      newPrice = oldPrice * (1 + batchForm.value.value / 100)
    } else {
      newPrice = oldPrice + batchForm.value.value
    }
    return { ...d, oldPrice, newPrice: Math.max(0, newPrice) }
  }).filter(d => d.newPrice !== d.oldPrice)
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getDishes()
    if (res.data) {
      list.value = (res.data.content || res.data || []).map(d => ({
        ...d,
        dishId: d.dishId || d.id,
        dishName: d.dishName || d.name,
        categoryName: d.categoryName || d.category,
        newPrice: d.salePrice || d.price || 0
      }))
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onPriceChange(row, val) {
  row.newPrice = val
}

async function applyPrice(row) {
  try {
    await ElMessageBox.confirm(
      `确定将"${row.dishName}"从¥${row.salePrice.toFixed(2)}调整为¥${row.newPrice.toFixed(2)}？`,
      '确认调价', { type: 'warning' }
    )
    const res = await updateDish(row.dishId, { salePrice: row.newPrice })
    if (res.code === 200) {
      recentChanges.value.unshift({
        time: new Date().toLocaleTimeString(),
        dishName: row.dishName,
        oldPrice: row.salePrice,
        newPrice: row.newPrice,
        operator: '管理员'
      })
      if (recentChanges.value.length > 20) recentChanges.value.pop()
      row.salePrice = row.newPrice
      row.lastAdjustDate = new Date().toLocaleDateString()
      ElMessage.success('调价成功')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('调价失败')
  }
}

function batchAdjust() {
  batchForm.value = { category: '', mode: 'percent', value: 0 }
  showBatchDialog.value = true
}

async function applyBatch() {
  if (batchPreview.value.length === 0) {
    ElMessage.warning('没有需要调价的菜品')
    return
  }
  try {
    await ElMessageBox.confirm(`确定对${batchPreview.value.length}道菜品进行调价？`, '确认批量调价', { type: 'warning' })
    let success = 0
    for (const item of batchPreview.value) {
      try {
        await updateDish(item.dishId, { salePrice: item.newPrice })
        success++
        const row = list.value.find(d => d.dishId === item.dishId)
        if (row) {
          recentChanges.value.unshift({
            time: new Date().toLocaleTimeString(),
            dishName: row.dishName,
            oldPrice: item.oldPrice,
            newPrice: item.newPrice,
            operator: '批量'
          })
          row.salePrice = item.newPrice
          row.newPrice = item.newPrice
          row.lastAdjustDate = new Date().toLocaleDateString()
        }
      } catch (e) { /* continue */ }
    }
    if (recentChanges.value.length > 20) recentChanges.value = recentChanges.value.slice(0, 20)
    ElMessage.success(`批量调价完成，${success}/${batchPreview.value.length}道成功`)
    showBatchDialog.value = false
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量调价失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.pricing-manage-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.search-input { width: 220px; }
.filter-select { width: 130px; }
.history-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; margin-bottom: 20px; }
.history-title { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 12px; }
.history-list { display: flex; flex-direction: column; gap: 6px; max-height: 160px; overflow-y: auto; }
.history-item { display: flex; align-items: center; gap: 16px; padding: 8px 12px; background: var(--color-bg-alt); border-radius: var(--radius-sm); font-size: 13px; }
.history-time { color: var(--color-text-muted); font-size: 12px; min-width: 60px; }
.history-dish { font-weight: 500; min-width: 120px; }
.history-change { flex: 1; }
.history-operator { color: var(--color-text-muted); font-size: 12px; }
.price-up { color: var(--color-danger); font-weight: 600; }
.price-down { color: var(--color-success); font-weight: 600; }
.no-change { color: var(--color-text-muted); }
.muted-text { color: var(--color-text-muted); font-size: 12px; }
.empty-history { text-align: center; color: var(--color-text-muted); padding: 20px; font-size: 13px; }
.price-table-wrapper { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.current-price { font-weight: 600; color: var(--color-text); }
.full-width { width: 100%; }
.form-suffix { margin-left: 8px; color: var(--color-text-muted); }
.batch-preview { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--color-border-light); }
.batch-preview-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.batch-preview-list { max-height: 200px; overflow-y: auto; }
.batch-preview-item { display: flex; justify-content: space-between; padding: 6px 8px; font-size: 13px; }
.batch-change { color: var(--color-accent-dark); }
.batch-more { text-align: center; color: var(--color-text-muted); padding: 8px; font-size: 12px; }
</style>
