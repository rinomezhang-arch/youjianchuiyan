# CO18 第三轮独立验收

候选d70b699b5526f742801a64c36c84956ecda56465。14/14独立复跑通过，0失败/跳过。最小客户端配置配合官方环境变量鉴权实连两龙原会话成功，均返回200条历史；未发送消息、未改变网关或生产。配置兼容性及上一轮两个反例已改善。

结论仍为changes_requested，仅剩本轮新复现的两处误判：

1. inspect-session.mjs把消息JSON前400字符内的tool/command/exec等字样计作工具活动，不检查来源角色。夹具只有两条user消息：第一条[TASK-X][RESUME]，第二条“继续执行 exec 检查，完成后向我报告”，结果realActivity=true，期望false。按结构化角色及真实工具事件类型判定，用户正文不得贡献活动计数。
2. probe.mjs classify对成功sessions.get返回的完整历史也扫描quota/额度等词。夹具call.ok=true、markerFound=true，返回历史仅含用户“额度不足前记得交接 quota”，结果quota_exhausted，期望delivered。额度/权限异常只从当前调用失败的结构化错误或实际错误输出判定，不能从历史正文推断当前故障；保留真正错误夹具。

新增这两个小反例并复跑相关14项，提交新SHA。不用重做配置/锁和全仓扫描。watchdog维持停止；真实连接只是只读鉴权通过，不代表完成激活或持续监管验收。
