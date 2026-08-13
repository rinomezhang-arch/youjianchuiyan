import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true // 自动带 cookie
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    const storeId = localStorage.getItem('storeId')
    if (storeId) config.headers['X-Store-Id'] = storeId
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
