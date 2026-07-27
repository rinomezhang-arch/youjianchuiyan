# 又见炊烟私房菜 · 餐饮管理系统 — 文件结构

## 后端 (Spring Boot)
```
com.youjian.banquet/
├── BanquetApplication.java        # 主入口
├── common/
│   └── Result.java                # 统一响应 {code, data, message}
├── entity/
│   ├── DishMaster.java            # 菜品 - dish_master
│   ├── TableMaster.java           # 桌台 - table_master
│   ├── BookingMaster.java         # 预订 - booking_master (双主键)
│   ├── BookingTable.java          # 预订-桌台关联
│   ├── BookingDish.java           # 预订-菜品关联 (新增)
│   ├── CustomerMaster.java        # 客户 - customer_master
│   ├── PackageMaster.java         # 套餐 - package_master
│   ├── PackageDishDetail.java     # 套餐-菜品明细
│   ├── Staff.java                 # 员工 - staff_master
│   ├── IngredientMaster.java      # 食材 - ingredient_master
│   ├── IngredientInventoryLog.java# 库存日志
│   └── AuditLog.java              # 审计日志 (新增)
├── repository/                    # JPA Repository (每个Entity一个)
├── controller/
│   ├── DishController.java        # /api/dishes ✅
│   ├── TableController.java       # /api/tables ✅
│   ├── StaffController.java       # /api/staff ✅
│   ├── BookingController.java     # /api/bookings ✅
│   ├── BookingDishController.java # /api/bookings/{id}/dishes 🔴新
│   ├── BookingExtController.java  # /api/bookings/swap + copy 🔴新
│   ├── BookingListExtController.java # /api/bookings/list ✅
│   ├── CustomerController.java    # /api/customers ✅
│   ├── CustomerExtController.java # /api/customers/search + history 🔴新
│   ├── PackageController.java     # /api/packages ✅
│   ├── InventoryController.java   # /api/inventory ✅
│   ├── DashboardController.java   # /api/dashboard/stats ✅
│   ├── DashboardReportController.java # /api/dashboard/report ✅
│   ├── DashboardReportV2Controller.java # /api/dashboard/reportv2 🔴新
│   ├── AttendanceController.java  # /api/attendance ✅
│   ├── StoreController.java       # /api/stores ✅
│   ├── HRController.java          # /api/hr/** ✅
│   ├── AuthController.java        # /api/auth/login ✅
│   ├── RecipesController.java     # /api/recipes/** ✅
│   ├── MenuApiController.java     # /api/menu-api/** ✅
│   └── UploadController.java      # /api/upload/image ✅
├── config/
│   └── ResponseWrapper.java       # 统一响应AOP
└── security/                      # Spring Security (已禁用)
```

## 前端 (Vue 3 + Element Plus)
```
/home/ubuntu/dist/
├── index.html                     # SPA入口
├── assets/
│   ├── index-DNIGm_PD.js          # 主JS (1.2MB)
│   └── index-CQMMdqP4.css         # 主CSS (102KB)
└── favicon.ico
```

## COS项目管理
```
/mnt/cos/天地双龙工作空间/项目管理/餐饮管理系统/
├── 01-工程概况.md
├── 02-开发过程.md
├── 03-文件地址清单.md
├── 04-项目文件结构.md  ← 你在这里
├── 05-使用说明.md
├── 06-聊天记录归档.md
└── 工程计划.md  (来自公共对话文件)
```

---
🦞 整理于 2026-07-22
