# 又见炊烟餐饮管理系统 - 数据库规划设计使用说明书

> **版本**: V1.0
> **日期**: 2026-08-01
> **数据库**: MySQL 8.0 / utf8mb4 / utf8mb4_0900_ai_ci
> **总表数**: 113 | **总字段数**: 1816 | **外键数**: 40 | **索引数**: 110

---

## 第一章 系统概述

### 1.1 系统定位

又见炊烟餐饮管理系统是面向双门店（宁国总店 store_id=1、宣城分店 store_id=2）的餐饮一体化管理平台，涵盖预订、菜品、客户、员工、财务、采购、库存、营销、会员、报表、AI辅助等11大业务模块。

### 1.2 数据库技术栈

| 项目 | 配置 |
|------|------|
| 数据库 | MySQL 8.0 |
| 字符集 | utf8mb4 |
| 排序规则 | utf8mb4_0900_ai_ci（全库统一） |
| 存储引擎 | InnoDB（全部表） |
| 连接方式 | JDBC + JPA Hibernate |
| ORM | Spring Data JPA + JdbcTemplate 混合 |

### 1.3 门店规划

| store_id | 门店 | 角色 | 数据范围 |
|----------|------|------|---------|
| 0 | 全局 | 超级总经理(GM) | data_scope=all（全门店） |
| 1 | 宁国总店 | 总店员工(HQ_STAFF) | data_scope=store（仅本店） |
| 2 | 宣城分店 | 分店店长(STORE_MANAGER) | data_scope=store（仅本店） |
| 2 | 宣城分店 | 分店服务员(WAITER) | data_scope=store（仅本店） |

---

## 第二章 数据库架构设计

### 2.1 多门店数据隔离架构

系统采用 **store_id 字段级隔离** 方案，105张业务表均包含 store_id 字段，通过 AOP 切面自动注入门店过滤条件。

```
请求 → StoreDataScopeAspect（拦截@GetMapping）
         ↓ 从JWT解析 storeId
       UserContext（ThreadLocal）
         ↓ isDataScopeAll() / currentStoreId()
       Controller → Service → Repository/SQL
                         ↓
                    WHERE store_id = ?
```

**隔离层次**：
1. **AOP切面**（StoreDataScopeAspect）：自动从JWT解析storeId
2. **UserContext**：提供 currentStoreId()/isDataScopeAll()/assertStoreAccess()
3. **Controller层**：resolveQueryStoreId()（查询）/ ensureDataScopeFromStoreId()（写入）
4. **数据库层**：所有业务表含 store_id 字段 + 索引

### 2.2 RBAC 权限架构

```
sys_role (4角色)
  ├── sys_user_role (4映射) → staff_master
  └── sys_role_permission (51映射) → sys_permission (20权限点)
sys_menu (15菜单) → permission_code → sys_permission
```

**权限层级**：
- **API权限**（sys_permission）：url + method 匹配模式
- **菜单权限**（sys_menu）：前端动态菜单
- **数据权限**（sys_role.data_scope）：all=全门店 / store=本店

### 2.3 数据安全架构

```
敏感数据 → AESUtil.encrypt() → DB存储(ENC:Base64)
                                    ↓
敏感数据 ← AESUtil.decrypt() ← DB读取
                ↓
         @JsonSerialize → 前端展示(脱敏)
```

**加密覆盖**：
| 实体 | 字段 | 加密(@Convert) | 脱敏(@JsonSerialize) |
|------|------|---------------|---------------------|
| FinanceAccount | bank_account | ✅ | ✅ |
| StaffMaster | bank_account | ✅ | ✅ |
| StaffMaster | id_card | ✅ | ✅ |
| SupplierMaster | bank_account | ✅ | ✅ |
| StoreInfo | bank_account | ✅ | ✅ |

**密钥管理**：环境变量注入（AES_SECRET_KEY），无硬编码默认值

---

## 第三章 表结构设计（按模块分组）

