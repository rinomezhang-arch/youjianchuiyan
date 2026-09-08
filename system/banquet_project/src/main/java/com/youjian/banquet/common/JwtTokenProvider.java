package com.youjian.banquet.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发器
 *
 * 硬约束：jwt.secret 未配置或长度不足 32 字节时直接抛异常，
 * 禁止在密钥缺失的情况下签发任何"看起来能用"的凭证。
 */
@Component
public class JwtTokenProvider {

    /** HS256 要求密钥不短于 256 bit */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String generate(Long staffId, Long storeId, String role, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(requireSecret().getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .claim("staffId", staffId)
                .claim("storeId", storeId)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpiration))
                .signWith(key)
                .compact();
    }

    private String requireSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT 密钥未配置（环境变量 JWT_SECRET 未设置），拒绝签发凭证");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT 密钥长度不足 " + MIN_SECRET_BYTES + " 字节，拒绝签发凭证");
        }
        return jwtSecret;
    }
}
