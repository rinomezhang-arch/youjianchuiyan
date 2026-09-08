const vscode = require('vscode');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { createBridge } = require('./server');

const WORKSPACE = 'F:/solo/artifacts/bridge-evidence-20260907/trae-workspace';
const normalize = p => path.resolve(p).toLowerCase();
async function activate(context) {
  const folders = vscode.workspace.workspaceFolders || [];
  if (folders.length !== 1 || normalize(folders[0].uri.fsPath) !== normalize(WORKSPACE)) return;
  let starting = false, started = false, disposed = false;
  const diagnostic = (state, extra = {}) => fs.writeFileSync(path.join(WORKSPACE, 'adapter-startup-status.json'), JSON.stringify({ version: '0.1.5', state, at: new Date().toISOString(), ...extra }, null, 2));
  context.subscriptions.push({ dispose() { disposed = true; } });
  const start = async () => {
    if (starting || started || disposed) return;
    if (!vscode.workspace.isTrusted) { diagnostic('waiting_for_workspace_trust'); return; }
    starting = true;
    try {
      const required = ['icube.chat.getCurrentSessionId', 'wx.bridge.sendAndWaitResponse', 'getContextKeyValue'];
      const deadline = Date.now() + 30000;
      let missing = required;
      while (!disposed) {
        const commands = await vscode.commands.getCommands(true);
        missing = required.filter(c => !commands.includes(c));
        if (!missing.length) break;
        diagnostic('waiting_for_native_commands', { missing });
        if (Date.now() >= deadline) return diagnostic('native_commands_timeout', { missing });
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      if (disposed) return;
      const token = crypto.randomBytes(32).toString('hex');
      const claimInitialization = () => {
        try {
          fs.writeFileSync(path.join(WORKSPACE, '.adapter-initialization-attempt.json'), JSON.stringify({ attemptedAt: new Date().toISOString(), version: '0.1.5' }), { flag: 'wx', mode: 0o600 });
          return true;
        } catch (error) { if (error.code === 'EEXIST') return false; throw error; }
      };
      const bridge = createBridge({
        token,
        claimInitialization,
        execute: (...args) => vscode.commands.executeCommand(...args),
        stateFile: path.join(WORKSPACE, '.adapter-delivery-state.json')
      });
      await new Promise((resolve, reject) => { bridge.once('error', reject); bridge.listen(0, '127.0.0.1', resolve); });
      context.subscriptions.push({ dispose() { bridge.close(); } });
      const descriptor = path.join(WORKSPACE, 'adapter-private.json');
      fs.writeFileSync(descriptor, JSON.stringify({ url: `http://127.0.0.1:${bridge.address().port}`, token, pid: process.pid, version: '0.1.5', stateProtocol: 'durable-at-most-once-v1' }, null, 2), { mode: 0o600 });
      started = true;
      diagnostic('listening');
    } catch (error) { diagnostic('startup_error', { code: typeof error.code === 'string' ? error.code : 'START_FAILED' }); }
    finally { starting = false; }
  };
  context.subscriptions.push(vscode.workspace.onDidGrantWorkspaceTrust(() => { void start(); }));
  context.subscriptions.push(vscode.extensions.onDidChange(() => { void start(); }));
  await start();
}
module.exports = { activate, deactivate() {} };
