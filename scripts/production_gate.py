#!/usr/bin/env python3
"""Production release gate for schema, tenant isolation and migration safety."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "banquet_project/src/main/java"
MIGRATIONS = ROOT / "scripts/migrations"
FRONTEND = ROOT / "frontend_v3"
BACKEND_RESOURCES = ROOT / "banquet_project/src/main/resources"

REQUIRED_MIGRATIONS = {
    "banquet_notice_migration_v1.sql",
    "ipad_device_binding_migration_v1.sql",
    "ipad_checkout_migration_v1.sql",
}
REQUIRED_TABLES = {
    "banquet_notice",
    "ipad_device_binding",
    "ipad_payment_request",
}
CRITICAL_TENANT_CONTROLLERS = {
    "BookingController.java": ("UserContext.getCurrentStoreId",),
    "FinanceController.java": ("resolveQueryStoreId(",),
    "IpadOrderController.java": ('getAttribute("ipad_store_id")',),
    "ReportController.java": ("resolveStoreId(",),
    "BanquetNoticeService.java": ("private Long resolveStore(", "UserContext.getStoreId"),
}


def fail(message: str, errors: list[str]) -> None:
    errors.append(message)


def main() -> int:
    errors: list[str] = []
    migration_files = {path.name for path in MIGRATIONS.glob("*.sql")}
    missing = REQUIRED_MIGRATIONS - migration_files
    if missing:
        fail(f"missing required migrations: {sorted(missing)}", errors)

    migration_sql = "\n".join(path.read_text(encoding="utf-8") for path in MIGRATIONS.glob("*.sql"))
    for table in REQUIRED_TABLES:
        if not re.search(rf"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?{re.escape(table)}`?", migration_sql, re.I):
            fail(f"required table has no CREATE migration: {table}", errors)

    for path in JAVA.rglob("*Controller.java"):
        text = path.read_text(encoding="utf-8")
        if "/api/ipad" in text and "ipad_store_id" not in text and "Ipad" in path.name:
            fail(f"iPad controller does not consume verified store scope: {path.relative_to(ROOT)}", errors)

    production_env = FRONTEND / ".env.production"
    if not production_env.exists() or "VITE_FALLBACK_MODE=prod" not in production_env.read_text(encoding="utf-8"):
        fail("frontend production fallback must be disabled", errors)

    production_yaml = BACKEND_RESOURCES / "application-prod.yml"
    production_config = production_yaml.read_text(encoding="utf-8") if production_yaml.exists() else ""
    if not re.search(r"fallback:\s*\n\s+mode:\s*prod\b", production_config):
        fail("backend production fallback.mode must be prod", errors)

    forbidden_demo_markers = ("演示模式", "MOCK_")
    for path in (FRONTEND / "src").rglob("*"):
        if path.suffix not in {".vue", ".js", ".ts"}:
            continue
        text = path.read_text(encoding="utf-8")
        for marker in forbidden_demo_markers:
            if marker in text:
                fail(f"user-visible or unconditional demo marker remains: {path.relative_to(ROOT)} ({marker})", errors)

    java_components = {path.name: path for path in JAVA.rglob("*.java")}
    for name, markers in CRITICAL_TENANT_CONTROLLERS.items():
        path = java_components.get(name)
        if path is None:
            fail(f"critical controller missing: {name}", errors)
            continue
        text = path.read_text(encoding="utf-8")
        if not any(marker in text for marker in markers):
            fail(f"critical controller lacks verified store scope: {path.relative_to(ROOT)}", errors)

    if errors:
        print("PRODUCTION GATE: FAILED")
        for error in errors:
            print(f" - {error}")
        return 1

    print(f"PRODUCTION GATE: PASSED ({len(migration_files)} migrations scanned)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
