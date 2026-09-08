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
          <img src="/logo.png" alt="又见炊烟私房菜" class="brand-logo" />
          <h1 class="brand-title">又见炊烟私房菜</h1>
          <div class="brand-divider"></div>
          <p class="brand-slogan">山间炊烟起，人间美味来</p>
        </div>

        <div class="form-section">
          <button class="back-btn" @click="goBack">
            <SiteIcon name="chevron-right" :size="14" class="back-arrow" />返回门店选择
          </button>
          <div class="form-card">
            <div class="form-header">
              <h2>{{ t('login.welcome') }}</h2>
              <p>{{ storeName || '宁国店' }}</p>
            </div>
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="login-form"
              autocomplete="off"
            >
              <!-- 隐藏诱饵：防止浏览器密码管理器自动填充 -->
              <div style="position:absolute;left:-9999px;top:-9999px;opacity:0;height:0;overflow:hidden">
                <input type="text" name="yj-user" tabindex="-1" autocomplete="username" />
                <input type="password" name="yj-pass" tabindex="-1" autocomplete="current-password" />
              </div>
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  :placeholder="t('login.username')"
                  size="large"
                  prefix-icon="User"
                  clearable
                  ref="accountInputRef"
                  autocomplete="off"
                  name="yj-account-input"
                  @focus="onAccountFocus"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  :placeholder="t('login.password')"
                  size="large"
                  prefix-icon="Lock"
                  show-password
                  ref="passwordInputRef"
                  autocomplete="new-password"
                  name="yj-pwd-input"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                {{ loading ? '登录中…' : t('login.login') }}
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
import SiteIcon from '@/components/site/SiteIcon.vue'
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

// 防止浏览器密码管理器自动填充账号
function onAccountFocus() {
  const v = loginForm.value.username
  if (v && !/^[a-zA-Z0-9_]{3,30}$/.test(v)) {
    loginForm.value.username = ''
  }
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
  background: var(--site-paper);
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

.c1 { display: none; }

.c2 { display: none; }

.c3 { display: none; }

/* 三个大号径向渐变圆加一层网格，是那种"科技感登录页"的老模板做法。
   圆本身看不清，只是把底色搅浑；网格在浅底上像屏幕脏了。
   留一层极淡的斜纹当纸纹，其余撤掉。 */
.pattern-overlay {
  position: absolute;
  inset: 0;
  /* 纹路铺满整屏时，卡片上那个密度会变成一层可见的斜纹，反而抢戏。
     整屏用更淡、更疏的一档，只在余光里留一点纸的质感。 */
  background-image: repeating-linear-gradient(45deg,
    rgba(30, 58, 47, 0.012) 0, rgba(30, 58, 47, 0.012) 1px, transparent 1px, transparent 16px);
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
  border-radius: var(--site-radius-lg);
  border: 1px solid var(--site-line);
  box-shadow: var(--site-lift-strong);
  overflow: hidden;
  min-height: 520px;
}

.brand-section {
  width: 42%;
  /* 原来是深绿到浅绿的三段渐变。渐变在大色块上很难不显廉价，
     尤其这种从暗到亮的斜向过渡，看着像塑料贴片。改成平色。 */
  background: var(--site-pine);
  padding: 56px 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: white;
}

.brand-logo {
  width: 76px;
  height: 76px;
  object-fit: contain;
  margin-bottom: 28px;
}

/*
  标题原来是 38px/字重 700/字间距 8px。8px 在 38px 的中文上把六个字拆成六个孤立的字，
  读起来要一个个拼；再加伪粗体，笔画糊成一片。
  收到 0.14em 并把字重降到 600，衬线体自己有骨架，不需要加粗撑。
*/
.brand-title {
  font-family: var(--site-serif);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-indent: 0.14em;
  line-height: 1.3;
  margin: 0 0 20px;
}

.brand-divider {
  width: 40px;
  height: 1px;
  background: var(--site-brass-soft);
  margin-bottom: 20px;
}

/* 中文没有真正的斜体，font-style: italic 只是把字形整体压斜，看着就是歪的。去掉。 */
.brand-slogan {
  font-size: var(--site-fs-small);
  color: rgba(255, 255, 255, 0.62);
  letter-spacing: 0.16em;
  text-indent: 0.16em;
  margin: 0;
}

.form-section {
  flex: 1;
  padding: 40px 44px;
  display: flex;
  flex-direction: column;
}

/* 原来是个写着"← 返回门店选择"的 div。用箭头字符当图标，
   不同字体下大小和基线都不一样；而且 div 不可聚焦，键盘用户按不到。
   换成 button + 描边图标。 */
.back-btn {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  font-size: var(--site-fs-small);
  color: var(--site-ink-3);
  cursor: pointer;
  margin-bottom: 32px;
  transition: color var(--site-dur) var(--site-ease);
}
.back-btn:hover { color: var(--site-pine); }
.back-arrow { transform: rotate(180deg); }

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
  font-family: var(--site-serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--site-pine);
  margin-bottom: 8px;
  letter-spacing: 0.06em;
}

.form-header p {
  font-size: var(--site-fs-small);
  color: var(--site-ink-3);
  letter-spacing: 0.04em;
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--site-line-strong) inset !important;
  border-radius: var(--site-radius);
  height: 46px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.15) inset !important;
}

.login-btn {
  width: 100%;
  height: 48px !important;
  font-size: var(--site-fs-lead) !important;
  letter-spacing: 0.1em;
  border-radius: var(--site-radius);
  background: var(--site-pine);
  border-color: var(--site-pine);
  margin-top: 14px;
}

.login-btn:hover {
  background: var(--site-pine-2) !important;
  border-color: var(--site-pine-2) !important;
}

.form-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 备案码是一长串十六进制，跟正文同色会显得像出了错。
   压到更浅一档，并允许换行，别让它撑破卡片。 */
.beian-code {
  font-size: var(--site-fs-micro);
  color: var(--site-ink-3);
  letter-spacing: 0.02em;
  opacity: 0.75;
  line-height: 1.6;
  word-break: break-all;
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
