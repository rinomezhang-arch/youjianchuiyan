# -*- coding: utf-8 -*-
"""精准分析后端覆盖情况：基于 Controller→表前缀映射"""
import os
import re
import pymysql

# 数据库表
conn = pymysql.connect(host='localhost', port=3306, user='rino', password='Wo002323',
                       database='banquet', charset='utf8mb4',
                       cursorclass=pymysql.cursors.DictCursor)
with conn.cursor() as cur:
    cur.execute("SELECT table_name AS tname, table_comment AS tcomment FROM information_schema.tables WHERE table_schema='banquet' ORDER BY table_name")
    rows = cur.fetchall()
conn.close()
db_tables = {}
for r in rows:
    tname = r.get('tname') or list(r.values())[0]
    tcomment = r.get('tcomment') or list(r.values())[1] or ''
    db_tables[tname] = tcomment

# 后端实体
entity_dir = r'f:\solo\project\又见炊烟餐饮管理系统2.0\banquet_project\src\main\java\com\youjian\banquet\entity'
entities = {}
for fn in os.listdir(entity_dir):
    if not fn.endswith('.java'):
        continue
    path = os.path.join(entity_dir, fn)
    content = open(path, encoding='utf-8').read()
    m = re.search(r'@Table\s*\(\s*name\s*=\s*"([^"]+)"', content)
    if m:
        entities[m.group(1)] = fn

# Controller → 表前缀映射（基于实际代码语义）
controller_table_map = {
    'BookingController': ['booking_master', 'booking_table', 'booking_dish_detail'],
    'CustomerController': ['customer_master'],
    'StaffController': ['staff_master'],
    'HRController': ['department', 'leave_record', 'overtime', 'schedule_day', 'schedule_month', 'attendance_records', 'employee_lifecycle'],
    'AttendanceRecordController': ['attendance_records'],
    'ScheduleMonthController': ['schedule_month', 'schedule_day'],
    'DepartmentPostController': ['department'],
    'DishController': ['dish_master', 'dish_tag', 'dish_usage', 'dish_tag_relation', 'dish_usage_relation', 'dish_occasion_names'],
    'RecipeController': ['dish_recipe', 'dish_cost_card', 'dish_cost_card_detail'],
    'DishController+category': ['dish_category'],
    'TableController': ['table_master'],
    'TableBoardController': ['table_master'],
    'PackageController': ['package_master', 'package_dish_detail', 'package_details'],
    'SupplierController': ['supplier_master'],
    'IngredientController': ['ingredient_master', 'ingredient_inventory_log'],
    'PurchaseController': ['purchase_order', 'purchase_order_detail', 'purchase_receipt', 'purchase_receipt_detail', 'purchase_return', 'purchase_return_detail', 'procurement_request', 'procurement_request_item'],
    'InventoryController': ['inventory_summary'],
    'StockTransferController': ['stock_transfer', 'stock_transfer_detail'],
    'MemberController': ['member_card', 'member_level', 'member_recharge_record', 'member_consume_record', 'member_point_log', 'member_point_rule'],
    'ReportController': ['report_daily', 'report_monthly', 'report_dish_sales', 'report_staff_kpi', 'report_department_cost'],
    'MaintenanceController': ['maintenance_asset', 'maintenance_request'],
    'EngineeringController': ['engineering_work_order', 'engineering_inspection', 'engineering_spare_part', 'inspection_photos', 'decoration_project', 'floor_project', 'safety_issue', 'duty_record'],
    'MarketingController': ['marketing_activity', 'marketing_coupon', 'marketing_coupon_record', 'marketing_discount_rule', 'marketing_lottery', 'marketing_member_reward', 'marketing_promo_code'],
    'MarketingOverviewController': ['marketing_activity'],
    'ApprovalController': ['approval_flow', 'approval_node', 'approval_template', 'approval_log'],
    'FinanceController': ['finance_voucher', 'finance_voucher_detail', 'finance_transaction', 'finance_reconciliation', 'finance_settlement', 'finance_cost_record'],
    'FinanceAccountController': ['finance_account'],
    'FinanceExpenseController': ['finance_expense'],
    'FinancePayableController': ['finance_payable'],
    'FinanceReportController': ['finance_report'],
    'FinancePaymentController-virtual': ['finance_payment_record'],
    'FinanceReceivableController-virtual': ['finance_receivable'],
    'KitchenController': ['kitchen_log'],
    'KitchenSupplyController': ['kitchen_supply'],
    'AIController': ['ai_chat_history', 'ai_memory'],
    'ChatController': ['ai_chat_history'],
    'AuthController': ['sys_user', 'admin_users'],
    'DictController': ['sys_dict', 'sys_dict_item'],
    'EnergyController': ['energy_record'],
    'ContractController': ['contract'],
    'RewardPunishController': ['reward_punish'],
    'PayrollController': ['payroll'],
    'SalaryController': ['salary_record'],
    'UploadController': ['attachment'],
    'IpadDishController': [],  # 视图接口
    'IpadOrderController': [],  # 视图接口
    'IpadTableController': [],  # 视图接口
    'DashboardController': [],  # 仪表盘聚合
}

