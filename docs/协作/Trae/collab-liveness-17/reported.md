# TR-COLLAB-LIVENESS-17 reported

- 任务：天地龙后台通信恢复、假死识别与原会话激活
- owner: trae（CLAIM_VERIFIED / EVENT_VERIFIED=started 已落板）
- base_sha: 24c3c611f1bf48796dfb2558dbc01758fcd260c6
- 时间: 2026-09-08 ~ 09
- 结论：**两龙原会话均已激活并验证真实活动（非仅 accepted）；常驻看门狗已运行；未解决项见末尾，不算通道完美但不阻断集成主线。**

## 一、真实探测数字（只读，未调 LLM）

| 成员 | TCP | sessions.get | 固定标识 | 标识后消息 | 助手/工具结果 | 最后活动（北京） |
|---|---|---|---|---|---|---|
| 地龙 127.0.0.1:18789 | 通 | 200 条 | CX-DL-AUTH16-0908-01 命中 | 首轮 26 条 | 14 / 11 | 20:36（原投递后） |
| 天龙 100.70.215.11:11500 | 通 | 200 条 | CX-TL-DATAMAP14-0908-01 命中 | 首轮 39 条 | 24 / 12 | 20:40（原投递后） |

- 交接时（20:27）"accepted 未回读 / 天龙 FORBIDDEN"的状态已变化：本轮两标识均可在原会话 agent:main:main 回读，且两龙在原投递后都有真实工具活动（不是端口通就判活）。
- 关键阻断根因：OpenClaw 2026.9.2 CLI 对 `~/.openclaw/openclaw.json` 的 `auth.cooldowns` 未知键严格校验，gateway call 被拒（"OpenClaw config is invalid"），并非网关掉线。**未执行 doctor --fix（在禁止清单）、未改网关配置/权限/身份**；改为客户端侧临时配置副本（仅删未知键、token 原样保留）经 OPENCLAW_CONFIG_PATH 注入 CLI。

## 二、激活与真实活动验证（续做指令投递后）

- 地龙（resume 键 4b6f0d31…-resume-r1）：续做 DL-IPAD-BATCH-AUTH-16 两项反例（IpadBatchSubmissionService 未接入/client_request_id 不落库；补两表真实回读，保留 12 项授权测试）。accepted 后 2 分钟只读复核：标识后 **14 条新消息（7 助手 + 7 工具结果），11 条工具类活动，复核前 11 秒仍在产出** → 真实激活。
- 天龙（resume 键 6da939f1…-resume-r1）：续做 TL-RELEASE-DATA-MAP-14（交付目录 docs/协作/天龙/release-data-map-14 当时不存在，属"有活动无交付回执"）。accepted 后复核：**2 条助手回复** → 真实响应；工具产出待后续轮次。
- 幂等：重跑激活直接 already_activated，不二次投递；限流 30min≤2 次；FORBIDDEN/额度 → blocked 不绕过（夹具覆盖）。

## 三、可复用固定命令

```bash
# 只读探测（不调 LLM、不发消息）
node scripts/agent_bridge/liveness/probe.mjs
# 会话尾部脱敏检查（标识后有无真实活动）
node scripts/agent_bridge/liveness/inspect-session.mjs
# 必要激活（幂等+限流，固定续做内容/固定键）
node scripts/agent_bridge/liveness/activate.mjs [dilong|tianlong]
# 常驻看门狗（单实例、60s 只读；当前已在跑 pid 见 artifacts/coordination-r3/liveness/watchdog.lock）
node scripts/agent_bridge/liveness/watchdog.mjs
# 夹具：node --test scripts/agent_bridge/liveness/liveness.test.mjs  → 7/7 通过
```

证据/日志（脱敏：只记状态/时间/消息ID/计数，无会话正文、无凭证）：
- artifacts/coordination-r3/liveness/liveness-events.jsonl
- artifacts/coordination-r3/liveness/state-dilong.json / state-tianlong.json / watchdog.lock

## 四、夹具验证（不停在线成员）

`node --test` **7/7 通过**：① TCP 离线分类+投递失败不杀进程；② 启动后未响应（读超时同态于 30000ms subscribe 超时）两次无真实活动后 blocked；③ 真实忙碌（标识命中）不发任何 agent 消息；④ FORBIDDEN 不重试不换身份；⑤ 幂等去重；⑥ 30 分钟 2 次限流 blocked；⑦ classify 单元判定。

## 五、边界合规

未抢鼠标/键盘、未新建聊天窗口、未重启生产/nginx/mysql、未碰法务、未 doctor --fix、未扩权限/换身份、未打印凭证或私人会话。

## 六、未解决项（继续安排，不阻断 RC-15）

1. 天龙/地龙各自任务的**最终交付**（DL-IPAD-BATCH-AUTH-16 返工新 SHA、TL-RELEASE-DATA-MAP-14 报告落 docs/协作/天龙/release-data-map-14 与任务板 reported）仍待两龙本人产出；由看门狗与后续轮次核验，本任务不代做。
2. 天龙写通道历史 FORBIDDEN 本轮未复现（resume accepted），若再发应仍按 blocked_forbidden 处理，不绕过。
3. openclaw.json 的 auth.cooldowns 未知键属网关侧配置兼容问题，建议 Codex 在不动权限前提下统一安排配置收敛（本任务只用客户端副本规避）。
4. sessions.get 返回上限 200 条，更早历史不翻页；活动判定基于最近窗口。
