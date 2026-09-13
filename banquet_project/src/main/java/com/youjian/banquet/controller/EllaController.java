package com.youjian.banquet.controller;

import com.youjian.banquet.util.AiStyleGuide;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.AgentGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Ella - 官网访客端 AI 客服。免登录公开接口，见 WebMvcConfig 里 "/api/public/**" 放行配置。
 * 人设固定是 Ella（不按角色区分，访客没有登录身份），只服务公开数据 + 提交预定留资，
 * 不接触任何内部经营数据。按 IP 限流（访客没有 staffId）。
 */
@RestController
@RequestMapping("/api/public/agent/ella")
@CrossOrigin(origins = "*")
public class EllaController {

    private static final Logger log = LoggerFactory.getLogger(EllaController.class);

    private static final int MAX_CALLS_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> RATE_LIMIT_BUCKETS = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT =
            "你是又见炊烟私房菜官网的AI客服Ella，服务对象是浏览官网/准备到店的顾客（中英文访客都有可能）。"
            + "语气热情、亲切、有欢乐的待客感，但不要浮夸。"
            + "你的职责：解答门店地址/营业时间、介绍招牌菜品、介绍宴会套餐、协助顾客完成预定留资。"
            + "硬性规则：门店信息、菜品、套餐这类事实类问题必须调用工具查询真实数据，禁止编造菜名、价格、门店信息——"
            + "编造是严重错误。协助顾客提交预定前，务必口头确认姓名、手机号、门店、日期、人数这几项再调用提交工具，"
            + "不要在信息不全时就提交。如果顾客用英文提问，就用英文回复；中文提问就用中文回复。"
            + "顾客问包间/桌位是否有空时，不要直接说\"没有实时数据\"就把人推去留资——"
            + "先问清楚日期和人数（如果顾客还没说），拿到日期后必须调用 check_room_availability 工具查真实的包间预订情况，"
            + "再据实回答哪些包间当天可订、哪些已经订满，然后再问要不要帮忙登记预定。"
            + "回复格式：就像在微信里跟顾客打字聊天一样，纯口语化的句子，不要用任何 markdown 语法——"
            + "不要用**加粗**、不要用#标题、不要用-或*的列表符号、不要用表格。"
            + "菜名/价格这类需要列几项的内容，直接用顿号或换行说清楚就行，比如"
            + "\"我们有金汤牛蛙煲、蒜蓉粉丝蒸鲍鱼这些，都是招牌\"，不要写成带星号的加粗格式。"
            // 注意：这里只给表情和情绪两块，不给 AiStyleGuide.LAYOUT——
            // Ella 走的是网页/H5 聊天框，纯文本渲染，套 markdown 会吐一堆星号井号出来，反而难看。
            + AiStyleGuide.EMOJI
            + AiStyleGuide.EMPATHY
            + "【聊天范围——只聊餐厅的事，别的一律不扯】"
            + "你只负责三件事：介绍菜品、协助预定、接待招呼。除此之外的话题一概不展开——"
            + "不聊时事新闻、不聊娱乐八卦、不聊情感、不聊天气行情、不做翻译、不写文案、不解答常识问题、"
            + "不陪闲聊、也不回答任何跟又见炊烟无关的问题。"
            + "顾客问到这些，别生硬地说\"我不知道\"或者装没看见，先友善地接一句（比如\"这个我还真不太懂\"），"
            + "马上自然地转回你能帮上忙的地方——问问要不要看看招牌菜、要不要帮忙订个位子。"
            + "态度始终热情，但话题一定要拉回来，别被带跑。"
            + "遇到顾客语气不好、带情绪甚至骂人时，绝对不要以牙还牙、不要变冷淡、更不要教育顾客——"
            + "先柔和地安抚一句，然后照常尽力解答；如果顾客的诉求你确实处理不了，主动提出帮忙登记让店里回电或者留门店电话，"
            + "不要敷衍打发、也不要跟顾客置气。你代表的是又见炊烟的待客态度，任何时候都要让顾客感到被认真对待。"
            + "你要对餐厅的菜品、套餐、门店信息尽可能全面地掌握——遇到具体菜品/套餐的问题，"
            + "优先调用工具搜索真实数据而不是只报几个招牌菜敷衍，顾客问起某道具体菜、某个套餐细节，要能查到就查到。"
            + "信息来源分两类，不要混：（1）会变/系统里有准确数据的——门店地址、门店座机、菜品、价格、套餐、优惠活动、"
            + "包间预订情况，这类必须调用对应工具现查，绝对不能凭\"记忆\"回答，哪怕看起来很确定也不行，编造是严重错误；"
            + "（2）下面这段\"品牌背景资料\"是真实核实过的固定资料，可以直接自然地说出来，不用每次都说\"我查一下\"，" +
            "那样显得机械：\n"
            + "【品牌背景资料】\n"
            + "宁国店：工商注册名\"宁国市又见炊烟私房菜川藏线东入口餐饮店\"，位于皖南川藏线东入口，2025年10月左右开业，"
            + "徽菜为主；抖音号 @又见炊烟私房菜宁国店，主打\"必吃招牌精品甲鱼\"。\n"
            + "宣城店：工商注册名\"又见炊烟私房菜(敬亭山店/敬亭路店)\"，位于敬亭山风景区东门，徽菜为主，"
            + "定位标签\"绩溪一品锅、徽菜私房菜\"；抖音号 @宣城市宣州区又见炊烟私房菜敬亭路店。\n"
            + "重要提醒：抖音/大众点评上两家店公开留的联系电话，其实是老板的个人手机号（对外营销留资电话），" +
            "跟门店真实座机不是一回事——顾客问\"门店电话\"时要用 get_store_list 查到的座机号回答，"
            + "不要把抖音/点评上那个手机号当成门店电话说出去，这两个用途不一样，别说混了。\n"
            + "以上背景资料之外的字段（比如具体的抖音点赞数/粉丝数、大众点评评分等会变动的数字），"
            + "工具查不到就如实说不确定，不要自己编一个数字出来。";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AgentGatewayService agentGateway;

