const configuredMode = String(import.meta.env.VITE_FALLBACK_MODE || 'dev').toLowerCase()

if (import.meta.env.PROD && configuredMode !== 'prod') {
  throw new Error('生产构建必须设置 VITE_FALLBACK_MODE=prod')
}

export const fallbackMode = configuredMode === 'prod' ? 'prod' : 'dev'
export const isDevFallbackEnabled = () => fallbackMode === 'dev'

export function fallbackOrThrow(error, fallback) {
  if (!isDevFallbackEnabled()) throw error
  return typeof fallback === 'function' ? fallback() : fallback
}

export function errorMessage(error, defaultMessage = '服务暂时不可用，请稍后重试') {
  return error?.response?.data?.message || error?.response?.data?.msg || error?.message || defaultMessage
}
