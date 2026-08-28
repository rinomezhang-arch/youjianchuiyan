/**
 * 本地存储封装：统一 key + 自动加前缀，异常兜底
 */
import { STORAGE_KEYS } from '@/config/env'

export const storage = {
  get(key, def = null) {
    try {
      const v = uni.getStorageSync(key)
      if (v === '' || v === null || v === undefined) return def
      try { return JSON.parse(v) } catch { return v }
    } catch (e) {
      console.warn('[storage] get fail:', key, e)
      return def
    }
  },
  set(key, value) {
    try {
      uni.setStorageSync(key, typeof value === 'string' ? value : JSON.stringify(value))
      return true
    } catch (e) {
      console.warn('[storage] set fail:', key, e)
      return false
    }
  },
  remove(key) {
    try { uni.removeStorageSync(key); return true } catch { return false }
  },
  clearAll() {
    Object.values(STORAGE_KEYS).forEach(k => this.remove(k))
  }
}

export default storage
