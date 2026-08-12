#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
外键补全脚本 v1.0
基于威龙的审计报告，补全147个缺失外键
策略：
1. 检查类型匹配（不匹配的跳过并报告）
2. 按模块分批执行，遇错不中断
3. 已存在的物理外键跳过
"""
import os
import sys
import subprocess
import datetime

DB = {
    'host': 'localhost', 'port': '3306', 'user': 'rino',
    'password': 'Wo002323', 'database': 'banquet',
}
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPORT_DIR = r'f:\solo\project\又见炊烟餐饮管理系统2.0\scripts\migrations'
os.makedirs(REPORT_DIR, exist_ok=True)
TIMESTAMP = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
OUT_SQL = os.path.join(REPORT_DIR, f'fk_add_{TIMESTAMP}.sql')
LOG_FILE = os.path.join(REPORT_DIR, f'fk_add_{TIMESTAMP}.log')


def mysql_query(sql, silent=False):
    cmd = ['mysql', f'-h{DB["host"]}', f'-P{DB["port"]}', f'-u{DB["user"]}', f'-p{DB["password"]}',
           '--default-character-set=utf8mb4', '-N', '-B', DB['database'], '-e', sql]
    try:
        r = subprocess.run(cmd, capture_output=True, text=False, timeout=30)
        if r.returncode != 0:
            if not silent:
                print(f"  SQL错误: {r.stderr.decode('utf-8','ignore')[:200]}")
            return []
        return [line for line in r.stdout.decode('utf-8','ignore').strip().split('\n') if line.strip()]
    except Exception as e:
        if not silent:
            print(f"  SQL异常: {e}")
        return []


def get_existing_fks():
    """获取已存在的物理外键: {(table, column): constraint_name}"""
    sql = ("SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
           "WHERE TABLE_SCHEMA='banquet' AND REFERENCED_TABLE_NAME IS NOT NULL")
    rows = mysql_query(sql, silent=True)
    result = {}
    for r in rows:
        p = r.split('\t')
        if len(p) >= 3:
            result[(p[0].strip(), p[1].strip())] = p[2].strip()
    return result


def get_column_type(table, column):
    """获取列类型(简化形式: int/bigint/varchar/datetime等)"""
    sql = f"SELECT DATA_TYPE, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='{table}' AND COLUMN_NAME='{column}'"
    rows = mysql_query(sql, silent=True)
    if not rows:
        return None, None
    p = rows[0].split('\t')
    return p[0].strip().lower(), p[1].strip().lower() if len(p) > 1 else None


# ============================== 外键清单（来自威龙审计报告） ==============================
# 格式: (从表, 从列, 主表, 主列, 模块, 说明)
FK_LIST = [
    # ===== 模块一: 采购管理 (~19个) =====
    ('purchase_order', 'supplier_id', 'supplier_master', 'supplier_id', '采购', '采购单→供应商'),
    ('purchase_order', 'purchaser_id', 'staff_master', 'staff_id', '采购', '采购员→员工'),
    ('purchase_order', 'approver_id', 'staff_master', 'staff_id', '采购', '审批人→员工'),
    ('purchase_order', 'warehouse_keeper_id', 'staff_master', 'staff_id', '采购', '仓管→员工'),
    ('purchase_order_detail', 'order_id', 'purchase_order', 'order_id', '采购', '明细→主单'),
    ('purchase_order_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '采购', '明细→食材'),
    ('purchase_receipt', 'order_id', 'purchase_order', 'order_id', '采购', '入库→采购单'),
    ('purchase_receipt', 'supplier_id', 'supplier_master', 'supplier_id', '采购', '入库→供应商'),
    ('purchase_receipt', 'warehouse_keeper_id', 'staff_master', 'staff_id', '采购', '仓管→员工'),
    ('purchase_receipt_detail', 'receipt_id', 'purchase_receipt', 'receipt_id', '采购', '明细→主单'),
    ('purchase_receipt_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '采购', '明细→食材'),
    ('purchase_return', 'receipt_id', 'purchase_receipt', 'receipt_id', '采购', '退货→入库单'),
    ('purchase_return', 'order_id', 'purchase_order', 'order_id', '采购', '退货→采购单'),
    ('purchase_return', 'supplier_id', 'supplier_master', 'supplier_id', '采购', '退货→供应商'),
    ('purchase_return_detail', 'return_id', 'purchase_return', 'return_id', '采购', '明细→主单'),
    ('purchase_return_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '采购', '明细→食材'),
    ('procurement_request', 'department_id', 'department', 'dept_id', '采购', '申请→部门'),
    ('procurement_request', 'requester_id', 'staff_master', 'staff_id', '采购', '申请人→员工'),
    ('procurement_request', 'approver_id', 'staff_master', 'staff_id', '采购', '审批人→员工'),

    # ===== 模块二: 库存管理 (~19个) =====
    ('ingredient_master', 'primary_supplier_id', 'supplier_master', 'supplier_id', '库存', '主供应商'),
    ('ingredient_master', 'supplier_id', 'supplier_master', 'supplier_id', '库存', '当前供应商'),
    ('ingredient_inventory_log', 'ingredient_id', 'ingredient_master', 'ingredient_id', '库存', '变动→食材'),
    ('ingredient_inventory_log', 'operator_id', 'staff_master', 'staff_id', '库存', '操作人→员工'),
    ('stock_take', 'operator_id', 'staff_master', 'staff_id', '库存', '盘点人→员工'),
    ('stock_take', 'supervisor_id', 'staff_master', 'staff_id', '库存', '监盘人→员工'),
    ('stock_take_detail', 'take_id', 'stock_take', 'take_id', '库存', '明细→主单'),
    ('stock_take_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '库存', '明细→食材'),
    ('stock_loss', 'applicant_id', 'staff_master', 'staff_id', '库存', '申请人→员工'),
    ('stock_loss', 'approver_id', 'staff_master', 'staff_id', '库存', '审批人→员工'),
    ('stock_loss', 'warehouse_keeper_id', 'staff_master', 'staff_id', '库存', '仓管→员工'),
    ('stock_loss_detail', 'loss_id', 'stock_loss', 'loss_id', '库存', '明细→主单'),
    ('stock_loss_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '库存', '明细→食材'),
    ('requisition_order', 'department_id', 'department', 'dept_id', '库存', '领料→部门'),
    ('requisition_order', 'requester_id', 'staff_master', 'staff_id', '库存', '申请人→员工'),
    ('requisition_order', 'approver_id', 'staff_master', 'staff_id', '库存', '审批人→员工'),
    ('requisition_order', 'warehouse_keeper_id', 'staff_master', 'staff_id', '库存', '仓管→员工'),
    ('requisition_detail', 'requisition_id', 'requisition_order', 'requisition_id', '库存', '明细→主单'),
    ('requisition_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '库存', '明细→食材'),

    # ===== 模块三: 成本/配方 (~8个) =====
    ('dish_cost_card', 'dish_id', 'dish_master', 'dish_id', '成本', '成本卡→菜品'),
    ('dish_cost_card_detail', 'cost_card_id', 'dish_cost_card', 'cost_card_id', '成本', '明细→主单'),
    ('dish_cost_card_detail', 'ingredient_id', 'ingredient_master', 'ingredient_id', '成本', '明细→食材'),
    ('dish_recipe', 'dish_id', 'dish_master', 'dish_id', '成本', '配方→菜品'),
    ('dish_recipe', 'ingredient_id', 'ingredient_master', 'ingredient_id', '成本', '配方→食材'),
    ('yield_rate_config', 'ingredient_id', 'ingredient_master', 'ingredient_id', '成本', '出成率→食材'),
    ('dish_occasion_names', 'dish_id', 'dish_master', 'dish_id', '成本', '场景名→菜品'),
    ('dish_master', 'category_id', 'dish_category', 'id', '成本', '菜品→分类'),

    # ===== 模块四: 财务管理 (~23个) =====
    ('finance_voucher_detail', 'voucher_id', 'finance_voucher', 'voucher_id', '财务', '明细→凭证'),
    ('finance_transaction', 'account_id', 'finance_account', 'account_id', '财务', '流水→账户'),
    ('finance_transaction', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),
    ('finance_payable', 'supplier_id', 'supplier_master', 'supplier_id', '财务', '应付→供应商'),
    ('finance_payable', 'purchase_id', 'purchase_order', 'order_id', '财务', '应付→采购单'),
    ('finance_payable', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),
    ('finance_receivable', 'customer_id', 'customer_master', 'customer_id', '财务', '应收→客户'),
    ('finance_receivable', 'booking_id', 'booking_master', 'booking_id', '财务', '应收→预订'),
    ('finance_receivable', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),
    ('finance_payment_record', 'receivable_id', 'finance_receivable', 'receivable_id', '财务', '收款→应收'),
    ('finance_payment_record', 'customer_id', 'customer_master', 'customer_id', '财务', '收款→客户'),
    ('finance_payment_record', 'booking_id', 'booking_master', 'booking_id', '财务', '收款→预订'),
    ('finance_payment_record', 'account_id', 'finance_account', 'account_id', '财务', '收款→账户'),
    ('finance_payment_record', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),
    ('finance_expense', 'applicant_id', 'staff_master', 'staff_id', '财务', '申请人→员工'),
    ('finance_expense', 'department_id', 'department', 'dept_id', '财务', '部门→部门'),
    ('finance_expense', 'approver_id', 'staff_master', 'staff_id', '财务', '审批人→员工'),
    ('finance_expense', 'account_id', 'finance_account', 'account_id', '财务', '费用→账户'),
    ('finance_cost_record', 'department_id', 'department', 'dept_id', '财务', '成本→部门'),
    ('finance_cost_record', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),
    ('finance_settlement', 'operator_id', 'staff_master', 'staff_id', '财务', '结算→员工'),
    ('finance_reconciliation', 'account_id', 'finance_account', 'account_id', '财务', '对账→账户'),
    ('finance_reconciliation', 'operator_id', 'staff_master', 'staff_id', '财务', '操作人→员工'),

    # ===== 模块五: 预订管理 (~7个) =====
    ('booking_dish_detail', 'booking_id', 'booking_master', 'booking_id', '预订', '菜品→预订'),
    ('booking_dish_detail', 'dish_id', 'dish_master', 'dish_id', '预订', '菜品→菜品'),
    ('booking_master', 'package_id', 'meal_package', 'id', '预订', '预订→套餐'),
    ('kitchen_log', 'booking_id', 'booking_master', 'booking_id', '预订', '出菜→预订'),
    ('kitchen_log', 'dish_id', 'dish_master', 'dish_id', '预订', '出菜→菜品'),
    ('kitchen_log', 'operator_id', 'staff_master', 'staff_id', '预订', '操作人→员工'),

    # ===== 模块六: 会员营销 (~14个) =====
    ('member_card', 'level_id', 'member_level', 'level_id', '会员', '会员→等级'),
    ('member_card', 'referrer_id', 'member_card', 'member_id', '会员', '推荐人→会员'),
    ('member_point_log', 'member_id', 'member_card', 'member_id', '会员', '积分→会员'),
    ('member_point_log', 'operator_id', 'staff_master', 'staff_id', '会员', '操作人→员工'),
    ('member_consume_record', 'member_id', 'member_card', 'member_id', '会员', '消费→会员'),
    ('member_consume_record', 'booking_id', 'booking_master', 'booking_id', '会员', '消费→预订'),
    ('member_consume_record', 'operator_id', 'staff_master', 'staff_id', '会员', '操作人→员工'),
    ('member_recharge_record', 'member_id', 'member_card', 'member_id', '会员', '充值→会员'),
    ('member_recharge_record', 'activity_id', 'marketing_activity', 'activity_id', '会员', '充值→活动'),
    ('member_recharge_record', 'operator_id', 'staff_master', 'staff_id', '会员', '操作人→员工'),
    ('marketing_coupon_record', 'coupon_id', 'marketing_coupon', 'coupon_id', '会员', '券记录→优惠券'),
    ('marketing_coupon_record', 'member_id', 'member_card', 'member_id', '会员', '券记录→会员'),
    ('marketing_coupon_record', 'booking_id', 'booking_master', 'booking_id', '会员', '券记录→预订'),
    ('marketing_member_reward', 'reward_coupon_id', 'marketing_coupon', 'coupon_id', '会员', '奖励→优惠券'),

    # ===== 模块七: 员工管理 (~10个) =====
    ('attendance', 'staff_id', 'staff_master', 'staff_id', '员工', '考勤→员工'),
    ('attendance_records', 'staff_id', 'staff_master', 'staff_id', '员工', '考勤→员工'),
    ('leave_record', 'staff_id', 'staff_master', 'staff_id', '员工', '请假→员工'),
    ('leave_record', 'approver_id', 'staff_master', 'staff_id', '员工', '审批人→员工'),
    ('overtime', 'staff_id', 'staff_master', 'staff_id', '员工', '加班→员工'),
    ('overtime', 'approver_id', 'staff_master', 'staff_id', '员工', '审批人→员工'),
    ('employee_lifecycle', 'staff_id', 'staff_master', 'staff_id', '员工', '生命周期→员工'),
    ('schedule', 'staff_id', 'staff_master', 'staff_id', '员工', '排班→员工'),
    ('staff_master', 'leader_id', 'staff_master', 'staff_id', '员工', '上级→员工(自引用)'),
    ('ai_chat_history', 'staff_id', 'staff_master', 'staff_id', '员工', '对话→员工'),

    # ===== 模块八: 审批流程 (~5个) =====
    ('approval_flow', 'applicant_id', 'staff_master', 'staff_id', '审批', '申请人→员工'),
    ('approval_node', 'flow_id', 'approval_flow', 'id', '审批', '节点→流程'),
    ('approval_node', 'approver_id', 'staff_master', 'staff_id', '审批', '审批人→员工'),
    ('approval_log', 'flow_id', 'approval_flow', 'id', '审批', '日志→流程'),
    ('approval_log', 'approver_id', 'staff_master', 'staff_id', '审批', '审批人→员工'),

    # ===== 模块九: 报表统计 (~5个) =====
    ('report_daily', 'operator_id', 'staff_master', 'staff_id', '报表', '日报→员工'),
    ('report_monthly', 'operator_id', 'staff_master', 'staff_id', '报表', '月报→员工'),
    ('report_staff_kpi', 'staff_id', 'staff_master', 'staff_id', '报表', 'KPI→员工'),
    ('report_department_cost', 'department_id', 'department', 'dept_id', '报表', '成本→部门'),
    ('report_dish_sales', 'dish_id', 'dish_master', 'dish_id', '报表', '销售→菜品'),

    # ===== 模块十: 系统管理 (~6个) =====
    ('sys_operation_log', 'operator_id', 'staff_master', 'staff_id', '系统', '日志→员工'),
    ('sys_notification', 'sender_id', 'staff_master', 'staff_id', '系统', '通知→员工'),
    ('sys_dict_item', 'dict_id', 'sys_dict', 'dict_id', '系统', '字典项→字典'),
    ('sys_dict_item', 'parent_id', 'sys_dict_item', 'item_id', '系统', '父级→字典项(自引用)'),
    ('store_info', 'manager_id', 'staff_master', 'staff_id', '系统', '店长→员工'),
    ('change_log', 'operator_id', 'staff_master', 'staff_id', '系统', '改动→员工'),

    # ===== 模块十一: 套餐菜品 (~3个) =====
    ('package_dish_detail', 'package_id', 'meal_package', 'id', '套餐', '明细→套餐'),
    ('package_dish_detail', 'dish_id', 'dish_master', 'dish_id', '套餐', '明细→菜品'),
    ('template_dish_rel', 'dish_id', 'dish_master', 'dish_id', '套餐', '模板→菜品'),

    # ===== 模块十二: 报销 (~4个) =====
    ('reimbursement', 'applicant_id', 'staff_master', 'staff_id', '报销', '申请人→员工'),
    ('reimbursement', 'department_id', 'department', 'dept_id', '报销', '部门→部门'),
    ('reimbursement', 'approver_id', 'staff_master', 'staff_id', '报销', '审批人→员工'),
    ('reimbursement', 'finance_approver_id', 'staff_master', 'staff_id', '报销', '财务审批→员工'),

    # ===== 模块十三: 工程管理 (~16个, 含新建表) =====
    ('maintenance_asset', 'handler_id', 'staff_master', 'staff_id', '工程', '资产负责人'),
    ('maintenance_request', 'asset_id', 'maintenance_asset', 'asset_id', '工程', '报修→资产'),
    ('maintenance_request', 'reporter_id', 'staff_master', 'staff_id', '工程', '报修人'),
    ('maintenance_request', 'handler_id', 'staff_master', 'staff_id', '工程', '维修人'),
    ('engineering_work_order', 'assignee_id', 'staff_master', 'staff_id', '工程', '工单指派人'),
    ('engineering_inspection', 'inspector_id', 'staff_master', 'staff_id', '工程', '巡检人'),
    ('inspection_photos', 'inspection_id', 'engineering_inspection', 'inspection_id', '工程', '照片→巡检记录'),
    ('decoration_project', 'manager_id', 'staff_master', 'staff_id', '工程', '项目负责人'),
    ('floor_project', 'manager_id', 'staff_master', 'staff_id', '工程', '项目负责人'),
    ('safety_issue', 'reporter_id', 'staff_master', 'staff_id', '工程', '报告人'),
    ('safety_issue', 'handler_id', 'staff_master', 'staff_id', '工程', '处理人'),
    ('duty_record', 'staff_id', 'staff_master', 'staff_id', '工程', '值班人'),
    ('attachment', 'upload_by', 'staff_master', 'staff_id', '工程', '上传人'),

    # ===== 模块十四: 工具资产 (~8个) =====
    ('tool_master', 'category_id', 'tool_category', 'category_id', '工具', '工具→分类'),
    ('tool_issue', 'staff_id', 'staff_master', 'staff_id', '工具', '领用人→员工'),
    ('tool_issue', 'tool_id', 'tool_master', 'tool_id', '工具', '领用→工具'),
    ('tool_return', 'issue_id', 'tool_issue', 'issue_id', '工具', '归还→领用'),
    ('tool_return', 'tool_id', 'tool_master', 'tool_id', '工具', '归还→工具'),
    ('tool_return', 'staff_id', 'staff_master', 'staff_id', '工具', '归还人→员工'),
    ('tool_damage', 'tool_id', 'tool_master', 'tool_id', '工具', '损坏→工具'),
    ('tool_damage', 'staff_id', 'staff_master', 'staff_id', '工具', '责任人→员工'),
    ('tool_inventory', 'staff_id', 'staff_master', 'staff_id', '工具', '盘点人→员工'),
]


def main():
    print("=" * 70)
    print("  外键补全脚本 v1.0")
    print("=" * 70)
    print(f"  待处理外键: {len(FK_LIST)} 个")
    print()

    existing_fks = get_existing_fks()
    print(f"  数据库已存在物理外键: {len(existing_fks)} 个")
    print(f"  注: 重复执行会跳过已创建的外键\n")

    sql_lines = [
        "-- ================================================================",
        f"-- 外键补全SQL - {TIMESTAMP}",
        "-- ================================================================",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS=0;",
        "",
    ]

    stats = {
        'added': 0,
        'skipped_exists': 0,
        'skipped_type_mismatch': 0,
        'skipped_table_missing': 0,
        'skipped_col_missing': 0,
    }
    log_lines = []
    type_mismatch_log = []
    missing_log = []

    # 按模块分组处理
    modules = {}
    for fk in FK_LIST:
        modules.setdefault(fk[4], []).append(fk)

    for module_name, fks in modules.items():
        sql_lines.append(f"-- =====================================================")
        sql_lines.append(f"-- 模块: {module_name} ({len(fks)}个)")
        sql_lines.append(f"-- =====================================================")
        print(f"\n[{module_name}] 处理 {len(fks)} 个外键...")
        for fk in fks:
            from_t, from_c, to_t, to_c, mod, desc = fk

            # 跳过已存在
            if (from_t, from_c) in existing_fks:
                stats['skipped_exists'] += 1
                log_lines.append(f"[跳过-已存在] {from_t}.{from_c} → {to_t}.{to_c} ({desc})")
                continue

            # 检查列存在性
            from_type_simple, from_type_full = get_column_type(from_t, from_c)
            to_type_simple, to_type_full = get_column_type(to_t, to_c)

            if not from_type_simple:
                stats['skipped_col_missing'] += 1
                missing_log.append(f"[从列缺失] {from_t}.{from_c} → {to_t}.{to_c} ({desc})")
                log_lines.append(f"[跳过-从列缺失] {from_t}.{from_c}")
                continue
            if not to_type_simple:
                stats['skipped_col_missing'] += 1
                missing_log.append(f"[主列缺失] {from_t}.{from_c} → {to_t}.{to_c} ({desc})")
                log_lines.append(f"[跳过-主列缺失] {to_t}.{to_c}")
                continue

            # 类型检查: bigint/int 视为兼容
            compatible = False
            if from_type_simple == to_type_simple:
                compatible = True
            elif from_type_simple in ('int', 'bigint', 'smallint', 'tinyint') and \
                 to_type_simple in ('int', 'bigint', 'smallint', 'tinyint'):
                compatible = True  # 整数家族相互兼容
            elif from_type_simple in ('varchar', 'char', 'text') and \
                 to_type_simple in ('varchar', 'char', 'text'):
                compatible = True  # 字符串家族相互兼容

            if not compatible:
                stats['skipped_type_mismatch'] += 1
                type_mismatch_log.append(
                    f"[类型不匹配] {from_t}.{from_c}({from_type_full}) → {to_t}.{to_c}({to_type_full}) ({desc})")
                log_lines.append(f"[跳过-类型不匹配] {from_t}.{from_c}({from_type_full}) vs {to_t}.{to_c}({to_type_full})")
                continue

            # 生成ADD CONSTRAINT语句
            constraint_name = f"fk_{from_t}_{from_c}"
            # 约束名长度限制 64
            if len(constraint_name) > 64:
                constraint_name = constraint_name[:64]
            sql = (f"ALTER TABLE `{from_t}` ADD CONSTRAINT `{constraint_name}` "
                   f"FOREIGN KEY (`{from_c}`) REFERENCES `{to_t}`(`{to_c}`) "
                   f"ON DELETE RESTRICT ON UPDATE CASCADE;")
            sql_lines.append(sql)
            stats['added'] += 1
            log_lines.append(f"[新增] {from_t}.{from_c} → {to_t}.{to_c} ({desc})")
            print(f"  [新增] {from_t}.{from_c} → {to_t}.{to_c}")

        sql_lines.append("")

    sql_lines.append("SET FOREIGN_KEY_CHECKS=1;")
    sql_lines.append("-- ================================================================")
    sql_lines.append(f"-- 共生成 {stats['added']} 条ADD CONSTRAINT语句")

    # 写入SQL文件
    with open(OUT_SQL, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_lines))

    # 写入日志
    with open(LOG_FILE, 'w', encoding='utf-8') as f:
        f.write("=" * 70 + "\n")
        f.write("外键补全脚本日志\n")
        f.write("=" * 70 + "\n")
        f.write(f"时间: {TIMESTAMP}\n\n")
        f.write("统计:\n")
        f.write(f"  新增: {stats['added']}\n")
        f.write(f"  跳过-已存在: {stats['skipped_exists']}\n")
        f.write(f"  跳过-类型不匹配: {stats['skipped_type_mismatch']}\n")
        f.write(f"  跳过-列缺失: {stats['skipped_col_missing']}\n\n")
        f.write("详细日志:\n")
        f.write('\n'.join(log_lines))
        if type_mismatch_log:
            f.write("\n\n=== 类型不匹配清单 ===\n")
            f.write('\n'.join(type_mismatch_log))
        if missing_log:
            f.write("\n\n=== 列缺失清单 ===\n")
            f.write('\n'.join(missing_log))

    print("\n" + "=" * 70)
    print(f"  统计:")
    print(f"    新增外键: {stats['added']}")
    print(f"    跳过-已存在: {stats['skipped_exists']}")
    print(f"    跳过-类型不匹配: {stats['skipped_type_mismatch']}")
    print(f"    跳过-列缺失: {stats['skipped_col_missing']}")
    print(f"\n  SQL文件: {OUT_SQL}")
    print(f"  日志文件: {LOG_FILE}")
    print(f"\n  执行命令:")
    print(f"    Get-Content -Path \"{OUT_SQL}\" -Encoding UTF8 | mysql -h{DB['host']} -P{DB['port']} -u{DB['user']} -p{DB['password']} --default-character-set=utf8mb4 {DB['database']}")
    print("=" * 70)


if __name__ == '__main__':
    main()
