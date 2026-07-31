package com.youjian.banquet.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.youjian.banquet.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作审计 AOP 切面。
 * <p>
 * 拦截所有写操作接口（{@code @PostMapping} / {@code @PutMapping} / {@code @DeleteMapping}），
 * 自动记录操作人ID、门店ID、操作时间、操作数据，写入 {@code audit_logs} 表。
 * <p>
 * 写入字段映射（复用已有 audit_logs 表结构）：
 * <ul>
 *   <li>user_id   ← staffId（未登录记为 anonymous）</li>
 *   <li>action    ← HTTP方法 + 请求URI，如 "POST /api/hr/staff"</li>
 *   <li>target    ← Controller类名.方法名</li>
 *   <li>detail    ← JSON：方法入参 + 执行结果(success/error) + 耗时</li>
 *   <li>store_id  ← 当前用户 storeId（未登录记 0）</li>
 *   <li>created_at ← 由数据库默认 unix_timestamp() 填充</li>
 * </ul>
 * <p>
 * 审计写入失败不影响业务流程（仅记录 warn 日志）。请求结束后清理 ThreadLocal 上下文。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_DETAIL_LEN = 2000;
    private static final int MAX_ERROR_LEN = 500;

    /** 详情序列化专用 ObjectMapper：容忍空 Bean，避免实体懒加载等导致序列化失败中断业务 */
    private static final ObjectMapper DETAIL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @Value("${jwt.secret:YJCY-Banquet-2026-Secret-Key-Wo002323}")
    private String jwtSecret;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 环绕所有写操作接口：执行前建立用户上下文，执行后（无论成功失败）写入审计日志。
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object aroundWriteOperation(ProceedingJoinPoint pjp) throws Throwable {
        boolean populatedHere = ensureUserContext();
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                writeAuditLog(pjp, error, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("[Audit] 审计日志写入异常(已忽略): {}", ex.getMessage());
            } finally {
                if (populatedHere) {
                    UserContext.clear();
                }
            }
        }
    }

    /**
     * 写入一条审计日志到 audit_logs 表。
     */
    private void writeAuditLog(ProceedingJoinPoint pjp, Throwable error, long elapsedMs) {
        Long staffId = UserContext.getStaffId();
        Long storeId = UserContext.getStoreId();
        String userId = staffId == null ? "anonymous" : String.valueOf(staffId);
        long storeIdVal = storeId == null ? 0L : storeId;
        String action = resolveAction();
        String target = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();
        String detail = buildDetail(pjp.getArgs(), error, elapsedMs);

        try {
            jdbcTemplate.update(
                    "INSERT INTO audit_logs (user_id, action, target, detail, store_id) VALUES (?, ?, ?, ?, ?)",
                    userId, action, target, detail, storeIdVal
            );
            log.debug("[Audit] 写入成功 action={} target={} staffId={} storeId={}", action, target, staffId, storeId);
        } catch (Exception ex) {
            log.warn("[Audit] 写入 audit_logs 失败(已忽略): {}", ex.getMessage());
        }
    }

    /**
     * 解析操作动作：HTTP方法 + 请求URI。
     */
    private String resolveAction() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "UNKNOWN";
    }

    /**
     * 构造审计详情 JSON：入参类型/值 + 执行结果 + 耗时。
     * 过滤掉 Servlet / MultipartFile 等不可序列化的入参。
     */
    private String buildDetail(Object[] args, Throwable error, long elapsedMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"args\":").append(argsToJson(args));
        sb.append(",\"result\":\"").append(error == null ? "success" : "error").append("\"");
        if (error != null) {
            sb.append(",\"error\":\"").append(sanitize(error.getMessage())).append("\"");
        }
        sb.append(",\"elapsedMs\":").append(elapsedMs).append("}");
        String detail = sb.toString();
        if (detail.length() > MAX_DETAIL_LEN) {
            detail = detail.substring(0, MAX_DETAIL_LEN);
        }
        return detail;
    }

    private String argsToJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<Object> safe = new ArrayList<>();
        for (Object a : args) {
            if (a == null) {
                safe.add(null);
                continue;
            }
            String cn = a.getClass().getName();
            if (cn.startsWith("jakarta.") || cn.startsWith("javax.")
                    || cn.startsWith("org.springframework.")
                    || cn.contains("MultipartFile") || cn.contains("HttpServletRequest")
                    || cn.contains("HttpServletResponse")) {
                continue;
            }
            safe.add(a);
        }
        try {
            return DETAIL_MAPPER.writeValueAsString(safe);
        } catch (Exception e) {
            return "[\"<unserializable>\"]";
        }
    }

    private String sanitize(String msg) {
        if (msg == null) {
            return "";
        }
        String s = msg.replace("\"", "'").replace("\\", "/");
        return s.length() > MAX_ERROR_LEN ? s.substring(0, MAX_ERROR_LEN) : s;
    }

    /**
     * 解析当前请求用户并写入 ThreadLocal（与 StoreDataScopeAspect 同策略）。
     *
     * @return true 表示由本方法新填充了上下文（调用方负责在结束时清理）
     */
    private boolean ensureUserContext() {
        if (UserContext.get() != null) {
            return false;
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return false;
        }
        UserContext.CurrentUser user = resolveFromJwt(request);
        if (user == null) {
            user = resolveFromIpadHeaders(request);
        }
        if (user != null) {
            UserContext.set(user);
            return true;
        }
        return false;
    }

    private UserContext.CurrentUser resolveFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return UserContext.resolveFromToken(token, jwtSecret);
        }
        return null;
    }

    private UserContext.CurrentUser resolveFromIpadHeaders(HttpServletRequest request) {
        String storeIdStr = request.getHeader("X-Store-Id");
        String staffIdStr = request.getHeader("X-Staff-Id");
        if (storeIdStr == null || staffIdStr == null) {
            return null;
        }
        try {
            Long storeId = Long.parseLong(storeIdStr);
            Long staffId = Long.parseLong(staffIdStr);
            return new UserContext.CurrentUser(staffId, storeId, null, null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
