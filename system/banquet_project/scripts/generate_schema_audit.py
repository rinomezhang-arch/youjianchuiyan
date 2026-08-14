#!/usr/bin/env python3
"""Generate evidence-backed table-to-code mapping SQL from the live schema and source tree."""
from __future__ import annotations

import argparse
import csv
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/com/youjian/banquet"
FRONTEND_ROOT = ROOT.parent / "frontend_v3/src"


def mysql_rows(database: str, query: str) -> list[list[str]]:
    command = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-N", "-B", database, "-e", query]
    result = subprocess.run(command, check=True, capture_output=True, text=True)
    return [line.split("\t") for line in result.stdout.splitlines() if line]


def read_sources(root: Path, suffixes: tuple[str, ...]) -> dict[Path, str]:
    return {path: path.read_text(encoding="utf-8", errors="ignore") for path in root.rglob("*") if path.suffix in suffixes}


def sql_text(value: str | None) -> str:
    if not value:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def java_table_mappings(java_sources: dict[Path, str]) -> dict[str, dict[str, set[str]]]:
    mappings: dict[str, dict[str, set[str]]] = {}
    entity_tables: dict[str, str] = {}
    for path, text in java_sources.items():
        if "/entity/" not in path.as_posix():
            continue
        table = re.search(r'@Table\s*\(\s*name\s*=\s*"([a-zA-Z0-9_]+)"', text)
        clazz = re.search(r'public\s+class\s+(\w+)', text)
        if table and clazz:
            entity_tables[clazz.group(1)] = table.group(1)
            mappings.setdefault(table.group(1), {"entity": set(), "repository": set(), "controller": set()})["entity"].add(clazz.group(1))

    for path, text in java_sources.items():
        kind = "repository" if "/repository/" in path.as_posix() else "controller" if "/controller/" in path.as_posix() else None
        if not kind:
            continue
        for clazz, table in entity_tables.items():
            if re.search(rf'\b{re.escape(clazz)}(?:Repository)?\b', text):
                mappings.setdefault(table, {"entity": set(), "repository": set(), "controller": set()})[kind].add(path.stem)
        for keyword in ("FROM", "JOIN", "UPDATE", "INTO"):
            for table in re.findall(rf'(?i)\b{keyword}\s+`?([a-z][a-z0-9_]+)`?', text):
                if table in mappings or "_" in table:
                    mappings.setdefault(table, {"entity": set(), "repository": set(), "controller": set()})[kind].add(path.stem)
    return mappings


def controller_routes(java_sources: dict[Path, str]) -> dict[str, set[str]]:
    routes: dict[str, set[str]] = {}
    for path, text in java_sources.items():
        if "/controller/" not in path.as_posix():
            continue
        base_match = re.search(r'@RequestMapping\s*\(\s*"([^"]+)"', text)
        base = base_match.group(1) if base_match else ""
        values = set()
        for route in re.findall(r'@(?:Get|Post|Put|Delete|Patch)Mapping\s*\(\s*"([^"]*)"', text):
            values.add((base.rstrip("/") + "/" + route.lstrip("/")).replace("//", "/"))
        if values:
            routes[path.stem] = values
    return routes


def frontend_bindings(frontend_sources: dict[Path, str], routes: set[str]) -> set[str]:
    files: set[str] = set()
    normalized = {re.sub(r'\{[^}]+}', '', route).rstrip("/") for route in routes}
    for path, text in frontend_sources.items():
        for route in normalized:
            prefix = route.split("/")[1:3]
            if route and (route in text or (prefix and "/" + "/".join(prefix) in text)):
                files.add(path.relative_to(FRONTEND_ROOT).as_posix())
                break
    return files


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", default="youjian_banquet_v2")
    parser.add_argument("--sql-output", required=True, type=Path)
    parser.add_argument("--csv-output", required=True, type=Path)
    args = parser.parse_args()

    tables = mysql_rows(args.database, "SELECT TABLE_NAME,COALESCE(TABLE_ROWS,0) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME")
    java_sources = read_sources(JAVA_ROOT, (".java",))
    frontend_sources = read_sources(FRONTEND_ROOT, (".js", ".vue"))
    mappings = java_table_mappings(java_sources)
    routes_by_controller = controller_routes(java_sources)

    rows = []
    for table, row_count in tables:
        mapping = mappings.get(table, {"entity": set(), "repository": set(), "controller": set()})
        controllers = mapping["controller"]
        routes = set().union(*(routes_by_controller.get(name, set()) for name in controllers)) if controllers else set()
        frontend = frontend_bindings(frontend_sources, routes)
        entity = ",".join(sorted(mapping["entity"]))
        repository = ",".join(sorted(mapping["repository"]))
        controller = ",".join(sorted(controllers))
        api_routes = ",".join(sorted(routes))
        frontend_files = ",".join(sorted(frontend))
        backend_status = "MAPPED" if entity and repository and controller else "PARTIAL" if entity or repository or controller else "UNMAPPED"
        frontend_status = "MAPPED" if frontend else "NO_DIRECT_UI"
        rows.append([table, row_count, entity, repository, controller, api_routes, frontend_files, backend_status, frontend_status])

    args.csv_output.parent.mkdir(parents=True, exist_ok=True)
    with args.csv_output.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(["table_name", "row_count", "entity_class", "repository_class", "controller_class", "api_routes", "frontend_files", "backend_status", "frontend_status"])
        writer.writerows(rows)

    statements = [
        "-- Generated by scripts/generate_schema_audit.py; do not hand-edit mapping rows.",
        "ALTER TABLE system_table_registry",
        "  ADD COLUMN IF NOT EXISTS entity_class varchar(255) NULL AFTER frontend_binding,",
        "  ADD COLUMN IF NOT EXISTS repository_class varchar(255) NULL AFTER entity_class,",
        "  ADD COLUMN IF NOT EXISTS controller_class varchar(500) NULL AFTER repository_class,",
        "  ADD COLUMN IF NOT EXISTS api_routes text NULL AFTER controller_class,",
        "  ADD COLUMN IF NOT EXISTS frontend_files text NULL AFTER api_routes,",
        "  ADD COLUMN IF NOT EXISTS mapping_status varchar(30) NOT NULL DEFAULT 'UNMAPPED' AFTER frontend_files;",
        "",
    ]
    for table, _, entity, repository, controller, api_routes, frontend_files, backend_status, frontend_status in rows:
        statements.append(
            "UPDATE system_table_registry SET "
            f"entity_class={sql_text(entity)},repository_class={sql_text(repository)},controller_class={sql_text(controller)},"
            f"api_routes={sql_text(api_routes)},frontend_files={sql_text(frontend_files)},"
            f"mapping_status={sql_text(backend_status)},backend_binding={sql_text(backend_status)},frontend_binding={sql_text(frontend_status)},reviewed_at=CURRENT_TIMESTAMP "
            f"WHERE table_name={sql_text(table)};"
        )
    args.sql_output.parent.mkdir(parents=True, exist_ok=True)
    args.sql_output.write_text("\n".join(statements) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
