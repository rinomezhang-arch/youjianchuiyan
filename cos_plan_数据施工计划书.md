# 又见炊烟餐饮管理系统 — 数据全链路虚拟数据施工计划书

> 生成：2026-07-23 | 数据库：banquet | 85表 | store_id=1(宁国) + store_id=2(宣城)

## 铁律
1. ❌ 不动表结构、键、约束、关系
2. ❌ 不删现有数据（宁国店数据寸土不伤）
3. ✅ 菜品名、套餐名用真实名称，其余字段虚拟但逼真
4. ✅ 所有字段全部填满（非 nullable 必须填，nullable 也尽量填）
5. ✅ 无孤儿数据，所有外键有对应记录
6. ✅ 成本链路可溯源：供应商进价 → 采购入库均价 → 食材出成率 → 配方用料 → 菜品成本 → 套餐成本 → 售价 → 毛利

## 数据完整链路溯源图

```
供应商(supplier_master)             部门(department)
  │                                     │
  ├→ 食材主档(ingredient_master)        ├→ 员工(staff_master)
  │   ├ avg_price / yield_rate          │   ├→ 考勤/加班/请假/排班
  │   ├ current_stock                   │   └→ 员工生命周期
  │   └→ 菜品配方(dish_recipe)          │
  │       ├ ingredient_id → qty/unit    │
  │       ├ unit_price(进价)            │
  │       ├ wastage_rate(损耗%)         │
  │       ├ yield_rate(出成率%)         │
  │       ├ net_unit_price(净价)        │
  │       └ total_cost(成本)            │
  │         │                             │
  │         └→ 菜品(dish_master)         │
  │              ├ sale_price(售价)      │
  │              ├ cost_price(成本=Σ配方) │
  │              └→ 套餐明细(package_dish_detail)
  │                   └→ 套餐(package_master)
  │                        ├ total_price(总价)
  │                        ├ cost_price(成本)
  │                        └ cost_rate(成本率)
  │
  ├→ 采购订单(purchase_order→purchase_order_detail)
  │   └→ 采购入库(purchase_receipt→purchase_receipt_detail)
  │       ├ 更新 ingredient_master.current_stock
  │       ├ 更新 ingredient_master.avg_price
  │       └→ 库存流水(ingredient_inventory_log)
  │           ├ log_type=inbound/purchase
  │           └ stock_after
  │
  ├→ 采购退货(purchase_return→purchase_return_detail)
  │   └→ ingredient_inventory_log(log_type=return)
  │
  ├→ 盘点(stock_take→stock_take_detail)
  │   └→ ingredient_inventory_log(log_type=check)
  │
  ├→ 报损(stock_loss→stock_loss_detail)
  │   └→ ingredient_inventory_log(log_type=loss)
  │
  └→ 调拨(stock_transfer)
      └→ ingredient_inventory_log(log_type=transfer)

客户(customer_master)
  └→ 预订(booking_master)
       ├→ 订桌(booking_table)
       ├→ 订菜(booking_dish_detail)
       │    └→ 厨房操作(kitchen_log)
       │
       └→ 财务应收(finance_receivable)
            └→ 收款(finance_payment_record)
                 └→ 账户流水(finance_transaction)
                      └→ 对账(finance_reconciliation)

财务账户(finance_account)
  ├→ 收支流水(finance_transaction)
  ├→ 应付(finance_payable ← supplier)
  ├→ 费用报销(finance_expense)
  ├→ 成本记录(finance_cost_record)
  ├→ 会计凭证(finance_voucher→voucher_detail)
  ├→ 结算(finance_settlement)
  └→ 对账(finance_reconciliation)

会员等级(member_level)
  └→ 会员卡(member_card)
       ├→ 储值充值(member_recharge_record)
       ├→ 消费记录(member_consume_record)
       ├→ 积分变动(member_point_log)
       └→ 积分规则(member_point_rule)

营销活动(marketing_activity) → 优惠券(marketing_coupon→coupon_record)
                          → 折扣规则(marketing_discount_rule)
                          → 会员奖励(marketing_member_reward)
                          → 优惠码(marketing_promo_code)
                          → 抽奖(marketing_lottery)

报表(report_daily/monthly/dish_sales/department_cost/staff_kpi)

系统(sys_dict/dict_item, admin_users, store_info, config, audit_logs,
     sys_notification, sys_operation_log, change_log, ai_history/memory)
```

## 施工阶段（必须按此顺序，阶段N依赖阶段N-1的外键）

### 阶段0：基础设施（2张空表）
- store_info: 2条（宁国店+宣城店完整信息）
- config: 5条（系统配置参数）

