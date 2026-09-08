# INTEGRATION-NEXT-20260908 集成复验报告

## 集成内容

- 基线：`b57dfac7`
- 应付创建、结算与幂等链：`01600000`
- iPad 收据到账户流水关联：`11640c0e`
- 应收未知结果恢复与真实链：合并提交 `821b9057`
- 法务模块、生产数据、生产配置均未修改。

## 验证结果

- 前端生产构建：通过。
- 应收前端 Node 测试：38/38，通过，失败0，跳过0。
- 后端真实 MySQL 聚合：101/101，通过，失败0，错误0，跳过0。覆盖应付HTTP合同、创建幂等、结算幂等、iPad流水关联、盘点持久化、物理结构与读取完整性。
- 应收真实 MySQL HTTP：22/22，通过，失败0，错误0，跳过0。
- 应收真实浏览器：9/9，通过，失败0，跳过0。
- 数据库回读：应收总额100.00，已收100.00，待收0.00，支付流水2条，流水合计100.00，金额守恒成立。
- 未知结果恢复：同一 requestId 恢复，无重复收款。
- 停用账户：HTTP拒绝，表单输入保留，数据库无新增流水。

## 证据

- `scripts/codex-receivable-real-e2e/evidence/backend-mysql-tests.json`
- `scripts/codex-receivable-real-e2e/evidence/browser-result.json`
- `scripts/codex-receivable-real-e2e/evidence/db-assertions.json`
- `scripts/codex-receivable-real-e2e/evidence/network-redacted.json`
- `scripts/codex-receivable-real-e2e/evidence/receivable-paid-and-disabled-rejected.png`

## 边界与后续

本报告只证明上述候选在同一集成分支上的组合回归。生产账号/RBAC、门店隔离、预订到结账全链、工资、报表打印、生产备份恢复、8080公网暴露与安全响应头仍须按生产门禁矩阵继续验收或整改，因此当前不标记整套系统生产就绪。
