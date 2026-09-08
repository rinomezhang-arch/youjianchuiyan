const http = require('node:http');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const STATE_VERSION = 1;
const KNOWN_WORK_STATUSES = new Set(['', 'WORK_STATUS_GENERATING', 'WORK_STATUS_APPLYING', 'WORK_STATUS_HAS_DIFF']);

function atomicWriteJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const tmp = `${file}.${process.pid}.${crypto.randomUUID()}.tmp`;
  const handle = fs.openSync(tmp, 'wx', 0o600);
  try {
    fs.writeFileSync(handle, `${JSON.stringify(value, null, 2)}\n`);
    fs.fsyncSync(handle);
  } finally {
    fs.closeSync(handle);
  }
  fs.renameSync(tmp, file);
}

function createStateStore(file) {
  if (!file) throw new Error('durable_state_file_required');
  let state = { version: STATE_VERSION, records: Object.create(null) };
  if (fs.existsSync(file)) {
    const loaded = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (loaded?.version !== STATE_VERSION || typeof loaded.records !== 'object' || loaded.records === null || Array.isArray(loaded.records)) {
      throw new Error('unsupported_or_invalid_state');
    }
    const records = Object.create(null);
    for (const [id, record] of Object.entries(loaded.records)) records[id] = record;
    state = { version: STATE_VERSION, records };
  }
  let changed = false;
  for (const record of Object.values(state.records)) {
    if (record.phase === 'dispatching') {
      record.phase = 'uncertain';
      record.reason = 'restart_after_dispatching';
      record.updatedAt = new Date().toISOString();
      changed = true;
    }
  }
  if (changed) atomicWriteJson(file, state);
  return {
    get(id) { return Object.hasOwn(state.records, id) ? state.records[id] : null; },
    put(id, record) {
      state.records[id] = { ...record, updatedAt: new Date().toISOString() };
      atomicWriteJson(file, state);
      return state.records[id];
    },
    snapshot() { return JSON.parse(JSON.stringify(state)); }
  };
}

