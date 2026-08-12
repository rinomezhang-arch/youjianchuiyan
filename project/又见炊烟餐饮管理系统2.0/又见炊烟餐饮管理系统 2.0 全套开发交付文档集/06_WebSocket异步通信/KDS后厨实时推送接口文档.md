# KDS后厨实时推送接口文档

> 版本：V2.0  
> 创建日期：2026-08-02  
> 适用：又见炊烟餐饮管理系统 2.0 后厨显示系统（KDS）  
> 技术栈：后端 Spring Boot + RabbitMQ + STOMP；前端 sockjs-client + stompjs  
> 维护：Trae（TRAE-BOT）

---

## 1. 概述

### 1.1 KDS 系统定位

KDS（Kitchen Display System，后厨显示系统）是又见炊烟餐饮管理系统 2.0 的实时厨房调度核心。它取代传统纸质后厨工单，通过厨房屏（壁挂平板/显示器）实时展示 incoming 订单、菜品制作状态、出菜进度，实现：

- **订单实时可视**：前厅下单后 1 秒内推送到后厨屏，无需人工传递工单。
- **制作状态追踪**：每道菜从"待制作 → 制作中 → 已出品"状态全流程可视。
- **优先级排序**：按下单时间、加急标记、桌台号自动排序，厨师按序制作。
- **退菜即时同步**：前厅退菜后后厨屏立即移除或标记，避免误制作。
- **结账联动**：订单结账后自动从后厨屏归档，释放屏幕空间。
- **呼叫服务**：后厨出菜完成呼叫服务员上菜，或前厅呼叫后厨加菜。

### 1.2 适用场景

| 角色 | 设备 | 使用场景 |
|------|------|---------|
| 厨师长 | 厨房壁挂屏（iPad/显示器） | 全局查看所有订单，分配制作任务 |
| 厨师 | 制作工位屏 | 查看分配给自己的菜品，标记制作进度 |
| 切配工 | 备料工位屏 | 查看待备料清单 |
| 出品核对员 | 出菜口屏 | 核对出菜完整性，标记已出品 |
| 前厅经理 | 前台看板 | 监控出菜进度，处理客户催菜 |

### 1.3 设计目标

- **低延迟**：消息从产生到到达前端 ≤ 1 秒（P99）。
- **高可靠**：消息至少送达一次（At-Least-Once），支持断线重连后补推。
- **幂等性**：前端按 `notifyId` 去重，重复消息不产生副作用。
- **多租户隔离**：按 `storeId` 隔离，宁国店与宣城店互不干扰。

---

## 2. 技术架构

### 2.1 整体架构

```
[业务方 Controller/Service]
        │
        ▼
[NotifyPublisher.publish(NotifyEvent)]
        │
        ├──→ MySQL.sys_notification（同步落库，确保不丢）
        │
        └──→ RabbitMQ.youjian.notify.exchange（异步推送）
                    │ routing-key = notify.event
                    ▼
              [youjian.notify.queue]
                    │
                    ▼
              [NotifyConsumer.onNotify]
                    │
                    ├──→ WebSocket STOMP 推送 → /topic/kitchen/{storeId}（KDS 厨房屏）
                    ├──→ WebSocket STOMP 推送 → /topic/notify/{userId}（个人通知）
                    └──→ 短信通道（客户类事件）
```

### 2.2 RabbitMQ 配置

| 项 | 值 | 配置项 |
|----|----|--------|
| Exchange | `youjian.notify.exchange` | `app.notify.exchange` |
| Exchange 类型 | `direct` | DirectExchange |
| Queue | `youjian.notify.queue` | `app.notify.queue` |
| Routing Key | `notify.event` | `app.notify.routing-key` |
| 消息 TTL | 7 天 | 队列属性 `x-message-ttl` |
| 队列持久化 | true | durable |
| 消息持久化 | true | Jackson2JsonMessageConverter |
| 重试次数 | 3 | `spring.rabbitmq.listener.simple.retry.max-attempts` |
| 重试间隔 | 1s 起步，×2 递增 | `spring.rabbitmq.listener.simple.retry.initial-interval` |
| 并发消费者 | 1（默认） | `spring.rabbitmq.listener.simple.concurrency` |

