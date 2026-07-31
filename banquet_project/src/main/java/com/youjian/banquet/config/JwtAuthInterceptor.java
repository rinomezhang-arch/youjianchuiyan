package com.youjian.banquet.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 全局鉴权拦截器
 * 硬约束：拦截全部 /api/** 接口，无Token/Token失效统一返回401
 * 仅放行 /api/auth/login 登录接口（在 WebMvcConfig 中通过 excludePathPatterns 配置）
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求放行，避免浏览器预检失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(response, 401, "未登录或缺少认证Token，请先登录");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return sendUnauthorized(response, 401, "Token不能为空，请先登录");
        }

        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.error("JWT 密钥未配置（环境变量 JWT_SECRET 未设置），拒绝所有受保护请求");
            return sendUnauthorized(response, 500, "服务端鉴权配置异常");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将 JWT Claims 中的用户信息放入 request 属性，供后续 Controller/拦截器使用
            request.setAttribute("jwt_staff_id", claims.get("staffId", Long.class));
            request.setAttribute("jwt_store_id", claims.get("storeId", Long.class));
            request.setAttribute("jwt_subject", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return sendUnauthorized(response, 401, "Token无效或已过期，请重新登录");
        }
    }

    /**
     * 统一返回 401 未授权 JSON 响应
     */
    private boolean sendUnauthorized(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\"}");
        return false;
    }
}
