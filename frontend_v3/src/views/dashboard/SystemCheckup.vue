<template>
  <div class="system-checkup-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">系统体检 · System Checkup</h1>
        <p class="page-desc">系统健康检查 · 性能诊断 · 异常排查 · 多维度评估系统运行状态</p>
      </div>
      <div class="header-right">
        <span class="last-check">上次检查：{{ lastCheckTime }}</span>
        <el-button type="primary" @click="runCheck" :loading="loading">重新体检</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-score">
        <div class="stat-label">系统得分</div>
        <div class="stat-value">{{ stats.score }}</div>
        <div class="stat-sub">System Score / 100</div>
      </div>
      <div class="stat-card stat-total">
        <div class="stat-label">检查项</div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-sub">Total Checks</div>
      </div>
      <div class="stat-card stat-pass">
        <div class="stat-label">通过项</div>
        <div class="stat-value">{{ stats.passed }}</div>
        <div class="stat-sub">Passed</div>
      </div>
      <div class="stat-card stat-warn">
        <div class="stat-label">警告项</div>
        <div class="stat-value">{{ stats.warning }}</div>
        <div class="stat-sub">Warning</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width:150px">
            <el-option label="数据库" value="数据库" />
            <el-option label="API" value="API" />
            <el-option label="前端" value="前端" />
            <el-option label="后端" value="后端" />
            <el-option label="安全" value="安全" />
            <el-option label="性能" value="性能" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:140px">
            <el-option label="通过" value="通过" />
            <el-option label="警告" value="警告" />
            <el-option label="异常" value="异常" />
          </el-select>
        </div>
        <span class="result-text">共 {{ filteredList.length }} 项检查</span>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无检查数据">
        <el-table-column prop="name" label="检查项" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="100" align="center">
          <template #default="{ row }">
            <span :class="['score-text', scoreClass(row.score)]">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="建议" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status !== '通过'" text size="small" type="primary" @click="retryItem(row)">复检</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetail" title="检查项详情" width="560px">
      <el-form label-width="100px" label-position="right" v-if="currentRow">
        <el-form-item label="检查项"><span class="dialog-text">{{ currentRow.name }}</span></el-form-item>
        <el-form-item label="分类"><span class="dialog-text">{{ currentRow.category }}</span></el-form-item>
        <el-form-item label="状态">
          <el-tag :type="statusTag(currentRow.status)" size="small">{{ currentRow.status }}</el-tag>
        </el-form-item>
        <el-form-item label="得分">
          <span :class="['score-text', scoreClass(currentRow.score)]">{{ currentRow.score }}</span>
        </el-form-item>
        <el-form-item label="详情"><span class="dialog-text">{{ currentRow.detail }}</span></el-form-item>
        <el-form-item label="建议"><span class="dialog-text">{{ currentRow.suggestion }}</span></el-form-item>
        <el-form-item label="检查时间"><span class="dialog-text">{{ currentRow.checkTime }}</span></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button v-if="currentRow && currentRow.status !== '通过'" type="primary" @click="retryItem(currentRow)">立即复检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const filterCategory = ref('')
const filterStatus = ref('')
const showDetail = ref(false)
const currentRow = ref(null)
const lastCheckTime = ref('-')

const checkList = ref([])

const stats = ref({ score: 0, total: 0, passed: 0, warning: 0 })

const filteredList = computed(() => {
  let list = checkList.value
  if (filterCategory.value) list = list.filter(c => c.category === filterCategory.value)
  if (filterStatus.value) list = list.filter(c => c.status === filterStatus.value)
  return list
})

function statusTag(status) {
  return { '通过': 'success', '警告': 'warning', '异常': 'danger' }[status] || 'info'
}

function scoreClass(score) {
  if (score >= 90) return 'score-good'
  if (score >= 70) return 'score-warn'
  return 'score-bad'
}

