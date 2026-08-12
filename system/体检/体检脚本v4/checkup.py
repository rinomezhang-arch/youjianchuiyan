#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
又见炊烟餐饮管理系统 — 一键全量体检 v4
模块化: config / db / analyzer / report
输出: reports/ 目录下 HTML + Markdown + JSON + CSV + TXT
"""
import os
import sys
import logging
import datetime

# 确保能导入同目录模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config import get_db_config, get_project_root
from db import Database
from analyzer import Analyzer
from report import ReportGenerator

# ── 日志 ──
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(name)s %(levelname)s: %(message)s',
    datefmt='%H:%M:%S',
    stream=sys.stderr,
)
logger = logging.getLogger("checkup")


def main():
    ts = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    ts_display = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, 'reports')
    
    print('=' * 60)
    print('  又见炊烟餐饮系统智能体检 v4')
    print(f'  时间: {ts_display}')
    print(f'  输出: {output_dir}')
    print('=' * 60)
    
    # 1. 连接数据库
    config = get_db_config()
    print(f'\n  数据库: {config.database}@{config.host}:{config.port}')
    
    try:
        with Database(config) as db:
            # 2. 分析
            print('\n  正在扫描数据库结构...')
            analyzer = Analyzer(db)
            findings, summary = analyzer.analyze()
            
            # 3. 打印摘要
            print(f'\n  表: {summary["tables"]}  空表: {summary["empty_tables"]}')
            print(f'  FATAL:{summary["fatal"]}  ERROR:{summary["error"]}'
                  f'  WARNING:{summary["warning"]}  INFO:{summary["info"]}')
            print(f'  总发现: {summary["total_findings"]}')
            
            # 4. 生成报告
            print(f'\n  正在生成报告...')
            tables = db.get_tables()
            gen = ReportGenerator(output_dir, ts)
            paths = gen.generate_all(findings, summary, tables)
            
            for p in paths:
                print(f'  [OK] {os.path.basename(p)}')
            
            # 5. 同时生成 latest 链接（复制最新一份）
            import shutil
            html_src = os.path.join(output_dir, f'report_{ts}.html')
            html_latest = os.path.join(output_dir, 'report_latest.html')
            if os.path.exists(html_src):
                shutil.copy2(html_src, html_latest)
                print(f'  [OK] report_latest.html (最新副本)')
            
            print(f'\n  报告目录: {output_dir}')
            print('=' * 60)
    
    except Exception as e:
        logger.error(f"体检失败: {e}", exc_info=True)
        print(f'\n   体检失败: {e}', file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
