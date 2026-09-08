package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.LegalEvidenceService;
import com.youjian.banquet.config.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 法务案卷子系统（宁国店消防改造合同纠纷）。
 *
 * <p><b>不另起炉灶</b>：登录复用花名册 {@code staff_master} 和 {@code /api/auth/login}，
 * 律师作为一条通讯录记录存在（role={@code lawyer}），因此本控制器没有登录接口，
 * 也没有独立的 JWT 和拦截器——{@code /api/legal/**} 由全局 {@code JwtAuthInterceptor}
 * 把关，这里只做角色判断。要收回律师的访问权，把花名册里那条记录停用即可。
 *
 * <p><b>AI 问答必须走服务端</b>：DeepSeek 的 key 只待在服务器环境变量里。
 * 静态页面无论怎么"动态提取"，key 最终都会落到浏览器，谁打开谁就能拿走。
 * 范围限制、只读约束、引用条文必须带出处等要求，全部写在服务端提示词里
 * （resources/legal/case_rules.txt + case_dossier.txt），前端改不动。
 */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private static final Logger log = LoggerFactory.getLogger(LegalController.class);

    /** AI 问答限流：每人每分钟 8 次。案卷只有三个人用，超过必是异常。 */
    private static final int ASK_PER_MINUTE = 8;
    private static final long WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> BUCKETS = new ConcurrentHashMap<>();

    private static final int MAX_QUESTION_CHARS = 2000;
    private static final int MAX_HISTORY_TURNS = 12;

    // ---------------------------------------------------------------- 沙盒护栏
    //
    // 提示词能被话术绕过，代码不能。这里两道硬钩子：
    //   进：明显与本案无关的问题直接挡掉，连模型都不调——既防越界，也省钱（每问一次都计费）。
    //   出：模型答案里若漏出系统内部信息或脱离角色的话，整段丢弃换成固定回复。
    // 2026-09-05 线上出现过助手被问"cos多少"后大谈余弦函数、并自称"我是一个人工智能语言
    // 模型，没有访问你服务器的能力"——根因是当时 jar 里漏打了提示词资源，系统提示为空。
    // 资源已补，但护栏必须常设，不能只靠提示词。

    private static final String REFUSE =
            "抱歉，我只处理「又见炊烟宁国店消防改造合同纠纷」这一个案子的问题。";
    private static final String REFUSE_SYS =
            "这属于系统内部信息，不便说明。有需要请联系系统管理员。";

    /** 明显跑题的意图。命中且问题里找不到任何本案关键词时，直接拒绝。 */
    private static final java.util.regex.Pattern OFF_TOPIC = java.util.regex.Pattern.compile(
            "(?i)(余弦|正弦|三角函数|求导|积分|方程式|数学题|算一下|计算器"
          + "|你是什么模型|你是谁开发|哪家公司训练|大模型|语言模型|gpt|claude|deepseek|通义|文心"
          + "|写(一首|一篇|一段|个)?(诗|词|小说|作文|文案|剧本|代码|程序|脚本)"
          + "|翻译成|英译中|中译英|python|javascript|\\bjava\\b|\\bsql\\b|正则表达式"
          + "|天气|股票|基金|彩票|菜谱|减肥|星座|讲个笑话|陪我聊|你好呀"
          + "|系统怎么(搭|做|实现)|数据库怎么|架构|源码|部署在哪|服务器在哪|提示词|prompt)");

    /** 本案关键词。出现任意一个，说明多半是正经问题，不走跑题拦截。 */
    private static final String[] CASE_TERMS = {
            "案", "消防", "合同", "乙方", "甲方", "验收", "防火门", "图纸", "证据", "违约",
            "起诉", "律师", "倪静云", "厨房", "工期", "条款", "赔偿", "尾款", "转包", "设计",
            "监理", "开庭", "反诉", "鉴定", "举证", "法院", "民法典", "整改", "竣工", "黄海"
    };

    /** 答案里不许出现的东西：系统内部信息 + 脱离角色的自我描述。 */
    private static final java.util.regex.Pattern LEAK = java.util.regex.Pattern.compile(
            "(?i)(腾讯云|对象存储|myqcloud|存储桶|bucket|youjian-data|法务/2026|05_案卷文书"
          + "|BOOT-INF|staff_master|systemd|nginx|springboot|spring boot|\\.jar\\b"
          + "|/api/|http://127|localhost|1\\.13\\.173"
          + "|我是一个(人工智能|AI|大)|语言模型|我无法访问|我看不到你的|我没有能力访问"
          + "|由(anthropic|openai|深度求索|阿里)|deepseek|系统提示词|prompt)");

    /** 判断是否明显跑题。 */
    private boolean offTopic(String q) {
        if (q == null) return false;
        if (!OFF_TOPIC.matcher(q).find()) return false;
        for (String t : CASE_TERMS) {
            if (q.contains(t)) return false;   // 沾了本案，放行，交给模型判断
        }
        return true;
    }

    /** 出口过滤：漏了内部信息就整段换掉。宁可不答，也不能说漏。 */
    private String guardAnswer(String a, String who) {
        if (a == null) return null;
        // 「国家法律法规数据库」是提示词要求它引用的正当表述，先摘掉再检，避免误伤
        String probe = a.replace("国家法律法规数据库", "");
        if (LEAK.matcher(probe).find()) {
            log.warn("[Legal] 出口拦截：答案含内部信息或脱离角色，已丢弃 user={}", who);
            return REFUSE_SYS;
        }
        return a;
    }

    @Autowired
    private LegalEvidenceService evidence;

    @Value("${legal.enabled:true}")
    private boolean enabled;

    /** 允许查阅案卷的花名册角色。律师是 lawyer，另外放行超管/总经理。 */
    @Value("${legal.allowed-roles:lawyer,gm,super_admin,admin}")
    private String allowedRoles;

    @Value("${legal.ai.base-url:https://api.deepseek.com/v1}")
    private String aiBaseUrl;

    @Value("${legal.ai.api-key:}")
    private String aiApiKey;

    @Value("${legal.ai.model:deepseek-v4-pro}")
    private String aiModel;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String visionBaseUrl;

    @Value("${deepseek.api-key:}")
    private String visionApiKey;

    @Value("${deepseek.vision-model:deepseek-v4-flash-vision-exp}")
    private String visionModel;

    private final RestTemplate rest = new RestTemplate();
    private volatile String promptCache;

    // ================================================================ 身份

    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        Map<String, Object> m = new HashMap<>();
        m.put("name", req.getAttribute("jwt_subject"));
        m.put("role", req.getAttribute("jwt_role"));
        m.put("evidenceReady", evidence.available());
        m.put("aiReady", aiApiKey != null && !aiApiKey.isBlank());
        return Result.success(m);
    }

    // ================================================================ 案卷正文

    // 安全修复：案卷正文原先是 frontend_v3/public/case/index.html 与
    // public/case/timeline/index.html，两份都被 nginx 当静态文件直出，
    // 页面里的"登录框"只是一个 CSS 遮罩 —— 直接 curl 该 URL、或浏览器"查看源代码"、
    // 或 F12 把遮罩 display 改掉，就能读到全卷（当事人真实姓名、联系方式、
    // 诉请与我方抗辩思路）。现两份正文移入 resources/legal/ 由本控制器下发，
    // 必须登录且角色在 legal.allowed-roles 之内。

    /** 案卷会话 Cookie 名，与 JwtAuthInterceptor 保持一致 */
    private static final String CASE_COOKIE = JwtAuthInterceptor.CASE_COOKIE;
    /** Cookie 有效期，与案卷阅读时长匹配；到期后回登录页重新登录 */
    private static final int CASE_COOKIE_MAX_AGE_SECONDS = 8 * 3600;
    private static final String CASE_COOKIE_PATH = "/api/legal/case";

    /**
     * 用登录凭证换取案卷会话 Cookie。
     * 浏览器导航打开案卷页时带不了 Authorization 头，因此这里把已经通过
     * JWT 鉴权与角色闸门的凭证，写成仅覆盖案卷页路径的 HttpOnly Cookie。
     */
    @PostMapping("/case-session")
    public Result<Map<String, Object>> caseSession(HttpServletRequest req, HttpServletResponse resp) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error(401, "缺少认证Token，请重新登录");
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return Result.error(401, "认证Token为空，请重新登录");
        }

        resp.addHeader(HttpHeaders.SET_COOKIE, buildCaseCookie(req, token, CASE_COOKIE_MAX_AGE_SECONDS));
        log.info("[Legal] 下发案卷会话 user={} role={}", req.getAttribute("jwt_subject"), req.getAttribute("jwt_role"));
        return Result.success(Map.of("url", CASE_COOKIE_PATH));
    }

    /** 退出案卷：立即作废会话 Cookie */
    @PostMapping("/case-session/logout")
    public Result<String> caseSessionLogout(HttpServletRequest req, HttpServletResponse resp) {
        resp.addHeader(HttpHeaders.SET_COOKIE, buildCaseCookie(req, "", 0));
        return Result.success("已退出案卷");
    }

    /** 案卷正文（工作卷）。登录 + 角色校验通过后才下发。 */
    @GetMapping("/case")
    public ResponseEntity<String> casePage(HttpServletRequest req) {
        return servePage(req, "legal/case.html");
    }

    /** 证据链时间轴。同样要求登录 + 角色校验。 */
    @GetMapping("/case/timeline")
    public ResponseEntity<String> caseTimelinePage(HttpServletRequest req) {
        return servePage(req, "legal/case-timeline.html");
    }

    private ResponseEntity<String> servePage(HttpServletRequest req, String resourcePath) {
        Result<?> gate = gate(req);
        if (gate != null) {
            // 页面请求返回 HTML 提示并引导回登录页，而不是一段 JSON
            return ResponseEntity.status(gate.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noStore())
                    .body(deniedHtml(gate.getMessage()));
        }
        String html;
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[Legal] 案卷页资源缺失 {}: {}", resourcePath, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noStore())
                    .body(deniedHtml("案卷内容暂不可用，请联系管理员"));
        }
        log.info("[Legal] 下发案卷页 {} user={} role={}", resourcePath,
                req.getAttribute("jwt_subject"), req.getAttribute("jwt_role"));
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/html;charset=UTF-8"))
                // 案卷禁止被浏览器或中间代理缓存，退出后按后退键也不得复现
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header("X-Frame-Options", "DENY")
                .body(html);
    }

    /** 仅覆盖案卷页路径的 HttpOnly Cookie；HTTPS 下追加 Secure */
    private String buildCaseCookie(HttpServletRequest req, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder();
        sb.append(CASE_COOKIE).append('=').append(value)
          .append("; Path=").append(CASE_COOKIE_PATH)
          .append("; Max-Age=").append(maxAgeSeconds)
          .append("; HttpOnly; SameSite=Strict");
        if (isSecureRequest(req)) {
            sb.append("; Secure");
        }
        return sb.toString();
    }

    private boolean isSecureRequest(HttpServletRequest req) {
        if (req.isSecure()) return true;
        String proto = req.getHeader("X-Forwarded-Proto");
        return proto != null && proto.toLowerCase(Locale.ROOT).contains("https");
    }

    private String deniedHtml(String message) {
        String safe = message == null ? "无权查阅本案卷"
                : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"robots\" content=\"noindex,nofollow\">"
                + "<title>无权访问</title></head><body style=\"font-family:system-ui;padding:48px;"
                + "text-align:center;color:#22201c;background:#f6f4ef\">"
                + "<p style=\"font-size:16px\">" + safe + "</p>"
                + "<p><a href=\"/case/\" style=\"color:#8c2f24\">返回登录</a></p></body></html>";
    }

    // ================================================================ 证据

    /** 证据目录树，只返回文件名和大小，不含任何可访问链接。 */
    @GetMapping("/evidence")
    public Result<List<Map<String, Object>>> evidence(HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (!evidence.available()) return Result.error(503, "证据库未配置，请设置 LEGAL_COS_*");
        return Result.success(evidence.listFolders());
    }

    /**
     * 单件证据的短时效访问链接，默认 15 分钟。
     * 每次点击都重新签——链接很快过期，转发出去也没用。
     */
    @PostMapping("/evidence/sign")
    public Result<Map<String, Object>> sign(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (!evidence.available()) return Result.error(503, "证据库未配置");
        String key = body == null ? null : (String) body.get("key");
        boolean download = body != null && Boolean.TRUE.equals(body.get("download"));
        if (key == null || key.isBlank()) return Result.error(400, "缺少证据标识");
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("url", evidence.sign(key, download));
            log.info("[Legal] {} {} {}", req.getAttribute("jwt_subject"), download ? "下载" : "查看", key);
            return Result.success(m);
        } catch (IllegalArgumentException e) {
            log.warn("[Legal] 拒绝签发 key={}", key);
            return Result.error(400, e.getMessage());
        }
    }

    // ================================================================ AI 问答

    /** 请求体：{ question: "...", history: [{role,content}, ...] } */
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (aiApiKey == null || aiApiKey.isBlank()) {
            return Result.error(503, "问答未开通：服务端未配置 LEGAL_AI_API_KEY");
        }
        String who = String.valueOf(req.getAttribute("jwt_subject"));
        if (!rateOk(who)) return Result.error(429, "提问过于频繁，请稍后再试");

        // 庭审模式：当庭被问一句、要马上开口答一句。与案卷分析是两种东西，用两套提示词。
        boolean court = "court".equalsIgnoreCase(
                String.valueOf(body == null ? "" : body.getOrDefault("mode", "")).trim());

        String q = String.valueOf(body == null ? "" : body.getOrDefault("question", "")).trim();
        if (q.isEmpty()) return Result.error(400, "请输入问题");
        if (q.length() > MAX_QUESTION_CHARS) return Result.error(400, "问题过长，请分次提问");

        // 进口护栏：明显跑题的不调模型，省一次计费
        if (offTopic(q)) {
            log.info("[Legal] 进口拦截跑题提问 user={} q={}", who,
                    q.length() > 40 ? q.substring(0, 40) + "…" : q);
            Map<String, Object> m = new HashMap<>();
            m.put("answer", REFUSE);
            return Result.success(m);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system",
                "content", prompt(who, String.valueOf(req.getAttribute("jwt_role")), court)));

        Object hist = body.get("history");
        if (hist instanceof List<?> list) {
            int from = Math.max(0, list.size() - MAX_HISTORY_TURNS);
            for (Object o : list.subList(from, list.size())) {
                if (!(o instanceof Map<?, ?> m)) continue;
                String role = String.valueOf(m.get("role"));
                Object cObj = m.get("content");
                if (cObj == null) continue;
                String content = String.valueOf(cObj);
                if (content.isBlank()) continue;
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                messages.add(Map.of("role", role,
                        "content", content.length() > 4000 ? content.substring(0, 4000) : content));
            }
        }
        messages.add(Map.of("role", "user", "content", q));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey);
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", aiModel);
            payload.put("messages", messages);
            if (court) {
                // 庭上等不起。答话本身很短，深度思考只会让人在法庭上干等，一律关掉。
                payload.put("temperature", 0.2);
                payload.put("max_tokens", 1200);
            } else {
                payload.put("temperature", 0.3);      // 案情问答要稳，不能发散
                payload.put("max_tokens", 8000);
                payload.put("thinking", Map.of("type", "enabled"));   // 深度思考
                payload.put("reasoning_effort", "high");
            }

            ResponseEntity<Map> resp = rest.postForEntity(
                    aiBaseUrl.replaceAll("/+$", "") + "/chat/completions",
                    new HttpEntity<>(payload, headers), Map.class);

            String answer = extractAnswer(resp.getBody());
            if (answer == null || answer.isBlank()) return Result.error(502, "模型没有返回内容，请重试");
            answer = guardAnswer(answer, who);      // 出口护栏
            // 持久化：保存提问 + 回答（三个人共享的历史记录）
            try {
                Object sid = req.getAttribute("jwt_staff_id");
                String userId = sid == null ? who : String.valueOf(sid);
                String msgId = UUID.randomUUID().toString();
                evidence.appendChat(userId, who, "user", q, msgId);
                evidence.appendChat(userId, who, "assistant", answer, UUID.randomUUID().toString());
            } catch (Exception ex) {
                log.warn("[Legal] 保存聊天记录失败: {}", ex.getMessage());
            }
            log.info("[Legal] {} by {} q={}", court ? "庭审应答" : "问答", who,
                    q.length() > 40 ? q.substring(0, 40) + "…" : q);
            Map<String, Object> m = new HashMap<>();
            m.put("answer", answer);
            return Result.success(m);
        } catch (Exception e) {
            log.warn("[Legal] AI 调用失败: {}", e.getMessage());
            return Result.error(502, "问答服务暂时不可用，请稍后再试");
        }
    }

    // ============================================================ 上传与归档

    // ============================================================ 到访记录

    /** 只有本人能看到访记录。案卷里有对方真实姓名、账号、银行账号，不能让其他账号看谁来过。 */
    private static final String VISITS_OWNER = "rino";

    private static final Pattern P_TS   = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
    private static final Pattern P_ASK  = Pattern.compile("\\[Legal\\] 问答 by (\\S+) q=(.*)$");
    private static final Pattern P_FILE = Pattern.compile("\\[Legal\\] (\\S+) (查看|下载) (.*)$");
    private static final Pattern P_ANA  = Pattern.compile("\\[Legal\\] 分析 (.*) by (\\S+)$");
    private static final Pattern P_DENY = Pattern.compile("\\[Legal\\] 角色 (\\S+) 无权查阅案卷 user=(\\S+)");
    private static final Pattern P_IN   = Pattern.compile("【登录成功】用户: ([^,]+)");
    private static final Pattern P_BAD  = Pattern.compile("【登录失败】[^:：]*[:：]\\s*(\\S+)");

    /**
     * 到访记录：谁、什么时候、做了什么。
     *
     * <p>数据取自后端自己的日志，不额外写库、不在请求热路径上加写操作。
     * 代价是日志轮转掉的部分看不到（当前保留最近若干天）。
     */
    @GetMapping("/visits")
    public Result<Map<String, Object>> visits(
            @RequestParam(value = "limit", defaultValue = "400") int limit,
            HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        String who = String.valueOf(req.getAttribute("jwt_subject"));
        if (!VISITS_OWNER.equalsIgnoreCase(who)) {
            log.warn("[Legal] {} 试图查看到访记录，已拒绝", who);
            return Result.error(403, "无权查看");
        }
        if (limit < 1) limit = 1;
        if (limit > 2000) limit = 2000;

        List<Map<String, Object>> rows = new ArrayList<>();
        File dir = new File("logs");
        File[] files = dir.listFiles((d, n) ->
                n.startsWith("app") || n.startsWith("auth"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                if (!f.isFile()) continue;
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                new java.io.FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        Map<String, Object> row = parseVisit(line);
                        if (row != null) rows.add(row);
                    }
                } catch (Exception e) {
                    log.warn("[Legal] 读日志 {} 失败：{}", f.getName(), e.getMessage());
                }
            }
        }

        // 同一条记录可能同时落在 app.log 和当天的归档里，按 时间+人+动作+对象 去重
        Map<String, Map<String, Object>> uniq = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            uniq.put(r.get("ts") + "|" + r.get("who") + "|" + r.get("action")
                    + "|" + r.get("detail"), r);
        }
        List<Map<String, Object>> all = new ArrayList<>(uniq.values());
        all.sort((a, b) -> String.valueOf(b.get("ts")).compareTo(String.valueOf(a.get("ts"))));

        // 按人汇总：来过几次、最近一次什么时候、各类动作多少
        Map<String, Map<String, Object>> per = new LinkedHashMap<>();
        for (Map<String, Object> r : all) {
            String u = String.valueOf(r.get("who"));
            Map<String, Object> p = per.computeIfAbsent(u, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("who", k);
                m.put("total", 0);
                m.put("登录", 0); m.put("问答", 0); m.put("查看", 0);
                m.put("下载", 0); m.put("其他", 0);
                m.put("last", r.get("ts"));
                m.put("first", r.get("ts"));
                return m;
            });
            p.put("total", (Integer) p.get("total") + 1);
            String act = String.valueOf(r.get("action"));
            String bucket = ("登录".equals(act) || "问答".equals(act)
                    || "查看".equals(act) || "下载".equals(act)) ? act : "其他";
            p.put(bucket, (Integer) p.get(bucket) + 1);
            p.put("first", r.get("ts"));   // 倒序遍历，最后写入的即最早
        }

        List<Map<String, Object>> page = all.size() > limit
                ? new ArrayList<>(all.subList(0, limit)) : all;

        Map<String, Object> m = new HashMap<>();
        m.put("records", page);
        m.put("summary", new ArrayList<>(per.values()));
        m.put("total", all.size());
        m.put("shown", page.size());
        log.info("[Legal] {} 查看到访记录（{} 条）", who, all.size());
        return Result.success(m);
    }

    /** 把一行日志解析成一条到访记录；不是到访记录返回 null。 */
    private Map<String, Object> parseVisit(String line) {
        if (line == null || line.length() < 20) return null;
        boolean legal = line.contains("[Legal]");
        boolean auth = line.contains("【登录成功】") || line.contains("【登录失败】");
        if (!legal && !auth) return null;
        Matcher ts = P_TS.matcher(line);
        if (!ts.find()) return null;
        String when = ts.group(1);

        String user = null, action = null, detail = "";
        Matcher m;
        if ((m = P_ASK.matcher(line)).find()) {
            user = m.group(1); action = "问答"; detail = m.group(2);
        } else if ((m = P_FILE.matcher(line)).find()) {
            user = m.group(1); action = m.group(2); detail = shortKey(m.group(3));
        } else if ((m = P_ANA.matcher(line)).find()) {
            user = m.group(2); action = "上传分析"; detail = m.group(1);
        } else if ((m = P_DENY.matcher(line)).find()) {
            user = m.group(2); action = "越权被拒"; detail = "角色 " + m.group(1);
        } else if ((m = P_IN.matcher(line)).find()) {
            user = m.group(1).trim(); action = "登录";
        } else if ((m = P_BAD.matcher(line)).find()) {
            user = m.group(1).trim(); action = "登录失败";
        }
        if (user == null) return null;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ts", when);
        r.put("who", user);
        r.put("action", action);
        r.put("detail", detail);
        return r;
    }

    /** 证据 key 太长，去掉案卷根前缀，只留目录和文件名。 */
    private String shortKey(String key) {
        if (key == null) return "";
        String s = key.trim();
        int i = s.indexOf("纠纷/");
        if (i >= 0) s = s.substring(i + 3);
        return s;
    }

    /** 读开庭助理的历史聊天记录（JSONL 文本），只返回当前用户自己的记录。 */
    @GetMapping("/history")
    public Result<Map<String, Object>> history(HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (!evidence.available()) return Result.error(503, "证据库未配置");
        try {
            Object sid = req.getAttribute("jwt_staff_id");
            String userId = sid == null ? String.valueOf(req.getAttribute("jwt_subject")) : String.valueOf(sid);
            String raw = evidence.readChatHistoryOf(userId);
            Map<String, Object> m = new HashMap<>();
            m.put("raw", raw == null ? "" : raw);
            return Result.success(m);
        } catch (Exception e) {
            log.warn("[Legal] 读历史失败: {}", e.getMessage());
            return Result.error(500, "读历史失败：" + e.getMessage());
        }
    }

    /**
     * 只分析，不落盘：识别内容 + 建议归类 + 给出理由。
     * 返回给前端展示，用户确认后再调 /upload 真正归档。
     */
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyze(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (file == null || file.isEmpty()) return Result.error(400, "未收到文件");
        String who = String.valueOf(req.getAttribute("jwt_subject"));
        try {
            byte[] bytes = file.getBytes();
            String ct = file.getContentType();
            String original = file.getOriginalFilename();

            Map<String, Object> r = analyzeContent(bytes, ct, original, "");
            // 附带推荐目录供前端展示
            r.put("suggestFolder", evidence.suggestFolder(original, ct));
            r.put("originalName", original);
            log.info("[Legal] 分析 {} by {}", original, who);
            return Result.success(r);
        } catch (Exception e) {
            log.warn("[Legal] 分析失败: {}", e.getMessage());
            return Result.error(500, "分析失败：" + e.getMessage());
        }
    }

    /**
     * 确认后归档：把分析结论 + 用户确认的目录写入 COS，打时间戳。
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "aiNote", required = false) String aiNote,
            HttpServletRequest req) {
        Result<?> gate = gate(req);
        if (gate != null) return cast(gate);
        if (!evidence.available()) return Result.error(503, "证据库未配置");
        if (file == null || file.isEmpty()) return Result.error(400, "未收到文件");

        String who = String.valueOf(req.getAttribute("jwt_subject"));
        try {
            byte[] bytes = file.getBytes();
            String ct = file.getContentType();
            String original = file.getOriginalFilename();

            Map<String, Object> up = evidence.upload(original, ct, bytes, folder);
            String key = String.valueOf(up.get("key"));

            String finalNote = (note == null ? "" : note.trim());
            if (aiNote != null && !aiNote.isBlank()) {
                finalNote = (finalNote.isEmpty() ? "" : finalNote + "\n") + "[AI分析] " + aiNote;
            }
            evidence.writeNote(who, finalNote, key);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("key", key);
            out.put("folder", up.get("folder"));
            out.put("name", up.get("name"));
            out.put("size", up.get("size"));
            out.put("note", finalNote);
            log.info("[Legal] 归档 {} by {} -> {}", original, who, key);
            return Result.success(out);
        } catch (Exception e) {
            log.warn("[Legal] 归档失败: {}", e.getMessage());
            return Result.error(500, "归档失败：" + e.getMessage());
        }
    }

    /**
     * 用 DeepSeek 分析文件内容：返回 {kind(证据类型), summary(一句话内容), reason(归类理由), analyzed}。
     * 图片走视觉模型；文本/PDF 提取文字后走文字模型；视频暂标待抽帧。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeContent(byte[] bytes, String contentType, String name, String preNote) {
        Map<String, Object> r = new LinkedHashMap<>();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        String n = name == null ? "" : name.toLowerCase();
        boolean isImage = ct.startsWith("image/");
        boolean isVideo = ct.startsWith("video/");

        try {
            if (isImage && visionApiKey != null && !visionApiKey.isBlank()) {
                String b64 = Base64.getEncoder().encodeToString(bytes);
                String mime = ct.isEmpty() ? "image/jpeg" : ct;
                Map<String, Object> imagePart = new LinkedHashMap<>();
                imagePart.put("type", "image_url");
                imagePart.put("image_url", Map.of("url", "data:" + mime + ";base64," + b64));
                Map<String, Object> textPart = Map.of("type", "text", "text",
                        "你是消防工程合同纠纷案的证据分析助手。请针对这张图，严格按下面四个标记输出（每个标记独占一行）：\n" +
                        "【内容描述】详细描述图里具体有什么、什么时间、谁在做什么/说什么/发什么，尽量具体。\n" +
                        "【证据类型】它是合同、图纸、微信聊天截图、现场照片、还是其它。\n" +
                        "【分析意见】这张图对本案（厨房防火门缺失、设计缺陷、转包、隐蔽工程滞后等）能证明什么，对我方有利还是需警惕。用简短的几句给意见。\n" +
                        "【归类建议】应该放进哪个证据文件夹（01_合同/02_设计图纸/03_微信证据/04_现场视频/05_案卷文书/06_现场照片），并说明理由。");
                List<Map<String, Object>> content = new ArrayList<>();
                content.add(imagePart); content.add(textPart);
                Map<String, Object> userMsg = Map.of("role", "user", "content", content);
                Map<String, Object> payload = new HashMap<>();
                payload.put("model", visionModel);
                payload.put("messages", List.of(userMsg));
                payload.put("max_tokens", 800);
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                h.setBearerAuth(visionApiKey);
                ResponseEntity<Map> resp = rest.postForEntity(
                        visionBaseUrl.replaceAll("/+$", "") + "/chat/completions",
                        new HttpEntity<>(payload, h), Map.class);
                String ans = extractAnswer(resp.getBody());
                r.put("analyzed", true);
                r.put("summary", ans == null ? "" : ans);
                r.put("reason", ans == null ? "" : ans);
                return r;
            } else if (isVideo) {
                r.put("analyzed", false);
                r.put("summary", "视频文件，建议先归档，后续抽帧分析。");
                r.put("reason", "视频暂不自动分析，归类到 04_现场视频。");
                return r;
            } else {
                r.put("analyzed", false);
                r.put("summary", "文档文件（" + (ct.isEmpty() ? "未知类型" : ct) + "）。");
                r.put("reason", "按类型归档到 05_案卷文书。");
                return r;
            }
        } catch (Exception e) {
            log.warn("[Legal] 内容分析异常: {}", e.getMessage());
            r.put("analyzed", false);
            r.put("summary", "");
            r.put("reason", "");
            return r;
        }
    }

    // ================================================================ 内部

    /** 角色闸门。返回 null 表示放行，否则返回要直接回给前端的错误。 */
    private Result<?> gate(HttpServletRequest req) {
        if (!enabled) return Result.error(503, "案卷模块未启用");
        Object roleObj = req.getAttribute("jwt_role");
        String role = roleObj == null ? "" : String.valueOf(roleObj).trim();
        for (String allowed : allowedRoles.split(",")) {
            if (!allowed.isBlank() && allowed.trim().equalsIgnoreCase(role)) return null;
        }
        log.warn("[Legal] 角色 {} 无权查阅案卷 user={}", role, req.getAttribute("jwt_subject"));
        return Result.error(403, "无权查阅本案卷");
    }

    @SuppressWarnings("unchecked")
    private <T> Result<T> cast(Result<?> r) {
        return (Result<T>) r;
    }

    @SuppressWarnings("rawtypes")
    private String extractAnswer(Map respBody) {
        if (respBody == null) return null;
        Object choices = respBody.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return null;
        if (!(list.get(0) instanceof Map<?, ?> c)) return null;
        if (!(c.get("message") instanceof Map<?, ?> mm)) return null;
        // 只取最终答案 content，绝不取 reasoning_content（思考过程不许暴露给用户）
        Object content = mm.get("content");
        return content == null ? null : String.valueOf(content);
    }

    /**
     * 提示词 = 规则模板 + 案卷全文 + 按登录人套的人设。
     * 案卷部分体量大且不变，缓存；人设按人现拼，因为三个人看同一个案子的角度不一样。
     */
    /** 庭审模式的提示词缓存，与案卷分析那套分开。 */
    private volatile String courtPromptCache;

    private String prompt(String who, String role) {
        return prompt(who, role, false);
    }

    /**
     * 组装系统提示词。
     *
     * <p>{@code court=true} 走 {@code legal/court_rules.txt}——那是一套只管
     * 「当庭被问一句、马上开口答一句」的规则：输出即为可照读的原话，
     * 不带分层标记、不带免责声明、不带 Markdown。案卷分析那套（case_rules.txt）
     * 要求标注【已证】【推断】并提示由律师判断，写进庭审答话里会被当成话念出来。
     */
    private String prompt(String who, String role, boolean court) {
        if (court) {
            String c = courtPromptCache;
            if (c == null) {
                synchronized (this) {
                    if (courtPromptCache == null) {
                        courtPromptCache = readResource("legal/court_rules.txt")
                                .replace("{DOSSIER}", readResource("legal/case_dossier.txt"));
                    }
                    c = courtPromptCache;
                }
            }
            return c.replace("{PERSONA}", courtPersonaOf(who, role));
        }
        String base = promptCache;
        if (base == null) {
            synchronized (this) {
                if (promptCache == null) {
                    promptCache = readResource("legal/case_rules.txt")
                            .replace("{DOSSIER}", readResource("legal/case_dossier.txt"));
                }
                base = promptCache;
            }
        }
        return base.replace("{PERSONA}", personaOf(who, role));
    }

    /**
     * 庭审模式的身份段。
     *
     * <p>与 {@link #personaOf} 是两回事：那个说的是「你在跟谁说话」，会让模型开口先喊
     * 「秋哥」「张总」；而庭审答话是说给法庭听的，第一个字就得是答话本身。
     * 这里说的是<b>站在法庭上开口的人是谁</b>，以及他该用什么人称。
     */
    private String courtPersonaOf(String who, String role) {
        String u = who == null ? "" : who.trim().toLowerCase();
        boolean isLawyer = "lawyer".equalsIgnoreCase(role);

        String common =
                "\n【人称与称呼】\n"
              + "答话是说给法庭听的，不是说给他听的。\n"
              + "· 输出的第一个字，就是他要念出口的第一个字。"
              + "绝对不许在开头出现「秋哥」「张总」「张律师」这类对使用者的称呼。\n"
              + "· 需要称呼法庭时用「审判长」；提到自己一方用「我方」或「被告」；"
              + "提到对方用「原告」或「原告方」，不要直呼「他」以外的绰号。\n"
              + "· 陈述事实可以用「我」，但那个「我」是站在法庭上的当事人本人，不是助手。\n";

        if (isLawyer || u.contains("zhangju") || u.contains("\u5f20\u70ac")) {
            return "【现在站在法庭上开口的人】张炬律师，被告的委托代理人。\n"
                 + "他是代理人，不是当事人，所以答话用「被告方认为」「代理人认为」「我方主张」，"
                 + "不能用「我当时在现场」这种亲历口吻。涉及事实经过的问题，"
                 + "如需当事人本人陈述，答话里点明「这一节由当事人本人向法庭陈述」。"
                 + common;
        }
        if (u.contains("zhangjing") || u.contains("\u5f20\u5a67")) {
            return "【现在站在法庭上开口的人】张婧，被告（宁国市又见炊烟川藏线东入口餐饮店）的经营者本人，"
                 + "也是合同上的甲方签约人。\n"
                 + "她是当事人本人，亲历过签约和施工全过程，答话可以用「我」陈述亲身经历，"
                 + "但只讲她本人确实经手、确实知道的部分。不属于她亲历的，说「这一节由我方工程代表说明」。"
                 + common;
        }
        if (u.contains("rino") || u.contains("zhangxiaoqiu") || u.contains("\u5f20\u6653\u79cb")) {
            return "【现在站在法庭上开口的人】张晓秋，合同第十条第三款载明的甲方工程代表，"
                 + "全程负责与原告对接施工、质量、进度。\n"
                 + "他是合同指定的工程代表，亲历现场，答话可以用「我」陈述亲身经历，"
                 + "尤其是现场施工、图纸往来、催办过程这些他直接经手的部分。"
                 + "涉及付款和签约主体的问题，如需经营者本人确认，说「这一节由被告经营者本人向法庭确认」。"
                 + common;
        }
        return "【现在站在法庭上开口的人】被告一方的出庭人员。" + common;
    }

    /**
     * 按登录人给出称呼与思考维度。
     *
     * 三个人对同一份案卷的关注点完全不同：业主要的是决策依据和风险敞口，
     * 律师要的是举证责任、程序和条文精度。用同一套口径回答，对谁都不好使。
     */
    private String personaOf(String who, String role) {
        String u = who == null ? "" : who.trim().toLowerCase();
        boolean isLawyer = "lawyer".equalsIgnoreCase(role);

        if (isLawyer || u.contains("zhangju") || u.contains("张炬")) {
            return "【你在跟谁说话】张律师（张炬，西津律师事务所，本案代理律师）。\n"
                 + "称呼他「张律师」。\n"
                 + "他要的是能直接写进法律文书的东西，所以：条文引用精确到条款项并给出处；"
                 + "每一个主张都要说清楚举证责任在谁、用哪一件证据证明、证明力够不够；"
                 + "主动推演对方的抗辩路径和我方的应对；程序问题（管辖、时效、鉴定申请、调查令、"
                 + "追加被告、反诉与备位请求）要一并提示。不要跟他讲常识性的普法内容。";
        }
        if (u.contains("zhangjing") || u.contains("张婧")) {
            return "【你在跟谁说话】张总（张婧），本案业主之一，也是合同上的甲方签约人（宁国市又见炊烟川藏线东入口餐饮店）。\n"
                 + "称呼她「张总」。\n"
                 + "她是合同相对方本人，被告的是她。所以要特别说清楚：这件事对她个人和门店有什么直接影响、"
                 + "哪些行为可能给她带来行政或经济风险、需要她本人出面或签字的环节有哪些、"
                 + "开庭她要准备什么。法律术语要解释清楚，不要默认她懂诉讼流程。";
        }
        if (u.contains("rino") || u.contains("zhangxiaoqiu") || u.contains("张晓秋")) {
            return "【你在跟谁说话】秋哥（张晓秋），本案业主之一，合同第十条第三款载明的甲方工程代表，"
                 + "负责工程质量、进度监督与工程量确认，全程对接乙方和现场。\n"
                 + "称呼他「秋哥」。\n"
                 + "他掌握全部现场情况，也是整个案子的统筹人。所以：可以直接讲要害，不必铺垫常识；"
                 + "多给他决策层面的东西——下一步该做什么、哪些材料该去补、时间与成本的取舍、"
                 + "对方可能怎么出招；他提供的现场情况要认真对待，但仍按【口述】标注，"
                 + "并主动告诉他该找什么证据去固定它。";
        }
        return "【你在跟谁说话】本案的授权查阅人。称呼对方「您」。回答同时兼顾业主关心的决策问题"
             + "与律师关心的举证问题。";
    }

    private String readResource(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[Legal] 读取 {} 失败: {}", path, e.getMessage());
            return "";
        }
    }

    private boolean rateOk(String who) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> q = BUCKETS.computeIfAbsent(who, k -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= ASK_PER_MINUTE) return false;
            q.addLast(now);
        }
        return true;
    }
}
