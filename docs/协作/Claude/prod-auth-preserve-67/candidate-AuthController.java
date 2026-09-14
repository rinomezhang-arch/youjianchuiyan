package com.youjian.banquet.controller;

import com.youjian.banquet.auth.StaffRealtimeGuard;
import com.youjian.banquet.common.Result;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 认证控制器
 * 硬约束：登录必须通过员工信息表 staff_master 验证用户存在性和唯一性
 * 密码支持 BCrypt 加密（兼容历史明文密码）
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录失败的唯一对外口径。
     * <p>
     * 原来"账号不存在或已停用"与"密码错误"是两句话，外部拿一个用户名就能问出这个人在不在册、
     * 在不在职。三种失败必须说同一句话、给同一个状态码。
     */
    private static final String LOGIN_FAILED = "账号或密码不正确";

    /**
     * 比对用的诱饵哈希。用户名不存在时也要真跑一次 BCrypt，让两条路径的耗时可比。
     * <p>
     * 不写死在源码里，进程启动后第一次用到时按随机串现算——它不是任何账号的凭据，
     * 只是一段用来消耗同等算力的密文。
     */
    private volatile String decoyHash;

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        log.info("【登录请求】用户名: {}", username);

        if (username == null || password == null) {
            log.warn("【登录失败】用户名或密码为空");
            return Result.error(400, "用户名和密码不能为空");
        }

        try {
            // 通过员工信息表 staff_master 验证用户存在性和唯一性
            // 姓名/账号/手机号/英文名 四选一都能登录——之前只认账号和手机号，员工习惯直接输真名登录会失败；
            // staff_en_name 是 2026-09-05 加的英文名列，用来把「张晓秋 / rino」这类同一个人的
            // 重复账号合成一条（原先 id200 拼音账号、id204 英文账号并存）。
            String sql = "SELECT * FROM staff_master WHERE (staff_phone = ? OR staff_account = ? OR staff_name = ? OR staff_en_name = ?) AND employment_status IN ('active', '在职') LIMIT 1";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, username, username, username, username);

            if (list.isEmpty()) {
                // 不能在这里就返回：直接返回的话，未知账号不走 BCrypt，
                // 而已知账号要走，两者的响应时间差本身就是一个可测的信号。
                // 跑一次诱饵比对，把耗时拉到同一量级，再给出与"密码错误"完全相同的回答。
                passwordEncoder.matches(password, decoyHash());
                log.warn("【登录失败】账号不存在或已停用（对外统一口径）");
                return Result.error(401, LOGIN_FAILED);
            }

            Map<String, Object> staff = list.get(0);
            String staffPassword = (String) staff.get("staff_password");

            // 密码校验：支持 BCrypt 和明文兼容
            boolean passwordMatch = false;
            if (staffPassword != null) {
                if (staffPassword.startsWith("$2a$") || staffPassword.startsWith("$2b$")) {
                    // BCrypt 加密密码
                    passwordMatch = passwordEncoder.matches(password, staffPassword);
                } else {
                    // 兼容历史明文密码
                    passwordMatch = staffPassword.equals(password);
                }
            }

            if (!passwordMatch) {
                log.warn("【登录失败】密码不匹配（对外统一口径）");
                return Result.error(401, LOGIN_FAILED);
            }

            // 生成 JWT Token
            Long staffId = ((Number) staff.get("staff_id")).longValue();
            Long storeId = ((Number) staff.get("store_id")).longValue();
            String role = (String) staff.get("role");
            String token = generateJwtToken(staffId, storeId, role, username);

            Map<String, Object> user = new HashMap<>();
            user.put("staffId", staff.get("staff_id"));
            user.put("staffName", staff.get("staff_name"));
            user.put("staffAccount", staff.get("staff_account"));
            user.put("department", staff.get("department"));
            user.put("role", staff.get("role"));
            user.put("position", staff.get("staff_position"));
            user.put("phone", staff.get("staff_phone"));
            user.put("permissionLevel", staff.get("permission_level"));

            // 从 store_master 表获取门店名称
            String storeName = getStoreName(storeId);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("storeId", storeId);
            data.put("storeName", storeName);

            log.info("【登录成功】用户: {}, 门店ID: {}, 门店: {}", username, storeId, storeName);
            return Result.success(data);
        } catch (Exception e) {
            // 细节只进日志，不进响应体：异常文案里可能带表名、列名、连接串片段。
            log.error("【登录异常】错误: {}", e.getMessage(), e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    /** 懒算一次并缓存：每次登录都现算一遍 BCrypt 太贵，算一次留着当固定诱饵就够了。 */
    private String decoyHash() {
        String cached = decoyHash;
        if (cached == null) {
            synchronized (this) {
                cached = decoyHash;
                if (cached == null) {
                    cached = passwordEncoder.encode(UUID.randomUUID().toString());
                    decoyHash = cached;
                }
            }
        }
        return cached;
    }

    @GetMapping("/auth/me")
    public Result<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();

        // 优先从 JWT Token 中解析用户信息
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                var claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long staffId = claims.get("staffId", Long.class);

                // 实时复核：签名过得了不等于这个人现在还在职。
                // 离职、停用、调店、降权都发生在签发之后，token 却还在有效期内。
                StaffRealtimeGuard.Verdict verdict = StaffRealtimeGuard.verify(jdbcTemplate, staffId);
                if (!verdict.ok()) {
                    // 与"token 解析失败"同一句话：否则拿一串 staffId 试过去就能问出谁还在职。
                    return Result.error(401, "身份验证失败，请重新登录");
                }
                // 门店以库里当前值为准，不用 token 里的——调店之后旧 token 不能还按旧门店看数据。
                Long storeId = verdict.storeId();

                String sql = "SELECT * FROM staff_master WHERE staff_id = ? LIMIT 1";
                List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, staffId);
                if (!list.isEmpty()) {
                    Map<String, Object> staff = list.get(0);
                    Map<String, Object> user = new HashMap<>();
                    user.put("staffId", staff.get("staff_id"));
                    user.put("staffName", staff.get("staff_name"));
                    user.put("staffAccount", staff.get("staff_account"));
                    user.put("department", staff.get("department"));
                    user.put("role", staff.get("role"));
                    user.put("position", staff.get("staff_position"));
                    user.put("phone", staff.get("staff_phone"));
                    user.put("permissionLevel", staff.get("permission_level"));
                    data.put("user", user);
                    data.put("storeId", storeId);
                    data.put("storeName", getStoreName(storeId));
                    return Result.success(data);
                }
            } catch (Exception e) {
                // 安全修复 N2：JWT 解析失败直接返回 401，不再回退到 X-Staff-Id 头部伪造身份
                return Result.error(401, "身份验证失败，请重新登录");
            }
        }

        // 安全修复 N2：删除 X-Staff-Id 头部回退分支（原实现允许任意客户端通过 X-Staff-Id 头部伪造身份查询任意员工信息含手机号）
        // 未携带有效 JWT Token 的请求直接拒绝，不再兜底返回默认门店
        return Result.error(401, "未授权访问，请先登录");
    }

    /**
     * 退出登录。
     * <p>
     * <b>约定必须说清楚，别让人以为这里会作废 token：</b>
     * 本系统的 JWT 是无状态的，服务端不保存已签发 token 的清单，也没有黑名单。
     * 因此本接口<b>不会</b>让已签发的 token 立即失效——它只是告诉前端"可以清掉本地凭据了"。
     * 一个被留存下来的 token，在过期之前签名照样验得过。
     * <p>
     * 真正兜住这件事的是<b>每次请求的实时档案复核</b>（见 {@link StaffRealtimeGuard}）：
     * 人一旦离职、停用、调店或降权，下一次请求就会被挡下，不必等 token 过期。
     * 也就是说：退出本身不作废 token，但"这个人还能不能做事"始终以库里当前状态为准。
     * <p>
     * 若日后要做到"退出即作废"，方向是签发时带 jti 并维护服务端失效清单，
     * 那会引入状态存储与集群一致性问题，属于会话机制改造，不在本接口的职责内。
     */
    @PostMapping("/auth/logout")
    public Result<String> logout() {
        return Result.success("退出成功");
    }

    @GetMapping("/stores")
    public Result<List<Map<String, Object>>> getStores() {
        List<Map<String, Object>> stores = new ArrayList<>();
        // 审计：门店列表必须来自真实数据库表 store_info，不允许任何硬编码回退
        String sql = "SELECT store_id, store_code, store_name, store_short_name, store_type, address, phone, status, sort_order FROM store_info WHERE status = 'open' ORDER BY sort_order, store_id";
        stores = jdbcTemplate.queryForList(sql);
        return Result.success(stores);
    }

    /** 生成 JWT Token */
    private String generateJwtToken(Long staffId, Long storeId, String role, String username) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .claim("staffId", staffId)
                .claim("storeId", storeId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    /** 从 store_info 表获取门店名称（真实数据库） */
    private String getStoreName(Long storeId) {
        // 审计：必须从真实数据库读取，不允许任何回退硬编码
        String storeSql = "SELECT store_name FROM store_info WHERE store_id = ? LIMIT 1";
        List<Map<String, Object>> storeList = jdbcTemplate.queryForList(storeSql, storeId);
        if (!storeList.isEmpty()) {
            Object name = storeList.get(0).get("store_name");
            if (name != null) {
                return name.toString();
            }
        }
        return "未知门店";
    }
}
