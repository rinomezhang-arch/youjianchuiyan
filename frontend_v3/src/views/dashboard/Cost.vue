<template>
  <div class="cost-page">
    <div class="sub-header">
      <div>
        <h2>成本分析 · Cost Analysis</h2>
        <p class="page-desc">菜品成本率排行 · 毛利率 · 利润分析</p>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="scard">
        <div class="sc-label">菜品总数</div>
        <div class="sc-val">{{ dishTotal }}</div>
      </div>
      <div class="scard">
        <div class="sc-label">已配成本</div>
        <div class="sc-val text-success">{{ costedCount }}</div>
      </div>
      <div class="scard">
        <div class="sc-label">平均成本率</div>
        <div class="sc-val" :class="avgCostRate > 40 ? 'text-danger' : 'text-primary'">{{ avgCostRate.toFixed(1) }}%</div>
      </div>
      <div class="scard">
        <div class="sc-label">平均毛利率</div>
        <div class="sc-val" :class="avgMargin > 60 ? 'text-success' : 'text-warning'">{{ avgMargin.toFixed(1) }}%</div>
      </div>
      <div class="scard">
        <div class="sc-label">最高成本率</div>
        <div class="sc-val text-danger">{{ maxCostRate.toFixed(1) }}%</div>
      </div>
      <div class="scard">
        <div class="sc-label">理论总毛利</div>
        <div class="sc-val text-success">¥{{ totalProfit.toFixed(0) }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="cost-toolbar">
      <el-select v-model="catFilter" placeholder="全部分类" clearable size="small" style="width:130px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input v-model="search" placeholder="搜索菜品" size="small" clearable style="width:180px" />
      <span style="flex:1" />
      <el-radio-group v-model="sortBy" size="small">
        <el-radio-button value="rate">按成本率</el-radio-button>
        <el-radio-button value="profit">按毛利</el-radio-button>
        <el-radio-button value="price">按售价</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="loading text-danger">{{ error }}</div>

    <el-table v-else :data="list" stripe size="small" max-height="calc(100vh - 340px)" row-key="dishId">
      <el-table-column type="index" width="40" />
      <el-table-column prop="dishName" label="菜品名称" min-width="140" />
      <el-table-column prop="dishCategory" label="分类" width="90" />
      <el-table-column label="成本" width="85">
        <template #default="{ row }">¥{{ (row.costPrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="售价" width="85">
        <template #default="{ row }">¥{{ (row.salePrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="成本率" width="150">
        <template #default="{ row }">
          <div class="rate-bar-wrap">
            <div class="rate-bar" :style="{ width: Math.min((row.costRate||0), 100) + '%', background: barColor(row.costRate || 0) }" />
            <span class="rate-text" :style="{ color: barColor(row.costRate || 0) }">{{ (row.costRate || 0).toFixed(1) }}%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="毛利率" width="80">
        <template #default="{ row }">
          <span :style="{ color: marginColor(100 - (row.costRate||0)) }">{{ (100 - (row.costRate || 0)).toFixed(0) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="毛利" width="85">
        <template #default="{ row }">¥{{ profit(row).toFixed(2) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import request from '@/utils/request'

// ── API 函数 ──
function getCostSummary() {
  return request({ url: '/api/cost/summary', method: 'get' })
}
function getCostRanking(params) {
  return request({ url: '/api/cost/ranking', method: 'get', params })
}
function getCostCategories() {
  return request({ url: '/api/cost/categories', method: 'get' })
}

// ── 状态 ──
const loading = ref(false)
const error = ref('')
const list = ref([])
const categories = ref([])
const search = ref('')
const catFilter = ref('')
const sortBy = ref('rate')

// ── 汇总数据（来自 API）──
const dishTotal = ref(0)
const costedCount = ref(0)
const avgCostRate = ref(0)
const avgMargin = ref(0)
const maxCostRate = ref(0)
const totalProfit = ref(0)

// ── 工具函数 ──
function profit(row) { return (row.salePrice || 0) - (row.costPrice || 0) }
function barColor(rate) { if (rate > 45) return '#dc2626'; if (rate > 38) return '#f59e0b'; return '#22c55e' }
function marginColor(m) { if (m >= 62) return '#22c55e'; if (m >= 55) return '#f59e0b'; return '#dc2626' }

// ── 数据加载 ──
async function fetchSummary() {
  try {
    const res = await getCostSummary()
    if (res.code === 200 && res.data) {
      dishTotal.value = res.data.dishTotal ?? 0
      costedCount.value = res.data.costedCount ?? 0
      avgCostRate.value = res.data.avgCostRate ?? 0
      avgMargin.value = res.data.avgMargin ?? 0
      maxCostRate.value = res.data.maxCostRate ?? 0
      totalProfit.value = res.data.totalProfit ?? 0
    }
  } catch (e) {
    console.error('获取成本汇总失败:', e)
  }
}

async function fetchCategories() {
  try {
    const res = await getCostCategories()
    if (res.code === 200) {
      categories.value = res.data || []
    }
  } catch (e) {
    console.error('获取分类列表失败:', e)
  }
}

async function fetchRanking() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (catFilter.value) params.category = catFilter.value
    if (search.value.trim()) params.search = search.value.trim()
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await getCostRanking(params)
    if (res.code === 200) {
      list.value = res.data?.content || res.data || []
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (e) {
    console.error('获取成本排行失败:', e)
    error.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function fetchAll() {
  await Promise.all([fetchSummary(), fetchCategories(), fetchRanking()])
}

// ── 筛选变化时重新拉取排行 ──
watch([catFilter, sortBy], () => fetchRanking())
// 搜索防抖：简单延迟触发
let searchTimer = null
watch(search, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => fetchRanking(), 400)
})

onMounted(fetchAll)
</script>

<style scoped>
.cost-page { padding: 16px; }
.sub-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
.sub-header h2 { font-size: 18px; margin: 0; }
.page-desc { font-size: 12px; color: #9ca3af; margin: 2px 0 0; }
.summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin-top: 16px; margin-bottom: 14px; }
.scard { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px 16px; }
.sc-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.sc-val { font-size: 24px; font-weight: 700; }
.text-success { color: #16a34a; }
.text-danger { color: #dc2626; }
.text-warning { color: #f59e0b; }
.text-primary { color: #3b82f6; }
.cost-toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
.loading { text-align: center; padding: 40px; color: #9ca3af; }
.rate-bar-wrap { display: flex; align-items: center; gap: 4px; width: 100%; }
.rate-bar { height: 6px; border-radius: 3px; min-width: 2px; }
.rate-text { font-size: 11px; font-weight: 600; white-space: nowrap; }
</style>
