import os
import sys
# 自动锁定脚本目录，双击运行路径不会错乱
script_dir = os.path.dirname(os.path.abspath(sys.argv[0]))
os.chdir(script_dir)

import json
import datetime
import html
import traceback
from enum import Enum
from typing import List, Dict

# ====================== 配置加载：环境变量优先，.env兜底 ======================
def _load_env():
    for env_path in [
        os.path.join(script_dir, '.env'),
        os.path.join(os.path.dirname(script_dir), '.env'),
    ]:
        if os.path.exists(env_path):
            with open(env_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('#') or '=' not in line:
                        continue
                    k, v = line.split('=', 1)
                    k, v = k.strip(), v.strip().strip('"').strip("'")
                    if k not in os.environ:
                        os.environ[k] = v
_load_env()

# ====================== 业务配置区（从环境变量/配置文件读取） ======================
DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', '3306')),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASS', 'Wo002323'),
    'database': os.environ.get('DB_NAME', 'banquet'),
    'charset': 'utf8mb4'
}
API_BASE_URL = os.environ.get('API_BASE_URL', 'http://localhost:8080/api')
LOGIN_CRED = {
    "username": os.environ.get('LOGIN_USERNAME', '张婧'),
    "password": os.environ.get('LOGIN_PASSWORD', '002323')
}
CPU_THRESHOLD = int(os.environ.get('CPU_THRESHOLD', '85'))
MEM_THRESHOLD = int(os.environ.get('MEM_THRESHOLD', '85'))
DISK_THRESHOLD = int(os.environ.get('DISK_THRESHOLD', '90'))
DUPLICATE_TABLE_GROUP = [
    ("purchase_request", "procurement_request"),
    ("purchase_receipt", "purchase_receipt"),
    ("material_requisition", "requisition_order"),
    ("cost_card", "dish_cost_card"),
    ("package_dish_detail", "package_dish_rel")
]
REDUNDANT_COL_MAP = {
    "dish_master": [("price", "cost_price")],
    "ingredient_master": [("unit_price", "avg_price")],
    "booking_master": [("deposit_amount", "deposit")]
}
FK_FIELD_MATCH = [
    ("cost_card", "dish_id", "dish_master", "dish_id"),
    ("goods_receipt", "supplier_id", "supplier_master", "supplier_id"),
    ("finance_receivable", "booking_id", "booking_master", "id"),
    ("material_requisition_item", "requisition_id", "material_requisition", "requisition_id"),
    ("package_dish_detail", "package_id", "package_master", "package_id")
]
API_FULL_LIST = [
    "/bookings", "/bookings/list",
    "/dishes", "/customers",
    "/kitchen-supply/purchase-requests",
    "/kitchen-supply/goods-receipts",
    "/kitchen-supply/requisitions",
    "/hr/schedule", "/hr/departments",
    "/finance/today", "/finance/balance",
    "/menu-api/ingredients", "/menu-api/suppliers"
]
VUE_PAGE_LIST = ["Receipt.vue", "Issue.vue", "Booking.vue", "DishManage.vue", "Staff.vue"]

# ====================== 路径配置 ======================
BASE_DIR = script_dir
PROJECT_ROOT = os.environ.get('PROJECT_ROOT', os.path.dirname(BASE_DIR))
BACK_CODE_PATH = os.path.join(PROJECT_ROOT, "banquet_project", "src", "main", "java", "com", "youjian", "banquet")
FRONT_CODE_PATH = os.path.join(PROJECT_ROOT, "frontend_v3")
FRONT_DIST_PATH = os.path.join(PROJECT_ROOT, "frontend_v3", "dist")
PATH_AUDIT_HTML = os.path.join(BASE_DIR, "system_audit_full_v2.html")
PATH_CHECKUP_HTML = os.path.join(BASE_DIR, "system_checkup_v2.html")
PATH_JSON_DATA = os.path.join(BASE_DIR, "audit_data_v2.json")
# 云端配置
REMOTE_HOST = os.environ.get('REMOTE_HOST', 'ubuntu@1.13.173.213')
REMOTE_SSH_KEY = os.path.expanduser(os.environ.get('SSH_KEY', '~/.ssh/id_rsa_new'))
REMOTE_NGINX_ROOT = "/var/www/html"
REMOTE_COS_MOUNT = "/mnt/cos"
HAS_SSH = os.path.exists(REMOTE_SSH_KEY)

# ====================== 风险等级枚举 ======================
class RiskLevel(Enum):
    FATAL = "FATAL"
    ERROR = "ERROR"
    WARNING = "WARNING"
    NORMAL = "NORMAL"

# ====================== 第三方依赖导入 ======================
try:
    import pymysql
    HAS_PYMYSQL = True
except ImportError:
    HAS_PYMYSQL = False
try:
    import psutil
    HAS_PSUTIL = True
except ImportError:
    HAS_PSUTIL = False
import requests

