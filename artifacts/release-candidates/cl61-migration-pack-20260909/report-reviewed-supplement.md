# CL61 复核补正件

首次 `report.md` 与全部原 SQL 原样保留、未覆盖。本件按统筹三条复核意见补正，
并用 TL64 送达的生产元数据关闭首次报告里留的缺口。
仍是**纯静态对照：没有执行任何 SQL，没有起库或 Java，没有改动任何原 SQL**。

新证据来源：`artifacts/coordination-r3/reviews/tl64-release-metadata/tl64-raw-result.txt`（58 行，逐行读过）。

---

## 补正一：补收 CO51 所需的另外两份 iPad 迁移

首次交付我以"不在卡内点名范围"为由没收，统筹已授权，现补上。原 SQL 复制进 `sql/`，未改一字。

| 脚本 | SHA256 | 建表 |
| --- | --- | --- |
| ipad_checkout_migration_v1.sql | `22b95e0ea449852947fbefd8896da9d00cbb1722363c0847a153e95b2e7f15e0` | ipad_payment_request |
| ipad_device_binding_migration_v1.sql | `938e56b61888917fa87e2d14f723868318ecc60886a3f5cb0d3a4d9d718c9117` | ipad_device_binding |

**缺口已由 TL64 关闭：两张表在生产上都已经存在。**

- `ipad_payment_request`：8 列，PK `payment_request_id`，
  `uk_ipad_payment_store_key (store_id, idempotency_key)`、`idx_ipad_payment_booking (store_id, booking_id)`
  —— 与脚本定义的键**一致**。`operator_id bigint`。
- `ipad_device_binding`：9 列，PK `id`，索引为 `device_sn`(唯一)、`idx_device_sn`、`idx_store_id`。

两份脚本都是 `CREATE TABLE IF NOT EXISTS`，表既然已存在，
**这两份在生产上执行等于空转，不会新建任何东西**。

---

## 补正二：我首次判断的那条"阻断级"外键问题，被 TL64 推翻，如实撤回

我在本件初稿里把 `ipad_device_binding_migration_v1.sql` 的两条外键
（`REFERENCES store_info(id)` 与 `REFERENCES staff_master(id)`）判为**阻断级**。
TL64 到手后这个结论**不成立，我撤回**。分三层说清楚，哪层对哪层错：

**(a) 脚本文本确实写错了 —— 这一层成立。**
TL64 里 `month_salary` 现有的两条生产外键写得明明白白：
`fk_month_salary_staff staff_id → staff_master(staff_id)`、
`fk_month_salary_store store_id → store_info(store_id)`。
所以生产被引用的列**就是 `staff_id` 和 `store_id`，不是 `id`**。
脚本里的 `staff_master(id)` / `store_info(id)` 是错的，
和同批 `payroll_approval_payout_v1.sql` 的 `REFERENCES store_info(store_id)` 也确实矛盾。

**(b) 但它不会触发 —— 这一层是我错了。**
`ipad_device_binding` 生产上**已经存在**，而脚本是 `CREATE TABLE IF NOT EXISTS`，
整个建表语句连同那两条外键**根本不会被执行**。
所以它不阻断任何东西。我先前说"只要列名问题解决，类型问题就会紧接着挡住"，
是在没有生产证据的情况下推演出来的，**推演的前提（表不存在）就不对**。

**(c) 类型那条同样要撤回，而且方向反了。**
我原以为 `staff_id BIGINT` 会去撞生产的 INT。TL64 显示
**生产 `ipad_device_binding.staff_id` 本来就是 `int`**，不是 bigint。
也就是说生产表比脚本更贴合"操作人 ID 一律 INT"的口径，脚本文本才是偏的那个。

**(d) 真正值得留意的，换成了另一件事。**
生产这张表和脚本的定义**对不上**，而且不止一处：
索引名是 `device_sn` / `idx_device_sn` / `idx_store_id`，
脚本写的是 `uk_ipad_device_sn` / `idx_ipad_device_store_status` / `idx_ipad_device_staff`；
`device_sn` 生产是 `varchar(100)`，脚本是 `varchar(128)`；生产该表**没有外键**。
结论：**生产这张表不是由这份脚本建出来的**，它另有来历。
这不影响本次发布（脚本空转），但意味着
"跑过这份迁移 = 生产结构等于脚本定义"这个假设是不成立的，
将来谁按脚本定义去写依赖索引名的代码会踩空。这一条我留给统筹判断，不自行处理。

---

## 补正三：把未实测的运行时判断降级（统筹第 2 条意见，我接受）

首次报告里我把几处**没有实际执行过 DDL** 的判断写成了结论，这是过度断言，逐条改口径：

