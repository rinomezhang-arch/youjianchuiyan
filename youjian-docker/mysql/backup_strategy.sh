#!/bin/bash
# 又见炊烟餐饮管理系统 - 数据库自动备份脚本
# 建议通过 crontab 每日执行：0 2 * * * /path/to/backup_strategy.sh

BACKUP_DIR="/data/backups/mysql"
DB_NAME="banquet"
DB_USER="rino"
DB_PASS="Wo002323"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_backup_${DATE}.sql.gz"

mkdir -p ${BACKUP_DIR}

echo "[$(date)] 开始备份 ${DB_NAME}..."

# 使用 docker exec 执行 mysqldump
docker exec youjian-mysql-local mysqldump -u${DB_USER} -p${DB_PASS} \
  --single-transaction --routines --triggers --events \
  ${DB_NAME} | gzip > ${BACKUP_FILE}

if [ $? -eq 0 ]; then
    echo "[$(date)] 备份成功: ${BACKUP_FILE}"
    echo "[$(date)] 文件大小: $(du -h ${BACKUP_FILE} | cut -f1)"
else
    echo "[$(date)] 备份失败!"
    exit 1
fi

# 清理过期备份
find ${BACKUP_DIR} -name "${DB_NAME}_backup_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "[$(date)] 已清理 ${RETENTION_DAYS} 天前的备份"
echo "[$(date)] 当前备份文件数: $(ls -1 ${BACKUP_DIR}/${DB_NAME}_backup_*.sql.gz 2>/dev/null | wc -l)"
