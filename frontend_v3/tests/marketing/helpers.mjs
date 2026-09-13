// TR-MARKETING-H5-UI-39 测试公共设施。
//
// ⚠️ MOCKED_CONTRACT：后端营销接口尚未接通。这里与 tests/auth 同款，用 axios 自定义
// adapter 替换 utils/request 单例的网络层——组件拿到的是真实请求/拦截器链路，
// 只把 HTTP 换成合同假后端。所有用例均为 MOCKED_CONTRACT，不冒充真实 HTTP/数据库闭环。
import request from '@/utils/request'

let handler = null
/** @type {Array<{method:string,url:string,data:object}>} 全部出站请求记录 */
export const calls = []

/**
 * 安装假后端。handler(url, config, callNo) 返回：
 *   { body }                      → HTTP 200（拦截器按 body.code 判定业务成败）
 *   { status, body }              → HTTP 级状态（非 2xx reject）
 *   { networkError }              → 无响应网络异常
 */
export function useAdapter(fn) {
  calls.length = 0
  handler = fn
  request.defaults.adapter = async (config) => {
    const url = String(config.url || '')
    const method = String(config.method || 'get').toLowerCase()
    let data = config.data
    if (typeof data === 'string') {
      try { data = JSON.parse(data) } catch { /* 保留原文 */ }
    }
    calls.push({ method, url, data: data || null, params: config.params || {} })
    const reply = await handler(url, config, calls.length)
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
  calls.length = 0
}

export function clearLocal() {
  localStorage.clear()
  sessionStorage.clear()
}

export function flushAll() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

export async function flushAllDeep(times = 5) {
  for (let i = 0; i < times; i += 1) await flushAll()
}

/** 找某 url 的全部出站调用 */
export function callsTo(urlPart, method) {
  return calls.filter((c) => c.url.includes(urlPart) && (!method || c.method === method))
}

/** 对 teleport 到 body 的原生 input 赋值并派发 input 事件（v-model 真实链路）。 */
export function setNativeInput(el, value) {
  const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype
  const setter = Object.getOwnPropertyDescriptor(proto, 'value').set
  setter.call(el, String(value))
  el.dispatchEvent(new Event('input', { bubbles: true }))
  el.dispatchEvent(new Event('change', { bubbles: true }))
}

export function inputByPlaceholder(placeholder, root = document) {
  return [...root.querySelectorAll('input,textarea')].find((el) => el.placeholder === placeholder)
}

export const OK = (data) => ({ body: { code: 200, data } })
export const FAIL_HTTP = (status = 500, message = 'error') => ({ status, body: { code: status, message } })
export const NETWORK_FAIL = (msg = 'Network Error') => ({ networkError: msg })
