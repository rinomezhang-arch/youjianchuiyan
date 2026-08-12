"""
纯标准库生成 xlsx（无依赖）
严格遵循 OOXML 元素顺序，确保 Excel/WPS 可打开
用途：生成"角色-权限-菜单对照表.xlsx"
数据来源：Dashboard.vue 实际菜单结构 + 角色权限矩阵
角色：管理员、老板、店长、服务员、厨师长、收银员、财务、采购、库管（9 个）
图例：√=可见可操作  △=只读  ×=无权限
"""
import os
import zipfile
from xml.sax.saxutils import escape

OUTPUT = r"f:\solo\project\又见炊烟餐饮管理系统2.0\又见炊烟餐饮管理系统 2.0 全套开发交付文档集\05_权限审批流\角色-权限-菜单对照表.xlsx"

# =====================================================================
# 角色顺序（9 个）
# =====================================================================
ROLES = ["管理员", "老板", "店长", "服务员", "厨师长", "收银员", "财务", "采购", "库管"]

# =====================================================================
# 菜单数据：每行为 (菜单路径, 菜单名, 9 个角色权限)
# 权限值：√=可见可操作  △=只读  ×=无权限
# 菜单来源：frontend_v3/src/views/Dashboard.vue allModulePages
# =====================================================================
# 列顺序：管理员 老板 店长 服务员 厨师长 收银员 财务 采购 库管
MENUS = [
    # 核心菜单
    ("/dashboard/home",                "工作台",       "√","√","√","√","√","√","√","√","√"),
    ("/dashboard/table-board",         "桌台看板",     "√","√","√","√","△","√","△","×","×"),

    # 前厅运营模块
    ("/dashboard/front-office",        "前厅运营",     "√","√","√","△","×","△","△","×","×"),
    ("/dashboard/front-desk",          "前台预定",     "√","△","√","√","×","△","×","×","×"),
    ("/dashboard/guest-analysis",      "客人分析",     "√","√","√","△","×","△","△","×","×"),
    ("/dashboard/staff-performance",   "员工绩效",     "√","√","√","×","△","×","△","×","×"),
    ("/dashboard/table-utilization",   "桌台利用率",   "√","√","√","△","×","△","△","×","×"),
    ("/dashboard/report-print",        "报表打印",     "√","√","√","△","△","△","√","×","×"),
    ("/dashboard/bookings",            "预订管理",     "√","△","√","√","×","△","×","×","×"),
    ("/dashboard/customers",           "客户管理",     "√","√","√","√","×","△","△","×","×"),
    ("/dashboard/table-layout",        "台型设计",     "√","△","√","×","×","×","×","×","×"),
    ("/dashboard/art-design",          "美工设计",     "√","△","△","×","×","×","×","×","×"),

    # 菜单管理模块（11 个子模块）
    ("/dashboard/menu",                "菜单管理",     "√","△","√","△","√","×","△","△","△"),
    ("/dashboard/ordering",            "点菜",         "√","×","△","√","△","×","×","×","×"),
    ("/dashboard/dish-library",        "菜库编辑",     "√","△","√","×","△","×","×","×","×"),
    ("/dashboard/cost-recipe",         "成本配方",     "√","√","△","×","√","×","√","△","△"),
    ("/dashboard/set-menu",            "套餐管理",     "√","△","√","×","△","×","△","×","×"),
    ("/dashboard/pricing",             "调价管理",     "√","√","△","×","×","×","△","×","×"),
    ("/dashboard/sold-out",            "沽清管控",     "√","△","√","△","√","×","×","×","×"),
    ("/dashboard/tags",                "标签管理",     "√","△","√","×","△","×","×","×","×"),
    ("/dashboard/print-config",        "打印配置",     "√","△","√","×","×","△","×","×","×"),
    ("/dashboard/store-permission",    "门店权限",     "√","√","△","×","×","×","×","×","×"),
    ("/dashboard/audit-log",           "操作日志",     "√","√","△","×","×","×","△","×","×"),
    ("/dashboard/price-tiers",         "多价格体系",   "√","√","△","×","×","×","△","×","×"),

    # 厨房管理模块
    ("/dashboard/kitchen",             "厨房管理",     "√","△","√","×","√","×","×","×","×"),
    ("/dashboard/kitchen-log",         "后厨日志",     "√","△","√","×","√","×","×","×","×"),
    ("/dashboard/production",          "出品管理",     "√","△","△","×","√","×","×","×","×"),
    ("/dashboard/packages",            "套餐(厨房)",   "√","△","△","×","√","×","×","×","×"),

    # 采购仓储模块
    ("/dashboard/supply-chain",        "采购仓储",     "√","△","√","×","×","×","△","√","√"),
    ("/dashboard/inventory",           "库存管理",     "√","△","√","×","△","×","△","△","√"),
    ("/dashboard/procurement",         "采购管理",     "√","△","△","×","×","×","△","√","△"),
    ("/dashboard/receipt",             "入库验收",     "√","△","△","×","×","×","△","△","√"),
    ("/dashboard/issue",               "领用出库",     "√","△","△","×","△","×","×","×","√"),
    ("/dashboard/supplier-reconciliation","供应商对账","√","√","△","×","×","×","√","△","△"),
    ("/dashboard/stock-take",          "盘点",         "√","△","√","×","×","×","△","×","√"),
    ("/dashboard/suppliers",           "供应商",       "√","△","△","×","×","×","△","√","△"),

    # 营销会员模块
    ("/dashboard/marketing",           "营销会员",     "√","√","√","△","×","△","△","×","×"),

    # 人事行政模块
    ("/dashboard/hr-admin",            "人事行政",     "√","√","√","×","×","×","×","×","×"),
    ("/dashboard/staff",               "员工档案",     "√","△","√","△","△","×","×","×","×"),
    ("/dashboard/attendance-calendar", "考勤日历",     "√","△","√","△","△","×","×","×","×"),
    ("/dashboard/payroll",             "工资管理",     "√","√","△","×","×","×","√","×","×"),
    ("/dashboard/schedule",            "排班管理",     "√","△","√","△","△","×","×","×","×"),
    ("/dashboard/leave",               "请假管理",     "√","△","√","△","△","×","×","×","×"),

    # 财务数据模块
    ("/dashboard/finance",             "财务数据",     "√","√","△","×","×","△","√","×","×"),
    ("/dashboard/finance/dish-cost",   "菜品成本",     "√","√","△","×","△","×","√","×","×"),
    ("/dashboard/reports",             "数据报表",     "√","√","√","×","×","△","√","×","×"),
    ("/dashboard/dish-cost-analysis",  "菜品成本分析", "√","√","△","×","△","×","√","×","×"),

    # 总经办 / 系统模块
    ("/dashboard/gm-office",           "总经办",       "√","√","△","×","×","×","×","×","×"),
    ("/dashboard/approval",            "审批中心",     "√","√","√","×","×","×","√","×","×"),
    ("/dashboard/bill-manage",         "账单管理",     "√","△","√","×","×","√","√","×","×"),
    ("/dashboard/ipad-menu",           "iPad点菜",     "√","△","√","√","△","×","×","×","×"),
    ("/dashboard/system-checkup",      "系统体检",     "√","△","△","×","×","×","×","×","×"),

    # 系统设置 / 数据大屏 / 工程管理
    ("/dashboard/settings",            "系统设置",     "√","△","△","×","×","×","×","×","×"),
    ("/dashboard/data-screen",         "数据大屏",     "√","√","√","△","△","△","△","×","×"),
    ("/dashboard/engineering",         "工程管理",     "√","△","√","×","×","×","×","×","×"),
]

