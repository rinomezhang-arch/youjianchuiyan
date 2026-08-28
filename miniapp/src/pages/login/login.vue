<template>
  <view class="login-page">
    <view class="brand">
      <view class="logo">炊烟</view>
      <text class="title">又见炊烟</text>
      <text class="subtitle">徽菜传承 · 家的味道</text>
    </view>

    <view class="form-card">
      <!-- 微信小程序：一键授权登录 + 授权手机号 -->
      <!-- #ifdef MP-WEIXIN -->
      <view class="section">
        <view class="sec-label">快捷登录</view>
        <button class="wx-btn" type="primary" @tap="onWxLogin" :disabled="loading">
          <text class="wx-logo">💬</text>
          <text>{{ loading ? '登录中…' : '微信一键登录' }}</text>
        </button>
        <text class="wx-tip">授权后可用微信身份登录又见炊烟</text>

        <button
          class="wx-phone-btn"
          open-type="getPhoneNumber"
          @getphonenumber="onGetPhone"
          type="primary"
          plain
          :disabled="!appStore.token"
        >
          📱 授权手机号（已授权手机号后，预订无需再次填写）
        </button>
      </view>
      <view class="split"><text>或使用手机号登录</text></view>
      <!-- #endif -->

      <view class="field">
        <text class="f-label">手机号</text>
        <input
          class="f-input"
          v-model="form.phone"
          type="number" maxlength="11"
          placeholder="请输入11位手机号"
        />
      </view>

      <view class="field">
        <text class="f-label">验证码</text>
        <view class="f-input-wrap">
          <input class="f-input" v-model="form.code" type="number" maxlength="6" placeholder="6位验证码" />
          <text
            :class="['code-btn', sending || cd>0 ? 'disabled' : '']"
            @tap="onSendSms"
          >{{ cd > 0 ? `${cd}s 后重发` : (sending ? '发送中' : '获取验证码') }}</text>
        </view>
      </view>

      <view class="field">
        <text class="f-label">姓名（选填）</text>
        <input class="f-input" v-model="form.name" placeholder="预订和消费记录更方便" maxlength="20" />
      </view>

      <view class="submit-wrap">
        <view :class="['submit-btn', submitting && 'disabled']" @tap="onSubmit">
          {{ submitting ? '登录中…' : '登录 / 注册' }}
        </view>
        <text class="agree">
          登录即代表您同意
          <text class="link" @tap.stop="showPact('user')">《用户协议》</text>
          与
          <text class="link" @tap.stop="showPact('privacy')">《隐私政策》</text>
        </text>
      </view>
    </view>

    <!-- #ifdef H5 -->
    <view class="tip-h5">
      <text>当前非微信小程序环境，部分原生能力（一键登录、获取手机号、微信支付、订阅消息）将不可用。</text>
      <text>如需完整体验，请使用【又见炊烟】微信小程序。</text>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useAppStore } from '@/store/app'
import { smsLogin, sendSms } from '@/api/user'
import { wechatMiniLogin, wechatGetPhoneFromEvent } from '@/api/wx'
import { isMobile } from '@/utils/util'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const sending = ref(false)
const cd = ref(0)
let cdTimer = null

const form = reactive({ phone: '', code: '', name: '' })

async function onWxLogin() {
  if (loading.value) return
  loading.value = true
  try {
    const r = await wechatMiniLogin()
    if (r && r.token) {
      appStore.setToken(r.token)
      if (r.openId) appStore.setOpenId(r.openId)
      if (r.userInfo) appStore.setUserInfo(r.userInfo)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.navigateBack().catch(() => uni.switchTab({ url: '/pages/index/index' })), 800)
    } else {
      uni.showToast({ title: '微信静默登录返回空，建议使用手机号登录', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: e?.message || '登录失败', icon: 'none' })
  } finally { loading.value = false }
}

async function onGetPhone(e) {
  try {
    const phone = await wechatGetPhoneFromEvent(e)
    if (phone) {
      form.phone = phone
      uni.showToast({ title: '已获取手机号', icon: 'success' })
    }
  } catch (err) {
    uni.showToast({ title: err.message || '授权失败', icon: 'none' })
  }
}

async function onSendSms() {
  if (sending.value || cd.value > 0) return
  if (!isMobile(form.phone)) return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
  sending.value = true
  try {
    await sendSms(form.phone, 'login')
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    cd.value = 60
    cdTimer = setInterval(() => {
      cd.value -= 1
      if (cd.value <= 0) { clearInterval(cdTimer); cdTimer = null }
    }, 1000)
  } catch (e) {
    // 统一 toast
  } finally { sending.value = false }
}

async function onSubmit() {
  if (submitting.value) return
  if (!isMobile(form.phone)) return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
  if (!/^\d{4,6}$/.test(form.code)) return uni.showToast({ title: '请输入验证码', icon: 'none' })
  submitting.value = true
  try {
    const r = await smsLogin(form.phone, form.code)
    if (r && r.token) {
      appStore.setToken(r.token)
      appStore.setUserInfo({
        phone: form.phone,
        nickname: form.name || (form.phone ? `${String(form.phone).slice(-4)}用户` : '会员'),
        ...(r.userInfo || {})
      })
      if (r.openId) appStore.setOpenId(r.openId)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.navigateBack().catch(() => uni.switchTab({ url: '/pages/index/index' })), 800)
    } else {
      uni.showToast({ title: '登录失败，未返回 token', icon: 'none' })
    }
  } catch {}
  finally { submitting.value = false }
}

