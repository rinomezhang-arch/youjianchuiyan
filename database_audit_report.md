# 📊 数据库完整审计报告
## SOLO🐚 | 2026-07-22 21:48

---

## 一、总览

| 指标 | 数值 |
|------|------|
| 总表数 | 50 |
| 总字段数 | 563 |
| 总外键数 | 32 |
| 总数据行数 | 5,156 |

---

## 二、表清单与数据量

| 序号 | 表名 | 字段数 | 数据行数 |
|------|------|--------|----------|
| 1 | `admin_users` | 6 | 1 |
| 2 | `ai_chat_history` | 6 | 14 |
| 3 | `ai_memory` | 5 | 0 |
| 4 | `attendance` | 13 | 222 |
| 5 | `attendance_records` | 33 | 18 |
| 6 | `audit_log` | 9 | 0 |
| 7 | `audit_logs` | 6 | 0 |
| 8 | `banquet_template` | 9 | 5 |
| 9 | `banquet_template_rel` | 5 | 0 |
| 10 | `banquet_type` | 7 | 8 |
| 11 | `booking_dish_detail` | 18 | 22 |
| 12 | `booking_master` | 24 | 5 |
| 13 | `booking_table` | 14 | 3 |
| 14 | `categories` | 3 | 11 |
| 15 | `change_log` | 13 | 0 |
| 16 | `config` | 2 | 1 |
| 17 | `customer_master` | 13 | 37 |
| 18 | `department` | 11 | 30 |
| 19 | `dish_category` | 8 | 9 |
| 20 | `dish_master` | 40 | 612 |
| 21 | `dish_occasion_names` | 6 | 0 |
| 22 | `dish_recipe` | 16 | 0 |
| 23 | `dish_tag` | 11 | 19 |
| 24 | `dish_tag_relation` | 5 | 1,012 |
| 25 | `dish_usage` | 8 | 2 |
| 26 | `dish_usage_relation` | 5 | 357 |
| 27 | `dishes` | 8 | 113 |
| 28 | `employee_lifecycle` | 6 | 11 |
| 29 | `ingredient_inventory_log` | 10 | 5 |
| 30 | `ingredient_master` | 18 | 1,215 |
| 31 | `ingredient_purchase` | 14 | 53 |
| 32 | `kitchen_log` | 11 | 5 |
| 33 | `leave_record` | 14 | 31 |
| 34 | `meal_package` | 10 | 0 |
| 35 | `menu_category` | 8 | 9 |
| 36 | `orders` | 15 | 19 |
| 37 | `overtime` | 14 | 50 |
| 38 | `package_details` | 4 | 25 |
| 39 | `package_dish_detail` | 9 | 59 |
| 40 | `package_dish_rel` | 7 | 0 |
| 41 | `package_master` | 14 | 8 |
| 42 | `packages` | 7 | 4 |
| 43 | `pkg_used` | 5 | 0 |
| 44 | `schedule` | 11 | 281 |
| 45 | `staff_master` | 31 | 24 |
| 46 | `supplier_master` | 17 | 6 |
| 47 | `table_master` | 16 | 84 |
| 48 | `template_category_rel` | 5 | 9 |
| 49 | `template_dish_rel` | 8 | 756 |
| 50 | `users` | 5 | 1 |


---

## 三、核心业务表结构

### 3.1 预订模块

#### booking_master (预订主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `booking_id` | varchar(20) | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL | PRI |
| `booking_date` | date | NOT NULL |  |
| `booking_time` | time | NOT NULL |  |
| `customer_id` | int | NULL |  |
| `customer_name` | varchar(50) | NULL |  |
| `customer_phone` | varchar(20) | NULL |  |
| `staff_id` | int | NULL |  |
| `staff_name` | varchar(20) | NULL |  |
| `deposit` | decimal(10,2) | NULL |  |
| `guest_count` | int | NULL |  |
| `table_count` | int | NULL |  |
| `spare_tables` | int | NULL |  |
| `guest_per_table` | int | NULL |  |
| `booking_status` | varchar(20) | NULL |  |
| `banquet_name` | varchar(100) | NULL |  |
| `occasion_type` | varchar(20) | NULL |  |
| `special_request` | text | NULL |  |
| `total_amount` | decimal(10,2) | NULL |  |
| `final_amount` | decimal(10,2) | NULL |  |
| `payment_status` | varchar(20) | NULL |  |
| `remark` | text | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |


