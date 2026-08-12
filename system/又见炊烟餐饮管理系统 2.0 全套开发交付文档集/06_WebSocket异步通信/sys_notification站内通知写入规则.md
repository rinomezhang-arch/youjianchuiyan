# sys_notification 站内通知写入规则

> 适用：所有需要落库的通知事件  
> 来源：`banquet_project/src/main/java/com/youjian/banquet/entity/SysNotification.java`  
> 维护：地龙（DL-BOT）  
> 更新：2026-08-02

---

## 1. 表结构

`sys_notification` 表（106 张业务表之一）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `notify_id` | BIGINT AUTO_INCREMENT | ✅ | 主键 |
| `store_id` | BIGINT NOT NULL | ✅ | 多租户隔离（0=全局） |
| `notify_type` | VARCHAR(50) NOT NULL | ✅ | 通知类型，对应 NotifyEvent.eventType |
| `notify_title` | VARCHAR(200) NOT NULL | ✅ | 标题 |
| `notify_content` | TEXT | 否 | 内容（可长文本） |
| `priority` | VARCHAR(20) | 否 | low / normal / high / urgent，默认 normal |
| `sender_id` | INT | 否 | 发送者员工ID |
| `sender_name` | VARCHAR(50) | 否 | 发送者姓名 |
| `send_time` | DATETIME | 否 | 发送时间（默认 NOW） |
| `receiver_type` | VARCHAR(20) | 否 | all / role / staff / dept，默认 all |
| `receiver_ids` | TEXT | 否 | 接收者ID列表（逗号分隔），null=全体 |
| `related_type` | VARCHAR(50) | 否 | 关联业务类型：booking / order / purchase 等 |
| `related_id` | BIGINT | 否 | 关联业务ID |
| `is_read` | TINYINT | 否 | 0=未读 1=已读，默认 0 |
| `status` | VARCHAR(20) | 否 | draft / published / archived，默认 published |
| `remark` | VARCHAR(500) | 否 | 备注 |
| `created_at` | DATETIME | 自动 | 创建时间 |
| `updated_at` | DATETIME | 自动 | 更新时间 |

## 2. 写入规则

### 2.1 通过 NotifyPublisher 自动写入

业务方**不直接操作 `sys_notification` 表**，统一通过 `NotifyPublisher.publish(NotifyEvent)`：

```java
@Autowired
private NotifyPublisher notifyPublisher;

notifyPublisher.publish(NotifyEvent.builder()
    .eventType(NotifyEvent.NotifyType.ORDER_CREATED)
    .storeId(1L)
    .title("新订单 ORD001")
    .build());
```

`NotifyPublisher` 内部：
1. 将 `NotifyEvent` 映射为 `SysNotification` 实体
2. 调用 `sysNotificationRepository.save(entity)` 同步落库
3. （若启用 MQ）额外异步发送到 RabbitMQ

### 2.2 实体映射规则

| NotifyEvent 字段 | SysNotification 字段 | 转换规则 |
|------------------|---------------------|---------|
| `eventType` | `notifyType` | 直接映射 |
| `storeId` | `storeId` | null → 0（全局） |
| `title` | `notifyTitle` | 直接映射 |
| `content` | `notifyContent` | 直接映射 |
| `priority` | `priority` | null → "normal"（@PrePersist 默认） |
| `senderId` | `senderId` | 直接映射 |
| `senderName` | `senderName` | 直接映射 |
| `triggerTime` | `sendTime` | 直接映射 |
| `receiverType` | `receiverType` | null → "all"（@PrePersist 默认） |
| `receiverIds` | `receiverIds` | 直接映射 |
| `relatedType` | `relatedType` | 直接映射 |
| `relatedId` | `relatedId` | 直接映射 |
| `remark` | `remark` | 直接映射 |
| - | `isRead` | @PrePersist 默认 0 |
| - | `status` | @PrePersist 默认 "published" |
| - | `createdAt` | @PrePersist 自动 NOW |
| - | `updatedAt` | @PrePersist 自动 NOW |

## 3. 查询接口

### 3.1 Repository 已提供方法

`SysNotificationRepository`：

```java
// 按门店查询全部（最新在前）
List<SysNotification> findByStoreIdOrderByCreatedAtDesc(Long storeId);

// 按门店查未读
List<SysNotification> findByStoreIdAndIsReadOrderByCreatedAtDesc(Long storeId, Integer isRead);

// 按门店+类型查
List<SysNotification> findByStoreIdAndNotifyTypeOrderByCreatedAtDesc(Long storeId, String notifyType);

// 按关联业务查（如查预订 123 的所有通知）
List<SysNotification> findByStoreIdAndRelatedTypeAndRelatedIdOrderByCreatedAtDesc(
    Long storeId, String relatedType, Long relatedId);
```