# 收集所有被Controller覆盖的表
covered_by_controller = set()
for ctrl, tables in controller_table_map.items():
    for t in tables:
        covered_by_controller.add(t)

# 分析
print('=' * 100)
print('后端覆盖精准分析')
print('=' * 100)
print(f'数据库表总数: {len(db_tables)}')
print(f'后端实体数: {len(entities)}')
print(f'Controller覆盖的表: {len(covered_by_controller)}')
print()

# 1. 数据库表分类
all_db_table_names = set(db_tables.keys())
uncovered_tables = sorted(all_db_table_names - covered_by_controller)

# 2. 真正缺失的表（按模块分组）
print('=' * 100)
print('一、Controller 未覆盖的表（按模块）')
print('=' * 100)

modules = {
    '工具管理 (db_fix_batch2新建,完全缺失)': ['tool_'],
    '库存盘点 (stock_take/stock_loss)': ['stock_take', 'stock_loss'],
    '宴会模板/类型': ['banquet_template', 'banquet_type'],
    '操作日志': ['change_log', 'audit_logs'],
    '会员相关': ['member_'],
    '报表 (无Controller,可能是统计查询)': ['report_'],
    '营销相关': ['marketing_'],
    '财务相关': ['finance_'],
    '审批流': ['approval_'],
    '工程相关': ['engineering_', 'inspection_photos', 'decoration_', 'floor_', 'safety_', 'duty_'],
    '采购相关': ['purchase_', 'procurement_'],
    'AI/聊天': ['ai_', 'chat_'],
    '系统表': ['sys_', 'admin_'],
}

shown = set()
for mod_name, prefixes in modules.items():
    matched = []
    for t in uncovered_tables:
        if t in shown:
            continue
        if any(t.startswith(p) or t == p for p in prefixes):
            matched.append(t)
    if matched:
        print(f'\n【{mod_name}】')
        for t in matched:
            has_entity = '✓' if t in entities else '✗'
            print(f'  [{has_entity}实体] {t:40s} {db_tables.get(t, "")}')
            shown.add(t)

# 未归类的
uncategorized = [t for t in uncovered_tables if t not in shown]
if uncategorized:
    print(f'\n【未归类/其他】')
    for t in uncategorized:
        has_entity = '✓' if t in entities else '✗'
        print(f'  [{has_entity}实体] {t:40s} {db_tables.get(t, "")}')

# 3. 汇总
print()
print('=' * 100)
print('汇总')
print('=' * 100)
print(f'业务表总数: {len(db_tables)}')
print(f'Controller 覆盖: {len(covered_by_controller & all_db_table_names)}')
print(f'Controller 未覆盖: {len(uncovered_tables)}')
print()
print('缺失严重度评估:')
print('  P0 (核心业务阻塞): 工具管理7表(新建) + 库存盘点4表 + 报销')
print('  P1 (业务模块缺失): 宴会模板/类型 + 操作日志 + 报表(统计)')
print('  P2 (边缘表): 其他')
