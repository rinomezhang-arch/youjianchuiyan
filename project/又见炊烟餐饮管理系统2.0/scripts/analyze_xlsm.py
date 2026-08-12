"""
分析 宴会成本菜单录入表.xlsm
提取: 1.所有工作表 2.VBA代码 3.各表头结构 4.数据样例
"""
import zipfile
import xml.etree.ElementTree as ET
import os
import re

XLSM_PATH = r"F:\又见炊烟私房菜宴会预定数据库\又见炊烟私房菜宴会预定数据库\宴会成本菜单录入表.xlsm"

def extract_xlsm_structure(xlsm_path):
    """提取xlsm文件结构"""
    print(f"=== 分析文件: {os.path.basename(xlsm_path)} ===")
    print(f"文件大小: {os.path.getsize(xlsm_path)} bytes\n")

    with zipfile.ZipFile(xlsm_path, 'r') as z:
        # 1. 列出所有文件
        all_files = z.namelist()
        print("--- 1. xlsm内部文件列表 ---")
        for f in all_files:
            if 'sheet' in f.lower() or 'vba' in f.lower() or 'workbook' in f.lower():
                print(f"  {f}")

        # 2. 提取工作表列表
        print("\n--- 2. 工作表列表 ---")
        try:
            wb_xml = z.read('xl/workbook.xml').decode('utf-8')
            root = ET.fromstring(wb_xml)
            ns = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main',
                  'r': 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
            sheets = root.findall('.//main:sheet', ns)
            for i, sheet in enumerate(sheets, 1):
                name = sheet.get('name')
                sheet_id = sheet.get('sheetId')
                rid = sheet.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id')
                print(f"  {i}. {name} (sheetId={sheet_id}, rId={rid})")
        except Exception as e:
            print(f"  读取workbook.xml失败: {e}")

        # 3. 提取VBA代码
        print("\n--- 3. VBA宏代码 ---")
        vba_found = False
        for f in all_files:
            if 'vbaProject.bin' in f:
                vba_found = True
                print(f"  找到VBA项目文件: {f}")
                # 尝试提取VBA代码(二进制格式,需要特殊解析)
                vba_data = z.read(f)
                # 简单提取可见的字符串
                text_parts = []
                for i in range(0, len(vba_data), 2):
                    try:
                        b = vba_data[i:i+2]
                        if len(b) == 2 and b[0] >= 0x20 and b[0] < 0x7f and b[1] == 0:
                            text_parts.append(chr(b[0]))
                        else:
                            if len(text_parts) > 10:
                                text = ''.join(text_parts)
                                if any(kw in text for kw in ['Sub ', 'Function ', 'Dim ', 'Set ', 'If ', 'For ', 'Range', 'Cells', 'Worksheets']):
                                    print(f"\n  [VBA代码片段]")
                                    print(f"  {text[:500]}")
                            text_parts = []
                    except:
                        pass
                break
        if not vba_found:
            print("  未找到VBA项目文件(可能是纯数据xlsm)")

        # 4. 提取各工作表的前几行数据
        print("\n--- 4. 各工作表数据 ---")
        shared_strings = []
        try:
            ss_xml = z.read('xl/sharedStrings.xml').decode('utf-8')
            ss_root = ET.fromstring(ss_xml)
            ns2 = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
            for si in ss_root.findall('.//main:si', ns2):
                texts = si.findall('.//main:t', ns2)
                text = ''.join([t.text or '' for t in texts])
                shared_strings.append(text)
        except:
            pass

        # 解析每个sheet
        sheet_files = [f for f in all_files if f.startswith('xl/worksheets/sheet') and f.endswith('.xml')]
        for sheet_file in sorted(sheet_files):
            sheet_name = sheet_file.replace('xl/worksheets/', '').replace('.xml', '')
            print(f"\n  === {sheet_name} ===")
            try:
                sheet_xml = z.read(sheet_file).decode('utf-8')
                sheet_root = ET.fromstring(sheet_xml)
                ns3 = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
                rows = sheet_root.findall('.//main:row', ns3)
                print(f"  总行数: {len(rows)}")
                # 显示前5行
                for row in rows[:5]:
                    r = row.get('r')
                    cells = row.findall('.//main:c', ns3)
                    cell_values = []
                    for cell in cells:
                        ref = cell.get('r')
                        t = cell.get('t')
                        v_elem = cell.find('main:v', ns3)
                        if v_elem is not None:
                            val = v_elem.text
                            if t == 's':
                                idx = int(val)
                                if idx < len(shared_strings):
                                    val = shared_strings[idx]
                            cell_values.append(f"{ref}={val}")
                    print(f"  行{r}: {' | '.join(cell_values[:10])}")
            except Exception as e:
                print(f"  解析失败: {e}")

if __name__ == '__main__':
    extract_xlsm_structure(XLSM_PATH)
