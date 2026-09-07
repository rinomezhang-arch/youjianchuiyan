package com.youjian.banquet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 三个 AI persona（Ella / Tom / Main）共用的云端模型调用底座。
 * <p>
 * 直连天龙的 OpenClaw 网关（{@code 127.0.0.1:11500}，与生产后端同机部署，OpenAI 兼容
 * {@code /v1/chat/completions}）——{@code model} 字段填 agent 目标（{@code openclaw/main}/
 * {@code openclaw/tom}/{@code openclaw/ella}），不是底层模型名，路由由网关自己决定。
 * 已实测确认该网关支持客户端自定义工具透传（真实发起过一次 tool_calls 往返）。
 * 视觉理解走 DeepSeek 官方 {@code deepseek-v4-flash-vision-exp} 多模态模型直连（不是天龙网关，
 * 天龙网关的 {@code /v1/chat/completions} 外部接口实测明确拒绝 {@code image_url} 内容；
 * 也不是百炼 Dashscope——那边配置的几把 key 全是 OpenClaw workspace 专属代理 token，
 * 直接打官方接口一律 401，2026-09-02 逐个实测排除）。用的是跟文字聊天同一把已验证有效的
 * DeepSeek Key（{@code deepseek.api-key}），2026-09-02 用真实图片实测过返回正确结果。
 * Token 必须从环境变量 {@code TIANLONG_TOKEN} / {@code DEEPSEEK_API_KEY} 读取，禁止硬编码。
 */
