// 'axios' 合同桩：记录 publicLookupClient 的调用配置并返回拦截器出口形态。
// 与 request-stub 共用 calls 数组，使 api-contract 测试可同时验证 request 和 axios 两条路径。
export const calls = []

function axiosCreate(opts) {
  function client(config) {
    calls.push({ config, at: new Date().toISOString() })
    // 返回 axios response 形态（marketing.js 里 .then(res => res.data)）
    return Promise.resolve({ data: { code: 200, message: 'ok', data: null } })
  }
  client.get = (url, config = {}) => client({ url, method: 'get', ...config })
  client.post = (url, data, config = {}) => client({ url, method: 'post', data, ...config })
  client.put = (url, data, config = {}) => client({ url, method: 'put', data, ...config })
  return client
}

export function resetCalls() {
  calls.length = 0
}

export default { create: axiosCreate }
