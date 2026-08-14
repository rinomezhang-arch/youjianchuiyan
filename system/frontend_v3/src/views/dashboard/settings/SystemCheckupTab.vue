<template>
  <div class="checkup-page">
    <div class="page-header">
      <h2 class="page-title">系统体检</h2>
      <p class="page-subtitle">System Checkup · 全面健康检查与诊断</p>
    </div>

    <!-- 体检概览 -->
    <div class="checkup-overview" v-if="!scanStarted">
      <div class="overview-content">
        <div class="overview-icon">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <h3>系统健康诊断</h3>
        <p>一键扫描系统状态，检测潜在问题，确保系统稳定运行</p>
        <div class="overview-features">
          <div class="feature-item">
            <span>数据库结构与数据完整性</span>
          </div>
          <div class="feature-item">
            <span>后端实体与 API 映射</span>
          </div>
          <div class="feature-item">
            <span>前端调用证据</span>
          </div>
          <div class="feature-item">
            <span>空表政策与现实用途</span>
          </div>
        </div>
        <el-button type="primary" size="large" @click="startScan">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.3-4.3"/>
          </svg>
          开始系统体检
        </el-button>
      </div>
    </div>

    <!-- 扫描进行中 -->
    <div class="scan-progress" v-if="scanStarted && !scanCompleted">
      <div class="progress-header">
        <h3>正在扫描系统...</h3>
        <p>检测项: {{ currentCheckItem }}</p>
      </div>
      <el-progress
        :percentage="scanProgress"
        :status="scanProgress === 100 ? 'success' : ''"
        :stroke-width="20"
        style="margin: 20px 0"
      />
      <div class="progress-details">
        <div class="detail-row">
          <span class="detail-label">已完成:</span>
          <span class="detail-value">{{ completedItems }} / {{ totalItems }} 项</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">发现问题:</span>
          <span class="detail-value">{{ issueCount }} 个</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">预计剩余:</span>
          <span class="detail-value">{{ remainingTime }} 秒</span>
        </div>
      </div>
    </div>

    <!-- 扫描结果 -->
    <div class="scan-results" v-if="scanCompleted">
      <div class="results-summary">
        <div class="summary-card" v-for="stat in summaryStats" :key="stat.level" :class="stat.level">
          <div class="stat-count">{{ stat.count }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>

      <div class="results-actions">
        <el-button type="primary" @click="exportReport">导出逐表报告</el-button>
        <el-button @click="rescan">重新扫描</el-button>
      </div>

      <section class="mapping-section" aria-labelledby="mapping-title">
        <div class="mapping-heading">
          <div>
            <h3 id="mapping-title">数据库—后端—前端逐表映射</h3>
            <p>每一行均来自实库结构与源码扫描证据；未映射项不会被计为完成。</p>
          </div>
          <el-input v-model="tableKeyword" clearable placeholder="搜索表名、实体、控制器或前端文件" class="mapping-search" />
        </div>
        <el-table :data="filteredTableRows" max-height="520" border stripe empty-text="没有符合条件的表">
          <el-table-column prop="tableName" label="数据库表" min-width="190" fixed />
          <el-table-column prop="rowCount" label="行数" width="90" align="right" />
          <el-table-column label="映射状态" width="110">
            <template #default="{ row }">
              <el-tag :type="mappingTagType(row.mappingStatus)" size="small">{{ mappingLabel(row.mappingStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="entityClass" label="Entity" min-width="180" show-overflow-tooltip />
          <el-table-column prop="repositoryClass" label="Repository" min-width="210" show-overflow-tooltip />
          <el-table-column prop="controllerClass" label="Controller" min-width="220" show-overflow-tooltip />
          <el-table-column prop="apiRoutes" label="API 路由" min-width="260" show-overflow-tooltip />
          <el-table-column prop="frontendFiles" label="前端调用文件" min-width="280" show-overflow-tooltip />
          <el-table-column prop="emptyPolicy" label="空表策略" width="150" />
          <el-table-column prop="purpose" label="现实业务用途" min-width="280" show-overflow-tooltip />
        </el-table>
      </section>

      <!-- 问题详情 -->
      <el-tabs v-model="activeCategory" class="results-tabs">
        <el-tab-pane
          v-for="cat in checkCategories"
          :key="cat.key"
          :label="`${cat.label} (${getCategoryCount(cat.key)})`"
          :name="cat.key"
        >
          <div class="category-items">
            <div
              v-for="item in getItemsByCategory(cat.key)"
              :key="item.name"
              class="check-item"
              :class="item.level"
            >
              <div class="item-header">
                <div class="item-info">
                  <h4>{{ item.name }}</h4>
                  <p>{{ item.description }}</p>
                </div>
                <el-tag :type="getTagType(item.level)" size="small">{{ item.level.toUpperCase() }}</el-tag>
              </div>
              <div class="item-details" v-if="item.details">
                <div class="detail-block">
                  <span class="block-label">详情:</span>
                  <span class="block-content">{{ item.details }}</span>
                </div>
                <div class="detail-block" v-if="item.suggestion">
                  <span class="block-label">建议:</span>
                  <span class="block-content">{{ item.suggestion }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getDatabaseGovernanceAudit } from '@/api/systemGovernance'

const scanStarted = ref(false)
const scanCompleted = ref(false)
const scanProgress = ref(0)
const currentCheckItem = ref('准备中...')
const completedItems = ref(0)
const totalItems = ref(0)
const issueCount = ref(0)
const remainingTime = ref(0)
const activeCategory = ref('all')
const checkItems = ref([])
const tableRows = ref([])
const tableKeyword = ref('')

const filteredTableRows = computed(() => {
  const keyword = tableKeyword.value.trim().toLowerCase()
  if (!keyword) return tableRows.value
  return tableRows.value.filter(row => [row.tableName, row.entityClass, row.repositoryClass, row.controllerClass,
    row.apiRoutes, row.frontendFiles, row.purpose].some(value => String(value || '').toLowerCase().includes(keyword)))
})

const checkCategories = [
  { key: 'all', label: '全部' },
  { key: 'database', label: '数据库' },
  { key: 'backend', label: '后端服务' },
  { key: 'frontend', label: '前端资源' },
]

const summaryStats = computed(() => [
  { level: 'fatal', label: '严重问题', count: checkItems.value.filter(i => i.level === 'fatal').length },
  { level: 'error', label: '错误', count: checkItems.value.filter(i => i.level === 'error').length },
  { level: 'warning', label: '警告', count: checkItems.value.filter(i => i.level === 'warning').length },
  { level: 'normal', label: '正常', count: checkItems.value.filter(i => i.level === 'normal').length },
])

function getCategoryCount(category) {
  if (category === 'all') return checkItems.value.length
  return checkItems.value.filter(i => i.category === category).length
}

function getItemsByCategory(category) {
  if (category === 'all') return checkItems.value
  return checkItems.value.filter(i => i.category === category)
}

function getTagType(level) {
  return { fatal: 'danger', error: 'warning', warning: '', normal: 'success' }[level] || 'info'
}

function mappingTagType(status) {
  return { MAPPED: 'success', PARTIAL: 'warning', UNMAPPED: 'danger' }[status] || 'info'
}

function mappingLabel(status) {
  return { MAPPED: '完整', PARTIAL: '部分', UNMAPPED: '未映射' }[status] || '未审计'
}

async function startScan() {
  scanStarted.value = true
  scanCompleted.value = false
  scanProgress.value = 20
  currentCheckItem.value = '读取数据库结构与用途登记...'
  try {
    const response = await getDatabaseGovernanceAudit()
    const audit = response.data
    scanProgress.value = 100
    currentCheckItem.value = '数据库治理检查完成'
    checkItems.value = audit.checks || []
    tableRows.value = audit.tables || []
    totalItems.value = audit.summary?.tables || tableRows.value.length
    completedItems.value = totalItems.value
    issueCount.value = checkItems.value.filter(item => item.level !== 'normal').length
    scanCompleted.value = true
    ElMessage.success(`已完成 ${totalItems.value} 张表的真实检查`)
  } catch (error) {
    scanStarted.value = false
    ElMessage.error(error.message || '系统体检失败')
  }
}

function rescan() {
  startScan()
}

function csvCell(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`
}

function exportReport() {
  const headers = ['表名', '业务域', '数据类型', '空表策略', '行数', '映射状态', 'Entity', 'Repository', 'Controller',
    'API 路由', '前端调用文件', '前端连接', '现实用途']
  const rows = filteredTableRows.value.map(row => [row.tableName, row.businessDomain, row.dataKind, row.emptyPolicy,
    row.rowCount, row.mappingStatus, row.entityClass, row.repositoryClass, row.controllerClass, row.apiRoutes,
    row.frontendFiles, row.frontendBinding, row.purpose])
  const csv = `\uFEFF${[headers, ...rows].map(row => row.map(csvCell).join(',')).join('\n')}`
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `database-governance-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('数据库治理报告已导出')
}
</script>

<style scoped>
.checkup-page {
  max-width: 1200px;
}

.page-header {
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.checkup-overview {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 60px 40px;
}

.overview-content {
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
}

.overview-icon {
  color: var(--color-primary);
  margin-bottom: 24px;
}

.overview-content h3 {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

.overview-content p {
  font-size: 14px;
  color: var(--color-text-muted);
  margin: 0 0 32px 0;
}

.overview-features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 400px;
  margin: 0 auto 32px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.feature-icon {
  font-size: 18px;
}

.scan-progress {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 40px;
}

.progress-header {
  text-align: center;
  margin-bottom: 24px;
}

.progress-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

.progress-header p {
  font-size: 14px;
  color: var(--color-text-muted);
  margin: 0;
}

.progress-details {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-top: 24px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.detail-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.scan-results {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.results-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  background: var(--color-bg-alt);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
}

.summary-card.fatal {
  border-color: #dc2626;
  background: rgba(220, 38, 38, 0.05);
}

.summary-card.error {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.05);
}

.summary-card.warning {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}

.summary-card.normal {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.05);
}

.stat-count {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 4px;
}

.fatal .stat-count {
  color: #dc2626;
}

.error .stat-count {
  color: #f59e0b;
}

.warning .stat-count {
  color: #3b82f6;
}

.normal .stat-count {
  color: #10b981;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.results-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.mapping-section {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-alt);
}

.mapping-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.mapping-heading h3 {
  margin: 0 0 4px;
  color: var(--color-text);
  font-size: 17px;
}

.mapping-heading p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.mapping-search {
  width: 360px;
  flex: 0 0 auto;
}

.results-tabs {
  margin-top: 20px;
}

.category-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.check-item {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  border-left: 4px solid var(--color-border);
}

.check-item.fatal {
  border-left-color: var(--el-color-danger);
}

.check-item.error {
  border-left-color: var(--el-color-warning);
}

.check-item.warning {
  border-left-color: var(--el-color-primary);
}

.check-item.normal {
  border-left-color: var(--el-color-success);
}

.item-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.item-info {
  flex: 1;
}

.item-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.item-info p {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.item-details {
  background: white;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  padding: 12px;
  margin-top: 12px;
}

.detail-block {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.detail-block:last-child {
  margin-bottom: 0;
}

.block-label {
  font-weight: 600;
  color: var(--color-text-secondary);
  min-width: 50px;
}

.block-content {
  color: var(--color-text);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .overview-features {
    grid-template-columns: 1fr;
  }
  
  .results-summary {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .checkup-overview {
    padding: 40px 20px;
  }

  .mapping-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .mapping-search {
    width: 100%;
  }
}
</style>
