// TR-COLLAB-LIVENESS-17 夹具测试：三场景必须在**不停在线成员、不碰真实网关**前提下验证。
// 用注入的假 cliRunner/假 TCP 模拟：离线、启动后未响应、真实忙碌不误杀；
// 外加 FORBIDDEN 不绕过、幂等去重、30 分钟 2 次限流。
// 运行：node --test scripts/agent_bridge/liveness/liveness.test.mjs
import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, renameSync, writeFileSync, readFileSync, readdirSync } from 'node:fs'
import { join, dirname, basename } from 'node:path'
import { fileURLToPath } from 'node:url'

// 必须在导入状态模块前固定证据目录（模块导入时读取 env）
const runtimeRoot = join(dirname(fileURLToPath(import.meta.url)), '.test-runtime')
mkdirSync(runtimeRoot, { recursive: true })
const tmp = mkdtempSync(join(runtimeRoot, 'run-'))
process.env.LIVENESS_EVIDENCE_DIR = tmp

const { MEMBERS, readState, writeState, finalizeActivation } = await import('./state.mjs')
const { probeMember, classify, buildCliInvocation } = await import('./probe.mjs')
const { activateMember, verifyActivity } = await import('./activate.mjs')
const { inspectMember } = await import('./inspect-session.mjs')
const { checkOnce, createWatchdogRunner, acquireLock } = await import('./watchdog.mjs')
const { prepareClientConfig } = await import('./client-config.mjs')

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

test.after(() => {
  const trash = join(runtimeRoot, 'trash')
  mkdirSync(trash, { recursive: true })
  renameSync(tmp, join(trash, basename(tmp)))
})

function fakeScheduler() {
  const jobs = []
  return {
    jobs,
    schedule(fn, delay) { jobs.push({ fn, delay }); return jobs.length }
  }
}

test('场景一 离线：TCP 不可达 → offline 分类，agent 投递失败记为 send_failed（不杀任何进程）', async () => {
  const { runner, calls } = fakeRunner({
    agent: async () => ({ ok: false, kind: 'timeout', err: 'cli_timeout', out: '' })
  })
  const tcpRunner = async () => ({ ok: false, err: 'ECONNREFUSED' })

  const probe = await probeMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(probe.status, 'offline')

  const res = await checkOnce(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  assert.equal(res.action, 'none')
  assert.equal(res.reason, 'send_failed')
  const state = readState(member.id)
  assert.equal(state.activations.at(-1).outcome, 'send_failed')
  // 离线场景下 sessions.get 不应被调用（端口不通没有任何会话可查）
  assert.equal(calls.filter(c => c.method === 'sessions.get').length, 0)
})

test('主循环真实调度：accepted 后经注入定时器回读，活动增长才结算 verified', async () => {
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({ ok: true, out: JSON.stringify({ messages: [
      { role: 'user', content: `[${member.marker}][RESUME] work`, createdAt: '2026-09-08T12:00:00Z' },
      { role: 'assistant', content: 'working', createdAt: '2026-09-08T12:01:00Z' }
    ] }) }),
    agent: async () => ({ ok: true, out: '{"accepted":true}' })
  })
  // 让探测进入 accepted_no_readback，同时保留可比较基线。
  let probes = 0
  const cliRunner = async args => {
    if (args.method === 'sessions.get' && probes++ === 0) {
      calls.push({ method: args.method, params: args.params })
      return { ok: false, kind: 'timeout', out: '' }
    }
    return runner(args)
  }
  const clock = fakeScheduler()
  const watchdog = createWatchdogRunner({ token: TOKEN, members: [member], cliRunner,
    tcpRunner: async () => ({ ok: true }), schedule: clock.schedule, verifyAfterMs: 25 })
  const result = await watchdog.tick()
  assert.equal(result[0].action, 'activated')
  assert.equal(clock.jobs.length, 1)
  await clock.jobs[0].fn()
  assert.equal(readState(member.id).activations.at(-1).outcome, 'verified')
})

