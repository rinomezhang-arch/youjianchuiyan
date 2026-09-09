# -*- coding: utf-8 -*-
"""CL25 r2：对**实际恢复算法**的三个最小反例。

跑的是 rc15_restore.py 本体（子进程调用，和脚本在远端调它的方式一样），
不是它的抄件，也不是"库里有这个函数"这种间接证据。
另外核一遍两个 shell 脚本确实调用了它——r1 我就是栽在"有函数没调用"上。

不连网络、不碰生产，全部在临时目录里造树。
"""
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile

# r3：TemporaryDirectory 会在退出时真删测试树，证据也跟着没了。
# 改成建在固定的测试垃圾桶下并保留，需要复核时能直接翻。
KEEP_ROOT = os.path.join(tempfile.gettempdir(), 'rc15_test_keep')
os.makedirs(KEEP_ROOT, exist_ok=True)


class KeptDir(object):
    """跟 TemporaryDirectory 同样的用法，但退出时不删。"""

    def __init__(self, tag):
        self.path = tempfile.mkdtemp(prefix=tag + '-', dir=KEEP_ROOT)

    def __enter__(self):
        return self.path

    def __exit__(self, *exc):
        return False

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(HERE, 'rc15_restore.py')
RC = os.path.normpath(os.path.join(HERE, '..', 'restaurant-rc-15'))

PASS = []
FAIL = []


def check(name, cond, detail=''):
    (PASS if cond else FAIL).append(name)
    print('%-46s %s  %s' % (name, 'PASS' if cond else 'FAIL', detail))


def sha(path):
    if not os.path.exists(path):
        return 'ABSENT'
    h = hashlib.sha256()
    with open(path, 'rb') as fh:
        for c in iter(lambda: fh.read(65536), b''):
            h.update(c)
    return h.hexdigest()


def run(*args):
    return subprocess.run([sys.executable, TOOL] + list(args),
                          capture_output=True, text=True, encoding='utf-8', errors='replace')


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(text)


def build_case(root):
    """造一棵最小的树：一个被本次发布改过的旧文件、一个本次新增文件、
    一个法务哨兵、一个别人后来加的资源。后两者都不在白名单里。"""
    src = os.path.join(root, 'repo')
    bk = os.path.join(root, 'backup')
    write(os.path.join(src, 'a/Changed.java'), 'AFTER-DEPLOY\n')
    write(os.path.join(src, 'a/Added.java'), 'NEW-BY-DEPLOY\n')
    write(os.path.join(src, 'legal/LegalController.java'), 'LEGAL-FROZEN\n')
    write(os.path.join(src, 'assets/other-person.js'), 'SOMEONE-ELSE\n')
    write(os.path.join(bk, 'a/Changed.java'), 'BEFORE-DEPLOY\n')      # 发布前版本
    wl = os.path.join(root, 'wl.txt')
    write(wl, 'a/Changed.java\na/Added.java\n')
    pre = os.path.join(root, 'pre.manifest')
    post = os.path.join(root, 'post.manifest')
    write(pre, '%s\ta/Changed.java\nABSENT\ta/Added.java\n' % sha(os.path.join(bk, 'a/Changed.java')))
    write(post, '%s\ta/Changed.java\n%s\ta/Added.java\n'
          % (sha(os.path.join(src, 'a/Changed.java')), sha(os.path.join(src, 'a/Added.java'))))
    return src, bk, pre, post


def snapshot(root):
    out = {}
    for dirpath, _, files in os.walk(root):
        for f in files:
            p = os.path.join(dirpath, f)
            out[os.path.relpath(p, root)] = sha(p)
    return out


# ============================ 反例① 发布后被他人改动 ============================
with KeptDir('case') as root:
    src, bk, pre, post = build_case(root)
    # 别人在发布之后又改了这个文件 -> 当前哈希不再等于 post
    write(os.path.join(src, 'a/Changed.java'), 'SOMEONE-ELSE-EDITED-AFTER-DEPLOY\n')
    before = snapshot(src)
    res = run('rollback', '--root', src, '--pre', pre, '--post', post,
              '--backup', bk, '--trash', os.path.join(root, 'trash'), '--apply')
    after = snapshot(src)
    check('①-1 发布后被改动 整体拒绝', res.returncode != 0,
          'exit=%d' % res.returncode)
    check('①-2 拒绝原因是 POST_MISMATCH', 'POST_MISMATCH' in res.stderr)
    check('①-3 零改动（其余文件哈希全不变）', before == after,
          '差异=%s' % ([k for k in before if before.get(k) != after.get(k)] or '无'))
    check('①-4 未生成垃圾桶（没动过手）', not os.path.exists(os.path.join(root, 'trash')))

