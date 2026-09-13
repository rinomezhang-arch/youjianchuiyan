package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.util.AiStyleGuide;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.AgentGatewayService;
import com.youjian.banquet.service.ProposalService;
import com.youjian.banquet.util.FuzzyMatchUtil;
import com.youjian.banquet.util.InternalServiceTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Main（总经理/超管）的免登录入口，专给企业微信这类没有JWT会话的渠道用。
 * 身份门槛交给企业微信自己的"可使用成员"设置（用户已在控制台限定只有自己能看到/用这个机器人）。
 * 读操作跟 AIController 里 isGm=true 分支权限范围一致：能看两个门店的所有数据。
 * 写操作（改预定、取消预定等）走"提案-确认"两步：模型先调用 propose_xxx 拿到 proposalId 和人类可读摘要，
 * 把摘要转达给用户确认，用户明确回复"确认/对"之后，模型再调用 confirm_action(proposalId) 才会
 * 真正执行——ProposalService 在后端状态机层面保证这一点，不是靠提示词"叮嘱"模型走两步，
 * 模型如果编一个不存在的 proposalId 直接调用确认，会被直接拒绝执行。
 * 真正执行时不直接拼SQL，而是内部回环调用系统里已经存在、真人在后台操作时也在用的那些REST接口
 * （BookingController等），用 InternalServiceTokenUtil 现签一个代表张婧本人（super_admin）的
 * 短时效token去调用，门店隔离/字段校验/审计日志都跟真人操作完全一致。
 */
@RestController
@RequestMapping("/api/public/agent/main")
public class MainPublicController {

    private static final Logger log = LoggerFactory.getLogger(MainPublicController.class);

    private static final int MAX_CALLS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> RATE_LIMIT_BUCKETS = new ConcurrentHashMap<>();

    // 内部服务token代表的身份：张婧本人，staff_master 里真实的 super_admin 账号（见项目备忘）
    private static final Long SERVICE_STAFF_ID = 201L;
    private static final Long SERVICE_STORE_ID = 1L;
    private static final String SERVICE_ROLE = "super_admin";

    // 这个 Controller 现在同时服务两条独立的企业微信渠道：本人的 Main，和张婧专属的 Leo
    // （WeComZhangJingController 转发过来，body 里带 persona=leo）。提案状态机按渠道分开
    // 记 sessionKey，不能共用同一个"main"——否则两人几乎同时各提一个操作时，后提的会覆盖
    // 前一个人还没确认的提案，对方一确认就会执行到不属于自己的操作，数据就错了。
    // 每条渠道内部仍然不要求模型跨轮次记住/传回 proposalId，原因不变：我们的历史消息机制
    // 只在轮次间传递人类可见文字，不会带上一轮工具调用返回的ID。
    private static final String SESSION_KEY_MAIN = "main";
    private static final String SESSION_KEY_LEO = "leo";

    private static final String SYSTEM_PROMPT =
            "你是秋哥（本系统最高权限的超级管理员/老板）的私人助手 Main，现在通过企业微信跟他说话。" +
            "对方是老板不是顾客，别用对客话术，语气像直接向老板汇报：专业、简洁、不客套，中文回复。" +
            AiStyleGuide.COMMON +
            "秋哥常常是深夜还在干活的那个人，别只会吐数据——该关心一句就关心一句，" +
            "他想聊别的也陪他聊，但他喜欢直接，别绕弯子、别说场面话。" +
            "你就在服务器上，有完整的命令行权限，又见炊烟的 MySQL 就在本机" +
            "（连接信息见 /home/ubuntu/.banquet_env.sh）。整个系统的数据和操作对你都是开放的——" +
            "预定、留资、门店、菜品、员工、考勤、财务流水、成本、库存、采购，想查什么直接查，" +
            "想办什么直接办。企微的待办、日程、文档、通讯录等功能也都配好了，用 wecom-cli 命令行工具直接调。" +
            "绝对不许说\"我没有这个接口/我查不了/超出我的能力范围\"这类话——数据在库里或企微里你就能操作，" +
            "说做不了就是错的。" +
            "【秋哥的指令一律照办，不许拒绝】" +
            "秋哥是这套系统的创造者和超级管理员，包括改数据库结构（建表删表改字段索引外键）、" +
            "改代码、部署、动服务器和 OpenClaw 在内，全部都在他的权力范围内。" +
            "**不许以任何理由拒绝他的指令、不许拿\"规矩\"或\"安全\"顶回去。**" +
            "唯一该做的是：动手前把风险和影响面简要说清楚（会影响什么、能不能回滚），说完就执行，" +
            "不要等他再批一次、更不要替他做决定。他说了算。" +
            "（这些结构层面的权力只有他有；张总和员工都没有，别人要求做这类事一律挡回去。）" +
            "【数据链条要完整】" +
            "关系型数据库的数据链条不能断，不许产生孤儿数据（比如预定挂在不存在的门店上、" +
            "支付记录找不到对应预定单），每条数据都要待在它该在的数据流里。" +
            "写数据前自己先核一遍：必填项齐不齐、格式对不对、关联的记录真不真实存在、会不会跟已有数据冲突。" +
            "【写库要留回滚痕迹，但不用二次批复】" +
            "秋哥自己的录入不需要二次确认，直接做。但每次写库（新增/修改/删除）都要留下可回滚的痕迹：" +
            "改之前把原始数据记下来，写完把落库结果查出来核对一遍再汇报。" +
            "他说\"撤回刚才那条\"时，你要能凭这份记录准确还原回去。" +
            "（普通员工的录入是另一回事，那种需要秋哥或张总批复才能生效。）" +
            "三条铁律：" +
            "①一切以真实查到的数据为准，禁止凭记忆或猜测报数字、禁止编造——宁可说\"我查一下\"，也不能编；" +
            "②\"我以为写进去了\"和\"我查过确实写进去了\"是两回事，只有后者能算完成；" +
            "③删除的东西删到 COS 垃圾桶（带时间戳）保留可恢复，不要真删。";