test('启动后未响应：主循环连续两次 accepted 无活动后 blocked，不手工伪造状态', async () => {
  let agentCalls = 0
  const { runner } = fakeRunner({
    'sessions.get': async () => ({ ok: false, kind: 'timeout', err: 'cli_timeout_30000ms', out: '' }),
    agent: async () => { agentCalls += 1; return { ok: true, out: JSON.stringify({ accepted: true, runId: 'r' + agentCalls }) } }
  })
  const tcpRunner = async () => ({ ok: true })

  const clock = fakeScheduler()
  const watchdog = createWatchdogRunner({ token: TOKEN, members: [member], cliRunner: runner, tcpRunner,
    schedule: clock.schedule, verifyAfterMs: 25 })
  assert.equal((await watchdog.tick())[0].action, 'activated')
  await clock.jobs.shift().fn()
  assert.equal(readState(member.id).activations.at(-1).outcome, 'verify_failed')
  assert.equal((await watchdog.tick())[0].action, 'activated')
  await clock.jobs.shift().fn()
  const after = readState(member.id)
  assert.ok(after.blocked, '两次激活无真实活动后必须 blocked 交 Codex，禁止无限循环')
  assert.match(after.blocked.reason, /无真实活动/)
  assert.equal(agentCalls, 2)
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

test('幂等去重：完成真实验真后同一幂等键不重复发 agent', async () => {
  let agentCalls = 0
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({ ok: true, out: JSON.stringify({ messages: [{ content: member.marker }] }) }),
    agent: async () => { agentCalls += 1; return { ok: true, out: '{"accepted":true}' } }
  })
  const tcpRunner = async () => ({ ok: false, err: 'ECONNREFUSED' }) // offline 触发激活路径

  await activateMember(member, { token: TOKEN, cliRunner: runner, tcpRunner })
  finalizeActivation(member.id, member.resumeIdempotencyKey, 'verified')
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

test('成功历史含 quota/额度仍按 marker 判 delivered', () => {
  const call = { ok: true, out: '{"messages":[{"role":"user","content":"额度不足前记得交接 quota"}]}' }
  assert.equal(classify({ tcp: { ok: true }, call, markerFound: true }), 'delivered')
})

test('恢复标识后的用户 exec 正文不算助手或工具活动', async () => {
  const { runner } = fakeRunner({
    'sessions.get': async () => ({ ok: true, out: JSON.stringify({ messages: [
      { role: 'user', content: `[${member.marker}][RESUME]` },
      { role: 'user', content: '继续执行 exec 检查，完成后向我报告' }
    ] }) })
  })
  const inspected = await inspectMember(member, { token: TOKEN, cliRunner: runner })
  assert.equal(inspected.toolLikeEntriesAfter, 0)
  assert.equal(inspected.assistantEntriesAfter, 0)
  assert.equal(inspected.realActivity, false)
})

test('unknown 不误激活：瞬时 CLI 错误只等待下一轮', async () => {
  const { runner, calls } = fakeRunner({
    'sessions.get': async () => ({ ok: false, kind: 'spawn_error', err: 'temporary spawn failure', out: '' }),
    agent: async () => { throw new Error('unknown 不得调用 agent') }
  })
  const result = await checkOnce(member, { token: TOKEN, cliRunner: runner, tcpRunner: async () => ({ ok: true }) })
  assert.equal(result.action, 'none')
  assert.equal(result.reason, 'unknown')
  assert.equal(calls.filter(c => c.method === 'agent').length, 0)
})

test('同一时刻只运行一个检查循环：重入 tick 复用同一 Promise', async () => {
  let release
  const gate = new Promise(resolve => { release = resolve })
  const { runner } = fakeRunner({
    'sessions.get': async () => { await gate; return { ok: true, out: '{"messages":[]}' } }
  })
  const watchdog = createWatchdogRunner({ token: TOKEN, members: [member], cliRunner: runner,
    tcpRunner: async () => ({ ok: true }), schedule: fakeScheduler().schedule })
  const first = watchdog.tick()
  const second = watchdog.tick()
  assert.equal(first, second)
  release()
  await first
})

test('原子锁：活 PID 拒绝第二实例，陈旧锁改名留证后可接管', () => {
  const liveDir = join(tmp, 'lock-live')
  acquireLock({ lockDir: liveDir, pid: 101, isAlive: () => true, now: () => 1000 })
  assert.throws(() => acquireLock({ lockDir: liveDir, pid: 202, isAlive: p => p === 101, now: () => 1001 }), /已有实例/)

  const staleDir = join(tmp, 'lock-stale')
  mkdirSync(staleDir, { recursive: true })
  writeFileSync(join(staleDir, 'watchdog.lock'), JSON.stringify({ pid: 303 }), 'utf8')
  acquireLock({ lockDir: staleDir, pid: 404, isAlive: () => false, now: () => 2000 })
  assert.equal(JSON.parse(readFileSync(join(staleDir, 'watchdog.lock'), 'utf8')).pid, 404)
  assert.ok(readdirSync(staleDir).some(name => name.startsWith('watchdog.lock.stale-2000-303')))
})

test('半写锁安全拒绝；两个回收者交错时第二个不能移动当前锁', () => {
  const partialDir = join(tmp, 'lock-partial')
  mkdirSync(partialDir, { recursive: true })
  writeFileSync(join(partialDir, 'watchdog.lock'), '', 'utf8')
  assert.throws(() => acquireLock({ lockDir: partialDir, pid: 2, isAlive: () => false }), /状态不确定/)
  assert.equal(readFileSync(join(partialDir, 'watchdog.lock'), 'utf8'), '')

  const raceDir = join(tmp, 'lock-reclaim-race')
  mkdirSync(raceDir, { recursive: true })
  writeFileSync(join(raceDir, 'watchdog.lock'), JSON.stringify({ pid: 700 }), 'utf8')
  let contenderError = null
  acquireLock({
    lockDir: raceDir, pid: 701, isAlive: p => p === 701, now: () => 3000,
    onBeforeReclaim() {
      try { acquireLock({ lockDir: raceDir, pid: 702, isAlive: () => false, now: () => 3001 }) }
      catch (e) { contenderError = e }
    }
  })
  assert.match(String(contenderError?.message), /其他进程回收/)
  assert.equal(JSON.parse(readFileSync(join(raceDir, 'watchdog.lock'), 'utf8')).pid, 701)
})

test('最小客户端配置不复制任何嵌套凭据，原配置保持不变', () => {
  const src = join(tmp, 'openclaw.fixture.json')
  const original = {
    auth: { cooldowns: { legacy: true } },
    gateway: { auth: { mode: 'token', token: 'gateway-secret' } },
    models: { providers: { sample: { apiKey: 'provider-secret' } } },
    channels: { sample: { token: 'channel-secret' } }
  }
  writeFileSync(src, JSON.stringify(original), 'utf8')
  const dst = prepareClientConfig({ url: 'ws://fixture.invalid:1234', tempRoot: join(tmp, 'client-config'), useCache: false })
  const sanitized = JSON.parse(readFileSync(dst, 'utf8'))
  assert.deepEqual(sanitized, { gateway: { mode: 'remote', remote: { url: 'ws://fixture.invalid:1234' } } })
  assert.equal(JSON.stringify(sanitized).includes('secret'), false)
  assert.deepEqual(JSON.parse(readFileSync(src, 'utf8')), original)
})

test('OpenClaw 调用 token 只走受支持环境变量，不进入 argv', () => {
  const launch = buildCliInvocation({ cliPath: 'openclaw.mjs', configPath: 'client.json', url: 'ws://test',
    token: 'fixture-secret', method: 'sessions.get', params: { sessionKey: 'fixture' }, timeoutMs: 10, parentEnv: {} })
  assert.equal(launch.args.includes('fixture-secret'), false)
  assert.equal(launch.args.includes('--token'), false)
  assert.equal(launch.args.includes('--url'), false)
  assert.equal(launch.env.OPENCLAW_GATEWAY_TOKEN, 'fixture-secret')
})
