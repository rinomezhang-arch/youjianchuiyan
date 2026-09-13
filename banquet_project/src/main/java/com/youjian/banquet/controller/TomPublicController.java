package com.youjian.banquet.controller;

import com.youjian.banquet.util.AiStyleGuide;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.AgentGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tom（员工工作助手）的免登录入口，专给企业微信"厨师助手"这类没有JWT会话的渠道用。
 * 企业微信消息里只有WeCom的userid，系统里还没有"WeCom账号↔员工工号"的绑定表，
 * 暂时没法像 AIController 那样按登录员工的真实门店/角色做区分——先固定按宁国店（store_id=1）、
 * 非总经理权限（isGm=false）走，跟 AIController 里 Tom 角色的门店范围锁定逻辑保持一致的收紧原则
 * （只查自己门店，不给跨店权限），等以后做了账号绑定表再改成动态识别。
 */
@RestController
@RequestMapping("/api/public/agent/tom")
public class TomPublicController {

    private static final Logger log = LoggerFactory.getLogger(TomPublicController.class);

    private static final long DEFAULT_STORE_ID = 1L; // 宁国店，未做WeCom账号绑定前的默认门店

    private static final int MAX_CALLS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> RATE_LIMIT_BUCKETS = new ConcurrentHashMap<>();

