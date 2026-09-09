# CO30 第四轮精简结果

来源HEAD：e99c2ba87a40b804bc5cf1a93ed3116f104381c9。main compile已修。
第四轮原因：明确排除未发布Guest，原AuthAuditPrivacyTest已归档，派生ScopedAuthAuditPrivacyTest，仅删Guest import/数组项/authorize分支及改名，其他断言保持原样；另外两个测试原字节保持。
本轮执行=21；pass/fail/error/skip=21/0/0/0；Maven exit=0；package=PASSED。
ScopedAuthAuditPrivacyTest: 2 tests, 0 failures, 0 errors, 0 skipped。
BillReceiptTest: 15 tests, 0 failures, 0 errors, 0 skipped。
IpadOrderScopeTest: 4 tests, 0 failures, 0 errors, 0 skipped。
JAR：F:/solo/artifacts/release-candidates/rc30-hybrid-20260909/target/banquet-1.0.0.jar。
SHA-256：3b5c428da3c9191aa48eb2f77447316a3919f4b362e2707e0b3ffe2b40bad16f
前三轮失败证据及原测试已保留，三个归档hash均核验不变。当前生产Java白名单12项；300个非白名单生产Java及3个法务文件字节保留，改密方法保留；本轮312份Java和26份构建输入hash未变。
未覆盖：未发布Guest authorize审计场景、其余48份测试源、真实DB/E2E/全后端。仅可称派生范围测试通过，不称原鉴权测试原样通过。
无DB连接/服务启动/部署/生产或工作树修改；未进行第五次Maven。来源和数字详见manifest.json、source-manifest.json；派生差异见scoped-auth-test.diff。
未完成项：本次授权范围内无；后续整合与发布由统筹负责。
