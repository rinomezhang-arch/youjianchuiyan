#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
孤儿记录清理脚本
清理3个逻辑外键孤儿:
  1. sys_role.store_id → store_info.store_id (设为默认门店1)
  2. sys_user_role.staff_id → staff_master.staff_id (删除无意义角色分配)
  3. sys_user_role.store_id → store_info.store_id (设为默认门店1)
用法: python fix_orphans.py [--dry-run]
"""

import os, sys, subprocess, argparse, datetime, configparser

# ── 配置 ──────────────────────────────────────────────────────────
def load_mysql_config():
    cfg = {"host": "127.0.0.1", "port": "3306", "user": "rino", "password": "Wo002323", "database": "banquet"}
    conf_path = os.path.join(os.path.expanduser("~"), ".mysql.conf")
    if os.path.isfile(conf_path):
        cp = configparser.ConfigParser()
        cp.read(conf_path, encoding="utf-8")
        if "mysql" in cp:
            s = cp["mysql"]
            cfg.update({k: s.get(k, v) for k, v in cfg.items()})
    return cfg

MYSQL = load_mysql_config()

def mysql_exec(sql, fetch=False):
    cmd = ["mysql", f"-h{MYSQL['host']}", f"-P{MYSQL['port']}", f"-u{MYSQL['user']}",
           f"-p{MYSQL['password']}", MYSQL["database"], "--batch", "--raw", "-N", "-e", sql]
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if p.returncode != 0:
        print(f"  [ERROR] MySQL: {p.stderr.strip()}")
        return []
    if fetch:
        return [l for l in p.stdout.strip().split("\n") if l]
    return []

def mysql_count(sql):
    rows = mysql_exec(sql, fetch=True)
    try:
        return int(rows[0]) if rows else 0
    except (ValueError, IndexError):
        return 0

# ── 清理规则 ──────────────────────────────────────────────────────
CLEANUP_RULES = [
    {
        "id": "ORPHAN-001",
        "table": "sys_role",
        "column": "store_id",
        "ref_table": "store_info",
        "ref_column": "store_id",
        "action": "update_default",  # 设为默认值1
        "default_value": "1",
        "description": "sys_role.store_id 不在 store_info 中, 设为默认门店1",
    },
    {
        "id": "ORPHAN-002",
        "table": "sys_user_role",
        "column": "staff_id",
        "ref_table": "staff_master",
        "ref_column": "staff_id",
        "action": "delete",  # 删除无意义角色分配
        "description": "sys_user_role.staff_id 不在 staff_master 中, 删除无意义角色分配",
    },
    {
        "id": "ORPHAN-003",
        "table": "sys_user_role",
        "column": "store_id",
        "ref_table": "store_info",
        "ref_column": "store_id",
        "action": "update_default",
        "default_value": "1",
        "description": "sys_user_role.store_id 不在 store_info 中, 设为默认门店1",
    },
]

def run_cleanup(dry_run=False):
    ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"\n{'='*60}")
    print(f"  孤儿记录清理 {'[DRY-RUN]' if dry_run else '[EXECUTE]'}")
    print(f"  时间: {ts}")
    print(f"{'='*60}\n")

    results = []
    for rule in CLEANUP_RULES:
        t = rule["table"]
        c = rule["column"]
        rt = rule["ref_table"]
        rc = rule["ref_column"]

        # 查找孤儿记录
        count_sql = (f"SELECT COUNT(*) FROM `{t}` t1 "
                     f"LEFT JOIN `{rt}` t2 ON t1.`{c}`=t2.`{rc}` "
                     f"WHERE t1.`{c}` IS NOT NULL AND t2.`{rc}` IS NULL")
        orphan_count = mysql_count(count_sql)

        # 查看孤儿数据样本
        sample_sql = (f"SELECT t1.* FROM `{t}` t1 "
                      f"LEFT JOIN `{rt}` t2 ON t1.`{c}`=t2.`{rc}` "
                      f"WHERE t1.`{c}` IS NOT NULL AND t2.`{rc}` IS NULL LIMIT 5")
        samples = mysql_exec(sample_sql, fetch=True)

        print(f"[{rule['id']}] {rule['description']}")
        print(f"  表: {t}.{c} → {rt}.{rc}")
        print(f"  孤儿记录数: {orphan_count}")
        if samples:
            print(f"  样本(前5条):")
            for s in samples[:5]:
                print(f"    {s}")

        if orphan_count == 0:
            print(f"  状态: 无需清理\n")
            results.append({**rule, "orphan_count": 0, "action_taken": "none"})
            continue

        if dry_run:
            print(f"  [DRY-RUN] 将执行: {rule['action']}\n")
            results.append({**rule, "orphan_count": orphan_count, "action_taken": "dry_run"})
            continue

        # 执行清理
        if rule["action"] == "update_default":
            dv = rule["default_value"]
            sql = (f"UPDATE `{t}` SET `{c}`={dv} "
                   f"WHERE `{c}` IS NOT NULL "
                   f"AND `{c}` NOT IN (SELECT `{rc}` FROM `{rt}`)")
            mysql_exec(sql)
            print(f"  已执行 UPDATE: {t}.{c} → {dv}")
            # 验证
            remaining = mysql_count(count_sql)
            print(f"  剩余孤儿: {remaining}\n")
            results.append({**rule, "orphan_count": orphan_count, "action_taken": "update", "remaining": remaining})

        elif rule["action"] == "delete":
            sql = (f"DELETE FROM `{t}` "
                   f"WHERE `{c}` IS NOT NULL "
                   f"AND `{c}` NOT IN (SELECT `{rc}` FROM `{rt}`)")
            mysql_exec(sql)
            print(f"  已执行 DELETE from {t} (staff_id not in staff_master)")
            remaining = mysql_count(count_sql)
            print(f"  剩余孤儿: {remaining}\n")
            results.append({**rule, "orphan_count": orphan_count, "action_taken": "delete", "remaining": remaining})

    # 汇总
    print(f"{'='*60}")
    print(f"  清理汇总 {'[DRY-RUN]' if dry_run else '[DONE]'}")
    total_orphans = sum(r["orphan_count"] for r in results)
    total_fixed = sum(r.get("orphan_count", 0) for r in results if r.get("action_taken") in ("update", "delete"))
    print(f"  总孤儿数: {total_orphans}")
    print(f"  已修复: {total_fixed}")
    for r in results:
        status = r["action_taken"]
        cnt = r["orphan_count"]
        print(f"    {r['id']}: {r['table']}.{r['column']} → {r['ref_table']} | {cnt}条 | {status}")
    print(f"{'='*60}\n")

    return results

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="清理逻辑外键孤儿记录")
    parser.add_argument("--dry-run", action="store_true", help="只查看不执行")
    args = parser.parse_args()
    run_cleanup(dry_run=args.dry_run)
