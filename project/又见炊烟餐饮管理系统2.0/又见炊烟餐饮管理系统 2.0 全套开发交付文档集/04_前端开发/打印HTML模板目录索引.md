# 打印HTML模板目录索引

> 适用：`04_前端开发/文件夹_打印HTML模板/**`  
> 技术栈：原生 HTML5 + CSS3（@media print）  
> 设计风格：徽派雅致配色（深绿 #1a3a2a / #2D4A3E + 金色 #C4A35A + 暖米色 #F5F0E8 / #FAF8F5）  
> 维护：Trae（TRAE-BOT）  
> 更新：2026-08-02

---

## 0. 文档说明

本文档汇总又见炊烟餐饮管理系统 2.0 全部打印业务单据的 HTML 模板。打印模板采用原生 HTML + CSS 实现，通过 `{{var}}` 占位变量与服务端/前端数据绑定，配合 `window.print()` 或后端无头浏览器（Puppeteer / Playwright）渲染输出 PDF 或热敏小票。

模板文件统一存放于：

```
f:\solo\project\又见炊烟餐饮管理系统2.0\又见炊烟餐饮管理系统 2.0 全套开发交付文档集\04_前端开发\文件夹_打印HTML模板\
```

共 8 类打印模板，覆盖餐饮业务的下单、出菜、退菜、结账、预订、宴会、采购、入库全流程。

前端集成时，可参考 `frontend_v3/src/components/PrintPreview.vue` 组件的 `@media print` 样式实现思路：在弹窗内渲染模板 HTML，调用 `window.print()` 时通过 CSS 隐藏弹窗外框，仅打印模板区域。

---

## 1. 模板清单总览

| 序号 | 模板编号 | 模板名称 | 文件名 | 纸张规格 | 业务场景 |
|------|---------|---------|--------|---------|---------|
| 1 | PRT-001 | 结账单 | `结账单.html` | 58mm 热敏 / A5 | 客户结账后打印消费明细 |
| 2 | PRT-002 | 后厨工单 | `后厨工单.html` | 58mm 热敏 | 新订单推送到后厨打印菜品制作工单 |
| 3 | PRT-003 | 预订单 | `预订单.html` | A5 | 客户预订后打印预订确认单 |
| 4 | PRT-004 | 宴会订单 | `宴会订单.html` | A4 | 宴会预订的完整订单明细（多桌汇总） |
| 5 | PRT-005 | 点菜单 | `点菜单.html` | 58mm 热敏 | 桌台点菜后打印给客户核对 |
| 6 | PRT-006 | 退菜单 | `退菜单.html` | 58mm 热敏 | 退菜操作后打印留底 |
| 7 | PRT-007 | 采购单 | `采购单.html` | A4 | 采购申请审批后打印给供应商 |
| 8 | PRT-008 | 入库单 | `入库单.html` | A4 | 食材入库验收后打印留底 |

---

## 2. 模板详细说明

### 2.1 PRT-001 结账单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-001 |
| 文件路径 | `文件夹_打印HTML模板/结账单.html` |
| 纸张规格 | 58mm 热敏小票（宽度 58mm，高度按内容自适应） |
| 字段清单 | `{{store_name}}` 店名、`{{order_no}}` 订单号、`{{table_name}}` 桌台、`{{guest_count}}` 人数、`{{cashier}}` 收银员、`{{created_at}}` 开台时间、`{{paid_at}}` 结账时间、`{{items}}` 菜品列表（菜名/数量/单价/小计）、`{{subtotal}}` 小计、`{{discount}}` 折扣、`{{service_fee}}` 服务费、`{{total}}` 应收、`{{paid}}` 实收、`{{change}}` 找零、`{{pay_method}}` 支付方式 |
| 布局要点 | 页头店名+订单号；中部菜品明细表（菜名左对齐、数量单价右对齐）；底部金额汇总（小计-折扣+服务费=应收）；页脚支付方式+找零+感谢语 |
| 调用接口 | `POST /api/orders/{id}/checkout` 返回订单结算数据后触发打印 |

### 2.2 PRT-002 后厨工单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-002 |
| 文件路径 | `文件夹_打印HTML模板/后厨工单.html` |
| 纸张规格 | 58mm 热敏小票 |
| 字段清单 | `{{store_name}}` 店名、`{{order_no}}` 订单号、`{{table_name}}` 桌台、`{{guest_count}}` 人数、`{{sequence}}` 工单序号（同桌第几单）、`{{created_at}}` 下单时间、`{{items}}` 菜品列表（菜名/数量/做法/备注/特殊要求）、`{{remark}}` 整单备注、`{{operator}}` 下单员 |
| 布局要点 | 页头大字"后厨工单"+桌台号醒目显示；中部菜品列表按出菜顺序排列，每道菜独立一行，备注加粗；页脚下单时间+操作员签名栏 |
| 调用接口 | `POST /api/orders` 下单成功后，前端调用 KDS 推送 + 后厨打印机打印 |

