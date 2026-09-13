# TR-OPS-RECEIVABLE-REAL-E2E-03 E2E 联调结果（2026-09-14 断点续作轮）

> 本轮从 2026-09-08 断点续作，不重新探索。09-08 轮结论（5 PASS / 2 FAIL / 1 N/A，卡 3 个后端缺陷）见文末附录 A。
> 本轮结论：**真实隔离链全绿——API 探针 17/17 PASS；浏览器真实 JWT 六场景 6/6 PASS；零产品代码改动。**

## 1. 断点状态与上游修复

09-08 轮阻断的 3 个后端缺陷，已由统筹分支（`codex/integration-20260913`，合入 `codex/receivable-recovery-13`）修复，本工作树候选前端未再打补丁即与新后端契约匹配：

| 09-08 缺陷 | 上游修复（集成分支提交） | 本轮验证 |
|---|---|---|
| 收款不联动应收余额 | `6a2969c3` 接通严格回执与真实链候选；`821b9057` 集成-合并：应收恢复真实链 | 部分收款 partial、收清 paid、回执 snapshot 内嵌应收最新状态 |
| paymentMethod 为 NULL | 同上（null 默认 cash） | 探针收清笔 paymentMethod 传 null，落库 cash |
| 中文乱码 | 隔离库/连接全部 utf8mb4（本轮环境侧核验） | 中文客户名 HEX 核验为正确 UTF-8 |
| （新增）幂等 replay、超额拒绝、停用账户拒绝、登录实时撤权 | `ba0695ba` 会话实时撤权与收款账户余额原子入账；`55200ace` 等验收提交 | 同 requestId replay=true、改参数 409、超额 400、停用账户 400 |

后端 JAR 来源：集成分支工作树 `f:\solo\artifacts\team-worktrees\codex-integration-20260913` 在 2026-09-14 构建时的快照（当时 HEAD 约 `0da17c88`，含上述全部提交），复制（排除 target/.git）到临时目录 `C:\Users\rinom\AppData\Local\Temp\banquet-e2e03` 后 `mvn -q -DskipTests package` 构建，未改动任何后端源码。

## 2. 环境恢复动作（全部只作用于隔离环境）

- Docker 容器 `youjian-mysql-e2e`（mysql:8.4，127.0.0.1:3307->3306，库 `banquet_e2e`，命名卷持久化）start；未触碰生产网络。
- 隔离库两条对齐动作（幂等、可重复）：
  1. 新后端 `StaffRealtimeGuard` 每请求回查 staff_master，要求 role 非空且在白名单；隔离种子 role 原为 NULL 导致 401：
     `UPDATE staff_master SET role='manager' WHERE staff_account IN ('rino','liming') AND role IS NULL;`（仅隔离库，rino=100/门店1 active，liming=102/门店2）
  2. `receivable_payment_request` 表由 ddl-auto=update 自动建立；裸 SQL 建表的 `finance_payment_record` 缺列，补：
     `ALTER TABLE finance_payment_record ADD COLUMN payment_category VARCHAR(32) NULL AFTER payment_method;`（仅隔离库）
- 库/表均 utf8mb4；中文经 HEX 核验：如「浏览器E2E客户甲」= E6B58F E8A788 E599A8 `45 32 45` E5AEA2 E688B7 E794B2，「停用账户测试」= E5819C E794A8 E8B4A6 E688B7 E6B58B E8AF95，「恢复场景客户」= E681A2 E5A48D E59CBA E699AF E5AEA2 E688B7。PowerShell 控制台显示 ???? 仅为控制台编码假象。
- 后端后台运行于 8080（启动约 48s）：SPRING_PROFILES_ACTIVE=prod；数据源指向 127.0.0.1:3307/banquet_e2e；ddl-auto=update；JWT_SECRET/AES_SECRET_KEY 为一次性测试值（不入库不提交）；APP_NOTIFY_ENABLED=false、LEGAL_ENABLED=false。日志 `backend-e2e03.log`。
- 前端本工作树 `npm run dev`（Vite 5.4.21，5173，/api 代理 8080）。日志 `frontend-dev-e2e03.log`。
- 按统筹要求未跑全量 `npm run build`（统一 Vite 构建由统筹分支完成）；本轮只跑专属探针与 dev server 真实浏览器验证。

