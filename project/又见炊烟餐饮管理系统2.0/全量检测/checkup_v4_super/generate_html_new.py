# ============================== HTML 报告生成 ==============================
def generate_html(findings, summary, tables, output_path):
    data_json = json.dumps(findings, ensure_ascii=False)
    modules = sorted(set(f['module'] for f in findings))
    scenes = sorted(set(f['scene'] for f in findings))
    
    # 生成按钮组（替代下拉选择）
    module_buttons = '\n'.join(
        f'<button class="btn-filter" data-module="{m}" onclick="setModuleFilter(\'{m}\',this)">{m}</button>'
        for m in modules
    )
    scene_buttons = '\n'.join(
        f'<button class="btn-filter" data-scene="{s}" onclick="setSceneFilter(\'{s}\',this)">{s}</button>'
        for s in scenes
    )
    
    # 表清单行 - 修复乱码
    def sanitize_comment(comment):
        if not comment:
            return ''
        # 如果包含连续??，尝试修复编码
        if '??' in comment:
            try:
                fixed = comment.encode('latin-1').decode('utf-8')
                return fixed
            except:
                pass
        return comment
    
    table_rows = ''
    for t in sorted(tables.values(), key=lambda x: x['rows'], reverse=True):
        comment = sanitize_comment(t.get('comment', ''))
        table_rows += f'<tr><td>{t["name"]}</td><td style="text-align:right">{t["rows"]}</td><td>{comment}</td><td>{t["engine"]}</td><td>{t["collation"]}</td></tr>\n'
    
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
.filter-section{{margin-bottom:16px;padding:16px;background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;}}
.filter-section label{{font-size:14px;font-weight:600;color:#475569;white-space:nowrap;margin-right:8px;display:inline-block;min-width:60px;}}
.filter-row{{display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:12px;}}
.filter-row:last-child{{margin-bottom:0;}}
.btn-filter{{padding:6px 14px;border:2px solid #cbd5e1;border-radius:6px;background:#fff;color:#475569;font-size:13px;cursor:pointer;transition:all 0.2s;white-space:nowrap;}}
.btn-filter:hover{{border-color:#2563eb;color:#2563eb;transform:translateY(-1px);}}
.btn-filter.active{{background:#2563eb;color:#fff;border-color:#2563eb;font-weight:600;}}
.search-input{{padding:8px 14px;border:2px solid #cbd5e1;border-radius:8px;font-size:14px;transition:all 0.2s;width:100%;max-width:400px;}}
.search-input:focus{{border-color:#2563eb;outline:none;box-shadow:0 0 0 3px rgba(37,99,235,0.1);}}
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
#itemContainer{{display:flex;flex-direction:column;gap:12px;max-height:70vh;overflow-y:auto;padding:8px;}}
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
.current-filter{{font-size:13px;color:#2563eb;padding:6px 12px;background:#eff6ff;border-radius:6px;margin-bottom:10px;display:inline-block;}}
.current-filter span{{font-weight:600;}}
.reset-btn{{padding:4px 12px;border:1px solid #ef4444;border-radius:4px;background:#fef2f2;color:#ef4444;font-size:12px;cursor:pointer;margin-left:8px;}}
.reset-btn:hover{{background:#ef4444;color:#fff;}}
@media print{{
 .filter-section,.tab-bar,.opt,#popMask,.result-count,.current-filter{{display:none !important;}}
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

<div class="filter-section">
<div class="filter-row">
<label>🔍 搜索</label>
<input type="text" class="search-input" id="searchInput" placeholder="输入关键字..." oninput="doFilter()">
</div>
<div class="filter-row">
<label>📁 类型</label>
<button class="btn-filter active" data-module="all" onclick="setModuleFilter('all',this)">全部</button>
{module_buttons}
</div>
<div class="filter-row">
<label>🎯 场景</label>
<button class="btn-filter active" data-scene="all" onclick="setSceneFilter('all',this)">全部</button>
{scene_buttons}
</div>
</div>

<div class="tab-bar">
<button class="tab-btn tab-all" data-filter="all" onclick="setFilter('all',this)">全部({summary['total']})</button>
<button class="tab-btn tab-fatal" data-filter="FATAL" onclick="setFilter('FATAL',this)">致命({summary['fatal']})</button>
<button class="tab-btn tab-error" data-filter="ERROR" onclick="setFilter('ERROR',this)">严重({summary['error']})</button>
<button class="tab-btn tab-warn" data-filter="WARNING" onclick="setFilter('WARNING',this)">警告({summary['warning']})</button>
<button class="tab-btn tab-info" data-filter="INFO" onclick="setFilter('INFO',this)">提示({summary['info']})</button>
<button class="tab-btn tab-normal" data-filter="NORMAL" onclick="setFilter('NORMAL',this)">正常({summary['normal']})</button>
</div>

<div class="current-filter" id="currentFilterDisplay" style="display:none;">
当前筛选：<span id="filterDetail"></span>
<button class="reset-btn" onclick="resetAll()">重置全部</button>
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
var currentModule = 'all';
var currentScene = 'all';

function render() {{
    var container = document.getElementById('itemContainer');
    var itemsHtml = [];
    var search = document.getElementById('searchInput').value.toLowerCase();
    var count = 0;
    
    for (var i = 0; i < DATA.length; i++) {{
        var item = DATA[i];
        if (currentFilter !== 'all' && item.level !== currentFilter) continue;
        if (currentModule !== 'all' && item.module !== currentModule) continue;
        if (currentScene !== 'all' && item.scene !== currentScene) continue;
        if (search) {{
            var haystack = (item.title + ' ' + item.module + ' ' + item.scene + ' ' + item.detail + ' ' + item.expect + ' ' + item.actual).toLowerCase();
            if (haystack.indexOf(search) < 0) continue;
        }}
        
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
        if (item.fix_sql) html += '<button class="btn-sql" onclick="showPop(' + i + ',\'sql\')">复制SQL</button>';
        if (item.fix_cmd) html += '<button class="btn-cmd" onclick="showPop(' + i + ',\'cmd\')">复制CMD</button>';
        if (item.fix_file) html += '<button class="btn-file" onclick="showPop(' + i + ',\'file\')">复制文件</button>';
        html += '</div></div>';
        
        itemsHtml.push(html);
    }}
    
    container.innerHTML = itemsHtml.join('');
    document.getElementById('resultCount').textContent = '显示 ' + count + ' / ' + DATA.length + ' 项';
    
    updateFilterDisplay();
}}

function setFilter(level, btn) {{
    currentFilter = level;
    document.querySelectorAll('.tab-btn').forEach(function(b) {{ b.classList.remove('active'); }});
    btn.classList.add('active');
    render();
}}

function setModuleFilter(module, btn) {{
    currentModule = module;
    var parent = btn.parentElement;
    parent.querySelectorAll('.btn-filter').forEach(function(b) {{ b.classList.remove('active'); }});
    btn.classList.add('active');
    render();
}}

function setSceneFilter(scene, btn) {{
    currentScene = scene;
    var parent = btn.parentElement;
    parent.querySelectorAll('.btn-filter').forEach(function(b) {{ b.classList.remove('active'); }});
    btn.classList.add('active');
    render();
}}

function doFilter() {{ render(); }}

function resetAll() {{
    currentFilter = 'all';
    currentModule = 'all';
    currentScene = 'all';
    document.getElementById('searchInput').value = '';
    
    document.querySelectorAll('.tab-btn').forEach(function(b) {{ b.classList.remove('active'); }});
    document.querySelector('.tab-btn[data-filter="all"]').classList.add('active');
    
    document.querySelectorAll('.btn-filter').forEach(function(b) {{ b.classList.remove('active'); }});
    document.querySelectorAll('.btn-filter[data-module="all"]').forEach(function(b) {{ b.classList.add('active'); }});
    document.querySelectorAll('.btn-filter[data-scene="all"]').forEach(function(b) {{ b.classList.add('active'); }});
    
    render();
}}

function updateFilterDisplay() {{
    var parts = [];
    if (currentFilter !== 'all') parts.push('级别=' + currentFilter);
    if (currentModule !== 'all') parts.push('类型=' + currentModule);
    if (currentScene !== 'all') parts.push('场景=' + currentScene);
    
    var display = document.getElementById('currentFilterDisplay');
    var detail = document.getElementById('filterDetail');
    
    if (parts.length > 0) {{
        detail.textContent = parts.join(' + ');
        display.style.display = 'inline-block';
    }} else {{
        display.style.display = 'none';
    }}
}}

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
    if (navigator.clipboard && navigator.clipboard.writeText) {{
        navigator.clipboard.writeText(textarea.value).then(function() {{
            alert('已复制到剪贴板');
        }}).catch(function() {{
            document.execCommand('copy');
            alert('已复制到剪贴板');
        }});
    }} else {{
        document.execCommand('copy');
        alert('已复制到剪贴板');
    }}
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
