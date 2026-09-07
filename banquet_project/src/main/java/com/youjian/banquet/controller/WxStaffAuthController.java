package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.StaffWxBinding;
import com.youjian.banquet.repository.StaffWxBindingRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工小程序端"微信身份自动识别"登录——员工打开小程序，用 wx.login() 拿到的 code 换 openid，
 * 查 staff_wx_binding 表：已绑定就直接签发跟桌面端登录格式完全一样的员工JWT（复用 AuthController
 * 同款 claims：staffId/storeId/role），后续调用现有的 /api/** 接口、走 JwtAuthInterceptor/
 * StoreDataScopeAspect/AuditLogAspect 跟桌面端登录的员工没有任何区别；没绑定就返回 needBind=true，
 * 前端引导员工输入手机号，跟 staff_master.staff_phone 核对上了才允许绑定（不能凭自称绑定任意员工身份）。
 */
@RestController
@RequestMapping("/api/public/auth")
public class WxStaffAuthController {

    private static final Logger log = LoggerFactory.getLogger(WxStaffAuthController.class);

    @Value("${wechat.miniapp.app-id:}")
    private String appId;

    @Value("${wechat.miniapp.app-secret:}")
    private String appSecret;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Autowired
    private StaffWxBindingRepository bindingRepo;

    @Autowired
    private JdbcTemplate jdbc;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String code2OpenId(String code) throws Exception {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appId + "&secret=" + appSecret
                + "&js_code=" + code + "&grant_type=authorization_code";
        // 同 WxAuthController 的坑：微信这个接口返回 Content-Type: text/plain，必须先当纯文本收，再自己解析JSON
        String rawResp = restTemplate.getForObject(url, String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rawResp == null ? null : objectMapper.readValue(rawResp, Map.class);
        if (resp == null || resp.get("openid") == null) {
            String errMsg = resp != null ? String.valueOf(resp.get("errmsg")) : "无响应";
            throw new RuntimeException("code2Session失败: " + errMsg);
        }
        return (String) resp.get("openid");
    }

    private String mintStaffToken(Map<String, Object> staff) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Long staffId = ((Number) staff.get("staff_id")).longValue();
        Long storeId = staff.get("store_id") == null ? null : ((Number) staff.get("store_id")).longValue();
        String role = (String) staff.get("role");
        String username = (String) staff.get("staff_account");
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

    private Map<String, Object> buildLoginResult(Map<String, Object> staff) {
        String token = mintStaffToken(staff);
        Map<String, Object> user = new HashMap<>();
        user.put("staffId", staff.get("staff_id"));
        user.put("staffName", staff.get("staff_name"));
        user.put("staffAccount", staff.get("staff_account"));
        user.put("role", staff.get("role"));
        user.put("position", staff.get("staff_position"));
        user.put("phone", staff.get("staff_phone"));

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        data.put("storeId", staff.get("store_id"));
        return data;
    }

    @PostMapping("/staff-wx-login")
    public Result<Map<String, Object>> staffWxLogin(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            return Result.error(400, "缺少code");
        }
        if (appId.isBlank() || appSecret.isBlank() || jwtSecret.isBlank()) {
            log.warn("[StaffWxLogin] 微信小程序凭证还没配置");
            return Result.error(500, "服务端还没配置好，请稍后再试");
        }
        try {
            String openId = code2OpenId(code);
            return bindingRepo.findByOpenId(openId)
                    .map(binding -> {
                        List<Map<String, Object>> rows = jdbc.queryForList(
                                "SELECT staff_id, store_id, staff_name, staff_account, role, staff_position, staff_phone " +
                                "FROM staff_master WHERE staff_id = ? AND employment_status != '离职'",
                                binding.getStaffId());
                        if (rows.isEmpty()) {
                            return Result.<Map<String, Object>>error(403, "绑定的员工账号已离职或不存在，请联系管理员");
                        }
                        return Result.success(buildLoginResult(rows.get(0)));
                    })
                    .orElseGet(() -> {
                        Map<String, Object> needBind = new HashMap<>();
                        needBind.put("needBind", true);
                        needBind.put("openId", openId);
                        return Result.success(needBind);
                    });
        } catch (Exception e) {
            log.warn("[StaffWxLogin] 登录失败: {}", e.getMessage());
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    @PostMapping("/staff-wx-bind")
    public Result<Map<String, Object>> staffWxBind(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String phone = (String) body.get("phone");
        if (code == null || code.isBlank() || phone == null || phone.isBlank()) {
            return Result.error(400, "缺少code或手机号");
        }
        try {
            String openId = code2OpenId(code);

            if (bindingRepo.findByOpenId(openId).isPresent()) {
                return Result.error(409, "这个微信已经绑定过员工账号了，直接登录即可");
            }

            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT staff_id, store_id, staff_name, staff_account, role, staff_position, staff_phone " +
                    "FROM staff_master WHERE staff_phone = ? AND employment_status != '离职'",
                    phone.trim());
            if (rows.isEmpty()) {
                return Result.error(404, "没有找到这个手机号对应的在职员工，请核对手机号或联系管理员");
            }
            if (rows.size() > 1) {
                log.warn("[StaffWxBind] 手机号 {} 对应多个在职员工记录，拒绝自动绑定", phone);
                return Result.error(409, "该手机号对应多个员工记录，请联系管理员手动处理");
            }

            Map<String, Object> staff = rows.get(0);
            Integer staffId = ((Number) staff.get("staff_id")).intValue();

            StaffWxBinding binding = new StaffWxBinding();
            binding.setStaffId(staffId);
            binding.setOpenId(openId);
            binding.setBoundAt(LocalDateTime.now());
            bindingRepo.save(binding);

            log.info("[StaffWxBind] 员工 {}（staffId={}）绑定微信成功", staff.get("staff_name"), staffId);
            return Result.success(buildLoginResult(staff));
        } catch (Exception e) {
            log.warn("[StaffWxBind] 绑定失败: {}", e.getMessage());
            return Result.error(500, "绑定失败，请稍后重试");
        }
    }
}
