// TR-COLLAB-LIVENESS-17 只读探测：TCP 可达 + 官方 sessions.get 回读固定标识。
// 正常探测不调用 LLM、不发消息、不改任何状态；输出分类结果并落脱敏日志。
//
// 分类（classify）：
//   offline        网关端口不可达 / 握手失败
//   forbidden      网关明确 403/FORBIDDEN（权限问题，禁止靠重启或换身份绕过）
//   quota_exhausted 模型额度耗尽（明确额度错误，禁止靠重启绕过）
//   delivered      sessions.get 回读到固定消息标识（指令确已进原会话）
//   accepted_no_readback  agent 曾 accepted 但标识回读不到 / 订阅超时
//   healthy_idle   网关健康但会话近期无活动（空闲）
//   unknown        其余不确定情况
import { spawn } from 'node:child_process'
import net from 'node:net'
import { fileURLToPath } from 'node:url'
import { MEMBERS, readGatewayToken, recordProbe, logEvent } from './state.mjs'
import { prepareClientConfig, openclawCliPath } from './client-config.mjs'

export function tcpCheck(url, timeoutMs = 5000) {
  return new Promise((resolve) => {
    const u = new URL(url)
    const socket = net.connect({ host: u.hostname, port: Number(u.port || 80) })
    let done = false
    const finish = (ok, err) => { if (done) return; done = true; socket.destroy(); resolve({ ok, err }) }
    socket.setTimeout(timeoutMs)
    socket.once('connect', () => finish(true))
    socket.once('timeout', () => finish(false, 'tcp_timeout'))
    socket.once('error', (e) => finish(false, String(e?.code || e?.message || e)))
  })
}

/** 调用官方 openclaw gateway call；cliRunner 可注入（夹具测试用假 CLI）。 */
export function gatewayCall({ url, token, method, params, timeoutMs = 30000, cliRunner = defaultCliRunner }) {
  return cliRunner({ url, token, method, params, timeoutMs })
}

function defaultCliRunner({ url, token, method, params, timeoutMs }) {
  return new Promise((resolve) => {
    const configPath = prepareClientConfig({ url })
    const cliPath = openclawCliPath()
    const launch = buildCliInvocation({ cliPath, configPath, url, token, method, params, timeoutMs })
    // node 直调 openclaw.mjs：不走 shell，避免 JSON 参数被 cmd 引号拆毁；
    // OPENCLAW_CONFIG_PATH 指向客户端侧净化副本（仅删未知键，token/身份不变）。
    const child = spawn(process.execPath, launch.args, {
      windowsHide: true,
      shell: false,
      env: launch.env
    })
    let out = '', err = ''
    let settled = false
    const finish = (r) => { if (settled) return; settled = true; resolve(r) }
    const timer = setTimeout(() => finish({ ok: false, kind: 'timeout', err: `cli_timeout_${timeoutMs}ms`, out }), timeoutMs + 15000)
    child.stdout.on('data', c => { out += c })
    child.stderr.on('data', c => { err += c })
    child.once('error', e => { clearTimeout(timer); finish({ ok: false, kind: 'spawn_error', err: String(e?.message || e), out }) })
    child.once('exit', code => {
      clearTimeout(timer)
      finish({ ok: code === 0, code, out, err })
    })
  })
}

/** 纯函数，供夹具证明 token 不进入 argv，只走 OpenClaw 官方环境变量入口。 */
export function buildCliInvocation({ cliPath, configPath, url, token, method, params, timeoutMs, parentEnv = process.env }) {
  return {
    args: [cliPath, 'gateway', 'call', method,
      '--params', JSON.stringify(params), '--timeout', String(timeoutMs), '--json'],
    env: { ...parentEnv, OPENCLAW_CONFIG_PATH: configPath, OPENCLAW_GATEWAY_TOKEN: token }
  }
}

/** 从 sessions.get 输出中提取脱敏证据：标识是否命中、消息计数、最近活动时间。 */
export function extractSessionEvidence(out, marker) {
  const raw = String(out || '')
  const evidence = { markerFound: raw.includes(marker), messageCount: null, lastActivityAt: null, parseOk: false }
  try {
    // CLI 可能输出多行，找第一个 JSON 对象/数组
    const start = raw.search(/[[{]/)
    if (start >= 0) {
      const parsed = JSON.parse(raw.slice(start))
      evidence.parseOk = true
      const msgs = Array.isArray(parsed) ? parsed : (parsed?.messages || parsed?.data?.messages || parsed?.sessions || [])
      if (Array.isArray(msgs)) {
        evidence.messageCount = msgs.length
        const times = msgs.map(m => m?.createdAt || m?.timestamp || m?.at || m?.time).filter(Boolean).sort()
        if (times.length) evidence.lastActivityAt = times[times.length - 1]
      }
    }
  } catch {
    // 解析失败不致命：markerFound 的字符串匹配仍然有效
  }
  return evidence
}

export function classify({ tcp, call, markerFound }) {
  if (!tcp.ok) return 'offline'
  if (call?.kind === 'spawn_error') return 'unknown'
  // 权限/额度只从本次失败调用判断；成功回读正文可能合法包含这些词。
  if (!call?.ok) {
    const blob = `${call?.err || ''} ${call?.out || ''}`.toLowerCase()
    if (/forbidden|403|unauthorized|401/.test(blob)) return 'forbidden'
    if (/quota|insufficient|balance|额度|余额不足/.test(blob)) return 'quota_exhausted'
    if (call?.kind === 'timeout') return 'accepted_no_readback'
    return 'unknown'
  }
  if (markerFound) return 'delivered'
  return 'healthy_idle'
}

export async function probeMember(member, { token, cliRunner, tcpRunner = tcpCheck, timeoutMs = 30000, now = Date.now() } = {}) {
  const tcp = await tcpRunner(member.url)
  let call = null
  let evidence = { markerFound: false, messageCount: null, lastActivityAt: null, parseOk: false }
  if (tcp.ok) {
    call = await gatewayCall({
      url: member.url, token, method: 'sessions.get',
      params: { sessionKey: member.sessionKey }, timeoutMs, cliRunner
    })
    evidence = extractSessionEvidence(call.out, member.marker)
  }
  const status = classify({ tcp, call, markerFound: evidence.markerFound })
  const result = {
    member: member.id, status,
    tcpOk: tcp.ok,
    cliOk: !!call?.ok,
    // 脱敏：只留布尔/计数/时间，不带任何会话正文
    markerFound: evidence.markerFound,
    messageCount: evidence.messageCount,
    lastActivityAt: evidence.lastActivityAt,
    errorKind: call?.kind || (tcp.ok ? null : 'tcp_unreachable')
  }
  recordProbe(member.id, result)
  logEvent({ member: member.id, kind: 'probe', status, markerFound: evidence.markerFound, messageCount: evidence.messageCount })
  return result
}

// 直接运行：探测两成员并打印脱敏 JSON 汇总。
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const token = readGatewayToken()
  const results = []
  for (const m of [MEMBERS.dilong, MEMBERS.tianlong]) {
    const r = await probeMember(m, { token })
    results.push(r)
  }
  console.log(JSON.stringify({ at: new Date().toISOString(), results }, null, 2))
}
