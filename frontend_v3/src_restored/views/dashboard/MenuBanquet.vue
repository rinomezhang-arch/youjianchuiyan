<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>宴会菜单 · Banquet Menu</h2>
        <p class="page-desc">宴会套餐菜品管理 · Banquet Set Menu Management</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="banquetType" placeholder="宴会类型" clearable @change="fetchData" style="width:160px">
          <el-option label="全部" value="" />
          <el-option label="宴会套餐" value="banquet_set" />
          <el-option label="宴会冷盘" value="banquet_cold" />
          <el-option label="宴会热菜" value="banquet_hot" />
          <el-option label="宴会汤羹" value="banquet_soup" />
          <el-option label="宴会主食" value="banquet_staple" />
          <el-option label="宴会甜品" value="banquet_dessert" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索菜品" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
    </div>

    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="dishId" label="编号" width="110" />
      <el-table-column prop="dishName" label="菜品名称" min-width="160" />
      <el-table-column prop="dishCategory" label="分类" width="100" />
      <el-table-column prop="salePrice" label="售价" width="80">
        <template #default="{ row }">¥{{ row.salePrice }}</template>
      </el-table-column>
      <el-table-column prop="tags" label="宴会类型" width="120">
        <template #default="{ row }">
          <el-tag v-for="t in row.banquetTags" :key="t" size="small" style="margin:2px">{{ t }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cookingTime" label="出菜时长" width="90">
        <template #default="{ row }">{{ row.cookingTime }}分钟</template>
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
const banquetType = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await fetch(`/api/dishes?storeId=1&tagType=banquet&keyword=${keyword.value}`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) {
      const dishes = data.data?.content || data.data || []
      list.value = dishes
      total.value = dishes.length
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
