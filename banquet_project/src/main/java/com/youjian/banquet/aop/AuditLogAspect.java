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
 *   <li>user_id   ← 已验证 staffId；仅设备身份的 iPad 请求记为 ipad-device:&lt;deviceSn&gt;；未登录记为 anonymous</li>
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
    private static final String IPAD_ATTR_STORE_ID = "ipad_store_id";
    private static final String IPAD_ATTR_STAFF_ID = "ipad_staff_id";
    private static final String IPAD_ATTR_DEVICE_SN = "ipad_device_sn";
    private static final String IPAD_DEVICE_ROLE = "ipad_device";
    private static final String IPAD_STAFF_ROLE = "ipad_staff";
    private static final String IPAD_DEVICE_USER_PREFIX = "ipad-device:";
    private static final int MAX_USER_ID_LEN = 64;
    private static final int MAX_DETAIL_LEN = 2000;
    private static final int MAX_ERROR_LEN = 500;

    /** 详情序列化专用 ObjectMapper：容忍空 Bean，避免实体懒加载等导致序列化失败中断业务 */
    private static final ObjectMapper DETAIL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

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
        String userId = resolveAuditUserId();
        long storeIdVal = storeId == null ? 0L : storeId;
        String action = resolveAction();
        String target = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();
        // Authentication bodies contain credentials; never serialize them or an exception
        // message which may repeat them. Other operation audit contracts stay unchanged.
        String detail = isCredentialOperation(pjp)
                ? "{\"args\":\"<credentials omitted>\",\"result\":\""
                    + (error == null ? "success" : "error") + "\",\"elapsedMs\":" + elapsedMs + "}"
                : buildDetail(pjp.getArgs(), error, elapsedMs);

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
     * 审计人只取服务端已经验证并放入 UserContext 的身份。
     * iPad 设备专用路由没有已验证员工时，用已验证设备序列号形成明确设备身份；
     * 其他空身份保持既有 anonymous 契约。
     */
    private String resolveAuditUserId() {
        UserContext.CurrentUser user = UserContext.get();
        if (user == null) {
            return "anonymous";
        }
        if (user.getStaffId() != null) {
            return String.valueOf(user.getStaffId());
        }
        if (IPAD_DEVICE_ROLE.equals(user.getRoleCode())
                && user.getUsername() != null && !user.getUsername().isBlank()) {
            String deviceUserId = IPAD_DEVICE_USER_PREFIX + user.getUsername();
            return deviceUserId.length() <= MAX_USER_ID_LEN
                    ? deviceUserId : deviceUserId.substring(0, MAX_USER_ID_LEN);
        }
        return "anonymous";
    }

    private boolean isCredentialOperation(ProceedingJoinPoint pjp) {
        String type = pjp.getSignature().getDeclaringType().getName();
        String method = pjp.getSignature().getName();
        return ((type.equals("com.youjian.banquet.controller.AuthController")
                    || type.equals("com.youjian.banquet.controller.IpadAuthController")) && method.equals("login"))
                || (type.equals("com.youjian.banquet.controller.IpadGuestOrderController") && method.equals("authorize"))
                || (type.equals("com.youjian.banquet.controller.IpadOrderController")
                    && (method.equals("authVerify") || method.equals("addDishesBatch")));
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
            user = resolveFromVerifiedIpadAttributes(request);
        }
        if (user != null) {
            UserContext.set(user);
            return true;
        }
        refuseUnverifiedIdentity(request);
        return false;
    }

    /**
     * 只认 JwtAuthInterceptor <b>复核之后</b>写入的 request 属性。
     * <p>
     * 原来这里是拿 Authorization 头自己再解析一遍 token。那等于绕过拦截器刚做完的实时复核，
     * 把签发那一刻的快照重新当成当前权威——人调了店、降了权，这份身份还是旧的，
     * 而门店数据范围与审计人都由它决定。
     */
    private UserContext.CurrentUser resolveFromJwt(HttpServletRequest request) {
        return UserContext.fromVerifiedAttributes(
                request.getAttribute(UserContext.ATTR_STAFF_ID),
                request.getAttribute(UserContext.ATTR_STORE_ID),
                request.getAttribute(UserContext.ATTR_ROLE),
                request.getAttribute(UserContext.ATTR_SUBJECT));
    }

    /**
     * 带着 Authorization 头进来、却没有任何一种已验证身份属性，说明这条路径没经过鉴权拦截器。
     * <b>这种情况必须拒绝，不能"没有身份就继续跑"</b>——继续跑意味着门店范围与审计人全部落空，
     * 而落空往往被下游当成"不限门店"。
     * <p>
     * 抛异常而不是返回 401 是因为切面的返回类型随被切方法而变，塞不进统一的错误体；
     * 全局异常处理会把它变成 500。这条路只可能由服务端配置疏漏触发（拦截器没挂上这个路径），
     * 500 恰好是它该有的语义——是服务端的问题，不是调用方没登录。
     */
    private void refuseUnverifiedIdentity(HttpServletRequest request) {
        boolean bearer = request.getHeader(AUTH_HEADER) != null
                && request.getHeader(AUTH_HEADER).startsWith(BEARER_PREFIX);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean ipadPath = path.equals("/api/ipad") || path.startsWith("/api/ipad/");
        if (bearer || ipadPath) {
            log.error("受保护请求没有经过身份复核，拒绝执行: {} {}",
                    request.getMethod(), request.getRequestURI());
            throw new SecurityException("鉴权链未生效：缺少已验证的身份属性");
        }
    }

    /**
     * 只认 IpadInterceptor 校验设备绑定后写入的 request 属性。
     * 客户端 X-Store-Id / X-Staff-Id 只是待校验输入，绝不能成为审计身份回退来源。
     */
    private UserContext.CurrentUser resolveFromVerifiedIpadAttributes(HttpServletRequest request) {
        Object storeId = request.getAttribute(IPAD_ATTR_STORE_ID);
        Object staffId = request.getAttribute(IPAD_ATTR_STAFF_ID);
        Object deviceSn = request.getAttribute(IPAD_ATTR_DEVICE_SN);
        if (!(storeId instanceof Number store) || deviceSn == null) {
            return null;
        }
        String verifiedDeviceSn = String.valueOf(deviceSn).trim();
        if (verifiedDeviceSn.isEmpty() || (staffId != null && !(staffId instanceof Number))) {
            return null;
        }
        Long verifiedStaffId = staffId instanceof Number staff ? staff.longValue() : null;
        return new UserContext.CurrentUser(
                verifiedStaffId,
                store.longValue(),
                verifiedStaffId == null ? IPAD_DEVICE_ROLE : IPAD_STAFF_ROLE,
                verifiedDeviceSn);
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