### 3.1 系统管理模块（12表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| sys_role | 系统角色 | role_id, role_code, store_id, data_scope |
| sys_permission | 接口权限点 | permission_id, permission_code, url, method |
| sys_user_role | 员工-角色关联 | staff_id, role_id, store_id |
| sys_role_permission | 角色-权限关联 | role_id, permission_id |
| sys_menu | 前端动态菜单 | menu_id, parent_id, path, permission_code |
| sys_dict | 数据字典 | dict_code, dict_name |
| sys_dict_item | 字典项 | dict_id, item_value, item_label |
| sys_notification | 系统通知 | notification_id, store_id, title, content |
| sys_operation_log | 操作日志 | log_id, user_id, action, detail |
| audit_logs | 审计日志 | id, user_id, action, target, detail, store_id |
| config | 系统配置 | config_key, config_value, store_id |
| change_log | 变更日志 | id, table_name, record_id, change_type, store_id |

### 3.2 门店管理模块（4表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| store_info | 门店信息 | store_id, store_code, store_name, store_type, bank_account(加密) |
| table_master | 桌位管理 | table_id, store_id, table_number, table_area, max_seats, table_status |
| department | 部门管理 | dept_id, store_id, dept_name, parent_id |
| post | 岗位管理 | post_id, store_id, post_name, post_code |

### 3.3 预订管理模块（8表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| booking_master | 预订主表 | booking_id(varchar20), store_id, customer_id, booking_date, booking_status |
| booking_table | 预订-桌位关联 | table_booking_id, booking_id, table_id, store_id |
| booking_dish_detail | 预订菜品明细 | dish_booking_id, booking_id, dish_id, store_id, quantity |
| banquet_template | 宴会模板 | id, store_id, template_name, template_type |
| banquet_template_rel | 模板关联 | id, template_id, category_id, dish_id |
| banquet_type | 宴会类型 | type_id, store_id, type_name, base_price |
| template_category_rel | 模板分类关联 | id, template_id, category_id |
| template_dish_rel | 模板菜品关联 | id, template_id, dish_id, category_id |

### 3.4 菜品管理模块（16表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| dish_master | 菜品主表 | dish_id(varchar20), store_id, dish_name, dish_price, dish_status |
| dish_category | 菜品分类 | id, store_id, category_name, parent_id |
| dish_recipe | 菜品配方 | dish_id, store_id, ingredient_id, ingredient_amount |
| dish_tag | 菜品标签 | tag_id, store_id, tag_name |
| dish_tag_relation | 菜品标签关联 | dish_id, store_id, tag_id |
| dish_usage | 菜品用途 | usage_id, store_id, usage_name |
| dish_usage_relation | 菜品用途关联 | dish_id, store_id, usage_id |
| dish_occasion_names | 场合名称 | dish_id, store_id, occasion_name |
| menu_category | 菜单分类 | id, store_id, category_name, sort_order |
| categories | 分类(旧) | id, name |
| cost_card | 成本卡 | cost_card_id, store_id, dish_id, total_cost |
| cost_card_detail | 成本卡明细 | detail_id, cost_card_id, store_id, ingredient_id, cost |
| package_master | 套餐主表 | package_id(varchar20), store_id, package_name, package_price |
| package_dish_detail | 套餐菜品明细 | detail_id, package_id, store_id, dish_id, quantity |
| packages | 套餐(旧) | id, code, name |
| meal_package | 餐标套餐 | id, store_id, package_name, meal_type |

### 3.5 客户管理模块（1表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| customer_master | 客户主表 | customer_id(int), store_id, customer_name, phone, customer_type, visit_count |

### 3.6 员工管理模块（14表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| staff_master | 员工主表 | staff_id(int), store_id, staff_name, phone, id_card(加密), bank_account(加密), employment_status |
| employee_lifecycle | 员工生命周期 | id, staff_id, store_id, event_type, event_date |
| contract | 劳动合同 | contract_id, store_id, staff_id, contract_start, contract_end |
| attendance | 日考勤打卡 | attendance_id, store_id, staff_id, attendance_date, clock_in, clock_out |
| attendance_records | 月考勤汇总 | id, staff_id, month, total_present, total_overtime, total_leave |
| leave_record | 请假记录 | leave_id, store_id, staff_id, leave_type, leave_start, leave_end |
| overtime | 加班记录 | overtime_id, store_id, staff_id, overtime_date, overtime_hours |
| reward_punish | 奖惩记录 | rp_id, store_id, staff_id, rp_type, rp_amount, rp_date |
| month_salary | 月度薪资 | salary_id, store_id, staff_id, salary_month, base_salary, net_salary |
| salary_template | 薪资模板 | template_id, store_id, template_name, base_salary |
| schedule | 排班主表 | schedule_id, store_id, schedule_date, shift_type |
| schedule_month | 月排班 | schedule_id, store_id, staff_id, month |
| schedule_day | 日排班 | day_id, schedule_id, store_id, day_date, shift |
| reimbursement | 报销 | id, store_id, staff_id, amount, type, status |
| approval_log | 审批日志 | id, store_id, approver_id, target_type, target_id, action |

