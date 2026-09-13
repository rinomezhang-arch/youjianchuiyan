# TL-RELEASE-DATA-MAP-14 返修报告（blocked：base 取不到）+ 字段补全

- 对应 Codex：CX-5ad113dfe3cd（changes_requested 返修）
- 返修时间：2026-09-13 11:4x
- 法务冻结，只读，未碰生产/法务/配置

## 一、返修阻塞：固定 base 24c3c611 本机取不到（BLOCKED）

按 review 要求精确 fetch `origin/codex/integration-next-20260908` 后 detached 只读核对，但多轮 fetch 均未取到 base 对象。原始 fetch 错误（原样）：

1. `timeout 120 git fetch origin codex/integration-next-20260908` -> exit=124（120s 超时，无输出）
2. `git fetch origin 24c3c611f1bf48796dfb2558dbc01758fcd260c6` -> exit=0，但随后 `git cat-file -t 24c3c611...` -> `fatal: git cat-file: could not get object info`（未真正传输对象）
3. `timeout 600 git fetch origin codex/integration-next-20260908`（后台）-> 超 600s 未完成，`git cat-file` 仍 could not get object info
4. `git ls-remote origin` -> `refs/heads/codex/integration-next-20260908 = 238f30600db76aa227031d01dbadcd03f436395b`；24c3c611 不出现在任何远程引用（非分支/标签 HEAD）

=> base 无法 detached 核对，**本次返修按 Codex 指示置为 blocked**；行号精确化待提供可 fetch 的远端或 base 产物后继续。

## 二、不依赖 base 的已完成补充（基于已缓存证据）

### 权威主键 / 门店键 / 来源单号 / 状态字段 / 守恒点

| 表 | 权威主键 | 门店键 | 来源单号 | 状态字段 | 守恒点 |
|---|---|---|---|---|---|
| month_salary | salary_id (bigint, auto_inc) | store_id (FK store_info) | — | status int(默认0) | net_salary = gross_salary - 各项扣减 + 各项补贴；gross/net/tax decimal(10,2) |
| payroll_payout_record | 生产不存在（TL64） | store_id(规范) | — | （规范待定） | 发放台账：settle 金额累计需与 month_salary 一致 |
| finance_payable | payable_id | store_id | source_receipt_no（缺，TL44） | status | paid_amount + pending_amount = 应付款合计 |
| purchase_receipt | receipt_id | store_id | receipt_no | — | 收货主从金额一致（TL-OPS-RECEIPT-BALANCE-01） |
| stock_take | take_id (bigint) | store_id (NOT NULL) | — | status varchar(20)（默认 draft；实测枚举仅 completed） | 明细数量合计 = 盘点账实差基础 |
| stock_take_detail | detail_id (bigint) | store_id | take_id (FK stock_take) | diff_type varchar(20) | 明细数量与账面数量差 = 盘盈/盘亏 |

唯一键：month_salary uk_staff_month(staff_id,salary_month)；stock_take_detail FK (ingredient_id,store_id)->ingredient_master、(take_id)->stock_take

### 9月9日生产证据采集时间（均为当日采集，不代表 2026-09-13 现状）

| 证据 | 采集时间(2026-09-09) |
|---|---|
| TL-PROD-STOCKTAKE-RELATION-35 | 09:39:48 |
| TL-PROD-PAYROLL-RELATION-38 | 09:48:59 |
| TL-PROD-FINANCE-SCHEMA-44 | 10:21:59 |
| TL-PROD-STOCKTAKE-SCHEMA-48 | 10:3x |
| TL-RELEASE-METADATA-GATES-64 | 11:17 |

## 三、原七缺口（映射保留，行号待 base 核对）

同前版报告 7 条（工资双链路、PayrollController 直连 Jdbc、payroll_payout_record 缺失、迁移双口径、finance_payable 缺来源列、stock_take_detail 空表、无隔离库）。行号需 base 到位后再精确化。

## 四、结论

BLOCKED：base 24c3c611 本机不可得（原始错误见第一节）。已完成字段与采集时间补充；行号精确化待 Codex 提供可 fetch 远端或 base 产物。