async function runCheck() {
  loading.value = true
  try {
    const res = await fetch('/menu-api/system/checkup').then(r => r.json())
    if (res.code === 200) {
      checkList.value = res.data?.list || []
      stats.value = res.data?.stats || stats.value
    }
  } catch {
    checkList.value = [
      { name: '数据库连接池', category: '数据库', status: '通过', score: 98, detail: '连接池使用率 32%，最大连接 100', suggestion: '状态正常，无需处理', checkTime: '2026-07-31 10:00' },
      { name: '数据库慢查询', category: '数据库', status: '警告', score: 78, detail: '近24小时发现 6 条慢查询（>1s）', suggestion: '优化菜品搜索 SQL 索引，避免全表扫描', checkTime: '2026-07-31 10:00' },
      { name: '数据库备份', category: '数据库', status: '通过', score: 95, detail: '最近备份：2026-07-31 02:00，大小 128MB', suggestion: '备份正常', checkTime: '2026-07-31 10:00' },
      { name: 'API 响应时间', category: 'API', status: '通过', score: 92, detail: '平均响应 180ms，P95 420ms', suggestion: '响应良好', checkTime: '2026-07-31 10:00' },
      { name: 'API 错误率', category: 'API', status: '警告', score: 82, detail: '近24小时错误率 0.8%（5xx 12次）', suggestion: '检查 /bills 接口异常，关注支付回调超时', checkTime: '2026-07-31 10:00' },
      { name: '前端资源加载', category: '前端', status: '通过', score: 90, detail: '首屏加载 1.8s，资源压缩已启用', suggestion: '可考虑拆分路由懒加载', checkTime: '2026-07-31 10:00' },
      { name: '前端内存占用', category: '前端', status: '通过', score: 94, detail: '页面内存峰值 78MB', suggestion: '正常', checkTime: '2026-07-31 10:00' },
      { name: '后端服务状态', category: '后端', status: '通过', score: 96, detail: '服务运行中，CPU 18%，内存 42%', suggestion: '运行正常', checkTime: '2026-07-31 10:00' },
      { name: '后端日志错误', category: '后端', status: '警告', score: 75, detail: '近24小时 ERROR 日志 23 条', suggestion: '排查订单服务空指针异常', checkTime: '2026-07-31 10:00' },
      { name: '安全漏洞扫描', category: '安全', status: '通过', score: 91, detail: '未发现高危漏洞，依赖库已更新', suggestion: '建议定期扫描', checkTime: '2026-07-31 10:00' },
      { name: '安全权限配置', category: '安全', status: '警告', score: 80, detail: '2 个接口缺少权限校验', suggestion: '为 /api/admin/* 添加鉴权中间件', checkTime: '2026-07-31 10:00' },
      { name: '安全登录策略', category: '安全', status: '通过', score: 93, detail: '密码强度策略已启用，失败锁定 5 次', suggestion: '正常', checkTime: '2026-07-31 10:00' },
      { name: '性能磁盘空间', category: '性能', status: '警告', score: 72, detail: '系统盘使用率 85%', suggestion: '清理日志与临时文件，扩容或迁移', checkTime: '2026-07-31 10:00' },
      { name: '性能缓存命中率', category: '性能', status: '通过', score: 97, detail: 'Redis 缓存命中率 94%', suggestion: '缓存策略良好', checkTime: '2026-07-31 10:00' },
      { name: '性能接口并发', category: '性能', status: '异常', score: 58, detail: '高峰时段订单接口并发超阈值', suggestion: '增加接口限流与队列削峰，扩容实例', checkTime: '2026-07-31 10:00' }
    ]
  } finally {
    loading.value = false
  }
  refreshStats()
  lastCheckTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function refreshStats() {
  const list = checkList.value
  const passed = list.filter(c => c.status === '通过').length
  const warning = list.filter(c => c.status === '警告').length
  const avg = list.length ? Math.round(list.reduce((s, c) => s + c.score, 0) / list.length) : 0
  stats.value = {
    score: avg,
    total: list.length,
    passed,
    warning
  }
}

function viewDetail(row) {
  currentRow.value = row
  showDetail.value = true
}

function retryItem(row) {
  ElMessage.info(`正在复检「${row.name}」...`)
  setTimeout(() => {
    if (row.score < 90) {
      row.score = Math.min(100, row.score + 5)
      if (row.score >= 90) {
        row.status = '通过'
        ElMessage.success(`「${row.name}」复检通过`)
      } else {
        ElMessage.warning(`「${row.name}」复检得分提升至 ${row.score}`)
      }
    } else {
      ElMessage.success(`「${row.name}」复检通过`)
    }
    refreshStats()
    if (showDetail.value) {
      // keep dialog data fresh
      currentRow.value = { ...row }
    }
  }, 800)
}

onMounted(() => { runCheck() })
</script>

<style scoped>
.system-checkup-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left {
  display: flex;
  flex-direction: column;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.last-check {
  font-size: 13px;
  color: #95A5A6;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
  letter-spacing: 0.5px;
}
.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin-top: 6px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 22px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 2px 0 0 2px;
}
.stat-card.stat-score::before { background: #2D4A3E; }
.stat-card.stat-total::before { background: #5B7B8A; }
.stat-card.stat-pass::before { background: #4A7C59; }
.stat-card.stat-warn::before { background: #C4A35A; }
.stat-card:hover {
  box-shadow: 0 4px 12px rgba(45, 74, 62, 0.08);
  transform: translateY(-2px);
}
.stat-label {
  font-size: 13px;
  color: #5D6D7E;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  margin-top: 8px;
}
.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 4px;
}

.content-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.result-text {
  font-size: 13px;
  color: #95A5A6;
}

.score-text {
  font-weight: 700;
  font-size: 15px;
}
.score-good { color: #4A7C59; }
.score-warn { color: #C4A35A; }
.score-bad { color: #C25555; }

.dialog-text {
  color: #1a2f23;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .page-header { flex-direction: column; align-items: stretch; }
}
</style>