### 2.3 后端 WebSocket 配置

后端需引入 `spring-boot-starter-websocket` 并配置 STOMP 端点（当前 `NotifyConsumer` 中为 TODO 待接入）：

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 浏览器订阅前缀：/topic/* 走内存 STOMP broker
        config.enableSimpleBroker("/topic");
        // 浏览器发送消息前缀（如调用 @MessageMapping）
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 连接端点，使用 SockJS 兼容老浏览器
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 2.4 NotifyConsumer 改造点

在 `NotifyConsumer.onNotify` 中将 TODO 替换为实际 STOMP 推送：

```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

@RabbitListener(queues = "${app.notify.queue}")
public void onNotify(NotifyEvent event) {
    if (event == null || event.getEventType() == null) return;
    log.info("收到 MQ 通知 eventType={} storeId={} title={}",
            event.getEventType(), event.getStoreId(), event.getTitle());

    try {
        // KDS 厨房屏推送：按门店隔离
        if (isKitchenEvent(event.getEventType())) {
            messagingTemplate.convertAndSend(
                "/topic/kitchen/" + event.getStoreId(), event);
        }

        // 个人通知推送：按员工 ID 定向
        if (isPersonalEvent(event.getEventType()) && event.getReceiverIds() != null) {
            for (String userId : event.getReceiverIds().split(",")) {
                messagingTemplate.convertAndSend(
                    "/topic/notify/" + userId.trim(), event);
            }
        }
    } catch (Exception e) {
        log.error("通知分发失败 eventType={} err={}",
                event.getEventType(), e.getMessage(), e);
    }
}
```

---

## 3. 消息结构

### 3.1 NotifyEvent 结构

KDS 推送的消息体复用 `NotifyEvent`（详见 `业务消息结构体说明.md`）：

```json
{
  "eventType": "order.created",
  "storeId": 1,
  "title": "新订单 ORD20260802001",
  "content": "桌台 A06 5 道菜",
  "priority": "normal",
  "senderId": 5,
  "senderName": "张三",
  "receiverType": "all",
  "receiverIds": null,
  "relatedType": "booking",
  "relatedId": 12345,
  "triggerTime": "2026-08-02 14:30:00",
  "remark": "客户备注：少辣"
}
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | KDS 场景说明 |
|------|------|------|-------------|
| `eventType` | String | 是 | 事件类型，见第 5 节 |
| `storeId` | Long | 是 | 门店 ID，用于 `/topic/kitchen/{storeId}` 路由 |
| `title` | String | 是 | 通知标题（≤200 字符），KDS 屏顶部展示 |
| `content` | String | 否 | 通知内容，KDS 详情区展示 |
| `priority` | String | 否 | low/normal/high/urgent，影响 KDS 排序与颜色 |
| `senderId` | Integer | 否 | 发送者员工 ID |
| `senderName` | String | 否 | 发送者姓名，KDS 显示"下单员" |
| `receiverType` | String | 否 | all/role/staff/dept |
| `receiverIds` | String | 否 | 接收者 ID 列表（逗号分隔），用于 `/topic/notify/{userId}` |
| `relatedType` | String | 否 | 关联业务类型：order/booking/dish |
| `relatedId` | Long | 否 | 关联业务 ID |
| `triggerTime` | LocalDateTime | 否 | 触发时间，KDS 显示"下单时间" |
| `remark` | String | 否 | 业务备注，如客户特殊要求 |

---

## 4. STOMP 连接

### 4.1 连接 URL

```
ws://{host}:8080/ws
```

或通过 Nginx 反向代理（生产环境）：

```
wss://{domain}/ws
```

Nginx 配置示例：

```nginx
location /ws {
    proxy_pass http://backend:8080/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
}
```

### 4.2 订阅路径

| 订阅路径 | 推送方 | 接收端 | 用途 |
|---------|--------|--------|------|
| `/topic/kitchen/{storeId}` | NotifyConsumer | KDS 厨房屏 | 按门店推送厨房类事件（新订单、菜品状态、出菜完成等） |
| `/topic/notify/{userId}` | NotifyConsumer | 个人客户端 | 按员工 ID 推送个人通知（呼叫服务、库存预警等） |

`{storeId}` 替换为实际门店 ID（如 `1` 表示宁国店），`{userId}` 替换为登录员工 ID。

### 4.3 鉴权

STOMP 连接时通过 HTTP Header 携带 JWT：

```js
const socket = new SockJS('/ws', null, {
  headers: { Authorization: 'Bearer ' + token }
})
```

后端可通过 `HandshakeInterceptor` 校验 JWT 并注入 `storeId`/`userId` 到 Principal：

```java
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validate(token)) return false;
        attributes.put("userId", jwtUtil.getUserId(token));
        attributes.put("storeId", jwtUtil.getStoreId(token));
        return true;
    }
}
```

---

## 5. 推送事件类型

KDS 系统共处理 7 类推送事件，全部走 `/topic/kitchen/{storeId}` 通道。

### 5.1 NEW_ORDER 新订单

| 项 | 内容 |
|---|---|
| 事件类型 | `order.created` |
| 触发时机 | 前厅下单成功（`POST /api/orders` 返回 200 后） |
| 推送目标 | `/topic/kitchen/{storeId}` |
| 优先级 | normal（加急订单为 high） |

数据结构示例：

```json
{
  "eventType": "order.created",
  "storeId": 1,
  "title": "新订单 ORD20260802001",
  "content": "桌台 A06 · 5 道菜 · 6 人",
  "priority": "normal",
  "senderId": 5,
  "senderName": "李四",
  "relatedType": "order",
  "relatedId": 1001,
  "triggerTime": "2026-08-02 18:30:00",
  "remark": "客户备注：少辣、不要香菜"
}
```

订阅端处理逻辑：
1. KDS 屏顶部弹出"新订单"提示音 + 闪烁动画。
2. 在订单列表头部插入新订单卡片，显示桌台号、菜品数、下单时间。
3. 按下单时间排序，加急订单（priority=high）置顶并红色高亮。
4. 同时触发热敏打印机输出后厨工单（PRT-002）。

### 5.2 DISH_STATUS 菜品状态变更

| 项 | 内容 |
|---|---|
| 事件类型 | `dish.status_changed` |
| 触发时机 | 厨师在 KDS 屏点击"开始制作"/"已完成"按钮 |
| 推送目标 | `/topic/kitchen/{storeId}` |
| 优先级 | normal |

数据结构示例：

```json
{
  "eventType": "dish.status_changed",
  "storeId": 1,
  "title": "菜品状态变更",
  "content": "A06 桌 · 红烧肉 · 已开始制作",
  "priority": "normal",
  "relatedType": "dish",
  "relatedId": 5001,
  "triggerTime": "2026-08-02 18:32:00",
  "remark": "status: cooking; dish_id: 201; order_id: 1001; table: A06"
}
```

`remark` 字段以分号分隔的 key-value 形式承载结构化数据：
- `status`：新状态（pending/cooking/done/cancelled）
- `dish_id`：菜品 ID
- `order_id`：订单 ID
- `table`：桌台号

订阅端处理逻辑：
1. 根据 `relatedId` 定位 KDS 屏上的菜品卡片。
2. 更新卡片状态标识：pending（灰色待制作）→ cooking（黄色制作中）→ done（绿色已完成）。
3. 状态为 `done` 时将卡片移至"已出品"区域。
4. 同步刷新前厅看板的出菜进度条。

### 5.3 ORDER_PAID 订单已结

| 项 | 内容 |
|---|---|
| 事件类型 | `order.paid` |
| 触发时机 | 前厅结账成功（`POST /api/orders/{id}/checkout` 返回 200 后） |
| 推送目标 | `/topic/kitchen/{storeId}` |
| 优先级 | normal |

数据结构示例：

```json
{
  "eventType": "order.paid",
  "storeId": 1,
  "title": "订单已结",
  "content": "A06 桌 · ORD20260802001 · ¥488.00",
  "priority": "normal",
  "relatedType": "order",
  "relatedId": 1001,
  "triggerTime": "2026-08-02 20:15:00",
  "remark": "pay_method: wechat; total: 488.00"
}
```

订阅端处理逻辑：
1. 检查该订单是否所有菜品已出品（status=done）。
2. 若有未出品菜品，弹出警告"该订单仍有 N 道菜未出品，确认归档？"。
3. 确认后从 KDS 活动列表移除，归档到"今日已完成"区域。
4. 同时触发结账单打印（PRT-001）。

### 5.4 CANCEL_DISH 退菜

| 项 | 内容 |
|---|---|
| 事件类型 | `dish.cancelled` |
| 触发时机 | 前厅退菜操作成功（`POST /api/orders/{id}/refund-dishes` 返回 200 后） |
| 推送目标 | `/topic/kitchen/{storeId}` |
| 优先级 | high |

数据结构示例：

```json
{
  "eventType": "dish.cancelled",
  "storeId": 1,
  "title": "退菜通知",
  "content": "A06 桌 · 红烧肉 ×2 已退",
  "priority": "high",
  "senderId": 5,
  "senderName": "王五",
  "relatedType": "dish",
  "relatedId": 5001,
  "triggerTime": "2026-08-02 18:45:00",
  "remark": "dish_id: 201; qty: 2; reason: 客户改菜; order_id: 1001"
}
```

订阅端处理逻辑：
1. KDS 屏弹出红色退菜告警 + 警示音。
2. 定位到对应菜品卡片，标记为"已退菜"（红色删除线）。
3. 若菜品状态为 `pending`（尚未制作），直接从列表移除。
4. 若状态为 `cooking`（制作中），高亮提示厨师停止制作。
5. 同步触发退菜单打印（PRT-006）。

### 5.5 CALL_SERVICE 呼叫服务

| 项 | 内容 |
|---|---|
| 事件类型 | `service.called` |
| 触发时机 | 后厨点击"呼叫服务员上菜"按钮，或前厅点击"催菜"按钮 |
| 推送目标 | 双向：后厨呼叫走 `/topic/kitchen/{storeId}`，前厅催菜走 `/topic/notify/{userId}`（服务员 ID） |
| 优先级 | urgent |

数据结构示例（后厨呼叫上菜）：

```json
{
  "eventType": "service.called",
  "storeId": 1,
  "title": "呼叫上菜",
  "content": "A06 桌 · 红烧肉已出品，请上菜",
  "priority": "urgent",
  "senderId": 8,
  "senderName": "厨师长",
  "relatedType": "dish",
  "relatedId": 5001,
  "triggerTime": "2026-08-02 18:50:00",
  "remark": "call_type: serve_dish; table: A06; dish_id: 201"
}
```

订阅端处理逻辑：
1. 服务员佩戴的智能手表/对讲机震动提醒。
2. 前厅看板右上角弹出呼叫卡片，显示桌台号、菜品名、呼叫时间。
3. 30 秒未响应则升级提醒（声音加大 + 通知店长）。
4. 服务员点击"已接单"后卡片消失，回执推送到后厨屏。

### 5.6 KITCHEN_DONE 出菜完成

| 项 | 内容 |
|---|---|
| 事件类型 | `dish.served` |
| 触发时机 | 菜品从后厨传到前厅（出品核对员点击"已传菜"按钮） |
| 推送目标 | `/topic/kitchen/{storeId}` + `/topic/notify/{服务员ID}` |
| 优先级 | normal |

数据结构示例：

```json
{
  "eventType": "dish.served",
  "storeId": 1,
  "title": "菜品已传菜",
  "content": "A06 桌 · 红烧肉 ×1 已传菜",
  "priority": "normal",
  "relatedType": "dish",
  "relatedId": 5001,
  "triggerTime": "2026-08-02 18:55:00",
  "remark": "dish_id: 201; order_id: 1001; table: A06; server_id: 12"
}
```

订阅端处理逻辑：
1. KDS 屏将该菜品从"制作中"移到"已传菜"归档区。
2. 前厅看板更新该桌台的出菜进度（已传菜数/总菜数）。
3. 全部菜品传菜完成后，前厅看板该桌台显示"全部上齐"。

### 5.7 INVENTORY_ALERT 库存预警

| 项 | 内容 |
|---|---|
| 事件类型 | `inventory.low_stock` |
| 触发时机 | 食材库存低于阈值（定时任务扫描 `inventory` 表） |
| 推送目标 | `/topic/notify/{采购员ID}` + `/topic/notify/{店长ID}` |
| 优先级 | high |

数据结构示例：

```json
{
  "eventType": "inventory.low_stock",
  "storeId": 1,
  "title": "库存预警",
  "content": "五花肉库存不足，当前 2.5kg，阈值 5kg",
  "priority": "high",
  "receiverType": "staff",
  "receiverIds": "15,3",
  "relatedType": "ingredient",
  "relatedId": 301,
  "triggerTime": "2026-08-02 09:00:00",
  "remark": "ingredient_id: 301; current_qty: 2.5; threshold: 5; unit: kg"
}
```

订阅端处理逻辑：
1. 采购员与店长收到站内通知 + 浏览器通知（Notification API）。
2. 采购员看板顶部固定显示预警条，点击跳转采购单创建页。
3. 同时影响 KDS 屏：相关菜品卡片标注"原料紧张"提示，建议厨师长优先制作其他菜品。

---

## 6. 事件类型与 Kitchen 事件判定

后端 `NotifyConsumer.isKitchenEvent()` 判定以下事件走 `/topic/kitchen/{storeId}`：

| eventType | 是否 Kitchen 事件 | 是否 Personal 事件 |
|-----------|------------------|-------------------|
| `order.created` | 是 | 否 |
| `dish.status_changed` | 是 | 否 |
| `order.paid` | 是 | 否 |
| `dish.cancelled` | 是 | 否 |
| `service.called` | 是（后厨发起时） | 是（前厅催菜时） |
| `dish.served` | 是 | 是（定向给服务员） |
| `inventory.low_stock` | 否（仅影响 KDS 显示） | 是（采购员/店长） |

---

## 7. 前端集成

### 7.1 依赖安装

```bash
npm install sockjs-client @stomp/stompjs
```

`package.json` 新增依赖：

```json
{
  "dependencies": {
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

### 7.2 WebSocket 工具封装

在 `frontend_v3/src/utils/ws.js` 封装连接管理（当前为待新增文件）：

```js
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/store/user'

let stompClient = null
let reconnectTimer = null
const RECONNECT_DELAY = 5000 // 断线重连间隔 5 秒

/**
 * 建立 STOMP 连接并订阅 KDS 通道
 * @param {Object} handlers 事件处理回调
 * @param {Function} handlers.onNewOrder 新订单回调
 * @param {Function} handlers.onDishStatus 菜品状态变更回调
 * @param {Function} handlers.onOrderPaid 订单已结回调
 * @param {Function} handlers.onCancelDish 退菜回调
 * @param {Function} handlers.onCallService 呼叫服务回调
 * @param {Function} handlers.onKitchenDone 出菜完成回调
 * @param {Function} handlers.onInventoryAlert 库存预警回调
 */
