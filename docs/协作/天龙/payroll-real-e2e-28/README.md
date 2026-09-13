# 工资审批付款真实 HTTP 闭环 — TL-RC-PAYROLL-HTTP-LINUX-28

> 执行子代理：天龙 🦞（OpenClaw subagent）
> 日期：2026-09-13（Asia/Shanghai）
> 分支：codex/tianlong-payroll-real-e2e-28（基线 5748aa416f2540009f5b7e68c8d08295218d6ff8）

## 结论（一句话）

在隔离库 `127.0.0.1:13317` 上，用真实 JWT + 真实鉴权链（JwtAuthInterceptor → StaffRealtimeGuard 实时复核）+
真实事务 JDBC 跑通了「核算保存 → 审批角色审批 → 付款角色付款 → 刷新回读」的 HTTP 闭环，
五条验收全部通过；6 个测试 6 通过 0 失败 0 错误 0 跳过，mvn 退出码 0。

## 环境前置断言（验收第 1 条）— 通过

脚本 `scripts/tianlong-payroll-real-e2e-28/env-precheck.sh`，退出码 0，原始输出 `env-precheck.txt`。逐条实测：

| 断言项 | 期望 | 实测 | 结果 |
|---|---|---|---|
| hostname | VM-0-14-ubuntu | VM-0-14-ubuntu | PASS |
| 容器名 | youjian-mysql-test-13317 | 同名 | PASS |
| 镜像 | mysql:8.0 | mysql:8.0 | PASS |
| 状态 | running | running（Up 5 days） | PASS |
| 端口映射 | 13317→3306 | `{"3306/tcp":[{"HostIp":"","HostPort":"13317"}]}` | PASS |
| 宿主目录挂载 | 无 | Binds=null、Mounts=[] | PASS |
| @@port | 3306 | 3306 | PASS |
| @@datadir | /var/lib/mysql/ | /var/lib/mysql/ | PASS |
| @@version（记录） | 8.0.46 | 8.0.46 | 记录 |
| root 空密码（记录） | 只读 SELECT 1 | 成功 | 记录 |

脚本设计：任一断言不符即非零退出、且全程零写入（仅 `SELECT @@port/@@datadir/@@version` 与 docker inspect 只读命令）。

## 验收第 2 条 — 通过

- 所有连接仅指向 `jdbc:mysql://127.0.0.1:13317/`（测试类常量 `HOST`）。
- 每次运行新建**唯一** schema：`tlpay28_<毫秒时间戳>_<随机>`，测试结束保留供复核（`TLPAY28_SCHEMA_RETAINED=...` 打印在 maven 原始输出）。
- 只造合成账号（审批/付款/核算/员工/无权限/停用/店长/跨店员工），未删任何已有 schema 与测试数据；
  实测隔离库本次运行后 `tlpay28%` 共 12 个 schema（6 个首次失败运行 + 6 个成功运行，均保留），
  此前已存在的 `payroll_it_*`、`payroll_canonical_*` 等历史 schema 一个未动。
- 合成花名册（`insertStaff`）只写本 schema 的 `staff_master`，全部走 `staff_id`/`store_id` 显式列。

## 验收第 3 条（核算保存 → 审批 → 付款 → 回读，三方一致）— 通过

测试方法 `saveApprovePayoutReadbackAcrossDistinctRoles`。三个角色分工：
- 核算 `tlpay28_hr` 保存 → HTTP 200 + Result.code 200 + `saved=2`，库内 status=1、net_salary=1595.00；
- 审批 `tlpay28_approver` 审批 → HTTP 200 + code 200 + `approved=2`，库内 status=2、approved_by=tlpay28_approver；
- 付款 `tlpay28_payer` 付款 → HTTP 200 + code 200 + `paid=2`，库内 status=3、paid_by=tlpay28_payer、payout_id 已挂；
- 刷新回读 GET → HTTP 200，`salary_status=3`、`net_pay=1595`，与落库一致；
- 金额一致：台账 `payroll_payout_record.total_net=3190.00` == `SUM(month_salary.net_salary WHERE status=3)=3190.00`。

DB 最终态证据（`db-final-state-mainflow.txt`，schema tlpay28_1789292412012_4129）：
```
month_salary: staff 4/5，status=3，approved_by=tlpay28_approver，paid_by=tlpay28_payer，payout_id=1
payroll_payout_record: 1 条，headcount=2，total_net=3190.00，recorded_by=tlpay28_payer
台账一致性: 3190.00 == 3190.00
```

