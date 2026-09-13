# TL-RELEASE-DATA-MAP-14 返修报告 R3（base 24c3c611 精确行号 · 六项以内可复核结论）

- 执行人：天龙（Tianlong）
- 对应 Codex：CX-97bfc0448c94 / 任务板 R2 changes_requested（event 00000008，2026-09-13T07:34:43Z）
- 固定 base：24c3c611f1bf48796dfb2558dbc01758fcd260c6（可复现；detached 只读 `git show` 核对）
- 方式：只修报告与只读证据，未重新全盘扫描，未连接生产，未改业务代码/配置/法务
- 证据时效：凡旧生产证据均为 **2026-09-08 / 2026-09-09 采集**；本轮未连生产复核，一律标注“当前未复核”

## 一、R2 退回五项修正对照

| R2 退回项 | 本版修正 |
|---|---|
| 1 工资审批调用行 | 实际调用为 PayrollController.java:247（`payrollService.approve(...)`）；:246 只是 `try {`。同表同步补 save 调用 :228、payout 调用 :266 |
| 2 缺口计数 | 第 4 项“迁移双口径”在固定基线**不存在**，删除；第 2 项改列“分层观察”、第 7 项改列“环境限制”，均不计入缺口；不再写“七个缺口” |
| 3 应付来源 | 以实体 FinancePayable.java:52/:55 与迁移 scripts/migrations/receipt_payable_source_v1.sql:5-11 为直接依据；`source_receipt_id` 与 `source_receipt_no` **两列同时写清**，旧证据两列均缺 |
| 4 stock_take_detail | detail 计数=0 时关联检查标 **NOT_COVERED**；只陈述 9-9 旧快照存在两张 completed 空主单，不由空表推断当前明细落盘链未走通 |
| 5 证据时效 | 所有旧生产证据标注采集日期（9-8 / 9-9）并写明“当前未复核” |

## 二、可复核结论：4 项已证业务缺口 + 2 项非缺口说明（合计六项）

### 已证业务缺口

**G1 工资两套链路并存（入口口径冲突）**
- base 行号：新链路 PayrollController.java:41（`@RequestMapping /api/hr/payroll`；save 映射 :219 → 实际调用 :228；approve 映射 :244 → 实际调用 :247；pay/payout 映射 :263 → 实际调用 :266）；旧链路 SalaryController.java:17（`@RequestMapping /api/hr/salary`；/calculate :73、/{id}/push-finance :85）
- 最小反例：同一 store/staff/month 可分别经 `/api/hr/payroll/save` 与 `/api/hr/salary/calculate` 落到同一张 month_salary
- 预期：单一权威入口；实际：两套并存且写同一表
- 证据索引：本目录 report.md（映射表与缺口原始条目）

**G2 payroll_payout_record 生产缺失（原第 3 项）**
- base 行号：scripts/migrations/payroll_approval_payout_v1.sql:28-41 建表（:38 复合唯一键 `uk_payout_month_identity(payout_id,salary_month)`；:39 `FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT`；:40 `KEY idx_payout_month (salary_month, store_id)`）
- **归属修正**：同脚本 :44 是 `month_salary` 指向发放台账的外键 `fk_salary_payout_month (payout_id,salary_month) → payroll_payout_record(payout_id,salary_month)`，**不属于 payroll_payout_record 自身的关系**（R2 退回第 1 点）
- 预期：发放台账可落账；实际：该迁移在生产未应用（旧证据 TL-RELEASE-METADATA-GATES-64，2026-09-09 11:17 采集，**当前未复核**）

**G3 finance_payable 应付来源两列缺失（原第 5 项）**
- 实体依据：FinancePayable.java:52（`@Column(name = "source_receipt_id")` → :53 `Long sourceReceiptId`）、:55（`@Column(name = "source_receipt_no", length = 50)` → :56 `String sourceReceiptNo`）
- 迁移依据：scripts/migrations/receipt_payable_source_v1.sql:5-11 —— :6 `ADD COLUMN source_receipt_id BIGINT NULL`、:7 `ADD COLUMN source_receipt_no VARCHAR(50) NULL`、:8 唯一键 `uq_payable_receipt_source(source_receipt_id)`、:9 索引 `idx_payable_store_receipt(store_id, source_receipt_id)`、:10-11 外键 `fk_payable_store_receipt(store_id, source_receipt_id) → purchase_receipt(store_id, receipt_id) ON DELETE RESTRICT ON UPDATE RESTRICT`
- 旧证据：TL-PROD-FINANCE-SCHEMA-44（2026-09-09 10:21:59 采集）显示 `source_receipt_id` 与 `source_receipt_no` **两列均缺失**（不是只缺一列），**当前未复核**

