/**
 * 通用工具函数
 */

/* ===== 金额分/元 ===== */
export const fenToYuan = (fen) => {
  if (fen === null || fen === undefined || isNaN(fen)) return '0.00'
  return (Number(fen) / 100).toFixed(2)
}
export const yuanToFen = (yuan) => Math.round(Number(yuan || 0) * 100)
export const formatPrice = (v) => `¥${fenToYuan(v)}`

/* ===== 日期 ===== */
export function pad(n) { return n < 10 ? '0' + n : '' + n }

export function formatDate(d, withTime = true) {
  d = d instanceof Date ? d : new Date(d)
  const s = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (!withTime) return s
  return `${s} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function todayStr() {
  return formatDate(new Date(), false)
}

export function addDays(dateStr, n) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + n)
  return formatDate(d, false)
}

/* ===== 常用校验 ===== */
export const isMobile = (s) => /^1[3-9]\d{9}$/.test(String(s || '').trim())
export const isEmpty = (v) => v === null || v === undefined || String(v).trim() === ''

/* ===== 防抖/节流 ===== */
export function debounce(fn, wait = 300) {
  let t = null
  return function (...args) {
    clearTimeout(t)
    t = setTimeout(() => fn.apply(this, args), wait)
  }
}
export function throttle(fn, wait = 300) {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last >= wait) { last = now; fn.apply(this, args) }
  }
}

/* ===== 深拷贝（简单场景） ===== */
export function deepClone(o) {
  if (o === null || typeof o !== 'object') return o
  if (Array.isArray(o)) return o.map(i => deepClone(i))
  const out = {}
  for (const k of Object.keys(o)) out[k] = deepClone(o[k])
  return out
}

/* ===== 平台判断 ===== */
export function isMpWeixin() {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  // #ifndef MP-WEIXIN
  return false
  // #endif
}

export function isH5Weixin() {
  // #ifdef H5
  return /MicroMessenger/i.test(navigator.userAgent || '')
  // #endif
  // #ifndef H5
  return false
  // #endif
}
