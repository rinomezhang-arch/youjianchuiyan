<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">人事管理 · HR Management</h1>
        <span class="page-desc">组织架构 · 员工档案 · 权限配置 · Organization · Staff · Permissions</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="showDeptTree = true"><span>组织架构 · Org</span></el-button>
        <el-button type="primary" @click="openAdd"><span>新增员工 · Add</span></el-button>
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
      <el-input v-model="search" placeholder="搜索姓名/账号/手机号..." class="search-input" clearable />
      <el-select v-model="filterDept" placeholder="部门筛选" clearable style="width:160px">
        <el-option v-for="d in allDepts" :key="d.deptId" :label="d.deptName" :value="d.deptName" />
      </el-select>
      <el-select v-model="filterRole" placeholder="角色筛选" clearable style="width:140px">
        <el-option label="超级管理员 · Super Admin" value="super_admin" />
        <el-option label="店长 · Manager" value="store_manager" />
        <el-option label="部门负责人 · Head" value="dept_head" />
        <el-option label="主管 · Supervisor" value="dept_supervisor" />
        <el-option label="普通员工 · Staff" value="staff" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px">
        <el-option label="在职 · Active" value="active" />
        <el-option label="停用 · Inactive" value="inactive" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      :default-sort="{ prop: 'staffId', order: 'ascending' }"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="staffId" label="工号 · ID" width="80" sortable />
      <el-table-column prop="staffName" label="姓名 · Name" width="100" />
      <el-table-column prop="staffAccount" label="账号 · Account" width="110" />
      <el-table-column prop="staffPhone" label="手机号 · Phone" width="130" />
      <el-table-column prop="department" label="部门 · Dept" width="120" />
      <el-table-column prop="staffPosition" label="职位 · Position" width="130" />
      <el-table-column prop="role" label="角色 · Role" width="110">
        <template #default="{ row }">
          <el-tag :type="roleTag(row.role)" size="small" effect="plain">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permissionLevel" label="权限 · Lv" width="70" align="center">
        <template #default="{ row }">
          <span class="perm-level" :class="'perm-' + row.permissionLevel">{{ row.permissionLevel }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="employmentStatus" label="状态 · Status" width="90" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="row.employmentStatus === 'active' ? 'active' : 'inactive'"></span>
          <span class="status-text">{{ row.employmentStatus === 'active' ? '在职' : '停用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="数据权限 · Data Perms" min-width="180">
        <template #default="{ row }">
          <div class="perm-tags">
            <span v-if="row.canViewAllStores" class="ptag all">全店</span>
            <span v-if="row.canEditSystem" class="ptag sys">系统</span>
            <span v-if="row.canManageKitchen" class="ptag k">厨房</span>
            <span v-if="row.canManageSales" class="ptag s">销售</span>
            <span v-if="row.canManageFinance" class="ptag f">财务</span>
            <span v-if="row.canManageHr" class="ptag hr">人事</span>
          </div>
        </template>
      </el-table-column>
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
      <div class="ctx-item" @click="ctxEdit">编辑资料 · Edit</div>
      <div class="ctx-item" @click="ctxPerms">编辑权限 · Perms</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑员工 · Edit Staff' : '新增员工 · Add Staff'" width="700px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="姓名" prop="staffName"><el-input v-model="form.staffName" placeholder="必填" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="账号" prop="staffAccount"><el-input v-model="form.staffAccount" placeholder="登录用" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="密码"><el-input v-model="form.staffPassword" type="password" :placeholder="editing ? '留空不修改' : '默认123456'" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="手机号"><el-input v-model="form.staffPhone" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.staffGender" style="width:100%">
                <el-option label="男 · Male" value="男" />
                <el-option label="女 · Female" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄"><el-input-number v-model="form.staffAge" :min="16" :max="80" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-tree-select
                v-model="form.department"
                :data="deptTreeData"
                :props="{ label: 'deptName', value: 'deptName', children: 'children' }"
                placeholder="选择部门"
                check-strictly
                filterable
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位"><el-input v-model="form.staffPosition" placeholder="如: 包厢服务员" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="角色">
              <el-select v-model="form.role" style="width:100%">
                <el-option label="超级管理员" value="super_admin" />
                <el-option label="店长" value="store_manager" />
                <el-option label="部门负责人" value="dept_head" />
                <el-option label="主管" value="dept_supervisor" />
                <el-option label="普通员工" value="staff" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权限等级">
              <el-input-number v-model="form.permissionLevel" :min="0" :max="99" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.employmentStatus" style="width:100%">
                <el-option label="在职" value="active" />
                <el-option label="停用" value="inactive" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">数据权限 · Data Permissions</el-divider>
        <el-checkbox-group v-model="form.dataPerms" class="perm-checkboxes">
          <el-checkbox label="canViewAllStores">查看全部门店</el-checkbox>
          <el-checkbox label="canEditSystem">系统配置权限</el-checkbox>
          <el-checkbox label="canManageKitchen">厨房管理</el-checkbox>
          <el-checkbox label="canManageSales">销售管理</el-checkbox>
          <el-checkbox label="canManageFinance">财务管理</el-checkbox>
          <el-checkbox label="canManageHr">人事管理</el-checkbox>
        </el-checkbox-group>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveStaff" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 权限编辑对话框 -->
    <el-dialog v-model="showPermDialog" title="权限配置 · Permission Settings" width="480px">
      <el-form label-width="100px">
        <el-form-item label="角色">
          <el-select v-model="permForm.role" style="width:100%">
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="店长" value="store_manager" />
            <el-option label="部门负责人" value="dept_head" />
            <el-option label="主管" value="dept_supervisor" />
            <el-option label="普通员工" value="staff" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限等级">
          <el-input-number v-model="permForm.permissionLevel" :min="0" :max="99" style="width:100%" />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">数据模块权限 · Module Permissions</el-divider>
      <div class="perm-grid">
        <el-checkbox v-model="permForm.canViewAllStores" border>全部门店</el-checkbox>
        <el-checkbox v-model="permForm.canEditSystem" border>系统配置</el-checkbox>
        <el-checkbox v-model="permForm.canManageKitchen" border>厨房管理</el-checkbox>
        <el-checkbox v-model="permForm.canManageSales" border>销售管理</el-checkbox>
        <el-checkbox v-model="permForm.canManageFinance" border>财务管理</el-checkbox>
        <el-checkbox v-model="permForm.canManageHr" border>人事管理</el-checkbox>
      </div>
      <template #footer>
        <el-button @click="showPermDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="savePerms" :loading="permSaving">应用 · Apply</el-button>
      </template>
    </el-dialog>

    <!-- 组织架构树对话框 -->
    <el-dialog v-model="showDeptTree" title="组织架构 · Organization" width="500px">
      <el-tree :data="deptTreeData" :props="{ label: 'deptName', children: 'children' }" node-key="deptId" default-expand-all>
        <template #default="{ node, data }">
          <div class="dept-node">
            <span>{{ data.deptName }}</span>
            <span class="dept-meta" v-if="data.deptCode">{{ data.deptCode }}</span>
          </div>
        </template>
      </el-tree>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStaffList, getDepartments, createStaff, updateStaff, deleteStaff, getStaffStats } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const permSaving = ref(false)
const list = ref([])
const allDepts = ref([])
const showDialog = ref(false)
const showPermDialog = ref(false)
const showDeptTree = ref(false)
const editing = ref(false)
const search = ref('')
const filterDept = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const formRef = ref(null)

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
function ctxPerms() {
  closeMenu()
  editPerms(ctxRow)
}
function ctxDelete() {
  closeMenu()
  if (ctxRow.permissionLevel >= 99) {
    ElMessage.warning('不能删除超级管理员')
    return
  }
  ElMessageBox.confirm(`确定删除「${ctxRow.staffName}」？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(() => removeStaff(ctxRow))
    .catch(() => {})
}

// ---------- form ----------
const form = ref({
  staffName: '', staffAccount: '', staffPassword: '', staffPhone: '',
  staffGender: '', staffAge: null, staffPosition: '', department: '',
  role: 'staff', permissionLevel: 1, employmentStatus: 'active',
  dataPerms: [],
})

const permForm = ref({ staffId: null, role: 'staff', permissionLevel: 1, canViewAllStores: false, canEditSystem: false, canManageKitchen: false, canManageSales: false, canManageFinance: false, canManageHr: false })

const rules = {
  staffName: [{ required: true, message: '姓名必填' }],
  staffAccount: [{ required: true, message: '账号必填' }],
}

const stats = computed(() => {
  const total = list.value.length
  const activeCount = list.value.filter(s => s.employmentStatus === 'active').length
  const adminCount = list.value.filter(s => s.permissionLevel >= 99).length
  const mgrCount = list.value.filter(s => s.permissionLevel >= 3 && s.permissionLevel < 99).length
  return [
    { label: '员工总数 · Total', value: total, cls: 'st-total' },
    { label: '在职 · Active', value: activeCount, cls: 'st-active' },
    { label: '管理层 · Manager', value: mgrCount, cls: 'st-mgr' },
    { label: '超管 · Admin', value: adminCount, cls: 'st-admin' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (search.value) {
    const q = search.value.toLowerCase()
    l = l.filter(s => (s.staffName || '').includes(q) || (s.staffAccount || '').includes(q) || (s.staffPhone || '').includes(q) || (s.staffPosition || '').includes(q))
  }
  if (filterDept.value) l = l.filter(s => s.department === filterDept.value)
  if (filterRole.value) l = l.filter(s => s.role === filterRole.value)
  if (filterStatus.value) l = l.filter(s => s.employmentStatus === filterStatus.value)
  return l
})

const deptTreeData = computed(() => {
  const map = {}
  const roots = []
  allDepts.value.forEach(d => { map[d.deptId] = { ...d, children: [] } })
  allDepts.value.forEach(d => {
    if (d.parentId && map[d.parentId]) {
      map[d.parentId].children.push(map[d.deptId])
    } else {
      roots.push(map[d.deptId])
    }
  })
  return roots
})

const roleTag = r => ({ super_admin: 'danger', store_manager: 'warning', dept_head: '', dept_supervisor: 'info', staff: 'info' }[r] || 'info')
const roleLabel = r => ({ super_admin: '超管', store_manager: '店长', dept_head: '负责人', dept_supervisor: '主管', staff: '员工', admin: '管理员' }[r] || r)

// ---------- API ----------
async function loadData() {
  loading.value = true
  try {
    const [staffRes, deptRes] = await Promise.all([
      getStaffList(),
      getDepartments()
    ])
    if (staffRes.code === 200) list.value = staffRes.data || []
    if (deptRes.code === 200) allDepts.value = deptRes.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载数据失败')
  }
  finally { loading.value = false }
}

function openAdd() {
  editing.value = false
  form.value = { staffName: '', staffAccount: '', staffPassword: '', staffPhone: '', staffGender: '', staffAge: null, staffPosition: '', department: '', role: 'staff', permissionLevel: 1, employmentStatus: 'active', dataPerms: [] }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  const perms = ['canViewAllStores', 'canEditSystem', 'canManageKitchen', 'canManageSales', 'canManageFinance', 'canManageHr'].filter(k => row[k])
  form.value = { ...row, staffPassword: '', dataPerms: perms }
  showDialog.value = true
}

async function saveStaff() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = { ...form.value }
    const perms = payload.dataPerms || []
    payload.canViewAllStores = perms.includes('canViewAllStores')
    payload.canEditSystem = perms.includes('canEditSystem')
    payload.canManageKitchen = perms.includes('canManageKitchen')
    payload.canManageSales = perms.includes('canManageSales')
    payload.canManageFinance = perms.includes('canManageFinance')
    payload.canManageHr = perms.includes('canManageHr')
    delete payload.dataPerms
    if (editing.value && (!payload.staffPassword || payload.staffPassword === '')) delete payload.staffPassword
    if (!editing.value && (!payload.staffPassword || payload.staffPassword === '')) payload.staffPassword = '123456'

    let res
    if (editing.value) {
      res = await updateStaff(payload.staffId, payload)
    } else {
      res = await createStaff(payload)
    }
    if (res.code === 200) {
      ElMessage.success(editing.value ? '已更新 · Updated' : '已创建 · Created')
      showDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    saving.value = false
  }
}

async function removeStaff(row) {
  try {
    const res = await deleteStaff(row.staffId)
    if (res.code === 200) { ElMessage.success('已删除'); loadData() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { ElMessage.error('网络错误') }
}

function editPerms(row) {
  permForm.value = {
    staffId: row.staffId,
    role: row.role,
    permissionLevel: row.permissionLevel || 1,
    canViewAllStores: row.canViewAllStores || false,
    canEditSystem: row.canEditSystem || false,
    canManageKitchen: row.canManageKitchen || false,
    canManageSales: row.canManageSales || false,
    canManageFinance: row.canManageFinance || false,
    canManageHr: row.canManageHr || false,
  }
  showPermDialog.value = true
}

async function savePerms() {
  permSaving.value = true
  try {
    const { staffId, ...payload } = permForm.value
    const res = await updateStaff(staffId, payload)
    if (res.code === 200) {
      ElMessage.success('权限已更新 · Updated')
      showPermDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    permSaving.value = false
  }
}

function clearFilters() { search.value = ''; filterDept.value = ''; filterRole.value = ''; filterStatus.value = '' }

onMounted(() => {
  loadData()
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
.st-admin .stat-num { color: #C25555; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.search-input { width: 240px; }

.data-table { border-radius: 2px; overflow: hidden; }
.perm-level { font-weight: 700; font-size: 14px; }
.perm-1 { color: #94a3b8; }
.perm-2 { color: #6366f1; }
.perm-3 { color: #2563eb; }
.perm-4 { color: #d97706; }
.perm-99 { color: #C25555; font-size: 16px; }

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.active { background: #4A7C59; }
.status-dot.inactive { background: #94a3b8; }
.status-text { font-size: 12px; }

.perm-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.ptag { padding: 1px 6px; border-radius: 2px; font-size: 10px; font-weight: 600; }
.ptag.all { background: rgba(196,163,90,0.1); color: #C4A35A; }
.ptag.sys { background: rgba(194,85,85,0.1); color: #C25555; }
.ptag.k { background: rgba(196,163,90,0.1); color: #C4A35A; }
.ptag.s { background: rgba(45,74,62,0.1); color: #2D4A3E; }
.ptag.f { background: rgba(74,124,89,0.1); color: #4A7C59; }
.ptag.hr { background: rgba(91,123,138,0.1); color: #5B7B8A; }

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

.perm-checkboxes { display: flex; gap: 16px; flex-wrap: wrap; }
.perm-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }

.dept-node { display: flex; align-items: center; gap: 8px; }
.dept-meta { font-size: 11px; color: var(--color-text-secondary); }
</style>
