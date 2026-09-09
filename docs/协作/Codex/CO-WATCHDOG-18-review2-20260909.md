# CO-COLLAB-WATCHDOG-FIX-18 第二轮独立验收

执行人：Codex。候选：2d89d038ccaaa8c8b54ca09bac33106682e6f45e。结论：changes_requested；自动监管维持暂停。

本轮主循环已接入延后验证。以下两项用纯合成配置和文件句柄复现，未访问真实网关、发送消息或改变运行配置。13项测试是执行方报告，本轮不冒充独立复跑。

1. client-config.mjs 移除 gateway.auth.token 后仍复制整份其余配置。合成输入另含 models.providers.sample.apiKey 和 channels.sample.token，输出检查结果为 gatewayTokenCopied=false, providerCredentialCopied=true, channelCredentialCopied=true。应采用经过验证的最小非敏感客户端配置，避免克隆其他凭据；保留现有鉴权和权限，不改原配置。新增嵌套合成凭据断言。
2. acquireLock 遇到半写锁立即按损坏锁改名并抢占。首调用已 openSync(lockPath,'wx') 但未写JSON时，第二调用成功取得锁；首调用随后仍能向改名后的原句柄写入。结果 firstProcessHeldOpenLock=true, secondProcessAcquired=true。需要对尚未完成或不确定的锁安全拒绝，并覆盖陈旧锁回收时多个竞争者的交错，确保不能移动另一个刚取得的有效锁。测试应涵盖真实竞争进程或等价确定性交错，而不只检查已写好JSON的顺序调用。

证据目录：F:/solo/artifacts/coordination-r3/reviews/co18-round2。仅含合成凭据与保留的临时文件。原运行文件、锁、日志未替换；监管进程尚未恢复。

下一步：同一助手在既有工作树最小修复两项，提交新SHA、实际测试数字和报告。餐饮RC15仍由Trae推进，不因监管修复回退分工。
