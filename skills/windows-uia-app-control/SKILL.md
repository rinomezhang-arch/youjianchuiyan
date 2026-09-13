---
name: windows-uia-app-control
description: Operate native Windows applications without moving the mouse, stealing focus, simulating keystrokes, or using the clipboard. Use for enumerating app windows, inspecting UI Automation controls, filling a uniquely identified field, invoking a button, selecting an item, toggling a control, or reading back state through Microsoft UI Automation.
---

# Windows UIA App Control

Use `scripts/uia-app.ps1` for deterministic Microsoft UI Automation operations.

## Workflow

1. Run `list-windows` with `-ProcessName` when known.
2. Run `inspect` against one uniquely matched window. Narrow with `-ControlName`, `-ControlAutomationId`, or `-ControlType`.
3. Use `get-state` before mutation. Treat nonempty fields, disabled controls, hidden controls, and duplicate matches as blockers.
4. For mutation, use an exact control name or automation ID and a fresh `-OperationId`.
5. Prefer one semantic UIA pattern: `ValuePattern`, `InvokePattern`, `SelectionItemPattern`, `ExpandCollapsePattern`, `RangeValuePattern`, `TogglePattern`, or `ScrollItemPattern`.
6. Read back state after the operation. For message submission, pass a unique `-ReadbackMarker`; `accepted` without marker readback is uncertain delivery.

## Hard boundaries

- Never use cursor coordinates, `SendKeys`, synthetic keyboard input, the clipboard, or `SetForegroundWindow`.
- Do not use this skill for browser pages when a DOM or browser tool is available.
- Never inspect or log credential fields, private account content, or legal case content.
- Never overwrite a nonempty field unless the user explicitly authorized it and `-AllowReplace` is supplied.
- Mutation aborts when recent user input is detected, the target is busy, the window/control match is ambiguous, or the semantic pattern is unavailable.
- Never automatically retry an uncertain mutation. Inspect state and use a new operation ID only after deciding that a retry is safe.
- This skill does not change application configuration or install accessibility hooks.

## Examples

```powershell
# Find windows without exposing control text.
pwsh -File scripts/uia-app.ps1 -Action list-windows -ProcessName claude

# Inspect buttons and edit fields in one window.
pwsh -File scripts/uia-app.ps1 -Action inspect -ProcessName claude -WindowTitleRegex 'Claude' -ControlType Edit

# Preview a submission without writing.
pwsh -File scripts/uia-app.ps1 -Action set-value-and-invoke -ProcessName claude -WindowTitleRegex 'Claude' -ControlName 'Write your prompt to Claude' -InvokeControlName 'Send message' -Text 'test-marker' -ReadbackMarker 'test-marker' -OperationId 'dryrun-001' -DryRun
```

See `references/patterns.md` for action-to-pattern mapping and result semantics.
