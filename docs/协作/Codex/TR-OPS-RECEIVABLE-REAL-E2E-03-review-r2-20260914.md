# TR-OPS-RECEIVABLE-REAL-E2E-03 R2 技术验收

结论：CHANGES_REQUESTED。

业务链证据仍沿用上一轮已认可的 API 17/17、浏览器 6/6，本轮不要求重跑 E2E。提交 `f5601cf0af0f1c76af12d07f9992a49fce55ce8e` 已删除脚本内默认口令并把文档中的口令改为 `[REDACTED]`，但运行时脱敏没有真正接入输出路径：

- `scripts/trae-receivable-real-e2e/api-probe-03.mjs:14-23` 定义了 `redactSecrets`，之后没有调用它。
- 同文件 `:37-49` 仍把原始请求体和原始响应保存进 `log`，并直接打印原始 `body` 与 `json`；登录请求会包含口令，登录响应会包含 JWT。
- 同文件 `:191` 又把上述原始 `log` 整体写入 `EVIDENCE_JSON`。因此以后复跑脚本时，口令和 JWT 仍会进入控制台与证据日志，和报告所称“统一打码”不符。

只做一个窄修：真实 HTTP 请求继续使用原始值；进入 `log`、`REQ`、`RESP` 和 `EVIDENCE_JSON` 前分别生成脱敏副本并只输出副本。补一个不发网络请求的定向断言，证明嵌套 password/token/authorization/jwt/secret 全部变成 `[REDACTED]`，普通业务字段保持原值。只跑语法检查和该定向断言，不重跑 E2E，不改产品代码，也不要改业务单号中偶然相同的数字。

