<template>
  <div class="perm-manager-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">权限管理 · Permission Management</h2>
        <p class="page-desc">角色权限 · 访问控制 · 安全设置</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleSave">保存修改</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">员工总数 · Total</span>
        <span class="stat-value">{{ stats.total }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">管理员 · Admin</span>
        <span class="stat-value">{{ stats.admin }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">主管 · Manager</span>
        <span class="stat-value">{{ stats.manager }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">普通员工 · Staff</span>
        <span class="stat-value">{{ stats.staff }}</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="searchText" placeholder="搜索姓名/工号" clearable style="width: 220px" />
          <el-select v-model="filterLevel" placeholder="权限级别" clearable style="width: 140px">
            <el-option label="普通员工" :value="1" />
            <el-option label="主管" :value="2" />
            <el-option label="部门负责人" :value="3" />
            <el-option label="门店经理" :value="4" />
            <el-option label="超级管理员" :value="99" />
          </el-select>
        </div>
      </div>

      <el-table :data="filteredStaff" stripe style="width: 100%">
        <el-table-column label="员工" min-width="180">
          <template #default="{ row }">
            <div class="staff-cell">
              <span class="staff-avatar">{{ (row.staffName || '?').charAt(0) }}</span>
              <div class="staff-info">
                <span class="staff-name">{{ row.staffName }}</span>
                <span class="staff-account">{{ row.staffAccount }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.roleName || '未设置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="120">
          <template #default="{ row }">
            {{ row.department || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="权限级别" width="120">
          <template #default="{ row }">
            <el-tag :type="levelType(row.permissionLevel)" effect="dark">
              {{ levelName(row.permissionLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showEditor" :title="'编辑权限 - ' + (editingStaff?.staffName || '')" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form v-if="editingStaff" label-width="100px">
        <el-form-item label="权限等级">
          <el-radio-group v-model="editForm.permissionLevel">
            <el-radio-button :value="1">普通员工</el-radio-button>
            <el-radio-button :value="2">主管</el-radio-button>
            <el-radio-button :value="3">部门负责人</el-radio-button>
            <el-radio-button :value="4">门店经理</el-radio-button>
            <el-radio-button :value="99">超级管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="editForm.deptId" placeholder="选择部门" clearable style="width: 100%">
            <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleId" placeholder="选择角色" clearable style="width: 100%">
            <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditor = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const searchText = ref('')
const filterLevel = ref('')
const showEditor = ref(false)
const editingStaff = ref(null)
const editForm = ref({ permissionLevel: 1, deptId: null, roleId: null })

const staffList = ref([])
const departments = ref([])
const roles = ref([])

const stats = computed(() => ({
  total: staffList.value.length,
  admin: staffList.value.filter(s => s.permissionLevel >= 99).length,
  manager: staffList.value.filter(s => s.permissionLevel >= 2 && s.permissionLevel < 99).length,
  staff: staffList.value.filter(s => s.permissionLevel <= 1).length
}))

const filteredStaff = computed(() => {
  return staffList.value.filter(s => {
    if (searchText.value) {
      const k = searchText.value.toLowerCase()
      if (!s.staffName?.toLowerCase().includes(k) && !s.staffAccount?.toLowerCase().includes(k)) return false
    }
    if (filterLevel.value !== '' && s.permissionLevel !== filterLevel.value) return false
    return true
  })
})

const levelName = (lvl) => {
  const map = { 1: '普通员工', 2: '主管', 3: '部门负责人', 4: '门店经理', 99: '超级管理员' }
  return map[lvl] || '未设置'
}

const levelType = (lvl) => {
  if (lvl >= 99) return 'danger'
  if (lvl >= 4) return 'warning'
  if (lvl >= 2) return 'primary'
  return 'info'
}

const openEdit = (row) => {
  editingStaff.value = row
  editForm.value = {
    permissionLevel: row.permissionLevel || 1,
    deptId: row.deptId || null,
    roleId: row.roleId || null
  }
  showEditor.value = true
}

const saveEdit = () => {
  if (editingStaff.value) {
    editingStaff.value.permissionLevel = editForm.value.permissionLevel
    editingStaff.value.deptId = editForm.value.deptId
    editingStaff.value.roleId = editForm.value.roleId
    ElMessage.success('权限已更新')
  }
  showEditor.value = false
}

const handleSave = () => {
  ElMessage.success('权限配置已保存')
}

onMounted(() => {
  // TODO: 加载员工列表、部门、角色
})
</script>

<style scoped>
.perm-manager-page { padding: 24px 32px; }

.page-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-desc { font-size: 13px; color: #8a9a8e; margin: 0; }

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px;
  padding: 18px 20px; display: flex; flex-direction: column; gap: 6px;
}
.stat-label { font-size: 12px; color: #8a9a8e; }
.stat-value { font-size: 28px; font-weight: 700; color: #2D4A3E; }

.content-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px; padding: 20px;
}

.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; flex-wrap: wrap; gap: 12px;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.staff-cell { display: flex; align-items: center; gap: 10px; }
.staff-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: #2D4A3E; color: #fff; display: flex;
  align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.staff-info { display: flex; flex-direction: column; gap: 2px; }
.staff-name { font-size: 14px; font-weight: 500; color: #1a2f23; }
.staff-account { font-size: 12px; color: #a0b0a5; }
</style>