# ============================ 反例② 正常回退 ============================
with KeptDir('case') as root:
    src, bk, pre, post = build_case(root)
    legal_before = sha(os.path.join(src, 'legal/LegalController.java'))
    other_before = sha(os.path.join(src, 'assets/other-person.js'))
    trash = os.path.join(root, 'trash')
    res = run('rollback', '--root', src, '--pre', pre, '--post', post,
              '--backup', bk, '--trash', trash, '--apply')
    check('②-1 预检通过并执行', res.returncode == 0, res.stdout.strip().splitlines()[-1:] or '')
    with open(os.path.join(src, 'a/Changed.java'), encoding='utf-8') as fh:
        restored = fh.read().strip()
    check('②-2 旧文件恢复为发布前内容', restored == 'BEFORE-DEPLOY', restored)
    check('②-3 本次新增文件被归档而非删除',
          not os.path.exists(os.path.join(src, 'a/Added.java'))
          and os.path.exists(os.path.join(trash, 'a/Added.java')))
    check('②-4 法务哨兵未动',
          sha(os.path.join(src, 'legal/LegalController.java')) == legal_before)
    check('②-5 他人后加的资源未动',
          sha(os.path.join(src, 'assets/other-person.js')) == other_before)

# ============================ 反例③ 备份损坏 / 清单缺失 ============================
with KeptDir('case') as root:
    src, bk, pre, post = build_case(root)
    write(os.path.join(bk, 'a/Changed.java'), 'CORRUPTED-BACKUP\n')   # 备份被改坏
    before = snapshot(src)
    res = run('rollback', '--root', src, '--pre', pre, '--post', post,
              '--backup', bk, '--trash', os.path.join(root, 't'), '--apply')
    check('③-1 备份损坏 拒绝', res.returncode != 0, 'exit=%d' % res.returncode)
    check('③-2 拒绝原因是 BACKUP_CORRUPT', 'BACKUP_CORRUPT' in res.stderr)
    check('③-3 零改动', before == snapshot(src))

with KeptDir('case') as root:
    src, bk, pre, post = build_case(root)
    before = snapshot(src)
    res = run('rollback', '--root', src, '--pre', os.path.join(root, 'nope.manifest'),
              '--post', post, '--backup', bk, '--trash', os.path.join(root, 't'), '--apply')
    check('③-4 清单缺失 拒绝', res.returncode != 0 and 'MISSING_MANIFEST' in res.stderr)
    check('③-5 零改动', before == snapshot(src))

# ============================ 冻结路径不许进清单 ============================
with KeptDir('case') as root:
    src, bk, pre, post = build_case(root)
    bad = os.path.join(root, 'bad.manifest')
    # 摘要格式检查排在冻结检查之前，这里得给格式合法的摘要，
    # 否则拦下它的是 BAD_DIGEST，测不到冻结那条。
    write(bad, ('a' * 64) + chr(9) + 'legal/LegalController.java' + chr(10))
    res = run('verify', '--root', src, '--manifest', bad)
    check('④ 冻结路径进清单即拒绝',
          res.returncode != 0 and 'FROZEN_PATH_IN_MANIFEST' in res.stderr)

# ============================ 接入核实：脚本真的调用了它 ============================
deploy = open(os.path.join(RC, 'deploy-rc15.sh'), encoding='utf-8').read()
rollback = open(os.path.join(RC, 'rollback-rc15.sh'), encoding='utf-8').read()
check('⑤-1 deploy 调用 record', 'rc15_restore.py record' in deploy)
check('⑤-2 deploy 调用 verify（发布后自校验）', 'rc15_restore.py verify' in deploy)
check('⑤-3 deploy 有 v2 结构门槛', 'SCHEMA_GATE_BLOCKED' in deploy)
check('⑤-4 rollback 调用 rollback --apply',
      'rc15_restore.py rollback' in rollback and '--apply' in rollback)
check('⑤-5 rollback 不再整目录搬 assets',
      'mv "$D/$item"' not in rollback and 'cp -r \\"\\$W/dist/\\$item\\"' not in rollback)
