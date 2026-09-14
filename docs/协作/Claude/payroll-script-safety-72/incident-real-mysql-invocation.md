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

## 能否证实未连接/未写入（按 Codex 纠正，两次分开表述，不外推）

**第二次（我的独立诊断命令）：连接结果已证实，止于认证失败。**
原始输出 `ERROR 1045 (28000): Access denied ... (using password: NO)` 是直接证据——
MySQL 协议层认证失败发生在任何 SQL 真正执行之前，该连接从未进入可执行查询的会话状态，
没有任何 SELECT/INSERT/DDL 被服务端执行。

**第一次（脚本内部那次）：连接结果未证实，不能套用第二次的结论。**
该行脚本自带 `2>/dev/null`，stderr 被直接丢弃，我手上只有"结果变量为空"这一个事实。
空结果可能来自认证失败，也可能来自其他原因（连接超时、协议错误等），
**仅凭空结果不足以证明它同样止于认证阶段**。此次连接结果标记为：**未证实**。

关于第一次的"零 DDL"，只能引用控制流、不能引用连接证据：
脚本第 35 行取到空值后，`if [ "$_SCHEMA_EXISTS" != "0" ]` 判定为真，立即打印
`[FAIL] ... 中止（零写入）` 并 `exit 1`，**后续 CREATE DATABASE / DDL / INSERT
等所有写入语句在代码路径上根本没有被执行到**。这是控制流层面的判断，
不依赖于那次连接究竟走到哪一步。

其余共同事实：
- 两次目标端口均为 `127.0.0.1:13318`（本项目既有隔离测试实例，非 3306/13317/生产）。
- 两次发出的语句均只是 `SELECT ... information_schema.SCHEMATA` 只读元数据查询。
- 未使用任何密码重试或绕过认证；未查看、未读取任何凭据；诊断后立即停止。
- `mysql` 客户端本身不产生本地写入，本机侧零写入。

## 后续

不再执行任何 mysql 验证，不以"未起服务"替代零写入证据（以上有明确认证失败记录为证，
不是靠"没启动服务"这种消极推断）。72 卡的 Windows 环境验证到此停止，按 Codex 指示
移交天龙在 Linux 侧（TL79）用离线桩继续；本人不再同时改动 `run-http-e2e.sh`，
等 TL79 报出明确缺陷后由 Codex 限定派修。
