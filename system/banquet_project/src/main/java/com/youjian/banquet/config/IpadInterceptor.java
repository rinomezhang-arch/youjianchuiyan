package com.youjian.banquet.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

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

    private final JdbcTemplate jdbcTemplate;

    public IpadInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
        boolean loginRequest = "/api/ipad/login".equals(request.getRequestURI());

        // 登录前仅校验门店与绑定设备；登录后所有接口继续强制校验员工身份
        if (storeIdStr == null || storeIdStr.isEmpty()
                || deviceSn == null || deviceSn.isEmpty()
                || (!loginRequest && (staffIdStr == null || staffIdStr.isEmpty()))) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"缺少 iPad 必需头部信息\"}");
            return false;
        }

        try {
            Long requestedStoreId = Long.parseLong(storeIdStr);
            Long requestedStaffId = loginRequest ? null : Long.parseLong(staffIdStr);
            List<Map<String, Object>> bindings = jdbcTemplate.queryForList(
                    "SELECT store_id, staff_id FROM ipad_device_binding WHERE device_sn = ? AND status = 'active' LIMIT 1",
                    deviceSn);
            if (bindings.isEmpty()) {
                reject(response, 401, "设备未绑定或已停用，请联系门店管理员");
                return false;
            }
            Map<String, Object> binding = bindings.get(0);
            Long boundStoreId = ((Number) binding.get("store_id")).longValue();
            Long boundStaffId = binding.get("staff_id") == null ? null : ((Number) binding.get("staff_id")).longValue();
            if (!boundStoreId.equals(requestedStoreId)
                    || (!loginRequest && boundStaffId != null && !boundStaffId.equals(requestedStaffId))) {
                reject(response, 403, "设备身份与门店绑定不一致");
                return false;
            }
            request.setAttribute("ipad_store_id", boundStoreId);
            request.setAttribute("ipad_staff_id", loginRequest ? boundStaffId :
                    (boundStaffId == null ? requestedStaffId : boundStaffId));
            request.setAttribute("ipad_device_sn", deviceSn);
        } catch (NumberFormatException e) {
            reject(response, 400, "X-Store-Id 和 X-Staff-Id 必须为数字");
            return false;
        } catch (Exception e) {
            reject(response, 503, "设备身份校验暂不可用，请联系系统管理员");
            return false;
        }

        return true;
    }

    private void reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
