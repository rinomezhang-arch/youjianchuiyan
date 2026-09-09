# -*- coding: utf-8 -*-
"""CL-RC15-RELEASE-SAFE-25：给 RC15 的发布/回退脚本打五处安全补丁。

单独放成脚本而不是内联命令，是因为要改的内容里同时有 shell 的单引号、双引号、
反斜杠和 $ 展开，走命令行拼接一定会在某一层被吃掉。这里直接读写文件，不经过 shell。

只改 deploy-rc15.sh 与 rollback-rc15.sh 两个文件，不动其他任何东西。
"""
import io
import os
import sys

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'restaurant-rc-15')
DEPLOY = os.path.normpath(os.path.join(BASE, 'deploy-rc15.sh'))
ROLLBACK = os.path.normpath(os.path.join(BASE, 'rollback-rc15.sh'))

missing = []


def rep(text, old, new, tag):
    global missing
    if text.count(old) != 1:
        missing.append('%s(count=%d)' % (tag, text.count(old)))
        return text
    return text.replace(old, new, 1)


d = io.open(DEPLOY, encoding='utf-8').read()

# ---------- A. 回收站：删除一律留痕 ----------
d = rep(
    d,
    'SCP="scp -i $KEY -o IdentitiesOnly=yes"',
    'SCP="scp -i $KEY -o IdentitiesOnly=yes"\n'
    '\n'
    '# ---- 删除一律进回收站，不用 rm ----\n'
    '# 原脚本三处 rm 直接抹掉暂存目录和已执行的 SQL 副本。发布出问题时，\n'
    '# 那些暂存件正是唯一能看出"到底推了什么上去"的证据，删掉之后只能靠回忆。\n'
    '# 统一改成移进带时间戳的回收站，留痕，需要时人工清理。\n'
    'TRASH_LOCAL="${TMPDIR:-/tmp}/rc15_trash/$TS"\n'
    'TRASH_REMOTE="/home/ubuntu/rc15_trash/$TS"\n'
    'trash_local() {\n'
    '  mkdir -p "$TRASH_LOCAL"\n'
    '  for p in "$@"; do\n'
    '    if [ -e "$p" ]; then mv -f "$p" "$TRASH_LOCAL"/; fi\n'
    '  done\n'
    '  echo "已移入本地回收站 $TRASH_LOCAL: $*"\n'
    '}\n',
    'A-trash')

# ---------- B. 构建吞错 ----------
d = rep(
    d,
    '$SSH $HOST "cd $REMOTE && mvn -q -DskipTests package 2>&1 | tail -20 && ls -l target/banquet-1.0.0.jar"',
    '# 原写法是 mvn ... 2>&1 | tail -20。管道的退出码是最后一个命令 tail 的，永远是 0，\n'
    '# 所以编译失败也照样往下走，直到后面以别的形式炸出来才被发现。\n'
    '# 远端 shell 没开 pipefail，靠管道解决不了，这里改成不用管道：\n'
    '# 输出落盘，用 mvn 自己的退出码判断，失败立刻中止发布。\n'
    '$SSH $HOST "cd $REMOTE\n'
    '  set -e\n'
    '  if mvn -DskipTests package > /tmp/mvn-rc15-$TS.log 2>&1; then\n'
    '    tail -20 /tmp/mvn-rc15-$TS.log\n'
    '  else\n'
    "    echo 'BUILD_FAILED 编译失败，发布中止。完整日志见 /tmp/mvn-rc15-$TS.log，末尾如下：'\n"
    '    tail -60 /tmp/mvn-rc15-$TS.log\n'
    '    exit 1\n'
    '  fi\n'
    '  ls -l target/banquet-1.0.0.jar"',
    'B-build')

# ---------- C. 健康检查超时不停 ----------
HEALTH_OLD = (
    '''$SSH $HOST 'for i in $(seq 1 45); do c=$(curl -s -o /dev/null -w "%{http_code}" '''
    '''http://127.0.0.1:8080/api/legal/me); [ "$c" = "401" ] && '''
    '''{ echo "后端起来了（法务入口 401 符合预期）"; break; }; sleep 2; done\'''')
HEALTH_NEW = (
    '# 原循环轮询 45 次后没有失败分支：一直起不来也只是循环自然结束，脚本继续做前端发布，\n'
    '# 于是"后端挂着、前端已经换成新版"这种最难收拾的状态就出现了。改成超时即失败。\n'
    """$SSH $HOST 'ok=0\n"""
    '  for i in $(seq 1 45); do\n'
    '    c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me || true)\n'
    '    if [ "$c" = "401" ]; then ok=1; echo "后端起来了（法务入口 401 符合预期）"; break; fi\n'
    '    sleep 2\n'
    '  done\n'
    '  if [ "$ok" != "1" ]; then echo "HEALTH_TIMEOUT 后端 90 秒内没起来，发布中止，前端不做切换"; exit 1; fi\'')
