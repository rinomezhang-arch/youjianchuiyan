// 客户端侧配置兼容：~/.openclaw/openclaw.json 里 auth.cooldowns 是历史遗留键，
// OpenClaw 2026.9.2 的 CLI 对 gateway call 做严格校验（"Unrecognized key: cooldowns"），
// 直接拒绝调用。网关进程本身不受影响（进程读的是它自己的配置），
// 这里只为**客户端 CLI**生成仅含 remote URL 的最小临时配置（不落 token，身份/权限不变），
// 通过 OPENCLAW_CONFIG_PATH 注入。禁止 doctor --fix、禁止改网关配置。
import { existsSync, writeFileSync, mkdirSync, mkdtempSync, chmodSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { homedir, tmpdir } from 'node:os'

const cachedPaths = new Map()

export function prepareClientConfig({ url, tempRoot = tmpdir(), useCache = true } = {}) {
  if (!url) throw new Error('gateway url required for minimal client config')
  if (useCache && cachedPaths.has(url)) return cachedPaths.get(url)
  // 当前 OpenClaw 对 CLI --url 强制要求凭据也出现在 argv；环境变量不足以满足该闸门。
  // 把非敏感 URL 放入官方 gateway.remote 配置并省略 --url，token 才可安全走官方环境变量。
  // 配置只含路由，不复制原 openclaw.json 的模型、渠道、认证或其他嵌套凭据。
  const cfg = { gateway: { mode: 'remote', remote: { url } } }
  mkdirSync(tempRoot, { recursive: true, mode: 0o700 })
  const dir = mkdtempSync(join(tempRoot, 'liveness-client-config-'))
  chmodSync(dir, 0o700)
  const dst = join(dir, 'openclaw.client.json')
  writeFileSync(dst, JSON.stringify(cfg), { encoding: 'utf8', mode: 0o600 })
  if (useCache) cachedPaths.set(url, dst)
  return dst
}

export function openclawCliPath() {
  // 与 openclaw-adapter.mjs 同一解析方式：全局 npm 安装的 openclaw.mjs
  const candidates = [
    join(process.env.APPDATA || join(homedir(), 'AppData', 'Roaming'), 'npm', 'node_modules', 'openclaw', 'openclaw.mjs'),
    join(homedir(), '.npm-global', 'lib', 'node_modules', 'openclaw', 'openclaw.mjs')
  ]
  const found = candidates.find(p => existsSync(p))
  if (!found) throw new Error('openclaw CLI entry not found')
  return found
}
