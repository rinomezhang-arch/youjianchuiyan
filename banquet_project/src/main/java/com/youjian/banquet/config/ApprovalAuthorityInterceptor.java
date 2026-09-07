package com.youjian.banquet.config;

import com.youjian.banquet.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 审批批复人白名单拦截器。
 * <p>
 * 现阶段公司只授权张婧、张晓秋两个人批复任何审批单据（请假、加班、采购申请、领料、
 * 报销、奖惩、自助入职、盘亏等全部走审批的模块）。这是临时的经营决定，不是权限模型
 * 重构：原有 RBAC 角色、门店隔离一律保留，本拦截器只在它们之上再叠一层"谁能点通过/驳回"。
 * <p>
 * 之所以做成拦截器而不是逐个接口加判断：审批入口分散在 8 个以上 Controller，
 * 而且后续还会加，逐个加判断必然漏。这里统一按 URL 里的 approve/reject 动作拦截，
 * 新增审批接口只要沿用同样的命名就自动受控。
 * <p>
 * 放开限制时改配置即可：{@code approval.approvers}（逗号分隔，姓名/账号/英文名都认），
 * 或 {@code approval.authority-enabled=false} 整个关掉。
 */
@Component
public class ApprovalAuthorityInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalAuthorityInterceptor.class);

    /**
     * 命中"批复动作"的路径：路径段本身是 approve/reject，或者带前缀的 chef-approve、store-approve 这类。
     * 只认这两个动作词，像 /auth/verify、/audit-logs 这种不是审批的接口不会被误伤。
     */
    private static final Pattern APPROVAL_ACTION =
            Pattern.compile("(^|/)([a-z]+-)?(approve|reject)(/|$)", Pattern.CASE_INSENSITIVE);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${approval.authority-enabled:true}")
    private boolean enabled;

    /** 默认白名单：张婧、张晓秋本人。同一个人在花名册里有中文名/拼音账号/英文名多种写法，这里全列上。 */
    @Value("${approval.approvers:张婧,zhangjing,张晓秋,zhangxiaoqiu,rino}")
    private String approversRaw;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!enabled) {
            return true;
        }
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null || !APPROVAL_ACTION.matcher(uri).find()) {
            return true;
        }

        Set<String> allowed = allowedIdentities();
        if (identityOf(request).stream().anyMatch(allowed::contains)) {
            return true;
        }

        Object subject = request.getAttribute("jwt_subject");
        log.warn("【审批越权拦截】{} {} 被拒：当前登录人 {} 不在批复白名单内", method, uri, subject);
        reject(response);
        return false;
    }

    /** 白名单标识：全部转小写去空格，便于和花名册里的写法比对。 */
    private Set<String> allowedIdentities() {
        Set<String> allowed = new LinkedHashSet<>();
        for (String item : approversRaw.split(",")) {
            String v = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
            if (!v.isEmpty()) {
                allowed.add(v);
            }
        }
        return allowed;
    }

    /**
     * 当前登录人的全部可比对写法：JWT 里的登录名，以及花名册里这条员工记录的
     * 中文名 / 登录账号 / 英文名。任意一种命中白名单即放行。
     * <p>
     * 必须查库而不是只认 JWT subject：登录支持姓名/账号/手机号/英文名四种输入，
     * 同一个人拿到的 subject 可能是其中任意一种。
     */
    private Set<String> identityOf(HttpServletRequest request) {
        Set<String> names = new LinkedHashSet<>();
        Object subject = request.getAttribute("jwt_subject");
        if (subject != null) {
            names.add(String.valueOf(subject).trim().toLowerCase(Locale.ROOT));
        }
        Object staffId = request.getAttribute("jwt_staff_id");
        if (staffId != null) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT staff_name, staff_account, staff_en_name FROM staff_master WHERE staff_id = ? LIMIT 1",
                        Long.valueOf(String.valueOf(staffId)));
                if (!rows.isEmpty()) {
                    Map<String, Object> row = rows.get(0);
                    for (String key : Arrays.asList("staff_name", "staff_account", "staff_en_name")) {
                        Object v = row.get(key);
                        if (v != null && !String.valueOf(v).isBlank()) {
                            names.add(String.valueOf(v).trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } catch (RuntimeException e) {
                // 查不到花名册时不放行——审批是钱和人事相关的动作，宁可拦住让人重登，也不能默认通过。
                log.warn("【审批白名单】读取员工 {} 信息失败，按未授权处理：{}", staffId, e.getMessage());
            }
        }
        names.remove("");
        return names;
    }

    private void reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<String> body = Result.error(403, "当前仅张婧、张晓秋有审批批复权限，请转交他们处理");
        response.getWriter().write(Objects.requireNonNull(MAPPER.writeValueAsString(body)));
        response.getWriter().flush();
    }

    /** Collaboration uses the same configured whitelist, but requires an existing active human staff record. */
    public java.util.Optional<String> collaborationApprover(Long staffId) {
        if (staffId == null || staffId <= 0) return java.util.Optional.empty();
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(
                "SELECT staff_name,staff_account,staff_en_name,role,employment_status FROM staff_master WHERE staff_id=? LIMIT 1", staffId);
        if (rows.size() != 1) return java.util.Optional.empty();
        Map<String,Object> row = rows.get(0);
        String role = String.valueOf(row.get("role")).toLowerCase(Locale.ROOT);
        String status = String.valueOf(row.get("employment_status")).toLowerCase(Locale.ROOT);
        if (Set.of("lawyer","agent","ai","bot").contains(role) || !Set.of("active","probation").contains(status))
            return java.util.Optional.empty();
        Set<String> allowed = allowedIdentities();
        Set<String> names = List.of("staff_name","staff_account","staff_en_name").stream()
                .map(row::get).filter(Objects::nonNull).map(Object::toString)
                .map(v -> v.trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        // User's collaboration authority: only Qiu-ge. Existing business approval policy is unchanged.
        boolean permitted = names.stream().anyMatch(allowed::contains)
                && names.stream().anyMatch(Set.of("rino","zhangxiaoqiu","张晓秋")::contains);
        if (!permitted) return java.util.Optional.empty();
        Object account = row.get("staff_account");
        return java.util.Optional.of(String.valueOf(account != null ? account : row.get("staff_name")));
    }
}
