<template>
  <div class="self-service-page">
    <div class="ss-container">
      <!-- 顶部 -->
      <div class="ss-header">
        <div class="ss-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <h1 class="ss-title">又见炊烟 · 员工自助入职登记</h1>
        <p class="ss-subtitle">Self-Service Onboarding</p>
      </div>

      <!-- 提交成功 -->
      <div v-if="submitted" class="ss-card ss-success">
        <div class="success-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <h2>提交成功</h2>
        <p>您的入职信息已提交，请等待 HR 审核</p>
        <p class="ss-tip">审核通过后将通知您入职安排</p>
        <el-button type="primary" @click="resetAll" class="ss-reset-btn">继续登记 · Continue</el-button>
      </div>

      <!-- 阶段一：欢迎页 -->
      <div v-else-if="stage === 'landing'" class="ss-card ss-landing">
        <p class="landing-desc">欢迎加入又见炊烟。请选择您要办理的事项：</p>
        <div class="landing-actions">
          <div class="landing-btn" @click="goJobList">
            <div class="landing-btn-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
            </div>
            <div class="landing-btn-text">
              <strong>查看岗位信息 · Browse Jobs</strong>
              <span>新员工入职，先看看在招岗位</span>
            </div>
            <span class="landing-btn-arrow">›</span>
          </div>
          <div class="landing-btn" @click="startUpdateFlow">
            <div class="landing-btn-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </div>
            <div class="landing-btn-text">
              <strong>老员工信息更新 · Update Info</strong>
              <span>已入职员工，补充或修改资料</span>
            </div>
            <span class="landing-btn-arrow">›</span>
          </div>
        </div>
      </div>

      <!-- 阶段二：岗位列表 -->
      <div v-else-if="stage === 'jobList'" class="ss-card ss-joblist">
        <div class="stage-nav">
          <button class="back-link" @click="stage = 'landing'">‹ 返回</button>
          <span class="stage-nav-title">在招岗位 · Open Positions</span>
        </div>
        <div v-if="jobsLoading" class="job-empty">加载中...</div>
        <div v-else-if="jobs.length === 0" class="job-empty">
          暂无在招岗位，您也可以直接登记
          <el-button text type="primary" @click="startNewFlow(null)">直接登记 · Apply Anyway</el-button>
        </div>
        <div v-else class="job-cards">
          <div v-for="job in jobs" :key="job.id" class="job-card">
            <div class="job-card-head">
              <strong>{{ job.position }}</strong>
              <span class="job-dept">{{ job.department }}</span>
            </div>
            <div class="job-card-meta">
              <span v-if="job.salaryRange">💰 {{ job.salaryRange }}</span>
              <span v-if="job.workTime">🕐 {{ job.workTime }}</span>
              <span>👥 招{{ job.headcount }}人</span>
            </div>
            <p v-if="job.requirements" class="job-req">{{ job.requirements }}</p>
            <p v-if="job.description" class="job-desc">{{ job.description }}</p>
            <el-button type="primary" class="job-join-btn" @click="startNewFlow(job)">加入 · Apply</el-button>
          </div>
        </div>
      </div>

      <!-- 阶段三：分步表单 -->
      <div v-else-if="stage === 'wizard'" class="ss-card ss-wizard">
        <div class="stage-nav">
          <button class="back-link" @click="prevStep">‹ 上一步</button>
          <span class="stage-nav-title">第 {{ stepIndex + 1 }} / {{ totalSteps }} 步</span>
        </div>
        <div class="step-dots">
          <span v-for="i in totalSteps" :key="i" class="step-dot" :class="{ active: i - 1 <= stepIndex }"></span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="wizard-form">
          <!-- Step 0: 姓名 + 手机号 -->
          <div v-show="stepIndex === 0" class="wizard-step">
            <h3 class="step-title">基本信息 · Your Name & Phone</h3>
            <el-form-item label="姓名 · Name" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" maxlength="20" show-word-limit size="large" />
            </el-form-item>
            <el-form-item label="手机号 · Phone" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" size="large" />
            </el-form-item>
          </div>

          <!-- Step 1: 头像 -->
          <div v-show="stepIndex === 1" class="wizard-step">
            <h3 class="step-title">头像照片 · Photo</h3>
            <div class="avatar-upload">
              <div class="avatar-preview" @click="triggerAvatarPick">
                <img v-if="form.avatarUrl" :src="form.avatarUrl" />
                <div v-else class="avatar-placeholder">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
                  <span>点击上传</span>
                </div>
                <div v-if="avatarUploading" class="avatar-uploading">上传中...</div>
              </div>
              <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
              <p class="avatar-hint">选填，可以稍后在预览页返回补充</p>
            </div>
          </div>

          <!-- Step 2: 身份证号 -->
          <div v-show="stepIndex === 2" class="wizard-step">
            <h3 class="step-title">身份证号 · ID Number</h3>
            <el-form-item label="身份证号（选填）· ID Number" prop="idCard">
              <el-input v-model="form.idCard" placeholder="选填，18位身份证号" maxlength="18" size="large" />
            </el-form-item>
          </div>

          <!-- Step 3: 部门/职位/性别 -->
          <div v-show="stepIndex === 3" class="wizard-step">
            <h3 class="step-title">岗位信息 · Position</h3>
            <el-form-item label="部门 · Department" prop="department">
              <el-select v-model="form.department" placeholder="请选择部门" size="large" style="width:100%">
                <el-option label="前厅 · FOH" value="前厅" />
                <el-option label="厨房 · Kitchen" value="厨房" />
                <el-option label="财务 · Finance" value="财务" />
                <el-option label="人事 · HR" value="人事" />
                <el-option label="管理 · Management" value="管理" />
              </el-select>
            </el-form-item>
            <el-form-item label="职位 · Position" prop="position">
              <el-input v-model="form.position" placeholder="请输入职位" maxlength="30" size="large" />
            </el-form-item>
            <el-form-item label="性别 · Gender" prop="gender">
              <el-radio-group v-model="form.gender" size="large">
                <el-radio value="男">男 · Male</el-radio>
                <el-radio value="女">女 · Female</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <!-- Step 4: 家庭住址 -->
          <div v-show="stepIndex === 4" class="wizard-step">
            <h3 class="step-title">家庭住址 · Address</h3>
            <el-form-item label="家庭住址 · Address" prop="address">
              <el-input v-model="form.address" placeholder="请输入家庭住址" maxlength="100" size="large" />
            </el-form-item>
          </div>

          <!-- Step 5: 紧急联系人 -->
          <div v-show="stepIndex === 5" class="wizard-step">
            <h3 class="step-title">紧急联系人 · Emergency Contact</h3>
            <el-form-item label="紧急联系人 · Name" prop="emergencyContact">
              <el-input v-model="form.emergencyContact" placeholder="姓名" maxlength="20" size="large" />
            </el-form-item>
            <el-form-item label="紧急联系电话 · Phone" prop="emergencyPhone">
              <el-input v-model="form.emergencyPhone" placeholder="手机号" maxlength="11" size="large" />
            </el-form-item>
          </div>

          <!-- Step 6: 备注 -->
          <div v-show="stepIndex === 6" class="wizard-step">
            <h3 class="step-title">备注 · Remarks</h3>
            <el-form-item prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="4"
                placeholder="其他需要说明的信息（选填）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </div>

          <!-- Step 7: 预览 -->
          <div v-show="stepIndex === 7" class="wizard-step">
            <h3 class="step-title">预览 · Preview</h3>
            <div class="preview-list">
              <div class="preview-avatar-row">
                <img v-if="form.avatarUrl" :src="form.avatarUrl" class="preview-avatar" />
                <div v-else class="preview-avatar preview-avatar-empty">无头像</div>
              </div>
              <div class="preview-row"><span>提交类型</span><strong>{{ form.submitType === 'new' ? '新增入职' : '信息更新' }}</strong></div>
              <div class="preview-row"><span>姓名</span><strong>{{ form.name || '—' }}</strong></div>
              <div class="preview-row"><span>手机号</span><strong>{{ form.phone || '—' }}</strong></div>
              <div class="preview-row"><span>身份证号</span><strong>{{ form.idCard || '—' }}</strong></div>
              <div class="preview-row"><span>部门</span><strong>{{ form.department || '—' }}</strong></div>
              <div class="preview-row"><span>职位</span><strong>{{ form.position || '—' }}</strong></div>
              <div class="preview-row"><span>性别</span><strong>{{ form.gender || '—' }}</strong></div>
              <div class="preview-row"><span>家庭住址</span><strong>{{ form.address || '—' }}</strong></div>
              <div class="preview-row"><span>紧急联系人</span><strong>{{ form.emergencyContact || '—' }}</strong></div>
              <div class="preview-row"><span>紧急联系电话</span><strong>{{ form.emergencyPhone || '—' }}</strong></div>
              <div class="preview-row"><span>备注</span><strong>{{ form.remark || '—' }}</strong></div>
            </div>
          </div>
        </el-form>

        <div class="wizard-actions">
          <el-button v-if="stepIndex < totalSteps - 1" type="primary" class="wizard-next-btn" @click="nextStep">
            下一步 · Next
          </el-button>
          <el-button v-else type="primary" class="wizard-next-btn" :loading="loading" @click="handleSubmit">
            {{ loading ? '发送中...' : '发送 · Submit' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const formRef = ref(null)
const loading = ref(false)
const submitted = ref(false)
const avatarInputRef = ref(null)
const avatarUploading = ref(false)

// stage: landing -> jobList -> wizard
const stage = ref('landing')
const jobs = ref([])
const jobsLoading = ref(false)
const selectedJob = ref(null)

const stepIndex = ref(0)
const totalSteps = 8 // 姓名手机号/头像/身份证/岗位性别/住址/紧急联系人/备注/预览

const form = reactive({
  submitType: 'new',
  jobPostingId: null,
  name: '',
  phone: '',
  idCard: '',
  department: '',
  position: '',
  gender: '',
  address: '',
  emergencyContact: '',
  emergencyPhone: '',
  avatarUrl: '',
  remark: ''
})

// 每一步对应校验哪些字段，只在离开当前步时校验，不需要一次性交出整张表单
const stepFieldMap = [
  ['name', 'phone'],
  [],
  ['idCard'],
  ['department', 'position', 'gender'],
  ['address'],
  ['emergencyContact', 'emergencyPhone'],
  [],
  []
]

async function loadJobs() {
  jobsLoading.value = true
  try {
    const res = await request.get('/api/hr/job-postings/open')
    jobs.value = res.data || []
  } catch (e) {
    jobs.value = []
  } finally {
    jobsLoading.value = false
  }
}

function goJobList() {
  stage.value = 'jobList'
  if (jobs.value.length === 0) loadJobs()
}

function startNewFlow(job) {
  form.submitType = 'new'
  if (job) {
    selectedJob.value = job
    form.jobPostingId = job.id
    form.department = job.department
    form.position = job.position
  } else {
    selectedJob.value = null
    form.jobPostingId = null
  }
  stepIndex.value = 0
  stage.value = 'wizard'
}

function startUpdateFlow() {
  form.submitType = 'update'
  selectedJob.value = null
  form.jobPostingId = null
  stepIndex.value = 0
  stage.value = 'wizard'
}

function triggerAvatarPick() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (file.size > 10 * 1024 * 1024) { ElMessage.warning('图片不能超过10MB'); return }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/api/upload/image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    const d = res.data || res
    form.avatarUrl = d.url
    ElMessage.success('头像上传成功')
  } catch (e2) {
    console.error('头像上传失败', e2)
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

const validatePhone = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

const validateIdCard = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  const idCardReg = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
  if (!idCardReg.test(value)) {
    callback(new Error('请输入正确的18位身份证号'))
  } else {
    callback()
  }
}

const validateEmergencyPhone = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  idCard: [
    { validator: validateIdCard, trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  position: [
    { required: true, message: '请输入职位', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入家庭住址', trigger: 'blur' }
  ],
  emergencyContact: [
    { required: true, message: '请输入紧急联系人', trigger: 'blur' }
  ],
  emergencyPhone: [
    { validator: validateEmergencyPhone, trigger: 'blur' }
  ],
  remark: []
}

async function nextStep() {
  const fields = stepFieldMap[stepIndex.value]
  if (fields.length > 0 && formRef.value) {
    try {
      await formRef.value.validateField(fields)
    } catch {
      ElMessage.warning('请完善本页必填信息')
      return
    }
  }
  stepIndex.value++
}

function prevStep() {
  if (stepIndex.value === 0) {
    stage.value = selectedJob.value || form.submitType === 'new' ? 'jobList' : 'landing'
    return
  }
  stepIndex.value--
}

function resetAll() {
  submitted.value = false
  stage.value = 'landing'
  stepIndex.value = 0
  selectedJob.value = null
  form.submitType = 'new'
  form.jobPostingId = null
  form.name = ''
  form.phone = ''
  form.idCard = ''
  form.department = ''
  form.position = ''
  form.gender = ''
  form.address = ''
  form.emergencyContact = ''
  form.emergencyPhone = ''
  form.avatarUrl = ''
  form.remark = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善必填信息，可返回对应步骤修改')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/api/hr/self-service/submit', {
      submitType: form.submitType,
      jobPostingId: form.jobPostingId,
      name: form.name,
      phone: form.phone,
      idCard: form.idCard || null,
      department: form.department,
      position: form.position,
      gender: form.gender,
      address: form.address,
      emergencyContact: form.emergencyContact,
      emergencyPhone: form.emergencyPhone || null,
      avatarUrl: form.avatarUrl || null,
      remark: form.remark || null
    })

    // 全局 request 拦截器已经在非 200 时把 promise reject 掉了，走到这里
    // 就是真的成功了——res 是拦截器 return 出来的完整信封 { code, message, data }，
    // res.data 只是信封里的业务数据，从来没有 .code 字段，不需要在这里再判断一次。
    void res
    submitted.value = true
    ElMessage.success('提交成功，等待HR审核')
  } catch (e) {
    ElMessage.error(e.message || '提交失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.self-service-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4f3 0%, #e8edea 50%, #dce5e1 100%);
  display: flex;
  justify-content: center;
  padding: 24px 16px 48px;
}

.ss-container {
  width: 100%;
  max-width: 520px;
}

.ss-header {
  text-align: center;
  margin-bottom: 28px;
  padding-top: 16px;
}

.ss-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  color: #2D4A3E;
  background: #fff;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px rgba(45, 74, 62, 0.12);
}

.ss-logo svg {
  width: 32px;
  height: 32px;
}

.ss-title {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0 0 4px;
}

.ss-subtitle {
  font-size: 13px;
  color: #7a8c84;
  margin: 0;
}

.ss-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

/* 成功页 */
.ss-success {
  padding: 48px 32px;
  text-align: center;
}

.success-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #2D4A3E, #4A7C59);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.success-icon svg {
  width: 40px;
  height: 40px;
}

.ss-success h2 {
  font-size: 22px;
  color: #2D4A3E;
  margin: 0 0 8px;
}

.ss-success p {
  font-size: 15px;
  color: #5a6d66;
  margin: 0 0 4px;
}

.ss-tip {
  font-size: 13px !important;
  color: #999 !important;
  margin-bottom: 24px !important;
}

.ss-reset-btn {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4f;
  --el-button-hover-border-color: #3a5e4f;
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
}

/* 欢迎页 */
.landing-desc {
  font-size: 14px;
  color: #5a6d66;
  margin: 0 0 20px;
  text-align: center;
}

.landing-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.landing-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border-radius: 12px;
  border: 1px solid #e8edea;
  cursor: pointer;
  transition: all 0.15s;
}

