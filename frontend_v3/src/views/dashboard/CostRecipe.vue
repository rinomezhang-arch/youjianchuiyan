<template>
  <div class="cost-recipe-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">成本配方 · Cost Recipe</h2>
        <p class="page-subtitle">配方管理 · 成本核算 · 毛利分析</p>
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
              {{ row.costRate.toFixed(1) }}%
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
    <el-dialog v-model="showRecipeDialog" :title="`配方编辑 - ${currentDish?.dishName}`" width="800px">
      <div class="recipe-editor">
        <div class="recipe-header">
          <div class="recipe-info">
            <span class="info-label">售价：¥{{ currentDish?.salePrice?.toFixed(2) }}</span>
            <span class="info-label">成本：¥{{ calculatedCost.toFixed(2) }}</span>
            <span class="info-label" :class="{ 'cost-high': calculatedCostRate > 45 }">
              成本率：{{ calculatedCostRate.toFixed(1) }}%
            </span>
          </div>
        </div>

        <el-table :data="recipeItems" stripe class="recipe-table">
          <el-table-column prop="ingredientName" label="原料名称" min-width="150">
            <template #default="{ row, $index }">
              <el-input v-model="row.ingredientName" size="small" placeholder="原料名称" />
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="用量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :precision="2" :min="0" controls-position="right" size="small" class="full-width" />
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="100">
            <template #default="{ row }">
              <el-input v-model="row.unit" size="small" placeholder="克/斤/个" />
            </template>
          </el-table-column>
          <el-table-column prop="unitPrice" label="单价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :precision="2" :min="0" controls-position="right" size="small" class="full-width" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">
              ¥{{ ((row.quantity || 0) * (row.unitPrice || 0)).toFixed(2) }}
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

const loading = ref(false)
const error = ref('')
const list = ref([])
const searchQuery = ref('')
const showRecipeDialog = ref(false)
const currentDish = ref(null)
const recipeItems = ref([])

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

const calculatedCost = computed(() => {
  return recipeItems.value.reduce((sum, item) => sum + (item.quantity || 0) * (item.unitPrice || 0), 0)
})

const calculatedCostRate = computed(() => {
  const price = currentDish.value?.salePrice || 0
  return price > 0 ? (calculatedCost.value / price) * 100 : 0
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/cost-recipes')
    const data = res.data || res
    list.value = (Array.isArray(data) ? data : data.content || []).map(d => ({
      dishId: d.dishId || d.id,
      dishName: d.dishName || d.name,
      categoryName: d.categoryName || d.category,
      salePrice: d.salePrice || d.price || 0,
      costPrice: d.costPrice || 0,
      costRate: d.costRate || 0,
      hasRecipe: !!d.recipeItems
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

function editRecipe(row) {
  currentDish.value = row
  recipeItems.value = row.recipeItems ? [...row.recipeItems] : []
  showRecipeDialog.value = true
}

function addIngredient() {
  recipeItems.value.push({ ingredientName: '', quantity: 0, unit: '克', unitPrice: 0 })
}

function removeIngredient(idx) {
  recipeItems.value.splice(idx, 1)
}

async function saveRecipe() {
  if (!currentDish.value) return
  try {
    await request.put(`/cost-recipes/${currentDish.value.dishId}`, recipeItems.value)
    ElMessage.success('配方已保存')
    showRecipeDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function recalcAll() {
  try {
    await request.post('/cost-recipes/recalc')
    ElMessage.success('成本重新核算完成')
    fetchData()
  } catch (e) {
    ElMessage.error('核算失败')
  }
}

onMounted(fetchData)
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
</style>
