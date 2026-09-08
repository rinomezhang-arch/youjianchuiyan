// TR-COLLAB-LIVENESS-17 夹具测试：三场景必须在**不停在线成员、不碰真实网关**前提下验证。
// 用注入的假 cliRunner/假 TCP 模拟：离线、启动后未响应、真实忙碌不误杀；
// 外加 FORBIDDEN 不绕过、幂等去重、30 分钟 2 次限流。
// 运行：node --test scripts/agent_bridge/liveness/liveness.test.mjs
import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

// 必须在导入状态模块前固定证据目录（模块导入时读取 env）
const tmp = mkdtempSync(join(tmpdir(), 'liveness-fixture-'))
process.env.LIVENESS_EVIDENCE_DIR = tmp

const { MEMBERS, readState, writeState } = await import('./state.mjs')
const { probeMember, classify } = await import('./probe.mjs')
const { activateMember, verifyActivity } = await import('./activate.mjs')
const { checkOnce, verifyOnce } = await import('./watchdog.mjs')

const TOKEN = 'fixture-token-not-a-real-secret'
const member = { ...MEMBERS.dilong }

function fakeRunner(handlers) {
  const calls = []
  return {
    calls,
    runner: async ({ method, params }) => {
      calls.push({ method, params })
      const h = handlers[method]
      return h ? h(params) : { ok: true, out: '{}' }
    }
  }
}

test.beforeEach(() => {
  // 每个用例从干净状态开始
  for (const id of Object.keys(MEMBERS)) writeState(id, { probes: [], activations: [], blocked: null })
})

test.after(() => rmSync(tmp, { recursive: true, force: true }))

