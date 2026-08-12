<template>
  <div class="hr-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">培训管理 · Training Management</h1>
        <span class="page-desc">入职培训 · 技能培训 · 安全培训 · 服务培训</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openAdd"><span>新增培训 · Add</span></el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card st-total">
        <div class="stat-num">{{ list.length }}</div>
        <div class="stat-label">培训总数 · Total</div>
      </div>
      <div class="stat-card st-active">
        <div class="stat-num">{{ list.filter(t => t.status === 'completed').length }}</div>
        <div class="stat-label">已完成 · Completed</div>
      </div>
      <div class="stat-card st-mgr">
        <div class="stat-num">{{ list.filter(t => t.status === 'in_progress').length }}</div>
        <div class="stat-label">进行中 · In Progress</div>
      </div>
      <div class="stat-card st-admin">
        <div class="stat-num">{{ passRate }}%</div>
        <div class="stat-label">合格率 · Pass Rate</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="search" placeholder="搜索培训名称/讲师..." class="search-input" clearable />
      <el-select v-model="filterType" placeholder="培训类型" clearable style="width:160px">
        <el-option label="入职培训 · Onboarding" value="onboarding" />
        <el-option label="技能培训 · Skill" value="skill" />
        <el-option label="安全培训 · Safety" value="safety" />
        <el-option label="服务培训 · Service" value="service" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width:140px">
        <el-option label="已完成 · Completed" value="completed" />
        <el-option label="进行中 · In Progress" value="in_progress" />
        <el-option label="已计划 · Planned" value="planned" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <el-table :data="filteredList" stripe class="data-table" @row-contextmenu="onRowMenu">
      <el-table-column prop="trainingName" label="培训名称 · Training" min-width="160" />
      <el-table-column prop="trainingType" label="类型 · Type" width="120">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.trainingType)" size="small" effect="plain">{{ typeLabel(row.trainingType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="部门 · Dept" width="130" />
      <el-table-column prop="participants" label="参与人数 · Count" width="90" align="center" />
      <el-table-column prop="instructor" label="讲师 · Instructor" width="110" />
      <el-table-column prop="trainingDate" label="日期 · Date" width="120" />
      <el-table-column prop="status" label="状态 · Status" width="110" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="row.status"></span>
          <span class="status-text">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="examResult" label="考核 · Exam" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.examResult" :class="'exam-' + row.examResult">{{ examLabel(row.examResult) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条 · 右键操作</span>
    </div>

    <!-- 右键菜单 -->
    <div v-if="ctxMenu.visible" class="ctx-menu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }">
      <div class="ctx-item" @click="ctxDetail">查看详情 · Detail</div>
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑培训 · Edit Training' : '新增培训 · Add Training'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="110px" :rules="rules" ref="formRef">
        <el-form-item label="培训名称" prop="trainingName">
          <el-input v-model="form.trainingName" placeholder="必填" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="培训类型" prop="trainingType">
              <el-select v-model="form.trainingType" style="width:100%">
                <el-option label="入职培训 · Onboarding" value="onboarding" />
                <el-option label="技能培训 · Skill" value="skill" />
                <el-option label="安全培训 · Safety" value="safety" />
                <el-option label="服务培训 · Service" value="service" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-select v-model="form.department" style="width:100%">
                <el-option v-for="d in depts" :key="d" :label="d" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-input v-model="form.instructor" placeholder="培训讲师" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训日期" prop="trainingDate">
              <el-date-picker v-model="form.trainingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="参与人数">
              <el-input-number v-model="form.participants" :min="1" :max="200" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核方式">
              <el-select v-model="form.examMethod" style="width:100%">
                <el-option label="笔试 · Written" value="written" />
                <el-option label="实操 · Practical" value="practical" />
                <el-option label="综合 · Combined" value="combined" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="培训内容描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveTraining" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetail" title="培训详情 · Training Detail" width="500px">
      <div v-if="detailRow" class="detail-content">
        <div class="detail-row"><span class="detail-label">培训名称</span><span>{{ detailRow.trainingName }}</span></div>
        <div class="detail-row"><span class="detail-label">类型</span><span>{{ typeLabel(detailRow.trainingType) }}</span></div>
        <div class="detail-row"><span class="detail-label">部门</span><span>{{ detailRow.department }}</span></div>
        <div class="detail-row"><span class="detail-label">讲师</span><span>{{ detailRow.instructor || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">日期</span><span>{{ detailRow.trainingDate }}</span></div>
        <div class="detail-row"><span class="detail-label">参与人数</span><span>{{ detailRow.participants }} 人</span></div>
        <div class="detail-row"><span class="detail-label">考核方式</span><span>{{ examLabel(detailRow.examMethod) }}</span></div>
        <div class="detail-row"><span class="detail-label">考核结果</span><span>{{ examLabel(detailRow.examResult) || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span><span>{{ statusLabel(detailRow.status) }}</span></div>
        <div class="detail-row" v-if="detailRow.content"><span class="detail-label">培训内容</span><span>{{ detailRow.content }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const depts = ['前厅服务部', '后厨生产部', '销售宴会部', '财务采购人事部', '高层管理部']

const list = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/training')
    list.value = res.data || []
  } catch (e) {
    console.error('获取培训列表失败', e)
    ElMessage.error('获取培训列表失败')
  } finally {
    loading.value = false
  }
}

const showDialog = ref(false)
const showDetail = ref(false)
const editing = ref(false)
const saving = ref(false)
const search = ref('')
const filterType = ref('')
const filterStatus = ref('')
const formRef = ref(null)
const detailRow = ref(null)
const ctxMenu = ref({ visible: false, x: 0, y: 0 })
let ctxRow = null

const form = ref({
  trainingName: '', trainingType: 'onboarding', department: '', participants: 1,
  instructor: '', trainingDate: '', examMethod: 'written', content: ''
})

const rules = {
  trainingName: [{ required: true, message: '培训名称必填' }],
  trainingType: [{ required: true, message: '培训类型必填' }],
  department: [{ required: true, message: '部门必填' }],
  trainingDate: [{ required: true, message: '培训日期必填' }],
}

const passRate = computed(() => {
  const completed = list.value.filter(t => t.examResult)
  if (completed.length === 0) return 0
  const passed = completed.filter(t => t.examResult === 'pass').length
  return Math.round((passed / completed.length) * 100)
})

const filteredList = computed(() => {
  let l = list.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(t => (t.trainingName || '').toLowerCase().includes(q) || (t.instructor || '').toLowerCase().includes(q))
  }
  if (filterType.value) l = l.filter(t => t.trainingType === filterType.value)
  if (filterStatus.value) l = l.filter(t => t.status === filterStatus.value)
  return l
})

const typeTag = t => ({ onboarding: '', skill: 'success', safety: 'warning', service: 'info' }[t] || 'info')
const typeLabel = t => ({ onboarding: '入职培训', skill: '技能培训', safety: '安全培训', service: '服务培训' }[t] || t)
const statusLabel = s => ({ completed: '已完成', in_progress: '进行中', planned: '已计划' }[s] || s)
const examLabel = e => ({ pass: '合格', fail: '不合格', written: '笔试', practical: '实操', combined: '综合' }[e] || e)

function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 180),
    y: Math.min(event.clientY, window.innerHeight - 120),
  }
}

