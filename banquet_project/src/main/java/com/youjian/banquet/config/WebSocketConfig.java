package com.youjian.banquet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import com.youjian.banquet.notify.NotifyWebSocketHandler;

/**
 * WebSocket 配置 — 通知推送通道。
 * <p>注册 /ws/notify 端点，供前端建立连接接收实时通知。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer, WebMvcConfigurer {

    @Autowired
    private NotifyWebSocketHandler notifyHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notifyHandler, "/ws/notify")
                .setAllowedOrigins("*");
    }
}
