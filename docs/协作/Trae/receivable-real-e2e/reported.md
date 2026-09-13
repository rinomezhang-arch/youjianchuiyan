# TR-OPS-RECEIVABLE-REAL-E2E-03 reported（2026-09-14 断点续作轮）

- 任务：应收前端接真实隔离后端联调
- owner：trae；base_sha：`09a15bbbb7b0d0eba3335fb45f830499cacf5405`
- 工作树：`f:\solo\artifacts\team-worktrees\trae-receivable-e2e`，分支 `codex/trae-receivable-e2e`，HEAD `3684ddc2`
- 状态：**reported，真实隔离链六个验收场景全部通过；零产品源码改动。**

## 断点与上游

09-08 started 轮（5 PASS/2 FAIL/1 N/A）卡 3 个后端缺陷：收款不联动应收、paymentMethod NULL、无资金账户端点/幂等。统筹分支 `codex/integration-20260913` 已修复并合入（`6a2969c3` 严格回执与真实链、`821b9057` 集成合并应收恢复真实链、`ba0695ba` 实时撤权与账户余额原子入账）。本轮从断点续作，后端用集成分支快照临时副本构建（未改后端源码），前端候选零补丁直接通过。

## 环境恢复（仅隔离环境）

- 容器 `youjian-mysql-e2e`（mysql:8.4，127.0.0.1:3307，库 banquet_e2e，卷持久化）；真实登录 rino/真实种子密码换 JWT。
- 隔离库两条对齐：staff_master 测试账号 role 补 manager（新实时撤权回查要求）；finance_payment_record 补 payment_category 列。生产零接触。
- 一次性 JWT/AES 测试密钥、APP_NOTIFY_ENABLED=false、LEGAL_ENABLED=false；未跑全量 build（统筹统一构建已过），仅 dev server（5173 代理 8080）。

## 验收数字

1. 真实 API 探针（scripts/trae-receivable-real-e2e/api-probe-03.mjs，真实 JWT）：**17/17 PASS，0 FAIL**。覆盖创建/详情/部分收款 800、同 requestId 重放 replayed=true 回原单、同键改金额 409、收清 1200（method 传 null 落 cash）、超额 400、账户建/停用 PUT、停用账户收款 400、被拒单回读 unpaid 0 流水、刷新持久。
2. 浏览器真实登录 E2E：**六场景 6/6 PASS**——创建应收 600（RV2169A90372494CE4）、部分收款 200、收清 400（按钮 disabled）、整页刷新回读、未知结果同 requestId 黄色在途告警+一键恢复（RVABCDEF0123456789 仅一条 333 unpaid，无重复）、停用账户新收款被 400 拒绝且对话框不关闭五项输入全保留。截图 9 张（02-10）在 scripts/trae-receivable-real-e2e/shots/。
3. DB 守恒（SQL 直读）：本轮新增应收 8、流水 5（800+1200/200/200+400 全部对得上），request 表 13 行（重放不加行、三类拒绝不加行），停用账户 1789330190500 余额保持 1200/is_active=0；中文客户名 HEX 核验 UTF-8 正确。
4. 控制台无产品代码错误（仅测试环境关闭通知的 WS 报错、F5 中断请求、测试脚本自身 2 条注入语法错误）。

## 交付物（本轮新增，全部在 allowed_paths）

- scripts/trae-receivable-real-e2e/api-probe-03.mjs + api-probe-03.log（JWT/密码已脱敏）
- scripts/trae-receivable-real-e2e/backend-e2e03.log、frontend-dev-e2e03.log
- scripts/trae-receivable-real-e2e/shots/02-10 共 9 张截图
- scripts/trae-receivable-real-e2e/e2e-results.md（重写为 09-14 轮完整证据，09-08 结果留附录 A）
- 文档：docs/协作/Trae/receivable-real-e2e/reported.md（本文件）

未提交 4 个非本任务的未跟踪文件（codex_board.md、download_cos.py、list_cos.py、根目录开发记录）。

## 2026-09-14 changes_requested 二次 reported（仅脱敏，业务证据不重跑）

- api-probe-03.mjs：登录口令改为必填环境变量 E2E_LOGIN_PASSWORD（无默认、缺失 exit 2、不回显）；控制台与 EVIDENCE_JSON 对 password/token/authorization/jwt/secret 键递归打码 [REDACTED]；语法检查与无变量中止路径实测通过。
- e2e-results.md：登录行口令改 [REDACTED]；复现入口改为环境变量方式。
- 任务路径脱敏扫描：JWT 字面量 0；Bearer 仅探针请求头构造代码与测试防泄漏断言；日志中口令仅存 [REDACTED] 占位；backend 日志仅法务 COS 未配置属性名；业务单号 RVABCDEF0123456789 中与口令形似的数字片段属业务 ID，未改动；未打码口令值扫描 0。
- 业务结论与数字不变（17/17、6/6、零产品代码改动）。
