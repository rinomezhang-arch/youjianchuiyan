# 又见炊烟餐饮管理系统 - 死角审计报告 V8

> **审计日期**: 2026-08-01
> **审计范围**: 多角度深度死角扫描（排序规则/重复表/主键类型/空表/枚举值/实体缺失/Repository冲突）
> **审计人**: 秋哥（AI 辅助）

---

## 一、排序规则不一致（已修复）

### 1.1 修复前
| 排序规则 | 表数量 |
|---------|--------|
| utf8mb4_0900_ai_ci | 46 |
| utf8mb4_unicode_ci | 73 |

**后果**：跨表 UNION/JOIN 时报 `Illegal mix of collations` 错误（实测确认）

### 1.2 修复后
| 排序规则 | 表数量 |
|---------|--------|
| utf8mb4_0900_ai_ci | 119 ✅ |

**修复方式**：`ALTER TABLE ... CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`
**附加修复**：employee_lifecycle.event_type ENUM→varchar（ENUM转换冲突）

### 1.3 验证
之前报错的跨表 UNION 查询现在正常执行 ✅

---

## 二、重复表/命名不一致（待处理）

### 2.1 发现的重复表组

| 重复组 | 表A | 表B | 说明 |
|--------|-----|-----|------|
| 菜品 | dishes(113条,id varchar64) | dish_master(600条,dish_id varchar20) | 两套菜品表，dishes疑似遗留 |
| 套餐 | packages(4条,code varchar64) | package_master(9条,package_id varchar20) | 两套套餐表 |
| 考勤 | attendance(226条) | attendance_records(18条) | 两套考勤表 |
| 采购 | purchase_order(4条) | requisition_order(4条) | 4套采购表（含material_requisition/purchase_request） |
| 收货 | purchase_receipt(4条) | goods_receipt(0条) | 两套收货表 |

### 2.2 建议
- `dishes` 表：Java 代码不访问，疑似遗留，建议确认后删除
- `orders` 表：Java 代码不访问，疑似遗留（预订走 booking_master），建议确认后删除
- 其他重复组：需业务确认哪个是主表，废弃冗余表

---

## 三、主键/外键类型不一致（已修复）

### 3.1 修复前

| 字段 | 类型不一致 |
|------|-----------|
| staff_id | int(staff_master) vs bigint(ai_chat_history/sys_user_role) vs varchar50(attendance_records/employee_lifecycle) |
| booking_id | varchar20(booking_master等) vs int(marketing_coupon_record/member_consume_record) |
| store_id | bigint(所有表) vs int(change_log) |

### 3.2 修复后

| 字段 | 统一类型 | 验证 |
|------|---------|------|
| staff_id | int | attendance_records ✅ employee_lifecycle ✅ month_salary ✅ |
| booking_id | varchar(20) | marketing_coupon_record ✅ member_consume_record ✅ |
| store_id | bigint | change_log ✅ |

**注意**：ai_chat_history.staff_id 和 sys_user_role.staff_id 仍为 bigint（JPA Long↔int 可兼容，暂不修改）

---

## 四、实体类/Repository 缺失（待处理）

### 4.1 无实体无Repository的表（7张，仅JdbcTemplate访问）

| 表名 | 访问方式 | 风险 |
|------|---------|------|
| finance_payment_record | FinanceController JdbcTemplate | 收款记录无法JPA查询 |
| finance_transaction | FinanceController JdbcTemplate | 资金交易无法JPA查询 |
| finance_voucher_detail | FinanceController JdbcTemplate | 凭证明细无法JPA查询 |
| finance_cost_record | FinanceController JdbcTemplate | 成本记录无法JPA查询 |
| finance_reconciliation | FinanceController JdbcTemplate | 对账记录无法JPA查询 |
| finance_settlement | FinanceController JdbcTemplate | 结算记录无法JPA查询 |
| attendance_records | AttendanceRecordService JdbcTemplate | 考勤记录无法JPA查询 |

### 4.2 有实体但无Repository的表（6张）

| 实体类 | 表名 | 说明 |
|--------|------|------|
| FinanceReceivable | finance_receivable | **应收主表无Repository，异常** |
| CostCardDetail | cost_card_detail | 成本卡明细 |
| DishCategory | dish_category | 菜品分类 |
| GoodsReceiptItem | goods_receipt_item | 收货明细 |
| MaterialRequisitionItem | material_requisition_item | 领料明细 |
| PurchaseRequestItem | purchase_request_item | 采购明细 |

### 4.3 JPA 双实体映射冲突（严重）

