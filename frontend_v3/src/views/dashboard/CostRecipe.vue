<template>
  <div class="cost-recipe-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">成本配方 · Cost Recipe</h2>
        <p class="page-subtitle">按最近一次有效入库价、单位换算和出成率核算标准原料成本；售价独立管理。</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品..." clearable class="search-input" />
        <el-button type="primary" @click="recalcAll">重新核算全部</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">总菜品数</div>
        <div class="stat-value">{{ stats.totalDishes }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均成本率</div>
        <div class="stat-value">{{ stats.avgCostRate.toFixed(1) }}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">高成本菜品</div>
        <div class="stat-value cost-warn">{{ stats.highCostCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">无配方菜品</div>
        <div class="stat-value">{{ stats.noRecipeCount }}</div>
      </div>
    </div>

    <!-- 菜品成本列表 -->
    <div class="cost-table-wrapper">
      <el-table :data="filteredDishes" stripe v-loading="loading" @row-click="viewRecipe">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="dishName" label="菜品名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价" width="100">
          <template #default="{ row }">¥{{ (row.salePrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="costPrice" label="成本" width="100">
          <template #default="{ row }">¥{{ (row.costPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="costRate" label="成本率" width="100">
          <template #default="{ row }">
            <span :class="{ 'cost-high': row.costRate > 45, 'cost-low': row.costRate < 30 }">
              {{ row.costRate == null ? '未核算' : row.costRate.toFixed(1) + '%' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="grossProfit" label="毛利" width="100">
          <template #default="{ row }">
            ¥{{ ((row.salePrice || 0) - (row.costPrice || 0)).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.hasRecipe ? 'success' : 'info'" size="small">
              {{ row.hasRecipe ? '有配方' : '无配方' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click.stop="editRecipe(row)">编辑配方</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 配方编辑弹窗 -->
    <el-dialog v-model="showRecipeDialog" :title="`配方编辑 - ${currentDish?.dishName}`" width="800px" class="cost-recipe-dialog">
      <div class="recipe-editor">
        <div class="recipe-header">
          <div class="recipe-info">
            <span class="info-label">售价：¥{{ currentDish?.salePrice?.toFixed(2) }}</span>
            <span class="info-label">预计原料成本：{{ calculatedCost == null ? '待补全配方' : '¥' + calculatedCost.toFixed(2) }}</span>
            <span class="info-label" :class="{ 'cost-high': calculatedCostRate > 45 }">
              成本率：{{ calculatedCostRate == null ? '待核算' : calculatedCostRate.toFixed(1) + '%' }}
            </span>
          </div>
        </div>

        <el-alert v-if="recipeError" :title="recipeError" type="warning" :closable="false" />

        <el-table :data="recipeItems" stripe class="recipe-table">
          <el-table-column prop="ingredientId" label="原料" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.ingredientId" size="small" filterable placeholder="选择原料" class="full-width" @change="onIngredientPick(row)">
                <el-option v-for="ing in ingredientOptions" :key="ing.ingredientId" :label="ing.ingredientName" :value="ing.ingredientId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="用量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :precision="3" :min="0" controls-position="right" size="small" class="full-width" />
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="100">
            <template #default="{ row }">
              <el-input v-model="row.unit" size="small" placeholder="克/斤/个" />
            </template>
          </el-table-column>
          <el-table-column prop="yieldRate" label="出成率(%)" width="130">
            <template #default="{row}"><el-input-number v-model="row.yieldRate" :min="0.01" :max="999.99" :precision="2" controls-position="right" size="small" /></template>
          </el-table-column>
          <el-table-column prop="unitPrice" label="净料单价" width="120">
            <template #default="{ row }">{{ linePreview(row).error ? '待核对' : '¥' + linePreview(row).netUnitPrice.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">
              {{ linePreview(row).error ? '待核对' : '¥' + linePreview(row).totalCost.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button text size="small" type="danger" @click="removeIngredient($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button @click="addIngredient" class="mt-3">+ 添加原料</el-button>
      </div>
      <template #footer>
        <el-button @click="showRecipeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRecipe">保存配方</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { previewRecipeLine } from '@/utils/dishCostPreview'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.storeId)

const loading = ref(false)
const error = ref('')
const list = ref([])
const searchQuery = ref('')
const showRecipeDialog = ref(false)
const currentDish = ref(null)
const recipeItems = ref([])
const ingredientOptions = ref([])

const stats = computed(() => {
  const total = list.value.length
  const avgCost = total > 0 ? list.value.reduce((sum, d) => sum + (d.costRate || 0), 0) / total : 0
  const highCost = list.value.filter(d => d.costRate > 45).length
  const noRecipe = list.value.filter(d => !d.hasRecipe).length
  return { totalDishes: total, avgCostRate: avgCost, highCostCount: highCost, noRecipeCount: noRecipe }
})

const filteredDishes = computed(() => {
  if (!searchQuery.value) return list.value
  const q = searchQuery.value.toLowerCase()
  return list.value.filter(d => (d.dishName || '').toLowerCase().includes(q))
})

const linePreview = row => previewRecipeLine(row, ingredientOptions.value.find(i => i.ingredientId === row.ingredientId))
const recipeError = computed(() => recipeItems.value.map(linePreview).find(item => item.error)?.error || '')
const calculatedCost = computed(() => !recipeItems.value.length || recipeError.value ? null : Number(recipeItems.value.reduce((sum, item) => sum + linePreview(item).totalCost, 0).toFixed(2)))

const calculatedCostRate = computed(() => {
  const price = currentDish.value?.salePrice || 0
  return price > 0 && calculatedCost.value != null ? (calculatedCost.value / price) * 100 : null
})

async function fetchIngredientOptions() {
  try {
    const res = await request.get('/ingredients', { params: { storeId: currentStoreId.value } })
    ingredientOptions.value = res.data || []
  } catch (e) {
    console.error('获取原料列表失败:', e)
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    // /cost/ranking 是真实接口(dish_master 真实聚合)，/recipes/dishes-with-recipe
    // 用来标出哪些菜品已经配好了原料配方(hasRecipe)
    const [rankingRes, withRecipeRes] = await Promise.all([
      request.get('/cost/ranking', { params: { size: 500 } }),
      request.get('/recipes/dishes-with-recipe').catch(() => ({ data: [] }))
    ])
    const rankingData = rankingRes.data || rankingRes
    const withRecipeIds = new Set((withRecipeRes.data || []).map(d => d.dishId))
    list.value = (rankingData.content || []).map(d => ({
      dishId: d.dishId,
      dishName: d.dishName,
      categoryName: d.category,
      salePrice: d.salePrice || 0,
      costPrice: d.costPrice || 0,
      costRate: withRecipeIds.has(d.dishId) && d.costPrice != null && d.salePrice > 0 ? Number(d.costRate) : null,
      hasRecipe: withRecipeIds.has(d.dishId)
    }))
  } catch (e) {
    console.error('获取成本配方失败:', e)
    error.value = '加载失败，请刷新重试'
    ElMessage.error('加载成本配方失败')
  } finally {
    loading.value = false
  }
}

function viewRecipe(row) {
  editRecipe(row)
}

async function editRecipe(row) {
  currentDish.value = row
  recipeItems.value = []
  showRecipeDialog.value = true
  try {
    const res = await request.get(`/recipes/${row.dishId}`)
    recipeItems.value = (res.data || []).map(r => ({
      ingredientId: r.ingredientId,
      ingredientName: r.ingredientName,
      quantity: r.quantity || 0,
      unit: r.unit || '',
      unitPrice: r.unitPrice,
      yieldRate: r.yieldRate ?? ingredientOptions.value.find(i => i.ingredientId === r.ingredientId)?.yieldRate,
      wastageRate: r.wastageRate
    }))
  } catch (e) {
    console.error('获取配方明细失败:', e)
    ElMessage.error('获取配方明细失败')
  }
}

function onIngredientPick(row) {
  const ing = ingredientOptions.value.find(i => i.ingredientId === row.ingredientId)
  if (ing) {
    row.ingredientName = ing.ingredientName
    row.unitPrice = ing.unitPrice || 0
    row.unit = ing.usageUnit || ing.purchaseUnit || ing.unit || ''
    row.yieldRate = ing.yieldRate
  }
}

function addIngredient() {
  recipeItems.value.push({ ingredientId: '', ingredientName: '', quantity: 0, unit: '', unitPrice: null, yieldRate: null })
}

function removeIngredient(idx) {
  recipeItems.value.splice(idx, 1)
}

async function saveRecipe() {
  if (!currentDish.value) return
  const items = recipeItems.value.filter(r => r.ingredientId)
  if (!items.length || recipeError.value) { ElMessage.warning(recipeError.value || '请至少配置一条有效原料'); return }
  try {
    await request.post(`/recipes/${currentDish.value.dishId}`, items.map(r => ({
      ingredientId: r.ingredientId,
      quantity: r.quantity,
      unit: r.unit,
      yieldRate: r.yieldRate,
      wastageRate: r.wastageRate
    })))
    // 配方保存只更新配方明细本身，菜品的 costPrice/costRate 需要重算才会刷新
    await request.post('/recipes/recalc-all')
    ElMessage.success('配方已保存，成本已重新核算')
    showRecipeDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

async function recalcAll() {
  try {
    await request.post('/recipes/recalc-all')
    ElMessage.success('成本重新核算完成')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '核算失败')
  }
}

onMounted(() => {
  fetchIngredientOptions()
  fetchData()
})
</script>

<style scoped>
.cost-recipe-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.search-input { width: 220px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; text-align: center; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text); }
.cost-warn { color: var(--color-danger); }
.cost-table-wrapper { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.cost-high { color: var(--color-danger); font-weight: 600; }
.cost-low { color: var(--color-success); font-weight: 600; }
.recipe-editor { max-height: 500px; overflow-y: auto; }
.recipe-header { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border-light); }
.recipe-info { display: flex; gap: 24px; }
.info-label { font-size: 13px; color: var(--color-text-secondary); }
.recipe-table { margin-bottom: 12px; }
.full-width { width: 100%; }
.mt-3 { margin-top: 12px; }

/* ===== 移动端适配（≤768px）：页面层；桌面样式不受影响 ===== */
@media (max-width: 768px) {
  .cost-recipe-page { padding: 0 2px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 10px; margin-bottom: 14px; }
  .page-title { font-size: 18px; margin-bottom: 2px; }
  .page-subtitle { font-size: 12px; line-height: 1.5; }
  .page-header-right { flex-direction: column; align-items: stretch; gap: 8px; width: 100%; }
  .page-header-right .search-input { width: 100%; }
  .page-header-right .el-button { width: 100%; min-height: 40px; margin-left: 0; }
  .stats-row { grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 14px; }
  .stat-card { padding: 12px 8px; }
  .stat-label { font-size: 12px; margin-bottom: 4px; }
  .stat-value { font-size: 22px; }
  /* 列表允许明确的局部横向滚动，不把页面整体撑出屏幕 */
  .cost-table-wrapper { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .cost-table-wrapper .el-button { min-height: 36px; padding: 6px 10px; }
  .recipe-info { flex-wrap: wrap; gap: 8px 14px; }
  .recipe-editor { max-height: 56vh; }
  .recipe-editor > .el-button { width: 100%; min-height: 40px; margin-left: 0; }
}
</style>

<!-- 全局块：el-dialog teleport 到 body，scoped 后代选择器不跨 teleport；
     全部规则限定在本页专属 class .cost-recipe-dialog 下，不影响其他页面弹窗 -->
<style>
@media (max-width: 768px) {
  .cost-recipe-dialog {
    width: calc(100vw - 16px) !important;
    max-width: calc(100vw - 16px) !important;
    --el-dialog-width: calc(100vw - 16px);
    margin: 3vh 8px !important;
  }
  /* global.css 对 .el-dialog__header/__title/__body 用了 !important，
     专属选择器 + !important 才能在本弹窗落实手机端收紧，不动全局文件 */
  .cost-recipe-dialog .el-dialog__header { padding: 12px 14px !important; margin-right: 0 !important; }
  .cost-recipe-dialog .el-dialog__title { font-size: 15px !important; line-height: 1.4 !important; white-space: normal; padding-right: 28px; }
  .cost-recipe-dialog .el-dialog__body { padding: 12px !important; }
  .cost-recipe-dialog .el-dialog__footer { padding: 10px 14px 14px !important; display: flex; gap: 10px; }
  .cost-recipe-dialog .el-dialog__footer .el-button { flex: 1; min-height: 40px; margin-left: 0; }
  /* 弹窗内宽表格：局部横向滚动，标题/原料列不再被挤出视口 */
  .cost-recipe-dialog .el-table { width: 100%; }
  .cost-recipe-dialog .el-input-number { width: 100%; }
  /* 触控目标：表格内 size=small 控件实际高度仅 24px，手机端抬到 >=40px */
  .cost-recipe-dialog .el-select__wrapper { min-height: 40px !important; font-size: 15px; }
  .cost-recipe-dialog .el-input__wrapper { min-height: 40px !important; padding: 0 11px !important; }
  .cost-recipe-dialog .el-input-number { height: 40px !important; line-height: 40px; }
  .cost-recipe-dialog .el-input-number .el-input__inner { height: 40px !important; line-height: 40px !important; font-size: 15px; }
  /* 普通 el-input（如单位列）内框同样撑到 40px，与 wrapper 触控区一致 */
  .cost-recipe-dialog .el-input__inner { height: 40px !important; line-height: 40px !important; font-size: 15px; }
  .cost-recipe-dialog .el-input-number .el-input-number__increase,
  .cost-recipe-dialog .el-input-number .el-input-number__decrease { height: 20px !important; line-height: 20px !important; }
  .cost-recipe-dialog .el-table .cell { padding-left: 8px; padding-right: 8px; }
}
</style>
