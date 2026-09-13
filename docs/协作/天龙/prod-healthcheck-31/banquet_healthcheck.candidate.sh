#!/bin/bash
# ============================================================================
# NOT APPLIED —— TL-PROD-HEALTHCHECK-31 最小补丁候选 v3（仅文档候选，未应用到生产）
#
# 原脚本：/home/ubuntu/banquet_healthcheck.sh
#         sha256 d0df91f676679bbb9215ba76da0f26c2852bde67d113f98b63599ddfc0ca4688（56 行）
# 本文件位置：docs/协作/天龙/prod-healthcheck-31/banquet_healthcheck.candidate.sh
#
# v2 修正（Codex R1 退回）：
#   1. 去掉全部 mktemp/rm：不再创建临时文件，body 与状态码一次取回后拆分，无任何物理删除动作；
#   2. 404 分支除告警外**清空连续失败计数**；
#   3. 全部外部命令与路径可用环境变量覆盖，便于离线分支测试。
# v3 修正（Codex R2 退回）：
#   4. 健康分支**只清失败计数，不再清空一小时重启历史**（`: > "$RESTART_STATE"` 已删除）；
#      重启历史文件保留，统计时按最近一小时过滤 —— 否则重启后下一轮一健康，限流就被归零绕过。
#
# 应用前必须先备份原脚本：
#   cp -a /home/ubuntu/banquet_healthcheck.sh /home/ubuntu/banquet_healthcheck.sh.bak-20260913
# 一条回退命令：
#   sudo cp -a /home/ubuntu/banquet_healthcheck.sh.bak-20260913 /home/ubuntu/banquet_healthcheck.sh
# ============================================================================
set -uo pipefail

URL="${BANQUET_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
STATE="${BANQUET_HEALTH_STATE:-/home/ubuntu/.banquet_health_fail}"
LOCK="${BANQUET_HEALTH_LOCK:-/home/ubuntu/.banquet_health.lock}"
RESTART_STATE="${BANQUET_HEALTH_RESTART_STATE:-/home/ubuntu/.banquet_health_restarts}"
IS_ACTIVE_CMD="${BANQUET_IS_ACTIVE_CMD:-systemctl is-active banquet}"
RESTART_CMD="${BANQUET_RESTART_CMD:-sudo systemctl restart banquet}"
MAX_FAILS="${BANQUET_MAX_FAILS:-3}"
MAX_RESTARTS_PER_HOUR="${BANQUET_MAX_RESTARTS_PER_HOUR:-2}"
COOLDOWN_SECONDS="${BANQUET_COOLDOWN_SECONDS:-900}"
RESTART_WAIT="${BANQUET_RESTART_WAIT:-30}"
LOG_TS="$(date '+%F %T')"
WECOM_WEBHOOK="${BANQUET_ALERT_WEBHOOK:-}"

notify() {
    local msg="$1"
    echo "$LOG_TS  $msg"
    if [ -n "$WECOM_WEBHOOK" ]; then
        curl -s -m 10 -H 'Content-Type: application/json' \
             -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[又见炊烟后端] $msg\"}}" \
             "$WECOM_WEBHOOK" > /dev/null
    fi
}

# 并发锁：避免 cron 重叠执行；锁文件只创建不删除（可恢复）
exec 9>"$LOCK" || exit 0
flock -n 9 || { echo "$LOG_TS  上一次检查仍在运行，跳过本轮"; exit 0; }

# 修正 1：无临时文件。body 与状态码一次取回；约定最后一行是状态码，其余是 body。
resp="$(curl -s -w '\n%{http_code}' --max-time 8 "$URL")"
code="$(printf '%s\n' "$resp" | tail -n1)"
body="$(printf '%s\n' "$resp" | sed '$d')"

# 404 = 检查器配置错误：只告警、清计数、绝不重启
if [ "$code" = "404" ]; then
    : > "$STATE"
    notify "健康检查 URL 返回 404（检查器配置可能已过期）：$URL —— 按策略只告警不重启，已清空连续失败计数"
    exit 0
fi

# 判定以 JSON status=UP 为准，不只看 HTTP 200
if [ "$code" = "200" ] && printf '%s' "$body" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    if [ -s "$STATE" ]; then
        notify "已恢复正常（此前连续 $(cat "$STATE") 次检查失败）"
    fi
    # v3：只清失败计数；重启历史按最近一小时过滤，绝不清空（否则限流可被绕过）
    : > "$STATE"
    exit 0
fi

fails=$(( $(cat "$STATE" 2>/dev/null || echo 0) + 1 ))
echo "$fails" > "$STATE"
notify "健康检查失败 第 ${fails} 次，HTTP=${code}"
[ "$fails" -ge "$MAX_FAILS" ] || exit 0

# 重启前先看单元与进程：单元 active 且端点返回 200（仅状态字段异常）时不重启
unit_active="$(bash -c "$IS_ACTIVE_CMD" 2>/dev/null || echo unknown)"
if [ "$unit_active" = "active" ] && [ "$code" = "200" ]; then
    notify "单元 active 且端点返回 200，仅状态字段异常：不重启，等待人工确认"
    exit 0
fi

# 重启频次与冷却
now="$(date +%s)"
recent="$(awk -v now="$now" '$1 > now-3600 {print $1}' "$RESTART_STATE" 2>/dev/null | wc -l)"
last="$(tail -1 "$RESTART_STATE" 2>/dev/null || echo 0)"
if [ "$recent" -ge "$MAX_RESTARTS_PER_HOUR" ]; then
    notify "本小时已重启 ${recent} 次，达到上限 ${MAX_RESTARTS_PER_HOUR}，本轮不重启，需要人工介入"
    exit 0
fi
if [ -n "$last" ] && [ "$last" -gt 0 ] && [ $(( now - last )) -lt "$COOLDOWN_SECONDS" ]; then
    notify "距上次重启不足 $(( COOLDOWN_SECONDS / 60 )) 分钟，处于冷却期，本轮不重启"
    exit 0
fi

notify "连续 ${fails} 次失败且单元状态=${unit_active}，正在重启服务"
echo "$now" >> "$RESTART_STATE"
bash -c "$RESTART_CMD"
sleep "$RESTART_WAIT"

# 重启后复检：同样无临时文件
resp2="$(curl -s -w '\n%{http_code}' --max-time 8 "$URL")"
after="$(printf '%s\n' "$resp2" | tail -n1)"
after_body="$(printf '%s\n' "$resp2" | sed '$d')"
after_ok="no"
if [ "$after" = "200" ] && printf '%s' "$after_body" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    after_ok="yes"
fi
if [ "$after_ok" = "yes" ]; then
    notify "重启后已恢复"
    : > "$STATE"
else
    notify "重启后仍不健康（HTTP=${after}），需要人工介入：查看 /home/ubuntu/backend.out"
fi
