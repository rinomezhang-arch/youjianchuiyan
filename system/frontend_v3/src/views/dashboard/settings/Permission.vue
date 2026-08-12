<template>
  <div class="permission-page">
    <div class="page-header">
      <h2 class="page-title">权限管理</h2>
      <p class="page-subtitle">Permission Management · 用户角色与权限配置</p>
    </div>

    <el-tabs v-model="activeTab" class="permission-tabs">
      <el-tab-pane label="用户列表" name="users">
        <div class="tab-content">
          <div class="toolbar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索用户姓名..."
              prefix-icon="Search"
              clearable
              style="width: 240px"
            />
            <el-button type="primary" @click="showAddUser">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              添加用户
            </el-button>
          </div>

          <el-table :data="filteredUsers" border stripe style="width: 100%">
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="role" label="角色" width="150" />
            <el-table-column prop="level" label="权限级别" width="120">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="permissions" label="权限范围">
              <template #default="{ row }">
                <el-tag v-for="perm in row.permissions" :key="perm" size="small" type="info" style="margin-right:4px">{{ perm }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="editUser(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="角色管理" name="roles">
        <div class="tab-content">
          <div class="role-grid">
            <div v-for="role in roles" :key="role.name" class="role-card">
              <div class="role-header">
                <div class="role-icon" :style="{ background: role.bgColor }">
                  <svg viewBox="0 0 24 24" fill="none" :stroke="role.color" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                  </svg>
                </div>
                <div class="role-title">
                  <h4>{{ role.name }}</h4>
                  <p>{{ role.count }} 人</p>
                </div>
              </div>
              <div class="role-permissions">
                <el-tag v-for="perm in role.permissions" :key="perm" size="small" type="info" style="margin:2px">{{ perm }}</el-tag>
              </div>
              <div class="role-actions">
                <el-button size="small" @click="editRole(role)">编辑权限</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审批设置" name="approval">
        <div class="tab-content">
          <div class="approval-section">
            <h3 class="section-title">审批开关</h3>
            <div class="approval-switches">
              <div class="switch-item">
                <div class="switch-label">
                  <div class="switch-name">请假审批</div>
                  <div class="switch-desc">员工请假需主管审批</div>
                </div>
                <el-switch v-model="approvalSettings.leave" />
              </div>
              <div class="switch-item">
                <div class="switch-label">
                  <div class="switch-name">报销审批</div>
                  <div class="switch-desc">费用报销需店长审批</div>
                </div>
                <el-switch v-model="approvalSettings.expense" />
              </div>
              <div class="switch-item">
                <div class="switch-label">
                  <div class="switch-name">采购审批</div>
                  <div class="switch-desc">采购订单需财务审批</div>
                </div>
                <el-switch v-model="approvalSettings.purchase" />
              </div>
              <div class="switch-item">
                <div class="switch-label">
                  <div class="switch-name">折扣审批</div>
                  <div class="switch-desc">账单折扣需经理审批</div>
                </div>
                <el-switch v-model="approvalSettings.discount" />
              </div>
            </div>
          </div>

          <div class="approval-section">
            <h3 class="section-title">审批流程配置</h3>
            <div class="flow-config">
              <div class="flow-item">
                <div class="flow-label">请假审批流程</div>
                <div class="flow-steps">
                  <div class="flow-step">员工提交</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">主管审批</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step" v-if="approvalSettings.leaveTwoLevel">店长审批</div>
                  <div class="flow-arrow" v-if="approvalSettings.leaveTwoLevel">→</div>
                  <div class="flow-step">完成</div>
                </div>
                <el-checkbox v-model="approvalSettings.leaveTwoLevel" style="margin-top:8px">
                  超过3天需店长审批
                </el-checkbox>
              </div>

              <div class="flow-item">
                <div class="flow-label">报销审批流程</div>
                <div class="flow-steps">
                  <div class="flow-step">员工提交</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">主管审批</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">财务审批</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">完成</div>
                </div>
              </div>

              <div class="flow-item">
                <div class="flow-label">采购审批流程</div>
                <div class="flow-steps">
                  <div class="flow-step">提交申请</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">采购主管</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step">财务审批</div>
                  <div class="flow-arrow">→</div>
                  <div class="flow-step" v-if="approvalSettings.purchaseHighLevel">总经理审批</div>
                  <div class="flow-arrow" v-if="approvalSettings.purchaseHighLevel">→</div>
                  <div class="flow-step">完成</div>
                </div>
                <el-checkbox v-model="approvalSettings.purchaseHighLevel" style="margin-top:8px">
                  金额超过5000元需总经理审批
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('users')
const searchQuery = ref('')

const users = ref([
  { id: 1, name: '张三', role: '超级管理员', level: '最高', permissions: ['全部权限'] },
  { id: 2, name: '李四', role: '店长', level: '高级', permissions: ['门店管理', '人事管理', '财务报表'] },
  { id: 3, name: '王五', role: '前厅主管', level: '中级', permissions: ['预订管理', '桌台管理', '客户管理'] },
  { id: 4, name: '赵六', role: '后厨主管', level: '中级', permissions: ['菜单管理', '库存管理', '采购管理'] },
  { id: 5, name: '钱七', role: '收银员', level: '基础', permissions: ['收银', '账单查询'] },
  { id: 6, name: '孙八', role: '服务员', level: '基础', permissions: ['点菜', '桌台查看'] },
])

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  return users.value.filter(u => u.name.includes(searchQuery.value))
})

