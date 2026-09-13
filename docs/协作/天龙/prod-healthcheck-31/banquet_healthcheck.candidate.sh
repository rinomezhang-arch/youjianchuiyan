#!/bin/bash
# ============================================================================
# NOT APPLIED —— TL-PROD-HEALTHCHECK-31 最小补丁候选（仅文档候选，未应用到生产）
#
# 原脚本：/home/ubuntu/banquet_healthcheck.sh
#         sha256 d0df91f676679bbb9215ba76da0f26c2852bde67d113f98b63599ddfc0ca4688（56 行）
# 本文件位置：docs/协作/天龙/prod-healthcheck-31/banquet_healthcheck.candidate.sh
# 应用前必须先备份原脚本：
#   cp -a /home/ubuntu/banquet_healthcheck.sh /home/ubuntu/banquet_healthcheck.sh.bak-20260913
# 一条回退命令：
#   sudo cp -a /home/ubuntu/banquet_healthcheck.sh.bak-20260913 /home/ubuntu/banquet_healthcheck.sh
#
# 相对原脚本的四处改动（第四类另加并发锁）：
#   A. 探测 URL 由 /api/actuator/health 修正为 /actuator/health（可用 env 覆盖）
#   B. 健康判定升级为 JSON status=UP；404 归类为"检查器配置错误"，只告警不重启
#   C. 重启前先看 systemd 单元/进程状态，只有真的进程不可用才重启
#   D. 连续失败阈值提到 3 次 + 每小时最多 2 次重启 + 最小间隔 15 分钟 + 文件锁
# ============================================================================
set -uo pipefail

URL="${BANQUET_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
STATE="/home/ubuntu/.banquet_health_fail"
LOCK="/home/ubuntu/.banquet_health.lock"
RESTART_STATE="/home/ubuntu/.banquet_health_restarts"
LOG_TS="$(date '+%F %T')"
MAX_FAILS=3
MAX_RESTARTS_PER_HOUR=2
COOLDOWN_SECONDS=900
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

# 并发锁：避免 cron 重叠执行
exec 9>"$LOCK" || exit 0
flock -n 9 || { echo "$LOG_TS  上一次检查仍在运行，跳过本轮"; exit 0; }

body_file="$(mktemp)"
code=$(curl -s -o "$body_file" -w '%{http_code}' --max-time 8 "$URL")
body="$(cat "$body_file")"
rm -f "$body_file"

# 改动 B：404 属检查器配置问题，只告警，绝不重启
if [ "$code" = "404" ]; then
    notify "健康检查 URL 返回 404（检查器配置可能已过期）：$URL —— 按策略只告警不重启"
    exit 0
fi

# 改动 B：以 JSON status=UP 为准
if [ "$code" = "200" ] && printf '%s' "$body" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    if [ -s "$STATE" ]; then
        notify "已恢复正常（此前连续 $(cat "$STATE") 次检查失败）"
    fi
    : > "$STATE"
    : > "$RESTART_STATE"
    exit 0
fi

fails=$(( $(cat "$STATE" 2>/dev/null || echo 0) + 1 ))
echo "$fails" > "$STATE"
notify "健康检查失败 第 ${fails} 次，HTTP=${code}"
[ "$fails" -ge "$MAX_FAILS" ] || exit 0

# 改动 C：先看单元与进程；单元 active 且进程存活时，仅 5xx/超时/无响应才考虑重启
unit_active="$(systemctl is-active banquet 2>/dev/null || echo unknown)"
if [ "$unit_active" = "active" ] && [ "$code" = "200" ]; then
    notify "单元 active 且端点返回 200，仅状态字段异常：不重启，等待人工确认"
    exit 0
fi

# 改动 D：重启频次与冷却
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
sudo systemctl restart banquet
sleep 30
after_body="$(mktemp)"
after=$(curl -s -o "$after_body" -w '%{http_code}' --max-time 8 "$URL")
after_ok="no"
if [ "$after" = "200" ] && grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' "$after_body"; then after_ok="yes"; fi
rm -f "$after_body"
if [ "$after_ok" = "yes" ]; then
    notify "重启后已恢复"
    : > "$STATE"
else
    notify "重启后仍不健康（HTTP=${after}），需要人工介入：查看 /home/ubuntu/backend.out"
fi
