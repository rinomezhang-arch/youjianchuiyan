import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({})
  const token = ref(localStorage.getItem('token') || '')
  const storeId = ref(Number(localStorage.getItem('storeId')) || 1)
  const storeName = ref(localStorage.getItem('storeName') || '')
  const initialized = ref(false)
  const isLoggedIn = computed(() => !!token.value)

  async function init() {
    if (initialized.value) return
    if (!token.value) {
      initialized.value = true
      return
    }
    try {
      const res = await request({ url: '/auth/me', method: 'get' })
      if (res.code === 200 && res.data) {
        userInfo.value = res.data.user || {}
        storeId.value = res.data.storeId || storeId.value
        storeName.value = res.data.storeName || storeName.value
        localStorage.setItem('storeId', storeId.value)
        localStorage.setItem('storeName', storeName.value)
      }
    } catch {
      // token 失效
      logout()
    }
    initialized.value = true
  }

  async function login(username, password) {
    const res = await loginApi({ username, password })
    if (res.code === 200) {
      const data = res.data
      token.value = data.token
      localStorage.setItem('token', data.token)
      userInfo.value = data.user || {}
      storeId.value = data.storeId || data.user?.storeId || 1
      storeName.value = data.storeName || data.user?.storeName || ''
      localStorage.setItem('storeId', storeId.value)
      localStorage.setItem('storeName', storeName.value)
      initialized.value = true
    }
    return res
  }

  function selectStore(store) {
    storeId.value = store.id
    storeName.value = store.name
    localStorage.setItem('storeId', store.id)
    localStorage.setItem('storeName', store.name)
  }

  function switchStore(id) {
    storeId.value = id
    const nameMap = { 1: '宁国店', 2: '宣城店' }
    storeName.value = nameMap[id] || ''
    localStorage.setItem('storeId', id)
    localStorage.setItem('storeName', storeName.value)
  }

  async function logout() {
    try {
      await request({ url: '/auth/logout', method: 'post' })
    } catch {
      // pass
    }
    userInfo.value = {}
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('storeId')
    localStorage.removeItem('storeName')
    storeId.value = 1
    storeName.value = ''
    initialized.value = false
  }

  return {
    userInfo,
    token,
    storeId,
    storeName,
    initialized,
    isLoggedIn,
    init,
    login,
    selectStore,
    switchStore,
    logout
  }
})
