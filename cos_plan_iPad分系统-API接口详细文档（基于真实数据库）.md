# iPad 点餐子系统 - API 接口详细文档（基于真实数据库）

> 生成时间：2026-07-25 16:55
> 责任人：天龙（后端）
> 数据来源：MySQL 数据库真实表结构
> 铁律：字段名与数据库 1:1 对齐，禁止别名/驼峰转换

---

## 全局规范

### 请求头（强制）
```
X-Store-Id: {store_id}
X-Staff-Id: {staff_id}
X-Device-Sn: {device_sn}
X-Client-Type: ipad
```

### 统一返回格式
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 字段命名规则
- 数据库字段：`snake_case`（如 `table_id`、`booking_date`）
- 接口返回：**保持 `snake_case`**，禁止转驼峰
- 前端使用：直接读取，不做转换

---

## 模块 1：登录与设备认证（5 个接口）

### 1.1 POST /api/ipad/login
**登录接口**

**入参**：
```json
{
  "phone": "string",      // 员工手机号，对应 staff_master.staff_phone
  "password": "string"    // 密码，对应 staff_master.staff_password
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "staff_id": 1,                    // staff_master.staff_id
    "staff_name": "张三",             // staff_master.staff_name
    "staff_phone": "13800138000",     // staff_master.staff_phone
    "role_type": "waiter",            // staff_master.role_type
    "store_id": 1,                    // staff_master.store_id
    "store_name": "宁国主店",         // store_info.store_name
    "device_sn": "IPAD001",           // ipad_device_info.device_sn
    "print_port": 9100,               // ipad_device_info.print_port
    "print_template_code": "default"  // ipad_device_info.print_template_code
  }
}
```

**核心逻辑**：
1. 校验 `staff_master` 账号密码
2. 查询 `ipad_device_info` 获取设备配置
3. 返回员工信息 + 门店信息 + 设备配置

---

### 1.2 GET /api/ipad/store/list
**门店列表**

**入参**：无（从请求头读取 `X-Store-Id`）

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "store_id": 1,
      "store_name": "宁国主店",
      "address": "宁国市XXX路",
      "table_count": 84,
      "status": "open"
    },
    {
      "store_id": 2,
      "store_name": "宣城分店",
      "address": "宣城市XXX路",
      "table_count": 16,
      "status": "open"
    }
  ]
}
```

**字段来源**：`store_info` 表

---

### 1.3 POST /api/ipad/device/bind
**设备绑定**

**入参**：
```json
{
  "device_sn": "string",       // 设备序列号，写入 ipad_device_info.device_sn
  "store_id": 1,               // 绑定门店，写入 ipad_device_info.store_id
  "bind_staff_id": 1           // 默认员工，写入 ipad_device_info.bind_staff_id（可选）
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "绑定成功",
  "data": {
    "id": 1,
    "device_sn": "IPAD001",
    "store_id": 1,
    "bind_staff_id": 1,
    "print_port": 9100,
    "print_template_code": "default"
  }
}
```

**字段来源**：`ipad_device_info` 表

---

### 1.4 GET /api/ipad/config/print
**打印配置**

**入参**：无

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "print_port": 9100,           // ipad_device_info.print_port 或 config 表
    "print_width": 80,            // ipad_device_info.print_width
    "print_template_code": "default"  // ipad_device_info.print_template_code
  }
}
```

**字段来源**：`ipad_device_info` + `config` 表

---

### 1.5 GET /api/ipad/sys/notice/list
**系统通知列表**

**入参**：
```json
{
  "pageNum": 1,
  "pageSize": 10
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "content": "后厨出餐通知",
        "create_time": "2026-07-25 16:00:00"
      }
    ]
  }
}
```

**字段来源**：`sys_notification` 表

---

## 模块 2：桌台与预定管理（6 个接口）

### 2.1 GET /api/ipad/table/all
**桌台列表**

**入参**：
```json
{
  "area": "string"  // 可选，区域筛选，对应 table_master.table_area
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "table_id": 1,
      "table_number": "A01",
      "table_name": "1号桌",
      "table_area": "大厅",
      "table_capacity": 10,
      "table_status": "available",
      "min_capacity": 6,
      "max_capacity": 12,
      "sort_order": 1
    }
  ]
}
```

**字段来源**：`table_master` 表（真实字段如上）

---

### 2.2 GET /api/ipad/table/filter
**按状态筛选桌台**

**入参**：
```json
{
  "status": "available"  // table_status: available/occupied/reserved/maintenance
}
```

**返回**：同 2.1

---

### 2.3 POST /api/ipad/table/open
**开台**