.landing-btn:hover {
  border-color: #2D4A3E;
  background: #f7faf8;
}

.landing-btn-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #f0f4f3;
  color: #2D4A3E;
  display: flex;
  align-items: center;
  justify-content: center;
}

.landing-btn-icon svg {
  width: 22px;
  height: 22px;
}

.landing-btn-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.landing-btn-text strong {
  font-size: 14px;
  color: #2D4A3E;
}

.landing-btn-text span {
  font-size: 12px;
  color: #9aaba3;
}

.landing-btn-arrow {
  color: #ccc;
  font-size: 18px;
}

/* 阶段导航 */
.stage-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.back-link {
  background: none;
  border: none;
  color: #2D4A3E;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
}

.stage-nav-title {
  font-size: 13px;
  color: #9aaba3;
}

/* 岗位列表 */
.job-empty {
  text-align: center;
  padding: 40px 0;
  color: #9aaba3;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.job-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.job-card {
  border: 1px solid #e8edea;
  border-radius: 12px;
  padding: 16px;
}

.job-card-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.job-card-head strong {
  font-size: 16px;
  color: #2D4A3E;
}

.job-dept {
  font-size: 12px;
  color: #fff;
  background: #4A7C59;
  padding: 2px 8px;
  border-radius: 6px;
}

