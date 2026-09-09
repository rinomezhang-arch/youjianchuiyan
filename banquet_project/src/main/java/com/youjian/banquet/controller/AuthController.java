package com.youjian.banquet.controller;

import com.youjian.banquet.common.LoginCredential;
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

    /** 统一的登录失败提示：不区分"账号不存在"与"密码错误"，避免账号枚举 */
    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误，请重新输入";

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = LoginCredential.normalizeUsername(body == null ? null : body.get("username"));
        String password = body == null ? null : body.get("password");

        log.info("【登录请求】用户名: {}", username);

        // 硬约束 1：用户名与密码均不得为空或纯空白，杜绝无密码进入。
        // 原实现只判 null，空字符串会一路走到密码比对；若库中该账号密码也是空串，
        // 不输密码即可登录成功。
        if (username == null || !LoginCredential.isUsablePassword(password)) {
            log.warn("【登录失败】用户名或密码为空");
            return Result.error(400, "用户名和密码不能为空");
        }

        // 硬约束 2：用户名格式校验，拒绝空格 / 控制字符 / 超长输入
        if (!LoginCredential.isValidUsername(username)) {
            log.warn("【登录失败】用户名格式非法: {}", username);
            return Result.error(400, "用户名格式不正确：仅支持 "
                    + LoginCredential.USERNAME_MIN + "~" + LoginCredential.USERNAME_MAX + " 位姓名/账号/手机号");
        }

        try {
            // 手机号 / 拼音账号 / 英文名 / 中文姓名，四种写法都能登录。
            //
            // 原实现把四者平铺成一个 OR 再 LIMIT 1：命中多条时静默取数据库返回的第一条，
            // 输同一个串可能今天登进 A、明天登进 B。改为按优先级逐项匹配，
            // 并要求"每一项内部唯一"——既保证三种写法都进得去，又不会落到别人账号上。
            // 逐项匹配还有个好处：某一项命中多条（例如两个同名员工）只影响这一项，
            // 当事人仍可用手机号或账号正常登录。
            Map<String, Object> staff = null;
            String matchedBy = null;
            for (String[] probe : new String[][]{
                    {"staff_phone", "手机号"},
                    {"staff_account", "账号"},
                    {"staff_en_name", "英文名"},
                    {"staff_name", "姓名"}}) {
                List<Map<String, Object>> hit;
                try {
                    hit = jdbcTemplate.queryForList(
                            "SELECT * FROM staff_master WHERE " + probe[0] + " = ? "
                                    + "AND employment_status IN ('active', '在职') LIMIT 2",
                            username);
                } catch (Exception columnMissing) {
                    // staff_en_name 是后加的列，老库里可能还没有；缺列只跳过这一项，不影响其余登录方式
                    log.debug("【登录】跳过 {} 匹配: {}", probe[0], columnMissing.getMessage());
                    continue;
                }
                if (hit.isEmpty()) {
                    continue;
                }
                if (hit.size() > 1) {
                    // 该项本身不唯一（如两个同名员工），不能凭它确定身份
                    log.error("【登录失败】{} 命中多条记录，无法确定身份: {}", probe[1], username);
                    return Result.error(409, "该" + probe[1] + "对应多名员工，请改用手机号登录或联系管理员");
                }
                staff = hit.get(0);
                matchedBy = probe[1];
                break;
            }

            if (staff == null) {
                log.warn("【登录失败】账号不存在或已停用: {}", username);
                return Result.error(401, LOGIN_FAILED_MESSAGE);
            }
            log.info("【登录】按{}匹配到员工: {}", matchedBy, username);
            String staffPassword = (String) staff.get("staff_password");

            // 硬约束 4：库中密码为空的账号一律拒绝（历史脏数据不得形成空口令登录）
            if (staffPassword == null || staffPassword.trim().isEmpty()) {
                log.error("【登录失败】账号未设置密码，拒绝登录: {}", username);
                return Result.error(401, "该账号尚未设置密码，请联系管理员重置后再登录");
            }

            if (!LoginCredential.matches(passwordEncoder, password, staffPassword)) {
                log.warn("【登录失败】密码错误: {}", username);
                return Result.error(401, LOGIN_FAILED_MESSAGE);
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
            log.error("【登录异常】用户: {}, 错误: {}", username, e.getMessage(), e);
            return Result.error(500, "登录失败: " + e.getMessage());
        }
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
                Long storeId = claims.get("storeId", Long.class);

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