| 首次写法（过度） | 应有写法 |
| --- | --- |
| payroll 那份"**安全**，可反复跑" | 脚本采用 PREPARE/EXECUTE 并按结构正确性逐项比对，**静态看具备幂等设计**；是否真能重复执行**未实测**，待在同版本隔离库实跑验证 |
| stocktake"**每次都重建**表" | `MODIFY COLUMN` 是否触发表重建取决于 MySQL 版本与 ALGORITHM，**本次未实测**；精度变更通常走 COPY 是通例，不是本环境的结论，**锁窗口待实测** |
| 精度变更"**不存在**数据丢失风险" | 仅就类型定义做算术比较：`(10,2)→(12,3)` 整数位 8→9、小数位 2→3；`(10,2)→(16,8)` 整数位 8→8 不变。**按定义看是扩宽、不截断**，属对类型的静态推断，**不等于实跑无损** |
| receipt_payable_source"**不幂等**，二次执行会失败" | 该脚本**未见** `IF NOT EXISTS` 或 PREPARE 守卫（静态可确认）；由此**推断**重复执行会因对象已存在而报错，**未实测** |
| 唯一索引"存在重复即失败" | 这是 MySQL 的定义性行为，可以断言；至于这些表当前有没有重复 —— 见下，TL64 已给出实测 |

一句话口径：本包里凡是**结构层面**的陈述（谁引用谁、有没有守卫、列名与类型写的是什么）
都是读原文得来、可核对；凡是**运行时行为**（锁多久、会不会重建表、重跑会怎样）
一律是待验证项，不作数。

---

## 补正四：TL64 关闭了首次报告里的三项"未验证"

**1. 工资两表的生产现状 —— 已知，且能确定迁移会走哪条分支。**

- `payroll_payout_record` **不存在** → 迁移走**新建表**分支。
- `month_salary` **存在，22 列**，且本次迁移要加的 7 列
  （`post_salary_snapshot`、`attendance_pay_snapshot`、`approved_by`、`approved_at`、
  `paid_by`、`paid_at`、`payout_id`）**一列都没有** → 全部走 **ADD 分支**，
  不进"自愈已有错误结构"那条路，因此**不会触发脚本里的 `DROP INDEX` / `DROP FOREIGN KEY`**。
  这一点把首次报告里"含 DROP，需留意"的担忧也一并消掉了。
- 现有索引：`PRIMARY(salary_id)`、`uk_staff_month(staff_id, salary_month)`、
  `idx_staff_salary`、`idx_store_salary`；**没有** `idx_month_salary_payout` → 迁移新增，无冲突。
- 现有外键两条，即上文 (a) 引用的那两条。迁移新增的
  `fk_salary_payout_month (payout_id, salary_month)` 指向新建的 `payroll_payout_record`，
  两端都是 BIGINT，无类型冲突。
- 附带确认：`month_salary.staff_id` 是 **int**、`store_id` 是 **bigint**，
  与生产"操作人 INT"口径一致；本批迁移不动这两列。

**2. 四处拟建唯一索引的重复数据 —— 采集时实测全为 0（查重前置通过，不等于可安全建立）。**

TL64 末尾四行：`finance_payable 0`、`finance_receivable 0`、`purchase_receipt 0`、`booking_master 0`。
统筹已纠正我这里的措辞，我接受：**重复组为 0 只是"查重前置通过"，不等于"可安全建立"**。
它证明的是采集那一刻没有冲突数据，不覆盖发布窗口内数据变化、
也不覆盖建索引本身的其它失败可能。
所以准确说法是：首次报告里列为最高风险的 `receipt_payable_source_v1` 与 `ipad v2`，
**"因当前重复数据而失败"这一条已排除**，其余风险（不幂等、加索引期间的锁、
发布窗口内新产生的冲突数据）仍在，且仍未实测。

**3. `booking_id` 长度兼容 —— 仍未验证，不动。**
TL64 未采集 `booking_master.booking_id`，TL33 的未验证声明继续有效，我不替它下结论。

---

## 现在还剩什么没有关掉

1. `booking_master.booking_id` 生产长度 50 vs iPad v1 子定义 `VARCHAR(255)`（沿用 TL33 声明）。
2. 一切运行时行为：锁窗口、是否重建表、重复执行的实际结果 —— 一律未实测。
3. `stock_take_detail` 行数：TL35 曾采集为 0，但那是历史值；按统筹口径**发布窗口内再采一次即可**，不另行广搜。
4. 生产 `ipad_device_binding` 与脚本定义不一致（补正二 (d)），来历未知，留给统筹。

不再新增查询请求：统筹已说明 TL64 到此为止、不再跑生产。上述四项如实挂着，不用推断填。

---

## 本件的边界

未执行任何 SQL，未起库或 Java，未改动任何原 SQL（含补收的两份），
未碰法务，未查凭证，未做新的探索或环境测试，未重做 CO55 / CO43 / CO46 / CL50。
首次 `report.md` 与 `plan.json` 原样保留，补充结构抽取在 `plan-supplement.json`。
本件不构成生产发布许可。
