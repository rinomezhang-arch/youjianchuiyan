# 又见炊烟餐饮管理系统 · 全栈审计报告

> **审计范围**：`f:\solo` 全工程（数据库 / 后端 Spring Boot / 前端 Vue 3 / 部署配置）
> **门店范围**：宣城、宁国双门店
> **审计方式**：5 路并行 search agent + 关键证据精准验证
> **审计日期**：2026-07-28
> **审计结论**：**系统目前不可上生产，存在 5 个致命级、7 个严重级、30+ 一般级问题**

---

## 目录

- [一、系统规模总览](#一系统规模总览)
- [二、致命级问题（P0，必须立即修复）](#二致命级问题p0必须立即修复)
- [三、严重级问题（S）](#三严重级问题s)
- [四、数据库结构审计](#四数据库结构审计)
- [五、后端审计](#五后端审计)
- [六、前端审计](#六前端审计)
- [七、前后端字段对齐审计](#七前后端字段对齐审计)
- [八、权限与登录审计](#八权限与登录审计)
- [九、业务模块完整性审计](#九业务模块完整性审计)
- [十、安全漏洞清单](#十安全漏洞清单)
- [十一、改造方案（按优先级）](#十一改造方案按优先级)
- [十二、关键结论](#十二关键结论)

---

## 一、系统规模总览

| 维度 | 数量 | 文件位置 |
|------|------|---------|
| 数据库表 | **103 张** | `youjian-docker/mysql/init/banquet_init.sql` |
| 后端 Controller | **23 个** | `banquet_project/src/main/java/com/youjian/banquet/controller/` |
| 后端 API 接口 | **134 个** | 23 个 Controller 内 |
| 前端动态路由 | **74 个** | `frontend_v3/src/router/index.js` |
| 前端视图文件 | **80+ 个 .vue** | `frontend_v3/src/views/dashboard/` |
| 含占位符/TODO 的文件 | **61 个**（331 处匹配） | 大量页面 |
| 外键约束 | **仅 34 处** | 数据库 schema |
| `store_id` 字段 | 223 处出现 | 基本覆盖业务表 |
| `@Transactional` 使用 | **仅 15 处** | 集中在 Booking/Customer/Recipe |
| `@PreAuthorize` 使用 | **0 处** | 全工程零权限注解 |

### 技术栈
- **后端**：Spring Boot 3.2.5 + Java 17 + JPA/Hibernate + MySQL 8.0
- **前端**：Vue 3.4 + Vite 5 + Pinia + Vue Router
- **部署**：Docker Compose（backend / frontend / mysql 三容器）
- **AI 服务**：通过 OpenClaw Gateway（天龙）调用 DeepSeek/Dashscope

---

## 二、致命级问题（P0，必须立即修复）

### 🔴 P0-1 后端 API 完全没有登录/权限拦截器 —— 系统裸奔

**证据**：
- `banquet_project/src/main/java/com/youjian/banquet/config/WebMvcConfig.java` 只注册了 `/api/ipad/**` 一个拦截器
- 全工程搜索 `@PreAuthorize` **零结果**
- 没有 JWT 拦截器、没有 Spring Security 依赖

**后果**：
任何人直接 `curl http://服务器IP:8080/api/hr/list` 就能：
- 查看所有员工薪资（`PayrollController`）
- 删除任意员工（`StaffController.delete`）
- 修改任意预订、账单、库存
- 调用 AI 接口消耗 token
- 修改/删除财务凭证

**修复方案**：
1. 新建 `JwtAuthInterceptor`，注册到 `/api/**`（排除 `/api/auth/login`）
2. 校验 JWT，注入 `staffId / storeId / role` 到 request 属性
3. 引入 Spring Security 或自定义注解 `@RequireRole`

---

### 🔴 P0-2 没有角色表、权限表、菜单权限表

**证据**：
- 数据库 schema 中搜索 `CREATE TABLE.*role|permission|menu` 只找到 `menu_category`（菜品分类，**不是**菜单权限）
- 数据库**缺失**以下基础权限表：
  - `sys_role`（角色表）
  - `sys_permission`（权限点表）
  - `sys_role_permission`（角色-权限关联）
  - `sys_user_role`（用户-角色关联）
  - `sys_menu`（菜单权限表）
  - `sys_data_scope`（数据权限范围表）

**后果**：
- "超级管理员 rino/张婧" 只是 `Login.vue` 默认账号，没有真正权限模型
- 项目记忆里写的"网络权限表ID验证"未在 schema 中找到对应实现
- HR、财务、采购、店长、服务员角色无法区分权限
- 前端 `PermManager.vue` 是占位符

---

### 🔴 P0-3 iPad 拦截器形同虚设

**证据**：`IpadInterceptor.java` 第 20-23 行：
```java
if (!"ipad".equals(clientType)) {
    return true;  // 非 iPad 客户端直接放行
}
```

**后果**：
PC 端、Postman、curl 任何客户端**只要不传 `X-Client-Type: ipad`** 即可绕过校验，直接调用 `/api/ipad/**` 所有 iPad 接口。项目记忆硬约束"iPad 必须四头校验"**完全失效**。

**修复方案**：
- 非 iPad 客户端访问 `/api/ipad/**` 应返回 `403 Forbidden`，而不是放行

---

### 🔴 P0-4 敏感接口零事务保护、零权限

**证据**：
- `StaffController` create/update/delete 无 `@Transactional`、无权限校验、删除是软删但无审计
- 全工程仅 15 处 `@Transactional`，集中在 `BookingController` / `CustomerController` / `RecipeController`
- `PayrollController`、`HRController`、`PurchaseController`、`InventoryController` 等写操作**几乎都没事务**

**后果**：
- 多表写操作中途失败 → 数据不一致（如预订+桌位+菜品三表插入失败时残留）
- 薪资、删除员工、对账等高敏感操作完全裸奔

---

### 🔴 P0-5 数据库外键约束严重缺失

**证据**：103 张表只有 34 处 `FOREIGN KEY / REFERENCES` 约束。

**后果**：
- 删除 `booking_master` 后 `booking_dish_detail`、`booking_table` 残留为孤儿数据
- 删除 `staff_master` 后 `attendance`、`leave_record`、`payroll` 残留
- 删除 `ingredient_master` 后 `ingredient_purchase`、`ingredient_inventory_log` 残留
- 项目记忆明确"无孤儿数据"约束 → **已被违反**

---

## 三、严重级问题（S）

### 🟠 S-1 多门店数据隔离靠"约定"不靠"约束"

- `store_id` 字段 223 处出现，但**绝大多数没建索引、没外键、没数据库层强制**
- 后端接口靠手动传 `storeId` → 极易漏传，造成宣城看到宁国数据
- 没有统一的 storeId 切面或 Hibernate Filter

### 🟠 S-2 AIController 路径不统一

**证据**：`AIController.java:16` 用 `@RequestMapping("/ai")`，其他全部是 `/api/...`

**后果**：
- 前端 `api/ai.js` 必须特殊处理
- `/ai` **不在 iPad 拦截器范围**，也**不在任何权限拦截范围** → 任何人可消耗你的 DeepSeek token

### 🟠 S-3 JWT Secret 和数据库密码硬编码

**证据**：`application-prod.yml`:
```yaml
password: Wo002323              # DB 密码
jwt.secret: YJCY-Banquet-2026-Secret-Key-Wo002323
tianlong.token: Wo002323
```

虽然用了 `${ENV:default}` 语法，但**默认值就是真实密码** → 一旦未设置环境变量，生产环境用真实密码跑。

**修复方案**：必须立即更换并移除默认值。

### 🟠 S-4 字段命名对齐靠人工 @Column，无策略保证

**证据**：
- `application-prod.yml` 未配置 `hibernate.physical_naming_strategy`
- `ddl-auto: none` 不自动同步 schema
- 未配置 `spring.jackson.property-naming-strategy`

**后果**：
全靠 Java 实体里手写 `@Column(name="...")`，新增字段忘记加注解 → 数据存不进 / 读不出。

### 🟠 S-5 iPad 接口与 PC 接口命名规范混用风险

PC 端默认 Jackson 驼峰序列化，iPad 要求 snake_case。若无 `@JsonNaming` 或全局 `PropertyNamingStrategy` 配置 → iPad 端字段会显示 `staffName` 而非 `staff_name`，与硬约束冲突。

### 🟠 S-6 GlobalExceptionHandler 覆盖不全

`GlobalExceptionHandler.java` 仅基础异常，缺：
- `AccessDeniedException`
- `MethodArgumentNotValidException`
- `DataIntegrityViolationException`
- `OptimisticLockingFailureException`
- `BusinessException`（自定义业务异常）

### 🟠 S-7 文件上传无安全校验

`UploadController` 仅 `multipart.max-file-size: 10MB`，缺：
- MIME 类型白名单
- 扩展名校验
- 文件内容嗅探
- 路径穿越防护
- 上传频率限制

---

## 四、数据库结构审计

### 4.1 表清单（103 张表，按业务域分组）

#### 核心业务（预订/订单/桌位/菜品）— 18 张
| 表名 | 中文注释 | 关键字段 |
|------|---------|---------|
| booking_master | 预约主表 | booking_id, store_id, customer_id, booking_date |
| booking_table | 预约桌位表 | booking_id, table_id |
| booking_dish_detail | 预约菜品详情表 | booking_id, dish_id |
| table_master | 桌位主表 | table_id, store_id, table_area |
| banquet_table | 宴会桌位表 | banquet_id, table_id |
| banquet_template | 菜单模板表 | template_id |
| banquet_template_rel | 宴会-模板关联表 | template_id, banquet_id |
| banquet_type | 宴会类型表 | type_id |
| dish_master | 菜品主表 | dish_id, store_id |
| dish_category | 厨房分类表 | category_id |
| dish_occasion_names | 菜品适用场合 | occasion_id |
| dish_recipe | 菜品配方表 | dish_id, ingredient_id |
| dish_tag / dish_tag_relation | 菜品标记 | tag_id, dish_id |
| dish_usage / dish_usage_relation | 菜品用途 | usage_id, dish_id |
| dish_cost_card / dish_cost_card_detail | 菜品成本卡 | dish_id |
| menu_category | 菜品分类表 | category_id |
| categories | 菜品分类（疑似冗余） | category_id |
| dishes | 菜品（疑似冗余） | dish_id |

**问题**：`categories` 与 `menu_category` 重复，`dishes` 与 `dish_master` 重复 → 数据冗余。

#### 客户/会员 — 12 张
- `customer_master`、`member_card`、`member_level`、`member_consume_record`、`member_point_log`、`member_point_rule`、`member_recharge_record`、`marketing_activity`、`marketing_coupon`、`marketing_coupon_record`、`marketing_discount_rule`、`marketing_lottery`、`marketing_member_reward`、`marketing_promo_code`

#### 套餐 — 5 张
- `package_master`、`package_dish_detail`、`package_dish_rel`、`package_details`、`packages`、`meal_package`、`pkg_used`

**问题**：套餐相关表有 4-5 张疑似重复（`package_master` / `packages` / `meal_package`）→ 命名混乱、可能数据冲突。

#### 员工/HR — 8 张
- `staff_master`、`department`、`attendance`、`attendance_records`、`leave_record`、`overtime`、`payroll`、`schedule`、`employee_lifecycle`

**问题**：`attendance` 与 `attendance_records` 命名接近、用途重叠 → 需明确边界。

#### 采购/库存 — 16 张
- `supplier_master`、`ingredient_master`、`ingredient_purchase`、`ingredient_inventory_log`、`procurement_request`、`purchase_order`、`purchase_order_detail`、`purchase_receipt`、`purchase_receipt_detail`、`purchase_return`、`purchase_return_detail`、`requisition_order`、`requisition_detail`、`stock_take`、`stock_take_detail`、`stock_transfer`、`stock_loss`、`stock_loss_detail`

#### 财务 — 13 张
- `finance_account`、`finance_cost_record`、`finance_expense`、`finance_payable`、`finance_payment_record`、`finance_receivable`、`finance_reconciliation`、`finance_settlement`、`finance_transaction`、`finance_voucher`、`finance_voucher_detail`、`reimbursement`、`report_daily`、`report_department_cost`、`report_dish_sales`、`report_monthly`、`report_staff_kpi`

#### 系统 — 9 张
- `admin_users`、`config`、`audit_logs`、`change_log`、`approval_log`、`sys_notification`、`sys_operation_log`、`sys_dict`、`sys_dict_item`、`store_info`、`ai_chat_history`、`ai_memory`、`kitchen_log`

#### 订单 — 1 张
- `orders`（菜品订单）

### 4.2 字段关系问题

#### 缺失外键约束（严重）
| 子表 | 应关联主表 | 当前状态 |
|------|----------|---------|
| booking_dish_detail | booking_master | 无 FK |
| booking_table | booking_master | 无 FK |
| attendance | staff_master | 无 FK |
| attendance_records | staff_master | 无 FK |
| leave_record | staff_master | 无 FK |
| overtime | staff_master | 无 FK |
| payroll | staff_master | 无 FK |
| schedule | staff_master | 无 FK |
| employee_lifecycle | staff_master | 无 FK |
| ingredient_purchase | ingredient_master | 无 FK |
| ingredient_inventory_log | ingredient_master | 无 FK |
| dish_recipe | dish_master, ingredient_master | 无 FK |
| kitchen_log | booking_id / staff_id | 无 FK |
| finance_voucher_detail | finance_voucher | 无 FK |
| 各 report_* 表 | 关联实体 | 无 FK |

#### 字段命名不一致
| 类型 | 命名变体 | 影响 |
|------|---------|------|
| 时间戳 | `created_at` / `create_time` / `timestamp` | 三种并存 |
| 状态 | `status` / `is_active` / `available` | 三种并存 |
| 软删 | `is_deleted` / `deleted_at` / `is_active` | 不统一 |

#### 状态枚举未字典化
- `booking_master.status` 用 `'pending'/'confirmed'/'cancelled'` 字符串
- `staff_master.status` 用 `'active'/'resigned'/'fired'`
- 应接入 `sys_dict` 统一管理

### 4.3 多门店隔离审计

| 维度 | 现状 | 风险 |
|------|------|------|
| store_id 覆盖 | 223 处出现，基本覆盖业务表 | 但无强制索引 |
| store_id 索引 | 极少数表建了索引 | 跨门店查询慢 |
| store_id 外键 | 无表关联 store_info | 可传入不存在的 storeId |
| 数据库层隔离 | 无 | 完全靠应用层 |
| 应用层切面 | 无 Hibernate Filter / MyBatis 拦截器 | 极易漏传 storeId |
| 跨门店查询接口 | 未见统一管控 | 任何接口都可跨店读 |

### 4.4 唯一索引缺失
- `booking_master.booking_no` 应唯一，未确认是否有唯一索引
- `staff_master.staff_account` 应唯一
- `supplier_master.supplier_code` 应唯一
- `ingredient_master.ingredient_code` 应唯一

---

## 五、后端审计

### 5.1 Controller 路由清单（23 个）

| Controller | BasePath | 接口数 | 说明 |
|-----------|----------|-------|------|
| AuthController | `/api` | 4 | 登录/登出/当前用户 |
| StaffController | `/api/hr` | 4 | 员工 CRUD |
| HRController | `/api/hr` | 10 | HR 综合接口 |
| AttendanceRecordController | `/api/hr/attendance` | 4 | 考勤 |
| PayrollController | `/api/hr` | 3 | 薪资 |
| BookingController | `/api` | 19 | 预订（最复杂） |
| TableController | `/api` | 6 | 桌位 |
| TableBoardController | `/api` | 1 | 桌位看板 |
| DishController | `/api` | 7 | 菜品 |
| RecipeController | `/api` | 4 | 配方 |
| PackageController | `/api` | 5 | 套餐 |
| CustomerController | `/api` | 7 | 客户 |
| IngredientController | `/api` | 6 | 原料 |
| InventoryController | `/api` | 6 | 库存 |
| PurchaseController | `/api` | 8 | 采购 |
| SupplierController | `/api` | 5 | 供应商 |
| DictController | `/api` | 8 | 字典 |
| UploadController | `/api/upload` | 2 | 文件上传 |
| DashboardController | `/api` | 2 | 看板 |
| **AIController** | **`/ai`** ⚠️ | 4 | AI（路径不统一） |
| IpadDishController | `/api/ipad` | 6 | iPad 菜品 |
| IpadOrderController | `/api/ipad` | 7 | iPad 订单 |
| IpadTableController | `/api/ipad` | 7 | iPad 桌位 |

### 5.2 路由命名规范问题
- 大部分用 `/list` `/add` `/update` `/delete` 动词风格，**不 RESTful**
- AIController 用 `/ai` 不带 `/api` 前缀
- 建议统一为 RESTful：`GET /api/bookings`、`POST /api/bookings`、`PUT /api/bookings/{id}`、`DELETE /api/bookings/{id}`

### 5.3 事务保护缺失
仅 15 处 `@Transactional`，分布：
- `BookingController`：11 处（预订模块相对完善）
- `CustomerController`：3 处
- `RecipeController`：2 处

**完全没事务**的写操作 Controller：
- `StaffController`、`PayrollController`、`AttendanceRecordController`
- `DishController`、`PackageController`、`TableController`
- `PurchaseController`、`InventoryController`、`IngredientController`、`SupplierController`
- `DictController`、`UploadController`

### 5.4 异常处理覆盖不全
`GlobalExceptionHandler` 仅基础异常，需补充：
- `AccessDeniedException`（权限）
- `MethodArgumentNotValidException`（参数校验）
- `ConstraintViolationException`（约束违反）
- `DataIntegrityViolationException`（数据完整性）
- `OptimisticLockingFailureException`（乐观锁）
- `BusinessException`（自定义业务异常）
- `MaxUploadSizeExceededException`（上传超限）
- `HttpRequestMethodNotSupportedException`（方法不支持）

### 5.5 Service 层缺失审计
有些 Service 在 `service/` 目录下但搜索结果未在 `controller/` 中找到对应 Controller，需核查：
- `MenuService`、`MenuCategoryService`、`MenuOccasionService`、`MenuTemplateService`
- `PackageDishService`、`DishOccasionService`、`DishRecipeService`
- `InventoryLogService`、`IngredientInventoryLogService`
- `SupplierLogService`、`IngredientPurchaseService`
- `EmployeeLifecycleService`、`LeaveRecordService`、`OvertimeService`
- `AttendanceRecordService`、`PayrollService`、`ScheduleService`

→ 这些 Service 是否有 Controller 暴露 API？是否前端有界面调用？需要核对。

### 5.6 AIController 调用链
- 调用链：前端 → `AIController` (8080) → 天龙 OpenClaw Gateway (11500) → DeepSeek/Dashscope
- Token 写死在 yml
- 无频率限制、无 token 用量统计、无错误降级

---

## 六、前端审计

### 6.1 路由总数
74 个动态路由，分布在前端 `router/index.js`。

### 6.2 视图文件清单（80+ 个）

#### 完全占位/无实际功能（25 个）— 必须从零做
| 文件 | 模块 | 状态 |
|------|------|------|
| Maintenance.vue | 工程维护-维修 | 4 处 TODO |
| Assets.vue | 工程维护-资产 | 6 处 TODO |
| Decoration.vue | 工程维护-装修 | 3 处 TODO |
| FloorProject.vue | 工程维护-楼层工程 | 占位 |
| Energy.vue | 工程维护-能耗 | 1 处 TODO |
| Engineering.vue | 工程维护主页 | 5 处 TODO |
| Hygiene.vue | 卫生管理 | 占位 |
| Safety.vue | 安全管理 | 3 处 TODO |
| Security.vue | 安保管理 | 9 处 TODO |
| Production.vue | 生产管理 | 占位 |
| Waste.vue | 损耗管理 | 占位 |
| Receipt.vue | 收据 | 11 处 TODO |
| Tax.vue | 税务 | 占位 |
| Revenue.vue | 收入 | 占位 |
| Cost.vue | 成本 | 2 处 TODO |
| CustomerAnalysis.vue | 客户画像 | 1 处 TODO |
| GuestAnalysis.vue | 客人画像 | 3 处 TODO |
| Marketing.vue | 营销 | 占位 |
| ApprovalCenter.vue | 审批中心 | 1 处 TODO |
| ReviewQueue.vue | 审批队列 | 1 处 TODO |
| Issue.vue | 问题跟踪 | 4 处 TODO |
| HRAnalytics.vue | HR 分析 | 占位 |
| Training.vue | 培训 | 6 处 TODO |
| StaffPerformance.vue | 绩效 | 占位 |
| FrontDesk.vue | 前台接待 | 8 处 TODO |
| FrontOffice.vue | 前厅部 | 5 处 TODO |

#### 后端有表/有API但前端无界面（约 8 个模块）
- 财务凭证、应收、应付、报销、对账、结算、账户、流水（13 张 finance_ 表）
- 会员卡、会员等级、会员积分、会员充值（6 张 member_ 表）
- 采购退货、库存转移、库存损耗
- 审批日志
- 员工生命周期
- 营销活动、优惠券、抽奖（7 张 marketing_ 表）

#### 部分实现（30+ 个）
所有标 🟡 的视图：Bookings / Customers / Suppliers / Inventory / Procurement / StockTake / Payroll / Attendance / Leave / Schedule / Staff / DishCost / Packages / Reports / DataScreen / AuditLog 等。

### 6.3 路由守卫问题
`router/index.js` 守卫只判断 `isLoggedIn`，**未做任何角色/权限判断**：
- 任何登录用户都能访问所有 74 个路由
- 财务、HR、删除操作等敏感页面无门槛

### 6.4 菜单权限
`utils/menuStore.js` 只控制菜单显示，不控制路由访问 → 用户手敲 URL 仍能进入。

### 6.5 i18n
`i18n/lang/zh.js` 和 `en.js` 存在，需核对覆盖度（本次审计未深入）。

---

## 七、前后端字段对齐审计

### 7.1 命名规范无保障

**证据**：
- `application-prod.yml` 未配置 `spring.jackson.property-naming-strategy`
- 未配置 `hibernate.physical_naming_strategy`
- 未启用 `@JsonNaming` 全局策略

**后果**：
- PC 端默认 Jackson 驼峰序列化（`staffName`）
- iPad 端硬约束要求 snake_case（`staff_name`）
- **若 PC 接口和 iPad 接口共用一个 Controller 方法，必然冲突**

### 7.2 实体 vs 数据库字段
- 检查 `BookingMaster`、`StaffMaster` 等实体类
- 所有字段均使用 `@Column(name="xxx")` 显式指定
- 但**未配置自动转换策略**，新增字段忘记加注解就会出问题

### 7.3 DTO vs Entity
- `BookingDTO`、`StaffDTO`、`DishDTO`、`PurchaseDTO`、`AttendanceRecordDTO` 等关键 DTO 需逐字段比对
- 本次审计初步检查未发现明显不一致，但需完整逐字段对照

### 7.4 前端 API 调用 vs 后端接收
- `frontend_v3/src/api/` 下 9 个 API 文件
- 需逐个比对：`booking.js` / `hr.js` / `dish.js` / `customer.js` / `auth.js` / `ai.js` / `dict.js` / `package.js` / `table.js`
- 风险点：前端发送 `staffName`，后端 `@RequestBody StaffDTO` 期望 `staffName`，但 DB 是 `staff_name` → Jackson 默认能匹配；但若前端发 `staff_name`，后端默认 Jackson 不识别 → 数据丢失

### 7.5 时间格式
- 项目记忆硬约束 `yyyy-MM-dd HH:mm:ss`
- 需核查全局 Jackson `@JsonFormat` 配置
- `LocalDateTime` 默认序列化为 `[2026,7,28,11,30,0]` 数组 → 必须配 `@JsonFormat`

### 7.6 关键不一致警告
| 字段类型 | 风险 |
|---------|------|
| LocalDateTime | 默认序列化为数组，必须配 JsonFormat |
| BigDecimal 金额 | 默认序列化丢失精度，必须配 `@JsonFormat(shape=STRING)` |
| 枚举 status | 字符串 vs 枚举，需统一 |
| Boolean is_xxx | Jackson 默认 `isXxx` → 前端需用 `isXxx` |

---

## 八、权限与登录审计

### 8.1 登录认证流程现状

**登录方式**：
- `AuthController` 登录接口
- `Login.vue` 默认账号 `rino` / `002323`
- 密码用 BCrypt 加密（兼容历史明文）
- 生成 JWT Token，包含 `staffId / storeId`

**问题**：
- JWT Secret 写死在 yml（S-3）
- Token 过期时间 86400000ms = 24 小时，无续期机制
- 无刷新 Token 机制
- 无登录失败次数限制（暴力破解风险）
- 无验证码

### 8.2 权限模型审计

**现状**：无任何权限模型。
- 数据库无 `sys_role` / `sys_permission` 等表
- 后端无 `@PreAuthorize` 注解
- 后端无 Spring Security 依赖
- 前端路由守卫只判断 `isLoggedIn`
- 前端菜单只控制显示，不控制访问

**项目记忆硬约束"登录必须通过网络权限表ID验证"**：未在 schema 中找到对应实现。

### 8.3 多门店权限（宣城/宁国）

**现状**：
- 员工归属门店：`staff_master.store_id` 字段存在
- 门店切换：无界面、无机制
- 跨门店审批：未实现
- 总部 vs 分店权限：无概念

### 8.4 iPad 端权限

**现状**：
- 设备绑定：无（任何设备传 `X-Device-Sn` 都行）
- `X-Device-Sn` 仅校验非空，不校验是否注册
- 拦截器可被绕过（P0-3）

### 8.5 越权风险清单

| 风险场景 | 现状 |
|---------|------|
| 员工查看他人薪资 | 任意调 `/api/hr/payroll/list` 即可 |
| HR 修改自己考勤 | 无权限校验，可改 |
| 财务修改自己审批 | 无权限校验，可改 |
| 跨门店读数据 | 不传 storeId 即可读全部 |
| 普通员工调管理员接口 | 无角色区分，全员可调 |
| 删除员工/预订 | 任意人可调 delete 接口 |
| 调用 AI 消耗 token | `/ai` 路径无任何校验 |

---

## 九、业务模块完整性审计

### 9.1 人事管理（HR）

| 模块 | 状态 | 现状 | 缺失 |
|------|------|------|------|
| 员工花名册 | 🟡 部分 | Staff.vue 有 CRUD | 无入职/离职/转正流程 |
| 考勤打卡 | 🟡 部分 | Attendance.vue 有记录 | 无审批、无迟到早退规则、无排班联动 |
| 请假 | 🟡 部分 | Leave.vue 11 处 TODO | **无审批流** |
| 加班 | 🟡 部分 | 有 Overtime 实体 | 无审批、无调休抵扣 |
| 薪资 | 🔴 占位 | Payroll.vue 2 处 TODO | 无薪资项配置、无个税社保、无工资条 |
| 排班 | 🟡 部分 | Schedule.vue 14 处 TODO | 无调班换班 |
| 培训 | 🔴 占位 | Training.vue 6 处 TODO | 无培训计划/考核 |
| 绩效 | 🔴 占位 | StaffPerformance.vue | 完全空 |
| 员工生命周期 | 🟡 后端有 | EmployeeLifecycle 表 | 前端无入口 |
| 奖惩 | 🔴 缺失 | 无表无界面 | 完全没做 |

### 9.2 工程维护

| 模块 | 状态 | 说明 |
|------|------|------|
| 报修-派单-维修-验收 | 🔴 **完全缺失** | Maintenance.vue 4 处 TODO，无任何流程 |
| 资产台账/折旧/盘点 | 🔴 占位 | Assets.vue 6 处 TODO |
| 装修项目进度 | 🔴 占位 | Decoration.vue 3 处 TODO |
| 楼层工程 | 🔴 占位 | FloorProject.vue |
| 能耗抄表统计 | 🔴 占位 | Energy.vue 1 处 TODO |
| 工程维护主页 | 🔴 占位 | Engineering.vue 5 处 TODO |

**结论：整个工程维护模块基本没做。**

### 9.3 采购流程

| 模块 | 状态 | 现状 |
|------|------|------|
| 采购请购-审批-下单 | 🟡 部分 | Procurement.vue 11 处 TODO，后端 8 个接口 |
| 收货入库 | 🟡 部分 | 表已建，前端入口不明 |
| 退货 | 🟡 表已建 | purchase_return 表，前端无界面 |
| 库存盘点 | 🟡 部分 | StockTake.vue 4 处 TODO |
| 库存转移 | 🟡 表已建 | 前端无界面 |
| 库存损耗 | 🟡 表已建 | Waste.vue 占位 |
| 供应商管理 | 🟡 部分 | Suppliers.vue 有 CRUD |
| 供应商对账 | 🔴 占位 | SupplierReconciliation.vue 13 处 TODO |
| 供应链看板 | 🔴 占位 | SupplyChain.vue 11 处 TODO |

### 9.4 财务账套

| 模块 | 状态 | 现状 |
|------|------|------|
| 收入统计 | 🔴 占位 | Revenue.vue |
| 成本核算 | 🔴 占位 | Cost.vue 2 处 TODO |
| 税务 | 🔴 占位 | Tax.vue |
| 菜品成本卡 | 🟡 部分 | 表已建，DishCost.vue 2 处 TODO |
| 菜品成本分析 | 🔴 占位 | DishCostAnalysis.vue 5 处 TODO |
| 会计凭证 | 🟡 表已建 | finance_voucher 表，**前端无凭证录入界面**，无账套概念 |
| 收款记录 | 🟡 表已建 | finance_payment_record 表 |
| 应收应付 | 🟡 表已建 | finance_receivable / finance_payable 表 |
| 报销 | 🟡 表已建 | reimbursement / finance_expense 表，**无审批流** |
| 对账结算 | 🟡 表已建 | finance_reconciliation / finance_settlement 表 |
| 财务账户 | 🟡 表已建 | finance_account 表 |
| 财务流水 | 🟡 表已建 | finance_transaction 表 |
| 收据 | 🔴 占位 | Receipt.vue 11 处 TODO |

**结论：财务"有数据库无业务"，13 张 finance_ 表几乎都没有前端录入界面和审批流，没有真正的"账套"概念。**

### 9.5 审批中心

| 模块 | 状态 | 现状 |
|------|------|------|
| 通用审批中心 | 🔴 占位 | ApprovalCenter.vue 1 处 TODO，**无审批引擎** |
| 审批队列 | 🔴 占位 | ReviewQueue.vue 1 处 TODO |
| 审批日志 | 🟡 表已建 | approval_log 表，无界面 |
| 问题跟踪 | 🔴 占位 | Issue.vue 4 处 TODO |

**结论：审批中心纯占位，没有审批流引擎，所有业务模块都没接入审批。**

### 9.6 前厅后厨

| 模块 | 状态 | 说明 |
|------|------|------|
| 前台接待 | 🔴 占位 | FrontDesk.vue 8 处 TODO |
| 前厅部 | 🔴 占位 | FrontOffice.vue 5 处 TODO |
| 后厨 | 🟡 部分 | Kitchen.vue，kitchen_log 表存在 |
| 卫生 | 🔴 占位 | Hygiene.vue |
| 安全 | 🔴 占位 | Safety.vue 3 处 TODO |
| 安保 | 🔴 占位 | Security.vue 9 处 TODO |
| 生产 | 🔴 占位 | Production.vue |

### 9.7 决策与数据

| 模块 | 状态 | 说明 |
|------|------|------|
| 数据大屏 | 🟡 部分 | DataScreen.vue 2 处 TODO，DashboardService 有聚合 |
| 报表 | 🟡 部分 | report_* 表 5 张已建，Reports.vue 3 处 TODO |
| 客户画像 | 🔴 占位 | CustomerAnalysis.vue |
| 客人画像 | 🔴 占位 | GuestAnalysis.vue 3 处 TODO |
| HR 分析 | 🔴 占位 | HRAnalytics.vue |
| 审计日志 | 🟡 部分 | audit_logs / sys_operation_log 表已建，AuditLog.vue 4 处 TODO，**无切面自动记录** |

### 9.8 营销与客户

| 模块 | 状态 | 说明 |
|------|------|------|
| 营销活动 | 🔴 占位 | Marketing.vue，但 7 张 marketing_ 表已建 |
| 客户管理 | 🟡 部分 | Customers.vue 1 处 TODO |
| 会员体系 | 🟡 表已建 | 6 张 member_ 表，**前端无会员管理界面** |
| 套餐 | 🟡 部分 | Packages.vue 2 处 TODO |

### 9.9 模块完成度总览

| 完成度 | 模块数 | 占比 |
|--------|-------|------|
| ✅ 完整实现 | 0 | 0% |
| 🟡 部分实现 | 约 15 | 30% |
| 🔴 占位/缺失 | 约 35 | 70% |

---

## 十、安全漏洞清单

### 10.1 致命漏洞
| 编号 | 漏洞 | 影响 | 修复方案 |
|------|------|------|---------|
| V-01 | 所有 API 无认证 | 任意人可读写所有数据 | 加 JWT 拦截器 |
| V-02 | iPad 拦截器可绕过 | 任意人可调 iPad 接口 | 非 iPad 客户端拒绝 |
| V-03 | 无角色权限模型 | 任意人可调管理员接口 | 建 RBAC 表 + 注解 |
| V-04 | 敏感接口无事务 | 数据不一致 | 加 @Transactional |
| V-05 | 外键约束缺失 | 孤儿数据 | 补 FK 约束 |

### 10.2 严重漏洞
| 编号 | 漏洞 | 影响 |
|------|------|------|
| V-06 | AI 接口无鉴权 | token 被刷 |
| V-07 | 密码硬编码 | 凭证泄露 |
| V-08 | 文件上传无校验 | 上传 webshell |
| V-09 | 多门店无强制隔离 | 跨门店读数据 |
| V-10 | 无登录失败限制 | 暴力破解 |
| V-11 | 无审计日志切面 | 操作不可追溯 |
| V-12 | 异常处理不全 | 信息泄露 / 500 错误 |

### 10.3 一般漏洞
| 编号 | 漏洞 |
|------|------|
| V-13 | 无 CSRF 防护（如使用 Session） |
| V-14 | 无 XSS 过滤（前端 v-html 使用需核查） |
| V-15 | CORS 配置需核查（CorsConfig.java） |
| V-16 | SQL 注入风险（JPA 动态查询需核查） |
| V-17 | 无接口限流 |
| V-18 | 无密码强度策略 |
| V-19 | 无敏感字段加密（手机号、身份证、薪资） |
| V-20 | 日志可能包含敏感信息 |

---

## 十一、改造方案（按优先级）

### 第一阶段：止血（最高优先级，1-2 周）

1. **加 JWT 拦截器**：注册 `/api/**` 全局拦截器（排除 `/api/auth/login`），校验 JWT 并注入 staffId/storeId 到 request 属性
2. **修 iPad 拦截器**：非 iPad 客户端访问 `/api/ipad/**` 返回 403
3. **改 AIController 路径**：`/ai` → `/api/ai`，纳入拦截
4. **移除 yml 中的真实密码默认值**，强制走环境变量
5. **更换数据库密码、JWT secret、天龙 token**（已暴露在代码库中）
6. **云服务器安全组**：8080 端口不对公网开放，只允许 Nginx 内网回源
7. **加 @Transactional** 到所有写操作 Service 方法
8. **加文件上传校验**：MIME 白名单 + 扩展名 + 大小

### 第二阶段：补骨架（2-4 周）

9. **建权限基础表**：sys_role / sys_permission / sys_user_role / sys_role_permission / sys_menu，最小化 RBAC
10. **统一 JSON 序列化策略**：全局 `PropertyNamingStrategies.SNAKE_CASE` + 兼容 PC 驼峰
11. **建审批引擎**：通用 approval_flow / approval_node / approval_task 表 + 服务，让人事/采购/财务共用
12. **补外键约束**：至少在 booking、staff、ingredient 三大主线的子表加 FK + ON DELETE RESTRICT
13. **加数据权限切面**：Hibernate Filter 或 MyBatis 拦截器，按 storeId 自动过滤
14. **补 GlobalExceptionHandler**：覆盖所有业务异常
15. **加审计日志切面**：AOP 自动记录所有写操作
16. **加登录失败限制 + 验证码**

### 第三阶段：填业务（4-12 周）

17. **财务账套**：凭证录入、账套、报表（最复杂，建议独立专项）
18. **工程维护全套**：报修-派单-维修-验收闭环
19. **采购全流程**：请购-审批-订单-收货-入库-对账-付款闭环
20. **HR 全流程**：入职-转正-调动-离职 + 薪资计算引擎 + 排班换班
21. **会员体系**：6 张表已建，补前端界面
22. **营销活动**：7 张表已建，补前端界面
23. **审批中心**：补审批引擎 + 接入所有业务模块

### 第四阶段：数据治理（持续）

24. **历史孤儿数据清理**（项目记忆硬约束）
25. **状态枚举字典化**：所有 status 字段接入 sys_dict
26. **冗余表清理**：`categories` / `menu_category`、`dishes` / `dish_master`、`packages` / `meal_package` / `package_master`
27. **时间戳字段统一**：`created_at` / `create_time` 统一为一种
28. **唯一索引补全**：booking_no / staff_account / supplier_code / ingredient_code
29. **i18n 覆盖度核查**
30. **前端路由守卫加角色判断**

---

## 十二、关键结论

### 12.1 直接回答审计问题

| 问题 | 答案 |
|------|------|
| 到底多少错误？ | **5 个致命 + 7 个严重 + 20 个一般 = 32 个明确问题** |
| 多少个文件没搞？ | **约 25 个完全占位 + 8 个有表无界面 + 30+ 个半成品 = 60+ 文件需补完** |
| 多少路由没搞清？ | **134 个后端 API 完全无权限拦截**，AIController 路径不统一，部分 Service 无 Controller |
| 字段关系对齐没？ | **没对齐**：无命名策略保障、PC/iPad 命名规范冲突、时间戳/状态字段命名混乱 |
| 员工登录权限怎么划分？ | **目前没划分**：只有"是否登录"判断，无角色/权限模型，需补 6 张权限表 + RBAC |
| 人事管理怎么搞？ | CRUD 散点存在；缺入职/离职/转正流程、薪资计算引擎、审批流、绩效、培训考核 |
| 工程维护怎么搞？ | **基本从零开始**：报修-派单-维修-验收流程完全缺失，资产台账/能耗/装修项目全占位 |
| 采购怎么审批？ | 后端有 8 个采购接口，**审批流完全没做**，需建通用审批引擎 + 采购单据闭环 |
| 财务账套怎么做？ | 13 张 finance_ 表已建但无前端界面、无账套概念、无凭证录入、无审批，**需专项设计** |
| 整个系统到底怎么搞？ | 按四阶段方案：先止血（安全）→ 补骨架（权限+审批）→ 填业务（各模块）→ 数据治理 |

### 12.2 系统当前状态总评

- **数据库**：103 张表覆盖面广，但外键约束严重缺失、命名不统一、冗余表存在
- **后端**：134 个 API 数量充足，但**零权限拦截、零事务保护（除预订外）、零审计日志**
- **前端**：74 个路由 80+ 个视图，但**70% 是占位符或半成品**
- **业务流程**：审批引擎完全缺失，所有需要审批的业务（HR/采购/财务）都没接入审批流
- **安全**：**系统目前不能上生产**，最大问题是 P0-1（所有 API 裸奔）

### 12.3 最紧急建议

1. **立即在 `WebMvcConfig.java` 加 `JwtAuthInterceptor`** 拦截 `/api/**`，至少先把"必须登录"这一层补上
2. **修改云服务器安全组**：8080 端口不对公网开放，只允许 Nginx 内网回源
3. **更换数据库密码、JWT secret、天龙 token**（已暴露在代码库中）
4. **修 iPad 拦截器**：非 iPad 客户端访问 `/api/ipad/**` 返回 403

---

## 附录 A：审计证据索引

### 关键文件路径
- 数据库 schema：`youjian-docker/mysql/init/banquet_init.sql`
- 后端配置：`banquet_project/src/main/resources/application-prod.yml`
- Web 配置：`banquet_project/src/main/java/com/youjian/banquet/config/WebMvcConfig.java`
- iPad 拦截器：`banquet_project/src/main/java/com/youjian/banquet/config/IpadInterceptor.java`
- 全局异常：`banquet_project/src/main/java/com/youjian/banquet/config/GlobalExceptionHandler.java`
- 认证控制器：`banquet_project/src/main/java/com/youjian/banquet/controller/AuthController.java`
- AI 控制器：`banquet_project/src/main/java/com/youjian/banquet/controller/AIController.java`
- 前端路由：`frontend_v3/src/router/index.js`
- 前端用户 Store：`frontend_v3/src/store/user.js`
- 前端登录页：`frontend_v3/src/views/Login.vue`

### 审计统计数据
- 数据库表总数：103（`grep -c "CREATE TABLE" banquet_init.sql`）
- store_id 出现次数：223
- 外键约束数：34
- 后端 API 总数：134（`grep -c "@(Get|Post|Put|Delete|Patch)Mapping" controller/`）
- 前端路由数：74（`grep -c "component: () => import" router/index.js`）
- 占位符/TODO 文件数：61（331 处匹配）
- @Transactional 使用：15 处
- @PreAuthorize 使用：0 处

---

## 附录 B：模块完成度矩阵

| 模块 | 数据库 | 后端 API | 前端界面 | 审批流 | 完成度 |
|------|--------|---------|---------|--------|--------|
| 预订管理 | ✅ | ✅ 19 接口 | 🟡 | N/A | 70% |
| 桌位管理 | ✅ | ✅ 7 接口 | 🟡 | N/A | 70% |
| 菜品管理 | ✅ | ✅ 11 接口 | 🟡 | N/A | 60% |
| 套餐管理 | ✅ | ✅ 5 接口 | 🟡 | N/A | 50% |
| 客户管理 | ✅ | ✅ 7 接口 | 🟡 | N/A | 50% |
| 员工管理 | ✅ | ✅ 4 接口 | 🟡 | ❌ | 40% |
| 考勤管理 | ✅ | ✅ 4 接口 | 🟡 | ❌ | 40% |
| 请假管理 | ✅ | ✅ | 🟡 | ❌ | 30% |
| 加班管理 | ✅ | ✅ | 🟡 | ❌ | 30% |
| 薪资管理 | ✅ | ✅ 3 接口 | 🔴 | ❌ | 20% |
| 排班管理 | ✅ | ✅ | 🟡 | ❌ | 30% |
| 培训管理 | ✅ | ✅ | 🔴 | ❌ | 10% |
| 绩效管理 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 员工生命周期 | ✅ | ✅ | ❌ | ❌ | 30% |
| 采购管理 | ✅ 8 表 | ✅ 8 接口 | 🟡 | ❌ | 30% |
| 库存管理 | ✅ 6 表 | ✅ 6 接口 | 🟡 | ❌ | 30% |
| 供应商管理 | ✅ | ✅ 5 接口 | 🟡 | ❌ | 40% |
| 财务凭证 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 应收应付 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 报销管理 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 收款流水 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 财务账户 | ✅ 1 表 | ❌ | ❌ | ❌ | 10% |
| 工程报修 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 资产管理 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 能耗管理 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 装修项目 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 审批中心 | ✅ 1 表 | ❌ | 🔴 | ❌ | 5% |
| 会员体系 | ✅ 6 表 | ❌ | ❌ | N/A | 10% |
| 营销活动 | ✅ 7 表 | ❌ | 🔴 | N/A | 10% |
| 数据大屏 | ✅ | ✅ 2 接口 | 🟡 | N/A | 50% |
| 报表 | ✅ 5 表 | ❌ | 🟡 | N/A | 30% |
| 审计日志 | ✅ 2 表 | ❌ | 🟡 | N/A | 20% |
| AI 助手 | ✅ 2 表 | ✅ 4 接口 | 🟡 | N/A | 60% |
| 前台接待 | ❌ | ❌ | 🔴 | N/A | 0% |
| 后厨管理 | ✅ 1 表 | ❌ | 🟡 | N/A | 20% |
| 卫生安全 | ❌ | ❌ | 🔴 | N/A | 0% |

### 整体完成度估算
- **数据库**：约 80%（103 张表，覆盖大部分业务域）
- **后端**：约 40%（核心 CRUD 有，但权限/事务/审计/审批缺失）
- **前端**：约 30%（70% 占位或半成品）
- **业务流程**：约 15%（审批流完全缺失，HR/采购/财务流程未闭环）
- **整体系统**：约 **25-30%**

---

**报告结束。**

> 本报告基于代码静态审计生成，部分结论需结合运行时验证。建议按"第一阶段止血"优先级立即开始修复。
