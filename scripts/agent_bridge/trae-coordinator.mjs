import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import { pathToFileURL } from 'node:url';

const STATE_VERSION = 2;
const DEFINITELY_NOT_SENT = new Set(['busy', 'session_mismatch', 'native_not_known_idle']);
const SAFE_ID = /^[a-zA-Z0-9_-]{1,100}$/;

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

export function createCoordinatorState(file, now = () => Date.now()) {
  let state;
  const parse = () => {
    if (!fs.existsSync(file)) return { version: STATE_VERSION, startupFiles: null, records: Object.create(null) };
    const loaded = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (loaded?.version !== STATE_VERSION || !Array.isArray(loaded.startupFiles) || typeof loaded.records !== 'object' || loaded.records === null || Array.isArray(loaded.records)) throw new Error('unsupported_or_invalid_coordinator_state');
    const records = Object.create(null);
    for (const [id, record] of Object.entries(loaded.records)) records[id] = record;
    return { version: STATE_VERSION, startupFiles: [...new Set(loaded.startupFiles)], records };
  };
  state = parse();
  return {
    reload() { state = parse(); },
    initializeStartupFiles(files) {
      if (state.startupFiles !== null) return;
      state.startupFiles = [...new Set(files.filter(name => /^[\w-]+\.json$/.test(name)))].sort();
      atomicWriteJson(file, state);
    },
    isStartupFile(fileName) { return state.startupFiles?.includes(fileName) === true; },
    recoverInterrupted(isProcessAlive) {
      let changed = false;
      for (const record of Object.values(state.records)) {
        const activeOwner = Number.isInteger(record.ownerPid) && isProcessAlive(record.ownerPid);
        if (activeOwner) continue;
        if (record.phase === 'dispatching') {
          record.phase = 'uncertain';
          record.reason = 'restart_after_dispatching';
        } else if (record.phase === 'response_received' || record.phase === 'reply_pending') {
          record.phase = 'uncertain';
          record.reason = 'restart_without_persisted_reply_body';
        } else continue;
        record.updatedAt = new Date(now()).toISOString();
        changed = true;
      }
      if (changed) atomicWriteJson(file, state);
    },
    get(id) { return Object.hasOwn(state.records, id) ? state.records[id] : null; },
    put(id, record) {
      state.records[id] = { ...record, updatedAt: new Date(now()).toISOString() };
      atomicWriteJson(file, state);
      return state.records[id];
    },
    remove(id) {
      delete state.records[id];
      atomicWriteJson(file, state);
    },
    snapshot() { return JSON.parse(JSON.stringify(state)); }
  };
}

function defaultIsProcessAlive(pid) {
  try { process.kill(pid, 0); return true; }
  catch (error) { return error?.code === 'EPERM'; }
}

function createPersistentScanLock(stateFile, isProcessAlive, ownerPid = process.pid) {
  const available = `${stateFile}.scan.available`;
  const held = `${stateFile}.scan.held`;
  fs.mkdirSync(path.dirname(stateFile), { recursive: true });
  if (!fs.existsSync(available) && !fs.existsSync(held)) {
    try { fs.writeFileSync(available, '{}\n', { flag: 'wx', mode: 0o600 }); }
    catch (error) { if (error?.code !== 'EEXIST') throw error; }
  }
  const acquire = () => {
    try {
      fs.renameSync(available, held);
      fs.writeFileSync(held, `${JSON.stringify({ ownerPid, acquiredAt: new Date().toISOString() })}\n`, { mode: 0o600 });
      return true;
    } catch (error) {
      if (!['ENOENT', 'EEXIST', 'EPERM'].includes(error?.code)) throw error;
      try {
        const owner = JSON.parse(fs.readFileSync(held, 'utf8'));
        if (!Number.isInteger(owner.ownerPid) || isProcessAlive(owner.ownerPid)) return false;
        fs.renameSync(held, available);
        return acquire();
      } catch { return false; }
    }
  };
  return {
    acquire,
    release() { fs.renameSync(held, available); }
  };
}

function messageFingerprint(message, sessionId = '') {
  return crypto.createHash('sha256').update(JSON.stringify([
    message.id || '', message.content || '', message.fromTarget || '', sessionId
  ])).digest('hex');
}

function replyMessageId(id, target, replyHash) {
  const hex = crypto.createHash('sha256').update(JSON.stringify([id, target, replyHash])).digest('hex');
  return `msg-${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`;
}

