#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""TL-IPAD-V2-PREFLIGHT-28 校验器 r1 严格版（纯函数，只读 metadata.json）。
不连接数据库、不执行 DDL。严格必填/类型/完整索引与 FK 校验：未知、未采集、畸形一律 BLOCKED 零 DDL。
输出 READY_FOR_V2 / ALREADY_APPLIED / BLOCKED + 原因 + 缺项规范 DDL。
命令行退出码: BLOCKED -> 1, READY_FOR_V2/ALREADY_APPLIED -> 0。
规范来源: ipad_batch_request_migration_v2.sql（base_sha 5f4511b7）。
"""

import json
import sys

STEP1_DDL = ("ALTER TABLE booking_master\n"
             "    ADD UNIQUE INDEX uk_booking_master_id_store_booking (id, store_id, booking_id);")
STEP2_DDL = ("ALTER TABLE ipad_batch_request\n"
             "    ADD CONSTRAINT fk_ipad_batch_booking_scope\n"
             "    FOREIGN KEY (booking_master_id, store_id, booking_id)\n"
             "    REFERENCES booking_master (id, store_id, booking_id)\n"
             "    ON DELETE RESTRICT ON UPDATE RESTRICT;")

EXPECTED_IDX = {"name": "uk_booking_master_id_store_booking", "unique": True,
                "columns": ["id", "store_id", "booking_id"]}
EXPECTED_FK = {"name": "fk_ipad_batch_booking_scope",
               "columns": ["booking_master_id", "store_id", "booking_id"],
               "ref_table": "booking_master", "ref_columns": ["id", "store_id", "booking_id"],
               "on_delete": "RESTRICT", "on_update": "RESTRICT"}

REQUIRED_COUNTS = ("orphan_receipts", "cross_store", "booking_id_mismatch")


def _blocked(reasons):
    return {"status": "BLOCKED", "reason": reasons, "ddl": []}


def _is_bool(v):
    return type(v) is bool


def _is_int(v):
    return type(v) is int and not isinstance(v, bool)


def _valid_col_def(c):
    """列定义必填校验：dict、type 非空 str、unsigned 真正 bool。"""
    if not isinstance(c, dict):
        return False, "列定义不是对象"
    t = c.get("type")
    if not isinstance(t, str) or not t.strip():
        return False, "列定义缺少有效 type"
    if not _is_bool(c.get("unsigned")):
        return False, "unsigned 必须是真正 bool"
    if not _is_bool(c.get("nullable")):
        return False, "nullable 必须显式采集为 bool"
    return True, None


def check_metadata(md):
    try:
        return _check(md)
    except Exception as e:  # 畸形输入不抛异常，一律 BLOCKED
        return _blocked(["输入畸形导致校验异常: %s: %s" % (type(e).__name__, e)])


def _check(md):
    if not isinstance(md, dict):
        return _blocked(["metadata 不是对象"])
    schema = md.get("schema")
    if not isinstance(schema, str) or not schema.strip():
        return _blocked(["schema 缺失或未采集"])
    if "tables" not in md or not isinstance(md["tables"], dict):
        return _blocked(["tables 缺失或不是对象（采集未完成，禁止放行）"])
    tables = md["tables"]
    for t in ("booking_master", "ipad_batch_request"):
        if t not in tables or not isinstance(tables[t], dict):
            return _blocked(["表 %s 缺失（采集未完成，禁止放行）" % t])
    if "counts" not in md or not isinstance(md["counts"], dict):
        return _blocked(["counts 缺失（采集未完成，禁止放行）"])
    if "foreign_key_checks" not in md:
        return _blocked(["foreign_key_checks 缺失"])

    # foreign_key_checks 严格整数 1
    if not _is_int(md["foreign_key_checks"]) or md["foreign_key_checks"] != 1:
        return _blocked(["foreign_key_checks 必须是整数 1（实际: %r）" % md["foreign_key_checks"]])

    # counts：三键必填，每项真正 int 且 >=0；缺失/null/bool/string/负数一律 BLOCKED
    for k in REQUIRED_COUNTS:
        if k not in md["counts"]:
            return _blocked(["counts.%s 缺失（采集未完成，禁止把缺失当 0）" % k])
        v = md["counts"][k]
        if not _is_int(v):
            return _blocked(["counts.%s 必须是整数（实际 %r，缺失/null/bool/string 不能当 0）" % (k, v)])
        if v < 0:
            return _blocked(["counts.%s 为负（%s），数据异常禁止放行" % (k, v)])
    dirty = {k: md["counts"][k] for k in REQUIRED_COUNTS if md["counts"][k] > 0}
    if dirty:
        return _blocked(["回执脏数据计数非零: " + ", ".join("%s=%s" % (k, v) for k, v in dirty.items())])

    bm = tables["booking_master"]
    ipad = tables["ipad_batch_request"]
    for key in ("booking_master_id", "store_id", "booking_id"):
        column = ipad.get("columns", {}).get(key)
        if isinstance(column, dict) and column.get("nullable") is not False:
            return _blocked(["ipad_batch_request.%s 必须显式非空，避免空值跳过范围外键" % key])

    # present 必须真正 bool；v1 表缺失 -> BLOCKED 提示先建 v1
    if not _is_bool(ipad.get("present")):
        return _blocked(["ipad_batch_request.present 必须是真正 bool"])
    if not _is_bool(bm.get("present")):
        return _blocked(["booking_master.present 必须是真正 bool"])
    if not ipad["present"]:
        return _blocked(["ipad_batch_request 表缺失（v1 未建）：先按 ipad_batch_request_migration_v1.sql 建表，再评估 v2"])
    if not bm["present"]:
        return _blocked(["booking_master 表缺失"])

    # indexes/foreign_keys 必须显式采集的数组，缺失当未采集 -> BLOCKED
    for tname, t in (("booking_master", bm), ("ipad_batch_request", ipad)):
        for key in ("indexes", "foreign_keys"):
            if key not in t or not isinstance(t[key], list):
                return _blocked(["%s.%s 未显式采集（禁止把缺失当空表）" % (tname, key)])

    # 列定义校验
    parent_cols = ("id", "store_id", "booking_id")
    child_cols = ("booking_master_id", "store_id", "booking_id")
    cols_meta = {}
    for cname in parent_cols:
        c = bm.get("columns", {}).get(cname) if isinstance(bm.get("columns"), dict) else None
        if c is None:
            return _blocked(["booking_master.%s 列缺失" % cname])
        ok, err = _valid_col_def(c)
        if not ok:
            return _blocked(["booking_master.%s: %s" % (cname, err)])
        cols_meta[("bm", cname)] = c
    for cname in child_cols:
        c = ipad.get("columns", {}).get(cname) if isinstance(ipad.get("columns"), dict) else None
        if c is None:
            return _blocked(["ipad_batch_request.%s 列缺失" % cname])
        ok, err = _valid_col_def(c)
        if not ok:
            return _blocked(["ipad_batch_request.%s: %s" % (cname, err)])
        cols_meta[("ipad", cname)] = c

    # id/store_id 按规范 signed BIGINT（type=bigint 且 unsigned=False），父/子一致
    for pc, cc in (("id", "booking_master_id"), ("store_id", "store_id")):
        p, c = cols_meta[("bm", pc)], cols_meta[("ipad", cc)]
        if p["type"].lower() != "bigint" or p["unsigned"] is not False:
            return _blocked(["booking_master.%s 必须是 signed BIGINT（type=%r unsigned=%r）" % (pc, p["type"], p["unsigned"])])
        if c["type"].lower() != "bigint" or c["unsigned"] is not False:
            return _blocked(["ipad_batch_request.%s 必须是 signed BIGINT（type=%r unsigned=%r）" % (cc, c["type"], c["unsigned"])])
        if p["type"].lower() != c["type"].lower() or p["unsigned"] != c["unsigned"]:
            return _blocked(["父 booking_master.%s 与子 ipad_batch_request.%s 类型/符号不一致" % (pc, cc)])

    # booking_id 字符类型，length/charset/collation 必填且父/子兼容
    pb, cb = cols_meta[("bm", "booking_id")], cols_meta[("ipad", "booking_id")]
    for tag, c in (("父 booking_master.booking_id", pb), ("子 ipad_batch_request.booking_id", cb)):
        if c["type"].lower() not in ("varchar", "char"):
            return _blocked(["%s 必须是字符类型(varchar/char)，实际 %r" % (tag, c["type"])])
        if not _is_int(c.get("length")) or c["length"] <= 0:
            return _blocked(["%s 缺少有效 length（整数>0）" % tag])
        if not isinstance(c.get("charset"), str) or not c["charset"]:
            return _blocked(["%s 缺少有效 charset" % tag])
        if not isinstance(c.get("collation"), str) or not c["collation"]:
            return _blocked(["%s 缺少有效 collation" % tag])
    if pb["charset"].lower() != cb["charset"].lower() or pb["collation"].lower() != cb["collation"].lower():
        return _blocked(["booking_id 字符集/排序规则不一致: 父 %s/%s vs 子 %s/%s"
                         % (pb["charset"], pb["collation"], cb["charset"], cb["collation"])])

    # 索引：同名存在但 unique 非真 True、列缺 prefix 或 prefix 非空（前缀索引）、列序不符 -> BLOCKED
    idx = None
    for ix in bm["indexes"]:
        if not isinstance(ix, dict):
            return _blocked(["booking_master.indexes 含非对象项"])
        if str(ix.get("name", "")).lower() == EXPECTED_IDX["name"].lower():
            idx = ix
            break
    if idx is not None:
        if not _is_bool(idx.get("unique")) or idx["unique"] is not True:
            return _blocked(["同名索引 %s 存在但 unique 不是真正 true（实际 %r），不得 drop/替换" % (EXPECTED_IDX["name"], idx.get("unique"))])
        cols = idx.get("columns")
        if not isinstance(cols, list) or len(cols) != 3:
            return _blocked(["同名索引 %s 列采集不完整/不是数组（实际 %r）" % (EXPECTED_IDX["name"], cols)])
        names = []
        for col in cols:
            if not isinstance(col, dict) or not isinstance(col.get("name"), str):
                return _blocked(["同名索引 %s 列项必须是 {name,prefix} 对象" % EXPECTED_IDX["name"]])
            if "prefix" not in col:
                return _blocked(["索引列 prefix 未采集，不能推断为完整列索引"])
            prefix = col["prefix"]
            if prefix is not None:
                return _blocked(["同名索引 %s 含前缀索引列 %s(prefix=%r)，前缀索引不能冒充完整列索引" % (EXPECTED_IDX["name"], col["name"], prefix)])
            names.append(col["name"].lower())
        if names != EXPECTED_IDX["columns"]:
            return _blocked(["同名索引 %s 存在但列序/列集不符（实际 %s），不得 drop/替换" % (EXPECTED_IDX["name"], names)])

    # FK：同名存在但定义（列序/目标/删除更新规则/同库）不符 -> BLOCKED
    fk = None
    for f in ipad["foreign_keys"]:
        if not isinstance(f, dict):
            return _blocked(["ipad_batch_request.foreign_keys 含非对象项"])
        if str(f.get("name", "")).lower() == EXPECTED_FK["name"].lower():
            fk = f
            break
    if fk is not None:
        ref_schema = fk.get("ref_schema")
        if not isinstance(ref_schema, str) or ref_schema != schema:
            return _blocked(["FK 引用schema缺失或不是当前目标库"])
        ok = ([str(x).lower() for x in fk.get("columns", [])] == EXPECTED_FK["columns"]
              and str(fk.get("ref_table", "")).lower() == EXPECTED_FK["ref_table"]
              and [str(x).lower() for x in fk.get("ref_columns", [])] == EXPECTED_FK["ref_columns"]
              and str(fk.get("on_delete", "")).upper() == EXPECTED_FK["on_delete"]
              and str(fk.get("on_update", "")).upper() == EXPECTED_FK["on_update"])
        if not ok:
            return _blocked(["同名 FK %s 存在但定义不同（%s），不得 drop/替换" % (EXPECTED_FK["name"], json.dumps(fk, ensure_ascii=False))])

    # 判定：不假定同名单列 FK(fk_ipad_batch_booking) 是 v2 复合 FK
    if idx is not None and fk is not None:
        return {"status": "ALREADY_APPLIED",
                "reason": ["uk_booking_master_id_store_booking 三列唯一索引与 fk_ipad_batch_booking_scope 三列复合外键均已精确存在"],
                "ddl": []}
    ddl = []
    if idx is None:
        ddl.append(STEP1_DDL)
    if fk is None:
        ddl.append(STEP2_DDL)
    return {"status": "READY_FOR_V2",
            "reason": ["存在缺失的规范 v2 步骤，输出对应规范 DDL（真实 MySQL 尚未执行）"],
            "ddl": ddl}


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "metadata.json"
    with open(path, "r", encoding="utf-8") as f:
        md = json.load(f)
    res = check_metadata(md)
    print(json.dumps(res, ensure_ascii=False, indent=2))
    sys.exit(1 if res["status"] == "BLOCKED" else 0)


if __name__ == "__main__":
    main()
