package com.youjian.banquet.controller;

import com.youjian.banquet.common.JwtTokenProvider;
import com.youjian.banquet.common.LoginCredential;
import com.youjian.banquet.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * iPad 点餐子系统认证接口
 *
 * 硬约束（原实现缺失该接口，前端在请求失败时回退到本地"演示会话"，
 * 造成任意手机号 + 任意密码即可进入 iPad 端；本控制器补齐真实校验）：
 *   1. 账号必须存在于 staff_master 且在职，命中多条（不唯一）一律拒绝
 *   2. 密码必须非空且与库中密码匹配，库中密码为空的账号禁止登录
 *   3. 校验通过后由服务端签发 JWT，前端不得自行伪造任何会话
 *
 * 全部数据来自本地数据库，不发起任何外部请求。
 */
@RestController
@RequestMapping("/api/ipad")
@CrossOrigin
public class IpadAuthController {

    private static final Logger log = LoggerFactory.getLogger(IpadAuthController.class);

    /** 统一的登录失败提示：不区分"账号不存在"与"密码错误"，避免账号枚举 */
    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误，请重新输入";

    private final JdbcTemplate jdbcTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IpadAuthController(JdbcTemplate jdbcTemplate, JwtTokenProvider jwtTokenProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String account = LoginCredential.normalizeUsername(body == null ? null : body.get("phone"));
        String password = body == null ? null : body.get("password");

        // 硬约束 1：账号与密码均不得为空（含纯空白），杜绝无密码进入
        if (account == null || !LoginCredential.isUsablePassword(password)) {
            log.warn("【iPad登录失败】账号或密码为空");
            return Result.error(400, "账号和密码不能为空");
        }

        // 硬约束 2：账号格式校验
        if (!LoginCredential.isValidUsername(account)) {
            log.warn("【iPad登录失败】账号格式非法: {}", account);
            return Result.error(400, "账号格式不正确：仅支持 "
                    + LoginCredential.USERNAME_MIN + "~" + LoginCredential.USERNAME_MAX + " 位手机号或员工账号");
        }

        try {
            String sql = "SELECT * FROM staff_master WHERE (staff_phone = ? OR staff_account = ?) "
                    + "AND employment_status IN ('active', '在职') LIMIT 2";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, account, account);

            if (list.isEmpty()) {
                log.warn("【iPad登录失败】账号不存在或已停用: {}", account);
                return Result.error(401, LOGIN_FAILED_MESSAGE);
            }
            // 硬约束 3：账号必须唯一
            if (list.size() > 1) {
                log.error("【iPad登录失败】账号存在重复记录: {}", account);
                return Result.error(409, "该账号存在重复记录，请联系管理员处理后再登录");
            }

            Map<String, Object> staff = list.get(0);
            String staffPassword = (String) staff.get("staff_password");

            // 硬约束 4：库中密码为空的账号一律拒绝
            if (staffPassword == null || staffPassword.trim().isEmpty()) {
                log.error("【iPad登录失败】账号未设置密码，拒绝登录: {}", account);
                return Result.error(401, "该账号尚未设置密码，请联系管理员重置后再登录");
            }

            if (!LoginCredential.matches(passwordEncoder, password, staffPassword)) {
                log.warn("【iPad登录失败】密码错误: {}", account);
                return Result.error(401, LOGIN_FAILED_MESSAGE);
            }

            Object staffIdValue = staff.get("staff_id");
            Object storeIdValue = staff.get("store_id");
            if (staffIdValue == null || storeIdValue == null) {
                log.error("【iPad登录失败】员工档案缺少 staff_id/store_id: {}", account);
                return Result.error(500, "员工档案数据异常，请联系管理员");
            }

            Long staffId = ((Number) staffIdValue).longValue();
            Long storeId = ((Number) storeIdValue).longValue();
            String role = (String) staff.get("role");

            Map<String, Object> data = new HashMap<>();
            // 服务端签发凭证：密钥缺失时直接抛异常，绝不下发无效 token
            data.put("token", jwtTokenProvider.generate(staffId, storeId, role, account));
            data.put("staff_id", staffId);
            data.put("staff_name", staff.get("staff_name"));
            data.put("staff_phone", staff.get("staff_phone"));
            data.put("role_type", role);
            data.put("store_id", storeId);
            data.put("store_name", getStoreName(storeId));

            log.info("【iPad登录成功】账号: {}, 门店ID: {}", account, storeId);
            return Result.success(data);
        } catch (IllegalStateException e) {
            log.error("【iPad登录异常】服务端鉴权配置异常: {}", e.getMessage());
            return Result.error(500, "服务端鉴权配置异常，请联系管理员");
        } catch (Exception e) {
            log.error("【iPad登录异常】账号: {}, 错误: {}", account, e.getMessage(), e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    /** 登录前的门店列表：仅返回门店基础信息，不含任何员工敏感字段 */
    @GetMapping("/store/list")
    public Result<List<Map<String, Object>>> storeList() {
        String sql = "SELECT store_id AS id, store_name, store_short_name, address "
                + "FROM store_info WHERE status = 'open' ORDER BY sort_order, store_id";
        return Result.success(jdbcTemplate.queryForList(sql));
    }

    private String getStoreName(Long storeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT store_name FROM store_info WHERE store_id = ? LIMIT 1", storeId);
        if (!rows.isEmpty() && rows.get(0).get("store_name") != null) {
            return rows.get(0).get("store_name").toString();
        }
        return "未知门店";
    }
}
