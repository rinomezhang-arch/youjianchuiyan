# PROD-BACKUP-SAFE-14 实现报告

## 结果

- 新增 `scripts/prod_daily_backup.sh`：从本机环境文件读取凭据并静默导出；生成 gzip 后检查压缩完整性与 `Dump completed`；复制后校验 SHA256；损坏、失败及过期文件全部移动到按运行时间分层的 `.trash`，没有物理删除。
- 新增 `scripts/verify_backup_restore.sh`：只允许 `127.0.0.1`/`localhost`，明确拒绝 3306，只允许 `restore_verify_` 前缀的新库；拒绝覆盖已存在 schema；恢复后核对实际表数与转储中的 `CREATE TABLE` 数量。验证库保留供复核。
- 新增 `scripts/test_prod_backup_safe.sh`：全部使用临时目录和假 `mysqldump/mysql/flock`，不连接任何数据库。

## 测试

- `bash -n`：3 个脚本语法通过。
- 合成测试：15 项通过，0 失败。
- 覆盖：成功备份非空、本地与复制文件一致、缺结束标记退出 21 并归档、转储失败退出 20 并归档、40 天旧文件移入时间戳垃圾桶、模拟恢复成功、表数不符退出 37、3306 拒绝、非回环地址拒绝、挂载点缺失退出 22 且保留本地备份、生产与恢复脚本不存在 `rm`/`unlink`/`find -delete`。
- `git diff --check`：通过。

## 过程中出现的失败

- 首次尝试使用 Windows `bash.exe`，实际落到没有 `/bin/bash` 的 WSL 入口，未执行测试。改用已安装的 Git Bash 后执行成功。
- Git Bash 没有 `flock`，首轮测试在锁步骤退出 13。生产脚本保留 Linux `flock`，增加 `FLOCK_BIN` 注入点，测试使用只返回成功的假锁程序；随后全部通过。

## 生产执行前仍需的输入与批准

1. 秋哥批准安装脚本、写 crontab 和使用生产数据库执行一次备份；本提交没有做这些操作。
2. 确认服务器环境文件位置及其中 `MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE` 已配置。只确认变量存在，不输出值。
3. 确认本地备份目录、异机/COS备份目录、保留天数和每日执行时间；目标父目录必须已挂载可用。
4. 确认生产服务器具备 `mysqldump`、`gzip`、`sha256sum`、`flock`。
5. 为恢复演练提供独立回环 MySQL 的非 3306 端口和新的 `restore_verify_...` schema 名；恢复脚本不会清理该库。
6. 生产执行后还须保存：新备份时间、大小、SHA256一致性、表数、cron下一次运行时间及隔离恢复结果；不得记录凭据或业务数据行。

## 建议任务允许路径

- `scripts/prod_daily_backup.sh`
- `scripts/verify_backup_restore.sh`
- `scripts/test_prod_backup_safe.sh`
- `docs/协作/Codex/prod-backup-safe-14/**`

本轮未连接或修改生产，未安装 cron，未改配置、服务、数据库或法务内容，未删除文件。