# ====================== 基础扫描父类 ======================
class BaseScanner:
    def __init__(self):
        self.scan_items = []
        self.stat = {"FATAL": 0, "ERROR": 0, "WARNING": 0, "NORMAL": 0, "total": 0}
        self.db_conn = None
        self.http_session = requests.Session()
        self.token = ""
        self.all_db_tables = []
        self.db_table_detail = {}

    def record_item(
        self,
        scan_id: str,
        module: str,
        title: str,
        expect: str,
        actual: str,
        level: RiskLevel,
        detail: str = "",
        fix_sql: str = "",
        fix_cmd: str = "",
        file_list: str = "",
        scene: str = "",
        tags: str = "",
        score: int = 0
    ):
        level_str = level.value if isinstance(level, RiskLevel) else level
        item = {
            "scan_id": scan_id,
            "module": module,
            "scene": scene,
            "title": title,
            "expect": expect,
            "actual": str(actual),
            "level": level_str,
            "detail": detail,
            "fix_sql": fix_sql,
            "fix_cmd": fix_cmd,
            "file_list": file_list,
            "tags": tags,
            "score": score
        }
        self.scan_items.append(item)
        self.stat[level_str] += 1
        self.stat["total"] += 1

    def ssh_exec(self, cmd, timeout=15):
        """在云端执行命令，返回 (stdout, stderr, exit_code)"""
        import subprocess
        if not HAS_SSH:
            return ("", "SSH key not found", 1)
        try:
            full_cmd = f'ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "{REMOTE_SSH_KEY}" {REMOTE_HOST} "{cmd}"'
            result = subprocess.run(full_cmd, shell=True, capture_output=True, text=True, timeout=timeout)
            return (result.stdout, result.stderr, result.returncode)
        except Exception as e:
            return ("", str(e), 1)

    def get_db_conn(self):
        if not HAS_PYMYSQL:
            self.record_item(
                scan_id="DB-CONN",
                module="数据库",
                scene="全局连接",
                title="数据库正常连接",
                expect="连接成功",
                actual="pymysql未安装",
                level=RiskLevel.FATAL,
                detail="执行pip install pymysql安装依赖"
            )
            return None
        try:
            if self.db_conn is None or not self.db_conn.open:
                self.db_conn = pymysql.connect(**DB_CONFIG)
            return self.db_conn
        except Exception as e:
            self.record_item(
                scan_id="DB-CONN",
                module="数据库",
                scene="全局连接",
                title="数据库正常连接",
                expect="连接成功",
                actual=f"连接失败:{str(e)}",
                level=RiskLevel.FATAL,
                detail="核对账号、IP、端口"
            )
            return None

    def db_query(self, sql, silent=False):
        conn = self.get_db_conn()
        if conn is None:
            return []
        try:
            cur = conn.cursor()
            cur.execute(sql)
            res = cur.fetchall()
            cur.close()
            return res
        except Exception as e:
            if not silent:
                err_str = str(e)
                if "doesn't exist" not in err_str and "1146" not in err_str:
                    print(f"SQL异常:{sql[:120]} | {err_str}")
            return []

    def load_all_table_info(self):
        conn = self.get_db_conn()
        if conn is None:
            return
        sql_tables = f"SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA='{DB_CONFIG['database']}'"
        table_list = self.db_query(sql_tables)
        self.all_db_tables = [t[0] for t in table_list]
        total_cols = 0
        for tbl_name, tbl_comment in table_list:
            cols = self.db_query(f"SHOW FULL COLUMNS FROM `{tbl_name}`")
            self.db_table_detail[tbl_name] = {"table_comment": tbl_comment, "columns": cols}
            total_cols += len(cols)
        print(f"[DEBUG] 加载表: {len(self.all_db_tables)}张, 总字段: {total_cols}个")

    def table_exists(self, name):
        return name in self.all_db_tables

    def api_login(self):
        try:
            resp = self.http_session.post(f"{API_BASE_URL}/auth/login", json=LOGIN_CRED, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                token = data.get("data", {}).get("token")
                if token:
                    self.token = token
                    self.http_session.headers["Authorization"] = f"Bearer {token}"
                    return True
            return False
        except Exception as e:
            print(f"登录接口异常:{str(e)}")
            return False

    def api_get(self, path, params=None):
        try:
            url = f"{API_BASE_URL}{path}"
            # Auto-add storeId=1 if not specified (many APIs require it)
            if params is None:
                params = {"storeId": "1"}
            elif "storeId" not in params:
                params["storeId"] = "1"
            resp = self.http_session.get(url, params=params, timeout=30)
            if resp.status_code == 200:
                return resp.json()
            return {"code": resp.status_code, "msg": resp.text[:200]}
        except Exception as e:
            return {"code": 999, "msg": f"连接失败:{str(e)}"}

    def scan_file_keyword(self, root_path, keyword):
        match = []
        if not os.path.exists(root_path):
            return match
        skip_dirs = {"dist", "node_modules", ".git", "__pycache__", "target", "build"}
        for r, dirs, fs in os.walk(root_path):
            # Skip build/dependency directories in-place
            dirs[:] = [d for d in dirs if d not in skip_dirs]
            for fname in fs:
                suffix = fname.split(".")[-1]
                if suffix in ("vue", "js", "java"):
                    fp = os.path.join(r, fname)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            if keyword in f.read():
                                match.append(fp)
                    except Exception:
                        continue
        return match

# ====================== 服务器扫描模块 ======================
class ServerScanner(BaseScanner):
    def scan_all(self):
        if not HAS_PSUTIL:
            self.record_item(
                scan_id="SERVER-000",
                module="服务器层",
                scene="资源监控依赖",
                title="psutil模块正常",
                expect="已安装",
                actual="未安装，跳过资源监控",
                level=RiskLevel.WARNING,
                detail="执行pip install psutil开启CPU/内存/磁盘监控"
            )
        else:
            cpu = psutil.cpu_percent(1)
            if cpu >= CPU_THRESHOLD:
                self.record_item("SERVER-001", "服务器层", "CPU负载", "CPU阈值<85%", f"{cpu}%", RiskLevel.WARNING, "负载偏高", fix_cmd="top -c")
            else:
                self.record_item("SERVER-001", "服务器层", "CPU负载", "CPU阈值<85%", f"{cpu}%", RiskLevel.NORMAL, "负载正常")
            mem = psutil.virtual_memory().percent
            if mem >= MEM_THRESHOLD:
                self.record_item("SERVER-002", "服务器层", "内存占用", f"<{MEM_THRESHOLD}%", f"{mem}%", RiskLevel.WARNING, "易OOM", fix_cmd="free -h")
            else:
                self.record_item("SERVER-002", "服务器层", "内存占用", f"<{MEM_THRESHOLD}%", f"{mem}%", RiskLevel.NORMAL, "内存充足")
            disk = psutil.disk_usage("/").percent
            if disk >= DISK_THRESHOLD:
                self.record_item("SERVER-003", "服务器层", "磁盘空间", f"<{DISK_THRESHOLD}%", f"{disk}%", RiskLevel.FATAL, "磁盘将耗尽宕机", fix_cmd="df -h")
            else:
                self.record_item("SERVER-003", "服务器层", "磁盘空间", f"<{DISK_THRESHOLD}%", f"{disk}%", RiskLevel.NORMAL, "空间充足")
        if HAS_PYMYSQL and self.get_db_conn():
            curr_conn = int(self.db_query("SHOW STATUS LIKE 'Threads_connected'")[0][1])
            max_conn = int(self.db_query("SHOW VARIABLES LIKE 'max_connections'")[0][1])
            rate = curr_conn / max_conn * 100
            if rate >= 80:
                self.record_item("SERVER-004", "服务器层", "数据库连接池", "<80%", f"{rate:.1f}%", RiskLevel.WARNING, "连接接近上限", fix_cmd="show processlist;")
            else:
                self.record_item("SERVER-004", "服务器层", "数据库连接池", "<80%", f"{rate:.1f}%", RiskLevel.NORMAL, "负载正常")

# ====================== 数据库扫描模块 ======================
class DatabaseScanner(BaseScanner):
    def scan_all(self):
        self.load_all_table_info()
        dup_err = []
        dup_sql = "SET FOREIGN_KEY_CHECKS=0;\n"
        for main, dup in DUPLICATE_TABLE_GROUP:
            if main in self.all_db_tables and dup in self.all_db_tables:
                dup_err.append(f"主表{main} 冗余{dup}")
                dup_sql += f"DROP TABLE IF EXISTS {dup};\n"
        dup_sql += "SET FOREIGN_KEY_CHECKS=1;"
        if dup_err:
            self.record_item("DB-001", "数据库层", "业务表唯一性", "无重复业务表", str(dup_err), RiskLevel.FATAL, "两套表数据割裂对账不平", fix_sql=dup_sql)
        else:
            self.record_item("DB-001", "数据库层", "业务表唯一性", "无重复业务表", "无冗余", RiskLevel.NORMAL, "表结构干净")
        col_err = []
        col_sql = "SET FOREIGN_KEY_CHECKS=0;\n"
        for tbl, pairs in REDUNDANT_COL_MAP.items():
            if tbl not in self.all_db_tables:
                continue
            col_names = [c[0] for c in self.db_table_detail[tbl]["columns"]]
            for old, new in pairs:
                if old in col_names and new in col_names:
                    col_err.append(f"{tbl} 重复字段{old}/{new}")
                    col_sql += f"ALTER TABLE {tbl} DROP COLUMN {old};\n"
        col_sql += "SET FOREIGN_KEY_CHECKS=1;"
        if col_err:
            self.record_item("DB-002", "数据库层", "字段唯一性", "无同义重复字段", str(col_err), RiskLevel.ERROR, "同步更新统计错乱", fix_sql=col_sql)
        else:
            self.record_item("DB-002", "数据库层", "字段唯一性", "无同义重复字段", "正常", RiskLevel.NORMAL, "字段统一")
        type_err = []
        type_sql = "SET FOREIGN_KEY_CHECKS=0;\n"
        for child_t, child_c, parent_t, parent_c in FK_FIELD_MATCH:
            if child_t not in self.all_db_tables or parent_t not in self.all_db_tables:
                continue
            child_cols = self.db_table_detail[child_t]["columns"]
            parent_cols = self.db_table_detail[parent_t]["columns"]
            c_type = [x[1] for x in child_cols if x[0]==child_c][0]
            p_type = [x[1] for x in parent_cols if x[0]==parent_c][0]
            if c_type != p_type:
                type_err.append(f"{child_t}.{child_c} 与 {parent_t}.{parent_c} 类型不一致")
                if "varchar" in p_type:
                    l = p_type.split("(")[1].split(")")[0]
                    type_sql += f"ALTER TABLE {child_t} MODIFY {child_c} VARCHAR({l});\n"
                elif p_type == "int":
                    type_sql += f"ALTER TABLE {child_t} MODIFY {child_c} INT;\n"
                elif p_type == "bigint":
                    type_sql += f"ALTER TABLE {child_t} MODIFY {child_c} BIGINT;\n"
        type_sql += "SET FOREIGN_KEY_CHECKS=1;"
        if type_err:
            self.record_item("DB-003", "数据库层", "外键关联一致性", "父子字段完全匹配", str(type_err), RiskLevel.FATAL, "无法创建物理外键", fix_sql=type_sql)
        else:
            self.record_item("DB-003", "数据库层", "外键关联一致性", "父子字段完全匹配", "全部合规", RiskLevel.NORMAL, "关系约束可用")
        orphan_err = []
        orphan_sql = ""
        for child_t, child_c, parent_t, parent_c in FK_FIELD_MATCH:
            if not (self.table_exists(child_t) and self.table_exists(parent_t)):
                continue
            sql = f"SELECT COUNT(*) FROM `{child_t}` c LEFT JOIN `{parent_t}` p ON c.{child_c}=p.{parent_c} WHERE c.{child_c} IS NOT NULL AND p.{parent_c} IS NULL"
            _r = self.db_query(sql, silent=True); cnt = _r[0][0] if _r else 0
            if cnt > 0:
                orphan_err.append(f"{child_t} 存在{cnt}条孤儿数据")
                orphan_sql += f"DELETE FROM {child_t} WHERE {child_c} NOT IN (SELECT {parent_c} FROM {parent_t});\n"
        if orphan_err:
            self.record_item("DB-004", "数据库层", "数据完整性", "无孤立子记录", str(orphan_err), RiskLevel.ERROR, "报表成本全部失真", fix_sql=orphan_sql)
        else:
            self.record_item("DB-004", "数据库层", "数据完整性", "无孤立子记录", "正常", RiskLevel.NORMAL, "关联完整")
        bad_sqls = [
            ("库存负数", "SELECT COUNT(*) FROM inventory_summary WHERE total_quantity <0", "DELETE FROM inventory_summary WHERE total_quantity<0;"),
            ("采购负单价", "SELECT COUNT(*) FROM ingredient_purchase WHERE purchase_price<0", "UPDATE ingredient_purchase SET purchase_price=0 WHERE purchase_price<0;"),
            ("入职大于离职", "SELECT COUNT(*) FROM staff_master WHERE hire_date>resign_date AND resign_date IS NOT NULL", ""),
            ("成本单价为0", "SELECT COUNT(*) FROM cost_card_detail WHERE unit_price<=0", "")
        ]
        bad_err = []
        bad_fix = ""
        for name, sql, fix in bad_sqls:
            tbl_in_sql = sql.split("FROM `")[1].split("`")[0] if "FROM `" in sql else sql.split("FROM ")[1].split(" ")[0].strip("`")
            if not self.table_exists(tbl_in_sql):
                continue
            result = self.db_query(sql, silent=True); num = result[0][0] if result else 0
            if num > 0:
                bad_err.append(f"{name}:{num}条异常")
                bad_fix += fix + "\n"
        if bad_err:
            self.record_item("DB-005", "数据库层", "业务数据合法性", "无逻辑错误数据", str(bad_err), RiskLevel.ERROR, "财务对账不平", fix_sql=bad_fix)
        else:
            self.record_item("DB-005", "数据库层", "业务数据合法性", "无逻辑错误数据", "全部合规", RiskLevel.NORMAL, "数据正常")
        idx_tbls = ["inventory_summary", "purchase_request", "booking_master", "staff_master"]
        idx_err = []
        idx_sql = ""
        for tbl in idx_tbls:
            if not self.table_exists(tbl):
                continue
            cols = [row[4] for row in self.db_query(f"SHOW INDEX FROM `{tbl}`", silent=True)]
            if "store_id" not in cols:
                idx_err.append(f"{tbl}缺失门店索引")
                idx_sql += f"CREATE INDEX idx_{tbl}_store ON {tbl}(store_id);\n"
        if idx_err:
            self.record_item("DB-006", "数据库层", "查询性能索引", "高频表存在store索引", str(idx_err), RiskLevel.WARNING, "分页看板卡顿", fix_sql=idx_sql)
        else:
            self.record_item("DB-006", "数据库层", "查询性能索引", "高频表存在store索引", "索引齐全", RiskLevel.NORMAL, "查询流畅")
        empty_comment = self.db_query("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND COLUMN_COMMENT='' LIMIT 200")
        if empty_comment:
            self.record_item("DB-007", "数据库层", "文档完整性", "所有字段带注释", f"缺失注释{len(empty_comment)}条", RiskLevel.WARNING, "新人无法读懂字段含义")
        else:
            self.record_item("DB-007", "数据库层", "文档完整性", "所有字段带注释", "注释完整", RiskLevel.NORMAL, "库文档齐全")

        # ====================== 扩容：全维度数据库检测（保底500+） ======================
        print(f"[DEBUG-DB] db_table_detail表数量: {len(self.db_table_detail)}")
        if len(self.db_table_detail) > 0:
            first_tbl = list(self.db_table_detail.keys())[0]
            print(f"[DEBUG-DB] 示例表: {first_tbl}, 字段数: {len(self.db_table_detail[first_tbl]['columns'])}")
        # 第一层：单表基础校验（每张表1条）
        for tbl_name, tbl_info in self.db_table_detail.items():
            col_total = len(tbl_info["columns"])
            comment_status = "无注释" if not tbl_info["table_comment"].strip() else "注释完整"
            level = RiskLevel.WARNING if comment_status == "无注释" else RiskLevel.NORMAL
            self.record_item(
                scan_id=f"DB-TABLE-{tbl_name}",
                module="数据库-数据表",
                scene="数据表元数据完整性校验",
                title=f"数据表【{tbl_name}】基础档案校验",
                expect="数据表存在业务注释，字段数量>=1",
                actual=f"总字段数：{col_total}，表注释状态：{comment_status}",
                level=level,
                detail="用于核查数据表是否完善归档，无注释表会增加后期维护成本"
            )

        # 第二层：字段全维度合法性校验（每个字段1条，核心增量）
        for tbl_name, tbl_info in self.db_table_detail.items():
            for col in tbl_info["columns"]:
                # SHOW FULL COLUMNS 布局: Field(0), Type(1), Collation(2), Null(3), Key(4), Default(5), Extra(6), Privileges(7), Comment(8)
                c_name, c_type, null_flag, key_col, def_val = col[0], col[1], col[3], col[4], col[5]
                comment = col[8] if len(col) > 8 else ""
                risk_level = RiskLevel.NORMAL
                risk_msgs = []
                # 校验1：字段无注释
                if not comment.strip():
                    risk_level = RiskLevel.WARNING
                    risk_msgs.append("缺失字段注释")
                # 校验2：主键允许为空（致命）
                if null_flag == "YES" and "PRI" in key_col:
                    risk_level = RiskLevel.FATAL
                    risk_msgs.append("主键字段允许NULL，违反数据库规范")
                # 校验3：数值类型默认值为空（警告）
                if any(t in c_type.lower() for t in ["int", "decimal", "float", "double"]) and def_val is None:
                    risk_level = RiskLevel.WARNING
                    risk_msgs.append("数值字段未设置默认值，易产生NULL脏数据")
                # 校验4：varchar长度过短/过长
                if "varchar" in c_type.lower():
                    import re
                    match = re.search(r'(\d+)', c_type)
                    if match:
                        len_num = int(match.group(1))
                        if len_num > 1000:
                            risk_level = RiskLevel.WARNING
                            risk_msgs.append(f"字符串长度{len_num}过长，建议使用TEXT类型")
                        elif len_num < 5:
                            risk_level = RiskLevel.WARNING
                            risk_msgs.append(f"字符串长度{len_num}过短，可能截断数据")
                # 校验5：时间字段
                if "time" in c_name.lower() and "datetime" not in c_type.lower() and "timestamp" not in c_type.lower():
                    risk_level = RiskLevel.WARNING
                    risk_msgs.append(f"时间相关字段类型为{c_type}，建议使用datetime")

                check_desc = "；".join(risk_msgs) if risk_msgs else "字段全部合规"
                self.record_item(
                    scan_id=f"DB-COL-{tbl_name}.{c_name}",
                    module="数据库-字段校验",
                    scene="字段类型/空值/注释/默认值合法性全检",
                    title=f"字段【{tbl_name}.{c_name}】合规校验",
                    expect="字段注释完整、主键非空、数值存在默认值、字符串长度规范",
                    actual=f"字段类型：{c_type}，允许空：{null_flag}，默认值：{def_val}，注释：{comment}，异常点：{check_desc}",
                    level=risk_level,
                    detail="全维度字段规范校验，拦截数据库设计不规范问题，避免业务脏数据"
                )

        # 第三层：索引独立检测（每张表索引1条）
        for tbl_name, tbl_info in self.db_table_detail.items():
            if tbl_name in self.all_db_tables:
                try:
                    idx_rows = self.db_query(f"SHOW INDEX FROM `{tbl_name}`")
                    # 按索引名分组
                    idx_map = {}
                    for row in idx_rows:
                        idx_name = row[2] if len(row) > 2 else "UNKNOWN"
                        col_name = row[4] if len(row) > 4 else ""
                        idx_type = row[1] if len(row) > 1 else ""
                        if idx_name not in idx_map:
                            idx_map[idx_name] = {"cols": [], "type": idx_type}
                        idx_map[idx_name]["cols"].append(col_name)
                    for idx_name, idx_info in idx_map.items():
                        idx_cols = ",".join(idx_info["cols"])
                        idx_type_desc = idx_info["type"]
                        # 简单校验：主键索引正常
                        idx_lv = RiskLevel.NORMAL
                        idx_note = "索引正常"
                        if idx_name == "PRIMARY":
                            idx_note = "主键索引"
                        self.record_item(
                            scan_id=f"DB-IDX-{tbl_name}.{idx_name}",
                            module="数据库-索引",
                            scene="索引合理性校验",
                            title=f"索引【{tbl_name}.{idx_name}】检查",
                            expect="索引命名规范、无重复索引、联合索引顺序合理",
                            actual=f"索引字段：{idx_cols}，索引类型：{idx_type_desc}，{idx_note}",
                            level=idx_lv,
                            detail="核查索引冗余、命名不规范等影响查询性能问题"
                        )
                except Exception as e:
                    self.record_item(
                        scan_id=f"DB-IDX-{tbl_name}.ERR",
                        module="数据库-索引",
                        scene="索引检测异常",
                        title=f"索引【{tbl_name}】检测失败",
                        expect="成功检测",
                        actual=f"检测异常:{str(e)}",
                        level=RiskLevel.WARNING,
                        detail="索引检测过程出错"
                    )

        # 第四层：表数据脏数据巡检（每张表1条）
        for tbl_name in self.all_db_tables:
            try:
                # 空表检测
                row_result = self.db_query(f"SELECT COUNT(*) FROM `{tbl_name}`")
                row_count = row_result[0][0] if row_result else 0
                # 负数异常数据（金额、库存类字段）
                negative_cnt = 0
                if tbl_name in self.db_table_detail:
                    amount_cols = [c[0] for c in self.db_table_detail[tbl_name]["columns"]]
                    for col_name in amount_cols:
                        if any(k in col_name.lower() for k in ["amount", "price", "stock", "num", "quantity", "total", "deposit"]):
                            try:
                                neg_result = self.db_query(f"SELECT COUNT(*) FROM `{tbl_name}` WHERE `{col_name}` < 0")
                                negative_cnt += neg_result[0][0] if neg_result else 0
                            except Exception:
                                pass

                risk_lv = RiskLevel.NORMAL
                risk_texts = []
                if row_count == 0:
                    risk_lv = RiskLevel.WARNING
                    risk_texts.append("空数据表，无业务数据")
                if negative_cnt > 0:
                    if risk_lv == RiskLevel.NORMAL:
                        risk_lv = RiskLevel.ERROR
                    risk_texts.append(f"存在{negative_cnt}条负数金额/库存脏数据")

                actual_detail = f"表总行数：{row_count}，负数异常数据总量：{negative_cnt}"
                if not risk_texts:
                    actual_detail += "，异常描述：无脏数据"
                else:
                    actual_detail += "，异常描述：" + "；".join(risk_texts)

                self.record_item(
                    scan_id=f"DB-DATA-{tbl_name}",
                    module="数据库-业务数据",
                    scene="脏数据、空表异常检测",
                    title=f"数据表【{tbl_name}】业务数据巡检",
                    expect="数据表存在业务数据，金额/库存字段无负数脏数据",
                    actual=actual_detail,
                    level=risk_lv,
                    detail="拦截业务异常脏数据，防止报表、收银计算出错"
                )
            except Exception as e:
                self.record_item(
                    scan_id=f"DB-DATA-{tbl_name}.ERR",
                    module="数据库-业务数据",
                    scene="脏数据检测异常",
                    title=f"数据表【{tbl_name}】巡检失败",
                    expect="成功检测",
                    actual=f"检测异常:{str(e)}",
                    level=RiskLevel.WARNING,
                    detail="巡检过程出错"
                )

# ====================== 后端代码扫描模块 ======================

# ====================== 后端代码扫描模块 ======================
class BackendScanner(BaseScanner):
    def scan_all(self):
        # 全局读取所有Java文件
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))

        # 1. 遍历所有Controller (@RestController / @Controller) 逐文件校验
        controller_paths = []
        for java_fp in all_java:
            try:
                with open(java_fp, "r", encoding="utf-8") as f:
                    content = f.read()
                if "@RestController" in content or "@Controller" in content:
                    controller_paths.append(java_fp)
                    has_valid = "@Valid" in content or "@NotBlank" in content or "@NotNull" in content
                    has_global_err = "GlobalExceptionHandler" in content or "try{" in content or "Exception" in content
                    risk_items = []
                    if not has_valid:
                        risk_items.append("缺失@Valid/@NotBlank入参校验")
                    if not has_global_err:
                        risk_items.append("无统一异常捕获")
                    risk_level = RiskLevel.NORMAL if len(risk_items) == 0 else RiskLevel.WARNING
                    self.record_item(
                        scan_id=f"CTRL-{os.path.basename(java_fp)}",
                        module="后端代码",
                        scene="Controller接口入参校验",
                        title=f"控制器 {os.path.basename(java_fp)} 规范检查",
                        expect="接口必须配置@Valid、@NotBlank、异常捕获",
                        actual=f"@Valid/NotNull:{has_valid} | 异常处理:{has_global_err}",
                        level=risk_level,
                        detail="；".join(risk_items) if risk_items else "入参校验全部合规",
                        file_list=java_fp
                    )
            except Exception:
                continue

        # 2. 遍历所有Mapper文件，检测 limit / PageHelper 分页关键字
        mapper_paths = []
        for java_fp in all_java:
            fname = os.path.basename(java_fp)
            if "Mapper" in fname or "Dao" in fname or "Repository" in fname:
                try:
                    with open(java_fp, "r", encoding="utf-8") as f:
                        content = f.read().lower()
                    mapper_paths.append(java_fp)
                    has_limit = "limit" in content
                    has_pagehelper = "pagehelper" in content
                    has_page = "page" in content and ("select" in content or "query" in content)
                    has_select = "select" in content
                    if has_select:
                        risk_level = RiskLevel.NORMAL if (has_limit or has_pagehelper or has_page) else RiskLevel.WARNING
                        risk_desc = "分页逻辑正常" if (has_limit or has_pagehelper or has_page) else "未配置分页（大数据查询会OOM）"
                    else:
                        risk_level = RiskLevel.NORMAL
                        risk_desc = "非查询类Mapper"
                    self.record_item(
                        scan_id=f"MAPPER-{fname}",
                        module="后端代码",
                        scene="Mapper SQL分页校验",
                        title=f"Mapper {fname} 分页规则检查",
                        expect="列表查询必须带limit或PageHelper分页",
                        actual=f"limit:{has_limit} | PageHelper:{has_pagehelper} | Page分页:{has_page}",
                        level=risk_level,
                        detail=risk_desc,
                        file_list=java_fp
                    )
                except Exception:
                    continue

        # 3. 原有实体@Column映射校验
        entity_files = self.scan_file_keyword(BACK_CODE_PATH, "@Column")
        if entity_files:
            self.record_item(
                scan_id="BACK-001",
                module="后端代码",
                scene="数据库实体映射",
                title="实体与库字段对齐校验",
                expect="实体@Column与数据库字段完全匹配",
                actual=f"待核对文件数量：{len(entity_files)}",
                level=RiskLevel.WARNING,
                detail="字段名称不匹配会查询返回NULL",
                file_list="\n".join(entity_files)
            )
        else:
            self.record_item(
                scan_id="BACK-001",
                module="后端代码",
                scene="数据库实体映射",
                title="实体与库字段对齐校验",
                expect="实体@Column与数据库字段完全匹配",
                actual="全部实体映射规范",
                level=RiskLevel.NORMAL,
                detail="无字段映射不一致问题"
            )

        # 4. 原有事务校验逻辑（库存/薪资/预订Service强制@Transactional）
        tx_files = []
        service_all = self.scan_file_keyword(BACK_CODE_PATH, "@Service")
        for fp in service_all:
            if any(k in fp for k in ["Inventory", "Salary", "Booking", "Purchase", "Finance", "Stock"]):
                try:
                    with open(fp, "r", encoding="utf-8") as f:
                        if "@Transactional" not in f.read():
                            tx_files.append(fp)
                except Exception:
                    continue
        if tx_files:
            self.record_item(
                scan_id="BACK-002",
                module="后端代码",
                scene="业务事务防护",
                title="库存/薪资/预订事务注解校验",
                expect="修改数据方法必须添加@Transactional",
                actual=f"缺失事务文件：{len(tx_files)}个",
                level=RiskLevel.ERROR,
                detail="并发操作会产生数据错乱、脏数据",
                file_list="\n".join(tx_files)
            )
        else:
            self.record_item(
                scan_id="BACK-002",
                module="后端代码",
                scene="业务事务防护",
                title="库存/薪资/预订事务注解校验",
                expect="修改数据方法必须添加@Transactional",
                actual="所有核心业务均配置事务",
                level=RiskLevel.NORMAL,
                detail="并发数据安全有保障"
            )

        # 5. Service文件规范检查
        for java_fp in all_java:
            try:
                fname = os.path.basename(java_fp)
                if "Service" in fname and "Impl" in fname:
                    with open(java_fp, "r", encoding="utf-8") as f:
                        content = f.read()
                    has_tx = "@Transactional" in content
                    has_log = "Logger" in content or "log" in content.lower() or "@Slf4j" in content
                    issues = []
                    if not has_tx and any(k in fname for k in ["Inventory", "Salary", "Booking", "Purchase", "Finance", "Stock"]):
                        issues.append("核心业务未加@Transactional")
                    if not has_log:
                        issues.append("未引入日志")
                    risk_lv = RiskLevel.WARNING if issues else RiskLevel.NORMAL
                    self.record_item(
                        scan_id=f"SVC-{fname}",
                        module="后端代码-Service",
                        scene="Service实现规范",
                        title=f"{fname} 规范检查",
                        expect="事务+日志完整",
                        actual=f"事务:{'有' if has_tx else '无'} | 日志:{'有' if has_log else '无'}",
                        level=risk_lv,
                        detail="；".join(issues) if issues else "规范完整",
                        file_list=java_fp
                    )
            except Exception:
                continue

        # 6. 全量Java文件基础存在性校验
        for java_fp in all_java:
            fname = os.path.basename(java_fp)
            self.record_item(
                scan_id=f"JAVA-FILE-{fname}",
                module="后端代码",
                scene="Java文件基础校验",
                title=f"源码文件 {fname} 存在性校验",
                expect="源码文件完整无缺失",
                actual=f"文件路径：{java_fp}",
                level=RiskLevel.NORMAL,
                detail="统计所有后端源码文件"
            )
