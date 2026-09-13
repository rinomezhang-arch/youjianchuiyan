#!/usr/bin/env python3
"""Deterministic, semantic AT-SPI control for native Linux desktop apps."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import time
from typing import Any, Iterable


MUTATIONS = {"set-text", "do-action", "select", "set-value"}


class ControlError(RuntimeError):
    pass


def sha256(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def emit(**payload: Any) -> None:
    print(json.dumps(payload, ensure_ascii=False, separators=(",", ":")))


def require_desktop() -> None:
    if not (os.environ.get("DISPLAY") or os.environ.get("WAYLAND_DISPLAY")):
        raise ControlError("NO_DESKTOP_SESSION: DISPLAY and WAYLAND_DISPLAY are unset")
    if not os.environ.get("DBUS_SESSION_BUS_ADDRESS"):
        raise ControlError("NO_DESKTOP_SESSION: DBUS_SESSION_BUS_ADDRESS is unset")


def load_atspi():
    require_desktop()
    try:
        import gi  # type: ignore

        gi.require_version("Atspi", "2.0")
        from gi.repository import Atspi  # type: ignore
    except Exception as exc:
        raise ControlError(f"ATSPI_UNAVAILABLE: {type(exc).__name__}: {exc}") from exc
    try:
        desktop = Atspi.get_desktop(0)
        _ = desktop.get_child_count()
    except Exception as exc:
        raise ControlError(f"ATSPI_UNAVAILABLE: registry unreachable: {type(exc).__name__}: {exc}") from exc
    return Atspi, desktop


def children(node: Any) -> Iterable[Any]:
    for index in range(max(0, int(node.get_child_count()))):
        child = node.get_child_at_index(index)
        if child is not None:
            yield child


def walk(node: Any, max_nodes: int) -> Iterable[Any]:
    queue = list(children(node))
    seen = 0
    while queue and seen < max_nodes:
        item = queue.pop(0)
        seen += 1
        yield item
        queue.extend(children(item))


def name_of(node: Any) -> str:
    try:
        return str(node.get_name() or "")
    except Exception:
        return ""


def role_of(node: Any) -> str:
    try:
        return str(node.get_role_name() or "")
    except Exception:
        return ""


def attributes_of(node: Any) -> dict[str, str]:
    result: dict[str, str] = {}
    try:
        raw = node.get_attributes() or []
    except Exception:
        return result
    if isinstance(raw, dict):
        return {str(k): str(v) for k, v in raw.items()}
    for entry in raw:
        text = str(entry)
        if ":" in text:
            key, value = text.split(":", 1)
            result[key] = value
    return result


def automation_id_of(node: Any) -> str:
    attrs = attributes_of(node)
    for key in ("automation-id", "accessible-id", "id"):
        if attrs.get(key):
            return attrs[key]
    return ""


def state_contains(Atspi: Any, node: Any, state_name: str) -> bool:
    try:
        state = getattr(Atspi.StateType, state_name)
        return bool(node.get_state_set().contains(state))
    except Exception:
        return False


def interface(node: Any, getter: str) -> Any | None:
    try:
        return getattr(node, getter)()
    except Exception:
        return None


def text_snapshot(node: Any) -> tuple[int | None, str | None]:
    text_iface = interface(node, "get_text_iface")
    if text_iface is None:
        return None, None
    try:
        count = int(text_iface.get_character_count())
        value = str(text_iface.get_text(0, count) or "")
        return count, sha256(value)
    except Exception:
        return None, None


def action_names(node: Any) -> list[str]:
    action = interface(node, "get_action_iface")
    if action is None:
        return []
    names: list[str] = []
    try:
        for index in range(int(action.get_n_actions())):
            names.append(str(action.get_action_name(index) or ""))
    except Exception:
        return []
    return names


def node_state(Atspi: Any, node: Any) -> dict[str, Any]:
    length, digest = text_snapshot(node)
    result: dict[str, Any] = {
        "name": name_of(node),
        "automation_id": automation_id_of(node),
        "role": role_of(node),
        "enabled": state_contains(Atspi, node, "ENABLED"),
        "visible": state_contains(Atspi, node, "VISIBLE"),
        "showing": state_contains(Atspi, node, "SHOWING"),
        "focused": state_contains(Atspi, node, "FOCUSED"),
        "selected": state_contains(Atspi, node, "SELECTED"),
        "actions": action_names(node),
        "child_count": int(node.get_child_count()),
    }
    if length is not None:
        result["text_length"] = length
        result["text_sha256"] = digest
    value_iface = interface(node, "get_value_iface")
    if value_iface is not None:
        try:
            result["value"] = {
                "current": float(value_iface.get_current_value()),
                "minimum": float(value_iface.get_minimum_value()),
                "maximum": float(value_iface.get_maximum_value()),
            }
        except Exception:
            pass
    return result


def exact_apps(desktop: Any, app_name: str | None) -> list[Any]:
    apps = list(children(desktop))
    if app_name:
        apps = [app for app in apps if name_of(app) == app_name]
    return apps


def unique(items: list[Any], missing: str, ambiguous: str) -> Any:
    if not items:
        raise ControlError(missing)
    if len(items) != 1:
        raise ControlError(f"{ambiguous}: count={len(items)}")
    return items[0]


def resolve_app(desktop: Any, app_name: str) -> Any:
    return unique(exact_apps(desktop, app_name), "APP_NOT_FOUND", "APP_AMBIGUOUS")


def resolve_window(app: Any, window_name: str) -> Any:
    windows = [node for node in children(app) if name_of(node) == window_name]
    return unique(windows, "WINDOW_NOT_FOUND", "WINDOW_AMBIGUOUS")


def controls(window: Any, args: argparse.Namespace) -> list[Any]:
    matches: list[Any] = []
    for node in walk(window, args.max_nodes):
        if args.control_name and name_of(node) != args.control_name:
            continue
        if args.automation_id and automation_id_of(node) != args.automation_id:
            continue
        if args.role and role_of(node).casefold() != args.role.casefold():
            continue
        matches.append(node)
        if len(matches) >= args.max_results:
            break
    return matches


def resolve_control(window: Any, args: argparse.Namespace) -> Any:
    if not (args.control_name or args.automation_id):
        raise ControlError("CONTROL_SELECTOR_REQUIRED")
    return unique(controls(window, args), "CONTROL_NOT_FOUND", "CONTROL_AMBIGUOUS")


def claim_operation(args: argparse.Namespace, window: Any) -> Path | None:
    if args.dry_run:
        return None
    if not args.operation_id:
        raise ControlError("OPERATION_ID_REQUIRED")
    root = Path.home() / ".agents" / "state" / "linux-atspi-app-control" / "claims"
    root.mkdir(parents=True, exist_ok=True)
    path = root / f"{sha256(args.operation_id)}.json"
    record = {
        "operation_id": args.operation_id,
        "status": "started",
        "time": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "action": args.command,
        "app": args.app_name,
        "window_name_sha256": sha256(name_of(window)),
        "text_sha256": sha256(args.text) if args.text is not None else None,
    }
    try:
        fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as exc:
        raise ControlError("DUPLICATE_OPERATION_ID") from exc
    with os.fdopen(fd, "w", encoding="utf-8") as handle:
        json.dump(record, handle, ensure_ascii=False, separators=(",", ":"))
    return path


def complete_claim(path: Path | None, status: str, **extra: Any) -> None:
    if path is None:
        return
    record = json.loads(path.read_text(encoding="utf-8"))
    record.update(extra)
    record["status"] = status
    record["completed_at"] = time.strftime("%Y-%m-%dT%H:%M:%S%z")
    path.write_text(json.dumps(record, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("command", choices=["list-apps", "list-windows", "inspect", "get-state", *sorted(MUTATIONS)])
    result.add_argument("--app-name")
    result.add_argument("--window-name")
    result.add_argument("--control-name")
    result.add_argument("--automation-id")
    result.add_argument("--role")
    result.add_argument("--text")
    result.add_argument("--action-name")
    result.add_argument("--number", type=float)
    result.add_argument("--operation-id")
    result.add_argument("--allow-replace", action="store_true")
    result.add_argument("--dry-run", action="store_true")
    result.add_argument("--max-nodes", type=int, default=5000)
    result.add_argument("--max-results", type=int, default=200)
    return result


def main() -> int:
    args = parser().parse_args()
    try:
        Atspi, desktop = load_atspi()
        if args.command == "list-apps":
            apps = [{"name": name_of(app), "child_count": int(app.get_child_count())} for app in exact_apps(desktop, args.app_name)]
            emit(status="ok", count=len(apps), apps=apps)
            return 0

        if not args.app_name:
            raise ControlError("APP_NAME_REQUIRED")
        app = resolve_app(desktop, args.app_name)
        if args.command == "list-windows":
            windows = [{"name": name_of(node), "role": role_of(node), "child_count": int(node.get_child_count())} for node in children(app)]
            emit(status="ok", count=len(windows), windows=windows)
            return 0

        if not args.window_name:
            raise ControlError("WINDOW_NAME_REQUIRED")
        window = resolve_window(app, args.window_name)
        if args.command == "inspect":
            items = [node_state(Atspi, node) for node in controls(window, args)]
            emit(status="ok", count=len(items), controls=items)
            return 0

        control = resolve_control(window, args)
        if args.command == "get-state":
            emit(status="ok", state=node_state(Atspi, control))
            return 0
        if not args.operation_id:
            raise ControlError("OPERATION_ID_REQUIRED")
        if not state_contains(Atspi, control, "ENABLED"):
            raise ControlError("CONTROL_DISABLED")
        if args.dry_run:
            emit(status="dry_run", action=args.command, operation_id=args.operation_id, target=node_state(Atspi, control))
            return 0

        claim = claim_operation(args, window)
        try:
            if args.command == "set-text":
                if args.text is None:
                    raise ControlError("TEXT_REQUIRED")
                editable = interface(control, "get_editable_text_iface")
                if editable is None:
                    raise ControlError("EDITABLE_TEXT_INTERFACE_UNAVAILABLE")
                count, _ = text_snapshot(control)
                if count and not args.allow_replace:
                    raise ControlError("NONEMPTY_TEXT_REQUIRES_ALLOW_REPLACE")
                if not bool(editable.set_text_contents(args.text)):
                    raise ControlError("SET_TEXT_REJECTED")
            elif args.command == "do-action":
                if not args.action_name:
                    raise ControlError("ACTION_NAME_REQUIRED")
                action = interface(control, "get_action_iface")
                if action is None:
                    raise ControlError("ACTION_INTERFACE_UNAVAILABLE")
                indexes = [i for i in range(int(action.get_n_actions())) if str(action.get_action_name(i) or "") == args.action_name]
                index = unique(indexes, "ACTION_NOT_FOUND", "ACTION_AMBIGUOUS")
                if not bool(action.do_action(index)):
                    raise ControlError("ACTION_REJECTED")
            elif args.command == "select":
                parent = control.get_parent()
                selection = interface(parent, "get_selection_iface") if parent is not None else None
                if selection is None:
                    raise ControlError("SELECTION_INTERFACE_UNAVAILABLE")
                index = next((i for i, child in enumerate(children(parent)) if child == control), None)
                if index is None or not bool(selection.select_child(index)):
                    raise ControlError("SELECTION_REJECTED")
            elif args.command == "set-value":
                if args.number is None:
                    raise ControlError("NUMBER_REQUIRED")
                value = interface(control, "get_value_iface")
                if value is None:
                    raise ControlError("VALUE_INTERFACE_UNAVAILABLE")
                minimum = float(value.get_minimum_value())
                maximum = float(value.get_maximum_value())
                if args.number < minimum or args.number > maximum:
                    raise ControlError("VALUE_OUT_OF_BOUNDS")
                if not bool(value.set_current_value(args.number)):
                    raise ControlError("SET_VALUE_REJECTED")
            state = node_state(Atspi, control)
            complete_claim(claim, "applied", state=state)
            emit(status="applied", operation_id=args.operation_id, state=state)
            return 0
        except Exception as exc:
            complete_claim(claim, "failed", error=str(exc))
            raise
    except ControlError as exc:
        emit(status="error", error=str(exc), action=args.command)
        return 2
    except Exception as exc:
        emit(status="error", error=f"UNEXPECTED: {type(exc).__name__}: {exc}", action=args.command)
        return 3


if __name__ == "__main__":
    sys.exit(main())
