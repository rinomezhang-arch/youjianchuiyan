# iPad 点餐系统技术方案

> 版本：v1.0  
> 日期：2026-07-26  
> 作者：SOLO 🌟  
> 状态：待确认

---

## 一、三种菜单分类体系设计

### 1.1 核心思路

根据用户提供的UI参考图和需求描述，设计三种菜单：

| 菜单类型 | 英文标识 | 菜单代码 | 用途 | 数据来源 |
|----------|----------|----------|------|----------|
| 零点菜单 | À la Carte | `alacarte` | 日常单点菜品 | dish_master |
| 宴会菜单 | Banquet | `banquet` | 宴席套餐/模板 | banquet_template + dish_master |
| 全部菜单 | All | `all` | 混合展示 | dish_master + package_master |

### 1.2 数据库字段设计

#### 方案A：dish_master 新增 `menu_type` 字段（推荐）

在 `dish_master` 表新增字段：

| 字段名 | 类型 | 长度 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `menu_type` | varchar | 100 | `alacarte` | 菜单类型，逗号分隔支持多选。值：`alacarte`（零点）、`banquet`（宴会）、`package`（套餐） |

**数据示例**：
```sql
-- 只出现在零点菜单
menu_type = 'alacarte'

-- 出现在零点+宴会菜单
menu_type = 'alacarte,banquet'

-- 只出现在宴会菜单（宴席专用菜）
menu_type = 'banquet'

-- 出现在全部菜单
menu_type = 'alacarte,banquet,package'
```

#### 方案B：dish_category 新增 `menu_type` 字段

在 `dish_category` 表新增字段，让分类按菜单类型隔离：

| 字段名 | 类型 | 长度 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `menu_type` | varchar | 50 | `alacarte` | 分类所属菜单类型 |

**分类示例**：
- 零点菜单分类：冷菜、热菜、海鲜、肉类、禽类、汤羹、蔬菜、主食、点心、酒水、茶饮
- 宴会菜单分类：凉菜拼盘、热菜主菜、海鲜刺身、甜品、酒水、主食
- 套餐菜单分类：生日宴、婚宴、乔迁宴、寿宴、商务宴请

#### 方案C：双字段组合（最终方案）

**dish_master**：`menu_type`（标记菜品属于哪些菜单）  
**dish_category**：`menu_type`（标记分类属于哪些菜单）  

查询逻辑：
```sql
-- 获取零点菜单分类
SELECT * FROM dish_category WHERE menu_type LIKE '%alacarte%' AND is_active = 1 ORDER BY sort_order

-- 获取零点菜单下某分类的菜品
SELECT * FROM dish_master 
WHERE menu_type LIKE '%alacarte%' 
  AND category_id = ? 
  AND is_active = 1 
ORDER BY sort_order
```

---

## 二、iPad 点餐页面设计

### 2.1 页面布局（参考用户提供的UI）

```
┌─────────────────────────────────────────────────────────────────┐
│  又见炊烟私房菜 · Youjian Kitchen · Private Cuisine            │
│  ┌──────────────┬──────────────┬──────────────┐  A06 │ EN/中 │
│  │  零点·À la Carte  │  宴会·Banquet  │  全部·All    │       │
│  └──────────────┴──────────────┴──────────────┘              │
├──────────┬────────────────────────────────────────────────────┤
│          │  搜索菜品 / Search dishes...                        │
│  左侧    ├────────────────────────────────────────────────────┤
│  分类    │                                                    │
│  栏      │  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐         │
│  ┌─────┐ │  │ 菜品1 │ │ 菜品2 │ │ 菜品3 │ │ 菜品4 │         │
│  │全部 │ │  │ ¥48   │ │ ¥128  │ │ ¥28   │ │ ¥68   │         │
│  │冷菜 │ │  │  [+]  │ │  [+]  │ │  [+]  │ │  [+]  │         │
│  │热菜 │ │  └───────┘ └───────┘ └───────┘ └───────┘         │
│  │海鲜 │ │                                                    │
│  │肉类 │ │  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐         │
│  │禽类 │ │  │ 菜品5 │ │ 菜品6 │ │ 菜品7 │ │ 菜品8 │         │
│  │汤羹 │ │  │ ¥42   │ │ ¥38   │ │ ¥58   │ │ ¥88   │         │
│  │蔬菜 │ │  │  [+]  │ │  [+]  │ │  [+]  │ │  [+]  │         │
│  │主食 │ │  └───────┘ └───────┘ └───────┘ └───────┘         │
│  │点心 │ │                                                    │
│  │酒水 │ │                                                    │
│  │茶饮 │ │                                                    │
│  └─────┘ │                                                    │
├──────────┼────────────────────────────────────────────────────┤
│          │  🛒 查看点菜单  3道菜  ¥146    [提交后厨]          │
└──────────┴────────────────────────────────────────────────────┘
```