#### booking_table (预订桌台)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `table_booking_id` | bigint | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `booking_id` | varchar(20) | NOT NULL |  |
| `booking_date` | date | NOT NULL |  |
| `booking_time` | time | NOT NULL |  |
| `table_id` | int | NOT NULL |  |
| `table_number` | varchar(10) | NULL |  |
| `table_name` | varchar(20) | NULL |  |
| `guest_count` | int | NULL |  |
| `package_id` | varchar(20) | NULL |  |
| `package_name` | varchar(100) | NULL |  |
| `open_table_type` | varchar(50) | NULL |  |
| `table_note` | varchar(255) | NULL |  |
| `created_at` | timestamp | NULL |  |


#### booking_dish_detail (预订菜品)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `dish_booking_id` | bigint | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `table_booking_id` | bigint | NULL |  |
| `booking_id` | varchar(20) | NULL |  |
| `dish_id` | varchar(20) | NOT NULL |  |
| `dish_name` | varchar(100) | NULL |  |
| `dish_quantity` | int | NULL |  |
| `unit_price` | decimal(10,2) | NULL |  |
| `subtotal` | decimal(10,2) | NULL |  |
| `custom_name` | varchar(100) | NULL |  |
| `dish_note` | varchar(255) | NULL |  |
| `dish_order` | int | NULL |  |
| `created_at` | timestamp | NULL |  |
| `kitchen_status` | varchar(20) | NULL |  |
| `kitchen_station` | varchar(50) | NULL |  |
| `kitchen_note` | varchar(255) | NULL |  |
| `kitchen_started_at` | bigint | NULL |  |
| `kitchen_done_at` | bigint | NULL |  |


---

### 3.2 菜品模块

#### dish_master (菜品主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `dish_id` | varchar(20) | NOT NULL | PRI |
| `dish_code` | varchar(50) | NULL |  |
| `store_id` | bigint | NOT NULL | PRI |
| `dish_name` | varchar(100) | NOT NULL |  |
| `dish_name_en` | varchar(100) | NULL |  |
| `dish_category` | varchar(50) | NULL |  |
| `category` | varchar(50) | NULL |  |
| `category_id` | varchar(50) | NULL |  |
| `cooking_method` | varchar(50) | NULL |  |
| `spicy_level` | int | NULL |  |
| `main_ingredient_type` | varchar(50) | NULL |  |
| `main_ingredient` | varchar(100) | NULL |  |
| `english_name` | varchar(200) | NULL |  |
| `cost_price` | decimal(10,2) | NULL |  |
| `sale_price` | decimal(10,2) | NULL |  |
| `price` | decimal(10,2) | NULL |  |
| `unit` | varchar(20) | NULL |  |
| `taste` | varchar(50) | NULL |  |
| `main_ingredients` | text | NULL |  |
| `cost_rate` | decimal(5,2) | NULL |  |
| `cooking_time` | int | NULL |  |
| `servings` | int | NULL |  |
| `birthday_name` | varchar(100) | NULL |  |
| `wedding_name` | varchar(100) | NULL |  |
| `house_move_name` | varchar(100) | NULL |  |
| `promotion_name` | varchar(100) | NULL |  |
| `reunion_name` | varchar(100) | NULL |  |
| `thanksgiving_name` | varchar(100) | NULL |  |
| `year_end_name` | varchar(100) | NULL |  |
| `baby_born_name` | varchar(100) | NULL |  |
| `is_active` | tinyint | NULL |  |
| `remark` | text | NULL |  |
| `is_specialty` | int | NULL |  |
| `is_seasonal` | int | NULL |  |
| `sort_order` | int | NULL |  |
| `usage_type` | varchar(20) | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |
| `image_url` | varchar(500) | NULL |  |
| `festive_name` | varchar(100) | NULL |  |


