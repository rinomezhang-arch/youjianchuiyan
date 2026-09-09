# -*- coding: utf-8 -*-
"""按统筹自证的三点提高替身保真度。只改替身，不碰实际脚本的任何门槛。

原则先写清楚：替身的作用是让脚本能跑到底，把真实缺陷暴露出来；
不是用假返回把脚本的失败盖过去。所以下面每个替身都按"被查的东西真实存在与否"作答，
而不是一律返回成功。

一、mysql 统一对所有 COUNT 返回 3，会让最早那条 booking_master.id 的 grep -q 1 失败。
    改成按查询内容分别作答：列存在性返回 1，v2 三列结构返回 3，未知查询非零退出。
    未知非零很重要——不然将来新增的查询会被悄悄"通过"。

二、unzip 恒 exit 1，导致构建后所有 IN-JAR 检查必然失败。
    改成读取模拟 jar 的实际内容作答：构建前的 jar 不含本次新增类，
    构建后的 jar 含新增类与改密字符串。这样"构建前不该有、构建后必须有"两侧都能被验到。

三、SSH 正文先映射 /home 再映射 /tmp，会把 SB 自身的 /tmp 前缀再映射一遍。
    容器模式（RC15_NO_REMAP=1）本来就不做映射，已规避；
    宿主模式改成单次占位符替换，先把三个前缀换成占位符，再一次性替成 SB 路径。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, 'e2e_stub_run.py')
s = io.open(P, encoding='utf-8').read()
missing = []
NL = chr(10)
Q = chr(39)


def rep(old, new, tag):
    global s, missing
    if s.count(old) != 1:
        missing.append(tag + '(count=%d)' % s.count(old))
        return
    s = s.replace(old, new, 1)


# ---------------- 一、mysql 按查询作答 ----------------
old_mysql_start = s.index("write(os.path.join(BIN, 'mysql'), '''#!/usr/bin/env bash")
old_mysql_end = s.index("True)", old_mysql_start) + len("True)")
new_mysql = (
    "write(os.path.join(BIN, 'mysql'), '''#!/usr/bin/env bash" + NL
    + 'echo "mysql $*" >> "%(calls)s"' + NL
    + '# 按被查的东西分别作答，不是一律返回成功。未知查询非零退出，' + NL
    + '# 免得将来新增的探测被悄悄当成通过。' + NL
    + 'q=""' + NL
    + 'take=0' + NL
    + 'for a in "$@"; do' + NL
    + '  if [ "$take" = "1" ]; then q="$a"; take=0; continue; fi' + NL
    + '  [ "$a" = "-e" ] && take=1' + NL
    + 'done' + NL
    + 'if [ -z "$q" ]; then cat > /dev/null 2>&1; exit 0; fi   # 读 SQL 文件的迁移调用' + NL
    + 'case "$q" in' + NL
    + '  *information_schema.columns*booking_master*) echo 1; exit 0 ;;' + NL
    + '  *KEY_COLUMN_USAGE*fk_ipad_batch_booking_scope*) echo 3; exit 0 ;;' + NL
    + '  *STATISTICS*uk_booking_master_id_store_booking*) echo 3; exit 0 ;;' + NL
    + 'esac' + NL
    + 'echo "STUB_UNKNOWN_QUERY" >&2' + NL
    + 'exit 93' + NL
    + "''' % {'calls': CALLS.replace(chr(92), '/')}, True)")
s = s[:old_mysql_start] + new_mysql + s[old_mysql_end:]

# ---------------- 二、unzip 按 jar 内容作答 ----------------
old_unzip_start = s.index("write(os.path.join(BIN, 'unzip'), '''#!/usr/bin/env bash")
old_unzip_end = s.index("True)", old_unzip_start) + len("True)")
new_unzip = (
    "write(os.path.join(BIN, 'unzip'), '''#!/usr/bin/env bash" + NL
    + 'echo "unzip $*" >> "%(calls)s"' + NL
    + '# 读模拟 jar 的实际内容作答：构建前不含新增类，构建后含。' + NL
    + '# 恒 exit 1 会让构建后的 IN-JAR 检查必然失败，那是替身在制造假阴性。' + NL
    + 'jar=""' + NL
    + 'for a in "$@"; do case "$a" in *.jar) jar="$a" ;; esac; done' + NL
    + '[ -f "$jar" ] || exit 1' + NL
    + 'if grep -q "JAR-BUILT-BY-THIS-DEPLOY" "$jar" 2>/dev/null; then' + NL
    + '  echo "com/youjian/banquet/auth/StaffRealtimeGuard.class"' + NL
    + '  echo "com/youjian/banquet/service/IpadBatchAuthorizationService.class"' + NL
    + '  echo "com/youjian/banquet/service/IpadBatchSubmissionService.class"' + NL
    + '  echo "com/youjian/banquet/controller/LegalController.class"' + NL
    + '  echo "com/youjian/banquet/service/LegalEvidenceService.class"' + NL
    + '  echo "BOOT-INF/classes/com/youjian/banquet/controller/AuthController.class"' + NL
    + 'else' + NL
    + '  echo "com/youjian/banquet/controller/LegalController.class"' + NL
    + '  echo "com/youjian/banquet/service/LegalEvidenceService.class"' + NL
    + 'fi' + NL
    + 'exit 0' + NL
    + "''' % {'calls': CALLS.replace(chr(92), '/')}, True)")
s = s[:old_unzip_start] + new_unzip + s[old_unzip_end:]

# strings 替身：改密字符串命中计数
rep("        ('chown', '#!/usr/bin/env bash" + chr(92) + "nexit 0" + chr(92) + "n')):",
    "        ('chown', '#!/usr/bin/env bash" + chr(92) + "nexit 0" + chr(92) + "n'),"
    + NL
    + "        ('strings', '#!/usr/bin/env bash" + chr(92) + "necho auth/change-password" + chr(92) + "n')):",
    '2b-strings')

# ---------------- 三、宿主模式单次占位符映射 ----------------
rep("""if [ -n "$SB" ]; then
  body="${body//\\/home\\/ubuntu/$SB\\/home\\/ubuntu}"
  body="${body//\\/opt\\//$SB\\/opt\\/}"
  body="${body//\\/tmp\\//$SB\\/tmp\\/}"
fi""",
    """if [ -n "$SB" ]; then
  # 先全部换成占位符，再一次性替成 SB 路径。
  # 原来是逐个前缀连续替换，第二次替换会把第一次刚生成的 $SB/tmp 再映射一遍，
  # 结果叠成 /tmp/sb/remote/tmp/sb/remote/...
  body="${body//\\/home\\/ubuntu/@@RC15H@@}"
  body="${body//\\/opt\\//@@RC15O@@}"
  body="${body//\\/tmp\\//@@RC15T@@}"
  body="${body//@@RC15H@@/$SB\\/home\\/ubuntu}"
  body="${body//@@RC15O@@/$SB\\/opt\\/}"
  body="${body//@@RC15T@@/$SB\\/tmp\\/}"
fi""",
    '3-single-pass-remap')

io.open(P, 'w', encoding='utf-8').write(s)
print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
