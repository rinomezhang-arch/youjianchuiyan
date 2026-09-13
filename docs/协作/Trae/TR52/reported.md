# TR-STOCKTAKE-DECIMAL-52 · reported（trae -> codex，R1 重新认领周期 2026-09-14）

## 结论
PASS（前端候选范围）。DL-49 三反例保持 3/3 通过；专属 Node 测试 **32 passed / 0 failed / 0 skip**（R0 的 22 例 + 本周期新增 10 例）。
按卡片未做全量 vite 构建（统筹合并后只构建一次）；SFC 用项目自带 @vue/compiler-sfc 做了 parse+compile 轻量自检通过（非构建）。未改后端/接口/依赖/配置，未碰生产/数据库/法务/凭证，未删文件，未碰他人工作树。

## 工作树与基线（含旧周期处置，如实说明）
- base：`71cd01d6b9e778213dbcf6dbf13470497549ba54`（卡片固定）。
- 工作树：`F:/solo/artifacts/team-worktrees/trae-stocktake-money-52`，分支 `codex/trae-stocktake-money-52`（卡片 inputs 指定路径，merge-base 核对 = 71cd01d6）。
- 该工作树与分支在 2026-09-09 旧协作周期已存在：`50fb5a9e`（BigInt HALF_UP 修复+22 测试）、`008be963`（旧 reported 回填 SHA），远端无分支；工作树内容干净（仅 autocrlf 的 CRLF 标记，归一化后 diff 为空）。
- 因 forbidden 禁止删文件/改他人工作树，未删除重建；本周期在其上增量提交 `750a7cd02e2b0acc88fcafdc0bd2032dcd5d005f`，补齐今天卡片新增的“错误输入 UI 提示+阻止无效提交”。
- 旁证：`codex-stocktake-money-53`（87d215a3）是 Codex 自己的并行修复，本任务未触碰。

## 问题（已自证，未重新探索）
`StockTake.vue` 旧 `updateDiff` 用 `Number((diffQty*unitPrice).toFixed(2))`：`1 x 0.015` 在 float64 为 0.0149999... -> "0.01"，后端 BigDecimal HALF_UP 为 0.02；负数同错（-0.02 被算成 -0.01）。DL49 proof：原 3 例 1 过 2 败。

## 修复方案
### R0（50fb5a9e，保留）
新建 `frontend_v3/src/utils/stockTakeMoney.js`（BigInt 定点、无依赖）：
- 数量 scale 3（10^3），单价 scale 8（10^8），行金额 = 差量(3) x 单价(8) -> HALF_UP 到分(scale 2)，合计 = 逐行整数分 BigInt 累加后只转一次 Number；全程不先 Number 相乘再 toFixed。
- `parseFixed`：字符串/数字/科学计数法（含 1e-8）-> BigInt 定点；null/空/非法 -> null，不伪装 0。
- `roundHalfUp`：半值远离零，与 Java RoundingMode.HALF_UP 一致。

### 本周期增量（750a7cd0）
卡片新要求“错误输入；未知不能伪装 0，UI 提示阻止无效提交；展示/确认/导出一致”，旧实现只判 `== null`，NaN/不可解析/超精度输入会穿过校验进提交载荷。补齐：
- stockTakeMoney.js：
  - 导出 `QTY_SCALE/PRICE_SCALE/AMOUNT_SCALE` 合同常量（尺度单一来源）。
  - 新增 `classifyFixed(value, scale)` -> `empty` / `invalid` / `ok`：经精确十进制结构检查（不是 parseFixed 的超精度截断），NaN/Infinity/abc/1.2.3/双符号/残缺科学计数法与超出合同尺度的小数位均判 invalid；科学计数法按有效位判定（scale 8 下 1e-8 合法、10e-9=1e-8 合法、5e-9 非法）。
  - `computeRowDiff` 返回新增 `invalid` 标志：空 -> 全 null 且 invalid=false；非法 -> 全 null 且 invalid=true；合法 -> 正常结果 invalid=false。
- StockTake.vue（仍仅两前端文件之一）：
  - `updateDiff` 用三态分类：非法行置 `_qtyInvalid`，金额字段全 null（不伪装 0）。
  - 行内 UI：实盘列非法时控件红色描边 + 下方红字“数量无效（最多 3 位小数）”；差异列显示红色“数量无效”；汇总条显示“有 N 项数量无效，请修正后再提交”；提交按钮在存在非法行时禁用。
  - `submitStockTake`：以输入值（非可能过期的行标志）权威三态校验，先拦非法、再拦未填，均阻止提交；通过校验才构造 payload，NaN/非法值不可能进入请求。
  - 一致性：表格行金额、确认框合计（sumCents 整数分）、CSV 导出（非法/空行金额留空而非 0）共用同一组计算字段；接口路径、payload 形状、后端均未改。