@Service
public class AgentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AgentGatewayService.class);

    /** 工具函数定义：交给模型的 JSON Schema + 真正在后端执行的逻辑（executor），模型只负责决定"要不要调、传什么参数"。 */
    public record ToolSpec(String name, String description, Map<String, Object> parameters,
                            Function<Map<String, Object>, Object> executor) {
    }

    /** 单轮工具调用循环最多跑几轮，防止模型异常时无限调工具 */
    private static final int MAX_TOOL_ROUNDS = 8;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tianlong.base-url:http://127.0.0.1:11500}")
    private String tianlongBaseUrl;

    @Value("${tianlong.token:}")
    private String tianlongToken;

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String dashscopeBaseUrl;

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${dashscope.vision-model:qwen-vl-max}")
    private String dashscopeVisionModel;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${deepseek.vision-model:deepseek-v4-flash-vision-exp}")
    private String deepseekVisionModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public String defaultVisionModel() {
        return deepseekVisionModel;
    }

    /**
     * 联网搜索，直连百炼 Dashscope 兼容模式，用 {@code enable_search} 参数触发模型自带的联网插件。
     * 给 Main/Leo 查系统数据库里没有的外部信息（新闻、行情、法规等）用，不经过天龙网关——
     * 天龙那边的自定义工具透传目前只验证过纯函数调用，没验证过网关自己转发联网插件参数。
     */
    @SuppressWarnings("unchecked")
    public String webSearch(String query) {
        if (dashscopeApiKey.isBlank()) {
            return "联网搜索功能还没配置好（缺少 DASHSCOPE_API_KEY），暂时无法搜索。";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(dashscopeApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-plus");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是网络信息检索助手，请联网搜索并给出简洁准确的中文回答，" +
                                    "涉及数字/日期/名称等事实请注明来源网站名。"),
                    Map.of("role", "user", "content", query)
            ));
            requestBody.put("enable_search", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    dashscopeBaseUrl + "/chat/completions", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    Object content = message.get("content");
                    if (content != null && !content.toString().isBlank()) {
                        return content.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AgentGatewayService] 联网搜索失败: query={}, error={}", query, e.getMessage());
        }
        return "联网搜索暂时失败了，请稍后重试或换个问法。";
    }

    /** 纯文本对话，走天龙网关，model 传 openclaw/main 或 openclaw/tom（agent 目标） */
    public String chatText(String model, String systemPrompt, String userPrompt) {
        return callChatCompletions(tianlongBaseUrl, tianlongToken, model, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
    }

    /**
     * 带图片对话，直连 DeepSeek 官方多模态 API（{@code deepseek-v4-flash-vision-exp}）。
     * <p>
     * imageUrlOrDataUri 传进来的必须已经是"可以直接给DeepSeek用"的内容——要么是公开可访问的
     * 真实图片URL，要么是 {@code data:image/...;base64,...} 格式的data URI。企业微信这种需要
     * 先解密的加密媒体，解密工作在各自的 WeComXxxController 里用 {@code WeComMediaDownloader}
     * 做完、转成 data URI 之后再调这个方法——这里不做任何下载/解密，因为解密需要"这条消息具体
     * 是哪个机器人收到的"对应的 EncodingAESKey，这个信息这一层拿不到（AgentGatewayService是
     * Ella/Tom/Main/Leo四个人设共用的，不该跟任何一个的密钥绑定）。
     */
    public String chatVision(String systemPrompt, String userPrompt, String imageUrlOrDataUri) {
        return chatVision(systemPrompt, userPrompt, imageUrlOrDataUri, List.of());
    }

    /**
     * 带上下文的看图对话。{@code history} 是之前几轮的纯文本 user/assistant 消息。
     * <p>
     * 之前看图是"无上下文"的——同事在对话里聊了半天，发一张图，模型那一刻什么都不知道，
     * 只能干巴巴地描述一遍图片。实际用法几乎都是接着前面的话题发图（"你看这个""就这台"），
     * 没有上下文就答不到点上。图片连同指令放在最后一条消息，指令离图片最近。
     */
    public String chatVision(String systemPrompt, String userPrompt, String imageUrlOrDataUri,
                             List<Map<String, Object>> history) {
        if (deepseekApiKey.isBlank()) {
            return "视觉理解功能还没配置好（缺少 DEEPSEEK_API_KEY），暂时无法识别图片。";
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> h : history) {
            Object role = h.get("role");
            Object content = h.get("content");
            if (role == null || content == null) continue;
            messages.add(Map.of("role", role, "content", content));
        }
        messages.add(Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", systemPrompt + "\n\n" + userPrompt),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrlOrDataUri))
        )));
        return callChatCompletions(deepseekBaseUrl, deepseekApiKey, deepseekVisionModel, messages);
    }

    /**
     * 带工具调用的文本对话，直连 DeepSeek 云端 API。
     * <p>
     * 传入完整对话历史（{@code messages} 已包含 system + 之前的多轮 user/assistant），
     * 以及一批 {@link ToolSpec}。模型如果决定调用工具，本方法在服务端真正执行 executor、
     * 把结果拼回对话继续问模型，直到模型给出最终文字回复或达到轮数上限。
     * <p>
     * 这是防止 AI 编造事实性数字（"多少道菜""多少家门店"之类）的关键机制——
     * 系统提示词要求模型对这类问题必须调用工具查询，不允许凭"记忆"直接回答。
     */
    @SuppressWarnings("unchecked")
    public String chatWithTools(String model, List<Map<String, Object>> messages, List<ToolSpec> tools) {
        List<Map<String, Object>> conversation = new ArrayList<>(messages);
        List<Map<String, Object>> toolDefs = tools.stream().map(t -> Map.<String, Object>of(
                "type", "function",
                "function", Map.of(
                        "name", t.name(),
                        "description", t.description(),
                        "parameters", t.parameters()
                )
        )).toList();

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            // 最后一轮禁掉工具，逼模型用已经查回来的结果给出文字答案，而不是把结果全丢掉
            boolean lastRound = round == MAX_TOOL_ROUNDS - 1;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", conversation);
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.3);
            // 工具列表为空时不要往请求里塞空数组——网关会当成非法参数拒绝，
            // 而且这种情况本来就是"让agent自己用它本身的全套能力干活"，不需要注入任何快捷工具。
            if (!toolDefs.isEmpty()) {
                requestBody.put("tools", toolDefs);
                requestBody.put("tool_choice", lastRound ? "none" : "auto");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tianlongToken);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tianlongBaseUrl + "/v1/chat/completions", entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "AI 服务暂时无法响应，请稍后重试。";
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                return "AI 服务暂时无法响应，请稍后重试。";
            }
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> assistantMessage = (Map<String, Object>) choice.get("message");
            String finishReason = (String) choice.get("finish_reason");
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) assistantMessage.get("tool_calls");

            if (lastRound || !"tool_calls".equals(finishReason) || toolCalls == null || toolCalls.isEmpty()) {
                Object content = assistantMessage.get("content");
                if (content != null && !content.toString().isBlank()) {
                    return redactSecrets(content.toString());
                }
                return "AI 服务暂时无法响应，请稍后重试。";
            }

            conversation.add(assistantMessage);
            for (Map<String, Object> call : toolCalls) {
                String callId = (String) call.get("id");
                Map<String, Object> function = (Map<String, Object>) call.get("function");
                String toolName = (String) function.get("name");
                String argsJson = (String) function.get("arguments");

                String toolResult = executeTool(tools, toolName, argsJson);

                Map<String, Object> toolMessage = new HashMap<>();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", callId);
                toolMessage.put("content", toolResult);
                conversation.add(toolMessage);
            }
        }
        return "AI 服务暂时无法响应，请稍后重试。";
    }

    /**
     * 回复内容发出去之前先擦掉里面可能夹带的机密。
     * <p>
     * 背景（2026-09-03 实测撞到）：agent 自己跑 shell 出错时，OpenClaw 会把失败的命令原样附在回复末尾
     * （形如 {@code ⚠️ Exec failed: `mysql -h localhost -u rino -pXXXX ...`}），
     * 而这条回复是直接推给企业微信的——群里一发就等于当众泄漏数据库密码。
     * 这里做最后一道兜底，不依赖模型"自觉不要说出来"。
     */
    private String redactSecrets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text
                // mysql -pXXXX / -p XXXX（-p 后面直接跟密码是 mysql 客户端的写法）
                .replaceAll("(?i)(-p)\\s*(?!assword\\b)\\S+", "$1***")
                // PASSWORD=xxx / api_key: xxx / token = xxx 这类通用形式
                .replaceAll("(?i)\\b(password|passwd|pwd|api[_-]?key|secret|token)\\b\\s*[:=]\\s*\\S+", "$1=***")
                // 常见 API key 前缀（sk-xxx / AKID...）
                .replaceAll("\\bsk-[A-Za-z0-9._\\-]{8,}", "sk-***")
                .replaceAll("\\bAKID[A-Za-z0-9]{10,}", "AKID***");
    }

    private String executeTool(List<ToolSpec> tools, String toolName, String argsJson) {
        try {
            ToolSpec spec = tools.stream().filter(t -> t.name().equals(toolName)).findFirst().orElse(null);
            if (spec == null) {
                return "{\"error\":\"未知工具: " + toolName + "\"}";
            }
            Map<String, Object> args = (argsJson == null || argsJson.isBlank())
                    ? Map.of()
                    : objectMapper.readValue(argsJson, Map.class);
            Object result = spec.executor().apply(args);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("[AgentGatewayService] 工具执行失败: tool={}, error={}", toolName, e.getMessage());
            return "{\"error\":\"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private String callChatCompletions(String baseUrl, String apiKey, String model, List<Map<String, Object>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions",
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return "AI 服务暂时无法响应，请稍后重试。";
    }
}
