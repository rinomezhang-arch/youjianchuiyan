#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
又见炊烟餐饮管理系统 — 智能体检 v4 超级版
单文件脚本：分析 + 生成交互式 HTML 报告
"""
import os
import sys
import json
import datetime
import pymysql
import pymysql.cursors

# ============================== 配置加载 ==============================
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def load_env():
    env_path = os.path.join(SCRIPT_DIR, '.env')
    if not os.path.exists(env_path):
        return
    with open(env_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            key, value = key.strip(), value.strip().strip('"').strip("'")
            if key not in os.environ:
                os.environ[key] = value

load_env()

DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', '3306')),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASS', ''),
    'database': os.environ.get('DB_NAME', 'banquet'),
}

# ============================== 数据库查询 ==============================
class DB:
    def __init__(self, config):
        self.config = config
        self.conn = None
    
    def connect(self):
        if self.conn is None or not self.conn.open:
            self.conn = pymysql.connect(
                host=self.config['host'],
                port=self.config['port'],
                user=self.config['user'],
                password=self.config['password'],
                database=self.config['database'],
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor,
                connect_timeout=10,
            )
    
    def close(self):
        if self.conn and self.conn.open:
            self.conn.close()
            self.conn = None
    
    def query(self, sql):
        self.connect()
        with self.conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()
    
    def query_one(self, sql):
        self.connect()
        with self.conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchone()
    
    def query_scalar(self, sql):
        row = self.query_one(sql)
        if row:
            return list(row.values())[0]
        return None

# ============================== 分析引擎 ==============================
class Analyzer:
    def __init__(self, db):
        self.db = db
        self.findings = []
        self.counters = {'FATAL': 0, 'ERROR': 0, 'WARNING': 0, 'INFO': 0, 'NORMAL': 0}
        # 缓存：避免重复查询
        self._cache = {
            'columns': {},      # table -> [cols]
            'indexes': {},      # table -> [indexes]
            'pk': {},           # table -> [pk_cols]
            'foreign_keys': None,  # [fks] 全局只查一次
        }
    
    def _get_columns_cached(self, table):
        if table not in self._cache['columns']:
            self._cache['columns'][table] = self.db.query(f"SHOW FULL COLUMNS FROM `{table}`")
        return self._cache['columns'][table]
    
    def _get_indexes_cached(self, table):
        if table not in self._cache['indexes']:
            self._cache['indexes'][table] = self.db.query(f"SHOW INDEX FROM `{table}`")
        return self._cache['indexes'][table]
    
    def _get_pk_cached(self, table):
        if table not in self._cache['pk']:
            cols = self._get_columns_cached(table)
            self._cache['pk'][table] = [c for c in cols if c['Key'] == 'PRI']
        return self._cache['pk'][table]
    
    def _get_foreign_keys_cached(self):
        if self._cache['foreign_keys'] is None:
            sql = """
                SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME, CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
            """
            self._cache['foreign_keys'] = self.db.query(sql)
        return self._cache['foreign_keys']
    
    def _progress(self, msg):
        print(f'  [{datetime.datetime.now().strftime("%H:%M:%S")}] {msg}')
    
    def add(self, level, module, scene, title, expect, actual, detail='', fix_sql='', fix_cmd='', fix_file=''):
        self.findings.append({
            'id': len(self.findings) + 1,
            'level': level,
            'module': module,
            'scene': scene,
            'title': title,
            'expect': expect,
            'actual': actual,
            'detail': detail,
            'fix_sql': fix_sql,
            'fix_cmd': fix_cmd,
            'fix_file': fix_file,
        })
        self.counters[level] = self.counters.get(level, 0) + 1
    
    def analyze(self):
        self._progress('正在获取表清单...')
        tables = self._get_tables()
        if not tables:
            return [], {}, {}
        self._progress(f'共{len(tables)}张表，开始深度分析...')
        
        # 第一阶段：基础结构检查
        self._progress('[01/20] 检查主键完整性...')
        self._analyze_primary_keys(tables)
        self._progress('[02/20] 检查外键完整性...')
        self._analyze_foreign_keys(tables)
        self._progress('[03/20] 检查索引覆盖...')
        self._analyze_indexes(tables)
        self._progress('[04/20] 检查空表...')
        self._analyze_empty_tables(tables)
        self._progress('[05/20] 检查字符集一致性...')
        self._analyze_collation(tables)
        self._progress('[06/20] 检查存储引擎...')
        self._analyze_engine(tables)
        
        # 第二阶段：字段深度检查
        self._progress('[07/22] 检查重复业务表...')
        self._analyze_duplicate_tables(tables)
        self._progress('[08/22] 检查同义冗余字段...')
        self._analyze_redundant_columns(tables)
        self._progress('[09/22] 检查字段冗余...')
        self._analyze_field_redundancy(tables)
        self._progress('[10/22] 检查多门店隔离...')
        self._analyze_store_isolation(tables)
        self._progress('[11/22] 检查表/列注释...')
        self._analyze_comments(tables)
        self._progress('[12/22] 检查数据类型规范...')
        self._analyze_data_types(tables)
        self._progress('[13/22] 检查命名规范...')
        self._analyze_naming(tables)
        
        # 第三阶段：深度检查
        self._progress('[14/22] 检查索引效率...')
        self._analyze_index_efficiency(tables)
        self._progress('[15/22] 检查数据质量...')
        self._analyze_data_quality(tables)
        self._progress('[16/22] 检查表关联完整性...')
        self._analyze_relationships(tables)
        self._progress('[17/22] 检查审计字段...')
        self._analyze_audit_fields(tables)
        self._progress('[18/22] 检查安全性...')
        self._analyze_security(tables)
        self._progress('[19/22] 检查varchar长度...')
        self._analyze_varchar_length(tables)
        self._progress('[20/22] 检查日期字段...')
        self._analyze_date_fields(tables)
        self._progress('[21/22] 检查软删除...')
        self._analyze_soft_delete(tables)
        self._progress('[22/22] 生成数据看板...')
        self._add_dashboard(tables)
        
        summary = {
            'time': datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'tables': len(tables),
            'empty_tables': sum(1 for t in tables.values() if t['rows'] == 0),
            'total': len(self.findings),
            'fatal': self.counters['FATAL'],
            'error': self.counters['ERROR'],
            'warning': self.counters['WARNING'],
            'info': self.counters['INFO'],
            'normal': self.counters['NORMAL'],
        }
        return self.findings, summary, tables
    
    def _get_tables(self):
        sql = """
            SELECT TABLE_NAME as name, TABLE_ROWS as `rows`, TABLE_COMMENT as comment,
                   TABLE_COLLATION as collation, ENGINE as engine,
                   ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024, 1) as size_kb
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
            ORDER BY TABLE_NAME
        """
        rows = self.db.query(sql)
        return {r['name']: r for r in rows}
    
    def _get_columns(self, table):
        return self.db.query(f"SHOW FULL COLUMNS FROM `{table}`")
    
    def _analyze_primary_keys(self, tables):
        for tname in tables:
            pks = self._get_pk_cached(tname)
            if not pks:
                self.add('FATAL', '数据库-数据表', '主键完整性',
                        f'表无主键: {tname}',
                        '每张表应有PRIMARY KEY', f'{tname}无主键',
                        '无主键无法唯一标识行，同步/恢复/去重全部失效',
                        f'ALTER TABLE `{tname}` ADD PRIMARY KEY (id);')
                continue
            pk = pks[0]
            pk_type = pk['Type'].lower().split('(')[0]
            if pk_type.startswith('varchar') or pk_type.startswith('char'):
                self.add('WARNING', '数据库-数据表', '主键完整性',
                        f'varchar主键: {tname}.{pk["Field"]}({pk["Type"]})',
                        '主键应为bigint自增', f'varchar({pk["Type"]})',
                        '字符串主键：并发冲突、索引碎片化、排序效率低',
                        f'ALTER TABLE `{tname}` ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY;')
            elif pk_type == 'int' and 'bigint' not in pk['Type']:
                self.add('WARNING', '数据库-数据表', '主键完整性',
                        f'int主键: {tname}.{pk["Field"]}({pk["Type"]})',
                        '应使用bigint(上限922亿亿)', f'int(上限21亿)',
                        '数据量大后可能耗尽',
                        f'ALTER TABLE `{tname}` MODIFY `{pk["Field"]}` BIGINT AUTO_INCREMENT;')
            else:
                self.add('NORMAL', '数据库-数据表', '主键完整性',
                        f'主键正常: {tname}.{pk["Field"]}({pk["Type"]})',
                        '主键类型合理', f'{pk["Type"]}', '')
    
    def _analyze_foreign_keys(self, tables):
        real_fks = self._get_foreign_keys_cached()
        real_fk_set = {(fk['TABLE_NAME'], fk['COLUMN_NAME']) for fk in real_fks}
        
        for fk in real_fks:
            tname = fk['TABLE_NAME']
            col = fk['COLUMN_NAME']
            ref_table = fk['REFERENCED_TABLE_NAME']
            ref_col = fk['REFERENCED_COLUMN_NAME']
            
            if ref_table not in tables:
                self.add('ERROR', '数据库-约束', '外键完整性',
                        f'外键引用不存在的表: {tname}.{col} → {ref_table}',
                        f'引用表应存在', f'{ref_table}不存在',
                        '外键约束指向不存在的表',
                        f'CREATE TABLE `{ref_table}` (...); 或 ALTER TABLE `{tname}` DROP FOREIGN KEY {fk["CONSTRAINT_NAME"]};')
                continue
            
            src_cols = self._get_columns_cached(tname)
            src_col = next((c for c in src_cols if c['Field'] == col), None)
            ref_pks = self._get_pk_cached(ref_table)
            
            if src_col and ref_pks:
                ref_cols = self._get_columns_cached(ref_table)
                # 修复：使用实际被引用列 ref_col，而非主键第一列 ref_pks[0]['Field']
                # 原逻辑会误判 booking_dish_detail.booking_id(varchar) → booking_master.id(bigint) 不匹配
                # 实际外键指向 booking_master.booking_id(varchar 业务编号)
                # 同理复合外键 (dish_id, store_id) → dish_master(dish_id, store_id) 不应拆分判类型
                ref_pk_col = next((c for c in ref_cols if c['Field'] == ref_col), None)
                if ref_pk_col:
                    src_type = src_col['Type'].lower().split('(')[0]
                    ref_type = ref_pk_col['Type'].lower().split('(')[0]
                    if src_type != ref_type:
                        level = 'FATAL' if src_col['Null'] == 'NO' else 'ERROR'
                        self.add(level, '数据库-约束', '外键完整性',
                                f'外键类型不匹配: {tname}.{col}({src_type}) → {ref_table}.{ref_pk_col["Field"]}({ref_type})',
                                f'FK类型应与被引用列一致: {ref_type}', f'FK={src_type} REF={ref_type}',
                                'JOIN全表扫描或类型转换失败',
                                f'ALTER TABLE `{tname}` MODIFY `{col}` {ref_pk_col["Type"]};')
            
            orphan_count = self._count_orphans(tname, col, ref_table, ref_col)
            if orphan_count > 0:
                self.add('ERROR', '数据库-数据质量', '外键孤儿',
                        f'外键孤儿: {tname}.{col} → {ref_table}.{ref_col} ({orphan_count}条)',
                        f'{tname}.{col} 应全部存在于 {ref_table}.{ref_col}',
                        f'{orphan_count}条孤儿记录',
                        '数据完整性破坏',
                        f'DELETE FROM `{tname}` WHERE `{col}` NOT IN (SELECT `{ref_col}` FROM `{ref_table}`);')
            else:
                self.add('NORMAL', '数据库-约束', '外键完整性',
                        f'外键正常: {tname}.{col} → {ref_table}.{ref_col}',
                        '外键约束有效', f'0条孤儿', '')
        
        ref_map = {
            'store': 'store_info', 'staff': 'staff_master', 'customer': 'customer_master',
            'supplier': 'supplier_master', 'dish': 'dish_master', 'ingredient': 'ingredient_master',
            'booking': 'booking_master', 'table_': 'table_master', 'package': 'package_master',
            'account': 'finance_account', 'purchase': 'purchase_order', 'dept': 'department',
            'member': 'member_card', 'voucher': 'finance_voucher', 'level': 'member_level',
            'category': 'dish_category', 'requisition': 'requisition_order',
            'menu': 'sys_menu', 'role': 'sys_role', 'permission': 'sys_permission',
        }
        
        for tname in tables:
            cols = self._get_columns_cached(tname)
            for col in cols:
                if not col['Field'].endswith('_id') or col['Field'] == 'id':
                    continue
                if (tname, col['Field']) in real_fk_set:
                    continue
                
                ref_table = col['Field'][:-3]
                for k, v in ref_map.items():
                    if ref_table == k:
                        ref_table = v
                        break
                
                if ref_table == tname or ref_table not in tables:
                    continue
                
                self.add('INFO', '数据库-约束', '外键推断',
                        f'建议添加外键: {tname}.{col["Field"]} → {ref_table}',
                        f'应添加外键约束保证数据完整性', f'当前无约束',
                        f'{col["Field"]} 命名暗示外键关系',
                        f'ALTER TABLE `{tname}` ADD CONSTRAINT fk_{tname}_{col["Field"]} FOREIGN KEY ({col["Field"]}) REFERENCES {ref_table}(id);')
    
    def _analyze_indexes(self, tables):
        for tname in tables:
            cols = self._get_columns_cached(tname)
            has_store = any(c['Field'] == 'store_id' for c in cols)
            if not has_store:
                continue
            indexes = self._get_indexes_cached(tname)
            has_idx = any(i['Column_name'] == 'store_id' for i in indexes)
            if not has_idx:
                self.add('WARNING', '数据库-索引', 'store_id索引',
                        f'store_id无索引: {tname}',
                        'store_id应有索引(多门店查询)', f'`{tname}`.store_id无独立索引',
                        '多门店场景下全表扫描',
                        f'CREATE INDEX idx_{tname}_store ON `{tname}`(store_id);')
            else:
                self.add('NORMAL', '数据库-索引', 'store_id索引',
                        f'store_id索引正常: {tname}',
                        'store_id有索引', '已建索引', '')
    
    def _analyze_empty_tables(self, tables):
        for tname, tinfo in tables.items():
            rows = tinfo['rows']
            if rows == 0:
                is_sys = any(k in tname for k in ['sys_', 'admin_', 'config', 'yield_rate', 'unit_'])
                if is_sys:
                    self.add('NORMAL', '数据库-数据表', '空表检测',
                            f'配置表(空): {tname}',
                            '配置表可为空', '0行', '')
                else:
                    self.add('WARNING', '数据库-数据质量', '空表检测',
                            f'空表: {tname}',
                            '业务表应有数据', '0行',
                            '模块未启用或数据未灌入',
                            f'INSERT INTO `{tname}` ... -- 灌入初始数据')
            else:
                self.add('NORMAL', '数据库-数据表', '空表检测',
                        f'表有数据: {tname} ({rows}行)',
                        '业务表有数据', f'{rows}行', '')
    
    def _analyze_collation(self, tables):
        from collections import Counter
        colls = Counter(t['collation'] for t in tables.values())
        target_coll = 'utf8mb4_0900_ai_ci'
        for tname, tinfo in tables.items():
            coll = tinfo.get('collation', '')
            if coll != target_coll:
                self.add('WARNING', '数据库-数据表', '字符集一致性',
                        f'{tname} 排序规则非标准: {coll}',
                        '统一为utf8mb4_0900_ai_ci', coll,
                        '不一致会导致JOIN无法使用索引',
                        f"ALTER TABLE `{tname}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
            else:
                self.add('NORMAL', '数据库-数据表', '字符集一致性',
                        f'{tname} 排序规则正常', 'utf8mb4_0900_ai_ci', coll, '')
    
    def _analyze_engine(self, tables):
        for tname, tinfo in tables.items():
            eng = tinfo.get('engine', '')
            if eng != 'InnoDB':
                self.add('ERROR', '数据库-数据表', '存储引擎',
                        f'{tname} 非InnoDB引擎: {eng}',
                        '所有表应为InnoDB', eng,
                        '不支持事务/行锁/外键',
                        f"ALTER TABLE `{tname}` ENGINE=InnoDB;")
            else:
                self.add('NORMAL', '数据库-数据表', '存储引擎',
                        f'{tname} InnoDB引擎正常', 'InnoDB', eng, '')
    
    def _count_orphans(self, table, column, ref_table, ref_column):
        sql = (
            f"SELECT COUNT(*) AS cnt FROM `{table}` t1 "
            f"LEFT JOIN `{ref_table}` t2 ON t1.`{column}` = t2.`{ref_column}` "
            f"WHERE t1.`{column}` IS NOT NULL AND t2.`{ref_column}` IS NULL"
        )
        result = self.db.query_one(sql)
        return int(result['cnt']) if result else 0
    
    def _analyze_duplicate_tables(self, tables):
        """检查重复业务表（两组表都必须实际存在才报警）"""
        DUPLICATE_TABLE_GROUP = [
            ("purchase_request", "procurement_request"),
            ("material_requisition", "requisition_order"),
            ("cost_card", "dish_cost_card"),
            ("package_dish_detail", "package_dish_rel"),
            ("package_master", "meal_package"),
        ]
        # 对每组重复表检查，每张表生成一个finding
        for main, dup in DUPLICATE_TABLE_GROUP:
            if main in tables and dup in tables:
                self.add('FATAL', '数据库-数据表', '重复业务表',
                        f'主表 {main} 与冗余重复表 {dup} 同时存在',
                        '不应存在重复业务表',
                        f'{main} 和 {dup} 同时存在',
                        '两套表业务数据割裂对账不平',
                        fix_sql=f"SET FOREIGN_KEY_CHECKS=0;\nDROP TABLE IF EXISTS `{dup}`;\nSET FOREIGN_KEY_CHECKS=1;")
            else:
                self.add('NORMAL', '数据库-数据表', '重复业务表',
                        f'无重复: {main}/{dup}', '无重复表', '无重复表', '')

    def _analyze_redundant_columns(self, tables):
        """检查同义冗余字段"""
        # 修正：dish_master 真正的同义字段是 price/sale_price (都是售价)
        # cost_price 是成本价，与售价不是同义字段
        REDUNDANT_COL_MAP = {
            "dish_master": [("price", "sale_price")],
            "ingredient_master": [("unit_price", "avg_price")],
            "booking_master": [("deposit_amount", "deposit")]
        }
        col_err = []
        col_sql = "SET FOREIGN_KEY_CHECKS=0;\n"
        for tbl, pairs in REDUNDANT_COL_MAP.items():
            if tbl not in tables:
                continue
            col_names = [c['Field'] for c in self._get_columns_cached(tbl)]
            for old, new in pairs:
                if old in col_names and new in col_names:
                    col_err.append(f"{tbl} 存在重复业务字段 {old}/{new}")
                    col_sql += f"ALTER TABLE `{tbl}` DROP COLUMN `{old}`;\n"
        col_sql += "SET FOREIGN_KEY_CHECKS=1;"
        if col_err:
            self.add('ERROR', '数据库-数据表', '同义冗余字段',
                    f'发现{len(col_err)}组同义冗余字段: {";".join(col_err)}',
                    '同义字段仅保留一个',
                    ';'.join(col_err),
                    '双字段同步更新易统计错乱',
                    fix_sql=col_sql)
        else:
            self.add('NORMAL', '数据库-数据表', '同义冗余字段',
                    '无同义冗余字段', '无冗余字段', '无冗余字段', '')

    def _analyze_field_redundancy(self, tables):
        for tname in tables:
            cols = self._get_columns_cached(tname)
            names = [c['Field'] for c in cols]
            # 检查price/cost_price冗余
            price_cols = set(['price', 'sale_price', 'cost_price']) & set(names)
            if len(price_cols) >= 2:
                self.add('WARNING', '数据库-数据表', '字段冗余',
                        f'{tname}: 疑似冗余字段组 price/cost_price ({len(price_cols)}个)',
                        '同义字段仅保留一个', f'{len(price_cols)}个价格字段',
                        '多价格字段易混淆统计')
            # 检查name冗余
            if 'name' in names and any(n.endswith('_name') for n in names):
                name_cols = [n for n in names if n.endswith('_name')]
                self.add('WARNING', '数据库-数据表', '字段冗余',
                        f'{tname}: name与*_name冗余',
                        '同义字段仅保留一个', f'{len(name_cols)}个*_name字段',
                        'name字段冗余')
            # 无冗余
            if len(price_cols) < 2 and not ('name' in names and any(n.endswith('_name') for n in names)):
                self.add('NORMAL', '数据库-数据表', '字段冗余',
                        f'{tname}: 无冗余字段', '无冗余', '正常', '')
    
    def _analyze_store_isolation(self, tables):
        for tname in tables:
            if any(k in tname for k in ['sys_', 'admin_', 'users', 'config', 'ai_']):
                continue
            cols = self._get_columns_cached(tname)
            has_store = any(c['Field'] == 'store_id' for c in cols)
            if not has_store:
                self.add('WARNING', '数据库-数据表', '多门店隔离',
                        f'{tname} 缺store_id',
                        '业务表应有store_id实现多门店隔离', '无store_id',
                        '缺少多门店隔离会导致数据混淆')
            else:
                self.add('NORMAL', '数据库-数据表', '多门店隔离',
                        f'{tname} 有store_id', '有store_id', '正常', '')
    
    def _analyze_comments(self, tables):
        # 表注释检查 - 每张表一个finding
        for tname, tinfo in tables.items():
            comment = tinfo.get('comment', '').strip()
            if not comment:
                self.add('WARNING', '数据库-数据表', '表注释完整性',
                        f'{tname} 表注释缺失',
                        '所有表应有注释', '无注释',
                        '无注释增加维护成本',
                        f"ALTER TABLE `{tname}` COMMENT='请补充表注释';")
            else:
                self.add('NORMAL', '数据库-数据表', '表注释完整性',
                        f'{tname} 表注释正常', '有注释', comment, '')
        
        # 列注释检查 - 每张表每个列一个finding
        for tname in sorted(tables.keys()):
            cols = self._get_columns_cached(tname)
            for col in cols:
                comment = col.get('Comment', '').strip()
                if not comment:
                    self.add('WARNING', '数据库-数据表', '列注释完整性',
                            f'{tname}.{col["Field"]} 列注释缺失',
                            '所有列应有COMMENT注释说明用途', '无注释',
                            '未注释列影响理解和维护',
                            f"ALTER TABLE `{tname}` MODIFY COLUMN `{col['Field']}` {col['Type']} COMMENT '请补充注释';")
                else:
                    self.add('NORMAL', '数据库-数据表', '列注释完整性',
                            f'{tname}.{col["Field"]} 列注释正常', '有注释', comment, '')
    
    def _analyze_data_types(self, tables):
        """检查数据类型规范 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            text_blob = []
            float_cols = []
            money_not_decimal = []
            
            for col in cols:
                col_type = col['Type'].lower()
                fname = col['Field'].lower()
                
                # TEXT/BLOB 大字段
                if col_type in ['text', 'mediumtext', 'longtext', 'blob', 'mediumblob', 'longblob']:
                    text_blob.append(f'{tname}.{col["Field"]}({col_type})')
                
                # float/double 精度问题
                if col_type.startswith('float') or col_type.startswith('double'):
                    float_cols.append(f'{tname}.{col["Field"]}({col_type})')
                
                # 金额相关字段应该用 decimal
                if any(k in fname for k in ['price', 'cost', 'amount', 'fee', 'money', 'pay', 'balance', 'total']):
                    if 'decimal' not in col_type and 'numeric' not in col_type:
                        money_not_decimal.append(f'{tname}.{col["Field"]}({col_type})')
            
            # 大字段检测
            if text_blob:
                self.add('INFO', '数据库-数据类型', '大字段检测',
                        f'{tname} 有TEXT/BLOB大字段: {len(text_blob)}个',
                        '大字段应考虑是否拆分到扩展表',
                        f'{len(text_blob)}个', '大字段影响查询性能',
                        fix_file='\n'.join(text_blob[:30]))
            else:
                self.add('NORMAL', '数据库-数据类型', '大字段检测',
                        f'{tname} 无TEXT/BLOB大字段', '无大字段', '合规', '')
            
            # 浮点数检测
            if float_cols:
                self.add('WARNING', '数据库-数据类型', '浮点数精度',
                        f'{tname} 使用float/double: {len(float_cols)}个',
                        '金额/数量应用decimal', f'{len(float_cols)}个列',
                        '浮点数有精度损失，金额计算会出错',
                        fix_file='\n'.join(float_cols[:30]))
            else:
                self.add('NORMAL', '数据库-数据类型', '浮点数精度',
                        f'{tname} 无float/double', '应用decimal', '合规', '')
            
            # 金额字段类型检测
            if money_not_decimal:
                self.add('WARNING', '数据库-数据类型', '金额字段类型',
                        f'{tname} 金额字段未用DECIMAL: {len(money_not_decimal)}个',
                        'price/cost/amount等应用DECIMAL(10,2)',
                        f'{len(money_not_decimal)}个', '金额用非decimal会精度丢失',
                        fix_file='\n'.join(money_not_decimal[:30]))
            else:
                self.add('NORMAL', '数据库-数据类型', '金额字段类型',
                        f'{tname} 金额字段均用DECIMAL', 'DECIMAL', '合规', '')
    
    def _analyze_naming(self, tables):
        """检查命名规范 - 逐表检查"""
        import re
        for tname in tables:
            issues = []
            # 表名应该小写下划线
            if not re.match(r'^[a-z][a-z0-9_]*$', tname):
                issues.append(('表', tname, '应小写下划线'))
            
            cols = self._get_columns_cached(tname)
            for col in cols:
                # 列名应该小写下划线
                if not re.match(r'^[a-z][a-z0-9_]*$', col['Field']):
                    issues.append(('列', f'{tname}.{col["Field"]}', '应小写下划线'))
            
            if issues:
                self.add('WARNING', '数据库-命名规范', '命名规范',
                        f'{tname} 命名不规范: {len(issues)}个',
                        '表名/列名应小写下划线', f'{len(issues)}个不规范',
                        '不统一增加维护成本',
                        fix_file='\n'.join([f'{t}:{n} - {r}' for t,n,r in issues[:20]]))
            else:
                self.add('NORMAL', '数据库-命名规范', '命名规范',
                        f'{tname} 命名规范统一', '小写下划线', '全部合规', '')
    
    def _analyze_index_efficiency(self, tables):
        """检查索引效率 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            indexes = self._get_indexes_cached(tname)
            no_index_fk = []
            redundant_index = []
            
            # 检查外键字段是否有索引
            for col in cols:
                if col['Field'].endswith('_id') and col['Field'] != 'id':
                    has_idx = any(i['Column_name'] == col['Field'] for i in indexes)
                    if not has_idx:
                        no_index_fk.append(f'{tname}.{col["Field"]}')
            
            # 检查冗余索引（同一列被多个索引覆盖）
            idx_cols = {}
            for idx in indexes:
                col_name = idx['Column_name']
                if col_name not in idx_cols:
                    idx_cols[col_name] = []
                idx_cols[col_name].append(idx['Key_name'])
            
            for col_name, idx_names in idx_cols.items():
                if len(idx_names) > 2:  # 同一列超过2个索引
                    redundant_index.append(f'{tname}.{col_name}: {", ".join(idx_names)}')
            
            # 外键索引
            if no_index_fk:
                self.add('WARNING', '数据库-索引', '外键索引',
                        f'{tname} 外键字段无索引: {len(no_index_fk)}个',
                        '外键字段应建索引加速JOIN',
                        f'{len(no_index_fk)}个',
                        '无索引导致JOIN全表扫描',
                        fix_file='\n'.join(no_index_fk[:30]))
            else:
                self.add('NORMAL', '数据库-索引', '外键索引',
                        f'{tname} 外键字段均有索引', '已索引', '合规', '')
            
            # 冗余索引
            if redundant_index:
                self.add('INFO', '数据库-索引', '冗余索引',
                        f'{tname} 疑似冗余索引: {len(redundant_index)}个',
                        '同一列不应有多个索引',
                        f'{len(redundant_index)}个',
                        '冗余索引浪费空间降低写入性能',
                        fix_file='\n'.join(redundant_index[:20]))
            else:
                self.add('NORMAL', '数据库-索引', '冗余索引',
                        f'{tname} 无冗余索引', '无冗余', '合规', '')
    
    def _analyze_data_quality(self, tables):
        """检查数据质量 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            null_checks = []
            enum_checks = []
            
            for col in cols:
                fname = col['Field'].lower()
                ftype = col['Type'].lower()
                nullable = col['Null'] == 'YES'
                
                # 状态/类型字段应该NOT NULL
                if any(k in fname for k in ['status', 'type', 'state', 'flag', 'is_']):
                    if nullable:
                        null_checks.append(f'{tname}.{col["Field"]}')
                
                # 性别/状态等低基数字段应用ENUM
                if fname in ['gender', 'sex', 'status', 'state'] and 'enum' not in ftype and 'tinyint' not in ftype:
                    enum_checks.append(f'{tname}.{col["Field"]}({ftype})')
            
            # NOT NULL约束
            if null_checks:
                self.add('WARNING', '数据库-数据质量', 'NOT NULL约束',
                        f'{tname} 状态/类型字段可为NULL: {len(null_checks)}个',
                        'status/type等字段应NOT NULL',
                        f'{len(null_checks)}个',
                        'NULL状态字段会导致查询歧义',
                        fix_file='\n'.join(null_checks[:30]))
            else:
                self.add('NORMAL', '数据库-数据质量', 'NOT NULL约束',
                        f'{tname} 状态字段均有NOT NULL约束', '合规', '', '')
            
            # ENUM优化
            if enum_checks:
                self.add('INFO', '数据库-数据质量', 'ENUM优化',
                        f'{tname} 低基数字段可用ENUM: {len(enum_checks)}个',
                        'gender/status等可用ENUM或TINYINT',
                        f'{len(enum_checks)}个',
                        'ENUM节省空间提升查询效率',
                        fix_file='\n'.join(enum_checks[:20]))
            else:
                self.add('NORMAL', '数据库-数据质量', 'ENUM优化',
                        f'{tname} 低基数字段已优化', '合规', '', '')
    
    def _analyze_relationships(self, tables):
        """检查表关联完整性 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            logical_fks = []
            for col in cols:
                if col['Field'].endswith('_id') and col['Field'] != 'id':
                    ref_table = col['Field'][:-3]
                    # 简单映射
                    ref_map = {
                        'store': 'store_info', 'staff': 'staff_master', 'customer': 'customer_master',
                        'supplier': 'supplier_master', 'dish': 'dish_master', 'ingredient': 'ingredient_master',
                    }
                    ref_table = ref_map.get(ref_table, f'{ref_table}_master')
                    if ref_table in tables:
                        logical_fks.append((tname, col['Field'], ref_table))
            
            if logical_fks:
                self.add('INFO', '数据库-关联', '逻辑外键',
                        f'{tname} 发现{len(logical_fks)}个逻辑外键关系',
                        '建议添加实际外键约束',
                        f'{len(logical_fks)}个',
                        '逻辑外键无数据完整性保证')
            else:
                self.add('NORMAL', '数据库-关联', '逻辑外键',
                        f'{tname} 无逻辑外键', '无', '')
    
    def _analyze_audit_fields(self, tables):
        """检查审计字段（创建时间、更新时间等）- 逐表检查"""
        for tname in tables:
            # 跳过系统表
            if any(tname.startswith(p) for p in ['sys_', 'admin_', 'config']):
                continue
            cols = self._get_columns_cached(tname)
            col_names = [c['Field'] for c in cols]
            
            # 检查是否有创建时间
            has_create_time = any('create' in c and 'time' in c for c in col_names)
            has_update_time = any('update' in c and 'time' in c for c in col_names)
            
            if not has_create_time and not has_update_time:
                self.add('WARNING', '数据库-审计', '审计字段缺失',
                        f'{tname} 缺少时间审计字段',
                        '业务表应有created_at/updated_at',
                        '无审计字段',
                        '无法追踪数据变更时间')
            else:
                self.add('NORMAL', '数据库-审计', '审计字段',
                        f'{tname} 有审计字段', '完整', '')
    
    def _analyze_security(self, tables):
        """检查安全性（敏感字段加密）- 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            sensitive_fields = []
            for col in cols:
                fname = col['Field'].lower()
                # 检查敏感字段
                if any(k in fname for k in ['password', 'secret', 'token', 'key', 'phone', 'mobile', 'idcard']):
                    # 检查是否明文存储（varchar类型）
                    if 'varchar' in col['Type'].lower() or 'char' in col['Type'].lower():
                        sensitive_fields.append(f'{tname}.{col["Field"]}')
            
            if sensitive_fields:
                self.add('WARNING', '数据库-安全', '敏感字段明文',
                        f'{tname} 有{len(sensitive_fields)}个敏感字段可能明文存储',
                        '密码/手机号等应加密存储',
                        ', '.join(sensitive_fields[:10]),
                        '敏感信息泄露风险')
            else:
                self.add('NORMAL', '数据库-安全', '敏感字段',
                        f'{tname} 未发现明文敏感字段', '安全', '')
    
    def _analyze_varchar_length(self, tables):
        """检查varchar长度合理性 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            issues = []
            for col in cols:
                if 'varchar' in col['Type'].lower():
                    # 提取长度
                    import re
                    match = re.search(r'varchar\((\d+)\)', col['Type'].lower())
                    if match:
                        length = int(match.group(1))
                        fname = col['Field'].lower()
                        
                        # 检查不合理的长度
                        if length > 500 and not any(k in fname for k in ['content', 'desc', 'remark', 'address', 'url', 'path']):
                            issues.append(f'{tname}.{col["Field"]}(varchar({length}))')
                        elif length < 10 and any(k in fname for k in ['name', 'title', 'desc']):
                            issues.append(f'{tname}.{col["Field"]}(varchar({length})过短)')
            
            if issues:
                self.add('INFO', '数据库-字段', 'varchar长度',
                        f'{tname} 有{len(issues)}个varchar长度可能不合理',
                        '检查varchar长度是否过大或过小',
                        f'{len(issues)}个',
                        '不合理的长度浪费空间或限制数据')
            else:
                self.add('NORMAL', '数据库-字段', 'varchar长度',
                        f'{tname} varchar长度合理', '合理', '')
    
    def _analyze_date_fields(self, tables):
        """检查日期字段类型 - 逐表检查"""
        for tname in tables:
            cols = self._get_columns_cached(tname)
            issues = []
            for col in cols:
                fname = col['Field'].lower()
                ftype = col['Type'].lower()
                
                # 日期字段应该用date/datetime/timestamp
                if any(k in fname for k in ['date', 'time', 'at']):
                    if not any(t in ftype for t in ['date', 'time', 'timestamp']):
                        issues.append(f'{tname}.{col["Field"]}({ftype})')
            
            if issues:
                self.add('WARNING', '数据库-字段', '日期字段类型',
                        f'{tname} 有{len(issues)}个日期字段类型不正确',
                        '日期时间字段应用date/datetime/timestamp',
                        f'{len(issues)}个',
                        '错误的类型导致无法正确查询')
            else:
                self.add('NORMAL', '数据库-字段', '日期字段',
                        f'{tname} 日期字段类型正确', '正确', '')
    
    def _analyze_soft_delete(self, tables):
        """检查软删除字段 - 逐表检查"""
        for tname in tables:
            # 跳过系统表
            if any(tname.startswith(p) for p in ['sys_', 'admin_', 'config']):
                continue
            cols = self._get_columns_cached(tname)
            col_names = [c['Field'] for c in cols]
            
            # 检查是否有软删除字段
            has_soft_delete = any('deleted' in c or 'is_delete' in c or 'status' in c for c in col_names)
            
            if not has_soft_delete:
                self.add('INFO', '数据库-设计', '软删除字段',
                        f'{tname} 缺少软删除字段',
                        '业务表建议有deleted/is_deleted字段',
                        '无软删除字段',
                        '硬删除无法恢复数据')
            else:
                self.add('NORMAL', '数据库-设计', '软删除',
                        f'{tname} 有软删除机制', '完整', '')
    
    def _add_dashboard(self, tables):
        self.add('NORMAL', '数据看板', '表统计', f'总表数: {len(tables)}', '统计', f'{len(tables)}张', '')
        empty = sum(1 for t in tables.values() if t['rows'] == 0)
        self.add('NORMAL', '数据看板', '表统计', f'空表数: {empty}', '统计', f'{empty}张', '')
        total_rows = sum(t['rows'] for t in tables.values())
        self.add('NORMAL', '数据看板', '表统计', f'总行数: {total_rows}', '统计', f'{total_rows}行', '')

# ============================== 前端检查 ==============================
def analyze_frontend(project_root):
    """前端Vue项目检查"""
    findings = []
    frontend_dir = os.path.join(project_root, 'frontend_v3', 'src')
    if not os.path.exists(frontend_dir):
        return findings
    
    views_dir = os.path.join(frontend_dir, 'views', 'dashboard')
    router_file = os.path.join(frontend_dir, 'router', 'index.js')
    api_dir = os.path.join(frontend_dir, 'api')
    
    # 1. 检查Vue组件完整性
    vue_files = []
    if os.path.exists(views_dir):
        for f in os.listdir(views_dir):
            if f.endswith('.vue'):
                vue_files.append(f.replace('.vue', ''))
    
    for vue in vue_files:
        findings.append({
            'id': f'FE-{len(findings)+1:04d}',
            'level': 'NORMAL',
            'module': '前端-组件',
            'scene': 'Vue组件完整性',
            'title': f'Vue组件存在: {vue}.vue',
            'expect': '组件文件应存在',
            'actual': f'{vue}.vue 存在',
            'detail': '',
            'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
        })
    
    # 2. 检查路由覆盖
    if os.path.exists(router_file):
        with open(router_file, 'r', encoding='utf-8') as f:
            router_content = f.read()
        
        for vue in vue_files:
            if vue.lower() in router_content.lower():
                findings.append({
                    'id': f'FE-{len(findings)+1:04d}',
                    'level': 'NORMAL',
                    'module': '前端-路由',
                    'scene': '路由覆盖',
                    'title': f'路由已配置: {vue}',
                    'expect': '组件应在路由中注册',
                    'actual': f'{vue} 在路由中',
                    'detail': '',
                    'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
                })
            else:
                findings.append({
                    'id': f'FE-{len(findings)+1:04d}',
                    'level': 'WARNING',
                    'module': '前端-路由',
                    'scene': '路由覆盖',
                    'title': f'路由未配置: {vue}',
                    'expect': '组件应在路由中注册',
                    'actual': f'{vue} 未在路由中',
                    'detail': '组件无法访问',
                    'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
                })
    
    # 3. 检查API文件
    api_files = []
    if os.path.exists(api_dir):
        for f in os.listdir(api_dir):
            if f.endswith('.js'):
                api_files.append(f.replace('.js', ''))
    
    for api in api_files:
        findings.append({
            'id': f'FE-{len(findings)+1:04d}',
            'level': 'NORMAL',
            'module': '前端-API',
            'scene': 'API文件完整性',
            'title': f'API文件存在: {api}.js',
            'expect': 'API文件应存在',
            'actual': f'{api}.js 存在',
            'detail': '',
            'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
        })
    
    return findings

# ============================== 后端检查 ==============================
def analyze_backend(project_root):
    """后端Java Spring Boot项目检查"""
    findings = []
    backend_dir = os.path.join(project_root, 'banquet_project', 'src', 'main', 'java', 'com', 'youjian', 'banquet')
    if not os.path.exists(backend_dir):
        return findings
    
    controller_dir = os.path.join(backend_dir, 'controller')
    service_dir = os.path.join(backend_dir, 'service')
    repository_dir = os.path.join(backend_dir, 'repository')
    entity_dir = os.path.join(backend_dir, 'entity')
    
    # 1. 检查Controller完整性
    controllers = []
    if os.path.exists(controller_dir):
        for f in os.listdir(controller_dir):
            if f.endswith('Controller.java'):
                controllers.append(f.replace('Controller.java', ''))
    
    for ctrl in controllers:
        findings.append({
            'id': f'BE-{len(findings)+1:04d}',
            'level': 'NORMAL',
            'module': '后端-Controller',
            'scene': 'Controller完整性',
            'title': f'Controller存在: {ctrl}Controller',
            'expect': 'Controller应存在',
            'actual': f'{ctrl}Controller.java 存在',
            'detail': '',
            'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
        })
    
    # 2. 检查Service完整性
    services = []
    if os.path.exists(service_dir):
        for f in os.listdir(service_dir):
            if f.endswith('Service.java') or f.endswith('ServiceImpl.java'):
                services.append(f.replace('Service.java', '').replace('ServiceImpl.java', ''))
    
    for svc in services:
        findings.append({
            'id': f'BE-{len(findings)+1:04d}',
            'level': 'NORMAL',
            'module': '后端-Service',
            'scene': 'Service完整性',
            'title': f'Service存在: {svc}',
            'expect': 'Service应存在',
            'actual': f'{svc} 存在',
            'detail': '',
            'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
        })
    
    # 3. 检查Controller-Service对应关系
    for ctrl in controllers:
        if ctrl not in services:
            findings.append({
                'id': f'BE-{len(findings)+1:04d}',
                'level': 'WARNING',
                'module': '后端-对应关系',
                'scene': 'Controller-Service对应',
                'title': f'Controller无对应Service: {ctrl}',
                'expect': 'Controller应有对应Service',
                'actual': f'{ctrl}Controller 无对应Service',
                'detail': '业务逻辑缺失',
                'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
            })
        else:
            findings.append({
                'id': f'BE-{len(findings)+1:04d}',
                'level': 'NORMAL',
                'module': '后端-对应关系',
                'scene': 'Controller-Service对应',
                'title': f'Controller-Service对应: {ctrl}',
                'expect': 'Controller应有对应Service',
                'actual': f'{ctrl}Controller 有对应Service',
                'detail': '',
                'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
            })
    
    # 4. 检查Entity完整性
    entities = []
    if os.path.exists(entity_dir):
        for f in os.listdir(entity_dir):
            if f.endswith('.java'):
                entities.append(f.replace('.java', ''))
    
    for ent in entities:
        findings.append({
            'id': f'BE-{len(findings)+1:04d}',
            'level': 'NORMAL',
            'module': '后端-Entity',
            'scene': 'Entity完整性',
            'title': f'Entity存在: {ent}',
            'expect': 'Entity应存在',
            'actual': f'{ent}.java 存在',
            'detail': '',
            'fix_sql': '', 'fix_cmd': '', 'fix_file': ''
        })
    
    return findings

# ============================== HTML 报告生成 ==============================
def generate_html(findings, summary, tables, output_path):
    data_json = json.dumps(findings, ensure_ascii=False)
    modules = sorted(set(f['module'] for f in findings))
    scenes = sorted(set(f['scene'] for f in findings))
    
    module_options = '\n'.join(f'<option value="{m}">{m}</option>' for m in modules)
    scene_options = '\n'.join(f'<option value="{s}">{s}</option>' for s in scenes)
    
    table_rows = ''
    for t in sorted(tables.values(), key=lambda x: x['rows'], reverse=True):
        table_rows += f'<tr><td>{t["name"]}</td><td style="text-align:right">{t["rows"]}</td><td>{t["comment"]}</td><td>{t["engine"]}</td><td>{t["collation"]}</td></tr>'
    
    html = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>餐饮系统体检报告 V4</title>
<style>
*{{margin:0;padding:0;box-sizing:border-box;font-family:"Microsoft YaHei","PingFang SC",sans-serif;}}
body{{padding:20px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;}}
.wrap{{max-width:1400px;margin:0 auto;background:#fff;padding:32px;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,0.3);}}
h1{{text-align:center;font-size:28px;padding-bottom:16px;border-bottom:3px solid #2563eb;margin-bottom:20px;color:#1e293b;}}
h1 small{{color:#64748b;font-size:14px;font-weight:normal;display:block;margin-top:8px;}}
.sum-top{{font-size:15px;padding:20px;background:linear-gradient(135deg,#e0f2fe 0%,#dbeafe 100%);border-radius:12px;margin-bottom:20px;line-height:1.8;border-left:4px solid #2563eb;}}
.sum-top strong{{color:#1e40af;}}
.filter-row{{display:flex;gap:12px;margin-bottom:16px;align-items:center;flex-wrap:wrap;padding:16px;background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;}}
.filter-row label{{font-size:14px;font-weight:600;color:#475569;white-space:nowrap;}}
.filter-row select,.filter-row input{{padding:10px 14px;border:2px solid #cbd5e1;border-radius:8px;font-size:14px;transition:all 0.2s;}}
.filter-row select:focus,.filter-row input:focus{{border-color:#2563eb;outline:none;box-shadow:0 0 0 3px rgba(37,99,235,0.1);}}
.filter-row input{{flex:1;min-width:200px;max-width:400px;}}
.tab-bar{{display:flex;gap:10px;margin-bottom:18px;flex-wrap:wrap;padding:8px;background:#f1f5f9;border-radius:12px;}}
.tab-btn{{padding:10px 20px;border:none;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;transition:all 0.2s;box-shadow:0 2px 4px rgba(0,0,0,0.05);}}
.tab-all{{background:#fff;color:#334155;border:2px solid #cbd5e1;}}
.tab-fatal{{background:#fef2f2;color:#dc2626;border:2px solid #fecaca;}}
.tab-error{{background:#fff7ed;color:#ea580c;border:2px solid #fed7aa;}}
.tab-warn{{background:#fffbeb;color:#d97706;border:2px solid #fde68a;}}
.tab-normal{{background:#f0fdf4;color:#16a34a;border:2px solid #bbf7d0;}}
.tab-info{{background:#eff6ff;color:#2563eb;border:2px solid #bfdbfe;}}
.tab-btn:hover{{transform:translateY(-2px);box-shadow:0 4px 12px rgba(0,0,0,0.1);}}
.tab-btn.active{{outline:3px solid #2563eb;font-weight:700;transform:translateY(-2px);}}
#itemContainer{{display:flex;flex-direction:column;gap:12px;max-height:700px;overflow-y:auto;padding:8px;}}
#itemContainer::-webkit-scrollbar{{width:8px;}}
#itemContainer::-webkit-scrollbar-track{{background:#f1f5f9;border-radius:4px;}}
#itemContainer::-webkit-scrollbar-thumb{{background:#cbd5e1;border-radius:4px;}}
#itemContainer::-webkit-scrollbar-thumb:hover{{background:#94a3b8;}}
.item{{padding:18px;border-radius:10px;border-width:2px;border-style:solid;transition:all 0.2s;}}
.item:hover{{transform:translateX(4px);box-shadow:0 4px 16px rgba(0,0,0,0.08);}}
.fatal{{background:#fef2f2;border-color:#ef4444;border-left-width:6px;}}
.error{{background:#fff7ed;border-color:#f97316;border-left-width:6px;}}
.warning{{background:#fffbeb;border-color:#eab308;border-left-width:6px;}}
.normal{{background:#f0fdf4;border-color:#22c55e;border-left-width:6px;}}
.info{{background:#eff6ff;border-color:#3b82f6;border-left-width:6px;}}
.item h4{{font-size:16px;margin-bottom:8px;word-break:break-all;color:#1e293b;font-weight:600;}}
.item .badge{{font-size:11px;padding:4px 10px;border-radius:10px;color:#fff;margin-right:8px;font-weight:600;text-transform:uppercase;}}
.badge-fatal{{background:linear-gradient(135deg,#dc2626,#b91c1c);}}
.badge-error{{background:linear-gradient(135deg,#ea580c,#c2410c);}}
.badge-warn{{background:linear-gradient(135deg,#eab308,#ca8a04);}}
.badge-norm{{background:linear-gradient(135deg,#22c55e,#16a34a);}}
.badge-info{{background:linear-gradient(135deg,#3b82f6,#2563eb);}}
.item p{{font-size:14px;margin:6px 0;line-height:1.6;word-break:break-all;color:#475569;}}
.item p code{{background:#f1f5f9;padding:2px 6px;border-radius:4px;font-family:Consolas,monospace;font-size:13px;color:#0f172a;}}
.item .meta{{font-size:12px;color:#64748b;margin-bottom:6px;font-weight:500;}}
.opt{{display:flex;gap:8px;margin-top:12px;flex-wrap:wrap;}}
.opt button{{padding:8px 14px;border:none;border-radius:6px;cursor:pointer;font-size:13px;font-weight:500;transition:all 0.2s;}}
.opt button:hover{{transform:translateY(-1px);box-shadow:0 4px 8px rgba(0,0,0,0.15);}}
.btn-sql{{background:linear-gradient(135deg,#3b82f6,#2563eb);color:#fff;}}
.btn-cmd{{background:linear-gradient(135deg,#a855f7,#7c3aed);color:#fff;}}
.btn-file{{background:linear-gradient(135deg,#06b6d4,#0891b2);color:#fff;}}
.result-count{{font-size:14px;color:#64748b;margin-bottom:10px;font-weight:500;padding:8px 12px;background:#f8fafc;border-radius:6px;display:inline-block;}}
#popMask{{display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.7);align-items:center;justify-content:center;z-index:9999;backdrop-filter:blur(4px);}}
.pop-box{{width:80%;max-height:85vh;background:#fff;padding:28px;border-radius:16px;overflow-y:auto;box-shadow:0 20px 60px rgba(0,0,0,0.4);}}
.pop-box h3{{font-size:20px;margin-bottom:16px;color:#1e293b;}}
#popText{{width:100%;min-height:300px;padding:16px;font-size:14px;margin-bottom:16px;resize:vertical;font-family:Consolas,monospace;border:2px solid #e2e8f0;border-radius:8px;background:#f8fafc;}}
#popText:focus{{border-color:#2563eb;outline:none;}}
.pop-btns{{display:flex;gap:12px;justify-content:flex-end;}}
.pop-btns button{{padding:10px 20px;border:none;border-radius:8px;cursor:pointer;font-size:14px;font-weight:500;transition:all 0.2s;}}
.pop-btns button:first-child{{background:linear-gradient(135deg,#3b82f6,#2563eb);color:#fff;}}
.pop-btns button:last-child{{background:#f1f5f9;color:#475569;border:2px solid #cbd5e1;}}
.pop-btns button:hover{{transform:translateY(-1px);box-shadow:0 4px 8px rgba(0,0,0,0.15);}}
table{{width:100%;border-collapse:collapse;font-size:13px;margin:20px 0;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.05);}}
table th,td{{border:1px solid #e2e8f0;padding:10px 14px;text-align:left;}}
table th{{background:linear-gradient(135deg,#f1f5f9,#e2e8f0);font-weight:600;color:#1e293b;text-transform:uppercase;font-size:12px;letter-spacing:0.5px;}}
tr:nth-child(even){{background:#f8fafc;}}
tr:hover{{background:#eff6ff;}}
td:nth-child(2){{text-align:right;font-family:Consolas,monospace;color:#64748b;}}
@media print{{
 .filter-row,.tab-bar,.opt,#popMask,.result-count{{display:none !important;}}
 body{{padding:6px;background:#fff;}}
 .wrap{{box-shadow:none;}}
 .item{{border:2px solid #999;page-break-inside:avoid;}}
 h1{{font-size:20px;}}
 .sum-top{{font-size:12px;background:#f5f5f5;}}
}}
</style>
</head>
<body class="wrap">
<h1>🍽️ 餐饮系统全量体检报告 V4<br><small>{summary['time']} · 纯数据库分析 · {summary['tables']}张表</small></h1>
<div class="sum-top">
体检时间：{summary['time']}<br>
总检查项：{summary['total']} 项 | FATAL:{summary['fatal']} ERROR:{summary['error']} WARN:{summary['warning']} INFO:{summary['info']} NORM:{summary['normal']}
</div>
<div class="filter-row">
<label>搜索</label>
<input type="text" id="searchInput" placeholder="输入关键字过滤（编号/标题/模块/场景）" oninput="doFilter()">
<label>类型</label>
<select id="moduleSelect" onchange="doFilter()"><option value="all">全部类型</option>
{module_options}
</select>
<label>场景</label>
<select id="sceneSelect" onchange="doFilter()"><option value="all">全部场景</option>
{scene_options}
</select>
</div>
<div class="tab-bar">
<button class="tab-btn tab-all" data-filter="all" onclick="setFilter('all',this)">全部({summary['total']})</button>
<button class="tab-btn tab-fatal" data-filter="FATAL" onclick="setFilter('FATAL',this)">致命({summary['fatal']})</button>
<button class="tab-btn tab-error" data-filter="ERROR" onclick="setFilter('ERROR',this)">严重({summary['error']})</button>
<button class="tab-btn tab-warn" data-filter="WARNING" onclick="setFilter('WARNING',this)">警告({summary['warning']})</button>
<button class="tab-btn tab-info" data-filter="INFO" onclick="setFilter('INFO',this)">提示({summary['info']})</button>
<button class="tab-btn tab-normal" data-filter="NORMAL" onclick="setFilter('NORMAL',this)">正常({summary['normal']})</button>
</div>
<div class="result-count" id="resultCount"></div>
<div id="itemContainer"></div>

<h3 style="margin-top:20px;">📋 表清单 ({len(tables)}张)</h3>
<table>
<tr><th>表名</th><th style="text-align:right">行数</th><th>注释</th><th>引擎</th><th>字符集</th></tr>
{table_rows}
</table>

<div id="popMask">
<div class="pop-box">
<h3>修复内容</h3>
<textarea id="popText" readonly></textarea>
<div class="pop-btns">
<button onclick="copyPopText()">一键复制</button>
<button onclick="closePop()">关闭</button>
</div>
</div>
</div>

<script>
var DATA = {data_json};
var currentFilter = 'all';

function render() {{
    var container = document.getElementById('itemContainer');
    container.innerHTML = '';
    var search = document.getElementById('searchInput').value.toLowerCase();
    var moduleFilter = document.getElementById('moduleSelect').value;
    var sceneFilter = document.getElementById('sceneSelect').value;
    var count = 0;
    
    for (var i = 0; i < DATA.length; i++) {{
        var item = DATA[i];
        if (currentFilter !== 'all' && item.level !== currentFilter) continue;
        if (moduleFilter !== 'all' && item.module !== moduleFilter) continue;
        if (sceneFilter !== 'all' && item.scene !== sceneFilter) continue;
        if (search && (item.title + item.module + item.scene + item.detail).toLowerCase().indexOf(search) < 0) continue;
        
        count++;
        var levelClass = item.level.toLowerCase();
        var badgeClass = 'badge-' + (item.level === 'WARNING' ? 'warn' : item.level === 'NORMAL' ? 'norm' : levelClass);
        
        var html = '<div class="item ' + levelClass + '">';
        html += '<div class="meta"><span class="badge ' + badgeClass + '">' + item.level + '</span>';
        html += ' #' + item.id + ' · ' + item.module + ' · ' + item.scene + '</div>';
        html += '<h4>' + item.title + '</h4>';
        html += '<p>📌 期望: ' + item.expect + '</p>';
        html += '<p>🔍 实际: <code>' + item.actual + '</code></p>';
        if (item.detail) html += '<p>📝 ' + item.detail + '</p>';
        
        html += '<div class="opt">';
        if (item.fix_sql) html += '<button class="btn-sql" onclick="showPop(' + i + ',\\'sql\\')">复制SQL</button>';
        if (item.fix_cmd) html += '<button class="btn-cmd" onclick="showPop(' + i + ',\\'cmd\\')">复制CMD</button>';
        if (item.fix_file) html += '<button class="btn-file" onclick="showPop(' + i + ',\\'file\\')">复制文件</button>';
        html += '</div></div>';
        
        container.innerHTML += html;
    }}
    
    document.getElementById('resultCount').textContent = '显示 ' + count + ' / ' + DATA.length + ' 项';
}}

function setFilter(level, btn) {{
    currentFilter = level;
    document.querySelectorAll('.tab-btn').forEach(function(b) {{ b.classList.remove('active'); }});
    btn.classList.add('active');
    render();
}}

function doFilter() {{ render(); }}

function showPop(idx, type) {{
    var item = DATA[idx];
    var text = type === 'sql' ? item.fix_sql : type === 'cmd' ? item.fix_cmd : item.fix_file;
    document.getElementById('popText').value = text;
    document.getElementById('popMask').style.display = 'flex';
}}

function closePop() {{
    document.getElementById('popMask').style.display = 'none';
}}

function copyPopText() {{
    var textarea = document.getElementById('popText');
    textarea.select();
    document.execCommand('copy');
    alert('已复制到剪贴板');
}}

document.getElementById('popMask').onclick = function(e) {{
    if (e.target === this) closePop();
}};

render();
</script>
</body>
</html>'''
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html)

# ============================== 主程序 ==============================
def main():
    ts = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    ts_display = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    print('=' * 60)
    print('  又见炊烟餐饮系统智能体检 V4 超级版')
    print(f'  时间: {ts_display}')
    print('=' * 60)
    print(f'\n  数据库: {DB_CONFIG["database"]}@{DB_CONFIG["host"]}:{DB_CONFIG["port"]}')
    
    db = DB(DB_CONFIG)
    try:
        analyzer = Analyzer(db)
        findings, summary, tables = analyzer.analyze()
        
        print(f'\n  表: {summary["tables"]}  空表: {summary["empty_tables"]}')
        print(f'  FATAL:{summary["fatal"]}  ERROR:{summary["error"]}'
              f'  WARNING:{summary["warning"]}  INFO:{summary["info"]}  NORMAL:{summary["normal"]}')
        print(f'  总发现: {summary["total"]}')
        
        # 前端检查
        print('\n  [前端检查] 分析 frontend_v3 ...')
        project_root = os.path.dirname(os.path.dirname(SCRIPT_DIR))
        fe_findings = analyze_frontend(project_root)
        findings.extend(fe_findings)
        print(f'  前端检查项: {len(fe_findings)}')
        
        # 后端检查
        print('  [后端检查] 分析 banquet_project ...')
        be_findings = analyze_backend(project_root)
        findings.extend(be_findings)
        print(f'  后端检查项: {len(be_findings)}')
        
        # 更新summary
        summary['total'] = len(findings)
        summary['fatal'] = sum(1 for f in findings if f['level'] == 'FATAL')
        summary['error'] = sum(1 for f in findings if f['level'] == 'ERROR')
        summary['warning'] = sum(1 for f in findings if f['level'] == 'WARNING')
        summary['info'] = sum(1 for f in findings if f['level'] == 'INFO')
        summary['normal'] = sum(1 for f in findings if f['level'] == 'NORMAL')
        
        print(f'\n  合计总发现: {summary["total"]}')
        print(f'  FATAL:{summary["fatal"]}  ERROR:{summary["error"]}'
              f'  WARNING:{summary["warning"]}  INFO:{summary["info"]}  NORMAL:{summary["normal"]}')
        
        output_path = os.path.join(SCRIPT_DIR, f'system_checkup_v4_{ts}.html')
        generate_html(findings, summary, tables, output_path)
        print(f'\n  [OK] 报告已生成: {output_path}')
        
        latest_path = os.path.join(SCRIPT_DIR, 'system_checkup_v4_latest.html')
        generate_html(findings, summary, tables, latest_path)
        print(f'  [OK] 最新副本: {latest_path}')
        
    finally:
        db.close()
    
    print('\n' + '=' * 60)

if __name__ == '__main__':
    main()