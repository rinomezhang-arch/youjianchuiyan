# banquet 数据库字段清单 - 2026-07-22 21:10
## 基准快照（SOLO对齐前）

### booking_master (24 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| booking_id | varchar(20) | NULL | NO |
| store_id | bigint | 1 | NO |
| booking_date | date | NULL | NO |
| booking_time | time | NULL | NO |
| customer_id | int | NULL | YES |
| customer_name | varchar(50) | NULL | YES |
| customer_phone | varchar(20) | NULL | YES |
| staff_id | int | NULL | YES |
| staff_name | varchar(20) | NULL | YES |
| deposit | decimal(10,2) | 0.00 | YES |
| guest_count | int | 0 | YES |
| table_count | int | 0 | YES |
| spare_tables | int | 0 | YES |
| guest_per_table | int | 10 | YES |
| booking_status | varchar(20) | confirmed | YES |
| banquet_name | varchar(100) | NULL | YES |
| occasion_type | varchar(20) | NULL | YES |
| special_request | text | NULL | YES |
| total_amount | decimal(10,2) | 0.00 | YES |
| final_amount | decimal(10,2) | 0.00 | YES |
| payment_status | varchar(20) | unpaid | YES |
| remark | text | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### booking_table (14 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| table_booking_id | bigint | NULL | NO |
| store_id | bigint | 1 | NO |
| booking_id | varchar(20) | NULL | NO |
| booking_date | date | NULL | NO |
| booking_time | time | NULL | NO |
| table_id | int | NULL | NO |
| table_number | varchar(10) | NULL | YES |
| table_name | varchar(20) | NULL | YES |
| guest_count | int | 0 | YES |
| package_id | varchar(20) | NULL | YES |
| package_name | varchar(100) | NULL | YES |
| open_table_type | varchar(50) | NULL | YES |
| table_note | varchar(255) | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |

### booking_dish (0 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|

### booking_dish_detail (18 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| dish_booking_id | bigint | NULL | NO |
| store_id | bigint | 1 | NO |
| table_booking_id | bigint | NULL | YES |
| booking_id | varchar(20) | NULL | YES |
| dish_id | varchar(20) | NULL | NO |
| dish_name | varchar(100) | NULL | YES |
| dish_quantity | int | 1 | YES |
| unit_price | decimal(10,2) | 0.00 | YES |
| subtotal | decimal(10,2) | 0.00 | YES |
| custom_name | varchar(100) | NULL | YES |
| dish_note | varchar(255) | NULL | YES |
| dish_order | int | 0 | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| kitchen_status | varchar(20) | pending | YES |
| kitchen_station | varchar(50) | NULL | YES |
| kitchen_note | varchar(255) | NULL | YES |
| kitchen_started_at | bigint | NULL | YES |
| kitchen_done_at | bigint | NULL | YES |

### table_master (16 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| table_id | int | NULL | NO |
| store_id | bigint | 1 | NO |
| table_number | varchar(10) | NULL | NO |
| table_name | varchar(20) | NULL | YES |
| table_location | varchar(50) | NULL | YES |
| table_area | varchar(20) | NULL | YES |
| table_capacity | int | 10 | YES |
| table_type | varchar(20) | NULL | YES |
| table_status | varchar(20) | available | YES |
| min_capacity | int | 6 | YES |
| max_capacity | int | 12 | YES |
| sort_order | int | 0 | YES |
| is_active | tinyint | 1 | YES |
| remark | text | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### customer_master (13 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| customer_id | int | NULL | NO |
| store_id | bigint | 1 | NO |
| customer_name | varchar(50) | NULL | NO |
| customer_phone | varchar(20) | NULL | NO |
| customer_preference | text | NULL | YES |
| total_amount | decimal(12,2) | 0.00 | YES |
| member_level | varchar(10) | v1 | YES |
| booking_count | int | 0 | YES |
| last_booking_date | date | NULL | YES |
| remark | text | NULL | YES |
| is_active | tinyint | 1 | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### staff_master (31 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| staff_id | int | NULL | NO |
| store_id | bigint | 1 | NO |
| staff_name | varchar(20) | NULL | NO |
| staff_account | varchar(20) | NULL | YES |
| staff_password | varchar(100) | NULL | YES |
| staff_gender | varchar(2) | NULL | YES |
| staff_age | int | NULL | YES |
| staff_phone | varchar(20) | NULL | YES |
| staff_position | varchar(50) | NULL | YES |
| department | varchar(50) | NULL | YES |
| hire_date | date | NULL | YES |
| monthly_salary | decimal(10,2) | 0.00 | YES |
| id_card | varchar(20) | NULL | YES |
| home_address | varchar(100) | NULL | YES |
| emergency_contact | varchar(20) | NULL | YES |
| emergency_phone | varchar(20) | NULL | YES |
| employment_status | varchar(10) | active | YES |
| resign_reason | text | NULL | YES |
| resign_date | date | NULL | YES |
| role | varchar(30) | NULL | YES |
| remark | text | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |
| permission_level | int | 0 | YES |
| dept_id | int | NULL | YES |
| can_manage_kitchen | tinyint | 0 | YES |
| can_manage_sales | tinyint | 0 | YES |
| can_manage_finance | tinyint | 0 | YES |
| can_manage_hr | tinyint | 0 | YES |
| can_view_all_stores | tinyint | 0 | YES |
| can_edit_system | tinyint | 0 | YES |

