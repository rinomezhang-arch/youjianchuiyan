// TR-COLLAB-LIVENESS-17 常驻看门狗：单实例、每 60 秒只读探测。
// 正常（delivered/healthy_idle/forbidden/quota）只记日志不调 LLM、不发消息、不建窗口；
// 仅对 offline / accepted_no_readback 且满足限流时触发一次激活；
// 激活后进入验证冷却，回读不到真实活动则计失败，连续失败/超限写 blocked 交 Codex，禁止无限循环。
import { existsSync, mkdirSync, writeFileSync, readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { MEMBERS, EVIDENCE_DIR, readGatewayToken, readState, logEvent } from './state.mjs'
import { probeMember } from './probe.mjs'
import { activateMember, verifyActivity } from './activate.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const INTERVAL_MS = Number(process.env.LIVENESS_INTERVAL_MS || 60000)
// 激活后给对方干活的时间，再回读验证
const VERIFY_AFTER_MS = Number(process.env.LIVENESS_VERIFY_AFTER_MS || 180000)
const MAX_FAILED_ACTIVATIONS = 2

function acquireLock() {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  const lockPath = join(EVIDENCE_DIR, 'watchdog.lock')
  if (existsSync(lockPath)) {
    try {
      const lock = JSON.parse(require_fs(lockPath))
      if (lock.pid && process.pid !== lock.pid) {
        // 不杀既有进程：单实例约束靠锁文件 + 存活探测
        throw new Error(`watchdog 已有实例运行 pid=${lock.pid}，拒绝重复启动`)
      }
    } catch (e) {
      if (String(e.message).includes('已有实例')) throw e
    }
  }
  writeFileSync(lockPath, JSON.stringify({ pid: process.pid, startedAt: new Date().toISOString() }), { encoding: 'utf8', mode: 0o600 })
  return lockPath
}

function require_fs(p) {
  // 极简读锁（避免再引依赖）
  return import_fs_read(p)
}
import { readFileSync as import_fs_read } from 'node:fs'

/**
 * 单轮检查。probe → 分类 → 必要时激活 → 冷却后验证。
 * cliRunner 可注入（夹具测试用）。返回脱敏结果。
 */
export async function checkOnce(member, { token, cliRunner, tcpRunner, now = Date.now() } = {}) {
  const probe = await probeMember(member, { token, cliRunner, tcpRunner, now })

  // 明确不健康但不可恢复的状态：不动作
  if (probe.status === 'forbidden' || probe.status === 'quota_exhausted') {
    return { member: member.id, action: 'none', reason: probe.status }
  }
  // 健康/已送达：不动作（正常检查不调 LLM）
  if (probe.status === 'delivered' || probe.status === 'healthy_idle') {
    return { member: member.id, action: 'none', reason: probe.status }
  }
  // offline / accepted_no_readback / unknown：尝试激活（内部幂等+限流）
  const activation = await activateMember(member, { token, cliRunner, now })
  if (activation.outcome === 'blocked_forbidden' || activation.outcome === 'blocked_quota' || activation.outcome === 'blocked_rate_limited') {
    return { member: member.id, action: 'blocked', reason: activation.outcome }
  }
  if (activation.outcome === 'already_activated') {
    return { member: member.id, action: 'verify_pending', reason: 'already_activated' }
  }
  return { member: member.id, action: 'activated', outcome: activation.outcome, verifyAfterMs: VERIFY_AFTER_MS }
}

/** 激活冷却后的活动复核；连续无真实活动累计失败，达上限写 blocked。 */
export async function verifyOnce(member, { token, cliRunner, baselineCount, baselineAt, now = Date.now() } = {}) {
  const v = await verifyActivity(member, { token, cliRunner, baselineCount, baselineAt })
  if (!v.verified) {
    const state = readState(member.id)
    const failed = (state.activations.filter(a => a.outcome === 'accepted_pending_verify').length)
    if (failed >= MAX_FAILED_ACTIVATIONS) {
      const { markBlocked } = await import('./state.mjs')
      markBlocked(member.id, `激活后 ${failed} 次回读无真实活动（只 accepted 不算激活），停止重试交 Codex`)
    }
  } else {
    logEvent({ member: member.id, kind: 'activated_verified', messageCount: v.messageCount, grew: v.grew, advanced: v.advanced })
  }
  return v
}

async function main() {
  const lockPath = acquireLock()
  const token = readGatewayToken()
  logEvent({ kind: 'watchdog_started', pid: process.pid, intervalMs: INTERVAL_MS })
  console.log(`[liveness] watchdog started pid=${process.pid} interval=${INTERVAL_MS}ms lock=${lockPath}`)
  const tick = async () => {
    for (const m of [MEMBERS.dilong, MEMBERS.tianlong]) {
      try {
        const r = await checkOnce(m, { token })
        console.log(`[liveness] ${m.id}`, JSON.stringify(r))
      } catch (e) {
        logEvent({ member: m.id, kind: 'check_error', error: String(e?.message || e).slice(0, 200) })
      }
    }
  }
  await tick()
  setInterval(tick, INTERVAL_MS)
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(e => { console.error('[liveness] fatal:', e?.message || e); process.exit(1) })
}
