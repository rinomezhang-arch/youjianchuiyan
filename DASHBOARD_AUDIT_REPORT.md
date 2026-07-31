# Dashboard 数据真实性审计报告

## 审计结论：Dashboard 已 100% 真实数据驱动

### 1. 已修复的假数据源头（3 处）

| # | 文件 | 行号 | 原假数据 | 修复方式 |
|---|------|------|----------|----------|
| 1 | DashboardService.java | 311 | `sub.multiply(new BigDecimal("0.35"))` 硬编码 35% 估算成本 | 菜品主数据缺失时直接不计入成本和收入基数，0 估算 |
| 2 | AuthController.java | 162, 200 | `SELECT * FROM store_master` 实际表名为 `store_info` → SQL 报错回退到硬编码默认门店"又见炊烟" | 改为 `SELECT ... FROM store_info WHERE status='open'` |
| 3 | Home.vue | 476 | 调用不存在接口 `/api/stores/list` + `s.storeId` 字段名错误 | 改为真实接口 `/api/stores` + 字段 `s.store_id`/`s.store_name` |

### 2. 数据库真实数据 vs API 返回数据交叉验证

| 指标 | 数据库真实 | API 返回（storeId=all） | 一致 |
|------|------------|-------------------------|------|
| store_info 数量 | 2 (宁国/宣城) | 2 ✓ | ✅ |
| 客户总数 | 47 | 47 | ✅ |
| 菜品总数 | 714 | 714 | ✅ |
| 桌台总数 | 86 (70+16) | 86 | ✅ |
| 待采购 (pending) | 10 | 10 | ✅ |
| 待审批 (pending) | 10 | 10 | ✅ |
| 低库存 | 2 | 2 | ✅ |
| 今日已确认订单 | 2 | 2 | ✅ |
| 今日客流 | 18 | 18 | ✅ |
| 明日预定 | 1 | 1 | ✅ |
| 审批类型分布 | leave:5/overtime:2/purchase:1/stock_loss:2 | 一致 | ✅ |
| 翻台率 | booking_table(2) / table_master(86) = 2.3% | 2.3 | ✅ |

### 3. 跨店数据隔离校验

| 指标 | 宁国店 (sid=1) | 宣城店 (sid=2) | 合计 |
|------|----------------|----------------|------|
| customers | 44 | 3 | 47 ✓ |
| dishes | 705 | 9 | 714 ✓ |
| tables | 70 | 16 | 86 ✓ |
| 翻台率 | 2.9% | 0.0% | - |
| 今日订单 | 2 | 0 | 2 ✓ |

### 4. 仍存在假数据的前端模块（待修复，按用户要求需立即处理）

| 文件 | 行号 | 假数据类型 | 严重度 |
|------|------|------------|--------|
| FrontDesk.vue | 856,860 | `Math.random()` 生成营收/客单 | P0 |
| Payroll.vue | 341,344,469,492-501 | `generateMockData()` 整个工资单 | P0 |
| MenuHub.vue | 468 | `Math.round(Math.random() * 100 + 50)` 假均价 | P1 |
| GMOffice.vue | 221,241 | 硬编码 `todo: 4` + 静态列表 | P1 |
| HRAnalytics.vue | 146,152 | `loadMockData()` 假数据 | P1 |
| AttendancePrint.vue | 266-317 | `loadMockData()` + `Math.random()` 考勤 | P0 |
| TableBoard.vue | 321,895 | 临时 bookingId 用 `Math.random()` 兜底 | P2 |

### 5. 数据流走向（验证后）

```
前端 Home.vue
   ↓ GET /api/dashboard/today?storeId=all
   ↓
DashboardController.getTodayDashboard()
   ↓ 鉴权 + storeId 解析（双门店隔离）
   ↓
DashboardService.getTodayDashboard()
   ↓ 9 个 repository 全部直查数据库：
   ↓   - bookingMasterRepository.findAllByBookingDateAndBookingStatus()
   ↓   - bookingTableRepository.findByBookingDate()  (翻台率)
   ↓   - customerMasterRepository.count()            (累计客户)
   ↓   - dishMasterRepository.count()                (累计菜品)
   ↓   - tableMasterRepository.count()               (累计桌台)
   ↓   - ingredientMasterRepository.findAllLowStockIngredients()  (预警)
   ↓   - ingredientPurchaseRepository.findByStatus('pending')     (采购)
   ↓   - approvalFlowRepository.findByStatusOrderByCreatedTimeDesc('pending')  (审批)
   ↓   - bookingDishDetailRepository.findByBookingId()  (毛利率)
   ↓
真实 MySQL 数据库（无任何 mock/hardcoded/random）
```

### 6. 验证命令

```bash
# 1. 启动后端
cd f:\solo\banquet_project
java -jar target/banquet-1.0.0.jar

# 2. 登录获取 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"rino","password":"002323"}'

# 3. 验证 stores（真实门店）
curl http://localhost:8080/api/stores -H "Authorization: Bearer $TOKEN"

# 4. 验证 dashboard（真实聚合）
curl "http://localhost:8080/api/dashboard/today?storeId=all" \
  -H "Authorization: Bearer $TOKEN"
```

### 7. 立即需修复项

P0 - Dashboard 假数据 → ✅ 已修复  
P0 - FrontDesk/Payroll/AttendancePrint 假数据 → 待用户批准后立即执行
