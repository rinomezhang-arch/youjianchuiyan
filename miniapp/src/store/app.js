/**
 * 全局 App Store：用户/门店/Token/OpenId
 */
import { defineStore } from 'pinia'
import storage from '@/utils/storage'
import { STORAGE_KEYS, DEFAULT_STORE_ID } from '@/config/env'

export const useAppStore = defineStore('app', {
  state: () => ({
    token:           storage.get(STORAGE_KEYS.TOKEN, '') || '',
    userInfo:        storage.get(STORAGE_KEYS.USER, {}) || {},
    openId:          storage.get(STORAGE_KEYS.OPENID, '') || '',
    stores:          storage.get(STORAGE_KEYS.STORES, []) || [],
    currentStoreId:  storage.get(STORAGE_KEYS.STORE_ID, DEFAULT_STORE_ID) || DEFAULT_STORE_ID,
    /** 微信手机号（getPhoneNumber 授权后保存，便于预订时自动填充） */
    phoneNumber:     '',
  }),
  getters: {
    isLogin:      (s) => !!s.token,
    currentStore: (s) => (s.stores || []).find(x => x.id === s.currentStoreId) || null,
    userName:     (s) => s.userInfo?.nickname || s.userInfo?.name || (s.userInfo?.phone ? `${String(s.userInfo.phone).slice(-4)}用户` : '') || '未登录',
    userAvatar:   (s) => s.userInfo?.avatar || '',
  },
  actions: {
    setToken(t) { this.token = t || ''; storage.set(STORAGE_KEYS.TOKEN, this.token) },
    setOpenId(o) { this.openId = o || ''; storage.set(STORAGE_KEYS.OPENID, this.openId) },
    setUserInfo(u) {
      this.userInfo = { ...(this.userInfo || {}), ...(u || {}) }
      storage.set(STORAGE_KEYS.USER, this.userInfo)
    },
    setStores(list) {
      this.stores = Array.isArray(list) ? list : []
      storage.set(STORAGE_KEYS.STORES, this.stores)
    },
    setCurrentStoreId(id) {
      if (!id) return
      this.currentStoreId = Number(id)
      storage.set(STORAGE_KEYS.STORE_ID, this.currentStoreId)
      // 切换门店后清空购物车（不同门店库存/菜品可能不一致）
      try {
        const { useCartStore } = require('./cart')
        useCartStore().clear()
      } catch {}
    },
    setPhoneNumber(p) { this.phoneNumber = p || '' },
    logout() {
      this.token = ''; this.userInfo = {}; this.openId = ''; this.phoneNumber = ''
      storage.remove(STORAGE_KEYS.TOKEN)
      storage.remove(STORAGE_KEYS.USER)
      storage.remove(STORAGE_KEYS.OPENID)
    }
  }
})