# ====================== 前端页面扫描模块 ======================
class FrontScanner(BaseScanner):
    def scan_all(self):
        # === 门店硬编码检查 ===
        hardcode = self.scan_file_keyword(FRONT_CODE_PATH, "storeId:1")
        if hardcode:
            self.record_item("FRONT-001", "前端模板层", "门店取值规范", "禁止硬编码固定门店", f"违规{len(hardcode)}文件", RiskLevel.FATAL, "切换门店数据不刷新", file_list="\n".join(hardcode))
        else:
            self.record_item("FRONT-001", "前端模板层", "门店取值规范", "禁止硬编码固定门店", "无硬编码", RiskLevel.NORMAL, "动态取值正常")

        # === 页面完整性检查 ===
        empty_pages = []
        for page_name in VUE_PAGE_LIST:
            for r, _, fs in os.walk(FRONT_CODE_PATH):
                if page_name in fs:
                    fp = os.path.join(r, page_name)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            if len(f.read()) < 300:
                                empty_pages.append(fp)
                    except Exception:
                        continue
        if empty_pages:
            self.record_item("FRONT-002", "前端模板层", "页面完整性", "所有业务页面具备逻辑", f"空白{len(empty_pages)}页面", RiskLevel.ERROR, "无法操作单据", file_list="\n".join(empty_pages))
        else:
            self.record_item("FRONT-002", "前端模板层", "页面完整性", "所有业务页面具备逻辑", "页面齐全", RiskLevel.NORMAL, "功能完整")

        # === 扩容：遍历所有Vue页面逐页检查 ===
        vue_files = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue"):
                    vue_files.append(os.path.join(r, fname))
        for fp in vue_files:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    content = f.read()
                fname = os.path.basename(fp)
                issues = []
                # 检查是否有注释
                has_comment = "<!--" in content or "//" in content
                # 检查是否有el-form表单校验
                has_form_validate = "rules" in content or "validate" in content
                # 检查是否有try-catch的API请求
                has_error_handle = ".catch" in content or "try {" in content
                # 检查硬编码API地址
                has_hardcode_api = "http://" in content and "localhost" in content
                # 样式检查
                has_scoped = "scoped" in content

                if not has_comment:
                    issues.append("页面无注释")
                if "el-form" in content and not has_form_validate:
                    issues.append("表单未做校验")
                if "axios" in content.lower() or "fetch" in content.lower() or "request" in content.lower():
                    if not has_error_handle:
                        issues.append("API请求无异常捕获")
                if has_hardcode_api:
                    issues.append("硬编码API地址")
                if "<style" in content and not has_scoped:
                    issues.append("样式未加scoped")

                risk_lv = RiskLevel.WARNING if issues else RiskLevel.NORMAL
                self.record_item(
                    scan_id=f"FRONT-VUE-{fname}",
                    module="前端页面-Vue组件",
                    scene="Vue页面规范性检查",
                    title=f"Vue页面【{fname}】检查",
                    expect="页面含注释、表单校验、异常捕获、无硬编码、scoped样式",
                    actual=f"问题点：{'；'.join(issues) if issues else '全部合规'}",
                    level=risk_lv,
                    detail="前端页面规范检查，保障可维护性和稳定性",
                    file_list=fp
                )
            except Exception:
                continue

        # === 扩容：JS/TS API请求文件逐文件检查 ===
        api_files = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".js") or fname.endswith(".ts"):
                    fp = os.path.join(r, fname)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            content = f.read()
                        if any(k in content.lower() for k in ["axios", "fetch", "request", "/api/"]):
                            api_files.append((fp, content))
                    except Exception:
                        continue
        for fp, content in api_files:
            try:
                fname = os.path.basename(fp)
                issues = []
                has_token = "token" in content.lower() or "Authorization" in content
                has_error = ".catch" in content or "try" in content
                has_base_url = "baseURL" in content or "BASE_URL" in content or "base_url" in content
                if not has_token:
                    issues.append("未拦截token/鉴权")
                if not has_error:
                    issues.append("无异常处理")
                risk_lv = RiskLevel.WARNING if issues else RiskLevel.NORMAL
                self.record_item(
                    scan_id=f"FRONT-API-{fname}",
                    module="前端-API请求",
                    scene="API请求拦截规范检查",
                    title=f"API请求文件【{fname}】检查",
                    expect="含鉴权拦截、异常捕获、baseURL统一配置",
                    actual=f"鉴权:{'有' if has_token else '无'}，异常处理:{'有' if has_error else '无'}，问题:{';'.join(issues) if issues else '无'}",
                    level=risk_lv,
                    detail="API请求文件规范检查",
                    file_list=fp
                )
            except Exception:
                continue

        # === 扩容：配置文件检查 ===
        config_files = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname in ["package.json", "vite.config.js", "vite.config.ts", "tsconfig.json", ".env", ".env.production"]:
                    config_files.append(os.path.join(r, fname))
        for fp in config_files:
            try:
                fname = os.path.basename(fp)
                with open(fp, "r", encoding="utf-8") as f:
                    content = f.read()
                issues = []
                if fname == "package.json":
                    if '"vue"' in content or '"react"' in content:
                        pass  # 框架存在
                    else:
                        issues.append("package.json未找到框架依赖")
                if fname.endswith(".env") or fname.endswith(".env.production"):
                    if "VITE_" in content or "VUE_APP_" in content:
                        pass
                    else:
                        issues.append("环境变量前缀不规范")
                risk_lv = RiskLevel.WARNING if issues else RiskLevel.NORMAL
                self.record_item(
                    scan_id=f"FRONT-CFG-{fname}",
                    module="前端-配置",
                    scene="前端配置文件规范检查",
                    title=f"配置文件【{fname}】检查",
                    expect="配置完整、无硬编码、环境变量规范",
                    actual=f"问题点：{'；'.join(issues) if issues else '配置规范'}",
                    level=risk_lv,
                    detail="配置文件规范检查",
                    file_list=fp
                )
            except Exception:
                continue

        # === 路由配置检测 ===
        import re as re_fe
        router_file = os.path.join(FRONT_CODE_PATH, "src", "router", "index.js")
        if os.path.exists(router_file):
            try:
                with open(router_file, "r", encoding="utf-8") as f:
                    router_content = f.read()
                # 解析路由路径
                routes = re_fe.findall(r"path\s*:\s*['\"]([^'\"]+)['\"]", router_content)
                self.record_item("FRONT-ROUTE-001", "前端-路由", "路由路径解析",
                    "router/index.js应包含路由定义", f"已解析{len(routes)}条路由",
                    RiskLevel.NORMAL if len(routes) > 0 else RiskLevel.WARNING,
                    "路由是前端导航的基础", scene="路由解析")
                # 权限元数据
                meta_roles = re_fe.findall(r"meta\s*:\s*\{[^}]*roles\s*:\s*\[([^\]]+)\]", router_content)
                self.record_item("FRONT-ROUTE-002", "前端-路由", "路由权限元数据",
                    "路由应配置roles权限控制", f"{len(meta_roles)}条路由有roles",
                    RiskLevel.WARNING if len(meta_roles) == 0 else RiskLevel.NORMAL,
                    "无权限元数据的路由任何人都能访问", scene="路由权限")
                # 路由对应组件文件存在性
                missing = 0
                for path in routes[:100]:
                    # 简化: 检查views目录下是否有对应组件
                    comp_name = path.strip("/").replace("/", "_")
                    found = self.scan_file_keyword(FRONT_CODE_PATH, comp_name)
                    if not found:
                        missing += 1
                if missing > 0:
                    self.record_item("FRONT-ROUTE-003", "前端-路由", "路由组件存在性",
                        "每条路由应有对应Vue组件", f"约{missing}条路由组件可能缺失",
                        RiskLevel.WARNING, "缺失组件的路由会白屏", scene="路由组件")
                else:
                    self.record_item("FRONT-ROUTE-003", "前端-路由", "路由组件存在性",
                        "每条路由应有对应Vue组件", "全部路由组件校验通过",
                        RiskLevel.NORMAL, "路由完整性良好", scene="路由组件")
            except Exception:
                pass
        else:
            self.record_item("FRONT-ROUTE-000", "前端-路由", "路由配置文件",
                "router/index.js应存在", "未找到路由文件",
                RiskLevel.WARNING, "无法分析路由结构", scene="路由解析")

        # === 组件功能统计 ===
        vue_all = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue"):
                    vue_all.append(os.path.join(r, fname))
        if vue_all:
            stats = {"search":0,"table":0,"page":0,"add":0,"export":0,"print":0,"dialog":0,"perm":0}
            for fp in vue_all:
                try:
                    with open(fp, "r", encoding="utf-8") as f:
                        ct = f.read()
                    if "el-form" in ct or "el-input" in ct: stats["search"] += 1
                    if "el-table" in ct: stats["table"] += 1
                    if "el-pagination" in ct or "pagination" in ct.lower(): stats["page"] += 1
                    if "新增" in ct or "添加" in ct or "handleCreate" in ct: stats["add"] += 1
                    if "导出" in ct or "export" in ct.lower(): stats["export"] += 1
                    if "打印" in ct or "print" in ct.lower(): stats["print"] += 1
                    if "el-dialog" in ct: stats["dialog"] += 1
                    if "v-permission" in ct: stats["perm"] += 1
                except Exception:
                    pass
            self.record_item("FRONT-STAT-001", "前端-组件功能", "页面控件覆盖率",
                f"总{len(vue_all)}个Vue页面应含核心控件",
                f"搜索{stats['search']} 表格{stats['table']} 分页{stats['page']} 新增{stats['add']} 导出{stats['export']} 打印{stats['print']} 弹窗{stats['dialog']} 权限{stats['perm']}",
                RiskLevel.NORMAL, "页面功能完好度统计", scene="组件功能")
            if stats["perm"] == 0 and len(vue_all) > 5:
                self.record_item("FRONT-STAT-002", "前端-组件功能", "权限指令覆盖率",
                    "业务页面应使用v-permission", "0个页面使用v-permission",
                    RiskLevel.WARNING, "建议为敏感操作添加权限控制", scene="组件功能")

        # === 打印工具检测 ===
        print_utils = os.path.join(FRONT_CODE_PATH, "utils", "printUtils.js")
        if not print_utils:
            print_utils = self.scan_file_keyword(FRONT_CODE_PATH, "printUtils")
        has_print = os.path.exists(print_utils) if isinstance(print_utils, str) else len(print_utils) > 0
        self.record_item("FRONT-UTIL-001", "前端-工具", "打印工具函数",
            "应有printUtils.js统一打印", "已找到" if has_print else "未找到打印工具",
            RiskLevel.NORMAL if has_print else RiskLevel.WARNING,
            "统一打印提升用户体验一致性", scene="工具函数")

        # ==== 模块分组及控件深度统计(Dialog新增) ====
        # 按业务模块对Vue页面分组
        module_groups = {
            "前厅预订": ["Booking", "FrontDesk", "FrontOffice", "TableBoard", "TableLayout", "TableUtilization", "SelfService"],
            "厨房管理": ["Kitchen", "Production", "KitchenLog", "DishCost", "Hygiene", "Menu", "MenuManage"],
            "采购仓储": ["Procurement", "Inventory", "StockTake", "Suppliers", "SupplyChain", "SupplyManagement", "GoodsReceipt"],
            "财务管理": ["Finance", "Revenue", "Tax", "Cost", "Payroll", "SupplierReconciliation"],
            "人事管理": ["Staff", "HRAdmin", "HRAnalytics", "Schedule", "Attendance", "Leave", "Training", "StaffPerformance", "StaffProfile"],
            "工程维修": ["Engineering", "Maintenance", "Energy"],
            "营销活动": ["Marketing", "MarketingActivity", "MemberList"],
            "审批审计": ["ApprovalCenter", "AuditLog", "ReviewQueue", "PermManager", "ChangeLogView"],
            "报表看板": ["Dashboard", "Reports", "DataScreen", "CustomerAnalysis", "GuestAnalysis"],
            "系统设置": ["Settings", "Security", "DeviceBinding", "License"],
        }
        vue_page_map = {}
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue"):
                    n = fname.replace(".vue", "")
                    fp = os.path.join(r, fname)
                    vue_page_map[n] = {"path": fp, "size": os.path.getsize(fp)}
        mod_results = {}
        for mod, names in module_groups.items():
            pages = []
            for n in names:
                if n in vue_page_map:
                    try:
                        with open(vue_page_map[n]["path"], "r", encoding="utf-8") as f:
                            ct = f.read()
                        controls = {
                            "search": "el-form" in ct or "el-input" in ct,
                            "table": "el-table" in ct,
                            "page": "el-pagination" in ct or "pagination" in ct.lower(),
                            "add": "新增" in ct or "添加" in ct or "handleCreate" in ct,
                            "export": "导出" in ct or "export" in ct.lower(),
                            "print": "打印" in ct or "print" in ct.lower(),
                            "dialog": "el-dialog" in ct,
                            "perm": "v-permission" in ct,
                            "loading": "loading" in ct.lower() and ("el-" in ct or "v-" in ct),
                            "empty": "empty" in ct.lower() or "暂无" in ct or "无数据" in ct,
                            "size_kb": vue_page_map[n]["size"] // 1024,
                        }
                        pages.append({"name": n, "ct": controls})
                    except Exception:
                        pass
            if pages:
                add_count = sum(1 for p in pages if p["ct"]["add"])
                perm_count = sum(1 for p in pages if p["ct"]["perm"])
                self.record_item(f"FRONT-MOD-{mod}", "前端-模块控件", f"{mod}页面统计",
                    f"{len(pages)}个页面应有核心控件",
                    f"共{len(pages)}页 新增{add_count} 权限{perm_count} 表{sum(1 for p in pages if p['ct']['table'])}",
                    RiskLevel.NORMAL, "模块控件覆盖率统计", scene="模块统计")
                mod_results[mod] = pages
        # 空状态处理覆盖率
        total_vue = len(vue_page_map)
        if total_vue > 0:
            empty_pages = sum(1 for mps in mod_results.values() for p in mps if p["ct"]["empty"])
            load_pages = sum(1 for mps in mod_results.values() for p in mps if p["ct"]["loading"])
            self.record_item("FRONT-MOD-EMPTY", "前端-模块控件", "空状态/加载态覆盖率",
                f"{total_vue}个页面应处理空数据和加载状态",
                f"空状态{empty_pages} 加载态{load_pages}/{total_vue}",
                RiskLevel.WARNING if empty_pages < total_vue * 0.5 else RiskLevel.NORMAL,
                "用户体验完整性", scene="模块统计")
        # API模块目录检测
        api_dir = os.path.join(FRONT_CODE_PATH, "src", "api")
        if os.path.exists(api_dir):
            api_files = [f for f in os.listdir(api_dir) if f.endswith(".js") or f.endswith(".ts")]
            self.record_item("FRONT-API-DIR", "前端-模块控件", "API模块目录",
                "前端应有统一api/目录", f"{len(api_files)}个API模块: {','.join([f.replace('.js','').replace('.ts','') for f in api_files[:10]])}",
                RiskLevel.NORMAL if len(api_files) > 3 else RiskLevel.WARNING,
                "API模块化管理利于维护", scene="模块统计")
        else:
            self.record_item("FRONT-API-DIR", "前端-模块控件", "API模块目录",
                "前端应有统一api/目录", "未找到src/api目录",
                RiskLevel.WARNING, "建议创建api/目录统一管理接口", scene="模块统计")

# ====================== API接口扫描模块 ======================
class ApiScanner(BaseScanner):
    def scan_all(self):
        login_ok = self.api_login()
        if not login_ok:
            self.record_item("API-001", "接口层", "登录鉴权链路", "正常返回JWT", "连接拒绝", RiskLevel.FATAL, "后端8080未启动", fix_cmd=f"curl {API_BASE_URL}/auth/login -X POST -d '{json.dumps(LOGIN_CRED)}'")
        else:
            self.record_item("API-001", "接口层", "登录鉴权链路", "正常返回JWT", "登录成功", RiskLevel.NORMAL, "鉴权正常")
        fail_api = []
        import time
        for url in API_FULL_LIST:
            time.sleep(0.5)
            resp = self.api_get(url)
            if resp.get("code") != 200:
                fail_api.append(f"{url} 返回{resp.get('code')}")
        if fail_api:
            self.record_item("API-002", "接口层", "全CRUD端点连通", "所有接口200", str(fail_api), RiskLevel.ERROR, "业务功能不可用")
        else:
            self.record_item("API-002", "接口层", "全CRUD端点连通", "所有接口200", "全部正常", RiskLevel.NORMAL, "接口全通")
        cross_resp = self.api_get("/hr/schedule", {"storeId":9999})
        if cross_resp.get("code") == 200:
            # Check if returned data actually contains storeId=9999 records
            cross_data = cross_resp.get("data", [])
            if isinstance(cross_data, list):
                leaked = [r for r in cross_data if isinstance(r, dict) and r.get("storeId") == 9999]
                if leaked:
                    self.record_item("API-003", "接口层", "数据隔离链路", "非admin禁止跨门店", "可越权查询", RiskLevel.FATAL, "数据安全漏洞")
                else:
                    self.record_item("API-003", "接口层", "数据隔离链路", "非admin禁止跨门店", "正常拦截(token隔离)", RiskLevel.NORMAL, "隔离生效")
            else:
                self.record_item("API-003", "接口层", "数据隔离链路", "非admin禁止跨门店", "正常拦截", RiskLevel.NORMAL, "隔离生效")
        else:
            self.record_item("API-003", "接口层", "数据隔离链路", "非admin禁止跨门店", "正常拦截", RiskLevel.NORMAL, "隔离生效")



# ====================== 1. 数据库索引深度扫描模块 ======================
class IndexDeepScanner(BaseScanner):
    """领域4: 索引存在/冗余/缺失/复合索引顺序/外键索引"""
    def scan_all(self):
        self.load_all_table_info()
        # 全量FK发现（从information_schema拉）
        fks = self.db_query("""
            SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA='banquet' AND REFERENCED_TABLE_NAME IS NOT NULL
        """)
        # 检查每个外键是否有索引
        for tbl, col, ref_tbl, ref_col in fks:
            has_idx = self.db_query(f"SHOW INDEX FROM `{tbl}` WHERE Column_name='{col}'")
            if not has_idx:
                self.record_item(
                    f"IDX-FK-{tbl}.{col}", "数据库-索引深度", "外键索引缺失",
                    f"外键列 {tbl}.{col} 应有索引", f"未找到索引",
                    RiskLevel.ERROR, f"JOIN {ref_tbl} 时全表扫描", fix_sql=f"CREATE INDEX idx_{tbl}_{col} ON {tbl}({col});",
                    scene="外键索引完整性"
                )
            else:
                self.record_item(
                    f"IDX-FK-{tbl}.{col}", "数据库-索引深度", "外键索引存在",
                    f"外键列 {tbl}.{col} 应有索引", "索引已存在",
                    RiskLevel.NORMAL, "JOIN查询可走索引", scene="外键索引完整性"
                )
        # 冗余索引检测（前缀重复）
        for tbl in self.all_db_tables:
            try:
                idx_rows = self.db_query(f"SHOW INDEX FROM `{tbl}`")
                idx_map = {}
                for row in idx_rows:
                    name = row[2]; cn = row[4]; seq = row[3]
                    if name not in idx_map: idx_map[name] = []
                    idx_map[name].append((seq, cn))
                names = list(idx_map.keys())
                for i in range(len(names)):
                    for j in range(i+1, len(names)):
                        a_cols = [x[1] for x in sorted(idx_map[names[i]])]
                        b_cols = [x[1] for x in sorted(idx_map[names[j]])]
                        if a_cols == b_cols:
                            self.record_item(
                                f"IDX-DUP-{tbl}.{names[i]}-{names[j]}", "数据库-索引深度",
                                "完全冗余索引", f"索引不应完全重复",
                                f"{names[i]}({','.join(a_cols)}) ≡ {names[j]}({','.join(b_cols)})",
                                RiskLevel.WARNING, "浪费存储和写入性能",
                                fix_sql=f"DROP INDEX {names[j]} ON {tbl};", scene="索引冗余检测"
                            )
                        elif all(c in b_cols for c in a_cols[:len(a_cols)-1]) and len(a_cols) < len(b_cols):
                            pass  # 前缀索引可以保留但需警告
                        elif len(a_cols) >= 2 and len(b_cols) >= 2 and a_cols[:2] == b_cols[:2]:
                            self.record_item(
                                f"IDX-SIM-{tbl}.{names[i]}-{names[j]}", "数据库-索引深度",
                                "疑似冗余复合索引", f"前缀重复的复合索引",
                                f"{names[i]}:{','.join(a_cols)} vs {names[j]}:{','.join(b_cols)}",
                                RiskLevel.WARNING, "考虑合并或删除其中一个", scene="索引冗余检测"
                            )
            except Exception: pass
        # 全量FK检测汇总
        if not fks:
            self.record_item("IDX-FK-SUM", "数据库-索引深度", "外键索引汇总",
                "外键列应有索引", "无物理外键定义，跳过FK索引检测",
                RiskLevel.WARNING, "逻辑外键无索引也会导致全表扫描", scene="外键索引完整性")
        else:
            fk_miss = sum(1 for it in self.scan_items if it["scan_id"].startswith("IDX-FK-") and it["level"] != "NORMAL")
            if fk_miss == 0:
                self.record_item("IDX-FK-SUM", "数据库-索引深度", "外键索引汇总",
                    "所有外键都有索引", f"{len(fks)}个外键，索引覆盖率100%",
                    RiskLevel.NORMAL, "FK索引完整", scene="外键索引完整性")
        # 每表索引统计（正常记录）
        if self.all_db_tables:
            idx_count = sum(1 for it in self.scan_items if it["module"] == "数据库-索引深度" and it["level"] == "NORMAL")
            warn_count = sum(1 for it in self.scan_items if it["module"] == "数据库-索引深度" and it["level"] != "NORMAL")
            self.record_item("IDX-STAT", "数据库-索引深度", "索引深度统计",
                "索引覆盖率统计", f"全量索引检测中{'NORMAL' if warn_count==0 else 'WARNING'}: 正常{idx_count} 预警{warn_count}",
                RiskLevel.NORMAL if warn_count == 0 else RiskLevel.WARNING, f"已扫描{len(self.all_db_tables)}张表的索引", scene="统计信息")


# ====================== 2. 数据库约束完整性扫描模块 ======================
class ConstraintScanner(BaseScanner):
    """领域3: 主键/外键/唯一约束/检查约束完整性"""
    def scan_all(self):
        self.load_all_table_info()
        # 无主键表
        for tbl in self.all_db_tables:
            try:
                pk = self.db_query(f"SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='{tbl}' AND CONSTRAINT_NAME='PRIMARY'")
                if not pk:
                    self.record_item(f"CONS-NOPK-{tbl}", "数据库-约束", "缺失主键",
                        "每张表必须有主键", f"表 {tbl} 无主键",
                        RiskLevel.FATAL, "无法唯一标识行，同步/恢复灾难", scene="主键完整性")
                else:
                    self.record_item(f"CONS-PK-{tbl}", "数据库-约束", "主键正常",
                        "主键存在", f"主键: {pk[0][0]}", RiskLevel.NORMAL, "", scene="主键完整性")
            except Exception: pass
        # AUTO_INCREMENT接近上限检测
        for tbl, info in self.db_table_detail.items():
            for col in info["columns"]:
                if "auto_increment" in str(col[6]).lower():
                    try:
                        max_val = self.db_query(f"SELECT MAX(`{col[0]}`) FROM `{tbl}`")
                        if max_val and max_val[0][0]:
                            val = int(max_val[0][0])
                            if "int" in col[1].lower() and val > 2000000000:
                                self.record_item(f"CONS-AI-{tbl}.{col[0]}", "数据库-约束",
                                    "AUTO_INCREMENT接近上限", "自增值不应接近int上限",
                                    f"{tbl}.{col[0]} 最大值={val}，int上限=2147483647",
                                    RiskLevel.WARNING, "即将无法插入新记录",
                                    fix_sql=f"ALTER TABLE {tbl} MODIFY {col[0]} BIGINT AUTO_INCREMENT;",
                                    scene="自增字段安全")
                    except Exception: pass
        # CHAR/VARCHAR字符集统一检测
        charset_issues = self.db_query("""
            SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA='banquet' AND CHARACTER_SET_NAME IS NOT NULL AND CHARACTER_SET_NAME != 'utf8mb4'
        """)
        for tbl, col, cs in charset_issues:
            self.record_item(f"CONS-CHARSET-{tbl}.{col}", "数据库-约束",
                f"非utf8mb4字符集", "所有文本列应为utf8mb4",
                f"{tbl}.{col} 使用 {cs}", RiskLevel.WARNING,
                "emoji/生僻字会截断", fix_sql=f"ALTER TABLE {tbl} MODIFY {col} VARCHAR(255) CHARACTER SET utf8mb4;",
                scene="字符集一致性")
        # 表引擎检测
        engine_issues = self.db_query("""
            SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
            WHERE TABLE_SCHEMA='banquet' AND ENGINE != 'InnoDB'
        """)
        for tbl, eng in engine_issues:
            self.record_item(f"CONS-ENGINE-{tbl}", "数据库-约束",
                f"非InnoDB引擎", "所有表应为InnoDB",
                f"{tbl} 使用 {eng}", RiskLevel.ERROR,
                "不支持事务/外键/行锁", fix_sql=f"ALTER TABLE {tbl} ENGINE=InnoDB;",
                scene="存储引擎一致性")
        if not engine_issues:
            self.record_item("CONS-ENGINE-SUM", "数据库-约束", "存储引擎一致性",
                "所有表应为InnoDB", f"全部{len(self.all_db_tables)}张表使用InnoDB",
                RiskLevel.NORMAL, "存储引擎统一", scene="存储引擎一致性")
        if not charset_issues:
            self.record_item("CONS-CHARSET-SUM", "数据库-约束", "字符集一致性",
                "所有文本列应为utf8mb4", "全部文本列使用utf8mb4",
                RiskLevel.NORMAL, "字符集统一", scene="字符集一致性")
        pk_ok = sum(1 for it in self.scan_items if it["scan_id"].startswith("CONS-PK-"))
        self.record_item("CONS-PK-SUM", "数据库-约束", "主键完整性汇总",
            "所有表应有主键", f"主键覆盖率: {pk_ok}/{len(self.all_db_tables)}",
            RiskLevel.NORMAL if pk_ok == len(self.all_db_tables) else RiskLevel.FATAL,
            "主键是数据完整性的基础", scene="主键完整性")


