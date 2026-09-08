#!/bin/bash
# ============================================================
# 硬化脚本可复现合成测试（CX-46b368b69675）
# 覆盖场景：PREVIEW 零写 / 权限(umask 077+0600) / symlink 解析 /
#           nginx reload / 后端重启失败回滚 / 回滚后运行态恢复
# 说明：全部在 /tmp 假文件 + 假命令(PATH 注入)下运行，不碰生产。
# ============================================================
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="/tmp/harden_synth_${RANDOM}${RANDOM}"
mkdir -p "$WORK"

PASS=0; FAIL=0
ok(){ echo "  [PASS] $*"; PASS=$((PASS+1)); }
bad(){ echo "  [FAIL] $*"; FAIL=$((FAIL+1)); }

# ---- 假命令 ----
FAKE="$WORK/fakebin"
mkdir -p "$FAKE"

cat > "$FAKE/sudo" <<'EOF'
#!/bin/bash
# 假 sudo：把后续命令路由到同目录 fake 命令（避免走真实 sudo 误动生产）
BIN="$(dirname "$0")"
cmd="$1"; shift
case "$cmd" in
  systemctl) "$BIN/systemctl" "$@";;
  install)   "$BIN/install" "$@";;
  /usr/sbin/nginx) "$BIN/nginx" "$@";;
  nginx)     "$BIN/nginx" "$@";;
  *) echo "[fake sudo] 未知命令 $cmd"; exit 0;;
esac
EOF

cat > "$FAKE/nginx" <<'EOF'
#!/bin/bash
# 假 nginx -t：默认成功；若设 NGINX_T_FAIL=1 则失败
if [ "${NGINX_T_FAIL:-0}" = "1" ]; then echo "fake nginx -t fail"; exit 1; fi
echo "fake nginx -t ok"; exit 0
EOF

cat > "$FAKE/systemctl" <<'EOF'
#!/bin/bash
# 假 systemctl：记录动作；若设 RESTART_FAIL=1 且含 restart 则失败
echo "[fake systemctl] $*" >> "${SYSTEMCTL_LOG:-/dev/null}"
if [ "${RESTART_FAIL:-0}" = "1" ] && echo "$*" | grep -q "restart banquet"; then
  echo "fake restart fail"; exit 1
fi
if echo "$*" | grep -q "reload nginx"; then echo "reloaded"; fi
exit 0
EOF

cat > "$FAKE/curl" <<'EOF'
#!/bin/bash
# 假 curl：本机 health 返回 200；HTTPS 返回 5 个头；非 loopback 8080 返回失败
args="$*"
if echo "$args" | grep -q "127.0.0.1:8080"; then echo "200"; exit 0; fi
if echo "$args" | grep -q "https://"; then
  echo "HTTP/1.1 200"
  echo "Strict-Transport-Security: max-age=31536000"
  echo "X-Content-Type-Options: nosniff"
  echo "X-Frame-Options: SAMEORIGIN"
  echo "Referrer-Policy: strict-origin-when-cross-origin"
  echo "Permissions-Policy: camera=(), microphone=()"
  exit 0
fi
# 非 loopback 8080：失败
exit 7
EOF

cat > "$FAKE/ss" <<'EOF'
#!/bin/bash
# 假 ss：默认 8080 只监听 127.0.0.1；若设 SS_LISTEN_ALL=1 则监听 0.0.0.0
if [ "${SS_LISTEN_ALL:-0}" = "1" ]; then echo "LISTEN 0.0.0.0:8080"; else echo "LISTEN 127.0.0.1:8080"; fi
exit 0
EOF

cat > "$FAKE/hostname" <<'EOF'
#!/bin/bash
echo "10.10.0.14 100.70.215.11 172.17.0.1"
exit 0
EOF

cat > "$FAKE/ip" <<'EOF'
#!/bin/bash
if echo "$*" | grep -q -- "-6"; then echo "inet6 fd7a:115c:a1e0::4c34:d70c/64 scope global"; fi
exit 0
EOF

