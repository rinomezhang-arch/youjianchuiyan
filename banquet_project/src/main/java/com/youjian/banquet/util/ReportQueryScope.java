package com.youjian.banquet.util;

/** Report-local scope validation; null is reserved for an explicitly permitted GM aggregate. */
public final class ReportQueryScope {
    private ReportQueryScope() {}

    public static Long resolve(String value) {
        if (UserContext.get() == null) throw new ScopeException(403, "缺少登录身份或门店权限");
        boolean gm = UserContext.isGeneralManager();
        Long own = UserContext.currentStoreId();
        if (!gm && (own == null || own <= 0)) throw new ScopeException(403, "缺少门店权限");
        String requested = value == null ? "" : value.trim();
        if (requested.isEmpty()) return gm ? null : own;
        if ("all".equalsIgnoreCase(requested)) {
            if (gm) return null;
            throw new ScopeException(403, "仅可查询本门店");
        }
        Long id;
        try {
            if (!requested.matches("[0-9]+")) throw new NumberFormatException();
            id = Long.valueOf(requested);
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new ScopeException(400, "storeId必须为正整数");
        }
        if (!gm && !own.equals(id)) throw new ScopeException(403, "仅可查询本门店");
        return id;
    }

    public static final class ScopeException extends RuntimeException {
        private final int status;
        public ScopeException(int status, String message) { super(message); this.status = status; }
        public int getStatus() { return status; }
    }
}
