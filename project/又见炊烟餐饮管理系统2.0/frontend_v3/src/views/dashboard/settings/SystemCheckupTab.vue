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
            <span class="feature-icon">🗄️</span>
            <span>数据库健康检查</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">⚡</span>
            <span>API响应时间监控</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">💾</span>
            <span>磁盘空间预警</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔒</span>
            <span>安全漏洞扫描</span>
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
        <el-button type="primary" @click="exportReport">导出报告</el-button>
        <el-button @click="rescan">重新扫描</el-button>
      </div>

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
                <div class="item-icon">
                  <span v-if="item.level === 'fatal'">❌</span>
                  <span v-else-if="item.level === 'error'">⚠️</span>
                  <span v-else-if="item.level === 'warning'">⚡</span>
                  <span v-else>✅</span>
                </div>
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
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

const scanStarted = ref(false)
const scanCompleted = ref(false)
const scanProgress = ref(0)
const currentCheckItem = ref('准备中...')
const completedItems = ref(0)
const totalItems = ref(4780)
const issueCount = ref(0)
const remainingTime = ref(30)
const activeCategory = ref('all')

const checkCategories = [
  { key: 'all', label: '全部' },
  { key: 'database', label: '数据库' },
  { key: 'backend', label: '后端服务' },
  { key: 'frontend', label: '前端资源' },
  { key: 'system', label: '系统环境' },
]

const checkItems = reactive([
  // FATAL
  { category: 'database', name: '数据库连接池', description: '检测数据库连接池状态', level: 'fatal', details: '连接池已耗尽，当前无可用连接', suggestion: '立即检查数据库服务，增加连接池大小或优化慢查询' },
  
  // ERROR
  { category: 'database', name: '表索引完整性', description: '检查核心表索引是否完整', level: 'error', details: 'booking_master 表缺少 idx_store_date 索引', suggestion: '执行: ALTER TABLE booking_master ADD INDEX idx_store_date (store_id, booking_date)' },
  { category: 'backend', name: 'API响应时间', description: '检测API接口响应时间', level: 'error', details: '/api/bookings 接口平均响应时间 > 2000ms', suggestion: '检查接口实现，优化数据库查询或添加缓存' },
  { category: 'system', name: '磁盘空间', description: '检查服务器磁盘使用率', level: 'error', details: '/mnt/cos 使用率 92%，剩余空间不足 10GB', suggestion: '清理过期备份文件和日志，或扩容磁盘' },
  
  // WARNING
  { category: 'database', name: '慢查询监控', description: '检测慢查询数量', level: 'warning', details: '最近1小时发现 15 条慢查询 (>1s)', suggestion: '检查慢查询日志，优化SQL语句' },
  { category: 'backend', name: '内存使用率', description: '检测JVM内存使用情况', level: 'warning', details: '堆内存使用率 78%，接近阈值', suggestion: '监控内存趋势，必要时增加JVM堆内存' },
  { category: 'frontend', name: '静态资源缓存', description: '检查静态资源缓存配置', level: 'warning', details: '部分JS/CSS文件未配置长期缓存', suggestion: '在Nginx配置中添加Cache-Control头' },
  { category: 'system', name: '日志文件大小', description: '检查日志文件占用空间', level: 'warning', details: '应用日志文件总计 8.5GB', suggestion: '配置日志轮转，定期清理旧日志' },
  
  // NORMAL
  { category: 'database', name: '数据库备份', description: '检查最近备份状态', level: 'normal', details: '最近备份时间: 2026-07-31 02:00:00，状态: 成功', suggestion: '' },
  { category: 'database', name: '数据一致性', description: '检查表数据一致性', level: 'normal', details: '所有表数据校验通过', suggestion: '' },
  { category: 'backend', name: '服务状态', description: '检查后端服务运行状态', level: 'normal', details: 'Spring Boot 服务运行正常，运行时间: 3天14小时', suggestion: '' },
  { category: 'backend', name: 'Redis连接', description: '检查Redis缓存服务', level: 'normal', details: 'Redis连接正常，命中率: 92%', suggestion: '' },
  { category: 'frontend', name: '前端构建', description: '检查前端构建状态', level: 'normal', details: '生产构建成功，无编译错误', suggestion: '' },
  { category: 'system', name: '系统时间', description: '检查系统时间同步', level: 'normal', details: 'NTP时间同步正常', suggestion: '' },
  { category: 'system', name: '安全证书', description: '检查SSL证书有效期', level: 'normal', details: 'SSL证书有效期至 2027-01-15', suggestion: '' },
])

const summaryStats = computed(() => [
  { level: 'fatal', label: '严重问题', count: checkItems.filter(i => i.level === 'fatal').length },
  { level: 'error', label: '错误', count: checkItems.filter(i => i.level === 'error').length },
  { level: 'warning', label: '警告', count: checkItems.filter(i => i.level === 'warning').length },
  { level: 'normal', label: '正常', count: checkItems.filter(i => i.level === 'normal').length },
])

function getCategoryCount(category) {
  if (category === 'all') return checkItems.length
  return checkItems.filter(i => i.category === category).length
}

function getItemsByCategory(category) {
  if (category === 'all') return checkItems
  return checkItems.filter(i => i.category === category)
}

function getTagType(level) {
  const map = { fatal: 'danger', error: 'warning', warning: '', normal: 'success' }
  return map[level] || 'info'
}

function startScan() {
  scanStarted.value = true
  scanCompleted.value = false
  scanProgress.value = 0
  completedItems.value = 0
  issueCount.value = 0
  remainingTime.value = 30

  const checkList = [
    '检查数据库连接...',
    '扫描表结构完整性...',
    '检测索引状态...',
    '验证数据一致性...',
    '检查API服务响应...',
    '监控内存使用情况...',
    '扫描静态资源...',
    '检查磁盘空间...',
    '验证安全配置...',
    '生成诊断报告...'
  ]

  let step = 0
  const interval = setInterval(() => {
    if (step < checkList.length) {
      currentCheckItem.value = checkList[step]
      scanProgress.value = Math.floor(((step + 1) / checkList.length) * 100)
      completedItems.value = Math.floor(((step + 1) / checkList.length) * totalItems.value)
      remainingTime.value = Math.max(0, 30 - (step + 1) * 3)
      
      // 模拟发现问题
      if (step === 1 || step === 3 || step === 5) {
        issueCount.value += Math.floor(Math.random() * 3 + 1)
      }
      
      step++
    } else {
      clearInterval(interval)
      scanCompleted.value = true
      ElMessage.success('系统体检完成')
    }
  }, 3000)
}

function rescan() {
  scanStarted.value = false
  scanCompleted.value = false
  startScan()
}

function exportReport() {
  ElMessage.info('正在生成体检报告...')
  setTimeout(() => {
    ElMessage.success('报告已导出到: /mnt/cos/reports/checkup_20260731.pdf')
  }, 1500)
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
  border-left-color: #dc2626;
}

.check-item.error {
  border-left-color: #f59e0b;
}

.check-item.warning {
  border-left-color: #3b82f6;
}

.check-item.normal {
  border-left-color: #10b981;
}

.item-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.item-icon {
  font-size: 20px;
  flex-shrink: 0;
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
}
</style>
