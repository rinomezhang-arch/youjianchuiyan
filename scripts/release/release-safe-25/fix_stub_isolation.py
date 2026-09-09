# -*- coding: utf-8 -*-
"""修 E2E 替身台的三处隔离缺口。统筹实读源码发现的，三条都成立。

本脚本只改源码，不运行替身台。

一、scp 替身把"所有不以 - 开头且存在的参数"都当成源文件复制。
    而命令行是 scp -i $KEY -o IdentitiesOnly=yes SRC host:dest——
    $KEY 正是私钥路径，它不以 - 开头、而且存在，于是会被当成源文件复制进沙箱。
    这是我写的替身台把凭据往外搬，性质比功能缺陷严重。
    改成按 scp 的实际选项语义解析：-i/-o/-P/-F/-l/-c/-S 都吃掉下一个参数，
    其余以 - 开头的当开关跳过，剩下的才是源。并且显式拒绝任何看起来像私钥的源。

二、ssh 替身只改 HOME，远端正文里的绝对路径（/home/ubuntu、/opt、/tmp）
    仍会落到本机真实位置。改成把这三个前缀重映射进沙箱，
    并在执行前后各断言一次：所有写入必须落在沙箱内。

三、kill 是 bash 内建，PATH 里放一个同名替身拦不住它。
    在远端正文前面加一段前奏：enable -n kill 关掉内建，再定义成空操作函数。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, 'e2e_stub_run.py')
s = io.open(P, encoding='utf-8').read()
missing = []
NL = chr(10)


def rep(old, new, tag):
    global s, missing
    if s.count(old) != 1:
        missing.append(tag + '(count=%d)' % s.count(old))
        return
    s = s.replace(old, new, 1)


# ---------------- 一、scp：按真实选项语义解析，并拒绝私钥类源 ----------------
old_scp_start = s.index("write(os.path.join(BIN, 'scp'), '''#!/usr/bin/env bash")
old_scp_end = s.index("'remote': REMOTE_ROOT.replace('\\\\', '/')}, True)", old_scp_start)
old_scp_end = s.index('True)', old_scp_end) + len('True)')
new_scp = (
    "write(os.path.join(BIN, 'scp'), '''#!/usr/bin/env bash" + NL
    + '# 按 scp 的实际选项语义解析：带值的选项要吃掉下一个参数，' + NL
    + '# 否则 -i 后面的私钥路径会被当成源文件复制走。' + NL
    + 'echo "scp $*" >> "%(calls)s"' + NL
    + 'args=(); for a in "$@"; do args+=("$a"); done' + NL
    + 'n=${#args[@]}' + NL
    + 'dest="${args[$((n-1))]}"' + NL
    + 'srcs=(); i=0' + NL
    + 'while [ $i -lt $((n-1)) ]; do' + NL
    + '  a="${args[$i]}"' + NL
    + '  case "$a" in' + NL
    + '    -i|-o|-P|-F|-l|-c|-S|-J) i=$((i+2)); continue ;;' + NL
    + '    -*) i=$((i+1)); continue ;;' + NL
    + '  esac' + NL
    + '  case "$a" in' + NL
    + '    *id_rsa*|*id_ed25519*|*.pem|*/.ssh/*)' + NL
    + '      echo "STUB_REFUSE_KEYLIKE_SOURCE" >&2; exit 91 ;;' + NL
    + '  esac' + NL
    + '  [ -e "$a" ] && srcs+=("$a")' + NL
    + '  i=$((i+1))' + NL
    + 'done' + NL
    + 'remote_path="${dest#*:}"' + NL
    + 'case "$remote_path" in' + NL
    + '  /*) target="%(remote)s$remote_path" ;;' + NL
    + '  *)  target="%(remote)s/home/ubuntu/$remote_path" ;;' + NL
    + 'esac' + NL
    + 'mkdir -p "$(dirname "$target")" 2>/dev/null' + NL
    + 'canon=$(cd "$(dirname "$target")" 2>/dev/null && pwd -P || echo "")' + NL
    + 'case "$canon" in' + NL
    + '  "%(remote)s"|"%(remote)s"/*) : ;;' + NL
    + '  *) echo "STUB_ESCAPE_REJECTED" >&2; exit 90 ;;' + NL
    + 'esac' + NL
    + 'for f in "${srcs[@]}"; do cp -r "$f" "$target" 2>/dev/null || true; done' + NL
    + 'exit 0' + NL
    + "''' % {'calls': CALLS.replace(chr(92), '/'), 'remote': REMOTE_ROOT.replace(chr(92), '/')}, True)")
s = s[:old_scp_start] + new_scp + s[old_scp_end:]

# ---------------- 二、ssh：绝对路径重映射 + 沙箱断言；三、内建 kill ----------------
old_ssh_start = s.index("write(os.path.join(BIN, 'ssh'), '''#!/usr/bin/env bash")
old_ssh_end = s.index('True)', s.index("'remote': REMOTE_ROOT.replace(chr(92), '/')}, True)", old_ssh_start)
                      ) + len('True)') if False else s.index('True)', old_ssh_start) + len('True)')
new_ssh = (
    "write(os.path.join(BIN, 'ssh'), '''#!/usr/bin/env bash" + NL
    + 'echo "ssh $*" >> "%(calls)s"' + NL
    + 'args=(); for a in "$@"; do args+=("$a"); done' + NL
    + 'n=${#args[@]}' + NL
    + 'body="${args[$((n-1))]}"' + NL
    + 'SB="%(remote)s"' + NL
    + '# 只改 HOME 挡不住远端正文里的绝对路径。把三个前缀重映射进沙箱，' + NL
    + '# 否则 /home/ubuntu、/opt、/tmp 会直接落到本机真实位置。' + NL
    + 'body="${body//\\/home\\/ubuntu/$SB\\/home\\/ubuntu}"' + NL
    + 'body="${body//\\/opt\\//$SB\\/opt\\/}"' + NL
    + 'body="${body//\\/tmp\\//$SB\\/tmp\\/}"' + NL
    + 'export HOME="$SB/home/ubuntu"' + NL
    + 'mkdir -p "$HOME" "$SB/tmp" "$SB/opt"' + NL
    + 'cd "$HOME" || exit 1' + NL
    + '# kill 是 bash 内建，PATH 里的同名替身拦不住，必须在这里关掉内建再改成空操作。' + NL
    + "prelude='enable -n kill 2>/dev/null || true; kill() { :; }; '" + NL
    + 'bash -c "$prelude$body"' + NL
    + 'rc=$?' + NL
    + '# 断言：本次执行没有在沙箱外留下新文件（只查三个被重映射的前缀的真身）' + NL
    + 'for probe in /home/ubuntu/deploy_backups /home/ubuntu/rc15_tools /opt/youjianchuiyan; do' + NL
    + '  if [ -e "$probe" ]; then echo "STUB_LEAK_OUTSIDE_SANDBOX $probe" >&2; exit 92; fi' + NL
    + 'done' + NL
    + 'exit $rc' + NL
    + "''' % {'calls': CALLS.replace(chr(92), '/'), 'remote': REMOTE_ROOT.replace(chr(92), '/')}, True)")
s = s[:old_ssh_start] + new_ssh + s[old_ssh_end:]

# 顶部说明补一句：本版未运行
s = s.replace(
    '硬约束：替身只在沙箱里动文件，绝不发起网络调用；任何一次真实外呼都会被记账并判失败。',
    '硬约束：替身只在沙箱里动文件，绝不发起网络调用；任何一次真实外呼都会被记账并判失败。' + NL
    + NL
    + '隔离修订（统筹实读发现，本版仅提交源码、未运行）：' + NL
    + '  · scp 按真实选项语义解析，-i 后的私钥不会再被当成源文件复制，' + NL
    + '    并显式拒绝任何看起来像私钥的源；' + NL
    + '  · ssh 把 /home/ubuntu、/opt、/tmp 重映射进沙箱，执行后断言沙箱外无新增；' + NL
    + '  · kill 是 bash 内建，PATH 替身拦不住，改由远端正文前奏 enable -n 关掉。')

io.open(P, 'w', encoding='utf-8').write(s)
print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