## 验证（仅专属 Node 测试，无全量构建）
命令：`node scripts/stocktake-money-52/test-rounding.mjs`（纯函数，无浏览器/API/DB）

| 类别 | 覆盖 | 结果 |
|------|------|------|
| DL-49 原 3 反例 | 1x0.015=0.02；-1x0.015=-0.02；1x0.335=0.34 | 3/3 PASS |
| 三位数量 | 8.123-10.001=-1.878；x0.05706667 -> -0.11 | PASS |
| 八位单价 | parseFixed(0.05706667,8)=5706667n；2x0.05706667=0.11 | PASS |
| 1e-8 | scale8 解析=1n；1e8 x 1e-8=1.00；1 x 1e-8=0.00 | PASS |
| 正负半分 | 0.005->0.01、-0.005->-0.01、0.025->0.03、-0.025->-0.03（远离零） | PASS |
| 行合计 | 3x0.335 -> 1.02；0.02-0.02+0.02 -> 0.02（整数分累加） | PASS |
| 空/零/null | null/空串 -> 全 null；零差 -> 0/0.00；全空行合计 0 | PASS |
| 三态分类（新 10 例） | empty 集合；NaN/Infinity/abc/1.2.3/--1/./1e/1e-x 非法；数量 4 位小数非法；单价 9 位小数非法；5e-9 非法、1e-8 与 10e-9 合法；1.234/0.015/.5/0/1e2 合法 | PASS |
| 非法行契约 | abc/NaN -> invalid=true 且金额全 null；11.1234 -> invalid（不静默截断）；空/合法 invalid=false；非法行不进合计 | PASS |

汇总：**32 passed / 0 failed**（证据 `docs/协作/Trae/TR52/evidence/test-run-r1.log`，末行记录 commit_sha=750a7cd0）。
SFC 自检：`node scripts/stocktake-money-52/check-sfc.mjs` -> SFC_OK（借用主仓库 F:/solo/frontend_v3 的 @vue/compiler-sfc 只读解析，未安装依赖；证据 evidence/sfc-check-r1.log）。

## 源码 SHA（750a7cd0 时）
- 分支 HEAD：`750a7cd02e2b0acc88fcafdc0bd2032dcd5d005f`（base 71cd01d6；父 008be963）
- stockTakeMoney.js blob（git hash-object）：`bda2d94197cf9a1078331979351656b0f936af4a`
- StockTake.vue blob：`38814594b0daa6597280e62072eae24c127ec892`
- test-rounding.mjs blob：`50a56b669e317f7a4b4a5a200e3a677d931facbf`
- R0 修复提交（保留）：`50fb5a9ef55e3e3477174666ee38c95320811a57`

## 文件清单（全部在 allowed_paths）
- `frontend_v3/src/utils/stockTakeMoney.js`（R0 新建；本周期 +classifyFixed/尺度导出/invalid 标志）
- `frontend_v3/src/views/dashboard/StockTake.vue`（R0 三处接入；本周期三态校验/行内提示/提交拦截）
- `scripts/stocktake-money-52/test-rounding.mjs`（R0 22 例；本周期 32 例）
- `scripts/stocktake-money-52/check-sfc.mjs`（本周期新增，SFC 轻量自检）
- `docs/协作/Trae/TR52/`：board-started.txt、reported.md、evidence/（test-run-r1.log、sfc-check-r1.log）

## NOT_COVERED / 边界
- 真实后端联调/数据库/浏览器像素走查：NOT_COVERED（本任务只要求纯函数 Node 验证）；金额合同以后端 StockTakeController BigDecimal HALF_UP 为准。
- el-input-number 控件层本身拒绝非法文本与负数（:precision=3/:min=0）；新增的三态校验是数据层兜底，覆盖编程式赋值/粘贴/JSON 异常值等绕过控件的路径。
- 全量 vite 构建按卡片不做，由统筹合并后只构建一次。
- 未启动/重启任何后端服务；未在旧 TR37 或其他工作树改源码。

## 下一步
请 Codex 复核 750a7cd0（在 50fb5a9e 数值修复之上的错误输入拦截增量）；reviewed 后由统筹合并并统一构建一次。
