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
        // 现实账号的总经理 store_id 都是具体门店号而非设计假设的 0，单靠 storeId==0 会让
        // 所有真实总经理账号永远进不了全门店分支，见 UserContext.hasGmRoleCode() 说明。
        if ((storeId != null && storeId == 0L) || UserContext.hasGmRoleCode()) {
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
        boolean ipadVerified = request.getAttribute("ipad_store_id") != null;
        if (bearer && !ipadVerified) {
            log.error("请求带了 Bearer 头却没有经过实时复核的身份属性，拒绝执行: {} {}",
                    request.getMethod(), request.getRequestURI());
            throw new SecurityException("鉴权链未生效：缺少已验证的身份属性");
        }
    }

    private UserContext.CurrentUser resolveFromIpadHeaders(HttpServletRequest request) {
        // 只信任 IpadInterceptor 在数据库校验设备绑定后写入的属性，绝不直接信任客户端头部。
        Object storeIdValue = request.getAttribute("ipad_store_id");
        Object staffIdValue = request.getAttribute("ipad_staff_id");
        if (!(storeIdValue instanceof Number storeId) || !(staffIdValue instanceof Number staffId)) {
            return null;
        }
        return new UserContext.CurrentUser(staffId.longValue(), storeId.longValue(), "ipad_operator", null);
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