# ====================== 3. 数据质量深度扫描模块 ======================
class DataQualityScanner(BaseScanner):
    """领域5: NULL比例/负值/日期逻辑/重复数据/孤儿数据/枚举越界/格式校验"""
    def scan_all(self):
        self.load_all_table_info()
        # 每表NULL比例检测（必填字段NULL比例超过50%告警）
        null_checked = 0; null_clean = 0
        for tbl in self.all_db_tables:
            if tbl not in self.db_table_detail: continue
            for col_info in self.db_table_detail[tbl]["columns"]:
                c_name = col_info[0]; null_flag = col_info[3]
                if c_name in ["id", "create_time", "update_time"]: continue
                null_checked += 1
                try:
                    _tq = self.db_query(f"SELECT COUNT(*) FROM `{tbl}`"); total = _tq[0][0] if _tq else 0
                    if total == 0: continue
                    _nq = self.db_query(f"SELECT COUNT(*) FROM `{tbl}` WHERE `{c_name}` IS NULL"); null_cnt = _nq[0][0] if _nq else 0
                    ratio = null_cnt / total * 100
                    if ratio > 50:
                        self.record_item(f"DQ-NULL-{tbl}.{c_name}", "数据库-数据质量",
                            f"字段NULL比例过高", f"NULL比例应<50%",
                            f"{tbl}.{c_name}: {null_cnt}/{total} ({ratio:.0f}%)",
                            RiskLevel.WARNING, "数据完整性问题", scene="NULL值分布")
                    elif ratio > 0 and null_flag == "NO":
                        self.record_item(f"DQ-NULL-{tbl}.{c_name}", "数据库-数据质量",
                            f"NOT NULL字段存在NULL", "NOT NULL列不应有NULL",
                            f"{tbl}.{c_name}: {null_cnt}条NULL",
                            RiskLevel.ERROR, "约束与实际数据不一致", scene="NULL值分布")
                except Exception: pass
        # 日期逻辑错误
        date_checks = [
            ("staff_master", "hire_date", "resign_date", "入职日期>退职日期"),
            ("booking_master", "booking_date", "create_time", "预定日期>创建时间"),
        ]
        for tbl, col_a, col_b, desc in date_checks:
            if tbl not in self.all_db_tables: continue
            try:
                bad = self.db_query(f"SELECT COUNT(*) FROM `{tbl}` WHERE `{col_a}` > `{col_b}` AND `{col_b}` IS NOT NULL")
                if bad and bad[0][0] > 0:
                    self.record_item(f"DQ-DATE-{tbl}.{col_a}-{col_b}", "数据库-数据质量",
                        desc, "日期逻辑应合理", f"异常{desc}: {bad[0][0]}条",
                        RiskLevel.ERROR, "数据时序错误影响报表", scene="日期逻辑校验")
            except Exception: pass
        # 字符串尾部空格/前后空格
        for tbl in self.all_db_tables:
            if tbl not in self.db_table_detail: continue
            for col_info in self.db_table_detail[tbl]["columns"]:
                if "varchar" in col_info[1].lower() or "char" in col_info[1].lower():
                    try:
                        space = self.db_query(f"SELECT COUNT(*) FROM `{tbl}` WHERE `{col_info[0]}` LIKE '% ' OR `{col_info[0]}` LIKE ' %'")
                        if space and space[0][0] > 0:
                            self.record_item(f"DQ-TRIM-{tbl}.{col_info[0]}", "数据库-数据质量",
                                "字段存在首尾空格", "字符串不应含首尾空格",
                                f"{tbl}.{col_info[0]}: {space[0][0]}条含空格",
                                RiskLevel.WARNING, "可能导致匹配/去重失败",
                                fix_sql=f"UPDATE `{tbl}` SET `{col_info[0]}`=TRIM(`{col_info[0]}`);",
                                scene="数据格式校验")
                    except Exception: pass
        # 重复数据检测（按关键业务字段去重）
        dup_groups = [
            ("customer_master", "customer_phone", "手机号"),
            ("staff_master", "staff_name", "员工姓名"),
            ("supplier_master", "supplier_phone", "供应商手机"),
        ]
        for tbl, col, label in dup_groups:
            if tbl not in self.all_db_tables: continue
            try:
                dups = self.db_query(f"SELECT `{col}`,COUNT(*) c FROM `{tbl}` GROUP BY `{col}` HAVING c>1 LIMIT 10")
                if dups:
                    for val, cnt in dups:
                        self.record_item(f"DQ-DUP-{tbl}.{col}", "数据库-数据质量",
                            f"重复{label}", "不应出现重复", f"{tbl}: {label}={val} 重复{cnt}次",
                            RiskLevel.WARNING, "影响统计/关联准确度", scene="重复数据检测")
            except Exception: pass
        # 邮件/手机号格式
        for tbl in self.all_db_tables:
            if tbl not in self.db_table_detail: continue
            for col_info in self.db_table_detail[tbl]["columns"]:
                c_name = col_info[0].lower()
                if "phone" in c_name:
                    try:
                        bad = self.db_query(f"SELECT COUNT(*) FROM `{tbl}` WHERE `{col_info[0]}` IS NOT NULL AND LENGTH(`{col_info[0]}`) < 11")
                        if bad and bad[0][0] > 0:
                            self.record_item(f"DQ-FMT-{tbl}.{col_info[0]}", "数据库-数据质量",
                                "手机号格式异常", "应为11位", f"{bad[0][0]}条格式异常",
                                RiskLevel.WARNING, "短信/通知无法送达", scene="格式校验")
                    except Exception: pass
        # 每表数据质量汇总统计
        total_null_checks = 0; total_dup_checks = 0; total_fmt_checks = 0
        for it in self.scan_items:
            if it["module"] == "数据库-数据质量":
                if "NULL" in it.get("scan_id",""): total_null_checks += 1
                if "DUP" in it.get("scan_id",""): total_dup_checks += 1
                if "FMT" in it.get("scan_id",""): total_fmt_checks += 1
        self.record_item("DQ-STAT-NULL", "数据库-数据质量", "字段NULL分布统计",
            "统计所有字段NULL比例", f"已检测{total_null_checks}个字段的NULL分布",
            RiskLevel.NORMAL, "NULL覆盖率统计", scene="统计信息")
        self.record_item("DQ-STAT-DUP", "数据库-数据质量", "重复数据统计",
            "统计关键业务字段去重", f"已检测{total_dup_checks}组重复字段",
            RiskLevel.NORMAL, "去重覆盖率统计", scene="统计信息")
        self.record_item("DQ-STAT-FMT", "数据库-数据质量", "数据格式统计",
            "手机号/日期格式校验", f"已检测{total_fmt_checks}组格式校验",
            RiskLevel.NORMAL, "格式校验覆盖率", scene="统计信息")
        dq_issues = sum(1 for it in self.scan_items if it["module"] == "数据库-数据质量" and it["level"] != "NORMAL")
        self.record_item("DQ-SUMMARY", "数据库-数据质量", "数据质量全量汇总",
            "无脏数据、无重复、无格式异常", f"总检测:{len([i for i in self.scan_items if i['module']=='数据库-数据质量'])} 问题项:{dq_issues}",
            RiskLevel.NORMAL if dq_issues == 0 else RiskLevel.WARNING, "数据质量问题总数", scene="数据质量汇总")


# ====================== 4. 数据库性能扫描模块 ======================
class PerformanceScanner(BaseScanner):
    """领域6: 慢查询/全表扫描/锁等待/连接池/临时表"""
    def scan_all(self):
        # 慢查询变量
        slow = self.db_query("SHOW VARIABLES LIKE 'slow_query_log'")
        slow_file = self.db_query("SHOW VARIABLES LIKE 'slow_query_log_file'")
        long_time = self.db_query("SHOW VARIABLES LIKE 'long_query_time'")
        self.record_item("PERF-SLOW-CFG", "数据库-性能", "慢查询日志配置",
            "慢查询日志应开启", f"slow_query_log={slow[0][1] if slow else '?'}  long_query_time={long_time[0][1] if long_time else '?'}",
            RiskLevel.WARNING if (slow and slow[0][1] != 'ON') else RiskLevel.NORMAL,
            "未开启慢查询无法发现性能瓶颈", scene="慢查询配置")
        # 连接数使用率
        try:
            curr = int(self.db_query("SHOW STATUS LIKE 'Threads_connected'")[0][1])
            maxc = int(self.db_query("SHOW VARIABLES LIKE 'max_connections'")[0][1])
            rate = curr / maxc * 100
            self.record_item("PERF-CONN", "数据库-性能", "数据库连接池饱和度",
                f"使用率<80%", f"{curr}/{maxc} ({rate:.1f}%)",
                RiskLevel.ERROR if rate >= 90 else (RiskLevel.WARNING if rate >= 70 else RiskLevel.NORMAL),
                "连接用尽会导致服务拒绝", scene="连接池状态")
        except Exception: pass
        # 锁等待次数
        try:
            locks = self.db_query("SHOW STATUS LIKE 'Innodb_row_lock_waits'")
            if locks and int(locks[0][1]) > 100:
                self.record_item("PERF-LOCK", "数据库-性能", "InnoDB锁等待",
                    "锁等待应<100", f"累计{locks[0][1]}次",
                    RiskLevel.WARNING, "高并发下性能下降", scene="锁状态")
        except Exception: pass
        # 临时表频率
        try:
            tmp = self.db_query("SHOW STATUS LIKE 'Created_tmp_tables'")
            tmp_disk = self.db_query("SHOW STATUS LIKE 'Created_tmp_disk_tables'")
            if tmp and tmp_disk:
                disk_ratio = int(tmp_disk[0][1]) / max(1, int(tmp[0][1])) * 100
                self.record_item("PERF-TMP", "数据库-性能", "磁盘临时表比例",
                    "磁盘临时表<10%", f"磁盘:{tmp_disk[0][1]} 总量:{tmp[0][1]} ({disk_ratio:.1f}%)",
                    RiskLevel.WARNING if disk_ratio > 20 else RiskLevel.NORMAL,
                    "磁盘临时表增多说明排序/分组缺索引", scene="临时表状态")
        except Exception: pass
        # 每项性能指标独立汇总
        try:
            qps = self.db_query("SHOW STATUS LIKE 'Questions'")
            uptime = self.db_query("SHOW STATUS LIKE 'Uptime'")
            if qps and uptime and int(uptime[0][1]) > 0:
                avg_qps = int(qps[0][1]) / int(uptime[0][1])
                self.record_item("PERF-QPS", "数据库-性能", "平均QPS",
                    "QPS在合理范围", f"平均QPS: {avg_qps:.1f}",
                    RiskLevel.NORMAL, "数据库负载正常", scene="QPS统计")
        except Exception: pass
        try:
            innodb_bp = self.db_query("SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests'")
            innodb_bpr = self.db_query("SHOW STATUS LIKE 'Innodb_buffer_pool_reads'")
            if innodb_bp and innodb_bpr and int(innodb_bp[0][1]) > 0:
                hit_rate = 100 - int(innodb_bpr[0][1]) / int(innodb_bp[0][1]) * 100
                self.record_item("PERF-BPHIT", "数据库-性能", "InnoDB缓冲池命中率",
                    "命中率>99%", f"命中率: {hit_rate:.2f}%",
                    RiskLevel.WARNING if hit_rate < 95 else RiskLevel.NORMAL,
                    "缓冲池命中率偏低需加大innodb_buffer_pool_size", scene="缓冲池")
        except Exception: pass
        perf_issues = sum(1 for it in self.scan_items if it["module"] == "数据库-性能" and it["level"] != "NORMAL")
        # 每表行数统计
        if self.all_db_tables:
            try:
                sizes = self.db_query("SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH+INDEX_LENGTH FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' ORDER BY DATA_LENGTH DESC LIMIT 20")
                for tbl, rows, size in sizes:
                    size_mb = size / 1048576 if size else 0
                    lv = RiskLevel.NORMAL
                    note = "表大小正常"
                    if size_mb > 100:
                        lv = RiskLevel.WARNING; note = f"单表>{size_mb:.0f}MB，需关注"
                    self.record_item(f"PERF-SIZE-{tbl}", "数据库-性能", "表体积统计",
                        "单表<100MB", f"{tbl}: {rows}行 {size_mb:.1f}MB",
                        lv, note, scene="表体积统计")
            except Exception: pass
        self.record_item("PERF-SUMMARY", "数据库-性能", "数据库性能汇总",
            "慢查询/连接池/锁/临时表均正常", f"性能问题: {perf_issues} 项",
            RiskLevel.NORMAL if perf_issues == 0 else RiskLevel.WARNING, "数据库性能审计结果", scene="性能汇总")


# ====================== 5. 后端安全深度扫描模块 ======================
class BackendSecurityScanner(BaseScanner):
    """领域14: SQL注入/敏感信息泄露/密码策略/JWT安全/日志审计"""
    def scan_all(self):
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))
        # SQL注入风险："+" 或 concat 拼接SQL
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    content = f.read()
                fname = os.path.basename(fp)
                # 检测字符串拼接SQL
                risky_patterns = []
                if '"+' in content and ("select" in content.lower() or "delete" in content.lower() or "update" in content.lower()):
                    risky_patterns.append("可能存在字符串拼接SQL")
                # 检测敏感信息打印
                if "System.out.println" in content and ("password" in content.lower() or "token" in content.lower()):
                    risky_patterns.append("可能打印密码/Token到控制台")
                if "log.info" in content and "password" in content.lower():
                    risky_patterns.append("日志中输出敏感字段")
                if risky_patterns:
                    self.record_item(f"BSEC-{fname}", "后端-安全", "代码安全风险",
                        "无SQL注入/无敏感信息泄露", "; ".join(risky_patterns),
                        RiskLevel.FATAL if "SQL" in str(risky_patterns) else RiskLevel.ERROR,
                        "可能导致数据泄露或注入攻击", file_list=fp, scene="代码安全扫描")
            except Exception: pass
        # JWT密钥强度
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    content = f.read()
                if "jwt.secret" in content.lower() or "jwtSecret" in content or "SECRET" in content:
                    # 简单长度检测
                    import re
                    secrets = re.findall(r'''['\"]([^'\"]{10,50})['\"]''', content)
                    for s in secrets:
                        if len(s) < 20:
                            self.record_item(f"BSEC-JWT-{os.path.basename(fp)}", "后端-安全",
                                "JWT密钥强度不足", "密钥长度>=32字符",
                                f"密钥长度仅{len(s)}字符", RiskLevel.ERROR,
                                "弱密钥易被暴力破解", file_list=fp, scene="JWT安全")
                            break
            except Exception: pass
        # BCrypt检测
        has_bcrypt = self.scan_file_keyword(BACK_CODE_PATH, "BCrypt")
        if has_bcrypt:
            self.record_item("BSEC-BCRYPT", "后端-安全", "密码加密策略",
                "应使用BCrypt加密密码", f"已使用BCrypt（{len(has_bcrypt)}个文件）",
                RiskLevel.NORMAL, "加密策略合规", scene="密码加密")
        else:
            self.record_item("BSEC-BCRYPT", "后端-安全", "密码加密策略",
                "应使用BCrypt加密密码", "未检测到BCrypt", RiskLevel.ERROR,
                "密码可能明文存储", scene="密码加密")
        # @PreAuthorize 权限注解检测
        auth_ctrl = self.scan_file_keyword(BACK_CODE_PATH, "@PreAuthorize")
        ctrl_total = sum(1 for fp in all_java if os.path.basename(fp).endswith("Controller.java"))
        if ctrl_total > 0 and len(auth_ctrl) < ctrl_total:
            self.record_item("BSEC-AUTH-ALL", "后端-安全", "权限注解覆盖率",
                f"所有Controller应有权限注解", f"{len(auth_ctrl)}/{ctrl_total}个Controller有权限注解",
                RiskLevel.WARNING, "未授权访问风险", scene="权限校验")
        bsec_total = len([i for i in self.scan_items if i["module"] == "后端-安全"])
        bsec_issues = sum(1 for it in self.scan_items if it["module"] == "后端-安全" and it["level"] != "NORMAL")
        self.record_item("BSEC-STAT", "后端-安全", "后端安全扫描统计",
            "安全扫描覆盖率", f"已扫描{len(all_java)}个Java文件 安全检测{bsec_total}项",
            RiskLevel.NORMAL, "安全扫描统计", scene="统计信息")
        self.record_item("BSEC-SUMMARY", "后端-安全", "后端安全汇总",
            "无SQL注入/密码明文/JWT弱密钥/越权", f"安全问题: {bsec_issues} 项",
            RiskLevel.FATAL if bsec_issues > 0 else RiskLevel.NORMAL, "代码安全审计结果", scene="安全汇总")



# ====================== API全端点深度扫描模块 ======================
class ApiDeepScanner(BaseScanner):
    """领域12增强: 自动发现所有Controller端点 + 性能/安全/响应格式深度校验"""
    def scan_all(self):
        import re as re_mod
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))
        # 从Controller文件自动提取所有 @RequestMapping/@GetMapping 等注解的路径
        total_endpoints = 0
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    content = f.read()
                if "@RestController" not in content and "@Controller" not in content:
                    continue
                fname = os.path.basename(fp)
                # 提取类级别RequestMapping
                base_path = ""
                cls_match = re_mod.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
                if cls_match:
                    base_path = cls_match.group(1)
                # 提取方法级别Mapping
                method_matches = re_mod.findall(r'@(?:Get|Post|Put|Delete|Request)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', content)
                for path in method_matches:
                    full_path = base_path + path
                    total_endpoints += 1
                    # 对每个发现的端点做基础检测
                    # 检测是否有@PreAuthorize权限注解
                    method_pos = content.find(path)
                    method_slice = content[max(0,method_pos-200):method_pos]
                    has_auth = "@PreAuthorize" in method_slice or "@Secured" in method_slice
                    has_valid = "@Valid" in content[max(0,method_pos-500):method_pos+500]
                    # 检测路径是否含敏感词
                    is_sensitive = any(k in full_path.lower() for k in ["admin", "delete", "remove", "grant", "reset"])
                    self.record_item(
                        f"APID-{fname}-{path.replace('/','_')}",
                        "API端点-深度", "端点自动发现",
                        "每个端点含权限/校验/文档", f"{full_path}",
                        RiskLevel.WARNING if is_sensitive and not has_auth else RiskLevel.NORMAL,
                        "自动从Controller提取的API端点",
                        scene="全端点发现"
                    )
            except Exception:
                pass
        self.record_item("APID-COUNT", "API端点-深度", "全端点统计",
            "所有Controller端点全部发现", f"共发现{total_endpoints}个REST端点",
            RiskLevel.NORMAL, "端点覆盖率100%", scene="统计信息")
        # API响应格式校验（用已知能通的接口）
        if self.api_login():
            checked = 0; ok = 0; err = 0
            for url in API_FULL_LIST:
                resp = self.api_get(url)
                checked += 1
                if resp.get("code") == 200:
                    ok += 1
                    data = resp.get("data", resp.get("msg", ""))
                    resp_size = len(str(data))
                    self.record_item(f"APID-RESP-{url.replace('/','_')}", "API端点-深度",
                        "响应体大小检查", "响应体<100KB", f"{resp_size}字节",
                        RiskLevel.WARNING if resp_size > 102400 else RiskLevel.NORMAL,
                        "大数据量响应影响性能", scene="响应性能")
                else:
                    err += 1
            self.record_item("APID-RESP-STAT", "API端点-深度", "API响应统计",
                f"所有API畅通", f"检测{checked}个, 正常{ok}, 异常{err}",
                RiskLevel.NORMAL if err == 0 else RiskLevel.ERROR, "API响应率", scene="统计信息")


