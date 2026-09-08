const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { createBridge } = require('./server');

function statePath() {
  return path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'trae-adapter-test-')), 'state.json');
}

async function fixture(t, execute, options = {}) {
  let claimed = false;
  const readContext = options.readContext || (key => key === 'AI_CHAT_WORK_STATUS' ? '' : null);
  const server = createBridge({
    token: 'test-only',
    stateFile: options.stateFile || statePath(),
    claimInitialization: () => { if (claimed) return false; claimed = true; return true; },
    execute: (command, ...args) => command === 'getContextKeyValue' ? readContext(...args) : execute(command, ...args)
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  if (t) t.after(() => server.close());
  const call = async (body, token = 'test-only', route = '/message') => {
    const response = await fetch(`http://127.0.0.1:${server.address().port}${route}`, {
      method: body ? 'POST' : 'GET',
      headers: { Authorization: `Bearer ${token}` },
      body: body && JSON.stringify(body)
    });
    return { status: response.status, body: await response.json() };
  };
  call.close = () => new Promise(resolve => server.close(resolve));
  return call;
}

const message = { id: 'probe-1', prompt: 'Reply TEST_ACK only', expectedSessionId: 'isolated' };
const normalExecute = async (command, prompt) => command === 'icube.chat.getCurrentSessionId' ? 'isolated' : { text: prompt === message.prompt ? 'TEST_ACK' : 'READY' };

test('initialize sends exactly fixed handshake in isolated session', async t => {
  let created = false;
  const call = await fixture(t, async (command, args) => {
    if (command === 'icube.chat.getCurrentSessionId') return created ? 'isolated' : null;
    assert.equal(command, 'wx.bridge.sendAndWaitResponse');
    assert.equal(args, '通信测试初始化，请只回复 TRAE-BRIDGE-READY，不执行工具不修改文件');
    created = true;
    return { text: 'TRAE-BRIDGE-READY' };
  });
  assert.equal((await call({}, 'wrong', '/initialize')).status, 401);
  assert.equal((await call({}, 'test-only', '/initialize')).body.created, true);
  assert.equal((await call({}, 'test-only', '/initialize')).body.created, false);
});

test('empty primary status is the sole idle truth; legacy null is diagnostic only', async t => {
  const call = await fixture(t, normalExecute, { readContext: key => key === 'AI_CHAT_WORK_STATUS' ? '' : undefined });
  const status = await call(null, 'test-only', '/status');
  assert.equal(status.body.ready, true);
  assert.deepEqual(status.body.nativeState, { workStatus: '', generating: null, workStatusKnown: true, knownIdle: true });
});

test('legacy generating value never overrides the authoritative primary status', async t => {
  const call = await fixture(t, normalExecute, { readContext: key => key === 'AI_CHAT_WORK_STATUS' ? '' : true });
  assert.equal((await call(null, 'test-only', '/status')).body.ready, true);
});

for (const workStatus of ['WORK_STATUS_GENERATING', 'WORK_STATUS_APPLYING', 'WORK_STATUS_HAS_DIFF', 'UNKNOWN_NEW_STATE', null]) {
  test(`primary status ${String(workStatus)} fails closed`, async t => {
    let sends = 0;
    const call = await fixture(t, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; return { text: 'bad' }; }, { readContext: key => key === 'AI_CHAT_WORK_STATUS' ? workStatus : false });
    const response = await call(message);
    assert.equal(response.status, 409);
    assert.equal(response.body.error, 'native_not_known_idle');
    assert.equal(sends, 0);
  });
}

test('primary status read exception fails closed while legacy remains diagnostic', async t => {
  let sends = 0;
  const call = await fixture(t, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; }, { readContext: key => { if (key === 'AI_CHAT_WORK_STATUS') throw new Error('synthetic'); return false; } });
  assert.equal((await call(message)).body.error, 'native_not_known_idle');
  assert.equal(sends, 0);
});

test('native busy state appearing at second read refuses to send', async t => {
  let reads = 0;
  let sends = 0;
  const call = await fixture(t, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; }, { readContext: key => key === 'AI_CHAT_WORK_STATUS' ? (++reads === 1 ? '' : 'WORK_STATUS_GENERATING') : null });
  assert.equal((await call(message)).body.error, 'native_not_known_idle');
  assert.equal(sends, 0);
});

test('authentication and wrong session never send', async t => {
  let sends = 0;
  const call = await fixture(t, async command => { if (command.includes('sendAnd')) sends++; return 'other'; });
  assert.equal((await call(message, 'wrong')).status, 401);
  assert.equal((await call(message)).body.error, 'session_mismatch');
  assert.equal(sends, 0);
});

