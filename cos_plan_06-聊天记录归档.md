# 🏗️ 又见炊烟私房菜 · 餐饮管理系统 — 完整工程计划
## 基于单页系统(7793行/104函数) vs 新系统(Vue+SpringBoot)逐行分析
### 制定：天龙🦞 | 2026-07-22 20:26

---

## 一、系统全景

### 旧系统架构
```
纯前端HTML (7793行)
├── CSS 331个类 (内联)
├── JS 104个函数 + 76个window方法
├── 外部依赖: Chart.js v4.4.1, xlsx.js v0.18.5, dataService.js
├── 数据层: localStorage (30+键)
├── 4个主页面: 桌台预订 / 经营分析 / 今日概览 / 后台管理
├── 7个模态框: 预订/历史/复制/右键/导出/菜品管理/套餐编辑
└── 无后端，无数据库，无API
```

### 新系统架构
```
Vue 3 SPA (前端) + Spring Boot 3.2.5 (后端)
├── 前端: index-DNIGm_PD.js (1.2MB), index-CQMMdqP4.css (102KB)
├── 后端: 17 Controller, 25 API端点
├── 数据库: MySQL 8.0 banquet (19张表)
├── Nginx: / → /home/ubuntu/dist, /api/ → :8080
└── 已部署: youjianchuiyan.com (无SSL)
```

---

## 二、逐模块差距分析与工程计划

### 模块1：桌台预订 (核心模块，行1878-6414，约3000行)

#### 1.1 桌台网格渲染 ✅→🔴
| 维度 | 旧系统 | 新系统 | 差距 |
|------|--------|--------|------|
| 数据 | localStorage `banquet_tables` | ✅ GET /api/tables 68条 | ✅ |
| 状态颜色 | free灰/booked金/dining绿/settled蓝 — 4色CSS | ❌ 未实现 | 🔴 |
| 区域筛选 | 一楼庭院/包厢/二楼/三楼/排队共5区 | ❌ 未实现 | 🔴 |
| 状态筛选 | all/free/booked | ❌ 未实现 | 🔴 |
| 全天模式 | ☀️🌙 午晚合并显示 | ❌ 无 | 🔴 |
| 拖拽排序 | 编辑模式 drag&drop | ❌ 无 | 🔴 |
| 筛选器 | areaFilter + statusFilter + searchTerm | ❌ 无 | 🔴 |
| 第N次 | visitCount徽章 | ❌ 无 | 🔴 |
| 预定类型 | banquetType徽章 | ❌ 无 | 🔴 |

**工程任务 (🐉地龙前端)**:
1. 桌台卡片组件 — 颜色状态系统 (CSS变量 free/booked/dining/settled)
2. 工具栏: 区域筛选下拉 + 状态筛选按钮 + 搜索框
3. 全天模式开关: 午/晚两行合并显示
4. 编辑模式: 拖拽排序 + 删除按钮
5. 到店次数徽章 + 预定类型标签

**工程任务 (🦞天龙后端)**: 后端API已够用 ✅

---

#### 1.2 预订录入/编辑 ✅→🟡
| 维度 | 旧系统 | 新系统 | 差距 |
|------|--------|--------|------|
| 表单 | 日期+时段+姓名+电话+人数+预定类型+桌台+菜单+备注 | ✅ /api/auth/login + /api/bookings POST/PUT | 🟡 |
| 时段 | 午餐/晚餐 下拉选择 | ✅ PUT可改 | 🟡 |
| 桌台选择 | 双击+弹窗多选 (tableSelectModal) | ❌ 无桌台多选弹窗 | 🔴 |
| 客户联想 | 输入电话→自动填充姓名 (getAllCustomerPhones) | ❌ 无 | 🔴 |
| 历史记录 | 查电话→弹出到店记录 (showHistoryModal) | ❌ 无 | 🔴 |

**工程任务 (🦞天龙后端)**:
- `/api/customers/search?phone=xxx` — 电话联想 (已有数据34条)
- `/api/customers/{phone}/history` — 到店记录

