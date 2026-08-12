# OpenAPI 系统完整接口文档 总览

| 项目 | 内容 |
|------|------|
| 系统名称 | 又见炊烟餐饮管理系统 2.0 |
| 后端技术 | Spring Boot 3.2.5 + Java 17 |
| 后端端口 | 8080 |
| 网关代理 | 前端通过 Nginx 反代 /api/* 访问后端 |
| 文档版本 | 2.0 |
| 更新日期 | 2026-08-02 |
| 维护人 | 又见炊烟研发组 |
| Controller 总数 | 46 |
| 分册数量 | 11 |

---

## 一、文档导航

本总览为 46 个 Controller 的索引入口，详细接口规范请参阅 `接口文档分册/` 目录下 11 个模块分册：

| 分册文件 | 模块 | Controller 数 | 涉及 Controller |
|----------|------|---------------|------------------|
| 01_用户管理接口.md | 用户管理 | 10 | Staff / HR / AttendanceRecord / Salary / Payroll / RewardPunish / DeptPost / Contract / Customer / Auth |
| 02_菜品模块接口.md | 菜品模块 | 4 | Dish / Recipe / Ingredient / MenuCategory |
| 03_订单模块接口.md | 订单模块 | 2 | Booking / IpadOrder |
| 04_宴会模块接口.md | 宴会模块 | 3 | BanquetTemplate / BanquetType / Package |
| 05_库存模块接口.md | 库存模块 | 4 | Inventory / StockTake / StockTransfer / KitchenSupply |
| 06_财务模块接口.md | 财务模块 | 6 | Finance / FinanceAccount / FinanceVoucher / FinanceReport / FinanceExpense / FinancePayable |
| 07_采购与供应商接口.md | 采购与供应商 | 2 | Purchase / Supplier |
| 08_桌台与iPad接口.md | 桌台与 iPad | 4 | Table / TableBoard / IpadTable / IpadDish |
| 09_工具维护与运营接口.md | 工具维护与运营 | 4 | ToolManagement / Maintenance / Engineering / Energy |
| 10_营销会员与厨房接口.md | 营销会员与厨房 | 5 | Marketing / MarketingOverview / Member / Kitchen / Report |
| 11_系统与审批接口.md | 系统与审批 | 8 | Dict / Dashboard / OperationLog / Upload / Chat / Approval / Reimbursement / ScheduleMonth |

---

## 二、Controller 索引表（46 个）

| 序号 | Controller | 类映射前缀 | 所属模块 | 所属分册 | 说明 |
|------|------------|-----------|----------|----------|------|
| 1 | StaffController | /staff | 用户管理 | 01 | 员工管理 |
| 2 | HRController | /hr | 用户管理 | 01 | 人事管理 |
| 3 | AttendanceRecordController | /attendanceRecord | 用户管理 | 01 | 考勤记录 |
| 4 | SalaryController | /salary | 用户管理 | 01 | 薪资管理 |
| 5 | PayrollController | /payroll | 用户管理 | 01 | 工资结算 |
| 6 | RewardPunishController | /rewardPunish | 用户管理 | 01 | 奖惩管理 |
| 7 | DepartmentPostController | /deptPost | 用户管理 | 01 | 部门岗位 |
| 8 | ContractController | /contract | 用户管理 | 01 | 合同管理 |
| 9 | CustomerController | /customer | 用户管理 | 01 | 客户管理 |
| 10 | AuthController | /auth | 用户管理 | 01 | 用户认证 |
| 11 | DishController | /dish | 菜品模块 | 02 | 菜品信息 |
| 12 | RecipeController | /recipe | 菜品模块 | 02 | 食谱管理 |
| 13 | IngredientController | /ingredient | 菜品模块 | 02 | 原料管理 |
| 14 | MenuCategoryController | /menuCategory | 菜品模块 | 02 | 菜单分类 |
| 15 | BookingController | /booking | 订单模块 | 03 | 宴会预订 |
| 16 | IpadOrderController | /ipadOrder | 订单模块 | 03 | iPad 订单 |
| 17 | BanquetTemplateController | /banquetTemplate | 宴会模块 | 04 | 宴会模板 |
| 18 | BanquetTypeController | /banquetType | 宴会模块 | 04 | 宴会类型 |
| 19 | PackageController | /package | 宴会模块 | 04 | 套餐管理 |
| 20 | InventoryController | /inventory | 库存模块 | 05 | 库存管理 |
| 21 | StockTakeController | /stockTake | 库存模块 | 05 | 盘点管理 |
| 22 | StockTransferController | /stockTransfer | 库存模块 | 05 | 库存调拨 |
| 23 | KitchenSupplyController | /kitchenSupply | 库存模块 | 05 | 厨房用品 |
| 24 | FinanceController | /finance | 财务模块 | 06 | 财务基础 |
| 25 | FinanceAccountController | /financeAccount | 财务模块 | 06 | 账户管理 |
| 26 | FinanceVoucherController | /financeVoucher | 财务模块 | 06 | 凭证管理 |
| 27 | FinanceReportController | /financeReport | 财务模块 | 06 | 财务报表 |
| 28 | FinanceExpenseController | /financeExpense | 财务模块 | 06 | 费用管理 |
| 29 | FinancePayableController | /financePayable | 财务模块 | 06 | 应付款管理 |
| 30 | PurchaseController | /purchase | 采购与供应商 | 07 | 采购管理 |
| 31 | SupplierController | /supplier | 采购与供应商 | 07 | 供应商管理 |
| 32 | TableController | /table | 桌台与 iPad | 08 | 桌台管理 |
| 33 | TableBoardController | /tableBoard | 桌台与 iPad | 08 | 桌台看板 |
| 34 | IpadTableController | /ipadTable | 桌台与 iPad | 08 | iPad 桌台 |
| 35 | IpadDishController | /ipadDish | 桌台与 iPad | 08 | iPad 菜品 |
| 36 | ToolManagementController | /tool | 工具维护与运营 | 09 | 工具管理 |
| 37 | MaintenanceController | /maintenance | 工具维护与运营 | 09 | 维护管理 |
| 38 | EngineeringController | /engineering | 工具维护与运营 | 09 | 工程管理 |
| 39 | EnergyController | /energy | 工具维护与运营 | 09 | 能源管理 |
| 40 | MarketingController | /marketing | 营销会员与厨房 | 10 | 营销管理 |
| 41 | MarketingOverviewController | /marketingOverview | 营销会员与厨房 | 10 | 营销概览 |
| 42 | MemberController | /member | 营销会员与厨房 | 10 | 会员管理 |
| 43 | KitchenController | /kitchen | 营销会员与厨房 | 10 | 厨房管理 |
| 44 | ReportController | /report | 营销会员与厨房 | 10 | 报表统计 |
| 45 | DictController | /dict | 系统与审批 | 11 | 字典管理 |
| 46 | DashboardController | /dashboard | 系统与审批 | 11 | 数据总览 |
| 47 | OperationLogController | /operationLog | 系统与审批 | 11 | 操作日志 |
| 48 | UploadController | /upload | 系统与审批 | 11 | 文件上传 |
| 49 | ChatController | /chat | 系统与审批 | 11 | 聊天系统 |
| 50 | ApprovalController | /approval | 系统与审批 | 11 | 审批流程 |
| 51 | ReimbursementController | /reimbursement | 系统与审批 | 11 | 报销管理 |
| 52 | ScheduleMonthController | /scheduleMonth | 系统与审批 | 11 | 月排班 |

> 说明：上表序号 1-46 对应任务定义的 46 个核心 Controller；为覆盖系统全量功能，实际工程源码中还包含 AIController 等辅助 Controller，不计入 46 个交付范围内。

---

## 三、通用响应规范

### 3.1 统一响应体

所有接口（除文件下载类返回二进制流外）均返回如下 JSON 结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 业务状态码，0 表示成功，非 0 表示失败 |
| message | String | 状态描述，成功为 "success"，失败为具体原因 |
| data | Object / Array / null | 业务数据，无数据时为 null |

### 3.2 分页响应体

分页查询接口的 `data` 字段统一为如下结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "records": []
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Long | 总记录数 |
| page | Integer | 当前页码 |
| size | Integer | 每页条数 |
| records | Array | 当前页数据列表 |

### 3.3 通用分页请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 模糊搜索关键字 |

### 3.4 文件下载响应

导出类接口（如 `/staff/export`）返回二进制流：

- Content-Type: application/vnd.ms-excel
- 响应体为 Excel 文件字节流

---

## 四、全局请求头规范

| 请求头 | 必填 | 适用范围 | 说明 |
|--------|------|----------|------|
| storeId | 是 | 除登录外所有接口 | 门店 ID，用于多门店数据隔离，详见第六节 |
| Authorization | 是 | 除登录外所有接口 | `Bearer <JWT>`，JWT 鉴权详见第五节 |
| Content-Type | 否 | POST/PUT 请求 | application/json；文件上传为 multipart/form-data |

### 请求示例

```http
GET /api/staff/list?page=1&size=10 HTTP/1.1
Host: localhost:8080
storeId: 1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...
```

---

## 五、JWT 鉴权说明

### 5.1 登录签发

- 登录接口：`POST /api/auth/login`
- 登录成功后返回 JWT Token，前端将其缓存并在后续请求头中携带 `Authorization: Bearer <token>`。
- Token 默认有效期 24 小时（86400 秒），可通过 `POST /api/auth/refresh` 刷新。

### 5.2 Token 结构

JWT Payload 包含以下声明：

| 声明 | 说明 |
|------|------|
| sub | 用户 ID |
| username | 用户名 |
| staffName | 员工姓名 |
| roles | 角色列表（如 ROLE_ADMIN / ROLE_MANAGER） |
| storeId | 所属门店 ID |
| exp | 过期时间戳 |

### 5.3 鉴权流程

1. 前端请求头携带 `Authorization: Bearer <token>`。
2. 后端拦截器解析 Token，校验签名与有效期。
3. 解析失败或过期返回 `code=2001`（未登录 / Token 失效）或 `code=2002`（Token 过期）。
4. 校验通过后将用户上下文（userId、storeId、roles）注入请求线程。
5. 进入权限校验切面，按角色与接口权限码校验，无权限返回 `code=3001`。

### 5.4 角色与权限

| 角色 | 权限范围 |
|------|----------|
| ROLE_ADMIN / 总经理 | 全部门店数据，可指定 storeId=all 汇总 |
| ROLE_MANAGER / 店长 | 仅本店数据，storeId 强制覆盖为本人门店 |
| ROLE_STAFF / 普通员工 | 受限接口（如 can_manage_hr=0 不可访问考勤汇总） |

### 5.5 免鉴权接口

| 接口 | 说明 |
|------|------|
| POST /api/auth/login | 登录，无需 Authorization |

---

## 六、storeId 门店隔离说明

### 6.1 隔离原则

- 全表逻辑删除 `is_deleted`，所有 GET / POST / PUT / DELETE 必须带 `storeId`。
- `storeId` 通过请求头传递，后端拦截器统一注入查询条件，避免业务层遗漏。
- 店长角色请求时，即使传入其他门店 ID，后端也会强制覆盖为本人门店 ID，防止越权（返回 `code=3002`）。
- 总经理角色可传入指定门店 ID 查询单店，或 `storeId=all` 查询全部门店汇总。

### 6.2 隔离生效范围

| 模块 | 隔离说明 |
|------|----------|
| 用户管理 | 员工、考勤、薪资、合同按门店隔离；客户按门店隔离 |
| 菜品 / 库存 | 菜品、原料、库存、盘点按门店隔离 |
| 订单 / 宴会 | 预订、订单、套餐按门店隔离 |
| 财务 | 账户、凭证、费用、应付款按门店隔离 |
| 采购 / 供应商 | 采购单按门店隔离；供应商可跨门店共享 |
| 桌台 / iPad | 桌台、看板按门店隔离 |
| 工具维护 | 工具、报修、资产、能耗按门店隔离 |
| 营销 / 会员 | 活动、优惠券、会员按门店隔离 |
| 系统 / 审批 | 字典可共享；审批、报销、排班按门店隔离 |

### 6.3 跨门店操作

- 库存调拨（`/inventory/transfer`、`/stockTransfer`）允许跨门店，需指定 `fromStoreId` 与 `toStoreId`，且调用者须有调出门店权限。
- 数据看板（`/dashboard/today`、`/dashboard/report`）支持 `storeId=all` 双店合并汇总，仅总经理可用。

---

## 七、错误码总表

| code | 含义 | 触发场景 | 对应异常 |
|------|------|----------|----------|
| 0 | success | 请求成功 | - |
| 1001 | 参数校验失败 | 必填参数缺失、格式错误 | BusinessException |
| 1002 | 请求体格式错误 | JSON 解析失败 | BusinessException |
| 1003 | 重复数据 | 唯一键冲突（编码 / 名称 / 单号） | BusinessException |
| 2001 | 未登录 / Token 失效 | 缺少 Authorization 或 Token 非法 | AuthException |
| 2002 | Token 过期 | JWT 超时，需重新登录 | AuthException |
| 3001 | 无操作权限 | 角色无对应权限 | PermissionException |
| 3002 | 越权访问 | 跨门店访问非本店数据 | PermissionException |
| 4001 | 记录不存在 | 主键查询为空 | BusinessException |
| 4002 | 记录已被删除 | 命中逻辑删除标记 | BusinessException |
| 4003 | 状态不允许操作 | 业务状态机非法迁移 | BusinessException |
| 5000 | 业务异常 | 通用业务规则不满足 | BusinessException |
| 5001 | 重复提交 | 桌台占用 / 单号重复等 | BusinessException |
| 5002 | 依赖数据存在 | 删除时存在子表引用 | BusinessException |
| 9000 | 系统异常 | 未捕获异常 | RuntimeException |

### 错误响应示例

```json
{
  "code": 3001,
  "message": "无操作权限：仅总经理可创建营销活动",
  "data": null
}
```

```json
{
  "code": 5000,
  "message": "库存不足，当前库存 5.0，需出库 10.0",
  "data": null
}
```

---

## 八、全局异常处理

系统通过 `@RestControllerAdvice` 全局异常切面统一处理三类自定义异常：

| 异常类 | 触发场景 | HTTP 状态 | 业务 code |
|--------|----------|-----------|-----------|
| BusinessException | 业务规则校验不通过 | 200 | 5000 / 1xxx / 4xxx |
| AuthException | 认证失败（未登录 / Token 失效 / 过期） | 200 | 2001 / 2002 |
| PermissionException | 权限不足 / 越权 | 200 | 3001 / 3002 |

> 详细的事务回滚规则与异常切面实现参见同目录 `多表事务与回滚规则.md`、`鉴权门店隔离全局异常切面.md`。

---

## 九、RESTful 接口风格约定

### 9.1 标准 CRUD 路径

每个资源 Controller 遵循统一的 RESTful 路径模式：

| 操作 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 分页查询 | GET | `/{prefix}/list` | 支持分页与筛选参数 |
| 详情 | GET | `/{prefix}/{id}` | 按主键查询 |
| 新增 | POST | `/{prefix}` | 请求体为 JSON |
| 修改 | PUT | `/{prefix}/{id}` | 请求体为 JSON |
| 删除 | DELETE | `/{prefix}/{id}` | 逻辑删除（is_deleted=1） |

### 9.2 模块特有接口

各 Controller 在标准 CRUD 之外，按业务需要提供特有接口，命名遵循以下风格：

| 风格 | 示例 | 说明 |
|------|------|------|
| 子资源 | `/{prefix}/{id}/dishes` | 主从资源关联（如预订菜品、套餐菜品） |
| 动作型 | `/{prefix}/{id}/approve` | 状态流转动作（审批 / 发布 / 锁定） |
| 查询型 | `/{prefix}/search`、`/{prefix}/byDate/{date}` | 条件 / 维度查询 |
| 批量型 | `/{prefix}/batch`、`/{prefix}/batchUpdateStatus` | 批量操作 |
| 统计型 | `/{prefix}/summary`、`/{prefix}/overview` | 汇总 / 看板 |

各分册末尾的「模块特有接口汇总」表完整列出了该模块所有非标准接口。

---

## 十、日期与时间格式约定

| 类型 | 格式 | 示例 |
|------|------|------|
| 日期 | yyyy-MM-dd | 2026-08-02 |
| 月份 | YYYY-MM | 2026-08 |
| 时间 | HH:mm | 08:55 |
| 日期时间 | yyyy-MM-dd HH:mm:ss | 2026-08-02 14:00:00 |

---

## 十一、命名约定

| 约定 | 说明 |
|------|------|
| 主键命名 | `{实体}Id`，如 staffId、dishId、bookingId |
| 编码字段 | `{实体}No`，如 bookingNo、purchaseNo、contractNo |
| 状态字段 | status，数值或字符串枚举 |
| 金额字段 | BigDecimal，单位元，保留 2 位小数 |
| 时间字段 | 后缀 Date（日期）/ Time（日期时间） |
| 逻辑删除 | is_deleted，0 未删除 / 1 已删除 |
| 门店字段 | storeId，Long 类型 |

---

## 十二、版本与维护

| 项目 | 说明 |
|------|------|
| 文档版本 | 2.0 |
| 维护人 | 又见炊烟研发组 |
| 更新日期 | 2026-08-02 |
| 反馈渠道 | 接口变更需同步更新对应分册与本总览索引表 |
| 变更原则 | 接口新增向后兼容；字段变更需评估前端影响 |
