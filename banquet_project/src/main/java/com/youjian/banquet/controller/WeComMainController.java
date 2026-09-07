package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.service.WeComMediaDownloader;
import com.youjian.banquet.service.WeComStreamReplier;
import com.youjian.banquet.util.WeComCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 企业微信"智能机器人"URL回调接入点（Main，超管专属）。协议跟客服Ella/厨师助手一样，
 * 唯一的关键区别：这里多了一道身份白名单——企业微信消息里没有JWT，认不出GM身份，
 * 只能靠 from.userid 做白名单判断，不在白名单里的人发消息直接不处理、不回复
 * （不回复而不是回复"无权限"，避免暴露这个机器人的存在和用途给非授权的人）。
 * 白名单从 wecom.aibot-main.allowed-userid 配置读取，不写死在代码里。
 */
@RestController
@RequestMapping("/api/public/wecom/aibot/main")
public class WeComMainController {

    private static final Logger log = LoggerFactory.getLogger(WeComMainController.class);

    private static final int HISTORY_MAX_TURNS = 12;
    private static final int HISTORY_MAX_CHATS = 500;

    @Value("${wecom.aibot-main.token:}")
    private String token;

    @Value("${wecom.aibot-main.encoding-aes-key:}")
    private String encodingAesKey;

    @Value("${wecom.aibot-main.allowed-userid:}")
    private String allowedUserId;

    @Autowired
    private WeComMediaDownloader mediaDownloader;

    @Autowired
    private WeComStreamReplier streamReplier;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> histories = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> perChatExecutors = new ConcurrentHashMap<>();

