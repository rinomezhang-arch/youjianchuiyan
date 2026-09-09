// Bind one reviewed commit, its source files, and its already-built artifacts.
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { readFileSync, readdirSync, statSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve, relative, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const scopes = ['frontend_v3', 'banquet_project/src/main/java/com/youjian/banquet/controller/BillController.java',
  'banquet_project/src/main/java/com/youjian/banquet/service/BillReceiptService.java',
  'banquet_project/src/test/java/com/youjian/banquet/controller/BillReceiptTest.java', 'scripts/release/receipt-real-24'];
const git = (...args) => execFileSync('git', args, { cwd: root, encoding: 'utf8' }).trim();
const sha = (data) => createHash('sha256').update(data).digest('hex');
export const fileHash = (path) => sha(readFileSync(path));
function sources() {
  return git('ls-files', '-z', '--', ...scopes).split('\0').filter(Boolean).sort().map(path => ({ path, sha256: fileHash(resolve(root, path)) }));
}
function distFiles(dir = resolve(root, 'frontend_v3/dist')) {
  return readdirSync(dir).sort().flatMap(name => {
    const path = resolve(dir, name);
    return statSync(path).isDirectory() ? distFiles(path) : [{ path: relative(root, path).replaceAll('\\', '/'), sha256: fileHash(path) }];
  });
}
function current() {
  const src = sources();
  const dist = distFiles();
  if (!dist.some(f => f.path === 'frontend_v3/dist/index.html')) throw new Error('Built frontend index missing');
  return { sources: src, sourcesSha256: sha(JSON.stringify(src)), distSha256: sha(JSON.stringify(dist)), distFiles: dist.length,
    jarSha256: fileHash(resolve(root, 'banquet_project/target/banquet-1.0.0.jar')) };
}
export function verifyManifest(path) {
  const m = JSON.parse(readFileSync(path, 'utf8').replace(/^\uFEFF/, ''));
  const c = current();
  // Docs/evidence-only commits may follow the build; executable sources must match.
  git('diff', '--quiet', m.sourceHead, '--', ...scopes);
  for (const key of ['sourcesSha256', 'distSha256', 'jarSha256']) {
    if (m[key] !== c[key]) throw new Error(`Artifact binding mismatch: ${key}`);
  }
  return { sourceHead: m.sourceHead, runtimeHead: git('rev-parse', 'HEAD'), sourcesSha256: m.sourcesSha256,
    distSha256: m.distSha256, jarSha256: m.jarSha256, distFiles: m.distFiles };
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const [command, pathArg] = process.argv.slice(2);
  if (!pathArg || !['create', 'verify'].includes(command)) throw new Error('Usage: receipt-artifacts.mjs create|verify manifest.json');
  const path = resolve(pathArg);
  if (command === 'create') {
    git('diff', '--quiet', 'HEAD', '--', ...scopes);
    const m = { task: 'CO-RECEIPT-R1-29', sourceHead: git('rev-parse', 'HEAD'), builtAt: new Date().toISOString(), ...current() };
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, JSON.stringify(m, null, 2), { flag: 'wx' });
  }
  console.log(JSON.stringify(verifyManifest(path)));
}
