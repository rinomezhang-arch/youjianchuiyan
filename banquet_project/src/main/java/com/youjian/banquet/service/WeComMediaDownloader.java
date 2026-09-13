package com.youjian.banquet.service;

import com.youjian.banquet.util.WeComCryptoUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;

/**
 * 企业微信"智能机器人"URL回调模式下载+解密图片/媒体消息，转成可以直接喂给视觉模型的 data URI。
 * <p>
 * 两个关键点，都是官方文档明确写的、不是猜的（developer.work.weixin.qq.com/document/path/101463）：
 * <ol>
 *   <li>URL回调模式下媒体文件下载下来的内容是密文，用的是跟消息正文解密同一把 EncodingAESKey
 *       （不是长连接模式那种每个URL独立的aeskey），AES-256-CBC，解密逻辑见
 *       {@link WeComCryptoUtil#decryptMediaBytes}。</li>
 *   <li>下载这一步本身，腾讯云COS对这条链接会校验请求方是不是"正常浏览器"，Java默认HTTP客户端的
 *       请求头会被判定成非正常来源直接403 AccessDenied（2026-09-02 生产环境实测复现），
 *       需要伪装成真实浏览器UA才能通过。</li>
 * </ol>
 * 每个"智能机器人"实例的 EncodingAESKey 不一样，所以调用方（各个 WeComXxxController）必须传入
 * 自己的 EncodingAESKey，这个类本身不持有任何具体机器人的身份信息。
 */
@Component
public class WeComMediaDownloader {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate = new RestTemplate();

    /** 下载企业微信图片URL + 用调用方指定的EncodingAESKey解密，返回可直接传给视觉模型的 data URI */
    public String downloadAndDecryptAsDataUri(String encodingAesKey, String mediaUrl) {
        byte[] cipherBytes = download(mediaUrl);
        byte[] plainBytes = WeComCryptoUtil.decryptMediaBytes(encodingAesKey, cipherBytes);
        String mimeType = guessImageMimeType(plainBytes);
        String base64 = Base64.getEncoder().encodeToString(plainBytes);
        return "data:" + mimeType + ";base64," + base64;
    }

    private byte[] download(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.set("Referer", "https://work.weixin.qq.com/");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        // 2026-09-02 生产环境复测确认真正病因：企业微信这条COS签名URL的 sign= 参数本身就是
        // URL编码过的（%3D代表=、%26代表&），RestTemplate.exchange(String,...) 默认把传入字符串
        // 当URI模板再编码一次，会把 %3D 变成 %253D，直接把签名污染坏，腾讯云验签失败=403 AccessDenied。
        // 用 UriComponentsBuilder...build(true) 显式声明"这串已经编码过了，不要再编"，绕开这个坑。
        URI uri = UriComponentsBuilder.fromUriString(url).build(true).toUri();
        ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);
        byte[] bytes = resp.getBody();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("下载到的媒体内容为空");
        }
        return bytes;
    }

    /** 按文件头魔数判断图片格式，解密后的内容没有Content-Type响应头可用，只能看字节本身 */
    private String guessImageMimeType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
