# CL-CONVERGED-MIGRATION-PACK-61 迁移顺序与现生产结构对照包

**只读对照。没有执行任何 SQL，没有起数据库，没有起 Java，没有改动任何原 SQL。**
本包不是发布许可；它只回答"现在生产缺什么、按什么顺序补、哪几步会锁表或可能失败"。

原 SQL 树：`artifacts/team-worktrees/codex-rc15-integrated-20260909/scripts/migrations`
（iPad 两份在 `banquet_project/src/main/resources`）。
副本在本目录 `sql/`，逐字复制，未改一个字符。
机器可读的结构抽取在 `plan.json`。

按卡要求**只收 `stocktake_precision_v1.sql`，没有收旧版 `stock_take_precision_v1.sql`**（两个文件名只差一个下划线，同目录并存，容易拿错）。

## 一、逐脚本 SHA256 与作用对象

| 脚本 | SHA256 | 建表 | 改表 |
| --- | --- | --- | --- |
| stocktake_precision_v1.sql | `02f88b91671325ba72118c0a5c22628353eba61c308f2f6da4d8549b0090638c` | — | stock_take_detail |
| payroll_approval_payout_v1.sql | `d0f627f1d13f2fa5dfdc677f708f45a75463805de26e4a7c7a5780688aae8677` | payroll_payout_record | month_salary, payroll_payout_record |
| payable_settlement_record_v1.sql | `5a9e0c93098e39200b646232503baf96c4efddb02483ad4a0429b824742300de` | payable_settlement_record | finance_payable, payable_settlement_record |
| payable_create_request_v1.sql | `1b3493a3f79d077ee192560cd26c2c20f694d8d3abcc83d18bc41e1318c0cadc` | payable_create_request | payable_create_request |
| receipt_payable_source_v1.sql | `79efbb28baecb5e01e07e174470d8276d6c9813f3e5f6366652a9127718c9d08` | — | purchase_receipt, finance_payable |
| receivable_payment_request_v1.sql | `881dcaa44bd85d9f18732751506c37fc59cf54c8ab592f6cb7d31d2e88b10193` | receivable_payment_request | finance_payment_record, finance_receivable |
| ipad_batch_request_migration_v1.sql | `62a4cecc085a670a9213c5a2b0c791f619fcca4bbc99d30ba4c4f8651237fb62` | ipad_batch_request | — |
| ipad_batch_request_migration_v2.sql | `924df5666090ffc703613c8281ed65a6de7799dc9c25e70e3f3eea4fae110f33` | — | booking_master, ipad_batch_request |

iPad 两份的收录依据：CO51 manifest 的 addedClasses 里有
`IpadBatchAuthorizationService` 与 `IpadBatchSubmissionService`，
这两个服务落的就是 `ipad_batch_request` 表。
manifest 里另有 IpadCheckout / IpadDish / IpadTable / IpadAuth 等控制器，
但它们对应的 `ipad_checkout_migration_v1.sql`（ipad_payment_request）
与 `ipad_device_binding_migration_v1.sql`（ipad_device_binding）**不在本卡点名范围**，
我没有擅自扩收——需要的话请明确追加。

## 二、现生产缺口（全部来自本地既有证据，未重查生产）

证据：TL44 `gaps.json` / `metadata.json`（采集于 2026-09-09 10:21:59）、
TL48 `schema_dump.json`（10:32:35）、TL33 runbook。

**财务四份：一份都没上生产，父表都在。**

- 父表存在：`finance_payable`、`finance_payment_record`、`finance_receivable`、`purchase_receipt` 均为 true。
- 缺表：`payable_create_request`、`payable_settlement_record`、`receivable_payment_request` 三张全部 **不存在**。
- 缺列：`finance_payable.source_receipt_id`、`finance_payable.source_receipt_no`、
  `finance_payment_record.payment_category` 全部 **不存在**。
- 缺键：`finance_payable.uk_finance_payable_id_store`、`uq_payable_receipt_source`、
  `idx_payable_store_receipt`、`purchase_receipt.uq_receipt_store_source`、
  `uk_finance_receivable_id_store` 全部 **不存在**。
- 四个脚本的 `all_applied` 均为 **false**。

**盘点：表在，精度是旧的。**

`stock_take_detail` 存在，当前
`system_quantity` / `actual_quantity` / `diff_quantity` 均为 `decimal(10,2)`，
`unit_price` 为 `decimal(10,2)`。
迁移把前三者改到 `DECIMAL(12,3)`、`unit_price` 改到 `DECIMAL(16,8)`。

**iPad：表不存在，v2 的前置也不满足。**

`ipad_batch_request` 生产不存在；`booking_master` 存在但**没有三列唯一索引**，
所以 v2 现在跑不了（TL33 记录 checker 输出 BLOCKED、零 DDL）。

