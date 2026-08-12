"""
纯标准库生成 xlsx（无依赖）
修复版：严格遵循 OOXML 元素顺序，确保 Excel/WPS 可打开
用途：生成"待人工产出任务清单.xlsx"
"""
import os
import zipfile
from xml.sax.saxutils import escape

OUTPUT = r"f:\solo\project\又见炊烟餐饮管理系统2.0\又见炊烟餐饮管理系统 2.0 全套开发交付文档集\待人工产出任务清单.xlsx"

# =====================================================================
# 任务数据
# =====================================================================
TASKS = [
    (1,  "01 产品PRD", "产品需求说明书 PRD",            "又见炊烟餐饮系统V2.0_产品需求说明书PRD.docx",        "高", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "—",                  "业务源头，最高优先级"),
    (2,  "01 产品PRD", "100 道业务端到端场景流程",       "100道餐饮业务端到端场景流程.docx",                  "高", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "—",                  "前后库数据流"),
    (3,  "01 产品PRD", "可配置参数&业务规则说明书",      "系统可配置参数&业务规则说明书.docx",                "高", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "config 表 30+ 项",    "SQL 已就绪，需文档化"),
    (4,  "01 产品PRD", "全局状态字典统一标准",            "全局状态字典统一标准文档.docx",                      "高", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "sys_dict 17 类字典",   "字典已就绪，需文档化"),
    (5,  "01 产品PRD", "客户短信通知模板",                "客户短信通知模板.docx",                              "中", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "NotifyConsumer 短信",  "待短信通道接入后定稿"),
    (6,  "01 产品PRD", "全套单据打印规范",                "全套单据打印规范说明.docx",                          "中", "产品经理(天龙)", "", "", "", "未开始", "01_产品PRD/",            "—",                  "结账单/后厨工单/预订单"),
    (7,  "01 产品PRD", "全端原型 Figma/Axure",            "素材包_原型_全端原型_FigmaAxure文件包",             "中", "UI设计师",       "", "", "", "未开始", "01_产品PRD/素材包_原型/", "—",                  "需 UI 设计师产出"),
    (8,  "01 产品PRD", "UI 设计规范&页面稿",              "素材包_UI_UI设计规范&全套页面稿.fig",               "中", "UI设计师",       "", "", "", "未开始", "01_产品PRD/素材包_UI/",  "—",                  "需 UI 设计师产出"),
    (9,  "03 接口规范", "OpenAPI 完整接口文档(HTML)",     "OpenAPI_系统完整接口文档.html",                      "高", "后端开发",       "", "", "", "未开始", "03_接口规范/",           "51 个 Controller",     "启动后端访问 /swagger-ui.html 导出"),
    (10, "03 接口规范", "后端接口开发规范",                "后端接口开发规范.docx",                              "中", "后端负责人",     "", "", "", "未开始", "03_接口规范/",           "鉴权门店隔离.md",     "可基于已补全文档整合"),
    (11, "04 前端开发", "全局公共组件使用文档",          "全局公共组件使用文档.docx",                          "中", "前端负责人",     "", "", "", "未开始", "04_前端开发/",           "Vue3前端路由.md",      "文档化 components/"),
    (12, "04 前端开发", "打印 HTML 模板",                 "文件夹_打印HTML模板",                                "低", "前端+业务",       "", "", "", "未开始", "04_前端开发/文件夹_打印HTML模板/", "—",      "结账单/后厨工单/预订单"),
    (13, "05 权限审批流", "角色-权限-菜单对照表(Excel)", "角色-权限-菜单对照表.xlsx",                          "中", "产品经理",       "", "", "", "未开始", "05_权限审批流/",         "角色权限菜单对照表.md", "由 .md 转 Excel"),
    (14, "05 权限审批流", "全业务审批流程图(PNG)",       "全业务审批流程图.png",                                "中", "设计师",         "", "", "", "未开始", "05_权限审批流/",         "审批SQL.sql",          "PlantUML 或 draw.io 绘制"),
    (15, "06 WebSocket异步", "KDS 后厨实时推送接口文档", "KDS后厨实时推送接口文档.docx",                        "低", "后端开发",       "", "", "", "未开始", "06_WebSocket异步通信/",  "业务消息结构体说明.md", "待 WebSocket 接入后补全"),
    (16, "07 架构部署测试", "100 场景完整测试用例(Excel)", "100场景完整测试用例.xlsx",                          "高", "测试人员",       "", "", "", "未开始", "07_架构部署测试/",       "上线CheckList.md",     "端到端测试用例"),
    (17, "07 架构部署测试", "运维操作说明书",              "运维操作说明书.docx",                                "中", "运维人员",       "", "", "", "未开始", "07_架构部署测试/",       "Docker部署指南.md",     "可基于已补全文档整合"),
    (18, "02 数据库设计", "ER 图 PNG 导出",              "业务实体ER图.png",                                   "低", "开发",           "", "", "", "未开始", "02_数据库设计/",         "业务实体ER图.plantuml", "VS Code PlantUML 插件导出"),
    (19, "02 数据库设计", "系统初始化基础数据SQL整合",   "系统初始化基础数据SQL.sql",                          "低", "开发",           "", "", "", "未开始", "02_数据库设计/",         "banquet_init.sql",     "整合或保留原位置"),
]

