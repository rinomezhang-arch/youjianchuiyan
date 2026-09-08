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

    /**
     * 认得的角色。<b>不在这张表里的一律不放行。</b>
     * <p>
     * 原先只要人在册在职就放行，角色是 null、空串或一个谁也没见过的值都照过——
     * 一条角色为空的档案等于一张没写权限的通行证。更要紧的是，它会以"没有角色"的身份
     * 穿过外部人员白名单那道判断，因为那道判断只拦它认识的外部角色，认不出来的一律当自己人。
     * <p>
     * 表的内容分两部分，来源不同，不要混为一谈：
     * <ul>
     *   <li><b>生产在册的 19 类</b>——由当值统筹只读核对生产 staff_master 得来。
     *       上一版这张表是我按代码里的字面量拼的，只有 9 条，会把厨师、传菜、采购、
     *       收银这些正常员工整个挡在门外。名单不是靠猜能补全的，这次是拿真实数据补的。</li>
     *   <li><b>代码内部使用的 4 类</b>——admin 见 {@code UserContext.hasGmRoleCode()}；
     *       store_manager、finance 在代码里出现过；ipad_operator 是切面给 iPad 请求合成的角色，
     *       从来不落 staff_master。这几条生产库里没有对应账号，留着不放宽任何人的权限。</li>
     * </ul>
     * <p>
     * 再有新角色进生产库，这里必须同步添加，否则那个角色的员工进不来。
     */
    private static final java.util.Set<String> KNOWN_ROLES = java.util.Set.of(
            // 生产在册（统筹只读核对所得，按字母序）
            "accountant", "banquet_manager", "cashier", "cold_dish", "cook", "cutter",
            "gm", "greeter", "helper", "kitchen_chef", "lawyer", "manager", "pastry",
            "purchaser", "staff", "super_admin", "supervisor", "waiter", "warehouse",
            // 代码内部使用，生产库无对应账号
            "admin", "store_manager", "finance", "ipad_operator");

    /**
     * 允许带全门店语义（store_id = 0）的角色。
     * <p>
     * 与 {@code UserContext.hasGmRoleCode()} 保持一致。别的角色即便档案里写着 0 也不放行——
     * 全门店范围应当来自角色，而不是来自某条记录门店号填错。
     */
    private static final java.util.Set<String> GLOBAL_SCOPE_ROLES =
            java.util.Set.of("gm", "super_admin", "admin");

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
        // 角色为空或不认识：不放行。
        // 放行的话，这个人会以"没有角色"的身份穿过外部人员白名单那道判断——
        // 白名单只拦它认识的外部角色，认不出来的一律当自己人，等于给未知角色开了后门。
        // 先规范成小写再比对：库里同一个角色可能大小写不一（Manager / MANAGER / manager），
        // 按原样比会把同一个角色判成不认识，白白挡下正常员工。
        String raw = staff.get("role") == null ? null : String.valueOf(staff.get("role")).trim();
        String role = raw == null ? null : raw.toLowerCase(java.util.Locale.ROOT);
        if (role == null || role.isEmpty() || !KNOWN_ROLES.contains(role)) {
            return Verdict.deny();
        }
        // 门店号也要判。store_id = 0 在本系统里不是"第 0 家店"，而是**全门店**的意思
        // （见 UserContext.isGeneralManager()）。一条 store_id 写成 0 的普通员工档案，
        // 会因此拿到跨全部门店的数据范围——这不该由一条数据的写法决定。
        // null 与负数同样不放行：没有门店归属的身份，下游没法判"这条数据是不是你的"。
        Object storeRaw = staff.get("store_id");
        Long storeId = storeRaw instanceof Number number ? number.longValue() : null;
        if (storeId == null || storeId < 0L) {
            return Verdict.deny();
        }
        if (storeId == 0L && !GLOBAL_SCOPE_ROLES.contains(role)) {
            return Verdict.deny();
        }
        return new Verdict(true, storeId, role);
    }
}
