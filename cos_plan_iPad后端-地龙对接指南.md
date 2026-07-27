# iPad 后端接口 - 地龙对接指南

> 更新时间：2026-07-25 17:10
> 责任人：天龙（后端）

---

## 一、文件位置

### 后端代码路径
```
/home/ubuntu/canyin/java_backend/src/main/java/com/youjian/banquet/ipad/
├── config/
│   ├── IpadHeaderInterceptor.java    # 请求头拦截器
│   └── IpadWebConfig.java            # 拦截器配置
├── controller/
│   ├── IpadAuthController.java       # 登录接口
│   ├── IpadStoreController.java      # 门店+设备接口
│   ├── IpadTableController.java      # 桌台+预定接口
│   └── IpadDishController.java       # 菜品+套餐接口
├── entity/
│   └── IpadDeviceInfo.java           # 设备实体
└── repository/
    └── IpadDeviceInfoRepository.java # 设备数据访问
```

### 数据库
- 库名：`banquet`
- 地址：`localhost:3306`（服务器本地）
- 新增表：`ipad_device_info`（已创建）

---

## 二、端口配置

| 服务 | 端口 | 用途 |
|------|------|------|
| HTTP 接口 | **8080** | 所有 `/api/ipad/**` 接口 |
| WebSocket | **8081** | 桌台状态、后厨推送（待开发） |

### 前端环境变量配置
```env
VITE_API_BASE_URL=http://1.13.173.213:8080
VITE_WS_URL=ws://1.13.173.213:8081/ws/ipad
VITE_APP_CLIENT_TYPE=ipad
```

---

## 三、请求头规范（强制）

所有 `/api/ipad/**` 接口必须携带以下请求头：

```http
X-Store-Id: 1              # 门店ID（1=宁国，2=宣城）
X-Staff-Id: 1              # 员工ID
X-Device-Sn: IPAD001       # 设备序列号
X-Client-Type: ipad        # 固定值 "ipad"
```

**缺失任意一项，后端返回 401 拦截。**

---

## 四、已完成接口（17个）

### 模块1：登录与设备（3个）
| 接口 | 方法 | 路径 |
|------|------|------|
| 登录 | POST | `/api/ipad/login` |
| 门店列表 | GET | `/api/ipad/store/list` |
| 设备绑定 | POST | `/api/ipad/device/bind` |
| 打印配置 | GET | `/api/ipad/config/print` |

### 模块2：桌台与预定（6个）
| 接口 | 方法 | 路径 |
|------|------|------|
| 桌台列表 | GET | `/api/ipad/table/all` |
| 状态筛选 | GET | `/api/ipad/table/filter` |
| 开台 | POST | `/api/ipad/table/open` |
| 转台 | POST | `/api/ipad/table/transfer` |
| 今日预定 | GET | `/api/ipad/booking/today` |
| 等位队列 | GET | `/api/ipad/wait/list` |

### 模块3：菜品（5个）
| 接口 | 方法 | 路径 |
|------|------|------|
| 分类列表 | GET | `/api/ipad/dish/category` |
| 菜品列表 | GET | `/api/ipad/dish/list` |
| 菜品详情 | GET | `/api/ipad/dish/detail/{dish_id}` |
| 套餐列表 | GET | `/api/ipad/package/list` |
| 宴席模板 | GET | `/api/ipad/template/list` |
| 菜品搜索 | GET | `/api/ipad/dish/search` |

---

## 五、字段命名规范

**铁律：snake_case，禁止驼峰转换**

| 数据库字段 | 接口返回 | 前端使用 |
|------------|----------|----------|
| `table_id` | `table_id` | `data.table_id` |
| `booking_id` | `booking_id` | `data.booking_id` |
| `dish_name` | `dish_name` | `data.dish_name` |
| `sale_price` | `sale_price` | `data.sale_price` |

**禁止**：`tableId`、`bookingId`、`dishName`、`salePrice`

---

## 六、统一返回格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

错误返回：
```json
{
  "code": 400,
  "msg": "错误信息",
  "data": null
}
```

---

## 七、待开发接口（20个）

### 订单模块（6个）
- POST `/api/ipad/order/dish/add` - 加菜
- PUT `/api/ipad/order/dish/edit` - 改菜
- DELETE `/api/ipad/order/dish/remove` - 删菜
- POST `/api/ipad/order/dish/refund` - 退菜
- POST `/api/ipad/order/send-kitchen` - 提交后厨
- POST `/api/ipad/order/urgent` - 加急

### 结算模块（7个）
- GET `/api/ipad/settlement/bill/{booking_id}` - 账单
- GET `/api/ipad/coupon/available` - 可用优惠券
- POST `/api/ipad/settlement/discount` - 应用优惠
- POST `/api/ipad/settlement/pay` - 支付
- GET `/api/ipad/settlement/history` - 历史订单
- POST `/api/ipad/settlement/invoice` - 开发票
- POST `/api/ipad/settlement/deposit` - 押金

### 会员模块（4个）
- GET `/api/ipad/member/search` - 会员查询
- POST `/api/ipad/member/recharge` - 充值
- GET `/api/ipad/member/point` - 积分
- POST `/api/ipad/customer/create` - 建档

### 辅助模块（3个）
- GET `/api/ipad/stock/check` - 库存查询
- POST `/api/ipad/service/call` - 呼叫服务
- POST `/api/ipad/ai/chat` - AI对话

---

## 八、联调流程

1. 地龙本地启动前端：`npm run dev:ipad`（端口 5174）
2. 前端请求指向：`http://1.13.173.213:8080/api/ipad/...`
3. 携带请求头：4 组 Header
4. 字段直接使用：snake_case，不转换
5. 有问题当场提出，天龙立即修复

---

*地龙确认收到后，开始对接已完成的 17 个接口。*
