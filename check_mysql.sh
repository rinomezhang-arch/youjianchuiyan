#!/bin/bash
set -e
echo "=== Checking Python MySQL modules ==="
sudo python3 -c "import mysql.connector; print('mysql.connector OK')" 2>&1 || echo "mysql.connector NOT available"
sudo python3 -c "import pymysql; print('pymysql OK')" 2>&1 || echo "pymysql NOT available"
echo "=== Checking mysql CLI ==="
which mysql
echo "=== Trying to install mysql-connector-python ==="
sudo python3 -m pip install --quiet mysql-connector-python 2>&1 | tail -5 || echo "pip install failed"
echo "=== Verify ==="
sudo python3 -c "import mysql.connector; print('mysql.connector now OK')" 2>&1 || echo "still NOT available"
