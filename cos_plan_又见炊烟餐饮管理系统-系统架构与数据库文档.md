# 又见炊烟餐饮管理系统 - 系统架构与数据库结构文档

> 生成时间：2026-07-25 15:05  
> 数据库：MySQL 8.0 @ 腾讯云轻量服务器  
> 库名：banquet（88张表）

---

## 一、系统架构

### 技术栈
| 层级 | 技术 | 端口 |
|------|------|------|
| 前端 | Vue3 + Vite + Element Plus | 5173 |
| 后端 | Spring Boot 3.2.5 + Java 17 | 8080 |
| 数据库 | MySQL 8.0 | 3306 |
| AI助手 | OpenClaw API | 27860 |
| AI模型 | deepseek-v4-pro / deepseek-chat | - |

### 部署架构
```
┌─────────────────────────────────────────────────┐
│           腾讯云轻量应用服务器 (Ubuntu)            │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │ Vue3前端  │  │Spring Boot│  │   MySQL 8.0  │  │
│  │ :5173    │→ │  :8080   │→ │   :3306      │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│                     │                            │
│              ┌──────┴──────┐                    │
│              │  OpenClaw   │                    │
│              │  :11500     │                    │
│              └─────────────┘                    │
└─────────────────────────────────────────────────┘
```

### 多租户设计
- 所有业务表含 `store_id` 字段实现分店隔离
- store_id=1 → 宁国店（主店，84个桌位）
- store_id=2 → 宣城店（新分店，16个桌位）

---

## 二、数据库表总览（88张表）

### 按业务模块分类

#### 1. 宴会预定模块（6表）
| 表名 | 说明 | 行数 |
|------|------|------|
| booking_master | 宴会预定主表 | 86 |
| booking_table | 预定-桌位关联 | 26 |
| booking_dish | 预定-菜品 | 0 |
| booking_dish_detail | 预定菜品明细 | 513 |
| banquet_type | 宴会类型 | 16 |
| banquet_template | 菜单模板 | 10 |
| banquet_template_rel | 宴会-模板关联 | 9 |
| template_category_rel | 模板-分类关联 | 24 |
| template_dish_rel | 模板-菜品关联 | 831 |

#### 2. 菜品管理模块（10表）
| 表名 | 说明 | 行数 |
|------|------|------|
| dish_master | 菜品主表 | 731 |
| dish_category | 厨房分类表 | 37 |
| dish_recipe | 菜品配方（食材用量） | 1312 |
| dish_tag | 菜牌标记类别 | 33 |
| dish_tag_relation | 菜品标记关联 | 1012 |
| dish_usage | 菜品用途表 | 4 |
| dish_usage_relation | 菜品用途关联 | 389 |
| dish_occasion_names | 菜品场景名称 | 113 |
| menu_category | 零点分类（菜单排版） | 18 |
| package_master | 套餐主表 | 18 |
| package_dish_detail | 套餐菜品明细 | 49 |

#### 3. 客户/会员模块（7表）
| 表名 | 说明 | 行数 |
|------|------|------|
| customer_master | 客户主表 | 153 |
| member_card | 会员卡主档 | 40 |
| member_level | 会员等级 | 4 |
| member_point_log | 积分变动日志 | 80 |
| member_point_rule | 积分规则 | 5 |
| member_consume_record | 会员消费记录 | 40 |
| member_recharge_record | 储值充值记录 | 60 |

#### 4. 食材/库存模块（10表）
| 表名 | 说明 | 行数 |
|------|------|------|
| ingredient_master | 食材主表 | 1395 |
| ingredient_inventory_log | 食材库存流水 | 287 |
| ingredient_purchase | 食材采购记录 | 416 |
| supplier_master | 供应商主表 | 43 |
| purchase_order | 采购订单主档 | 15 |
| purchase_order_detail | 采购订单明细 | 42 |
| purchase_receipt | 采购入库单主档 | 10 |
| purchase_receipt_detail | 采购入库明细 | 35 |
| purchase_return | 采购退货单主档 | 3 |
| purchase_return_detail | 采购退货明细 | 5 |
| stock_take | 盘点单主档 | 3 |
| stock_take_detail | 盘点明细 | 9 |
| stock_loss | 报损单主档 | 5 |
| stock_loss_detail | 报损明细 | 8 |
| stock_transfer | 库存调拨单 | 3 |

#### 5. 财务管理模块（13表）
| 表名 | 说明 | 行数 |
|------|------|------|
| finance_account | 财务账户 | 4 |
| finance_transaction | 收支流水 | 200 |
| finance_voucher | 会计凭证 | 15 |
| finance_voucher_detail | 会计凭证明细 | 41 |
| finance_receivable | 应收账款 | 15 |
| finance_payable | 应付账款 | 10 |
| finance_expense | 费用报销 | 10 |
| finance_payment_record | 收款记录 | 20 |
| finance_cost_record | 成本记录 | 30 |
| finance_settlement | 结算记录 | 4 |
| finance_reconciliation | 对账记录 | 3 |

#### 6. 人事/考勤模块（10表）
| 表名 | 说明 | 行数 |
|------|------|------|
| staff_master | 员工主表 | 96 |
| department | 部门 | 45 |
| employee_lifecycle | 员工生命周期 | 51 |
| schedule | 排班 | 608 |
| attendance | 考勤 | 412 |
| attendance_records | 考勤记录 | 43 |
| overtime | 加班 | 93 |
| leave_record | 请假记录 | 58 |