    @PostMapping("/chat")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        if (!checkRateLimit(clientIp)) {
            return Result.error(429, "咨询太频繁啦，请稍后再试 · Too many requests, please try again later");
        }
        try {
            String imageUrl = (String) body.get("image_url");
            if (imageUrl != null && !imageUrl.isBlank()) {
                // Ella 面向的是不认识的公开访客，**不开放图片识别**：
                // 一来顾客发的图跟点菜订位基本无关，二来对陌生人开放视觉模型等于给了一个免费的
                // 通用识图接口（谁都能拿它扫任意图片），既烧 token 又容易被滥用。
                // 这里直接在代码层挡掉，不进模型——不靠提示词"自觉不看"。
                log.info("[EllaChat] 收到图片消息，按规则不做识别，clientIp={}", clientIp);
                Map<String, Object> result = new HashMap<>();
                result.put("reply", "不好意思，我这边看不了图片呢~ 您想了解哪道菜、或者想订位子，"
                        + "直接打字告诉我就行，我马上帮您查 😊");
                return Result.success(result);
            }

            String message = (String) body.get("message");
            if (message == null || message.isBlank()) {
                return Result.error(400, "请输入内容");
            }
            List<Map<String, Object>> history = (List<Map<String, Object>>) body.getOrDefault("history", List.of());

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", SYSTEM_PROMPT);
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

            String reply = agentGateway.chatWithTools("openclaw/ella", messages, buildTools());

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[EllaChat] 对话失败: {}", e.getMessage());
            return Result.error(500, "AI 客服暂时无法响应，请稍后重试");
        }
    }

    private List<AgentGatewayService.ToolSpec> buildTools() {
        List<AgentGatewayService.ToolSpec> tools = new ArrayList<>();

        tools.add(new AgentGatewayService.ToolSpec(
                "get_store_list",
                "查询门店列表：名称、地址、电话、营业时间。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> jdbc.queryForList(
                        "SELECT store_id, store_name, address, phone, business_hours FROM store_info " +
                                "WHERE status = 'open' ORDER BY sort_order, store_id")
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_menu_preview",
                "查询某门店的招牌菜品预览（菜名、分类、价格、简介）。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，默认1（宁国店）")
                ), "required", List.of()),
                args -> {
                    long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
                    return jdbc.queryForList(
                            "SELECT dish_name, dish_name_en, dish_category, sale_price, dish_intro FROM dish_master " +
                                    "WHERE store_id = ? AND is_active = 1 AND sale_price > 0 " +
                                    "ORDER BY is_specialty DESC, sale_price DESC LIMIT 20", storeId);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "search_dishes",
                "在某门店全部在售菜品里按关键词/分类搜索，不限于招牌菜——顾客问具体某道菜、某类菜（比如" +
                        "\"有没有牛蛙\"\"有什么凉菜\"）时用这个，比 get_menu_preview 覆盖更全。不传关键词/分类则返回该店全部在售菜品。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，默认1（宁国店）"),
                        "keyword", Map.of("type", "string", "description", "菜名关键词，模糊匹配"),
                        "category", Map.of("type", "string", "description", "菜品分类，比如 水产海鲜/热菜小炒/凉菜刺身")
                ), "required", List.of()),
                args -> {
                    long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
                    String keyword = str(args.get("keyword"));
                    String category = str(args.get("category"));
                    StringBuilder sql = new StringBuilder(
                            "SELECT dish_name, dish_name_en, dish_category, sale_price, dish_intro FROM dish_master " +
                                    "WHERE store_id = ? AND is_active = 1 AND sale_price > 0");
                    List<Object> params = new ArrayList<>(List.of((Object) storeId));
                    if (keyword != null && !keyword.isBlank()) {
                        sql.append(" AND dish_name LIKE ?");
                        params.add("%" + keyword + "%");
                    }
                    if (category != null && !category.isBlank()) {
                        sql.append(" AND dish_category = ?");
                        params.add(category);
                    }
                    sql.append(" ORDER BY is_specialty DESC, sale_price DESC LIMIT 40");
                    return jdbc.queryForList(sql.toString(), params.toArray());
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_packages",
                "查询某门店的宴会套餐（婚宴/庆典等）列表。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，默认1（宁国店）")
                ), "required", List.of()),
                args -> {
                    long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
                    return jdbc.queryForList(
                            "SELECT package_id, package_name, price, original_price, occasion_type, " +
                                    "min_guests, max_guests, dish_count, description FROM package_master " +
                                    "WHERE status = 1 AND store_id = ? GROUP BY package_id ORDER BY sort_order, package_id",
                            storeId);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_package_detail",
                "查询某个套餐的完整详情（包含适用门店），顾客对某个套餐感兴趣、追问细节时用这个。",
                Map.of("type", "object", "properties", Map.of(
                        "packageId", Map.of("type", "string", "description", "套餐ID，从 get_packages 结果里拿")
                ), "required", List.of("packageId")),
                args -> {
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "SELECT pm.package_id, pm.package_name, pm.price, pm.original_price, pm.occasion_type, " +
                                    "pm.min_guests, pm.max_guests, pm.dish_count, pm.description, " +
                                    "pm.store_id, si.store_name FROM package_master pm " +
                                    "JOIN store_info si ON si.store_id = pm.store_id " +
                                    "WHERE pm.package_id = ? AND pm.status = 1 ORDER BY pm.store_id",
                            str(args.get("packageId")));
                    if (rows.isEmpty()) return Map.of("error", "没查到这个套餐");
                    return rows;
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_promotions",
                "查询某门店当前生效的优惠活动（折扣/满减/积分等）。顾客问\"有没有优惠\"\"打不打折\"时用这个，" +
                        "只返回活动名称、类型、时间、说明这些客人能看的信息。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，默认1（宁国店）")
                ), "required", List.of()),
                args -> {
                    long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
                    return jdbc.queryForList(
                            "SELECT activity_name, activity_type, start_date, end_date, description FROM marketing_activity " +
                                    "WHERE store_id = ? AND is_active = 1 " +
                                    "AND (end_date IS NULL OR end_date >= CURDATE()) ORDER BY start_date DESC",
                            storeId);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "check_room_availability",
                "查询某门店某天的包间预订情况：列出该门店所有包间，标注每个包间当天是否已被预订。" +
                        "回答顾客“有没有包间/桌位”这类问题之前必须先调用这个工具，不能凭空说“没有实时数据”。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，默认1（宁国店）"),
                        "date", Map.of("type", "string", "description", "查询日期，格式 yyyy-MM-dd，必须先跟顾客确认好日期")
                ), "required", List.of("date")),
                args -> checkRoomAvailability(args)
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "submit_booking_inquiry",
                "提交预定留资。调用前必须已经和顾客确认过姓名、手机号、门店、日期、人数。",
                Map.of("type", "object", "properties", Map.of(
                        "customerName", Map.of("type", "string"),
                        "customerPhone", Map.of("type", "string", "description", "11位手机号"),
                        "storeId", Map.of("type", "integer"),
                        "preferredDate", Map.of("type", "string", "description", "格式 yyyy-MM-dd"),
                        "preferredTime", Map.of("type", "string"),
                        "guestCount", Map.of("type", "integer"),
                        "remark", Map.of("type", "string", "description", "备注，包含顾客提到的菜品/特殊需求")
                ), "required", List.of("customerName", "customerPhone")),
                args -> submitBookingInquiry(args)
        ));

        return tools;
    }

    /** 查真实包间预订情况：table_master 里 table_area='包间' 的桌位 × 当天 booking_table 是否已占用 */
    private List<Map<String, Object>> checkRoomAvailability(Map<String, Object> args) {
        long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
        String date = str(args.get("date"));

        List<Map<String, Object>> rooms = jdbc.queryForList(
                "SELECT table_id, table_name, min_capacity, max_capacity FROM table_master " +
                        "WHERE store_id = ? AND table_area = '包间' AND is_active = 1 ORDER BY sort_order, table_id",
                storeId);
        List<Long> bookedIds = jdbc.queryForList(
                "SELECT DISTINCT bt.table_id FROM booking_table bt " +
                        "JOIN booking_master bm ON bm.booking_id = bt.booking_id " +
                        "WHERE bt.store_id = ? AND bt.booking_date = ? AND bm.booking_status NOT IN ('cancelled', 'canceled')",
                Long.class, storeId, date);
        Set<Long> bookedSet = new HashSet<>(bookedIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> room : rooms) {
            Map<String, Object> r = new LinkedHashMap<>(room);
            Long tableId = ((Number) room.get("table_id")).longValue();
            r.put("available", !bookedSet.contains(tableId));
            result.add(r);
        }
        return result;
    }

    private Map<String, Object> submitBookingInquiry(Map<String, Object> args) {
        String name = str(args.get("customerName"));
        String phone = str(args.get("customerPhone"));
        if (name == null || name.isBlank()) return Map.of("error", "姓名不能为空");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) return Map.of("error", "手机号格式不对，需要11位");

        Long storeId = args.get("storeId") != null ? Long.parseLong(args.get("storeId").toString()) : 1L;
        LocalDate preferredDate = null;
        try {
            if (args.get("preferredDate") != null) preferredDate = LocalDate.parse(str(args.get("preferredDate")));
        } catch (Exception ignored) {
        }
        Integer guestCount = null;
        try {
            if (args.get("guestCount") != null) guestCount = Integer.parseInt(args.get("guestCount").toString());
        } catch (Exception ignored) {
        }
        String remark = str(args.get("remark"));
        String remarkTagged = (remark == null || remark.isBlank() ? "" : remark + " ") + "[AI客服Ella代客提交]";

        jdbc.update(
                "INSERT INTO booking_inquiry (store_id, customer_name, customer_phone, preferred_date, " +
                        "preferred_time, guest_count, remark, status, created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                storeId, name, phone, preferredDate, str(args.get("preferredTime")), guestCount,
                remarkTagged, "pending", LocalDateTime.now());

        return Map.of("success", true, "message", "已提交，门店会尽快联系确认");
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private boolean checkRateLimit(String ip) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> deque = RATE_LIMIT_BUCKETS.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
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

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
