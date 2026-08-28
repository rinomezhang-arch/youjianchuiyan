# 又见炊烟 微信小程序 / H5 多端项目 · 部署使用说明

## 📂 项目路径
**生成位置：`/workspace/youjianchuiyan-miniapp/`**

> ⚠️ 回答你问的两个问题：
> 1. **当前服务器**：这台 TRAE 沙盒主机名是 `all-in-one-72-9jl4q`（K8s 容器内网 IP `10.25.104.43`），不是"001"，也不是你的**腾讯云"天龙"(100.70.215.11 / 1.13.173.213)**。代码要部署到正式环境需要 `scp/rsync/git push` 过去。
> 2. **GitHub 开源项目参考**：借鉴了 `HackerAC/yshop-drink`（意象点餐，SpringBoot3+UniApp(Vue3)，含多门店、扫码点餐、微信登录/支付），以及 `jackywq/my-diancan-uniapp` 和 `ifavcode/mini_online_order` 两个高星项目的工程结构和 API 封装写法。

---

## 🚀 快速开始

### 方式 A：HBuilderX（推荐，最省心）
1. 打开 HBuilderX → 文件 → 导入 → 从本地目录导入 → 选择本项目文件夹
2. **配 AppID**：打开 `manifest.json` → 微信小程序配置 → 填入你自己的小程序 AppID（替换 `YOUR_WECHAT_APPID_HERE`）
3. 运行：
   - **小程序**：运行 → 运行到小程序模拟器 → 微信开发者工具
   - **H5**：运行 → 运行到浏览器 → Chrome 即可作为 `/m` 移动端站点调试
4. 发行：
   - **小程序**：发行 → 小程序-微信 → 生成 `unpackage/dist/build/mp-weixin`，用微信开发者工具上传提审
   - **H5**：发行 → 网站-H5，产物 `unpackage/dist/build/h5` 拷贝到你的 Nginx `/usr/local/openresty/nginx/html/m/`

### 方式 B：CLI（npm 命令行）
```bash
cd /workspace/youjianchuiyan-miniapp
npm i -g pnpm
pnpm i
pnpm dev:h5            # 本地H5调试
pnpm dev:mp-weixin     # 本地小程序调试（产物 unpackage/dist/dev/mp-weixin 丢微信开发者工具）
pnpm build:h5
pnpm build:mp-weixin
```

---

## ✏️ 部署前 **必须** 改的 5 处配置

| # | 路径 | 要改啥 |
|---|------|--------|
| 1 | `manifest.json` → `mp-weixin.appid` | 你的小程序 AppID，微信公众平台可查 |
| 2 | `config/env.js` → `API_BASE_URL` | 已填 `https://youjianchuiyan.com/api`，如端口不对请改 |
| 3 | `config/env.js` → `SUBSCRIBE_TMPL_IDS.*` | 公众平台 → 订阅消息 → 我的模板，复制 3 个模板 ID 进来 |
| 4 | `static/tabbar/*.png` | 已生成"占位图标"（灰色/金色十字方块），建议替换为你品牌的 81×81 PNG 图标 |
| 5 | 后端需要加 3 个接口（见下一节） | 前端已对接，后端补齐即可跑通完整闭环 |

---

## 🔌 后端 SpringBoot 需要补充的 3 个接口（你原本可能没写）

你现有的 `/dishes /bookings /packages /stores /auth/login` 已经就绪，**不动**。
为了完整的小程序端体验，建议**新增**以下 3 个 Controller：

### 1. `POST /api/auth/wx-login`（小程序启动时静默登录调用）
```
参数：{ code: string }           // uni.login() 返回的 code
返回：{
  code: 200,
  data: {
    token: string,              // JWT（和你现有 /auth/login 发的 token 同格式即可）
    openId: string,             // 微信 openid，保存一下
    userInfo?: {                // 如果该 openid 以前登录过，一并返回
      id, nickname, phone, avatar
    }
  }
}
```
后端用 `code` 调微信 `https://api.weixin.qq.com/sns/jscode2session` 拿到 `{openid, session_key, unionid}`，然后和用户表做绑定。