**工资：本包不判定生产现状。**
TL44/TL48 的采集范围不含 `month_salary` / `payroll_payout_record`，
我不拿 CL50 隔离库的结构冒充生产结构。这一项列在第五节"未验证"。

## 三、执行顺序（有硬依赖，顺序不能调）

财务链内部有一处**真实依赖**，不是习惯问题：

> `payable_create_request_v1.sql` 的外键要引用 `finance_payable` 上的
> `uk_finance_payable_id_store`，而**这个唯一键是由 `payable_settlement_record_v1.sql`
> 第 48 行建的**（`ADD UNIQUE KEY uk_finance_payable_id_store (payable_id, store_id)`）。
> 顺序反了，create_request 直接建不出外键。
> create_request 脚本自己的注释也写了这件事。

建议顺序：

1. `payable_settlement_record_v1.sql` —— 先把 `uk_finance_payable_id_store` 立起来
2. `payable_create_request_v1.sql` —— 依赖上一步
3. `receipt_payable_source_v1.sql` —— 独立，动 purchase_receipt 与 finance_payable
4. `receivable_payment_request_v1.sql` —— 独立（应收侧）
5. `stocktake_precision_v1.sql` —— 独立，与财务无关
6. `payroll_approval_payout_v1.sql` —— 独立，自带深度自愈
7. `ipad_batch_request_migration_v1.sql`
8. **重新只读采集 + 跑统筹 checker**，确认 v1 表/列/索引就绪、孤儿与跨店为 0
9. `ipad_batch_request_migration_v2.sql`

第 8 步不是客套，是 TL33 runbook 明确的口径：v2 前置未满足时 checker 会 BLOCKED，
不能凭"v1 跑完了"就直接接 v2。

## 四、幂等与备份 / 锁风险（逐条，不含糊）

| 脚本 | 幂等做法 | 重复执行 | 锁 / 备份风险 |
| --- | --- | --- | --- |
| payroll_approval_payout_v1 | PREPARE/EXECUTE + 按"正确性"逐项比对，含预检 | **安全**，可反复跑 | 含 `DROP INDEX` / `DROP FOREIGN KEY`：只在同名结构定义不符时才触发。`payroll_payout_record` 多条 `MODIFY COLUMN` 会重建表 —— **执行前必须备份该表** |
| payable_settlement_record_v1 | `IF NOT EXISTS` | 建表部分安全；`ADD UNIQUE KEY` 部分**不幂等**，二次执行报重复键 | 在 `finance_payable` 上加唯一键：**若存在 (payable_id, store_id) 重复行会直接失败**，先查重 |
| payable_create_request_v1 | `IF NOT EXISTS` | 同上，外键部分不幂等 | 依赖上一步的唯一键，缺则失败 |
| receivable_payment_request_v1 | `IF NOT EXISTS` | 同上 | 在 `finance_receivable` 上加唯一键，同样要先查重 |
| receipt_payable_source_v1 | **无 `IF NOT EXISTS`、无 PREPARE 守卫** | **不幂等**，二次执行会因列/索引已存在而失败 | 动两张有数据的生产表（purchase_receipt、finance_payable），加唯一索引前必须查重；**这是本批风险最高的一份** |
| stocktake_precision_v1 | 无守卫，但 `MODIFY COLUMN` 到同定义是 no-op | 可重复，但每次都重建表 | `stock_take_detail` 四列 `MODIFY`，**表重建 + 锁**，按行数评估窗口，执行前备份 |
| ipad_batch_request_v1 | `IF NOT EXISTS` | 安全 | 新表，无历史数据风险 |
| ipad_batch_request_v2 | 无守卫 | **不幂等** | 在 `booking_master`（核心业务表，数据量最大）上加三列唯一索引：**存在重复即失败**，且索引构建期间有锁。风险仅次于 receipt_payable_source |

精度变更的数据安全性我算过：
`decimal(10,2)` → `(12,3)` 整数位 8 → 9、小数位 2 → 3，是**纯扩宽**；
`unit_price` `(10,2)` → `(16,8)` 整数位 8 → 8 不变、小数位 2 → 8，**也不截断**。
所以这一份不存在数据丢失风险，只有锁与重建成本。

## 五、生产 INT 必须保持 INT —— 逐列核过

TL48 的 `type_check_operator_supervisor_vs_staff` 实测生产：

- `staff_master.staff_id` = **int**，NOT NULL
- `stock_take.operator_id` = **int**，NULL
- `stock_take.supervisor_id` = **int**，NULL

本批八份里**没有任何一份把生产的 staff/operator 列改宽成 BIGINT**，这一点我逐份查过，是干净的。

唯一需要点名的是：`ipad_batch_request_migration_v1.sql` 里
`operator_id BIGINT NOT NULL`。说清楚它的实际影响，不夸大也不放过：