function showPact(t) {
  const map = {
    user:    '用户协议\n\n1. 您需保证预订信息真实有效\n2. 如需取消请提前2小时联系门店\n3. 定金支付后，未按时到店可能产生费用扣除',
    privacy: '隐私政策\n\n1. 我们仅将您的手机号、姓名用于预订确认\n2. 消费记录用于会员权益计算\n3. 未经同意不会将数据分享给第三方'
  }
  uni.showModal({ title: t === 'user' ? '用户协议' : '隐私政策', content: map[t] || '', showCancel: false })
}

onUnmounted(() => { clearInterval(cdTimer) })
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.login-page {
  min-height: 100vh; padding: 0 0 80rpx; box-sizing: border-box;
  background:
    radial-gradient(120% 60% at 50% 0%, #FFF6DB 0%, transparent 70%),
    $brand-bg;
}
.brand { text-align:center; padding: 120rpx 0 80rpx; }
.logo {
  width: 160rpx; height: 160rpx; margin: 0 auto 20rpx; border-radius: 40rpx;
  background: linear-gradient(135deg, #1A1A1A 0%, #564428 60%, #C9A86C 100%);
  color: #FFDCA1; display:flex; align-items: center; justify-content: center;
  font-size: 52rpx; font-weight: 900; letter-spacing: 8rpx; box-shadow: $shadow-gold;
}
.title { display:block; font-size: 48rpx; font-weight: 800; color: $text-1; letter-spacing: 4rpx; }
.subtitle { display:block; margin-top: 8rpx; color: $brand-gold-dark; font-size: $font-sm; letter-spacing: 2rpx; }

.form-card {
  margin: 0 $spacing-md; background: #fff; border-radius: $radius-lg;
  padding: $spacing-lg; box-shadow: $shadow-card;
}

.section { margin-bottom: $spacing-md; }
.sec-label { font-size: $font-sm; color: $text-3; margin-bottom: 16rpx; }
.wx-btn {
  background: linear-gradient(135deg,#07C160,#06AD56) !important; border: none !important; border-radius: $radius-pill !important;
  color:#fff !important; font-size: $font-md !important; font-weight: 600 !important;
  display:flex; align-items:center; justify-content: center; gap: 12rpx; height: 92rpx;
}
.wx-logo { font-size: $font-lg; }
.wx-tip { display:block; text-align:center; color: $text-3; font-size: 22rpx; margin-top: 10rpx; }

.wx-phone-btn {
  margin-top: 20rpx !important; border-color: $brand-gold !important; color: $brand-gold-dark !important;
  border-radius: $radius-pill !important; font-size: $font-xs !important;
}

.split {
  text-align: center; margin: 24rpx 0; position: relative; color: $text-3; font-size: $font-xs;
}
.split::before, .split::after {
  content: ''; position: absolute; top: 50%; width: 30%; height: 1rpx; background: #EEE5CF;
}
.split::before { left: 5%; }
.split::after  { right: 5%; }

.field { display: flex; flex-direction: column; gap: 10rpx; margin-bottom: $spacing-md; }
.f-label { color: $text-2; font-size: $font-sm; }
.f-input {
  height: 84rpx; padding: 0 24rpx; background: #FAF7F2; border-radius: $radius-md;
  font-size: $font-md; color: $text-1;
}
.f-input-wrap { display:flex; align-items: center; gap: $spacing-sm; background: #FAF7F2; border-radius: $radius-md; padding-right: 12rpx; }
.f-input-wrap .f-input { background: transparent; flex: 1; }
.code-btn {
  flex-shrink: 0; padding: 12rpx 22rpx; background: linear-gradient(135deg, #FFF3D6, #FFE3A8);
  color: $brand-gold-dark; border-radius: $radius-pill; font-size: $font-xs; font-weight: 600;
}
.code-btn.disabled { opacity: 0.6; }

.submit-wrap { margin-top: $spacing-lg; }
.submit-btn {
  height: 96rpx; line-height: 96rpx; text-align:center; color:#fff; font-weight: 700; letter-spacing: 2rpx;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); border-radius: $radius-pill;
  box-shadow: $shadow-gold; font-size: $font-md;
}
.submit-btn.disabled { opacity: 0.6; }
.agree { display:block; text-align:center; font-size: 22rpx; color: $text-3; margin-top: 16rpx; }
.link { color: $brand-gold-dark; }

.tip-h5 {
  margin: $spacing-md; padding: $spacing-md;
  background: #FFEAEA; border: 1rpx solid #FFCECE;
  color: #C94B33; border-radius: $radius-md; font-size: $font-xs; line-height: 1.6;
  display:flex; flex-direction: column; gap: 6rpx;
}
</style>
