<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">安保保洁 · Security & Cleaning</h1>
        <span class="page-desc">安保管理 · 保洁管理 · 排班巡检 · Security · Cleaning · Patrol</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="showSchedule = true"><span>排班管理 · Schedule</span></el-button>
        <el-button type="primary" @click="openAdd"><span>新增人员 · Add</span></el-button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'security' }" @click="activeTab = 'security'">
        安保管理 · Security
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'cleaning' }" @click="activeTab = 'cleaning'">
        保洁管理 · Cleaning
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :class="s.cls">
        <div class="stat-num">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="filter-bar">
      <el-input v-model="search" placeholder="搜索姓名/手机号..." class="search-input" clearable />
      <el-select v-model="filterShift" placeholder="班次筛选 · Shift" clearable style="width:140px">
        <el-option label="早班 · Morning" value="早班" />
        <el-option label="中班 · Afternoon" value="中班" />
        <el-option label="晚班 · Night" value="晚班" />
      </el-select>
      <el-select v-model="filterArea" placeholder="区域筛选 · Area" clearable style="width:160px">
        <el-option v-for="a in allAreas" :key="a" :label="a" :value="a" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态 · Status" clearable style="width:120px">
        <el-option label="在岗 · On Duty" value="在岗" />
        <el-option label="休息 · Off" value="休息" />
        <el-option label="请假 · Leave" value="请假" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="name" label="姓名 · Name" width="100" />
      <el-table-column prop="position" label="岗位 · Position" width="100">
        <template #default="{ row }">
          <el-tag :type="row.position === '保安' ? 'warning' : 'success'" size="small" effect="plain">
            {{ row.position === '保安' ? '保安 · Security' : '保洁 · Cleaning' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="area" label="负责区域 · Area" width="140" />
      <el-table-column prop="shift" label="班次 · Shift" width="110">
        <template #default="{ row }">
          <span class="shift-tag" :class="shiftClass(row.shift)">{{ shiftLabel(row.shift) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话 · Phone" width="140" />
      <el-table-column prop="status" label="状态 · Status" width="100" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="statusClass(row.status)"></span>
          <span class="status-text">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注 · Remark" min-width="160" />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 人 · 右键编辑</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-item" @click="ctxSchedule">排班 · Schedule</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑人员 · Edit Staff' : '新增人员 · Add Staff'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名 · Name" prop="name">
              <el-input v-model="form.name" placeholder="必填 · Required" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位 · Position" prop="position">
              <el-select v-model="form.position" style="width:100%">
                <el-option label="保安 · Security" value="保安" />
                <el-option label="保洁 · Cleaning" value="保洁" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责区域 · Area">
              <el-input v-model="form.area" placeholder="如: 一楼大厅" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次 · Shift" prop="shift">
              <el-select v-model="form.shift" style="width:100%">
                <el-option label="早班 · Morning" value="早班" />
                <el-option label="中班 · Afternoon" value="中班" />
                <el-option label="晚班 · Night" value="晚班" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系电话 · Phone">
              <el-input v-model="form.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态 · Status">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="在岗 · On Duty" value="在岗" />
                <el-option label="休息 · Off" value="休息" />
                <el-option label="请假 · Leave" value="请假" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注 · Remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveStaff" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 排班对话框 -->
    <el-dialog v-model="showSchedule" title="排班管理 · Schedule" width="500px">
      <div class="schedule-info">
        <p>排班管理功能开发中...</p>
        <p>Schedule management is under development...</p>
      </div>
      <template #footer>
        <el-button @click="showSchedule = false">关闭 · Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('security')
const showDialog = ref(false)
const showSchedule = ref(false)
const editing = ref(false)
const search = ref('')
const filterShift = ref('')
const filterArea = ref('')
const filterStatus = ref('')
const formRef = ref(null)

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0, row: null })
let ctxRow = null

// 安保保洁数据
const securityList = ref([])
const cleaningList = ref([])

// 加载真实数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/security')
    const allData = res.data || []
    securityList.value = allData.filter(item => item.position === '保安')
    cleaningList.value = allData.filter(item => item.position === '保洁')
  } catch (e) {
    console.error('获取安保保洁数据失败', e)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const allList = computed(() => {
  if (activeTab.value === 'security') return securityList.value
  return cleaningList.value
})

const allAreas = computed(() => {
  const areas = new Set(allList.value.map(s => s.area))
  return [...areas]
})

const stats = computed(() => {
  const secCount = securityList.value.length
  const cleanCount = cleaningList.value.length
  const onDuty = allList.value.filter(s => s.status === '在岗').length
  const patrolCount = activeTab.value === 'security' ? 12 : 0
  return [
    { label: '安保人数 · Security', value: secCount, cls: 'st-security' },
    { label: '保洁人数 · Cleaning', value: cleanCount, cls: 'st-cleaning' },
    { label: '今日值班 · On Duty', value: onDuty, cls: 'st-duty' },
    { label: '巡检记录 · Patrol', value: patrolCount, cls: 'st-patrol' },
  ]
})

const filteredList = computed(() => {
  let l = allList.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(s => (s.name || '').includes(q) || (s.phone || '').includes(q))
  }
  if (filterShift.value) l = l.filter(s => s.shift === filterShift.value)
  if (filterArea.value) l = l.filter(s => s.area === filterArea.value)
  if (filterStatus.value) l = l.filter(s => s.status === filterStatus.value)
  return l
})

// form
const defaultForm = () => ({
  name: '',
  position: activeTab.value === 'security' ? '保安' : '保洁',
  area: '',
  shift: '早班',
  phone: '',
  status: '在岗',
  remark: '',
})
const form = ref(defaultForm())

const rules = {
  name: [{ required: true, message: '姓名必填 · Name required' }],
  position: [{ required: true, message: '岗位必选 · Position required' }],
  shift: [{ required: true, message: '班次必选 · Shift required' }],
}

// helpers
const shiftClass = s => ({ '早班': 'shift-morning', '中班': 'shift-afternoon', '晚班': 'shift-night' }[s] || '')
const shiftLabel = s => ({ '早班': '早班 · Morning', '中班': '中班 · Afternoon', '晚班': '晚班 · Night' }[s] || s)
const statusClass = s => ({ '在岗': 'on-duty', '休息': 'off', '请假': 'leave' }[s] || '')
const statusLabel = s => ({ '在岗': '在岗 · On Duty', '休息': '休息 · Off', '请假': '请假 · Leave' }[s] || s)

// right-click menu
function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 180),
    y: Math.min(event.clientY, window.innerHeight - 120),
  }
}

