# CO-AUTH-CONTEXT-RECOVERY-26 reported

## 状态与基线

- 执行人/工具：Codex
- 记录时间：2026-09-13 16:05 +08:00
- 任务工作树：`F:\solo\artifacts\team-worktrees\codex-auth-context-recovery`
- 任务基线：`4aecfa4413789a7023eb679779556d0b95022fb9`
- 代码修复祖先基线：`e945316854ec74f9eeffcbe6c1c4de68fd9327cb`；已用 `git merge-base --is-ancestor e9453168 HEAD` 确认退出码 0。
- 当前状态：本任务修复与新增反例完成；本文件所在提交的 SHA 以提交后 `git rev-parse HEAD` 回读为准。

## 实际改动

1. `AuditLogAspect.java`
   - 删除 iPad 身份从客户端 `X-Store-Id`、`X-Staff-Id` 头回退构造的路径。
   - 只消费 `IpadInterceptor` 写入的 `ipad_store_id`、`ipad_staff_id`、`ipad_device_sn` request 属性。
   - 有已验证员工时审计 `user_id` 使用员工号；设备专用路由没有已验证员工时使用 `ipad-device:<已验证设备号>`；其他空身份保持 `anonymous`。
   - iPad 写路径缺少完整已验证属性时拒绝执行，避免以客户端自报身份或不明身份继续写入。
2. `UserContext.java`
   - `fromVerifiedAttributes` 现在要求 subject 非 null、非空、非纯空白，并保存 trim 后的 subject；与“四项缺一不可”注释统一。
3. `AuthAuditPrivacyTest.java`
   - 新增 null、空串、纯空白 subject 三个反例及正常 subject 对照。
4. `IpadAuditIdentityHttpMysqlTest.java`
   - 新增真实随机监听端口、真实 `IpadInterceptor`、真实 `AuditLogAspect`、真实 iPad POST 控制器链和隔离 MySQL 回读。
   - 设备绑定无员工时发送伪造 `X-Staff-Id=900001`，断言业务写入口执行且 `audit_logs.user_id` 为已验证设备身份，绝不采用伪造员工号。
5. 本目录 `started.md`、`reported.md`
   - 记录动手前意图、验证方法、实际结果与遗留项。因任务明确限定 allowed paths，本轮未改中央登记簿。

本任务没有修改 `4aecfa44` 父提交中已有的 `StoreDataScopeAspect.java`、Payable/Receivable/RestaurantPrint 测试，也未触碰法务路径。`IpadBatchAuthorizationTest.java` 最终与任务基线无内容差异。

## 最终测试证据

隔离数据库监听：`127.0.0.1:13317`。测试过程中仅使用并保留合成 schema，不连接生产、不删除 schema。

### 本任务可执行矩阵

命令：

```powershell
$env:YOUJIAN_TEST_MYSQL='1'
mvn -B -o -f banquet_project/pom.xml "-Dtest=AuthAuditPrivacyTest,AuthContextScopeHttpMysqlTest,AuthRealtimeHttpMysqlTest,IpadAuditIdentityHttpMysqlTest" test
```

结果：64 通过、0 失败、0 错误、0 跳过，BUILD SUCCESS，结束时间 2026-09-13 16:02:25 +08:00。

| 测试类 | 通过 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| AuthAuditPrivacyTest | 3 | 0 | 0 | 0 |
| AuthContextScopeHttpMysqlTest | 8 | 0 | 0 | 0 |
| AuthRealtimeHttpMysqlTest | 52 | 0 | 0 | 0 |
| IpadAuditIdentityHttpMysqlTest | 1 | 0 | 0 | 0 |

真实 iPad 审计证据：Tomcat 随机端口 `63966`；独占 schema `ipad_audit_0b931152f8b04825be6080c4addc7be6`；数据库回读 `audit_user=ipad-device:SYN-AUDIT-DEVICE`，`forged_user_rejected=true`。AuthContext 与 AuthRealtime 最终保留 schema 分别为 `auth_ctx_77419cd572234f719786e480f0ff1f8e`、`auth_rt_a851be723a5144cba687efcb84f9972e`。

逐类原始摘要位于工作树 `banquet_project/target/surefire-reports/` 对应 `.txt` 和 `.xml` 文件。

### 按评审要求复跑的旧 iPad 套件

命令：

```powershell
$env:YOUJIAN_TEST_MYSQL='1'
mvn -B -o -f banquet_project/pom.xml "-Dtest=IpadAuthChainIsolationTest,IpadBatchAuthorizationTest" test
```

结果：0 通过、0 失败、18 错误、0 跳过，BUILD FAILURE，结束时间 2026-09-13 16:04:09 +08:00。

| 测试类 | 通过 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| IpadAuthChainIsolationTest | 0 | 0 | 6 | 0 |
| IpadBatchAuthorizationTest | 0 | 0 | 12 | 0 |

18 项均在 `@BeforeEach` 注入阶段报同一错误：测试尝试给 `IpadOrderController` 注入 `batchAuthorization`，但任务基线 `4aecfa44` 的该生产类没有这个字段。错误发生在 HTTP 请求及本次审计切面执行之前。本轮没有通过修改测试断言来掩盖基线漂移，也没有越过 allowed paths 带入后续 iPad 控制器/服务实现。

## 过程失败记录

- 15:45:56：首轮 24 项，3 通过、0 失败、21 错误、0 跳过；本机隔离 MySQL 尚未完成启动，21 项均为连接拒绝。随后以隐藏后台方式启动既有本机测试服务，未改配置。
- 15:52:45：探索矩阵 82 项，63 通过、0 失败、19 错误、0 跳过；19 项均为旧 iPad 夹具的 `batchAuthorization` 字段漂移。该轮临时把新反例放在旧套件中，随后移至独立真实端口测试。
- 15:58:56：AuthAudit 与新真实端口测试共 4 项，3 通过、0 失败、1 错误、0 跳过；测试上下文缺合成通知仓储 bean，补齐测试夹具后复跑。
- 16:00:20：新真实端口 iPad 审计反例单独复跑 1 通过、0 失败、0 错误、0 跳过。
- 16:02:25 与 16:04:09：以上“最终测试证据”两组结果。

## 遗留与接续

1. P1：`IpadAuthChainIsolationTest` 和 `IpadBatchAuthorizationTest` 与 `4aecfa44` 的生产 iPad 控制器不在同一实现代际，当前无法执行到断言。修复需要把后续 iPad 授权服务/控制器提交纳入统一基线，涉及本任务禁止修改的生产路径，应由统筹另立整合任务处理。
2. 本任务直接范围内无已知未解决缺陷。未访问生产、未部署、未改配置、未保存真实账号密码或 JWT、未修改或测试法务模块、未执行物理删除。
