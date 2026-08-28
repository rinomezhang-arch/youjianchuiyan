<template>
  <view class="setting-page">
    <!-- 用户信息卡片 -->
    <view class="profile-card" @tap="goProfile">
      <image class="avatar" :src="user?.avatar || defaultAvatar" mode="aspectFill"/>
      <view class="info">
        <view class="name">{{ user?.nickname || (isLogin ? '微信用户' : '未登录') }}</view>
        <view class="sub muted">{{ user?.phone ? '手机号 ' + maskPhone(user.phone) : '点击完善个人资料' }}</view>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 通用 -->
    <view class="group">
      <view class="item" @tap="togglePush">
        <text class="ico">🔔</text>
        <text class="label">消息通知</text>
        <switch :checked="pushOn" color="#C9A86C" @change="pushOn = $event.detail.value" @tap.stop/>
      </view>
      <view class="item" @tap="toggleDark">
        <text class="ico">🌙</text>
        <text class="label">深色模式（跟随系统）</text>
        <switch :checked="darkFollow" color="#C9A86C" @change="darkFollow = $event.detail.value" @tap.stop/>
      </view>
      <view class="item" @tap="openCache">
        <text class="ico">🗂️</text>
        <text class="label">清除本地缓存</text>
        <text class="value muted">{{ cacheSize }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @tap="changeStore">
        <text class="ico">🏘️</text>
        <text class="label">当前门店</text>
        <text class="value">{{ appStore.currentStore?.name || '请选择' }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 账号 -->
    <view class="group">
      <view class="item" @tap="openAgreement('privacy')">
        <text class="ico">🔏</text>
        <text class="label">隐私政策</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @tap="openAgreement('service')">
        <text class="ico">📋</text>
        <text class="label">用户服务协议</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @tap="openAbout">
        <text class="ico">ℹ️</text>
        <text class="label">关于又见炊烟</text>
        <text class="value muted">v{{ version }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @tap="openContact">
        <text class="ico">☎️</text>
        <text class="label">联系客服</text>
        <text class="value muted">400-0000-000</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 运营 / 门店员工入口 -->
    <view class="group" v-if="isLogin && user?.roles?.includes('STAFF')">
      <view class="item" @tap="toast('员工管理端（可接入ERP端 H5）')">
        <text class="ico">🛠️</text>
        <text class="label">商家工作台</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="gap"></view>
    <view class="logout-wrap">
      <button v-if="isLogin" class="logout-btn" @tap="doLogout">退出登录</button>
      <button v-else class="gold-btn big" @tap="doLogin">立即登录</button>
    </view>
    <view class="copyright muted">© 又见炊烟 · 徽菜名店  All Rights Reserved</view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/store/app'
import { toast } from '@/api/wx'

const appStore = useAppStore()
const version = '1.0.0'
const defaultAvatar = 'https://img.icons8.com/ios/200/C9A86C/user-male-circle.png'

const user = computed(() => appStore.user)
const isLogin = computed(() => !!appStore.token)

const pushOn = ref(true)
const darkFollow = ref(true)
const cacheSize = ref('0 KB')

onMounted(async () => {
  try {
    const r = await uni.getStorageInfo()
    const kb = Math.ceil(r.currentSize)
    cacheSize.value = kb > 1024 ? (kb/1024).toFixed(1) + ' MB' : kb + ' KB'
  } catch(e){}
})

function maskPhone(p){ return p && p.length >= 11 ? p.slice(0,3)+'****'+p.slice(-4) : p }
function goProfile(){
  if (!isLogin.value) return doLogin()
  uni.navigateTo({ url: '/pages/profile/profile' })
}
function togglePush(e){
  if (e && e.type === 'click') {
    // 整行点击：toggle（但 switch 自己已 stop，点击行触发反转）
    pushOn.value = !pushOn.value
  }
  // #ifdef MP-WEIXIN
  if (pushOn.value) uni.requestSubscribeMessage({ tmplIds: [] }).catch(() => {})
  // #endif
}
function toggleDark(){ darkFollow.value = !darkFollow.value }
async function openCache(){
  const r = await uni.showModal({ title:'清除本地缓存', content:'将清除本地图片、购物车、地址等缓存数据' })
  if (!r.confirm) return
  try { await uni.clearStorageSync(); cacheSize.value = '0 KB'; toast('缓存已清理') }
  catch(e){ toast('清除失败') }
}
function changeStore(){ uni.navigateTo({ url: '/pages/stores/stores' }) }
function openAgreement(type){ uni.navigateTo({ url: `/pages/agreement/agreement?type=${type}` }) }
function openAbout(){ uni.showModal({
  title: '关于又见炊烟',
  content: `又见炊烟小程序 v${version}\n\n以徽菜、农家土菜、喜宴包厢为主营的连锁餐饮品牌。\n后端：SpringBoot + MySQL + ERP 集成\n前端：Uni-App(Vue3)，一套代码发布小程序 + H5 + APP\n\n客服热线：400-0000-000`,
  showCancel:false
})}
function openContact(){ uni.makePhoneCall({ phoneNumber: '4000000000' }).catch(()=>{}) }
function doLogin(){ uni.navigateTo({ url: '/pages/login/login' }) }
async function doLogout(){
  const r = await uni.showModal({ title:'退出登录', content:'确定退出当前账号？' })
  if (!r.confirm) return
  appStore.logout()
  toast('已退出登录')
  setTimeout(() => uni.switchTab({ url: '/pages/me/me' }), 500)
}
</script>

<style lang="scss">
@import '@/uni.scss';
.setting-page{background:$page-bg; min-height:100vh; padding:20rpx 0 60rpx}
.profile-card{@include row-start; @include card; margin:0 20rpx 20rpx;
  .avatar{width:120rpx; height:120rpx; border-radius:50%; background:#eee; margin-right:24rpx}
  .info{flex:1; .name{font-size:32rpx; font-weight:600; color:$ink-black} .sub{margin-top:8rpx}}
  .arrow{color:#ccc; font-size:40rpx; padding:0 10rpx}
}
.group{background:#fff; margin:0 20rpx 20rpx; border-radius:20rpx; overflow:hidden; box-shadow:$shadow-card}
.item{@include row-center; padding:30rpx 28rpx; border-top:2rpx solid #f5f0e2; &:first-child{border-top:0}
  .ico{width:52rpx; font-size:32rpx; text-align:center; margin-right:20rpx}
  .label{flex:1; font-size:28rpx; color:$ink-black}
  .value{font-size:26rpx; color:$ink-black; margin-right:8rpx; max-width:360rpx; overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
  .arrow{color:#ccc; font-size:36rpx; margin-left:6rpx}
}
.gap{height:40rpx}
.logout-wrap{padding:0 20rpx}
.logout-btn{width:100%; height:88rpx; line-height:86rpx; background:#fff; color:#ff5a4d; border:2rpx solid #ffd2cf; border-radius:44rpx; font-size:30rpx; font-weight:600}
.gold-btn.big{width:100%; height:92rpx; line-height:92rpx; border-radius:46rpx; @include gold-btn;}
.copyright{text-align:center; padding:40rpx 0 20rpx; font-size:22rpx}
</style>
