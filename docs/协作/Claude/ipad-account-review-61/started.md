# CL-IPAD-ACCOUNT-REVIEW-61 started

已 claim。只读 `git diff ccf24b31..e226289f` 的三个前端文件，
读 `docs/协作/Codex/ipad-account-recovery-60/` 报告与测试日志。
不扫全仓、不复跑测试、不改业务代码。

重点核查：切店时旧请求（成功/失败）不得回写新账户；
现金找零弹窗与重复点击对幂等键、支付状态的影响；
区分本次引入缺陷 vs 基线已有缺陷；核实 15/15 覆盖边界，
不把历史浏览器 30 项当作新 HEAD 端到端通过。