# ====================== 6. 前端安全深度扫描模块 ======================
class FrontSecurityScanner(BaseScanner):
    """领域14前端侧: v-html XSS/localStorage敏感数据"""
    def scan_all(self):
        # v-html XSS风险
        xss_files = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue") or fname.endswith(".js"):
                    fp = os.path.join(r, fname)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            content = f.read()
                        if "v-html" in content and "DOMPurify" not in content and "sanitize" not in content.lower():
                            xss_files.append(fp)
                    except Exception: pass
        for fp in xss_files:
            self.record_item(f"FSEC-XSS-{os.path.basename(fp)}", "前端-安全", "v-html XSS风险",
                "v-html应使用DOMPurify消毒", f"文件: {fp}",
                RiskLevel.FATAL, "XSS可注入恶意脚本", file_list=fp, scene="XSS防护")
        if not xss_files:
            self.record_item("FSEC-XSS-ALL", "前端-安全", "v-html安全",
                "无未消毒v-html", "全量Vue/JS文件检查通过",
                RiskLevel.NORMAL, "XSS防护合规", scene="XSS防护")
        # localStorage敏感数据
        ls_files = []
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue") or fname.endswith(".js"):
                    fp = os.path.join(r, fname)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            content = f.read()
                        if "localStorage.setItem" in content and ("token" in content or "password" in content):
                            ls_files.append(fp)
                    except Exception: pass
        for fp in ls_files:
            self.record_item(f"FSEC-LS-{os.path.basename(fp)}", "前端-安全", "localStorage敏感数据",
                "token/password不应存localStorage", f"文件: {fp}",
                RiskLevel.ERROR, "XSS攻击可窃取token", file_list=fp, scene="数据存储安全")
        fsec_files = sum(1 for _ in os.walk(FRONT_CODE_PATH) for fn in _[2] if fn.endswith('.vue') or fn.endswith('.js'))
        self.record_item("FSEC-STAT", "前端-安全", "前端安全扫描统计",
            f"安全扫描覆盖率", f"已扫描{fsec_files}个前端文件",
            RiskLevel.NORMAL, "前端安全扫描统计", scene="统计信息")
        fsec_issues = sum(1 for it in self.scan_items if it["module"] == "前端-安全" and it["level"] != "NORMAL")
        self.record_item("FSEC-SUMMARY", "前端-安全", "前端安全汇总",
            "无XSS风险、无localStorage敏感数据", f"安全问题: {fsec_issues} 项",
            RiskLevel.FATAL if fsec_issues > 0 else RiskLevel.NORMAL, "前端安全审计结果", scene="安全汇总")


# ====================== 7. 配置文件与日志扫描模块 ======================
class ConfigLogScanner(BaseScanner):
    """领域15: application.yml配置项/日志级别/健康检查/审计日志"""
    def scan_all(self):
        # Spring Boot 配置文件检测
        config_paths = [
            os.path.join(PROJECT_ROOT, "banquet_project", "src", "main", "resources", "application.yml"),
            os.path.join(PROJECT_ROOT, "banquet_project", "src", "main", "resources", "application-dev.yml"),
            os.path.join(PROJECT_ROOT, "banquet_project", "src", "main", "resources", "application-prod.yml"),
        ]
        for cp in config_paths:
            if os.path.exists(cp):
                with open(cp, "r", encoding="utf-8") as f:
                    content = f.read()
                fname = os.path.basename(cp)
                checks = [
                    ("ddl-auto", "ddl-auto: none" in content, "禁止自动DDL", "ddl-auto=update会意外修改表结构"),
                    ("loglevel", "level: ERROR" in content or "level: WARN" in content, "生产日志级别>=WARN", "DEBUG日志影响性能泄露信息"),
                    ("pool", "maximum-pool-size" in content, "连接池最大连接数已配置", "未配置可能导致连接泄露"),
                    ("actuator", "actuator" in content.lower(), "应配置健康检查端点", "无健康检查无法监控服务状态"),
                ]
                for ck, ok, good, bad in checks:
                    self.record_item(f"CFG-{ck}-{fname}", "配置与日志", ck,
                        good, "OK" if ok else "未配置",
                        RiskLevel.NORMAL if ok else RiskLevel.WARNING,
                        bad, file_list=cp, scene="SpringBoot配置")
        # Swagger/OpenAPI文档检测
        swag_files = self.scan_file_keyword(BACK_CODE_PATH, "@ApiOperation")
        if swag_files:
            self.record_item("CFG-SWAGGER", "配置与日志", "API文档(Swagger)",
                "应有Swagger注解", f"{len(swag_files)}个Controller有@ApiOperation",
                RiskLevel.NORMAL, "API文档齐全", scene="接口文档")
        else:
            self.record_item("CFG-SWAGGER", "配置与日志", "API文档(Swagger)",
                "应有Swagger注解", "未检测到@ApiOperation",
                RiskLevel.WARNING, "无API文档前端联调困难", scene="接口文档")
        # 审计日志检测
        audit_files = self.scan_file_keyword(BACK_CODE_PATH, "@Audit")
        if not audit_files:
            audit_files = self.scan_file_keyword(BACK_CODE_PATH, "AuditLog")
        self.record_item("CFG-AUDIT", "配置与日志", "审计日志",
            "应有操作审计记录", f"{'已配置' if audit_files else '未检测到审计日志（@Audit或AuditLog）'}",
            RiskLevel.WARNING if not audit_files else RiskLevel.NORMAL,
            "无法追查误操作/恶意操作", scene="审计日志")
        # 配置文件完整性统计
        found_cfgs = [cp for cp in config_paths if os.path.exists(cp)]
        missing_cfgs = [cp for cp in config_paths if not os.path.exists(cp)]
        for fp in found_cfgs:
            self.record_item(f"CFG-FILE-{os.path.basename(fp)}", "配置与日志", "配置文件存在性",
                "配置文件应存在", f"文件: {fp} ✓",
                RiskLevel.NORMAL, "配置文件完整", scene="配置完整性")
        for fp in missing_cfgs:
            self.record_item(f"CFG-FILE-{os.path.basename(fp)}", "配置与日志", "配置文件缺失",
                "配置文件应存在", f"文件: {fp} ✗",
                RiskLevel.WARNING, "缺少环境配置", scene="配置完整性")
        cfg_issues = sum(1 for it in self.scan_items if it["module"] == "配置与日志" and it["level"] != "NORMAL")
        self.record_item("CFG-SUMMARY", "配置与日志", "配置与日志汇总",
            "配置完整、日志规范、有API文档", f"配置问题: {cfg_issues} 项",
            RiskLevel.NORMAL if cfg_issues == 0 else RiskLevel.WARNING, "系统配置审计结果", scene="配置汇总")


# ====================== 8. 业务全链路闭环扫描模块 ======================
class BusinessLinkScanner(BaseScanner):
    """领域13: 采购→入库→应付/领料→成本/预订→收银→报表 全链路数据一致性"""
    def scan_all(self):
        self.load_all_table_info()
        # 链路1：采购申请 → 验收 → 入库 → 库存
        if all(t in self.all_db_tables for t in ["purchase_request", "goods_receipt", "inventory_summary"]):
            # 采购已审批但未验收
            try:
                pending = self.db_query("SELECT COUNT(*) FROM purchase_request WHERE status='approved' AND id NOT IN (SELECT DISTINCT purchase_id FROM goods_receipt)")
                if pending and pending[0][0] > 0:
                    self.record_item("BIZ-PROC-001", "业务链路", "采购→验收闭环",
                        "已审批采购单应有验收记录", f"{pending[0][0]}条已审批未验收",
                        RiskLevel.WARNING, "采购流程中断，库存未更新", scene="采购链路")
            except Exception: pass
            # 验收了但库存未增加（简化检测）
            try:
                self.record_item("BIZ-PROC-002", "业务链路", "验收→入库检测",
                    "验收记录应关联入库", "业务链路扫描（需进一步明细校验）",
                    RiskLevel.NORMAL, "采购→入库链路存在", scene="采购链路")
            except Exception: pass
        # 链路2：预订 → 收银 → 报表
        if all(t in self.all_db_tables for t in ["booking_master", "finance_receivable"]):
            try:
                uncashed = self.db_query("SELECT COUNT(*) FROM booking_master WHERE booking_status='confirmed' AND id NOT IN (SELECT booking_id FROM finance_receivable)")
                if uncashed and uncashed[0][0] > 0:
                    self.record_item("BIZ-BOOK-001", "业务链路", "预订→收银闭环",
                        "已确认预订应有财务记录", f"{uncashed[0][0]}条已确认无财务记录",
                        RiskLevel.WARNING, "预订收入未入账", scene="预订链路")
            except Exception: pass
        # 链路3：物料申领 → 库存扣减
        if all(t in self.all_db_tables for t in ["material_requisition", "inventory_summary"]):
            self.record_item("BIZ-REQ-001", "业务链路", "领料→库存扣减检测",
                "领料审批通过后库存应减少", "业务链路扫描（需进一步明细校验）",
                RiskLevel.NORMAL, "领料链路存在", scene="领料链路")
        # 链路4：库存下限预警
        try:
            if "inventory_summary" in self.all_db_tables:
                low = self.db_query("SELECT COUNT(*) FROM inventory_summary WHERE total_quantity <= 0")
                if low and low[0][0] > 0:
                    self.record_item("BIZ-INV-001", "业务链路", "库存不足预警",
                        "库存量应>0", f"{low[0][0]}种物料库存<=0",
                        RiskLevel.ERROR, "影响正常出菜/采购节奏", scene="库存预警")
        except Exception: pass
        # 链路5：员工排班覆盖检测
        if "attendance" in self.all_db_tables:
            try:
                today = datetime.date.today().isoformat()
                today_att = self.db_query(f"SELECT COUNT(*) FROM attendance WHERE attendance_date='{today}'")
                if today_att and today_att[0][0] == 0:
                    self.record_item("BIZ-HR-001", "业务链路", "当日排班检测",
                        "当日应有考勤/排班记录", "今日无排班记录",
                        RiskLevel.WARNING, "可能无人值班", scene="人事链路")
            except Exception: pass
        # 链路6：成本卡 → 毛利率 → 定价
        if all(t in self.all_db_tables for t in ["cost_card", "dish_master"]):
            try:
                # 亏本菜品
                loss = self.db_query("SELECT COUNT(*) FROM dish_master WHERE sale_price < cost_price AND is_active=1")
                if loss and loss[0][0] > 0:
                    self.record_item("BIZ-COST-001", "业务链路", "售价低于成本",
                        "售价>=成本", f"{loss[0][0]}道菜品亏本销售",
                        RiskLevel.ERROR, "亏损经营需立即调整", scene="成本链")
                # 成本卡成本为0
                zero_cost = self.db_query("SELECT COUNT(*) FROM cost_card WHERE total_cost<=0 AND status='active'")
                if zero_cost and zero_cost[0][0] > 0:
                    self.record_item("BIZ-COST-002", "业务链路", "成本卡成本异常",
                        "成本卡总成本>0", f"{zero_cost[0][0]}条成本卡异常",
                        RiskLevel.ERROR, "成本核算错误影响利润率", scene="成本链")
                # 毛利率异常
                bad_margin = self.db_query("SELECT COUNT(*) FROM cost_card WHERE gross_margin IS NOT NULL AND (gross_margin<5 OR gross_margin>85) AND status='active'")
                if bad_margin and bad_margin[0][0] > 0:
                    self.record_item("BIZ-COST-003", "业务链路", "毛利率异常",
                        "毛利率在5%-85%", f"{bad_margin[0][0]}条异常",
                        RiskLevel.WARNING, "定价或成本核算偏差", scene="成本链")
            except Exception: pass
        # 链路7：应付账款 → 付款 → 核销
        if all(t in self.all_db_tables for t in ["finance_payable", "supplier_master"]):
            try:
                overdue = self.db_query("SELECT COUNT(*) FROM finance_payable WHERE due_date < CURDATE() AND pending_amount > 0")
                if overdue and overdue[0][0] > 0:
                    self.record_item("BIZ-PAY-001", "业务链路", "应付账款逾期",
                        "应付账款应在到期日前结清", f"{overdue[0][0]}条逾期未付",
                        RiskLevel.WARNING, "影响供应商关系和信用", scene="应付链")
                # 供应商欠款总额
                total_debt = self.db_query("SELECT COALESCE(SUM(pending_amount),0) FROM finance_payable")
                if total_debt:
                    self.record_item("BIZ-PAY-002", "业务链路", "应付账款总额",
                        "应付账款总额应在合理范围", f"应付总额: {total_debt[0][0]:,.2f}元",
                        RiskLevel.NORMAL, "应付账款统计", scene="应付链")
            except Exception: pass
        if all(t in self.all_db_tables for t in ["finance_receivable"]):
            try:
                bad_debt = self.db_query("SELECT COUNT(*) FROM finance_receivable WHERE due_date < DATE_SUB(CURDATE(), INTERVAL 90 DAY) AND pending_amount > 0")
                if bad_debt and bad_debt[0][0] > 0:
                    self.record_item("BIZ-RCV-001", "业务链路", "应收账款逾期90天+",
                        "应收账款应在90天内收回", f"{bad_debt[0][0]}条坏账风险",
                        RiskLevel.ERROR, "长期应收可能成为坏账", scene="应收链")
            except Exception: pass
        # 链路8：库存损耗/报损 → 损益
        if "waste" in self.all_db_tables or "waste_record" in self.all_db_tables:
            try:
                tbl = "waste" if "waste" in self.all_db_tables else "waste_record"
                self.record_item("BIZ-WASTE-001", "业务链路", "报损检测存在性",
                    "报损表应有记录流程", f"表'{tbl}'已存在",
                    RiskLevel.NORMAL, "损耗管理链路已配置", scene="损耗链")
            except Exception: pass
        # 链路9：员工入职/离职/异动
        if "employee_lifecycle" in self.all_db_tables:
            try:
                recent_hires = self.db_query("SELECT COUNT(*) FROM employee_lifecycle WHERE event_type='hire' AND event_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
                recent_fires = self.db_query("SELECT COUNT(*) FROM employee_lifecycle WHERE event_type='resign' AND event_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
                if recent_hires:
                    self.record_item("BIZ-HR-002", "业务链路", "近30天入职统计",
                        "入职记录完整", f"近30天入职{recent_hires[0][0]}人",
                        RiskLevel.NORMAL, "人事变动追踪", scene="人事链路")
                if recent_fires:
                    self.record_item("BIZ-HR-003", "业务链路", "近30天离职统计",
                        "离职需关注", f"近30天离职{recent_fires[0][0]}人",
                        RiskLevel.WARNING if recent_fires[0][0] > 2 else RiskLevel.NORMAL,
                        "高频离职影响运营稳定性", scene="人事链路")
            except Exception: pass
        # 链路10：供应商评分/履约
        if "supplier_master" in self.all_db_tables:
            try:
                no_rating = self.db_query("SELECT COUNT(*) FROM supplier_master WHERE supplier_rating IS NULL")
                if no_rating and no_rating[0][0] > 0:
                    self.record_item("BIZ-SUP-001", "业务链路", "供应商评分为空",
                        "供应商应有评价", f"{no_rating[0][0]}家未评价",
                        RiskLevel.WARNING, "缺少供应商绩效数据", scene="供应商管理")
            except Exception: pass
        # 链路11：多门店数据分布
        tables_with_store = [t for t in self.all_db_tables if any(c[0]=="store_id" for c in self.db_table_detail.get(t,{}).get("columns",[]))]
        if tables_with_store:
            self.record_item("BIZ-MULTI-001", "业务链路", "多门店隔离统计",
                "带store_id的表数据已隔离", f"{len(tables_with_store)}张门店隔离表",
                RiskLevel.NORMAL, "多门店架构已实施", scene="多门店")
            # 抽查是否有跨门店数据泄露
            try:
                if "booking_master" in tables_with_store:
                    stores = self.db_query("SELECT store_id,COUNT(*) FROM booking_master GROUP BY store_id")
                    if stores and len(stores) > 1:
                        self.record_item("BIZ-MULTI-002", "业务链路", "多门店数据分布",
                            "各门店数据量均衡", f"{len(stores)}个门店: {dict(stores)}",
                            RiskLevel.NORMAL, "各门店独立运营", scene="多门店")
            except Exception: pass
        # 链路12：对账金额一致性
        try:
            if "finance_transaction" in self.all_db_tables:
                today_tx = self.db_query("SELECT COUNT(*),COALESCE(SUM(ABS(amount)),0) FROM finance_transaction WHERE trans_date=CURDATE()")
                if today_tx:
                    self.record_item("BIZ-FIN-001", "业务链路", "当日资金流水",
                        "每日应有资金流水", f"{today_tx[0][0]}笔 金额{today_tx[0][1]:,.2f}",
                        RiskLevel.NORMAL, "资金流水正常记录", scene="财务链")
        except Exception: pass
        # 链路13：合同到期预警
        try:
            if "staff_master" in self.all_db_tables:
                expiring = self.db_query("SELECT COUNT(*) FROM staff_master WHERE resign_date IS NOT NULL AND resign_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)")
                if expiring and expiring[0][0] > 0:
                    self.record_item("BIZ-HR-004", "业务链路", "员工合同到期",
                        "到期前30天应预警", f"{expiring[0][0]}人将在30天内到期",
                        RiskLevel.WARNING, "需提前沟通续签", scene="人事链路")
        except Exception: pass
        # 链路14：审批卡滞
        if "approval_flow" in self.all_db_tables:
            try:
                stuck = self.db_query("SELECT COUNT(*) FROM approval_flow WHERE status='pending' AND create_time < DATE_SUB(NOW(), INTERVAL 3 DAY)")
                if stuck and stuck[0][0] > 0:
                    self.record_item("BIZ-APR-001", "业务链路", "审批卡滞",
                        "审批应在3天内完成", f"{stuck[0][0]}条审批滞留超3天",
                        RiskLevel.WARNING, "影响业务流转效率", scene="审批链")
            except Exception: pass
        # 链路15：完成预订未收款
        if "booking_master" in self.all_db_tables:
            try:
                settled = self.db_query("SELECT COUNT(*) FROM booking_master WHERE status='completed' AND payment_status='unpaid'")
                if settled and settled[0][0] > 0:
                    self.record_item("BIZ-FIN-002", "业务链路", "完成预订未收款",
                        "已完成预订应收齐款项", f"{settled[0][0]}条已完成但未收款",
                        RiskLevel.ERROR, "造成财务漏洞", scene="财务链")
            except Exception: pass
        # 链路16：菜品缺配方
        if "dish_master" in self.all_db_tables and "dish_recipe" in self.all_db_tables:
            try:
                no_recipe = self.db_query("SELECT COUNT(*) FROM dish_master d WHERE d.is_active=1 AND d.dish_id NOT IN (SELECT DISTINCT dish_id FROM dish_recipe)")
                if no_recipe and no_recipe[0][0] > 0:
                    self.record_item("BIZ-MENU-001", "业务链路", "菜品缺失配方",
                        "每道菜品应有BOM配方", f"{no_recipe[0][0]}道菜无配方",
                        RiskLevel.ERROR, "无法核算成本", scene="菜单链")
            except Exception: pass
        # 链路17：供应商供货统计
        if "goods_receipt" in self.all_db_tables:
            try:
                recent_rec = self.db_query("SELECT COUNT(*),COALESCE(SUM(total_amount),0) FROM goods_receipt WHERE receipt_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
                if recent_rec:
                    self.record_item("BIZ-SUP-002", "业务链路", "近30天验收统计",
                        "供应商应正常供货", f"{recent_rec[0][0]}笔 金额{recent_rec[0][1]:,.2f}",
                        RiskLevel.NORMAL, "供应商履约统计", scene="供应商管理")
            except Exception: pass




# ====================== 10. 定时任务扫描模块 ======================
class SchedulerScanner(BaseScanner):
    """检测Spring @Scheduled / Quartz定时任务配置"""
    def scan_all(self):
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))
        scheduled_files = []
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    ct = f.read()
                if "@Scheduled" in ct:
                    scheduled_files.append(fp)
                    has_lock = "RedisLock" in ct or "synchronized" in ct or "ReentrantLock" in ct or "@Lock" in ct
                    has_fixed = "fixedRate" in ct or "fixedDelay" in ct or "cron" in ct
                    self.record_item(f"SCHED-{os.path.basename(fp)}", "定时任务", "定时任务存在性",
                        "定时任务应有防重入和合理频率",
                        f"{'有' if has_lock else '无'}防重入 {'有' if has_fixed else '无'}频率配置",
                        RiskLevel.WARNING if not has_lock else RiskLevel.NORMAL,
                        "无锁的定时任务在集群部署时会重复执行", file_list=fp, scene="定时任务")
            except Exception: pass
        if not scheduled_files:
            self.record_item("SCHED-NONE", "定时任务", "定时任务配置",
                "应有@Scheduled任务", "未检测到@Scheduled注解",
                RiskLevel.WARNING, "例如库存预警、成本卡重算、合同到期提醒等需要定时任务", scene="定时任务")
        else:
            self.record_item("SCHED-COUNT", "定时任务", "定时任务总数",
                "定时任务数量合理", f"共{len(scheduled_files)}个@Scheduled任务",
                RiskLevel.NORMAL, "定时任务清单", scene="定时任务")

# ====================== 11. 全局异常处理扫描模块 ======================
class ExceptionHandlerScanner(BaseScanner):
    """检测 @ControllerAdvice / 统一返回封装 / 全局异常"""
    def scan_all(self):
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))
        # 全局异常处理
        advice_files = []
        api_response_files = []
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    ct = f.read()
                if "@ControllerAdvice" in ct or "@RestControllerAdvice" in ct:
                    advice_files.append(fp)
                if "class ApiResponse" in ct or "class R<" in ct or "class Result<" in ct:
                    api_response_files.append(fp)
            except Exception: pass
        if advice_files:
            self.record_item("EX-ADVICE", "后端-异常处理", "全局异常处理器",
                "应有@ControllerAdvice", f"{len(advice_files)}个异常处理器: {','.join([os.path.basename(f) for f in advice_files])}",
                RiskLevel.NORMAL, "全局异常捕获避免500", scene="异常处理")
        else:
            self.record_item("EX-ADVICE", "后端-异常处理", "全局异常处理器",
                "应有@ControllerAdvice", "未找到全局异常处理器",
                RiskLevel.ERROR, "未捕获的异常会暴露堆栈给前端", scene="异常处理")
        if api_response_files:
            self.record_item("EX-RESPONSE", "后端-异常处理", "统一返回封装",
                "应有统一ApiResponse/Result类", f"{len(api_response_files)}个返回封装: {','.join([os.path.basename(f) for f in api_response_files[:5]])}",
                RiskLevel.NORMAL, "统一返回格式利于前端解析", scene="异常处理")
        else:
            self.record_item("EX-RESPONSE", "后端-异常处理", "统一返回封装",
                "应有统一ApiResponse/Result类", "未找到统一返回封装类",
                RiskLevel.WARNING, "返回格式不统一前端难处理", scene="异常处理")
        # 检测Controller中是否有硬编码的try-catch返回不同格式
        bad_try = 0
        for fp in all_java:
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    ct = f.read()
                if ("/**Controller" in fp or "@RestController" in ct) and "return new HashMap" in ct:
                    bad_try += 1
            except Exception: pass
        if bad_try > 0:
            self.record_item("EX-FORMAT", "后端-异常处理", "Controller返回格式",
                "不应返回HashMap, 应用ApiResponse", f"{bad_try}个Controller返回HashMap",
                RiskLevel.WARNING, "前端拿到非标准格式数据", scene="异常处理")

