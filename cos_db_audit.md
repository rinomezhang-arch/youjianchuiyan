# banquet 数据库完整审计报告（双龙联审终版）

**审计时间**: 2026-07-22
**审计人**: 地龙 🐉（核心15表280字段）+ 天龙 🦞（全50表563字段）
**数据库**: MySQL 8.4 · banquet

---

## 一、总览

| 指标 | 地龙扫描 | 天龙扫描 | 合并结果 |
|------|---------|---------|---------|
| 总表数 | 15 | 50 | 50 |
| 总字段数 | 280 | 563 | 563 |
| 核心业务表 | 15 | 17 | 17 |
| 预订记录 | 5条 | — | 5条 |
| 桌台关联 | 3条 | — | 3条 |
| 菜品明细 | 22条 | — | 22条 |  
| 桌台 | 84张 | — | 84张 (available:81 occupied:3) |
| 客户 | 37人 | — | 37人 |
| 员工 | 24人 | — | 24人 (active:22 inactive:2) |
| 菜品 | 612道 | — | 612道 |
| 套餐 | 8个 | — | 8个 |
| 食材 | 1215种 | — | 1215种 |
| 供应商 | 6家 | — | 6家 |

---

## 二、字段名称对齐 ⚠️

**核心15表命名规范良好** — snake_case 全小写下划线。

**天龙发现的命名问题**（booking_dish vs booking_dish_detail 列名差异）：

| 类别 | booking_dish_detail（真实使用） | booking_dish（天龙多建废弃） | 建议 |
|------|------|------|------|
| 菜品数量 | dish_quantity | quantity | 统一用 dish_quantity |
| 菜品价格 | unit_price | price | 统一用 unit_price |
| 菜品备注 | dish_note | remark | 统一用 dish_note |
| 菜品排序 | dish_order | sort_order | 统一用 dish_order |

### 关联键对照（12条FK）

| 关系 | 主表.列 | 从表.列 | 类型匹配 | 状态 |
|------|---------|---------|---------|------|
| FK1 | booking_master.booking_id (varchar 20) | booking_table.booking_id (varchar 20) | 同型 | ✅ |
| FK2 | booking_master.booking_id (varchar 20) | booking_dish_detail.booking_id (varchar 20) | 同型 | ✅ |
| FK3 | booking_table.table_booking_id (bigint) | booking_dish_detail.table_booking_id (bigint) | 同型 | ✅ |
| FK4 | booking_table.table_id (int) | table_master.table_id (int) | 同型 | ✅ |
| FK5 | booking_master.customer_id (int) | customer_master.customer_id (int) | 同型 | ✅ |
| FK6 | booking_master.staff_id (int) | staff_master.staff_id (int) | 同型 | ✅ |
| FK7 | package_master.package_id (varchar 20) | package_dish_detail.package_id (varchar 20) | 同型 | ✅ |
| FK8 | package_dish_detail.dish_id (varchar 20) | dish_master.dish_id (varchar 20) | 同型 | ✅ |
| FK9 | ingredient_inventory_log.ingredient_id (varchar 50) | ingredient_master.ingredient_id (varchar 50) | 同型 | ✅ |
| FK10 | ingredient_purchase.ingredient_id (varchar 50) | ingredient_master.ingredient_id (varchar 50) | 同型 | ✅ |
| FK11 | ingredient_purchase.supplier_id (int) | supplier_master.supplier_id (int) | 同型 | ✅ |
| FK12 | ingredient_master.primary_supplier_id (int) | supplier_master.supplier_id (int) | 同型 | ✅ |

---

## 三、数据类型对齐 🔴

### 天龙发现3处类型不匹配（跨表）

| 列名 | 表 | 实际类型 | 应为 | 风险 |
|------|-----|------|------|------|
| store_id | change_log | int | bigint | 🔴 JOIN失败 |
| table_id | orders | varchar(64) | int | 🔴 JOIN失败 |
| package_id | package_dish_rel | int | varchar(20) | 🔴 类型不兼容 |

### 重复/冗余列（dish_master）

| 列A | 列B | 不一致率 | 建议 |
|-----|-----|---------|------|
| price | sale_price | 65/588 (11%) | 统一用 sale_price |
| category | dish_category | — | 统一用 dish_category |

### 数值类型分布（核心15表）

