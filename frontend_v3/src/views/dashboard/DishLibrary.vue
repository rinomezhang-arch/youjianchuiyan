<template>
  <div class="dish-library-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">菜库编辑 · Dish Library</h2>
        <p class="page-subtitle">菜品信息管理 · 分类维护 · 图片管理</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品..." clearable class="search-input" />
        <el-select v-model="filterCategory" placeholder="全部分类" clearable class="filter-select">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-button type="primary" @click="openAddDish">+ 新增菜品</el-button>
      </div>
    </div>

    <!-- 菜品列表 -->
    <div class="dish-table-wrapper">
      <el-table :data="filteredDishes" stripe v-loading="loading" class="dish-table">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.imageUrl || row.image" :src="row.imageUrl || row.image" class="table-thumb" />
            <div v-else class="thumb-placeholder">菜</div>
          </template>
        </el-table-column>
        <el-table-column prop="dishName" label="菜品名称" min-width="150" />
        <el-table-column prop="englishName" label="英文名" min-width="120" />
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价" width="90">
          <template #default="{ row }">¥{{ (row.salePrice || row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="costPrice" label="成本" width="90">
          <template #default="{ row }">¥{{ (row.costPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="costRate" label="成本率" width="90">
          <template #default="{ row }">
            <span :class="{ 'cost-high': (row.costRate || 0) > 45 }">
              {{ (row.costRate || 0).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'soldout' ? 'danger' : 'success'" size="small">
              {{ row.status === 'soldout' ? '沽清' : '在售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="editDish(row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="deleteDish(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑菜品弹窗 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑菜品' : '新增菜品'" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜品名称" required>
              <el-input v-model="form.dishName" placeholder="请输入菜品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文名">
              <el-input v-model="form.englishName" placeholder="English name" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类" required>
              <el-select v-model="form.category" placeholder="选择分类" class="full-width">
                <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :precision="2" :min="0" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="份/斤/只" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="菜品描述（可选）" />
        </el-form-item>
        <el-form-item label="菜品图片">
          <div class="image-upload-area" @click="triggerUpload">
            <img v-if="form.imageUrl" :src="form.imageUrl" class="preview-image" />
            <div v-else class="upload-placeholder">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              <span>点击上传图片</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDish">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDishes, createDish, updateDish, deleteDish as apiDeleteDish } from '@/api/dish'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const searchQuery = ref('')
const filterCategory = ref('')
const showDialog = ref(false)
const editing = ref(false)

const categories = ['凉菜', '热菜', '汤羹', '主食', '点心', '水果', '饮品']

const form = ref({
  dishId: '',
  dishName: '',
  englishName: '',
  category: '',
  salePrice: 0,
  unit: '份',
  description: '',
  imageUrl: ''
})

const filteredDishes = computed(() => {
  let result = list.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(d =>
      (d.dishName || '').toLowerCase().includes(q) ||
      (d.englishName || '').toLowerCase().includes(q)
    )
  }
  if (filterCategory.value) {
    result = result.filter(d => (d.categoryName || d.category) === filterCategory.value)
  }
  return result
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
        categoryName: d.categoryName || d.category
      }))
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openAddDish() {
  editing.value = false
  form.value = { dishId: '', dishName: '', englishName: '', category: '', salePrice: 0, unit: '份', description: '', imageUrl: '' }
  showDialog.value = true
}

function editDish(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveDish() {
  if (!form.value.dishName) {
    ElMessage.warning('请输入菜品名称')
    return
  }
  try {
    const res = editing.value
      ? await updateDish(form.value.dishId, form.value)
      : await createDish(form.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showDialog.value = false
      fetchData()
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteDish(row) {
  try {
    await ElMessageBox.confirm(`确定删除菜品"${row.dishName}"？`, '确认删除', { type: 'warning' })
    const res = await apiDeleteDish(row.dishId)
    if (res.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function triggerUpload() {
  ElMessage.info('图片上传功能开发中')
}

onMounted(fetchData)
</script>

<style scoped>
.dish-library-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.search-input { width: 220px; }
.filter-select { width: 130px; }
.dish-table-wrapper { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.table-thumb { width: 50px; height: 50px; border-radius: 6px; object-fit: cover; }
.thumb-placeholder { width: 50px; height: 50px; border-radius: 6px; background: var(--color-bg-alt); color: var(--color-text-muted); font-size: 12px; display: flex; align-items: center; justify-content: center; }
.cost-high { color: var(--color-danger); font-weight: 600; }
.full-width { width: 100%; }
.image-upload-area { width: 120px; height: 120px; border: 2px dashed var(--color-border); border-radius: var(--radius-md); cursor: pointer; overflow: hidden; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.image-upload-area:hover { border-color: var(--color-accent); }
.preview-image { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--color-text-muted); font-size: 12px; }
</style>
