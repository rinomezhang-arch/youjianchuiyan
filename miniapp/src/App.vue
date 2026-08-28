<script setup>
/**
 * 又见炊烟 - Uni-App 入口
 * 启动时完成：
 *  1. 拉取门店列表并设置默认门店
 *  2. #ifdef MP-WEIXIN：调用 uni.login 拿 code → 后端 /auth/wx-login → 拿到用户信息/token/openid
 *  3. #ifdef H5：若当前微信浏览器则走微信公众号 OAuth（代码里预留，不强制）
 */
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { useCartStore } from '@/store/cart'
import { fetchStores } from '@/api/store'
import { wechatMiniLogin, ensureWechatUser } from '@/api/wx'
import { APP_ENV } from '@/config/env'

const appStore  = useAppStore()
const cartStore = useCartStore()

onLaunch(async () => {
  console.log('[App] onLaunch 又见炊烟小程序启动 env=', APP_ENV)

  // 1) 拉门店（所有 GET 都要带 storeId，默认 1 宁国店）
  try {
    const stores = await fetchStores()
    if (stores && stores.length) {
      appStore.setStores(stores)
      // 如果用户上次选过门店，优先用；否则默认第一个
      const last = appStore.currentStoreId
      const hit = stores.find(s => s.id === last) || stores[0]
      appStore.setCurrentStoreId(hit.id)
    }
  } catch (e) {
    console.warn('[App] 门店列表拉取失败，采用默认 storeId=1', e)
    appStore.setCurrentStoreId(1)
  }

  // 2) 平台特定登录
  // #ifdef MP-WEIXIN
  try {
    const loginData = await wechatMiniLogin() // {code} -> 后端 /auth/wx-login
    if (loginData && loginData.token) {
      appStore.setToken(loginData.token)
      if (loginData.userInfo) appStore.setUserInfo(loginData.userInfo)
      if (loginData.openId)   appStore.setOpenId(loginData.openId)
    }
  } catch (e) {
    console.warn('[App] 微信静默登录失败，将在需要时手动登录：', e?.message || e)
  }
  // #endif
})

onShow(() => {
  // 每次切前台，检查用户信息是否过期（简单策略：无token且小程序端自动再试一次）
  // #ifdef MP-WEIXIN
  if (!appStore.token) {
    ensureWechatUser().then(ok => ok && console.log('[App] onShow 补全登录状态'))
  }
  // #endif
})

onHide(() => {
  // 退出前把购物车持久化（cartStore 内部每次操作都持久化了，这里兜底写一次）
  try { cartStore.persist() } catch (e) {}
})
</script>

<style lang="scss">
/* 每个页面公共样式 */
@import '@/uni.scss';

/* 全局字体、通用 class */
.container {
  padding: $spacing-md;
  min-height: 100vh;
  box-sizing: border-box;
}

.gold-line {
  height: 2rpx;
  background: linear-gradient(90deg, transparent, $brand-gold, transparent);
  margin: $spacing-md 0;
}

.gold-text {
  color: $brand-gold-dark;
  font-weight: 600;
}

.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ellipsis-2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
