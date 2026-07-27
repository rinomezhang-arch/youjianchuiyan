#!/bin/bash
set -e
echo "=== Installing mysql-connector-python with --break-system-packages ==="
sudo python3 -m pip install --quiet --break-system-packages mysql-connector-python 2>&1 | tail -5
echo "=== Verify ==="
sudo python3 -c "import mysql.connector; print('mysql.connector now OK')"
