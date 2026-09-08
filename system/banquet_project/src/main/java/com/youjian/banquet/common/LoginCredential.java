package com.youjian.banquet.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * 登录凭证校验工具（PC 端与 iPad 端共用）
 *
 * 硬约束：
 * 1. 用户名必须非空、去除首尾空白后符合账号格式（禁止空格 / 控制字符 / 超长输入）
 * 2. 密码必须非空且不得为纯空白字符 —— 任何情况下都不允许"无密码进入"
 * 3. 库中密码为空 / NULL 的账号一律拒绝登录，避免历史脏数据造成空口令登录
 */
public final class LoginCredential {

    /** 账号最大长度（staff_account / staff_phone 均为 varchar(20)，这里放宽到 32 做兜底） */
    public static final int USERNAME_MAX = 32;
    public static final int USERNAME_MIN = 2;
    /** BCrypt 只取前 72 字节，超长输入直接拒绝 */
    public static final int PASSWORD_MAX = 72;

    /** 账号：字母 / 数字 / . _ - @ / 中文，2~32 位，不含空白与控制字符 */
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._@\\u4e00-\\u9fa5-]{" + USERNAME_MIN + "," + USERNAME_MAX + "}$");

    private LoginCredential() {
    }

    /** 去除首尾空白；null 或纯空白返回 null */
    public static String normalizeUsername(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 用户名格式校验 */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /** 密码可用性校验：非 null、非纯空白、长度不超过 BCrypt 上限 */
    public static boolean isUsablePassword(String password) {
        return password != null && !password.trim().isEmpty() && password.length() <= PASSWORD_MAX;
    }

    /**
     * 校验明文密码与库中存储的密码是否匹配。
     * 支持 BCrypt（$2a$/$2b$/$2y$ 前缀）与历史明文密码；
     * 库中密码为空一律返回 false，杜绝空口令账号被登录。
     */
    public static boolean matches(BCryptPasswordEncoder encoder, String rawPassword, String storedPassword) {
        if (!isUsablePassword(rawPassword)) {
            return false;
        }
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        // 历史明文密码：定长比较，避免按字符短路带来的时序差异
        return MessageDigest.isEqual(
                storedPassword.getBytes(StandardCharsets.UTF_8),
                rawPassword.getBytes(StandardCharsets.UTF_8));
    }
}