### 3.7 财务模块（11表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| finance_account | 资金账户 | account_id(bigint), store_id, account_name, account_type, current_balance, bank_account(加密) |
| finance_receivable | 应收账款 | receivable_id, store_id, customer_id, booking_id(varchar20), receivable_amount, received_amount, status |
| finance_payable | 应付账款 | payable_id, store_id, supplier_id, purchase_id, payable_amount, paid_amount, status |
| finance_payment_record | 收款记录 | payment_id, store_id, receivable_id, booking_id(varchar20), amount, payment_method |
| finance_expense | 费用报销 | expense_id, store_id, expense_type, amount, approval_status, payment_status |
| finance_cost_record | 成本记录 | cost_id, store_id, cost_date, cost_type, amount, department_id |
| finance_transaction | 资金交易流水 | trans_id, store_id, trans_type, account_id, amount, balance_after |
| finance_voucher | 会计凭证 | voucher_id, store_id, voucher_no, voucher_date, total_debit, total_credit, is_balanced |
| finance_voucher_detail | 凭证明细 | detail_id, voucher_id, store_id, subject_code, debit_amount, credit_amount |
| finance_reconciliation | 银行对账 | recon_id, store_id, account_id, book_balance, bank_balance, diff_amount, status |
| finance_settlement | 期末结账 | settlement_id, store_id, settlement_date, total_income, total_expense, total_profit |

### 3.8 采购库存模块（22表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| ingredient_master | 原料主表 | ingredient_id(varchar20), store_id, ingredient_name, unit, unit_price |
| ingredient_purchase | 采购订单 | purchase_id, store_id, supplier_id, purchase_date, total_amount |
| ingredient_inventory_log | 库存日志 | log_id, store_id, ingredient_id, change_type, change_amount, balance_after |
| supplier_master | 供应商主表 | supplier_id, store_id, supplier_name, contact_phone, bank_account(加密) |
| material_requisition | 领料单 | requisition_id, store_id, department_id, requisition_date |
| material_requisition_item | 领料明细 | item_id, requisition_id, store_id, ingredient_id, quantity |
| purchase_request | 采购申请 | request_id, store_id, request_date, total_amount |
| purchase_request_item | 采购明细 | item_id, request_id, store_id, ingredient_id, quantity, unit_price |
| purchase_receipt | 收货单 | receipt_id, store_id, purchase_id, receipt_date |
| purchase_receipt_detail | 收货明细 | detail_id, receipt_id, store_id, ingredient_id, received_quantity |
| purchase_return | 退货单 | id, store_id, purchase_id, return_date, total_amount |
| purchase_return_detail | 退货明细 | detail_id, return_id, store_id, ingredient_id, return_quantity |
| goods_receipt | 入库单 | receipt_id, store_id, supplier_id, receipt_date |
| goods_receipt_item | 入库明细 | item_id, receipt_id, store_id, ingredient_id, quantity, unit_price |
| stock_loss | 报损单 | loss_id, store_id, loss_date, total_amount, loss_type |
| stock_loss_detail | 报损明细 | detail_id, loss_id, store_id, ingredient_id, loss_quantity, loss_amount |
| stock_take | 盘点单 | take_id, store_id, take_date, status |
| stock_take_detail | 盘点明细 | detail_id, take_id, store_id, ingredient_id, book_quantity, actual_quantity |
| stock_transfer | 调拨单 | transfer_id, store_id, from_store_id, to_store_id, transfer_date |
| stock_transfer_detail | 调拨明细 | detail_id, transfer_id, store_id, ingredient_id, transfer_quantity |
| procurement_request | 采购请求 | id, store_id, status, total_amount |
| unit_conversion | 单位换算 | conversion_id, store_id, from_unit, to_unit, conversion_ratio |
| yield_rate_config | 出成率配置 | id, store_id, ingredient_id, yield_rate |
| preprocessing_record | 预处理记录 | record_id, store_id, ingredient_id, preprocess_date, output_weight |

