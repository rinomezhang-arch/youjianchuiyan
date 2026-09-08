# CO19 / CO21 运行验收

结论：上述两项在各自辅助进程修复范围reviewed，不代表餐饮生产交付。

- 先固定5415bef5启动PID34072，独立17/17通过；已核实单实例、两龙各3轮探测、0次正常期激活、stderr 0字节，CO19运行要求满足。
- 随后修复真实发现的thinking/empty assistant虚假活动，提交fab0c1fc05d2baf0dc1ebb0f9c968e9100b21865，独立18/18通过、0失败/跳过，语法与diff检查通过。只停止已核实身份的旧PID34072，新固定快照PID60924唯一运行。
- 最新运行已核实地龙3轮、天龙2轮真实只读探测，0次agent激活、stderr 0字节。天龙实际429周额度耗尽已写恢复阻断，保持其账号/模型/网关不变；工资交CO20继续，不绕过额度。
- 当前版本：artifacts/coordination-r3/runtime/watchdog-fab0c1fc05d2/runtime-manifest.json与launch.json；固定8文件快照逐项SHA256。日志位于artifacts/coordination-r3/liveness/runtime-fab0c1fc05d2.stdout.log及liveness-events.jsonl，旧快照、锁及日志均保留。
- 地龙补证消息CX-DL-IPAD16-EVIDENCE-0909-0120随后按默认30秒查询回读成功，且有真实toolCall/toolResult。此前15秒查询失败不是离线证据，不重复发送。

能力边界：这是本机两龙连接巡检与受限恢复辅助进程，正常检查不调用模型、不新建聊天窗口；不自动代替餐饮业务验收、生产部署或四人任务统筹。未安装系统服务，不承诺关机后运行。全部生产、法务和原网关配置未改。