HEADERS = ["菜单路径", "菜单名"] + ROLES

# 列宽（字符数）：路径列宽，菜单名列，9 个角色列窄
COL_WIDTHS = [34, 18, 9, 9, 9, 9, 9, 9, 9, 9, 9]

# 图例统计行后缀说明
LEGEND = "图例：√=可见可操作  △=只读  ×=无权限"


def col_letter(idx):
    """1-based index to Excel column letter"""
    s = ""
    while idx > 0:
        idx, r = divmod(idx - 1, 26)
        s = chr(65 + r) + s
    return s


def build_xlsx():
    # -----------------------------------------------------------------
    # 1. 收集所有字符串（去重）
    # -----------------------------------------------------------------
    str_list = []
    str_index = {}

    def reg(s):
        if s in str_index:
            return str_index[s]
        i = len(str_list)
        str_index[s] = i
        str_list.append(s)
        return i

    header_ids = [reg(h) for h in HEADERS]
    all_rows_ids = []
    for row in MENUS:
        row_ids = [reg(str(v)) for v in row]
        all_rows_ids.append(row_ids)

    total_refs = len(HEADERS) + sum(len(r) for r in MENUS)
    unique_count = len(str_list)

    # -----------------------------------------------------------------
    # 2. sharedStrings.xml
    # -----------------------------------------------------------------
    ss_parts = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>']
    ss_parts.append(
        f'<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        f'count="{total_refs}" uniqueCount="{unique_count}">'
    )
    for s in str_list:
        ss_parts.append(f'<si><t xml:space="preserve">{escape(s)}</t></si>')
    ss_parts.append('</sst>')
    ss_xml = "".join(ss_parts)

    # -----------------------------------------------------------------
    # 3. styles.xml
    #    样式索引：
    #    0: 默认（普通单元格，自动换行+垂直居中）
    #    1: 表头（深绿底 + 金字加粗 + 居中 + 金色边框）
    #    2: 路径/菜单名列（白底 + 边框，左对齐）
    #    3: √ 单元格（浅绿底 + 居中）
    #    4: △ 单元格（浅黄底 + 居中）
    #    5: × 单元格（浅灰底 + 居中，灰字）
    # -----------------------------------------------------------------
    styles_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="微软雅黑"/><color rgb="FF000000"/></font>
    <font><sz val="12"/><name val="微软雅黑"/><color rgb="FFFFD78A"/><b/></font>
    <font><sz val="11"/><name val="微软雅黑"/><color rgb="FF2D7A3E"/><b/></font>
    <font><sz val="11"/><name val="微软雅黑"/><color rgb="FF999999"/></font>
  </fonts>
  <fills count="6">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1A3A2A"/><bgColor rgb="FF1A3A2A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8F5E9"/><bgColor rgb="FFE8F5E9"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF6E0"/><bgColor rgb="FFFEF6E0"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF5F5F5"/><bgColor rgb="FFF5F5F5"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFC4A35A"/></left>
      <right style="thin"><color rgb="FFC4A35A"/></right>
      <top style="thin"><color rgb="FFC4A35A"/></top>
      <bottom style="thin"><color rgb="FFC4A35A"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="3" fillId="5" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
  </cellXfs>
  <cellStyles count="1">
    <cellStyle name="常规" xfId="0" builtinId="0"/>
  </cellStyles>
