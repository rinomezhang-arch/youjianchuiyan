# 老版单页预订系统 剖析报告

**文件**：`单页餐饮管理系统.html`（389KB / 7793行）  
**来源**：宣城店本地 `D:\宣城店预定系统-新.html`  
**分析时间**：2026-07-23

---

## 一、技术概览

| 属性 | 值 |
|------|-----|
| 架构 | **纯前端单页**（零后端，零数据库） |
| 数据存储 | 浏览器 localStorage（所有数据存客户端） |
| JS引擎 | 原生 JS（无框架），103 个函数 |
| UI框架 | Tailwind CSS + 自定义 CSS |
| 外部依赖 | xlsx.js（导出）、Chart.js 4.4（图表）、dataService.js（外部引用但缺失） |
| 运行方式 | 本地 HTML 文件直接双击打开 |

## 二、核心数据模型

### 2.1 localStorage 完整键值表

| Key | 用途 | 数据结构 |
|-----|------|---------|
| `banquet_ng_v1` | 预订数据（主存储） | `{date: {morning|afternoon: {tableName: bookingObj}}}` |
| `banquet_tables` | 桌台配置 | `[{name, people, area}]` |
| `banquet_users_ng` | 用户账号 | `[{username, password, role}]` |
| `banquet_audit_ng` | 审计日志 | `[{timestamp, user, action, detail}]` |
| `banquet_menu_cats_ng` | 菜品分类 | `[{id, name}]` |
| `banquet_menu_cat_order_ng` | 分类排序 | `[catId1, catId2, ...]` |
| `banquet_menu_dishes_ng` | 菜品数据 | `[{id, name, categoryId, price, spicyLevel, unit, en}]` |
| `banquet_menu_dish_order_ng` | 菜品排序 | `[dishId1, dishId2, ...]` |
| `banquet_menu_pkgs_ng` | 套餐定义 | `[{code, name, price, people, desc}]` |
| `banquet_menu_pkgd_ng` | 套餐菜品明细 | `[{code, seq, dishId}]` |
| `banquet_pkg_used_ng` | 套餐使用记录 | `[{code, date, tableName}]` |

**会话存储（sessionStorage）：**
| Key | 用途 |
|-----|------|
| `banquet_session_ng` | 登录会话 |

> **重要纠正：** 预订数据主键是 `banquet_ng_v1`，不是之前报告的 `banquet_bookings_ng`。

### 2.2 根数据结构（localStorage key: `banquet_ng_v1`）

```
{
  "2026-07-23": {
    "morning": {
      "101和风": { name:"张三", phone:"138...", people:8, status:"booked", remark:"", banquetType:"商务宴" },
      "201紫霞": { name:"李四", phone:"139...", people:12, status:"booked", remark:"忌辛辣", banquetType:"如意宴" }
    },
    "afternoon": {
      "扶摇1": { name:"王五", phone:"137...", people:72, status:"booked", remark:"婚宴", banquetType:"团圆宴" }
    }
  },
  "2026-07-24": { ... }
}
```

**关键点**：每天两个时段（午餐 morning / 晚餐 afternoon），每时段一个 `{桌名: 预订对象}` 映射。

### 2.2 预订对象结构
```
{ name, phone, people, status: "booked"|"free", remark, banquetType }
```

### 2.3 桌台模型（`baseTables[]`）
```
{ name: "101和风", people: 10, area: "一楼包厢" }
```

### 2.4 菜单模型
```
菜品: {id:"d001", name:"黄焖甲鱼锅", categoryId:"c1", price:298, spicyLevel:"辣度可选", unit:"只"}
分类: {id:"c1", name:"徽菜经典特色招牌菜"}
套餐: {code:"p1", name:"如意宴", price:888, people:"8-10人"}
```

## 三、系统完整功能清单（按模块分类，含内部函数）

> 通过逐行分析全部 7793 行代码，共识别 **~160 个函数/事件处理器**，分布在 10 个功能模块中。

### 3.0 初始化与全局状态（~20行）
| 函数/变量 | 用途 |
|-----------|------|
| `baseTables` | 全局桌台数组 |
| `STORAGE = 'banquet_ng_v1'` | 预订数据 localStorage key |
| `getAll()` / `saveAll()` | 预订数据读/写 |
| `periodData(date, period)` / `savePeriod()` | 时段数据读/写 |
| `getStoredTables()` / `saveTables()` | 桌台配置读/写 |
| `getSession()` | 读 sessionStorage 会话 |

