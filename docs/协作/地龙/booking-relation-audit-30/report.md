# DL-RC-BOOKING-RELATION-30 只读关系审计报告

执行: 地龙 | 2026-09-13 18:14 +08:00 | owner=dilong
依赖: DL-AUTH-E2E-MATRIX-15 (reviewed)
基线: 5748aa416f2540009f5b7e68c8d08295218d6ff8

---

## 一、环境守卫（查询前断言）

| 断言 | 实测 | 结果 |
|---|---|---|
| @@port | 13318 | PASS |
| @@datadir | F:\solo\artifacts\mysql-test-13317\ | PASS（隔离目录） |
| schema banquet_rc15 存在 | 1 | PASS |

- 全程只读事务（SET TRANSACTION READ ONLY 语义）
- 未连接 13317 / 3306；未执行任何 INSERT/UPDATE/DELETE/DROP/TRUNCATE/DDL
- 未停启数据库；未改前后端/迁移/配置/部署/法务
- 输出仅聚合计数、表列名、约束名；无顾客/员工逐行明细，无凭证明文

---

## 二、逐项审计结果（28 PASS / 0 FAIL）

### 1. booking_dish_detail → booking_master（booking_id+store_id）
```
dishdetail.total                  rows=5
dishdetail.orphan-parent-count    orphans=0        (缺父单)
dishdetail.cross-store-count      crossStore=0
dishdetail.null-booking-id        nullBookingId=0
```

### 2. booking_table → booking_master
```
bookingtable.total                    rows=0
bookingtable.orphan-by-master-id      orphans=0
bookingtable.orphan-by-pair           orphans=0
bookingtable.pair-consistency         inconsistent=0   (master_id 命中但 pair 不一致)
```

### 3. booking_dish_detail.dish_id → dish_master（区分自定义菜）
```
dishdetail.dish-id-nonempty        nonEmptyDishId=5
dishdetail.dish-orphan-nonempty    orphans=0            (非空 dish_id 未命中主档)
dishdetail.custom-name-allowed     customNamedAllowed=0 (允许的自定义菜，非孤儿)
dishdetail.empty-id-no-customname  emptyIdNoCustomName=0
```

### 4. finance_payment_record.booking_id → booking_master
```
payment.total                rows=0
payment.nonempty-booking-id  nonEmptyBookingId=0
payment.orphan-booking       orphans=0
```

### 5. 金额一致性
```
amount.dish-subtotal-mismatch        rows=0     (qty*price != subtotal)
amount.dish-sum                      sumSubtotal=75.00
amount.masters                       bookingMasterRows=1
amount.total-vs-dishsum-mismatch     masters=0  (total_amount != 菜品合计)
amount.final-lt-total                finalLtTotal=0  (不擅自判错：可能含折扣/定金)
amount.deposit-present               mastersWithDeposit=0
```

### 6. booking_id 重复 / 跨店复用
```
dup.booking-id-dup-in-store        dupPairs=0
dup.booking-id-cross-store-reuse   crossStoreReused=0
```

### 7. 约束元数据（仅名称，无数据行）
```
外键数量: 0
唯一索引 (7):
  booking_dish_detail.PRIMARY(dish_booking_id)
  booking_master.PRIMARY(id)
  booking_master.uk_booking_master_id_store_booking(id,store_id,booking_id)
  booking_master.UK_os7xouent53pbspgm7b96ww0m(booking_id)
  booking_table.PRIMARY(table_booking_id)
  dish_master.PRIMARY(dish_id,store_id)
  finance_payment_record.PRIMARY(payment_id)
```

---

## 三、严重度结论

- **零孤儿、零跨店、零金额不一致、零重复 booking_id**（在当前隔离库数据量下）
- **结构性观察（非缺陷，供参考）**：
  1. 五张表**无外键约束**（fkCount=0）——关系完整性完全依赖应用层保证
  2. `booking_master` 存在**两个唯一索引**覆盖 booking_id：
     - `UK_os7xouent53pbspgm7b96ww0m(booking_id)` —— 全局唯一（**跨店也不允许重复**）
     - `uk_booking_master_id_store_booking(id,store_id,booking_id)` —— 覆盖 (id,store,booking) 组合
     - 这意味着当前 schema **禁止** booking_id 跨店复用；若业务上需要跨店同号，此为约束级限制
  3. `booking_table` 与 `finance_payment_record` 在隔离库中为空表（rows=0），
     其关系断言为"空集通过"，**不构成正向验证**——需真实数据量下复验

---

## 四、通过失败数字

| 项 | 值 |
|---|---|
| 断言总数 | 30（28 断言 + 2 INFO） |
| PASS | 28 |
| FAIL | 0 |
| 异常计数 | 全零 |

---

## 五、复跑命令

```powershell
cd F:\solo\artifacts\team-worktrees\codex-rc15-integrated-20260909\scripts\booking-relation-audit-30
$env:MX_DB_PORT="13318"; $env:MX_DB_SCHEMA="banquet_rc15"
node relation-audit.mjs
```
