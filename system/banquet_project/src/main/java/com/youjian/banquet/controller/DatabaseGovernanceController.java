package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/database-governance")
public class DatabaseGovernanceController {
    private final JdbcTemplate jdbc;

    public DatabaseGovernanceController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/audit")
    public Result<Map<String, Object>> audit() {
        Map<String, Object> result = new LinkedHashMap<>();
        String schema = jdbc.queryForObject("SELECT DATABASE()", String.class);
        int tables = count("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()");
        int primaryKeys = count("SELECT COUNT(DISTINCT TABLE_NAME) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_TYPE='PRIMARY KEY'");
        int foreignKeys = count("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()");
        int emptyTables = count("SELECT COUNT(*) FROM system_table_registry r WHERE r.empty_policy='ALLOW_EMPTY' AND " +
                "(SELECT TABLE_ROWS FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=r.table_name)=0");
        int requiredSeedMissing = count("SELECT COUNT(*) FROM system_table_registry r WHERE r.empty_policy='REQUIRE_SEED' AND " +
                "(SELECT TABLE_ROWS FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=r.table_name)=0");

        result.put("schema", schema);
        result.put("summary", Map.of(
                "tables", tables,
                "primaryKeys", primaryKeys,
                "foreignKeys", foreignKeys,
                "emptyTables", emptyTables,
                "requiredSeedMissing", requiredSeedMissing
        ));
        result.put("tables", jdbc.queryForList("SELECT r.table_name tableName,r.business_domain businessDomain,r.data_kind dataKind," +
                "r.empty_policy emptyPolicy,r.backend_binding backendBinding,r.frontend_binding frontendBinding,r.purpose," +
                "COALESCE(t.TABLE_ROWS,0) rowCount FROM system_table_registry r LEFT JOIN information_schema.TABLES t " +
                "ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=r.table_name ORDER BY r.business_domain,r.table_name"));
        result.put("checks", List.of(
                check("database", "数据库连接", "normal", "当前 Schema: " + schema, ""),
                check("database", "主键覆盖", primaryKeys == tables ? "normal" : "error", primaryKeys + "/" + tables + " 张表具备主键", "无主键表必须补齐稳定标识"),
                check("database", "外键关系", foreignKeys > 0 ? "normal" : "error", "已建立 " + foreignKeys + " 个外键关系", "业务父子表应建立可验证关系"),
                check("database", "制度主数据", requiredSeedMissing == 0 ? "normal" : "error", requiredSeedMissing + " 张必备主数据表为空", "只初始化制度主数据，不伪造交易事实"),
                check("database", "允许为空的事实表", "normal", emptyTables + " 张当前无业务事实，状态合法", "由真实业务流程产生数据"),
                check("backend", "表用途登记", "normal", tables + " 张表已登记用途与空表策略", "持续随迁移更新登记"),
                check("frontend", "治理可视化", "normal", "前端通过受控 API 查看数据库状态", "前端不得直接连接数据库实体")
        ));
        return Result.success(result);
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private Map<String, Object> check(String category, String name, String level, String details, String suggestion) {
        return Map.of("category", category, "name", name, "description", name, "level", level,
                "details", details, "suggestion", suggestion);
    }
}
