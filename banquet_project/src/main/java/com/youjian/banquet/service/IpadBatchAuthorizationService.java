package com.youjian.banquet.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;

/** Process-local, one-use capability. Not a login/JWT; restart or another node fails closed. */
@Service
public class IpadBatchAuthorizationService {
    private static final String PURPOSE = "ipad:batch-add";
    private final Map<String, Grant> grants = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    public IpadBatchAuthorizationService() { this(Clock.systemUTC()); }
    public IpadBatchAuthorizationService(Clock clock) { this.clock = clock; }
    private record Grant(long store, long staff, String device, String booking, String purpose, long expires) {}
    public synchronized String issue(long store, long staff, String device, String booking) {
        long now = clock.millis();
        grants.values().removeIf(g -> g.expires <= now);
        if (store <= 0 || staff <= 0 || staff > Integer.MAX_VALUE || device == null || device.isBlank() || booking == null || booking.isBlank())
            throw new IllegalArgumentException("授权范围无效");
        if (grants.size() >= 10000) throw new IllegalStateException("授权繁忙，请稍后重试");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        grants.put(token, new Grant(store,staff,device,booking,PURPOSE,now+120000));
        return token;
    }
    public synchronized Integer consume(String token, long store, String device, String booking, JdbcTemplate jdbc) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new SecurityException("请重新由店员授权");
        Grant g = grants.get(token);
        if (g == null || g.expires <= clock.millis() || g.store != store || !g.device.equals(device) || !g.booking.equals(booking) || !PURPOSE.equals(g.purpose))
            throw new SecurityException("授权已失效或不属于本次操作，请重新授权");
        // A valid scoped attempt consumes the grant even if staff has since left or JDBC fails.
        grants.remove(token);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM staff_master WHERE staff_id=? AND store_id=? AND employment_status IN ('active','在职')", Integer.class, g.staff, store);
        if (count == null || count != 1) throw new SecurityException("员工状态已变化，请重新授权");
        return Math.toIntExact(g.staff);
    }
}
