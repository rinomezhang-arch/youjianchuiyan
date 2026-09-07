package com.youjian.banquet.config;

import com.youjian.banquet.util.WxPayCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;

/**
 * 微信支付配置——只负责在启动时把商户API私钥从文件读进内存，其余签名/验签/加解密逻辑在
 * WxPayCryptoUtil / WxPayService 里，见 pom.xml 注释：这里不用官方SDK，手写实现。
 */
@Component
public class WxPayConfig {

    private static final Logger log = LoggerFactory.getLogger(WxPayConfig.class);

    @Value("${wxpay.mch-id:}")
    private String mchId;

    @Value("${wxpay.private-key-path:}")
    private String privateKeyPath;

    @Value("${wxpay.cert-serial-no:}")
    private String certSerialNo;

    @Value("${wxpay.api-v3-key:}")
    private String apiV3Key;

    @Value("${wxpay.notify-url:}")
    private String notifyUrl;

    @Value("${wxpay.deposit-amount-fen:20000}")
    private int depositAmountFen;

    @Value("${wechat.miniapp.app-id:}")
    private String appId;

    private PrivateKey privateKey;

    public boolean isConfigured() {
        return !mchId.isBlank() && !privateKeyPath.isBlank() && !certSerialNo.isBlank() && !apiV3Key.isBlank();
    }

    public synchronized PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
        if (privateKeyPath.isBlank()) return null;
        try {
            String pem = Files.readString(Path.of(privateKeyPath));
            privateKey = WxPayCryptoUtil.loadPrivateKey(pem);
            return privateKey;
        } catch (Exception e) {
            log.error("[WxPay] 读取商户私钥失败: {}", privateKeyPath, e);
            return null;
        }
    }

    public String getMchId() { return mchId; }
    public String getCertSerialNo() { return certSerialNo; }
    public String getApiV3Key() { return apiV3Key; }
    public String getNotifyUrl() { return notifyUrl; }
    public int getDepositAmountFen() { return depositAmountFen; }
    public String getAppId() { return appId; }
}
