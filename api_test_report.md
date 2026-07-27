# 餐饮管理系统 API 接口测试报告

**测试时间**: 2026-07-27
**基础URL**: http://youjianchuiyan.com/api/


## 1. AuthController - 登录认证
- ✅ POST /api/auth/login - 200 OK (登录)
- ✅ GET /api/auth/me - 200 OK (获取当前用户)
- ✅ GET /api/stores - 200 OK (获取门店列表)
- ✅ POST /api/auth/logout - 200 OK (退出登录)

## 2. DashboardController - 仪表盘/工作台
- ❌ GET /api/dashboard/today - 500 Error: JDBC exception executing SQL [select pm1_0.package_id,pm1_0.store_id,pm1_0.categ (今日仪表盘)
- ✅ GET /api/dashboard/report - 200 OK (报表)

## 3. TableController - 桌台管理
- ✅ GET /api/tables - 200 OK (桌台列表)
- ✅ GET /api/tables/160 - 200 OK (桌台详情(ID=160))
- ✅ POST /api/tables - 200 OK (创建桌台)
- ✅ PUT /api/tables/160 - 200 OK (更新桌台(ID=160))
- ✅ POST /api/tables/reorder - 200 OK (重新排序桌台)
- ✅ POST /api/tables/swap-booking - 200 OK (交换预订桌台)

## 4. BookingController - 预订管理
- ✅ GET /api/bookings - 200 OK (预订列表)
- ✅ GET /api/bookings/list - 200 OK (预订列表(无筛选))
- ✅ GET /api/bookings/list - 200 OK (预订列表(按状态))
- ✅ GET /api/bookings/list - 200 OK (预订列表(按日期))
- ❌ GET /api/bookings/1 - 400 Error: Booking not found: 1 (预订详情(不存在ID))
- ✅ GET /api/bookings/customer/1 - 200 OK (客户预订列表)
- ❌ POST /api/bookings - 500 Error: could not execute statement [Column 'booking_date' cannot be null] [insert into  (创建预订)

## 5. CustomerController - 客户管理
- ✅ GET /api/customers - 200 OK (客户列表)
- ❌ GET /api/customers/1 - 400 Error: Customer not found: 1 (客户详情)
- ✅ GET /api/customers/search - 200 OK (搜索客户)
- ❌ GET /api/customers/1/history - 400 Error: Customer not found: 1 (客户历史)
- ❌ POST /api/customers - 500 Error: could not execute statement [Duplicate entry '1-API测试客户-13800138000' for key 'cu (创建客户)

## 6. DishController - 菜品管理
- ✅ GET /api/dishes - 200 OK (菜品列表)
- ✅ GET /api/dishes/CY000001 - 200 OK (菜品详情(ID=CY000001))
- ✅ GET /api/dishes/categories - 200 OK (菜品分类)
- ✅ GET /api/dishes/search - 200 OK (搜索菜品)
- ✅ POST /api/dishes - 200 OK (创建菜品)

## 7. RecipeController - 菜品配方
- ❌ GET /api/recipes/CY000001 - 500 Error: JDBC exception executing SQL [select dr1_0.dish_id,dr1_0.ingredient_id,dr1_0.sto (菜品配方(菜品ID=CY000001))
- ✅ GET /api/recipes/dishes-with-recipe - 200 OK (有配方的菜品)
- ❌ POST /api/recipes - 500 Error: JDBC exception executing SQL [select dr1_0.dish_id,dr1_0.ingredient_id,dr1_0.sto (保存配方)
- ❌ POST /api/recipes/recalc-all - 500 Error: JDBC exception executing SQL [select dr1_0.dish_id,dr1_0.ingredient_id,dr1_0.sto (重新计算所有)

## 8. PackageController - 套餐管理
- ❌ GET /api/packages - 500 Error: JDBC exception executing SQL [select pm1_0.package_id,pm1_0.store_id,pm1_0.categ (套餐列表)
- ❌ GET /api/packages/1 - 500 Error: JDBC exception executing SQL [select pm1_0.package_id,pm1_0.store_id,pm1_0.categ (套餐详情)
- ❌ POST /api/packages - 500 Error: JDBC exception executing SQL [select pm1_0.package_id,pm1_0.store_id,pm1_0.categ (创建套餐)

## 9. SupplierController - 供应商管理
- ❌ GET /api/menu-api/suppliers - 500 Error: JDBC exception executing SQL [select sm1_0.supplier_id,sm1_0.address,sm1_0.categ (供应商列表)
- ❌ GET /api/menu-api/suppliers/1 - 500 Error: JDBC exception executing SQL [select sm1_0.supplier_id,sm1_0.address,sm1_0.categ (供应商详情)
- ❌ POST /api/menu-api/suppliers - 500 Error: JDBC exception executing SQL [select sm1_0.supplier_id,sm1_0.address,sm1_0.categ (创建供应商)

## 10. IngredientController - 食材/原料管理
- ❌ GET /api/menu-api/ingredients - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (食材列表)
- ❌ GET /api/menu-api/ingredients/1 - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (食材详情)
- ❌ GET /api/menu-api/ingredients/low-stock - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (低库存食材)
- ❌ POST /api/menu-api/ingredients - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (创建食材)

## 11. InventoryController - 库存管理
- ❌ GET /api/menu-api/inventory/logs - 500 Error: JDBC exception executing SQL [select iil1_0.log_id,iil1_0.after_stock,iil1_0.bef (库存日志)
- ❌ GET /api/menu-api/inventory/logs/1 - 500 Error: JDBC exception executing SQL [select iil1_0.log_id,iil1_0.after_stock,iil1_0.bef (按食材的库存日志)
- ❌ GET /api/menu-api/inventory/logs/range - 500 Error: JDBC exception executing SQL [select iil1_0.log_id,iil1_0.after_stock,iil1_0.bef (按日期范围的库存日志)
- ❌ GET /api/menu-api/inventory/alerts - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (低库存预警)
- ❌ POST /api/menu-api/inventory/in - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (入库)
- ❌ POST /api/menu-api/inventory/out - 500 Error: JDBC exception executing SQL [select im1_0.ingredient_id,im1_0.store_id,im1_0.ca (出库)

## 12. PurchaseController - 采购管理
- ❌ GET /api/menu-api/purchases - 500 Error: JDBC exception executing SQL [select ip1_0.purchase_id,ip1_0.approved_at,ip1_0.a (采购列表)
- ❌ GET /api/menu-api/purchases/1 - 500 Error: JDBC exception executing SQL [select ip1_0.purchase_id,ip1_0.approved_at,ip1_0.a (采购详情)
- ❌ GET /api/menu-api/purchases/status/pending - 500 Error: JDBC exception executing SQL [select ip1_0.purchase_id,ip1_0.approved_at,ip1_0.a (按状态获取采购)
- ❌ GET /api/menu-api/purchases/range - 500 Error: JDBC exception executing SQL [select ip1_0.purchase_id,ip1_0.approved_at,ip1_0.a (按日期范围获取采购)
- ❌ POST /api/menu-api/purchases - 500 Error: could not execute statement [Unknown column 'approved_at' in 'field list'] [inse (创建采购)

## 13. StaffController - 员工管理（菜单API）
- ✅ GET /api/menu-api/staff - 200 OK (员工列表)
- ✅ GET /api/menu-api/staff/1 - 200 OK (员工详情)
- ✅ POST /api/menu-api/staff - 200 OK (创建员工)

## 14. HRController - 人事管理
- ✅ GET /api/hr/departments - 200 OK (部门列表)
- ✅ GET /api/hr/leave - 200 OK (请假列表)
- ✅ POST /api/hr/leave - 200 OK (创建请假)
- ✅ GET /api/hr/schedule - 200 OK (排班列表)
- ✅ POST /api/hr/schedule - 200 OK (创建排班)
- ✅ GET /api/hr/overtime - 200 OK (加班列表)
- ✅ POST /api/hr/overtime - 200 OK (创建加班)
- ✅ GET /api/hr/lifecycle - 200 OK (员工生命周期)
- ✅ GET /api/hr/attendance - 200 OK (考勤列表)
- ✅ POST /api/hr/attendance - 200 OK (创建考勤)

## 15. AttendanceRecordController - 考勤记录
- ✅ GET /api/hr/attendance/record - 200 OK (加载考勤记录)
- ✅ POST /api/hr/attendance/record - 200 OK (保存考勤记录)
- ✅ GET /api/hr/attendance/summary - 200 OK (考勤汇总)
- ✅ DELETE /api/hr/attendance/record/999 - 200 OK (删除考勤记录(不存在ID))

## 16. PayrollController - 薪酬管理
- ✅ GET /api/hr/payroll - 200 OK (薪酬列表)
- ✅ POST /api/hr/payroll/unlock - 200 OK (验证码解锁)
- ✅ POST /api/hr/payroll/lock - 200 OK (锁定)

## 17. UploadController - 文件上传
- ❌ POST /api/upload/image - 500 Error: Current request is not a multipart request (上传图片(无文件测试))

## 18. IpadController - iPad点餐
- ✅ GET /api/ipad/table/list - 200 OK (桌台列表)
- ✅ POST /api/ipad/table/open - 200 OK (开台)
- ✅ GET /api/ipad/dish/category - 200 OK (菜品分类)
- ✅ GET /api/ipad/dish/list - 200 OK (菜品列表)
- ✅ GET /api/ipad/dish/detail/CY000001 - 200 OK (菜品详情(ID=CY000001))
- ✅ POST /api/ipad/order/dish/add - 200 OK (添加菜品到订单)
- ✅ PUT /api/ipad/order/dish/edit - 200 OK (更新订单菜品)
- ✅ DELETE /api/ipad/order/dish/remove/999 - 200 OK (删除订单菜品(不存在ID))
- ✅ GET /api/ipad/order/current - 200 OK (获取当前订单)
- ✅ POST /api/ipad/order/send-kitchen - 200 OK (提交后厨)

---

## 汇总
- **总接口数**: 85
- **成功数**: 54
- **失败数**: 31
- **成功率**: 63.5%

### 按状态码分类:
- 200 成功: 54 个
- 500 服务器内部错误: 28 个
- 400 请求错误/业务错误: 3 个
- 404 资源不存在: 0 个

### 失败接口详情:

#### 500 内部服务器错误 (28 个)

**主要原因**: 数据库表字段不匹配（JPA实体类与实际表结构不一致）

- **unknown 表相关**: 25 个接口失败
  - GET /api/dashboard/today (今日仪表盘)
  - POST /api/bookings (创建预订)
  - POST /api/customers (创建客户)
  - ... 等共 25 个
- **dish_recipe 表相关**: 3 个接口失败
  - GET /api/recipes/CY000001 (菜品配方(菜品ID=CY000001))
  - POST /api/recipes (保存配方)
  - POST /api/recipes/recalc-all (重新计算所有)

#### 400 业务错误 (3 个) - 正常业务校验

- ⚠️  GET /api/bookings/1 (预订详情(不存在ID))
  - 说明: Booking not found: 1
- ⚠️  GET /api/customers/1 (客户详情)
  - 说明: Customer not found: 1
- ⚠️  GET /api/customers/1/history (客户历史)
  - 说明: Customer not found: 1