### 阶段1：人事链路（1新+6增）
- department: 已有30条，补齐宁国+宣城的合理部门
- staff_master: 已有24员工→→ 补至30+（含完整薪资、银行信息）
- attendance: 已有222条→→ 追加宣城店考勤
- attendance_records: 已有18条→→ 追加
- overtime: 已有50条→→ 追加
- leave_record: 已有31条→→ 追加
- schedule: 已有281条→→ 追加
- employee_lifecycle: 已有11条→→ 追加

### 阶段2：供应链—供应商与食材（1新+2增+4空）
- supplier_master: 已有6条→→ 补至10条（宣城追加）
- ingredient_master: 已有1215条→→ **关键**：补齐 yield_rate、avg_price、current_stock
- ingredient_purchase: 已有53条→→ 追加历史采购记录
- **ingredient_inventory_log: 0条→ 填满**（入库/出库/盘点/报损流水）
- **purchase_order + detail: 0条→ 填满**（最近2个月采购单）
- **purchase_receipt + detail: 0条→ 填满**（对应入库单）
- **purchase_return + detail: 0条→ 填满**（少量退货）

### 阶段3：菜品配方/成本卡（核心链路，0条→填满）
- **dish_recipe: 0条→ 填满** ⚠️ 最核心表
  - 每个菜品 3~12 个食材配方行
  - 含进价、用量、损耗率、出成率、净价、总成本
  - 总成本汇总后反向写入 dish_master.cost_price

### 阶段4：菜品周边（1新+增补）
- dish_occasion_names: 0条→ 填满（婚宴/寿宴/乔迁别名）
- dish_category/dish_tag/dish_tag_relation/dish_usage/dish_usage_relation: 增量补

### 阶段5：套餐与宴会模板（0条→填满）
- **banquet_type: 已有8条→ 检查补全**
- **banquet_template: 已有5条→ 补全**
- **banquet_template_rel: 0条→ 填满**
- **menu_category: 已有9条→ 补全**
- **template_category_rel: 已有9条→ 检查**
- **template_dish_rel: 已有756条→ 检查补**

### 阶段6：预订链路（3空+1新+1增）
- customer_master: 已有37条→ 补至50+
- **booking_master: 0条→ 填满**（最近2个月预订）
- **booking_table: 0条→ 填满**（每单1~3桌）
- **booking_dish_detail: 0条→ 填满**（每桌8~16道菜）
- kitchen_log: 已有5条→ 追加

### 阶段7：财务链路（11空表全部填满）
- **finance_account: 4~6个账户**
- **finance_transaction: 流水（采购支出+营收+工资+费用）**
- **finance_receivable: 应收（客户挂账）**
- **finance_payable: 应付（供应商）**
- **finance_payment_record: 收款记录**
- **finance_expense: 费用报销**
- **finance_voucher + detail: 会计凭证**
- **finance_cost_record: 成本记录**
- **finance_settlement: 日结/月结**
- **finance_reconciliation: 对账**

### 阶段8：会员链路（6空表全部填满）
- **member_level: 4~5个等级**
- **member_card: 30~50个会员**
- **member_recharge_record: 充值记录**
- **member_consume_record: 消费记录**
- **member_point_log: 积分流水**
- **member_point_rule: 积分规则**

### 阶段9：营销链路（7空表全部填满）
- **marketing_coupon + record**
- **marketing_activity**
- **marketing_discount_rule**
- **marketing_member_reward**
- **marketing_promo_code**
- **marketing_lottery**

### 阶段10：报表链路（5空表全部填满）
- **report_daily**
- **report_monthly**
- **report_dish_sales**
- **report_department_cost**
- **report_staff_kpi**

### 阶段11：系统辅助（6空表+2空表）
- store_info（阶段0已做）
- admin_users: 2~3个管理员
- audit_logs / sys_operation_log / change_log
- sys_notification
- ai_memory
- config（阶段0已做）

## 预估数据量

| 阶段 | 新增行数 |
|------|---------|
| 0. 基础设施 | 7 |
| 1. 人事 | ~600 |
| 2. 供应链 | ~2000 |
| 3. 配方(核心) | ~800 |
| 4. 菜品周边 | ~300 |
| 5. 套餐模板 | ~50 |
| 6. 预订 | ~300 |
| 7. 财务 | ~500 |
| 8. 会员 | ~200 |
| 9. 营销 | ~100 |
| 10. 报表 | ~100 |
| 11. 系统 | ~100 |
| **总计** | **~5057 行** |

## 数据虚拟规则
- 日期：2026年5月~7月（最近3个月）
- 金额：真实餐饮价格区间（食材8~200元、菜品28~598元、套餐688~1888元）
- 人员：从 staff_master 已有员工中随机分配
- 食材价格：参考真实市场批发价
- 出成率：蔬菜85~95%、肉类70~85%、海鲜80~90%
- 损耗率：蔬菜5~10%、肉类3~8%、海鲜5~12%
- 成本率：菜品25~40%、套餐28~35%
- 毛利率：菜品60~75%、套餐65~72%
