package com.youjian.banquet.service;

import com.youjian.banquet.dto.NotifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 通知事件消费者。
 * <p>仅在 {@code app.notify.enabled=true} 时启用。
 * <p>接收 MQ 消息后分发到下游通道：
 * <ul>
 *   <li>KDS 厨房屏（通过 WebSocket 推送，待补充）</li>
 *   <li>前台看板（通过 WebSocket 推送，待补充）</li>
 *   <li>短信通道（阿里云/腾讯云短信，待补充）</li>
 * </ul>
 * 落库由生产者 {@link NotifyPublisher} 同步完成，消费者仅做下游分发。
 */
@Component
@ConditionalOnProperty(name = "app.notify.enabled", havingValue = "true")
public class NotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotifyConsumer.class);

    /**
     * 消费通知事件。
     * <p>下游通道尚未接入时仅记录日志，便于后续扩展。
     *
     * @param event 通知事件
     */
    @RabbitListener(queues = "${app.notify.queue}")
    public void onNotify(NotifyEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("收到空通知事件，忽略");
            return;
        }
        log.info("收到 MQ 通知 eventType={} storeId={} title={}",
                event.getEventType(), event.getStoreId(), event.getTitle());

        try {
            // TODO: WebSocket 推送到 KDS 厨房屏
            if (isKitchenEvent(event.getEventType())) {
                log.debug("[KDS 待接入] {} - {}", event.getEventType(), event.getTitle());
            }

            // TODO: WebSocket 推送到前台看板
            if (isFrontDeskEvent(event.getEventType())) {
                log.debug("[前台看板待接入] {} - {}", event.getEventType(), event.getTitle());
            }

            // TODO: 短信通道（客户类事件）
            if (isCustomerEvent(event.getEventType())) {
                log.debug("[短信通道待接入] {} - {}", event.getEventType(), event.getTitle());
            }
        } catch (Exception e) {
            log.error("通知分发失败 eventType={} err={}",
                    event.getEventType(), e.getMessage(), e);
            // 不抛异常，避免消息被反复重投
        }
    }

    private boolean isKitchenEvent(String type) {
        return "order.created".equals(type)
                || "dish.served".equals(type)
                || "kitchen.order_timeout".equals(type);
    }

    private boolean isFrontDeskEvent(String type) {
        return "booking.created".equals(type)
                || "booking.cancelled".equals(type)
                || "booking.confirmed".equals(type)
                || "table.occupancy_warn".equals(type);
    }

    private boolean isCustomerEvent(String type) {
        return "booking.confirmed".equals(type)
                || "booking.cancelled".equals(type);
    }
}
