# DL-RC-STOCKTAKE-PERSISTENCE-33 盘点单主从金额与落盘关系只读审计

执行: 地龙 | 2026-09-13 | owner=dilong
依赖: DL-RC-BOOKING-RELATION-30 (reviewed)
基线: 1c9fbfd8f54100a2b25d9cd67630afd4f54875d1
环境: 仅 127.0.0.1:13318 / tr37_stocktake_20260909
修订: R2（不输出环境变量值 / 查询数真实 / JSON与报告纳入允许路径 / 复跑命令更正）

---

## 一、连接目标锁死（常量硬编码，无任何注入点）

脚本内连接参数为常量：`HOST=127.0.0.1`、`PORT=13318`、`SCHEMA=tr37_stocktake_20260909`。

- 刻意不读取 `MX_DB_HOST` / `MX_DB_PORT` / `MX_DB_SCHEMA` / `MX_MYSQL_CLI` 等任何环境变量
- `guard.no-env-injection` 仅记录**被忽略的变量名**，**不记录其值**（R2 修正：值可能含路径或敏感信息）
- 前 4 条语句仅查 `@@port` / `@@datadir` / schema 存在性 / `DATABASE()`，不触碰任何业务表

> 说明：MySQL 协议下连接目标无法在连接**前**用 SQL 自证，本实现为等价达成
> （常量硬编码 + 零注入面 + 忽略行为可断言），非字面意义的「零查询退出」。

## 二、只读事务

每次查询在同一 MySQL 会话内以
`SET SESSION TRANSACTION READ ONLY` + `START TRANSACTION READ ONLY` + 查询 + `COMMIT` 执行，
通过单次 `mysql -e` 调用保证同连接。本次运行**真实查询数 = 24**。无任何写 SQL。

---

## 三、环境与数据前置

| 项 | 实测 |
|---|---|
| @@port | 13318 |
| @@datadir | F:\solo\artifacts\mysql-test-13317\ |
| schema tr37_stocktake_20260909 | 存在 |
| stock_take | 2 行（非空） |
| stock_take_detail | 3 行（非空） |

---

## 四、逐项审计结果（22 PASS / 0 FAIL）

### 1. 明细 → 主单（take_id + store_id 一致）
```
detail.orphan-master   orphans=0
detail.cross-store     crossStore=0
```

### 2. ingredient_id → ingredient_master（ingredient_id+store_id）
```
ingredient.nonempty-id  nonEmpty=3 (>0)
ingredient.orphan       orphans=0
```

### 3. 主单汇总 = 明细聚合
```
rollup.total-items        mismatch=0
rollup.total-diff-items   mismatch=0
rollup.total-diff-amount  mismatch=0
```

实测对照：
| take_id | total_items | 明细数 | total_diff_items | diff非零数 | total_diff_amount | Σdiff_amount |
|---|---|---|---|---|---|---|
| 3 | 2 | 2 | 2 | 2 | -40.11 | -40.00 + -0.11 = -40.11 |
| 4 | 1 | 1 | 1 | 1 | -3.00 | -3.00 |

### 4. 明细公式
```
formula.diff-quantity   mismatch=0
formula.system-amount   mismatch=0
formula.actual-amount   mismatch=0
formula.diff-amount     mismatch=0
formula.diff-type-sign  mismatch=0
```

实测对照（SYN-FISH 为 HALF_UP 两位边界样本）：
| detail_id | system_qty | actual_qty | diff_qty | unit_price | system_amt | actual_amt | diff_amt | type |
|---|---|---|---|---|---|---|---|---|
| 1 (SYN-PORK) | 10.000 | 8.000 | -2.000 | 20.00000000 | 200.00 | 160.00 | -40.00 | shortage |
| 2 (SYN-FISH) | 10.001 | 8.123 | -1.878 | 0.05706667 | 0.57 | 0.46 | -0.11 | shortage |
| 3 (SYN-OTHER) | 20.000 | 19.000 | -1.000 | 3.00000000 | 60.00 | 57.00 | -3.00 | shortage |

> SYN-FISH：10.001×0.05706667=0.5706→0.57；8.123×0.05706667=0.4635→0.46；-1.878×0.05706667=-0.1071→-0.11，全部符合。

### 5. completed 必须有 finish_time
```
status.completed-has-finish-time   missing=0
```

### 6. 重复项
```
dup.line-no         dupPairs=0
dup.ingredient-id   dupPairs=0
```

### 7. 约束元数据（INFO）
```
外键数量: 5
  stock_take.fk_stock_take_operator_id:operator_id->staff_master.staff_id
  stock_take.fk_stock_take_supervisor_id:supervisor_id->staff_master.staff_id
  stock_take_detail.fk_stock_take_detail_ingredient_id:ingredient_id->ingredient_master.ingredient_id
  stock_take_detail.fk_stock_take_detail_ingredient_id:store_id->ingredient_master.store_id
  stock_take_detail.fk_stock_take_detail_take_id:take_id->stock_take.take_id
唯一索引数量: 4
  ingredient_master.PRIMARY(ingredient_id,store_id)
  stock_take.idx_take_no(take_no)
  stock_take.PRIMARY(take_id)
  stock_take_detail.PRIMARY(detail_id)
```

---

## 五、通过失败数字

| 项 | 值 |
|---|---|
| 断言总数 | 27 |
| PASS | 22 |
| FAIL | 0 |
| NOT_COVERED | 0 |
| INFO | 5 |
| ANOMALIES | 0 |
| 真实查询数 | 24 |
| 退出码 | 0 |

---

## 六、复跑命令（真实入口）

脚本连接参数为常量，**不接受任何环境变量指定连接目标**。唯一可选参数是输出路径：

```powershell
cd F:\solo\artifacts\team-worktrees\codex-rc15-integrated-20260909\scripts\stocktake-persistence-audit-33
$env:MX_OUT = "F:\solo\artifacts\team-worktrees\codex-rc15-integrated-20260909\docs\协作\地龙\stocktake-persistence-audit-33\audit-results-r2.json"
node stocktake-audit.mjs
```

> 注：`MX_DB_PORT` / `MX_DB_SCHEMA` 等连接类变量**已被脚本刻意忽略**，设置它们不会改变连接目标，不应作为连接保护手段使用。

---

## 七、机器可读原始结果

`docs/协作/地龙/stocktake-persistence-audit-33/audit-results-r2.json`
（随本次提交交付，含 `queries_executed=24`、`ignored_injection_keys`、逐项 results）

## 八、合规与零写入

只读；无任何写 SQL / DDL / 建 schema / 清理 / 停启库；未连接 13317 / 3306；
未修改或清理现有隔离数据；无员工顾客明细；无凭证明文。
