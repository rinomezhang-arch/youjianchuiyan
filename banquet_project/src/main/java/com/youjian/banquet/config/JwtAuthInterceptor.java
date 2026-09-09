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

    /**
     * 外部人员的可访问路径白名单：key 是花名册里的 role（小写），value 是允许的路径前缀。
     * 凡是列在这里的角色，只能访问对应前缀，其余 /api/** 一律 403。
     *
     * lawyer = 代理律师，在通讯录里是一条正常记录（便于管理和收回），
     * 但绝不能看到经营数据、员工资料和客户信息。
     */
    private static final java.util.Map<String, java.util.List<String>> OUTSIDER_SCOPES =
            java.util.Map.of(
                    "lawyer", java.util.List.of("/api/legal/", "/api/auth/me", "/api/auth/logout")
            );

    /** 案卷会话 Cookie 名，与 LegalController 保持一致 */
    public static final String CASE_COOKIE = "legal_case";

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求放行，避免浏览器预检失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (isCasePageRequest(request)) {
            // 案卷正文是浏览器直接导航打开的页面，导航请求带不了 Authorization 头，
            // 因此这两个只读页面额外接受登录时下发的 legal_case Cookie。
            // Cookie 为 HttpOnly + SameSite=Strict + Path=/api/legal/case，
            // 只覆盖这两个 GET 页面，不会被用于任何写接口，故不引入 CSRF 面。
            token = readCaseCookie(request);
        }

        if (token == null || token.isEmpty()) {
            return sendUnauthorized(response, 401, "未登录或缺少认证Token，请先登录");
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

            String role = claims.get("role", String.class);

            // 外部人员按白名单收口：花名册里的外部角色（如代理律师）只能访问自己那块，
            // 其余业务接口一律拒绝。
            //
            // 为什么放在这里而不是各业务 Controller 里：本拦截器原先只验签名不判角色，
            // 而业务接口各自也没有统一的角色校验，导致 2026-09-05 实测中律师账号可以拉到
            // /api/hr/staff（含身份证、工资、家庭住址）、/api/customers、/api/bookings。
            // 用「默认拒绝 + 显式放行」的白名单在入口一次堵死，比给每个接口补黑名单可靠。
            if (role != null && OUTSIDER_SCOPES.containsKey(role.toLowerCase())) {
                String uri = request.getRequestURI();
                boolean allowed = false;
                for (String prefix : OUTSIDER_SCOPES.get(role.toLowerCase())) {
                    if (uri.startsWith(prefix)) { allowed = true; break; }
                }
                if (!allowed) {
                    log.warn("外部角色 {} 越权访问被拒: {} {}", role, request.getMethod(), uri);
                    return sendUnauthorized(response, 403, "该账号无权访问此功能");
                }
            }

            // 将 JWT Claims 中的用户信息放入 request 属性，供后续 Controller/拦截器使用
            request.setAttribute("jwt_staff_id", claims.get("staffId", Long.class));
            request.setAttribute("jwt_store_id", claims.get("storeId", Long.class));
            request.setAttribute("jwt_role", role);
            request.setAttribute("jwt_subject", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return sendUnauthorized(response, 401, "Token无效或已过期，请重新登录");
        }
    }

    /** 案卷正文页：仅 GET /api/legal/case 与 /api/legal/case/timeline 允许用 Cookie 认证 */
    private boolean isCasePageRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/legal/case".equals(path) || "/api/legal/case/timeline".equals(path);
    }

    /** 读取案卷会话 Cookie */
    private String readCaseCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie c : cookies) {
            if (CASE_COOKIE.equals(c.getName())) {
                String v = c.getValue();
                return v == null ? null : v.trim();
            }
        }
        return null;
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
