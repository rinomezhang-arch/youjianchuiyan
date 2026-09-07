# 又见炊烟餐饮管理系统 — 项目记忆路由

> **用途**：项目唯一入口索引。任何 Agent 或开发者接手本项目时，首先读取本文件即可掌握全貌。
> **结构**：锚点目录 + 切片章节，支持快速跳转。
> **更新**：每次重大变更、新脚本、新审计、客户约束变更时必须同步更新。

---

## 锚点目录（点击跳转）

- [1. 项目概述](#1-项目概述)
- [2. 技术栈与版本](#2-技术栈与版本)
- [3. 目录结构索引](#3-目录结构索引)
- [4. 凭证与密钥（指针，无明文）](#4-凭证与密钥指针无明文)
- [5. 数据库速查](#5-数据库速查)
- [6. 脚本清单与用途](#6-脚本清单与用途)
- [7. 审计报告索引](#7-审计报告索引)
- [8. 重大变更日志](#8-重大变更日志)
- [9. 客户约束摘要](#9-客户约束摘要)
- [10. API 接口路由图](#10-api-接口路由图)
- [11. 前端页面路由图](#11-前端页面路由图)
- [12. 部署与备份](#12-部署与备份)

---

## 1. 项目概述

| 项 | 值 |
|---|---|
| 项目名 | 又见炊烟餐饮管理系统 v2.0 |
| 类型 | 餐饮宴会连锁管理系统（多门店SaaS架构） |
| 业务模块 | 宴会预订、点菜、厨房出品、采购仓储、财务、人事行政、客户管理、营销、数据大屏 |
| 门店数 | 2家（宁国店 store_id=1，宣城店 store_id=2） |
| 代码仓库 | GitHub `git@github.com:rinomezhang-arch/youjianchuiyan.git` |
| 开发分支 | master（主分支，已对齐远程） |
| 最新 commit | `7599861` 清理 dashboard 目录 |

---

## 2. 技术栈与版本

| 层 | 技术 | 版本 |
|---|---|---|
| 前端框架 | Vue 3 + `<script setup>` | 3.4+ |
| 构建工具 | Vite | 5.x |
| UI 组件库 | Element Plus | — |
| 状态管理 | Pinia | — |
| 路由 | Vue Router 4 | — |
| 国际化 | vue-i18n | — |
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17 |
| 数据库 | MySQL | 8.0 |
| ORM | Spring Data JPA + Hibernate | — |
| 安全 | JWT + AES-256-GCM + RBAC | — |
| 部署 | Docker Compose + Nginx | — |
| 云存储 | 腾讯云 COS (youjian-data-1409286104) | ap-nanjing |

---

## 3. 目录结构索引

```
f:\solo\project\又见炊烟餐饮管理系统2.0\
│
├── PROJECT_MEMORY.md          ← 本文件（项目唯一记忆入口）
├── README.md                  ← 项目说明
├── 数据库设计使用说明书.md     ← 设计文档 + 审计内容整合
│
├── banquet_project/           ← 后端 (Spring Boot)
│   ├── pom.xml                ← Maven 依赖
│   └── src/main/
│       ├── java/com/youjian/banquet/
│       │   ├── BanquetApplication.java    ← 启动类
│       │   ├── aop/                        ← AOP切面（门店隔离+审计日志）
│       │   ├── config/                     ← 配置类（12个：CORS/JWT/加密/限流...）
│       │   ├── controller/                ← REST控制器（40+个）
│       │   ├── dto/                        ← 数据传输对象
│       │   ├── entity/                     ← JPA实体（62个）
│       │   ├── repository/                 ← Spring Data Repositories（60个）
│       │   ├── service/                    ← 业务服务层（25个）
│       │   └── util/                       ← 工具类（5个：AESUtil/DataMaskUtil/UserContext...）
│       └── resources/
│           ├── application-prod.yml       ← 生产配置（密钥全部走环境变量）
│           └── *.sql                       ← 迁移脚本（运行时自动加载）
│
├── frontend_v3/               ← 前端 (Vue 3)
│   ├── package.json           ← npm 依赖
│   ├── index.html             ← HTML入口
│   └── src/
│       ├── main.js / App.vue ← 应用入口
│       ├── api/              ← API请求模块（11个：http/auth/booking/dish...）
│       ├── components/       ← 公共组件（6个：AIChatFloat/BizPageWrapper/PrintPreview...）
│       ├── router/           ← 路由配置（index.js + ipad.js）
│       ├── store/            ← Pinia状态（user.js + ipad.js）
│       ├── views/
│       │   ├── Welcome.vue   ← 欢迎页（未登录）
│       │   ├── Login.vue     ← 登录页
│       │   ├── Dashboard.vue  ← 主框架
│       │   └── dashboard/    ← 业务页面（89个.vue + settings/7个）
│       └── utils/            ← 工具函数（request.js/menuStore.js）
│
├── youjian-docker/            ← Docker 部署
│   ├── docker-compose.yml    ← 编排文件
│   └── mysql/
│       ├── init/
│       │   ├── banquet_init.sql  ← 数据库初始化（113张表）
│       │   └── rbac_init.sql     ← RBAC权限初始化
│       ├── backup_strategy.sh    ← 自动备份脚本
│       └── my.cnf                ← MySQL配置
│
├── scripts/                   ← 项目级脚本（长期保留）
│   ├── migrations/           ← 数据库迁移脚本（10个）
│   ├── seed/                 ← 种子数据脚本（5个）
│   └── backup/               ← 备份脚本（cos_upload.py）
│
├── chat-server/               ← AI聊天服务
│
└── 体检/                      ← 审计报告目录
    └── 体检报告/
        ├── SYSTEM_AUDIT_REPORT_V4.md     ← 初始审计报告
        ├── DEEP_AUDIT_REPORT_V7.md        ← 深度审计
        ├── DEAD_CORNER_AUDIT_V8.md        ← 死角审计
        ├── FINAL_COMPLETION_V9.md         ← 最终完成报告
        ├── ISOLATION_RBAC_CASCADE_V10.md  ← 隔离/RBAC/级联审计
        ├── HIDDEN_RISKS_RESOLVED_V11.md  ← 隐患消除报告
        ├── DATABASE_DESIGN_MANUAL.md      ← 数据库设计手册
        └── vulnerability_report_template.md
```

---

## 4. 凭证与密钥（指针，无明文）

> 本章节**仅记录路径指针**，不存放任何密钥明文。

| 凭证类型 | 存储位置 | 读取方式 |
|---|---|---|
| 腾讯云 COS | `C:\Users\rinom\.cos.conf` | `scripts/backup/cos_upload.py` 自动解析 INI |
| JWT 密钥 | 环境变量 `JWT_SECRET` | `application.yml` 中 `${JWT_SECRET}` |
| AES-256 密钥 | 环境变量 `AES_SECRET_KEY` | `application.yml` 中 `${AES_SECRET_KEY}` |
| 天龙 Token | 环境变量 `TIANLONG_TOKEN` | `application-prod.yml` 中 `${TIANLONG_TOKEN:}` |
| 天龙 BaseURL | 环境变量 `TIANLONG_BASE_URL` | 默认 `http://127.0.0.1:11500` |
| MySQL 密码 | 硬编码 `application.yml` | 开发用；生产走 `MYSQL_PASSWORD` 环境变量 |
| 地龙(OpenClaw)配置 | `C:\Users\rinom\.openclaw\openclaw.json` | 含 deepseek/dashscope key（由地龙守护进程读取） |

**COS 路径约定**：`ai公共工作空间/项目管理/餐饮管理系统/`

**密码本**：`C:\Users\rinom\.openclaw\密码本.md`（含 DeepSeek 等 API Key）

---

## 5. 数据库速查

| 项 | 值 |
|---|---|
| 总表数 | 113 张 |
| 总字段数 | 1816 个 |
| 外键数 | 41 个 |
| 索引数 | 110 个 |
| 字符集 | utf8mb4，排序规则 utf8mb4_0900_ai_ci |
| 存储引擎 | InnoDB |
| 多租户 | `store_id` 字段级隔离 + AOP 切面自动注入 |
| 初始化脚本 | `youjian-docker/mysql/init/banquet_init.sql` |
| RBAC 初始化 | `youjian-docker/mysql/init/rbac_init.sql` |

**表分类**：
- 系统管理（12表）：sys_role, sys_permission, sys_user, sys_user_role, sys_user_permission, sys_dict, sys_dict_item, audit_logs, sys_config, sys_log, sys_notice, sys_job
- 核心业务（28表）：booking_master, booking_dish_detail, booking_table, banquet_template, banquet_template_rel, table_master, dish_master, dish_category, dish_recipe, package_master, package_dish_detail, customer_master, contract, staff_master, department, post, store_info
- 财务（18表）：finance_account, finance_payment_record, finance_receivable, finance_payable, finance_cost_record, finance_transaction, finance_voucher, finance_voucher_detail, finance_expense, finance_reconciliation, finance_settlement, month_salary, salary_template, reward_punish, attendance, attendance_record, leave_record, overtime
- 采购仓储（22表）：ingredient_master, ingredient_purchase, ingredient_inventory_log, goods_receipt, goods_receipt_item, purchase_request, purchase_request_item, material_requisition, material_requisition_item, stock_transfer, stock_transfer_detail, supplier_master, preprocessing_record, inventory_summary, kitchen_log, menu_category, unit_conversion, cost_card, cost_card_detail, approval_flow, approval_node, approval_template
- 其他（33表）

详细表结构和字段见 [数据库设计使用说明书.md](./数据库设计使用说明书.md)

---

## 6. 脚本清单与用途

### 6.1 数据库迁移脚本（scripts/migrations/）

| 脚本 | 用途 | 执行时机 |
|---|---|---|
| `db_fix_v1.sql` | P0修复：booking_id类型转换、时间字段统一timestamp、排序规则修正 | 首次部署前 |
| `db_fix_v2.sql` | P1修复：财务模块外键补充、store_id在detail表添加 | 首次部署前 |
| `db_fix_v3.sql` | P2修复：字段注释补充、默认值修正 | 首次部署前 |
| `dedup_migration_v1.sql` | 去重：删除重复表(dishes/orders→_deprecated)、合并三套认证表→staff_master | 数据迁移时 |
| `finance_migration_v1.sql` | 财务数据灌入：生成3020行财务测试数据 | 部署后 |
| `fix_collation_v1.sql` | 排序规则统一：119张表→utf8mb4_0900_ai_ci | 数据迁移前 |
| `hr_migration_v1.sql` | 人事数据迁移：员工、考勤、排班数据 | 部署后 |
| `post_migration_v1.sql` | 岗位数据迁移 | 部署后 |
| `salary_migration_v1.sql` | 薪资字段独立：31条工资从staff_master→month_salary | 部署后 |
| `stock_transfer_migration_v1.sql` | 库存调拨数据迁移 | 部署后 |

### 6.2 种子数据（scripts/seed/）

| 脚本 | 用途 |
|---|---|
| `banquet_full_seed.sql` | 完整种子数据：菜单/宴会/客户/桌台 |
| `init_real_data_v4.sql` | 真实业务数据初始化 |
| `schema_kitchen.sql` | 厨房模块表结构+数据 |
| `seed_e2e_test_data.sql` | 端对端测试数据 |
| `seed_kitchen_test_data.sql` | 厨房测试数据 |

### 6.3 备份脚本（scripts/backup/）

| 脚本 | 用途 |
|---|---|
| `cos_upload.py` | 通用 COS XML API 上传工具。从 `C:\Users\rinom\.cos.conf` 读取凭证，支持上传任意文件到 COS。用法：`python cos_upload.py <本地文件> <COS对象键>` |

### 6.4 运行时脚本（youjian-docker/mysql/）

| 文件 | 用途 |
|---|---|
| `init/banquet_init.sql` | Docker MySQL 初始化时自动执行（113张表结构） |
| `init/rbac_init.sql` | RBAC 权限数据初始化 |
| `backup_strategy.sh` | 自动备份策略（每日02:00，保留30天） |

---

## 7. 审计报告索引

| 报告 | 路径 | 核心内容 |
|---|---|---|
| V4 初始审计 | `体检/体检报告/SYSTEM_AUDIT_REPORT_V4.md` | 36项数据库问题修复 |
| V7 深度审计 | `体检/体检报告/DEEP_AUDIT_REPORT_V7.md` | 字段注释/关系/虚拟数据/端对端数据流 |
| V8 死角审计 | `体检/体检报告/DEAD_CORNER_AUDIT_V8.md` | 排序规则/重复表/字段类型/孤儿记录 |
| V9 最终完成 | `体检/体检报告/FINAL_COMPLETION_V9.md` | 全量问题闭环验证 |
| V10 隔离/RBAC/级联 | `体检/体检报告/ISOLATION_RBAC_CASCADE_V10.md` | 门店隔离/RBAC权限/父子表级联 |
| V11 隐患消除 | `体检/体检报告/HIDDEN_RISKS_RESOLVED_V11.md` | SQL注入/密钥/脱敏/限流/自动备份 |
| 数据库设计手册 | `体检/体检报告/DATABASE_DESIGN_MANUAL.md` | 113表结构/41外键/索引设计 |

---

## 8. 重大变更日志

| 日期 | 版本 | 变更摘要 |
|---|---|---|
| 2026-08-01 | v9 | 数据库重构：删除旧版SQL脚本，推送113表/41外键/安全加固/脱敏加密/RBAC/门店隔离的新版源数据库到GitHub |
| 2026-08-01 | v9 | dashboard 目录清理：删除8个开发笔记.md + 6个孤儿/重复.vue，保留89个业务页面 |
| 2026-08-01 | v9 | COS 备份机制建立：git bundle + XML API 上传 |
| 2026-08-01 | v9 | 项目记忆体系建立：PROJECT_MEMORY.md + user_profile.md 凭证指针 |
| 2026-08-01 | v8 | 死角审计修复：10个实体类缺失@PrePersist/@PreUpdate补齐，排序规则统一 |
| 2026-08-01 | v6 | P1-15 薪资字段独立：month_salary表+StaffService修改+PayrollController LEFT JOIN |
| 2026-08-01 | v5 | P0致命问题修复：booking_id类型转换、时间字段timestamp统一、36项数据库问题 |
| 2026-08-01 | v4 | 初始系统审计报告生成 |
| 2026-07-31 | — | 银行账号加密：AESUtil+BankAccountConverter+DataEncryptionInitializer |
| 2026-07-31 | — | JPA实体与数据库字段全面对齐，62个实体注解补全 |

---

## 9. 客户约束摘要

> 完整约束见 `.trae-cn/memory/projects/-f-solo--p2-75939eac3438d7bdebdf/project_memory.md`

### UI 设计
- 宽页面横向排列，多行对称
- **不用图标**（文字标签替代 emoji/SVG）
- 弹窗宽度 50vw
- 徽派雅致配色 + 田园风
- 选中元素金边高亮
- 输入框 32px 高度，窄宽度
- 圆角 16px（弹窗），按钮组居中
- 数字控件：减号左、加号右
- 创建时间单行显示

### 技术约束
- 数据库位置：`youjian-docker/mysql/init/`
- 多租户：store_id 字段级隔离
- 前端后端字段统一命名
- 生产密钥全部走环境变量
- 禁止硬编码密码/密钥/Token
- 跨平台 UTF-8 编码一致性

### 工作流
- 调试完成后统一构建部署
- 先调试后推送 GitHub
- 部署在云端（腾讯云）
- COS 路径前缀：`ai公共工作空间/项目管理/餐饮管理系统/`

---

## 10. API 接口路由图

### 后端 Controller → 路由前缀

| Controller | 前缀 | 功能 |
|---|---|---|
| AuthController | `/api/auth` | 登录/登出/刷新 |
| BookingController | `/api/booking` | 宴会预订CRUD |
| TableController | `/api/table` | 桌台管理 |
| DishController | `/api/dish` | 菜品管理 |
| PackageController | `/api/package` | 套餐管理 |
| CustomerController | `/api/customer` | 客户管理 |
| ContractController | `/api/contract` | 合同管理 |
| StaffController | `/api/staff` | 员工管理 |
| DepartmentPostController | `/api/dept` | 部门岗位 |
| FinanceController | `/api/finance` | 财务主入口 |
| FinanceAccountController | `/api/finance/account` | 财务账户 |
| FinanceExpenseController | `/api/finance/expense` | 财务支出 |
| FinancePayableController | `/api/finance/payable` | 应付账款 |
| FinanceReportController | `/api/finance/report` | 财务报表 |
| FinanceVoucherController | `/api/finance/voucher` | 凭证管理 |
| HRController | `/api/hr` | 人事行政 |
| PayrollController | `/api/payroll` | 工资管理 |
| ScheduleMonthController | `/api/schedule` | 排班管理 |
| KitchenController | `/api/kitchen` | 厨房出品 |
| KitchenSupplyService | — | 厨房供应服务 |
| IngredientController | `/api/ingredient` | 食材管理 |
| InventoryController | `/api/inventory` | 库存管理 |
| PurchaseController | `/api/purchase` | 采购管理 |
| SupplierController | `/api/supplier` | 供应商管理 |
| EngineeringController | `/api/engineering` | 工程管理 |
| MaintenanceController | `/api/maintenance` | 维修工单 |
| EnergyController | `/api/energy` | 能耗管理 |
| MarketingController | `/api/marketing` | 营销管理 |
| MarketingOverviewController | `/api/marketing/overview` | 营销概览 |
| MemberController | `/api/member` | 会员管理 |
| ApprovalController | `/api/approval` | 审批中心 |
| ReportController | `/api/report` | 数据报表 |
| DictController | `/api/dict` | 数据字典 |
| DashboardController | `/api/dashboard` | 仪表盘数据 |
| AIController | `/api/ai` | AI助手（转发天龙） |
| ChatController | `/api/chat` | 聊天服务 |
| UploadController | `/api/upload` | 文件上传 |
| IpadDishController | `/ipad/dish` | iPad点菜-菜品 |
| IpadOrderController | `/ipad/order` | iPad点菜-订单 |
| IpadTableController | `/ipad/table` | iPad点菜-桌台 |

### 安全中间件
- JWT 拦截：`JwtAuthInterceptor` → 所有请求解析 staffId/storeId
- 门店隔离：`StoreDataScopeAspect` → GET 请求自动注入 store_id 过滤
- 审计日志：`AuditLogAspect` → POST/PUT/DELETE 自动记录操作
- 限流：`RateLimitInterceptor` → 防止 API 滥用
- iPad 拦截：`IpadInterceptor` → iPad 端请求特殊处理

---

## 11. 前端页面路由图

### 主路由（/dashboard 子路由）

| 路径 | 组件 | 说明 |
|---|---|---|
| `/home` | Home.vue | 工作台 |
| `/table-board` | TableBoard.vue | 桌台看板 |
| `/bookings` | Bookings.vue | 预订管理 |
| `/menu` | MenuHub.vue | 点菜系统主入口 |
| `/menu-banquet` | MenuBanquet.vue | 宴会菜单 |
| `/menu-alacarte` | MenuAlacarte.vue | 零点菜单 |
| `/menu-soldout` | MenuSoldout.vue | 沽清内容 |
| `/menu-festive` | MenuFestive.vue | 节日菜单 |
| `/menu-full` | MenuFull.vue | 总菜单 |
| `/menu-detail` | MenuDetail.vue | 菜单详情 |
| `/menu-manager` | MenuManager.vue | 菜单管理 |
| `/dish-library` | DishLibrary.vue | 菜库编辑 |
| `/cost-recipe` | CostRecipe.vue | 成本配方 |
| `/set-menu` | SetMenu.vue | 套餐管理 |
| `/set-menu-edit` | SetMenuEdit.vue | 套餐编辑 |
| `/pricing-manage` | PricingManage.vue | 调价管理 |
| `/soldout-control` | SoldoutControl.vue | 沽清管控 |
| `/tags` | Tags.vue | 标签管理 |
| `/print-config` | PrintConfig.vue | 打印配置 |
| `/store-permission` | StorePermission.vue | 门店权限 |
| `/price-tiers` | PriceTiers.vue | 多价格体系 |
| `/categories` → 重定向 → `/category-sort` | Placeholder.vue | 分类排序（占位） |
| `/customers` | Customers.vue | 客户管理 |
| `/marketing` | Marketing.vue | 营销会员 |
| `/member-list` | MemberList.vue | 会员管理 |
| `/approval` | Approval.vue | 审批中心 |
| `/guest-analysis` | GuestAnalysis.vue | 客人分析 |
| `/front-office` | FrontOffice.vue | 前厅运营 |
| `/ai-assistant` | AiAssistant.vue | AI助手 |
| `/floor-project` | FloorProject.vue | 楼面工程 |
| `/front-desk` | FrontDesk.vue | 前台 |
| `/kitchen` | Kitchen.vue | 厨房出品 |
| `/kitchen-log` | KitchenLog.vue | 后厨日志 |
| `/art-design` | ArtDesign.vue | 美工 |
| `/table-layout` | TableLayout.vue | 台型 |
| `/production` | Production.vue | 出品 |
| `/table-utilization` | TableUtilization.vue | 桌台利用率 |
| `/supply-chain` | SupplyChain.vue | 采购仓储 |
| `/inventory` | Inventory.vue | 库存管理 |
| `/procurement` | Procurement.vue | 采购管理 |
| `/receipt` | Receipt.vue | 入库验收 |
| `/issue` | Issue.vue | 领用出库 |
| `/supplier-reconciliation` | SupplierReconciliation.vue | 供应商对账 |
| `/stock-take` | StockTake.vue | 盘点 |
| `/finance` | Finance.vue | 财务管理 |
| `/finance/dish-cost` | DishCost.vue | 菜品成本 |
| `/finance/cost-analysis` | Cost.vue | 成本分析 |
| `/dish-cost-analysis` | DishCostAnalysis.vue | 菜品成本分析 |
| `/suppliers` | Suppliers.vue | 供应商管理 |
| `/bill-manage` | BillManage.vue | 账单管理 |
| `/reports` | Reports.vue | 数据报表 |
| `/report-print` | ReportPrint.vue | 报表打印 |
| `/export-panel` | ExportPanel.vue | 数据导出 |
| `/audit-log` | AuditLog.vue | 审计日志 |
| `/dict-manager` | DictManager.vue | 数据字典 |
| `/admin` | Admin.vue | 后台管理 |
| `/perm-manager` | PermManager.vue | 权限管理 |
| `/hr-admin` | HRAdmin.vue | 人事行政 |
| `/staff` | Staff.vue | 人事管理 |
| `/training` | Training.vue | 培训管理 |
| `/license` | License.vue | 证照管理 |
| `/security` | Security.vue | 安保保洁 |
| `/assets` | Assets.vue | 行政资产 |
| `/attendance` | Attendance.vue | 考勤管理 |
| `/attendance-calendar` | AttendanceCalendar.vue | 考勤日历 |
| `/attendance-print` | AttendancePrint.vue | 考勤报表 |
| `/schedule` | Schedule.vue | 排班管理 |
| `/leave` | Leave.vue | 请假管理 |
| `/payroll` | Payroll.vue | 工资管理 |
| `/self-service` | SelfService.vue | 自助登记 |
| `/review-queue` | ReviewQueue.vue | 审核队列 |
| `/hr-analytics` | HRAnalytics.vue | HR数据 |
| `/data-screen` | DataScreen.vue | 数据大屏 |
| `/engineering` | Engineering.vue | 工程管理 |
| `/decoration` | Decoration.vue | 装修管理 |
| `/energy` | Energy.vue | 能耗管理 |
| `/safety` | Safety.vue | 安全管理 |
| `/store-org` | StoreOrg.vue | 门店组织 |
| `/gm-office` | GMOffice.vue | 总经办 |
| `/welcome` | Welcome.vue | 欢迎页 |
| `/system-checkup` | SystemCheckup.vue | 系统体检 |
| `/change-logs` | ChangeLogView.vue | 改动日志 |
| `/help` | HelpCenter.vue | 帮助文件 |
| `/help/dev-process` | DevProcess.vue | 开发过程 |
| `/ordering` | IpadMenu.vue | iPad点菜 |
| `/ipad-menu` | IpadMenu.vue | iPad点菜 |

### 系统设置子路由（/dashboard/settings/）

| 路径 | 组件 | 说明 |
|---|---|---|
| `/settings` | SettingsHub.vue | 设置主入口 |
| `/settings/info` | Info.vue | 系统信息 |
| `/settings/permission` | Permission.vue | 权限管理 |
| `/settings/org` | Organization.vue | 门店组织 |
| `/settings/config` | Config.vue | 系统配置 |
| `/settings/help` | HelpFiles.vue | 帮助日志 |
| `/settings/checkup` | SystemCheckupTab.vue | 系统体检 |

---

## 12. 部署与备份

### 本地开发
```bash
# 后端
cd banquet_project && mvn spring-boot:run
# 前端
cd frontend_v3 && npm run dev
# 访问 http://localhost:5173
```

### Docker 部署
```bash
cd youjian-docker && docker-compose up -d
# MySQL 自动初始化 banquet_init.sql + rbac_init.sql
```

### 备份
- **自动**：Docker MySQL 每日 02:00 自动备份（`backup_strategy.sh`）
- **手动**：git bundle + COS 上传
  ```bash
  # 1. 创建 bundle
  git bundle create youjianchuiyan_$(date +%Y%m%d_%H%M%S).bundle --all
  # 2. 上传到 COS
  python scripts/backup/cos_upload.py <bundle文件> "ai公共工作空间/项目管理/餐饮管理系统/备份/<文件名>"
  ```
- **COS 路径**：`ai公共工作空间/项目管理/餐饮管理系统/备份/`

### 恢复
```bash
git clone <bundle文件> restored-dir
```

---

## 文件索引（全文关键字快速定位）

### 固定接手入口（2026-09-07）

- 简称：接手文件／接受文件。用户说“接手”就先读入口与最新追加记录。
- COS：`ai公共工作空间/项目管理/餐饮管理系统/开发记录_20260907_Claude接手.md`
- 本地：`docs/开发记录_20260907_Claude接手.md`
- 配套四份文件与执行规则：`docs/接手索引.md`。这里仅记位置与规则，不缓存入口正文。
- 每次会话结束前在同一 COS 目录追加同格式记录，本地保留副本；额度将尽时提前写好、确认同步，再等待。保留他人记录，不覆盖。
- 任何人更改任何东西必须登记：执行前记意图和计划，执行后记实际改动、证据、未解决问题和下一步，失败/回退也记录；统一入口 `docs/变更登记簿.md`，交接时同步到 COS 新记录。此处保存规则与指针，不复制日志正文。
- 2026-09-07 已授权统筹多工具协作；分工、任务和合并规则指针为 `docs/协作分工与合并规则_20260907.md`。GitHub 分支/PR 汇合、本地独立工作区、COS 交接；实际领取以回执为准。
- 数据口径：库存等为业务模拟数据；“每克多少钱”成本表为真实来源，具体文件待核实。法务仍冻结。

> 本索引用于 Agent 快速定位需要的信息。

| 需求 | 见章节 |
|---|---|
| 项目是什么 | [1. 项目概述](#1-项目概述) |
| 技术栈版本 | [2. 技术栈与版本](#2-技术栈与版本) |
| 文件在哪里 | [3. 目录结构索引](#3-目录结构索引) |
| 密钥/密码 | [4. 凭证与密钥](#4-凭证与密钥指针无明文) + `C:\Users\rinom\.cos.conf` |
| 数据库表 | [5. 数据库速查](#5-数据库速查) + [数据库设计使用说明书.md](./数据库设计使用说明书.md) |
| 脚本怎么用 | [6. 脚本清单与用途](#6-脚本清单与用途) |
| 审计报告在哪 | [7. 审计报告索引](#7-审计报告索引) + `体检/体检报告/` |
| 改了什么 | [8. 重大变更日志](#8-重大变更日志) |
| 客户要求 | [9. 客户约束摘要](#9-客户约束摘要) + `.trae-cn/memory/` |
| 后端 API | [10. API 接口路由图](#10-api-接口路由图) |
| 前端页面 | [11. 前端页面路由图](#11-前端页面路由图) + `router/index.js` |
| 怎么部署 | [12. 部署与备份](#12-部署与备份) |
| COS 上传 | `scripts/backup/cos_upload.py` |

## 2026-09-06 Codex 生产准备进展

- 本轮范围：按开发文档跑通业务；法务模块及张律师既有权限冻结，三个指定账号待实际登录验收。
- 功能分支：codex/production-readiness-20260905。保留了原有未提交修改，本轮未部署线上。
- 已修复预订取消留存、桌台冲突、订金校验、客户时间字段、点菜回滚、现金收银幂等、库存并发与工具归还保留字映射；金额显示保留两位小数。
- 本机独立 MySQL 验收：29 项测试通过，85 个实体映射及 82 个仓储定义初始化通过。生产存量结构、真实账号、其他业务闭环尚未全部验收。
- 详情见 docs/生产验收执行记录_20260905.md 与 docs/开发文档逐项对照_20260906.md（84 项原文要求）。
- 自动审批拒绝从运行进程获取数据库凭证；在用户明确授权前禁止继续该生产核查或绕过拒绝。

## 2026-09-06 秋哥确定的长期测试要求

- 以后只要做测试，一律按开发文档执行端到端、场景、数据库关系、完整数据流及前后端对齐测试。
- 在独立测试环境写入可追踪的合成数据，逐个验证实体及关联，检查幽灵数据、孤儿数据和不合理数据；只建表或只验证接口返回成功不算业务验收。
- 每项保留实际输入、流转标识、断言和结果证据。全部细则见 `docs/测试验收长期标准.md`；根目录 `AGENTS.md` 已设置为后续工作规则。
- 法务正在用于诉讼，代码、数据、证据文件与张律师既有权限继续冻结，不向法务写入测试数据，不因其他业务发布而覆盖法务。
- 后续实测进展：MySQL 业务场景扩展至 16 项并通过，每场景后检查 30 条关联/数量/金额规则；新增 MVC JSON 契约与孤儿/错账检测器验证。85 个实体已逐个登记于 `docs/实体数据流验收清单_20260906.md`；该清单不是全实体验收通过证明。
- 秋哥强调菜肴成本是重点：必须贯通入库、领料、加工、出成率、菜谱用量、菜肴成本、售价、成本率；核对采购单位到使用单位换算及加工损耗，价格变化须能追踪到成本结果。售价调整不能由成本重算擅自触发。
- 秋哥已明确选择菜肴成本计价口径：按最近一次入库价核算，不采用移动加权平均价。历史入库单和流水保留当时价格、数量、金额，最新有效入库价用于后续成本重算。
- 2026-09-06 12:06 最终成本链回归：47 项通过，0 失败、0 错误、0 跳过；包含真实 Chrome 入库→领料→加工→成本页面链路。浏览器使用隔离身份上下文，非生产账号登录测试。前端完整构建成功，8 个前端计价算例通过。
- 成本链新增餐饮专用字段 `preprocessing_record.requisition_item_id`，仅测试库已有；生产需先核对增量脚本。记录见 `docs/菜肴成本链验收_20260906.md`。本轮未部署，法务与账号权限未动。

### 秋哥最新协作授权与R4入口（2026-09-07）
- 仅会造成系统瘫痪或中毒风险的事项属于重要事项，由秋哥本人决定，不需要张婧点头。其余既有授权范围内由Codex验收推进，不能仅因涉及配置/发布而额外要求批准。法务冻结、垃圾桶删除、保密、备份及全程留痕继续有效。
- 当前使用分工与唯一API契约见 docs/协作使用与分配说明_20260907_R4.md；COS工作任务技术验收状态统一reviewed，禁止混同真人approved。R4覆盖R3的旧验收用语。
- 手机收件实际是否打通以测试证据及设备回执为准；网页轮询不是关闭页面后的消息推送。
