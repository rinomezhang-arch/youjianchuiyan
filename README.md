# 又见炊烟餐饮管理系统

> 私厨 · 宴会 · 臻选

基于 Vue 3 + Element Plus 构建的餐饮管理系统，支持多门店运营、客户点菜、后台管理等全业务流程。

## 🛠 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue | 3.x |
| 路由 | Vue Router | 4.x |
| 状态管理 | Pinia | 2.x |
| UI组件库 | Element Plus | 2.x |
| 构建工具 | Vite | 6.x |
| 语言 | JavaScript | ES6+ |

## 📁 项目结构

```
frontend_v3/                           # 前端项目根目录
├── src/                               # 源代码目录
│   ├── api/                           # API接口封装
│   │   ├── auth.js                    # 认证相关接口
│   │   ├── booking.js                 # 宴会预定接口
│   │   ├── customer.js                # 客户管理接口
│   │   ├── dish.js                    # 菜品管理接口
│   │   ├── hr.js                      # 人力资源接口
│   │   ├── package.js                 # 套餐管理接口
│   │   ├── table.js                   # 桌台管理接口
│   │   └── http.js                    # HTTP请求封装
│   ├── assets/                        # 静态资源
│   │   ├── images/                    # 图片资源
│   │   │   └── logo.png               # 系统Logo
│   │   ├── styles/                    # 全局样式
│   │   │   └── global.css             # 全局CSS
│   │   └── main.css                   # 主样式文件
│   ├── components/                    # 公共组件
│   │   ├── AIChatFloat.vue            # AI助手悬浮窗
│   │   ├── BookingDialog.vue          # 预定弹窗
│   │   ├── DishOrderDialog.vue        # 菜品下单弹窗
│   │   ├── PrintButton.vue            # 打印按钮
│   │   └── PrintPreview.vue           # 打印预览
│   ├── i18n/                          # 国际化
│   │   ├── lang/                      # 语言包
│   │   │   ├── en.js                  # 英文
│   │   │   └── zh.js                  # 中文
│   │   └── index.js                   # i18n配置
│   ├── router/                        # 路由配置
│   │   └── index.js                   # 路由定义
│   ├── store/                         # Pinia状态管理
│   │   └── user.js                    # 用户状态
│   ├── utils/                         # 工具函数
│   │   ├── menuStore.js               # 菜单存储
│   │   └── request.js                 # 请求工具
│   ├── views/                         # 页面视图
│   │   ├── dashboard/                 # 仪表盘页面
│   │   │   ├── AiAssistant.vue        # AI助手页面
│   │   │   ├── Bookings.vue           # 宴会预定看板
│   │   │   ├── Customers.vue          # 客户管理
│   │   │   ├── Finance.vue            # 财务总账看板
│   │   │   ├── GMOffice.vue           # 总经理总驾驶舱
│   │   │   ├── HRAdmin.vue            # 人事管理
│   │   │   ├── Inventory.vue          # 库存管理
│   │   │   ├── IpadMenu.vue           # iPad点菜页面
│   │   │   ├── Kitchen.vue            # 后厨管理
│   │   │   ├── Menu.vue               # 菜品管理
│   │   │   ├── MenuManage.vue         # 菜单管理
│   │   │   ├── Procurement.vue        # 采购审批
│   │   │   ├── Reports.vue            # 报表中心
│   │   │   ├── Revenue.vue            # 营收看板
│   │   │   ├── Safety.vue             # 安全管理
│   │   │   ├── Settings.vue           # 系统设置
│   │   │   ├── Staff.vue              # 员工管理
│   │   │   ├── SupplyChain.vue        # 供应链看板
│   │   │   ├── TableBoard.vue         # 桌台看板
│   │   │   ├── Waste.vue              # 损耗管理
│   │   │   └── ...                    # 其他功能页面
│   │   ├── Dashboard.vue              # 仪表盘主页面
│   │   ├── Login.vue                  # 登录页面
│   │   ├── StoreSelect.vue            # 门店选择页面
│   │   ├── TableSelect.vue            # 桌台选择页面
│   │   └── Welcome.vue                # 欢迎页面（客户入口）
│   ├── App.vue                        # 根组件
│   └── main.js                        # 入口文件
├── index.html                         # HTML模板
├── package.json                       # 依赖配置
├── vite.config.js                     # Vite配置
└── .gitignore                         # Git忽略配置
```

## 🎯 功能模块

### 1. 总经理总驾驶舱
- 经营指标卡片（营收、客流、翻台率、毛利率）
- 预定看板（包厢预定、宴席预定、空包厢预警）
- 待办审批（采购、请假、维修、报销、对账）
- 风险预警（食材临期、卫生不合格、消防隐患、能耗异常）

### 2. 业务审批中心
- 采购审批看板
- 人事请假审批看板
- 维修/报修审批看板
- 费用报销审批看板
- 供应商对账确认看板

