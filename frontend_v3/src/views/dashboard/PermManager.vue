<template>
  <div class="perm-manager">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>权限管理 · Permission Management</h2>
      <p class="page-desc">角色权限 · 访问控制 · 安全设置</p>
    </div>
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索员工姓名、工号…"
        prefix-icon="Search"
        clearable
        size="large"
        class="search-input"
        @input="onSearch"
      />
    </div>

    <!-- 员工列表 -->
    <div class="staff-list" v-loading="loading">
      <div
        v-for="s in filteredStaff"
        :key="s.staffId"
        class="staff-row"
        @click="openEdit(s)"
      >
        <div class="staff-info">
          <span class="staff-name">{{ s.staffName }}</span>
          <span class="staff-account">{{ s.staffAccount }}</span>
          <span class="staff-dept">{{ s.department || '未分配' }}</span>
        </div>
        <div class="staff-meta">
          <el-tag :type="levelTag(s.permissionLevel)" size="small">
            {{ levelName(s.permissionLevel) }}
          </el-tag>
          <span class="edit-hint">点击编辑 →</span>
        </div>
      </div>
      <el-empty v-if="!loading && filteredStaff.length === 0" description="无匹配员工" />
    </div>

    <!-- 权限编辑弹窗 -->
    <el-dialog
      v-model="showEditor"
      :title="'编辑权限 - ' + editingStaff?.staffName"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form v-if="editingStaff" label-width="100px" class="perm-form">
        <!-- 权限等级 -->
        <el-form-item label="权限等级">
          <el-radio-group v-model="editForm.permissionLevel">
            <el-radio-button :value="1">普通员工</el-radio-button>
            <el-radio-button :value="2">主管</el-radio-button>
            <el-radio-button :value="3">部门负责人</el-radio-button>
            <el-radio-button :value="4">门店经理</el-radio-button>
            <el-radio-button :value="99">超级管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 所属部门 -->
        <el-form-item label="所属部门">
          <el-select v-model="editForm.deptId" placeholder="选择部门" clearable style="width:100%">
            <el-option
              v-for="d in deptList"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>

        <!-- 所属门店 -->
        <el-form-item label="所属门店">
          <el-select v-model="editForm.storeId" style="width:100%">
            <el-option :value="1" label="宁国店" />
            <el-option :value="2" label="宣城店" />
          </el-select>
        </el-form-item>

        <el-divider>模块权限</el-divider>

        <el-form-item label="厨房管理">
          <el-switch v-model="editForm.canManageKitchen" />
        </el-form-item>
        <el-form-item label="销售管理">
          <el-switch v-model="editForm.canManageSales" />
        </el-form-item>
        <el-form-item label="财务管理">
          <el-switch v-model="editForm.canManageFinance" />
        </el-form-item>
        <el-form-item label="人事管理">
          <el-switch v-model="editForm.canManageHr" />
        </el-form-item>
        <el-form-item label="跨店查看">
          <el-switch v-model="editForm.canViewAllStores" />
        </el-form-item>
        <el-form-item label="系统配置">
          <el-switch v-model="editForm.canEditSystem" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditor = false">取消</el-button>
        <el-button type="primary" @click="savePerm" :loading="saving">确定保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const searchText = ref('')
const loading = ref(false)
const saving = ref(false)
const allStaff = ref([])
const showEditor = ref(false)
const editingStaff = ref(null)
const editForm = ref({
  permissionLevel: 1,
  deptId: null,
  storeId: 1,
  canManageKitchen: false,
  canManageSales: false,
  canManageFinance: false,
  canManageHr: false,
  canViewAllStores: false,
  canEditSystem: false
})

const deptList = [
  { id: 1, name: '高层管理部' },
  { id: 2, name: '销售部' },
  { id: 3, name: '厨房部' },
  { id: 4, name: '财务部' },
  { id: 5, name: '楼面部' },
  { id: 6, name: '人事部' },
  { id: 7, name: '宴会统筹部' },
  { id: 8, name: '采购部' },
  { id: 9, name: '库管部' }
]

const filteredStaff = computed(() => {
  const q = searchText.value.toLowerCase()
  if (!q) return allStaff.value
  return allStaff.value.filter(s =>
    (s.staffName || '').includes(q) ||
    (s.staffAccount || '').toLowerCase().includes(q) ||
    (s.department || '').includes(q)
  )
})

function levelName(lv) {
  const m = { 0: '游客', 1: '员工', 2: '主管', 3: '负责人', 4: '店长', 99: '超管' }
  return m[lv] || '未知'
}
function levelTag(lv) {
  const m = { 0: 'info', 1: '', 2: 'success', 3: 'warning', 4: 'primary', 99: 'danger' }
  return m[lv] || 'info'
}

async function fetchStaff() {
  loading.value = true
  try {
    const res = await request({ url: '/hr/staff', method: 'get' })
    if (res.code === 200) {
      allStaff.value = res.data?.rows || res.data?.list || res.data || []
    }
  } catch (e) {
    ElMessage.error('获取员工列表失败')
  }
  loading.value = false
}

function onSearch() {
  // debounce handled by computed
}

function openEdit(staff) {
  editingStaff.value = staff
  editForm.value = {
    permissionLevel: staff.permissionLevel || 1,
    deptId: staff.deptId || null,
    storeId: staff.storeId || 1,
    canManageKitchen: !!staff.canManageKitchen,
    canManageSales: !!staff.canManageSales,
    canManageFinance: !!staff.canManageFinance,
    canManageHr: !!staff.canManageHr,
    canViewAllStores: !!staff.canViewAllStores,
    canEditSystem: !!staff.canEditSystem
  }
  showEditor.value = true
}

async function savePerm() {
  if (!editingStaff.value) return
  saving.value = true
  try {
    const res = await request({
      url: '/hr/staff/' + editingStaff.value.staffId + '/permissions',
      method: 'put',
      data: {
        ...editForm.value
      }
    })
    if (res.code === 200) {
      ElMessage.success('权限已更新')
      showEditor.value = false
      fetchStaff() // refresh
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

onMounted(fetchStaff)
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 13px;
  color: #666;
  margin: 0;
}

.perm-manager {
  min-height: 400px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-input {
  max-width: 360px;
}

.staff-list {
  max-height: 55vh;
  overflow-y: auto;
}

.staff-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s;
}
.staff-row:hover {
  background: #f8fafc;
}

.staff-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.staff-name {
  font-weight: 600;
  font-size: 14px;
}
.staff-account {
  font-size: 12px;
  color: #94a3b8;
}
.staff-dept {
  font-size: 12px;
  color: #64748b;
}

.staff-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.edit-hint {
  font-size: 12px;
  color: #cbd5e1;
}

.perm-form {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
