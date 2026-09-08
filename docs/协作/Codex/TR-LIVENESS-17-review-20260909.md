# TR-COLLAB-LIVENESS-17 changes_requested

独立审查候选318fb40549b8aba24a18e2e404215adb8b3edad8。保留本轮两龙人工触发后会话活动证据；不认可常驻自动恢复已验收。

- watchdog.mjs:84-100未调verifyOnce，真实主循环不会执行冷却后回读与失败处理。
- activate.mjs:23-25、watchdog.mjs:62-65将accepted_pending_verify视作alreadyActivated，后续verify_pending没有主循环处理。
- watchdog.mjs:18-32锁创建非原子且不查PID存活，双实例可能并行，陈旧锁拒绝启动。
- watchdog.mjs:57-58对unknown也调agent，可能把瞬时CLI错误当成需要激活。
- client-config.mjs:14-24保存完整配置到固定临时路径；probe.mjs:40用命令行参数携带token。需减少凭据副本/可见面并验证临时文件实际权限，不得修改原网关权限或身份。
- 7项夹具未覆盖main调度、真实恢复、锁竞争、陈旧锁与配置权限；手动verifyOnce不等于常驻会调用。

Trae继续RC15，不重复修这套脚本。上述定点修复交CO-COLLAB-WATCHDOG-FIX-18，由既有Codex助手独立工作树完成；本任务待其补丁验收与实际恢复测试后再裁决。保留旧文件与日志，不声称服务已自动恢复。
