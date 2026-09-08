#!/bin/bash
# ============================================================
# 生产硬化脚本（默认 PREVIEW 零写，APPLY=YES 才真正改）
# 任务：CX-bc0eac9427cf（返工版，逐条满足 Codex 8 点修正）
#
# 变更：
#   1. ~/.banquet_env.sh 追加/更新 export SERVER_ADDRESS=127.0.0.1
#   2. Nginx 443 块补 5 条安全响应头（HSTS/nosniff/X-Frame-Options/Referrer-Policy/Permissions-Policy，不含 CSP）
#
# 安全约定：
#   - PREVIEW 零写：不 mkdir、不写任何文件、不建目录。
#   - set -euo pipefail + EXIT trap 统一回滚；所有写失败立即回滚。
#   - 写 /etc/nginx 用 sudo install 保持 owner/mode（临时文件 + install，非同目录 .new + mv）。
#   - 应用安全头后 nginx -t → reload nginx → 真实 HTTPS 检查 5 头。
#   - 任一步失败：恢复 env+nginx → 重启 banquet → reload nginx → 复验旧状态。
#   - 不回显 SERVER_ADDRESS 现值。
#   - 验证 127.0.0.1 监听 + 非 loopback 主动 curl 8080 失败。
#   - 禁止 rm（旧文件/临时文件 mv 垃圾桶）。
# ============================================================
set -euo pipefail

APPLY="${APPLY:-PREVIEW}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/sites-enabled/default}"
TRASH="${TRASH:-/home/ubuntu/.openclaw/workspace/垃圾桶}"
BACKUP_ROOT="${BACKUP_ROOT:-/home/ubuntu/.openclaw/workspace/tianlong-hardening-backups}"
RUN_ID="$(date +%Y%m%d_%H%M%S)"
SELF_IPS="$(hostname -I 2>/dev/null | tr ' ' '\n' | grep -vE '^127\.|^::1' | head -3 | tr '\n' ' ')"
DOMAIN="youjianchuiyan.com"

# 全局状态（EXIT trap 回滚用）
BACKUP_DIR=""
ENV_CHANGED=0
NGINX_CHANGED=0

log(){ echo "[harden] $*"; }
warn(){ echo "[harden][WARN] $*" >&2; }

nginx_t() { /usr/sbin/nginx -t 2>&1 || sudo /usr/sbin/nginx -t 2>&1; }

# ---------- EXIT trap 统一回滚 ----------
rollback_all(){
  set +e
  log "触发统一回滚..."
  if [ "$ENV_CHANGED" = "1" ] && [ -n "$BACKUP_DIR" ]; then
    cp -a "$BACKUP_DIR/env.bak" "$ENV_FILE" 2>/dev/null && log "已恢复 env" || warn "env 恢复失败"
  fi
  if [ "$NGINX_CHANGED" = "1" ] && [ -n "$BACKUP_DIR" ]; then
    local mode; mode="$(stat -c '%a' "$NGINX_SITE" 2>/dev/null || echo 644)"
    local own; own="$(stat -c '%U' "$NGINX_SITE" 2>/dev/null || echo root)"
    local grp; grp="$(stat -c '%G' "$NGINX_SITE" 2>/dev/null || echo root)"
    sudo install -o "$own" -g "$grp" -m "$mode" "$BACKUP_DIR/nginx.bak" "$NGINX_SITE" 2>/dev/null && log "已恢复 nginx" || warn "nginx 恢复失败"
  fi
  # 复验旧状态：重启 banquet + reload nginx + nginx -t
  sudo systemctl restart banquet.service 2>/dev/null || warn "banquet 重启失败"
  sudo systemctl reload nginx 2>/dev/null || warn "nginx reload 失败"
  nginx_t >/dev/null 2>&1 && log "回滚后 nginx -t 复验通过" || warn "回滚后 nginx -t 仍失败"
  set -e
}
trap 'rc=$?; if [ "$APPLY" = "YES" ] && [ "$rc" -ne 0 ]; then rollback_all; fi; exit $rc' EXIT

# ---------- PREVIEW 零写 ----------
if [ "$APPLY" != "YES" ]; then
  log "PREVIEW 模式：不写任何文件、不建目录。"
  if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=' "$ENV_FILE" 2>/dev/null; then
    log "PREVIEW: SERVER_ADDRESS 已存在（现值不回显），将更新为 127.0.0.1"
  else
    log "PREVIEW: 将追加 export SERVER_ADDRESS=127.0.0.1 到 $ENV_FILE"
  fi
  log "PREVIEW: 将在 nginx 443 块插入 5 条安全头（HSTS/nosniff/X-Frame-Options/Referrer-Policy/Permissions-Policy，不含 CSP）"
  log "PREVIEW: 验证序列 = nginx -t → reload nginx → restart banquet → 本机 health 200 → 非loopback curl 8080 失败 → HTTPS 检查 5 头"
  log "确认后执行：APPLY=YES bash $0"
  exit 0
fi

# ---------- APPLY ----------
log "APPLY 模式，run_id=$RUN_ID"
BACKUP_DIR="$BACKUP_ROOT/$RUN_ID"
mkdir -p "$BACKUP_DIR" "$TRASH"
log "备份目录: $BACKUP_DIR"