### 3.1 登录与权限（~15函数）
- `doLogin()` — 用户名密码验证
- `doLogout()` — 登出清理
- `checkAuth()` — 页面加载鉴权检查
- `canEdit()` / `isAdmin()` — 角色判断
- `showUserManager()` — 用户管理（CRUD）
- `addNewUser()` / `deleteUser(name)` — 增删用户
- `showAuditLog()` — 审计日志查看
- `addAudit(action, target, detail)` — 写审计日志
- 登录页：Canvas 星空背景 + 金色流星粒子动画

### 3.2 房态看板（主界面，~15函数）
- `renderTables()` — 🏠 核心渲染（过滤→排序→生成HTML→批量绑定事件）
- `toggleSelect(idx)` — 点击选中/取消桌台
- `updateActionToolbar()` — 动态操作工具栏
- `dblClick(idx)` — 双击打开预订/编辑
- `updateStatusSummary()` — 统计栏（总数/空闲/已预订）
- `refreshData()` — 重置全部筛选条件
- **筛选器**：区域按钮组、状态（全部/空闲/已预订）、搜索输入
- **时段切换**：午/晚/全天三态，全天视图合并 ☀️/🌙 双标

### 3.3 预订流程（~10函数）
- `openBookingModal(editData)` — 打开预订弹窗
- `closeBookingModal()` — 关闭弹窗
- `bindBookingEnter()` — Enter键自动跳转字段
- `saveBooking()` — 保存预订（校验→写入→刷新→审计）
- `editSelectedBooking()` — 编辑选中预订
- `deleteBooking()` — 删除预订

### 3.4 客户智能辅助（~6函数）
- `setupNameSuggest()` — 客户姓名实时联想
- `triggerPhoneHistory()` — 手机号失焦触发历史查询
- `queryPhoneHistory(phone)` — 按手机号检索全部历史
- `getVisitCount(phone, upToDate)` — 计算回头次数
- `showHistoryModal(phone)` — 历史预订详情弹窗
- `closeHistoryModal()` — 关闭历史弹窗

### 3.5 一键通知文案（~3函数）
- `copyBookingNotification()` — 生成文案→复制
- `generateNotificationText(date, period, table, people, area, type)` — 文案模板
- `openManualCopy(text)` — 手动复制对话框

### 3.6 桌台操作（~10函数）
- `copyToFreeTables()` — 复制预订到空闲桌台（深拷贝）
- `performSwap(srcIdx, tgtIdx)` — 调换两个桌台（移动/互换）
- `quickDeleteBooking()` — 快速删除
- `ctxMenu(e, idx)` — 右键菜单
- `enterEditMode()` / `exitEditMode()` — 桌面编辑模式
- `addNewTable()` / `deleteTable(idx)` — 增删桌台
- 拖拽排序：`handleDragStart/GridDragOver/Drop/DragEnd`

### 3.7 桌台选择弹窗（~5函数）
- `openTableSelectModal()` — 双击已选桌台触发
- `renderSelectableTables()` — 渲染可选桌台列表（单选/多选/调换三种模式）
- `toggleTableSelect(idx)` — 切换桌台选中
- `oneClickSwap(targetIdx)` — 一键调换（移动/互换两种模式）
- `confirmTableSelection()` / `closeTableSelectModal()`

### 3.8 菜品管理（~15函数）
- `openDishManage()` / `closeDishManage()` — 菜品管理弹窗
- `renderDishMgmtList()` — 渲染菜品列表（按分类/搜索/排序表）
- `showAddDishForm()` / `saveNewDish()` — 新增菜品表单
- `showEditDishForm(id)` / `saveEditDish()` — 编辑菜品
- `deleteDish(id)` — 删除菜品
- `openBatchImport()` / `doBatchImport()` — 批量导入（Tab分隔+自动建分类）
- `handleFileImport()` — 文件上传（.xlsx→SheetJS→CSV）
- `downloadDishTemplate()` — 下载模板
- **菜品搜索框** `#dishMgmtSearch` — 实时过滤
- 拖拽排序：`dishMgmtDragStart/Over/Drop/End`