check('⑤-6 前端也走同一实现',
      rollback.count('rc15_restore.py rollback') >= 4)

# ==================== r3 6 jar 被后来的编译替换 -> 整体拒绝，三方零改动 ====
with KeptDir('jar') as root:
    src, bk, pre, post = build_case(root)
    # jar 进受验清单：发布后 jar 与备份 jar 各记一份
    write(os.path.join(src, 'target/banquet-1.0.0.jar'), 'JAR-AFTER-DEPLOY' + chr(10))
    write(os.path.join(bk, 'target/banquet-1.0.0.jar'), 'JAR-BEFORE-DEPLOY' + chr(10))
    for mf, base in ((pre, bk), (post, src)):
        with open(mf, 'a', encoding='utf-8', newline=chr(10)) as fh:
            fh.write(sha(os.path.join(base, 'target/banquet-1.0.0.jar')) + chr(9)
                     + 'target/banquet-1.0.0.jar' + chr(10))
    # 法务后来重新编译，jar 被换掉
    write(os.path.join(src, 'target/banquet-1.0.0.jar'), 'JAR-REBUILT-BY-LEGAL-FIX' + chr(10))
    before = snapshot(src)
    res = run('rollback', '--root', src, '--pre', pre, '--post', post,
              '--backup', bk, '--trash', os.path.join(root, 'trash'), '--apply')
    check('6-1 jar 被后来替换 整体拒绝', res.returncode != 0 and 'POST_MISMATCH' in res.stderr)
    check('6-2 源码/jar/前端三方零改动', before == snapshot(src))

# ==================== r3 7 路径逃逸与摘要格式 ====================
with KeptDir('escape') as root:
    src, bk, pre, post = build_case(root)
    cases = ((('a' * 64) + chr(9) + '../../outside.java', 'PATH_ESCAPE_REJECTED'),
             (('a' * 64) + chr(9) + '/etc/passwd', 'ABSOLUTE_PATH_REJECTED'),
             ('nothex' + chr(9) + 'a/Changed.java', 'BAD_DIGEST'))
    for bad_line, tag in cases:
        m = os.path.join(root, 'bad-' + tag + '.manifest')
        write(m, bad_line + chr(10))
        r_ = run('verify', '--root', src, '--manifest', m)
        check('7 ' + tag, r_.returncode != 0 and tag in r_.stderr)

# ==================== r3 8 链接跳转 ====================
with KeptDir('link') as root:
    src, bk, pre, post = build_case(root)
    outside = os.path.join(root, 'outside')
    os.makedirs(outside, exist_ok=True)
    write(os.path.join(outside, 'Sneaky.java'), 'OUTSIDE' + chr(10))
    linked = True
    try:
        os.symlink(outside, os.path.join(src, 'linkdir'), target_is_directory=True)
    except (OSError, NotImplementedError, AttributeError):
        linked = False
    if linked:
        m = os.path.join(root, 'link.manifest')
        write(m, ('a' * 64) + chr(9) + 'linkdir/Sneaky.java' + chr(10))
        r_ = run('verify', '--root', src, '--manifest', m)
        check('8 链接跳转被拒', r_.returncode != 0 and 'SYMLINK_REJECTED' in r_.stderr)
    else:
        print('8 链接跳转被拒                                SKIP  本机无建链接权限，不计入通过')

# ==================== r3 9 垃圾桶碰撞不覆盖 ====================
with KeptDir('trash') as root:
    src, bk, pre, post = build_case(root)
    trash = os.path.join(root, 'trash')
    write(os.path.join(trash, 'a/Added.java'), 'EARLIER-ROLLBACK-EVIDENCE' + chr(10))
    keep = sha(os.path.join(trash, 'a/Added.java'))
    before = snapshot(src)
    res = run('rollback', '--root', src, '--pre', pre, '--post', post,
              '--backup', bk, '--trash', trash, '--apply')
    check('9-1 垃圾桶碰撞 拒绝', res.returncode != 0 and 'TRASH_COLLISION' in res.stderr)
    check('9-2 先前留存未被覆盖', sha(os.path.join(trash, 'a/Added.java')) == keep)
    check('9-3 零改动', before == snapshot(src))

print('-' * 78)
print('夹具保留在: ' + KEEP_ROOT)
print('PASS=%d FAIL=%d' % (len(PASS), len(FAIL)))
if FAIL:
    print('失败项: ' + ', '.join(FAIL))
sys.exit(1 if FAIL else 0)
