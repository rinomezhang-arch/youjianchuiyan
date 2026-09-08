import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import request from '@/utils/request'
import { canAccessPath, landingPath } from '@/utils/moduleAccess'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({})
  const token = ref(localStorage.getItem('token') || '')
  const storeId = ref(Number(localStorage.getItem('storeId')) || 1)
  const storeName = ref(localStorage.getItem('storeName') || '')
  const initialized = ref(false)
  // 板块白名单：由后端下发，非空表示该员工只能进入这些板块
  const modules = ref(readModules())
  const isLoggedIn = computed(() => !!token.value)
  const moduleRestricted = computed(() => modules.value.length > 0)

  function readModules() {
    try {
      const raw = JSON.parse(localStorage.getItem('modules') || '[]')
      return Array.isArray(raw) ? raw.filter(m => typeof m === 'string') : []
    } catch {
      return []
    }
  }

  function persistModules(list) {
    modules.value = Array.isArray(list) ? list.filter(m => typeof m === 'string') : []
    localStorage.setItem('modules', JSON.stringify(modules.value))
  }

  /** 是否可以进入某个页面路径（受限用户只能进入自身板块） */
  function canAccess(path) {
    return canAccessPath(modules.value, path)
  }

  /** 登录后的落地页：受限用户直接落到自身板块首页 */
  function homePath() {
    return landingPath(modules.value)
  }

  async function init() {
    if (initialized.value) return
    if (!token.value) {
      initialized.value = true
      return
    }
    try {
      const res = await request({ url: '/auth/me', method: 'get' })
      if (res.code === 200 && res.data) {
        userInfo.value = res.data
        storeId.value = res.data.storeId || storeId.value
        storeName.value = res.data.storeName || storeName.value
        persistModules(res.data.modules)
      }
    } catch {
      // token 失效
      logout()
    }
    initialized.value = true
  }

  async function login(username, password) {
    const account = (username || '').trim()
    const secret = password || ''
    // 硬约束：账号或密码为空一律不发起请求，杜绝无密码进入
    if (!account || !secret.trim()) {
      return { code: 400, message: '账号和密码均不能为空', data: null }
    }
    const res = await loginApi({ username: account, password: secret })
    // 硬约束：必须拿到后端签发的非空 token 才写入本地会话
    const issuedToken = res?.code === 200 ? res.data?.token : null
    if (!issuedToken || typeof issuedToken !== 'string') {
      clearSession()
      return res?.code === 200
        ? { code: 401, message: '登录失败：服务端未签发有效凭证', data: null }
        : res
    }
    const data = res.data
    token.value = issuedToken
    localStorage.setItem('token', issuedToken)
    userInfo.value = data.user || {}
    storeId.value = data.storeId || data.user?.storeId || 1
    storeName.value = data.storeName || data.user?.storeName || ''
    localStorage.setItem('storeId', storeId.value)
    localStorage.setItem('storeName', storeName.value)
    persistModules(data.modules)
    initialized.value = true
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

  // 清空本地会话（不调用后端），登录失败/凭证非法时使用
  function clearSession() {
    userInfo.value = {}
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('storeId')
    localStorage.removeItem('storeName')
    localStorage.removeItem('currentStoreId')
    localStorage.removeItem('roles')
    localStorage.removeItem('modules')
    modules.value = []
    storeId.value = 1
    storeName.value = ''
    initialized.value = false
  }

  async function logout() {
    try {
      await request({ url: '/auth/logout', method: 'post' })
    } catch {
      // pass
    }
    clearSession()
  }

  return {
    userInfo,
    token,
    storeId,
    storeName,
    initialized,
    isLoggedIn,
    modules,
    moduleRestricted,
    canAccess,
    homePath,
    init,
    login,
    clearSession,
    selectStore,
    switchStore,
    logout
  }
})