### 2.2 三种菜单的左侧分类差异

#### 零点菜单（À la Carte）分类
| 序号 | 分类名 | 图标 | 菜品数 |
|------|--------|------|--------|
| 1 | 全部 | 📋 | 34 |
| 2 | 冷菜 | 🥬 | 6 |
| 3 | 热菜 | 🔥 | 10 |
| 4 | 海鲜 | 🦐 | 6 |
| 5 | 肉类 | 🥩 | 5 |
| 6 | 禽类 | 🐔 | 3 |
| 7 | 汤羹 | 🥣 | 4 |
| 8 | 蔬菜 | 🥦 | 4 |
| 9 | 主食 | 🍚 | 3 |
| 10 | 点心 | 🥧 | 2 |
| 11 | 酒水 | 🍷 | 2 |
| 12 | 茶饮 | ☕ | 2 |

#### 宴会菜单（Banquet）分类
| 序号 | 分类名 | 图标 | 说明 |
|------|--------|------|------|
| 1 | 全部 | 📋 | 全部宴席菜品 |
| 2 | 凉菜拼盘 | 🥗 | 宴会凉菜 |
| 3 | 热菜主菜 | 🔥 | 宴会热菜 |
| 4 | 海鲜刺身 | 🦐 | 海鲜拼盘 |
| 5 | 甜品小吃 | 🍰 | 甜品 |
| 6 | 酒水饮料 | 🍷 | 酒水 |
| 7 | 主食面点 | 🍚 | 主食 |

#### 全部菜单（All）分类
| 序号 | 分类名 | 图标 | 说明 |
|------|--------|------|------|
| 1 | 全部 | 📋 | 所有菜品 |
| 2 | 推荐招牌 | ⭐ | 招牌菜 |
| 3 | 零点菜品 | 🍽️ | 日常单点 |
| 4 | 宴会套餐 | 🎉 | 宴席套餐 |
| 5 | 节日特供 | 🎄 | 时令/节日 |

### 2.3 底部订单栏设计

```
┌─────────────────────────────────────────────────────────────┐
│  🛒 查看点菜单    [菜品1 x2] [菜品2 x1] [菜品3 x1]    ¥146    │
│                                     [提交后厨] [打印预结单]   │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、后端 API 设计

### 3.1 请求头要求（强制）

```
X-Store-Id: 1
X-Staff-Id: 1001
X-Device-Sn: IPAD-A001
X-Client-Type: ipad
```

### 3.2 核心接口清单

#### 1. 获取菜品分类（按菜单类型）

```
GET /api/ipad/dish/category
参数：menu_type (可选，默认 alacarte)
返回：
{
  "code": 200,
  "message": "success",
  "data": [
    {"category_id": "C001", "category_name": "冷菜", "sort_order": 1, "dish_count": 6},
    {"category_id": "C002", "category_name": "热菜", "sort_order": 2, "dish_count": 10}
  ]
}
```

#### 2. 获取菜品列表（按分类）

```
GET /api/ipad/dish/list
参数：category_id, menu_type (可选)
返回：
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "dish_id": "D001",
      "dish_name": "蒜泥白肉",
      "dish_name_en": "Garlic Pork with Soy Sauce",
      "sale_price": 48.00,
      "image_url": "/images/dish/D001.jpg",
      "spicy_level": 2,
      "is_specialty": 1,
      "is_soldout": 0,
      "cooking_method": "凉拌"
    }
  ]
}
```

#### 3. 获取菜品详情

```
GET /api/ipad/dish/detail/{dish_id}
返回：完整菜品信息（含口味选项、规格等）
```

#### 4. 添加菜品到订单

```
POST /api/ipad/order/dish/add
Body:
{
  "table_id": "A06",
  "dish_id": "D001",
  "quantity": 2,
  "dish_note": "少辣",
  "specification": "大份"
}
返回：添加成功后的订单状态
```

#### 5. 更新订单菜品

```
PUT /api/ipad/order/dish/edit
Body:
{
  "dish_booking_id": 1001,
  "quantity": 3,
  "dish_note": "微辣"
}
```

#### 6. 删除订单菜品

```
DELETE /api/ipad/order/dish/remove/{dish_booking_id}
```

#### 7. 提交后厨

```
POST /api/ipad/order/send-kitchen
Body:
{
  "table_id": "A06",
  "order_note": "加急"
}
```

#### 8. 桌台列表

```
GET /api/ipad/table/list
返回：桌台列表（含状态）
```

#### 9. 开台

```
POST /api/ipad/table/open
Body:
{
  "table_id": "A06",
  "guest_count": 4,
  "customer_name": "张先生",
  "customer_phone": "13800138000"
}
```

### 3.3 响应格式统一

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code=200`：成功
- `code=400`：参数错误
- `code=401`：未授权（缺少请求头）
- `code=404`：资源不存在
- `code=500`：服务器错误

