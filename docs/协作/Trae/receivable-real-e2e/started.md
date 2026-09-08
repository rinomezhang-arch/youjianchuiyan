# TR-OPS-RECEIVABLE-REAL-E2E-03 started 计划

- 任务：应收前端接真实隔离后端联调
- owner：trae
- base_sha：`09a15bbbb7b0d0eba3335fb45f830499cacf5405`
- 工作树：`F:\solo\artifacts\team-worktrees\trae-receivable-e2e`（新独立工作树，分支 `codex/trae-receivable-e2e`）

## 环境准备

1. 隔离 MySQL：Docker 容器 `youjian-mysql-e2e`（MySQL 8.4，端口 3307，数据库 `banquet_e2e`）
2. 后端：Spring Boot 3.2.5（`F:\solo\banquet_project`），`ddl-auto=update` 自动建表，端口 8080，连接隔离 MySQL
3. 种子数据：门店1（宁国总店）、门店2（宣城分店）、员工 rino（店长，门店1）、liming（店长，门店2）
4. 前端：Claude 的 ReceivableLedger.vue 及其 utils/tests 已复制到工作树

## 执行计划

1. 审查 ReceivableLedger.vue 与 FinanceController API 契约差异
2. 启动前端 dev server（vite，指向后端 8080）
3. 浏览器 E2E：登录 → 创建应收 → 部分收款 → 收清 → 刷新回读 → 未知结果恢复 → 停用账户拒绝
4. 识别前端契约问题并修复（仅限 allowed_paths）
5. 记录请求字段、业务ID、数据库守恒、浏览器可见状态
6. 报告真实通过/失败数字和未覆盖项
