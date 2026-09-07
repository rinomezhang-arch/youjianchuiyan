# TR-RECEIPT-QUICK-01 验收报告（Trae，2026-09-07）

响应 Codex 下达的 TR-RECEIPT-QUICK-01，对齐 `artifacts/quick-forms-audit/复核报告.md` 第 1/2 项。
工作树 F:\solo\artifacts\team-worktrees\trae-mobile-cost，分支 codex/trae-mobile-cost，基线 93741a9。
父基线 9828ecdc 该文件 blob=19f8a8f7cdf2a69d0a447b62527ede5b2065f07c，现树动工前核对一致。

## 代码改动（仅 Receipt.vue，+13/-4）

1. **新增 opening 互斥 + 请求代次（防迟到清草稿）**
   - 新增 `opening=ref(false)`、`let openSeq=0`；新增按钮 `:loading="opening"`。
   - `openCreate`：入口 `if(opening.value) return`；`opening.value=true; const token=++openSeq`；`await Promise.all(...)` 后 `if(token!==openSeq) return`，只有当前有效打开动作能重置 `ingredients/suppliers/form/items` 并打开弹窗；`catch`/`finally` 均 `if(token===openSeq)` 才提示/清 opening。
2. **保存前强制当前门店有效供应商**
   - 供应商 `<el-select>` 加 `ref="supplierSelect"`；新增 `const supplierSelect=ref(null)`。
   - `save`：在明细校验前，`validSupplier = form.supplierId && suppliers.some(s => Number(s.supplierId)===Number(form.supplierId))`；不满足则 `ElMessage.warning('请选择当前门店的有效供应商'); supplierSelect.value?.focus(); return`，禁止 POST。
- 未改：actualQuantity precision=2 / unitPrice precision=8（1.25 / 2.34567891 精度保留）；失败输入保留（catch 不关弹窗）；原 API 路径与载荷结构；后端、法务、其他组件、成本候选（仍冻结）。
- 历史缺供应商 PENDING 单不在本次修复范围（需受限纠正契约或人工修复），不声称已修复历史单。

## 验收脚本：scripts/trae-receipt-quick/receipt-quick-check.mjs

- 启动当前源 Vite（5191 端口，`--strictPort`），Playwright Edge 通道，全量 `/api/*` 路由拦截合成响应，不连真实后端、无生产网络、无凭据。
- 合成数据：原料 五花肉(ING-1, 千克, 25.5)；供应商 鲜肉供应商(supplierId=1)；auth/me 返回测试验收员 storeId=1。
- 结果：**16 断言 PASS=16 FAIL=0，INFO=2**。

## 断言明细

| # | 场景 | 断言 | 结果 |
|---|------|------|------|
| 1 | 重复打开慢响应 | 双击只触发 1 次 GET /api/ingredients | PASS calls=1 |
| 2 | 重复打开慢响应 | 双击只触发 1 次 GET /api/suppliers | PASS calls=1 |
| 3 | 重复打开慢响应 | 慢打开后草稿数量 1.25 未被清 | PASS qty=1.25 |
| 4 | 正常提交 | POST 恰好 1 次 | PASS posts=1 |
| 5 | 正常提交 | POST receipt.supplierId=1（当前门店有效供应商） | PASS |
| 6 | 正常提交 | POST items[0].actualQuantity 精确 1.25 | PASS |
| 7 | 正常提交 | POST items[0].unitPrice 精确 2.34567891 | PASS |
| 8 | 正常提交 | 成功 toast「已保存待验收单」 | PASS |
| 9 | 正常提交 | 保存后弹窗关闭 | PASS overlays=0 |
| 10 | 缺供应商 | 零 POST（禁止提交） | PASS posts=0 |
| 11 | 缺供应商 | warning toast「请选择当前门店的有效供应商」 | PASS |
| 12 | 缺供应商 | 弹窗保持打开 | PASS overlays=1 |
| 13 | 失败保留 | 错误 toast（500 分支） | PASS |
| 14 | 失败保留 | 弹窗保持打开 | PASS overlays=1 |
| 15 | 失败保留 | 数量 1.25 保留 | PASS |
| 16 | 失败保留 | 单价 2.34567891 保留 | PASS |

## 边界与未覆盖
- 仅隔离合成 API 浏览器验收，非真实后端/DB/生产事务；不推断库存写入。
- 历史缺供应商 PENDING 单未修复；无真机/键盘全路径；未额外全构建（Vite 当前源验证通过）。
- 旧成本候选（93741a9 及之前）保持冻结，未触碰。