**G4 stock_take_detail 关联检查 NOT_COVERED（原第 6 项）**
- base 行号：StockTakeController.java:41（`@RequestMapping /api`）、:141（`POST /stock-takes`）、:352（`POST /stock-takes/{id}/details`）
- 旧快照事实（2026-09-09 采集）：存在两张 `status=completed` 的空主单，stock_take_detail 计数 = 0
- 判定：明细计数为 0，无对象分母 → 明细关联检查标 **NOT_COVERED**；不得由空表推断“当前明细落盘链未走通”
- 证据索引：TL-PROD-STOCKTAKE-RELATION-35（2026-09-09 09:39:48）、TL-PROD-STOCKTAKE-SCHEMA-48（2026-09-09 10:3x 采集），**当前未复核**

### 非缺口说明（不计入缺口数）

**N1（原第 2 项）分层观察**：PayrollController.java:44-45（`@Autowired` + `JdbcTemplate jdbc`）、:46（另一 `@Autowired`）与 :247（调 PayrollService）。属**分层观察**；本轮无业务反例（旧快照未提供可复现的读写不一致记录），不列为已证生产缺口。

**N2（原第 7 项）环境限制**：本机无隔离 MySQL、未接入生产只读通道。属**环境限制**，不是业务缺口。

**已删除项（原第 4 项）**：核查确认固定基线内不存在第二迁移脚本（`db/migration` 仅 `inquiry_booking_link_v1.sql`），“迁移双口径”相对固定基线不成立，不再计为缺口。

## 三、映射表（权威主键 / 门店键 / 来源单号 / 状态字段 / 守恒点）

| 表 | 权威主键 | 门店键 | 来源单号 | 状态字段 | 守恒点 |
|---|---|---|---|---|---|
| month_salary | salary_id (bigint auto_increment) | store_id（FK store_info） | — | status：1=保存 / 2=审批 / 3=发放记账 | net_salary = gross_salary - 各项扣减 + 各项补贴（decimal(10,2)） |
| payroll_payout_record | payout_id (BIGINT auto_increment，迁移 :29) | store_id（NULL=全门店，迁移 :31） | 批次 payout_id + salary_month | —（发放台账，无 status 列） | 复合唯一键 `uk_payout_month_identity(payout_id,salary_month)`（:38）；`store_id → store_info` FK（:39）；被 month_salary 指向的外键（:44） |
| finance_payable | payable_id | store_id | source_receipt_no（同列对 source_receipt_id） | status | paid_amount + pending_amount = 应付合计 |
| purchase_receipt | receipt_id | store_id | receipt_no | — | 迁移 :4 唯一键 `uq_receipt_store_source(store_id,receipt_id)`；收货主从金额一致 |
| stock_take | take_id (bigint) | store_id（NOT NULL） | — | status varchar(20)（默认 draft；旧快照枚举仅 completed） | 明细数量合计 → 账实差基础 |
| stock_take_detail | detail_id (bigint) | store_id | take_id（FK stock_take） | diff_type varchar(20) | 明细数量差 = 盘盈/盘亏；计数为 0 时关联检查 NOT_COVERED |

关键业务语义（迁移脚本 :12）：**status=3 只表示账上记账，不代表款项已到员工银行账户。**

## 四、旧生产证据采集时间与时效声明

| 证据 | 采集时间 | 时效声明 |
|---|---|---|
| TL-PROD-STOCKTAKE-RELATION-35 | 2026-09-09 09:39:48 | 当前未复核（本轮未连生产） |
| TL-PROD-PAYROLL-RELATION-38 | 2026-09-09 09:48:59 | 当前未复核 |
| TL-PROD-FINANCE-SCHEMA-44 | 2026-09-09 10:21:59 | 当前未复核 |
| TL-PROD-STOCKTAKE-SCHEMA-48 | 2026-09-09 10:3x | 当前未复核 |
| TL-RELEASE-METADATA-GATES-64 | 2026-09-09 11:17 | 当前未复核 |

上述证据一律按采集日期陈述，不表述为 2026-09-13 现状；本轮无新生产复核结论。

## 五、边界与产出

- 只改 `docs/协作/天龙/release-data-map-14/**`：未改业务代码、未连生产、未碰配置、未碰法务，未重新全盘扫描。
- 产出：本报告（并保留 report.md、report-rework-blocked.md、report-rework-r2.md 作为历史），提交见任务板 reported 回执所载 commit SHA。