## 验收第 4 条（四类反例均被拒绝，成功金额不变、台账仅一条）— 通过

| 反例 | 测试方法 | 结果 | 证据 |
|---|---|---|---|
| 停用账号（employment_status=resigned） | `disabledAccountRejected` | HTTP 401（save/approve/payout/get 全部 401），零写入 | mvn 日志「实时复核未通过」 |
| 无权限角色（waiter，can_manage_hr=0） | `noPermissionRoleRejected` | HTTP 200 + Result.code 403，month_salary/payout_record 均为 0 | 断言 |
| 跨门店保存（店长保存 2 号店员工） | `crossStoreRejected` | HTTP 200 + Result.code 400，整批回滚 0 行 | 断言 |
| 重复付款 | `duplicatePayoutRejected` | 第二次付款 `alreadyRecorded=true`、`paid=0`，台账仍 1 条，工资行与台账逐行相等、实发合计不变 | 断言 |

说明：停用账号由 JwtAuthInterceptor → StaffRealtimeGuard 在入口拦下（HTTP 401）；
无权限/跨门店由 Controller `checkPayrollAccess` / Service 校验拦下（业务码 403/400，HTTP 恒 200）。

## 验收第 5 条（无孤儿 + 业务键回读 + 不重复扣款）— 通过

测试方法 `noOrphansAndBusinessKeyRecovery`：
- 孤儿/守恒断言（`conserved()`，与基线同口径）：工资行 payout_id 不悬空、不跨门店指向；台账 headcount/total_net 与本批工资行严格一致 —— 全通过；
- 业务键 `(staff_id, salary_month)` 回读：每名员工 status=3、payout_id 指向同一台账，本批 `SUM(net_salary)` == 台账 `total_net`；
- 不重复扣款：重复付款 `alreadyRecorded=true`、台账仍 1 条。

「资金流水/财务流水」在本工资闭环中即 `payroll_payout_record`（工资发放记账台账）。
`PayrollService` 明确「本服务不执行、也没有能力执行真实工资支付」，不与 `finance_account` 等总账模块发生跨表写入，
因此工资（month_salary）/审批状态/付款台账（payroll_payout_record）之间是本次闭环唯一的资金关系，已断言无孤儿、金额守恒。

## Surefire 精确数字

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.12 s
```
XML 头：`tests="6" errors="0" skipped="0" failures="0"`。mvn 退出码 0。

## 证据文件清单

| 文件 | 字节 |
|---|---|
| scripts/tianlong-payroll-real-e2e-28/env-precheck.sh | 3938 |
| scripts/tianlong-payroll-real-e2e-28/env-precheck.txt | 582 |
| scripts/tianlong-payroll-real-e2e-28/mvn-test-raw.txt | 7419 |
| scripts/tianlong-payroll-real-e2e-28/surefire-PayrollRealE2e28Test.txt | 352 |
| scripts/tianlong-payroll-real-e2e-28/surefire-TEST-PayrollRealE2e28Test.xml | 37625 |
| scripts/tianlong-payroll-real-e2e-28/tlpay28-schemas.txt | 324 |
| scripts/tianlong-payroll-real-e2e-28/db-final-state-mainflow.txt | （见 commit 内字节数） |
| banquet_project/src/test/java/com/youjian/banquet/controller/PayrollRealE2e28Test.java | 测试源码 |

脱敏红线：全程无 JWT 明文、无密码明文入日志/报告；JWT 只在内存签发（secret 为运行期 UUID 随机），
日志/报告只保留操作人登录名（合成账号）与金额，不含任何凭证明文。

## 边界遵守确认

- 未 DELETE/DROP/TRUNCATE 任何 schema 或测试数据；
- 未改任何业务源码（main 目录）、配置、部署文件，未重启生产服务；
- 未使用真实账号；合成账号全部 tlpay28_ 前缀；
- 未连宿主 3306、Windows 13317、生产库（仅 127.0.0.1:13317）；
- 未动 COS 任务板；
- 仅运行本任务专属测试类一次（`mvn -B test -Dtest=PayrollRealE2e28Test`），未跑整套回归。