**入参**：
```json
{
  "table_id": 1,              // table_master.table_id
  "guest_count": 8,           // booking_master.guest_count
  "remark": "string"          // booking_master.remark（可选）
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "开台成功",
  "data": {
    "booking_id": "B20260725001",  // booking_master.booking_id
    "table_id": 1,
    "table_name": "1号桌",
    "guest_count": 8,
    "booking_date": "2026-07-25",
    "booking_time": "16:00:00"
  }
}
```

**核心逻辑**：
1. 创建 `booking_master` 记录（booking_type=normal, booking_status=pending）
2. 创建 `booking_table` 记录（关联 table_id）
3. 更新 `table_master.table_status` 为 `occupied`

---

### 2.4 POST /api/ipad/table/transfer
**转台/合台/拆台**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "target_table_id": 2,
  "type": "transfer"  // transfer/merge/split
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "转台成功",
  "data": {
    "booking_id": "B20260725001",
    "old_table_id": 1,
    "new_table_id": 2,
    "new_table_name": "2号桌"
  }
}
```

**核心逻辑**：
1. 更新 `booking_table.table_id`
2. 更新 `table_master` 状态（旧桌 available，新桌 occupied）

---

### 2.5 GET /api/ipad/booking/today
**今日预定列表**

**入参**：
```json
{
  "date": "2026-07-25"  // 可选，默认今天
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "booking_id": "B20260725001",
      "booking_date": "2026-07-25",
      "booking_time": "18:00:00",
      "customer_name": "张先生",
      "customer_phone": "13800138000",
      "guest_count": 10,
      "table_count": 2,
      "banquet_name": "生日宴",
      "booking_status": "confirmed",
      "total_amount": 2000.00
    }
  ]
}
```

**字段来源**：`booking_master` 表

---

### 2.6 GET /api/ipad/wait/list
**等位队列**

**入参**：无

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "booking_id": "B20260725002",
      "customer_name": "李女士",
      "guest_count": 4,
      "booking_time": "17:30:00",
      "wait_time": 15
    }
  ]
}
```

**字段来源**：`booking_master`（booking_type=wait）

---

## 模块 3：点餐核心业务（12 个接口）

### 3.1 GET /api/ipad/dish/category
**菜品分类**

**入参**：无

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "category_id": "1",
      "dish_category": "热菜",
      "sort_order": 1
    },
    {
      "category_id": "2",
      "dish_category": "凉菜",
      "sort_order": 2
    }
  ]
}
```

**字段来源**：`dish_master.dish_category`（去重）

---

### 3.2 GET /api/ipad/dish/list
**菜品列表**

**入参**：
```json
{
  "category_id": "1",     // dish_master.category_id
  "keyword": "string"     // 可选，菜品名称搜索
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "dish_id": "D001",
      "dish_name": "红烧肉",
      "dish_category": "热菜",
      "sale_price": 68.00,
      "cost_price": 25.00,
      "spicy_level": 1,
      "is_active": 1,
      "image_url": "https://...",
      "cooking_method": "红烧",
      "taste": "咸甜"
    }
  ]
}
```

**字段来源**：`dish_master` 表（真实字段如上）

---

### 3.3 GET /api/ipad/dish/detail/{dish_id}
**菜品详情**

**入参**：路径参数 `dish_id`

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dish_id": "D001",
    "dish_name": "红烧肉",
    "dish_category": "热菜",
    "sale_price": 68.00,
    "cost_price": 25.00,
    "spicy_level": 1,
    "cooking_method": "红烧",
    "taste": "咸甜",
    "main_ingredients": "五花肉、冰糖、酱油",
    "image_url": "https://...",
    "is_active": 1,
    "remark": "招牌菜"
  }
}
```

**字段来源**：`dish_master` 表

---

### 3.4 GET /api/ipad/package/list
**套餐列表**

