# -*- coding: utf-8 -*-
"""一次性把 deploy 里全部 mysql 调用建模，并修实跑暴露的一处同类缺陷。

先把调用清点完再建模，不逐条失败再猜。deploy 里的 mysql 调用共 8 处（rollback 里 0 处）：

  元数据只读（-e，需要返回值）
    1) columns / booking_master / COLUMN_NAME=id          -> 1
    2) KEY_COLUMN_USAGE / fk_ipad_batch_booking_scope     -> 3
    3) STATISTICS / uk_booking_master_id_store_booking    -> 3
    4) tables / ipad_batch_request                        -> ipad_batch_request
    5) columns / dish_recipe / is_active,revision_id      -> 2
  DDL 与迁移（stdin，不需要返回值）
    6) < payroll_approval_payout_v1.sql
    7) <<SQL  ipad_batch_request 建表
    8) <<SQL  dish_recipe 版本化 + < restaurant_print_config_v1.sql

stdin 那几路只记 SQL 的 sha256 与长度，返回成功；不假称"这段 SQL 已经被验证过"——
替身没有真数据库，它只能证明脚本的控制流，证明不了 SQL 本身。这一点写在这里，
免得日后有人把这个跑绿当成迁移已验证。
未知查询仍然非零退出，不给静默通过留口子。

顺带修实跑暴露的一处真缺陷：1d 打印配置迁移仍是
  mysql ... < file && echo OK
后面紧跟 mv。与 1a 工资迁移当初那处同类——失败时 mv 照跑。改显式 if/else。
"""
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RC = os.path.normpath(os.path.join(HERE, '..', 'restaurant-rc-15'))
DEPLOY = os.path.join(RC, 'deploy-rc15.sh')
P = os.path.join(HERE, 'e2e_stub_run.py')
NL = chr(10)
missing = []


def rep_in(text, old, new, tag):
    global missing
    if text.count(old) != 1:
        missing.append(tag + '(count=%d)' % text.count(old))
        return text
    return text.replace(old, new, 1)


# ---------------- 1. 修 1d 的 && echo 缺陷 ----------------
d = io.open(DEPLOY, encoding='utf-8').read()
d = rep_in(
    d,
    """  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/restaurant_print_config_v1.rc15-'"$TS"'.sql \\
    && echo "OK print config migration applied\"""",
    """  # 与 1a 同类：mysql ... && echo OK 之后紧跟 mv，失败时 mv 照跑。改显式 if/else。
  if mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/restaurant_print_config_v1.rc15-'"$TS"'.sql; then
    echo "OK print config migration applied"
  else
    echo "ABORT print config migration failed，后续文件移动停止" >&2
    exit 1
  fi""",
    '1d-flow')
io.open(DEPLOY, 'w', encoding='utf-8').write(d)

# ---------------- 2. mysql 替身：全量模型 ----------------
s = io.open(P, encoding='utf-8').read()
start = s.index("write(os.path.join(BIN, 'mysql'), '''#!/usr/bin/env bash")
end = s.index('True)', start) + len('True)')
new_stub = (
    "write(os.path.join(BIN, 'mysql'), '''#!/usr/bin/env bash" + NL
    + 'echo "mysql $*" >> "%(calls)s"' + NL
    + '# 按具体语句作答。未知查询非零退出——不给静默通过留口子。' + NL
    + 'q=""; take=0' + NL
    + 'for a in "$@"; do' + NL
    + '  if [ "$take" = "1" ]; then q="$a"; take=0; continue; fi' + NL
    + '  [ "$a" = "-e" ] && take=1' + NL
    + 'done' + NL
    + 'if [ -z "$q" ]; then' + NL
    + '  # stdin 路：DDL 与迁移脚本。只记 sha256 与长度，返回成功。' + NL
    + '  # 替身没有真数据库，这只能证明脚本控制流，证明不了 SQL 本身。' + NL
    + '  tmp=$(mktemp); cat > "$tmp"' + NL
    + '  h=$(sha256sum "$tmp" | cut -d" " -f1); n=$(wc -c < "$tmp")' + NL
    + '  echo "SQL_STDIN sha256=$h bytes=$n" >> "%(calls)s"' + NL
    + '  if [ -n "${RC15_FAIL_MIGRATION:-}" ] && grep -q "$RC15_FAIL_MIGRATION" "$tmp"; then' + NL
    + '    echo "ERROR 1064 (42000): injected failure" >&2; exit 1' + NL
    + '  fi' + NL
    + '  exit 0' + NL
    + 'fi' + NL
    + 'case "$q" in' + NL
    + "  *information_schema.columns*booking_master*) echo 1; exit 0 ;;" + NL
    + "  *KEY_COLUMN_USAGE*fk_ipad_batch_booking_scope*) echo 3; exit 0 ;;" + NL
    + "  *STATISTICS*uk_booking_master_id_store_booking*) echo 3; exit 0 ;;" + NL
    + "  *information_schema.tables*ipad_batch_request*) echo ipad_batch_request; exit 0 ;;" + NL
    + "  *information_schema.columns*dish_recipe*) echo 2; exit 0 ;;" + NL
    + 'esac' + NL
    + 'echo "STUB_UNKNOWN_QUERY" >&2' + NL
    + 'exit 93' + NL
    + "''' % {'calls': CALLS.replace(chr(92), '/')}, True)")
s = s[:start] + new_stub + s[end:]
io.open(P, 'w', encoding='utf-8').write(s)

print('MISSING=' + (','.join(missing) if missing else 'none'))
sys.exit(1 if missing else 0)
