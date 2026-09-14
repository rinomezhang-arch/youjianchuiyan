# Claude 上线前最终状态与阻断项清单（2026-09-15）

无在途任务。以下为交付 SHA、上线阻断项、可留用脚本清单。**我不自行部署。**

## 一、最终 SHA

| 任务 | 分支 | 最终 SHA | 状态 |
| --- | --- | --- | --- |
| CL-AUTH-BLANK-PASSWORD-GUARD-70 | codex/claude-auth-blank-password-70 | `6c099e020f312f4a9762f81f0e47565fef78a846` | reviewed，**未合并** |
| CL-PAYROLL-SCRIPT-SAFETY-72 | codex/claude-payroll-script-safety-72 | `f63634d03ec8502048c013ac68004a25e4b00d67` | changes_requested，已移交 TL79 |
| CL-VISUAL-RC-PACKAGE-22 | （四个视觉提交已等价进入候选 013afcf1） | 干净构建源 `226d765f`，产物树 `d65e6697…d0b5b` | 已补证 reported |
| 文档/证据 | codex/production-readiness-20260905 | `8f968358` | 已推送 |

## 二、上线阻断项（按严重度）

**1. 阻断级 —— 空密码登录漏洞的修复分支尚未合并。**
CL67 查实：候选的 `AuthController.java` 与 `IpadAuthController.java` 明文密码分支
只判 `null`、不判空串，若某账号库内密码字段为空字符串，提交空密码即可登录（PC 与 iPad 两端都中招）。
CL70 已修并通过验收（`6c099e02`，两行 `|| password.isBlank()` + 5/5 专属测试），
但**该分支尚未并入发布候选**。若按当前候选直接上线，这个洞仍在线上。
→ 合并 `codex/claude-auth-blank-password-70` 是上线前必须做的动作。

**2. 阻断级（生产既有）—— 已发放工资再保存会被静默退回。**
CL45 查实：**生产**的 `save` 硬写 `status=1` 且无状态条件，已发放记账(3)的月份
再点一次保存就被静默退回"已保存"，发放在库里消失且无任何提示。
候选已修（CL50 实测确认候选是显式 400 报错），但**生产上线前这个洞一直在**。

**3. 迁移顺序硬依赖，顺序反了直接失败。**
`payable_create_request_v1` 的外键要引用的 `uk_finance_payable_id_store`，
是由 `payable_settlement_record_v1` 建的 —— **settlement 必须先跑**。
另：`receipt_payable_source_v1` 是本批风险最高的一份（无任何幂等守卫、
动两张有数据的生产表并加唯一索引）；iPad v2 需先跑 v1 再重新采集 + 跑 checker 才能上。
详见 `docs/协作/Claude/CL61/`。

**4. 运营风险（非阻断，但上线要告知一线）。**
iPad 收款的"支付结果待确认"状态只存在组件内存，**刷新页面即丢失**。
若后端实际已收款成功而前端状态丢了，界面看不出来，需要人工对账。
不是重复扣款风险（幂等键会重新生成），但一线要知道该怎么处理。

**5. 工资金额不受解锁码保护（既有设计，非本轮引入）。**
`GET /api/hr/payroll` 不调 unlock 也返回真实金额；解锁码只是客户端展示遮罩。
真正的访问控制是 `can_manage_hr`。别把解锁码当防护写进上线清单。

## 三、未验证/不可当作通过的项

- CL72 的脚本安全修复（`f63634d0`）：代码逻辑经审查自洽，但**没有一次干净的自动化
  测试证据**，Windows 侧验证失败已如实记录，移交 TL79 在 Linux 侧验。
  在 TL79 出结论前，**不要把 `run-http-e2e.sh` 的运行结果当作验收证据**。
- 我全程未做真实设备/浏览器/生产环境验证，未连生产库。

## 四、可留用脚本与资产（我的部分）

建议保留：
- `scripts/release/finance-integrate-34/money_chain_check.py`（12/12 资金链校验）
- `scripts/release/release-safe-25/`（发布/回退安全实现与反例套件）
- `scripts/release/restaurant-rc-15/{deploy-rc15.sh,rollback-rc15.sh}`
- `artifacts/release-candidates/cl61-migration-pack-20260909/`（迁移顺序与风险对照包）
- `docs/协作/Claude/` 全部报告（CL45/50/61/64/67/70/72/22 的证据与结论）

其余由我产生的临时目录/中间产物，交 Codex 统一盘点，**我不删除任何东西**。