**入参**：无

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "package_id": "P001",
      "package_name": "迎春接福宴",
      "package_price": 988.00,
      "dish_list": [
        {
          "dish_id": "D001",
          "dish_name": "红烧肉",
          "dish_quantity": 1
        }
      ]
    }
  ]
}
```

**字段来源**：`package_master` + `package_dish_detail` + `dish_master`

---

### 3.5 GET /api/ipad/template/list
**宴席模板列表**

**入参**：
```json
{
  "banquet_type_id": 1  // banquet_type.id
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "template_id": 1,
      "template_name": "生日宴模板A",
      "total_price": 1588.00,
      "dish_list": [
        {
          "dish_id": "D001",
          "dish_name": "红烧肉",
          "dish_quantity": 1,
          "unit_price": 68.00
        }
      ]
    }
  ]
}
```

**字段来源**：`banquet_template` + `template_dish_rel` + `dish_master`

---

### 3.6 GET /api/ipad/dish/search
**菜品搜索**

**入参**：
```json
{
  "keyword": "红烧"  // dish_name 模糊搜索
}
```

**返回**：同 3.2

---

### 3.7 POST /api/ipad/order/dish/add
**加菜**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "dish_id": "D001",
  "dish_quantity": 2,
  "dish_note": "少盐"  // 可选
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "加菜成功",
  "data": {
    "dish_booking_id": 1,
    "booking_id": "B20260725001",
    "dish_id": "D001",
    "dish_name": "红烧肉",
    "dish_quantity": 2,
    "unit_price": 68.00,
    "subtotal": 136.00,
    "kitchen_status": "pending"
  }
}
```

**字段来源**：`booking_dish_detail` 表

---

### 3.8 PUT /api/ipad/order/dish/edit
**改菜**

**入参**：
```json
{
  "dish_booking_id": 1,
  "dish_quantity": 3,
  "dish_note": "多辣"
}
```

**返回**：同 3.7

---

### 3.9 DELETE /api/ipad/order/dish/remove
**删菜**

**入参**：
```json
{
  "dish_booking_id": 1
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "删菜成功",
  "data": null
}
```

---

### 3.10 POST /api/ipad/order/dish/refund
**退菜**

**入参**：
```json
{
  "dish_booking_id": 1,
  "refund_reason": "上菜太慢"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "退菜申请已提交",
  "data": {
    "dish_booking_id": 1,
    "kitchen_status": "refunded"
  }
}
```

---

### 3.11 POST /api/ipad/order/send-kitchen
**提交后厨**

**入参**：
```json
{
  "booking_id": "B20260725001"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "已提交后厨",
  "data": {
    "booking_id": "B20260725001",
    "dish_count": 5,
    "kitchen_status": "submitted"
  }
}
```

**核心逻辑**：
1. 更新 `booking_dish_detail.kitchen_status` 为 `submitted`
2. 写入 `kitchen_log`
3. 触发 WebSocket 推送

---

### 3.12 POST /api/ipad/order/urgent
**菜品加急**

**入参**：
```json
{
  "dish_booking_id": 1
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "已加急",
  "data": {
    "dish_booking_id": 1,
    "kitchen_status": "urgent"
  }
}
```

---

## 模块 4：结算财务（7 个接口）

### 4.1 GET /api/ipad/settlement/bill/{booking_id}
**账单明细**

**入参**：路径参数 `booking_id`

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "booking_id": "B20260725001",
    "customer_name": "张先生",
    "guest_count": 10,
    "total_amount": 2000.00,
    "final_amount": 1800.00,
    "payment_status": "unpaid",
    "dish_list": [
      {
        "dish_name": "红烧肉",
        "dish_quantity": 2,
        "unit_price": 68.00,
        "subtotal": 136.00
      }
    ]
  }
}
```

**字段来源**：`booking_master` + `booking_dish_detail`

---

### 4.2 GET /api/ipad/coupon/available
**可用优惠券**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "phone": "13800138000"  // 可选
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "coupon_id": 1,
      "coupon_name": "满1000减100",
      "discount_amount": 100.00,
      "min_amount": 1000.00,
      "expire_date": "2026-12-31"
    }
  ]
}
```

**字段来源**：`marketing_coupon` + `marketing_coupon_record`

---

### 4.3 POST /api/ipad/settlement/discount
**应用优惠**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "discount_type": "coupon",  // coupon/manual
  "discount_amount": 100.00
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "优惠已应用",
  "data": {
    "booking_id": "B20260725001",
    "total_amount": 2000.00,
    "discount_amount": 100.00,
    "final_amount": 1900.00
  }
}
```

---

### 4.4 POST /api/ipad/settlement/pay
**支付**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "pay_type": "wechat",  // wechat/alipay/cash/card
  "pay_amount": 1900.00
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "支付成功",
  "data": {
    "booking_id": "B20260725001",
    "payment_status": "paid",
    "pay_order_no": "PAY20260725001",
    "pay_amount": 1900.00
  }
}
```

**字段来源**：`booking_master` + `finance_payment_record`

---

### 4.5 GET /api/ipad/settlement/history
**历史订单**

