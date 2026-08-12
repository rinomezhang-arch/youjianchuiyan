<template>
  <div class="page">
    <div class="page-header">
      
      <h2>菜单管理 · Menu Management</h2>
      <p class="page-desc">菜品管理 · 喜庆菜名 · 英文菜单</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="categoryFilter" placeholder="菜品分类" clearable @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="凉菜" value="凉菜" />
          <el-option label="热菜" value="热菜" />
          <el-option label="汤羹" value="汤羹" />
          <el-option label="海鲜" value="海鲜" />
          <el-option label="点心甜品" value="点心甜品" />
          <el-option label="干锅煲仔" value="干锅煲仔" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 新增菜品</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="dishId" label="编号" width="100" />
      <el-table-column prop="dishName" label="菜品名称" width="140" />
      <el-table-column prop="dishCategory" label="分类" width="80" />
      <el-table-column prop="costPrice" label="成本" width="70" />
      <el-table-column prop="salePrice" label="售价" width="70" />
      <el-table-column prop="costRate" label="成本率" width="70">
        <template #default="{ row }">{{ (row.costRate || 0).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="cookingTime" label="出菜时长" width="80" />
      <el-table-column prop="mainIngredient" label="主料" min-width="120" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="editRow(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />

    <el-dialog v-model="showDialog" :title="editing ? '编辑菜品' : '新增菜品'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="菜品名称" required><el-input v-model="form.dishName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分类">
            <el-select v-model="form.dishCategory"><el-option label="凉菜" value="凉菜" /><el-option label="热菜" value="热菜" /><el-option label="汤羹" value="汤羹" /><el-option label="海鲜" value="海鲜" /><el-option label="点心" value="点心甜品" /><el-option label="干锅" value="干锅煲仔" /></el-select>
          </el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="成本"><el-input-number v-model="form.costPrice" :precision="2" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="售价"><el-input-number v-model="form.salePrice" :precision="2" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="出菜时长"><el-input-number v-model="form.cookingTime" :min="1" />分钟</el-form-item></el-col>
        </el-row>
        <el-form-item label="主料"><el-input v-model="form.mainIngredient" /></el-form-item>
        <el-form-item label="英文名称"><el-input v-model="form.englishName" /></el-form-item>
        <el-collapse>
          <el-collapse-item title="喜庆菜名（8种场合）">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="婚宴"><el-input v-model="form.weddingName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="生日"><el-input v-model="form.birthdayName" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="乔迁"><el-input v-model="form.houseMoveName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="升迁"><el-input v-model="form.promotionName" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="团圆"><el-input v-model="form.reunionName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="答谢"><el-input v-model="form.thanksgivingName" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="尾牙"><el-input v-model="form.yearEndName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="满月"><el-input v-model="form.babyBornName" /></el-form-item></el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
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
import { getDishes, createDish, updateDish } from '@/api/booking'
import { ElMessage } from 'element-plus'

const loading = ref(false); const list = ref([]); const total = ref(0)
const keyword = ref(''); const categoryFilter = ref(''); const showDialog = ref(false); const editing = ref(false)
const form = ref({ dishName: '', dishCategory: '热菜', costPrice: 0, salePrice: 0, cookingTime: 15, mainIngredient: '', englishName: '', birthdayName: '', weddingName: '', houseMoveName: '', promotionName: '', reunionName: '', thanksgivingName: '', yearEndName: '', babyBornName: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getDishes({ category: categoryFilter.value, keyword: keyword.value })
    if (res.code === 200) list.value = res.data?.content || res.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() { editing.value = false; form.value = { dishName: '', dishCategory: '热菜', costPrice: 0, salePrice: 0, cookingTime: 15, mainIngredient: '', englishName: '' }; showDialog.value = true }
function editRow(row) { editing.value = true; form.value = { ...row }; showDialog.value = true }

async function saveDish() {
  const res = editing.value ? await updateDish(form.value.dishId, form.value) : await createDish(form.value)
  if (res.code === 200) { ElMessage.success('保存成功'); showDialog.value = false; fetchData() }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }
</style>


