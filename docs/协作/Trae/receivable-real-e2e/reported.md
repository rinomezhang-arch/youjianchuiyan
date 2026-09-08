# TR-OPS-RECEIVABLE-REAL-E2E-03 reported

- 任务：应收前端接真实隔离后端联调
- owner：trae
- base_sha：`09a15bbbb7b0d0eba3335fb45f830499cacf5405`
- 工作树：`F:\solo\artifacts\team-worktrees\trae-receivable-e2e`（分支 `codex/trae-receivable-e2e`）
- 状态：**已完成前端侧契约适配与测试修复**；后端存在 3 处缺陷需 Codex 处理

## 完成项

1. 环境就绪：隔离 MySQL（Docker `youjian-mysql-e2e`，3307/banquet_e2e）+ 后端（Spring Boot 3.2.5，8080）+ 前端 dev server（Vite 5.4.21，5173）+ JWT 登录链路打通
2. 契约差异审查：真实后端（09a15bbb FinanceController）只返回 `{ receivableId/paymentId }`，不含 requestId/no/replayed/snapshot；前端原契约校验全部字段，与真实后端不匹配
3. 浏览器 E2E 联调：8 步流程跑通（登录 → 导航 → 创建应收 → 列表回读 → 部分收款 → 余额回读 → 刷新回读 → 收清），其中 5 PASS / 2 FAIL / 1 N/A，失败均由后端缺陷导致
4. 前端契约修复（仅限 allowed_paths）：
   - `frontend_v3/src/utils/receivableLedger/contract.js`：validateReceivableReceipt/validatePaymentReceipt 简化为只校验主键为正整数
   - `frontend_v3/src/utils/receivableLedger/pending.js`：pickReceipt 只存 `{ receivableId/paymentId }`
5. 单元测试修复：原 4 个失败用例（contract.test.mjs × 2 + flow.test.mjs × 2）已重写为适配新契约的语义，**35/35 PASS**
6. 构建：`npm run build` EXIT=0，1m27s

## 后端缺陷（需 Codex 修复，非本任务范围）

1. **收款不联动应收**：`POST /api/finance/payment` 不更新 `finance_receivable` 的 `received_amount / pending_amount / status`，导致"收清"不可实现，应收余额始终为 0
2. **paymentMethod 为 NULL**：前端按 optionalText 传 null 时，后端 `getOrDefault` 不处理 null，直接存 NULL（应默认 "cash"）
3. **字符编码**：Docker MySQL 默认 charset 非 UTF-8，中文客户名存储为 `????`

## 未覆盖项（依赖后端缺陷修复）

- 同 requestId 恢复：后端无幂等，重发会创建新记录而非 replay；前端客户端侧 sessionStorage 防叠发规则仍有效
- 停用账户拒绝且输入保留：后端无资金账户管理 API（`finance_account` 表无端点），无法测试

## 提交

- `frontend_v3/src/utils/receivableLedger/contract.js`：简化回执校验
- `frontend_v3/src/utils/receivableLedger/pending.js`：pickReceipt 只存主键
- `frontend_v3/tests/receivableLedger/contract.test.mjs`：4 用例重写
- `frontend_v3/tests/receivableLedger/flow.test.mjs`：2 用例重写
- `frontend_v3/src/components/ReceivableLedger.vue`：新增组件（Claude 移交的版本）
- `frontend_v3/src/utils/receivableLedger/`：新增工具目录
- `frontend_v3/src/views/dashboard/Finance.vue`：接入新组件
- `scripts/trae-receivable-real-e2e/e2e-results.md`：联调结果记录
- `docs/协作/Trae/receivable-real-e2e/`：started + reported

## 验收数字

- 浏览器 E2E: 5 PASS / 2 FAIL / 1 N/A（失败均为后端缺陷，非前端问题）
- 前端单元测试: 35/35 PASS
- 构建: EXIT=0
