import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

// 错误提示去重机制
const errorMap = new Map()
const ERROR_DURATION = 3000 // 错误提示持续时间

function showError(message, key) {
  const now = Date.now()
  const lastTime = errorMap.get(key) || 0
  if (now - lastTime < ERROR_DURATION) {
    return // 3秒内相同错误只显示一次
  }
  errorMap.set(key, now)
  ElMessage.error({
    message,
    duration: 2000,
    showClose: false
  })
}

request.interceptors.request.use(
  (config) => {
    if (config.url?.startsWith('/menu-api')) {
      config.baseURL = ''
    }
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const storeId = localStorage.getItem('storeId')
    if (storeId) {
      config.headers['X-Store-Id'] = storeId
    }
    const staffId = localStorage.getItem('staffId')
    if (staffId) {
      config.headers['X-Staff-Id'] = staffId
    }
    config.headers['X-Device-Sn'] = 'web'
    config.headers['X-Client-Type'] = 'web'
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
      if (res.code === 401) {
        window.location.href = '/login'
      } else {
        // 默认静默处理业务错误，不在此处弹提示
        // 调用方自行处理错误
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // 500等服务器错误静默处理，避免重复弹窗
    // 只在严重错误时显示一次提示
    const status = error.response?.status
    const url = error.config?.url || ''
    if (status >= 500) {
      // 服务器错误只记录日志，不弹窗
      console.warn(`API ${url} 服务器错误:`, error.message)
    }
    return Promise.reject(error)
  }
)

export default request