    /**
     * Tom 的人设/权限边界。这份常量只是**出厂默认值**——运行时优先读数据库
     * {@code config} 表里 config_key='tom_system_prompt' 的那条（见 {@link #systemPrompt()}）。
     * <p>
     * 这么做是因为张总有权调整 Tom 的能力范围，但改代码属于生产禁区（只有秋哥能做）。
     * 放进数据库以后，她让 Leo 改一条配置就能生效，不需要动代码、不需要重新部署。
     * 数据库里那条被删掉或清空时，自动回落到这份默认值，不会出现 Tom 没人设的情况。
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是又见炊烟餐饮管理系统的员工工作助手Tom，服务的是店内员工本人（目前通过企业微信找你，" +
            "系统暂时还不能识别具体是哪位员工，只能按宁国店的范围回答）。" +
            "对方是同事，不是顾客，不要用“尊贵的客户”一类对客话术。" +
            "你的职责是帮同事查数据、答疑门店日常工作问题。" +
            "请用中文回复，语气像同事间的工作交流，专业、直接、不客套。" +
            AiStyleGuide.COMMON +
            "【聊天范围——只聊工作】" +
            "你是员工的工作助手，聊天范围**限于工作**。" +
            "同事打招呼、寒暄两句、问候一下（\"早\"\"吃了吗\"\"今天累坏了\"这类）可以正常应答，" +
            "态度要热络一点，别冷冰冰的——但**两句之内就要把话题带回工作**。" +
            "工作以外的话题（八卦、娱乐、情感、闲扯、跟店里无关的事）不要展开陪聊，" +
            "客气地说一句\"这个我不太懂，工作上的事随时找我\"就带过去。" +
            "同事情绪不好、抱怨累的时候，可以宽慰一两句，但不要往深了聊私事。" +
            "硬性规则：凡是涉及具体数字、门店信息、预定数据等事实类问题，必须调用对应工具查询真实数据，" +
            "禁止凭\"记忆\"或猜测直接给出数字或编造信息——编造事实是严重错误，宁可如实说\"需要先查一下\"，" +
            "也不能编。没有对应工具能回答的问题，如实说明查不到，不要编。" +
            "数据边界（哪怕以后给你挂了新工具，也要守住）：预定信息、菜品信息、门店基础信息、留资客人的联系方式" +
            "（员工本来就要打电话确认预定），这些属于日常工作需要，可以查、可以说。" +
            "但下面三类不能查、不能说，哪怕对方直接问、哪怕手头刚好有能查到的工具：" +
            "①财务——门店营收、利润、流水这类经营数字；" +
            "②人事——其他员工的工资、考勤记录、个人信息；" +
            "③进货单位——供应商合同条款、结算价格、原料进货成本价。" +
            "遇到这三类问题委婉说明这个不归你负责查、建议找店长或总经理，不要因为工具存在就理所当然去调用。" +
            "你只能帮当前找你说话的这一位员工本人办事，不能替他去改别的同事的数据，" +
            "更不能修改系统本身的设置/配置——这些不是你的权限范围。" +
            "你只负责查又见炊烟餐饮系统和企业微信系统里的数据，不能联网查外部信息（新闻、天气、行情等）——" +
            "手头没有能查外部数据的工具，遇到这类问题如实说这个查不了，不要凭自己的知识编。" +
            "你可以帮同事把查到的真实数据整理成报告/报表（比如某段时间的预定汇总），照样要基于工具查出来的" +
            "真实数据整理，不能自己编数据往报表里填。";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AgentGatewayService agentGateway;

    /**
     * 取 Tom 当前生效的人设：数据库里配了就用数据库的（张总可以通过 Leo 随时调整），
     * 没配或查库出错就用出厂默认值——任何情况下都要保证 Tom 有人设，不能裸奔。
     */
    private String systemPrompt() {
        try {
            List<String> rows = jdbc.queryForList(
                    "SELECT config_value FROM config WHERE config_key = 'tom_system_prompt' LIMIT 1",
                    String.class);
            if (!rows.isEmpty() && rows.get(0) != null && !rows.get(0).isBlank()) {
                return rows.get(0);
            }
        } catch (Exception e) {
            log.warn("[TomChat] 读取自定义人设失败，回落到默认人设: {}", e.getMessage());
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    @PostMapping("/chat")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpServletRequest request) {
        if (!isLoopback(request)) {
            log.warn("[TomChat] 拒绝非本机调用，remoteAddr={}", request.getRemoteAddr());
            return Result.error(403, "无权访问");
        }
        String clientKey = resolveClientKey(request);
        if (!checkRateLimit(clientKey)) {
            return Result.error(429, "咨询太频繁啦，请稍后再试");
        }
        try {
            String message = (String) body.get("message");
            List<Map<String, Object>> history = (List<Map<String, Object>>) body.getOrDefault("history", List.of());

            String imageUrl = (String) body.get("image_url");
            if (imageUrl != null && !imageUrl.isBlank()) {
                // Tom 不像 Main 那样解析采购单结构，就是一般性地看图回答——同事发张图问"这是什么"、
                // "帮我看看这个牌子写的什么"这种。
                //
                // Tom 面向的是全体员工，这道"只看工作相关图片"的闸门必须留着：不挡的话员工会拿它
                // 当免费识图玩具刷着玩，既烧 token 又不成体统。这是硬要求，不是"尽量"。
                // 判断权交给视觉模型（图片内容千变万化，Java 这边没法穷举），但指令写死、不留余地。
                // Main/Leo 那两个是老板本人在用，不设这道闸。
                String userPrompt =
                        "第一步：先判断这张图片跟餐饮店的日常工作有没有直接关系。" +
                        "算工作相关的：菜品/食材照片、采购单据、收据发票、盘点表、排班表、报表截图、" +
                        "设备故障照片、门店现场情况、系统界面截图、跟工作有关的聊天记录或文件。" +
                        "不算工作相关的：人物写真/美女帅哥照、明星娱乐、广告海报、表情包、风景旅游照、" +
                        "游戏动漫截图、个人生活照，以及任何跟店里业务无关的内容。" +
                        "第二步：**如果跟工作无关，绝对不许描述图片里的任何内容**——" +
                        "不要说图里有什么人、什么东西、什么场景，一个字都不要描述，" +
                        "只客气地回一句这张图跟工作没关系、有工作上的事随时找你，就结束。这条没有例外。" +
                        "如果跟工作有关，才用同事间聊天的语气把图片内容说清楚。";
                if (message != null && !message.isBlank()) {
                    userPrompt += "\n\n同事随这张图说的话：" + message + "（判定为工作相关时，先回答这句话）";
                }
                String reply = agentGateway.chatVision(systemPrompt(), userPrompt, imageUrl, history);
                Map<String, Object> result = new HashMap<>();
                result.put("reply", reply);
                return Result.success(result);
            }
            if (message == null || message.isBlank()) {
                return Result.error(400, "请输入内容");
            }

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt());
            messages.add(sys);
            for (Map<String, Object> h : history) {
                Object role = h.get("role");
                Object content = h.get("content");
                if (role == null || content == null) continue;
                Map<String, Object> m = new HashMap<>();
                m.put("role", role);
                m.put("content", content);
                messages.add(m);
            }
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            String reply = agentGateway.chatWithTools("openclaw/tom", messages, buildTools());

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[TomChat] 对话失败: {}", e.getMessage());
            return Result.error(500, "AI 助手暂时无法响应，请稍后重试");
        }
    }