export function connectKDS(handlers = {}) {
  const userStore = useUserStore()
  const token = localStorage.getItem('token') || ''
  const storeId = userStore.userInfo?.storeId || 1
  const userId = userStore.userInfo?.staffId

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: 'Bearer ' + token },
    reconnectDelay: RECONNECT_DELAY,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[KDS] STOMP 已连接')

      // 订阅厨房通道
      stompClient.subscribe(`/topic/kitchen/${storeId}`, (message) => {
        const event = JSON.parse(message.body)
        dispatchKitchenEvent(event, handlers)
      })

      // 订阅个人通知通道
      if (userId) {
        stompClient.subscribe(`/topic/notify/${userId}`, (message) => {
          const event = JSON.parse(message.body)
          dispatchPersonalEvent(event, handlers)
        })
      }
    },
    onStompError: (frame) => {
      console.error('[KDS] STOMP 错误:', frame.headers['message'])
    },
    onWebSocketClose: () => {
      console.warn('[KDS] WebSocket 断开，将自动重连')
    }
  })

  stompClient.activate()
}

const processedNotifyIds = new Set()
const MAX_CACHE = 500

function dispatchKitchenEvent(event, handlers) {
  // 幂等性：按 notifyId 去重（relatedId + triggerTime 组合作为简易 ID）
  const notifyId = `${event.relatedId}_${event.triggerTime}`
  if (processedNotifyIds.has(notifyId)) return
  processedNotifyIds.add(notifyId)
  if (processedNotifyIds.size > MAX_CACHE) {
    // 清理最早的缓存（Set 保持插入顺序）
    const first = processedNotifyIds.values().next().value
    processedNotifyIds.delete(first)
  }

  switch (event.eventType) {
    case 'order.created':
      handlers.onNewOrder?.(event)
      break
    case 'dish.status_changed':
      handlers.onDishStatus?.(event)
      break
    case 'order.paid':
      handlers.onOrderPaid?.(event)
      break
    case 'dish.cancelled':
      handlers.onCancelDish?.(event)
      break
    case 'service.called':
      handlers.onCallService?.(event)
      break
    case 'dish.served':
      handlers.onKitchenDone?.(event)
      break
    case 'inventory.low_stock':
      handlers.onInventoryAlert?.(event)
      break
    default:
      console.warn('[KDS] 未知事件类型:', event.eventType)
  }
}

