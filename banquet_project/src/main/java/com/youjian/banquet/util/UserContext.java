package com.youjian.banquet.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 当前登录用户上下文（ThreadLocal 工具类）。
 * <p>
 * 存储 staffId / storeId / roleCode / username，供 AOP 切面、Service 层在请求生命周期内读取。
 * 由 {@code StoreDataScopeAspect} / {@code AuditLogAspect} 在请求入口解析 JWT 并写入，
 * 请求结束时必须调用 {@link #clear()} 释放，避免线程池场景下的 ThreadLocal 泄露。
 * <p>
 * 约定：store_id = 0 表示超级总经理（全局数据范围），store_id = 1/2 表示具体门店（仅本店数据）。
 */
public final class UserContext {

    private UserContext() {
    }

    /** 当前用户信息载体 */
    public static class CurrentUser {
        private Long staffId;
        private Long storeId;
        private String roleCode;
        private String username;

        public CurrentUser() {
        }

        public CurrentUser(Long staffId, Long storeId, String roleCode, String username) {
            this.staffId = staffId;
            this.storeId = storeId;
            this.roleCode = roleCode;
            this.username = username;
        }

        public Long getStaffId() {
            return staffId;
        }

        public void setStaffId(Long staffId) {
            this.staffId = staffId;
        }

        public Long getStoreId() {
            return storeId;
        }

        public void setStoreId(Long storeId) {
            this.storeId = storeId;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    private static final ThreadLocal<CurrentUser> CONTEXT = new ThreadLocal<>();

    /** 数据范围标记：true=不限制门店（总经理全门店），false=仅本门店 */
    private static final ThreadLocal<Boolean> DATA_SCOPE_ALL = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        CONTEXT.set(user);
    }

    public static CurrentUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
        DATA_SCOPE_ALL.remove();
    }

    public static Long getStaffId() {
        CurrentUser u = CONTEXT.get();
        return u == null ? null : u.getStaffId();
    }

    public static Long getStoreId() {
        CurrentUser u = CONTEXT.get();
        return u == null ? null : u.getStoreId();
    }

    public static String getRoleCode() {
        CurrentUser u = CONTEXT.get();
        return u == null ? null : u.getRoleCode();
    }

    public static String getUsername() {
        CurrentUser u = CONTEXT.get();
        return u == null ? null : u.getUsername();
    }

    /**
     * 标记当前请求的数据范围是否为全门店。
     * 由 {@code StoreDataScopeAspect} 在请求入口设置，下游 Repository/Service 据此决定是否拼接 store_id 过滤。
     */
    public static void setDataScopeAll(boolean allStores) {
        DATA_SCOPE_ALL.set(allStores);
    }

    /**
     * 是否允许查询全部门店数据（store_id = 0 总经理场景）。
     */
    public static boolean isDataScopeAll() {
        Boolean v = DATA_SCOPE_ALL.get();
        return v != null && v;
    }

    /**
     * 当前门店ID，若未设置返回 null。
     */
    public static Long currentStoreId() {
        return getStoreId();
    }

    /**
     * 当前门店ID，若未设置返回 null。{@link #currentStoreId()} 的语义化别名。
     */
    public static Long getCurrentStoreId() {
        return getStoreId();
    }

    /**
     * 是否为总经理（全门店数据范围）。
     * <p>
     * 同时支持两种判定方式，避免依赖切面是否设置了 DATA_SCOPE_ALL：
     * <ul>
     *   <li>{@link #isDataScopeAll()} 为 true（GET 请求由 StoreDataScopeAspect 设置）</li>
     *   <li>当前用户 storeId = 0（POST/PUT/DELETE 请求由 AuditLogAspect 仅填充用户身份）</li>
     * </ul>
     */
    public static boolean isGeneralManager() {
        if (isDataScopeAll()) {
            return true;
        }
        Long sid = getStoreId();
        if (sid != null && sid == 0L) {
            return true;
        }
        // super_admin role 同样视为总经理(跨门店)
        String role = getRoleCode();
        if (role != null && (role.equals("super_admin") || role.equals("admin"))) {
            return true;
        }
        return false;
    }

    /**
     * 断言当前用户为总经理，否则抛出 IllegalArgumentException。
     * 由 {@code GlobalExceptionHandler} 转换为 400 响应。
     */
    public static void assertGeneralManager() {
        if (!isGeneralManager()) {
            throw new IllegalArgumentException("无权限：仅总经理可执行此操作");
        }
    }

    /**
     * 断言当前用户可访问指定门店数据。
     * <p>
     * 总经理可访问任意门店；店长仅可访问本门店。storeId 为 null/空视为非法。
     */
    public static void assertStoreAccess(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            throw new IllegalArgumentException("缺少 storeId 参数");
        }
        try {
            assertStoreAccess(Long.parseLong(storeId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("storeId 参数格式非法: " + storeId);
        }
    }

    /**
     * 断言当前用户可访问指定门店数据（Long 重载）。
     */
    public static void assertStoreAccess(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("缺少 storeId 参数");
        }
        if (isGeneralManager()) {
            return;
        }
        Long current = getStoreId();
        if (current == null || !current.equals(storeId)) {
            throw new IllegalArgumentException("无权限：仅可操作本门店数据");
        }
    }

    /**
     * 写操作（POST/PUT/DELETE）入口调用：依据当前用户 storeId 兜底初始化 dataScopeAll 标记。
     * <p>
     * GET 请求由 {@code StoreDataScopeAspect} 自动设置标记；写操作由 {@code AuditLogAspect}
     * 仅填充 {@link CurrentUser}（含 storeId）但未设置 dataScopeAll，导致 {@link #isDataScopeAll()}
     * 永远返回 false。本方法按 storeId==0 兜底推导并填充标记，使下游 {@link #isDataScopeAll()}
     * 在写操作中也能正确反映"是否为总经理（全门店数据范围）"。
     * <p>
     * 调用约定：写操作 Controller 方法体首行调用本方法，然后用
     * {@code UserContext.isDataScopeAll()} 判断总经理权限，用
     * {@code UserContext.getCurrentStoreId()} 获取店长门店。
     *
     * @return 当前用户的 storeId（0 表示总经理，1/2 表示分店，null 表示未登录）
     */
    public static Long ensureDataScopeFromStoreId() {
        Long sid = getStoreId();
        if (sid != null && sid == 0L) {
            setDataScopeAll(true);
        } else {
            setDataScopeAll(false);
        }
        return sid;
    }

    /**
     * 从 JWT Token 解析当前用户信息。
     * <p>
     * 与 {@code AuthController} 的 Token 生成逻辑保持一致：
     * claim "staffId" / "storeId"，subject 为 username。
     * 解析失败返回 null，调用方按未登录处理。
     *
     * @param token    Bearer Token（纯 token，不含 "Bearer " 前缀）
     * @param jwtSecret JWT 签名密钥
     */
    public static CurrentUser resolveFromToken(String token, String jwtSecret) {
        if (token == null || token.isEmpty() || jwtSecret == null || jwtSecret.isEmpty()) {
            return null;
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
            String roleCode = claims.get("role", String.class);
            String username = claims.getSubject();
            return new CurrentUser(staffId, storeId, roleCode, username);
        } catch (Exception e) {
            return null;
        }
    }
}
