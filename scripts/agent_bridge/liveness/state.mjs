// TR-COLLAB-LIVENESS-17 共享：成员定义、状态日志、幂等去重与恢复限流。
// 所有写入只落 artifacts/coordination-r3/liveness/（证据目录），不碰网关配置与凭证。
import { existsSync, mkdirSync, readFileSync, writeFileSync, appendFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { homedir } from 'node:os'

const here = dirname(fileURLToPath(import.meta.url))
// 生产固定证据目录；夹具测试通过 LIVENESS_EVIDENCE_DIR 注入临时目录隔离
export const EVIDENCE_DIR = process.env.LIVENESS_EVIDENCE_DIR
  || join(here, '..', '..', '..', 'artifacts', 'coordination-r3', 'liveness')

// 固定成员与固定标识：来自任务 JSON / 交接文档，不允许命令行临时改身份或地址。
export const MEMBERS = {
  dilong: {
    id: 'dilong',
    name: '地龙',
    url: 'ws://127.0.0.1:18789',
    sessionKey: 'agent:main:main',
    marker: 'CX-DL-AUTH16-0908-01',
    idempotencyKey: '4b6f0d31-76c3-4f6b-a3ce-19dbaf87e216',
    // 续做指令用独立固定键：与原投递区分，重跑同一恢复仍幂等不重复
    resumeIdempotencyKey: '4b6f0d31-76c3-4f6b-a3ce-19dbaf87e216-resume-r1'
  },
  tianlong: {
    id: 'tianlong',
    name: '天龙',
    url: 'ws://100.70.215.11:11500',
    sessionKey: 'agent:main:main',
    marker: 'CX-TL-DATAMAP14-0908-01',
    idempotencyKey: '6da939f1-8084-41d7-9ed9-e979da1fe39d',
    resumeIdempotencyKey: '6da939f1-8084-41d7-9ed9-e979da1fe39d-resume-r1'
  }
}

// 恢复限流：每成员 30 分钟最多 2 次恢复尝试（任务 acceptance 硬约束）。
const RATE_WINDOW_MS = 30 * 60 * 1000
const RATE_MAX_ATTEMPTS = 2

export function ensureDir(dir) {
  mkdirSync(dir, { recursive: true })
  return dir
}

export function statePath(memberId) {
  ensureDir(EVIDENCE_DIR)
  return join(EVIDENCE_DIR, `state-${memberId}.json`)
}

export function readState(memberId) {
  const p = statePath(memberId)
  if (!existsSync(p)) return { probes: [], activations: [], blocked: null }
  try {
    return JSON.parse(readFileSync(p, 'utf8'))
  } catch {
    return { probes: [], activations: [], blocked: null }
  }
}

export function writeState(memberId, state) {
  ensureDir(EVIDENCE_DIR)
  writeFileSync(statePath(memberId), JSON.stringify(state, null, 2), { encoding: 'utf8', mode: 0o600 })
}

/** 追加脱敏事件日志（只记状态/时间/消息ID/计数，绝不记会话正文或凭证）。 */
export function logEvent(event) {
  ensureDir(EVIDENCE_DIR)
  const line = JSON.stringify({ at: new Date().toISOString(), ...event })
  appendFileSync(join(EVIDENCE_DIR, 'liveness-events.jsonl'), line + '\n')
  return line
}

/** 30 分钟窗口内恢复尝试次数；超限返回 false，调用方必须转为 blocked 而不是重试。 */
export function recoveryAllowed(memberId, now = Date.now()) {
  return recoveryAttemptCount(memberId, now) < RATE_MAX_ATTEMPTS
}

export function recoveryAttemptCount(memberId, now = Date.now()) {
  const state = readState(memberId)
  return state.activations.filter(a => {
    const at = Date.parse(a.at)
    return Number.isFinite(at) && now >= at && now - at < RATE_WINDOW_MS
  }).length
}

export function recordProbe(memberId, result) {
  const state = readState(memberId)
  state.probes.push({ at: new Date().toISOString(), ...result })
  // 只保留最近 200 条探测，证据文件不膨胀
  state.probes = state.probes.slice(-200)
  writeState(memberId, state)
}

export function recordActivation(memberId, record) {
  const state = readState(memberId)
  state.activations.push({ at: new Date().toISOString(), ...record })
  writeState(memberId, state)
}

export function markBlocked(memberId, reason) {
  const state = readState(memberId)
  state.blocked = { at: new Date().toISOString(), reason }
  writeState(memberId, state)
  logEvent({ member: memberId, kind: 'blocked', reason })
}

/** 幂等：只有完成真实活动验证的记录才算成功，pending/失败仍需后续闭环。 */
export function alreadyActivated(memberId, idempotencyKey) {
  const state = readState(memberId)
  return state.activations.find(a => a.idempotencyKey === idempotencyKey && a.outcome === 'verified') || null
}

export function pendingActivation(memberId, idempotencyKey) {
  const state = readState(memberId)
  return [...state.activations].reverse().find(a =>
    a.idempotencyKey === idempotencyKey && a.outcome === 'accepted_pending_verify') || null
}

/** 将最近一条 pending 原位结算，避免把同一次恢复重复计数。 */
export function finalizeActivation(memberId, idempotencyKey, outcome, details = {}) {
  const state = readState(memberId)
  const index = state.activations.findLastIndex(a =>
    a.idempotencyKey === idempotencyKey && a.outcome === 'accepted_pending_verify')
  if (index < 0) return null
  state.activations[index] = {
    ...state.activations[index],
    ...details,
    outcome,
    finalizedAt: new Date().toISOString()
  }
  writeState(memberId, state)
  return state.activations[index]
}

/** 读取网关 token（复用既有入口 ~/.openclaw/openclaw.json；只读取、绝不打印）。 */
export function readGatewayToken() {
  const cfgPath = join(homedir(), '.openclaw', 'openclaw.json')
  const cfg = JSON.parse(readFileSync(cfgPath, 'utf8'))
  const token = cfg?.gateway?.auth?.token
  if (!token || typeof token !== 'string') throw new Error('gateway token unavailable')
  return token
}