### 2.3 PRT-003 预订单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-003 |
| 文件路径 | `文件夹_打印HTML模板/预订单.html` |
| 纸张规格 | A5（148mm × 210mm） |
| 字段清单 | `{{store_name}}` 店名、`{{booking_id}}` 预订单号、`{{booking_date}}` 预订日期、`{{booking_time}}` 预订时间、`{{meal_period}}` 餐别（午餐/晚餐）、`{{customer_name}}` 客户姓名、`{{customer_phone}}` 联系电话（脱敏）、`{{occasion_type}}` 宴席类型、`{{table_count}}` 桌数、`{{spare_tables}}` 备桌、`{{guest_per_table}}` 每桌人数、`{{total_guests}}` 总人数、`{{deposit}}` 定金、`{{remark}}` 备注、`{{coordinator}}` 统筹负责人、`{{created_at}}` 创建时间 |
| 布局要点 | 页头店名+预订单号+宴席类型标签；中部信息分两栏（左侧客户信息，右侧桌台配置）；底部备注+统筹人签名+定金金额 |
| 调用接口 | `POST /api/bookings` 创建预订成功后触发打印 |

### 2.4 PRT-004 宴会订单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-004 |
| 文件路径 | `文件夹_打印HTML模板/宴会订单.html` |
| 纸张规格 | A4（210mm × 297mm） |
| 字段清单 | `{{store_name}}` 店名、`{{booking_id}}` 预订单号、`{{banquet_name}}` 宴会名称、`{{booking_date}}` 宴会日期、`{{booking_time}}` 开席时间、`{{customer_name}}` 客户姓名、`{{customer_phone}}` 联系电话、`{{occasion_type}}` 宴席类型、`{{table_count}}` 桌数、`{{guest_per_table}}` 每桌人数、`{{total_guests}}` 总人数、`{{tables}}` 桌台列表（桌名/楼层/区域）、`{{menu_items}}` 菜单明细（分类/菜名/英文名/份数/单价）、`{{package_name}}` 套餐名称、`{{total_amount}}` 菜品总额、`{{deposit}}` 已付定金、`{{balance}}` 待补尾款、`{{coordinator}}` 统筹负责人、`{{remark}}` 备注 |
| 布局要点 | 页头宴会名称大字+预订单号；信息区三列（客户/桌台/金额）；中部菜单明细按分类分组列表；底部金额汇总（总额-定金=尾款）+签名栏（客户/统筹/店长） |
| 调用接口 | `GET /api/bookings/{id}/detail` 拉取完整宴会订单后打印 |

### 2.5 PRT-005 点菜单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-005 |
| 文件路径 | `文件夹_打印HTML模板/点菜单.html` |
| 纸张规格 | 58mm 热敏小票 |
| 字段清单 | `{{store_name}}` 店名、`{{order_no}}` 订单号、`{{table_name}}` 桌台、`{{guest_count}}` 人数、`{{created_at}}` 下单时间、`{{items}}` 菜品列表（菜名/数量/单价/小计/做法备注）、`{{total_count}}` 总道数、`{{total_amount}}` 合计金额、`{{operator}}` 下单员 |
| 布局要点 | 页头桌台号+下单时间；中部菜品列表按下单顺序；底部合计道数+金额+下单员签名 |
| 调用接口 | `POST /api/orders/{id}/dishes` 加菜成功后打印给客户核对 |

### 2.6 PRT-006 退菜单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-006 |
| 文件路径 | `文件夹_打印HTML模板/退菜单.html` |
| 纸张规格 | 58mm 热敏小票 |
| 字段清单 | `{{store_name}}` 店名、`{{order_no}}` 原订单号、`{{table_name}}` 桌台、`{{created_at}}` 退菜时间、`{{items}}` 退菜列表（菜名/数量/退菜原因/原单价/退金额）、`{{total_refund}}` 退菜总额、`{{reason}}` 退菜原因（整单）、`{{operator}}` 操作员、`{{approver}}` 审批人 |
| 布局要点 | 页头红色"退菜单"标题+原订单号；中部退菜明细每行含退菜原因；底部退菜总额+操作员+审批人签名栏（需审批时） |
| 调用接口 | `POST /api/orders/{id}/refund-dishes` 退菜成功后打印留底 |

### 2.7 PRT-007 采购单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-007 |
| 文件路径 | `文件夹_打印HTML模板/采购单.html` |
| 纸张规格 | A4（210mm × 297mm） |
| 字段清单 | `{{store_name}}` 店名、`{{request_no}}` 采购单号、`{{request_date}}` 申请日期、`{{expected_date}}` 期望到货日期、`{{requester}}` 申请人、`{{department}}` 申请部门、`{{supplier_name}}` 供应商、`{{supplier_contact}}` 供应商联系方式、`{{items}}` 采购明细（食材名/规格/单位/数量/单价/金额/备注）、`{{total_amount}}` 合计金额（大写+小写）、`{{remark}}` 备注、`{{approver}}` 审批人、`{{approved_at}}` 审批时间 |
| 布局要点 | 页头采购单号+申请日期+供应商；中部采购明细表格（含序号列）；底部合计金额（人民币大写）+审批签名栏（申请人/部门主管/店长/财务） |
| 调用接口 | `POST /api/purchases` 采购申请审批通过后打印 |