export function createCoordinator({
  inbox,
  archive,
  stateFile,
  startupSnapshot = null,
  adapterStatus,
  adapterSend,
  sendReply,
  isProcessAlive = defaultIsProcessAlive,
  ownerPid = process.pid,
  now = () => Date.now(),
  log = () => {}
}) {
  fs.mkdirSync(inbox, { recursive: true });
  fs.mkdirSync(archive, { recursive: true });
  const capturedStartupFiles = startupSnapshot === null
    ? fs.readdirSync(inbox).filter(name => /^[\w-]+\.json$/.test(name)).sort()
    : [...startupSnapshot];
  const state = createCoordinatorState(stateFile, now);
  const scanLock = createPersistentScanLock(stateFile, isProcessAlive, ownerPid);
  const pendingReplies = new Map();
  let scanning = false;

  const record = (id, value) => state.put(id, value);
  const archiveFile = (file, id, fingerprint, contentHash = null) => {
    const source = path.join(inbox, file);
    if (!fs.existsSync(source)) return;
    fs.renameSync(source, path.join(archive, file));
    record(id, { fingerprint, contentHash, phase: 'archived' });
  };

  async function deliverReply({ file, id, fingerprint, contentHash, fromTarget, text }) {
    const replyHash = crypto.createHash('sha256').update(text).digest('hex');
    const outboundId = replyMessageId(id, fromTarget, replyHash);
    record(id, { fingerprint, contentHash, phase: 'reply_pending', replyHash, outboundId, ownerPid });
    pendingReplies.set(id, { file, id, fingerprint, contentHash, fromTarget, text, outboundId });
    try {
      await sendReply(fromTarget, id, text, outboundId);
    } catch (error) {
      log(`REPLY pending msg=${id} code=${error?.code || 'FAILED'}`);
      return false;
    }
    pendingReplies.delete(id);
    record(id, { fingerprint, contentHash, phase: 'replied' });
    try {
      archiveFile(file, id, fingerprint, contentHash);
    } catch (error) {
      log(`ARCHIVE pending msg=${id} code=${error?.code || 'FAILED'}`);
    }
    return true;
  }

  async function retryReplies() {
    for (const pending of [...pendingReplies.values()]) await deliverReply(pending);
  }

  async function processFile(file) {
    const source = path.join(inbox, file);
    if (!fs.existsSync(source)) return;
    let message;
    try { message = JSON.parse(fs.readFileSync(source, 'utf8')); }
    catch {
      const id = file.replace(/\.json$/, '');
      record(id, { fingerprint: null, phase: 'quarantined', reason: 'unparseable' });
      return;
    }
    const fileId = file.replace(/\.json$/, '');
    if (typeof message.id !== 'string' || !SAFE_ID.test(message.id) || message.id !== fileId) {
      record(fileId, { fingerprint: null, phase: 'quarantined', reason: message.id === fileId ? 'invalid_message_id' : 'message_id_filename_mismatch' });
      return;
    }
    const id = message.id;
    const contentHash = messageFingerprint(message);
    const existing = state.get(id);
    if (existing?.contentHash && existing.contentHash !== contentHash) {
      record(id, { ...existing, phase: 'conflict', reason: 'id_reused_with_different_content' });
      return;
    }
    if (state.isStartupFile(file) && !existing) {
      record(id, { fingerprint: contentHash, contentHash, phase: 'quarantined', reason: 'startup_snapshot' });
      return;
    }
    if (existing) {
      if (existing.phase === 'replied') {
        try { archiveFile(file, id, existing.fingerprint, existing.contentHash); } catch {}
      }
      return;
    }
    const prompt = typeof message.content === 'string' ? message.content : '';
    const fromTarget = message.fromTarget || 'openclaw/solo';
    if (!prompt.trim()) {
      record(id, { fingerprint: contentHash, contentHash, phase: 'quarantined', reason: 'empty_prompt' });
      return;
    }
    const status = await adapterStatus();
    if (!status?.ready || status?.busy || !status?.sessionId) return;
    const fingerprint = messageFingerprint(message, status.sessionId);
    record(id, { fingerprint, contentHash, phase: 'dispatching', sessionId: status.sessionId, ownerPid });
    let result;
    try {
      result = await adapterSend(id, prompt.substring(0, 16000), status.sessionId);
    } catch (error) {
      record(id, { fingerprint, contentHash, phase: 'uncertain', reason: 'adapter_transport_failed' });
      log(`UNCERTAIN msg=${id} code=${error?.code || 'FAILED'}`);
      return;
    }
    if (result.status === 409 && DEFINITELY_NOT_SENT.has(result.data?.error)) {
      // Adapter explicitly proves the native command was not invoked. It is
      // safe to remove the dispatch claim and retry on a later scan.
      state.remove(id);
      return;
    }
    if (result.status === 200 && result.data?.passed && typeof result.data.text === 'string') {
      record(id, { fingerprint, contentHash, phase: 'response_received', responseHash: crypto.createHash('sha256').update(result.data.text).digest('hex'), ownerPid });
      await deliverReply({ file, id, fingerprint, contentHash, fromTarget, text: result.data.text });
      return;
    }
    const uncertain = result.data?.deliveryUncertain !== false;
    record(id, { fingerprint, contentHash, phase: uncertain ? 'uncertain' : 'terminal_failure', reason: result.data?.error || `http_${result.status}` });
  }

  async function scan() {
    if (scanning) return { skipped: true };
    scanning = true;
    let locked = false;
    try {
      locked = scanLock.acquire();
      if (!locked) return { skipped: true, reason: 'cross_process_lock_held' };
      state.reload();
      state.initializeStartupFiles(capturedStartupFiles);
      state.recoverInterrupted(isProcessAlive);
      await retryReplies();
      const files = fs.readdirSync(inbox).filter(name => /^[\w-]+\.json$/.test(name)).sort();
      for (const file of files) await processFile(file);
      return { skipped: false, files: files.length };
    } finally {
      if (locked) scanLock.release();
      scanning = false;
    }
  }

  return { scan, stateSnapshot: () => state.snapshot(), get scanning() { return scanning; } };
}

