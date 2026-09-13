# AT-SPI interface map

| Requested operation | AT-SPI interface | Readback |
|---|---|---|
| Enumerate apps/windows/controls | Accessible tree | role, state, child count |
| Fill editable text | `EditableText` | character count and SHA-256 |
| Press, activate, expand, or toggle | `Action` with exact action name | action return plus refreshed state |
| Select an item | parent `Selection` | selected child state |
| Set numeric value | `Value` | current/min/max |

The helper checks `DISPLAY` or `WAYLAND_DISPLAY` plus the session D-Bus address before importing the live accessibility registry. A server without a desktop session returns `NO_DESKTOP_SESSION`. This is a valid capability result, not a reason to install keyboard emulation.

Operation records live under `~/.agents/state/linux-atspi-app-control` and contain identifiers and hashes only. Submitted text is never persisted.
