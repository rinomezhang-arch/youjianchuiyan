# 又见炊烟餐饮管理系统 · 全栈深度审计报告 v2

> **审计范围**：`f:\solo` 全工程（数据库 / 后端 Spring Boot / 前端 Vue 3 / 部署配置）
> **门店范围**：宣城（store_id=2）、宁国（store_id=1）双门店
> **审计方式**：7 路并行 general_purpose_task，每个 agent 实际 Read 文件内容逐行分析
> **审计日期**：2026-07-28
> **本次重点**：合作项目特有问题（代码风格不一致、重复实现、反编译产物、未集成代码）
> **修正项**：v1 报告中"无 JWT 拦截器"结论错误，实际已存在 `JwtAuthInterceptor`

---

## 目录

- [一、系统规模总览（实测数据）](#一系统规模总览实测数据)
- [二、致命级问题 P0（必须立即修复）](#二致命级问题-p0必须立即修复)
- [三、严重级问题 S](#三严重级问题-s)
- [四、合作项目特有问题（核心）](#四合作项目特有问题核心)
- [五、数据库结构审计（逐表实测）](#五数据库结构审计逐表实测)
- [六、后端 Controller 审计（23个逐个读代码）](#六后端-controller-审计23个逐个读代码)
- [七、前端 Vue 审计（82个文件实测分类）](#七前端-vue-审计82个文件实测分类)
- [八、前后端字段对齐审计（逐字段对照）](#八前后端字段对齐审计逐字段对照)
- [九、权限与登录审计（实际代码验证）](#九权限与登录审计实际代码验证)
- [十、业务模块完整性审计（流程闭环验证）](#十业务模块完整性审计流程闭环验证)
- [十一、部署与配置审计](#十一部署与配置审计)
- [十二、改造方案（按优先级）](#十二改造方案按优先级)
- [十三、关键结论](#十三关键结论)

---

## 一、系统规模总览（实测数据）

| 维度 | 实测数量 | 文件位置 |
|------|---------|---------|
| 数据库表（最终生效） | **105 张** | `banquet_init.sql` 4771 行 |
| 后端 Controller | **23 个** | `controller/` |
| 后端 API 接口 | **134+ 个** | 23 个 Controller 内 |
| 前端动态路由 | **74 个** | `router/index.js` |
| 前端视图文件 | **82 个 .vue** | `views/dashboard/` |
| 含反编译产物的 Java 文件 | **17 个**（7 Controller + 10 Service） | 全部带 `Decompiled with CFR 0.152` |
| 外键约束 | **25 处**（含 Stage1 补建 8 处） | 数据库 schema |
| `store_id` 字段 | 96 张表覆盖（92%） | 业务表基本覆盖 |
| `@Transactional` 使用 | **仅 15 处** | 集中在 Booking/Customer/Recipe |
| `@PreAuthorize` 使用 | **0 处** | 全工程零权限注解 |
| 前端 i18n 实际使用 | **0 个文件用 $t()** | 82 个 Vue 全部硬编码中英文 |
| 完整功能 Vue 文件 | **37 个**（45.1%） | — |
| 静态展示 Vue 文件（数据写死） | **20 个**（24.4%） | — |
| 纯占位 Vue 文件 | **4 个**（4.9%） | — |

### 技术栈
- **后端**：Spring Boot 3.2.5 + Java 17 + JPA/Hibernate + MySQL 8.0
- **前端**：Vue 3.4 + Vite 5 + Pinia + Vue Router + Element Plus（部分文件用自定义 SVG）
- **部署**：Docker Compose（mysql / backend / frontend 三容器）
- **AI 服务**：OpenClaw Gateway（天龙）→ DeepSeek/Dashscope

---

## 二、致命级问题 P0（必须立即修复）

### 🔴 P0-1 CustomerController 完全无门店校验（IDOR 越权）

**证据**（实际读取 `CustomerController.java`）：
- `PUT /api/customers/{id}`（行 147-164）：直接 `findById` → `setXxx` → `save`，**无 `UserContext.assertStoreAccess`**
- `DELETE /api/customers/{id}`（行 166-180）：直接 `findById` → `setIsActive(0)` → `save`，**无门店校验**
- 而 `GET /api/customers/{id}` 和 `GET /api/customers/{id}/history` 反而做了 `assertStoreAccess` → **校验逻辑不对称**

**后果**：任意登录用户凭 `customer_id` 即可删除/修改任意门店客户。

### 🔴 P0-2 DictController 全接口无权限校验 + 暴露敏感字段

**证据**（实际读取 `DictController.java` 全文 209 行）：
- 7 个接口全部无任何门店/角色权限校验
- `storeId` 直接从前端取并拼 SQL
- `GET /staff` 暴露员工手机号 `staff_phone`
- `GET /customers` 暴露客户手机号 `customer_phone`、消费金额 `total_amount`
- `POST/PUT/DELETE /items` 可跨门店修改任意门店字典项

### 🔴 P0-3 PayrollController 硬编码验证码 + 反编译产物

**证据**（实际读取 `PayrollController.java`）：
- 文件首行 `/* Decompiled with CFR 0.152. */` → 反编译产物
- `POST /api/hr/payroll/unlock`（行 164-180）：硬编码验证码 `"002323"`，与登录默认密码相同
- 通过后返回 `payroll-<timestamp>` token，**无用户绑定、可预测**
- `POST /api/hr/payroll/lock`（行 182-185）：**只返回 success 不做任何操作**
- 薪资计算（个税、BigDecimal 运算）全部塞在 Controller 里，未抽到 Service

### 🔴 P0-4 明文密码全员 + 前端硬编码凭证

**证据**：
- `banquet_init.sql` 行 3593：20 条 staff_master 种子数据，`staff_password='002323'` 明文
- `banquet_init.sql` 行 4248：`users` 表 `password='admin123'` 明文
- `Login.vue` 行 107-110：`const loginForm = ref({ username: 'rino', password: '002323' })` 真实凭证写死
- `AuthController.java` 行 65-67：BCrypt 与明文双轨兼容，**BCrypt 分支在种子数据下从不命中**
- `StoreDataScopeAspect.java` 行 44：`@Value("${jwt.secret:YJCY-Banquet-2026-Secret-Key-Wo002323}")` 硬编码 JWT secret 默认值，**已入源码**

### 🔴 P0-5 iPad 多表写入无事务 + 跨店越权

**证据**（实际读取 3 个 IpadXxxController）：
- `IpadTableController.openTable`（行 123）：连续写 BookingMaster + BookingTable + 更新 BanquetTable 三表，**无 `@Transactional`**
- `IpadTableController.transferTable`（行 214）：多表更新无事务
- `IpadOrderController.addDish`（行 72）：bookingId 为空时连续创建 BookingMaster + BookingTable + BookingDishDetail 三表，**无事务**
- `IpadOrderController.editDish/removeDish/refundDish/urgentDish`：**仅按 dishBookingId 查找，无门店校验**，iPad 可修改其他门店订单
- `IpadTableController.openTable`：storeId/staffId 为 null 时**从 body 读取**，前端可伪造任意门店/员工开台
- `IpadTableController.transferTable`：storeId 为 null 时**退化为 `findAll().stream().filter(...)` 全表扫描**

### 🔴 P0-6 前端运行时崩溃 Bug（合作项目核心问题）

**证据**：
- `frontend_v3/src/api/table.js` 行 1：`import axios from './http'` —— **`utils/http.js` 文件不存在**！只有 `utils/request.js`
- 这意味着所有用 table.js 的页面（TableBoard 等）会**在 import 阶段就报错**，整个路由崩溃
- 典型合作问题：开发者 A 改了文件名（`http.js` → `request.js`），未通知开发者 B

### 🔴 P0-7 nginx 未代理 /menu-api/ 路径

**证据**（实际读取 `nginx.conf`）：
- 仅配置 `location /api/ { proxy_pass http://backend:8080/api/; }`
- **未配置 `/menu-api/` 代理**
- 但后端有 5 个 Controller 用 `/api/menu-api/xxx` 路径（Ingredient/Inventory/Purchase/Supplier + BookingController 双前缀）
- `vite.config.js` 行 32-35 把 `/menu-api` 代理到 **3001 端口**，但 3001 无服务
- **生产环境所有 `/menu-api/*` 请求会落到 `location /`，被 try_files 当作静态文件返回 index.html**，axios 解析 JSON 失败

---

## 三、严重级问题 S

### 🟠 S-1 后端 7 个 Controller 是反编译产物

**证据**：7 个文件首行带 `/* Decompiled with CFR 0.152. */`：
- `DishController.java`
- `PackageController.java`
- `IngredientController.java`
- `InventoryController.java`
- `PurchaseController.java`
- `SupplierController.java`
- `PayrollController.java`

**特征**：
- 字符串使用 unicode 转义 `"\u83b7\u53d6\u85aa\u916c\u5931\u8d25"`
- 无 Javadoc
- lambda 参数都是 `arg_0`
- **源码已丢失，从 .class 反编译恢复** —— 强烈暗示 Git 历史中断或冲突误删

**后果**：无法在源码层正常维护，注释丢失，逻辑可读性差。

### 🟠 S-2 两套响应封装并存

**证据**：
- `common.Result<T>`（11 个 Controller 用）：Auth/Staff/HR/Attendance/Payroll/Booking/Table/TableBoard/Recipe/Customer/Dict/Upload/AI/Ipad×3
- `config.ApiResponse<T>`（7 个反编译 Controller 用）：Dish/Package/Ingredient/Inventory/Purchase/Supplier/Dashboard
- 前端拦截器只处理 `code===200`，但两套封装的字段名/语义可能不一致

### 🟠 S-3 BookingService/StaffService/CustomerService 是死代码

**证据**（Grep 验证）：
- `BookingService.java` 存在（反编译，274 行），但 `controller/` 目录 Grep "BookingService" 返回 **0 命中**
- `StaffService.java` 存在，但 StaffController 直接调 `StaffMasterRepository`
- `CustomerService.java` 存在，但 CustomerController 直接调 `CustomerMasterRepository`
- 这 3 个 Service 提供了完整的 `toDTO()` 方法，但无人调用
- 违反项目硬约束"No abandoned/garbage code allowed"

### 🟠 S-4 前端 API 模块双重定义

**证据**（实际读取 `api/booking.js` 182 行）：
- `booking.js` 是 catch-all 文件，包含 13 类 API（Tables/Bookings/Dashboard/Customers/Dishes/Recipes/Packages/Staff/Suppliers/Ingredients/Upload/Inventory/Purchases）
- 同时存在独立的 `customer.js`、`dish.js`、`hr.js`、`package.js`、`table.js`
- 4 类 API（Customers/Dishes/Packages/Staff）有两套实现
- **Staff API 双重定义且 URL 不同**：
  - `booking.js` 调 `/menu-api/staff`（baseURL 清空 → 实际请求 `/menu-api/staff`）
  - `hr.js` 调 `/hr/staff`（baseURL 是 `/api` → 实际请求 `/api/hr/staff`）
  - StaffController 实际注册在 `/api/hr/staff` → **booking.js 的 getStaffList 必然 404**

### 🟠 S-5 路径前缀四套并存

**证据**：
- `/api/`（大多数 Controller）
- `/api/menu-api/xxx`（Ingredient/Inventory/Purchase/Supplier，**双重前缀的怪异路径**）
- `/menu-api/xxx`（BookingController 双前缀之一）
- `/api/ipad/`（iPad 接口）
- 前端 `request.js` 用 `if (url.startsWith('/menu-api')) baseURL=''` 兼容，但容易出错

### 🟠 S-6 storeId 跨实体类型不统一

**证据**：
- `BookingMaster.storeId` 是 **Long**
- `StaffMaster.storeId` 是 **Long**
- `BanquetTable.storeId` 是 **String** ⚠️
- `DishMaster.storeId` 是 **Long**（但 DishDTO 用 String）
- Repository 调用必须 `String.valueOf(storeId)` 兜底，增加 NPE 风险

### 🟠 S-7 9 个 Service 各自实现 toDTO（无 MapStruct）

**证据**（Grep `toDTO`）：
- TableService/SupplierService/StaffService/PurchaseService/PackageService/CustomerService/IngredientService/InventoryService/DishService/BookingService 各自实现 private toDTO 方法
- 无 MapStruct、无共用转换工具类
- 每个 Service 复制粘贴模式

### 🟠 S-8 9 个 Vue 各自实现 formatDate/formatTime

**证据**（Grep `formatDate|formatTime`）：
- ChangeLogView/AuditLog/ExportPanel/DishCost/Issue/Procurement/ReviewQueue/SupplierReconciliation/Receipt 9 个文件各自定义
- 部分实现还各自不同（`String(d).slice(0, 10)` vs `String(d).split('T')[0]`）
- 无共用 `utils/format.js`

### 🟠 S-9 CORS 配置三层冲突

**证据**：
- 全局 `CorsConfig.java`：白名单 `http://localhost:5173,http://localhost:80,...`，`setAllowCredentials(true)`
- 但各 Controller 重复加 `@CrossOrigin(origins = "*")` 或 `@CrossOrigin`（无参数）
- Spring 中方法级 `@CrossOrigin` 优先级高于全局 CorsFilter，导致白名单被绕过
- `allowCredentials=true` + `addAllowedOriginPattern("*")` 实际等同于全开

### 🟠 S-10 AIController 无超时配置

**证据**（实际读取 `AIController.java`）：
- 行 60-66：`@Value("${tianlong.timeout:60000}") int timeout` **定义但从未使用**
- `private final RestTemplate restTemplate = new RestTemplate();` 无超时配置
- 上游 AI 网关卡死会拖垮本服务线程池
- token 配置缺失时用空字符串调用上游
- 无 token 用量统计（日志只记录 content 文本）
- 弱降级（无模型回退、无缓存、无重试）

### 🟠 S-11 外键约束仍缺失 18+ 处

**证据**（实际统计）：
- 25 处已建外键（含 Stage1 补建 8 处）
- 但仍有 18+ 处缺失，包括：
  - `booking_dish_detail.dish_id` → `dish_master.dish_id`
  - `booking_master.package_id` → `package_master.package_id`
  - `purchase_order_detail.purchase_order_id` → `purchase_order.id`
  - `purchase_receipt_detail.purchase_receipt_id` → `purchase_receipt.id`
  - `stock_loss_detail.stock_loss_id` → `stock_loss.id`
  - `finance_voucher_detail.voucher_id` → `finance_voucher.id`
  - `staff_master.leader_id` → `staff_master.staff_id`（自引用）
  - `approval_node.flow_id` → `approval_flow.id`
  - 等

---

## 四、合作项目特有问题（核心）

### 4.1 三种代码风格并存

#### 风格 A：BookingController（业务老手型，979 行）
- 类注解：仅 `@RestController` + `@RequestMapping({"/api/bookings", "/menu-api/bookings"})`，**无 @CrossOrigin**
- 方法命名：`list/create/update/delete/copyBooking/swapBooking`（动词直述）
- 返回类型：**混用** `Result<T>` / `ResponseEntity<Result<T>>` / `ResponseEntity<?>` / `Result<?>`
- 异常处理：`try-catch + e.printStackTrace() + TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`
- @Transactional 位置：**Controller 方法上**
- 参数接收：**全部用 `Map<String, Object>` 手动解析**，且每个字段都做 `snake_case/camelCase` 双兼容（极其冗长）
- 业务逻辑：**全部写在 Controller 里**，无 Service 调用
- 日志：`System.out.println("=== upsertCustomer error: ...")`

#### 风格 B：StaffController / CustomerController / HRController（实体派）
- 类注解：`@RestController + @RequestMapping("/api/...") + @CrossOrigin(origins = "*")`
- 方法命名：`getAllStaff / createStaff / updateStaff / deleteStaff`（getXxx/createXxx 风格）
- 返回类型：统一 `Result<T>` / `Result<?>`
- 异常处理：`try-catch + SecurityException + Exception 二级 catch`
- @Transactional 位置：**Controller 方法上**（仅 CustomerController）
- 参数接收：`@RequestBody 实体`（StaffMaster / CustomerMaster / LeaveRecord）
- 业务逻辑：依然下沉到 Controller（直接调 Repository + JdbcTemplate）

#### 风格 C：DishController / PurchaseController（DTO+Service 派，反编译产物）
- 文件首行 `/* Decompiled with CFR 0.152. */`
- 类注解：`@RestController + @RequestMapping(value={"/api/..."}) + @CrossOrigin`（无参数）
- 方法命名：`getAllDishes / createDish`（getXxx/createXxx 风格）
- 返回类型：**`ApiResponse<T>`**，与 Result 完全不同
- 异常处理：`throw new IllegalArgumentException / UserContext.assertStoreAccess`（依赖全局异常处理器）
- @Transactional 位置：**Service 层**
- 参数接收：`@RequestBody DTO`（DishDTO / PurchaseDTO，规范用法）
- 业务逻辑：通过 Service 层调用，Controller 极简洁

### 4.2 前端两种 UI 风格并存

#### 风格 X：Bookings/Finance/HRAdmin（手写派）
- 全部用 **自定义 SVG 图标**（内联 `<svg>...`）
- 全部用 **自定义 CSS class**（page-header / stat-card / toolbar 等）
- Finance.vue **完全没有 API 调用**！数据全部硬编码

#### 风格 Y：Staff/Customers/Procurement（Element Plus 派）
- 完整使用 Element Plus：`el-table / el-form / el-dialog / el-button / el-input / el-select / el-tree-select`
- 用 Element Plus 内置图标
- Procurement.vue 引入 `dayjs + pinyin-pro`

### 4.3 重复实现清单

| 重复内容 | 重复次数 | 位置 |
|---------|---------|------|
| 后端 toDTO 方法 | 10 处 | 10 个 Service 各自实现 |
| 后端 `getCurrentStaffId`/`resolveQueryStoreId`/`resolveWriteStoreId` | 8+ 处 | 8 个 Controller 逐字复制 |
| 前端 formatDate/formatTime | 9 处 | 9 个 Vue 各自定义 |
| 前端 API 双重定义 | 4 类 API | booking.js catch-all vs customer.js/dish.js/hr.js/package.js |
| 后端响应封装 | 2 套 | Result vs ApiResponse |
| 后端 @CrossOrigin | 3 种写法 | 无 / `origins="*"` / 无参数 |

### 4.4 未集成代码

| 类型 | 详情 |
|------|------|
| Controller 无前端调用 | AttendanceRecordController 部分接口、iPad 三 Controller（独立模块）|
| Service 无 Controller 调用 | BookingService / StaffService / CustomerService（死代码）|
| Controller 无 Service 层 | 11+ 个 Controller 直接调 Repository，违反三层架构 |
| 数据库表无 API 无界面 | 13 张 finance_ 表、6 张 member_ 表、7 张 marketing_ 表 |
| 前端路由无后端 API | Finance.vue / Revenue.vue / Tax.vue 等 20 个静态展示页 |

### 4.5 依赖管理问题

| 依赖 | 问题 |
|------|------|
| `spring-boot-starter-validation` | 引入但 **0 处使用** `@Valid`，全靠手动 if 校验 |
| `echarts` | 仅 `HRAnalytics.vue` 1 个文件用，其他图表用 SVG 手写 |
| `vue-i18n` | 引入但 **0 个文件用 $t()**，全直接写中英文混合 |
| `xlsx` | 仅 `ExportPanel.vue` 1 个文件用 |
| `dayjs` | **未声明在 package.json**，但 Procurement/SupplyChain/AttendancePrint 直接 import |

---

## 五、数据库结构审计（逐表实测）

### 5.1 表清单（105 张最终生效表）

审计文件：`banquet_init.sql`（4771 行，约 1079KB）

#### 表数量演变
- 原始 dump：102 张表
- Stage1-Task3 DROP 4 张冗余表：`dishes`、`packages`、`meal_package`、`categories`
- Stage3 追加 4 张表：`maintenance_request`、`maintenance_asset`、`energy_record` + 审批流 3 表（IF NOT EXISTS）
- **最终生效：105 张唯一表名**

#### 按业务域分组
- **核心业务**（预订/桌位/菜品）：18 张
- **客户/会员/营销**：14 张
- **套餐**：5 张
- **员工/HR**：9 张
- **采购/库存**：18 张
- **财务**：17 张（含 5 张 report_）
- **审批**：4 张
- **系统**：13 张
- **工程维护**：3 张（Stage3 新增）
- **其他**：4 张

### 5.2 多门店隔离实际验证

#### store_info 种子数据（行 3861）
```sql
INSERT INTO store_info VALUES
(1,'HQ_NINGGUO','又见炊烟（宁国店）','宁国店','normal','flagship', 安徽省宁国市宁城南路88号, ...),
(2,'BR_XUANCHENG','又见炊烟（宣城店）','宣城店','normal','branch', 安徽省宣城市宣州区鳌峰东路66号, ...);
```
- 宁国店 store_id=1（旗舰店），宣城店 store_id=2（分店）✓

#### store_id 字段分布
- 含 store_id 的表：**约 96 张**（92%）
- **未含 store_id 的表**：
  - `menu_category`（行 2485）— 零点分类表无 store_id，分类全门店共享
  - `package_dish_rel`（行 2651）— 已删外键，未补 store_id
  - `pkg_used`（行 2759）— 缺失 store_id
  - `users`（行 4230）— 旧版表
  - `approval_node`（行 177）— 通过 flow_id 间接关联

#### store_id 类型不一致
| 类型 | 数量 | 代表表 |
|------|------|--------|
| `bigint NOT NULL DEFAULT '1'` | 约 70 张 | dish_master、booking_master、staff_master |
| `bigint DEFAULT '1'` (nullable) | 4 张 | attendance、leave_record、orders、schedule |
| `int NOT NULL DEFAULT '1'` | **1 张** | `change_log`（行 629）⚠️ 类型不一致 |
| `bigint NOT NULL DEFAULT '0'` | 2 张 | approval_template、approval_flow（0=全局） |

#### store_id 外键全缺失
**全库无任何表对 `store_info.store_id` 建立外键约束**。所有 store_id 仅靠应用层校验。

### 5.3 外键约束完整清单（25 处）

#### CREATE TABLE 内联外键（17 处）
- `banquet_template_rel` → banquet_type（CASCADE）
- `banquet_template_rel` → banquet_template（CASCADE）
- `booking_master.customer_id` → customer_master（RESTRICT）
- `booking_master.staff_id` → staff_master（RESTRICT）
- `booking_table.booking_master_id` → booking_master（RESTRICT）
- `booking_table.table_id` → table_master（RESTRICT）
- `dish_tag_relation` → dish_master（CASCADE）
- `dish_tag_relation` → dish_tag（CASCADE）
- `dish_usage_relation` → dish_master（CASCADE）
- `dish_usage_relation` → dish_usage（CASCADE）
- `ingredient_purchase.supplier_id` → supplier_master（RESTRICT）
- `staff_master.dept_id` → department（RESTRICT）
- `template_category_rel` → banquet_template（CASCADE）
- `template_category_rel` → menu_category（CASCADE）
- `template_dish_rel` → banquet_template（CASCADE）
- `template_dish_rel` → menu_category（SET NULL）

#### Stage1 补建外键（8 处，行 4576-4604，均 ON DELETE RESTRICT ON UPDATE CASCADE）
- `booking_table.booking_master_id` → booking_master.id（强化策略）
- `booking_dish_detail.booking_id` → booking_master.booking_id
- `attendance_records.staff_id` → staff_master.staff_id
- `leave_record.staff_id` → staff_master.staff_id
- `overtime.staff_id` → staff_master.staff_id
- `dish_recipe.(dish_id,store_id)` → dish_master
- `ingredient_inventory_log.(ingredient_id,store_id)` → ingredient_master
- `ingredient_purchase.(ingredient_id,store_id)` → ingredient_master

#### 仍缺失外键（18+ 处）
- `booking_dish_detail.dish_id` → dish_master
- `booking_master.package_id` → package_master
- `purchase_order_detail.purchase_order_id` → purchase_order
- `purchase_receipt_detail.purchase_receipt_id` → purchase_receipt
- `purchase_return_detail.purchase_return_id` → purchase_return
- `stock_loss_detail.stock_loss_id` → stock_loss
- `stock_take_detail.stock_take_id` → stock_take
- `requisition_detail.requisition_order_id` → requisition_order
- `package_dish_detail.package_id` → package_master
- `finance_voucher_detail.voucher_id` → finance_voucher
- `dish_cost_card_detail.cost_card_id` → dish_cost_card
- `staff_master.leader_id` → staff_master（自引用）
- `marketing_coupon_record.coupon_id` → marketing_coupon
- `member_point_log.member_id` → member_card
- `approval_node.flow_id` → approval_flow
- `maintenance_request.handler_id/reporter_id` → staff_master
- `energy_record.recorder_id` → staff_master

### 5.4 字段命名规范

#### 时间戳字段（Stage1 已统一）
- 行 4389-4541 通过 ALTER TABLE 全库统一改名：`created_at → create_time`、`updated_at → update_time`
- **遗留问题**：`approval_flow.created_time/updated_time`（行 163/4713）使用 `created_time` 而非 `create_time` ⚠️
- 类型混用：`timestamp` / `datetime` / `bigint`（audit_logs.create_time 是 bigint Unix 时间戳）

#### 状态字段命名混乱
| 命名 | 出现表 |
|------|--------|
| `status` | store_info、supplier_master、ingredient_master、booking_master 等 |
| `is_active` | dish_master、customer_master、ingredient_master、menu_category 等 |
| `employment_status` | staff_master |
| `booking_status` | booking_master |
| `payment_status` | booking_master |
| `kitchen_status` | booking_dish_detail |

**问题**：`booking_master` 同时存在 `booking_status`、`payment_status`、`status` 三个状态字段，易混淆。

#### 排序规则不一致
- `utf8mb4_0900_ai_ci`：约 70 张表
- `utf8mb4_unicode_ci`：约 30 张表（approval_*、finance_*、marketing_*、member_*、report_*、stock_*）
- 跨表 JOIN 可能引发排序规则冲突错误

### 5.5 冗余/重复表

#### 已清理（Stage1 已 DROP）
- `dishes` → 迁移到 `dish_master`
- `packages` → 迁移到 `package_master`
- `meal_package` → 迁移到 `package_master`
- `categories` → 迁移到 `dish_category`

#### 仍存在的冗余
- `users`（行 4230，5 字段）vs `admin_users`（行 29，7 字段）vs `staff_master`（行 3513，65 字段）—— 三套用户表
- `dish_category`（带 store_id）vs `menu_category`（不带 store_id）—— 两套分类表
- `package_dish_detail` / `package_details` / `package_dish_rel` —— 三表语义接近

### 5.6 初始数据分析

#### 管理员账号（明文密码高危）
| 表 | 行号 | 账号 | 密码 | 角色 |
|----|------|------|------|------|
| `admin_users` | 48 | admin | `$2a$10$N9qo8uLOick...`（BCrypt） | admin |
| `users` | 4248 | admin | `admin123`（**明文**） | admin |
| `staff_master` | 3593 | rino / 张婧 / ngdz / xcdz 等 20 条 | `002323`（**明文**） | super_admin / store_manager / staff |

#### 门店员工分布严重不均
- store_id=1（宁国店）：19 条员工
- store_id=2（宣城店）：**仅 1 条**（宣城店长 xcdz）
- 宣城店无普通员工数据

#### 预订数据
- 66 条预订，**全部 store_id=1**，无宣城店预订数据
- `customer_id` 大量为 NULL，违反业务约束"客户名+电话必填"

#### 字典数据
- sys_dict 10 条 + sys_dict_item 31 条，覆盖核心业务状态
- 但 15+ 枚举字段（menu_type/role/payment_status/kitchen_status/priority 等）仍硬编码未字典化

#### 审批模板（行 4758-4770）
- 5 条模板，全部 store_id=0（全局通用）
- leave/overtime/stock_loss 由 store_manager 审批
- purchase/expense 由 general_manager 审批

---

## 六、后端 Controller 审计（23个逐个读代码）

### 6.1 Controller 路由清单（实测）

| Controller | BasePath | 接口数 | 反编译 | 事务 | 说明 |
|-----------|----------|-------|--------|------|------|
| AuthController | `/api` | 4 | 否 | 无 | 登录/登出/当前用户/门店列表 |
| StaffController | `/api/hr` | 4 | 否 | **无** | 员工 CRUD |
| HRController | `/api/hr` | 10 | 否 | **无** | HR 综合接口 |
| AttendanceRecordController | `/api/hr/attendance` | 4 | 否 | 依赖 Service | 考勤 |
| PayrollController | `/api/hr/payroll` | 3 | **是** | 无 | 薪资（硬编码验证码） |
| BookingController | `/api/bookings` + `/menu-api/bookings` | 18 | 否 | ✅ 11 处 | 预订（979 行，最长） |
| TableController | `/api` | 6 | 否 | **无** | 桌位 |
| TableBoardController | 无类级前缀 | 1 | 否 | 无 | 桌台看板（**无门店隔离**） |
| DishController | `/api/dishes` | 7 | **是** | 无 | 菜品 |
| RecipeController | `/api` | 4 | 否 | ✅ 2 处 | 配方 |
| PackageController | `/api/packages` | 5 | **是** | 无 | 套餐 |
| CustomerController | `/api` | 7 | 否 | ✅ 3 处 | 客户（**delete/update 无门店校验**） |
| IngredientController | `/api/menu-api/ingredients` | 6 | **是** | 无 | 原料（双重前缀） |
| InventoryController | `/api/menu-api/inventory` | 8 | **是** | 依赖 Service | 库存 |
| PurchaseController | `/api/menu-api/purchases` | 8 | **是** | 无 | 采购 |
| SupplierController | `/api/menu-api/suppliers` | 5 | **是** | 无 | 供应商 |
| DictController | `/api/dict` + `/menu-api/dict` | 7 | 否 | 无 | 字典（**全无权限校验**） |
| UploadController | `/api/upload` | 1 | 否 | 无 | 文件上传 |
| DashboardController | `/api/dashboard` | 2 | 否 | 无 | 看板 |
| AIController | `/api/ai` | 4 | 否 | 无 | AI（无超时配置） |
| IpadDishController | `/api/ipad` | 6 | 否 | 无 | iPad 菜品 |
| IpadOrderController | `/api/ipad` | 7 | 否 | **无** | iPad 订单（**多表无事务**） |
| IpadTableController | `/api/ipad` | 7 | 否 | **无** | iPad 桌位（**多表无事务**） |

### 6.2 权限漏洞清单（按严重程度）

#### 🔴 严重（10 个）
| 漏洞 | 位置 | 影响 |
|------|------|------|
| `/auth/me` 回退到 `X-Staff-Id` 请求头查库 | AuthController L147-175 | 任意登录用户伪造头部即可获取任意员工信息 |
| `/auth/me` 最终 fallback 返回 `storeId=1` 默认门店 | AuthController L177-179 | 未登录也能拿到门店信息 |
| `/api/hr/payroll/unlock` 硬编码验证码 `"002323"` | PayrollController L169 | 任意用户输入 002323 即解锁薪酬模块 |
| DictController 全部 7 个接口无任何门店/角色权限校验 | DictController 全文 | 任意用户可跨店改字典、枚举员工/客户手机号 |
| TableBoardController 无门店隔离，storeId 直接拼 SQL | TableBoardController L17-50 | 店长可查看任意门店桌台看板 |
| `PUT/DELETE /api/customers/{id}` 无门店校验 | CustomerController L147-180 | 任意店长可改/删其他门店客户 |
| IpadOrderController `editDish/removeDish/refundDish/urgentDish` 无门店校验 | IpadOrderController L171-335 | iPad 可跨店修改其他门店订单 |
| IpadTableController `openTable` storeId/staffId 为 null 时从 body 读取 | IpadTableController L131-140 | 伪造 `X-Client-Type: ipad` 头即可冒充任意门店/员工开台 |
| IpadTableController `transferTable` storeId 为 null 时退化为全表扫描 | IpadTableController L262-271 | 跨店转台，可恶意占用其他门店桌台 |
| AuthController 登录支持明文密码 `equals` 比较 | AuthController L67 | 时序攻击风险 |

#### 🟠 高（5 个）
- IpadDishController `getDishCategories`/`getTemplateList` 未按门店过滤
- IpadOrderController `addDish` storeId 为 null 时 fallback 到 1L
- IpadTableController `openTable`/`transferTable` 无 `@Transactional`，三表写入
- IpadOrderController `addDish`/`sendToKitchen` 多表写入无 `@Transactional`
- BookingController 双前缀 `/api/bookings` + `/menu-api/bookings` 暴露

### 6.3 代码风格不一致清单

| # | 不一致项 | 表现 |
|---|---------|------|
| 1 | 统一返回包装类 | `Result<T>` vs `ApiResponse<T>` 两套并存 |
| 2 | 反编译产物混入源码 | 7 个 Controller 是 CFR 反编译产物 |
| 3 | `@CrossOrigin` 不统一 | 有 / `origins="*"` / 无 三种状态 |
| 4 | 类级 `@RequestMapping` 前缀风格 | `/api`、`/api/hr`、`/api/bookings`、`/api/menu-api/ingredients`、无前缀 混用 |
| 5 | storeId 类型不统一 | Long vs String |
| 6 | 是否走 Service 层 | 部分直接 Repository/JdbcTemplate，部分走 Service |
| 7 | 异常处理方式 | try-catch 包到 Result.error vs 异常冒泡 vs setRollbackOnly 三种 |
| 8 | 门店隔离辅助方法重复 | `getCurrentStaffId`/`resolveQueryStoreId`/`resolveWriteStoreId` 在 8+ 个 Controller 中逐字复制 |
| 9 | 入参接收方式 | 强类型 DTO/Entity vs `Map<String,Object>` vs `Object` 兼容 PowerShell 序列化 |
| 10 | 命名风格 | `getById`/`findById`/`detail`/`getCurrentUser`/`list`/`getAllStaff` 混用 |
| 11 | 日志方式 | `System.out.println("=== ...")` vs `log.warn` vs `e.printStackTrace()` |
| 12 | `@Transactional` 使用 | 写操作有的加、有的不加 |
| 13 | 路径双前缀暴露 | Booking/TableBoard/Dict 同时映射 `/api/xxx` 和 `/menu-api/xxx` |
| 14 | 路径 `/api/menu-api/xxx` | 双重前缀的怪异路径（Ingredient/Inventory/Purchase/Supplier） |
| 15 | 注释风格 | 中英混用、Javadoc 与行注释混用 |

---

## 七、前端 Vue 审计（82个文件实测分类）

### 7.1 分类统计

| 分类 | 数量 | 占比 |
|------|------|------|
| ✅ 完整功能（真实 API + CRUD） | **37** | 45.1% |
| 🟡 部分实现（API 不完整或工具组件） | **21** | 25.6% |
| 🔴 纯占位符（BizPageWrapper 包裹，13 行） | **4** | 4.9% |
| ⚪ 静态展示（UI 完整但数据写死，无 API） | **20** | 24.4% |

### 7.2 完整功能文件清单（37 个）

#### 预订/桌位（2 个）
- Bookings.vue（925 行）— 预订列表+弹窗+打印
- TableBoard.vue（1518 行）— 桌台看板核心页，全CRUD+双击预订+拖拽排序

#### 菜单/菜品（7 个）
- Menu.vue / MenuAlacarte.vue / MenuManage.vue / MenuManager.vue / Packages.vue / DishCost.vue / IpadMenu.vue

#### 客户/营销（3 个）
- Customers.vue / MarketingActivity.vue / MemberList.vue

#### HR（11 个）
- Staff / StaffProfile / Schedule / Leave / Attendance / AttendanceCalendar / SelfService / Training / HRAdmin / Security / License

#### 采购/库存（5 个）
- Procurement / Suppliers / SupplierReconciliation / SupplyChain / Receipt

#### 工程（2 个）
- Maintenance.vue（维修工单）/ Assets.vue（资产管理）

#### 后厨（1 个）
- KitchenLog.vue

#### 审批（2 个）
- ApprovalCenter.vue / ReviewQueue.vue

#### 系统（4 个）
- Home.vue / PermManager.vue / AiAssistant.vue / ChangeLogView.vue

### 7.3 纯占位符文件清单（4 个）— 重点

| 文件 | 行数 | 内容 |
|------|------|------|
| TableLayout.vue | 13 | 仅 `<BizPageWrapper title="桌台布局">` |
| Production.vue | 13 | 仅 `<BizPageWrapper title="生产管理">` |
| FloorProject.vue | 13 | 仅 `<BizPageWrapper title="楼面工程">` |
| ArtDesign.vue | 13 | 仅 `<BizPageWrapper title="艺术设计">` |

### 7.4 静态展示文件清单（20 个）— 数据写死，无后端 API

| 文件 | 行数 | 写死数据示例 |
|------|------|-------------|
| Finance.vue | 590 | 营收¥16,800 / 毛利¥11,500 |
| Revenue.vue | 946 | 营收总额/订单数/佣金数据 |
| Tax.vue | 1017 | 票据总数/金额/资金流水 |
| TableUtilization.vue | 555 | 利用率78%/翻台率2.8 |
| Marketing.vue | 492 | 新增会员28/储值¥52,800 |
| GuestAnalysis.vue | 657 | 新客420/回头客540/VIP226 |
| StaffPerformance.vue | 555 | 绩效数据 |
| Engineering.vue | 973 | 12条工单全写死 |
| Energy.vue | 286 | 电4200/水68/气320 |
| Decoration.vue | 222 | 3个装修项目写死 |
| FrontDesk.vue | 1406 | 42预订/¥28,600营收 |
| Kitchen.vue | 546 | 8待制作/2超时/156出品 |
| Waste.vue | 992 | 48次报废/¥28,600 |
| Hygiene.vue | 918 | 合格156/不合格8 |
| Safety.vue | 346 | 5条安全隐患写死 |
| GMOffice.vue | 672 | 3决策/5通知/2审批 |
| Settings.vue | 638 | 角色/设备数据写死 |
| DataScreen.vue | 557 | 营收/热销菜品/预警写死 |
| ReportPrint.vue | 398 | 报表配置/员工列表写死 |
| Placeholder.vue | 13 | 通用占位组件 |

### 7.5 部分实现文件清单（21 个，半成品）

包括：MenuBanquet / MenuFestive / MenuFull / MenuHub / MenuDetail / MenuDisplay / MenuPicker / MenuSoldout / DishCostAnalysis / Cost / CustomerAnalysis / AttendancePrint / HRAnalytics / Payroll / Inventory / StockTake / Issue / FrontOffice / AuditLog / Reports / ExportPanel

### 7.6 前端关键发现

1. **i18n 几乎未使用**：82 个文件中 **0 个使用 `$t()` 模板调用**，vue-i18n 依赖浪费
2. **AuditLog.vue 使用 localStorage** 而非后端 API，审计日志仅存浏览器
3. **MenuDetail.vue 使用本地 menuStore** 而非后端 API
4. **components 仅 6 个**，但 views 有 82 个 —— 组件复用率极低
5. **完整功能集中区域**：HR 模块（11/15=73%）、采购模块（5/8=62.5%）、菜单模块（7/16=43.75%）

---

## 八、前后端字段对齐审计（逐字段对照）

### 8.1 实体 vs 数据库字段

#### ✅ 全部对齐
- BookingMaster、StaffMaster、DishMaster、BanquetTable、TemplateDishRel 等实体
- 所有字段均使用 `@Column(name="xxx")` 显式指定
- 时间戳字段经 Stage1 ALTER 后一致（create_time/update_time）

#### ⚠️ 类型不一致
- `BanquetTable.storeId` 是 **String**，其他实体是 **Long**
- 跨实体 storeId 类型不统一，Repository 调用必须 `String.valueOf(storeId)` 兜底

### 8.2 DTO vs Entity 字段对照

#### BookingDTO vs BookingMaster（严重）
| BookingDTO 字段 | 类型 | BookingMaster 对应 | 状态 |
|---|---|---|---|
| **storeId** | **String** | storeId (**Long**) | 🔴 类型不一致 |
| **customerId** | **String** | customerId (**Integer**) | 🔴 类型不一致 |
| pendingName / pendingPhone | String | **不存在** | 🟡 计算字段，永不填充 |
| sourceType / referrerName / referrerPhone | String | **不存在** | 🟡 同上 |
| staffDept / coordinatorName / coordinatorPhone | String | **不存在** | 🟡 同上 |

#### StaffDTO vs StaffMaster
| StaffDTO 字段 | 类型 | StaffMaster 对应 | 状态 |
|---|---|---|---|
| **staffId** | **String** | staffId (**Integer**) | 🔴 类型转换风险 |
| storeId | String | storeId (Long) | 🔴 类型不一致 |
| phone | String | **staffPhone** | 🟡 字段名不对应 |
| gender | String | **staffGender** | 🟡 字段名不对应 |
| position | String | **staffPosition** | 🟡 字段名不对应 |
| **hireDate** | **String** | hireDate (**LocalDate**) | 🔴 类型不一致 |
| salary | BigDecimal | **monthlySalary** | 🟡 字段名不一致 |
| status | String | **employmentStatus** | 🟡 字段名不一致 |
| notes | String | **remark** | 🟡 字段名不一致 |

#### DishDTO vs DishMaster
| DishDTO 字段 | 类型 | DishMaster 对应 | 状态 |
|---|---|---|---|
| **storeId** | **String** | storeId (**Long**) | 🔴 类型不一致 |
| **category** | String | **dishCategory** | 🟡 字段名不对应 |
| **price** | BigDecimal | **salePrice**（无 price 字段） | 🟡 字段名不对应 |
| unit | String | **不存在** | 🟡 DTO 多余字段 |
| description | String | **不存在** | 🟡 DTO 多余字段 |
| **status** | String | **isActive (Integer)** | 🟡 类型 + 名称均不一致 |
| tags | String | **不存在** | 🟡 DTO 多余字段 |

### 8.3 BookingDTO + BookingService 是死代码

**证据**：
- `controller/` 目录 Grep "BookingService" 返回 **0 命中**
- BookingService.toDTO() 填充了 17+ 字段，但无任何调用方
- BookingController 全部用 `Map<String,Object>` + `BookingMaster` 实体直传
- 违反项目硬约束"No abandoned/garbage code allowed"

### 8.4 前端 API vs 后端 Controller

#### ✅ HR 模块前后端路径完全对齐
- `/hr/staff`、`/hr/departments`、`/hr/attendance`、`/hr/leave`、`/hr/schedule`、`/hr/attendance/record`、`/hr/attendance/summary` 全部命中

#### 🔴 booking.js 中 staff/supplier/inventory/purchase 调用路径错乱
- `getStaffList` 走 `/menu-api/staff`（baseURL 清空 → 实际请求 `/menu-api/staff`）
- 但 StaffController 注册在 `/api/hr/staff`，**该请求会 404**
- 类似问题影响 supplier/inventory/purchase 共 12 个调用点
- **影响**：员工/供应商/库存/采购模块的 PC 端列表加载可能整体失效

### 8.5 时间格式审计

- 项目记忆硬约束：`yyyy-MM-dd HH:mm:ss`
- 实际：`entity/` 目录 Grep `@JsonFormat` 返回 **0 命中**
- JacksonConfig 注册了 LocalDateTime **反序列化器**（输入方向，多格式兼容）
- **未注册序列化器**（输出方向）→ 默认输出 ISO `2026-07-29T15:30:00`，与硬约束不符
- IpadTableController 用 `LocalDate.toString()` / `LocalTime.toString()` → 可能输出纳秒精度

### 8.6 配置审计

| 配置项 | 值 | 评估 |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | none | ✅ 不自动改表 |
| `spring.jackson.property-naming-strategy` | **未配置** | ⚠️ 默认 camelCase |
| `hibernate.physical_naming_strategy` | **未配置** | ⚠️ 靠 @Column 显式指定 |
| `jwt.secret` | `${JWT_SECRET:}`（环境变量） | ✅ 不硬编码 |
| `jwt.expiration` | 86400000 (24h) | ✅ |
| `tianlong.*` | 环境变量注入 | ✅ |
| `cos.*` | 环境变量注入 | ✅ |

**JacksonConfig.java**：
- 注册 `customJavaTimeModule` 覆盖 LocalDateTime 反序列化
- `ClientAwareJacksonConverter` 根据 `X-Client-Type` 在 `writeInternal` 中线程安全地选择 pcMapper（camelCase）或 ipadMapper（snake_case）
- ✅ 并发安全，iPad snake_case 实际有效

---

## 九、权限与登录审计（实际代码验证）

### 9.1 登录认证流程

**登录方式**：
- `POST /api/auth/login`，body 为 `{username, password}`
- `AuthController.java` L38-45

**密码校验**（L59-69）：
- BCrypt 与明文双轨兼容
- 以 `$2a$` / `$2b$` 开头走 `BCryptPasswordEncoder.matches()`
- **否则走明文 `staffPassword.equals(password)`**
- 结合 SQL 种子数据：**所有种子账号密码均为明文 `002323`**，BCrypt 分支从不命中

**JWT Token 生成**（L213-224）：
- payload：subject=username、staffId、storeId、iat、exp
- secret：`@Value("${jwt.secret:}")` 默认空串（fail-secure）
- 过期时间：24 小时
- 算法：HS256
- **未签发 iss/aud**

**前端存储**：
- localStorage：token / storeId / storeName / roles / currentStoreId
- Pinia：token / userInfo / roles / storeId / currentStoreId
- **角色非后端下发**，前端 `mapRoles(role, storeId)` 推导

**请求拦截器**（`request.js`）：
- 所有请求：`Authorization: Bearer <token>`
- iPad 请求：额外注入 `X-Device-Sn / X-Store-Id / X-Staff-Id / X-Client-Type: ipad`，值全部来自 **localStorage**

**Token 刷新**：**不存在**。24h 后强制重登。

**登出**：
- 前端：清空 Pinia 与 localStorage
- 后端：`logout()` **只返回"退出成功"，不做任何事** —— 无 token 黑名单、无服务端失效
- **登出后旧 token 在 24h 内仍完全有效**

### 9.2 拦截器实际验证（修正 v1 错误结论）

**v1 错误**：报告说"无 JWT 拦截器，所有 API 裸奔"
**v2 修正**：**JWT 拦截器确实存在**

`WebMvcConfig.java` L24-38 注册 **2 个**拦截器：
1. `JwtAuthInterceptor`（order=0）：`addPathPatterns("/api/**")`，`excludePathPatterns("/api/auth/login")`
2. `IpadInterceptor`（order=1）：`addPathPatterns("/api/ipad/**")`

**JwtAuthInterceptor 实际逻辑**：
- OPTIONS 放行
- 必须有 `Authorization: Bearer <token>`
- secret 为空时拒绝所有请求并返回 500（fail-secure）
- 校验签名 + exp，将 staffId/storeId 写入 request 属性
- **不校验 role / permission_level / can_view_all_stores** —— 纯身份认证，无权限判定

**IpadInterceptor 实际逻辑**（已修复绕过漏洞）：
- L30-35：必须 `X-Client-Type: ipad`，否则 403
- L42-49：必须四项头部齐全，否则 401
- L51-62：仅 `Long.parseLong` 校验数字格式
- **不校验 device_sn 是否已注册、不校验 staff_id 是否归属 store_id**

**Spring Security**：全项目搜索 `SecurityConfig` / `@EnableWebSecurity` —— **无任何匹配**。完全依赖自研拦截器。

### 9.3 权限模型审计

**现状**：无 RBAC 表。
- 数据库无 `sys_role` / `sys_permission` / `sys_role_user` / `sys_menu` 等表
- 后端无 `@PreAuthorize` 注解
- 前端路由守卫只判断 `isLoggedIn` + `meta.roles`
- 前端菜单只控制显示，不控制访问

**staff_master 表权限字段**（实际存在）：
- `role` varchar(30) — 值如 super_admin/store_manager/dept_head/staff
- `permission_level` int
- `can_manage_kitchen`/`can_manage_sales`/`can_manage_finance`/`can_manage_hr` tinyint
- `can_view_all_stores` tinyint — 决定是否全局数据范围
- `can_edit_system` tinyint

**admin_users 表**：遗留表，含 BCrypt 密码但**无任何 Controller 引用**，是死表。

### 9.4 越权场景实际验证

| 场景 | 实际验证结果 |
|------|------------|
| StaffController.delete | ✅ 有门店隔离（resolveWriteStoreId） |
| PayrollController.getPayroll | ✅ 有角色+门店校验（can_manage_hr） |
| BookingController.update | ✅ 有门店隔离（ensureDataScopeFromStoreId） |
| PurchaseController.approve | ✅ 仅总经理（assertGeneralManager） |
| **CustomerController.delete** | ❌ **无任何门店校验**（IDOR 越权） |
| **CustomerController.update** | ❌ **无任何门店校验**（IDOR 越权） |
| **CustomerController.create** | ❌ 不校验 storeId 归属 |
| **DictController 全部** | ❌ **无任何权限校验** |
| **TableBoardController** | ❌ **无门店隔离** |

### 9.5 iPad 设备绑定实际验证

- iPad 拦截器校验 `X-Device-Sn` 非空，**不查任何 device 表**
- SQL 全库搜 `device_master`/`ipad_device` —— **无任何设备注册表**
- 任意字符串（如 `forged-sn-001`）即通过
- 前端 `ipadRequest.js` `isDeviceBound()`：仅读 localStorage `ipad_device_bound==='true'`
- **整个设备绑定是前端 UI 门禁，无安全价值**

### 9.6 多门店权限实际验证

**storeId 来源链**：
- JWT payload `storeId` → JwtAuthInterceptor 写入 request 属性 `jwt_store_id` → StoreDataScopeAspect 解析 → UserContext.set(staffId, storeId, ...)

**storeId=0 超级管理员判定不一致**（严重）：
- `UserContext.isGeneralManager()` = `isDataScopeAll()` || `storeId==0`
- 但种子数据中 rino/张婧 store_id=1（非 0），靠 `can_view_all_stores=1` 走 `StaffController.resolveQueryStoreId` 放行
- 而 `UserContext.isGeneralManager()` 在写操作里**只认 storeId==0**
- **对 store_id=1 的 super_admin 会误判为非总经理** → `PurchaseController.approvePurchase` L138 会拒绝 rino 审批

### 9.7 权限漏洞总清单

| # | 漏洞 | 位置 | 风险 |
|---|------|------|------|
| 1 | 明文密码全员 | banquet_init.sql L3593 | HIGH |
| 2 | 前端硬编码真实凭证 rino/002323 | Login.vue L108-110 | HIGH |
| 3 | 登出不清 token | AuthController L182-185 | HIGH |
| 4 | CustomerController 越权删除 | CustomerController L166-180 | HIGH |
| 5 | CustomerController 越权修改 | CustomerController L147-164 | HIGH |
| 6 | 硬编码 JWT secret 默认值（已入源码） | StoreDataScopeAspect L44 | HIGH |
| 7 | 店长可改员工 role/permissionLevel | StaffController L111-112 | MEDIUM |
| 8 | Payroll unlock 硬编码口令 002323 | PayrollController L169 | MEDIUM |
| 9 | iPad 设备绑定纯前端、SN 不校验注册 | IpadInterceptor L39 | MEDIUM |
| 10 | 角色/总经理判定不一致 | UserContext L151-157 vs SQL L3593 | MEDIUM |
| 11 | CustomerController.create 不校验 storeId | CustomerController L113-145 | MEDIUM |
| 12 | admin_users 遗留表 | banquet_init.sql L29-48 | LOW |
| 13 | JWT 缺 iss/aud | AuthController L213-224 | LOW |
| 14 | 默认 MySQL 用户名 rino | application-prod.yml L12 | LOW |

---

## 十、业务模块完整性审计（流程闭环验证）

### 10.1 人事管理流程

| 模块 | 闭环度 | 实际代码验证 |
|------|--------|------------|
| 员工入职 | ❌ 完全缺失 | StaffController.createStaff 仅 `staffRepository.save(staff)`，无员工编号生成、无建账号逻辑、无入职登记、无试用期/转正 |
| 员工离职 | ❌ 完全缺失 | StaffController.deleteStaff 只把 employmentStatus 改为 "resigned"，无离职申请/审批/工作交接/账号停用 |
| 考勤打卡 | ❌ 部分实现 | HRController.createAttendance 纯保存，**无迟到/早退判定**、**无打卡接口**（无 clockIn/clockOut） |
| 请假 | ✅ 已实现 | HRController.createLeave → 自动提交审批流 → 审批通过回写 leave.status。**但无销假接口** |
| 加班 | ✅ 已实现 | HRController.createOvertime → 自动提交审批流。**有加班费计算**（PayrollController 按时薪×1.5）。**但无调休转换** |
| 薪资 | ⚠️ 部分实现 | PayrollController.getPayroll 确实实现：考勤汇总+薪资项计算+社保+7级累进个税。**缺失**：无工资条生成（不写库）、无发放记录、unlock 硬编码 |
| 排班 | ❌ 完全缺失 | HRController.createSchedule 纯保存。**无调班/换班/考勤联动** |
| 培训 | ❌ 完全缺失 | 全项目无 Training 相关 Controller / Service / Entity |
| 奖惩 | ❌ 完全缺失 | 无表无界面 |

### 10.2 采购流程

**PurchaseController 8 个接口实际功能**：
1. `GET /api/menu-api/purchases?storeId` — 列表
2. `GET /{purchaseId}` — 详情
3. `GET /status/{status}?storeId` — 按状态查
4. `GET /range?storeId&start&end` — 按日期查
5. `POST /` — 创建采购单，**自动提交审批流**（调 approvalService.submit）
6. `PUT /{purchaseId}` — 更新
7. `POST /{purchaseId}/approve` — 审批通过（走 approvalService.approve）
8. `DELETE /{purchaseId}` — 删除

**已闭环**：请购单 → 审批（总经理） → 状态变更
**未实现**：采购订单生成、收货确认、入库联动（approve 后未调 inventoryService.stockIn）、对账、付款

### 10.3 库存流程

**InventoryController 8 个接口实际功能**：
1. `GET /logs?storeId` — 库存日志列表
2. `GET /logs/{ingredientId}?storeId` — 按原料查日志
3. `GET /logs/range?storeId&startTime&endTime` — 按时间查日志
4. `POST /in` — 入库（更新 currentStock + 写日志）
5. `POST /out` — 出库（校验库存不足抛异常）
6. `GET /alerts?storeId` — 低库存预警
7. `POST /loss` — 报损审批（调 approvalService.submitStockLoss，审批通过后自动 stockOut）
8. `POST /transfer` — 跨门店调拨（仅总经理，事务内 stockOut+stockIn）

**已实现**：出库、入库、调拨、报损（带审批）、低库存预警
**缺失**：**盘点流程**（无 stocktaking 接口）

### 10.4 财务账套

**验证结果**：
- Grep 搜 `FinanceController|VoucherController|ReceivableController|ExpenseController` 在 controller 目录：**0 匹配**
- 全项目无 `FinanceExpense` 实体类
- `finance_expense` 表仅在 `ApprovalService.java:395` 被引用一次，且是 `UPDATE` 语句（审批通过时回写状态）——**但全项目没有任何 INSERT INTO finance_expense**，也没有任何 Controller 提交 flowType="expense" 的审批

| 项目 | 结论 |
|------|------|
| 凭证录入接口 | ❌ 无 |
| 应收应付管理接口 | ❌ 无 |
| 报销审批接口 | ❌ 无（仅 ApprovalService 有死代码 UPDATE） |
| 对账接口 | ❌ 无 |
| 财务报表接口 | ❌ 无 |
| **财务模块 API 数量** | **0** |

**结论：13 张 finance_ 表完全空壳，0 个 API。**

### 10.5 审批流程

**通用审批引擎：✅ 完整实现**
`ApprovalService`（527 行）是真正的通用引擎：
- `submit(flowType, businessId, ...)` — 按模板创建 flow + nodes
- `approve(flowId, comment)` — 节点流转，末节点通过则更新业务单据
- `reject(flowId, comment)` — 驳回终止
- `cancel(flowId)` — 撤销
- 模板支持多节点
- 角色解析：store_manager / general_manager
- 权限校验 `assertCanApprove`：分店单据本店店长或总经理

**接入审批的业务（4 个）**：
| 业务 | 接入点 |
|------|--------|
| 请假 leave | HRController.createLeave L78 |
| 加班 overtime | HRController.createOvertime L194 |
| 采购 purchase | PurchaseController.createPurchase L104 |
| 库存报损 stock_loss | InventoryController.submitStockLoss L157 |
| 报销 expense | ❌ **无 Controller 创建**，仅 ApprovalService 死代码 |

**approval_log 表：❌ 无实际写入**
Grep 全项目 `approval_log|ApprovalLog`：**0 匹配**。表存在但完全无代码使用。审批轨迹实际记录在 `approval_node` 表。

### 10.6 工程维护

**Controller 位置**：`MaintenanceController.java`（498 行）。**无 EngineeringController**。

**实现内容**：
- 报修工单 3 接口：`GET /requests`、`POST /requests`（自动生成 MR+日期+随机数单号）、`PUT /requests/{id}/dispatch`（派单）、`PUT /requests/{id}/complete`（完成）
- 资产管理 3 接口：`GET /assets`、`POST /assets`（仅总经理）、`PUT /assets/{id}/check`（盘点）
- 状态机：pending → dispatched → done
- 门店隔离完整

**缺失**：无计划性维护、无维修工时/费用统计、无设备故障历史查询

### 10.7 预订流程

**BookingController 18 个接口实际功能**：
- 完整 CRUD + 桌台管理 + 菜品管理 + 复制 + 互换
- ✅ 生成订单号：`"BK" + System.currentTimeMillis()`
- ✅ 选桌 + 选菜 + 填客户
- ✅ 客户自动录入：`upsertCustomer` 自动新建/更新 customer_master
- ✅ 桌台状态联动：创建时 occupied，删除时 available

**项目记忆硬约束验证：❌ 未落地**
- **4 必填字段（客户名/电话/日期/桌位）**：create 方法**未做任何必填校验**
- customerName / customerPhone / bookingDate 为 null 时直接 setNull
- bookingDate 缺失时默认 LocalDate.now()
- tables 为空时直接跳过桌台保存
- **三层验证**：代码中找不到
- **双击触发**：代码中找不到

### 10.8 iPad 流程

**三个 Controller 实际功能**：
- IpadDishController：6 接口（菜品分类/列表/详情/搜索/套餐/模板）
- IpadOrderController：7 接口（当前订单/加菜/改菜/删菜/退菜/下发后厨/加急）
- IpadTableController：7 接口（桌台列表/全部/筛选/今日预订/等位/开台/转台）

**snake_case 验证：✅ 确认**
所有响应字段均用 snake_case：`dish_id`、`dish_name`、`table_id`、`booking_id` 等

**ipad_store_id 验证：✅ 确认**
所有 iPad 接口均读 `request.getAttribute("ipad_store_id")`

**流程闭环**：开台 → 加菜 → 下发后厨 → 改菜/退菜/加急 → 转台。**无结账/买单接口**（iPad 端）。

### 10.9 AI 接口

**4 个接口**：
1. `POST /api/ai/banquet/suggest` — 宴会方案建议
2. `POST /api/ai/dish/recommend` — 菜品推荐
3. `POST /api/ai/copy/generate` — 营销文案生成
4. `POST /api/ai/chat` — 通用对话（支持图片，自动切 visionModel）

**天龙网关调用：✅ 确认**
- `callTianlong`：POST 到 `baseUrl + "/v1/chat/completions"`，Bearer token 鉴权
- `callTianlongWithImage`：多模态调用，传 image_url

**限流**：滑动窗口，每用户每分钟 10 次
**日志**：`logAiCall` 写入 `ai_chat_history` 表
**Token 用量统计**：❌ 无（未记录 prompt_tokens / completion_tokens / total_tokens）
**错误降级**：⚠️ 弱降级（无模型回退、无缓存、无重试）

### 10.10 Dashboard 接口

**2 个接口**：
1. `GET /api/dashboard/today?storeId=all` — 今日大屏
2. `GET /api/dashboard/report?storeId&period&startDate&endDate` — 报表

**数据真实性：✅ 真实计算**
所有指标从 Repository 真实查询聚合，**无 mock**。支持 storeId=all 全门店聚合或单店明细。

**问题**：`topDishes` **永远返回空数组**（DashboardService 行 153 `setTopDishes(new ArrayList())`）

### 10.11 文件上传

**UploadController（1 接口）**：
- `POST /api/upload/image`
- 文件大小校验：10MB 上限
- 文件类型校验：必须 `image/` 开头
- 返回：`{filename, url, original_name}`

**CosService 实际代码：✅ 真存 COS**
- 使用 `com.qcloud.cos.COSClient`（腾讯云 SDK）
- `cosClient.putObject(putRequest)` 真实上传
- key 生成：`cosConfig.getPrefix() + UUID.randomUUID()` + 扩展名
- cosClient 为 null 时抛异常，**不会回退到本地存储**

**缺失**：无文件扩展名校验、无文件头魔数校验、无文件名清洗

### 10.12 模块完成度矩阵

| 模块 | 数据库 | 后端 API | 前端界面 | 审批流 | 完成度 |
|------|--------|---------|---------|--------|--------|
| 预订管理 | ✅ | ✅ 18 接口 | ✅ | N/A | 80% |
| 桌位管理 | ✅ | ✅ 6 接口 | ✅ | N/A | 80% |
| 菜品管理 | ✅ | ✅ 7 接口 | ✅ | N/A | 70% |
| 套餐管理 | ✅ | ✅ 5 接口 | ✅ | N/A | 60% |
| 客户管理 | ✅ | ✅ 7 接口 | ✅ | N/A | 60% |
| 员工管理 | ✅ | ✅ 4 接口 | ✅ | ❌ | 40% |
| 考勤管理 | ✅ | ✅ 4 接口 | ✅ | ❌ | 40% |
| 请假管理 | ✅ | ✅ | ✅ | ✅ | 80% |
| 加班管理 | ✅ | ✅ | ✅ | ✅ | 80% |
| 薪资管理 | ✅ | ✅ 3 接口 | 🟡 | ❌ | 30% |
| 排班管理 | ✅ | ✅ | ✅ | ❌ | 30% |
| 培训管理 | ❌ | ❌ | 🔴 | ❌ | 0% |
| 绩效管理 | ❌ | ❌ | ⚪ | ❌ | 0% |
| 员工生命周期 | ✅ | ✅ | ❌ | ❌ | 30% |
| 采购管理 | ✅ 8 表 | ✅ 8 接口 | ✅ | ✅ | 60% |
| 库存管理 | ✅ 6 表 | ✅ 8 接口 | 🟡 | ✅ | 60% |
| 供应商管理 | ✅ | ✅ 5 接口 | ✅ | ❌ | 50% |
| 供应商对账 | ❌ | ✅ | ✅ | ❌ | 40% |
| 财务凭证 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 应收应付 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 报销管理 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 收款流水 | ✅ 2 表 | ❌ | ❌ | ❌ | 10% |
| 财务账户 | ✅ 1 表 | ❌ | ❌ | ❌ | 10% |
| 工程报修 | ✅ 1 表 | ✅ 3 接口 | ✅ | ❌ | 50% |
| 资产管理 | ✅ 1 表 | ✅ 3 接口 | ✅ | ❌ | 50% |
| 能耗管理 | ✅ 1 表 | ❌ | ⚪ | ❌ | 10% |
| 装修项目 | ❌ | ❌ | ⚪ | ❌ | 0% |
| 审批中心 | ✅ 4 表 | ✅ 引擎 | ✅ | ✅ | 70% |
| 会员体系 | ✅ 6 表 | ❌ | ✅ | N/A | 30% |
| 营销活动 | ✅ 7 表 | ❌ | ✅ | N/A | 30% |
| 数据大屏 | ✅ | ✅ 2 接口 | ⚪ | N/A | 40% |
| 报表 | ✅ 5 表 | ❌ | 🟡 | N/A | 20% |
| 审计日志 | ✅ 2 表 | ❌ | 🟡 | N/A | 20% |
| AI 助手 | ✅ 2 表 | ✅ 4 接口 | ✅ | N/A | 70% |
| 前台接待 | ❌ | ❌ | ⚪ | N/A | 0% |
| 后厨管理 | ✅ 1 表 | ❌ | ⚪ | N/A | 10% |
| 卫生安全 | ❌ | ❌ | ⚪ | N/A | 0% |

### 整体完成度估算
- **数据库**：约 85%（105 张表，覆盖大部分业务域）
- **后端**：约 45%（核心 CRUD 有，但权限/事务/审计/审批部分缺失）
- **前端**：约 45%（37 个完整 + 21 个部分，但 20 个静态展示需重做）
- **业务流程**：约 25%（审批引擎已实现但仅 4 业务接入，HR/财务流程未闭环）
- **整体系统**：约 **30-35%**

---

## 十一、部署与配置审计

### 11.1 docker-compose.yml
- 3 个服务：mysql / backend / frontend
- **安全问题**：MySQL 端口 `3306:3306` 直接暴露到宿主机
- backend 用 `expose`（仅容器间可见），frontend 用 `ports: "80:80"`
- healthcheck 仅 MySQL 有，backend/frontend 无健康检查

### 11.2 backend/Dockerfile
- 多阶段构建：`maven:3.9-eclipse-temurin-17` 构建 → `eclipse-temurin:17-jre-alpine` 运行
- `-Xms512m -Xmx1024m` — JVM 堆内存配置合理
- **`-DskipTests`** — 跳过测试构建，CI/CD 完全无质量门禁

### 11.3 nginx.conf（重大配置缺失）
```nginx
location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
}
location /api/ {
    proxy_pass http://backend:8080/api/;
}
# 缺少 /menu-api/ 代理！
```
**重大问题**：
- **未代理 `/menu-api/`** —— 所有 `/menu-api/*` 请求会 404
- 缺少 gzip 压缩
- 缺少静态资源缓存头
- 缺少请求限流
- 缺少安全头（X-Frame-Options / X-Content-Type-Options 等）
- `worker_connections 1024` 偏小

### 11.4 vite.config.js
```js
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true },
  '/menu-api': { target: 'http://localhost:3001', changeOrigin: true }  // 错误！
}
```
- `/menu-api` 代理到 **3001 端口**，但后端只在 **8080**，根本没有 3001 服务

### 11.5 my.cnf
- `innodb_buffer_pool_size=512M` — 对 1G 内存容器偏大，与 JVM 1024M 叠加可能 OOM
- `max_connections=200` — 合理
- `skip-name-resolve` — 良好
- 字符集 `utf8mb4` — 合理

---

## 十二、改造方案（按优先级）

### 第一阶段：止血（最高优先级）

1. **修复 table.js import 错误**：`import axios from './http'` → `from './request'`
2. **修复 nginx.conf**：添加 `location /menu-api/ { proxy_pass http://backend:8080/api/menu-api/; }`
3. **修复 vite.config.js**：`/menu-api` 代理到 8080 而非 3001
4. **修复 booking.js 路由**：`/menu-api/staff` → `/hr/staff` 等 12 个调用点
5. **加 CustomerController 门店校验**：update/delete 加 `UserContext.assertStoreAccess`
6. **加 DictController 权限校验**：所有接口加角色校验 + 门店校验
7. **移除 PayrollController 硬编码验证码**：改为基于角色的真实鉴权
8. **全量密码改 BCrypt**：所有 staff_master 密码改为 BCrypt 哈希
9. **删除 Login.vue 默认凭证**：`username: ''`、`password: ''`
10. **移除 StoreDataScopeAspect.java L44 硬编码 JWT secret 默认值**
11. **加 @Transactional** 到 IpadTableController.openTable/transferTable、IpadOrderController.addDish/sendToKitchen
12. **加 iPad 接口门店校验**：editDish/removeDish/refundDish/urgentDish 加门店过滤
13. **修复 IpadTableController.openTable**：storeId/staffId 为 null 时拒绝，不从 body 读取

### 第二阶段：补骨架

14. **删除死代码**：BookingService / StaffService / CustomerService（无 Controller 调用）
15. **统一响应封装**：全部改为 `Result<T>`，废弃 `ApiResponse<T>`
16. **统一 storeId 类型**：BanquetTable.storeId 改为 Long
17. **抽公共工具**：8 个 Controller 重复的 `resolveQueryStoreId` 等抽到 `UserContext` 或 Aspect
18. **抽公共 toDTO**：引入 MapStruct 或共用转换工具
19. **抽公共 formatDate**：前端建 `utils/format.js`
20. **补 JacksonConfig 序列化器**：固定 LocalDateTime 输出为 `yyyy-MM-dd HH:mm:ss`
21. **补外键约束**：18+ 处缺失外键
22. **补 GlobalExceptionHandler**：覆盖所有业务异常
23. **加审计日志切面**：AOP 自动记录所有写操作
24. **建 RBAC 权限表**：sys_role / sys_permission / sys_user_role / sys_role_permission
25. **统一 CORS 配置**：删除各 Controller 的 @CrossOrigin，只用全局 CorsConfig
26. **统一路径前缀**：废弃 `/api/menu-api/xxx`，全部用 `/api/xxx`
27. **AIController 加超时配置**：RestTemplate 设置 connect/read timeout
28. **AIController 加 token 用量统计**

### 第三阶段：填业务

29. **财务账套**：13 张表 0 API，需专项设计（凭证录入、应收应付、报销、对账、报表）
30. **HR 全流程**：入职/离职/转正流程、薪资计算引擎、排班换班、培训
31. **采购全流程**：收货入库联动、对账付款
32. **库存盘点流程**
33. **20 个静态展示页对接真实 API**：Finance/Revenue/Tax/FrontDesk/Kitchen/Waste/Hygiene/Safety 等
34. **会员体系**：6 张表已有，补后端 API
35. **营销活动**：7 张表已有，补后端 API
36. **iPad 结账/买单接口**
37. **预订 4 必填字段硬约束落地**
38. **Dashboard topDishes 实现**

### 第四阶段：数据治理

39. **历史孤儿数据清理**：booking_master.customer_id 大量 NULL
40. **状态枚举字典化**：15+ 硬编码枚举接入 sys_dict
41. **冗余表清理**：users 表废弃、package_dish_detail/package_details/package_dish_rel 整合
42. **排序规则统一**：全部改为 `utf8mb4_0900_ai_ci`
43. **change_log.store_id 类型统一**：int → bigint
44. **approval_flow.created_time 改名**：→ create_time
45. **i18n 落地**：82 个 Vue 文件接入 $t()
46. **前端路由守卫加角色判断**
47. **依赖清理**：删除未使用的 spring-boot-starter-validation、echarts、vue-i18n（或真正使用）
48. **dayjs 加入 package.json**
49. **MySQL 3306 端口不暴露**
50. **nginx 加 gzip / 缓存 / 安全头 / 限流**

---

## 十三、关键结论

### 13.1 直接回答审计问题

| 问题 | 答案 |
|------|------|
| 到底多少错误？ | **7 个致命 + 11 个严重 + 30+ 一般 = 48+ 个明确问题** |
| 多少个文件没搞？ | **4 个纯占位 + 20 个静态展示（数据写死）+ 21 个半成品 = 45 个文件需补完** |
| 多少路由没搞清？ | **路径前缀四套并存**（/api/、/api/menu-api/、/menu-api/、/api/ipad/），nginx 缺 /menu-api 代理，booking.js 12 个调用点 404 |
| 字段关系对齐没？ | **未完全对齐**：实体 vs DB ✅，但 DTO vs Entity 类型/字段名大量不一致，BookingDTO 是死代码，storeId 跨实体类型不统一 |
| 员工登录权限怎么划分？ | **无 RBAC 表**，靠 staff_master.role + can_manage_* 字段；前端路由守卫只检查 token + meta.roles；后端只做身份认证不做权限判定 |
| 人事管理怎么搞？ | 入职/离职/排班/培训全缺；请假/加班/薪资有部分闭环；考勤无打卡接口 |
| 工程维护怎么搞？ | 报修+资产已实现（3+3 接口），能耗/装修/楼面工程全占位 |
| 采购怎么审批？ | **审批引擎已实现**（通用 ApprovalService），采购已接入审批流，但缺收货入库对账付款联动 |
| 财务账套怎么做？ | **13 张表 0 API**，完全空壳，需专项设计 |
| 整个系统到底怎么搞？ | 按四阶段方案：先止血（13 项）→ 补骨架（15 项）→ 填业务（10 项）→ 数据治理（12 项） |

### 13.2 合作项目特有问题总结

1. **17 个反编译产物混入源码**（7 Controller + 10 Service）—— 强烈暗示 Git 历史中断或冲突误删
2. **三种代码风格并存**（Map派/实体派/DTO派）—— 多人协作无统一约定
3. **两套响应封装并存**（Result vs ApiResponse）—— 两批开发者各做各的
4. **3 个 Service 是死代码**（BookingService/StaffService/CustomerService）—— 重构未完成
5. **前端 API 双重定义**（booking.js catch-all vs 分文件）—— 拆分未完成
6. **table.js 引用不存在的 http.js** —— 改名未同步通知
7. **vite 代理到不存在的 3001 端口** —— 配置覆盖
8. **CORS 三层冲突**（全局白名单 vs @CrossOrigin origins="*" vs 无参数）—— 多人重复配置
9. **9 个 Service 各自实现 toDTO** —— 无共用工具
10. **9 个 Vue 各自实现 formatDate** —— 无共用 utils

### 13.3 系统当前状态总评

- **数据库**：105 张表覆盖面广，Stage1 已清理冗余表和统一时间戳，但外键仍缺 18+ 处，排序规则不统一
- **后端**：JWT 拦截器已存在（修正 v1 错误），但鉴权层严重不均（Booking/Staff/Purchase 规范，Customer/Dict 裸奔，Payroll 硬编码）
- **前端**：37 个完整功能文件（45.1%），但 20 个静态展示页数据写死，i18n 完全未用
- **业务流程**：审批引擎已实现且 4 业务接入，但财务 0 API、HR 入职/离职/排班/培训全缺
- **合作项目**：17 个反编译产物 + 三种代码风格 + 死代码 + 路径前缀混乱 + CORS 冲突，典型的多人协作失控特征
- **安全**：明文密码全员 + 前端硬编码凭证 + 硬编码 JWT secret + 登出不清 token + CustomerController IDOR 越权

### 13.4 最紧急建议（按优先级）

1. **修复 table.js import 错误** —— 否则桌台管理页面整体崩溃
2. **修复 nginx.conf 添加 /menu-api/ 代理** —— 否则采购/库存/原料/供应商模块生产环境 404
3. **修复 vite.config.js 代理到 8080** —— 否则开发环境同样失败
4. **修复 booking.js 12 个调用点路径** —— 否则员工/供应商/库存/采购列表加载失败
5. **加 CustomerController 门店校验** —— 否则任意用户可删/改任意门店客户
6. **加 DictController 权限校验** —— 否则任意用户可枚举员工/客户手机号
7. **全量密码改 BCrypt + 删除 Login.vue 默认凭证**
8. **加 @Transactional 到 iPad 多表写入**

---

## 附录 A：审计证据索引

### 关键文件路径
- 数据库 schema：`youjian-docker/mysql/init/banquet_init.sql`（4771 行）
- 后端配置：`banquet_project/src/main/resources/application-prod.yml`
- Web 配置：`banquet_project/src/main/java/com/youjian/banquet/config/WebMvcConfig.java`
- JWT 拦截器：`banquet_project/src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java`
- iPad 拦截器：`banquet_project/src/main/java/com/youjian/banquet/config/IpadInterceptor.java`
- Jackson 配置：`banquet_project/src/main/java/com/youjian/banquet/config/JacksonConfig.java`
- 全局异常：`banquet_project/src/main/java/com/youjian/banquet/config/GlobalExceptionHandler.java`
- CORS 配置：`banquet_project/src/main/java/com/youjian/banquet/config/CorsConfig.java`
- 数据范围切面：`banquet_project/src/main/java/com/youjian/banquet/config/StoreDataScopeAspect.java`
- 认证控制器：`banquet_project/src/main/java/com/youjian/banquet/controller/AuthController.java`
- AI 控制器：`banquet_project/src/main/java/com/youjian/banquet/controller/AIController.java`
- 预订控制器（979 行）：`banquet_project/src/main/java/com/youjian/banquet/controller/BookingController.java`
- 客户控制器（越权）：`banquet_project/src/main/java/com/youjian/banquet/controller/CustomerController.java`
- 字典控制器（无权限）：`banquet_project/src/main/java/com/youjian/banquet/controller/DictController.java`
- 薪资控制器（反编译）：`banquet_project/src/main/java/com/youjian/banquet/controller/PayrollController.java`
- 前端路由：`frontend_v3/src/router/index.js`
- 前端用户 Store：`frontend_v3/src/store/user.js`
- 前端请求封装：`frontend_v3/src/utils/request.js`
- 前端登录页：`frontend_v3/src/views/Login.vue`
- 前端 API（catch-all）：`frontend_v3/src/api/booking.js`
- 前端 API（import 错误）：`frontend_v3/src/api/table.js`
- vite 配置：`frontend_v3/vite.config.js`
- nginx 配置：`youjian-docker/frontend/nginx.conf`

### 反编译产物清单（17 个）
**Controller（7 个）**：
- DishController.java / PackageController.java / IngredientController.java
- InventoryController.java / PurchaseController.java / SupplierController.java
- PayrollController.java

**Service（10 个）**：
- BookingService.java / StaffService.java / CustomerService.java
- DishService.java / PackageService.java / TableService.java
- SupplierService.java / IngredientService.java / InventoryService.java
- PurchaseService.java（其中 BookingService/StaffService/CustomerService 是死代码）

---

**报告结束。**

> 本报告基于 7 路并行 agent 实际读取全部关键文件内容生成，所有结论附行号引用。修正了 v1 报告中"无 JWT 拦截器"的错误结论。建议按"第一阶段止血"13 项优先级立即开始修复。
