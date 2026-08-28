/**
 * 统一 HTTP 请求封装：基于 uni.request（跨 H5/小程序/APP 三端通用）
 * 特性：
 *   - 自动注入 Authorization: Bearer <token>
 *   - 自动给所有 GET 请求追加 storeId 参数（除非显式传 skipStoreId:true）
 *   - 统一响应结构判断：后端返回 {code, message, data}，code=200 视为成功
 *   - 401 自动清 token 并跳登录页
 *   - 错误统一 showToast，调用方只处理成功分支
 */
import { API_BASE_URL, DEFAULT_STORE_ID } from '@/config/env'
import storage from './storage'
import { useAppStore } from '@/store/app'

/**
 * 标准请求函数
 * @param {Object} opts
 * @param {string} opts.url       接口路径，如 /dishes
 * @param {string} opts.method    GET|POST|PUT|DELETE
 * @param {Object} opts.data      请求参数（GET=query，POST=body）
 * @param {Object} opts.header    自定义 header
 * @param {boolean} opts.loading  是否显示加载中，默认 true
 * @param {boolean} opts.skipToken  不要自动加 token，默认 false
 * @param {boolean} opts.skipStoreId GET 不要自动加 storeId，默认 false
 * @param {boolean} opts.silent   出错不弹 toast，默认 false
 * @param {number}  opts.timeout  超时ms，默认 15000
 */
export function request(opts = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    loading = false,
    skipToken = false,
    skipStoreId = false,
    silent = false,
    timeout = 15000
  } = opts

  if (!url) return Promise.reject(new Error('request url required'))

  // 1) loading
  if (loading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  // 2) 组装完整 URL
  let fullUrl = /^https?:/.test(url) ? url : `${API_BASE_URL}${url}`

  // 3) GET 请求自动追加 storeId（用 appStore.currentStoreId，没有则走默认）
  if (!skipStoreId && method.toUpperCase() === 'GET') {
    let sid = DEFAULT_STORE_ID
    try {
      const store = useAppStore()
      if (store && store.currentStoreId) sid = store.currentStoreId
    } catch {}
    data.storeId = data.storeId || sid
  }

  // 4) 构造请求
  const token = !skipToken ? (storage.get('yjcy_token') || '') : ''
  const fullHeader = {
    'Content-Type': 'application/json',
    ...header,
  }
  if (token) fullHeader['Authorization'] = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: fullUrl,
      method: method.toUpperCase(),
      data,
      header: fullHeader,
      timeout,
      success(res) {
        if (loading) uni.hideLoading()
        const body = res.data || {}

        // 标准后端契约：{code, message, data}
        if (typeof body === 'object' && 'code' in body) {
          if (body.code === 200 || body.code === 0) {
            resolve(body.data !== undefined ? body.data : body)
            return
          }
          if (body.code === 401) {
            storage.remove('yjcy_token')
            storage.remove('yjcy_user')
            if (!silent) {
              uni.showToast({ title: '请先登录', icon: 'none' })
              setTimeout(() => {
                uni.navigateTo({ url: '/pages/login/login' }).catch(() => {})
              }, 800)
            }
            reject(new Error(body.message || '未登录'))
            return
          }
          if (!silent) {
            uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          }
          reject(new Error(body.message || '请求失败'))
          return
        }

        // 后端未按标准结构返回：直接 resolve 完整 body
        resolve(body)
      },
      fail(err) {
        if (loading) uni.hideLoading()
        if (!silent) {
          uni.showToast({ title: err.errMsg || '网络异常', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

/* ===== 快捷方法 ===== */
export const http = {
  get:   (url, params, opt = {}) => request({ ...opt, url, method: 'GET',  data: params }),
  post:  (url, data,   opt = {}) => request({ ...opt, url, method: 'POST', data }),
  put:   (url, data,   opt = {}) => request({ ...opt, url, method: 'PUT',  data }),
  del:   (url, data,   opt = {}) => request({ ...opt, url, method: 'DELETE', data }),
  upload(url, filePath, formData = {}, name = 'file', header = {}) {
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: /^https?:/.test(url) ? url : `${API_BASE_URL}${url}`,
        filePath,
        name,
        formData,
        header: { Authorization: `Bearer ${storage.get('yjcy_token') || ''}`, ...header },
        success(r) {
          try {
            const body = JSON.parse(r.data)
            if (body.code === 200 || body.code === 0) resolve(body.data || body)
            else reject(new Error(body.message || '上传失败'))
          } catch (e) { reject(r.data) }
        },
        fail: reject
      })
    })
  }
}

export default http
