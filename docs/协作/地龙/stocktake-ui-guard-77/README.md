# DL-STOCKTAKE-UI-GUARD-77 交付说明

执行: 地龙 | 2026-09-15
基线: 6cf5e6d15fc908a159d4aa29b64d1e3bdb82a652 (codex/integration-20260913)
分支: codex/dilong-stocktake-ui-guard-77
范围: 只做离线 UI guard，不执行 53、不登录、不造单、不改密码

## 目的

修复 DL-RC-STOCKTAKE-HTTP-R3-53 中过弱的页面断言：

```js
// 旧断言（会把"被路由守卫踢回登录页"误判为成功）
assert('ui.login-via-page', !page.url().includes('/login'));
```

该断言在"时机早 + redirect 未触发"时误判 PASS，导致盘点页从未真正进入，
take 9~13 实为登录页状态下直接打 API 造单，不构成真实页面提交。

## 交付物

| 文件 | 说明 |
|---|---|
| `scripts/dilong-stocktake-ui-guard-77/assert-stocktake-ready.mjs` | 导出 `assertStocktakeEditingReady(page, {expectedPath})`，5 条硬断言 |
| `scripts/dilong-stocktake-ui-guard-77/fixtures.json` | 6 个离线 DOM 夹具，含 53 登录页陷阱回归样例 |

## 断言规则（任一不满足即 FAIL，不做宽容放行）

1. `guard.path-is-expected` —— pathname 必须等于期望路径
2. `guard.not-login-page` —— 路径或 redirect 指纹命中登录页即 FAIL
3. `guard.qty-input-present` —— 实盘数量框必须存在
4. `guard.qty-input-visible` —— 至少一个实盘框可见（拒绝隐藏）
5. `guard.qty-input-editable` —— 至少一个实盘框可编辑（拒绝 disabled/readonly）

## 离线自测

```
node scripts/dilong-stocktake-ui-guard-77/assert-stocktake-ready.mjs --self-test
```

夹具覆盖：登录页陷阱 / 纯登录页 / 真编辑态 / 隐藏框 / 禁用框 / 错误路径。
不需要真实后端、不需要浏览器、不连库。

## 给 Trae66 的接入方式

```js
import { assertStocktakeEditingReady } from './scripts/dilong-stocktake-ui-guard-77/assert-stocktake-ready.mjs';

const guard = await assertStocktakeEditingReady(page, { expectedPath: '/dashboard/stock-take' });
if (!guard.ok) {
  // guard.fail 项与 anomalies 可直接落证据
  throw new Error('stocktake editing not ready: ' + guard.anomalies.join(','));
}
```

## 边界

- 未登录现有业务、未造单、未改密码、未执行 53、未写库、未改源码
- 未起 18080/5183，未恢复旧 15/53，未碰律师边界
- 旧工作区 codex-rc15-integrated-20260909 (HEAD 895e3567) 保留未动，未 reset/stash/强推
