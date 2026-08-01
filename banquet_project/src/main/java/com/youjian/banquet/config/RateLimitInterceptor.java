package com.youjian.banquet.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 限流拦截器：基于内存的请求计数，防止暴力破解
 * 1. 普通接口：每个IP每分钟最多60次请求
 * 2. 登录接口（/login）：每个IP每分钟最多5次请求
 * 3. 超过限制返回 429 Too Many Requests
 * 4. 每分钟自动清理过期计数，避免内存泄漏
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int DEFAULT_LIMIT = 60;            // 普通接口每分钟60次
    private static final int LOGIN_LIMIT = 5;               // 登录接口每分钟5次
    private static final long WINDOW_MS = 60_000;           // 1分钟窗口
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 清理间隔1分钟

    private final ConcurrentHashMap<String, RateInfo> ipCounts = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        // 启动守护线程定时清理过期计数，避免无效IP条目长期堆积导致内存泄漏
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries,
                CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求放行，避免浏览器预检失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        int limit = path.contains("/login") ? LOGIN_LIMIT : DEFAULT_LIMIT;
        String key = clientIp + ":" + (path.contains("/login") ? "login" : "default");

        RateInfo info = ipCounts.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart > WINDOW_MS) {
                return new RateInfo(1, now);
            }
            v.count.incrementAndGet();
            return v;
        });

        if (info.count.get() > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    /**
     * 清理已超过时间窗口的计数条目，避免内存泄漏
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        ipCounts.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private static class RateInfo {
        final AtomicInteger count;
        final long windowStart;

        RateInfo(int initial, long windowStart) {
            this.count = new AtomicInteger(initial);
            this.windowStart = windowStart;
        }
    }
}
