package com.youjian.banquet.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 给 Main/Tom 的写操作用——AI 真正执行一个已确认的提案时，不直接拼SQL，而是内部回环调用
 * 系统里已经存在、真人在后台点按钮时也在用的那些 REST 接口（BookingController等），
 * 让 JwtAuthInterceptor / StoreDataScopeAspect / AuditLogAspect 按现有方式真实触发一遍——
 * 门店隔离、字段校验、审计日志，跟真人操作完全一致，不需要在AI这条链路上重新实现一遍。
 * 但WeCom消息没有真实登录会话，所以这里现签一个短时效的内部服务token，代表张婧本人
 * （staff_id=201, super_admin, store_id=1）去调用——审计日志里显示的操作人就是真实的她。
 */
public final class InternalServiceTokenUtil {

    private InternalServiceTokenUtil() {
    }

    public static String mint(String jwtSecret, Long staffId, Long storeId, String role, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .claim("staffId", staffId)
                .claim("storeId", storeId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000L)) // 5分钟，用完即弃
                .signWith(key)
                .compact();
    }
}