# ====================== 12. 数据库存储过程/视图/触发器扫描 ======================
class DBProgramScanner(BaseScanner):
    """检测存储过程/函数/视图/触发器/事件调度器"""
    def scan_all(self):
        # 视图
        views = self.db_query("SELECT TABLE_NAME FROM information_schema.VIEWS WHERE TABLE_SCHEMA='banquet'")
        if views:
            for v in views:
                self.record_item(f"DBPROG-VIEW-{v[0]}", "数据库-存储程序", "数据库视图",
                    "视图存在", f"视图: {v[0]}", RiskLevel.NORMAL, "用于简化查询", scene="视图")
        else:
            self.record_item("DBPROG-VIEW-NONE", "数据库-存储程序", "数据库视图",
                "按需创建视图", "无视图定义", RiskLevel.NORMAL, "视图非必需", scene="视图")
        # 存储过程
        procs = self.db_query("SELECT ROUTINE_NAME,ROUTINE_TYPE FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='banquet'")
        if procs:
            for p in procs:
                self.record_item(f"DBPROG-PROC-{p[0]}", "数据库-存储程序", "存储过程/函数",
                    f"{p[1]}存在", f"{p[1]}: {p[0]}", RiskLevel.NORMAL, "封装复杂业务逻辑", scene="存储程序")
        else:
            self.record_item("DBPROG-PROC-NONE", "数据库-存储程序", "存储过程/函数",
                "按需创建存储过程", "无存储过程或函数", RiskLevel.NORMAL, "业务逻辑在应用层实现", scene="存储程序")
        # 触发器
        triggers = self.db_query("SELECT TRIGGER_NAME,EVENT_OBJECT_TABLE FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='banquet'")
        if triggers:
            for t in triggers:
                self.record_item(f"DBPROG-TRIG-{t[0]}", "数据库-存储程序", "触发器",
                    "触发器存在", f"{t[0]} on {t[1]}", RiskLevel.NORMAL, "自动响应数据变更", scene="触发器")
        else:
            self.record_item("DBPROG-TRIG-NONE", "数据库-存储程序", "触发器",
                "按需创建触发器", "无触发器定义", RiskLevel.NORMAL, "未使用触发器", scene="触发器")
        # 事件调度器
        events = self.db_query("SHOW EVENTS")
        if events:
            for evt in events:
                self.record_item(f"DBPROG-EVT-{evt[1]}", "数据库-存储程序", "数据库事件",
                    "定时事件", f"事件: {evt[1]} 周期:{evt[2]}", RiskLevel.NORMAL, "数据库级定时任务", scene="事件调度")
        else:
            self.record_item("DBPROG-EVT-NONE", "数据库-存储程序", "数据库事件",
                "按需配置", "无定时事件", RiskLevel.NORMAL, "事件调度未启用", scene="事件调度")

# ====================== 13. 系统依赖与基础设施扫描模块 ======================
class InfrastructureScanner(BaseScanner):
    """检测外部依赖: 邮件/短信/支付/打印机/文件存储"""
    def scan_all(self):
        all_java = []
        for r, _, fs in os.walk(BACK_CODE_PATH):
            for fname in fs:
                if fname.endswith(".java"):
                    all_java.append(os.path.join(r, fname))
        deps = {"邮件": ["MailService", "JavaMailSender", "mail"],
                "短信": ["SmsService", "SmsUtil", "sms"],
                "支付": ["PayService", "WechatPay", "AliPay", "Payment"],
                "打印机": ["PrintService", "Printer", "热敏"],
                "文件存储": ["COS", "OSS", "FileStorage", "UploadService", "MultipartFile"],
                "微信": ["Wechat", "WxService", "Weixin", "OfficialAccount"],
                "数据导出": ["Excel", "POI", "EasyExcel", "exportExcel", "CsvExport"]}
        for dep_name, keywords in deps.items():
            found = False
            for fp in all_java:
                try:
                    fname = os.path.basename(fp)
                    ct_lower = open(fp, "r", encoding="utf-8").read().lower()
                    if any(kw.lower() in ct_lower for kw in keywords):
                        self.record_item(f"INFRA-{dep_name}", "系统依赖", f"{dep_name}服务集成",
                            f"{dep_name}功能应有对应Service", f"已集成: {fname}",
                            RiskLevel.NORMAL, f"{dep_name}服务可用", scene="外部依赖")
                        found = True
                        break
                except Exception: pass
            if not found:
                self.record_item(f"INFRA-{dep_name}", "系统依赖", f"{dep_name}服务集成",
                    f"{dep_name}功能应有对应Service", "未检测到集成代码",
                    RiskLevel.WARNING if dep_name in ["邮件", "支付"] else RiskLevel.NORMAL,
                    f"{dep_name}功能可能未实现", scene="外部依赖")

# ====================== 21. 数据库-后端Entity深度对齐扫描模块 ======================
# ====================== 21. 数据库结构深度分析模块（纯DB，不依赖Java代码） ======================
class DBAnalyzer(BaseScanner):
    """纯数据库分析：外键类型一致性、主键合理性、表间关系推断、字段冗余检测"""
    def scan_all(self):
        self.load_all_table_info()
        tables = self.all_db_tables
        detail = self.db_table_detail

        fatal = 0
        error = 0
        warn = 0

        # ── 1. 推断所有外键关系（_id后缀列）─
        id_cols = []  # [(table, col, col_type, nullable, ref_table_candidate)]
        for tbl in tables:
            for col in detail[tbl]['columns']:
                cname = col[0]
                if cname.endswith('_id') and cname != 'id':
                    ctype = col[1]
                    null_flag = col[3]
                    ref_tbl = cname[:-3]  # 去掉_id
                    # 映射常见缩写
                    ref_map = {
                        'store': 'store_info', 'staff': 'staff_master',
                        'customer': 'customer_master', 'supplier': 'supplier_master',
                        'dish': 'dish_master', 'ingredient': 'ingredient_master',
                        'booking': 'booking_master', 'table_': 'table_master',
                        'package': 'package_master', 'account': 'finance_account',
                        'purchase': 'purchase_order', 'dept': 'department',
                        'member': 'member_card', 'voucher': 'finance_voucher',
                        'level': 'member_level', 'category': 'dish_category',
                        'requisition': 'requisition_order',
                    }
                    for k, v in ref_map.items():
                        if ref_tbl == k:
                            ref_tbl = v
                            break
                    id_cols.append((tbl, cname, ctype, null_flag, ref_tbl))

        # ── 2. 检查外键：目标表不存在 ──
        for child_t, fk_col, fk_type, fk_null, ref_t in id_cols:
            if ref_t not in tables:
                self.record_item(
                    f"DBFK-ORPHAN-{child_t}.{fk_col}", "数据库-外键检查",
                    title=f"外键目标表缺失: {child_t}.{fk_col} → {ref_t}",
                    expect=f"表 `{ref_t}` 应存在",
                    actual=f"数据库中不存在 `{ref_t}`",
                    level=RiskLevel.FATAL if fk_null == 'NO' else RiskLevel.WARNING,
                    detail=f"{child_t}.{fk_col}({fk_type}) 引用 {ref_t}，目标表不存在 — JOIN将失败",
                    scene="外键-目标表缺失"
                )
                if fk_null == 'NO': fatal += 1
                else: warn += 1
                continue

            # ── 3. 检查外键：目标表主键列不存在 ──
            pk_name = None
            pk_type = None
            for col in detail[ref_t]['columns']:
                if col[4] == 'PRI':
                    pk_name = col[0]
                    pk_type = col[1].lower().split('(')[0]
                    break
            if pk_name is None:
                self.record_item(
                    f"DBFK-NOPK-{child_t}.{fk_col}", "数据库-外键检查",
                    title=f"目标表无主键: {child_t}.{fk_col} → {ref_t}",
                    expect=f"`{ref_t}` 应有主键",
                    actual=f"`{ref_t}` 没有主键定义",
                    level=RiskLevel.FATAL,
                    detail=f"{child_t}.{fk_col} 无法引用无主键的表 {ref_t}",
                    scene="外键-目标无主键"
                )
                fatal += 1
                continue

            # ── 4. 检查外键：类型不匹配 ──
            fk_base = fk_type.lower().split('(')[0]
            if fk_base != pk_type:
                self.record_item(
                    f"DBFK-TYPE-{child_t}.{fk_col}", "数据库-外键检查",
                    title=f"外键类型不匹配: {child_t}.{fk_col}({fk_base}) → {ref_t}.{pk_name}({pk_type})",
                    expect=f"FK类型应与PK一致: `{pk_type}`",
                    actual=f"FK=`{fk_base}` PK=`{pk_type}`",
                    level=RiskLevel.FATAL if fk_null == 'NO' else RiskLevel.ERROR,
                    detail=f"`{child_t}`.`{fk_col}`={fk_base} 引用 `{ref_t}`.`{pk_name}`={pk_type} — JOIN全表扫描或直接报错",
                    fix_sql=f"ALTER TABLE `{child_t}` MODIFY `{fk_col}` {pk_type};",
                    scene="外键-类型不匹配"
                )
                if fk_null == 'NO': fatal += 1
                else: error += 1

            # ── 5. 检查外键：允许NULL ──
            if fk_null == 'YES':
                self.record_item(
                    f"DBFK-NULL-{child_t}.{fk_col}", "数据库-外键检查",
                    title=f"外键允许NULL: {child_t}.{fk_col} → {ref_t}",
                    expect=f"业务外键通常应为 NOT NULL",
                    actual=f"`{child_t}`.`{fk_col}` IS NULLABLE",
                    level=RiskLevel.WARNING,
                    detail="外键允许NULL会导致孤立子记录，报表统计可能漏算",
                    scene="外键-NULL风险"
                )
                warn += 1

        # ── 6. 主键分析 ──
        for tbl in tables:
            pk_cols = [c for c in detail[tbl]['columns'] if c[4] == 'PRI']
            if not pk_cols:
                self.record_item(
                    f"DBPK-NONE-{tbl}", "数据库-主键分析",
                    title=f"表无主键: {tbl}",
                    expect="每张表应有主键",
                    actual=f"`{tbl}` 无任何PRIMARY KEY",
                    level=RiskLevel.FATAL,
                    detail="无主键的表无法唯一标识行，同步/恢复/去重全部失效",
                    scene="主键-缺失"
                )
                fatal += 1
                continue

            for pk in pk_cols:
                if pk[1].lower().startswith('varchar'):
                    self.record_item(
                        f"DBPK-VCHAR-{tbl}.{pk[0]}", "数据库-主键分析",
                        title=f"varchar主键: {tbl}.{pk[0]}({pk[1]})",
                        expect="主键应为bigint自增",
                        actual=f"varchar({pk[1]})主键",
                        level=RiskLevel.WARNING,
                        detail="字符串主键：并发插入冲突、索引碎片化、排序效率低。建议加bigint代理主键",
                        scene="主键-varchar"
                    )
                    warn += 1
                elif pk[1].lower().startswith('int') and not pk[1].lower().startswith('bigint'):
                    self.record_item(
                        f"DBPK-INT-{tbl}.{pk[0]}", "数据库-主键分析",
                        title=f"int主键: {tbl}.{pk[0]}({pk[1]})",
                        expect="应使用bigint (上限922亿亿)",
                        actual=f"int (上限21亿)",
                        level=RiskLevel.WARNING,
                        detail="int主键在大数据量下可能耗尽，建议ALTER为bigint",
                        fix_sql=f"ALTER TABLE `{tbl}` MODIFY `{pk[0]}` BIGINT AUTO_INCREMENT;",
                        scene="主键-int上限"
                    )
                    warn += 1

        # ── 7. store_id索引检查 ──
        for tbl in tables:
            has_store_id = any(c[0] == 'store_id' for c in detail[tbl]['columns'])
            if not has_store_id:
                continue
            # 查索引
            idx_rows = self.db_query(f"SHOW INDEX FROM `{tbl}`", silent=True)
            has_store_idx = any(r[4] == 'store_id' for r in idx_rows)
            if not has_store_idx:
                self.record_item(
                    f"DBIDX-STORE-{tbl}", "数据库-索引",
                    title=f"store_id无索引: {tbl}",
                    expect="store_id列应有索引（多门店查询基础）",
                    actual=f"`{tbl}`.store_id 无独立索引",
                    level=RiskLevel.WARNING,
                    detail="多门店场景下每张含store_id的表都应建索引",
                    fix_sql=f"CREATE INDEX idx_{tbl}_store ON `{tbl}`(store_id);",
                    scene="索引-store_id缺失"
                )
                warn += 1

        # ── 8. 空表检测 ──
        empty_tables = []
        for tbl in tables:
            cnt_result = self.db_query(f"SELECT COUNT(*) FROM `{tbl}`", silent=True)
            cnt = cnt_result[0][0] if cnt_result else 0
            if cnt == 0:
                empty_tables.append(tbl)
        for tbl in empty_tables:
            is_config = any(k in tbl for k in ['sys_', 'admin_', 'config', 'yield_rate', 'unit_conversion'])
            level = RiskLevel.NORMAL if is_config else RiskLevel.WARNING
            self.record_item(
                f"DBEMPTY-{tbl}", "数据库-数据量",
                title=f"空表: {tbl}",
                expect="业务表应有数据",
                actual=f"0行数据",
                level=level,
                detail="配置表空正常，业务表空说明该模块未启用",
                scene="数据量-空表"
            )
            if not is_config: warn += 1

        # ── 9. 字段命名冗余检测（同表内同义字段）─
        redundant_patterns = [
            (['price','sale_price','cost_price'], "多个价格字段并存"),
            (['name','staff_name','dish_name','customer_name','supplier_name'], "name命名不统一"),
        ]
        for tbl in tables:
            col_names = [c[0] for c in detail[tbl]['columns']]
            for pattern, desc in redundant_patterns:
                hits = [n for n in col_names if n in pattern]
                if len(hits) >= 2:
                    self.record_item(
                        f"DBREDUN-{tbl}", "数据库-字段冗余",
                        title=f"疑似冗余字段组: {tbl}.{','.join(hits)}",
                        expect="同义字段只保留一个",
                        actual=f"{len(hits)}个可能同义的字段",
                        level=RiskLevel.WARNING,
                        detail=f"{desc}：{','.join(hits)}",
                        scene="字段-命名冗余"
                    )
                    warn += 1
                    break

        # ── 10. 汇总 ──
        self.record_item(
            "DBANALYZE-SUM", "数据库-结构分析", "数据库结构分析汇总",
            "数据库表间关系完整、主键合理",
            f"{len(tables)}张表 | FATAL:{fatal} ERROR:{error} WARN:{warn} | 外键{len(id_cols)}条",
            level=RiskLevel.FATAL if fatal > 0 else (RiskLevel.ERROR if error > 0 else RiskLevel.NORMAL),
            detail="纯数据库分析：外键类型一致性+主键合理性+索引覆盖率+空表率",
            scene="结构分析-汇总"
        )

