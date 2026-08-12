package com.youjian.banquet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 开发期临时兼容：前端部分页面 request.get('/api/X') 显式加了 /api 前缀，
 * 但 axios baseURL 本身又是 /api，所以形成 "/api/api/X"，这里自动重写到 "/api/X"。
 * 等以后前端统一清理所有显式 "/api" 前缀后，可删除此 Filter。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DoubleApiStripFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        String afterCtx = (ctx == null || ctx.isEmpty()) ? uri : uri.substring(ctx.length());
        if (afterCtx.startsWith("/api/api/")) {
            String target = afterCtx.replaceFirst("^/api/api", "/api");
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }
        if ("/api/api".equals(afterCtx)) {
            request.getRequestDispatcher("/api").forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
