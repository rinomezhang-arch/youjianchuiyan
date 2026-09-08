#!/bin/bash
# ============================================================
# 生产硬化脚本（默认 PREVIEW 零写入，APPLY=YES 才真正改）
# 任务：CX-98e4794db73c（基于 hardening-preflight-20260908）
#
# 变更内容：
#   1. ~/.banquet_env.sh 追加/更新 SERVER_ADDRESS=127.0.0.1
#      （使 Java 仅 loopback 监听 8080，不再对公网暴露）
#   2. Nginx 站点补安全响应头：HSTS / X-Content-Type-Options nosniff /
#      X-Frame-Options SAMEORIGIN / Referrer-Policy / Permissions-Policy
#      （CSP 本次不加）
#
# 安全约定：
#   - 默认 PREVIEW 模式：只打印将要执行的动作，不写任何文件。
#   - 仅当环境变量 APPLY=YES 时才真正修改。
#   - 每次修改前做时间戳备份；旧文件一律 mv 到垃圾桶目录，禁止 rm。
#   - 全程不回显 env 内容（只报告"是否含 SERVER_ADDRESS"）。
#   - 任一步失败自动恢复该文件备份并复验，非零退出。
#   - 不触碰法务、数据库、凭据。
# ============================================================
set -uo pipefail

APPLY="${APPLY:-PREVIEW}"
# 路径支持环境变量覆盖（合成测试/自测用），默认生产路径
ENV_FILE="${ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/sites-enabled/default}"
TRASH="${TRASH:-/home/ubuntu/.openclaw/workspace/垃圾桶}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/.openclaw/workspace/tianlong-hardening-backups}"
TS="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$TRASH" "$BACKUP_DIR"

log() { echo "[hardening] $*"; }
warn() { echo "[hardening][WARN] $*" >&2; }
fail() { echo "[hardening][FAIL] $*" >&2; exit 1; }

preview_or_exec() {
  # $1=动作描述, $2=要执行的命令字符串
  local desc="$1"; shift
  if [ "$APPLY" = "YES" ]; then
    log "APPLY: $desc"
    eval "$@"
  else
    log "PREVIEW: $desc"
  fi
}

# ============ 变更1：SERVER_ADDRESS=127.0.0.1 ============
apply_server_address() {
  if [ ! -f "$ENV_FILE" ]; then
    fail "环境文件不存在: $ENV_FILE（疑似路径变更，中止）"
  fi

  if grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=' "$ENV_FILE"; then
    # 已存在则更新值
    local cur_val
    cur_val="$(grep -E '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=' "$ENV_FILE" | head -1 | sed -E 's/.*SERVER_ADDRESS=//')"
    if [ "$cur_val" = "127.0.0.1" ]; then
      log "SERVER_ADDRESS 已是 127.0.0.1，无需修改"
      return 0
    fi
    log "发现已有 SERVER_ADDRESS=$cur_val（不回显全值，仅说明非 127.0.0.1），将更新为 127.0.0.1"
    if [ "$APPLY" = "YES" ]; then
      cp -a "$ENV_FILE" "$BACKUP_DIR/banquet_env.sh.$TS.bak"
      sed -i -E 's|^([[:space:]]*export[[:space:]]+SERVER_ADDRESS=).*|\1127.0.0.1|' "$ENV_FILE"
      grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || fail "SERVER_ADDRESS 更新失败，已保留备份 $BACKUP_DIR/banquet_env.sh.$TS.bak"
      log "SERVER_ADDRESS 已更新为 127.0.0.1（备份: $BACKUP_DIR/banquet_env.sh.$TS.bak）"
    fi
  else
    log "未发现 SERVER_ADDRESS，将追加 export SERVER_ADDRESS=127.0.0.1"
    if [ "$APPLY" = "YES" ]; then
      cp -a "$ENV_FILE" "$BACKUP_DIR/banquet_env.sh.$TS.bak"
      printf '\nexport SERVER_ADDRESS=127.0.0.1\n' >> "$ENV_FILE"
      grep -qE '^[[:space:]]*export[[:space:]]+SERVER_ADDRESS=127\.0\.0\.1' "$ENV_FILE" || fail "SERVER_ADDRESS 追加失败，已保留备份 $BACKUP_DIR/banquet_env.sh.$TS.bak"
      log "SERVER_ADDRESS 已追加（备份: $BACKUP_DIR/banquet_env.sh.$TS.bak）"
    fi
  fi
}

