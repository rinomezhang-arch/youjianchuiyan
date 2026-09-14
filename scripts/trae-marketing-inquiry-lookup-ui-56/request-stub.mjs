// '@/utils/request' 合同桩：记录调用配置并返回拦截器出口形态（{code,message,data}）。
// 仅在 node --import 钩子下被加载；不发起任何网络请求。
export const calls = []

async function requestStub(config) {
  calls.push({ config, at: new Date().toISOString() })
  return { code: 200, message: 'ok', data: null }
}

requestStub.get = (url, config = {}) => requestStub({ url, method: 'get', ...config })
requestStub.post = (url, data, config = {}) => requestStub({ url, method: 'post', data, ...config })
requestStub.put = (url, data, config = {}) => requestStub({ url, method: 'put', data, ...config })

export function resetCalls() {
  calls.length = 0
}

export default requestStub
