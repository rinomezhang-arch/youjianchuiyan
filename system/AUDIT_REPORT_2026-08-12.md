# 全系统深度可用性审计报告

审计日期：2026-08-12

## 结论摘要

- 管理端真实账号登录成功，最终落点为 `/dashboard/home`，JWT、门店和角色信息均已写入浏览器。
- 管理端定义约 100 个子路由；自动导航覆盖 97 个无参数实际页面，应用外壳均可加载，但“页面可打开”不等同于业务可用。
- 前端生产构建通过（2664 modules transformed）；后端 Maven 构建通过，但项目没有自动化测试（`No tests to run`）。
- 后端 `/actuator/health` 为 `DOWN`：数据库可用，但 RabbitMQ 未运行，健康检查报 `Connection refused`。
- iPad 公共入口和门店选择可打开；真实 iPad 登录请求 `/api/ipad/login` 返回 401，无法继续核验受保护业务链路。
- 浏览器存在系统性中文乱码，数据库返回的门店名和用户名均显示为 mojibake；这是当前最显著的全局 UI 缺陷。

## 严重问题

### P1-01 iPad 真实登录不可用

- 路由：`/ipad/login`
- 复现：选择“宁国店”，使用可成功登录管理端的验收账号提交。
- 证据：`POST /api/ipad/login` 返回 401，页面停留在登录页。
- 影响：`/ipad/home`、`/ipad/bookings`、`/ipad/wait`、点餐、账单、支付、历史、会员、设置均被真实认证阻塞。
- 根因线索：iPad 使用独立认证与设备绑定，管理端账号不能直接满足该链路；生产模式下 `fallbackOrThrow` 不会启用演示会话。

### P1-02 多个正式路由实际为未完成功能

- 明确 Placeholder：`/dashboard/category-sort`、`/dashboard/menu-sort`。
- 仅 13 行空态壳：`/dashboard/art-design`、`/dashboard/floor-project`、`/dashboard/menu-festive`、`/dashboard/production`、`/dashboard/table-layout`。
- 证据：这些组件只渲染“暂无内容/功能开发中”，没有读取或写入业务数据。
- 建议：从正式菜单下线或实现后端驱动的列表、详情和状态流转，不能继续标记为可用页面。

### P1-03 全局 AI 模型请求未授权

- 证据：管理端导航期间重复出现 `GET /api/ai/models` 401；此前日志同时出现 `Unexpected end of JSON input`。
- 影响：全局 AI 浮层在大量页面加载时产生失败请求，AI 助手无法可靠工作。
- 根因线索：`AIChatFloat.vue` 的模型请求没有复用带 JWT 的 Axios 实例，并直接解析非成功响应。

### P1-04 用户会话恢复字段结构错误

- 路由：所有管理端页面硬刷新后。
- 证据：首次登录显示“预览管理员/admin”，随后重新导航后顶栏退化为 `User/U`。
- 根因：`store/user.js:init()` 将 `/auth/me` 的整个 `res.data` 写入 `userInfo`；后端实际返回 `{ user, storeId, storeName }`，组件期待 `userInfo.staffName`。
- 修复：写入 `res.data.user`，并同步 `storeId/storeName`。

## 中等问题

### P2-01 全局中文乱码

- 影响区域：登录页门店名、管理端门店名、登录用户姓名，截图中大量中文成为 `å®å›½...`。
- 证据：数据库和后端日志已返回乱码文本，不是浏览器字体缺失。
- 根因线索：历史数据以错误编码写入，或数据库连接/导入过程发生 UTF-8 与 latin1 二次编码。
- 建议：先备份数据库，识别乱码列后做一次性数据修复；继续保留 JDBC `characterEncoding=utf-8` 并核验库表/连接均为 utf8mb4。

### P2-02 健康检查长期为 DOWN

- 证据：RabbitMQ `localhost:5672` 连接被拒绝。
- 影响：部署健康判断失败；依赖消息队列的通知类业务不可用。
- 建议：预览环境启动 RabbitMQ，或在明确禁用通知的环境排除 Rabbit 健康指标，不能让服务处于假健康状态。

### P2-03 测试覆盖为空

