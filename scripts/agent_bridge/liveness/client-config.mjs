// 客户端侧配置兼容：~/.openclaw/openclaw.json 里 auth.cooldowns 是历史遗留键，
// OpenClaw 2026.9.2 的 CLI 对 gateway call 做严格校验（"Unrecognized key: cooldowns"），
// 直接拒绝调用。网关进程本身不受影响（进程读的是它自己的配置），
// 这里只为**客户端 CLI**生成一份删去未知键的临时副本（不落 token，身份/权限不变），
// 通过 OPENCLAW_CONFIG_PATH 注入。禁止 doctor --fix、禁止改网关配置。
import { existsSync, readFileSync, writeFileSync, mkdirSync, mkdtempSync, chmodSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { homedir, tmpdir } from 'node:os'

let cachedPath = null

export function prepareClientConfig({ srcPath = join(homedir(), '.openclaw', 'openclaw.json'), tempRoot = tmpdir(), useCache = true } = {}) {
  if (useCache && cachedPath) return cachedPath
  const cfg = JSON.parse(readFileSync(srcPath, 'utf8'))
  // 只删触发校验失败的未知键；其余非凭据字段原样保留
  if (cfg?.auth && Object.prototype.hasOwnProperty.call(cfg.auth, 'cooldowns')) {
    delete cfg.auth.cooldowns
  }
  // token 通过 OpenClaw 官方支持的 OPENCLAW_GATEWAY_TOKEN 传入子进程；
  // 临时兼容配置不落凭据，也不改变原配置、身份或权限。
  if (cfg?.gateway?.auth && Object.prototype.hasOwnProperty.call(cfg.gateway.auth, 'token')) {
    delete cfg.gateway.auth.token
  }
  mkdirSync(tempRoot, { recursive: true, mode: 0o700 })
  const dir = mkdtempSync(join(tempRoot, 'liveness-client-config-'))
  chmodSync(dir, 0o700)
  const dst = join(dir, 'openclaw.client.json')
  writeFileSync(dst, JSON.stringify(cfg), { encoding: 'utf8', mode: 0o600 })
  if (useCache) cachedPath = dst
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
