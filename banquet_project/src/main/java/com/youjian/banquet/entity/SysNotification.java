package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统通知实体 — 对应数据库 sys_notification 表。
 * <p>MQ 异步通知链路落库表，所有推送事件最终都持久化到此表供前端轮询 / 查询。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_notification")
public class SysNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notify_id")
    private Long notifyId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** 通知类型：order.created / booking.confirmed / inventory.low_stock 等 */
    @Column(name = "notify_type", nullable = false, length = 50)
    private String notifyType;

    @Column(name = "notify_title", nullable = false, length = 200)
    private String notifyTitle;

    @Column(name = "notify_content", columnDefinition = "text")
    private String notifyContent;

    /** 优先级：low / normal / high / urgent */
    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "sender_id")
    private Integer senderId;

    @Column(name = "sender_name", length = 50)
    private String senderName;

    @Column(name = "send_time")
    private LocalDateTime sendTime;

    /** 接收者类型：all / role / staff / dept */
    @Column(name = "receiver_type", length = 20)
    private String receiverType;

    /** 接收者ID 列表（逗号分隔），null=全体 */
    @Column(name = "receiver_ids", columnDefinition = "text")
    private String receiverIds;

    /** 关联业务类型：booking / order / purchase / reimbursement 等 */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    /** 是否已读：0=未读 1=已读 */
    @Column(name = "is_read")
    private Integer isRead;

    /** 状态：draft / published / archived */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (priority == null) priority = "normal";
        if (receiverType == null) receiverType = "all";
        if (isRead == null) isRead = 0;
        if (status == null) status = "published";
        if (sendTime == null) sendTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
