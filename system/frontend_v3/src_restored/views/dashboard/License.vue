<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">证照管理 · License Management</h1>
        <span class="page-desc">证照档案 · 到期提醒 · 续期管理 · Licenses · Expiry · Renewal</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openAdd"><span>新增证照 · Add</span></el-button>
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
      <el-input v-model="search" placeholder="搜索员工姓名..." class="search-input" clearable />
      <el-select v-model="filterType" placeholder="证照类型" clearable style="width:160px">
        <el-option label="健康证 · Health" value="健康证" />
        <el-option label="食品经营许可证 · Food" value="食品经营许可证" />
        <el-option label="消防证 · Fire" value="消防证" />
        <el-option label="特种作业证 · Special" value="特种作业证" />
        <el-option label="其他 · Other" value="其他" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px">
        <el-option label="有效 · Valid" value="有效" />
        <el-option label="即将到期 · Expiring" value="即将到期" />
        <el-option label="已过期 · Expired" value="已过期" />
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
      <el-table-column prop="staffName" label="员工姓名 · Staff" width="120" />
      <el-table-column prop="licenseType" label="证照类型 · Type" width="150" />
      <el-table-column prop="licenseNo" label="证照编号 · No." width="160" />
      <el-table-column prop="issueDate" label="发证日期 · Issue" width="120" />
      <el-table-column prop="expireDate" label="到期日期 · Expire" width="120" />
      <el-table-column prop="status" label="状态 · Status" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注 · Remark" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条记录 · 右键编辑</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-item" @click="ctxRemind">续期提醒 · Remind</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑证照 · Edit License' : '新增证照 · Add License'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="员工" prop="staffId">
          <el-select v-model="form.staffId" placeholder="选择员工" filterable style="width:100%">
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName + ' (' + s.staffId + ')'" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="证照类型" prop="licenseType">
          <el-select v-model="form.licenseType" placeholder="选择类型" style="width:100%">
            <el-option label="健康证 · Health" value="健康证" />
            <el-option label="食品经营许可证 · Food" value="食品经营许可证" />
            <el-option label="消防证 · Fire" value="消防证" />
            <el-option label="特种作业证 · Special" value="特种作业证" />
            <el-option label="其他 · Other" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="证照编号" prop="licenseNo">
          <el-input v-model="form.licenseNo" placeholder="如: HC2024001" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发证日期" prop="issueDate">
              <el-date-picker v-model="form.issueDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="expireDate">
              <el-date-picker v-model="form.expireDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveLicense" :loading="saving">保存 · Save</el-button>
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
const showDialog = ref(false)
const editing = ref(false)
const search = ref('')
const filterType = ref('')
const filterStatus = ref('')
const formRef = ref(null)

// 员工列表（用于选择）
const staffList = ref([])

// 证照数据
const list = ref([])

// 加载真实数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/license')
    list.value = res.data || []
  } catch (e) {
    console.error('获取证照列表失败', e)
    ElMessage.error('获取证照列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    staffList.value = res.data || []
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0, row: null })
let ctxRow = null

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

function ctxRemind() {
  closeMenu()
  ElMessage.success(`已设置续期提醒 · Reminder set for ${ctxRow.staffName}`)
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.staffName}」的${ctxRow.licenseType}？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.delete(`/hr/license/${ctxRow.id}`)
        ElMessage.success('已删除 · Deleted')
        await fetchData()
      } catch (e) {
        console.error('删除失败', e)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// ---------- form ----------
const form = ref({
  id: null,
  staffId: '',
  licenseType: '',
  licenseNo: '',
  issueDate: '',
  expireDate: '',
  remark: '',
})

const rules = {
  staffId: [{ required: true, message: '请选择员工' }],
  licenseType: [{ required: true, message: '请选择证照类型' }],
  licenseNo: [{ required: true, message: '请输入证照编号' }],
  issueDate: [{ required: true, message: '请选择发证日期' }],
  expireDate: [{ required: true, message: '请选择到期日期' }],
}

// 计算状态
function calcStatus(expireDate) {
  if (!expireDate) return '有效'
  const now = new Date()
  const expire = new Date(expireDate)
  const diffDays = Math.ceil((expire - now) / (1000 * 60 * 60 * 24))
  if (diffDays < 0) return '已过期'
  if (diffDays <= 30) return '即将到期'
  return '有效'
}

const stats = computed(() => {
  const total = list.value.length
  const expiringCount = list.value.filter(l => l.status === '即将到期').length
  const expiredCount = list.value.filter(l => l.status === '已过期').length
  const healthCount = list.value.filter(l => l.licenseType === '健康证').length
  return [
    { label: '证照总数 · Total', value: total, cls: 'st-total' },
    { label: '即将到期 · Expiring', value: expiringCount, cls: 'st-expiring' },
    { label: '已过期 · Expired', value: expiredCount, cls: 'st-expired' },
    { label: '健康证 · Health', value: healthCount, cls: 'st-health' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(item => (item.staffName || '').includes(q))
  }
  if (filterType.value) l = l.filter(item => item.licenseType === filterType.value)
  if (filterStatus.value) l = l.filter(item => item.status === filterStatus.value)
  return l
})

const statusTag = status => {
  if (status === '有效') return 'success'
  if (status === '即将到期') return 'warning'
  if (status === '已过期') return 'danger'
  return 'info'
}

// ---------- actions ----------
function openAdd() {
  editing.value = false
  form.value = { id: null, staffId: '', licenseType: '', licenseNo: '', issueDate: '', expireDate: '', remark: '' }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

async function saveLicense() {
  await formRef.value?.validate().catch(() => false)
  
  saving.value = true
  try {
    const staff = staffList.value.find(s => s.staffId === form.value.staffId)
    const staffName = staff ? staff.staffName : ''
    const status = calcStatus(form.value.expireDate)
    const data = { ...form.value, staffName, status }
    
    if (editing.value) {
      await request.put(`/hr/license/${form.value.id}`, data)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/license', data)
      ElMessage.success('已创建 · Created')
    }
    
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function clearFilters() {
  search.value = ''
  filterType.value = ''
  filterStatus.value = ''
}

onMounted(() => {
  fetchData()
  fetchStaff()
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
.st-expiring { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-expired { background: rgba(194,85,85,0.04); border-color: rgba(194,85,85,0.2); }
.st-health { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.st-expired .stat-num { color: #C25555; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }

.data-table { border-radius: 2px; overflow: hidden; }

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
</style>
