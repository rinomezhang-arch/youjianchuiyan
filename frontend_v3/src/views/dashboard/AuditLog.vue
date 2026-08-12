<template>
  <div class="audit-log-view">
    <div class="page-header">
      <h2>审计日志 · Audit Log</h2>
      <div class="header-actions">
        <el-button @click="loadData" :loading="loading">🔄 刷新</el-button>
        <el-button @click="exportLog" :loading="exporting">📤 导出</el-button>
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
import request from '@/utils/request'

// ============================================================
// API 函数
// ============================================================

/**
 * 获取审计日志列表（支持分页、筛选、日期范围）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 当前页码
 * @param {number} params.pageSize - 每页条数
 * @param {string} [params.keyword] - 关键词（操作人/内容）
 * @param {string} [params.action] - 操作类型
 * @param {string} [params.startDate] - 开始日期 YYYY-MM-DD
 * @param {string} [params.endDate] - 结束日期 YYYY-MM-DD
 */
const getAuditLogs = (params) => {
  return request({
    url: '/api/audit/logs',
    method: 'get',
    params
  })
}

/**
 * 导出审计日志（返回文件流或下载链接）
 * @param {Object} params - 查询参数（同 getAuditLogs，不含分页）
 */
const exportAuditLogs = (params) => {
  return request({
    url: '/api/audit/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// ============================================================
// 响应式状态
// ============================================================

const logs = ref([])
const loading = ref(false)
const exporting = ref(false)
const filterKeyword = ref('')
const filterAction = ref('')
const dateRange = ref([])
const currentPage = ref(1)
const pageSize = ref(50)
const totalLogs = ref(0)

// ============================================================
// 数据加载
// ============================================================

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterKeyword.value) {
      params.keyword = filterKeyword.value
    }
    if (filterAction.value) {
      params.action = filterAction.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getAuditLogs(params)

    // 兼容两种常见返回格式：
    // 1) { data: { list: [...], total: N } }
    // 2) { list: [...], total: N }（request 已解包 data）
    const payload = res?.data ?? res
    logs.value = payload?.list ?? payload?.rows ?? []
    totalLogs.value = payload?.total ?? logs.value.length ?? 0
  } catch (e) {
    console.error('加载审计日志失败:', e)
    ElMessage.error('加载审计日志失败，请稍后重试')
    logs.value = []
    totalLogs.value = 0
  } finally {
    loading.value = false
  }
}

// ============================================================
// 前端二次过滤（服务端已做主过滤，这里做补充/兜底）
// ============================================================

const filteredLogs = computed(() => {
  // 服务端分页模式下，filteredLogs 就是当前页数据，直接返回
  // 如果后端未做 keyword/action/date 过滤，可在此处补充前端过滤
  return logs.value
})

// ============================================================
// 事件处理
// ============================================================

const handleFilter = () => {
  currentPage.value = 1
  loadData()
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

// ============================================================
// 导出
// ============================================================

const exportLog = async () => {
  exporting.value = true
  try {
    const params = {}
    if (filterKeyword.value) params.keyword = filterKeyword.value
    if (filterAction.value) params.action = filterAction.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await exportAuditLogs(params)

    // 如果后端返回的是文件流（Blob），直接触发下载
    const blob = res instanceof Blob ? res : (res?.data instanceof Blob ? res.data : null)

    if (blob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `审计日志_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } else {
      // 兜底：如果后端返回的是下载链接
      const downloadUrl = res?.data?.url ?? res?.url
      if (downloadUrl) {
        window.open(downloadUrl, '_blank')
      } else {
        ElMessage.warning('导出失败：未获取到文件数据')
        return
      }
    }

    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出审计日志失败:', e)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

// ============================================================
// 暴露方法（供父组件调用）
// ============================================================

defineExpose({ loadData })

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
