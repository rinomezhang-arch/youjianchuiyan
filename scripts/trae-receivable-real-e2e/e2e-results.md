# TR-OPS-RECEIVABLE-REAL-E2E-03 E2E 联调结果

## 环境信息
- 前端 dev server: http://localhost:5173/ (Vite 5.4.21, 工作树 trae-receivable-e2e)
- 后端: Spring Boot 3.2.5, 端口 8080, 连接隔离 MySQL
- 隔离 MySQL: Docker 容器 youjian-mysql-e2e, MySQL 8.4, 端口 3307, 数据库 banquet_e2e
- 后端 ddl-auto=update 自动建表, 种子数据: 门店1(宁国总店)+门店2(宣城分店), 员工 rino(店长/门店1)
- 登录方式: POST /api/auth/login {username:"rino", password:"123456"} → JWT Bearer token

## 契约差异（前端 vs 真实后端 09a15bbb）

| 项 | Claude 前端期望 | 真实后端 | 影响 |
|---|---|---|---|
| 回执格式 | {requestId, receivableId, no, replayed, snapshot} | {receivableId: <id>} | 已修复 contract.js 适配简单回执 |
| 幂等 | requestId 重放返回相同结果 | 无幂等，每次 POST 创建新记录 | 前端"有未确认请求就不许叠发"规则仍有效（客户端侧防护） |
| payment → receivable | 收款自动更新应收余额(received_amount, pending_amount, status) | 收款独立记录，不更新应收 | **收清不可实现**，应收余额始终为 0 |
| receivableDetail | GET /finance/receivable?id=xxx | GET 只返回门店全部列表，不支持 id 过滤 | 前端需从列表自行查找 |
| 字符编码 | UTF-8 中文 | Docker MySQL 默认 charset 可能非 UTF-8 | 客户名存储为 ???? |
| paymentMethod | optionalText → null 时后端应默认 "cash" | getOrDefault 不处理 null 值 → 存储 NULL | 后端缺陷 |

## 浏览器 E2E 结果

| 步骤 | 结果 | 详情 |
|---|---|---|
| 1. 登录 | ✅ PASS | rino/123456 登录成功，获取 JWT |
| 2. 导航到财务页 | ✅ PASS | /dashboard/finance 路由可达 |
| 3. 创建应收 | ✅ PASS | 客户名"E2E测试客户", 金额 2000.00, receivable_no=RVB715A5CFD2A147CD |
| 4. 应收列表回读 | ✅ PASS | 新建应收出现在列表，receivable_id=1788852951441 |
| 5. 部分收款 | ✅ PASS | 提交 800.00, payment_no=PAY805F400DEF4D4D8D, payment_id=1788852986216 |
| 6. 收款后应收余额 | ❌ FAIL | 应收 received_amount 仍为 0.00, pending_amount 仍为 2000.00, status 仍为 "unpaid" |
| 7. 刷新回读 | ✅ PASS | 刷新后应收记录和付款记录均持久存在 |
| 8. 收清（全额收款）| ❌ N/A | 后端不支持收款更新应收，无法实现收清 |

## 数据库守恒验证

```
finance_receivable:
  receivable_id=1788852530738, receivable_no=RV-TEST-001, total=1000.00, received=0.00, pending=1000.00, status=unpaid
  receivable_id=1788852951441, receivable_no=RVB715A5CFD2A147CD, total=2000.00, received=0.00, pending=2000.00, status=unpaid

finance_payment_record:
  payment_id=1788852530955, payment_no=PAY-TEST-001, amount=500.00, method=cash
  payment_id=1788852986216, payment_no=PAY805F400DEF4D4D8D, amount=800.00, method=NULL
```

- 应收记录数: 2（与 API/浏览器提交次数一致）
- 付款记录数: 2（与 API/浏览器提交次数一致）
- 应收 received_amount 均为 0.00 → 后端不联动更新（已确认）

## 未覆盖项

1. **未知结果同 requestId 恢复**: 后端不支持 requestId 幂等，重发会创建新记录而非返回原结果。前端客户端侧的 sessionStorage 恢复日志仍可工作（防止叠发），但服务端侧的 replay 不可用。
2. **收清（全额收款）**: 后端 POST /payment 不更新 finance_receivable 的 received_amount/pending_amount/status，无法标记应收为已收清。需要后端新增"结算应收"端点。
3. **停用账户新收款拒绝且输入保留**: 后端无资金账户管理端点（finance_account 表无 API），无法测试停用账户场景。
4. **字符编码**: Docker MySQL 容器默认 charset 可能非 UTF-8，中文名存储为 ????。非前端问题，需调整 Docker MySQL 配置。
5. **paymentMethod 为 NULL**: 前端 optionalText 返回 null 时，后端 getOrDefault 不处理 null → 存储 NULL。后端缺陷。

## 修复清单（仅限 allowed_paths）

| 文件 | 修复 |
|---|---|
| `frontend_v3/src/utils/receivableLedger/contract.js` | validateReceivableReceipt/validatePaymentReceipt 简化为只检查主键为正整数（移除 requestId/no/replayed/snapshot 校验） |
| `frontend_v3/src/utils/receivableLedger/pending.js` | pickReceipt 只存储 {receivableId/paymentId}（移除 requestId/no/replayed/snapshot 提取） |

## 真实通过/失败数字

- 浏览器 E2E 步骤: 8 步（5 PASS, 2 FAIL, 1 N/A）
- 后端 API 测试: 创建应收 ✅, 列表 ✅, 创建付款 ✅, 付款更新应收 ❌
- 前端单元测试（contract.test.mjs + flow.test.mjs）: 35/35 PASS（0 FAIL）
  - 原 4 个失败用例已修复：回执校验从"全字段比对"改为"只校验主键正整数"，与真实后端简单回执契约一致
  - contract.test.mjs: 16/16 PASS
  - flow.test.mjs: 19/19 PASS
- 构建: ✅ `npm run build` EXIT=0, 1m27s（仅 chunk size 警告，非错误）

## 修复后的测试用例（4 个）

| 用例 | 原期望 | 修复后 |
|---|---|---|
| contract: 回执校验 | requestId/no/replayed/snapshot 全字段比对 | 主键 receivableId 正整数才通过 |
| contract: 收款回执一致性 | snapshot.receivable_id 与请求一致 | 主键 paymentId 正整数才通过 |
| flow: 盘上自相矛盾锁死 | 改 payload.totalAmount 触发锁死 | 改 receipt.receivableId=0 触发锁死 |
| flow: 回执对不上保留恢复记录 | 改 no 字段触发拒绝 | 改 receivableId=0 触发拒绝 |
