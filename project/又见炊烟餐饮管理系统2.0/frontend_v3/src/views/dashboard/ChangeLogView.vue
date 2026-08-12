<template>
  <div class="change-log-view">
    <div class="page-header">
      <h2>改动日志 · Change Log</h2>
      <div class="header-tools">
        <el-select v-model="filterType" placeholder="操作类型" clearable style="width:130px" @change="loadData">
          <el-option label="全部" value="" />
          <el-option label="创建" value="create" />
          <el-option label="修改" value="update" />
          <el-option label="删除" value="delete" />
          <el-option label="登录" value="login" />
        </el-select>
        <el-select v-model="filterTarget" placeholder="目标类型" clearable style="width:120px" @change="loadData">
          <el-option label="全部" value="" />
          <el-option label="预订" value="booking" />
          <el-option label="客户" value="customer" />
          <el-option label="员工" value="staff" />
          <el-option label="桌台" value="table" />
          <el-option label="系统" value="system" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索摘要/操作人" clearable style="width:220px" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">🔍 查询</el-button>
        <el-button @click="refresh">🔄 刷新</el-button>
      </div>
    </div>

    <el-table :data="logs" style="width:100%" stripe size="small" v-loading="loading" @row-click="showDetail">
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="operationType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.operationType)" size="small" effect="plain">
            {{ typeLabel(row.operationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetType" label="目标" width="80">
        <template #default="{ row }">
          <span class="target-badge">{{ targetLabel(row.targetType) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" width="160" />
      <el-table-column prop="summary" label="摘要" min-width="300" show-overflow-tooltip />
    </el-table>

    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="loadData"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="📄 日志详情" width="680px">
      <div v-if="detail" class="detail-card">
        <div class="detail-row"><span class="dl">时间</span><span>{{ formatTime(detail.createdAt) }}</span></div>
        <div class="detail-row"><span class="dl">操作人</span><span>{{ detail.operatorName || '-' }}</span></div>
        <div class="detail-row"><span class="dl">类型</span><el-tag :type="typeTag(detail.operationType)" size="small">{{ typeLabel(detail.operationType) }}</el-tag></div>
        <div class="detail-row"><span class="dl">目标</span>{{ targetLabel(detail.targetType) }} #{{ detail.targetId || '-' }}</div>
        <div class="detail-row"><span class="dl">摘要</span>{{ detail.summary }}</div>
        <div v-if="detail.oldValue" class="detail-block">
          <div class="dl">修改前</div>
          <pre>{{ prettyJson(detail.oldValue) }}</pre>
        </div>
        <div v-if="detail.newValue" class="detail-block">
          <div class="dl">修改后</div>
          <pre>{{ prettyJson(detail.newValue) }}</pre>
        </div>
        <div v-if="detail.detail" class="detail-block">
          <div class="dl">详情</div>
          <pre>{{ detail.detail }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(50)
const total = ref(0)
const filterType = ref('')
const filterTarget = ref('')
const keyword = ref('')
const detailVisible = ref(false)
const detail = ref(null)

function typeTag(t) {
  const map = { create: 'success', update: 'warning', delete: 'danger', login: 'info', config: 'primary' }
  return map[t] || 'info'
}
function typeLabel(t) {
  const map = { create: '创建', update: '修改', delete: '删除', login: '登录', config: '配置' }
  return map[t] || t
}
function targetLabel(t) {
  const map = { booking: '预订', customer: '客户', staff: '员工', table: '桌台', dish: '菜品', menu: '菜单', supplier: '供应商', system: '系统' }
  return map[t] || t
}
function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ')
}

function prettyJson(s) {
  if (!s) return ''
  try { return JSON.stringify(JSON.parse(s), null, 2) } catch { return s }
}

async function loadData() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, pageSize: pageSize.value })
    if (filterType.value) params.set('operationType', filterType.value)
    if (filterTarget.value) params.set('targetType', filterTarget.value)
    if (keyword.value) params.set('keyword', keyword.value)
    const res = await fetch('/api/change-logs?' + params.toString(), { credentials: 'include' })
    const json = await res.json()
    if (json.code === 200) {
      logs.value = json.data || []
      total.value = json.extra?.total || 0
    } else {
      ElMessage.error(json.message || '加载失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  try {
    const res = await fetch(`/api/change-logs/${row.logId}`, { credentials: 'include' })
    const json = await res.json()
    if (json.code === 200) {
      detail.value = json.data
      detailVisible.value = true
    }
  } catch (e) {
    // ignore
  }
}

function refresh() { loadData() }

onMounted(loadData)
</script>

<style scoped>
.change-log-view { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; font-size: 20px; }
.header-tools { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.target-badge { display: inline-block; padding: 1px 8px; background: #f3f4f6; border-radius: 4px; font-size: 12px; color: #6b7280; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
.detail-card { padding: 0 4px; }
.detail-row { display: flex; padding: 8px 0; border-bottom: 1px solid #f3f4f6; gap: 12px; align-items: flex-start; }
.detail-row .dl { flex-shrink: 0; width: 70px; color: #6b7280; font-weight: 500; }
.detail-block { margin-top: 12px; }
.detail-block .dl { color: #6b7280; font-weight: 500; margin-bottom: 6px; }
.detail-block pre { background: #f8fafc; padding: 10px; border-radius: 6px; font-size: 12px; max-height: 200px; overflow: auto; margin: 0; border: 1px solid #eef2f6; }
</style>
