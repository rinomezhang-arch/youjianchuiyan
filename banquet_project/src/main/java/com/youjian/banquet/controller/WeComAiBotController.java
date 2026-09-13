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

/**
 * 企业微信"智能机器人"URL回调接入点（客服AI）。
 * 协议：GET 用于首次保存URL时的签名+echostr验证；POST 推送加密消息，服务端要先立即同步response，
 * 再用消息体里的 response_url（一次性，1小时有效）异步把AI回复推回去——不能在POST的HTTP响应里
 * 直接同步带上业务回复，企业微信不认。
 * 业务逻辑（人设/工具/真实数据查询）不重复实现，直接内部回环调用已经在生产验证过的
 * EllaController（/api/public/agent/ella/chat），保持"网页版Ella"和"企业微信客服AI"背后是同一套大脑、
 * 同一套防幻觉工具集，不会出现两边各说各话。
 */
@RestController
@RequestMapping("/api/public/wecom/aibot")
public class WeComAiBotController {

    private static final Logger log = LoggerFactory.getLogger(WeComAiBotController.class);

    private static final int HISTORY_MAX_TURNS = 12;
    private static final int HISTORY_MAX_CHATS = 500;

    @Value("${wecom.aibot.token:}")
    private String token;

    @Value("${wecom.aibot.encoding-aes-key:}")
    private String encodingAesKey;

    @Autowired
    private WeComMediaDownloader mediaDownloader;

    @Autowired
    private WeComStreamReplier streamReplier;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // chatid -> 最近几轮对话，机器人进程重启（后端重启）就清空，够用，不做持久化
    private final Map<String, List<Map<String, Object>>> histories = new ConcurrentHashMap<>();

