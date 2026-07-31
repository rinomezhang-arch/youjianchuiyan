<template>
  <div class="self-service-page">
    <div class="ss-container">
      <!-- 顶部 -->
      <div class="ss-header">
        <h1 class="ss-title">又见炊烟 · 员工自助入职登记</h1>
        <p class="ss-subtitle">Self-Service Onboarding</p>
      </div>

      <!-- 提交成功 -->
      <div v-if="submitted" class="ss-success">
        <h2>提交成功</h2>
        <p>您的入职信息已提交，请等待 HR 审核</p>
        <p class="ss-tip">审核通过后将通知您入职安排</p>
        <el-button type="primary" @click="resetForm" class="ss-reset-btn">继续登记 · Continue</el-button>
      </div>

      <!-- 表单 -->
      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="ss-form"
        @submit.prevent="handleSubmit"
      >
        <!-- 提交类型 -->
        <div class="form-section">
          <h3 class="section-title">提交类型</h3>
          <el-radio-group v-model="form.submitType" class="type-radio-group">
            <el-radio-button value="new">新增入职 · New</el-radio-button>
            <el-radio-button value="update">信息更新 · Update</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 基本信息 -->
        <div class="form-section">
          <h3 class="section-title">基本信息 · Basic Info</h3>

          <el-form-item label="姓名 · Name" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" maxlength="20" show-word-limit />
          </el-form-item>

          <el-form-item label="手机号 · Phone" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
          </el-form-item>

          <el-form-item label="身份证号 · ID Number" prop="idCard">
            <el-input v-model="form.idCard" placeholder="选填，18位身份证号" maxlength="18" />
          </el-form-item>

          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="部门 · Department" prop="department">
                <el-select v-model="form.department" placeholder="请选择部门" style="width:100%">
                  <el-option label="前厅 · FOH" value="前厅" />
                  <el-option label="厨房 · Kitchen" value="厨房" />
                  <el-option label="财务 · Finance" value="财务" />
                  <el-option label="人事 · HR" value="人事" />
                  <el-option label="管理 · Management" value="管理" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职位 · Position" prop="position">
                <el-input v-model="form.position" placeholder="请输入职位" maxlength="30" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="性别 · Gender" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio value="男">男 · Male</el-radio>
              <el-radio value="女">女 · Female</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <!-- 联系信息 -->
        <div class="form-section">
          <h3 class="section-title">联系信息 · Contact</h3>

          <el-form-item label="家庭住址 · Address" prop="address">
            <el-input v-model="form.address" placeholder="请输入家庭住址" maxlength="100" />
          </el-form-item>

          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="紧急联系人 · Emergency Contact" prop="emergencyContact">
                <el-input v-model="form.emergencyContact" placeholder="姓名" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="紧急联系电话 · Emergency Phone" prop="emergencyPhone">
                <el-input v-model="form.emergencyPhone" placeholder="手机号" maxlength="11" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 备注 -->
        <div class="form-section">
          <h3 class="section-title">备注 · Remarks</h3>
          <el-form-item prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="其他需要说明的信息（选填）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="ss-submit-btn"
          >
            {{ loading ? '提交中...' : '提交登记 · Submit' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)
const submitted = ref(false)

const form = reactive({
  submitType: 'new',
  name: '',
  phone: '',
  idCard: '',
  department: '',
  position: '',
  gender: '',
  address: '',
  emergencyContact: '',
  emergencyPhone: '',
  remark: ''
})

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

const resetForm = () => {
  submitted.value = false
  form.submitType = 'new'
  form.name = ''
  form.phone = ''
  form.idCard = ''
  form.department = ''
  form.position = ''
  form.gender = ''
  form.address = ''
  form.emergencyContact = ''
  form.emergencyPhone = ''
  form.remark = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善必填信息')
    return
  }

  loading.value = true
  try {
    const res = await fetch('/api/hr/self-service/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        submitType: form.submitType,
        name: form.name,
        phone: form.phone,
        idCard: form.idCard || null,
        department: form.department,
        position: form.position,
        gender: form.gender,
        address: form.address,
        emergencyContact: form.emergencyContact,
        emergencyPhone: form.emergencyPhone || null,
        remark: form.remark || null
      })
    })

    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || '提交失败')
    }

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

/* 成功页 */
.ss-success {
  background: #fff;
  border-radius: 16px;
  padding: 48px 32px;
  text-align: center;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
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

/* 表单 */
.ss-form {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.form-section {
  margin-bottom: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2D4A3E;
  margin: 0 0 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e8edea;
}

.type-radio-group {
  width: 100%;
}

.type-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.type-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 8px !important;
}

.type-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #2D4A3E;
  border-color: #2D4A3E;
  box-shadow: none;
}

/* Element Plus 覆盖 */
.ss-form :deep(.el-form-item__label) {
  color: #4a5c55;
  font-weight: 500;
  font-size: 13px;
}

.ss-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce5e1 inset;
}

.ss-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.ss-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.ss-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

.ss-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce5e1 inset;
}

.ss-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.ss-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #2D4A3E inset;
}

.ss-form :deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #2D4A3E;
  border-color: #2D4A3E;
}

.ss-form :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #2D4A3E;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
}

.ss-submit-btn {
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

/* 移动端适配 */
@media (max-width: 480px) {
  .self-service-page {
    padding: 12px 8px 32px;
  }

  .ss-title {
    font-size: 18px;
  }

  .ss-form {
    padding: 20px 14px;
  }
}
</style>