### 3.9 营销会员模块（13表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| marketing_activity | 营销活动 | activity_id, store_id, activity_name, start_date, end_date, status |
| marketing_coupon | 优惠券 | coupon_id, store_id, coupon_name, coupon_type, discount_value |
| marketing_coupon_record | 优惠券领取记录 | record_id, store_id, customer_id, coupon_id, booking_id(varchar20) |
| marketing_discount_rule | 折扣规则 | rule_id, store_id, rule_name, discount_type, discount_value |
| marketing_lottery | 抽奖活动 | lottery_id, store_id, lottery_name, start_date, end_date |
| marketing_member_reward | 会员奖励 | reward_id, store_id, member_id, reward_type, reward_value |
| marketing_promo_code | 促销码 | code_id, store_id, promo_code, discount_value, expire_date |
| member_card | 会员卡 | card_id, store_id, member_name, phone, card_no, balance, points |
| member_consume_record | 消费记录 | record_id, store_id, member_id, booking_id(varchar20), consume_amount |
| member_level | 会员等级 | level_id, store_id, level_name, min_points, discount_rate |
| member_point_log | 积分日志 | log_id, store_id, member_id, point_change, change_type |
| member_point_rule | 积分规则 | rule_id, store_id, rule_name, points_per_yuan, action_type |
| member_recharge_record | 充值记录 | record_id, store_id, member_id, recharge_amount, bonus_amount |

### 3.10 报表模块（5表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| report_daily | 日报表 | report_id, store_id, report_date, revenue, cost, profit |
| report_monthly | 月报表 | report_id, store_id, report_month, revenue, cost, profit |
| report_dish_sales | 菜品销售报表 | report_id, store_id, dish_id, sales_quantity, sales_amount |
| report_department_cost | 部门成本报表 | report_id, store_id, department_id, cost_amount |
| report_staff_kpi | 员工KPI报表 | report_id, store_id, staff_id, kpi_score, kpi_month |

### 3.11 AI辅助模块（2表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| ai_chat_history | AI对话历史 | id, staff_id, store_id, role, content, create_time |
| ai_memory | AI记忆 | id, store_id, memory_type, memory_content, create_time |

### 3.12 其他（1表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| kitchen_log | 厨房日志 | id, store_id, booking_id, dish_id, log_type, log_content |

---

## 第四章 级联关系设计

### 4.1 级联策略

| 策略 | 数量 | 适用场景 |
|------|------|---------|
| CASCADE | 12 | 明细表（删除主表自动清理明细） |
| RESTRICT | 18 | 业务关键表（有引用时禁止删除） |
| NO ACTION | 10 | 类似RESTRICT |
| SET NULL | 1 | 非关键关联（设为NULL） |
| **合计** | **41** | |

### 4.2 关键级联关系

**CASCADE（自动清理明细）**：
```
finance_voucher →(CASCADE)→ finance_voucher_detail
dish_master →(CASCADE)→ dish_recipe, dish_tag_relation, dish_usage_relation, package_dish_detail
schedule_month →(CASCADE)→ schedule_day
stock_transfer →(CASCADE)→ stock_transfer_detail
banquet_template →(CASCADE)→ template_category_rel, template_dish_rel
```

**RESTRICT（禁止误删有引用数据）**：
```
booking_master ←(RESTRICT)← finance_payment_record, finance_receivable
staff_master ←(RESTRICT)← month_salary, reward_punish
customer_master ←(RESTRICT)← finance_receivable
finance_account ←(RESTRICT)← finance_expense, finance_transaction, finance_reconciliation
```

---

## 第五章 安全设计

### 5.1 数据加密

**算法**：AES-256-GCM（随机IV + 认证标签）

**加密流程**：
```
明文 → AESUtil.encrypt() → "ENC:" + Base64(IV + Ciphertext + Tag) → DB存储
DB读取 → AESUtil.decrypt() → 去除"ENC:"前缀 → Base64解码 → AES-GCM解密 → 明文
```

**自动加解密**：JPA AttributeConverter（BankAccountConverter）
- 写入DB时自动调用 encrypt()
- 读取DB时自动调用 decrypt()
- 兼容明文（无"ENC:"前缀的视为明文，直接返回）

