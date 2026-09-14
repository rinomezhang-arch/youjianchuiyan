# TR-MARKETING-INQUIRY-API-R2-57 完成报告 reported

## 一、结论

PASS。天龙 TL55 营销咨询后端 R1 验收退回的两项均已修复，并在云端隔离 MySQL 中完成定向验证：一次 Maven 构建内 MarketingInquiryApiTest 共 13 项全部通过（11 项 TL55 既有回归 + 2 项 R2 新增定向反例），Tests run: 13, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。

PASS 13 / FAIL 0 / NOT_COVERED 0。

## 二、分支与提交

- 工作分支：codex/trae-marketing-inquiry-api-r2-57
- 任务基线：7db5d3b286b47c4b401b7b328a05b8c4909a210a（TL55 R1 候选）
- 修复提交：40f1f6b5（两项源码修复 + 两个定向反例测试 + started 记录）
- 验证脚本提交：2468c742（隔离库定向 runner）
- 证据提交：d8f3f9ab（库级旁证脚本与完整测试日志）
- 最终 HEAD：d8f3f9ab7b94335f1c2b5b6e542554c45b426be1

## 三、改动范围（仅任务卡允许路径）

源码与测试 3 个文件：
1. banquet_project/src/main/java/com/youjian/banquet/service/MarketingInquiryService.java
2. banquet_project/src/main/java/com/youjian/banquet/controller/BookingInquiryController.java
3. banquet_project/src/test/java/com/youjian/banquet/marketing/MarketingInquiryApiTest.java

本任务专属脚本与证据：
4. scripts/trae-marketing-inquiry-api-r2-57/run-r2.sh
5. scripts/trae-marketing-inquiry-api-r2-57/evidence-r2.sh
6. scripts/trae-marketing-inquiry-api-r2-57/r2-output.txt（完整构建与测试日志）
7. docs/协作/Trae/marketing-inquiry-api-r2-57/ 下的 started.md 与本报告

未改动任何实体、迁移、依赖、配置、全局异常处理、审批与转预订链路，未触碰其他接口。

## 四、退回项一：幂等完整载荷比较

问题：MarketingInquiryService.resolveReplay 重放回查时只比较了部分字段，漏比 remark（备注）以及业务语义 business_type=booking_inquiry、business_no=INQ加咨询ID。同一 requestId 仅把备注改成别的值，会被误判为相同重复请求并返回原回执，篡改不被拦截。

修复（仅在 resolveReplay 及其调用处）：
- submit 的 catch 回查调用补传本次请求的 remark。
- 回查 marketing_attribution_event 时补取 business_type、business_no 两列；事件一致判定除原有字段外，新增 business_type 必须等于 booking_inquiry、business_no 必须等于 INQ加本次业务ID。
- 回查 booking_inquiry 时补取 remark；咨询一致判定新增 remark 与本次请求备注完全相等。
- 任一不一致维持既有语义：抛 IllegalStateException，由提交接口映射为业务 409，事务回滚，不新增任何记录。

定向反例（新增测试 httpSameRequestIdOnlyRemarkChanged409，真实 HTTP + 隔离 MySQL）：
- 首次提交带备注 A 返回 200 与回执；同 requestId 仅把备注改为 B，断言返回业务 409。
- 断言原咨询备注仍为 A、状态不变，原回执不被覆盖。
- 断言该 requestId 在 marketing_attribution_event 始终只有一行、booking_inquiry 始终只有一行，篡改与重放零新增。
- 断言携带原备注 A 的完全相同载荷重放仍返回同一原回执（正常幂等不被误伤），事件表仍只有一行。

## 五、退回项二：lookup 系统异常误判

问题：BookingInquiryController.lookupMarketingInquiry 的 catch(Exception) 把一切异常都吞成 Result.success(null)。查无此单、手机号不符、非法输入本就应返回 success(null) 的空态；但数据库断连、SQL 错误等系统异常也被伪装成空态，前端无法按 TR56 契约（业务 code 非 200 进入错误态）区分“查不到”和“系统故障”。

