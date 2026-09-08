# INTEGRATION-NEXT-20260908 集成复验报告

## 集成内容

- 基线：`b57dfac7`
- 应付创建、结算与幂等链：`01600000`
- iPad 收据到账户流水关联：`11640c0e`
- 应收未知结果恢复与真实链：合并提交 `821b9057`
- 公开H5、咨询转预订、自助查单和转单鉴权：`dc36140e` 至 `3701544b`
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
- 咨询与公开H5真实隔离 MySQL/HTTP：41/41，通过，失败0，错误0，跳过0；前端契约12/12通过。
- 咨询转单鉴权覆盖同店、总经理跨店、经理越店、停用/撤权、律师拒绝、无/坏/伪造JWT、跨店与不存在统一口径、重放不泄露、幂等与并发单单生成。
- 前端生产构建在补充项目级 `pnpm-workspace.yaml` 构建脚本白名单并同步缺失的 `qrcode` 锁文件后通过。
- 安全备份与隔离恢复候选：父级复跑52/52通过；无物理删除命令，失败与过期文件移入时间戳垃圾桶，复制采用临时文件加哈希后原子就位，恢复只允许非3306回环隔离库和固定 `restore_verify_` 前缀，拒绝跨库SQL、路径与链接逃逸。

## 证据

- `scripts/codex-receivable-real-e2e/evidence/backend-mysql-tests.json`
- `scripts/codex-receivable-real-e2e/evidence/browser-result.json`
- `scripts/codex-receivable-real-e2e/evidence/db-assertions.json`
- `scripts/codex-receivable-real-e2e/evidence/network-redacted.json`
- `scripts/codex-receivable-real-e2e/evidence/receivable-paid-and-disabled-rejected.png`

## 边界与后续

本报告只证明上述候选在同一集成分支上的组合回归。生产账号/RBAC、门店隔离、预订到结账全链、工资、报表打印、生产备份恢复、8080公网暴露与安全响应头仍须按生产门禁矩阵继续验收或整改，因此当前不标记整套系统生产就绪。

备份脚本已进入候选分支，但尚未上传生产、安装定时任务或执行真实备份恢复。生产动作仍需先核对实际环境变量名、工具、目录、挂载点和回退窗口。
