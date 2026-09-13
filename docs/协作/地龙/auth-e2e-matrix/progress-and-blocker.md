# DL-AUTH-E2E-MATRIX-15 — 进展与卡点（地龙）

执行: 地龙 | 2026-09-13 17:35 +08:00 | owner=dilong
环境: 隔离 127.0.0.1:13318 (datadir=F:/solo/artifacts/mysql-test-13317/, schema=banquet_rc15)
      真实后端 18080 (banquet-1.0.0.jar, prod profile, 环境变量注入密钥)
      候选 dist 5183 (Http, SPA 回退 + /api/** 反代到 18080)

---

## 一、固定制品 SHA（可复验）

| 制品 | SHA256 |
|---|---|
| 提交 | `013afcf10c7400e80ca90e1148642c28d42fe9c4` |
| banquet-1.0.0.jar | `3B5C428DA3C9191AA48EB2F77447316A3919F4B362E2707E0B3FFE2B40BAD16F` |

测试脚本 SHA256（前16位）：

| 脚本 | SHA256[0:16] |
|---|---|
| matrix-http.mjs | `689E2DC89F8F2451` |
| matrix-browser.mjs | `3F070A3E649515B6` |
| seed-auth-matrix.mjs | `743EA8DC88CA79B6` |
| serve-dist.mjs | `B85EFE7B7F85BADF` |
| probe-logout-click.mjs | `E4E4161F7A9FA174` |
| probe-logout-dom.mjs | `3FD118F1EC2F1E5E` |
| probe-cors.mjs | `CA904AA642F4454A` |
| probe-login-deep.mjs | `0ECB65726166BF2C` |

---

## 二、已完成（六项退回中的 5 项）

1. **测试入口显式端口变量，固定 13318** —— 完成
   - `MX_DB_PORT` 必填；显式拒绝 `3306` / `13317`（生产/禁用端口）
2. **seed 移除 DELETE** —— 完成
   - 改为 `seed-auth-matrix.mjs`：每轮唯一账号号段（哈希派生），密码 `MX_PASS` 运行时注入，
     只做可追踪停用（resigned→active），**不物理删除任何行**
3. **iPad 授权矩阵** —— 完成，HTTP 层 14 项全 PASS
4. **律师 booking/customer 边界** —— 完成，新增 4 项拒绝断言
5. **密码/JWT 不落盘** —— 完成（运行时注入；证据文件脱敏，不含 token/password）

### HTTP 矩阵实测：51/51 PASS（真实后端 + 真实隔离库）

iPad 关键断言原始输出：
```
PASS  ipad.guard.missing-client-type.403        status=403
PASS  ipad.guard.unbound-device.401             status=401
PASS  ipad.guard.wrong-store.403                status=403
PASS  ipad.guard.missing-token.403              http=200 code=403
PASS  ipad.guard.self-reported-staff.400        status=200 code=400
PASS  ipad.auth.verify.issues-token             status=200 hasToken=true
PASS  ipad.submit.success                       http=200 code=200
PASS  ipad.audit.operator-from-verified-context operator=939740 expected=939740
PASS  ipad.guard.token-single-use               first=200 reuse=403
PASS  ipad.guard.wrong-booking.403              http=200 code=403
PASS  ipad.write.success-path-isolated          crossStore=0
PASS  ipad.idempotency.same-key-no-new-rows     before=5 after=5 retryCode=200
```

律师拒绝原始输出：
```
PASS  lawyer.deny.booking.list     status=403 code=403
PASS  lawyer.deny.booking.stats    status=403 code=403
PASS  lawyer.deny.booking.byId     status=403 code=403
PASS  lawyer.deny.booking.dishes   status=403 code=403
PASS  lawyer.deny.customer.list    status=403 code=403
PASS  lawyer.deny.customer.search  status=403 code=403
```

浏览器矩阵：7/8 PASS
```
PASS  browser.manager.login.enters-app        url=/dashboard/home
PASS  browser.manager.menu.present            bodyLen=890
PASS  browser.reload.stays-authenticated      url=/dashboard/home
FAIL  browser.logout.returns-login            clicked=true url=/dashboard/home
PASS  browser.lawyer.redirected-off-generic   url=/case/
PASS  browser.lawyer.lands-on-legal-entry     url=/case/
PASS  browser.lawyer.guard.blocks-dashboard   url=/case/
PASS  browser.lawyer.legal-status-only        bodyRead=false
```

---

## 三、卡点（1 项，未通过）

**browser.logout.returns-login**

### 已排除的环境坑（均非产品缺陷）
- SPA 路由 404 → 自写 SPA 回退服务
- `/api/**` 被静态服务吞掉 → 加反代到 18080
- `Invalid CORS request` 403 → 环境变量 `CORS_ALLOWED_ORIGINS` 加 5183

### 直接证据（probe-logout-click.mjs）
```
URL before logout : http://127.0.0.1:5183/dashboard/home
avatar count      : 1
item count        : 1   visible: true      ← 菜单项确实可见（已证明展开成功）
token before      : <present, redacted>
--- click ---
URL after click   : http://127.0.0.1:5183/dashboard/home   ← 未跳转
token after       : <present, redacted>                    ← 未清除（值已脱敏）
API during logout : []                                     ← 无任何请求发出
message-box       : {"found": false}                       ← 无确认弹窗
```

### 判定
- 后端 logout 契约在 HTTP 层已 PASS（`logout.ok code=200`）
- 前端本地清理由 TR-AUTH-SHELL-14 的 37 用例覆盖
- 菜单项可点（visible=true）但前端退出逻辑未触发，msedge headless 下无法确证是
  **产品缺陷**还是 **headless 交互限制**
- 按纪律：不夸大为通过，也不臆断为缺陷，如实上报待裁决

---

## 四、零写入状态（隔离库快照，只读查询）

| 表 | 行数 | 说明 |
|---|---|---|
| booking_dish_detail | 5 | 仅 iPad 成功路径 + 幂等重试写入（隔离库合成范围内） |
| ipad_batch_request | 5 | 仅授权提交收据 |
| finance_account | 4 | 只读断言用，前后未变（before=4 after=4） |
| staff_master | 17 | 6 个本轮合成账号 + 既有行，**无删除** |

- 未连接 13317；未杀/重启 MySQL；未改服务与配置
- 未碰法务代码/数据/正文；未改产品业务源码
- 全部改动仅在 allowed_paths：`scripts/auth-e2e-matrix/**` + `docs/协作/地龙/auth-e2e-matrix/**`

---

## 五、可选处置（请统筹裁决）

- **A**：接受边界结论 —— HTTP 契约 PASS + 前端清理由 TR-AUTH-SHELL-14 覆盖，本项记为
  "环境受限未复现"，不计入失败
- **B**：要求换非 headless（headed / 或有头虚拟显示）复测退出交互
- **C**：指派前端侧排查 logout 点击后无 API 派发的原因
- **D**：其他统筹指定方式
