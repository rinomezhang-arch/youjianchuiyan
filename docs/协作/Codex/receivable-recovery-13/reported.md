# CX-OPS-RECEIVABLE-RECOVERY-13 完成报告

- 执行人：Codex
- 分支：`codex/receivable-recovery-13`
- 原始基线：`09a15bbbb7b0d0eba3335fb45f830499cacf5405`
- 被测源码提交：`6a2969c33c306691874a9bc19bd1e09eb2ad9d0e`
- 后端 reviewed 来源：`0fb2fce02649e0ca91e88b4c66a25db6d2e0b509`
- 前端候选来源：`c3750bb006c555e995568b102dc6b58ed9455e10`
- 最终隔离库：`cx_recv13_20260908_173230`，本机 MySQL 13317；未连接或写入生产
- 发布/部署：未执行

## 实际改动

1. 精确带入 reviewed 后端 `ReceivablePaymentService` 与真实 MySQL HTTP 测试。
2. 带入应收前端候选，并恢复严格关联：回执同时核对 requestId、实体 ID、业务单号、门店和金额；列表回读再核对业务单号、金额、客户、预订单、账户和目标应收关系。
3. requestId 只接受 32 位十六进制或标准 UUID；业务单号使用完整 32 位 UUID 十六进制，避免不同请求前 16 位相同导致碰撞。
4. 未知结果只允许原 requestId、原参数恢复；列表关系不符时不得清除恢复记录。
5. 修复财务月度图全零数据除以零导致 SVG `NaN` 的界面错误。
6. 新增可重复执行的隔离库、真实 Spring Boot、真实 JWT、Edge/Playwright 浏览器链脚本及脱敏证据。

## 验证结果

- 前端契约和恢复单测：38/38 通过，0 失败，0 跳过。
- reviewed 后端真实 MySQL HTTP 测试：22/22 通过，0 失败，0 错误，0 跳过。
- 前端生产构建：1/1 成功。
- 真实浏览器 E2E：9/9 通过，0 失败，0 跳过。
- 最终账务守恒：应收 100.00，已收 100.00，待收 0.00；两笔收款合计 100.00；状态 paid；守恒断言 true。
- 未知结果恢复：70.00 收款由后端成功提交后主动中断浏览器响应，再以同 requestId 重发；回执 `replayed=true`，仍只有两条收款流水。
- 停用账户：账户 103 的 12.34 手工收款真实返回 HTTP 400；金额、账户、类别和说明仍留在表单；数据库未增加流水。
- 幂等登记：3 行（创建应收、30.00 收款、70.00 收款），恢复重放没有新增第 4 行。
- 后端 class SHA-256：`05BA1A5D19261F2549E0DDB1CDC94A300008973EC99CB336936AA5BDCA6FCDCA`。
- 证据敏感信息扫描：通过；网络证据不含 token、密码、JWT 密钥或 AES 密钥明文。

主要证据位于 `scripts/codex-receivable-real-e2e/evidence/`：

- `environment.json`：被测源码 HEAD、来源提交、class 和迁移哈希、隔离端口与库名；
- `network-redacted.json`：脱敏请求、回执三联、模拟断流、重放和 400 拒绝；
- `business-ids.json`：应收和两笔收款的实体 ID、业务单号；
- `db-assertions.json`：金额守恒、状态、流水数和幂等登记数；
- `browser-result.json`：9 项浏览器断言；
- `receivable-paid-and-disabled-rejected.png`：收清回读与停用账户拒绝后输入保留；
- `frontend-node-tests.txt`、`frontend-build.txt`、`backend-mysql-tests.json`：测试和构建数字。

## 失败尝试与处理

失败尝试均发生在独立合成库，未物理删除，`cx_recv13_*` 诊断库保留供复核。

1. PowerShell 向 MySQL 传递中文种子时编码不一致；种子标识改为 ASCII。
2. Maven `.cmd` 启动参数中的 JDBC URL 被命令行解析；改为直接运行当前源码打出的 JAR。
3. 独立工作树没有依赖目录；仅建立到主工作区现有 `node_modules` 的只读式目录联接，不改主工作区文件。
4. 浏览器代理通配符误拦截 `/src/api/auth.js`；收窄为站点根 `/api/**`。
5. 隔离前端端口不在后端默认 CORS 列表；仅为隔离进程加入本次端口参数。
6. 旧模板库缺少 reviewed 迁移追加的 `payment_category`；每次新库都执行正式迁移脚本。
7. 回执校验一度要求后端快照未返回的客户/预订单字段；恢复为两层核对：回执核对其真实字段，列表回读核对完整业务关系。
8. 浏览器脚本一度重复点击已由组件自动完成的流水核对；改为等待自动 GET 流水和写入锁解除。

## 未解决项

- 本任务范围内无阻断项。
- 前端构建仍有既存大 chunk 警告；不影响本次构建和应收链，未在本任务扩大到全站拆包。
- 登录页有浏览器关于复杂表单结构的提示；不影响登录和本任务数据链。