| 表名 | 实体A | 实体B | 问题 |
|------|-------|-------|------|
| table_master | BanquetTable (@Table="table_master") | TableMaster (@Table="table_master") | 两个JPA实体映射同一张表，Hibernate元数据冲突 |

**风险**：缓存不一致、更新互相覆盖、脏检查异常

---

## 五、空表分析

### 5.1 真正空表（业务功能未启用）

| 模块 | 空表 | 说明 |
|------|------|------|
| 营销 | marketing_activity/coupon/coupon_record/discount_rule/lottery/member_reward/promo_code | 营销模块未启用 |
| 会员 | member_card/consume_record/level/point_log/point_rule/recharge_record | 会员模块未启用 |
| 报表 | report_daily/monthly/dish_sales/department_cost/staff_kpi | 报表模块未启用 |
| 库存 | stock_loss/stock_take/stock_transfer + 明细 | 库存管理未启用 |
| 系统 | sys_dict/dict_item/notification/operation_log | 系统配置未初始化 |
| 其他 | contract/post/salary_template/unit_conversion/yield_rate_config | 辅助功能未启用 |

### 5.2 已灌入数据的表

| 表 | 记录数 | 来源 |
|----|--------|------|
| store_info | 2 | 种子数据（宁国总店/宣城分店） |
| customer_master | 17 | 种子数据（15客户+2预订客户） |
| finance_account | 4 | finance_seed.sql |
| finance_receivable | 16 | finance_seed.sql + E2E测试 |
| finance_payment_record | 961 | finance_seed.sql |
| finance_transaction | 961 | finance_seed.sql |
| finance_voucher | 19 | finance_seed.sql + E2E测试 |
| finance_voucher_detail | 68 | finance_seed.sql + E2E测试 |
| finance_reconciliation | 7 | finance_seed.sql + E2E测试 |

---

## 六、枚举值/状态值一致性

### 6.1 修复后跨表状态查询（UNION成功）

| 表名 | 字段 | 值分布 |
|------|------|--------|
| booking_master | status | pending(64), confirmed(3) |
| finance_receivable | status | paid(1), partial(11), unpaid(4) |
| finance_voucher | status | approved(1), posted(18) |
| finance_reconciliation | status | matched(1), pending(6) |
| staff_master | employment_status | active(29), inactive(2) |

**结论**：状态值命名合理，跨表 UNION 查询正常 ✅

---

## 七、修复文件清单

| 文件 | 类型 | 修复内容 |
|------|------|---------|
| fix_collation_v1.sql | 新增 | 73表排序规则统一 + staff_id/booking_id/store_id类型统一 + ENUM修复 |

### 修复统计
- 排序规则修复：73 表 ✅
- 字段类型修复：5 列 ✅
- ENUM 修复：1 列 ✅
- UNION 验证：通过 ✅
- Maven 编译：通过 ✅

---

## 八、待处理事项优先级

### P0 致命（建议立即处理）
1. **table_master 双实体映射冲突**：BanquetTable vs TableMaster 映射同一表，需合并

### P1 严重（建议近期处理）
2. **FinanceReceivable 无 Repository**：应收主表无法JPA查询，需创建 FinanceReceivableRepository
3. **FinanceReceivable.bookingId 类型不匹配**：实体 Long vs DB varchar(20)
4. **7张财务/考勤表无实体类**：仅JdbcTemplate访问，建议创建实体+Repository

### P2 警告（建议后续处理）
5. **重复表清理**：dishes/orders 疑似遗留表，需确认后删除
6. **字段注释补充**：120+表大量缺注释
7. **StaffMaster 类型不一致**：maritalStatus/employmentType Integer vs varchar
8. **DishMaster 不存在的列**：dish_intro/tiktok_recommend
9. **store_info.bank_account 未加密**：有@Convert注解但实体未映射

---

## 九、审计总结

| 维度 | 修复前状态 | 修复后状态 |
|------|-----------|-----------|
| 排序规则一致性 | ❌ 73表不一致（UNION报错） | ✅ 119表统一 |
| 主键/外键类型 | ❌ staff_id 3种/booking_id 2种 | ✅ 类型统一 |
| 重复表 | ⚠️ 5组重复 | 待业务确认 |
| 实体类缺失 | ⚠️ 7表无实体 | 待创建 |
| Repository冲突 | ❌ table_master双映射 | 待合并 |
| 枚举值一致性 | ❌ UNION报错 | ✅ 正常查询 |
| 空表 | ⚠️ 大量空表 | 业务模块未启用（正常） |
| 编译验证 | - | ✅ 通过 |

**本次死角审计发现并修复了 2 个系统级问题（排序规则+字段类型），发现了 3 个待处理问题（双实体冲突+实体缺失+重复表）。**
