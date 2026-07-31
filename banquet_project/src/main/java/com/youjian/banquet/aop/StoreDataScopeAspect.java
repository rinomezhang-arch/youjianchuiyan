package com.youjian.banquet.aop;

import com.youjian.banquet.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 门店数据隔离切面。
 * <p>
 * 拦截所有 {@code @GetMapping} 查询接口，在请求入口解析当前登录用户的 store_id 并写入
 * {@link UserContext}，同时根据 store_id 设置数据范围标记：
 * <ul>
 *   <li>store_id = 0（超级总经理）：{@code UserContext.setDataScopeAll(true)}，下游查询不拼接门店过滤，可查全门店数据</li>
 *   <li>store_id = 1 / 2（普通员工/店长）：{@code UserContext.setDataScopeAll(false)}，下游 Repository/Service 应依据
 *       {@code UserContext.currentStoreId()} 自动拼接 {@code where store_id = ?} 仅查本店数据</li>
 * </ul>
 * <p>
 * 用户身份解析优先级：
 * 1. ThreadLocal 中已存在的 {@link UserContext}（被更外层切面/拦截器预先填充）；
 * 2. PC 端 Authorization 头部的 JWT Token（claim staffId / storeId）；
 * 3. iPad 端 X-Store-Id / X-Staff-Id 头部。
 * <p>
 * 请求结束后 {@link #aroundGetMapping} 的 finally 块调用 {@link UserContext#clear()} 释放 ThreadLocal，
 * 避免线程池复用导致的上下文泄露。实际的 SQL 门店过滤由 Repository 层依据本切面建立的上下文完成。
 */
@Aspect
@Component
public class StoreDataScopeAspect {

    private static final Logger log = LoggerFactory.getLogger(StoreDataScopeAspect.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:YJCY-Banquet-2026-Secret-Key-Wo002323}")
    private String jwtSecret;

    /**
     * 环绕所有 @GetMapping 方法：建立用户上下文 + 数据范围标记后放行。
     */
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public Object aroundGetMapping(ProceedingJoinPoint pjp) throws Throwable {
        boolean populatedHere = ensureUserContext();
        applyDataScope();
        try {
            return pjp.proceed();
        } finally {
            // 仅当本切面填充了上下文时才清理，避免误清更外层切面建立的上下文
            if (populatedHere) {
                UserContext.clear();
            }
        }
    }

    /**
     * 根据当前用户 store_id 设置数据范围标记。
     * store_id = 0 → 全门店；其余 → 仅本门店。
     */
    private void applyDataScope() {
        Long storeId = UserContext.getStoreId();
        if (storeId != null && storeId == 0L) {
            UserContext.setDataScopeAll(true);
            log.debug("[DataScope] 总经理全局数据范围，staffId={}", UserContext.getStaffId());
        } else {
            UserContext.setDataScopeAll(false);
            log.debug("[DataScope] 门店数据范围 storeId={}", storeId);
        }
    }

    /**
     * 解析当前请求用户并写入 ThreadLocal。
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
        // 1. 优先解析 JWT（PC 端）
        UserContext.CurrentUser user = resolveFromJwt(request);
        // 2. 回退到 iPad 头部
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