d = rep(d, HEALTH_OLD, HEALTH_NEW, 'C-health')

# ---------- D. 备份时同时记录源码清单 ----------
D_OLD = ('$SSH $HOST "mkdir -p ~/deploy_backups && cd $REMOTE && '
         'tar czf ~/deploy_backups/src-rc15-$TS.tgz src/main && '
         'cp target/banquet-1.0.0.jar ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS && '
         'ls -lh ~/deploy_backups/src-rc15-$TS.tgz ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS"')
D_NEW = (
    '# 除了 tar，另存一份发布前的文件清单。原因：回退时解 tar 只会覆盖和补齐，\n'
    '# 不会删除本次发布"新增"的源码文件（例如 auth/StaffRealtimeGuard.java）。\n'
    '# 没有清单就无从判断哪些是新增的，回退后这些文件留在盘上，下次构建又被编进 jar，\n'
    '# 回退等于没退干净。\n'
    '$SSH $HOST "set -e\n'
    '  mkdir -p ~/deploy_backups\n'
    '  cd $REMOTE\n'
    '  tar czf ~/deploy_backups/src-rc15-$TS.tgz src/main\n'
    '  find src/main -type f | LC_ALL=C sort > ~/deploy_backups/src-rc15-$TS.manifest\n'
    '  cp target/banquet-1.0.0.jar ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS\n'
    '  ls -lh ~/deploy_backups/src-rc15-$TS.tgz ~/deploy_backups/src-rc15-$TS.manifest '
    '~/deploy_backups/banquet-1.0.0.jar.rc15-$TS"')
d = rep(d, D_OLD, D_NEW, 'D-manifest')

# ---------- E. 本地暂存目录进回收站 ----------
d = rep(d, 'rm -rf "$STAGE"\n', 'trash_local "$STAGE"\n', 'E-stage')

# ---------- F. 前端覆盖前整包备份 ----------
F_OLD = '$SSH $HOST "sudo cp -r /tmp/fe_rc15_$TS/assets/. /opt/youjianchuiyan/frontend_v3/dist/assets/ \\'
F_NEW = (
    '# 覆盖前先整包备份 dist。原脚本直接 sudo cp 覆盖，一旦新前端有问题，\n'
    '# 回退脚本里只能写一句"如无备份，旧前端仍可工作"——那是把风险留给未来的人。\n'
    '$SSH $HOST "set -e\n'
    '  sudo mkdir -p /home/ubuntu/deploy_backups\n'
    '  sudo tar czf /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz -C /opt/youjianchuiyan/frontend_v3 dist\n'
    '  sudo chown ubuntu:ubuntu /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz\n'
    '  ls -lh /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz"\n'
    + F_OLD)
d = rep(d, F_OLD, F_NEW, 'F-febackup')

# ---------- G. 远端三处 rm 改为回收站 ----------
d = rep(d, '  && rm -rf /tmp/fe_rc15_$TS \\',
        '  && mkdir -p $TRASH_REMOTE && mv -f /tmp/fe_rc15_$TS $TRASH_REMOTE/ \\', 'G-fe-trash')
d = rep(d, "  rm -f /tmp/payroll_approval_payout_v1.rc15-'\"$TS\"'.sql",
        "  mkdir -p '\"$TRASH_REMOTE\"' && mv -f /tmp/payroll_approval_payout_v1.rc15-'\"$TS\"'.sql "
        "'\"$TRASH_REMOTE\"'/", 'G-sql-payroll')
d = rep(d, "  rm -f /tmp/restaurant_print_config_v1.rc15-'\"$TS\"'.sql",
        "  mkdir -p '\"$TRASH_REMOTE\"' && mv -f /tmp/restaurant_print_config_v1.rc15-'\"$TS\"'.sql "
        "'\"$TRASH_REMOTE\"'/", 'G-sql-print')

io.open(DEPLOY, 'w', encoding='utf-8').write(d)

# ================= rollback-rc15.sh =================
r = io.open(ROLLBACK, encoding='utf-8').read()

# ---------- H. 回退时清掉本次新增的源码文件（移入回收站，不删）----------
R_OLD = ("  echo '--- 1. 恢复源码树 ---'\n"
         '  cd $REMOTE\n'
         '  tar xzf ~/deploy_backups/src-rc15-$TS.tgz -C .\n')
