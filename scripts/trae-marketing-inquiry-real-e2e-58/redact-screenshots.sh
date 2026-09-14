#!/bin/bash
# Redact phone numbers in TR58 screenshots (R3 precise coordinates).
# Only the phone-number glyphs are masked; business content stays visible.
# Coordinates verified by pixel scan + manual review of every image (777x1228):
#   01 store phone  x=232..358  y=898..927
#   02-05 input phone x=168..308 y=278..300
# Input: originals in $SRC_DIR (default /tmp/shots58). Output overwrites the
# files under the task screenshots dir. Never commit unredacted originals.
set -euo pipefail
SRC_DIR="${SRC_DIR:-/tmp/shots58}"
DST_DIR="${DST_DIR:-$(cd "$(dirname "$0")/../../docs/协作/Trae/marketing-inquiry-real-e2e-58/screenshots" && pwd)}"

redact() {
  local f="$1" x1="$2" y1="$3" x2="$4" y2="$5"
  [ -f "$SRC_DIR/$f" ] || { echo "  SKIP $f (not found)"; return 0; }
  convert "$SRC_DIR/$f" -fill black -draw "rectangle ${x1},${y1} ${x2},${y2}" "$DST_DIR/$f"
  echo "  redacted $f -> $DST_DIR/$f"
}

redact 01-submit-success.jpg        232 898 358 927
redact 02-lookup-success.jpg        168 278 308 300
redact 03-wrong-phone-empty.jpg     168 278 308 300
redact 04-invalid-inquiry-empty.jpg 168 278 308 300
redact 05-system-error.jpg          168 278 308 300
echo "ALL_REDACTED"
