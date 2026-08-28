/**
 * 微信小程序 原生能力封装
 *  1) 登录：uni.login() -> code -> 后端 /auth/wx-login -> { token, openId, userInfo?, phone? }
 *  2) 获取手机号：<button open-type="getPhoneNumber" .../> 后处理 code -> 后端 /auth/wx-phone
 *  3) 支付：后端 /pay/unified-order 返回参数 -> uni.requestPayment()
 *  4) 订阅消息：uni.requestSubscribeMessage(tmplIds)
 *
 *  ⚠️ 后端需要补充的接口（如果还没实现，直接用 user.js + 微信官方SDK即可，
 *     微信登录、手机号、支付三个接口是小程序端必备的）
 *
 *   POST /auth/wx-login
 *        参数：{ code }   （可选 encryptedData/iv 新版不用了，解密走 code2Session）
 *        返回：{ token, openId, userInfo?: {name, avatar, phone} }
 *
 *   POST /auth/wx-phone
 *        参数：{ code } （来自 getPhoneNumber 返回的 code，不是手机号本体）
 *        返回：{ phoneNumber, purePhoneNumber, countryCode }
 *
 *   POST /pay/create
 *        参数：{ orderType:'booking'|'dish', bizId, amountFen, attach? }
 *        返回：{ timeStamp, nonceStr, package, signType, paySign } （微信 JSAPI/小程序支付签名）
 */
import http from '@/utils/request'
import { useAppStore } from '@/store/app'
import { SUBSCRIBE_TMPL_IDS } from '@/config/env'

/* =========================================================
 *  1) 微信登录（启动时静默走一次；失败不阻塞，需要时再弹手动登录）
 * ========================================================= */
export async function wechatMiniLogin() {
  // #ifdef MP-WEIXIN
  try {
    const { code } = await uni.login({ provider: 'weixin' })
    if (!code) throw new Error('uni.login 返回空 code')
    const res = await http.post('/auth/wx-login', { code }, { skipToken: true, silent: true })
    return res || {}
  } catch (e) {
    // 失败静默返回，不影响首屏渲染
    return null
  }
  // #endif

  // #ifndef MP-WEIXIN
  return Promise.resolve(null)
  // #endif
}

/** 启动失败后，再尝试一次；仍失败引导用户去登录页 */
export async function ensureWechatUser() {
  // #ifdef MP-WEIXIN
  const appStore = useAppStore()
  if (appStore.isLogin) return true
  const data = await wechatMiniLogin()
  if (data && data.token) {
    appStore.setToken(data.token)
    if (data.userInfo) appStore.setUserInfo(data.userInfo)
    if (data.openId)   appStore.setOpenId(data.openId)
    return true
  }
  return false
  // #endif
  // #ifndef MP-WEIXIN
  return Promise.resolve(false)
  // #endif
}

/* =========================================================
 *  2) 获取手机号（按钮触发，需用户点击同意）
 * ========================================================= */
/** 轻量提示框（uni.showToast 封装） */
export function toast(title, icon = 'none') {
  uni.showToast({ title, icon })
}

/**
 * 通过 code 直接获取手机号（返回后端完整响应，含 phoneNumber）
 * 用法：fetchPhoneNumber(e.detail.code).then(r => r.phoneNumber)
 */
export async function fetchPhoneNumber(code) {
  if (!code) throw new Error('缺少手机号授权 code')
  const res = await http.post('/auth/wx-phone', { code }, { skipToken: false })
  const phone = (res && (res.phoneNumber || res.phone)) || ''
  if (phone) {
    useAppStore().setPhoneNumber(phone)
    useAppStore().setUserInfo({ phone })
  }
  return res || {}
}

export async function wechatGetPhoneFromEvent(e) {
  const detail = (e && e.detail) || {}
  if (detail.errMsg && detail.errMsg !== 'getPhoneNumber:ok') {
    throw new Error('用户拒绝授权手机号')
  }
  const code = detail.code
  if (!code) throw new Error('getPhoneNumber 未返回 code')

  // #ifdef MP-WEIXIN
  const res = await http.post('/auth/wx-phone', { code }, { skipToken: false })
  const phone = (res && (res.phoneNumber || res.phone)) || ''
  if (phone) {
    useAppStore().setPhoneNumber(phone)
    useAppStore().setUserInfo({ phone })
  }
  return phone
  // #endif

  // #ifndef MP-WEIXIN
  return Promise.resolve('')
  // #endif
}

/* =========================================================
 *  3) 微信支付
 *     输入：后端 /pay/create 返回的签名包
 *     返回：微信支付结果（success/fail）
 * ========================================================= */
export function wechatPay(signPack) {
  return new Promise((resolve, reject) => {
    if (!signPack || !signPack.package) {
      reject(new Error('后端未返回支付签名'))
      return
    }
    const params = {
      provider: 'wxpay',
      timeStamp: String(signPack.timeStamp || signPack.timestamp),
      nonceStr:  signPack.nonceStr || signPack.noncestr,
      package:   signPack.package,
      signType:  signPack.signType || 'RSA',
      paySign:   signPack.paySign,
      success: resolve,
      fail: reject
    }
    // #ifdef MP-WEIXIN
    wx.requestPayment(params)
    // #endif
    // #ifdef H5
    uni.showToast({ title: '请在微信小程序内完成支付', icon: 'none' })
    reject(new Error('H5 暂不支持微信小程序支付'))
    // #endif
  })
}

/* =========================================================
 *  4) 订阅消息授权
 *     场景：预订提交成功后调用，请求 "预订成功" 和 "到店提醒" 两张模板
 * ========================================================= */
export function requestSubscribe(...keys) {
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    const ids = (keys && keys.length ? keys : Object.keys(SUBSCRIBE_TMPL_IDS))
      .map(k => SUBSCRIBE_TMPL_IDS[k] || k)
      .filter(x => x && !x.startsWith('TMPL_ID_')) // 过滤掉占位空值
    if (!ids.length) { resolve({ skipped: true, reason: 'no tmplIds' }); return }
    uni.requestSubscribeMessage({
      tmplIds: ids,
      success: (r) => resolve({ ok: true, detail: r }),
      fail:    (r) => resolve({ ok: false, detail: r })
    })
    // #endif
    // #ifndef MP-WEIXIN
    resolve({ skipped: true, reason: 'not wechat mini' })
    // #endif
  })
}

/* 别名：部分页面使用 wxPay 命名导入 */
export const wxPay = wechatPay

export default {
  wechatMiniLogin, ensureWechatUser,
  wechatGetPhoneFromEvent, fetchPhoneNumber,
  wechatPay, wxPay,
  toast,
  requestSubscribe
}