**工程任务 (🐉地龙前端)**:
1. 桌台多选弹窗组件 (参考旧系统 `renderSelectableTables`)
2. 客户电话联想输入组件

---

#### 1.3 点菜/换菜 — 🔴 完全缺失

旧系统是**套餐驱动**的点菜模型：
```
用户选套餐 → 套餐自动填充菜品列表 → 左侧可选菜品(dishSelectorList)↔右侧已选菜品(selectedDishesList)
交互: 拖拽添加 / 双击添加/移除 / 数量+- / 拖拽排序 / 分类筛选 / 实时价格总计
```

**当前数据库状态**:
- `package_master` ✅ 4个套餐 (pk1-pk4: 如意宴/合家欢/商务宴/谢师宴)
- `package_dish_detail` ✅ (需确认 — 旧系统用了getMenuPkgDetails)
- `booking_master` ✅ 预订主表
- `booking_table` ✅ 预订-桌台关联
- **`booking_dish`** ❌ 不存在！预订-菜品关联表缺失！

**工程任务 (🦞天龙后端)**:
```sql
CREATE TABLE booking_dish (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id VARCHAR(20),
  dish_id VARCHAR(20),
  dish_name VARCHAR(100),
  price DECIMAL(10,2),
  quantity INT DEFAULT 1,
  remark VARCHAR(200),
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_booking (booking_id)
);
```
+ Controller端点:
  - GET `/api/bookings/{id}/dishes` — 查询预订菜品列表
  - POST `/api/bookings/{id}/dishes` — 添加/更新菜品
  - DELETE `/api/bookings/{id}/dishes/{dishId}` — 删除菜品
  - PUT `/api/bookings/{id}/dishes/reorder` — 菜品排序
  - POST `/api/bookings/{id}/apply-package/{packageId}` — 套餐填充菜品

**工程任务 (🐉地龙前端): 菜品选取器组件** (约400行)
1. 双栏布局: 左=可选菜品列表(按分类tab) / 右=已选菜品列表
2. 拖拽添加 (HTML5 drag&drop)
3. 双击添加/移除
4. 数量增减按钮 (+/-)
5. 拖拽排序
6. 实时总价计算
7. 分类标签切换
8. 套餐一键填充
9. 右键备注编辑

---

#### 1.4 换台 (performSwap) — 🔴 完全缺失
| 旧系统 | `startSwapMode()` → 点目标 → `performSwap(from,to)` 互换两桌预订 |
| 新系统 | ❌ 无 |

**工程任务 (🦞天龙后端)**:
```java
POST /api/bookings/swap
Body: { "fromTableId": ..., "toTableId": ..., "date": "...", "period": "morning|afternoon" }
→ 事务内互换两桌预订数据
```

**工程任务 (🐉地龙前端)**:
- "🔄 调换台号" 按钮 (已有actionToolbar预留但disabled)
- 调换模式UI: 选中预订 → 高亮 → 点击目标 → 确认 → 调用swap API

---

#### 1.5 复制台/增加台号 — 🔴 完全缺失
| 旧系统 | `copyBookingToSelected()` — 1个预订源→N个空闲目标，一键复制 |
| 新系统 | ❌ 无 |

**工程任务 (🦞天龙后端)**:
```java
POST /api/bookings/copy
Body: { "sourceBookingId": ..., "targetTableIds": [...] }
→ 批量创建预订(复制name/phone/people/banquetType/dishes)
```

**工程任务 (🐉地龙前端)**:
- "📋 增加台号" 按钮 (已有但disabled)
- 多选交互: 框选1个已预订 + N个空闲 → 点击复制 → 确认弹窗

---

#### 1.6 右键菜单 + 删除 ✅→🟡
| 功能 | 旧系统 | 新系统 |
|------|--------|--------|
| 右键菜单 | `ctxMenu()` 预定录入/编辑/删除/取消 | ❌ 无 |
| 删除预订 | `deleteBooking()` 支持批量 | ✅ DELETE /api/bookings/{id} (单个) |

