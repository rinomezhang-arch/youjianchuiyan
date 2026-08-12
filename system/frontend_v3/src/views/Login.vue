<template>
  <div class="login-page">
    <div class="bg-decoration">
      <div class="circle c1"></div>
      <div class="circle c2"></div>
      <div class="circle c3"></div>
      <div class="pattern-overlay"></div>
    </div>

    <div class="login-container">
      <div class="login-content">
        <div class="brand-section">
          <div class="brand-logo">
            <svg viewBox="0 0 64 64" fill="none">
              <rect x="10" y="10" width="44" height="44" rx="6" stroke="#FAF8F5" stroke-width="3"/>
              <path d="M18 30 L22 38 L26 30 L30 38 L34 30" stroke="#FAF8F5" stroke-width="3" stroke-linecap="round"/>
              <path d="M22 26 L22 42" stroke="#FAF8F5" stroke-width="2"/>
              <path d="M30 26 L30 42" stroke="#FAF8F5" stroke-width="2"/>
              <path d="M40 22 C40 22 42 26 42 30" stroke="#C4A35A" stroke-width="2" stroke-linecap="round"/>
              <path d="M42 26 C42 26 44 30 44 34" stroke="#C4A35A" stroke-width="2" stroke-linecap="round"/>
              <path d="M44 30 C44 30 46 34 46 38" stroke="#C4A35A" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <h1 class="brand-title">{{ t('login.title') }}</h1>
          <p class="brand-subtitle">{{ t('login.titleEn') }}</p>
          <p class="brand-desc">徽派私房菜 · 田园风情</p>
          <div class="brand-divider"></div>
          <p class="brand-slogan">山间炊烟起 · 人间美味来</p>
        </div>

        <div class="form-section">
          <div class="back-btn" @click="goBack">← 返回门店选择</div>
          <div class="form-card">
            <div class="form-header">
              <h2>{{ t('login.welcome') }} · {{ t('login.welcomeEn') }}</h2>
              <p>{{ storeName || '宁国店' }}</p>
            </div>
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="login-form"
              autocomplete="off"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  :placeholder="`${t('login.username')} · ${t('login.usernameEn')}`"
                  size="large"
                  prefix-icon="User"
                  clearable
                  ref="accountInputRef"
                  autocomplete="off"
                  name="username"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  :placeholder="`${t('login.password')} · ${t('login.passwordEn')}`"
                  size="large"
                  prefix-icon="Lock"
                  show-password
                  ref="passwordInputRef"
                  autocomplete="off"
                  name="password"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : `${t('login.login')} · ${t('login.loginEn')}` }}
              </el-button>
            </el-form>
            <div class="form-footer">
              <div class="beian-code">公安联网备案数据码：b1443b2b16d2c4030e6a50cfb21dd492</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const loginFormRef = ref(null)
const loading = ref(false)
const accountInputRef = ref(null)
const passwordInputRef = ref(null)
const storeName = computed(() => userStore.storeName)

const loginForm = ref({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: `${t('login.username')} · ${t('login.usernameEn')}`, trigger: 'blur' }],
  password: [{ required: true, message: `${t('login.password')} · ${t('login.passwordEn')}`, trigger: 'blur' }]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用 POST /api/auth/login，body: {username, password}
        const res = await userStore.login(loginForm.value.username, loginForm.value.password)
        if (res.code === 200 && res.data) {
          // 显式确保 token、userInfo、roles、storeId 已写入 userStore 及 localStorage
          // userStore.login() 内部已通过 persistAuth() 持久化 roles / storeId / currentStoreId
          // 此处做兜底校验：若缺失则补存
          if (res.data.token && !localStorage.getItem('token')) {
            localStorage.setItem('token', res.data.token)
          }
          if (res.data.storeId !== undefined && res.data.storeId !== null) {
            localStorage.setItem('storeId', String(res.data.storeId))
          }
          if (res.data.storeName) {
            localStorage.setItem('storeName', res.data.storeName)
          }
          // roles 由 userStore.login 根据 role+storeId 推导并持久化
          // 兜底：若 roles 为空则按 storeId 推导存入
          if (!userStore.roles || userStore.roles.length === 0) {
            const role = res.data.user?.role || res.data.role || ''
            const sid = Number(res.data.storeId)
            let fallbackRoles = ['staff']
            if (sid === 0 || role === 'admin') fallbackRoles = ['super_admin']
            else if (role === 'manager') fallbackRoles = ['store_manager']
            userStore.roles = fallbackRoles
            localStorage.setItem('roles', JSON.stringify(fallbackRoles))
          }
          // 同步 currentStoreId
          localStorage.setItem('currentStoreId', String(userStore.currentStoreId || res.data.storeId || 1))
          ElMessage.success('登录成功')
          router.push('/dashboard')
        } else {
          ElMessage.error(res.message || '登录失败，请检查账号密码')
        }
      } catch (e) {
        ElMessage.error('账号或密码错误，请重试')
      } finally {
        loading.value = false
      }
    }
  })
}

