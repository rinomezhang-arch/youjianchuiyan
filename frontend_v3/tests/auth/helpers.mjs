// TR-AUTH-SHELL-14 测试公共设施。
// 用 axios 自定义 adapter 拦截 request 单例：store 与组件拿到的是同一条真实请求链路，
// 只把网络层换成可控假后端——不伪造 request.js/store 的任何一行代码。

import request from '@/utils/request'

/** @typedef {{status?:number, body:object}} MockReply */

let handler = null

/**
 * 安装假后端（同步生效，杜绝"adapter 未装好请求已飞"的竞态）。handler(url, config) 返回：
 *   { body }                      → 业务响应（HTTP 200，拦截器按 code 判定）
 *   { status, body }              → HTTP 级响应（status 非 2xx 时以 rejected promise 呈现）
 *   { networkError: 'message' }   → 网络层异常（error.response 不存在）
 */
export function useAdapter(fn) {
  handler = fn
  request.defaults.adapter = async (config) => {
    const url = String(config.url || '')
    const h = handler
    if (!h) throw new Error('测试未设置假后端 handler')
    const reply = await h(url, config)
    if (reply && reply.networkError) {
      const err = new Error(reply.networkError)
      err.config = config
      throw err
    }
    const status = reply?.status ?? 200
    const response = {
      data: reply?.body ?? {},
      status,
      statusText: String(status),
      headers: {},
      config
    }
    if (status >= 200 && status < 300) return response
    const err = new Error(`Request failed with status code ${status}`)
    err.response = response
    err.config = config
    err.isAxiosError = true
    throw err
  }
}

export function resetAdapter() {
  handler = null
}

/** 清空本地身份（测试之间互不污染） */
export function clearLocal() {
  localStorage.clear()
  sessionStorage.clear()
}

/** 预置一套已登录身份 */
export function seedIdentity({ token = 'test-token', storeId = 1, storeName = '宁国店', roles = ['staff'] } = {}) {
  localStorage.setItem('token', token)
  localStorage.setItem('storeId', String(storeId))
  localStorage.setItem('storeName', storeName)
  localStorage.setItem('roles', JSON.stringify(roles))
  localStorage.setItem('currentStoreId', String(storeId))
}

/** 让微任务队列（含 axios 链路）跑干净 */
export function flushAll() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

export async function flushAllDeep(times = 4) {
  for (let i = 0; i < times; i += 1) {
    await flushAll()
  }
}

/** 判定 401 时 window.location.href 是否真的被改写（happy-dom 里断言 pathname） */
export function currentPathname() {
  try {
    return window.location.pathname
  } catch {
    return null
  }
}

