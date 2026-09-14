# CL-AUTH-BLANK-PASSWORD-GUARD-70 reported

最小补丁：AuthController.java:66、IpadAuthController.java:37 各加
`|| password.isBlank()`，堵住空白密码登录口子；不trim有效密码，
BCrypt/历史明文两条路径保留（用带首尾空格密码验证）。

专属测试 AuthBlankPasswordGuardTest.java：纯MockMvc+Mockito合成夹具，
不连库不读真实账号。首次跑5项2败（我的Mockito varargs匹配写法有误，
不是补丁问题），定向重跑1次后 **5/5全过，0失败**。

未改用户名格式校验、未连库建库、未动权限/拦截器/配置/法务。
分支 codex/claude-auth-blank-password-70，工作树独立，未合并未部署。

## 补记：远端推送核对（不改代码不重跑测试）

- 分支已推送：`codex/claude-auth-blank-password-70`，本地/远端 HEAD 一致：
  `6c099e020f312f4a9762f81f0e47565fef78a846`（`git ls-remote origin` 实核）。
- 原 5 项测试证据：`AuthBlankPasswordGuardTest.java`（同分支该次提交内），
  surefire 报告 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
  （首跑 5 跑 2 败为本人 Mockito varargs 匹配写法问题，定向重跑 1 次后见此结果，
  用满本卡允许的 1 次重跑额度，未再重跑）。
- 额度状态：unknown，未查 key、未做付费探测。