### 3. 门店经营业务看板
- 宴席包厢预定看板（日历视图、预定明细、预警提醒）
- 营收数据看板（堂食、外卖、团购、宴席收入拆分）
- 菜品销售毛利看板（热销/滞销菜品、单品毛利统计）

### 4. 安全·卫生·能耗管控看板
- 卫生巡检看板
- 安全巡检看板
- 能源能耗看板

### 5. 供应链·库存·应付账款看板
- 采购入库看板
- 实时库存看板
- 损耗报废看板
- 供应商应付看板

### 6. 人事考勤看板
- 员工基础台账
- 考勤统计看板
- 工资核算看板

### 7. 财务总账看板
- 收支流水看板
- 月度利润看板
- 票据税务台账

### 8. 客户点菜系统
- 门店选择弹窗
- 桌台选择弹窗
- iPad全屏点菜模式
- 员工卡号验证

## 🚀 快速开始

### 安装依赖

```bash
cd frontend_v3
npm install
```

### 开发模式

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 📝 数据流转路由

### 采购流程
门店提交采购申请 → 审批中心审核 → 通过后同步入库台账 → 成本同步至财务总账

### 请假流程
员工提交请假单 → 审批中心审核 → 通过后同步考勤系统 → 月末自动核算工资

### 预定流程
前台录入预定 → 同步总驾驶舱和经营看板 → 推送后厨备货提醒 → 定金同步财务营收

### 对账流程
供应商送货单录入 → 审批中心对账确认 → 同步应付账款 → 付款完成更新余额

## 🔒 权限管理

| 角色 | 权限范围 |
|------|----------|
| 总经理 | 全部8大模块，全门店数据，审批终审 |
| 门店店长 | 本店经营、预定、库存、巡检，提交采购/请假/报修 |
| 后厨主管 | 库存、损耗、采购提交 |
| 前厅接待 | 预定录入、本店营收查看 |

## 👥 团队分工

### 成员 A（负责人/总经理视角）
**职责范围**：核心业务模块 + 总驾驶舱

| 模块 | 页面文件 | 说明 |
|------|----------|------|
| 总经理总驾驶舱 | `GMOffice.vue` | 经营指标、预定看板、待办审批、风险预警 |
| 业务审批中心 | `Procurement.vue`, `Leave.vue`, `Maintenance.vue` | 采购/请假/维修/报销/对账审批 |
| 宴会预定看板 | `Bookings.vue` | 日历视图、预定明细、预警提醒 |
| 桌台管理 | `TableBoard.vue` | 桌台布局、状态管理 |
| 系统设置 | `Settings.vue` | 组织架构、权限配置 |

### 成员 B（运营/财务视角）
**职责范围**：经营数据 + 财务模块

| 模块 | 页面文件 | 说明 |
|------|----------|------|
| 营收数据看板 | `Revenue.vue` | 堂食/外卖/团购/宴席收入拆分 |
| 菜品销售毛利 | `Menu.vue`, `DishCost.vue` | 热销/滞销菜品、单品毛利 |
| 财务总账看板 | `Finance.vue` | 收支流水、月度利润、票据税务 |
| 供应链管理 | `SupplyChain.vue`, `Inventory.vue` | 采购入库、实时库存、应付账款 |
| 报表中心 | `Reports.vue` | 各类报表生成与导出 |

### 成员 C（门店/人事视角）
**职责范围**：门店运营 + 人事管理 + 客户点菜

| 模块 | 页面文件 | 说明 |
|------|----------|------|
| 人事考勤看板 | `HRAdmin.vue`, `Attendance.vue`, `Payroll.vue` | 员工台账、考勤统计、工资核算 |
| 安全卫生能耗 | `Safety.vue`, `Hygiene.vue`, `Energy.vue` | 安全巡检、卫生检查、能耗管控 |
| 后厨管理 | `Kitchen.vue`, `Waste.vue` | 损耗报废、后厨管理 |
| 客户点菜系统 | `Welcome.vue`, `IpadMenu.vue` | 门店选择、桌台选择、iPad点菜 |
| 客户管理 | `Customers.vue` | 客户信息、会员管理 |

### 协作规范

1. **分支管理**：每人维护独立分支，合并前需代码审查
2. **提交规范**：`feat: 添加功能`、`fix: 修复bug`、`docs: 更新文档`、`style: 样式调整`
3. **代码审查**：合并到 main 分支前需至少一人 review
4. **每日同步**：每日提交更新，保持代码最新

## 📄 备案信息

苏公网安备32132302010492号

[备案查询](https://beian.mps.gov.cn/#/query/webSearch?code=32132302010492)

## 📞 联系方式

如有问题请联系项目维护者。

---

© 2026 又见炊烟餐饮管理系统