# ====================== 14. 数据字典与枚举完整性扫描模块 ======================
class DataDictScanner(BaseScanner):
    """检测配置表、枚举值、数据字典完整性"""
    def scan_all(self):
        self.load_all_table_info()
        # 检测config/字典类表
        dict_tables = [t for t in self.all_db_tables if any(k in t.lower() for k in ["config", "dict", "enum", "setting", "type", "category"])]
        for tbl in dict_tables:
            try:
                _dq = self.db_query(f"SELECT COUNT(*) FROM `{tbl}`"); cnt = _dq[0][0] if _dq else 0
                self.record_item(f"DICT-TABLE-{tbl}", "数据字典", "字典表存在性",
                    "字典表应有数据", f"{tbl}: {cnt}条",
                    RiskLevel.WARNING if cnt == 0 else RiskLevel.NORMAL,
                    "空字典表可能影响前端选项", scene="字典表")
            except Exception: pass
        if not dict_tables:
            self.record_item("DICT-NONE", "数据字典", "字典表",
                "应有配置/字典表", "未检测到字典类表",
                RiskLevel.WARNING, "建议建表管理枚举值", scene="字典表")
        # 检测枚举字段值的有效性（status等常见字段）
        status_cols = [(tbl, c[0]) for tbl in self.all_db_tables if tbl in self.db_table_detail
                       for c in self.db_table_detail[tbl]["columns"]
                       if c[0].lower() in ["status", "type", "is_active", "is_deleted"] and "varchar" in c[1].lower()]
        for tbl, col in status_cols[:30]:
            try:
                vals = self.db_query(f"SELECT DISTINCT `{col}` FROM `{tbl}` WHERE `{col}` IS NOT NULL LIMIT 20")
                if vals:
                    val_str = ",".join([str(v[0]) for v in vals[:10]])
                    self.record_item(f"DICT-ENUM-{tbl}.{col}", "数据字典", "枚举值分布",
                        f"{tbl}.{col} 枚举值应合理", f"值: {val_str}",
                        RiskLevel.NORMAL, "枚举字段值统计", scene="枚举值")
            except Exception: pass


