#!/bin/bash
# TL71: SHA/JAR 哈希校验入口。用法: verify-jar-sha256.sh <文件> <期望sha256>
# 一致退出 0；不一致退出 1；参数/文件问题退出 2。
set -uo pipefail
F="${1:-}"; EXPECT="${2:-}"
if [ -z "$F" ] || [ -z "$EXPECT" ]; then echo "用法: verify-jar-sha256.sh <文件> <期望sha256>" >&2; exit 2; fi
if [ ! -f "$F" ]; then echo "[FAIL] 文件不存在: $F" >&2; exit 2; fi
ACTUAL="$(sha256sum "$F" | awk '{print $1}')"
if [ "$ACTUAL" = "$EXPECT" ]; then echo "[OK] sha256 一致: $ACTUAL"; exit 0; fi
echo "[FAIL] sha256 不一致 期望=$EXPECT 实际=$ACTUAL" >&2; exit 1
