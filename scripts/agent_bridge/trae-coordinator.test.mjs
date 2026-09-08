import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import { createCoordinator } from './trae-coordinator.mjs';

function fixture(overrides = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'trae-coordinator-test-'));
  const inbox = path.join(root, 'inbox');
  const archive = path.join(root, 'archive');
  const stateFile = path.join(root, 'state.json');
  fs.mkdirSync(inbox, { recursive: true });
  fs.mkdirSync(archive, { recursive: true });
  const calls = { status: 0, send: 0, reply: 0 };
  const dependencies = {
    inbox,
    archive,
    stateFile,
    startupSnapshot: overrides.startupSnapshot ?? [],
    isProcessAlive: overrides.isProcessAlive || (pid => pid === (overrides.ownerPid ?? 4242)),
    ownerPid: overrides.ownerPid ?? 4242,
    now: overrides.now || (() => 10_000),
    log: () => {},
    adapterStatus: overrides.adapterStatus || (async () => { calls.status++; return { ready: true, busy: false, sessionId: 'session-1' }; }),
    adapterSend: overrides.adapterSend || (async (id, prompt, sessionId) => { calls.send++; return { status: 200, data: { id, passed: true, sessionId, text: `reply:${prompt}` } }; }),
    sendReply: overrides.sendReply || (async () => { calls.reply++; })
  };
  return { root, inbox, archive, stateFile, calls, dependencies, coordinator: createCoordinator(dependencies) };
}

function put(inbox, id, content = `prompt:${id}`, fromTarget = 'codex/current', mtimeMs = Date.now()) {
  const file = path.join(inbox, `${id}.json`);
  fs.writeFileSync(file, JSON.stringify({ id, content, fromTarget }));
  fs.utimesSync(file, mtimeMs / 1000, mtimeMs / 1000);
  return file;
}

test('all 39 startup-snapshot messages are quarantined in place with zero sends', async () => {
  const startupSnapshot = Array.from({ length: 39 }, (_, i) => `old-${i}.json`);
  const f = fixture({ startupSnapshot });
  for (let i = 0; i < 39; i++) put(f.inbox, `old-${i}`, `historical-${i}`, 'codex/current', 10_000);
  await f.coordinator.scan();
  assert.equal(f.calls.status, 0);
  assert.equal(f.calls.send, 0);
  assert.equal(f.calls.reply, 0);
  assert.equal(fs.readdirSync(f.inbox).length, 39);
  assert.equal(fs.readdirSync(f.archive).length, 0);
  assert.equal(Object.values(f.coordinator.stateSnapshot().records).filter(x => x.phase === 'quarantined').length, 39);
  assert.deepEqual(f.coordinator.stateSnapshot().startupFiles, startupSnapshot.sort());
  const raw = fs.readFileSync(f.stateFile, 'utf8');
  assert.equal(raw.includes('historical-'), false);
});

test('new message completes once, replies once, archives, and persists no bodies', async () => {
  const f = fixture();
  put(f.inbox, 'new-1', 'SENSITIVE_PROMPT');
  await f.coordinator.scan();
  assert.deepEqual(f.calls, { status: 1, send: 1, reply: 1 });
  assert.equal(fs.readdirSync(f.inbox).length, 0);
  assert.equal(fs.readdirSync(f.archive).length, 1);
  assert.equal(f.coordinator.stateSnapshot().records['new-1'].phase, 'archived');
  const raw = fs.readFileSync(f.stateFile, 'utf8');
  assert.equal(raw.includes('SENSITIVE_PROMPT'), false);
  assert.equal(raw.includes('reply:SENSITIVE_PROMPT'), false);
});

test('reply failure retries only the reply and never calls native send again', async () => {
  let replyAttempts = 0;
  const f = fixture({ sendReply: async () => { f.calls.reply++; if (++replyAttempts === 1) throw Object.assign(new Error('synthetic'), { code: 'REPLY_DOWN' }); } });
  put(f.inbox, 'reply-retry');
  await f.coordinator.scan();
  assert.equal(f.coordinator.stateSnapshot().records['reply-retry'].phase, 'reply_pending');
  await f.coordinator.scan();
  assert.equal(f.calls.send, 1);
  assert.equal(f.calls.reply, 2);
  assert.equal(f.coordinator.stateSnapshot().records['reply-retry'].phase, 'archived');
});

