# TL-RELEASE-DATA-MAP-14 餐饮实体迁移与业务数据流差异核查（天龙 · 续作）

- 对应 Codex 续派：CX-0240a2014ae8
- 固定 base：24c3c611f1bf48796dfb2558dbc01758fcd260c6（**本机不可得**：本地仓库无此对象；`git fetch origin codex/integration-next-20260908` 未取到。当前远程该分支 HEAD=238f306）
- 映射基准：当前工作树 HEAD 1a6d840 + 已缓存证据（行号可能与 base 有偏移，已逐条标注）
- 方式：只读；未碰生产/法务/配置；未重新全盘搜索
- 复用证据：payroll-contract-gap-10 / payroll-payment-e2e-09 / payroll-payout-guard-11 / payroll-migration-canonical-13 / payroll-migration-preflight-12 / TL44 / TL64 等

---

## 一、工资审批支付：实体 → 表 → 接口 → 业务流

| 实体 | 表 | 接口（当前工作树行号） | 业务流 |
|---|---|---|---|
| 工资单（新链路） | month_salary | PayrollController `/api/hr/payroll`（PayrollController.java:41）；GET :51、POST /unlock :180、POST /lock :198 | 生成→锁定/解锁审批→发放 |
| 工资单（旧链路） | month_salary | SalaryController `/api/hr/salary`（SalaryController.java:17）；template CRUD :24/:33/:42/:51、GET :62、POST /calculate :73、POST /{id}/push-finance :85、GET /staff/{staffId} :96 | 模板→核算 calculate→推送财务 |
| 发放流水 | payroll_payout_record | 无独立 Controller（随 PayrollController/SalaryService 落账） | 发放台账（幂等） |
| 员工 | staff_master | staff_id 主键 | 关联核算对象 |

## 二、采购入库盘点：实体 → 表 → 接口 → 业务流

| 实体 | 表 | 接口（当前工作树行号） | 业务流 |
|---|---|---|---|
| 采购申请/单 | purchase_request / purchase_receipt | PurchaseController `/api/purchases`（PurchaseController.java:51）；GET :59、GET/{id} :65、POST :91、PUT :123、approve :147、DELETE :170、procurement/requests :190、procurement/receipts :218 | 申请→建单→审批 |
| 收货 | goods_receipt / goods_receipt_item | 随采购/入库接口（无独立 Controller） | 收货→入库 |
| 应付 | finance_payable | FinancePayableController `/api/finance/payables`（FinancePayableController.java:20）；GET :32、POST :41 | 入库→应付生成 |
| 库存 | ingredient_inventory_log / inventory_summary | InventoryController `/api/inventory`（InventoryController.java:48）；summary :58、logs :64、in :103、out :117、loss :162、transfer :205 | 出入库/报损/调拨 |
| 盘点 | stock_take / stock_take_detail | StockTakeController `/api`（StockTakeController.java:34）；stock-takes :54/:71/:89/:115/:140、details :161/:170、stock-losses :194/:211/:229、chef-approve :261、store-approve :287、finance-confirm :316 | 建盘点→录明细→差异→报损审批链 |

---

## 三、缺口清单（文件行号 / 最小反例 / 预期 / 实际 / 证据索引）

1. **工资两套链路并存，口径冲突**
   - 行号：PayrollController.java:41（新）vs SalaryController.java:17（旧）
   - 最小反例：同一 store/staff/month 同时可从 `/api/hr/payroll` 与 `/api/hr/salary/calculate` 触发
   - 预期：单一权威入口；实际：两套并存且落同一 month_salary
   - 证据：payroll-contract-gap-10/report.md

2. **PayrollController 直连 JdbcTemplate，无 Service 层**
   - 行号：PayrollController.java:44-45（@Autowired JdbcTemplate jdbc）
   - 最小反例：Controller 内直接拼 SQL，无事务/契约边界
   - 预期：Controller→Service→DAO 分层；实际：控制器直连 JDBC
   - 证据：本报告第二节 + payroll-contract-gap-10

3. **payroll_payout_record 表在生产不存在**
   - 行号：迁移 scripts/migrations/payroll_approval_payout_v1.sql（未应用）
   - 最小反例：information_schema 查 payroll_payout_record 返回 0 行（表缺失）
   - 预期：发放台账表存在；实际：生产未建（TL64 门禁确认）
   - 证据：TL-RELEASE-METADATA-GATES-64 report.md

4. **工资迁移双口径（冲突第二入口）**
   - 行号：正式 scripts/migrations/payroll_approval_payout_v1.sql vs 废弃 db/migration/V20260908_01__payroll_payout_record.sql
   - 预期：唯一执行入口；实际：两条脚本口径冲突（项目无 Flyway，V__ 不自动执行但留隐患）
   - 证据：payroll-migration-canonical-13/report.md

5. **finance_payable 缺 source_receipt_id/source_receipt_no，采购→入库→应付结构未闭合**
   - 行号：FinancePayableController.java:41（创建应付）需 receipt 来源列
   - 最小反例：information_schema 查 finance_payable 无 source_receipt_id（TL44 确认）
   - 预期：应付可溯源收货；实际：来源列缺失，链路断
   - 证据：TL-PROD-FINANCE-SCHEMA-44 gaps.json

6. **stock_take_detail 空表，completed 盘点无明细**
   - 行号：StockTakeController.java:170（POST details）
   - 最小反例：stock_take 2 条 completed，stock_take_detail 0 行
   - 预期：盘点单有明细；实际：明细表空，落盘链路未走通
   - 证据：TL-PROD-STOCKTAKE-RELATION-35 report.md（已交 Trae 真实复验）

7. **无隔离环境导致工资 E2E 未闭环**
   - 行号：—（环境约束）
   - 预期：唯一隔离 MySQL + 隔离账号；实际：仅 banquet 生产库
   - 证据：payroll-payment-e2e-09/report.md（blocked）

---

## 四、证据索引（本机路径）

- docs/协作/天龙/payroll-contract-gap-10/report.md
- docs/协作/天龙/payroll-payment-e2e-09/report.md + raw_evidence.txt
- docs/协作/天龙/payroll-payout-guard-11/{report.md,correction.md}
- docs/协作/天龙/payroll-migration-canonical-13/、payroll-migration-preflight-12/
- docs/协作/天龙/prod-diff-02/（生产源码差异 manifest）

## 五、限制与待复核

- base 24c3c611 不可得：所有源码行号取自当前工作树 1a6d840，与 base 可能有偏移；如需 base 精确行号，请提供可 fetch 的远端或 base 产物。
- 缺口 4/5/6 的行号为接口入口行号，具体落账语句行号可定向复核（未全盘搜索）。
