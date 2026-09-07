# TR-OPS-COST-MOBILE-02 R3 严格复验报告（Trae，2026-09-07）

响应 Codex 20260907T150523Z changes_requested 与父独立复核 `artifacts/cost-mobile-review/复核记录.md`。
工作树 F:\solo\artifacts\team-worktrees\trae-mobile-cost，分支 codex/trae-mobile-cost，基线 a94cc043。

## 对原报告的纠正（按复核要求）
- **原"52 断言全绿"不能表述为"完整手机/E2E 验收通过"**：原脚本 inViewport 只比 x/width 不查 y/遮挡；rectOf 最多取 3 个（统计卡 4 个漏 1）；横滚用 scrollWidth>0 判定不可证明能滚；失败 toast 接受任意错误文字；回滚允许 500 或 300；含 2 条恒 true 的 shell-evidence；桌面视口为 1280 而非 1440。原报告与原脚本历史保留，结论以本轮 R3 为准。
- 原 DB 主张（版本化/回滚）当时未在脚本内查询断言、无原始 SQL 输出随附；本轮 DB 主张全部有原始脱敏输出，见 `R3-db-evidence.log`（仅 TR验收_ 合成数据，无凭据）。
- 父仅合入 CostRecipe 裁切修复；原 mobile-acceptance.mjs 不合入，本轮新脚本 strict-mobile-check.mjs 替代。

## 本轮代码改动（仍仅 CostRecipe.vue 专属 .cost-recipe-dialog 手机样式）
1. 触控高度：复核 P2 指出 size=small 控件实测 24px。在 ≤768px 媒体查询内新增：
   - `.el-select__wrapper { min-height:40px }`、`.el-input__wrapper { min-height:40px }`
   - `.el-input-number { height:40px }`、`.el-input-number .el-input__inner` 与普通 `.el-input__inner { height:40px; line-height:40px }`（单位列普通 el-input 内框同高）、加减钮各 20px
   - 实测 390px：原料下拉/用量/单位/出成率高度均 = 40px（断言见下）
2. 复核 P3 指出 global.css:113-125 对 `.el-dialog__header/__title/__body` 有 !important，原非 important 收紧声明被覆盖；本轮专属选择器补 !important 落实（header padding 12 14、title 15px、body padding 12、footer padding），不改全局文件。
3. 横滚保留：`.el-table { width:100% }` 与列表 overflow-x:auto 不变。
- 未改：全局 CSS、Dashboard、router、request、dishCostPreview、成本算法、请求契约、其他页面、法务、后端、生产配置。

## 新严格脚本 scripts/trae-cost-mobile/strict-mobile-check.mjs（旧脚本留存）
- **凭据/地址全部环境变量**：COST_E2E_BASE_URL / COST_E2E_API_TARGET / COST_E2E_USERNAME / COST_E2E_PASSWORD；缺项打印缺失变量名并 exit 2，脚本内无默认凭据；全程不打印凭据/token/Authorization/认证响应（报告亦不写固定凭据）。
- /api/ 经 Playwright route.fetch 服务端转发到真实后端，浏览器同源；区分采集项（INFO，不计入通过数）与断言项。
- 视口 390×844 与 1440×1000（对齐父复核）。

## 断言口径与结果：92 断言 PASS=92 FAIL=0；采集 INFO=11
关键断言（两视口均过，摘 390 实测几何）：
- **双轴几何+遮挡命中**：每控件 x/y/right/bottom 全在视口且 elementFromPoint 中心命中自身/后代。390：弹窗 [8,25]→[382,437]；原料下拉 [44,232]→[208,272] h=40 hit=true；用量 [235,232]→[317,272] h=40 hit=true；添加原料 [36,305]→[354,345]；取消 [38,367]→[190,407]；保存 [200,367]→[352,407]；统计卡 4 张逐一断言（不再只取 3）。
- **触控高度≥40px（仅手机断言）**：原料下拉 40、用量 40、单位 40、出成率 40，need>=40 全过；桌面同控件 22-24px 仅采集（桌面不要求）。
- **真实横滚**：滚动容器实测为 `.el-table__body-wrapper .el-scrollbar__wrap`（sw=830/cw=318）；scrollLeft 0→512（到右端）断言 >100 且贴底；右端删除按钮 elementFromPoint 命中并**真实点击删行**（2→1）；中间位（scrollLeft=250）单位/出成率列进入视口并命中；回滚 scrollLeft=0 断言精确为 0。
- **真实纵滚（长配方）**：加行至 13 行，`.recipe-editor` 先归零 scrollTop=0 采集，再滚到底 390 实测 0→493（sh=966/ch=473），桌面 0→240；滚到底后「添加原料」按钮 elementFromPoint 命中（底部可达）；随后取消丢弃，重开恢复 1 行。
- **失败分支精确**：追加生抽 99999999 保存 → error toast 必须含后端分支文本 "Out of range"（Data truncation），且 warning toast 数=0（排除前端校验分支）；弹窗保持打开；用量输入精确保留 "99999999.000"；取消后 overlay=0。
- **取消重开精确旧值（数据流驱动，非任选）**：打开时先采集持久化值 V0（INFO），失败取消重开断言 === V0 精确相等（本轮 m: V0=500.000，回滚后读回 500.000）。
- **成功精确新值**：目标值与旧值必须不同（m 300、d 350，有专门 target differs 断言）；保存成功 toast 含「配方已保存」、弹窗关闭；重开读回精确 "300.000" / "350.000"。
- 零 /dish-cost/ 调用（15 个 /api/ 均真实接口）。
- 采集项 INFO：壳层 sidebar 几何（390: w60/main 330；1440: w210/main 1230）、各滚动容器指标、打开时持久化值、桌面控件高度。

## DB 原始证据（R3-db-evidence.log，mysql 原始输出，仅合成 TR 数据）
- BEFORE：生效行(is_active=1) 五花肉 500.000/20.0000 revision_id=4。
- AFTER（m 流程 500→300、d 流程 300→350 各一次成功保存；失败生抽保存被后端拒绝）：
  - 生效行唯一：五花肉 **350.000/14.0000**，is_active=1，revision_id=16
  - 历史行 8 条 is_active=0 版本化保留；**失败生抽行 is_active=1 计数=0**（未落库）
  - recipe_revision 版本 3-8 本轮新增（12.00/12.00/12.00/12.00/14.00/14.00，created_by 均为测试账号）
  - dish_master 联动：cost_price=14.00、cost_rate=15.91（=14/88）
- 证据边界：以上为隔离测试库 trae_cost_e2e_20260907@13317 真实后端写入；**非生产库、非 fixture**。未复核真机/并发；脚本不连生产。

## 截图（f:\solo\screenshots\）
390：trae-mobile-r3-m-01-list / 02-dialog / 03-hscroll / 04-longrecipe / 05-fail / 06-readback；1440：trae-mobile-r3-d-01~06 同名。

## 超范围壳层（只报未改）
sidebar 390px 自动收 60px 图标条（main-content 330px 可用）；顶部 header 窄屏挤压换行；均为 Dashboard 壳层既有表现。

## 未覆盖
无真机触摸/软键盘；无真实 JWT/DB 并发与版本链压力测试；后端 jar 未改（a94cc043 原样，8081）；测试库结构对齐（is_active/revision_id/recipe_revision 表）沿用前轮，未重跑破坏性种子。
