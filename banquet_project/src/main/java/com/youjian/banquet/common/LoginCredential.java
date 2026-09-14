package com.youjian.banquet.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * 登录凭证校验（PC 端 /api/auth/login 与 iPad 端 /api/ipad/login 共用）。
 *
 * 硬约束：
 *   1. 用户名去除首尾空白后必须符合账号格式，禁止空格、控制字符、超长输入；
 *   2. 密码不得为空或纯空白 —— 任何情况下都不允许"无密码进入"；
 *   3. 库中密码为空 / NULL 的账号一律拒绝登录。
 *      原实现只判 {@code password == null}，空字符串会走到
 *      {@code staffPassword.equals(password)}，若库里那条记录密码也是空串，
 *      不输密码即可登录成功。
 */
public final class LoginCredential {

    public static final int USERNAME_MIN = 2;
    public static final int USERNAME_MAX = 32;
    /** BCrypt 只取前 72 字节，超长输入直接拒绝 */
    public static final int PASSWORD_MAX = 72;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._@\\u4e00-\\u9fa5-]{" + USERNAME_MIN + "," + USERNAME_MAX + "}$");

    private LoginCredential() {
    }

    /** 去除首尾空白；null 或纯空白返回 null */
    public static String normalizeUsername(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /** 密码可用性：非 null、非纯空白、不超过 BCrypt 上限 */
    public static boolean isUsablePassword(String password) {
        return password != null && !password.trim().isEmpty() && password.length() <= PASSWORD_MAX;
    }

    /**
     * 明文密码与库中密码比对。支持 BCrypt（$2a$/$2b$/$2y$）与历史明文；
     * 库中密码为空一律返回 false。
     */
    public static boolean matches(BCryptPasswordEncoder encoder, String rawPassword, String storedPassword) {
        if (!isUsablePassword(rawPassword)) return false;
        if (storedPassword == null || storedPassword.trim().isEmpty()) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        // 历史明文密码：定长比较，避免按字符短路带来的时序差异
        return MessageDigest.isEqual(
                storedPassword.getBytes(StandardCharsets.UTF_8),
                rawPassword.getBytes(StandardCharsets.UTF_8));
    }
}
