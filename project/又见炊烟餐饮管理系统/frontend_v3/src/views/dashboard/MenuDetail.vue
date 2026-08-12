<template>
  <BizPageWrapper
    title="菜单详情 · Menu Detail"
    subtitle="点菜结果 · 菜品明细 · Order Results · Dish Details"
    icon="️"
    :has-content="hasData"
    empty-title="暂未点菜"
    empty-desc="请在桌台看板打开预订并点菜"
  >
    <!-- 筛选 -->
    <div class="filter-bar">
      <el-date-picker v-model="filterDate" type="date" placeholder="选择日期" style="width:160px" />
      <el-select v-model="filterPeriod" placeholder="时段" style="width:120px">
        <el-option label="☀️ 午餐" value="lunch" />
        <el-option label="🌙 晚餐" value="dinner" />
      </el-select>
      <el-input v-model="filterTable" placeholder="桌台名" style="width:160px" clearable />
      <el-button type="primary" size="small" @click="loadData">🔍 查询</el-button>
    </div>

    <!-- 桌台卡片 -->
    <div class="table-cards" v-if="tableOrders.length > 0">
      <div v-for="item in tableOrders" :key="item.key" class="table-card">
        <div class="card-header">
          <span class="card-table">🪑 {{ item.tableName }}</span>
          <span class="card-info">{{ item.date }} · {{ item.period }}</span>
          <el-tag size="small" type="warning">{{ item.totalCount }}道</el-tag>
          <el-tag size="small" type="success">¥{{ item.totalPrice }}</el-tag>
        </div>
        <div class="card-body">
          <div v-for="(d, i) in item.dishes" :key="i" class="dish-row">
            <span class="dish-seq">{{ i+1 }}</span>
            <span class="dish-name">{{ d.dishName }}</span>
            <span class="dish-qty">×{{ d.qty }}</span>
            <span class="dish-price">¥{{ d.price * d.qty }}</span>
            <span v-if="d.remark" class="dish-remark">📝 {{ d.remark }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span>总价：<b>¥{{ item.totalPrice }}</b></span>
          <span>道数：{{ item.totalCount }}</span>
        </div>
      </div>
    </div>
  </BizPageWrapper>
</template>

<script setup>
import { ref, computed } from 'vue'
import BizPageWrapper from '@/components/BizPageWrapper.vue'
import { getOrders, getDishes } from '@/utils/menuStore'

const filterDate = ref(new Date())
const filterPeriod = ref('dinner')
const filterTable = ref('')
const allData = ref([])

const tableOrders = computed(() => allData.value)

const hasData = computed(() => allData.value.length > 0)

// 缓存菜品映射
let dishMap = {}

function loadData() {
  const d = filterDate.value
  const date = d instanceof Date ? d.toISOString().slice(0, 10) : String(d)
  const period = filterPeriod.value

  // 从内存缓存读菜品数据
  try {
    const dishes = getDishes()
    dishMap = {}
    dishes.forEach(d => { dishMap[d.id] = d })
  } catch(e) {}

  const orders = getOrders()
  const result = []

  Object.entries(orders).forEach(([key, items]) => {
    const [datePart, periodPart, ...tableParts] = key.split('_')
    const tableName = tableParts.join('_')

    // 筛选
    if (datePart !== date) return
    if (periodPart !== period) return
    if (filterTable.value && !tableName.includes(filterTable.value)) return

    const enriched = items.map(item => {
      const d = dishMap[item.dishCode] || {}
      return {
        ...item,
        dishName: d.name || item.dishCode,
        price: d.price || 0
      }
    })

    result.push({
      key,
      date: datePart,
      period: periodPart === 'lunch' ? '午餐' : '晚餐',
      tableName,
      dishes: enriched,
      totalCount: enriched.reduce((s, d) => s + (d.qty||1), 0),
      totalPrice: enriched.reduce((s, d) => s + (d.price||0)*(d.qty||1), 0)
    })
  })

  allData.value = result
}

// 初始化加载
loadData()
</script>

<style scoped>
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }

.table-cards { display: flex; flex-direction: column; gap: 12px; }
.table-card {
  border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;
  background: #fff; transition: 0.2s;
}
.table-card:hover { border-color: #c4b5fd; box-shadow: 0 2px 8px rgba(124,58,237,0.1); }

.card-header {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  background: #f8f5ff; border-bottom: 1px solid #ede9fe;
}
.card-table { font-size: 15px; font-weight: 600; color: #7c3aed; }
.card-info { font-size: 12px; color: #6d28d9; flex: 1; }

.card-body { padding: 4px 16px; }
.dish-row {
  display: flex; align-items: center; gap: 10px; padding: 8px 0;
  border-bottom: 1px solid #f1f5f9; font-size: 13px;
}
.dish-row:last-child { border-bottom: none; }
.dish-seq { width: 22px; height: 22px; border-radius: 50%; background: #ede9fe; color: #7c3aed; font-size: 10px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.dish-name { flex: 1; color: #1e293b; }
.dish-qty { color: #64748b; }
.dish-price { color: #7c3aed; font-weight: 500; }
.dish-remark { font-size: 11px; color: #dc2626; }

.card-footer {
  display: flex; justify-content: space-between; padding: 8px 16px;
  background: #fafbff; border-top: 1px solid #ede9fe; font-size: 13px; color: #6d28d9;
}
.card-footer b { color: #7c3aed; }
</style>