### 2. `POST /api/auth/wx-phone`（用户点"一键获取手机号"后调用）
```
参数：{ code: string }           // 按钮 open-type="getPhoneNumber" 返回的 code
返回：{
  code: 200,
  data: {
    phoneNumber: string,         // 13800138000
    purePhoneNumber: string,
    countryCode: string
  }
}
```
同样用 code 调微信 `https://api.weixin.qq.com/wxa/business/getuserphonenumber`。

### 3. `POST /api/pay/create`（预订定金支付 / 菜品订单支付 签名下发）
```
参数：{ orderType:'booking'|'dish', bizId, amountFen, attach? }
返回：{
  code: 200,
  data: {
    timeStamp: string,
    nonceStr: string,
    package: string,              // prepay_id=xxx
    signType: 'RSA' | 'MD5',
    paySign: string
  }
}
```
后端走微信 JSAPI/小程序支付下单（你的商户号、apiclient_cert.p12、notifyUrl 都填好），拿到 `prepay_id` 后用你的商户私钥签名。
`/wx/pay/notify` 支付成功回调里把 booking 状态改为 1(已确认) 或生成订单支付流水。

### 4. 点餐订单（OrderController，6 个 CRUD）
| Method | Path | 说明 |
|---|---|---|
| GET    | `/api/orders?page=&size=&status=&storeId=` | 我的订单列表：status 0待付 1已付待上菜 2制作中 3待取 4完成 5取消 |
| GET    | `/api/orders/{id}` | 订单详情（含 items 明细 + 各阶段时间戳） |
| POST   | `/api/orders` | 下单（payload 已在 `api/order.js` 注释：`{storeId, orderType, tableNo/addressId, items[], totalFen, couponId, packFeeFen, deliveryFeeFen, payableFen, remark, expectTime, contact}`） |
| PUT    | `/api/orders/{id}/cancel` | 取消订单（待支付才可取消，status==0） |
| POST   | `/api/orders/{id}/again` | 再来一单：返回 items[]，前端自动装回购物车 |

### 5. 优惠券 CouponController
| Method | Path | 说明 |
|---|---|---|
| GET  | `/api/coupons/available?storeId=` | 领券中心：可领的券列表（含剩余 stock） |
| GET  | `/api/coupons/mine?status=`       | 我的优惠券：0未用 1已用 2过期 |
| POST | `/api/coupons/{id}/receive`       | 单人领取，返回券实例 |
| GET  | `/api/coupons/useful-for-order?orderFen=&storeId=` | 下单页筛选可用券（按金额门槛 / 适用菜品池） |

### 6. 收货地址（下单页会用到）
| Method | Path | 说明 |
|---|---|---|
| GET    | `/api/addresses`              | 地址列表 |
| POST   | `/api/addresses`              | 新增 |
| PUT    | `/api/addresses/{id}`         | 修改 |
| DELETE | `/api/addresses/{id}`         | 删除 |
| PUT    | `/api/addresses/{id}/default` | 设为默认 |

> 前端已做兜底：上述接口未实现时，地址默认存本地 Storage，保证"外卖"流程先能跑通。

### 7. 评价 / 反馈 / 上传
| Method | Path | 说明 |
|---|---|---|
| POST | `/api/comments`                       | 写评价：`{ orderId, rating(1-5), taste, env, service, tags[], images[], text, anonymous }` |
| GET  | `/api/comments/dish/{dishId}?page=`   | 某菜品的评价列表（菜品详情页会用） |
| GET  | `/api/comments/order/{orderId}`       | 订单是否已评价，用于订单详情的"去评价/已评价"按钮 |
| POST | `/api/feedback`                       | 意见反馈：`{ type, content, images[], contact(phone) }` |
| POST | `/api/files/upload`（form-data, field=file） | 图片上传，返回 `{code:200, data:{ url }}` |