    @GetMapping("/callback")
    public ResponseEntity<String> verify(@RequestParam("msg_signature") String msgSignature,
                                          @RequestParam("timestamp") String timestamp,
                                          @RequestParam("nonce") String nonce,
                                          @RequestParam("echostr") String echostr) {
        if (token.isBlank() || encodingAesKey.isBlank()) {
            log.warn("[WeComMain] 收到验证请求，但 wecom.aibot-main.token / encoding-aes-key 还没配置");
            return ResponseEntity.status(500).body("not configured");
        }
        String expected = WeComCryptoUtil.sign(token, timestamp, nonce, echostr);
        if (!expected.equals(msgSignature)) {
            log.warn("[WeComMain] 验证签名不匹配");
            return ResponseEntity.status(403).body("signature mismatch");
        }
        try {
            String plain = WeComCryptoUtil.decrypt(encodingAesKey, echostr);
            log.info("[WeComMain] URL 验证通过");
            return ResponseEntity.ok(plain);
        } catch (Exception e) {
            log.warn("[WeComMain] 验证echostr解密失败: {}", e.getMessage());
            return ResponseEntity.status(400).body("decrypt failed");
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<String> receive(@RequestParam("msg_signature") String msgSignature,
                                           @RequestParam("timestamp") String timestamp,
                                           @RequestParam("nonce") String nonce,
                                           @RequestBody String rawBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> outer = objectMapper.readValue(rawBody, Map.class);
            String encrypt = (String) outer.get("encrypt");
            String expected = WeComCryptoUtil.sign(token, timestamp, nonce, encrypt);
            if (!expected.equals(msgSignature)) {
                log.warn("[WeComMain] POST 签名不匹配");
                return ResponseEntity.ok("");
            }
            String plainJson = WeComCryptoUtil.decrypt(encodingAesKey, encrypt);
            log.info("[WeComMain] 解密后明文: {}", plainJson);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(plainJson, Map.class);
            String chatId = extractChatId(payload);
            getOrCreateExecutor(chatId).submit(() -> handleAsync(payload));
        } catch (Exception e) {
            log.warn("[WeComMain] 处理回调失败: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("");
    }

    @SuppressWarnings("unchecked")
    private void handleAsync(Map<String, Object> payload) {
        try {
            // 身份门槛交给企业微信自己的"可使用成员"设置（用户已在控制台限定只有自己能看到/用这个
            // 机器人），代码这层不再重复做白名单拦截——但只读这条线不变，Main 依然没有任何写能力。
            Map<String, Object> from = (Map<String, Object>) payload.get("from");
            String senderUserId = from == null ? null : str(from.get("userid"));

            String msgtype = str(payload.get("msgtype"));
            String content = null;
            String imageUrl = null;
            if ("text".equals(msgtype)) {
                Map<String, Object> textObj = (Map<String, Object>) payload.get("text");
                content = textObj == null ? null : str(textObj.get("content"));
            } else if ("voice".equals(msgtype)) {
                Map<String, Object> voiceObj = (Map<String, Object>) payload.get("voice");
                content = voiceObj == null ? null : str(voiceObj.get("content"));
            } else if ("image".equals(msgtype)) {
                // 企业微信给的是一个带签名的COS临时URL，有效期很短（实测大概5分钟），
                // 必须马上转发去做视觉识别，不能等，也不能缓存到历史消息里重复使用
                Map<String, Object> imageObj = (Map<String, Object>) payload.get("image");
                imageUrl = imageObj == null ? null : str(imageObj.get("url"));
            } else {
                log.info("[WeComMain] 暂不处理消息类型: {}", msgtype);
                return;
            }
            String chatId = str(payload.get("chatid"));
            if (chatId == null) {
                chatId = senderUserId;
            }
            String responseUrl = str(payload.get("response_url"));
            boolean hasContent = (content != null && !content.isBlank()) || imageUrl != null;
            if (!hasContent || responseUrl == null || chatId == null) {
                log.warn("[WeComMain] 消息字段不完整，跳过。content={}, imageUrl={}, chatId={}, responseUrl={}",
                        content, imageUrl, chatId, responseUrl);
                return;
            }
            log.info("[WeComMain] 处理消息 chatId={}, content={}, imageUrl={}", chatId, content, imageUrl != null);

            List<Map<String, Object>> history = getHistory(chatId);

            Map<String, Object> reqBody = new HashMap<>();
            if (imageUrl != null) {
                // 企业微信这条图片URL下载下来是密文，得先用本机器人自己的EncodingAESKey解密，
                // 转成data URI后再往下发——直接把原始URL传给视觉模型，DeepSeek服务器读到的只是
                // 加密乱码，会报"不支持的图片格式"（2026-09-02 生产环境实测过这个坑）。
                try {
                    String dataUri = mediaDownloader.downloadAndDecryptAsDataUri(encodingAesKey, imageUrl);
                    reqBody.put("image_url", dataUri);
                } catch (Exception e) {
                    log.warn("[WeComMain] 图片下载/解密失败: {}", e.getMessage());
                    reqBody.put("message", "（有一张图片下载失败，请提示用户重新发送）");
                }
            } else {
                reqBody.put("message", content);
            }
            reqBody.put("history", history);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Forwarded-For", "wecom-main-" + chatId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // 先秒回一条占位消息：Main/Leo 现在自己写SQL干活，复杂问题要跑几十秒到几分钟，
            // 干等着用户会以为机器人死了。流式消息用同一个 streamId，真结果出来再覆盖掉这句。
            String streamId = streamReplier.begin("WeComMain", responseUrl, "收到，正在查，请稍等…");

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "http://127.0.0.1:8080/api/public/agent/main/chat", entity, Map.class);

            String reply = null;
            if (resp.getBody() != null) {
                Object data = resp.getBody().get("data");
                if (data instanceof Map) {
                    reply = str(((Map<String, Object>) data).get("reply"));
                }
            }
            if (reply == null || reply.isBlank()) {
                reply = "暂时联系不上，请稍后再试~";
            }

            pushHistory(chatId, "user", imageUrl != null ? "[发送了一张图片]" : content);
            pushHistory(chatId, "assistant", reply);

            log.info("[WeComMain] 回复内容: {}", reply);
            streamReplier.finish("WeComMain", responseUrl, streamId, reply);
        } catch (Exception e) {
            log.warn("[WeComMain] 异步处理消息失败: {}", e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> getHistory(String chatId) {
        return histories.getOrDefault(chatId, List.of());
    }

    private void pushHistory(String chatId, String role, String content) {
        if (histories.size() > HISTORY_MAX_CHATS) {
            histories.clear();
        }
        List<Map<String, Object>> h = new ArrayList<>(histories.getOrDefault(chatId, List.of()));
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        h.add(m);
        while (h.size() > HISTORY_MAX_TURNS) {
            h.remove(0);
        }
        histories.put(chatId, h);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractChatId(Map<String, Object> payload) {
        String chatId = str(payload.get("chatid"));
        if (chatId != null && !chatId.isBlank()) {
            return chatId;
        }
        Map<String, Object> from = (Map<String, Object>) payload.get("from");
        String userId = from == null ? null : str(from.get("userid"));
        return userId != null ? userId : "unknown";
    }

    private ExecutorService getOrCreateExecutor(String chatId) {
        return perChatExecutors.computeIfAbsent(chatId, k -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WeComMain-" + chatId);
            t.setDaemon(true);
            return t;
        }));
    }
}