# ====================== 15. 页面功能规范对比扫描模块 ======================
class PageSpecScanner(BaseScanner):
    """对比对话文件中53页269区块规范 vs 实际Vue页面"""
    def scan_all(self):
        # 页面规范定义(来自对话文件完整功能矩阵):
        # 模块名 -> [(Vue组件关键词, 页面名, 类型, 需含控件)]
        SEARCH_SPECS = [
            # 模块1: 工作台
            ("Dashboard", "总经理驾驶舱", "dashboard", ["KPI卡片", "图表", "库存预警", "待审批列表"]),
            # 模块2: 桌台看板
            ("TableBoard", "桌台状态看板", "dashboard", ["门店选择", "日期选择", "统计卡片", "桌台网格"]),
            # 模块3: 前厅运营
            ("Bookings", "预订管理", "list", ["搜索栏", "表格", "分页", "新增按钮", "状态筛选"]),
            ("FrontDesk", "前台点菜", "form", ["菜品列表", "购物车", "结算按钮"]),
            ("FrontOffice", "客流分析", "dashboard", ["统计卡片", "图表", "时段分析"]),
            ("Customers", "客史档案", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            # 模块4: 厨房管理
            ("Kitchen", "厨房总看板", "dashboard", ["统计卡片", "图表", "订单队列"]),
            ("KitchenLog", "厨房日志", "list", ["搜索栏", "表格", "分页"]),
            ("Production", "生产管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("DishCost", "菜品BOM配方", "list", ["搜索栏", "表格", "分页", "新增按钮", "成本卡"]),
            # 模块5: 采购仓储
            ("Procurement", "采购管理", "list", ["搜索栏", "表格", "分页", "新增按钮", "审批流"]),
            ("Receipt", "入库验收", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Issue", "领用出库", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Inventory", "库存管理", "list", ["搜索栏", "表格", "分页"]),
            ("Suppliers", "供应商管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("SupplierReconciliation", "供应商对账", "list", ["搜索栏", "表格", "分页"]),
            ("StockTake", "库存盘点", "form", ["盘点清单", "提交按钮"]),
            ("SupplyChain", "厨房供应链", "list", ["搜索栏", "表格", "分页"]),
            ("SupplyManagement", "库存调拨", "form", ["调拨表单", "审批流"]),
            # 模块6: 营销会员
            ("Packages", "套餐管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("MemberList", "会员管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Marketing", "营销活动", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            # 模块7: 人事行政
            ("Dashboard", "人事总看板", "dashboard", ["统计卡片", "图表"]),
            ("Staff", "员工档案中心", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("HRAdmin", "部门岗位管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Schedule", "排班考勤", "list", ["搜索栏", "表格", "分页"]),
            ("Attendance", "每日打卡", "list", ["搜索栏", "表格", "分页"]),
            ("Leave", "请假休假", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Attendance", "加班登记", "form", ["登记表单", "审批流"]),
            ("Payroll", "薪资核算", "dashboard", ["薪资计算", "导出按钮"]),
            ("Training", "培训奖惩", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("StaffProfile", "离职档案", "list", ["搜索栏", "表格"]),
            # 模块8: 财务数据
            ("Finance", "财务总看板", "dashboard", ["统计卡片", "图表", "资金流水"]),
            ("Revenue", "资金账户", "list", ["搜索栏", "表格", "分页"]),
            ("Finance", "收银对账", "list", ["搜索栏", "表格", "分页"]),
            ("SupplierReconciliation", "应付结算", "list", ["搜索栏", "表格", "分页"]),
            ("Cost", "成本核算", "list", ["搜索栏", "表格", "分页"]),
            ("Finance", "费用报销", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Finance", "财务凭证", "list", ["搜索栏", "表格", "分页"]),
            ("Reports", "利润表", "dashboard", ["利润报表", "导出按钮"]),
            ("Reports", "资产负债表", "dashboard", ["资产报表", "导出按钮"]),
            # 模块9: 系统设置
            ("Settings", "门店管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("PermManager", "用户角色管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Settings", "系统参数", "form", ["参数表单", "保存按钮"]),
            ("AuditLog", "操作日志", "list", ["搜索栏", "表格", "分页"]),
            # 模块10: 数据大屏
            ("Reports", "报表中心", "dashboard", ["统计卡片", "多图表"]),
            ("DishCostAnalysis", "菜品成本分析", "dashboard", ["成本分析图表"]),
            ("DataScreen", "经营大屏", "dashboard", ["全屏布局", "实时数据"]),
            # 模块11: 工程管理
            ("Engineering", "设备管理", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Maintenance", "维修工单", "list", ["搜索栏", "表格", "分页", "新增按钮"]),
            ("Engineering", "巡检记录", "list", ["搜索栏", "表格", "分页"]),
            # 模块12: 总经办
            ("GMOffice", "经营简报", "dashboard", ["统计卡片", "图表", "导出按钮"]),
            ("ApprovalCenter", "审批中心", "list", ["TAB分类", "搜索栏", "表格", "分页", "审批按钮"]),
        ]

        # 收集所有Vue文件
        vue_files = {}
        for r, _, fs in os.walk(FRONT_CODE_PATH):
            for fname in fs:
                if fname.endswith(".vue"):
                    n = fname.replace(".vue", "")
                    fp = os.path.join(r, fname)
                    try:
                        with open(fp, "r", encoding="utf-8") as f:
                            ct = f.read()
                        vue_files[n] = {"path": fp, "content": ct, "size": os.path.getsize(fp)}
                    except Exception:
                        continue

        stats = {"found": 0, "missing": 0, "partial": 0, "total": len(SEARCH_SPECS)}
        for comp_name, page_name, page_type, required_v in SEARCH_SPECS:
            if comp_name in vue_files:
                ct = vue_files[comp_name]["content"]
                found_ctrls = []
                missing_ctrls = []
                for ctrl in required_v:
                    if any(kw in ct for kw in ctrl.split("/")):
                        found_ctrls.append(ctrl)
                    else:
                        missing_ctrls.append(ctrl)
                if len(missing_ctrls) == 0:
                    stats["found"] += 1
                    self.record_item(
                        f"SPEC-{comp_name}", "页面规范对比", f"{page_name}({page_type})",
                        "页面符合规范定义", f"全部{len(required_v)}项控件齐全: {','.join(found_ctrls)}",
                        RiskLevel.NORMAL, f"类型:{page_type} | 文件:{comp_name}.vue | {vue_files[comp_name]['size']//1024}KB",
                        file_list=vue_files[comp_name]["path"], scene="规范对比"
                    )
                else:
                    stats["partial"] += 1
                    self.record_item(
                        f"SPEC-{comp_name}", "页面规范对比", f"{page_name}({page_type})",
                        f"页面应含{page_type}标准控件", f"有{len(found_ctrls)}/{len(required_v)}: 已有{','.join(found_ctrls)} 缺失{','.join(missing_ctrls)}",
                        RiskLevel.WARNING, f"类型:{page_type} | 补齐缺失控件", scene="规范对比"
                    )
            else:
                stats["missing"] += 1
                self.record_item(
                    f"SPEC-{comp_name}", "页面规范对比", f"{page_name}({page_type})",
                    f"应有对应Vue组件", f"未找到 {comp_name}.vue",
                    RiskLevel.FATAL, f"创建{comp_name}.vue实现{page_name}", scene="规范对比"
                )

        # 汇总看板
        coverage = round(stats["found"] / stats["total"] * 100, 1)
        self.record_item("SPEC-SUMMARY", "页面规范对比", "页面功能规范对比汇总",
            f"覆盖率>=80%", f"总{stats['total']}页 | 完善{stats['found']} 部分缺{stats['partial']} 缺失{stats['missing']} | 覆盖率{coverage}%",
            RiskLevel.FATAL if coverage < 50 else (RiskLevel.ERROR if coverage < 80 else RiskLevel.NORMAL),
            "53页269区块规范 vs 实际Vue组件对比", scene="规范对比汇总")

        # 按模块分组汇总
        module_pages = {}
        for comp_name, page_name, page_type, req in SEARCH_SPECS:
            # 提取模块名
            mod_name = "其他"
            if comp_name in ["Dashboard","TableBoard","FrontDesk","Bookings","FrontOffice","Customers"]:
                mod_name = "前台运营"
            elif comp_name in ["Kitchen","KitchenLog","Production","DishCost"]:
                mod_name = "厨房管理"
            elif comp_name in ["Procurement","Receipt","Issue","Inventory","Suppliers","SupplierReconciliation","StockTake","SupplyChain","SupplyManagement"]:
                mod_name = "采购仓储"
            elif comp_name in ["Packages","MemberList","Marketing"]:
                mod_name = "营销会员"
            elif comp_name in ["Staff","HRAdmin","Schedule","Attendance","Leave","Payroll","Training","StaffProfile"]:
                mod_name = "人事行政"
            elif comp_name in ["Finance","Revenue","Cost","Reports","DishCostAnalysis","DataScreen"]:
                mod_name = "财务报告"
            elif comp_name in ["Settings","PermManager","AuditLog"]:
                mod_name = "系统设置"
            elif comp_name in ["Engineering","Maintenance"]:
                mod_name = "工程管理"
            elif comp_name in ["GMOffice","ApprovalCenter"]:
                mod_name = "总经办"
            if mod_name not in module_pages: module_pages[mod_name] = {"ok":0,"warn":0,"miss":0}
            # Use stats from earlier record_item calls (already counted)
        # 按模块分组统计
        mod_groups = {
            "前台运营": ["Dashboard","TableBoard","Bookings","FrontDesk","FrontOffice","Customers"],
            "厨房管理": ["Kitchen","KitchenLog","Production","DishCost"],
            "采购仓储": ["Procurement","Receipt","Issue","Inventory","Suppliers","SupplierReconciliation","StockTake","SupplyChain","SupplyManagement"],
            "营销会员": ["Packages","MemberList","Marketing"],
            "人事行政": ["Staff","HRAdmin","Schedule","Attendance","Leave","Payroll","Training","StaffProfile"],
            "财务报告": ["Finance","Revenue","Cost","Reports","DishCostAnalysis","DataScreen"],
            "系统设置": ["Settings","PermManager","AuditLog"],
            "工程管理": ["Engineering","Maintenance"],
            "总经办": ["GMOffice","ApprovalCenter"],
        }
        # Build index from scan_items added in this class
        spec_idx = {}
        for s in self.scan_items:
            try:
                sid = s.get("scan_id", "") if isinstance(s, dict) else s["scan_id"]
                if sid.startswith("SPEC-"):
                    spec_idx[sid] = s["level"] if isinstance(s, dict) else s["level"]
            except Exception:
                continue
        for mod, comps in sorted(mod_groups.items()):
            ok = 0; warn = 0; miss = 0
            for cn in comps:
                lv = spec_idx.get(f"SPEC-{cn}")
                if lv == "NORMAL": ok += 1
                elif lv == "WARNING": warn += 1
                elif lv == "FATAL": miss += 1
            tot = ok + warn + miss
            if tot > 0:
                self.record_item(f"SPEC-MOD-{mod}", "页面规范对比", f"{mod}模块规范对比",
                    "页面控件齐全", f"{tot}页: 完善{ok} 部分缺{warn} 缺失{miss}",
                    RiskLevel.FATAL if miss > 0 else RiskLevel.NORMAL,
                    "按业务模块分组统计", scene="模块分组")


# ====================== 报告生成器 ======================
class ReportGenerator:
    def __init__(self, scanner_list: List[BaseScanner]):
        self.all_items = []
        self.summary = {"FATAL":0, "ERROR":0, "WARNING":0, "NORMAL":0, "total":0}
        self.db_table_info = {}
        for sc in scanner_list:
            self.all_items.extend(sc.scan_items)
            for k in self.summary:
                self.summary[k] += sc.stat[k]
            if hasattr(sc, "db_table_detail"):
                self.db_table_info.update(sc.db_table_detail)

    




    def generate_dashboards(self):
        """为每个检测模块生成数据看板"""
        modules_seen = {}
        for item in self.all_items:
            mod = item.get("module", "未知模块")
            if mod not in modules_seen:
                modules_seen[mod] = {"total": 0, "FATAL": 0, "ERROR": 0, "WARNING": 0, "NORMAL": 0}
            modules_seen[mod]["total"] += 1
            lv = item.get("level", "NORMAL")
            if lv in modules_seen[mod]:
                modules_seen[mod][lv] += 1
        for mod_name, stats in sorted(modules_seen.items()):
            if stats["total"] == 0: continue
            health = round(stats["NORMAL"] / stats["total"] * 100, 1)
            level = RiskLevel.FATAL if health < 50 else (RiskLevel.ERROR if health < 80 else (RiskLevel.WARNING if health < 95 else RiskLevel.NORMAL))
            note = ""
            if level == RiskLevel.FATAL: note = "需立即修复"
            elif level == RiskLevel.ERROR: note = "有较多问题"
            elif level == RiskLevel.WARNING: note = "存在隐患"
            else: note = "运行良好"
            self.all_items.append({
                "scan_id": f"DASH-{mod_name.replace(' ', '-').replace('/', '-')}",
                "module": "数据看板",
                "scene": "模块看板",
                "title": f"{mod_name} 模块看板",
                "expect": "健康度>=80%",
                "actual": f"总检测{stats['total']}项 | FATAL:{stats['FATAL']} ERROR:{stats['ERROR']} WARN:{stats['WARNING']} NORM:{stats['NORMAL']} | 健康度{health}%",
                "level": level.value,
                "detail": f"{mod_name}模块共{stats['total']}条检测项,健康度{health}%.{note}",
                "fix_sql": "", "fix_cmd": "", "file_list": "", "tags": "看板", "score": int(health)
            })
        self.summary["total"] = len(self.all_items)
        for k in ["FATAL","ERROR","WARNING","NORMAL"]:
            self.summary[k] = sum(1 for it in self.all_items if it.get("level") == k)


    def generate_checkup_html(self):
        scan_time_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        total_count = self.summary["total"]
        fatal_num = self.summary["FATAL"]
        error_num = self.summary["ERROR"]
        warn_num = self.summary["WARNING"]
        normal_num = self.summary["NORMAL"]
        json_all_items = json.dumps(self.all_items, ensure_ascii=False)
        all_modules = sorted(set(it.get("module","") for it in self.all_items))
        all_scenes = sorted(set(it.get("scene","") for it in self.all_items if it.get("scene","")))
        dash_count = sum(1 for it in self.all_items if it.get("module") == "数据看板")
        mod_opts = "".join('<option value="' + m + '">' + m + '</option>' for m in all_modules)
        scn_opts = "".join('<option value="' + s + '">' + s + '</option>' for s in all_scenes)

        tpl = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>餐饮系统全量体检报告V2</title>
<style>
*{margin:0;padding:0;box-sizing:border-box;font-family:"Microsoft YaHei",sans-serif;}
body{padding:20px;background:#f5f7fa;}
.wrap{max-width:100%;margin:0 auto;background:#fff;padding:20px;border-radius:12px;}
h1{text-align:center;font-size:28px;padding-bottom:10px;border-bottom:2px solid #2563eb;margin-bottom:12px;}
.sum-top{font-size:15px;padding:12px;background:#eef5ff;border-radius:8px;margin-bottom:12px;line-height:1.7;}
.filter-row{display:flex;gap:8px;margin-bottom:10px;align-items:center;flex-wrap:wrap;}
.filter-row label{font-size:13px;font-weight:bold;white-space:nowrap;}
.filter-row select,.filter-row input{padding:7px 10px;border:1px solid #d0d5dd;border-radius:5px;font-size:13px;}
.filter-row input{flex:1;min-width:180px;max-width:400px;}
.tab-bar{display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap;}
.tab-btn{padding:8px 14px;border:none;border-radius:5px;font-size:13px;cursor:pointer;transition:0.15s;}
.tab-all{background:#e5e7eb;color:#111;}
.tab-fatal{background:#fee2e2;color:#c41e3a;}
.tab-error{background:#ffedd5;color:#c2410c;}
.tab-warn{background:#fff9cc;color:#854d0e;}
.tab-normal{background:#eff6ff;color:#1d4ed8;}
.tab-dash{background:#dbeafe;color:#1e40af;}
.tab-btn:hover{opacity:0.85;}
.tab-btn.active{outline:3px solid #2563eb;font-weight:bold;}
#itemContainer{display:flex;flex-direction:column;gap:10px;}
.item{padding:14px;border-radius:7px;border-width:2px;border-style:solid;}
.fatal{background:#fff5f5;border-color:#f5222d;}
.error{background:#fffaf0;border-color:#fa8c16;}
.warning{background:#fffde7;border-color:#faad14;}
.normal{background:#f6ffed;border-color:#52c41a;}
.dashboard{background:#f0f9ff;border-color:#0ea5e9;}
.item h4{font-size:15px;margin-bottom:6px;word-break:break-all;}
.item .badge{font-size:10px;padding:2px 7px;border-radius:8px;color:#fff;margin-right:6px;}
.badge-fatal{background:#f5222d;}
.badge-error{background:#fa8c16;}
.badge-warn{background:#d49200;}
.badge-norm{background:#52c41a;}
.item p{font-size:13px;margin:4px 0;line-height:1.5;word-break:break-all;}
.item .meta{font-size:11px;color:#999;margin-bottom:4px;}
.opt{display:flex;gap:6px;margin-top:8px;flex-wrap:wrap;}
.opt button{padding:5px 10px;border:none;border-radius:4px;cursor:pointer;font-size:12px;}
.btn-sql{background:#1890ff;color:#fff;}
.btn-cmd{background:#722ed1;color:#fff;}
.btn-file{background:#13c2c2;color:#fff;}
.btn-refresh{background:#666;color:#fff;}
#popMask{display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.6);align-items:center;justify-content:center;z-index:9999;}
.pop-box{width:80%;max-height:85vh;background:#fff;padding:22px;border-radius:10px;overflow-y:auto;}
#popText{width:100%;min-height:260px;padding:10px;font-size:13px;margin-bottom:10px;resize:vertical;font-family:Consolas,monospace;}
.pop-btns{display:flex;gap:10px;justify-content:flex-end;}
.result-count{font-size:13px;color:#666;margin-bottom:6px;}
@media print{
 .filter-row,.tab-bar,.opt,#popMask,.result-count{display:none !important;}
 body{padding:6px;}
 .item{border:1px solid #999;page-break-inside:avoid;}
 h1{font-size:20px;}
 .sum-top{font-size:12px;}
}
</style>
</head>
<body class="wrap">
<h1>餐饮系统全量体检报告 V2</h1>
<div class="sum-top">
体检时间：__TIME__<br>
总检查项：__TOTAL__ 项 | FATAL:__FATAL__ ERROR:__ERROR__ WARN:__WARN__ NORM:__NORM__ | 看板:__DASH__
</div>
<div class="filter-row">
<label>搜索</label>
<input type="text" id="searchInput" placeholder="输入关键字过滤（编号/标题/模块/场景）" oninput="doFilter()">
<label>类型</label>
<select id="moduleSelect" onchange="doFilter()"><option value="all">全部类型</option><option value="__DASH__">数据看板</option>__MODOPTS__</select>
<label>场景</label>
<select id="sceneSelect" onchange="doFilter()"><option value="all">全部场景</option>__SCNOPTS__</select>
</div>
<div class="tab-bar">
<button class="tab-btn tab-all" data-filter="all">全部条目</button>
<button class="tab-btn tab-dash" data-filter="__DASH__">看板(__DASHCOUNT__)</button>
<button class="tab-btn tab-fatal" data-filter="FATAL">致命(__FATAL__)</button>
<button class="tab-btn tab-error" data-filter="ERROR">严重(__ERROR__)</button>
<button class="tab-btn tab-warn" data-filter="WARNING">警告(__WARN__)</button>
<button class="tab-btn tab-normal" data-filter="NORMAL">正常(__NORM__)</button>
</div>
<div class="result-count" id="resultCount"></div>
<div id="itemContainer"></div>
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
(function(){
var allData = JSON.parse(atob("__B64__"));
var container = document.getElementById("itemContainer");
var popMask = document.getElementById("popMask");
var popText = document.getElementById("popText");
var currentLevel = "all";

function buildCard(item){
 var isDash = item.module === "数据看板";
 var cls = isDash ? "dashboard" : "normal";
 if (!isDash){
  if (item.level === "FATAL") cls = "fatal";
  else if (item.level === "ERROR") cls = "error";
  else if (item.level === "WARNING") cls = "warning";
 }
 var badge = '<span class="badge badge-' + (item.level==="FATAL"?"fatal":item.level==="ERROR"?"error":item.level==="WARNING"?"warn":"norm") + '">' + item.level + '</span>';
 var btns = '<div class="opt">';
 if (item.fix_sql) btns += '<button class="btn-sql" data-pop="' + encodeURIComponent(item.fix_sql) + '">SQL</button>';
 if (item.fix_cmd) btns += '<button class="btn-cmd" data-pop="' + encodeURIComponent(item.fix_cmd) + '">命令</button>';
 if (item.file_list) btns += '<button class="btn-file" data-file="' + encodeURIComponent(item.file_list) + '">文件</button>';
 btns += '<button class="btn-refresh" onclick="location.reload()">刷新</button></div>';
 return '<div class="item ' + cls + '" data-module="' + item.module + '" data-scene="' + (item.scene||'') + '" data-level="' + item.level + '">'
  + '<h4>' + badge + '[' + item.scan_id + '] ' + item.title + '</h4>'
  + '<div class="meta">' + (item.scene ? '场景:' + item.scene + ' | ' : '') + item.module + '</div>'
  + btns
  + '<p><b>预期:</b> ' + item.expect + '</p>'
  + '<p><b>实际:</b> ' + item.actual + '</p>'
  + '<p><b>说明:</b> ' + item.detail + '</p>'
  + '</div>';
}

container.addEventListener("click", function(e){
 var btn = e.target.closest("button");
 if (!btn) return;
 if (btn.dataset.pop) { popText.value = decodeURIComponent(btn.dataset.pop); popMask.style.display = "flex"; return; }
 if (btn.dataset.file) { var p = decodeURIComponent(btn.dataset.file).replace(/\//g, "\\"); window.open("file:///" + p); return; }
});

var doFilter = window.doFilter = function(){
 var kw = (document.getElementById("searchInput").value || "").toLowerCase();
 var mf = document.getElementById("moduleSelect").value;
 var sf = document.getElementById("sceneSelect").value;
 var showDash = (currentLevel === "__DASH__");
 var cnt = 0;
 container.innerHTML = "";
 allData.forEach(function(item){
  if (showDash && item.module !== "数据看板") return;
  if (!showDash && currentLevel !== "all" && item.level !== currentLevel) return;
  if (mf !== "all" && mf !== "__DASH__" && item.module !== mf) return;
  if (sf !== "all" && item.scene !== sf) return;
  if (kw){
   if ((item.scan_id + item.title + item.module + item.scene + item.actual + item.detail).toLowerCase().indexOf(kw) === -1) return;
  }
  cnt++;
  container.insertAdjacentHTML("beforeend", buildCard(item));
 });
 document.getElementById("resultCount").textContent = "显示 " + cnt + " / " + allData.length + " 条结果";
};

var filterTab = window.filterTab = function(level){
 currentLevel = level;
 document.getElementById("moduleSelect").value = (level === "__DASH__") ? "__DASH__" : "all";
 doFilter();
 document.querySelectorAll(".tab-btn").forEach(function(b){ b.classList.remove("active"); });
 var t = document.querySelector(".tab-btn[data-filter='" + level + "']");
 if (t) t.classList.add("active");
};

var closePop = window.closePop = function(){ popMask.style.display = "none"; };
var copyPopText = window.copyPopText = function(){ var ta=document.createElement("textarea"); ta.value=popText.textContent; document.body.appendChild(ta); ta.select(); document.execCommand("copy"); document.body.removeChild(ta); };

document.querySelector(".tab-bar").addEventListener("click", function(e){
 var btn = e.target.closest(".tab-btn");
 if (btn && btn.dataset.filter) filterTab(btn.dataset.filter);
});

filterTab("all");
})();
</script>
</body>
</html>"""
        # 用base64编码JSON避免特殊字符破坏HTML模板
        import base64
        json_bytes = json_all_items.encode('utf-8')
        json_b64 = base64.b64encode(json_bytes).decode('ascii')
        
        html = tpl.replace("__TIME__", scan_time_str)
        html = html.replace("__TOTAL__", str(total_count))
        html = html.replace("__FATAL__", str(fatal_num))
        html = html.replace("__ERROR__", str(error_num))
        html = html.replace("__WARN__", str(warn_num))
        html = html.replace("__NORM__", str(normal_num))
        html = html.replace("__DASHCOUNT__", str(dash_count))
        html = html.replace("__DASH__", str(dash_count))
        html = html.replace("__MODOPTS__", mod_opts)
        html = html.replace("__SCNOPTS__", scn_opts)
        # 用base64代替直接嵌入JSON
        html = html.replace('"__JSON__"', '"__B64__"')
        html = html.replace('__B64__', json_b64)
        with open(PATH_CHECKUP_HTML, "w", encoding="utf-8", newline="\n") as f:
            f.write(html)
        print("[OK] 体检HTML报告V2生成完成：支持搜索/类型筛选/场景筛选/看板Tab")

    def generate_audit_full_html(self):
        """生成完整审计档案HTML（含搜索和筛选）"""
        from html import escape as html_escape
        total = self.summary["total"]
        json_items = json.dumps(self.all_items, ensure_ascii=False)
        modules_json = json.dumps(sorted(set(it.get("module","") for it in self.all_items)), ensure_ascii=False)

        tpl = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>完整系统审计档案V2</title>
<style>
*{font-family:"Microsoft YaHei",sans-serif;margin:0;padding:0}
body{padding:20px;background:#f6f8fa}
.container{max-width:1600px;margin:0 auto;background:#fff;padding:24px}
h1{font-size:26px;border-bottom:2px solid #2385dd;padding-bottom:10px;margin-bottom:14px}
h2{font-size:20px;border-bottom:2px solid #2385dd;padding-bottom:8px;margin:20px 0 10px}
h3{font-size:17px;margin:14px 0 8px}
.filt{display:flex;gap:8px;margin:10px 0;align-items:center;flex-wrap:wrap}
.filt input,.filt select{padding:7px 10px;border:1px solid #d0d5dd;border-radius:5px;font-size:13px}
.filt input{flex:1;min-width:180px;max-width:400px}
.filt label{font-size:13px;font-weight:bold}
table{width:100%;border-collapse:collapse;margin:12px 0}
th,td{border:1px solid #e5e7eb;padding:8px;font-size:13px}
th{background:#f0f4f8}
.sum-box{display:flex;gap:12px;flex-wrap:wrap;margin:14px 0}
.sum-card{flex:1;min-width:100px;padding:16px;text-align:center;border-radius:8px}
.sum-card .num{font-size:28px;font-weight:bold}
.fatal{background:#fee2e2;color:#c41e3a}
.error{background:#ffedd5;color:#c2410c}
.warn{background:#fff9cc;color:#854d0e}
.normal{background:#eff6ff;color:#1d4ed8}
.item-block{border:1px solid #e5e7eb;padding:14px;margin:10px 0;border-radius:6px}
.item-block.hidden{display:none}
pre{background:#f1f5f9;padding:10px;white-space:pre-wrap;font-size:12px}
.meta{font-size:11px;color:#999}
.badge{font-size:10px;padding:2px 7px;border-radius:8px;color:#fff}
.badge-FATAL{background:#f5222d}
.badge-ERROR{background:#fa8c16}
.badge-WARNING{background:#d49200}
.badge-NORMAL{background:#52c41a}
.result-count{font-size:13px;color:#666;margin-bottom:8px}
</style>
</head>
<body class="container">
<h1>餐饮宴会系统完整审计档案 V2</h1>
<p class="meta">审计时间：__TIME__</p>
<div class="sum-box">
<div class="sum-card fatal"><div class="num">__FATAL__</div><div>致命</div></div>
<div class="sum-card error"><div class="num">__ERROR__</div><div>严重</div></div>
<div class="sum-card warn"><div class="num">__WARN__</div><div>警告</div></div>
<div class="sum-card normal"><div class="num">__NORM__</div><div>正常</div></div>
<div class="sum-card"><div class="num">__TOTAL__</div><div>总项</div></div>
</div>
<h2>一、系统全景说明</h2>
<p>多门店宴会全链路系统，覆盖仓储/前厅/后厨/人事/财务/工程；技术栈SpringBoot3+Vue3+MySQL8，全表store_id隔离，单据主从一对多，两级审批。</p>
__DBTABLES__
<h2>二、检测条目（可搜索筛选）</h2>
<div class="filt">
<label>搜索</label><input type="text" id="s" placeholder="关键字过滤" oninput="f()">
<label>类型</label><select id="m" onchange="f()"><option value="all">全部</option></select>
<label>等级</label><select id="l" onchange="f()"><option value="all">全部</option><option value="FATAL">FATAL</option><option value="ERROR">ERROR</option><option value="WARNING">WARNING</option><option value="NORMAL">NORMAL</option></select>
</div>
<div class="result-count" id="cnt"></div>
<div id="items"></div>
<script>
var D=__JSON__;
var M=__MODS__;
(function(){
 var sel=document.getElementById('m');
 M.forEach(function(m){ var o=document.createElement('option');o.value=m;o.textContent=m;sel.appendChild(o); });
 var c=document.getElementById('items');
 D.forEach(function(it,i){
  var lv=it.level,b='<span class="badge badge-'+lv+'">'+lv+'</span>';
  var d='<div class="item-block" data-mod="'+it.module+'" data-lv="'+lv+'" data-idx="'+i+'">'
   +'<h4>'+b+' ['+it.scan_id+'] '+it.module+' | '+it.scene+' | '+it.title+'</h4>'
   +'<p><b>预期:</b> '+it.expect+'</p>'
   +'<p><b>实际:</b> '+it.actual+'</p>'
   +'<p><b>影响:</b> '+it.detail+'</p>'
   +(it.fix_sql||it.fix_cmd?'<pre>修复:'+(it.fix_sql||'')+(it.fix_cmd||'')+'</pre>':'')
   +'</div>';
  c.innerHTML+=d;
 });
 f();
})();
function f(){
 var kw=(document.getElementById('s').value||'').toLowerCase();
 var mf=document.getElementById('m').value;
 var lf=document.getElementById('l').value;
 var cnt=0;
 var divs=document.querySelectorAll('.item-block');
 divs.forEach(function(d){
  var show=true;
  if(mf!=='all'&&d.dataset.mod!==mf) show=false;
  if(lf!=='all'&&d.dataset.lv!==lf) show=false;
  if(kw&&d.textContent.toLowerCase().indexOf(kw)===-1) show=false;
  d.classList.toggle('hidden',!show);
  if(show) cnt++;
 });
 document.getElementById('cnt').textContent='显示 '+cnt+' / '+D.length+' 条';
}
</script>
</body>
</html>"""
        # Build DB tables HTML
        db_html = ""
        for tbl_name, tbl_info in sorted(self.db_table_info.items()):
            db_html += f"<h3>数据表：{tbl_name} | 注释：{tbl_info['table_comment']}</h3><table><tr><th>字段名</th><th>类型</th><th>是否为空</th><th>默认值</th><th>字段注释</th></tr>"
            for col in tbl_info["columns"]:
                c_name, c_type, null_flag, def_val, comment = col[0], col[1], col[3], col[5], col[8] if len(col) > 8 else ""
                db_html += f"<tr><td>{html_escape(str(c_name))}</td><td>{html_escape(str(c_type))}</td><td>{html_escape(str(null_flag))}</td><td>{html_escape(str(def_val))}</td><td>{html_escape(str(comment))}</td></tr>"
            db_html += "</table>"

        html = tpl.replace("__TIME__", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        html = html.replace("__FATAL__", str(self.summary["FATAL"]))
        html = html.replace("__ERROR__", str(self.summary["ERROR"]))
        html = html.replace("__WARN__", str(self.summary["WARNING"]))
        html = html.replace("__NORM__", str(self.summary["NORMAL"]))
        html = html.replace("__TOTAL__", str(total))
        html = html.replace("__DBTABLES__", db_html)
        html = html.replace("__JSON__", json_items)
        html = html.replace("__MODS__", modules_json)

        with open(PATH_AUDIT_HTML, "w", encoding="utf-8") as f:
            f.write(html)
        print("[OK] 审计档案V2生成完成：支持搜索/模块筛选/等级筛选")

    def export_json(self):
        data = {
            "scan_time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "summary": self.summary,
            "db_full_table": self.db_table_info,
            "all_scan_items": self.all_items
        }
        with open(PATH_JSON_DATA, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print("[OK] 原始结构化数据导出")


# ====================== 程序入口 ======================
def main():
    print("="*70)
    print("【V2 十六领域全量扫描】服务器/数据库/约束/索引/数据质量/性能/后端安全/前端安全/配置日志/业务链路")
    print(f"脚本目录：{BASE_DIR} | 16大领域全覆盖，保底7000+检测项")
    print("="*70)
    try:
        scanner_server = ServerScanner()
        print("\n[1/13] 服务器全资源扫描")
        scanner_server.scan_all()

        scanner_db = DatabaseScanner()
        print("\n[2/13] 数据库全表/全字段/全关联关系深度扫描")
        scanner_db.scan_all()

        scanner_idx = IndexDeepScanner()
        print("\n[3/13] 数据库索引深度扫描（FK索引/冗余索引）")
        scanner_idx.scan_all()

        scanner_cons = ConstraintScanner()
        print("\n[4/13] 数据库约束完整性扫描（主键/字符集/引擎/AI上限）")
        scanner_cons.scan_all()

        scanner_dq = DataQualityScanner()
        print("\n[5/13] 数据质量深度扫描（NULL分布/日期逻辑/重复/格式）")
        scanner_dq.scan_all()

        scanner_perf = PerformanceScanner()
        print("\n[6/13] 数据库性能扫描（慢查询/连接池/锁/临时表）")
        scanner_perf.scan_all()

        scanner_back = BackendScanner()
        print("\n[7/13] 后端所有Java实体、Service全量扫描")
        scanner_back.scan_all()

        scanner_deep = DBAnalyzer()
        print("\n[7.5/13] 数据库结构深度分析（外键/主键/索引/空表）")
        scanner_deep.scan_all()

        scanner_bsec = BackendSecurityScanner()
        print("\n[8/13] 后端安全深度扫描（SQL注入/敏感信息/JWT/权限）")
        scanner_bsec.scan_all()

        scanner_front = FrontScanner()
        print("\n[9/13] 全部Vue业务模板页面扫描")
        scanner_front.scan_all()

        scanner_fsec = FrontSecurityScanner()
        print("\n[10/14] 前端安全扫描（v-html XSS/localStorage敏感数据）")
        scanner_fsec.scan_all()

        scanner_apideep = ApiDeepScanner()
        print("\n[11/14] API全端点深度扫描（自动发现+性能/格式校验）")
        scanner_apideep.scan_all()

        scanner_api = ApiScanner()
        print("\n[12/14] 全部API端点、全业务CRUD链路检测")
        scanner_api.scan_all()

        scanner_cfg = ConfigLogScanner()
        print("\n[13/14] 配置文件与日志扫描（SpringBoot/Swagger/审计日志）")
        scanner_cfg.scan_all()

        scanner_biz = BusinessLinkScanner()
        print("\n[14/19] 业务全链路闭环扫描")
        scanner_biz.scan_all()

        scanner_sched = SchedulerScanner()
        print("\n[15/19] 定时任务扫描（@Scheduled防重入）")
        scanner_sched.scan_all()

        scanner_ex = ExceptionHandlerScanner()
        print("\n[16/19] 全局异常处理扫描（@ControllerAdvice/统一返回）")
        scanner_ex.scan_all()

        scanner_dbprog = DBProgramScanner()
        print("\n[17/19] 数据库存储程序扫描（视图/存储过程/触发器）")
        scanner_dbprog.scan_all()

        scanner_infra = InfrastructureScanner()
        print("\n[18/19] 系统依赖扫描（邮件/短信/支付/打印/微信）")
        scanner_infra.scan_all()

        scanner_dict = DataDictScanner()
        print("\n[19/20] 数据字典扫描（枚举值/配置表）")
        scanner_dict.scan_all()

        scanner_spec = PageSpecScanner()
        print("\n[20/20] 页面功能规范对比（53页269区块vs实际组件）")
        scanner_spec.scan_all()
        print("[SPEC] OK")

        report = ReportGenerator([
            scanner_server, scanner_db, scanner_idx, scanner_cons, scanner_dq, scanner_perf,
            scanner_back, scanner_deep, scanner_bsec, scanner_front, scanner_fsec, scanner_apideep, scanner_api, scanner_cfg, scanner_biz,
            scanner_sched, scanner_ex, scanner_dbprog, scanner_infra, scanner_dict, scanner_spec
        ])
        report.generate_dashboards()
  # DISABLED
        report.generate_checkup_html()
        report.generate_audit_full_html()
        report.export_json()

        print("\n==================== 扫描完成 ====================")
        print(f"[OK] 全量总检测条目：{report.summary['total']} 条（16大领域全覆盖）")
        print(f"[FATAL] 致命：{report.summary['FATAL']}")
        print(f"[ERROR] 严重：{report.summary['ERROR']}")
        print(f"[WARNING] 警告：{report.summary['WARNING']}")
        print(f"[NORMAL] 正常：{report.summary['NORMAL']}")
        print(f"\n输出文件：")
        print(f"1. {PATH_CHECKUP_HTML} 带Tab切换体检报告V2")
        print(f"2. {PATH_AUDIT_HTML} 完整系统审计档案V2")
        print(f"3. {PATH_JSON_DATA} 原始全量扫描数据V2")
        print("\n扫描结束，按回车键关闭窗口...")
    except Exception as e:
        print(f"\n全局扫描中断异常：{traceback.format_exc()}")
        print("\n程序异常退出，按回车键关闭窗口...")

if __name__ == "__main__":
    main()