#### 7. 营销模块（7表）
| 表名 | 说明 | 行数 |
|------|------|------|
| marketing_activity | 营销活动 | 5 |
| marketing_coupon | 优惠券 | 5 |
| marketing_coupon_record | 优惠券领取使用记录 | 20 |
| marketing_discount_rule | 优惠规则 | 5 |
| marketing_lottery | 抽奖活动 | 3 |
| marketing_member_reward | 会员奖励规则 | 3 |
| marketing_promo_code | 优惠码 | 5 |

#### 8. 报表模块（5表）
| 表名 | 说明 | 行数 |
|------|------|------|
| report_daily | 日报表 | 31 |
| report_monthly | 月报表 | 3 |
| report_dish_sales | 菜品销售统计 | 20 |
| report_department_cost | 部门成本统计 | 5 |
| report_staff_kpi | 员工KPI统计 | 10 |

#### 9. 系统管理模块（7表）
| 表名 | 说明 | 行数 |
|------|------|------|
| admin_users | 管理员用户 | 4 |
| config | 系统配置 | 12 |
| store_info | 门店信息 | 2 |
| table_master | 桌位主表 | 71 |
| sys_dict | 数据字典 | 51 |
| sys_dict_item | 数据字典项 | 377 |
| sys_notification | 系统通知 | 10 |
| sys_operation_log | 操作日志 | 20 |
| audit_logs | 审计日志 | 10 |
| change_log | 系统改动日志 | 20 |
| kitchen_log | 厨房日志 | 25 |

#### 10. AI模块（2表）
| 表名 | 说明 | 行数 |
|------|------|------|
| ai_chat_history | AI对话历史 | 24 |
| ai_memory | AI记忆 | 5 |

---

## 三、表关系（外键）

### 核心关系图

```
                    ┌──────────────┐
                    │customer_master│
                    └──────┬───────┘
                           │ customer_id
                           ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ staff_master │◄───│booking_master│───►│ table_master │
└──────┬───────┘    └──────┬───────┘    └──────────────┘
       │ staff_id           │ booking_id
       │                    ▼
       │             ┌──────────────┐
       │             │booking_table │
       │             └──────────────┘
       │                    │
       │             ┌──────────────┐
       └────────────►│booking_dish_ │
                     │   detail     │
                     └──────┬───────┘
                            │ dish_id
                            ▼
                     ┌──────────────┐
                     │ dish_master  │◄──── dish_tag_relation ────► dish_tag
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       ┌────────────┐ ┌──────────┐ ┌──────────────┐
       │ dish_recipe│ │dish_usage│ │dish_tag_     │
       │            │ │_relation │ │relation      │
       └─────┬──────┘ └──────────┘ └──────────────┘
             │ ingredient_id
             ▼
       ┌──────────────┐
       │ingredient_   │◄──── ingredient_inventory_log
       │  master      │◄──── ingredient_purchase ──► supplier_master
       └──────────────┘
```

### 完整外键关系表

| 子表 | 外键字段 | 父表 | 关联字段 |
|------|----------|------|----------|
| booking_master | customer_id | customer_master | customer_id |
| booking_master | staff_id | staff_master | staff_id |
| booking_table | booking_id | booking_master | booking_id |
| booking_table | table_id | table_master | table_id |
| booking_dish_detail | booking_id | booking_master | booking_id |
| booking_dish_detail | dish_id | dish_master | dish_id |
| dish_recipe | dish_id | dish_master | dish_id |
| dish_recipe | ingredient_id | ingredient_master | ingredient_id |
| dish_tag_relation | dish_id | dish_master | dish_id |
| dish_tag_relation | tag_id | dish_tag | id |
| dish_usage_relation | dish_id | dish_master | dish_id |
| dish_usage_relation | usage_id | dish_usage | id |
| ingredient_inventory_log | ingredient_id | ingredient_master | ingredient_id |
| ingredient_purchase | ingredient_id | ingredient_master | ingredient_id |
| ingredient_purchase | supplier_id | supplier_master | supplier_id |
| package_dish_detail | dish_id | dish_master | dish_id |
| staff_master | dept_id | department | dept_id |
| banquet_template_rel | banquet_type_id | banquet_type | id |
| banquet_template_rel | template_id | banquet_template | id |
| template_category_rel | template_id | banquet_template | id |
| template_category_rel | menu_category_id | menu_category | id |
| template_dish_rel | template_id | banquet_template | id |
| template_dish_rel | menu_category_id | menu_category | id |

---

## 四、核心业务流程

### 宴会预定流程
```
客户(customer_master) → 创建预定(booking_master) → 分配桌位(booking_table)
    → 选择菜品(booking_dish_detail) → 关联套餐(package_master)
```

### 采购入库流程
```
创建采购单(purchase_order) → 明细(purchase_order_detail)
    → 入库(purchase_receipt) → 明细(purchase_receipt_detail)
    → 库存流水(ingredient_inventory_log)
```

### 财务结算流程
```
预定(booking_master) → 收款(finance_payment_record)
    → 流水(finance_transaction) → 凭证(finance_voucher)
    → 对账(finance_reconciliation) → 结算(finance_settlement)
```

### 会员体系
```
办卡(member_card) → 充值(member_recharge_record) → 消费(member_consume_record)
    → 积分(member_point_log) → 等级(member_level)
```

---

## 五、数据量统计

| 模块 | 表数 | 总行数 |
|------|------|--------|
| 菜品管理 | 11 | ~3,600 |
| 食材/库存 | 15 | ~2,200 |
| 宴会预定 | 9 | ~1,500 |
| 财务 | 13 | ~350 |
| 人事/考勤 | 10 | ~1,400 |
| 客户/会员 | 7 | ~400 |
| 营销 | 7 | ~50 |
| 报表 | 5 | ~70 |
| 系统管理 | 11 | ~530 |
| AI | 2 | ~30 |
| **合计** | **88** | **~10,000** |
