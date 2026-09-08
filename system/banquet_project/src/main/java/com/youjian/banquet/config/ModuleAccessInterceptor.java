package com.youjian.banquet.config;

import com.youjian.banquet.common.ModuleAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 板块访问拦截器
 *
 * 在 JWT 鉴权之后执行：对被列入 sys_staff_module_access 白名单的员工，
 * 只放行公共接口与其授权板块的接口，其余一律 403。
 *
 * 未被列入白名单的员工不受影响，行为与改动前一致。
 */
@Component
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ModuleAccessInterceptor.class);

    private final ModuleAccessService moduleAccessService;

    public ModuleAccessInterceptor(ModuleAccessService moduleAccessService) {
        this.moduleAccessService = moduleAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Object staffIdAttr = request.getAttribute("jwt_staff_id");
        if (!(staffIdAttr instanceof Long staffId)) {
            // 没有 JWT 身份的请求由 JwtAuthInterceptor 负责拦截，这里不重复处理
            return true;
        }

        List<String> allowedModules = moduleAccessService.allowedModules(staffId);
        if (allowedModules.isEmpty()) {
            return true;
        }

        String path = request.getRequestURI();
        if (moduleAccessService.isApiAllowed(allowedModules, path)) {
            request.setAttribute("allowed_modules", allowedModules);
            return true;
        }

        log.warn("【板块越权拦截】staffId={}, 授权板块={}, 被拒接口={}", staffId, allowedModules, path);
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"无权访问该板块，请联系管理员\"}");
        return false;
    }
}
