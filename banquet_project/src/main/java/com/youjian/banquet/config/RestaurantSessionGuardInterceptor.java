package com.youjian.banquet.config;

import com.youjian.banquet.auth.StaffRealtimeGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Revalidate restaurant sessions after the existing authentication interceptors. */
@Component
public class RestaurantSessionGuardInterceptor implements HandlerInterceptor {
    private final StaffRealtimeGuard guard;

    public RestaurantSessionGuardInterceptor(StaffRealtimeGuard guard) {
        this.guard = guard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || excluded(path)) return true;
        String tokenRole = normalize(request.getAttribute("jwt_role"));
        // The existing legal authentication and scope interceptors remain the authority.
        if ("lawyer".equals(tokenRole)) return true;

        boolean ipad = path.startsWith("/api/ipad/");
        Object staff = request.getAttribute(ipad ? "ipad_staff_id" : "jwt_staff_id");
        Object store = request.getAttribute(ipad ? "ipad_store_id" : "jwt_store_id");
        Long staffId = asLong(staff), storeId = asLong(store);
        if (staffId == null || staffId <= 0 || storeId == null || storeId < 0) return reject(response);
        StaffRealtimeGuard.Verdict current;
        try {
            current = guard.verify(staffId);
        } catch (RuntimeException failure) {
            return reject(response);
        }
        if (current == null || !current.ok() || current.storeId() == null || "lawyer".equals(current.role())) return reject(response);
        if (ipad) {
            // Global staff still have to pass the existing device/store binding validation.
            if (current.storeId() != 0L && !Objects.equals(storeId, current.storeId())) return reject(response);
        } else if (!Objects.equals(storeId, current.storeId()) || !Objects.equals(tokenRole, current.role())) {
            // Do not silently grant a newly assigned role or silently switch a token's store.
            return reject(response);
        }
        return true;
    }

    static boolean excluded(String path) {
        return path.equals("/api/legal") || path.startsWith("/api/legal/")
                || path.equals("/api/auth/login")
                || path.equals("/api/public") || path.startsWith("/api/public/")
                || path.equals("/api/actuator") || path.startsWith("/api/actuator/")
                || path.equals("/api/hr/self-service/submit")
                || path.equals("/api/hr/job-postings/open")
                || path.matches("/api/bookings/confirm/[^/]+");
    }

    private static Long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static String normalize(Object value) {
        return value == null ? null : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"账号状态已变更或无效，请重新登录\"}");
        return false;
    }
}
