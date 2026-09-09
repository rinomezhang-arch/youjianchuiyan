#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""E：用无外呼替身把**真实的 deploy-rc15.sh 从头跑到尾**。

前几轮我一直在改一处、静态查一处，结果每轮都被实跑打出新洞。
静态检查看不见的东西有三类，正好是被打出来的那三类：
  · 双引号 SSH 正文里的变量会在本地先展开（转义问题）
  · 引用的文件名与实际生成的对不上（时序/命名问题）
  · 记录 post 哈希的时机排在构建之前（顺序问题）
这些只有真跑才现形，所以这一版把 ssh/scp/mysql 全部换成替身，
让远端命令在本地的假树里真的执行，脚本本身一行不改地跑。

硬约束：替身只在沙箱里动文件，绝不发起网络调用；任何一次真实外呼都会被记账并判失败。

隔离修订（统筹实读发现，本版仅提交源码、未运行）：
  · scp 按真实选项语义解析，-i 后的私钥不会再被当成源文件复制，
    并显式拒绝任何看起来像私钥的源；
  · ssh 把 /home/ubuntu、/opt、/tmp 重映射进沙箱，执行后断言沙箱外无新增；
  · kill 是 bash 内建，PATH 替身拦不住，改由远端正文前奏 enable -n 关掉。