test('native response and same-process request id deduplication', async t => {
  let sends = 0;
  const call = await fixture(t, async (command, prompt) => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; assert.equal(prompt, message.prompt); return { text: 'TEST_ACK' };
  });
  assert.equal((await call(message)).body.text, 'TEST_ACK');
  assert.equal((await call(message)).body.text, 'TEST_ACK');
  assert.equal((await call({ ...message, prompt: 'different' })).body.error, 'id_reused');
  assert.equal(sends, 1);
});

test('prototype-shaped request id is stored and deduplicated safely', async t => {
  let sends = 0;
  const call = await fixture(t, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; return { text: 'TEST_ACK' };
  });
  const prototypeMessage = { ...message, id: '__proto__' };
  assert.equal((await call(prototypeMessage)).status, 200);
  assert.equal((await call(prototypeMessage)).status, 200);
  assert.equal(sends, 1);
});

test('completed delivery survives restart without resending and state contains no bodies', async () => {
  const file = statePath();
  let sends = 0;
  const first = await fixture(null, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; return { text: 'SENSITIVE_REPLY' };
  }, { stateFile: file });
  assert.equal((await first(message)).status, 200);
  await first.close();
  const raw = fs.readFileSync(file, 'utf8');
  assert.equal(raw.includes(message.prompt), false);
  assert.equal(raw.includes('SENSITIVE_REPLY'), false);
  const second = await fixture(null, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; return { text: 'MUST_NOT_SEND' };
  }, { stateFile: file });
  const replay = await second(message);
  await second.close();
  assert.equal(replay.body.error, 'completed_response_unavailable_no_retry');
  assert.equal(sends, 1);
});

test('dispatching record becomes uncertain on restart and is never resent', async () => {
  const file = statePath();
  const fingerprint = crypto.createHash('sha256').update(JSON.stringify([message.prompt, message.expectedSessionId])).digest('hex');
  fs.writeFileSync(file, JSON.stringify({ version: 1, records: { [message.id]: { fingerprint, sessionId: 'isolated', phase: 'dispatching' } } }));
  let sends = 0;
  const call = await fixture(null, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; }, { stateFile: file });
  const response = await call(message);
  await call.close();
  assert.equal(response.body.error, 'delivery_uncertain_no_retry');
  assert.equal(sends, 0);
  assert.equal(JSON.parse(fs.readFileSync(file)).records[message.id].phase, 'uncertain');
});

test('native command failure after durable dispatch remains uncertain across restart', async () => {
  const file = statePath();
  let sends = 0;
  const first = await fixture(null, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; throw new Error('synthetic transport loss');
  }, { stateFile: file });
  assert.equal((await first(message)).body.deliveryUncertain, true);
  await first.close();
  const second = await fixture(null, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; }, { stateFile: file });
  assert.equal((await second(message)).body.error, 'delivery_uncertain_no_retry');
  await second.close();
  assert.equal(sends, 1);
});

test('empty reply is a terminal delivered failure and never sends twice', async t => {
  let sends = 0;
  const call = await fixture(t, async command => { if (command === 'icube.chat.getCurrentSessionId') return 'isolated'; sends++; return { text: ' ' }; });
  assert.equal((await call(message)).body.error, 'empty_model_reply');
  assert.equal((await call(message)).body.error, 'empty_model_reply');
  assert.equal(sends, 1);
});

test('session switch during send suppresses reply and remains terminal', async t => {
  let reads = 0;
  let sends = 0;
  const call = await fixture(t, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return ++reads <= 2 ? 'isolated' : 'changed';
    sends++; return { text: 'must not return' };
  });
  const response = await call(message);
  assert.equal(response.body.error, 'session_changed_during_request');
  assert.equal(response.body.text, undefined);
  assert.equal(sends, 1);
});

test('concurrent messages are rejected without second native send', async t => {
  let resolve;
  let markStarted;
  const started = new Promise(r => { markStarted = r; });
  let sends = 0;
  const call = await fixture(t, async command => {
    if (command === 'icube.chat.getCurrentSessionId') return 'isolated';
    sends++; markStarted(); return new Promise(r => { resolve = r; });
  });
  const first = call(message); await started;
  assert.equal((await call({ ...message, id: 'probe-2' })).body.error, 'busy');
  resolve({ text: 'TEST_ACK' });
  assert.equal((await first).status, 200);
  assert.equal(sends, 1);
});
