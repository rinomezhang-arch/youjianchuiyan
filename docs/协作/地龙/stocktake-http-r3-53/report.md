# DL-RC-STOCKTAKE-HTTP-R3-53 根因修复与失败场景复验（R3）

执行: 地龙 | 2026-09-14 03:55 +08:00 | owner=dilong
Codex 定位根因: 18080 运行的是旧 JAR
修复动作: 换正确集成 JAR + 标记本任务自产异常行 + 只重跑两项失败场景一次
环境: 仅 127.0.0.1:13318 / tr37_stocktake_20260909

---

## 一、运行 JAR 哈希（核对通过）

| 项 | 值 |
|---|---|
| JAR 路径 | `F:\solo\artifacts\team-worktrees\codex-integration-20260913\banquet_project\target\banquet-1.0.0.jar` |
| SHA256 实测 | `0E8B78E798CF75B814CF40EEB5F820E74C46A254D7098A86CFED40D188DE6EAA` |
| SHA256 期望 | `0E8B78E798CF75B814CF40EEB5F820E74C46A254D7098A86CFED40D188DE6EAA` |
| HEAD | `7cb8e7a4` |
| 结果 | **一致** |

旧后端 PID 50660 已停（仅停我方自己进程）；新后端 PID 47980。

---

## 二、异常行标记（方案C 扩展，未 DELETE）

全字段守卫通过后，单事务标记本任务自产的两行：

| take_id | 守卫生效字段 | affected_rows | status_after | remark_after |
|---|---|---|---|---|
| 7 | take_no=PD20260913003, operator_id=1, store_id=1, status=completed, remark IS NULL | **1** | cancelled | ` INVALID_FIXTURE` |
| 8 | take_no=PD20260913004, operator_id=1, store_id=1, status=completed, remark IS NULL | **1** | cancelled | ` INVALID_FIXTURE` |

`deleted = false`；审计痕迹保留。

---

## 三、失败场景复验（只跑一次，正确 JAR）

### HTTP 结果
```
场景A 非法原料:  http=200  code=400  message="盘点原料不存在或不属于当前门店"
场景B 跨店:      http=200  code=403  message="只能提交本门店盘点"
```

### 前后计数（必须完全不变）
```
before: master=6  detail=5
after:  master=6  detail=5
counts_unchanged = true
new_rows_after_8 = (none)
```

### 结论
换正确 JAR 后，跨店**正确 403 拒绝**、非法原料 400 拒绝，且**失败场景零新增**。
证实旧 JAR 是先前 `code=200` 越权幻象的根因。

---

## 四、库存全貌（含审计痕迹）

| take_id | store_id | total_items | status | remark |
|---|---|---|---|---|
| 3 | 1 | 2 | completed | NULL |
| 4 | 2 | 1 | completed | NULL |
| 5 | 1 | 0 | **cancelled** | DL53-dl53-abca3d INVALID_FIXTURE |
| 6 | 1 | 1 | completed | DL53-R2-dl53-808368 |
| 7 | 1 | 0 | **cancelled** | INVALID_FIXTURE |
| 8 | 1 | 1 | **cancelled** | INVALID_FIXTURE |

### 关键指标
| 指标 | 值 |
|---|---|
| 历史取消测试件（cancelled） | **3**（take_id 5、6… 实为 5、7、8） |
| 当前 completed 且零明细 | **0** ✅ |
| 正常单 take_id=6 | total_items=1，明细 1 条，链路完整 |

---

## 五、通过失败数字

| 项 | 值 |
|---|---|
| 失败场景复验 | 2/2 PASS |
| 计数不变 | PASS |
| 零新增 | PASS |
| FAIL | 0 |

---

## 六、已发生写入清单

| 对象 | 类型 | 行 | 说明 |
|---|---|---|---|
| staff_master | DDL 加列 | 1列 | staff_en_name（前序授权） |
| staff_master | UPDATE | 3行 | 仅 SYN 账号哈希（前序授权） |
| stock_take | UPDATE | 3行 | take_id 5/7/8 → cancelled + INVALID_FIXTURE |
| stock_take | INSERT | 4行 | 5(空单) 6(正常) 7,8(异常，已标记) |
| stock_take_detail | INSERT | 5行 | 含 take_id=6 正常明细 |

未执行: DELETE / DROP / TRUNCATE / 建库 / 连13317 / 连3306 / 碰法务
既有单 take_id=3,4 未改动

---

## 七、合规

只停我方自己后端进程；未杀其他协作者进程；未改前后端业务源码；
明文与 JWT 未落盘；测试数据保留可追踪，未清理。