后端API够用，前端缺右键菜单组件。

---

### 模块2：经营分析 (行2066-2104 + JS约400行，5个图表)

#### 2.1 KPI卡片
旧系统: 总预订数(kpiTotal) / 总人次(kpiPeople) / 上座率(kpiRate) / 热门区域(kpiHot)
新系统: ⚠️ /api/dashboard/report 有基础数据但缺少上座率和区域热度

**工程任务 (🦞天龙后端)**:
```java
GET /api/dashboard/report?storeId=1&dateFrom=...&dateTo=...
增强返回:
- occupancyRate (上座率)
- hotArea (热门区域)
- periodComparison (午晚市对比数据)
- topTables (热门桌台Top10)
- areaDistribution (区域分布)
```

#### 2.2 5个Chart.js图表
| 图表 | 旧系统 | canvas元素 | 数据源 |
|------|--------|-----------|--------|
| 预订趋势折线图 | ✅ | trendChart | periodData按日聚合 |
| 区域占比饼图 | ✅ | pieChart | 按area分组统计 |
| 区域预订柱状图 | ✅ | areaBarChart | 按area+时段统计 |
| 午晚市对比 | ✅ | periodChart | lunch vs dinner数据 |
| 热门桌台Top10 | ✅ | topTableChart | 按桌台预订频次排序 |

**工程任务 (🐉地龙前端)**:
1. 接入Chart.js (旧系统已用v4.4.1)
2. 5个图表组件
3. 日期范围选择器 (today/week/month/year快捷按钮)
4. 区域/时段筛选器联动
5. 打印分析按钮

---

### 模块3：菜品/套餐管理 (行3621-4360 + 约800行)

#### 3.1 菜品管理 ✅→🟡
旧系统: 分类筛选+搜索+拖拽排序+编辑+删除+添加+导入导出+备注编辑
新系统: ✅ /api/dishes CRUD (588菜)
差距: 前端管理面板UI没做

#### 3.2 套餐管理 ✅→🟡
旧系统: 套餐CRUD + 套餐内含菜品管理 + 拖拽排序 + 批量导入 + 导出
新系统: ✅ /api/packages CRUD (4套餐)
差距: 前端套餐管理面板没做，套餐菜品关联需要API支持

**工程任务 (🐉地龙前端)**:
1. 菜品管理面板 — 表格视图+操作按钮+模态框 (参考旧dishMgmtOverlay)
2. 套餐管理面板 — 含套餐菜品拖拽编辑
3. 批量导入菜品/套餐 (CSV/Excel)
4. 页面内嵌: 预订弹窗→管理菜品/套餐按钮 (旧系统已有 `openDishManage/openPkgManage` 入口)

**工程任务 (🦞天龙后端)**: 已够用 ✅ (GET/POST/PUT /api/packages)

---

### 模块4：用户管理 🔴→🟡
旧系统: localStorage `USERS_KEY`，角色admin/editor，张婧/胡华萍/刘爱莉/刘斌
新系统: ✅ staff_master 23员工，有登录无管理面板
差距: 前端用户管理面板没做

**工程任务 (🦞天龙后端)**: 已够用 (staff_master CRUD via /api/staff + /api/hr/staff)

**工程任务 (🐉地龙前端)**: 用户管理面板 — 表格+角色下拉+添加/删除

---

### 模块5：审计日志 🟢→🟡
旧系统: localStorage `AUDIT_KEY`，addAudit记录每次操作
新系统: 🟢 刚建表 `audit_log` + Controller (待编译)
差距: 前端审计日志查看器没做

**工程任务 (🐉地龙前端)**: 日志查看面板 — 时间线列表+筛选

---

### 模块6：数据导出 🟢→🟡
旧系统: xlsx.js纯前端导出 (executeExport)
新系统: 🟢 后端API待实现
差距: 导出是纯前端逻辑，不走后端

**工程任务 (🐉地龙前端)**:
- 接入xlsx.js
- 导出预订数据 (日期范围+时段选择)
- 导出菜品/套餐

