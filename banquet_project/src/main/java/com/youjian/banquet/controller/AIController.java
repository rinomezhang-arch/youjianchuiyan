package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.AgentGatewayService;
import com.youjian.banquet.util.AiPersonaUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * AIController - 员工端 AI 助手（人设按角色区分：超管/总经理 = Main，其余员工 = Tom，见 AiPersonaUtil）
 * 调用链：前端 → AIController(8080) → AgentGatewayService → 天龙 OpenClaw 网关（127.0.0.1:11500）
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AgentGatewayService agentGateway;

    /** 人设名字（Main/Tom）映射到天龙网关的 agent 目标（model 字段填的是 agent 目标，不是底层模型名） */
    private String personaModel(HttpServletRequest request) {
        return "openclaw/" + AiPersonaUtil.personaName(request).toLowerCase();
    }

    /**
     * 按登录角色装配系统提示词身份前缀：超管/总经理看到的人设是 Main，其余员工是 Tom。
     * 关键：必须明确告诉模型"对方是谁"——员工端场景下对话者是内部员工/管理者本人，
     * 不是顾客，绝不能套用对客话术（例如"尊贵的客户"），否则会答非所问。
     */
    private String personaIdentityPrefix(HttpServletRequest request) {
        String persona = AiPersonaUtil.personaName(request);
        String name = staffName(request);
        String who = name != null ? "（当前对话者：" + name + "）" : "";
        if ("Main".equals(persona)) {
            return "你是又见炊烟餐饮管理系统内部的AI助手Main，专门服务系统最高权限的超级管理员/总经理本人" + who
                    + "。对方是管理者/老板，不是顾客，绝不要用“尊贵的客户”一类对客话术称呼对方，"
                    + "也不要自我介绍成面向普通用户的客服。你的职责是协助解读跨门店经营数据、给出管理决策建议、说明系统运行情况。";
        }
        return "你是又见炊烟餐饮管理系统的员工工作助手Tom，服务当前登录的在职员工本人" + who
                + "。对方是同事，不是顾客，不要用“尊贵的客户”一类对客话术。"
                + "你的职责是帮同事处理日常工作：查数据、写报表摘要、解答系统操作和门店流程问题。";
    }

    private String systemPromptText(HttpServletRequest request) {
        return personaIdentityPrefix(request) + "请用中文回复，语气像同事间的工作交流，专业、直接、不客套。"
                + "硬性规则：凡是涉及具体数字、门店信息、菜品数量等事实类问题，必须调用对应工具查询真实数据，"
                + "禁止凭自己的“记忆”或猜测直接给出数字或编造门店/菜品名称——编造事实是严重错误，"
                + "宁可如实说“需要先查一下”，也不能编。没有对应工具能回答的问题，如实说明查不到，不要编。";
    }

    private String systemPromptVision(HttpServletRequest request) {
        return personaIdentityPrefix(request) + "你还擅长分析图片内容。请用中文回复。";
    }

    /** Main/Tom 可用的只读查询工具——防止模型在事实性问题上编造数据。 */
    private List<AgentGatewayService.ToolSpec> buildReadOnlyTools(HttpServletRequest request) {
        List<AgentGatewayService.ToolSpec> tools = new ArrayList<>();

        tools.add(new AgentGatewayService.ToolSpec(
                "get_store_list",
                "查询系统里真实的门店列表，返回每个门店的ID、名称、地址、状态。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> jdbcTemplate.queryForList(
                        "SELECT store_id, store_name, address, status FROM store_info ORDER BY sort_order, store_id")
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_dish_count",
                "查询在售菜品总数。可选按门店过滤；不传 storeId 则返回全部门店汇总加逐店明细。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，不传则查全部门店")
                ), "required", List.of()),
                args -> {
                    Object storeIdArg = args.get("storeId");
                    if (storeIdArg != null) {
                        Long storeId = Long.valueOf(storeIdArg.toString());
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM dish_master WHERE store_id = ? AND is_active = 1 AND sale_price > 0",
                                Integer.class, storeId);
                        return Map.of("storeId", storeId, "dishCount", count);
                    }
                    List<Map<String, Object>> perStore = jdbcTemplate.queryForList(
                            "SELECT store_id, COUNT(*) AS dish_count FROM dish_master " +
                                    "WHERE is_active = 1 AND sale_price > 0 GROUP BY store_id");
                    Integer total = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM dish_master WHERE is_active = 1 AND sale_price > 0", Integer.class);
                    return Map.of("totalDishCount", total, "byStore", perStore);
                }
        ));

        // Tom 只能看自己门店的预定；Main（总经理/超管）能看全部门店——门店范围在这里就锁死，
        // 不是靠提示词提醒，模型传别的 storeId 进来也会被这里强制改回它自己的门店。
        boolean isGm = "Main".equals(AiPersonaUtil.personaName(request));
        Long ownStoreId = resolveStoreId(request);

        tools.add(new AgentGatewayService.ToolSpec(
                "get_bookings",
                "查询某天的真实预订/宴会数据（客户、人数、桌数、状态等）。不传日期默认查今天。" +
                        (isGm ? "可选按门店过滤，不传则查全部门店。" : "只能查你自己门店的数据。"),
                Map.of("type", "object", "properties", Map.of(
                        "date", Map.of("type", "string", "description", "查询日期，格式 yyyy-MM-dd，不传默认今天"),
                        "storeId", Map.of("type", "integer", "description", isGm ? "门店ID，不传则查全部门店" : "无效，会被忽略，只查你自己门店")
                ), "required", List.of()),
                args -> {
                    String date = args.get("date") != null ? args.get("date").toString() : java.time.LocalDate.now().toString();
                    Long storeId = isGm && args.get("storeId") != null ? Long.valueOf(args.get("storeId").toString()) : ownStoreId;
                    String sql = "SELECT booking_id, store_id, customer_name, guest_count, table_count, booking_time, " +
                            "banquet_name, occasion_type, booking_status, package_name FROM booking_master " +
                            "WHERE booking_date = ?" + (storeId != null ? " AND store_id = ?" : "") + " ORDER BY booking_time";
                    List<Map<String, Object>> rows = storeId != null
                            ? jdbcTemplate.queryForList(sql, date, storeId)
                            : jdbcTemplate.queryForList(sql, date);
                    return Map.of("date", date, "count", rows.size(), "bookings", rows);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_pending_inquiries",
                "查询还没被门店处理的客人预定留资（官网/小程序客人自己提交、等员工电话确认的那种），" +
                        "帮忙统计\"今天有多少客人留资还没跟进\"这类问题用这个。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", isGm ? "门店ID，不传则查全部门店" : "无效，会被忽略，只查你自己门店")
                ), "required", List.of()),
                args -> {
                    Long storeId = isGm && args.get("storeId") != null ? Long.valueOf(args.get("storeId").toString()) : ownStoreId;
                    String sql = "SELECT id, store_id, customer_name, customer_phone, preferred_date, preferred_time, " +
                            "guest_count, remark, created_at FROM booking_inquiry WHERE status = 'pending'" +
                            (storeId != null ? " AND store_id = ?" : "") + " ORDER BY created_at DESC LIMIT 30";
                    List<Map<String, Object>> rows = storeId != null
                            ? jdbcTemplate.queryForList(sql, storeId)
                            : jdbcTemplate.queryForList(sql);
                    return Map.of("count", rows.size(), "inquiries", rows);
                }
        ));

        return tools;
    }

    /**
     * 拉取当前用户最近 N 轮对话历史，拼成模型能用的 messages 数组——
     * 之前的实现每次只发当前这一句给模型，模型完全没有上下文记忆，
     * 造成"反复问同一个问题""答非所问"这类体验问题。
     */
    private List<Map<String, Object>> loadRecentHistoryMessages(Long staffId, int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (staffId == null) return messages;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT role, content FROM ai_chat_history WHERE staff_id = ? ORDER BY id DESC LIMIT ?",
                    staffId, limit);
            Collections.reverse(rows);
            for (Map<String, Object> row : rows) {
                String role = (String) row.get("role");
                String content = (String) row.get("content");
                if (content == null || content.startsWith("ERROR:")) continue;
                Map<String, Object> m = new HashMap<>();
                m.put("role", role);
                m.put("content", content);
                messages.add(m);
            }
        } catch (Exception e) {
            log.warn("[AIChat] 加载历史上下文失败: {}", e.getMessage());
        }
        return messages;
    }

    /** 查询当前登录员工姓名，用于让 AI 知道自己在跟谁说话；查不到时静默返回 null，不影响主流程。 */
    private String staffName(HttpServletRequest request) {
        Long staffId = resolveStaffId(request);
        if (staffId == null) return null;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT staff_name FROM staff_master WHERE staff_id = ? LIMIT 1", staffId);
            if (!rows.isEmpty()) return (String) rows.get(0).get("staff_name");
        } catch (Exception ignored) {
        }
        return null;
    }

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
            String aiResponse = agentGateway.chatText(personaModel(request), systemPromptText(request), prompt);
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
            String aiResponse = agentGateway.chatText(personaModel(request), systemPromptText(request), prompt);
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
            String aiResponse = agentGateway.chatText(personaModel(request), systemPromptText(request), prompt);
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
            boolean hasImage = imageUrl != null && !imageUrl.isEmpty();
            String model = hasImage ? agentGateway.defaultVisionModel() : personaModel(request);

            // 先拿历史（在这条新消息落库之前拿，否则会把当前这句重复拼进去）
            List<Map<String, Object>> history = hasImage ? List.of() : loadRecentHistoryMessages(staffId, 12);

            logAiCall(request, "user", message != null ? message : "", imageUrl);

            String aiResponse;
            if (hasImage) {
                aiResponse = agentGateway.chatVision(systemPromptVision(request), message, imageUrl);
            } else {
                List<Map<String, Object>> messages = new ArrayList<>();
                Map<String, Object> sys = new HashMap<>();
                sys.put("role", "system");
                sys.put("content", systemPromptText(request));
                messages.add(sys);
                messages.addAll(history);
                Map<String, Object> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", message);
                messages.add(userMsg);
                aiResponse = agentGateway.chatWithTools(model, messages, buildReadOnlyTools(request));
            }

            logAiCall(request, "assistant", aiResponse, null);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", aiResponse);
            result.put("model", model);
            result.put("persona", AiPersonaUtil.personaName(request));
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
                "SELECT role, content, image_url, created_at FROM ai_chat_history WHERE staff_id = ? ORDER BY id ASC LIMIT 50",
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
    public Result<Map<String, Object>> getModels(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, String>> models = new ArrayList<>();
        String chatModel = personaModel(request);
        Map<String, String> m1 = new HashMap<>();
        m1.put("id", chatModel);
        m1.put("name", AiPersonaUtil.personaName(request));
        m1.put("type", "chat");
        models.add(m1);
        Map<String, String> m2 = new HashMap<>();
        m2.put("id", agentGateway.defaultVisionModel());
        m2.put("name", "通义千问 VL");
        m2.put("type", "vision");
        models.add(m2);
        data.put("models", models);
        data.put("defaultModel", chatModel);
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

            String aiResponse = agentGateway.chatVision(systemPromptVision(request), message, imageUrl);
            logAiCall(request, "assistant", aiResponse, null);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", aiResponse);
            result.put("model", agentGateway.defaultVisionModel());
            result.put("persona", AiPersonaUtil.personaName(request));
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
     * 表结构：id / staff_id / role / content / image_url / created_at / store_id
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
                    "INSERT INTO ai_chat_history (staff_id, role, content, image_url, store_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    staffId, role, truncatedContent, imageUrl, storeId, LocalDateTime.now()
            );
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.warn("[AIChat] 记录 AI 调用日志失败: {}", e.getMessage());
        }
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