test('ACK_LOST retries one fixed outbound id and produces one externally visible reply', async () => {
  const delivered = new Set();
  const outboundIds = [];
  let attempts = 0;
  const f = fixture({ sendReply: async (_target, _replyTo, _text, outboundId) => {
    f.calls.reply++;
    outboundIds.push(outboundId);
    delivered.add(outboundId);
    if (++attempts === 1) throw Object.assign(new Error('ack lost after delivery'), { code: 'ACK_LOST' });
  } });
  put(f.inbox, 'ack-lost');
  await f.coordinator.scan();
  const pendingState = fs.readFileSync(f.stateFile, 'utf8');
  const pendingRecord = f.coordinator.stateSnapshot().records['ack-lost'];
  assert.match(pendingRecord.outboundId, /^msg-[0-9a-f-]{36}$/);
  assert.equal(pendingState.includes('prompt:ack-lost'), false);
  assert.equal(pendingState.includes('reply:prompt:ack-lost'), false);
  await f.coordinator.scan();
  assert.equal(f.calls.send, 1);
  assert.equal(f.calls.reply, 2);
  assert.equal(delivered.size, 1);
  assert.equal(outboundIds[0], outboundIds[1]);
  assert.equal(f.coordinator.stateSnapshot().records['ack-lost'].phase, 'archived');
});

test('dispatching record on restart becomes uncertain and is never resent', async () => {
  const f = fixture();
  const file = put(f.inbox, 'crash-dispatch');
  const message = JSON.parse(fs.readFileSync(file));
  const contentHash = crypto.createHash('sha256').update(JSON.stringify([message.id, message.content, message.fromTarget, ''])).digest('hex');
  fs.writeFileSync(f.stateFile, JSON.stringify({ version: 2, startupFiles: [], records: { 'crash-dispatch': { fingerprint: 'dispatch-hash', contentHash, phase: 'dispatching' } } }));
  const restarted = createCoordinator(f.dependencies);
  await restarted.scan();
  assert.equal(f.calls.send, 0);
  assert.equal(restarted.stateSnapshot().records['crash-dispatch'].phase, 'uncertain');
  assert.equal(restarted.stateSnapshot().records['crash-dispatch'].reason, 'restart_after_dispatching');
});

test('reply-pending record on restart becomes uncertain and does not rerun native work', async () => {
  const f = fixture();
  const file = put(f.inbox, 'crash-reply');
  const message = JSON.parse(fs.readFileSync(file));
  const contentHash = crypto.createHash('sha256').update(JSON.stringify([message.id, message.content, message.fromTarget, ''])).digest('hex');
  fs.writeFileSync(f.stateFile, JSON.stringify({ version: 2, startupFiles: [], records: { 'crash-reply': { fingerprint: 'dispatch-hash', contentHash, phase: 'reply_pending' } } }));
  const restarted = createCoordinator(f.dependencies);
  await restarted.scan();
  assert.equal(f.calls.send, 0);
  assert.equal(f.calls.reply, 0);
  assert.equal(restarted.stateSnapshot().records['crash-reply'].phase, 'uncertain');
});

test('same id with changed content is marked conflict and never sent', async () => {
  const f = fixture({ startupSnapshot: ['same-id.json'] });
  const file = put(f.inbox, 'same-id', 'old-content', 'codex/current', 10_000);
  await f.coordinator.scan();
  fs.writeFileSync(file, JSON.stringify({ id: 'same-id', content: 'different-content', fromTarget: 'codex/current' }));
  fs.utimesSync(file, 30, 30);
  await f.coordinator.scan();
  assert.equal(f.calls.send, 0);
  assert.equal(f.coordinator.stateSnapshot().records['same-id'].phase, 'conflict');
});

test('overlapping scans are serialized by the persistent scan mutex', async () => {
  let release;
  const entered = new Promise(resolve => { release = resolve; });
  let markEntered;
  const waiting = new Promise(resolve => { markEntered = resolve; });
  const f = fixture({ adapterStatus: async () => { f.calls.status++; markEntered(); await entered; return { ready: true, busy: false, sessionId: 'session-1' }; } });
  put(f.inbox, 'mutex');
  const first = f.coordinator.scan();
  await waiting;
  const second = await f.coordinator.scan();
  assert.equal(second.skipped, true);
  release();
  await first;
  assert.equal(f.calls.send, 1);
});

