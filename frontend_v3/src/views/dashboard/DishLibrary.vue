<template>
  <div class="dish-library-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">菜库编辑 · Dish Library</h2>
        <p class="page-subtitle">菜品信息管理 · 分类维护 · 图片管理</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品名称..." clearable class="search-input" @change="fetchData" />
        <el-select v-model="filterCategory" placeholder="全部分类" clearable class="filter-select" @change="fetchData">
          <el-option v-for="cat in categoryList" :key="cat.caipinleixing" :label="cat.caipinleixing" :value="cat.caipinleixing" />
        </el-select>
        <el-button type="primary" @click="openAddDish">+ 新增菜品</el-button>
      </div>
    </div>

    <div class="dish-table-wrapper">
      <el-table :data="tableData" stripe v-loading="loading" class="dish-table">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.tupian" :src="row.tupian" class="table-thumb" />
            <div v-else class="thumb-placeholder">{{ (row.caipinmingcheng || '菜').charAt(0) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="caipinmingcheng" label="菜品名称" min-width="150" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.caipinleixing }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100">
          <template #default="{ row }">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="kouwei" label="口味" width="100" />
        <el-table-column prop="yujishijian" label="预计时间" width="100" />
        <el-table-column label="点击量" width="90">
          <template #default="{ row }">{{ row.clicktime ? '已点' : '-' }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ row.fabushijian || row.addtime }}</template>
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
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :current-page="currentPage"
          @size-change="(s) => { pageSize = s; currentPage = 1; fetchData() }"
          @current-change="(p) => { currentPage = p; fetchData() }"
        />
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="editing ? '编辑菜品' : '新增菜品'" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜品名称" required>
              <el-input v-model="form.caipinmingcheng" placeholder="请输入菜品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜品分类" required>
              <el-select v-model="form.caipinleixing" placeholder="选择分类" class="full-width">
                <el-option v-for="cat in categoryList" :key="cat.caipinleixing" :label="cat.caipinleixing" :value="cat.caipinleixing" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="售价" required>
              <el-input-number v-model="form.price" :precision="2" :min="0" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="口味">
              <el-input v-model="form.kouwei" placeholder="如：微辣" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计时间">
              <el-input v-model="form.yujishijian" placeholder="如：15分钟" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="菜品介绍">
          <el-input v-model="form.caipinjieshao" type="textarea" :rows="3" placeholder="菜品介绍（可选）" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.tupian" placeholder="图片URL（暂支持直接输入）" />
        </el-form-item>
        <el-form-item v-if="form.tupian" label="预览">
          <img :src="form.tupian" class="preview-image-block" />
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
import { ref, onMounted } from 'vue'
import {
  btDishPage,
  btDishSave,
  btDishUpdate,
  btDishDelete,
  btDishTypeList
} from '@/api/dish'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filterCategory = ref('')
const categoryList = ref([])
const showDialog = ref(false)
const editing = ref(false)

const form = ref({
  id: null,
  caipinmingcheng: '',
  caipinleixing: '',
  tupian: '',
  kouwei: '',
  yujishijian: '',
  caipinjieshao: '',
  price: 0,
  storeId: null
})

async function fetchCategories() {
  try {
    const res = await btDishTypeList()
    categoryList.value = res.data || res || []
  } catch (e) {
    console.warn('加载分类失败，使用默认分类', e)
    categoryList.value = [
      { caipinleixing: '凉菜' },
      { caipinleixing: '热菜' },
      { caipinleixing: '汤羹' },
      { caipinleixing: '主食' },
      { caipinleixing: '点心' },
      { caipinleixing: '饮品' }
    ]
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      limit: pageSize.value,
      caipinmingcheng: searchQuery.value || undefined,
      caipinleixing: filterCategory.value || undefined
    }
    const res = await btDishPage(params)
    const data = res.data || {}
    tableData.value = data.list || []
    total.value = data.total || 0
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
    id: null,
    caipinmingcheng: '',
    caipinleixing: categoryList.value[0]?.caipinleixing || '',
    tupian: '',
    kouwei: '',
    yujishijian: '',
    caipinjieshao: '',
    price: 0,
    storeId: Number(localStorage.getItem('currentStoreId') || localStorage.getItem('storeId') || 1)
  }
  showDialog.value = true
}

function editDish(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveDish() {
  if (!form.value.caipinmingcheng) {
    ElMessage.warning('请输入菜品名称')
    return
  }
  if (!form.value.caipinleixing) {
    ElMessage.warning('请选择菜品分类')
    return
  }
  if (form.value.price == null || form.value.price < 0) {
    ElMessage.warning('请输入有效售价')
    return
  }
  try {
    if (editing.value && form.value.id) {
      await btDishUpdate(form.value)
    } else {
      await btDishSave(form.value)
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    fetchData()
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  }
}

async function deleteDish(row) {
  try {
    await ElMessageBox.confirm(`确定删除菜品“${row.caipinmingcheng}”？`, '确认删除', { type: 'warning' })
    await btDishDelete([row.id])
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {
    if (e !== 'cancel' && !e.message?.includes('cancel')) {
      console.error('删除失败:', e)
      ElMessage.error('删除失败')
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