function moveToPassword() {
  passwordInputRef.value?.focus()
}

function goBack() {
  router.push('/')
}

onMounted(() => {
  accountInputRef.value?.focus()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #FAF8F5 0%, #F0EBE5 50%, #E8E4DE 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
}

.c1 {
  top: -200px;
  right: -100px;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(45, 74, 62, 0.06) 0%, transparent 70%);
}

.c2 {
  bottom: -300px;
  left: -200px;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(196, 163, 90, 0.04) 0%, transparent 70%);
}

.c3 {
  top: 20%;
  left: 30%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(74, 124, 89, 0.03) 0%, transparent 70%);
}

.pattern-overlay {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(45, 74, 62, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45, 74, 62, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 900px;
  padding: 24px;
}

.login-content {
  display: flex;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(45, 74, 62, 0.08);
  overflow: hidden;
  min-height: 520px;
}

.brand-section {
  width: 42%;
  background: linear-gradient(135deg, #2D4A3E 0%, #3D5A4E 50%, #4A7C59 100%);
  padding: 56px 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: white;
}

.brand-logo {
  margin-bottom: 28px;
}

.brand-logo svg {
  width: 72px;
  height: 72px;
}

.brand-title {
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 8px;
  margin-bottom: 8px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}

.brand-subtitle {
  font-size: 12px;
  letter-spacing: 3px;
  opacity: 0.7;
  margin-bottom: 16px;
  font-weight: 300;
}

.brand-desc {
  font-size: 14px;
  opacity: 0.5;
  letter-spacing: 3px;
  margin-bottom: 24px;
}

.brand-divider {
  width: 40px;
  height: 2px;
  background: rgba(196, 163, 90, 0.6);
  margin-bottom: 24px;
}

.brand-slogan {
  font-size: 13px;
  opacity: 0.6;
  letter-spacing: 2px;
  font-style: italic;
}

.form-section {
  flex: 1;
  padding: 40px 44px;
  display: flex;
  flex-direction: column;
}

.back-btn {
  font-size: 13px;
  color: var(--color-text-muted);
  cursor: pointer;
  margin-bottom: 28px;
  transition: color 0.25s;
}

.back-btn:hover {
  color: var(--color-primary);
}

.form-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 380px;
  margin: 0 auto;
  width: 100%;
}

.form-header {
  margin-bottom: 36px;
}

.form-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.form-header p {
  font-size: 14px;
  color: var(--color-text-muted);
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--color-border) inset !important;
  border-radius: var(--radius-md);
  height: 44px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.15) inset !important;
}

.login-btn {
  width: 100%;
  height: 46px !important;
  font-size: 16px !important;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  border-color: var(--color-primary);
  margin-top: 12px;
}

.login-btn:hover {
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary)) !important;
  border-color: var(--color-primary-dark) !important;
}

.form-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.beian-code {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
  opacity: 0.7;
}

@media (max-width: 640px) {
  .login-content {
    flex-direction: column;
    min-height: auto;
  }
  .brand-section {
    width: 100%;
    padding: 36px 24px;
  }
  .brand-logo svg {
    width: 56px;
    height: 56px;
  }
  .brand-title {
    font-size: 28px;
    letter-spacing: 4px;
  }
  .form-section {
    padding: 28px 24px;
  }
}
</style>
