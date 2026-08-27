<template>
  <div class="dish-library-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">菜库编辑 · Dish Library</h2>
        <p class="page-subtitle">菜品信息管理 · 分类维护 · 图片管理</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品名称..." clearable class="search-input" />
        <el-select v-model="filterCategory" placeholder="全部分类" clearable class="filter-select">
          <el-option v-for="cat in categoryList" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-button type="primary" @click="openAddDish">+ 新增菜品</el-button>
      </div>
    </div>

    <div class="dish-table-wrapper">
      <el-table :data="pagedData" stripe v-loading="loading" class="dish-table">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.imageUrl" :src="row.imageUrl" class="table-thumb" />
            <div v-else class="thumb-placeholder">{{ (row.dishName || '菜').charAt(0) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="dishName" label="菜品名称" min-width="150" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100">
          <template #default="{ row }">¥{{ (row.salePrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="tags" label="口味/标签" width="120" />
        <el-table-column label="预计时间" width="100">
          <template #default="{ row }">{{ row.cookingTime ? row.cookingTime + '分钟' : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '上架' : '下架' }}
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

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredData.length"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :current-page="currentPage"
          @size-change="(s) => { pageSize = s; currentPage = 1 }"
          @current-change="(p) => { currentPage = p }"
        />
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="editing ? '编辑菜品' : '新增菜品'" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜品名称" required>
              <el-input v-model="form.dishName" placeholder="请输入菜品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜品分类" required>
              <el-select v-model="form.category" placeholder="选择分类" filterable allow-create class="full-width">
                <el-option v-for="cat in categoryList" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="售价" required>
              <el-input-number v-model="form.salePrice" :precision="2" :min="0" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="口味/标签">
              <el-input v-model="form.tags" placeholder="如：微辣" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计时间(分钟)">
              <el-input-number v-model="form.cookingTime" :min="0" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="是否上架">
          <el-switch v-model="form.status" active-value="active" inactive-value="inactive" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item label="菜品介绍">
          <el-input v-model="form.dishIntro" type="textarea" :rows="3" placeholder="菜品介绍（可选）" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.imageUrl" placeholder="图片URL（暂支持直接输入）" />
        </el-form-item>
        <el-form-item v-if="form.imageUrl" label="预览">
          <img :src="form.imageUrl" class="preview-image-block" />
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
import { getDishes, getCategories, createDish, updateDish, deleteDish as apiDeleteDish } from '@/api/dish'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const loading = ref(false)
const allDishes = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterCategory = ref('')
const categoryList = ref([])
const showDialog = ref(false)
const editing = ref(false)

const form = ref({
  dishId: null,
  dishName: '',
  category: '',
  imageUrl: '',
  tags: '',
  cookingTime: null,
  dishIntro: '',
  salePrice: 0,
  status: 'active'
})

const filteredData = computed(() => {
  let result = allDishes.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(d => (d.dishName || '').toLowerCase().includes(q))
  }
  if (filterCategory.value) {
    result = result.filter(d => d.category === filterCategory.value)
  }
  return result
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

async function fetchCategories() {
  try {
    const res = await getCategories({ storeId: currentStoreId.value })
    categoryList.value = res.data || []
  } catch (e) {
    console.error('加载分类失败', e)
    categoryList.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getDishes({ storeId: currentStoreId.value })
    allDishes.value = res.data || []
  } catch (e) {
    console.error('获取菜品列表失败:', e)
    ElMessage.error('加载菜品失败')
  } finally {
    loading.value = false
  }
}

function openAddDish() {
  editing.value = false
  form.value = {
    dishId: null,
    dishName: '',
    category: categoryList.value[0] || '',
    imageUrl: '',
    tags: '',
    cookingTime: null,
    dishIntro: '',
    salePrice: 0,
    status: 'active'
  }
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
  if (!form.value.category) {
    ElMessage.warning('请选择菜品分类')
    return
  }
  if (form.value.salePrice == null || form.value.salePrice < 0) {
    ElMessage.warning('请输入有效售价')
    return
  }
  try {
    if (editing.value && form.value.dishId) {
      await updateDish(form.value.dishId, { ...form.value, storeId: String(currentStoreId.value) }, { params: { storeId: currentStoreId.value } })
    } else {
      await createDish({ ...form.value, storeId: String(currentStoreId.value) })
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    fetchData()
    fetchCategories()
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

async function deleteDish(row) {
  try {
    await ElMessageBox.confirm(`确定删除菜品"${row.dishName}"？`, '确认删除', { type: 'warning' })
    await apiDeleteDish(row.dishId, { params: { storeId: currentStoreId.value } })
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {
    if (e !== 'cancel' && !e.message?.includes('cancel')) {
      console.error('删除失败:', e)
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  await fetchCategories()
  fetchData()
})
</script>

<style scoped>
.dish-library-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.search-input { width: 220px; }
.filter-select { width: 150px; }
.dish-table-wrapper { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.table-thumb { width: 50px; height: 50px; border-radius: 6px; object-fit: cover; }
.thumb-placeholder { width: 50px; height: 50px; border-radius: 6px; background: var(--color-bg-alt); color: var(--color-text-muted); font-size: 14px; font-weight: 600; display: flex; align-items: center; justify-content: center; }
.full-width { width: 100%; }
.pagination-bar { padding: 16px; display: flex; justify-content: flex-end; border-top: 1px solid var(--color-border); }
.preview-image-block { width: 160px; height: 160px; border-radius: 8px; object-fit: cover; border: 1px solid var(--color-border); }
</style>