# ---- 变更1：SERVER_ADDRESS=127.0.0.1 ----
if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=' "$ENV_FILE" 2>/dev/null; then
  # 已存在：判断是否为 127.0.0.1（不回显现值）
  if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE"; then
    log "SERVER_ADDRESS 已是 127.0.0.1，跳过"
  else
    log "SERVER_ADDRESS 存在但非 127.0.0.1（值不回显），将更新"
    cp -a "$ENV_FILE" "$BACKUP_DIR/env.bak"
    sed -i -E 's|^([[:space:]]*export[[:space:]]+SERVER_ADDRESS=).*|\1127.0.0.1|' "$ENV_FILE"
    grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || { echo "SERVER_ADDRESS 更新失败" >&2; exit 1; }
    ENV_CHANGED=1
    log "SERVER_ADDRESS 已更新为 127.0.0.1"
  fi
else
  log "未发现 SERVER_ADDRESS，将追加"
  cp -a "$ENV_FILE" "$BACKUP_DIR/env.bak"
  printf '\nexport SERVER_ADDRESS=127.0.0.1\n' >> "$ENV_FILE"
  grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || { echo "SERVER_ADDRESS 追加失败" >&2; exit 1; }
  ENV_CHANGED=1
  log "SERVER_ADDRESS 已追加"
fi

# ---- 变更2：Nginx 安全头 ----
MARKER="# --- tianlong-hardening-20260908 security headers ---"
if grep -qF "$MARKER" "$NGINX_SITE"; then
  log "安全头已存在（marker 命中），跳过"
else
  TMP_NGINX="$(mktemp)"
  awk -v marker="$MARKER" '
    { print }
    /ssl_prefer_server_ciphers on;/ && !done {
      print "        " marker
      print "        add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;"
      print "        add_header X-Content-Type-Options \"nosniff\" always;"
      print "        add_header X-Frame-Options \"SAMEORIGIN\" always;"
      print "        add_header Referrer-Policy \"strict-origin-when-cross-origin\" always;"
      print "        add_header Permissions-Policy \"camera=(), microphone=(), geolocation=()\" always;"
      done=1
    }
  ' "$NGINX_SITE" > "$TMP_NGINX"

  # 校验生成结果包含 marker，否则失败
  grep -qF "$MARKER" "$TMP_NGINX" || { mv "$TMP_NGINX" "$TRASH/nginx_tmp_fail.$RUN_ID"; echo "安全头生成失败" >&2; exit 1; }

  # 备份原站点
  cp -a "$NGINX_SITE" "$BACKUP_DIR/nginx.bak"
  # 记录 owner/mode 并用 sudo install 写回（保持 owner/mode，非同目录 .new+mv）
  local_mode="$(stat -c '%a' "$NGINX_SITE" 2>/dev/null || echo 644)"
  local_own="$(stat -c '%U' "$NGINX_SITE" 2>/dev/null || echo root)"
  local_grp="$(stat -c '%G' "$NGINX_SITE" 2>/dev/null || echo root)"
  sudo install -o "$local_own" -g "$local_grp" -m "$local_mode" "$TMP_NGINX" "$NGINX_SITE" || { mv "$TMP_NGINX" "$TRASH/nginx_tmp_fail.$RUN_ID"; echo "nginx install 失败" >&2; exit 1; }
  # 临时文件移垃圾桶（不 rm）
  mv "$TMP_NGINX" "$TRASH/nginx_tmp.$RUN_ID"
  NGINX_CHANGED=1
  log "Nginx 安全头已写入（保持 owner=$local_own group=$local_grp mode=$local_mode）"
fi

# ---- 写 manifest（供 standalone rollback 用） ----
{
  echo "run_id=$RUN_ID"
  echo "env_changed=$ENV_CHANGED"
  echo "nginx_changed=$NGINX_CHANGED"
  echo "env_file=$ENV_FILE"
  echo "nginx_file=$NGINX_SITE"
  echo "env_backup=env.bak"
  echo "nginx_backup=nginx.bak"
} > "$BACKUP_DIR/manifest.txt"

# ---- 验证序列 ----
log "nginx -t..."
nginx_t || { echo "nginx -t 失败" >&2; exit 1; }

log "reload nginx..."
sudo systemctl reload nginx || { echo "reload nginx 失败" >&2; exit 1; }

log "restart banquet..."
sudo systemctl restart banquet.service || { echo "restart banquet 失败" >&2; exit 1; }
sleep 5

log "本机 health 验证（127.0.0.1:8080）..."
code="$(curl -s -o /dev/null -w '%{http_code}' -m 8 http://127.0.0.1:8080/ 2>/dev/null || echo ERR)"
[ "$code" = "200" ] || { echo "本机 health 异常 HTTP $code" >&2; exit 1; }
log "本机 health OK"

log "验证 8080 仅监听 127.0.0.1..."
if ss -tlnp 2>/dev/null | grep ':8080 ' | grep -qE '0\.0\.0\.0:8080|\*:8080'; then
  echo "8080 仍监听 0.0.0.0" >&2; exit 1
fi
log "8080 未监听 0.0.0.0"

log "非 loopback 主动 curl 8080 应失败..."
for ip in $SELF_IPS; do
  if curl -s -o /dev/null -m 5 "http://$ip:8080/" 2>/dev/null; then
    echo "非 loopback($ip) curl 8080 意外可达" >&2; exit 1
  fi
done
log "非 loopback 主动 curl 8080 全部失败（符合预期）"

log "HTTPS 检查 5 个响应头..."
HEADERS="$(curl -sI -m 8 "https://$DOMAIN/" 2>/dev/null | tr -d '\r')"
for h in "Strict-Transport-Security" "X-Content-Type-Options" "X-Frame-Options" "Referrer-Policy" "Permissions-Policy"; do
  echo "$HEADERS" | grep -qi "^$h:" || { echo "缺少响应头 $h" >&2; exit 1; }
done
log "5 个安全响应头全部就位"

log "硬化完成（run_id=$RUN_ID）"
