package com.youjian.banquet.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 实时员工档案复核。
 * <p>
 * <b>要解决的问题：JWT 是签发那一刻的快照。</b>
 * 签发之后这个人可能已离职、被停用、被调去别的门店、角色被降成外部律师——
 * 而 token 还在有效期内，照样验得过签名。只验签名等于承认"半小时前的事实"就是现在的事实。
 * <p>
 * 所以每次请求都回 staff_master 拿一次当前状态，并且<b>下游只认这里查回来的值</b>，
 * 不认 token 里带的 storeId 与 role。否则把人从一店调到二店、或把角色改成 lawyer，
 * 旧 token 仍然按旧门店旧角色放行，撤权就成了一句空话。
 * <p>
 * 这里只读不写，只查四列，不碰任何业务表，也不碰法务数据。
 */
@Component
public class StaffRealtimeGuard {

    /** 在职的两种写法：历史数据里中英文并存，两种都算在职。 */
    private static final String ACTIVE_EN = "active", ACTIVE_CN = "在职";

    private static final String SQL =
            "SELECT staff_id, store_id, role, employment_status FROM staff_master WHERE staff_id = ? LIMIT 1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public StaffRealtimeGuard() {
    }

    public StaffRealtimeGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 复核结论。
     * <p>
     * {@code ok=false} 时 {@code storeId/role} 一律为空——被拒的调用方拿不到任何档案信息，
     * 免得"拒绝"本身变成一个查人接口。
     */
    public record Verdict(boolean ok, Long storeId, String role) {

        static Verdict deny() {
            return new Verdict(false, null, null);
        }
    }

    public Verdict verify(Long staffId) {
        return verify(jdbcTemplate, staffId);
    }

    /**
     * 静态入口：给已经持有 JdbcTemplate、不方便再注入一个 Bean 的调用方用（如 AuthController）。
     * 两条路径必须共用同一份判定，分开写迟早会走岔。
     */
    public static Verdict verify(JdbcTemplate jdbcTemplate, Long staffId) {
        if (jdbcTemplate == null || staffId == null) {
            // 查不了就当没通过。鉴权这种地方，"不确定"只能等于"不放行"。
            return Verdict.deny();
        }
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(SQL, staffId);
        } catch (Exception e) {
            // 连库失败同样按不通过处理，不因为基础设施抖动就把门打开。
            return Verdict.deny();
        }
        if (rows.isEmpty()) {
            return Verdict.deny();
        }
        Map<String, Object> staff = rows.get(0);
        String employment = Objects.toString(staff.get("employment_status"), "");
        if (!ACTIVE_EN.equals(employment) && !ACTIVE_CN.equals(employment)) {
            return Verdict.deny();
        }
        Object storeRaw = staff.get("store_id");
        Long storeId = storeRaw instanceof Number number ? number.longValue() : null;
        String role = staff.get("role") == null ? null : String.valueOf(staff.get("role"));
        return new Verdict(true, storeId, role);
    }
}
