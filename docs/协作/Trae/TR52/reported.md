# TR-STOCKTAKE-DECIMAL-52 · reported（trae → codex）

## 结论
前端盘点预览舍入与后端 HALF_UP 一致：原 DL-49 三例从 1 通过 2 失败转为 **3 通过**；
Node 反例测试 **22 pass / 0 fail**。未改后端/接口/依赖，未碰生产/数据库/法务。

## 问题根因（已自证，未重新探索）
`StockTake.vue:150` `updateDiff` 用 `Number((diffQty * unitPrice).toFixed(2))` 浮点运算：
`1 × 0.015` 在 float64 中为 `0.0149999…`，`toFixed(2)` 得 `"0.01"`，而后端 `BigDecimal.setScale(2, HALF_UP)` 得 `0.02`；
负数同错：`-0.015` 浮点为 `-0.0149999…` → `"-0.01"`，后端 `-0.02`。

## 修复方案
新建 `frontend_v3/src/utils/stockTakeMoney.js`（BigInt 定点运算，无依赖）：
- 数量 scale 3（`10^3`），单价 scale 8（`10^8`）
- `parseFixed(value, scale)`：字符串/数字/科学计数法 → BigInt 定点；null/空/非法返回 null（不伪装 0）
- `roundHalfUp(value, fromScale, toScale)`：BigInt HALF_UP（半值远离零），与 Java `RoundingMode.HALF_UP` 一致
- `computeRowDiff(actual, system, price)`：BigInt 乘 `scale 3×8=11`，HALF_UP 到分 `scale 2`，返回 `{diffQty, diffAmount, diffAmountCents}`
- `sumCents(rows)`：Σ 逐行整数分（BigInt），转 Number 一次，无浮点累加漂移

`StockTake.vue` 改动（仅 3 处）：
1. L106：`import { computeRowDiff, sumCents } from '@/utils/stockTakeMoney'`
2. L133：`totalDiffAmount` computed → `sumCents(list.value)`（BigInt 累加替代浮点 reduce）
3. L151-159：`updateDiff` → 调 `computeRowDiff`，存 `row._diffAmountCents`（BigInt 分）；null/空→全 null

展示/确认/导出一致：`diffQty`/`diffAmount` 为 Number（量 3 位/额 2 位，库存范围内安全），模板 `>0`/`<0`/`toFixed(2)` 不变。

## 验证
Node 测试：`scripts/stocktake-money-52/test-rounding.mjs`（纯函数，无浏览器/API/DB）

| 类别 | 用例 | 期望 | 结果 |
|------|------|------|------|
| 原例#1 | 1×0.015 | 0.02 | 0.02 ✅ |
| 原例#2 | -1×0.015 | -0.02 | -0.02 ✅ |
| 原例#3 | 1×0.335 | 0.34 | 0.34 ✅ |
| 三位数量 | 8.123−10.001=−1.878 | diffQty=−1.878 | ✅ |
| 三位数量额 | −1.878×0.05706667 | −0.11 | −0.11 ✅ |
| 八位单价 | parseFixed(0.05706667,8) | 5706667n | ✅ |
| 1e-8 解析 | parseFixed(1e-8,8) | 1n | ✅ |
| 1e-8 非零 | 1e8×1e-8 | 1.00 | 1.00 ✅ |
| 正半分 | 0.005→0.01 / 0.025→0.03 | 远离零 | ✅ |
| 负半分 | −0.005→−0.01 / −0.025→−0.03 | 远离零 | ✅ |
| 行合计 | 3×0.335→0.34×3 | 1.02 | 1.02 ✅ |
| 空/非法 | null/空串→null（非0） | null | ✅ |
| roundHalfUp | ±15n scale3→2 | ±2n | ✅ |

**22 pass / 0 fail / 0 skip**

## 源码 SHA
- commit: 提交后回填
- base_sha: `71cd01d6b9e778213dbcf6dbf13470497549ba54`
- stockTakeMoney.js git-object: `867178947e5a8b0b89a0488b6ac971c0646e60c1`
- StockTake.vue git-object: `5d70d198ac43927189fc2d03a28882a9fdf78bfd`

## 文件清单
- `frontend_v3/src/utils/stockTakeMoney.js`（新建，BigInt 定点模块）
- `frontend_v3/src/views/dashboard/StockTake.vue`（改 3 处：import + totalDiffAmount + updateDiff）
- `scripts/stocktake-money-52/test-rounding.mjs`（Node 反例测试）
- `docs/协作/Trae/TR52/reported.md`（本报告）

## 边界
- 未改后端/接口/依赖；未做全量 vite 构建（由统筹合并后构建一次）；未碰生产/数据库/法务/凭证/配置；未删文件；未改他人工作树。
- 工作树：`trae-stocktake-money-52`（branch `codex/trae-stocktake-money-52`，from `71cd01d6`）。
