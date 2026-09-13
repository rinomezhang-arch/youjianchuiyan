#!/usr/bin/env python3
# DL-RC-STOCKTAKE-HTTP-R3-53 合成账号口令注入（严格受限于任务卡）
#
# 约束：
# - 只更新 tr37_stocktake_20260909 中已存在的 3 个 SYN 合成账号的 BCrypt 哈希
# - 更新前逐项断言 staff_account / staff_id / store_id / role 完全匹配，且账号含 SYN 标识
# - 口令在进程内随机生成，不落盘、不打印、不进日志
# - 不新增、不删除、不改真实账号
# - 连接目标常量锁死，无环境变量注入点
import subprocess, sys, json, secrets, bcrypt

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
HOST, PORT, SCHEMA = "127.0.0.1", "13318", "tr37_stocktake_20260909"

EXPECTED = [
    {"staff_id": 1,  "account": "synmgr1", "store_id": 1, "role": "store_manager"},
    {"staff_id": 2,  "account": "synmgr2", "store_id": 2, "role": "store_manager"},
    {"staff_id": 99, "account": "syngm99", "store_id": 0, "role": "gm"},
]

def sql(q, db=SCHEMA):
    args = [MYSQL, "-h", HOST, "-P", PORT, "-u", "root", "-N", "-B"]
    if db:
        args.append(db)
    args += ["-e", q]
    out = subprocess.run(args, capture_output=True, encoding="utf-8")
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip())
    return out.stdout.strip()

def rows(q):
    out = sql(q)
    return [l.split("\t") for l in out.splitlines()] if out else []

# ---- 守卫 ----
port = rows("SELECT @@port")[0][0]
if str(port) != PORT:
    print("GUARD_FAIL port=" + str(port)); sys.exit(2)
datadir = rows("SELECT @@datadir")[0][0]
if "mysql-test-13317" not in datadir.replace("\\", "/").lower():
    print("GUARD_FAIL datadir=" + datadir); sys.exit(2)

# ---- 逐项断言 ----
actual = rows("SELECT staff_id, staff_account, store_id, role FROM staff_master ORDER BY staff_id")
amap = {int(r[0]): {"account": r[1], "store_id": int(r[2]), "role": r[3]} for r in actual}
checks, all_ok = [], True
for e in EXPECTED:
    a = amap.get(e["staff_id"])
    ok = (a is not None and a["account"] == e["account"]
          and a["store_id"] == e["store_id"] and a["role"] == e["role"]
          and a["account"].lower().startswith("syn"))
    all_ok = all_ok and ok
    checks.append({"staff_id": e["staff_id"], "expected": e["account"],
                   "actual": a["account"] if a else None, "match": ok})
if not all_ok:
    print("PRECONDITION_FAIL=" + json.dumps(checks, ensure_ascii=False)); sys.exit(3)

# ---- 进程内随机口令 + BCrypt(cost=10) ----
results = []
for e in EXPECTED:
    plain = secrets.token_urlsafe(24)                     # 仅存在于本进程
    h = bcrypt.hashpw(plain.encode(), bcrypt.gensalt(rounds=10)).decode()
    esc = h.replace("'", "''")
    sql(f"UPDATE staff_master SET staff_password='{esc}' WHERE staff_id={e['staff_id']} AND staff_account='{e['account']}'")
    back = rows(f"SELECT staff_password FROM staff_master WHERE staff_id={e['staff_id']}")
    results.append({"staff_id": e["staff_id"], "account": e["account"],
                    "updated": len(back) == 1 and back[0][0] == h,
                    "hash_prefix": h[:7]})

print(json.dumps({
    "ok": all(r["updated"] for r in results),
    "guard": {"port": str(port), "datadir": datadir},
    "precondition_checks": checks,
    "updated_rows": len(results),
    "accounts": [{"staff_id": r["staff_id"], "account": r["account"], "updated": r["updated"]} for r in results],
    "plaintext_printed": False,
}, ensure_ascii=False, indent=2))
