#!/bin/bash
# ============================================================
# 生产硬化脚本（默认 PREVIEW 零写，APPLY=YES 才真正改）
# 任务：CX-46b368b69675（补充审查要点，合并 e869c96 整改）
#
# 变更：
#   1. ~/.banquet_env.sh 追加/更新 export SERVER_ADDRESS=127.0.0.1
#   2. Nginx 443 ssl 块补 5 条安全响应头（HSTS/nosniff/X-Frame-Options/Referrer-Policy/Permissions-Policy，不含 CSP）
#
# 安全约定（含 Codex 6 点补充）：
#   - PREVIEW 零写：不 mkdir、不写任何文件。
#   - set -euo pipefail + EXIT trap 统一回滚；写失败立即回滚并复验运行态。
#   - umask 077；env 备份强制 chmod 600。
#   - nginx 入口：解析 symlink 真实目标(readlink -f)，校验 owner/mode，原子 install 到真实目标，
#     绝不用可预测 .new 替换 symlink。
#   - HTTPS 安全头锚点：仅在 listen 443 ssl 的 server 块内插入。
#   - 端口检查覆盖 IPv4+IPv6；用非 loopback 地址实测 curl 8080 失败。
#   - 批次 ID 用纳秒+随机；env/nginx 备份同一 manifest 成对恢复。
#   - 禁止 rm（旧文件/临时文件 mv 垃圾桶）；不回显 SERVER_ADDRESS 现值。
# ============================================================
set -euo pipefail

APPLY="${APPLY:-PREVIEW}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/sites-enabled/default}"
TRASH="${TRASH:-/home/ubuntu/.openclaw/workspace/垃圾桶}"
BACKUP_ROOT="${BACKUP_ROOT:-/home/ubuntu/.openclaw/workspace/tianlong-hardening-backups}"
RUN_ID="$(date +%Y%m%d_%H%M%S)_${RANDOM}${RANDOM}"
DOMAIN="youjianchuiyan.com"

# 非 loopback 地址（IPv4 + 全局 IPv6）
SELF_IPV4="$(hostname -I 2>/dev/null | tr ' ' '\n' | grep -vE '^127\.|^::1' | grep -E '^[0-9]+\.' | head -3 | tr '\n' ' ')"
SELF_IPV6="$(ip -6 addr show scope global 2>/dev/null | grep -oE 'inet6 [0-9a-f:]+' | awk '{print $2}' | grep -v '^::1$' | head -3 | tr '\n' ' ')"
GLOBAL6_IPV4_PREFIX="fd7a:115c:a1e0::"

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
    cp -a "$BACKUP_DIR/env.bak" "$ENV_FILE" 2>/dev/null && chmod 600 "$ENV_FILE" 2>/dev/null && log "已恢复 env" || warn "env 恢复失败"
  fi
  if [ "$NGINX_CHANGED" = "1" ] && [ -n "$BACKUP_DIR" ]; then
    local real; real="$(readlink -f "$NGINX_SITE" 2>/dev/null || echo "$NGINX_SITE")"
    local mode; mode="$(stat -c '%a' "$real" 2>/dev/null || echo 644)"
    local own; own="$(stat -c '%U' "$real" 2>/dev/null || echo root)"
    local grp; grp="$(stat -c '%G' "$real" 2>/dev/null || echo root)"
    sudo install -o "$own" -g "$grp" -m "$mode" "$BACKUP_DIR/nginx.bak" "$real" 2>/dev/null && log "已恢复 nginx" || warn "nginx 恢复失败"
  fi
  # 复验运行态：重启 banquet + reload nginx + nginx -t
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
  log "PREVIEW: 将在 nginx listen 443 ssl 块内插入 5 条安全头（不含 CSP）"
  log "PREVIEW: 验证序列 = nginx -t → reload nginx → restart banquet → 本机 health 200 → 非loopback(IPv4+IPv6) curl 8080 失败 → HTTPS 检查 5 头"
  log "确认后执行：APPLY=YES bash $0"
  exit 0
fi

# ---------- APPLY ----------
log "APPLY 模式，run_id=$RUN_ID"
umask 077
BACKUP_DIR="$BACKUP_ROOT/$RUN_ID"
mkdir -p "$BACKUP_DIR" "$TRASH"
log "备份目录: $BACKUP_DIR"

# ---- 变更1：SERVER_ADDRESS=127.0.0.1 ----
if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=' "$ENV_FILE" 2>/dev/null; then
  if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE"; then
    log "SERVER_ADDRESS 已是 127.0.0.1，跳过"
  else
    log "SERVER_ADDRESS 存在但非 127.0.0.1（值不回显），将更新"
    cp -a "$ENV_FILE" "$BACKUP_DIR/env.bak"
    chmod 600 "$BACKUP_DIR/env.bak"
    sed -i -E 's|^([[:space:]]*export[[:space:]]+SERVER_ADDRESS=).*|\1127.0.0.1|' "$ENV_FILE"
    grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || { echo "SERVER_ADDRESS 更新失败" >&2; exit 1; }
    ENV_CHANGED=1
    log "SERVER_ADDRESS 已更新为 127.0.0.1"
  fi
