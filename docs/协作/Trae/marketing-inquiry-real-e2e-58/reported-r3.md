# TR-MARKETING-INQUIRY-REAL-E2E-58 R3 报告（返工收口）

执行人：Trae。日期：2026-09-14。分支：codex/trae-marketing-inquiry-real-e2e-58。

## 本轮范围（R3 退回三项 + 一项附加，不重跑主链）

R3 退回（Codex changes_requested，20260914T090240Z）只剩三项证据修正，另加一项脚本修正。
同载荷重放证据由 Codex 在 ccf24b31de9853f9190d51d23914b060928a4b44（分支 codex/integration-20260913，
docs/协作/Codex/tr58-replay-evidence/）补证完成：预检 9 项 true，业务 code=200，同一 inquiryNo=INQ3，
booking_inquiry 1->1、marketing_attribution_event 2->2，两个 delta=0，退出码 0，stderr 为空。
按指令本轮不重复执行重放，不启动任何服务，零数据库写入。

## 1. 截图脱敏修正（5/5 人工逐张核对）

原 R2 黑条位置错误（号码在黑条上方仍完整可见，且宽幅黑条遮住大块业务内容）。
本轮从 e4c5b5fe 提取未遮原图，按像素扫描定位（阈值扫描 + 人工放大核对）后只遮号码字符：

- 01-submit-success.jpg：门店联系电话，遮罩 x=232..358, y=898..927。标签"联系电话："与咨询编号 INQ3、
  门店信息卡片全部保留可见。人工核对：完整号码不可见。
- 02-lookup-success.jpg：手机号输入框，遮罩 x=168..308, y=278..300。INQ3、处理中、期望日期、人数、
  提交时间均保留。人工核对：完整号码不可见。
- 03-wrong-phone-empty.jpg：同 02 坐标。空态文案保留。人工核对：不可见。
- 04-invalid-inquiry-empty.jpg：同 02 坐标。INQ999 与空态文案保留。人工核对：不可见。
- 05-system-error.jpg：同 02 坐标。系统繁忙与重试按钮保留。人工核对：不可见。

redact-screenshots.sh 已同步改为上述精确坐标（只遮号码矩形，不再整行横幅），坐标来源已写注释。

## 2. 同载荷重放脱敏原始输出

由 Codex 补证（见上）。输出文件：docs/协作/Codex/tr58-replay-evidence/same-payload-output.txt，
核验脚本 verify-replay.py，说明 README.md，均在提交 ccf24b31。执行方未重复运行。

## 3. 报告修正

本文件即 R3 报告，替代误复用 R2 的报告。精确提交与远端最终 HEAD 见末节。

## 4. 附加修正（评审末条）

seed.sh 与 setup-env.sh 中 ${STORE_PHONE:-REDACTED} 缺省值已改为必填运行时变量
${STORE_PHONE:?...}，未设置即报错退出，脚本不会再缺省写入非法号码文本。

## 测试数字

- 本轮改动性质：证据修正 + 脚本守卫，无产品代码改动，无服务启动，无数据库写入。
- 截图人工核对：5/5 通过（PASS=5, FAIL=0）。
- 重放：NOT_RUN_BY_TRAE（Codex 已补证 PASS=1：200 + 同一 INQ3 + 两表 delta=0）。
- 主成功链 / 错误手机号 / 非法咨询号 / 系统错误：沿用 R2 已验收证据，本轮 NOT_COVERED（按指令不重跑）。

## 提交与 HEAD

- 基线 base_sha：e5adb15a82e762e3646ca742e3986432428b1432
- R2 提交：836d1308488891b26a0fcb38d817b43538c3d90f
- 本轮提交与远端最终 HEAD：提交后在此回填精确 SHA（见任务板 reported 事件与登记簿）。

## 边界声明

未改产品代码、迁移、配置、任务板工具；未触碰生产、共享进程、法务；未执行 DELETE/DROP/TRUNCATE；
报告不含凭证明文、真实手机号或客户信息（截图中号码均为合成测试号且已遮罩）。
