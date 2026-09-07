package com.youjian.banquet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.config.WxPayConfig;
import com.youjian.banquet.util.WxPayCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信支付 APIv3 调用封装——手写签名，不用官方SDK（见 pom.xml / WxPayCryptoUtil 注释）。
 * 平台证书（用来验证微信服务器返回内容的签名，防止有人伪造"支付成功"回调）懒加载 + 内存缓存，
 * 首次用到时去 /v3/certificates 拉一次，之后按 Wechatpay-Serial 直接查缓存。
 */
@Service
public class WxPayService {

    private static final Logger log = LoggerFactory.getLogger(WxPayService.class);
    private static final String API_HOST = "https://api.mch.weixin.qq.com";

    @Autowired
    private WxPayConfig config;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** serialNo -> PEM证书内容 */
    private final Map<String, String> platformCertCache = new ConcurrentHashMap<>();
    private volatile Instant certFetchedAt;

    private HttpEntity<String> buildSignedRequest(String method, String urlPath, String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = WxPayCryptoUtil.randomNonce();
        String signature = WxPayCryptoUtil.sign(config.getPrivateKey(), method, urlPath, timestamp, nonce, body);
        String authHeader = WxPayCryptoUtil.buildAuthHeader(config.getMchId(), config.getCertSerialNo(), nonce, timestamp, signature);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        headers.set("User-Agent", "youjianchuiyan-banquet/1.0");
        return new HttpEntity<>(body, headers);
    }

    /** 拉取并缓存微信支付平台证书（用自己的APIv3密钥解密返回内容），5分钟内不重复拉取 */
    private synchronized void refreshPlatformCertsIfNeeded() {
        if (certFetchedAt != null && certFetchedAt.plusSeconds(300).isAfter(Instant.now())) return;
        try {
            String urlPath = "/v3/certificates";
            HttpEntity<String> entity = buildSignedRequest("GET", urlPath, "");
            ResponseEntity<String> resp = restTemplate.exchange(API_HOST + urlPath, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            for (JsonNode item : root.get("data")) {
                String serialNo = item.get("serial_no").asText();
                JsonNode enc = item.get("encrypt_certificate");
                String certPem = WxPayCryptoUtil.aesGcmDecrypt(
                        config.getApiV3Key(),
                        enc.get("nonce").asText(),
                        enc.get("associated_data").asText(),
                        enc.get("ciphertext").asText());
                platformCertCache.put(serialNo, certPem);
            }
            certFetchedAt = Instant.now();
            log.info("[WxPay] 平台证书刷新成功，共{}张", platformCertCache.size());
        } catch (Exception e) {
            log.error("[WxPay] 拉取平台证书失败", e);
        }
    }

    /** 验证微信服务器发来的签名（回调通知 / API响应都用这个），防止伪造 */
    public boolean verifySignature(String serialNo, String timestamp, String nonce, String body, String signature) {
        refreshPlatformCertsIfNeeded();
        String certPem = platformCertCache.get(serialNo);
        if (certPem == null) {
            // 缓存里没有可能是证书刚轮换，强制刷新一次再试
            certFetchedAt = null;
            refreshPlatformCertsIfNeeded();
            certPem = platformCertCache.get(serialNo);
        }
        if (certPem == null) {
            log.warn("[WxPay] 找不到序列号为{}的平台证书，验签失败", serialNo);
            return false;
        }
        try {
            PublicKey publicKey = WxPayCryptoUtil.parseCertPublicKey(certPem);
            return WxPayCryptoUtil.verify(publicKey, timestamp, nonce, body, signature);
        } catch (Exception e) {
            log.error("[WxPay] 验签异常", e);
            return false;
        }
    }

    /** 解密回调通知里的 resource 字段 */
    public String decryptNotifyResource(JsonNode resource) throws Exception {
        return WxPayCryptoUtil.aesGcmDecrypt(
                config.getApiV3Key(),
                resource.get("nonce").asText(),
                resource.get("associated_data") != null ? resource.get("associated_data").asText() : "",
                resource.get("ciphertext").asText());
    }

    /** JSAPI 下单，返回小程序 wx.requestPayment 需要的参数 */
    public Map<String, Object> prepayJsapi(String outTradeNo, String description, int amountFen, String openId) throws Exception {
        String urlPath = "/v3/pay/transactions/jsapi";
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("appid", config.getAppId());
        reqBody.put("mchid", config.getMchId());
        reqBody.put("description", description);
        reqBody.put("out_trade_no", outTradeNo);
        reqBody.put("notify_url", config.getNotifyUrl());
        reqBody.put("amount", Map.of("total", amountFen, "currency", "CNY"));
        reqBody.put("payer", Map.of("openid", openId));
        String bodyJson = objectMapper.writeValueAsString(reqBody);

        HttpEntity<String> entity = buildSignedRequest("POST", urlPath, bodyJson);
        ResponseEntity<String> resp;
        try {
            resp = restTemplate.postForEntity(API_HOST + urlPath, entity, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("[WxPay] 下单失败: {}", e.getResponseBodyAsString());
            throw new RuntimeException("微信下单失败: " + e.getResponseBodyAsString());
        }
        JsonNode root = objectMapper.readTree(resp.getBody());
        String prepayId = root.get("prepay_id").asText();

        // 小程序调起支付需要的第二次签名：对象是 "package"（prepay_id拼出来的）
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = WxPayCryptoUtil.randomNonce();
        String packageStr = "prepay_id=" + prepayId;
        String payMessage = config.getAppId() + "\n" + timestamp + "\n" + nonce + "\n" + packageStr + "\n";
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(config.getPrivateKey());
        sign.update(payMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String paySign = java.util.Base64.getEncoder().encodeToString(sign.sign());

        Map<String, Object> result = new HashMap<>();
        result.put("timeStamp", timestamp);
        result.put("nonceStr", nonce);
        result.put("package", packageStr);
        result.put("signType", "RSA");
        result.put("paySign", paySign);
        return result;
    }
}