const roles = ref([
  { 
    name: '超级管理员', 
    count: 1, 
    color: '#2D4A3E', 
    bgColor: 'rgba(45,74,62,0.06)',
    permissions: ['全部权限', '系统设置', '用户管理', '数据导出']
  },
  { 
    name: '店长', 
    count: 2, 
    color: '#4A7C59', 
    bgColor: 'rgba(74,124,89,0.06)',
    permissions: ['门店管理', '人事管理', '财务报表', '审批管理']
  },
  { 
    name: '前厅主管', 
    count: 3, 
    color: '#C4A35A', 
    bgColor: 'rgba(196,163,90,0.06)',
    permissions: ['预订管理', '桌台管理', '客户管理', '员工排班']
  },
  { 
    name: '后厨主管', 
    count: 2, 
    color: '#5B7B8A', 
    bgColor: 'rgba(91,123,138,0.06)',
    permissions: ['菜单管理', '库存管理', '采购管理', '厨房出品']
  },
  { 
    name: '收银员', 
    count: 4, 
    color: '#8B9A8C', 
    bgColor: 'rgba(139,154,140,0.06)',
    permissions: ['收银', '账单查询', '退款申请']
  }
])

const approvalSettings = reactive({
  leave: true,
  expense: true,
  purchase: true,
  discount: true,
  leaveTwoLevel: true,
  purchaseHighLevel: false
})

function getLevelType(level) {
  const map = { '最高': 'danger', '高级': 'warning', '中级': '', '基础': 'info' }
  return map[level] || 'info'
}

function showAddUser() {
  ElMessage.info('添加用户功能待实现')
}

function editUser(row) {
  ElMessage.info(`编辑用户: ${row.name}`)
}

function deleteUser(row) {
  ElMessageBox.confirm(`确定删除用户 ${row.name} 吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const idx = users.value.findIndex(u => u.id === row.id)
    if (idx > -1) users.value.splice(idx, 1)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function editRole(role) {
  ElMessage.info(`编辑角色: ${role.name}`)
}

onMounted(async () => {
  try {
    const res = await fetch('/api/hr/staff')
    if (res.ok) {
      const data = await res.json()
      if (data && data.length) {
        users.value = data.map((s, i) => ({
          id: s.id || i,
          name: s.staffName || s.name,
          role: s.staffPosition || s.role || '员工',
          level: s.level || '基础',
          permissions: s.permissions || ['基础权限']
        }))
      }
    }
  } catch (e) {
    // 使用模拟数据
  }
})
</script>

<style scoped>
.permission-page {
  max-width: 1400px;
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

.permission-tabs {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.tab-content {
  margin-top: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.role-card {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all 0.2s;
}

.role-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.role-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.role-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-icon svg {
  width: 22px;
  height: 22px;
}

.role-title h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 2px 0;
}

.role-title p {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.role-permissions {
  margin-bottom: 16px;
  min-height: 40px;
}

.role-actions {
  display: flex;
  justify-content: flex-end;
}

.approval-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.approval-switches {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
}

.switch-label {
  flex: 1;
}

.switch-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.switch-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.flow-config {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.flow-item {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 20px;
}

.flow-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}

.flow-steps {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.flow-step {
  padding: 8px 16px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text);
}

.flow-arrow {
  font-size: 18px;
  color: var(--color-text-muted);
}
</style>
