package com.youjian.banquet.service;

import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.notify.NotifyWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 通知事件消费者。
 * <p>接收 MQ 消息后分发到下游通道：
 * <ul>
 *   <li>WebSocket 实时推送到前端浏览器（厨房屏 / 管理员后台 / iPad）</li>
 *   <li>短信通道（阿里云/腾讯云短信，待补充）</li>
 * </ul>
 * 落库由生产者 {@link NotifyPublisher} 同步完成，消费者仅做下游分发。
 */
@Component
@ConditionalOnProperty(name = "app.notify.enabled", havingValue = "true")
public class NotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotifyConsumer.class);

    @Autowired
    private NotifyWebSocketHandler webSocketHandler;

    /**
     * 消费通知事件 — 通过 WebSocket 实时广播给所有在线前端。
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
            // WebSocket 实时推送给所有在线前端（管理员后台、厨房屏、iPad）
            webSocketHandler.broadcast(event);
            log.info("WebSocket 广播完成 eventType={} onlineSessions={}",
                    event.getEventType(), webSocketHandler.getOnlineCount());

            // TODO: 短信通道（客户类事件）
            if (isCustomerEvent(event.getEventType())) {
                log.debug("[短信通道待接入] {} - {}", event.getEventType(), event.getTitle());
            }
        } catch (Exception e) {
            log.error("通知分发失败 eventType={} err={}",
                    event.getEventType(), e.getMessage(), e);
        }
    }

    private boolean isCustomerEvent(String type) {
        return "booking.confirmed".equals(type)
                || "booking.cancelled".equals(type);
    }
}