function closeMenu() { ctxMenu.value.visible = false; ctxRow = null }

function ctxDetail() { closeMenu(); detailRow.value = ctxRow; showDetail.value = true }
function ctxEdit() { closeMenu(); editRow(ctxRow) }
function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.trainingName}」？`, '删除确认', {
    confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    try {
      await request.delete(`/hr/training/${ctxRow.id}`)
      ElMessage.success('已删除')
      await fetchData()
    } catch (e) {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

function openAdd() {
  editing.value = false
  form.value = { trainingName: '', trainingType: 'onboarding', department: '', participants: 1, instructor: '', trainingDate: '', examMethod: 'written', content: '' }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveTraining() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await request.put(`/hr/training/${form.value.id}`, form.value)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/training', form.value)
      ElMessage.success('已创建 · Created')
    }
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存培训失败', e)
    ElMessage.error('保存失败')
  } finally { saving.value = false }
}

function clearFilters() { search.value = ''; filterType.value = ''; filterStatus.value = '' }

onMounted(() => {
  fetchData()
  document.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
  closeMenu()
})
</script>

<style scoped>
.hr-page { max-width: 1600px; margin: 0 auto; padding-bottom: 40px; }
.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; padding: 14px 18px; border-radius: 2px; background: var(--color-card); text-align: center; border: 1px solid var(--color-border); }
.st-total { border-color: var(--color-border); }
.st-active { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.st-mgr { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-admin { background: rgba(194,85,85,0.04); border-color: rgba(194,85,85,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }
.data-table { border-radius: 2px; overflow: hidden; }
.status-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 4px; }
.status-dot.completed { background: #4A7C59; }
.status-dot.in_progress { background: #D4A853; }
.status-dot.planned { background: #94a3b8; }
.status-text { font-size: 12px; }
.exam-pass { color: #4A7C59; font-weight: 600; }
.exam-fail { color: #C25555; font-weight: 600; }
.table-footer { margin-top: 10px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }
.ctx-menu { position: fixed; z-index: 9999; background: var(--color-card); border: 1px solid var(--color-border); border-radius: 2px; box-shadow: 0 8px 30px rgba(0,0,0,0.15); padding: 6px; min-width: 150px; }
.ctx-item { padding: 8px 14px; font-size: 13px; cursor: pointer; border-radius: 2px; color: var(--color-text-primary); transition: background 0.1s; }
.ctx-item:hover { background: rgba(45,74,62,0.04); }
.ctx-item.danger { color: #C25555; }
.ctx-item.danger:hover { background: rgba(194,85,85,0.04); }
.ctx-divider { height: 1px; background: var(--color-border); margin: 4px 8px; }
.detail-content { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--color-border); }
.detail-label { font-size: 13px; color: var(--color-text-secondary); min-width: 80px; font-weight: 500; }
.detail-row span:last-child { font-size: 13px; color: var(--color-text-primary); }
</style>
