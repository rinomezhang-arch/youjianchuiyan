// TR-COLLAB-LIVENESS-17 原会话激活：仅对"确认假死/已收指令无活动"的成员，
// 用固定幂等键向**原会话**投递一次带固定任务ID的续做指令，随后只读回读验证真实活动。
//
// 硬约束：
//   - 同一幂等键已成功投递过绝不重发（幂等去重）
//   - 每成员 30 分钟最多 2 次恢复尝试（state.recoveryAllowed），超限转 blocked
//   - FORBIDDEN/额度耗尽 不靠重启或换身份绕过，直接 blocked
//   - 只 accepted / 端口通 不算激活；必须回读到带标识回复或真实工具活动/任务状态变化
import { fileURLToPath } from 'node:url'
import { MEMBERS, readGatewayToken, recoveryAllowed, recordActivation, alreadyActivated, pendingActivation, readState, markBlocked, logEvent } from './state.mjs'
import { gatewayCall } from './probe.mjs'
import { inspectMember } from './inspect-session.mjs'

// 恢复后投递的**实际工作**（不发空泛问候）。内容来自秋哥/Codex 板上反例。
export const RESUME_INSTRUCTIONS = {
  dilong: `[CX-DL-AUTH16-0908-01][RESUME] 续做 DL-IPAD-BATCH-AUTH-16（已 changes_requested）。两项反例：1) e5f7c84e 只导入 IpadBatchSubmissionService 但未调用，client_request_id 未落入 ipad_batch_request，新授权同请求可能重复加菜——接入现有 normalize/submit 链路；2) 成功测试仅 mock details，未证明菜品落库——补两表（ipad_batch_request / 明细）真实回读断言。保留既有 12 项授权测试。完成后在任务板 reported：新 SHA、真实测试数字、报告路径。只动原 allowed_paths 与你本人隔离库，不碰他人工作树与生产。`,
  tianlong: `[CX-TL-DATAMAP14-0908-01][RESUME] 续做 TL-RELEASE-DATA-MAP-14（最后 started 20:27）。请回读你本人工作树/任务板当前状态：若已完成请给出新 SHA、真实数字、reported 路径；若卡住请给出具体阻断。不要重启无关进程。`
}

export async function activateMember(member, { token, cliRunner, now = Date.now(), verifyTimeoutMs = 90000, baselineCount = null, baselineAt = null } = {}) {
  // 续做指令使用独立固定幂等键（resume-r1）；与原任务投递区分，重跑不重复
  const resumeKey = member.resumeIdempotencyKey || member.idempotencyKey
  // 1) 幂等：同一续做键已成功投递过直接返回既有结果
  const prior = alreadyActivated(member.id, resumeKey)
  if (prior) {
    return { member: member.id, outcome: 'already_activated', prior }
  }
  const pending = pendingActivation(member.id, resumeKey)
  if (pending) {
    return { member: member.id, outcome: 'pending_verify', prior: pending }
  }
  const state = readState(member.id)
  if (state.blocked) {
    return { member: member.id, outcome: 'blocked_existing', reason: state.blocked.reason }
  }
  // 2) 限流：30 分钟窗口最多 2 次
  if (!recoveryAllowed(member.id, now)) {
    markBlocked(member.id, 'recovery_rate_limited: 30min 内恢复尝试已达 2 次上限，停止重试，通知 Codex')
    return { member: member.id, outcome: 'blocked_rate_limited' }
  }

  const message = RESUME_INSTRUCTIONS[member.id]
  if (!message) return { member: member.id, outcome: 'no_instruction' }

  // 3) 投递（官方 gateway call agent；禁止换身份/放权限）
  const send = await gatewayCall({
    url: member.url, token, method: 'agent',
    params: { sessionKey: member.sessionKey, message, idempotencyKey: resumeKey },
    timeoutMs: verifyTimeoutMs, cliRunner
  })

  const blob = `${send?.err || ''} ${send?.out || ''}`.toLowerCase()
  if (!send?.ok) {
    if (/forbidden|403|unauthorized|401/.test(blob)) {
      markBlocked(member.id, 'gateway FORBIDDEN：写通道被拒，按约束不换身份/不放权限，交 Codex 处理权限')
      recordActivation(member.id, { idempotencyKey: resumeKey, outcome: 'forbidden' })
      return { member: member.id, outcome: 'blocked_forbidden' }
    }
    if (/quota|insufficient|balance|额度/.test(blob)) {
      markBlocked(member.id, '模型额度耗尽：不靠重启/换身份绕过，交 Codex 处理')
      recordActivation(member.id, { idempotencyKey: resumeKey, outcome: 'quota_exhausted' })
      return { member: member.id, outcome: 'blocked_quota' }
    }
    recordActivation(member.id, { idempotencyKey: resumeKey, outcome: 'send_failed', errorKind: send.kind || 'cli_error' })
    return { member: member.id, outcome: 'send_failed', errorKind: send.kind || 'cli_error' }
  }

  // 4) accepted 只登记 pending；真实活动由 watchdog 冷却后回读结算。
  const record = {
    idempotencyKey: resumeKey,
    outcome: 'accepted_pending_verify',
    messageCountBefore: baselineCount,
    lastActivityAt: baselineAt
  }
  recordActivation(member.id, record)
  logEvent({ member: member.id, kind: 'activate', outcome: record.outcome })
  return { member: member.id, ...record }
}

/** 激活后复核：回读是否出现新活动（消息数增长 / 最近活动时间推进 / 新标识回复）。 */
export async function verifyActivity(member, { token, cliRunner, baselineCount, baselineAt } = {}) {
  const inspected = await inspectMember(member, { token, cliRunner })
  if (!inspected.readable) return { member: member.id, verified: false, reason: inspected.reason }
  return {
    member: member.id,
    verified: inspected.realActivity,
    markerFound: inspected.markerFound,
    messageCount: inspected.totalReturned,
    lastActivityAt: inspected.lastActivityAt,
    grew: baselineCount != null && inspected.totalReturned > baselineCount,
    advanced: baselineAt && inspected.lastActivityAt && String(inspected.lastActivityAt) > String(baselineAt),
    assistantEntriesAfter: inspected.assistantEntriesAfter,
    toolLikeEntriesAfter: inspected.toolLikeEntriesAfter
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const token = readGatewayToken()
  const only = process.argv[2] // 可选：dilong | tianlong
  const targets = only ? [MEMBERS[only]].filter(Boolean) : [MEMBERS.dilong, MEMBERS.tianlong]
  const out = []
  for (const m of targets) out.push(await activateMember(m, { token }))
  console.log(JSON.stringify({ at: new Date().toISOString(), out }, null, 2))
}
