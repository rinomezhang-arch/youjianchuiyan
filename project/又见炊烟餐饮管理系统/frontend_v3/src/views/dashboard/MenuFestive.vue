<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>节日菜单 · Festive Menu</h2>
        <p class="page-desc">节日喜庆菜名管理 · Festive Dish Names</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="festiveType" placeholder="喜庆场合" clearable @change="fetchData" style="width:160px">
          <el-option label="全部" value="" />
          <el-option label="婚宴" value="weddingName" />
          <el-option label="生日宴" value="birthdayName" />
          <el-option label="乔迁宴" value="houseMoveName" />
          <el-option label="升迁宴" value="promotionName" />
          <el-option label="团圆宴" value="reunionName" />
          <el-option label="答谢宴" value="thanksgivingName" />
          <el-option label="尾牙宴" value="yearEndName" />
          <el-option label="满月宴" value="babyBornName" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品/喜庆名" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
    </div>

    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="dishId" label="编号" width="110" />
      <el-table-column prop="dishName" label="菜品名称" min-width="130" />
      <el-table-column prop="festiveName" label="喜庆菜名" min-width="160">
        <template #default="{ row }">
          <span v-if="row.festiveName" style="color:var(--color-accent);font-weight:600">{{ row.festiveName }}</span>
          <span v-else style="color:var(--color-text-muted)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="婚宴" width="120">
        <template #default="{ row }">{{ row.weddingName || '-' }}</template>
      </el-table-column>
      <el-table-column label="生日" width="120">
        <template #default="{ row }">{{ row.birthdayName || '-' }}</template>
      </el-table-column>
      <el-table-column label="乔迁" width="120">
        <template #default="{ row }">{{ row.houseMoveName || '-' }}</template>
      </el-table-column>
      <el-table-column label="升迁" width="120">
        <template #default="{ row }">{{ row.promotionName || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const keyword = ref('')
const festiveType = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await fetch(`/api/dishes?storeId=1&hasFestive=true&festiveType=${festiveType.value}&keyword=${keyword.value}`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) {
      list.value = data.data?.content || data.data || []
      total.value = list.value.length
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
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