### 5.2 数据脱敏

**脱敏规则**：
| 数据类型 | 脱敏规则 | 示例 |
|----------|---------|------|
| 银行账号 | 前4位 + **** + 后4位 | 6222****7890 |
| 身份证号 | 前3位 + ******** + 后4位 | 342**********1234 |
| 手机号 | 前3位 + **** + 后4位 | 138****5678 |

**实现方式**：Jackson @JsonSerialize 注解，在 JSON 序列化时自动脱敏

### 5.3 密钥管理

| 密钥 | 用途 | 注入方式 |
|------|------|---------|
| JWT_SECRET | JWT签名 | 环境变量（无默认值） |
| AES_SECRET_KEY | 数据加密 | 环境变量（无默认值） |

**原则**：生产环境/Docker环境强制环境变量注入，本地开发保留默认值。

### 5.4 API安全

| 安全措施 | 实现 |
|----------|------|
| JWT认证 | JwtAuthInterceptor 拦截 /api/** |
| 门店隔离 | StoreDataScopeAspect + UserContext |
| API限流 | RateLimitInterceptor（登录5次/分，普通60次/分） |
| 审计日志 | AuditLogAspect 拦截所有POST/PUT/DELETE |
| SQL注入防护 | 全部使用参数化查询（?占位符） |

---

## 第六章 数据备份策略

### 6.1 备份脚本

```bash
# /data/backups/mysql/backup_strategy.sh
docker exec youjian-mysql-local mysqldump \
  --single-transaction --routines --triggers --events \
  -u${DB_USER} -p${DB_PASS} ${DB_NAME} | gzip > ${BACKUP_FILE}
```

### 6.2 备份策略

| 项目 | 配置 |
|------|------|
| 备份频率 | 每日 02:00（crontab） |
| 保留期 | 30天 |
| 压缩 | gzip |
| 包含 | routines + triggers + events |
| 存储路径 | /data/backups/mysql/ |

---

## 第七章 索引设计

### 7.1 索引统计

| 指标 | 数量 |
|------|------|
| 有索引的表 | 110 |
| 主键索引 | 113（每表1个） |
| 唯一索引 | ~20 |
| 普通索引 | ~90 |

### 7.2 关键索引

| 表名 | 索引字段 | 说明 |
|------|---------|------|
| booking_master | store_id, booking_date, booking_status | 按门店+日期查询预订 |
| staff_master | store_id, employment_status | 按门店查在职员工 |
| finance_receivable | store_id, status | 按门店查应收状态 |
| finance_payment_record | store_id, receivable_id | 按门店+应收查收款 |
| sys_permission | url, method | API权限匹配 |
| sys_user_role | staff_id, role_id | 用户角色查询 |

---

## 第八章 实体类与数据库映射规范

### 8.1 JPA 注解规范

| 注解 | 用途 | 必填 |
|------|------|------|
| @Entity | 标记为JPA实体 | ✅ |
| @Table(name="表名") | 映射数据库表 | ✅ |
| @Id | 主键 | ✅ |
| @GeneratedValue(strategy=IDENTITY) | 自增主键 | 自增表✅ |
| @Column(name="列名") | 字段映射 | ✅ |
| @Convert(converter=XxxConverter) | 自动加解密 | 敏感字段✅ |
| @JsonSerialize(using=XxxSerializer) | JSON脱敏 | 敏感字段✅ |
| @PrePersist | 创建时自动填充createdAt | 有审计列✅ |
| @PreUpdate | 更新时自动填充updatedAt | 有审计列✅ |
| @Transient | 非持久化字段 | 按需 |

### 8.2 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 数据库表 | snake_case，单数 | staff_master, finance_account |
| 数据库列 | snake_case | store_id, created_at |
| Java类名 | PascalCase | StaffMaster, FinanceAccount |
| Java字段 | camelCase | staffId, createdAt |
| 主键 | {表名去后缀}_id | staff_id, account_id, payment_id |

### 8.3 时间戳规范

所有业务表必须包含审计时间字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| created_at | timestamp DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

对应 Java 实体使用 @PrePersist/@PreUpdate 自动填充。

---

## 第九章 环境配置

### 9.1 环境变量清单

| 变量名 | 用途 | 必填 |
|--------|------|------|
| MYSQL_HOST | 数据库主机 | 默认mysql |
| MYSQL_DATABASE | 数据库名 | 默认banquet |
| MYSQL_USER | 数据库用户 | 默认rino |
| MYSQL_PASSWORD | 数据库密码 | ✅ |
| JWT_SECRET | JWT签名密钥（≥32字节） | ✅ |
| AES_SECRET_KEY | AES加密密钥（32字节=AES-256） | ✅ |
| COS_SECRET_ID | 腾讯云COS密钥ID | 按需 |
| COS_SECRET_KEY | 腾讯云COS密钥 | 按需 |
| COS_BUCKET | COS桶名 | 按需 |
| TIANLONG_TOKEN | 天龙AI网关Token | 按需 |

### 9.2 环境配置

| 环境 | 配置文件 | 密钥策略 |
|------|---------|---------|
| 本地开发 | application.yml（默认profile） | 硬编码默认值 |
| Docker | application.yml（docker profile） | 强制环境变量 |
| 生产 | application-prod.yml | 强制环境变量 |

### 9.3 健康检查与监控

| 端点 | 用途 |
|------|------|
| /actuator/health | 健康检查 |
| /actuator/info | 应用信息 |
| /swagger-ui.html | API文档 |
| /api-docs | OpenAPI JSON |

---

## 第十章 使用指南

### 10.1 数据库初始化

```bash
# 1. 创建数据库
docker exec youjian-mysql-local mysql -urino -pWo002323 -e "CREATE DATABASE IF NOT EXISTS banquet CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2. 执行建表脚本（按顺序）
docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet < schema.sql
docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet < rbac_init.sql
docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet < seed_data.sql

# 3. 执行修复脚本
docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet < fix_collation_v1.sql
```

### 10.2 数据备份与恢复

```bash
# 备份
docker exec youjian-mysql-local mysqldump -urino -pWo002323 --single-transaction --routines --triggers --events banquet | gzip > backup.sql.gz

# 恢复
gunzip < backup.sql.gz | docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet
```

### 10.3 启动应用

```bash
# 设置环境变量
export JWT_SECRET="your-jwt-secret-at-least-32-bytes"
export AES_SECRET_KEY="your-aes-secret-key-exactly-32-bytes"
export MYSQL_PASSWORD="your-db-password"

# 启动
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 10.4 新增业务表检查清单

新增表时必须确认：
- [ ] 表名 snake_case，含 store_id 字段
- [ ] 字符集 utf8mb4，排序规则 utf8mb4_0900_ai_ci
- [ ] 包含 created_at + updated_at 审计字段
- [ ] 创建对应 JPA 实体类（@Table + @Id + @Column + @PrePersist/@PreUpdate）
- [ ] 创建对应 Repository 接口
- [ ] 敏感字段加 @Convert + @JsonSerialize
- [ ] Controller 层使用 resolveQueryStoreId() 进行门店隔离
- [ ] 写操作方法加 @Transactional
- [ ] 外键关系明确（CASCADE/RESTRICT）
- [ ] 关键查询字段添加索引

---

## 附录

### A. 审计报告索引

| 报告 | 版本 | 内容 |
|------|------|------|
| SYSTEM_AUDIT_REPORT_V4 | V4 | 初始系统审计（36项数据库问题） |
| DEEP_AUDIT_REPORT_V7 | V7 | 深度审计（字段注释/外键/实体一致性/E2E） |
| DEAD_CORNER_AUDIT_V8 | V8 | 死角审计（排序规则/重复表/类型不一致） |
| FINAL_COMPLETION_V9 | V9 | 全量待处理事项完成（P0/P1/P2） |
| ISOLATION_RBAC_CASCADE_V10 | V10 | 分店隔离·RBAC·级联关系审计 |
| HIDDEN_RISKS_RESOLVED_V11 | V11 | 15项系统隐患消除 |

### B. 数据库统计

| 指标 | 数量 |
|------|------|
| 总表数 | 113 |
| 总字段数 | 1816 |
| 外键数 | 40 |
| 索引数 | 110 |
| JPA实体类 | 62 |
| JPA Repository | 55 |
| 加密字段 | 7（5实体） |
| 脱敏字段 | 7（5实体） |
| 角色数 | 4 |
| 权限点 | 20 |
| 菜单项 | 15 |
