// 只读会话尾部检查：固定标识是否在会话中、标识**之后**有没有真实活动。
// 脱敏输出：只有角色/类型/时间/计数与标记命中，绝不输出会话正文。
import { fileURLToPath } from 'node:url'
import { MEMBERS, readGatewayToken, logEvent } from './state.mjs'
import { gatewayCall } from './probe.mjs'

function normalizeMessages(out) {
  const raw = String(out || '')
  const start = raw.search(/[[{]/)
  if (start < 0) return []
  try {
    const parsed = JSON.parse(raw.slice(start))
    const msgs = Array.isArray(parsed) ? parsed : (parsed?.messages || parsed?.data?.messages || parsed?.sessions || [])
    return Array.isArray(msgs) ? msgs : []
  } catch {
    return []
  }
}

function tsOf(m) {
  const v = m?.createdAt || m?.timestamp || m?.at || m?.time || m?.created_at
  const n = Number(v)
  return Number.isFinite(n) ? n : Date.parse(String(v)) || 0
}

function roleOf(m) {
  return String(m?.role || m?.type || m?.author || m?.sender || m?.kind || 'unknown')
}

export async function inspectMember(member, { token, cliRunner } = {}) {
  const call = await gatewayCall({
    url: member.url, token, method: 'sessions.get',
    params: { sessionKey: member.sessionKey }, timeoutMs: 30000, cliRunner
  })
  if (!call?.ok) {
    const blob = `${call?.err || ''} ${call?.out || ''}`.toLowerCase()
    return { member: member.id, readable: false, reason: /forbidden|403/.test(blob) ? 'forbidden' : (call?.kind || 'cli_error') }
  }
  const msgs = normalizeMessages(call.out)
  // 恢复验真只认本轮 RESUME 标识，避免把更早的原任务消息当作恢复指令。
  const resumeMarker = member.resumeMarker || `[${member.marker}][RESUME]`
  // 定位最后一条命中恢复标识的消息
  let markerIdx = -1
  for (let i = msgs.length - 1; i >= 0; i--) {
    const text = JSON.stringify(msgs[i] ?? {})
    if (text.includes(resumeMarker) && /user|human/i.test(roleOf(msgs[i]))) { markerIdx = i; break }
  }
  const after = markerIdx >= 0 ? msgs.slice(markerIdx + 1) : []
  const afterRoles = after.map(roleOf)
  const roleCounts = afterRoles.reduce((acc, r) => { acc[r] = (acc[r] || 0) + 1; return acc }, {})
  // 工具活动证据：工具调用/执行记录条目（不解析内容，只看类型与数量）
  const toolish = after.filter(m => /tool|command|exec|tool_use|tool_result|process|shell/i.test(JSON.stringify(m).slice(0, 400))).length
  const assistantish = after.filter(m => /assistant|agent|bot/i.test(roleOf(m))).length
  const markerTs = markerIdx >= 0 ? tsOf(msgs[markerIdx]) : 0
  const lastTs = msgs.length ? Math.max(...msgs.map(tsOf)) : 0

  return {
    member: member.id,
    readable: true,
    totalReturned: msgs.length,
    markerFound: markerIdx >= 0,
    markerAt: markerTs || null,
    messagesAfterMarker: after.length,
    afterRoleCounts: roleCounts,
    toolLikeEntriesAfter: toolish,
    assistantEntriesAfter: assistantish,
    lastActivityAt: lastTs || null,
    // 判定：恢复标识之后有助手回复或工具活动才算真实响应；用户消息不计。
    realActivity: assistantish > 0 || toolish > 0
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const token = readGatewayToken()
  const out = []
  for (const m of [MEMBERS.dilong, MEMBERS.tianlong]) {
    const r = await inspectMember(m, { token })
    out.push(r)
    logEvent({ member: m.id, kind: 'inspect', markerFound: r.markerFound, after: r.messagesAfterMarker, realActivity: r.realActivity })
  }
  console.log(JSON.stringify({ at: new Date().toISOString(), out }, null, 2))
}