### 3.9 分类管理（~6函数）
- `openMenuManage()` / `closeMenuManage()` — 分类管理弹窗
- `renderMenuMgmtList()` — 渲染分类列表
- `addMenuCategory()` — 新增分类（自动生成 id cX）
- `editMenuCategory(catId)` / `deleteMenuCategory(catId)` — 编辑/删除
- 拖拽排序：`menuMgmtDragStart/Over/Drop/End`

### 3.10 套餐管理（~20函数）
**套餐定义管理**
- `openPkgManage()` / `closePkgManage()` — 套餐管理弹窗
- `renderPkgList()` — 渲染套餐列表
- `addNewPackage()` — 新增套餐
- `selectPkg(code)` — 选中套餐，加载菜品明细
- `editPkgInfo(code)` / `savePkgEdit()` — 编辑套餐信息
- `deletePkg(code)` — 删除套餐+明细

**套餐菜品明细**
- `renderDishSelector()` — 左侧菜品选择器（按分类分组，已选高亮）
- `addDishToPkg(dishId)` / `toggleDish(dishId)` — 添加/切换菜品
- `renderSelectedDishes()` — 右侧已选菜品列表（数量+/-、小计）
- `changeDishQty(idx, delta)` / `removeDishFromPkg(idx)` — 数量调整/删除
- `clearSelectedDishes()` / `saveCurrentPackage()` — 清空/保存
- 拖拽排序：`selectedDishDragStart/End/Over/Drop`

**套餐导入导出**
- `openPkgBatchImport()` / `doPkgBatchImport()` — 批量导入
- `exportPackages()` — 导出 Excel
- `downloadPkgTemplate()` — 下载模板

### 3.11 经营分析（~5函数）
- `renderAnalysis()` → `refreshAnalysis()` — 主入口
- `setupAnalysisDateShortcuts()` — 日期快捷按钮 + 范围校验
- 5 个 Chart.js 图表（预订趋势/区域占比/午晚对比/桌台TOP/客户频次）

### 3.12 导出面板（~5函数）
- `openExportPanel()` / `closeExportPanel()` — 导出弹窗
- `setupExportDateShortcuts()` — 日期快捷 + 拖拽滑块（0-90天）
- `executeExport()` — 生成 Excel
- `previewExportData()` — 预览新窗口

### 3.13 今日概览（~3函数）
- `createModal()` / `closeModal()` — 动态创建/关闭模态框
- `openTodayOverview()` — 汇总统计 + 5大操作按钮（明细/高亮/清午/清晚/清全部）

### 3.14 工具函数（~10函数）
- `escapeHtml(str)` — HTML 转义
- `showToast(msg)` — Toast 提示
- `showCustomAlert(msg)` — 自定义 Alert 弹窗
- `showModal(title, content)` — 通用模态框组件
- `daysFromToday(dateStr)` / `dateFromDays(days)` — 日期偏移计算
- `getMonday(date)` / `getMonthStart(date)` / `getYearStart(date)` — 周期起始
- `formatDate(d)` — 日期格式化

## 四、权限系统

- **localStorage 用户表**（`banquet_users_ng`）：
  - 默认用户：`admin/admin888`（角色 admin）、`staff/123456`（角色 operator）
  - 角色：admin（全部权限）/ operator（不能删用户、不能改角色）
  - 支持新增/删除/修改用户角色
- **审计日志**（`banquet_audit_ng`）：记录每次预订增删改
- **会话**（sessionStorage）：登录后保存，页面关闭即失效

## 五、后端功能模块详细拆解

### 5.1 菜品管理体系（菜单+菜品+套餐）

**菜品分类管理（menu management overlay）**
- `window.openMenuManage()` — 打开分类管理弹窗 (`#menuMgmtOverlay`)
- `renderMenuMgmtList()` — 渲染分类列表，支持拖拽排序（dragstart/dragover/drop）
- `window.addMenuCategory()` — 新增分类（读 `#newCatName`，自动生成 id `cX`）
- `window.editMenuCategory(catId)` — 编辑分类名称（prompt弹窗）
- `window.deleteMenuCategory(catId)` — 删除分类（确认后从 cats 数组移除，菜品变为未分类）
- **排序系统**：`getCatOrderMap()` / `saveCatOrderMap()` — localStorage key `banquet_menu_cat_order_ng`

