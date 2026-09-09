# -*- coding: utf-8 -*-
"""按端到端实跑暴露出来的问题修 deploy，并顺手把白名单从 heredoc 改成独立文件。

实跑第一次就打出来的症状：
  bash: line 13: warning: here-document at line 1 delimited by end-of-file (wanted "WL")

原因是白名单用 heredoc 写在 SSH 的双引号正文里，中间还要靠 '"'"' 来回切引号。
这种写法在本地展开一层、远端再展开一层，delimiter 根本对不上，
结果白名单文件压根没生成——而后面所有 record 都依赖它。

修法不是继续调引号，是把这类内容彻底移出 shell 引号：白名单落成独立文件，scp 上去。
同一原因导致的其它几处（备份段 $h/$p 未转义、ls 指向过时的清单名）一并按同样思路改：
凡是要在远端逐行处理清单的，一律交给已经上传的 Python 模块，不在 shell 里写循环。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RC = os.path.normpath(os.path.join(HERE, '..', 'restaurant-rc-15'))
DEPLOY = os.path.join(RC, 'deploy-rc15.sh')
missing = []

WHITELIST = [
    'src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java',
    'src/main/java/com/youjian/banquet/aop/StoreDataScopeAspect.java',
    'src/main/java/com/youjian/banquet/aop/AuditLogAspect.java',
    'src/main/java/com/youjian/banquet/util/UserContext.java',
    'src/main/java/com/youjian/banquet/auth/StaffRealtimeGuard.java',
    'src/main/java/com/youjian/banquet/controller/IpadOrderController.java',
    'src/main/java/com/youjian/banquet/controller/AuthController.java',
    'src/main/java/com/youjian/banquet/service/IpadBatchAuthorizationService.java',
    'src/main/java/com/youjian/banquet/service/IpadBatchSubmissionService.java',
    'src/main/resources/ipad_batch_request_migration_v1.sql',
    'target/banquet-1.0.0.jar',
]
NL = chr(10)
with io.open(os.path.join(HERE, 'backend.whitelist'), 'w', encoding='utf-8', newline=NL) as fh:
    fh.write(NL.join(WHITELIST) + NL)


def rep(text, old, new, tag):
    global missing
    if text.count(old) != 1:
        missing.append(tag + '(count=%d)' % text.count(old))
        return text
    return text.replace(old, new, 1)


d = io.open(DEPLOY, encoding='utf-8').read()

# ---- 1. 白名单：heredoc 改 scp ----
start = d.index('# 白名单逐文件写死')
end = d.index('WL"', start) + len('WL"')
d = (d[:start]
     + '# 白名单改成独立文件 scp 上去。原来用 heredoc 写在 SSH 双引号正文里，' + NL
     + '# 中间靠 \'"\'"\' 来回切引号，本地展开一层远端再展开一层，delimiter 对不上，' + NL
     + '# 实跑报 "here-document delimited by end-of-file"，白名单压根没生成。' + NL
     + '$SCP "$RC15_TOOLS/backend.whitelist" $HOST:$REMOTE_TOOLS/'
     + d[end:])

# ---- 2. 备份体：不在 shell 里逐行读清单，交给已上传的模块 ----
old_loop_start = d.index('  # 备份体：白名单里当前存在的文件逐个复制进备份树')
old_loop_end = d.index('done < ~/deploy_backups/src-rc15-$TS.pre.manifest', old_loop_start) \
    + len('done < ~/deploy_backups/src-rc15-$TS.pre.manifest')
d = (d[:old_loop_start]
     + '  # 备份体：原来在 SSH 双引号正文里写 while read 循环，$h/$p/$(dirname "$p")' + NL
     + '  # 会被本地先展开，远端拿到的是空值。凡是要逐行处理清单的都交给 Python 模块，' + NL
     + '  # 不在 shell 引号里写循环——这类转义问题静态看不出来，只有实跑才现形。' + NL
     + '  python3 $REMOTE_TOOLS/rc15_restore.py backup --root . \\' + NL
     + '    --manifest ~/deploy_backups/src-rc15-$TS.pre.manifest \\' + NL
     + '    --into ~/deploy_backups/src-rc15-$TS.files'
     + d[old_loop_end:])

# ---- 3. ls 指向过时的清单名 ----
d = d.replace('~/deploy_backups/src-rc15-$TS.manifest', '~/deploy_backups/src-rc15-$TS.pre.manifest')

# ---- 4. jar 的 post 必须在构建成功之后记 ----
# 原来 post 记在 3b（就位后）而构建在第 4 步，jar 记的是旧的，正常回退必然 POST_MISMATCH。
old_post = d[d.index('echo "==================== 3b. 记录发布后哈希并当场核对 ===================="'):]
old_post = old_post[:old_post.index('"') + old_post[old_post.index('"'):].index('\n\n')]
post_block_start = d.index('echo "==================== 3b. 记录发布后哈希并当场核对 ====================')
post_block_end = d.index('echo "==================== 4.', post_block_start)
post_block = d[post_block_start:post_block_end]
d = d[:post_block_start] + d[post_block_end:]

anchor4 = d.index('echo "==================== 5. 校验新代码进 jar')
d = (d[:anchor4]
     + '# post 清单必须记在构建之后。原来它排在第 4 步构建之前，' + NL
     + '# 而 target/banquet-1.0.0.jar 已经进了受验清单——记下的是构建前的旧 jar，' + NL
     + '# 回退时当前 jar（构建产物）与 post 永远对不上，正常回退会被自己的预检拒掉。' + NL
     + post_block.replace('3b. 记录发布后哈希并当场核对', '4b. 记录发布后哈希并当场核对（构建之后）')
     + d[anchor4:])

io.open(DEPLOY, 'w', encoding='utf-8').write(d)

# ---- 5. 给 rc15_restore.py 加 backup 子命令（上面第 2 步要用）----
TOOL = os.path.join(HERE, 'rc15_restore.py')
t = io.open(TOOL, encoding='utf-8').read()
if 'def cmd_backup' not in t:
    t = t.replace(
        'def cmd_verify(args):',
        'def cmd_backup(args):' + NL
        + '    """把清单里当前存在的文件逐个复制进备份树。' + NL
        + NL
        + '    这一步原来是在 SSH 双引号正文里写 while read 循环，变量被本地先展开，' + NL
        + '    远端拿到的是空路径。挪到这里之后就没有引号层数问题了。' + NL
        + '    """' + NL
        + '    rows = read_manifest(args.manifest)' + NL
        + '    n = 0' + NL
        + '    for rel, digest in sorted(rows.items()):' + NL
        + '        if digest == ABSENT:' + NL
        + '            continue' + NL
        + '        src = resolve_within(args.root, rel, "backup-src")' + NL
        + '        dst = resolve_within(args.into, rel, "backup-dst")' + NL
        + '        os.makedirs(os.path.dirname(dst), exist_ok=True)' + NL
        + '        shutil.copyfile(src, dst)' + NL
        + '        n += 1' + NL
        + '    print("BACKED_UP %d -> %s" % (n, args.into))' + NL
        + NL
        + NL
        + 'def cmd_verify(args):')
    t = t.replace(
        "    p = sub.add_parser('rollback'); p.set_defaults(fn=cmd_rollback)",
        "    p = sub.add_parser('backup'); p.set_defaults(fn=cmd_backup)" + NL
        + "    p.add_argument('--root', required=True); p.add_argument('--manifest', required=True)" + NL
        + "    p.add_argument('--into', required=True)" + NL
        + NL
        + "    p = sub.add_parser('rollback'); p.set_defaults(fn=cmd_rollback)")
    io.open(TOOL, 'w', encoding='utf-8').write(t)

print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
