# -*- coding: utf-8 -*-
"""替身台加容器模式：在隔离容器里就不要再拼前缀了。

容器里跑出来的报错是双重重映射：
/tmp/rc15_e2e_x/remote/tmp/rc15_e2e_x/remote/home/ubuntu/...
因为 $SB 本身在 /tmp 下，我先把 /home/ubuntu 映射进 $SB，
又把 /tmp/ 映射进 $SB，于是已经映射过的路径又被映射一次。

在 Windows 宿主上做前缀重映射是必要的——不映射就会写到真实路径去。
但在 network=none、无宿主凭据、cap-drop ALL 的容器里，容器本身就是隔离边界，
假远端直接用 /home/ubuntu、/opt、/tmp 这些真实绝对路径最干净，也免掉整类拼接错误。

所以加一个开关 RC15_NO_REMAP=1：置位时不重映射、假远端就落在容器命名空间的真实路径上。
宿主模式的行为一个字不改。
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


rep("REMOTE_ROOT = os.path.join(SANDBOX, 'remote')",
    "# RC15_NO_REMAP=1（容器内）：假远端直接用真实绝对路径，不拼前缀。" + NL
    + "# 容器已经是隔离边界，再拼一层只会把路径叠成 /tmp/sb/remote/tmp/sb/remote/... " + NL
    + "NO_REMAP = os.environ.get('RC15_NO_REMAP') == '1'" + NL
    + "REMOTE_ROOT = '' if NO_REMAP else os.path.join(SANDBOX, 'remote')",
    '1-remote-root')

rep("""SB="%(remote)s"
# 只改 HOME 挡不住远端正文里的绝对路径。把三个前缀重映射进沙箱，
# 否则 /home/ubuntu、/opt、/tmp 会直接落到本机真实位置。
body="${body//\\/home\\/ubuntu/$SB\\/home\\/ubuntu}"
body="${body//\\/opt\\//$SB\\/opt\\/}"
body="${body//\\/tmp\\//$SB\\/tmp\\/}"
export HOME="$SB/home/ubuntu\"""",
    """SB="%(remote)s"
# SB 为空 = 容器模式：容器本身就是隔离边界，不做前缀重映射。
# 非空 = 宿主模式：必须把三个前缀映射进沙箱，否则会写到本机真实位置。
if [ -n "$SB" ]; then
  body="${body//\\/home\\/ubuntu/$SB\\/home\\/ubuntu}"
  body="${body//\\/opt\\//$SB\\/opt\\/}"
  body="${body//\\/tmp\\//$SB\\/tmp\\/}"
fi
export HOME="$SB/home/ubuntu\"""",
    '2-ssh-remap-guard')

rep("""for probe in /home/ubuntu/deploy_backups /home/ubuntu/rc15_tools /opt/youjianchuiyan; do
  if [ -e "$probe" ]; then echo "STUB_LEAK_OUTSIDE_SANDBOX $probe" >&2; exit 92; fi
done""",
    """# 宿主模式才查外泄；容器模式下这些路径本来就是假远端的正当位置。
if [ -n "$SB" ]; then
  for probe in /home/ubuntu/deploy_backups /home/ubuntu/rc15_tools /opt/youjianchuiyan; do
    if [ -e "$probe" ]; then echo "STUB_LEAK_OUTSIDE_SANDBOX $probe" >&2; exit 92; fi
  done
fi""",
    '3-leak-probe-guard')

io.open(P, 'w', encoding='utf-8').write(s)
print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
