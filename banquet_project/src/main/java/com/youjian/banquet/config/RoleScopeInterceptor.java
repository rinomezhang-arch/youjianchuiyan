package com.youjian.banquet.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

/**
 * 外部角色作用域拦截器。
 *
 * <p>背景：律师（张炬，role={@code lawyer}）作为外部顾问被写进花名册 staff_master，
 * 才能复用 /api/auth/login 登录。但花名册身份同时也是餐饮系统的身份 —— 拿这个
 * 账号的 JWT 去调 /api/finance/**、/api/hr/**、/api/staff/** 等接口，
 * 原先没有任何拦截，等于外部律师可以读整个餐饮经营与人事数据。
 *
 * <p>本拦截器对"外部角色"实行默认拒绝：只放行身份自查与其被授权的接口前缀，
 * 其余 /api/** 一律 403。内部员工角色不受影响，行为与改动前完全一致。
 */
@Component
public class RoleScopeInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleScopeInterceptor.class);

    /** 受限的外部角色。默认只有律师。 */
    @Value("${security.external-roles:lawyer}")
    private String externalRoles;

    /** 外部角色允许访问的接口前缀。默认只有法务案卷与身份自查。 */
    @Value("${security.external-role-allowed-prefixes:/api/legal,/api/auth/me,/api/auth/logout}")
    private String allowedPrefixes;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Object roleObj = request.getAttribute("jwt_role");
        if (roleObj == null) {
            // 无 JWT 身份的请求由 JwtAuthInterceptor 负责，这里不重复处理
            return true;
        }
        String role = String.valueOf(roleObj).trim();
        if (!isExternalRole(role)) {
            return true;
        }

        String path = request.getRequestURI();
        if (isAllowed(path)) {
            return true;
        }

        log.warn("[RoleScope] 外部角色 {} 越权访问被拒 user={} path={}",
                role, request.getAttribute("jwt_subject"), path);
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"该账号仅被授权查阅法务案卷，无权访问其他板块\"}");
        return false;
    }

    private boolean isExternalRole(String role) {
        for (String r : externalRoles.split(",")) {
            if (!r.isBlank() && r.trim().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowed(String path) {
        if (path == null) {
            return false;
        }
        for (String p : allowedPrefixes.split(",")) {
            String prefix = p.trim();
            if (!prefix.isEmpty() && (path.equals(prefix) || path.startsWith(prefix + "/"))) {
                return true;
            }
        }
        return false;
    }
}