test('场景一 离线：TCP 不可达 → offline 分类，agent 投递失败记为 send_failed（不杀任何进程）', async () => {
  const { runner, calls } = fakeRunner({
    agent: async () => ({ ok: false, kind: 'timeout', err: 'cli_timeout', out: '' })
  })
  const tcpRunner = async () => ({ ok: false, err: 'ECONNREFUSED' })

  const probe = await probeMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(probe.status, 'offline')

  const res = await checkOnce(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(res.action, 'activated')
  const state = readState(member.id)
  assert.equal(state.activations.at(-1).outcome, 'send_failed')
  // 离线场景下 sessions.get 不应被调用（端口不通没有任何会话可查）
  assert.equal(calls.filter(c => c.method === 'sessions.get').length, 0)
})

test('场景二 启动后未响应：端口通但 sessions.get 订阅超时（同态于真实 30000ms 超时）→ accepted 无回读；两次验证无真实活动后 blocked，不无限重试', async () => {
  let agentCalls = 0
  const { runner } = fakeRunner({
    'sessions.get': async () => ({ ok: false, kind: 'timeout', err: 'cli_timeout_30000ms', out: '' }),
    agent: async () => { agentCalls += 1; return { ok: true, out: JSON.stringify({ accepted: true, runId: 'r' + agentCalls }) } }
  })
  const tcpRunner = async () => ({ ok: true })

  // 第一轮：读超时触发激活，agent accepted 但回读不到
  const probe = await probeMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(probe.status, 'accepted_no_readback')
  const r1 = await checkOnce(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(r1.action, 'activated')
  const v1 = await verifyOnce(member, { token: TOKEN, cliRunner: runner, baselineCount: 0 })
  assert.equal(v1.verified, false, '只 accepted 不算激活')

  // 第二个恢复周期仍无真实活动 → 达上限 blocked
  const st = readState(member.id)
  st.activations.push({ at: new Date().toISOString(), idempotencyKey: 'other', outcome: 'accepted_pending_verify' })
  writeState(member.id, st)
  const v2 = await verifyOnce(member, { token: TOKEN, cliRunner: runner, baselineCount: 0 })
  assert.equal(v2.verified, false)

  const after = readState(member.id)
  assert.ok(after.blocked, '两次激活无真实活动后必须 blocked 交 Codex，禁止无限循环')
  assert.match(after.blocked.reason, /无真实活动/)
})

test('场景三 真实忙碌不误杀：sessions.get 回读到固定标识 → delivered，checkOnce 不发 agent 消息', async () => {
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({
      ok: true,
      out: JSON.stringify({ messages: [
        { id: 1, content: '前期工作', createdAt: '2026-09-08T12:00:00Z' },
        { id: 2, content: member.marker + ' 任务正在处理中', createdAt: '2026-09-08T12:05:00Z' }
      ] })
    }),
    agent: async () => { throw new Error('忙碌成员绝不应被调用 agent') }
  })
  const tcpRunner = async () => ({ ok: true })

  const probe = await probeMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(probe.status, 'delivered')
  assert.equal(probe.markerFound, true)
  assert.equal(probe.messageCount, 2)

  const res = await checkOnce(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(res.action, 'none')
  assert.equal(calls.filter(c => c.method === 'agent').length, 0, '忙碌/已送达成员不得被打扰')
})

test('FORBIDDEN：读超时触发激活、写通道被拒 → blocked_forbidden，不换身份不放权限，不重试 agent', async () => {
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({ ok: false, kind: 'timeout', err: 'cli_timeout_30000ms', out: '' }),
    agent: async () => ({ ok: false, kind: 'http_error', out: '{"error":"FORBIDDEN"}', err: '403 FORBIDDEN' })
  })
  const tcpRunner = async () => ({ ok: true })

  const probe = await probeMember(MEMBERS.tianlong, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(probe.status, 'accepted_no_readback')
  const res = await checkOnce(MEMBERS.tianlong, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(res.action, 'blocked')
  assert.equal(res.reason, 'blocked_forbidden')
  const st = readState(MEMBERS.tianlong.id)
  assert.ok(st.blocked)
  assert.match(st.blocked.reason, /FORBIDDEN/)
  assert.equal(calls.filter(c => c.method === 'agent').length, 1, '写被拒后不得重试 agent')
})

test('幂等去重：同一幂等键第二次激活直接返回 already_activated，不重复发 agent', async () => {
  let agentCalls = 0
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({ ok: true, out: JSON.stringify({ messages: [{ content: member.marker }] }) }),
    agent: async () => { agentCalls += 1; return { ok: true, out: '{"accepted":true}' } }
  })
  const tcpRunner = async () => ({ ok: false, err: 'ECONNREFUSED' }) // offline 触发激活路径

  await activateMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  const again = await activateMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(again.outcome, 'already_activated')
  assert.equal(agentCalls, 1, '重复激活不得二次投递')
})

test('限流：30 分钟窗口内第 3 次恢复尝试被拒并 blocked', async () => {
  const now = Date.parse('2026-09-08T12:00:00Z')
  const st = readState(member.id)
  st.activations = [
    { at: new Date(now - 1000).toISOString(), idempotencyKey: 'k1', outcome: 'send_failed' },
    { at: new Date(now - 2000).toISOString(), idempotencyKey: 'k2', outcome: 'send_failed' }
  ]
  writeState(member.id, st)

  const { runner } = fakeRunner({ agent: async () => ({ ok: true, out: '{"accepted":true}' }) })
  const res = await activateMember(member, { token: TOKEN, cliRunner: runner, now })
  assert.equal(res.outcome, 'blocked_rate_limited')
  const after = readState(member.id)
  assert.ok(after.blocked)
})

test('classify 单元判定：额度耗尽与权限拒绝不靠重启绕过', () => {
  assert.equal(classify({ tcp: { ok: true }, call: { ok: false, out: 'quota exceeded' }, markerFound: false }), 'quota_exhausted')
  assert.equal(classify({ tcp: { ok: true }, call: { ok: false, err: '403 FORBIDDEN', out: '' }, markerFound: false }), 'forbidden')
  assert.equal(classify({ tcp: { ok: true }, call: { kind: 'timeout' }, markerFound: false }), 'accepted_no_readback')
  assert.equal(classify({ tcp: { ok: true }, call: { ok: true, out: '{}' }, markerFound: true }), 'delivered')
  assert.equal(classify({ tcp: { ok: false }, call: null, markerFound: false }), 'offline')
})
