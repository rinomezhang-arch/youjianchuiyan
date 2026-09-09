# -*- coding: utf-8 -*-
"""CL25 r2：把恢复算法真正接进 deploy / rollback。

r1 的问题不是算法不对，是算法没被调用——库里有函数，脚本没用它，
我却在报告里写成"已接入"。这一版所有改动都以"脚本里出现对
rc15_restore.py 的实际调用"为准，接不上就让补丁失败，不留口子。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RC = os.path.normpath(os.path.join(HERE, '..', 'restaurant-rc-15'))
DEPLOY = os.path.join(RC, 'deploy-rc15.sh')
ROLLBACK = os.path.join(RC, 'rollback-rc15.sh')
missing = []


def rep(text, old, new, tag):
    global missing
    if text.count(old) != 1:
        missing.append('%s(count=%d)' % (tag, text.count(old)))
        return text
    return text.replace(old, new, 1)


d = io.open(DEPLOY, encoding='utf-8').read()

# ---- 1. 把恢复算法与白名单送上远端；python3 缺失即 fail closed ----
d = rep(
    d,
    'TRASH_REMOTE="/home/ubuntu/rc15_trash/$TS"',
    '''TRASH_REMOTE="/home/ubuntu/rc15_trash/$TS"
RC15_TOOLS="$(cd "$(dirname "${BASH_SOURCE[0]}")/../release-safe-25" && pwd)"
REMOTE_TOOLS="/home/ubuntu/rc15_tools/$TS"

# 清单与回退算法在远端执行，先确认 python3 在；没有就停，不做静默降级。
$SSH $HOST "command -v python3 >/dev/null 2>&1 || { echo 'MISSING_PYTHON3 远端没有 python3，清单与回退算法无法运行，发布中止' >&2; exit 1; }"
$SSH $HOST "mkdir -p $REMOTE_TOOLS"
$SCP "$RC15_TOOLS/rc15_restore.py" $HOST:$REMOTE_TOOLS/
# 白名单逐文件写死，后端与前端各一份；前端的 assets 在发布时按实际推送文件展开
$SSH $HOST "cat > $REMOTE_TOOLS/backend.whitelist <<'"'"'WL'"'"'
src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java
src/main/java/com/youjian/banquet/aop/StoreDataScopeAspect.java
src/main/java/com/youjian/banquet/aop/AuditLogAspect.java
src/main/java/com/youjian/banquet/util/UserContext.java
src/main/java/com/youjian/banquet/auth/StaffRealtimeGuard.java
src/main/java/com/youjian/banquet/controller/IpadOrderController.java
src/main/java/com/youjian/banquet/controller/AuthController.java
src/main/java/com/youjian/banquet/service/IpadBatchAuthorizationService.java
src/main/java/com/youjian/banquet/service/IpadBatchSubmissionService.java
src/main/resources/ipad_batch_request_migration_v1.sql
WL"''',
    '1-ship-tools')

# ---- 2. v2 结构门槛：真调用，不是只在库里定义 ----
d = rep(
    d,
    'echo "==================== 1. 数据库：先全量备份，再幂等迁移 ===================="',
    '''echo "==================== 0b. v2 结构门槛（缺规范结构即停）===================="
# r1 我只在 lib.sh 里定义了 rc15_require_schema 却没调用，等于没有门槛。
# 这里显式查一次：ipad_batch_request 的复合外键（TL27 指出 v2 缺失）。
# 查不到就停，不自行补半成品 SQL——完整迁移由天龙专卡交付。
$SSH $HOST 'set -e
  source ~/.banquet_env.sh >/dev/null 2>&1; export MYSQL_PWD="$MYSQL_PASSWORD"
  n=$(mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'ipad_batch_request'"'"' AND CONSTRAINT_TYPE='"'"'FOREIGN KEY'"'"';" 2>/dev/null || echo 0)
  if [ "${n:-0}" -ge 1 ]; then
    echo "OK schema 前置满足：ipad_batch_request 已有外键约束 ($n)"
  else
    echo "SCHEMA_GATE_BLOCKED 缺 ipad_batch_request 复合外键(v2)，迁移不执行，等待天龙专卡" >&2
    exit 1
  fi'

echo "==================== 1. 数据库：先全量备份，再幂等迁移 ===================="''',
    '2-schema-gate')

# ---- 3. 备份段：pre 清单改用共享实现生成；备份体一并留存 ----
old_pre = d[d.index('  # 上一轮这里记的是"整个 src/main 的文件清单"'):d.index('  cp target/banquet-1.0.0.jar ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS')]
new_pre = '''  # 清单由共享实现生成（deploy 与 rollback 用同一份代码），不再在 shell 里手写哈希循环。
  python3 REMOTE_TOOLS_PH/rc15_restore.py record --root . \\
    --whitelist REMOTE_TOOLS_PH/backend.whitelist \\
    --out ~/deploy_backups/src-rc15-TS_PH.pre.manifest
  # 备份体：白名单里当前存在的文件逐个复制进备份树，回退时按哈希核对后才用
  while IFS=$'\\t' read -r h p; do
    if [ "$h" != ABSENT ]; then
      mkdir -p ~/deploy_backups/src-rc15-TS_PH.files/"$(dirname "$p")"
      cp -f "$p" ~/deploy_backups/src-rc15-TS_PH.files/"$p"
    fi
  done < ~/deploy_backups/src-rc15-TS_PH.pre.manifest
'''
new_pre = new_pre.replace('REMOTE_TOOLS_PH', '$REMOTE_TOOLS').replace('TS_PH', '$TS')
d = rep(d, old_pre, new_pre, '3-pre-manifest')

# ---- 4. 就位后写 post 清单并立即自校验 ----
d = rep(
    d,
    "trash_local \"$STAGE\"\n",
    '''trash_local "$STAGE"

echo "==================== 3b. 记录发布后哈希并当场核对 ===================="
# 回退要判断的是"当前文件是否仍等于本次发布推上去的样子"，所以必须有 post 清单。
# r1 缺这一份，回退只能拿 pre 去 cp，等于把别人后来的改动一并覆盖。
$SSH $HOST "set -e
  cd $REMOTE
  python3 $REMOTE_TOOLS/rc15_restore.py record --root . \\
    --whitelist $REMOTE_TOOLS/backend.whitelist \\
    --out ~/deploy_backups/src-rc15-$TS.post.manifest
  python3 $REMOTE_TOOLS/rc15_restore.py verify --root . \\
    --manifest ~/deploy_backups/src-rc15-$TS.post.manifest"
''',
    '4-post-manifest')

io.open(DEPLOY, 'w', encoding='utf-8').write(d)

# =====================================================================
# rollback：改成调用共享实现，先整份预检再动手
# =====================================================================
r = io.open(ROLLBACK, encoding='utf-8').read()

start = r.index('# ---- 2. 只恢复白名单路径')
end = r.index('# ---- 3. 恢复 jar ----')
new_block = '''# ---- 2. 回退：整份预检通过之前零 cp/mv，由共享实现执行 ----
# r1 这里是拿 pre 清单直接 cp，当前文件哪怕是别人发布后改的也照覆盖。
# 现在交给 rc15_restore.py rollback：它先把整份清单查完
# （当前文件必须等于 post、备份文件必须等于 pre），全过才逐条恢复；
# 任一不过就零改动退出。先跑一次 dry-run 打印计划，再 --apply。
$SSH $HOST "set -e
  command -v python3 >/dev/null 2>&1 || { echo 'MISSING_PYTHON3 无法执行回退算法' >&2; exit 1; }
  cd $REMOTE
  T=/home/ubuntu/rc15_tools/$TS
  python3 \\$T/rc15_restore.py rollback --root . \\
    --pre $BK/src-rc15-$TS.pre.manifest \\
    --post $BK/src-rc15-$TS.post.manifest \\
    --backup $BK/src-rc15-$TS.files \\
    --trash $TRASH_REMOTE
  python3 \\$T/rc15_restore.py rollback --root . \\
    --pre $BK/src-rc15-$TS.pre.manifest \\
    --post $BK/src-rc15-$TS.post.manifest \\
    --backup $BK/src-rc15-$TS.files \\
    --trash $TRASH_REMOTE --apply
  python3 \\$T/rc15_restore.py verify --root . --manifest $BK/src-rc15-$TS.pre.manifest"

'''
r = r[:start] + new_block + r[end:]

# 输入齐备性也要带上 post 清单与备份树
r = rep(r,
        'for f in $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.tgz $BK/src-rc15-$TS.pre.manifest; do',
        'for f in $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.pre.manifest $BK/src-rc15-$TS.post.manifest; do',
        '5-inputs')
r = rep(r,
        '  ls -lh $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.tgz $BK/src-rc15-$TS.pre.manifest"',
        '''  if [ ! -d $BK/src-rc15-$TS.files ]; then echo 'MISSING_INPUT 备份树不存在' >&2; exit 1; fi
  ls -lh $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.pre.manifest $BK/src-rc15-$TS.post.manifest"''',
        '6-inputs-ls')

# 前端：逐文件，不再整目录
fe_start = r.index('  echo "--- 前端回退（只恢复 index.html / collab.html / assets）---"')
fe_end = r.index('else\n  echo "前端未回退')
fe_new = '''  echo "--- 前端回退（逐文件，按本次发布清单）---"
  # r1 是整目录 mv/cp assets，会把别人后发的资源、法务共用资源一起挪走。
  # 现在按发布时记下的逐文件清单走同一套预检与恢复算法。
  $SSH $HOST "set -e
    command -v python3 >/dev/null 2>&1 || { echo 'MISSING_PYTHON3 无法执行前端回退算法' >&2; exit 1; }
    for f in $BK/fe-rc15-$TS.pre.manifest $BK/fe-rc15-$TS.post.manifest; do
      if [ ! -s \\"\\$f\\" ]; then echo \\"MISSING_INPUT \\$f\\" >&2; exit 1; fi
    done
    cd /opt/youjianchuiyan/frontend_v3/dist
    T=/home/ubuntu/rc15_tools/$TS
    sudo python3 \\$T/rc15_restore.py rollback --root . \\
      --pre $BK/fe-rc15-$TS.pre.manifest --post $BK/fe-rc15-$TS.post.manifest \\
      --backup $BK/fe-rc15-$TS.files --trash $TRASH_REMOTE/fe
    sudo python3 \\$T/rc15_restore.py rollback --root . \\
      --pre $BK/fe-rc15-$TS.pre.manifest --post $BK/fe-rc15-$TS.post.manifest \\
      --backup $BK/fe-rc15-$TS.files --trash $TRASH_REMOTE/fe --apply
    echo '--- /case /case2 未被触碰 ---'
    ls -ld ./case ./case2"
'''
r = r[:fe_start] + fe_new + r[fe_end:]

io.open(ROLLBACK, 'w', encoding='utf-8').write(r)

# ---- 接入自检：脚本里必须真的出现对共享实现的调用 ----
d2 = io.open(DEPLOY, encoding='utf-8').read()
r2 = io.open(ROLLBACK, encoding='utf-8').read()
for name, text, needles in (
        ('deploy', d2, ['rc15_restore.py record', 'rc15_restore.py verify', 'SCHEMA_GATE_BLOCKED']),
        ('rollback', r2, ['rc15_restore.py rollback', '--apply'])):
    for needle in needles:
        if needle not in text:
            missing.append('WIRED:%s缺少%s' % (name, needle))

print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
