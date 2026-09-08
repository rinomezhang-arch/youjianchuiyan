#!/bin/bash
# ============================================================
# 生产硬化回滚脚本（默认 PREVIEW 零写入，APPLY=YES 才真正回滚）
# 任务：CX-98e4794db73c
#
# 回滚内容：
#   1. 恢复 ~/.banquet_env.sh 最近一次时间戳备份（去掉 SERVER_ADDRESS 改动）
#   2. 恢复 /etc/nginx/sites-enabled/default 最近一次时间戳备份
#
# 安全约定：
#   - 默认 PREVIEW：只打印将恢复的备份路径，不写任何文件。
#   - APPLY=YES 才真正恢复。
#   - 旧文件一律 mv 到垃圾桶，禁止 rm。
#   - 恢复后 nginx -t 复验。
# ============================================================
set -uo pipefail

APPLY="${APPLY:-PREVIEW}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/sites-enabled/default}"
TRASH="${TRASH:-/home/ubuntu/.openclaw/workspace/垃圾桶}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/.openclaw/workspace/tianlong-hardening-backups}"
TS="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$TRASH"

log() { echo "[rollback] $*"; }
warn() { echo "[rollback][WARN] $*" >&2; }
fail() { echo "[rollback][FAIL] $*" >&2; exit 1; }

# 取最近一次备份
latest_env="$(ls -1t "$BACKUP_DIR"/banquet_env.sh.*.bak 2>/dev/null | head -1)"
latest_nginx="$(ls -1t "$BACKUP_DIR"/nginx_default.*.bak 2>/dev/null | head -1)"

echo "===== 生产硬化回滚脚本（模式: $APPLY）====="
echo "ENV 备份: ${latest_env:-（无备份）}"
echo "NGINX 备份: ${latest_nginx:-（无备份）}"
echo ""

rollback_env() {
  if [ -z "$latest_env" ]; then
    warn "无 ENV 备份，跳过（需人工检查 ~/.banquet_env.sh 是否残留 SERVER_ADDRESS）"
    return 0
  fi
  if [ "$APPLY" = "YES" ]; then
    # 旧文件移垃圾桶
    cp -a "$ENV_FILE" "$TRASH/banquet_env.sh.before_rollback.$TS" 2>/dev/null
    cp -a "$latest_env" "$ENV_FILE"
    log "已恢复 ENV: $latest_env"
  else
    log "PREVIEW: 将用 $latest_env 恢复 $ENV_FILE"
  fi
}

rollback_nginx() {
  if [ -z "$latest_nginx" ]; then
    warn "无 NGINX 备份，跳过"
    return 0
  fi
  if [ "$APPLY" = "YES" ]; then
    cp -a "$NGINX_SITE" "$TRASH/nginx_default.before_rollback.$TS" 2>/dev/null
    cp -a "$latest_nginx" "$NGINX_SITE"
    log "已恢复 NGINX: $latest_nginx"
    nginx -t >/dev/null 2>&1 || /usr/sbin/nginx -t >/dev/null 2>&1 || fail "回滚后 nginx -t 失败"
    log "nginx -t 复验通过"
  else
    log "PREVIEW: 将用 $latest_nginx 恢复 $NGINX_SITE"
  fi
}

rollback_env
rollback_nginx

echo ""
if [ "$APPLY" = "YES" ]; then
  echo "回滚完成。建议重启服务：sudo systemctl restart banquet.service && sudo systemctl reload nginx"
else
  echo "PREVIEW 完成：以上为将要回滚的动作，未写任何文件。"
  echo "确认后运行：APPLY=YES bash $0"
fi