R_NEW = (
    "  echo '--- 1. 恢复源码树 ---'\n"
    '  cd $REMOTE\n'
    '  tar xzf ~/deploy_backups/src-rc15-$TS.tgz -C .\n'
    '  # 解 tar 只覆盖和补齐，不会删掉本次发布新增的文件。\n'
    '  # 拿发布前的清单比一遍，多出来的移进回收站——不 rm，留痕以便复核。\n'
    '  if [ -f ~/deploy_backups/src-rc15-$TS.manifest ]; then\n'
    '    TRASH=/home/ubuntu/rc15_trash/rollback-$TS\n'
    '    mkdir -p \\$TRASH\n'
    '    find src/main -type f | LC_ALL=C sort > /tmp/src-now-$TS.manifest\n'
    '    ADDED=\\$(comm -13 ~/deploy_backups/src-rc15-$TS.manifest /tmp/src-now-$TS.manifest || true)\n'
    '    if [ -n \\"\\$ADDED\\" ]; then\n'
    '      echo \\"回退：本次发布新增的源码文件将移入 \\$TRASH\\"\n'
    '      echo \\"\\$ADDED\\" | while read -r f; do\n'
    '        [ -n \\"\\$f\\" ] || continue\n'
    '        mkdir -p \\"\\$TRASH/\\$(dirname \\$f)\\"\n'
    '        mv -f \\"\\$f\\" \\"\\$TRASH/\\$f\\"\n'
    '        echo \\"  moved \\$f\\"\n'
    '      done\n'
    '    else\n'
    '      echo \\"回退：没有需要清理的新增源码文件\\"\n'
    '    fi\n'
    '  else\n'
    '    echo \\"警告：找不到 src-rc15-$TS.manifest，无法判断新增文件，回退可能不彻底\\"\n'
    '  fi\n')
r = rep(r, R_OLD, R_NEW, 'H-rollback-added')

# ---------- I. 回退的健康检查同样要能失败 ----------
RH_OLD = ('''$SSH $HOST 'for i in $(seq 1 45); do c=$(curl -s -o /dev/null -w "%{http_code}" '''
          '''http://127.0.0.1:8080/api/legal/me); [ "$c" = "401" ] && '''
          '''{ echo "后端已恢复（法务入口 401）"; break; }; sleep 2; done''')
RH_NEW = ('# 回退的健康检查同样不能只是循环完就算数——回退没起来必须让人知道。\n'
          """$SSH $HOST 'ok=0\n"""
          '  for i in $(seq 1 45); do\n'
          '    c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me || true)\n'
          '    if [ "$c" = "401" ]; then ok=1; echo "后端已恢复（法务入口 401）"; break; fi\n'
          '    sleep 2\n'
          '  done\n'
          '  if [ "$ok" != "1" ]; then echo "ROLLBACK_HEALTH_TIMEOUT 回退后后端 90 秒未恢复，需人工介入"; exit 1; fi')
r = rep(r, RH_OLD, RH_NEW, 'I-rollback-health')

# ---------- J. 前端回退：现在有备份了，写成可执行步骤 ----------
J_OLD = ('echo "如需前端回退：用发布前 dist 备份恢复 index.html/collab.html（旧 assets 为哈希文件名仍在）。"')
J_NEW = (
    '# 发布脚本现在会在覆盖前整包备份 dist，所以前端回退不再是"没有备份、只能将就"。\n'
    'if [ "${ROLLBACK_FRONTEND:-0}" = "1" ]; then\n'
    '  echo "--- 5. 前端回退（ROLLBACK_FRONTEND=1）---"\n'
    '  $SSH $HOST "set -e\n'
    '    ls -lh /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz\n'
    '    TRASH=/home/ubuntu/rc15_trash/rollback-$TS\n'
    '    sudo mkdir -p \\$TRASH\n'
    '    sudo mv /opt/youjianchuiyan/frontend_v3/dist \\$TRASH/dist-before-rollback\n'
    '    sudo tar xzf /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz -C /opt/youjianchuiyan/frontend_v3\n'
    '    sudo chown -R www-data:www-data /opt/youjianchuiyan/frontend_v3/dist\n'
    '    ls -ld /opt/youjianchuiyan/frontend_v3/dist/case /opt/youjianchuiyan/frontend_v3/dist/case2"\n'
    'else\n'
    '  echo "前端未回退。如需回退：ROLLBACK_FRONTEND=1 bash $0 $TS"\n'
    '  echo "（发布时的整包备份在 /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz）"\n'
    'fi')
r = rep(r, J_OLD, J_NEW, 'J-frontend-rollback')

io.open(ROLLBACK, 'w', encoding='utf-8').write(r)

print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