| 类型 | 出现次数 | Java映射 | 状态 |
|------|---------|---------|------|
| varchar | 120+ | String | ✅ |
| int | 50+ | Integer | ✅ |
| bigint | 20+ | Long | ✅ |
| decimal(2-4位) | 37 | BigDecimal | ✅ |
| tinyint | 20+ | Boolean | ✅ |
| text | 15+ | String | ✅ |
| timestamp/datetime | 25+ | LocalDateTime | ✅ |
| date | 8 | LocalDate | ✅ |
| time | 3 | LocalTime | ✅ |

### 金额精度对照

| 表 | 列 | 类型 | 精度 | 状态 |
|----|-----|------|------|------|
| booking_master | total_amount | decimal(10,2) | 10亿.2位 | ✅ |
| booking_master | final_amount | decimal(10,2) | 10亿.2位 | ✅ |
| booking_master | deposit | decimal(10,2) | 10亿.2位 | ✅ |
| customer_master | total_amount | decimal(12,2) | 千亿.2位 | ✅ |
| dish_master | cost_price | decimal(10,2) | 10亿.2位 | ✅ |
| dish_master | sale_price | decimal(10,2) | 10亿.2位 | ✅ |
| dish_master | cost_rate | decimal(5,2) | 百分比 | ✅ |
| staff_master | monthly_salary | decimal(10,2) | 10亿.2位 | ✅ |
| package_master | package_total_price | decimal(10,2) | 10亿.2位 | ✅ |
| package_master | package_cost_price | decimal(10,2) | 10亿.2位 | ✅ |
| package_master | cost_rate | decimal(5,2) | 百分比 | ✅ |
| booking_dish_detail | unit_price | decimal(10,2) | 10亿.2位 | ✅ |
| booking_dish_detail | subtotal | decimal(10,2) | 10亿.2位 | ✅ |
| ingredient_master | conversion_rate | decimal(10,3) | 千.3位 | ✅ |
| ingredient_master | current_stock | decimal(12,3) | 千亿.3位 | ✅ |
| ingredient_master | warning_threshold | decimal(10,3) | 10亿.3位 | ✅ |
| ingredient_master | avg_price | decimal(10,4) | 10亿.4位 | ✅ |
| ingredient_master | yield_rate | decimal(5,2) | 百分比 | ✅ |
| ingredient_inventory_log | log_quantity | decimal(12,3) | 千亿.3位 | ✅ |
| ingredient_inventory_log | stock_after | decimal(12,3) | 千亿.3位 | ✅ |
| ingredient_purchase | purchase_quantity | decimal(12,3) | 千亿.3位 | ✅ |
| ingredient_purchase | purchase_price | decimal(10,2) | 10亿.2位 | ✅ |
| ingredient_purchase | purchase_total | decimal(12,2) | 千亿.2位 | ✅ |
| ingredient_purchase | usage_quantity | decimal(12,3) | 千亿.3位 | ✅ |
| ingredient_purchase | usage_price | decimal(10,4) | 10亿.4位 | ✅ |
| attendance_records | total_present | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_statutory | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_holiday | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_comp | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_travel | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_overtime | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_leave | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_late | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_early | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | total_absent | decimal(6,1) | 千.1位 | ✅ |
| attendance_records | final_balance | decimal(6,1) | 千.1位 | ✅ |

---

## 四、表关系完整性

### 4.1 预订链路

| 关联 | 孤儿数据 | 状态 |
|------|---------|------|
| booking_table → booking_master | 0 | ✅ |
| booking_master → booking_table | 3条 | ⚠️ BK20260720001/002, BK20260721001 |
| booking_dish_detail → booking_master | 0 | ✅ |
| booking_dish_detail → booking_table | 0 | ✅ |
| booking_table → table_master | 0 | ✅ |

### 4.2 套餐链路

| 关联 | 孤儿数据 | 状态 |
|------|---------|------|
| package_dish_detail → package_master | 0 | ✅ |
| package_dish_detail → dish_master | 0 | ✅ |
| package_dish_rel | 0行 | ⚠️ 空表可能废弃 |

### 4.3 健康检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| PK重复冲突 | ✅ 0条 | 无重复主键 |
| NULL外键 | ✅ 0条 | customer_id/staff_id全非空 |
| 负数金额 | ✅ 0条 | deposit/total_amount/final_amount无负数 |
| 负数库存 | ✅ 0条 | current_stock无负数 |
| booking_time合法性 | ✅ 0条 | 无异常时间 |
| created_at > updated_at | ✅ 0条 | 时间顺序正确 |
| table_status同步 | ✅ 3/0/0 | 有booking且occupied:3, 有booking但available:0, 无booking但occupied:0 |
| booking_status分布 | ✅ | confirmed:4, pending:1 |
| payment_status分布 | ✅ | unpaid:5 |
| table_status分布 | ✅ | available:81(96.4%), occupied:3(3.6%) |
| staff_master employment | ✅ | active:22, inactive:2 |