.job-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #7a8c84;
  margin-bottom: 8px;
}

.job-req, .job-desc {
  font-size: 12px;
  color: #5a6d66;
  margin: 0 0 4px;
  line-height: 1.5;
}

.job-join-btn {
  width: 100%;
  margin-top: 8px;
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4f;
  --el-button-hover-border-color: #3a5e4f;
}

/* 分步向导 */
.step-dots {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
}

.step-dot {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: #e8edea;
}

.step-dot.active {
  background: #2D4A3E;
}

.wizard-step {
  min-height: 180px;
}

.step-title {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
  margin: 0 0 20px;
}

.avatar-upload { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.avatar-preview {
  width: 120px; height: 120px; border-radius: 50%;
  background: #f0f4f3; border: 2px dashed #dce5e1;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; overflow: hidden; position: relative;
}
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #9aaba3; font-size: 12px; }
.avatar-placeholder span { color: #7a8c84; }
.avatar-uploading {
  position: absolute; inset: 0; background: rgba(255,255,255,0.85);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #2D4A3E;
}
.avatar-hint {
  font-size: 12px;
  color: #9aaba3;
  margin: 0;
}

/* 预览页 */
.preview-avatar-row {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.preview-avatar {
  width: 72px; height: 72px; border-radius: 50%; object-fit: cover;
  border: 1px solid #e8edea;
}

.preview-avatar-empty {
  display: flex; align-items: center; justify-content: center;
  background: #f0f4f3; color: #9aaba3; font-size: 11px;
}

.preview-list {
  display: flex;
  flex-direction: column;
}

.preview-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f4f3;
  font-size: 13px;
}

.preview-row span {
  color: #9aaba3;
}

.preview-row strong {
  color: #2D4A3E;
  font-weight: 500;
  text-align: right;
  max-width: 65%;
  word-break: break-all;
}

.wizard-actions {
  margin-top: 24px;
  padding-top: 16px;
}

.wizard-next-btn {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4f;
  --el-button-hover-border-color: #3a5e4f;
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 1px;
}

/* Element Plus 覆盖 */
.wizard-form :deep(.el-form-item__label) {
  color: #4a5c55;
  font-weight: 500;
  font-size: 13px;
}

.wizard-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce5e1 inset;
}

.wizard-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.wizard-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.wizard-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

.wizard-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce5e1 inset;
}

.wizard-form :deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #2D4A3E;
  border-color: #2D4A3E;
}

.wizard-form :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #2D4A3E;
}

/* 移动端适配 */
@media (max-width: 480px) {
  .self-service-page {
    padding: 12px 8px 32px;
  }

  .ss-title {
    font-size: 18px;
  }

  .ss-card {
    padding: 20px 14px;
  }
}
</style>