修复（仅该方法异常分支）：
- catch 收窄为 org.springframework.dao.DataAccessException，命中时返回 Result.error(500, 固定中文文案“系统繁忙，请稍后重试”)，data 为 null，不回传、不记录任何异常明文。
- 服务层返回 null（查无/手机号不符/非法输入）仍走 Result.success，空态语义不变；非 DataAccessException 的其他异常不再被该分支吞掉。

定向反例（新增测试 lookupSystemFailureReturnsBiz500NotNull，控制器直调 + Mockito 桩）：
- 桩令 marketingInquiryService.lookup 抛 DataRetrievalFailureException（DataAccessException 子类，模拟数据库/SQL 系统故障）。
- 断言响应 code=500、message 恰为“系统繁忙，请稍后重试”、data=null，而非 success(null)；响应中不含异常明文。
- 既有的查无/手机号不符/非法输入空态由原 lookup 四态测试继续守护，本次一并通过。

## 六、云端隔离验证

环境（独立 worktree /home/ubuntu/tr57-work，detached 于 d8f3f9ab，与生产工作树 main 完全隔离）：
- 容器 youjian-mysql-test-13317，镜像 mysql:8.0.46，状态 Up 5 days，端口 0.0.0.0:13317->3306，Mounts 为空（无宿主目录挂载）。
- 闸门核实：@@port=3306、@@datadir=/var/lib/mysql/，三项闸门均通过；JDK 17.0.19、Maven 3.8.7。
- 新建带时间戳隔离库 tl55_inquiry_r2_20260914_141518，依次应用 TL55 基座 base_schema.sql 与迁移 marketing_publication_v1.sql，迁移自校验输出 completed。
- 测试由环境变量 YOUJIAN_TEST_MYSQL=1、YOUJIAN_TEST_MYSQL_PORT=13317、YOUJIAN_TEST_MYSQL_DATADIR=/var/lib/mysql/、TL55_SCHEMA 指向新库驱动，真实 HTTP 随机端口 + 隔离 MySQL。

测试结果：surefire 汇总 Tests run: 13, Failures: 0, Errors: 0, Skipped: 0；surefire XML 中 13 个 testcase 均无 failure/error，两个新增方法 httpSameRequestIdOnlyRemarkChanged409、lookupSystemFailureReturnsBiz500NotNull 均实际执行通过。完整日志见 scripts/trae-marketing-inquiry-api-r2-57/r2-output.txt。

库级旁证（只读 SELECT，evidence-r2.sh）：
- 实例下 TL55 前缀库共 5 个：既有 4 个（含 TL55 最终库 tl55_inquiry_20260914_060145）全部原样保留，新增本次 r2 库一个；未 DROP、未 DELETE、未 TRUNCATE、未清理任何旧库。
- 本次新库两表各 9 行（8 条来自既有有效提交用例，1 条来自退回项一反例的首次合法提交；篡改重放与同载荷重放零新增，退回项二反例为纯 Mockito 不写库）。
- 退回项一反例对应 requestId（前缀 req-http-rmk）在事件表仅 1 行，business_type=booking_inquiry、business_no=INQ7，与咨询主键一致。
- 对应合成测试号码（尾号 0021）咨询仅 1 行，备注保持为原始值 A、status=pending、未转预订；备注为篡改值 B 的记录数为 0。

## 七、合规说明

- 仅在云端容器隔离库执行，未接触生产库与本机 Windows 库；生产工作树始终停留在 main，未切换、未重置。
- 全程无 DROP/DELETE/TRUNCATE，旧 TL55 库保留不清理。
- 对外 500 仅返回固定中文文案，异常明文不进响应、不进报告。
- 报告中业务号码均为测试合成数据并已脱敏，不含密码、令牌、真实客户姓名或手机号。
