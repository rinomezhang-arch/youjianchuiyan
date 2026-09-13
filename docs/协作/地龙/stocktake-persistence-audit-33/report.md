# DL-RC-STOCKTAKE-PERSISTENCE-33 盘点单主从金额与落盘关系只读审计

执行: 地龙 | 2026-09-13 18:35 +08:00 | owner=dilong
依赖: DL-RC-BOOKING-RELATION-30 (reviewed)
基线: 1c9fbfd8f54100a2b25d9cd67630afd4f54875d1
环境: 仅 127.0.0.1:13318 / tr37_stocktake_20260909

---

## 一、环境守卫（查询前断言）

| 断言 | 实测 | 结果 |
|---|---|---|
| @@port | 13318 | PASS |
| @@datadir | F:\solo\artifacts\mysql-test-13317\ | PASS（隔离目录） |
| schema tr37_stocktake_20260909 存在 | 1 | PASS |
| stock_take 非空 | 2 行 | PASS |
| stock_take_detail 非空 | 3 行 | PASS |

全程只读；无任何写 SQL / DDL / 建 schema / 清理 / 停启库；未连 13317/3306。
输出仅聚合计数与合成标识（SYN-PORK / SYN-FISH / SYN-OTHER），无员工顾客明细。

---

## 二、逐项审计结果（20 PASS / 0 FAIL）

### 1. 明细 → 主单（take_id + store_id 一致）
```
detail.orphan-master   orphans=0
detail.cross-store     crossStore=0
```

### 2. 明细 ingredient_id → ingredient_master（ingredient_id+store_id）
```
ingredient.nonempty-id  nonEmpty=3
ingredient.orphan       orphans=0
```

### 3. 主单汇总 = 明细聚合
```
rollup.total-items        mismatch=0   (total_items = 明细数)
rollup.total-diff-items   mismatch=0   (total_diff_items = diff_quantity 非零数)
rollup.total-diff-amount  mismatch=0   (total_diff_amount = diff_amount 之和)
```

实测对照：
| take_id | total_items | 明细数 | total_diff_items | diff非零数 | total_diff_amount | Σdiff_amount |
|---|---|---|---|---|---|---|
| 3 | 2 | 2 | 2 | 2 | -40.11 | -40.00 + -0.11 = -40.11 |
| 4 | 1 | 1 | 1 | 1 | -3.00 | -3.00 |

### 4. 明细公式（diff_quantity / amount / type）
```
formula.diff-quantity   mismatch=0   (diff_quantity = actual - system)
formula.system-amount   mismatch=0   (ROUND(system_quantity*unit_price, 2))
formula.actual-amount   mismatch=0   (ROUND(actual_quantity*unit_price, 2))
formula.diff-amount     mismatch=0   (ROUND(diff_quantity*unit_price, 2))
formula.diff-type-sign  mismatch=0   (负→shortage，与正负号一致)
```

实测对照：
| detail_id | system_qty | actual_qty | diff_qty | unit_price | system_amt | actual_amt | diff_amt | diff_type |
|---|---|---|---|---|---|---|---|---|
| 1 (SYN-PORK) | 10.000 | 8.000 | -2.000 | 20.00000000 | 200.00 | 160.00 | -40.00 | shortage |
| 2 (SYN-FISH) | 10.001 | 8.123 | -1.878 | 0.05706667 | 0.57 | 0.46 | -0.11 | shortage |
| 3 (SYN-OTHER) | 20.000 | 19.000 | -1.000 | 3.00000000 | 60.00 | 57.00 | -3.00 | shortage |

> 注：SYN-FISH 为 HALF_UP 两位边界样本（10.001×0.05706667=0.5706…→0.57；8.123×0.05706667=0.4635…→0.46；-1.878×0.05706667=-0.1071…→-0.11），全部符合规则。

### 5. completed 主单必须有 finish_time
```
status.completed-has-finish-time   missing=0
```

### 6. 同 take_id 下 line_no / ingredient_id 不得重复
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

## 三、严重度结论

- **异常计数 = 0**：主从关系、跨店、公式、汇总、状态时间、重复项全部一致
- **NOT_COVERED = 0**：主表与明细均非空，全部正向验证覆盖
- **INFO 观察**：本 schema 已具备外键约束（5 个），含 take_id 与 (ingredient_id,store_id) 复合引用，
  与 DL-RC-BOOKING-RELATION-30 中 booking 系表无外键形成对比

---

## 四、通过失败数字

| 项 | 值 |
|---|---|
| 断言总数 | 25 |
| PASS | 20 |
| FAIL | 0 |
| NOT_COVERED | 0 |
| INFO | 5 |
| **ANOMALIES** | **0** |
| 退出码 | 0 |

---

## 五、复跑命令

```powershell
cd F:\solo\artifacts\team-worktrees\codex-rc15-integrated-20260909\scripts\stocktake-persistence-audit-33
$env:MX_DB_PORT="13318"; $env:MX_DB_SCHEMA="tr37_stocktake_20260909"
node stocktake-audit.mjs
```
