package com.youjian.banquet.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 小程序顾客身份的JWT签发/校验——刻意用一把独立的密钥（CUSTOMER_JWT_SECRET），
 * 不跟员工JWT_SECRET共用。这样顾客token物理上不可能被 JwtAuthInterceptor
 * （员工/后台那套鉴权）验签通过，就算某个员工接口的角色判断写得不够严谨，
 * 顾客token也天然进不去——不依赖每个接口自己判断"这是不是员工token"。
 */
public final class CustomerJwtUtil {

    private static final long EXPIRE_MS = 30L * 24 * 60 * 60 * 1000; // 30天，小程序端体验上不想让用户频繁重新登录

    private CustomerJwtUtil() {
    }

    public static String mint(String secret, Long customerId, String openId) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(openId)
                .claim("customerId", customerId)
                .claim("openId", openId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(key)
                .compact();
    }

    /** 校验失败（过期/签名不对/密钥不对）直接抛异常，调用方按401处理。 */
    public static Claims verify(String secret, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