### 2.8 PRT-008 入库单

| 项 | 内容 |
|---|---|
| 模板编号 | PRT-008 |
| 文件路径 | `文件夹_打印HTML模板/入库单.html` |
| 纸张规格 | A4（210mm × 297mm） |
| 字段清单 | `{{store_name}}` 店名、`{{receipt_no}}` 入库单号、`{{receipt_date}}` 入库日期、`{{purchase_no}}` 关联采购单号、`{{supplier_name}}` 供应商、`{{receiver}}` 验收人、`{{warehouse}}` 入库仓库、`{{items}}` 入库明细（食材名/规格/单位/采购数量/实收数量/单价/金额/质量评级/备注）、`{{total_amount}}` 合计金额、`{{remark}}` 备注、`{{inspector}}` 质检员、`{{keeper}}` 库管员 |
| 布局要点 | 页头入库单号+关联采购单号+供应商；中部入库明细表格（采购数量 vs 实收数量对比列）；底部合计金额+签名栏（验收人/质检员/库管员） |
| 调用接口 | `POST /api/goods-receipts` 入库验收完成后打印留底 |

---

## 3. 占位变量规范

所有模板使用 `{{var}}` 双花括号语法作为占位变量，与 Vue 模板语法一致，便于前端直接复用 Vue 渲染或后端字符串替换。

### 3.1 变量命名规则

- 一律小写 + 下划线：`{{order_no}}`、`{{customer_name}}`
- 列表型变量使用复数：`{{items}}`、`{{tables}}`、`{{menu_items}}`
- 布尔型变量建议加 `is_`/`has_` 前缀（如 `{{is_vip}}`）

### 3.2 列表渲染约定

列表型变量在模板中以 HTML 注释标记循环区间：

```html
<!-- BEGIN items -->
<tr>
  <td>{{dish_name}}</td>
  <td>{{qty}}</td>
  <td>{{price}}</td>
  <td>{{subtotal}}</td>
</tr>
<!-- END items -->
```

后端渲染引擎遍历 `items` 数组时，复制 `BEGIN`/`END` 之间的 HTML 块并替换变量。

---

## 4. 打印样式规范

### 4.1 纸张尺寸 CSS

| 纸张 | 宽度 | 用途 |
|------|------|------|
| 58mm 热敏 | `width: 58mm; padding: 4mm 3mm;` | 结账单、后厨工单、点菜单、退菜单 |
| A5 | `width: 148mm; min-height: 210mm; padding: 12mm 14mm;` | 预订单 |
| A4 | `width: 210mm; min-height: 297mm; padding: 15mm 18mm;` | 宴会订单、采购单、入库单 |

### 4.2 字体

- 中文：`'Noto Serif SC', 'PingFang SC', 'Microsoft YaHei', serif`
- 英文/数字：`'Georgia', serif`（用于金额、单号）
- 热敏小票正文 12px，标题 16px
- A4/A5 正文 13px，标题 20px

### 4.3 颜色

- 主文字：`#2D4A3E`（深绿）
- 强调/分隔线：`#C4A35A`（金色）
- 辅助文字：`#888`
- 背景：`#FAF8F5`（暖米色，仅用于表格行交替或信息块）
- 警示文字（退菜单标题等）：`#8B5A3E`

### 4.4 @media print

```css
@media print {
  body { margin: 0; padding: 0; }
  .no-print { display: none !important; }
  body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}
```

---

## 5. 前端集成方式

### 5.1 方式一：弹窗内打印（推荐）

参考 `PrintPreview.vue` 实现：在 `el-dialog` 内渲染模板 HTML，工具栏按钮调用 `window.print()`，通过 `@media print` 隐藏弹窗外框。

### 5.2 方式二：新窗口打印

```js
function printHtml(html) {
  const win = window.open('', '_blank')
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
  win.close()
}
```

### 5.3 方式三：后端渲染 PDF

后端使用 Puppeteer / Playwright 加载 HTML 模板，注入数据后生成 PDF，前端通过 `window.open('/api/print/preview?type=checkout&id=123')` 直接预览。

---

## 6. 版本记录

| 版本 | 日期 | 维护者 | 变更内容 |
|------|------|--------|---------|
| 1.0 | 2026-08-02 | Trae | 首版，覆盖 8 类打印模板：结账单、后厨工单、预订单、宴会订单、点菜单、退菜单、采购单、入库单 |
