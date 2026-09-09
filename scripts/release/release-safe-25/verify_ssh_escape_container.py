#!/usr/bin/env python3
"""One actual deploy, rejected drift rollback, then complete rollback; container only."""
from pathlib import Path
import datetime, hashlib, json, os, shutil, subprocess, sys

ROOT = Path(__file__).resolve().parent.parent
if not Path('/.dockerenv').is_file() or os.environ.get('RC15_NO_REMAP') != '1':
    sys.exit('CONTAINER_ONLY')
ROOT.relative_to(Path('/work/release'))
assert not Path('/root/.ssh').exists() and not Path('/home/ubuntu/.ssh').exists()
OUT = ROOT / 'evidence-ssh-escape'
OUT.mkdir(exist_ok=False)
sha = lambda data: hashlib.sha256(data).hexdigest()
def tree(path):
    return {p.relative_to(path).as_posix(): sha(p.read_bytes()) for p in sorted(path.rglob('*')) if p.is_file()}
result = {'baseHead': '55c302c030af0da0af1a96f2dd3f913b09ba0a23', 'container': 'rc15-safe-e2e-20260909',
          'scope': 'Actual Bash deploy/rollback with synthetic filesystem and external-command models; no real DB/build/service/network',
          'sourceHashes': {str(p.relative_to(ROOT)): sha(p.read_bytes()) for p in [ROOT/'restaurant-rc-15/deploy-rc15.sh', ROOT/'restaurant-rc-15/rollback-rc15.sh', Path(__file__), Path(__file__).with_name('e2e_stub_run.py')]},
          'runs': [], 'checks': {}, 'status': 'running'}
try:
    # Preserve earlier synthetic runs before creating this run's clean fixtures.
    for src in [Path('/home/ubuntu'), Path('/opt/youjianchuiyan')]:
        if src.exists():
            assert src.resolve() == src and src in [Path('/home/ubuntu'), Path('/opt/youjianchuiyan')]
            dst = OUT / ('prior-' + src.name)
            dst.resolve().relative_to(OUT.resolve())
            assert not dst.exists()
            shutil.move(str(src), str(dst))
    env = dict(os.environ, RC15_TS=datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
    setup = subprocess.run([sys.executable, str(Path(__file__).with_name('e2e_stub_run.py'))], env=env, capture_output=True, text=True)
    (OUT/'setup.log').write_text(setup.stdout + setup.stderr)
    assert setup.returncode == 0, 'fixture setup failed: see setup.log'
    sandbox = Path(next(line.split(' ', 1)[1] for line in setup.stdout.splitlines() if line.startswith('SANDBOX_READY ')))
    result['sandbox'] = str(sandbox)
    env.update(PATH=str(sandbox/'bin') + ':' + env['PATH'], WORKTREE=str(sandbox/'worktree'), TMPDIR=str(sandbox/'tmp'),
               R='LOCAL_R_MUST_NOT_EXPAND', T='LOCAL_T_MUST_NOT_EXPAND', c='LOCAL_C_MUST_NOT_EXPAND')
    project = Path('/home/ubuntu/deploy_tmp_main/banquet_project')
    dist = Path('/opt/youjianchuiyan/frontend_v3/dist')
    jar = project/'target/banquet-1.0.0.jar'
    initial_backend, initial_frontend = tree(project), tree(dist)
    def run(name, script, extra=()):
        completed = subprocess.run(['bash', str(ROOT/'restaurant-rc-15'/script), *extra], env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        path = OUT/(name+'.log')
        path.write_text(completed.stdout)
        result['runs'].append({'name': name, 'exit': completed.returncode, 'log': str(path), 'logSha256': sha(path.read_bytes())})
        return completed
    deployed = run('deploy', 'deploy-rc15.sh')
    assert deployed.returncode == 0, 'new deploy failure; see deploy.log'
    assert tree(project) != initial_backend and sha(jar.read_bytes()) != initial_backend['target/banquet-1.0.0.jar']
    result['checks']['localVariablePoisonIgnored'] = True
    result['backendPreDeploy'] = initial_backend
    result['backendPostDeploy'] = tree(project)
    # Change one deployed frontend file, preserving its exact post-deploy bytes.
    index = dist/'index.html'
    post_index = index.read_bytes()
    (OUT/'index.post-deploy.fixture').write_bytes(post_index)
    index.write_bytes(post_index + b'\nSYNTHETIC-FRONTEND-DRIFT\n')
    before_backend, before_frontend, before_jar = tree(project), tree(dist), sha(jar.read_bytes())
    calls = sandbox/'calls.log'
    call_offset = len(calls.read_bytes())
    env['ROLLBACK_FRONTEND'] = '1'
    rejected = run('frontend-drift-rejection', 'rollback-rc15.sh', [env['RC15_TS']])
    added_calls = calls.read_bytes()[call_offset:]
    assert rejected.returncode != 0, 'frontend drift was not rejected'
    assert 'index.html' in rejected.stdout, 'rejection did not identify frontend drift'
    assert '--apply' not in added_calls.decode() and '2c.' not in rejected.stdout, 'restore started before drift rejection'
    assert tree(project) == before_backend and tree(dist) == before_frontend and sha(jar.read_bytes()) == before_jar
    result['checks'].update(frontendDriftRejectedBeforeFirstRestore=True, backendAllFileHashesUnchanged=True, jarUnchanged=True, frontendUnchangedAfterRejection=True)
    result['driftJarBeforeSha256'] = before_jar
    result['driftJarAfterSha256'] = sha(jar.read_bytes())
    (OUT/'index.drift.fixture').write_bytes(index.read_bytes())
    index.write_bytes(post_index)
    rolled = run('complete-rollback', 'rollback-rc15.sh', [env['RC15_TS']])
    assert rolled.returncode == 0, 'new complete rollback failure; see complete-rollback.log'
    assert tree(project) == initial_backend, 'backend or JAR not fully restored'
    assert tree(dist) == initial_frontend, 'frontend not fully restored'
    result['checks'].update(backendAndJarRestored=True, frontendRestored=True, frozenFixturesPreserved=True)
    result['status'] = 'passed'
except Exception as exc:
    result['status'] = 'failed'
    result['failure'] = str(exc)
finally:
    result['finishedAt'] = datetime.datetime.now().astimezone().isoformat()
    (OUT/'summary.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'status': result['status'], 'runs': result['runs'], 'checks': result['checks'], 'failure': result.get('failure'), 'summary': str(OUT/'summary.json')}, ensure_ascii=False))
sys.exit(0 if result['status'] == 'passed' else 1)
