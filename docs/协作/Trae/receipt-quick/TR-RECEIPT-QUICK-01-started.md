# TR-RECEIPT-QUICK-01 started（Trae，2026-09-07）

- 工作树：F:/solo/artifacts/team-worktrees/trae-mobile-cost，分支 codex/trae-mobile-cost，基线 93741a9。
- 父基线 9828ecdc 中 Receipt.vue blob=19f8a8f7cdf2a69d0a447b62527ede5b2065f07c，现树核一致后动工。
- 唯一文件边界：frontend_v3/src/views/dashboard/Receipt.vue；个人 scripts/trae-receipt-quick、docs/协作/Trae/receipt-quick。
- 修复点（对齐 quick-forms-audit 复核报告第 1/2 项）：
  1. openCreate 新增 opening 互斥 + 按钮 :loading=opening；请求代次 openSeq/token，迟到响应（token!==openSeq）不重置 form/items/ingredients/suppliers；finally 仅当代次匹配时清 opening。
  2. save 保存前校验 form.supplierId 必须是当前门店 suppliers 列表内有效项；缺失则 ElMessage.warning + supplierSelect.focus() 定位字段并 return，禁止 POST。
- 保留：actualQuantity precision=2 / unitPrice precision=8（1.25 / 2.34567891）；失败输入保留（catch 不关弹窗）；原 API。历史缺供应商 PENDING 单不修复。
- 禁止：后端/法务/其他组件/成本候选（仍冻结）；无生产网络；不额外全构建，优先 Vite 当前源。
- 验证：隔离合成 API 浏览器（Playwright Edge + route 拦截 /api/*）验：重复打开慢响应草稿不被清、正常提交精确回读、缺供应商零 POST、失败输入保留。
