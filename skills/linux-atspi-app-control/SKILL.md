---
name: linux-atspi-app-control
description: Operate native Linux desktop applications without mouse coordinates, focus stealing, simulated keystrokes, or clipboard use. Use for enumerating applications and windows, inspecting AT-SPI controls, setting editable text, invoking an exact accessibility action, selecting an item, or setting a numeric value through AT-SPI.
---

# Linux AT-SPI App Control

Use `scripts/atspi_app.py`. It talks to the desktop accessibility tree through PyGObject and AT-SPI.

## Workflow

1. Run `list-apps`, then `list-windows` for an exact application name.
2. Run `inspect` and narrow by exact `--control-name`, `--automation-id`, or `--role`.
3. Run `get-state` before mutation.
4. Mutations require one unique window, one unique control, and a fresh `--operation-id`.
5. Use `set-text`, `do-action`, `select`, or `set-value`; then read state back.

## Hard boundaries

- Never use `xdotool`, `ydotool`, XTest, synthetic key events, pointer movement, clipboard injection, or forced focus.
- Never inspect or log credentials, private account content, or legal case content.
- Do not overwrite nonempty editable text without explicit `--allow-replace`.
- Reject duplicate targets, disabled controls, unavailable semantic interfaces, and uncertain repeat operations.
- If no graphical session or accessibility bus is available, return `NO_DESKTOP_SESSION` or `ATSPI_UNAVAILABLE`. Do not fake success and do not fall back to mouse/keyboard tools.
- A headless agent should send a structured operation request to the Windows coordinator instead of trying to operate a Windows app remotely.

## Examples

```bash
python3 scripts/atspi_app.py list-apps
python3 scripts/atspi_app.py inspect --app-name 'Example' --window-name 'Example' --role push-button
python3 scripts/atspi_app.py set-text --app-name 'Example' --window-name 'Example' --control-name 'Message' --text 'test-marker' --operation-id dryrun-001 --dry-run
```

See `references/patterns.md` for interface mapping and headless behavior.
