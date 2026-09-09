# TR-RECEIPT-REAL-24 started（Trae）

- 卡片：docs/协作/Codex/TR-RECEIPT-REAL-24.json；base 7c782b86；独立工作树 F:/solo/artifacts/team-worktrees/trae-receipt-real-24（分支 codex/trae-receipt-real-24）
- 范围：只做「BillManage 实际打印按钮 → GET /api/bills/{bookingId}/receipt?storeId 真实后端快照（Result JSON）→ 同步开窗安全 textContent 预览（80mm）→ window.print 另存 PDF」一条链；替换 printBill 虚假成功提示
- 环境：13317 MySQL（schema co_print23_20260909_022305，订单 COPRINT23-BK-001，100.00）只读断言；后端 18083；Java -Xmx512m、Maven 256m、单一浏览器上下文
- RC15 检查点已保留（trae-release-rc-15 HEAD 5f4511b7，reported）；RC15 的 18080 jar/5183 代理已停释放内存；共享 MySQL 未重启
- 禁止项遵守：不碰生产/法务/鉴权全局/request 拦截器；不重灌库；不重跑工资13/iPad16/打印配置15；不装依赖