**菜品管理（dish management overlay）**
- `window.openDishManage()` — 打开菜品管理弹窗 (`#dishMgmtOverlay`)，填充分类筛选下拉
- `renderDishMgmtList()` — 按分类+搜索过滤+排序表排序后渲染菜品列表
- 拖拽排序：`dishMgmtDragStart/Over/Drop/End` — localStorage key `banquet_menu_dish_order_ng`
- `window.showAddDishForm()` — 新增菜品表单（中文名/英文名/分类/价格/单位/辣度）
- `window.showEditDishForm(id)` — 编辑菜品（同上，预填）
- `window.saveNewDish()` — 保存新菜品（读 `#newDishName` 等字段，生成 id `dXXX`）
- `window.saveEditDish()` — 保存编辑
- `window.deleteDish(id)` — 删除菜品

**菜品搜索功能**  
- `document.getElementById('dishMgmtSearch')` 输入框监听 input 事件实时过滤
- `window.handleDishMgmtSearch(e)` — 搜索处理

**批量导入菜品**
- `window.openBatchImport()` — 打开批量导入弹窗 (`#batchImportOverlay`)
- `window.doBatchImport()` — 解析 Tab 分隔的行（中文名\t英文名\t分类\t单价\t单位\t辣度），自动创建缺失分类
- `window.handleFileImport(fileInputId, textareaId)` — 文件上传处理（.xlsx 用 SheetJS 解析为 CSV；.txt/.csv 直接读）
- `window.downloadDishTemplate()` — 下载 Excel 导入模板

**套餐管理体系（package management overlay）**
- `window.openPkgManage()` — 打开套餐管理 (`#pkgMgmtOverlay`)
- `renderPkgList()` — 渲染套餐列表（编码/名称/价格/人数/菜品数）
- `window.addNewPackage()` — 新增套餐（读 `#newPkgCode/Name/Price/People/Desc`）
- `window.selectPkg(code)` — 选中套餐，加载菜品明细
- `window.editPkgInfo(code)` — 编辑套餐基本信息
- `window.savePkgEdit()` — 保存套餐编辑
- `window.deletePkg(code)` — 删除套餐（删除定义+明细）
- `window.finishEditingPkg()` — 完成编辑，返回列表

**套餐菜品明细管理**
- `renderDishSelector()` — 渲染左侧菜品选择器（按分类分组，绿色高亮已选）
- `window.addDishToPkg(dishId)` — 添加菜品到套餐
- `window.toggleDish(dishId)` — 切换菜品选中状态
- `renderSelectedDishes()` — 渲染右侧已选菜品列表（数量+/-、小计、拖拽排序）
- `window.changeDishQty(idx, delta)` — 调整菜品数量（最低 1）
- `window.removeDishFromPkg(idx)` — 移除菜品
- `window.clearSelectedDishes()` — 清空全部（带确认）
- `window.saveCurrentPackage()` — 保存套餐菜品配置到 localStorage
- **拖拽排序**：`selectedDishDragStart/End/Over/Drop`

**套餐批量导入/导出**
- `window.openPkgBatchImport()` — 打开套餐批量导入 (`#pkgBatchOverlay`)
- `window.doPkgBatchImport()` — 解析 Tab 分隔行（编码\t名称\t英文\t价格\t人数\t描述\t菜品编号）
- `window.exportPackages()` — 导出全部套餐为 Excel
- `window.downloadPkgTemplate()` — 下载套餐导入模板

**套餐在点餐中的应用**
- `initPkgSelector()` — 填充套餐下拉框
- `window.applyPackage()` — 应用套餐到当前桌台（替换已有菜品，存使用记录到 `banquet_pkg_used_ng`）
- `getPkgUsedInfo(code, tableName)` — 查询套餐使用记录

### 5.2 宴会预定核心流程

**预订弹窗（booking modal）**
- `window.openBookingModal(editData)` — 打开预订弹窗（新预订/编辑模式）
- `window.closeBookingModal()` — 关闭弹窗
- `bindBookingEnter()` — Enter 键自动跳转下一个字段（日期→时段→姓名→电话→人数→类型→备注→保存）
- `window.saveBooking()` — 保存预订（字段校验→构建数据→写入 periodData→刷新）
- `window.editSelectedBooking()` — 编辑选中预订
- `window.deleteBooking()` — 删除选中预订

