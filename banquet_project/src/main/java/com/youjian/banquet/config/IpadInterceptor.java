package com.youjian.banquet.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * iPad API 拦截器：校验 X-* 头部并设置 request 属性
 * 硬约束：iPad API 必须包含 X-Store-Id, X-Staff-Id, X-Device-Sn, X-Client-Type: ipad
 *
 * 安全修复：
 * 1. 访问 /api/ipad/** 未携带 X-Client-Type: ipad 直接返回 403 禁止访问
 *    （原实现无头部时直接放行，存在绕过漏洞）
 * 2. 必须四项头部齐全且数值合法才允许放行
 */
@Component
public class IpadInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientType = request.getHeader("X-Client-Type");

        // 强制校验：访问 /api/ipad/** 必须携带 X-Client-Type: ipad，否则 403
        if (!"ipad".equals(clientType)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"禁止访问：iPad 接口必须携带 X-Client-Type: ipad 头部\"}");
            return false;
        }

        String storeIdStr = request.getHeader("X-Store-Id");
        String staffIdStr = request.getHeader("X-Staff-Id");
        String deviceSn = request.getHeader("X-Device-Sn");

        // 强制校验四个头部
        if (storeIdStr == null || storeIdStr.isEmpty()
                || staffIdStr == null || staffIdStr.isEmpty()
                || deviceSn == null || deviceSn.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"缺少 iPad 必需头部信息（X-Store-Id/X-Staff-Id/X-Device-Sn/X-Client-Type）\"}");
            return false;
        }

        try {
            Long storeId = Long.parseLong(storeIdStr);
            Long staffId = Long.parseLong(staffIdStr);
            request.setAttribute("ipad_store_id", storeId);
            request.setAttribute("ipad_staff_id", staffId);
            request.setAttribute("ipad_device_sn", deviceSn);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"X-Store-Id 和 X-Staff-Id 必须为数字\"}");
            return false;
        }

        return true;
    }
}