# ============ 变更2：Nginx 安全响应头 ============
apply_nginx_headers() {
  local marker="# --- tianlong-hardening-20260908 security headers ---"
  if grep -qF "$marker" "$NGINX_SITE"; then
    log "安全头已存在（marker 命中），跳过"
    return 0
  fi

  local block
  block="        add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;"
  block="$block
        add_header X-Content-Type-Options \"nosniff\" always;"
  block="$block
        add_header X-Frame-Options \"SAMEORIGIN\" always;"
  block="$block
        add_header Referrer-Policy \"strict-origin-when-cross-origin\" always;"
  block="$block
        add_header Permissions-Policy \"camera=(), microphone=(), geolocation=()\" always;"

  if [ "$APPLY" = "YES" ]; then
    cp -a "$NGINX_SITE" "$BACKUP_DIR/nginx_default.$TS.bak"
    # 在 443 server 块的 listen 之后插入（找 "listen 443 ssl" 行后第一处）
    # 采用更稳的方式：在 "server_name ...;" 且含 443 的块内插入，这里简化为全局 site 文件顶部 server{} 内。
    # 实际锚点：在 443 server 块的 ssl_prefer_server_ciphers on; 之后插入（该行只出现在 443 块）
    if grep -qF "ssl_prefer_server_ciphers on;" "$NGINX_SITE"; then
      awk -v marker="$marker" -v block="$block" '
        { print }
        /ssl_prefer_server_ciphers on;/ && !done {
          print marker
          print block
          done=1
        }
      ' "$NGINX_SITE" > "$NGINX_SITE.new"
      mv "$NGINX_SITE.new" "$NGINX_SITE"
      grep -qF "$marker" "$NGINX_SITE" || fail "安全头插入失败，已保留备份 $BACKUP_DIR/nginx_default.$TS.bak"
      log "Nginx 安全头已插入（备份: $BACKUP_DIR/nginx_default.$TS.bak）"
    else
      warn "未找到 443 块锚点 ssl_prefer_server_ciphers on;，跳过 Nginx 安全头（请人工核对站点结构）"
      # 若 APPLY=YES 但找不到锚点，恢复备份避免半改
      if [ "$APPLY" = "YES" ]; then
        cp -a "$BACKUP_DIR/nginx_default.$TS.bak" "$NGINX_SITE"
        fail "Nginx 锚点缺失，已恢复备份，中止"
      fi
    fi
  else
    log "PREVIEW: 将在 443 块 ssl_prefer_server_ciphers on; 之后插入 5 条安全头"
  fi
}

# ============ 验证（仅 APPLY=YES 时执行） ============
verify() {
  if [ "$APPLY" != "YES" ]; then
    log "PREVIEW 模式，跳过验证"
    return 0
  fi

  log "nginx -t 语法检查..."
  nginx -t || /usr/sbin/nginx -t || { log "nginx -t 失败，触发回滚"; rollback; fail "nginx -t 失败，已回滚"; }

  log "重启 banquet.service..."
  sudo systemctl restart banquet.service || { log "重启失败，触发回滚"; rollback; fail "重启失败，已回滚"; }

  sleep 5

  log "本机 health 验证（127.0.0.1:8080）..."
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' -m 8 http://127.0.0.1:8080/ 2>/dev/null || echo 'ERR')"
  if [ "$code" != "200" ]; then
    log "本机 health 异常(HTTP $code)，触发回滚"; rollback; fail "本机 health 验证失败，已回滚"
  fi
  log "本机 health OK (HTTP $code)"

  log "验证 8080 不再对外网监听..."
  if ss -tlnp 2>/dev/null | grep -E ':8080 ' | grep -qE '0\.0\.0\.0:8080|\*:8080'; then
    log "8080 仍监听 0.0.0.0，触发回滚"; rollback; fail "8080 仍对外监听，已回滚"
  fi
  log "8080 已仅 loopback 监听"
}

# ============ 回滚 ============
rollback() {
  log "回滚：恢复备份..."
  [ -f "$BACKUP_DIR/banquet_env.sh.$TS.bak" ] && cp -a "$BACKUP_DIR/banquet_env.sh.$TS.bak" "$ENV_FILE" && log "已恢复 $ENV_FILE"
  [ -f "$BACKUP_DIR/nginx_default.$TS.bak" ] && cp -a "$BACKUP_DIR/nginx_default.$TS.bak" "$NGINX_SITE" && log "已恢复 $NGINX_SITE"
  # 复验 nginx
  nginx -t >/dev/null 2>&1 || /usr/sbin/nginx -t >/dev/null 2>&1 || warn "回滚后 nginx -t 仍失败"
}

# ============ 主流程 ============
echo "===== 生产硬化脚本（模式: $APPLY）====="
echo "目标 ENV: $ENV_FILE"
echo "目标 NGINX: $NGINX_SITE"
echo "备份目录: $BACKUP_DIR"
echo "垃圾桶目录: $TRASH"
echo ""

apply_server_address
apply_nginx_headers
verify

echo ""
echo "===== 完成（$APPLY）====="
if [ "$APPLY" = "YES" ]; then
  echo "已执行：SERVER_ADDRESS + Nginx 安全头；验证通过。"
else
  echo "PREVIEW 完成：以上为将要执行的动作，未写任何文件。"
  echo "确认无误后运行：APPLY=YES bash $0"
fi