</styleSheet>
"""

    # -----------------------------------------------------------------
    # 4. sheet1.xml
    #    严格遵循 OOXML 元素顺序：
    #    sheetPr? , dimension? , sheetViews? , sheetFormatPr? , cols? ,
    #    sheetData , autoFilter? , mergeCells? , ...
    #    freezePanes 通过 sheetViews/sheetView/pane 实现（非直接子元素）
    # -----------------------------------------------------------------
    # 列定义
    cols_xml = []
    for i, w in enumerate(COL_WIDTHS, 1):
        cols_xml.append(f'<col min="{i}" max="{i}" width="{w}" customWidth="1"/>')
    cols_block = f'<cols>{"".join(cols_xml)}</cols>'

    # 行定义
    rows_xml = []
    # 表头行（row 1, style=1）
    cells = []
    for ci, sid in enumerate(header_ids, 1):
        cells.append(f'<c r="{col_letter(ci)}1" t="s" s="1"><v>{sid}</v></c>')
    rows_xml.append(f'<row r="1" ht="32" customHeight="1">{"".join(cells)}</row>')

    # 数据行（row 2+）
    # 列 1=菜单路径(style 2)  列 2=菜单名(style 2)
    # 列 3-11=角色权限：√=style3  △=style4  ×=style5
    for ri, row_ids in enumerate(all_rows_ids, start=2):
        row_vals = MENUS[ri - 2]
        cells = []
        for ci, sid in enumerate(row_ids, 1):
            if ci <= 2:
                # 菜单路径/菜单名列
                style = 2
            else:
                # 角色权限列：按符号选样式
                val = row_vals[ci - 1]
                if val == "√":
                    style = 3
                elif val == "△":
                    style = 4
                else:  # ×
                    style = 5
            cells.append(f'<c r="{col_letter(ci)}{ri}" t="s" s="{style}"><v>{sid}</v></c>')
        rows_xml.append(f'<row r="{ri}" ht="22" customHeight="1">{"".join(cells)}</row>')

    last_row = len(MENUS) + 1
    last_col = col_letter(len(HEADERS))

    sheet_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <dimension ref="A1:{last_col}{last_row}"/>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane xSplit="2" ySplit="1" topLeftCell="C2" activePane="bottomRight" state="frozen"/>
      <selection pane="bottomRight" activeCell="C2" sqref="C2"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="20"/>
  {cols_block}
  <sheetData>
    {"".join(rows_xml)}
  </sheetData>
  <autoFilter ref="A1:{last_col}{last_row}"/>
</worksheet>"""

    # -----------------------------------------------------------------
    # 5. workbook.xml
    # -----------------------------------------------------------------
    workbook_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="角色权限菜单对照表" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    # -----------------------------------------------------------------
    # 6. workbook.xml.rels
    # -----------------------------------------------------------------
    workbook_rels_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    # -----------------------------------------------------------------
    # 7. [Content_Types].xml
    # -----------------------------------------------------------------
    content_types_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    # -----------------------------------------------------------------
    # 8. _rels/.rels（根关系）
    # -----------------------------------------------------------------
    root_rels_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    # -----------------------------------------------------------------
    # 9. 打包成 xlsx（zip）
    # -----------------------------------------------------------------
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    if os.path.exists(OUTPUT):
        os.remove(OUTPUT)

    with zipfile.ZipFile(OUTPUT, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml', content_types_xml)
        z.writestr('_rels/.rels', root_rels_xml)
        z.writestr('xl/workbook.xml', workbook_xml)
        z.writestr('xl/_rels/workbook.xml.rels', workbook_rels_xml)
        z.writestr('xl/styles.xml', styles_xml)
        z.writestr('xl/sharedStrings.xml', ss_xml)
        z.writestr('xl/worksheets/sheet1.xml', sheet_xml)

    size_kb = os.path.getsize(OUTPUT) / 1024
    print(f"[OK] 生成成功: {OUTPUT}")
    print(f"     文件大小: {size_kb:.2f} KB")
    print(f"     菜单总数: {len(MENUS)}")
    print(f"     角色数: {len(ROLES)}")
    print(f"     {LEGEND}")
    print(f"     字符串总数: {unique_count} (引用 {total_refs} 次)")

    # 统计每个角色的权限分布
    print("     各角色权限分布:")
    for ri, role in enumerate(ROLES):
        full = sum(1 for m in MENUS if m[ri + 2] == "√")
        read = sum(1 for m in MENUS if m[ri + 2] == "△")
        none = sum(1 for m in MENUS if m[ri + 2] == "×")
        print(f"       {role}: √={full}  △={read}  ×={none}")

    with zipfile.ZipFile(OUTPUT, 'r') as z:
        names = z.namelist()
        print(f"     zip 文件数: {len(names)}")
        for n in names:
            print(f"       - {n}")


if __name__ == "__main__":
    build_xlsx()