function createBridge({ token, execute, claimInitialization, stateFile }) {
  let busy = false;
  const responseCache = new Map();
  const stateStore = createStateStore(stateFile);

  const readNativeState = async () => {
    let workStatus = null;
    let generating = null;
    try {
      workStatus = (await execute('getContextKeyValue', 'AI_CHAT_WORK_STATUS')) ?? null;
    } catch { workStatus = null; }
    // Legacy diagnostic only. Current Trae builds may leave this key unbound.
    try {
      generating = (await execute('getContextKeyValue', 'icubeAiNgChatIng')) ?? null;
    } catch { generating = null; }
    const workStatusKnown = typeof workStatus === 'string' && KNOWN_WORK_STATUSES.has(workStatus);
    return { workStatus, generating, workStatusKnown, knownIdle: workStatusKnown && workStatus === '' };
  };

  const equal = value => {
    const received = Buffer.from(value || '');
    const expected = Buffer.from(`Bearer ${token}`);
    return received.length === expected.length && crypto.timingSafeEqual(received, expected);
  };

  return http.createServer(async (req, res) => {
    const reply = (status, data) => {
      if (res.destroyed) return;
      res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(data));
    };
    if (!equal(req.headers.authorization)) return reply(401, { error: 'unauthorized' });
    if (req.headers.origin) return reply(403, { error: 'browser_origin_rejected' });
    try {
      if (req.method === 'GET' && req.url === '/status') {
        const sessionId = await execute('icube.chat.getCurrentSessionId');
        const nativeState = await readNativeState();
        return reply(200, { ready: typeof sessionId === 'string' && !!sessionId && nativeState.knownIdle, busy, sessionId: sessionId || null, nativeState, stateProtocol: 'durable-at-most-once-v1' });
      }
      if (req.method === 'POST' && req.url === '/initialize') {
        if (busy) return reply(409, { error: 'busy' });
        busy = true;
        try {
          const existing = await execute('icube.chat.getCurrentSessionId');
          if (existing) return reply(200, { ready: true, created: false, sessionId: existing });
          const nativeState = await readNativeState();
          if (!nativeState.knownIdle) return reply(409, { error: 'native_not_known_idle', nativeState });
          if (!claimInitialization || !claimInitialization()) return reply(409, { ready: false, error: 'initialization_already_attempted' });
          const response = await execute('wx.bridge.sendAndWaitResponse', '通信测试初始化，请只回复 TRAE-BRIDGE-READY，不执行工具不修改文件');
          const sessionId = await execute('icube.chat.getCurrentSessionId');
          if (!sessionId || response?.text?.trim() !== 'TRAE-BRIDGE-READY') return reply(503, { ready: false, created: false, error: 'initialization_reply_unverified', deliveryUncertain: true });
          return reply(200, { ready: true, created: true, sessionId, text: response.text });
        } finally { busy = false; }
      }
      if (req.method !== 'POST' || req.url !== '/message') return reply(404, { error: 'not_found' });
      const chunks = [];
      let size = 0;
      for await (const chunk of req) {
        size += chunk.length;
        if (size > 32768) return reply(413, { error: 'request_too_large' });
        chunks.push(chunk);
      }
      let body;
      try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); }
      catch { return reply(400, { error: 'invalid_json' }); }
      const { id, prompt, expectedSessionId } = body || {};
      if (typeof id !== 'string' || !/^[a-zA-Z0-9_-]{1,100}$/.test(id) || typeof prompt !== 'string' || !prompt.trim() || prompt.length > 16000 || typeof expectedSessionId !== 'string' || !expectedSessionId) return reply(400, { error: 'invalid_request' });
      const fingerprint = crypto.createHash('sha256').update(JSON.stringify([prompt, expectedSessionId])).digest('hex');
      const previous = stateStore.get(id);
      if (previous) {
        if (previous.fingerprint !== fingerprint) return reply(409, { error: 'id_reused' });
        if (responseCache.has(id)) {
          const cached = responseCache.get(id);
          return reply(cached.status, cached.result);
        }
        if (previous.phase === 'uncertain' || previous.phase === 'dispatching') return reply(409, { error: 'delivery_uncertain_no_retry', deliveryUncertain: true });
        return reply(409, { error: 'completed_response_unavailable_no_retry', deliveryUncertain: false });
      }
      if (busy) return reply(409, { error: 'busy' });
      busy = true;
      try {
        const before = await execute('icube.chat.getCurrentSessionId');
        if (before !== expectedSessionId) return reply(409, { error: 'session_mismatch' });
        const firstState = await readNativeState();
        if (!firstState.knownIdle) return reply(409, { error: 'native_not_known_idle', nativeState: firstState });
        await new Promise(resolve => setTimeout(resolve, 150));
        const secondState = await readNativeState();
        if (!secondState.knownIdle) return reply(409, { error: 'native_not_known_idle', nativeState: secondState });
        if (await execute('icube.chat.getCurrentSessionId') !== expectedSessionId) return reply(409, { error: 'session_mismatch' });
        try {
          stateStore.put(id, { fingerprint, sessionId: expectedSessionId, phase: 'dispatching' });
        } catch {
          return reply(503, { error: 'state_persistence_failed', deliveryUncertain: false });
        }
        let result;
        try {
          result = await execute('wx.bridge.sendAndWaitResponse', prompt);
        } catch {
          stateStore.put(id, { fingerprint, sessionId: expectedSessionId, phase: 'uncertain', reason: 'native_command_failed_after_dispatch' });
          const output = { id, passed: false, error: 'native_command_failed', deliveryUncertain: true };
          responseCache.set(id, { status: 502, result: output });
          return reply(502, output);
        }
        const after = await execute('icube.chat.getCurrentSessionId');
        let status = 200;
        let output;
        let outcome = 'reply_received';
        if (after !== expectedSessionId) {
          status = 409; outcome = 'session_changed_during_request'; output = { id, passed: false, error: outcome };
        } else if (typeof result?.text !== 'string' || !result.text.trim()) {
          status = 502; outcome = 'empty_model_reply'; output = { id, passed: false, error: outcome };
        } else {
          output = { id, passed: true, sessionId: after, text: result.text };
        }
        stateStore.put(id, { fingerprint, sessionId: expectedSessionId, phase: 'completed', outcome, responseHash: typeof result?.text === 'string' ? crypto.createHash('sha256').update(result.text).digest('hex') : null });
        responseCache.set(id, { status, result: output });
        return reply(status, output);
      } finally { busy = false; }
    } catch {
      return reply(500, { error: 'adapter_failure' });
    }
  });
}

module.exports = { createBridge, createStateStore, KNOWN_WORK_STATUSES };
