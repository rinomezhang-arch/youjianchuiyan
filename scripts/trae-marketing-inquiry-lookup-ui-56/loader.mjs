// ESM 加载钩子：把 marketing.js 依赖的 '@/utils/request' 和 'axios' 解析到本目录合同桩，
// 使 API 契约测试可在纯 node 下运行（零安装、零网络）。仅测试用。
export async function resolve(specifier, context, nextResolve) {
  if (specifier === '@/utils/request') {
    return { url: new URL('./request-stub.mjs', import.meta.url).href, shortCircuit: true }
  }
  if (specifier === 'axios') {
    return { url: new URL('./axios-stub.mjs', import.meta.url).href, shortCircuit: true }
  }
  return nextResolve(specifier, context)
}
