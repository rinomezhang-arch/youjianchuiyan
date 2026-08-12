"""
提取宴会成本菜单录入表.xlsm中的配料明细数据
重点读取: 模板表 + NO开头的订单表中的配料数据
"""
import zipfile
import xml.etree.ElementTree as ET

XLSM_PATH = r"F:\又见炊烟私房菜宴会预定数据库\又见炊烟私房菜宴会预定数据库\宴会成本菜单录入表.xlsm"

def read_shared_strings(z):
    shared = []
    try:
        xml_data = z.read('xl/sharedStrings.xml').decode('utf-8')
        root = ET.fromstring(xml_data)
        ns = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
        for si in root.findall('.//main:si', ns):
            texts = si.findall('.//main:t', ns)
            text = ''.join([t.text or '' for t in texts])
            shared.append(text)
    except:
        pass
    return shared

def read_sheet_full(z, sheet_file, shared_strings):
    """读取工作表全部数据"""
    try:
        xml_data = z.read(f'xl/worksheets/{sheet_file}.xml').decode('utf-8')
        root = ET.fromstring(xml_data)
        ns = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
        rows = root.findall('.//main:row', ns)
        result = []
        for row in rows:
            r = int(row.get('r'))
            cells = row.findall('.//main:c', ns)
            row_data = {}
            for cell in cells:
                ref = cell.get('r')
                col = ''.join([c for c in ref if c.isalpha()])
                t = cell.get('t')
                v = cell.find('main:v', ns)
                val = None
                if v is not None:
                    val = v.text
                    if t == 's':
                        idx = int(val)
                        if idx < len(shared_strings):
                            val = shared_strings[idx]
                row_data[col] = val
            result.append((r, row_data))
        return result
    except Exception as e:
        return [(0, {'error': str(e)})]

def main():
    with zipfile.ZipFile(XLSM_PATH, 'r') as z:
        shared = read_shared_strings(z)

        # 读取模板表完整数据
        print("=" * 80)
        print("模板表 (sheet2) 完整数据")
        print("=" * 80)
        rows = read_sheet_full(z, 'sheet2', shared)
        for r, data in rows:
            if data:
                # 过滤掉None值
                clean = {k: v for k, v in data.items() if v is not None}
                if clean:
                    print(f"  行{r}: {clean}")

        # 读取NO251229181154490完整数据
        print("\n" + "=" * 80)
        print("NO251229181154490 (sheet4) 完整数据")
        print("=" * 80)
        rows = read_sheet_full(z, 'sheet4', shared)
        for r, data in rows:
            if data:
                clean = {k: v for k, v in data.items() if v is not None}
                if clean:
                    print(f"  行{r}: {clean}")

        # 读取NO_202512250135370完整数据(宴会订单)
        print("\n" + "=" * 80)
        print("NO_202512250135370 (sheet6) 完整数据")
        print("=" * 80)
        rows = read_sheet_full(z, 'sheet6', shared)
        for r, data in rows:
            if data:
                clean = {k: v for k, v in data.items() if v is not None}
                if clean:
                    print(f"  行{r}: {clean}")

        # 读取菜肴信息表的完整列头(行2)
        print("\n" + "=" * 80)
        print("菜肴信息表 (sheet1) 完整列头")
        print("=" * 80)
        rows = read_sheet_full(z, 'sheet1', shared)
        if len(rows) >= 2:
            r, data = rows[1]  # 行2
            print(f"  行{r}列头: {data}")

        # 检查菜肴信息表行3的数据(第一道菜)
        if len(rows) >= 3:
            r, data = rows[2]  # 行3
            print(f"  行{r}数据: {data}")

        # 读取成本信息表前5行
        print("\n" + "=" * 80)
        print("成本信息表 (sheet3) 前5行")
        print("=" * 80)
        rows = read_sheet_full(z, 'sheet3', shared)
        for r, data in rows[:5]:
            clean = {k: v for k, v in data.items() if v is not None}
            if clean:
                print(f"  行{r}: {clean}")

if __name__ == '__main__':
    main()
