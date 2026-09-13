package com.youjian.banquet.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 助手人设名称解析。
 * <p>
 * 员工端 AI 助手按登录角色显示不同名字：超管/总经理看到 "Main"，其余在职员工看到 "Tom"。
 * 判定口径与 {@code UserContext.hasGmRoleCode()} 保持一致（gm/super_admin/admin 视为总经理级别）。
 * 角色来自 {@code JwtAuthInterceptor} 写入的 request 属性 "jwt_role"，不依赖 UserContext 的
 * ThreadLocal（AI 相关接口未经过 StoreDataScopeAspect/AuditLogAspect，不会自动填充 UserContext）。
 */
public final class AiPersonaUtil {

    private AiPersonaUtil() {
    }

    public static String resolveRole(HttpServletRequest request) {
        Object role = request.getAttribute("jwt_role");
        return role != null ? role.toString() : null;
    }

    public static boolean isGmRole(String role) {
        return role != null && (role.equals("gm") || role.equals("super_admin") || role.equals("admin"));
    }

    /** 返回当前登录员工应看到的 AI 人设名字："Main"（超管/总经理）或 "Tom"（普通员工）。 */
    public static String personaName(HttpServletRequest request) {
        return isGmRole(resolveRole(request)) ? "Main" : "Tom";
    }
}
