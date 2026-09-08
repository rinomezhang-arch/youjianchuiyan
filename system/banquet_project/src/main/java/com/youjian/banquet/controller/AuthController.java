package com.youjian.banquet.controller;

import com.youjian.banquet.common.JwtTokenProvider;
import com.youjian.banquet.common.LoginCredential;
import com.youjian.banquet.common.ModuleAccessService;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ModuleAccessService moduleAccessService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 统一的登录失败提示：不区分"账号不存在"与"密码错误"，避免账号枚举 */
    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误，请重新输入";

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = LoginCredential.normalizeUsername(body == null ? null : body.get("username"));
        String password = body == null ? null : body.get("password");

        log.info("【登录请求】用户名: {}", username);

        // 硬约束 1：用户名与密码均不得为空（含纯空白），杜绝无密码进入
        if (username == null || !LoginCredential.isUsablePassword(password)) {
            log.warn("【登录失败】用户名或密码为空");
            return Result.error(400, "用户名和密码不能为空");
        }

        // 硬约束 2：用户名格式校验，拒绝空格 / 控制字符 / 超长输入
        if (!LoginCredential.isValidUsername(username)) {
            log.warn("【登录失败】用户名格式非法: {}", username);
            return Result.error(400, "用户名格式不正确：仅支持 "
                    + LoginCredential.USERNAME_MIN + "~" + LoginCredential.USERNAME_MAX + " 位手机号或员工账号");
        }

        try {
            // 通过员工信息表 staff_master 验证用户存在性和唯一性（取 2 条用于唯一性判定）
            String sql = "SELECT * FROM staff_master WHERE (staff_phone = ? OR staff_account = ?) AND employment_status IN ('active', '在职') LIMIT 2";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, username, username);

            if (list.isEmpty()) {
                log.warn("【登录失败】账号不存在或已停用: {}", username);
                return Result.error(401, LOGIN_FAILED_MESSAGE);
            }

            // 硬约束 3：用户名必须唯一，命中多条说明数据异常，一律拒绝登录
            if (list.size() > 1) {
                log.error("【登录失败】账号在 staff_master 中存在重复记录: {}", username);
                return Result.error(409, "该账号存在重复记录，请联系管理员处理后再登录");
            }

            Map<String, Object> staff = list.get(0);
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

            Object staffIdValue = staff.get("staff_id");
            Object storeIdValue = staff.get("store_id");
            if (staffIdValue == null || storeIdValue == null) {
                log.error("【登录失败】员工档案缺少 staff_id/store_id: {}", username);
                return Result.error(500, "员工档案数据异常，请联系管理员");
            }

            // 生成 JWT Token（密钥缺失时 JwtTokenProvider 直接抛异常，绝不下发无效凭证）
            Long staffId = ((Number) staffIdValue).longValue();
            Long storeId = ((Number) storeIdValue).longValue();
            String role = (String) staff.get("role");
            String token = jwtTokenProvider.generate(staffId, storeId, role, username);

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

            // 板块白名单：非空表示该员工只能进入这些板块，由服务端下发，前端不得自行推导
            List<String> modules = moduleAccessService.allowedModules(staffId);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("storeId", storeId);
            data.put("storeName", storeName);
            data.put("modules", modules);
            data.put("moduleRestricted", !modules.isEmpty());

            log.info("【登录成功】用户: {}, 门店ID: {}, 门店: {}", username, storeId, storeName);
            return Result.success(data);
        } catch (IllegalStateException e) {
            log.error("【登录异常】服务端鉴权配置异常: {}", e.getMessage());
            return Result.error(500, "服务端鉴权配置异常，请联系管理员");
        } catch (Exception e) {
            // 不向客户端回显异常细节，避免泄露表结构 / SQL 信息
            log.error("【登录异常】用户: {}, 错误: {}", username, e.getMessage(), e);
            return Result.error(500, "登录失败，请稍后重试");
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

                // 离职 / 停用员工的历史 Token 立即失效，不再仅凭签名放行
                String sql = "SELECT * FROM staff_master WHERE staff_id = ? "
                        + "AND employment_status IN ('active', '在职') LIMIT 1";
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
                    List<String> modules = moduleAccessService.allowedModules(staffId);
                    data.put("user", user);
                    data.put("storeId", storeId);
                    data.put("storeName", getStoreName(storeId));
                    data.put("modules", modules);
                    data.put("moduleRestricted", !modules.isEmpty());
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
