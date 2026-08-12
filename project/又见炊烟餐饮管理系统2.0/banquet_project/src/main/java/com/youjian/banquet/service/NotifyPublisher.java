package com.youjian.banquet.service;

import com.youjian.banquet.dto.NotifyEvent;
import com.youjian.banquet.entity.SysNotification;
import com.youjian.banquet.repository.SysNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 通知事件发布者。
 * <p>双模式工作：
 * <ul>
 *   <li>{@code app.notify.enabled=true}：通过 RabbitTemplate 异步发送到 MQ</li>
 *   <li>{@code app.notify.enabled=false}（默认）：直接同步落库 sys_notification</li>
 * </ul>
 * 业务方调用 {@link #publish(NotifyEvent)}，无需感知 MQ 是否启用。
 */
@Service
public class NotifyPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotifyPublisher.class);

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SysNotificationRepository notificationRepo;

    @Value("${app.notify.enabled:false}")
    private boolean notifyEnabled;

    @Value("${app.notify.exchange:youjian.notify.exchange}")
    private String exchange;

    @Value("${app.notify.routing-key:notify.event}")
    private String routingKey;

    /**
     * 发布通知事件。
     * <p>同步落库失败或 MQ 发送失败均不抛异常，仅记录日志，避免影响主业务流程。
     *
     * @param event 通知事件
     */
    public void publish(NotifyEvent event) {
        if (event == null) return;

        // 始终同步落库一份（确保不丢）
        try {
            SysNotification entity = toEntity(event);
            notificationRepo.save(entity);
        } catch (Exception e) {
            log.error("通知落库失败 eventType={} storeId={} err={}",
                    event.getEventType(), event.getStoreId(), e.getMessage(), e);
        }

        // 若启用 MQ，额外发送到 MQ 供下游消费（KDS / 短信 / 看板）
        if (notifyEnabled && rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, event);
                log.debug("MQ 通知已发送 eventType={} storeId={}",
                        event.getEventType(), event.getStoreId());
            } catch (Exception e) {
                log.warn("MQ 通知发送失败（已落库兜底）eventType={} err={}",
                        event.getEventType(), e.getMessage());
            }
        }
    }

    /** NotifyEvent → SysNotification 实体映射 */
    private SysNotification toEntity(NotifyEvent event) {
        SysNotification n = new SysNotification();
        n.setStoreId(event.getStoreId() != null ? event.getStoreId() : 0L);
        n.setNotifyType(event.getEventType());
        n.setNotifyTitle(event.getTitle());
        n.setNotifyContent(event.getContent());
        n.setPriority(event.getPriority());
        n.setSenderId(event.getSenderId());
        n.setSenderName(event.getSenderName());
        n.setSendTime(event.getTriggerTime());
        n.setReceiverType(event.getReceiverType());
        n.setReceiverIds(event.getReceiverIds());
        n.setRelatedType(event.getRelatedType());
        n.setRelatedId(event.getRelatedId());
        n.setRemark(event.getRemark());
        return n;
    }
}
