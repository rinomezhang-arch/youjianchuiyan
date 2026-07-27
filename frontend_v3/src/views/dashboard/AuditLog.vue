<template>
  <div class="audit-log-view">
    <div class="page-header">
      <h2>审计日志 · Audit Log</h2>
      <div class="header-actions">
        <el-button @click="loadData" :loading="loading">🔄 刷新</el-button>
        <el-button @click="exportLog">📤 导出</el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="filterKeyword"
        placeholder="搜索操作人/内容..."
        clearable
        style="width: 250px"
        @input="handleFilter"
      />
      <el-select v-model="filterAction" placeholder="操作类型" clearable @change="handleFilter">
        <el-option label="创建 · Create" value="create" />
        <el-option label="更新 · Update" value="update" />
        <el-option label="删除 · Delete" value="delete" />
        <el-option label="登录 · Login" value="login" />
        <el-option label="导出 · Export" value="export" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="handleFilter"
      />
    </div>

    <!-- 日志列表 -->
    <el-table :data="filteredLogs" style="width: 100%" v-loading="loading" stripe>
      <el-table-column prop="time" label="时间 · Time" width="180">
        <template #default="{ row }">
          {{ formatTime(row.time) }}
        </template>
      </el-table-column>
      <el-table-column prop="user" label="操作人 · User" width="120" />
      <el-table-column prop="role" label="角色 · Role" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ row.role === 'admin' ? '管理员' : '操作员' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="操作类型 · Action" width="120">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)" size="small">
            {{ actionLabel(row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="target" label="对象 · Target" width="150" />
      <el-table-column prop="detail" label="详情 · Detail" />
    </el-table>

    <!-- 分页 -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="totalLogs"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const filterKeyword = ref('')
const filterAction = ref('')
const dateRange = ref([])
const currentPage = ref(1)
const pageSize = ref(50)

// 从 localStorage 加载审计日志
const loadData = () => {
  loading.value = true
  try {
    const stored = localStorage.getItem('banquet_audit_log')
    if (stored) {
      logs.value = JSON.parse(stored)
    } else {
      logs.value = []
    }
  } catch (e) {
    console.error('加载审计日志失败:', e)
    logs.value = []
  }
  loading.value = false
}

// 过滤后的日志
const filteredLogs = computed(() => {
  let result = [...logs.value]

  // 按关键词过滤
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    result = result.filter(log =>
      (log.user || '').toLowerCase().includes(kw) ||
      (log.detail || '').toLowerCase().includes(kw) ||
      (log.target || '').toLowerCase().includes(kw)
    )
  }

  // 按操作类型过滤
  if (filterAction.value) {
    result = result.filter(log => log.action === filterAction.value)
  }

  // 按日期范围过滤
  if (dateRange.value && dateRange.value.length === 2) {
    const start = new Date(dateRange.value[0]).getTime()
    const end = new Date(dateRange.value[1]).getTime() + 86400000
    result = result.filter(log => {
      const t = new Date(log.time).getTime()
      return t >= start && t < end
    })
  }

  // 按时间倒序
  result.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())

  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  return result.slice(start, start + pageSize.value)
})

const totalLogs = computed(() => {
  let result = [...logs.value]
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    result = result.filter(log =>
      (log.user || '').toLowerCase().includes(kw) ||
      (log.detail || '').toLowerCase().includes(kw) ||
      (log.target || '').toLowerCase().includes(kw)
    )
  }
  if (filterAction.value) {
    result = result.filter(log => log.action === filterAction.value)
  }
  if (dateRange.value && dateRange.value.length === 2) {
    const start = new Date(dateRange.value[0]).getTime()
    const end = new Date(dateRange.value[1]).getTime() + 86400000
    result = result.filter(log => {
      const t = new Date(log.time).getTime()
      return t >= start && t < end
    })
  }
  return result.length
})

const handleFilter = () => {
  currentPage.value = 1
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const actionTagType = (action) => {
  const map = {
    create: 'success',
    update: 'warning',
    delete: 'danger',
    login: 'info',
    export: ''
  }
  return map[action] || ''
}

const actionLabel = (action) => {
  const map = {
    create: '创建',
    update: '更新',
    delete: '删除',
    login: '登录',
    export: '导出'
  }
  return map[action] || action
}

// 导出日志
const exportLog = () => {
  if (logs.value.length === 0) {
    ElMessage.warning('没有可导出的日志')
    return
  }

  const lines = ['时间,操作人,角色,操作类型,对象,详情']
  logs.value.forEach(log => {
    lines.push(`"${formatTime(log.time)}","${log.user}","${log.role}","${log.action}","${log.target}","${log.detail}"`)
  })

  const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `审计日志_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功')
}

// 添加审计日志（供其他组件调用）
const addAudit = (action, target, detail) => {
  const session = JSON.parse(sessionStorage.getItem('banquet_session') || '{}')
  const log = {
    time: new Date().toISOString(),
    user: session.user || '未知',
    role: session.role || 'editor',
    action,
    target,
    detail
  }

  const existing = JSON.parse(localStorage.getItem('banquet_audit_log') || '[]')
  existing.push(log)

  // 最多保留500条
  if (existing.length > 500) {
    existing.splice(0, existing.length - 500)
  }

  localStorage.setItem('banquet_audit_log', JSON.stringify(existing))
}

// 暴露方法
defineExpose({ addAudit, loadData })

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.audit-log-view {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
