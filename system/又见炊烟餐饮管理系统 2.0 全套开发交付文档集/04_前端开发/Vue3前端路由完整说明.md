# Vue3 前端路由完整说明

> 适用：`frontend_v3/src/router/index.js`  
> 维护：Trae（TRAE-BOT）/ 地龙（DL-BOT）  
> 更新：2026-08-02

---

## 1. 路由配置

### 1.1 主路由文件
- 主应用：`frontend_v3/src/router/index.js`
- iPad：`frontend_v3/src/router/ipad.js`

### 1.2 路由守卫

```js
router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (!token) return next({ path: '/login', query: { redirect: to.fullPath } })
    if (!userStore.initialized) await userStore.init()
    if (!userStore.isLoggedIn) return next({ path: '/login', query: { redirect: to.fullPath } })
    next()
  } else {
    next()
  }
})
```

## 2. 路由清单（按一级模块）

| 一级模块 | 路由 | 页面组件 | 鉴权 | 备注 |
|---------|------|---------|------|------|
| **登录** | `/login` | `Login.vue` | 否 | Enter 键导航 |
| **门店选择** | `/store-select` | `StoreSelect.vue` | 是 | 多门店切换 |
| **欢迎页** | `/` | `Welcome.vue` | 否 | 星云+流星动画 |
| **工作台** | `/dashboard/home` | `Home.vue` | 是 | 经营总览 |
| **桌台看板** | `/dashboard/table-board` | `TableBoard.vue` | 是 | 桌台可视化 |
| **预订管理** | `/dashboard/bookings` | `Bookings.vue` | 是 | 宴会预订 |
| **菜单系统** | `/dashboard/menu` | `MenuHub.vue` | 是 | 11 子模块入口 |
| ├ 点菜 | `menu/ordering` | `IpadMenu.vue` | 是 | 美团风格卡片 |
| ├ 菜库编辑 | `dish-library` | `DishLibrary.vue` | 是 | |
| ├ 成本配方 | `cost-recipe` | `CostRecipe.vue` | 是 | |
| ├ 套餐管理 | `set-menu` / `set-menu-edit` | `SetMenu*.vue` | 是 | 价格联动 |
| ├ 调价管理 | `pricing-manage` | `PricingManage.vue` | 是 | |
| ├ 沽清管控 | `soldout-control` | `SoldoutControl.vue` | 是 | |
| ├ 标签管理 | `tags` | `Tags.vue` | 是 | |
| ├ 打印配置 | `print-config` | `PrintConfig.vue` | 是 | |
| ├ 门店权限 | `store-permission` | `StorePermission.vue` | 是 | |
| ├ 多价格体系 | `price-tiers` | `PriceTiers.vue` | 是 | |
| └ 操作日志 | `audit-log` | `AuditLog.vue` | 是 | |
| **前厅运营** | `front-office` / `front-desk` | `FrontOffice.vue` | 是 | |
| **厨房出品** | `kitchen` / `kitchen-log` | `Kitchen.vue` | 是 | KDS |
| **楼面工程** | `floor-project` / `decoration` | - | 是 | 装修管理 |
| **采购仓储** | `supply-chain` 等 6 子模块 | - | 是 | 采购/入库/调拨/盘点 |
| **财务管理** | `finance` + 5 子模块 | `Finance.vue` | 是 | 账户/应付/费用/凭证/报表 |
| **菜品成本** | `finance/dish-cost` 等 | `DishCost*.vue` | 是 | |
| **人事行政** | `hr-admin` 11 子模块 | - | 是 | 员工/培训/考勤/排班/工资 |
| **客户管理** | `customers` | `Customers.vue` | 是 | |
| **营销会员** | `marketing` / `member-list` | `Marketing.vue` | 是 | |
| **数据报表** | `reports` / `data-screen` | - | 是 | 含大屏 |
| **系统设置** | `settings` 6 子模块 | - | 是 | 信息/权限/组织/配置/帮助 |
| **工程能耗** | `engineering` / `energy` / `safety` | - | 是 | |
| **审批中心** | `approval` / `review-queue` | `Approval.vue` | 是 | |
| **权限管理** | `perm-manager` | `PermManager.vue` | 是 | |
| **数据导出** | `export-panel` | `ExportPanel.vue` | 是 | |

## 3. 路由元信息

```js
{
  path: '/dashboard/bookings',
  name: 'bookings',
  component: () => import('@/views/Bookings.vue'),
  meta: {
    requiresAuth: true,           // 鉴权标志
    title: '预订管理',            // 页面标题
    icon: 'calendar',            // 菜单图标
    permission: 'booking:list',  // 接口权限点（对接 RBAC）
    keepAlive: true              // 是否缓存组件
  }
}
```

## 4. 路由懒加载

所有页面组件使用动态 import，按需加载：
```js
component: () => import('@/views/Bookings.vue')
```

打包后每个页面单独 chunk，首屏只加载当前路由。

## 5. 关键交互

### 5.1 登录表单 Enter 键导航
- 输入用户名 → Enter → 焦点跳到密码框
- 输入密码 → Enter → 提交登录

### 5.2 IpadMenu.vue
- 拖拽加菜到购物车
- 双击购物车项减数量
- 悬浮显示蓝色加号
- 购物车面板 480px 独立滚动条

### 5.3 SetMenuEdit.vue 价格联动
- 价格/折扣/成本率三联动
- 用 `lastEditSource` 标志防止循环更新
- 计算公式：
  - `discount = price / originalPrice × 100`
  - `costRate = cost / price × 100`
  - `price = originalPrice × discount / 100`（折扣变时）
  - `price = cost / costRate × 100`（成本率变时）

### 5.4 数值输入
- `el-input-number` 左 `-` 右 `+`
- 百分比 `min=0 max=100 step=1`

### 5.5 只读字段
- `readonly` 属性 + `dblclick` 切换为可编辑
- 时间字段：灰色文字 → 双击 → `el-date-picker`
- 制作人字段：灰色文字 → 双击 → 可搜索下拉

## 6. 路由配置约定

1. **新增路由**：必须加 `meta.requiresAuth=true`（除 `/login` `/` 等公开页）
2. **权限点**：必须加 `meta.permission` 对接 RBAC
3. **懒加载**：必须用动态 `import()`
4. **命名**：`name` 用 kebab-case
5. **路径**：用 `/dashboard/<module>` 二级路径，避免根级污染

## 7. 开发环境

- **端口**：5173（不是 5175）
- **绑定**：`0.0.0.0` 支持 IPv4 + IPv6
- **访问**：http://localhost:5173
- **后端代理**：`vite.config.js` 中 `proxy: { '/api': 'http://localhost:8080' }`
