#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析引擎模块
执行数据库结构分析，生成发现项
"""
import logging
from typing import List, Dict, Tuple
from dataclasses import dataclass
from collections import Counter
from db import Database, TableInfo, ColumnInfo, ForeignKeyInfo

logger = logging.getLogger("checkup.analyzer")


@dataclass
class Finding:
    level: str  # FATAL, ERROR, WARNING, INFO
    module: str
    title: str
    expect: str
    actual: str
    detail: str = ''
    fix: str = ''


class Analyzer:
    """数据库分析引擎"""
    
    def __init__(self, db: Database):
        self.db = db
        self.findings: List[Finding] = []
        self.counters = {'FATAL': 0, 'ERROR': 0, 'WARNING': 0, 'INFO': 0}
    
    def add(self, level: str, module: str, title: str, expect: str, actual: str, detail: str = '', fix: str = '') -> None:
        """添加发现项"""
        self.findings.append(Finding(level, module, title, expect, actual, detail, fix))
        self.counters[level] = self.counters.get(level, 0) + 1
    
    def analyze(self) -> Tuple[List[Finding], Dict]:
        """执行完整分析"""
        tables = self.db.get_tables()
        if not tables:
            logger.error("数据库连接失败或无表")
            return [], {}
        
        logger.info(f"开始分析 {len(tables)} 张表")
        
        # 1. 主键分析
        self._analyze_primary_keys(tables)
        
        # 2. 外键分析（真实约束 + 推断）
        self._analyze_foreign_keys(tables)
        
        # 3. 索引分析
        self._analyze_indexes(tables)
        
        # 4. 空表检测
        self._analyze_empty_tables(tables)
        
        # 5. 字符集一致性
        self._analyze_collation(tables)
        
        # 6. 引擎检查
        self._analyze_engine(tables)
        
        # 7. 字段冗余
        self._analyze_field_redundancy(tables)
        
        # 8. 多门店隔离
        self._analyze_store_isolation(tables)
        
        # 9. 注释完整性
        self._analyze_comments(tables)
        
        summary = {
            'tables': len(tables),
            'empty_tables': sum(1 for t in tables.values() if t.rows == 0),
            'total_findings': len(self.findings),
            'fatal': self.counters['FATAL'],
            'error': self.counters['ERROR'],
            'warning': self.counters['WARNING'],
            'info': self.counters['INFO'],
        }
        
        logger.info(f"分析完成: {summary}")
        return self.findings, summary
    
    def _analyze_primary_keys(self, tables: Dict[str, TableInfo]) -> None:
        """主键分析"""
        for table_name, table_info in tables.items():
            pk_cols = self.db.get_pk_columns(table_name)
            
            if not pk_cols:
                self.add('FATAL', '主键', f'表无主键: {table_name}',
                        '每张表应有PRIMARY KEY', f'{table_name}无主键',
                        '无主键无法唯一标识行，同步/恢复/去重全部失效',
                        f'ALTER TABLE `{table_name}` ADD PRIMARY KEY (id);')
                continue
            
            cols = self.db.get_columns(table_name)
            pk_col = next((c for c in cols if c.name == pk_cols[0]), None)
            if not pk_col:
                continue
            
            pk_type = pk_col.type.lower().split('(')[0]
            
            if pk_type.startswith('varchar') or pk_type.startswith('char'):
                self.add('WARNING', '主键', f'varchar主键: {table_name}.{pk_col.name}({pk_col.type})',
                        '主键应为bigint自增', f'varchar({pk_col.type})',
                        '字符串主键：并发冲突、索引碎片化、排序效率低',
                        f'ALTER TABLE `{table_name}` ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY;')
            elif pk_type == 'int' and 'bigint' not in pk_col.type:
                self.add('WARNING', '主键', f'int主键: {table_name}.{pk_col.name}({pk_col.type})',
                        '应使用bigint(上限922亿亿)', f'int(上限21亿)',
                        '数据量大后可能耗尽',
                        f'ALTER TABLE `{table_name}` MODIFY `{pk_col.name}` BIGINT AUTO_INCREMENT;')
    
    def _analyze_foreign_keys(self, tables: Dict[str, TableInfo]) -> None:
        """外键分析：真实约束 + _id后缀推断"""
        # 获取真实外键约束
        real_fks = self.db.get_foreign_keys()
        real_fk_set = {(fk.table, fk.column) for fk in real_fks}
        
        # 分析真实外键
        for fk in real_fks:
            if fk.ref_table not in tables:
                self.add('ERROR', '外键', f'外键引用不存在的表: {fk.table}.{fk.column} → {fk.ref_table}',
                        f'引用表应存在', f'{fk.ref_table}不存在',
                        '外键约束指向不存在的表',
                        f'创建表 `{fk.ref_table}` 或删除外键约束')
                continue
            
            # 检查类型匹配
            src_cols = self.db.get_columns(fk.table)
            src_col = next((c for c in src_cols if c.name == fk.column), None)
            ref_pks = self.db.get_pk_columns(fk.ref_table)
            
            if src_col and ref_pks:
                ref_cols = self.db.get_columns(fk.ref_table)
                ref_pk_col = next((c for c in ref_cols if c.name == ref_pks[0]), None)
                
                if ref_pk_col:
                    src_type = src_col.type.lower().split('(')[0]
                    ref_type = ref_pk_col.type.lower().split('(')[0]
                    
                    if src_type != ref_type:
                        level = 'FATAL' if src_col.nullable == 'NO' else 'ERROR'
                        self.add(level, '外键', f'外键类型不匹配: {fk.table}.{fk.column}({src_type}) → {fk.ref_table}.{ref_pk_col.name}({ref_type})',
                                f'FK类型应与PK一致: {ref_type}', f'FK={src_type} PK={ref_type}',
                                'JOIN全表扫描或类型转换失败',
                                f'ALTER TABLE `{fk.table}` MODIFY `{fk.column}` {ref_pk_col.type};')
            
            # 检查孤儿记录
            orphan_count = self.db.count_orphans(fk.table, fk.column, fk.ref_table, fk.ref_column)
            if orphan_count > 0:
                self.add('ERROR', '孤儿记录', f'外键孤儿: {fk.table}.{fk.column} → {fk.ref_table}.{fk.ref_column} ({orphan_count}条)',
                        f'{fk.table}.{fk.column} 应全部存在于 {fk.ref_table}.{fk.ref_column}',
                        f'{orphan_count}条孤儿记录',
                        f'数据完整性破坏',
                        f'运行 fix_orphans.py 清理, 或手动 UPDATE/DELETE')
        
        # 推断潜在外键（_id后缀列）
        ref_map = {
            'store': 'store_info', 'staff': 'staff_master', 'customer': 'customer_master',
            'supplier': 'supplier_master', 'dish': 'dish_master', 'ingredient': 'ingredient_master',
            'booking': 'booking_master', 'table_': 'table_master', 'package': 'package_master',
            'account': 'finance_account', 'purchase': 'purchase_order', 'dept': 'department',
            'member': 'member_card', 'voucher': 'finance_voucher', 'level': 'member_level',
            'category': 'dish_category', 'requisition': 'requisition_order',
            'menu': 'sys_menu', 'role': 'sys_role', 'permission': 'sys_permission',
            'dict': 'sys_dict', 'notification': 'sys_notification',
            'cost_card': 'dish_cost_card', 'cost': 'finance_cost_record',
            'expense': 'finance_expense', 'payable': 'finance_payable',
            'payment': 'finance_payment_record', 'receivable': 'finance_receivable',
            'recon': 'finance_reconciliation', 'settlement': 'finance_settlement',
            'trans': 'finance_transaction', 'salary': 'month_salary',
            'template': 'banquet_template', 'return': 'purchase_return',
            'receipt': 'purchase_receipt', 'request': 'procurement_request',
            'loss': 'stock_loss', 'take': 'stock_take', 'transfer': 'stock_transfer',
            'leave': 'leave_record', 'reward': 'reward_punish',
            'activity': 'marketing_activity', 'coupon': 'marketing_coupon',
            'recharge': 'member_recharge_record', 'consume': 'member_consume_record',
            'log': 'sys_operation_log', 'report': 'report_daily',
            'recipe': 'dish_recipe', 'conversion': 'unit_conversion',
            'schedule': 'schedule', 'day': 'schedule_day',
        }
        
        all_pk_cols = set()
        for t_name in tables:
            for pk in self.db.get_pk_columns(t_name):
                all_pk_cols.add(f"{t_name}.{pk}")
        
        inferred_count = 0
        for table_name, table_info in tables.items():
            cols = self.db.get_columns(table_name)
            for col in cols:
                if not col.name.endswith('_id') or col.name == 'id':
                    continue
                if f"{table_name}.{col.name}" in all_pk_cols:
                    continue  # 自身主键
                
                ref_table = col.name[:-3]
                for k, v in ref_map.items():
                    if ref_table == k:
                        ref_table = v
                        break
                
                if ref_table == table_name:
                    continue  # 自引用
                
                # 如果已有真实外键约束，跳过
                if (table_name, col.name) in real_fk_set:
                    continue
                
                inferred_count += 1
                
                if ref_table not in tables:
                    self.add('WARNING', '外键推断', f'_id列未匹配到表: {table_name}.{col.name} → {ref_table}?',
                            f'如果是外键需确认目标表', f'数据库中无`{ref_table}`',
                            f'{table_name}.{col.name}({col.type}) 可能是业务ID或自增主键',
                            f'如非外键可忽略; 如是外键需创建表`{ref_table}`或添加约束')
                else:
                    # 建议添加外键约束
                    self.add('INFO', '外键推断', f'建议添加外键: {table_name}.{col.name} → {ref_table}',
                            f'应添加外键约束保证数据完整性', f'当前无约束',
                            f'{col.name} 命名暗示外键关系',
                            f'ALTER TABLE `{table_name}` ADD CONSTRAINT fk_{table_name}_{col.name} FOREIGN KEY ({col.name}) REFERENCES {ref_table}(id);')
        
        logger.info(f"外键分析: 真实约束 {len(real_fks)} 个, 推断 {inferred_count} 个")
    
    def _analyze_indexes(self, tables: Dict[str, TableInfo]) -> None:
        """索引分析"""
        for table_name in tables:
            cols = self.db.get_columns(table_name)
            has_store = any(c.name == 'store_id' for c in cols)
            if not has_store:
                continue
            
            indexes = self.db.get_indexes(table_name)
            has_idx = any(i.col == 'store_id' for i in indexes)
            if not has_idx:
                self.add('WARNING', '索引', f'store_id无索引: {table_name}',
                        'store_id应有索引(多门店查询)', f'`{table_name}`.store_id无独立索引',
                        '多门店场景下全表扫描',
                        f'CREATE INDEX idx_{table_name}_store ON `{table_name}`(store_id);')
    
    def _analyze_empty_tables(self, tables: Dict[str, TableInfo]) -> None:
        """空表检测"""
        for table_name, table_info in tables.items():
            if table_info.rows == 0:
                is_sys = any(k in table_name for k in ['sys_', 'admin_', 'config', 'yield_rate', 'unit_'])
                self.add('INFO' if is_sys else 'WARNING', '数据量',
                        f'空表: {table_name}' if not is_sys else f'配置表(空): {table_name}',
                        '业务表应有数据', '0行',
                        '' if is_sys else '模块未启用或数据未灌入')
    
    def _analyze_collation(self, tables: Dict[str, TableInfo]) -> None:
        """字符集一致性"""
        colls = Counter(t.collation for t in tables.values())
        if len(colls) > 1:
            self.add('WARNING', '字符集', f'排序规则不一致({len(colls)}种)',
                    '统一为utf8mb4_0900_ai_ci', '; '.join(f'{k}({v}张)' for k, v in colls.items()),
                    '不一致会导致JOIN无法使用索引')
    
    def _analyze_engine(self, tables: Dict[str, TableInfo]) -> None:
        """引擎检查"""
        engines = Counter(t.engine for t in tables.values())
        for eng, cnt in engines.items():
            if eng != 'InnoDB':
                self.add('ERROR', '引擎', f'非InnoDB引擎: {cnt}张表({eng})',
                        '所有表应为InnoDB', f'{eng}({cnt}张)',
                        '不支持事务/行锁/外键',
                        f'ALTER TABLE t ENGINE=InnoDB;')
    
    def _analyze_field_redundancy(self, tables: Dict[str, TableInfo]) -> None:
        """字段冗余检测"""
        score = Counter()
        for table_name in tables:
            cols = self.db.get_columns(table_name)
            names = [c.name for c in cols]
            
            if len(set(['price', 'sale_price', 'cost_price']) & set(names)) >= 2:
                score[f'{table_name}:price/cost_price'] += 1
            
            if 'name' in names and any(n.endswith('_name') for n in names):
                score[f'{table_name}:name冗余'] += 1
        
        for k, v in score.items():
            self.add('WARNING', '字段冗余', f'疑似冗余字段组: {k}',
                    '同义字段仅保留一个', f'{v}组疑似重复')
    
    def _analyze_store_isolation(self, tables: Dict[str, TableInfo]) -> None:
        """多门店隔离检查"""
        no_store = []
        for table_name in tables:
            if any(k in table_name for k in ['sys_', 'admin_', 'users', 'config', 'ai_']):
                continue
            cols = self.db.get_columns(table_name)
            if not any(c.name == 'store_id' for c in cols):
                no_store.append(table_name)
        
        if no_store:
            self.add('WARNING', '多门店隔离', f'{len(no_store)}张业务表缺store_id',
                    '业务表应有store_id实现多门店隔离',
                    ', '.join(no_store[:8]) + ('...' if len(no_store) > 8 else ''),
                    '缺少多门店隔离会导致数据混淆')
    
    def _analyze_comments(self, tables: Dict[str, TableInfo]) -> None:
        """注释完整性"""
        no_comment_tbls = [t for t in tables if not tables[t].comment.strip()]
        no_comment_cols = 0
        for table_name in tables:
            cols = self.db.get_columns(table_name)
            for c in cols:
                if not c.comment.strip():
                    no_comment_cols += 1
        
        self.add('INFO' if not no_comment_tbls else 'WARNING', '注释',
                f'表注释缺失: {len(no_comment_tbls)}张 | 列注释缺失: {no_comment_cols}个',
                '所有表/列应有注释', f'表{len(no_comment_tbls)} 列{no_comment_cols}',
                '无注释增加维护成本')