HEADERS = ["序号", "类别", "任务名", "目标文件名", "优先级", "负责角色", "负责人",
           "计划开始", "计划完成", "状态", "交付物路径", "关联已补全文档", "备注"]

# 列宽（字符数）
COL_WIDTHS = [6, 22, 28, 38, 8, 16, 12, 12, 12, 10, 28, 28, 32]


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
    for row in TASKS:
        row_ids = [reg(str(v)) for v in row]
        all_rows_ids.append(row_ids)

    total_refs = len(HEADERS) + sum(len(r) for r in TASKS)
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
    #    2: 高优先级行（浅红底 + 边框，左对齐）
    #    3: 中优先级行（浅黄底 + 边框，左对齐）
    #    4: 低优先级行（浅灰底 + 边框，左对齐）
    #    5: 高优先级 + 居中
    #    6: 中优先级 + 居中
    #    7: 低优先级 + 居中
    # -----------------------------------------------------------------
    styles_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="微软雅黑"/><color rgb="FF000000"/></font>
    <font><sz val="12"/><name val="微软雅黑"/><color rgb="FFFFD78A"/><b/></font>
    <font><sz val="11"/><name val="微软雅黑"/><color rgb="FF8B2020"/><b/></font>
  </fonts>
  <fills count="6">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1A3A2A"/><bgColor rgb="FF1A3A2A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFDEAEA"/><bgColor rgb="FFFDEAEA"/></patternFill></fill>
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
  <cellXfs count="8">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="5" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="0" fillId="5" borderId="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
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
    #    sheetData , sheetCalcPr? , sheetProtection? , ..., mergeCells? ,
    #    phoneticPr? , conditionalFormatting? , dataValidations? ,
    #    hyperlinks? , printOptions? , pageMargins? , pageSetup? ,
    #    headerFooter? , rowBreaks? , colBreaks? , customProperties? ,
    #    cellWatches? , ignoredErrors? , smartTags? , drawing? ,
    #    legacyDrawing? , legacyDrawingHF? , picture? , oleObjects? ,
    #    controls? , webPublishItems? , tableParts? , extLst?
    #    注意：autoFilter 应位于 sheetData 之后、mergeCells 之前
    #    freezePanes 通过 sheetViews/sheetView/pane 实现
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
    for ri, row_ids in enumerate(all_rows_ids, start=2):
        task = TASKS[ri - 2]
        priority = task[4]
        if priority == "高":
            base_style = 2
            center_style = 5
        elif priority == "中":
            base_style = 3
            center_style = 6
        else:
            base_style = 4
            center_style = 7

        cells = []
        for ci, sid in enumerate(row_ids, 1):
            # 序号(1)/优先级(5)/状态(10) 列居中
            if ci in (1, 5, 10):
                style = center_style
            else:
                style = base_style
            cells.append(f'<c r="{col_letter(ci)}{ri}" t="s" s="{style}"><v>{sid}</v></c>')
        rows_xml.append(f'<row r="{ri}" ht="22" customHeight="1">{"".join(cells)}</row>')

    last_row = len(TASKS) + 1
    last_col = col_letter(len(HEADERS))

    sheet_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <dimension ref="A1:{last_col}{last_row}"/>
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane xSplit="0" ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
      <selection pane="bottomLeft" activeCell="A2" sqref="A2"/>
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
    <sheet name="任务清单" sheetId="1" r:id="rId1"/>
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
    print(f"     任务总数: {len(TASKS)}")
    print(f"     字符串总数: {unique_count} (引用 {total_refs} 次)")

    with zipfile.ZipFile(OUTPUT, 'r') as z:
        names = z.namelist()
        print(f"     zip 文件数: {len(names)}")
        for n in names:
            print(f"       - {n}")


if __name__ == "__main__":
    build_xlsx()
