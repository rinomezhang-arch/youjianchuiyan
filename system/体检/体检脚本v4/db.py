#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库连接与查询模块
使用 pymysql 替代 subprocess 调用 mysql CLI
"""
import pymysql
import pymysql.cursors
from typing import List, Dict, Optional, Tuple, Any
from dataclasses import dataclass
from config import DBConfig, get_db_config
import logging

logger = logging.getLogger("checkup.db")


@dataclass
class ColumnInfo:
    name: str
    type: str
    collation: str
    nullable: str
    key: str
    default: Optional[str]
    extra: str
    comment: str


@dataclass
class IndexInfo:
    name: str
    col: str
    unique: bool
    seq: int


@dataclass
class TableInfo:
    name: str
    rows: int
    comment: str
    collation: str
    engine: str
    size_kb: float


@dataclass
class ForeignKeyInfo:
    table: str
    column: str
    ref_table: str
    ref_column: str
    constraint_name: str


class Database:
    """数据库连接管理器"""
    
    def __init__(self, config: Optional[DBConfig] = None):
        self.config = config or get_db_config()
        self._conn: Optional[pymysql.Connection] = None
    
    def connect(self) -> None:
        """建立连接"""
        if self._conn is None or not self._conn.open:
            self._conn = pymysql.connect(
                host=self.config.host,
                port=self.config.port,
                user=self.config.user,
                password=self.config.password,
                database=self.config.database,
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor,
                connect_timeout=10,
            )
            logger.info(f"已连接 {self.config.database}@{self.config.host}:{self.config.port}")
    
    def close(self) -> None:
        """关闭连接"""
        if self._conn and self._conn.open:
            self._conn.close()
            self._conn = None
    
    def __enter__(self):
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
    
    def query(self, sql: str, params: Optional[Tuple] = None) -> List[Dict[str, Any]]:
        """执行查询，返回字典列表"""
        self.connect()
        with self._conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    
    def query_rows(self, sql: str, params: Optional[Tuple] = None) -> List[Tuple]:
        """执行查询，返回元组列表"""
        self.connect()
        with self._conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    
    def query_one(self, sql: str, params: Optional[Tuple] = None) -> Optional[Dict[str, Any]]:
        """执行查询，返回单行"""
        self.connect()
        with self._conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()
    
    def query_scalar(self, sql: str, params: Optional[Tuple] = None) -> Any:
        """执行查询，返回标量值"""
        self.connect()
        with self._conn.cursor() as cursor:
            cursor.execute(sql, params)
            row = cursor.fetchone()
            if row:
                return list(row.values())[0] if isinstance(row, dict) else row[0]
            return None
    
    def get_tables(self) -> Dict[str, TableInfo]:
        """获取所有表信息"""
        sql = """
            SELECT TABLE_NAME, TABLE_ROWS, TABLE_COMMENT, TABLE_COLLATION, ENGINE,
                   ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024, 1) AS size_kb
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
            ORDER BY TABLE_NAME
        """
        rows = self.query(sql)
        tables = {}
        for r in rows:
            tables[r['TABLE_NAME']] = TableInfo(
                name=r['TABLE_NAME'],
                rows=int(r['TABLE_ROWS'] or 0),
                comment=r['TABLE_COMMENT'] or '',
                collation=r['TABLE_COLLATION'] or '',
                engine=r['ENGINE'] or '',
                size_kb=float(r['size_kb'] or 0),
            )
        return tables
    
    def get_columns(self, table: str) -> List[ColumnInfo]:
        """获取表的列信息"""
        rows = self.query(f"SHOW FULL COLUMNS FROM `{table}`")
        return [
            ColumnInfo(
                name=r['Field'],
                type=r['Type'],
                collation=r['Collation'] or '',
                nullable=r['Null'],
                key=r['Key'],
                default=r['Default'],
                extra=r['Extra'],
                comment=r['Comment'] or '',
            )
            for r in rows
        ]
    
    def get_pk_columns(self, table: str) -> List[str]:
        """获取主键列名"""
        cols = self.get_columns(table)
        return [c.name for c in cols if c.key == 'PRI']
    
    def get_indexes(self, table: str) -> List[IndexInfo]:
        """获取索引信息"""
        rows = self.query(f"SHOW INDEX FROM `{table}`")
        return [
            IndexInfo(
                name=r['Key_name'],
                col=r['Column_name'],
                unique=r['Non_unique'] == 0,
                seq=r['Seq_in_index'],
            )
            for r in rows
        ]
    
    def get_foreign_keys(self, table: Optional[str] = None) -> List[ForeignKeyInfo]:
        """获取真实外键约束"""
        sql = """
            SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME, CONSTRAINT_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE()
              AND REFERENCED_TABLE_NAME IS NOT NULL
        """
        if table:
            sql += f" AND TABLE_NAME = '{table}'"
        rows = self.query(sql)
        return [
            ForeignKeyInfo(
                table=r['TABLE_NAME'],
                column=r['COLUMN_NAME'],
                ref_table=r['REFERENCED_TABLE_NAME'],
                ref_column=r['REFERENCED_COLUMN_NAME'],
                constraint_name=r['CONSTRAINT_NAME'],
            )
            for r in rows
        ]
    
    def count_orphans(self, table: str, column: str, ref_table: str, ref_column: str) -> int:
        """检查孤儿记录数量"""
        sql = (
            f"SELECT COUNT(*) AS cnt FROM `{table}` t1 "
            f"LEFT JOIN `{ref_table}` t2 ON t1.`{column}` = t2.`{ref_column}` "
            f"WHERE t1.`{column}` IS NOT NULL AND t2.`{ref_column}` IS NULL"
        )
        result = self.query_one(sql)
        return int(result['cnt']) if result else 0
