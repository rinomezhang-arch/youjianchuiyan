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
 * 企业微信"智能机器人"URL回调接入点——张婧专属，人设叫 Leo。协议、加解密方案、转发目标
 * （MainPublicController，读写全模块+提案确认）跟 WeComMainController 完全一样，唯一区别是
 * 这是企业微信里独立的一个机器人实例（自己的 Token/EncodingAESKey），身份门槛靠企业微信
 * 管理后台该机器人的"可使用成员"锁定成只有张婧能看到/能用，不在代码里重复做白名单判断
 * （跟 WeComMainController 的现有做法一致）。请求体里带 persona=leo，MainPublicController
 * 据此切换成专门给她准备的系统提示词（假设她完全不懂系统，写操作确认前必须用大白话讲清楚
 * 具体改了什么、会有什么后果）并用独立的提案会话key（避免和 Main 那边的待确认提案互相顶掉）。
 * MainPublicController 内部签发的服务身份本来就固定代表张婧本人（SERVICE_STAFF_ID=201,
 * super_admin），所以这条链路不需要额外的身份映射。
 */
@RestController
@RequestMapping("/api/public/wecom/aibot/zhangjing")
public class WeComZhangJingController {

    private static final Logger log = LoggerFactory.getLogger(WeComZhangJingController.class);

    private static final int HISTORY_MAX_TURNS = 12;
    private static final int HISTORY_MAX_CHATS = 500;

    @Value("${wecom.aibot-zhangjing.token:}")
    private String token;

    @Value("${wecom.aibot-zhangjing.encoding-aes-key:}")
    private String encodingAesKey;

    @Autowired
    private WeComMediaDownloader mediaDownloader;

    @Autowired
    private WeComStreamReplier streamReplier;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> histories = new ConcurrentHashMap<>();
    // 同一个 chatId 的消息必须串行处理——否则先快后慢的消息就会回复错位。
    // 每个活跃的 chatId 拥有自己的单线程 executor，消息在该 executor 的队列里排队。
    private final Map<String, ExecutorService> perChatExecutors = new ConcurrentHashMap<>();

    @GetMapping("/callback")
    public ResponseEntity<String> verify(@RequestParam("msg_signature") String msgSignature,
                                          @RequestParam("timestamp") String timestamp,
                                          @RequestParam("nonce") String nonce,
                                          @RequestParam("echostr") String echostr) {
        if (token.isBlank() || encodingAesKey.isBlank()) {
            log.warn("[WeComZhangJing] 收到验证请求，但 wecom.aibot-zhangjing.token / encoding-aes-key 还没配置");
            return ResponseEntity.status(500).body("not configured");
        }
        String expected = WeComCryptoUtil.sign(token, timestamp, nonce, echostr);
        if (!expected.equals(msgSignature)) {
            log.warn("[WeComZhangJing] 验证签名不匹配");
            return ResponseEntity.status(403).body("signature mismatch");
        }
        try {
            String plain = WeComCryptoUtil.decrypt(encodingAesKey, echostr);
            log.info("[WeComZhangJing] URL 验证通过");
            return ResponseEntity.ok(plain);
        } catch (Exception e) {
            log.warn("[WeComZhangJing] 验证echostr解密失败: {}", e.getMessage());
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
                log.warn("[WeComZhangJing] POST 签名不匹配");
                return ResponseEntity.ok("");
            }
            String plainJson = WeComCryptoUtil.decrypt(encodingAesKey, encrypt);
            log.info("[WeComZhangJing] 解密后明文: {}", plainJson);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(plainJson, Map.class);
            // 同一个 chatId 的消息排队处理，避免回复错位（先快后慢的消息抢过去导致顺序乱了）。
            String chatId = extractChatId(payload);
            getOrCreateExecutor(chatId).submit(() -> handleAsync(payload));
        } catch (Exception e) {
            log.warn("[WeComZhangJing] 处理回调失败: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("");
    }

    @SuppressWarnings("unchecked")
    private void handleAsync(Map<String, Object> payload) {
        try {
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
                log.info("[WeComZhangJing] 暂不处理消息类型: {}", msgtype);
                return;
            }
            String chatId = str(payload.get("chatid"));
            if (chatId == null) {
                chatId = senderUserId;
            }
            String responseUrl = str(payload.get("response_url"));
            boolean hasContent = (content != null && !content.isBlank()) || imageUrl != null;
            if (!hasContent || responseUrl == null || chatId == null) {
                log.warn("[WeComZhangJing] 消息字段不完整，跳过。content={}, imageUrl={}, chatId={}, responseUrl={}",
                        content, imageUrl, chatId, responseUrl);
                return;
            }
            log.info("[WeComZhangJing] 处理消息 chatId={}, content={}, imageUrl={}", chatId, content, imageUrl != null);

            List<Map<String, Object>> history = getHistory(chatId);

            Map<String, Object> reqBody = new HashMap<>();
            if (imageUrl != null) {
                // 同 WeComMainController 的坑：企业微信图片URL下载下来是密文，得先用本机器人自己的
                // EncodingAESKey解密再往下发，直接传原始URL会被视觉模型当成"不支持的图片格式"。
                try {
                    String dataUri = mediaDownloader.downloadAndDecryptAsDataUri(encodingAesKey, imageUrl);
                    reqBody.put("image_url", dataUri);
                } catch (Exception e) {
                    log.warn("[WeComZhangJing] 图片下载/解密失败: {}", e.getMessage());
                    reqBody.put("message", "（有一张图片下载失败，请提示用户重新发送）");
                }
            } else {
                reqBody.put("message", content);
            }
            reqBody.put("history", history);
            reqBody.put("persona", "leo");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Forwarded-For", "wecom-zhangjing-" + chatId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // 先秒回一条占位消息：Main/Leo 现在自己写SQL干活，复杂问题要跑几十秒到几分钟，
            // 干等着用户会以为机器人死了。流式消息用同一个 streamId，真结果出来再覆盖掉这句。
            String streamId = streamReplier.begin("WeComZhangJing", responseUrl, "收到了，我这就去查，请稍等…");

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

            log.info("[WeComZhangJing] 回复内容: {}", reply);
            streamReplier.finish("WeComZhangJing", responseUrl, streamId, reply);
        } catch (Exception e) {
            log.warn("[WeComZhangJing] 异步处理消息失败: {}", e.getMessage(), e);
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

    /** 从 payload 提出 chatId（优先用 chatid 字段，不然用 from.userid）。 */
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

    /** 获取或创建这个 chatId 的单线程 executor（保证消息串行处理）。 */
    private ExecutorService getOrCreateExecutor(String chatId) {
        return perChatExecutors.computeIfAbsent(chatId, k -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WeComZhangJing-" + chatId);
            t.setDaemon(true);
            return t;
        }));
    }
}
