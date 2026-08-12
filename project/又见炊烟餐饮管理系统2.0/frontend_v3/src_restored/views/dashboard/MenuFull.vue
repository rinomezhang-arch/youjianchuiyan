<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>总菜单 · Full Menu</h2>
        <p class="page-desc">全部菜品总览 · Complete Dish Overview</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="categoryFilter" placeholder="菜品分类" clearable @change="fetchData" style="width:160px">
          <el-option label="全部" value="" />
          <el-option label="本店招牌" value="本店招牌" />
          <el-option label="干锅煲仔" value="干锅煲仔" />
          <el-option label="水产海鲜" value="水产海鲜" />
          <el-option label="热菜小炒" value="热菜小炒" />
          <el-option label="美味汤羹" value="美味汤羹" />
          <el-option label="田园时蔬" value="田园时蔬" />
          <el-option label="凉菜刺身" value="凉菜刺身" />
          <el-option label="健康主食" value="健康主食" />
        </el-select>
        <el-select v-model="usageFilter" placeholder="用途" clearable @change="fetchData" style="width:140px">
          <el-option label="全部" value="" />
          <el-option label="宴会" value="banquet" />
          <el-option label="零点" value="a_la_carte" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 新增菜品</el-button>
      </div>
    </div>

    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="dishId" label="编号" width="110" />
      <el-table-column prop="dishName" label="菜品名称" min-width="160" />
      <el-table-column prop="dishCategory" label="分类" width="100" />
      <el-table-column prop="costPrice" label="成本" width="70">
        <template #default="{ row }">¥{{ row.costPrice }}</template>
      </el-table-column>
      <el-table-column prop="salePrice" label="售价" width="70">
        <template #default="{ row }">¥{{ row.salePrice }}</template>
      </el-table-column>
      <el-table-column prop="costRate" label="成本率" width="70">
        <template #default="{ row }">{{ (row.costRate || 0).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="cookingTime" label="出菜时长" width="90">
        <template #default="{ row }">{{ row.cookingTime }}分钟</template>
      </el-table-column>
      <el-table-column prop="mainIngredient" label="主料" min-width="120" />
      <el-table-column prop="festiveName" label="喜庆名" width="140">
        <template #default="{ row }">
          <span v-if="row.festiveName" style="color:var(--color-accent)">{{ row.festiveName }}</span>
          <span v-else style="color:var(--color-text-muted)">-</span>
        </template>
      </el-table-column>
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
            <el-select v-model="form.dishCategory">
              <el-option label="本店招牌" value="本店招牌" />
              <el-option label="干锅煲仔" value="干锅煲仔" />
              <el-option label="水产海鲜" value="水产海鲜" />
              <el-option label="热菜小炒" value="热菜小炒" />
              <el-option label="美味汤羹" value="美味汤羹" />
              <el-option label="田园时蔬" value="田园时蔬" />
              <el-option label="凉菜刺身" value="凉菜刺身" />
              <el-option label="健康主食" value="健康主食" />
            </el-select>
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
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const keyword = ref('')
const categoryFilter = ref('')
const usageFilter = ref('')
const showDialog = ref(false)
const editing = ref(false)
const form = ref({})

async function fetchData() {
  loading.value = true
  try {
    const res = await fetch(`/api/dishes?storeId=1&category=${categoryFilter.value}&usage=${usageFilter.value}&keyword=${keyword.value}`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) {
      list.value = data.data?.content || data.data || []
      total.value = data.data?.totalElements || list.value.length
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() {
  editing.value = false
  form.value = { dishName: '', dishCategory: '热菜小炒', costPrice: 0, salePrice: 0, cookingTime: 15, mainIngredient: '', englishName: '', weddingName: '', birthdayName: '', houseMoveName: '', promotionName: '', reunionName: '', thanksgivingName: '', yearEndName: '', babyBornName: '' }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveDish() {
  try {
    const url = editing.value ? `/api/dishes/${form.value.dishId}` : '/api/dishes'
    const method = editing.value ? 'PUT' : 'POST'
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(form.value)
    })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('保存成功')
      showDialog.value = false
      fetchData()
    }
  } catch (e) { console.error(e) }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-muted); margin: 2px 0 0; }
.back-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 14px; border: 1px solid var(--color-border);
  background: var(--color-card); color: var(--color-text);
  border-radius: 2px; cursor: pointer; font-size: 13px;
  transition: all 0.2s;
}
.back-btn:hover { background: var(--color-bg-alt); color: var(--color-primary); border-color: var(--color-accent); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.search-box { width: 200px; }
.mt-4 { margin-top: 16px; }
</style>
