package com.youjian.banquet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置。
 * <p>仅在 {@code app.notify.enabled=true} 时生效，未启用时不创建任何 Bean，
 * 保证应用在无 RabbitMQ 服务环境下也能正常启动。
 */
@Configuration
@ConditionalOnProperty(name = "app.notify.enabled", havingValue = "true")
public class RabbitMQConfig {

    @Value("${app.notify.queue}")
    private String queueName;

    @Value("${app.notify.exchange}")
    private String exchangeName;

    @Value("${app.notify.routing-key}")
    private String routingKey;

    /** 通知队列（durable=true，broker 重启后保留） */
    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-message-ttl", 7 * 24 * 60 * 60 * 1000L) // 7 天过期
                .build();
    }

    /** 通知交换机（direct，按 routing-key 精确路由） */
    @Bean
    public DirectExchange notifyExchange() {
        return ExchangeBuilder.directExchange(exchangeName).durable(true).build();
    }

    /** 绑定：队列 ↔ 交换机 ↔ routing-key */
    @Bean
    public Binding notifyBinding(Queue notifyQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(notifyQueue).to(notifyExchange).with(routingKey);
    }

    /** JSON 消息转换器（支持 LocalDateTime 等 Java 8 时间类型） */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    /** RabbitTemplate：使用 JSON 序列化发送消息 */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(exchangeName);
        template.setRoutingKey(routingKey);
        // 强制 confirm，发送失败时回调
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .warn("RabbitMQ 消息发送失败 ack=false cause={}", cause);
            }
        });
        return template;
    }
}