function dispatchPersonalEvent(event, handlers) {
  // 个人通知同样按 notifyId 去重
  const notifyId = `personal_${event.relatedId}_${event.triggerTime}`
  if (processedNotifyIds.has(notifyId)) return
  processedNotifyIds.add(notifyId)

  if (event.eventType === 'inventory.low_stock') {
    handlers.onInventoryAlert?.(event)
  } else if (event.eventType === 'service.called') {
    handlers.onCallService?.(event)
  }
}

export function disconnectKDS() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}
```

### 7.3 在 KDS 页面集成

```vue
<template>
  <div class="kds-screen">
    <div class="kds-header">
      <h2>又见炊烟 · 后厨显示系统</h2>
      <span class="kds-time">{{ currentTime }}</span>
    </div>
    <div class="kds-orders">
      <OrderCard
        v-for="order in activeOrders"
        :key="order.id"
        :order="order"
        @start-cooking="onStartCooking"
        @finish-dish="onFinishDish"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { connectKDS, disconnectKDS } from '@/utils/ws'
import { ElNotification } from 'element-plus'
import OrderCard from './OrderCard.vue'

const activeOrders = ref([])
const currentTime = ref('')

onMounted(() => {
  connectKDS({
    onNewOrder: (event) => {
      activeOrders.value.unshift(parseOrderFromEvent(event))
      ElNotification({
        title: '新订单',
        message: event.content,
        type: 'success',
        duration: 3000
      })
      playSound('new-order')
    },
    onDishStatus: (event) => {
      updateDishStatus(event)
    },
    onOrderPaid: (event) => {
      archiveOrder(event.relatedId)
    },
    onCancelDish: (event) => {
      markDishCancelled(event)
      ElNotification({
        title: '退菜通知',
        message: event.content,
        type: 'warning',
        duration: 0
      })
      playSound('cancel-dish')
    },
    onCallService: (event) => {
      ElNotification({
        title: '呼叫服务',
        message: event.content,
        type: 'error',
        duration: 0
      })
    },
    onKitchenDone: (event) => {
      moveDishToServed(event)
    },
    onInventoryAlert: (event) => {
      ElNotification({
        title: '库存预警',
        message: event.content,
        type: 'warning',
        duration: 0
      })
    }
  })
})