- 证据：Maven 输出 `No tests to run`。
- 影响：约 100 个管理端路由和 17 个 iPad 路由没有后端回归保护。
- 建议：优先补认证、预订、桌台、菜单保存、库存出入库与 iPad 登录的集成测试。

### P2-04 路由定义存在重复和错误归类

- `/dashboard/packages` 重复定义两次。
- 设置页子路由在静态抽取时容易被误判为 `/dashboard/info` 等，实际规范路径应为 `/dashboard/settings/info` 等。
- 建议：去重并为路由生成唯一、可机器校验的清单。

## 安全与工程风险

- JWT 存储在 `localStorage`，一旦出现 XSS 可被直接读取；建议改为 Secure、HttpOnly、SameSite Cookie 会话。
- `AuthController` 仍兼容明文密码，当前验收账号密码字段也是明文格式；应迁移为 BCrypt 并删除明文兼容分支。
- `Placeholder.vue` 和部分业务页面使用 emoji 作为图标，视觉与可访问性不一致。
- 前端同时存在根 `pnpm-lock.yaml`、子目录 `pnpm-lock.yaml` 和 `package-lock.json`，应统一包管理边界，避免依赖漂移。

## 路由矩阵

### 管理端：已真实登录并自动导航

以下页面状态为“外壳可加载/需按上述问题降级”，不是所有写操作均已通过：

- 核心：home、table-board、bookings、banquet-notices、front-office、front-desk、kitchen、kitchen-log。
- 菜单：menu、menu-banquet、menu-alacarte、menu-soldout、menu-full、menu-detail、menu-manager、ordering、set-menu、set-menu-edit、dish-library、cost-recipe、pricing-manage、soldout-control、tags、print-config、price-tiers。
- 供应链：supply-chain、inventory、procurement、receipt、issue、supplier-reconciliation、stock-take、suppliers。
- 财务：finance、finance/dish-cost、finance/cost-analysis、dish-cost-analysis、bill-manage、reports、report-print、export-panel。
- 人事：hr-admin、staff、training、license、security、assets、attendance、schedule、leave、overtime、attendance-calendar、staff-profile、payroll、self-service、review-queue、hr-analytics、attendance-print、approval。
- 运营分析：customers、marketing、member-list、data-screen、guest-analysis、staff-performance、table-utilization。
- 工程：engineering、decoration、energy、safety、system-checkup。
- 系统：settings/info、settings/permission、settings/org、settings/config、settings/help、settings/checkup、help、help/dev-process、change-logs、perm-manager、store-permission、audit-log、dict-manager、admin、store-org、gm-office、ipad-menu、welcome、ai-assistant。
- 未实现：category-sort、menu-sort、art-design、floor-project、menu-festive、production、table-layout。

### iPad

- 可用：`/ipad/store`、`/ipad/login`（仅页面和门店选择）。
- 认证阻塞：`/ipad/home`、`/ipad/bookings`、`/ipad/wait`、`/ipad/order`、`/ipad/dishes`、`/ipad/dish/:dishId`、`/ipad/packages`、`/ipad/cart`、`/ipad/bill/:bookingId`、`/ipad/pay/:bookingId`、`/ipad/history`、`/ipad/member`、`/ipad/settings`。
- 未做业务可用性背书：`/ipad/guest-order/:bookingId` 需要有效 bookingId，当前没有可安全使用的验收预订。

## 验证证据

- 登录成功截图：`/tmp/agent-browser/audit-dashboard-login.png`
- 管理端预订页截图：`/tmp/agent-browser/audit-bookings.png`
- iPad 门店页截图：`/tmp/agent-browser/audit-ipad-store.png`
- iPad 登录页截图：`/tmp/agent-browser/audit-ipad-login.png`
- 前端生产构建：通过。
- 后端 Maven 测试阶段：构建通过、0 测试。
- 数据库：MariaDB 可连接，`youjian_banquet_v2` 共 153 张表。

## 建议修复顺序

1. 修复 iPad 认证/设备绑定验收链路，并提供专用 iPad 验收账号。
2. 修复数据库中文乱码与会话恢复字段错误。
3. 修复全局 AI 请求鉴权和错误响应解析。
4. 启动或正确禁用 RabbitMQ 健康检查。
5. 下线或实现 7 个未完成正式路由。
6. 增加核心业务集成测试，再开展安全的写入/回读验收。
