package com.youjian.banquet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知事件 DTO — MQ 消息体（跨进程序列化，需实现 Serializable）。
 * <p>对应档案第 5.2 节异步链路定义，事件类型见 {@link NotifyType}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型，见 {@link NotifyType} 常量 */
    private String eventType;

    /** 门店ID：0=全局，1=宁国，2=宣城 */
    private Long storeId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 优先级：low / normal / high / urgent */
    private String priority;

    /** 发送者ID（员工ID） */
    private Integer senderId;

    /** 发送者名称 */
    private String senderName;

    /** 接收者类型：all / role / staff / dept */
    private String receiverType;

    /** 接收者ID列表（逗号分隔） */
    private String receiverIds;

    /** 关联业务类型 */
    private String relatedType;

    /** 关联业务ID */
    private Long relatedId;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 业务备注 */
    private String remark;

    /**
     * 通知事件类型常量（对应档案第 5.2 节"期望异步链路"）
     */
    public static final class NotifyType {
        /** 订单创建 */
        public static final String ORDER_CREATED       = "order.created";
        /** 菜品出品完成 */
        public static final String DISH_SERVED         = "dish.served";
        /** 预订创建 */
        public static final String BOOKING_CREATED     = "booking.created";
        /** 预订取消 */
        public static final String BOOKING_CANCELLED   = "booking.cancelled";
        /** 预订确认 */
        public static final String BOOKING_CONFIRMED   = "booking.confirmed";
        /** 库存低于阈值 */
        public static final String INVENTORY_LOW_STOCK = "inventory.low_stock";
        /** 采购入库 */
        public static final String PURCHASE_RECEIVED   = "purchase.received";
        /** 报销审批通过 */
        public static final String REIMBURSEMENT_APPROVED = "reimbursement.approved";
        /** 工具领用 */
        public static final String TOOL_ISSUE         = "tool.issue";
        /** 工具归还 */
        public static final String TOOL_RETURN        = "tool.return";
        /** 工具损坏 */
        public static final String TOOL_DAMAGE        = "tool.damage";

        private NotifyType() {}
    }

    /** 优先级常量 */
    public static final class Priority {
        public static final String LOW     = "low";
        public static final String NORMAL  = "normal";
        public static final String HIGH   = "high";
        public static final String URGENT  = "urgent";
        private Priority() {}
    }

    /** 接收者类型常量 */
    public static final class ReceiverType {
        public static final String ALL    = "all";
        public static final String ROLE   = "role";
        public static final String STAFF  = "staff";
        public static final String DEPT   = "dept";
        private ReceiverType() {}
    }
}