test('two coordinators sharing inbox and state cannot dispatch the same message', async () => {
  const f = fixture({ isProcessAlive: () => true, ownerPid: 7001 });
  put(f.inbox, 'two-processes');
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  let entered;
  const enteredPromise = new Promise(resolve => { entered = resolve; });
  f.dependencies.adapterStatus = async () => { f.calls.status++; entered(); await gate; return { ready: true, busy: false, sessionId: 'session-1' }; };
  const first = createCoordinator(f.dependencies);
  const second = createCoordinator({ ...f.dependencies, ownerPid: 7002 });
  const firstScan = first.scan();
  await enteredPromise;
  const blocked = await second.scan();
  assert.equal(blocked.skipped, true);
  assert.equal(blocked.reason, 'cross_process_lock_held');
  release();
  await firstScan;
  await second.scan();
  assert.equal(f.calls.send, 1);
  assert.equal(f.calls.reply, 1);
});

test('message id must be safe and equal the filename basename', async () => {
  const f = fixture();
  fs.writeFileSync(path.join(f.inbox, 'file-id.json'), JSON.stringify({ id: 'other-id', content: 'no', fromTarget: 'codex/current' }));
  fs.writeFileSync(path.join(f.inbox, 'missing-id.json'), JSON.stringify({ content: 'no', fromTarget: 'codex/current' }));
  const oversized = 'x'.repeat(101);
  fs.writeFileSync(path.join(f.inbox, `${oversized}.json`), JSON.stringify({ id: oversized, content: 'no', fromTarget: 'codex/current' }));
  await f.coordinator.scan();
  assert.equal(f.calls.send, 0);
  assert.equal(f.coordinator.stateSnapshot().records['file-id'].reason, 'message_id_filename_mismatch');
  assert.equal(f.coordinator.stateSnapshot().records['missing-id'].reason, 'message_id_filename_mismatch');
  assert.equal(f.coordinator.stateSnapshot().records[oversized].reason, 'invalid_message_id');
});

test('prototype-shaped ids are isolated by a null-prototype state container', async () => {
  const f = fixture();
  put(f.inbox, '__proto__');
  put(f.inbox, 'constructor');
  await f.coordinator.scan();
  assert.equal(f.calls.send, 2);
  assert.equal(f.coordinator.stateSnapshot().records.__proto__.phase, 'archived');
  assert.equal(f.coordinator.stateSnapshot().records.constructor.phase, 'archived');
});

test('definitely-not-sent 409 may retry later, with native success occurring once', async () => {
  let attempts = 0;
  const f = fixture({ adapterSend: async (id, prompt, sessionId) => {
    f.calls.send++;
    if (++attempts === 1) return { status: 409, data: { error: 'busy' } };
    return { status: 200, data: { id, passed: true, sessionId, text: 'done' } };
  } });
  put(f.inbox, 'safe-retry');
  await f.coordinator.scan();
  assert.equal(f.coordinator.stateSnapshot().records['safe-retry'], undefined);
  await f.coordinator.scan();
  assert.equal(f.calls.send, 2);
  assert.equal(f.calls.reply, 1);
});

test('adapter transport failure becomes uncertain and never retries native send', async () => {
  const f = fixture({ adapterSend: async () => { f.calls.send++; throw Object.assign(new Error('lost'), { code: 'ECONNRESET' }); } });
  put(f.inbox, 'transport-loss');
  await f.coordinator.scan();
  await f.coordinator.scan();
  assert.equal(f.calls.send, 1);
  assert.equal(f.coordinator.stateSnapshot().records['transport-loss'].phase, 'uncertain');
});

test('adapter not ready or internally busy leaves new message untouched and unclaimed', async () => {
  const f = fixture({ adapterStatus: async () => { f.calls.status++; return { ready: false, busy: true, sessionId: 'session-1' }; } });
  put(f.inbox, 'not-ready');
  await f.coordinator.scan();
  assert.equal(f.calls.send, 0);
  assert.equal(f.coordinator.stateSnapshot().records['not-ready'], undefined);
  assert.equal(fs.existsSync(path.join(f.inbox, 'not-ready.json')), true);
});
