# DL-IPAD-BATCH-AUTH-16 技术验收

结论reviewed。精确候选ecf98b7a（完整SHA以独立验收工作树HEAD为准），包含466d6e7d业务接线及本次两条补证。

补充：已读回完整HEAD为ecf98b7a505dfc6faf136910be61884dff8aff84。

Codex从该提交新建codex-ipad16-review独立工作树，未改地龙目录。首次16项均在setup因13317拒绝连接出错，0项断言失败；恢复既有隔离MySQL后同命令独立复跑：Tests run 16, Failures 0, Errors 0, Skipped 0，BUILD SUCCESS，20.100秒。

两项补证已逐行核对：同请求同payload两次data一致；同token重放的新请求回执行数0、总回执1、菜品1。执行后又用原生MySQL客户端独立回读4个新建ipad_idem_UUID schema，全部菜品1/回执1/被拒新请求0。日志和JSON证据保留：
- artifacts/coordination-r3/reviews/dl-ipad16-ecf98b7a.log（环境失败）
- artifacts/coordination-r3/reviews/dl-ipad16-ecf98b7a-retry.log（16/16）
- artifacts/coordination-r3/reviews/dl-ipad16-ecf98b7a-db-readback.json（4库回读）

覆盖本任务控制器到真实MySQL两表、一次性授权及幂等回执。该套件使用MockMvc，不冒充最终候选真实监听端口/浏览器验收；后者由RC15合入后完成。此技术验收不授权修改法务、律师权限或直接发布生产。