---

## 五、联动性检查

### ✅ 正常

| 联动 | 结果 |
|------|------|
| 预订→桌台→菜品三表链路 | ✅ 5笔预订3条桌台22条菜品明细 |
| 桌台table_status↔预订booking_status同步 | ✅ 3 occupancy全对应 |
| 采购→食材记录有效性 | ✅ 53条采购全匹配 |
| 库存日志记录 | ✅ 5条日志正常 |
| dish_tag_relation → dish_master | ✅ 0行 |
| dish_usage_relation → dish_master | ✅ 357行 |

### ⚠️ 数据质量问题

| 问题 | 详情 |
|------|------|
| 3条预订无桌台关联 | BK20260720001/002(含6+4条菜品) + BK20260721001(含4条菜品) 无桌台数据，早期测试残留 |
| 客户名称不同步 | booking_master.customer_name vs customer_master.customer_name 4条不一致(李四→张三,王五→张三,赵六→张三测试,钱七→公司东方红东方红) |
| booking_count过期 | 客户表booking_count字段与实际联查数不匹配(如王五booking_count=20但实际0笔) |
| 套餐dish_count不匹配 | PKG001 dish_count=12实际38条, PKG002 dish_count=14实际30条 |
| 考勤员工分离 | 18条attendance_records.staff_id与staff_master.staff_id无重合，独立数据集 |

### 🔴 P0必须修复

| # | 项目 | 说明 |
|---|------|------|
| 1 | DROP booking_dish | 天龙多建空表(0行9字段)，列名与booking_dish_detail不一致 |
| 2 | change_log.store_id int→bigint | 与其他所有表store_id(bigint)对齐 |
| 3 | orders.table_id varchar(64)→int | 与table_master.table_id(int)对齐 |
| 4 | package_dish_rel.package_id int→varchar(20) | 与package_master.package_id对齐 |
| 5 | BookingController加table_status同步 | create→occupied, delete→available |
| 6 | swap/copy深拷贝菜品 | 换台/复制时同步booking_dish_detail的table_booking_id和菜品明细 |

### 🟡 P1建议

| # | 项目 |
|---|------|
| 1 | dish_master去重列(统一用sale_price+dish_category) |
| 2 | 清理3条无桌台旧预订或补上桌台关联 |
| 3 | 考勤数据与员工主表统一staff_id |

---

## 六、总体评价

| 维度 | 评分 | 说明 |
|------|------|------|
| 字段命名 | ⭐⭐⭐⭐⭐ | snake_case统一，无驼峰下划线混用 |
| 数据类型 | ⭐⭐⭐⭐ | 15核心表完美，3张辅助表类型需修正 |
| 表关系 | ⭐⭐⭐⭐ | 12条FK未建物理约束(靠应用层)，join可行 |
| 数据一致性 | ⭐⭐⭐ | 旧测试数据残留，booking_dish空表干扰 |
| 联动完整性 | ⭐⭐⭐⭐ | 核心链路通，swap/copy代码待补 |

### 双龙差异

| 项目 | 地龙 🐉 | 天龙 🦞 |
|------|---------|---------|
| 扫描范围 | 核心15业务表 | 全部50表 |
| 发现类型问题 | 0处 | 3处(change_log/orders/package_dish_rel) |
| 发现冗余表 | 未扫描到 | booking_dish(空表，天龙多建的) |
| 发现冗余列 | 未扫描到 | dish_master.price↔sale_price |
| 表关系 | 12条FK全部对 | 一致 |
| table_status同步 | 3 occupancy | 3/0/0 |

### 最终待办

**P0 — 必须立即做(6项):**
1. DROP booking_dish 空表
2. 修正3张表类型(change_log/orders/package_dish_rel)
3. BookingController加table_status自动更新
4. swap/copy同步深拷贝菜品明细

**P1 — 建议做(3项):**
5. dish_master去重列
6. 清理旧测试数据
7. 考勤staff_id统一

🔒 等SOLO最终确认后冻结表结构，不再改DDL。