**客户智能辅助**
- `setupNameSuggest()` — 初始化客户姓名联想（监听 `#custName` 输入，匹配历史客户名和电话）
- `triggerPhoneHistory()` — 手机号失焦→查询历史记录（`#custPhone` blur 事件）
- `queryPhoneHistory(phone)` — 按手机号检索全部历史预订
- `getVisitCount(phone, upToDate)` — 计算某日期前的到访次数
- `showHistoryModal(phone)` — 弹窗显示历史预订详情

**一键通知文案（核心差异化功能）**
- `window.copyBookingNotification()` — 生成通知文案→打开手动复制对话框
- `generateNotificationText(date, period, tableName, people, area, banquetType)` — 生成标准文案模板：
  ```
  亲爱的朋友们，我已于X月X日午/晚餐【宴会类型】安排在又见炊烟私房菜宁国店
  X楼 桌台名 共N人欢聚！恭候您的到来！
  
  🟩 添加微信：15905638866
  📞 电话：0563-4626666
  📍 地址：宁国市青龙西路1号
  高德导航↓ + 百度导航↓
  ```
- `openManualCopy(text)` — 弹出带复制按钮的对话框

**桌台操作（自动工具栏）**
- `renderTables()` — 核心渲染（过滤→排序→生成HTML→批量绑定事件）
- `window.toggleSelect(idx)` — 点击选中/取消桌台
- `updateActionToolbar()` — 根据选中状态动态显示操作按钮
- `window.copyToFreeTables()` — 复制预订到空闲桌台（深拷贝）
- `performSwap(srcIdx, tgtIdx)` — 调换两个桌台（移动/互换）
- `window.quickDeleteBooking()` — 快速删除预订

**桌台编辑模式**
- `enterEditMode()` / `window.exitEditMode()` — 切换编辑模式
- `window.addNewTable()` — 新增桌台（读 `#newTableName/People/Area`）
- `window.deleteTable(idx)` — 删除桌台（confirm 确认）
- `handleDragStart/GridDragOver/Drop/DragEnd` — 拖拽交换桌台位置

**数据刷新**
- `window.refreshData()` — 重置全部筛选条件→重新渲染房态

### 5.3 经营分析（analysis page）

- `window.renderAnalysis()` → `refreshAnalysis()` — 主入口
- `setupAnalysisDateShortcuts()` — 日期快捷按钮（今天/本周/本月/今年）+ 范围校验
- 5 个图表（Chart.js）：
  - 📈 预订趋势折线图（`chart-trend`）
  - 🥧 区域占比饼图（`chart-area`）
  - 🌓 午晚对比柱状图（`chart-period`）
  - 📊 桌台 TOP 排名表
  - 👤 客户预订频次表

### 5.4 导出面板

- `window.openExportPanel()` → `#exportPanel` 弹窗
- 日期范围拖拽滑块（0-90 天可视化）
- `setupExportDateShortcuts()` — 快捷按钮 + 滑块联动
- `window.executeExport()` — 生成 Excel（xlsx.js）
- `window.previewExportData()` — 弹新窗口预览表格

### 5.5 权限与审计

- `window.showUserManager()` — 用户管理弹窗（列角色下拉 + 删除按钮）
- `window.addNewUser()` — 新增用户（读 `#newUserName/Pass/Role`）
- `window.deleteUser(name)` — 删除用户
- `window.showAuditLog()` — 审计日志弹窗（时间倒序）
- `addAudit(action, target, detail)` — 写审计日志
- `showModal(title, content)` — 通用弹窗组件
- `window.closeModal()` — 关闭通用弹窗

### 5.6 登录系统

- `getSession()` — 读 sessionStorage `banquet_session_ng`
- `doLogin()` — 用户名密码验证（读 `#loginName/Pass`）
- `doLogout()` — 清除会话 + 显示登录页
- `checkAuth()` — 页面加载时检查会话
- `canEdit()` / `isAdmin()` — 角色判断
- 登录页：Canvas 星空背景 + 金色流星动画（requestAnimationFrame）

---

## 六、和 Vue3 新系统的差异对比

