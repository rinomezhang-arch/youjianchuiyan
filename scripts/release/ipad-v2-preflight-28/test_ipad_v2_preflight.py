#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""TL-IPAD-V2-PREFLIGHT-28 固定夹具单元测试 r1（标准库 unittest，不连库、不执行 DDL）。
覆盖：原 8 项语义 + Codex 5 个反例（缺 counts / orphan null / orphan -1 / 父子均缺 type / unique 字符串 false）
     + 关键缺失/畸形类型（indexes 未采集 / fkc 字符串 / counts 字符串 / unsigned 缺失 / prefix 前缀索引）。
夹具是固定 dict，绝不充当真实 MySQL 验收。
运行: python3 -m unittest test_ipad_v2_preflight -v
"""

import unittest

from check import check_metadata, STEP1_DDL, STEP2_DDL

IDX_OK = {"name": "uk_booking_master_id_store_booking", "unique": True,
          "columns": [{"name": "id", "prefix": None}, {"name": "store_id", "prefix": None},
                      {"name": "booking_id", "prefix": None}]}
FK_V1 = {"name": "fk_ipad_batch_booking", "columns": ["booking_master_id"],
         "ref_table": "booking_master", "ref_columns": ["id"],
         "on_delete": "RESTRICT", "on_update": "NO ACTION"}
FK_V2_OK = {"name": "fk_ipad_batch_booking_scope",
            "ref_schema": "isolated_test",
            "columns": ["booking_master_id", "store_id", "booking_id"],
            "ref_table": "booking_master", "ref_columns": ["id", "store_id", "booking_id"],
            "on_delete": "RESTRICT", "on_update": "RESTRICT"}


def bigint_col(unsigned=False):
    return {"type": "bigint", "unsigned": unsigned, "nullable": False}


def vchar_col(length=255, charset="utf8mb4", collation="utf8mb4_0900_ai_ci"):
    return {"type": "varchar", "length": length, "unsigned": False, "nullable": False,
            "charset": charset, "collation": collation}


def base_md(**over):
    md = {
        "schema": "isolated_test",
        "foreign_key_checks": 1,
        "tables": {
            "booking_master": {
                "present": True,
                "columns": {"id": bigint_col(), "store_id": bigint_col(), "booking_id": vchar_col()},
                "indexes": [],
                "foreign_keys": [],
            },
            "ipad_batch_request": {
                "present": True,
                "columns": {"booking_master_id": bigint_col(), "store_id": bigint_col(), "booking_id": vchar_col()},
                "indexes": [],
                "foreign_keys": [FK_V1],
            },
        },
        "counts": {"orphan_receipts": 0, "cross_store": 0, "booking_id_mismatch": 0},
    }
    md.update(over)
    return md


def with_index(md, ix=IDX_OK):
    md["tables"]["booking_master"]["indexes"] = [dict(ix)]
    return md


def with_scope_fk(md, fk=FK_V2_OK):
    md["tables"]["ipad_batch_request"]["foreign_keys"] = [dict(FK_V1), dict(fk)]
    return md


class TestPreflightR1(unittest.TestCase):
    # ---- 原 8 项语义 ----
    def test_01_fully_applied_zero_ddl(self):
        r = check_metadata(with_scope_fk(with_index(base_md())))
        self.assertEqual(r["status"], "ALREADY_APPLIED")
        self.assertEqual(r["ddl"], [])

    def test_02_both_steps_missing(self):
        r = check_metadata(base_md())
        self.assertEqual(r["status"], "READY_FOR_V2")
        self.assertEqual(r["ddl"], [STEP1_DDL, STEP2_DDL])

    def test_03_only_fk_missing(self):
        r = check_metadata(with_index(base_md()))
        self.assertEqual(r["status"], "READY_FOR_V2")
        self.assertEqual(r["ddl"], [STEP2_DDL])

    def test_04_only_index_missing(self):
        r = check_metadata(with_scope_fk(base_md()))
        self.assertEqual(r["status"], "READY_FOR_V2")
        self.assertEqual(r["ddl"], [STEP1_DDL])

    def test_05_index_same_name_wrong_order_blocked(self):
        ix = {"name": "uk_booking_master_id_store_booking", "unique": True,
              "columns": [{"name": "id", "prefix": None}, {"name": "booking_id", "prefix": None},
                          {"name": "store_id", "prefix": None}]}
        r = check_metadata(with_index(base_md(), ix))
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(r["ddl"], [])

    def test_06_type_collation_incompatible_blocked(self):
        md = base_md()
        md["tables"]["ipad_batch_request"]["columns"]["booking_master_id"]["unsigned"] = True
        self.assertEqual(check_metadata(md)["status"], "BLOCKED")
        md2 = base_md()
        md2["tables"]["ipad_batch_request"]["columns"]["booking_id"]["collation"] = "utf8mb4_general_ci"
        self.assertEqual(check_metadata(md2)["status"], "BLOCKED")

    def test_07_dirty_data_blocked(self):
        md = base_md()
        md["counts"]["orphan_receipts"] = 3
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(r["ddl"], [])

    def test_08_v1_table_missing_blocked(self):
        md = base_md()
        md["tables"]["ipad_batch_request"]["present"] = False
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertIn("先按 ipad_batch_request_migration_v1.sql 建表", r["reason"][0])

    # ---- Codex 5 个反例 ----
    def test_09_counts_missing_blocked(self):
        md = base_md()
        del md["counts"]
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertIn("counts", r["reason"][0])

    def test_10_orphan_null_blocked(self):
        md = base_md()
        md["counts"]["orphan_receipts"] = None
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(r["ddl"], [])

    def test_11_orphan_negative_blocked(self):
        md = base_md()
        md["counts"]["orphan_receipts"] = -1
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(r["ddl"], [])

    def test_12_both_parent_child_missing_type_blocked(self):
        md = base_md()
        del md["tables"]["booking_master"]["columns"]["id"]["type"]
        del md["tables"]["ipad_batch_request"]["columns"]["booking_master_id"]["type"]
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")
        self.assertIn("type", r["reason"][0])

    def test_13_unique_string_false_blocked(self):
        ix = {"name": "uk_booking_master_id_store_booking", "unique": "false",
              "columns": [{"name": "id", "prefix": None}, {"name": "store_id", "prefix": None},
                          {"name": "booking_id", "prefix": None}]}
        r = check_metadata(with_index(base_md(), ix))
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(r["ddl"], [])

    # ---- 关键缺失/畸形类型 ----
    def test_14_indexes_not_collected_blocked(self):
        md = base_md()
        del md["tables"]["booking_master"]["indexes"]
        self.assertEqual(check_metadata(md)["status"], "BLOCKED")

    def test_15_fkc_string_one_blocked(self):
        md = base_md()
        md["foreign_key_checks"] = "1"
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")

    def test_16_counts_string_zero_blocked(self):
        md = base_md()
        md["counts"]["orphan_receipts"] = "0"
        r = check_metadata(md)
        self.assertEqual(r["status"], "BLOCKED")

    def test_17_unsigned_missing_blocked(self):
        md = base_md()
        del md["tables"]["booking_master"]["columns"]["id"]["unsigned"]
        self.assertEqual(check_metadata(md)["status"], "BLOCKED")

    def test_18_prefix_index_blocks(self):
        ix = {"name": "uk_booking_master_id_store_booking", "unique": True,
              "columns": [{"name": "id", "prefix": None}, {"name": "store_id", "prefix": None},
                          {"name": "booking_id", "prefix": 10}]}
        r = check_metadata(with_index(base_md(), ix))
        self.assertEqual(r["status"], "BLOCKED")
        self.assertIn("前缀索引", r["reason"][0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
