import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import request from '@/utils/request'
import { canonicalRole, normalizeStoreId, canSwitchTo, clearIdentity } from '@/utils/authScope'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({})
  const token = ref(localStorage.getItem('token') || '')
  // storeId=0 是总经理的合法身份（全店视角），不能用 || 1 兜底吞掉
  const storeId = ref(normalizeStoreId(localStorage.getItem('storeId'), 1))
  const storeName = ref(localStorage.getItem('storeName') || '')
  // Login.vue 一直按 userStore.roles 读角色数组，这里把契约补上（此前从未真正存在）
  const roles = ref(parseRoles(localStorage.getItem('roles')))
  const initialized = ref(false)
  const isLoggedIn = computed(() => !!token.value)

  function parseRoles(raw) {
    try {
      const list = JSON.parse(raw || '[]')
      return Array.isArray(list) ? list : []
    } catch {
      return []
    }
  }

  function persistIdentity() {
    localStorage.setItem('token', token.value)
    localStorage.setItem('storeId', String(storeId.value))
    localStorage.setItem('storeName', storeName.value)
    localStorage.setItem('roles', JSON.stringify(roles.value))
    localStorage.setItem('currentStoreId', String(storeId.value))
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
        // /auth/me 返回 {storeName, storeId, user:{...}}，真正的用户字段在 res.data.user 里
        // （之前误取了 res.data 本身，导致每次刷新页面后 userInfo.role/staffName 都是 undefined）
        userInfo.value = res.data.user || {}
        // 后端没给 storeId 时才保留本地值；给了就必须尊重 0（总经理）
        storeId.value = normalizeStoreId(res.data.storeId, storeId.value)
        storeName.value = res.data.storeName || storeName.value
        roles.value = [canonicalRole(userInfo.value, roles.value)]
        persistIdentity()
      }
    } catch {
      // token 失效：清本地身份后回登录页，不残留半套状态
      clearIdentity(localStorage)
      userInfo.value = {}
      token.value = ''
      storeId.value = 1
      storeName.value = ''
      roles.value = []
      initialized.value = false
    }
    initialized.value = true
  }

  async function login(username, password) {
    const res = await loginApi({ username, password })
    if (res.code === 200) {
      const data = res.data
      token.value = data.token
      userInfo.value = data.user || {}
      storeId.value = normalizeStoreId(data.storeId ?? data.user?.storeId, 1)
      storeName.value = data.storeName || data.user?.storeName || ''
      // 角色以服务端 userInfo.role 为权威（canonicalRole 内部保证本地 roles 不参与提权）
      roles.value = [canonicalRole(userInfo.value, [])]
      initialized.value = true
      persistIdentity()
    }
    return res
  }

  function selectStore(store) {
    if (!store || !canSwitchTo(currentRole(), storeId.value, store.id)) {
      return false
    }
    storeId.value = store.id
    storeName.value = store.name
    localStorage.setItem('storeId', store.id)
    localStorage.setItem('storeName', store.name)
    localStorage.setItem('currentStoreId', String(store.id))
    return true
  }

  function currentRole() {
    // 服务端 userInfo.role 权威优先；本地 roles 只在服务端 role 缺失时兜底（canonicalRole 内部保证）
    return canonicalRole(userInfo.value, roles.value)
  }

  function switchStore(id) {
    // 越权切换在此直接拒绝：不改动任何状态，调用方负责提示
    if (!canSwitchTo(currentRole(), storeId.value, id)) {
      return false
    }
    storeId.value = id
    const nameMap = { 1: '宁国店', 2: '宣城店' }
    storeName.value = nameMap[id] || ''
    localStorage.setItem('storeId', id)
    localStorage.setItem('storeName', storeName.value)
    localStorage.setItem('currentStoreId', String(id))
    return true
  }

  async function logout() {
    // 先清本地身份再通知服务端：logout 请求失败（网络断、token 已失效、服务端 500）
    // 都不允许留下能二次进入后台的本地状态。
    const capturedToken = token.value || localStorage.getItem('token') || ''
    const removed = clearIdentity(localStorage)
    userInfo.value = {}
    token.value = ''
    storeId.value = 1
    storeName.value = ''
    roles.value = []
    initialized.value = false
    try {
      await request({
        url: '/auth/logout',
        method: 'post',
        headers: capturedToken ? { Authorization: `Bearer ${capturedToken}` } : {}
      })
    } catch {
      // 服务端通知失败不影响本地清理结果
    }
    return removed
  }

  return {
    userInfo,
    token,
    storeId,
    storeName,
    roles,
    initialized,
    isLoggedIn,
    init,
    login,
    selectStore,
    switchStore,
    logout
  }
})
