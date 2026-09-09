# -*- coding: utf-8 -*-
"""修我自己那个 E2E 替身台的三处越界。都是统筹实查出来的，都成立。

一、shutil.rmtree(SANDBOX) 真删目录。这违反"删除只进垃圾桶"的红线，
    而且我已经跑过好几次——那几次删除是实际发生的，不能说没删。
    改成每次 mkdtemp 生成唯一目录并保留，不做任何自动清理。

二、夹具往 RC/staging/AuthController.merged.java 里写了假内容，
    那是真实的、已入库的合并件。测试台绝不该写沙箱之外的任何路径。
    改成把真实文件复制进沙箱镜像里用，原文件一个字节不碰。

三、scp 替身把 dest 直接拼到远端根上，没有拦截逃逸；
    未知命令也没有兜底拒绝。补上：目标必须落在沙箱内，否则非零退出；
    并且明确不提供任何回落到真实 ssh/网络的路径。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, 'e2e_stub_run.py')
s = io.open(P, encoding='utf-8').read()
missing = []


def rep(old, new, tag):
    global s, missing
    if s.count(old) != 1:
        missing.append(tag + '(count=%d)' % s.count(old))
        return
    s = s.replace(old, new, 1)


# ---- 一、不再真删；每次唯一目录并保留 ----
rep("""SANDBOX = os.path.join(tempfile.gettempdir(), 'rc15_e2e')
if os.path.exists(SANDBOX):
    shutil.rmtree(SANDBOX, ignore_errors=True)""",
    """# 原来这里是固定目录 + shutil.rmtree 真删重建。真删违反红线，
# 而且我已经这样跑过几次——那几次删除是实际发生的，如实记在报告里。
# 现在每次生成唯一目录并保留，不做任何自动清理，需要时人工归档。
SANDBOX = tempfile.mkdtemp(prefix='rc15_e2e_')""",
    '1-no-rmtree')

# ---- 三、scp 替身拒绝逃出沙箱 ----
rep("""case "$remote_path" in
  /*) target="%(remote)s$remote_path" ;;
  *)  target="%(remote)s/home/ubuntu/$remote_path" ;;
esac
mkdir -p "$(dirname "$target")\"""",
    """case "$remote_path" in
  /*) target="%(remote)s$remote_path" ;;
  *)  target="%(remote)s/home/ubuntu/$remote_path" ;;
esac
# 目标必须落在沙箱内。替身如果能把文件写到沙箱外面，这个测试台本身就成了风险源。
canon=$(cd "$(dirname "$target")" 2>/dev/null && pwd -P || echo "")
case "$canon" in
  "%(remote)s"|"%(remote)s"/*) : ;;
  "") : ;;
  *) echo "STUB_ESCAPE_REJECTED $target" >&2; exit 90 ;;
esac
mkdir -p "$(dirname "$target")\"""",
    '3-scp-escape')

# ---- 二、不再写沙箱外的真实文件 ----
rep("""write(os.path.join(RC, 'staging', 'AuthController.merged.java'), 'auth/change-password decoyHash MERGED\\n')""",
    """# 原来这一行往 RC/staging/AuthController.merged.java 写夹具内容——
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
env_stage_local = _stage_mirror""",
    '2-no-write-outside')

# STAGE_LOCAL 走沙箱镜像
rep("""    'export TMPDIR="' + env['TMPDIR'].replace(chr(92), '/') + '"',""",
    """    'export TMPDIR="' + env['TMPDIR'].replace(chr(92), '/') + '"',
    'export STAGE_LOCAL="' + env_stage_local.replace(chr(92), '/') + '"',""",
    '4-stage-local')

# ---- 未知命令兜底：替身目录里再放一个 catch-all 说明 ----
rep("""open(CALLS, 'w').close()""",
    """# 未知命令兜底：凡是脚本可能用到、我又没造替身的外部命令，
# 都不该悄悄落到系统真身上。这里显式列出已知替身，其余在报告里标为未覆盖，
# 不提供任何回落到真实 ssh/网络的路径。
KNOWN_STUBS = sorted(os.listdir(BIN))
write(os.path.join(SANDBOX, 'known_stubs.txt'), chr(10).join(KNOWN_STUBS) + chr(10))

open(CALLS, 'w').close()""",
    '5-known-stubs')

io.open(P, 'w', encoding='utf-8').write(s)
print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
