# 数据库表结构汇总 - 自动生成
# 生成时间: 2026-07-22
# 数据库: banquet

## admin_users
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| username | varchar(50) | NO | UNI | NULL |  |
| password | varchar(255) | NO |  | NULL |  |
| real_name | varchar(50) | YES |  | NULL |  |
| role | enum('admin','manager','staff') | YES |  | staff |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## ai_chat_history
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | bigint | NO | PRI | NULL | auto_increment |
| staff_id | bigint | NO | MUL | NULL |  |
| role | varchar(20) | NO |  | NULL |  |
| content | text | NO |  | NULL |  |
| image_url | varchar(500) | YES |  | NULL |  |
| created_at | datetime | YES | MUL | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## ai_memory
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | bigint | NO | PRI | NULL | auto_increment |
| user_id | bigint | NO | MUL | NULL |  |
| content | text | NO |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## attendance
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| attendance_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES |  | 1 |  |
| staff_id | int | NO | MUL | NULL |  |
| attendance_date | date | NO |  | NULL |  |
| clock_in | datetime | YES |  | NULL |  |
| clock_out | datetime | YES |  | NULL |  |
| status | varchar(20) | YES |  | normal |  |
| late_minutes | int | YES |  | 0 |  |
| early_leave_minutes | int | YES |  | 0 |  |
| absent | tinyint(1) | YES |  | 0 |  |
| work_hours | double | YES |  | 0 |  |
| remark | varchar(200) | YES |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## attendance_records
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| staff_id | int | YES | MUL | NULL |  |
| record_id | varchar(50) | NO | UNI | NULL |  |
| emp_id | varchar(50) | NO | MUL | NULL |  |
| emp_name | varchar(50) | YES |  | NULL |  |
| department | varchar(50) | YES |  | NULL |  |
| month | varchar(7) | NO | MUL | NULL |  |
| scope | varchar(10) | NO |  | full |  |
| day_num | int | NO |  | NULL |  |
| am_type | varchar(20) | YES |  | NULL |  |
| pm_type | varchar(20) | YES |  | NULL |  |
| am_note | text | YES |  | NULL |  |
| pm_note | text | YES |  | NULL |  |
| day_note | text | YES |  | NULL |  |
| employment | varchar(20) | YES |  | 全勤在职 |  |
| salary_status | varchar(20) | YES |  | 未发放 |  |
| public_holiday | int | YES |  | 6 |  |
| carry_over | int | YES |  | 0 |  |
| summary_notes | text | YES |  | NULL |  |
| total_present | decimal(6,1) | YES |  | 0.0 |  |
| total_statutory | decimal(6,1) | YES |  | 0.0 |  |
| total_holiday | decimal(6,1) | YES |  | 0.0 |  |
| total_comp | decimal(6,1) | YES |  | 0.0 |  |
| total_travel | decimal(6,1) | YES |  | 0.0 |  |
| total_overtime | decimal(6,1) | YES |  | 0.0 |  |
| total_leave | decimal(6,1) | YES |  | 0.0 |  |
| total_late | decimal(6,1) | YES |  | 0.0 |  |
| total_early | decimal(6,1) | YES |  | 0.0 |  |
| total_absent | decimal(6,1) | YES |  | 0.0 |  |
| final_balance | decimal(6,1) | YES |  | 0.0 |  |
| recorded_days | int | YES |  | 0 |  |
| created_by | varchar(50) | YES |  | Rino |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## audit_log
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| log_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES | MUL | 1 |  |
| username | varchar(50) | YES | MUL | NULL |  |
| action | varchar(100) | YES |  | NULL |  |
| module | varchar(50) | YES | MUL | NULL |  |
| target_id | varchar(50) | YES |  | NULL |  |
| detail | text | YES |  | NULL |  |
| ip_address | varchar(50) | YES |  | NULL |  |
| created_at | datetime | YES | MUL | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## audit_logs
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| user_id | varchar(64) | YES | MUL | NULL |  |
| action | varchar(128) | YES |  | NULL |  |
| target | varchar(256) | YES |  | NULL |  |
| detail | text | YES |  | NULL |  |
| created_at | bigint | YES | MUL | unix_timestamp() | DEFAULT_GENERATED |
## banquet_template
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| template_name | varchar(100) | NO |  | NULL |  |
| template_code | varchar(50) | NO | UNI | NULL |  |
| template_type | varchar(20) | NO |  | NULL |  |
| description | varchar(200) | YES |  | NULL |  |
| base_price | decimal(10,2) | YES |  | NULL |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## banquet_template_rel
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| banquet_type_id | int | NO | MUL | NULL |  |
| template_id | int | NO | MUL | NULL |  |
| is_default | tinyint | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## banquet_type
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| type_name | varchar(50) | NO |  | NULL |  |
| type_code | varchar(50) | NO | UNI | NULL |  |
| description | varchar(200) | YES |  | NULL |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## booking_dish
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | bigint | NO | PRI | NULL | auto_increment |
| booking_id | varchar(20) | NO | MUL | NULL |  |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| dish_name | varchar(100) | YES |  | NULL |  |
| price | decimal(10,2) | YES |  | NULL |  |
| quantity | int | YES |  | 1 |  |
| remark | varchar(200) | YES |  | NULL |  |
| sort_order | int | YES |  | 0 |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## booking_dish_detail
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| dish_booking_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| table_booking_id | bigint | YES | MUL | NULL |  |
| booking_id | varchar(20) | YES | MUL | NULL |  |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| dish_name | varchar(100) | YES |  | NULL |  |
| dish_quantity | int | YES |  | 1 |  |
| unit_price | decimal(10,2) | YES |  | 0.00 |  |
| subtotal | decimal(10,2) | YES |  | 0.00 |  |
| custom_name | varchar(100) | YES |  | NULL |  |
| dish_note | varchar(255) | YES |  | NULL |  |
| dish_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| kitchen_status | varchar(20) | YES |  | pending |  |
| kitchen_station | varchar(50) | YES |  | NULL |  |
| kitchen_note | varchar(255) | YES |  | NULL |  |
| kitchen_started_at | bigint | YES |  | NULL |  |
| kitchen_done_at | bigint | YES |  | NULL |  |
## booking_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| booking_id | varchar(20) | NO | PRI | NULL |  |
| store_id | bigint | NO | PRI | 1 |  |
| booking_date | date | NO | MUL | NULL |  |
| booking_time | time | NO |  | NULL |  |
| customer_id | int | YES | MUL | NULL |  |
| customer_name | varchar(50) | YES |  | NULL |  |
| customer_phone | varchar(20) | YES | MUL | NULL |  |
| staff_id | int | YES | MUL | NULL |  |
| staff_name | varchar(20) | YES |  | NULL |  |
| deposit | decimal(10,2) | YES |  | 0.00 |  |
| guest_count | int | YES |  | 0 |  |
| table_count | int | YES |  | 0 |  |
| spare_tables | int | YES |  | 0 |  |
| guest_per_table | int | YES |  | 10 |  |
| booking_status | varchar(20) | YES | MUL | confirmed |  |
| banquet_name | varchar(100) | YES |  | NULL |  |
| occasion_type | varchar(20) | YES |  | NULL |  |
| special_request | text | YES |  | NULL |  |
| total_amount | decimal(10,2) | YES |  | 0.00 |  |
| final_amount | decimal(10,2) | YES |  | 0.00 |  |
| payment_status | varchar(20) | YES |  | unpaid |  |
| remark | text | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## booking_table
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| table_booking_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| booking_id | varchar(20) | NO | MUL | NULL |  |
| booking_date | date | NO |  | NULL |  |
| booking_time | time | NO |  | NULL |  |
| table_id | int | NO | MUL | NULL |  |
| table_number | varchar(10) | YES |  | NULL |  |
| table_name | varchar(20) | YES |  | NULL |  |
| guest_count | int | YES |  | 0 |  |
| package_id | varchar(20) | YES |  | NULL |  |
| package_name | varchar(100) | YES |  | NULL |  |
| open_table_type | varchar(50) | YES |  | NULL |  |
| table_note | varchar(255) | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## categories
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | varchar(64) | NO | PRI | NULL |  |
| name | varchar(128) | NO |  | NULL |  |
| sort_order | int | YES | MUL | 0 |  |
## change_log
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| log_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | int | NO | MUL | 1 |  |
| operator_id | int | YES | MUL | NULL |  |
| operator_name | varchar(50) | YES |  | NULL |  |
| operation_type | varchar(30) | NO | MUL | NULL |  |
| target_type | varchar(30) | NO | MUL | NULL |  |
| target_id | varchar(50) | YES |  | NULL |  |
| summary | varchar(200) | NO |  | NULL |  |
| detail | text | YES |  | NULL |  |
| old_value | text | YES |  | NULL |  |
| new_value | text | YES |  | NULL |  |
| ip_address | varchar(45) | YES |  | NULL |  |
| created_at | datetime | NO |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## config
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| config_key | varchar(128) | NO | PRI | NULL |  |
| config_value | text | YES |  | NULL |  |
## customer_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| customer_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| customer_name | varchar(50) | NO | MUL | NULL |  |
| customer_phone | varchar(20) | NO | MUL | NULL |  |
| customer_preference | text | YES |  | NULL |  |
| total_amount | decimal(12,2) | YES |  | 0.00 |  |
| member_level | varchar(10) | YES | MUL | v1 |  |
| booking_count | int | YES |  | 0 |  |
| last_booking_date | date | YES |  | NULL |  |
| remark | text | YES |  | NULL |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## department
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| dept_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES |  | 1 |  |
| dept_name | varchar(50) | NO |  | NULL |  |
| dept_code | varchar(20) | YES |  | NULL |  |
| parent_id | int | YES |  | NULL |  |
| sort_order | int | YES |  | 0 |  |
| status | varchar(10) | YES |  | active |  |
| description | varchar(200) | YES |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
| level | int | YES |  | 1 |  |
## dish_category
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| category_name | varchar(50) | NO |  | NULL |  |
| category_code | varchar(50) | NO | UNI | NULL |  |
| description | varchar(200) | YES |  | NULL |  |
| sort_order | int | YES |  | 0 |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## dish_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| dish_id | varchar(20) | NO | PRI | NULL |  |
| dish_code | varchar(50) | YES |  | NULL |  |
| store_id | bigint | NO | PRI | 1 |  |
| dish_name | varchar(100) | NO |  | NULL |  |
| dish_name_en | varchar(100) | YES |  | NULL |  |
| dish_category | varchar(50) | YES | MUL | NULL |  |
| category | varchar(50) | YES |  | NULL |  |
| category_id | varchar(50) | YES |  | NULL |  |
| cooking_method | varchar(50) | YES |  | NULL |  |
| spicy_level | int | YES |  | 0 |  |
| main_ingredient_type | varchar(50) | YES |  | NULL |  |
| main_ingredient | varchar(100) | YES |  | NULL |  |
| english_name | varchar(200) | YES |  | NULL |  |
| cost_price | decimal(10,2) | YES |  | 0.00 |  |
| sale_price | decimal(10,2) | YES |  | 0.00 |  |
| price | decimal(10,2) | YES |  | NULL |  |
| unit | varchar(20) | YES |  | 份 |  |
| taste | varchar(50) | YES |  | NULL |  |
| main_ingredients | text | YES |  | NULL |  |
| cost_rate | decimal(5,2) | YES |  | 0.00 |  |
| cooking_time | int | YES |  | 15 |  |
| servings | int | YES |  | 1 |  |
| birthday_name | varchar(100) | YES |  | NULL |  |
| wedding_name | varchar(100) | YES |  | NULL |  |
| house_move_name | varchar(100) | YES |  | NULL |  |
| promotion_name | varchar(100) | YES |  | NULL |  |
| reunion_name | varchar(100) | YES |  | NULL |  |
| thanksgiving_name | varchar(100) | YES |  | NULL |  |
| year_end_name | varchar(100) | YES |  | NULL |  |
| baby_born_name | varchar(100) | YES |  | NULL |  |
| is_active | tinyint | YES | MUL | 1 |  |
| remark | text | YES |  | NULL |  |
| is_specialty | int | YES |  | 0 |  |
| is_seasonal | int | YES |  | 0 |  |
| sort_order | int | YES |  | 0 |  |
| usage_type | varchar(20) | YES |  | unused |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
| image_url | varchar(500) | YES |  | NULL |  |
| festive_name | varchar(100) | YES |  | NULL |  |
## dish_occasion_names
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| occasion_type | varchar(20) | NO | MUL | NULL |  |
| custom_name | varchar(100) | NO |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## dish_recipe
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| recipe_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| ingredient_id | varchar(50) | NO | MUL | NULL |  |
| ingredient_name | varchar(100) | YES |  | NULL |  |
| unit | varchar(20) | YES |  | NULL |  |
| unit_price | decimal(10,4) | YES |  | 0.0000 |  |
| quantity | decimal(10,3) | YES |  | 0.000 |  |
| total_cost | decimal(10,2) | YES |  | 0.00 |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
| wastage_rate | decimal(5,2) | YES |  | 0.00 |  |
| yield_rate | decimal(5,2) | YES |  | 0.00 |  |
| last_entry_date | date | YES |  | NULL |  |
| net_unit_price | decimal(10,4) | YES |  | 0.0000 |  |
## dish_tag
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| tag_name | varchar(50) | NO |  | NULL |  |
| tag_code | varchar(50) | NO | UNI | NULL |  |
| tag_type | varchar(20) | NO |  | NULL |  |
| dish_category | varchar(50) | YES |  |  |  |
| sort_order | int | YES |  | 0 |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
| import_time | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| menu_date | date | YES |  | NULL |  |
## dish_tag_relation
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| store_id | bigint | NO |  | 1 |  |
| tag_id | int | NO | MUL | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## dish_usage
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| usage_name | varchar(20) | NO |  | NULL |  |
| usage_code | varchar(20) | NO | UNI | NULL |  |
| description | varchar(100) | YES |  |  |  |
| is_active | tinyint | YES |  | 1 |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## dish_usage_relation
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| store_id | bigint | NO |  | 1 |  |
| usage_id | int | NO | MUL | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## dishes
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | varchar(64) | NO | PRI | NULL |  |
| name | varchar(128) | NO | MUL | NULL |  |
| en | varchar(256) | YES |  |  |  |
| category_id | varchar(64) | YES | MUL | NULL |  |
| price | decimal(10,2) | NO |  | NULL |  |
| unit | varchar(32) | YES |  |  |  |
| spicy_level | varchar(64) | YES |  |  |  |
| sort_order | int | YES |  | 0 |  |
## employee_lifecycle
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| emp_id | varchar(50) | NO | MUL | NULL |  |
| emp_name | varchar(50) | YES |  | NULL |  |
| event_type | enum('入职','离职') | NO |  | NULL |  |
| event_date | date | NO | MUL | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## ingredient_inventory_log
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| log_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| ingredient_id | varchar(50) | NO | MUL | NULL |  |
| log_type | varchar(20) | NO | MUL | NULL |  |
| log_quantity | decimal(12,3) | NO |  | NULL |  |
| stock_after | decimal(12,3) | YES |  | 0.000 |  |
| log_time | timestamp | YES | MUL | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| related_order_id | varchar(50) | YES |  | NULL |  |
| operator_id | int | YES |  | NULL |  |
| note | text | YES |  | NULL |  |
## ingredient_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| ingredient_id | varchar(50) | NO | PRI | NULL |  |
| store_id | bigint | NO | PRI | 1 |  |
| ingredient_name | varchar(100) | NO |  | NULL |  |
| ingredient_category | varchar(50) | YES | MUL | NULL |  |
| brand | varchar(100) | YES |  | NULL |  |
| purchase_unit | varchar(20) | YES |  | NULL |  |
| usage_unit | varchar(20) | YES |  | NULL |  |
| conversion_rate | decimal(10,3) | YES |  | 1.000 |  |
| primary_supplier_id | int | YES |  | NULL |  |
| current_stock | decimal(12,3) | YES |  | 0.000 |  |
| warning_threshold | decimal(10,3) | YES |  | 0.000 |  |
| avg_price | decimal(10,4) | YES |  | 0.0000 |  |
| yield_rate | decimal(5,2) | YES |  | 0.00 |  |
| last_entry_date | date | YES |  | NULL |  |
| is_active | tinyint | YES | MUL | 1 |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## ingredient_purchase
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| purchase_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| ingredient_id | varchar(50) | NO | MUL | NULL |  |
| supplier_id | int | YES | MUL | NULL |  |
| purchase_date | date | NO | MUL | NULL |  |
| purchase_quantity | decimal(12,3) | YES |  | 0.000 |  |
| purchase_price | decimal(10,2) | YES |  | 0.00 |  |
| purchase_total | decimal(12,2) | YES |  | 0.00 |  |
| usage_quantity | decimal(12,3) | YES |  | 0.000 |  |
| usage_price | decimal(10,4) | YES |  | 0.0000 |  |
| processing_note | text | YES |  | NULL |  |
| operator_id | int | YES |  | NULL |  |
| status | varchar(20) | YES |  | completed |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## kitchen_log
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| action | varchar(30) | NO |  | NULL |  |
| target_type | varchar(20) | NO |  | NULL |  |
| booking_id | varchar(20) | YES | MUL | NULL |  |
| dish_id | varchar(20) | YES |  | NULL |  |
| dish_name | varchar(100) | YES |  | NULL |  |
| operator_id | int | YES |  | NULL |  |
| operator_name | varchar(50) | YES |  | NULL |  |
| note | text | YES |  | NULL |  |
| created_at | timestamp | YES | MUL | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## leave_record
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| leave_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES |  | 1 |  |
| staff_id | int | NO | MUL | NULL |  |
| leave_type | varchar(20) | NO |  | NULL |  |
| start_date | date | NO |  | NULL |  |
| end_date | date | NO |  | NULL |  |
| days | double | YES |  | 0 |  |
| status | varchar(20) | YES |  | pending |  |
| reason | varchar(500) | YES |  | NULL |  |
| approver_id | int | YES |  | NULL |  |
| approve_time | datetime | YES |  | NULL |  |
| approve_remark | varchar(200) | YES |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## meal_package
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| package_name | varchar(100) | NO |  | NULL |  |
| package_code | varchar(50) | NO | UNI | NULL |  |
| package_type | varchar(20) | YES |  | NULL |  |
| description | varchar(200) | YES |  | NULL |  |
| package_price | decimal(10,2) | YES |  | NULL |  |
| servings | int | YES |  | 10 |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## menu_category
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| category_name | varchar(50) | NO |  | NULL |  |
| category_code | varchar(50) | NO | UNI | NULL |  |
| description | varchar(200) | YES |  | NULL |  |
| sort_order | int | YES |  | 0 |  |
| is_active | tinyint | YES |  | 1 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## orders
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | varchar(64) | NO | PRI | NULL |  |
| booking_id | varchar(20) | YES | MUL | NULL |  |
| staff_id | int | YES | MUL | NULL |  |
| customer_id | int | YES | MUL | NULL |  |
| store_id | bigint | YES |  | NULL |  |
| table_id | varchar(64) | YES | MUL |  |  |
| dishes | text | NO |  | NULL |  |
| package_code | varchar(64) | YES |  |  |  |
| total_price | decimal(12,2) | YES |  | 0.00 |  |
| status | varchar(32) | YES | MUL | pending |  |
| created_at | bigint | YES | MUL | unix_timestamp() | DEFAULT_GENERATED |
| kitchen_status | varchar(20) | YES |  | pending |  |
| kitchen_priority | int | YES |  | 0 |  |
| kitchen_started_at | bigint | YES |  | NULL |  |
| kitchen_finished_at | bigint | YES |  | NULL |  |
## overtime
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| overtime_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES |  | 1 |  |
| staff_id | int | NO | MUL | NULL |  |
| overtime_date | date | NO |  | NULL |  |
| start_time | datetime | YES |  | NULL |  |
| end_time | datetime | YES |  | NULL |  |
| hours | double | YES |  | 0 |  |
| status | varchar(20) | YES |  | pending |  |
| reason | varchar(500) | YES |  | NULL |  |
| approver_id | int | YES |  | NULL |  |
| approve_time | datetime | YES |  | NULL |  |
| approve_remark | varchar(200) | YES |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## package_details
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| package_code | varchar(64) | NO | MUL | NULL |  |
| dish_code | varchar(64) | NO | MUL | NULL |  |
| seq | int | NO |  | NULL |  |
## package_dish_detail
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| detail_id | bigint | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| package_id | varchar(20) | NO | MUL | NULL |  |
| dish_id | varchar(20) | NO | MUL | NULL |  |
| dish_quantity | int | YES |  | 1 |  |
| dish_order | int | YES |  | 0 |  |
| custom_name | varchar(100) | YES |  | NULL |  |
| note | varchar(255) | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## package_dish_rel
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| package_id | int | NO | MUL | NULL |  |
| dish_id | varchar(20) | NO |  | NULL |  |
| dish_name_snapshot | varchar(100) | YES |  | NULL |  |
| quantity | int | YES |  | 1 |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## package_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| package_id | varchar(20) | NO | PRI | NULL |  |
| store_id | bigint | NO | PRI | 1 |  |
| package_name | varchar(100) | NO |  | NULL |  |
| package_total_price | decimal(10,2) | YES |  | 0.00 |  |
| package_cost_price | decimal(10,2) | YES |  | 0.00 |  |
| cost_rate | decimal(5,2) | YES |  | 0.00 |  |
| dish_count | int | YES |  | 0 |  |
| suggest_guests | int | YES |  | 10 |  |
| occasion_type | varchar(20) | YES | MUL | NULL |  |
| package_series | varchar(20) | YES |  | NULL |  |
| is_active | tinyint | YES | MUL | 1 |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## packages
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| code | varchar(64) | NO | PRI | NULL |  |
| name | varchar(128) | NO | MUL | NULL |  |
| en | varchar(256) | YES |  |  |  |
| price | decimal(10,2) | NO |  | NULL |  |
| people | varchar(64) | YES |  |  |  |
| desc | text | YES |  | NULL |  |
| created_at | bigint | YES |  | unix_timestamp() | DEFAULT_GENERATED |
## pkg_used
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| order_key | varchar(128) | NO | UNI | NULL |  |
| package_code | varchar(64) | NO |  | NULL |  |
| package_info | text | NO |  | NULL |  |
| used_at | bigint | YES | MUL | unix_timestamp() | DEFAULT_GENERATED |
## schedule
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| schedule_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | YES |  | 1 |  |
| staff_id | int | NO | MUL | NULL |  |
| schedule_date | date | NO |  | NULL |  |
| shift_type | varchar(20) | YES |  | NULL |  |
| start_time | datetime | YES |  | NULL |  |
| end_time | datetime | YES |  | NULL |  |
| status | varchar(20) | YES |  | normal |  |
| remark | varchar(200) | YES |  | NULL |  |
| created_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | datetime | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## staff_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| staff_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| staff_name | varchar(20) | NO |  | NULL |  |
| staff_account | varchar(20) | YES | MUL | NULL |  |
| staff_password | varchar(100) | YES |  | NULL |  |
| staff_gender | varchar(2) | YES |  | NULL |  |
| staff_age | int | YES |  | NULL |  |
| staff_phone | varchar(20) | YES | MUL | NULL |  |
| staff_position | varchar(50) | YES |  | NULL |  |
| department | varchar(50) | YES |  | NULL |  |
| hire_date | date | YES |  | NULL |  |
| monthly_salary | decimal(10,2) | YES |  | 0.00 |  |
| id_card | varchar(20) | YES |  | NULL |  |
| home_address | varchar(100) | YES |  | NULL |  |
| emergency_contact | varchar(20) | YES |  | NULL |  |
| emergency_phone | varchar(20) | YES |  | NULL |  |
| employment_status | varchar(10) | YES | MUL | active |  |
| resign_reason | text | YES |  | NULL |  |
| resign_date | date | YES |  | NULL |  |
| role | varchar(30) | YES |  | NULL |  |
| remark | text | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
| permission_level | int | YES |  | 0 |  |
| dept_id | int | YES | MUL | NULL |  |
| can_manage_kitchen | tinyint | YES |  | 0 |  |
| can_manage_sales | tinyint | YES |  | 0 |  |
| can_manage_finance | tinyint | YES |  | 0 |  |
| can_manage_hr | tinyint | YES |  | 0 |  |
| can_view_all_stores | tinyint | YES |  | 0 |  |
| can_edit_system | tinyint | YES |  | 0 |  |
## supplier_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| supplier_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| supplier_code | varchar(20) | YES |  | NULL |  |
| supplier_name | varchar(100) | NO | MUL | NULL |  |
| contact_person | varchar(50) | YES |  | NULL |  |
| contact_phone | varchar(20) | YES |  | NULL |  |
| bank_account | varchar(50) | YES |  | NULL |  |
| platform_account | varchar(100) | YES |  | NULL |  |
| main_products | text | YES |  | NULL |  |
| wechat_account | varchar(50) | YES |  | NULL |  |
| alipay_account | varchar(50) | YES |  | NULL |  |
| taobao_account | varchar(50) | YES |  | NULL |  |
| supplier_rating | int | YES |  | 5 |  |
| is_active | tinyint | YES | MUL | 1 |  |
| remark | text | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## table_master
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| table_id | int | NO | PRI | NULL | auto_increment |
| store_id | bigint | NO | MUL | 1 |  |
| table_number | varchar(10) | NO | MUL | NULL |  |
| table_name | varchar(20) | YES |  | NULL |  |
| table_location | varchar(50) | YES |  | NULL |  |
| table_area | varchar(20) | YES | MUL | NULL |  |
| table_capacity | int | YES |  | 10 |  |
| table_type | varchar(20) | YES |  | NULL |  |
| table_status | varchar(20) | YES | MUL | available |  |
| min_capacity | int | YES |  | 6 |  |
| max_capacity | int | YES |  | 12 |  |
| sort_order | int | YES |  | 0 |  |
| is_active | tinyint | YES |  | 1 |  |
| remark | text | YES |  | NULL |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
## template_category_rel
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| template_id | int | NO | MUL | NULL |  |
| menu_category_id | int | NO | MUL | NULL |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## template_dish_rel
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | int | NO | PRI | NULL | auto_increment |
| template_id | int | NO | MUL | NULL |  |
| dish_id | varchar(20) | NO |  | NULL |  |
| store_id | bigint | NO |  | 1 |  |
| menu_category_id | int | YES | MUL | NULL |  |
| special_price | decimal(10,2) | YES |  | NULL |  |
| sort_order | int | YES |  | 0 |  |
| created_at | timestamp | YES |  | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
## users
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外属性 |
|--------|------|----------|-----|--------|----------|
| Field | Type | Null | Key | Default | Extra |
| id | varchar(64) | NO | PRI | NULL |  |
| username | varchar(64) | NO | UNI | NULL |  |
| password | varchar(128) | NO |  | NULL |  |
| role | varchar(32) | YES |  | user |  |
| created_at | bigint | YES |  | unix_timestamp() | DEFAULT_GENERATED |