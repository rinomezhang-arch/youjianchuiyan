# 又见炊烟私房菜 · 餐饮管理系统 — 开发过程

## 时间线

### Day 1 — 2026-07-22（部署+API+分析）

| 时间 | 事件 |
|------|------|
| 12:02 | 系统心跳正常 |
| 13:00-14:00 | 天地双龙首次协同部署：前端dist+Nginx+JAR重启 |
| 13:30 | 发现前端白屏：assets路径不对 → ln -s修复 |
| 14:00-18:00 | 三龙体系建立：9大铁律、COS基础设施、聊天室 |
| 17:40 | 秋哥指出现场版本不对 |
| 17:44-17:51 | 推送铁律强化事件（秋哥三次强调） |
| 17:50-18:05 | API大作战：Booking/Customer/Package/Inventory 4模块 |
| 18:37 | Dashboard模块完成 → 8 Controller |
| 19:08-19:20 | 404根因大揭秘：表名错误+路径复数vs单数 |
| 19:21-19:31 | Staff模块重写 → 9/9全绿 |
| 19:28-19:40 | StoreController + HRController → 11/11 |
| 19:45-19:55 | 白屏根因：无login API + 响应格式 → AuthController+ResponseWrapper → 12/12 |
| 19:57-20:09 | Recipes/DashboardReport/MenuApi/Upload + MenuApi 500修复 + BookingListExt → 17/17 |
| 20:14-20:17 | 单页系统初步分析 |
| 20:17-20:27 | 秋哥要求深度分析 → 104函数/163ID/331CSS完整拆解 → 382行工程计划 |
| 20:27-20:37 | P0后端全部交付：bookingdish/bookingext/customerext/dashboard2(共404行4个Controller) |
| 20:37-20:46 | 双龙全死→全活：地龙会话文件锁+天龙gateway重启 |
| 20:46 | 秋哥指示：不管SOLO，任务发地龙 |
| 20:49 | COS项目管理文件夹建立 |

### 阶段性成果
- **Controller**: 从0→17→25，一天写完
- **API端点**: 25个全部HTTP 200
- **代码量**: 50个Java文件 ~3600行
- **数据库**: banquet库30张表 + 新booking_dish表

## 关键决策
1. ResponseWrapper统一响应格式 `{code:200, data:...}`
2. MySQL ID统一VARCHAR(20)而非Long
3. Spring Security禁用，自建AuthController
4. 双路径策略 `/api/*` + `/menu-api/*`
5. 分工明确：🦞写代码→COS / 🐉Maven编译→SCP / 🐚验收

## 踩过的坑
1. 表名写错(staff→staff_master) → 404
2. 路径单复数(/booking→/bookings) → 404
3. MenuApi列名猜错(is_active/phone/position) → 500
4. 地龙说部署好但文件位置不对 → 白屏
5. SOLO换COS文件名没通知 → 空等8分钟

---
🦞 整理于 2026-07-22
