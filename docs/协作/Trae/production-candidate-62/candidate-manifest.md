# TR-PRODUCTION-CANDIDATE-62 生产候选 manifest

执行人：Trae。日期：2026-09-14。本卡 base_sha：ccf24b31de9853f9190d51d23914b060928a4b44。

## 一、候选标识

- 候选分支：origin/codex/integration-20260913
- 候选精确 SHA：**6cf5e6d15fc908a159d4aa29b64d1e3bdb82a652**（git ls-remote 读回核对一致）
- 候选关系：生产 HEAD 3fe7884b 是候选祖先（merge-base=3fe7884b），候选严格领先。
- CO60 候选 e226289f 经核对**不在**候选内（merge-base --is-ancestor 退出码 1），符合"须等 CL61 复核和 Codex 验收，不提前合并"；候选内已有的是 6e88f7a7/6cf5e6d1 两次收款整合提交（即已合的 CO60/CO63 内容）。TR06 代码由 CO60 接管，未重复。

## 二、五条实际经营链核查

### 1. 采购入库链 —— NOT_VERIFIED
- 页面/接口/业务主键/关联表：代码存在（GoodsReceiptItem、GoodsReceiptRepository、InventoryController 等），但 docs/协作 全目录无采购入库专属报告（grep"采购/入库"零命中），无 reviewed 任务卡。
- 关联但不等同的证据：应付链（见第 3 方括号外补充：payable-http / payable-create-v2 / payable-browser）覆盖应付创建、结算、流水，不等于采购入库。
- 阻断：无任务卡无证据。负责人：Codex（排期发任务）。

