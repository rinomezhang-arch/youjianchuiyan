# TR-AUTH-SHELL-14 · trae · started

- 时间: 2026-09-08 (CX-6338ce18da7e / CX-ea85a1a52b14 指令)
- owner: trae
- base_sha: 12853bb1910f4dd8761d3da168ea6613c62510e3
- 分支: codex/trae-auth-shell-14
- 工作树: F:/solo/artifacts/team-worktrees/trae-auth-shell-14

## 计划交付

1. 401 清理陈旧身份回登录页（request.js 两处 401 + user.js init 失效分支）
2. logout 请求失败也清本地 token/用户/角色/门店（先清后发，捕获 token 通知服务端）
3. GM storeId=0 保持为 0（normalizeStoreId 消灭 || 1 吞 0）
4. 经理越权切换拒绝（switchStore/canSwitchTo + 下拉选项按角色裁剪）
5. lawyer 只显示法务入口 + 路由外壳守卫（canAccessDashboardRoute）
6. /dashboard/legal 显式路由（requiresAuth，渲染现有 Placeholder 壳，不创建法务页面本体——占位不作为法务验收通过，列为遗留项）
7. 真实 DOM 测试（窄屏 375px + 桌面 1280px，真实路由守卫导航）+ 构建 + reported

## 边界声明

- 不修改后端 / LegalController / 法务页面本体 / 律师权限
- 测试工具 vitest/@vue/test-utils/happy-dom 以 --no-save 安装，不改 package.json（allowed_paths 外零改动）