async function main() {
  const inbox = path.join(os.homedir(), '.agent-bridge', 'inbox', 'trae', 'solo');
  const archive = path.join(os.homedir(), '.agent-bridge', 'inbox', '.archive', 'trae', 'solo');
  const stateDir = path.join(os.homedir(), '.agent-bridge', 'trae-coordinator');
  const stateFile = path.join(stateDir, 'delivery-state.json');
  const workspace = 'F:/solo/artifacts/bridge-evidence-20260907/trae-workspace';
  const descriptor = path.join(workspace, 'adapter-private.json');
  const startupSnapshot = fs.existsSync(inbox)
    ? fs.readdirSync(inbox).filter(name => /^[\w-]+\.json$/.test(name)).sort()
    : [];
  const log = message => console.log(`[${new Date().toISOString()}] ${message}`);

  const bridgeRoot = '../../artifacts/agent-bridge-20260907/mcp-server/build';
  const { createMessage, sendLocalMessage } = await import(`${bridgeRoot}/inbox.js`);
  const { getLocalMachineName } = await import(`${bridgeRoot}/config.js`);

  const getAdapter = () => {
    if (!fs.existsSync(descriptor)) return null;
    const value = JSON.parse(fs.readFileSync(descriptor, 'utf8'));
    return value?.url && value?.token ? value : null;
  };
  const adapterCall = async (route, init = {}) => {
    const adapter = getAdapter();
    if (!adapter) throw Object.assign(new Error('adapter_descriptor_missing'), { code: 'NO_ADAPTER' });
    return fetch(`${adapter.url}${route}`, { ...init, headers: { Authorization: `Bearer ${adapter.token}`, ...(init.headers || {}) } });
  };
  const coordinator = createCoordinator({
    inbox,
    archive,
    stateFile,
    startupSnapshot,
    log,
    adapterStatus: async () => (await adapterCall('/status')).json(),
    adapterSend: async (id, prompt, sessionId) => {
      const response = await adapterCall('/message', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id, prompt, expectedSessionId: sessionId }) });
      return { status: response.status, data: await response.json() };
    },
    sendReply: async (target, replyTo, text, outboundId) => {
      const localName = getLocalMachineName();
      const message = createMessage(localName, localName, 'message', text, replyTo, undefined, target, 'trae/solo');
      message.id = outboundId;
      sendLocalMessage(message);
    }
  });
  log(`Trae coordinator started startup_snapshot=${startupSnapshot.length}`);
  await coordinator.scan();
  setInterval(() => { void coordinator.scan().catch(error => log(`ERR ${error.message}`)); }, 3000);
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  main().catch(error => { console.error(`[fatal] ${error.message}`); process.exitCode = 1; });
}