    @GetMapping("/callback")
    public ResponseEntity<String> verify(@RequestParam("msg_signature") String msgSignature,
                                          @RequestParam("timestamp") String timestamp,
                                          @RequestParam("nonce") String nonce,
                                          @RequestParam("echostr") String echostr) {
        if (token.isBlank() || encodingAesKey.isBlank()) {
            log.warn("[WeComAiBot] 收到验证请求，但 wecom.aibot.token / encoding-aes-key 还没配置");
            return ResponseEntity.status(500).body("not configured");
        }
        String expected = WeComCryptoUtil.sign(token, timestamp, nonce, echostr);
        if (!expected.equals(msgSignature)) {
            log.warn("[WeComAiBot] 验证签名不匹配");
            return ResponseEntity.status(403).body("signature mismatch");
        }
        try {
            String plain = WeComCryptoUtil.decrypt(encodingAesKey, echostr);
            log.info("[WeComAiBot] URL 验证通过");
            return ResponseEntity.ok(plain);
        } catch (Exception e) {
            log.warn("[WeComAiBot] 验证echostr解密失败: {}", e.getMessage());
            return ResponseEntity.status(400).body("decrypt failed");
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<String> receive(@RequestParam("msg_signature") String msgSignature,
                                           @RequestParam("timestamp") String timestamp,
                                           @RequestParam("nonce") String nonce,
                                           @RequestBody String rawBody) {
        try {
            // 实测确认：智能机器人URL回调推送的是 JSON {"encrypt":"..."}，不是自建应用/公众号
            // 那种经典XML信封——官方"回调配置"文档描述的是后者，这里踩了一次坑，靠真实请求日志验证过了
            @SuppressWarnings("unchecked")
            Map<String, Object> outer = objectMapper.readValue(rawBody, Map.class);
            String encrypt = (String) outer.get("encrypt");
            String expected = WeComCryptoUtil.sign(token, timestamp, nonce, encrypt);
            if (!expected.equals(msgSignature)) {
                log.warn("[WeComAiBot] POST 签名不匹配");
                return ResponseEntity.ok("");
            }
            String plainJson = WeComCryptoUtil.decrypt(encodingAesKey, encrypt);
            log.info("[WeComAiBot] 解密后明文: {}", plainJson);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(plainJson, Map.class);
            // 立即同步返回空响应，AI 回复走后台线程 + response_url 异步推送——
            // 企业微信这个POST请求本身不等业务处理完，等太久会被当成超时失败
            new Thread(() -> handleAsync(payload)).start();
        } catch (Exception e) {
            log.warn("[WeComAiBot] 处理回调失败: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("");
    }

    @SuppressWarnings("unchecked")
    private void handleAsync(Map<String, Object> payload) {
        try {
            String msgtype = str(payload.get("msgtype"));
            String content = null;
            String imageUrl = null;
            if ("text".equals(msgtype)) {
                Map<String, Object> textObj = (Map<String, Object>) payload.get("text");
                content = textObj == null ? null : str(textObj.get("content"));
            } else if ("voice".equals(msgtype)) {
                // 企业微信官方已经把语音转写成文字了（voice.content），不需要我们自己接语音识别
                Map<String, Object> voiceObj = (Map<String, Object>) payload.get("voice");
                content = voiceObj == null ? null : str(voiceObj.get("content"));
            } else if ("image".equals(msgtype)) {
                // Ella 是面向顾客的客服，按规则不开放图片识别（同 EllaController 里的处理）：
                // 顾客发的图跟点菜订位基本无关，而且对外开放视觉模型等于给了个免费的通用识图接口。
                // 这里直接回一句就结束，连图都不下载，省得白跑一趟 COS。
                log.info("[WeComAiBot] 收到图片消息，按规则不识别，chatId={}", str(payload.get("chatid")));
                String responseUrlForImage = str(payload.get("response_url"));
                if (responseUrlForImage != null) {
                    streamReplier.finish("WeComAiBot", responseUrlForImage, "img-skip",
                            "不好意思，我这边看不了图片呢~ 您想了解哪道菜、或者想订位子，直接打字告诉我就行，我马上帮您查 😊");
                }
                return;
            } else {
                log.info("[WeComAiBot] 暂不处理消息类型: {}", msgtype);
                return;
            }
            // 单聊消息实测不带 chatid 字段（大概只有群聊才有，用来区分是哪个群）——
            // 单聊用 from.userid 当会话标识，同一个人的历史消息才能串起来
            String chatId = str(payload.get("chatid"));
            if (chatId == null) {
                Map<String, Object> from = (Map<String, Object>) payload.get("from");
                chatId = from == null ? null : str(from.get("userid"));
            }
            String responseUrl = str(payload.get("response_url"));
            boolean hasContent = (content != null && !content.isBlank()) || imageUrl != null;
            if (!hasContent || responseUrl == null || chatId == null) {
                log.warn("[WeComAiBot] 消息字段不完整，跳过。content={}, imageUrl={}, chatId={}, responseUrl={}",
                        content, imageUrl, chatId, responseUrl);
                return;
            }
            log.info("[WeComAiBot] 处理消息 chatId={}, content={}, imageUrl={}", chatId, content, imageUrl != null);

            List<Map<String, Object>> history = getHistory(chatId);

            Map<String, Object> reqBody = new HashMap<>();
            if (imageUrl != null) {
                // 图片URL下载下来是密文，得先用本机器人自己的EncodingAESKey解密，转成data URI后
                // 再往下发——直接把原始URL传给视觉模型会读到加密乱码（同Main/Tom机器人踩过的坑）。
                try {
                    String dataUri = mediaDownloader.downloadAndDecryptAsDataUri(encodingAesKey, imageUrl);
                    reqBody.put("image_url", dataUri);
                } catch (Exception e) {
                    log.warn("[WeComAiBot] 图片下载/解密失败: {}", e.getMessage());
                    reqBody.put("message", "（有一张图片下载失败，请提示用户重新发送）");
                }
            } else {
                reqBody.put("message", content);
            }
            reqBody.put("history", history);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 所有企业微信会话都从这台服务器自己回环调用，出口IP相同——EllaController按IP限流，
            // 这里按chatid伪造一个区分用的转发IP，避免所有WeCom用户挤占同一个限流额度
            headers.set("X-Forwarded-For", "wecom-" + chatId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // 先秒回一条占位消息：Main/Leo 现在自己写SQL干活，复杂问题要跑几十秒到几分钟，
            // 干等着用户会以为机器人死了。流式消息用同一个 streamId，真结果出来再覆盖掉这句。
            String streamId = streamReplier.begin("WeComAiBot", responseUrl, "收到啦，我马上帮您看看，请稍等…");

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "http://127.0.0.1:8080/api/public/agent/ella/chat", entity, Map.class);

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

            log.info("[WeComAiBot] 回复内容: {}", reply);
            streamReplier.finish("WeComAiBot", responseUrl, streamId, reply);
        } catch (Exception e) {
            log.warn("[WeComAiBot] 异步处理消息失败: {}", e.getMessage(), e);
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
}
