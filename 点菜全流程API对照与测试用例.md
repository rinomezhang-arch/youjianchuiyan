# 点菜全流程 API对照与测试用例

> 编写：solo | 日期：2026-07-27 | 用于三人合拢验证

---

## 一、点菜全流程

```
欢迎页 → 选门店弹窗 → 选桌台弹窗 → iPad点菜页 → 加菜 → 查看订单 → 客户信息 → 员工验证 → 提交后厨
```

## 二、API端点对照表

### 已有端点（正常工作）

| # | 方法 | 路径 | 状态 | 说明 |
|---|------|------|------|------|
| 1 | GET | /api/ipad/table/list | 200 | 桌台列表 |
| 2 | GET | /api/ipad/dish/category | 200 | 菜品分类 |
| 3 | GET | /api/ipad/dish/list | 200 | 菜品列表 |
| 4 | POST | /api/ipad/table/open | 存在 | 开台 |
| 5 | POST | /api/ipad/order/dish/add | 存在 | 加菜 |
| 6 | PUT | /api/ipad/order/dish/edit | 存在 | 改数量 |
| 7 | DELETE | /api/ipad/order/dish/remove/{id} | 存在 | 删菜 |
| 8 | GET | /api/ipad/order/current | 存在 | 当前订单 |
| 9 | POST | /api/ipad/order/send-kitchen | 存在 | 提交后厨 |

### 缺失端点（天龙需创建）

| # | 方法 | 路径 | 说明 | 优先级 |
|---|------|------|------|--------|
| A | POST | /api/ipad/staff/verify | 员工验证 | P0 |
| B | POST | /api/ipad/order/customer | 保存客户信息 | P0 |

### 参数不匹配（需对齐）

| 端点 | 前端发送 | 后端期望 | 修复方案 |
|------|---------|---------|---------|
| send-kitchen | {table_id, customer_name, phone, person_count, table_count, note, staff_id} | {table_id, order_note} | 天龙改后端兼容note字段 |

## 三、缺失端点详细规格

### A. POST /api/ipad/staff/verify

**请求头（必须）：**
```
X-Store-Id: 1
X-Staff-Id: 1
X-Device-Sn: test001
X-Client-Type: ipad
Content-Type: application/json
```

**请求体：**
```json
{
  "card_number": "001",
  "password": "123456"
}
```

**成功响应：**
```json
{
  "code": 200,
  "data": {
    "staff_id": 1,
    "staff_name": "张三",
    "card_number": "001"
  }
}
```

**失败响应：**
```json
{
  "code": 400,
  "message": "工号或密码错误"
}
```

**SQL参考：**
```sql
SELECT staff_id, staff_name, card_number
FROM staff_master
WHERE card_number = ? AND password = ? AND store_id = ? AND is_active = 1
```

### B. POST /api/ipad/order/customer

**请求体：**
```json
{
  "table_id": "T001",
  "customer_name": "李四",
  "phone": "13800138000",
  "person_count": 4,
  "table_count": 1,
  "note": "不要辣"
}
```

**成功响应：**
```json
{
  "code": 200,
  "message": "保存成功"
}
```

**SQL参考：**
```sql
-- 找到最新booking
SELECT booking_id FROM booking_master b
JOIN booking_table tb ON b.booking_id = tb.booking_id
WHERE tb.table_id = ? AND b.booking_status = 'confirmed'
ORDER BY b.created_at DESC LIMIT 1

-- 更新客户信息
UPDATE booking_master
SET customer_name = ?, customer_phone = ?, guest_count = ?, remark = ?
WHERE booking_id = ?
```

## 四、前端修复清单（地龙负责）

| # | 文件 | 行号 | 问题 | 严重性 |
|---|------|------|------|--------|
| 1 | IpadMenu.vue | 612-617 | verifyStaffCard catch块绕过验证 | 严重 |
| 2 | IpadMenu.vue | 620-651 | doSubmitOrder参数名不匹配 | 中等 |
| 3 | IpadMenu.vue | 全部 | X-Store-Id/X-Staff-Id硬编码 | 低 |

## 五、测试用例

### TC-01: 欢迎页入口
1. 打开 http://localhost:5173/
2. 验证显示"客户点菜"和"后台管理"两个入口
3. 验证底部显示"苏公网安备32132302010492号"
4. 点击"客户点菜"
5. **预期：** 弹出门店选择弹窗

### TC-02: 门店选择
1. 弹窗显示宁国店、宣城店
2. 点击宁国店
3. **预期：** 门店弹窗关闭，弹出桌台选择弹窗

### TC-03: 桌台选择
1. 桌台弹窗显示区域按钮和台号卡片
2. 区域按钮使用API返回的table_area原始值
3. 点击可用台卡
4. **预期：** 跳转到iPad点菜页，显示桌台号

### TC-04: 浏览菜品
1. 验证左侧分类导航显示
2. 验证菜品卡片显示（图片、名称、价格）
3. 点击分类切换
4. **预期：** 菜品列表按分类刷新

### TC-05: 加菜
1. 点击菜品卡片
2. 设置数量
3. 确认添加
4. **预期：** 购物车数量+1，调用 POST /api/ipad/order/dish/add

### TC-06: 查看订单
1. 点击购物车图标
2. 验证订单列表显示
3. 验证数量加减功能
4. 验证总价计算
5. **预期：** 订单数据从 GET /api/ipad/order/current 获取

### TC-07: 提交订单 - 客户信息
1. 点击"提交后厨"
2. 验证弹出客户信息弹窗
3. 填写姓名、电话、人数
4. 点击确认
5. **预期：** 调用 POST /api/ipad/order/customer，成功后弹出员工验证

### TC-08: 员工验证
1. 输入员工工号
2. 输入密码
3. 点击"验证提交"
4. **预期：** 调用 POST /api/ipad/staff/verify
5. 验证成功 → 提交后厨
6. 验证失败 → 显示错误，不提交
7. 网络错误 → 显示错误，不提交（不能绕过！）

### TC-09: 提交后厨
1. 员工验证通过后
2. **预期：** 调用 POST /api/ipad/order/send-kitchen
3. 成功 → 显示"已提交后厨"，清空购物车
4. 失败 → 显示错误信息

### TC-10: 已有预定的客户
1. 选择已预定桌台
2. 进入点菜页
3. 点击"提交后厨"
4. **预期：** 客户信息自动填入，不弹出客户信息弹窗

## 六、请求头规范（所有iPad端接口必须携带）

```
X-Store-Id: {门店ID，从userStore.storeId获取}
X-Staff-Id: {员工ID，从userStore.userInfo.staff_id获取}
X-Device-Sn: {设备序列号，从localStorage或userStore获取}
X-Client-Type: ipad
```

后端validateHeader()校验逻辑：缺少任意一项返回401。

---

*本文档由solo编写，用于2小时后合拢验证。*