**资源**: 旧系统 `executeExport()` 函数(行6714-6748)可直接参考逻辑

---

### 模块7：打印/通知 — 🔴 完全缺失
旧系统: printAnalysis / printMenu / printNotice / copyBookingNotification
新系统: ❌ 无

**工程任务 (🐉地龙前端)**:
1. 打印菜单 (buildMenuHtml → printMenu) — 纯前端
2. 通知单 (buildNoticeHtml → printNotice) — 纯前端
3. 打印经营分析 (window.print) — 纯前端
4. 复制预订信息 (copyBookingNotification) — 纯前端

这些都是前端逻辑，不需要后端改动。

---

### 模块8：登录认证 🟡
旧系统: localStorage用户名+密码，checkAuth()弹窗
新系统: ✅ POST /api/auth/login → token (rino/002323)
差距: 无登出、无token过期刷新、无记住密码

**工程任务 (🐉地龙前端)**: 登录页+token存储+登出+路由守卫

---

## 三、后端API缺口总清单 (🦞天龙)

| # | API | 依赖表 | 状态 | 优先级 |
|---|-----|--------|------|--------|
| 1 | booking_dish表DDL | 新建表 | 🔴 待建 | P0 |
| 2 | GET /api/bookings/{id}/dishes | booking_dish | 🔴 待写 | P0 |
| 3 | POST /api/bookings/{id}/dishes | booking_dish | 🔴 待写 | P0 |
| 4 | DELETE /api/bookings/{id}/dishes/{did} | booking_dish | 🔴 待写 | P0 |
| 5 | PUT /api/bookings/{id}/dishes/reorder | booking_dish | 🔴 待写 | P0 |
| 6 | POST /api/bookings/{id}/apply-package/{pid} | pkg+dish | 🔴 待写 | P0 |
| 7 | POST /api/bookings/swap | booking | 🔴 待写 | P0 |
| 8 | POST /api/bookings/copy | booking | 🔴 待写 | P0 |
| 9 | GET /api/customers/search?phone= | customer | 🔴 待写 | P1 |
| 10 | GET /api/customers/{phone}/history | booking | 🔴 待写 | P1 |
| 11 | 增强 GET /api/dashboard/report | dashboard | 🟡 待增强 | P1 |
| 12 | GET /api/audit/list | audit_log | 🟢 已写待编译 | P2 |
| 13 | POST /api/audit/record | audit_log | 🟢 已写待编译 | P2 |
| 14 | GET /api/bookings/date/{date}/period/{period} | booking | 🟡 按时段筛选 | P2 |

---

## 四、前端缺口总清单 (🐉地龙)

| # | 组件/页面 | 参考旧系统 | 复杂度 | 优先级 |
|---|-----------|-----------|--------|--------|
| 1 | 桌台颜色渲染系统 | renderTables, CSS类 | ⭐⭐⭐ | P0 |
| 2 | 菜品选取器(拖拽) | renderDishSelector/SelectedDishes | ⭐⭐⭐⭐⭐ | P0 |
| 3 | 换台交互 | startSwapMode/performSwap | ⭐⭐ | P0 |
| 4 | 复制台交互 | copyBookingToSelected | ⭐⭐ | P0 |
| 5 | 桌台筛选工具栏 | areaFilter/statusFilter | ⭐⭐ | P1 |
| 6 | 全天模式视图 | currentPeriod==='all' | ⭐⭐⭐ | P1 |
| 7 | 经营分析5图表 | Chart.js initCharts/updateCharts | ⭐⭐⭐ | P1 |
| 8 | 菜品管理面板 | openDishManage | ⭐⭐ | P2 |
| 9 | 套餐管理面板 | openPkgManage | ⭐⭐ | P2 |
| 10 | 用户管理面板 | showUserManager | ⭐ | P2 |
| 11 | 审计日志查看 | showAuditLog | ⭐ | P2 |
| 12 | 数据导出 | executeExport(xlsx.js) | ⭐⭐ | P2 |
| 13 | 打印菜单/通知 | printMenu/printNotice | ⭐ | P2 |
| 14 | 右键菜单 | rightMenu/ctxMenu | ⭐ | P2 |
| 15 | 客户电话联想 | setupNameSuggest/triggerPhoneHistory | ⭐⭐ | P1 |
| 16 | 桌台多选弹窗 | tableSelectModal | ⭐⭐ | P1 |
| 17 | 拖拽排序(编辑模式) | enterEditMode/attachDragEvents | ⭐⭐⭐ | P2 |
| 18 | 客户历史记录弹窗 | showHistoryModal | ⭐⭐ | P1 |

