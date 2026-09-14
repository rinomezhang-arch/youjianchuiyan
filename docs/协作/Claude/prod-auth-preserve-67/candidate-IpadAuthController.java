package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * iPad 子系统：登录与设备认证。
 * 注意：/api/ipad/** 已在 WebMvcConfig 里排除出全局 JWT 拦截器，走 IpadInterceptor 的
 * 设备头部（X-Store-Id/X-Staff-Id/X-Device-Sn/X-Client-Type）鉴权 —— 这意味着调用本类
 * 任何接口时设备必须已经在 ipad_device_binding 里有一条 active 记录（由店长/管理员在后台
 * 预先绑定），这里不做"全新设备自助注册"，与常见 POS 一体机的设备管理方式一致。
 */
@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadAuthController {

    @Autowired
    private JdbcTemplate jdbc;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                              HttpServletRequest request) {
        String phone = body.get("phone");
        String password = body.get("password");
        if (phone == null || password == null) {
            return Result.error(400, "手机号和密码不能为空");
        }
        Long storeId = (Long) request.getAttribute("ipad_store_id");
        String deviceSn = (String) request.getAttribute("ipad_device_sn");

        try {
            String sql = "SELECT * FROM staff_master WHERE staff_phone = ? AND store_id = ? " +
                    "AND employment_status IN ('active','在职') LIMIT 1";
            List<Map<String, Object>> list = jdbc.queryForList(sql, phone, storeId);
            if (list.isEmpty()) {
                return Result.error(401, "账号不存在或不属于本店");
            }
            Map<String, Object> staff = list.get(0);
            String staffPassword = (String) staff.get("staff_password");
            boolean passwordMatch = false;
            if (staffPassword != null) {
                if (staffPassword.startsWith("$2a$") || staffPassword.startsWith("$2b$")) {
                    passwordMatch = passwordEncoder.matches(password, staffPassword);
                } else {
                    passwordMatch = staffPassword.equals(password);
                }
            }
            if (!passwordMatch) {
                return Result.error(401, "密码错误");
            }

            Long staffId = ((Number) staff.get("staff_id")).longValue();
            // 设备与该员工绑定（若绑定记录未指定具体员工，则记录本次登录人）
            jdbc.update("UPDATE ipad_device_binding SET staff_id = ?, last_seen_at = ? WHERE device_sn = ?",
                    staffId, LocalDateTime.now(), deviceSn);

            String storeName = getStoreName(storeId);
            Map<String, Object> printConfig = getPrintConfig(deviceSn);

            Map<String, Object> data = new HashMap<>();
            data.put("staff_id", staffId);
            data.put("staff_name", staff.get("staff_name"));
            data.put("staff_phone", staff.get("staff_phone"));
            data.put("role_type", staff.get("role"));
            data.put("store_id", storeId);
            data.put("store_name", storeName);
            data.put("device_sn", deviceSn);
            data.put("print_port", printConfig.getOrDefault("print_port", 9100));
            data.put("print_template_code", printConfig.getOrDefault("print_template_code", "default"));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "登录失败：" + e.getMessage());
        }
    }

    @GetMapping("/store/list")
    public Result<List<Map<String, Object>>> storeList() {
        try {
            return Result.success(jdbc.queryForList(
                    "SELECT store_id AS id, store_name, store_short_name FROM store_info WHERE status='open' ORDER BY sort_order"));
        } catch (Exception e) {
            return Result.error(500, "查询门店列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/device/bind")
    public Result<Map<String, Object>> deviceBind(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        Long storeId = (Long) request.getAttribute("ipad_store_id");
        String deviceSn = (String) request.getAttribute("ipad_device_sn");
        String deviceName = body.get("device_name") != null ? body.get("device_name").toString() : null;
        try {
            jdbc.update("UPDATE ipad_device_binding SET device_name = COALESCE(?, device_name), " +
                    "last_seen_at = ? WHERE device_sn = ? AND store_id = ?",
                    deviceName, LocalDateTime.now(), deviceSn, storeId);
            Map<String, Object> data = new HashMap<>();
            data.put("device_sn", deviceSn);
            data.put("store_id", storeId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "设备绑定更新失败：" + e.getMessage());
        }
    }

    @GetMapping("/config/print")
    public Result<Map<String, Object>> printConfig(HttpServletRequest request) {
        String deviceSn = (String) request.getAttribute("ipad_device_sn");
        try {
            return Result.success(getPrintConfig(deviceSn));
        } catch (Exception e) {
            return Result.error(500, "查询打印配置失败：" + e.getMessage());
        }
    }

    private String getStoreName(Long storeId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT store_name FROM store_info WHERE store_id = ? LIMIT 1", storeId);
        return list.isEmpty() ? "未知门店" : String.valueOf(list.get(0).get("store_name"));
    }

    /** ipad_device_binding 目前没有打印相关列，暂返回默认值，后续如需按设备定制打印机再扩展表结构。 */
    private Map<String, Object> getPrintConfig(String deviceSn) {
        Map<String, Object> config = new HashMap<>();
        config.put("print_port", 9100);
        config.put("print_template_code", "default");
        return config;
    }
}
