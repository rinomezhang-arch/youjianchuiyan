# 08 桌台与iPad接口分册

| 项目 | 内容 |
|------|------|
| 模块名称 | 桌台与 iPad |
| Controller 数量 | 4 |
| 覆盖 Controller | TableController / TableBoardController / IpadTableController / IpadDishController |
| 维护人 | 又见炊烟研发组 |
| 文档版本 | 2.0 |
| 更新日期 | 2026-08-02 |
| 后端端口 | 8080 |
| 网关前缀 | 前端通过 Nginx 反代 /api/* 访问后端 |

---

## 通用约定

### 全局请求头

| 请求头 | 必填 | 说明 |
|--------|------|------|
| storeId | 是 | 门店 ID，多门店数据隔离 |
| Authorization | 是 | `Bearer <JWT>` |
| Content-Type | 否 | POST/PUT 为 application/json |

### 全局响应格式

```json
{ "code": 0, "message": "success", "data": {} }
```

### 通用错误码

| code | 含义 | 触发场景 |
|------|------|----------|
| 0 | success | 请求成功 |
| 1001 | 参数校验失败 | 必填参数缺失 |
| 1002 | 请求体格式错误 | JSON 解析失败 |
| 1003 | 重复数据 | 桌台编号重复 |
| 2001 | 未登录 / Token 失效 | 缺少 Authorization |
| 2002 | Token 过期 | JWT 超时 |
| 3001 | 无操作权限 | 角色无权限 |
| 3002 | 越权访问 | 跨门店访问 |
| 4001 | 记录不存在 | 桌台查询为空 |
| 4002 | 记录已被删除 | 命中逻辑删除标记 |
| 4003 | 状态不允许操作 | 空桌才能开台 |
| 5000 | 业务异常 | BusinessException |
| 5001 | 重复提交 | 桌台已被占用 |
| 9000 | 系统异常 | 未捕获异常 |

### 桌台状态码

| 状态值 | 含义 |
|--------|------|
| idle | 空闲 |
| occupied | 就餐中 |
| reserved | 已预订 |
| cleaning | 打扫中 |

---

## 8.1 TableController 桌台管理

- **类映射前缀**：`/table`
- **完整路径示例**：`/api/table/list`
- **说明**：桌台档案、区域、座位数、状态、拖拽排序。

### 8.1.1 桌台列表

- **方法**：GET
- **路径**：`/api/table/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| areaId | Long | 否 | 区域 ID 过滤 |
| status | String | 否 | 状态过滤 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 30,
    "page": 1,
    "size": 10,
    "records": [
      {
        "tableId": 11,
        "tableName": "A1 大厅",
        "areaId": 1,
        "areaName": "大厅",
        "seats": 10,
        "status": "idle",
        "sort": 1,
        "storeId": 1
      }
    ]
  }
}
```

- **错误码**：0 / 2001

### 8.1.2 桌台详情

- **方法**：GET
- **路径**：`/api/table/{id}`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 桌台 ID（路径参数） |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tableId": 11,
    "tableName": "A1 大厅",
    "areaId": 1,
    "areaName": "大厅",
    "seats": 10,
    "status": "idle",
    "sort": 1,
    "storeId": 1
  }
}
```

- **错误码**：0 / 4001 / 3002

### 8.1.3 新增桌台

- **方法**：POST
- **路径**：`/api/table`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tableName | String | 是 | 桌台名称 |
| areaId | Long | 是 | 区域 ID |
| seats | Integer | 是 | 座位数 |
| sort | Integer | 否 | 排序号 |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": { "tableId": 12 } }
```

- **错误码**：0 / 1001 / 1003

### 8.1.4 修改桌台

- **方法**：PUT
- **路径**：`/api/table/{id}`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 桌台 ID（路径参数） |
| tableName | String | 否 | 桌台名称 |
| areaId | Long | 否 | 区域 ID |
| seats | Integer | 否 | 座位数 |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": null }
```

- **错误码**：0 / 4001 / 3002

### 8.1.5 更新桌台状态

- **方法**：PUT
- **路径**：`/api/table/{id}/status`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 桌台 ID（路径参数） |
| status | String | 是 | 目标状态：idle / occupied / reserved / cleaning |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": { "tableId": 11, "status": "occupied" } }
```

- **错误码**：0 / 4001 / 4003 / 5001

### 8.1.6 桌台排序

- **方法**：PUT
- **路径**：`/api/table/reorder`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sortList | Array | 是 | 排序列表 |
| sortList[].tableId | Long | 是 | 桌台 ID |
| sortList[].sort | Integer | 是 | 排序号 |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": null }
```

- **错误码**：0 / 1001 / 3002

### 8.1.7 删除桌台

- **方法**：DELETE
- **路径**：`/api/table/{id}`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 桌台 ID（路径参数） |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": null }
```

- **错误码**：0 / 4001 / 4003（就餐中不可删）

---

## 8.2 TableBoardController 桌台看板

- **类映射前缀**：`/tableBoard`
- **完整路径示例**：`/api/tableBoard/board`
- **说明**：实时桌台看板聚合视图。

### 8.2.1 桌台看板

- **方法**：GET
- **路径**：`/api/tableBoard/board`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "summary": { "total": 30, "idle": 12, "occupied": 15, "reserved": 3 },
    "areas": [
      {
        "areaId": 1,
        "areaName": "大厅",
        "tables": [
          {
            "tableId": 11,
            "tableName": "A1 大厅",
            "seats": 10,
            "status": "occupied",
            "orderId": "O20260801001",
            "openTime": "2026-08-01 11:30:00",
            "guestCount": 8
          }
        ]
      }
    ]
  }
}
```

- **错误码**：0 / 2001

### 8.2.2 桌台状态统计

- **方法**：GET
- **路径**：`/api/tableBoard/statistics`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 30,
    "idle": 12,
    "occupied": 15,
    "reserved": 3,
    "cleaning": 0,
    "turnoverRate": 0.5
  }
}
```

- **错误码**：0 / 2001

---

## 8.3 IpadTableController iPad 桌台

- **类映射前缀**：`/ipadTable`
- **完整路径示例**：`/api/ipadTable/table/list`
- **说明**：iPad 端桌台选择、开台、转台、今日预订、等位队列。

### 8.3.1 iPad 桌台列表

- **方法**：GET
- **路径**：`/api/ipadTable/table/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| areaId | Long | 否 | 区域 ID |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "tableId": 11,
      "tableName": "A1 大厅",
      "seats": 10,
      "status": "idle",
      "areaName": "大厅"
    }
  ]
}
```

- **错误码**：0 / 2001

### 8.3.2 全部桌台

- **方法**：GET
- **路径**：`/api/ipadTable/table/all`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 返回全部桌台 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "tableId": 11, "tableName": "A1 大厅", "status": "idle", "seats": 10 }
  ]
}
```

- **错误码**：0 / 2001

### 8.3.3 按状态筛选

- **方法**：GET
- **路径**：`/api/ipadTable/table/filter`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 状态：idle / occupied / reserved |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "tableId": 11, "tableName": "A1 大厅", "status": "idle" }
  ]
}
```

- **错误码**：0 / 1001 / 2001

### 8.3.4 开台

- **方法**：POST
- **路径**：`/api/ipadTable/table/open`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tableId | Long | 是 | 桌台 ID |
| guestCount | Integer | 否 | 用餐人数 |
| waiterId | Long | 否 | 服务员 ID |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": { "orderId": "O20260801002", "tableId": 11, "status": "occupied" } }
```

- **错误码**：0 / 1001 / 4003（非空桌）/ 5001

### 8.3.5 转台

- **方法**：POST
- **路径**：`/api/ipadTable/table/transfer`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fromTableId | Long | 是 | 原桌台 ID |
| toTableId | Long | 是 | 目标桌台 ID |

- **响应示例**：

```json
{ "code": 0, "message": "success", "data": { "fromTableId": 11, "toTableId": 12, "orderId": "O20260801002" } }
```

- **错误码**：0 / 1001 / 4001 / 5001（目标桌台已占用）

### 8.3.6 今日预订

- **方法**：GET
- **路径**：`/api/ipadTable/booking/today`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "bookingId": "B20260801001", "customerName": "李四", "tableCount": 20, "banquetDate": "2026-08-01" }
  ]
}
```

- **错误码**：0 / 2001

### 8.3.7 等位队列

- **方法**：GET
- **路径**：`/api/ipadTable/wait/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "waitId": 1, "customerName": "赵六", "phone": "13800000006", "guestCount": 4, "queueNo": 1, "waitMinutes": 15 }
  ]
}
```

- **错误码**：0 / 2001

---

## 8.4 IpadDishController iPad 菜品

- **类映射前缀**：`/ipadDish`
- **完整路径示例**：`/api/ipadDish/dish/list`
- **说明**：iPad 端点餐菜品浏览、套餐、模板、搜索。

### 8.4.1 菜品分类

- **方法**：GET
- **路径**：`/api/ipadDish/dish/category`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "categoryId": 1, "categoryName": "凉菜", "sort": 1 }
  ]
}
```

- **错误码**：0 / 2001

### 8.4.2 菜品列表

- **方法**：GET
- **路径**：`/api/ipadDish/dish/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | Long | 否 | 分类 ID |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "dishId": 1001,
      "dishName": "宫保鸡丁",
      "price": 38.00,
      "unit": "份",
      "image": "/upload/dish/1001.jpg",
      "status": 1,
      "categoryId": 5
    }
  ]
}
```

- **错误码**：0 / 2001

### 8.4.3 菜品详情

- **方法**：GET
- **路径**：`/api/ipadDish/dish/detail/{dishId}`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| dishId | Long | 是 | 菜品 ID（路径参数） |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "dishId": 1001,
    "dishName": "宫保鸡丁",
    "price": 38.00,
    "unit": "份",
    "image": "/upload/dish/1001.jpg",
    "description": "经典川菜",
    "status": 1,
    "tags": ["麻辣", "招牌"]
  }
}
```

- **错误码**：0 / 4001 / 3002

### 8.4.4 套餐列表

- **方法**：GET
- **路径**：`/api/ipadDish/package/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 无 | - | - | 由 storeId 隔离 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "packageId": 1, "packageName": "四人经济套餐", "packagePrice": 198.00, "personCount": 4 }
  ]
}
```

- **错误码**：0 / 2001

### 8.4.5 模板列表

- **方法**：GET
- **路径**：`/api/ipadDish/template/list`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| typeId | Long | 否 | 宴会类型 ID |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "templateId": 1, "templateName": "标准婚宴 1288 套餐", "tablePrice": 1288.00 }
  ]
}
```

- **错误码**：0 / 2001

### 8.4.6 菜品搜索

- **方法**：GET
- **路径**：`/api/ipadDish/dish/search`
- **请求头**：storeId、Authorization

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键字 |

- **响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "dishId": 1001, "dishName": "宫保鸡丁", "price": 38.00, "status": 1 }
  ]
}
```

- **错误码**：0 / 1001 / 2001

---

## 附录：模块特有接口汇总

| Controller | 特有接口 | 方法 | 说明 |
|------------|----------|------|------|
| TableController | /table/{id}/status | PUT | 更新桌台状态 |
| TableController | /table/reorder | PUT | 桌台排序 |
| TableBoardController | /tableBoard/board | GET | 桌台看板 |
| TableBoardController | /tableBoard/statistics | GET | 状态统计 |
| IpadTableController | /ipadTable/table/list | GET | iPad 桌台列表 |
| IpadTableController | /ipadTable/table/all | GET | 全部桌台 |
| IpadTableController | /ipadTable/table/filter | GET | 按状态筛选 |
| IpadTableController | /ipadTable/table/open | POST | 开台 |
| IpadTableController | /ipadTable/table/transfer | POST | 转台 |
| IpadTableController | /ipadTable/booking/today | GET | 今日预订 |
| IpadTableController | /ipadTable/wait/list | GET | 等位队列 |
| IpadDishController | /ipadDish/dish/category | GET | 菜品分类 |
| IpadDishController | /ipadDish/dish/list | GET | 菜品列表 |
| IpadDishController | /ipadDish/dish/detail/{dishId} | GET | 菜品详情 |
| IpadDishController | /ipadDish/package/list | GET | 套餐列表 |
| IpadDishController | /ipadDish/template/list | GET | 模板列表 |
| IpadDishController | /ipadDish/dish/search | GET | 菜品搜索 |
