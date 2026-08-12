#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
报告生成模块
支持 HTML / Markdown / JSON / CSV 多格式输出
"""
import os
import json
import csv
import datetime
from typing import List, Dict
from analyzer import Finding
from db import TableInfo


def _esc(s: str) -> str:
    """HTML 转义"""
    return (s
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;'))


class ReportGenerator:
    """多格式报告生成器"""
    
    def __init__(self, output_dir: str, timestamp: str):
        self.output_dir = output_dir
        self.timestamp = timestamp
        os.makedirs(output_dir, exist_ok=True)
    
    def generate_all(self, findings: List[Finding], summary: Dict, tables: Dict[str, TableInfo]) -> List[str]:
        """生成所有格式报告，返回文件路径列表"""
        paths = []
        paths.append(self.generate_html(findings, summary, tables))
        paths.append(self.generate_markdown(findings, summary, tables))
        paths.append(self.generate_json(findings, summary, tables))
        paths.append(self.generate_csv(findings))
        paths.append(self.generate_summary_txt(findings, summary, tables))
        return paths
    
    def generate_html(self, findings: List[Finding], summary: Dict, tables: Dict[str, TableInfo]) -> str:
        """生成交互式 HTML 报告"""
        colors = {'FATAL': '#f5222d', 'ERROR': '#fa8c16', 'WARNING': '#d49200', 'INFO': '#1890ff'}
        bg = {'FATAL': '#fff5f5', 'ERROR': '#fffaf0', 'WARNING': '#fffde7', 'INFO': '#f0f9ff'}
        
        items_html = []
        for f in findings:
            lv = f.level
            title_e = _esc(f.title)
            expect_e = _esc(f.expect)
            actual_e = _esc(f.actual)
            detail_e = _esc(f.detail)
            fix_e = _esc(f.fix)
            module_e = _esc(f.module)
            
            detail_html = '<p>📝 ' + detail_e + '</p>' if detail_e else ''
            fix_html = '<p class="fix">🔧 ' + fix_e + '</p>' if fix_e else ''
            
            item = (
                '<div class="item ' + lv + '" style="background:' + bg[lv] + ';border-color:' + colors[lv] + ';">'
                '<div class="meta"><span class="badge" style="background:' + colors[lv] + ';">' + lv + '</span>'
                ' <b>' + module_e + '</b></div>'
                '<h4>' + title_e + '</h4>'
                '<p> 期望: ' + expect_e + '</p>'
                '<p>🔍 实际: <code>' + actual_e + '</code></p>'
                + detail_html
                + fix_html
                + '</div>'
            )
            items_html.append(item)
        
        table_rows = ''
        for t in sorted(tables.values(), key=lambda x: x.rows, reverse=True):
            table_rows += (
                '<tr><td>' + _esc(t.name) + '</td>'
                '<td style="text-align:right">' + str(t.rows) + '</td>'
                '<td>' + _esc(t.comment) + '</td>'
                '<td>' + _esc(t.engine) + '</td>'
                '<td>' + _esc(t.collation) + '</td></tr>'
            )
        
        items_joined = ''.join(items_html)
        
        html = (
            '<!DOCTYPE html>\n'
            '<html lang="zh-CN">\n'
            '<head>\n'
            '<meta charset="UTF-8">\n'
            '<meta name="viewport" content="width=device-width,initial-scale=1">\n'
            '<title>餐饮系统体检报告 - ' + self.timestamp + '</title>\n'
            '<style>\n'
            '*{margin:0;padding:0;box-sizing:border-box;font-family:"Microsoft YaHei","PingFang SC",sans-serif;}\n'
            'body{padding:20px;background:#f5f7fa;color:#333;}\n'
            '.wrap{max-width:1400px;margin:0 auto;background:#fff;padding:28px;border-radius:12px;box-shadow:0 2px 16px rgba(0,0,0,.06);}\n'
            'h1{text-align:center;font-size:26px;margin-bottom:6px;color:#1a1a1a;}\n'
            'h1 small{color:#999;font-size:14px;font-weight:normal;}\n'
            'h3{font-size:16px;margin:20px 0 10px;color:#444;border-bottom:2px solid #eee;padding-bottom:6px;}\n'
            '.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin:20px 0;}\n'
            '.card{padding:16px;border-radius:10px;text-align:center;transition:transform .15s;}\n'
            '.card:hover{transform:translateY(-2px);}\n'
            '.card .n{font-size:30px;font-weight:bold;} .card .l{font-size:12px;color:#666;margin-top:4px;}\n'
            '.cf{background:#eff6ff;color:#2563eb;} .cr{background:#fff5f5;color:#f5222d;}\n'
            '.co{background:#fffaf0;color:#fa8c16;} .cy{background:#fffde7;color:#854d0e;}\n'
            '.cg{background:#f6ffed;color:#389e0d;} .cx{background:#f5f5f5;color:#666;}\n'
            '.tbar{display:flex;gap:8px;margin:14px 0;flex-wrap:wrap;align-items:center;}\n'
            '.tbtn{padding:8px 16px;border:none;border-radius:6px;font-size:13px;cursor:pointer;background:#e5e7eb;transition:all .15s;}\n'
            '.tbtn.f{background:#fee2e2;color:#c41e3a;} .tbtn.e{background:#ffedd5;color:#c2410c;}\n'
            '.tbtn.w{background:#fff9cc;color:#854d0e;} .tbtn.i{background:#dbeafe;color:#1e40af;}\n'
            '.tbtn:hover{opacity:.85;transform:scale(1.03);} .tbtn.active{outline:3px solid #2563eb;font-weight:bold;}\n'
            '#box{display:flex;flex-direction:column;gap:8px;max-height:750px;overflow-y:auto;padding:6px;}\n'
            '.item{padding:14px;border-radius:8px;border:2px solid;transition:box-shadow .15s;}\n'
            '.item:hover{box-shadow:0 2px 8px rgba(0,0,0,.08);}\n'
            '.item h4{font-size:14px;margin-bottom:5px;color:#222;}\n'
            '.item p{font-size:12px;margin:3px 0;color:#555;} .item .meta{font-size:11px;color:#999;}\n'
            '.item .badge{font-size:10px;padding:2px 8px;border-radius:10px;color:#fff;margin-right:6px;}\n'
            '.item code{background:rgba(0,0,0,.06);padding:1px 5px;border-radius:3px;font-size:12px;}\n'
            '.item .fix{color:#2563eb;background:#eff6ff;padding:8px 10px;border-radius:5px;font-family:"Cascadia Code",monospace;font-size:12px;margin-top:8px;}\n'
            'table{width:100%;border-collapse:collapse;font-size:12px;margin:12px 0;}\n'
            'table th,td{border:1px solid #e0e0e0;padding:6px 10px;text-align:left;}\n'
            'table th{background:#f5f7fa;font-weight:600;} tr:nth-child(even){background:#fafafa;}\n'
            'input[type=text]{padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;width:280px;font-size:13px;outline:none;}\n'
            'input[type=text]:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.1);}\n'
            '.search-bar{display:flex;gap:10px;align-items:center;flex-wrap:wrap;}\n'
            '</style>\n'
            '</head>\n'
            '<body>\n'
            '<div class="wrap">\n'
            '<h1>🍽️ 又见炊烟餐饮系统体检报告<br><small>' + self.timestamp + ' · 纯数据库分析 · ' + str(summary['tables']) + '张表</small></h1>\n'
            '\n'
            '<div class="grid">\n'
            '<div class="card cf"><div class="n">' + str(summary['tables']) + '</div><div class="l">数据库表</div></div>\n'
            '<div class="card cx"><div class="n">' + str(summary['empty_tables']) + '</div><div class="l">空表</div></div>\n'
            '<div class="card cr"><div class="n">' + str(summary['fatal']) + '</div><div class="l">FATAL</div></div>\n'
            '<div class="card co"><div class="n">' + str(summary['error']) + '</div><div class="l">ERROR</div></div>\n'
            '<div class="card cy"><div class="n">' + str(summary['warning']) + '</div><div class="l">WARNING</div></div>\n'
            '<div class="card cg"><div class="n">' + str(summary['info']) + '</div><div class="l">INFO</div></div>\n'
            '<div class="card cx"><div class="n">' + str(summary['total_findings']) + '</div><div class="l">总发现</div></div>\n'
            '</div>\n'
            '\n'
            '<h3> 发现列表</h3>\n'
            '<div class="search-bar">\n'
            '<input type="text" id="s" placeholder="搜索关键字 (编号/标题/模块/场景)..." oninput="f()">\n'
            '</div>\n'
            '<div class="tbar">\n'
            '<button class="tbtn active" onclick="fl(\'ALL\',this)">全部(' + str(summary['total_findings']) + ')</button>\n'
            '<button class="tbtn f" onclick="fl(\'FATAL\',this)">FATAL(' + str(summary['fatal']) + ')</button>\n'
            '<button class="tbtn e" onclick="fl(\'ERROR\',this)">ERROR(' + str(summary['error']) + ')</button>\n'
            '<button class="tbtn w" onclick="fl(\'WARNING\',this)">WARNING(' + str(summary['warning']) + ')</button>\n'
            '<button class="tbtn i" onclick="fl(\'INFO\',this)">INFO(' + str(summary['info']) + ')</button>\n'
            '</div>\n'
            '<div id="box">\n'
            + items_joined + '\n'
            '</div>\n'
            '\n'
            '<h3>📋 表清单 (' + str(len(tables)) + '张)</h3>\n'
            '<table>\n'
            '<tr><th>表名</th><th style="text-align:right">行数</th><th>注释</th><th>引擎</th><th>字符集</th></tr>\n'
            + table_rows + '\n'
            '</table>\n'
            '</div>\n'
            '\n'
            '<script>\n'
            'var ct=\'ALL\';\n'
            'function fl(t,b){ct=t;document.querySelectorAll(\'.tbtn\').forEach(function(x){x.classList.remove(\'active\');});b.classList.add(\'active\');f();}\n'
            'function f(){\n'
            ' var q=document.getElementById(\'s\').value.toLowerCase();\n'
            ' var its=document.querySelectorAll(\'#box .item\');var n=0;\n'
            ' its.forEach(function(i){\n'
            '  var lv=i.className.split(\' \').pop();\n'
            '  var ok=(ct===\'ALL\'||lv===ct)&&(q===\'\'||i.textContent.toLowerCase().indexOf(q)>=0);\n'
            '  i.style.display=ok?\'block\':\'none\';if(ok)n++;\n'
            ' });\n'
            '}\n'
            '</script>\n'
            '</body>\n'
            '</html>'
        )
        
        path = os.path.join(self.output_dir, 'report_' + self.timestamp + '.html')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(html)
        return path
    
    def generate_markdown(self, findings: List[Finding], summary: Dict, tables: Dict[str, TableInfo]) -> str:
        """生成 Markdown 报告"""
        lines = [
            '# 餐饮系统体检报告',
            '',
            '**时间**: ' + self.timestamp,
            '**数据库**: ' + str(summary['tables']) + '张表 | 空表: ' + str(summary['empty_tables']),
            '',
            '## 统计',
            '',
            '| 级别 | 数量 |',
            '|------|------|',
            '| 🔴 FATAL | ' + str(summary['fatal']) + ' |',
            '|  ERROR | ' + str(summary['error']) + ' |',
            '| 🟡 WARNING | ' + str(summary['warning']) + ' |',
            '| 🔵 INFO | ' + str(summary['info']) + ' |',
            '| **总计** | **' + str(summary['total_findings']) + '** |',
            '',
        ]
        
        for level in ['FATAL', 'ERROR', 'WARNING', 'INFO']:
            items = [f for f in findings if f.level == level]
            if not items:
                continue
            emoji = {'FATAL': '🔴', 'ERROR': '', 'WARNING': '🟡', 'INFO': '🔵'}[level]
            lines.append('## ' + emoji + ' ' + level + ' (' + str(len(items)) + ')')
            lines.append('')
            for fi in items:
                lines.append('### ' + fi.title)
                lines.append('- **模块**: ' + fi.module)
                lines.append('- **期望**: ' + fi.expect)
                lines.append('- **实际**: `' + fi.actual + '`')
                if fi.detail:
                    lines.append('- **说明**: ' + fi.detail)
                if fi.fix:
                    lines.append('- **修复**: `' + fi.fix + '`')
                lines.append('')
        
        lines.append('## 📋 表清单')
        lines.append('')
        lines.append('| 表名 | 行数 | 注释 | 引擎 | 字符集 |')
        lines.append('|------|------|------|------|--------|')
        for t in sorted(tables.values(), key=lambda x: x.rows, reverse=True):
            lines.append('| ' + t.name + ' | ' + str(t.rows) + ' | ' + t.comment + ' | ' + t.engine + ' | ' + t.collation + ' |')
        
        path = os.path.join(self.output_dir, 'report_' + self.timestamp + '.md')
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        return path
    
    def generate_json(self, findings: List[Finding], summary: Dict, tables: Dict[str, TableInfo]) -> str:
        """生成 JSON 数据"""
        data = {
            'timestamp': self.timestamp,
            'summary': summary,
            'findings': [
                {
                    'level': f.level,
                    'module': f.module,
                    'title': f.title,
                    'expect': f.expect,
                    'actual': f.actual,
                    'detail': f.detail,
                    'fix': f.fix,
                }
                for f in findings
            ],
            'tables': {
                name: {
                    'rows': t.rows,
                    'comment': t.comment,
                    'engine': t.engine,
                    'collation': t.collation,
                    'size_kb': t.size_kb,
                }
                for name, t in tables.items()
            },
        }
        path = os.path.join(self.output_dir, 'data_' + self.timestamp + '.json')
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        return path
    
    def generate_csv(self, findings: List[Finding]) -> str:
        """生成 CSV 表格"""
        path = os.path.join(self.output_dir, 'findings_' + self.timestamp + '.csv')
        with open(path, 'w', encoding='utf-8-sig', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['级别', '模块', '标题', '期望', '实际', '说明', '修复建议'])
            for fi in findings:
                writer.writerow([fi.level, fi.module, fi.title,
                                fi.expect, fi.actual, fi.detail, fi.fix])
        return path
    
    def generate_summary_txt(self, findings: List[Finding], summary: Dict, tables: Dict[str, TableInfo]) -> str:
        """生成纯文本摘要"""
        lines = [
            '=' * 60,
            '  又见炊烟餐饮系统体检摘要',
            '=' * 60,
            '  时间: ' + self.timestamp,
            '  数据库表: ' + str(summary['tables']),
            '  空表: ' + str(summary['empty_tables']),
            '  FATAL: ' + str(summary['fatal']) + '  ERROR: ' + str(summary['error']),
            '  WARNING: ' + str(summary['warning']) + '  INFO: ' + str(summary['info']),
            '  总发现: ' + str(summary['total_findings']),
            '=' * 60,
            '',
            '── FATAL ─',
        ]
        for fi in findings:
            if fi.level == 'FATAL':
                lines.append('  [' + fi.module + '] ' + fi.title)
                if fi.fix:
                    lines.append('    → ' + fi.fix)
        
        lines.append('')
        lines.append('── ERROR ──')
        for fi in findings:
            if fi.level == 'ERROR':
                lines.append('  [' + fi.module + '] ' + fi.title)
                if fi.fix:
                    lines.append('    → ' + fi.fix)
        
        path = os.path.join(self.output_dir, 'summary_' + self.timestamp + '.txt')
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        return path
