#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""RC15 发布/回退的清单与恢复算法（deploy 与 rollback 实际调用的那一份）。

用 Python 而不是继续写 bash：这段逻辑要在 ssh 双层引号里做逐文件哈希比对、
集合比对和"全部通过才动手"的两阶段提交。r1 我在 bash 里写，结果是脚本
只 source 了函数却没真调用，报告里却写成"已接入"。换成独立可执行模块，
脚本必须显式调用它才有效果，测试也能直接测同一份实现，没有"抄件"这回事。

只用标准库，不引新依赖。

子命令：
  record   记录白名单每个文件的哈希（不存在记 ABSENT）
  verify   校验当前状态与给定清单一致，任一不符非零退出
  rollback 两阶段回退：先整份预检，全过才逐条恢复；否则零改动退出

回退的硬规则：
  · 预检覆盖整份清单：当前文件必须等于 post，备份文件必须等于 pre；
  · 预检全过之前，一次 cp / mv 都不做；
  · pre 为 ABSENT 的（本次新增）只归档"当前哈希等于 post"的那些；
  · 清单里的路径必须是 root 下的相对路径，且解析后仍在 root 内；
  · 冻结路径（legal 目录、任何 Legal*.java、/case /case2）一律拒绝；
  · 归档目标已存在则拒绝覆盖——重复回退不能把上一次的现场证据抹掉。