## 3. 真实 API 探针（真实 JWT，禁合成）

脚本：[api-probe-03.mjs](file:///f:/solo/artifacts/team-worktrees/trae-receivable-e2e/scripts/trae-receivable-real-e2e/api-probe-03.mjs)；原始请求/响应日志：`api-probe-03.log`（JWT 与密码已脱敏）。
结果：**17 passed / 0 failed / 17 total**。全部请求经 `POST /api/auth/login`（rino/真实种子密码）取得的 Bearer JWT 发起。

| # | 场景 | 方法/路径 | HTTP | 关键业务ID/数字 |
|---|---|---|---|---|
| 1 | 真实登录 | POST /api/auth/login | 200 | staffId=100, role=manager, storeId=1 |
| 2 | 创建应收A 2000（中文客户） | POST /api/finance/receivable | 200 | receivableId=1788852951442，RV-E2E03-A |
| 3 | 应收列表 | GET /api/finance/receivable?storeId=1 | 200 | 含新单 |
| 4 | 按 id 详情 | GET ...&id=1788852951442 | 200 | payments=[] |
| 5 | 部分收款 800 | POST /api/finance/payment | 200 | paymentId=1788852986217，A -> partial 800/1200 |
| 6 | 同 requestId 重放 | POST /api/finance/payment | 200 | replayed=true，回原 paymentId 217，message「未重复记账」 |
| 7 | 同 requestId 改金额 801 | POST /api/finance/payment | **409** | 拒绝按重试处理 |
| 8 | 收清 1200（paymentMethod 留空 null） | POST /api/finance/payment | 200 | paymentId=1788852986218，落库 method=cash，A -> paid 2000/0 |
| 9 | 创建应收D 100 | POST /api/finance/receivable | 200 | receivableId=1788852951443 |
| 10 | 超额支付 150/待收100 | POST /api/finance/payment | **400** | 「收款金额超过该应收单的待收金额，本次未登记」 |
| 11 | 创建资金账户（初额1000） | POST /api/finance/accounts | 200 | accountId=1789330190500 |
| 12 | 创建应收B 500 | POST /api/finance/receivable | 200 | receivableId=1788852951444 |
| 13 | 启用账户收款 200 | POST /api/finance/payment | 200 | paymentId=1788852986219，账户 1000->1200，B -> partial |
| 14 | 停用账户 | PUT /api/finance/accounts/1789330190500 `{"status":"inactive"}` | 200 | isActive=false，余额仍 1200 |
| 15 | 创建应收C 300 | POST /api/finance/receivable | 200 | receivableId=1788852951445 |
| 16 | 停用账户新收款 100 | POST /api/finance/payment | **400** | 「收款账户不存在、不属于当前门店或已停用」 |
| 17 | C 回读 | GET ...&id=1788852951445 | 200 | 仍 unpaid/待收300，payments=0；另刷新回读 A 仍 paid 两笔流水持久 |

请求字段（创建应收固定键）：requestId, storeId, receivableNo, customerName(可选), totalAmount, receivableDate, remark(可选)。
请求字段（收款固定键）：requestId, storeId, paymentNo, paymentDate, receivableId, amount, paymentMethod(可空->cash), accountId(手工/账户收款时)。
回执（R0 简化契约）：data 只认主键 `receivableId`/`paymentId` 为正整数；后端实际还回 requestId/no/replayed/snapshot，前端不强制比对。

## 4. 浏览器真实 JWT E2E（http://localhost:5173，rino 真实登录）

**六场景 6/6 PASS。** 截图目录：[shots/](file:///f:/solo/artifacts/team-worktrees/trae-receivable-e2e/scripts/trae-receivable-real-e2e/shots)

| 验收场景 | 结果 | 浏览器可见状态 | 业务ID / DB 印证 | 截图 |
|---|---|---|---|---|
| 登录 | PASS | rino/[REDACTED] 真实 JWT 进入财务页 | /api/auth/me 200 | 02-list.png |
| 创建应收 | PASS | 列表首行出现「浏览器E2E客户甲 600」 | receivable_id=1788852951446，单号 RV2169A90372494CE4 | 03-created.png |
| 部分收款 | PASS | 行状态「部分收款」，已收200/待收400 | payment_id=1788852986220，PAYDE1A4D90BA1747D3，cash | 04-partial.png |
| 收清 | PASS | 行状态「已收清」，待收0，登记按钮 disabled | payment_id=1788852986221，PAY28852CD376914D53 400，received=600/pending=0/status=paid | 06-paid.png |
| 刷新回读 | PASS | 整页重新导航后 10 张单全在，金额/状态持久 | GET 列表 200（刷新后网络面板 11 条 /api 全 GET，无加载即重复写入） | 05-refresh.png |
| 未知结果同 requestId 恢复 | PASS | F5 后黄色在途告警+「恢复」按钮；点击后告警消失，列表恰好 1 条 unpaid 333 | 注入 requestId=abcdef0123456789abcd；恢复创建 receivable_id=1788852951448（RVABCDEF0123456789，「恢复场景客户」），receivable_payment_request 登记同一 requestId，无重复单 | 07-pending-banner.png、08-resumed.png |
| 停用账户新收款拒绝且输入保留 | PASS | 红色 alert「收款账户不存在、不属于当前门店或已停用」；对话框未关闭，金额100.00/方式cash/账户1789330190500/类别/说明五项输入全部保留；人工「取消」才关闭 | 无新增 payment_record（最大 payment_id 仍 221）、无新增 request 行、账户余额仍 1200 且 is_active=0；同一拒绝路径 API 侧为 HTTP 400（探针#16） | 09-denied-retain.png、10-denied-alert.png |

停用账户场景走「手工收款（无应收来源）」入口（金额100、cash、账户1789330190500、类别「停用账户浏览器验证」、说明「停用账户新收款应被拒绝且输入保留」）；该入口与行内「登记收款」共用同一后端校验 `validateAccount`，应收来源路径的 400 拒绝由探针 #15-17 独立证明。另有一笔浏览器代理早期残留单 RV2445D43A9EB34479（600 unpaid，无害，未对其登记任何流水）与 09-08 历史单 RVB715A5CFD2A147CD/RV-TEST-001 同列表展示。

网络（本 tab XHR，/api）：登录与 /api/auth/me 200；GET today/monthly-trend/pending-docs/balance/payables/receivable 均 200；写请求 = 4 次 POST /finance/receivable（RV2169、残留 RV2445、恢复 RVABCDEF、停用账户测试 RV94A0FB）+ 3 次 POST /finance/payment（200/400 成功两笔 + 停用账户被拒一笔），与 DB request 表 6 个浏览器键完全对应（被拒请求在 validate 阶段返回，不落 request 表，符合设计）。
控制台：无产品代码运行时错误。仅有：Notify WebSocket 连接失败（测试环境 APP_NOTIFY_ENABLED=false，预期）；F5 重载中断在途模块请求的 net::ERR_ABORTED（浏览器正常行为）；2 条 SyntaxError 来自测试脚本自身注入 evaluate 的转义错误，非产品代码。

## 5. DB 守恒（隔离库 banquet_e2e，全部数字经 SQL 直读）

finance_receivable（本轮新增 8 张；另有 09-08 历史 2 张）：

| receivable_id | 单号 | total | received | pending | status | 来源 |
|---|---|---|---|---|---|---|
| 1788852951442 | RV-E2E03-A | 2000 | 2000 | 0 | paid | 探针 |
| 1788852951443 | RV-E2E03-D | 100 | 0 | 100 | unpaid（超额被拒） | 探针 |
| 1788852951444 | RV-E2E03-B | 500 | 200 | 300 | partial | 探针 |
| 1788852951445 | RV-E2E03-C | 300 | 0 | 300 | unpaid（停用账户被拒，0流水） | 探针 |
| 1788852951446 | RV2169A90372494CE4 | 600 | 600 | 0 | paid | 浏览器 |
| 1788852951447 | RV2445D43A9EB34479 | 600 | 0 | 600 | unpaid（残留，无流水） | 浏览器 |
| 1788852951448 | RVABCDEF0123456789 | 333 | 0 | 333 | unpaid（恢复生成，唯一一条） | 浏览器恢复 |
| 1788852951449 | RV94A0FB9613EC4E5C | 250 | 0 | 250 | unpaid（停用账户测试单，被拒无流水） | 浏览器 |

finance_payment_record（本轮 5 笔，金额守恒：800+1200=A的2000；200=B部分；200+400=600收清）：

| payment_id | payment_no | amount | method | account_id | receivable_id |
|---|---|---|---|---|---|
| 1788852986217 | PAY-E2E03-A1 | 800 | cash | NULL | 1442 |
| 1788852986218 | PAY-E2E03-A2 | 1200 | cash（null 默认修复） | NULL | 1442 |
| 1788852986219 | PAY-E2E03-B1 | 200 | cash | 1789330190500 | 1444 |
| 1788852986220 | PAYDE1A4D90BA1747D3 | 200 | cash | NULL | 1446 |
| 1788852986221 | PAY28852CD376914D53 | 400 | cash | NULL | 1446 |

历史对照（非本轮）：1788852530955/PAY-TEST-001 500 cash（09-08）；1788852986216/PAY805F400DEF4D4D8D 800 **method=NULL**（09-08 旧缺陷产物，本轮所有新笔均为 cash，反证修复生效）。

finance_account：1789330190500 current_balance=1200（初额1000+200），is_active=0；停用后的被拒收款未改动余额。
receivable_payment_request：共 13 行（探针 7 + 浏览器 6）；同 requestId 重放不新增行（主键即 request_id）；409 改参数、超额 400、停用账户 400 均不产生新行；恢复键 `abcdef0123456789abcd` 恰好一行指向 1788852951448。

## 6. 结论与改动清单

- 六个验收场景在真实隔离 MySQL + 真实后端（集成分支修复后）+ 真实登录 JWT 下全部通过；无合成 API、无 mock。
- **零产品源码改动**：本工作树 `git status` 对 `frontend_v3/` 无任何修改/新增（09-08 轮提交 3684ddc2 的候选组件与工具契约即最终版本，与修复后后端直接兼容）。本轮新增仅证据/脚本：`scripts/trae-receivable-real-e2e/` 下探针、日志、截图与本文件。
- 后端缺陷未自行修复（由统筹分支修复并构建）；未碰生产、法务、共享路由、全局 CSS。
- 手机可用性/视觉：沿用 09-08 轮组件既有克制样式，本轮未改样式；弹窗在 777px 宽视口（截图视口）下正常使用。
- 复现入口：启动容器 `docker start youjian-mysql-e2e`；后端从集成分支临时副本 `java -jar target\banquet-1.0.0.jar`（环境变量见第 2 节）；前端工作树 `npm run dev`；探针须先在环境提供必填口令 `$env:E2E_LOGIN_PASSWORD='<测试账号口令>'`（无默认、不回显、不入日志），再 `node scripts/trae-receivable-real-e2e/api-probe-03.mjs`；浏览器走 http://localhost:5173/login（口令由环境持有人输入，不落文档）。收尾后容器数据卷保留可直接复用。

## 附录 A：2026-09-08 断点轮结果（历史）

基于当时基线 `09a15bbb`：浏览器 8 步 5 PASS / 2 FAIL / 1 N/A；前端单测 35/35（contract 16 + flow 19）；`npm run build` EXIT=0。失败/N/A 均为后端侧：收款不联动应收（收清不可实现）、paymentMethod null 落 NULL、无资金账户端点（停用账户场景无法测）；同 requestId 服务端 replay 当时不存在。这些项已在第 1、3、4、5 节被上游修复后的真实链全部转为 PASS。
