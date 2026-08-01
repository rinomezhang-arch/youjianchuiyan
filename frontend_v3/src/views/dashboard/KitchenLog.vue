<template>
  <div class="kitchen-log-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">后厨日志</h1>
        <span class="page-desc">Kitchen Log · 出菜记录 · 工时统计</span>
      </div>
      <el-button type="primary" @click="showAddDialog = true">新增日志</el-button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-value">{{ todayCount }}</span>
        <span class="stat-label">今日日志</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ weekCount }}</span>
        <span class="stat-label">本周日志</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ operationCount }}</span>
        <span class="stat-label">操作次数</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ logTotal }}</span>
        <span class="stat-label">日志总数</span>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="全部类型" clearable style="width:120px">
        <el-option label="出菜" value="出菜" />
        <el-option label="预订" value="预订" />
        <el-option label="沽清" value="沽清" />
        <el-option label="备菜中" value="备菜中" />
      </el-select>
      <el-input v-model="searchKeyword" placeholder="搜索订单号/菜名/操作员..." style="width:240px" clearable />
      <el-button @click="doSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="filteredList" stripe class="data-table" v-loading="loading">
      <el-table-column prop="logTime" label="时间" width="180" />
      <el-table-column prop="operation" label="操作" width="100">
        <template #default="{ row }">
          <el-tag :type="operationTag(row.operation)" size="small" effect="plain">{{ row.operation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="80" />
      <el-table-column prop="orderNo" label="预订号" width="160" />
      <el-table-column prop="dishName" label="菜品" min-width="120" />
      <el-table-column prop="operator" label="操作员" width="100" />
      <el-table-column prop="remark" label="备注" min-width="150" />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条</span>
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredList.length"
        layout="prev, pager, next"
        small
      />
    </div>

    <!-- 新增日志对话框 -->
    <el-dialog v-model="showAddDialog" title="新增后厨日志" width="500px">
      <el-form :model="logForm" label-width="80px">
        <el-form-item label="操作类型">
          <el-select v-model="logForm.operation" style="width:100%">
            <el-option label="出菜" value="出菜" />
            <el-option label="预订" value="预订" />
            <el-option label="沽清" value="沽清" />
            <el-option label="备菜中" value="备菜中" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="logForm.type" style="width:100%">
            <el-option label="菜品" value="菜品" />
            <el-option label="预订" value="预订" />
          </el-select>
        </el-form-item>
        <el-form-item label="预订号">
          <el-input v-model="logForm.orderNo" />
        </el-form-item>
        <el-form-item label="菜品名称">
          <el-input v-model="logForm.dishName" />
        </el-form-item>
        <el-form-item label="操作员">
          <el-input v-model="logForm.operator" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="logForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const filterType = ref('')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const showAddDialog = ref(false)

const logForm = ref({
  operation: '出菜',
  type: '菜品',
  orderNo: '',
  dishName: '',
  operator: '',
  remark: ''
})

const todayCount = computed(() => {
  const today = new Date().toDateString()
  return logs.value.filter(l => new Date(l.logTime).toDateString() === today).length
})

const weekCount = computed(() => {
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  return logs.value.filter(l => new Date(l.logTime).getTime() > weekAgo).length
})

const operationCount = computed(() => logs.value.length)
const logTotal = computed(() => logs.value.length)

const filteredList = computed(() => {
  let list = logs.value
  if (filterType.value) list = list.filter(l => l.operation === filterType.value)
  if (searchKeyword.value) {
    const q = searchKeyword.value.toLowerCase()
    list = list.filter(l =>
      (l.orderNo || '').toLowerCase().includes(q) ||
      (l.dishName || '').toLowerCase().includes(q) ||
      (l.operator || '').toLowerCase().includes(q)
    )
  }
  return list
})

function operationTag(op) {
  return { '出菜': 'success', '预订': '', '沽清': 'danger', '备菜中': 'warning' }[op] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetch('/menu-api/kitchen-logs').then(r => r.json())
    if (res.code === 200) {
      logs.value = res.data?.list || res.data || []
    }
  } catch {
    logs.value = [
      { logTime: '2026-07-29 03:22:33', operation: '出菜', type: '菜品', orderNo: 'BK20260729032225', dishName: 'TEST', operator: 'rino', remark: 'xsmoke' },
      { logTime: '2026-07-29 03:22:33', operation: '出菜', type: '菜品', orderNo: 'BK1785266352763', dishName: '更新菜名', operator: '1', remark: 'iPad 下单到厨房' },
      { logTime: '2026-07-29 03:10:45', operation: '出菜', type: '菜品', orderNo: 'BK1785160326439', dishName: 'SMOKE_TEST', operator: 'rino', remark: 'SMOKE_TEST' },
      { logTime: '2026-07-03 17:55:17', operation: '预订', type: '预订', orderNo: 'BK2026070317098', dishName: '-', operator: '-', remark: '状态: 已完成' },
      { logTime: '2026-07-03 17:55:17', operation: '备菜中', type: '预订', orderNo: 'BK2026070317098', dishName: '-', operator: '-', remark: '状态: 备菜中' },
      { logTime: '2026-07-03 17:55:16', operation: '备菜中', type: '预订', orderNo: 'BK20260703272A', dishName: '-', operator: '-', remark: '状态: 已完成' },
      { logTime: '2026-07-03 17:55:14', operation: '备菜中', type: '预订', orderNo: 'BK20260703272A', dishName: '-', operator: '-', remark: '状态: 备菜中' },
      { logTime: '2026-07-03 17:55:23', operation: '备菜中', type: '预订', orderNo: 'BK202607024034', dishName: '-', operator: '-', remark: '状态: 备菜中' },
    ]
  } finally {
    loading.value = false
  }
}

function doSearch() { currentPage.value = 1 }
function resetFilter() { filterType.value = ''; searchKeyword.value = '' }

function saveLog() {
  const now = new Date()
  const timeStr = now.toISOString().replace('T', ' ').substring(0, 19)
  logs.value.unshift({
    logTime: timeStr,
    ...logForm.value
  })
  ElMessage.success('日志已添加')
  showAddDialog.value = false
  logForm.value = { operation: '出菜', type: '菜品', orderNo: '', dishName: '', operator: '', remark: '' }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.kitchen-log-page { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }
.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 13px; color: var(--color-text-secondary); }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.data-table { border-radius: 2px; overflow: hidden; }
.table-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } }
</style>
