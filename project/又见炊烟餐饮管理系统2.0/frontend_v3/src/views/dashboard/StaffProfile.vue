<template>
  <div class="staff-profile">
    <!-- 顶部导航 -->
    <div class="profile-header-bar">
      <el-button text @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回员工列表</span>
      </el-button>
      <el-button type="primary" @click="openEditDialog" class="edit-btn">
        <el-icon><Edit /></el-icon>
        <span>编辑档案</span>
      </el-button>
    </div>

    <!-- 员工基本信息卡片 -->
    <el-card class="profile-card" v-loading="loading">
      <div class="profile-basic">
        <div class="avatar-section">
          <div class="avatar-circle">{{ staffNameFirstChar }}</div>
        </div>
        <div class="info-section">
          <h1 class="staff-name">{{ staff.staff_name }}</h1>
          <div class="info-tags">
            <span class="info-item">
              <el-icon><User /></el-icon>
              <span>工号：{{ staff.staff_id }}</span>
            </span>
            <span class="info-item">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ staff.department || '--' }}</span>
            </span>
            <span class="info-item">
              <el-icon><Briefcase /></el-icon>
              <span>{{ staff.staff_position || '--' }}</span>
            </span>
            <el-tag
              :type="statusTagType"
              effect="dark"
              size="large"
              class="status-tag"
            >
              {{ statusLabel }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 锚点导航 + 详情区 -->
    <div class="profile-body">
      <aside class="profile-anchor">
        <el-anchor :offset="80" direction="vertical" container=".profile-body">
          <el-anchor-link href="#section-identity" title="基础身份" />
          <el-anchor-link href="#section-contact" title="联系方式" />
          <el-anchor-link href="#section-employment" title="任职信息" />
          <el-anchor-link href="#section-salary" title="薪酬银行" />
          <el-anchor-link href="#section-cert" title="证书资质" />
          <el-anchor-link href="#section-training" title="培训记录" />
          <el-anchor-link href="#section-reward" title="奖惩记录" />
          <el-anchor-link href="#section-changes" title="人事异动" />
        </el-anchor>
      </aside>

      <main class="profile-detail">
        <!-- 基础身份 -->
        <el-card class="section-card" id="section-identity">
          <template #header>
            <div class="section-header">
              <el-icon><UserFilled /></el-icon>
              <span>基础身份</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="性别">
              {{ staff.staff_gender || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="年龄">
              {{ staff.staff_age != null ? staff.staff_age + ' 岁' : '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="身份证号" :span="2">
              {{ staff.id_card || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="家庭住址" :span="2">
              {{ staff.home_address || '--' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 联系方式 -->
        <el-card class="section-card" id="section-contact">
          <template #header>
            <div class="section-header">
              <el-icon><Phone /></el-icon>
              <span>联系方式</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="手机号">
              {{ staff.staff_phone || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="紧急联系人">
              {{ staff.emergency_contact || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="紧急联系电话" :span="2">
              {{ staff.emergency_phone || '--' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 任职信息 -->
        <el-card class="section-card" id="section-employment">
          <template #header>
            <div class="section-header">
              <el-icon><Calendar /></el-icon>
              <span>任职信息</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="入职日期">
              {{ staff.hire_date || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="用工类型">
              {{ staff.employment_status || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ staff.remark || '--' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 薪酬银行 -->
        <el-card class="section-card" id="section-salary">
          <template #header>
            <div class="section-header">
              <el-icon><Money /></el-icon>
              <span>薪酬银行</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="基本工资">
              {{ staff.monthly_salary != null ? '¥' + Number(staff.monthly_salary).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="银行账户">
              {{ staff.bank_account || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="开户行" :span="2">
              {{ staff.bank_name || '--' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 证书资质 -->
        <el-card class="section-card" id="section-cert">
          <template #header>
            <div class="section-header">
              <el-icon><Medal /></el-icon>
              <span>证书资质</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="健康证号">
              {{ staff.health_cert_no || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="有效期">
              {{ staff.health_cert_expire || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag
                :type="healthCertStatusType"
                size="small"
                v-if="staff.health_cert_no"
              >
                {{ healthCertStatusLabel }}
              </el-tag>
              <span v-else>--</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 培训记录 -->
        <el-card class="section-card" id="section-training">
          <template #header>
            <div class="section-header">
              <el-icon><Reading /></el-icon>
              <span>培训记录</span>
            </div>
          </template>
          <el-empty description="暂无记录" :image-size="80" />
        </el-card>

        <!-- 奖惩记录 -->
        <el-card class="section-card" id="section-reward">
          <template #header>
            <div class="section-header">
              <el-icon><TrophyBase /></el-icon>
              <span>奖惩记录</span>
            </div>
          </template>
          <el-empty description="暂无记录" :image-size="80" />
        </el-card>

        <!-- 人事异动 -->
        <el-card class="section-card" id="section-changes">
          <template #header>
            <div class="section-header">
              <el-icon><Clock /></el-icon>
              <span>人事异动</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              :timestamp="staff.hire_date || '--'"
              placement="top"
              type="success"
            >
              <el-card shadow="hover" class="timeline-card">
                <h4>入职</h4>
                <p>部门：{{ staff.department || '--' }} | 职位：{{ staff.staff_position || '--' }}</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item
              v-if="staff.resign_date"
              :timestamp="staff.resign_date"
              placement="top"
              type="danger"
            >
              <el-card shadow="hover" class="timeline-card">
                <h4>离职</h4>
                <p>原因：{{ staff.resign_reason || '--' }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </main>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑员工档案"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="editForm"
        :rules="formRules"
        label-width="110px"
        class="edit-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="staff_name">
              <el-input v-model="editForm.staff_name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="staff_id">
              <el-input v-model="editForm.staff_id" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="staff_gender">
              <el-select v-model="editForm.staff_gender" placeholder="请选择" style="width:100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="staff_age">
              <el-input-number v-model="editForm.staff_age" :min="16" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="staff_phone">
              <el-input v-model="editForm.staff_phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="staff_position">
              <el-input v-model="editForm.staff_position" placeholder="请输入职位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-input v-model="editForm.department" placeholder="请输入部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="hire_date">
              <el-date-picker
                v-model="editForm.hire_date"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="基本工资" prop="monthly_salary">
              <el-input-number
                v-model="editForm.monthly_salary"
                :min="0"
                :precision="2"
                :step="100"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用工类型" prop="employment_status">
              <el-select v-model="editForm.employment_status" placeholder="请选择" style="width:100%">
                <el-option label="全职" value="全职" />
                <el-option label="兼职" value="兼职" />
                <el-option label="实习" value="实习" />
                <el-option label="劳务派遣" value="劳务派遣" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="id_card">
              <el-input v-model="editForm.id_card" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家庭住址" prop="home_address">
              <el-input v-model="editForm.home_address" placeholder="请输入家庭住址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人" prop="emergency_contact">
              <el-input v-model="editForm.emergency_contact" placeholder="请输入紧急联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系电话" prop="emergency_phone">
              <el-input v-model="editForm.emergency_phone" placeholder="请输入紧急联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-input v-model="editForm.role" placeholder="请输入角色" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="editForm.remark" placeholder="请输入备注" type="textarea" :rows="1" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  User,
  OfficeBuilding,
  Briefcase,
  UserFilled,
  Phone,
  Calendar,
  Money,
  Medal,
  Reading,
  TrophyBase,
  Clock
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const staff = ref({})
const loading = ref(false)
const saving = ref(false)
const editDialogVisible = ref(false)
const formRef = ref(null)

const editForm = reactive({
  staff_id: '',
  staff_name: '',
  staff_account: '',
  staff_gender: '',
  staff_age: null,
  staff_phone: '',
  staff_position: '',
  department: '',
  hire_date: '',
  monthly_salary: null,
  employment_status: '',
  id_card: '',
  home_address: '',
  emergency_contact: '',
  emergency_phone: '',
  role: '',
  remark: ''
})

const formRules = {
  staff_name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  staff_phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }]
}

const staffNameFirstChar = computed(() => {
  const name = staff.value.staff_name || ''
  return name ? name.charAt(0) : '?'
})

const statusLabel = computed(() => {
  const s = staff.value.employment_status || ''
  if (s === '离职') return '已离职'
  if (s === '全职' || s === '兼职' || s === '实习' || s === '劳务派遣') return '在职'
  return s || '未知'
})

const statusTagType = computed(() => {
  return statusLabel.value === '已离职' ? 'info' : 'success'
})

const healthCertStatusType = computed(() => {
  if (!staff.value.health_cert_expire) return ''
  const expire = new Date(staff.value.health_cert_expire)
  return expire > new Date() ? 'success' : 'danger'
})

const healthCertStatusLabel = computed(() => {
  if (!staff.value.health_cert_expire) return '--'
  const expire = new Date(staff.value.health_cert_expire)
  return expire > new Date() ? '有效' : '已过期'
})

function goBack() {
  router.push('/dashboard/hr/staff')
}

async function fetchStaff() {
  const id = route.params.id
  if (!id) {
    ElMessage.error('员工ID不存在')
    router.push('/dashboard/hr/staff')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/api/hr/staff/${id}`)
    staff.value = res.data || res
  } catch (err) {
    ElMessage.error('获取员工信息失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function openEditDialog() {
  Object.assign(editForm, {
    staff_id: staff.value.staff_id || '',
    staff_name: staff.value.staff_name || '',
    staff_account: staff.value.staff_account || '',
    staff_gender: staff.value.staff_gender || '',
    staff_age: staff.value.staff_age ?? null,
    staff_phone: staff.value.staff_phone || '',
    staff_position: staff.value.staff_position || '',
    department: staff.value.department || '',
    hire_date: staff.value.hire_date || '',
    monthly_salary: staff.value.monthly_salary ?? null,
    employment_status: staff.value.employment_status || '',
    id_card: staff.value.id_card || '',
    home_address: staff.value.home_address || '',
    emergency_contact: staff.value.emergency_contact || '',
    emergency_phone: staff.value.emergency_phone || '',
    role: staff.value.role || '',
    remark: staff.value.remark || ''
  })
  editDialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await request.put(`/api/hr/staff/${editForm.staff_id}`, editForm)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await fetchStaff()
  } catch (err) {
    ElMessage.error('保存失败')
    console.error(err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchStaff()
})
</script>

<style scoped>
.staff-profile {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部导航 */
.profile-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  color: #2D4A3E;
  font-size: 15px;
}
.back-btn:hover {
  color: #1a2e25;
}

.edit-btn {
  background: #2D4A3E;
  border-color: #2D4A3E;
}
.edit-btn:hover {
  background: #3a5e4f;
  border-color: #3a5e4f;
}

/* 基本信息卡片 */
.profile-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-card :deep(.el-card__body) {
  padding: 32px;
}

.profile-basic {
  display: flex;
  align-items: center;
  gap: 28px;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2D4A3E, #4a7c65);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 2px;
  box-shadow: 0 4px 14px rgba(45, 74, 62, 0.3);
}

.info-section {
  flex: 1;
}

.staff-name {
  margin: 0 0 12px 0;
  font-size: 26px;
  font-weight: 700;
  color: #2D4A3E;
}

.info-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.info-item .el-icon {
  color: #2D4A3E;
}

.status-tag {
  margin-left: 8px;
}

/* 主体布局 */
.profile-body {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧锚点导航 */
.profile-anchor {
  flex-shrink: 0;
  width: 160px;
  position: sticky;
  top: 80px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-anchor :deep(.el-anchor__list) {
  padding: 0;
}

.profile-anchor :deep(.el-anchor__item) {
  padding: 0;
}

.profile-anchor :deep(.el-anchor__link) {
  padding: 10px 20px;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.profile-anchor :deep(.el-anchor__link:hover) {
  color: #2D4A3E;
}

.profile-anchor :deep(.is-active > .el-anchor__link) {
  color: #2D4A3E;
  font-weight: 600;
  background: rgba(45, 74, 62, 0.06);
  border-right: 3px solid #2D4A3E;
}

/* 右侧详情区 */
.profile-detail {
  flex: 1;
  min-width: 0;
}

.section-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.section-card :deep(.el-card__header) {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
}

.section-card :deep(.el-card__body) {
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
}

.section-header .el-icon {
  font-size: 18px;
}

/* 描述列表 */
:deep(.el-descriptions__label) {
  font-weight: 500;
  color: #909399;
  background: #fafafa;
}

:deep(.el-descriptions__content) {
  color: #303133;
}

/* 时间线 */
.timeline-card {
  margin-bottom: 0;
}

.timeline-card h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #2D4A3E;
}

.timeline-card p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 编辑表单 */
.edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

/* 响应式 */
@media (max-width: 768px) {
  .staff-profile {
    padding: 12px;
  }

  .profile-basic {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-body {
    flex-direction: column;
  }

  .profile-anchor {
    position: static;
    width: 100%;
    display: flex;
    overflow-x: auto;
    padding: 8px 12px;
    white-space: nowrap;
  }

  .profile-anchor :deep(.el-anchor) {
    display: flex;
    flex-direction: row;
    gap: 0;
  }

  .profile-anchor :deep(.el-anchor__item) {
    flex-shrink: 0;
  }

  .profile-anchor :deep(.is-active > .el-anchor__link) {
    border-right: none;
    border-bottom: 3px solid #2D4A3E;
  }

  .info-tags {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