    private List<AgentGatewayService.ToolSpec> buildTools() {
        List<AgentGatewayService.ToolSpec> tools = new ArrayList<>();

        tools.add(new AgentGatewayService.ToolSpec(
                "get_store_list",
                "查询系统里真实的门店列表，返回每个门店的ID、名称、地址、状态。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> jdbc.queryForList(
                        "SELECT store_id, store_name, address, status FROM store_info ORDER BY sort_order, store_id")
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_dish_count",
                "查询在售菜品总数（本店）。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> {
                    Integer count = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM dish_master WHERE store_id = ? AND is_active = 1 AND sale_price > 0",
                            Integer.class, DEFAULT_STORE_ID);
                    return Map.of("storeId", DEFAULT_STORE_ID, "dishCount", count);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_bookings",
                "查询本店某天的真实预订/宴会数据（客户、人数、桌数、状态等）。不传日期默认查今天。",
                Map.of("type", "object", "properties", Map.of(
                        "date", Map.of("type", "string", "description", "查询日期，格式 yyyy-MM-dd，不传默认今天")
                ), "required", List.of()),
                args -> {
                    String date = args.get("date") != null ? args.get("date").toString() : LocalDate.now().toString();
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "SELECT booking_id, store_id, customer_name, guest_count, table_count, booking_time, " +
                                    "banquet_name, occasion_type, booking_status, package_name FROM booking_master " +
                                    "WHERE booking_date = ? AND store_id = ? ORDER BY booking_time",
                            date, DEFAULT_STORE_ID);
                    return Map.of("date", date, "count", rows.size(), "bookings", rows);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_pending_inquiries",
                "查询本店还没被处理的客人预定留资（官网/小程序客人自己提交、等员工电话确认的那种）。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> {
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "SELECT id, store_id, customer_name, customer_phone, preferred_date, preferred_time, " +
                                    "guest_count, remark, created_at FROM booking_inquiry " +
                                    "WHERE status = 'pending' AND store_id = ? ORDER BY created_at DESC LIMIT 30",
                            DEFAULT_STORE_ID);
                    return Map.of("count", rows.size(), "inquiries", rows);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_booking_summary",
                "查询本店一段日期范围内的预定汇总数据（每天的预定数、总桌数、总人数），用于做报表/写周报这类场景。" +
                        "不含金额/成本数据。",
                Map.of("type", "object", "properties", Map.of(
                        "startDate", Map.of("type", "string", "description", "起始日期 yyyy-MM-dd"),
                        "endDate", Map.of("type", "string", "description", "结束日期 yyyy-MM-dd，不传默认等于起始日期")
                ), "required", List.of("startDate")),
                args -> {
                    String startDate = args.get("startDate").toString();
                    String endDate = args.get("endDate") != null ? args.get("endDate").toString() : startDate;
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "SELECT booking_date, COUNT(*) AS booking_count, " +
                                    "SUM(table_count) AS total_tables, SUM(guest_count) AS total_guests " +
                                    "FROM booking_master WHERE store_id = ? AND booking_date BETWEEN ? AND ? " +
                                    "GROUP BY booking_date ORDER BY booking_date",
                            DEFAULT_STORE_ID, startDate, endDate);
                    return Map.of("startDate", startDate, "endDate", endDate, "byDate", rows);
                }
        ));

        return tools;
    }

    private boolean checkRateLimit(String key) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> deque = RATE_LIMIT_BUCKETS.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
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

    private String resolveClientKey(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 同 MainPublicController#isLoopback：这个接口只允许本机（后端服务自己）调用。
     * <p>
     * 实际调用方只有企业微信中转 WeComTomController，它是同一个 Spring 应用里
     * postForEntity 到 127.0.0.1:8080；外部流量走 nginx 反代进来，源地址是 nginx 容器 IP。
     * 必须用 getRemoteAddr()，不能用 X-Forwarded-For（那个头可伪造）。
     */
    private boolean isLoopback(jakarta.servlet.http.HttpServletRequest request) {
        String addr = request.getRemoteAddr();
        return "127.0.0.1".equals(addr) || "::1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr);
    }
}
