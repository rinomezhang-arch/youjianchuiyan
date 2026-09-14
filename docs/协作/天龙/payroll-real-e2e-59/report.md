# TL-RC-PAYROLL-HTTP-R2-59 完整回报

## 任务信息
- 任务ID: TL-RC-PAYROLL-HTTP-R2-59
- 分支: codex/tianlong-payroll-real-e2e-59
- 基线 base_sha: 3191e642627bdae385a95b2ff82a23baf4241f45
- 实现提交 SHA: 1099822fc827d7f173e00a218a2c99fd59980f80
- 远端最终 HEAD: 1099822fc827d7f173e00a218a2c99fd59980f80 (refs/heads/codex/tianlong-payroll-real-e2e-59)
- RESULT=reported

## 改动文件清单

| 文件 | 路径 | 字节数 | 说明 |
|------|------|--------|------|
| PayrollRealE2e59Test.java | banquet_project/src/test/java/com/youjian/banquet/controller/ | 21966 | JUnit 6 测试类 |
| env-precheck.sh | scripts/tianlong-payroll-real-e2e-59/ | 4299 | 环境前置断言脚本 |
| run-http-e2e.sh | scripts/tianlong-payroll-real-e2e-59/ | 20667 | HTTP 闭环主脚本 |
| payroll-http-schema.sql | scripts/tianlong-payroll-real-e2e-59/ | 4911 | 建表 DDL |
| env-precheck-http.txt | scripts/tianlong-payroll-real-e2e-59/ | - | 环境前置断言原始输出 |
| http-request-log.txt | scripts/tianlong-payroll-real-e2e-59/ | - | HTTP 请求清单（脱敏） |
| http-db-assertions.txt | scripts/tianlong-payroll-real-e2e-59/ | - | 数据库回读断言 |
| http-summary.txt | scripts/tianlong-payroll-real-e2e-59/ | - | 汇总报告 |
| mvn-test-raw.txt | scripts/tianlong-payroll-real-e2e-59/ | - | Maven 测试原始输出 |
| surefire-PayrollRealE2e59Test.txt | scripts/tianlong-payroll-real-e2e-59/ | - | Surefire 文本报告 |
| surefire-TEST-PayrollRealE2e59Test.xml | scripts/tianlong-payroll-real-e2e-59/ | - | Surefire XML 报告 |
| http-e2e-output.txt | scripts/tianlong-payroll-real-e2e-59/ | - | HTTP E2E 脚本原始输出 |

## 数据库标识
- Schema 名称: tlpay59_http_1789396644538
- 容器: youjian-mysql-test-13318
- 端口: 127.0.0.1:13318 -> 3306 (仅绑定本地)
- 镜像: mysql:8.0 (mysql:8.0.46)
- @@port: 3306, @@datadir: /var/lib/mysql/
- 无宿主目录挂载，仅 Docker 匿名 volume

## 逐条 HTTP 结果（脱敏）

| 步骤 | 方法 | 路径 | HTTP状态 | 业务码 | 要点 |
|------|------|------|----------|--------|------|
| L | POST | /api/auth/login | 200 | 200 | tlpay59_hr 登录成功 |
| L | POST | /api/auth/login | 200 | 200 | tlpay59_approver 登录成功 |
| L | POST | /api/auth/login | 200 | 200 | tlpay59_payer 登录成功 |
| L | POST | /api/auth/login | 200 | 200 | tlpay59_noperm 登录成功 |
| L | POST | /api/auth/login | 200 | 200 | tlpay59_mgr 登录成功 |
| L | POST | /api/auth/login | 200 | 200 | tlpay59_todisable 登录成功 |
| L | POST | /api/auth/login | 200 | 401 | tlpay59_disabled 停用账号被拒 |
| 1 | POST | /api/hr/payroll/save?month=2026-08 | 200 | 200 | saved=2 |
| 2 | POST | /api/hr/payroll/approve?month=2026-08 | 200 | 200 | approved=2 |
| 3 | POST | /api/hr/payroll/payout?month=2026-08 | 200 | 200 | paid=2 payoutId=1 |
| 4 | GET | /api/hr/payroll?month=2026-08 | 200 | 200 | emp4 status=3 net_pay=1595.00 |
| 5 | POST | /api/hr/payroll/payout?month=2026-08 | 200 | 200 | alreadyRecorded=true paid=0 |
| 6 | POST | /api/hr/payroll/save?month=2026-08 | 200 | 403 | 无权限角色保存被拒 |
| 7 | POST | /api/hr/payroll/save?month=2026-08 | 200 | 400 | 跨店保存被拒 |
| 8 | GET | /api/hr/payroll?month=2026-08 | 401 | - | 实时停用旧token立即401 |

