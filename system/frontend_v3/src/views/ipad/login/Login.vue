<template>
  <div class="ipad-login">
    <div class="bg-layer">
      <div class="bg-blob b1"></div>
      <div class="bg-blob b2"></div>
    </div>
    <div class="login-card">
      <div class="brand-side">
        <div class="brand-logo">
          <img :src="logoImage" alt="又见炊烟私房菜 Logo" />
        </div>
        <h2 class="brand-title">又见炊烟私房菜</h2>
        <p class="brand-sub">Youjian Private Kitchen</p>
        <p class="brand-desc">{{ ipad.storeName || '宁国店' }}</p>
        <div class="brand-divider"></div>
        <p class="brand-slogan">iPad 点餐系统 · Ordering System</p>
      </div>
      <div class="form-side">
        <div class="back-btn" @click="router.push('/ipad/store')">← 返回门店 · Back to Stores</div>
        <div class="welcome-copy">
          <p>欢迎回来 · Welcome Back</p>
          <h3 class="form-title">员工登录 · Staff Login</h3>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="account">
            <el-input v-model="form.account" placeholder="用户名或手机号 · Username or Phone" size="large" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码 · Password" size="large" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登录 · Login' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadLogin } from '@/api/ipad'
import { ElMessage } from 'element-plus'
import { fallbackOrThrow, errorMessage } from '@/utils/fallback'
import logoImage from '@/assets/images/logo.png'

const router = useRouter()
const ipad = useIpadStore()
const formRef = ref(null)
const loading = ref(false)

const form = ref({ account: '', password: '' })
const rules = {
  account: [{ required: true, message: '请输入用户名或手机号 · Enter username or phone', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码 · Enter password', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await ipadLogin(form.value.account, form.value.password)
      if (res.code === 200) {
        ipad.setLogin(res.data)
        ElMessage.success('登录成功')
        router.push('/ipad/home')
      } else {
        ElMessage.error(res.msg || '登录失败')
      }
    } catch (error) {
      try {
        const demoSession = fallbackOrThrow(error, () => ({
          staff_id: 1, staff_name: '服务员', staff_account: form.value.account,
          role_type: 'waiter', store_id: ipad.storeId, store_name: ipad.storeName,
          device_sn: ipad.deviceSn, print_port: 9100, print_template_code: 'default'
        }))
        ipad.setLogin(demoSession)
        router.push('/ipad/home')
      } catch (productionError) {
        ElMessage.error(errorMessage(productionError, '登录服务不可用'))
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.ipad-login {
  width: 100%; height: 100%;
  font-family: "Noto Sans SC", "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  background: linear-gradient(135deg, #FAF8F5 0%, #F0EBE5 50%, #E8E4DE 100%);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.bg-layer { position: absolute; inset: 0; pointer-events: none; }
.bg-blob { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.3; }
.b1 { width: 600px; height: 600px; background: rgba(45, 74, 62, 0.08); top: -200px; right: -100px; }
.b2 { width: 500px; height: 500px; background: rgba(196, 163, 90, 0.06); bottom: -200px; left: -100px; }

.login-card {
  position: relative; z-index: 1;
  display: flex; width: 720px; max-width: 90vw;
  background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(20px);
  border-radius: 24px; box-shadow: 0 20px 60px rgba(45, 74, 62, 0.08);
  overflow: hidden; min-height: 440px;
}

.brand-side {
  width: 40%; padding: 48px 28px;
  background: linear-gradient(135deg, #2D4A3E 0%, #3D5A4E 50%, #4A7C59 100%);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  text-align: center; color: white;
}
.brand-logo {
  width: 82px; height: 82px; margin-bottom: 22px;
  display: flex; align-items: center; justify-content: center;
}
.brand-logo img { width: 82px; height: 82px; object-fit: contain; }
.brand-title { font-size: 28px; font-weight: 700; letter-spacing: 4px; margin-bottom: 6px; font-family: "Noto Sans SC", "Microsoft YaHei", sans-serif; }
.brand-sub { font-size: 11px; letter-spacing: 2px; opacity: 0.78; margin-bottom: 16px; }
.brand-desc { font-size: 14px; opacity: 0.58; letter-spacing: 2px; margin-bottom: 20px; }
.brand-divider { width: 32px; height: 2px; background: rgba(196, 163, 90, 0.75); margin-bottom: 20px; }
.brand-slogan { font-size: 12px; opacity: 0.68; letter-spacing: 1px; }

.form-side { flex: 1; padding: 36px 40px; display: flex; flex-direction: column; justify-content: center; }
.back-btn { font-size: 13px; color: var(--color-text-muted); cursor: pointer; margin-bottom: 24px; transition: color 0.2s; }
.back-btn:hover { color: var(--color-primary); }
.welcome-copy p { margin: 0 0 8px; color: var(--color-text-muted); font-size: 13px; letter-spacing: 1px; }
.form-title { font-size: 22px; font-weight: 600; color: var(--color-text); margin-bottom: 28px; letter-spacing: 1px; }

.login-form :deep(.el-input__wrapper) { border-radius: var(--radius-md); height: 48px; box-shadow: 0 0 0 1px var(--color-border) inset !important; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.15) inset !important; }
.login-btn { width: 100%; height: 48px !important; font-size: 16px !important; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); border-color: var(--color-primary); margin-top: 8px; }
.login-btn:hover { background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary)) !important; }
</style>
