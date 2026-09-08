#!/bin/bash
# ============================================================
# 生产硬化回滚脚本（默认 PREVIEW 零写，APPLY=YES 才真正回滚）
# 任务：CX-46b368b69675（补充审查要点：symlink 真实目标 + 同一 manifest 成对恢复）
#
# 行为：
#   - 读取最近一次 run 的 manifest，按同一 manifest 成对恢复 env + nginx。
#   - nginx 入口解析 symlink 真实目标(readlink -f)后再 install 恢复。
#   - 旧现场文件 mv 到垃圾桶（非 cp）。
#   - 恢复后自动 restart banquet + reload nginx + nginx -t + 本机 health。
#   - PREVIEW 零写；APPLY=YES 才执行。
# ============================================================
set -euo pipefail

APPLY="${APPLY:-PREVIEW}"
TRASH="${TRASH:-/home/ubuntu/.openclaw/workspace/垃圾桶}"
BACKUP_ROOT="${BACKUP_ROOT:-/home/ubuntu/.openclaw/workspace/tianlong-hardening-backups}"
TS="$(date +%Y%m%d_%H%M%S)_${RANDOM}${RANDOM}"

log(){ echo "[rollback] $*"; }
warn(){ echo "[rollback][WARN] $*" >&2; }

nginx_t() { /usr/sbin/nginx -t 2>&1 || sudo /usr/sbin/nginx -t 2>&1; }

# 取最近一次 run 目录（含 manifest）
LATEST_RUN="$(ls -1dt "$BACKUP_ROOT"/20* 2>/dev/null | head -1)"

if [ -z "$LATEST_RUN" ] || [ ! -f "$LATEST_RUN/manifest.txt" ]; then
  warn "无可用 manifest（$BACKUP_ROOT 下无 run 目录或 manifest），无法成对恢复，中止"
  exit 1
fi

ENV_FILE=""; NGINX_FILE=""; NGINX_REAL=""
ENV_CHANGED=0; NGINX_CHANGED=0
ENV_BACKUP=""; NGINX_BACKUP=""
while IFS='=' read -r k v; do
  case "$k" in
    env_file) ENV_FILE="$v";;
    nginx_file) NGINX_FILE="$v";;
    nginx_real) NGINX_REAL="$v";;
    env_changed) ENV_CHANGED="$v";;
    nginx_changed) NGINX_CHANGED="$v";;
    env_backup) ENV_BACKUP="$v";;
    nginx_backup) NGINX_BACKUP="$v";;
  esac
done < "$LATEST_RUN/manifest.txt"

log "回滚目标 run: $(basename "$LATEST_RUN")"
log "env_changed=$ENV_CHANGED nginx_changed=$NGINX_CHANGED"

if [ "$APPLY" != "YES" ]; then
  log "PREVIEW 模式：不写任何文件。"
  [ "$ENV_CHANGED" = "1" ] && log "PREVIEW: 将恢复 env（$ENV_FILE，旧现场 mv 垃圾桶）"
  [ "$NGINX_CHANGED" = "1" ] && log "PREVIEW: 将恢复 nginx（$NGINX_FILE → 真实目标 ${NGINX_REAL:-未记录}，旧现场 mv 垃圾桶，install 保持 owner/mode）"
  log "PREVIEW: 恢复后 restart banquet + reload nginx + nginx -t + 本机 health"
  log "确认后执行：APPLY=YES bash $0"
  exit 0
fi

umask 077
mkdir -p "$TRASH"

# ---- 恢复 env ----
if [ "$ENV_CHANGED" = "1" ] && [ -f "$LATEST_RUN/$ENV_BACKUP" ]; then
  mv "$ENV_FILE" "$TRASH/banquet_env.sh.before_rollback.$TS"
  cp -a "$LATEST_RUN/$ENV_BACKUP" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  log "已恢复 env（旧现场已移垃圾桶，mode 600）"
fi

# ---- 恢复 nginx（解析真实目标） ----
if [ "$NGINX_CHANGED" = "1" ] && [ -f "$LATEST_RUN/$NGINX_BACKUP" ]; then
  real="${NGINX_REAL:-$(readlink -f "$NGINX_FILE" 2>/dev/null || echo "$NGINX_FILE")}"
  local_mode="$(stat -c '%a' "$real" 2>/dev/null || echo 644)"
  local_own="$(stat -c '%U' "$real" 2>/dev/null || echo root)"
  local_grp="$(stat -c '%G' "$real" 2>/dev/null || echo root)"
  mv "$real" "$TRASH/nginx_default.before_rollback.$TS"
  sudo install -o "$local_own" -g "$local_grp" -m "$local_mode" "$LATEST_RUN/$NGINX_BACKUP" "$real"
  log "已恢复 nginx 真实目标（旧现场已移垃圾桶，保持 owner/mode）"
fi

# ---- 恢复后自动重启/reload/复验 ----
log "nginx -t 复验..."
nginx_t || { echo "回滚后 nginx -t 失败" >&2; exit 1; }

log "restart banquet + reload nginx..."
sudo systemctl restart banquet.service || warn "banquet 重启失败"
sudo systemctl reload nginx || warn "nginx reload 失败"
sleep 5

log "本机 health 复验（127.0.0.1:8080）..."
code="$(curl -s -o /dev/null -w '%{http_code}' -m 8 http://127.0.0.1:8080/ 2>/dev/null || echo ERR)"
[ "$code" = "200" ] || { echo "回滚后本机 health 异常 HTTP $code" >&2; exit 1; }
log "回滚完成，本机 health OK（HTTP $code）"
