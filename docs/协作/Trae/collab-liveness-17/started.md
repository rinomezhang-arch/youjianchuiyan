# TR-COLLAB-LIVENESS-17 started

- 任务：天地龙后台通信恢复、假死识别与原会话激活
- owner: trae（CLAIM_VERIFIED 已落板）
- base_sha: 24c3c611f1bf48796dfb2558dbc01758fcd260c6
- 开始: 2026-09-08（Codex 续接，无需二次确认，直接执行）
- 交接输入：F:/solo/docs/协作/Codex/通信投递_20260908.md
- 复用入口：scripts/agent_bridge/collab-send.ps1（openclaw gateway call agent / sessions.get）

## 现场基线（交接 + 截图证据，不重复全盘排查）

- 地龙 127.0.0.1:18789：node 监听活，sessions.get 曾返回 200 条；CX-DL-AUTH16-0908-01（幂等键 4b6f0d31-…）agent accepted 但固定标识回读超时；截图显示 sessions.messages.subscribe 30s 超时、UI 有历史工具活动（9 命令/3 读/2 编辑/Process×3）
- 天龙 100.70.215.11:11500：TCP 可达、握手成功，但 agent 写调用 FORBIDDEN（非掉线）；CX-TL-DATAMAP14-0908-01（幂等键 6da939f1-…）未确认；按约束不换身份/不放权限
- 秋哥已确认消息框显示正常，停止气泡位置/身份渲染排查；不盲目重发用户消息

## 执行计划（先登记再变更）

1. scripts/agent_bridge/liveness/ 新增可复用固定命令：
   - probe（只读：网关握手 + sessions.get 回读固定标识，不调 LLM）
   - classify（区分 掉线/忙碌/额度耗尽/权限拒绝/空闲/已收无活动）
   - activate（仅对确认假死的原会话投递一次带固定任务ID续做指令，幂等去重）
   - watcher（单实例 60s 只读后台检查，正常不调 LLM；每成员 30 分钟最多 2 次恢复，失败退避并 blocked 通知）
2. 只读探测天地龙 → 分类 → 仅确认停止/假死才按既有入口恢复
3. 激活后验证：带ID回复 / 真实工具活动 / 任务状态变化；只 accepted 或端口通不算激活
4. 夹具验证三场景：离线、启动后未响应、真实忙碌不误杀（不停在线成员）
5. 报告 + 脱敏日志落 artifacts/coordination-r3/liveness/，reported 上板

## 边界

- 不抢鼠标键盘、不新建聊天窗口、不重启餐饮生产/nginx/mysql、不碰法务
- 不 doctor --fix、不扩大网关权限、不替换身份绕过 FORBIDDEN
- 不打印凭证/完整私人会话；未验证收信不标送达、未验证活动不标激活