### dish_master (40 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| dish_id | varchar(20) | NULL | NO |
| dish_code | varchar(50) | NULL | YES |
| store_id | bigint | 1 | NO |
| dish_name | varchar(100) | NULL | NO |
| dish_name_en | varchar(100) | NULL | YES |
| dish_category | varchar(50) | NULL | YES |
| category | varchar(50) | NULL | YES |
| category_id | varchar(50) | NULL | YES |
| cooking_method | varchar(50) | NULL | YES |
| spicy_level | int | 0 | YES |
| main_ingredient_type | varchar(50) | NULL | YES |
| main_ingredient | varchar(100) | NULL | YES |
| english_name | varchar(200) | NULL | YES |
| cost_price | decimal(10,2) | 0.00 | YES |
| sale_price | decimal(10,2) | 0.00 | YES |
| price | decimal(10,2) | NULL | YES |
| unit | varchar(20) | 份 | YES |
| taste | varchar(50) | NULL | YES |
| main_ingredients | text | NULL | YES |
| cost_rate | decimal(5,2) | 0.00 | YES |
| cooking_time | int | 15 | YES |
| servings | int | 1 | YES |
| birthday_name | varchar(100) | NULL | YES |
| wedding_name | varchar(100) | NULL | YES |
| house_move_name | varchar(100) | NULL | YES |
| promotion_name | varchar(100) | NULL | YES |
| reunion_name | varchar(100) | NULL | YES |
| thanksgiving_name | varchar(100) | NULL | YES |
| year_end_name | varchar(100) | NULL | YES |
| baby_born_name | varchar(100) | NULL | YES |
| is_active | tinyint | 1 | YES |
| remark | text | NULL | YES |
| is_specialty | int | 0 | YES |
| is_seasonal | int | 0 | YES |
| sort_order | int | 0 | YES |
| usage_type | varchar(20) | unused | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |
| image_url | varchar(500) | NULL | YES |
| festive_name | varchar(100) | NULL | YES |

### package_master (14 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| package_id | varchar(20) | NULL | NO |
| store_id | bigint | 1 | NO |
| package_name | varchar(100) | NULL | NO |
| package_total_price | decimal(10,2) | 0.00 | YES |
| package_cost_price | decimal(10,2) | 0.00 | YES |
| cost_rate | decimal(5,2) | 0.00 | YES |
| dish_count | int | 0 | YES |
| suggest_guests | int | 10 | YES |
| occasion_type | varchar(20) | NULL | YES |
| package_series | varchar(20) | NULL | YES |
| is_active | tinyint | 1 | YES |
| sort_order | int | 0 | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### package_dish_detail (9 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| detail_id | bigint | NULL | NO |
| store_id | bigint | 1 | NO |
| package_id | varchar(20) | NULL | NO |
| dish_id | varchar(20) | NULL | NO |
| dish_quantity | int | 1 | YES |
| dish_order | int | 0 | YES |
| custom_name | varchar(100) | NULL | YES |
| note | varchar(255) | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |

### ingredient_master (18 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| ingredient_id | varchar(50) | NULL | NO |
| store_id | bigint | 1 | NO |
| ingredient_name | varchar(100) | NULL | NO |
| ingredient_category | varchar(50) | NULL | YES |
| brand | varchar(100) | NULL | YES |
| purchase_unit | varchar(20) | NULL | YES |
| usage_unit | varchar(20) | NULL | YES |
| conversion_rate | decimal(10,3) | 1.000 | YES |
| primary_supplier_id | int | NULL | YES |
| current_stock | decimal(12,3) | 0.000 | YES |
| warning_threshold | decimal(10,3) | 0.000 | YES |
| avg_price | decimal(10,4) | 0.0000 | YES |
| yield_rate | decimal(5,2) | 0.00 | YES |
| last_entry_date | date | NULL | YES |
| is_active | tinyint | 1 | YES |
| sort_order | int | 0 | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### supplier_master (17 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| supplier_id | int | NULL | NO |
| store_id | bigint | 1 | NO |
| supplier_code | varchar(20) | NULL | YES |
| supplier_name | varchar(100) | NULL | NO |
| contact_person | varchar(50) | NULL | YES |
| contact_phone | varchar(20) | NULL | YES |
| bank_account | varchar(50) | NULL | YES |
| platform_account | varchar(100) | NULL | YES |
| main_products | text | NULL | YES |
| wechat_account | varchar(50) | NULL | YES |
| alipay_account | varchar(50) | NULL | YES |
| taobao_account | varchar(50) | NULL | YES |
| supplier_rating | int | 5 | YES |
| is_active | tinyint | 1 | YES |
| remark | text | NULL | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |
| updated_at | timestamp | CURRENT_TIMESTAMP | YES |

### ingredient_inventory_log (10 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| log_id | bigint | NULL | NO |
| store_id | bigint | 1 | NO |
| ingredient_id | varchar(50) | NULL | NO |
| log_type | varchar(20) | NULL | NO |
| log_quantity | decimal(12,3) | NULL | NO |
| stock_after | decimal(12,3) | 0.000 | YES |
| log_time | timestamp | CURRENT_TIMESTAMP | YES |
| related_order_id | varchar(50) | NULL | YES |
| operator_id | int | NULL | YES |
| note | text | NULL | YES |

### ingredient_purchase (14 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| purchase_id | bigint | NULL | NO |
| store_id | bigint | 1 | NO |
| ingredient_id | varchar(50) | NULL | NO |
| supplier_id | int | NULL | YES |
| purchase_date | date | NULL | NO |
| purchase_quantity | decimal(12,3) | 0.000 | YES |
| purchase_price | decimal(10,2) | 0.00 | YES |
| purchase_total | decimal(12,2) | 0.00 | YES |
| usage_quantity | decimal(12,3) | 0.000 | YES |
| usage_price | decimal(10,4) | 0.0000 | YES |
| processing_note | text | NULL | YES |
| operator_id | int | NULL | YES |
| status | varchar(20) | completed | YES |
| created_at | timestamp | CURRENT_TIMESTAMP | YES |

### attendance_records (33 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| id | int | NULL | NO |
| staff_id | int | NULL | YES |
| record_id | varchar(50) | NULL | NO |
| emp_id | varchar(50) | NULL | NO |
| emp_name | varchar(50) | NULL | YES |
| department | varchar(50) | NULL | YES |
| month | varchar(7) | NULL | NO |
| scope | varchar(10) | full | NO |
| day_num | int | NULL | NO |
| am_type | varchar(20) | NULL | YES |
| pm_type | varchar(20) | NULL | YES |
| am_note | text | NULL | YES |
| pm_note | text | NULL | YES |
| day_note | text | NULL | YES |
| employment | varchar(20) | 全勤在职 | YES |
| salary_status | varchar(20) | 未发放 | YES |
| public_holiday | int | 6 | YES |
| carry_over | int | 0 | YES |
| summary_notes | text | NULL | YES |
| total_present | decimal(6,1) | 0.0 | YES |
| total_statutory | decimal(6,1) | 0.0 | YES |
| total_holiday | decimal(6,1) | 0.0 | YES |
| total_comp | decimal(6,1) | 0.0 | YES |
| total_travel | decimal(6,1) | 0.0 | YES |
| total_overtime | decimal(6,1) | 0.0 | YES |
| total_leave | decimal(6,1) | 0.0 | YES |
| total_late | decimal(6,1) | 0.0 | YES |
| total_early | decimal(6,1) | 0.0 | YES |
| total_absent | decimal(6,1) | 0.0 | YES |
| final_balance | decimal(6,1) | 0.0 | YES |
| recorded_days | int | 0 | YES |
| created_by | varchar(50) | Rino | YES |
| created_at | datetime | CURRENT_TIMESTAMP | YES |

### audit_log (9 cols)
| 字段 | 类型 | 默认值 | NULL |
|------|------|--------|------|
| log_id | bigint | NULL | NO |
| store_id | bigint | 1 | YES |
| username | varchar(50) | NULL | YES |
| action | varchar(100) | NULL | YES |
| module | varchar(50) | NULL | YES |
| target_id | varchar(50) | NULL | YES |
| detail | text | NULL | YES |
| ip_address | varchar(50) | NULL | YES |
| created_at | datetime | CURRENT_TIMESTAMP | YES |