| 维度 | 老系统（单页HTML） | 新系统（Vue3+Spring Boot） |
|------|-------------------|--------------------------|
| 数据存储 | localStorage（本地） | MySQL 数据库 |
| 多门店 | 硬编码宁国店 | store_id 隔离（宁国+宣城） |
| 桌台管理 | 固定72桌，拖拽添加 | 数据库 table_master 动态管理 |
| 预订流程 | 选桌→弹窗填写→保存 | 同上但走 API |
| 菜品管理 | 113道菜硬编码 | 数据库 dish_master 357道 |
| 套餐 | 4个固定套餐 | 动态套餐体系 |
| 客户管理 | 无独立模块（依赖预订记录） | customer_master 表 |
| 复制通知 | ✅ 核心功能（自动生成导航文案） | ❌ 缺失 |
| 编辑模式 | 拖拽添加/删除桌台 | 后台配置 |
| 经营分析 | 5个图表 | 数据大屏模块 |
| 用户管理 | localStorage 简单角色 | Spring Security + JWT |
| 离线可用 | ✅ | ❌（需网络） |

## 六、交互设计亮点（值得借鉴）

### 6.1 桌台选择弹窗（双击已选桌台）
- 支持**单选/多选/调换**三种模式
- 调换模式下：发起方卡片高亮紫色边框，目标方显示一键调换按钮
- 已预订桌台显示客户名+电话+宴会类型
- 支持**移动模式**（预订信息A→空闲桌台B）和**交换模式**（A↔B互换）

### 6.2 全天合并视图
- 午餐和晚餐的预订信息同时显示在一张桌台卡片上
- ☀️/🌙 图标区分时段
- 统计栏动态计算合并数据

### 6.3 动态操作工具栏
- 根据选中桌台状态智能显示可用操作
- 复制到空闲桌台（选中1个已预订+N个空闲 = 批量复制）
- 快速删除、调换按钮（右键菜单备选）

### 6.4 经营分析面板
- 日期范围拖拽滑块（0-90天，可视化时间轴）
- 数据明细弹窗、高亮桌台、按餐段清除
- 5种图表+客户分析表格

### 6.5 桌面编辑模式
- 拖拽手感：自由拖拽任意两个桌台交换位置
- 自动保存localStorage，无需额外操作

## 七、老系统值得迁移到新系统的功能

| 优先级 | 功能 | 理由 |
|--------|------|------|
| 🔴 **高** | **一键复制预订通知文案** | 老系统核心差异化功能，客户最常用操作 |
| 🔴 **高** | **客户姓名智能联想** | 输入时从历史预订匹配，减少重复打字 |
| 🔴 **高** | **回头客标记（第N次）** | 桌台卡片显示该客户历史到店次数 |
| 🟡 **中** | **全天视图（午晚合并）** | 一屏看整天预订，☀️/🌙图标直观 |
| 🟡 **中** | **复制到多个空闲桌台** | 大宴会需要多个桌台时一键复制预订信息 |
| 🟡 **中** | **操作工具栏动态变化** | 根据选中桌台状态智能显示可用操作 |
| 🟢 **低** | **AI图片转文字** | 拍照识别手写预订信息（需接入 OCR） |
| 🟢 **低** | **本地离线缓存** | 断网时仍可查看（PWA + Service Worker） |

## 八、架构简图

```
浏览器本地
┌─────────────────────────────────────┐
│  localStorage                      │
│  ├─ banquet_bookings_ng  预订数据  │
│  ├─ banquet_tables_ng    桌台配置  │
│  ├─ banquet_users_ng     用户账号  │
│  ├─ banquet_audit_ng     审计日志  │
│  ├─ banquet_menu_dishes  菜品数据  │
│  └─ banquet_menu_pkgs    套餐数据  │
├─────────────────────────────────────┤
│  渲染引擎                          │
│  ├─ renderTables()     房态网格    │
│  ├─ saveBooking()      预订保存    │
│  ├─ renderAnalysis()   分析图表    │
│  └─ generateNotification() 文案    │
└─────────────────────────────────────┘
           ↓ (缺失的 dataService.js 试图桥接)
    外部 API / OCR 服务
```

## 九、总结

这是一个**设计精良的纯前端预订工具**，核心价值在于：
1. **极简操作流**：选桌→填信息→生成通知，三步完成
2. **一键通知文案**是最实用的功能，直接产生客户价值
3. 用 localStorage 实现了完整的 CRUD + 审计 + 权限 + 分析，证明了产品逻辑的成熟度
4. **全天视图**和**动态工具栏**的交互设计值得新系统借鉴

局限：
- 数据存在用户浏览器，换设备/清缓存即丢失
- 无并发控制、无数据备份
- 桌台硬编码，改布局需改代码
- 引用缺失的 `dataService.js`（AI 图片识别功能不可用）