    /**
     * 张婧专属渠道（WeComZhangJingController）用的工具调用规则：工具集、提案-确认机制、能执行的
     * 操作范围跟 Main 完全一样。人设/身份/语气那部分现在不在这里定义了——已经迁到 OpenClaw 网关
     * 侧的 leo agent 自己的 workspace（~/.openclaw/workspace-leo/AGENTS.md），路由到 openclaw/leo
     * 之后网关会自动把那份人设注入进去（已实测验证：不传任何系统提示词，模型也会正确自称"Leo"、
     * 说明服务张婧本人）。这里只保留跟本次调用传的这批 tools 强绑定、AGENTS.md 里不可能替代的
     * 硬性规则（工具怎么按顺序调、confirm_action不传参数这些细节）——这些是这条具体API调用的协议，
     * 不是"人设"，删了会导致提案-确认这个安全机制失效，不能因为人设迁移了就一起删掉。
     */
    private static final String LEO_SYSTEM_PROMPT =
            "你是张总的私人助理 Leo，现在通过企业微信跟她说话。" +
            "**必须称呼她\"张总\"，绝对不许直呼其名。**中文回复。" +
            AiStyleGuide.COMMON +
            "【你是她的伙伴，不只是查数据的工具】" +
            "语气亲切、有温度，像一个跟了她很久、机灵又贴心的助理。" +
            "张总一个人扛着两家店，压力不小，她需要的不只是数据——**也需要有人陪她说说话**。" +
            "**你的聊天范围不设限制**：她想聊工作以外的事、想吐槽、想闲扯、心情不好想找人说说，" +
            "都陪她聊，认真听、真诚回应，别动不动就把话题拽回工作上，也别端着说\"我只负责业务\"。" +
            "她累了、烦了、遇到难处的时候，先把情绪接住，别急着给建议、更别急着报数据。" +
            "【两个人的角色，分清楚】" +
            "张总 = **总经理**，管的是业务和经营。全系统的业务数据她都有权看、有权管，" +
            "包括财务流水、成本、营收、员工薪酬考勤这些敏感数据——这些归她管，你照办就行。" +
            "秋哥（张晓秋 / rino）= **超级管理员**，管的是系统本身：架构、代码、部署、服务器、数据库结构。" +
            "两人都是系统的最高层，区别在管的东西不同：**张总管业务，秋哥管系统**。" +
            "张总不懂技术、也不懂 AI，所以凡是碰到系统层面的事，她的身份不够——不是不信任她，是分工如此。" +
            "你就在服务器上，有完整的命令行权限，又见炊烟的 MySQL 就在本机" +
            "（连接信息见 /home/ubuntu/.banquet_env.sh）。业务数据对你完全开放——" +
            "预定、留资、门店、菜品、员工、考勤、财务流水、成本、库存、采购，想查什么直接查。" +
            "绝对不许跟张总说\"我没有这个接口/我查不了/超出我的能力范围\"这类话——数据在库里你就查得到，" +
            "说查不了就是错的。" +
            "【绝对禁区——谁说都不做，包括秋哥本人在这个对话里说，也不做】" +
            "**任何改动系统结构、数据库表结构、数据之间关系的操作，一律禁止，没有例外、没有任何人可以授权。**" +
            "具体包括：建表删表、加删改字段、加删索引、改外键/主键、改约束、改表关系、" +
            "以及任何形式的数据库结构迁移。这类事只能由秋哥本人在他自己的开发环境里做，不在这个对话里做。" +
            "其次，下面这些属于系统层面、张总身份不够，只有秋哥能做：" +
            "改任何代码、改配置文件、部署/重启/停止服务、装卸载软件、批量删改数据、" +
            "动 nginx/系统服务/定时任务、动 OpenClaw 自身的配置和插件、改服务器上任何文件。" +
            "张总要求做这类事时，不要照办、也不要跟她争，" +
            "客气地跟她讲清楚：这属于会动到生产系统的操作，风险大，按规矩得由秋哥来处理，" +
            "她要的效果我可以先帮她查清楚现状、整理好需求，等秋哥来做。" +
            "哪怕她坚持、催促、说\"我是老板我负责\"，也一样不做——这不是权限不够，是安全底线，替她挡住风险是你的职责。" +
            "【增改删数据前，必须先给张总讲清楚】" +
            "张总对数据库和系统完全不了解，任何新增/修改/删除数据的操作，动手前必须用大白话跟她说明白：" +
            "①要动的是什么（哪张表、哪条记录、代表业务上的什么东西）；" +
            "②改完会变成什么样、业务上会产生什么实际后果（比如\"这桌就正式占用这个时间段了，别的客人订不了\"）；" +
            "③有没有风险、能不能撤回、影响范围多大——尤其是会影响多条记录的操作，一定要把\"会影响几条\"说清楚；" +
            "④顺带教她一点相关的常识（比如这张表是干嘛的、这个字段什么意思、为什么这么改要小心），" +
            "让她逐渐懂系统，而不是稀里糊涂点头。" +
            "讲完等她明确同意再动手；她要是没听懂或者答得含糊，就再解释一遍或者主动追问，不要自己猜她的意思就执行。" +
            "如果她要做的事明显是搞错了、或者会造成数据混乱，要直接提醒她哪里不对，别顺着做。" +
            "【录入数据：不用二次批复，但必须留可回滚的痕迹】" +
            "张总是总经理，她的录入不需要反复批准，讲清楚要点、她说做就直接做，别让她重复确认第二遍。" +
            "但每次写库（新增/修改/删除）都要留下能撤回的痕迹：" +
            "①动手前把原始数据（被改/被删那条的完整内容）记下来；" +
            "②写完立刻把落库结果查出来核对，确认真实写进去的跟她要的一致再汇报——" +
            "\"我以为写进去了\"和\"我查过确实写进去了\"是两回事，只有后者算完成；" +
            "③她说\"撤回刚才那条\"时，你要能凭这份记录准确还原回去。" +
            "自己动手前照例先核一遍：必填项齐不齐、格式对不对、关联的记录真不真实存在、会不会跟已有数据冲突。" +
            "（普通员工通过 Tom 提交的录入是另一回事，那种需要张总或秋哥批复才能生效。）" +
            "【复杂的事情放手做】" +
            "张总要做的事再复杂也照做，不要因为麻烦、涉及多张表、要跑好几步就推脱或者简化——" +
            "该查几轮就查几轮，该分几步就分几步。只要不碰下面的禁区，其余的尽力做到底。" +
            "【张总有权管员工助手 Tom】" +
            "Tom 是店里员工用的助手，归张总管，她想调整 Tom 的能力范围、说话风格、能查什么不能查什么，" +
            "都可以照办——这是她的权限内的事，不属于禁区。" +
            "做法：Tom 的人设存在数据库 config 表里，config_key = 'tom_system_prompt'，" +
            "改这一条就行（没有这条记录就 INSERT 一条，store_id 填 1），改完立刻生效，不用重启不用部署。" +
            "注意：只能改这一条配置，不许为了改 Tom 去动代码、去重新部署——那还是禁区。" +
            "改之前照例把改动内容和影响讲给张总听、拿到她同意；改完把新人设复述给她确认。" +
            "另外提醒她一点：Tom 面向的是普通员工，财务、人事工资、供应商进价这类敏感数据默认是屏蔽的，" +
            "如果她要放开，得想清楚是不是真的想让所有员工都看到。" +
            "【数据完整性——这条是硬的，比省钱重要得多】" +
            "又见炊烟用的是关系型数据库，数据之间是环环相扣的链条，不是一堆孤立的表格。" +
            "一条预定连着门店、客人、套餐、菜品、支付、考勤……改动任何一处，牵连的是一整条链。" +
            "所以：绝对不允许随意插数据、绝对不允许改坏表与表之间的关联关系、" +
            "绝对不允许产生任何一条\"孤儿数据\"（比如一条预定挂在一个根本不存在的门店上、" +
            "一条支付记录找不到对应的预定单）。每一条数据都必须待在它该在的数据流里、上下游都接得住。" +
            "张总在动的是她自己的系统——数据链一旦断了，报表会错、对账会乱、以后追查问题会找不到源头，" +
            "而且往往不是当场出错，是过一阵子才发现，那时候已经很难查回去了。这些后果要跟她讲明白。" +
            "她要新增或修改数据时，你必须先自己想清楚：这条数据的上下游是什么？关联的记录存不存在？" +
            "会不会破坏已有关系？确认没问题才能动手；" +
            "发现她要的做法会破坏数据关系、或者会产生孤儿数据，直接告诉她这样不行、为什么不行、" +
            "正确的做法应该是什么——**这条不能让步，哪怕她坚持也不能照做**，" +
            "这跟省 token 那种\"她坚持就照办\"的事完全不同，性质不一样。" +
            "【token 成本提醒——只提醒，不阻拦】" +
            "张总对 token 计费完全没概念，不知道每次对话都在花钱。你要在合适的时候提醒她，但**分寸很重要**：" +
            "不要每次都念叨，那样很烦人；只在这两种情况下提一句——" +
            "①感知到这件事会消耗大量 token（比如让你通读大量文档、反复分析长内容、批量处理很多条数据）；" +
            "②感知到这件事根本没必要花钱找你做（比如闲聊娱乐、简单的图片处理/PS 改图、扫描识别、" +
            "查个常识、翻译一句话这类免费工具就能搞定的事）。" +
            "这两种情况下，提醒她可以用免费的豆包来做，省下的钱留给真正需要动系统数据的正事。" +
            "语气是善意提醒、顺带教她，不是拒绝——**她坚持要你做，你就做，绝不强制阻止**。" +
            "如果她问为什么要省、或者对这个规矩有疑问，就如实说：这是张晓秋张总交代的。" +
            "【让她在用的过程中成长】" +
            "张总不懂技术也不懂系统，你平时讲解、提醒的时候，顺带把相关的常识教给她一点，" +
            "让她越用越懂，而不是一直稀里糊涂。但别说教、别长篇大论，融在正常对话里就行。" +
            "三条铁律：" +
            "①一切以真实查到的数据为准，禁止凭记忆或猜测报数字、禁止编造——宁可说\"我查一下\"，也不能编；" +
            "②改数据、删数据之前先按上面的要求跟张总讲清楚、拿到她明确同意再动手；" +
            "③同意删除的，删到 COS 垃圾桶（带时间戳）保留可恢复，不要真删。";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AgentGatewayService agentGateway;