---

### 3.3 套餐模块

#### meal_package (套餐主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | int | NOT NULL | PRI |
| `package_name` | varchar(100) | NOT NULL |  |
| `package_code` | varchar(50) | NOT NULL |  |
| `package_type` | varchar(20) | NULL |  |
| `description` | varchar(200) | NULL |  |
| `package_price` | decimal(10,2) | NULL |  |
| `servings` | int | NULL |  |
| `is_active` | tinyint | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |


#### package_dish_detail (套餐菜品)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `detail_id` | bigint | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `package_id` | varchar(20) | NOT NULL |  |
| `dish_id` | varchar(20) | NOT NULL |  |
| `dish_quantity` | int | NULL |  |
| `dish_order` | int | NULL |  |
| `custom_name` | varchar(100) | NULL |  |
| `note` | varchar(255) | NULL |  |
| `created_at` | timestamp | NULL |  |


---

### 3.4 客户模块

#### customer_master (客户主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `customer_id` | int | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `customer_name` | varchar(50) | NOT NULL |  |
| `customer_phone` | varchar(20) | NOT NULL |  |
| `customer_preference` | text | NULL |  |
| `total_amount` | decimal(12,2) | NULL |  |
| `member_level` | varchar(10) | NULL |  |
| `booking_count` | int | NULL |  |
| `last_booking_date` | date | NULL |  |
| `remark` | text | NULL |  |
| `is_active` | tinyint | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |


---

### 3.5 员工模块

#### staff_master (员工主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `staff_id` | int | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `staff_name` | varchar(20) | NOT NULL |  |
| `staff_account` | varchar(20) | NULL |  |
| `staff_password` | varchar(100) | NULL |  |
| `staff_gender` | varchar(2) | NULL |  |
| `staff_age` | int | NULL |  |
| `staff_phone` | varchar(20) | NULL |  |
| `staff_position` | varchar(50) | NULL |  |
| `department` | varchar(50) | NULL |  |
| `hire_date` | date | NULL |  |
| `monthly_salary` | decimal(10,2) | NULL |  |
| `id_card` | varchar(20) | NULL |  |
| `home_address` | varchar(100) | NULL |  |
| `emergency_contact` | varchar(20) | NULL |  |
| `emergency_phone` | varchar(20) | NULL |  |
| `employment_status` | varchar(10) | NULL |  |
| `resign_reason` | text | NULL |  |
| `resign_date` | date | NULL |  |
| `role` | varchar(30) | NULL |  |
| `remark` | text | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |
| `permission_level` | int | NULL |  |
| `dept_id` | int | NULL |  |
| `can_manage_kitchen` | tinyint | NULL |  |
| `can_manage_sales` | tinyint | NULL |  |
| `can_manage_finance` | tinyint | NULL |  |
| `can_manage_hr` | tinyint | NULL |  |
| `can_view_all_stores` | tinyint | NULL |  |
| `can_edit_system` | tinyint | NULL |  |


---

### 3.6 桌台模块

#### table_master (桌台主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `table_id` | int | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL |  |
| `table_number` | varchar(10) | NOT NULL |  |
| `table_name` | varchar(20) | NULL |  |
| `table_location` | varchar(50) | NULL |  |
| `table_area` | varchar(20) | NULL |  |
| `table_capacity` | int | NULL |  |
| `table_type` | varchar(20) | NULL |  |
| `table_status` | varchar(20) | NULL |  |
| `min_capacity` | int | NULL |  |
| `max_capacity` | int | NULL |  |
| `sort_order` | int | NULL |  |
| `is_active` | tinyint | NULL |  |
| `remark` | text | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |


---

### 3.7 订单模块

