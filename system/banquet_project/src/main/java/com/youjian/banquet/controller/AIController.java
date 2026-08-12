package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * AIController - 调用天龙 OpenClaw Gateway
 * 调用链：前端 → AIController(8080) → 天龙 OpenClaw Gateway(11500) → DeepSeek/Dashscope API
 *
 * 路由调整：@RequestMapping("/ai") → @RequestMapping("/api/ai")
 * 纳入 JWT 全局鉴权体系（/api/** 由 JwtAuthInterceptor 统一拦截）
 * Token 必须从环境变量 TIANLONG_TOKEN 读取，禁止硬编码
 *
 * 双门店改造：
 * 1. 调用日志记录到 ai_chat_history 表（staff_id / store_id / role / content / image_url）
 * 2. 简单限流：同一用户每分钟最多 10 次调用，超出返回 429
 * 3. 仅登录用户（JWT 校验通过）可调用，staff_id / store_id 从 JwtAuthInterceptor 注入的 request 属性读取
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AIController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    /** 每分钟最大调用次数 */
    private static final int MAX_CALLS_PER_MINUTE = 10;

    /** 限流窗口（毫秒） */
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    /** 每用户调用时间戳队列（用于滑动窗口限流） */
    private static final ConcurrentHashMap<Long, ConcurrentLinkedDeque<Long>> RATE_LIMIT_BUCKETS = new ConcurrentHashMap<>();

    @Value("${tianlong.base-url:http://127.0.0.1:11500}")
    private String baseUrl;

    @Value("${tianlong.token:}")
    private String token;

    @Value("${tianlong.default-model:deepseek/deepseek-chat}")
    private String defaultModel;

    @Value("${tianlong.vision-model:dashscope/qwen-vl-max}")
    private String visionModel;

    @Value("${tianlong.timeout:60000}")
    private int timeout;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 宴会方案建议 */
    @PostMapping("/banquet/suggest")
    public Result<Map<String, Object>> banquetSuggest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (!checkRateLimit(staffId)) {
            return Result.error(429, "AI 调用过于频繁，请稍后重试（每分钟最多 " + MAX_CALLS_PER_MINUTE + " 次）");
        }
        String prompt = buildBanquetPrompt(body);
        logAiCall(request, "user", prompt, null);
        try {
            String aiResponse = callTianlong(prompt, defaultModel);
            logAiCall(request, "assistant", aiResponse, null);
            Map<String, Object> result = new HashMap<>();
            result.put("suggestion", aiResponse);
            result.put("type", "banquet_suggest");
            return Result.success(result);
        } catch (Exception e) {
            logAiCall(request, "assistant", "ERROR: " + e.getMessage(), null);
            return Result.error(500, "AI 宴会建议失败: " + e.getMessage());
        }
    }

    /** 菜品推荐 */
    @PostMapping("/dish/recommend")
    public Result<Map<String, Object>> dishRecommend(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (!checkRateLimit(staffId)) {
            return Result.error(429, "AI 调用过于频繁，请稍后重试（每分钟最多 " + MAX_CALLS_PER_MINUTE + " 次）");
        }
        String prompt = buildDishPrompt(body);
        logAiCall(request, "user", prompt, null);
        try {
            String aiResponse = callTianlong(prompt, defaultModel);
            logAiCall(request, "assistant", aiResponse, null);
            Map<String, Object> result = new HashMap<>();
            result.put("recommendation", aiResponse);
            result.put("type", "dish_recommend");
            return Result.success(result);
        } catch (Exception e) {
            logAiCall(request, "assistant", "ERROR: " + e.getMessage(), null);
            return Result.error(500, "AI 菜品推荐失败: " + e.getMessage());
        }
    }

    /** 营销文案生成 */
    @PostMapping("/copy/generate")
    public Result<Map<String, Object>> copyGenerate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (!checkRateLimit(staffId)) {
            return Result.error(429, "AI 调用过于频繁，请稍后重试（每分钟最多 " + MAX_CALLS_PER_MINUTE + " 次）");
        }
        String prompt = buildCopyPrompt(body);
        logAiCall(request, "user", prompt, null);
        try {
            String aiResponse = callTianlong(prompt, defaultModel);
            logAiCall(request, "assistant", aiResponse, null);
            Map<String, Object> result = new HashMap<>();
            result.put("copywriting", aiResponse);
            result.put("type", "copy_generate");
            return Result.success(result);
        } catch (Exception e) {
            logAiCall(request, "assistant", "ERROR: " + e.getMessage(), null);
            return Result.error(500, "AI 文案生成失败: " + e.getMessage());
        }
    }

    /** AI 对话（通用） */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (!checkRateLimit(staffId)) {
            return Result.error(429, "AI 调用过于频繁，请稍后重试（每分钟最多 " + MAX_CALLS_PER_MINUTE + " 次）");
        }
        try {
            String message = (String) body.get("message");
            String imageUrl = (String) body.get("image_url");
            String model = (imageUrl != null && !imageUrl.isEmpty()) ? visionModel : defaultModel;

            logAiCall(request, "user", message != null ? message : "", imageUrl);

            String aiResponse;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                aiResponse = callTianlongWithImage(message, imageUrl, model);
            } else {
                aiResponse = callTianlong(message, model);
            }

            logAiCall(request, "assistant", aiResponse, null);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", aiResponse);
            result.put("model", model);
            return Result.success(result);
        } catch (Exception e) {
            logAiCall(request, "assistant", "ERROR: " + e.getMessage(), null);
            return Result.error(500, "AI 对话失败: " + e.getMessage());
        }
    }

    /** 获取当前用户的 AI 聊天历史 */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory(HttpServletRequest request) {
        try {
            Long staffId = resolveStaffId(request);
            if (staffId == null) return Result.error(401, "未授权");
            List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT role, content, image_url, create_time FROM ai_chat_history WHERE staff_id = ? ORDER BY id ASC LIMIT 50",
                staffId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(500, "获取历史记录失败: " + e.getMessage());
        }
    }

    /** 清空当前用户的 AI 聊天历史 */
    @PostMapping("/history/clear")
    public Result<Void> clearHistory(HttpServletRequest request) {
        try {
            Long staffId = resolveStaffId(request);
            if (staffId == null) return Result.error(401, "未授权");
            jdbcTemplate.update("DELETE FROM ai_chat_history WHERE staff_id = ?", staffId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "清空历史记录失败: " + e.getMessage());
        }
    }

    /** 获取可用模型列表 */
    @GetMapping("/models")
    public Result<Map<String, Object>> getModels() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, String>> models = new ArrayList<>();
        Map<String, String> m1 = new HashMap<>();
        m1.put("id", defaultModel);
        m1.put("name", "DeepSeek Chat");
        m1.put("type", "chat");
        models.add(m1);
        Map<String, String> m2 = new HashMap<>();
        m2.put("id", visionModel);
        m2.put("name", "通义千问 VL");
        m2.put("type", "vision");
        models.add(m2);
        data.put("models", models);
        data.put("defaultModel", defaultModel);
        return Result.success(data);
    }

    /** 带图片的 AI 对话 */
    @PostMapping("/chat-with-image")
    public Result<Map<String, Object>> chatWithImage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (!checkRateLimit(staffId)) {
            return Result.error(429, "AI 调用过于频繁，请稍后重试");
        }
        try {
            String message = (String) body.get("message");
            String imageBase64 = (String) body.get("image");
            String imageUrl = imageBase64 != null ? "data:image/jpeg;base64," + imageBase64 : null;

            logAiCall(request, "user", message != null ? message : "", imageUrl);

            String aiResponse = callTianlongWithImage(message, imageUrl, visionModel);
            logAiCall(request, "assistant", aiResponse, null);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", aiResponse);
            result.put("model", visionModel);
            return Result.success(result);
        } catch (Exception e) {
            logAiCall(request, "assistant", "ERROR: " + e.getMessage(), null);
            return Result.error(500, "AI 图片对话失败: " + e.getMessage());
        }
    }

    /**
     * 简单限流：滑动窗口算法，同一用户每分钟最多 MAX_CALLS_PER_MINUTE 次。
     * 利用 ConcurrentHashMap + ConcurrentLinkedDeque 实现线程安全。
     * 返回 true 表示允许调用，false 表示被限流。
     */
    private boolean checkRateLimit(Long staffId) {
        if (staffId == null) {
            // 未登录用户不应当走到这里（JWT 拦截器已拒绝），防御性返回 false
            return false;
        }
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> deque = RATE_LIMIT_BUCKETS.computeIfAbsent(staffId, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            // 清理超过窗口的旧记录
            while (!deque.isEmpty() && now - deque.peekFirst() > RATE_LIMIT_WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_CALLS_PER_MINUTE) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /**
     * 从 JWT 拦截器注入的 request 属性中解析 staffId。
     */
    private Long resolveStaffId(HttpServletRequest request) {
        Object staffId = request.getAttribute("jwt_staff_id");
        if (staffId instanceof Long) {
            return (Long) staffId;
        }
        if (staffId instanceof Number) {
            return ((Number) staffId).longValue();
        }
        return null;
    }

    /**
     * 从 JWT 拦截器注入的 request 属性中解析 storeId。
     */
    private Long resolveStoreId(HttpServletRequest request) {
        Object storeId = request.getAttribute("jwt_store_id");
        if (storeId instanceof Long) {
            return (Long) storeId;
        }
        if (storeId instanceof Number) {
            return ((Number) storeId).longValue();
        }
        return null;
    }

    /**
     * 记录 AI 调用日志到 ai_chat_history 表。
     * 表结构：id / staff_id / role / content / image_url / create_time / store_id
     */
    private void logAiCall(HttpServletRequest request, String role, String content, String imageUrl) {
        try {
            Long staffId = resolveStaffId(request);
            Long storeId = resolveStoreId(request);
            if (staffId == null) {
                log.warn("[AIChat] 无法记录日志：staffId 为空");
                return;
            }
            if (storeId == null) {
                storeId = 0L;
            }
            String truncatedContent = content;
            if (truncatedContent != null && truncatedContent.length() > 5000) {
                truncatedContent = truncatedContent.substring(0, 5000);
            }
            jdbcTemplate.update(
                    "INSERT INTO ai_chat_history (staff_id, role, content, image_url, store_id, create_time) VALUES (?, ?, ?, ?, ?, ?)",
                    staffId, role, truncatedContent, imageUrl, storeId, LocalDateTime.now()
            );
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.warn("[AIChat] 记录 AI 调用日志失败: {}", e.getMessage());
        }
    }

    /** 调用天龙 OpenClaw Gateway（纯文本） */
    private String callTianlong(String prompt, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "你是又见炊烟餐饮管理系统的AI助手炊小助，为餐饮经营提供专业建议。"),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/v1/chat/completions",
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

    /** 调用天龙 OpenClaw Gateway（带图片） */
    private String callTianlongWithImage(String prompt, String imageUrl, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "你是又见炊烟餐饮管理系统的AI助手炊小助，擅长分析图片内容。"),
            Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
            ))
        ));
        requestBody.put("max_tokens", 2000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/v1/chat/completions",
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
        return "AI 图片分析暂时无法响应，请稍后重试。";
    }

    private String buildBanquetPrompt(Map<String, Object> body) {
        StringBuilder sb = new StringBuilder("请为以下宴会需求提供方案建议：\n");
        sb.append("宴会类型：").append(body.getOrDefault("occasion_type", "未指定")).append("\n");
        sb.append("用餐人数：").append(body.getOrDefault("guest_count", "未指定")).append("\n");
        sb.append("桌数：").append(body.getOrDefault("table_count", "未指定")).append("\n");
        sb.append("预算：").append(body.getOrDefault("budget", "未指定")).append("\n");
        sb.append("特殊要求：").append(body.getOrDefault("special_request", "无")).append("\n");
        sb.append("请提供菜单搭配、场地布置和服务建议。");
        return sb.toString();
    }

    private String buildDishPrompt(Map<String, Object> body) {
        StringBuilder sb = new StringBuilder("请根据以下条件推荐菜品：\n");
        sb.append("偏好口味：").append(body.getOrDefault("taste", "不限")).append("\n");
        sb.append("忌口：").append(body.getOrDefault("avoid", "无")).append("\n");
        sb.append("人数：").append(body.getOrDefault("guest_count", "未指定")).append("\n");
        sb.append("预算：").append(body.getOrDefault("budget", "未指定")).append("\n");
        sb.append("请推荐适合的菜品组合。");
        return sb.toString();
    }

    private String buildCopyPrompt(Map<String, Object> body) {
        StringBuilder sb = new StringBuilder("请为以下内容生成营销文案：\n");
        sb.append("主题：").append(body.getOrDefault("topic", "餐厅推广")).append("\n");
        sb.append("渠道：").append(body.getOrDefault("channel", "朋友圈")).append("\n");
        sb.append("风格：").append(body.getOrDefault("style", "温馨")).append("\n");
        sb.append("请生成吸引人的营销文案。");
        return sb.toString();
    }
}