**入参**：
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "date": "2026-07-25"  // 可选
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 15,
    "list": [
      {
        "booking_id": "B20260725001",
        "customer_name": "张先生",
        "total_amount": 2000.00,
        "final_amount": 1800.00,
        "payment_status": "paid",
        "booking_date": "2026-07-25"
      }
    ]
  }
}
```

---

### 4.6 POST /api/ipad/settlement/invoice
**开具发票**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "invoice_title": "XX公司",
  "tax_no": "91340000..."
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "发票信息已保存",
  "data": {
    "booking_id": "B20260725001",
    "invoice_title": "XX公司",
    "tax_no": "91340000..."
  }
}
```

**字段来源**：`finance_voucher`

---

### 4.7 POST /api/ipad/settlement/deposit
**押金收取/退还**

**入参**：
```json
{
  "booking_id": "B20260725001",
  "type": "collect",  // collect/refund
  "amount": 200.00
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "押金已收取",
  "data": {
    "booking_id": "B20260725001",
    "deposit_amount": 200.00,
    "type": "collect"
  }
}
```

---

## 模块 5：会员与客户（4 个接口）

### 5.1 GET /api/ipad/member/search
**会员查询**

**入参**：
```json
{
  "phone": "13800138000"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "customer_id": 1,
    "customer_name": "张先生",
    "customer_phone": "13800138000",
    "member_card": {
      "card_id": 1,
      "balance": 500.00,
      "point": 1000,
      "level_name": "金卡会员"
    }
  }
}
```

**字段来源**：`customer_master` + `member_card` + `member_level`

---

### 5.2 POST /api/ipad/member/recharge
**会员充值**

**入参**：
```json
{
  "customer_id": 1,
  "recharge_money": 500.00,
  "pay_type": "wechat"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "充值成功",
  "data": {
    "customer_id": 1,
    "recharge_money": 500.00,
    "new_balance": 1000.00,
    "gift_point": 500
  }
}
```

**字段来源**：`member_recharge_record` + `member_point_log`

---

### 5.3 GET /api/ipad/member/point
**积分查询**

**入参**：
```json
{
  "customer_id": 1
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "point_balance": 1000,
    "point_rules": [
      {
        "rule_id": 1,
        "rule_name": "100积分兑换10元",
        "exchange_rate": 10
      }
    ]
  }
}
```

**字段来源**：`member_point_log` + `member_point_rule`

---

### 5.4 POST /api/ipad/customer/create
**客户建档**

**入参**：
```json
{
  "customer_name": "张先生",
  "customer_phone": "13800138000"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "建档成功",
  "data": {
    "customer_id": 1,
    "customer_name": "张先生",
    "customer_phone": "13800138000"
  }
}
```

**字段来源**：`customer_master`

---

## 模块 6：辅助功能与AI（3 个接口）

### 6.1 GET /api/ipad/stock/check
**库存查询**

**入参**：
```json
{
  "dish_id": "D001"
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dish_id": "D001",
    "dish_name": "红烧肉",
    "available_quantity": 20,
    "stock_status": "sufficient"  // sufficient/low/out
  }
}
```

**核心逻辑**：基于 `ingredient_master` + `dish_recipe` 计算

---

### 6.2 POST /api/ipad/service/call
**呼叫服务**

**入参**：
```json
{
  "table_id": 1,
  "service_type": "water"  // water/bill/other
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "呼叫已发送",
  "data": {
    "table_id": 1,
    "service_type": "water",
    "create_time": "2026-07-25 16:30:00"
  }
}
```

**字段来源**：`sys_notification`

---

### 6.3 POST /api/ipad/ai/chat
**AI对话**

**入参**：
```json
{
  "message": "推荐几道招牌菜",
  "booking_id": "B20260725001"  // 可选
}
```

**返回**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "reply": "为您推荐：1. 红烧肉（招牌）2. 清蒸鲈鱼 3. 蒜蓉粉丝蒸扇贝",
    "create_time": "2026-07-25 16:30:00"
  }
}
```

**核心逻辑**：调用 OpenClaw API（27860端口），记录到 `ai_chat_history`

---

## 附录：字段映射对照表（节选）

| 数据库表 | 数据库字段 | 接口字段 | 说明 |
|----------|------------|----------|------|
| table_master | table_id | table_id | 一致 |
| table_master | table_name | table_name | 一致 |
| table_master | table_status | table_status | 一致 |
| booking_master | booking_id | booking_id | 一致 |
| booking_master | total_amount | total_amount | 一致 |
| dish_master | dish_id | dish_id | 一致 |
| dish_master | sale_price | sale_price | 一致 |

**铁律**：所有字段名保持 `snake_case`，禁止转驼峰。

---

*文档完成，地龙/SOLO 请确认。有问题当场提出，确认后开始开发。*