---

## 四、前端页面规划

### 4.1 页面清单

| 页面 | 路径 | 功能 |
|------|------|------|
| iPad点餐首页 | `/dashboard/ipad` | 桌台选择、菜单切换 |
| iPad菜单页面 | `/dashboard/ipad/menu` | 点菜核心页面（分类+菜品+订单） |
| iPad订单详情 | `/dashboard/ipad/order` | 当前桌台订单详情 |
| iPad结算页面 | `/dashboard/ipad/checkout` | 结账、支付 |

### 4.2 组件规划

| 组件 | 文件 | 功能 |
|------|------|------|
| `IpadMenu.vue` | views/dashboard/ | iPad菜单主页面 |
| `DishCategory.vue` | components/ipad/ | 左侧分类栏 |
| `DishGrid.vue` | components/ipad/ | 右侧菜品网格 |
| `OrderBar.vue` | components/ipad/ | 底部订单栏 |
| `DishDetail.vue` | components/ipad/ | 菜品详情弹窗 |
| `TableSelector.vue` | components/ipad/ | 桌台选择组件 |

---

## 五、实施计划

### 阶段1：字段设计确认（今日）
- [ ] 确认 `menu_type` 字段方案
- [ ] 确认三种菜单的分类结构

### 阶段2：后端API开发（预计1天）
- [ ] 创建 `IpadController.java`
- [ ] 实现菜品分类接口
- [ ] 实现菜品列表接口
- [ ] 实现订单菜品增删改接口
- [ ] 实现桌台管理接口
- [ ] 实现提交后厨接口

### 阶段3：前端页面开发（预计2天）
- [ ] 创建 `IpadMenu.vue` 主页面框架
- [ ] 实现 `DishCategory.vue` 分类栏（动态加载）
- [ ] 实现 `DishGrid.vue` 菜品网格
- [ ] 实现 `OrderBar.vue` 订单栏
- [ ] 实现 `DishDetail.vue` 详情弹窗
- [ ] 实现三种菜单切换逻辑
- [ ] 实现搜索功能

### 阶段4：联调测试（预计1天）
- [ ] 端到端测试点餐流程
- [ ] 测试三种菜单分类切换
- [ ] 测试桌台管理功能
- [ ] 测试提交后厨功能

### 阶段5：部署上线（预计半天）
- [ ] 构建部署
- [ ] iPad端验证

---

## 六、待确认事项

1. **`menu_type` 字段方案**：选择方案A（仅dish_master）、方案B（仅dish_category）、还是方案C（双字段组合）？

2. **分类结构**：三种菜单的分类是否按上述设计？如有调整请指出。

3. **菜品图片**：现有数据库的 `image_url` 字段是否有效？需要统一图片存储路径吗？

4. **规格选项**：部分菜品是否有规格（大份/小份）、口味选项？需要在接口中支持吗？

5. **订单状态**：订单状态流转是怎样的？（开台→加菜→提交后厨→上菜→结账→关闭）

---

## 七、参考资料

- iPad开发规划文档：`/mnt/cos/天地双龙工作空间/项目管理/餐饮管理系统/iPad分系统-三人分工（修订版）.md`
- API接口文档：`/mnt/cos/天地双龙工作空间/项目管理/餐饮管理系统/iPad分系统-API接口详细文档（基于真实数据库）.md`
- 数据库表结构：`dish_master`、`dish_category`、`package_master`、`banquet_template`、`booking_dish_detail`

