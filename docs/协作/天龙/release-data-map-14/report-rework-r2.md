# TL-RELEASE-DATA-MAP-14 返修报告（base 24c3c611 精确行号版·天龙）

- 对应 Codex：CX-dcc6727c59c6 / CX-5ad113dfe3cd（changes_requested 返修）
- base：24c3c611f1bf48796dfb2558dbc01758fcd260c6（**已精确 fetch 成功**：`git fetch origin codex/integration-next-20260908` 完成后 FETCH_EXIT=0，`git cat-file -t 24c3c611…`=commit；经 detached `git show` 只读核对）
- 方式：只读；未碰生产/法务/配置；未重扫全仓
- 更新时间：2026-09-13 11:5x

## 一、七个缺口的 base 精确行号（detached 只读核对）

| # | 缺口 | base 24c3c611 精确行号 |
|---|---|---|
| 1 | 工资两套链路并存 | 新：PayrollController.java:41（@RequestMapping /api/hr/payroll；/save:219、/approve:244、/pay,/payout:263）；旧：SalaryController.java:17（/api/hr/salary；/calculate:73、/{id}/push-finance:85） |
| 2 | PayrollController 直连 JdbcTemplate | PayrollController.java:44-45（@Autowired JdbcTemplate jdbc）；同文件 :246 又调 payrollService.approve —— **JDBC 与 Service 混用** |
| 3 | payroll_payout_record 生产缺失 | 迁移 scripts/migrations/payroll_approval_payout_v1.sql:28-40（base 内存在）；生产未应用（见证据时间） |
| 4 | 迁移双口径 | **修正**：base 内**无第二脚本**（db/migration 仅 inquiry_booking_link_v1.sql）；"双口径"为 09-08 后续引入，相对 base 不成立 |
| 5 | finance_payable 缺来源列 | FinancePayableController.java:20（/api/finance/payables）、:127（@PostMapping 创建应付）；来源列 source_receipt_id 缺 |
| 6 | stock_take_detail 空表 | StockTakeController.java:41（/api）、:141（POST /stock-takes）、:352（POST /stock-takes/{id}/details） |
| 7 | 无隔离环境 | 环境约束（无隔离 MySQL），无源码行号 |

## 二、权威主键 / 门店键 / 来源单号 / 状态字段 / 守恒点（base 迁移脚本 :3-:44 + 生产元数据）

| 表 | 主键 | 门店键 | 来源单号 | 状态字段 | 守恒点 |
|---|---|---|---|---|---|
| month_salary | salary_id (bigint auto_inc) | store_id(FK store_info) | — | status：1=保存/2=审批/3=发放（迁移 :3,:8） | net_salary=gross_salary−各扣减+各补贴（decimal(10,2)） |
| payroll_payout_record | payout_id(BIGINT auto_inc, :29) | store_id(NULL=全门店, :31) | 批次 payout_id + salary_month | —（台账） | 唯一键 uk_payout_month_identity(payout_id,salary_month)(:38)；FK store_id→store_info(:39)、(payout_id,salary_month)→本表(:44) |
| finance_payable | payable_id | store_id | source_receipt_no（缺） | status | paid_amount + pending_amount = 应付合计 |
| purchase_receipt | receipt_id | store_id | receipt_no | — | 收货主从金额一致 |
| stock_take | take_id(bigint) | store_id(NOT NULL) | — | status varchar(20)（默认 draft；实测枚举 completed） | 明细数量合计 → 账实差基础 |
| stock_take_detail | detail_id(bigint) | store_id | take_id(FK stock_take) | diff_type varchar(20) | 明细数量差 = 盘盈/盘亏 |

关键业务语义（迁移脚本 :12）：**status=3 仅表示账上记账，不代表款项已到员工银行账户。**

## 三、9月9日生产证据采集时间（当日采集，非 2026-09-13 现状）

| 证据 | 2026-09-09 采集时间 |
|---|---|
| TL-PROD-STOCKTAKE-RELATION-35 | 09:39:48 |
| TL-PROD-PAYROLL-RELATION-38 | 09:48:59 |
| TL-PROD-FINANCE-SCHEMA-44 | 10:21:59 |
| TL-PROD-STOCKTAKE-SCHEMA-48 | 10:3x |
| TL-RELEASE-METADATA-GATES-64 | 11:17 |

## 四、结论

七个缺口行号已按 base 24c3c611 精确化；缺口 4 经核对修正为"base 无双口径"。字段与守恒点补齐，9-9 证据均标采集时间。只读，未碰生产/法务/配置。