function closeMenu() {
  ctxMenu.value.visible = false
  ctxRow = null
}

function ctxEdit() {
  closeMenu()
  editRow(ctxRow)
}

function ctxSchedule() {
  closeMenu()
  showSchedule.value = true
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.name}」？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.delete(`/hr/security/${ctxRow.id}`)
        ElMessage.success('已删除 · Deleted')
        await fetchData()
      } catch (e) {
        console.error('删除失败', e)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// actions
function openAdd() {
  editing.value = false
  form.value = defaultForm()
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveStaff() {
  await formRef.value?.validate().catch(() => false)
  saving.value = true
  try {
    if (editing.value) {
      await request.put(`/hr/security/${form.value.id}`, form.value)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/security', form.value)
      ElMessage.success('已创建 · Created')
    }
    saving.value = false
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
    saving.value = false
  }
}

function removeStaff(row) {
  const target = activeTab.value === 'security' ? securityList : cleaningList
  const idx = target.value.findIndex(s => s.id === row.id)
  if (idx !== -1) {
    target.value.splice(idx, 1)
    ElMessage.success('已删除 · Deleted')
  }
}

function clearFilters() {
  search.value = ''
  filterShift.value = ''
  filterArea.value = ''
  filterStatus.value = ''
}

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

/* Tabs */
.tab-bar { display: flex; gap: 0; margin-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.tab-item {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  color: var(--color-text-secondary);
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.tab-item:hover { color: var(--color-text-primary); }
.tab-item.active {
  color: var(--color-text-primary);
  border-bottom-color: #4A7C59;
  font-weight: 600;
}

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; padding: 14px 18px; border-radius: 2px; background: var(--color-card); text-align: center; border: 1px solid var(--color-border); }
.st-security { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-cleaning { background: rgba(74,124,89,0.04); border-color: rgba(74,124,89,0.2); }
.st-duty { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.st-patrol { background: rgba(91,123,138,0.04); border-color: rgba(91,123,138,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }

.data-table { border-radius: 2px; overflow: hidden; }

.shift-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 2px;
}
.shift-morning { background: rgba(196,163,90,0.1); color: #C4A35A; }
.shift-afternoon { background: rgba(74,124,89,0.1); color: #4A7C59; }
.shift-night { background: rgba(91,123,138,0.1); color: #5B7B8A; }

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.on-duty { background: #4A7C59; }
.status-dot.off { background: #94a3b8; }
.status-dot.leave { background: #C4A35A; }
.status-text { font-size: 12px; }

.table-footer { margin-top: 10px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }

/* 右键菜单 */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  padding: 6px;
  min-width: 150px;
}
.ctx-item {
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 2px;
  color: var(--color-text-primary);
  transition: background 0.1s;
}
.ctx-item:hover {
  background: rgba(45,74,62,0.04);
}
.ctx-item.danger {
  color: #C25555;
}
.ctx-item.danger:hover {
  background: rgba(194,85,85,0.04);
}
.ctx-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}

.schedule-info {
  padding: 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.schedule-info p { margin: 8px 0; }
</style>
