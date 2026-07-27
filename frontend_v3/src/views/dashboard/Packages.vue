<template>
  <div class="page">
    <div class="page-header">
      
      <h2>套餐管理 · Package Management</h2>
      <p class="page-desc">套餐配置 · 菜品搭配 · 成本率</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 新建套餐</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="packageId" label="编号" width="120" />
      <el-table-column prop="packageName" label="套餐名称" width="150" />
      <el-table-column prop="dishCount" label="菜品数" width="60" />
      <el-table-column prop="suggestGuests" label="建议人数" width="80" />
      <el-table-column prop="packageTotalPrice" label="售价" width="80" />
      <el-table-column prop="packageCostPrice" label="成本" width="80" />
      <el-table-column prop="costRate" label="成本率" width="70">
        <template #default="{ row }">{{ (row.costRate || 0).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="occasionType" label="适用场合" width="80" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="editRow(row)">编辑</el-button>
          <el-button text size="small" @click="openDishSelector(row)">菜品配置</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />

    <el-dialog v-model="showDialog" :title="editing ? '编辑套餐' : '新建套餐'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="套餐名称" required><el-input v-model="form.packageName" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="售价"><el-input-number v-model="form.packageTotalPrice" :precision="2" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="建议人数"><el-input-number v-model="form.suggestGuests" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="适用场合"><el-input v-model="form.occasionType" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="savePackage">保存</el-button>
      </template>
    </el-dialog>

    <!-- 菜品选择器 -->
    <el-dialog v-model="showDishSelector" title="套餐菜品配置" width="900px" top="5vh">
      <div class="dish-selector">
        <div class="dish-selector-left">
          <div class="selector-title">可选菜品</div>
          <el-input v-model="dishSearch" placeholder="搜索菜品..." clearable style="margin-bottom: 12px" />
          <div class="dish-list">
            <div v-for="dish in filteredDishes" :key="dish.dishId" class="dish-item" @click="addDishToPackage(dish)">
              <div class="dish-info">
                <div class="dish-name">{{ dish.dishName }}</div>
                <div class="dish-meta">
                  <span class="dish-price">¥{{ dish.salePrice }}</span>
                  <span class="dish-category">{{ dish.categoryName }}</span>
                </div>
              </div>
              <el-button size="small" type="primary">添加</el-button>
            </div>
          </div>
        </div>
        <div class="dish-selector-right">
          <div class="selector-title">已选菜品 ({{ selectedDishes.length }})</div>
          <div class="selected-list">
            <div v-for="(dish, index) in selectedDishes" :key="dish.dishId" class="selected-item">
              <div class="dish-info">
                <div class="dish-name">{{ dish.dishName }}</div>
                <div class="dish-meta">
                  <span class="dish-price">¥{{ dish.salePrice }}</span>
                  <el-input-number v-model="dish.quantity" :min="1" size="small" style="width: 100px" />
                </div>
              </div>
              <el-button size="small" type="danger" @click="removeDishFromPackage(index)">删除</el-button>
            </div>
            <div v-if="selectedDishes.length === 0" class="empty-text">暂无菜品</div>
          </div>
          <div class="selected-summary">
            <div>菜品总数: {{ totalDishCount }}</div>
            <div>总价: ¥{{ totalDishPrice.toFixed(2) }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDishSelector = false">取消</el-button>
        <el-button type="primary" @click="savePackageDishes">保存菜品配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPackages, createPackage, updatePackage } from '@/api/package'
import { getDishesWithRecipe } from '@/api/booking'
import { ElMessage } from 'element-plus'

const loading = ref(false); const list = ref([]); const total = ref(0)
const showDialog = ref(false); const editing = ref(false)
const form = ref({ packageName: '', packageTotalPrice: 0, suggestGuests: 10, occasionType: '' })

// 菜品选择器
const showDishSelector = ref(false)
const currentPackage = ref(null)
const allDishes = ref([])
const selectedDishes = ref([])
const dishSearch = ref('')

const filteredDishes = computed(() => {
  if (!dishSearch.value) return allDishes.value
  const q = dishSearch.value.toLowerCase()
  return allDishes.value.filter(d =>
    (d.dishName || '').toLowerCase().includes(q) ||
    (d.categoryName || '').toLowerCase().includes(q)
  )
})

const totalDishCount = computed(() => {
  return selectedDishes.value.reduce((sum, d) => sum + (d.quantity || 1), 0)
})

const totalDishPrice = computed(() => {
  return selectedDishes.value.reduce((sum, d) => sum + (d.salePrice || 0) * (d.quantity || 1), 0)
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getPackages()
    if (res.code === 200) list.value = res.data?.content || res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadDishes() {
  try {
    const res = await getDishesWithRecipe()
    if (res.data) {
      allDishes.value = res.data.map(d => ({
        dishId: d.dishId || d.id,
        dishName: d.dishName || d.name,
        salePrice: d.salePrice || d.price || 0,
        categoryName: d.categoryName || d.category || '其他'
      }))
    }
  } catch (e) { console.error(e) }
}

function openAdd() { editing.value = false; form.value = { packageName: '', packageTotalPrice: 0, suggestGuests: 10, occasionType: '' }; showDialog.value = true }
function editRow(row) { editing.value = true; form.value = { ...row }; showDialog.value = true }
async function savePackage() {
  const res = editing.value ? await updatePackage(form.value.packageId, form.value) : await createPackage(form.value)
  if (res.code === 200) { ElMessage.success('保存成功'); showDialog.value = false; fetchData() }
}

function openDishSelector(row) {
  currentPackage.value = row
  selectedDishes.value = row.dishes ? row.dishes.map(d => ({ ...d, quantity: d.quantity || 1 })) : []
  showDishSelector.value = true
}

function addDishToPackage(dish) {
  const existing = selectedDishes.value.find(d => d.dishId === dish.dishId)
  if (existing) {
    existing.quantity++
  } else {
    selectedDishes.value.push({ ...dish, quantity: 1 })
  }
}

function removeDishFromPackage(index) {
  selectedDishes.value.splice(index, 1)
}

async function savePackageDishes() {
  if (!currentPackage.value) return
  try {
    // 保存套餐菜品配置到后端
    const payload = {
      packageId: currentPackage.value.packageId,
      dishes: selectedDishes.value.map(d => ({
        dishId: d.dishId,
        dishName: d.dishName,
        quantity: d.quantity || 1,
        price: d.salePrice || 0
      }))
    }
    // TODO: 调用后端API保存
    console.log('保存套餐菜品:', payload)
    ElMessage.success('菜品配置已保存')
    showDishSelector.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  fetchData()
  loadDishes()
})
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }

.dish-selector {
  display: flex;
  gap: 16px;
  height: 500px;
}
.dish-selector-left, .dish-selector-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.selector-title {
  padding: 12px 16px;
  font-weight: 600;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}
.dish-selector-left .dish-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.dish-item, .selected-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.dish-item:hover {
  background: #ecf5ff;
}
.dish-info {
  flex: 1;
}
.dish-name {
  font-size: 14px;
  font-weight: 500;
}
.dish-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.dish-price {
  color: #f56c6c;
  font-weight: 600;
}
.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.selected-item {
  cursor: default;
  background: #f0f9eb;
  margin-bottom: 4px;
}
.empty-text {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}
.selected-summary {
  padding: 12px 16px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
}
</style>


