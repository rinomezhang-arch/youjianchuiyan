package com.youjian.banquet.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 员工板块访问白名单
 *
 * 语义：
 *   1. 员工在 sys_staff_module_access 中【有】记录 → 受限用户，只能访问记录中列出的板块
 *   2. 员工在表中【无】记录 → 不受限，沿用原有权限逻辑
 *
 * 数据全部来自本地数据库，不发起任何外部请求。
 */
@Service
public class ModuleAccessService {

    private static final Logger log = LoggerFactory.getLogger(ModuleAccessService.class);

    /** 每个板块对应的后端接口前缀：受限用户只能调用自身板块的接口 */
    private static final Map<String, List<String>> MODULE_API_PREFIXES = buildModuleApiPrefixes();

    /** 任何已登录用户都需要的公共接口（身份自查 / 退出登录 / 门店信息） */
    private static final List<String> COMMON_API_PREFIXES = List.of(
            "/api/auth/me", "/api/auth/logout", "/api/stores");

    private final JdbcTemplate jdbcTemplate;

    public ModuleAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询某员工的板块白名单。
     * @return 空列表表示该员工不受板块白名单限制
     */
    public List<String> allowedModules(Long staffId) {
        if (staffId == null) {
            return List.of();
        }
        try {
            List<String> modules = jdbcTemplate.queryForList(
                    "SELECT module_key FROM sys_staff_module_access WHERE staff_id = ? ORDER BY module_key",
                    String.class, staffId);
            return modules == null ? List.of() : modules;
        } catch (Exception e) {
            // 白名单表尚未创建时不影响既有用户登录，按"不受限"处理
            log.debug("查询板块白名单失败（表可能尚未创建）: {}", e.getMessage());
            return List.of();
        }
    }

    /** 该员工是否受板块白名单约束 */
    public boolean isRestricted(Long staffId) {
        return !allowedModules(staffId).isEmpty();
    }

    /**
     * 受限用户是否允许访问某个后端接口路径。
     * 采用默认拒绝策略：只放行公共接口与自身板块的接口前缀。
     */
    public boolean isApiAllowed(List<String> allowedModules, String requestPath) {
        if (allowedModules == null || allowedModules.isEmpty()) {
            return true;
        }
        if (requestPath == null) {
            return false;
        }
        for (String prefix : COMMON_API_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        for (String module : allowedModules) {
            for (String prefix : MODULE_API_PREFIXES.getOrDefault(module, List.of())) {
                if (requestPath.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, List<String>> buildModuleApiPrefixes() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("legal", List.of("/api/legal", "/api/hr/contract", "/api/hr/license"));
        map.put("front", List.of("/api/bookings", "/api/tables", "/api/table-board", "/api/banquet",
                "/api/customers", "/api/front-office"));
        map.put("menu", List.of("/api/dishes", "/api/menu", "/api/packages", "/api/recipes", "/api/dict"));
        map.put("kitchen", List.of("/api/kitchen", "/api/production"));
        map.put("supply", List.of("/api/purchase", "/api/inventory", "/api/suppliers", "/api/ingredients",
                "/api/stock-take", "/api/stock-transfer"));
        map.put("marketing", List.of("/api/marketing", "/api/members"));
        map.put("hr", List.of("/api/hr", "/api/staff", "/api/attendance", "/api/schedule", "/api/salary",
                "/api/payroll", "/api/reward-punish"));
        map.put("finance", List.of("/api/finance", "/api/reports", "/api/cost", "/api/reimbursement"));
        map.put("engineering", List.of("/api/engineering", "/api/maintenance", "/api/energy", "/api/tool"));
        map.put("gm", List.of("/api/gm-office", "/api/approval"));
        map.put("system", List.of("/api/operation-log", "/api/upload"));
        map.put("settings", List.of("/api/dict", "/api/department-post"));
        map.put("analytics", List.of("/api/dashboard", "/api/reports"));
        return Collections.unmodifiableMap(map);
    }
}
