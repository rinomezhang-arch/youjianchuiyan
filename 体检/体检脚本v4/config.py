#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
体检脚本配置模块
从 .env 文件加载数据库配置
"""
import os
from typing import Dict, Optional
from dataclasses import dataclass

@dataclass
class DBConfig:
    host: str
    port: int
    user: str
    password: str
    database: str

def load_env(env_path: Optional[str] = None) -> None:
    """加载 .env 文件到环境变量"""
    if env_path is None:
        env_path = os.path.join(os.path.dirname(__file__), '.env')
    
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

def get_db_config() -> DBConfig:
    """从环境变量获取数据库配置"""
    load_env()
    return DBConfig(
        host=os.environ.get('DB_HOST', 'localhost'),
        port=int(os.environ.get('DB_PORT', '3306')),
        user=os.environ.get('DB_USER', 'root'),
        password=os.environ.get('DB_PASS', ''),
        database=os.environ.get('DB_NAME', 'banquet'),
    )

def get_project_root() -> str:
    """获取项目根目录"""
    return os.environ.get('PROJECT_ROOT', os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
