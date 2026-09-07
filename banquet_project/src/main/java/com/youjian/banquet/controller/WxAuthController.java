package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.WxCustomer;
import com.youjian.banquet.repository.WxCustomerRepository;
import com.youjian.banquet.util.CustomerJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序顾客登录——免登录公开接口（顾客还没有账号，不可能带 JWT）。
 * 微信官方 code2Session 流程：小程序端 uni.login() 拿到 code，传给这里，
 * 这里用 code 换 openid（服务端持有 AppSecret，不能让前端直接碰）。
 * 换到 openid 后按 openid upsert 一个 wx_customer 记录，签发顾客自己的JWT
 * （CustomerJwtUtil，跟员工JWT用不同密钥，物理隔离，见该类注释）。
 */
@RestController
@RequestMapping("/api/public/auth")
public class WxAuthController {

    private static final Logger log = LoggerFactory.getLogger(WxAuthController.class);

    @Value("${wechat.miniapp.app-id:}")
    private String appId;

    @Value("${wechat.miniapp.app-secret:}")
    private String appSecret;

    @Value("${customer.jwt.secret:}")
    private String customerJwtSecret;

    @Autowired
    private WxCustomerRepository customerRepo;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/wx-login")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            return Result.error(400, "缺少code");
        }
        if (appId.isBlank() || appSecret.isBlank() || customerJwtSecret.isBlank()) {
            log.warn("[WxLogin] 微信小程序凭证还没配置");
            return Result.error(500, "服务端还没配置好，请稍后再试");
        }

        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=" + appId + "&secret=" + appSecret
                    + "&js_code=" + code + "&grant_type=authorization_code";
            // 微信 code2Session 接口返回 Content-Type: text/plain，即便body是JSON——
            // RestTemplate 用 Map.class 直接反序列化会因为没有匹配的HttpMessageConverter失败，
            // 必须先当纯文本拿到手，再自己用Jackson解析
            String rawResp = restTemplate.getForObject(url, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rawResp == null ? null : objectMapper.readValue(rawResp, Map.class);
            if (resp == null || resp.get("openid") == null) {
                String errMsg = resp != null ? String.valueOf(resp.get("errmsg")) : "无响应";
                log.warn("[WxLogin] code2Session失败: {}", errMsg);
                return Result.error(500, "微信登录失败: " + errMsg);
            }
            String openId = (String) resp.get("openid");
            String unionId = (String) resp.get("unionid");

            WxCustomer customer = customerRepo.findByOpenId(openId).orElseGet(() -> {
                WxCustomer c = new WxCustomer();
                c.setOpenId(openId);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            if (unionId != null) customer.setUnionId(unionId);
            customer.setLastLoginAt(LocalDateTime.now());
            customer = customerRepo.save(customer);

            String token = CustomerJwtUtil.mint(customerJwtSecret, customer.getId(), openId);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("nickname", customer.getNickname());
            userInfo.put("avatar", customer.getAvatarUrl());
            userInfo.put("phone", customer.getPhone());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("openId", openId);
            result.put("userInfo", userInfo);
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[WxLogin] 登录失败: {}", e.getMessage());
            return Result.error(500, "微信登录失败，请稍后重试");
        }
    }
}
