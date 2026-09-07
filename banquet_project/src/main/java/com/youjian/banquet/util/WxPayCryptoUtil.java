package com.youjian.banquet.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * 微信支付 APIv3 签名/验签/加解密——手写实现，不用官方 wechatpay-java SDK
 * （那个包跟项目里其他依赖冲突，会让Lombok编译期失效，见 pom.xml 改动记录）。
 * 算法本身是公开文档化的标准流程，跟之前给企业微信写的 WeComCryptoUtil 是同一类活：
 *   1) 请求签名：RSA-SHA256 对 "METHOD\nURL\nTIMESTAMP\nNONCE\nBODY\n" 签名，
 *      拼进 Authorization 头（WECHATPAY2-SHA256-RSA2048）
 *   2) 回调/响应验签：用微信支付平台证书的公钥，验证 "TIMESTAMP\nNONCE\nBODY\n" 的签名
 *   3) AEAD_AES_256_GCM 解密：用APIv3密钥解开回调里的 resource.ciphertext，
 *      以及 /v3/certificates 接口返回的 encrypt_certificate（平台证书本身也是加密传输的）
 */
public class WxPayCryptoUtil {

    /** 加载商户API私钥（PEM格式，PKCS8） */
    public static PrivateKey loadPrivateKey(String pemContent) throws Exception {
        String privateKeyPem = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /** 请求签名：返回Base64签名值 */
    public static String sign(PrivateKey privateKey, String method, String urlPathWithQuery, String timestamp, String nonce, String body) throws Exception {
        String message = method + "\n" + urlPathWithQuery + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(privateKey);
        sign.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sign.sign());
    }

    /** 拼装 Authorization 头 */
    public static String buildAuthHeader(String mchId, String certSerialNo, String nonce, String timestamp, String signature) {
        return String.format(
                "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial_no=\"%s\",signature=\"%s\"",
                mchId, nonce, timestamp, certSerialNo, signature);
    }

    public static String randomNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 验证微信服务器返回的响应/回调签名，用平台证书公钥 */
    public static boolean verify(PublicKey platformPublicKey, String timestamp, String nonce, String body, String signatureBase64) throws Exception {
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(platformPublicKey);
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signatureBase64));
    }

    /** AEAD_AES_256_GCM 解密（回调 resource / 平台证书 encrypt_certificate 通用） */
    public static String aesGcmDecrypt(String apiV3Key, String nonce, String associatedData, String ciphertextBase64) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64));
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** 从PEM证书内容解析出公钥，用于验签 */
    public static PublicKey parseCertPublicKey(String certPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
        return cert.getPublicKey();
    }

    /** 从PEM证书内容解析出证书序列号（十六进制大写），微信回调头 Wechatpay-Serial 就是这个格式 */
    public static String parseCertSerialNo(String certPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
        return cert.getSerialNumber().toString(16).toUpperCase();
    }

    private WxPayCryptoUtil() {}
}