### 8. 营销：Banners / 公告 / 活动弹窗
| Method | Path | 说明 |
|---|---|---|
| GET | `/api/banners?storeId=`               | 首页轮播：返回 `[ { id, image/imgUrl, title/name, link/url/target } ]` |
| GET | `/api/notice?storeId=`                | 门店公告（虚线提示条，春节闭店/临时通知）|
| GET | `/api/popup?storeId=`                 | 首次进店弹窗（发券/活动海报，`{ id, image, type:'COUPON'|'PAGE', couponId, link/url/target }`，前端按 id 记忆，每个会话只显示一次） |

---

## 📐 项目结构（文件清单 · 共 48 个文件）

```
youjianchuiyan-miniapp/
├── package.json / jsconfig.json / index.html / vue.config.js
├── manifest.json                 ← AppID、小程序权限、H5路由
├── pages.json                    ← 17 个页面路由 + 5 TabBar
├── uni.scss                      ← 全局品牌色（金#C9A86C/墨#1A1A1A）
├── App.vue / main.js / DEPLOY.md ← 入口与部署文档
├── config/env.js                 ← 后端URL/模板ID/存储Key/商户配置
├── components/Stepper.vue        ← 全局数字步进器（sm/md/lg + 可编辑输入）
├── utils/
│   ├── request.js                ← uni.request + uploadFile 封装 / token / 自动storeId / 401跳登录
│   ├── storage.js                ← uni.storage 封装（JSON序列化、异常兜底）
│   └── util.js                   ← 日期/金额/手机号/防抖节流/平台判断
├── store/ (Pinia, 持久化)
│   ├── app.js                    ← 门店/用户/Token/OpenId/手机号
│   └── cart.js                   ← 购物车（分门店隔离、自动持久化）
├── api/ (12 个模块，100% 契约对齐)
│   ├── store.js                  GET /stores  /{id}
│   ├── dish.js                   GET /dishes /categories /search /{id} /featured
│   ├── booking.js                CRUD /bookings · cancel · confirm-deposit
│   ├── package.js                GET /packages?type=  /{id}
│   ├── order.js                  ⭐ 下单流程 · 我的订单 · 订单详情 · 再来一单
│   ├── coupon.js                 ⭐ 领券中心 · 我的券 · 下单页可用券筛选
│   ├── comment.js                ⭐ 写评价 · 菜品评价 · 意见反馈
│   ├── upload.js                 ⭐ 图片选择+上传 1 行搞定（chooseAndUpload）
│   ├── activity.js               ⭐ Banner / 公告 / 活动弹窗
│   ├── user.js                   /auth/login /phone-login /sms /me /logout
│   ├── pay.js                    /pay/create /status
│   └── wx.js                     微信登录/手机号/支付/订阅消息（原生调用）
├── pages/                        ← 24 个页面，全部金色徽派中式配色 + 入口全部打通
│   ├── 🏠 首页 index             门店切换(跳stores页)/搜索(跳search页)/Banner/公告/弹窗/招牌菜
│   ├── 🍲 菜单 menu              左分类/右菜品卡片/步进器/底部购物车胶囊 → 点"去结算"跳 checkout
│   ├── 📅 预订 book              日期/时段/人数步进/包厢/一键手机号授权/订阅消息
│   ├── 🎎 套餐 packages          类型chip/婚宴寿宴家宴商务宴升学宴卡片 → 跳 packageDetail
│   ├── 🙋 我的 me                渐变头像卡/统计/**12 功能入口**（全部真实跳转，无占位toast）
│   │       │                     📅我的预订 · 🧾我的订单 · 🍲浏览菜单 · 💰待支付
│   │       │                     🎟优惠券 · 🎖会员码 · 📲我的桌码 · ⭐我的收藏
│   │       │                     📍收货地址 · 💬意见反馈 · 📤分享邀请 · ⚙️设置
│   ├── 🧾 确认下单 checkout      堂食/自取/外卖三Tab + 优惠券 + 联系信息 + 金额汇总
│   ├── 📝 订单列表 orderList     6 Tab 状态 / 取消/支付/评价/联系门店 → 点卡片跳 orderDetail
│   ├── 📜 订单详情 orderDetail   顶部金色状态流 + 菜品明细 + 支付进度
│   ├── 📋 我的预订 bookingList   Tab 切换 / 取消 / 支付定金 → 点卡片跳 bookingDetail
│   ├── 🧾 预订详情 bookingDetail 详情 + 状态 + 操作 + 联系门店（⭐ 本次新增）
│   ├── 🎫 优惠券中心 coupon      领券中心 + 未用/已用/过期 Tab + checkout 回传选中
│   ├── ✍️ 写评价 comment         5星多维度 + 快速标签 + 9图上传 + 匿名
│   ├── 📢 意见反馈 feedback      6 种类型卡片 + 6张图 + 微信授权手机号
│   ├── 📍 收货地址 address       新增/编辑/删除/设默认 + checkout 回调回传
│   ├── 🎎 套餐详情 packageDetail 分组菜单 + 预订须知 + 立即预订
│   ├── 🍲 菜品详情 dishDetail    主图/食材/辣度/过敏原/相关推荐/加购
│   ├── 🔑 登录 login             微信一键/授权手机号/验证码三种方式
│   ├── 🌐 外链 webview           活动页跳转 / 小程序内置 web-view / H5 用 iframe
│   ├── ⚙️ 设置 setting           退出登录/清除缓存/协议跳转 → 跳 agreement（⭐ 本次新增）
│   ├── 👤 个人资料 profile       头像/昵称/性别/生日/手机号编辑（⭐ 本次新增）
│   ├── 🏬 门店列表 stores        多门店选择 / 距离 / 电话 / 导航 → 回首页自动选中（⭐ 本次新增）
│   ├── 📜 服务协议 agreement     用户协议/隐私协议/免责条款 Markdown 渲染（⭐ 本次新增）
│   ├── 🔍 搜索 search            菜品/套餐/门店 三合一搜索 + 热门/历史记录（⭐ 本次新增）
│   └── 📲 我的码 qrcode          Canvas 纯 JS 生成：邀请码/会员码/桌码/核销码（⭐ 本次新增）
└── static/tabbar/ (10 PNG)       TabBar 占位图标（灰/金 48×48）→ 请替换为品牌设计 81×81
```

