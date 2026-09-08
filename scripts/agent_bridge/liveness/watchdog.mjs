// TR-COLLAB-LIVENESS-17 常驻看门狗：单实例、每 60 秒只读探测。
// 正常（delivered/healthy_idle/forbidden/quota）只记日志不调 LLM、不发消息、不建窗口；
// 仅对 offline / accepted_no_readback 且满足限流时触发一次激活；
// 激活后进入验证冷却，回读不到真实活动则计失败，连续失败/超限写 blocked 交 Codex，禁止无限循环。
import { existsSync, mkdirSync, writeFileSync, readFileSync, renameSync, linkSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { MEMBERS, EVIDENCE_DIR, readGatewayToken, recoveryAttemptCount, pendingActivation, finalizeActivation, markBlocked, logEvent } from './state.mjs'
import { probeMember } from './probe.mjs'
import { activateMember, verifyActivity } from './activate.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const INTERVAL_MS = Number(process.env.LIVENESS_INTERVAL_MS || 60000)
// 激活后给对方干活的时间，再回读验证
const VERIFY_AFTER_MS = Number(process.env.LIVENESS_VERIFY_AFTER_MS || 180000)
const MAX_FAILED_ACTIVATIONS = 2

export function processAlive(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return false
  try { process.kill(pid, 0); return true } catch (e) { return e?.code !== 'ESRCH' }
}

/** 完整候选经 hard-link 原子发布；陈旧锁受 guard 保护回收，损坏锁安全拒绝。 */
export function acquireLock({ lockDir = EVIDENCE_DIR, pid = process.pid, isAlive = processAlive, now = Date.now, onBeforeReclaim } = {}) {
  mkdirSync(lockDir, { recursive: true })
  const lockPath = join(lockDir, 'watchdog.lock')

  const readLock = () => {
    try {
      const lock = JSON.parse(readFileSync(lockPath, 'utf8'))
      if (!Number.isInteger(Number(lock?.pid)) || Number(lock.pid) <= 0) throw new Error('invalid pid')
      return lock
    } catch {
      throw new Error('watchdog 锁状态不确定（空白、半写或损坏），安全拒绝自动接管')
    }
  }

  const publishCompleteLock = () => {
    const nonce = `${pid}-${now()}-${Math.random().toString(16).slice(2)}`
    const candidate = join(lockDir, `watchdog.lock.candidate-${nonce}`)
    writeFileSync(candidate, JSON.stringify({ pid, startedAt: new Date(now()).toISOString() }), { encoding: 'utf8', mode: 0o600, flag: 'wx' })
    try {
      // hard-link 发布是原子的；watchdog.lock 从不会暴露为空白或半写状态。
      linkSync(candidate, lockPath)
      renameSync(candidate, join(lockDir, `watchdog.lock.owner-${nonce}`))
      return true
    } catch (e) {
      renameSync(candidate, join(lockDir, `watchdog.lock.contender-${nonce}`))
      if (e?.code === 'EEXIST') return false
      throw e
    }
  }

  for (let attempt = 0; attempt < 4; attempt += 1) {
    if (!existsSync(lockPath) && publishCompleteLock()) return lockPath
    const lock = readLock()
    if (lock?.pid && isAlive(Number(lock.pid))) {
      throw new Error(`watchdog 已有实例运行 pid=${lock.pid}，拒绝重复启动`)
    }

    // 多回收者先竞争独立 guard；持有者在 guard 内重新读取，不能移动后来者的新锁。
    const guardPath = join(lockDir, 'watchdog.reclaim')
    try { mkdirSync(guardPath) } catch (e) {
      if (e?.code === 'EEXIST') throw new Error('watchdog 陈旧锁正在被其他进程回收，安全拒绝并发接管')
      throw e
    }
    try {
      onBeforeReclaim?.()
      const current = readLock()
      if (isAlive(Number(current.pid))) {
        throw new Error(`watchdog 已有实例运行 pid=${current.pid}，拒绝回收`)
      }
      renameSync(lockPath, `${lockPath}.stale-${now()}-${current.pid}`)
      if (publishCompleteLock()) return lockPath
    } finally {
      renameSync(guardPath, `${guardPath}.done-${now()}-${pid}-${attempt}`)
    }
  }
  throw new Error('watchdog 锁竞争超过上限，拒绝启动')
}

/**
 * 单轮检查。probe → 分类 → 必要时激活 → 冷却后验证。
 * cliRunner 可注入（夹具测试用）。返回脱敏结果。
 */
export async function checkOnce(member, { token, cliRunner, tcpRunner, now = Date.now() } = {}) {
  const probe = await probeMember(member, { token, cliRunner, tcpRunner, now })
  const resumeKey = member.resumeIdempotencyKey || member.idempotencyKey
  const pending = pendingActivation(member.id, resumeKey)
  if (pending) return { member: member.id, action: 'verify_pending', prior: pending }

  // 明确不健康但不可恢复的状态：不动作
  if (probe.status === 'forbidden' || probe.status === 'quota_exhausted') {
    return { member: member.id, action: 'none', reason: probe.status }
  }
  // 健康/已送达：不动作（正常检查不调 LLM）
  if (probe.status === 'delivered' || probe.status === 'healthy_idle') {
    return { member: member.id, action: 'none', reason: probe.status }
  }
  // unknown 可能只是瞬时 CLI/解析异常，不发送消息；只记录并等待下一轮。
  if (probe.status === 'unknown') {
    return { member: member.id, action: 'none', reason: 'unknown' }
  }
  // 仅 offline / accepted_no_readback 尝试激活（内部幂等+限流）。
  const activation = await activateMember(member, {
    token, cliRunner, now,
    baselineCount: probe.messageCount,
    baselineAt: probe.lastActivityAt
  })
  if (['blocked_forbidden', 'blocked_quota', 'blocked_rate_limited', 'blocked_existing'].includes(activation.outcome)) {
    return { member: member.id, action: 'blocked', reason: activation.outcome }
  }
  if (activation.outcome === 'already_activated') {
    return { member: member.id, action: 'none', reason: 'already_activated' }
  }
  if (activation.outcome === 'pending_verify') {
    return { member: member.id, action: 'verify_pending', prior: activation.prior }
  }
  if (activation.outcome !== 'accepted_pending_verify') {
    return { member: member.id, action: 'none', reason: activation.outcome }
  }
  return { member: member.id, action: 'activated', outcome: activation.outcome, pending: activation, verifyAfterMs: VERIFY_AFTER_MS }
}

/** 激活冷却后的活动复核；连续无真实活动累计失败，达上限写 blocked。 */
export async function verifyOnce(member, { token, cliRunner, baselineCount, baselineAt, idempotencyKey, now = Date.now() } = {}) {
  const v = await verifyActivity(member, { token, cliRunner, baselineCount, baselineAt })
  const resumeKey = idempotencyKey || member.resumeIdempotencyKey || member.idempotencyKey
  if (!v.verified) {
    finalizeActivation(member.id, resumeKey, 'verify_failed', { verifyReason: v.reason || 'no_activity' })
    const recentAttempts = recoveryAttemptCount(member.id, now)
    if (recentAttempts >= MAX_FAILED_ACTIVATIONS) {
      markBlocked(member.id, `30 分钟内 ${recentAttempts} 次恢复均无真实活动（只 accepted 不算激活），停止重试交 Codex`)
    }
  } else {
    finalizeActivation(member.id, resumeKey, 'verified', {
      messageCountAfter: v.messageCount,
      lastActivityAtAfter: v.lastActivityAt
    })
    logEvent({ member: member.id, kind: 'activated_verified', messageCount: v.messageCount, grew: v.grew, advanced: v.advanced })
  }
  return v
}

/** 可注入调度器的真实主循环；夹具从这里验证 accepted→冷却→回读闭环。 */
export function createWatchdogRunner({
  token,
  members = [MEMBERS.dilong, MEMBERS.tianlong],
  cliRunner,
  tcpRunner,
  schedule = setTimeout,
  verifyAfterMs = VERIFY_AFTER_MS,
  now = Date.now
} = {}) {
  const verificationTimers = new Map()
  let running = null

  const scheduleVerification = (member, prior) => {
    if (!prior || verificationTimers.has(member.id)) return false
    const timer = schedule(async () => {
      try {
        await verifyOnce(member, {
          token, cliRunner,
          baselineCount: prior.messageCountBefore,
          baselineAt: prior.lastActivityAt,
          idempotencyKey: prior.idempotencyKey,
          now: now()
        })
      } catch (e) {
        finalizeActivation(member.id, prior.idempotencyKey, 'verify_failed', { verifyReason: 'verify_exception' })
        const attempts = recoveryAttemptCount(member.id, now())
        logEvent({ member: member.id, kind: 'verify_error', error: String(e?.message || e).slice(0, 200), attempts })
        if (attempts >= MAX_FAILED_ACTIVATIONS) {
          markBlocked(member.id, `30 分钟内 ${attempts} 次恢复验证失败，停止重试交 Codex`)
        }
      } finally {
        verificationTimers.delete(member.id)
      }
    }, verifyAfterMs)
    verificationTimers.set(member.id, timer)
    return true
  }

  const runCycle = async () => {
    const results = []
    for (const member of members) {
      try {
        const result = await checkOnce(member, { token, cliRunner, tcpRunner, now: now() })
        const prior = result.action === 'activated' ? result.pending : result.prior
        if (result.action === 'activated' || result.action === 'verify_pending') {
          scheduleVerification(member, prior)
        }
        results.push(result)
      } catch (e) {
        logEvent({ member: member.id, kind: 'check_error', error: String(e?.message || e).slice(0, 200) })
        results.push({ member: member.id, action: 'none', reason: 'check_error' })
      }
    }
    return results
  }

  return {
    verificationTimers,
    tick() {
      if (running) return running
      running = runCycle().finally(() => { running = null })
      return running
    }
  }
}

async function main() {
  const lockPath = acquireLock()
  const token = readGatewayToken()
  logEvent({ kind: 'watchdog_started', pid: process.pid, intervalMs: INTERVAL_MS })
  console.log(`[liveness] watchdog started pid=${process.pid} interval=${INTERVAL_MS}ms lock=${lockPath}`)
  const runner = createWatchdogRunner({ token })
  const tick = async () => {
    try {
      const results = await runner.tick()
      for (const r of results) console.log(`[liveness] ${r.member}`, JSON.stringify(r))
    } catch (e) {
      logEvent({ kind: 'check_error', error: String(e?.message || e).slice(0, 200) })
    }
  }
  await tick()
  setInterval(tick, INTERVAL_MS)
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(e => { console.error('[liveness] fatal:', e?.message || e); process.exit(1) })
}