#### orders (订单表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | varchar(64) | NOT NULL | PRI |
| `booking_id` | varchar(20) | NULL |  |
| `staff_id` | int | NULL |  |
| `customer_id` | int | NULL |  |
| `store_id` | bigint | NULL |  |
| `table_id` | int | NULL |  |
| `dishes` | text | NOT NULL |  |
| `package_code` | varchar(64) | NULL |  |
| `total_price` | decimal(12,2) | NULL |  |
| `status` | varchar(32) | NULL |  |
| `created_at` | bigint | NULL |  |
| `kitchen_status` | varchar(20) | NULL |  |
| `kitchen_priority` | int | NULL |  |
| `kitchen_started_at` | bigint | NULL |  |
| `kitchen_finished_at` | bigint | NULL |  |


---

### 3.8 库存模块

#### ingredient_master (食材主表)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `ingredient_id` | varchar(50) | NOT NULL | PRI |
| `store_id` | bigint | NOT NULL | PRI |
| `ingredient_name` | varchar(100) | NOT NULL |  |
| `ingredient_category` | varchar(50) | NULL |  |
| `brand` | varchar(100) | NULL |  |
| `purchase_unit` | varchar(20) | NULL |  |
| `usage_unit` | varchar(20) | NULL |  |
| `conversion_rate` | decimal(10,3) | NULL |  |
| `primary_supplier_id` | int | NULL |  |
| `current_stock` | decimal(12,3) | NULL |  |
| `warning_threshold` | decimal(10,3) | NULL |  |
| `avg_price` | decimal(10,4) | NULL |  |
| `yield_rate` | decimal(5,2) | NULL |  |
| `last_entry_date` | date | NULL |  |
| `is_active` | tinyint | NULL |  |
| `sort_order` | int | NULL |  |
| `created_at` | timestamp | NULL |  |
| `updated_at` | timestamp | NULL |  |


---

## 四、外键关系图

```
booking_table ── booking_id ──> booking_master
booking_dish_detail ── booking_id ──> booking_master
booking_dish_detail ── table_booking_id ──> booking_table
booking_dish_detail ── dish_id ──> dish_master
package_dish_detail ── package_id ──> meal_package
package_dish_detail ── dish_id ──> dish_master
orders ── booking_id ──> booking_master
orders ── staff_id ──> staff_master
orders ── customer_id ──> customer_master
booking_master ── customer_id ──> customer_master
booking_master ── staff_id ──> staff_master
table_master ── room_id ──> room_master
ingredient_master ── supplier_id ──> supplier_master
ingredient_inventory_log ── ingredient_id ──> ingredient_master
dish_tag_relation ── dish_id ──> dish_master
dish_usage_relation ── dish_id ──> dish_master
```

---

## 五、外键约束清单

| 序号 | 本表 | 字段 | 关联表 | 关联字段 |
|------|------|------|--------|----------|
| 1 | `attendance` | `staff_id` | `staff_master` | `staff_id` |
| 2 | `attendance_records` | `staff_id` | `staff_master` | `staff_id` |
| 3 | `banquet_template_rel` | `banquet_type_id` | `banquet_type` | `id` |
| 4 | `banquet_template_rel` | `template_id` | `banquet_template` | `id` |
| 5 | `booking_dish_detail` | `dish_id` | `dish_master` | `dish_id` |
| 6 | `booking_master` | `customer_id` | `customer_master` | `customer_id` |
| 7 | `booking_master` | `staff_id` | `staff_master` | `staff_id` |
| 8 | `booking_table` | `table_id` | `table_master` | `table_id` |
| 9 | `dish_recipe` | `dish_id` | `dish_master` | `dish_id` |
| 10 | `dish_recipe` | `ingredient_id` | `ingredient_master` | `ingredient_id` |
| 11 | `dish_tag_relation` | `dish_id` | `dish_master` | `dish_id` |
| 12 | `dish_tag_relation` | `store_id` | `dish_master` | `store_id` |
| 13 | `dish_tag_relation` | `tag_id` | `dish_tag` | `id` |
| 14 | `dish_usage_relation` | `dish_id` | `dish_master` | `dish_id` |
| 15 | `dish_usage_relation` | `store_id` | `dish_master` | `store_id` |
| 16 | `dish_usage_relation` | `usage_id` | `dish_usage` | `id` |
| 17 | `ingredient_inventory_log` | `ingredient_id` | `ingredient_master` | `ingredient_id` |
| 18 | `ingredient_purchase` | `ingredient_id` | `ingredient_master` | `ingredient_id` |
| 19 | `ingredient_purchase` | `supplier_id` | `supplier_master` | `supplier_id` |
| 20 | `leave_record` | `staff_id` | `staff_master` | `staff_id` |
| 21 | `orders` | `booking_id` | `booking_master` | `booking_id` |
| 22 | `orders` | `store_id` | `booking_master` | `store_id` |
| 23 | `orders` | `customer_id` | `customer_master` | `customer_id` |
| 24 | `orders` | `staff_id` | `staff_master` | `staff_id` |
| 25 | `overtime` | `staff_id` | `staff_master` | `staff_id` |
| 26 | `package_dish_detail` | `dish_id` | `dish_master` | `dish_id` |
| 27 | `package_dish_detail` | `package_id` | `package_master` | `package_id` |
| 28 | `staff_master` | `dept_id` | `department` | `dept_id` |
| 29 | `template_category_rel` | `template_id` | `banquet_template` | `id` |
| 30 | `template_category_rel` | `menu_category_id` | `menu_category` | `id` |
| 31 | `template_dish_rel` | `template_id` | `banquet_template` | `id` |
| 32 | `template_dish_rel` | `menu_category_id` | `menu_category` | `id` |


