<template>
  <div class="art-design-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">美工设计 · Art Design</h2>
        <p class="page-desc">设计任务 · 菜单海报 · LED屏内容 · 审核管理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">设计任务</div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-sub">全部任务</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">进行中</div>
        <div class="stat-value">{{ stats.inProgress }}</div>
        <div class="stat-sub">设计制作中</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待审核</div>
        <div class="stat-value">{{ stats.pending }}</div>
        <div class="stat-sub">等待审核</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value">{{ stats.completed }}</div>
        <div class="stat-sub">本月交付</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="search"
            placeholder="搜索任务编号 / 项目名称"
            clearable
            style="width: 240px"
          />
          <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="待开始" value="待开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="待审核" value="待审核" />
            <el-option label="已完成" value="已完成" />
          </el-select>
          <el-select v-model="filterType" placeholder="类型" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="菜单设计" value="菜单设计" />
            <el-option label="海报" value="海报" />
            <el-option label="传单" value="传单" />
            <el-option label="LED屏" value="LED屏" />
            <el-option label="其他" value="其他" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
          <el-button @click="onReset">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="openAdd">+ 新建任务</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading">
        <el-table-column prop="taskNo" label="任务编号" width="140" />
        <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requester" label="需求人" width="110" />
        <el-table-column prop="deadline" label="截止日期" width="130" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ row.status || '待开始' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="previewTask(row)">预览</el-button>
            <el-button text size="small" @click="uploadFile(row)">上传</el-button>
            <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="showFormDialog" :title="editing ? '编辑任务' : '新建任务'" width="560px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="项目名称" required><el-input v-model="taskForm.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="taskForm.type" style="width: 100%">
            <el-option label="菜单设计" value="菜单设计" />
            <el-option label="海报" value="海报" />
            <el-option label="传单" value="传单" />
            <el-option label="LED屏" value="LED屏" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求人" required><el-input v-model="taskForm.requester" /></el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="taskForm.deadline"
            type="date"
            placeholder="选择截止日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskForm.status" style="width: 100%">
            <el-option label="待开始" value="待开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="待审核" value="待审核" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="附件文件">
          <el-input
            v-model="taskForm.files"
            placeholder="多个文件用逗号分隔，或点击下方上传"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="showPreviewDialog" title="任务预览 · Task Preview" width="600px">
      <div v-if="previewRow" class="preview-container">
        <div class="preview-header">
          <h3 class="preview-name">{{ previewRow.name || '-' }}</h3>
          <el-tag :type="statusTagType(previewRow.status)" size="small" effect="plain">
            {{ previewRow.status || '待开始' }}
          </el-tag>
        </div>
        <div class="preview-grid">
          <div class="preview-item">
            <span class="preview-label">任务编号</span>
            <span class="preview-value">{{ previewRow.taskNo || '-' }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">类型</span>
            <span class="preview-value">
              <el-tag :type="typeTagType(previewRow.type)" size="small" effect="plain">{{ previewRow.type || '-' }}</el-tag>
            </span>
          </div>
          <div class="preview-item">
            <span class="preview-label">需求人</span>
            <span class="preview-value">{{ previewRow.requester || '-' }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">截止日期</span>
            <span class="preview-value">{{ previewRow.deadline || '-' }}</span>
          </div>
          <div class="preview-item full">
            <span class="preview-label">需求描述</span>
            <span class="preview-value">{{ previewRow.description || '无' }}</span>
          </div>
          <div class="preview-item full">
            <span class="preview-label">附件文件</span>
            <span class="preview-value">{{ previewRow.files || '暂无附件' }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPreviewDialog = false">关闭</el-button>
        <el-button type="primary" @click="uploadFromPreview">上传附件</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const search = ref('')
const filterStatus = ref('')
const filterType = ref('')
const dateRange = ref([])

const tasks = ref([])

const stats = ref({ total: 0, inProgress: 0, pending: 0, completed: 0 })

const showFormDialog = ref(false)
const showPreviewDialog = ref(false)
const editing = ref(false)
const previewRow = ref(null)

const taskForm = ref({
  name: '',
  type: '菜单设计',
  requester: '',
  deadline: '',
  status: '待开始',
  description: '',
  files: ''
})

const filteredList = computed(() => {
  let list = tasks.value
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(t =>
      (t.name || '').toLowerCase().includes(q) ||
      (t.taskNo || '').toLowerCase().includes(q)
    )
  }
  if (filterStatus.value) list = list.filter(t => t.status === filterStatus.value)
  if (filterType.value) list = list.filter(t => t.type === filterType.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(t => {
      if (!t.deadline) return false
      return t.deadline >= start && t.deadline <= end
    })
  }
  return list
})

function typeTagType(type) {
  return {
    '菜单设计': 'warning',
    '海报': 'success',
    '传单': 'info',
    'LED屏': 'danger',
    '其他': 'info'
  }[type] || 'info'
}

function statusTagType(status) {
  return {
    '待开始': 'info',
    '进行中': 'warning',
    '待审核': 'danger',
    '已完成': 'success'
  }[status] || 'info'
}

function onReset() {
  search.value = ''
  filterStatus.value = ''
  filterType.value = ''
  dateRange.value = []
}

function openAdd() {
  editing.value = false
  taskForm.value = {
    name: '', type: '菜单设计', requester: '', deadline: '',
    status: '待开始', description: '', files: ''
  }
  showFormDialog.value = true
}

function openEdit(row) {
  editing.value = true
  taskForm.value = { ...row }
  showFormDialog.value = true
}

function saveTask() {
  if (!taskForm.value.name) {
    ElMessage.warning('请填写项目名称')
    return
  }
  if (!taskForm.value.requester) {
    ElMessage.warning('请填写需求人')
    return
  }
  ElMessage.success('保存成功')
  showFormDialog.value = false
}

function previewTask(row) {
  previewRow.value = row
  showPreviewDialog.value = true
}

function uploadFile(row) {
  ElMessage.success(`已为任务 ${row.taskNo || ''} 打开上传入口`)
}

function uploadFromPreview() {
  if (previewRow.value) {
    ElMessage.success(`已为任务 ${previewRow.value.taskNo || ''} 打开上传入口`)
  }
}
</script>

<style scoped>
.art-design-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2D4A3E 0%, #C4A35A 100%);
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #95A5A6;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 6px;
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

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-container {
  padding: 4px 0;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #E8E4DE;
}

.preview-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 24px;
}

.preview-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-item.full {
  grid-column: 1 / -1;
}

.preview-label {
  font-size: 12px;
  color: #95A5A6;
  font-weight: 500;
}

.preview-value {
  font-size: 14px;
  color: #1a2f23;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