---

## 🎨 设计规范（金色徽派中式风）

| 元素 | 色值 | 用途 |
|------|------|------|
| 主金 | `#C9A86C` | 主按钮、价格、选中色、TabBar 激活 |
| 浅金 | `#E6D4A8` | 渐变、卡片高亮背景 |
| 深金 | `#9A7E46` | 次要强调文字、链接 |
| 墨黑 | `#1A1A1A` | 主标题文字 |
| 暖米白 | `#FAF7F2` | 全局 page 背景 |

所有页面的 `.card` / `.gold-line` / `.gold-btn` / `.chip.active` 都用了统一 scss 变量 + mixin，改品牌色只动 `uni.scss` 一处。

---

## 📱 已接入的微信小程序原生能力

| 功能 | 文件 | 代码位置 |
|------|------|----------|
| 微信一键登录（启动静默） | `App.vue` + `api/wx.js` | `uni.login()` → `POST /auth/wx-login` |
| 授权获取手机号 | `pages/book/book.vue` + `pages/login/login.vue` | `<button open-type="getPhoneNumber">` → 后端 `POST /auth/wx-phone` |
| 微信支付 | `pages/bookingList/bookingList.vue` + `api/wx.js` + `api/pay.js` | 后端签名 → `uni.requestPayment` |
| 订阅消息（预订成功/到店提醒） | `pages/book/book.vue` 提交前调用 | `uni.requestSubscribeMessage(tmplIds)` |
| 分享给好友/朋友圈 | 每个页面可加 `onShareAppMessage` / `onShareTimeline` | 已预留，可按页面细化 |
| 拨打门店电话 | 首页/套餐/我的/预订详情 | `uni.makePhoneCall` |

---

> 💡 **提示**：H5 和小程序编译的是同一套代码。把 H5 编译产物丢到 Nginx `/m/` 目录下，就同时拥有了 `https://youjianchuiyan.com/m` 移动端和微信小程序两套前端，完全符合你"方案 2"的预期。
