import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({})
  const token = ref(localStorage.getItem('token') || '')
  const storeId = ref(Number(localStorage.getItem('storeId')) || null)
  const storeName = ref(localStorage.getItem('storeName') || '')
  const currentStore = ref({})
  const currentTable = ref({})
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
        userInfo.value = res.data
        storeId.value = res.data.storeId || storeId.value
        storeName.value = res.data.storeName || storeName.value
        // 同步保存到 localStorage
        const sid = res.data.staffId || res.data.staff_id || res.data.id
        const sname = res.data.staffName || res.data.staff_name || res.data.name || res.data.userName || res.data.username
        const sdept = res.data.department || res.data.dept || res.data.deptName || res.data.dept_name || res.data.departmentName || res.data.department_name
        if (sid) localStorage.setItem('staffId', sid)
        if (sname) localStorage.setItem('staffName', sname)
        if (sdept) localStorage.setItem('staffDept', sdept)
      }
    } catch (e) {
      // 仅401才登出（token失效），500等服务器错误保留登录状态
      if (e.response?.status === 401) {
        logout()
      }
      // 服务器错误时静默处理，不影响后续操作
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
      storeId.value = data.storeId || data.user?.storeId || null
      storeName.value = data.storeName || data.user?.storeName || ''
      const staffId = data.user?.staffId || data.user?.staff_id || data.user?.id || null
      const staffName = data.user?.staffName || data.user?.staff_name || data.user?.name || data.user?.userName || data.user?.username || ''
      const staffDept = data.user?.department || data.user?.dept || data.user?.deptName || data.user?.dept_name || data.user?.departmentName || data.user?.department_name || ''
      localStorage.setItem('storeId', storeId.value)
      localStorage.setItem('storeName', storeName.value)
      localStorage.setItem('staffId', staffId)
      localStorage.setItem('staffName', staffName)
      localStorage.setItem('staffDept', staffDept)
      initialized.value = true
    }
    return res
  }

  function selectStore(store) {
    storeId.value = store.id
    storeName.value = store.name
    currentStore.value = store
    localStorage.setItem('storeId', store.id)
    localStorage.setItem('storeName', store.name)
  }

  function selectTable(table) {
    currentTable.value = table
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
    localStorage.removeItem('staffId')
    localStorage.removeItem('staffName')
    localStorage.removeItem('staffDept')
    storeId.value = null
    storeName.value = ''
    initialized.value = false
  }

  return {
    userInfo,
    token,
    storeId,
    storeName,
    currentStore,
    currentTable,
    initialized,
    isLoggedIn,
    init,
    login,
    selectStore,
    selectTable,
    logout
  }
})
