# 工资审批付款 HTTP 闭环 R1 窄返工 — 真实 TCP 后端 + 真实登录 + 外部 HTTP 链

> 执行子代理：天龙 🦞（OpenClaw subagent）
> 日期：2026-09-13（Asia/Shanghai）
> 分支：codex/tianlong-payroll-real-e2e-28（固定候选 HEAD = b13c64740368d67fd12fc582c1f76994cef6fea6）
> 性质：对 TL-RC-PAYROLL-HTTP-LINUX-28 的 R1 changes_requested 窄返工（任务板事件 00000003）
> 本报告是既有 `README.md`（6/6 Java 用例）的 R1 补充，不覆盖、不重跑 Java 套件。

## 结论（一句话）

在隔离容器 `youjian-mysql-test-13317`（127.0.0.1:13317）上，从固定候选**真实启动**了一个只绑本机的非生产
Spring Boot 进程（127.0.0.1:18081），通过**真实 `POST /api/auth/login`** 为 6 个合成账号签发 JWT，
再经 **TCP（curl）** 跑通「保存 → 审批 → 付款 → 刷新回读 → 重复付款 → 停用账号 → 跨店 → 未知结果恢复」完整外部
HTTP 链，最后做数据库回读断言。**24 项断言全部通过，0 失败**；既有 6/6 Java 用例结果保留、未复跑。

---

## 1) 新 schema 名

- **本次成功运行 schema：`tlpay28_http_1789294026274`**（唯一时间戳，本运行新建，保留供复核）。
- 另有一个**首次失败运行的 seed-only schema** `tlpay28_http_1789293819699`：它因脚本 jar 路径笔误
  在「启动后端」这一步就中止，只建了表并写入 10 名合成账号，**没有任何工资/台账数据**；按边界铁律
  （禁 DELETE/DROP/TRUNCATE）**保留未动**，已如实记录。
- 13317 上此前已存在的 `payroll_it_*`、`payroll_canonical_*`、历史 `tlpay28_*` 等 schema **一个未动**。

## 2) 进程端口 + 就绪证据

- 端口：**127.0.0.1:18081**（只绑本机，`--server.address=127.0.0.1`）。
- 启动命令（脱敏，密钥走环境变量不进命令行）：
  ```
  java -jar .../banquet_project/target/banquet-1.0.0.jar \
    --server.address=127.0.0.1 --server.port=18081 \
    --spring.jpa.hibernate.ddl-auto=none \
    --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
  环境变量: JWT_SECRET=<随机hex64,内存> AES_SECRET_KEY=<随机hex32,内存> APPROVAL_APPROVERS=tlpay28_approver
            SPRING_PROFILES_ACTIVE=dev SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:13317/<schema>?...
            SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=<空>
  ```
- 就绪证据（原始日志见 `scripts/tianlong-payroll-real-e2e-28/app-startup.log`，脱敏后入库）：
  - `Tomcat initialized with port 18081 (http)`
  - `Tomcat started on port 18081 (http) with context path ''`
  - `Started BanquetApplication in 12.991 seconds`
  - 就绪探测 `GET /api/stores`（无 token）→ **HTTP 401**（进程已监听、鉴权链已生效）。
- 运行结束已停掉该非生产进程（pid 3004773），未留孤儿；未重启/未部署任何生产服务。

## 3) 请求清单与各步 HTTP 状态

| 步骤 | 方法 | 路径 | actor | HTTP | 业务码 | 说明 |
|---|---|---|---|---|---|---|
| 登录 | POST | /api/auth/login | tlpay28_hr | 200 | 200 | JWT 入内存（长度 228） |
| 登录 | POST | /api/auth/login | tlpay28_approver | 200 | 200 | JWT 入内存（长度 236） |
| 登录 | POST | /api/auth/login | tlpay28_payer | 200 | 200 | JWT 入内存（长度 232） |
| 登录 | POST | /api/auth/login | tlpay28_noperm | 200 | 200 | JWT 入内存（长度 239） |
| 登录 | POST | /api/auth/login | tlpay28_mgr | 200 | 200 | JWT 入内存（长度 244） |
| 登录 | POST | /api/auth/login | tlpay28_todisable | 200 | 200 | JWT 入内存（长度 243） |
| 停用账号 | POST | /api/auth/login | tlpay28_disabled | 200 | **401** | 停用账号登录被拒 |
| 1 保存 | POST | /api/hr/payroll/save?month=2026-08 | tlpay28_hr | 200 | 200 | saved=2 |
| 2 审批 | POST | /api/hr/payroll/approve?month=2026-08 | tlpay28_approver | 200 | 200 | approved=2 |
| 3 付款 | POST | /api/hr/payroll/payout?month=2026-08 | tlpay28_payer | 200 | 200 | paid=2 payoutId=1 |
| 4 回读 | GET | /api/hr/payroll?month=2026-08 | tlpay28_approver | 200 | 200 | emp4 status=3 net_pay=1595.00 |
| 5 重复付款 | POST | /api/hr/payroll/payout?month=2026-08 | tlpay28_payer | 200 | 200 | alreadyRecorded=true paid=0 |
| 6 无权限 | POST | /api/hr/payroll/save?month=2026-08 | tlpay28_noperm | 200 | **403** | 无权限角色被拒 |
| 7 跨店 | POST | /api/hr/payroll/save?month=2026-08 | tlpay28_mgr | 200 | **400** | 店长保存 2 号店员工 9 被拒 |
| 8 实时停用 | GET | /api/hr/payroll?month=2026-08 | tlpay28_todisable | **401** | — | 停用前 200，UPDATE 为 resigned 后旧 token 立即 401 |

