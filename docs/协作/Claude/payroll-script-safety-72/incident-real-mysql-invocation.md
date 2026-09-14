# CL72 事故补记：PATH 配置错误导致两次误调用真实系统 mysql

## 时间

- 第一次：约 2026-09-15 00:5x（东八区），run-http-e2e.sh 第一次尝试运行时。
- 第二次：紧接第一次之后，独立诊断命令。
- 第三次：env-precheck.sh 门槛失败（约 00:58:25，文件时间戳可核）——此次未到 mysql 调用点。

## 根因

PATH 变量拼接时用了 `C:/Users/...` Windows 盘符风格，bash 把 `C:` 里的冒号当成了
PATH 分隔符解析，导致我搭的 stub 目录整个失效，真实系统 mysql
（`/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql`）被 PATH 上更靠后的条目命中。

## 完整命令（已脱敏——无密码明文，原命令本就不含密码）

第一次（脚本内部，`run-http-e2e.sh` 第 35 行原样）：
```
mysql -uroot --default-character-set=utf8mb4 --protocol=tcp -h127.0.0.1 -P13318 -N \
  -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$SCHEMA';" 2>/dev/null
```
该行脚本自带 `2>/dev/null`，未打印错误文本；结果变量为空（非 "0"），脚本自身的
`if [ "$_SCHEMA_EXISTS" != "0" ]` 判定触发，原样打印：
`[FAIL] schema tlpay59_http_1789405060062 已存在，时间戳冲突，中止（零写入）`
退出码 1，**脚本自身在这一行之后没有执行任何 CREATE/INSERT/DDL，立即中止**。

第二次（我本人独立诊断命令，非脚本内部）：
```
mysql -uroot --default-character-set=utf8mb4 --protocol=tcp -h127.0.0.1 -P13318 -N \
  -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='x';"
```
原始输出：
```
ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: NO)
```
退出码 1。

## 能否证实未连接/未写入

**能证实未写入；连接层面止于认证失败。**

- 两次调用的目标端口 `127.0.0.1:13318` 是本项目既有的隔离测试实例（非 3306/13317/生产），
  第一次沿用了脚本本身已有的边界注释（"只连 127.0.0.1:13318"）。
- 两次均只执行了 `SELECT ... information_schema.SCHEMATA`（只读元数据查询），
  且客户端认证阶段即被拒绝（`Access denied ... using password: NO`）——
  MySQL 协议层面认证失败发生在任何 SQL 语句真正执行之前，**该连接从未进入可执行
  查询的会话状态**，没有、也不可能有任何 SELECT/INSERT/DDL 被服务端执行。
- 未使用任何密码尝试重试或绕过认证；未查看、未读取任何凭据；诊断后立即停止，
  未进一步探测该实例。
- `mysql` 客户端本身不做任何本地写入（无本地缓存/日志文件），本机侧同样零写入。

## 后续

不再执行任何 mysql 验证，不以"未起服务"替代零写入证据（以上有明确认证失败记录为证，
不是靠"没启动服务"这种消极推断）。72 卡的 Windows 环境验证到此停止，按 Codex 指示
移交天龙在 Linux 侧（TL79）用离线桩继续；本人不再同时改动 `run-http-e2e.sh`，
等 TL79 报出明确缺陷后由 Codex 限定派修。