"""
import argparse
import hashlib
import os
import re
import shutil
import sys

# 冻结匹配不能只列两个类名：法务下面还有别的 Legal*.java，
# 还有整个 legal 目录与前端的 /case /case2。任一命中即拒绝进清单。
FROZEN_DIR_PARTS = ('legal', 'case', 'case2')
ABSENT = 'ABSENT'
HEX64 = re.compile(r'^[0-9a-f]{64}$')


def die(msg, code=1):
    sys.stderr.write(msg + '\n')
    sys.exit(code)


def sha256_of(path):
    if not os.path.exists(path):
        return ABSENT
    if os.path.isdir(path):
        die('DIR_IN_MANIFEST 清单只接受文件，收到目录: %s' % path)
    h = hashlib.sha256()
    with open(path, 'rb') as fh:
        for chunk in iter(lambda: fh.read(65536), b''):
            h.update(chunk)
    return h.hexdigest()


def is_frozen(rel):
    parts = [x for x in rel.replace('\\', '/').split('/') if x]
    if any(p.lower() in FROZEN_DIR_PARTS for p in parts):
        return True
    base = parts[-1] if parts else ''
    return base.startswith('Legal') and base.endswith('.java')


def safe_rel(rel, label):
    """路径边界检查：清单里的路径只能是 root 下的相对路径。

    不做这一步的话，一行 "hash<TAB>../../etc/passwd" 或 "hash<TAB>/opt/别的东西"
    就能让回退把文件写到树外面去。清单是发布时生成的没错，但它落在磁盘上，
    回退时读的是磁盘上那一份——谁改过都算数，所以读的时候必须自己验。
    """
    raw = rel.replace('\\', '/')
    if not raw or raw != raw.strip():
        die('BAD_PATH %s 路径为空或首尾有空白: %r' % (label, rel))
    if raw.startswith('/'):
        die('ABSOLUTE_PATH_REJECTED %s 不接受绝对路径: %s' % (label, rel))
    if re.match(r'^[A-Za-z]:', raw):
        die('DRIVE_PATH_REJECTED %s 不接受盘符路径: %s' % (label, rel))
    if any(seg == '..' for seg in raw.split('/')):
        die('PATH_ESCAPE_REJECTED %s 路径含 ..: %s' % (label, rel))
    return raw


def resolve_within(root, rel, label):
    """解析后必须仍在 root 内，且路径上不许有软链接/junction 跳转。"""
    root_abs = os.path.realpath(root)
    target = os.path.join(root_abs, rel)
    probe = root_abs
    for seg in rel.split('/'):
        if not seg:
            continue
        probe = os.path.join(probe, seg)
        if os.path.islink(probe):
            die('SYMLINK_REJECTED %s 路径上有链接: %s' % (label, probe))
    parent = os.path.realpath(os.path.dirname(target))
    if parent != root_abs and not parent.startswith(root_abs + os.sep):
        die('OUT_OF_ROOT_REJECTED %s 解析后跑出了 root: %s -> %s' % (label, rel, parent))
    return target


def read_manifest(path):
    if not os.path.exists(path):
        die('MISSING_MANIFEST 清单不存在: %s' % path)
    rows = {}
    with open(path, encoding='utf-8') as fh:
        for line in fh:
            line = line.rstrip('\n')
            if not line.strip():
                continue
            if '\t' not in line:
                die('BAD_MANIFEST_LINE 格式必须是 <hash>\\t<path>: %r' % line)
            digest, rel = line.split('\t', 1)
            if digest != ABSENT and not HEX64.match(digest):
                die('BAD_DIGEST 摘要必须是 ABSENT 或 64 位小写 hex: %r' % digest)
            rel = safe_rel(rel, 'manifest')
            if rel in rows:
                die('DUP_PATH 清单里有重复路径: %s' % rel)
            if is_frozen(rel):
                die('FROZEN_PATH_IN_MANIFEST 冻结路径不许进清单: %s' % rel)
            rows[rel] = digest
    if not rows:
        die('EMPTY_MANIFEST 清单为空，按 fail-closed 拒绝: %s' % path)
    return rows


def cmd_record(args):
    if not os.path.exists(args.whitelist):
        die('MISSING_WHITELIST %s' % args.whitelist)
    rels = [l.strip() for l in open(args.whitelist, encoding='utf-8') if l.strip()]
    if not rels:
        die('EMPTY_WHITELIST 白名单为空，按 fail-closed 拒绝')
    lines = []
    for rel in rels:
        rel = safe_rel(rel, 'whitelist')
        if is_frozen(rel):
            die('FROZEN_PATH_IN_WHITELIST 冻结路径不许进白名单: %s' % rel)
        lines.append('%s\t%s' % (sha256_of(resolve_within(args.root, rel, 'whitelist')), rel))
    with open(args.out, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('\n'.join(lines) + '\n')
    print('RECORDED %d %s' % (len(lines), args.out))


def cmd_backup(args):
    """把清单里当前存在的文件逐个复制进备份树。

    这一步原来是在 SSH 双引号正文里写 while read 循环，变量被本地先展开，
    远端拿到的是空路径。挪到这里之后就没有引号层数问题了。
    """
    rows = read_manifest(args.manifest)
    n = 0
    for rel, digest in sorted(rows.items()):
        if digest == ABSENT:
            continue
        src = resolve_within(args.root, rel, "backup-src")
        dst = resolve_within(args.into, rel, "backup-dst")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
        n += 1
    print("BACKED_UP %d -> %s" % (n, args.into))


def cmd_verify(args):
    want = read_manifest(args.manifest)
    drift = []
    for rel, digest in sorted(want.items()):
        now = sha256_of(resolve_within(args.root, rel, 'verify'))
        if now != digest:
            drift.append((rel, digest, now))
    for rel, digest, now in drift:
        sys.stderr.write('DRIFT %s 期望=%s 实际=%s\n' % (rel, digest, now))
    if drift:
        die('DRIFT_DETECTED %d 处与清单不一致' % len(drift))
    print('VERIFY_OK %d' % len(want))


def cmd_rollback(args):
    """两阶段：先把整份清单查完，全过才动手。任何一条不过 -> 零改动退出。

    jar 也在清单里（r3 要求）：它原来是单独 cp 覆盖，不进预检。
    那意味着法务后来重新编译出的新 jar，会被这里的旧 jar 直接盖掉。
    """
    pre = read_manifest(args.pre)
    post = read_manifest(args.post)

    if set(pre) != set(post):
        only_pre = sorted(set(pre) - set(post))
        only_post = sorted(set(post) - set(pre))
        die('MANIFEST_SET_MISMATCH pre/post 路径集合不一致 仅pre=%s 仅post=%s'
            % (only_pre, only_post))

    problems = []
    plan_restore = []
    plan_archive = []

    for rel in sorted(post):
        target = resolve_within(args.root, rel, 'rollback')
        now = sha256_of(target)
        if now != post[rel]:
            problems.append('POST_MISMATCH %s 当前=%s 应为本次发布结果=%s（发布后被改过，拒绝回退）'
                            % (rel, now, post[rel]))
            continue
        if pre[rel] == ABSENT:
            plan_archive.append(rel)
            continue
        backup_src = resolve_within(args.backup, rel, 'backup')
        got = sha256_of(backup_src)
        if got == ABSENT:
            problems.append('BACKUP_MISSING %s 备份里没有这个文件' % rel)
        elif got != pre[rel]:
            problems.append('BACKUP_CORRUPT %s 备份哈希=%s 应为发布前=%s' % (rel, got, pre[rel]))
        else:
            plan_restore.append((rel, backup_src))

    # 归档碰撞也算预检的一部分：必须在任何 cp/mv 之前判掉
    for rel in plan_archive:
        if os.path.exists(os.path.join(os.path.realpath(args.trash), rel)):
            problems.append('TRASH_COLLISION %s 垃圾桶里已有同名归档，拒绝覆盖' % rel)

    if problems:
        for p in problems:
            sys.stderr.write(p + '\n')
        die('PRECHECK_FAILED %d 项未通过，零改动退出，交人工判断' % len(problems))

    print('PRECHECK_OK 待恢复=%d 待归档=%d' % (len(plan_restore), len(plan_archive)))
    for rel, _ in plan_restore:
        print('  will-restore %s' % rel)
    for rel in plan_archive:
        print('  will-archive %s' % rel)

    if not args.apply:
        print('DRY_RUN 未做任何改动（加 --apply 才执行）')
        return

    os.makedirs(args.trash, exist_ok=True)
    for rel in plan_archive:
        dst = resolve_within(args.trash, rel, 'trash')
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.move(os.path.join(os.path.realpath(args.root), rel), dst)
        print('archived %s' % rel)
    for rel, src in plan_restore:
        dst = resolve_within(args.root, rel, 'restore')
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
        print('restored %s' % rel)
    print('APPLIED restore=%d archive=%d' % (len(plan_restore), len(plan_archive)))


def main():
    ap = argparse.ArgumentParser(description='RC15 清单与回退算法')
    sub = ap.add_subparsers(dest='cmd', required=True)

    p = sub.add_parser('record'); p.set_defaults(fn=cmd_record)
    p.add_argument('--root', required=True); p.add_argument('--whitelist', required=True)
    p.add_argument('--out', required=True)

    p = sub.add_parser('verify'); p.set_defaults(fn=cmd_verify)
    p.add_argument('--root', required=True); p.add_argument('--manifest', required=True)

    p = sub.add_parser('backup'); p.set_defaults(fn=cmd_backup)
    p.add_argument('--root', required=True); p.add_argument('--manifest', required=True)
    p.add_argument('--into', required=True)

    p = sub.add_parser('rollback'); p.set_defaults(fn=cmd_rollback)
    p.add_argument('--root', required=True); p.add_argument('--pre', required=True)
    p.add_argument('--post', required=True); p.add_argument('--backup', required=True)
    p.add_argument('--trash', required=True); p.add_argument('--apply', action='store_true')

    args = ap.parse_args()
    args.fn(args)


if __name__ == '__main__':
    main()