完整脱敏清单见 `scripts/tianlong-payroll-real-e2e-28/http-request-log.txt`。

## 4) 数据库回读断言（schema tlpay28_http_1789294026274）

| 断言 | 结果 |
|---|---|
| 金额一致 | 台账 `total_net=3190.00` == `SUM(net_salary WHERE status=3)=3190.00` ✅ |
| 流水唯一 | `payroll_payout_record` 仅 **1** 条（重复付款后仍 1） ✅ |
| 无孤儿 | 悬空/跨店指向 = 0；台账 headcount/total_net 与工资行守恒一致 = 0 ✅ |
| 业务键恢复 | staff4/5 均 `status=3, payout_id=1, net=1595.00`（`staff_id,salary_month` 回读恢复） ✅ |
| 跨店零写入 | 跨店目标员工 9 的工资行 = 0 ✅ |

原始输出见 `scripts/tianlong-payroll-real-e2e-28/http-db-assertions.txt`。

## 5) 通过 / 失败数字

- **R1 HTTP 链：PASS=24，FAIL=0**（登录 6 + 停用登录 1 + 链 8 + DB 断言 5 + 环境前置 1 + 建表 1 + 种子 1 + 就绪 1，共 24 个 ok 断言，0 个 fail）。
- **既有 6/6 Java 用例保留**：未复跑 `PayrollRealE2e28Test`，`surefire-TEST-PayrollRealE2e28Test.xml` 维持
  `tests="6" errors="0" skipped="0" failures="0"`（见本目录既有证据文件，未改动）。

## 6) 提交 SHA + ls-remote

见提交记录（本次提交信息：【天龙】工资HTTP闭环28-R1：真实TCP后端+/api/auth/login真实登录+外部HTTP链证据）。
`git ls-remote origin codex/tianlong-payroll-real-e2e-28` 与本地 HEAD 一致（无 force push）。

## 7) 证据文件清单（路径 + 字节）

| 文件 | 字节 | 说明 |
|---|---|---|
| scripts/tianlong-payroll-real-e2e-28/env-precheck.sh | 5041 | 环境闸门（R1 扩展后） |
| scripts/tianlong-payroll-real-e2e-28/env-precheck.txt | 582 | 既有 6/6 运行原始前置输出（未动） |
| scripts/tianlong-payroll-real-e2e-28/env-precheck-http.txt | 790 | 本运行前置输出（复用+扩展后） |
| scripts/tianlong-payroll-real-e2e-28/payroll-http-schema.sql | 6036 | 独立 schema 建表 DDL（仅 CREATE/INSERT） |
| scripts/tianlong-payroll-real-e2e-28/run-http-e2e.sh | 21831 | 主脚本（一次运行，密码/JWT 仅内存） |
| scripts/tianlong-payroll-real-e2e-28/http-request-log.txt | 2184 | 请求清单（脱敏） |
| scripts/tianlong-payroll-real-e2e-28/http-db-assertions.txt | 929 | DB 回读断言结果 |
| scripts/tianlong-payroll-real-e2e-28/http-summary.txt | 1785 | 通过/失败汇总 + 启动命令（脱敏） |
| scripts/tianlong-payroll-real-e2e-28/app-startup.log | 8560 | 后端启动日志（脱敏，force-add 因 *.log 被 gitignore） |
| docs/协作/天龙/payroll-real-e2e-28/R1-http-e2e-report.md | 本文件 | R1 报告 |

脱敏红线遵守：全程无 JWT 明文、无密码明文、无 BCrypt 散列落任何文件/日志/报告；响应体 token 字段只记长度；
`eyJ` 扫描仅命中 run-http-e2e.sh 里的脱敏规则本身（非真实 token）。

## 8) 未完成 / 不确定项（如实列出）

1. **首次运行失败（jar 路径笔误）**：`run-http-e2e.sh` 初版 `REPO_ROOT` 多跳一级，导致 `java` 报
   "Unable to access jarfile"，后端未启动即中止；已修正为 `SCRIPT_DIR/../..` 后复跑通过。该次遗留一个
   seed-only schema `tlpay28_http_1789293819699`，因禁 DROP **保留未动**。
2. **观察到既有生产进程**：本机 18:05 起有一个 `java ... -jar target/banquet-1.0.0.jar --spring.profiles.active=prod`
   监听 **8080**（cwd `/home/ubuntu/deploy_tmp_main/banquet_project`，pid 3003450），非本次任务启动；
   **全程未触碰**，本任务只启 18081 非生产进程、只连 13317。
3. **法务 COS 未配置告警**：启动日志有一条 `LegalCosConfig` WARN（"法务 COS 未配置…案卷证据模块不可用"），
   与本工资闭环无关，未启用法务模块、未访问任何法务数据，属正常非阻塞提示。
4. 后端以 **dev profile + 环境变量注入 datasource** 启动（非 prod），真实登录/鉴权链（JwtAuthInterceptor →
   StaffRealtimeGuard → ApprovalAuthorityInterceptor → StoreDataScopeAspect/AuditLogAspect）全量生效。

## 边界遵守确认

- 未 DELETE/DROP/TRUNCATE/清理任何 schema 或数据（只 CREATE/INSERT/UPDATE 本 schema 内合成数据）。
- 未改任何业务源码（src/main）、配置、前端、部署文件（本次仅 `scripts/` 与 `docs/` 两处允许路径）。
- 未部署、未重启任何生产服务；未使用真实账号（合成账号全 tlpay28_ 前缀，密码运行期随机）。
- 未连宿主 3306、Windows 13317、生产库（仅 127.0.0.1:13317）；未动 COS 任务板（父代理负责上板）。
- 只运行本任务专属 HTTP 链一次，未复跑 Java 套件。