else
  log "未发现 SERVER_ADDRESS，将追加"
  cp -a "$ENV_FILE" "$BACKUP_DIR/env.bak"
  chmod 600 "$BACKUP_DIR/env.bak"
  printf '\nexport SERVER_ADDRESS=127.0.0.1\n' >> "$ENV_FILE"
  grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || { echo "SERVER_ADDRESS 追加失败" >&2; exit 1; }
  ENV_CHANGED=1
  log "SERVER_ADDRESS 已追加"
fi

# ---- 变更2：Nginx 安全头（解析 symlink 真实目标 + 443 ssl 块锚点） ----
MARKER="# --- tianlong-hardening-20260908 security headers ---"
NGINX_REAL="$(readlink -f "$NGINX_SITE" 2>/dev/null || echo "$NGINX_SITE")"
log "nginx 真实目标: $NGINX_REAL"

if grep -qF "$MARKER" "$NGINX_REAL"; then
  log "安全头已存在（marker 命中），跳过"
else
  TMP_NGINX="$(mktemp)"
  # awk 状态机：只在 listen 443 ssl 的 server 块内、且 ssl_prefer_server_ciphers on 之后插入安全头
  awk -v marker="$MARKER" '
    /^[[:space:]]*server[[:space:]]*\{/ { in_server=1; is_443=0; print; next }
    /^[[:space:]]*\}/ { if (in_server) { in_server=0; is_443=0 } ; print; next }
    in_server && /listen[[:space:]]+.*443[[:space:]]+ssl/ { is_443=1 }
    in_server && is_443 && /ssl_prefer_server_ciphers[[:space:]]+on[[:space:]]*;/ && !done {
      print
      print "        " marker
      print "        add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;"
      print "        add_header X-Content-Type-Options \"nosniff\" always;"
      print "        add_header X-Frame-Options \"SAMEORIGIN\" always;"
      print "        add_header Referrer-Policy \"strict-origin-when-cross-origin\" always;"
      print "        add_header Permissions-Policy \"camera=(), microphone=(), geolocation=()\" always;"
      done=1
      next
    }
    { print }
  ' "$NGINX_REAL" > "$TMP_NGINX"

  grep -qF "$MARKER" "$TMP_NGINX" || { mv "$TMP_NGINX" "$TRASH/nginx_tmp_fail.$RUN_ID"; echo "安全头生成失败（443 ssl 块锚点未命中）" >&2; exit 1; }

  cp -a "$NGINX_REAL" "$BACKUP_DIR/nginx.bak"
  local_mode="$(stat -c '%a' "$NGINX_REAL" 2>/dev/null || echo 644)"
  local_own="$(stat -c '%U' "$NGINX_REAL" 2>/dev/null || echo root)"
  local_grp="$(stat -c '%G' "$NGINX_REAL" 2>/dev/null || echo root)"
  sudo install -o "$local_own" -g "$local_grp" -m "$local_mode" "$TMP_NGINX" "$NGINX_REAL" || { mv "$TMP_NGINX" "$TRASH/nginx_tmp_fail.$RUN_ID"; echo "nginx install 失败" >&2; exit 1; }
  mv "$TMP_NGINX" "$TRASH/nginx_tmp.$RUN_ID"
  NGINX_CHANGED=1
  log "Nginx 安全头已写入真实目标（owner=$local_own group=$local_grp mode=$local_mode）"
fi

# ---- 写 manifest ----
{
  echo "run_id=$RUN_ID"
  echo "env_changed=$ENV_CHANGED"
  echo "nginx_changed=$NGINX_CHANGED"
  echo "env_file=$ENV_FILE"
  echo "nginx_file=$NGINX_SITE"
  echo "nginx_real=$NGINX_REAL"
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

log "端口检查：8080 不应监听 0.0.0.0 或 [::]（IPv4+IPv6）..."
if ss -tln 2>/dev/null | grep ':8080 ' | grep -qE '0\.0\.0\.0:8080|\*:8080|\[::\]:8080'; then
  echo "8080 仍对外监听（IPv4 或 IPv6）" >&2; exit 1
fi
log "8080 未对外监听"

log "非 loopback 主动 curl 8080 应失败（IPv4）..."
for ip in $SELF_IPV4; do
  if curl -s -o /dev/null -m 5 "http://$ip:8080/" 2>/dev/null; then
    echo "非 loopback IPv4($ip) curl 8080 意外可达" >&2; exit 1
  fi
done
log "非 loopback IPv4 curl 8080 全部失败（符合预期）"

log "非 loopback 主动 curl 8080 应失败（IPv6）..."
for ip in $SELF_IPV6; do
  if curl -s -o /dev/null -m 5 "http://[$ip]:8080/" 2>/dev/null; then
    echo "非 loopback IPv6($ip) curl 8080 意外可达" >&2; exit 1
  fi
done
log "非 loopback IPv6 curl 8080 全部失败（符合预期）"

log "HTTPS 检查 5 个响应头..."
HEADERS="$(curl -sI -m 8 "https://$DOMAIN/" 2>/dev/null | tr -d '\r')"
for h in "Strict-Transport-Security" "X-Content-Type-Options" "X-Frame-Options" "Referrer-Policy" "Permissions-Policy"; do
  echo "$HEADERS" | grep -qi "^$h:" || { echo "缺少响应头 $h" >&2; exit 1; }
done
log "5 个安全响应头全部就位"

log "硬化完成（run_id=$RUN_ID）"