onUnmounted(() => {
  disconnectKDS()
})

function playSound(type) {
  const audio = new Audio(`/sounds/${type}.mp3`)
  audio.play().catch(() => {})
}
</script>
```

---

## 8. 异常处理

### 8.1 断线重连

`@stomp/stompjs` 内置断线重连机制，通过 `reconnectDelay` 配置重连间隔（默认 5000ms）。重连成功后会自动重新订阅之前的通道，无需手动处理。

建议在前端增加连接状态指示器：

```js
const connectionStatus = ref('disconnected') // connected / connecting / disconnected / error

stompClient = new Client({
  // ...
  onChangeState: (state) => {
    connectionStatus.value = state
  },
  onConnect: () => {
    connectionStatus.value = 'connected'
  },
  onWebSocketClose: () => {
    connectionStatus.value = 'disconnected'
  }
})
```

KDS 屏顶部显示连接状态图标：绿色（已连接）、黄色（重连中）、红色（断开）。

### 8.2 消息幂等性

由于 RabbitMQ 的 At-Least-Once 语义 + 前端断线重连，可能收到重复消息。前端通过 `notifyId` 去重：

- `notifyId` 由 `relatedId + triggerTime` 组合生成（`业务消息结构体说明.md` 中 `NotifyEvent` 暂无独立 `notifyId` 字段，前端用此组合作为简易去重键）。
- 使用 `Set` 缓存最近 500 条已处理消息 ID。
- 缓存超过 500 条时，删除最早的一条（FIFO）。
- 重复消息直接 return，不触发 UI 更新。

建议后端在 `NotifyEvent` 中增加 `notifyId` 字段（UUID），作为更可靠的去重键。

### 8.3 消息丢失兜底

WebSocket 推送存在丢失风险（网络抖动、客户端离线）。兜底方案：

1. **定时全量同步**：KDS 页面每 60 秒调用 `GET /api/orders?status=active&store_id={storeId}` 全量拉取活动订单，与本地列表 diff 并修正。
2. **重新连接后补拉**：断线重连成功后，立即调用一次全量同步接口，补齐离线期间错过的消息。
3. **站内通知兜底**：所有 `NotifyEvent` 已同步落库 `sys_notification`，前端可通过 `GET /api/notifications?is_read=0` 拉取未读通知作为最终兜底。

### 8.4 消息顺序

RabbitMQ 单队列单消费者保证消息 FIFO 顺序。但前端多通道订阅（`/topic/kitchen/*` + `/topic/notify/*`）可能乱序。处理策略：

- 同一订单的事件按 `triggerTime` 在前端排序后渲染。
- 状态变更事件（pending → cooking → done）若乱序到达，前端按状态机校验：仅允许正向转换，逆向转换忽略（如 `done` 后收到 `cooking` 则丢弃）。

### 8.5 降级方案

当 RabbitMQ 不可用（`app.notify.enabled=false`）时：

1. `NotifyPublisher` 仅同步落库 `sys_notification`，不发送 MQ。
2. 前端 WebSocket 无消息推送，改为 5 秒轮询 `GET /api/notifications?is_read=0`。
3. KDS 页面同时轮询 `GET /api/orders?status=active` 保证订单列表实时性。

---

## 9. 测试验证

### 9.1 连接测试

```bash
# 安装 wscat 测试 STOMP 连接
npm install -g wscat

# 连接测试
wscat -c ws://localhost:8080/ws
```

### 9.2 消息推送测试

后端单元测试模拟事件发布：

```java
@SpringBootTest
class KDSNotifyTest {
    @Autowired
    private NotifyPublisher publisher;

    @Test
    void testNewOrderPush() {
        publisher.publish(NotifyEvent.builder()
            .eventType(NotifyEvent.NotifyType.ORDER_CREATED)
            .storeId(1L)
            .title("测试新订单")
            .content("A06 桌 3 道菜")
            .relatedType("order")
            .relatedId(9999L)
            .triggerTime(LocalDateTime.now())
            .build());
    }
}
```

### 9.3 前端联调

1. 启动后端（`app.notify.enabled=true`）+ RabbitMQ。
2. 前端打开 KDS 页面，确认控制台输出 `[KDS] STOMP 已连接`。
3. 在另一个浏览器标签页执行下单操作。
4. 观察 KDS 页面是否在 1 秒内收到新订单卡片。

---

## 10. 版本记录

| 版本 | 日期 | 维护者 | 变更内容 |
|------|------|--------|---------|
| V1.0 | 2026-02-15 | 后端组 | 初版，仅 RabbitMQ 落库，WebSocket 为 TODO |
| V2.0 | 2026-08-02 | Trae | 完整定义 KDS 推送协议，覆盖 7 类事件，补充前端集成与异常处理 |
