# UIA pattern map

| Requested operation | Required UIA pattern | Readback |
|---|---|---|
| Read or fill a text field | `ValuePattern` | length and SHA-256 by default; plaintext only with explicit `-IncludeText` |
| Press a button | `InvokePattern` | enabled/offscreen state plus optional marker search |
| Select a list/tab item | `SelectionItemPattern` | `IsSelected` |
| Expand or collapse | `ExpandCollapsePattern` | `ExpandCollapseState` |
| Set a slider/spinner | `RangeValuePattern` | current/min/max |
| Set a checkbox/switch | `TogglePattern` | `ToggleState` |
| Bring a virtualized item into its container | `ScrollItemPattern` | pattern invocation result |

`list-windows`, `inspect`, and `get-state` are read-only. Every mutating action requires a unique `OperationId`. The helper stores only operation metadata and message hashes under `~/.agent-skills/windows-uia-app-control`; it never stores submitted text.

`set-value-and-invoke` resolves the invoke control after filling the field because some apps create or enable their Send button only when content exists. If the readback marker is missing, the result is `uncertain`; do not repeat automatically.
