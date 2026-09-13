#!/usr/bin/env python3
# DL-RC-STOCKTAKE-HTTP-R3-53 登录可用性验证
# 在进程内为 SYN 账号设置随机口令并立即用真实后端验证登录；明文不落盘不输出。
import subprocess, secrets, bcrypt, json, urllib.request

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
BACKEND = "http://127.0.0.1:18080"
SCHEMA = "tr37_stocktake_20260909"

def sql(q):
    return subprocess.run(
        [MYSQL, "-h", "127.0.0.1", "-P", "13318", "-u", "root", "-N", "-B", SCHEMA, "-e", q],
        capture_output=True, encoding="utf-8").stdout.strip()

def login(username, password):
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(BACKEND + "/api/auth/login", data=body,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.loads(r.read().decode())
    except Exception as e:
        return None, {"error": str(e)}

out = []
for sid, acct in [(1, "synmgr1"), (2, "synmgr2"), (99, "syngm99")]:
    plain = secrets.token_urlsafe(24)
    h = bcrypt.hashpw(plain.encode(), bcrypt.gensalt(rounds=10)).decode()
    sql(f"UPDATE staff_master SET staff_password='{h}' WHERE staff_id={sid} AND staff_account='{acct}'")
    status, j = login(acct, plain)
    out.append({
        "account": acct, "http": status,
        "code": j.get("code"),
        "role": (j.get("data") or {}).get("user", {}).get("role") if isinstance(j.get("data"), dict) else None,
        "storeId": (j.get("data") or {}).get("storeId") if isinstance(j.get("data"), dict) else None,
        "has_token": bool((j.get("data") or {}).get("token")) if isinstance(j.get("data"), dict) else False,
    })
    del plain  # 立即丢弃

print(json.dumps({"login_probe": out, "plaintext_printed": False}, ensure_ascii=False, indent=2))
