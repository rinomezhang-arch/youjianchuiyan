import axios from 'axios'
import { ElMessage } from 'element-plus'
// iPad 设备绑定函数（内联定义，避免循环依赖）
function isDeviceBound() {
  return localStorage.getItem('ipad_device_bound') === 'true'
}
function getDeviceSn() {
  return localStorage.getItem('ipad_device_sn') || ''
}
function getDeviceStoreId() {
  return localStorage.getItem('ipad_store_id') || '1'
}
function getDeviceStaffId() {
  return localStorage.getItem('ipad_staff_id') || ''
}
const DEVICE_UNBOUND_EVENT = 'ipad-device-unbound'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

// 判断是否为 iPad 接口请求（URL 包含 /ipad/）
function isIpadRequest(url) {
  return !!url && url.includes('/ipad/')
}

request.interceptors.request.use(
  (config) => {
    // 兼容历史页面中以 /api 开头的路径，避免与 baseURL 组合成 /api/api/*。
    if (config.url === '/api') config.url = '/'
    else if (config.url?.startsWith('/api/')) config.url = config.url.slice(4)

    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 自动注入 storeId 参数：后端部分接口要求 @RequestParam storeId 必传
    // 优先级：业务显式传入 > localStorage.currentStoreId > localStorage.storeId
    // 超级管理员 storeId=0 时默认查宁国店(1)，避免空数据
    if (config.method === 'get') {
      if (!config.params) config.params = {}
      if (config.params.storeId === undefined) {
        let sid = localStorage.getItem('currentStoreId') || localStorage.getItem('storeId')
        if (sid === '0' || !sid) sid = '1'
        config.params.storeId = sid
      }
    }

    // iPad 请求拦截：检查设备 SN 绑定状态
    // 如果没有 SN 或 SN 未绑定，派发未绑定事件并拒绝请求
    if (isIpadRequest(config.url)) {
      if (!isDeviceBound()) {
        console.warn('[iPad拦截器] 设备 SN 未绑定，拦截请求:', config.url)
        window.dispatchEvent(new CustomEvent(DEVICE_UNBOUND_EVENT, { detail: { url: config.url } }))
        return Promise.reject(new Error('设备未绑定，请先完成设备绑定'))
      }
      // 动态注入 iPad 必需请求头（覆盖业务侧可能传入的硬编码值）
      config.headers['X-Device-Sn'] = getDeviceSn()
      config.headers['X-Store-Id'] = String(getDeviceStoreId())
      config.headers['X-Staff-Id'] = getDeviceStaffId()
      config.headers['X-Client-Type'] = 'ipad'
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      // iPad 设备相关错误（后端返回设备未绑定 / SN 失效）：清除本地绑定并派发事件
      if (isIpadRequest(response.config?.url) && res.code === 401 && res.message && res.message.includes('设备')) {
        localStorage.removeItem('ipad_device_sn')
        localStorage.removeItem('ipad_device_bound')
        localStorage.removeItem('ipad_store_id')
        localStorage.removeItem('ipad_staff_id')
        window.dispatchEvent(new CustomEvent(DEVICE_UNBOUND_EVENT, { detail: { url: response.config?.url } }))
        return Promise.reject(new Error(res.message || '设备未绑定'))
      }
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // 请求拦截器因未绑定拒绝时，error.message 为"设备未绑定..."，无需额外提示
    if (error?.message && error.message.includes('设备未绑定')) {
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