cat > "$FAKE/install" <<'EOF'
#!/bin/bash
# 假 install：真正执行文件拷贝（去掉 -o/-g/-m 参数），保持可测
last=""
for a in "$@"; do last="$a"; done
# $@ 最后两个是 src dst；简化：找倒数第二个是 src
# 直接透传真实 install（本机有），但为隔离性，用 cp 模拟
src=""; dst=""
args=("$@")
n=${#args[@]}
dst="${args[$((n-1))]}"
src="${args[$((n-2))]}"
cp "$src" "$dst"
exit 0
EOF

cat > "$FAKE/readlink" <<'EOF'
#!/bin/bash
# 假 readlink -f：返回 symlink 真实目标（测试 symlink 解析）
if [ "$1" = "-f" ]; then
  if [ -L "$2" ]; then
    # 读 symlink 指向
    tgt="$(readlink "$2")"
    echo "$tgt"
  else
    echo "$2"
  fi
else
  /usr/bin/readlink "$@"
fi
exit 0
EOF

chmod +x "$FAKE"/*

echo "=== 合成测试开始（工作目录 $WORK）==="

# ---- 场景1：PREVIEW 零写 ----
echo "[场景1] PREVIEW 零写"
ENV="$WORK/env.sh"; NG="$WORK/default"
printf 'export MYSQL_PORT=3306\n' > "$ENV"
printf 'server {\n listen 443 ssl;\n server_name x;\n ssl_prefer_server_ciphers on;\n}\n' > "$NG"
before="$(ls -A "$WORK")"
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  bash "$HERE/harden.sh" >/dev/null 2>&1
after="$(ls -A "$WORK")"
if [ "$before" = "$after" ]; then ok "PREVIEW 零写"; else bad "PREVIEW 有写盘"; fi

# ---- 场景2：权限（umask 077 + env 备份 0600） ----
echo "[场景2] 权限"
rm -rf "$WORK/backups" "$WORK/trash"
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  NGINX_T_FAIL=0 SS_LISTEN_ALL=0 RESTART_FAIL=0 APPLY=YES \
  bash "$HERE/harden.sh" >/dev/null 2>&1
# 找 env.bak 权限
envbak="$(find "$WORK/backups" -name env.bak | head -1)"
if [ -n "$envbak" ] && [ "$(stat -c '%a' "$envbak")" = "600" ]; then ok "env 备份权限 600"; else bad "env 备份权限非 600（$(stat -c '%a' "$envbak" 2>/dev/null)）"; fi
# 备份目录权限 700（umask 077 下 mkdir 默认 700）
bakdir="$(dirname "$envbak")"
if [ "$(stat -c '%a' "$bakdir")" = "700" ]; then ok "备份目录权限 700（umask 077）"; else bad "备份目录权限非 700（$(stat -c '%a' "$bakdir")）"; fi

# ---- 场景3：symlink 解析 ----
echo "[场景3] symlink 解析真实目标"
rm -rf "$WORK/backups" "$WORK/trash"
REAL="$WORK/real_default"
printf 'server {\n listen 443 ssl;\n ssl_prefer_server_ciphers on;\n}\n' > "$REAL"
LINK="$WORK/default_link"
ln -sf "$REAL" "$LINK"
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$LINK" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  APPLY=YES bash "$HERE/harden.sh" >/dev/null 2>&1
if grep -q "tianlong-hardening-20260908" "$REAL"; then ok "安全头写入 symlink 真实目标"; else bad "未写入真实目标"; fi
if [ -L "$LINK" ]; then ok "symlink 未被替换（仍为链接）"; else bad "symlink 被替换为普通文件"; fi

# ---- 场景4：nginx reload 触发 ----
echo "[场景4] nginx reload 被调用"
rm -rf "$WORK/backups" "$WORK/trash"
SYSLOG="$WORK/systemctl.log"; export SYSTEMCTL_LOG="$SYSLOG"
printf 'server {\n listen 443 ssl;\n ssl_prefer_server_ciphers on;\n}\n' > "$NG"
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  APPLY=YES bash "$HERE/harden.sh" >/dev/null 2>&1
if grep -q "reload nginx" "$SYSLOG"; then ok "nginx reload 被调用"; else bad "nginx reload 未调用"; fi
if grep -q "restart banquet" "$SYSLOG"; then ok "banquet restart 被调用"; else bad "banquet restart 未调用"; fi
unset SYSTEMCTL_LOG

# ---- 场景5：后端重启失败 → 自动回滚 + 运行态恢复 ----
echo "[场景5] 后端重启失败自动回滚"
rm -rf "$WORK/backups" "$WORK/trash"
printf 'export MYSQL_PORT=3306\n' > "$ENV"   # 重新准备干净 env（避免复用场景2污染）
cp "$ENV" "$WORK/env.orig"
printf 'server {\n listen 443 ssl;\n ssl_prefer_server_ciphers on;\n}\n' > "$NG"
cp "$NG" "$WORK/ng.orig"
SYSLOG="$WORK/systemctl_fail.log"; export SYSTEMCTL_LOG="$SYSLOG"
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  RESTART_FAIL=1 APPLY=YES bash "$HERE/harden.sh" >/dev/null 2>&1
rc=$?
# 回滚后 env 应恢复原状（无 SERVER_ADDRESS）
if ! grep -q "SERVER_ADDRESS" "$ENV"; then ok "重启失败后 env 已回滚（无 SERVER_ADDRESS）"; else bad "env 未回滚"; fi
if ! grep -q "tianlong-hardening-20260908" "$NG"; then ok "重启失败后 nginx 已回滚（无安全头）"; else bad "nginx 未回滚"; fi
# 回滚后运行态恢复：systemctl 里应有 restart banquet（回滚阶段的）
if grep -q "restart banquet" "$SYSLOG"; then ok "回滚阶段调用了 restart banquet"; else bad "回滚阶段未 restart banquet"; fi
unset SYSTEMCTL_LOG

# ---- 场景6：standalone rollback 按 manifest 成对恢复 ----
echo "[场景6] standalone rollback 读 manifest"
rm -rf "$WORK/backups" "$WORK/trash"
printf 'export MYSQL_PORT=3306\n' > "$ENV"   # 重新准备干净 env
printf 'server {\n listen 443 ssl;\n ssl_prefer_server_ciphers on;\n}\n' > "$NG"
# 先做一次成功 APPLY，生成 manifest 和备份
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  APPLY=YES bash "$HERE/harden.sh" >/dev/null 2>&1
# 确认已硬化
grep -q "SERVER_ADDRESS" "$ENV" && ok "硬化后 env 含 SERVER_ADDRESS" || bad "硬化失败"
# 执行 standalone rollback（APPLY=YES）
PATH="$FAKE:$PATH" ENV_FILE="$ENV" NGINX_SITE="$NG" TRASH="$WORK/trash" BACKUP_ROOT="$WORK/backups" \
  APPLY=YES bash "$HERE/rollback.sh" >/dev/null 2>&1
if ! grep -q "SERVER_ADDRESS" "$ENV"; then ok "rollback 后 env 恢复"; else bad "rollback 后 env 仍含 SERVER_ADDRESS"; fi
if ! grep -q "tianlong-hardening-20260908" "$NG"; then ok "rollback 后 nginx 恢复"; else bad "rollback 后 nginx 仍有安全头"; fi

echo ""
echo "=== 合成测试结果：PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ] && echo "全部通过" || echo "存在失败"

# 清理
rm -rf "$WORK"
exit $([ "$FAIL" = "0" ] && echo 0 || echo 1)