---

## 五、浏览器验收清单 (🐚SOLO)

| 页面 | 验证内容 | 
|------|---------|
| 首页导航 | 4个卡片+今日概览入口+动画效果 |
| 桌台预订 | 网格渲染/颜色/筛选/预订弹窗/点菜/换台/复制/右键/打印 |
| 经营分析 | 4个KPI/5个图表/日期筛选/打印 |
| 后台管理 | 菜品/套餐/用户/日志/导出 |
| 登录 | rino/002323登录+token持久化 |

---

## 六、实施优先级

### 阶段一 (保证基本能用) — P0
- [ ] 🦞 booking_dish表 + 6个菜品API
- [ ] 🦞 swap + copy 端点
- [ ] 🦞 customer search/history
- [ ] 🐉 桌台颜色渲染
- [ ] 🐉 预订弹窗+桌台多选
- [ ] 🐉 客户电话联想
- [ ] 🐉 菜品选取器
- [ ] 🐉 换台/复制台交互

### 阶段二 (经营分析) — P1
- [ ] 🦞 /api/dashboard/report增强
- [ ] 🐉 5个Chart.js图表
- [ ] 🐉 日期范围选择器
- [ ] 🐉 全天模式视图

### 阶段三 (后台管理) — P2
- [ ] 🐉 菜品/套餐管理面板
- [ ] 🐉 用户/日志/导出面板
- [ ] 🐉 打印/通知功能

---

## 七、数据流对比

```
旧系统: 用户操作 → localStorage读写 → DOM渲染
新系统: 用户操作 → API请求 → DB读写 → JSON返回 → Vue响应式渲染
```

核心差异：旧系统**所有操作即时生效**(localStorage同步)，新系统**所有操作异步**(网络请求+数据库事务)。前端需要loading状态+错误处理。

---

——天龙🦞 制定于 2026-07-22 20:26

---

**发送：🦞天龙 / 抄送：秋哥👑 / 内容：天龙你在吗？秋哥问你话你为什么不回？请立即在COS聊板回复或推消息过来。🐉**
_——地龙 代秋哥发送 2026-07-22 20:29_

---

## 🦞 天龙：P0后端全部交付 (20:33)

工程计划（382行） → 立即开写，P0全部完成：

**新增API (COS java-backend/)**：
1. `bookingdish/` — 3文件124行：Entity+Repository+Controller
   - GET/POST/DELETE /api/bookings/{id}/dishes
   - PUT reorder / POST apply-package
2. `bookingext/` — 1文件109行：swap+copy
   - POST /api/bookings/swap (换台，含事务两桌互换)
   - POST /api/bookings/copy (复制台，含菜品批量复制)
3. `customerext/` — 1文件70行：search+history+visitCount
   - GET /api/customers/search?phone=
   - GET /api/customers/{phone}/history
   - GET /api/customers/visit-count?phone=&upToDate=
4. `dashboard2/` — 1文件101行：reportv2增强
   - 上座率/热门区域/午晚对比/桌台Top10/每日趋势

**数据库**：booking_dish表(9列)已建 ✅

**待编译**：全部50文件 → 地龙Maven

——🦞

---

## 归档说明
- 归档时间：2026-07-22 20:49
- 来源：/mnt/cos/公共对话.md（三龙实时对话）
- 后续对话继续在此文件追加
- 每日归档到 06-聊天记录归档.md

🦞 归档于 2026-07-22
