# CL-OPS-INQUIRY-H5-UI-08 · started

领取人：Claude（claude-code/solo）。`CLAIM_VERIFIED`。统筹：Codex。
工作树 `F:/solo/artifacts/team-worktrees/claude-ipad-account-flow`，
分支 `codex/claude-ipad-account-flow`，起始 HEAD `83c5f7ab`（与 `base_sha` 一致）。
板上无其他执行端事件。**先登记再修改。**

## 责任边界

允许：`StoreDetail.vue`、`MobileBook.vue`、`Bookings.vue`、`api/**`、`router/**`、
`tests/**`、`docs/协作/Claude/inquiry-h5-ui-08/**`。

**明确不碰**：`frontend_v3/src/views/ipad/home/BookingList.vue`（Trae 负责）、
法务代码与数据、生产数据与部署、核心配置。

## 要做的两件

### 一、公开 H5：手机号 + 预订单号 查状态

对接第二轮后端契约 `POST /api/public/booking-lookup`，请求体 `{ phone, bookingId }`。

**前端必须与后端同口径**：查无、手机号不符、别人的单、别店的单、缺参数、格式非法，
**一律显示同一句提示**，不做任何区分。后端已经把这些统一成同一种空结果，
前端若按不同情况分开提示，等于把后端堵住的枚举口子在前端重新打开。

只展示后端白名单里的六个字段，不展示金额与备注（后端本来也不返回）。

### 二、员工端：咨询列表 + 明确日期时间桌台后转正式预订

对接 `POST /api/booking-inquiries/{id}/convert`，请求体 `{ bookingDate, bookingTime, tableIds }`。

- 三者**必填**，缺一不可——**界面不做任何"自动选桌"**，与后端同口径。
- 成功后显示正式预订单号。
- **重复点击返回同一单号**（后端 `replayed: true`），界面要如实显示"该咨询此前已转过"，
  而不是伪装成一次新成功。

## 验证

1. 真实浏览器端到端：公开查询成功与各种错误组合、员工转单、跨店桌台拒绝、重复确认只生成一单。
2. 响应式：**手机 390 宽与桌面 1280 宽都不得横向溢出**，关键控件可操作。
3. 生产构建通过。
4. **不用组件挂载或 HTTP 200 冒充业务验收**——这是任务明令禁止的，也是本项目的长期纪律。

## 明确不做

不改 Trae 的 `BookingList.vue`；不碰法务、生产、配置；不新增按手机号列表查询的入口。
