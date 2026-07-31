<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>沽清内容 · Sold Out Items</h2>
        <p class="page-desc">今日沽清菜品管理 · Daily Sold Out Management</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker v-model="filterDate" type="date" placeholder="选择日期" style="width:160px" @change="fetchData" />
        <el-select v-model="filterCategory" placeholder="分类" clearable @change="fetchData" style="width:140px">
          <el-option label="全部" value="" />
          <el-option label="宴会" value="banquet" />
          <el-option label="零点" value="a_la_carte" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 添加沽清</el-button>
      </div>
    </div>

    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="dishId" label="编号" width="110" />
      <el-table-column prop="dishName" label="菜品名称" min-width="160" />
      <el-table-column prop="dishCategory" label="分类" width="100" />
      <el-table-column prop="salePrice" label="售价" width="80">
        <template #default="{ row }">¥{{ row.salePrice }}</template>
      </el-table-column>
      <el-table-column prop="soldOutDate" label="沽清日期" width="120" />
      <el-table-column prop="soldOutReason" label="沽清原因" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isRestored ? 'success' : 'danger'" size="small">
            {{ row.isRestored ? '已恢复' : '沽清中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.isRestored" text size="small" type="success" @click="restoreDish(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />

    <el-dialog v-model="showDialog" title="添加沽清" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="选择菜品" required>
          <el-select v-model="form.dishId" filterable placeholder="搜索菜品" style="width:100%">
            <el-option v-for="d in allDishes" :key="d.dishId" :label="d.dishName" :value="d.dishId" />
          </el-select>
        </el-form-item>
        <el-form-item label="沽清日期">
          <el-date-picker v-model="form.soldOutDate" type="date" style="width:100%" />
        </el-form-item>
        <el-form-item label="沽清原因">
          <el-input v-model="form.soldOutReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSoldOut">保存</el-button>
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
const filterDate = ref(new Date())
const filterCategory = ref('')
const showDialog = ref(false)
const allDishes = ref([])
const form = ref({ dishId: '', soldOutDate: '', soldOutReason: '' })

async function fetchData() {
  loading.value = true
  try {
    const d = filterDate.value
    const dateStr = d instanceof Date ? d.toISOString().slice(0, 10) : String(d)
    const res = await fetch(`/api/dishes/soldout?date=${dateStr}&category=${filterCategory.value}&keyword=${keyword.value}`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) {
      list.value = data.data?.content || data.data || []
      total.value = list.value.length
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function fetchAllDishes() {
  try {
    const res = await fetch('/api/dishes?storeId=1&pageSize=999', { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) allDishes.value = data.data?.content || data.data || []
  } catch (e) {}
}

function openAdd() { showDialog.value = true }

async function saveSoldOut() {
  try {
    const res = await fetch('/api/dishes/soldout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(form.value)
    })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('添加成功')
      showDialog.value = false
      fetchData()
    }
  } catch (e) { console.error(e) }
}

async function restoreDish(row) {
  try {
    const res = await fetch(`/api/dishes/soldout/${row.dishId}/restore`, {
      method: 'POST', credentials: 'include'
    })
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('已恢复')
      fetchData()
    }
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchData(); fetchAllDishes() })
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