### 3.2 推荐接口设计

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private SysNotificationRepository notificationRepo;

    /** 查询当前门店未读通知 */
    @GetMapping("/unread")
    public Result<List<SysNotification>> unread(@RequestParam Long storeId) {
        return Result.success(notificationRepo.findByStoreIdAndIsReadOrderByCreatedAtDesc(storeId, 0));
    }

    /** 标记为已读 */
    @PutMapping("/{id}/read")
    @Transactional
    public Result<?> markAsRead(@PathVariable Long id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.setIsRead(1);
            notificationRepo.save(n);
        });
        return Result.success();
    }

    /** 按业务查询通知历史 */
    @GetMapping("/by-business")
    public Result<List<SysNotification>> byBusiness(
            @RequestParam Long storeId,
            @RequestParam String relatedType,
            @RequestParam Long relatedId) {
        return Result.success(notificationRepo
            .findByStoreIdAndRelatedTypeAndRelatedIdOrderByCreatedAtDesc(storeId, relatedType, relatedId));
    }
}
```

> **注意**：以上接口为推荐实现，当前项目中尚未提供 `NotificationController`，可按需补全。

## 4. 状态机

```
[草稿 draft] ──发布──> [已发布 published] ──归档──> [已归档 archived]
                          │
                          └──读取──> is_read=1（已读，不改变 status）
```

- **draft**：草稿，不展示给用户（暂未使用，预留给定时通知）
- **published**：已发布，用户可见（默认）
- **archived**：已归档，超出保留期移至归档（定时任务清理 7+ 天前的通知）

## 5. 保留策略

- 消息 TTL：7 天（RabbitMQ 队列属性 `x-message-ttl`）
- 落库保留：30 天
- 归档清理：定时任务每天扫描 30 天前的 `published` 状态记录，改为 `archived`（保留数据，仅状态变更）

```sql
-- 定时任务（每天凌晨执行）
UPDATE sys_notification
SET status='archived', updated_at=NOW()
WHERE status='published'
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 6. 隔离规则

1. **store_id 强制**：所有写入必须带 `storeId`，未指定默认 0（全局）
2. **查询过滤**：前端查询必须按当前门店 `storeId` 过滤
3. **跨店可见**：`storeId=0` 的通知所有门店可见（如系统公告）
4. **GM 跨店**：GM 角色 `data_scope=all` 可查看所有门店通知

## 7. 使用场景示例

### 7.1 预订创建 → 通知前台

```java
// BookingController.createBooking() 内
notifyPublisher.publish(NotifyEvent.builder()
    .eventType(NotifyEvent.NotifyType.BOOKING_CREATED)
    .storeId(booking.getStoreId())
    .title("新预订 " + booking.getBookingId())
    .content(booking.getCustomerName() + " " + booking.getGuestCount() + "人 " + booking.getBookingDate())
    .priority(NotifyEvent.Priority.NORMAL)
    .receiverType(NotifyEvent.ReceiverType.ALL)
    .relatedType("booking")
    .relatedId(savedBooking.getId())
    .build());
```

### 7.2 库存低于阈值 → 通知采购员

```java
// InventoryService.checkLowStock() 内
if (currentQty < threshold) {
    notifyPublisher.publish(NotifyEvent.builder()
        .eventType(NotifyEvent.NotifyType.INVENTORY_LOW_STOCK)
        .storeId(storeId)
        .title("库存预警：" + ingredient.getName())
        .content("当前库存 " + currentQty + " " + ingredient.getUnit() + "，低于阈值 " + threshold)
        .priority(NotifyEvent.Priority.HIGH)
        .receiverType(NotifyEvent.ReceiverType.ROLE)
        .receiverIds("3")  // 店长角色
        .relatedType("ingredient")
        .relatedId(ingredient.getId())
        .build());
}
```

### 7.3 报销审批通过 → 通知申请人 + 财务

```java
// ApprovalService.approve() 内
notifyPublisher.publish(NotifyEvent.builder()
    .eventType(NotifyEvent.NotifyType.REIMBURSEMENT_APPROVED)
    .storeId(reimbursement.getStoreId())
    .title("报销已通过：" + reimbursement.getAmount() + "元")
    .content("报销单 " + reimbursement.getReimbursementNo() + " 已审批通过，请等待财务出款")
    .priority(NotifyEvent.Priority.NORMAL)
    .receiverType(NotifyEvent.ReceiverType.STAFF)
    .receiverIds(reimbursement.getApplicantId() + ",finance_dept")
    .relatedType("reimbursement")
    .relatedId(reimbursement.getReimbursementId())
    .build());
```

## 8. 监控与告警

建议监控指标：
- 每日落库通知数量（异常波动告警）
- 未读通知堆积数（>100 告警）
- MQ 发送失败次数（>10 告警）
- 消费者重试次数（>5 告警）

## 9. 验证查询

```sql
-- 按门店+类型统计通知数
SELECT
  store_id,
  notify_type,
  priority,
  COUNT(*) AS cnt,
  SUM(CASE WHEN is_read=0 THEN 1 ELSE 0 END) AS unread_cnt
FROM sys_notification
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY store_id, notify_type, priority
ORDER BY store_id, cnt DESC;

-- 查询某预订的所有通知
SELECT * FROM sys_notification
WHERE related_type='booking' AND related_id=?
ORDER BY created_at;
```
