# RC15 最终整体复验 — 真实浏览器回归证据（2026-09-09）

环境：http://127.0.0.1:5183（serve-rc15-web.mjs 读 dist 磁盘文件 + /api 反代 127.0.0.1:18080 全 Spring 栈，隔离库 13317 banquet_rc15）。
构建：业务 HEAD 7c782b86，jar SHA256 95de8d5e…b8cb30；dist 2813 modules（含 CL22 视觉包）。
合成账号（均为 synpass123）：syn_stayer=staff_id 404 / role manager / store_id 1「合成一店」；syn_demoted=staff_id 402 / role lawyer / store_id 1。

## 结果总览：7/7 PASS，0 FAIL（无白屏/致命 JS 错误阻断）

| # | 步骤 | 结果 | 证据 |
|---|------|------|------|
| 1 | syn_stayer + 错误密码 | PASS | POST /api/auth/login → HTTP 200 包裹 code=401，body `账号或密码不正确`（代理修复后带 Origin 复测 0.19s）；页面停留 /login。文案链：HTTP body + Login.vue:151 `ElMessage.error(res.message)`；网络面板见 POST 后整页回 /login（ERR_ABORTED 为同页刷新取消，非缺陷）。截图 01 |
| 2 | syn_stayer 正确登录 | PASS | 跳转 /dashboard/home，「总经理总驾驶舱」完整渲染，顶栏「合成在职员工 manager」；token 落 localStorage。截图 02。注：驾驶舱门店筛选器「旗舰店/分店A/分店B」与「宁国店」为 Home.vue:384-385 / Admin.vue:122 历史静态展示文案（非候选改动）；门店数据隔离由服务端 StoreDataScopeAspect 保证（登录链已验），DB store_id=1=「合成一店」由登录响应 storeName 返回 |
| 3 | 整页刷新 F5 | PASS | URL 仍 /dashboard/home，token 保持，驾驶舱重渲染，未掉登录页 |
| 4 | 侧边栏业务页 | PASS | 直链 /dashboard/table-board（桌台看板）正常渲染：日期/餐段/状态筛选、空闲 0 / 预订 0（隔离库空数据态正常）。截图 03 |
| 5 | 退出 + 未登录守卫 | PASS | 头像菜单「退出登录」→ 确认弹窗「确认·Confirm」→ 回 /login 且 token 清除；随后直访 /dashboard/home 被拦至 /login?redirect=/dashboard/home。截图 04 |
| 6 | syn_demoted(lawyer) 登录 + 法务入口 | PASS | SPA 登录后角色守卫整页送 /case/（router/index.js:223-225，不进管理外壳）；/case/ 独立门禁输入 syn_demoted/synpass123 → /api/legal/me 200，案卷 shell 展开，whoami「syn_demoted（代理律师）」，DOM 478KB 含完整案卷/法条（含「打印全卷/退出」）。截图 05、07 |
| 7 | lawyer 管理壳拒绝 + 法务退出 | PASS | 登录态访问 /dashboard/home 被强制回 /case/（isCase=true，无管理外壳）；/case/「退出」→ 门禁重现、shellWrap 隐藏、sessionStorage legalTok 清空。截图 06 |

## 过程事件与定性（均非候选业务缺陷）

1. **测试代理 CORS 缺陷已修**：浏览器跨源头 Origin: http://127.0.0.1:5183 被后端生产域名白名单拒绝（403 "Invalid CORS request"）。根因是 serve-rc15-web.mjs 透传浏览器 Origin；已在代理转发时改写 Origin 为生产域名并处理（模拟生产 nginx 同源转发；浏览器侧 5183 同源不做 CORS 校验）。修复后 OPTIONS 预检 200、错误密码 200/code401。
2. **共享 MySQL 13317 运行中崩溃（XA crash recovery）**：复验中途 mysqld 进程消失（数据目录 F:/solo/artifacts/mysql-test-13317 完好），导致请求 15s 超时与页面 timeout 文案。以**同一数据目录**重启 mysqld 8.4.9（--innodb-buffer-pool-size=128M，非重建、非删库）；恢复后 banquet_rc15 87 表、co_print23_20260909_022305 库完好；后端 Hikari 自动重连，登录 0.19s。
3. **终验发现发布脚本迁移缺口（已补，见 release-manifest prod_readonly_diff.deploy_migration_gap）**：浏览器控制台出现 `Unknown column 'dr1_0.is_active'`（dish_recipe 版本化 c0b3c04b）。生产库只读取证：dish_recipe 缺 is_active/revision_id、recipe_revision/ipad_batch_request/restaurant_print_printer/restaurant_print_rule 表缺。隔离库应用 scripts/migrations/recipe_revision_v1.sql + restaurant_print_config_v1.sql（均退出码 0）后，原失败接口 /api/recipes/dishes-with-recipe、/api/recipes/{id} 复返 HTTP 200；deploy-rc15.sh 已补 1c（配方列/表，information_schema 幂等守卫+ABORT 校验）与 1d（打印配置表，脚本自身幂等），git-bash `bash -n` 双脚本 exit 0。
4. 控制台噪声（非阻断）：WebSocket onerror（隔离环境无通知 WS 端点）、@vite/client 404（IDE 预览注入）、ERR_ABORTED（整页跳转取消）。
5. /api/public/stores 恢复后稳定 200（隔离库 data:[]）；先前超时系 OOM 期资源饥饿。

截图：本目录 rc15-browser-01..07.png。