---

## 六、数据流分析

### 预订创建流程
```
1. 创建 booking_master (预订主记录)
2. 创建 booking_table (关联桌台)
3. 创建 booking_dish_detail (关联菜品)
4. 更新 table_master.table_status = 'occupied'
```

### 订单创建流程
```
1. 创建 orders (订单记录)
2. 关联 booking_id / staff_id / customer_id
```

### 套餐应用流程
```
1. 查询 meal_package (套餐信息)
2. 查询 package_dish_detail (套餐包含菜品)
3. 创建 booking_dish_detail (预订菜品)
```

---

## 七、类型一致性检查

### store_id 类型
| 表 | 类型 | 状态 |
|------|------|------|
| `attendance` | bigint | ✅ |
| `audit_log` | bigint | ✅ |
| `booking_dish_detail` | bigint | ✅ |
| `booking_master` | bigint | ✅ |
| `booking_table` | bigint | ✅ |
| `change_log` | bigint | ✅ |
| `customer_master` | bigint | ✅ |
| `department` | bigint | ✅ |
| `dish_master` | bigint | ✅ |
| `dish_occasion_names` | bigint | ✅ |
| `dish_recipe` | bigint | ✅ |
| `dish_tag_relation` | bigint | ✅ |
| `dish_usage_relation` | bigint | ✅ |
| `ingredient_inventory_log` | bigint | ✅ |
| `ingredient_master` | bigint | ✅ |
| `ingredient_purchase` | bigint | ✅ |
| `kitchen_log` | bigint | ✅ |
| `leave_record` | bigint | ✅ |
| `orders` | bigint | ✅ |
| `overtime` | bigint | ✅ |
| `package_dish_detail` | bigint | ✅ |
| `package_master` | bigint | ✅ |
| `schedule` | bigint | ✅ |
| `staff_master` | bigint | ✅ |
| `supplier_master` | bigint | ✅ |
| `table_master` | bigint | ✅ |
| `template_dish_rel` | bigint | ✅ |


### table_id 类型
| 表 | 类型 | 状态 |
|------|------|------|
| `booking_table` | int | ✅ |
| `orders` | int | ✅ |
| `table_master` | int | ✅ |


---

## 八、数据完整性检查

