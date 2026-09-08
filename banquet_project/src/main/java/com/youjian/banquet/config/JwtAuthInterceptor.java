package com.youjian.banquet.config;

import com.youjian.banquet.auth.StaffRealtimeGuard;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${jwt.secret:}")
    private String jwtSecret;

    /**
     * 实时档案复核。
     * <p>
     * 用 required=false 是给"手工 new 出拦截器"的单元验收台留的余地——
     * 真实应用里 {@link StaffRealtimeGuard} 是 @Component，组件扫描一定装得上，
     * 因此生产路径始终带复核。缺 Bean 的那条路只会出现在没有 Spring 上下文的测试台上，
     * 攻击者删不掉一个 Bean。真出现缺失就按 ERROR 大声记一笔，不静悄悄降级。
     */
    @Autowired(required = false)
    private StaffRealtimeGuard staffRealtimeGuard;

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

            Long staffId = claims.get("staffId", Long.class);
            Long storeId = claims.get("storeId", Long.class);
            String role = claims.get("role", String.class);

            // ===== 实时复核：token 只证明"这串字符是我们签的"，不证明"这个人现在还是这个身份" =====
            // 离职、停用、调店、降权都发生在签发之后，而 token 还在有效期内。
            // 所以从这里往下，storeId 与 role 一律以库里当前值为准，不再用 token 里的。
            if (staffRealtimeGuard != null) {
                StaffRealtimeGuard.Verdict verdict = staffRealtimeGuard.verify(staffId);
                if (!verdict.ok()) {
                    // 不在册与已停用给同一句话：否则拿一串 staffId 试过去就能问出谁还在职。
                    log.warn("实时复核未通过，拒绝请求: {} {}", request.getMethod(), request.getRequestURI());
                    return sendUnauthorized(response, 401, "登录状态已失效，请重新登录");
                }
                storeId = verdict.storeId();
                role = verdict.role();
            } else {
                log.error("StaffRealtimeGuard 未装配，本次请求只验了签名，未复核在职/角色/门店: {} {}",
                        request.getMethod(), request.getRequestURI());
            }

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
            // 写下去的是复核之后的值：调店、降权在下一次请求就生效，不必等 token 过期。
            request.setAttribute("jwt_staff_id", staffId);
            request.setAttribute("jwt_store_id", storeId);
            request.setAttribute("jwt_role", role);
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