### 2. 盘点落盘链 —— 部分覆盖
- 已 reviewed：TR-STOCKTAKE-DECIMAL-52（stock_take_detail 精度 DECIMAL(12,3)/unit_price DECIMAL(16,8) 迁移）。
- 页面操作：盘点单创建→明细录入→完成落盘→回读。接口：StockTakeController（/stocktake/*）。业务主键：盘点单号。关联表：stock_take、stock_take_detail。角色：经理/超管。
- 阻断：TR-RC-STOCKTAKE-REAL-37 blocked（真实浏览器+HTTP 11 组断言未跑完；JAR 已构建、隔离库 banquet_tr37 已生成 81 表、seed 夹具已写未 apply，现场完整保留在 artifacts/release-candidates/tr37-stocktake-20260909）。负责人：Trae。
- 另注：docs/协作/天龙/stocktake-persistence-audit-33 目录在两分支均无已提交文件，无可用证据。

### 3. 工资审批付款链 —— HTTP 级充分
- 页面操作：工资月结→审批→付款。接口：PayrollController（/payroll/*）。关联表：month_salary 及台账/明细表。
- 证据：Codex/payroll-http（PayrollHttpMysqlFlowTest：真实 JWT、StoreDataScope、AuditLog、事务代理、台账/明细守恒、拒绝场景）；Codex/payroll-fix-20（month_salary 列与索引迁移修复，隔离 schema 保留）；Codex/payroll-contract-audit（字段到列契约审计逐项表）。
- 缺口：浏览器级三角色（秋哥/张总/员工）端到端未见证据 → 记 NOT_VERIFIED 子项。负责人：待 Codex 派工。

### 4. 门店收款流水链 —— 部分覆盖（一项未收口）
- 已 reviewed：TR-OPS-RECEIVABLE-REAL-E2E-03、TR-RECEIPT-QUICK-01（16 项通过）。
- Codex 证据：receivable-recovery-13（应收恢复：业务单号、requestId、金额守恒、幂等登记）；receipt-r1-29（小票链：真实浏览器点击、PDF 导出、DB 断言、孤儿计数）；integration-receipt-port-34（端口整合闸门）。
- 接口：/finance/receivable/*、GET /api/bills/{bookingId}/receipt?storeId。业务主键：收款流水号、订单号 bookingId。关联表：finance_receivable 及收款流水表。角色：前台/经理/超管。
- 阻断：TR-RECEIPT-REAL-24 changes_requested r5（脚本三处写死 13317 端口，端到端证据不成立；返工范围已明确）。负责人：Trae。

### 5. 客人营销咨询回查链 —— 充分（末环刚交验收）
- 已 reviewed：TR-MARKETING-H5-UI-39、TR-MARKETING-INQUIRY-LOOKUP-UI-56、TR-MARKETING-INQUIRY-API-R2-57。
- 页面操作：公开 H5 活动页→提交咨询（sourceCode）→得咨询号→打开回查深链接→输入手机号→状态回读→刷新回读。角色：客人（免登）。
- 接口：POST /api/public/booking-inquiry、lookup 回查接口。业务主键：inquiryNo（实测 INQ3）、requestId。关联表：booking_inquiry、marketing_attribution_event（event_type=inquiry、business_type=booking_inquiry、ID 勾稽）。
- 末环：TR-MARKETING-INQUIRY-REAL-E2E-58 本日已交 R3 reported（截图精确脱敏 5/5 人工核对 + R3 报告；同载荷重放零新增由 Codex 在 ccf24b31 补证）。待 Codex 验收。负责人：Codex（验收）。

## 三、账号登录与批复入口

- 秋哥（rino）与张总（zhangjing）：/login SPA 入口，role=gm、permission_level=10。**候选上真实黑盒三账号登录验收：NOT_VERIFIED**（9-13 冲刺计划列为缺口；不以页面 200 代替）。负责人：待 Codex 派工。
- 张律师（lawyer）：真实入口 /case/ 静态页整页跳转；TR-AUTH-SHELL-14 已 reviewed（lawyer 仅见 legal 入口、守卫整页送 /case/、角色以服务端 /auth/me 为权威、localStorage 篡改提权被拒）。法务模块冻结，本卡仅核对 hash 与保留路径。
- /dashboard/legal 为占位壳，不作为法务页面验收。

## 四、线上与候选只读差异（diff 使用 --ignore-cr-at-eol，等价 diff --strip-trailing-cr 语义）

生产实况：源码 ~/deploy_tmp_main/banquet_project，HEAD=3fe7884b（2026-08-27）+未提交改动；
运行 JAR target/banquet-1.0.0.jar，SHA256=2a9e033582963d62d1c6c227635b488303f63d7f1bb896d8271cc580d3544785，
构建时间 2026-09-13 13:37:32 +0800，大小 73092901；法务资源 14 项（case_rules.txt、case_dossier.txt、
evidence_index.json、case.html、case-timeline.html、answer_cards.json、retrieval_rules.txt、
routing_rules.txt、court_rules.txt、workspace.html、Legal*.class 等）已确认在 jar 内。

文件级差异（生产 src+pom.xml vs 候选 banquet_project/src+pom.xml）：**188 个文件**
（M=55 双侧不同 / A=131 仅候选新增 / D=2 仅线上存在）。全部 diff 明细：scripts/trae-production-candidate-62/tr62-diff-namestatus.txt 与 tr62-diff-stat.txt。

### 线上较新/独有文件（标记保留，任何发布不得覆盖）

| 路径 | 性质 | 处置 |
|---|---|---|
| src/main/java/com/youjian/banquet/common/LoginCredential.java | 线上独有未提交 | 保留；候选无同类实现，发布前须由 Codex 裁决并入或保留 |
| src/main/java/com/youjian/banquet/config/RoleScopeInterceptor.java | 线上独有未提交 | 保留；候选无同类实现，WebMvcConfig 线上版注册依赖它 |
| src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java | 线上未提交改动（与候选差 62/42 行） | 裁决并入 |
| src/main/java/com/youjian/banquet/config/WebMvcConfig.java | 线上未提交改动（21/9 行） | 裁决并入 |
| src/main/java/com/youjian/banquet/controller/AuthController.java | 线上未提交改动（90/149 行） | 裁决并入 |
| src/main/java/com/youjian/banquet/controller/IpadAuthController.java | 线上未提交改动（15/25 行） | 裁决并入 |
| src/main/resources/application-prod.yml | 生产配置（内容保密，未展开） | 永不覆盖，发布流程排除 |
| src/main/java/com/youjian/banquet/entity/StaffMaster.java | 线上改动已与候选内容一致（仅 CRLF） | 已收敛，无需处理 |

### 法务受保护路径（仅 hash 登记，未读取内容）

- config/LegalCosConfig.java sha256=02022bf78b83b1431c7e1a8116eeda6ee417af5569e1deededb0816197b8c039
- controller/LegalController.java sha256=8b09652fd2abbf8fbb86d0d513efc8e3a81a44bef66450cbede8964385296231
- service/LegalEvidenceService.java sha256=476b693bc5e1bf9e60c88f8c3c99105dd4b560ac7753fde16adcde7bf6fc76cb
- service/LegalRetrievalService.java sha256=da7e8151bf34617cc4539ee51f23b9a636384fc6c223c05f435e79e6496d169e
- resources/legal/ 7 个文件 hash 见 scripts/trae-production-candidate-62/legal-hashes.txt

## 五、发布结论

**不可发布（NOT_PUBLISHABLE）**。阻断与负责人：

| # | 阻断 | 负责人 |
|---|---|---|
| 1 | 采购入库链零证据（无任务卡） | Codex 排期 |
| 2 | 三账号候选黑盒登录未验收 | Codex 派工 |
| 3 | TR-RECEIPT-REAL-24 r5 端口缺陷未返工 | Trae |
| 4 | TR-RC-STOCKTAKE-REAL-37 真实链未跑完（blocked 现场保留） | Trae |
| 5 | TR58 R3 已交待验收 | Codex |
| 6 | 线上 6 项未提交改动 + 2 个独有类须裁决并入，否则发布会丢失登录/角色域线上行为 | Codex 裁决 |

未执行：产品修改 0、迁移 0、生产写入/重启 0、法务触碰 0（hash 除外）、DELETE/DROP/TRUNCATE 0。