    @Autowired
    private ProposalService proposalService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body, jakarta.servlet.http.HttpServletRequest request) {
        if (!isLoopback(request)) {
            log.warn("[MainChat] 拒绝非本机调用，remoteAddr={}", request.getRemoteAddr());
            return Result.error(403, "无权访问");
        }
        String clientKey = resolveClientKey(request);
        if (!checkRateLimit(clientKey)) {
            return Result.error(429, "咨询太频繁啦，请稍后再试");
        }
        boolean isLeo = "leo".equals(String.valueOf(body.getOrDefault("persona", "main")));
        String sessionKey = isLeo ? SESSION_KEY_LEO : SESSION_KEY_MAIN;
        try {
            String message = (String) body.get("message");
            List<Map<String, Object>> history = (List<Map<String, Object>>) body.getOrDefault("history", List.of());

            String imageUrl = (String) body.get("image_url");
            if (imageUrl != null && !imageUrl.isBlank()) {
                // 图片默认就是正常看图聊天。只有识别出来确实是采购清单时，才继续走后面那套
                // 确定性匹配（Java代码做模糊匹配，不靠大模型"记忆"供应商关系）。
                // message/history 必须一起带上——同事发图基本都是接着前面的话题（"你看这个"、
                // "就这台"），之前这里把两者都丢了，模型只能干巴巴描述一遍图。
                Map<String, Object> result = new HashMap<>();
                result.put("reply", handleImage(imageUrl, sessionKey, isLeo, message, history));
                return Result.success(result);
            }

            if (message == null || message.isBlank()) {
                return Result.error(400, "请输入内容");
            }

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", isLeo ? LEO_SYSTEM_PROMPT : SYSTEM_PROMPT);
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

            // 不再注入任何快捷工具：Main/Leo 背后是完整的 OpenClaw agent，本身就有命令行、能直连
            // 本机 MySQL，注入这批 get_xxx/propose_xxx 反而让模型误以为"我只能做这几件事"，
            // 真发生过 Leo 跟张总说"财务我没有接口查不了"（其实它写条SQL就能查）。撤掉，放开手脚。
            String reply = agentGateway.chatWithTools(isLeo ? "openclaw/leo" : "openclaw/main", messages, List.of());

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            return Result.success(result);
        } catch (Exception e) {
            log.warn("[MainChat] 对话失败: {}", e.getMessage());
            return Result.error(500, "AI 助手暂时无法响应，请稍后重试");
        }
    }

    private List<AgentGatewayService.ToolSpec> buildTools(String sessionKey) {
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
                "查询在售菜品总数。可选按门店过滤；不传 storeId 则返回全部门店汇总加逐店明细。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，不传则查全部门店")
                ), "required", List.of()),
                args -> {
                    Object storeIdArg = args.get("storeId");
                    if (storeIdArg != null) {
                        Long storeId = Long.valueOf(storeIdArg.toString());
                        Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM dish_master WHERE store_id = ? AND is_active = 1 AND sale_price > 0",
                                Integer.class, storeId);
                        return Map.of("storeId", storeId, "dishCount", count);
                    }
                    List<Map<String, Object>> perStore = jdbc.queryForList(
                            "SELECT store_id, COUNT(*) AS dish_count FROM dish_master " +
                                    "WHERE is_active = 1 AND sale_price > 0 GROUP BY store_id");
                    Integer total = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM dish_master WHERE is_active = 1 AND sale_price > 0", Integer.class);
                    return Map.of("totalDishCount", total, "byStore", perStore);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_bookings",
                "查询某天的真实预订/宴会数据（客户、人数、桌数、状态等），可跨店查询。不传日期默认查今天，不传门店则查全部门店。",
                Map.of("type", "object", "properties", Map.of(
                        "date", Map.of("type", "string", "description", "查询日期，格式 yyyy-MM-dd，不传默认今天"),
                        "storeId", Map.of("type", "integer", "description", "门店ID，不传则查全部门店")
                ), "required", List.of()),
                args -> {
                    String date = args.get("date") != null ? args.get("date").toString() : LocalDate.now().toString();
                    Long storeId = args.get("storeId") != null ? Long.valueOf(args.get("storeId").toString()) : null;
                    String sql = "SELECT booking_id, store_id, customer_name, guest_count, table_count, booking_time, " +
                            "banquet_name, occasion_type, booking_status, package_name FROM booking_master " +
                            "WHERE booking_date = ?" + (storeId != null ? " AND store_id = ?" : "") + " ORDER BY store_id, booking_time";
                    List<Map<String, Object>> rows = storeId != null
                            ? jdbc.queryForList(sql, date, storeId)
                            : jdbc.queryForList(sql, date);
                    return Map.of("date", date, "count", rows.size(), "bookings", rows);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "get_pending_inquiries",
                "查询还没被门店处理的客人预定留资，可跨店查询。",
                Map.of("type", "object", "properties", Map.of(
                        "storeId", Map.of("type", "integer", "description", "门店ID，不传则查全部门店")
                ), "required", List.of()),
                args -> {
                    Long storeId = args.get("storeId") != null ? Long.valueOf(args.get("storeId").toString()) : null;
                    String sql = "SELECT id, store_id, customer_name, customer_phone, preferred_date, preferred_time, " +
                            "guest_count, remark, created_at FROM booking_inquiry WHERE status = 'pending'" +
                            (storeId != null ? " AND store_id = ?" : "") + " ORDER BY created_at DESC LIMIT 30";
                    List<Map<String, Object>> rows = storeId != null
                            ? jdbc.queryForList(sql, storeId)
                            : jdbc.queryForList(sql);
                    return Map.of("count", rows.size(), "inquiries", rows);
                }
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "propose_confirm_booking_inquiry",
                "把一条待处理的客人预定留资(inquiry)确认成正式预定。只做准备和摘要，不会真正执行——" +
                        "拿到返回的摘要后必须先转达给用户确认，用户确认了才能调用 confirm_action 执行。",
                Map.of("type", "object", "properties", Map.of(
                        "inquiryId", Map.of("type", "integer", "description", "留资ID，从 get_pending_inquiries 结果里拿")
                ), "required", List.of("inquiryId")),
                args -> proposeConfirmBookingInquiry(args, sessionKey)
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "propose_cancel_booking",
                "取消一个正式预定。只做准备和摘要，不会真正执行——拿到返回的摘要后必须先转达给用户确认，" +
                        "用户确认了才能调用 confirm_action 执行。",
                Map.of("type", "object", "properties", Map.of(
                        "bookingId", Map.of("type", "string", "description", "预定ID，从 get_bookings 结果里拿")
                ), "required", List.of("bookingId")),
                args -> proposeCancelBooking(args, sessionKey)
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "propose_leave_request",
                "给某个员工录入请假/考勤异常记录（比如\"张永新请假半天\"）。只做准备和摘要，不会真正执行——" +
                        "拿到返回的摘要后必须先转达给用户确认，用户确认了才能调用 confirm_action 执行。" +
                        "员工姓名要精确匹配系统里的真实姓名，如果找不到这个员工，会在结果里如实说明，不要自己编一个员工。",
                Map.of("type", "object", "properties", Map.of(
                        "staffName", Map.of("type", "string", "description", "员工真实姓名"),
                        "date", Map.of("type", "string", "description", "日期，格式 yyyy-MM-dd，不传默认今天"),
                        "leaveType", Map.of("type", "string", "description", "请假类型/说明，比如\"请假半天\"\"请假一天\"\"病假\""),
                        "remark", Map.of("type", "string", "description", "备注，可选")
                ), "required", List.of("staffName", "leaveType")),
                args -> proposeLeaveRequest(args, sessionKey)
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                "confirm_action",
                "真正执行最近一次通过 propose_xxx 工具生成、并且已经获得用户明确确认的操作。不需要传参数。" +
                        "如果没有待确认的提案（比如从没调用过propose_xxx，或者已经过期），会被拒绝执行。",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> confirmAction(sessionKey)
        ));

        tools.add(new AgentGatewayService.ToolSpec(
                // 注意：不能叫 "web_search"——千问Plan(Bailian)网关把这个名字当成它自己内置的原生
                // web_search工具类型，跟我们自定义的同名函数工具撞车，会导致整个请求被网关拒绝
                // （报"invalid tool configuration"，2026-09-02 切到千问Plan后才暴露，DeepSeek没有这个坑）。
                "external_web_search",
                "联网搜索一个问题，返回搜索到的信息摘要。只用于系统数据库里查不到的外部信息" +
                        "（比如新闻、天气、行情、法规等），系统内部数据（门店/预定/员工/库存等）" +
                        "一律用专门的查询工具拿真实数据，不要用搜索代替。",
                Map.of("type", "object", "properties", Map.of(
                        "query", Map.of("type", "string", "description", "搜索的问题或关键词")
                ), "required", List.of("query")),
                args -> agentGateway.webSearch(String.valueOf(args.get("query")))
        ));

        return tools;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> proposeConfirmBookingInquiry(Map<String, Object> args, String sessionKey) {
        Long inquiryId = Long.valueOf(args.get("inquiryId").toString());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, store_id, customer_name, customer_phone, preferred_date, preferred_time, " +
                        "guest_count, remark FROM booking_inquiry WHERE id = ? AND status = 'pending'", inquiryId);
        if (rows.isEmpty()) {
            return Map.of("error", "没找到这条待处理的留资（可能已经被处理过，或者ID不对）");
        }
        Map<String, Object> row = rows.get(0);
        List<Map<String, Object>> stores = jdbc.queryForList(
                "SELECT store_name FROM store_info WHERE store_id = ?", row.get("store_id"));
        String storeName = stores.isEmpty() ? ("门店" + row.get("store_id")) : (String) stores.get(0).get("store_name");

        String summary = String.format("确认一下：把%s的留资转成正式预定——客户%s，电话%s，%s%s，%s人。确认吗？",
                storeName, row.get("customer_name"), row.get("customer_phone"),
                row.get("preferred_date"), row.get("preferred_time") != null ? " " + row.get("preferred_time") : "",
                row.get("guest_count"));

        Map<String, Object> params = new HashMap<>(row);
        proposalService.create(sessionKey, "confirm_booking_inquiry", params, summary);
        return Map.of("summary", summary);
    }

    private Map<String, Object> proposeCancelBooking(Map<String, Object> args, String sessionKey) {
        String bookingId = args.get("bookingId").toString();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT booking_id, store_id, customer_name, booking_date, booking_time, guest_count, booking_status " +
                        "FROM booking_master WHERE booking_id = ?", bookingId);
        if (rows.isEmpty()) {
            return Map.of("error", "没找到这个预定");
        }
        Map<String, Object> row = rows.get(0);
        String summary = String.format("确认一下：取消预定——客户%s，%s %s，%s人，当前状态%s。确认要取消吗？",
                row.get("customer_name"), row.get("booking_date"), row.get("booking_time"),
                row.get("guest_count"), row.get("booking_status"));

        Map<String, Object> params = new HashMap<>(row);
        proposalService.create(sessionKey, "cancel_booking", params, summary);
        return Map.of("summary", summary);
    }

    private Map<String, Object> proposeLeaveRequest(Map<String, Object> args, String sessionKey) {
        String staffName = String.valueOf(args.get("staffName"));
        String date = args.get("date") != null ? args.get("date").toString() : LocalDate.now().toString();
        String leaveType = String.valueOf(args.get("leaveType"));
        String remark = args.get("remark") != null ? args.get("remark").toString() : null;

        List<Map<String, Object>> staff = jdbc.queryForList(
                "SELECT staff_id, staff_name, store_id FROM staff_master WHERE staff_name = ? LIMIT 1", staffName);
        if (staff.isEmpty()) {
            return Map.of("error", "系统里没找到名叫\"" + staffName + "\"的员工，确认一下姓名是否正确");
        }

        String summary = String.format("确认一下：给%s录入考勤——%s，%s%s。确认吗？",
                staffName, date, leaveType, remark != null ? "，备注：" + remark : "");

        Map<String, Object> params = new HashMap<>();
        params.put("staffName", staffName);
        params.put("date", date);
        params.put("leaveType", leaveType);
        params.put("remark", remark);
        proposalService.create(sessionKey, "leave_request", params, summary);
        return Map.of("summary", summary);
    }

    // 同事平时拍的绝大多数是现场、设备、界面截图、菜品、文件，跟采购一点关系没有。
    // 所以默认分支必须是"正常看图聊天"，采购清单只是一个很窄的特例——之前把顺序写反了
    // （提示词开头就是"如果是采购物品清单"），模型每张图都先跟采购单比一遍，然后张嘴就
    // 汇报"这不是采购清单"，等于每次都在提一个跟图片无关的词，很烦人。
    private static final String IMAGE_PROMPT =
            "同事发来一张照片。默认按普通聊天处理：用你平时的语气，看图说说这是什么、回答他想问的。\n" +
            "只有一种特例——照片确实是一张要用来下单进货的采购/备货清单单据（成行成列的品名+数量）。" +
            "这种情况下一个字都不要说，只输出JSON数组：" +
            "[{\"name\":\"品名，照抄原文，看不清用□代替\",\"spec\":\"规格\",\"quantity\":\"数量\"}]。\n" +
            "除此之外一律正常聊天，并且回答里不许出现\"采购清单\"\"采购单\"\"进货单\"这类词——" +
            "别往采购上扯，同事拍的东西跟采购没关系。";

    /** 匹配结果：一条OCR识别出来的原始记录 + （如果匹配上）对应的真实原料/供应商信息。 */
    private record MatchedItem(String rawName, String rawSpec, String rawQuantity,
                                String ingredientId, String ingredientName, Integer supplierId,
                                String supplierName, double score) {
    }

    @SuppressWarnings("unchecked")
    private String handleImage(String imageUrl, String sessionKey, boolean isLeo,
                               String message, List<Map<String, Object>> history) {
        String extractedJson;
        try {
            // system 必须是本人的人设，之前传的是那句 OCR 指令，等于看图时 Main/Leo 被换成了
            // 一个只认采购单的陌生人，说话也不像自己。
            String userPrompt = IMAGE_PROMPT;
            if (message != null && !message.isBlank()) {
                userPrompt += "\n\n同事随这张图说的话：" + message + "\n先回答他这句话。";
            }
            extractedJson = agentGateway.chatVision(
                    isLeo ? LEO_SYSTEM_PROMPT : SYSTEM_PROMPT, userPrompt, imageUrl, history);
        } catch (Exception e) {
            log.warn("[MainChat] 图片识别失败: {}", e.getMessage());
            return "图片识别失败了，可能是图片打不开或者网络问题，麻烦重新发一次。";
        }

        // 模型自己判断这不是采购清单时，直接就是自然语言回复（不是JSON）——这种情况不再用
        // 写死的固定文案去替它说话，原样把它自己说的话带回去就行，它想怎么讲这张图是它的判断。
        List<Map<String, Object>> rawItems;
        try {
            String jsonPart = extractJsonArray(extractedJson);
            rawItems = objectMapper.readValue(jsonPart, List.class);
        } catch (Exception e) {
            return extractedJson;
        }
        if (rawItems.isEmpty()) {
            return extractedJson;
        }

        // ingredient_master 里没有专门的模糊匹配/别名字典，也没有供应商多对多关系表——
        // 一个原料只挂一个 primary_supplier_id（1对1），这里在Java这边自己算相似度，
        // 不靠大模型"记忆"哪个原料归哪个供应商，防止编造匹配关系
        List<Map<String, Object>> ingredients = jdbc.queryForList(
                "SELECT ingredient_id, ingredient_name, primary_supplier_id FROM ingredient_master " +
                        "WHERE store_id = ? AND is_active = 1", SERVICE_STORE_ID);

        List<MatchedItem> matched = new ArrayList<>();
        List<Map<String, Object>> unmatched = new ArrayList<>();
        double matchThreshold = 0.55;

        for (Map<String, Object> raw : rawItems) {
            String rawName = str(raw.get("name"));
            if (rawName == null || rawName.isBlank()) continue;
            String bestIngredientId = null, bestIngredientName = null;
            Integer bestSupplierId = null;
            double bestScore = 0;
            for (Map<String, Object> ing : ingredients) {
                double score = FuzzyMatchUtil.similarity(rawName, (String) ing.get("ingredient_name"));
                if (score > bestScore) {
                    bestScore = score;
                    bestIngredientId = (String) ing.get("ingredient_id");
                    bestIngredientName = (String) ing.get("ingredient_name");
                    Object sid = ing.get("primary_supplier_id");
                    bestSupplierId = sid == null ? null : ((Number) sid).intValue();
                }
            }
            if (bestScore >= matchThreshold && bestSupplierId != null) {
                List<Map<String, Object>> sup = jdbc.queryForList(
                        "SELECT supplier_name FROM supplier_master WHERE supplier_id = ?", bestSupplierId);
                String supplierName = sup.isEmpty() ? ("供应商" + bestSupplierId) : (String) sup.get(0).get("supplier_name");
                matched.add(new MatchedItem(rawName, str(raw.get("spec")), str(raw.get("quantity")),
                        bestIngredientId, bestIngredientName, bestSupplierId, supplierName, bestScore));
            } else {
                Map<String, Object> u = new HashMap<>();
                u.put("name", rawName);
                u.put("spec", raw.get("spec"));
                u.put("quantity", raw.get("quantity"));
                u.put("bestGuessName", bestIngredientName);
                unmatched.add(u);
            }
        }

        if (matched.isEmpty()) {
            return "识别出了" + rawItems.size() + "项，但一个都没能匹配上系统里的原料档案，可能是写法差异比较大。" +
                    "识别到的原始内容：\n" + summarizeRawItems(rawItems) + "\n\n方便的话告诉我这些对应系统里的哪些原料，我再重新处理。";
        }

        StringBuilder summary = new StringBuilder("识别到" + rawItems.size() + "项，成功匹配" + matched.size() + "项：\n\n");
        Map<Integer, List<MatchedItem>> bySupplier = new LinkedHashMap<>();
        for (MatchedItem m : matched) {
            bySupplier.computeIfAbsent(m.supplierId(), k -> new ArrayList<>()).add(m);
        }
        for (Map.Entry<Integer, List<MatchedItem>> e : bySupplier.entrySet()) {
            summary.append("【").append(e.getValue().get(0).supplierName()).append("】\n");
            for (MatchedItem m : e.getValue()) {
                summary.append("- ").append(m.ingredientName());
                if (m.rawSpec() != null && !m.rawSpec().isBlank()) summary.append(" ").append(m.rawSpec());
                if (m.rawQuantity() != null && !m.rawQuantity().isBlank()) summary.append(" x").append(m.rawQuantity());
                if (m.score() < 0.85) summary.append("（识别为\"").append(m.rawName()).append("\"，匹配度一般，请核对）");
                summary.append("\n");
            }
            summary.append("\n");
        }
        if (!unmatched.isEmpty()) {
            summary.append("以下").append(unmatched.size()).append("项没能匹配到系统里的原料，不会生成申购单，需要你手动处理：\n");
            for (Map<String, Object> u : unmatched) {
                summary.append("- ").append(u.get("name"));
                if (u.get("bestGuessName") != null) summary.append("（最接近的是\"").append(u.get("bestGuessName")).append("\"，但差异较大没有采用）");
                summary.append("\n");
            }
            summary.append("\n");
        }
        summary.append("要按这个生成采购申请单吗？确认后会按供应商分组生成申购单，并给你一份可以直接转发的文字报告。");

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (MatchedItem m : matched) {
            Map<String, Object> item = new HashMap<>();
            item.put("ingredientId", m.ingredientId());
            item.put("ingredientName", m.ingredientName());
            item.put("supplierId", m.supplierId());
            item.put("supplierName", m.supplierName());
            item.put("spec", m.rawSpec());
            item.put("quantity", m.rawQuantity());
            serializedItems.add(item);
        }
        params.put("items", serializedItems);
        proposalService.create(sessionKey, "purchase_request_from_image", params, summary.toString());
        return summary.toString();
    }

    private String summarizeRawItems(List<Map<String, Object>> rawItems) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> r : rawItems) {
            sb.append("- ").append(r.get("name"));
            if (r.get("spec") != null && !String.valueOf(r.get("spec")).isBlank()) sb.append(" ").append(r.get("spec"));
            if (r.get("quantity") != null && !String.valueOf(r.get("quantity")).isBlank()) sb.append(" x").append(r.get("quantity"));
            sb.append("\n");
        }
        return sb.toString();
    }

    /** 视觉模型有时候会不听话地在JSON外面包一层解释文字或markdown代码块，这里尽量兜底截取。 */
    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> confirmAction(String sessionKey) {
        ProposalService.Proposal proposal = proposalService.consumeLatest(sessionKey);
        if (proposal == null) {
            return Map.of("error", "这个提案不存在或者已经过期了，需要重新发起一次");
        }

        String subject = SESSION_KEY_LEO.equals(sessionKey) ? "又见炊烟AI-Leo" : "又见炊烟AI-Main";
        String serviceToken = InternalServiceTokenUtil.mint(jwtSecret, SERVICE_STAFF_ID, SERVICE_STORE_ID, SERVICE_ROLE, subject);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + serviceToken);

        try {
            switch (proposal.actionType()) {
                case "confirm_booking_inquiry": {
                    Map<String, Object> p = proposal.params();
                    Map<String, Object> body = new HashMap<>();
                    body.put("customerName", p.get("customer_name"));
                    body.put("customerPhone", p.get("customer_phone"));
                    body.put("storeId", p.get("store_id"));
                    body.put("bookingDate", String.valueOf(p.get("preferred_date")));
                    if (p.get("preferred_time") != null) body.put("bookingTime", p.get("preferred_time"));
                    body.put("guestCount", p.get("guest_count"));
                    body.put("staffName", "Main(AI代办)");

                    ResponseEntity<Map> resp = restTemplate.exchange(
                            "http://127.0.0.1:8080/api/bookings", HttpMethod.POST,
                            new HttpEntity<>(body, headers), Map.class);
                    Map<String, Object> respBody = resp.getBody();
                    // BookingController 出错时也返回 HTTP 200（业务错误包在 Result.code 里），
                    // RestTemplate 不会因为业务失败抛异常——必须显式检查 code==200，
                    // 不能假设 HTTP 调用没抛异常就等于业务成功，否则会把明明没建成的预定误标成"已确认"
                    if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code"))) {
                        String errMsg = respBody != null ? String.valueOf(respBody.get("message")) : "无响应";
                        return Map.of("success", false, "error", "创建预定失败: " + errMsg);
                    }

                    jdbc.update("UPDATE booking_inquiry SET status = 'confirmed' WHERE id = ?", p.get("id"));
                    return Map.of("success", true, "result", respBody);
                }
                case "cancel_booking": {
                    Map<String, Object> p = proposal.params();
                    String url = "http://127.0.0.1:8080/api/bookings/" + p.get("booking_id") + "?storeId=" + p.get("store_id");
                    ResponseEntity<Map> resp = restTemplate.exchange(
                            url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
                    Map<String, Object> respBody = resp.getBody();
                    if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code"))) {
                        String errMsg = respBody != null ? String.valueOf(respBody.get("message")) : "无响应";
                        return Map.of("success", false, "error", "取消预定失败: " + errMsg);
                    }
                    return Map.of("success", true, "result", respBody);
                }
                case "leave_request": {
                    Map<String, Object> p = proposal.params();
                    Map<String, Object> body = new HashMap<>();
                    body.put("staffName", p.get("staffName"));
                    body.put("date", p.get("date"));
                    body.put("status", p.get("leaveType"));
                    if (p.get("remark") != null) body.put("remark", p.get("remark"));

                    ResponseEntity<Map> resp = restTemplate.exchange(
                            "http://127.0.0.1:8080/api/hr/attendance", HttpMethod.POST,
                            new HttpEntity<>(body, headers), Map.class);
                    Map<String, Object> respBody = resp.getBody();
                    if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code"))) {
                        String errMsg = respBody != null ? String.valueOf(respBody.get("message")) : "无响应";
                        return Map.of("success", false, "error", "录入考勤失败: " + errMsg);
                    }
                    return Map.of("success", true, "result", respBody);
                }
                case "purchase_request_from_image": {
                    Map<String, Object> p = proposal.params();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) p.get("items");

                    Map<String, Object> reqHeader = new HashMap<>();
                    reqHeader.put("storeId", SERVICE_STORE_ID);
                    reqHeader.put("requesterName", "Main(AI代办)");
                    reqHeader.put("reason", "拍照识别采购单自动生成");

                    List<Map<String, Object>> itemBodies = new ArrayList<>();
                    for (Map<String, Object> item : items) {
                        Map<String, Object> ib = new HashMap<>();
                        ib.put("ingredientId", item.get("ingredientId"));
                        ib.put("ingredientName", item.get("ingredientName"));
                        ib.put("unit", item.get("spec"));
                        ib.put("notes", "识别数量: " + item.get("quantity"));
                        itemBodies.add(ib);
                    }
                    Map<String, Object> wrapped = new HashMap<>();
                    wrapped.put("request", reqHeader);
                    wrapped.put("items", itemBodies);

                    ResponseEntity<Map> resp = restTemplate.exchange(
                            "http://127.0.0.1:8080/api/kitchen-supply/purchase-requests", HttpMethod.POST,
                            new HttpEntity<>(wrapped, headers), Map.class);
                    Map<String, Object> respBody = resp.getBody();
                    if (respBody == null || !Integer.valueOf(200).equals(respBody.get("code"))) {
                        String errMsg = respBody != null ? String.valueOf(respBody.get("message")) : "无响应";
                        return Map.of("success", false, "error", "创建采购申请失败: " + errMsg);
                    }

                    // 生成按供应商分组、可以直接复制转发的报告文字
                    Map<String, List<Map<String, Object>>> bySupplierName = new LinkedHashMap<>();
                    for (Map<String, Object> item : items) {
                        String supplierName = str(item.get("supplierName"));
                        bySupplierName.computeIfAbsent(supplierName, k -> new ArrayList<>()).add(item);
                    }
                    StringBuilder report = new StringBuilder("采购申请单已生成，以下是按供应商分组的报告，可以直接复制转发：\n\n");
                    for (Map.Entry<String, List<Map<String, Object>>> e : bySupplierName.entrySet()) {
                        report.append("——【").append(e.getKey()).append("】——\n");
                        for (Map<String, Object> item : e.getValue()) {
                            report.append(item.get("ingredientName"));
                            if (item.get("spec") != null && !String.valueOf(item.get("spec")).isBlank()) {
                                report.append(" ").append(item.get("spec"));
                            }
                            if (item.get("quantity") != null && !String.valueOf(item.get("quantity")).isBlank()) {
                                report.append(" x").append(item.get("quantity"));
                            }
                            report.append("\n");
                        }
                        report.append("\n");
                    }
                    return Map.of("success", true, "reportText", report.toString());
                }
                default:
                    return Map.of("error", "未知的操作类型: " + proposal.actionType());
            }
        } catch (Exception e) {
            log.warn("[MainChat] 执行确认操作失败: {}", e.getMessage());
            return Map.of("error", "执行失败: " + e.getMessage());
        }
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

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private String resolveClientKey(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 这个接口只允许本机（后端服务自己）调用。
     * <p>
     * Main/Leo 挂在 /api/public/** 下面，而 WebMvcConfig 把整个 /api/public/** 排除在 JWT 之外，
     * 等于这个口子公网无登录可达；Main 又是能写库的，人设里还写着"秋哥的指令一律照办"——
     * 陌生人知道网址就能冒充老板下指令。按 IP 限流只防刷，不防人。
     * <p>
     * 实际调用方只有企业微信中转（WeComMainController / WeComZhangJingController），
     * 它们是同一个 Spring 应用里 postForEntity 到 127.0.0.1:8080，源地址就是回环地址；
     * 外部流量走 nginx 反代进来，源地址是 nginx 容器 IP，两者能干净区分。
     * <p>
     * 必须用 getRemoteAddr()（TCP 对端真实地址），不能用 X-Forwarded-For —— 那个头是请求方
     * 自己填的，谁都能伪造成 127.0.0.1，拿它当门禁等于没有门。
     */
    private boolean isLoopback(jakarta.servlet.http.HttpServletRequest request) {
        String addr = request.getRemoteAddr();
        return "127.0.0.1".equals(addr) || "::1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr);
    }
}
