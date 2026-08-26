package com.youjian.banquet.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.dto.NotifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * WebSocket 通知处理器 — 维护所有前端连接，支持定向广播。
 * <p>连接可通过 URL 参数传入 storeId 和 staffId 做过滤：
 *   ws://host/ws/notify?storeId=1&staffId=200
 */
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyWebSocketHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();

    /** 所有在线会话（线程安全）*/
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    /** 每个会话的属性：storeId、staffId */
    private final Map<String, Map<String, String>> sessionAttrs = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        Map<String, String> attrs = parseQuery(query);
        sessionAttrs.put(session.getId(), attrs);
        sessions.add(session);
        log.info("WebSocket 客户端已连接 id={} storeId={} staffId={} total={}",
                session.getId(), attrs.get("storeId"), attrs.get("staffId"), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sessionAttrs.remove(session.getId());
        log.info("WebSocket 客户端已断开 id={} total={}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可发送 {"type":"ping"} 做心跳
        String payload = message.getPayload();
        if (payload.contains("ping")) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            } catch (IOException ignored) {}
        }
    }

    /**
     * 广播通知事件给所有匹配的前端连接。
     *
     * @param event 通知事件
     */
    public void broadcast(NotifyEvent event) {
        if (event == null || sessions.isEmpty()) return;

        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("通知序列化失败: {}", e.getMessage());
            return;
        }

        TextMessage msg = new TextMessage(json);
        Long eventStoreId = event.getStoreId();

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;

            Map<String, String> attrs = sessionAttrs.get(session.getId());
            String clientStoreId = attrs != null ? attrs.get("storeId") : null;

            // 过滤：事件 storeId=0 表示全局广播，推给所有人
            // 否则只推给同门店的客户端
            if (eventStoreId != null && eventStoreId != 0L && clientStoreId != null) {
                if (!clientStoreId.equals(String.valueOf(eventStoreId))) {
                    continue;
                }
            }

            try {
                session.sendMessage(msg);
            } catch (IOException e) {
                log.warn("WebSocket 发送失败 sessionId={}: {}", session.getId(), e.getMessage());
            }
        }
    }

    /** 当前在线会话数 */
    public int getOnlineCount() {
        return sessions.size();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