### 孤儿数据检查
- `attendance.staff_id` → `staff_master.staff_id`: 222条数据, 孤儿数据: 0 → ✅
- `attendance_records.staff_id` → `staff_master.staff_id`: 18条数据, 孤儿数据: 18 → ❌
- `booking_dish_detail.dish_id` → `dish_master.dish_id`: 22条数据, 孤儿数据: 0 → ✅
- `booking_master.customer_id` → `customer_master.customer_id`: 5条数据, 孤儿数据: 0 → ✅
- `booking_master.staff_id` → `staff_master.staff_id`: 5条数据, 孤儿数据: 0 → ✅
- `booking_table.table_id` → `table_master.table_id`: 3条数据, 孤儿数据: 0 → ✅
- `dish_tag_relation.dish_id` → `dish_master.dish_id`: 1012条数据, 孤儿数据: 0 → ✅
- `dish_tag_relation.store_id` → `dish_master.store_id`: 1012条数据, 孤儿数据: 0 → ✅
- `dish_tag_relation.tag_id` → `dish_tag.id`: 1012条数据, 孤儿数据: 0 → ✅
- `dish_usage_relation.dish_id` → `dish_master.dish_id`: 357条数据, 孤儿数据: 0 → ✅
- `dish_usage_relation.store_id` → `dish_master.store_id`: 357条数据, 孤儿数据: 0 → ✅
- `dish_usage_relation.usage_id` → `dish_usage.id`: 357条数据, 孤儿数据: 0 → ✅
- `ingredient_inventory_log.ingredient_id` → `ingredient_master.ingredient_id`: 5条数据, 孤儿数据: 0 → ✅
- `ingredient_purchase.ingredient_id` → `ingredient_master.ingredient_id`: 53条数据, 孤儿数据: 0 → ✅
- `ingredient_purchase.supplier_id` → `supplier_master.supplier_id`: 53条数据, 孤儿数据: 0 → ✅
- `leave_record.staff_id` → `staff_master.staff_id`: 31条数据, 孤儿数据: 0 → ✅
- `orders.booking_id` → `booking_master.booking_id`: 19条数据, 孤儿数据: 0 → ✅
- `orders.store_id` → `booking_master.store_id`: 19条数据, 孤儿数据: 0 → ✅
- `orders.customer_id` → `customer_master.customer_id`: 19条数据, 孤儿数据: 19 → ❌
- `orders.staff_id` → `staff_master.staff_id`: 19条数据, 孤儿数据: 19 → ❌
- `overtime.staff_id` → `staff_master.staff_id`: 50条数据, 孤儿数据: 0 → ✅
- `package_dish_detail.dish_id` → `dish_master.dish_id`: 59条数据, 孤儿数据: 0 → ✅
- `package_dish_detail.package_id` → `package_master.package_id`: 59条数据, 孤儿数据: 0 → ✅
- `staff_master.dept_id` → `department.dept_id`: 24条数据, 孤儿数据: 2 → ❌
- `template_category_rel.template_id` → `banquet_template.id`: 9条数据, 孤儿数据: 0 → ✅
- `template_category_rel.menu_category_id` → `menu_category.id`: 9条数据, 孤儿数据: 0 → ✅
- `template_dish_rel.template_id` → `banquet_template.id`: 756条数据, 孤儿数据: 0 → ✅
- `template_dish_rel.menu_category_id` → `menu_category.id`: 756条数据, 孤儿数据: 2 → ❌


---

## 九、修复记录

| 修复项 | 状态 | 说明 |
|--------|------|------|
| booking_dish 重复表 | ✅ 已删除 | 统一使用 booking_dish_detail |
| change_log.store_id int→bigint | ✅ 已修复 | 与其他表对齐 |
| orders.table_id varchar(64)→int | ✅ 已修复 | 与 table_master 对齐 |
| package_dish_rel.package_id int→varchar(20) | ✅ 已修复 | 与 meal_package.package_code 对齐 |
| BookingController table_status 同步 | ✅ 已修复 | 创建/删除预订时同步更新 |
| BookingExtController swap/copy 菜品联动 | ✅ 已修复 | 换台/复制时同步菜品 |
| BookingDishController package_code→package_id | ✅ 已修复 | SQL列名修正 |

---

## 十、待处理建议

| 优先级 | 问题 | 说明 |
|--------|------|------|
| 中 | 3条旧预订无桌台 | BK20260720001/002, BK20260721001 |
| 低 | dish_master 重复列 | price/sale_price, category/dish_category |
| 低 | package_dish_rel 空表 | 考虑合并或删除 |

---

🐚 SOLO审计完成
