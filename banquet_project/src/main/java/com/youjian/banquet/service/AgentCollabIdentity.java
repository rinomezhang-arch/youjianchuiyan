package com.youjian.banquet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.config.ApprovalAuthorityInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** No employee JWT is ever minted here. AI tokens identify AI; human approval uses staff JWT and existing policy. */
@Component
public class AgentCollabIdentity {
    @Value("${agent.collab.tokens:${AGENT_COLLAB_TOKENS:}}") private String tokensJson;
    @Value("${jwt.secret:}") private String jwtSecret;
    @Autowired private ApprovalAuthorityInterceptor approvalPolicy;
    private static final Set<String> AGENTS = Set.of("codex","claude","dilong","tianlong","trae");

    public String agent(HttpServletRequest request) {
        String supplied = request.getHeader("X-Agent-Token");
        if (supplied == null || supplied.isBlank() || tokensJson == null || tokensJson.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"需要独立AI令牌");
        Map<String,String> tokens;
        try { tokens = new ObjectMapper().readValue(tokensJson,new TypeReference<Map<String,String>>(){}); }
        catch (Exception ignored) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI通道配置不可用"); }
        if (tokens.isEmpty() || !AGENTS.containsAll(tokens.keySet()) || tokens.values().stream().anyMatch(t -> t == null || t.length() < 32)
                || new HashSet<>(tokens.values()).size() != tokens.size())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI通道配置不可用");
        for (var item : tokens.entrySet())
            if (MessageDigest.isEqual(item.getValue().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
                return item.getKey();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"AI令牌无效");
    }

    public Human human(HttpServletRequest request) {
        // A human browser must not carry an AI credential; never fall back from AI to employee impersonation.
        if (request.getHeader("X-Agent-Token") != null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"人工批复不能使用AI通道");
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请使用真人登录会话");
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(header.substring(7).trim()).getPayload();
            if (claims.getExpiration() == null || claims.getSubject() == null || claims.getSubject().isBlank()) throw new IllegalArgumentException();
        } catch (RuntimeException ignored) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录会话无效或已过期"); }
        String role = claims.get("role",String.class);
        if (role == null || Set.of("lawyer","agent","ai","bot").contains(role.toLowerCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"该身份无权访问协作批复");
        Object rawId = claims.get("staffId");
        Long staffId = rawId instanceof Number ? ((Number)rawId).longValue() : null;
        String account = approvalPolicy.collaborationApprover(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"协作重要事项仅秋哥本人可批复"));
        return new Human(staffId,account);
    }

    public record Human(Long staffId,String account) {}
}