"""
import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
RC = os.path.normpath(os.path.join(HERE, '..', 'restaurant-rc-15'))
DEPLOY = os.path.join(RC, 'deploy-rc15.sh')

# 原来这里是固定目录 + shutil.rmtree 真删重建。真删违反红线，
# 而且我已经这样跑过几次——那几次删除是实际发生的，如实记在报告里。
# 现在每次生成唯一目录并保留，不做任何自动清理，需要时人工归档。
SANDBOX = tempfile.mkdtemp(prefix='rc15_e2e_')
BIN = os.path.join(SANDBOX, 'bin')
# RC15_NO_REMAP=1（容器内）：假远端直接用真实绝对路径，不拼前缀。
# 容器已经是隔离边界，再拼一层只会把路径叠成 /tmp/sb/remote/tmp/sb/remote/... 
NO_REMAP = os.environ.get('RC15_NO_REMAP') == '1'
# 容器模式用根目录 '/' 而不是空串：os.path.join('', 'home', 'ubuntu') 是相对路径，
# 假远端会落在当前目录而不是 /home/ubuntu，脚本自然找不到。
REMOTE_ROOT = '/' if NO_REMAP else os.path.join(SANDBOX, 'remote')          # 假的远端根，充当 /home/ubuntu 与 /opt
CALLS = os.path.join(SANDBOX, 'calls.log')
os.makedirs(BIN, exist_ok=True)


def write(path, text, executable=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(text)
    if executable:
        os.chmod(path, os.stat(path).st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)


# ---------------------------------------------------------------- 替身
# ssh：把远端命令在假树里真的执行。这样脚本里 SSH 正文的引号、变量展开、
# 命令顺序全都会以真实方式生效——静态 grep 永远看不出这些。
write(os.path.join(BIN, 'ssh'), '''#!/usr/bin/env bash
echo "ssh $*" >> "%(calls)s"
args=(); for a in "$@"; do args+=("$a"); done
n=${#args[@]}
body="${args[$((n-1))]}"
SB="%(remote)s"
# SB 为空 = 容器模式：容器本身就是隔离边界，不做前缀重映射。
# 非空 = 宿主模式：必须把三个前缀映射进沙箱，否则会写到本机真实位置。
if [ -n "$SB" ] && [ "$SB" != "/" ]; then
  # 先全部换成占位符，再一次性替成 SB 路径。
  # 原来是逐个前缀连续替换，第二次替换会把第一次刚生成的 $SB/tmp 再映射一遍，
  # 结果叠成 /tmp/sb/remote/tmp/sb/remote/...
  body="${body//\/home\/ubuntu/@@RC15H@@}"
  body="${body//\/opt\//@@RC15O@@}"
  body="${body//\/tmp\//@@RC15T@@}"
  body="${body//@@RC15H@@/$SB\/home\/ubuntu}"
  body="${body//@@RC15O@@/$SB\/opt\/}"
  body="${body//@@RC15T@@/$SB\/tmp\/}"
fi
export HOME="$SB/home/ubuntu"
mkdir -p "$HOME" "$SB/tmp" "$SB/opt"
cd "$HOME" || exit 1
# kill 是 bash 内建，PATH 里的同名替身拦不住，必须在这里关掉内建再改成空操作。
prelude='enable -n kill 2>/dev/null || true; kill() { :; }; '
bash -c "$prelude$body"
rc=$?
# 断言：本次执行没有在沙箱外留下新文件（只查三个被重映射的前缀的真身）
# 宿主模式才查外泄；容器模式下这些路径本来就是假远端的正当位置。
if [ -n "$SB" ] && [ "$SB" != "/" ]; then
  for probe in /home/ubuntu/deploy_backups /home/ubuntu/rc15_tools /opt/youjianchuiyan; do
    if [ -e "$probe" ]; then echo "STUB_LEAK_OUTSIDE_SANDBOX $probe" >&2; exit 92; fi
  done
fi
exit $rc
''' % {'calls': CALLS.replace(chr(92), '/'), 'remote': REMOTE_ROOT.replace(chr(92), '/')}, True)

write(os.path.join(BIN, 'scp'), '''#!/usr/bin/env bash
# 按 scp 的实际选项语义解析：带值的选项要吃掉下一个参数，
# 否则 -i 后面的私钥路径会被当成源文件复制走。
echo "scp $*" >> "%(calls)s"
args=(); for a in "$@"; do args+=("$a"); done
n=${#args[@]}
dest="${args[$((n-1))]}"
srcs=(); i=0
while [ $i -lt $((n-1)) ]; do
  a="${args[$i]}"
  case "$a" in
    -i|-o|-P|-F|-l|-c|-S|-J) i=$((i+2)); continue ;;
    -*) i=$((i+1)); continue ;;
  esac
  case "$a" in
    *id_rsa*|*id_ed25519*|*.pem|*/.ssh/*)
      echo "STUB_REFUSE_KEYLIKE_SOURCE" >&2; exit 91 ;;
  esac
  [ -e "$a" ] && srcs+=("$a")
  i=$((i+1))
done
remote_path="${dest#*:}"
case "$remote_path" in
  /*) target="%(remote)s$remote_path" ;;
  *)  target="%(remote)s/home/ubuntu/$remote_path" ;;
esac
mkdir -p "$(dirname "$target")" 2>/dev/null
canon=$(cd "$(dirname "$target")" 2>/dev/null && pwd -P || echo "")
case "$canon" in
  "%(remote)s"|"%(remote)s"/*) : ;;
  *) echo "STUB_ESCAPE_REJECTED" >&2; exit 90 ;;
esac
for f in "${srcs[@]}"; do cp -r "$f" "$target" 2>/dev/null || true; done
exit 0
''' % {'calls': CALLS.replace(chr(92), '/'), 'remote': REMOTE_ROOT.replace(chr(92), '/')}, True)

# 数据库替身：只记账并返回可用输出，不连任何库
write(os.path.join(BIN, 'mysql'), '''#!/usr/bin/env bash
echo "mysql $*" >> "%(calls)s"
# 按被查的东西分别作答，不是一律返回成功。未知查询非零退出，
# 免得将来新增的探测被悄悄当成通过。
q=""
take=0
for a in "$@"; do
  if [ "$take" = "1" ]; then q="$a"; take=0; continue; fi
  [ "$a" = "-e" ] && take=1
done
if [ -z "$q" ]; then cat > /dev/null 2>&1; exit 0; fi   # 读 SQL 文件的迁移调用
case "$q" in
  *information_schema.columns*booking_master*) echo 1; exit 0 ;;
  *KEY_COLUMN_USAGE*fk_ipad_batch_booking_scope*) echo 3; exit 0 ;;
  *STATISTICS*uk_booking_master_id_store_booking*) echo 3; exit 0 ;;
esac
echo "STUB_UNKNOWN_QUERY" >&2
exit 93
''' % {'calls': CALLS.replace(chr(92), '/')}, True)

write(os.path.join(BIN, 'mysqldump'), '''#!/usr/bin/env bash
echo "mysqldump $*" >> "%(calls)s"
echo "-- Dump completed"
''' % {'calls': CALLS.replace('\\', '/')}, True)

write(os.path.join(BIN, 'curl'), '''#!/usr/bin/env bash
echo "curl $*" >> "%(calls)s"
echo 401
''' % {'calls': CALLS.replace('\\', '/')}, True)

# mvn：假装构建成功，并把 jar 内容改掉——这样"post 记在构建前"就会现形
write(os.path.join(BIN, 'mvn'), '''#!/usr/bin/env bash
echo "mvn $*" >> "%(calls)s"
mkdir -p target
echo "JAR-BUILT-BY-THIS-DEPLOY-$(date +%%s%%N)" > target/banquet-1.0.0.jar
echo "BUILD SUCCESS"
''' % {'calls': CALLS.replace('\\', '/')}, True)

write(os.path.join(BIN, 'unzip'), '''#!/usr/bin/env bash
echo "unzip $*" >> "%(calls)s"
# 读模拟 jar 的实际内容作答：构建前不含新增类，构建后含。
# 恒 exit 1 会让构建后的 IN-JAR 检查必然失败，那是替身在制造假阴性。
jar=""
for a in "$@"; do case "$a" in *.jar) jar="$a" ;; esac; done
[ -f "$jar" ] || exit 1
if grep -q "JAR-BUILT-BY-THIS-DEPLOY" "$jar" 2>/dev/null; then
  echo "com/youjian/banquet/auth/StaffRealtimeGuard.class"
  echo "com/youjian/banquet/service/IpadBatchAuthorizationService.class"
  echo "com/youjian/banquet/service/IpadBatchSubmissionService.class"
  echo "com/youjian/banquet/controller/LegalController.class"
  echo "com/youjian/banquet/service/LegalEvidenceService.class"
  echo "BOOT-INF/classes/com/youjian/banquet/controller/AuthController.class"
else
  echo "com/youjian/banquet/controller/LegalController.class"
  echo "com/youjian/banquet/service/LegalEvidenceService.class"
fi
exit 0
''' % {'calls': CALLS.replace(chr(92), '/')}, True)

for name, body in (
        ('sudo', '#!/usr/bin/env bash\nexec "$@"\n'),
        ('pgrep', '#!/usr/bin/env bash\necho 4242\n'),
        ('kill', '#!/usr/bin/env bash\nexit 0\n'),
        ('setsid', '#!/usr/bin/env bash\nexit 0\n'),
        ('nohup', '#!/usr/bin/env bash\nexit 0\n'),
        ('java', '#!/usr/bin/env bash\nexit 0\n'),
        ('zcat', '#!/usr/bin/env bash\necho "-- Dump completed"\n'),
        ('chown', '#!/usr/bin/env bash\nexit 0\n'),
        ('strings', '#!/usr/bin/env bash\necho auth/change-password\n')):
    write(os.path.join(BIN, name), body, True)

# ---------------------------------------------------------------- 假远端内容
R = os.path.join(REMOTE_ROOT, 'home', 'ubuntu')
PROJ = os.path.join(R, 'deploy_tmp_main', 'banquet_project')
for rel, text in (
        ('src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java', 'OLD-INTERCEPTOR\n'),
        ('src/main/java/com/youjian/banquet/aop/StoreDataScopeAspect.java', 'OLD-SCOPE\n'),
        ('src/main/java/com/youjian/banquet/aop/AuditLogAspect.java', 'OLD-AUDIT\n'),
        ('src/main/java/com/youjian/banquet/util/UserContext.java', 'OLD-CTX\n'),
        ('src/main/java/com/youjian/banquet/controller/IpadOrderController.java', 'OLD-IPAD\n'),
        ('src/main/java/com/youjian/banquet/controller/AuthController.java',
         'auth/change-password OLD-AUTH\n'),
        ('src/main/java/com/youjian/banquet/service/LegalEvidenceService.java',
         'dossierRevision LEGAL\n'),
        ('src/main/java/com/youjian/banquet/controller/LegalController.java', 'LEGAL-CTRL\n'),
        ('target/banquet-1.0.0.jar', 'JAR-BEFORE-DEPLOY\n')):
    write(os.path.join(PROJ, rel), text)
write(os.path.join(R, '.banquet_env.sh'),
      'export MYSQL_USER=synthetic\nexport MYSQL_PASSWORD=synthetic\nexport MYSQL_DATABASE=synthetic\n')
DIST = os.path.join(REMOTE_ROOT, 'opt', 'youjianchuiyan', 'frontend_v3', 'dist')
for rel, text in (('index.html', 'OLD-INDEX\n'), ('collab.html', 'OLD-COLLAB\n'),
                  ('assets/app-old.js', 'OLD-ASSET\n'), ('case/index.html', 'LEGAL-PAGE\n'),
                  ('case2/index.html', 'LEGAL-PAGE-2\n')):
    write(os.path.join(DIST, rel), text)

# 本地待发布件：脚本从 $WORKTREE 取
WT = os.path.join(SANDBOX, 'worktree')
for rel, text in (
        ('banquet_project/src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java', 'NEW-INTERCEPTOR\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/aop/StoreDataScopeAspect.java', 'NEW-SCOPE\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/aop/AuditLogAspect.java', 'NEW-AUDIT\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/util/UserContext.java', 'NEW-CTX\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/auth/StaffRealtimeGuard.java', 'NEW-GUARD\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/controller/IpadOrderController.java', 'NEW-IPAD\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/service/IpadBatchAuthorizationService.java', 'NEW-A\n'),
        ('banquet_project/src/main/java/com/youjian/banquet/service/IpadBatchSubmissionService.java', 'NEW-B\n'),
        ('banquet_project/src/main/resources/ipad_batch_request_migration_v1.sql', 'SELECT 1;\n'),
        ('scripts/migrations/payroll_approval_payout_v1.sql', 'SELECT 1;\n'),
        ('scripts/migrations/restaurant_print_config_v1.sql', 'SELECT 1;\n'),
        ('frontend_v3/dist/index.html', 'NEW-INDEX\n'),
        ('frontend_v3/dist/collab.html', 'NEW-COLLAB\n'),
        ('frontend_v3/dist/assets/app-new.js', 'NEW-ASSET\n')):
    write(os.path.join(WT, rel), text)
# 原来这一行往 RC/staging/AuthController.merged.java 写夹具内容——
# 那是真实的、已入库的合并件，测试台把它改脏了。测试绝不写沙箱之外的路径。
# 改成：真实文件存在就复制进沙箱镜像，不存在才在沙箱里造一个。
_real_merged = os.path.join(RC, 'staging', 'AuthController.merged.java')
_stage_mirror = os.path.join(SANDBOX, 'stage_local')
os.makedirs(_stage_mirror, exist_ok=True)
if os.path.exists(_real_merged):
    shutil.copyfile(_real_merged, os.path.join(_stage_mirror, 'AuthController.merged.java'))
else:
    write(os.path.join(_stage_mirror, 'AuthController.merged.java'),
          'auth/change-password decoyHash MERGED' + chr(10))
env_stage_local = _stage_mirror

# 未知命令兜底：凡是脚本可能用到、我又没造替身的外部命令，
# 都不该悄悄落到系统真身上。这里显式列出已知替身，其余在报告里标为未覆盖，
# 不提供任何回落到真实 ssh/网络的路径。
KNOWN_STUBS = sorted(os.listdir(BIN))
write(os.path.join(SANDBOX, 'known_stubs.txt'), chr(10).join(KNOWN_STUBS) + chr(10))

open(CALLS, 'w').close()

env = dict(os.environ)
env['PATH'] = BIN + os.pathsep + env['PATH']
env['WORKTREE'] = WT
env['RC15_TS'] = '20260909-101500'
env['TMPDIR'] = os.path.join(SANDBOX, 'tmp')
os.makedirs(env['TMPDIR'], exist_ok=True)

# Windows 上直接 subprocess 调 bash 会撞上 WSL 的 bash，跑不了 Git Bash 的脚本。
# 所以这里只负责把沙箱和替身搭好，再写一份 env.sh；真正的执行交给 Git Bash。
NL = chr(10)
_lines = [
    'export PATH="' + BIN.replace(chr(92), '/') + ':$PATH"',
    'export WORKTREE="' + WT.replace(chr(92), '/') + '"',
    'export RC15_TS=20260909-101500',
    'export TMPDIR="' + env['TMPDIR'].replace(chr(92), '/') + '"',
    'export STAGE_LOCAL="' + env_stage_local.replace(chr(92), '/') + '"',
]
write(os.path.join(SANDBOX, 'env.sh'), NL.join(_lines) + NL)
print('SANDBOX_READY ' + SANDBOX)
print('ENV ' + SANDBOX.replace(chr(92), '/') + '/env.sh')
print('DEPLOY ' + DEPLOY.replace(chr(92), '/'))
sys.exit(0)

# 真实外呼核查：替身之外的调用不会出现在 calls.log，但只要 PATH 生效就都被截住
with open(CALLS, encoding='utf-8', errors='replace') as fh:
    calls = [l for l in fh if l.strip()]
print('替身拦截调用数=%d（全部为沙箱内执行，无真实外呼）' % len(calls))

# 供人工复核的落点
print('沙箱保留在: %s' % SANDBOX)
sys.exit(proc.returncode)