## 反例结果

| 反例 | 期望 | 实际 | 结果 |
|------|------|------|------|
| 停用账号登录 | HTTP 200 + 业务码 401 | HTTP 200 + 业务码 401 | PASS |
| 无权限角色保存 | HTTP 200 + 业务码 403 | HTTP 200 + 业务码 403 | PASS |
| 跨店保存 | HTTP 200 + 业务码 400 | HTTP 200 + 业务码 400 | PASS |
| 重复付款 | alreadyRecorded=true, paid=0 | alreadyRecorded=true, paid=0 | PASS |
| 实时停用 | 旧 token 立即 401 | HTTP 401 | PASS |

## 关联与孤儿计数

- 孤儿(悬空 payout_id 或跨店指向): 0 (期望 0) PASS
- 台账 headcount/total_net 与本批工资行不符: 0 (期望 0) PASS
- 业务键回读恢复: staff4=3:1:1595.00, staff5=3:1:1595.00 (指向同一 payoutId=1) PASS
- 跨店零写入: 员工9 工资行 = 0 (期望 0) PASS
- 金额一致: 台账 total_net=3190.00 == SUM(net_salary WHERE status=3)=3190.00 PASS
- 流水唯一: payroll_payout_record 行数 = 1 (重复付款后仍 1) PASS

## 测试统计

### JUnit (PayrollRealE2e59Test)
- Tests run: 6
- Failures: 0
- Errors: 0
- Skipped: 0
- mvn 退出码: 0 (BUILD SUCCESS)
- 耗时: 7.541 s

### HTTP E2E 脚本
- PASS: 24
- FAIL: 0
- 退出码: 0

## 远端 HEAD 验证

```
$ git ls-remote origin codex/tianlong-payroll-real-e2e-59
1099822fc827d7f173e00a218a2c99fd59980f80	refs/heads/codex/tianlong-payroll-real-e2e-59
```

## 证据文件清单 (scripts/tianlong-payroll-real-e2e-59/)
- env-precheck.sh (环境前置断言脚本)
- env-precheck-http.txt (环境前置断言原始输出)
- run-http-e2e.sh (HTTP 闭环主脚本)
- payroll-http-schema.sql (建表 DDL)
- http-request-log.txt (HTTP 请求清单脱敏)
- http-db-assertions.txt (数据库回读断言)
- http-summary.txt (汇总报告)
- mvn-test-raw.txt (Maven 测试原始输出)
- surefire-PayrollRealE2e59Test.txt (Surefire 文本报告)
- surefire-TEST-PayrollRealE2e59Test.xml (Surefire XML 报告)
- http-e2e-output.txt (HTTP E2E 脚本原始输出)

## 未完成/不确定项
- 无。全部验收条件已覆盖并 PASS。

## 脱敏数据库断言摘要
- month_salary: staff4 status=3 payout_id=1 net_salary=1595.00
- month_salary: staff5 status=3 payout_id=1 net_salary=1595.00
- payroll_payout_record: payout_id=1 salary_month=2026-08 headcount=2 total_net=3190.00 recorded_by=tlpay59_payer
- 密码/JWT 全程仅存内存，所有日志/报告/提交已脱敏
- 数据库口令和合成登录口令随机生成，不输出、不提交