- 这是**新表自己的列**，该表**没有**指向 `staff_master(staff_id)` 的外键
  （v1 里唯一的外键是 `fk_ipad_batch_booking`，指向 `booking_master(id)`）。
  所以它**不会**在建表时因类型不匹配而失败。
- 但它与生产"操作人 ID 一律 INT"的口径不一致。将来若要给
  `ipad_batch_request.operator_id` 补一条指向 `staff_master(staff_id)` 的外键，
  BIGINT 对 INT 会直接建不上，届时只能改子列而**不能**去改生产的 `staff_master.staff_id`。
- 结论：**不要**为了"对齐"把生产 INT 改成 BIGINT。要动只动这张新表的子列。

另外声明一句：我在 CL50 用的隔离库 `co_pay50_20260909` 是**测试夹具，不是迁移**，
其中任何结构都不应被带进生产。该夹具的 `staff_id` 本身也是 INT，与生产一致，没有 BIGINT 适配可带。

## 六、明确未验证 / 我没做的

1. **工资两表的生产现状未知。** TL44/TL48 采集范围不含 `month_salary` 与
   `payroll_payout_record`，我没有它们的生产 metadata，因此**无法判断**
   `payroll_approval_payout_v1` 在生产上会走"新增"还是"自愈已有错误结构"分支。
   这条不能拿 CL50 隔离库顶替。精确查询见第七节。
2. **booking_id 长度兼容未验证。** TL33 明写：父 `booking_master.booking_id` 实际长度 50，
   v1 子定义 `VARCHAR(255)`，本机未做真实验证。这条沿用 TL33 的未验证声明，我没有替它下结论。
3. **各表重复数据是否会让唯一索引失败，未验证。** 上面点名的四处唯一键
   （finance_payable、finance_receivable、purchase_receipt、booking_master）
   我都只能指出风险，不能断言会不会失败——查重需要读生产，本卡禁止。查询见第七节。
4. **没跑任何 SQL、没起库、没起 Java、没改原 SQL、没碰法务、没查凭证、没做全库探索。**
5. iPad 的 checkout / device_binding 两份迁移不在本卡点名范围，未收录。

## 七、需要统筹代跑的精确查询（缺这些我判断不了，不自行广搜）

只读，四条，可直接贴进生产只读会话：

```sql
-- Q1 工资两表是否已存在，以及本次迁移涉及的列/键现状
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('month_salary','payroll_payout_record')
  AND COLUMN_NAME IN ('post_salary_snapshot','attendance_pay_snapshot','approved_by',
                      'approved_at','paid_by','paid_at','payout_id',
                      'salary_month','store_id','headcount','total_net','recorded_by','recorded_at','note')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- Q2 工资两表现有索引与外键（判断走新增还是自愈分支）
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('month_salary','payroll_payout_record')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE;

-- Q3 五处唯一索引会不会因重复数据建失败（每行返回 0 才安全）
-- 列名逐条取自本包 sql/ 里的脚本原文，不是我推的：
--   uk_finance_payable_id_store   (payable_id, store_id)      payable_settlement_record_v1.sql:48
--   uk_finance_receivable_id_store(receivable_id, store_id)   receivable_payment_request_v1.sql:36
--   uq_receipt_store_source       (store_id, receipt_id)      receipt_payable_source_v1.sql:4
--   uq_payable_receipt_source     (source_receipt_id)         receipt_payable_source_v1.sql:8
--   uk_booking_master_id_store_booking (id, store_id, booking_id) ipad v2:16
SELECT 'finance_payable.uk_finance_payable_id_store' AS target, COUNT(*) AS dup FROM (
  SELECT payable_id, store_id FROM finance_payable
  GROUP BY payable_id, store_id HAVING COUNT(*)>1) x
UNION ALL SELECT 'finance_receivable.uk_finance_receivable_id_store', COUNT(*) FROM (
  SELECT receivable_id, store_id FROM finance_receivable
  GROUP BY receivable_id, store_id HAVING COUNT(*)>1) x
UNION ALL SELECT 'purchase_receipt.uq_receipt_store_source', COUNT(*) FROM (
  SELECT store_id, receipt_id FROM purchase_receipt
  GROUP BY store_id, receipt_id HAVING COUNT(*)>1) x
UNION ALL SELECT 'booking_master.uk_booking_master_id_store_booking', COUNT(*) FROM (
  SELECT id, store_id, booking_id FROM booking_master
  GROUP BY id, store_id, booking_id HAVING COUNT(*)>1) x;

-- Q4 stock_take_detail 行数，用来估锁窗口
SELECT COUNT(*) FROM stock_take_detail;
```

`uq_payable_receipt_source (source_receipt_id)` 不用查重：`source_receipt_id`
这一列本身就是 `receipt_payable_source_v1.sql` 这次新加的，加上时全为 NULL，
MySQL 的唯一索引允许多个 NULL，所以不会冲突。列在这里只是为了让清单齐全。
